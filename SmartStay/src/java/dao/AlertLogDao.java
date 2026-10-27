package dao;

import model.AlertEntry;
import util.DBUtil;
import util.PageResult.PageRequest;
import util.PageResult;
import util.SqlUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data access object for the ALERT_LOG table.
 */
public class AlertLogDao {

    private static final Map<String, String> SORT_WHITELIST;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("time", "a.created_at");
        map.put("severity", "CASE a.severity WHEN 'CRITICAL' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 END");
        map.put("status", "a.status");
        map.put("type", "a.alert_type");
        SORT_WHITELIST = Collections.unmodifiableMap(map);
    }

    /** R10: joins that give each alert a human source (device name, room, booking). */
    private static final String SOURCE_JOINS =
            "LEFT JOIN DEVICE dv ON a.device_id = dv.device_id "
            + "LEFT JOIN ROOM r ON a.room_id = r.room_id "
            + "LEFT JOIN BOOKING b ON a.booking_id = b.booking_id ";

    public AlertLogDao() {
    }

    /**
     * R10 tab counts (NEW / ACKNOWLEDGED / RESOLVED / ALL), independent of search and filters.
     */
    public Map<String, Integer> getStatusCounts() throws SQLException {
        Map<String, Integer> m = new HashMap<>();
        m.put("NEW", 0);
        m.put("ACKNOWLEDGED", 0);
        m.put("RESOLVED", 0);
        int all = 0;
        String sql = "SELECT status, COUNT(*) AS cnt FROM ALERT_LOG GROUP BY status";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String st = rs.getString("status");
                int cnt = rs.getInt("cnt");
                if (st != null) {
                    m.put(st.toUpperCase(), cnt);
                }
                all += cnt;
            }
        }
        m.put("ALL", all);
        return m;
    }

    /**
     * Counts active unresolved alerts in 'NEW' status for navbar indicator badge.
     */
    public int countNewAlerts() {
        String sql = "SELECT COUNT(*) FROM ALERT_LOG WHERE status = 'NEW'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    public AlertEntry findById(long alertId) throws SQLException {
        String sql = "SELECT a.alert_id, a.alert_type, a.severity, a.status, a.message, "
                   + "a.device_id, a.room_id, a.zone_id, a.booking_id, a.motion_log_id, "
                   + "a.threshold_id, a.snapshot_path, "
                   + "a.acknowledged_by, u1.full_name AS ack_user_name, a.acknowledged_at, "
                   + "CONVERT(varchar, a.acknowledged_at, 120) AS ack_at_str, "
                   + "a.resolved_by, u2.full_name AS res_user_name, a.resolved_at, "
                   + "CONVERT(varchar, a.resolved_at, 120) AS res_at_str, "
                   + "a.resolution_note, a.created_at, "
                   + "CONVERT(varchar, a.created_at, 120) AS created_at_str, "
                   + "dv.device_name, r.room_number, b.booking_code "
                   + "FROM ALERT_LOG a "
                   + "LEFT JOIN APP_USER u1 ON a.acknowledged_by = u1.user_id "
                   + "LEFT JOIN APP_USER u2 ON a.resolved_by = u2.user_id "
                   + SOURCE_JOINS
                   + "WHERE a.alert_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, alertId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public PageResult<AlertEntry> findPage(PageRequest pr) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        String status = pr.getFilter("status");
        if (status != null && !status.trim().isEmpty()) {
            where.append("AND a.status = ? ");
            params.add(status.trim());
        }

        String severity = pr.getFilter("severity");
        if (severity != null && !severity.trim().isEmpty()) {
            where.append("AND a.severity = ? ");
            params.add(severity.trim());
        }

        String type = pr.getFilter("type");
        if (type != null && !type.trim().isEmpty()) {
            where.append("AND a.alert_type = ? ");
            params.add(type.trim());
        }

        String device = pr.getFilter("device");
        if (device != null && !device.trim().isEmpty()) {
            where.append("AND a.device_id = ? ");
            params.add(device.trim());
        }

        String fromStr = pr.getFilter("from");
        if (fromStr != null && !fromStr.trim().isEmpty()) {
            try {
                LocalDate fromDate = LocalDate.parse(fromStr.trim());
                where.append("AND a.created_at >= ? ");
                params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
            } catch (DateTimeParseException ignored) {
            }
        }

        String toStr = pr.getFilter("to");
        if (toStr != null && !toStr.trim().isEmpty()) {
            try {
                LocalDate toDate = LocalDate.parse(toStr.trim());
                where.append("AND a.created_at < ? ");
                params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
            } catch (DateTimeParseException ignored) {
            }
        }

        String keyword = pr.getKeyword();
        if (keyword != null && !keyword.trim().isEmpty()) {
            // R10: search message, type, device, room or booking
            where.append("AND (a.message LIKE ? OR a.alert_type LIKE ? OR a.device_id LIKE ? OR dv.device_name LIKE ? "
                    + "OR r.room_number LIKE ? OR b.booking_code LIKE ?) ");
            String kw = SqlUtil.escapeLike(keyword.trim());
            for (int i = 0; i < 6; i++) {
                params.add(kw);
            }
        }

        String countSql = "SELECT COUNT(*) FROM ALERT_LOG a " + SOURCE_JOINS + where.toString();
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
            return new PageResult<>(Collections.<AlertEntry>emptyList(), 0, pr);
        }

        pr.clampPage(total);

        String sortKey = pr.getSort();
        String sortColumn = SORT_WHITELIST.get(sortKey);
        if (sortColumn == null) {
            sortColumn = "a.created_at";
        }
        String dir = "ASC".equalsIgnoreCase(pr.getDir()) ? "ASC" : "DESC";
        String orderBy = sortColumn + " " + dir + ", a.alert_id DESC ";
        if ("smart".equals(sortKey)) {
            // R10 default: most severe first, then newest
            orderBy = SORT_WHITELIST.get("severity") + " DESC, a.created_at DESC, a.alert_id DESC ";
        }

        StringBuilder dataSql = new StringBuilder();
        dataSql.append("SELECT a.alert_id, a.alert_type, a.severity, a.status, a.message, ")
               .append("a.device_id, a.room_id, a.zone_id, a.booking_id, a.motion_log_id, ")
               .append("a.threshold_id, a.snapshot_path, ")
               .append("a.acknowledged_by, u1.full_name AS ack_user_name, a.acknowledged_at, ")
               .append("CONVERT(varchar, a.acknowledged_at, 120) AS ack_at_str, ")
               .append("a.resolved_by, u2.full_name AS res_user_name, a.resolved_at, ")
               .append("CONVERT(varchar, a.resolved_at, 120) AS res_at_str, ")
               .append("a.resolution_note, a.created_at, ")
               .append("CONVERT(varchar, a.created_at, 120) AS created_at_str, ")
               .append("dv.device_name, r.room_number, b.booking_code ")
               .append("FROM ALERT_LOG a ")
               .append("LEFT JOIN APP_USER u1 ON a.acknowledged_by = u1.user_id ")
               .append("LEFT JOIN APP_USER u2 ON a.resolved_by = u2.user_id ")
               .append(SOURCE_JOINS)
               .append(where.toString())
               .append("ORDER BY ").append(orderBy)
               .append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        List<AlertEntry> list = new ArrayList<>();
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

    /**
     * Atomically transitions alert from NEW to ACKNOWLEDGED.
     * Returns 1 if transitioned, 0 if alert is not in NEW state or not found.
     */
    public int acknowledge(long alertId, long userId) throws SQLException {
        String sql = "UPDATE ALERT_LOG SET status = 'ACKNOWLEDGED', acknowledged_by = ?, "
                   + "acknowledged_at = SYSDATETIME() "
                   + "WHERE alert_id = ? AND status = 'NEW'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, alertId);
            return ps.executeUpdate();
        }
    }

    /**
     * Atomically transitions alert from ACKNOWLEDGED to RESOLVED with resolution note.
     * Returns 1 if transitioned, 0 if alert is not in ACKNOWLEDGED state or not found.
     */
    public int resolve(long alertId, long userId, String note) throws SQLException {
        String sql = "UPDATE ALERT_LOG SET status = 'RESOLVED', resolved_by = ?, "
                   + "resolved_at = SYSDATETIME(), resolution_note = ? "
                   + "WHERE alert_id = ? AND status = 'ACKNOWLEDGED'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, note != null ? note.trim() : "");
            ps.setLong(3, alertId);
            return ps.executeUpdate();
        }
    }

    public List<String> findDistinctTypes() throws SQLException {
        String sql = "SELECT DISTINCT alert_type FROM ALERT_LOG WHERE alert_type IS NOT NULL ORDER BY alert_type ASC";
        List<String> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString(1));
            }
        }
        return list;
    }

    private AlertEntry mapRow(ResultSet rs) throws SQLException {
        AlertEntry a = new AlertEntry();
        a.setAlertId(rs.getLong("alert_id"));
        a.setAlertType(rs.getString("alert_type"));
        a.setSeverity(rs.getString("severity"));
        a.setStatus(rs.getString("status"));
        a.setMessage(rs.getString("message"));
        a.setDeviceId(rs.getString("device_id"));

        long roomId = rs.getLong("room_id");
        if (!rs.wasNull()) a.setRoomId(roomId);

        a.setZoneId(rs.getString("zone_id"));

        long bookingId = rs.getLong("booking_id");
        if (!rs.wasNull()) a.setBookingId(bookingId);

        long motionId = rs.getLong("motion_log_id");
        if (!rs.wasNull()) a.setMotionLogId(motionId);

        long threshId = rs.getLong("threshold_id");
        if (!rs.wasNull()) a.setThresholdId(threshId);

        a.setSnapshotPath(rs.getString("snapshot_path"));

        long ackBy = rs.getLong("acknowledged_by");
        if (!rs.wasNull()) a.setAcknowledgedBy(ackBy);
        a.setAcknowledgedByName(rs.getString("ack_user_name"));
        a.setAcknowledgedAt(rs.getTimestamp("acknowledged_at"));
        a.setAcknowledgedAtFormatted(rs.getString("ack_at_str"));

        long resBy = rs.getLong("resolved_by");
        if (!rs.wasNull()) a.setResolvedBy(resBy);
        a.setResolvedByName(rs.getString("res_user_name"));
        a.setResolvedAt(rs.getTimestamp("resolved_at"));
        a.setResolvedAtFormatted(rs.getString("res_at_str"));

        a.setResolutionNote(rs.getString("resolution_note"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        a.setCreatedAtFormatted(rs.getString("created_at_str"));
        a.setDeviceName(rs.getString("device_name"));
        a.setRoomNumber(rs.getString("room_number"));
        a.setBookingCode(rs.getString("booking_code"));
        return a;
    }
}
