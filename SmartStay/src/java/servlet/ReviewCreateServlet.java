package servlet;

import service.BookingService;
import service.ServiceException;
import util.Flash;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Controller receiving guest reviews for completed stays.
 */
@WebServlet(name = "ReviewCreateServlet", urlPatterns = {"/guest/reviews/create"})
public class ReviewCreateServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ReviewCreateServlet.class.getName());
    private final BookingService bookingService = new BookingService();

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

        String idStr = req.getParameter("bookingId");
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

        String ratingStr = req.getParameter("rating");
        int rating = 0;
        if (ratingStr != null && !ratingStr.trim().isEmpty()) {
            try {
                rating = Integer.parseInt(ratingStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String comment = req.getParameter("comment");

        try {
            bookingService.createReview(guestId, bookingId, rating, comment, req.getRemoteAddr());
            Flash.success(req, "Thank you for your review.");
            resp.sendRedirect(req.getContextPath() + "/guest/bookings/detail?id=" + bookingId);
        } catch (ServiceException re) {
            if ("COMMENT_TOO_LONG".equals(re.getCode())) {
                // Forward back to detail with input preserved so guest does not lose long text
                try {
                    boolean ok = new GuestBookingServlet().populateDetailAttributes(req, bookingId, guestId);
                    if (ok) {
                        req.setAttribute("commentError", re.getMessage());
                        req.setAttribute("enteredComment", comment);
                        req.setAttribute("enteredRating", rating);
                        req.getRequestDispatcher("/WEB-INF/views/guest/booking-detail.jsp").forward(req, resp);
                        return;
                    }
                } catch (SQLException ignored) {
                }
            }
            Flash.error(req, re.getMessage());
            resp.sendRedirect(req.getContextPath() + "/guest/bookings/detail?id=" + bookingId);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "Database error saving review for booking " + bookingId, se);
            Flash.error(req, "Review could not be saved. Please try again later.");
            resp.sendRedirect(req.getContextPath() + "/guest/bookings/detail?id=" + bookingId);
        }
    }
}
