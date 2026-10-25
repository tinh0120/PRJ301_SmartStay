package servlet;

import dao.UserDao;
import filter.AuthFilter;
import model.User.AuthUser;
import model.User;
import service.AuthService;
import util.SecurityUtil;
import util.Flash;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
 * Controller consolidating authentication lifecycle: login, registration, and logout.
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/login", "/logout", "/register"})
public class AuthServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AuthServlet.class.getName());
    private final AuthService authService = new AuthService();
    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/logout".equals(path)) {
            resp.sendRedirect(req.getContextPath() + "/");
            return;
        }
        if ("/register".equals(path)) {
            showRegister(req, resp);
            return;
        }
        showLogin(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/logout".equals(path)) {
            handleLogout(req, resp);
            return;
        }
        if ("/register".equals(path)) {
            handleRegister(req, resp);
            return;
        }
        handleLogin(req, resp);
    }

    // =========================================================================
    // LOGIN
    // =========================================================================

    private void showLogin(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        if (authUser != null) {
            resp.sendRedirect(AuthFilter.getLandingUrl(req.getContextPath(), authUser.getRoleName()));
            return;
        }

        String next = req.getParameter("next");
        if (next == null || next.trim().isEmpty()) {
            next = extractRelativeReferer(req);
        }
        if (next != null && !next.trim().isEmpty()) {
            req.setAttribute("next", next.trim());
        }

        req.setAttribute("_csrf", SecurityUtil.tokenForSession(req.getSession(true)));
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!SecurityUtil.valid(req)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req, resp);
            return;
        }

        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String next = req.getParameter("next");

        AuthService.Result result = authService.login(email, password, req.getRemoteAddr());

        if (!result.ok) {
            req.setAttribute("globalError", result.error);
            Map<String, String> form = new HashMap<>();
            if (email != null) {
                form.put("email", email.trim());
            }
            req.setAttribute("form", form);
            if (next != null && !next.trim().isEmpty()) {
                req.setAttribute("next", next.trim());
            }
            req.setAttribute("_csrf", SecurityUtil.tokenForSession(req.getSession(true)));
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
            return;
        }

        // Session fixation defence: terminate old session, establish clean session
        HttpSession oldSession = req.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession newSession = req.getSession(true);
        SecurityUtil.tokenForSession(newSession);

        try {
            User user = userDao.findById(result.userId);
            if (user == null) {
                req.setAttribute("globalError", "User record could not be loaded.");
                req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
                return;
            }

            Set<String> permissions = userDao.findPermissionCodes(user.getUserId());
            AuthUser authUser = new AuthUser(user.getUserId(), user.getFullName(), user.getEmail(), user.getRoleName());

            newSession.setAttribute("authUser", authUser);
            newSession.setAttribute("roleName", user.getRoleName());
            newSession.setAttribute("userId", user.getUserId());
            newSession.setAttribute("permissions", new HashSet<>(permissions));

            // Validate same-site redirection URL
            if (isValidNextUrl(next)) {
                String ctx = req.getContextPath();
                if (next.startsWith(ctx + "/")) {
                    resp.sendRedirect(next);
                } else if (next.startsWith("/")) {
                    resp.sendRedirect(ctx + next);
                } else {
                    resp.sendRedirect(ctx + "/" + next);
                }
            } else {
                resp.sendRedirect(AuthFilter.getLandingUrl(req.getContextPath(), user.getRoleName()));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load user profile or permissions on login success", e);
            req.setAttribute("globalError", "A database error occurred during session initialization.");
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
        }
    }

    private boolean isValidNextUrl(String next) {
        if (next == null || next.trim().isEmpty()) {
            return false;
        }
        String trimmed = next.trim();
        return trimmed.startsWith("/") && !trimmed.startsWith("//") && !trimmed.contains("://");
    }

    private String extractRelativeReferer(HttpServletRequest req) {
        String referer = req.getHeader("Referer");
        if (referer == null || referer.trim().isEmpty()) {
            return null;
        }
        try {
            URI refUri = URI.create(referer.trim());
            String refHost = refUri.getHost();
            String reqHost = req.getServerName();
            if (refHost != null && !refHost.equalsIgnoreCase(reqHost)) {
                boolean isRefLoopback = "localhost".equalsIgnoreCase(refHost) || "127.0.0.1".equals(refHost);
                boolean isReqLoopback = "localhost".equalsIgnoreCase(reqHost) || "127.0.0.1".equals(reqHost);
                if (!(isRefLoopback && isReqLoopback)) {
                    return null;
                }
            }
            if (refUri.getPort() != -1 && refUri.getPort() != req.getServerPort()) {
                return null;
            }
            String rawPath = refUri.getRawPath();
            if (rawPath == null || rawPath.isEmpty()) {
                return null;
            }
            String ctx = req.getContextPath();
            String appPath;
            if (ctx != null && !ctx.isEmpty()) {
                if (!rawPath.startsWith(ctx)) {
                    return null;
                }
                appPath = rawPath.substring(ctx.length());
            } else {
                appPath = rawPath;
            }
            if (appPath == null || appPath.isEmpty()) {
                appPath = "/";
            }
            if ("/login".equalsIgnoreCase(appPath) || "/register".equalsIgnoreCase(appPath) || "/logout".equalsIgnoreCase(appPath)) {
                return null;
            }
            String rawQuery = refUri.getRawQuery();
            return (rawQuery != null && !rawQuery.isEmpty()) ? (appPath + "?" + rawQuery) : appPath;
        } catch (Exception ignored) {
            return null;
        }
    }

    // =========================================================================
    // REGISTER
    // =========================================================================

    private void showRegister(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        if (authUser != null) {
            resp.sendRedirect(AuthFilter.getLandingUrl(req.getContextPath(), authUser.getRoleName()));
            return;
        }

        req.setAttribute("_csrf", SecurityUtil.tokenForSession(req.getSession(true)));
        req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!SecurityUtil.valid(req)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req, resp);
            return;
        }

        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String phone = req.getParameter("phone");
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirm");

        AuthService.Result result = authService.register(fullName, email, phone, password, confirm, req.getRemoteAddr());

        if (result.ok) {
            Flash.success(req, "Registration successful. Please log in.");
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        if (result.fieldErrors != null && !result.fieldErrors.isEmpty()) {
            req.setAttribute("errors", result.fieldErrors);
        }
        if (result.error != null) {
            req.setAttribute("globalError", result.error);
        }

        Map<String, String> form = new HashMap<>();
        if (fullName != null) {
            form.put("fullName", fullName.trim());
        }
        if (email != null) {
            form.put("email", email.trim());
        }
        if (phone != null) {
            form.put("phone", phone.trim());
        }
        req.setAttribute("form", form);

        req.setAttribute("_csrf", SecurityUtil.tokenForSession(req.getSession(true)));
        req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
    }

    // =========================================================================
    // LOGOUT
    // =========================================================================

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!SecurityUtil.valid(req)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req, resp);
            return;
        }

        HttpSession session = req.getSession(false);
        if (session != null) {
            Long userId = (Long) session.getAttribute("userId");
            if (userId != null) {
                LOGGER.info("User " + userId + " signed out");
            }
            session.invalidate();
        }

        resp.sendRedirect(req.getContextPath() + "/login");
    }
}

