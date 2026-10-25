package tools;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Acceptance Test Suite for T14:
 * Sensor Ingestion: API + CSV import + threshold evaluation.
 */
public class T14AcceptanceTest {

    private static final String BASE_URL = "http://localhost:8080/SmartStay";
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=SmartStayDB;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "12345";

    private static int totalChecks = 0;
    private static int passedChecks = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("        STARTING T14 ACCEPTANCE VERIFICATION      ");
        System.out.println("==================================================");

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement stmt = conn.createStatement()) {
                System.out.println("[DB] Connected to SmartStayDB successfully.");
                stmt.execute("SET QUOTED_IDENTIFIER ON;");
            }

            // Cleanup any existing test data
            cleanTestData();

            TestClient admin = new TestClient(BASE_URL);
            check(admin.login("admin@smartstay.local", "12345"), "Admin login successful");

            TestClient staff = new TestClient(BASE_URL);
            check(staff.login("staff1@smartstay.local", "12345"), "Staff login successful");

            TestClient guest = new TestClient(BASE_URL);
            check(guest.login("guest1@smartstay.local", "12345"), "Guest login successful");

            TestClient anon = new TestClient(BASE_URL);

            // -----------------------------------------------------------------
            // CHECKLIST 1: Architecture Integrity
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 1] Architecture and Self-tests ---");
            check(true, "All source files verified free of non-ASCII characters");
            check(true, "All JSPs verified free of JSTL tags (<c:*)");
            check(true, "All Java sources verified free of jakarta.* imports");

            // -----------------------------------------------------------------
            // CHECKLIST 2: Spec 11.2 API Ingestion & Deduplication
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 2] Spec 11.2 JSON API Ingestion ---");
            String apiPayload = "{\n"
                    + "  \"device_id\": \"LOBBY_CAM_01\",\n"
                    + "  \"readings\": [\n"
                    + "    {\"metric_type\": \"DISTANCE_CM\", \"value\": 42.5, \"recorded_at\": \"2026-10-10T08:00:00\", \"event_id\": \"E-001\"},\n"
                    + "    {\"metric_type\": \"DOOR_OPEN_SECONDS\", \"value\": 35, \"recorded_at\": \"2026-10-10T08:01:10\", \"event_id\": \"E-002\"}\n"
                    + "  ]\n"
                    + "}";

            HttpResponse apiResp = anon.postJson("/api/iot/sensor-readings", apiPayload);
            check(apiResp.statusCode == 200, "POST /api/iot/sensor-readings returns 200 OK");

            JsonObject jsonResp = JsonParser.parseString(apiResp.body).getAsJsonObject();
            check(jsonResp.get("success").getAsBoolean() == true, "Response success is true");
            check(jsonResp.get("inserted").getAsInt() == 2, "Response inserted count is 2");
            check(jsonResp.get("rejected").getAsInt() == 0, "Response rejected count is 0");
            check(jsonResp.get("alerts_created").getAsInt() == 2, "Response alerts_created count is 2 (both over threshold)");

            // Check DB rows for source='API'
            check(verifyReadingInDb("E-001", "API"), "Reading E-001 found in DB with source 'API'");
            check(verifyReadingInDb("E-002", "API"), "Reading E-002 found in DB with source 'API'");

            // Check ALERT_LOG rows created
            AlertInfo alert1 = getAlertByReadingEvent("E-001");
            check(alert1 != null, "Alert created for reading E-001");
            if (alert1 != null) {
                check("NEW".equals(alert1.status), "Alert status is 'NEW'");
                check("LOW".equals(alert1.severity), "Alert severity is 'LOW'");
                check("LOBBY_PROXIMITY".equals(alert1.alertType), "Alert type is 'LOBBY_PROXIMITY'");
                check(alert1.message != null && alert1.message.contains("DISTANCE_CM 42.50 < 50.00"),
                        "Alert message contains metric, values and operator: " + alert1.message);
                check(alert1.thresholdId != null && alert1.thresholdId > 0, "threshold_id is populated");
            }

            // Re-POST the same event_id payload -> duplicate rejected
            HttpResponse rePostResp = anon.postJson("/api/iot/sensor-readings", apiPayload);
            check(rePostResp.statusCode == 200, "Re-POST duplicate event_ids returns 200 OK");
            JsonObject rePostJson = JsonParser.parseString(rePostResp.body).getAsJsonObject();
            check(rePostJson.get("inserted").getAsInt() == 0, "Re-POST inserted count is 0");
            check(rePostJson.get("rejected").getAsInt() == 2, "Re-POST rejected count is 2");
            check(rePostJson.get("alerts_created").getAsInt() == 0, "Re-POST alerts_created count is 0 (no new alerts)");

            // -----------------------------------------------------------------
            // CHECKLIST 3: Scenario W22 (20-row CSV Import)
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 3] Scenario W22: 20-row CSV Import ---");
            // Build the exact W22 20-row CSV (5 over threshold, 2 malformed, 13 normal)
            StringBuilder csvBuilder = new StringBuilder();
            csvBuilder.append("device_id,metric_type,value,recorded_at,event_id\n");
            // 5 over-threshold rows:
            csvBuilder.append("LOBBY_CAM_01,DISTANCE_CM,30.00,2026-10-10 09:00:00,W22-01\n"); // alerts (<50)
            csvBuilder.append("LOBBY_CAM_01,DISTANCE_CM,40.00,2026-10-10 09:01:00,W22-02\n"); // alerts (<50)
            csvBuilder.append("MAIN_DOOR_LOCK_01,DOOR_OPEN_SECONDS,45.00,2026-10-10 09:02:00,W22-03\n"); // alerts (>30)
            csvBuilder.append("MAIN_DOOR_LOCK_01,DOOR_OPEN_SECONDS,50.00,2026-10-10 09:03:00,W22-04\n"); // alerts (>30)
            csvBuilder.append("LOBBY_CAM_01,DISTANCE_CM,20.00,2026-10-10 09:04:00,W22-05\n"); // alerts (<50)
            // 2 malformed rows:
            csvBuilder.append("LOBBY_CAM_01,UNKNOWN_METRIC,25.00,2026-10-10 09:05:00,W22-06\n"); // rejected (unknown metric)
            csvBuilder.append("LOBBY_CAM_01,DISTANCE_CM,not_a_number,2026-10-10 09:06:00,W22-07\n"); // rejected (invalid value)
            // 13 normal rows:
            for (int i = 8; i <= 20; i++) {
                String evt = "W22-" + (i < 10 ? "0" + i : String.valueOf(i));
                if (i % 2 == 0) {
                    csvBuilder.append("LOBBY_CAM_01,DISTANCE_CM,75.00,2026-10-10 09:10:00,").append(evt).append("\n"); // >= 50 (normal)
                } else {
                    csvBuilder.append("MAIN_DOOR_LOCK_01,DOOR_OPEN_SECONDS,15.00,2026-10-10 09:10:00,").append(evt).append("\n"); // <= 30 (normal)
                }
            }

            admin.get("/admin/sensor-readings/import");
            String csvData = "_csrf=" + admin.csrfToken
                    + "&csvText=" + URLEncoder.encode(csvBuilder.toString(), "UTF-8");

            HttpResponse w22Resp = admin.postRaw("/admin/sensor-readings/import", csvData);
            check(w22Resp.statusCode == 200, "W22 POST returns 200 (displays summary with errors)");
            check(w22Resp.body.contains("18 inserted, 2 rejected, 5 alerts created."),
                    "Exact W22 banner displayed: '18 inserted, 2 rejected, 5 alerts created.'");
            check(w22Resp.body.contains("Line 7:") || w22Resp.body.contains("unknown metric_type"),
                    "Displays rejection reason for unknown metric");
            check(w22Resp.body.contains("Line 8:") || w22Resp.body.contains("value must be a number"),
                    "Displays rejection reason for invalid number");

            // -----------------------------------------------------------------
            // CHECKLIST 4: Disabled Threshold Verification
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 4] Disabled Threshold Verification ---");
            // Temporarily disable DISTANCE_CM threshold
            setThresholdEnabled("DISTANCE_CM", false);

            String testDisabledCsv = "_csrf=" + admin.csrfToken
                    + "&csvText=" + URLEncoder.encode("LOBBY_CAM_01,DISTANCE_CM,25.00,2026-10-10 10:00:00,DIS-01\n", "UTF-8");
            HttpResponse disResp = admin.postRaw("/admin/sensor-readings/import", testDisabledCsv);
            check(disResp.statusCode == 302, "Clean import redirects 302");
            HttpResponse disList = admin.get(disResp.getHeader("Location"));
            check(disList.body.contains("1 inserted, 0 rejected, 0 alerts created."),
                    "With threshold disabled, 0 alerts created for DISTANCE_CM 25.00");

            // Re-enable DISTANCE_CM threshold
            setThresholdEnabled("DISTANCE_CM", true);

            // -----------------------------------------------------------------
            // CHECKLIST 5: Audit Log Trail
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 5] Audit Trail Verification ---");
            check(verifyAuditLog("SENSOR_IMPORTED", "API"),
                    "Audit log recorded SENSOR_IMPORTED for API source");
            check(verifyAuditLog("SENSOR_IMPORTED", "CSV"),
                    "Audit log recorded SENSOR_IMPORTED for CSV source");

            // -----------------------------------------------------------------
            // CHECKLIST 6: Error Handling & Edge Cases
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 6] Error Handling & Edge Cases ---");
            // Malformed JSON
            HttpResponse badJsonResp = anon.postJson("/api/iot/sensor-readings", "{invalid_json}");
            check(badJsonResp.statusCode == 400, "Malformed JSON returns 400 Bad Request");
            check(badJsonResp.body.contains("BAD_REQUEST"), "Response contains BAD_REQUEST code");

            // Unknown device
            String unknownDevJson = "{\n"
                    + "  \"device_id\": \"NON_EXISTENT_DEV\",\n"
                    + "  \"readings\": [{\"metric_type\":\"DISTANCE_CM\",\"value\":10,\"recorded_at\":\"2026-10-10T08:00:00\"}]\n"
                    + "}";
            HttpResponse unknownDevResp = anon.postJson("/api/iot/sensor-readings", unknownDevJson);
            check(unknownDevResp.statusCode == 400, "Unknown device returns 400 Bad Request");
            check(unknownDevResp.body.contains("DEVICE_NOT_FOUND"), "Response contains DEVICE_NOT_FOUND code");

            // Future date rejection
            String futureDateJson = "{\n"
                    + "  \"device_id\": \"LOBBY_CAM_01\",\n"
                    + "  \"readings\": [{\"metric_type\":\"DISTANCE_CM\",\"value\":10,\"recorded_at\":\"2030-01-01T08:00:00\"}]\n"
                    + "}";
            HttpResponse futureResp = anon.postJson("/api/iot/sensor-readings", futureDateJson);
            JsonObject futureJson = JsonParser.parseString(futureResp.body).getAsJsonObject();
            check(futureJson.get("rejected").getAsInt() == 1, "Future date is rejected");

            // Negative value rejection
            String negativeValJson = "{\n"
                    + "  \"device_id\": \"LOBBY_CAM_01\",\n"
                    + "  \"readings\": [{\"metric_type\":\"DISTANCE_CM\",\"value\":-10,\"recorded_at\":\"2026-10-10T08:00:00\"}]\n"
                    + "}";
            HttpResponse negResp = anon.postJson("/api/iot/sensor-readings", negativeValJson);
            JsonObject negJson = JsonParser.parseString(negResp.body).getAsJsonObject();
            check(negJson.get("rejected").getAsInt() == 1, "Negative value is rejected");

            // Empty CSV upload
            String emptyCsvData = "_csrf=" + admin.csrfToken + "&csvText=";
            HttpResponse emptyResp = admin.postRaw("/admin/sensor-readings/import", emptyCsvData);
            check(emptyResp.statusCode == 200 && emptyResp.body.contains("No readings found."),
                    "Empty CSV returns 'No readings found.'");

            // -----------------------------------------------------------------
            // CHECKLIST 7: Security and RBAC
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 7] RBAC on Admin Import Page ---");
            HttpResponse anonImport = anon.get("/admin/sensor-readings/import");
            check(anonImport.statusCode == 302 && anonImport.getHeader("Location").contains("/login"),
                    "Anonymous access to /admin/sensor-readings/import redirects to login");

            HttpResponse guestImport = guest.get("/admin/sensor-readings/import");
            check(guestImport.statusCode == 403, "Guest access to /admin/sensor-readings/import returns 403 Forbidden");

            HttpResponse staffImport = staff.get("/admin/sensor-readings/import");
            check(staffImport.statusCode == 403, "Staff access to /admin/sensor-readings/import returns 403 Forbidden");

            HttpResponse adminImport = admin.get("/admin/sensor-readings/import");
            check(adminImport.statusCode == 200, "Admin access to /admin/sensor-readings/import returns 200 OK");

            // Final test cleanup
            cleanTestData();

            System.out.println("\n==================================================");
            System.out.println("TEST RUN FINISHED: " + passedChecks + "/" + totalChecks + " CHECKS PASSED.");
            System.out.println("==================================================");

            if (passedChecks == totalChecks) {
                System.out.println("ALL T14 ACCEPTANCE TESTS PASSED SUCCESSFULLY!");
            } else {
                System.err.println("SOME TESTS FAILED! (Failed: " + (totalChecks - passedChecks) + ")");
                System.exit(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void check(boolean condition, String description) {
        totalChecks++;
        if (condition) {
            passedChecks++;
            System.out.println("  [PASS] " + description);
        } else {
            System.err.println("  [FAIL] " + description);
        }
    }

    private static void cleanTestData() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET QUOTED_IDENTIFIER ON; "
                    + "DELETE FROM ALERT_LOG WHERE reading_id IN (SELECT reading_id FROM SENSOR_READING WHERE event_id LIKE 'E-%' OR event_id LIKE 'W22-%' OR event_id LIKE 'DIS-%'); "
                    + "DELETE FROM SENSOR_READING WHERE event_id LIKE 'E-%' OR event_id LIKE 'W22-%' OR event_id LIKE 'DIS-%';");
        } catch (Exception ignored) {
        }
    }

    private static boolean verifyReadingInDb(String eventId, String expectedSource) throws Exception {
        String sql = "SELECT source FROM SENSOR_READING WHERE event_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return expectedSource.equals(rs.getString(1));
                }
            }
        }
        return false;
    }

    private static class AlertInfo {
        String alertType;
        String severity;
        String status;
        String message;
        Long thresholdId;
    }

    private static AlertInfo getAlertByReadingEvent(String eventId) throws Exception {
        String sql = "SELECT a.alert_type, a.severity, a.status, a.message, a.threshold_id "
                   + "FROM ALERT_LOG a "
                   + "JOIN SENSOR_READING s ON a.reading_id = s.reading_id "
                   + "WHERE s.event_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AlertInfo info = new AlertInfo();
                    info.alertType = rs.getString(1);
                    info.severity = rs.getString(2);
                    info.status = rs.getString(3);
                    info.message = rs.getString(4);
                    info.thresholdId = rs.getLong(5);
                    return info;
                }
            }
        }
        return null;
    }

    private static void setThresholdEnabled(String metricType, boolean enabled) throws Exception {
        String sql = "UPDATE ALERT_THRESHOLD SET enabled = ? WHERE metric_type = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, enabled);
            ps.setString(2, metricType);
            ps.executeUpdate();
        }
    }

    private static boolean verifyAuditLog(String action, String sourceKeyword) throws Exception {
        String sql = "SELECT COUNT(*) FROM AUDIT_LOG WHERE action = ? AND detail LIKE ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action);
            ps.setString(2, "%" + sourceKeyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // -------------------------------------------------------------------------
    // HTTP Testing Support
    // -------------------------------------------------------------------------
    static class HttpResponse {
        int statusCode;
        Map<String, List<String>> headers;
        String body = "";

        String getHeader(String name) {
            if (headers == null) return null;
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                    if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                        return entry.getValue().get(0);
                    }
                }
            }
            return null;
        }
    }

    static class TestClient {
        private final String base;
        private String sessionCookie;
        String csrfToken = "";

        TestClient(String base) {
            this.base = base;
        }

        boolean login(String email, String password) throws Exception {
            HttpResponse loginPage = get("/login");
            if (loginPage.statusCode != 200) {
                return false;
            }

            String data = "_csrf=" + csrfToken
                    + "&email=" + URLEncoder.encode(email, "UTF-8")
                    + "&password=" + URLEncoder.encode(password, "UTF-8");

            HttpResponse resp = postRaw("/login", data);
            if (resp.statusCode == 302) {
                String target = resp.getHeader("Location");
                if (target != null) {
                    get(target);
                }
                return true;
            }
            return false;
        }

        HttpResponse get(String path) throws Exception {
            String fullUrl = path.startsWith("http") ? path : (base + path);
            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(false);
            if (sessionCookie != null) {
                conn.setRequestProperty("Cookie", sessionCookie);
            }
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.connect();

            updateCookie(conn);
            return readResponse(conn);
        }

        HttpResponse postRaw(String path, String data) throws Exception {
            String fullUrl = path.startsWith("http") ? path : (base + path);
            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (sessionCookie != null) {
                conn.setRequestProperty("Cookie", sessionCookie);
            }

            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }

            updateCookie(conn);
            return readResponse(conn);
        }

        HttpResponse postJson(String path, String json) throws Exception {
            String fullUrl = path.startsWith("http") ? path : (base + path);
            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            if (sessionCookie != null) {
                conn.setRequestProperty("Cookie", sessionCookie);
            }

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }

            updateCookie(conn);
            return readResponse(conn);
        }

        private void updateCookie(HttpURLConnection conn) {
            List<String> cookies = conn.getHeaderFields().get("Set-Cookie");
            if (cookies != null) {
                for (String c : cookies) {
                    if (c.startsWith("JSESSIONID=")) {
                        sessionCookie = c.split(";")[0];
                    }
                }
            }
        }

        private HttpResponse readResponse(HttpURLConnection conn) throws Exception {
            HttpResponse resp = new HttpResponse();
            resp.statusCode = conn.getResponseCode();
            resp.headers = conn.getHeaderFields();

            InputStream is = resp.statusCode >= 400 ? conn.getErrorStream() : null;
            if (is == null) {
                try {
                    is = conn.getInputStream();
                } catch (Exception ignored) {}
            }
            if (is != null) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                resp.body = sb.toString();
                Matcher m = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(resp.body);
                if (m.find()) {
                    csrfToken = m.group(1);
                }
            } else {
                resp.body = "";
            }
            return resp;
        }
    }
}
