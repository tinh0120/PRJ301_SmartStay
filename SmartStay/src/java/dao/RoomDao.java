package dao;

import model.Room;
import model.RoomSlot;
import util.DBUtil;
import util.PageResult.PageRequest;
import util.PageResult;
import util.SqlUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unified Data Access Object for Room aggregates:
 * - ROOM table: catalog search, room detail, admin CRUD, operational status.
 * - ROOM_TIME_SLOT table: slot scheduling, pricing definitions, hold lookups.
 */
public class RoomDao {

    private static final Map<String, String> SORT_WHITELIST;
    private static final Map<String, String> ADMIN_SORT_WHITELIST;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("room", "r.room_number");
        map.put("number", "r.room_number");
        map.put("name", "r.display_name");
        map.put("display_name", "r.display_name");
        map.put("floor", "r.floor_number");
        map.put("capacity", "r.capacity");
        map.put("price", "s.min_price");
        SORT_WHITELIST = Collections.unmodifiableMap(map);

        Map<String, String> adminMap = new HashMap<>(map);
        adminMap.put("status", "r.status");
        ADMIN_SORT_WHITELIST = Collections.unmodifiableMap(adminMap);
    }

    public RoomDao() {
    }

    // =========================================================================
    // ROOM CATALOG & ADMIN METHODS
    // =========================================================================

    public PageResult<Room> findPage(PageRequest pr, LocalDate date) throws SQLException {
        StringBuilder where = new StringBuilder();
        where.append("FROM ROOM r ")
             .append("JOIN ( ")
             .append("    SELECT room_id, MIN(list_price) AS min_price ")
             .append("    FROM ROOM_TIME_SLOT ")
             .append("    WHERE is_active = 1 ")
             .append("    GROUP BY room_id ")
             .append(") s ON r.room_id = s.room_id ")
             .append("WHERE r.is_active = 1 ");

        List<Object> params = new ArrayList<>();

        String minCapStr = pr.getFilter("minCapacity");
        if (minCapStr != null && !minCapStr.trim().isEmpty()) {
            try {
                int cap = Integer.parseInt(minCapStr.trim());
                if (cap >= 1) {
                    where.append("AND r.capacity >= ? ");
                    params.add(cap);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        String floorStr = pr.getFilter("floor");
        if (floorStr != null && !floorStr.trim().isEmpty()) {
            try {
                int fl = Integer.parseInt(floorStr.trim());
                if (fl >= 0) {
                    where.append("AND r.floor_number = ? ");
                    params.add(fl);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        String minPriceStr = pr.getFilter("minPrice");
        if (minPriceStr != null && !minPriceStr.trim().isEmpty()) {
            try {
                long minP = Long.parseLong(minPriceStr.trim());
                if (minP >= 0) {
                    where.append("AND s.min_price >= ? ");
                    params.add(minP);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        String maxPriceStr = pr.getFilter("maxPrice");
        if (maxPriceStr != null && !maxPriceStr.trim().isEmpty()) {
            try {
                long maxP = Long.parseLong(maxPriceStr.trim());
                if (maxP >= 0) {
                    where.append("AND s.min_price <= ? ");
                    params.add(maxP);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        String keyword = pr.getKeyword();
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append("AND (r.room_number LIKE ? OR r.display_name LIKE ? OR r.description LIKE ? OR r.amenities LIKE ?) ");
            String kw = SqlUtil.escapeLike(keyword);
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        String countSql = "SELECT COUNT(*) " + where.toString();
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
            return new PageResult<>(Collections.<Room>emptyList(), 0, pr);
        }

        pr.clampPage(total);

        String sortKey = pr.getSort();
        String sortCol = SORT_WHITELIST.get(sortKey);
        if (sortCol == null) {
            sortCol = "r.room_number";
        }
        String dir = "DESC".equalsIgnoreCase(pr.getDir()) ? "DESC" : "ASC";

        StringBuilder dataSql = new StringBuilder();
        dataSql.append("SELECT r.room_id, r.room_number, r.display_name, r.floor_number, ")
               .append("r.location_description, r.capacity, r.description, r.amenities, ")
               .append("r.image_url, r.status, r.is_active, s.min_price ")
               .append(where.toString())
               .append("ORDER BY ").append(sortCol).append(" ").append(dir);

        // Tiebreaker without repeating column name in ORDER BY
        if (!"r.room_number".equals(sortCol)) {
            dataSql.append(", r.room_number ASC ");
        } else {
            dataSql.append(", r.room_id ASC ");
        }
        dataSql.append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        List<Room> rooms = new ArrayList<>();
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
                    Room room = mapRow(rs);
                    rooms.add(room);
                }
            }
        }

        return new PageResult<>(rooms, total, pr);
    }

    public Room findById(long roomId) throws SQLException {
        String sql = "SELECT room_id, room_number, display_name, floor_number, location_description, "
                   + "capacity, description, amenities, image_url, status, is_active "
                   + "FROM ROOM WHERE room_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public PageResult<Room> findAdminPage(PageRequest pr) throws SQLException {
        StringBuilder where = new StringBuilder();
        where.append("FROM ROOM r ")
             .append("LEFT JOIN ( ")
             .append("    SELECT room_id, MIN(list_price) AS min_price ")
             .append("    FROM ROOM_TIME_SLOT ")
             .append("    WHERE is_active = 1 ")
             .append("    GROUP BY room_id ")
             .append(") s ON r.room_id = s.room_id ")
             .append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        String status = pr.getFilter("status");
        if (status != null && !status.trim().isEmpty()) {
            where.append("AND r.status = ? ");
            params.add(status.trim());
        }

        String floorStr = pr.getFilter("floor");
        if (floorStr != null && !floorStr.trim().isEmpty()) {
            try {
                int fl = Integer.parseInt(floorStr.trim());
                if (fl >= 0) {
                    where.append("AND r.floor_number = ? ");
                    params.add(fl);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        String activeStr = pr.getFilter("active");
        if ("1".equals(activeStr)) {
            where.append("AND r.is_active = 1 ");
        } else if ("0".equals(activeStr)) {
            where.append("AND r.is_active = 0 ");
        }

        String keyword = pr.getKeyword();
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append("AND (r.room_number LIKE ? OR r.display_name LIKE ? OR r.description LIKE ?) ");
            String kw = SqlUtil.escapeLike(keyword);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        String countSql = "SELECT COUNT(*) " + where.toString();
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
            return new PageResult<>(Collections.<Room>emptyList(), 0, pr);
        }

        pr.clampPage(total);

        String sortKey = pr.getSort();
        String sortCol = ADMIN_SORT_WHITELIST.get(sortKey);
        if (sortCol == null) {
            sortCol = "r.room_number";
        }
        String dir = "DESC".equalsIgnoreCase(pr.getDir()) ? "DESC" : "ASC";

        StringBuilder dataSql = new StringBuilder();
        dataSql.append("SELECT r.room_id, r.room_number, r.display_name, r.floor_number, ")
               .append("r.location_description, r.capacity, r.description, r.amenities, ")
               .append("r.image_url, r.status, r.is_active, s.min_price ")
               .append(where.toString())
               .append("ORDER BY ").append(sortCol).append(" ").append(dir);

        if (!"r.room_number".equals(sortCol)) {
            dataSql.append(", r.room_number ASC ");
        } else {
            dataSql.append(", r.room_id ASC ");
        }
        dataSql.append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        List<Room> rooms = new ArrayList<>();
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
                    Room room = mapRow(rs);
                    rooms.add(room);
                }
            }
        }

        return new PageResult<>(rooms, total, pr);
    }

    public boolean existsRoomNumber(Connection conn, String roomNumber, Long excludeRoomId)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ROOM WHERE room_number = ?");
        if (excludeRoomId != null && excludeRoomId > 0) {
            sql.append(" AND room_id <> ?");
        }
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, roomNumber.trim());
            if (excludeRoomId != null && excludeRoomId > 0) {
                ps.setLong(2, excludeRoomId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public long insert(Connection conn, Room room) throws SQLException {
        String sql = "INSERT INTO ROOM (room_number, display_name, floor_number, location_description, "
                + "capacity, description, amenities, image_url, status, is_active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'AVAILABLE', ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getDisplayName());
            ps.setInt(3, room.getFloorNumber());
            ps.setString(4, room.getLocationDescription());
            ps.setInt(5, room.getCapacity());
            ps.setString(6, room.getDescription());
            ps.setString(7, room.getAmenities());
            ps.setString(8, room.getImageUrl());
            ps.setBoolean(9, room.isActive());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to obtain generated room_id.");
    }

    public void update(Connection conn, Room room) throws SQLException {
        String sql = "UPDATE ROOM SET room_number = ?, display_name = ?, floor_number = ?, location_description = ?, "
                + "capacity = ?, description = ?, amenities = ?, image_url = ?, status = ?, is_active = ?, updated_at = SYSDATETIME() "
                + "WHERE room_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getDisplayName());
            ps.setInt(3, room.getFloorNumber());
            ps.setString(4, room.getLocationDescription());
            ps.setInt(5, room.getCapacity());
            ps.setString(6, room.getDescription());
            ps.setString(7, room.getAmenities());
            ps.setString(8, room.getImageUrl());
            ps.setString(9, (room.getStatus() != null && !room.getStatus().trim().isEmpty()) ? room.getStatus() : "AVAILABLE");
            ps.setBoolean(10, room.isActive());
            ps.setLong(11, room.getRoomId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection conn, long roomId, String newStatus) throws SQLException {
        String sql = "UPDATE ROOM SET status = ?, updated_at = SYSDATETIME() WHERE room_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setLong(2, roomId);
            ps.executeUpdate();
        }
    }

    public boolean hasActiveCheckin(Connection conn, long roomId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM BOOKING WHERE room_id = ? AND status = 'CHECKED_IN'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void delete(Connection conn, long roomId) throws SQLException {
        String delSlots = "DELETE FROM ROOM_TIME_SLOT WHERE room_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(delSlots)) {
            ps.setLong(1, roomId);
            ps.executeUpdate();
        }

        String delRoom = "DELETE FROM ROOM WHERE room_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(delRoom)) {
            ps.setLong(1, roomId);
            ps.executeUpdate();
        }
    }

    public List<Room> findAllActive() throws SQLException {
        String sql = "SELECT room_id, room_number, display_name, floor_number, location_description, "
                   + "capacity, description, amenities, image_url, status, is_active "
                   + "FROM ROOM WHERE is_active = 1 ORDER BY room_number ASC";
        List<Room> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Room> findAllShowcase() throws SQLException {
        String sql = "SELECT r.room_id, r.room_number, r.display_name, r.floor_number, r.location_description, r.capacity, "
                   + "r.description, r.amenities, r.image_url, r.status, r.is_active, "
                   + "MIN(CASE WHEN s.slot_code IN ('S1','S2','S3') THEN s.list_price END) AS min_price, "
                   + "MAX(CASE WHEN s.slot_code = 'S4' THEN s.list_price END) AS overnight_price "
                   + "FROM ROOM r "
                   + "LEFT JOIN ROOM_TIME_SLOT s ON r.room_id = s.room_id AND s.is_active = 1 "
                   + "WHERE r.is_active = 1 "
                   + "GROUP BY r.room_id, r.room_number, r.display_name, r.floor_number, r.location_description, r.capacity, "
                   + "r.description, r.amenities, r.image_url, r.status, r.is_active "
                   + "ORDER BY r.room_number ASC";
        List<Room> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Room r = mapRow(rs);
                try {
                    BigDecimal op = rs.getBigDecimal("overnight_price");
                    if (op != null) {
                        r.setOvernightPrice(op.longValue());
                    }
                } catch (SQLException ignored) {
                }
                list.add(r);
            }
        }
        return list;
    }

    public List<Room> findByStatus(String status) throws SQLException {
        String sql = "SELECT room_id, room_number, display_name, floor_number, location_description, "
                   + "capacity, description, amenities, image_url, status, is_active "
                   + "FROM ROOM WHERE is_active = 1 AND status = ? ORDER BY room_number ASC";
        List<Room> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        Room r = new Room();
        r.setRoomId(rs.getLong("room_id"));
        r.setRoomNumber(rs.getString("room_number"));
        try {
            r.setDisplayName(rs.getString("display_name"));
        } catch (SQLException ignored) {
        }
        if (r.getDisplayName() == null || r.getDisplayName().trim().isEmpty()) {
            r.setDisplayName(util.ElFunctions.getDisplayName(r.getRoomNumber()));
        }
        r.setFloorNumber(rs.getInt("floor_number"));
        try {
            r.setLocationDescription(rs.getString("location_description"));
        } catch (SQLException ignored) {
        }
        r.setCapacity(rs.getInt("capacity"));
        r.setDescription(rs.getString("description"));
        r.setAmenities(rs.getString("amenities"));
        r.setImageUrl(rs.getString("image_url"));
        r.setStatus(rs.getString("status"));
        r.setActive(rs.getBoolean("is_active"));

        try {
            BigDecimal p = rs.getBigDecimal("min_price");
            if (p != null) {
                r.setMinPrice(p.longValue());
            }
        } catch (SQLException ignored) {
            // column min_price might not be present in all queries
        }
        return r;
    }

    // =========================================================================
    // ROOM TIME SLOT METHODS (merged from RoomSlotDao)
    // =========================================================================

    /**
     * Loads active time slot definitions for the given room IDs, grouped by room ID.
     */
    public Map<Long, List<RoomSlot>> findSlotsByRoomIds(Collection<Long> roomIds) throws SQLException {
        if (roomIds == null || roomIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = SqlUtil.placeholders(roomIds.size());
        String sql = "SELECT room_slot_id, room_id, slot_code, slot_name, start_time, end_time, "
                   + "crosses_midnight, list_price, sort_order, is_active, status "
                   + "FROM ROOM_TIME_SLOT "
                   + "WHERE is_active = 1 AND room_id IN (" + placeholders + ") "
                   + "ORDER BY room_id ASC, sort_order ASC";

        Map<Long, List<RoomSlot>> map = new HashMap<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int pIndex = 1;
            for (Long rid : roomIds) {
                ps.setLong(pIndex++, rid);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long rid = rs.getLong("room_id");
                    List<RoomSlot> list = map.get(rid);
                    if (list == null) {
                        list = new ArrayList<>();
                        map.put(rid, list);
                    }
                    RoomSlot slot = mapSlotRow(rs);
                    list.add(slot);
                }
            }
        }
        return map;
    }

    public Map<Long, List<RoomSlot>> findByRoomIds(Collection<Long> roomIds) throws SQLException {
        return findSlotsByRoomIds(roomIds);
    }

    /**
     * Loads ALL time slot definitions (both active and inactive) for multiple rooms.
     */
    public Map<Long, List<RoomSlot>> findAllSlotsByRoomIds(Collection<Long> roomIds) throws SQLException {
        if (roomIds == null || roomIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = SqlUtil.placeholders(roomIds.size());
        String sql = "SELECT room_slot_id, room_id, slot_code, slot_name, start_time, end_time, "
                   + "crosses_midnight, list_price, sort_order, is_active, status "
                   + "FROM ROOM_TIME_SLOT "
                   + "WHERE room_id IN (" + placeholders + ") "
                   + "ORDER BY room_id ASC, sort_order ASC";

        Map<Long, List<RoomSlot>> map = new HashMap<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int pIndex = 1;
            for (Long rid : roomIds) {
                ps.setLong(pIndex++, rid);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long rid = rs.getLong("room_id");
                    List<RoomSlot> list = map.get(rid);
                    if (list == null) {
                        list = new ArrayList<>();
                        map.put(rid, list);
                    }
                    RoomSlot slot = mapSlotRow(rs);
                    list.add(slot);
                }
            }
        }
        return map;
    }

    public Map<Long, List<RoomSlot>> findAllByRoomIds(Collection<Long> roomIds) throws SQLException {
        return findAllSlotsByRoomIds(roomIds);
    }

    /**
     * Loads ALL time slot definitions (active and inactive) for a specific room using the provided connection,
     * ordered by sort_order. Used within the transactional booking creation flow.
     */
    public List<RoomSlot> findSlotsByRoomId(Connection conn, long roomId) throws SQLException {
        String sql = "SELECT room_slot_id, room_id, slot_code, slot_name, start_time, end_time, "
                   + "crosses_midnight, list_price, sort_order, is_active, status "
                   + "FROM ROOM_TIME_SLOT "
                   + "WHERE room_id = ? "
                   + "ORDER BY sort_order ASC";
        List<RoomSlot> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSlotRow(rs));
                }
            }
        }
        return list;
    }

    public List<RoomSlot> findByRoomId(Connection conn, long roomId) throws SQLException {
        return findSlotsByRoomId(conn, roomId);
    }

    public List<RoomSlot> findSlotsByRoomId(long roomId) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            return findSlotsByRoomId(conn, roomId);
        }
    }

    public void updateRoomSlotPrices(long roomId, Map<String, BigDecimal> slotPrices, Long updatedBy) throws SQLException {
        if (slotPrices == null || slotPrices.isEmpty()) {
            return;
        }
        String sql = "UPDATE ROOM_TIME_SLOT SET list_price = ?, updated_by = ?, updated_at = SYSDATETIME() "
                   + "WHERE room_id = ? AND slot_code = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, BigDecimal> entry : slotPrices.entrySet()) {
                if (entry.getValue() != null && entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                    ps.setBigDecimal(1, entry.getValue());
                    if (updatedBy != null) {
                        ps.setLong(2, updatedBy);
                    } else {
                        ps.setNull(2, java.sql.Types.BIGINT);
                    }
                    ps.setLong(3, roomId);
                    ps.setString(4, entry.getKey());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    /**
     * Retrieves the set of room_slot_id currently HELD on the specified date for the given rooms.
     */
    public Set<Long> findHeldSlotIds(Collection<Long> roomIds, LocalDate date) throws SQLException {
        if (roomIds == null || roomIds.isEmpty() || date == null) {
            return Collections.emptySet();
        }

        String placeholders = SqlUtil.placeholders(roomIds.size());
        String sql = "SELECT DISTINCT room_slot_id "
                   + "FROM BOOKING_SLOT "
                   + "WHERE hold_status = 'HELD' AND slot_date = ? "
                   + "AND room_id IN (" + placeholders + ")";

        Set<Long> set = new HashSet<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            int pIndex = 2;
            for (Long rid : roomIds) {
                ps.setLong(pIndex++, rid);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    set.add(rs.getLong("room_slot_id"));
                }
            }
        }
        return set;
    }

    public Map<LocalDate, Set<Long>> findHeldSlotIdsInRange(long roomId, LocalDate startDate, LocalDate endDate) throws SQLException {
        if (startDate == null || endDate == null) {
            return Collections.emptyMap();
        }
        String sql = "SELECT slot_date, room_slot_id "
                   + "FROM BOOKING_SLOT "
                   + "WHERE hold_status = 'HELD' AND room_id = ? "
                   + "AND slot_date >= ? AND slot_date <= ?";
        Map<LocalDate, Set<Long>> map = new HashMap<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date sd = rs.getDate("slot_date");
                    if (sd != null) {
                        LocalDate d = sd.toLocalDate();
                        long sid = rs.getLong("room_slot_id");
                        Set<Long> set = map.get(d);
                        if (set == null) {
                            set = new HashSet<>();
                            map.put(d, set);
                        }
                        set.add(sid);
                    }
                }
            }
        }
        return map;
    }

    private RoomSlot mapSlotRow(ResultSet rs) throws SQLException {
        RoomSlot s = new RoomSlot();
        s.setRoomSlotId(rs.getLong("room_slot_id"));
        s.setRoomId(rs.getLong("room_id"));
        s.setSlotCode(rs.getString("slot_code"));
        s.setSlotName(rs.getString("slot_name"));

        String startStr = rs.getString("start_time");
        if (startStr != null && !startStr.trim().isEmpty()) {
            try {
                s.setStartTime(LocalTime.parse(startStr.trim()));
            } catch (Exception ex) {
                Time t = rs.getTime("start_time");
                if (t != null) s.setStartTime(t.toLocalTime());
            }
        }

        String endStr = rs.getString("end_time");
        if (endStr != null && !endStr.trim().isEmpty()) {
            try {
                s.setEndTime(LocalTime.parse(endStr.trim()));
            } catch (Exception ex) {
                Time t = rs.getTime("end_time");
                if (t != null) s.setEndTime(t.toLocalTime());
            }
        }

        s.setCrossesMidnight(rs.getBoolean("crosses_midnight"));

        BigDecimal priceBd = rs.getBigDecimal("list_price");
        s.setListPrice(priceBd != null ? priceBd.longValue() : 0L);

        s.setSortOrder(rs.getInt("sort_order"));
        s.setActive(rs.getBoolean("is_active"));
        try {
            String st = rs.getString("status");
            s.setStatus(st != null ? st : "AVAILABLE");
        } catch (Exception ignored) {
            s.setStatus("AVAILABLE");
        }
        return s;
    }
}
