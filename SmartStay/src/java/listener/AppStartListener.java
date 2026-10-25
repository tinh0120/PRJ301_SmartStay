package listener;

import job.FastCloseJobWorker;
import job.SlowWorker;
import websocket.RealtimeWebSocketEndpoint;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Application lifecycle listener that starts and gracefully shuts down the background
 * scheduler executor services (Fast Close-Job Worker: 5s, Slow Worker: 60s).
 */
@WebListener
public class AppStartListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(AppStartListener.class.getName());

    private static ScheduledExecutorService fastScheduler;
    private static ScheduledExecutorService slowScheduler;
    private static final ExecutorService MANUAL_SYNC_EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "manual-sync-worker");
            t.setDaemon(true);
            return t;
        }
    });

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Initializing SmartStay background workers...");

        // 1. Fast Close-Job Worker (5 seconds cycle)
        fastScheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "smartstay-fast-worker");
                t.setDaemon(true);
                return t;
            }
        });
        fastScheduler.scheduleWithFixedDelay(new FastCloseJobWorker(), 1, 5, TimeUnit.SECONDS);
        LOGGER.info("SmartStay Fast Close-Job Worker started (5s fixed delay).");

        // 2. Slow Worker (60 seconds cycle)
        slowScheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "smartstay-slow-worker");
                t.setDaemon(true);
                return t;
            }
        });
        slowScheduler.scheduleWithFixedDelay(new SlowWorker(), 3, 60, TimeUnit.SECONDS);
        LOGGER.info("SmartStay Slow Worker started (60s fixed delay).");
    }

    /**
     * Triggers both workers to run immediately on-demand.
     */
    public static void triggerSyncNow() {
        LOGGER.info("Manual sync triggered by Admin.");
        MANUAL_SYNC_EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    new FastCloseJobWorker().run();
                    new SlowWorker().run();
                    RealtimeWebSocketEndpoint.broadcastEvent("SYNC_COMPLETED", null);
                    LOGGER.info("Manual sync completed successfully.");
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Manual sync encountered an error", e);
                }
            }
        });
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Shutting down SmartStay schedulers...");
        shutdownExecutor(fastScheduler, "FastScheduler");
        shutdownExecutor(slowScheduler, "SlowScheduler");
        MANUAL_SYNC_EXECUTOR.shutdownNow();

        LOGGER.info("Schedulers stopped.");
        util.DBUtil.shutdownPool();
    }

    private void shutdownExecutor(ScheduledExecutorService service, String name) {
        if (service != null) {
            service.shutdown();
            try {
                if (!service.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.warning(name + " did not terminate within 5 seconds; forcing shutdown.");
                    service.shutdownNow();
                }
            } catch (InterruptedException e) {
                LOGGER.warning("Interrupted while waiting for " + name + " termination; forcing shutdown.");
                service.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
