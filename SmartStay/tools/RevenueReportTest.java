package tools;

import util.RevenueReport;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit test for util.RevenueReport (Task R11): period change %, CSV builder, quick ranges, labels.
 */
public class RevenueReportTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(boolean condition, String message) {
        if (condition) {
            System.out.println("  [PASS] " + message);
            passed++;
        } else {
            System.err.println("  [FAIL] " + message);
            failed++;
        }
    }

    private static Map<String, Object> bucket(String period, int count, long revenue) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("period", period);
        m.put("bookingCount", count);
        m.put("revenue", revenue);
        return m;
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("        REVENUE REPORT HELPERS VERIFICATION (R11) ");
        System.out.println("==================================================");

        try {
            System.out.println("\n--- 1. Change vs previous period ---");
            check(Double.valueOf(25.0).equals(RevenueReport.changePercent(1250000, 1000000)), "+25%");
            check(Double.valueOf(-40.0).equals(RevenueReport.changePercent(600000, 1000000)), "-40%");
            check(Double.valueOf(33.3).equals(RevenueReport.changePercent(400000, 300000)), "Rounded to 1 decimal (33.3)");
            check(Double.valueOf(0.0).equals(RevenueReport.changePercent(500000, 500000)), "No change -> 0");
            check(RevenueReport.changePercent(500000, 0) == null, "Previous 0 -> null (nothing to compare)");
            check("up".equals(RevenueReport.direction(12.5)) && "down".equals(RevenueReport.direction(-3.0))
                    && "flat".equals(RevenueReport.direction(0.0)) && "flat".equals(RevenueReport.direction(null)), "Direction");

            System.out.println("\n--- 2. CSV builder ---");
            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(bucket("2026-10-24", 2, 518000));
            rows.add(bucket("2026-10-25", 0, 0));
            rows.add(bucket("2026-10-26", 1, 259000));
            String csv = RevenueReport.buildCsv(rows, false);
            String[] lines = csv.split("\r\n");
            check(csv.startsWith("﻿"), "Starts with UTF-8 BOM for Excel");
            check(lines[0].equals("﻿Period,Bookings,Revenue (VND),Avg per booking (VND)"), "Header row");
            check("24/10/2026,2,518000,259000".equals(lines[1]), "Day row uses dd/MM/yyyy and plain numbers");
            check("25/10/2026,0,0,0".equals(lines[2]), "Zero row kept by default");
            check(lines.length == 5 && "Total,3,777000,259000".equals(lines[4]), "Total row = sum of the rows");
            String hidden = RevenueReport.buildCsv(rows, true);
            String[] hl = hidden.split("\r\n");
            check(hl.length == 4 && !hidden.contains("25/10/2026"), "hideZero drops zero-revenue rows");
            check("Total,3,777000,259000".equals(hl[3]), "Total unchanged when zero rows are hidden");
            check(RevenueReport.buildCsv(new ArrayList<Map<String, Object>>(), false).endsWith("Total,0,0,0\r\n"), "Empty -> Total 0");
            List<Map<String, Object>> months = new ArrayList<>();
            months.add(bucket("2026-09", 10, 2590000));
            check(RevenueReport.buildCsv(months, false).contains("09/2026,10,2590000,259000"), "Month label MM/yyyy");
            List<Map<String, Object>> odd = new ArrayList<>();
            odd.add(bucket("=SUM(A1),x", 1, 1));
            check(RevenueReport.buildCsv(odd, false).contains("\"'=SUM(A1),x\""), "Formula + comma are neutralised and quoted");

            System.out.println("\n--- 3. Quick ranges ---");
            LocalDate today = LocalDate.of(2026, 10, 26);
            LocalDate[] r7 = RevenueReport.quickRange("7d", today);
            check(r7[0].equals(LocalDate.of(2026, 10, 20)) && r7[1].equals(today), "7d = 20/10 .. 26/10 (7 days inclusive)");
            check(RevenueReport.quickRange("30d", today)[0].equals(LocalDate.of(2026, 9, 27)), "30d starts 27/09");
            check(RevenueReport.quickRange("90d", today)[0].equals(LocalDate.of(2026, 7, 29)), "90d starts 29/07");
            check(RevenueReport.quickRange("month", today)[0].equals(LocalDate.of(2026, 10, 1)), "This month starts on the 1st");
            check(RevenueReport.quickRange("bogus", today) == null && RevenueReport.quickRange(null, today) == null, "Unknown key -> null");
            check("7d".equals(RevenueReport.activeQuickRange(r7[0], r7[1], today)), "Active chip detected");
            check("".equals(RevenueReport.activeQuickRange(LocalDate.of(2026, 1, 1), today, today)), "Custom range -> no chip");

            System.out.println("\n--- 4. Labels, average, sorting ---");
            check("26/10/2026".equals(RevenueReport.periodLabel("2026-10-26")) && "2026".equals(RevenueReport.periodLabel("2026")), "Period labels");
            check(RevenueReport.avgPerBooking(777000, 3) == 259000 && RevenueReport.avgPerBooking(100, 0) == 0, "Average per booking");
            List<Map<String, Object>> sorted = RevenueReport.sortByRevenueDesc(rows);
            check(((Number) sorted.get(0).get("revenue")).longValue() == 518000
                    && ((Number) sorted.get(2).get("revenue")).longValue() == 0, "Sorted by revenue desc");
            check("2026-10-24".equals(rows.get(0).get("period")), "Original list untouched");
        } catch (Throwable t) {
            System.err.println("Unexpected error: " + t);
            t.printStackTrace();
            failed++;
        }

        System.out.println("\n==================================================");
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        System.out.println("==================================================");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
