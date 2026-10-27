package job;

import util.AppConstants;
import util.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consolidated scheduler jobs for background maintenance tasks.
 * All 9 jobs execute idempotent operations against the database.
 */
public final class SchedulerJobs {

    private static final Logger LOGGER = Logger.getLogger(SchedulerJobs.class.getName());

    /**
     * Shared reentrant lock held during each job cycle to prevent race conditions
     * between background workers (FastCloseJobWorker, SlowWorker) and manual triggers.
     */
    public static final java.util.concurrent.locks.ReentrantLock JOB_LOCK = new java.util.concurrent.locks.ReentrantLock();

    private SchedulerJobs() {
    }

    /**
     * R10: tells open admin pages that new alerts exist (Alerts inbox, dashboard, nav badge).
     * Called after the transaction commits; a broadcast failure never breaks the job.
     * Public so the IoT event API (servlet.IotDeviceApiServlet) announces device alerts the same way.
     */
    public static void announceAlerts(String alertType) {
        try {
            java.util.Map<String, Object> ev = new java.util.HashMap<>();
            ev.put("alertType", alertType);
            ev.put("title", util.AlertTitles.title(alertType));
            ev.put("status", "NEW");
            ev.put("newAlertCount", new dao.AlertLogDao().countNewAlerts());
            websocket.RealtimeWebSocketEndpoint.broadcastEvent("ALERT_CREATED", ev);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to broadcast ALERT_CREATED for " + alertType, e);
        }
    }

    // --- Helper DTOs for multi-step jobs ---

    private static class ExpiredBookingInfo {
        final long bookingId;
        final String bookingCode;

        ExpiredBookingInfo(long bookingId, String bookingCode) {
            this.bookingId = bookingId;
            this.bookingCode = bookingCode;
        }
    }

    private static class NoShowBookingInfo {
        final long bookingId;
        final String bookingCode;
        final long roomId;
        final String roomNumber;
        final String slotDate;

        NoShowBookingInfo(long bookingId, String bookingCode, long roomId, String roomNumber, String slotDate) {
            this.bookingId = bookingId;
            this.bookingCode = bookingCode;
            this.roomId = roomId;
            this.roomNumber = roomNumber;
            this.slotDate = slotDate;
        }
    }

    private static class AutoCheckoutBookingInfo {
        final long bookingId;
        final String bookingCode;
        final long roomId;

        AutoCheckoutBookingInfo(long bookingId, String bookingCode, long roomId) {
            this.bookingId = bookingId;
            this.bookingCode = bookingCode;
            this.roomId = roomId;
        }
    }

    private static class ExpiredSessionInfo {
        final long sessionId;
        final long roomId;
        final String roomNumber;
        final String staffName;

        ExpiredSessionInfo(long sessionId, long roomId, String roomNumber, String staffName) {
            this.sessionId = sessionId;
            this.roomId = roomId;
            this.roomNumber = roomNumber;
            this.staffName = staffName;
        }
    }

    private static class OfflineDeviceInfo {
        final String deviceId;
        final String deviceName;
        final Long roomId;
        final String zoneId;

        OfflineDeviceInfo(String deviceId, String deviceName, Long roomId, String zoneId) {
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.roomId = roomId;
            this.zoneId = zoneId;
        }
    }

