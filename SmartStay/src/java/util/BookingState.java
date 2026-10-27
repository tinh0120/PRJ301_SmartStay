package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Guest-facing booking display helpers (Task R5).
 * <ul>
 *   <li>{@link #of(String, String)}: ONE friendly state from booking + payment status, so a
 *       PENDING_PAYMENT booking with a FAILED payment shows "Payment failed", not two badges.</li>
 *   <li>{@link #stayTimes}: "16:30 → 09:00 (+1 day)"; {@link #stayLine}: "25/10 · 16:30 → 09:00 (+1 day)".</li>
 *   <li>{@link #slotNames}: "S3+S4" or slot rows -> "Evening + Overnight".</li>
 * </ul>
 */
public final class BookingState {

    /** The one visible action of a row. */
    public enum Action {
        PAY,
        PIN,
        DETAILS
    }

    private final String key;
    private final String label;
    private final String tone;
    private final Action action;
    private final String actionLabel;

    private BookingState(String key, String label, String tone, Action action, String actionLabel) {
        this.key = key;
        this.label = label;
        this.tone = tone;
        this.action = action;
        this.actionLabel = actionLabel;
    }

    /** Stable key for CSS/tests, e.g. "payment-failed". */
    public String getKey() { return key; }
    /** Text shown to the guest, e.g. "Awaiting payment". */
    public String getLabel() { return label; }
    /** ss-badge modifier: success, warning, danger or neutral. */
    public String getTone() { return tone; }
    public Action getAction() { return action; }
    /** Button text: "Pay now", "Retry payment", "View PIN" or "View details". */
    public String getActionLabel() { return actionLabel; }
    public boolean isPayAction() { return action == Action.PAY; }
    public boolean isPinAction() { return action == Action.PIN; }

    /** Friendly state from BOOKING.status and the latest PAYMENT.status ("NONE" when no payment). */
    public static BookingState of(String bookingStatus, String paymentStatus) {
        String b = (bookingStatus == null) ? "" : bookingStatus.trim().toUpperCase();
        String p = (paymentStatus == null) ? "" : paymentStatus.trim().toUpperCase();
        switch (b) {
            case "PENDING_PAYMENT":
                if ("FAILED".equals(p)) {
                    return new BookingState("payment-failed", "Payment failed", "danger", Action.PAY, "Retry payment");
                }
                return new BookingState("awaiting-payment", "Awaiting payment", "warning", Action.PAY, "Pay now");
            case "CONFIRMED":
                return new BookingState("confirmed", "Confirmed", "success", Action.PIN, "View PIN");
            case "CHECKED_IN":
                return new BookingState("checked-in", "Checked in", "success", Action.PIN, "View PIN");
            case "CHECKED_OUT":
            case "COMPLETED":
                return new BookingState("completed", "Completed", "neutral", Action.DETAILS, "View details");
            case "CANCELLED":
                return new BookingState("cancelled", "Cancelled", "neutral", Action.DETAILS, "View details");
            case "EXPIRED":
                return new BookingState("expired", "Expired", "neutral", Action.DETAILS, "View details");
            default:
                return new BookingState("unknown", b.isEmpty() ? "Unknown" : ElFunctions.humanStatus(b), "neutral",
                        Action.DETAILS, "View details");
        }
    }

    /**
     * ONE admin badge combining booking + payment (Task R7), e.g. "Confirmed · Paid",
     * "Cancelled · Refund pending", "Expired · No-show". Normal states are success/neutral,
     * states that need an operator are warning/danger.
     */
    public static BookingState admin(String bookingStatus, String paymentStatus, String expireReason) {
        String b = (bookingStatus == null) ? "" : bookingStatus.trim().toUpperCase();
        String p = (paymentStatus == null) ? "" : paymentStatus.trim().toUpperCase();
        switch (b) {
            case "PENDING_PAYMENT":
                if ("FAILED".equals(p)) {
                    return new BookingState("payment-failed", "Payment failed", "danger", Action.DETAILS, "Detail");
                }
                if ("REVIEW_REQUIRED".equals(p)) {
                    return new BookingState("payment-review", "Payment needs review", "warning", Action.DETAILS, "Detail");
                }
                return new BookingState("awaiting-payment", "Awaiting payment", "warning", Action.DETAILS, "Detail");
            case "CONFIRMED":
                if ("PAID".equals(p)) {
                    return new BookingState("confirmed-paid", "Confirmed · Paid", "success", Action.DETAILS, "Detail");
                }
                if ("REVIEW_REQUIRED".equals(p)) {
                    return new BookingState("confirmed-review", "Confirmed · Payment review", "warning", Action.DETAILS, "Detail");
                }
                return new BookingState("confirmed", "Confirmed", "success", Action.DETAILS, "Detail");
            case "CHECKED_IN":
                return new BookingState("checked-in", "Checked in", "success", Action.DETAILS, "Detail");
            case "CHECKED_OUT":
            case "COMPLETED":
                return new BookingState("completed", "Completed", "neutral", Action.DETAILS, "Detail");
            case "CANCELLED":
                if ("REFUND_PENDING".equals(p)) {
                    return new BookingState("cancelled-refund-pending", "Cancelled · Refund pending", "warning", Action.DETAILS, "Detail");
                }
                if ("REFUNDED".equals(p)) {
                    return new BookingState("cancelled-refunded", "Cancelled · Refunded", "neutral", Action.DETAILS, "Detail");
                }
                if ("PAID".equals(p)) {
                    return new BookingState("cancelled-no-refund", "Cancelled · No refund", "neutral", Action.DETAILS, "Detail");
                }
                return new BookingState("cancelled", "Cancelled", "neutral", Action.DETAILS, "Detail");
            case "EXPIRED":
                if ("NO_SHOW".equalsIgnoreCase(expireReason)) {
                    return new BookingState("expired-no-show", "Expired · No-show", "danger", Action.DETAILS, "Detail");
                }
                return new BookingState("expired", "Expired", "neutral", Action.DETAILS, "Detail");
            default:
                return new BookingState("unknown", b.isEmpty() ? "Unknown" : ElFunctions.humanStatus(b), "neutral",
                        Action.DETAILS, "Detail");
        }
    }

    /** "26/10 · Morning + Afternoon · 09:30 → 19:30" (Task R7 admin rows). */
    public static String slotLine(LocalDateTime start, LocalDateTime end, String slotNames) {
        if (start == null || end == null) {
            return slotNames == null ? "" : slotNames;
        }
        String names = (slotNames == null || slotNames.isEmpty()) ? "" : slotNames + " · ";
        return start.format(DateTimeFormatter.ofPattern("dd/MM")) + " · " + names + stayTimes(start, end);
    }

    /** "16:30 → 09:00 (+1 day)", or "" when a time is missing. */
    public static String stayTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return "";
        }
        DateTimeFormatter hm = DateTimeFormatter.ofPattern("HH:mm");
        String s = start.format(hm) + " → " + end.format(hm);
        long days = end.toLocalDate().toEpochDay() - start.toLocalDate().toEpochDay();
        if (days == 1) {
            s += " (+1 day)";
        } else if (days > 1) {
            s += " (+" + days + " days)";
        }
        return s;
    }

    /** "25/10 · 16:30 → 09:00 (+1 day)". */
    public static String stayLine(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return "";
        }
        return start.format(DateTimeFormatter.ofPattern("dd/MM")) + " · " + stayTimes(start, end);
    }

    /**
     * "Evening + Overnight" from "S3+S4" (anything after " · " is ignored) or from a list of
     * slot names. Unknown codes are kept as they are.
     */
    public static String slotNames(String codes) {
        if (codes == null || codes.trim().isEmpty()) {
            return "";
        }
        String raw = codes;
        int dot = raw.indexOf('·');
        if (dot >= 0) {
            raw = raw.substring(0, dot);
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split("[+,]")) {
            String c = part.trim();
            if (!c.isEmpty()) {
                out.add(RoomFilter.slotLabel(c));
            }
        }
        return String.join(" + ", out);
    }

    /** "Evening + Overnight" from slot names already in day order. */
    public static String joinNames(List<String> names) {
        List<String> out = new ArrayList<>();
        if (names != null) {
            for (String n : names) {
                if (n != null && !n.trim().isEmpty()) {
                    out.add(n.trim());
                }
            }
        }
        return String.join(" + ", out);
    }
}
