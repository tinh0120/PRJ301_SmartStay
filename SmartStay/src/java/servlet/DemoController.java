package servlet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import filter.DemoModeFilter;
import job.FastCloseJobWorker;
import job.JobCoordinator;
import job.SchedulerJobs;
import job.SlowWorker;
import model.User.AuthUser;
import service.BookingService;
import service.PaymentService;
import util.AppConstants;
import util.ClockService;
import util.DBUtil;
import util.DemoTime;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Demo tools API (Task R2-B2). JSON only, no UI.
 * <ul>
 *   <li>POST /demo/time/jump      bookingCode, target [, dryRun=true]</li>
 *   <li>POST /demo/time/reset</li>
 *   <li>GET  /demo/state</li>
 *   <li>POST /demo/payment/simulate bookingCode [, result=SUCCESS|FAILED]</li>
 * </ul>
 * Hidden (404) when demo.mode=false (see DemoModeFilter). POST requires the CSRF token
 * (_csrf parameter or X-CSRF-TOKEN header, enforced by CsrfFilter). Allowed: the booking's
 * guest, OWNER and ADMIN.
 */
@WebServlet(name = "DemoController", urlPatterns = {"/demo/*"})
public class DemoController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DemoController.class.getName());

    /** Max wait for the job lock after a jump before answering 409. */
    static final long LOCK_TIMEOUT_MS = 5000L;
    /** Extra window after the new time so upcoming 30-minute reminders count as affected. */
    private static final int AFFECTED_LOOKAHEAD_MINUTES = 30;
    private static final int MAX_LISTED = 10;
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BookingService bookingService = new BookingService();
    private final PaymentService paymentService = new PaymentService();

    /** Error with an HTTP status, rendered as {success:false, error, message}. */
    private static final class DemoException extends Exception {
        final int status;
        final String code;

        DemoException(int status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }
    }

    /** Booking row needed by the demo endpoints. */
    private static final class BookingInfo {
        long bookingId;
        String bookingCode;
        long guestId;
        long roomId;
        String roomNumber;
        String roomName;
        String roomStatus;
        String status;
        LocalDateTime firstStart;
        LocalDateTime lastEnd;
    }

    // =========================================================================
    // ROUTING
    // =========================================================================

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = pathOf(req);
        if (!DemoModeFilter.isDemoMode(getServletContext())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            AuthUser user = requireDemoUser(req);
            if ("/state".equals(path)) {
                writeJson(resp, HttpServletResponse.SC_OK, handleState(user));
            } else if (isPostRoute(path)) {
                throw new DemoException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Use POST for " + path + ".");
            } else {
                throw new DemoException(HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Unknown demo endpoint.");
            }
        } catch (DemoException e) {
            writeError(resp, e);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Demo GET " + path + " failed", e);
            writeError(resp, new DemoException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB_ERROR", "Database error."));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = pathOf(req);
        if (!DemoModeFilter.isDemoMode(getServletContext())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            AuthUser user = requireDemoUser(req);
            JsonObject body;
            if ("/time/jump".equals(path)) {
                body = handleJump(req, user);
            } else if ("/time/reset".equals(path)) {
                body = handleReset(user);
            } else if ("/payment/simulate".equals(path)) {
                body = handlePaymentSimulate(req, user);
            } else if ("/state".equals(path)) {
                throw new DemoException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Use GET for /state.");
            } else {
                throw new DemoException(HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Unknown demo endpoint.");
            }
            writeJson(resp, HttpServletResponse.SC_OK, body);
        } catch (DemoException e) {
            writeError(resp, e);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Demo POST " + path + " failed", e);
            writeError(resp, new DemoException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB_ERROR", "Database error."));
        }
    }

    private static String pathOf(HttpServletRequest req) {
        String p = req.getPathInfo();
        return (p == null) ? "" : p;
    }

    private static boolean isPostRoute(String path) {
        return "/time/jump".equals(path) || "/time/reset".equals(path) || "/payment/simulate".equals(path);
    }

    // =========================================================================
    // GET /demo/state
    // =========================================================================

    private JsonObject handleState(AuthUser user) throws SQLException {
        JsonObject o = clockJson();
        o.addProperty("success", true);

        boolean staffSide = isOwnerOrAdmin(user);
        String sql = "SELECT TOP 50 b.booking_code, b.status, b.checkin_at, b.checkout_at, "
                + "r.room_number, r.display_name "
                + "FROM BOOKING b JOIN ROOM r ON b.room_id = r.room_id "
                + "WHERE b.status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN') "
                + (staffSide ? "" : "AND b.guest_id = ? ")
                + "ORDER BY b.checkin_at ASC, b.booking_id ASC";

        JsonArray arr = new JsonArray();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!staffSide) {
                ps.setLong(1, user.getUserId());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject b = new JsonObject();
                    b.addProperty("code", rs.getString("booking_code"));
                    b.addProperty("room", rs.getString("room_number") + " · " + rs.getString("display_name"));
                    b.addProperty("status", rs.getString("status"));
                    addTime(b, "checkin", toLdt(rs.getTimestamp("checkin_at")));
                    addTime(b, "checkout", toLdt(rs.getTimestamp("checkout_at")));
                    arr.add(b);
                }
            }
        }
        o.add("bookings", arr);
        return o;
    }

    // =========================================================================
    // POST /demo/time/jump
    // =========================================================================

    private JsonObject handleJump(HttpServletRequest req, AuthUser user) throws DemoException, SQLException {
        String bookingCode = requireParam(req, "bookingCode");
        final DemoTime.Target target = DemoTime.Target.parse(req.getParameter("target"));
        if (target == null) {
            throw new DemoException(HttpServletResponse.SC_BAD_REQUEST, "INVALID_TARGET",
                    "target must be one of BEFORE_CHECKIN_15, CHECKIN_NOW, BEFORE_CHECKOUT_15, CHECKOUT_NOW.");
        }
        boolean dryRun = "true".equalsIgnoreCase(req.getParameter("dryRun"));
        final String ip = req.getRemoteAddr();
        final long actorId = user.getUserId();

        BookingInfo b = loadBookingForActor(bookingCode, user);
        final LocalDateTime targetAt = DemoTime.targetMoment(target, b.firstStart, b.lastEnd);
        final LocalDateTime before = ClockService.now();
        checkJumpAllowed(b, target, targetAt, before);
        final boolean checkInFirst = target.isCheckout() && "CONFIRMED".equalsIgnoreCase(b.status);

        if (dryRun) {
            JsonObject o = clockJson();
            o.addProperty("success", true);
            o.addProperty("dryRun", true);
            o.addProperty("bookingCode", b.bookingCode);
            o.addProperty("target", target.name());
            addTime(o, "targetAt", targetAt);
            o.addProperty("checkInFirst", checkInFirst);
            addAffected(o, b.bookingId, before, targetAt);
            return o;
        }

        final long bookingId = b.bookingId;
        final LocalDateTime firstStart = b.firstStart;
        final DemoException[] failure = new DemoException[1];
        final SQLException[] dbFailure = new SQLException[1];
        final List<SchedulerJobs.PendingMail> mails = new ArrayList<>();

        boolean ran = JobCoordinator.tryRunExclusive(LOCK_TIMEOUT_MS, new Runnable() {
            @Override
            public void run() {
                try {
                    // Re-check under the lock: a job may have changed the booking meanwhile.
                    BookingInfo fresh = loadBooking(bookingCode);
                    checkJumpAllowed(fresh, target, targetAt, ClockService.now());

                    if (target.isCheckout() && "CONFIRMED".equalsIgnoreCase(fresh.status)) {
                        // Check in first so the stay is not treated as a no-show.
                        if (ClockService.now().isBefore(firstStart)) {
                            ClockService.advanceTo(firstStart);
                        }
                        try {
                            bookingService.checkInManual(actorId, bookingId, ip);
                        } catch (IllegalStateException e) {
                            throw new DemoException(HttpServletResponse.SC_CONFLICT, "CHECKIN_FAILED",
                                    "Could not check the guest in before jumping: " + e.getMessage());
                        }
                    }

                    ClockService.advanceTo(targetAt);

                    // One job cycle right away; e-mails are only collected here.
                    FastCloseJobWorker.runJobs();
                    mails.addAll(SlowWorker.runJobsCollectingMails());
                } catch (DemoException e) {
                    failure[0] = e;
                } catch (SQLException e) {
                    dbFailure[0] = e;
                } catch (IllegalArgumentException e) {
                    failure[0] = new DemoException(HttpServletResponse.SC_BAD_REQUEST, "TARGET_IN_PAST", e.getMessage());
                }
            }
        });

        if (!ran) {
            throw new DemoException(HttpServletResponse.SC_CONFLICT, "JOBS_BUSY",
                    "Background jobs are running. Nothing was changed, try again in a few seconds.");
        }
        if (dbFailure[0] != null) {
            throw dbFailure[0];
        }
        if (failure[0] != null) {
            throw failure[0];
        }

        // Outside the lock: e-mail failures must not abort the jump.
        try {
            SchedulerJobs.sendPendingMails(mails);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Demo jump: sending reminder e-mails failed", e);
        }

        BookingInfo after = loadBooking(bookingCode);
        JsonObject o = clockJson();
        o.addProperty("success", true);
        o.addProperty("bookingCode", after.bookingCode);
        o.addProperty("target", target.name());
        addTime(o, "targetAt", targetAt);
        o.addProperty("checkedInFirst", checkInFirst);
        o.addProperty("bookingStatus", after.status);
        o.addProperty("roomStatus", after.roomStatus);
        o.addProperty("remindersQueued", mails.size());
        List<BookingInfo> affected = addAffected(o, after.bookingId, before, targetAt);

        broadcast(after, affected);
        LOGGER.info("Demo jump by user #" + actorId + ": " + after.bookingCode + " -> " + target
                + " (simulated now " + ClockService.now() + ")");
        return o;
    }

    /** Status and forward-only rules shared by the dry run and the real jump. */
    private static void checkJumpAllowed(BookingInfo b, DemoTime.Target target, LocalDateTime targetAt,
                                         LocalDateTime virtualNow) throws DemoException {
        if ("PENDING_PAYMENT".equalsIgnoreCase(b.status)) {
            throw new DemoException(HttpServletResponse.SC_CONFLICT, "NOT_PAID",
                    "Pay for booking " + b.bookingCode + " first (use the payment buttons).");
        }
        if (!"CONFIRMED".equalsIgnoreCase(b.status) && !"CHECKED_IN".equalsIgnoreCase(b.status)) {
            throw new DemoException(HttpServletResponse.SC_CONFLICT, "BOOKING_CLOSED",
                    "Booking " + b.bookingCode + " is " + b.status + ", time travel is only for confirmed or checked-in stays.");
        }
        if (!DemoTime.isReachable(targetAt, virtualNow)) {
            throw new DemoException(HttpServletResponse.SC_BAD_REQUEST, "TARGET_IN_PAST",
                    "That moment (" + targetAt.format(DISPLAY) + ") is before the simulated time ("
                    + virtualNow.format(DISPLAY) + "). Time travel only goes forward.");
        }
    }

    // =========================================================================
    // POST /demo/time/reset
    // =========================================================================

    private JsonObject handleReset(AuthUser user) {
        long previous = ClockService.offsetMinutes();
        ClockService.reset();
        LOGGER.info("Demo clock reset by user #" + user.getUserId() + " (offset was " + previous + " min)");
        JsonObject o = clockJson();
        o.addProperty("success", true);
        o.addProperty("previousOffsetMinutes", previous);
        o.addProperty("message", "Clock reset to real time. Data changes made while time was shifted are not undone.");
        return o;
    }

    // =========================================================================
    // POST /demo/payment/simulate
    // =========================================================================

    private JsonObject handlePaymentSimulate(HttpServletRequest req, AuthUser user) throws DemoException, SQLException {
        String bookingCode = requireParam(req, "bookingCode");
        String result = req.getParameter("result");
        result = (result == null || result.trim().isEmpty()) ? "SUCCESS" : result.trim().toUpperCase();
        if (!"SUCCESS".equals(result) && !"FAILED".equals(result)) {
            throw new DemoException(HttpServletResponse.SC_BAD_REQUEST, "INVALID_RESULT", "result must be SUCCESS or FAILED.");
        }

        BookingInfo b = loadBookingForActor(bookingCode, user);

        long orderCode = -1;
        long amount = 0;
        String provider = null;
        String payStatus = null;
        String sql = "SELECT TOP 1 provider, provider_order_code, amount, status FROM PAYMENT "
                + "WHERE booking_id = ? ORDER BY payment_id DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, b.bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    provider = rs.getString("provider");
                    orderCode = rs.getLong("provider_order_code");
                    amount = rs.getBigDecimal("amount").longValue();
                    payStatus = rs.getString("status");
                }
            }
        }
        if (orderCode <= 0 || !"PENDING".equalsIgnoreCase(payStatus)) {
            throw new DemoException(HttpServletResponse.SC_CONFLICT, "NO_PENDING_PAYMENT",
                    "Booking " + b.bookingCode + " has no pending payment link. Open checkout first.");
        }
        if (!"MOCK".equalsIgnoreCase(provider) || !"MOCK".equalsIgnoreCase(AppConstants.PAYMENT_PROVIDER)) {
            throw new DemoException(HttpServletResponse.SC_CONFLICT, "NOT_MOCK_PAYMENT", "Only sandbox (MOCK) payments can be simulated.");
        }

        // Same entry point as /mock-payos/simulate.
        paymentService.processResult(orderCode, amount, result, "TXN_DEMO_" + orderCode, req.getRemoteAddr());

        String newPayStatus = null;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM PAYMENT WHERE provider_order_code = ?")) {
            ps.setLong(1, orderCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    newPayStatus = rs.getString("status");
                }
            }
        }
        BookingInfo after = loadBooking(bookingCode);

        JsonObject o = new JsonObject();
        o.addProperty("success", true);
        o.addProperty("bookingCode", after.bookingCode);
        o.addProperty("result", result);
        o.addProperty("paymentStatus", newPayStatus);
        o.addProperty("bookingStatus", after.status);
        o.addProperty("redirectUrl", "/guest/payment/result?bookingId=" + after.bookingId);
        return o;
    }

    // =========================================================================
    // DATA HELPERS
    // =========================================================================

    private static BookingInfo loadBooking(String bookingCode) throws DemoException, SQLException {
        String sql = "SELECT b.booking_id, b.booking_code, b.guest_id, b.room_id, b.status, "
                + "b.checkin_at, b.checkout_at, r.room_number, r.display_name, r.status AS room_status "
                + "FROM BOOKING b JOIN ROOM r ON b.room_id = r.room_id WHERE b.booking_code = ?";
        BookingInfo b = null;
        try (Connection conn = DBUtil.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, bookingCode.trim().toUpperCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        b = new BookingInfo();
                        b.bookingId = rs.getLong("booking_id");
                        b.bookingCode = rs.getString("booking_code");
                        b.guestId = rs.getLong("guest_id");
                        b.roomId = rs.getLong("room_id");
                        b.status = rs.getString("status");
                        b.roomNumber = rs.getString("room_number");
                        b.roomName = rs.getString("display_name");
                        b.roomStatus = rs.getString("room_status");
                        b.firstStart = toLdt(rs.getTimestamp("checkin_at"));
                        b.lastEnd = toLdt(rs.getTimestamp("checkout_at"));
                    }
                }
            }
            if (b == null) {
                throw new DemoException(HttpServletResponse.SC_NOT_FOUND, "BOOKING_NOT_FOUND", "Booking " + bookingCode + " was not found.");
            }
            // Stay window from the booked slots (first slot start / last slot end).
            List<LocalDateTime[]> windows = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT start_at, end_at FROM BOOKING_SLOT WHERE booking_id = ?")) {
                ps.setLong(1, b.bookingId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        windows.add(new LocalDateTime[]{toLdt(rs.getTimestamp("start_at")), toLdt(rs.getTimestamp("end_at"))});
                    }
                }
            }
            if (!windows.isEmpty()) {
                b.firstStart = DemoTime.firstStart(windows);
                b.lastEnd = DemoTime.lastEnd(windows);
            }
        }
        return b;
    }

    /** Loads the booking and enforces ownership: guests only reach their own bookings. */
    private static BookingInfo loadBookingForActor(String bookingCode, AuthUser user) throws DemoException, SQLException {
        BookingInfo b = loadBooking(bookingCode);
        if (!isOwnerOrAdmin(user) && b.guestId != user.getUserId()) {
            throw new DemoException(HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "This booking does not belong to you.");
        }
        return b;
    }

    /**
     * Other active bookings whose check-in or check-out falls in (from, to + 30 min]: the global
     * clock jump moves them too (activation, no-show, auto checkout, reminders).
     */
    private static List<BookingInfo> findAffected(long excludeBookingId, LocalDateTime from, LocalDateTime to) throws SQLException {
        Timestamp fromTs = Timestamp.valueOf(from);
        Timestamp toTs = Timestamp.valueOf(to.plusMinutes(AFFECTED_LOOKAHEAD_MINUTES));
        String sql = "SELECT b.booking_id, b.booking_code, b.status, b.room_id, b.checkin_at, b.checkout_at, "
                + "r.room_number, r.display_name, r.status AS room_status "
                + "FROM BOOKING b JOIN ROOM r ON b.room_id = r.room_id "
                + "WHERE b.booking_id <> ? AND b.status IN ('CONFIRMED', 'CHECKED_IN') "
                + "  AND ((b.checkin_at > ? AND b.checkin_at <= ?) OR (b.checkout_at > ? AND b.checkout_at <= ?)) "
                + "ORDER BY b.checkin_at ASC";
        List<BookingInfo> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, excludeBookingId);
            ps.setTimestamp(2, fromTs);
            ps.setTimestamp(3, toTs);
            ps.setTimestamp(4, fromTs);
            ps.setTimestamp(5, toTs);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookingInfo b = new BookingInfo();
                    b.bookingId = rs.getLong("booking_id");
                    b.bookingCode = rs.getString("booking_code");
                    b.status = rs.getString("status");
                    b.roomId = rs.getLong("room_id");
                    b.roomNumber = rs.getString("room_number");
                    b.roomName = rs.getString("display_name");
                    b.roomStatus = rs.getString("room_status");
                    b.firstStart = toLdt(rs.getTimestamp("checkin_at"));
                    b.lastEnd = toLdt(rs.getTimestamp("checkout_at"));
                    list.add(b);
                }
            }
        }
        return list;
    }

    private static List<BookingInfo> addAffected(JsonObject o, long bookingId, LocalDateTime from, LocalDateTime to) throws SQLException {
        List<BookingInfo> affected = findAffected(bookingId, from, to);
        JsonArray arr = new JsonArray();
        for (int i = 0; i < affected.size() && i < MAX_LISTED; i++) {
            BookingInfo a = affected.get(i);
            JsonObject j = new JsonObject();
            j.addProperty("code", a.bookingCode);
            j.addProperty("room", a.roomNumber + " · " + a.roomName);
            j.addProperty("status", a.status);
            addTime(j, "checkin", a.firstStart);
            addTime(j, "checkout", a.lastEnd);
            arr.add(j);
        }
        o.addProperty("affectedCount", affected.size());
        o.add("affectedBookings", arr);
        return affected;
    }

    /** Pushes the new states to open pages (booking pages, dashboard, room tiles). */
    private static void broadcast(BookingInfo main, List<BookingInfo> affected) {
        try {
            Map<Long, BookingInfo> rooms = new LinkedHashMap<>();
            websocket.RealtimeWebSocketEndpoint.broadcastBookingUpdated(main.bookingCode, main.status, main.roomId);
            rooms.put(main.roomId, main);
            for (BookingInfo a : affected) {
                // Re-read: the job cycle may have changed the other bookings as well.
                BookingInfo fresh = loadBooking(a.bookingCode);
                websocket.RealtimeWebSocketEndpoint.broadcastBookingUpdated(fresh.bookingCode, fresh.status, fresh.roomId);
                rooms.put(fresh.roomId, fresh);
            }
            for (BookingInfo r : rooms.values()) {
                websocket.RealtimeWebSocketEndpoint.broadcastRoomStatusChanged(r.roomId, r.roomNumber, r.roomStatus);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Demo jump: realtime broadcast failed", e);
        }
    }

    // =========================================================================
    // AUTH + JSON HELPERS
    // =========================================================================

    /** Guests, owners and admins may use the demo tools; staff may not. */
    private static AuthUser requireDemoUser(HttpServletRequest req) throws DemoException {
        HttpSession session = req.getSession(false);
        AuthUser user = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        if (user == null) {
            throw new DemoException(HttpServletResponse.SC_UNAUTHORIZED, "LOGIN_REQUIRED", "Please log in.");
        }
        String role = user.getRoleName();
        if (!"GUEST".equalsIgnoreCase(role) && !isOwnerOrAdmin(user)) {
            throw new DemoException(HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "Demo tools are not available for your role.");
        }
        return user;
    }

    private static boolean isOwnerOrAdmin(AuthUser user) {
        String role = user.getRoleName();
        return "OWNER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }

    private static String requireParam(HttpServletRequest req, String name) throws DemoException {
        String v = req.getParameter(name);
        if (v == null || v.trim().isEmpty()) {
            throw new DemoException(HttpServletResponse.SC_BAD_REQUEST, "MISSING_PARAM", name + " is required.");
        }
        return v.trim();
    }

    private static JsonObject clockJson() {
        JsonObject o = new JsonObject();
        o.addProperty("offsetMinutes", ClockService.offsetMinutes());
        o.addProperty("shifted", ClockService.isShifted());
        addTime(o, "simulatedNow", ClockService.now());
        addTime(o, "realNow", ClockService.realNow());
        return o;
    }

    /** Adds "name" (ISO, seconds precision) and "nameDisplay" (dd/MM/yyyy HH:mm). */
    private static void addTime(JsonObject o, String name, LocalDateTime t) {
        if (t == null) {
            o.add(name, null);
            return;
        }
        LocalDateTime s = t.withNano(0);
        o.addProperty(name, s.toString());
        o.addProperty(name + "Display", s.format(DISPLAY));
    }

    private static LocalDateTime toLdt(Timestamp ts) {
        return (ts != null) ? ts.toLocalDateTime() : null;
    }

    private static void writeJson(HttpServletResponse resp, int status, JsonObject body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json; charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(body.toString());
    }

    private static void writeError(HttpServletResponse resp, DemoException e) throws IOException {
        JsonObject o = new JsonObject();
        o.addProperty("success", false);
        o.addProperty("error", e.code);
        o.addProperty("message", e.getMessage());
        writeJson(resp, e.status, o);
    }
}
