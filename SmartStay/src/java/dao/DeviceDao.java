package dao;

import model.AlertThreshold;
import model.Device;
import util.DBUtil;
import util.PageResult.PageRequest;
import util.PageResult;
import util.SqlUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified Data Access Object for hardware devices and alert thresholds:
 * - DEVICE table: IoT hardware inventory, heartbeats, status transitions, API key hashes.
 * - ALERT_THRESHOLD table: telemetry threshold rules, metric evaluation, severity configs.
 */
public class DeviceDao {

    private static final Map<String, String> DEVICE_SORT_WHITELIST;
    private static final Map<String, String> THRESHOLD_SORT_WHITELIST;

    static {
        Map<String, String> dMap = new HashMap<>();
        dMap.put("id", "d.device_id");
        dMap.put("name", "d.device_name");
        dMap.put("type", "d.device_type");
        dMap.put("status", "d.status");
        dMap.put("seen", "d.last_seen_at");
        DEVICE_SORT_WHITELIST = Collections.unmodifiableMap(dMap);

        Map<String, String> tMap = new HashMap<>();
        tMap.put("metric", "t.metric_type");
        tMap.put("value", "t.threshold_value");
        tMap.put("type", "t.alert_type");
        tMap.put("severity", "t.severity");
        tMap.put("updated", "t.updated_at");
        THRESHOLD_SORT_WHITELIST = Collections.unmodifiableMap(tMap);
    }

    public DeviceDao() {
    }

    // =========================================================================
    // DEVICE MANAGEMENT METHODS
    // =========================================================================

    public PageResult<Device> findPage(PageRequest pr) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        String type = pr.getFilter("type");
        if (type != null && !type.trim().isEmpty()) {
            where.append("AND d.device_type = ? ");
            params.add(type.trim());
        }

        String status = pr.getFilter("status");
        if (status != null && !status.trim().isEmpty()) {
            String st = status.trim().toUpperCase();
            if ("ONLINE".equals(st)) {
                where.append("AND d.status != 'FAULT' AND d.last_seen_at IS NOT NULL AND d.last_seen_at >= DATEADD(SECOND, -?, SYSDATETIME()) ");
                params.add(util.AppConstants.DEVICE_OFFLINE_AFTER_SECONDS);
            } else if ("OFFLINE".equals(st)) {
                where.append("AND d.status != 'FAULT' AND (d.last_seen_at IS NULL OR d.last_seen_at < DATEADD(SECOND, -?, SYSDATETIME())) ");
                params.add(util.AppConstants.DEVICE_OFFLINE_AFTER_SECONDS);
            } else if ("FAULT".equals(st)) {
                where.append("AND d.status = 'FAULT' ");
            } else {
                where.append("AND d.status = ? ");
                params.add(status.trim());
            }
        }

        String active = pr.getFilter("active");
        if ("1".equals(active)) {
            where.append("AND d.is_active = 1 ");
        } else if ("0".equals(active)) {
            where.append("AND d.is_active = 0 ");
        }

