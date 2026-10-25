package servlet;

import dao.DeviceDao;
import dao.RoomDao;
import model.AlertThreshold;
import model.User.AuthUser;
import model.Device;
import util.Flash;
import util.PageResult.PageRequest;
import util.PageResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
 * Administrative controller consolidating device management and alert threshold configuration.
 */
@WebServlet(name = "DeviceAdminServlet", urlPatterns = {
    "/admin/devices",
    "/admin/devices/edit",
    "/admin/devices/delete",
    "/admin/devices/api-key",
    "/admin/thresholds",
    "/admin/thresholds/edit",
    "/admin/thresholds/delete"
})
public class DeviceAdminServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DeviceAdminServlet.class.getName());
    private static final String[] SORT_KEYS = {"id", "name", "type", "status", "seen"};
    private static final String[] THRESHOLD_SORT_KEYS = {"metric", "value", "type", "severity", "updated"};
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Set<String> ALLOWED_METRICS = new HashSet<>(
            Arrays.asList("DISTANCE_CM", "MOTION", "DOOR_OPEN_SECONDS", "PIR_TRIGGER"));
    private static final Set<String> ALLOWED_OPERATORS = new HashSet<>(
            Arrays.asList("GT", "GTE", "LT", "LTE"));
    private static final Set<String> ALLOWED_SEVERITIES = new HashSet<>(
            Arrays.asList("LOW", "MEDIUM", "HIGH", "CRITICAL"));
    private static final Set<String> ALLOWED_ALERT_TYPES = new HashSet<>(
            Arrays.asList(
                    "PIN_BRUTE_FORCE", "DOOR_OPEN_ABNORMAL", "DOOR_OPEN_TOO_LONG",
                    "LOBBY_PROXIMITY", "SUSPICIOUS_MOTION", "CLEANING_SESSION_EXPIRED",
                    "PAYMENT_REVIEW_REQUIRED", "BOOKING_NO_SHOW", "DEVICE_OFFLINE",
                    "DEVICE_FAULT"
            ));

    private final DeviceDao deviceDao = new DeviceDao();
    private final RoomDao roomDao = new RoomDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/devices".equals(path)) {
            listDevices(req, resp);
            return;
        }
        if ("/admin/devices/edit".equals(path)) {
            showEditForm(req, resp);
            return;
        }
        if ("/admin/devices/delete".equals(path)) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "POST is required for device deletion.");
            return;
        }
        if ("/admin/devices/api-key".equals(path)) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "POST is required for API key regeneration.");
            return;
        }
        if ("/admin/thresholds".equals(path)) {
            listThresholds(req, resp);
            return;
        }
        if ("/admin/thresholds/edit".equals(path)) {
            showThresholdEditForm(req, resp);
            return;
        }
        if ("/admin/thresholds/delete".equals(path)) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "POST is required for threshold deletion.");
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/devices/edit".equals(path)) {
            saveDevice(req, resp);
            return;
        }
        if ("/admin/devices/delete".equals(path)) {
            deleteDevice(req, resp);
            return;
        }
        if ("/admin/devices/api-key".equals(path)) {
            regenerateApiKey(req, resp);
            return;
        }
        if ("/admin/thresholds/edit".equals(path)) {
            saveThreshold(req, resp);
            return;
        }
        if ("/admin/thresholds/delete".equals(path)) {
            deleteThreshold(req, resp);
            return;
        }
        if ("/admin/devices".equals(path) || "/admin/thresholds".equals(path)) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void listDevices(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        PageRequest pr = PageRequest.from(req, SORT_KEYS, "id", "ASC");
        Map<String, String> errors = new HashMap<>();

        String type = pr.getFilter("type");
        if (!type.isEmpty() && !"SMART_LOCK".equals(type) && !"SECURITY_CAMERA".equals(type)) {
            errors.put("type", "Invalid device type filter.");
            pr.setFilter("type", null);
        }

        String status = pr.getFilter("status");
        if (!status.isEmpty() && !"ONLINE".equals(status) && !"OFFLINE".equals(status) && !"FAULT".equals(status)) {
            errors.put("status", "Invalid status filter.");
            pr.setFilter("status", null);
        }

        String active = pr.getFilter("active");
        if (!active.isEmpty() && !"1".equals(active) && !"0".equals(active)) {
            errors.put("active", "Invalid active filter.");
            pr.setFilter("active", null);
        }

        PageResult<Device> result;
        try {
            result = deviceDao.findPage(pr);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load device list from database", e);
            result = new PageResult<>(Collections.<Device>emptyList(), 0, pr);
            req.setAttribute("dbError", "Failed to load devices from database.");
        }

        HttpSession session = req.getSession(false);
        if (session != null) {
            Object newKey = session.getAttribute("newApiKeyRevealed");
            Object newDevId = session.getAttribute("newApiKeyDeviceId");
            if (newKey != null) {
                req.setAttribute("newApiKeyRevealed", newKey);
                req.setAttribute("newApiKeyDeviceId", newDevId);
                session.removeAttribute("newApiKeyRevealed");
                session.removeAttribute("newApiKeyDeviceId");
            }
        }

        req.setAttribute("result", result);
        req.setAttribute("pr", pr);
        req.setAttribute("errors", errors);
        req.getRequestDispatcher("/WEB-INF/views/admin/devices.jsp").forward(req, resp);
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String deviceId = req.getParameter("deviceId");
        Device device;
        boolean isEdit = false;

        if (deviceId != null && !deviceId.trim().isEmpty()) {
            try {
                device = deviceDao.findById(deviceId.trim());
                if (device == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Device not found.");
                    return;
                }
                isEdit = true;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load device " + deviceId, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error loading device.");
                return;
            }
        } else {
            device = new Device();
            device.setStatus("OFFLINE");
            device.setActive(true);
            device.setDeviceType("SMART_LOCK");
            device.setZoneId("MAIN_ENTRANCE");
        }

        try {
            req.setAttribute("rooms", roomDao.findAllActive());
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load rooms for device form", e);
        }

        HttpSession session = req.getSession(false);
        if (session != null) {
            Object newKey = session.getAttribute("newApiKeyRevealed");
            if (newKey != null) {
                req.setAttribute("newApiKeyRevealed", newKey);
                session.removeAttribute("newApiKeyRevealed");
                session.removeAttribute("newApiKeyDeviceId");
            }
        }

        req.setAttribute("device", device);
        req.setAttribute("isEdit", isEdit);
        req.getRequestDispatcher("/WEB-INF/views/admin/device-form.jsp").forward(req, resp);
    }

    private void saveDevice(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        boolean isEdit = "true".equalsIgnoreCase(req.getParameter("isEdit"));
        String deviceId = req.getParameter("deviceId");
        String deviceName = req.getParameter("deviceName");
        String deviceType = req.getParameter("deviceType");
        String zoneId = req.getParameter("zoneId");
        String roomIdStr = req.getParameter("roomId");
        String status = req.getParameter("status");
        boolean active = "on".equalsIgnoreCase(req.getParameter("active"))
                || "true".equalsIgnoreCase(req.getParameter("active"))
                || "1".equals(req.getParameter("active"));

        Map<String, String> errors = new HashMap<>();

        if (deviceId == null || deviceId.trim().isEmpty()) {
            errors.put("deviceId", "Device ID is required.");
        } else {
            deviceId = deviceId.trim();
            if (!deviceId.matches("^[A-Z0-9_]{3,50}$")) {
                errors.put("deviceId", "Device ID must be 3-50 uppercase alphanumeric characters or underscores.");
            } else if (!isEdit) {
                try {
                    if (deviceDao.findById(deviceId) != null) {
                        errors.put("deviceId", "This device ID already exists.");
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error checking device ID uniqueness", e);
                }
            }
        }

        if (deviceName == null || deviceName.trim().isEmpty()) {
            errors.put("deviceName", "Device name is required.");
        } else if (deviceName.trim().length() > 100) {
            errors.put("deviceName", "Device name must not exceed 100 characters.");
        }

        if (deviceType == null || (!"SMART_LOCK".equals(deviceType) && !"SECURITY_CAMERA".equals(deviceType))) {
            errors.put("deviceType", "Invalid device type selected.");
        }

        if (zoneId == null || (!"ROOM_DOOR".equals(zoneId) && !"MAIN_ENTRANCE".equals(zoneId) && !"LOBBY".equals(zoneId))) {
            errors.put("zoneId", "Invalid location zone selected.");
        }

        Long roomId = null;
        if (roomIdStr != null && !roomIdStr.trim().isEmpty()) {
            try {
                long parsedRoomId = Long.parseLong(roomIdStr.trim());
                if (roomDao.findById(parsedRoomId) == null) {
                    errors.put("roomId", "Room not found.");
                } else {
                    roomId = parsedRoomId;
                }
            } catch (NumberFormatException e) {
                errors.put("roomId", "Invalid room ID.");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error checking room existence", e);
            }
        }

        if (status == null || (!"ONLINE".equals(status) && !"OFFLINE".equals(status) && !"FAULT".equals(status))) {
            errors.put("status", "Invalid operational status selected.");
        }

        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setDeviceName(deviceName != null ? deviceName.trim() : "");
        device.setDeviceType(deviceType);
        device.setZoneId(zoneId);
        device.setRoomId(roomId);
        device.setStatus(status);
        device.setActive(active);

        if (!errors.isEmpty()) {
            try {
                req.setAttribute("rooms", roomDao.findAllActive());
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to load rooms for device form error reload", e);
            }
            req.setAttribute("device", device);
            req.setAttribute("isEdit", isEdit);
            req.setAttribute("errors", errors);
            req.getRequestDispatcher("/WEB-INF/views/admin/device-form.jsp").forward(req, resp);
            return;
        }

        try {
            if (isEdit) {
                deviceDao.update(device);
                LOGGER.info("Device updated: " + device.getDeviceId() + " - " + device.getDeviceName());
                Flash.success(req, "Device updated successfully.");
            } else {
                deviceDao.insert(device);
                LOGGER.info("Device created: " + device.getDeviceId() + " - " + device.getDeviceName());
                Flash.success(req, "Device created successfully.");
            }
            resp.sendRedirect(req.getContextPath() + "/admin/devices");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save device " + deviceId, e);
            Flash.danger(req, "Database error occurred while saving device.");
            try {
                req.setAttribute("rooms", roomDao.findAllActive());
            } catch (SQLException ignored) {
            }
            req.setAttribute("device", device);
            req.setAttribute("isEdit", isEdit);
            req.getRequestDispatcher("/WEB-INF/views/admin/device-form.jsp").forward(req, resp);
        }
    }

    private void deleteDevice(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String deviceId = req.getParameter("deviceId");
        if (deviceId == null || deviceId.trim().isEmpty()) {
            Flash.danger(req, "Device ID is required.");
            resp.sendRedirect(req.getContextPath() + "/admin/devices");
            return;
        }

        deviceId = deviceId.trim();
        String confirmDeviceId = req.getParameter("confirmDeviceId");
        if (confirmDeviceId != null && !confirmDeviceId.trim().isEmpty() && !confirmDeviceId.trim().equalsIgnoreCase(deviceId)) {
            Flash.danger(req, "Confirmation mismatch: typed device ID does not match.");
            resp.sendRedirect(req.getContextPath() + "/admin/devices");
            return;
        }

        try {
            deviceDao.delete(deviceId);
            LOGGER.info("Device deleted: " + deviceId);
            Flash.success(req, "Device " + deviceId + " deleted successfully.");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to delete device " + deviceId + " (error code: " + e.getErrorCode() + ")", e);
            if (e.getErrorCode() == 547) {
                Flash.danger(req, "This device has related records (credentials, logs or readings) and cannot be deleted. Deactivate it instead.");
            } else {
                Flash.danger(req, "Database error occurred while deleting device.");
            }
        }

        resp.sendRedirect(req.getContextPath() + "/admin/devices");
    }

    private void regenerateApiKey(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String deviceId = req.getParameter("deviceId");
        if (deviceId == null || deviceId.trim().isEmpty()) {
            Flash.danger(req, "Device ID is required.");
            resp.sendRedirect(req.getContextPath() + "/admin/devices");
            return;
        }

        deviceId = deviceId.trim();

        try {
            Device device = deviceDao.findById(deviceId);
            if (device == null) {
                Flash.danger(req, "Device not found.");
                resp.sendRedirect(req.getContextPath() + "/admin/devices");
                return;
            }

            // Generate 32 alphanumeric random characters
            StringBuilder sb = new StringBuilder(32);
            for (int i = 0; i < 32; i++) {
                sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
            }
            String rawKey = sb.toString();

            // Compute SHA-256 hex digest
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexSb = new StringBuilder(64);
            for (byte b : digest) {
                hexSb.append(String.format("%02x", b));
            }
            String sha256Hex = hexSb.toString();

            deviceDao.updateApiKeyHash(deviceId, sha256Hex);

            LOGGER.info("API key regenerated for device: " + deviceId);

            String accept = req.getHeader("Accept");
            String requestedWith = req.getHeader("X-Requested-With");
            boolean isJson = (accept != null && accept.contains("application/json"))
                    || "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                    || "1".equals(req.getParameter("ajax"));

            if (isJson) {
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write("{\"success\":true,\"deviceId\":\"" + util.ElFunctions.esc(deviceId)
                        + "\",\"rawKey\":\"" + rawKey + "\"}");
                return;
            }

            req.getSession().setAttribute("newApiKeyRevealed", rawKey);
            req.getSession().setAttribute("newApiKeyDeviceId", deviceId);
            Flash.warning(req, "New API key for " + deviceId + ": " + rawKey
                    + ". Copy it now - it will never be shown again.");

            String returnUrl = req.getParameter("returnUrl");
            if (returnUrl != null && !returnUrl.trim().isEmpty() && returnUrl.startsWith("/")) {
                resp.sendRedirect(returnUrl);
            } else {
                resp.sendRedirect(req.getContextPath() + "/admin/devices");
            }
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "SHA-256 algorithm unavailable", e);
            Flash.danger(req, "Cryptographic algorithm error.");
            resp.sendRedirect(req.getContextPath() + "/admin/devices");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update API key for device " + deviceId, e);
            Flash.danger(req, "Database error updating API key.");
            resp.sendRedirect(req.getContextPath() + "/admin/devices");
        }
    }

    // =========================================================================
    // ALERT THRESHOLDS
    // =========================================================================

    private void listThresholds(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        PageRequest pr = PageRequest.from(req, THRESHOLD_SORT_KEYS, "updated", "DESC");
        Map<String, String> errors = new HashMap<>();

        String metric = pr.getFilter("metric");
        if (!metric.isEmpty() && !"DISTANCE_CM".equals(metric)
                && !"PIR_TRIGGER".equals(metric) && !"DOOR_OPEN_SECONDS".equals(metric)) {
            errors.put("metric", "Invalid metric type filter.");
            pr.setFilter("metric", null);
        }

        String enabled = pr.getFilter("enabled");
        if (!enabled.isEmpty() && !"1".equals(enabled) && !"0".equals(enabled)) {
            errors.put("enabled", "Invalid enabled filter.");
            pr.setFilter("enabled", null);
        }

        String device = pr.getFilter("device");
        if (!device.isEmpty() && !"__GLOBAL__".equalsIgnoreCase(device)) {
            try {
                if (deviceDao.findById(device) == null) {
                    errors.put("device", "Device not found.");
                    pr.setFilter("device", null);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error validating filter device " + device, e);
            }
        }

        PageResult<AlertThreshold> result;
        List<Device> devices;
        try {
            result = deviceDao.findThresholdPage(pr);
            devices = deviceDao.findAll();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load thresholds or devices", e);
            result = new PageResult<>(Collections.<AlertThreshold>emptyList(), 0, pr);
            devices = Collections.emptyList();
            req.setAttribute("dbError", "Failed to load alert thresholds from database.");
        }

        req.setAttribute("result", result);
        req.setAttribute("devices", devices);
        req.setAttribute("pr", pr);
        req.setAttribute("errors", errors);
        req.getRequestDispatcher("/WEB-INF/views/admin/thresholds.jsp").forward(req, resp);
    }

    private void showThresholdEditForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String idStr = req.getParameter("thresholdId");
        if (idStr == null || idStr.trim().isEmpty()) {
            idStr = req.getParameter("id");
        }

        AlertThreshold threshold;
        if (idStr != null && !idStr.trim().isEmpty()) {
            try {
                long thresholdId = Long.parseLong(idStr.trim());
                threshold = deviceDao.findThresholdById(thresholdId);
                if (threshold == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Threshold not found.");
                    return;
                }
            } catch (NumberFormatException e) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid threshold ID.");
                return;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to load threshold " + idStr, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error loading threshold.");
                return;
            }
        } else {
            threshold = new AlertThreshold();
            threshold.setEnabled(true);
            threshold.setMetricType("DISTANCE_CM");
            threshold.setOperator("LT");
            threshold.setThresholdValue(BigDecimal.valueOf(50));
            threshold.setAlertType("LOBBY_PROXIMITY");
            threshold.setSeverity("LOW");
        }

        try {
            req.setAttribute("devices", deviceDao.findAll());
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load devices for threshold form", e);
        }

        req.setAttribute("threshold", threshold);
        req.getRequestDispatcher("/WEB-INF/views/admin/threshold-form.jsp").forward(req, resp);
    }

    private void saveThreshold(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        Long userId = (authUser != null) ? authUser.getUserId() : null;

        String idStr = req.getParameter("thresholdId");
        String deviceId = req.getParameter("deviceId");
        String metricType = req.getParameter("metricType");
        String operator = req.getParameter("operator");
        String valueStr = req.getParameter("thresholdValue");
        String alertType = req.getParameter("alertType");
        String severity = req.getParameter("severity");
        boolean enabled = "on".equalsIgnoreCase(req.getParameter("enabled"))
                || "true".equalsIgnoreCase(req.getParameter("enabled"))
                || "1".equals(req.getParameter("enabled"));

        Map<String, String> errors = new HashMap<>();

        Long thresholdId = null;
        if (idStr != null && !idStr.trim().isEmpty()) {
            try {
                thresholdId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException e) {
                errors.put("thresholdId", "Invalid threshold ID.");
            }
        }

        if (deviceId != null && deviceId.trim().isEmpty()) {
            deviceId = null;
        } else if (deviceId != null) {
            deviceId = deviceId.trim();
            try {
                if (deviceDao.findById(deviceId) == null) {
                    errors.put("deviceId", "Device not found.");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error verifying device existence", e);
            }
        }

        if (metricType == null || !ALLOWED_METRICS.contains(metricType.trim())) {
            errors.put("metricType", "Invalid metric type selected.");
        }

        if (operator == null || !ALLOWED_OPERATORS.contains(operator.trim())) {
            errors.put("operator", "Invalid operator selected.");
        }

        BigDecimal thresholdValue = null;
        if (valueStr == null || valueStr.trim().isEmpty()) {
            errors.put("thresholdValue", "Threshold value is required.");
        } else {
            try {
                thresholdValue = new BigDecimal(valueStr.trim());
                if (thresholdValue.compareTo(BigDecimal.ZERO) < 0) {
                    errors.put("thresholdValue", "Threshold value must be non-negative.");
                }
            } catch (NumberFormatException e) {
                errors.put("thresholdValue", "Please enter a valid numeric value.");
            }
        }

        if (alertType == null || !ALLOWED_ALERT_TYPES.contains(alertType.trim())) {
            errors.put("alertType", "Invalid alert type selected.");
        }

        if (severity == null || !ALLOWED_SEVERITIES.contains(severity.trim())) {
            errors.put("severity", "Invalid alert severity selected.");
        }

        AlertThreshold t = new AlertThreshold();
        if (thresholdId != null) {
            t.setThresholdId(thresholdId);
        }
        t.setDeviceId(deviceId);
        t.setMetricType(metricType);
        t.setOperator(operator);
        t.setThresholdValue(thresholdValue);
        t.setAlertType(alertType);
        t.setSeverity(severity);
        t.setEnabled(enabled);

        if (!errors.isEmpty()) {
            try {
                req.setAttribute("devices", deviceDao.findAll());
            } catch (SQLException ignored) {
            }
            req.setAttribute("threshold", t);
            req.setAttribute("errors", errors);
            req.getRequestDispatcher("/WEB-INF/views/admin/threshold-form.jsp").forward(req, resp);
            return;
        }

        try {
            if (thresholdId != null) {
                deviceDao.updateThreshold(t);
                LOGGER.info("Threshold updated for " + t.getAlertType() + " id=" + thresholdId);
                Flash.success(req, "Threshold updated successfully.");
            } else {
                t.setUpdatedBy(userId);
                long newId = deviceDao.insertThreshold(t);
                LOGGER.info("Threshold created for " + t.getAlertType() + " id=" + newId);
                Flash.success(req, "Threshold created successfully.");
            }
            resp.sendRedirect(req.getContextPath() + "/admin/thresholds");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save threshold", e);
            Flash.danger(req, "Database error occurred while saving threshold.");
            try {
                req.setAttribute("devices", deviceDao.findAll());
            } catch (SQLException ignored) {
            }
            req.setAttribute("threshold", t);
            req.getRequestDispatcher("/WEB-INF/views/admin/threshold-form.jsp").forward(req, resp);
        }
    }

    private void deleteThreshold(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String idStr = req.getParameter("thresholdId");
        if (idStr == null || idStr.trim().isEmpty()) {
            Flash.danger(req, "Threshold ID is required.");
            resp.sendRedirect(req.getContextPath() + "/admin/thresholds");
            return;
        }

        try {
            long thresholdId = Long.parseLong(idStr.trim());
            deviceDao.deleteThreshold(thresholdId);
            LOGGER.info("Threshold deleted: " + thresholdId);
            Flash.success(req, "Threshold deleted successfully.");
        } catch (NumberFormatException e) {
            Flash.danger(req, "Invalid threshold ID format.");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to delete threshold " + idStr + " (code: " + e.getErrorCode() + ")", e);
            if (e.getErrorCode() == 547) {
                Flash.danger(req, "This threshold is referenced by alert history and cannot be deleted. Disable it instead.");
            } else {
                Flash.danger(req, "Database error occurred while deleting threshold.");
            }
        }

        resp.sendRedirect(req.getContextPath() + "/admin/thresholds");
    }
}
