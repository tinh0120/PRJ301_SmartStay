package dao;

import model.Payment;
import util.DBUtil;
import util.PageResult.PageRequest;
import util.PageResult;
import util.SqlUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data Access Object for PAYMENT records and administrative review queries.
 */
public class PaymentDao {

    private static final Set<String> ALLOWED_STATUSES = new HashSet<>(Arrays.asList(
            "PENDING", "PAID", "FAILED", "EXPIRED", "REVIEW_REQUIRED", "REFUND_PENDING", "REFUNDED"
    ));

    private static final Set<String> SORT_WHITELIST = new HashSet<>(Arrays.asList(
            "created", "amount", "status", "paid_at", "order"
    ));

    public PaymentDao() {
    }

    /**
     * Retrieves paginated payment transactions with optional keyword search and status filter.
     */
    public PageResult<Payment> findPage(PageRequest pr) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        String status = pr.getFilter("status");
        if (!status.isEmpty() && ALLOWED_STATUSES.contains(status.toUpperCase())) {
            where.append(" AND p.status = ? ");
            params.add(status.toUpperCase());
        }

        String keyword = pr.getKeyword();
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (b.booking_code LIKE ? ESCAPE '!' OR u.full_name LIKE ? ESCAPE '!' "
                    + "OR u.email LIKE ? ESCAPE '!' OR CAST(p.provider_order_code AS VARCHAR(30)) LIKE ? ESCAPE '!') ");
            String escaped = "%" + SqlUtil.escapeLike(keyword.trim()) + "%";
            params.add(escaped);
            params.add(escaped);
            params.add(escaped);
            params.add(escaped);
        }

        String countSql = "SELECT COUNT(*) FROM PAYMENT p "
                + "JOIN BOOKING b ON p.booking_id = b.booking_id "
                + "JOIN APP_USER u ON b.guest_id = u.user_id "
                + where;

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
            return new PageResult<>(new ArrayList<Payment>(), 0, pr);
        }

        String sortCol = "p.created_at";
        String sortReq = pr.getSort();
        if (sortReq != null && SORT_WHITELIST.contains(sortReq.toLowerCase())) {
            switch (sortReq.toLowerCase()) {
                case "amount":
                    sortCol = "p.amount";
                    break;
                case "status":
                    sortCol = "p.status";
                    break;
                case "paid_at":
                    sortCol = "p.paid_at";
                    break;
                case "order":
                    sortCol = "p.provider_order_code";
                    break;
                case "created":
                default:
                    sortCol = "p.created_at";
                    break;
            }
        }
        String dir = "ASC".equalsIgnoreCase(pr.getDir()) ? "ASC" : "DESC";

        String pageSql = "SELECT p.payment_id, p.booking_id, b.booking_code, u.full_name AS guest_name, u.email AS guest_email, "
                + "p.provider, p.provider_order_code, p.payment_link_id, p.amount, p.currency, p.status, "
                + "p.transaction_reference, p.expires_at, p.paid_at, p.resolution_note, p.resolved_by, "
                + "r.full_name AS resolver_name, p.resolved_at, p.created_at, p.updated_at "
                + "FROM PAYMENT p "
                + "JOIN BOOKING b ON p.booking_id = b.booking_id "
                + "JOIN APP_USER u ON b.guest_id = u.user_id "
                + "LEFT JOIN APP_USER r ON p.resolved_by = r.user_id "
                + where
                + "ORDER BY " + sortCol + " " + dir + " "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<Payment> items = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(pageSql)) {
            int idx = 1;
            for (Object param : params) {
                ps.setObject(idx++, param);
            }
            ps.setInt(idx++, pr.getOffset());
            ps.setInt(idx, pr.getSize());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }

        return new PageResult<>(items, total, pr);
    }

    /**
     * Finds a single payment by ID with joined guest and booking codes.
     */
    public Payment findById(long paymentId) throws SQLException {
        String sql = "SELECT p.payment_id, p.booking_id, b.booking_code, u.full_name AS guest_name, u.email AS guest_email, "
                + "p.provider, p.provider_order_code, p.payment_link_id, p.amount, p.currency, p.status, "
                + "p.transaction_reference, p.expires_at, p.paid_at, p.resolution_note, p.resolved_by, "
                + "r.full_name AS resolver_name, p.resolved_at, p.created_at, p.updated_at "
                + "FROM PAYMENT p "
                + "JOIN BOOKING b ON p.booking_id = b.booking_id "
                + "JOIN APP_USER u ON b.guest_id = u.user_id "
                + "LEFT JOIN APP_USER r ON p.resolved_by = r.user_id "
                + "WHERE p.payment_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds all payment transaction attempts for a booking ordered newest first.
     */
    public List<Payment> findByBookingId(long bookingId) throws SQLException {
        String sql = "SELECT p.payment_id, p.booking_id, b.booking_code, u.full_name AS guest_name, u.email AS guest_email, "
                + "p.provider, p.provider_order_code, p.payment_link_id, p.amount, p.currency, p.status, "
                + "p.transaction_reference, p.expires_at, p.paid_at, p.resolution_note, p.resolved_by, "
                + "r.full_name AS resolver_name, p.resolved_at, p.created_at, p.updated_at "
                + "FROM PAYMENT p "
                + "JOIN BOOKING b ON p.booking_id = b.booking_id "
                + "JOIN APP_USER u ON b.guest_id = u.user_id "
                + "LEFT JOIN APP_USER r ON p.resolved_by = r.user_id "
                + "WHERE p.booking_id = ? "
                + "ORDER BY p.payment_id DESC";

        List<Payment> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getLong("payment_id"));
        p.setBookingId(rs.getLong("booking_id"));
        p.setBookingCode(rs.getString("booking_code"));
        p.setGuestName(rs.getString("guest_name"));
        p.setGuestEmail(rs.getString("guest_email"));
        p.setProvider(rs.getString("provider"));
        p.setProviderOrderCode(rs.getLong("provider_order_code"));
        p.setPaymentLinkId(rs.getString("payment_link_id"));

        BigDecimal amtBd = rs.getBigDecimal("amount");
        p.setAmount(amtBd != null ? amtBd.longValueExact() : 0L);

        p.setCurrency(rs.getString("currency"));
        p.setStatus(rs.getString("status"));
        p.setTransactionReference(rs.getString("transaction_reference"));
        p.setExpiresAt(rs.getTimestamp("expires_at"));
        p.setPaidAt(rs.getTimestamp("paid_at"));
        p.setResolutionNote(rs.getString("resolution_note"));

        long rb = rs.getLong("resolved_by");
        if (!rs.wasNull()) {
            p.setResolvedBy(rb);
        }
        p.setResolverName(rs.getString("resolver_name"));
        p.setResolvedAt(rs.getTimestamp("resolved_at"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        return p;
    }
}
