package util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Pure helpers for the Revenue report (Task R11): period change, quick ranges, period labels,
 * revenue sorting and the CSV export. No database access, unit-tested in tools/RevenueReportTest.
 */
public final class RevenueReport {

    /** UTF-8 byte order mark so Excel opens the CSV with the right encoding. */
    public static final String BOM = "﻿";

    private RevenueReport() {
    }

    /**
     * % change vs the previous period, rounded to 1 decimal; null when the previous period had no
     * revenue (nothing to compare with).
     */
    public static Double changePercent(long current, long previous) {
        if (previous <= 0) {
            return null;
        }
        double pct = (current - previous) * 100.0 / previous;
        return Math.round(pct * 10.0) / 10.0;
    }

    /** "up", "down" or "flat" for a change value (null counts as flat). */
    public static String direction(Double pct) {
        if (pct == null || Math.abs(pct) < 0.05) {
            return "flat";
        }
        return pct > 0 ? "up" : "down";
    }

    /**
     * Quick range chips: 7d / 30d / 90d end today (inclusive, so 7d = today and the 6 days before);
     * "month" = first day of this month to today. Unknown keys return null.
     */
    public static LocalDate[] quickRange(String key, LocalDate today) {
        if (key == null || today == null) {
            return null;
        }
        switch (key) {
            case "7d":
                return new LocalDate[]{today.minusDays(6), today};
            case "30d":
                return new LocalDate[]{today.minusDays(29), today};
            case "90d":
                return new LocalDate[]{today.minusDays(89), today};
            case "month":
                return new LocalDate[]{today.withDayOfMonth(1), today};
            default:
                return null;
        }
    }

    /** Which quick chip matches from..to (or "" for a custom range). */
    public static String activeQuickRange(LocalDate from, LocalDate to, LocalDate today) {
        for (String k : new String[]{"7d", "30d", "90d", "month"}) {
            LocalDate[] r = quickRange(k, today);
            if (r[0].equals(from) && r[1].equals(to)) {
                return k;
            }
        }
        return "";
    }

    /** Bucket key -> display label: day "2026-10-26" -> "26/10/2026", month "2026-10" -> "10/2026". */
    public static String periodLabel(String key) {
        if (key == null) {
            return "";
        }
        if (key.length() == 10 && key.charAt(4) == '-' && key.charAt(7) == '-') {
            return key.substring(8, 10) + "/" + key.substring(5, 7) + "/" + key.substring(0, 4);
        }
        if (key.length() == 7 && key.charAt(4) == '-') {
            return key.substring(5, 7) + "/" + key.substring(0, 4);
        }
        return key;
    }

    /** Average per booking, 0 when there are no bookings. */
    public static long avgPerBooking(long revenue, int bookings) {
        return bookings > 0 ? revenue / bookings : 0L;
    }

    /** Copy of the rows sorted by "revenue" descending (stable, so ties keep their order). */
    public static List<Map<String, Object>> sortByRevenueDesc(List<Map<String, Object>> rows) {
        List<Map<String, Object>> copy = new ArrayList<>(rows == null ? Collections.<Map<String, Object>>emptyList() : rows);
        Collections.sort(copy, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> a, Map<String, Object> b) {
                return Long.compare(num(b.get("revenue")), num(a.get("revenue")));
            }
        });
        return copy;
    }

    /**
     * CSV of the detailed table: Period, Bookings, Revenue (VND), Avg per booking (VND) and a Total row.
     * Same rows as the page; with hideZero the zero-revenue periods are left out (the total does not
     * change because they add 0). Numbers are plain integers so Excel can sum them.
     */
    public static String buildCsv(List<Map<String, Object>> buckets, boolean hideZero) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append("Period,Bookings,Revenue (VND),Avg per booking (VND)\r\n");
        long totalRevenue = 0L;
        int totalBookings = 0;
        if (buckets != null) {
            for (Map<String, Object> b : buckets) {
                long rev = num(b.get("revenue"));
                int cnt = (int) num(b.get("bookingCount"));
                if (hideZero && rev == 0L) {
                    continue;
                }
                totalRevenue += rev;
                totalBookings += cnt;
                sb.append(csvCell(periodLabel(String.valueOf(b.get("period"))))).append(',')
                  .append(cnt).append(',')
                  .append(rev).append(',')
                  .append(avgPerBooking(rev, cnt)).append("\r\n");
            }
        }
        sb.append("Total,").append(totalBookings).append(',').append(totalRevenue).append(',')
          .append(avgPerBooking(totalRevenue, totalBookings)).append("\r\n");
        return sb.toString();
    }

    /** RFC 4180 quoting; also neutralises spreadsheet formulas (=, +, -, @ at the start). */
    static String csvCell(String s) {
        if (s == null) {
            return "";
        }
        String v = s;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.indexOf(',') >= 0 || v.indexOf('"') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private static long num(Object o) {
        return (o instanceof Number) ? ((Number) o).longValue() : 0L;
    }
}
