package model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Access log entry entity representing physical door unlock attempts.
 * Security Note: Raw door access PINs are strictly never stored in or retrieved from ACCESS_LOG.
 */
public class AccessLogEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long accessLogId;
    private Long credentialId;
    private String deviceId;
    private String requestId;
    private Timestamp attemptedAt;
    private String attemptedAtFormatted;
    private String result;
    private String reasonCode;

    // Linked context through credential -> booking -> room / guest
    private Long bookingId;
    private String bookingCode;
    private String guestName;
    private String roomNumber;

    public AccessLogEntry() {
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Long getAccessLogId() {
        return accessLogId;
    }

    public void setAccessLogId(Long accessLogId) {
        this.accessLogId = accessLogId;
    }

    public Long getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(Long credentialId) {
        this.credentialId = credentialId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Timestamp getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(Timestamp attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public String getAttemptedAtFormatted() {
        return attemptedAtFormatted;
    }

    public void setAttemptedAtFormatted(String attemptedAtFormatted) {
        this.attemptedAtFormatted = attemptedAtFormatted;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }
}
