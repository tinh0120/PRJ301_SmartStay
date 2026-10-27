package util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure helpers for the public Rooms page filters (Task R4): slot labels, the
 * consecutive-slot rule, sort keys and URL state. No servlet or database code.
 */
public final class RoomFilter {

    /** Standard slot codes in day order (sort order 1..4). */
    public static final List<String> SLOT_CODES = Collections.unmodifiableList(Arrays.asList("S1", "S2", "S3", "S4"));
    private static final String[] SLOT_LABELS = {"Morning", "Afternoon", "Evening", "Overnight"};

    /** Sort keys offered on the page. */
    public static final String SORT_ROOM = "room";
    public static final String SORT_PRICE = "price";
    public static final String SORT_PRICE_DESC = "price_desc";
    public static final String SORT_CAPACITY = "capacity";
    private static final List<String> SORTS = Arrays.asList(SORT_ROOM, SORT_PRICE, SORT_PRICE_DESC, SORT_CAPACITY);

    private RoomFilter() {
    }

    /** "S1" -> "Morning"; unknown codes are returned unchanged. */
    public static String slotLabel(String code) {
        int i = (code == null) ? -1 : SLOT_CODES.indexOf(code.trim().toUpperCase());
        return (i < 0) ? code : SLOT_LABELS[i];
    }

    /** "Morning, Afternoon" for a list of codes. */
    public static String slotLabels(Collection<String> codes) {
        List<String> out = new ArrayList<>();
        if (codes != null) {
            for (String c : codes) {
                out.add(slotLabel(c));
            }
        }
        return String.join(", ", out);
    }

    /**
     * Parses slot codes from repeated and/or comma-separated values, keeps only known codes,
     * removes duplicates and returns them in day order.
     */
    public static List<String> parseSlots(String[] values) {
        boolean[] seen = new boolean[SLOT_CODES.size()];
        if (values != null) {
            for (String v : values) {
                if (v == null) {
                    continue;
                }
                for (String part : v.split(",")) {
                    int i = SLOT_CODES.indexOf(part.trim().toUpperCase());
                    if (i >= 0) {
                        seen[i] = true;
                    }
                }
            }
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < seen.length; i++) {
            if (seen[i]) {
                out.add(SLOT_CODES.get(i));
            }
        }
        return out;
    }

    /** True when the (day-ordered) codes are next to each other, e.g. S2+S3; empty or single is consecutive. */
    public static boolean isConsecutive(List<String> orderedCodes) {
        for (int i = 1; i < orderedCodes.size(); i++) {
            if (SLOT_CODES.indexOf(orderedCodes.get(i)) != SLOT_CODES.indexOf(orderedCodes.get(i - 1)) + 1) {
                return false;
            }
        }
        return true;
    }

    /** Keeps the first run of adjacent slots (same rule as slot-matrix.js keepFirstContiguousGroup). */
    public static List<String> firstConsecutiveRun(List<String> orderedCodes) {
        List<String> out = new ArrayList<>();
        for (String c : orderedCodes) {
            if (out.isEmpty() || SLOT_CODES.indexOf(c) == SLOT_CODES.indexOf(out.get(out.size() - 1)) + 1) {
                out.add(c);
            } else {
                break;
            }
        }
        return out;
    }

    /** Normalizes the sort parameter; accepts the legacy sort=price&dir=DESC form. */
    public static String normalizeSort(String sort, String dir) {
        String s = (sort == null) ? "" : sort.trim().toLowerCase();
        if (SORT_PRICE.equals(s) && "DESC".equalsIgnoreCase(dir)) {
            return SORT_PRICE_DESC;
        }
        return SORTS.contains(s) ? s : SORT_ROOM;
    }

