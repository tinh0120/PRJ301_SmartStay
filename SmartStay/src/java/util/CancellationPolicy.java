package util;

import java.time.LocalDateTime;

/**
 * 48-hour cancellation policy (Task R3).
 * <ul>
 *   <li>Deadline = start of the FIRST booked slot (BOOKING.checkin_at) minus CANCEL_FREE_HOURS.</li>
 *   <li>Cancelling at or before the deadline: 100% refund. After it: allowed, but no refund.</li>
 *   <li>Unpaid (PENDING_PAYMENT) bookings can always be cancelled; nothing was charged.</li>
 *   <li>A booking made when check-in is already closer than the window is non-refundable from the start.</li>
 * </ul>
 * Pure logic; callers pass the virtual time (ClockService.now()).
 */
public final class CancellationPolicy {

    /** What a guest cancellation does with the money. */
    public enum Outcome {
        FULL_REFUND,
        NO_REFUND,
        NOTHING_PAID
    }

    private CancellationPolicy() {
    }

    /** Last moment of free cancellation; null when the first slot start is unknown. */
    public static LocalDateTime deadline(LocalDateTime firstSlotStart) {
        return (firstSlotStart == null) ? null : firstSlotStart.minusHours(AppConstants.CANCEL_FREE_HOURS);
    }

    /** True while now is not after the deadline (exactly 48 h before still refunds in full). */
    public static boolean isRefundable(LocalDateTime firstSlotStart, LocalDateTime now) {
        LocalDateTime d = deadline(firstSlotStart);
        return d != null && now != null && !now.isAfter(d);
    }

    /** Outcome of a guest cancellation right now. */
    public static Outcome outcomeFor(String bookingStatus, LocalDateTime firstSlotStart, LocalDateTime now) {
        if ("PENDING_PAYMENT".equalsIgnoreCase(bookingStatus)) {
            return Outcome.NOTHING_PAID;
        }
        return isRefundable(firstSlotStart, now) ? Outcome.FULL_REFUND : Outcome.NO_REFUND;
    }

    /** One-line policy sentence for the confirmation e-mail (English UI copy). */
    public static String policySentence(LocalDateTime firstSlotStart, LocalDateTime now) {
        LocalDateTime d = deadline(firstSlotStart);
        if (d == null) {
            return "";
        }
        if (isRefundable(firstSlotStart, now)) {
            return "Free cancellation until " + d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + " (" + AppConstants.CANCEL_FREE_HOURS + " hours before check-in); after that the booking is non-refundable.";
        }
        return "This booking is non-refundable because check-in is less than "
                + AppConstants.CANCEL_FREE_HOURS + " hours away.";
    }

    /** View model for JSPs, built by the EL function e:cancellationInfo. */
    public static Info info(String bookingStatus, LocalDateTime firstSlotStart, LocalDateTime now) {
        return new Info(deadline(firstSlotStart), isRefundable(firstSlotStart, now),
                outcomeFor(bookingStatus, firstSlotStart, now));
    }

    /** Read-only result: refundable (time-based), deadline, and the outcome for the booking's status. */
    public static final class Info {
        private final LocalDateTime deadline;
        private final boolean refundable;
        private final Outcome outcome;

        Info(LocalDateTime deadline, boolean refundable, Outcome outcome) {
            this.deadline = deadline;
            this.refundable = refundable;
            this.outcome = outcome;
        }

        /** Last moment of free cancellation. */
        public LocalDateTime getDeadline() {
            return deadline;
        }

        /** True while cancelling would refund 100% (regardless of payment status). */
        public boolean isRefundable() {
            return refundable;
        }

        /** FULL_REFUND, NO_REFUND or NOTHING_PAID for a cancellation right now. */
        public Outcome getOutcome() {
            return outcome;
        }

        public boolean isNothingPaid() {
            return outcome == Outcome.NOTHING_PAID;
        }

        public boolean isKnown() {
            return deadline != null;
        }

        public int getFreeHours() {
            return AppConstants.CANCEL_FREE_HOURS;
        }
    }
}
