package model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model representing a customer review for a room.
 */
public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    private long reviewId;
    private long bookingId;
    private String bookingCode;
    private long guestId;
    private long roomId;
    private int rating;
    private String comment;
    private String guestName;
    private String status;
    private Timestamp createdAt;
    private String roomNumber;
    private String roomDisplayName;
    private String guestEmail;
    private Long moderatedBy;
    private String moderatorName;
    private Timestamp moderatedAt;

    public Review() {
    }

    public long getReviewId() {
        return reviewId;
    }

    public void setReviewId(long reviewId) {
        this.reviewId = reviewId;
    }

    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public long getGuestId() {
        return guestId;
    }

    public void setGuestId(long guestId) {
        this.guestId = guestId;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
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

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public Long getModeratedBy() {
        return moderatedBy;
    }

    public void setModeratedBy(Long moderatedBy) {
        this.moderatedBy = moderatedBy;
    }

    public String getModeratorName() {
        return moderatorName;
    }

    public void setModeratorName(String moderatorName) {
        this.moderatorName = moderatorName;
    }

    public Timestamp getModeratedAt() {
        return moderatedAt;
    }

    public void setModeratedAt(Timestamp moderatedAt) {
        this.moderatedAt = moderatedAt;
    }

    /**
     * Helper to mask a full name to "First Name + Last Initial" (e.g. "Grace Nguyen" -> "Grace N.").
     */
    public static String maskGuestName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Anonymous";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        String last = parts[parts.length - 1];
        char initial = last.charAt(0);
        return parts[0] + " " + Character.toUpperCase(initial) + ".";
    }

    /**
     * Renders rating stars as HTML entity &#9733; characters.
     */
    public String getStarsHtml() {
        int r = Math.max(1, Math.min(5, rating));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < r; i++) {
            sb.append("&#9733;");
        }
        return sb.toString();
    }
}
