package servlet;

import com.google.gson.Gson;
import dao.BookingDao;
import model.BookingDetail;
import service.CredentialService;
import service.ServiceException;
import service.PaymentService;
import util.DBUtil;
import util.ElFunctions;
import util.Flash;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
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
 * Controller consolidating guest payment flows: initiating payment link, viewing result page, and polling status.
 */
@WebServlet(name = "GuestPaymentServlet", urlPatterns = {
    "/guest/bookings/pay",
    "/guest/payment/result",
    "/guest/payment/status"
})
public class GuestPaymentServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(GuestPaymentServlet.class.getName());

    private final PaymentService paymentService = new PaymentService();
    private final CredentialService credentialService = new CredentialService();
    private final BookingDao bookingDao = new BookingDao();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/guest/bookings/pay".equals(path)) {
            resp.sendRedirect(req.getContextPath() + "/guest/bookings");
            return;
        }
        if ("/guest/payment/result".equals(path)) {
            showResult(req, resp);
            return;
        }
        if ("/guest/payment/status".equals(path)) {
            pollStatus(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/guest/bookings/pay".equals(path)) {
            createPayment(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void createPayment(HttpServletRequest req, HttpServletResponse resp)
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
            resp.sendRedirect(req.getContextPath() + "/guest/bookings");
            return;
        }

        try {
            String checkoutUrl = paymentService.createPayment(bookingId, guestId, req.getRemoteAddr());
            if (checkoutUrl.startsWith("http://") || checkoutUrl.startsWith("https://")) {
                resp.sendRedirect(checkoutUrl);
            } else {
                resp.sendRedirect(req.getContextPath() + (checkoutUrl.startsWith("/") ? checkoutUrl : "/" + checkoutUrl));
            }
        } catch (ServiceException pe) {
            Flash.error(req, pe.getMessage());
            resp.sendRedirect(req.getContextPath() + "/guest/bookings/detail?id=" + bookingId);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "Database error generating payment link for booking " + bookingId, se);
            Flash.error(req, "Payment service temporarily unavailable. Please try again.");
            resp.sendRedirect(req.getContextPath() + "/guest/bookings/detail?id=" + bookingId);
        }
    }

    private void showResult(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object uidObj = (session != null) ? session.getAttribute("userId") : null;
        String roleName = (session != null) ? (String) session.getAttribute("roleName") : null;

        if (!(uidObj instanceof Long)) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        long currentUid = (Long) uidObj;

        String idStr = req.getParameter("bookingId");
        if (idStr == null || idStr.trim().isEmpty()) {
            idStr = req.getParameter("id");
        }

        long bookingId = -1;
        if (idStr != null && !idStr.trim().isEmpty()) {
            try {
                bookingId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        if (bookingId <= 0) {
            String ocStr = req.getParameter("orderCode");
            if (ocStr != null && !ocStr.trim().isEmpty()) {
                try {
                    long oc = Long.parseLong(ocStr.trim());
                    try (Connection conn = DBUtil.getConnection();
                         PreparedStatement ps = conn.prepareStatement("SELECT booking_id FROM PAYMENT WHERE provider_order_code = ?")) {
                        ps.setLong(1, oc);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                bookingId = rs.getLong(1);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        if (bookingId <= 0) {
            String bCode = req.getParameter("booking");
            if (bCode != null && !bCode.trim().isEmpty()) {
                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT booking_id FROM BOOKING WHERE booking_code = ?")) {
                    ps.setString(1, bCode.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            bookingId = rs.getLong(1);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        if (bookingId <= 0) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Booking ID is required.");
            return;
        }

        try {
            BookingDetail bookingDetail = bookingDao.findDetail(bookingId);
            if (bookingDetail == null || bookingDetail.getBooking() == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Booking not found.");
                return;
            }

            long ownerGuestId = bookingDetail.getBooking().getGuestId();
            boolean isPrivileged = "ADMIN".equalsIgnoreCase(roleName) || "OWNER".equalsIgnoreCase(roleName);
            if (ownerGuestId != currentUid && !isPrivileged) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: booking does not belong to you.");
                return;
            }

            String paymentStatus = "NONE";
            long orderCode = -1;
            long paymentAmount = bookingDetail.getBooking().getTotal();

            String paySql = "SELECT TOP 1 status, amount, provider_order_code FROM PAYMENT WHERE booking_id = ? ORDER BY payment_id DESC";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(paySql)) {
                ps.setLong(1, bookingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        paymentStatus = rs.getString("status");
                        orderCode = rs.getLong("provider_order_code");
                        if (rs.getBigDecimal("amount") != null) {
                            paymentAmount = rs.getBigDecimal("amount").longValue();
                        }
                    }
                }
            }

            boolean isSuccess = "PAID".equalsIgnoreCase(paymentStatus)
                    || "CONFIRMED".equalsIgnoreCase(bookingDetail.getBooking().getStatus())
                    || "CHECKED_IN".equalsIgnoreCase(bookingDetail.getBooking().getStatus())
                    || "CHECKED_OUT".equalsIgnoreCase(bookingDetail.getBooking().getStatus());

            if (isSuccess) {
                // Decrypt guest PIN safely for the booking owner (Task 7 Option A)
                CredentialService.GuestPinDetails pinDetails = credentialService.getGuestPinForBooking(bookingId, ownerGuestId);
                req.setAttribute("pinDetails", pinDetails);
            }

            String bookingCode = bookingDetail.getBooking().getBookingCode();
            String roomNumber = (bookingDetail.getRoom() != null) ? bookingDetail.getRoom().getRoomNumber() : "";
            String roomName = (bookingDetail.getRoom() != null) ? bookingDetail.getRoom().getDisplayName() : "";
            int floor = (bookingDetail.getRoom() != null) ? bookingDetail.getRoom().getFloorNumber() : 1;

            req.setAttribute("bookingDetail", bookingDetail);
            req.setAttribute("bookingId", bookingId);
            req.setAttribute("bookingCode", bookingCode);
            req.setAttribute("roomNumber", roomNumber);
            req.setAttribute("roomName", roomName);
            req.setAttribute("floor", floor);
            req.setAttribute("totalAmount", paymentAmount);
            req.setAttribute("bookingStatus", bookingDetail.getBooking().getStatus());
            req.setAttribute("paymentStatus", paymentStatus);
            req.setAttribute("orderCode", orderCode);

            req.getRequestDispatcher("/WEB-INF/views/guest/payment-result.jsp").forward(req, resp);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error loading payment result for booking " + bookingId, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Payment result could not be loaded.");
        }
    }

    private void pollStatus(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

        HttpSession session = req.getSession(false);
        Object uidObj = (session != null) ? session.getAttribute("userId") : null;
        String roleName = (session != null) ? (String) session.getAttribute("roleName") : null;

        if (uidObj == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "UNAUTHORIZED");
            err.put("error", "Authentication required. Please log in.");
            resp.getWriter().write(gson.toJson(err));
            return;
        }

        String bookingParam = req.getParameter("booking");
        String bookingCodeParam = req.getParameter("bookingCode");
        String idStr = req.getParameter("bookingId");
        if (idStr == null || idStr.trim().isEmpty()) {
            idStr = req.getParameter("id");
        }
        String orderCodeStr = req.getParameter("orderCode");

        String searchBookingCode = null;
        long searchBookingId = -1;
        long searchOrderCode = -1;

        if (bookingParam != null && !bookingParam.trim().isEmpty()) {
            String bTrim = bookingParam.trim();
            if (bTrim.toUpperCase().startsWith("BK")) {
                searchBookingCode = bTrim;
            } else {
                try {
                    searchBookingId = Long.parseLong(bTrim);
                } catch (NumberFormatException ignored) {
                    searchBookingCode = bTrim;
                }
            }
        }
        if (bookingCodeParam != null && !bookingCodeParam.trim().isEmpty()) {
            searchBookingCode = bookingCodeParam.trim();
        }
        if (idStr != null && !idStr.trim().isEmpty()) {
            try {
                searchBookingId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        if (orderCodeStr != null && !orderCodeStr.trim().isEmpty()) {
            try {
                searchOrderCode = Long.parseLong(orderCodeStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        if (searchBookingId <= 0 && (searchBookingCode == null || searchBookingCode.isEmpty()) && searchOrderCode <= 0) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "INVALID_REQUEST");
            err.put("error", "Booking identifier (booking, bookingCode, bookingId, or orderCode) is required.");
            resp.getWriter().write(gson.toJson(err));
            return;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT b.booking_id, b.booking_code, b.status AS booking_status, b.total_amount, b.guest_id, ")
           .append("COALESCE(p.status, 'NONE') AS payment_status, p.provider_order_code, p.expires_at ")
           .append("FROM BOOKING b ")
           .append("LEFT JOIN PAYMENT p ON p.booking_id = b.booking_id ");

        if (searchBookingId > 0) {
            sql.append("WHERE b.booking_id = ? ");
        } else if (searchBookingCode != null && !searchBookingCode.isEmpty()) {
            sql.append("WHERE UPPER(b.booking_code) = UPPER(?) ");
        } else {
            sql.append("WHERE p.provider_order_code = ? ");
        }
        sql.append("ORDER BY p.payment_id DESC");

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (searchBookingId > 0) {
                ps.setLong(1, searchBookingId);
            } else if (searchBookingCode != null && !searchBookingCode.isEmpty()) {
                ps.setString(1, searchBookingCode);
            } else {
                ps.setLong(1, searchOrderCode);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put("status", "NOT_FOUND");
                    err.put("error", "Booking not found");
                    resp.getWriter().write(gson.toJson(err));
                    return;
                }

                long currentUid = (Long) uidObj;
                long guestId = rs.getLong("guest_id");
                boolean isPrivileged = "ADMIN".equalsIgnoreCase(roleName) || "OWNER".equalsIgnoreCase(roleName);
                if (guestId != currentUid && !isPrivileged) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put("status", "FORBIDDEN");
                    err.put("error", "Access denied: booking does not belong to the logged-in guest.");
                    resp.getWriter().write(gson.toJson(err));
                    return;
                }

                long bookingId = rs.getLong("booking_id");
                String bookingCode = rs.getString("booking_code");
                String bookingStatus = rs.getString("booking_status");
                String paymentStatus = rs.getString("payment_status");
                long orderCode = rs.getLong("provider_order_code");
                Timestamp expiresAtTs = rs.getTimestamp("expires_at");
                boolean isExpired = (expiresAtTs != null && java.time.LocalDateTime.now().isAfter(expiresAtTs.toLocalDateTime()));

                String status;
                String redirectUrl = null;

                if ("PAID".equalsIgnoreCase(paymentStatus)
                        || "CONFIRMED".equalsIgnoreCase(bookingStatus)
                        || "CHECKED_IN".equalsIgnoreCase(bookingStatus)
                        || "CHECKED_OUT".equalsIgnoreCase(bookingStatus)) {
                    status = "PAID";
                    redirectUrl = req.getContextPath() + "/guest/payment/result?bookingId=" + bookingId;
                } else if ("REVIEW_REQUIRED".equalsIgnoreCase(paymentStatus)) {
                    status = "REVIEW_REQUIRED";
                    redirectUrl = req.getContextPath() + "/guest/payment/result?bookingId=" + bookingId;
                } else if ("FAILED".equalsIgnoreCase(paymentStatus)) {
                    status = "FAILED";
                    redirectUrl = req.getContextPath() + "/guest/payment/result?bookingId=" + bookingId;
                } else if ("CANCELLED".equalsIgnoreCase(bookingStatus) || "CANCEL".equalsIgnoreCase(paymentStatus)) {
                    status = "CANCELLED";
                    redirectUrl = req.getContextPath() + "/guest/bookings";
                } else if ("EXPIRED".equalsIgnoreCase(bookingStatus) || "EXPIRED".equalsIgnoreCase(paymentStatus) || isExpired) {
                    status = "EXPIRED";
                    redirectUrl = req.getContextPath() + "/guest/bookings";

                    // Server releases held slots and marks booking EXPIRED if payment expired
                    if (isExpired && "PENDING_PAYMENT".equalsIgnoreCase(bookingStatus)) {
                        try {
                            try (PreparedStatement psUpdB = conn.prepareStatement(
                                    "UPDATE BOOKING SET status = 'EXPIRED', expire_reason = 'PAYMENT_TIMEOUT', updated_at = SYSDATETIME() WHERE booking_id = ? AND status = 'PENDING_PAYMENT'")) {
                                psUpdB.setLong(1, bookingId);
                                psUpdB.executeUpdate();
                            }
                            try (PreparedStatement psUpdS = conn.prepareStatement(
                                    "UPDATE BOOKING_SLOT SET hold_status = 'RELEASED', released_at = SYSDATETIME() WHERE booking_id = ? AND hold_status = 'HELD'")) {
                                psUpdS.setLong(1, bookingId);
                                psUpdS.executeUpdate();
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.WARNING, "Failed to release held slots for expired booking " + bookingId, ex);
                        }
                    }
                } else {
                    status = "PENDING";
                    redirectUrl = null;
                }

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("status", status);
                data.put("redirectUrl", redirectUrl);
                data.put("bookingCode", bookingCode);
                data.put("bookingId", bookingId);
                data.put("orderCode", orderCode);
                data.put("paymentStatus", paymentStatus);
                data.put("bookingStatus", bookingStatus);
                data.put("totalText", ElFunctions.money(rs.getBigDecimal("total_amount").longValue()));

                resp.getWriter().write(gson.toJson(data));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error polling payment status for search id=" + searchBookingId + " code=" + searchBookingCode, e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "ERROR");
            err.put("error", "Internal database error");
            resp.getWriter().write(gson.toJson(err));
        }
    }
}
