package dao;

import util.PageResult.PageRequest;
import util.PageResult;
import model.User;
import util.DBUtil;
import util.SqlUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data access object for user queries and registrations.
 */
public class UserDao {

    public UserDao() {
    }

    public User findByEmail(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        String cleanEmail = email.trim().toLowerCase();
        User user = queryByEmailInternal(cleanEmail);
        if (user != null) {
            return user;
        }

        // Aliases fallback: supports convenient aliases & backward-compatible test accounts
        String alias = null;
        if ("admin@gmail.com".equals(cleanEmail)) {
            alias = "admin1@gmail.com";
        } else if ("staff@gmail.com".equals(cleanEmail)) {
            alias = "staff1@gmail.com";
        } else if ("owner1@gmail.com".equals(cleanEmail)) {
            alias = "owner@gmail.com";
        } else if ("owner@smartstay.local".equals(cleanEmail)) {
            alias = "owner@gmail.com";
        } else if ("admin@smartstay.local".equals(cleanEmail)) {
            alias = "admin1@gmail.com";
        } else if ("admin2@smartstay.local".equals(cleanEmail)) {
            alias = "admin2@gmail.com";
        } else if ("staff1@smartstay.local".equals(cleanEmail)) {
            alias = "staff1@gmail.com";
        } else if ("staff2@smartstay.local".equals(cleanEmail)) {
            alias = "staff2@gmail.com";
        }

        if (alias != null) {
            return queryByEmailInternal(alias);
        }
        return null;
    }

