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

    public String getTodayStatus() {
        return todayStatus != null ? todayStatus : "AVAILABLE";
    }

    public void setTodayStatus(String todayStatus) {
        this.todayStatus = todayStatus;
    }

    /**
     * Determines operational status considering holds for a target date.
     */
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
