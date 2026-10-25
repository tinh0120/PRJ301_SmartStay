package service;

import dao.BookingDao;
import dao.ReviewDao;
import dao.RoomDao;
import model.Booking;
import model.Booking.Quote;
import model.RoomSlot;
import util.AppConstants;
import util.DBUtil;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

/**
 * Unified Booking Service managing full booking lifecycle:
 * - Guest actions: reservation creation, quotation parsing, guest cancellation, post-stay reviews.
 * - Admin actions: walk-in bookings, booking edits, cancellations, check-in/out transitions,
 *   no-show release/reinstate, and access credential revocations.
 */
public class BookingService {

    private static final Logger LOGGER = Logger.getLogger(BookingService.class.getName());

    private final RoomService roomService = new RoomService();
    private final PricingService pricingService = new PricingService();
    private final RoomDao roomDao = new RoomDao();
    private final BookingDao bookingDao = new BookingDao();
    private final CredentialService credentialService = new CredentialService();
    private final ReviewDao reviewDao = new ReviewDao();

    public BookingService() {
    }

    /**
     * Shared helper to extract and parse slot codes from repeated parameters and comma-separated values.
     */
    public static List<String> parseSlotCodes(HttpServletRequest req) {
        List<String> slotCodes = new ArrayList<>();
        String[] codeParams = req.getParameterValues("slotCodes");
        if (codeParams == null || codeParams.length == 0) {
            codeParams = req.getParameterValues("slots");
        }
        if (codeParams != null) {
            for (String p : codeParams) {
                if (p != null) {
                    if (p.contains(",")) {
                        String[] split = p.split(",");
                        for (String s : split) {
                            if (!s.trim().isEmpty()) {
                                slotCodes.add(s.trim());
                            }
                        }
                    } else if (!p.trim().isEmpty()) {
                        slotCodes.add(p.trim());
                    }
                }
            }
        }
        return slotCodes;
    }

    // =========================================================================
    // GUEST BOOKING METHODS
    // =========================================================================

    /**
     * Creates a new booking within a single transaction under an explicit row lock.
     */
    public Booking createBooking(long guestId, long roomId, LocalDate date, Collection<String> codes, String ip)
            throws SQLException, ServiceException {

        LocalDateTime now = LocalDateTime.now();

        // Step 1: Advisory validation via quoteFor to obtain exact error codes/messages
        Quote advisoryQuote = roomService.quoteFor(roomId, date, codes, now);

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Step 2: Lock the room row
                String lockRoomSql = "SELECT room_id, room_number, status, is_active "
                        + "FROM ROOM WITH (UPDLOCK, HOLDLOCK) "
                        + "WHERE room_id = ?";
                String roomNumber;
                try (PreparedStatement psLock = conn.prepareStatement(lockRoomSql)) {
                    psLock.setLong(1, roomId);
                    try (ResultSet rs = psLock.executeQuery()) {
                        if (!rs.next() || !rs.getBoolean("is_active")) {
                            throw new ServiceException("ROOM_NOT_FOUND", "This room is not available.");
                        }
                        String status = rs.getString("status");
                        if ("MAINTENANCE".equalsIgnoreCase(status)) {
                            throw new ServiceException("ROOM_NOT_BOOKABLE", "This room is under maintenance and cannot be booked.");
                        }
                        roomNumber = rs.getString("room_number");
                    }
                }

                // Step 3: Load all slots of the room with the locked connection
                List<RoomSlot> allSlots = roomDao.findByRoomId(conn, roomId);

                // Step 4: Re-run pricing calculation inside transaction
                Quote quote = pricingService.quote(allSlots, codes, PricingService.DiscountRates.fromConfig());

                // Step 5: Validate each selected slot's availability under lock
                LocalDateTime checkinAt = null;
                LocalDateTime checkoutAt = null;

                for (RoomSlot slot : quote.getSlots()) {
                    if (!slot.isActive()) {
                        throw new ServiceException("SLOT_UNAVAILABLE", "One or more selected slots are no longer available.");
                    }

                    LocalDateTime startAt = LocalDateTime.of(date, slot.getStartTime());
                    LocalDateTime endAt = LocalDateTime.of(
                            slot.isCrossesMidnight() ? date.plusDays(1) : date,
                            slot.getEndTime()
                    );

                    if (!startAt.isAfter(now)) {
                        throw new ServiceException("SLOT_UNAVAILABLE", "One or more selected slots are no longer available.");
                    }

                    // Check HELD row in BOOKING_SLOT
                    String heldCheckSql = "SELECT 1 FROM BOOKING_SLOT WITH (UPDLOCK, HOLDLOCK) "
                            + "WHERE room_slot_id = ? AND slot_date = ? AND hold_status = 'HELD'";
                    try (PreparedStatement psHeld = conn.prepareStatement(heldCheckSql)) {
                        psHeld.setLong(1, slot.getRoomSlotId());
                        psHeld.setDate(2, Date.valueOf(date));
                        try (ResultSet rsHeld = psHeld.executeQuery()) {
                            if (rsHeld.next()) {
                                throw new ServiceException("SLOT_UNAVAILABLE", "One or more selected slots are no longer available.");
                            }
                        }
                    }

                    if (checkinAt == null || startAt.isBefore(checkinAt)) {
                        checkinAt = startAt;
                    }
                    if (checkoutAt == null || endAt.isAfter(checkoutAt)) {
                        checkoutAt = endAt;
                    }
                }

                int holdMinutes = AppConstants.BOOKING_HOLD_MINUTES;
                LocalDateTime paymentDeadline = now.plusMinutes(holdMinutes);

                // Step 6: Insert BOOKING
                String insertBookingSql = "INSERT INTO BOOKING ("
                        + "guest_id, room_id, slot_date, checkin_at, checkout_at, "
                        + "subtotal_amount, discount_percent, discount_amount, total_amount, "
                        + "status, expire_reason, payment_deadline, note, created_by, created_at"
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING_PAYMENT', NULL, ?, NULL, ?, ?)";

                long newBookingId;
                try (PreparedStatement psInsert = conn.prepareStatement(insertBookingSql, Statement.RETURN_GENERATED_KEYS)) {
                    psInsert.setLong(1, guestId);
                    psInsert.setLong(2, roomId);
                    psInsert.setDate(3, Date.valueOf(date));
                    psInsert.setTimestamp(4, Timestamp.valueOf(checkinAt));
                    psInsert.setTimestamp(5, Timestamp.valueOf(checkoutAt));
                    psInsert.setLong(6, quote.getSubtotal());
                    psInsert.setInt(7, quote.getDiscountPercent());
                    psInsert.setLong(8, quote.getDiscountAmount());
                    psInsert.setLong(9, quote.getTotal());
                    psInsert.setTimestamp(10, Timestamp.valueOf(paymentDeadline));
                    psInsert.setLong(11, guestId);
                    psInsert.setTimestamp(12, Timestamp.valueOf(now));

                    psInsert.executeUpdate();
                    try (ResultSet gk = psInsert.getGeneratedKeys()) {
                        if (!gk.next()) {
                            throw new SQLException("Failed to retrieve generated booking ID.");
                        }
                        newBookingId = gk.getLong(1);
                    }
                }

                // Step 7: Insert each BOOKING_SLOT
                String insertSlotSql = "INSERT INTO BOOKING_SLOT ("
                        + "booking_id, room_id, room_slot_id, slot_date, start_at, end_at, list_price, hold_status"
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, 'HELD')";

