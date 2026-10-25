package model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Model representing a BOOKING record with joined room number and latest payment status.
 */
public class Booking implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CHECKED_IN = "CHECKED_IN";
    public static final String STATUS_CHECKED_OUT = "CHECKED_OUT";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private long bookingId;
    private String bookingCode;
    private long guestId;
    private long roomId;
    private String roomNumber;
    private String roomDisplayName;
    private LocalDate slotDate;
    private LocalDateTime checkinAt;
    private LocalDateTime checkoutAt;
    private long subtotal;
    private int discountPercent;
    private long discountAmount;
    private long total;
    private String status;
    private String expireReason;
    private LocalDateTime paymentDeadline;
    private String note;
    private String cancelReason;
    private LocalDateTime cancelledAt;
    private Long cancelledBy;
    private String paymentStatus = "NONE";
    private Timestamp createdAt;
    private String slotsSummary;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private LocalDateTime actualCheckinAt;
    private LocalDateTime actualCheckoutAt;
    private boolean allowEarlyCheckin;
    private Long createdBy;
    private String createdByName;
    private boolean hasHeldSlots;

    public Booking() {
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

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomDisplayName() {
        return roomDisplayName;
    }

    public void setRoomDisplayName(String roomDisplayName) {
        this.roomDisplayName = roomDisplayName;
    }

    public String getRoomTitle() {
        if (roomDisplayName != null && !roomDisplayName.trim().isEmpty()) {
            return roomDisplayName;
        }
        if (roomNumber != null && !roomNumber.trim().isEmpty()) {
            return util.ElFunctions.getDisplayName(roomNumber);
        }
        return "Room";
    }

    public String getRoomDisplayTitle() {
        return getRoomTitle();
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public void setSlotDate(LocalDate slotDate) {
        this.slotDate = slotDate;
    }

    public LocalDateTime getCheckinAt() {
        return checkinAt;
    }

    public void setCheckinAt(LocalDateTime checkinAt) {
        this.checkinAt = checkinAt;
    }

    public LocalDateTime getCheckoutAt() {
        return checkoutAt;
    }

    public void setCheckoutAt(LocalDateTime checkoutAt) {
        this.checkoutAt = checkoutAt;
    }

    public long getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(long subtotal) {
        this.subtotal = subtotal;
    }

    public long getSubtotalAmount() {
        return subtotal;
    }

    public void setSubtotalAmount(long subtotalAmount) {
        this.subtotal = subtotalAmount;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getTotalAmount() {
        return total;
    }

    public void setTotalAmount(long totalAmount) {
        this.total = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExpireReason() {
        return expireReason;
    }

    public void setExpireReason(String expireReason) {
        this.expireReason = expireReason;
    }

    public LocalDateTime getPaymentDeadline() {
        return paymentDeadline;
    }

    public void setPaymentDeadline(LocalDateTime paymentDeadline) {
        this.paymentDeadline = paymentDeadline;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Long getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(Long cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getSlotsSummary() {
        return slotsSummary;
    }

    public void setSlotsSummary(String slotsSummary) {
        this.slotsSummary = slotsSummary;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public LocalDateTime getActualCheckinAt() {
        return actualCheckinAt;
    }

    public void setActualCheckinAt(LocalDateTime actualCheckinAt) {
        this.actualCheckinAt = actualCheckinAt;
    }

    public LocalDateTime getActualCheckoutAt() {
        return actualCheckoutAt;
    }

    public void setActualCheckoutAt(LocalDateTime actualCheckoutAt) {
        this.actualCheckoutAt = actualCheckoutAt;
    }

    public boolean isAllowEarlyCheckin() {
        return allowEarlyCheckin;
    }

    public boolean getAllowEarlyCheckin() {
        return allowEarlyCheckin;
    }

    public void setAllowEarlyCheckin(boolean allowEarlyCheckin) {
        this.allowEarlyCheckin = allowEarlyCheckin;
    }

    public boolean isEarlyCheckin() {
        return actualCheckinAt != null && checkinAt != null && actualCheckinAt.isBefore(checkinAt);
    }

    public boolean getEarlyCheckin() {
        return isEarlyCheckin();
    }

    public long getEarlyCheckinMinutes() {
        if (actualCheckinAt != null && checkinAt != null && actualCheckinAt.isBefore(checkinAt)) {
            return java.time.Duration.between(actualCheckinAt, checkinAt).toMinutes();
        }
        return 0;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public boolean isHasHeldSlots() {
        return hasHeldSlots;
    }

    public boolean getHasHeldSlots() {
        return hasHeldSlots;
    }

    public void setHasHeldSlots(boolean hasHeldSlots) {
        this.hasHeldSlots = hasHeldSlots;
    }

    // =========================================================================
    // INNER DTO: Quote (merged into Booking)
    // =========================================================================

    /**
     * Model representing a calculated price quotation for a booking selection.
     */
    public static class Quote implements Serializable {

        private static final long serialVersionUID = 1L;

        private List<RoomSlot> slots;
        private long subtotal;
        private int discountPercent;
        private long discountAmount;
        private long total;
        private int slotCount;

        public Quote() {
            this.slots = Collections.emptyList();
        }

        public Quote(List<RoomSlot> slots, long subtotal, int discountPercent, long discountAmount, long total, int slotCount) {
            this.slots = slots != null ? slots : Collections.<RoomSlot>emptyList();
            this.subtotal = subtotal;
            this.discountPercent = discountPercent;
            this.discountAmount = discountAmount;
            this.total = total;
            this.slotCount = slotCount;
        }

        public List<RoomSlot> getSlots() {
            return slots;
        }

        public void setSlots(List<RoomSlot> slots) {
            this.slots = slots != null ? slots : Collections.<RoomSlot>emptyList();
        }

        public long getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(long subtotal) {
            this.subtotal = subtotal;
        }

        public int getDiscountPercent() {
            return discountPercent;
        }

        public void setDiscountPercent(int discountPercent) {
            this.discountPercent = discountPercent;
        }

        public long getDiscountAmount() {
            return discountAmount;
        }

        public void setDiscountAmount(long discountAmount) {
            this.discountAmount = discountAmount;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public int getSlotCount() {
            return slotCount;
        }

        public void setSlotCount(int slotCount) {
            this.slotCount = slotCount;
        }
    }
}
