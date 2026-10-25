package dao;

import model.Booking;
import model.BookingDetail;
import model.Payment;
import model.Room;
import util.DBUtil;
import util.PageResult.PageRequest;
import util.PageResult;
import util.SqlUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data access object for the BOOKING and BOOKING_SLOT tables.
 */
public class BookingDao {

    public static final Set<String> ADMIN_SORT_WHITELIST = new HashSet<>(Arrays.asList(
            "date", "created", "total", "status", "room", "guest"
    ));

    private static final Set<String> ALLOWED_STATUSES = new HashSet<>(Arrays.asList(
            "PENDING_PAYMENT", "CONFIRMED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED", "EXPIRED"
    ));

    public BookingDao() {
    }

    /**
     * Finds a paginated list of bookings belonging to a specific guest.
     */
    public PageResult<Booking> findPageByGuest(PageRequest pr, long guestId) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE b.guest_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(guestId);

        // Filter: status (exact match, verified against allowed statuses)
        String status = pr.getFilter("status");
        if (!status.isEmpty() && ALLOWED_STATUSES.contains(status.toUpperCase())) {
            where.append(" AND b.status = ? ");
            params.add(status.toUpperCase());
        }

        // Filter: room (room number LIKE)
        String room = pr.getFilter("room");
        if (!room.isEmpty()) {
            where.append(" AND r.room_number LIKE ? ESCAPE '!' ");
            params.add("%" + SqlUtil.escapeLike(room) + "%");
        }

        // Filter: from (slot_date >= ?)
        String from = pr.getFilter("from");
        if (!from.isEmpty()) {
            try {
                LocalDate fDate = LocalDate.parse(from);
                where.append(" AND b.slot_date >= ? ");
                params.add(Date.valueOf(fDate));
            } catch (DateTimeParseException ignored) {
            }
        }

        // Filter: to (slot_date <= ?)
        String to = pr.getFilter("to");
        if (!to.isEmpty()) {
            try {
                LocalDate tDate = LocalDate.parse(to);
                where.append(" AND b.slot_date <= ? ");
                params.add(Date.valueOf(tDate));
            } catch (DateTimeParseException ignored) {
            }
        }

