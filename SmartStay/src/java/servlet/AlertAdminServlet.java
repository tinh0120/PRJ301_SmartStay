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
    private static final String[] SORT_KEYS = {"smart", "time", "severity", "status", "type"};

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
        // R10: inbox tabs Open (NEW, default) | Acknowledged | Resolved | All; most severe first, then newest
        PageRequest pr = PageRequest.from(req, SORT_KEYS, "smart", "DESC");
        pr.setSize(20);
        Map<String, String> errors = new HashMap<>();

        String tab = pr.getFilter("tab").toLowerCase();
        String legacyStatus = pr.getFilter("status").toUpperCase();
        if (tab.isEmpty()) {
            tab = "ACKNOWLEDGED".equals(legacyStatus) ? "ack" : ("RESOLVED".equals(legacyStatus) ? "resolved" : "open");
            if (!legacyStatus.isEmpty() && !"NEW".equals(legacyStatus) && !"ACKNOWLEDGED".equals(legacyStatus)
                    && !"RESOLVED".equals(legacyStatus)) {
                tab = "all";
            }
        }
        if (!"open".equals(tab) && !"ack".equals(tab) && !"resolved".equals(tab) && !"all".equals(tab)) {
            tab = "open";
        }
        pr.setFilter("tab", tab);
        pr.setFilter("status", "open".equals(tab) ? "NEW" : ("ack".equals(tab) ? "ACKNOWLEDGED"
                : ("resolved".equals(tab) ? "RESOLVED" : null)));
        req.setAttribute("tab", tab);
        req.setAttribute("chipBaseQs", pr.toQueryString("page", "size", "severity", "status"));
        req.setAttribute("tabBaseQs", pr.toQueryString("page", "size", "tab", "status"));
        req.setAttribute("returnQs", pr.toQueryString("size", "status"));
        req.setAttribute("moreFiltersOpen", !pr.getFilter("type").isEmpty() || !pr.getFilter("from").isEmpty()
                || !pr.getFilter("to").isEmpty());
        try {
            req.setAttribute("statusCounts", alertLogDao.getStatusCounts());
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to count alerts by status", e);
        }
        req.setAttribute("canHandle", canHandle(req));

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

        // ESP32-CAM photo in the detail drawer: only when the file really exists and SNAPSHOT_VIEW is granted
        HttpSession snapSession = req.getSession(false);
        Object perms = (snapSession != null) ? snapSession.getAttribute("permissions") : null;
        boolean canSnapshot = perms instanceof Set && ((Set<?>) perms).contains("SNAPSHOT_VIEW");
        for (AlertEntry a : result.getItems()) {
            a.setSnapshotAvailable(canSnapshot && util.IotDevices.snapshotExists(getServletContext(), a.getSnapshotPath()));
        }

        List<String> types = Collections.emptyList();
        try {
            types = alertLogDao.findDistinctTypes();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load distinct alert types", e);
        }
        Map<String, String> typeTitles = new java.util.LinkedHashMap<>();
        for (String code : types) {
            typeTitles.put(code, util.AlertTitles.title(code));
        }
        req.setAttribute("types", types);
        req.setAttribute("typeTitles", typeTitles);
        req.setAttribute("result", result);
        req.setAttribute("pr", pr);
        req.setAttribute("errors", errors);
        req.getRequestDispatcher("/WEB-INF/views/admin/alerts.jsp").forward(req, resp);
    }

    /** ALERT_HANDLE permission, or an Owner/Admin account (unchanged rule). */
    private static boolean canHandle(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        @SuppressWarnings("unchecked")
        Set<String> perms = (session != null) ? (Set<String>) session.getAttribute("permissions") : null;
        String roleName = (session != null) ? (String) session.getAttribute("roleName") : null;
        if (roleName == null && authUser != null) {
            roleName = authUser.getRoleName();
        }
        return authUser != null && ((perms != null && perms.contains("ALERT_HANDLE"))
                || "ADMIN".equalsIgnoreCase(roleName)
                || "OWNER".equalsIgnoreCase(roleName));
    }

    private static boolean isAjax(HttpServletRequest req) {
        return "XMLHttpRequest".equalsIgnoreCase(req.getHeader("X-Requested-With"));
    }

    /** JSON answer for in-place updates (R10): {success, message, alertId, status, newAlertCount}. */
    private void writeJson(HttpServletResponse resp, int status, boolean success, String message,
                           long alertId, String alertStatus) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json; charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
        o.addProperty("success", success);
        o.addProperty("message", message);
        o.addProperty("alertId", alertId);
        if (alertStatus != null) {
            o.addProperty("status", alertStatus);
        }
        o.addProperty("newAlertCount", alertLogDao.countNewAlerts());
        resp.getWriter().write(o.toString());
    }

    private void handleAlertAction(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;

        if (authUser == null || !canHandle(req)) {
            if (isAjax(req)) {
                writeJson(resp, HttpServletResponse.SC_FORBIDDEN, false, "You are not allowed to handle alerts.", 0, null);
                return;
            }
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req, resp);
            return;
        }

        String returnUrl = req.getParameter("returnUrl");
        String back = (returnUrl != null && returnUrl.startsWith(req.getContextPath() + "/admin/alerts")
                && returnUrl.indexOf('\r') < 0 && returnUrl.indexOf('\n') < 0)
                ? returnUrl : req.getContextPath() + "/admin/alerts";

        String action = req.getParameter("action");
        String idStr = req.getParameter("alertId");
        long alertId;
        try {
            alertId = Long.parseLong(idStr != null ? idStr.trim() : "");
        } catch (NumberFormatException e) {
            finishAction(req, resp, back, HttpServletResponse.SC_BAD_REQUEST, "error", "Invalid alert ID specified.", 0, null);
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
            finishAction(req, resp, back, HttpServletResponse.SC_NOT_FOUND, "error", "This alert no longer exists.", alertId, null);
            return;
        }

        long userId = authUser.getUserId();
        String title = alert.getTitle();

        if ("acknowledge".equalsIgnoreCase(action)) {
            try {
                int updated = alertLogDao.acknowledge(alertId, userId);
                if (updated > 0) {
                    LOGGER.info("Alert acknowledged: " + alert.getAlertType() + " id=" + alertId);
                    broadcastUpdate(alertId, "ACKNOWLEDGED", alert.getAlertType());
                    finishAction(req, resp, back, HttpServletResponse.SC_OK, "success", "Acknowledged: " + title + ".", alertId, "ACKNOWLEDGED");
                } else {
                    finishAction(req, resp, back, HttpServletResponse.SC_CONFLICT, "info", "Someone already handled this alert.", alertId, alert.getStatus());
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to acknowledge alert #" + alertId, e);
                finishAction(req, resp, back, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error", "Could not save. Please try again.", alertId, null);
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
                    broadcastUpdate(alertId, "RESOLVED", alert.getAlertType());
                    finishAction(req, resp, back, HttpServletResponse.SC_OK, "success", "Resolved: " + title + ".", alertId, "RESOLVED");
                } else {
                    finishAction(req, resp, back, HttpServletResponse.SC_CONFLICT, "info", "Someone already handled this alert.", alertId, alert.getStatus());
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to resolve alert #" + alertId, e);
                finishAction(req, resp, back, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error", "Could not save. Please try again.", alertId, null);
            }
        } else {
            finishAction(req, resp, back, HttpServletResponse.SC_BAD_REQUEST, "error", "Invalid action requested.", alertId, null);
        }
    }

    private void broadcastUpdate(long alertId, String status, String alertType) {
        Map<String, Object> ev = new HashMap<>();
        ev.put("alertId", alertId);
        ev.put("status", status);
        ev.put("alertType", alertType);
        ev.put("title", util.AlertTitles.title(alertType));
        ev.put("newAlertCount", alertLogDao.countNewAlerts());
        websocket.RealtimeWebSocketEndpoint.broadcastEvent("ALERT_UPDATED", ev);
    }

    /** fetch() callers get JSON; a plain form post gets a flash message and a redirect back to the same tab. */
    private void finishAction(HttpServletRequest req, HttpServletResponse resp, String back, int status,
                              String level, String message, long alertId, String alertStatus) throws IOException {
        if (isAjax(req)) {
            writeJson(resp, status, status == HttpServletResponse.SC_OK, message, alertId, alertStatus);
            return;
        }
        if ("success".equals(level)) {
            Flash.success(req, message);
        } else if ("info".equals(level)) {
            Flash.info(req, message);
        } else {
            Flash.error(req, message);
        }
        resp.sendRedirect(back);
    }
}
