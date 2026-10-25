package filter;

import model.User.AuthUser;
import util.Flash;
import java.io.IOException;
import java.net.URLEncoder;
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
 * Filter enforcing authentication requirements and caching headers across application routes.
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = "/*")
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Apply strict anti-caching headers on all dynamic responses
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");

        String contextPath = req.getContextPath();
        String path = req.getRequestURI().substring(contextPath.length());

        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;

        // If logged-in user navigates to login or register, redirect to role landing page
        if (authUser != null && ("GET".equalsIgnoreCase(req.getMethod()))) {
            if ("/login".equalsIgnoreCase(path) || "/register".equalsIgnoreCase(path)) {
                String landing = getLandingUrl(contextPath, authUser.getRoleName());
                resp.sendRedirect(landing);
                return;
            }
        }

        // Check if path is public
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Path requires authentication
        if (authUser == null) {
            if ("GET".equalsIgnoreCase(req.getMethod())) {
                String target = path;
                String query = req.getQueryString();
                if (query != null && !query.trim().isEmpty()) {
                    target += "?" + query;
                }
                String nextParam = URLEncoder.encode(target, "UTF-8");
                resp.sendRedirect(contextPath + "/login?next=" + nextParam);
            } else {
                if (session != null) {
                    Flash.warning(req, "Your session expired. Please log in again.");
                }
                resp.sendRedirect(contextPath + "/login");
            }
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path) || "/home".equals(path)
                || "/rooms".equals(path) || "/rooms/detail".equals(path)
                || "/contact".equals(path) || "/terms".equals(path) || "/policy".equals(path) || "/house-rules".equals(path)
                || "/login".equals(path) || "/register".equals(path)
                || "/guest/payment/result".equals(path) || "/guest/payment/status".equals(path)) {
            return true;
        }

        return path.startsWith("/assets/")
                || path.startsWith("/api/iot/")
                || path.startsWith("/api/payments/")
                || path.startsWith("/mock-payos/")
                || path.startsWith("/ws/");
    }

    public static String getLandingUrl(String contextPath, String roleName) {
        if ("GUEST".equalsIgnoreCase(roleName)) {
            return contextPath + "/guest/bookings";
        }
        if ("STAFF".equalsIgnoreCase(roleName)) {
            return contextPath + "/staff/assignments";
        }
        if ("ADMIN".equalsIgnoreCase(roleName) || "OWNER".equalsIgnoreCase(roleName)) {
            return contextPath + "/admin/dashboard";
        }
        return contextPath + "/home";
    }

    @Override
    public void destroy() {
    }
}
