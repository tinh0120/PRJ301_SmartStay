package util;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Global Virtual Clock Service (Task R2 - Virtual Time Management).
 * Enables shifting business time forward during interactive demos while preserving
 * absolute REAL time for hardware IoT heartbeats, network timeouts, and security tokens.
 *
 * Thread-safe, stored purely in-memory (resets to 0 on restart).
 * Enforces forward-only time advancement during simulation.
 */
public final class ClockService {

    private static final Logger LOGGER = Logger.getLogger(ClockService.class.getName());

    // In-memory offset in milliseconds from real system time
    private static final AtomicLong OFFSET_MILLIS = new AtomicLong(0L);

    private ClockService() {
        // Utility class
    }

    /**
     * Current virtual business time.
     * Calculated as real system time + simulated forward offset.
     */
    public static LocalDateTime now() {
        long offset = OFFSET_MILLIS.get();
        if (offset == 0L) {
            return LocalDateTime.now();
        }
        return LocalDateTime.now().plusNanos(offset * 1_000_000L);
    }

    /**
     * Current virtual business time formatted as java.sql.Timestamp.
     */
    public static Timestamp nowTimestamp() {
        return Timestamp.valueOf(now());
    }

    /**
     * Real, unshifted system time (always actual physical wall clock).
     */
    public static LocalDateTime realNow() {
        return LocalDateTime.now();
    }

    /**
     * Real, unshifted system timestamp.
     */
    public static Timestamp realNowTimestamp() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    /**
     * Active simulated forward offset in minutes.
     */
    public static long offsetMinutes() {
        return OFFSET_MILLIS.get() / (60 * 1000L);
    }

    /**
     * Active simulated forward offset in milliseconds.
     */
    public static long offsetMillis() {
        return OFFSET_MILLIS.get();
    }

    /**
     * Advances virtual time forward to a target LocalDateTime.
     * Enforces strict forward-only progression (throws if target is before current virtual now).
     *
     * @param target Target moment to simulate.
     * @throws IllegalArgumentException if target is null or earlier than current virtual now.
     */
    public static synchronized void advanceTo(LocalDateTime target) {
        if (target == null) {
            throw new IllegalArgumentException("Target simulation time cannot be null.");
        }
        LocalDateTime currentVirtualNow = now();
        if (target.isBefore(currentVirtualNow)) {
            throw new IllegalArgumentException(
                "Cannot advance clock backwards: target (" + target 
                + ") is earlier than current virtual time (" + currentVirtualNow + ")."
            );
        }

        long newOffsetMillis = Duration.between(realNow(), target).toMillis();
        if (newOffsetMillis < 0L) {
            newOffsetMillis = 0L;
        }
        OFFSET_MILLIS.set(newOffsetMillis);
        LOGGER.info("ClockService advanced: offset is now " + (newOffsetMillis / 60000L) + " minutes (simulated now: " + target + ")");
    }

    /**
     * Sets offset in minutes (forward only).
     */
    public static synchronized void setOffsetMinutes(long minutes) {
        if (minutes < 0L) {
            throw new IllegalArgumentException("Offset minutes cannot be negative.");
        }
        long newMillis = minutes * 60 * 1000L;
        if (newMillis < OFFSET_MILLIS.get()) {
            throw new IllegalArgumentException("Cannot advance clock backwards. Current offset is " + offsetMinutes() + " mins.");
        }
        OFFSET_MILLIS.set(newMillis);
        LOGGER.info("ClockService set offset to " + minutes + " minutes (simulated now: " + now() + ")");
    }

    /**
     * Resets simulated clock back to physical real time (offset = 0).
     */
    public static void reset() {
        OFFSET_MILLIS.set(0L);
        LOGGER.info("ClockService reset to real time (offset = 0).");
    }

    /**
     * Checks whether virtual time travel is currently active.
     */
    public static boolean isShifted() {
        return OFFSET_MILLIS.get() > 0L;
    }
}
