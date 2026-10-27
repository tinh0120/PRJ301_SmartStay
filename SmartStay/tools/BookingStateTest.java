package tools;

import util.BookingState;
import util.RoomFilter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Unit test for util.BookingState and RoomFilter.weekRangeLabel (Task R5 - guest polish).
 */
public class BookingStateTest {

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
        System.out.println("      GUEST BOOKING STATE VERIFICATION (R5)       ");
        System.out.println("==================================================");

        try {
            System.out.println("\n--- 1. One friendly state ---");
            BookingState failedPay = BookingState.of("PENDING_PAYMENT", "FAILED");
            check("Payment failed".equals(failedPay.getLabel()) && "Retry payment".equals(failedPay.getActionLabel())
                    && failedPay.isPayAction() && "danger".equals(failedPay.getTone()),
                    "PENDING_PAYMENT + FAILED -> Payment failed / Retry payment (no second badge)");
            BookingState awaiting = BookingState.of("PENDING_PAYMENT", "PENDING");
            check("Awaiting payment".equals(awaiting.getLabel()) && "Pay now".equals(awaiting.getActionLabel()), "PENDING_PAYMENT + PENDING -> Awaiting payment / Pay now");
            check("Awaiting payment".equals(BookingState.of("PENDING_PAYMENT", "NONE").getLabel()), "No payment yet -> Awaiting payment");
            check("Awaiting payment".equals(BookingState.of("PENDING_PAYMENT", "EXPIRED").getLabel()), "Expired link while booking still held -> Awaiting payment");
            BookingState confirmed = BookingState.of("CONFIRMED", "PAID");
            check("Confirmed".equals(confirmed.getLabel()) && confirmed.isPinAction() && "View PIN".equals(confirmed.getActionLabel()), "CONFIRMED -> Confirmed / View PIN");
            check("Checked in".equals(BookingState.of("CHECKED_IN", "PAID").getLabel()), "CHECKED_IN -> Checked in");
            check("Completed".equals(BookingState.of("CHECKED_OUT", "PAID").getLabel()), "CHECKED_OUT -> Completed");
            check("Cancelled".equals(BookingState.of("CANCELLED", "REFUND_PENDING").getLabel())
                    && "View details".equals(BookingState.of("CANCELLED", "REFUND_PENDING").getActionLabel()), "CANCELLED -> Cancelled / View details");
            check("Expired".equals(BookingState.of("EXPIRED", "FAILED").getLabel()), "EXPIRED + FAILED -> Expired (one state)");
            check("confirmed".equals(BookingState.of(" confirmed ", null).getKey()), "Status is trimmed and case-insensitive");
            check(!BookingState.of(null, null).isPayAction(), "Unknown -> details only");

            System.out.println("\n--- 2. Stay times ---");
            LocalDateTime in = LocalDateTime.of(2026, 10, 25, 16, 30);
            check("16:30 → 09:00 (+1 day)".equals(BookingState.stayTimes(in, LocalDateTime.of(2026, 10, 26, 9, 0))), "Overnight adds (+1 day)");
            check("09:30 → 19:30".equals(BookingState.stayTimes(LocalDateTime.of(2026, 10, 26, 9, 30), LocalDateTime.of(2026, 10, 26, 19, 30))), "Same day has no suffix");
            check("25/10 · 16:30 → 09:00 (+1 day)".equals(BookingState.stayLine(in, LocalDateTime.of(2026, 10, 26, 9, 0))), "Stay line with dd/MM");
            check("".equals(BookingState.stayTimes(null, in)), "Missing time -> empty");

            System.out.println("\n--- 3. Slot names ---");
            check("Evening + Overnight".equals(BookingState.slotNames("S3+S4")), "S3+S4 -> Evening + Overnight");
            check("Morning + Afternoon".equals(BookingState.slotNames("S1+S2 · 09:30–19:30")), "Times after the middle dot are ignored");
            check("Overnight".equals(BookingState.joinNames(Arrays.asList("Overnight"))), "Single name");
            check("".equals(BookingState.slotNames(null)), "Null -> empty");

            System.out.println("\n--- 3b. Admin badge (R7: one badge per row) ---");
            check("Confirmed · Paid".equals(BookingState.admin("CONFIRMED", "PAID", null).getLabel())
                    && "success".equals(BookingState.admin("CONFIRMED", "PAID", null).getTone()), "CONFIRMED + PAID -> Confirmed · Paid (success)");
            check("Awaiting payment".equals(BookingState.admin("PENDING_PAYMENT", "PENDING", null).getLabel()), "PENDING_PAYMENT -> Awaiting payment");
            check("Payment failed".equals(BookingState.admin("PENDING_PAYMENT", "FAILED", null).getLabel()), "PENDING_PAYMENT + FAILED -> Payment failed");
            check("Cancelled · Refund pending".equals(BookingState.admin("CANCELLED", "REFUND_PENDING", null).getLabel())
                    && "warning".equals(BookingState.admin("CANCELLED", "REFUND_PENDING", null).getTone()), "Refund pending is loud (warning)");
            check("Cancelled · No refund".equals(BookingState.admin("CANCELLED", "PAID", null).getLabel()), "Cancelled after deadline -> No refund");
            check("Cancelled · Refunded".equals(BookingState.admin("CANCELLED", "REFUNDED", null).getLabel())
                    && "neutral".equals(BookingState.admin("CANCELLED", "REFUNDED", null).getTone()), "Refunded is quiet");
            check("Expired · No-show".equals(BookingState.admin("EXPIRED", "PAID", "NO_SHOW").getLabel()), "EXPIRED + NO_SHOW -> Expired · No-show");
            check("Expired".equals(BookingState.admin("EXPIRED", "EXPIRED", "PAYMENT_TIMEOUT").getLabel()), "Payment timeout -> Expired");
            check("Checked in".equals(BookingState.admin("CHECKED_IN", "PAID", null).getLabel()), "CHECKED_IN -> Checked in");
            check("26/10 · Morning + Afternoon · 09:30 → 19:30".equals(BookingState.slotLine(
                    LocalDateTime.of(2026, 10, 26, 9, 30), LocalDateTime.of(2026, 10, 26, 19, 30), "Morning + Afternoon")),
                    "Admin row line");

            System.out.println("\n--- 4. Week label ---");
            check("25 – 31 Oct".equals(RoomFilter.weekRangeLabel(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 31))), "Same month: 25 – 31 Oct");
            check("29 Oct – 4 Nov".equals(RoomFilter.weekRangeLabel(LocalDate.of(2026, 10, 29), LocalDate.of(2026, 11, 4))), "Two months: 29 Oct – 4 Nov");
            check("29 Dec 2026 – 4 Jan 2027".equals(RoomFilter.weekRangeLabel(LocalDate.of(2026, 12, 29), LocalDate.of(2027, 1, 4))), "Two years show the years");
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