    /** Human label of a sort key. */
    public static String sortLabel(String sort) {
        if (SORT_PRICE.equals(sort)) {
            return "Price low to high";
        }
        if (SORT_PRICE_DESC.equals(sort)) {
            return "Price high to low";
        }
        if (SORT_CAPACITY.equals(sort)) {
            return "Capacity";
        }
        return "Room A to Z";
    }

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    /**
     * Compact week label (Task R5): "25 – 31 Oct", "29 Oct – 4 Nov", "29 Dec 2026 – 4 Jan 2027".
     */
    public static String weekRangeLabel(java.time.LocalDate start, java.time.LocalDate end) {
        if (start == null || end == null) {
            return "";
        }
        String endText = end.getDayOfMonth() + " " + MONTHS[end.getMonthValue() - 1];
        if (start.getYear() != end.getYear()) {
            return start.getDayOfMonth() + " " + MONTHS[start.getMonthValue() - 1] + " " + start.getYear()
                    + " – " + endText + " " + end.getYear();
        }
        if (start.getMonthValue() != end.getMonthValue()) {
            return start.getDayOfMonth() + " " + MONTHS[start.getMonthValue() - 1] + " – " + endText;
        }
        return start.getDayOfMonth() + " – " + endText;
    }

    /** Parses a positive integer filter, or null when blank/invalid/out of [min, max]. */
    public static Integer parseIntFilter(String value, int min, int max) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            int v = Integer.parseInt(value.trim());
            return (v < min || v > max) ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Immutable URL state of the Rooms page. Default values are left out of the query string
     * so links stay short: /rooms?date=2026-10-25&slots=S1,S2&minCapacity=3&floor=1&sort=price&available=1
     */
    public static final class State {
        private final String date;
        private final String weekStart;
        private final List<String> slots;
        private final Integer minCapacity;
        private final Integer floor;
        private final String sort;
        private final boolean availableOnly;

        public State(String date, String weekStart, List<String> slots, Integer minCapacity, Integer floor,
                     String sort, boolean availableOnly) {
            this.date = date;
            this.weekStart = weekStart;
            this.slots = (slots == null) ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<>(slots));
            this.minCapacity = minCapacity;
            this.floor = floor;
            this.sort = (sort == null) ? SORT_ROOM : sort;
            this.availableOnly = availableOnly;
        }

        public String getDate() { return date; }
        public String getWeekStart() { return weekStart; }
        public List<String> getSlots() { return slots; }
        public String getSlotsCsv() { return String.join(",", slots); }
        public Integer getMinCapacity() { return minCapacity; }
        public Integer getFloor() { return floor; }
        public String getSort() { return sort; }
        public boolean isAvailableOnly() { return availableOnly; }

        public State withDate(String d, String ws) {
            return new State(d, ws, slots, minCapacity, floor, sort, availableOnly);
        }

        public State withSlots(List<String> s) {
            return new State(date, weekStart, s, minCapacity, floor, sort, availableOnly);
        }

        public State withMinCapacity(Integer c) {
            return new State(date, weekStart, slots, c, floor, sort, availableOnly);
        }

        public State withFloor(Integer f) {
            return new State(date, weekStart, slots, minCapacity, f, sort, availableOnly);
        }

        public State withAvailableOnly(boolean a) {
            return new State(date, weekStart, slots, minCapacity, floor, sort, a);
        }

        /** Query string (no leading "?"), default values omitted. */
        public String toQuery() {
            Map<String, String> q = new LinkedHashMap<>();
            if (date != null && !date.isEmpty()) {
                q.put("date", date);
            }
            if (weekStart != null && !weekStart.isEmpty() && !weekStart.equals(date)) {
                q.put("weekStart", weekStart);
            }
            if (!slots.isEmpty()) {
                q.put("slots", getSlotsCsv());
            }
            if (minCapacity != null) {
                q.put("minCapacity", String.valueOf(minCapacity));
            }
            if (floor != null) {
                q.put("floor", String.valueOf(floor));
            }
            if (!SORT_ROOM.equals(sort)) {
                q.put("sort", sort);
            }
            if (availableOnly) {
                q.put("available", "1");
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : q.entrySet()) {
                if (sb.length() > 0) {
                    sb.append('&');
                }
                sb.append(e.getKey()).append('=').append(encode(e.getValue()));
            }
            return sb.toString();
        }

        /** Full URL for a path, e.g. url(ctx + "/rooms"). */
        public String url(String path) {
            String q = toQuery();
            return q.isEmpty() ? path : path + "?" + q;
        }
    }

    private static String encode(String v) {
        try {
            // Keep "S1,S2" readable in the address bar.
            return URLEncoder.encode(v, "UTF-8").replace("%2C", ",");
        } catch (UnsupportedEncodingException e) {
            return v;
        }
    }
}
