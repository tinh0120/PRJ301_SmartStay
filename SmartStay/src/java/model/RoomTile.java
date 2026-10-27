package model;

import java.io.Serializable;

/**
 * One tile of the dashboard "Rooms" grid (Task R6): code, name, status and the next event.
 * Only problem states (Needs cleaning, Maintenance) are coloured.
 */
public class RoomTile implements Serializable {

    private static final long serialVersionUID = 1L;

    private long roomId;
    private String roomNumber;
    private String displayName;
    private String status;
    private String nextEvent;

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    /** "Check-in 14:00", "Check-out 26/10 09:00" or null when nothing is planned. */
    public String getNextEvent() { return nextEvent; }
    public void setNextEvent(String nextEvent) { this.nextEvent = nextEvent; }

    /** Human status label. */
    public String getStatusLabel() {
        if ("NEEDS_CLEANING".equalsIgnoreCase(status)) return "Needs cleaning";
        if ("MAINTENANCE".equalsIgnoreCase(status)) return "Maintenance";
        if ("OCCUPIED".equalsIgnoreCase(status)) return "Occupied";
        return "Available";
    }

    /** ss-status-dot modifier: warn / danger for problems, muted otherwise (normal is quiet). */
    public String getTone() {
        if ("NEEDS_CLEANING".equalsIgnoreCase(status)) return "warn";
        if ("MAINTENANCE".equalsIgnoreCase(status)) return "danger";
        return "muted";
    }

    public boolean isProblem() {
        return "NEEDS_CLEANING".equalsIgnoreCase(status) || "MAINTENANCE".equalsIgnoreCase(status);
    }
}