                try (PreparedStatement psSlot = conn.prepareStatement(insertSlotSql)) {
                    for (RoomSlot slot : quote.getSlots()) {
                        LocalDateTime startAt = LocalDateTime.of(date, slot.getStartTime());
                        LocalDateTime endAt = LocalDateTime.of(
                                slot.isCrossesMidnight() ? date.plusDays(1) : date,
                                slot.getEndTime()
                        );

                        psSlot.setLong(1, newBookingId);
                        psSlot.setLong(2, roomId);
                        psSlot.setLong(3, slot.getRoomSlotId());
                        psSlot.setDate(4, Date.valueOf(date));
                        psSlot.setTimestamp(5, Timestamp.valueOf(startAt));
                        psSlot.setTimestamp(6, Timestamp.valueOf(endAt));
                        psSlot.setLong(7, slot.getListPrice());
                        psSlot.addBatch();
                    }
                    psSlot.executeBatch();
                }

                // Step 8: Read computed booking code for audit logging
                String bookingCode = "BK" + String.format("%06d", newBookingId);
                String auditDetail = "Guest booking " + bookingCode + ", room " + roomNumber + ", "
                        + date + ", " + quote.getSlotCount() + " slot(s), total " + quote.getTotal() + " VND";
                LOGGER.info("Audit: " + auditDetail);

                // Step 9: Commit transaction
                conn.commit();

