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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * End-to-end acceptance tests for prompt T08 (User/Staff CRUD + M8 role guards).
 */
public class T08AcceptanceTest {

    private static final String BASE_URL = "http://localhost:8080/SmartStay";
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=SmartStayDB;encrypt=true;trustServerCertificate=true;";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "12345";

    private static int totalChecks = 0;
    private static int passedChecks = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("        STARTING T08 ACCEPTANCE VERIFICATION      ");
        System.out.println("==================================================");

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                System.out.println("[DB] Connected to SmartStayDB successfully.");
            }

            // Cleanup any leftovers from previous test runs
            cleanupTestUser("teststaff@smartstay.local");
            cleanupTestUser("testadmin@smartstay.local");
            cleanupTestUser("testguest@smartstay.local");
            cleanupTestUser("staffbyadmin@smartstay.local");
            cleanupTestUser("t8delete@smartstay.local");

            // -------------------------------------------------------------------------
            // 0. Setup test sessions
            // -------------------------------------------------------------------------
            TestClient owner = new TestClient(BASE_URL);
            check(owner.login("owner@smartstay.local", "12345"), "Owner login successful");

            TestClient admin = new TestClient(BASE_URL);
            check(admin.login("admin@smartstay.local", "12345"), "Admin login successful");

            TestClient staff = new TestClient(BASE_URL);
            check(staff.login("staff1@smartstay.local", "12345"), "Staff login successful");

            TestClient guest = new TestClient(BASE_URL);
            check(guest.login("guest1@smartstay.local", "12345"), "Guest login successful");

            TestClient anonymous = new TestClient(BASE_URL);

            // -------------------------------------------------------------------------
            // 1. RBAC and Permission Checks on /admin/users
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T08] Test 1: RBAC Access Control on /admin/users ---");
            check(owner.get("/admin/users").statusCode == 200, "Owner accessing /admin/users receives 200 OK");
            check(admin.get("/admin/users").statusCode == 200, "Admin accessing /admin/users receives 200 OK");
            check(staff.get("/admin/users").statusCode == 403, "Staff accessing /admin/users receives 403 Forbidden");
            check(guest.get("/admin/users").statusCode == 403, "Guest accessing /admin/users receives 403 Forbidden");
            check(anonymous.get("/admin/users").statusCode == 302, "Anonymous accessing /admin/users redirects to /login");

            // -------------------------------------------------------------------------
            // 2. Owner User Management (Full CRUD, ADMIN + STAFF roles)
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T08] Test 2: Owner User Management (STAFF, ADMIN, GUEST) ---");
            HttpResponse ownerForm = owner.get("/admin/users/edit");
            check(ownerForm.statusCode == 200, "Owner GET /admin/users/edit returns 200 OK");
            check(ownerForm.body.contains("GUEST") && ownerForm.body.contains("STAFF") && ownerForm.body.contains("ADMIN"),
                    "Owner form includes GUEST, STAFF, and ADMIN roles");
            check(!ownerForm.body.contains("<option value=\"OWNER\""),
                    "Owner form excludes OWNER option (Owner accounts cannot be created from web)");

            // 2.1 Owner creates STAFF user
            String staffData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&fullName=Nguyen+Van+Staff"
                    + "&email=teststaff%40smartstay.local"
                    + "&phone=0988111222"
                    + "&role=STAFF"
                    + "&status=ACTIVE"
                    + "&password=Password123"
                    + "&confirmPassword=Password123"
                    + "&position=Senior+Room+Attendant"
                    + "&employmentStatus=ACTIVE"
                    + "&hireDate=2026-09-01";

            HttpResponse createStaffResp = owner.postRaw("/admin/users/edit", staffData);
            check(createStaffResp.statusCode == 302, "Owner create STAFF returned 302 redirect");
            long staffId = getUserIdByEmail("teststaff@smartstay.local");
            check(staffId > 0, "teststaff@smartstay.local created in database (ID=" + staffId + ")");
            check("STAFF".equals(getUserRole(staffId)), "User role is STAFF");
            check("Senior Room Attendant".equals(getStaffPosition(staffId)), "Staff position is Senior Room Attendant");
            check("ACTIVE".equals(getStaffEmploymentStatus(staffId)), "Employment status is ACTIVE");
            check(countAuditLogs("USER_CREATED", "APP_USER", String.valueOf(staffId)) >= 1, "Audit USER_CREATED recorded");

            // Verify login as newly created staff
            TestClient newStaffClient = new TestClient(BASE_URL);
            check(newStaffClient.login("teststaff@smartstay.local", "Password123"), "New staff login with Password123 successful");

            // 2.2 Owner creates ADMIN user
            String adminData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&fullName=Tran+Van+Admin"
                    + "&email=testadmin%40smartstay.local"
                    + "&phone=0988333444"
                    + "&role=ADMIN"
                    + "&status=ACTIVE"
                    + "&password=Password123"
                    + "&confirmPassword=Password123"
                    + "&position=Operations+Manager"
                    + "&employmentStatus=ACTIVE"
                    + "&hireDate=2026-08-01";

            HttpResponse createAdminResp = owner.postRaw("/admin/users/edit", adminData);
            check(createAdminResp.statusCode == 302, "Owner create ADMIN returned 302 redirect");
            long adminId = getUserIdByEmail("testadmin@smartstay.local");
            check(adminId > 0, "testadmin@smartstay.local created in database (ID=" + adminId + ")");
            check("ADMIN".equals(getUserRole(adminId)), "User role is ADMIN");

            // 2.3 Owner creates GUEST user (no staff profile)
            String guestData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&fullName=Le+Thi+Guest"
                    + "&email=testguest%40smartstay.local"
                    + "&phone=0988555666"
                    + "&role=GUEST"
                    + "&status=ACTIVE"
                    + "&password=Password123"
                    + "&confirmPassword=Password123";

            HttpResponse createGuestResp = owner.postRaw("/admin/users/edit", guestData);
            check(createGuestResp.statusCode == 302, "Owner create GUEST returned 302 redirect");
            long guestId = getUserIdByEmail("testguest@smartstay.local");
            check(guestId > 0, "testguest@smartstay.local created in database (ID=" + guestId + ")");
            check("GUEST".equals(getUserRole(guestId)), "User role is GUEST");
            check(getStaffPosition(guestId) == null, "GUEST has no staff profile row");

            // -------------------------------------------------------------------------
            // 3. Admin User Management & Role Privilege Escalation Guards (Spec M8)
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T08] Test 3: Admin Privilege Escalation Guards (M8) ---");
            HttpResponse adminForm = admin.get("/admin/users/edit");
            check(adminForm.statusCode == 200, "Admin GET /admin/users/edit returns 200 OK");
            check(adminForm.body.contains("GUEST") && adminForm.body.contains("STAFF"), "Admin form includes GUEST and STAFF");
            check(!adminForm.body.contains("<option value=\"ADMIN\""), "Admin form EXCLUDES ADMIN role option");
            check(!adminForm.body.contains("<option value=\"OWNER\""), "Admin form EXCLUDES OWNER role option");

            // Admin creates STAFF -> ALLOWED
            String staffByAdminData = "_csrf=" + URLEncoder.encode(admin.csrfToken, "UTF-8")
                    + "&fullName=Pham+Van+Staff"
                    + "&email=staffbyadmin%40smartstay.local"
                    + "&phone=0988777888"
                    + "&role=STAFF"
                    + "&status=ACTIVE"
                    + "&password=Password123"
                    + "&confirmPassword=Password123"
                    + "&position=Housekeeping+Assistant"
                    + "&employmentStatus=ACTIVE"
                    + "&hireDate=2026-09-15";

            HttpResponse createStaffByAdminResp = admin.postRaw("/admin/users/edit", staffByAdminData);
            check(createStaffByAdminResp.statusCode == 302, "Admin create STAFF returned 302 redirect");
            long staffByAdminId = getUserIdByEmail("staffbyadmin@smartstay.local");
            check(staffByAdminId > 0, "staffbyadmin created in database (ID=" + staffByAdminId + ")");

            // Admin crafts POST assigning ADMIN -> 403 FORBIDDEN
            String adminEscalateData = "_csrf=" + URLEncoder.encode(admin.csrfToken, "UTF-8")
                    + "&fullName=Hacker+Admin"
                    + "&email=hackeradmin%40smartstay.local"
                    + "&phone=0988999000"
                    + "&role=ADMIN"
                    + "&status=ACTIVE"
                    + "&password=Password123"
                    + "&confirmPassword=Password123"
                    + "&position=Hacker"
                    + "&employmentStatus=ACTIVE";

            HttpResponse escalateResp = admin.postRaw("/admin/users/edit", adminEscalateData);
            check(escalateResp.statusCode == 403, "Admin assigning ADMIN returns 403 Forbidden");
            check(getUserIdByEmail("hackeradmin@smartstay.local") == -1, "No hackeradmin user created in DB");

            // Admin crafts POST assigning OWNER -> 403 FORBIDDEN
            String ownerEscalateData = "_csrf=" + URLEncoder.encode(admin.csrfToken, "UTF-8")
                    + "&fullName=Hacker+Owner"
                    + "&email=hackerowner%40smartstay.local"
                    + "&phone=0988999111"
                    + "&role=OWNER"
                    + "&status=ACTIVE"
                    + "&password=Password123"
                    + "&confirmPassword=Password123";

            HttpResponse escalateOwnerResp = admin.postRaw("/admin/users/edit", ownerEscalateData);
            check(escalateOwnerResp.statusCode == 403, "Admin assigning OWNER returns 403 Forbidden");

            // Admin attempts to edit OWNER account (userId=1) -> 403 FORBIDDEN
            HttpResponse adminEditOwnerResp = admin.get("/admin/users/edit?userId=1");
            check(adminEditOwnerResp.statusCode == 403, "Admin accessing Owner edit form returns 403 Forbidden");

            // -------------------------------------------------------------------------
            // 4. Form Validation & Password Handling on Edit
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T08] Test 4: Form Validation & Password Handling ---");

            // Duplicate email pre-check
            String dupEmailData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&fullName=Dup+Email"
                    + "&email=owner%40smartstay.local"
                    + "&phone=0977111222"
                    + "&role=GUEST&status=ACTIVE&password=Password123&confirmPassword=Password123";

            HttpResponse dupEmailResp = owner.postRaw("/admin/users/edit", dupEmailData);
            check(dupEmailResp.statusCode == 200, "Duplicate email re-renders form (status 200)");
            check(dupEmailResp.body.contains("This email is already registered."), "Displays duplicate email field error");

            // Duplicate phone pre-check
            String dupPhoneData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&fullName=Dup+Phone"
                    + "&email=uniquephone%40smartstay.local"
                    + "&phone=0988111222" // Already used by teststaff
                    + "&role=GUEST&status=ACTIVE&password=Password123&confirmPassword=Password123";

            HttpResponse dupPhoneResp = owner.postRaw("/admin/users/edit", dupPhoneData);
            check(dupPhoneResp.statusCode == 200, "Duplicate phone re-renders form (status 200)");
            check(dupPhoneResp.body.contains("This phone number is already registered."), "Displays duplicate phone field error");

            // Future hire date validation
            String futureHireData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&fullName=Future+Hire"
                    + "&email=futurehire%40smartstay.local"
                    + "&phone=0977222333"
                    + "&role=STAFF&status=ACTIVE&password=Password123&confirmPassword=Password123"
                    + "&position=Attendant&employmentStatus=ACTIVE&hireDate=2099-01-01";

            HttpResponse futureHireResp = owner.postRaw("/admin/users/edit", futureHireData);
            check(futureHireResp.statusCode == 200, "Future hire date re-renders form (status 200)");
            check(futureHireResp.body.contains("Hire date cannot be in the future."), "Displays future hire date field error");

            // Edit with BLANK password -> preserves existing hash and login
            String oldHash = getUserPasswordHash(staffId);
            String editBlankPwData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&userId=" + staffId
                    + "&fullName=Nguyen+Van+Staff+Updated"
                    + "&email=teststaff%40smartstay.local"
                    + "&phone=0988111222"
                    + "&role=STAFF"
                    + "&status=ACTIVE"
                    + "&password="
                    + "&confirmPassword="
                    + "&position=Lead+Room+Attendant"
                    + "&employmentStatus=ACTIVE"
                    + "&hireDate=2026-09-01";

            HttpResponse editBlankPwResp = owner.postRaw("/admin/users/edit", editBlankPwData);
            check(editBlankPwResp.statusCode == 302, "Edit with blank password returned 302 redirect");
            String newHash = getUserPasswordHash(staffId);
            check(oldHash.equals(newHash), "Password hash remains completely unchanged");
            check("Nguyen Van Staff Updated".equals(getUserFullName(staffId)), "Full name updated in DB");
            check("Lead Room Attendant".equals(getStaffPosition(staffId)), "Position updated in DB");

            // Login with old password still works
            TestClient staffLoginAgain = new TestClient(BASE_URL);
            check(staffLoginAgain.login("teststaff@smartstay.local", "Password123"), "Login with original password remains valid");

            // -------------------------------------------------------------------------
            // 5. Self-Lock & Self-Delete Guards
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T08] Test 5: Self-Lock and Self-Delete Safety Guards ---");

            // Owner attempts to self-lock (userId=1, status=LOCKED)
            String selfLockData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&userId=1"
                    + "&fullName=Owner+SelfLock"
                    + "&email=owner%40smartstay.local"
                    + "&phone=0901234567"
                    + "&role=OWNER"
                    + "&status=LOCKED";

            HttpResponse selfLockResp = owner.postRaw("/admin/users/edit", selfLockData);
            check(selfLockResp.statusCode == 200 || selfLockResp.statusCode == 403, "Self-lock attempt was refused");
            check(selfLockResp.body.contains("cannot delete or lock your own account") || selfLockResp.statusCode == 403,
                    "Displays self-lock refusal message");
            check("ACTIVE".equals(getUserStatus(1)), "Owner account status remains ACTIVE in DB");

            // Owner attempts to self-delete (userId=1)
            String selfDeleteData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&userId=1";

            HttpResponse selfDeleteResp = owner.postRaw("/admin/users/delete", selfDeleteData);
            check(selfDeleteResp.statusCode == 302, "Self-delete attempt returned 302");
            HttpResponse userListPage = owner.get("/admin/users");
            check(userListPage.body.contains("You cannot delete or lock your own account"),
                    "Displays self-delete refusal message");
            check(getUserById(1) != null, "Owner account remains safely intact in DB");

            // -------------------------------------------------------------------------
            // 6. Foreign Key Protection (Delete Guest With Bookings)
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T08] Test 6: Foreign Key Delete Protection (A8) ---");
            long guestWithBookingsId = 6; // Seed guest with booking history

            String deleteGuestData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&userId=" + guestWithBookingsId;

            HttpResponse delGuestResp = owner.postRaw("/admin/users/delete", deleteGuestData);
            check(delGuestResp.statusCode == 302, "Delete guest with bookings returns 302 (not 500)");
            HttpResponse usersAfterDel = owner.get("/admin/users");
            check(usersAfterDel.body.contains("This user has related records and cannot be deleted. Lock the account instead."),
                    "Displays friendly FK error recommending account locking");
            check(getUserById(guestWithBookingsId) != null, "Guest with booking history remains safely intact in DB");

            // Hard delete of unreferenced user
            String createDelUserData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&fullName=Delete+Me"
                    + "&email=t8delete%40smartstay.local"
                    + "&phone=0977999888"
                    + "&role=GUEST&status=ACTIVE&password=Password123&confirmPassword=Password123";
            owner.postRaw("/admin/users/edit", createDelUserData);
            long delUserId = getUserIdByEmail("t8delete@smartstay.local");
            check(delUserId > 0, "Created unreferenced user for deletion test");

            String delSuccessData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&userId=" + delUserId;
            HttpResponse delSuccessResp = owner.postRaw("/admin/users/delete", delSuccessData);
            check(delSuccessResp.statusCode == 302, "Hard delete returned 302 redirect");
            check(getUserById(delUserId) == null, "User successfully deleted from database");
            check(countAuditLogs("USER_DELETED", "APP_USER", String.valueOf(delUserId)) >= 1, "Audit USER_DELETED recorded");

            // -------------------------------------------------------------------------
            // 7. Search, Filtering, Sorting, and Pagination
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T08] Test 7: Catalog Search, Filters, and Sorting ---");
            HttpResponse listPage = owner.get("/admin/users");
            check(listPage.statusCode == 200, "Users list returns 200 OK");
            check(listPage.body.contains("Showing") && listPage.body.contains("users"), "Contains pagination summary");

            // Search by keyword
            HttpResponse searchResp = owner.get("/admin/users?keyword=teststaff");
            check(searchResp.statusCode == 200, "Search by keyword returns 200 OK");
            check(searchResp.body.contains("teststaff@smartstay.local"), "Search results contain matching teststaff");
            check(!searchResp.body.contains("guest1@smartstay.local"), "Search results do not contain non-matching guest1");

            // Filter by role
            HttpResponse filterRoleResp = owner.get("/admin/users?role=STAFF");
            check(filterRoleResp.statusCode == 200, "Filter by role STAFF returns 200 OK");
            check(filterRoleResp.body.contains("Staff"), "Results contain Staff role");

            // Filter by status
            HttpResponse filterStatusResp = owner.get("/admin/users?status=ACTIVE");
            check(filterStatusResp.statusCode == 200, "Filter by status ACTIVE returns 200 OK");

            // Sort by name
            HttpResponse sortNameResp = owner.get("/admin/users?sort=name&dir=ASC");
            check(sortNameResp.statusCode == 200, "Sort by name ASC returns 200 OK");

            // Sort by email
            HttpResponse sortEmailResp = owner.get("/admin/users?sort=email&dir=DESC");
            check(sortEmailResp.statusCode == 200, "Sort by email DESC returns 200 OK");

            // Cleanup created test users
            cleanupTestUser("teststaff@smartstay.local");
            cleanupTestUser("testadmin@smartstay.local");
            cleanupTestUser("testguest@smartstay.local");
            cleanupTestUser("staffbyadmin@smartstay.local");

            System.out.println("\n==================================================");
            System.out.println("TEST RESULTS: " + passedChecks + " PASSED, " + (totalChecks - passedChecks) + " FAILED");
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
            System.out.println("  [PASS] " + description);
        } else {
            System.out.println("  [FAIL] " + description);
        }
    }

    // --- DB Helpers ---

    private static void cleanupTestUser(String email) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            long uid = -1;
            try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM APP_USER WHERE email = ?")) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) uid = rs.getLong(1);
                }
            }
            if (uid > 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM AUDIT_LOG WHERE user_id = ?")) {
                    ps.setLong(1, uid);
                    ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM STAFF_INFORMATION WHERE user_id = ?")) {
                    ps.setLong(1, uid);
                    ps.executeUpdate();
                } catch (Exception ignored) {}
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM APP_USER WHERE user_id = ?")) {
                    ps.setLong(1, uid);
                    ps.executeUpdate();
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private static long getUserIdByEmail(String email) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM APP_USER WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1;
    }

    private static String getUserRole(long userId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT r.role_name FROM APP_USER u JOIN ROLE r ON u.role_id = r.role_id WHERE u.user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    private static String getUserStatus(long userId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM APP_USER WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    private static String getUserFullName(long userId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT full_name FROM APP_USER WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    private static String getUserPasswordHash(long userId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT password_hash FROM APP_USER WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    private static String getStaffPosition(long userId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT position FROM STAFF_INFORMATION WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    private static String getStaffEmploymentStatus(long userId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT employment_status FROM STAFF_INFORMATION WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    private static String getUserById(long userId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT email FROM APP_USER WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    private static int countAuditLogs(String action, String entityType, String entityId) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM AUDIT_LOG WHERE action = ? AND entity_type = ?");
        if (entityId != null) {
            sql.append(" AND entity_id = ?");
        }
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, action);
            ps.setString(2, entityType);
            if (entityId != null) {
                ps.setString(3, entityId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    // --- HTTP Client ---

    private static class HttpResponse {
        int statusCode;
        String body;
        Map<String, List<String>> headers;
    }

    private static class TestClient {
        private final String base;
        private String sessionCookie;
        String csrfToken;

        TestClient(String base) {
            this.base = base;
        }

        boolean login(String email, String password) throws Exception {
            HttpResponse loginPage = get("/login");
            Matcher m = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(loginPage.body);
            if (m.find()) {
                csrfToken = m.group(1);
            }

            String data = "email=" + URLEncoder.encode(email, "UTF-8")
                    + "&password=" + URLEncoder.encode(password, "UTF-8")
                    + "&_csrf=" + URLEncoder.encode(csrfToken, "UTF-8");

            HttpResponse loginPost = postRaw("/login", data);
            if (loginPost.statusCode == 302) {
                HttpResponse afterLogin = get("/admin/users");
                Matcher m2 = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(afterLogin.body);
                if (m2.find()) {
                    csrfToken = m2.group(1);
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
                } catch (Exception ignored) {
                }
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
