package servlet;

import dao.AssignmentDao;
import dao.RoomDao;
import dao.UserDao;
import model.Assignment;
import model.User.AuthUser;
import model.Room;
import model.User;
import service.AssignmentService;
import util.Flash;
import util.PageResult;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Administrative controller consolidating staff assignment listing, creation/editing, and cancellation.
 */
@WebServlet(name = "AssignmentAdminServlet", urlPatterns = {"/admin/assignments", "/admin/assignments/edit", "/admin/assignments/cancel"})
public class AssignmentAdminServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AssignmentAdminServlet.class.getName());

    private final AssignmentService assignmentService = new AssignmentService();
    private final AssignmentDao assignmentDao = new AssignmentDao();
    private final UserDao userDao = new UserDao();
    private final RoomDao roomDao = new RoomDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/assignments".equals(path)) {
            listAssignments(req, resp);
            return;
        }
        if ("/admin/assignments/edit".equals(path)) {
            showEditForm(req, resp);
            return;
        }
        if ("/admin/assignments/cancel".equals(path)) {
            resp.sendRedirect(req.getContextPath() + "/admin/assignments");
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/assignments/edit".equals(path)) {
            saveAssignment(req, resp);
            return;
        }
        if ("/admin/assignments/cancel".equals(path)) {
            cancelAssignment(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void listAssignments(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String search = req.getParameter("search");
        String status = req.getParameter("status");

        Long staffId = null;
        String staffIdStr = req.getParameter("staffId");
        if (staffIdStr != null && !staffIdStr.trim().isEmpty()) {
            try {
                staffId = Long.parseLong(staffIdStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        Long roomId = null;
        String roomIdStr = req.getParameter("roomId");
        if (roomIdStr != null && !roomIdStr.trim().isEmpty()) {
            try {
                roomId = Long.parseLong(roomIdStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        LocalDate shiftDate = null;
        String shiftDateStr = req.getParameter("shiftDate");
        if (shiftDateStr != null && !shiftDateStr.trim().isEmpty()) {
            try {
                shiftDate = LocalDate.parse(shiftDateStr.trim());
            } catch (DateTimeParseException ignored) {}
        }

        int page = 1;
        String pageStr = req.getParameter("page");
        if (pageStr != null && !pageStr.trim().isEmpty()) {
            try {
                page = Math.max(1, Integer.parseInt(pageStr.trim()));
            } catch (NumberFormatException ignored) {}
        }
        int pageSize = 15;
        String sortCol = req.getParameter("sort");
        String sortDir = req.getParameter("dir");

        try {
            PageResult<Assignment> pageResult = assignmentDao.findPageAdmin(
                    search, status, staffId, roomId, shiftDate, sortCol, sortDir, page, pageSize);

            List<User> staffList = userDao.findAllActiveStaff();
            List<Room> roomList = roomDao.findAllActive();

            req.setAttribute("pageResult", pageResult);
            req.setAttribute("assignments", pageResult.getItems());
            req.setAttribute("staffList", staffList);
            req.setAttribute("roomList", roomList);
            req.setAttribute("search", search != null ? search : "");
            req.setAttribute("status", status != null ? status : "");
            req.setAttribute("staffId", staffId);
            req.setAttribute("roomId", roomId);
            req.setAttribute("shiftDate", shiftDate);

            req.getRequestDispatcher("/WEB-INF/views/admin/assignments.jsp").forward(req, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load assignments list", e);
            Flash.error(req, "Failed to load assignments from database.");
            req.getRequestDispatcher("/WEB-INF/views/admin/assignments.jsp").forward(req, resp);
        }
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String idStr = req.getParameter("id");
        Long assignmentId = null;
        if (idStr != null && !idStr.trim().isEmpty()) {
            try {
                assignmentId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        try {
            List<User> staffList = userDao.findAllActiveStaff();
            req.setAttribute("staffList", staffList);

            if (assignmentId != null && assignmentId > 0) {
                // Edit mode
                Assignment assignment = assignmentDao.findById(assignmentId);
                if (assignment == null) {
                    Flash.error(req, "Assignment not found.");
                    resp.sendRedirect(req.getContextPath() + "/admin/assignments");
                    return;
                }
                if (!Assignment.STATUS_ASSIGNED.equalsIgnoreCase(assignment.getStatus())) {
                    Flash.error(req, "Only assignments in ASSIGNED status can be edited.");
                    resp.sendRedirect(req.getContextPath() + "/admin/assignments");
                    return;
                }

                req.setAttribute("mode", "edit");
                req.setAttribute("assignment", assignment);
                req.setAttribute("roomList", roomDao.findAllActive());

            } else {
                // Create mode - only rooms needing cleaning
                req.setAttribute("mode", "create");
                List<Room> cleaningRooms = roomDao.findByStatus("NEEDS_CLEANING");
                req.setAttribute("roomList", cleaningRooms);
            }

            req.getRequestDispatcher("/WEB-INF/views/admin/assignment-form.jsp").forward(req, resp);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load assignment form", e);
            Flash.error(req, "Failed to load form from database.");
            resp.sendRedirect(req.getContextPath() + "/admin/assignments");
        }
    }

    private void saveAssignment(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AuthUser authUser = (AuthUser) req.getSession().getAttribute("authUser");
        if (authUser == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String idStr = req.getParameter("assignmentId");
        Long assignmentId = null;
        if (idStr != null && !idStr.trim().isEmpty()) {
            try {
                assignmentId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        String staffIdStr = req.getParameter("staffId");
        long staffId = -1;
        if (staffIdStr != null && !staffIdStr.trim().isEmpty()) {
            try {
                staffId = Long.parseLong(staffIdStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        String shiftStartStr = req.getParameter("shiftStart");
        String shiftEndStr = req.getParameter("shiftEnd");
        String note = req.getParameter("note");

        LocalDateTime shiftStart = null;
        LocalDateTime shiftEnd = null;

        try {
            if (shiftStartStr != null && !shiftStartStr.trim().isEmpty()) {
                shiftStart = LocalDateTime.parse(shiftStartStr.trim());
            }
            if (shiftEndStr != null && !shiftEndStr.trim().isEmpty()) {
                shiftEnd = LocalDateTime.parse(shiftEndStr.trim());
            }
        } catch (DateTimeParseException dtpe) {
            Flash.error(req, "Invalid shift start or end time format. Please use yyyy-MM-ddTHH:mm.");
            String backUrl = (assignmentId != null) ? "/admin/assignments/edit?id=" + assignmentId : "/admin/assignments/edit";
            resp.sendRedirect(req.getContextPath() + backUrl);
            return;
        }

        if (staffId <= 0 || shiftStart == null || shiftEnd == null) {
            Flash.error(req, "Please select staff and provide valid shift start and end times.");
            String backUrl = (assignmentId != null) ? "/admin/assignments/edit?id=" + assignmentId : "/admin/assignments/edit";
            resp.sendRedirect(req.getContextPath() + backUrl);
            return;
        }

        if (!shiftEnd.isAfter(shiftStart)) {
            Flash.error(req, "Shift end time must be after shift start time.");
            String backUrl = (assignmentId != null) ? "/admin/assignments/edit?id=" + assignmentId : "/admin/assignments/edit";
            resp.sendRedirect(req.getContextPath() + backUrl);
            return;
        }

        if (assignmentId != null && assignmentId > 0) {
            // Edit assignment
            try {
                assignmentService.updateAssignment(
                        authUser.getUserId(), assignmentId, staffId, shiftStart, shiftEnd, note, req.getRemoteAddr());
                Flash.success(req, "Assignment #" + assignmentId + " updated successfully.");
                resp.sendRedirect(req.getContextPath() + "/admin/assignments");
            } catch (IllegalArgumentException | IllegalStateException ie) {
                Flash.error(req, ie.getMessage());
                resp.sendRedirect(req.getContextPath() + "/admin/assignments/edit?id=" + assignmentId);
            } catch (SQLException se) {
                LOGGER.log(Level.SEVERE, "Failed to update assignment #" + assignmentId, se);
                Flash.error(req, "Failed to update assignment: " + se.getMessage());
                resp.sendRedirect(req.getContextPath() + "/admin/assignments/edit?id=" + assignmentId);
            }

        } else {
            // Create assignment
            String roomIdStr = req.getParameter("roomId");
            long roomId = -1;
            if (roomIdStr != null && !roomIdStr.trim().isEmpty()) {
                try {
                    roomId = Long.parseLong(roomIdStr.trim());
                } catch (NumberFormatException ignored) {}
            }

            if (roomId <= 0) {
                Flash.error(req, "Please select a room for the assignment.");
                resp.sendRedirect(req.getContextPath() + "/admin/assignments/edit");
                return;
            }

            try {
                long newId = assignmentService.createAssignment(
                        authUser.getUserId(), staffId, roomId, shiftStart, shiftEnd, note, req.getRemoteAddr());
                Flash.success(req, "Assignment #" + newId + " created successfully with staff PIN credential.");
                resp.sendRedirect(req.getContextPath() + "/admin/assignments");
            } catch (IllegalArgumentException | IllegalStateException ie) {
                Flash.error(req, ie.getMessage());
                resp.sendRedirect(req.getContextPath() + "/admin/assignments/edit");
            } catch (SQLException se) {
                LOGGER.log(Level.SEVERE, "Failed to create assignment", se);
                Flash.error(req, "Failed to create assignment: " + se.getMessage());
                resp.sendRedirect(req.getContextPath() + "/admin/assignments/edit");
            }
        }
    }

    private void cancelAssignment(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AuthUser authUser = (AuthUser) req.getSession().getAttribute("authUser");
        if (authUser == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String idStr = req.getParameter("id");
        long assignmentId = -1;
        if (idStr != null && !idStr.trim().isEmpty()) {
            try {
                assignmentId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        if (assignmentId <= 0) {
            Flash.error(req, "Invalid assignment ID.");
            resp.sendRedirect(req.getContextPath() + "/admin/assignments");
            return;
        }

        try {
            assignmentService.cancelAssignment(authUser.getUserId(), assignmentId, req.getRemoteAddr());
            Flash.success(req, "Assignment #" + assignmentId + " and its cleaning session cancelled, PIN revoked.");
        } catch (IllegalStateException ie) {
            Flash.error(req, ie.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to cancel assignment #" + assignmentId, e);
            Flash.error(req, "Failed to cancel assignment: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/admin/assignments");
    }
}
