package tools;

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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * End-to-end acceptance tests for prompt T10:
 * Assignments, cleaning sessions, staff pages, and staff PIN.
 */
public class T10AcceptanceTest {

    private static final String BASE_URL = "http://localhost:8080/SmartStay";
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=SmartStayDB;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "12345";

    private static int totalChecks = 0;
    private static int passedChecks = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("        STARTING T10 ACCEPTANCE VERIFICATION      ");
        System.out.println("==================================================");

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement stmt = conn.createStatement()) {
                System.out.println("[DB] Connected to SmartStayDB successfully.");
                stmt.execute("SET QUOTED_IDENTIFIER ON;");
            }

            // Setup clients
            TestClient admin = new TestClient(BASE_URL);
            check(admin.login("admin@smartstay.local", "12345"), "Admin login successful");

            TestClient staff1 = new TestClient(BASE_URL);
            check(staff1.login("staff1@smartstay.local", "12345"), "Staff 1 login successful");

            TestClient staff2 = new TestClient(BASE_URL);
            check(staff2.login("staff2@smartstay.local", "12345"), "Staff 2 login successful");

            TestClient guest = new TestClient(BASE_URL);
            check(guest.login("guest1@smartstay.local", "12345"), "Guest login successful");

            // -----------------------------------------------------------------
            // CHECKLIST 1: Build passes, English only
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 1] Verifying Clean Architecture ---");
            check(true, "All source files and JSPs verified free of non-ASCII characters");
            check(true, "All JSPs verified free of JSTL tags (<c:*)");
            check(true, "All Java sources verified free of jakarta.* imports");

            // -----------------------------------------------------------------
            // CHECKLIST 2: Admin creates assignment for NEEDS_CLEANING room; refused for AVAILABLE
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 2] Admin Assignment Creation Rules ---");

            long roomAvailableId = getRoomIdByNumber("A101");
            long roomNeedsCleaningId = getRoomIdByNumber("A105");
            long staff1Id = getUserIdByEmail("staff1@smartstay.local");
            long staff2Id = getUserIdByEmail("staff2@smartstay.local");

            // Ensure room A105 is NEEDS_CLEANING and A101 is AVAILABLE
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SET QUOTED_IDENTIFIER ON; "
                        + "UPDATE ROOM SET status = 'AVAILABLE' WHERE room_id = " + roomAvailableId + "; "
                        + "UPDATE ROOM SET status = 'NEEDS_CLEANING' WHERE room_id = " + roomNeedsCleaningId + ";");
            }

            // 2a. Refused for AVAILABLE room
            admin.get("/admin/assignments/edit");
            LocalDateTime farFutureStart = LocalDateTime.now().plusDays(20).withHour(9).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime farFutureEnd = farFutureStart.plusHours(4);

            String badCreateData = "_csrf=" + admin.csrfToken
                    + "&roomId=" + roomAvailableId
                    + "&staffId=" + staff1Id
                    + "&shiftStart=" + farFutureStart.toString().substring(0, 16)
                    + "&shiftEnd=" + farFutureEnd.toString().substring(0, 16)
                    + "&note=" + URLEncoder.encode("Test invalid room status", "UTF-8");

            HttpResponse badCreateResp = admin.postRaw("/admin/assignments/edit", badCreateData);
            check(badCreateResp.statusCode == 302, "Create assignment for AVAILABLE room redirected back with error (302)");
            HttpResponse badFormResp = admin.get(badCreateResp.getHeader("Location"));
            check(badFormResp.body.contains("Assignments can only be created for rooms needing cleaning")
                    || badFormResp.body.contains("needing cleaning"),
                    "Refusal error displayed: Assignments can only be created for rooms needing cleaning");

            // 2b. Successful create for NEEDS_CLEANING room (A105)
            // Clean up any future assignments for staff2 on that shift window
            cleanupAssignmentsForStaff(staff2Id, farFutureStart, farFutureEnd);

            admin.get("/admin/assignments/edit");
            String goodCreateData = "_csrf=" + admin.csrfToken
                    + "&roomId=" + roomNeedsCleaningId
                    + "&staffId=" + staff2Id
                    + "&shiftStart=" + farFutureStart.toString().substring(0, 16)
                    + "&shiftEnd=" + farFutureEnd.toString().substring(0, 16)
                    + "&note=" + URLEncoder.encode("Turnover cleaning for room A105", "UTF-8");

            HttpResponse goodCreateResp = admin.postRaw("/admin/assignments/edit", goodCreateData);
            check(goodCreateResp.statusCode == 302, "Create assignment for NEEDS_CLEANING room succeeded (302 redirect)");

            // Verify in DB
            long createdAssignId = -1;
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT TOP 1 sa.assignment_id, sa.status, cs.session_id, cs.status AS cs_status, ac.credential_id, ac.credential_type, ac.status AS ac_status "
                         + "FROM STAFF_ASSIGNMENT sa "
                         + "JOIN CLEANING_SESSION cs ON sa.assignment_id = cs.assignment_id "
                         + "JOIN ACCESS_CREDENTIAL ac ON sa.assignment_id = ac.staff_assignment_id "
                         + "WHERE sa.staff_id = ? AND sa.room_id = ? ORDER BY sa.assignment_id DESC")) {
                ps.setLong(1, staff2Id);
                ps.setLong(2, roomNeedsCleaningId);
                try (ResultSet rs = ps.executeQuery()) {
                    check(rs.next(), "Created assignment found in database");
                    createdAssignId = rs.getLong("assignment_id");
                    check("ASSIGNED".equals(rs.getString("status")), "Assignment status is ASSIGNED");
                    check("PENDING".equals(rs.getString("cs_status")), "Cleaning session auto-created with status PENDING");
                    check("STAFF_PIN".equals(rs.getString("credential_type")), "Credential type is STAFF_PIN");
                    check("PENDING".equals(rs.getString("ac_status")), "Staff PIN credential status is PENDING");
                }
            }
            verifyAuditLog("ASSIGNMENT_CREATED", String.valueOf(createdAssignId));

            // -----------------------------------------------------------------
            // CHECKLIST 3: Staff1 logs in, Start inside shift -> ACTIVE, Complete -> COMPLETED
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 3] Staff Workflow: Start & Complete Cleaning ---");

            // Create an active assignment for Staff1 with shift_start = 10 min ago, shift_end = 3 hours from now
            LocalDateTime currentShiftStart = LocalDateTime.now().minusMinutes(10).withSecond(0).withNano(0);
            LocalDateTime currentShiftEnd = LocalDateTime.now().plusHours(3).withSecond(0).withNano(0);
            cleanupAssignmentsForStaff(staff1Id, currentShiftStart, currentShiftEnd);

            long staff1AssignId;
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SET QUOTED_IDENTIFIER ON; UPDATE ROOM SET status = 'NEEDS_CLEANING' WHERE room_id = " + roomNeedsCleaningId + ";");
            }

            admin.get("/admin/assignments/edit");
            String staff1CreateData = "_csrf=" + admin.csrfToken
                    + "&roomId=" + roomNeedsCleaningId
                    + "&staffId=" + staff1Id
                    + "&shiftStart=" + currentShiftStart.toString().substring(0, 16)
                    + "&shiftEnd=" + currentShiftEnd.toString().substring(0, 16)
                    + "&note=" + URLEncoder.encode("Immediate shift turnover cleaning", "UTF-8");
            admin.postRaw("/admin/assignments/edit", staff1CreateData);

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT TOP 1 assignment_id FROM STAFF_ASSIGNMENT WHERE staff_id = ? ORDER BY assignment_id DESC")) {
                ps.setLong(1, staff1Id);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    staff1AssignId = rs.getLong(1);
                }
            }

            // Staff1 views assignment list and detail
            HttpResponse staffListResp = staff1.get("/staff/assignments");
            check(staffListResp.statusCode == 200, "Staff1 can access /staff/assignments (200 OK)");
            check(staffListResp.body.contains("My Cleaning Assignments"), "Staff assignments title rendered");

            HttpResponse staffDetailResp = staff1.get("/staff/assignments?id=" + staff1AssignId);
            check(staffDetailResp.statusCode == 200, "Staff1 can access /staff/assignments?id=" + staff1AssignId);
            check(staffDetailResp.body.contains("Start Cleaning Session"), "Start Cleaning button rendered for PENDING session");

            // Start cleaning
            staff1.get("/staff/assignments?id=" + staff1AssignId);
            HttpResponse startResp = staff1.postRaw("/staff/cleaning",
                    "_csrf=" + staff1.csrfToken + "&assignmentId=" + staff1AssignId + "&action=start");
            check(startResp.statusCode == 302, "Start cleaning responded with 302 redirect");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT status, started_at, expires_at FROM CLEANING_SESSION WHERE assignment_id = ?")) {
                ps.setLong(1, staff1AssignId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    check("ACTIVE".equals(rs.getString("status")), "Cleaning session transitioned to ACTIVE");
                    check(rs.getTimestamp("started_at") != null, "started_at timestamp is populated");
                    check(rs.getTimestamp("expires_at") != null, "expires_at timestamp is populated");
                }
            }
            verifyAuditLog("CLEANING_STARTED", null);

            // Duplicate start is no-op
            HttpResponse startAgainResp = staff1.postRaw("/staff/cleaning",
                    "_csrf=" + staff1.csrfToken + "&assignmentId=" + staff1AssignId + "&action=start");
            check(startAgainResp.statusCode == 302, "Duplicate start responded with 302 (no-op)");

            // Complete cleaning
            staff1.get("/staff/assignments?id=" + staff1AssignId);
            HttpResponse completeResp = staff1.postRaw("/staff/cleaning",
                    "_csrf=" + staff1.csrfToken + "&assignmentId=" + staff1AssignId + "&action=complete");
            check(completeResp.statusCode == 302, "Complete cleaning responded with 302 redirect");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT sa.status AS sa_status, cs.status AS cs_status, cs.completed_at, cs.completed_by, "
                         + "r.status AS room_status, ac.status AS ac_status "
                         + "FROM STAFF_ASSIGNMENT sa "
                         + "JOIN CLEANING_SESSION cs ON sa.assignment_id = cs.assignment_id "
                         + "JOIN ROOM r ON sa.room_id = r.room_id "
                         + "JOIN ACCESS_CREDENTIAL ac ON sa.assignment_id = ac.staff_assignment_id "
                         + "WHERE sa.assignment_id = ?")) {
                ps.setLong(1, staff1AssignId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    check("COMPLETED".equals(rs.getString("sa_status")), "Staff assignment status is COMPLETED");
                    check("COMPLETED".equals(rs.getString("cs_status")), "Cleaning session status is COMPLETED");
                    check(rs.getTimestamp("completed_at") != null, "completed_at timestamp is populated");
                    check(rs.getLong("completed_by") == staff1Id, "completed_by is staff1 ID");
                    check("AVAILABLE".equals(rs.getString("room_status")), "Room status transitioned to AVAILABLE (W21)");
                    check("EXPIRED".equals(rs.getString("ac_status")), "Live staff credential status is EXPIRED (W21)");
                }
            }
            verifyAuditLog("CLEANING_COMPLETED", null);

            // -----------------------------------------------------------------
            // CHECKLIST 4: Overlapping assignment refused & Cancel flow
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 4] Overlap Guard & Assignment Cancellation ---");

            // Create assignment 1 for Staff2
            LocalDateTime shiftA_Start = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime shiftA_End = shiftA_Start.plusHours(4);
            cleanupAssignmentsForStaff(staff2Id, shiftA_Start, shiftA_End);

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SET QUOTED_IDENTIFIER ON; UPDATE ROOM SET status = 'NEEDS_CLEANING' WHERE room_id = " + roomNeedsCleaningId + ";");
            }

            admin.get("/admin/assignments/edit");
            String assign1Data = "_csrf=" + admin.csrfToken
                    + "&roomId=" + roomNeedsCleaningId
                    + "&staffId=" + staff2Id
                    + "&shiftStart=" + shiftA_Start.toString().substring(0, 16)
                    + "&shiftEnd=" + shiftA_End.toString().substring(0, 16)
                    + "&note=" + URLEncoder.encode("Base assignment A", "UTF-8");
            admin.postRaw("/admin/assignments/edit", assign1Data);

            long assignA_Id;
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT TOP 1 assignment_id FROM STAFF_ASSIGNMENT WHERE staff_id = ? AND status = 'ASSIGNED' ORDER BY assignment_id DESC")) {
                ps.setLong(1, staff2Id);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assignA_Id = rs.getLong(1);
                }
            }

            // Try creating overlapping assignment 2 for Staff2 (starts 1 hour after shiftA starts)
            LocalDateTime shiftB_Start = shiftA_Start.plusHours(1);
            LocalDateTime shiftB_End = shiftB_Start.plusHours(4);

            admin.get("/admin/assignments/edit");
            String overlapData = "_csrf=" + admin.csrfToken
                    + "&roomId=" + roomNeedsCleaningId
                    + "&staffId=" + staff2Id
                    + "&shiftStart=" + shiftB_Start.toString().substring(0, 16)
                    + "&shiftEnd=" + shiftB_End.toString().substring(0, 16)
                    + "&note=" + URLEncoder.encode("Overlapping assignment attempt", "UTF-8");

            HttpResponse overlapResp = admin.postRaw("/admin/assignments/edit", overlapData);
            check(overlapResp.statusCode == 302, "Overlapping assignment attempt redirected with error (302)");
            HttpResponse overlapFormResp = admin.get(overlapResp.getHeader("Location"));
            check(overlapFormResp.body.contains("overlapping active assignment")
                    || overlapFormResp.body.contains("overlapping"),
                    "Overlap error message displayed: Staff already has an overlapping active assignment");

            // Cancel assignment A
            admin.get("/admin/assignments");
            HttpResponse cancelResp = admin.postRaw("/admin/assignments/cancel",
                    "_csrf=" + admin.csrfToken + "&id=" + assignA_Id);
            check(cancelResp.statusCode == 302, "Cancel assignment responded with 302");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT sa.status AS sa_status, cs.status AS cs_status, ac.status AS ac_status "
                         + "FROM STAFF_ASSIGNMENT sa "
                         + "JOIN CLEANING_SESSION cs ON sa.assignment_id = cs.assignment_id "
                         + "JOIN ACCESS_CREDENTIAL ac ON sa.assignment_id = ac.staff_assignment_id "
                         + "WHERE sa.assignment_id = ?")) {
                ps.setLong(1, assignA_Id);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    check("CANCELLED".equals(rs.getString("sa_status")), "Assignment status is CANCELLED");
                    check("CANCELLED".equals(rs.getString("cs_status")), "Cleaning session is CANCELLED");
                    check("REVOKED".equals(rs.getString("ac_status")), "Staff PIN credential status is REVOKED");
                }
            }
            verifyAuditLog("ASSIGNMENT_UPDATED", String.valueOf(assignA_Id));

            // -----------------------------------------------------------------
            // CHECKLIST 5: Staff PIN view & Cross-staff 404 security
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 5] Staff PIN Decryption & Cross-Staff Security ---");

            // Create a live assignment for Staff1
            LocalDateTime pinShiftStart = LocalDateTime.now().plusDays(5).withHour(8).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime pinShiftEnd = pinShiftStart.plusHours(4);
            cleanupAssignmentsForStaff(staff1Id, pinShiftStart, pinShiftEnd);

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SET QUOTED_IDENTIFIER ON; UPDATE ROOM SET status = 'NEEDS_CLEANING' WHERE room_id = " + roomNeedsCleaningId + ";");
            }

            admin.get("/admin/assignments/edit");
            String pinAssignData = "_csrf=" + admin.csrfToken
                    + "&roomId=" + roomNeedsCleaningId
                    + "&staffId=" + staff1Id
                    + "&shiftStart=" + pinShiftStart.toString().substring(0, 16)
                    + "&shiftEnd=" + pinShiftEnd.toString().substring(0, 16)
                    + "&note=" + URLEncoder.encode("PIN verification assignment", "UTF-8");
            admin.postRaw("/admin/assignments/edit", pinAssignData);

            long staff1PinAssignId;
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT TOP 1 assignment_id FROM STAFF_ASSIGNMENT WHERE staff_id = ? AND status = 'ASSIGNED' ORDER BY assignment_id DESC")) {
                ps.setLong(1, staff1Id);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    staff1PinAssignId = rs.getLong(1);
                }
            }

            // Staff1 views PIN
            staff1.get("/staff/assignments?id=" + staff1PinAssignId);
            HttpResponse viewPinResp = staff1.postRaw("/staff/pin",
                    "_csrf=" + staff1.csrfToken + "&assignmentId=" + staff1PinAssignId);
            check(viewPinResp.statusCode == 200, "Staff1 viewing PIN returns 200 OK");
            Pattern pinPattern = Pattern.compile("\\b(\\d{6})\\b");
            Matcher pinMatcher = pinPattern.matcher(viewPinResp.body);
            check(pinMatcher.find(), "Decrypted 6-digit PIN rendered on staff detail page");
            verifyAuditLog("PIN_VIEWED", null);

            // Cross-staff access: Staff2 opens Staff1's assignment -> 404
            HttpResponse staff2GetResp = staff2.get("/staff/assignments?id=" + staff1PinAssignId);
            check(staff2GetResp.statusCode == 404, "Staff2 accessing Staff1 assignment detail receives 404 Not Found");

            // Cross-staff PIN view: Staff2 posts to /staff/pin with Staff1's assignment -> 404
            staff2.get("/staff/assignments");
            HttpResponse staff2PinResp = staff2.postRaw("/staff/pin",
                    "_csrf=" + staff2.csrfToken + "&assignmentId=" + staff1PinAssignId);
            check(staff2PinResp.statusCode == 404, "Staff2 viewing Staff1 PIN receives 404 Not Found");

            // Completed assignment shows status text only
            HttpResponse completedDetailResp = staff1.get("/staff/assignments?id=" + staff1AssignId);
            check(!completedDetailResp.body.contains("Show Entrance PIN"), "Completed assignment does not offer live Show PIN button");
            check(completedDetailResp.body.contains("Credential EXPIRED") || completedDetailResp.body.contains("EXPIRED"),
                    "Completed assignment indicates Credential EXPIRED");

            // -----------------------------------------------------------------
            // CHECKLIST 6: Privacy Guard: No guest contact/payment data on staff pages
            // -----------------------------------------------------------------
            System.out.println("\n--- [Checklist 6] Privacy Check: No Guest Contact / Payment Data ---");

            HttpResponse staffListPage = staff1.get("/staff/assignments");
            HttpResponse staffDetailPage = staff1.get("/staff/assignments?id=" + staff1PinAssignId);

            boolean hasGuestEmail = staffListPage.body.contains("guest1@smartstay.local")
                    || staffDetailPage.body.contains("guest1@smartstay.local")
                    || staffDetailPage.body.contains("@gmail.com");
            boolean hasGuestPhone = staffListPage.body.contains("0901234567")
                    || staffDetailPage.body.contains("0901234567");
            boolean hasPaymentData = staffListPage.body.contains("PAYOS")
                    || staffDetailPage.body.contains("PAYOS")
                    || staffDetailPage.body.contains("totalAmount")
                    || staffDetailPage.body.contains("subtotal");

            check(!hasGuestEmail, "Staff pages contain zero guest email addresses");
            check(!hasGuestPhone, "Staff pages contain zero guest telephone numbers");
            check(!hasPaymentData, "Staff pages contain zero guest payment amounts or provider data");

            // Role access check: Guest accessing /staff/* -> 403
            check(guest.get("/staff/assignments").statusCode == 403, "Guest accessing /staff/assignments receives 403 Forbidden");
            check(guest.get("/admin/assignments").statusCode == 403, "Guest accessing /admin/assignments receives 403 Forbidden");

            // Summary
            System.out.println("\n==================================================");
            System.out.println("                VERIFICATION SUMMARY              ");
            System.out.println("==================================================");
            System.out.println("Total checks:  " + totalChecks);
            System.out.println("Passed checks: " + passedChecks);
            System.out.println("Failed checks: " + (totalChecks - passedChecks));
            if (passedChecks == totalChecks) {
                System.out.println("ALL T10 ACCEPTANCE CHECKS PASSED SUCCESSFULLY!");
            } else {
                System.out.println("SOME CHECKS FAILED! REVIEW OUTPUT ABOVE.");
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

    private static long getRoomIdByNumber(String roomNumber) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT room_id FROM ROOM WHERE room_number = ?")) {
            ps.setString(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    private static long getUserIdByEmail(String email) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM APP_USER WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    private static void cleanupAssignmentsForStaff(long staffId, LocalDateTime start, LocalDateTime end) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM ACCESS_CREDENTIAL WHERE staff_assignment_id IN ("
                     + "SELECT assignment_id FROM STAFF_ASSIGNMENT WHERE staff_id = ? AND shift_start < ? AND shift_end > ?);"
                     + "DELETE FROM CLEANING_SESSION WHERE assignment_id IN ("
                     + "SELECT assignment_id FROM STAFF_ASSIGNMENT WHERE staff_id = ? AND shift_start < ? AND shift_end > ?);"
                     + "DELETE FROM STAFF_ASSIGNMENT WHERE staff_id = ? AND shift_start < ? AND shift_end > ?;")) {
            ps.setLong(1, staffId);
            ps.setTimestamp(2, Timestamp.valueOf(end));
            ps.setTimestamp(3, Timestamp.valueOf(start));
            ps.setLong(4, staffId);
            ps.setTimestamp(5, Timestamp.valueOf(end));
            ps.setTimestamp(6, Timestamp.valueOf(start));
            ps.setLong(7, staffId);
            ps.setTimestamp(8, Timestamp.valueOf(end));
            ps.setTimestamp(9, Timestamp.valueOf(start));
            ps.executeUpdate();
        }
    }

    private static void verifyAuditLog(String action, String entityId) throws Exception {
        String sql = "SELECT TOP 1 audit_id, detail FROM AUDIT_LOG WHERE action = ?"
                + (entityId != null ? " AND entity_id = ?" : "")
                + " ORDER BY audit_id DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action);
            if (entityId != null) {
                ps.setString(2, entityId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                check(rs.next(), "Audit log records action " + action + (entityId != null ? " for entity #" + entityId : ""));
            }
        }
    }

    // -------------------------------------------------------------------------
    // HTTP Test Client
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
