package tools;

import util.IotDevices;
import util.IotDevices.EventAlert;

/**
 * Unit test for util.IotDevices (IoT round: Blynk removed): device event -> alert mapping,
 * X-Device-Key check and the HMAC signature shared with the ESP32 firmware.
 * Reference vector computed independently with Node crypto (same steps as the ESP32 sketch).
 */
public class IotDevicesTest {

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
        System.out.println("        IOT DEVICE HELPERS VERIFICATION           ");
        System.out.println("==================================================");

        // 1. Event mapping uses only alert types allowed by CK_ALERT_LOG_alert_type
        EventAlert lockout = IotDevices.mapEvent("LOCKOUT", null);
        check(lockout != null && "PIN_BRUTE_FORCE".equals(lockout.alertType) && "CRITICAL".equals(lockout.severity),
                "LOCKOUT -> PIN_BRUTE_FORCE / CRITICAL");
        EventAlert pry = IotDevices.mapEvent("pry", "");
        check(pry != null && "DOOR_OPEN_ABNORMAL".equals(pry.alertType) && "CRITICAL".equals(pry.severity),
                "PRY (any case) -> DOOR_OPEN_ABNORMAL / CRITICAL");
        EventAlert forced = IotDevices.mapEvent("DOOR_FORCED", null);
        check(forced != null && "DOOR_OPEN_ABNORMAL".equals(forced.alertType) && "MEDIUM".equals(forced.severity),
                "DOOR_FORCED -> DOOR_OPEN_ABNORMAL / MEDIUM");
        EventAlert tooLong = IotDevices.mapEvent("DOOR_OPEN_TOO_LONG", "open more than 30 s");
        check(tooLong != null && "HIGH".equals(tooLong.severity) && tooLong.message.endsWith("(open more than 30 s)"),
                "DOOR_OPEN_TOO_LONG keeps the detail in the message");
        check("SUSPICIOUS_MOTION".equals(IotDevices.mapEvent("LOITERING", null).alertType), "LOITERING -> SUSPICIOUS_MOTION");
        check("DEVICE_FAULT".equals(IotDevices.mapEvent("EXIT_STUCK", null).alertType), "EXIT_STUCK -> DEVICE_FAULT");
        check(IotDevices.mapEvent("REBOOT", null) == null, "Unknown event type -> null (HTTP 400)");
        check(IotDevices.mapEvent(null, null) == null, "Missing event type -> null");
        StringBuilder longDetail = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            longDetail.append('x');
        }
        check(IotDevices.mapEvent("LOCKOUT", longDetail.toString()).message.length() < 500,
                "Long detail is cut so the message fits NVARCHAR(500)");

        // 2. Device key
        String keyHash = IotDevices.sha256Hex("TESTKEY123");
        check("1f0bcc7d75cfaf1cdc08ffbdad4916e146e2b2c7e627f9d7b5dd94857a37153b".equals(keyHash),
                "sha256Hex matches the reference value");
        check(IotDevices.keyMatches(null, null), "No stored key -> device accepted (legacy)");
        check(IotDevices.keyMatches("", "anything"), "Empty stored key -> device accepted");
        check(IotDevices.keyMatches(keyHash, "TESTKEY123"), "Right key -> accepted");
        check(IotDevices.keyMatches(keyHash.toUpperCase(), " TESTKEY123 "), "Stored hash case and spaces ignored");
        check(!IotDevices.keyMatches(keyHash, "WRONGKEY"), "Wrong key -> rejected");
        check(!IotDevices.keyMatches(keyHash, null), "Missing header -> rejected");

        // 3. Remote command signature = what the ESP32 recomputes (key = sha256(API key) as ASCII hex)
        String sig = IotDevices.signCommand(keyHash, "UNLOCK", 1730000000L, "abc123");
        check("0d8c25821576fab593844d8aa6c4f2eba34885c7e3993b738da2fa2ad5a9b8ce".equals(sig),
                "signCommand matches the reference HMAC (Node crypto)");
        check(!sig.equals(IotDevices.signCommand(keyHash, "SILENCE", 1730000000L, "abc123")),
                "Different command -> different signature");
        check(!sig.equals(IotDevices.signCommand(keyHash, "UNLOCK", 1730000001L, "abc123")),
                "Different timestamp -> different signature");

        // 4. Snapshot paths stored in the DB map to plain file names in the snapshot folder
        check("cam_20261027_143005_123.jpg".equals(util.SnapshotStore.fileName("assets/images/snapshots/cam_20261027_143005_123.jpg")),
                "Stored path -> file name");
        check("x.jpg".equals(util.SnapshotStore.fileName("/assets/images/snapshots/x.jpg")), "Leading slash accepted");
        check(util.SnapshotStore.fileName("assets/images/snapshots/../../WEB-INF/web.xml") == null, "Path traversal rejected");
        check(util.SnapshotStore.fileName("assets/images/snapshots/sub/x.jpg") == null, "Sub folder rejected");
        check(util.SnapshotStore.fileName("assets/images/rooms/x.jpg") == null, "Other asset folders rejected");
        check(util.SnapshotStore.fileName("http://192.168.137.20/photo.jpg") == null, "External URL rejected");
        check(util.SnapshotStore.fileName(null) == null, "Null path -> null");

        System.out.println("--------------------------------------------------");
        System.out.println("Passed: " + passed + " | Failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }
}
