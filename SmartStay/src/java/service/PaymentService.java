package service;

import dao.PaymentDao;
import model.Payment;
import util.AppConstants;
import util.DBUtil;
import util.PageResult.PageRequest;
import util.PageResult;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing payment link generation, mock transactions, and the single processResult outcome entry point.
 */
public class PaymentService {

    private static final Logger LOGGER = Logger.getLogger(PaymentService.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CredentialService credentialService = new CredentialService();
    private final PaymentDao paymentDao = new PaymentDao();

    /**
     * Creates a payment attempt and returns a checkout URL for a guest's pending booking.
     * Validates booking existence, ownership, status, and hold deadlines under a row lock.
     */
    public String createPayment(long bookingId, long guestId, String ip)
            throws SQLException, ServiceException {

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Lock the booking
                String lockSql = "SELECT booking_id, booking_code, guest_id, total_amount, status, payment_deadline "
                        + "FROM BOOKING WITH (UPDLOCK, HOLDLOCK) WHERE booking_id = ?";

                String bookingCode;
                long totalAmount;
                String status;
                Timestamp deadlineTs;

                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setLong(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || rs.getLong("guest_id") != guestId) {
                            throw new ServiceException("NOT_FOUND", "Booking not found.");
                        }
                        bookingCode = rs.getString("booking_code");
                        totalAmount = rs.getBigDecimal("total_amount").longValue();
                        status = rs.getString("status");
                        deadlineTs = rs.getTimestamp("payment_deadline");
                    }
                }

                if (!"PENDING_PAYMENT".equalsIgnoreCase(status)) {
                    throw new ServiceException("INVALID_STATUS", "Booking is not in pending payment status.");
                }

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime deadline = (deadlineTs != null) ? deadlineTs.toLocalDateTime() : null;

