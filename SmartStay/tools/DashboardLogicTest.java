package tools;

import dao.DashboardDao;
import model.AttentionItem;
import model.RoomTile;
import servlet.DashboardServlet;
import util.AlertTitles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for the pure dashboard logic (Task R6): alert titles, attention ordering,
 * revenue change and event times. No database needed.
 */
public class DashboardLogicTest {

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

    private static AttentionItem item(int rank, String title, LocalDateTime at) {
        return new AttentionItem(rank, "medium", title, "", "Open", "/admin", at);
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("        ADMIN DASHBOARD LOGIC VERIFICATION (R6)   ");
        System.out.println("==================================================");

        try {
            System.out.println("\n--- 1. Alert titles in plain words ---");
            check("Door held open too long".equals(AlertTitles.title("DOOR_OPEN_TOO_LONG")), "DOOR_OPEN_TOO_LONG");
            check("Guest did not check in".equals(AlertTitles.title("booking_no_show")), "booking_no_show (any case)");
            check("Device offline".equals(AlertTitles.title("DEVICE_OFFLINE")), "DEVICE_OFFLINE");
            check("Some new type".equals(AlertTitles.title("SOME_NEW_TYPE")), "Unknown type is prettified");
            check("Alert".equals(AlertTitles.title(null)) && "Alert".equals(AlertTitles.title("  ")), "Empty type -> Alert");

            System.out.println("\n--- 2. Needs attention ordering ---");
            LocalDateTime t = LocalDateTime.of(2026, 10, 25, 9, 0);
            List<AttentionItem> items = new ArrayList<>();
            items.add(item(AttentionItem.RANK_REFUND, "refund", t));
            items.add(item(AttentionItem.RANK_HIGH_ALERT, "high-new", t.plusHours(2)));
            items.add(item(AttentionItem.RANK_CRITICAL_ALERT, "critical", t));
            items.add(item(AttentionItem.RANK_HIGH_ALERT, "high-old", t));
            items.add(item(AttentionItem.RANK_DEVICES_OFFLINE, "offline", null));
            items.add(item(AttentionItem.RANK_CLEANING_EXPIRED, "cleaning", t));
            DashboardDao.sortAttention(items);
            check("critical".equals(items.get(0).getTitle()), "Critical alert first");
            check("offline".equals(items.get(1).getTitle()), "Offline devices before high alerts");
            check("high-old".equals(items.get(2).getTitle()) && "high-new".equals(items.get(3).getTitle()), "Same rank: longest waiting first");
            check("cleaning".equals(items.get(items.size() - 1).getTitle()), "Expired cleaning last");

            System.out.println("\n--- 3. Revenue change ---");
            check(Integer.valueOf(6).equals(DashboardServlet.changePercent(1060000, 1000000)), "+6%");
            check(Integer.valueOf(-25).equals(DashboardServlet.changePercent(750000, 1000000)), "-25%");
            check(DashboardServlet.changePercent(500000, 0) == null, "No previous revenue -> no chip");

            System.out.println("\n--- 4. Event times on room tiles ---");
            LocalDateTime now = LocalDateTime.of(2026, 10, 25, 10, 0);
            check("14:00".equals(DashboardDao.eventTime(LocalDateTime.of(2026, 10, 25, 14, 0), now)), "Same day -> HH:mm");
            check("26/10 09:00".equals(DashboardDao.eventTime(LocalDateTime.of(2026, 10, 26, 9, 0), now)), "Other day -> dd/MM HH:mm");

            System.out.println("\n--- 5. Room tile tones (normal is quiet) ---");
            RoomTile tile = new RoomTile();
            tile.setStatus("AVAILABLE");
            check("muted".equals(tile.getTone()) && !tile.isProblem(), "Available is neutral");
            tile.setStatus("OCCUPIED");
            check("muted".equals(tile.getTone()) && "Occupied".equals(tile.getStatusLabel()), "Occupied is neutral");
            tile.setStatus("NEEDS_CLEANING");
            check("warn".equals(tile.getTone()) && tile.isProblem(), "Needs cleaning is coloured (warn)");
            tile.setStatus("MAINTENANCE");
            check("danger".equals(tile.getTone()) && tile.isProblem(), "Maintenance is coloured (danger)");
        } catch (Throwable ex) {
            System.err.println("Unexpected error: " + ex);
            ex.printStackTrace();
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