                // Re-read and return created booking
                return bookingDao.findById(newBookingId);

            } catch (SQLException e) {
                conn.rollback();
                if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                    throw new ServiceException("SLOT_UNAVAILABLE", "One or more selected slots are no longer available.");
                }
                LOGGER.log(Level.SEVERE, "Database error during booking creation", e);
                throw e;
            } catch (ServiceException pe) {
                conn.rollback();
                throw pe;
            } catch (Exception ex) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Unexpected error during booking creation", ex);
                throw new SQLException("Booking creation encountered an unexpected failure.", ex);
            }
        }
    }

    /**
     * Cancels an existing booking on behalf of a guest within a single transaction.
     */
    public void cancelByGuest(long bookingId, long guestId, String reason, String ip)
            throws SQLException, ServiceException {

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Lock the booking row
                String lockBookingSql = "SELECT booking_id, guest_id, status, checkin_at, booking_code "
                        + "FROM BOOKING WITH (UPDLOCK, HOLDLOCK) "
                        + "WHERE booking_id = ?";

                String currentStatus;
                Timestamp checkinTs;
                String bookingCode;

                try (PreparedStatement psLock = conn.prepareStatement(lockBookingSql)) {
                    psLock.setLong(1, bookingId);
                    try (ResultSet rs = psLock.executeQuery()) {
                        if (!rs.next() || rs.getLong("guest_id") != guestId) {
                            throw new ServiceException("BAD_REQUEST", "Booking not found.");
                        }
                        currentStatus = rs.getString("status");
                        checkinTs = rs.getTimestamp("checkin_at");
                        bookingCode = rs.getString("booking_code");
                    }
                }

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime checkinAt = (checkinTs != null) ? checkinTs.toLocalDateTime() : null;

                boolean cancellable = (Booking.STATUS_PENDING_PAYMENT.equalsIgnoreCase(currentStatus)
                        || Booking.STATUS_CONFIRMED.equalsIgnoreCase(currentStatus))
                        && checkinAt != null
                        && now.isBefore(checkinAt);

                if (!cancellable) {
                    throw new ServiceException("NOT_CANCELLABLE", "This booking can no longer be cancelled.");
                }

                String cleanReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Cancelled by guest";
                if (cleanReason.length() > 255) {
                    cleanReason = cleanReason.substring(0, 255);
                }

                // 1. Update BOOKING
                String updateBookingSql = "UPDATE BOOKING "
                        + "SET status = 'CANCELLED', cancel_reason = ?, cancelled_at = ?, cancelled_by = ? "
                        + "WHERE booking_id = ?";
                try (PreparedStatement psUpdB = conn.prepareStatement(updateBookingSql)) {
                    psUpdB.setString(1, cleanReason);
                    psUpdB.setTimestamp(2, Timestamp.valueOf(now));
                    psUpdB.setLong(3, guestId);
                    psUpdB.setLong(4, bookingId);
                    psUpdB.executeUpdate();
                }

                // 2. Update BOOKING_SLOT
                String updateSlotsSql = "UPDATE BOOKING_SLOT "
                        + "SET hold_status = 'RELEASED', released_at = ? "
                        + "WHERE booking_id = ? AND hold_status = 'HELD'";
                try (PreparedStatement psUpdS = conn.prepareStatement(updateSlotsSql)) {
                    psUpdS.setTimestamp(1, Timestamp.valueOf(now));
                    psUpdS.setLong(2, bookingId);
                    psUpdS.executeUpdate();
                }

                // 3. Update ACCESS_CREDENTIAL (if any exist)
                String updateCredSql = "UPDATE ACCESS_CREDENTIAL "
                        + "SET status = 'REVOKED', revoke_reason = 'CANCELLED', revoked_at = ? "
                        + "WHERE booking_id = ? AND status IN ('PENDING', 'ACTIVE')";
                int revokedCreds = 0;
                try (PreparedStatement psUpdC = conn.prepareStatement(updateCredSql)) {
                    psUpdC.setTimestamp(1, Timestamp.valueOf(now));
                    psUpdC.setLong(2, bookingId);
                    revokedCreds = psUpdC.executeUpdate();
                }

                // 4. Update latest PAID payment to REFUND_PENDING
                String updatePaySql = "UPDATE PAYMENT "
                        + "SET status = 'REFUND_PENDING', resolution_note = 'Auto-flagged on guest cancellation', updated_at = ? "
                        + "WHERE payment_id = (SELECT TOP 1 payment_id FROM PAYMENT WHERE booking_id = ? AND status = 'PAID' ORDER BY payment_id DESC)";
                try (PreparedStatement psUpdP = conn.prepareStatement(updatePaySql)) {
                    psUpdP.setTimestamp(1, Timestamp.valueOf(now));
                    psUpdP.setLong(2, bookingId);
                    psUpdP.executeUpdate();
                }

                // 5. Audit logs
                LOGGER.info("Booking " + bookingCode + " cancelled by guest");

                if (revokedCreds > 0) {
                    LOGGER.info("Credentials revoked on booking cancellation for booking " + bookingCode);
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Database error during booking cancellation", e);
                throw e;
            } catch (ServiceException pe) {
                conn.rollback();
                throw pe;
            } catch (Exception ex) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Unexpected error during booking cancellation", ex);
                throw new SQLException("Booking cancellation encountered an unexpected failure.", ex);
            }
        }
    }

    // =========================================================================
    // GUEST REVIEW METHOD
    // =========================================================================

    /**
     * Submits a guest review for an eligible checked-out booking.
     */
    public long createReview(long guestId, long bookingId, int rating, String comment, String ip)
            throws SQLException, ServiceException {

        // 1. Validate rating
        if (rating < 1 || rating > 5) {
            throw new ServiceException("INVALID_RATING", "Please choose a rating from 1 to 5.");
        }

        // 2. Validate comment length
        String trimmedComment = (comment != null) ? comment.trim() : null;
        if (trimmedComment != null && trimmedComment.length() > 1000) {
            throw new ServiceException("COMMENT_TOO_LONG", "Your comment must be at most 1000 characters.");
        }

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 3. Validate booking eligibility
                String checkSql = "SELECT booking_id, guest_id, room_id, status FROM BOOKING WHERE booking_id = ?";
                long roomId = -1;
                String status = null;

                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setLong(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || rs.getLong("guest_id") != guestId) {
                            throw new ServiceException("NOT_FOUND", "Booking not found.");
                        }
                        roomId = rs.getLong("room_id");
                        status = rs.getString("status");
                    }
                }

                if (!"CHECKED_OUT".equalsIgnoreCase(status)) {
                    throw new ServiceException("NOT_ELIGIBLE", "You can only review after checkout.");
                }

                // 4. Insert review
                long reviewId;
                try {
                    reviewId = reviewDao.insertReview(conn, bookingId, guestId, roomId, rating, trimmedComment);
                } catch (SQLException e) {
                    if (e.getErrorCode() == 2627 || e.getErrorCode() == 2601) {
                        throw new ServiceException("ALREADY_REVIEWED", "You have already reviewed this booking.");
                    }
                    throw e;
                }

                // 5. Audit
                LOGGER.info("Guest review submitted for booking #" + bookingId + " (" + rating + " stars)");

                conn.commit();
                return reviewId;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof ServiceException) {
                    throw (ServiceException) e;
                }
                if (e instanceof SQLException) {
                    throw (SQLException) e;
                }
                throw new SQLException("Failed to save review", e);
            }
        }
    }

    // =========================================================================
    // ADMIN BOOKING METHODS
    // =========================================================================

    /**
     * Creates a walk-in booking on behalf of an active guest.
     * Admin may book a slot currently in progress (end_at > now even if start_at <= now).
     */
    public Booking createWalkIn(long adminUserId, long guestUserId, long roomId, LocalDate date,
                                Collection<String> codes, String note, String ip)
            throws SQLException, ServiceException {

        if (note == null || note.trim().isEmpty()) {
            throw new ServiceException("NOTE_REQUIRED", "Walk-in reason note is required.");
        }
        String cleanNote = note.trim();
        if (cleanNote.length() > 500) {
            cleanNote = cleanNote.substring(0, 500);
        }

        if (codes == null || codes.isEmpty()) {
            throw new ServiceException("SLOTS_REQUIRED", "Please select at least one time slot.");
        }
        if (date == null) {
            throw new ServiceException("DATE_REQUIRED", "Please select a booking date.");
        }

        LocalDateTime now = LocalDateTime.now();

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Validate guest
                String guestSql = "SELECT u.user_id, u.full_name, u.email, u.status, r.role_name "
                        + "FROM APP_USER u JOIN ROLE r ON u.role_id = r.role_id "
                        + "WHERE u.user_id = ?";
                String guestName;
                try (PreparedStatement psG = conn.prepareStatement(guestSql)) {
                    psG.setLong(1, guestUserId);
                    try (ResultSet rsG = psG.executeQuery()) {
                        if (!rsG.next() || !"ACTIVE".equalsIgnoreCase(rsG.getString("status"))
                                || !"GUEST".equalsIgnoreCase(rsG.getString("role_name"))) {
                            throw new ServiceException("INVALID_GUEST", "Selected guest account must be active with role GUEST.");
                        }
                        guestName = rsG.getString("full_name");
                    }
                }

                // 2. Lock room row
                String lockRoomSql = "SELECT room_id, room_number, status, is_active "
                        + "FROM ROOM WITH (UPDLOCK, HOLDLOCK) WHERE room_id = ?";
                String roomNumber;
                try (PreparedStatement psLock = conn.prepareStatement(lockRoomSql)) {
                    psLock.setLong(1, roomId);
                    try (ResultSet rs = psLock.executeQuery()) {
                        if (!rs.next() || !rs.getBoolean("is_active")) {
                            throw new ServiceException("ROOM_NOT_FOUND", "This room is not available.");
                        }
                        if ("MAINTENANCE".equalsIgnoreCase(rs.getString("status"))) {
                            throw new ServiceException("ROOM_NOT_BOOKABLE", "This room is under maintenance and cannot be booked.");
                        }
                        roomNumber = rs.getString("room_number");
                    }
                }

                // 3. Load slots and compute pricing
                List<RoomSlot> allSlots = roomDao.findByRoomId(conn, roomId);
                Quote quote = pricingService.quote(allSlots, codes, PricingService.DiscountRates.fromConfig());

                // 4. Validate availability (admin relaxation: endAt > now)
                LocalDateTime checkinAt = null;
                LocalDateTime checkoutAt = null;

                for (RoomSlot slot : quote.getSlots()) {
                    if (!slot.isActive()) {
                        throw new ServiceException("SLOT_UNAVAILABLE", "One or more selected slots are no longer available.");
                    }

                    LocalDateTime startAt = LocalDateTime.of(date, slot.getStartTime());
                    LocalDateTime endAt = LocalDateTime.of(
                            slot.isCrossesMidnight() ? date.plusDays(1) : date,
                            slot.getEndTime()
                    );

                    if (!endAt.isAfter(now)) {
                        throw new ServiceException("SLOT_UNAVAILABLE", "One or more selected slots have already ended.");
                    }

                    // Check HELD row
                    String heldCheckSql = "SELECT 1 FROM BOOKING_SLOT WITH (UPDLOCK, HOLDLOCK) "
                            + "WHERE room_slot_id = ? AND slot_date = ? AND hold_status = 'HELD'";
                    try (PreparedStatement psHeld = conn.prepareStatement(heldCheckSql)) {
                        psHeld.setLong(1, slot.getRoomSlotId());
                        psHeld.setDate(2, Date.valueOf(date));
                        try (ResultSet rsHeld = psHeld.executeQuery()) {
                            if (rsHeld.next()) {
                                throw new ServiceException("SLOT_UNAVAILABLE", "One or more selected slots are no longer available.");
                            }
                        }
                    }

                    if (checkinAt == null || startAt.isBefore(checkinAt)) {
                        checkinAt = startAt;
                    }
                    if (checkoutAt == null || endAt.isAfter(checkoutAt)) {
                        checkoutAt = endAt;
                    }
                }

                int holdMinutes = AppConstants.BOOKING_HOLD_MINUTES;
                LocalDateTime paymentDeadline = now.plusMinutes(holdMinutes);

                // 5. Insert BOOKING
                String insertBookingSql = "INSERT INTO BOOKING ("
                        + "guest_id, room_id, slot_date, checkin_at, checkout_at, "
                        + "subtotal_amount, discount_percent, discount_amount, total_amount, "
                        + "status, expire_reason, payment_deadline, note, created_by, created_at"
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING_PAYMENT', NULL, ?, ?, ?, ?)";

                long newBookingId;
                try (PreparedStatement psInsert = conn.prepareStatement(insertBookingSql, Statement.RETURN_GENERATED_KEYS)) {
                    psInsert.setLong(1, guestUserId);
                    psInsert.setLong(2, roomId);
                    psInsert.setDate(3, Date.valueOf(date));
                    psInsert.setTimestamp(4, Timestamp.valueOf(checkinAt));
                    psInsert.setTimestamp(5, Timestamp.valueOf(checkoutAt));
                    psInsert.setLong(6, quote.getSubtotal());
                    psInsert.setInt(7, quote.getDiscountPercent());
                    psInsert.setLong(8, quote.getDiscountAmount());
                    psInsert.setLong(9, quote.getTotal());
                    psInsert.setTimestamp(10, Timestamp.valueOf(paymentDeadline));
                    psInsert.setString(11, cleanNote);
                    psInsert.setLong(12, adminUserId);
                    psInsert.setTimestamp(13, Timestamp.valueOf(now));

                    psInsert.executeUpdate();
                    try (ResultSet gk = psInsert.getGeneratedKeys()) {
                        if (!gk.next()) {
                            throw new SQLException("Failed to retrieve generated booking ID.");
                        }
                        newBookingId = gk.getLong(1);
                    }
                }

                // 6. Insert BOOKING_SLOT
                String insertSlotSql = "INSERT INTO BOOKING_SLOT ("
                        + "booking_id, room_id, room_slot_id, slot_date, start_at, end_at, list_price, hold_status"
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, 'HELD')";

                try (PreparedStatement psSlot = conn.prepareStatement(insertSlotSql)) {
                    for (RoomSlot slot : quote.getSlots()) {
                        LocalDateTime startAt = LocalDateTime.of(date, slot.getStartTime());
                        LocalDateTime endAt = LocalDateTime.of(
                                slot.isCrossesMidnight() ? date.plusDays(1) : date,
                                slot.getEndTime()
                        );

                        psSlot.setLong(1, newBookingId);
                        psSlot.setLong(2, roomId);
                        psSlot.setLong(3, slot.getRoomSlotId());
                        psSlot.setDate(4, Date.valueOf(date));
                        psSlot.setTimestamp(5, Timestamp.valueOf(startAt));
                        psSlot.setTimestamp(6, Timestamp.valueOf(endAt));
                        psSlot.setLong(7, slot.getListPrice());
                        psSlot.addBatch();
                    }
                    psSlot.executeBatch();
                }

                String bookingCode = "BK" + String.format("%06d", newBookingId);
                String auditDetail = "Walk-in by admin #" + adminUserId + ": Booking " + bookingCode
                        + " for " + guestName + ", room " + roomNumber + ", " + date + ", total " + quote.getTotal() + " VND";
                LOGGER.info("Audit: " + auditDetail);

                conn.commit();
                return bookingDao.findById(newBookingId);

            } catch (SQLException e) {
                conn.rollback();
                if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                    throw new ServiceException("SLOT_UNAVAILABLE", "One or more selected slots are no longer available.");
                }
                LOGGER.log(Level.SEVERE, "Database error during walk-in booking creation", e);
                throw e;
            } catch (ServiceException pe) {
                conn.rollback();
                throw pe;
            } catch (Exception ex) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Unexpected error during walk-in creation", ex);
                throw new SQLException("Walk-in booking failed unexpectedly.", ex);
            }
        }
    }

    /**
     * Updates an existing booking.
     * If PENDING_PAYMENT: may update room/date/slots (releases old slots, recomputes amounts, sets new payment deadline).
     * If CONFIRMED: may only update note.
     */
    public void updateAdmin(long adminUserId, long bookingId, Long newRoomId, LocalDate newDate,
                            Collection<String> newCodes, String newNote, String ip)
            throws SQLException, ServiceException {

        String cleanNote = (newNote != null) ? newNote.trim() : "";
        if (cleanNote.length() > 500) {
            cleanNote = cleanNote.substring(0, 500);
        }

        LocalDateTime now = LocalDateTime.now();

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String lockSql = "SELECT booking_id, booking_code, guest_id, room_id, slot_date, status, total_amount, note "
                        + "FROM BOOKING WITH (UPDLOCK, HOLDLOCK) WHERE booking_id = ?";

                String bookingCode;
                long guestId;
                long currentRoomId;
                LocalDate currentDate;
                String currentStatus;
                long oldTotal;
                String oldNote;

                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setLong(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new ServiceException("NOT_FOUND", "Booking not found.");
                        }
                        bookingCode = rs.getString("booking_code");
                        guestId = rs.getLong("guest_id");
                        currentRoomId = rs.getLong("room_id");
                        currentDate = rs.getDate("slot_date").toLocalDate();
                        currentStatus = rs.getString("status");
                        oldTotal = rs.getBigDecimal("total_amount").longValueExact();
                        oldNote = rs.getString("note");
                    }
                }

                if (Booking.STATUS_CONFIRMED.equalsIgnoreCase(currentStatus)) {
                    // Confirmed: only note may be updated
                    boolean triedToChangeRoom = (newRoomId != null && newRoomId != currentRoomId);
                    boolean triedToChangeDate = (newDate != null && !newDate.equals(currentDate));
                    boolean triedToChangeSlots = (newCodes != null && !newCodes.isEmpty());

                    if (triedToChangeRoom || triedToChangeDate || triedToChangeSlots) {
                        throw new ServiceException("CONFIRMED_NOTE_ONLY", "Confirmed bookings can only have their note updated.");
                    }

                    String updSql = "UPDATE BOOKING SET note = ?, updated_at = SYSDATETIME() WHERE booking_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updSql)) {
                        ps.setString(1, cleanNote);
                        ps.setLong(2, bookingId);
                        ps.executeUpdate();
                    }

                    String auditDetail = "Admin #" + adminUserId + " updated note on confirmed booking " + bookingCode
                            + ": '" + (oldNote != null ? oldNote : "") + "' -> '" + cleanNote + "'";
                    LOGGER.info("Audit: " + auditDetail);

                    conn.commit();
                    return;
                }

                if (Booking.STATUS_PENDING_PAYMENT.equalsIgnoreCase(currentStatus)) {
                    boolean hasSlotChanges = (newRoomId != null && newDate != null && newCodes != null && !newCodes.isEmpty());

                    if (hasSlotChanges) {
                        // 1. Delete existing slots for this booking so they don't block new hold
                        String delOldSql = "DELETE FROM BOOKING_SLOT WHERE booking_id = ?";
                        try (PreparedStatement psDel = conn.prepareStatement(delOldSql)) {
                            psDel.setLong(1, bookingId);
                            psDel.executeUpdate();
                        }

                        // 2. Lock target room
                        String lockRoomSql = "SELECT room_id, room_number, status, is_active "
                                + "FROM ROOM WITH (UPDLOCK, HOLDLOCK) WHERE room_id = ?";
                        String targetRoomNumber;
                        try (PreparedStatement psLockR = conn.prepareStatement(lockRoomSql)) {
                            psLockR.setLong(1, newRoomId);
                            try (ResultSet rs = psLockR.executeQuery()) {
                                if (!rs.next() || !rs.getBoolean("is_active") || "MAINTENANCE".equalsIgnoreCase(rs.getString("status"))) {
                                    throw new ServiceException("ROOM_NOT_BOOKABLE", "Target room is not bookable.");
                                }
                                targetRoomNumber = rs.getString("room_number");
                            }
                        }

                        // 3. Load slots and calculate pricing
                        List<RoomSlot> allSlots = roomDao.findByRoomId(conn, newRoomId);
                        Quote quote = pricingService.quote(allSlots, newCodes, PricingService.DiscountRates.fromConfig());

                        LocalDateTime checkinAt = null;
                        LocalDateTime checkoutAt = null;

                        for (RoomSlot slot : quote.getSlots()) {
                            if (!slot.isActive()) {
                                throw new ServiceException("SLOT_UNAVAILABLE", "One or more selected slots are no longer available.");
                            }
                            LocalDateTime startAt = LocalDateTime.of(newDate, slot.getStartTime());
                            LocalDateTime endAt = LocalDateTime.of(
                                    slot.isCrossesMidnight() ? newDate.plusDays(1) : newDate,
                                    slot.getEndTime()
                            );
                            if (!endAt.isAfter(now)) {
                                throw new ServiceException("SLOT_UNAVAILABLE", "One or more selected slots have already ended.");
                            }

                            String heldCheckSql = "SELECT 1 FROM BOOKING_SLOT WITH (UPDLOCK, HOLDLOCK) "
                                    + "WHERE room_slot_id = ? AND slot_date = ? AND hold_status = 'HELD'";
                            try (PreparedStatement psHeld = conn.prepareStatement(heldCheckSql)) {
                                psHeld.setLong(1, slot.getRoomSlotId());
                                psHeld.setDate(2, Date.valueOf(newDate));
                                try (ResultSet rsHeld = psHeld.executeQuery()) {
                                    if (rsHeld.next()) {
                                        throw new ServiceException("SLOT_UNAVAILABLE", "One or more selected slots are no longer available.");
                                    }
                                }
                            }

                            if (checkinAt == null || startAt.isBefore(checkinAt)) checkinAt = startAt;
                            if (checkoutAt == null || endAt.isAfter(checkoutAt)) checkoutAt = endAt;
                        }

                        int holdMinutes = AppConstants.BOOKING_HOLD_MINUTES;
                        LocalDateTime newDeadline = now.plusMinutes(holdMinutes);

                        // 4. Update BOOKING
                        String updBSql = "UPDATE BOOKING SET room_id = ?, slot_date = ?, checkin_at = ?, checkout_at = ?, "
                                + "subtotal_amount = ?, discount_percent = ?, discount_amount = ?, total_amount = ?, "
                                + "payment_deadline = ?, note = ?, updated_at = SYSDATETIME() "
                                + "WHERE booking_id = ?";
                        try (PreparedStatement psUpd = conn.prepareStatement(updBSql)) {
                            psUpd.setLong(1, newRoomId);
                            psUpd.setDate(2, Date.valueOf(newDate));
                            psUpd.setTimestamp(3, Timestamp.valueOf(checkinAt));
                            psUpd.setTimestamp(4, Timestamp.valueOf(checkoutAt));
                            psUpd.setLong(5, quote.getSubtotal());
                            psUpd.setInt(6, quote.getDiscountPercent());
                            psUpd.setLong(7, quote.getDiscountAmount());
                            psUpd.setLong(8, quote.getTotal());
                            psUpd.setTimestamp(9, Timestamp.valueOf(newDeadline));
                            psUpd.setString(10, cleanNote);
                            psUpd.setLong(11, bookingId);
                            psUpd.executeUpdate();
                        }

                        // 5. Insert new BOOKING_SLOT rows
                        String insSlotSql = "INSERT INTO BOOKING_SLOT ("
                                + "booking_id, room_id, room_slot_id, slot_date, start_at, end_at, list_price, hold_status"
                                + ") VALUES (?, ?, ?, ?, ?, ?, ?, 'HELD')";
                        try (PreparedStatement psSlot = conn.prepareStatement(insSlotSql)) {
                            for (RoomSlot slot : quote.getSlots()) {
                                LocalDateTime startAt = LocalDateTime.of(newDate, slot.getStartTime());
                                LocalDateTime endAt = LocalDateTime.of(
                                        slot.isCrossesMidnight() ? newDate.plusDays(1) : newDate,
                                        slot.getEndTime()
                                );
                                psSlot.setLong(1, bookingId);
                                psSlot.setLong(2, newRoomId);
                                psSlot.setLong(3, slot.getRoomSlotId());
                                psSlot.setDate(4, Date.valueOf(newDate));
                                psSlot.setTimestamp(5, Timestamp.valueOf(startAt));
                                psSlot.setTimestamp(6, Timestamp.valueOf(endAt));
                                psSlot.setLong(7, slot.getListPrice());
                                psSlot.addBatch();
                            }
                            psSlot.executeBatch();
                        }

                        String auditDetail = "Admin #" + adminUserId + " updated booking " + bookingCode + ": "
                                + "Room " + currentRoomId + " -> " + newRoomId + ", Date " + currentDate + " -> " + newDate
                                + ", Total " + oldTotal + " -> " + quote.getTotal() + " VND";
                        LOGGER.info("Audit: " + auditDetail);

                    } else {
                        // Only note updated
                        String updNoteSql = "UPDATE BOOKING SET note = ?, updated_at = SYSDATETIME() WHERE booking_id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(updNoteSql)) {
                            ps.setString(1, cleanNote);
                            ps.setLong(2, bookingId);
                            ps.executeUpdate();
                        }

                        String auditDetail = "Admin #" + adminUserId + " updated note on booking " + bookingCode
                                + ": '" + (oldNote != null ? oldNote : "") + "' -> '" + cleanNote + "'";
                        LOGGER.info("Audit: " + auditDetail);
                    }

                    conn.commit();
                    return;
                }

                throw new ServiceException("INVALID_STATUS", "Only bookings in PENDING_PAYMENT or CONFIRMED status can be edited.");

            } catch (SQLException e) {
                conn.rollback();
                if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                    throw new ServiceException("SLOT_UNAVAILABLE", "One or more selected slots are no longer available.");
                }
                LOGGER.log(Level.SEVERE, "Database error updating booking " + bookingId, e);
                throw e;
            } catch (ServiceException pe) {
                conn.rollback();
                throw pe;
            } catch (Exception ex) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Unexpected error updating booking " + bookingId, ex);
                throw new SQLException("Booking update failed unexpectedly.", ex);
            }
        }
    }

    /**
     * Cancels an existing booking as an administrator with mandatory reason.
     * Allowed for PENDING_PAYMENT and CONFIRMED (even after start time).
     */
    public void cancelByAdmin(long adminUserId, long bookingId, String reason, String ip)
            throws SQLException, ServiceException {

        if (reason == null || reason.trim().isEmpty()) {
            throw new ServiceException("REASON_REQUIRED", "Cancellation reason is required.");
        }
        String cleanReason = reason.trim();
        if (cleanReason.length() > 255) {
            cleanReason = cleanReason.substring(0, 255);
        }

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String lockSql = "SELECT booking_id, booking_code, guest_id, status FROM BOOKING WITH (UPDLOCK, HOLDLOCK) WHERE booking_id = ?";
                String bookingCode;
                String status;

                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setLong(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new ServiceException("NOT_FOUND", "Booking not found.");
                        }
                        bookingCode = rs.getString("booking_code");
                        status = rs.getString("status");
                    }
                }

                if (!Booking.STATUS_PENDING_PAYMENT.equalsIgnoreCase(status) && !Booking.STATUS_CONFIRMED.equalsIgnoreCase(status)) {
                    throw new ServiceException("NOT_CANCELLABLE", "Booking in status " + status + " cannot be cancelled.");
                }

                // 1. Update BOOKING
                String updBSql = "UPDATE BOOKING SET status = 'CANCELLED', cancel_reason = ?, "
                        + "cancelled_at = SYSDATETIME(), cancelled_by = ?, updated_at = SYSDATETIME() "
                        + "WHERE booking_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updBSql)) {
                    ps.setString(1, cleanReason);
                    ps.setLong(2, adminUserId);
                    ps.setLong(3, bookingId);
                    ps.executeUpdate();
                }

                // 2. Release slots
                String updSlots = "UPDATE BOOKING_SLOT SET hold_status = 'RELEASED', released_at = SYSDATETIME() "
                        + "WHERE booking_id = ? AND hold_status = 'HELD'";
                try (PreparedStatement ps = conn.prepareStatement(updSlots)) {
                    ps.setLong(1, bookingId);
                    ps.executeUpdate();
                }

                // 3. Revoke live credentials
                String updCred = "UPDATE ACCESS_CREDENTIAL SET status = 'REVOKED', revoke_reason = 'CANCELLED', "
                        + "revoked_at = SYSDATETIME() WHERE booking_id = ? AND status IN ('PENDING', 'ACTIVE')";
                int revokedCount = 0;
                try (PreparedStatement ps = conn.prepareStatement(updCred)) {
                    ps.setLong(1, bookingId);
                    revokedCount = ps.executeUpdate();
                }

                // 4. Update PAID payment to REFUND_PENDING
                String updPay = "UPDATE PAYMENT SET status = 'REFUND_PENDING', resolution_note = 'Auto-flagged on admin cancellation', "
                        + "updated_at = SYSDATETIME() "
                        + "WHERE payment_id = (SELECT TOP 1 payment_id FROM PAYMENT WHERE booking_id = ? AND status = 'PAID' ORDER BY payment_id DESC)";
                try (PreparedStatement ps = conn.prepareStatement(updPay)) {
                    ps.setLong(1, bookingId);
                    ps.executeUpdate();
                }

                // 5. Audit
                LOGGER.info("Admin #" + adminUserId + " cancelled booking " + bookingCode + ": " + cleanReason);
                if (revokedCount > 0) {
                    LOGGER.info("Credentials revoked on admin cancellation for booking " + bookingCode);
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Database error cancelling booking " + bookingId, e);
                throw e;
            } catch (ServiceException pe) {
                conn.rollback();
                throw pe;
            } catch (Exception ex) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Unexpected error cancelling booking " + bookingId, ex);
                throw new SQLException("Booking cancellation failed unexpectedly.", ex);
            }
        }
    }

    /**
     * Performs manual check-in for a CONFIRMED booking before no-show expiry.
     * Sets booking CHECKED_IN, room OCCUPIED, actual_checkin_at = now.
     */
    public void checkInManual(long adminUserId, long bookingId, String ip) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String lockSql = "SELECT booking_id, booking_code, room_id, status, checkin_at, allow_early_checkin "
                        + "FROM BOOKING WITH (UPDLOCK, HOLDLOCK) WHERE booking_id = ?";

                String bookingCode;
                long roomId;
                String status;
                Timestamp checkinAt;
                boolean allowEarly = false;

                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setLong(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("Booking not found.");
                        }
                        bookingCode = rs.getString("booking_code");
                        roomId = rs.getLong("room_id");
                        status = rs.getString("status");
                        checkinAt = rs.getTimestamp("checkin_at");
                        allowEarly = rs.getBoolean("allow_early_checkin");
                    }
                }

                if (Booking.STATUS_PENDING_PAYMENT.equalsIgnoreCase(status)) {
                    throw new IllegalStateException("Booking is pending payment. Payment must be completed before check-in.");
                }
                if (!Booking.STATUS_CONFIRMED.equalsIgnoreCase(status)) {
                    throw new IllegalStateException("Booking cannot be checked in because it is in status " + status + ".");
                }

                LocalDateTime now = LocalDateTime.now();
                // Check early check-in constraint
                if (checkinAt != null) {
                    LocalDateTime scheduledCheckin = checkinAt.toLocalDateTime();
                    if (now.isBefore(scheduledCheckin) && !allowEarly) {
                        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
                        throw new IllegalStateException("Too early to check in (Scheduled check-in time: " + scheduledCheckin.format(dtf) + ")");
                    }
                }

                // Check no-show grace window
                int graceMinutes = AppConstants.NOSHOW_GRACE_MINUTES;
                if (checkinAt != null) {
                    LocalDateTime limit = checkinAt.toLocalDateTime().plusMinutes(graceMinutes);
                    if (now.isAfter(limit)) {
                        throw new IllegalStateException("Booking has passed the no-show limit.");
                    }
                }

                // Update booking
                String updBSql = "UPDATE BOOKING SET status = 'CHECKED_IN', actual_checkin_at = SYSDATETIME(), "
                        + "updated_at = SYSDATETIME() WHERE booking_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updBSql)) {
                    ps.setLong(1, bookingId);
                    ps.executeUpdate();
                }

                // Update room to OCCUPIED
                String updRSql = "UPDATE ROOM SET status = 'OCCUPIED', updated_at = SYSDATETIME() WHERE room_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updRSql)) {
                    ps.setLong(1, roomId);
                    ps.executeUpdate();
                }

                // Activate credential if PENDING
                String updCSql = "UPDATE ACCESS_CREDENTIAL SET status = 'ACTIVE' WHERE booking_id = ? AND status = 'PENDING'";
                try (PreparedStatement ps = conn.prepareStatement(updCSql)) {
                    ps.setLong(1, bookingId);
                    ps.executeUpdate();
                }

                LOGGER.info("Manual check-in for booking " + bookingCode + " by admin #" + adminUserId);

                conn.commit();
                websocket.RealtimeWebSocketEndpoint.broadcastBookingUpdated(bookingCode, "CHECKED_IN", roomId);
                websocket.RealtimeWebSocketEndpoint.broadcastRoomStatusChanged(roomId, "", "OCCUPIED");
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Database error during check-in for booking " + bookingId, e);
                if (e instanceof SQLException) throw (SQLException) e;
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new SQLException("Manual check-in failed unexpectedly.", e);
            }
        }
    }

    /**
     * Toggles early check-in permission for a CONFIRMED booking.
     * Allowed only when the room is currently AVAILABLE.
     */
    public void toggleAllowEarlyCheckin(long adminUserId, long bookingId, boolean allow, String ip) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String lockSql = "SELECT b.booking_id, b.booking_code, b.room_id, b.status, r.status AS room_status "
                        + "FROM BOOKING b WITH (UPDLOCK, HOLDLOCK) "
                        + "JOIN ROOM r WITH (UPDLOCK, HOLDLOCK) ON b.room_id = r.room_id "
                        + "WHERE b.booking_id = ?";
                long roomId;
                String bStatus;
                String rStatus;
                String bCode;
                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setLong(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("Booking not found.");
                        }
                        bCode = rs.getString("booking_code");
                        roomId = rs.getLong("room_id");
                        bStatus = rs.getString("status");
                        rStatus = rs.getString("room_status");
                    }
                }

                if (allow) {
                    if (!Booking.STATUS_CONFIRMED.equalsIgnoreCase(bStatus)) {
                        throw new IllegalStateException("Early check-in can only be enabled for CONFIRMED bookings. Current status: " + bStatus);
                    }
                    if (!"AVAILABLE".equalsIgnoreCase(rStatus)) {
                        throw new IllegalStateException("Room is currently '" + rStatus + "'. Early check-in can only be enabled when the room is AVAILABLE.");
                    }
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE BOOKING SET allow_early_checkin = 1, updated_at = SYSDATETIME() WHERE booking_id = ?")) {
                        ps.setLong(1, bookingId);
                        ps.executeUpdate();
                    }
                    // Activate credential immediately so PIN is valid right away
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE ACCESS_CREDENTIAL SET status = 'ACTIVE', valid_from = SYSDATETIME() WHERE booking_id = ? AND status = 'PENDING'")) {
                        ps.setLong(1, bookingId);
                        ps.executeUpdate();
                    }
                    LOGGER.info("Admin #" + adminUserId + " enabled early check-in for booking " + bCode);
                } else {
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE BOOKING SET allow_early_checkin = 0, updated_at = SYSDATETIME() WHERE booking_id = ?")) {
                        ps.setLong(1, bookingId);
                        ps.executeUpdate();
                    }
                    LOGGER.info("Admin #" + adminUserId + " disabled early check-in for booking " + bCode);
                }

                conn.commit();
                websocket.RealtimeWebSocketEndpoint.broadcastBookingUpdated(bCode, "CONFIRMED", roomId);
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof IllegalStateException) throw (IllegalStateException) e;
                if (e instanceof SQLException) throw (SQLException) e;
                throw new SQLException("Failed to toggle early check-in.", e);
            }
        }
    }

    /**
     * Performs manual check-out for a CHECKED_IN booking.
     * Sets booking CHECKED_OUT, room NEEDS_CLEANING, credentials EXPIRED.
     */
    public void checkOutManual(long adminUserId, long bookingId, String ip) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String lockSql = "SELECT booking_id, booking_code, room_id, status "
                        + "FROM BOOKING WITH (UPDLOCK, HOLDLOCK) WHERE booking_id = ?";

                String bookingCode;
                long roomId;
                String status;

                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setLong(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("Booking not found.");
                        }
                        bookingCode = rs.getString("booking_code");
                        roomId = rs.getLong("room_id");
                        status = rs.getString("status");
                    }
                }

                if (Booking.STATUS_CONFIRMED.equalsIgnoreCase(status)) {
                    throw new IllegalStateException("Booking must be checked in first.");
                }
                if (!Booking.STATUS_CHECKED_IN.equalsIgnoreCase(status)) {
                    throw new IllegalStateException("Booking cannot be checked out because it is in status " + status + ".");
                }

                // Update booking
                String updBSql = "UPDATE BOOKING SET status = 'CHECKED_OUT', actual_checkout_at = SYSDATETIME(), "
                        + "updated_at = SYSDATETIME() WHERE booking_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updBSql)) {
                    ps.setLong(1, bookingId);
                    ps.executeUpdate();
                }

                // Update room to NEEDS_CLEANING
                String updRSql = "UPDATE ROOM SET status = 'NEEDS_CLEANING', updated_at = SYSDATETIME() WHERE room_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updRSql)) {
                    ps.setLong(1, roomId);
                    ps.executeUpdate();
                }

                // Expire live credentials
                String updCSql = "UPDATE ACCESS_CREDENTIAL SET status = 'EXPIRED' WHERE booking_id = ? AND status IN ('PENDING', 'ACTIVE')";
                try (PreparedStatement ps = conn.prepareStatement(updCSql)) {
                    ps.setLong(1, bookingId);
                    ps.executeUpdate();
                }

                LOGGER.info("Manual check-out for booking " + bookingCode + " by admin #" + adminUserId);

                conn.commit();
                websocket.RealtimeWebSocketEndpoint.broadcastBookingUpdated(bookingCode, "CHECKED_OUT", roomId);
                websocket.RealtimeWebSocketEndpoint.broadcastRoomStatusChanged(roomId, "", "NEEDS_CLEANING");
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Database error during check-out for booking " + bookingId, e);
                if (e instanceof SQLException) throw (SQLException) e;
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new SQLException("Manual check-out failed unexpectedly.", e);
            }
        }
    }

    /**
     * Releases held slots for a no-show expired booking (G2).
     * Returns true if slots were released, or false if slots were already released (no-op).
     */
    public boolean releaseNoshow(long adminUserId, long bookingId, String ip) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String lockSql = "SELECT booking_id, booking_code, status, expire_reason "
                        + "FROM BOOKING WITH (UPDLOCK, HOLDLOCK) WHERE booking_id = ?";

                String bookingCode;
                String status;
                String expireReason;

                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setLong(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("Booking not found.");
                        }
                        bookingCode = rs.getString("booking_code");
                        status = rs.getString("status");
                        expireReason = rs.getString("expire_reason");
                    }
                }

                if (!Booking.STATUS_EXPIRED.equalsIgnoreCase(status) || !"NO_SHOW".equalsIgnoreCase(expireReason)) {
                    throw new IllegalStateException("Only no-show expired bookings can have slots released.");
                }

                String countHeldSql = "SELECT COUNT(*) FROM BOOKING_SLOT WHERE booking_id = ? AND hold_status = 'HELD'";
                int heldCount = 0;
                try (PreparedStatement ps = conn.prepareStatement(countHeldSql)) {
                    ps.setLong(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) heldCount = rs.getInt(1);
                    }
                }

                if (heldCount == 0) {
                    conn.rollback();
                    return false; // No-op, already released
                }

                String updSlots = "UPDATE BOOKING_SLOT SET hold_status = 'RELEASED', released_at = SYSDATETIME() "
                        + "WHERE booking_id = ? AND hold_status = 'HELD'";
                try (PreparedStatement ps = conn.prepareStatement(updSlots)) {
                    ps.setLong(1, bookingId);
                    ps.executeUpdate();
                }

                // Revoke live credentials if any
                String revokeCredSql = "UPDATE ACCESS_CREDENTIAL SET status = 'REVOKED', revoke_reason = 'NO_SHOW', "
                        + "revoked_at = SYSDATETIME() "
                        + "WHERE booking_id = ? AND status IN ('PENDING', 'ACTIVE')";
                try (PreparedStatement psCred = conn.prepareStatement(revokeCredSql)) {
                    psCred.setLong(1, bookingId);
                    psCred.executeUpdate();
                }

                LOGGER.info("Released held slots for no-show booking " + bookingCode + " by admin #" + adminUserId);

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Database error releasing no-show slots for booking " + bookingId, e);
                if (e instanceof SQLException) throw (SQLException) e;
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new SQLException("Failed to release no-show slots.", e);
            }
        }
    }

    /**
     * Reinstates a no-show expired booking (G2) if slots are still held and end_at > now.
     * Transitions booking to CONFIRMED (expire_reason = NULL), generates new credential PIN.
     * Optionally marks latest PAID payment as REFUND_PENDING.
     */
    public void reinstateNoshow(long adminUserId, long bookingId, boolean refundPayment, String refundNote, String ip)
            throws SQLException {

        LocalDateTime now = LocalDateTime.now();

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String lockSql = "SELECT booking_id, booking_code, status, expire_reason, checkin_at, checkout_at "
                        + "FROM BOOKING WITH (UPDLOCK, HOLDLOCK) WHERE booking_id = ?";

                String bookingCode;
                String status;
                String expireReason;
                Timestamp checkinAtTs;
                Timestamp checkoutAtTs;

                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setLong(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("Booking not found.");
                        }
                        bookingCode = rs.getString("booking_code");
                        status = rs.getString("status");
                        expireReason = rs.getString("expire_reason");
                        checkinAtTs = rs.getTimestamp("checkin_at");
                        checkoutAtTs = rs.getTimestamp("checkout_at");
                    }
                }

                if (!Booking.STATUS_EXPIRED.equalsIgnoreCase(status) || !"NO_SHOW".equalsIgnoreCase(expireReason)) {
                    throw new IllegalStateException("Only no-show expired bookings can be reinstated.");
                }

                // Check that slots are still HELD and last slot end_at > now
                String checkSql = "SELECT COUNT(*), MAX(end_at) FROM BOOKING_SLOT WHERE booking_id = ? AND hold_status = 'HELD'";
                int heldCount = 0;
                Timestamp maxEndAt = null;

                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setLong(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            heldCount = rs.getInt(1);
                            maxEndAt = rs.getTimestamp(2);
                        }
                    }
                }

                if (heldCount == 0) {
                    throw new IllegalStateException("Cannot reinstate booking because slots have already been released.");
                }

                if (maxEndAt == null || !maxEndAt.toLocalDateTime().isAfter(now)) {
                    throw new IllegalStateException("Cannot reinstate booking after the last slot has ended.");
                }

                // Old live credential stays REVOKED; satisfy UX_ACCESS_CREDENTIAL_live_booking
                String revokeOldCredSql = "UPDATE ACCESS_CREDENTIAL SET status = 'REVOKED', revoke_reason = 'NO_SHOW', "
                        + "revoked_at = SYSDATETIME() "
                        + "WHERE booking_id = ? AND status IN ('PENDING', 'ACTIVE')";
                try (PreparedStatement psRev = conn.prepareStatement(revokeOldCredSql)) {
                    psRev.setLong(1, bookingId);
                    psRev.executeUpdate();
                }

                // Transition booking to CONFIRMED with expire_reason NULL
                String updBSql = "UPDATE BOOKING SET status = 'CONFIRMED', expire_reason = NULL, "
                        + "checkout_at = ?, updated_at = SYSDATETIME() "
                        + "WHERE booking_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updBSql)) {
                    ps.setTimestamp(1, maxEndAt);
                    ps.setLong(2, bookingId);
                    ps.executeUpdate();
                }

                // Refresh start_at to now for held slots that started in the past, granting fresh grace period
                String updSlots = "UPDATE BOOKING_SLOT SET start_at = SYSDATETIME() "
                        + "WHERE booking_id = ? AND hold_status = 'HELD' AND start_at < SYSDATETIME()";
                try (PreparedStatement ps = conn.prepareStatement(updSlots)) {
                    ps.setLong(1, bookingId);
                    ps.executeUpdate();
                }

                // Generate NEW credential PIN valid from now until the last slot ends
                String deviceId = AppConstants.MAIN_DOOR_DEVICE_ID;
                LocalDateTime validFrom = now;
                LocalDateTime validUntil = (maxEndAt != null) ? maxEndAt.toLocalDateTime() : now.plusHours(2);
                credentialService.createGuestPin(conn, bookingId, deviceId, validFrom, validUntil);

                // Optional refund
                if (refundPayment) {
                    String cleanNote = (refundNote != null && !refundNote.trim().isEmpty())
                            ? refundNote.trim() : "Flagged for refund on no-show reinstatement";
                    String updPay = "UPDATE PAYMENT SET status = 'REFUND_PENDING', resolution_note = ?, "
                            + "resolved_by = ?, resolved_at = SYSDATETIME(), updated_at = SYSDATETIME() "
                            + "WHERE payment_id = (SELECT TOP 1 payment_id FROM PAYMENT WHERE booking_id = ? AND status = 'PAID' ORDER BY payment_id DESC)";
                    try (PreparedStatement ps = conn.prepareStatement(updPay)) {
                        ps.setString(1, cleanNote);
                        ps.setLong(2, adminUserId);
                        ps.setLong(3, bookingId);
                        ps.executeUpdate();
                    }

                    LOGGER.info("Refund marked on reinstatement for booking " + bookingCode + ": " + cleanNote);
                }

                LOGGER.info("Reinstated no-show booking " + bookingCode + " by admin #" + adminUserId + " with newly issued PIN");

                conn.commit();
                websocket.RealtimeWebSocketEndpoint.broadcastBookingUpdated(bookingCode, "CONFIRMED", null);
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Database error reinstating no-show booking " + bookingId, e);
                if (e instanceof SQLException) throw (SQLException) e;
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new SQLException("Failed to reinstate no-show booking.", e);
            }
        }
    }

    /**
     * Manually revokes a live door PIN credential (CREDENTIAL_REVOKE).
     */
    public void revokeCredential(long adminUserId, long credentialId, String reason, String ip) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String lockSql = "SELECT credential_id, booking_id, status FROM ACCESS_CREDENTIAL WITH (UPDLOCK, HOLDLOCK) WHERE credential_id = ?";
                Long bookingId;
                String status;

                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setLong(1, credentialId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("Credential not found.");
                        }
                        long bId = rs.getLong("booking_id");
                        bookingId = rs.wasNull() ? null : bId;
                        status = rs.getString("status");
                    }
                }

                if (!"PENDING".equalsIgnoreCase(status) && !"ACTIVE".equalsIgnoreCase(status)) {
                    throw new IllegalStateException("Only pending or active credentials can be revoked.");
                }

                String cleanReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "ADMIN_REVOKE";
                if (cleanReason.length() > 50) {
                    cleanReason = cleanReason.substring(0, 50);
                }

                String updSql = "UPDATE ACCESS_CREDENTIAL SET status = 'REVOKED', revoke_reason = 'ADMIN_REVOKE', "
                        + "revoked_at = SYSDATETIME() WHERE credential_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updSql)) {
                    ps.setLong(1, credentialId);
                    ps.executeUpdate();
                }

                LOGGER.info("Admin #" + adminUserId + " manually revoked credential #" + credentialId + ": " + cleanReason);

                conn.commit();
                websocket.RealtimeWebSocketEndpoint.broadcastEvent("CREDENTIAL_REVOKED", null);
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Database error revoking credential " + credentialId, e);
                if (e instanceof SQLException) throw (SQLException) e;
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new SQLException("Failed to revoke credential.", e);
            }
        }
    }
}