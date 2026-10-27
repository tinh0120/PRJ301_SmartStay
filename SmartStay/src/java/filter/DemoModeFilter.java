package filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Hides the demo tools (/demo/*) when the context-param "demo.mode" is false.
 * Mapped in web.xml BEFORE CsrfFilter and AuthFilter so a disabled demo always answers 404,
 * whatever the method, session or CSRF token.
 */
public class DemoModeFilter implements Filter {

    /** Context-param name; missing or blank means enabled (default true). */
    public static final String DEMO_MODE_PARAM = "demo.mode";

    private ServletContext context;

    /** True unless the context-param demo.mode is explicitly set to something other than "true". */
    public static boolean isDemoMode(ServletContext ctx) {
        String v = (ctx != null) ? ctx.getInitParameter(DEMO_MODE_PARAM) : null;
        return v == null || v.trim().isEmpty() || Boolean.parseBoolean(v.trim());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.context = filterConfig.getServletContext();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!isDemoMode(context)) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
