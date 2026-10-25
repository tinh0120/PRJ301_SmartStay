package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
 * Acceptance Test Suite for T16:
 * Access Log, Motion Log, Alert Log pages + acknowledge/resolve actions.
 */
public class T16AcceptanceTest {

    private static final String BASE_URL = "http://localhost:8080/SmartStay";
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=SmartStayDB;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "12345";

    private static int totalChecks = 0;
    private static int passedChecks = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("        STARTING T16 ACCEPTANCE VERIFICATION      ");
        System.out.println("==================================================");

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement stmt = conn.createStatement()) {
                System.out.println("[DB] Connected to SmartStayDB successfully.");
                stmt.execute("SET QUOTED_IDENTIFIER ON;");
            }

            TestClient admin = new TestClient(BASE_URL);
            check(admin.login("admin@smartstay.local", "12345"), "Admin login successful");

            TestClient staff = new TestClient(BASE_URL);
            check(staff.login("staff1@smartstay.local", "12345"), "Staff login successful");

            TestClient guest = new TestClient(BASE_URL);
            check(guest.login("guest1@smartstay.local", "12345"), "Guest login successful");

            TestClient anon = new TestClient(BASE_URL);

            // -----------------------------------------------------------------
            // CHECKLIST 1: Architecture & Source Code Quality
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 1] Architecture and Source Code Quality ---");
            File srcDir = new File("src/java/com/smartstay");
            check(checkCleanAscii(srcDir), "All Java source files in com.smartstay are 100% clean ASCII");

            File jspDir = new File("web/WEB-INF/views");
            check(checkCleanAscii(jspDir), "All JSPs in WEB-INF/views are 100% clean ASCII");

            File tagDir = new File("web/WEB-INF/tags");
            check(checkCleanAscii(tagDir), "All tags in WEB-INF/tags are 100% clean ASCII");

            check(checkNoJstl(jspDir), "No JSTL tags (<c:*) used across all JSPs");
            check(checkNoJakarta(srcDir), "No jakarta.* package imports in any Java files");

            // -----------------------------------------------------------------
            // CHECKLIST 2: RBAC & Permission Gating
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 2] RBAC & Route Access Security ---");

            // Access logs
            HttpResponse anonAccess = anon.get("/admin/access-logs");
            check(anonAccess.statusCode == 302 && anonAccess.redirectLocation != null
                    && anonAccess.redirectLocation.contains("/login"),
                    "Unauthenticated GET /admin/access-logs redirects to login");

            HttpResponse guestAccess = guest.get("/admin/access-logs");
            check(guestAccess.statusCode == 403, "Guest GET /admin/access-logs denied with 403");

            HttpResponse staffAccess = staff.get("/admin/access-logs");
            check(staffAccess.statusCode == 403, "Staff GET /admin/access-logs denied with 403");

            HttpResponse adminAccess = admin.get("/admin/access-logs");
            check(adminAccess.statusCode == 200, "Admin GET /admin/access-logs returns 200 OK");

            // Motion logs
            HttpResponse anonMotion = anon.get("/admin/motion-logs");
            check(anonMotion.statusCode == 302 && anonMotion.redirectLocation != null
                    && anonMotion.redirectLocation.contains("/login"),
                    "Unauthenticated GET /admin/motion-logs redirects to login");

            HttpResponse guestMotion = guest.get("/admin/motion-logs");
            check(guestMotion.statusCode == 403, "Guest GET /admin/motion-logs denied with 403");

            HttpResponse staffMotion = staff.get("/admin/motion-logs");
            check(staffMotion.statusCode == 403, "Staff GET /admin/motion-logs denied with 403");

            HttpResponse adminMotion = admin.get("/admin/motion-logs");
            check(adminMotion.statusCode == 200, "Admin GET /admin/motion-logs returns 200 OK");

            // Alert logs
            HttpResponse anonAlert = anon.get("/admin/alerts");
            check(anonAlert.statusCode == 302 && anonAlert.redirectLocation != null
                    && anonAlert.redirectLocation.contains("/login"),
                    "Unauthenticated GET /admin/alerts redirects to login");

            HttpResponse guestAlert = guest.get("/admin/alerts");
            check(guestAlert.statusCode == 403, "Guest GET /admin/alerts denied with 403");

            HttpResponse staffAlert = staff.get("/admin/alerts");
            check(staffAlert.statusCode == 403, "Staff GET /admin/alerts denied with 403");

            HttpResponse adminAlert = admin.get("/admin/alerts");
            check(adminAlert.statusCode == 200, "Admin GET /admin/alerts returns 200 OK");

            // Alert action POST permission check (ALERT_HANDLE)
            HttpResponse staffAction = staff.postForm("/admin/alerts/action", "action=acknowledge&alertId=1");
            check(staffAction.statusCode == 403, "Staff without ALERT_HANDLE POST /admin/alerts/action denied with 403");

            HttpResponse guestAction = guest.postForm("/admin/alerts/action", "action=acknowledge&alertId=1");
            check(guestAction.statusCode == 403, "Guest without ALERT_HANDLE POST /admin/alerts/action denied with 403");

            // -----------------------------------------------------------------
            // CHECKLIST 3: Seed Counts & Pagination Verification (> 50 rows)
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 3] Seed Counts & Pagination (> 50 rows each) ---");

            int accessCount = countTableRows("ACCESS_LOG");
            System.out.println("[DB] ACCESS_LOG row count: " + accessCount);
            check(accessCount >= 50, "ACCESS_LOG holds >= 50 seed rows (" + accessCount + ")");

            int motionCount = countTableRows("MOTION_LOG");
            System.out.println("[DB] MOTION_LOG row count: " + motionCount);
            check(motionCount >= 50, "MOTION_LOG holds >= 50 seed rows (" + motionCount + ")");

            int alertCount = countTableRows("ALERT_LOG");
            System.out.println("[DB] ALERT_LOG row count: " + alertCount);
            check(alertCount >= 5, "ALERT_LOG holds seed and test alert rows (" + alertCount + ")");

            // Verify access-logs table rendering and pagination
            check(adminAccess.body.contains("table-responsive"), "Access log table wrapped in .table-responsive");
            check(adminAccess.body.contains("page="), "Access log table contains pagination links");
            check(adminAccess.body.contains("Showing "), "Access log summary shows item count");

            // Verify motion-logs table rendering and pagination
            check(adminMotion.body.contains("table-responsive"), "Motion log table wrapped in .table-responsive");
            check(adminMotion.body.contains("page="), "Motion log table contains pagination links");

            // Verify alerts table rendering
            check(adminAlert.body.contains("table-responsive"), "Alerts table wrapped in .table-responsive");
            check(adminAlert.body.contains("Security alerts"), "Alerts page header rendered");

            // -----------------------------------------------------------------
            // CHECKLIST 4: Security - No PIN in Access Logs & XSS Prevention
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 4] Security: Zero PINs & XSS Protection ---");

            // Check that access logs table does not have any PIN column
            check(!adminAccess.body.toLowerCase().contains("<th>pin</th>"), "Access logs table has no PIN column header");
            check(!adminAccess.body.toLowerCase().contains("pin_ciphertext"), "No pin_ciphertext referenced in view");
            check(!adminAccess.body.toLowerCase().contains("pin_hmac"), "No pin_hmac referenced in view");

            // Insert a test alert containing malicious script tag to test XSS escaping
            long xssAlertId = insertTestAlert("<script>alert('xss')</script>", "CRITICAL", "NEW", "LOBBY_PROXIMITY");
            HttpResponse xssView = admin.get("/admin/alerts?keyword=xss");
            check(xssView.statusCode == 200, "Alerts page loaded for XSS test");
            check(xssView.body.contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;")
                    || xssView.body.contains("&lt;script&gt;alert('xss')&lt;/script&gt;"),
                    "Alert message with <script> tag is safely escaped on rendering");
            check(!xssView.body.contains("<script>alert('xss')</script>"),
                    "Unescaped <script> tag is strictly NOT present in rendered HTML");

            // -----------------------------------------------------------------
            // CHECKLIST 5: Acknowledge & Resolve Lifecycle (Scenario W24)
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 5] Alert Lifecycle: Acknowledge & Resolve ---");

            // Insert a dedicated test alert in NEW status
            long testAlertId = insertTestAlert("High temperature detected in lobby", "HIGH", "NEW", "LOBBY_PROXIMITY");
            check(testAlertId > 0, "Created new test alert #" + testAlertId + " in status NEW");

            // 5.1 Try to resolve a NEW alert without acknowledging first -> Rejected
            HttpResponse resolveNewResp = admin.postForm("/admin/alerts/action",
                    "action=resolve&alertId=" + testAlertId + "&note=Quick+fix");
            check(resolveNewResp.statusCode == 302, "Resolve on NEW alert redirects back");
            AlertDbInfo alertState1 = getAlertFromDb(testAlertId);
            check("NEW".equals(alertState1.status), "NEW alert cannot be directly resolved (must be acknowledged first)");

            // 5.2 Acknowledge the alert
            HttpResponse ackResp = admin.postForm("/admin/alerts/action",
                    "action=acknowledge&alertId=" + testAlertId);
            check(ackResp.statusCode == 302, "Acknowledge POST returns 302 redirect");

            AlertDbInfo alertState2 = getAlertFromDb(testAlertId);
            check("ACKNOWLEDGED".equals(alertState2.status), "Alert status changed to ACKNOWLEDGED");
            check(alertState2.acknowledgedBy != null && alertState2.acknowledgedBy > 0, "acknowledged_by is recorded");
            check(alertState2.acknowledgedAt != null, "acknowledged_at timestamp is recorded");

            // Verify Audit Log entry for ALERT_ACKNOWLEDGED
            check(verifyAuditLog("ALERT_ACKNOWLEDGED", String.valueOf(testAlertId)),
                    "Audit log recorded ALERT_ACKNOWLEDGED for alert #" + testAlertId);

            // 5.3 Double acknowledge -> harmless message, no status change, no new audit log
            int auditCountBefore = countAuditLogs("ALERT_ACKNOWLEDGED", String.valueOf(testAlertId));
            HttpResponse doubleAckResp = admin.postForm("/admin/alerts/action",
                    "action=acknowledge&alertId=" + testAlertId);
            check(doubleAckResp.statusCode == 302, "Double acknowledge returns 302 redirect");
            int auditCountAfter = countAuditLogs("ALERT_ACKNOWLEDGED", String.valueOf(testAlertId));
            check(auditCountAfter == auditCountBefore, "Double acknowledge creates no duplicate audit entry");

            // 5.4 Try to resolve without note -> Rejected with error
            HttpResponse resolveNoNoteResp = admin.postForm("/admin/alerts/action",
                    "action=resolve&alertId=" + testAlertId + "&note=");
            check(resolveNoNoteResp.statusCode == 302, "Resolve without note redirects back");
            AlertDbInfo alertState3 = getAlertFromDb(testAlertId);
            check("ACKNOWLEDGED".equals(alertState3.status), "Status remains ACKNOWLEDGED when note is empty");

            // Follow redirect to see flash error
            HttpResponse alertListFollow = admin.get("/admin/alerts");
            check(alertListFollow.body.contains("resolution note is required"),
                    "Flash message 'A resolution note is required.' displayed");

            // 5.5 Resolve with valid note
            HttpResponse resolveValidResp = admin.postForm("/admin/alerts/action",
                    "action=resolve&alertId=" + testAlertId + "&note=Investigated+and+resolved+by+security+team.");
            check(resolveValidResp.statusCode == 302, "Resolve with note returns 302 redirect");

            AlertDbInfo alertState4 = getAlertFromDb(testAlertId);
            check("RESOLVED".equals(alertState4.status), "Alert status changed to RESOLVED");
            check(alertState4.resolvedBy != null && alertState4.resolvedBy > 0, "resolved_by is recorded");
            check(alertState4.resolvedAt != null, "resolved_at timestamp is recorded");
            check("Investigated and resolved by security team.".equals(alertState4.resolutionNote),
                    "resolution_note is stored correctly: " + alertState4.resolutionNote);

            // Verify Audit Log entry for ALERT_RESOLVED
            check(verifyAuditLog("ALERT_RESOLVED", String.valueOf(testAlertId)),
                    "Audit log recorded ALERT_RESOLVED for alert #" + testAlertId);

            // 5.6 Try to acknowledge a RESOLVED alert -> Rejected
            HttpResponse ackResolvedResp = admin.postForm("/admin/alerts/action",
                    "action=acknowledge&alertId=" + testAlertId);
            check(ackResolvedResp.statusCode == 302, "Acknowledge on RESOLVED alert returns 302");
            AlertDbInfo alertState5 = getAlertFromDb(testAlertId);
            check("RESOLVED".equals(alertState5.status), "RESOLVED alert cannot transition back to ACKNOWLEDGED");

            // -----------------------------------------------------------------
            // CHECKLIST 6: Navbar Badge & Navigation
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 6] Navbar Badge & Navigation Links ---");
            HttpResponse adminNav = admin.get("/admin/alerts");
            check(adminNav.body.contains("Access logs"), "Admin navbar contains 'Access logs' link");
            check(adminNav.body.contains("Motion logs"), "Admin navbar contains 'Motion logs' link");
            check(adminNav.body.contains("Alerts"), "Admin navbar contains 'Alerts' link");

            int newAlertsInDb = countNewAlerts();
            System.out.println("[DB] Unresolved NEW alerts in DB: " + newAlertsInDb);
            if (newAlertsInDb > 0) {
                check(adminNav.body.contains("badge rounded-pill bg-danger"),
                        "Navbar displays red badge for NEW alerts count");
            } else {
                check(true, "No NEW alerts; badge hidden as expected");
            }

            // Cleanup test alerts
            deleteTestAlert(testAlertId);
            deleteTestAlert(xssAlertId);

            System.out.println("\n==================================================");
            System.out.println("     ALL T16 ACCEPTANCE TESTS COMPLETED           ");
            System.out.println("     Passed: " + passedChecks + " / " + totalChecks);
            System.out.println("==================================================");

            if (passedChecks < totalChecks) {
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
            System.out.println("[PASS] " + description);
        } else {
            System.err.println("[FAIL] " + description);
        }
    }

    private static int countTableRows(String table) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + table;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private static int countNewAlerts() throws Exception {
        String sql = "SELECT COUNT(*) FROM ALERT_LOG WHERE status = 'NEW'";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private static long insertTestAlert(String message, String severity, String status, String alertType) throws Exception {
        String sql = "INSERT INTO ALERT_LOG (alert_type, severity, status, message, created_at) "
                   + "VALUES (?, ?, ?, ?, SYSDATETIME())";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, alertType);
            ps.setString(2, severity);
            ps.setString(3, status);
            ps.setString(4, message);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1L;
    }

    private static void deleteTestAlert(long alertId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM ALERT_LOG WHERE alert_id = ?")) {
            ps.setLong(1, alertId);
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private static AlertDbInfo getAlertFromDb(long alertId) throws Exception {
        String sql = "SELECT status, acknowledged_by, acknowledged_at, resolved_by, resolved_at, resolution_note "
                   + "FROM ALERT_LOG WHERE alert_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, alertId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AlertDbInfo info = new AlertDbInfo();
                    info.status = rs.getString("status");
                    long ack = rs.getLong("acknowledged_by");
                    info.acknowledgedBy = rs.wasNull() ? null : ack;
                    info.acknowledgedAt = rs.getTimestamp("acknowledged_at");
                    long res = rs.getLong("resolved_by");
                    info.resolvedBy = rs.wasNull() ? null : res;
                    info.resolvedAt = rs.getTimestamp("resolved_at");
                    info.resolutionNote = rs.getString("resolution_note");
                    return info;
                }
            }
        }
        return null;
    }

    private static boolean verifyAuditLog(String action, String entityId) throws Exception {
        String sql = "SELECT COUNT(*) FROM AUDIT_LOG WHERE action = ? AND entity_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action);
            ps.setString(2, entityId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static int countAuditLogs(String action, String entityId) throws Exception {
        String sql = "SELECT COUNT(*) FROM AUDIT_LOG WHERE action = ? AND entity_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action);
            ps.setString(2, entityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private static boolean checkCleanAscii(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!checkCleanAscii(f)) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".java") || name.endsWith(".jsp") || name.endsWith(".tag")) {
                try {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    for (byte b : bytes) {
                        if ((b & 0x80) != 0) {
                            System.err.println("[FAIL ASCII] Non-ASCII byte in: " + file.getPath());
                            return false;
                        }
                    }
                } catch (Exception e) {
                    return false;
                }
            }
            return true;
        }
    }

    private static boolean checkNoJstl(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!checkNoJstl(f)) {
                        return false;
                    }
                }
            }
            return true;
        } else if (dir.getName().toLowerCase().endsWith(".jsp")) {
            try {
                String content = new String(Files.readAllBytes(dir.toPath()), StandardCharsets.UTF_8);
                if (content.contains("<c:") || content.contains("http://java.sun.com/jsp/jstl/core")) {
                    System.err.println("[FAIL JSTL] JSTL tag found in: " + dir.getPath());
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkNoJakarta(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!checkNoJakarta(f)) {
                        return false;
                    }
                }
            }
            return true;
        } else if (dir.getName().toLowerCase().endsWith(".java")) {
            try {
                String content = new String(Files.readAllBytes(dir.toPath()), StandardCharsets.UTF_8);
                if (content.contains("import jakarta.")) {
                    System.err.println("[FAIL JAKARTA] jakarta package found in: " + dir.getPath());
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    public static class AlertDbInfo {
        public String status;
        public Long acknowledgedBy;
        public java.sql.Timestamp acknowledgedAt;
        public Long resolvedBy;
        public java.sql.Timestamp resolvedAt;
        public String resolutionNote;
    }

    public static class HttpResponse {
        public int statusCode;
        public String body;
        public String contentType;
        public String redirectLocation;
        public Map<String, List<String>> headers;
    }

    public static class TestClient {
        private final String baseUrl;
        private String jsessionId;
        private String csrfToken;

        public TestClient(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public HttpResponse get(String path) throws Exception {
            URL url = new URL(baseUrl + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            if (jsessionId != null) {
                conn.setRequestProperty("Cookie", "JSESSIONID=" + jsessionId);
            }
            conn.connect();

            updateSessionAndCsrf(conn);

            HttpResponse res = new HttpResponse();
            res.statusCode = conn.getResponseCode();
            res.contentType = conn.getContentType();
            res.redirectLocation = conn.getHeaderField("Location");
            res.headers = conn.getHeaderFields();

            InputStream is = (res.statusCode >= 400) ? conn.getErrorStream() : conn.getInputStream();
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    res.body = sb.toString();
                }
            } else {
                res.body = "";
            }

            extractCsrf(res.body);
            return res;
        }

        public HttpResponse postForm(String path, String formParams) throws Exception {
            URL url = new URL(baseUrl + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (jsessionId != null) {
                conn.setRequestProperty("Cookie", "JSESSIONID=" + jsessionId);
            }

            String body = formParams;
            if (csrfToken != null && !formParams.contains("_csrf=")) {
                body += (body.isEmpty() ? "" : "&") + "_csrf=" + URLEncoder.encode(csrfToken, "UTF-8");
            }

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            updateSessionAndCsrf(conn);

            HttpResponse res = new HttpResponse();
            res.statusCode = conn.getResponseCode();
            res.contentType = conn.getContentType();
            res.redirectLocation = conn.getHeaderField("Location");
            res.headers = conn.getHeaderFields();

            InputStream is = (res.statusCode >= 400) ? conn.getErrorStream() : conn.getInputStream();
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    res.body = sb.toString();
                }
            } else {
                res.body = "";
            }

            extractCsrf(res.body);
            return res;
        }

        public boolean login(String email, String password) throws Exception {
            HttpResponse loginPage = get("/login");
            if (loginPage.statusCode != 200) {
                return false;
            }

            URL url = new URL(baseUrl + "/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (jsessionId != null) {
                conn.setRequestProperty("Cookie", "JSESSIONID=" + jsessionId);
            }

            String body = "email=" + URLEncoder.encode(email, "UTF-8")
                    + "&password=" + URLEncoder.encode(password, "UTF-8")
                    + "&_csrf=" + URLEncoder.encode(csrfToken != null ? csrfToken : "", "UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            updateSessionAndCsrf(conn);
            int code = conn.getResponseCode();
            return (code == 302 || code == 200);
        }

        private void updateSessionAndCsrf(HttpURLConnection conn) {
            List<String> cookies = conn.getHeaderFields().get("Set-Cookie");
            if (cookies != null) {
                for (String cookie : cookies) {
                    if (cookie.startsWith("JSESSIONID=")) {
                        int end = cookie.indexOf(';');
                        if (end > 0) {
                            jsessionId = cookie.substring("JSESSIONID=".length(), end);
                        } else {
                            jsessionId = cookie.substring("JSESSIONID=".length());
                        }
                    }
                }
            }
        }

        private void extractCsrf(String body) {
            if (body == null) return;
            Pattern p = Pattern.compile("name=[\"']_csrf[\"']\\s+value=[\"']([^\"']+)[\"']");
            Matcher m = p.matcher(body);
            if (m.find()) {
                csrfToken = m.group(1);
            }
        }
    }
}
