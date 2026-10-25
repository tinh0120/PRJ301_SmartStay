package job;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runner that executes all 9 SmartStay background scheduler jobs in spec-table order.
 * Each job runs in its own try/catch block and manages its own database transaction;
 * an unhandled exception in one job never prevents subsequent jobs from executing.
 */
public class SchedulerRunner implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(SchedulerRunner.class.getName());

    @Override
    public void run() {
        LOGGER.fine("SchedulerRunner tick started.");

        // 1. ExpirePendingBookings
        try {
            SchedulerJobs.expirePendingBookings();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Scheduler job ExpirePendingBookings failed", e);
        }

        // 2. ExpirePaymentLinks
        try {
            SchedulerJobs.expirePaymentLinks();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Scheduler job ExpirePaymentLinks failed", e);
        }

        // 3. ActivateCredentials
        try {
            SchedulerJobs.activateCredentials();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Scheduler job ActivateCredentials failed", e);
        }

        // 4. HandleNoShow
        try {
            SchedulerJobs.handleNoShow();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Scheduler job HandleNoShow failed", e);
        }

        // 5. AutoCheckout
        try {
            SchedulerJobs.autoCheckout();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Scheduler job AutoCheckout failed", e);
        }

        // 6. ExpireCredentials
        try {
            SchedulerJobs.expireCredentials();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Scheduler job ExpireCredentials failed", e);
        }

        // 7. ExpireStaleAssignments
        try {
            SchedulerJobs.expireStaleAssignments();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Scheduler job ExpireStaleAssignments failed", e);
        }

        // 8. PurgeExpiredPinSecrets
        try {
            SchedulerJobs.purgeExpiredPinSecrets();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Scheduler job PurgeExpiredPinSecrets failed", e);
        }

        // 9. DeviceOfflineSweep
        try {
            SchedulerJobs.deviceOfflineSweep();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Scheduler job DeviceOfflineSweep failed", e);
        }

        // 10. Check upcoming check-in emails (30 min reminder)
        try {
            SchedulerJobs.checkUpcomingCheckinEmails();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Scheduler job CheckUpcomingCheckinEmails failed", e);
        }

        // 11. Check upcoming check-out emails (30 min reminder)
        try {
            SchedulerJobs.checkUpcomingCheckoutEmails();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Scheduler job CheckUpcomingCheckoutEmails failed", e);
        }

        // 12. Check overdue checkout for buzzer alarm via MQTT
        try {
            SchedulerJobs.checkOverdueCheckoutBuzzer();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Scheduler job CheckOverdueCheckoutBuzzer failed", e);
        }

        LOGGER.fine("SchedulerRunner tick completed.");
    }
}
