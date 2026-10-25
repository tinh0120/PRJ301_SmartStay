package tools;

import util.DBUtil;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automated end-to-end acceptance test for T04.
 */
public class T04AcceptanceTest {

    private static final String BASE_URL = "http://localhost:8080/SmartStay";
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("        STARTING T04 ACCEPTANCE VERIFICATION      ");
        System.out.println("==================================================");

        try {
            // Verify DB connectivity
            try (Connection conn = DBUtil.getConnection()) {
                System.out.println("[DB] Successfully connected to SmartStayDB.");
            }

            // Create HTTP sessions
            HttpSessionHelper guest1 = new HttpSessionHelper(BASE_URL);
            boolean g1Login = guest1.login("guest1@smartstay.local", "12345");
            check(g1Login, "Guest 1 login successful");

            HttpSessionHelper guest2 = new HttpSessionHelper(BASE_URL);
            boolean g2Login = guest2.login("guest2@smartstay.local", "12345");
            check(g2Login, "Guest 2 login successful");

            // Pick a clean future test date
            LocalDate testDate1 = LocalDate.now().plusDays(10);
            cleanupSlots(1L, testDate1);

            // -------------------------------------------------------------------------
            // Test 1: Happy path booking creation (Checklist 2)
            // A101 S1+S2 -> PENDING_PAYMENT, amounts correct, deadline ~ 15m, slots HELD, BOOKING_CREATED audit
            // -------------------------------------------------------------------------
            System.out.println("\n--- Test 1: Standard Booking Creation (S1+S2) ---");
            Map<String, String> params = new HashMap<>();
            params.put("roomId", "1");
            params.put("slotDate", testDate1.toString());
            params.put("slotCodes", "S1");

            // Using raw post with multiple slotCodes
            String postData = "_csrf=" + URLEncoder.encode(guest1.csrfToken, "UTF-8")
                    + "&roomId=1"
                    + "&slotDate=" + testDate1.toString()
                    + "&slotCodes=S1&slotCodes=S2";

            HttpResponse resp1 = guest1.postRaw("/guest/bookings", postData);
            check(resp1.statusCode == 302, "Booking create returned 302 redirect (PRG)");
            String redirectUrl = resp1.getHeader("Location");
            System.out.println("Redirected to: " + redirectUrl);
            check(redirectUrl != null && redirectUrl.contains("/guest/bookings/detail?id="), "Redirect targets detail page");

            long bookingId1 = extractIdFromUrl(redirectUrl);
            check(bookingId1 > 0, "Extracted valid booking ID: " + bookingId1);

            // Verify in DB
            BookingRecord b1 = loadBooking(bookingId1);
            check(b1 != null, "Booking row exists in DB");
            check("PENDING_PAYMENT".equals(b1.status), "Booking status is PENDING_PAYMENT (got: " + b1.status + ")");
            check(b1.bookingCode != null && b1.bookingCode.startsWith("BK"), "Booking code generated: " + b1.bookingCode);
            check(b1.subtotal == 300000L, "Subtotal is 300,000 VND (got: " + b1.subtotal + ")");
            check(b1.discountPercent == 10, "Discount percent is 10% (got: " + b1.discountPercent + "%)");
            check(b1.discountAmount == 30000L, "Discount amount is 30,000 VND (got: " + b1.discountAmount + ")");
            check(b1.total == 270000L, "Total is 270,000 VND (got: " + b1.total + ")");

            long minutesToDeadline = ChronoUnit.MINUTES.between(LocalDateTime.now(), b1.paymentDeadline.toLocalDateTime());
            check(minutesToDeadline >= 14 && minutesToDeadline <= 16, "Payment deadline is ~15 minutes (got: " + minutesToDeadline + "m)");

            int heldSlotsCount = countHeldSlots(1L, testDate1);
            check(heldSlotsCount == 2, "2 slots are HELD for Room 1 on date (got: " + heldSlotsCount + ")");

            int auditCreatedCount = countAuditLogs("BOOKING_CREATED", "BOOKING", String.valueOf(bookingId1));
            check(auditCreatedCount == 1, "BOOKING_CREATED audit log recorded (count: " + auditCreatedCount + ")");

            // Verify detail view page renders
            HttpResponse detailResp = guest1.get("/guest/bookings/detail?id=" + bookingId1);
            check(detailResp.statusCode == 200, "Booking detail page returns 200 OK");
            check(detailResp.body.contains("PENDING_PAYMENT"), "Detail page shows PENDING_PAYMENT badge");
            check(detailResp.body.contains("Pay now"), "Detail page shows Pay now button");
            check(detailResp.body.contains("Cancel booking"), "Detail page shows Cancel booking button");

            // -------------------------------------------------------------------------
            // Test 2: Slot Collision / Unavailable (Checklist 3 & W05)
            // guest2 attempts to book the same slot -> failure with SLOT_UNAVAILABLE message
            // -------------------------------------------------------------------------
            System.out.println("\n--- Test 2: Slot Collision Guard (W05) ---");
            String postData2 = "_csrf=" + URLEncoder.encode(guest2.csrfToken, "UTF-8")
                    + "&roomId=1"
                    + "&slotDate=" + testDate1.toString()
                    + "&slotCodes=S1";

            HttpResponse resp2 = guest2.postRaw("/guest/bookings", postData2);
            check(resp2.statusCode == 302, "Collision attempt redirected with 302");
            String colRedirect = resp2.getHeader("Location");
            check(colRedirect != null && colRedirect.contains("/rooms/detail"), "Redirected back to room detail page");

            // Follow redirect and check flash message
            HttpResponse colRoomPage = guest2.get(colRedirect.substring(colRedirect.indexOf("/rooms/detail")));
            check(colRoomPage.body.contains("One or more selected slots are no longer available."),
                    "Flash message displayed: 'One or more selected slots are no longer available.'");

            // Verify held slots remains 2 (no corrupt extra rows)
            int heldAfterCollision = countHeldSlots(1L, testDate1);
            check(heldAfterCollision == 2, "Held slot count remains exactly 2");

            // -------------------------------------------------------------------------
            // Test 3: Non-consecutive slots S1+S3 (Checklist 4 & W07)
            // -------------------------------------------------------------------------
            System.out.println("\n--- Test 3: Non-Consecutive Slots Guard (W07) ---");
            LocalDate testDate2 = LocalDate.now().plusDays(11);
            cleanupSlots(1L, testDate2);

            String postData3 = "_csrf=" + URLEncoder.encode(guest1.csrfToken, "UTF-8")
                    + "&roomId=1"
                    + "&slotDate=" + testDate2.toString()
                    + "&slotCodes=S1&slotCodes=S3";

            HttpResponse resp3 = guest1.postRaw("/guest/bookings", postData3);
            check(resp3.statusCode == 302, "Non-consecutive attempt redirected with 302");
            String ncRedirect = resp3.getHeader("Location");
            check(ncRedirect != null && ncRedirect.contains("/rooms/detail"), "Redirected back to room detail page");

            HttpResponse ncRoomPage = guest1.get(ncRedirect.substring(ncRedirect.indexOf("/rooms/detail")));
            check(ncRoomPage.body.contains("Please select consecutive time slots."),
                    "Flash message displayed: 'Please select consecutive time slots.'");

            int ncHeldCount = countHeldSlots(1L, testDate2);
            check(ncHeldCount == 0, "No slots were held for invalid non-consecutive attempt");

            // -------------------------------------------------------------------------
            // Test 4: Parameter Tampering Immunity (Checklist 5 & W08)
            // POST crafted with total=1&discountPercent=99
            // -------------------------------------------------------------------------
            System.out.println("\n--- Test 4: Parameter Tampering Resistance (W08) ---");
            LocalDate testDate3 = LocalDate.now().plusDays(12);
            cleanupSlots(1L, testDate3);

            String postData4 = "_csrf=" + URLEncoder.encode(guest1.csrfToken, "UTF-8")
                    + "&roomId=1"
                    + "&slotDate=" + testDate3.toString()
                    + "&slotCodes=S1&slotCodes=S2"
                    + "&total=1"
                    + "&discountPercent=99"
                    + "&subtotal=1";

            HttpResponse resp4 = guest1.postRaw("/guest/bookings", postData4);
            check(resp4.statusCode == 302, "Tampered post redirected with 302");
            long bookingId4 = extractIdFromUrl(resp4.getHeader("Location"));
            BookingRecord b4 = loadBooking(bookingId4);
            check(b4 != null, "Booking 4 created successfully");
            check(b4.total == 270000L, "Stored total is 270,000 VND (tampered 1 VND ignored!)");
            check(b4.discountPercent == 10, "Stored discount is 10% (tampered 99% ignored!)");

            // -------------------------------------------------------------------------
            // Test 5: Booking Cancellation (Checklist 6)
            // Cancel booking 4 with reason -> status CANCELLED, slots RELEASED, AUDIT logged
            // -------------------------------------------------------------------------
            System.out.println("\n--- Test 5: Booking Cancellation & Re-cancel Guard ---");
            String cancelData = "_csrf=" + URLEncoder.encode(guest1.csrfToken, "UTF-8")
                    + "&id=" + bookingId4
                    + "&reason=" + URLEncoder.encode("Plans changed for trip", "UTF-8");

            HttpResponse cancelResp = guest1.postRaw("/guest/bookings/cancel", cancelData);
            check(cancelResp.statusCode == 302, "Cancellation returned 302 redirect");

            BookingRecord b4AfterCancel = loadBooking(bookingId4);
            check("CANCELLED".equals(b4AfterCancel.status), "Booking status updated to CANCELLED");
            check("Plans changed for trip".equals(b4AfterCancel.cancelReason), "Cancel reason stored correctly");

            int heldAfterCancel = countHeldSlots(1L, testDate3);
            check(heldAfterCancel == 0, "Slots were RELEASED (0 slots held)");

            int auditCancelCount = countAuditLogs("BOOKING_CANCELLED", "BOOKING", String.valueOf(bookingId4));
            check(auditCancelCount == 1, "BOOKING_CANCELLED audit log recorded");

            // Try to cancel again -> must fail with NOT_CANCELLABLE
            HttpResponse reCancelResp = guest1.postRaw("/guest/bookings/cancel", cancelData);
            check(reCancelResp.statusCode == 302, "Re-cancel returned 302 redirect");
            HttpResponse reCancelDetail = guest1.get("/guest/bookings/detail?id=" + bookingId4);
            check(reCancelDetail.body.contains("This booking can no longer be cancelled."),
                    "Flash error on re-cancel: 'This booking can no longer be cancelled.'");

            // -------------------------------------------------------------------------
            // Test 6: Cross-Guest Isolation (Checklist 8 & W14)
            // guest2 tries to view guest1's booking -> 404
            // -------------------------------------------------------------------------
            System.out.println("\n--- Test 6: Cross-Guest Security (W14) ---");
            HttpResponse crossResp = guest2.get("/guest/bookings/detail?id=" + bookingId1);
            check(crossResp.statusCode == 404, "Guest 2 accessing Guest 1 booking receives 404 Not Found (got: " + crossResp.statusCode + ")");

            // -------------------------------------------------------------------------
            // Test 7: Booking List, Filters, and Sorting (Checklist 8)
            // -------------------------------------------------------------------------
            System.out.println("\n--- Test 7: Bookings List Page with Filters & Sort ---");
            HttpResponse listResp = guest1.get("/guest/bookings");
            check(listResp.statusCode == 200, "Bookings list page returns 200 OK");
            check(listResp.body.contains("My bookings"), "List page contains 'My bookings' header");
            check(listResp.body.contains(b1.bookingCode), "List page contains booking code " + b1.bookingCode);

            // Filter by status=CANCELLED
            HttpResponse cancelledFilterResp = guest1.get("/guest/bookings?status=CANCELLED");
            check(cancelledFilterResp.statusCode == 200, "Filter by status=CANCELLED returns 200 OK");
            check(cancelledFilterResp.body.contains(b4.bookingCode), "Cancelled filter contains " + b4.bookingCode);

            // Filter with invalid date range
            HttpResponse badDateFilterResp = guest1.get("/guest/bookings?from=invalid-date");
            check(badDateFilterResp.statusCode == 200, "Bad date filter handled gracefully (200 OK)");
            check(badDateFilterResp.body.contains("Invalid &#39;from&#39; date format"), "Field error displayed for bad date filter");

            // Clean up test bookings
            cleanupBooking(bookingId1);
            cleanupBooking(bookingId4);

            System.out.println("\n==================================================");
            System.out.println(String.format("TEST RESULTS: %d PASSED, %d FAILED", testsPassed, testsFailed));
            System.out.println("==================================================");

            if (testsFailed > 0) {
                System.exit(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

    private static void check(boolean condition, String description) {
        if (condition) {
            System.out.println("  [PASS] " + description);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + description);
            testsFailed++;
        }
    }

    private static long extractIdFromUrl(String url) {
        if (url == null) return -1;
        Pattern p = Pattern.compile("id=(\\d+)");
        Matcher m = p.matcher(url);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }
        return -1;
    }

    private static void cleanupSlots(long roomId, LocalDate date) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM BOOKING_SLOT WHERE room_id = ? AND slot_date = ?")) {
            ps.setLong(1, roomId);
            ps.setDate(2, java.sql.Date.valueOf(date));
            ps.executeUpdate();
        }
    }

    private static void cleanupBooking(long bookingId) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM BOOKING_SLOT WHERE booking_id = ?")) {
                ps.setLong(1, bookingId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM BOOKING WHERE booking_id = ?")) {
                ps.setLong(1, bookingId);
                ps.executeUpdate();
            }
        }
    }

    private static int countHeldSlots(long roomId, LocalDate date) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM BOOKING_SLOT WHERE room_id = ? AND slot_date = ? AND hold_status = 'HELD'")) {
            ps.setLong(1, roomId);
            ps.setDate(2, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private static int countAuditLogs(String action, String entityType, String entityId) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM AUDIT_LOG WHERE action = ? AND entity_type = ? AND entity_id = ?")) {
            ps.setString(1, action);
            ps.setString(2, entityType);
            ps.setString(3, entityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private static class BookingRecord {
        long bookingId;
        String bookingCode;
        String status;
        long subtotal;
        int discountPercent;
        long discountAmount;
        long total;
        Timestamp paymentDeadline;
        String cancelReason;
    }

    private static BookingRecord loadBooking(long bookingId) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT booking_id, booking_code, status, subtotal_amount AS subtotal, discount_percent, discount_amount, total_amount, payment_deadline, cancel_reason "
                     + "FROM BOOKING WHERE booking_id = ?")) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BookingRecord b = new BookingRecord();
                    b.bookingId = rs.getLong("booking_id");
                    b.bookingCode = rs.getString("booking_code");
                    b.status = rs.getString("status");
                    b.subtotal = rs.getBigDecimal("subtotal").longValue();
                    b.discountPercent = rs.getInt("discount_percent");
                    b.discountAmount = rs.getBigDecimal("discount_amount").longValue();
                    b.total = rs.getBigDecimal("total_amount").longValue();
                    b.paymentDeadline = rs.getTimestamp("payment_deadline");
                    b.cancelReason = rs.getString("cancel_reason");
                    return b;
                }
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // HTTP helper handling Cookies and CSRF
    // -------------------------------------------------------------------------
    private static class HttpResponse {
        int statusCode;
        String body;
        Map<String, List<String>> headers;

        String getHeader(String name) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (name.equalsIgnoreCase(e.getKey()) && !e.getValue().isEmpty()) {
                    return e.getValue().get(0);
                }
            }
            return null;
        }
    }

    private static class HttpSessionHelper {
        private final String base;
        private String sessionCookie = null;
        String csrfToken = null;

        HttpSessionHelper(String base) {
            this.base = base;
        }

        boolean login(String email, String password) throws Exception {
            HttpResponse loginPage = get("/login");
            if (loginPage.statusCode != 200) return false;

            Matcher m = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(loginPage.body);
            if (!m.find()) return false;
            csrfToken = m.group(1);

            String data = "email=" + URLEncoder.encode(email, "UTF-8")
                    + "&password=" + URLEncoder.encode(password, "UTF-8")
                    + "&_csrf=" + URLEncoder.encode(csrfToken, "UTF-8");

            HttpResponse loginPost = postRaw("/login", data);
            if (loginPost.statusCode == 302) {
                // Refresh CSRF after login
                HttpResponse afterLogin = get("/guest/bookings");
                Matcher m2 = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(afterLogin.body);
                if (m2.find()) {
                    csrfToken = m2.group(1);
                }
                return true;
            }
            return false;
        }

        HttpResponse get(String path) throws Exception {
            URL url = new URL(path.startsWith("http") ? path : (base + path));
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
            URL url = new URL(path.startsWith("http") ? path : (base + path));
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
            } else {
                resp.body = "";
            }
            return resp;
        }
    }
}
