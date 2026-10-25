package servlet;

import dao.DeviceLogDao;
import model.AccessLogEntry;
import model.MotionLogEntry;
import util.PageResult.PageRequest;
import util.PageResult;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
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
 * Administrative controller consolidating hardware access logs and PIR motion sensor logs.
 */
@WebServlet(name = "LogAdminServlet", urlPatterns = {"/admin/motion-logs", "/admin/access-logs"})
public class LogAdminServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LogAdminServlet.class.getName());
    private static final String[] MOTION_SORT_KEYS = {"time", "distance", "device"};
    private static final String[] ACCESS_SORT_KEYS = {"time", "device", "result"};

    private final DeviceLogDao logDao = new DeviceLogDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/motion-logs".equals(path)) {
            showMotionLogs(req, resp);
            return;
        }
        if ("/admin/access-logs".equals(path)) {
            showAccessLogs(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "POST is not supported for log viewing.");
    }

    // =========================================================================
    // MOTION LOGS
    // =========================================================================

    private void showMotionLogs(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        PageRequest pr = PageRequest.from(req, MOTION_SORT_KEYS, "time", "DESC");
        pr.setSize(10);
        Map<String, String> errors = new HashMap<>();

        String fromStr = pr.getFilter("from");
        String toStr = pr.getFilter("to");
        LocalDate fromDate = null;
        LocalDate toDate = null;

        if (!fromStr.isEmpty()) {
            try {
                fromDate = LocalDate.parse(fromStr.trim());
            } catch (DateTimeParseException e) {
                errors.put("from", "Invalid 'from' date format (expected YYYY-MM-DD).");
                pr.setFilter("from", null);
            }
        }

        if (!toStr.isEmpty()) {
            try {
                toDate = LocalDate.parse(toStr.trim());
            } catch (DateTimeParseException e) {
                errors.put("to", "Invalid 'to' date format (expected YYYY-MM-DD).");
                pr.setFilter("to", null);
            }
        }

        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                errors.put("from", "'From' date must be before or equal to 'to' date.");
                pr.setFilter("from", null);
                pr.setFilter("to", null);
            } else if (ChronoUnit.DAYS.between(fromDate, toDate) > 366) {
                errors.put("from", "Date range cannot exceed 366 days.");
                pr.setFilter("from", null);
                pr.setFilter("to", null);
            }
        }

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

        HttpSession session = req.getSession(false);
        boolean hasSnapshot = false;
        if (session != null) {
            Object perms = session.getAttribute("permissions");
            if (perms instanceof Set) {
                hasSnapshot = ((Set<?>) perms).contains("SNAPSHOT_VIEW");
            }
        }
        req.setAttribute("hasSnapshotView", hasSnapshot);
        req.setAttribute("result", result);
        req.setAttribute("devices", devices);
        req.setAttribute("zones", zones);
        req.setAttribute("errors", errors);

        req.getRequestDispatcher("/WEB-INF/views/admin/motion-logs.jsp").forward(req, resp);
    }

    // =========================================================================
    // ACCESS LOGS
    // =========================================================================

    private void showAccessLogs(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        PageRequest pr = PageRequest.from(req, ACCESS_SORT_KEYS, "time", "DESC");
        pr.setSize(10);
        Map<String, String> errors = new HashMap<>();

        String fromStr = pr.getFilter("from");
        String toStr = pr.getFilter("to");
        LocalDate fromDate = null;
        LocalDate toDate = null;

        if (!fromStr.isEmpty()) {
            try {
                fromDate = LocalDate.parse(fromStr.trim());
            } catch (DateTimeParseException e) {
                errors.put("from", "Invalid 'from' date format (expected YYYY-MM-DD).");
                pr.setFilter("from", null);
            }
        }

        if (!toStr.isEmpty()) {
            try {
                toDate = LocalDate.parse(toStr.trim());
            } catch (DateTimeParseException e) {
                errors.put("to", "Invalid 'to' date format (expected YYYY-MM-DD).");
                pr.setFilter("to", null);
            }
        }

        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                errors.put("from", "'From' date must be before or equal to 'to' date.");
                pr.setFilter("from", null);
                pr.setFilter("to", null);
            } else if (ChronoUnit.DAYS.between(fromDate, toDate) > 366) {
                errors.put("from", "Date range cannot exceed 366 days.");
                pr.setFilter("from", null);
                pr.setFilter("to", null);
            }
        }

        PageResult<AccessLogEntry> result;
        List<String> devices;
        List<String> reasons;

        try {
            result = logDao.findAccessPage(pr);
            devices = logDao.findDistinctAccessDevices();
            reasons = logDao.findDistinctAccessReasons();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load access logs", e);
            result = new PageResult<>(Collections.<AccessLogEntry>emptyList(), 0, pr);
            devices = Collections.emptyList();
            reasons = Collections.emptyList();
            req.setAttribute("dbError", "Access logs could not be loaded. Please try again later.");
        }

        req.setAttribute("result", result);
        req.setAttribute("devices", devices);
        req.setAttribute("reasons", reasons);
        req.setAttribute("errors", errors);

        req.getRequestDispatcher("/WEB-INF/views/admin/access-logs.jsp").forward(req, resp);
    }
}
