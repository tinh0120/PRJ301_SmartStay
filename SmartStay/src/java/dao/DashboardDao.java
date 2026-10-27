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
                   + "WHERE bs.slot_date = ? "
                   + "GROUP BY b.status";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(util.ClockService.now().toLocalDate()));
            try (ResultSet rs = ps.executeQuery()) {
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
        LocalDate today = util.ClockService.now().toLocalDate();
        String sql = "SELECT "
                   + "  ISNULL(SUM(CASE WHEN CAST(p.paid_at AS date) = ? THEN p.amount ELSE 0 END), 0) AS rev_today, "
                   + "  ISNULL(SUM(CASE WHEN YEAR(p.paid_at) = ? AND MONTH(p.paid_at) = ? THEN p.amount ELSE 0 END), 0) AS rev_month "
                   + "FROM PAYMENT p "
                   + "JOIN BOOKING b ON p.booking_id = b.booking_id "
                   + "WHERE p.status = 'PAID' AND b.status <> 'CANCELLED'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(today));
            ps.setInt(2, today.getYear());
            ps.setInt(3, today.getMonthValue());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result[0] = rs.getLong("rev_today");
                    result[1] = rs.getLong("rev_month");
                }
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
        LocalDate today = util.ClockService.now().toLocalDate();
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

    // =========================================================================
    // DASHBOARD v2 (Task R6): "what do I need to do today?"
    // Business time comes from ClockService; device status uses REAL time.
    // =========================================================================

    /**
     * Check-ins today: [0] = already checked in, [1] = expected (first slot starts today and the
     * booking is confirmed, checked in, completed or became a no-show).
     */
    public int[] getCheckinsToday(LocalDate today) throws SQLException {
        // Same predicate as the Bookings page chip ?chip=checkins_today (R7)
        String sql = "SELECT "
                + "  SUM(CASE WHEN b.actual_checkin_at IS NOT NULL THEN 1 ELSE 0 END) AS done_cnt, "
                + "  COUNT(*) AS expected_cnt "
                + "FROM BOOKING b WHERE " + BookingDao.CHECKIN_TODAY_PREDICATE;
        return countPair(sql, today);
    }

    /** Check-outs today: [0] = already checked out, [1] = expected (last slot ends today). */
    public int[] getCheckoutsToday(LocalDate today) throws SQLException {
        // Same predicate as the Bookings page chip ?chip=checkouts_today (R7)
        String sql = "SELECT "
                + "  SUM(CASE WHEN b.status = 'CHECKED_OUT' THEN 1 ELSE 0 END) AS done_cnt, "
                + "  COUNT(*) AS expected_cnt "
                + "FROM BOOKING b WHERE " + BookingDao.CHECKOUT_TODAY_PREDICATE;
        return countPair(sql, today);
    }

    private int[] countPair(String sql, LocalDate day) throws SQLException {
        int[] out = new int[]{0, 0};
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(day));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    out[0] = rs.getInt("done_cnt");
                    out[1] = rs.getInt("expected_cnt");
                }
            }
        }
        return out;
    }

    /**
     * NEW (unacknowledged) alerts by severity: the same set as the Alerts page "Open" tab
     * (/admin/alerts?status=NEW). Keys CRITICAL, HIGH, MEDIUM, LOW, TOTAL.
     */
    public Map<String, Integer> getNewAlertsBySeverity() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("CRITICAL", 0);
        map.put("HIGH", 0);
        map.put("MEDIUM", 0);
        map.put("LOW", 0);
        int total = 0;
        String sql = "SELECT severity, COUNT(*) AS cnt FROM ALERT_LOG WHERE status = 'NEW' GROUP BY severity";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String sev = rs.getString("severity");
                int cnt = rs.getInt("cnt");
                if (sev != null) {
                    map.put(sev.toUpperCase(), cnt);
                }
                total += cnt;
            }
        }
        map.put("TOTAL", total);
        return map;
    }

    /**
     * Revenue month-to-date vs the same days of the previous month (PAID, booking not cancelled).
     * [0] = this month 1..today, [1] = previous month 1..same day (clamped to its length).
     */
    public long[] getRevenueMonthToDate(LocalDate today) throws SQLException {
        LocalDate start = today.withDayOfMonth(1);
        LocalDate prevStart = start.minusMonths(1);
        LocalDate prevEnd = prevStart.withDayOfMonth(Math.min(today.getDayOfMonth(), prevStart.lengthOfMonth()));
        String sql = "SELECT "
                + "  ISNULL(SUM(CASE WHEN CAST(p.paid_at AS date) BETWEEN ? AND ? THEN p.amount ELSE 0 END), 0) AS cur, "
                + "  ISNULL(SUM(CASE WHEN CAST(p.paid_at AS date) BETWEEN ? AND ? THEN p.amount ELSE 0 END), 0) AS prev "
                + "FROM PAYMENT p JOIN BOOKING b ON p.booking_id = b.booking_id "
                + "WHERE p.status = 'PAID' AND b.status <> 'CANCELLED'";
        long[] out = new long[]{0L, 0L};
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(today));
            ps.setDate(3, Date.valueOf(prevStart));
            ps.setDate(4, Date.valueOf(prevEnd));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    out[0] = rs.getLong("cur");
                    out[1] = rs.getLong("prev");
                }
            }
        }
        return out;
    }

    /** Active rooms with status and next event (check-out of the current stay, else next check-in). */
    public List<model.RoomTile> getRoomTiles(java.time.LocalDateTime now) throws SQLException {
        String sql = "SELECT r.room_id, r.room_number, r.display_name, r.status, "
                + "  (SELECT MIN(b.checkout_at) FROM BOOKING b WHERE b.room_id = r.room_id AND b.status = 'CHECKED_IN') AS next_checkout, "
                + "  (SELECT MIN(b.checkin_at) FROM BOOKING b WHERE b.room_id = r.room_id AND b.status = 'CONFIRMED' AND b.checkin_at >= ?) AS next_checkin "
                + "FROM ROOM r WHERE r.is_active = 1 ORDER BY r.room_number";
        List<model.RoomTile> tiles = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(now));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.RoomTile t = new model.RoomTile();
                    t.setRoomId(rs.getLong("room_id"));
                    t.setRoomNumber(rs.getString("room_number"));
                    t.setDisplayName(rs.getString("display_name"));
                    t.setStatus(rs.getString("status"));
                    Timestamp out = rs.getTimestamp("next_checkout");
                    Timestamp in = rs.getTimestamp("next_checkin");
                    if (out != null) {
                        t.setNextEvent("Check-out " + eventTime(out.toLocalDateTime(), now));
                    } else if (in != null) {
                        t.setNextEvent("Check-in " + eventTime(in.toLocalDateTime(), now));
                    }
                    tiles.add(t);
                }
            }
        }
        return tiles;
    }

    /** "14:00" today, "26/10 14:00" on another day. */
    public static String eventTime(java.time.LocalDateTime t, java.time.LocalDateTime now) {
        if (t.toLocalDate().equals(now.toLocalDate())) {
            return t.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        return t.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
    }

    /**
     * "Needs attention" sources, most urgent first. Returns every candidate; the caller shows the
     * first few. Sources: NEW HIGH/CRITICAL alerts, faulty / offline devices (REAL time), overdue
     * check-ins, REFUND_PENDING payments, no-shows still holding slots, expired cleaning shifts.
     */
    public List<model.AttentionItem> getAttentionItems(java.time.LocalDateTime now) throws SQLException {
        List<model.AttentionItem> items = new ArrayList<>();
        DateTimeFormatter hm = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dm = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        try (Connection conn = DBUtil.getConnection()) {

            // 1. Unacknowledged HIGH / CRITICAL alerts
            String alertSql = "SELECT TOP 10 alert_id, alert_type, severity, message, created_at "
                    + "FROM ALERT_LOG WHERE status = 'NEW' AND severity IN ('CRITICAL', 'HIGH') "
                    + "ORDER BY created_at DESC, alert_id DESC";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(alertSql)) {
                while (rs.next()) {
                    boolean critical = "CRITICAL".equalsIgnoreCase(rs.getString("severity"));
                    String type = rs.getString("alert_type");
                    Timestamp ts = rs.getTimestamp("created_at");
                    items.add(new model.AttentionItem(
                            critical ? model.AttentionItem.RANK_CRITICAL_ALERT : model.AttentionItem.RANK_HIGH_ALERT,
                            critical ? "critical" : "high",
                            util.AlertTitles.title(type),
                            shorten(rs.getString("message"), 110),
                            "Review",
                            "/admin/alerts?status=NEW&type=" + urlEncode(type),
                            ts != null ? ts.toLocalDateTime() : null));
                }
            }

            // 2. Devices: faults one by one, offline ones grouped (REAL time heartbeat)
            String devSql = "SELECT device_id, device_name, status, last_seen_at FROM DEVICE "
                    + "WHERE is_active = 1 AND (status = 'FAULT' OR last_seen_at IS NULL "
                    + "      OR last_seen_at < DATEADD(SECOND, -?, SYSDATETIME())) "
                    + "ORDER BY device_name";
            List<String> offline = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(devSql)) {
                ps.setInt(1, util.AppConstants.DEVICE_OFFLINE_AFTER_SECONDS);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("device_name");
                        if ("FAULT".equalsIgnoreCase(rs.getString("status"))) {
                            Timestamp seen = rs.getTimestamp("last_seen_at");
                            items.add(new model.AttentionItem(model.AttentionItem.RANK_DEVICE_FAULT, "critical",
                                    name + " reports a fault",
                                    seen != null ? "Last seen " + util.ElFunctions.timeAgo(seen) : "Never connected",
                                    "Check device",
                                    "/admin/devices/edit?deviceId=" + urlEncode(rs.getString("device_id")),
                                    seen != null ? seen.toLocalDateTime() : null));
                        } else {
                            offline.add(name);
                        }
                    }
                }
            }
            if (!offline.isEmpty()) {
                items.add(new model.AttentionItem(model.AttentionItem.RANK_DEVICES_OFFLINE, "high",
                        // No count here: the Devices strip already shows it (no number twice on the page)
                        offline.size() == 1 ? offline.get(0) + " is offline" : "Some devices are offline",
                        shorten(String.join(", ", offline), 110),
                        offline.size() == 1 ? "Check device" : "Check devices",
                        "/admin/devices",
                        null));
            }

            // 3. Overdue check-ins (confirmed, first slot already started, nobody checked in yet)
            String overdueSql = "SELECT TOP 10 b.booking_id, b.booking_code, b.checkin_at, r.room_number "
                    + "FROM BOOKING b JOIN ROOM r ON b.room_id = r.room_id "
                    + "WHERE b.status = 'CONFIRMED' AND b.actual_checkin_at IS NULL AND b.checkin_at < ? "
                    + "ORDER BY b.checkin_at";
            try (PreparedStatement ps = conn.prepareStatement(overdueSql)) {
                ps.setTimestamp(1, Timestamp.valueOf(now));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        java.time.LocalDateTime due = rs.getTimestamp("checkin_at").toLocalDateTime();
                        items.add(new model.AttentionItem(model.AttentionItem.RANK_OVERDUE_CHECKIN, "high",
                                rs.getString("booking_code") + " has not checked in",
                                "Room " + rs.getString("room_number") + ", due at " + eventTime(due, now),
                                "View booking",
                                "/admin/bookings/detail?id=" + rs.getLong("booking_id"),
                                due));
                    }
                }
            }

            // 4. Refunds waiting for the operator
            String refundSql = "SELECT TOP 10 b.booking_id, b.booking_code, p.amount, b.cancelled_at "
                    + "FROM PAYMENT p JOIN BOOKING b ON p.booking_id = b.booking_id "
                    + "WHERE p.status = 'REFUND_PENDING' ORDER BY b.cancelled_at";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(refundSql)) {
                while (rs.next()) {
                    Timestamp cancelled = rs.getTimestamp("cancelled_at");
                    items.add(new model.AttentionItem(model.AttentionItem.RANK_REFUND, "medium",
                            "Refund " + rs.getString("booking_code"),
                            util.ElFunctions.money(rs.getBigDecimal("amount"))
                                    + (cancelled != null ? ", cancelled " + cancelled.toLocalDateTime().format(dm) : ""),
                            "Refund",
                            "/admin/bookings/detail?id=" + rs.getLong("booking_id"),
                            cancelled != null ? cancelled.toLocalDateTime() : null));
                }
            }

            // 5. No-shows still holding slots (grouped)
            int noshowHeld = getNoshowHeldCount();
            if (noshowHeld > 0) {
                items.add(new model.AttentionItem(model.AttentionItem.RANK_NOSHOW_HELD, "medium",
                        noshowHeld == 1 ? "1 no-show still holds its slots" : noshowHeld + " no-shows still hold their slots",
                        "Release the slots so other guests can book them.",
                        "View bookings",
                        "/admin/bookings?flag=noshow_held",
                        null));
            }

            // 6. Cleaning shifts that ended while the room is still dirty
            String cleanSql = "SELECT TOP 10 sa.assignment_id, sa.shift_end, r.room_number "
                    + "FROM STAFF_ASSIGNMENT sa JOIN ROOM r ON sa.room_id = r.room_id "
                    + "WHERE sa.status = 'EXPIRED' AND r.status = 'NEEDS_CLEANING' "
                    + "  AND NOT EXISTS (SELECT 1 FROM STAFF_ASSIGNMENT sa2 WHERE sa2.room_id = sa.room_id "
                    + "                  AND sa2.status = 'ASSIGNED') "
                    + "ORDER BY sa.shift_end DESC";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(cleanSql)) {
                java.util.Set<String> seenRooms = new java.util.HashSet<>();
                while (rs.next()) {
                    String room = rs.getString("room_number");
                    if (!seenRooms.add(room)) {
                        continue;
                    }
                    java.time.LocalDateTime end = rs.getTimestamp("shift_end").toLocalDateTime();
                    items.add(new model.AttentionItem(model.AttentionItem.RANK_CLEANING_EXPIRED, "medium",
                            "Room " + room + " was not cleaned in time",
                            "Shift ended " + eventTime(end, now),
                            "Reassign",
                            "/admin/assignments",
                            end));
                }
            }
        }

        sortAttention(items);
        return items;
    }

    /** Most urgent first; within the same rank, the oldest (longest waiting) first. */
    public static void sortAttention(List<model.AttentionItem> items) {
        java.util.Collections.sort(items, new java.util.Comparator<model.AttentionItem>() {
            @Override
            public int compare(model.AttentionItem a, model.AttentionItem b) {
                int c = Integer.compare(a.getRank(), b.getRank());
                if (c != 0) {
                    return c;
                }
                if (a.getAt() == null || b.getAt() == null) {
                    return (a.getAt() == null) ? (b.getAt() == null ? 0 : 1) : -1;
                }
                return a.getAt().compareTo(b.getAt());
            }
        });
    }

    private static String shorten(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max - 1).trim() + "…";
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return "";
        }
    }
}