        String keyword = pr.getKeyword();
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append("AND (d.device_id LIKE ? OR d.device_name LIKE ? OR d.zone_id LIKE ?) ");
            String kw = SqlUtil.escapeLike(keyword);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        String countSql = "SELECT COUNT(*) FROM DEVICE d LEFT JOIN ROOM r ON d.room_id = r.room_id " + where.toString();
        int total = 0;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }
        }

        if (total == 0) {
            return new PageResult<>(Collections.<Device>emptyList(), 0, pr);
        }

        pr.clampPage(total);

        String sortKey = pr.getSort();
        String sortColumn = DEVICE_SORT_WHITELIST.get(sortKey);
        if (sortColumn == null) {
            sortColumn = "d.device_id";
        }
        String dir = "DESC".equalsIgnoreCase(pr.getDir()) ? "DESC" : "ASC";

        StringBuilder dataSql = new StringBuilder();
        dataSql.append("SELECT d.device_id, d.device_name, d.device_type, d.zone_id, d.room_id, ")
               .append("r.room_number, d.status, d.api_key_hash, d.is_active, d.last_seen_at, ")
               .append("d.created_at, d.updated_at ")
               .append("FROM DEVICE d ")
               .append("LEFT JOIN ROOM r ON d.room_id = r.room_id ")
               .append(where.toString())
               .append("ORDER BY ").append(sortColumn).append(" ").append(dir);

        if (!"d.device_id".equals(sortColumn)) {
            dataSql.append(", d.device_id ASC ");
        }
        dataSql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        List<Device> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(dataSql.toString())) {
            int pIndex = 1;
            for (Object param : params) {
                ps.setObject(pIndex++, param);
            }
            ps.setInt(pIndex++, pr.getOffset());
            ps.setInt(pIndex++, pr.getSize());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return new PageResult<>(list, total, pr);
    }

    public Device findById(String deviceId) throws SQLException {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT d.device_id, d.device_name, d.device_type, d.zone_id, d.room_id, "
                   + "r.room_number, d.status, d.api_key_hash, d.is_active, d.last_seen_at, "
                   + "d.created_at, d.updated_at "
                   + "FROM DEVICE d "
                   + "LEFT JOIN ROOM r ON d.room_id = r.room_id "
                   + "WHERE d.device_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deviceId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Device> findAll() throws SQLException {
        String sql = "SELECT d.device_id, d.device_name, d.device_type, d.zone_id, d.room_id, "
                   + "r.room_number, d.status, d.api_key_hash, d.is_active, d.last_seen_at, "
                   + "d.created_at, d.updated_at "
                   + "FROM DEVICE d "
                   + "LEFT JOIN ROOM r ON d.room_id = r.room_id "
                   + "ORDER BY d.device_id ASC";
        List<Device> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public void insert(Device device) throws SQLException {
        String sql = "INSERT INTO DEVICE (device_id, device_name, device_type, zone_id, room_id, status, is_active, created_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, SYSDATETIME())";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, device.getDeviceId());
            ps.setString(2, device.getDeviceName());
            ps.setString(3, device.getDeviceType());
            ps.setString(4, device.getZoneId());
            if (device.getRoomId() != null && device.getRoomId() > 0) {
                ps.setLong(5, device.getRoomId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            ps.setString(6, device.getStatus() != null ? device.getStatus() : "OFFLINE");
            ps.setBoolean(7, device.isActive());
            ps.executeUpdate();
        }
    }

    public void update(Device device) throws SQLException {
        String sql = "UPDATE DEVICE SET device_name = ?, device_type = ?, zone_id = ?, room_id = ?, "
                   + "status = ?, is_active = ?, updated_at = SYSDATETIME() "
                   + "WHERE device_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, device.getDeviceName());
            ps.setString(2, device.getDeviceType());
            ps.setString(3, device.getZoneId());
            if (device.getRoomId() != null && device.getRoomId() > 0) {
                ps.setLong(4, device.getRoomId());
            } else {
                ps.setNull(4, Types.BIGINT);
            }
            ps.setString(5, device.getStatus());
            ps.setBoolean(6, device.isActive());
            ps.setString(7, device.getDeviceId());
            ps.executeUpdate();
        }
    }

    public void delete(String deviceId) throws SQLException {
        String sql = "DELETE FROM DEVICE WHERE device_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deviceId.trim());
            ps.executeUpdate();
        }
    }

    public void updateApiKeyHash(String deviceId, String hash) throws SQLException {
        String sql = "UPDATE DEVICE SET api_key_hash = ?, updated_at = SYSDATETIME() WHERE device_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, deviceId.trim());
            ps.executeUpdate();
        }
    }

    private Device mapRow(ResultSet rs) throws SQLException {
        Device d = new Device();
        d.setDeviceId(rs.getString("device_id"));
        d.setDeviceName(rs.getString("device_name"));
        d.setDeviceType(rs.getString("device_type"));
        d.setZoneId(rs.getString("zone_id"));
        long roomId = rs.getLong("room_id");
        if (!rs.wasNull()) {
            d.setRoomId(roomId);
        }
        d.setRoomNumber(rs.getString("room_number"));
        String rawStatus = rs.getString("status");
        java.sql.Timestamp lastSeen = rs.getTimestamp("last_seen_at");
        if (!"FAULT".equalsIgnoreCase(rawStatus)) {
            if (lastSeen == null || (System.currentTimeMillis() - lastSeen.getTime()) > (util.AppConstants.DEVICE_OFFLINE_AFTER_SECONDS * 1000L)) {
                rawStatus = "OFFLINE";
            } else {
                rawStatus = "ONLINE";
            }
        }
        d.setStatus(rawStatus);
        d.setApiKeyHash(rs.getString("api_key_hash"));
        d.setActive(rs.getBoolean("is_active"));
        d.setLastSeenAt(lastSeen);
        d.setCreatedAt(rs.getTimestamp("created_at"));
        d.setUpdatedAt(rs.getTimestamp("updated_at"));
        return d;
    }

    // =========================================================================
    // ALERT THRESHOLD METHODS (merged from ThresholdDao)
    // =========================================================================

    public PageResult<AlertThreshold> findThresholdPage(PageRequest pr) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        String metric = pr.getFilter("metric");
        if (metric != null && !metric.trim().isEmpty()) {
            where.append("AND t.metric_type = ? ");
            params.add(metric.trim());
        }

        String device = pr.getFilter("device");
        if (device != null && !device.trim().isEmpty()) {
            if ("__GLOBAL__".equalsIgnoreCase(device.trim())) {
                where.append("AND t.device_id IS NULL ");
            } else {
                where.append("AND t.device_id = ? ");
                params.add(device.trim());
            }
        }

        String enabled = pr.getFilter("enabled");
        if ("1".equals(enabled)) {
            where.append("AND t.enabled = 1 ");
        } else if ("0".equals(enabled)) {
            where.append("AND t.enabled = 0 ");
        }

        String keyword = pr.getKeyword();
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append("AND (t.alert_type LIKE ? OR t.description LIKE ?) ");
            String kw = SqlUtil.escapeLike(keyword);
            params.add(kw);
            params.add(kw);
        }

        String countSql = "SELECT COUNT(*) FROM ALERT_THRESHOLD t LEFT JOIN DEVICE d ON t.device_id = d.device_id " + where.toString();
        int total = 0;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }
        }

        if (total == 0) {
            return new PageResult<>(Collections.<AlertThreshold>emptyList(), 0, pr);
        }

        pr.clampPage(total);

        String sortKey = pr.getSort();
        String sortColumn = THRESHOLD_SORT_WHITELIST.get(sortKey);
        if (sortColumn == null) {
            sortColumn = "t.updated_at";
        }
        String dir = "ASC".equalsIgnoreCase(pr.getDir()) ? "ASC" : "DESC";

        StringBuilder dataSql = new StringBuilder();
        dataSql.append("SELECT t.threshold_id, t.device_id, d.device_name, t.metric_type, t.operator, ")
               .append("t.threshold_value, t.alert_type, t.severity, t.enabled, t.description, ")
               .append("t.updated_by, t.updated_at ")
               .append("FROM ALERT_THRESHOLD t ")
               .append("LEFT JOIN DEVICE d ON t.device_id = d.device_id ")
               .append(where.toString())
               .append("ORDER BY ").append(sortColumn).append(" ").append(dir)
               .append(", t.threshold_id DESC ")
               .append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        List<AlertThreshold> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(dataSql.toString())) {
            int pIndex = 1;
            for (Object param : params) {
                ps.setObject(pIndex++, param);
            }
            ps.setInt(pIndex++, pr.getOffset());
            ps.setInt(pIndex++, pr.getSize());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapThresholdRow(rs));
                }
            }
        }

        return new PageResult<>(list, total, pr);
    }

    public AlertThreshold findThresholdById(long thresholdId) throws SQLException {
        String sql = "SELECT t.threshold_id, t.device_id, d.device_name, t.metric_type, t.operator, "
                   + "t.threshold_value, t.alert_type, t.severity, t.enabled, t.description, "
                   + "t.updated_by, t.updated_at "
                   + "FROM ALERT_THRESHOLD t "
                   + "LEFT JOIN DEVICE d ON t.device_id = d.device_id "
                   + "WHERE t.threshold_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, thresholdId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapThresholdRow(rs);
                }
            }
        }
        return null;
    }

    public long insertThreshold(AlertThreshold t) throws SQLException {
        String sql = "INSERT INTO ALERT_THRESHOLD (device_id, metric_type, operator, threshold_value, "
                   + "alert_type, severity, enabled, description, updated_by, updated_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATETIME())";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (t.getDeviceId() != null && !t.getDeviceId().trim().isEmpty()) {
                ps.setString(1, t.getDeviceId().trim());
            } else {
                ps.setNull(1, Types.VARCHAR);
            }
            ps.setString(2, t.getMetricType());
            ps.setString(3, t.getOperator());
            ps.setBigDecimal(4, t.getThresholdValue());
            ps.setString(5, t.getAlertType());
            ps.setString(6, t.getSeverity());
            ps.setBoolean(7, t.isEnabled());
            ps.setString(8, t.getDescription());
            if (t.getUpdatedBy() != null && t.getUpdatedBy() > 0) {
                ps.setLong(9, t.getUpdatedBy());
            } else {
                ps.setNull(9, Types.BIGINT);
            }
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to obtain generated threshold_id.");
    }

    public void updateThreshold(AlertThreshold t) throws SQLException {
        String sql = "UPDATE ALERT_THRESHOLD SET device_id = ?, metric_type = ?, operator = ?, "
                   + "threshold_value = ?, alert_type = ?, severity = ?, enabled = ?, description = ?, "
                   + "updated_by = ?, updated_at = SYSDATETIME() "
                   + "WHERE threshold_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (t.getDeviceId() != null && !t.getDeviceId().trim().isEmpty()) {
                ps.setString(1, t.getDeviceId().trim());
            } else {
                ps.setNull(1, Types.VARCHAR);
            }
            ps.setString(2, t.getMetricType());
            ps.setString(3, t.getOperator());
            ps.setBigDecimal(4, t.getThresholdValue());
            ps.setString(5, t.getAlertType());
            ps.setString(6, t.getSeverity());
            ps.setBoolean(7, t.isEnabled());
            ps.setString(8, t.getDescription());
            if (t.getUpdatedBy() != null && t.getUpdatedBy() > 0) {
                ps.setLong(9, t.getUpdatedBy());
            } else {
                ps.setNull(9, Types.BIGINT);
            }
            ps.setLong(10, t.getThresholdId());
            ps.executeUpdate();
        }
    }

    public void deleteThreshold(long thresholdId) throws SQLException {
        String sql = "DELETE FROM ALERT_THRESHOLD WHERE threshold_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, thresholdId);
            ps.executeUpdate();
        }
    }

    public List<AlertThreshold> findMatchingEnabledThresholds(String deviceId, String metricType) throws SQLException {
        String sql = "SELECT threshold_id, device_id, metric_type, operator, threshold_value, "
                   + "alert_type, severity, description FROM ALERT_THRESHOLD "
                   + "WHERE enabled = 1 AND metric_type = ? AND (device_id IS NULL OR device_id = ?) "
                   + "ORDER BY threshold_id ASC";
        List<AlertThreshold> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, metricType);
            ps.setString(2, deviceId != null ? deviceId : "");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AlertThreshold t = new AlertThreshold();
                    t.setThresholdId(rs.getLong("threshold_id"));
                    t.setDeviceId(rs.getString("device_id"));
                    t.setMetricType(rs.getString("metric_type"));
                    t.setOperator(rs.getString("operator"));
                    t.setThresholdValue(rs.getBigDecimal("threshold_value"));
                    t.setAlertType(rs.getString("alert_type"));
                    t.setSeverity(rs.getString("severity"));
                    t.setDescription(rs.getString("description"));
                    list.add(t);
                }
            }
        }
        return list;
    }

    private AlertThreshold mapThresholdRow(ResultSet rs) throws SQLException {
        AlertThreshold t = new AlertThreshold();
        t.setThresholdId(rs.getLong("threshold_id"));
        t.setDeviceId(rs.getString("device_id"));
        t.setDeviceName(rs.getString("device_name"));
        t.setMetricType(rs.getString("metric_type"));
        t.setOperator(rs.getString("operator"));
        t.setThresholdValue(rs.getBigDecimal("threshold_value"));
        t.setAlertType(rs.getString("alert_type"));
        t.setSeverity(rs.getString("severity"));
        t.setEnabled(rs.getBoolean("enabled"));
        t.setDescription(rs.getString("description"));
        long uid = rs.getLong("updated_by");
        if (!rs.wasNull()) {
            t.setUpdatedBy(uid);
        }
        t.setUpdatedAt(rs.getTimestamp("updated_at"));
        return t;
    }
}
