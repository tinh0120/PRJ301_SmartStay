package dao;

import model.Assignment;
import util.DBUtil;
import util.PageResult.PageRequest;
import util.PageResult;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DAO for STAFF_ASSIGNMENT table and associated relations.
 */
public class AssignmentDao {

    private static final Set<String> ALLOWED_SORT_COLUMNS = new HashSet<>(Arrays.asList(
            "shift_start", "created_at", "status", "room_number", "staff_name"
    ));

    public PageResult<Assignment> findPageAdmin(String search, String status, Long staffId, Long roomId,
                                                LocalDate shiftDate, String sortCol, String sortDir,
                                                int page, int pageSize) throws SQLException {

        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;

        String safeSort = "shift_start";
        if (sortCol != null && ALLOWED_SORT_COLUMNS.contains(sortCol.toLowerCase())) {
            safeSort = sortCol.toLowerCase();
        }
        String safeOrder = "DESC".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
        if (sortCol == null) {
            safeOrder = "DESC";
        }

        String sortClause;
        switch (safeSort) {
            case "created_at":
                sortClause = "sa.created_at " + safeOrder;
                break;
            case "status":
                sortClause = "sa.status " + safeOrder;
                break;
            case "room_number":
                sortClause = "r.room_number " + safeOrder;
                break;
            case "staff_name":
                sortClause = "u.full_name " + safeOrder;
                break;
            case "shift_start":
            default:
                sortClause = "sa.shift_start " + safeOrder;
                break;
        }

        StringBuilder whereSql = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            whereSql.append(" AND (u.full_name LIKE ? OR u.email LIKE ? OR r.room_number LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (status != null && !status.trim().isEmpty()) {
            whereSql.append(" AND sa.status = ? ");
            params.add(status.trim().toUpperCase());
        }

        if (staffId != null && staffId > 0) {
            whereSql.append(" AND sa.staff_id = ? ");
            params.add(staffId);
        }

        if (roomId != null && roomId > 0) {
            whereSql.append(" AND sa.room_id = ? ");
            params.add(roomId);
        }

        if (shiftDate != null) {
            whereSql.append(" AND CAST(sa.shift_start AS DATE) = ? ");
            params.add(Date.valueOf(shiftDate));
        }

        String countSql = "SELECT COUNT(*) FROM STAFF_ASSIGNMENT sa "
                + "JOIN APP_USER u ON sa.staff_id = u.user_id "
                + "JOIN ROOM r ON sa.room_id = r.room_id "
                + whereSql;

        int totalCount = 0;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement psCount = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) {
                psCount.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = psCount.executeQuery()) {
                if (rs.next()) {
                    totalCount = rs.getInt(1);
                }
            }
        }

        if (totalCount == 0) {
            PageRequest pr = new PageRequest();
            pr.setPage(page);
            pr.setSize(pageSize);
            return new PageResult<>(new ArrayList<>(), 0, pr);
        }

        int offset = (page - 1) * pageSize;
        String querySql = "SELECT sa.assignment_id, sa.staff_id, sa.room_id, sa.assigned_by, "
                + "sa.shift_start, sa.shift_end, sa.status, sa.note, sa.created_at, sa.updated_at, "
                + "sa.started_at, sa.completed_at, "
                + "u.full_name AS staff_name, u.email AS staff_email, "
                + "r.room_number, r.status AS room_status, "
                + "ab.full_name AS assigned_by_name, "
                + "ac.credential_id, ac.status AS credential_status, ac.valid_from, ac.valid_until "
                + "FROM STAFF_ASSIGNMENT sa "
                + "JOIN APP_USER u ON sa.staff_id = u.user_id "
                + "JOIN ROOM r ON sa.room_id = r.room_id "
                + "JOIN APP_USER ab ON sa.assigned_by = ab.user_id "
                + "LEFT JOIN ACCESS_CREDENTIAL ac ON sa.assignment_id = ac.staff_assignment_id AND ac.status IN ('PENDING', 'ACTIVE') "
                + whereSql
                + " ORDER BY " + sortClause
                + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<Assignment> items = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(querySql)) {
            int pIdx = 1;
            for (Object param : params) {
                ps.setObject(pIdx++, param);
            }
            ps.setInt(pIdx++, offset);
            ps.setInt(pIdx, pageSize);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }

        PageRequest pr = new PageRequest();
        pr.setPage(page);
        pr.setSize(pageSize);
        return new PageResult<>(items, totalCount, pr);
    }

    public PageResult<Assignment> findPageByStaff(long staffId, int page, int pageSize) throws SQLException {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;

        String countSql = "SELECT COUNT(*) FROM STAFF_ASSIGNMENT WHERE staff_id = ?";
        int totalCount = 0;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement psCount = conn.prepareStatement(countSql)) {
            psCount.setLong(1, staffId);
            try (ResultSet rs = psCount.executeQuery()) {
                if (rs.next()) {
                    totalCount = rs.getInt(1);
                }
            }
        }

        if (totalCount == 0) {
            PageRequest pr = new PageRequest();
            pr.setPage(page);
            pr.setSize(pageSize);
            return new PageResult<>(new ArrayList<>(), 0, pr);
        }

        int offset = (page - 1) * pageSize;
        String querySql = "SELECT sa.assignment_id, sa.staff_id, sa.room_id, sa.assigned_by, "
                + "sa.shift_start, sa.shift_end, sa.status, sa.note, sa.created_at, sa.updated_at, "
                + "sa.started_at, sa.completed_at, "
                + "u.full_name AS staff_name, u.email AS staff_email, "
                + "r.room_number, r.status AS room_status, "
                + "ab.full_name AS assigned_by_name, "
                + "ac.credential_id, ac.status AS credential_status, ac.valid_from, ac.valid_until "
                + "FROM STAFF_ASSIGNMENT sa "
                + "JOIN APP_USER u ON sa.staff_id = u.user_id "
                + "JOIN ROOM r ON sa.room_id = r.room_id "
                + "JOIN APP_USER ab ON sa.assigned_by = ab.user_id "
                + "LEFT JOIN ACCESS_CREDENTIAL ac ON sa.assignment_id = ac.staff_assignment_id AND ac.status IN ('PENDING', 'ACTIVE') "
                + "WHERE sa.staff_id = ? "
                + "ORDER BY sa.shift_start DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<Assignment> items = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(querySql)) {
            ps.setLong(1, staffId);
            ps.setInt(2, offset);
            ps.setInt(3, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }

        PageRequest pr = new PageRequest();
        pr.setPage(page);
        pr.setSize(pageSize);
        return new PageResult<>(items, totalCount, pr);
    }

    public Assignment findById(long assignmentId) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            return findById(conn, assignmentId);
        }
    }

    public Assignment findById(Connection conn, long assignmentId) throws SQLException {
        String querySql = "SELECT sa.assignment_id, sa.staff_id, sa.room_id, sa.assigned_by, "
                + "sa.shift_start, sa.shift_end, sa.status, sa.note, sa.created_at, sa.updated_at, "
                + "sa.started_at, sa.completed_at, "
                + "u.full_name AS staff_name, u.email AS staff_email, "
                + "r.room_number, r.status AS room_status, "
                + "ab.full_name AS assigned_by_name, "
                + "ac.credential_id, ac.status AS credential_status, ac.valid_from, ac.valid_until "
                + "FROM STAFF_ASSIGNMENT sa "
                + "JOIN APP_USER u ON sa.staff_id = u.user_id "
                + "JOIN ROOM r ON sa.room_id = r.room_id "
                + "JOIN APP_USER ab ON sa.assigned_by = ab.user_id "
                + "LEFT JOIN ACCESS_CREDENTIAL ac ON sa.assignment_id = ac.staff_assignment_id AND ac.status IN ('PENDING', 'ACTIVE') "
                + "WHERE sa.assignment_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(querySql)) {
            ps.setLong(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public boolean hasOverlap(Connection conn, long staffId, LocalDateTime start, LocalDateTime end, Long excludeAssignmentId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM STAFF_ASSIGNMENT "
                + "WHERE staff_id = ? AND status = 'ASSIGNED' "
                + "AND shift_start < ? AND shift_end > ?"
        );
        if (excludeAssignmentId != null && excludeAssignmentId > 0) {
            sql.append(" AND assignment_id <> ?");
        }

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setLong(1, staffId);
            ps.setTimestamp(2, Timestamp.valueOf(end));
            ps.setTimestamp(3, Timestamp.valueOf(start));
            if (excludeAssignmentId != null && excludeAssignmentId > 0) {
                ps.setLong(4, excludeAssignmentId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public long create(Connection conn, long staffId, long roomId, long assignedBy,
                       LocalDateTime shiftStart, LocalDateTime shiftEnd, String note) throws SQLException {
        String sql = "INSERT INTO STAFF_ASSIGNMENT (staff_id, room_id, assigned_by, shift_start, shift_end, status, note) "
                + "VALUES (?, ?, ?, ?, ?, 'ASSIGNED', ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, staffId);
            ps.setLong(2, roomId);
            ps.setLong(3, assignedBy);
            ps.setTimestamp(4, Timestamp.valueOf(shiftStart));
            ps.setTimestamp(5, Timestamp.valueOf(shiftEnd));
            ps.setString(6, note);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to create staff assignment.");
    }

    public void update(Connection conn, long assignmentId, long staffId, LocalDateTime shiftStart,
                       LocalDateTime shiftEnd, String note) throws SQLException {
        String sql = "UPDATE STAFF_ASSIGNMENT SET staff_id = ?, shift_start = ?, shift_end = ?, note = ?, "
                + "updated_at = SYSDATETIME() WHERE assignment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, staffId);
            ps.setTimestamp(2, Timestamp.valueOf(shiftStart));
            ps.setTimestamp(3, Timestamp.valueOf(shiftEnd));
            ps.setString(4, note);
            ps.setLong(5, assignmentId);
            ps.executeUpdate();
        }
    }

    public void cancel(Connection conn, long assignmentId) throws SQLException {
        String sql = "UPDATE STAFF_ASSIGNMENT SET status = 'CANCELLED', updated_at = SYSDATETIME() WHERE assignment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, assignmentId);
            ps.executeUpdate();
        }
    }

    public void complete(Connection conn, long assignmentId) throws SQLException {
        String sql = "UPDATE STAFF_ASSIGNMENT SET status = 'COMPLETED', updated_at = SYSDATETIME() WHERE assignment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, assignmentId);
            ps.executeUpdate();
        }
    }

    public void startAssignment(Connection conn, long assignmentId, LocalDateTime startedAt) throws SQLException {
        String sql = "UPDATE STAFF_ASSIGNMENT SET started_at = ?, updated_at = SYSDATETIME() WHERE assignment_id = ? AND status = 'ASSIGNED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(startedAt));
            ps.setLong(2, assignmentId);
            ps.executeUpdate();
        }
    }

    public void completeAssignment(Connection conn, long assignmentId, LocalDateTime completedAt) throws SQLException {
        String sql = "UPDATE STAFF_ASSIGNMENT SET status = 'COMPLETED', completed_at = ?, updated_at = SYSDATETIME() WHERE assignment_id = ? AND status = 'ASSIGNED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(completedAt));
            ps.setLong(2, assignmentId);
            ps.executeUpdate();
        }
    }

    private Assignment mapRow(ResultSet rs) throws SQLException {
        Assignment a = new Assignment();
        a.setAssignmentId(rs.getLong("assignment_id"));
        a.setStaffId(rs.getLong("staff_id"));
        a.setStaffName(rs.getString("staff_name"));
        a.setStaffEmail(rs.getString("staff_email"));
        a.setRoomId(rs.getLong("room_id"));
        a.setRoomNumber(rs.getString("room_number"));
        a.setRoomStatus(rs.getString("room_status"));
        a.setAssignedBy(rs.getLong("assigned_by"));
        a.setAssignedByName(rs.getString("assigned_by_name"));

        Timestamp ss = rs.getTimestamp("shift_start");
        if (ss != null) a.setShiftStart(ss.toLocalDateTime());
        Timestamp se = rs.getTimestamp("shift_end");
        if (se != null) a.setShiftEnd(se.toLocalDateTime());

        a.setStatus(rs.getString("status"));
        a.setNote(rs.getString("note"));

        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) a.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) a.setUpdatedAt(ua.toLocalDateTime());

        Timestamp sa = rs.getTimestamp("started_at");
        if (sa != null) a.setStartedAt(sa.toLocalDateTime());
        Timestamp comp = rs.getTimestamp("completed_at");
        if (comp != null) a.setCompletedAt(comp.toLocalDateTime());

        long credId = rs.getLong("credential_id");
        if (!rs.wasNull()) {
            a.setCredentialId(credId);
            a.setCredentialStatus(rs.getString("credential_status"));
            Timestamp vf = rs.getTimestamp("valid_from");
            if (vf != null) a.setCredentialValidFrom(vf.toLocalDateTime());
            Timestamp vu = rs.getTimestamp("valid_until");
            if (vu != null) a.setCredentialValidUntil(vu.toLocalDateTime());
        }

        return a;
    }
}
