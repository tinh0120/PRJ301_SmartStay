package tools;

import util.ElFunctions;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Date;

/**
 * Unit tests for Booking Detail EL formatters in ElFunctions.
 */
public class BookingDetailFormatterTest {

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
        System.out.println("   BOOKING DETAIL FORMATTER UNIT VERIFICATION     ");
        System.out.println("==================================================");

        // 1. Currency Formatting (VND)
        System.out.println("\n--- 1. Currency Formatting (VND) ---");
        check("289,000 VND".equals(ElFunctions.money(289000L)), "289000L formats to '289,000 VND'");
        check("0 VND".equals(ElFunctions.money(0L)), "0L formats to '0 VND'");
        check("1,500,000 VND".equals(ElFunctions.money(1500000)), "1500000 formats to '1,500,000 VND'");
        check("".equals(ElFunctions.money(null)), "null formats to empty string");
        check("125,000,000 VND".equals(ElFunctions.money(125000000L)), "Large number 125000000L formats to '125,000,000 VND'");
        check("259,000 VND".equals(ElFunctions.money(259000.4)), "Rounding 259000.4 formats to '259,000 VND'");
        check("259,001 VND".equals(ElFunctions.money(259000.6)), "Rounding 259000.6 formats to '259,001 VND'");

        // 2. Date/Time Formatting
        System.out.println("\n--- 2. Date/Time Formatting (DD/MM/YYYY and HH:mm) ---");
        LocalDateTime testDt = LocalDateTime.of(2026, Month.OCTOBER, 23, 16, 30, 0);
        check("23/10/2026 16:30".equals(ElFunctions.datetime(testDt)),
                "LocalDateTime(2026-10-23 16:30) formats to '23/10/2026 16:30'");
        check("Fri, 23/10/2026 16:30".equals(ElFunctions.weekdayDatetime(testDt)),
                "weekdayDatetime formats to 'Fri, 23/10/2026 16:30'");
        check("16:30".equals(ElFunctions.time(testDt)), "time formats to '16:30'");
        check("23/10/2026".equals(ElFunctions.date(testDt)), "date formats to '23/10/2026'");

        // 3. Duration Calculation
        System.out.println("\n--- 3. Duration Calculation ---");
        LocalDateTime checkin = LocalDateTime.of(2026, Month.OCTOBER, 23, 16, 30, 0);
        LocalDateTime checkout3h = LocalDateTime.of(2026, Month.OCTOBER, 23, 19, 30, 0);
        LocalDateTime checkout1h = LocalDateTime.of(2026, Month.OCTOBER, 23, 17, 30, 0);
        LocalDateTime checkout45m = LocalDateTime.of(2026, Month.OCTOBER, 23, 17, 15, 0);
        LocalDateTime checkout2h30m = LocalDateTime.of(2026, Month.OCTOBER, 23, 19, 0, 0);

        check("3 hours".equals(ElFunctions.duration(checkin, checkout3h)), "16:30 to 19:30 computes to '3 hours'");
        check("1 hour".equals(ElFunctions.duration(checkin, checkout1h)), "16:30 to 17:30 computes to '1 hour'");
        check("45 minutes".equals(ElFunctions.duration(checkin, checkout45m)), "16:30 to 17:15 computes to '45 minutes'");
        check("2 hours 30 mins".equals(ElFunctions.duration(checkin, checkout2h30m)), "16:30 to 19:00 computes to '2 hours 30 mins'");

        // 4. Human-readable Status Titles
        System.out.println("\n--- 4. Human-readable Status Titles ---");
        check("Pending payment".equals(ElFunctions.humanStatus("PENDING_PAYMENT")), "PENDING_PAYMENT -> 'Pending payment'");
        check("Confirmed".equals(ElFunctions.humanStatus("CONFIRMED")), "CONFIRMED -> 'Confirmed'");
        check("Checked in".equals(ElFunctions.humanStatus("CHECKED_IN")), "CHECKED_IN -> 'Checked in'");
        check("Completed".equals(ElFunctions.humanStatus("CHECKED_OUT")), "CHECKED_OUT -> 'Completed'");
        check("Completed".equals(ElFunctions.humanStatus("COMPLETED")), "COMPLETED -> 'Completed'");
        check("Cancelled".equals(ElFunctions.humanStatus("CANCELLED")), "CANCELLED -> 'Cancelled'");
        check("Expired".equals(ElFunctions.humanStatus("EXPIRED")), "EXPIRED -> 'Expired'");
        check("Held".equals(ElFunctions.humanStatus("HELD")), "HELD -> 'Held'");
        check("Released".equals(ElFunctions.humanStatus("RELEASED")), "RELEASED -> 'Released'");

        System.out.println("\n==================================================");
        System.out.println(String.format("TEST RESULTS: %d PASSED, %d FAILED", passed, failed));
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
