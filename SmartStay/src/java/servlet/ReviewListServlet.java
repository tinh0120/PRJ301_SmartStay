package servlet;

import dao.ReviewDao;
import dao.RoomDao;
import model.User.AuthUser;
import model.Review;
import model.Room;
import util.SecurityUtil;
import util.Flash;
import util.PageResult.PageRequest;
import util.PageResult;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Controller for reviewing and moderating guest reviews.
 * <p>
 * Requires permission REVIEW_MODERATE.
 */
@WebServlet(name = "ReviewListServlet", urlPatterns = {"/admin/reviews", "/admin/reviews/action"})
public class ReviewListServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ReviewListServlet.class.getName());
    private static final String[] SORT_KEYS = {"created", "rating", "room", "status"};

    private final ReviewDao reviewDao = new ReviewDao();
    private final RoomDao roomDao = new RoomDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        @SuppressWarnings("unchecked")
        Set<String> perms = (session != null) ? (Set<String>) session.getAttribute("permissions") : null;

        if (perms == null || !perms.contains("REVIEW_MODERATE")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to moderate reviews.");
            return;
        }

        try {
            PageRequest pr = PageRequest.from(req, SORT_KEYS, "created", "DESC");

            // R11 tabs: All | Visible | Hidden | Needs attention (rating 2 or lower)
            String tab = pr.getFilter("tab").toLowerCase();
            if (tab.isEmpty()) {
                String legacy = pr.getFilter("status").toUpperCase();
                tab = "VISIBLE".equals(legacy) ? "visible" : ("HIDDEN".equals(legacy) ? "hidden" : "all");
            }
            if (!"all".equals(tab) && !"visible".equals(tab) && !"hidden".equals(tab) && !"attention".equals(tab)) {
                tab = "all";
            }
            pr.setFilter("tab", "all".equals(tab) ? null : tab);
            pr.setFilter("status", "visible".equals(tab) ? "VISIBLE" : ("hidden".equals(tab) ? "HIDDEN" : null));
            pr.setFilter("attention", "attention".equals(tab) ? "1" : null);

            PageResult<Review> result = reviewDao.findPage(pr);

            Long roomFilter = null;
            try {
                if (!pr.getFilter("roomId").isEmpty()) {
                    roomFilter = Long.valueOf(pr.getFilter("roomId").trim());
                }
            } catch (NumberFormatException ignored) {
                pr.setFilter("roomId", null);
            }
            try {
                req.setAttribute("tabCounts", reviewDao.getTabCounts(roomFilter));
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Failed to count reviews per tab", ex);
            }
            req.setAttribute("tab", tab);
            req.setAttribute("selectedRoomId", roomFilter);
            req.setAttribute("tabBaseQs", pr.toQueryString("page", "size", "tab", "status", "attention"));
            req.setAttribute("roomBaseQs", pr.toQueryString("page", "size", "roomId", "status", "attention"));
            req.setAttribute("returnQs", pr.toQueryString("size", "status", "attention"));
            List<Room> rooms = roomDao.findAllActive();

            req.setAttribute("result", result);
            req.setAttribute("rooms", rooms != null ? rooms : Collections.<Room>emptyList());
            try {
                req.setAttribute("roomRatingSummaries", reviewDao.getRoomRatingSummaries());
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Failed to load room rating summaries", ex);
                req.setAttribute("roomRatingSummaries", Collections.emptyList());
            }
            req.setAttribute("_csrf", SecurityUtil.tokenForSession(req.getSession(true)));
            req.getRequestDispatcher("/WEB-INF/views/admin/reviews.jsp").forward(req, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error loading reviews for moderation", e);
            Flash.danger(req, "Database error loading reviews.");
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        @SuppressWarnings("unchecked")
        Set<String> perms = (session != null) ? (Set<String>) session.getAttribute("permissions") : null;

        if (perms == null || !perms.contains("REVIEW_MODERATE")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to moderate reviews.");
            return;
        }

        if (!SecurityUtil.valid(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token.");
            return;
        }

        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        Long userId = (authUser != null) ? authUser.getUserId() : null;
        String ip = req.getRemoteAddr();

        String action = req.getParameter("action");
        String reviewIdStr = req.getParameter("reviewId");
        String redirectQuery = req.getParameter("redirectQuery");

        if (reviewIdStr == null || reviewIdStr.trim().isEmpty()) {
            Flash.danger(req, "Review ID is required.");
            redirectToReviews(req, resp, redirectQuery);
            return;
        }

        long reviewId;
        try {
            reviewId = Long.parseLong(reviewIdStr.trim());
        } catch (NumberFormatException e) {
            Flash.danger(req, "Invalid Review ID format.");
            redirectToReviews(req, resp, redirectQuery);
            return;
        }

        try {
            Review rev = reviewDao.findById(reviewId);
            if (rev == null) {
                Flash.danger(req, "Review #" + reviewId + " not found.");
                redirectToReviews(req, resp, redirectQuery);
                return;
            }

            String reason = req.getParameter("reason");

            if ("hide".equalsIgnoreCase(action)) {
                reviewDao.updateStatus(reviewId, "HIDDEN", userId);
                String reasonLog = (reason != null && !reason.trim().isEmpty()) ? " (Reason: " + reason.trim() + ")" : "";
                LOGGER.info("Review #" + reviewId + " hidden by admin #" + userId + reasonLog);
                Flash.success(req, "Review #" + reviewId + " has been hidden from public display."
                        + (reason != null && !reason.trim().isEmpty() ? " Reason: " + reason.trim() : ""));
            } else if ("show".equalsIgnoreCase(action)) {
                reviewDao.updateStatus(reviewId, "VISIBLE", userId);
                LOGGER.info("Review #" + reviewId + " made visible publicly by admin #" + userId);
                Flash.success(req, "Review #" + reviewId + " is now visible publicly.");
            } else {
                Flash.danger(req, "Unrecognized moderation action: " + action);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to perform review moderation action: " + action, e);
            Flash.danger(req, "Database error during review moderation.");
        }

        redirectToReviews(req, resp, redirectQuery);
    }

    private void redirectToReviews(HttpServletRequest req, HttpServletResponse resp, String redirectQuery)
            throws IOException {
        String target = req.getContextPath() + "/admin/reviews";
        if (redirectQuery != null && !redirectQuery.trim().isEmpty()
                && redirectQuery.indexOf('\r') < 0 && redirectQuery.indexOf('\n') < 0) {
            target += "?" + redirectQuery.trim();
        }
        resp.sendRedirect(target);
    }
}

