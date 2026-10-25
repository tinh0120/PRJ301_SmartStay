package tools;

import model.Quote;
import model.RoomSlot;
import service.PricingException;
import service.PricingService;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Standalone test runner for PricingService quotation logic.
 * <p>
 * Usage:
 * javac -d build/tools -cp build/web/WEB-INF/classes tools/PricingSelfTest.java
 * java -cp build/tools;build/web/WEB-INF/classes tools.PricingSelfTest
 */
public class PricingSelfTest {

    private static int totalPassed = 0;
    private static int totalFailed = 0;

    public static void main(String[] args) {
        System.out.println("=== Running PricingSelfTest ===");

        List<RoomSlot> standardSlots = createStandardA101Slots();
        PricingService.DiscountRates rates = new PricingService.DiscountRates(10, 20, 50);
        PricingService service = new PricingService();

        // 8 Spec 2.3 cases
        // 1. S1 -> 150k, 0%, 0, 150k
        assertSuccess(service, standardSlots, Arrays.asList("S1"), rates, 150000L, 0, 0L, 150000L, "Row 1: S1 (single slot, 0%)");

        // 2. S1+S2 -> 300k, 10%, 30k, 270k
        assertSuccess(service, standardSlots, Arrays.asList("S1", "S2"), rates, 300000L, 10, 30000L, 270000L, "Row 2: S1+S2 (2 slots, 10%)");

        // 3. S2+S3 -> 330k, 10%, 33k, 297k
        assertSuccess(service, standardSlots, Arrays.asList("S2", "S3"), rates, 330000L, 10, 33000L, 297000L, "Row 3: S2+S3 (2 slots, 10%)");

        // 4. S1+S3 -> Non-consecutive
        assertFailure(service, standardSlots, Arrays.asList("S1", "S3"), rates, PricingException.NON_CONSECUTIVE_SLOTS,
                PricingService.MSG_NON_CONSECUTIVE, "Row 4: S1+S3 (non-consecutive)");

        // 5. S3+S4 -> 480k, 10%, 48k, 432k
        assertSuccess(service, standardSlots, Arrays.asList("S3", "S4"), rates, 480000L, 10, 48000L, 432000L, "Row 5: S3+S4 (2 slots, 10%)");

        // 6. S1+S2+S3 -> 480k, 20%, 96k, 384k
        assertSuccess(service, standardSlots, Arrays.asList("S1", "S2", "S3"), rates, 480000L, 20, 96000L, 384000L, "Row 6: S1+S2+S3 (3 slots, 20%)");

        // 7. S1+S2+S4 -> Non-consecutive
        assertFailure(service, standardSlots, Arrays.asList("S1", "S2", "S4"), rates, PricingException.NON_CONSECUTIVE_SLOTS,
                PricingService.MSG_NON_CONSECUTIVE, "Row 7: S1+S2+S4 (non-consecutive)");

        // 8. S1+S2+S3+S4 -> 780k, 50%, 390k, 390k
        assertSuccess(service, standardSlots, Arrays.asList("S1", "S2", "S3", "S4"), rates, 780000L, 50, 390000L, 390000L, "Row 8: All 4 slots (50%)");

        // Extra cases
        // 9. Out of order ["S3","s2"] = same as S2+S3
        assertSuccess(service, standardSlots, Arrays.asList("S3", "s2"), rates, 330000L, 10, 33000L, 297000L, "Extra 9: Out of order ['S3','s2']");

        // 10. Duplicate ["S1","S1"] -> DUPLICATE_SLOT
        assertFailure(service, standardSlots, Arrays.asList("S1", "S1"), rates, PricingException.DUPLICATE_SLOT,
                "Each time slot can be selected only once.", "Extra 10: Duplicate slot ['S1','S1']");

        // 11. Unknown ["S5"] -> UNKNOWN_SLOT
        assertFailure(service, standardSlots, Arrays.asList("S5"), rates, PricingException.UNKNOWN_SLOT,
                "One or more selected time slots are not offered for this room.", "Extra 11: Unknown slot ['S5']");

        // 12. Empty [] -> EMPTY_SELECTION
        assertFailure(service, standardSlots, new ArrayList<String>(), rates, PricingException.EMPTY_SELECTION,
                "Please select at least one time slot.", "Extra 12: Empty selection");

        // 13. HALF_UP rounding check: subtotal 155,005 with 10% discount -> 15,501 (15,500.5 rounds up)
        List<RoomSlot> roundingSlots = new ArrayList<>();
        RoomSlot r1 = new RoomSlot();
        r1.setSlotCode("S1");
        r1.setSortOrder(1);
        r1.setListPrice(155005L);
        r1.setActive(true);
        roundingSlots.add(r1);

        RoomSlot r2 = new RoomSlot();
        r2.setSlotCode("S2");
        r2.setSortOrder(2);
        r2.setListPrice(0L);
        r2.setActive(true);
        roundingSlots.add(r2);

        assertSuccess(service, roundingSlots, Arrays.asList("S1", "S2"), rates, 155005L, 10, 15501L, 139504L, "Extra 13: HALF_UP rounding (15,500.5 -> 15,501)");

        System.out.println("=== Test Summary: " + totalPassed + " PASSED, " + totalFailed + " FAILED ===");
        if (totalFailed > 0) {
            System.exit(1);
        }
    }

