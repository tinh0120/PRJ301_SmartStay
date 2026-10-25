package tools;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Acceptance Test Suite for T15:
 * Sensor readings viewer: table + Chart.js + threshold lines + localStorage cache.
 */
public class T15AcceptanceTest {

    private static final String BASE_URL = "http://localhost:8080/SmartStay";
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=SmartStayDB;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "12345";

    private static int totalChecks = 0;
    private static int passedChecks = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("        STARTING T15 ACCEPTANCE VERIFICATION      ");
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
            // CHECKLIST 1: Architecture & Code Quality
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
            // CHECKLIST 2: RBAC & Navigation (SENSOR_VIEW)
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 2] RBAC & Navigation Security ---");

            HttpResponse anonView = anon.get("/admin/sensor-readings");
            check(anonView.statusCode == 302 && anonView.redirectLocation != null
                    && anonView.redirectLocation.contains("/login"),
                    "Unauthenticated GET /admin/sensor-readings redirects to login");

            HttpResponse anonData = anon.get("/admin/sensor-readings/data");
            check(anonData.statusCode == 302 || anonData.statusCode == 403,
                    "Unauthenticated GET /admin/sensor-readings/data denied");

            HttpResponse guestView = guest.get("/admin/sensor-readings");
            check(guestView.statusCode == 403, "Guest GET /admin/sensor-readings denied with 403 Forbidden");

            HttpResponse guestData = guest.get("/admin/sensor-readings/data");
            check(guestData.statusCode == 403, "Guest GET /admin/sensor-readings/data denied with 403 Forbidden");

            HttpResponse staffView = staff.get("/admin/sensor-readings");
            check(staffView.statusCode == 403, "Staff GET /admin/sensor-readings denied with 403 Forbidden");

            HttpResponse staffData = staff.get("/admin/sensor-readings/data");
            check(staffData.statusCode == 403, "Staff GET /admin/sensor-readings/data denied with 403 Forbidden");

            HttpResponse adminView = admin.get("/admin/sensor-readings");
            check(adminView.statusCode == 200, "Admin (with SENSOR_VIEW) GET /admin/sensor-readings returns 200 OK");
            check(adminView.body.contains("Sensor data"), "Admin navbar contains 'Sensor data' link");

            // -----------------------------------------------------------------
            // CHECKLIST 3: Default View (LOBBY_CAM_01, DISTANCE_CM, last 7 days)
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 3] Default View & Paginated Table ---");
            check(adminView.body.contains("<canvas id=\"sensorChart\""), "Page contains #sensorChart canvas");
            check(adminView.body.contains("id=\"sensorChartStatus\""), "Page contains #sensorChartStatus div");
            check(adminView.body.contains("DISTANCE_CM"), "Page displays DISTANCE_CM metric badge");
            check(adminView.body.contains("Reading Records"), "Page contains Reading Records table header");
            check(adminView.body.contains("chart-loader.js"), "Page includes chart-loader.js");
            check(adminView.body.contains("chart.umd.min.js"), "Page includes local vendor Chart.js 4 file");

            // Query default JSON chart endpoint
            HttpResponse dataResp = admin.get("/admin/sensor-readings/data");
            check(dataResp.statusCode == 200, "GET /admin/sensor-readings/data returns 200 OK");
            check(dataResp.contentType != null && dataResp.contentType.contains("application/json"),
                    "Endpoint returns application/json Content-Type");

            JsonObject json = JsonParser.parseString(dataResp.body).getAsJsonObject();
            check("DISTANCE_CM".equals(json.get("metric").getAsString()), "JSON payload metric is DISTANCE_CM");

            JsonArray labels = json.getAsJsonArray("labels");
            JsonArray values = json.getAsJsonArray("values");
            JsonArray thresholds = json.getAsJsonArray("thresholds");

            check(labels != null && labels.size() > 0, "Labels array is populated (" + (labels != null ? labels.size() : 0) + " points)");
            check(values != null && values.size() == labels.size(), "Values array matches labels size");
            check(thresholds != null && thresholds.size() > 0, "Thresholds array is populated (" + (thresholds != null ? thresholds.size() : 0) + " thresholds)");

            boolean foundLobbyThreshold = false;
            if (thresholds != null) {
                for (JsonElement el : thresholds) {
                    JsonObject th = el.getAsJsonObject();
                    if (th.get("value").getAsDouble() == 50.0
                            && th.get("label").getAsString().contains("50")
                            && th.get("label").getAsString().contains("LOBBY_PROXIMITY")) {
                        foundLobbyThreshold = true;
                        System.out.println("[DEBUG] Found threshold line: " + th.get("label").getAsString());
                        break;
                    }
                }
            }
            check(foundLobbyThreshold, "Dashed threshold line for '< 50 (LOBBY_PROXIMITY)' is present");

            // -----------------------------------------------------------------
            // CHECKLIST 4: Switch Metric to PIR_TRIGGER (Hourly Bar Chart)
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 4] Switch Metric to PIR_TRIGGER (Hourly Bar Chart) ---");
            HttpResponse pirView = admin.get("/admin/sensor-readings?metric=PIR_TRIGGER");
            check(pirView.statusCode == 200, "GET /admin/sensor-readings?metric=PIR_TRIGGER returns 200 OK");
            check(pirView.body.contains("PIR_TRIGGER"), "PIR_TRIGGER is active metric in view");

            HttpResponse pirDataResp = admin.get("/admin/sensor-readings/data?metric=PIR_TRIGGER");
            check(pirDataResp.statusCode == 200, "GET /admin/sensor-readings/data?metric=PIR_TRIGGER returns 200 OK");

            JsonObject pirJson = JsonParser.parseString(pirDataResp.body).getAsJsonObject();
            check("PIR_TRIGGER".equals(pirJson.get("metric").getAsString()), "JSON metric is PIR_TRIGGER");

            JsonArray pirLabels = pirJson.getAsJsonArray("labels");
            JsonArray pirValues = pirJson.getAsJsonArray("values");
            check(pirLabels != null && pirLabels.size() > 0, "PIR hourly labels populated (" + (pirLabels != null ? pirLabels.size() : 0) + " hours)");
            check(pirValues != null && pirValues.size() == pirLabels.size(), "PIR hourly values size matches labels");

            boolean validHourFormat = false;
            if (pirLabels != null && pirLabels.size() > 0) {
                String firstLabel = pirLabels.get(0).getAsString();
                validHourFormat = firstLabel.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:00");
                System.out.println("[DEBUG] Sample PIR hour bucket: " + firstLabel);
            }
            check(validHourFormat, "PIR labels are correctly bucketed by hour (YYYY-MM-DD HH:00)");

            // -----------------------------------------------------------------
            // CHECKLIST 5: Filter Modifications & Edge Cases
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 5] Filter Modification & Edge Cases ---");

            // 5.1 Door Open Seconds on MAIN_DOOR_LOCK_01
            HttpResponse doorDataResp = admin.get("/admin/sensor-readings/data?device=MAIN_DOOR_LOCK_01&metric=DOOR_OPEN_SECONDS");
            check(doorDataResp.statusCode == 200, "GET data for MAIN_DOOR_LOCK_01 and DOOR_OPEN_SECONDS returns 200 OK");
            JsonObject doorJson = JsonParser.parseString(doorDataResp.body).getAsJsonObject();
            check("DOOR_OPEN_SECONDS".equals(doorJson.get("metric").getAsString()), "Door metric confirmed");

            boolean foundDoorThreshold = false;
            JsonArray doorThresh = doorJson.getAsJsonArray("thresholds");
            if (doorThresh != null) {
                for (JsonElement el : doorThresh) {
                    JsonObject th = el.getAsJsonObject();
                    if (th.get("value").getAsDouble() == 30.0
                            && th.get("label").getAsString().contains("DOOR_OPEN_TOO_LONG")) {
                        foundDoorThreshold = true;
                        System.out.println("[DEBUG] Found door threshold: " + th.get("label").getAsString());
                        break;
                    }
                }
            }
            check(foundDoorThreshold, "Dashed threshold line for '> 30 (DOOR_OPEN_TOO_LONG)' is present");

            // 5.2 Invalid date range: from > to
            HttpResponse invalidDateResp = admin.get("/admin/sensor-readings?from=2026-10-20&to=2026-10-10");
            check(invalidDateResp.statusCode == 200, "From > to returns 200 with form error");
            check(invalidDateResp.body.contains("before or equal to"), "Field error message shown when from > to");

            // 5.3 Crafted unknown device id
            HttpResponse craftedDevResp = admin.get("/admin/sensor-readings?device=FAKE_DEVICE_XYZ");
            check(craftedDevResp.statusCode == 200, "Unknown device id returns 200 with defaults applied");
            check(craftedDevResp.body.contains("Unknown device."), "Field error message shown for crafted device ID");
            check(craftedDevResp.body.contains("LOBBY_CAM_01"), "Defaults applied to LOBBY_CAM_01");

            // 5.4 Empty range (no data)
            HttpResponse emptyDataResp = admin.get("/admin/sensor-readings/data?from=2020-01-01&to=2020-01-02");
            check(emptyDataResp.statusCode == 200, "No data range returns 200 OK");
            JsonObject emptyJson = JsonParser.parseString(emptyDataResp.body).getAsJsonObject();
            check(emptyJson.getAsJsonArray("labels").size() == 0, "No data returns empty labels array");
            check(emptyJson.getAsJsonArray("values").size() == 0, "No data returns empty values array");

            HttpResponse emptyViewResp = admin.get("/admin/sensor-readings?from=2020-01-01&to=2020-01-02");
            check(emptyViewResp.statusCode == 200, "Empty view returns 200 OK");
            check(emptyViewResp.body.contains("No records found"), "Empty range displays 'No records found' in table");

            // -----------------------------------------------------------------
            // CHECKLIST 6: Offline Cache & Chart Loader Contract
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 6] Offline Cache Contract & Script Guards ---");
            File chartLoaderFile = new File("web/assets/js/chart-loader.js");
            check(chartLoaderFile.exists(), "chart-loader.js exists at web/assets/js/chart-loader.js");

            String chartLoaderContent = new String(Files.readAllBytes(chartLoaderFile.toPath()), StandardCharsets.UTF_8);
            check(chartLoaderContent.contains("localStorage.getItem(options.cacheKey)"),
                    "chart-loader.js reads cache via localStorage.getItem");
            check(chartLoaderContent.contains("localStorage.setItem(options.cacheKey"),
                    "chart-loader.js saves cache via localStorage.setItem");
            check(chartLoaderContent.contains("Showing cached data saved at"),
                    "chart-loader.js contains fallback message: 'Showing cached data saved at'");
            check(chartLoaderContent.contains("typeof Chart === 'undefined'"),
                    "chart-loader.js guards against missing Chart vendor file");

            File sensorViewJsp = new File("web/WEB-INF/views/admin/sensor-readings.jsp");
            String jspContent = new String(Files.readAllBytes(sensorViewJsp.toPath()), StandardCharsets.UTF_8);
            check(jspContent.contains("'smartstay.chart.sensor.' + device + '.' + metric"),
                    "sensor-readings.jsp constructs cacheKey 'smartstay.chart.sensor.<device>.<metric>'");
            check(jspContent.contains("No readings in this range."),
                    "sensor-readings.jsp handles empty range with 'No readings in this range.' message");

            // -----------------------------------------------------------------
            // CHECKLIST 7: Pagination URL Preservation
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 7] Pagination URL Preservation ---");
            check(adminView.body.contains("page="), "Pagination controls render page navigation");

            System.out.println("\n==================================================");
            System.out.println("     ALL T15 ACCEPTANCE TESTS COMPLETED           ");
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
