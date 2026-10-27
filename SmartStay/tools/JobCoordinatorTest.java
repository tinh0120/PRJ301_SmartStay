package tools;

import job.JobCoordinator;
import job.SchedulerJobs;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit test for job.JobCoordinator (Task R2-B1.1 - job lock helpers).
 * No database needed: only the shared SchedulerJobs.JOB_LOCK is exercised.
 */
public class JobCoordinatorTest {

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
        System.out.println("     JOB COORDINATOR UNIT VERIFICATION (R2-B1.1)  ");
        System.out.println("==================================================");

        try {
            // 1. runExclusive runs the task and releases the lock
            System.out.println("\n--- 1. runExclusive ---");
            final AtomicBoolean heldInside = new AtomicBoolean(false);
            JobCoordinator.runExclusive(new Runnable() {
                @Override
                public void run() {
                    heldInside.set(SchedulerJobs.JOB_LOCK.isHeldByCurrentThread());
                }
            });
            check(heldInside.get(), "Task runs while holding JOB_LOCK");
            check(!SchedulerJobs.JOB_LOCK.isLocked(), "JOB_LOCK released after runExclusive");

            // 2. Lock released even when the task throws
            System.out.println("\n--- 2. Exception safety ---");
            try {
                JobCoordinator.runExclusive(new Runnable() {
                    @Override
                    public void run() {
                        throw new IllegalStateException("boom");
                    }
                });
                check(false, "Exception propagates from runExclusive");
            } catch (IllegalStateException expected) {
                check(true, "Exception propagates from runExclusive");
            }
            check(!SchedulerJobs.JOB_LOCK.isLocked(), "JOB_LOCK released after a failing task");

            // 3. tryRunExclusive succeeds when the lock is free
            System.out.println("\n--- 3. tryRunExclusive (free lock) ---");
            final AtomicInteger runs = new AtomicInteger(0);
            boolean ran = JobCoordinator.tryRunExclusive(500L, new Runnable() {
                @Override
                public void run() {
                    runs.incrementAndGet();
                }
            });
            check(ran && runs.get() == 1, "tryRunExclusive returns true and runs the task once");
            check(!SchedulerJobs.JOB_LOCK.isLocked(), "JOB_LOCK released after tryRunExclusive");

            // 4. tryRunExclusive times out while another thread holds the lock
            System.out.println("\n--- 4. tryRunExclusive timeout ---");
            final CountDownLatch locked = new CountDownLatch(1);
            final CountDownLatch release = new CountDownLatch(1);
            Thread holder = new Thread(new Runnable() {
                @Override
                public void run() {
                    JobCoordinator.runExclusive(new Runnable() {
                        @Override
                        public void run() {
                            locked.countDown();
                            try {
                                release.await(10, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    });
                }
            }, "lock-holder");
            holder.start();
            check(locked.await(5, TimeUnit.SECONDS), "Holder thread acquired JOB_LOCK");

            final AtomicBoolean timedOutTaskRan = new AtomicBoolean(false);
            long t0 = System.nanoTime();
            boolean acquired = JobCoordinator.tryRunExclusive(300L, new Runnable() {
                @Override
                public void run() {
                    timedOutTaskRan.set(true);
                }
            });
            long waitedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
            check(!acquired, "tryRunExclusive returns false when the lock is busy");
            check(!timedOutTaskRan.get(), "Task is NOT executed on timeout");
            check(waitedMs >= 250L && waitedMs < 3000L, "Waited about the timeout (" + waitedMs + " ms for 300 ms)");

            release.countDown();
            holder.join(5000L);
            check(!SchedulerJobs.JOB_LOCK.isLocked(), "JOB_LOCK free after the holder finished");

            // 5. Works again after the holder released the lock
            System.out.println("\n--- 5. tryRunExclusive after release ---");
            boolean ranAgain = JobCoordinator.tryRunExclusive(500L, new Runnable() {
                @Override
                public void run() {
                    runs.incrementAndGet();
                }
            });
            check(ranAgain && runs.get() == 2, "tryRunExclusive succeeds once the lock is free again");

            // 6. Interrupted waiter returns false and keeps the interrupt flag
            System.out.println("\n--- 6. Interrupted waiter ---");
            final CountDownLatch locked2 = new CountDownLatch(1);
            final CountDownLatch release2 = new CountDownLatch(1);
            Thread holder2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    JobCoordinator.runExclusive(new Runnable() {
                        @Override
                        public void run() {
                            locked2.countDown();
                            try {
                                release2.await(10, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    });
                }
            }, "lock-holder-2");
            holder2.start();
            locked2.await(5, TimeUnit.SECONDS);
            Thread.currentThread().interrupt();
            boolean interruptedResult = JobCoordinator.tryRunExclusive(2000L, new Runnable() {
                @Override
                public void run() {
                }
            });
            boolean flagKept = Thread.interrupted(); // also clears the flag
            check(!interruptedResult, "Interrupted tryRunExclusive returns false");
            check(flagKept, "Interrupt flag is preserved");
            release2.countDown();
            holder2.join(5000L);
        } catch (Throwable t) {
            System.err.println("Unexpected error: " + t);
            t.printStackTrace();
            failed++;
        }

        System.out.println("\n==================================================");
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        System.out.println("==================================================");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
