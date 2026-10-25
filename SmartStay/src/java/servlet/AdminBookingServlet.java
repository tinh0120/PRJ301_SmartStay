package servlet;

import dao.BookingDao;
import dao.RoomDao;
import dao.UserDao;
import model.User.AuthUser;
import model.Booking;
import model.BookingDetail;
import model.Room;
import model.RoomSlot;
import model.User;
import service.BookingService;
import service.ServiceException;
import util.Flash;
import util.PageResult.PageRequest;
import util.PageResult;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
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
 * Administrative controller consolidating booking listing, walk-in/editing forms, detail views,
 * cancellation, check-in, check-out, no-show intervention, and credential revocation.
 */
@WebServlet(name = "AdminBookingServlet", urlPatterns = {
    "/admin/bookings",
    "/admin/bookings/edit",
    "/admin/bookings/detail",
    "/admin/bookings/cancel",
    "/admin/bookings/checkin",
    "/admin/bookings/checkout",
    "/admin/bookings/early-checkin",
    "/admin/bookings/noshow",
    "/admin/bookings/refund",
    "/admin/credentials/revoke"
})
public class AdminBookingServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminBookingServlet.class.getName());

    private final BookingService bookingService = new BookingService();
    private final BookingDao bookingDao = new BookingDao();
    private final RoomDao roomDao = new RoomDao();
    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/bookings".equals(path)) {
            listBookings(req, resp);
            return;
        }
        if ("/admin/bookings/edit".equals(path)) {
            showEditForm(req, resp);
            return;
        }
        if ("/admin/bookings/detail".equals(path)) {
            showDetail(req, resp);
            return;
        }
        if ("/admin/bookings/cancel".equals(path)
                || "/admin/bookings/checkin".equals(path)
                || "/admin/bookings/checkout".equals(path)
                || "/admin/bookings/noshow".equals(path)
                || "/admin/bookings/refund".equals(path)
                || "/admin/credentials/revoke".equals(path)) {
            resp.sendRedirect(req.getContextPath() + "/admin/bookings");
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/bookings/edit".equals(path)) {
            saveBooking(req, resp);
            return;
        }
        if ("/admin/bookings/cancel".equals(path)) {
            cancelBooking(req, resp);
            return;
        }
        if ("/admin/bookings/checkin".equals(path) || "/admin/bookings/checkout".equals(path)) {
            handleCheckInOut(req, resp);
            return;
        }
        if ("/admin/bookings/early-checkin".equals(path)) {
            handleEarlyCheckinToggle(req, resp);
            return;
        }
        if ("/admin/bookings/noshow".equals(path)) {
            handleNoshow(req, resp);
            return;
        }
        if ("/admin/bookings/refund".equals(path)) {
            handleRefund(req, resp);
            return;
        }
        if ("/admin/credentials/revoke".equals(path)) {
            handleCredentialRevoke(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void listBookings(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        PageRequest pr = PageRequest.from(req, BookingDao.ADMIN_SORT_WHITELIST, "created", "DESC");

        String q = req.getParameter("q");
        if (q != null && !q.trim().isEmpty() && pr.getKeyword().isEmpty()) {
            pr.setKeyword(q.trim());
        }

        String roomId = req.getParameter("room_id");
        if (roomId != null && !roomId.trim().isEmpty() && pr.getFilter("room").isEmpty()) {
            pr.setFilter("room", roomId.trim());
        }

        String slotDateFrom = req.getParameter("slot_date_from");
        if (slotDateFrom != null && !slotDateFrom.trim().isEmpty() && pr.getFilter("from").isEmpty()) {
            pr.setFilter("from", slotDateFrom.trim());
        }

        String slotDateTo = req.getParameter("slot_date_to");
        if (slotDateTo != null && !slotDateTo.trim().isEmpty() && pr.getFilter("to").isEmpty()) {
            pr.setFilter("to", slotDateTo.trim());
        }

        List<Room> rooms = Collections.emptyList();
        try {
            rooms = roomDao.findAllActive();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load active rooms for booking list filter", e);
        }

        try {
            PageResult<Booking> pageResult = bookingDao.findPageAdmin(pr);
            req.setAttribute("pageResult", pageResult);
            req.setAttribute("rooms", rooms);
            req.setAttribute("pr", pr);
            try {
                req.setAttribute("statusCounts", bookingDao.getStatusCounts());
            } catch (SQLException ignored) {
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load admin booking list", e);
            req.setAttribute("dbError", "Failed to load bookings from database.");
            req.setAttribute("rooms", rooms);
            req.setAttribute("pr", pr);
        }

        req.getRequestDispatcher("/WEB-INF/views/admin/bookings.jsp").forward(req, resp);
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            Flash.error(req, "Booking ID is required.");
            resp.sendRedirect(req.getContextPath() + "/admin/bookings");
            return;
        }

        long bookingId;
        try {
            bookingId = Long.parseLong(idStr.trim());
        } catch (NumberFormatException e) {
            Flash.error(req, "Invalid booking ID format.");
            resp.sendRedirect(req.getContextPath() + "/admin/bookings");
            return;
        }

        try {
            BookingDetail detail = bookingDao.findDetailAdmin(bookingId);
            if (detail == null) {
                Flash.error(req, "Booking not found.");
                resp.sendRedirect(req.getContextPath() + "/admin/bookings");
                return;
            }

            req.setAttribute("detail", detail);
            req.getRequestDispatcher("/WEB-INF/views/admin/booking-detail.jsp").forward(req, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load booking detail for ID " + bookingId, e);
            Flash.error(req, "Failed to load booking detail from database.");
            resp.sendRedirect(req.getContextPath() + "/admin/bookings");
        }
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String idStr = req.getParameter("id");
        if (idStr == null) {
            idStr = req.getParameter("bookingId");
        }

        List<Room> rooms;
        try {
            rooms = roomDao.findAllActive();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load active rooms", e);
            rooms = Collections.emptyList();
        }
        req.setAttribute("rooms", rooms);

        if (idStr != null && !idStr.trim().isEmpty()) {
            // Edit mode
            long bookingId;
            try {
                bookingId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException e) {
                Flash.error(req, "Invalid booking ID.");
                resp.sendRedirect(req.getContextPath() + "/admin/bookings");
                return;
            }

            try {
                BookingDetail detail = bookingDao.findDetailAdmin(bookingId);
                if (detail == null) {
                    Flash.error(req, "Booking not found.");
                    resp.sendRedirect(req.getContextPath() + "/admin/bookings");
                    return;
                }

                req.setAttribute("detail", detail);
                req.setAttribute("booking", detail.getBooking());
                req.setAttribute("mode", "edit");

                // Determine active slots for the booking's room
                long targetRoomId = detail.getBooking().getRoomId();
                String changeRoomStr = req.getParameter("roomId");
                if (changeRoomStr != null && !changeRoomStr.trim().isEmpty()) {
                    try {
                        targetRoomId = Long.parseLong(changeRoomStr.trim());
                    } catch (NumberFormatException ignored) {
                    }
                }

                Map<Long, List<RoomSlot>> slotsMap = roomDao.findByRoomIds(Collections.singletonList(targetRoomId));
                List<RoomSlot> slots = slotsMap.getOrDefault(targetRoomId, Collections.<RoomSlot>emptyList());
                req.setAttribute("slots", slots);
                req.setAttribute("selectedRoomId", targetRoomId);

                LocalDate targetDate = detail.getBooking().getSlotDate();
                String changeDateStr = req.getParameter("date");
                if (changeDateStr != null && !changeDateStr.trim().isEmpty()) {
                    try {
                        targetDate = LocalDate.parse(changeDateStr.trim());
                    } catch (DateTimeParseException ignored) {
                    }
                }
                req.setAttribute("selectedDate", targetDate);

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load booking for editing", e);
                Flash.error(req, "Failed to load booking from database.");
                resp.sendRedirect(req.getContextPath() + "/admin/bookings");
                return;
            }
        } else {
            // Create walk-in mode
            req.setAttribute("mode", "create");

            long targetRoomId = -1;
            String roomIdStr = req.getParameter("roomId");
            if (roomIdStr != null && !roomIdStr.trim().isEmpty()) {
                try {
                    targetRoomId = Long.parseLong(roomIdStr.trim());
                } catch (NumberFormatException ignored) {
                }
            } else if (!rooms.isEmpty()) {
                targetRoomId = rooms.get(0).getRoomId();
            }

            LocalDate targetDate = LocalDate.now();
            String dateStr = req.getParameter("date");
            if (dateStr != null && !dateStr.trim().isEmpty()) {
                try {
                    targetDate = LocalDate.parse(dateStr.trim());
                } catch (DateTimeParseException ignored) {
                }
            }

            req.setAttribute("selectedRoomId", targetRoomId);
            req.setAttribute("selectedDate", targetDate);

            if (targetRoomId > 0) {
                try {
                    Map<Long, List<RoomSlot>> slotsMap = roomDao.findByRoomIds(Collections.singletonList(targetRoomId));
                    req.setAttribute("slots", slotsMap.getOrDefault(targetRoomId, Collections.<RoomSlot>emptyList()));
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to load room slots for walk-in", e);
                    req.setAttribute("slots", Collections.<RoomSlot>emptyList());
                }
            }

            String guestEmail = req.getParameter("guestEmail");
            if (guestEmail != null) {
                req.setAttribute("guestEmail", guestEmail);
            }
        }

        req.getRequestDispatcher("/WEB-INF/views/admin/booking-form.jsp").forward(req, resp);
    }

    private void saveBooking(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        long adminUserId = (authUser != null) ? authUser.getUserId() : 1L;

        String bookingIdStr = req.getParameter("bookingId");
        if (bookingIdStr == null || bookingIdStr.trim().isEmpty()) {
            bookingIdStr = req.getParameter("id");
        }

        if (bookingIdStr != null && !bookingIdStr.trim().isEmpty()) {
            // Edit existing booking
            long bookingId;
            try {
                bookingId = Long.parseLong(bookingIdStr.trim());
            } catch (NumberFormatException e) {
                Flash.error(req, "Invalid booking ID.");
                resp.sendRedirect(req.getContextPath() + "/admin/bookings");
                return;
            }

            String note = req.getParameter("note");
            String roomIdStr = req.getParameter("roomId");
            Long newRoomId = null;
            if (roomIdStr != null && !roomIdStr.trim().isEmpty()) {
                try {
                    newRoomId = Long.parseLong(roomIdStr.trim());
                } catch (NumberFormatException ignored) {
                }
            }

            String dateStr = req.getParameter("slotDate");
            if (dateStr == null || dateStr.trim().isEmpty()) {
                dateStr = req.getParameter("date");
            }
            LocalDate newDate = null;
            if (dateStr != null && !dateStr.trim().isEmpty()) {
                try {
                    newDate = LocalDate.parse(dateStr.trim());
                } catch (DateTimeParseException ignored) {
                }
            }

            List<String> slotCodes = BookingService.parseSlotCodes(req);

            try {
                bookingService.updateAdmin(adminUserId, bookingId, newRoomId, newDate, slotCodes, note, req.getRemoteAddr());
                Flash.success(req, "Booking updated successfully.");
                resp.sendRedirect(req.getContextPath() + "/admin/bookings/detail?id=" + bookingId);
            } catch (ServiceException pe) {
                Flash.error(req, pe.getMessage());
                resp.sendRedirect(req.getContextPath() + "/admin/bookings/edit?id=" + bookingId);
            } catch (SQLException se) {
                LOGGER.log(Level.SEVERE, "Database error updating booking ID " + bookingId, se);
                Flash.error(req, "Booking could not be updated. Please try again later.");
                resp.sendRedirect(req.getContextPath() + "/admin/bookings/edit?id=" + bookingId);
            }

        } else {
            // Create walk-in booking
            String guestEmail = req.getParameter("guestEmail");
            if (guestEmail == null || guestEmail.trim().isEmpty()) {
                Flash.error(req, "Guest email is required for walk-in booking.");
                resp.sendRedirect(req.getContextPath() + "/admin/bookings/edit");
                return;
            }

            User guest;
            try {
                guest = userDao.findByEmail(guestEmail.trim());
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error querying guest by email", e);
                Flash.error(req, "Error verifying guest email.");
                resp.sendRedirect(req.getContextPath() + "/admin/bookings/edit");
                return;
            }

            if (guest == null) {
                Flash.error(req, "Guest account with email '" + guestEmail.trim() + "' does not exist.");
                resp.sendRedirect(req.getContextPath() + "/admin/bookings/edit");
                return;
            }

            if (!"GUEST".equalsIgnoreCase(guest.getRoleName())) {
                Flash.error(req, "User account is not a guest.");
                resp.sendRedirect(req.getContextPath() + "/admin/bookings/edit");
                return;
            }

            if (!"ACTIVE".equalsIgnoreCase(guest.getStatus())) {
                Flash.error(req, "Guest account is " + guest.getStatus() + ", only active guests can book.");
                resp.sendRedirect(req.getContextPath() + "/admin/bookings/edit");
                return;
            }

            String roomIdStr = req.getParameter("roomId");
            long roomId = -1;
            if (roomIdStr != null && !roomIdStr.trim().isEmpty()) {
                try {
                    roomId = Long.parseLong(roomIdStr.trim());
                } catch (NumberFormatException ignored) {
                }
            }

            String dateStr = req.getParameter("slotDate");
            if (dateStr == null || dateStr.trim().isEmpty()) {
                dateStr = req.getParameter("date");
            }
            LocalDate slotDate = null;
            if (dateStr != null && !dateStr.trim().isEmpty()) {
                try {
                    slotDate = LocalDate.parse(dateStr.trim());
                } catch (DateTimeParseException ignored) {
                }
            }

            if (roomId <= 0 || slotDate == null) {
                Flash.error(req, "Valid room and slot date must be selected.");
                resp.sendRedirect(req.getContextPath() + "/admin/bookings/edit");
                return;
            }

            List<String> slotCodes = BookingService.parseSlotCodes(req);
            String note = req.getParameter("note");

            try {
                Booking created = bookingService.createWalkIn(adminUserId, guest.getUserId(), roomId, slotDate,
                        slotCodes, note, req.getRemoteAddr());
                Flash.success(req, "Walk-in booking " + created.getBookingCode() + " created successfully.");
                resp.sendRedirect(req.getContextPath() + "/admin/bookings/detail?id=" + created.getBookingId());
            } catch (ServiceException pe) {
                Flash.error(req, pe.getMessage());
                String redirectUrl = req.getContextPath() + "/admin/bookings/edit?roomId=" + roomId
                        + "&date=" + slotDate
                        + "&guestEmail=" + URLEncoder.encode(guestEmail.trim(), StandardCharsets.UTF_8.name());
                resp.sendRedirect(redirectUrl);
            } catch (SQLException se) {
                LOGGER.log(Level.SEVERE, "Database error creating walk-in booking", se);
                Flash.error(req, "Failed to create walk-in booking in database.");
                resp.sendRedirect(req.getContextPath() + "/admin/bookings/edit");
            }
        }
    }

    private void cancelBooking(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        long adminUserId = (authUser != null) ? authUser.getUserId() : 1L;

        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            idStr = req.getParameter("bookingId");
        }

        long bookingId = -1;
        if (idStr != null && !idStr.trim().isEmpty()) {
            try {
                bookingId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        if (bookingId <= 0) {
            Flash.error(req, "Invalid booking ID.");
            resp.sendRedirect(req.getContextPath() + "/admin/bookings");
            return;
        }

        String reason = req.getParameter("reason");
        if (reason == null || reason.trim().isEmpty()) {
            Flash.error(req, "Cancellation reason is required.");
            resp.sendRedirect(req.getContextPath() + "/admin/bookings/detail?id=" + bookingId);
            return;
        }

        try {
            bookingService.cancelByAdmin(adminUserId, bookingId, reason, req.getRemoteAddr());
            Flash.success(req, "Booking cancelled successfully.");
            resp.sendRedirect(req.getContextPath() + "/admin/bookings/detail?id=" + bookingId);
        } catch (ServiceException pe) {
            Flash.error(req, pe.getMessage());
            resp.sendRedirect(req.getContextPath() + "/admin/bookings/detail?id=" + bookingId);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "Database error cancelling booking ID " + bookingId, se);
            Flash.error(req, "Booking could not be cancelled. Please try again later.");
            resp.sendRedirect(req.getContextPath() + "/admin/bookings/detail?id=" + bookingId);
        }
    }

    private void handleCheckInOut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        long adminUserId = (authUser != null) ? authUser.getUserId() : 1L;

        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            idStr = req.getParameter("bookingId");
        }

        long bookingId = -1;
        if (idStr != null && !idStr.trim().isEmpty()) {
            try {
                bookingId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        if (bookingId <= 0) {
            Flash.error(req, "Invalid booking ID.");
            resp.sendRedirect(req.getContextPath() + "/admin/bookings");
            return;
        }

        String servletPath = req.getServletPath();
        boolean isCheckIn = servletPath.endsWith("/checkin");

        try {
            if (isCheckIn) {
                bookingService.checkInManual(adminUserId, bookingId, req.getRemoteAddr());
                Flash.success(req, "Guest successfully checked in. Room marked OCCUPIED.");
            } else {
                bookingService.checkOutManual(adminUserId, bookingId, req.getRemoteAddr());
                Flash.success(req, "Guest successfully checked out. Room marked NEEDS_CLEANING and active credentials expired.");
            }
            resp.sendRedirect(req.getContextPath() + "/admin/bookings/detail?id=" + bookingId);
        } catch (IllegalStateException ise) {
            Flash.error(req, ise.getMessage());
            resp.sendRedirect(req.getContextPath() + "/admin/bookings/detail?id=" + bookingId);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "Database error during check-in/out for booking ID " + bookingId, se);
            Flash.error(req, "Failed to perform check-" + (isCheckIn ? "in" : "out") + " due to a database error.");
            resp.sendRedirect(req.getContextPath() + "/admin/bookings/detail?id=" + bookingId);
        }
    }

    private void handleEarlyCheckinToggle(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        long adminUserId = (authUser != null) ? authUser.getUserId() : 1L;

        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            idStr = req.getParameter("bookingId");
        }
        long bookingId = -1;
        if (idStr != null) {
            try {
                bookingId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException ignored) {}
        }
        if (bookingId <= 0) {
            Flash.error(req, "Invalid booking ID.");
            resp.sendRedirect(req.getContextPath() + "/admin/bookings");
            return;
        }

        String allowParam = req.getParameter("allow");
        boolean allow = "true".equalsIgnoreCase(allowParam) || "1".equals(allowParam) || "on".equalsIgnoreCase(allowParam);

        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(req.getHeader("X-Requested-With"))
                || (req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json"));

        try {
            bookingService.toggleAllowEarlyCheckin(adminUserId, bookingId, allow, req.getRemoteAddr());
            if (isAjax) {
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write("{\"success\":true,\"allowEarlyCheckin\":" + allow + ",\"message\":\""
                        + (allow ? "Early check-in enabled successfully." : "Early check-in disabled successfully.") + "\"}");
                return;
            }
            Flash.success(req, allow ? "Early check-in enabled for booking #" + bookingId : "Early check-in disabled for booking #" + bookingId);
        } catch (IllegalStateException ise) {
            if (isAjax) {
                resp.setContentType("application/json;charset=UTF-8");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"" + ise.getMessage() + "\"}");
                return;
            }
            Flash.error(req, ise.getMessage());
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "Database error toggling early check-in for booking ID " + bookingId, se);
            if (isAjax) {
                resp.setContentType("application/json;charset=UTF-8");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false,\"message\":\"Database error occurred.\"}");
                return;
            }
            Flash.error(req, "Failed to toggle early check-in due to database error.");
        }

        String referer = req.getHeader("Referer");
        if (referer != null && !referer.trim().isEmpty()) {
            resp.sendRedirect(referer);
        } else {
            resp.sendRedirect(req.getContextPath() + "/admin/bookings/detail?id=" + bookingId);
        }
    }

    private void handleNoshow(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        long adminUserId = (authUser != null) ? authUser.getUserId() : 1L;

        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            idStr = req.getParameter("bookingId");
        }

        long bookingId = -1;
        if (idStr != null && !idStr.trim().isEmpty()) {
            try {
                bookingId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        if (bookingId <= 0) {
            Flash.error(req, "Invalid booking ID.");
            resp.sendRedirect(req.getContextPath() + "/admin/bookings");
            return;
        }

        String action = req.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            Flash.error(req, "Action parameter is required (release or reinstate).");
            resp.sendRedirect(req.getContextPath() + "/admin/bookings/detail?id=" + bookingId);
            return;
        }

        try {
            if ("release".equalsIgnoreCase(action.trim())) {
                boolean released = bookingService.releaseNoshow(adminUserId, bookingId, req.getRemoteAddr());
                if (released) {
                    Flash.success(req, "No-show held slots have been successfully released.");
                } else {
                    Flash.info(req, "No held slots found to release. Slots were already released.");
                }
            } else if ("reinstate".equalsIgnoreCase(action.trim())) {
                String refundParam = req.getParameter("refundPayment");
                boolean refund = "true".equalsIgnoreCase(refundParam) || "1".equals(refundParam) || "on".equalsIgnoreCase(refundParam);
                String paymentNote = req.getParameter("paymentNote");

                bookingService.reinstateNoshow(adminUserId, bookingId, refund, paymentNote, req.getRemoteAddr());
                Flash.success(req, "No-show booking successfully reinstated. New PIN credential generated.");
            } else {
                Flash.error(req, "Unsupported action: " + action);
            }
            resp.sendRedirect(req.getContextPath() + "/admin/bookings/detail?id=" + bookingId);
        } catch (IllegalStateException ise) {
            Flash.error(req, ise.getMessage());
            resp.sendRedirect(req.getContextPath() + "/admin/bookings/detail?id=" + bookingId);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "Database error handling no-show action for booking ID " + bookingId, se);
            Flash.error(req, "Failed to perform no-show intervention due to a database error.");
            resp.sendRedirect(req.getContextPath() + "/admin/bookings/detail?id=" + bookingId);
        }
    }

    private void handleCredentialRevoke(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        long adminUserId = (authUser != null) ? authUser.getUserId() : 1L;

        String credIdStr = req.getParameter("credentialId");
        long credentialId = -1;
        if (credIdStr != null && !credIdStr.trim().isEmpty()) {
            try {
                credentialId = Long.parseLong(credIdStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String bookingIdStr = req.getParameter("bookingId");
        String redirectTarget = (bookingIdStr != null && !bookingIdStr.trim().isEmpty())
                ? (req.getContextPath() + "/admin/bookings/detail?id=" + bookingIdStr.trim())
                : (req.getContextPath() + "/admin/bookings");

        if (credentialId <= 0) {
            Flash.error(req, "Invalid credential ID.");
            resp.sendRedirect(redirectTarget);
            return;
        }

        String reason = req.getParameter("reason");

        try {
            bookingService.revokeCredential(adminUserId, credentialId, reason, req.getRemoteAddr());
            Flash.success(req, "Credential #" + credentialId + " successfully revoked.");
            resp.sendRedirect(redirectTarget);
        } catch (IllegalStateException ise) {
            Flash.error(req, ise.getMessage());
            resp.sendRedirect(redirectTarget);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "Database error revoking credential " + credentialId, se);
            Flash.error(req, "Failed to revoke credential due to a database error.");
            resp.sendRedirect(redirectTarget);
        }
    }

    private void handleRefund(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        long adminUserId = (authUser != null) ? authUser.getUserId() : 1L;

        String bookingIdStr = req.getParameter("bookingId");
        long bookingId = -1;
        if (bookingIdStr != null && !bookingIdStr.trim().isEmpty()) {
            try {
                bookingId = Long.parseLong(bookingIdStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String redirectTarget = req.getParameter("returnUrl");
        if (redirectTarget == null || redirectTarget.trim().isEmpty() || !redirectTarget.startsWith("/")) {
            redirectTarget = req.getContextPath() + "/admin/bookings";
        }

        if (bookingId <= 0) {
            Flash.error(req, "Invalid booking ID.");
            resp.sendRedirect(redirectTarget);
            return;
        }

        String note = req.getParameter("note");
        if (note == null || note.trim().isEmpty()) {
            note = "Marked refunded by admin";
        }

        try {
            boolean success = bookingDao.markPaymentRefunded(bookingId, adminUserId, note);
            if (success) {
                Flash.success(req, "Payment for booking #" + bookingId + " marked as REFUNDED.");
            } else {
                Flash.error(req, "No pending refund found for booking #" + bookingId + ".");
            }
            resp.sendRedirect(redirectTarget);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "Database error marking payment refunded for booking " + bookingId, se);
            Flash.error(req, "Failed to mark payment as refunded due to database error.");
            resp.sendRedirect(redirectTarget);
        }
    }
}
