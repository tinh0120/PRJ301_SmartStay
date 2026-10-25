package tools;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automated end-to-end acceptance test for T17:
 * Operational Dashboard, Revenue Reports, and Chart.js telemetry endpoints.
 */
public class T17AcceptanceTest {

    private static final String BASE_URL = "http://localhost:8080/SmartStay";
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=SmartStayDB;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "12345";

    private static int totalChecks = 0;
    private static int passedChecks = 0;
    private static final Gson GSON = new Gson();

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("        STARTING T17 ACCEPTANCE VERIFICATION      ");
        System.out.println("==================================================");

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                System.out.println("[DB] Connected to SmartStayDB successfully.");
            }

            TestClient admin = new TestClient(BASE_URL);
            TestClient staff = new TestClient(BASE_URL);
            TestClient guest = new TestClient(BASE_URL);
            TestClient anon = new TestClient(BASE_URL);

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

            // /admin/dashboard
            HttpResponse anonDash = anon.get("/admin/dashboard");
            check(anonDash.statusCode == 302 && anonDash.redirectLocation != null && anonDash.redirectLocation.contains("/login"),
                    "Unauthenticated GET /admin/dashboard redirects to login");
            check(guest.get("/admin/dashboard").statusCode == 403, "Guest GET /admin/dashboard denied with 403 Forbidden");
            check(staff.get("/admin/dashboard").statusCode == 403, "Staff GET /admin/dashboard denied with 403 Forbidden");
            HttpResponse adminDash = admin.get("/admin/dashboard");
            check(adminDash.statusCode == 200, "Admin (with DASHBOARD_VIEW) GET /admin/dashboard returns 200 OK");

            // /admin/dashboard/charts
            HttpResponse anonDashCharts = anon.get("/admin/dashboard/charts");
            check(anonDashCharts.statusCode == 302, "Unauthenticated GET /admin/dashboard/charts redirects to login");
            check(guest.get("/admin/dashboard/charts").statusCode == 403, "Guest GET /admin/dashboard/charts denied with 403");
            check(staff.get("/admin/dashboard/charts").statusCode == 403, "Staff GET /admin/dashboard/charts denied with 403");
            HttpResponse adminDashCharts = admin.get("/admin/dashboard/charts");
            check(adminDashCharts.statusCode == 200, "Admin GET /admin/dashboard/charts returns 200 OK");

            // /admin/reports/revenue
            HttpResponse anonRev = anon.get("/admin/reports/revenue");
            check(anonRev.statusCode == 302, "Unauthenticated GET /admin/reports/revenue redirects to login");
            check(guest.get("/admin/reports/revenue").statusCode == 403, "Guest GET /admin/reports/revenue denied with 403");
            check(staff.get("/admin/reports/revenue").statusCode == 403, "Staff GET /admin/reports/revenue denied with 403");
            HttpResponse adminRev = admin.get("/admin/reports/revenue");
            check(adminRev.statusCode == 200, "Admin (with REPORT_REVENUE) GET /admin/reports/revenue returns 200 OK");

            // /admin/reports/revenue/data
            HttpResponse anonRevData = anon.get("/admin/reports/revenue/data");
            check(anonRevData.statusCode == 302, "Unauthenticated GET /admin/reports/revenue/data redirects to login");
            check(guest.get("/admin/reports/revenue/data").statusCode == 403, "Guest GET /admin/reports/revenue/data denied with 403");
            check(staff.get("/admin/reports/revenue/data").statusCode == 403, "Staff GET /admin/reports/revenue/data denied with 403");
            HttpResponse adminRevData = admin.get("/admin/reports/revenue/data");
            check(adminRevData.statusCode == 200, "Admin GET /admin/reports/revenue/data returns 200 OK");

            // -----------------------------------------------------------------
            // CHECKLIST 3: Dashboard Numbers Match Hand-run SQL
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 3] Dashboard Numbers Match Hand-run SQL ---");

            // Rooms by status
            Map<String, Integer> dbRooms = getRoomStatusCountsDb();
            check(adminDash.body.contains("Available: <strong>" + dbRooms.getOrDefault("AVAILABLE", 0) + "</strong>"),
                    "Dashboard renders exact AVAILABLE rooms count (" + dbRooms.getOrDefault("AVAILABLE", 0) + ")");
            check(adminDash.body.contains("Occupied: <strong>" + dbRooms.getOrDefault("OCCUPIED", 0) + "</strong>"),
                    "Dashboard renders exact OCCUPIED rooms count (" + dbRooms.getOrDefault("OCCUPIED", 0) + ")");
            check(adminDash.body.contains("Cleaning: <strong>" + dbRooms.getOrDefault("NEEDS_CLEANING", 0) + "</strong>"),
                    "Dashboard renders exact NEEDS_CLEANING rooms count (" + dbRooms.getOrDefault("NEEDS_CLEANING", 0) + ")");
            check(adminDash.body.contains("Maintenance: <strong>" + dbRooms.getOrDefault("MAINTENANCE", 0) + "</strong>"),
                    "Dashboard renders exact MAINTENANCE rooms count (" + dbRooms.getOrDefault("MAINTENANCE", 0) + ")");

            // Devices by status
            Map<String, Integer> dbDevices = getDeviceStatusCountsDb();
            check(adminDash.body.contains("Online: <strong>" + dbDevices.getOrDefault("ONLINE", 0) + "</strong>"),
                    "Dashboard renders exact ONLINE devices count (" + dbDevices.getOrDefault("ONLINE", 0) + ")");
            check(adminDash.body.contains("Offline: <strong>" + dbDevices.getOrDefault("OFFLINE", 0) + "</strong>"),
                    "Dashboard renders exact OFFLINE devices count (" + dbDevices.getOrDefault("OFFLINE", 0) + ")");
            check(adminDash.body.contains("Fault: <strong>" + dbDevices.getOrDefault("FAULT", 0) + "</strong>"),
                    "Dashboard renders exact FAULT devices count (" + dbDevices.getOrDefault("FAULT", 0) + ")");

            // Alerts NEW count
            int dbNewAlerts = getNewAlertCountDb();
            check(adminDash.body.contains(dbNewAlerts + " NEW"),
                    "Dashboard renders exact NEW alerts count (" + dbNewAlerts + ")");

            // No-show held count
            int dbNoshowHeld = getNoshowHeldCountDb();
            check(adminDash.body.contains(dbNoshowHeld + " <span class=\"fs-6 fw-normal text-muted\">held</span>"),
                    "Dashboard renders exact No-Show Held count (" + dbNoshowHeld + ")");
            check(adminDash.body.contains("flag=noshow_held") || adminDash.body.contains("noshow_held=1"),
                    "Dashboard links directly to bookings filtered by noshow_held");

            // 5 Latest alerts table
            check(adminDash.body.contains("Latest security alerts"), "Dashboard contains Latest security alerts card");
            check(adminDash.body.contains("table-responsive"), "Latest alerts table wrapped in .table-responsive");
            check(adminDash.body.contains("/admin/alerts"), "Dashboard contains link to /admin/alerts");

            // -----------------------------------------------------------------
            // CHECKLIST 4: Dashboard Charts JSON Endpoint & Caching
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 4] Dashboard Charts Telemetry & Contract ---");
            check(adminDashCharts.contentType != null && adminDashCharts.contentType.contains("application/json"),
                    "Charts endpoint returns Content-Type: application/json");
            check(adminDashCharts.cacheControl != null && adminDashCharts.cacheControl.contains("no-store"),
                    "Charts endpoint returns Cache-Control: no-store");

            JsonObject dashChartsJson = GSON.fromJson(adminDashCharts.body, JsonObject.class);
            check(dashChartsJson.has("revenue7"), "Charts payload contains revenue7 object");
            check(dashChartsJson.has("alertsByType"), "Charts payload contains alertsByType object");

            JsonObject rev7Obj = dashChartsJson.getAsJsonObject("revenue7");
            JsonArray rev7Labels = rev7Obj.getAsJsonArray("labels");
            JsonArray rev7Values = rev7Obj.getAsJsonArray("values");
            check(rev7Labels.size() == 7, "revenue7 contains exactly 7 labels for 7 days");
            check(rev7Values.size() == 7, "revenue7 contains exactly 7 values matching labels");

            long rev7Sum = 0L;
            for (JsonElement el : rev7Values) {
                rev7Sum += el.getAsLong();
            }
            long dbRev7Sum = getRevenueLast7DaysSumDb();
            check(rev7Sum == dbRev7Sum, "Sum of revenue7 (" + rev7Sum + ") matches database sum (" + dbRev7Sum + ")");

            JsonObject alertsObj = dashChartsJson.getAsJsonObject("alertsByType");
            JsonArray alertLabels = alertsObj.getAsJsonArray("labels");
            JsonArray alertValues = alertsObj.getAsJsonArray("values");
            check(alertLabels.size() > 0, "alertsByType contains alert type categories");
            check(alertLabels.size() == alertValues.size(), "alertsByType labels and values array sizes match");

            // Dashboard HTML contains Chart.js canvas and cache keys
            check(adminDash.body.contains("id=\"revenue7Chart\""), "Dashboard contains #revenue7Chart canvas");
            check(adminDash.body.contains("id=\"alertTypesChart\""), "Dashboard contains #alertTypesChart canvas");
            check(adminDash.body.contains("smartstay.chart.revenue7"), "Dashboard script registers cacheKey smartstay.chart.revenue7");
            check(adminDash.body.contains("smartstay.chart.alertTypes"), "Dashboard script registers cacheKey smartstay.chart.alertTypes");
            check(adminDash.body.contains("chart.umd.min.js"), "Dashboard includes local Chart.js bundle");
            check(adminDash.body.contains("chart-loader.js"), "Dashboard includes chart-loader.js");

            // -----------------------------------------------------------------
            // CHECKLIST 5: Revenue Report (Day, Month, Year & Totals)
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 5] Revenue Report: Range Table & Buckets ---");

            // 5.1 Default view (last 30 days)
            check(adminRev.body.contains("Financial revenue report"), "Revenue report page header rendered");
            check(adminRev.body.contains("Total revenue"), "Revenue report contains Total revenue KPI");
            check(adminRev.body.contains("Paid bookings"), "Revenue report contains Paid bookings KPI");
            check(adminRev.body.contains("table-responsive"), "Revenue table wrapped in .table-responsive");
            check(adminRev.body.contains("id=\"revenueChart\""), "Revenue page contains #revenueChart canvas");

            // 5.2 Month Mode across 60 days
            LocalDate today = LocalDate.now();
            String from60 = today.minusDays(60).toString();
            String toNow = today.toString();
            HttpResponse monthReportResp = admin.get("/admin/reports/revenue?mode=month&from=" + from60 + "&to=" + toNow);
            check(monthReportResp.statusCode == 200, "Month mode report returns 200 OK");
            check(monthReportResp.body.contains("selected>Month (Monthly)</option>"), "Month mode is selected in form");

            // Verify Month Data JSON endpoint
            HttpResponse monthDataResp = admin.get("/admin/reports/revenue/data?mode=month&from=" + from60 + "&to=" + toNow);
            check(monthDataResp.statusCode == 200, "GET /admin/reports/revenue/data (month) returns 200 OK");
            check(monthDataResp.cacheControl != null && monthDataResp.cacheControl.contains("no-store"),
                    "Revenue data endpoint returns Cache-Control: no-store");
            JsonObject monthJson = GSON.fromJson(monthDataResp.body, JsonObject.class);
            JsonArray monthLabels = monthJson.getAsJsonArray("labels");
            check(monthLabels.size() >= 2 && monthLabels.size() <= 4,
                    "60-day range produces 2-4 monthly buckets (" + monthLabels.size() + ")");

            long jsonTotalRev = monthJson.get("totalRevenue").getAsLong();
            int jsonTotalBookings = monthJson.get("totalBookings").getAsInt();
            long[] dbRev60 = getRevenueRangeDb(today.minusDays(60), today);
            check(jsonTotalRev == dbRev60[0], "JSON totalRevenue (" + jsonTotalRev + ") matches DB (" + dbRev60[0] + ")");
            check(jsonTotalBookings == (int) dbRev60[1], "JSON totalBookings (" + jsonTotalBookings + ") matches DB (" + dbRev60[1] + ")");

            // 5.3 Year Mode
            HttpResponse yearDataResp = admin.get("/admin/reports/revenue/data?mode=year&from=2025-01-01&to=2026-12-31");
            check(yearDataResp.statusCode == 200, "Year mode data returns 200 OK");
            JsonObject yearJson = GSON.fromJson(yearDataResp.body, JsonObject.class);
            JsonArray yearLabels = yearJson.getAsJsonArray("labels");
            check(yearLabels.size() == 2, "Year range 2025-2026 contains exactly 2 year buckets");

            // -----------------------------------------------------------------
            // CHECKLIST 6: Edge Cases & Input Validation
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 6] Validation & Edge Cases ---");

            // 6.1 from > to
            HttpResponse badRangeHtml = admin.get("/admin/reports/revenue?from=2026-10-15&to=2026-10-01");
            check(badRangeHtml.statusCode == 200, "from > to returns 200 with form error");
            check(badRangeHtml.body.contains("must be before or equal to"),
                    "Validation error 'From date must be before or equal to to date' displayed");

            // 6.2 Invalid mode fallback (mode=hour)
            HttpResponse badModeResp = admin.get("/admin/reports/revenue/data?mode=hour");
            check(badModeResp.statusCode == 200, "Crafted mode=hour handled safely");
            JsonObject badModeJson = GSON.fromJson(badModeResp.body, JsonObject.class);
            check("day".equals(badModeJson.get("mode").getAsString()), "Invalid mode=hour ignored and defaulted to 'day'");

            // 6.3 Empty range period (no revenue in 2020)
            HttpResponse emptyRangeResp = admin.get("/admin/reports/revenue?from=2020-01-01&to=2020-01-10");
            check(emptyRangeResp.statusCode == 200, "Empty historical range returns 200 OK");
            check(emptyRangeResp.body.contains("No revenue in this range."),
                    "Empty range displays 'No revenue in this range.' notice");

            HttpResponse emptyRangeData = admin.get("/admin/reports/revenue/data?from=2020-01-01&to=2020-01-10");
            check(emptyRangeData.statusCode == 200, "Empty historical data returns 200 OK");
            JsonObject emptyJson = GSON.fromJson(emptyRangeData.body, JsonObject.class);
            check(emptyJson.get("totalRevenue").getAsLong() == 0L, "Empty range totalRevenue is 0");
            check(emptyJson.get("totalBookings").getAsInt() == 0, "Empty range totalBookings is 0");

            // -----------------------------------------------------------------
            // CHECKLIST 7: Navbar Links Integration
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 7] Navbar Links Integration ---");
            HttpResponse navCheck = admin.get("/admin/dashboard");
            check(navCheck.body.contains("/admin/dashboard"), "Navbar contains link to Dashboard");
            check(navCheck.body.contains("/admin/reports/revenue"), "Navbar contains link to Revenue report");

            System.out.println("\n==================================================");
            System.out.println("     ALL T17 ACCEPTANCE TESTS COMPLETED           ");
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

    private static Map<String, Integer> getRoomStatusCountsDb() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT status, COUNT(*) FROM ROOM GROUP BY status";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString(1), rs.getInt(2));
            }
        }
        return map;
    }

    private static Map<String, Integer> getDeviceStatusCountsDb() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT status, COUNT(*) FROM DEVICE GROUP BY status";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString(1), rs.getInt(2));
            }
        }
        return map;
    }

    private static int getNewAlertCountDb() throws Exception {
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

    private static int getNoshowHeldCountDb() throws Exception {
        String sql = "SELECT COUNT(*) FROM BOOKING b WHERE b.status = 'EXPIRED' AND b.expire_reason = 'NO_SHOW' "
                   + "AND EXISTS (SELECT 1 FROM BOOKING_SLOT bs WHERE bs.booking_id = b.booking_id AND bs.hold_status = 'HELD')";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private static long getRevenueLast7DaysSumDb() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        String sql = "SELECT ISNULL(SUM(amount), 0) FROM PAYMENT "
                   + "WHERE status = 'PAID' AND CAST(paid_at AS date) >= ? AND CAST(paid_at AS date) <= ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0L;
    }

    private static long[] getRevenueRangeDb(LocalDate from, LocalDate to) throws Exception {
        String sql = "SELECT ISNULL(SUM(amount), 0), COUNT(*) FROM PAYMENT "
                   + "WHERE status = 'PAID' AND paid_at >= ? AND paid_at < ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new long[]{rs.getLong(1), rs.getLong(2)};
                }
            }
        }
        return new long[]{0L, 0L};
    }

    private static boolean verifyAsciiDirectory(File dir) {
        if (!dir.exists()) {
            return true;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return true;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                if (!verifyAsciiDirectory(f)) {
                    return false;
                }
            } else if (f.getName().endsWith(".java") || f.getName().endsWith(".jsp") || f.getName().endsWith(".tag")) {
                if (!isCleanAscii(f)) {
                    System.err.println("[NON-ASCII DETECTED] " + f.getAbsolutePath());
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isCleanAscii(File file) {
        try (InputStream in = new FileInputStream(file)) {
            int b;
            while ((b = in.read()) != -1) {
                if (b > 127) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean verifyNoJstl(File dir) {
        if (!dir.exists()) {
            return true;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return true;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                if (!verifyNoJstl(f)) {
                    return false;
                }
            } else if (f.getName().endsWith(".jsp") || f.getName().endsWith(".tag")) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.contains("<c:") || line.contains("http://java.sun.com/jsp/jstl/core")) {
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
        if (!dir.exists()) {
            return true;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return true;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                if (!verifyNoJakarta(f)) {
                    return false;
                }
            } else if (f.getName().endsWith(".java")) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.contains("import jakarta.")) {
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

    public static class HttpResponse {
        public int statusCode;
        public String body = "";
        public String contentType;
        public String redirectLocation;
        public String cacheControl;
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
