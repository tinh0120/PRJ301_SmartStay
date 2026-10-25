package filter;

import util.SecurityUtil;
import java.io.IOException;
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
 * Filter protecting mutating HTTP methods against Cross-Site Request Forgery (CSRF).
 */
@WebFilter(filterName = "CsrfFilter", urlPatterns = "/*")
public class CsrfFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(CsrfFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        if (session != null) {
            req.setAttribute(SecurityUtil.CSRF_PARAM, SecurityUtil.tokenForSession(session));
        }

        String method = req.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method) || "TRACE".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.startsWith("/assets/") || path.startsWith("/api/iot/") || path.startsWith("/api/payments/")
                || path.startsWith("/mock-payos/") || path.startsWith("/ws/")) {
            chain.doFilter(request, response);
            return;
        }

        if (!SecurityUtil.valid(req)) {
            LOGGER.warning("CSRF validation failed for " + method + " " + path + " from IP " + req.getRemoteAddr());
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setHeader("X-Content-Type-Options", "nosniff");
            req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req, resp);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}