                if (deadline != null && now.isAfter(deadline)) {
                    // Check if slots are still held and in future
                    String slotCheckSql = "SELECT COUNT(*) FROM BOOKING_SLOT WHERE booking_id = ? AND start_at > ? AND hold_status = 'HELD'";
                    String totalSlotsSql = "SELECT COUNT(*) FROM BOOKING_SLOT WHERE booking_id = ?";
                    int validHeld = 0;
                    int totalSlots = 0;

                    try (PreparedStatement ps = conn.prepareStatement(slotCheckSql)) {
                        ps.setLong(1, bookingId);
                        ps.setTimestamp(2, Timestamp.valueOf(now));
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) validHeld = rs.getInt(1);
                        }
                    }
                    try (PreparedStatement ps = conn.prepareStatement(totalSlotsSql)) {
                        ps.setLong(1, bookingId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) totalSlots = rs.getInt(1);
                        }
                    }

                    if (totalSlots > 0 && validHeld == totalSlots) {
                        // Slots still available, extend deadline
                        int holdMinutes = AppConstants.BOOKING_HOLD_MINUTES;
                        LocalDateTime newDeadline = now.plusMinutes(holdMinutes);
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE BOOKING SET payment_deadline = ?, updated_at = SYSDATETIME() WHERE booking_id = ?")) {
                            ps.setTimestamp(1, Timestamp.valueOf(newDeadline));
                            ps.setLong(2, bookingId);
                            ps.executeUpdate();
                        }
                    } else {
                        // Slots expired or passed
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE BOOKING SET status = 'EXPIRED', expire_reason = 'PAYMENT_TIMEOUT', updated_at = SYSDATETIME() WHERE booking_id = ?")) {
                            ps.setLong(1, bookingId);
                            ps.executeUpdate();
                        }
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE BOOKING_SLOT SET hold_status = 'RELEASED', released_at = SYSDATETIME() WHERE booking_id = ?")) {
                            ps.setLong(1, bookingId);
                            ps.executeUpdate();
                        }
                        conn.commit();
                        throw new ServiceException("HOLD_EXPIRED", "Selected slots are no longer available.");
                    }
                }

                // 2. Generate unique order code
                long orderCode = -1;
                for (int attempt = 0; attempt < 10; attempt++) {
                    long candidate = (System.currentTimeMillis() % 1000000000L) * 1000L + RANDOM.nextInt(1000);
                    String checkOrderSql = "SELECT 1 FROM PAYMENT WHERE provider_order_code = ?";
                    try (PreparedStatement ps = conn.prepareStatement(checkOrderSql)) {
                        ps.setLong(1, candidate);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                orderCode = candidate;
                                break;
                            }
                        }
                    }
                }
                if (orderCode <= 0) {
                    orderCode = Math.abs(RANDOM.nextLong() % 1000000000000L);
                }

                int ttlSeconds = AppConstants.PAYMENT_LINK_TTL_SECONDS;
                Timestamp expiresAt = Timestamp.valueOf(now.plusSeconds(ttlSeconds));

                String provider = AppConstants.PAYMENT_PROVIDER;
                String linkId = "mock_" + java.util.UUID.randomUUID().toString();
                String checkoutUrl = "/mock-payos/checkout?orderCode=" + orderCode;

                // 3. Insert PAYMENT row
                String insertPaySql = "INSERT INTO PAYMENT (booking_id, provider, provider_order_code, "
                        + "payment_link_id, amount, currency, status, expires_at) "
                        + "VALUES (?, 'MOCK', ?, ?, ?, 'VND', 'PENDING', ?)";

                long paymentId;
                try (PreparedStatement ps = conn.prepareStatement(insertPaySql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, bookingId);
                    ps.setLong(2, orderCode);
                    ps.setString(3, linkId);
                    ps.setBigDecimal(4, BigDecimal.valueOf(totalAmount));
                    ps.setTimestamp(5, expiresAt);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            paymentId = rs.getLong(1);
                        } else {
                            throw new SQLException("Failed to obtain generated payment_id.");
                        }
                    }
                }

                // 4. Audit
                LOGGER.info("Payment created for booking " + bookingCode + ", amount " + totalAmount + " VND");


                conn.commit();
                return checkoutUrl;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof ServiceException) {
                    throw (ServiceException) e;
                }
                if (e instanceof SQLException) {
                    throw (SQLException) e;
                }
                throw new SQLException("Failed to create payment", e);
            }
        }
    }

    /**
     * Single entry point for processing payment outcomes (mock simulator, webhooks, or callbacks).
     * Serializes execution under a row lock on PAYMENT. Idempotent and safe against replays and tampering.
     */
    public void processResult(long orderCode, long amount, String resultStatus, String transactionRef, String ip)
            throws SQLException {

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String selectSql = "SELECT p.payment_id, p.booking_id, p.provider_order_code, p.amount, p.status AS payment_status, "
                        + "p.expires_at, p.paid_at, "
                        + "b.booking_code, b.guest_id, b.room_id, b.status AS booking_status, b.total_amount, "
                        + "b.checkin_at, b.checkout_at "
                        + "FROM PAYMENT p WITH (UPDLOCK, HOLDLOCK) "
                        + "JOIN BOOKING b WITH (UPDLOCK, HOLDLOCK) ON p.booking_id = b.booking_id "
                        + "WHERE p.provider_order_code = ?";

                long paymentId = -1;
                long bookingId = -1;
                boolean confirmedAndPinIssued = false;
                long expectedAmount = -1;
                long bookingTotal = -1;
                String paymentStatus = null;
                String bookingStatus = null;
                Timestamp expiresAt = null;
                String bookingCode = null;
                long guestId = -1;
                LocalDateTime checkinAt = null;
                LocalDateTime checkoutAt = null;

                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setLong(1, orderCode);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            paymentId = rs.getLong("payment_id");
                            bookingId = rs.getLong("booking_id");
                            expectedAmount = rs.getBigDecimal("amount").longValue();
                            bookingTotal = rs.getBigDecimal("total_amount").longValue();
                            paymentStatus = rs.getString("payment_status");
                            bookingStatus = rs.getString("booking_status");
                            expiresAt = rs.getTimestamp("expires_at");
                            bookingCode = rs.getString("booking_code");
                            guestId = rs.getLong("guest_id");
                            Timestamp in = rs.getTimestamp("checkin_at");
                            if (in != null) checkinAt = in.toLocalDateTime();
                            Timestamp out = rs.getTimestamp("checkout_at");
                            if (out != null) checkoutAt = out.toLocalDateTime();
                        }
                    }
                }

                // 1. Unknown order code
                if (paymentId <= 0) {
                    LOGGER.warning("Unknown order code received in payment callback: " + orderCode);
                    conn.commit();
                    return;
                }

                // 2. Amount mismatch (tampering check)
                if (amount != expectedAmount || amount != bookingTotal) {
                    LOGGER.warning("Amount mismatch for order " + orderCode + ": received " + amount + " vs expected " + expectedAmount);
                    conn.commit();
                    return;
                }

                // 3. Duplicate callback (already paid)
                if ("PAID".equalsIgnoreCase(paymentStatus)) {
                    LOGGER.info("Duplicate callback for already paid order " + orderCode);
                    conn.commit();
                    return;
                }

                LocalDateTime now = LocalDateTime.now();

                // 4. Success outcome
                if ("SUCCESS".equalsIgnoreCase(resultStatus)) {
                    boolean unexpiredLink = (expiresAt != null && !now.isAfter(expiresAt.toLocalDateTime()));
                    boolean bookingPending = "PENDING_PAYMENT".equalsIgnoreCase(bookingStatus);

                    // Check if all slots still held
                    boolean slotsHeld = false;
                    if (bookingPending) {
                        String slotsCheck = "SELECT COUNT(*) FROM BOOKING_SLOT WHERE booking_id = ? AND hold_status <> 'HELD'";
                        try (PreparedStatement ps = conn.prepareStatement(slotsCheck)) {
                            ps.setLong(1, bookingId);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next() && rs.getInt(1) == 0) {
                                    slotsHeld = true;
                                }
                            }
                        }
                    }

                    if (unexpiredLink && bookingPending && slotsHeld) {
                        // Normal happy path: Confirm booking, mark payment PAID, issue PIN
                        String txRef = (transactionRef != null && !transactionRef.trim().isEmpty())
                                ? transactionRef.trim()
                                : ("TXN_" + orderCode);

                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE PAYMENT SET status = 'PAID', paid_at = SYSDATETIME(), transaction_reference = ?, updated_at = SYSDATETIME() WHERE payment_id = ?")) {
                            ps.setString(1, txRef);
                            ps.setLong(2, paymentId);
                            ps.executeUpdate();
                        }

                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE BOOKING SET status = 'CONFIRMED', updated_at = SYSDATETIME() WHERE booking_id = ?")) {
                            ps.setLong(1, bookingId);
                            ps.executeUpdate();
                        }

                        // Create PIN credential
                        String deviceId = AppConstants.MAIN_DOOR_DEVICE_ID;
                        credentialService.createGuestPin(conn, bookingId, deviceId, checkinAt, checkoutAt);
                        confirmedAndPinIssued = true;

                        LOGGER.info("Payment confirmed and guest PIN issued for booking " + bookingCode);

                    } else {
                        // Late payment or hold expired
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE PAYMENT SET status = 'REVIEW_REQUIRED', resolution_note = 'Payment received after link expired or booking hold expired', updated_at = SYSDATETIME() WHERE payment_id = ?")) {
                            ps.setLong(1, paymentId);
                            ps.executeUpdate();
                        }

                        // Create Admin Alert
                        String alertMsg = "Payment for booking " + bookingCode + " (order " + orderCode + ") requires review (late or expired hold).";
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO ALERT_LOG (alert_type, severity, status, message, booking_id) VALUES ('PAYMENT_REVIEW_REQUIRED', 'HIGH', 'NEW', ?, ?)")) {
                            ps.setString(1, alertMsg);
                            ps.setLong(2, bookingId);
                            ps.executeUpdate();
                        }

                        LOGGER.warning("Late payment for booking " + bookingCode + " flagged for review");
                    }

                } else {
                    // 5. Failure / cancel
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE PAYMENT SET status = 'FAILED', updated_at = SYSDATETIME() WHERE payment_id = ?")) {
                        ps.setLong(1, paymentId);
                        ps.executeUpdate();
                    }

                    LOGGER.info("Payment failed for booking " + bookingCode);
                }

                conn.commit();

                // Broadcast WebSocket event for real-time checkout confirmation & auto-redirect
                try {
                    Map<String, Object> evData = new java.util.HashMap<>();
                    evData.put("bookingId", bookingId);
                    evData.put("bookingCode", bookingCode);
                    evData.put("orderCode", orderCode);
                    if (confirmedAndPinIssued) {
                        evData.put("status", "PAID");
                        evData.put("redirectUrl", "/guest/payment/result?bookingId=" + bookingId);
                        websocket.RealtimeWebSocketEndpoint.broadcastEvent("PAYMENT_CONFIRMED", evData);
                    } else if (!"SUCCESS".equalsIgnoreCase(resultStatus)) {
                        evData.put("status", "FAILED");
                        evData.put("redirectUrl", "/guest/payment/result?bookingId=" + bookingId);
                        websocket.RealtimeWebSocketEndpoint.broadcastEvent("PAYMENT_FAILED", evData);
                    } else {
                        evData.put("status", "REVIEW_REQUIRED");
                        evData.put("redirectUrl", "/guest/payment/result?bookingId=" + bookingId);
                        websocket.RealtimeWebSocketEndpoint.broadcastEvent("PAYMENT_UPDATED", evData);
                    }
                } catch (Exception wsEx) {
                    LOGGER.log(Level.WARNING, "Failed to broadcast payment websocket event", wsEx);
                }

                // Trigger booking confirmation email with PIN asynchronously if payment succeeded
                if (confirmedAndPinIssued) {
                    sendConfirmationEmailAsync(bookingId);
                }
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Database error in processResult for orderCode " + orderCode, e);
                if (e instanceof SQLException) {
                    throw (SQLException) e;
                }
                throw new SQLException("Failed to process payment result", e);
            }
        }
    }

    /**
     * Delegates to PaymentDao to retrieve paginated payment records.
     */
    public PageResult<Payment> findPage(PageRequest pr) throws SQLException {
        return paymentDao.findPage(pr);
    }

    /**
     * Resolves a flagged payment (such as REVIEW_REQUIRED) with administrative action and note.
     * action: 'refund_pending' or 'dismiss'.
     */
    public void resolvePayment(long adminUserId, long paymentId, String action, String note, String ip)
            throws SQLException, ServiceException {

        if (note == null || note.trim().isEmpty()) {
            throw new ServiceException("NOTE_REQUIRED", "Resolution note is required.");
        }
        String cleanNote = note.trim();
        if (cleanNote.length() > 500) {
            cleanNote = cleanNote.substring(0, 500);
        }

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String lockSql = "SELECT p.payment_id, p.booking_id, b.booking_code, p.status, p.amount "
                        + "FROM PAYMENT p WITH (UPDLOCK, HOLDLOCK) "
                        + "JOIN BOOKING b ON p.booking_id = b.booking_id "
                        + "WHERE p.payment_id = ?";

                String bookingCode;
                long bookingId;
                String currentStatus;

                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setLong(1, paymentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new ServiceException("NOT_FOUND", "Payment record not found.");
                        }
                        bookingId = rs.getLong("booking_id");
                        bookingCode = rs.getString("booking_code");
                        currentStatus = rs.getString("status");
                    }
                }

                if (!"REVIEW_REQUIRED".equalsIgnoreCase(currentStatus) && !"FAILED".equalsIgnoreCase(currentStatus) && !"PAID".equalsIgnoreCase(currentStatus)) {
                    throw new ServiceException("INVALID_STATUS", "Only payments requiring review can be resolved.");
                }

                if ("refund_pending".equalsIgnoreCase(action)) {
                    String updateSql = "UPDATE PAYMENT SET status = 'REFUND_PENDING', resolution_note = ?, "
                            + "resolved_by = ?, resolved_at = SYSDATETIME(), updated_at = SYSDATETIME() "
                            + "WHERE payment_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, cleanNote);
                        ps.setLong(2, adminUserId);
                        ps.setLong(3, paymentId);
                        ps.executeUpdate();
                    }

                    LOGGER.info("Payment #" + paymentId + " (Booking " + bookingCode + ") marked refund pending by admin " + adminUserId + ": " + cleanNote);

                } else if ("dismiss".equalsIgnoreCase(action)) {
                    String updateSql = "UPDATE PAYMENT SET resolution_note = ?, "
                            + "resolved_by = ?, resolved_at = SYSDATETIME(), updated_at = SYSDATETIME() "
                            + "WHERE payment_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, cleanNote);
                        ps.setLong(2, adminUserId);
                        ps.setLong(3, paymentId);
                        ps.executeUpdate();
                    }

                    LOGGER.info("Payment #" + paymentId + " (Booking " + bookingCode + ") review dismissed by admin " + adminUserId + ": " + cleanNote);

                } else {
                    throw new ServiceException("INVALID_ACTION", "Unrecognized resolution action: " + action);
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Database error resolving payment " + paymentId, e);
                throw e;
            } catch (ServiceException pe) {
                conn.rollback();
                throw pe;
            } catch (Exception ex) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Unexpected error resolving payment " + paymentId, ex);
                throw new SQLException("Payment resolution failed unexpectedly.", ex);
            }
        }
    }

    /**
     * Sends booking confirmation email with PIN to the guest asynchronously.
     */
    private void sendConfirmationEmailAsync(long bookingId) {
        new Thread(() -> {
            try (Connection conn = DBUtil.getConnection()) {
                String sql = "SELECT b.booking_code, b.checkin_at, b.checkout_at, b.guest_id, "
                        + "u.full_name, u.email, r.room_number, ac.credential_id "
                        + "FROM BOOKING b "
                        + "JOIN APP_USER u ON b.guest_id = u.user_id "
                        + "JOIN ROOM r ON b.room_id = r.room_id "
                        + "JOIN ACCESS_CREDENTIAL ac ON b.booking_id = ac.booking_id "
                        + "WHERE b.booking_id = ? "
                        + "ORDER BY ac.credential_id DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String bookingCode = rs.getString("booking_code");
                            String guestName = rs.getString("full_name");
                            String guestEmail = rs.getString("email");
                            String roomNumber = rs.getString("room_number");
                            String checkin = rs.getTimestamp("checkin_at").toString();
                            String checkout = rs.getTimestamp("checkout_at").toString();
                            long credId = rs.getLong("credential_id");
                            long guestId = rs.getLong("guest_id");

                            String pin = credentialService.decryptPinForGuest(credId, guestId);
                            if (pin == null) {
                                pin = "******";
                            }

                            EmailService.sendBookingSuccess(guestEmail, guestName, bookingCode, roomNumber, pin, checkin, checkout);
                            LOGGER.info("Confirmation email sent to " + guestEmail + " for booking " + bookingCode);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to send booking confirmation email for booking #" + bookingId, e);
            }
        }).start();
    }
}
