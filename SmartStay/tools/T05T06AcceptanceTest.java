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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automated end-to-end acceptance test for T05 and T06.
 */
public class T05T06AcceptanceTest {

    private static final String BASE_URL = "http://localhost:8080/SmartStay";
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("    STARTING T05 & T06 ACCEPTANCE VERIFICATION    ");
        System.out.println("==================================================");

        try {
            try (Connection conn = DBUtil.getConnection()) {
                System.out.println("[DB] Connected to SmartStayDB successfully.");
            }

            HttpSessionHelper guest1 = new HttpSessionHelper(BASE_URL);
            check(guest1.login("guest1@smartstay.local", "12345"), "Guest 1 login successful");

            HttpSessionHelper guest2 = new HttpSessionHelper(BASE_URL);
            check(guest2.login("guest2@smartstay.local", "12345"), "Guest 2 login successful");

            // =========================================================================
            // PART 1: T05 - PAYMENT & CREDENTIAL
            // =========================================================================
            System.out.println("\n--- [T05] Test 1: Standard Payment Creation & Success (W09) ---");
            LocalDate testDate1 = LocalDate.now().plusDays(20);
            cleanupDateSlots(1L, testDate1);

            // 1. Create a booking (Room 1, S1+S2)
            long bookingId1 = createTestBooking(guest1, 1L, testDate1, "S1", "S2");
            check(bookingId1 > 0, "Created booking #" + bookingId1);

            // 2. Click "Pay now" -> POST /guest/bookings/pay
            String payData = "_csrf=" + URLEncoder.encode(guest1.csrfToken, "UTF-8")
                    + "&id=" + bookingId1;
            HttpResponse payResp = guest1.postRaw("/guest/bookings/pay", payData);
            check(payResp.statusCode == 302, "POST /guest/bookings/pay returned 302 redirect");
            String checkoutLocation = payResp.getHeader("Location");
            check(checkoutLocation != null && checkoutLocation.contains("/mock-payos/checkout?orderCode="),
                    "Redirected to mock checkout: " + checkoutLocation);

            long orderCode1 = extractOrderCodeFromUrl(checkoutLocation);
            check(orderCode1 > 0, "Extracted order code: " + orderCode1);

            PaymentRecord prInitial1 = loadPaymentByOrder(orderCode1);
            long expectedAmount1 = (prInitial1 != null) ? prInitial1.amount : 270000L;
            String expectedAmountStr = util.ElFunctions.money(expectedAmount1);

            // 3. Inspect mock checkout page
            HttpResponse checkoutPage = guest1.get(checkoutLocation);
            check(checkoutPage.statusCode == 200, "Mock checkout page returns 200 OK");
            check(checkoutPage.body.contains("PayOS Gateway"), "Contains PayOS Gateway header");
            check(checkoutPage.body.contains(expectedAmountStr), "Displays exact total amount (" + expectedAmountStr + ")");
            check(checkoutPage.body.contains("timer-display"), "Contains countdown timer element");
            check(checkoutPage.body.contains("MOCK"), "Contains mock SVG QR code");

            // 4. Before paying, open result page -> verify "Verifying Payment..." state (W12)
            HttpResponse earlyResultResp = guest1.get("/guest/payment/result?bookingId=" + bookingId1);
            check(earlyResultResp.statusCode == 200, "Early result page returns 200 OK");
            check(earlyResultResp.body.contains("Verifying Payment"), "Shows 'Verifying Payment...' before payment completed (W12)");

            // 5. Simulate Pay successfully -> POST /mock-payos/simulate
            String simSuccessData = "_csrf=" + URLEncoder.encode(guest1.csrfToken, "UTF-8")
                    + "&orderCode=" + orderCode1
                    + "&amount=" + expectedAmount1
                    + "&result=SUCCESS";
            HttpResponse simResp = guest1.postRaw("/mock-payos/simulate", simSuccessData);
            check(simResp.statusCode == 302, "Simulate success returned 302 redirect");
            check(simResp.getHeader("Location").contains("/guest/payment/result?bookingId=" + bookingId1),
                    "Redirected to payment result page");

            // Verify DB state
            PaymentRecord pr1 = loadPaymentByOrder(orderCode1);
            check(pr1 != null, "Payment row exists in DB");
            check("PAID".equals(pr1.status), "Payment status is PAID");
            check(pr1.amount == expectedAmount1, "Payment amount is " + expectedAmountStr);
            check(pr1.paidAt != null, "Payment paid_at is populated");

            String bookingStatus1 = getBookingStatus(bookingId1);
            check("CONFIRMED".equals(bookingStatus1), "Booking status is now CONFIRMED");

            // Verify credential created
            CredentialRecord cr1 = loadCredentialForBooking(bookingId1);
            check(cr1 != null, "Access credential created in DB");
            check("PENDING".equals(cr1.status), "Credential status is PENDING (until stay starts)");
            check("GUEST_PIN".equals(cr1.credentialType), "Credential type is GUEST_PIN");
            check(cr1.pinCiphertext != null && cr1.pinCiphertext.length > 0, "pin_ciphertext is non-null binary");
            check(cr1.pinHmac != null && cr1.pinHmac.length == 32, "pin_hmac is 32-byte HMAC-SHA256");

            // Verify audit logs
            check(countAuditLogs("PAYMENT_PAID", "PAYMENT", String.valueOf(pr1.paymentId)) == 1, "Audit PAYMENT_PAID recorded");
            check(countAuditLogs("CREDENTIAL_CREATED", "ACCESS_CREDENTIAL", String.valueOf(cr1.credentialId)) == 1, "Audit CREDENTIAL_CREATED recorded");

            // 6. Test polling endpoint
            HttpResponse statusPollResp = guest1.get("/guest/payment/status?bookingId=" + bookingId1);
            check(statusPollResp.statusCode == 200, "Polling endpoint returns 200 OK");
            check(statusPollResp.body.contains("\"paymentStatus\":\"PAID\""), "Polling response contains paymentStatus PAID");
            check(statusPollResp.body.contains("\"bookingStatus\":\"CONFIRMED\""), "Polling response contains bookingStatus CONFIRMED");

            // Result page now shows Confirmed
            HttpResponse finalResultResp = guest1.get("/guest/payment/result?bookingId=" + bookingId1);
            check(finalResultResp.body.contains("Payment Confirmed!"), "Result page shows Payment Confirmed!");

            // -------------------------------------------------------------------------
            // [T05] Test 2: Replay of Success POST (W10)
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T05] Test 2: Replay Attack Guard (W10) ---");
            HttpResponse replayResp = guest1.postRaw("/mock-payos/simulate", simSuccessData);
            check(replayResp.statusCode == 302, "Replay returned 302");
            check(countWebhookLogs(orderCode1, "DUPLICATE") >= 1, "PAYMENT_WEBHOOK_LOG recorded DUPLICATE process_status");
            check(countCredentialsForBooking(bookingId1) == 1, "Still exactly 1 credential row (no duplicate credential issued!)");

