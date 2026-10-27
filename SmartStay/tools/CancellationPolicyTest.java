package tools;

import util.AppConstants;
import util.CancellationPolicy;
import util.CancellationPolicy.Outcome;
import util.DemoTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for util.CancellationPolicy (Task R3 - 48-hour cancellation policy).
 */
public class CancellationPolicyTest {

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

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("     CANCELLATION POLICY VERIFICATION (R3)        ");
        System.out.println("==================================================");

        try {
            LocalDateTime start = at(2026, 10, 28, 9, 30);

            System.out.println("\n--- 1. Constant and deadline ---");
            check(AppConstants.CANCEL_FREE_HOURS == 48, "CANCEL_FREE_HOURS is 48");
            check(at(2026, 10, 26, 9, 30).equals(CancellationPolicy.deadline(start)), "Deadline = first slot start - 48 h (26/10 09:30)");
            check(CancellationPolicy.deadline(null) == null, "No start -> no deadline");

            System.out.println("\n--- 2. Boundary ---");
            check(CancellationPolicy.isRefundable(start, start.minusHours(48)), "Exactly 48 h before -> refundable");
            check(!CancellationPolicy.isRefundable(start, start.minusHours(48).plusSeconds(1)), "48 h minus 1 s -> non-refundable");
            check(!CancellationPolicy.isRefundable(start, start.minusHours(47).minusMinutes(59)), "47 h 59 min before -> non-refundable");
            check(CancellationPolicy.isRefundable(start, start.minusDays(3)), "3 days before -> refundable");
            check(!CancellationPolicy.isRefundable(start, start.minusDays(1)), "1 day before -> non-refundable");
            check(!CancellationPolicy.isRefundable(start, null), "Null now -> non-refundable");

            System.out.println("\n--- 3. Outcome by status ---");
            check(CancellationPolicy.outcomeFor("CONFIRMED", start, start.minusDays(3)) == Outcome.FULL_REFUND, "Paid, 3 days ahead -> FULL_REFUND");
            check(CancellationPolicy.outcomeFor("CONFIRMED", start, start.minusDays(1)) == Outcome.NO_REFUND, "Paid, 1 day ahead -> NO_REFUND");
            check(CancellationPolicy.outcomeFor("PENDING_PAYMENT", start, start.minusDays(1)) == Outcome.NOTHING_PAID, "Unpaid, 1 day ahead -> NOTHING_PAID (always free)");
            check(CancellationPolicy.outcomeFor("pending_payment", start, start.minusDays(3)) == Outcome.NOTHING_PAID, "Unpaid status is case-insensitive");

            System.out.println("\n--- 4. Multi-slot uses the first slot ---");
            LocalDate day = LocalDate.of(2026, 10, 28);
            List<LocalDateTime[]> slots = new ArrayList<>();
            slots.add(new LocalDateTime[]{DemoTime.slotStart(day, LocalTime.of(14, 0)), DemoTime.slotEnd(day, LocalTime.of(18, 0), false)});
            slots.add(new LocalDateTime[]{DemoTime.slotStart(day, LocalTime.of(9, 30)), DemoTime.slotEnd(day, LocalTime.of(13, 30), false)});
            LocalDateTime first = DemoTime.firstStart(slots);
            LocalDateTime now = at(2026, 10, 26, 12, 0); // 50 h before 14:00, 45.5 h before 09:30
            check(!CancellationPolicy.isRefundable(first, now), "Afternoon+Morning: deadline follows 09:30, so 26/10 12:00 is too late");
            check(CancellationPolicy.isRefundable(DemoTime.slotStart(day, LocalTime.of(14, 0)), now), "(control) the 14:00 slot alone would still refund");

            System.out.println("\n--- 5. Overnight ---");
            LocalDateTime overnightStart = DemoTime.slotStart(day, LocalTime.of(20, 0));
            check(at(2026, 10, 26, 20, 0).equals(CancellationPolicy.deadline(overnightStart)),
                    "Overnight 28/10 20:00 -> 09:00 (+1 day): deadline 26/10 20:00 (start, not end)");
            check(CancellationPolicy.isRefundable(overnightStart, at(2026, 10, 26, 19, 59)), "26/10 19:59 -> refundable");
            check(!CancellationPolicy.isRefundable(overnightStart, at(2026, 10, 26, 20, 1)), "26/10 20:01 -> non-refundable");

            System.out.println("\n--- 6. Booked late = non-refundable from the start ---");
            LocalDateTime bookedAt = start.minusHours(30);
            check(!CancellationPolicy.isRefundable(start, bookedAt), "Booked 30 h before check-in -> non-refundable immediately");
            check(!CancellationPolicy.info("CONFIRMED", start, bookedAt).isRefundable(), "info().refundable false for a late booking");
            check(CancellationPolicy.info("PENDING_PAYMENT", start, bookedAt).isNothingPaid(), "info().nothingPaid true before paying");
            check(!CancellationPolicy.info(null, null, bookedAt).isKnown(), "info() without a start is not known");

            System.out.println("\n--- 7. E-mail sentence ---");
            String free = CancellationPolicy.policySentence(start, start.minusDays(3));
            check(free.startsWith("Free cancellation until 26/10/2026 09:30"), "Refundable sentence names the deadline (" + free + ")");
            String locked = CancellationPolicy.policySentence(start, start.minusDays(1));
            check(locked.contains("non-refundable") && locked.contains("48 hours"), "Late sentence says non-refundable");
            check(CancellationPolicy.policySentence(null, start).isEmpty(), "No start -> empty sentence");
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
