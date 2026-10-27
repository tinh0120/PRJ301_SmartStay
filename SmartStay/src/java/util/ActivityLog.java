package util;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure helpers for Devices and Activity logs (Task R9): plain-words reasons, zone labels,
 * day labels and grouping. Log timestamps are REAL time (hardware), so callers pass the real date.
 */
public final class ActivityLog {

    private static final Map<String, String> REASONS;

    static {
        Map<String, String> m = new HashMap<>();
        m.put("OK", "");
        m.put("WRONG_PIN", "Wrong PIN");
        m.put("NOT_YET_VALID", "Not yet valid");
        m.put("EXPIRED", "Expired");
        m.put("REVOKED", "Revoked");
        m.put("LOCKED_OUT", "Locked out after too many tries");
        REASONS = Collections.unmodifiableMap(m);
    }

    private ActivityLog() {
    }

    /** ACCESS_LOG.reason_code in plain words; "OK" becomes "" (hidden when allowed). */
    public static String reasonText(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "";
        }
        String key = code.trim().toUpperCase();
        String t = REASONS.get(key);
        return (t != null) ? t : AlertTitles.prettify(key);
    }

    /** "MAIN_ENTRANCE" -> "Main entrance", "LOBBY" -> "Lobby", null -> "Other". */
    public static String zoneLabel(String zone) {
        if (zone == null || zone.trim().isEmpty()) {
            return "Other";
        }
        return AlertTitles.prettify(zone.trim().toUpperCase());
    }

    /** "Today", "Yesterday" or "23/10/2026". */
    public static String dayLabel(LocalDate day, LocalDate today) {
        if (day == null) {
            return "";
        }
        if (day.equals(today)) {
            return "Today";
        }
        if (today != null && day.equals(today.minusDays(1))) {
            return "Yesterday";
        }
        return day.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /** A labelled group of rows (a day of logs, a zone of devices). */
    public static final class Group<T> implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String label;
        private final List<T> items = new ArrayList<>();

        public Group(String label) {
            this.label = label;
        }

        public String getLabel() { return label; }
        public List<T> getItems() { return items; }
        public int getSize() { return items.size(); }
    }

    /** Extracts the value a row is grouped by (Java 8 friendly, no lambdas required). */
    public interface KeyFn<T> {
        String key(T item);
    }

    /** Extracts the timestamp of a row. */
    public interface TimeFn<T> {
        LocalDateTime time(T item);
    }

    /**
     * Groups rows that are already in time order by calendar day, keeping that order:
     * Today / Yesterday / dd/MM/yyyy. Rows without a time go to an "Unknown date" group.
     */
    public static <T> List<Group<T>> groupByDay(List<T> rows, TimeFn<T> timeFn, LocalDate today) {
        Map<String, Group<T>> groups = new LinkedHashMap<>();
        if (rows != null) {
            for (T row : rows) {
                LocalDateTime t = timeFn.time(row);
                String label = (t == null) ? "Unknown date" : dayLabel(t.toLocalDate(), today);
                Group<T> g = groups.get(label);
                if (g == null) {
                    g = new Group<>(label);
                    groups.put(label, g);
                }
                g.items.add(row);
            }
        }
        return new ArrayList<>(groups.values());
    }

    /** Groups rows by a key in first-seen order, labelling each group with labelFn(key). */
    public static <T> List<Group<T>> groupBy(List<T> rows, KeyFn<T> keyFn, KeyFn<String> labelFn) {
        Map<String, Group<T>> groups = new LinkedHashMap<>();
        if (rows != null) {
            for (T row : rows) {
                String key = keyFn.key(row);
                Group<T> g = groups.get(key);
                if (g == null) {
                    g = new Group<>(labelFn.key(key));
                    groups.put(key, g);
                }
                g.items.add(row);
            }
        }
        return new ArrayList<>(groups.values());
    }

    /** Zone order on the Devices page: main entrance first, then lobby, then the rest A-Z. */
    public static int zoneRank(String zone) {
        if ("MAIN_ENTRANCE".equalsIgnoreCase(zone)) return 0;
        if ("LOBBY".equalsIgnoreCase(zone)) return 1;
        return 2;
    }

    /** Distance bar width in % (0–300 cm scale) and the "close" flag (< 50 cm). */
    public static int distancePercent(Double cm) {
        if (cm == null || cm <= 0) {
            return 0;
        }
        return (int) Math.min(100, Math.round(cm * 100.0 / 300.0));
    }

    public static boolean isClose(Double cm) {
        return cm != null && cm < 50.0;
    }
}
