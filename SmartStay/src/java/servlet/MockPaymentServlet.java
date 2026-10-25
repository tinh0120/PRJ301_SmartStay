package servlet;

import com.google.gson.JsonObject;
import dao.BookingDao;
import model.BookingDetail;
import service.PaymentService;
import util.AppConstants;
import util.SecurityUtil;
import util.DBUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controller simulating the PayOS hosted checkout flow and simulation callbacks.
 */
@WebServlet(name = "MockPaymentServlet", urlPatterns = {"/mock-payos/checkout", "/mock-payos/simulate"})
public class MockPaymentServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(MockPaymentServlet.class.getName());
    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/mock-payos/checkout".equals(path)) {
            handleCheckout(req, resp);
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/guest/bookings");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/mock-payos/simulate".equals(path)) {
            handleSimulate(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleCheckout(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String providerConfig = AppConstants.PAYMENT_PROVIDER;
        if (!"MOCK".equalsIgnoreCase(providerConfig)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Mock payment gateway is disabled.");
            return;
        }

        String orderCodeStr = req.getParameter("orderCode");
        long orderCode = -1;
        if (orderCodeStr != null && !orderCodeStr.trim().isEmpty()) {
            try {
                orderCode = Long.parseLong(orderCodeStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        if (orderCode <= 0) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid or missing order code.");
            return;
        }

        String sql = "SELECT p.payment_id, p.booking_id, p.provider, p.provider_order_code, p.amount, "
                + "p.status AS payment_status, p.expires_at, b.booking_code, b.room_id, r.room_number, b.total_amount "
                + "FROM PAYMENT p "
                + "JOIN BOOKING b ON p.booking_id = b.booking_id "
                + "JOIN ROOM r ON b.room_id = r.room_id "
                + "WHERE p.provider_order_code = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Payment order not found.");
                    return;
                }

                String provider = rs.getString("provider");
                if (!"MOCK".equalsIgnoreCase(provider)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Payment is not assigned to mock provider.");
                    return;
                }

                long bookingId = rs.getLong("booking_id");
                String bookingCode = rs.getString("booking_code");
                String roomNumber = rs.getString("room_number");
                long amount = rs.getBigDecimal("amount").longValue();
                String paymentStatus = rs.getString("payment_status");
                Timestamp expiresAtTs = rs.getTimestamp("expires_at");

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime expiresAt = (expiresAtTs != null) ? expiresAtTs.toLocalDateTime() : now;
                long remainingSeconds = ChronoUnit.SECONDS.between(now, expiresAt);
                if (remainingSeconds < 0) {
                    remainingSeconds = 0;
                }
                boolean isExpired = remainingSeconds <= 0;

                // Load booking detail with room, date, and slots breakdown
                BookingDao bookingDao = new BookingDao();
                BookingDetail bookingDetail = bookingDao.findDetail(bookingId);
                req.setAttribute("bookingDetail", bookingDetail);

                // Merchant & Simulator configuration
                String simParam = getServletContext().getInitParameter("SIMULATOR_ENABLED");
                boolean simulatorEnabled = (simParam != null) ? Boolean.parseBoolean(simParam) : AppConstants.SIMULATOR_ENABLED;
                req.setAttribute("simulatorEnabled", simulatorEnabled);
                req.setAttribute("merchantAccountName", AppConstants.MERCHANT_ACCOUNT_NAME);
                req.setAttribute("merchantBankName", AppConstants.MERCHANT_BANK_NAME);
                req.setAttribute("merchantAccountNumber", AppConstants.MERCHANT_ACCOUNT_NUMBER);

                req.setAttribute("orderCode", orderCode);
                req.setAttribute("bookingId", bookingId);
                req.setAttribute("bookingCode", bookingCode);
                req.setAttribute("roomNumber", roomNumber);
                req.setAttribute("amount", amount);
                req.setAttribute("paymentStatus", paymentStatus);
                req.setAttribute("remainingSeconds", remainingSeconds);
                req.setAttribute("isExpired", isExpired);
                req.setAttribute("expiresAt", expiresAtTs);
                req.setAttribute("_csrf", SecurityUtil.tokenForSession(req.getSession(true)));

                req.getRequestDispatcher("/WEB-INF/views/mockpay/checkout.jsp").forward(req, resp);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error loading mock checkout for order " + orderCode, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Checkout could not be loaded.");
        }
    }

    private void handleSimulate(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String orderCodeStr = req.getParameter("orderCode");
        long orderCode = -1;
        if (orderCodeStr != null && !orderCodeStr.trim().isEmpty()) {
            try {
                orderCode = Long.parseLong(orderCodeStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String amountStr = req.getParameter("amount");
        long amount = 0;
        if (amountStr != null && !amountStr.trim().isEmpty()) {
            try {
                amount = Long.parseLong(amountStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String result = req.getParameter("result");
        if (result == null || result.trim().isEmpty()) {
            result = "SUCCESS";
        } else {
            result = result.trim().toUpperCase();
        }

        long bookingId = -1;
        String querySql = "SELECT booking_id FROM PAYMENT WHERE provider_order_code = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(querySql)) {
            ps.setLong(1, orderCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    bookingId = rs.getLong("booking_id");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to resolve bookingId for orderCode " + orderCode, e);
        }

        try {
            paymentService.processResult(orderCode, amount, result, "TXN_MOCK_" + orderCode, req.getRemoteAddr());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error processing payment result for order " + orderCode, e);
        }

        // Return JSON response if requested via fetch / XMLHttpRequest
        boolean isJson = "application/json".equalsIgnoreCase(req.getContentType())
                || (req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(req.getHeader("X-Requested-With"))
                || "json".equalsIgnoreCase(req.getParameter("format"));

        if (isJson) {
            resp.setContentType("application/json; charset=UTF-8");
            resp.setHeader("Cache-Control", "no-store");
            JsonObject jsonResp = new JsonObject();
            jsonResp.addProperty("success", true);
            jsonResp.addProperty("result", result);
            jsonResp.addProperty("orderCode", orderCode);
            jsonResp.addProperty("bookingId", bookingId);
            resp.getWriter().write(jsonResp.toString());
            return;
        }

        if (bookingId > 0) {
            resp.sendRedirect(req.getContextPath() + "/guest/payment/result?bookingId=" + bookingId);
        } else {
            resp.sendRedirect(req.getContextPath() + "/guest/bookings");
        }
    }
}

