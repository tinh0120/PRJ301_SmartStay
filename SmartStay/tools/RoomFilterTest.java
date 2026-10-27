package tools;

import model.Booking.Quote;
import model.RoomSlot;
import service.PricingService;
import util.RoomFilter;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit test for util.RoomFilter and the Rooms-page price (Task R4).
 */
public class RoomFilterTest {

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

    private static RoomSlot slot(String code, int sort, long price) {
        RoomSlot s = new RoomSlot();
        s.setSlotCode(code);
        s.setSortOrder(sort);
        s.setListPrice(price);
        s.setActive(true);
        s.setStartTime(LocalTime.of(6 + sort * 3, 0));
        s.setEndTime(LocalTime.of(8 + sort * 3, 0));
        return s;
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("        ROOMS PAGE FILTER VERIFICATION (R4)       ");
        System.out.println("==================================================");

        try {
            System.out.println("\n--- 1. Slot labels ---");
            check("Morning".equals(RoomFilter.slotLabel("S1")) && "Overnight".equals(RoomFilter.slotLabel("s4")), "S1 -> Morning, s4 -> Overnight");
            check("Morning, Afternoon".equals(RoomFilter.slotLabels(Arrays.asList("S1", "S2"))), "Labels joined with commas");

            System.out.println("\n--- 2. Parsing ---");
            check(Arrays.asList("S1", "S2").equals(RoomFilter.parseSlots(new String[]{"S2", "s1"})), "Repeated values, any case, day order");
            check(Arrays.asList("S2", "S3").equals(RoomFilter.parseSlots(new String[]{"S3,S2,S2"})), "Comma list, duplicates removed");
            check(Collections.singletonList("S4").equals(RoomFilter.parseSlots(new String[]{"S9,x,S4"})), "Unknown codes ignored");
            check(RoomFilter.parseSlots(null).isEmpty(), "Null -> empty");

            System.out.println("\n--- 3. Consecutive rule ---");
            check(RoomFilter.isConsecutive(Arrays.asList("S1", "S2", "S3")), "S1+S2+S3 consecutive");
            check(RoomFilter.isConsecutive(Collections.singletonList("S3")) && RoomFilter.isConsecutive(new ArrayList<String>()), "Single / empty consecutive");
            check(!RoomFilter.isConsecutive(Arrays.asList("S1", "S3")), "S1+S3 not consecutive");
            check(Arrays.asList("S1", "S2").equals(RoomFilter.firstConsecutiveRun(Arrays.asList("S1", "S2", "S4"))), "S1+S2+S4 keeps S1+S2");
            check(Collections.singletonList("S2").equals(RoomFilter.firstConsecutiveRun(Arrays.asList("S2", "S4"))), "S2+S4 keeps S2");

            System.out.println("\n--- 4. Sort + int filters ---");
            check("price_desc".equals(RoomFilter.normalizeSort("price", "DESC")), "Legacy sort=price&dir=DESC -> price_desc");
            check("room".equals(RoomFilter.normalizeSort("hack", null)), "Unknown sort -> room");
            check("capacity".equals(RoomFilter.normalizeSort("CAPACITY", null)), "Case-insensitive sort");
            check(Integer.valueOf(3).equals(RoomFilter.parseIntFilter(" 3 ", 1, 20)), "Capacity 3 parsed");
            check(RoomFilter.parseIntFilter("0", 1, 20) == null && RoomFilter.parseIntFilter("abc", 1, 20) == null, "Out of range / invalid -> null");

            System.out.println("\n--- 5. URL state ---");
            RoomFilter.State s = new RoomFilter.State("2026-10-25", "2026-10-25", Arrays.asList("S1", "S2"), 3, 1, "price", true);
            check("date=2026-10-25&slots=S1,S2&minCapacity=3&floor=1&sort=price&available=1".equals(s.toQuery()),
                    "Full state query (" + s.toQuery() + ")");
            RoomFilter.State plain = new RoomFilter.State("2026-10-25", "2026-10-25", null, null, null, "room", false);
            check("date=2026-10-25".equals(plain.toQuery()), "Defaults left out");
            check("/rooms?date=2026-10-25&weekStart=2026-10-22".equals(plain.withDate("2026-10-25", "2026-10-22").url("/rooms")),
                    "weekStart kept only when it differs from date");
            check(s.withSlots(null).toQuery().indexOf("slots=") < 0 && s.withFloor(null).toQuery().indexOf("floor=") < 0
                    && s.withMinCapacity(null).toQuery().indexOf("minCapacity=") < 0 && s.withAvailableOnly(false).toQuery().indexOf("available=") < 0,
                    "Each chip removal drops only its own parameter");
            check(s.withSlots(null).toQuery().contains("minCapacity=3"), "Removing slots keeps the other filters");

            System.out.println("\n--- 6. Card price = booking price (same PricingService) ---");
            List<RoomSlot> slots = Arrays.asList(slot("S1", 1, 259000), slot("S2", 2, 259000), slot("S3", 3, 289000), slot("S4", 4, 399000));
            PricingService.DiscountRates rates = new PricingService.DiscountRates(10, 20, 50);
            Quote q2 = new PricingService().quote(slots, Arrays.asList("S1", "S2"), rates);
            check(q2.getTotal() == 466200 && q2.getDiscountPercent() == 10, "Morning+Afternoon 2 x 259,000 -10% = 466,200 VND");
            long jsTotal = (259000 + 259000) - Math.round((259000 + 259000) * 10 / 100.0);
            check(jsTotal == q2.getTotal(), "Same total as slot-matrix.js rounding (Math.round)");
            Quote q3 = new PricingService().quote(slots, Arrays.asList("S2", "S3", "S4"), rates);
            check(q3.getTotal() == Math.round((259000 + 289000 + 399000) * 0.8), "3 slots -20%");
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