    private static List<RoomSlot> createStandardA101Slots() {
        List<RoomSlot> slots = new ArrayList<>();

        RoomSlot s1 = new RoomSlot();
        s1.setSlotCode("S1");
        s1.setSlotName("Morning");
        s1.setStartTime(LocalTime.of(9, 30));
        s1.setEndTime(LocalTime.of(12, 30));
        s1.setCrossesMidnight(false);
        s1.setListPrice(150000L);
        s1.setSortOrder(1);
        s1.setActive(true);
        slots.add(s1);

        RoomSlot s2 = new RoomSlot();
        s2.setSlotCode("S2");
        s2.setSlotName("Afternoon");
        s2.setStartTime(LocalTime.of(13, 0));
        s2.setEndTime(LocalTime.of(16, 0));
        s2.setCrossesMidnight(false);
        s2.setListPrice(150000L);
        s2.setSortOrder(2);
        s2.setActive(true);
        slots.add(s2);

        RoomSlot s3 = new RoomSlot();
        s3.setSlotCode("S3");
        s3.setSlotName("Evening");
        s3.setStartTime(LocalTime.of(16, 30));
        s3.setEndTime(LocalTime.of(19, 30));
        s3.setCrossesMidnight(false);
        s3.setListPrice(180000L);
        s3.setSortOrder(3);
        s3.setActive(true);
        slots.add(s3);

        RoomSlot s4 = new RoomSlot();
        s4.setSlotCode("S4");
        s4.setSlotName("Overnight");
        s4.setStartTime(LocalTime.of(20, 0));
        s4.setEndTime(LocalTime.of(9, 0));
        s4.setCrossesMidnight(true);
        s4.setListPrice(300000L);
        s4.setSortOrder(4);
        s4.setActive(true);
        slots.add(s4);

        return slots;
    }

    private static void assertSuccess(PricingService service, List<RoomSlot> slots, List<String> codes,
                                      PricingService.DiscountRates rates, long expSubtotal, int expPercent,
                                      long expDiscount, long expTotal, String desc) {
        try {
            Quote q = service.quote(slots, codes, rates);
            if (q.getSubtotal() == expSubtotal && q.getDiscountPercent() == expPercent
                    && q.getDiscountAmount() == expDiscount && q.getTotal() == expTotal) {
                System.out.println("PASS: " + desc);
                totalPassed++;
            } else {
                System.err.println("FAIL: " + desc + " => Expected [" + expSubtotal + ", " + expPercent + "%, "
                        + expDiscount + ", " + expTotal + "] but got [" + q.getSubtotal() + ", "
                        + q.getDiscountPercent() + "%, " + q.getDiscountAmount() + ", " + q.getTotal() + "]");
                totalFailed++;
            }
        } catch (Exception e) {
            System.err.println("FAIL: " + desc + " => Unexpected exception: " + e.getMessage());
            totalFailed++;
        }
    }

    private static void assertFailure(PricingService service, List<RoomSlot> slots, List<String> codes,
                                      PricingService.DiscountRates rates, String expCode, String expMsg, String desc) {
        try {
            service.quote(slots, codes, rates);
            System.err.println("FAIL: " + desc + " => Expected exception with code " + expCode + " but succeeded");
            totalFailed++;
        } catch (PricingException e) {
            if (expCode.equals(e.getCode()) && (expMsg == null || expMsg.equals(e.getMessage()))) {
                System.out.println("PASS: " + desc);
                totalPassed++;
            } else {
                System.err.println("FAIL: " + desc + " => Expected [" + expCode + ", " + expMsg + "] but got ["
                        + e.getCode() + ", " + e.getMessage() + "]");
                totalFailed++;
            }
        } catch (Exception e) {
            System.err.println("FAIL: " + desc + " => Unexpected exception type: " + e.getClass().getName());
            totalFailed++;
        }
    }
}
