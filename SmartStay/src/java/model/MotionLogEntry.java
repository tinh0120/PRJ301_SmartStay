package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Motion log entry entity representing PIR and distance sensor events.
 */
public class MotionLogEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long motionLogId;
    private String deviceId;
    private String zoneId;
    private String eventId;
    private Timestamp detectedAt;
    private String detectedAtFormatted;
    private BigDecimal distanceCm;
    private String snapshotPath;
    private Timestamp receivedAt;

    public MotionLogEntry() {
    }

    public Long getMotionLogId() {
        return motionLogId;
    }

    public void setMotionLogId(Long motionLogId) {
        this.motionLogId = motionLogId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Timestamp getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Timestamp detectedAt) {
        this.detectedAt = detectedAt;
    }

    public String getDetectedAtFormatted() {
        return detectedAtFormatted;
    }

    public void setDetectedAtFormatted(String detectedAtFormatted) {
        this.detectedAtFormatted = detectedAtFormatted;
    }

    public BigDecimal getDistanceCm() {
        return distanceCm;
    }

    public void setDistanceCm(BigDecimal distanceCm) {
        this.distanceCm = distanceCm;
    }

    public String getSnapshotPath() {
        return snapshotPath;
    }

    public void setSnapshotPath(String snapshotPath) {
        this.snapshotPath = snapshotPath;
    }

    public Timestamp getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Timestamp receivedAt) {
        this.receivedAt = receivedAt;
    }
}
