package servlet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dao.DeviceDao;
import dao.DeviceLogDao;
import model.Device;
import model.MotionLogEntry;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * IoT Ingestion REST API endpoint: POST /api/iot/sensor-readings and /api/iot/motion-events.
 * Ingests PIR motion and ultrasonic distance events directly into MOTION_LOG.
 */
@WebServlet(name = "SensorReadingApiServlet", urlPatterns = {"/api/iot/sensor-readings", "/api/iot/motion-events"})
public class SensorReadingApiServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(SensorReadingApiServlet.class.getName());
    private static final Gson GSON = new Gson();

    private final DeviceDao deviceDao = new DeviceDao();
    private final DeviceLogDao deviceLogDao = new DeviceLogDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json; charset=UTF-8");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(sb.toString());
            if (!parsed.isJsonObject()) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "BAD_REQUEST", "Invalid JSON payload.");
                return;
            }
            root = parsed.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "BAD_REQUEST", "Invalid JSON payload.");
            return;
        }

        if (!root.has("device_id") || root.get("device_id").isJsonNull()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "BAD_REQUEST", "device_id is required.");
            return;
        }

        String deviceId = root.get("device_id").getAsString();
        if (deviceId == null || deviceId.trim().isEmpty()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "BAD_REQUEST", "device_id is required.");
            return;
        }
        deviceId = deviceId.trim();

        try {
            Device device = deviceDao.findById(deviceId);
            if (device == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "DEVICE_NOT_FOUND",
                        "Device '" + deviceId + "' does not exist.");
                return;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error looking up device " + deviceId, e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Database error checking device.");
            return;
        }

        String rootZone = root.has("zone_id") && !root.get("zone_id").isJsonNull()
                ? root.get("zone_id").getAsString().trim() : "LOBBY";

        int inserted = 0;
        int rejected = 0;
        List<String> errors = new ArrayList<>();

        if (root.has("readings") && root.get("readings").isJsonArray()) {
            JsonArray readingsArr = root.getAsJsonArray("readings");
            for (int i = 0; i < readingsArr.size(); i++) {
                JsonElement elem = readingsArr.get(i);
                if (!elem.isJsonObject()) {
                    rejected++;
                    errors.add("Item " + (i + 1) + ": reading must be a JSON object.");
                    continue;
                }
                JsonObject item = elem.getAsJsonObject();
                try {
                    boolean ok = processReading(deviceId, rootZone, item, errors);
                    if (ok) {
                        inserted++;
                    } else {
                        rejected++;
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed inserting motion event", e);
                    rejected++;
                    errors.add("Item " + (i + 1) + ": database error " + e.getMessage());
                }
            }
        } else {
            // Single event payload
            try {
                boolean ok = processReading(deviceId, rootZone, root, errors);
                if (ok) {
                    inserted++;
                } else {
                    rejected++;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed inserting motion event", e);
                rejected++;
                errors.add("Database error: " + e.getMessage());
            }
        }

        Map<String, Object> respMap = new LinkedHashMap<>();
        respMap.put("success", true);
        respMap.put("inserted", inserted);
        respMap.put("rejected", rejected);
        respMap.put("errors", errors);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(GSON.toJson(respMap));
    }

    private boolean processReading(String deviceId, String defaultZone, JsonObject item, List<String> errors)
            throws SQLException {
        String zone = item.has("zone_id") && !item.get("zone_id").isJsonNull()
                ? item.get("zone_id").getAsString().trim() : defaultZone;
        if (!"MAIN_ENTRANCE".equals(zone) && !"LOBBY".equals(zone)) {
            zone = "LOBBY";
        }

        String eventId = item.has("event_id") && !item.get("event_id").isJsonNull()
                ? item.get("event_id").getAsString().trim() : "EVT-" + UUID.randomUUID().toString();

        if (deviceLogDao.existsMotionEventId(eventId)) {
            errors.add("Duplicate event_id: " + eventId);
            return false;
        }

        BigDecimal distanceCm = null;
        if (item.has("distance_cm") && !item.get("distance_cm").isJsonNull()) {
            try {
                distanceCm = new BigDecimal(item.get("distance_cm").getAsString().trim());
            } catch (NumberFormatException ignored) {
            }
        } else if (item.has("value") && !item.get("value").isJsonNull()) {
            String metric = item.has("metric_type") && !item.get("metric_type").isJsonNull()
                    ? item.get("metric_type").getAsString().trim() : "";
            if ("DISTANCE_CM".equalsIgnoreCase(metric)) {
                try {
                    distanceCm = new BigDecimal(item.get("value").getAsString().trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        String snapshotPath = null;
        if (item.has("snapshot_url") && !item.get("snapshot_url").isJsonNull()) {
            snapshotPath = item.get("snapshot_url").getAsString().trim();
        } else if (item.has("snapshot_path") && !item.get("snapshot_path").isJsonNull()) {
            snapshotPath = item.get("snapshot_path").getAsString().trim();
        }

        Timestamp detectedAt = null;
        String timeStr = null;
        if (item.has("recorded_at") && !item.get("recorded_at").isJsonNull()) {
            timeStr = item.get("recorded_at").getAsString().trim();
        } else if (item.has("detected_at") && !item.get("detected_at").isJsonNull()) {
            timeStr = item.get("detected_at").getAsString().trim();
        }
        if (timeStr != null) {
            LocalDateTime ldt = parseDateTime(timeStr);
            if (ldt != null) {
                detectedAt = Timestamp.valueOf(ldt);
            }
        }
        if (detectedAt == null) {
            detectedAt = new Timestamp(System.currentTimeMillis());
        }

        MotionLogEntry entry = new MotionLogEntry();
        entry.setDeviceId(deviceId);
        entry.setZoneId(zone);
        entry.setEventId(eventId);
        entry.setDetectedAt(detectedAt);
        entry.setDistanceCm(distanceCm);
        entry.setSnapshotPath(snapshotPath);

        deviceLogDao.insertMotion(entry);
        return true;
    }

    private void sendError(HttpServletResponse resp, int statusCode, String code, String message) throws IOException {
        resp.setStatus(statusCode);
        Map<String, Object> errMap = new LinkedHashMap<>();
        errMap.put("success", false);
        errMap.put("code", code);
        errMap.put("message", message);
        resp.getWriter().write(GSON.toJson(errMap));
    }

    private static LocalDateTime parseDateTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        text = text.trim();
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }
}
