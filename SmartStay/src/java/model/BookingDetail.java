package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model representing detailed booking information for display on the booking detail page.
 */
public class BookingDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Row representing an individual slot booked in BOOKING_SLOT.
     */
    public static class BookingSlotRow implements Serializable {

        private static final long serialVersionUID = 1L;
        private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

        private long bookingSlotId;
        private long roomSlotId;
        private String slotCode;
        private String slotName;
        private String timeRange;
        private LocalDateTime startAt;
        private LocalDateTime endAt;
        private long listPrice;
        private String holdStatus;

        public BookingSlotRow() {
        }

        public long getBookingSlotId() {
            return bookingSlotId;
        }

        public void setBookingSlotId(long bookingSlotId) {
            this.bookingSlotId = bookingSlotId;
        }

        public long getRoomSlotId() {
            return roomSlotId;
        }

        public void setRoomSlotId(long roomSlotId) {
            this.roomSlotId = roomSlotId;
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

        public String getTimeRange() {
            if (timeRange != null && !timeRange.trim().isEmpty()) {
                return timeRange;
            }
            if (startAt != null && endAt != null) {
                String s = startAt.format(TIME_FMT);
                String e = endAt.format(TIME_FMT);
                if (endAt.toLocalDate().isAfter(startAt.toLocalDate())) {
                    return s + "\u2013" + e + " (+1 day)";
                }
                return s + "\u2013" + e;
            }
            return "";
        }

        public void setTimeRange(String timeRange) {
            this.timeRange = timeRange;
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

        public long getListPrice() {
            return listPrice;
        }

        public void setListPrice(long listPrice) {
            this.listPrice = listPrice;
        }

        public String getHoldStatus() {
            return holdStatus;
        }

        public void setHoldStatus(String holdStatus) {
            this.holdStatus = holdStatus;
        }
    }

    public static class CredentialMeta implements Serializable {
        private static final long serialVersionUID = 1L;

        private long credentialId;
        private String credentialType;
        private String deviceId;
        private String status;
        private LocalDateTime validFrom;
        private LocalDateTime validUntil;
        private int usedCount;
        private LocalDateTime revokedAt;
        private String revokeReason;

        public CredentialMeta() {
        }

        public long getCredentialId() { return credentialId; }
        public void setCredentialId(long credentialId) { this.credentialId = credentialId; }

        public String getCredentialType() { return credentialType; }
        public void setCredentialType(String credentialType) { this.credentialType = credentialType; }

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public LocalDateTime getValidFrom() { return validFrom; }
        public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }

        public LocalDateTime getValidUntil() { return validUntil; }
        public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }

        public int getUsedCount() { return usedCount; }
        public void setUsedCount(int usedCount) { this.usedCount = usedCount; }

        public LocalDateTime getRevokedAt() { return revokedAt; }
        public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

        public String getRevokeReason() { return revokeReason; }
        public void setRevokeReason(String revokeReason) { this.revokeReason = revokeReason; }
    }

    public static class AccessLogRow implements Serializable {
        private static final long serialVersionUID = 1L;

        private long accessLogId;
        private String deviceId;
        private String requestId;
        private LocalDateTime attemptedAt;
        private String result;
        private String reasonCode;
        private Long credentialId;

        public AccessLogRow() {
        }

        public long getAccessLogId() { return accessLogId; }
        public void setAccessLogId(long accessLogId) { this.accessLogId = accessLogId; }

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }

        public LocalDateTime getAttemptedAt() { return attemptedAt; }
        public void setAttemptedAt(LocalDateTime attemptedAt) { this.attemptedAt = attemptedAt; }

        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }

        public String getReasonCode() { return reasonCode; }
        public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

        public Long getCredentialId() { return credentialId; }
        public void setCredentialId(Long credentialId) { this.credentialId = credentialId; }
    }

    public static class AlertLogRow implements Serializable {
        private static final long serialVersionUID = 1L;

        private long alertId;
        private String alertType;
        private String severity;
        private String status;
        private String message;
        private LocalDateTime createdAt;

        public AlertLogRow() {
        }

        public long getAlertId() { return alertId; }
        public void setAlertId(long alertId) { this.alertId = alertId; }

        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    private Booking booking;
    private List<BookingSlotRow> slots = new ArrayList<>();
    private List<Payment> payments = new ArrayList<>();
    private List<CredentialMeta> credentials = new ArrayList<>();
    private List<AccessLogRow> accessLogs = new ArrayList<>();
    private List<AlertLogRow> alertLogs = new ArrayList<>();
    private Room room;
    private boolean canCancel;

    public BookingDetail() {
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public List<BookingSlotRow> getSlots() {
        return slots != null ? slots : Collections.<BookingSlotRow>emptyList();
    }

    public void setSlots(List<BookingSlotRow> slots) {
        this.slots = slots != null ? slots : new ArrayList<BookingSlotRow>();
    }

    public List<Payment> getPayments() {
        return payments != null ? payments : Collections.<Payment>emptyList();
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments != null ? payments : new ArrayList<Payment>();
    }

    public List<CredentialMeta> getCredentials() {
        return credentials != null ? credentials : Collections.<CredentialMeta>emptyList();
    }

    public void setCredentials(List<CredentialMeta> credentials) {
        this.credentials = credentials != null ? credentials : new ArrayList<CredentialMeta>();
    }

    public List<AccessLogRow> getAccessLogs() {
        return accessLogs != null ? accessLogs : Collections.<AccessLogRow>emptyList();
    }

    public void setAccessLogs(List<AccessLogRow> accessLogs) {
        this.accessLogs = accessLogs != null ? accessLogs : new ArrayList<AccessLogRow>();
    }

    public List<AlertLogRow> getAlertLogs() {
        return alertLogs != null ? alertLogs : Collections.<AlertLogRow>emptyList();
    }

    public void setAlertLogs(List<AlertLogRow> alertLogs) {
        this.alertLogs = alertLogs != null ? alertLogs : new ArrayList<AlertLogRow>();
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public boolean isCanCancel() {
        return canCancel;
    }

    public boolean getCanCancel() {
        return canCancel;
    }

    public void setCanCancel(boolean canCancel) {
        this.canCancel = canCancel;
    }
}
