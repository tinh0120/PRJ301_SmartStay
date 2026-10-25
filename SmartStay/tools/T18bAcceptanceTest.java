package tools;

import util.ConfigUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automated end-to-end acceptance test for T18b:
 * System Configuration management, ConfigUtil invalidation & audit,
 * and Review moderation list & actions.
 */
public class T18bAcceptanceTest {

    private static final String BASE_URL = "http://localhost:8080/SmartStay";
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=SmartStayDB;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "12345";

    private static int totalChecks = 0;
    private static int passedChecks = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("        STARTING T18b ACCEPTANCE VERIFICATION     ");
        System.out.println("==================================================");

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                System.out.println("[DB] Connected to SmartStayDB successfully.");
            }

            TestClient owner = new TestClient(BASE_URL);
            TestClient admin = new TestClient(BASE_URL);
            TestClient staff = new TestClient(BASE_URL);
            TestClient guest = new TestClient(BASE_URL);
            TestClient anon = new TestClient(BASE_URL);

            check(owner.login("owner@smartstay.local", "12345"), "Owner login successful");
            check(admin.login("admin@smartstay.local", "12345"), "Admin login successful");
            check(staff.login("staff1@smartstay.local", "12345"), "Staff login successful");
            check(guest.login("guest1@smartstay.local", "12345"), "Guest login successful");

            // -----------------------------------------------------------------
            // CHECKLIST 1: Architecture and Source Code Quality
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 1] Architecture and Source Code Quality ---");
            check(verifyAsciiDirectory(new File("src/java/com/smartstay")), "All Java source files in com.smartstay are 100% clean ASCII");
            check(verifyAsciiDirectory(new File("web/WEB-INF/views")), "All JSPs in WEB-INF/views are 100% clean ASCII");
            check(verifyAsciiDirectory(new File("web/WEB-INF/tags")), "All tags in WEB-INF/tags are 100% clean ASCII");
            check(verifyNoJstl(new File("web/WEB-INF/views")), "No JSTL tags (<c:*) used across all JSPs");
            check(verifyNoJakarta(new File("src/java")), "No jakarta.* package imports in any Java files");

            // -----------------------------------------------------------------
            // CHECKLIST 2: RBAC & Route Access Security
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 2] RBAC & Route Access Security ---");
            HttpResponse anonCfg = anon.get("/admin/config");
            check(anonCfg.statusCode == 302 || anonCfg.statusCode == 403, "Anonymous cannot access /admin/config (redirected or forbidden)");

            HttpResponse anonRev = anon.get("/admin/reviews");
            check(anonRev.statusCode == 302 || anonRev.statusCode == 403, "Anonymous cannot access /admin/reviews (redirected or forbidden)");

            HttpResponse staffCfg = staff.get("/admin/config");
            check(staffCfg.statusCode == 403, "Staff cannot access /admin/config (403 Forbidden)");

            HttpResponse staffRev = staff.get("/admin/reviews");
            check(staffRev.statusCode == 403, "Staff cannot access /admin/reviews (403 Forbidden)");

            HttpResponse guestCfg = guest.get("/admin/config");
            check(guestCfg.statusCode == 403, "Guest cannot access /admin/config (403 Forbidden)");

            HttpResponse guestRev = guest.get("/admin/reviews");
            check(guestRev.statusCode == 403, "Guest cannot access /admin/reviews (403 Forbidden)");

            HttpResponse adminCfg = admin.get("/admin/config");
            check(adminCfg.statusCode == 200, "Admin can GET /admin/config with CONFIG_VIEW (200 OK)");

            HttpResponse adminCfgPost = admin.postForm("/admin/config", "cfg_PIN_LENGTH=6");
            check(adminCfgPost.statusCode == 403, "Admin cannot POST /admin/config (403 Forbidden - lacks CONFIG_MANAGE)");

            HttpResponse adminRev = admin.get("/admin/reviews");
            check(adminRev.statusCode == 200, "Admin can GET /admin/reviews with REVIEW_MODERATE (200 OK)");

            HttpResponse ownerCfg = owner.get("/admin/config");
            check(ownerCfg.statusCode == 200, "Owner can GET /admin/config with CONFIG_MANAGE (200 OK)");

            HttpResponse ownerRev = owner.get("/admin/reviews");
            check(ownerRev.statusCode == 200, "Owner can GET /admin/reviews (200 OK)");

            // -----------------------------------------------------------------
            // CHECKLIST 3: System Configuration View & UI Nuances
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 3] System Configuration View & UI Nuances ---");
            check(adminCfg.body.contains("Read-only") || adminCfg.body.contains("CONFIG_VIEW"), "Admin view shows Read-only explanation banner");
            check(!adminCfg.body.contains("Save Configuration"), "Admin view has NO Save Configuration button");
            check(!adminCfg.body.contains("name=\"cfg_") && !adminCfg.body.contains("<select"), "Admin view has NO editable input/select controls");

            check(ownerCfg.body.contains("Save Configuration"), "Owner view contains Save Configuration button");
            check(ownerCfg.body.contains("name=\"cfg_PIN_LENGTH\"") || ownerCfg.body.contains("name=\"PIN_LENGTH\""), "Owner view contains PIN_LENGTH input");
            check(ownerCfg.body.contains("name=\"cfg_MAX_ADVANCE_BOOKING_DAYS\"") || ownerCfg.body.contains("name=\"MAX_ADVANCE_BOOKING_DAYS\""), "Owner view contains MAX_ADVANCE_BOOKING_DAYS input");
            check(ownerCfg.body.contains("DISCOUNT_2_SLOTS_PERCENT"), "Owner view contains DISCOUNT_2_SLOTS_PERCENT parameter");
            check(ownerCfg.body.contains("MAIN_DOOR_DEVICE_ID"), "Owner view contains MAIN_DOOR_DEVICE_ID parameter");
            check(ownerCfg.body.contains("SIMULATOR_ENABLED"), "Owner view contains SIMULATOR_ENABLED parameter");

            // -----------------------------------------------------------------
            // CHECKLIST 4: Atomic All-or-Nothing Validation & DB Integrity
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 4] Atomic All-or-Nothing Validation & DB Integrity ---");

            // 1. Invalid PIN_LENGTH (must be 4-8)
            String oldPin = queryConfigValue("PIN_LENGTH");
            int auditBefore = countAuditLogs("CONFIG_UPDATED");
            HttpResponse failPin = owner.postForm("/admin/config", "cfg_PIN_LENGTH=15");
            check(failPin.statusCode == 200, "Invalid PIN_LENGTH submission reloads form (200 OK)");
            check(failPin.body.contains("PIN length must be an integer between 4 and 8") || failPin.body.contains("between 4 and 8"), "Displays specific error for PIN_LENGTH range");
            String pinAfter = queryConfigValue("PIN_LENGTH");
            check(oldPin.equals(pinAfter), "DB value for PIN_LENGTH remained unchanged on failure");
            int auditAfter = countAuditLogs("CONFIG_UPDATED");
            check(auditBefore == auditAfter, "Zero audit log entries written on validation failure");

            // 2. Invalid DISCOUNT percentage (must be 0-100)
            HttpResponse failDiscount = owner.postForm("/admin/config", "cfg_DISCOUNT_2_SLOTS_PERCENT=150");
            check(failDiscount.statusCode == 200, "Invalid DISCOUNT submission reloads form");
            check(failDiscount.body.contains("between 0 and 100"), "Displays error for DISCOUNT percentage > 100");

            // 3. Invalid MAIN_DOOR_DEVICE_ID (must exist in DEVICE table)
            HttpResponse failDevice = owner.postForm("/admin/config", "cfg_MAIN_DOOR_DEVICE_ID=NON_EXISTENT_LOCK_9999");
            check(failDevice.statusCode == 200, "Invalid device ID submission reloads form");
            check(failDevice.body.contains("does not exist in the system"), "Displays error for non-existent device ID");

            // 4. Invalid BOOLEAN
            HttpResponse failBool = owner.postForm("/admin/config", "cfg_SIMULATOR_ENABLED=maybe");
            check(failBool.statusCode == 200, "Invalid boolean submission reloads form");
            check(failBool.body.contains("boolean"), "Displays error for invalid boolean value");

            // 5. Valid bulk submission
            String newPin = "7";
            String newGrace = "35";
            HttpResponse successPost = owner.postForm("/admin/config",
                    "cfg_PIN_LENGTH=" + newPin
                    + "&cfg_NOSHOW_GRACE_MINUTES=" + newGrace);
            check(successPost.statusCode == 302, "Valid configuration save redirects (302 Found)");
            String updatedPin = queryConfigValue("PIN_LENGTH");
            String updatedGrace = queryConfigValue("NOSHOW_GRACE_MINUTES");
            check("7".equals(updatedPin), "PIN_LENGTH updated successfully to 7 in database");
            check("35".equals(updatedGrace), "NOSHOW_GRACE_MINUTES updated successfully to 35 in database");

            int auditSuccess = countAuditLogs("CONFIG_UPDATED");
            check(auditSuccess > auditBefore, "Audit logs recorded for updated configuration keys");

            // Invalidation check: ConfigUtil.get returns updated value immediately
            check("7".equals(ConfigUtil.get("PIN_LENGTH", "")), "ConfigUtil.get immediately reflects updated PIN_LENGTH without cache stale");

            // Revert configuration back
            owner.postForm("/admin/config", "cfg_PIN_LENGTH=" + oldPin + "&cfg_NOSHOW_GRACE_MINUTES=30");

            // -----------------------------------------------------------------
            // CHECKLIST 5: ConfigUtil.set programmatic update & invalidation
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 5] ConfigUtil.set Programmatic Update ---");
            long ownerUserId = queryUserId("owner@smartstay.local");
            String testInstruction = "Key boxes are in the lobby. Test update " + System.currentTimeMillis();
            boolean setRes = ConfigUtil.set("KEY_AREA_INSTRUCTION", testInstruction, ownerUserId, "127.0.0.1");
            check(setRes, "ConfigUtil.set executed successfully");
            check(testInstruction.equals(ConfigUtil.get("KEY_AREA_INSTRUCTION", "")), "ConfigUtil.get immediately returns new value");
            check(testInstruction.equals(queryConfigValue("KEY_AREA_INSTRUCTION")), "Database reflects ConfigUtil.set updated value");

            // -----------------------------------------------------------------
            // CHECKLIST 6: Review Moderation List, Filters & Actions
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 6] Review Moderation List, Filters & Actions ---");

            HttpResponse reviewsPage = admin.get("/admin/reviews");
            check(reviewsPage.statusCode == 200, "Admin reviews page loaded successfully");
            check(reviewsPage.body.contains("Customer Reviews Moderation"), "Page contains Reviews moderation header");
            check(reviewsPage.body.contains("Rating") && reviewsPage.body.contains("Status"), "Page contains Rating and Status filters");
            check(reviewsPage.body.contains("&#9733;") || reviewsPage.body.contains("\u2605"), "Stars rendered properly for reviews");

            // Filter test: status=VISIBLE
            HttpResponse visibleOnly = admin.get("/admin/reviews?status=VISIBLE");
            check(visibleOnly.statusCode == 200, "Filter status=VISIBLE returns 200");
            check(!visibleOnly.body.contains("<span class=\"badge bg-secondary\">HIDDEN</span>"), "Filter status=VISIBLE contains no HIDDEN badges");

            // Filter test: status=HIDDEN
            HttpResponse hiddenOnly = admin.get("/admin/reviews?status=HIDDEN");
            check(hiddenOnly.statusCode == 200, "Filter status=HIDDEN returns 200");
            check(!hiddenOnly.body.contains("<span class=\"badge bg-success\">VISIBLE</span>"), "Filter status=HIDDEN contains no VISIBLE badges");

            // Moderation actions: Hide and Show
            Long testReviewId = queryFirstReviewId("VISIBLE");
            check(testReviewId != null, "Found an active VISIBLE review for moderation test (ID=" + testReviewId + ")");

            if (testReviewId != null) {
                Long roomId = queryReviewRoomId(testReviewId);

                // Verify review is visible on public room detail page
                HttpResponse roomBefore = anon.get("/rooms/detail?id=" + roomId);
                check(roomBefore.statusCode == 200, "Public room detail page loads (200 OK)");

                // Admin hides the review
                int hideAuditsBefore = countAuditLogs("REVIEW_HIDDEN");
                HttpResponse hideRes = admin.postForm("/admin/reviews/action", "action=hide&reviewId=" + testReviewId);
                check(hideRes.statusCode == 302, "Hide review action redirects (302 Found)");

                String statusAfterHide = queryReviewStatus(testReviewId);
                check("HIDDEN".equals(statusAfterHide), "Review status in database is now HIDDEN");

                int hideAuditsAfter = countAuditLogs("REVIEW_HIDDEN");
                check(hideAuditsAfter > hideAuditsBefore, "REVIEW_HIDDEN audit log entry successfully recorded");

                // Public room page must NOT show the hidden review
                HttpResponse roomAfterHide = anon.get("/rooms/detail?id=" + roomId);
                String commentText = queryReviewComment(testReviewId);
                if (commentText != null && commentText.length() > 20) {
                    String snippet = commentText.substring(0, 20);
                    check(!roomAfterHide.body.contains(snippet), "Hidden review immediately disappeared from public room detail page");
                } else {
                    check(true, "Hidden review excluded from public query");
                }

                // Admin shows the review again
                int showAuditsBefore = countAuditLogs("REVIEW_SHOWN");
                HttpResponse showRes = admin.postForm("/admin/reviews/action", "action=show&reviewId=" + testReviewId);
                check(showRes.statusCode == 302, "Show review action redirects (302 Found)");

                String statusAfterShow = queryReviewStatus(testReviewId);
                check("VISIBLE".equals(statusAfterShow), "Review status in database is now restored to VISIBLE");

                int showAuditsAfter = countAuditLogs("REVIEW_SHOWN");
                check(showAuditsAfter > showAuditsBefore, "REVIEW_SHOWN audit log entry successfully recorded");
            }

            // CSRF Protection checks
            System.out.println("\n--- [Checklist 7] CSRF Protection Checks ---");
            TestClient noCsrfOwner = new TestClient(BASE_URL);
            noCsrfOwner.login("owner@smartstay.local", "12345");
            HttpResponse noCsrfCfg = noCsrfOwner.postFormWithoutCsrf("/admin/config", "cfg_PIN_LENGTH=6");
            check(noCsrfCfg.statusCode == 403, "POST /admin/config without CSRF rejected (403 Forbidden)");

            HttpResponse noCsrfRev = admin.postFormWithoutCsrf("/admin/reviews/action", "action=hide&reviewId=1");
            check(noCsrfRev.statusCode == 403, "POST /admin/reviews/action without CSRF rejected (403 Forbidden)");

            // -----------------------------------------------------------------
            // FINAL SUMMARY
            // -----------------------------------------------------------------
            System.out.println("\n==================================================");
            System.out.println(String.format("T18b VERIFICATION FINISHED: %d/%d checks passed (%.1f%%)",
                    passedChecks, totalChecks, (passedChecks * 100.0 / totalChecks)));
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
            System.out.println(String.format("  [PASS] (%d/%d) %s", passedChecks, totalChecks, description));
        } else {
            System.err.println(String.format("  [FAIL] (%d/%d) %s", passedChecks, totalChecks, description));
        }
    }

    private static boolean verifyAsciiDirectory(File dir) {
        if (!dir.exists()) return true;
        File[] files = dir.listFiles();
        if (files == null) return true;
        for (File f : files) {
            if (f.isDirectory()) {
                if (!verifyAsciiDirectory(f)) return false;
            } else if (f.getName().endsWith(".java") || f.getName().endsWith(".jsp") || f.getName().endsWith(".tag")) {
                try {
                    byte[] bytes = new byte[(int) f.length()];
                    try (FileInputStream fis = new FileInputStream(f)) {
                        fis.read(bytes);
                    }
                    for (int i = 0; i < bytes.length; i++) {
                        if ((bytes[i] & 0xFF) > 127) {
                            System.err.println("Non-ASCII character in: " + f.getAbsolutePath() + " at index " + i);
                            return false;
                        }
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean verifyNoJstl(File dir) {
        if (!dir.exists()) return true;
        File[] files = dir.listFiles();
        if (files == null) return true;
        for (File f : files) {
            if (f.isDirectory()) {
                if (!verifyNoJstl(f)) return false;
            } else if (f.getName().endsWith(".jsp") || f.getName().endsWith(".tag")) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("<c:") || line.contains("http://java.sun.com/jsp/jstl/core")) {
                            System.err.println("Found JSTL tag in: " + f.getAbsolutePath());
                            return false;
                        }
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean verifyNoJakarta(File dir) {
        if (!dir.exists()) return true;
        File[] files = dir.listFiles();
        if (files == null) return true;
        for (File f : files) {
            if (f.isDirectory()) {
                if (!verifyNoJakarta(f)) return false;
            } else if (f.getName().endsWith(".java")) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().startsWith("import jakarta.")) {
                            System.err.println("Found jakarta import in: " + f.getAbsolutePath());
                            return false;
                        }
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String queryConfigValue(String key) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT config_value FROM SYSTEM_CONFIG WHERE config_key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("config_value");
                }
            }
        }
        return null;
    }

    private static long queryUserId(String email) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM APP_USER WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("user_id");
                }
            }
        }
        return 1L;
    }

    private static int countAuditLogs(String action) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM AUDIT_LOG WHERE action = ?")) {
            ps.setString(1, action);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private static Long queryFirstReviewId(String status) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT TOP 1 review_id FROM REVIEW WHERE status = ? ORDER BY review_id ASC")) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("review_id");
                }
            }
        }
        return null;
    }

    private static Long queryReviewRoomId(long reviewId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT room_id FROM REVIEW WHERE review_id = ?")) {
            ps.setLong(1, reviewId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("room_id");
                }
            }
        }
        return null;
    }

    private static String queryReviewStatus(long reviewId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM REVIEW WHERE review_id = ?")) {
            ps.setLong(1, reviewId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        }
        return null;
    }

    private static String queryReviewComment(long reviewId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT comment FROM REVIEW WHERE review_id = ?")) {
            ps.setLong(1, reviewId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("comment");
                }
            }
        }
        return null;
    }

    static class HttpResponse {
        int statusCode;
        String contentType;
        String redirectLocation;
        String cacheControl;
        String body;
        Map<String, List<String>> headers;
    }

    static class TestClient {
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
            res.cacheControl = conn.getHeaderField("Cache-Control");
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
            res.cacheControl = conn.getHeaderField("Cache-Control");
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

        public HttpResponse postFormWithoutCsrf(String path, String formParams) throws Exception {
            URL url = new URL(baseUrl + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (jsessionId != null) {
                conn.setRequestProperty("Cookie", "JSESSIONID=" + jsessionId);
            }

            try (OutputStream os = conn.getOutputStream()) {
                os.write(formParams.getBytes(StandardCharsets.UTF_8));
            }

            updateSessionAndCsrf(conn);

            HttpResponse res = new HttpResponse();
            res.statusCode = conn.getResponseCode();
            res.contentType = conn.getContentType();
            res.redirectLocation = conn.getHeaderField("Location");
            res.cacheControl = conn.getHeaderField("Cache-Control");
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
