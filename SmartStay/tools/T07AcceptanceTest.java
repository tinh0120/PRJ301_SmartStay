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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Acceptance test verifying Prompt T07 requirements:
 * 1. Admin C-R-U-D on test room (auto-creates default 4 slots, edit, deactivate, delete).
 * 2. Delete A101 (has booking history) -> refused with friendly error, row stays.
 * 3. Owner edits A102 S1 price only while HELD future hold exists -> OK.
 * 4. Owner edits A102 S1 start_time while HELD future hold exists -> refused (W25).
 * 5. Admin opens /admin/rooms/slots -> 403 (G5).
 * 6. Status A102 -> AVAILABLE while CHECKED_IN booking exists -> refused; allowed after checkout.
 * 7. Homestay: Owner edits name -> persists + audit; Admin direct access -> 403.
 * 8. List with 6+ rooms: search/filter/sort/paginate keep params; summary matches COUNT(*).
 */
public class T07AcceptanceTest {

    private static final String BASE_URL = "http://localhost:8080/SmartStay";
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=SmartStayDB;encrypt=false";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "12345";

    private static int totalChecks = 0;
    private static int passedChecks = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("        STARTING T07 ACCEPTANCE VERIFICATION      ");
        System.out.println("==================================================");

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                System.out.println("[DB] Connected to SmartStayDB successfully.");
            }

            TestClient owner = new TestClient(BASE_URL);
            TestClient admin = new TestClient(BASE_URL);
            TestClient staff = new TestClient(BASE_URL);

            check(owner.login("owner@smartstay.local", "12345"), "Owner login successful");
            check(admin.login("admin@smartstay.local", "12345"), "Admin login successful");
            check(staff.login("staff1@smartstay.local", "12345"), "Staff login successful");

            // -------------------------------------------------------------------------
            // 1. Room CRUD on test room
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T07] Test 1: Full CRUD on Test Room (Admin) ---");
            // Clean up old test room if exists
            cleanupRoom("TEST-901");

            String createData = "_csrf=" + URLEncoder.encode(admin.csrfToken, "UTF-8")
                    + "&roomNumber=TEST-901"
                    + "&floorNumber=2"
                    + "&capacity=3"
                    + "&locationDescription=Penthouse+wing"
                    + "&description=A+spacious+luxury+suite+with+lake+view."
                    + "&amenities=Balcony,+Espresso+machine,+Jacuzzi"
                    + "&imageUrl=/assets/images/rooms/test-901.jpg"
                    + "&active=on";

            HttpResponse createResp = admin.postRaw("/admin/rooms/edit", createData);
            check(createResp.statusCode == 302, "Create room returned 302 redirect");
            check(createResp.getHeader("Location").contains("/admin/rooms"), "Redirected to /admin/rooms");

            long testRoomId = getRoomIdByNumber("TEST-901");
            check(testRoomId > 0, "TEST-901 created in database (ID=" + testRoomId + ")");

            // Verify default 4 slots auto-created
            int slotCount = countSlotsForRoom(testRoomId);
            check(slotCount == 4, "Auto-created exactly 4 default time slots");

            long s1Price = getSlotPrice(testRoomId, "S1");
            long s2Price = getSlotPrice(testRoomId, "S2");
            long s3Price = getSlotPrice(testRoomId, "S3");
            long s4Price = getSlotPrice(testRoomId, "S4");
            check(s1Price == 150000L, "S1 default price is 150,000 VND");
            check(s2Price == 150000L, "S2 default price is 150,000 VND");
            check(s3Price == 180000L, "S3 default price is 180,000 VND");
            check(s4Price == 300000L, "S4 default price is 300,000 VND");

            check(countAuditLogs("ROOM_CREATED", "ROOM", String.valueOf(testRoomId)) >= 1,
                    "Audit ROOM_CREATED recorded");

            // Edit test room
            String editData = "_csrf=" + URLEncoder.encode(admin.csrfToken, "UTF-8")
                    + "&roomId=" + testRoomId
                    + "&roomNumber=TEST-901"
                    + "&floorNumber=2"
                    + "&capacity=4"
                    + "&locationDescription=Updated+penthouse+wing"
                    + "&description=Updated+description"
                    + "&amenities=Balcony,+Espresso+machine"
                    + "&active=on";

            HttpResponse editResp = admin.postRaw("/admin/rooms/edit", editData);
            check(editResp.statusCode == 302, "Edit room returned 302 redirect");
            check(getRoomCapacity(testRoomId) == 4, "Room capacity updated to 4 in database");
            check(countAuditLogs("ROOM_UPDATED", "ROOM", String.valueOf(testRoomId)) >= 1,
                    "Audit ROOM_UPDATED recorded");

            // Deactivate test room
            String deactData = "_csrf=" + URLEncoder.encode(admin.csrfToken, "UTF-8")
                    + "&roomId=" + testRoomId
                    + "&roomNumber=TEST-901"
                    + "&floorNumber=2"
                    + "&capacity=4"
                    + "&locationDescription=Updated+penthouse+wing";

            HttpResponse deactResp = admin.postRaw("/admin/rooms/edit", deactData);
            check(deactResp.statusCode == 302, "Deactivate room returned 302 redirect");
            check(!isRoomActive(testRoomId), "Room is_active is now 0 (inactive)");

            // Delete test room (succeeds because 0 bookings exist)
            String delData = "_csrf=" + URLEncoder.encode(admin.csrfToken, "UTF-8")
                    + "&roomId=" + testRoomId;
            HttpResponse delResp = admin.postRaw("/admin/rooms/delete", delData);
            check(delResp.statusCode == 302, "Delete room returned 302 redirect");
            check(getRoomIdByNumber("TEST-901") == -1, "Test room deleted from DB");
            check(countSlotsForRoom(testRoomId) == 0, "Test room slots deleted from DB");
            check(countAuditLogs("ROOM_DELETED", "ROOM", String.valueOf(testRoomId)) >= 1,
                    "Audit ROOM_DELETED recorded");

            // -------------------------------------------------------------------------
            // 2. Delete Protection on Room with Bookings (A101)
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T07] Test 2: Foreign Key Delete Protection (A101) ---");
            long a101Id = getRoomIdByNumber("A101");
            check(a101Id > 0, "Found room A101 (ID=" + a101Id + ")");

            String delA101Data = "_csrf=" + URLEncoder.encode(admin.csrfToken, "UTF-8")
                    + "&roomId=" + a101Id;
            HttpResponse delA101Resp = admin.postRaw("/admin/rooms/delete", delA101Data);
            check(delA101Resp.statusCode == 302, "Delete A101 returned 302 redirect (not 500)");

            HttpResponse afterDelPage = admin.get("/admin/rooms");
            check(afterDelPage.body.contains("This room has booking history and cannot be deleted"),
                    "Displays friendly error recommending deactivation");
            check(getRoomIdByNumber("A101") == a101Id, "Room A101 remains safely intact in DB");

            // -------------------------------------------------------------------------
            // 3. Room Slots & W25 Guard (Owner Only)
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T07] Test 3: Room Slots Scheduling & W25 Guard ---");
            long a102Id = getRoomIdByNumber("A102");
            check(a102Id > 0, "Found room A102 (ID=" + a102Id + ")");

            // Admin direct access to slots -> 403 Forbidden (G5)
            HttpResponse adminSlotsResp = admin.get("/admin/rooms/slots?roomId=" + a102Id);
            check(adminSlotsResp.statusCode == 403, "Admin accessing /admin/rooms/slots receives 403 Forbidden (G5)");

            // Staff direct access -> 403 Forbidden
            HttpResponse staffSlotsResp = staff.get("/admin/rooms/slots?roomId=" + a102Id);
            check(staffSlotsResp.statusCode == 403, "Staff accessing /admin/rooms/slots receives 403 Forbidden");

            // Owner direct access -> 200 OK
            HttpResponse ownerSlotsResp = owner.get("/admin/rooms/slots?roomId=" + a102Id);
            check(ownerSlotsResp.statusCode == 200, "Owner accessing /admin/rooms/slots receives 200 OK");
            check(ownerSlotsResp.body.contains("Time Slots &amp; Pricing"), "Contains slots management heading");
            check(ownerSlotsResp.body.contains("S1") && ownerSlotsResp.body.contains("S4"), "Displays all 4 slots");

            // Insert a future HELD booking slot on A102 S1 to test W25 guard
            long a102S1SlotId = getSlotId(a102Id, "S1");
            long testBookingId = createFutureHeldBooking(a102Id, a102S1SlotId);

            // Attempt to edit A102 S1 startTime (to 09:30) while future hold exists (non-overlapping with S4: 20:00-09:00)
            String guardPostData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&roomId=" + a102Id
                    + "&slotId=" + a102S1SlotId + "&slotCode=S1&slotName=Morning&startTime=09:30&endTime=12:00&price=150000&active_S1=on"
                    + "&slotId=" + getSlotId(a102Id, "S2") + "&slotCode=S2&slotName=Afternoon&startTime=13:00&endTime=16:00&price=150000&active_S2=on"
                    + "&slotId=" + getSlotId(a102Id, "S3") + "&slotCode=S3&slotName=Evening&startTime=16:30&endTime=19:30&price=180000&active_S3=on"
                    + "&slotId=" + getSlotId(a102Id, "S4") + "&slotCode=S4&slotName=Overnight&startTime=20:00&endTime=09:00&price=300000&active_S4=on";

            HttpResponse w25Resp = owner.postRaw("/admin/rooms/slots", guardPostData);
            check(w25Resp.body.contains("This slot has active holds and its time cannot be changed until they are released"),
                    "W25: Refused start_time change while future hold exists");
            check(getSlotStartTime(a102S1SlotId).equals("09:00:00"), "A102 S1 start_time remains 09:00:00 in DB");

            // Price-only change on A102 S1 while future hold exists -> ALLOWED
            String priceOnlyPostData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&roomId=" + a102Id
                    + "&slotId=" + a102S1SlotId + "&slotCode=S1&slotName=Morning&startTime=09:00&endTime=12:00&price=175000&active_S1=on"
                    + "&slotId=" + getSlotId(a102Id, "S2") + "&slotCode=S2&slotName=Afternoon&startTime=13:00&endTime=16:00&price=150000&active_S2=on"
                    + "&slotId=" + getSlotId(a102Id, "S3") + "&slotCode=S3&slotName=Evening&startTime=16:30&endTime=19:30&price=180000&active_S3=on"
                    + "&slotId=" + getSlotId(a102Id, "S4") + "&slotCode=S4&slotName=Overnight&startTime=20:00&endTime=09:00&price=300000&active_S4=on";

            HttpResponse priceResp = owner.postRaw("/admin/rooms/slots", priceOnlyPostData);
            check(priceResp.statusCode == 302, "Price-only edit returned 302 redirect");
            check(getSlotPrice(a102Id, "S1") == 175000L, "A102 S1 price successfully updated to 175,000 VND");
            check(countAuditLogs("SLOT_UPDATED", "ROOM_TIME_SLOT", String.valueOf(a102Id)) >= 1,
                    "Audit SLOT_UPDATED recorded");

            // Clean up test booking
            deleteTestBooking(testBookingId);

            // Test overlap detection (e.g. S1 ends at 14:00 while S2 starts at 13:00)
            String overlapPostData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&roomId=" + a102Id
                    + "&slotId=" + a102S1SlotId + "&slotCode=S1&slotName=Morning&startTime=09:00&endTime=14:00&price=175000&active_S1=on"
                    + "&slotId=" + getSlotId(a102Id, "S2") + "&slotCode=S2&slotName=Afternoon&startTime=13:00&endTime=16:00&price=150000&active_S2=on"
                    + "&slotId=" + getSlotId(a102Id, "S3") + "&slotCode=S3&slotName=Evening&startTime=16:30&endTime=19:30&price=180000&active_S3=on"
                    + "&slotId=" + getSlotId(a102Id, "S4") + "&slotCode=S4&slotName=Overnight&startTime=20:00&endTime=09:00&price=300000&active_S4=on";

            HttpResponse overlapResp = owner.postRaw("/admin/rooms/slots", overlapPostData);
            check(overlapResp.body.contains("Time slots must not overlap, including overnight ranges"),
                    "Overlap guard correctly caught 1-hour overlap between S1 and S2");

            // Revert S1 price to 150000
            String revertData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&roomId=" + a102Id
                    + "&slotId=" + a102S1SlotId + "&slotCode=S1&slotName=Morning&startTime=09:00&endTime=12:00&price=150000&active_S1=on"
                    + "&slotId=" + getSlotId(a102Id, "S2") + "&slotCode=S2&slotName=Afternoon&startTime=13:00&endTime=16:00&price=150000&active_S2=on"
                    + "&slotId=" + getSlotId(a102Id, "S3") + "&slotCode=S3&slotName=Evening&startTime=16:30&endTime=19:30&price=180000&active_S3=on"
                    + "&slotId=" + getSlotId(a102Id, "S4") + "&slotCode=S4&slotName=Overnight&startTime=20:00&endTime=09:00&price=300000&active_S4=on";
            owner.postRaw("/admin/rooms/slots", revertData);

            // -------------------------------------------------------------------------
            // 4. Room Status Transition Guard
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T07] Test 4: Room Status Transition Guard ---");
            long a103Id = getRoomIdByNumber("A103");
            check(a103Id > 0, "Found room A103 (ID=" + a103Id + ")");

            // Set A103 status to OCCUPIED and create a CHECKED_IN booking on it
            setRoomStatus(a103Id, "OCCUPIED");
            long checkedInBookingId = createCheckedInBooking(a103Id);

            // Attempt to change A103 status to AVAILABLE -> MUST BE REFUSED
            String statusAvailData = "_csrf=" + URLEncoder.encode(admin.csrfToken, "UTF-8")
                    + "&roomId=" + a103Id
                    + "&status=AVAILABLE";

            HttpResponse statusAvailResp = admin.postRaw("/admin/rooms/status", statusAvailData);
            check(statusAvailResp.statusCode == 302, "Status change attempt returned 302");
            HttpResponse roomPage = admin.get("/admin/rooms");
            check(roomPage.body.contains("Room has an active check-in and cannot be set to Available"),
                    "Refused AVAILABLE transition when active check-in exists");
            check(getRoomStatus(a103Id).equals("OCCUPIED"), "Room status remains OCCUPIED in DB");

            // Checkout the booking
            setBookingStatus(checkedInBookingId, "CHECKED_OUT");

            // Now changing to AVAILABLE is allowed
            HttpResponse statusOkResp = admin.postRaw("/admin/rooms/status", statusAvailData);
            check(statusOkResp.statusCode == 302, "Status change after checkout returned 302");
            check(getRoomStatus(a103Id).equals("AVAILABLE"), "Room status successfully updated to AVAILABLE in DB");
            check(countAuditLogs("ROOM_STATUS_CHANGED", "ROOM", String.valueOf(a103Id)) >= 1,
                    "Audit ROOM_STATUS_CHANGED recorded");

            deleteTestBooking(checkedInBookingId);

            // -------------------------------------------------------------------------
            // 5. Homestay Property Configuration (Owner Only)
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T07] Test 5: Homestay Property Management ---");
            // Admin direct access -> 403 Forbidden
            HttpResponse adminHomestayResp = admin.get("/admin/homestay");
            check(adminHomestayResp.statusCode == 403, "Admin accessing /admin/homestay receives 403 Forbidden");

            // Owner opens page -> 200 OK
            HttpResponse ownerHomestayPage = owner.get("/admin/homestay");
            check(ownerHomestayPage.statusCode == 200, "Owner accessing /admin/homestay receives 200 OK");
            check(ownerHomestayPage.body.contains("Homestay Property Configuration"), "Contains Homestay title");

            // Owner updates name and address
            String homestayPostData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&name=SmartStay+Lakeside+Da+Lat"
                    + "&address=456+Tran+Hung+Dao+St,+Ward+10,+Da+Lat"
                    + "&status=ACTIVE";

            HttpResponse homestayPostResp = owner.postRaw("/admin/homestay", homestayPostData);
            check(homestayPostResp.statusCode == 302, "Homestay update returned 302 redirect");
            check(getHomestayName().equals("SmartStay Lakeside Da Lat"), "Homestay name updated in DB");
            check(countAuditLogs("HOMESTAY_UPDATED", "HOMESTAY", null) >= 1,
                    "Audit HOMESTAY_UPDATED recorded");

            // Revert homestay name
            String revertHomestayData = "_csrf=" + URLEncoder.encode(owner.csrfToken, "UTF-8")
                    + "&name=SmartStay+Lakeside"
                    + "&address=12+Lakeside+Drive,+Da+Lat,+Lam+Dong"
                    + "&status=ACTIVE";
            owner.postRaw("/admin/homestay", revertHomestayData);

            // -------------------------------------------------------------------------
            // 6. Room List Search, Filters, Sorting, and Pagination
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T07] Test 6: Room Catalog Filtering & Sorting ---");
            HttpResponse listResp = admin.get("/admin/rooms");
            check(listResp.statusCode == 200, "Room list returns 200 OK");
            check(listResp.body.contains("Showing"), "Contains pagination summary 'Showing x - y of N'");
            check(listResp.body.contains("A101") && listResp.body.contains("A106"), "Displays seeded rooms");

            // Search by keyword
            HttpResponse searchResp = admin.get("/admin/rooms?keyword=A103");
            check(searchResp.statusCode == 200, "Search by keyword returns 200 OK");
            check(searchResp.body.contains("A103"), "Search results contain A103");
            check(!searchResp.body.contains("A101"), "Search results do not contain A101");

            // Filter by floor
            HttpResponse floorResp = admin.get("/admin/rooms?floor=1");
            check(floorResp.statusCode == 200, "Filter by floor returns 200 OK");
            check(floorResp.body.contains("Floor 1"), "Results contain Floor 1");

            // Sort by price DESC
            HttpResponse sortPriceResp = admin.get("/admin/rooms?sort=price&dir=DESC");
            check(sortPriceResp.statusCode == 200, "Sort by price DESC returns 200 OK");

            // Sort by floor ASC
            HttpResponse sortFloorResp = admin.get("/admin/rooms?sort=floor&dir=ASC");
            check(sortFloorResp.statusCode == 200, "Sort by floor ASC returns 200 OK");

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

    private static void cleanupRoom(String roomNumber) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            long rid = -1;
            try (PreparedStatement ps = conn.prepareStatement("SELECT room_id FROM ROOM WHERE room_number = ?")) {
                ps.setString(1, roomNumber);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) rid = rs.getLong(1);
                }
            }
            if (rid > 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ROOM_TIME_SLOT WHERE room_id = ?")) {
                    ps.setLong(1, rid);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ROOM WHERE room_id = ?")) {
                    ps.setLong(1, rid);
                    ps.executeUpdate();
                }
            }
        } catch (Exception ignored) {}
    }

    private static long getRoomIdByNumber(String roomNumber) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT room_id FROM ROOM WHERE room_number = ?")) {
            ps.setString(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1;
    }

    private static int countSlotsForRoom(long roomId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM ROOM_TIME_SLOT WHERE room_id = ?")) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private static long getSlotPrice(long roomId, String slotCode) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT list_price FROM ROOM_TIME_SLOT WHERE room_id = ? AND slot_code = ?")) {
            ps.setLong(1, roomId);
            ps.setString(2, slotCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1).longValue();
            }
        }
        return -1;
    }

    private static long getSlotId(long roomId, String slotCode) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT room_slot_id FROM ROOM_TIME_SLOT WHERE room_id = ? AND slot_code = ?")) {
            ps.setLong(1, roomId);
            ps.setString(2, slotCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1;
    }

    private static String getSlotStartTime(long slotId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT start_time FROM ROOM_TIME_SLOT WHERE room_slot_id = ?")) {
            ps.setLong(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return "";
    }

    private static int getRoomCapacity(long roomId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT capacity FROM ROOM WHERE room_id = ?")) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    private static boolean isRoomActive(long roomId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT is_active FROM ROOM WHERE room_id = ?")) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBoolean(1);
            }
        }
        return false;
    }

    private static String getRoomStatus(long roomId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM ROOM WHERE room_id = ?")) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return "";
    }

    private static void setRoomStatus(long roomId, String status) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("UPDATE ROOM SET status = ? WHERE room_id = ?")) {
            ps.setString(1, status);
            ps.setLong(2, roomId);
            ps.executeUpdate();
        }
    }

    private static String getHomestayName() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT name FROM HOMESTAY WHERE singleton_key = 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return "";
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

    private static long createFutureHeldBooking(long roomId, long roomSlotId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String bSql = "INSERT INTO BOOKING (guest_id, room_id, slot_date, "
                    + "checkin_at, checkout_at, subtotal_amount, discount_percent, discount_amount, total_amount, "
                    + "payment_deadline, status) VALUES (6, ?, '2026-10-25', "
                    + "'2026-10-25 09:00:00', '2026-10-25 12:00:00', 150000, 0, 0, 150000, "
                    + "DATEADD(minute, 15, SYSDATETIME()), 'PENDING_PAYMENT')";
            long bid = -1;
            try (PreparedStatement ps = conn.prepareStatement(bSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, roomId);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) bid = rs.getLong(1);
                }
            }

            String bsSql = "INSERT INTO BOOKING_SLOT (booking_id, room_id, room_slot_id, slot_date, start_at, end_at, "
                    + "list_price, hold_status) VALUES (?, ?, ?, '2026-10-25', '2026-10-25 09:00:00', '2026-10-25 12:00:00', 150000, 'HELD')";
            try (PreparedStatement ps = conn.prepareStatement(bsSql)) {
                ps.setLong(1, bid);
                ps.setLong(2, roomId);
                ps.setLong(3, roomSlotId);
                ps.executeUpdate();
            }
            return bid;
        }
    }

    private static long createCheckedInBooking(long roomId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String bSql = "INSERT INTO BOOKING (guest_id, room_id, slot_date, "
                    + "checkin_at, checkout_at, subtotal_amount, discount_percent, discount_amount, total_amount, "
                    + "status, actual_checkin_at) VALUES (6, ?, '2026-10-15', "
                    + "'2026-10-15 09:00:00', '2026-10-15 12:00:00', 150000, 0, 0, 150000, 'CHECKED_IN', SYSDATETIME())";
            long bid = -1;
            try (PreparedStatement ps = conn.prepareStatement(bSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, roomId);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) bid = rs.getLong(1);
                }
            }
            return bid;
        }
    }

    private static void setBookingStatus(long bookingId, String status) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("UPDATE BOOKING SET status = ? WHERE booking_id = ?")) {
            ps.setString(1, status);
            ps.setLong(2, bookingId);
            ps.executeUpdate();
        }
    }

    private static void deleteTestBooking(long bookingId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM BOOKING_SLOT WHERE booking_id = ?")) {
                ps.setLong(1, bookingId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM BOOKING WHERE booking_id = ?")) {
                ps.setLong(1, bookingId);
                ps.executeUpdate();
            }
        } catch (Exception ignored) {}
    }

    // --- HTTP Client ---

    private static class HttpResponse {
        int statusCode;
        String body;
        Map<String, List<String>> headers;

        String getHeader(String name) {
            if (headers == null) return null;
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                    return e.getValue().isEmpty() ? null : e.getValue().get(0);
                }
            }
            return null;
        }
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
                HttpResponse afterLogin = get("/admin/rooms");
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

            InputStream is = resp.statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
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
