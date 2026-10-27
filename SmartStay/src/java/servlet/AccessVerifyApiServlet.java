package servlet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import util.AppConstants;
import util.DBUtil;
import util.SecurityUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST API Endpoint for IoT Smart Lock Access Verification.
 * URL: POST /api/iot/access/verify
 * Handles online PIN verification from ESP32 with anti-replay protection.
 */
@WebServlet(name = "AccessVerifyApiServlet", urlPatterns = {"/api/iot/access/verify"})
public class AccessVerifyApiServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccessVerifyApiServlet.class.getName());
    private static final int MAX_TIME_DRIFT_SECONDS = 60; // 60s tolerance for IoT NTP drift

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        JsonObject responseJson = new JsonObject();
        long startNs = System.nanoTime();
        // Realtime pushes run after the answer is flushed so the ESP32 opens the door without waiting on them
        long checkedInRoomId = -1;
        long checkedInBookingId = -1;

        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            if (sb.length() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseJson.addProperty("allowed", false);
                responseJson.addProperty("reason", "EMPTY_PAYLOAD");
                resp.getWriter().write(responseJson.toString());
                return;
            }

            JsonObject payload = JsonParser.parseString(sb.toString()).getAsJsonObject();

            String deviceId = payload.has("deviceId") ? payload.get("deviceId").getAsString().trim() : AppConstants.MAIN_DOOR_DEVICE_ID;
            String requestId = payload.has("requestId") ? payload.get("requestId").getAsString().trim() : null;
            long timestamp = payload.has("timestamp") ? payload.get("timestamp").getAsLong() : 0;
            String pin = payload.has("pin") ? payload.get("pin").getAsString().trim() : "";

            if (requestId == null || requestId.isEmpty() || pin.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseJson.addProperty("allowed", false);
                responseJson.addProperty("reason", "INVALID_ARGUMENTS");
                resp.getWriter().write(responseJson.toString());
                return;
            }

            // 1. Anti-Replay: Check timestamp drift (if timestamp provided)
            if (timestamp > 0) {
                long now = Instant.now().getEpochSecond();
                if (Math.abs(now - timestamp) > MAX_TIME_DRIFT_SECONDS) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    responseJson.addProperty("allowed", false);
                    responseJson.addProperty("reason", "TIMESTAMP_DRIFT_EXCEEDED");
                    resp.getWriter().write(responseJson.toString());
                    LOGGER.warning("Access request rejected due to timestamp drift: " + requestId);
                    return;
                }
            }

            // 2. Compute HMAC-SHA256 of PIN
            byte[] pinHmac = SecurityUtil.hmacPin(pin);

            try (Connection conn = DBUtil.getConnection()) {
                // Device key (X-Device-Key, only once a key was generated) + heartbeat stamp
                util.IotDevices.DeviceAuth auth = util.IotDevices.authenticateAndTouch(conn, deviceId, req);
                if (!auth.found || !auth.allowed) {
                    resp.setStatus(auth.found ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_NOT_FOUND);
                    responseJson.addProperty("allowed", false);
                    responseJson.addProperty("reason", auth.found ? "BAD_DEVICE_KEY" : "DEVICE_NOT_FOUND");
                    resp.getWriter().write(responseJson.toString());
                    return;
                }

                // Check if requestId exists (anti-replay attack)
                String checkReplaySql = "SELECT 1 FROM ACCESS_LOG WHERE request_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(checkReplaySql)) {
                    ps.setString(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            resp.setStatus(HttpServletResponse.SC_CONFLICT);
                            responseJson.addProperty("allowed", false);
                            responseJson.addProperty("reason", "REPLAY_ATTACK_DETECTED");
                            resp.getWriter().write(responseJson.toString());
                            LOGGER.warning("Duplicate request_id detected (replay): " + requestId);
                            return;
                        }
                    }
                }

                // 3. Lookup credential matching pin_hmac and device_id
                String findCredSql = "SELECT ac.credential_id, ac.booking_id, ac.staff_assignment_id, "
                        + "ac.valid_from, ac.valid_until, ac.status, b.room_id, b.allow_early_checkin "
                        + "FROM ACCESS_CREDENTIAL ac "
                        + "LEFT JOIN BOOKING b ON ac.booking_id = b.booking_id "
                        + "WHERE ac.device_id = ? AND ac.pin_hmac = ?";

                long credentialId = -1;
                long bookingId = -1;
                long roomId = -1;
                String credStatus = null;
                Timestamp validFrom = null;
                Timestamp validUntil = null;
                boolean allowEarly = false;
                boolean foundCred = false;

                try (PreparedStatement ps = conn.prepareStatement(findCredSql)) {
                    ps.setString(1, deviceId);
                    ps.setBytes(2, pinHmac);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            foundCred = true;
                            credentialId = rs.getLong("credential_id");
                            bookingId = rs.getLong("booking_id");
                            roomId = rs.getLong("room_id");
                            credStatus = rs.getString("status");
                            validFrom = rs.getTimestamp("valid_from");
                            validUntil = rs.getTimestamp("valid_until");
                            allowEarly = rs.getBoolean("allow_early_checkin");
                        }
                    }
                }

                Timestamp clockTs = util.ClockService.nowTimestamp();
                Timestamp realTs = util.ClockService.realNowTimestamp();
                boolean allowed = false;
                String reasonCode = "WRONG_PIN";

                if (foundCred) {
                    if ("REVOKED".equalsIgnoreCase(credStatus)) {
                        reasonCode = "REVOKED";
                    } else if ("EXPIRED".equalsIgnoreCase(credStatus) || (validUntil != null && clockTs.after(validUntil))) {
                        reasonCode = "EXPIRED";
                    } else if (validFrom != null && clockTs.before(validFrom) && !allowEarly) {
                        reasonCode = "NOT_YET_VALID"; // "Too early to check in"
                    } else if ("PENDING".equalsIgnoreCase(credStatus) || "ACTIVE".equalsIgnoreCase(credStatus)) {
                        allowed = true;
                        reasonCode = "OK";
                    }
                }

                // 4. Record ACCESS_LOG (STRICT REAL TIME for security audit)
                String insertLogSql = "INSERT INTO ACCESS_LOG (credential_id, device_id, request_id, attempted_at, result, reason_code) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertLogSql)) {
                    if (credentialId > 0) {
                        ps.setLong(1, credentialId);
                    } else {
                        ps.setNull(1, Types.BIGINT);
                    }
                    ps.setString(2, deviceId);
                    ps.setString(3, requestId);
                    ps.setTimestamp(4, realTs);
                    ps.setString(5, allowed ? "ALLOWED" : "DENIED");
                    ps.setString(6, reasonCode);
                    ps.executeUpdate();
                }

                // 5. If allowed -> Update status
                if (allowed) {
                    // Update credential to ACTIVE and increment used_count
                    String updateCredSql = "UPDATE ACCESS_CREDENTIAL SET status = 'ACTIVE', used_count = used_count + 1 WHERE credential_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateCredSql)) {
                        ps.setLong(1, credentialId);
                        ps.executeUpdate();
                    }

                    // If first access, transition BOOKING to CHECKED_IN and ROOM to OCCUPIED
                    if (bookingId > 0) {
                        String updateBookingSql = "UPDATE BOOKING SET status = 'CHECKED_IN', actual_checkin_at = ? "
                                + "WHERE booking_id = ? AND status = 'CONFIRMED'";
                        try (PreparedStatement ps = conn.prepareStatement(updateBookingSql)) {
                            ps.setTimestamp(1, clockTs);
                            ps.setLong(2, bookingId);
                            int rows = ps.executeUpdate();
                            if (rows > 0 && roomId > 0) {
                                String updateRoomSql = "UPDATE ROOM SET status = 'OCCUPIED' WHERE room_id = ? AND status = 'AVAILABLE'";
                                try (PreparedStatement psR = conn.prepareStatement(updateRoomSql)) {
                                    psR.setLong(1, roomId);
                                    psR.executeUpdate();
                                }
                                checkedInRoomId = roomId;
                                checkedInBookingId = bookingId;
                            }
                        }
                    }

                    resp.setStatus(HttpServletResponse.SC_OK);
                    responseJson.addProperty("allowed", true);
                    responseJson.addProperty("reason", "OK");
                    responseJson.addProperty("durationSeconds", 5);
                    responseJson.addProperty("holder", bookingId > 0 ? "GUEST" : "STAFF");
                    LOGGER.info("Access GRANTED for device " + deviceId + " with credential #" + credentialId);
                } else {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    responseJson.addProperty("allowed", false);
                    responseJson.addProperty("reason", reasonCode);
                    if ("NOT_YET_VALID".equals(reasonCode)) {
                        responseJson.addProperty("message", "Too early to check in");
                    }
                    LOGGER.warning("Access DENIED for device " + deviceId + " | Reason: " + reasonCode);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during access verification", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseJson.addProperty("allowed", false);
            responseJson.addProperty("reason", "INTERNAL_DATABASE_ERROR");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during access verification", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseJson.addProperty("allowed", false);
            responseJson.addProperty("reason", "INTERNAL_SERVER_ERROR");
        }

        responseJson.addProperty("serverMs", (System.nanoTime() - startNs) / 1000000L);
        resp.getWriter().write(responseJson.toString());
        resp.flushBuffer();

        if (checkedInRoomId > 0) {
            websocket.RealtimeWebSocketEndpoint.broadcastBookingUpdated("BK" + String.format("%06d", checkedInBookingId), "CHECKED_IN", checkedInRoomId);
            websocket.RealtimeWebSocketEndpoint.broadcastRoomStatusChanged(checkedInRoomId, "", "OCCUPIED");
        }
    }
}
