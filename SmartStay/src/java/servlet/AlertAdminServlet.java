package servlet;

import dao.AlertLogDao;
import model.AlertEntry;
import model.User.AuthUser;
import util.Flash;
import util.PageResult.PageRequest;
import util.PageResult;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
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
 * Administrative controller consolidating alert log listing and state transition handling.
 */
@WebServlet(name = "AlertAdminServlet", urlPatterns = {"/admin/alerts", "/admin/alerts/action"})
public class AlertAdminServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AlertAdminServlet.class.getName());
    private static final String[] SORT_KEYS = {"time", "severity", "status", "type"};

    private final AlertLogDao alertLogDao = new AlertLogDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/alerts".equals(path)) {
            listAlerts(req, resp);
            return;
        }
        if ("/admin/alerts/action".equals(path)) {
            resp.sendRedirect(req.getContextPath() + "/admin/alerts");
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/alerts/action".equals(path)) {
            handleAlertAction(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void listAlerts(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        PageRequest pr = PageRequest.from(req, SORT_KEYS, "time", "DESC");
        pr.setSize(15);
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
            } else if (ChronoUnit.DAYS.between(fromDate, toDate) > 90) {
                errors.put("range", "Date range cannot exceed 90 days.");
                pr.setFilter("from", null);
            }
        }

        String severity = pr.getFilter("severity");
        if (!severity.isEmpty() && !"LOW".equals(severity) && !"MEDIUM".equals(severity)
                && !"HIGH".equals(severity) && !"CRITICAL".equals(severity)) {
            errors.put("severity", "Invalid severity filter.");
            pr.setFilter("severity", null);
        }

        String status = pr.getFilter("status");
        if (!status.isEmpty() && !"NEW".equals(status) && !"ACKNOWLEDGED".equals(status)
                && !"RESOLVED".equals(status)) {
            errors.put("status", "Invalid status filter.");
            pr.setFilter("status", null);
        }

        PageResult<AlertEntry> result;
        try {
            result = alertLogDao.findPage(pr);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load alert logs from database", e);
            result = new PageResult<>(Collections.<AlertEntry>emptyList(), 0, pr);
            req.setAttribute("dbError", "Failed to load alert logs from database.");
        }

        List<String> types = Collections.emptyList();
        try {
            types = alertLogDao.findDistinctTypes();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load distinct alert types", e);
        }
        req.setAttribute("types", types);
        req.setAttribute("result", result);
        req.setAttribute("pr", pr);
        req.setAttribute("errors", errors);
        req.getRequestDispatcher("/WEB-INF/views/admin/alerts.jsp").forward(req, resp);
    }

    private void handleAlertAction(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        @SuppressWarnings("unchecked")
        Set<String> perms = (session != null) ? (Set<String>) session.getAttribute("permissions") : null;

        String roleName = (session != null) ? (String) session.getAttribute("roleName") : null;
        if (roleName == null && authUser != null) {
            roleName = authUser.getRoleName();
        }
        boolean canHandle = (perms != null && perms.contains("ALERT_HANDLE"))
                || "ADMIN".equalsIgnoreCase(roleName)
                || "OWNER".equalsIgnoreCase(roleName);

        if (authUser == null || !canHandle) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req, resp);
            return;
        }

        String action = req.getParameter("action");
        String idStr = req.getParameter("alertId");
        long alertId;

        try {
            alertId = Long.parseLong(idStr != null ? idStr.trim() : "");
        } catch (NumberFormatException e) {
            Flash.error(req, "Invalid alert ID specified.");
            resp.sendRedirect(req.getContextPath() + "/admin/alerts");
            return;
        }

        AlertEntry alert;
        try {
            alert = alertLogDao.findById(alertId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load alert #" + alertId, e);
            alert = null;
        }

        if (alert == null) {
            Flash.error(req, "Alert #" + alertId + " not found.");
            resp.sendRedirect(req.getContextPath() + "/admin/alerts");
            return;
        }

        long userId = authUser.getUserId();

        if ("acknowledge".equalsIgnoreCase(action)) {
            try {
                int updated = alertLogDao.acknowledge(alertId, userId);
                if (updated > 0) {
                    LOGGER.info("Alert acknowledged: " + alert.getAlertType() + " id=" + alertId);
                    Flash.success(req, "Alert #" + alertId + " has been acknowledged.");
                    Map<String, Object> ev = new HashMap<>();
                    ev.put("alertId", alertId);
                    ev.put("status", "ACKNOWLEDGED");
                    ev.put("alertType", alert.getAlertType());
                    ev.put("newAlertCount", alertLogDao.countNewAlerts());
                    websocket.RealtimeWebSocketEndpoint.broadcastEvent("ALERT_UPDATED", ev);
                } else {
                    Flash.info(req, "This alert cannot be updated.");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to acknowledge alert #" + alertId, e);
                Flash.error(req, "Database error occurred while acknowledging alert.");
            }
        } else if ("resolve".equalsIgnoreCase(action)) {
            String note = req.getParameter("note");
            String trimmedNote = (note != null && !note.trim().isEmpty()) ? note.trim() : "Resolved by " + authUser.getFullName();
            if (trimmedNote.length() > 500) {
                trimmedNote = trimmedNote.substring(0, 500);
            }

            try {
                int updated = alertLogDao.resolve(alertId, userId, trimmedNote);
                if (updated > 0) {
                    LOGGER.info("Alert resolved: " + alert.getAlertType() + " id=" + alertId + " note=" + trimmedNote);
                    Flash.success(req, "Alert #" + alertId + " has been resolved.");
                    Map<String, Object> ev = new HashMap<>();
                    ev.put("alertId", alertId);
                    ev.put("status", "RESOLVED");
                    ev.put("alertType", alert.getAlertType());
                    ev.put("newAlertCount", alertLogDao.countNewAlerts());
                    websocket.RealtimeWebSocketEndpoint.broadcastEvent("ALERT_UPDATED", ev);
                } else {
                    Flash.info(req, "This alert cannot be updated.");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to resolve alert #" + alertId, e);
                Flash.error(req, "Database error occurred while resolving alert.");
            }
        } else {
            Flash.error(req, "Invalid action requested.");
        }

        resp.sendRedirect(req.getContextPath() + "/admin/alerts");
    }
}
