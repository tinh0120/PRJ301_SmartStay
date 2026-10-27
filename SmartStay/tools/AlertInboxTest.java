package tools;

import model.AlertEntry;
import util.AlertTitles;
import util.ElFunctions;
import java.time.LocalDateTime;

/**
 * Unit test for the Security alerts inbox (Task R10): type -> title map, relative time, source line.
 */
public class AlertInboxTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(boolean condition, String message) {
        if (condition) {
            System.out.println("  [PASS] " + message);
            passed++;
        } else {
            System.err.println("  [FAIL] " + message);
            failed++;
        }
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("        ALERTS INBOX VERIFICATION (R10)           ");
        System.out.println("==================================================");

        try {
            System.out.println("\n--- 1. Type -> title map ---");
            check("Door held open too long".equals(AlertTitles.title("DOOR_OPEN_TOO_LONG")), "DOOR_OPEN_TOO_LONG");
            check("Guest did not check in".equals(AlertTitles.title("BOOKING_NO_SHOW")), "BOOKING_NO_SHOW");
            check("Device offline".equals(AlertTitles.title("DEVICE_OFFLINE")), "DEVICE_OFFLINE");
            check("Cleaning not finished in time".equals(AlertTitles.title("CLEANING_SESSION_EXPIRED")), "CLEANING_SESSION_EXPIRED");
            check("Payment needs review".equals(AlertTitles.title("PAYMENT_REVIEW_REQUIRED")), "PAYMENT_REVIEW_REQUIRED");
            check("Device offline".equals(AlertTitles.title(" device_offline ")), "Case and spaces ignored");
            check("Smoke sensor triggered".equals(AlertTitles.title("SMOKE_SENSOR_TRIGGERED")), "Unknown type -> prettified enum");
            check("Alert".equals(AlertTitles.title(null)) && "Alert".equals(AlertTitles.title("  ")), "Empty -> Alert");
            check(!AlertTitles.title("NEW_TYPE_X").contains("_"), "No underscores ever reach the list");

            System.out.println("\n--- 2. Relative time ---");
            LocalDateTime now = LocalDateTime.of(2026, 10, 26, 12, 0);
            check("just now".equals(ElFunctions.relativeTime(now.minusSeconds(30), now)), "30 s -> just now");
            check("just now".equals(ElFunctions.relativeTime(now.plusMinutes(5), now)), "Future -> just now");
            check("1 min ago".equals(ElFunctions.relativeTime(now.minusMinutes(1), now)), "1 min ago");
            check("45 min ago".equals(ElFunctions.relativeTime(now.minusMinutes(45), now)), "45 min ago");
            check("1 hour ago".equals(ElFunctions.relativeTime(now.minusMinutes(61), now)), "1 hour ago");
            check("2 hours ago".equals(ElFunctions.relativeTime(now.minusHours(2), now)), "2 hours ago");
            check("1 day ago".equals(ElFunctions.relativeTime(now.minusHours(30), now)), "1 day ago");
            check("3 days ago".equals(ElFunctions.relativeTime(now.minusDays(3), now)), "3 days ago");
            check("2 months ago".equals(ElFunctions.relativeTime(now.minusDays(65), now)), "2 months ago");
            check("-".equals(ElFunctions.relativeTime(null, now)), "Null -> -");

            System.out.println("\n--- 3. Source line and severity tone ---");
            AlertEntry a = new AlertEntry();
            a.setAlertType("DEVICE_OFFLINE");
            a.setDeviceId("ESP32-LOBBY-01");
            check("ESP32-LOBBY-01".equals(a.getSource()), "Device ID when no name");
            a.setDeviceName("Lobby sensor");
            check("Lobby sensor".equals(a.getSource()), "Device name preferred");
            AlertEntry b = new AlertEntry();
            b.setRoomNumber("101");
            check("Room 101".equals(b.getSource()), "Room when no device");
            AlertEntry c = new AlertEntry();
            c.setZoneId("MAIN_ENTRANCE");
            check("Main entrance".equals(c.getSource()), "Zone label");
            AlertEntry d = new AlertEntry();
            d.setBookingCode("SS-2026-0001");
            check("Booking SS-2026-0001".equals(d.getSource()), "Booking code last");
            check("".equals(new AlertEntry().getSource()), "Nothing -> empty");
            d.setSeverity("CRITICAL");
            check("critical".equals(d.getSeverityTone()) && "low".equals(new AlertEntry().getSeverityTone()), "Severity tone");
            check("Device offline".equals(a.getTitle()), "AlertEntry.getTitle uses the central map");
        } catch (Throwable t) {
            System.err.println("Unexpected error: " + t);
            t.printStackTrace();
            failed++;
        }

        System.out.println("\n==================================================");
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        System.out.println("==================================================");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
