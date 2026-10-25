package dao;

import model.AlertEntry;
import util.DBUtil;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified Data Access Object for operational dashboard metrics, cards, alerts,
 * and financial revenue reports/bucketing.
 */
public class DashboardDao {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy");

    /**
     * Retrieves room counts grouped by operational status.
     * Guaranteed keys: AVAILABLE, OCCUPIED, NEEDS_CLEANING, MAINTENANCE.
     */
    public Map<String, Integer> getRoomStatusCounts() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("AVAILABLE", 0);
        map.put("OCCUPIED", 0);
        map.put("NEEDS_CLEANING", 0);
        map.put("MAINTENANCE", 0);

        String sql = "SELECT status, COUNT(*) AS cnt FROM ROOM GROUP BY status";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String st = rs.getString("status");
                if (st != null) {
                    map.put(st.toUpperCase(), rs.getInt("cnt"));
                }
            }
        }
        return map;
    }

    /**
     * Retrieves today's booking counts (by slot_date = server local date).
     * Includes breakdown by status and TOTAL.
     */
    public Map<String, Integer> getTodayBookingCounts() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("CONFIRMED", 0);
        map.put("CHECKED_IN", 0);
        map.put("CHECKED_OUT", 0);
        map.put("CANCELLED", 0);
        map.put("EXPIRED", 0);
        map.put("TOTAL", 0);

        String sql = "SELECT b.status, COUNT(DISTINCT b.booking_id) AS cnt "
                   + "FROM BOOKING b "
                   + "JOIN BOOKING_SLOT bs ON b.booking_id = bs.booking_id "
                   + "WHERE bs.slot_date = CAST(SYSDATETIME() AS date) "
                   + "GROUP BY b.status";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int total = 0;
            while (rs.next()) {
                String st = rs.getString("status");
                int cnt = rs.getInt("cnt");
                if (st != null) {
                    map.put(st.toUpperCase(), cnt);
                }
                total += cnt;
            }
            map.put("TOTAL", total);
        }
        return map;
    }

    /**
     * Retrieves revenue for today and this calendar month.
     * Only PAID payments are included, grouped by paid_at.
     *
     * @return long array: [0] = revenueToday, [1] = revenueThisMonth
     */
    public long[] getRevenueTodayAndMonth() throws SQLException {
        long[] result = new long[]{0L, 0L};
        String sql = "SELECT "
                   + "  ISNULL(SUM(CASE WHEN CAST(p.paid_at AS date) = CAST(SYSDATETIME() AS date) THEN p.amount ELSE 0 END), 0) AS rev_today, "
                   + "  ISNULL(SUM(CASE WHEN YEAR(p.paid_at) = YEAR(SYSDATETIME()) AND MONTH(p.paid_at) = MONTH(SYSDATETIME()) THEN p.amount ELSE 0 END), 0) AS rev_month "
                   + "FROM PAYMENT p "
                   + "JOIN BOOKING b ON p.booking_id = b.booking_id "
                   + "WHERE p.status = 'PAID' AND b.status <> 'CANCELLED'";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                result[0] = rs.getLong("rev_today");
                result[1] = rs.getLong("rev_month");
            }
        }
        return result;
    }

    /**
     * Retrieves hardware device counts grouped by operational status.
     * Guaranteed keys: ONLINE, OFFLINE, FAULT.
     */
    public Map<String, Integer> getDeviceStatusCounts() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("ONLINE", 0);
        map.put("OFFLINE", 0);
        map.put("FAULT", 0);

        String sql = "SELECT eff_status, COUNT(*) AS cnt "
                   + "FROM ( "
                   + "  SELECT "
                   + "    CASE "
                   + "      WHEN status = 'FAULT' THEN 'FAULT' "
                   + "      WHEN last_seen_at IS NOT NULL AND last_seen_at >= DATEADD(SECOND, -?, SYSDATETIME()) THEN 'ONLINE' "
                   + "      ELSE 'OFFLINE' "
                   + "    END AS eff_status "
                   + "  FROM DEVICE "
                   + ") d "
                   + "GROUP BY eff_status";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, util.AppConstants.DEVICE_OFFLINE_AFTER_SECONDS);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String st = rs.getString("eff_status");
                    if (st != null) {
                        map.put(st.toUpperCase(), rs.getInt("cnt"));
                    }
                }
            }
        }
        return map;
    }

    /**
     * Retrieves count of rooms currently in NEEDS_CLEANING status.
     */
    public int getPendingCleaningCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM ROOM WHERE status = 'NEEDS_CLEANING'";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Retrieves open unresolved alert counts grouped by severity.
     * Guaranteed keys: CRITICAL, HIGH, MEDIUM, LOW.
     */
    public Map<String, Integer> getOpenAlertsBySeverity() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("CRITICAL", 0);
        map.put("HIGH", 0);
        map.put("MEDIUM", 0);
        map.put("LOW", 0);

        String sql = "SELECT severity, COUNT(*) AS cnt "
                   + "FROM ALERT_LOG "
                   + "WHERE status IN ('NEW', 'ACKNOWLEDGED') "
                   + "GROUP BY severity";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String sev = rs.getString("severity");
                if (sev != null) {
                    map.put(sev.toUpperCase(), rs.getInt("cnt"));
                }
            }
        }
        return map;
    }

    /**
     * Retrieves the count of unacknowledged alerts in status NEW.
     */
    public int getNewAlertCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM ALERT_LOG WHERE status = 'NEW'";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Retrieves count of no-show bookings that currently hold room slots.
     */
    public int getNoshowHeldCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM BOOKING b "
                   + "WHERE b.status = 'EXPIRED' AND b.expire_reason = 'NO_SHOW' "
                   + "AND EXISTS (SELECT 1 FROM BOOKING_SLOT bs WHERE bs.booking_id = b.booking_id AND bs.hold_status = 'HELD')";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Retrieves the 5 newest alerts.
     */
    public List<AlertEntry> getLatestAlerts(int limit) throws SQLException {
        List<AlertEntry> list = new ArrayList<>();
        String sql = "SELECT TOP (?) alert_id, alert_type, severity, status, message, created_at "
                   + "FROM ALERT_LOG "
                   + "ORDER BY created_at DESC, alert_id DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit <= 0 ? 5 : limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AlertEntry entry = new AlertEntry();
                    entry.setAlertId(rs.getLong("alert_id"));
                    entry.setAlertType(rs.getString("alert_type"));
                    entry.setSeverity(rs.getString("severity"));
                    entry.setStatus(rs.getString("status"));
                    entry.setMessage(rs.getString("message"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    entry.setCreatedAt(ts);
                    if (ts != null) {
                        entry.setCreatedAtFormatted(ts.toLocalDateTime().format(TIME_FMT));
                    }
                    list.add(entry);
                }
            }
        }
        return list;
    }

    /**
     * Retrieves revenue for the last 7 calendar days ending today.
     * Missing days are filled with 0.
     *
     * @return Map with "labels" (List of yyyy-MM-dd) and "values" (List of Long)
     */
    public Map<String, Object> getRevenueLast7Days() throws SQLException {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);

        Map<String, Long> dayTotals = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = startDate.plusDays(i);
            dayTotals.put(d.format(DATE_FMT), 0L);
        }

        String sql = "SELECT CAST(p.paid_at AS date) AS dt, ISNULL(SUM(p.amount), 0) AS total "
                   + "FROM PAYMENT p "
                   + "JOIN BOOKING b ON p.booking_id = b.booking_id "
                   + "WHERE p.status = 'PAID' AND b.status <> 'CANCELLED' "
                   + "  AND CAST(p.paid_at AS date) >= ? AND CAST(p.paid_at AS date) <= ? "
                   + "GROUP BY CAST(p.paid_at AS date)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date dt = rs.getDate("dt");
                    if (dt != null) {
                        String key = dt.toLocalDate().format(DATE_FMT);
                        if (dayTotals.containsKey(key)) {
                            dayTotals.put(key, rs.getLong("total"));
                        }
                    }
                }
            }
        }

        List<String> labels = new ArrayList<>(dayTotals.keySet());
        List<Long> values = new ArrayList<>(dayTotals.values());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("values", values);
        return result;
    }

    /**
     * Retrieves alert count grouped by alert type for doughnut chart.
     *
     * @return Map with "labels" (List of alert_type) and "values" (List of count)
     */
    public Map<String, Object> getAlertsByType() throws SQLException {
        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        List<Map<String, Object>> items = new ArrayList<>();

        String sql = "SELECT alert_type, COUNT(*) AS cnt, "
                   + "  MAX(severity) AS sample_severity "
                   + "FROM ALERT_LOG "
                   + "WHERE alert_type IS NOT NULL "
                   + "GROUP BY alert_type "
                   + "ORDER BY cnt DESC";
        int maxCount = 0;
        int totalCount = 0;
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String type = rs.getString("alert_type");
                int cnt = rs.getInt("cnt");
                String sev = rs.getString("sample_severity");
                if (sev == null || sev.trim().isEmpty()) {
                    sev = "MEDIUM";
                }
                labels.add(type);
                values.add(cnt);
                totalCount += cnt;
                if (cnt > maxCount) {
                    maxCount = cnt;
                }

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("type", type);
                item.put("count", cnt);
                item.put("severity", sev.toUpperCase());
                items.add(item);
            }
        }

        for (Map<String, Object> item : items) {
            int cnt = (Integer) item.get("count");
            int percent = (maxCount > 0) ? (int) Math.round((cnt * 100.0) / maxCount) : 0;
            if (percent < 8 && cnt > 0) percent = 8;
            item.put("percent", percent);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("values", values);
        result.put("items", items);
        result.put("maxCount", maxCount);
        result.put("totalCount", totalCount);
        return result;
    }

    // =========================================================================
    // FINANCIAL REVENUE REPORT METHODS (merged from RevenueDao)
    // =========================================================================

    /**
     * Retrieves aggregated revenue buckets across the specified date range.
     * Missing buckets within the range are filled with 0 so charts and tables are continuous.
     *
     * @param from Start date (inclusive)
     * @param to   End date (inclusive)
     * @param mode Bucketing mode: "day", "month", or "year"
     * @return Ordered list of bucket maps with keys: period, bookingCount, revenue
     */
    public List<Map<String, Object>> findRevenueBuckets(LocalDate from, LocalDate to, String mode) throws SQLException {
        String normalizedMode = normalizeMode(mode);

        // 1. Pre-populate all continuous buckets in Java
        Map<String, Map<String, Object>> bucketMap = buildContinuousBuckets(from, to, normalizedMode);

        // 2. Determine SQL period expression
        String periodExpr;
        if ("month".equals(normalizedMode)) {
            periodExpr = "FORMAT(p.paid_at, 'yyyy-MM')";
        } else if ("year".equals(normalizedMode)) {
            periodExpr = "FORMAT(p.paid_at, 'yyyy')";
        } else {
            periodExpr = "CONVERT(varchar(10), p.paid_at, 23)";
        }

        String sql = "SELECT " + periodExpr + " AS period_key, "
                   + "       COUNT(DISTINCT p.booking_id) AS bookings_count, "
                   + "       ISNULL(SUM(p.amount), 0) AS total_revenue "
                   + "FROM PAYMENT p "
                   + "JOIN BOOKING b ON p.booking_id = b.booking_id "
                   + "WHERE p.status = 'PAID' AND b.status <> 'CANCELLED' "
                   + "  AND p.paid_at >= ? AND p.paid_at < ? "
                   + "GROUP BY " + periodExpr + " "
                   + "ORDER BY period_key ASC";

        Timestamp startTs = Timestamp.valueOf(from.atStartOfDay());
        Timestamp endTs = Timestamp.valueOf(to.plusDays(1).atStartOfDay());

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, startTs);
            ps.setTimestamp(2, endTs);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("period_key");
                    if (key != null && bucketMap.containsKey(key)) {
                        Map<String, Object> b = bucketMap.get(key);
                        b.put("bookingCount", rs.getInt("bookings_count"));
                        b.put("revenue", rs.getLong("total_revenue"));
                    }
                }
            }
        }

        return new ArrayList<>(bucketMap.values());
    }

    private String normalizeMode(String mode) {
        if ("month".equalsIgnoreCase(mode)) {
            return "month";
        }
        if ("year".equalsIgnoreCase(mode)) {
            return "year";
        }
        return "day";
    }

    private Map<String, Map<String, Object>> buildContinuousBuckets(LocalDate from, LocalDate to, String mode) {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();

        if ("month".equals(mode)) {
            YearMonth cur = YearMonth.from(from);
            YearMonth end = YearMonth.from(to);
            while (!cur.isAfter(end)) {
                String key = cur.format(MONTH_FMT);
                map.put(key, createBucket(key, 0, 0L));
                cur = cur.plusMonths(1);
            }
        } else if ("year".equals(mode)) {
            int startYear = from.getYear();
            int endYear = to.getYear();
            for (int y = startYear; y <= endYear; y++) {
                String key = String.valueOf(y);
                map.put(key, createBucket(key, 0, 0L));
            }
        } else {
            LocalDate cur = from;
            while (!cur.isAfter(to)) {
                String key = cur.format(DATE_FMT);
                map.put(key, createBucket(key, 0, 0L));
                cur = cur.plusDays(1);
            }
        }
        return map;
    }

    private Map<String, Object> createBucket(String period, int bookingCount, long revenue) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("period", period);
        b.put("bookingCount", bookingCount);
        b.put("revenue", revenue);
        return b;
    }

    /**
     * Retrieves high-level revenue and occupancy KPIs across the given period,
     * including comparison against the immediately preceding period of identical duration.
     */
    public Map<String, Object> getRevenueKpis(LocalDate from, LocalDate to) throws SQLException {
        Map<String, Object> kpis = new LinkedHashMap<>();

        Timestamp startTs = Timestamp.valueOf(from.atStartOfDay());
        Timestamp endTs = Timestamp.valueOf(to.plusDays(1).atStartOfDay());

        long totalRevenue = 0L;
        int paidBookings = 0;

        String currentSql = "SELECT COUNT(DISTINCT p.booking_id) AS total_bookings, "
                          + "       ISNULL(SUM(p.amount), 0) AS total_rev "
                          + "FROM PAYMENT p "
                          + "JOIN BOOKING b ON p.booking_id = b.booking_id "
                          + "WHERE p.status = 'PAID' AND b.status <> 'CANCELLED' "
                          + "  AND p.paid_at >= ? AND p.paid_at < ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(currentSql)) {
            ps.setTimestamp(1, startTs);
            ps.setTimestamp(2, endTs);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    paidBookings = rs.getInt("total_bookings");
                    totalRevenue = rs.getLong("total_rev");
                }
            }
        }

        long avgPerBooking = (paidBookings > 0) ? (totalRevenue / paidBookings) : 0L;

        // Occupancy calculation
        long daysCount = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        if (daysCount < 1) daysCount = 1;

        int activeRoomCount = 6;
        String roomCountSql = "SELECT COUNT(*) FROM ROOM WHERE status <> 'MAINTENANCE'";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(roomCountSql)) {
            if (rs.next()) {
                int c = rs.getInt(1);
                if (c > 0) activeRoomCount = c;
            }
        }

        int bookedSlots = 0;
        String slotSql = "SELECT COUNT(*) FROM BOOKING_SLOT bs "
                       + "JOIN BOOKING b ON bs.booking_id = b.booking_id "
                       + "WHERE bs.slot_date >= ? AND bs.slot_date <= ? "
                       + "  AND b.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(slotSql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    bookedSlots = rs.getInt(1);
                }
            }
        }

        long totalCapacitySlots = activeRoomCount * 4L * daysCount;
        double occupancyRate = (totalCapacitySlots > 0) ? ((bookedSlots * 100.0) / totalCapacitySlots) : 0.0;
        occupancyRate = Math.round(occupancyRate * 10.0) / 10.0;

        // Previous period change
        LocalDate prevTo = from.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(daysCount - 1);
        Timestamp prevStartTs = Timestamp.valueOf(prevFrom.atStartOfDay());
        Timestamp prevEndTs = Timestamp.valueOf(prevTo.plusDays(1).atStartOfDay());

        long prevRevenue = 0L;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(currentSql)) {
            ps.setTimestamp(1, prevStartTs);
            ps.setTimestamp(2, prevEndTs);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    prevRevenue = rs.getLong("total_rev");
                }
            }
        }

        double changePercent = 0.0;
        String direction = "flat";
        if (prevRevenue > 0) {
            changePercent = ((double) (totalRevenue - prevRevenue) / prevRevenue) * 100.0;
            changePercent = Math.round(changePercent * 10.0) / 10.0;
            if (changePercent > 0.05) {
                direction = "up";
            } else if (changePercent < -0.05) {
                direction = "down";
            }
        } else if (totalRevenue > 0) {
            changePercent = 100.0;
            direction = "up";
        }

        kpis.put("totalRevenue", totalRevenue);
        kpis.put("paidBookings", paidBookings);
        kpis.put("avgPerBooking", avgPerBooking);
        kpis.put("occupancyRate", occupancyRate);
        kpis.put("bookedSlots", bookedSlots);
        kpis.put("totalCapacitySlots", totalCapacitySlots);
        kpis.put("prevRevenue", prevRevenue);
        kpis.put("changePercent", changePercent);
        kpis.put("changeDirection", direction);

        return kpis;
    }

    /**
     * Retrieves revenue grouped by room across the period.
     */
    public List<Map<String, Object>> getRevenueByRoom(LocalDate from, LocalDate to) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        Timestamp startTs = Timestamp.valueOf(from.atStartOfDay());
        Timestamp endTs = Timestamp.valueOf(to.plusDays(1).atStartOfDay());

        String sql = "SELECT r.room_id, r.room_number, r.display_name, "
                   + "       COUNT(DISTINCT p.booking_id) AS booking_count, "
                   + "       ISNULL(SUM(p.amount), 0) AS room_revenue "
                   + "FROM ROOM r "
                   + "LEFT JOIN BOOKING b ON r.room_id = b.room_id AND b.status <> 'CANCELLED' "
                   + "LEFT JOIN PAYMENT p ON b.booking_id = p.booking_id AND p.status = 'PAID' "
                   + "                   AND p.paid_at >= ? AND p.paid_at < ? "
                   + "GROUP BY r.room_id, r.room_number, r.display_name "
                   + "ORDER BY r.room_number ASC";

        long grandTotal = 0L;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, startTs);
            ps.setTimestamp(2, endTs);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("roomId", rs.getLong("room_id"));
                    map.put("roomNumber", rs.getString("room_number"));
                    map.put("displayName", rs.getString("display_name"));
                    map.put("bookingCount", rs.getInt("booking_count"));
                    long rev = rs.getLong("room_revenue");
                    map.put("revenue", rev);
                    grandTotal += rev;
                    list.add(map);
                }
            }
        }

        for (Map<String, Object> item : list) {
            long rev = (Long) item.get("revenue");
            int pct = (grandTotal > 0) ? (int) Math.round((rev * 100.0) / grandTotal) : 0;
            item.put("percent", pct);
        }

        return list;
    }

    /**
     * Retrieves revenue grouped by room time slot (S1 to S4) across the period.
     */
    public List<Map<String, Object>> getRevenueBySlot(LocalDate from, LocalDate to) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        Timestamp startTs = Timestamp.valueOf(from.atStartOfDay());
        Timestamp endTs = Timestamp.valueOf(to.plusDays(1).atStartOfDay());

        String sql = "SELECT rts.slot_code, rts.slot_name, "
                   + "       COUNT(DISTINCT bs.booking_id) AS booking_count, "
                   + "       COUNT(bs.booking_slot_id) AS slot_count, "
                   + "       ISNULL(SUM(CAST(bs.list_price * (100 - b.discount_percent) / 100.0 AS DECIMAL(12,0))), 0) AS total_revenue "
                   + "FROM ROOM_TIME_SLOT rts "
                   + "JOIN BOOKING_SLOT bs ON rts.room_slot_id = bs.room_slot_id "
                   + "JOIN BOOKING b ON bs.booking_id = b.booking_id AND b.status <> 'CANCELLED' "
                   + "JOIN PAYMENT p ON b.booking_id = p.booking_id AND p.status = 'PAID' "
                   + "               AND p.paid_at >= ? AND p.paid_at < ? "
                   + "GROUP BY rts.slot_code, rts.slot_name "
                   + "ORDER BY rts.slot_code ASC";

        long grandTotal = 0L;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, startTs);
            ps.setTimestamp(2, endTs);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("slotCode", rs.getString("slot_code"));
                    map.put("slotName", rs.getString("slot_name"));
                    map.put("bookingCount", rs.getInt("booking_count"));
                    map.put("slotCount", rs.getInt("slot_count"));
                    long rev = rs.getLong("total_revenue");
                    map.put("revenue", rev);
                    grandTotal += rev;
                    list.add(map);
                }
            }
        }

        for (Map<String, Object> item : list) {
            long rev = (Long) item.get("revenue");
            int pct = (grandTotal > 0) ? (int) Math.round((rev * 100.0) / grandTotal) : 0;
            item.put("percent", pct);
        }

        return list;
    }
}
