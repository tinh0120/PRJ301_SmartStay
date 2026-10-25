package dao;

import model.AccessLogEntry;
import model.MotionLogEntry;
import util.DBUtil;
import util.PageResult.PageRequest;
import util.PageResult;
import util.SqlUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consolidated Data Access Object for device logs:
 * - Access logs (ACCESS_LOG table): door unlock attempts, credential validations, result codes.
 * - Motion logs (MOTION_LOG table): PIR and ultrasonic sensor readings, distance measurements.
 */
public class DeviceLogDao {

    private static final Map<String, String> ACCESS_SORT_WHITELIST;
    private static final Map<String, String> MOTION_SORT_WHITELIST;

    static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put("time", "a.attempted_at");
        aMap.put("device", "a.device_id");
        aMap.put("result", "a.result");
        ACCESS_SORT_WHITELIST = Collections.unmodifiableMap(aMap);

        Map<String, String> mMap = new HashMap<>();
        mMap.put("time", "m.detected_at");
        mMap.put("distance", "m.distance_cm");
        mMap.put("device", "m.device_id");
        MOTION_SORT_WHITELIST = Collections.unmodifiableMap(mMap);
    }

    public DeviceLogDao() {
    }

    // =========================================================================
    // ACCESS LOG METHODS (ACCESS_LOG)
    // =========================================================================

    public PageResult<AccessLogEntry> findAccessPage(PageRequest pr) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        String device = pr.getFilter("device");
        if (device != null && !device.trim().isEmpty()) {
            where.append("AND a.device_id = ? ");
            params.add(device.trim());
        }

        String result = pr.getFilter("result");
        if (result != null && !result.trim().isEmpty()) {
            where.append("AND a.result = ? ");
            params.add(result.trim());
        }

        String reason = pr.getFilter("reason");
        if (reason != null && !reason.trim().isEmpty()) {
            where.append("AND a.reason_code = ? ");
            params.add(reason.trim());
        }

        String fromStr = pr.getFilter("from");
        if (fromStr != null && !fromStr.trim().isEmpty()) {
            try {
                LocalDate fromDate = LocalDate.parse(fromStr.trim());
                where.append("AND a.attempted_at >= ? ");
                params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
            } catch (DateTimeParseException ignored) {
            }
        }

        String toStr = pr.getFilter("to");
        if (toStr != null && !toStr.trim().isEmpty()) {
            try {
                LocalDate toDate = LocalDate.parse(toStr.trim());
                where.append("AND a.attempted_at < ? ");
                params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
            } catch (DateTimeParseException ignored) {
            }
        }

        String keyword = pr.getKeyword();
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append("AND a.request_id LIKE ? ");
            params.add(SqlUtil.escapeLike(keyword.trim()));
        }

        String countSql = "SELECT COUNT(*) FROM ACCESS_LOG a " + where.toString();
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
            return new PageResult<>(Collections.<AccessLogEntry>emptyList(), 0, pr);
        }

        pr.clampPage(total);

        String sortKey = pr.getSort();
        String sortColumn = ACCESS_SORT_WHITELIST.get(sortKey);
        if (sortColumn == null) {
            sortColumn = "a.attempted_at";
        }
        String dir = "ASC".equalsIgnoreCase(pr.getDir()) ? "ASC" : "DESC";

        StringBuilder dataSql = new StringBuilder();
        dataSql.append("SELECT a.access_log_id, a.credential_id, a.device_id, a.request_id, ")
               .append("a.attempted_at, CONVERT(varchar, a.attempted_at, 120) AS attempted_at_str, ")
               .append("a.result, a.reason_code, ")
               .append("c.booking_id, b.booking_code, u.full_name AS guest_name, r.room_number ")
               .append("FROM ACCESS_LOG a ")
               .append("LEFT JOIN ACCESS_CREDENTIAL c ON a.credential_id = c.credential_id ")
               .append("LEFT JOIN BOOKING b ON c.booking_id = b.booking_id ")
               .append("LEFT JOIN APP_USER u ON b.guest_id = u.user_id ")
               .append("LEFT JOIN ROOM r ON b.room_id = r.room_id ")
               .append(where.toString())
               .append("ORDER BY ").append(sortColumn).append(" ").append(dir)
               .append(", a.access_log_id DESC ")
               .append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        List<AccessLogEntry> list = new ArrayList<>();
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
                    AccessLogEntry entry = new AccessLogEntry();
                    entry.setAccessLogId(rs.getLong("access_log_id"));
                    long credId = rs.getLong("credential_id");
                    if (!rs.wasNull()) {
                        entry.setCredentialId(credId);
                    }
                    entry.setDeviceId(rs.getString("device_id"));
                    entry.setRequestId(rs.getString("request_id"));
                    entry.setAttemptedAt(rs.getTimestamp("attempted_at"));
                    entry.setAttemptedAtFormatted(rs.getString("attempted_at_str"));
                    entry.setResult(rs.getString("result"));
                    entry.setReasonCode(rs.getString("reason_code"));

                    long bId = rs.getLong("booking_id");
                    if (!rs.wasNull()) {
                        entry.setBookingId(bId);
                    }
                    entry.setBookingCode(rs.getString("booking_code"));
                    entry.setGuestName(rs.getString("guest_name"));
                    entry.setRoomNumber(rs.getString("room_number"));

                    list.add(entry);
                }
            }
        }

        return new PageResult<>(list, total, pr);
    }

    public List<String> findDistinctAccessDevices() throws SQLException {
        String sql = "SELECT DISTINCT device_id FROM ACCESS_LOG WHERE device_id IS NOT NULL ORDER BY device_id ASC";
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

    public List<String> findDistinctAccessReasons() throws SQLException {
        String sql = "SELECT DISTINCT reason_code FROM ACCESS_LOG WHERE reason_code IS NOT NULL ORDER BY reason_code ASC";
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

    // =========================================================================
    // MOTION LOG METHODS (MOTION_LOG)
    // =========================================================================

    public PageResult<MotionLogEntry> findMotionPage(PageRequest pr) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        String device = pr.getFilter("device");
        if (device != null && !device.trim().isEmpty()) {
            where.append("AND m.device_id = ? ");
            params.add(device.trim());
        }

        String zone = pr.getFilter("zone");
        if (zone != null && !zone.trim().isEmpty()) {
            where.append("AND m.zone_id = ? ");
            params.add(zone.trim());
        }

        String fromStr = pr.getFilter("from");
        if (fromStr != null && !fromStr.trim().isEmpty()) {
            try {
                LocalDate fromDate = LocalDate.parse(fromStr.trim());
                where.append("AND m.detected_at >= ? ");
                params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
            } catch (DateTimeParseException ignored) {
            }
        }

        String toStr = pr.getFilter("to");
        if (toStr != null && !toStr.trim().isEmpty()) {
            try {
                LocalDate toDate = LocalDate.parse(toStr.trim());
                where.append("AND m.detected_at < ? ");
                params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
            } catch (DateTimeParseException ignored) {
            }
        }

        String keyword = pr.getKeyword();
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append("AND m.event_id LIKE ? ");
            params.add(SqlUtil.escapeLike(keyword.trim()));
        }

        String countSql = "SELECT COUNT(*) FROM MOTION_LOG m " + where.toString();
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
            return new PageResult<>(Collections.<MotionLogEntry>emptyList(), 0, pr);
        }

        pr.clampPage(total);

        String sortKey = pr.getSort();
        String sortColumn = MOTION_SORT_WHITELIST.get(sortKey);
        if (sortColumn == null) {
            sortColumn = "m.detected_at";
        }
        String dir = "ASC".equalsIgnoreCase(pr.getDir()) ? "ASC" : "DESC";

        StringBuilder dataSql = new StringBuilder();
        dataSql.append("SELECT m.motion_log_id, m.device_id, m.zone_id, m.event_id, ")
               .append("m.detected_at, CONVERT(varchar, m.detected_at, 120) AS detected_at_str, ")
               .append("m.distance_cm, m.snapshot_path, m.received_at ")
               .append("FROM MOTION_LOG m ")
               .append(where.toString())
               .append("ORDER BY ").append(sortColumn).append(" ").append(dir)
               .append(", m.motion_log_id DESC ")
               .append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        List<MotionLogEntry> list = new ArrayList<>();
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
                    MotionLogEntry entry = new MotionLogEntry();
                    entry.setMotionLogId(rs.getLong("motion_log_id"));
                    entry.setDeviceId(rs.getString("device_id"));
                    entry.setZoneId(rs.getString("zone_id"));
                    entry.setEventId(rs.getString("event_id"));
                    entry.setDetectedAt(rs.getTimestamp("detected_at"));
                    entry.setDetectedAtFormatted(rs.getString("detected_at_str"));
                    entry.setDistanceCm(rs.getBigDecimal("distance_cm"));
                    entry.setSnapshotPath(rs.getString("snapshot_path"));
                    entry.setReceivedAt(rs.getTimestamp("received_at"));
                    list.add(entry);
                }
            }
        }

        return new PageResult<>(list, total, pr);
    }

    public List<String> findDistinctMotionDevices() throws SQLException {
        String sql = "SELECT DISTINCT device_id FROM MOTION_LOG WHERE device_id IS NOT NULL ORDER BY device_id ASC";
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

    public List<String> findDistinctMotionZones() throws SQLException {
        String sql = "SELECT DISTINCT zone_id FROM MOTION_LOG WHERE zone_id IS NOT NULL ORDER BY zone_id ASC";
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

    public long insertMotion(MotionLogEntry entry) throws SQLException {
        String sql = "INSERT INTO MOTION_LOG (device_id, zone_id, event_id, detected_at, distance_cm, snapshot_path, received_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, SYSDATETIME())";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entry.getDeviceId());
            String zone = entry.getZoneId();
            if (zone == null || (!"MAIN_ENTRANCE".equals(zone) && !"LOBBY".equals(zone))) {
                zone = "LOBBY";
            }
            ps.setString(2, zone);
            ps.setString(3, entry.getEventId());
            ps.setTimestamp(4, entry.getDetectedAt() != null ? entry.getDetectedAt() : new Timestamp(System.currentTimeMillis()));
            if (entry.getDistanceCm() != null) {
                ps.setBigDecimal(5, entry.getDistanceCm());
            } else {
                ps.setNull(5, java.sql.Types.DECIMAL);
            }
            ps.setString(6, entry.getSnapshotPath());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0L;
    }

    public boolean existsMotionEventId(String eventId) throws SQLException {
        if (eventId == null || eventId.trim().isEmpty()) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM MOTION_LOG WHERE event_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
