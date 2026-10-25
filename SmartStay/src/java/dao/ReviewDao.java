package dao;

import model.Review;
import util.DBUtil;
import util.PageResult.PageRequest;
import util.PageResult;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data access object for the REVIEW table.
 */
public class ReviewDao {

    private static final Map<String, String> SORT_WHITELIST;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("created", "r.created_at");
        map.put("rating", "r.rating");
        map.put("room", "rm.room_number");
        map.put("status", "r.status");
        SORT_WHITELIST = Collections.unmodifiableMap(map);
    }

    public ReviewDao() {
    }

    /**
     * Retrieves the latest visible reviews for a room up to the specified limit.
     */
    public List<Review> findVisibleByRoom(long roomId, int limit) throws SQLException {
        int max = (limit > 0) ? limit : 10;
        String sql = "SELECT TOP (?) r.review_id, r.booking_id, r.guest_id, r.room_id, "
                   + "r.rating, r.comment, r.status, r.created_at, u.full_name "
                   + "FROM REVIEW r "
                   + "JOIN APP_USER u ON r.guest_id = u.user_id "
                   + "WHERE r.room_id = ? AND r.status = 'VISIBLE' "
                   + "ORDER BY r.created_at DESC, r.review_id DESC";

        List<Review> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, max);
            ps.setLong(2, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review rev = new Review();
                    rev.setReviewId(rs.getLong("review_id"));
                    rev.setBookingId(rs.getLong("booking_id"));
                    rev.setGuestId(rs.getLong("guest_id"));
                    rev.setRoomId(rs.getLong("room_id"));
                    rev.setRating(rs.getInt("rating"));
                    rev.setComment(rs.getString("comment"));
                    rev.setStatus(rs.getString("status"));
                    rev.setCreatedAt(rs.getTimestamp("created_at"));
                    rev.setGuestName(Review.maskGuestName(rs.getString("full_name")));
                    list.add(rev);
                }
            }
        }
        return list;
    }

    /**
     * Calculates the average rating and total count of visible reviews for a room.
     *
     * @return double array where index 0 is average rating, index 1 is count
     */
    public double[] averageRating(long roomId) throws SQLException {
        String sql = "SELECT AVG(CAST(rating AS DECIMAL(4,2))) AS avg_rating, COUNT(*) AS review_count "
                   + "FROM REVIEW "
                   + "WHERE room_id = ? AND status = 'VISIBLE'";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("review_count");
                    if (count > 0) {
                        BigDecimal avg = rs.getBigDecimal("avg_rating");
                        return new double[]{avg != null ? avg.doubleValue() : 0.0, (double) count};
                    }
                }
            }
        }
        return new double[]{0.0, 0.0};
    }

    /**
     * Retrieves the existing review for a specific booking, if any.
     */
    public Review findByBookingId(long bookingId) throws SQLException {
        String sql = "SELECT r.review_id, r.booking_id, r.guest_id, r.room_id, "
                   + "r.rating, r.comment, r.status, r.created_at, u.full_name "
                   + "FROM REVIEW r "
                   + "JOIN APP_USER u ON r.guest_id = u.user_id "
                   + "WHERE r.booking_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Review rev = new Review();
                    rev.setReviewId(rs.getLong("review_id"));
                    rev.setBookingId(rs.getLong("booking_id"));
                    rev.setGuestId(rs.getLong("guest_id"));
                    rev.setRoomId(rs.getLong("room_id"));
                    rev.setRating(rs.getInt("rating"));
                    rev.setComment(rs.getString("comment"));
                    rev.setStatus(rs.getString("status"));
                    rev.setCreatedAt(rs.getTimestamp("created_at"));
                    rev.setGuestName(Review.maskGuestName(rs.getString("full_name")));
                    return rev;
                }
            }
        }
        return null;
    }

    /**
     * Inserts a new review for a completed reservation.
     */
    public long insertReview(Connection conn, long bookingId, long guestId, long roomId,
                             int rating, String comment) throws SQLException {
        String sql = "INSERT INTO REVIEW (booking_id, guest_id, room_id, rating, comment, status) "
                   + "VALUES (?, ?, ?, ?, ?, 'VISIBLE')";

        try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, bookingId);
            ps.setLong(2, guestId);
            ps.setLong(3, roomId);
            ps.setInt(4, rating);
            if (comment != null && !comment.trim().isEmpty()) {
                ps.setString(5, comment.trim());
            } else {
                ps.setNull(5, java.sql.Types.NVARCHAR);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    /**
     * Retrieves paginated reviews with filters for admin moderation.
     */
    public PageResult<Review> findPage(PageRequest pr) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        // 1. rating
        String ratingStr = pr.getFilter("rating");
        if (ratingStr != null && !ratingStr.trim().isEmpty()) {
            try {
                int rating = Integer.parseInt(ratingStr.trim());
                where.append("AND r.rating = ? ");
                params.add(rating);
            } catch (NumberFormatException ignored) {
            }
        }

        // 2. status
        String status = pr.getFilter("status");
        if (status != null && !status.trim().isEmpty()) {
            where.append("AND r.status = ? ");
            params.add(status.trim().toUpperCase());
        }

        // 3. roomId
        String roomIdStr = pr.getFilter("roomId");
        if (roomIdStr != null && !roomIdStr.trim().isEmpty()) {
            try {
                long roomId = Long.parseLong(roomIdStr.trim());
                where.append("AND r.room_id = ? ");
                params.add(roomId);
            } catch (NumberFormatException ignored) {
            }
        }

        // 4. keyword on comment OR guest name OR guest email
        String kw = pr.getKeyword();
        if (kw != null && !kw.trim().isEmpty()) {
            where.append("AND (r.comment LIKE ? OR u.full_name LIKE ? OR u.email LIKE ?) ");
            String wildcard = "%" + kw.trim() + "%";
            params.add(wildcard);
            params.add(wildcard);
            params.add(wildcard);
        }

        // Count query
        String countSql = "SELECT COUNT(*) FROM REVIEW r "
                        + "JOIN APP_USER u ON r.guest_id = u.user_id "
                        + "JOIN ROOM rm ON r.room_id = rm.room_id "
                        + where.toString();

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
            return new PageResult<>(Collections.<Review>emptyList(), 0, pr);
        }

        pr.clampPage(total);

        // Sorting
        String sortCol = SORT_WHITELIST.get(pr.getSort());
        if (sortCol == null) {
            sortCol = "r.created_at";
        }
        String dir = "ASC".equalsIgnoreCase(pr.getDir()) ? "ASC" : "DESC";

        String dataSql = "SELECT r.review_id, r.booking_id, r.guest_id, r.room_id, r.rating, r.comment, r.status, "
                       + "r.moderated_by, r.moderated_at, r.created_at, "
                       + "u.full_name AS guest_name, u.email AS guest_email, "
                       + "rm.room_number, "
                       + "m.full_name AS moderator_name, "
                       + "b.booking_code "
                       + "FROM REVIEW r "
                       + "JOIN APP_USER u ON r.guest_id = u.user_id "
                       + "JOIN ROOM rm ON r.room_id = rm.room_id "
                       + "LEFT JOIN BOOKING b ON r.booking_id = b.booking_id "
                       + "LEFT JOIN APP_USER m ON r.moderated_by = m.user_id "
                       + where.toString()
                       + "ORDER BY " + sortCol + " " + dir + ", r.review_id DESC "
                       + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<Review> items = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(dataSql)) {
            int idx = 1;
            for (Object param : params) {
                ps.setObject(idx++, param);
            }
            ps.setInt(idx++, pr.getOffset());
            ps.setInt(idx, pr.getSize());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review rev = new Review();
                    rev.setReviewId(rs.getLong("review_id"));
                    rev.setBookingId(rs.getLong("booking_id"));
                    rev.setBookingCode(rs.getString("booking_code"));
                    rev.setGuestId(rs.getLong("guest_id"));
                    rev.setRoomId(rs.getLong("room_id"));
                    rev.setRating(rs.getInt("rating"));
                    rev.setComment(rs.getString("comment"));
                    rev.setStatus(rs.getString("status"));
                    rev.setCreatedAt(rs.getTimestamp("created_at"));
                    rev.setGuestName(rs.getString("guest_name"));
                    rev.setGuestEmail(rs.getString("guest_email"));
                    rev.setRoomNumber(rs.getString("room_number"));
                    long mb = rs.getLong("moderated_by");
                    if (!rs.wasNull()) {
                        rev.setModeratedBy(mb);
                    }
                    rev.setModeratorName(rs.getString("moderator_name"));
                    rev.setModeratedAt(rs.getTimestamp("moderated_at"));
                    items.add(rev);
                }
            }
        }

        return new PageResult<>(items, total, pr);
    }

    /**
     * Retrieves a single review by ID with guest, room, and moderator details.
     */
    public Review findById(long reviewId) throws SQLException {
        String sql = "SELECT r.review_id, r.booking_id, r.guest_id, r.room_id, r.rating, r.comment, r.status, "
                   + "r.moderated_by, r.moderated_at, r.created_at, "
                   + "u.full_name AS guest_name, u.email AS guest_email, "
                   + "rm.room_number, "
                   + "m.full_name AS moderator_name "
                   + "FROM REVIEW r "
                   + "JOIN APP_USER u ON r.guest_id = u.user_id "
                   + "JOIN ROOM rm ON r.room_id = rm.room_id "
                   + "LEFT JOIN APP_USER m ON r.moderated_by = m.user_id "
                   + "WHERE r.review_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reviewId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Review rev = new Review();
                    rev.setReviewId(rs.getLong("review_id"));
                    rev.setBookingId(rs.getLong("booking_id"));
                    rev.setGuestId(rs.getLong("guest_id"));
                    rev.setRoomId(rs.getLong("room_id"));
                    rev.setRating(rs.getInt("rating"));
                    rev.setComment(rs.getString("comment"));
                    rev.setStatus(rs.getString("status"));
                    rev.setCreatedAt(rs.getTimestamp("created_at"));
                    rev.setGuestName(rs.getString("guest_name"));
                    rev.setGuestEmail(rs.getString("guest_email"));
                    rev.setRoomNumber(rs.getString("room_number"));
                    long mb = rs.getLong("moderated_by");
                    if (!rs.wasNull()) {
                        rev.setModeratedBy(mb);
                    }
                    rev.setModeratorName(rs.getString("moderator_name"));
                    rev.setModeratedAt(rs.getTimestamp("moderated_at"));
                    return rev;
                }
            }
        }
        return null;
    }

    /**
     * Updates moderation status (VISIBLE or HIDDEN) and records moderator.
     */
    public boolean updateStatus(long reviewId, String status, Long moderatedBy) throws SQLException {
        String sql = "UPDATE REVIEW SET status = ?, moderated_by = ?, moderated_at = SYSDATETIME() "
                   + "WHERE review_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            if (moderatedBy != null) {
                ps.setLong(2, moderatedBy);
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setLong(3, reviewId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves aggregated rating statistics for each active room.
     */
    public List<Map<String, Object>> getRoomRatingSummaries() throws SQLException {
        String sql = "SELECT rm.room_id, rm.room_number, "
                   + "COALESCE(COUNT(r.review_id), 0) AS total_reviews, "
                   + "COALESCE(AVG(CAST(r.rating AS FLOAT)), 0.0) AS avg_rating "
                   + "FROM ROOM rm "
                   + "LEFT JOIN REVIEW r ON rm.room_id = r.room_id AND r.status = 'VISIBLE' "
                   + "WHERE rm.is_active = 1 "
                   + "GROUP BY rm.room_id, rm.room_number "
                   + "ORDER BY rm.room_number ASC";

        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("roomId", rs.getLong("room_id"));
                String roomNum = rs.getString("room_number");
                map.put("roomNumber", roomNum);
                map.put("displayName", util.ElFunctions.getDisplayName(roomNum));
                int count = rs.getInt("total_reviews");
                map.put("totalReviews", count);
                double avg = rs.getDouble("avg_rating");
                map.put("avgRating", String.format(java.util.Locale.US, "%.1f", avg));
                map.put("rawAvg", avg);
                list.add(map);
            }
        }
        return list;
    }
}

