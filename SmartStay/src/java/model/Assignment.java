package model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a staff room-cleaning assignment and its linked cleaning session & credential.
 */
public class Assignment implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private long assignmentId;
    private long staffId;
    private String staffName;
    private String staffEmail;
    private long roomId;
    private String roomNumber;
    private String roomDisplayName;
    private String roomStatus;
    private long assignedBy;
    private String assignedByName;
    private LocalDateTime shiftStart;
    private LocalDateTime shiftEnd;
    private String status;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Cleaning timestamps
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // Linked access credential
    private Long credentialId;
    private String credentialStatus;
    private LocalDateTime credentialValidFrom;
    private LocalDateTime credentialValidUntil;

    public Assignment() {
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public long getStaffId() {
        return staffId;
    }

    public void setStaffId(long staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getStaffEmail() {
        return staffEmail;
    }

    public void setStaffEmail(String staffEmail) {
        this.staffEmail = staffEmail;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomDisplayName() {
        if (roomDisplayName != null && !roomDisplayName.trim().isEmpty()) {
            return roomDisplayName;
        }
        return util.ElFunctions.getDisplayName(roomNumber);
    }

    public void setRoomDisplayName(String roomDisplayName) {
        this.roomDisplayName = roomDisplayName;
    }

    public String getRoomTitle() {
        String dn = getRoomDisplayName();
        if (dn != null && !dn.trim().isEmpty()) {
            return dn;
        }
        return (roomNumber != null && !roomNumber.trim().isEmpty()) ? ("Room " + roomNumber) : "Room";
    }

    public String getRoomStatus() {
        return roomStatus;
    }

    public void setRoomStatus(String roomStatus) {
        this.roomStatus = roomStatus;
    }

    public long getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(long assignedBy) {
        this.assignedBy = assignedBy;
    }

    public String getAssignedByName() {
        return assignedByName;
    }

    public void setAssignedByName(String assignedByName) {
        this.assignedByName = assignedByName;
    }

    public LocalDateTime getShiftStart() {
        return shiftStart;
    }

    public void setShiftStart(LocalDateTime shiftStart) {
        this.shiftStart = shiftStart;
    }

    public LocalDateTime getShiftEnd() {
        return shiftEnd;
    }

    public void setShiftEnd(LocalDateTime shiftEnd) {
        this.shiftEnd = shiftEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(Long credentialId) {
        this.credentialId = credentialId;
    }

    public String getCredentialStatus() {
        return credentialStatus;
    }

    public void setCredentialStatus(String credentialStatus) {
        this.credentialStatus = credentialStatus;
    }

    public LocalDateTime getCredentialValidFrom() {
        return credentialValidFrom;
    }

    public void setCredentialValidFrom(LocalDateTime credentialValidFrom) {
        this.credentialValidFrom = credentialValidFrom;
    }

    public LocalDateTime getCredentialValidUntil() {
        return credentialValidUntil;
    }

    public void setCredentialValidUntil(LocalDateTime credentialValidUntil) {
        this.credentialValidUntil = credentialValidUntil;
    }

    public String getSessionStatus() {
        if (STATUS_COMPLETED.equalsIgnoreCase(status)) return "COMPLETED";
        if (STATUS_CANCELLED.equalsIgnoreCase(status)) return "CANCELLED";
        if (STATUS_EXPIRED.equalsIgnoreCase(status)) return "EXPIRED";
        if (startedAt != null) return "ACTIVE";
        return "PENDING";
    }

    public boolean isStarted() {
        return startedAt != null;
    }

    public boolean isAssigned() {
        return STATUS_ASSIGNED.equalsIgnoreCase(status);
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return STATUS_CANCELLED.equalsIgnoreCase(status);
    }
}
