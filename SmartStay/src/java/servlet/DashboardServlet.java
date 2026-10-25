package servlet;

import com.google.gson.Gson;
import dao.DashboardDao;
import model.AlertEntry;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controller consolidating operational dashboard, operational chart metrics, and financial revenue reports.
 * Protected by PermissionFilter (requires DASHBOARD_VIEW / REPORT_REVENUE).
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {
    "/admin/dashboard",
    "/admin/dashboard/charts",
    "/admin/reports/revenue",
    "/admin/reports/revenue/data",
    "/admin/sync-now"
})
public class DashboardServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DashboardServlet.class.getName());

    private final DashboardDao dashboardDao = new DashboardDao();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/dashboard".equals(path)) {
            showDashboard(req, resp);
            return;
        }
        if ("/admin/dashboard/charts".equals(path)) {
            serveCharts(req, resp);
            return;
        }
        if ("/admin/reports/revenue".equals(path)) {
            showRevenueReport(req, resp);
            return;
        }
        if ("/admin/reports/revenue/data".equals(path)) {
            serveRevenueData(req, resp);
            return;
        }
        if ("/admin/sync-now".equals(path)) {
            handleManualSync(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/sync-now".equals(path)) {
            handleManualSync(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleManualSync(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        listener.AppStartListener.triggerSyncNow();
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(req.getHeader("X-Requested-With"))
                || (req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json"));
        if (isAjax) {
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"success\":true,\"message\":\"Background synchronization triggered successfully...\"}");
            return;
        }
        util.Flash.success(req, "Background synchronization triggered successfully!");
        String referer = req.getHeader("Referer");
        if (referer != null && !referer.trim().isEmpty()) {
            resp.sendRedirect(referer);
        } else {
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
        }
    }

    // =========================================================================
    // OPERATIONAL DASHBOARD
    // =========================================================================

    private void showDashboard(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Map<String, Integer> roomCounts = new HashMap<>();
        Map<String, Integer> todayBookings = new HashMap<>();
        Map<String, Integer> deviceCounts = new HashMap<>();
        long revenueToday = 0L;
        long revenueMonth = 0L;
        int newAlertCount = 0;
        int noshowHeldCount = 0;
        int pendingCleaningCount = 0;
        Map<String, Integer> openAlerts = new HashMap<>();
        Map<String, Object> alertsByType = new HashMap<>();
        List<AlertEntry> latestAlerts = Collections.emptyList();

        try {
            roomCounts = dashboardDao.getRoomStatusCounts();
            todayBookings = dashboardDao.getTodayBookingCounts();
            long[] rev = dashboardDao.getRevenueTodayAndMonth();
            revenueToday = rev[0];
            revenueMonth = rev[1];
            deviceCounts = dashboardDao.getDeviceStatusCounts();
            newAlertCount = dashboardDao.getNewAlertCount();
            noshowHeldCount = dashboardDao.getNoshowHeldCount();
            latestAlerts = dashboardDao.getLatestAlerts(5);
            pendingCleaningCount = dashboardDao.getPendingCleaningCount();
            openAlerts = dashboardDao.getOpenAlertsBySeverity();
            alertsByType = dashboardDao.getAlertsByType();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load dashboard operational statistics", e);
            req.setAttribute("dbError", "Operational metrics could not be loaded. Please try again later.");
        }

        req.setAttribute("roomCounts", roomCounts);
        req.setAttribute("todayBookings", todayBookings);
        req.setAttribute("revenueToday", revenueToday);
        req.setAttribute("revenueMonth", revenueMonth);
        req.setAttribute("deviceCounts", deviceCounts);
        req.setAttribute("newAlertCount", newAlertCount);
        req.setAttribute("noshowHeldCount", noshowHeldCount);
        req.setAttribute("latestAlerts", latestAlerts);
        req.setAttribute("pendingCleaningCount", pendingCleaningCount);
        req.setAttribute("openAlerts", openAlerts);
        req.setAttribute("alertsByType", alertsByType);

        req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(req, resp);
    }

    private void serveCharts(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        try {
            Map<String, Object> revenue7 = dashboardDao.getRevenueLast7Days();
            Map<String, Object> alertsByType = dashboardDao.getAlertsByType();

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("revenue7", revenue7);
            payload.put("alertsByType", alertsByType);

            resp.getWriter().write(gson.toJson(payload));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve dashboard chart metrics", e);
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            Map<String, String> err = Collections.singletonMap("error", "Database unavailable");
            resp.getWriter().write(gson.toJson(err));
        }
    }

    // =========================================================================
    // REVENUE REPORTS
    // =========================================================================

    private void showRevenueReport(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        LocalDate today = LocalDate.now();
        LocalDate defaultFrom = today.minusDays(30);
        LocalDate defaultTo = today;

        String modeParam = req.getParameter("mode");
        String fromParam = req.getParameter("from");
        String toParam = req.getParameter("to");

        String mode = "day";
        if ("month".equalsIgnoreCase(modeParam)) {
            mode = "month";
        } else if ("year".equalsIgnoreCase(modeParam)) {
            mode = "year";
        }

        Map<String, String> errors = new HashMap<>();
        LocalDate fromDate = defaultFrom;
        LocalDate toDate = defaultTo;

        if (fromParam != null && !fromParam.trim().isEmpty()) {
            try {
                fromDate = LocalDate.parse(fromParam.trim());
            } catch (DateTimeParseException e) {
                errors.put("from", "Invalid 'from' date format (expected YYYY-MM-DD).");
                fromDate = defaultFrom;
            }
        }

        if (toParam != null && !toParam.trim().isEmpty()) {
            try {
                toDate = LocalDate.parse(toParam.trim());
            } catch (DateTimeParseException e) {
                errors.put("to", "Invalid 'to' date format (expected YYYY-MM-DD).");
                toDate = defaultTo;
            }
        }

        if (fromDate.isAfter(toDate)) {
            errors.put("from", "'From' date must be before or equal to 'to' date.");
            fromDate = defaultFrom;
            toDate = defaultTo;
        }

        List<Map<String, Object>> buckets = Collections.emptyList();
        long totalRevenue = 0L;
        int totalBookings = 0;
        Map<String, Object> kpis = Collections.emptyMap();
        List<Map<String, Object>> revenueByRoom = Collections.emptyList();
        List<Map<String, Object>> revenueBySlot = Collections.emptyList();

        try {
            buckets = dashboardDao.findRevenueBuckets(fromDate, toDate, mode);
            for (Map<String, Object> b : buckets) {
                totalRevenue += ((Number) b.get("revenue")).longValue();
                totalBookings += ((Number) b.get("bookingCount")).intValue();
            }
            kpis = dashboardDao.getRevenueKpis(fromDate, toDate);
            revenueByRoom = dashboardDao.getRevenueByRoom(fromDate, toDate);
            revenueBySlot = dashboardDao.getRevenueBySlot(fromDate, toDate);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load revenue aggregation", e);
            req.setAttribute("dbError", "Financial report data could not be loaded from database.");
        }

        req.setAttribute("buckets", buckets);
        req.setAttribute("totalRevenue", totalRevenue);
        req.setAttribute("totalBookings", totalBookings);
        req.setAttribute("kpis", kpis);
        req.setAttribute("revenueByRoom", revenueByRoom);
        req.setAttribute("revenueBySlot", revenueBySlot);
        req.setAttribute("mode", mode);
        req.setAttribute("from", fromDate.toString());
        req.setAttribute("to", toDate.toString());
        req.setAttribute("errors", errors);

        req.getRequestDispatcher("/WEB-INF/views/admin/revenue.jsp").forward(req, resp);
    }

    private void serveRevenueData(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        LocalDate today = LocalDate.now();
        LocalDate defaultFrom = today.minusDays(30);
        LocalDate defaultTo = today;

        String modeParam = req.getParameter("mode");
        String fromParam = req.getParameter("from");
        String toParam = req.getParameter("to");

        String mode = "day";
        if ("month".equalsIgnoreCase(modeParam)) {
            mode = "month";
        } else if ("year".equalsIgnoreCase(modeParam)) {
            mode = "year";
        }

        LocalDate fromDate = defaultFrom;
        LocalDate toDate = defaultTo;

        if (fromParam != null && !fromParam.trim().isEmpty()) {
            try {
                fromDate = LocalDate.parse(fromParam.trim());
            } catch (DateTimeParseException ignored) {
            }
        }

        if (toParam != null && !toParam.trim().isEmpty()) {
            try {
                toDate = LocalDate.parse(toParam.trim());
            } catch (DateTimeParseException ignored) {
            }
        }

        if (fromDate.isAfter(toDate)) {
            fromDate = defaultFrom;
            toDate = defaultTo;
        }

        try {
            List<Map<String, Object>> buckets = dashboardDao.findRevenueBuckets(fromDate, toDate, mode);
            List<String> labels = new ArrayList<>();
            List<Long> values = new ArrayList<>();
            List<Integer> bookingCounts = new ArrayList<>();

            long totalRevenue = 0L;
            for (Map<String, Object> b : buckets) {
                labels.add((String) b.get("period"));
                long rev = ((Number) b.get("revenue")).longValue();
                values.add(rev);
                totalRevenue += rev;
                bookingCounts.add(((Number) b.get("bookingCount")).intValue());
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("labels", labels);
            payload.put("values", values);
            payload.put("bookingCounts", bookingCounts);
            payload.put("totalRevenue", totalRevenue);

            resp.getWriter().write(gson.toJson(payload));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve revenue chart JSON telemetry", e);
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            Map<String, String> err = Collections.singletonMap("error", "Database unavailable");
            resp.getWriter().write(gson.toJson(err));
        }
    }
}
