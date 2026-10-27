package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Model representing a ROOM_TIME_SLOT record.
 */
public class RoomSlot implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private long roomSlotId;
    private long roomId;
    private String slotCode;
    private String slotName;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean crossesMidnight;
    private long listPrice;
    private int sortOrder;
    private boolean active;
    private String status = "AVAILABLE";

    public RoomSlot() {
    }

    public long getRoomSlotId() {
        return roomSlotId;
    }

    public void setRoomSlotId(long roomSlotId) {
        this.roomSlotId = roomSlotId;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public String getSlotCode() {
        return slotCode;
    }

    public void setSlotCode(String slotCode) {
        this.slotCode = slotCode;
    }

    public String getSlotName() {
        return slotName;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public boolean isCrossesMidnight() {
        return crossesMidnight;
    }

    public void setCrossesMidnight(boolean crossesMidnight) {
        this.crossesMidnight = crossesMidnight;
    }

    public long getListPrice() {
        return listPrice;
    }

    public void setListPrice(long listPrice) {
        this.listPrice = listPrice;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private String todayStatus;
    private String todayDifference;

    public String getTodayStatus() {
        return todayStatus != null ? todayStatus : "AVAILABLE";
    }

    public void setTodayStatus(String todayStatus) {
        this.todayStatus = todayStatus;
    }

    public String getTodayDifference() {
        if (todayDifference != null) {
            return todayDifference;
        }
        if (!active) {
            return null;
        }
        if ("BOOKED".equalsIgnoreCase(todayStatus)) {
            return "Today: booked";
        }
        if ("NEEDS_CLEANING".equalsIgnoreCase(todayStatus) && "AVAILABLE".equalsIgnoreCase(status)) {
            return "Today: cleaning";
        }
        if ("MAINTENANCE".equalsIgnoreCase(todayStatus) && "AVAILABLE".equalsIgnoreCase(status)) {
            return "Today: maintenance";
        }
        return null;
    }

    public void setTodayDifference(String todayDifference) {
        this.todayDifference = todayDifference;
    }

    /**
     * Computes the difference between today's operational state and the row's own controls (Round 3b item 5).
     * Returns a note ONLY when today's state differs, e.g. "Today: not offered (room hidden)",
     * "Today: booked", "Today: maintenance (room in maintenance)", "Today: cleaning (room needs cleaning)".
     * Returns null for normal available slots and normal inactive slots.
     */
    public void computeTodayDifference(String roomStatus, boolean roomActive) {
        this.todayDifference = calculateTodayDifference(roomStatus, roomActive);
    }

    public String calculateTodayDifference(String roomStatus, boolean roomActive) {
        if (!active) {
            return null;
        }
        if (!roomActive) {
            return "Today: not offered (room hidden)";
        }
        if ("BOOKED".equalsIgnoreCase(todayStatus)) {
            return "Today: booked";
        }
        if ("MAINTENANCE".equalsIgnoreCase(roomStatus)) {
            if (!"MAINTENANCE".equalsIgnoreCase(status)) {
                return "Today: maintenance (room in maintenance)";
            }
            return null;
        }
        if ("NEEDS_CLEANING".equalsIgnoreCase(roomStatus)) {
            if ("AVAILABLE".equalsIgnoreCase(status)) {
                return "Today: cleaning (room needs cleaning)";
            }
            return null;
        }
        return null;
    }

    /**
     * Determines operational status considering holds for a target date.
     */
    /**
     * Slot state as shown on the admin room list and the Slots page (Task R8): the ROOM status
     * overrides the slot. An inactive room shows every slot as INACTIVE, a MAINTENANCE room shows
     * MAINTENANCE everywhere, a NEEDS_CLEANING room shows its free slots as NEEDS_CLEANING
     * (booked slots stay BOOKED).
     */
    public static String effectiveForRoom(String slotState, String roomStatus, boolean roomActive) {
        if (!roomActive) {
            return "INACTIVE";
        }
        if ("MAINTENANCE".equalsIgnoreCase(roomStatus)) {
            return "MAINTENANCE";
        }
        if ("NEEDS_CLEANING".equalsIgnoreCase(roomStatus) && "AVAILABLE".equalsIgnoreCase(slotState)) {
            return "NEEDS_CLEANING";
        }
        return slotState;
    }

    /** Human label of a display state: Available, Booked, Cleaning, Maintenance, Not offered, Inactive. */
    public static String stateLabel(String state) {
        if ("BOOKED".equalsIgnoreCase(state)) return "Booked";
        if ("NEEDS_CLEANING".equalsIgnoreCase(state)) return "Cleaning";
        if ("MAINTENANCE".equalsIgnoreCase(state)) return "Maintenance";
        if ("INACTIVE".equalsIgnoreCase(state)) return "Not offered";
        return "Available";
    }

    /** Label of todayStatus for tooltips, e.g. "Morning: Available". */
    public String getTodayLabel() {
        return stateLabel(getTodayStatus());
    }

    public String getEffectiveStatus(Set<Long> heldSlotIds) {
        if (!active) {
            return "INACTIVE";
        }
        if (heldSlotIds != null && heldSlotIds.contains(roomSlotId)) {
            return "BOOKED";
        }
        if ("NEEDS_CLEANING".equalsIgnoreCase(status)) {
            return "NEEDS_CLEANING";
        }
        if ("MAINTENANCE".equalsIgnoreCase(status)) {
            return "MAINTENANCE";
        }
        return "AVAILABLE";
    }

    /**
     * Formats the slot's operating time range with an en-dash separator.
     */
    public String getTimeRange() {
        String s = (startTime != null) ? startTime.format(TIME_FMT) : "";
        String e = (endTime != null) ? endTime.format(TIME_FMT) : "";
        if (crossesMidnight) {
            return s + "\u2013" + e + " (+1 day)";
        }
        return s + "\u2013" + e;
    }

    // =========================================================================
    // INNER DTO: SlotAvailability (merged into RoomSlot)
    // =========================================================================

    /**
     * Model representing availability state of a room slot for a specific date.
     */
    public static class SlotAvailability implements Serializable {

        private static final long serialVersionUID = 1L;

        public static final String STATE_AVAILABLE = "AVAILABLE";
        public static final String STATE_HELD = "HELD";
        public static final String STATE_STARTED = "STARTED";
        public static final String STATE_NOT_OFFERED = "NOT_OFFERED";
        public static final String STATE_OUT_OF_RANGE = "OUT_OF_RANGE";

        private RoomSlot slot;
        private LocalDateTime startAt;
        private LocalDateTime endAt;
        private String state;
        private boolean selectable;
        private String label;

        public SlotAvailability() {
        }

        public SlotAvailability(RoomSlot slot, LocalDateTime startAt, LocalDateTime endAt,
                                String state, boolean selectable, String label) {
            this.slot = slot;
            this.startAt = startAt;
            this.endAt = endAt;
            this.state = state;
            this.selectable = selectable;
            this.label = label;
        }

        public RoomSlot getSlot() {
            return slot;
        }

        public void setSlot(RoomSlot slot) {
            this.slot = slot;
        }

        public LocalDateTime getStartAt() {
            return startAt;
        }

        public void setStartAt(LocalDateTime startAt) {
            this.startAt = startAt;
        }

        public LocalDateTime getEndAt() {
            return endAt;
        }

        public void setEndAt(LocalDateTime endAt) {
            this.endAt = endAt;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public boolean isSelectable() {
            return selectable;
        }

        public void setSelectable(boolean selectable) {
            this.selectable = selectable;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getCssClass() {
            if (state == null) {
                return "slot-not-offered";
            }
            switch (state) {
                case STATE_AVAILABLE:
                    return "slot-available";
                case STATE_HELD:
                    return "slot-held";
                case STATE_STARTED:
                    return "slot-started";
                case STATE_NOT_OFFERED:
                    return "slot-not-offered";
                case STATE_OUT_OF_RANGE:
                    return "slot-out-of-range";
                default:
                    return "slot-" + state.toLowerCase().replace('_', '-');
            }
        }
    }
}
