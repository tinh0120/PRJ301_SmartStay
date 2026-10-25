package servlet;

import com.google.gson.Gson;
import dao.BookingDao;
import dao.ReviewDao;
import model.Booking;
import model.BookingDetail;
import model.Booking.Quote;
import model.Review;
import model.RoomSlot;
import service.BookingService;
import service.CredentialService;
import service.ServiceException;
import service.RoomService;
import util.AppConstants;
import util.SecurityUtil;
import util.ElFunctions;
import util.Flash;
import util.PageResult.PageRequest;
import util.PageResult;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
 * Controller consolidating guest booking management: listing, creation, quotation calculation, detail inspection, and cancellation.
 */
@WebServlet(name = "GuestBookingServlet", urlPatterns = {
    "/guest/bookings",
    "/guest/bookings/detail",
    "/guest/bookings/cancel",
    "/guest/bookings/quote"
})
public class GuestBookingServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(GuestBookingServlet.class.getName());
    private static final String[] SORT_KEYS = {"date", "created", "total", "status", "room"};
    private static final List<String> STATUS_OPTIONS = Arrays.asList(
            "PENDING_PAYMENT", "CONFIRMED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED", "EXPIRED"
    );

    public static final String DISCLAIMER_TEXT = "The system confirms main-door access only. It cannot verify which room you enter.";

    private final BookingDao bookingDao = new BookingDao();
    private final BookingService bookingService = new BookingService();
    private final CredentialService credentialService = new CredentialService();
    private final ReviewDao reviewDao = new ReviewDao();
    private final RoomService catalogService = new RoomService();
    private final Gson gson = new Gson();

    /**
     * Helper method to populate booking detail view attributes (shared with PinViewServlet and ReviewCreateServlet).
     */
    public boolean populateDetailAttributes(HttpServletRequest req, long bookingId, long guestId)
            throws SQLException {
        BookingDetail detail = bookingDao.findDetail(bookingId);
        if (detail == null || detail.getBooking() == null || detail.getBooking().getGuestId() != guestId) {
            return false;
        }

        Booking b = detail.getBooking();
        long remainingMinutes = 0;
        if (Booking.STATUS_PENDING_PAYMENT.equalsIgnoreCase(b.getStatus()) && b.getPaymentDeadline() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(b.getPaymentDeadline())) {
                remainingMinutes = Math.max(1, ChronoUnit.MINUTES.between(now, b.getPaymentDeadline()));
            }
        }

        String areaInstruction = AppConstants.KEY_AREA_INSTRUCTION;
        req.setAttribute("areaInstruction", areaInstruction);
        req.setAttribute("disclaimer", DISCLAIMER_TEXT);

        CredentialService.CredentialInfo cred = credentialService.getCredentialForBooking(bookingId);
        req.setAttribute("credential", cred);
        boolean pinAvailable = (cred != null
                && ("PENDING".equalsIgnoreCase(cred.getStatus()) || "ACTIVE".equalsIgnoreCase(cred.getStatus())));
        req.setAttribute("pinAvailable", pinAvailable);
        req.setAttribute("pinStatus", (cred != null) ? cred.getStatus() : "NONE");

        Review userReview = reviewDao.findByBookingId(bookingId);
        req.setAttribute("userReview", userReview);
        boolean canReview = ("CHECKED_OUT".equalsIgnoreCase(b.getStatus()) && userReview == null);
        req.setAttribute("canReview", canReview);

        req.setAttribute("detail", detail);
        req.setAttribute("remainingMinutes", remainingMinutes);

        HttpSession session = req.getSession(false);
        if (session != null) {
            req.setAttribute("_csrf", SecurityUtil.tokenForSession(session));
        }

        return true;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/guest/bookings".equals(path)) {
            listBookings(req, resp);
            return;
        }
        if ("/guest/bookings/detail".equals(path)) {
            showDetail(req, resp);
            return;
        }
        if ("/guest/bookings/quote".equals(path)) {
            handleQuote(req, resp);
            return;
        }
        if ("/guest/bookings/cancel".equals(path)) {
            resp.sendRedirect(req.getContextPath() + "/guest/bookings");
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/guest/bookings".equals(path)) {
            createBooking(req, resp);
            return;
        }
        if ("/guest/bookings/cancel".equals(path)) {
            cancelBooking(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void listBookings(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object uidObj = (session != null) ? session.getAttribute("userId") : null;
        if (!(uidObj instanceof Long)) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        long guestId = (Long) uidObj;

        PageRequest pr = PageRequest.from(req, SORT_KEYS, "date", "DESC");
        Map<String, String> errors = new HashMap<>();

        String fromStr = pr.getFilter("from");
        String toStr = pr.getFilter("to");
        LocalDate fromDate = null;
        LocalDate toDate = null;

        if (!fromStr.isEmpty()) {
            try {
                fromDate = LocalDate.parse(fromStr.trim());
            } catch (DateTimeParseException e) {
                errors.put("from", "Invalid 'from' date format (expected YYYY-MM-DD).");
                pr.setFilter("from", null);
            }
        }

        if (!toStr.isEmpty()) {
            try {
                toDate = LocalDate.parse(toStr.trim());
            } catch (DateTimeParseException e) {
                errors.put("to", "Invalid 'to' date format (expected YYYY-MM-DD).");
                pr.setFilter("to", null);
            }
        }

        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                errors.put("from", "'From' date must be before or equal to 'to' date.");
                pr.setFilter("from", null);
            } else if (ChronoUnit.DAYS.between(fromDate, toDate) > 365) {
                errors.put("range", "Date range cannot exceed 365 days.");
                pr.setFilter("from", null);
            }
        }

        String status = pr.getFilter("status");
        if (!status.isEmpty() && !STATUS_OPTIONS.contains(status)) {
            errors.put("status", "Invalid status filter.");
            pr.setFilter("status", null);
        }

        PageResult<Booking> pageResult;
        try {
            pageResult = bookingDao.findPageByGuest(pr, guestId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load guest bookings for guest " + guestId, e);
            pageResult = new PageResult<>(Collections.<Booking>emptyList(), 0, pr);
            req.setAttribute("dbError", "Failed to load bookings from database.");
        }

        req.setAttribute("result", pageResult);
        req.setAttribute("pageResult", pageResult);
        req.setAttribute("pr", pr);
        req.setAttribute("statuses", STATUS_OPTIONS);
        req.setAttribute("statusOptions", STATUS_OPTIONS);
        req.setAttribute("errors", errors);

        req.getRequestDispatcher("/WEB-INF/views/guest/bookings.jsp").forward(req, resp);
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object uidObj = (session != null) ? session.getAttribute("userId") : null;
        if (!(uidObj instanceof Long)) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        long guestId = (Long) uidObj;

        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Booking ID is required.");
            return;
        }

        long bookingId;
        try {
            bookingId = Long.parseLong(idStr.trim());
            if (bookingId <= 0) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid booking ID.");
                return;
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid booking ID format.");
            return;
        }

        try {
            boolean found = populateDetailAttributes(req, bookingId, guestId);
            if (!found) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Booking not found.");
                return;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error loading booking detail for ID " + bookingId, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Booking detail could not be loaded.");
            return;
        }

        req.getRequestDispatcher("/WEB-INF/views/guest/booking-detail.jsp").forward(req, resp);
    }

    private void createBooking(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object uidObj = (session != null) ? session.getAttribute("userId") : null;
        if (!(uidObj instanceof Long)) {
            Flash.error(req, "Your session has expired. Please log in again.");
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        long guestId = (Long) uidObj;

        String roomIdStr = req.getParameter("roomId");
        long roomId = -1;
        if (roomIdStr != null && !roomIdStr.trim().isEmpty()) {
            try {
                roomId = Long.parseLong(roomIdStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String slotDateStr = req.getParameter("slotDate");
        LocalDate slotDate = null;
        if (slotDateStr != null && !slotDateStr.trim().isEmpty()) {
            try {
                slotDate = LocalDate.parse(slotDateStr.trim());
            } catch (DateTimeParseException ignored) {
            }
        }

        List<String> slotCodes = BookingService.parseSlotCodes(req);
        String note = req.getParameter("note");

        if (roomId <= 0 || slotDate == null) {
            Flash.error(req, "Please select a valid room and date.");
            resp.sendRedirect(req.getContextPath() + "/rooms");
            return;
        }

        try {
            Booking booking = bookingService.createBooking(guestId, roomId, slotDate, slotCodes, req.getRemoteAddr());
            Flash.success(req, "Booking " + booking.getBookingCode() + " created successfully! Please complete payment within 15 minutes.");
            resp.sendRedirect(req.getContextPath() + "/guest/bookings/detail?id=" + booking.getBookingId());
        } catch (ServiceException pe) {
            Flash.error(req, pe.getMessage());
            resp.sendRedirect(req.getContextPath() + "/rooms/detail?id=" + roomId);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "Database error creating booking for guest " + guestId, se);
            Flash.error(req, "Failed to create booking due to a database error. Please try again.");
            resp.sendRedirect(req.getContextPath() + "/rooms/detail?id=" + roomId);
        }
    }

    private void cancelBooking(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object uidObj = (session != null) ? session.getAttribute("userId") : null;
        if (!(uidObj instanceof Long)) {
            Flash.error(req, "Your session has expired. Please log in again.");
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        long guestId = (Long) uidObj;

        String idStr = req.getParameter("id");
        long bookingId = -1;
        if (idStr != null && !idStr.trim().isEmpty()) {
            try {
                bookingId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        if (bookingId <= 0) {
            Flash.error(req, "Invalid booking ID.");
            resp.sendRedirect(req.getContextPath() + "/guest/bookings");
            return;
        }

        String reason = req.getParameter("reason");

        try {
            bookingService.cancelByGuest(bookingId, guestId, reason, req.getRemoteAddr());
            Flash.success(req, "Booking cancelled successfully.");
            resp.sendRedirect(req.getContextPath() + "/guest/bookings/detail?id=" + bookingId);
        } catch (ServiceException pe) {
            Flash.error(req, pe.getMessage());
            resp.sendRedirect(req.getContextPath() + "/guest/bookings/detail?id=" + bookingId);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "Database error cancelling booking ID " + bookingId, se);
            Flash.error(req, "Booking could not be cancelled. Please try again later.");
            resp.sendRedirect(req.getContextPath() + "/guest/bookings/detail?id=" + bookingId);
        }
    }

    // =========================================================================
    // DYNAMIC QUOTATION (AJAX)
    // =========================================================================

    private void handleQuote(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        String roomIdStr = req.getParameter("roomId");
        long roomId;
        if (roomIdStr == null || roomIdStr.trim().isEmpty()) {
            writeQuoteError(resp, HttpServletResponse.SC_BAD_REQUEST, "BAD_REQUEST", "Invalid request.");
            return;
        }
        try {
            roomId = Long.parseLong(roomIdStr.trim());
            if (roomId <= 0) {
                writeQuoteError(resp, HttpServletResponse.SC_BAD_REQUEST, "BAD_REQUEST", "Invalid request.");
                return;
            }
        } catch (NumberFormatException e) {
            writeQuoteError(resp, HttpServletResponse.SC_BAD_REQUEST, "BAD_REQUEST", "Invalid request.");
            return;
        }

        String dateParam = req.getParameter("date");
        LocalDate date = RoomService.parseDateOrToday(dateParam);

        List<String> slotCodes = BookingService.parseSlotCodes(req);

        try {
            Quote quote = catalogService.quoteFor(roomId, date, slotCodes, LocalDateTime.now());

            Map<String, Object> respMap = new LinkedHashMap<>();
            respMap.put("success", true);
            respMap.put("roomId", roomId);
            respMap.put("date", date.toString());

            List<Map<String, Object>> slotsJson = new ArrayList<>();
            for (RoomSlot rs : quote.getSlots()) {
                Map<String, Object> sm = new LinkedHashMap<>();
                sm.put("code", rs.getSlotCode());
                sm.put("name", rs.getSlotName());
                sm.put("time", rs.getTimeRange());
                sm.put("price", rs.getListPrice());
                slotsJson.add(sm);
            }
            respMap.put("slots", slotsJson);
            respMap.put("subtotal", quote.getSubtotal());
            respMap.put("discountPercent", quote.getDiscountPercent());
            respMap.put("discountAmount", quote.getDiscountAmount());
            respMap.put("total", quote.getTotal());
            respMap.put("subtotalText", ElFunctions.money(quote.getSubtotal()));
            respMap.put("discountText", ElFunctions.money(quote.getDiscountAmount()));
            respMap.put("totalText", ElFunctions.money(quote.getTotal()));

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(respMap));

        } catch (ServiceException e) {
            writeQuoteError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getCode(), e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during quotation calculation", e);
            writeQuoteError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "UNAVAILABLE",
                    "Prices could not be calculated. Please try again later.");
        }
    }

    private void writeQuoteError(HttpServletResponse resp, int status, String code, String message) throws IOException {
        resp.setStatus(status);
        Map<String, Object> errMap = new LinkedHashMap<>();
        errMap.put("success", false);
        errMap.put("code", code);
        errMap.put("message", message);
        resp.getWriter().write(gson.toJson(errMap));
    }
}

