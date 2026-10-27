package servlet;

import dao.DeviceLogDao;
import model.AccessLogEntry;
import model.MotionLogEntry;
import util.ActivityLog;
import util.PageResult.PageRequest;
import util.PageResult;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
 * Activity logs (Task R9): ONE page /admin/activity with two tabs, "Door access" (ACCESS_LOG) and
 * "Motion" (MOTION_LOG). Each tab follows its own permission (LOG_ACCESS_VIEW / LOG_MOTION_VIEW).
 * The old /admin/access-logs and /admin/motion-logs URLs redirect to the matching tab.
 * Log times are REAL (hardware) time, so day groups use the real date.
 */
@WebServlet(name = "LogAdminServlet", urlPatterns = {"/admin/activity", "/admin/motion-logs", "/admin/access-logs"})
public class LogAdminServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LogAdminServlet.class.getName());
    private static final String[] MOTION_SORT_KEYS = {"time", "distance", "device"};
    private static final String[] ACCESS_SORT_KEYS = {"time", "device", "result"};
    private static final int PAGE_SIZE = 15; // round 3 item 3: shorter pages (was 25)

    private final DeviceLogDao logDao = new DeviceLogDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/access-logs".equals(path) || "/admin/motion-logs".equals(path)) {
            // Old URLs keep working: same filters, new page
            String tab = "/admin/motion-logs".equals(path) ? "motion" : "door";
            String qs = req.getQueryString();
            resp.sendRedirect(req.getContextPath() + "/admin/activity?tab=" + tab
                    + (qs != null && !qs.isEmpty() ? "&" + qs : ""));
            return;
        }
        if ("/admin/activity".equals(path)) {
            showActivity(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "POST is not supported for log viewing.");
    }

    private void showActivity(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        boolean canDoor = hasPermission(req, "LOG_ACCESS_VIEW");
        boolean canMotion = hasPermission(req, "LOG_MOTION_VIEW");
        if (!canDoor && !canMotion) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String tab = req.getParameter("tab");
        if ("motion".equals(tab) && !canMotion) {
            tab = "door";
        }
        if (!"motion".equals(tab)) {
            tab = canDoor ? "door" : "motion";
        }
        req.setAttribute("tab", tab);
        req.setAttribute("canDoor", canDoor);
        req.setAttribute("canMotion", canMotion);

        if ("door".equals(tab)) {
            loadDoor(req);
        } else {
            loadMotion(req);
        }
        req.getRequestDispatcher("/WEB-INF/views/admin/activity.jsp").forward(req, resp);
    }

    // =========================================================================
    // DOOR ACCESS
    // =========================================================================

    private void loadDoor(HttpServletRequest req) {
        PageRequest pr = PageRequest.from(req, ACCESS_SORT_KEYS, "time", "DESC");
        pr.setFilter("tab", "door");
        pr.setSize(PAGE_SIZE);
        Map<String, String> errors = validateDates(pr);
        boolean deniedOnly = "DENIED".equalsIgnoreCase(pr.getFilter("result"));

        PageResult<AccessLogEntry> result;
        List<String> devices;
        try {
            result = logDao.findAccessPage(pr);
            devices = logDao.findDistinctAccessDevices();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load access logs", e);
            result = new PageResult<>(Collections.<AccessLogEntry>emptyList(), 0, pr);
            devices = Collections.emptyList();
            req.setAttribute("dbError", "Door access logs could not be loaded. Please try again later.");
        }

        List<ActivityLog.Group<AccessLogEntry>> days = ActivityLog.groupByDay(result.getItems(),
                new ActivityLog.TimeFn<AccessLogEntry>() {
                    @Override
                    public LocalDateTime time(AccessLogEntry a) {
                        return a.getAttemptedAt() != null ? a.getAttemptedAt().toLocalDateTime() : null;
                    }
                }, util.ClockService.realNow().toLocalDate());

        req.setAttribute("result", result);
        req.setAttribute("dayGroups", days);
        req.setAttribute("devices", devices);
        req.setAttribute("errors", errors);
        req.setAttribute("pr", pr);
        req.setAttribute("deniedOnly", deniedOnly);
        req.setAttribute("chipBaseQs", pr.toQueryString("page", "size", "result"));
        req.setAttribute("moreFiltersOpen", !pr.getFilter("device").isEmpty() || !pr.getFilter("from").isEmpty()
                || !pr.getFilter("to").isEmpty());
    }

    // =========================================================================
    // MOTION
    // =========================================================================

    private void loadMotion(HttpServletRequest req) {
        PageRequest pr = PageRequest.from(req, MOTION_SORT_KEYS, "time", "DESC");
        pr.setFilter("tab", "motion");
        pr.setSize(PAGE_SIZE);
        Map<String, String> errors = validateDates(pr);

        PageResult<MotionLogEntry> result;
        List<String> devices;
        List<String> zones;
        try {
            result = logDao.findMotionPage(pr);
            devices = logDao.findDistinctMotionDevices();
            zones = logDao.findDistinctMotionZones();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load motion logs", e);
            result = new PageResult<>(Collections.<MotionLogEntry>emptyList(), 0, pr);
            devices = Collections.emptyList();
            zones = Collections.emptyList();
            req.setAttribute("dbError", "Motion logs could not be loaded. Please try again later.");
        }

        // Snapshot thumbnails only when the path is set AND the file really exists (no placeholders)
        boolean canSnapshot = hasPermission(req, "SNAPSHOT_VIEW");
        for (MotionLogEntry m : result.getItems()) {
            m.setSnapshotAvailable(canSnapshot && snapshotExists(req, m.getSnapshotPath()));
        }

        List<ActivityLog.Group<MotionLogEntry>> days = ActivityLog.groupByDay(result.getItems(),
                new ActivityLog.TimeFn<MotionLogEntry>() {
                    @Override
                    public LocalDateTime time(MotionLogEntry m) {
                        return m.getDetectedAt() != null ? m.getDetectedAt().toLocalDateTime() : null;
                    }
                }, util.ClockService.realNow().toLocalDate());

        req.setAttribute("result", result);
        req.setAttribute("dayGroups", days);
        req.setAttribute("devices", devices);
        req.setAttribute("zones", zones);
        req.setAttribute("errors", errors);
        req.setAttribute("pr", pr);
        req.setAttribute("closeOnly", "1".equals(pr.getFilter("close")));
        req.setAttribute("chipBaseQs", pr.toQueryString("page", "size", "close"));
        req.setAttribute("moreFiltersOpen", !pr.getFilter("device").isEmpty() || !pr.getFilter("zone").isEmpty()
                || !pr.getFilter("from").isEmpty() || !pr.getFilter("to").isEmpty());
    }

    /** Stored path (e.g. "assets/images/snapshots/x.jpg") whose file exists in util.SnapshotStore (or the legacy web folder). */
    private boolean snapshotExists(HttpServletRequest req, String path) {
        return util.SnapshotStore.exists(req.getServletContext(), path);
    }

    private Map<String, String> validateDates(PageRequest pr) {
        Map<String, String> errors = new HashMap<>();
        LocalDate fromDate = null;
        LocalDate toDate = null;
        String fromStr = pr.getFilter("from");
        String toStr = pr.getFilter("to");
        if (!fromStr.isEmpty()) {
            try {
                fromDate = LocalDate.parse(fromStr.trim());
            } catch (DateTimeParseException e) {
                errors.put("from", "Enter the start date as dd/MM/yyyy.");
                pr.setFilter("from", null);
            }
        }
        if (!toStr.isEmpty()) {
            try {
                toDate = LocalDate.parse(toStr.trim());
            } catch (DateTimeParseException e) {
                errors.put("to", "Enter the end date as dd/MM/yyyy.");
                pr.setFilter("to", null);
            }
        }
        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                errors.put("from", "The start date must be on or before the end date.");
                pr.setFilter("from", null);
                pr.setFilter("to", null);
            } else if (ChronoUnit.DAYS.between(fromDate, toDate) > 366) {
                errors.put("from", "Choose a range of one year or less.");
                pr.setFilter("from", null);
                pr.setFilter("to", null);
            }
        }
        return errors;
    }

    private static boolean hasPermission(HttpServletRequest req, String perm) {
        HttpSession session = req.getSession(false);
        Object perms = (session != null) ? session.getAttribute("permissions") : null;
        return perms instanceof Set && ((Set<?>) perms).contains(perm);
    }
}
