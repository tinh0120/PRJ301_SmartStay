package servlet;

import model.User.AuthUser;
import model.User;
import service.UserAdminService;
import util.Flash;
import util.PageResult.PageRequest;
import util.PageResult;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Administrative controller consolidating user listing, creation/editing, and deletion.
 */
@WebServlet(name = "UserAdminServlet", urlPatterns = {"/admin/users", "/admin/users/edit", "/admin/users/delete"})
public class UserAdminServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(UserAdminServlet.class.getName());
    private static final String[] SORT_KEYS = {"name", "email", "role", "status", "created"};

    private final UserAdminService userAdminService = new UserAdminService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/users".equals(path)) {
            listUsers(req, resp);
            return;
        }
        if ("/admin/users/edit".equals(path)) {
            showEditForm(req, resp);
            return;
        }
        if ("/admin/users/delete".equals(path)) {
            resp.sendRedirect(req.getContextPath() + "/admin/users");
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/users/edit".equals(path)) {
            saveUser(req, resp);
            return;
        }
        if ("/admin/users/delete".equals(path)) {
            deleteUser(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void listUsers(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        PageRequest pr = PageRequest.from(req, SORT_KEYS, "created", "DESC");
        try {
            PageResult<User> pageResult = userAdminService.getUserPage(pr);
            req.setAttribute("pageResult", pageResult);
            req.setAttribute("pr", pr);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load user list", e);
            req.setAttribute("dbError", "Failed to load users from database.");
        }
        req.getRequestDispatcher("/WEB-INF/views/admin/users.jsp").forward(req, resp);
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        String actorRole = (session != null) ? (String) session.getAttribute("roleName") : "";

        String userIdStr = req.getParameter("userId");
        User targetUser;

        if (userIdStr != null && !userIdStr.trim().isEmpty()) {
            try {
                long userId = Long.parseLong(userIdStr.trim());
                targetUser = userAdminService.getUserWithStaff(userId);
                if (targetUser == null) {
                    Flash.danger(req, "User not found.");
                    resp.sendRedirect(req.getContextPath() + "/admin/users");
                    return;
                }

                // Admin cannot view or edit Owner accounts
                if ("OWNER".equalsIgnoreCase(targetUser.getRoleName()) && !"OWNER".equalsIgnoreCase(actorRole)) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req, resp);
                    return;
                }
            } catch (NumberFormatException | SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load user for edit", e);
                Flash.danger(req, "Invalid user ID or database error.");
                resp.sendRedirect(req.getContextPath() + "/admin/users");
                return;
            }
        } else {
            targetUser = new User();
            targetUser.setStatus("ACTIVE");
            targetUser.setRoleName("STAFF");
            targetUser.setEmploymentStatus("ACTIVE");
        }

        req.setAttribute("targetUser", targetUser);
        req.setAttribute("availableRoles", getAvailableRoles(actorRole));
        req.getRequestDispatcher("/WEB-INF/views/admin/user-form.jsp").forward(req, resp);
    }

    private void saveUser(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        String actorRole = (session != null) ? (String) session.getAttribute("roleName") : "";
        long actorUserId = (authUser != null) ? authUser.getUserId() : 1L;

        String userIdStr = req.getParameter("userId");
        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String phone = req.getParameter("phone");
        String targetRole = req.getParameter("role");
        String status = req.getParameter("status");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");
        String position = req.getParameter("position");
        String employmentStatus = req.getParameter("employmentStatus");
        String hireDateStr = req.getParameter("hireDate");

        // Guard against role assignment privilege escalation (Spec M8)
        if ("OWNER".equalsIgnoreCase(targetRole)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req, resp);
            return;
        }

        if ("ADMIN".equalsIgnoreCase(actorRole)) {
            if ("ADMIN".equalsIgnoreCase(targetRole) || "OWNER".equalsIgnoreCase(targetRole)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req, resp);
                return;
            }
        }

        Long targetUserId = null;
        if (userIdStr != null && !userIdStr.trim().isEmpty()) {
            try {
                targetUserId = Long.parseLong(userIdStr.trim());
            } catch (NumberFormatException e) {
                Flash.danger(req, "Invalid user ID.");
                resp.sendRedirect(req.getContextPath() + "/admin/users");
                return;
            }
        }

        try {
            UserAdminService.SaveResult result = userAdminService.saveUser(
                    targetUserId, fullName, email, phone, targetRole, status,
                    password, confirmPassword, position, employmentStatus, hireDateStr,
                    actorUserId, actorRole, req.getRemoteAddr()
            );

            if (result.ok) {
                Flash.success(req, targetUserId == null ? "User created successfully." : "User updated successfully.");
                resp.sendRedirect(req.getContextPath() + "/admin/users");
                return;
            }

            // Validation or uniqueness conflict failed
            req.setAttribute("globalError", result.globalError);
            req.setAttribute("fieldErrors", result.fieldErrors);
            User entered = buildEnteredUser(targetUserId, fullName, email, phone, targetRole, status,
                    position, employmentStatus, hireDateStr);
            req.setAttribute("targetUser", entered);
            req.setAttribute("availableRoles", getAvailableRoles(actorRole));
            req.getRequestDispatcher("/WEB-INF/views/admin/user-form.jsp").forward(req, resp);

        } catch (SecurityException e) {
            LOGGER.log(Level.WARNING, "Security violation in user form submission", e);
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req, resp);
        } catch (IllegalStateException e) {
            req.setAttribute("globalError", e.getMessage());
            User entered = buildEnteredUser(targetUserId, fullName, email, phone, targetRole, status,
                    position, employmentStatus, hireDateStr);
            req.setAttribute("targetUser", entered);
            req.setAttribute("availableRoles", getAvailableRoles(actorRole));
            req.getRequestDispatcher("/WEB-INF/views/admin/user-form.jsp").forward(req, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error saving user", e);
            req.setAttribute("globalError", "A database error occurred while saving user.");
            User entered = buildEnteredUser(targetUserId, fullName, email, phone, targetRole, status,
                    position, employmentStatus, hireDateStr);
            req.setAttribute("targetUser", entered);
            req.setAttribute("availableRoles", getAvailableRoles(actorRole));
            req.getRequestDispatcher("/WEB-INF/views/admin/user-form.jsp").forward(req, resp);
        }
    }

    private void deleteUser(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        String actorRole = (session != null) ? (String) session.getAttribute("roleName") : "";
        long actorUserId = (authUser != null) ? authUser.getUserId() : 1L;

        String userIdStr = req.getParameter("userId");
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            Flash.danger(req, "User ID is required for deletion.");
            resp.sendRedirect(req.getContextPath() + "/admin/users");
            return;
        }

        long targetUserId;
        try {
            targetUserId = Long.parseLong(userIdStr.trim());
        } catch (NumberFormatException e) {
            Flash.danger(req, "Invalid user ID format.");
            resp.sendRedirect(req.getContextPath() + "/admin/users");
            return;
        }

        try {
            userAdminService.deleteUser(targetUserId, actorUserId, actorRole, req.getRemoteAddr());
            Flash.success(req, "User deleted successfully.");
        } catch (SecurityException e) {
            LOGGER.log(Level.WARNING, "Security violation while deleting user: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req, resp);
            return;
        } catch (IllegalStateException | IllegalArgumentException e) {
            Flash.danger(req, e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error deleting user " + targetUserId, e);
            Flash.danger(req, "Failed to delete user due to a database error.");
        }

        resp.sendRedirect(req.getContextPath() + "/admin/users");
    }

    private List<String> getAvailableRoles(String actorRole) {
        if ("OWNER".equalsIgnoreCase(actorRole)) {
            return Arrays.asList("GUEST", "ADMIN", "STAFF");
        }
        return Arrays.asList("GUEST", "STAFF");
    }

    private User buildEnteredUser(Long targetUserId, String fullName, String email, String phone,
                                 String targetRole, String status, String position,
                                 String employmentStatus, String hireDateStr) {
        User u = new User();
        u.setUserId(targetUserId);
        u.setFullName(fullName != null ? fullName.trim() : "");
        u.setEmail(email != null ? email.trim() : "");
        u.setPhone(phone != null ? phone.trim() : "");
        u.setRoleName(targetRole != null ? targetRole.trim().toUpperCase() : "GUEST");
        u.setStatus(status != null ? status.trim().toUpperCase() : "ACTIVE");
        u.setPosition(position != null ? position.trim() : "");
        u.setEmploymentStatus(employmentStatus != null ? employmentStatus.trim().toUpperCase() : "ACTIVE");
        if (hireDateStr != null && !hireDateStr.trim().isEmpty()) {
            try {
                u.setHireDate(Date.valueOf(hireDateStr.trim()));
            } catch (Exception ignored) {}
        }
        return u;
    }
}
