package util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * Shared helpers for the ESP32 endpoints (/api/iot/*): device API key check, heartbeat,
 * device event -> ALERT_LOG mapping and the signature of remote commands sent over MQTT.
 *
 * Device authentication: header "X-Device-Key". A device without an API key (api_key_hash NULL)
 * is accepted for backward compatibility; once a key is generated on the Devices page, every call
 * must carry it. Heartbeat time is REAL time (see docs/time-inventory.md).
 */
public final class IotDevices {

    public static final String KEY_HEADER = "X-Device-Key";

    /** Result of a device lookup + key check. */
    public static final class DeviceAuth {
        public final boolean found;
        public final boolean allowed;
        public final String zoneId;
        public final Long roomId;

        DeviceAuth(boolean found, boolean allowed, String zoneId, Long roomId) {
            this.found = found;
            this.allowed = allowed;
            this.zoneId = zoneId;
            this.roomId = roomId;
        }
    }

    /** One device event mapped to an ALERT_LOG row (types already allowed by CK_ALERT_LOG_alert_type). */
    public static final class EventAlert {
        public final String alertType;
        public final String severity;
        public final String message;

        EventAlert(String alertType, String severity, String message) {
            this.alertType = alertType;
            this.severity = severity;
            this.message = message;
        }
    }

    private static final Map<String, String[]> EVENTS = new HashMap<>();

    static {
        EVENTS.put("LOCKOUT", new String[]{"PIN_BRUTE_FORCE", "CRITICAL",
            "Keypad locked after 5 wrong PIN attempts in a row"});
        EVENTS.put("PRY", new String[]{"DOOR_OPEN_ABNORMAL", "CRITICAL",
            "Door forced open while locked - someone was standing at the door"});
        EVENTS.put("DOOR_FORCED", new String[]{"DOOR_OPEN_ABNORMAL", "MEDIUM",
            "Door opened while locked - no one detected outside (latch may not have closed)"});
        EVENTS.put("DOOR_OPEN_TOO_LONG", new String[]{"DOOR_OPEN_TOO_LONG", "HIGH",
            "Main entrance door held open too long"});
        EVENTS.put("LOITERING", new String[]{"SUSPICIOUS_MOTION", "MEDIUM",
            "Someone stayed at the door without entering a PIN"});
        EVENTS.put("EXIT_STUCK", new String[]{"DEVICE_FAULT", "MEDIUM",
            "Inside exit button held or stuck for more than 5 seconds"});
    }

    private IotDevices() {
    }

    /** Maps a device event type (LOCKOUT, PRY, ...) to an alert; null when the type is unknown. */
    public static EventAlert mapEvent(String type, String detail) {
        if (type == null) {
            return null;
        }
        String[] row = EVENTS.get(type.trim().toUpperCase());
        if (row == null) {
            return null;
        }
        String message = row[2];
        if (detail != null && !detail.trim().isEmpty()) {
            String d = detail.trim();
            message = message + " (" + (d.length() > 200 ? d.substring(0, 200) : d) + ")";
        }
        return new EventAlert(row[0], row[1], message);
    }

    public static String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return toHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Signature of a remote command: hex HMAC-SHA256 with key = the device's api_key_hash (64 hex chars,
     * used as ASCII bytes) over "CMD|ts|nonce". The ESP32 computes the same key as sha256(its API key).
     */
    public static String signCommand(String apiKeyHash, String command, long ts, String nonce) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiKeyHash.getBytes(StandardCharsets.US_ASCII), "HmacSHA256"));
            byte[] sig = mac.doFinal((command + "|" + ts + "|" + nonce).getBytes(StandardCharsets.US_ASCII));
            return toHex(sig);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Looks the device up and checks its key on an open connection; on success also stamps
     * last_seen_at (REAL time) so every API call counts as a heartbeat. FAULT is kept as is.
     */
    public static DeviceAuth authenticateAndTouch(Connection conn, String deviceId, HttpServletRequest req)
            throws SQLException {
        String sql = "SELECT api_key_hash, is_active, zone_id, room_id FROM DEVICE WHERE device_id = ?";
        String keyHash;
        boolean active;
        String zone;
        Long roomId;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deviceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return new DeviceAuth(false, false, null, null);
                }
                keyHash = rs.getString("api_key_hash");
                active = rs.getBoolean("is_active");
                zone = rs.getString("zone_id");
                long r = rs.getLong("room_id");
                roomId = rs.wasNull() ? null : r;
            }
        }
        boolean allowed = active && keyMatches(keyHash, req.getHeader(KEY_HEADER));
        if (allowed) {
            String touch = "UPDATE DEVICE SET last_seen_at = SYSDATETIME(), "
                    + "status = CASE WHEN status = 'FAULT' THEN status ELSE 'ONLINE' END "
                    + "WHERE device_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(touch)) {
                ps.setString(1, deviceId);
                ps.executeUpdate();
            }
        }
        return new DeviceAuth(true, allowed, zone, roomId);
    }

    /** No stored hash = open device (legacy); otherwise sha256(header) must equal the stored hash. */
    public static boolean keyMatches(String storedHash, String presentedKey) {
        if (storedHash == null || storedHash.trim().isEmpty()) {
            return true;
        }
        if (presentedKey == null || presentedKey.trim().isEmpty()) {
            return false;
        }
        byte[] a = storedHash.trim().toLowerCase().getBytes(StandardCharsets.US_ASCII);
        byte[] b = sha256Hex(presentedKey.trim()).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(a, b);
    }

    /** Reads at most maxBytes from the request body; returns null when the body is larger. */
    public static byte[] readBody(HttpServletRequest req, int maxBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(1024, Math.min(req.getContentLength(), maxBytes)));
        byte[] buf = new byte[8192];
        int total = 0;
        try (InputStream in = req.getInputStream()) {
            int n;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > maxBytes) {
                    return null;
                }
                out.write(buf, 0, n);
            }
        }
        return out.toByteArray();
    }

    /** Stored snapshot path (e.g. "assets/images/snapshots/x.jpg") whose file exists (see util.SnapshotStore). */
    public static boolean snapshotExists(ServletContext ctx, String path) {
        return SnapshotStore.exists(ctx, path);
    }
}
