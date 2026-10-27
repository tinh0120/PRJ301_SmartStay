package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Sensor alert threshold configuration entity.
 */
public class AlertThreshold implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long thresholdId;
    private String deviceId; // Nullable; null indicates a global threshold
    private String deviceName; // Display only via LEFT JOIN DEVICE
    private String metricType;
    private String operator;
    private BigDecimal thresholdValue;
    private String alertType;
    private String severity;
    private boolean enabled;
    private String description;
    private Long updatedBy;
    private Timestamp updatedAt;

    public AlertThreshold() {
    }

    public Long getThresholdId() {
        return thresholdId;
    }

    public void setThresholdId(Long thresholdId) {
        this.thresholdId = thresholdId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    /** Returns thresholdValue without trailing zeros (e.g. "30", "50", "12.5") for display. */
    public String getThresholdValueFormatted() {
        if (thresholdValue == null) {
            return "";
        }
        return thresholdValue.stripTrailingZeros().toPlainString();
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
