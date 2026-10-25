package servlet;

import service.CredentialService;
import util.DBUtil;
import util.Flash;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Controller decrypting and displaying the smart lock access PIN for an authenticated guest.
 * Forwards to detail view rather than redirecting to prevent PIN in URLs (M5, G3).
 */
@WebServlet(name = "PinViewServlet", urlPatterns = {"/guest/bookings/pin"})
public class PinViewServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(PinViewServlet.class.getName());
    private final CredentialService credentialService = new CredentialService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/guest/bookings");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        Object uidObj = (session != null) ? session.getAttribute("userId") : null;
        if (!(uidObj instanceof Long)) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        long guestId = (Long) uidObj;

        String credIdStr = req.getParameter("credentialId");
        long credentialId = -1;
        if (credIdStr != null && !credIdStr.trim().isEmpty()) {
            try {
                credentialId = Long.parseLong(credIdStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        if (credentialId <= 0) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Credential not found.");
            return;
        }

        String sql = "SELECT ac.credential_id, ac.booking_id, ac.status, ac.valid_from, ac.valid_until, b.guest_id "
                + "FROM ACCESS_CREDENTIAL ac "
                + "JOIN BOOKING b ON ac.booking_id = b.booking_id "
                + "WHERE ac.credential_id = ?";

        long bookingId = -1;
        String status = null;
        LocalDateTime validFrom = null;
        LocalDateTime validUntil = null;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, credentialId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getLong("guest_id") != guestId) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Credential not found.");
                    return;
                }
                bookingId = rs.getLong("booking_id");
                status = rs.getString("status");
                Timestamp vf = rs.getTimestamp("valid_from");
                if (vf != null) validFrom = vf.toLocalDateTime();
                Timestamp vu = rs.getTimestamp("valid_until");
                if (vu != null) validUntil = vu.toLocalDateTime();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error verifying credential " + credentialId, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Credential verification failed.");
            return;
        }

        if (!"PENDING".equalsIgnoreCase(status) && !"ACTIVE".equalsIgnoreCase(status)) {
            Flash.error(req, "The PIN for this booking is no longer available.");
            resp.sendRedirect(req.getContextPath() + "/guest/bookings/detail?id=" + bookingId);
            return;
        }

        try {
            String pin = credentialService.decryptPinForGuest(credentialId, guestId);
            if (pin == null) {
                Flash.error(req, "The PIN for this booking is no longer available.");
                resp.sendRedirect(req.getContextPath() + "/guest/bookings/detail?id=" + bookingId);
                return;
            }

            LOGGER.info("Guest viewed PIN credential " + credentialId + " for booking " + bookingId);

            boolean loaded = new GuestBookingServlet().populateDetailAttributes(req, bookingId, guestId);
            if (!loaded) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Booking not found.");
                return;
            }

            req.setAttribute("shownPin", pin);
            req.setAttribute("pinValidFrom", validFrom);
            req.setAttribute("pinValidUntil", validUntil);

            req.getRequestDispatcher("/WEB-INF/views/guest/booking-detail.jsp").forward(req, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error decrypting PIN for credential " + credentialId, e);
            Flash.error(req, "Could not retrieve access PIN. Please try again later.");
            resp.sendRedirect(req.getContextPath() + "/guest/bookings/detail?id=" + bookingId);
        }
    }
}
