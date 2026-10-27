package util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Pure time-travel math for the demo tools (Task R2-B2). No database, no clock state.
 * A booking's stay runs from its first slot start to its last slot end; overnight slots
 * end on the next day.
 */
public final class DemoTime {

    /** Jump targets offered by POST /demo/time/jump. */
    public enum Target {
        BEFORE_CHECKIN_15,
        CHECKIN_NOW,
        BEFORE_CHECKOUT_15,
        CHECKOUT_NOW;

        public boolean isCheckout() {
            return this == BEFORE_CHECKOUT_15 || this == CHECKOUT_NOW;
        }

        /** Parses a target name (case-insensitive); returns null when unknown. */
        public static Target parse(String s) {
            if (s == null) {
                return null;
            }
            try {
                return Target.valueOf(s.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** Minutes before check-in / check-out used by the two "15 min before" targets. */
    public static final int LEAD_MINUTES = 15;

    /**
     * Seconds added after the last slot end for CHECKOUT_NOW so the strict
     * "now > end_at" checks of the auto-checkout and credential-expiry jobs fire at once.
     */
    public static final int CHECKOUT_MARGIN_SECONDS = 1;

    private DemoTime() {
    }

    /** Start of a slot on the given date. */
    public static LocalDateTime slotStart(LocalDate date, LocalTime start) {
        return LocalDateTime.of(date, start);
    }

    /** End of a slot on the given date; an overnight slot ends on the next day. */
    public static LocalDateTime slotEnd(LocalDate date, LocalTime end, boolean crossesMidnight) {
        return LocalDateTime.of(crossesMidnight ? date.plusDays(1) : date, end);
    }

    /** Earliest start among slot windows ({start, end} pairs). */
    public static LocalDateTime firstStart(List<LocalDateTime[]> windows) {
        LocalDateTime first = null;
        for (LocalDateTime[] w : windows) {
            if (w != null && w[0] != null && (first == null || w[0].isBefore(first))) {
                first = w[0];
            }
        }
        return first;
    }

    /** Latest end among slot windows ({start, end} pairs). */
    public static LocalDateTime lastEnd(List<LocalDateTime[]> windows) {
        LocalDateTime last = null;
        for (LocalDateTime[] w : windows) {
            if (w != null && w[1] != null && (last == null || w[1].isAfter(last))) {
                last = w[1];
            }
        }
        return last;
    }

    /** The moment a target points to for a stay [firstStart, lastEnd]. */
    public static LocalDateTime targetMoment(Target target, LocalDateTime firstStart, LocalDateTime lastEnd) {
        if (target == null || firstStart == null || lastEnd == null) {
            throw new IllegalArgumentException("Target and stay window are required.");
        }
        switch (target) {
            case BEFORE_CHECKIN_15:
                return firstStart.minusMinutes(LEAD_MINUTES);
            case CHECKIN_NOW:
                return firstStart;
            case BEFORE_CHECKOUT_15:
                return lastEnd.minusMinutes(LEAD_MINUTES);
            case CHECKOUT_NOW:
            default:
                return lastEnd.plusSeconds(CHECKOUT_MARGIN_SECONDS);
        }
    }

    /** Forward-only rule: a target is reachable only if it is not earlier than the current virtual time. */
    public static boolean isReachable(LocalDateTime target, LocalDateTime virtualNow) {
        return target != null && virtualNow != null && !target.isBefore(virtualNow);
    }
}
