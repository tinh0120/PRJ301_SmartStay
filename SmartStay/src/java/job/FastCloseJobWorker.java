package job;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fast Close-Job Worker (Cycle: 5 seconds).
 * Dedicated to high-urgency lifecycle events:
 * - Expiring pending bookings with elapsed 10-minute hold window.
 * - Expiring pending payment gateway transactions.
 * - Activating access credentials when check-in window opens.
 * - Auto checkout when booking stay concludes.
 * - Expiring access credentials after check-out.
 * - Handling no-show bookings when grace period elapses.
 */
public class FastCloseJobWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(FastCloseJobWorker.class.getName());

    @Override
    public void run() {
        LOGGER.finest("FastCloseJobWorker tick started.");

        // 1. ExpirePendingBookings (10-minute hold window)
        try {
            SchedulerJobs.expirePendingBookings();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "FastCloseJobWorker ExpirePendingBookings failed", e);
        }

        // 2. ExpirePaymentLinks
        try {
            SchedulerJobs.expirePaymentLinks();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "FastCloseJobWorker ExpirePaymentLinks failed", e);
        }

        // 3. ActivateCredentials
        try {
            SchedulerJobs.activateCredentials();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "FastCloseJobWorker ActivateCredentials failed", e);
        }

        // 4. AutoCheckout
        try {
            SchedulerJobs.autoCheckout();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "FastCloseJobWorker AutoCheckout failed", e);
        }

        // 5. ExpireCredentials
        try {
            SchedulerJobs.expireCredentials();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "FastCloseJobWorker ExpireCredentials failed", e);
        }

        // 6. HandleNoShow
        try {
            SchedulerJobs.handleNoShow();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "FastCloseJobWorker HandleNoShow failed", e);
        }

        LOGGER.finest("FastCloseJobWorker tick finished.");
    }
}
