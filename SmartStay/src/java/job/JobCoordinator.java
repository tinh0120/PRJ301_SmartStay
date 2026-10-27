package job;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Serializes job cycles (FastCloseJobWorker, SlowWorker, manual sync, demo time jumps)
 * on the shared {@link SchedulerJobs#JOB_LOCK}.
 *
 * Only database work belongs inside the lock. Slow side effects such as sending e-mail
 * must be collected inside and executed after the lock is released.
 */
public final class JobCoordinator {

    private static final Logger LOGGER = Logger.getLogger(JobCoordinator.class.getName());

    private JobCoordinator() {
    }

    /**
     * Runs the task while holding the job lock, waiting as long as needed.
     */
    public static void runExclusive(Runnable task) {
        ReentrantLock lock = SchedulerJobs.JOB_LOCK;
        lock.lock();
        try {
            task.run();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Runs the task while holding the job lock, waiting at most timeoutMs for it.
     *
     * @return true if the task ran, false if the lock could not be acquired in time
     *         (or the waiting thread was interrupted).
     */
    public static boolean tryRunExclusive(long timeoutMs, Runnable task) {
        ReentrantLock lock = SchedulerJobs.JOB_LOCK;
        boolean acquired;
        try {
            acquired = lock.tryLock(Math.max(0L, timeoutMs), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        if (!acquired) {
            LOGGER.warning("JobCoordinator: job lock busy, gave up after " + timeoutMs + " ms.");
            return false;
        }
        try {
            task.run();
            return true;
        } finally {
            lock.unlock();
        }
    }
}
