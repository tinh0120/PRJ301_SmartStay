package servlet;

import dao.AssignmentDao;
import model.Assignment;
import model.User.AuthUser;
import service.AssignmentService;
import service.CredentialService;
import util.Flash;
import util.PageResult;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controller consolidating staff housekeeping portal: assignment listing/detail, cleaning session actions, and PIN viewing.
 */
@WebServlet(name = "StaffServlet", urlPatterns = {
    "/staff/assignments",
    "/staff/cleaning",
    "/staff/pin"
})
public class StaffServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(StaffServlet.class.getName());

    private final AssignmentDao assignmentDao = new AssignmentDao();
    private final AssignmentService assignmentService = new AssignmentService();
    private final CredentialService credentialService = new CredentialService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/staff/assignments".equals(path)) {
            handleAssignmentsGet(req, resp);
            return;
        }
        if ("/staff/cleaning".equals(path) || "/staff/pin".equals(path)) {
            resp.sendRedirect(req.getContextPath() + "/staff/assignments");
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/staff/cleaning".equals(path)) {
            handleCleaningPost(req, resp);
            return;
        }
        if ("/staff/pin".equals(path)) {
            handlePinPost(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleAssignmentsGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AuthUser authUser = (AuthUser) req.getSession().getAttribute("authUser");
        if (authUser == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String idStr = req.getParameter("id");
        if (idStr != null && !idStr.trim().isEmpty()) {
            // View assignment detail
            long assignmentId;
            try {
                assignmentId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException nfe) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Assignment not found.");
                return;
            }

            try {
                Assignment assignment = assignmentDao.findById(assignmentId);
                // 404 if not found or belongs to another staff member
                if (assignment == null || assignment.getStaffId() != authUser.getUserId()) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Assignment not found.");
                    return;
                }

                req.setAttribute("assignment", assignment);
                req.setAttribute("credentialInfo", credentialService.getCredentialForAssignment(assignmentId));

                req.getRequestDispatcher("/WEB-INF/views/staff/assignment-detail.jsp").forward(req, resp);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load assignment detail #" + assignmentId, e);
                Flash.error(req, "Failed to load assignment detail from database.");
                resp.sendRedirect(req.getContextPath() + "/staff/assignments");
            }
        } else {
            // View paginated list of own assignments
            int page = 1;
            String pageStr = req.getParameter("page");
            if (pageStr != null && !pageStr.trim().isEmpty()) {
                try {
                    page = Math.max(1, Integer.parseInt(pageStr.trim()));
                } catch (NumberFormatException ignored) {}
            }
            int pageSize = 15;

            try {
                PageResult<Assignment> pageResult = assignmentDao.findPageByStaff(authUser.getUserId(), page, pageSize);
                req.setAttribute("pageResult", pageResult);
                req.setAttribute("assignments", pageResult.getItems());
                req.getRequestDispatcher("/WEB-INF/views/staff/assignments.jsp").forward(req, resp);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load staff assignments for staff #" + authUser.getUserId(), e);
                Flash.error(req, "Failed to load assignments from database.");
                req.getRequestDispatcher("/WEB-INF/views/staff/assignments.jsp").forward(req, resp);
            }
        }
    }

    private void handleCleaningPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AuthUser authUser = (AuthUser) req.getSession().getAttribute("authUser");
        if (authUser == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String idStr = req.getParameter("assignmentId");
        long assignmentId = -1;
        if (idStr != null && !idStr.trim().isEmpty()) {
            try {
                assignmentId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        if (assignmentId <= 0) {
            Flash.error(req, "Invalid assignment ID.");
            resp.sendRedirect(req.getContextPath() + "/staff/assignments");
            return;
        }

        String action = req.getParameter("action");
        if (action == null) {
            action = "";
        }

        try {
            if ("start".equalsIgnoreCase(action)) {
                boolean started = assignmentService.startCleaning(authUser.getUserId(), assignmentId, req.getRemoteAddr());
                if (started) {
                    Flash.success(req, "Cleaning session started. Please complete it before expiry.");
                } else {
                    Flash.info(req, "Cleaning session is already in progress.");
                }
            } else if ("complete".equalsIgnoreCase(action)) {
                assignmentService.completeCleaning(authUser.getUserId(), assignmentId, req.getRemoteAddr());
                Flash.success(req, "Cleaning session completed! Room has been returned to AVAILABLE status.");
            } else {
                Flash.error(req, "Unsupported action: " + action);
            }
        } catch (IllegalArgumentException | IllegalStateException ie) {
            Flash.error(req, ie.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to process cleaning session action for assignment #" + assignmentId, e);
            Flash.error(req, "Failed to update cleaning session: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/staff/assignments?id=" + assignmentId);
    }

    private void handlePinPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AuthUser authUser = (AuthUser) req.getSession().getAttribute("authUser");
        if (authUser == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String idStr = req.getParameter("assignmentId");
        long assignmentId = -1;
        if (idStr != null && !idStr.trim().isEmpty()) {
            try {
                assignmentId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        if (assignmentId <= 0) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Assignment not found.");
            return;
        }

        try {
            Assignment assignment = assignmentDao.findById(assignmentId);
            if (assignment == null || assignment.getStaffId() != authUser.getUserId()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Assignment not found.");
                return;
            }

            CredentialService.CredentialInfo credInfo = credentialService.getCredentialForAssignment(assignmentId);
            if (credInfo == null || (!"PENDING".equalsIgnoreCase(credInfo.getStatus()) && !"ACTIVE".equalsIgnoreCase(credInfo.getStatus()))) {
                Flash.error(req, "No active PIN credential is available for this assignment.");
                resp.sendRedirect(req.getContextPath() + "/staff/assignments?id=" + assignmentId);
                return;
            }

            String pin = credentialService.decryptPinForStaffAssignment(assignmentId, authUser.getUserId());
            if (pin != null) {
                LOGGER.info("Staff " + authUser.getUserId() + " viewed PIN for credential " + credInfo.getCredentialId() + " on assignment #" + assignmentId);
                req.setAttribute("shownPin", pin);
            } else {
                Flash.error(req, "Could not decrypt PIN for this assignment.");
            }

            req.setAttribute("assignment", assignment);
            req.setAttribute("credentialInfo", credInfo);

            req.getRequestDispatcher("/WEB-INF/views/staff/assignment-detail.jsp").forward(req, resp);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to decrypt staff PIN for assignment #" + assignmentId, e);
            Flash.error(req, "Failed to retrieve PIN due to a database error.");
            resp.sendRedirect(req.getContextPath() + "/staff/assignments?id=" + assignmentId);
        }
    }
}