    // 1. ExpirePendingBookings
    public static void expirePendingBookings() throws SQLException {
        int holdMinutes = AppConstants.BOOKING_HOLD_MINUTES;

        String selectSql = "SELECT booking_id, booking_code "
                + "FROM BOOKING "
                + "WHERE status = 'PENDING_PAYMENT' "
                + "  AND ( "
                + "      (payment_deadline IS NOT NULL AND payment_deadline < SYSDATETIME()) "
                + "      OR (payment_deadline IS NULL AND created_at < DATEADD(MINUTE, -?, SYSDATETIME())) "
                + "  )";

        String updateBookingSql = "UPDATE BOOKING "
                + "SET status = 'EXPIRED', expire_reason = 'PAYMENT_TIMEOUT', updated_at = SYSDATETIME() "
                + "WHERE booking_id = ? AND status = 'PENDING_PAYMENT'";

        String releaseSlotsSql = "UPDATE BOOKING_SLOT "
                + "SET hold_status = 'RELEASED', released_at = SYSDATETIME() "
                + "WHERE booking_id = ? AND hold_status = 'HELD'";

        String expirePaymentsSql = "UPDATE PAYMENT "
                + "SET status = 'EXPIRED' "
                + "WHERE booking_id = ? AND status = 'PENDING'";

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<ExpiredBookingInfo> expiredBookings = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, holdMinutes);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            expiredBookings.add(new ExpiredBookingInfo(
                                    rs.getLong("booking_id"),
                                    rs.getString("booking_code")
                            ));
                        }
                    }
                }

                if (!expiredBookings.isEmpty()) {
                    try (PreparedStatement psBooking = conn.prepareStatement(updateBookingSql);
                         PreparedStatement psSlots = conn.prepareStatement(releaseSlotsSql);
                         PreparedStatement psPayment = conn.prepareStatement(expirePaymentsSql)) {

                        for (ExpiredBookingInfo b : expiredBookings) {
                            psBooking.setLong(1, b.bookingId);
                            psBooking.executeUpdate();

                            psSlots.setLong(1, b.bookingId);
                            psSlots.executeUpdate();

                            psPayment.setLong(1, b.bookingId);
                            psPayment.executeUpdate();

                            LOGGER.info("Scheduler: Booking " + b.bookingCode + " expired due to payment timeout");
                        }
                    }
                    LOGGER.info("Scheduler ExpirePendingBookings expired " + expiredBookings.size() + " bookings.");
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Scheduler ExpirePendingBookings transaction rolled back", e);
                throw e;
            }
        }
    }

    // 2. ExpirePaymentLinks
    public static void expirePaymentLinks() throws SQLException {
        String sql = "UPDATE PAYMENT "
                + "SET status = 'EXPIRED' "
                + "WHERE status = 'PENDING' "
                + "  AND expires_at IS NOT NULL "
                + "  AND expires_at < SYSDATETIME()";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int updated = ps.executeUpdate();
            if (updated > 0) {
                LOGGER.info("Scheduler ExpirePaymentLinks expired " + updated + " payment links.");
            }
        }
    }

    // 3. ActivateCredentials
    public static void activateCredentials() throws SQLException {
        Timestamp nowTs = util.ClockService.nowTimestamp();
        String sql = "UPDATE ACCESS_CREDENTIAL "
                + "SET status = 'ACTIVE' "
                + "WHERE status = 'PENDING' "
                + "  AND valid_from <= ? "
                + "  AND valid_until > ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, nowTs);
            ps.setTimestamp(2, nowTs);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                LOGGER.info("Scheduler ActivateCredentials activated " + updated + " credentials.");
            }
        }
    }

    // 4. HandleNoShow
    public static void handleNoShow() throws SQLException {
        int graceMinutes = AppConstants.NOSHOW_GRACE_MINUTES;
        Timestamp nowTs = util.ClockService.nowTimestamp();

        String selectSql = "SELECT b.booking_id, b.booking_code, b.room_id, r.room_number, CONVERT(VARCHAR(10), b.slot_date, 120) AS slot_date_str "
                + "FROM BOOKING b "
                + "JOIN ROOM r ON b.room_id = r.room_id "
                + "JOIN BOOKING_SLOT bs ON b.booking_id = bs.booking_id "
                + "WHERE b.status = 'CONFIRMED' "
                + "  AND b.actual_checkin_at IS NULL "
                + "GROUP BY b.booking_id, b.booking_code, b.room_id, r.room_number, b.slot_date "
                + "HAVING ? > DATEADD(MINUTE, ?, MIN(bs.start_at))";

        String updateBookingSql = "UPDATE BOOKING "
                + "SET status = 'EXPIRED', expire_reason = 'NO_SHOW', updated_at = ? "
                + "WHERE booking_id = ? AND status = 'CONFIRMED'";

        String revokeCredentialsSql = "UPDATE ACCESS_CREDENTIAL "
                + "SET status = 'REVOKED', revoked_at = ?, revoke_reason = 'NO_SHOW' "
                + "WHERE booking_id = ? AND status IN ('PENDING', 'ACTIVE')";

        String insertAlertSql = "INSERT INTO ALERT_LOG (alert_type, severity, status, message, room_id, booking_id, created_at) "
                + "SELECT 'BOOKING_NO_SHOW', 'HIGH', 'NEW', ?, ?, ?, SYSDATETIME() "
                + "WHERE NOT EXISTS ( "
                + "    SELECT 1 FROM ALERT_LOG "
                + "    WHERE alert_type = 'BOOKING_NO_SHOW' AND booking_id = ? "
                + ")";

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<NoShowBookingInfo> noShowBookings = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setTimestamp(1, nowTs);
                    ps.setInt(2, graceMinutes);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            noShowBookings.add(new NoShowBookingInfo(
                                    rs.getLong("booking_id"),
                                    rs.getString("booking_code"),
                                    rs.getLong("room_id"),
                                    rs.getString("room_number"),
                                    rs.getString("slot_date_str")
                            ));
                        }
                    }
                }

                if (!noShowBookings.isEmpty()) {
                    try (PreparedStatement psBooking = conn.prepareStatement(updateBookingSql);
                         PreparedStatement psCred = conn.prepareStatement(revokeCredentialsSql);
                         PreparedStatement psAlert = conn.prepareStatement(insertAlertSql)) {

                        for (NoShowBookingInfo b : noShowBookings) {
                            psBooking.setTimestamp(1, nowTs);
                            psBooking.setLong(2, b.bookingId);
                            psBooking.executeUpdate();

                            psCred.setTimestamp(1, nowTs);
                            psCred.setLong(2, b.bookingId);
                            psCred.executeUpdate();

                            String alertMsg = "Guest did not check in within " + graceMinutes
                                    + " minutes grace period for booking " + b.bookingCode
                                    + " (Room " + b.roomNumber + ", date: " + b.slotDate + ")";
                            psAlert.setString(1, alertMsg);
                            psAlert.setLong(2, b.roomId);
                            psAlert.setLong(3, b.bookingId);
                            psAlert.setLong(4, b.bookingId);
                            psAlert.executeUpdate();

                            LOGGER.info("Scheduler: Booking " + b.bookingCode + " marked NO_SHOW");

                            LOGGER.info("Scheduler: Door PIN revoked due to NO_SHOW for booking " + b.bookingCode);
                        }
                    }
                    LOGGER.info("Scheduler HandleNoShow handled " + noShowBookings.size() + " no-show bookings.");
                }

                conn.commit();
                if (!noShowBookings.isEmpty()) {
                    announceAlerts("BOOKING_NO_SHOW");
                }
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Scheduler HandleNoShow transaction rolled back", e);
                throw e;
            }
        }
    }

    // 5. AutoCheckout
    public static void autoCheckout() throws SQLException {
        Timestamp nowTs = util.ClockService.nowTimestamp();
        String selectSql = "SELECT b.booking_id, b.booking_code, b.room_id "
                + "FROM BOOKING b "
                + "JOIN BOOKING_SLOT bs ON b.booking_id = bs.booking_id "
                + "WHERE b.status = 'CHECKED_IN' "
                + "GROUP BY b.booking_id, b.booking_code, b.room_id "
                + "HAVING ? > MAX(bs.end_at)";

        String updateBookingSql = "UPDATE BOOKING "
                + "SET status = 'CHECKED_OUT', actual_checkout_at = ?, updated_at = ? "
                + "WHERE booking_id = ? AND status = 'CHECKED_IN'";

        String expireCredSql = "UPDATE ACCESS_CREDENTIAL "
                + "SET status = 'EXPIRED' "
                + "WHERE booking_id = ? AND status IN ('PENDING', 'ACTIVE')";

        String updateRoomSql = "UPDATE ROOM "
                + "SET status = 'NEEDS_CLEANING', updated_at = SYSDATETIME() "
                + "WHERE room_id = ?";

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<AutoCheckoutBookingInfo> bookings = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setTimestamp(1, nowTs);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            bookings.add(new AutoCheckoutBookingInfo(
                                    rs.getLong("booking_id"),
                                    rs.getString("booking_code"),
                                    rs.getLong("room_id")
                            ));
                        }
                    }
                }

                if (!bookings.isEmpty()) {
                    try (PreparedStatement psBooking = conn.prepareStatement(updateBookingSql);
                         PreparedStatement psCred = conn.prepareStatement(expireCredSql);
                         PreparedStatement psRoom = conn.prepareStatement(updateRoomSql)) {

                        for (AutoCheckoutBookingInfo b : bookings) {
                            psBooking.setTimestamp(1, nowTs);
                            psBooking.setTimestamp(2, nowTs);
                            psBooking.setLong(3, b.bookingId);
                            psBooking.executeUpdate();

                            psCred.setLong(1, b.bookingId);
                            psCred.executeUpdate();

                            psRoom.setLong(1, b.roomId);
                            psRoom.executeUpdate();

                            LOGGER.info("Scheduler: Auto checkout completed for booking " + b.bookingCode);
                        }
                    }
                    LOGGER.info("Scheduler AutoCheckout checked out " + bookings.size() + " bookings.");
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Scheduler AutoCheckout transaction rolled back", e);
                throw e;
            }
        }
    }

    // 6. ExpireCredentials
    public static void expireCredentials() throws SQLException {
        Timestamp nowTs = util.ClockService.nowTimestamp();
        String sql = "UPDATE ACCESS_CREDENTIAL "
                + "SET status = 'EXPIRED' "
                + "WHERE status IN ('ACTIVE', 'PENDING') "
                + "  AND valid_until < ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, nowTs);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                LOGGER.info("Scheduler ExpireCredentials expired " + updated + " credentials.");
            }
        }
    }

    // 7. ExpireStaleAssignments
    public static void expireStaleAssignments() throws SQLException {
        Timestamp nowTs = util.ClockService.nowTimestamp();
        String selectSql = "SELECT sa.assignment_id, sa.room_id, r.room_number, u.full_name AS staff_name "
                + "FROM STAFF_ASSIGNMENT sa "
                + "JOIN ROOM r ON sa.room_id = r.room_id "
                + "LEFT JOIN APP_USER u ON sa.staff_id = u.user_id "
                + "WHERE sa.status = 'ASSIGNED' "
                + "  AND sa.shift_end < ?";

        String updateAssignmentSql = "UPDATE STAFF_ASSIGNMENT "
                + "SET status = 'EXPIRED', updated_at = SYSDATETIME() "
                + "WHERE assignment_id = ? AND status = 'ASSIGNED'";

        String insertAlertSql = "INSERT INTO ALERT_LOG (alert_type, severity, status, message, room_id, created_at) "
                + "SELECT 'CLEANING_SESSION_EXPIRED', 'MEDIUM', 'NEW', ?, ?, SYSDATETIME() "
                + "WHERE NOT EXISTS ( "
                + "    SELECT 1 FROM ALERT_LOG "
                + "    WHERE alert_type = 'CLEANING_SESSION_EXPIRED' AND message LIKE ? "
                + ")";

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<ExpiredSessionInfo> expiredAssignments = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setTimestamp(1, nowTs);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            expiredAssignments.add(new ExpiredSessionInfo(
                                    rs.getLong("assignment_id"),
                                    rs.getLong("room_id"),
                                    rs.getString("room_number"),
                                    rs.getString("staff_name")
                            ));
                        }
                    }
                }

                if (!expiredAssignments.isEmpty()) {
                    try (PreparedStatement psUpdate = conn.prepareStatement(updateAssignmentSql);
                         PreparedStatement psAlert = conn.prepareStatement(insertAlertSql)) {

                        for (ExpiredSessionInfo s : expiredAssignments) {
                            psUpdate.setLong(1, s.sessionId);
                            psUpdate.executeUpdate();

                            String alertMsg = "Cleaning assignment #" + s.sessionId + " expired for Room " + s.roomNumber
                                    + (s.staffName != null ? " (Staff: " + s.staffName + ")" : "");
                            String likePattern = "%#" + s.sessionId + " %";

                            psAlert.setString(1, alertMsg);
                            psAlert.setLong(2, s.roomId);
                            psAlert.setString(3, likePattern);
                            psAlert.executeUpdate();
                        }
                    }
                    LOGGER.info("Scheduler ExpireStaleAssignments expired " + expiredAssignments.size() + " assignments.");
                }

                conn.commit();
                if (!expiredAssignments.isEmpty()) {
                    announceAlerts("CLEANING_SESSION_EXPIRED");
                }
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Scheduler ExpireStaleAssignments transaction rolled back", e);
                throw e;
            }
        }
    }

    // 8. PurgeExpiredPinSecrets
    public static void purgeExpiredPinSecrets() throws SQLException {
        Timestamp nowTs = util.ClockService.nowTimestamp();
        String sql = "UPDATE ACCESS_CREDENTIAL "
                + "SET pin_ciphertext = NULL, "
                + "    pin_hmac = NULL, "
                + "    secret_purged_at = ? "
                + "WHERE status IN ('EXPIRED', 'REVOKED') "
                + "  AND valid_until < DATEADD(DAY, -7, ?) "
                + "  AND secret_purged_at IS NULL "
                + "  AND (pin_ciphertext IS NOT NULL OR pin_hmac IS NOT NULL)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, nowTs);
            ps.setTimestamp(2, nowTs);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                LOGGER.info("Scheduler PurgeExpiredPinSecrets purged secrets for " + updated + " expired credentials.");
            }
        }
    }

    // 9. DeviceOfflineSweep
    public static void deviceOfflineSweep() throws SQLException {
        boolean enabled = AppConstants.DEVICE_OFFLINE_SWEEP_ENABLED;
        if (!enabled) {
            LOGGER.fine("Scheduler DeviceOfflineSweep skipped (disabled by config DEVICE_OFFLINE_SWEEP_ENABLED=false).");
            return;
        }

        int afterSeconds = AppConstants.DEVICE_OFFLINE_AFTER_SECONDS;

        String selectSql = "SELECT device_id, device_name, room_id, zone_id "
                + "FROM DEVICE "
                + "WHERE status != 'FAULT' "
                + "  AND (last_seen_at IS NULL OR last_seen_at < DATEADD(SECOND, -?, SYSDATETIME()))";

        String updateDeviceSql = "UPDATE DEVICE "
                + "SET status = 'OFFLINE', updated_at = SYSDATETIME() "
                + "WHERE device_id = ? AND status = 'ONLINE'";

        String insertAlertSql = "INSERT INTO ALERT_LOG (alert_type, severity, status, message, device_id, room_id, zone_id, created_at) "
                + "SELECT 'DEVICE_OFFLINE', 'HIGH', 'NEW', ?, ?, ?, ?, SYSDATETIME() "
                + "WHERE NOT EXISTS ( "
                + "    SELECT 1 FROM ALERT_LOG "
                + "    WHERE alert_type = 'DEVICE_OFFLINE' "
                + "      AND device_id = ? "
                + "      AND status IN ('NEW', 'ACKNOWLEDGED') "
                + ")";

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<OfflineDeviceInfo> offlineDevices = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, afterSeconds);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            long rId = rs.getLong("room_id");
                            Long roomId = rs.wasNull() ? null : rId;
                            offlineDevices.add(new OfflineDeviceInfo(
                                    rs.getString("device_id"),
                                    rs.getString("device_name"),
                                    roomId,
                                    rs.getString("zone_id")
                            ));
                        }
                    }
                }

                if (!offlineDevices.isEmpty()) {
                    int alertsCreated = 0;
                    try (PreparedStatement psUpdate = conn.prepareStatement(updateDeviceSql);
                         PreparedStatement psAlert = conn.prepareStatement(insertAlertSql)) {

                        for (OfflineDeviceInfo d : offlineDevices) {
                            psUpdate.setString(1, d.deviceId);
                            psUpdate.executeUpdate();

                            String alertMsg = "Device " + d.deviceId + " (" + d.deviceName + ") is offline due to missing heartbeat";
                            psAlert.setString(1, alertMsg);
                            psAlert.setString(2, d.deviceId);
                            if (d.roomId != null) {
                                psAlert.setLong(3, d.roomId);
                            } else {
                                psAlert.setNull(3, Types.BIGINT);
                            }
                            if (d.zoneId != null) {
                                psAlert.setString(4, d.zoneId);
                            } else {
                                psAlert.setNull(4, Types.VARCHAR);
                            }
                            psAlert.setString(5, d.deviceId);
                            int created = psAlert.executeUpdate();
                            if (created > 0) {
                                alertsCreated++;
                            }
                        }
                    }
                    LOGGER.info("Scheduler DeviceOfflineSweep marked " + offlineDevices.size() + " devices OFFLINE, created " + alertsCreated + " new alerts.");
                }

                conn.commit();
                if (!offlineDevices.isEmpty()) {
                    announceAlerts("DEVICE_OFFLINE");
                }
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Scheduler DeviceOfflineSweep transaction rolled back", e);
                throw e;
            }
        }
    }

    // --- Tracking sets for automated notifications and buzzer triggers ---
    private static final java.util.Set<Long> sentCheckinMailBookings = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private static final java.util.Set<Long> sentCheckoutMailBookings = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private static final java.util.Set<Long> triggeredBuzzerBookings = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    /**
     * A reminder e-mail collected inside the job lock and sent after it is released.
     */
    public static final class PendingMail {
        final long bookingId;
        final boolean checkin;
        final String email;
        final String guestName;
        final String bookingCode;
        final String roomNumber;
        /** PIN for check-in reminders, raw checkout_at string for check-out reminders. */
        final String detail;

        PendingMail(long bookingId, boolean checkin, String email, String guestName,
                    String bookingCode, String roomNumber, String detail) {
            this.bookingId = bookingId;
            this.checkin = checkin;
            this.email = email;
            this.guestName = guestName;
            this.bookingCode = bookingCode;
            this.roomNumber = roomNumber;
            this.detail = detail;
        }
    }

    /**
     * Sends collected reminders. Call it OUTSIDE the job lock. A failed send is logged,
     * un-marked so the next cycle retries it, and never aborts the remaining mails.
     */
    public static void sendPendingMails(List<PendingMail> mails) {
        if (mails == null) {
            return;
        }
        for (PendingMail m : mails) {
            try {
                if (m.checkin) {
                    service.EmailService.sendCheckinReminder(m.email, m.guestName, m.bookingCode, m.roomNumber, m.detail);
                    LOGGER.info("Scheduler sent 30m check-in reminder for booking #" + m.bookingId);
                } else {
                    service.EmailService.sendCheckoutReminder(m.email, m.guestName, m.bookingCode, m.roomNumber, m.detail);
                    LOGGER.info("Scheduler sent 30m checkout reminder for booking #" + m.bookingId);
                }
            } catch (Exception mailEx) {
                (m.checkin ? sentCheckinMailBookings : sentCheckoutMailBookings).remove(m.bookingId);
                LOGGER.log(Level.WARNING, "Failed to send " + (m.checkin ? "check-in" : "checkout")
                        + " reminder for booking #" + m.bookingId + ": " + mailEx.getMessage());
            }
        }
    }

    /**
     * Automatically sends reminder email 30 minutes before check-in (including PIN).
     * Standalone variant: collects and sends in the caller's thread.
     */
    public static void checkUpcomingCheckinEmails() {
        sendPendingMails(collectCheckinReminders());
    }

    /**
     * Collects 30-minute check-in reminders (DB query + PIN decrypt) without sending them.
     * Each booking is claimed once so repeated cycles never produce duplicate mails.
     */
    public static List<PendingMail> collectCheckinReminders() {
        List<PendingMail> mails = new ArrayList<>();
        Timestamp nowTs = util.ClockService.nowTimestamp();
        String sql = "SELECT b.booking_id, b.booking_code, b.checkin_at, b.guest_id, "
                + "u.full_name, u.email, r.room_number, ac.credential_id "
                + "FROM BOOKING b "
                + "JOIN APP_USER u ON b.guest_id = u.user_id "
                + "JOIN ROOM r ON b.room_id = r.room_id "
                + "JOIN ACCESS_CREDENTIAL ac ON b.booking_id = ac.booking_id "
                + "WHERE b.status = 'CONFIRMED' "
                + "AND DATEDIFF(MINUTE, ?, b.checkin_at) BETWEEN 0 AND 30";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, nowTs);
            try (ResultSet rs = ps.executeQuery()) {
                service.CredentialService credService = new service.CredentialService();
                while (rs.next()) {
                    long bookingId = rs.getLong("booking_id");
                    if (sentCheckinMailBookings.add(bookingId)) {
                        try {
                            long credId = rs.getLong("credential_id");
                            long guestId = rs.getLong("guest_id");
                            String pin = credService.decryptPinForGuest(credId, guestId);
                            if (pin == null) {
                                pin = "******";
                            }

                            mails.add(new PendingMail(bookingId, true,
                                    rs.getString("email"),
                                    rs.getString("full_name"),
                                    rs.getString("booking_code"),
                                    rs.getString("room_number"),
                                    pin
                            ));
                        } catch (Exception mailEx) {
                            sentCheckinMailBookings.remove(bookingId);
                            LOGGER.log(Level.WARNING, "Failed to prepare check-in reminder for booking #" + bookingId + ": " + mailEx.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking upcoming check-in emails", e);
        }
        return mails;
    }

    /**
     * Automatically sends reminder email 30 minutes before check-out.
     * Standalone variant: collects and sends in the caller's thread.
     */
    public static void checkUpcomingCheckoutEmails() {
        sendPendingMails(collectCheckoutReminders());
    }

    /**
     * Collects 30-minute check-out reminders without sending them (claimed once per booking).
     */
    public static List<PendingMail> collectCheckoutReminders() {
        List<PendingMail> mails = new ArrayList<>();
        Timestamp nowTs = util.ClockService.nowTimestamp();
        String sql = "SELECT b.booking_id, b.booking_code, b.checkout_at, "
                + "u.full_name, u.email, r.room_number "
                + "FROM BOOKING b "
                + "JOIN APP_USER u ON b.guest_id = u.user_id "
                + "JOIN ROOM r ON b.room_id = r.room_id "
                + "WHERE b.status = 'CHECKED_IN' "
                + "AND DATEDIFF(MINUTE, ?, b.checkout_at) BETWEEN 0 AND 30";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, nowTs);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long bookingId = rs.getLong("booking_id");
                    if (sentCheckoutMailBookings.add(bookingId)) {
                        try {
                            Timestamp coTs = rs.getTimestamp("checkout_at");
                            String checkoutStr = (coTs != null) ? coTs.toString() : "";

                            mails.add(new PendingMail(bookingId, false,
                                    rs.getString("email"),
                                    rs.getString("full_name"),
                                    rs.getString("booking_code"),
                                    rs.getString("room_number"),
                                    checkoutStr
                            ));
                        } catch (Exception mailEx) {
                            sentCheckoutMailBookings.remove(bookingId);
                            LOGGER.log(Level.WARNING, "Failed to prepare checkout reminder for booking #" + bookingId + ": " + mailEx.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking upcoming checkout emails", e);
        }
        return mails;
    }

    /**
     * Silent overdue check-out handler (Loud buzzer has been disabled per requirements).
     */
    public static void checkOverdueCheckoutBuzzer() {
        Timestamp nowTs = util.ClockService.nowTimestamp();
        String sql = "SELECT b.booking_id, b.booking_code, b.room_id "
                + "FROM BOOKING b "
                + "WHERE b.status = 'CHECKED_IN' AND ? > b.checkout_at";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, nowTs);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long bookingId = rs.getLong("booking_id");
                    if (!triggeredBuzzerBookings.contains(bookingId)) {
                        // SILENT CHECKOUT: Loud buzzer disabled per requirements
                        triggeredBuzzerBookings.add(bookingId);
                        LOGGER.info("Overdue checkout for booking #" + bookingId + " (Silent notice - NO BUZZER)");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking overdue checkout buzzer", e);
        }
    }
}
