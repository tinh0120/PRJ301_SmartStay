package tools;

import util.DemoTime;
import util.DemoTime.Target;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for util.DemoTime (Task R2-B2 - demo time-travel target math).
 */
public class DemoTimeTest {

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

    private static LocalDateTime at(int y, int mo, int d, int h, int mi) {
        return LocalDateTime.of(y, mo, d, h, mi);
    }

    private static LocalDateTime[] window(LocalDate date, String start, String end, boolean overnight) {
        return new LocalDateTime[]{
            DemoTime.slotStart(date, LocalTime.parse(start)),
            DemoTime.slotEnd(date, LocalTime.parse(end), overnight)
        };
    }

    private static List<LocalDateTime[]> windows(LocalDateTime[]... ws) {
        List<LocalDateTime[]> list = new ArrayList<>();
        for (LocalDateTime[] w : ws) {
            list.add(w);
        }
        return list;
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("       DEMO TIME TARGET VERIFICATION (R2-B2)      ");
        System.out.println("==================================================");

        try {
            LocalDate day = LocalDate.of(2026, 10, 26);

            // 1. Single slot (Morning 09:30 - 13:30)
            System.out.println("\n--- 1. Single slot ---");
            List<LocalDateTime[]> single = windows(window(day, "09:30", "13:30", false));
            LocalDateTime s1 = DemoTime.firstStart(single);
            LocalDateTime e1 = DemoTime.lastEnd(single);
            check(s1.equals(at(2026, 10, 26, 9, 30)), "First start = 26/10 09:30");
            check(e1.equals(at(2026, 10, 26, 13, 30)), "Last end = 26/10 13:30");
            check(DemoTime.targetMoment(Target.BEFORE_CHECKIN_15, s1, e1).equals(at(2026, 10, 26, 9, 15)), "BEFORE_CHECKIN_15 = 09:15");
            check(DemoTime.targetMoment(Target.CHECKIN_NOW, s1, e1).equals(at(2026, 10, 26, 9, 30)), "CHECKIN_NOW = 09:30");
            check(DemoTime.targetMoment(Target.BEFORE_CHECKOUT_15, s1, e1).equals(at(2026, 10, 26, 13, 15)), "BEFORE_CHECKOUT_15 = 13:15");
            check(DemoTime.targetMoment(Target.CHECKOUT_NOW, s1, e1).equals(at(2026, 10, 26, 13, 30).plusSeconds(1)),
                    "CHECKOUT_NOW = 13:30:01 (1 s margin for strict end checks)");

            // 2. Multi-slot, given out of order (Afternoon + Morning + Evening)
            System.out.println("\n--- 2. Multi-slot ---");
            List<LocalDateTime[]> multi = windows(
                    window(day, "14:00", "18:00", false),
                    window(day, "09:30", "13:30", false),
                    window(day, "18:30", "22:30", false));
            LocalDateTime s2 = DemoTime.firstStart(multi);
            LocalDateTime e2 = DemoTime.lastEnd(multi);
            check(s2.equals(at(2026, 10, 26, 9, 30)), "First start uses the earliest slot (09:30)");
            check(e2.equals(at(2026, 10, 26, 22, 30)), "Last end uses the latest slot (22:30)");
            check(DemoTime.targetMoment(Target.BEFORE_CHECKOUT_15, s2, e2).equals(at(2026, 10, 26, 22, 15)), "BEFORE_CHECKOUT_15 = 22:15");

            // 3. Overnight (Evening 16:30-20:00 + Overnight 20:00-09:00 next day)
            System.out.println("\n--- 3. Overnight +1 day ---");
            LocalDateTime[] overnight = window(day, "20:00", "09:00", true);
            check(overnight[1].equals(at(2026, 10, 27, 9, 0)), "Overnight slot ends 27/10 09:00 (+1 day)");
            check(window(day, "20:00", "09:00", false)[1].equals(at(2026, 10, 26, 9, 0)), "Without crossesMidnight the end stays on the same date");
            List<LocalDateTime[]> stay = windows(window(day, "16:30", "20:00", false), overnight);
            LocalDateTime s3 = DemoTime.firstStart(stay);
            LocalDateTime e3 = DemoTime.lastEnd(stay);
            check(DemoTime.targetMoment(Target.BEFORE_CHECKIN_15, s3, e3).equals(at(2026, 10, 26, 16, 15)), "BEFORE_CHECKIN_15 = 26/10 16:15");
            check(DemoTime.targetMoment(Target.BEFORE_CHECKOUT_15, s3, e3).equals(at(2026, 10, 27, 8, 45)), "BEFORE_CHECKOUT_15 = 27/10 08:45");
            check(DemoTime.targetMoment(Target.CHECKOUT_NOW, s3, e3).toLocalDate().equals(LocalDate.of(2026, 10, 27)), "CHECKOUT_NOW is on the next day");

            // Overnight crossing a month/year boundary
            LocalDateTime[] nye = window(LocalDate.of(2026, 12, 31), "20:00", "09:00", true);
            check(nye[1].equals(at(2027, 1, 1, 9, 0)), "31/12 overnight ends 01/01/2027 09:00");

            // 4. Forward-only rule
            System.out.println("\n--- 4. Forward only ---");
            LocalDateTime now = at(2026, 10, 26, 10, 0);
            check(!DemoTime.isReachable(DemoTime.targetMoment(Target.CHECKIN_NOW, s1, e1), now), "CHECKIN_NOW 09:30 refused at 10:00");
            check(DemoTime.isReachable(DemoTime.targetMoment(Target.BEFORE_CHECKOUT_15, s1, e1), now), "BEFORE_CHECKOUT_15 13:15 allowed at 10:00");
            check(DemoTime.isReachable(now, now), "Target equal to now is allowed");
            check(!DemoTime.isReachable(null, now), "Null target refused");

            // 5. Parsing and edge cases
            System.out.println("\n--- 5. Parsing ---");
            check(Target.parse("checkout_now") == Target.CHECKOUT_NOW, "parse is case-insensitive");
            check(Target.parse(" CHECKIN_NOW ") == Target.CHECKIN_NOW, "parse trims spaces");
            check(Target.parse("YESTERDAY") == null && Target.parse(null) == null, "Unknown / null target -> null");
            check(Target.BEFORE_CHECKOUT_15.isCheckout() && Target.CHECKOUT_NOW.isCheckout()
                    && !Target.CHECKIN_NOW.isCheckout() && !Target.BEFORE_CHECKIN_15.isCheckout(), "isCheckout() only for the two checkout targets");
            check(DemoTime.firstStart(new ArrayList<LocalDateTime[]>()) == null, "No slots -> null first start");
            boolean threw = false;
            try {
                DemoTime.targetMoment(Target.CHECKIN_NOW, null, e1);
            } catch (IllegalArgumentException expected) {
                threw = true;
            }
            check(threw, "Missing window throws IllegalArgumentException");
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
