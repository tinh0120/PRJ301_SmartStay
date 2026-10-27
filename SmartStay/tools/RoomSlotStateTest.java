package tools;

import model.RoomSlot;

/**
 * Unit test for RoomSlot.effectiveForRoom (Task R8): the room status overrides the slot so the
 * room list and the Slots page never contradict the room status.
 */
public class RoomSlotStateTest {

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
        System.out.println("      ROOM SLOT DISPLAY STATE VERIFICATION (R8)   ");
        System.out.println("==================================================");

        try {
            System.out.println("\n--- 1. Room status overrides the slot ---");
            check("MAINTENANCE".equals(RoomSlot.effectiveForRoom("AVAILABLE", "MAINTENANCE", true)),
                    "Bug fix: MAINTENANCE room no longer shows a slot as Available");
            check("MAINTENANCE".equals(RoomSlot.effectiveForRoom("BOOKED", "MAINTENANCE", true)), "Maintenance wins over Booked");
            check("NEEDS_CLEANING".equals(RoomSlot.effectiveForRoom("AVAILABLE", "NEEDS_CLEANING", true)), "Needs cleaning room: free slot shows Cleaning");
            check("BOOKED".equals(RoomSlot.effectiveForRoom("BOOKED", "NEEDS_CLEANING", true)), "Needs cleaning room: booked slot stays Booked");
            check("INACTIVE".equals(RoomSlot.effectiveForRoom("AVAILABLE", "AVAILABLE", false)), "Hidden room: every slot Not offered");

            System.out.println("\n--- 2. Normal rooms keep the slot state ---");
            check("AVAILABLE".equals(RoomSlot.effectiveForRoom("AVAILABLE", "AVAILABLE", true)), "Available stays Available");
            check("BOOKED".equals(RoomSlot.effectiveForRoom("BOOKED", "OCCUPIED", true)), "Occupied room keeps Booked");
            check("MAINTENANCE".equals(RoomSlot.effectiveForRoom("MAINTENANCE", "AVAILABLE", true)), "Slot-level maintenance kept");

            System.out.println("\n--- 3. Labels ---");
            check("Available".equals(RoomSlot.stateLabel("AVAILABLE")) && "Booked".equals(RoomSlot.stateLabel("BOOKED")), "Available / Booked");
            check("Cleaning".equals(RoomSlot.stateLabel("NEEDS_CLEANING")) && "Maintenance".equals(RoomSlot.stateLabel("MAINTENANCE")), "Cleaning / Maintenance");
            check("Not offered".equals(RoomSlot.stateLabel("INACTIVE")), "Inactive -> Not offered");

            System.out.println("\n--- 4. Today difference note (Round 3b item 5) ---");
            RoomSlot slot = new RoomSlot();
            slot.setActive(true);
            slot.setStatus("AVAILABLE");
            slot.setTodayStatus("AVAILABLE");

            // Normal available slot -> null
            slot.computeTodayDifference("AVAILABLE", true);
            check(slot.getTodayDifference() == null, "Normal available slot shows no difference note");

            // Inactive slot -> null
            slot.setActive(false);
            slot.computeTodayDifference("AVAILABLE", true);
            check(slot.getTodayDifference() == null, "Inactive slot shows no difference note");

            // Room hidden -> "Today: not offered (room hidden)"
            slot.setActive(true);
            slot.computeTodayDifference("AVAILABLE", false);
            check("Today: not offered (room hidden)".equals(slot.getTodayDifference()), "Room hidden produces 'Today: not offered (room hidden)'");

            // Booked today -> "Today: booked"
            slot.setTodayStatus("BOOKED");
            slot.computeTodayDifference("AVAILABLE", true);
            check("Today: booked".equals(slot.getTodayDifference()), "Booked slot produces 'Today: booked'");

            // Room in maintenance -> "Today: maintenance (room in maintenance)"
            slot.setTodayStatus("MAINTENANCE");
            slot.computeTodayDifference("MAINTENANCE", true);
            check("Today: maintenance (room in maintenance)".equals(slot.getTodayDifference()), "Maintenance room produces 'Today: maintenance (room in maintenance)'");

            // Room needs cleaning -> "Today: cleaning (room needs cleaning)"
            slot.setTodayStatus("NEEDS_CLEANING");
            slot.computeTodayDifference("NEEDS_CLEANING", true);
            check("Today: cleaning (room needs cleaning)".equals(slot.getTodayDifference()), "Cleaning room produces 'Today: cleaning (room needs cleaning)'");
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
