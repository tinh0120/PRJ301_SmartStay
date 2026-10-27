package servlet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import util.DBUtil;
import util.IotDevices;
import util.IotDevices.DeviceAuth;
import util.IotDevices.EventAlert;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ESP32 endpoints added for the real door lock + ESP32-CAM (Blynk removed):
 * <ul>
 *   <li>POST /api/iot/heartbeat  {deviceId, ack?}  - keeps the device ONLINE; "ack" confirms a remote command</li>
 *   <li>POST /api/iot/events     {deviceId, type, detail?} - LOCKOUT, PRY, DOOR_FORCED, DOOR_OPEN_TOO_LONG,
 *       LOITERING, EXIT_STUCK -> ALERT_LOG (+ realtime ALERT_CREATED)</li>
 *   <li>POST /api/iot/snapshot?deviceId=..&amp;reason=.. (body image/jpeg) - saved in util.SnapshotStore
 *       (served at assets/images/snapshots/..), logged in MOTION_LOG and attached to the latest matching alert</li>
 * </ul>
 * Authentication: header X-Device-Key (see util.IotDevices). All times here are REAL time.
 */
@WebServlet(name = "IotDeviceApiServlet", urlPatterns = {"/api/iot/heartbeat", "/api/iot/events", "/api/iot/snapshot"})
public class IotDeviceApiServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(IotDeviceApiServlet.class.getName());
    private static final int MAX_JSON_BYTES = 4 * 1024;
    private static final int MAX_JPEG_BYTES = 3 * 1024 * 1024;
    /** Same device + same alert type within this window is treated as a repeat and not stored again. */
    private static final int EVENT_DEDUP_SECONDS = 30;
    /** A snapshot is attached to an alert raised at most this long before it arrives. */
    private static final int SNAPSHOT_LINK_SECONDS = 120;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String path = req.getServletPath();
        try {
            if ("/api/iot/snapshot".equals(path)) {
                handleSnapshot(req, resp);
                return;
            }
            JsonObject body = readJson(req);
            if (body == null) {
                send(resp, HttpServletResponse.SC_BAD_REQUEST, error("BAD_REQUEST", "Invalid JSON payload."));
                return;
            }
            if ("/api/iot/heartbeat".equals(path)) {
                handleHeartbeat(req, resp, body);
            } else {
                handleEvent(req, resp, body);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error on " + path, e);
            send(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error("INTERNAL_ERROR", "Database error."));
        }
    }

    private void handleHeartbeat(HttpServletRequest req, HttpServletResponse resp, JsonObject body)
            throws SQLException, IOException {
        String deviceId = str(body, "deviceId");
        try (Connection conn = DBUtil.getConnection()) {
            if (!authorize(conn, deviceId, req, resp)) {
                return;
            }
        }
        String ack = str(body, "ack");
        if (ack != null && !ack.isEmpty()) {
            Map<String, Object> ev = new HashMap<>();
            ev.put("deviceId", deviceId);
            ev.put("command", ack.length() > 20 ? ack.substring(0, 20) : ack);
            websocket.RealtimeWebSocketEndpoint.broadcastEvent("DEVICE_ACK", ev);
        }
        JsonObject out = new JsonObject();
        out.addProperty("ok", true);
        out.addProperty("serverTime", System.currentTimeMillis() / 1000L);
        send(resp, HttpServletResponse.SC_OK, out);
    }

    private void handleEvent(HttpServletRequest req, HttpServletResponse resp, JsonObject body)
            throws SQLException, IOException {
        String deviceId = str(body, "deviceId");
        EventAlert alert = IotDevices.mapEvent(str(body, "type"), str(body, "detail"));
        if (alert == null) {
            send(resp, HttpServletResponse.SC_BAD_REQUEST, error("UNKNOWN_EVENT", "Unknown event type."));
            return;
        }
        boolean created = false;
        try (Connection conn = DBUtil.getConnection()) {
            DeviceAuth auth = IotDevices.authenticateAndTouch(conn, deviceId == null ? "" : deviceId, req);
            if (!checkAuth(auth, resp)) {
                return;
            }
            String insert = "INSERT INTO ALERT_LOG (alert_type, severity, status, message, device_id, room_id, zone_id, created_at) "
                    + "SELECT ?, ?, 'NEW', ?, ?, ?, ?, SYSDATETIME() "
                    + "WHERE NOT EXISTS (SELECT 1 FROM ALERT_LOG WHERE device_id = ? AND alert_type = ? "
                    + "  AND created_at >= DATEADD(SECOND, -?, SYSDATETIME()))";
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setString(1, alert.alertType);
                ps.setString(2, alert.severity);
                ps.setString(3, alert.message);
                ps.setString(4, deviceId);
                if (auth.roomId != null) {
                    ps.setLong(5, auth.roomId);
                } else {
                    ps.setNull(5, Types.BIGINT);
                }
                ps.setString(6, auth.zoneId);
                ps.setString(7, deviceId);
                ps.setString(8, alert.alertType);
                ps.setInt(9, EVENT_DEDUP_SECONDS);
                created = ps.executeUpdate() > 0;
            }
            if (created && "PIN_BRUTE_FORCE".equals(alert.alertType)) {
                // Lockout also shows in Activity logs (access side); attempted_at is REAL time
                String log = "INSERT INTO ACCESS_LOG (credential_id, device_id, request_id, attempted_at, result, reason_code) "
                        + "VALUES (NULL, ?, ?, SYSDATETIME(), 'DENIED', 'LOCKED_OUT')";
                try (PreparedStatement ps = conn.prepareStatement(log)) {
                    ps.setString(1, deviceId);
                    ps.setString(2, "LOCKOUT-" + UUID.randomUUID().toString());
                    ps.executeUpdate();
                }
            }
        }
        if (created) {
            job.SchedulerJobs.announceAlerts(alert.alertType);
            LOGGER.warning("Device alert " + alert.alertType + " from " + deviceId + ": " + alert.message);
        }
        JsonObject out = new JsonObject();
        out.addProperty("ok", true);
        out.addProperty("created", created);
        out.addProperty("alertType", alert.alertType);
        send(resp, HttpServletResponse.SC_OK, out);
    }

    private void handleSnapshot(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        String deviceId = req.getParameter("deviceId");
        String reason = req.getParameter("reason");
        DeviceAuth auth;
        try (Connection conn = DBUtil.getConnection()) {
            auth = IotDevices.authenticateAndTouch(conn, deviceId == null ? "" : deviceId.trim(), req);
        }
        if (!checkAuth(auth, resp)) {
            return;
        }
        deviceId = deviceId.trim();
        byte[] jpg = IotDevices.readBody(req, MAX_JPEG_BYTES);
        if (jpg == null) {
            send(resp, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, error("TOO_LARGE", "Image is larger than 3 MB."));
            return;
        }
        if (jpg.length < 4 || (jpg[0] & 0xFF) != 0xFF || (jpg[1] & 0xFF) != 0xD8) {
            send(resp, HttpServletResponse.SC_BAD_REQUEST, error("NOT_JPEG", "Body must be a JPEG image."));
            return;
        }

        // Outside build/web (util.SnapshotStore) so Clean and Build never deletes photos
        File dir = util.SnapshotStore.dir(getServletContext());
        if (!dir.isDirectory()) {
            send(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error("NO_STORAGE", "Snapshot folder cannot be created."));
            return;
        }
        String name = "cam_" + new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date()) + ".jpg";
        try (OutputStream os = new FileOutputStream(new File(dir, name))) {
            os.write(jpg);
        }
        String relPath = util.SnapshotStore.URL_PREFIX + name;

        long motionId = -1;
        boolean linked = false;
        String zone = "LOBBY".equals(auth.zoneId) ? "LOBBY" : "MAIN_ENTRANCE";
        try (Connection conn = DBUtil.getConnection()) {
            String insert = "INSERT INTO MOTION_LOG (device_id, zone_id, event_id, detected_at, distance_cm, snapshot_path, received_at) "
                    + "VALUES (?, ?, ?, SYSDATETIME(), NULL, ?, SYSDATETIME())";
            try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, deviceId);
                ps.setString(2, zone);
                ps.setString(3, "CAM-" + (reason == null ? "manual" : reason.replaceAll("[^A-Za-z_]", "")) + "-"
                        + UUID.randomUUID().toString().substring(0, 8));
                ps.setString(4, relPath);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        motionId = rs.getLong(1);
                    }
                }
            }
            // First photo of a burst goes on the newest security alert of the same zone that has none yet
            String link = "UPDATE ALERT_LOG SET snapshot_path = ?, motion_log_id = ? "
                    + "WHERE alert_id = (SELECT TOP 1 alert_id FROM ALERT_LOG "
                    + "  WHERE zone_id = ? AND snapshot_path IS NULL "
                    + "    AND alert_type IN ('PIN_BRUTE_FORCE','DOOR_OPEN_ABNORMAL','SUSPICIOUS_MOTION') "
                    + "    AND created_at >= DATEADD(SECOND, -?, SYSDATETIME()) "
                    + "  ORDER BY created_at DESC, alert_id DESC)";
            try (PreparedStatement ps = conn.prepareStatement(link)) {
                ps.setString(1, relPath);
                if (motionId > 0) {
                    ps.setLong(2, motionId);
                } else {
                    ps.setNull(2, Types.BIGINT);
                }
                ps.setString(3, zone);
                ps.setInt(4, SNAPSHOT_LINK_SECONDS);
                linked = ps.executeUpdate() > 0;
            }
        }
        LOGGER.info("Snapshot saved from " + deviceId + " (" + jpg.length + " bytes) -> " + relPath
                + (linked ? " [linked to alert]" : ""));
        JsonObject out = new JsonObject();
        out.addProperty("ok", true);
        out.addProperty("path", relPath);
        out.addProperty("linkedToAlert", linked);
        send(resp, HttpServletResponse.SC_OK, out);
    }

    private boolean authorize(Connection conn, String deviceId, HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, IOException {
        if (deviceId == null || deviceId.isEmpty()) {
            send(resp, HttpServletResponse.SC_BAD_REQUEST, error("BAD_REQUEST", "deviceId is required."));
            return false;
        }
        return checkAuth(IotDevices.authenticateAndTouch(conn, deviceId, req), resp);
    }

    private boolean checkAuth(DeviceAuth auth, HttpServletResponse resp) throws IOException {
        if (!auth.found) {
            send(resp, HttpServletResponse.SC_NOT_FOUND, error("DEVICE_NOT_FOUND", "Unknown device."));
            return false;
        }
        if (!auth.allowed) {
            send(resp, HttpServletResponse.SC_UNAUTHORIZED, error("BAD_DEVICE_KEY", "Missing or wrong X-Device-Key, or device disabled."));
            return false;
        }
        return true;
    }

    private static JsonObject readJson(HttpServletRequest req) throws IOException {
        byte[] raw = IotDevices.readBody(req, MAX_JSON_BYTES);
        if (raw == null || raw.length == 0) {
            return null;
        }
        try {
            JsonElement el = JsonParser.parseString(new String(raw, StandardCharsets.UTF_8));
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String str(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return null;
        }
        try {
            return o.get(key).getAsString().trim();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static JsonObject error(String code, String message) {
        JsonObject o = new JsonObject();
        o.addProperty("ok", false);
        o.addProperty("code", code);
        o.addProperty("message", message);
        return o;
    }

    private static void send(HttpServletResponse resp, int status, JsonObject body) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write(body.toString());
    }
}
