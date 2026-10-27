package filter;

import model.User.AuthUser;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Filter enforcing Role-Based Access Control (RBAC) and fine-grained permissions.
 * <p>
 * Permission Matrix Reference (Spec Section 7 & T02, Round 2 ADDENDUM):
 * <ul>
 *   <li>OWNER (all 30 permissions): Full access across all modules.</li>
 *   <li>ADMIN: Granted all permissions held by Owner.</li>
 *   <li>STAFF (2 permissions): ASSIGNMENT_OWN, CLEANING_PERFORM. Redirected from /admin to /staff/assignments.</li>
 *   <li>GUEST (2 permissions): BOOKING_OWN, REVIEW_CREATE.</li>
 * </ul>
 */
@WebFilter(filterName = "PermissionFilter", urlPatterns = "/*")
public class PermissionFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(PermissionFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI().substring(req.getContextPath().length());

        // Skip non-protected prefixes
        if (!path.startsWith("/guest/") && !path.startsWith("/staff/") && !path.startsWith("/admin/") && !"/admin".equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Public payment status/result callback endpoints are handled by GuestPaymentServlet
        if ("/guest/payment/result".equals(path) || "/guest/payment/status".equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        String roleName = (session != null) ? (String) session.getAttribute("roleName") : null;
        @SuppressWarnings("unchecked")
        Set<String> perms = (session != null) ? (Set<String>) session.getAttribute("permissions") : null;

        if (authUser == null || roleName == null) {
            // AuthFilter handles unauthenticated requests; if reached here, forbid
            deny(req, resp, null, path);
            return;
        }

        // 1. Guest routes
        if (path.startsWith("/guest/")) {
            if (!"GUEST".equalsIgnoreCase(roleName) && !"ADMIN".equalsIgnoreCase(roleName) && !"OWNER".equalsIgnoreCase(roleName)) {
                deny(req, resp, authUser.getUserId(), path);
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // 2. Staff routes
        if (path.startsWith("/staff/")) {
            if (!"STAFF".equalsIgnoreCase(roleName) || perms == null || !perms.contains("ASSIGNMENT_OWN")) {
                deny(req, resp, authUser.getUserId(), path);
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // 3. Admin routes (ADDENDUM Rule 5: Dashboard and admin routes are for OWNER and ADMIN only. Staff get redirect to /staff/assignments)
        if (path.startsWith("/admin/") || "/admin".equals(path)) {
            if (!"OWNER".equalsIgnoreCase(roleName) && !"ADMIN".equalsIgnoreCase(roleName)) {
                if ("STAFF".equalsIgnoreCase(roleName)) {
                    resp.sendRedirect(req.getContextPath() + "/staff/assignments");
                    return;
                }
                deny(req, resp, authUser.getUserId(), path);
                return;
            }

            // Fine-grained path checks for OWNER and ADMIN
            if (path.startsWith("/admin/rooms/slots")) {
                if (perms == null || !perms.contains("ROOM_SLOT_MANAGE")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/rooms")) {
                if (perms == null || !perms.contains("ROOM_MANAGE")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/users")) {
                if (perms == null || !perms.contains("USER_MANAGE")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/dashboard")) {
                if (perms == null || !perms.contains("DASHBOARD_VIEW")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/reports")) {
                if (perms == null || !perms.contains("REPORT_REVENUE")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/bookings/noshow")) {
                if (perms == null || !perms.contains("BOOKING_MANAGE") || !perms.contains("NOSHOW_INTERVENE")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/bookings/refund")) {
                if (perms == null || (!perms.contains("PAYMENT_RESOLVE") && !perms.contains("BOOKING_MANAGE"))) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/bookings")) {
                if (perms == null || !perms.contains("BOOKING_MANAGE")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/payments/resolve")) {
                if (perms == null || !perms.contains("PAYMENT_RESOLVE")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/payments")) {
                if (perms == null || !perms.contains("BOOKING_MANAGE")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/credentials/revoke")) {
                if (perms == null || !perms.contains("CREDENTIAL_REVOKE")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/assignments")) {
                if (perms == null || !perms.contains("ASSIGNMENT_MANAGE")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/devices")) {
                if (perms == null || !perms.contains("DEVICE_MANAGE")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/thresholds")) {
                if (perms == null || !perms.contains("THRESHOLD_MANAGE")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/sensor-readings/import")) {
                if (perms == null || !perms.contains("DEVICE_MANAGE")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/sensor-readings")) {
                if (perms == null || !perms.contains("SENSOR_VIEW")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/access-logs")) {
                if (perms == null || !perms.contains("LOG_ACCESS_VIEW")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/motion-logs")) {
                if (perms == null || !perms.contains("LOG_MOTION_VIEW")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/activity")) {
                // R9: merged Activity logs page; each tab re-checks its own permission in LogAdminServlet
                if (perms == null || (!perms.contains("LOG_ACCESS_VIEW") && !perms.contains("LOG_MOTION_VIEW"))) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/alerts/action")) {
                if (perms == null || (!perms.contains("ALERT_HANDLE") && !"OWNER".equalsIgnoreCase(roleName) && !"ADMIN".equalsIgnoreCase(roleName))) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/alerts")) {
                boolean isPost = "POST".equalsIgnoreCase(req.getMethod());
                String requiredPerm = isPost ? "ALERT_HANDLE" : "ALERT_VIEW";
                if (perms == null || (!perms.contains(requiredPerm) && !"OWNER".equalsIgnoreCase(roleName) && !"ADMIN".equalsIgnoreCase(roleName))) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            } else if (path.startsWith("/admin/reviews")) {
                if (perms == null || !perms.contains("REVIEW_MODERATE")) {
                    deny(req, resp, authUser.getUserId(), path);
                    return;
                }
            }

            chain.doFilter(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    private void deny(HttpServletRequest req, HttpServletResponse resp, Long userId, String path)
            throws ServletException, IOException {
        LOGGER.info("Access denied for user " + userId + " attempting to access " + path);
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req, resp);
    }

    @Override
    public void destroy() {
    }
}
