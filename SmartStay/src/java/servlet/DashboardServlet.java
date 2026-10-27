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
    "/admin/reports/revenue/export",
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
        if ("/admin/reports/revenue/export".equals(path)) {
            exportRevenueCsv(req, resp);
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

    /** Max rows in the "Needs attention" card. */
    static final int ATTENTION_LIMIT = 5;

    /**
     * Dashboard v2 (Task R6): "what do I need to do today?". KPIs, needs-attention list (max 5),
     * room tiles, devices / revenue strips; the existing revenue chart moves below the fold.
     */
    private void showDashboard(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        java.time.LocalDateTime now = util.ClockService.now();
        LocalDate today = now.toLocalDate();

        int[] checkins = new int[]{0, 0};
        int[] checkouts = new int[]{0, 0};
        int roomsToClean = 0;
        Map<String, Integer> newAlerts = new HashMap<>();
        Map<String, Integer> deviceCounts = new HashMap<>();
        long[] revenue = new long[]{0L, 0L};
        List<model.AttentionItem> attention = Collections.emptyList();
        List<model.RoomTile> rooms = Collections.emptyList();

        try {
            checkins = dashboardDao.getCheckinsToday(today);
            checkouts = dashboardDao.getCheckoutsToday(today);
            roomsToClean = dashboardDao.getPendingCleaningCount();
            newAlerts = dashboardDao.getNewAlertsBySeverity();
            deviceCounts = dashboardDao.getDeviceStatusCounts();
            revenue = dashboardDao.getRevenueMonthToDate(today);
            attention = dashboardDao.getAttentionItems(now);
            rooms = dashboardDao.getRoomTiles(now);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load dashboard operational statistics", e);
            req.setAttribute("dbError", "Dashboard figures could not be loaded. Please reload the page.");
        }

        req.setAttribute("today", today);
        req.setAttribute("todayLabel", today.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", java.util.Locale.ENGLISH)));
        req.setAttribute("checkinsDone", checkins[0]);
        req.setAttribute("checkinsExpected", checkins[1]);
        req.setAttribute("checkoutsDone", checkouts[0]);
        req.setAttribute("checkoutsExpected", checkouts[1]);
        req.setAttribute("roomsToClean", roomsToClean);
        req.setAttribute("newAlerts", newAlerts);
        req.setAttribute("deviceCounts", deviceCounts);
        req.setAttribute("revenueMonth", revenue[0]);
        req.setAttribute("revenueChangePct", changePercent(revenue[0], revenue[1]));
        req.setAttribute("attentionItems", attention.size() > ATTENTION_LIMIT ? attention.subList(0, ATTENTION_LIMIT) : attention);
        req.setAttribute("attentionTotal", attention.size());
        req.setAttribute("roomTiles", rooms);

        req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(req, resp);
    }

    /** Rounded % change vs the previous period, or null when there is nothing to compare with. */
    public static Integer changePercent(long current, long previous) {
        if (previous <= 0) {
            return null;
        }
        return (int) Math.round((current - previous) * 100.0 / previous);
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
        LocalDate today = util.ClockService.now().toLocalDate();
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

        // R11 quick range chips (7d / 30d / 90d / This month) win over typed dates
        LocalDate[] quick = util.RevenueReport.quickRange(req.getParameter("range"), today);
        if (quick != null) {
            fromDate = quick[0];
            toDate = quick[1];
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

        // R11: dd/MM/yyyy labels, tables sorted by revenue, change chip from one tested helper
        for (Map<String, Object> b : buckets) {
            b.put("label", util.RevenueReport.periodLabel((String) b.get("period")));
            b.put("avg", util.RevenueReport.avgPerBooking(((Number) b.get("revenue")).longValue(),
                    ((Number) b.get("bookingCount")).intValue()));
        }
        revenueByRoom = util.RevenueReport.sortByRevenueDesc(revenueByRoom);
        revenueBySlot = util.RevenueReport.sortByRevenueDesc(revenueBySlot);
        long prevRevenue = (kpis.get("prevRevenue") instanceof Number) ? ((Number) kpis.get("prevRevenue")).longValue() : 0L;
        Double changePct = util.RevenueReport.changePercent(totalRevenue, prevRevenue);
        req.setAttribute("changePct", changePct);
        req.setAttribute("changeDir", util.RevenueReport.direction(changePct));
        req.setAttribute("activeRange", util.RevenueReport.activeQuickRange(fromDate, toDate, today));
        req.setAttribute("totalAvg", util.RevenueReport.avgPerBooking(totalRevenue, totalBookings));

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

    /**
     * R11: server-side CSV of the detailed table with the same filters (mode, from, to, hideZero),
     * UTF-8 with BOM so Excel shows it correctly. Same permission rule as the report (/admin/reports).
     */
    private void exportRevenueCsv(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        LocalDate today = util.ClockService.now().toLocalDate();
        LocalDate fromDate = today.minusDays(30);
        LocalDate toDate = today;
        String mode = "month".equalsIgnoreCase(req.getParameter("mode")) ? "month"
                : ("year".equalsIgnoreCase(req.getParameter("mode")) ? "year" : "day");
        try {
            if (req.getParameter("from") != null && !req.getParameter("from").trim().isEmpty()) {
                fromDate = LocalDate.parse(req.getParameter("from").trim());
            }
            if (req.getParameter("to") != null && !req.getParameter("to").trim().isEmpty()) {
                toDate = LocalDate.parse(req.getParameter("to").trim());
            }
        } catch (DateTimeParseException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid date (expected YYYY-MM-DD).");
            return;
        }
        if (fromDate.isAfter(toDate)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'From' date must be before or equal to 'to' date.");
            return;
        }
        boolean hideZero = "1".equals(req.getParameter("hideZero"));
        try {
            List<Map<String, Object>> buckets = dashboardDao.findRevenueBuckets(fromDate, toDate, mode);
            byte[] body = util.RevenueReport.buildCsv(buckets, hideZero).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            resp.setContentType("text/csv; charset=UTF-8");
            resp.setHeader("Cache-Control", "no-store");
            resp.setHeader("Content-Disposition", "attachment; filename=\"smartstay-revenue_" + fromDate + "_" + toDate + ".csv\"");
            resp.setContentLength(body.length);
            resp.getOutputStream().write(body);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to export revenue CSV", e);
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Revenue data could not be loaded.");
        }
    }

    private void serveRevenueData(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        LocalDate today = util.ClockService.now().toLocalDate();
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