        // Filter: keyword (booking_code LIKE)
        String keyword = pr.getKeyword();
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND b.booking_code LIKE ? ESCAPE '!' ");
            params.add("%" + SqlUtil.escapeLike(keyword.trim()) + "%");
        }

        // 1. Count query
        String countSql = "SELECT COUNT(*) FROM BOOKING b JOIN ROOM r ON b.room_id = r.room_id " + where;
        int total = 0;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }
        }

        pr.clampPage(total);

        if (total == 0) {
            return new PageResult<>(Collections.<Booking>emptyList(), 0, pr);
        }

        // Whitelist sort column mapping
        String sortCol;
        String reqSort = (pr.getSort() != null) ? pr.getSort().toLowerCase() : "date";
        switch (reqSort) {
            case "created":
                sortCol = "b.created_at";
                break;
            case "total":
                sortCol = "b.total_amount";
                break;
            case "status":
                sortCol = "b.status";
                break;
            case "room":
                sortCol = "r.room_number";
                break;
            case "date":
            default:
                sortCol = "b.slot_date";
                break;
        }

        String dir = "DESC".equalsIgnoreCase(pr.getDir()) ? "DESC" : "ASC";
        String orderByClause = " ORDER BY " + sortCol + " " + dir + ", b.booking_id DESC ";

        String selectSql = "SELECT b.booking_id, b.booking_code, b.guest_id, b.room_id, r.room_number, "
                + "b.slot_date, b.checkin_at, b.checkout_at, b.subtotal_amount, b.discount_percent, "
                + "b.discount_amount, b.total_amount, b.status, b.allow_early_checkin, b.expire_reason, b.payment_deadline, "
                + "b.note, b.cancel_reason, b.cancelled_at, b.cancelled_by, b.created_at, "
                + "COALESCE(p.status, 'NONE') AS payment_status, "
                + "STUFF((SELECT '+' + rts.slot_code FROM BOOKING_SLOT bs "
                + "       JOIN ROOM_TIME_SLOT rts ON bs.room_slot_id = rts.room_slot_id "
                + "       WHERE bs.booking_id = b.booking_id ORDER BY rts.sort_order "
                + "       FOR XML PATH('')), 1, 1, '') AS slots_summary "
                + "FROM BOOKING b "
                + "JOIN ROOM r ON b.room_id = r.room_id "
                + "OUTER APPLY (SELECT TOP 1 status FROM PAYMENT WHERE booking_id = b.booking_id ORDER BY payment_id DESC) p "
                + where
                + orderByClause
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<Booking> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            int pIndex = 1;
            for (Object p : params) {
                ps.setObject(pIndex++, p);
            }
            ps.setInt(pIndex++, pr.getOffset());
            ps.setInt(pIndex, pr.getSize());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBooking(rs));
                }
            }
        }

        return new PageResult<>(list, total, pr);
    }

    /**
     * Loads a single booking by ID with joined room number and payment status.
     */
    public Booking findById(long bookingId) throws SQLException {
        String sql = "SELECT b.booking_id, b.booking_code, b.guest_id, b.room_id, r.room_number, "
                + "b.slot_date, b.checkin_at, b.checkout_at, b.subtotal_amount, b.discount_percent, "
                + "b.discount_amount, b.total_amount, b.status, b.allow_early_checkin, b.expire_reason, b.payment_deadline, "
                + "b.note, b.cancel_reason, b.cancelled_at, b.cancelled_by, b.created_at, "
                + "COALESCE(p.status, 'NONE') AS payment_status, "
                + "STUFF((SELECT '+' + rts.slot_code FROM BOOKING_SLOT bs "
                + "       JOIN ROOM_TIME_SLOT rts ON bs.room_slot_id = rts.room_slot_id "
                + "       WHERE bs.booking_id = b.booking_id ORDER BY rts.sort_order "
                + "       FOR XML PATH('')), 1, 1, '') AS slots_summary "
                + "FROM BOOKING b "
                + "JOIN ROOM r ON b.room_id = r.room_id "
                + "OUTER APPLY (SELECT TOP 1 status FROM PAYMENT WHERE booking_id = b.booking_id ORDER BY payment_id DESC) p "
                + "WHERE b.booking_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBooking(rs);
                }
            }
        }
        return null;
    }

    /**
     * Loads comprehensive booking details including booked slots and room information.
     */
    public BookingDetail findDetail(long bookingId) throws SQLException {
        Booking booking = findById(bookingId);
        if (booking == null) {
            return null;
        }

        BookingDetail detail = new BookingDetail();
        detail.setBooking(booking);

        // Load Room
        String roomSql = "SELECT room_id, room_number, floor_number, location_description, "
                + "capacity, description, amenities, image_url, status, is_active "
                + "FROM ROOM WHERE room_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(roomSql)) {
            ps.setLong(1, booking.getRoomId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Room r = new Room();
                    r.setRoomId(rs.getLong("room_id"));
                    r.setRoomNumber(rs.getString("room_number"));
                    r.setFloorNumber(rs.getInt("floor_number"));
                    r.setLocationDescription(rs.getString("location_description"));
                    r.setCapacity(rs.getInt("capacity"));
                    r.setDescription(rs.getString("description"));
                    r.setAmenities(rs.getString("amenities"));
                    r.setImageUrl(rs.getString("image_url"));
                    r.setStatus(rs.getString("status"));
                    r.setActive(rs.getBoolean("is_active"));
                    detail.setRoom(r);
                }
            }
        }

        // Load Slots
        String slotsSql = "SELECT bs.booking_slot_id, bs.room_slot_id, "
                + "COALESCE(rts.slot_code, 'S' + CAST(COALESCE(rts.sort_order, 1) AS VARCHAR(5))) AS slot_code, "
                + "COALESCE(rts.slot_name, 'Slot ' + CAST(COALESCE(rts.sort_order, 1) AS VARCHAR(5))) AS slot_name, "
                + "rts.start_time, rts.end_time, rts.crosses_midnight, "
                + "bs.start_at, bs.end_at, bs.list_price, bs.hold_status "
                + "FROM BOOKING_SLOT bs "
                + "LEFT JOIN ROOM_TIME_SLOT rts ON bs.room_slot_id = rts.room_slot_id "
                + "WHERE bs.booking_id = ? "
                + "ORDER BY COALESCE(rts.sort_order, 99) ASC, bs.start_at ASC";

        List<BookingDetail.BookingSlotRow> slotRows = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(slotsSql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookingDetail.BookingSlotRow row = new BookingDetail.BookingSlotRow();
                    row.setBookingSlotId(rs.getLong("booking_slot_id"));
                    row.setRoomSlotId(rs.getLong("room_slot_id"));
                    row.setSlotCode(rs.getString("slot_code"));
                    row.setSlotName(rs.getString("slot_name"));

                    Timestamp st = rs.getTimestamp("start_at");
                    if (st != null) {
                        row.setStartAt(st.toLocalDateTime());
                    }
                    Timestamp et = rs.getTimestamp("end_at");
                    if (et != null) {
                        row.setEndAt(et.toLocalDateTime());
                    }

                    BigDecimal priceBd = rs.getBigDecimal("list_price");
                    row.setListPrice(priceBd != null ? priceBd.longValueExact() : 0L);
                    row.setHoldStatus(rs.getString("hold_status"));

                    slotRows.add(row);
                }
            }
        }
        detail.setSlots(slotRows);

        // Server-side compute canCancel
        LocalDateTime now = LocalDateTime.now();
        boolean cancellable = (Booking.STATUS_PENDING_PAYMENT.equalsIgnoreCase(booking.getStatus())
                || Booking.STATUS_CONFIRMED.equalsIgnoreCase(booking.getStatus()))
                && booking.getCheckinAt() != null
                && now.isBefore(booking.getCheckinAt());
        detail.setCanCancel(cancellable);

        return detail;
    }

    /**
     * Finds a paginated list of bookings for administrative management.
     */
    public PageResult<Booking> findPageAdmin(PageRequest pr) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        // Filter: status
        String status = pr.getFilter("status");
        if (!status.isEmpty() && ALLOWED_STATUSES.contains(status.toUpperCase())) {
            where.append(" AND b.status = ? ");
            params.add(status.toUpperCase());
        }

        // Filter: payment_status (via outer apply or subquery)
        String paymentStatus = pr.getFilter("payment_status");
        if (!paymentStatus.isEmpty()) {
            where.append(" AND p.status = ? ");
            params.add(paymentStatus.toUpperCase());
        }

        // Filter: room (room number LIKE or room id)
        String room = pr.getFilter("room");
        if (!room.isEmpty()) {
            where.append(" AND (r.room_number LIKE ? ESCAPE '!' OR CAST(b.room_id AS VARCHAR(20)) = ?) ");
            params.add("%" + SqlUtil.escapeLike(room) + "%");
            params.add(room);
        }

        // Filter: from (slot_date >= ?)
        String from = pr.getFilter("from");
        if (!from.isEmpty()) {
            try {
                LocalDate fDate = LocalDate.parse(from);
                where.append(" AND b.slot_date >= ? ");
                params.add(Date.valueOf(fDate));
            } catch (DateTimeParseException ignored) {
            }
        }

        // Filter: to (slot_date <= ?)
        String to = pr.getFilter("to");
        if (!to.isEmpty()) {
            try {
                LocalDate tDate = LocalDate.parse(to);
                where.append(" AND b.slot_date <= ? ");
                params.add(Date.valueOf(tDate));
            } catch (DateTimeParseException ignored) {
            }
        }

        // Filter: noshow_held (EXPIRED + NO_SHOW + exists HELD slot)
        String noshowHeld = pr.getFilter("noshow_held");
        String flag = pr.getFilter("flag");
        if ("true".equalsIgnoreCase(noshowHeld) || "1".equals(noshowHeld) || "on".equalsIgnoreCase(noshowHeld)
                || "noshow_held".equalsIgnoreCase(flag)) {
            where.append(" AND b.status = 'EXPIRED' AND b.expire_reason = 'NO_SHOW' "
                    + "AND EXISTS (SELECT 1 FROM BOOKING_SLOT bs WHERE bs.booking_id = b.booking_id AND bs.hold_status = 'HELD') ");
        }

        // Filter: keyword (booking_code, guest name, guest email, room number)
        String keyword = pr.getKeyword();
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (b.booking_code LIKE ? ESCAPE '!' OR u.full_name LIKE ? ESCAPE '!' "
                    + "OR u.email LIKE ? ESCAPE '!' OR r.room_number LIKE ? ESCAPE '!') ");
            String escaped = "%" + SqlUtil.escapeLike(keyword.trim()) + "%";
            params.add(escaped);
            params.add(escaped);
            params.add(escaped);
            params.add(escaped);
        }

        // 1. Count query
        String countSql = "SELECT COUNT(*) FROM BOOKING b "
                + "JOIN ROOM r ON b.room_id = r.room_id "
                + "JOIN APP_USER u ON b.guest_id = u.user_id "
                + "OUTER APPLY (SELECT TOP 1 status FROM PAYMENT WHERE booking_id = b.booking_id ORDER BY payment_id DESC) p "
                + where;

        int total = 0;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }
        }

        pr.clampPage(total);

        if (total == 0) {
            return new PageResult<>(Collections.<Booking>emptyList(), 0, pr);
        }

        // Whitelist sort
        String sortCol = "b.created_at";
        String reqSort = (pr.getSort() != null) ? pr.getSort().toLowerCase() : "created";
        switch (reqSort) {
            case "date":
                sortCol = "b.slot_date";
                break;
            case "total":
                sortCol = "b.total_amount";
                break;
            case "status":
                sortCol = "b.status";
                break;
            case "room":
                sortCol = "r.room_number";
                break;
            case "guest":
                sortCol = "u.full_name";
                break;
            case "created":
            default:
                sortCol = "b.created_at";
                break;
        }

        String dir = "ASC".equalsIgnoreCase(pr.getDir()) ? "ASC" : "DESC";
        String orderByClause = " ORDER BY " + sortCol + " " + dir + ", b.booking_id DESC ";

        String selectSql = "SELECT b.booking_id, b.booking_code, b.guest_id, b.room_id, r.room_number, "
                + "u.full_name AS guest_name, u.email AS guest_email, u.phone AS guest_phone, "
                + "b.slot_date, b.checkin_at, b.checkout_at, b.actual_checkin_at, b.actual_checkout_at, "
                + "b.subtotal_amount, b.discount_percent, b.discount_amount, b.total_amount, "
                + "b.status, b.allow_early_checkin, b.expire_reason, b.payment_deadline, b.note, b.cancel_reason, "
                + "b.cancelled_at, b.cancelled_by, b.created_by, c.full_name AS creator_name, b.created_at, "
                + "COALESCE(p.status, 'NONE') AS payment_status, "
                + "CASE WHEN EXISTS (SELECT 1 FROM BOOKING_SLOT bs WHERE bs.booking_id = b.booking_id AND bs.hold_status = 'HELD') THEN 1 ELSE 0 END AS has_held_slots, "
                + "STUFF((SELECT '+' + rts.slot_code FROM BOOKING_SLOT bs "
                + "       JOIN ROOM_TIME_SLOT rts ON bs.room_slot_id = rts.room_slot_id "
                + "       WHERE bs.booking_id = b.booking_id ORDER BY rts.sort_order "
                + "       FOR XML PATH('')), 1, 1, '') AS slots_summary "
                + "FROM BOOKING b "
                + "JOIN ROOM r ON b.room_id = r.room_id "
                + "JOIN APP_USER u ON b.guest_id = u.user_id "
                + "LEFT JOIN APP_USER c ON b.created_by = c.user_id "
                + "OUTER APPLY (SELECT TOP 1 status FROM PAYMENT WHERE booking_id = b.booking_id ORDER BY payment_id DESC) p "
                + where
                + orderByClause
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<Booking> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            int pIndex = 1;
            for (Object p : params) {
                ps.setObject(pIndex++, p);
            }
            ps.setInt(pIndex++, pr.getOffset());
            ps.setInt(pIndex, pr.getSize());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBooking(rs));
                }
            }
        }

        return new PageResult<>(list, total, pr);
    }

    /**
     * Finds booking detail including payments, credential metadata, access logs, and alerts.
     */
    public BookingDetail findDetailAdmin(long bookingId) throws SQLException {
        BookingDetail detail = findDetail(bookingId);
        if (detail == null) {
            return null;
        }

        // 1. Payments
        PaymentDao paymentDao = new PaymentDao();
        detail.setPayments(paymentDao.findByBookingId(bookingId));

        // 2. Credentials metadata
        String credSql = "SELECT credential_id, credential_type, device_id, status, "
                + "valid_from, valid_until, used_count, revoked_at, revoke_reason "
                + "FROM ACCESS_CREDENTIAL WHERE booking_id = ? ORDER BY credential_id DESC";
        List<BookingDetail.CredentialMeta> credList = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(credSql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookingDetail.CredentialMeta cm = new BookingDetail.CredentialMeta();
                    cm.setCredentialId(rs.getLong("credential_id"));
                    cm.setCredentialType(rs.getString("credential_type"));
                    cm.setDeviceId(rs.getString("device_id"));
                    cm.setStatus(rs.getString("status"));
                    Timestamp vf = rs.getTimestamp("valid_from");
                    if (vf != null) cm.setValidFrom(vf.toLocalDateTime());
                    Timestamp vu = rs.getTimestamp("valid_until");
                    if (vu != null) cm.setValidUntil(vu.toLocalDateTime());
                    cm.setUsedCount(rs.getInt("used_count"));
                    Timestamp ra = rs.getTimestamp("revoked_at");
                    if (ra != null) cm.setRevokedAt(ra.toLocalDateTime());
                    cm.setRevokeReason(rs.getString("revoke_reason"));
                    credList.add(cm);
                }
            }
        }
        detail.setCredentials(credList);

        // 3. Access logs (latest 20)
        String logSql = "SELECT TOP 20 al.access_log_id, al.device_id, al.request_id, al.attempted_at, "
                + "al.result, al.reason_code, ac.credential_id "
                + "FROM ACCESS_LOG al "
                + "JOIN ACCESS_CREDENTIAL ac ON al.credential_id = ac.credential_id "
                + "WHERE ac.booking_id = ? "
                + "ORDER BY al.attempted_at DESC";
        List<BookingDetail.AccessLogRow> logList = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(logSql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookingDetail.AccessLogRow al = new BookingDetail.AccessLogRow();
                    al.setAccessLogId(rs.getLong("access_log_id"));
                    al.setDeviceId(rs.getString("device_id"));
                    al.setRequestId(rs.getString("request_id"));
                    Timestamp att = rs.getTimestamp("attempted_at");
                    if (att != null) al.setAttemptedAt(att.toLocalDateTime());
                    al.setResult(rs.getString("result"));
                    al.setReasonCode(rs.getString("reason_code"));
                    al.setCredentialId(rs.getLong("credential_id"));
                    logList.add(al);
                }
            }
        }
        detail.setAccessLogs(logList);

        // 4. Alert logs
        String alertSql = "SELECT alert_id, alert_type, severity, status, message, created_at "
                + "FROM ALERT_LOG WHERE booking_id = ? ORDER BY alert_id DESC";
        List<BookingDetail.AlertLogRow> alertList = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(alertSql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookingDetail.AlertLogRow ar = new BookingDetail.AlertLogRow();
                    ar.setAlertId(rs.getLong("alert_id"));
                    ar.setAlertType(rs.getString("alert_type"));
                    ar.setSeverity(rs.getString("severity"));
                    ar.setStatus(rs.getString("status"));
                    ar.setMessage(rs.getString("message"));
                    Timestamp ca = rs.getTimestamp("created_at");
                    if (ca != null) ar.setCreatedAt(ca.toLocalDateTime());
                    alertList.add(ar);
                }
            }
        }
        detail.setAlertLogs(alertList);

        return detail;
    }

    private Booking mapBooking(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setBookingId(rs.getLong("booking_id"));
        b.setBookingCode(rs.getString("booking_code"));
        b.setGuestId(rs.getLong("guest_id"));
        b.setRoomId(rs.getLong("room_id"));
        b.setRoomNumber(rs.getString("room_number"));

        Date d = rs.getDate("slot_date");
        if (d != null) {
            b.setSlotDate(d.toLocalDate());
        }

        Timestamp ci = rs.getTimestamp("checkin_at");
        if (ci != null) {
            b.setCheckinAt(ci.toLocalDateTime());
        }

        Timestamp co = rs.getTimestamp("checkout_at");
        if (co != null) {
            b.setCheckoutAt(co.toLocalDateTime());
        }

        BigDecimal subtotalBd = rs.getBigDecimal("subtotal_amount");
        b.setSubtotal(subtotalBd != null ? subtotalBd.longValueExact() : 0L);

        b.setDiscountPercent(rs.getInt("discount_percent"));

        BigDecimal discAmtBd = rs.getBigDecimal("discount_amount");
        b.setDiscountAmount(discAmtBd != null ? discAmtBd.longValueExact() : 0L);

        BigDecimal totalBd = rs.getBigDecimal("total_amount");
        b.setTotal(totalBd != null ? totalBd.longValueExact() : 0L);

        b.setStatus(rs.getString("status"));
        b.setExpireReason(rs.getString("expire_reason"));

        Timestamp pd = rs.getTimestamp("payment_deadline");
        if (pd != null) {
            b.setPaymentDeadline(pd.toLocalDateTime());
        }

        b.setNote(rs.getString("note"));
        b.setCancelReason(rs.getString("cancel_reason"));

        Timestamp cld = rs.getTimestamp("cancelled_at");
        if (cld != null) {
            b.setCancelledAt(cld.toLocalDateTime());
        }

        long cb = rs.getLong("cancelled_by");
        if (!rs.wasNull()) {
            b.setCancelledBy(cb);
        }

        b.setCreatedAt(rs.getTimestamp("created_at"));
        b.setPaymentStatus(rs.getString("payment_status"));

        String rawSlots = rs.getString("slots_summary");
        if (rawSlots != null && !rawSlots.trim().isEmpty()) {
            if (b.getCheckinAt() != null && b.getCheckoutAt() != null) {
                String times = String.format("%02d:%02d\u2013%02d:%02d",
                        b.getCheckinAt().getHour(), b.getCheckinAt().getMinute(),
                        b.getCheckoutAt().getHour(), b.getCheckoutAt().getMinute());
                b.setSlotsSummary(rawSlots.trim() + " · " + times);
            } else {
                b.setSlotsSummary(rawSlots.trim());
            }
        } else {
            b.setSlotsSummary("");
        }

        if (hasColumn(rs, "guest_name")) {
            b.setGuestName(rs.getString("guest_name"));
        }
        if (hasColumn(rs, "guest_email")) {
            b.setGuestEmail(rs.getString("guest_email"));
        }
        if (hasColumn(rs, "guest_phone")) {
            b.setGuestPhone(rs.getString("guest_phone"));
        }
        if (hasColumn(rs, "actual_checkin_at")) {
            Timestamp aci = rs.getTimestamp("actual_checkin_at");
            if (aci != null) b.setActualCheckinAt(aci.toLocalDateTime());
        }
        if (hasColumn(rs, "actual_checkout_at")) {
            Timestamp aco = rs.getTimestamp("actual_checkout_at");
            if (aco != null) b.setActualCheckoutAt(aco.toLocalDateTime());
        }
        if (hasColumn(rs, "created_by")) {
            long crBy = rs.getLong("created_by");
            if (!rs.wasNull()) b.setCreatedBy(crBy);
        }
        if (hasColumn(rs, "creator_name")) {
            b.setCreatedByName(rs.getString("creator_name"));
        }
        if (hasColumn(rs, "has_held_slots")) {
            b.setHasHeldSlots(rs.getInt("has_held_slots") == 1);
        }
        if (hasColumn(rs, "allow_early_checkin")) {
            b.setAllowEarlyCheckin(rs.getBoolean("allow_early_checkin"));
        }

        return b;
    }

    public boolean updateAllowEarlyCheckin(long bookingId, boolean allowEarlyCheckin) throws SQLException {
        String sql = "UPDATE BOOKING SET allow_early_checkin = ?, updated_at = SYSDATETIME() WHERE booking_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, allowEarlyCheckin);
            ps.setLong(2, bookingId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves aggregated booking counts for administrative status tabs.
     */
    public java.util.Map<String, Integer> getStatusCounts() throws SQLException {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        counts.put("ALL", 0);
        counts.put("PENDING_PAYMENT", 0);
        counts.put("CONFIRMED", 0);
        counts.put("CHECKED_IN", 0);
        counts.put("CHECKED_OUT", 0);
        counts.put("NOSHOW_HELD", 0);

        String sql = "SELECT "
                + "COUNT(*) AS total_all, "
                + "SUM(CASE WHEN b.status = 'PENDING_PAYMENT' THEN 1 ELSE 0 END) AS pending_payment, "
                + "SUM(CASE WHEN b.status = 'CONFIRMED' THEN 1 ELSE 0 END) AS confirmed, "
                + "SUM(CASE WHEN b.status = 'CHECKED_IN' THEN 1 ELSE 0 END) AS checked_in, "
                + "SUM(CASE WHEN b.status = 'CHECKED_OUT' THEN 1 ELSE 0 END) AS checked_out, "
                + "SUM(CASE WHEN b.status = 'EXPIRED' AND b.expire_reason = 'NO_SHOW' "
                + "         AND EXISTS (SELECT 1 FROM BOOKING_SLOT bs WHERE bs.booking_id = b.booking_id AND bs.hold_status = 'HELD') "
                + "         THEN 1 ELSE 0 END) AS noshow_held "
                + "FROM BOOKING b";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                counts.put("ALL", rs.getInt("total_all"));
                counts.put("PENDING_PAYMENT", rs.getInt("pending_payment"));
                counts.put("CONFIRMED", rs.getInt("confirmed"));
                counts.put("CHECKED_IN", rs.getInt("checked_in"));
                counts.put("CHECKED_OUT", rs.getInt("checked_out"));
                counts.put("NOSHOW_HELD", rs.getInt("noshow_held"));
            }
        }
        return counts;
    }

    /**
     * Marks payment as REFUNDED for a CANCELLED booking with REFUND_PENDING payment.
     */
    public boolean markPaymentRefunded(long bookingId, long adminUserId, String note) throws SQLException {
        String cleanNote = (note != null && !note.trim().isEmpty()) ? note.trim() : "Marked refunded by admin";
        if (cleanNote.length() > 500) {
            cleanNote = cleanNote.substring(0, 500);
        }

        String sql = "UPDATE PAYMENT SET status = 'REFUNDED', resolution_note = ?, "
                + "resolved_by = ?, resolved_at = SYSDATETIME(), updated_at = SYSDATETIME() "
                + "WHERE booking_id = ? AND status = 'REFUND_PENDING'";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanNote);
            ps.setLong(2, adminUserId);
            ps.setLong(3, bookingId);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                java.util.logging.Logger.getLogger(BookingDao.class.getName())
                        .info("Audit: Payment for booking #" + bookingId + " marked REFUNDED by admin #" + adminUserId + ": " + cleanNote);
                return true;
            }
        }
        return false;
    }

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }
}
