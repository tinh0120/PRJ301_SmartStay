package model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Security alert entry entity representing system and hardware alert events.
 */
public class AlertEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long alertId;
    private String alertType;
    private String severity;
    private String status;
    private String message;
    private String deviceId;
    private Long roomId;
    private String zoneId;
    private Long bookingId;
    private Long motionLogId;
    private Long thresholdId;
    private String snapshotPath;
    private Long acknowledgedBy;
    private String acknowledgedByName;
    private Timestamp acknowledgedAt;
    private String acknowledgedAtFormatted;
    private Long resolvedBy;
    private String resolvedByName;
    private Timestamp resolvedAt;
    private String resolvedAtFormatted;
    private String resolutionNote;
    private Timestamp createdAt;
    private String createdAtFormatted;

    public AlertEntry() {
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getMotionLogId() {
        return motionLogId;
    }

    public void setMotionLogId(Long motionLogId) {
        this.motionLogId = motionLogId;
    }

    public Long getThresholdId() {
        return thresholdId;
    }

    public void setThresholdId(Long thresholdId) {
        this.thresholdId = thresholdId;
    }

    public String getSnapshotPath() {
        return snapshotPath;
    }

    public void setSnapshotPath(String snapshotPath) {
        this.snapshotPath = snapshotPath;
    }

    public Long getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(Long acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public String getAcknowledgedByName() {
        return acknowledgedByName;
    }

    public void setAcknowledgedByName(String acknowledgedByName) {
        this.acknowledgedByName = acknowledgedByName;
    }

    public Timestamp getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Timestamp acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public String getAcknowledgedAtFormatted() {
        return acknowledgedAtFormatted;
    }

    public void setAcknowledgedAtFormatted(String acknowledgedAtFormatted) {
        this.acknowledgedAtFormatted = acknowledgedAtFormatted;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getResolvedByName() {
        return resolvedByName;
    }

    public void setResolvedByName(String resolvedByName) {
        this.resolvedByName = resolvedByName;
    }

    public Timestamp getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Timestamp resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolvedAtFormatted() {
        return resolvedAtFormatted;
    }

    public void setResolvedAtFormatted(String resolvedAtFormatted) {
        this.resolvedAtFormatted = resolvedAtFormatted;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedAtFormatted() {
        return createdAtFormatted;
    }

    public void setCreatedAtFormatted(String createdAtFormatted) {
        this.createdAtFormatted = createdAtFormatted;
    }

    // --- R10 display fields ---
    private String deviceName;
    private String roomNumber;
    private String bookingCode;

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    /** True only when snapshot_path is set, the file exists and the viewer may see snapshots (set by the servlet). */
    private boolean snapshotAvailable;

    public boolean isSnapshotAvailable() { return snapshotAvailable; }
    public void setSnapshotAvailable(boolean snapshotAvailable) { this.snapshotAvailable = snapshotAvailable; }

    /** Plain-words title from the central map (util.AlertTitles); the raw code stays in the detail view. */
    public String getTitle() {
        return util.AlertTitles.title(alertType);
    }

    /** Where it happened: device name, else room, else zone, else booking. */
    public String getSource() {
        if (deviceName != null && !deviceName.trim().isEmpty()) return deviceName;
        if (deviceId != null && !deviceId.trim().isEmpty()) return deviceId;
        if (roomNumber != null && !roomNumber.trim().isEmpty()) return "Room " + roomNumber;
        if (zoneId != null && !zoneId.trim().isEmpty()) return util.ActivityLog.zoneLabel(zoneId);
        if (bookingCode != null && !bookingCode.trim().isEmpty()) return "Booking " + bookingCode;
        return "";
    }

    /** ss-badge / severity modifier: low, medium, high or critical. */
    public String getSeverityTone() {
        return (severity == null) ? "low" : severity.trim().toLowerCase();
    }
}
