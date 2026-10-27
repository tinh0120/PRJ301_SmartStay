package tools;

import util.ActivityLog;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit test for util.ActivityLog (Task R9): plain-words reasons and the day-grouping helper.
 */
public class ActivityLogTest {

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
        System.out.println("        ACTIVITY LOG HELPERS VERIFICATION (R9)    ");
        System.out.println("==================================================");

        try {
            System.out.println("\n--- 1. Reasons in plain words ---");
            check("Wrong PIN".equals(ActivityLog.reasonText("WRONG_PIN")), "WRONG_PIN -> Wrong PIN");
            check("Not yet valid".equals(ActivityLog.reasonText("not_yet_valid")), "not_yet_valid -> Not yet valid");
            check("Expired".equals(ActivityLog.reasonText("EXPIRED")) && "Revoked".equals(ActivityLog.reasonText("REVOKED")), "EXPIRED / REVOKED");
            check("Locked out after too many tries".equals(ActivityLog.reasonText("LOCKED_OUT")), "LOCKED_OUT");
            check("".equals(ActivityLog.reasonText("OK")), "OK is hidden (empty)");
            check("Some new code".equals(ActivityLog.reasonText("SOME_NEW_CODE")), "Unknown code prettified");
            check("".equals(ActivityLog.reasonText(null)), "Null -> empty");

            System.out.println("\n--- 2. Day labels ---");
            LocalDate today = LocalDate.of(2026, 10, 26);
            check("Today".equals(ActivityLog.dayLabel(today, today)), "Today");
            check("Yesterday".equals(ActivityLog.dayLabel(today.minusDays(1), today)), "Yesterday");
            check("23/10/2026".equals(ActivityLog.dayLabel(LocalDate.of(2026, 10, 23), today)), "Older -> dd/MM/yyyy");

            System.out.println("\n--- 3. Group by day (keeps order) ---");
            List<LocalDateTime> rows = Arrays.asList(
                    LocalDateTime.of(2026, 10, 26, 9, 0), LocalDateTime.of(2026, 10, 26, 8, 0),
                    LocalDateTime.of(2026, 10, 25, 22, 0), LocalDateTime.of(2026, 10, 23, 7, 0), null);
            List<ActivityLog.Group<LocalDateTime>> groups = ActivityLog.groupByDay(rows,
                    new ActivityLog.TimeFn<LocalDateTime>() {
                        @Override
                        public LocalDateTime time(LocalDateTime t) {
                            return t;
                        }
                    }, today);
            check(groups.size() == 4, "4 groups (today, yesterday, 23/10, unknown)");
            check("Today".equals(groups.get(0).getLabel()) && groups.get(0).getSize() == 2, "Today has 2 rows");
            check("Yesterday".equals(groups.get(1).getLabel()) && groups.get(1).getSize() == 1, "Yesterday has 1 row");
            check("23/10/2026".equals(groups.get(2).getLabel()), "Then 23/10/2026");
            check("Unknown date".equals(groups.get(3).getLabel()), "Rows without a time go last");
            check(ActivityLog.groupByDay(new ArrayList<LocalDateTime>(), null, today).isEmpty(), "Empty list -> no groups");

            System.out.println("\n--- 4. Zones and distance ---");
            check("Main entrance".equals(ActivityLog.zoneLabel("MAIN_ENTRANCE")) && "Lobby".equals(ActivityLog.zoneLabel("LOBBY")), "Zone labels");
            check(ActivityLog.zoneRank("MAIN_ENTRANCE") < ActivityLog.zoneRank("LOBBY") && ActivityLog.zoneRank("LOBBY") < ActivityLog.zoneRank("ROOM"), "Main entrance before lobby before others");
            check(ActivityLog.isClose(49.9) && !ActivityLog.isClose(50.0) && !ActivityLog.isClose(null), "Close range is < 50 cm");
            check(ActivityLog.distancePercent(150.0) == 50 && ActivityLog.distancePercent(900.0) == 100 && ActivityLog.distancePercent(null) == 0, "Distance bar width");
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