    private User queryByEmailInternal(String targetEmail) throws SQLException {
        String sql = "SELECT u.user_id, u.role_id, r.role_name, u.full_name, u.email, u.phone, u.password_hash, u.status "
                   + "FROM APP_USER u "
                   + "JOIN ROLE r ON u.role_id = r.role_id "
                   + "WHERE u.email = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public User findByPhone(String phone) throws SQLException {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT u.user_id, u.role_id, r.role_name, u.full_name, u.email, u.phone, u.password_hash, u.status "
                   + "FROM APP_USER u "
                   + "JOIN ROLE r ON u.role_id = r.role_id "
                   + "WHERE u.phone = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public User findById(long userId) throws SQLException {
        String sql = "SELECT u.user_id, u.role_id, r.role_name, u.full_name, u.email, u.phone, u.password_hash, u.status "
                   + "FROM APP_USER u "
                   + "JOIN ROLE r ON u.role_id = r.role_id "
                   + "WHERE u.user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public long insertGuest(User u) throws SQLException {
        String resolveRoleSql = "SELECT role_id FROM ROLE WHERE role_name = 'GUEST'";
        String insertSql = "INSERT INTO APP_USER (role_id, full_name, email, phone, password_hash, status) "
                         + "VALUES (?, ?, ?, ?, ?, 'ACTIVE')";

        try (Connection conn = DBUtil.getConnection()) {
            int guestRoleId = 0;
            try (PreparedStatement psRole = conn.prepareStatement(resolveRoleSql);
                 ResultSet rsRole = psRole.executeQuery()) {
                if (rsRole.next()) {
                    guestRoleId = rsRole.getInt(1);
                } else {
                    throw new SQLException("Role GUEST not found in database.");
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, guestRoleId);
                ps.setString(2, u.getFullName().trim());
                ps.setString(3, u.getEmail().trim().toLowerCase());
                ps.setString(4, u.getPhone().trim());
                ps.setString(5, u.getPasswordHash());
                ps.executeUpdate();

                try (ResultSet rsKeys = ps.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        return rsKeys.getLong(1);
                    } else {
                        throw new SQLException("Failed to retrieve generated user_id for guest.");
                    }
                }
            }
        }
    }

    public Set<String> findPermissionCodes(long userId) throws SQLException {
        String sql = "SELECT p.permission_code "
                   + "FROM APP_USER u "
                   + "JOIN ROLE_PERMISSION rp ON u.role_id = rp.role_id "
                   + "JOIN PERMISSION p ON rp.permission_id = p.permission_id "
                   + "WHERE u.user_id = ?";
        Set<String> perms = new HashSet<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    perms.add(rs.getString("permission_code"));
                }
            }
        }
        return perms;
    }

    public static final Map<String, String> ADMIN_SORT_WHITELIST;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("name", "u.full_name");
        m.put("email", "u.email");
        m.put("role", "r.role_name");
        m.put("status", "u.status");
        m.put("created", "u.created_at");
        ADMIN_SORT_WHITELIST = Collections.unmodifiableMap(m);
    }

    public PageResult<User> findAdminPage(PageRequest pr) throws SQLException {
        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        String role = pr.getFilter("role");
        if (role != null && !role.trim().isEmpty()) {
            where.append("AND r.role_name = ? ");
            params.add(role.trim());
        }

        String status = pr.getFilter("status");
        if (status != null && !status.trim().isEmpty()) {
            where.append("AND u.status = ? ");
            params.add(status.trim());
        }

        String keyword = pr.getKeyword();
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append("AND (u.full_name LIKE ? OR u.email LIKE ? OR u.phone LIKE ?) ");
            String kw = SqlUtil.escapeLike(keyword);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        String countSql = "SELECT COUNT(*) FROM APP_USER u JOIN ROLE r ON u.role_id = r.role_id " + where.toString();
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
            return new PageResult<>(Collections.<User>emptyList(), 0, pr);
        }

        pr.clampPage(total);

        String sortKey = pr.getSort();
        String sortCol = ADMIN_SORT_WHITELIST.get(sortKey);
        if (sortCol == null) {
            sortCol = "u.created_at";
            pr.setSort("created");
            pr.setDir("DESC");
        }
        String dir = "ASC".equalsIgnoreCase(pr.getDir()) ? "ASC" : "DESC";

        String dataSql = "SELECT u.user_id, u.role_id, r.role_name, u.full_name, u.email, u.phone, u.status, "
                       + "u.created_at, u.updated_at, u.position, u.employment_status, u.hire_date "
                       + "FROM APP_USER u "
                       + "JOIN ROLE r ON u.role_id = r.role_id "
                       + where.toString()
                       + "ORDER BY " + sortCol + " " + dir + " "
                       + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<User> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(dataSql)) {
            int idx = 1;
            for (Object p : params) {
                ps.setObject(idx++, p);
            }
            ps.setInt(idx++, pr.getOffset());
            ps.setInt(idx, pr.getSize());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowWithStaff(rs));
                }
            }
        }

        return new PageResult<>(list, total, pr);
    }

    public User findByIdWithStaff(long userId) throws SQLException {
        String sql = "SELECT u.user_id, u.role_id, r.role_name, u.full_name, u.email, u.phone, u.password_hash, u.status, "
                   + "u.created_at, u.updated_at, u.position, u.employment_status, u.hire_date "
                   + "FROM APP_USER u "
                   + "JOIN ROLE r ON u.role_id = r.role_id "
                   + "WHERE u.user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowWithStaff(rs);
                }
            }
        }
        return null;
    }

    public long insert(Connection conn, User u) throws SQLException {
        String sql = "INSERT INTO APP_USER (role_id, full_name, email, phone, password_hash, status, created_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, SYSDATETIME())";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, u.getRoleId());
            ps.setString(2, u.getFullName().trim());
            ps.setString(3, u.getEmail().trim().toLowerCase());
            ps.setString(4, u.getPhone().trim());
            ps.setString(5, u.getPasswordHash());
            ps.setString(6, u.getStatus());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to retrieve generated user ID.");
    }

    public void update(Connection conn, User u, boolean updatePassword) throws SQLException {
        String sql;
        if (updatePassword) {
            sql = "UPDATE APP_USER SET role_id = ?, full_name = ?, email = ?, phone = ?, "
                + "password_hash = ?, status = ?, updated_at = SYSDATETIME() WHERE user_id = ?";
        } else {
            sql = "UPDATE APP_USER SET role_id = ?, full_name = ?, email = ?, phone = ?, "
                + "status = ?, updated_at = SYSDATETIME() WHERE user_id = ?";
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, u.getRoleId());
            ps.setString(2, u.getFullName().trim());
            ps.setString(3, u.getEmail().trim().toLowerCase());
            ps.setString(4, u.getPhone().trim());
            if (updatePassword) {
                ps.setString(5, u.getPasswordHash());
                ps.setString(6, u.getStatus());
                ps.setLong(7, u.getUserId());
            } else {
                ps.setString(5, u.getStatus());
                ps.setLong(6, u.getUserId());
            }
            ps.executeUpdate();
        }
    }

    public void upsertStaffInfo(Connection conn, long userId, String position, String employmentStatus, java.sql.Date hireDate) throws SQLException {
        String updateSql = "UPDATE APP_USER SET position = ?, employment_status = ?, hire_date = ? WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, position != null ? position.trim() : null);
            ps.setString(2, employmentStatus != null ? employmentStatus.trim() : null);
            ps.setDate(3, hireDate);
            ps.setLong(4, userId);
            ps.executeUpdate();
        }
    }

    public void deleteStaffInfo(Connection conn, long userId) throws SQLException {
        String sql = "UPDATE APP_USER SET position = NULL, employment_status = NULL, hire_date = NULL WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, long userId) throws SQLException {
        String sql = "DELETE FROM APP_USER WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    public int countOwners(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM APP_USER u JOIN ROLE r ON u.role_id = r.role_id WHERE r.role_name = 'OWNER'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int getRoleIdByName(String roleName) throws SQLException {
        String sql = "SELECT role_id FROM ROLE WHERE role_name = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public String getRoleNameById(int roleId) throws SQLException {
        String sql = "SELECT role_name FROM ROLE WHERE role_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    public boolean existsEmailExcluding(String email, Long excludeUserId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM APP_USER WHERE email = ?");
        if (excludeUserId != null) {
            sql.append(" AND user_id != ?");
        }
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, email.trim().toLowerCase());
            if (excludeUserId != null) {
                ps.setLong(2, excludeUserId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean existsPhoneExcluding(String phone, Long excludeUserId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM APP_USER WHERE phone = ?");
        if (excludeUserId != null) {
            sql.append(" AND user_id != ?");
        }
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, phone.trim());
            if (excludeUserId != null) {
                ps.setLong(2, excludeUserId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getLong("user_id"));
        u.setRoleId(rs.getInt("role_id"));
        u.setRoleName(rs.getString("role_name"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setStatus(rs.getString("status"));
        return u;
    }

    private User mapRowWithStaff(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getLong("user_id"));
        u.setRoleId(rs.getInt("role_id"));
        u.setRoleName(rs.getString("role_name"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setStatus(rs.getString("status"));
        try {
            u.setPasswordHash(rs.getString("password_hash"));
        } catch (SQLException ignored) {}
        try {
            u.setCreatedAt(rs.getTimestamp("created_at"));
            u.setUpdatedAt(rs.getTimestamp("updated_at"));
        } catch (SQLException ignored) {}
        try {
            u.setPosition(rs.getString("position"));
            u.setEmploymentStatus(rs.getString("employment_status"));
            u.setHireDate(rs.getDate("hire_date"));
        } catch (SQLException ignored) {}
        return u;
    }

    public List<User> findAllActiveStaff() throws SQLException {
        String sql = "SELECT u.user_id, u.role_id, r.role_name, u.full_name, u.email, u.phone, u.password_hash, u.status "
                   + "FROM APP_USER u JOIN ROLE r ON u.role_id = r.role_id "
                   + "WHERE r.role_name = 'STAFF' AND u.status = 'ACTIVE' "
                   + "ORDER BY u.full_name ASC";
        List<User> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }
}
