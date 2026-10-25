package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database utility class managing SQL Server JDBC connections with a built-in
 * lightweight connection pool.
 * This is the only class allowed to open connections via DriverManager.
 */
public final class DBUtil {

    private static final Logger LOGGER = Logger.getLogger(DBUtil.class.getName());

    private static volatile boolean initialized = false;
    private static String dbHost;
    private static String dbPort;
    private static String dbName;
    private static String dbUser;
    private static String dbPassword;
    private static String dbExtra;
    private static String jdbcUrl;

    private static final int DEFAULT_MAX_POOL_SIZE = 15;
    private static int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
    private static final BlockingQueue<PooledItem> pool = new LinkedBlockingQueue<>();
    private static final AtomicInteger activeCount = new AtomicInteger(0);

    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "SQL Server JDBC driver not found. Make sure lib/sqljdbc4.jar is added to Libraries.", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(DBUtil::shutdownPool, "DBUtil-ShutdownHook"));
    }

    private static class PooledItem {
        final Connection connection;
        volatile long lastUsed;

        PooledItem(Connection connection) {
            this.connection = connection;
            this.lastUsed = System.currentTimeMillis();
        }
    }

    private DBUtil() {
    }

    private static synchronized void loadConfigIfNeeded() throws SQLException {
        if (initialized) {
            return;
        }

        Properties props = new Properties();

        // 3. Properties from classpath
        try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load db.properties from classpath", e);
        }

        // 2. System property -Dsmartstay.db=<path>
        String customPath = System.getProperty("smartstay.db");
        if (customPath != null && !customPath.trim().isEmpty()) {
            File customFile = new File(customPath.trim());
            if (customFile.isFile() && customFile.canRead()) {
                try (InputStream in = new FileInputStream(customFile);
                     InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    Properties customProps = new Properties();
                    customProps.load(reader);
                    props.putAll(customProps);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to load database properties from " + customPath, e);
                }
            }
        }

        // Determine values with priority: 1. Env vars -> 2/3. props -> Defaults
        String envHost = System.getenv("SMARTSTAY_DB_HOST");
        String envPort = System.getenv("SMARTSTAY_DB_PORT");
        String envName = System.getenv("SMARTSTAY_DB_NAME");
        String envUser = System.getenv("SMARTSTAY_DB_USER");
        String envPassword = System.getenv("SMARTSTAY_DB_PASSWORD");
        String envExtra = System.getenv("SMARTSTAY_DB_EXTRA");

        dbHost = (envHost != null && !envHost.trim().isEmpty()) ? envHost.trim() : props.getProperty("db.host", "localhost").trim();
        dbPort = (envPort != null && !envPort.trim().isEmpty()) ? envPort.trim() : props.getProperty("db.port", "1433").trim();
        dbName = (envName != null && !envName.trim().isEmpty()) ? envName.trim() : props.getProperty("db.name", "SmartStayDB").trim();
        dbExtra = (envExtra != null) ? envExtra.trim() : props.getProperty("db.extra", "").trim();

        dbUser = (envUser != null && !envUser.trim().isEmpty()) ? envUser.trim() : props.getProperty("db.user");
        if (dbUser != null) {
            dbUser = dbUser.trim();
        }

        dbPassword = (envPassword != null) ? envPassword : props.getProperty("db.password");

        if (dbUser == null || dbUser.isEmpty() || dbPassword == null) {
            throw new SQLException(
                "Database settings not found. Copy src/java/db.properties.sample to src/java/db.properties and fill in db.user and db.password (or set the SMARTSTAY_DB_* environment variables)."
            );
        }

        String poolMaxStr = props.getProperty("db.pool.max");
        if (poolMaxStr != null && !poolMaxStr.trim().isEmpty()) {
            try {
                int parsed = Integer.parseInt(poolMaxStr.trim());
                if (parsed > 0) {
                    maxPoolSize = parsed;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("jdbc:sqlserver://").append(dbHost).append(":").append(dbPort)
                  .append(";databaseName=").append(dbName);
        if (dbExtra != null && !dbExtra.isEmpty()) {
            String extraClean = dbExtra.startsWith(";") ? dbExtra.substring(1) : dbExtra;
            if (!extraClean.isEmpty()) {
                urlBuilder.append(";").append(extraClean);
            }
        }

        jdbcUrl = urlBuilder.toString();
        initialized = true;

        // Pre-warm 2 connections for fast first-hit response
        for (int i = 0; i < 2; i++) {
            if (activeCount.get() < maxPoolSize) {
                try {
                    Connection c = createPhysicalConnection();
                    activeCount.incrementAndGet();
                    pool.offer(new PooledItem(c));
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Pre-warming connection failed: " + e.getMessage());
                    break;
                }
            }
        }
    }

    private static Connection createPhysicalConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET QUOTED_IDENTIFIER ON; SET ANSI_NULLS ON;");
        }
        return conn;
    }

    private static boolean isConnectionAlive(Connection conn, long lastUsed) {
        try {
            if (conn.isClosed()) {
                return false;
            }
            if (System.currentTimeMillis() - lastUsed > 5000L) {
                try {
                    return conn.isValid(1);
                } catch (Throwable t) {
                    try (Statement stmt = conn.createStatement()) {
                        return stmt.execute("SELECT 1");
                    } catch (SQLException e) {
                        return false;
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static void returnToPool(Connection rawConn) {
        if (rawConn == null) {
            return;
        }
        try {
            if (rawConn.isClosed()) {
                activeCount.decrementAndGet();
                return;
            }
            if (!rawConn.getAutoCommit()) {
                try {
                    rawConn.rollback();
                } catch (SQLException ignored) {
                }
                rawConn.setAutoCommit(true);
            }
            rawConn.clearWarnings();
            pool.offer(new PooledItem(rawConn));
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error resetting connection before returning to pool", e);
            try {
                rawConn.close();
            } catch (SQLException ignored) {
            }
            activeCount.decrementAndGet();
        }
    }

    private static Connection wrap(Connection rawConn) {
        return (Connection) Proxy.newProxyInstance(
            DBUtil.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            new PooledConnectionHandler(rawConn)
        );
    }

    /**
     * Borrows and returns a pooled JDBC connection to SQL Server.
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            loadConfigIfNeeded();
        }

        while (true) {
            PooledItem item = pool.poll();
            if (item != null) {
                if (isConnectionAlive(item.connection, item.lastUsed)) {
                    return wrap(item.connection);
                }
                try {
                    item.connection.close();
                } catch (SQLException ignored) {
                }
                activeCount.decrementAndGet();
                continue;
            }

            int current = activeCount.get();
            if (current < maxPoolSize) {
                if (activeCount.compareAndSet(current, current + 1)) {
                    try {
                        Connection rawConn = createPhysicalConnection();
                        return wrap(rawConn);
                    } catch (SQLException | RuntimeException e) {
                        activeCount.decrementAndGet();
                        throw e;
                    }
                }
                continue;
            }

            try {
                item = pool.poll(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted while waiting for database connection from pool", e);
            }

            if (item == null) {
                throw new SQLException("Database connection pool exhausted. No connection available within 10 seconds.");
            }

            if (isConnectionAlive(item.connection, item.lastUsed)) {
                return wrap(item.connection);
            }
            try {
                item.connection.close();
            } catch (SQLException ignored) {
            }
            activeCount.decrementAndGet();
        }
    }

    /**
     * Gracefully closes all idle connections in the pool.
     */
    public static synchronized void shutdownPool() {
        PooledItem item;
        while ((item = pool.poll()) != null) {
            try {
                if (!item.connection.isClosed()) {
                    item.connection.close();
                }
            } catch (SQLException ignored) {
            } finally {
                activeCount.decrementAndGet();
            }
        }
    }

    /**
     * Returns a human-readable description of host, port, and database name.
     * Never contains username or password.
     */
    public static String describeTarget() {
        if (!initialized) {
            try {
                loadConfigIfNeeded();
            } catch (SQLException ignored) {
                // Return fallback description if config loading failed
                String h = System.getenv("SMARTSTAY_DB_HOST");
                String p = System.getenv("SMARTSTAY_DB_PORT");
                String n = System.getenv("SMARTSTAY_DB_NAME");
                return (h != null ? h : "localhost") + ":" + (p != null ? p : "1433") + "/" + (n != null ? n : "SmartStayDB");
            }
        }
        return dbHost + ":" + dbPort + "/" + dbName;
    }

    private static class PooledConnectionHandler implements InvocationHandler {
        private final Connection rawConn;
        private volatile boolean closed = false;

        PooledConnectionHandler(Connection rawConn) {
            this.rawConn = rawConn;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            int argCount = (args == null) ? 0 : args.length;

            if ("close".equals(name) && argCount == 0) {
                if (!closed) {
                    closed = true;
                    returnToPool(rawConn);
                }
                return null;
            }

            if ("isClosed".equals(name) && argCount == 0) {
                return closed || rawConn.isClosed();
            }

            if (closed) {
                throw new SQLException("Connection has already been closed.");
            }

            if ("equals".equals(name) && argCount == 1) {
                return proxy == args[0];
            }

            if ("hashCode".equals(name) && argCount == 0) {
                return System.identityHashCode(proxy);
            }

            if ("toString".equals(name) && argCount == 0) {
                return "PooledConnectionProxy[" + rawConn.toString() + "]";
            }

            if ("unwrap".equals(name) && argCount == 1) {
                Class<?> iface = (Class<?>) args[0];
                if (iface.isInstance(proxy)) {
                    return proxy;
                }
                if (iface.isInstance(rawConn)) {
                    return rawConn;
                }
                return rawConn.unwrap(iface);
            }

            if ("isWrapperFor".equals(name) && argCount == 1) {
                Class<?> iface = (Class<?>) args[0];
                if (iface.isInstance(proxy) || iface.isInstance(rawConn)) {
                    return true;
                }
                return rawConn.isWrapperFor(iface);
            }

            try {
                return method.invoke(rawConn, args);
            } catch (InvocationTargetException e) {
                Throwable target = e.getTargetException();
                if (target instanceof SQLException) {
                    throw (SQLException) target;
                }
                if (target instanceof RuntimeException) {
                    throw (RuntimeException) target;
                }
                if (target instanceof Error) {
                    throw (Error) target;
                }
                throw target;
            }
        }
    }
}
