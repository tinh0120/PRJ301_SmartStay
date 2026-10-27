package tools;

import util.ClockService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit test for util.ClockService (Task R2 - Virtual Clock Management).
 */
public class ClockServiceTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(boolean condition, String message) {
        if (condition) {
            System.out.println("  [PASS] " + message);
            passed++;
        } else {
            System.err.println("  [FAIL] " + message);
            failed++;
        }
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("       CLOCK SERVICE UNIT VERIFICATION (R2-B1)    ");
        System.out.println("==================================================");

        try {
            // Always start from clean reset
            ClockService.reset();

            // 1. Initial State
            System.out.println("\n--- 1. Initial State (Unshifted) ---");
            check(ClockService.offsetMinutes() == 0L, "Initial offsetMinutes() is 0");
            check(ClockService.offsetMillis() == 0L, "Initial offsetMillis() is 0");
            check(!ClockService.isShifted(), "isShifted() is false initially");

            LocalDateTime realNow = ClockService.realNow();
            LocalDateTime clockNow = ClockService.now();
            long diffMillis = Math.abs(Duration.between(realNow, clockNow).toMillis());
            check(diffMillis < 100L, "clockNow() matches realNow() within 100ms (diff: " + diffMillis + "ms)");

            // 2. Forward Advance via advanceTo()
            System.out.println("\n--- 2. Forward Time Travel via advanceTo() ---");
            LocalDateTime target1 = ClockService.realNow().plusHours(3);
            ClockService.advanceTo(target1);

            check(ClockService.isShifted(), "isShifted() is true after advanceTo()");
            long offsetMin = ClockService.offsetMinutes();
            check(offsetMin >= 179L && offsetMin <= 181L, "offsetMinutes() is ~180 (actual: " + offsetMin + ")");
            long targetDiff = Math.abs(Duration.between(target1, ClockService.now()).toMillis());
            check(targetDiff < 1000L, "now() matches target1 within 1000ms (diff: " + targetDiff + "ms)");
            check(ClockService.nowTimestamp() != null, "nowTimestamp() produces valid Timestamp");

            // Further forward advancement
            LocalDateTime target2 = target1.plusHours(2);
            ClockService.advanceTo(target2);
            long offsetMin2 = ClockService.offsetMinutes();
            check(offsetMin2 >= 299L && offsetMin2 <= 301L, "offsetMinutes() after second advance is ~300 (actual: " + offsetMin2 + ")");

            // 3. Backward Travel Guard (Throws IllegalArgumentException)
            System.out.println("\n--- 3. Backward Protection Guard ---");
            boolean threwOnBackward = false;
            try {
                LocalDateTime pastTarget = ClockService.realNow().plusHours(1); // Earlier than current simulated ~5h
                ClockService.advanceTo(pastTarget);
            } catch (IllegalArgumentException e) {
                threwOnBackward = true;
            }
            check(threwOnBackward, "advanceTo() backwards throws IllegalArgumentException");

            boolean threwOnNull = false;
            try {
                ClockService.advanceTo(null);
            } catch (IllegalArgumentException e) {
                threwOnNull = true;
            }
            check(threwOnNull, "advanceTo(null) throws IllegalArgumentException");

            // 4. setOffsetMinutes() validation
            System.out.println("\n--- 4. setOffsetMinutes() Validation ---");
            ClockService.setOffsetMinutes(400L);
            check(ClockService.offsetMinutes() == 400L, "setOffsetMinutes(400) sets offset to 400");

            boolean threwOnLowerOffset = false;
            try {
                ClockService.setOffsetMinutes(200L); // Lower than current 400
            } catch (IllegalArgumentException e) {
                threwOnLowerOffset = true;
            }
            check(threwOnLowerOffset, "setOffsetMinutes() with lower value throws IllegalArgumentException");

            boolean threwOnNegative = false;
            try {
                ClockService.setOffsetMinutes(-10L);
            } catch (IllegalArgumentException e) {
                threwOnNegative = true;
            }
            check(threwOnNegative, "setOffsetMinutes() with negative value throws IllegalArgumentException");

            // 5. Reset to Real Time
            System.out.println("\n--- 5. Reset to Physical Real Time ---");
            ClockService.reset();
            check(ClockService.offsetMinutes() == 0L, "offsetMinutes() is 0 after reset()");
            check(ClockService.offsetMillis() == 0L, "offsetMillis() is 0 after reset()");
            check(!ClockService.isShifted(), "isShifted() is false after reset()");

            // 6. Concurrency & Thread-Safety Test
            System.out.println("\n--- 6. Concurrency and Thread Safety ---");
            int threadCount = 10;
            int iterationsPerThread = 1000;
            ExecutorService pool = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                pool.submit(() -> {
                    try {
                        for (int i = 0; i < iterationsPerThread; i++) {
                            LocalDateTime n = ClockService.now();
                            if (n == null) {
                                errorCount.incrementAndGet();
                            }
                            if (threadId == 0 && i % 200 == 0) {
                                ClockService.setOffsetMinutes(10L + (i / 10));
                            }
                        }
                    } catch (Exception ex) {
                        errorCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            pool.shutdown();
            check(completed, "Concurrent threads completed within 10 seconds");
            check(errorCount.get() == 0, "Zero errors during concurrent read/write stress (errors: " + errorCount.get() + ")");

            // Final clean reset
            ClockService.reset();
            check(!ClockService.isShifted(), "Clean reset after concurrent test");

        } catch (Exception e) {
            e.printStackTrace();
            failed++;
        }

        System.out.println("\n==================================================");
        System.out.println("TOTAL CHECKS: " + (passed + failed));
        System.out.println("PASSED:       " + passed);
        System.out.println("FAILED:       " + failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
