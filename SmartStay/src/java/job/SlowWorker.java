package job;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Slow Worker (Cycle: 60 seconds).
 * Dedicated to recurring operational sweeps and notifications:
 * - AutoCheckout of past-due stays
 * - HandleNoShow releases/flags
 * - Expiring stale cleaning assignments
 * - Purging expired PIN secrets & sweep offline IoT devices
 * - 30-minute upcoming check-in and check-out emails
 * - Silent check-out reminder sweep (replaces loud buzzer)
 *
 * Reminder e-mails are collected while holding the job lock and sent after it is released.
 */
public class SlowWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(SlowWorker.class.getName());

    @Override
    public void run() {
        final List<SchedulerJobs.PendingMail> mails = new ArrayList<>();
        JobCoordinator.runExclusive(new Runnable() {
            @Override
            public void run() {
                mails.addAll(runJobsCollectingMails());
            }
        });
        SchedulerJobs.sendPendingMails(mails);
    }

    /**
     * One cycle of the slow jobs. The caller must hold the job lock (see JobCoordinator)
     * and must send the returned mails with SchedulerJobs.sendPendingMails AFTER unlocking.
     */
    public static List<SchedulerJobs.PendingMail> runJobsCollectingMails() {
        List<SchedulerJobs.PendingMail> mails = new ArrayList<>();
        LOGGER.fine("SlowWorker periodic sweep started.");

        // 1. AutoCheckout
        try {
            SchedulerJobs.autoCheckout();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "SlowWorker AutoCheckout failed", e);
        }

        // 2. HandleNoShow
        try {
            SchedulerJobs.handleNoShow();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "SlowWorker HandleNoShow failed", e);
        }

        // 3. ExpireCredentials
        try {
            SchedulerJobs.expireCredentials();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "SlowWorker ExpireCredentials failed", e);
        }

        // 4. ExpireStaleAssignments
        try {
            SchedulerJobs.expireStaleAssignments();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "SlowWorker ExpireStaleAssignments failed", e);
        }

        // 5. PurgeExpiredPinSecrets
        try {
            SchedulerJobs.purgeExpiredPinSecrets();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "SlowWorker PurgeExpiredPinSecrets failed", e);
        }

        // 6. DeviceOfflineSweep
        try {
            SchedulerJobs.deviceOfflineSweep();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "SlowWorker DeviceOfflineSweep failed", e);
        }

        // 7. Collect upcoming check-in emails (30 min reminder)
        try {
            mails.addAll(SchedulerJobs.collectCheckinReminders());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "SlowWorker CheckUpcomingCheckinEmails failed", e);
        }

        // 8. Collect upcoming check-out emails (30 min reminder)
        try {
            mails.addAll(SchedulerJobs.collectCheckoutReminders());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "SlowWorker CheckUpcomingCheckoutEmails failed", e);
        }

        // 9. Silent overdue checkout sweep (no loud buzzer)
        try {
            SchedulerJobs.checkOverdueCheckoutBuzzer();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "SlowWorker CheckOverdueCheckoutBuzzer failed", e);
        }

        LOGGER.fine("SlowWorker periodic sweep finished.");
        return mails;
    }
}
