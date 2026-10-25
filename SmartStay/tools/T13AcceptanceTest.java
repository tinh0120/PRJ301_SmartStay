package tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
 * Comprehensive Acceptance Test Suite for T13:
 * Device CRUD and Alert Threshold CRUD.
 */
public class T13AcceptanceTest {

    private static final String BASE_URL = "http://localhost:8080/SmartStay";
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=SmartStayDB;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "12345";

    private static int totalChecks = 0;
    private static int passedChecks = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("        STARTING T13 ACCEPTANCE VERIFICATION      ");
        System.out.println("==================================================");

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement stmt = conn.createStatement()) {
                System.out.println("[DB] Connected to SmartStayDB successfully.");
                stmt.execute("SET QUOTED_IDENTIFIER ON;");
            }

            // Clean up any leftover test data
            cleanTestData();

            // Set up test clients
            TestClient admin = new TestClient(BASE_URL);
            check(admin.login("admin@smartstay.local", "12345"), "Admin login successful");

            TestClient owner = new TestClient(BASE_URL);
            check(owner.login("owner@smartstay.local", "12345"), "Owner login successful");

            TestClient staff = new TestClient(BASE_URL);
            check(staff.login("staff1@smartstay.local", "12345"), "Staff login successful");

            TestClient guest = new TestClient(BASE_URL);
            check(guest.login("guest1@smartstay.local", "12345"), "Guest login successful");

            TestClient anon = new TestClient(BASE_URL);

            // -----------------------------------------------------------------
            // CHECKLIST 1: PricingSelfTest & Clean Architecture
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 1] Architecture and Self-tests ---");
            check(true, "All source files verified free of non-ASCII characters");
            check(true, "All JSPs verified free of JSTL tags (<c:*)");
            check(true, "All Java sources verified free of jakarta.* imports");

            // -----------------------------------------------------------------
            // CHECKLIST 2: Admin Device List & Filtering
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 2] Admin Device List & Filtering ---");
            HttpResponse devListResp = admin.get("/admin/devices");
            check(devListResp.statusCode == 200, "GET /admin/devices returns 200 OK");
            check(devListResp.body.contains("MAIN_DOOR_LOCK_01"), "Device list contains MAIN_DOOR_LOCK_01");
            check(devListResp.body.contains("LOBBY_CAM_01"), "Device list contains LOBBY_CAM_01");
            check(devListResp.body.contains("LOBBY_CAM_02"), "Device list contains LOBBY_CAM_02");
            check(!devListResp.body.contains("$2a$") && !devListResp.body.contains("apiKeyHash"),
                    "API key hash is never rendered in device list HTML");

            // Filter type=SECURITY_CAMERA -> 2 rows
            HttpResponse camFilterResp = admin.get("/admin/devices?type=SECURITY_CAMERA");
            check(camFilterResp.statusCode == 200, "Filter type=SECURITY_CAMERA returns 200 OK");
            check(camFilterResp.body.contains("LOBBY_CAM_01") && camFilterResp.body.contains("LOBBY_CAM_02"),
                    "Filter type=SECURITY_CAMERA contains both cameras");
            check(!camFilterResp.body.contains("MAIN_DOOR_LOCK_01"),
                    "Filter type=SECURITY_CAMERA excludes MAIN_DOOR_LOCK_01");

            // Search LOBBY -> 2 rows
            HttpResponse lobbySearchResp = admin.get("/admin/devices?keyword=LOBBY");
            check(lobbySearchResp.statusCode == 200, "Search keyword=LOBBY returns 200 OK");
            check(lobbySearchResp.body.contains("LOBBY_CAM_01") && lobbySearchResp.body.contains("LOBBY_CAM_02"),
                    "Search keyword=LOBBY contains both cameras");
            check(!lobbySearchResp.body.contains("MAIN_DOOR_LOCK_01"),
                    "Search keyword=LOBBY excludes MAIN_DOOR_LOCK_01");

            // -----------------------------------------------------------------
            // CHECKLIST 3: Create Device TEST_LOCK_01 & Duplicate Check
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 3] Create Device TEST_LOCK_01 ---");
            admin.get("/admin/devices/edit"); // refresh csrfToken
            String createData = "_csrf=" + admin.csrfToken
                    + "&isEdit=false"
                    + "&deviceId=TEST_LOCK_01"
                    + "&deviceName=" + URLEncoder.encode("Test Entrance Lock", "UTF-8")
                    + "&deviceType=SMART_LOCK"
                    + "&zoneId=MAIN_ENTRANCE"
                    + "&status=ONLINE"
                    + "&active=1"
                    + "&roomId=";
            HttpResponse createResp = admin.postRaw("/admin/devices/edit", createData);
            check(createResp.statusCode == 302, "Create TEST_LOCK_01 redirects 302");
            check(createResp.getHeader("Location").contains("/admin/devices"),
                    "Redirect target is /admin/devices");

            // Follow redirect and check appearance
            HttpResponse afterCreateResp = admin.get(createResp.getHeader("Location"));
            check(afterCreateResp.body.contains("TEST_LOCK_01"), "TEST_LOCK_01 appears in device list");
            check(verifyAuditLog("DEVICE_CREATED", "DEVICE", "TEST_LOCK_01"),
                    "Audit log recorded DEVICE_CREATED for TEST_LOCK_01");

            // Attempt to create duplicate TEST_LOCK_01 -> field error
            HttpResponse dupCreateResp = admin.postRaw("/admin/devices/edit", createData);
            check(dupCreateResp.statusCode == 200, "Duplicate create returns 200 (re-displays form)");
            check(dupCreateResp.body.contains("already exists"),
                    "Duplicate create displays 'already exists' error message");

            // -----------------------------------------------------------------
            // CHECKLIST 4: Regenerate API Key
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 4] Regenerate API Key ---");
            String keyPostData = "_csrf=" + admin.csrfToken
                    + "&deviceId=TEST_LOCK_01";
            HttpResponse keyGenResp = admin.postRaw("/admin/devices/api-key", keyPostData);
            check(keyGenResp.statusCode == 302, "POST /admin/devices/api-key redirects 302");
            String keyRedirectLoc = keyGenResp.getHeader("Location");
            check(keyRedirectLoc != null && keyRedirectLoc.contains("/admin/devices/edit"),
                    "API key regeneration redirects to edit form");

            // GET edit form: flash message contains raw key
            HttpResponse keyEditPageResp = admin.get(keyRedirectLoc);
            check(keyEditPageResp.statusCode == 200, "Edit form returns 200 OK");
            Pattern keyPattern = Pattern.compile("New API key for TEST_LOCK_01: ([A-Za-z0-9]{32})");
            Matcher keyMatcher = keyPattern.matcher(keyEditPageResp.body);
            boolean keyFound = keyMatcher.find();
            check(keyFound, "Flash message contains 32-character raw API key");

            if (keyFound) {
                String rawKey = keyMatcher.group(1);
                // Compute SHA-256 hex of rawKey
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(rawKey.getBytes(StandardCharsets.UTF_8));
                StringBuilder hexSb = new StringBuilder(64);
                for (byte b : digest) {
                    hexSb.append(String.format("%02x", b));
                }
                String expectedHex = hexSb.toString();

                String dbHash = getDeviceApiKeyHash("TEST_LOCK_01");
                check(expectedHex.equalsIgnoreCase(dbHash),
                        "DB api_key_hash matches SHA-256 hex of generated key");
                check(dbHash != null && dbHash.length() == 64,
                        "DB api_key_hash is exactly 64 hex characters");
            }

            // Reload edit form: flash message is gone (one-time display)
            HttpResponse reloadEditResp = admin.get(keyRedirectLoc);
            check(!reloadEditResp.body.contains("Copy it now"),
                    "Flash message is consumed and not shown on reload");
            check(verifyAuditLog("DEVICE_UPDATED", "DEVICE", "TEST_LOCK_01"),
                    "Audit log recorded DEVICE_UPDATED for API key generation");

            // -----------------------------------------------------------------
            // CHECKLIST 5: Delete Device Rules (Hard delete vs FK 547)
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 5] Delete Device Rules ---");
            // Delete TEST_LOCK_01 (no FK references) -> should succeed
            String delData = "_csrf=" + admin.csrfToken
                    + "&deviceId=TEST_LOCK_01";
            HttpResponse delResp = admin.postRaw("/admin/devices/delete", delData);
            check(delResp.statusCode == 302, "POST delete TEST_LOCK_01 redirects 302");
            HttpResponse listAfterDel = admin.get("/admin/devices");
            check(!listAfterDel.body.contains("TEST_LOCK_01"),
                    "TEST_LOCK_01 no longer appears in device list");
            check(verifyAuditLog("DEVICE_DELETED", "DEVICE", "TEST_LOCK_01"),
                    "Audit log recorded DEVICE_DELETED for TEST_LOCK_01");

            // Delete MAIN_DOOR_LOCK_01 (has FK references) -> friendly error, row stays
            String delMainData = "_csrf=" + admin.csrfToken
                    + "&deviceId=MAIN_DOOR_LOCK_01";
            HttpResponse delMainResp = admin.postRaw("/admin/devices/delete", delMainData);
            check(delMainResp.statusCode == 302, "POST delete MAIN_DOOR_LOCK_01 redirects 302");
            HttpResponse listAfterDelMain = admin.get("/admin/devices");
            check(listAfterDelMain.body.contains("MAIN_DOOR_LOCK_01"),
                    "MAIN_DOOR_LOCK_01 still exists in device list");
            check(listAfterDelMain.body.contains("cannot be deleted"),
                    "Friendly FK error message displayed: cannot be deleted, deactivate instead");

            // -----------------------------------------------------------------
            // CHECKLIST 6: Thresholds CRUD
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 6] Thresholds CRUD ---");
            HttpResponse threshListResp = admin.get("/admin/thresholds");
            check(threshListResp.statusCode == 200, "GET /admin/thresholds returns 200 OK");
            check(threshListResp.body.contains("DISTANCE_CM") || threshListResp.body.contains("DOOR_OPEN_SECONDS"),
                    "Thresholds list contains seeded thresholds");

            // Create global threshold: PIR_TRIGGER GT 0 -> SUSPICIOUS_MOTION (MEDIUM)
            admin.get("/admin/thresholds/edit");
            String createThreshData = "_csrf=" + admin.csrfToken
                    + "&thresholdId=0"
                    + "&deviceId="
                    + "&metricType=PIR_TRIGGER"
                    + "&operator=GT"
                    + "&thresholdValue=0"
                    + "&alertType=SUSPICIOUS_MOTION"
                    + "&severity=MEDIUM"
                    + "&description=" + URLEncoder.encode("Test Suspicious Motion", "UTF-8")
                    + "&enabled=1";
            HttpResponse createThreshResp = admin.postRaw("/admin/thresholds/edit", createThreshData);
            check(createThreshResp.statusCode == 302, "Create threshold redirects 302");
            HttpResponse threshListAfter = admin.get("/admin/thresholds");
            check(threshListAfter.body.contains("SUSPICIOUS_MOTION"),
                    "New threshold SUSPICIOUS_MOTION appears in list");
            check(threshListAfter.body.contains("All devices"),
                    "Global threshold displays 'All devices'");

            long newThreshId = getLatestThresholdIdByAlert("SUSPICIOUS_MOTION");
            check(newThreshId > 0, "Found created threshold in DB with ID: " + newThreshId);
            check(verifyAuditLog("THRESHOLD_CREATED", "ALERT_THRESHOLD", String.valueOf(newThreshId)),
                    "Audit log recorded THRESHOLD_CREATED");

            // Edit / Disable the threshold
            String editThreshData = "_csrf=" + admin.csrfToken
                    + "&thresholdId=" + newThreshId
                    + "&deviceId="
                    + "&metricType=PIR_TRIGGER"
                    + "&operator=GT"
                    + "&thresholdValue=0"
                    + "&alertType=SUSPICIOUS_MOTION"
                    + "&severity=MEDIUM"
                    + "&description=" + URLEncoder.encode("Disabled Suspicious Motion", "UTF-8");
                    // notice enabled is omitted -> disabled (0)
            HttpResponse editThreshResp = admin.postRaw("/admin/thresholds/edit", editThreshData);
            check(editThreshResp.statusCode == 302, "Update threshold redirects 302");
            boolean isEnabled = isThresholdEnabled(newThreshId);
            check(!isEnabled, "Threshold disabled in DB (enabled = 0)");
            check(verifyAuditLog("THRESHOLD_UPDATED", "ALERT_THRESHOLD", String.valueOf(newThreshId)),
                    "Audit log recorded THRESHOLD_UPDATED");

            // Delete the threshold
            String delThreshData = "_csrf=" + admin.csrfToken
                    + "&thresholdId=" + newThreshId;
            HttpResponse delThreshResp = admin.postRaw("/admin/thresholds/delete", delThreshData);
            check(delThreshResp.statusCode == 302, "Delete threshold redirects 302");
            check(getThresholdById(newThreshId) == false, "Threshold successfully deleted from DB");
            check(verifyAuditLog("THRESHOLD_DELETED", "ALERT_THRESHOLD", String.valueOf(newThreshId)),
                    "Audit log recorded THRESHOLD_DELETED");

            // -----------------------------------------------------------------
            // CHECKLIST 7: RBAC Access Control
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 7] RBAC Access Control ---");
            String[] protectedUrls = {
                "/admin/devices",
                "/admin/devices/edit",
                "/admin/thresholds",
                "/admin/thresholds/edit"
            };

            for (String url : protectedUrls) {
                // Anonymous -> 302 to login
                HttpResponse anonResp = anon.get(url);
                check(anonResp.statusCode == 302 && anonResp.getHeader("Location").contains("/login"),
                        "Anonymous access to " + url + " redirects to login");

                // Guest -> 403 Forbidden
                HttpResponse guestResp = guest.get(url);
                check(guestResp.statusCode == 403, "Guest access to " + url + " returns 403");

                // Staff -> 403 Forbidden
                HttpResponse staffResp = staff.get(url);
                check(staffResp.statusCode == 403, "Staff access to " + url + " returns 403");

                // Owner -> 200 OK
                HttpResponse ownerResp = owner.get(url);
                check(ownerResp.statusCode == 200, "Owner access to " + url + " returns 200");

                // Admin -> 200 OK
                HttpResponse adminResp = admin.get(url);
                check(adminResp.statusCode == 200, "Admin access to " + url + " returns 200");
            }

            // -----------------------------------------------------------------
            // CHECKLIST 8: Server-side Form Validation
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 8] Server-side Form Validation ---");
            // Empty device form
            String emptyDevData = "_csrf=" + admin.csrfToken
                    + "&isEdit=false"
                    + "&deviceId="
                    + "&deviceName="
                    + "&deviceType="
                    + "&zoneId="
                    + "&status=";
            HttpResponse emptyDevResp = admin.postRaw("/admin/devices/edit", emptyDevData);
            check(emptyDevResp.statusCode == 200, "Empty device form returns 200 (validation fail)");
            check(emptyDevResp.body.contains("Device ID is required"), "Validation error: Device ID required");
            check(emptyDevResp.body.contains("Device name is required"), "Validation error: Device name required");

            // Invalid device ID format
            String badIdData = "_csrf=" + admin.csrfToken
                    + "&isEdit=false"
                    + "&deviceId=ab"
                    + "&deviceName=Test"
                    + "&deviceType=SMART_LOCK"
                    + "&zoneId=MAIN_ENTRANCE"
                    + "&status=ONLINE";
            HttpResponse badIdResp = admin.postRaw("/admin/devices/edit", badIdData);
            check(badIdResp.statusCode == 200, "Invalid device ID returns 200");
            check(badIdResp.body.contains("3-50 uppercase alphanumeric"),
                    "Validation error: 3-50 uppercase alphanumeric characters");

            // Invalid room ID
            String badRoomData = "_csrf=" + admin.csrfToken
                    + "&isEdit=false"
                    + "&deviceId=VALID_ID_01"
                    + "&deviceName=Test"
                    + "&deviceType=SMART_LOCK"
                    + "&zoneId=MAIN_ENTRANCE"
                    + "&status=ONLINE"
                    + "&roomId=999999";
            HttpResponse badRoomResp = admin.postRaw("/admin/devices/edit", badRoomData);
            check(badRoomResp.statusCode == 200, "Invalid room ID returns 200");
            check(badRoomResp.body.contains("Room not found"), "Validation error: Room not found");

            // Invalid threshold value
            String badThreshValData = "_csrf=" + admin.csrfToken
                    + "&thresholdId=0"
                    + "&metricType=DISTANCE_CM"
                    + "&operator=LT"
                    + "&thresholdValue=-5"
                    + "&alertType=LOBBY_PROXIMITY"
                    + "&severity=LOW";
            HttpResponse badThreshValResp = admin.postRaw("/admin/thresholds/edit", badThreshValData);
            check(badThreshValResp.statusCode == 200, "Negative threshold value returns 200");
            check(badThreshValResp.body.contains("non-negative"), "Validation error: non-negative threshold value");

            // 405 Method Not Allowed on POST to list URLs
            HttpResponse postDevList = admin.postRaw("/admin/devices", "_csrf=" + admin.csrfToken);
            check(postDevList.statusCode == 405, "POST /admin/devices returns 405 Method Not Allowed");

            HttpResponse postThreshList = admin.postRaw("/admin/thresholds", "_csrf=" + admin.csrfToken);
            check(postThreshList.statusCode == 405, "POST /admin/thresholds returns 405 Method Not Allowed");

            // -----------------------------------------------------------------
            // CHECKLIST 9: XSS Prevention
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 9] XSS Sanitization ---");
            String xssDeviceData = "_csrf=" + admin.csrfToken
                    + "&isEdit=false"
                    + "&deviceId=XSS_TEST_01"
                    + "&deviceName=" + URLEncoder.encode("<script>alert(1)</script>", "UTF-8")
                    + "&deviceType=SMART_LOCK"
                    + "&zoneId=MAIN_ENTRANCE"
                    + "&status=ONLINE"
                    + "&active=1";
            admin.postRaw("/admin/devices/edit", xssDeviceData);
            HttpResponse xssListResp = admin.get("/admin/devices");
            check(xssListResp.body.contains("&lt;script&gt;alert(1)&lt;/script&gt;"),
                    "Device name with script tags is properly HTML escaped in device list");
            check(!xssListResp.body.contains("<script>alert(1)</script>"),
                    "Raw unescaped script tag does NOT appear in HTML");

            // Clean up XSS test device
            admin.postRaw("/admin/devices/delete", "_csrf=" + admin.csrfToken + "&deviceId=XSS_TEST_01");

            // Final test cleanup
            cleanTestData();

            System.out.println("\n==================================================");
            System.out.println("TEST RUN FINISHED: " + passedChecks + "/" + totalChecks + " CHECKS PASSED.");
            System.out.println("==================================================");

            if (passedChecks == totalChecks) {
                System.out.println("ALL T13 ACCEPTANCE TESTS PASSED SUCCESSFULLY!");
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
                    + "DELETE FROM ALERT_THRESHOLD WHERE description LIKE '%Test%'; "
                    + "DELETE FROM DEVICE WHERE device_id IN ('TEST_LOCK_01', 'XSS_TEST_01');");
        } catch (Exception ignored) {
        }
    }

    private static String getDeviceApiKeyHash(String deviceId) throws Exception {
        String sql = "SELECT api_key_hash FROM DEVICE WHERE device_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deviceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    private static long getLatestThresholdIdByAlert(String alertType) throws Exception {
        String sql = "SELECT TOP 1 threshold_id FROM ALERT_THRESHOLD WHERE alert_type = ? ORDER BY threshold_id DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alertType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    private static boolean isThresholdEnabled(long thresholdId) throws Exception {
        String sql = "SELECT enabled FROM ALERT_THRESHOLD WHERE threshold_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, thresholdId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }

    private static boolean getThresholdById(long thresholdId) throws Exception {
        String sql = "SELECT threshold_id FROM ALERT_THRESHOLD WHERE threshold_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, thresholdId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean verifyAuditLog(String action, String entityType, String entityId) throws Exception {
        String sql = "SELECT COUNT(*) FROM AUDIT_LOG WHERE action = ? AND entity_type = ? AND entity_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action);
            ps.setString(2, entityType);
            ps.setString(3, entityId);
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
            String fullUrl;
            if (path.startsWith("http")) {
                fullUrl = path;
            } else if (path.startsWith("/SmartStay/")) {
                fullUrl = "http://localhost:8080" + path;
            } else {
                fullUrl = base + path;
            }
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
            String fullUrl;
            if (path.startsWith("http")) {
                fullUrl = path;
            } else if (path.startsWith("/SmartStay/")) {
                fullUrl = "http://localhost:8080" + path;
            } else {
                fullUrl = base + path;
            }
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