            // -------------------------------------------------------------------------
            // [T05] Test 3: Tampered Amount in simulate (W11)
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T05] Test 3: Tampered Amount Guard (W11) ---");
            LocalDate testDate2 = LocalDate.now().plusDays(21);
            cleanupDateSlots(1L, testDate2);
            long bookingId2 = createTestBooking(guest1, 1L, testDate2, "S1", "S2");

            String payData2 = "_csrf=" + URLEncoder.encode(guest1.csrfToken, "UTF-8") + "&id=" + bookingId2;
            HttpResponse payResp2 = guest1.postRaw("/guest/bookings/pay", payData2);
            long orderCode2 = extractOrderCodeFromUrl(payResp2.getHeader("Location"));

            // Submit with tampered amount 100 VND
            String simTamperedData = "_csrf=" + URLEncoder.encode(guest1.csrfToken, "UTF-8")
                    + "&orderCode=" + orderCode2
                    + "&amount=100"
                    + "&result=SUCCESS";
            HttpResponse tamperedResp = guest1.postRaw("/mock-payos/simulate", simTamperedData);
            check(tamperedResp.statusCode == 302, "Tampered post returned 302");

            PaymentRecord pr2 = loadPaymentByOrder(orderCode2);
            check("PENDING".equals(pr2.status), "Payment status remains PENDING (not altered!)");
            check(countWebhookLogs(orderCode2, "AMOUNT_MISMATCH") == 1, "PAYMENT_WEBHOOK_LOG recorded AMOUNT_MISMATCH");
            check(countCredentialsForBooking(bookingId2) == 0, "No credential issued for tampered payment");

            // -------------------------------------------------------------------------
            // [T05] Test 4: Late Payment / Expired Link (W13)
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T05] Test 4: Late Payment Handling (W13) ---");
            // Set expires_at in the past for payment 2
            expirePaymentInPast(orderCode2);

            String simLateData = "_csrf=" + URLEncoder.encode(guest1.csrfToken, "UTF-8")
                    + "&orderCode=" + orderCode2
                    + "&amount=" + pr2.amount
                    + "&result=SUCCESS";
            HttpResponse lateResp = guest1.postRaw("/mock-payos/simulate", simLateData);
            check(lateResp.statusCode == 302, "Late post returned 302");

            PaymentRecord pr2AfterLate = loadPaymentByOrder(orderCode2);
            check("REVIEW_REQUIRED".equals(pr2AfterLate.status), "Payment status changed to REVIEW_REQUIRED");
            check(countWebhookLogs(orderCode2, "LATE") == 1, "PAYMENT_WEBHOOK_LOG recorded LATE status");
            check(countAlertLogs("PAYMENT_REVIEW_REQUIRED", bookingId2) >= 1, "ALERT_LOG created for Admin review");
            check(countCredentialsForBooking(bookingId2) == 0, "No PIN issued on late payment");

            // =========================================================================
            // PART 2: T06 - CHECK-IN GUIDE, SHOW PIN, GUEST REVIEW
            // =========================================================================
            System.out.println("\n--- [T06] Test 5: Check-in Guide & Show PIN ---");

            // 1. Check booking 1 detail page (CONFIRMED booking with credential cr1)
            HttpResponse detailPage1 = guest1.get("/guest/bookings/detail?id=" + bookingId1);
            check(detailPage1.statusCode == 200, "Booking detail page returns 200 OK");
            check(detailPage1.body.contains("Check-in Guide"), "Detail page contains 'Check-in Guide'");
            check(detailPage1.body.contains("The system confirms main-door access only. It cannot verify which room you enter."),
                    "Detail page contains exact required disclaimer line");
            check(detailPage1.body.contains("Show my PIN"), "Detail page contains 'Show my PIN' button");

            // 2. Request Show PIN via POST /guest/bookings/pin
            String pinPostData = "_csrf=" + URLEncoder.encode(guest1.csrfToken, "UTF-8")
                    + "&credentialId=" + cr1.credentialId;
            HttpResponse pinResp = guest1.postRaw("/guest/bookings/pin", pinPostData);
            check(pinResp.statusCode == 200, "POST /guest/bookings/pin forwards to detail page with 200 OK");

            // Extract PIN: 6 digits
            Matcher pinMatcher = Pattern.compile("display-4[^>]*>(\\d{6})<").matcher(pinResp.body);
            boolean foundPin = pinMatcher.find();
            check(foundPin, "6-digit PIN successfully decrypted and displayed on page");
            if (foundPin) {
                System.out.println("  [INFO] Revealed PIN: " + pinMatcher.group(1));
            }
            check(pinResp.body.contains("Valid for the whole stay, including gaps between your slots."),
                    "Page shows stay validity notice");
            check(countAuditLogs("PIN_VIEWED", "ACCESS_CREDENTIAL", String.valueOf(cr1.credentialId)) == 1,
                    "Audit log recorded PIN_VIEWED");

            // 3. Security: Guest 2 tries to reveal Guest 1's PIN -> 404 (Section 5 edge case)
            String crossPinData = "_csrf=" + URLEncoder.encode(guest2.csrfToken, "UTF-8")
                    + "&credentialId=" + cr1.credentialId;
            HttpResponse crossPinResp = guest2.postRaw("/guest/bookings/pin", crossPinData);
            check(crossPinResp.statusCode == 404, "Guest 2 POSTs Guest 1's credentialId receives 404 Not Found");

            // 4. Expired PIN check: flip credential status to EXPIRED
            setCredentialStatus(cr1.credentialId, "EXPIRED");
            HttpResponse expiredPinPage = guest1.get("/guest/bookings/detail?id=" + bookingId1);
            check(expiredPinPage.body.contains("This PIN has expired."), "Expired credential shows 'This PIN has expired.'");
            check(!expiredPinPage.body.contains("Show my PIN"), "No 'Show my PIN' button when expired");

            // -------------------------------------------------------------------------
            // [T06] Test 6: Guest Reviews (M6)
            // -------------------------------------------------------------------------
            System.out.println("\n--- [T06] Test 6: Guest Reviews Eligibility & Submission ---");

            // 1. Try to review CONFIRMED booking -> NOT_ELIGIBLE (must be CHECKED_OUT)
            String revConfirmedData = "_csrf=" + URLEncoder.encode(guest1.csrfToken, "UTF-8")
                    + "&bookingId=" + bookingId1
                    + "&rating=5"
                    + "&comment=Great+stay!";
            HttpResponse revConfResp = guest1.postRaw("/guest/reviews/create", revConfirmedData);
            check(revConfResp.statusCode == 302, "Review on CONFIRMED returned 302");
            HttpResponse revConfDetail = guest1.get("/guest/bookings/detail?id=" + bookingId1);
            check(revConfDetail.body.contains("You can only review after checkout."),
                    "Error message: 'You can only review after checkout.'");
            check(countReviewsForBooking(bookingId1) == 0, "No review row was created");

            // 2. Set booking 1 status to CHECKED_OUT and test valid review submission
            setBookingStatus(bookingId1, "CHECKED_OUT");
            HttpResponse revCheckoutPage = guest1.get("/guest/bookings/detail?id=" + bookingId1);
            check(revCheckoutPage.body.contains("Overall Rating"), "Review form displayed for CHECKED_OUT booking");

            // Submit valid review (5 stars)
            HttpResponse revSubmitResp = guest1.postRaw("/guest/reviews/create", revConfirmedData);
            check(revSubmitResp.statusCode == 302, "Valid review submit returned 302");
            HttpResponse revAfterSubmit = guest1.get("/guest/bookings/detail?id=" + bookingId1);
            check(revAfterSubmit.body.contains("Thank you for your review."), "Flash success: 'Thank you for your review.'");
            check(revAfterSubmit.body.contains("5 / 5"), "Displayed guest rating 5 / 5");
            check(revAfterSubmit.body.contains("Great stay!"), "Displayed guest review comment");
            check(countReviewsForBooking(bookingId1) == 1, "Review record created in DB");
            check(countAuditLogs("REVIEW_CREATED", "REVIEW", null) >= 1, "Audit log recorded REVIEW_CREATED");

            // 3. Try to submit a second review -> ALREADY_REVIEWED
            HttpResponse revDuplicateResp = guest1.postRaw("/guest/reviews/create", revConfirmedData);
            check(revDuplicateResp.statusCode == 302, "Duplicate review submit returned 302");
            HttpResponse revDupDetail = guest1.get("/guest/bookings/detail?id=" + bookingId1);
            check(revDupDetail.body.contains("You have already reviewed this booking."),
                    "Error message: 'You have already reviewed this booking.'");
            check(countReviewsForBooking(bookingId1) == 1, "Still exactly 1 review row");

            // Clean up test bookings
            cleanupBooking(bookingId1);
            cleanupBooking(bookingId2);

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

    private static void check(boolean cond, String desc) {
        if (cond) {
            System.out.println("  [PASS] " + desc);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + desc);
            testsFailed++;
        }
    }

    private static long createTestBooking(HttpSessionHelper guest, long roomId, LocalDate date, String slot1, String slot2)
            throws Exception {
        String postData = "_csrf=" + URLEncoder.encode(guest.csrfToken, "UTF-8")
                + "&roomId=" + roomId
                + "&slotDate=" + date.toString()
                + "&slotCodes=" + slot1 + "&slotCodes=" + slot2;

        HttpResponse resp = guest.postRaw("/guest/bookings", postData);
        if (resp.statusCode == 302) {
            String loc = resp.getHeader("Location");
            Matcher m = Pattern.compile("id=(\\d+)").matcher(loc);
            if (m.find()) {
                return Long.parseLong(m.group(1));
            }
        }
        return -1;
    }

    private static long extractOrderCodeFromUrl(String url) {
        if (url == null) return -1;
        Matcher m = Pattern.compile("orderCode=(\\d+)").matcher(url);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }
        return -1;
    }

    private static void cleanupDateSlots(long roomId, LocalDate date) throws SQLException {
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
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM REVIEW WHERE booking_id = ?")) {
                ps.setLong(1, bookingId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ACCESS_CREDENTIAL WHERE booking_id = ?")) {
                ps.setLong(1, bookingId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM PAYMENT_WEBHOOK_LOG WHERE payment_id IN (SELECT payment_id FROM PAYMENT WHERE booking_id = ?)")) {
                ps.setLong(1, bookingId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ALERT_LOG WHERE booking_id = ?")) {
                ps.setLong(1, bookingId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM PAYMENT WHERE booking_id = ?")) {
                ps.setLong(1, bookingId);
                ps.executeUpdate();
            }
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

    private static class PaymentRecord {
        long paymentId;
        String status;
        long amount;
        Timestamp paidAt;
    }

    private static PaymentRecord loadPaymentByOrder(long orderCode) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT payment_id, status, amount, paid_at FROM PAYMENT WHERE provider_order_code = ?")) {
            ps.setLong(1, orderCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PaymentRecord pr = new PaymentRecord();
                    pr.paymentId = rs.getLong("payment_id");
                    pr.status = rs.getString("status");
                    pr.amount = rs.getBigDecimal("amount").longValue();
                    pr.paidAt = rs.getTimestamp("paid_at");
                    return pr;
                }
            }
        }
        return null;
    }

    private static class CredentialRecord {
        long credentialId;
        String credentialType;
        String status;
        byte[] pinCiphertext;
        byte[] pinHmac;
    }

    private static CredentialRecord loadCredentialForBooking(long bookingId) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT credential_id, credential_type, status, pin_ciphertext, pin_hmac FROM ACCESS_CREDENTIAL WHERE booking_id = ?")) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    CredentialRecord cr = new CredentialRecord();
                    cr.credentialId = rs.getLong("credential_id");
                    cr.credentialType = rs.getString("credential_type");
                    cr.status = rs.getString("status");
                    cr.pinCiphertext = rs.getBytes("pin_ciphertext");
                    cr.pinHmac = rs.getBytes("pin_hmac");
                    return cr;
                }
            }
        }
        return null;
    }

    private static int countCredentialsForBooking(long bookingId) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM ACCESS_CREDENTIAL WHERE booking_id = ?")) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private static int countReviewsForBooking(long bookingId) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM REVIEW WHERE booking_id = ?")) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private static int countWebhookLogs(long orderCode, String processStatus) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM PAYMENT_WEBHOOK_LOG WHERE provider_order_code = ? AND process_status = ?")) {
            ps.setLong(1, orderCode);
            ps.setString(2, processStatus);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private static int countAlertLogs(String alertType, long bookingId) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM ALERT_LOG WHERE alert_type = ? AND booking_id = ?")) {
            ps.setString(1, alertType);
            ps.setLong(2, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private static int countAuditLogs(String action, String entityType, String entityId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM AUDIT_LOG WHERE action = ? AND entity_type = ?";
        if (entityId != null) {
            sql += " AND entity_id = ?";
        }
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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

    private static String getBookingStatus(long bookingId) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM BOOKING WHERE booking_id = ?")) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    private static void setBookingStatus(long bookingId, String status) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE BOOKING SET status = ? WHERE booking_id = ?")) {
            ps.setString(1, status);
            ps.setLong(2, bookingId);
            ps.executeUpdate();
        }
    }

    private static void setCredentialStatus(long credentialId, String status) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE ACCESS_CREDENTIAL SET status = ? WHERE credential_id = ?")) {
            ps.setString(1, status);
            ps.setLong(2, credentialId);
            ps.executeUpdate();
        }
    }

    private static void expirePaymentInPast(long orderCode) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE PAYMENT SET expires_at = DATEADD(minute, -10, SYSDATETIME()) WHERE provider_order_code = ?")) {
            ps.setLong(1, orderCode);
            ps.executeUpdate();
        }
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
            } else {
                resp.body = "";
            }
            return resp;
        }
    }
}
