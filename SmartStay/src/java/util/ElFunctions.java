package util;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Custom JSP EL helper functions and room media/display metadata utilities.
 */
public final class ElFunctions {

    // =========================================================================
    // ROOM MEDIA & AMENITY METADATA (Merged from RoomMediaHelper)
    // =========================================================================

    private static final Map<String, String> AMENITY_ICONS;
    private static final Map<String, RoomMeta> ROOM_METAS;

    static {
        Map<String, String> am = new HashMap<>();
        am.put("kitchen", "kitchen.png");
        am.put("bathtub", "bathtub.png");
        am.put("bean bag chair", "bean-bag-chair.png");
        am.put("king bed", "king-bed.png");
        am.put("full-length mirror", "full-length-mirror.png");
        am.put("full length mirror", "full-length-mirror.png");
        am.put("projector", "projector.png");
        am.put("mini game console", "mini-game-console.png");
        am.put("netflix", "netflix.png");
        am.put("sofa bed", "sofa-bed.png");
        am.put("private bathroom", "private-bathroom.png");
        AMENITY_ICONS = Collections.unmodifiableMap(am);

        Map<String, RoomMeta> rm = new HashMap<>();
        rm.put("A101", new RoomMeta("butter-beige", "Butter Beige", ".jpeg", ".jpeg"));
        rm.put("A102", new RoomMeta("vanilla-zen", "Vanilla Zen", ".jpeg", ".jpeg"));
        rm.put("A103", new RoomMeta("fresh-tale", "Fresh Tale", ".jpeg", ".jpeg"));
        rm.put("A104", new RoomMeta("power-rush", "Power Rush", ".jpeg", ".jpeg"));
        rm.put("A105", new RoomMeta("sunny-love", "Sunny Love", ".jpeg", ".jpg"));
        rm.put("A106", new RoomMeta("warm-house", "Warm House", ".png", ".jpg"));
        ROOM_METAS = Collections.unmodifiableMap(rm);
    }

    public static class AmenityInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String label;
        private final String iconFile;
        private final boolean hasIcon;

        public AmenityInfo(String label, String iconFile, boolean hasIcon) {
            this.label = label;
            this.iconFile = iconFile;
            this.hasIcon = hasIcon;
        }

        public String getLabel() {
            return label;
        }

        public String getIconFile() {
            return iconFile;
        }

        public boolean isHasIcon() {
            return hasIcon;
        }

        public boolean getHasIcon() {
            return hasIcon;
        }
    }

    public static class GalleryImage implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String url;
        private final String alt;
        private final boolean main;
        private final int index;

        public GalleryImage(String url, String alt, boolean main, int index) {
            this.url = url;
            this.alt = alt;
            this.main = main;
            this.index = index;
        }

        public String getUrl() {
            return url;
        }

        public String getAlt() {
            return alt;
        }

        public boolean isMain() {
            return main;
        }

        public boolean getMain() {
            return main;
        }

        public int getIndex() {
            return index;
        }
    }

    private static class RoomMeta {
        final String slug;
        final String displayName;
        final String mainExt;
        final String detailExt;

        RoomMeta(String slug, String displayName, String mainExt, String detailExt) {
            this.slug = slug;
            this.displayName = displayName;
            this.mainExt = mainExt;
            this.detailExt = detailExt;
        }
    }

    /**
     * Parses a comma-separated amenity string into AmenityInfo instances.
     */
    public static List<AmenityInfo> parseAmenities(String amenitiesCsv) {
        if (amenitiesCsv == null || amenitiesCsv.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] tokens = amenitiesCsv.split(",");
        List<AmenityInfo> list = new ArrayList<>();
        for (String tok : tokens) {
            String label = tok.trim();
            if (label.isEmpty()) {
                continue;
            }
            String key = label.toLowerCase();
            String iconFile = AMENITY_ICONS.get(key);
            boolean hasIcon = (iconFile != null);
            list.add(new AmenityInfo(label, iconFile, hasIcon));
        }
        return list;
    }

    /**
     * Returns a compact subset of amenities up to the given limit.
     */
    public static List<AmenityInfo> compactAmenities(String amenitiesCsv, int limit) {
        List<AmenityInfo> all = parseAmenities(amenitiesCsv);
        if (limit <= 0 || all.size() <= limit) {
            return all;
        }
        return all.subList(0, limit);
    }

    /**
     * Returns standard display name for a known room number, or fallback.
     */
    public static String getDisplayName(String roomNumber) {
        if (roomNumber == null) {
            return "Room";
        }
        RoomMeta meta = ROOM_METAS.get(roomNumber.trim().toUpperCase());
        if (meta != null) {
            return meta.displayName;
        }
        return "Room " + roomNumber.trim();
    }

    /**
     * Resolves the gallery images (main view + 4 detail views) for a room.
     */
    public static List<GalleryImage> getGalleryImages(String roomNumber, String displayName) {
        if (roomNumber == null) {
            return Collections.emptyList();
        }
        RoomMeta meta = ROOM_METAS.get(roomNumber.trim().toUpperCase());
        if (meta == null) {
            return Collections.emptyList();
        }
        String name = (displayName != null && !displayName.trim().isEmpty()) ? displayName : meta.displayName;
        List<GalleryImage> images = new ArrayList<>(5);
        String mainUrl = "assets/images/rooms/" + meta.slug + "/main" + meta.mainExt;
        String mainAlt = name + " main view";
        images.add(new GalleryImage(mainUrl, mainAlt, true, 0));
        for (int i = 1; i <= 4; i++) {
            String url = "assets/images/rooms/" + meta.slug + "/detail-" + i + meta.detailExt;
            String alt = name + " detail view " + i;
            images.add(new GalleryImage(url, alt, false, i));
        }
        return images;
    }

    private ElFunctions() {
    }

    // =========================================================================
    // JSP EL FORMATTING FUNCTIONS
    // =========================================================================

    /**
     * HTML-escapes special characters (&amp;, &lt;, &gt;, &quot;, &#39;).
     * Returns empty string if input is null.
     */
    public static String esc(Object x) {
        if (x == null) {
            return "";
        }
        String s = x.toString();
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * Formats numeric amounts as Vietnamese Dong currency string (e.g. "259,000 VND").
     * Rounds HALF_UP to 0 decimal places. Returns empty string if null or invalid.
     */
    public static String money(Object x) {
        if (x == null) {
            return "";
        }
        BigDecimal bd;
        if (x instanceof BigDecimal) {
            bd = (BigDecimal) x;
        } else if (x instanceof Number) {
            bd = new BigDecimal(x.toString());
        } else {
            String str = x.toString().trim();
            if (str.isEmpty()) {
                return "";
            }
            try {
                bd = new BigDecimal(str);
            } catch (NumberFormatException e) {
                return "";
            }
        }
        bd = bd.setScale(0, RoundingMode.HALF_UP);
        DecimalFormat df = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(bd) + " VND";
    }

    /**
     * Formats Date, Timestamp, or LocalDateTime as unified "dd/MM/yyyy HH:mm".
     */
    public static String datetime(Object x) {
        if (x == null) {
            return "";
        }
        if (x instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            return sdf.format((Date) x);
        }
        if (x instanceof LocalDateTime) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return ((LocalDateTime) x).format(dtf);
        }
        if (x instanceof ZonedDateTime) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return ((ZonedDateTime) x).format(dtf);
        }
        if (x instanceof LocalDate) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return ((LocalDate) x).format(dtf);
        }
        String s = x.toString().trim();
        if (s.isEmpty()) {
            return "";
        }
        if (s.length() >= 16 && s.charAt(4) == '-' && s.charAt(7) == '-') {
            try {
                String clean = s.replace(' ', 'T');
                if (clean.length() == 16) {
                    clean += ":00";
                }
                LocalDateTime ldt = LocalDateTime.parse(clean.substring(0, 19));
                return ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            } catch (Exception ignored) {
            }
        }
        return s;
    }

    /**
     * Formats Date or LocalDateTime with weekday: "EEE, dd/MM/yyyy HH:mm" (e.g. "Fri, 23/10/2026 16:30").
     */
    public static String weekdayDatetime(Object x) {
        if (x == null) {
            return "";
        }
        if (x instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd/MM/yyyy HH:mm", Locale.ENGLISH);
            return sdf.format((Date) x);
        }
        if (x instanceof LocalDateTime) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy HH:mm", Locale.ENGLISH);
            return ((LocalDateTime) x).format(dtf);
        }
        if (x instanceof ZonedDateTime) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy HH:mm", Locale.ENGLISH);
            return ((ZonedDateTime) x).format(dtf);
        }
        return "";
    }

    /**
     * Formats time only as "HH:mm".
     */
    public static String time(Object x) {
        if (x == null) {
            return "";
        }
        if (x instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            return sdf.format((Date) x);
        }
        if (x instanceof LocalDateTime) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
            return ((LocalDateTime) x).format(dtf);
        }
        if (x instanceof ZonedDateTime) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
            return ((ZonedDateTime) x).format(dtf);
        }
        return "";
    }

    /**
     * Computes human-readable duration between start and end (e.g. "3 hours", "1 hour", "45 minutes").
     */
    public static String duration(Object start, Object end) {
        if (start == null || end == null) {
            return "";
        }
        LocalDateTime startLdt = toLocalDateTime(start);
        LocalDateTime endLdt = toLocalDateTime(end);
        if (startLdt == null || endLdt == null) {
            return "";
        }
        long minutes = java.time.temporal.ChronoUnit.MINUTES.between(startLdt, endLdt);
        if (minutes < 0) {
            return "";
        }
        if (minutes < 60) {
            return minutes + " minute" + (minutes != 1 ? "s" : "");
        }
        long hours = minutes / 60;
        long remMins = minutes % 60;
        if (remMins == 0) {
            return hours + " hour" + (hours != 1 ? "s" : "");
        }
        return hours + " hour" + (hours != 1 ? "s " : " ") + remMins + " mins";
    }

    private static LocalDateTime toLocalDateTime(Object x) {
        if (x instanceof LocalDateTime) {
            return (LocalDateTime) x;
        }
        if (x instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) x).toLocalDateTime();
        }
        if (x instanceof Date) {
            return ((Date) x).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        }
        if (x instanceof ZonedDateTime) {
            return ((ZonedDateTime) x).toLocalDateTime();
        }
        return null;
    }

    /**
     * Converts booking/payment status string to human-readable title.
     */
    public static String humanStatus(Object status) {
        if (status == null) {
            return "";
        }
        String s = status.toString().trim();
        if (s.isEmpty()) {
            return "";
        }
        switch (s) {
            case "PENDING_PAYMENT":
                return "Pending payment";
            case "CONFIRMED":
                return "Confirmed";
            case "CHECKED_IN":
                return "Checked in";
            case "CHECKED_OUT":
            case "COMPLETED":
                return "Completed";
            case "CANCELLED":
                return "Cancelled";
            case "EXPIRED":
                return "Expired";
            case "HELD":
                return "Held";
            case "RELEASED":
                return "Released";
            case "ACTIVE":
                return "Active";
            case "PAID":
                return "Paid";
            default:
                String formatted = s.toLowerCase().replace('_', ' ');
                return Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
        }
    }

    /**
     * Maps catalog sort key and direction to human-readable dropdown label.
     */
    public static String sortLabel(Object sort, Object dir) {
        String s = (sort != null) ? sort.toString() : "room";
        String d = (dir != null) ? dir.toString() : "ASC";
        if ("room".equalsIgnoreCase(s)) {
            return "DESC".equalsIgnoreCase(d) ? "Room Z\u2013A" : "Room A\u2013Z";
        }
        if ("price".equalsIgnoreCase(s)) {
            return "DESC".equalsIgnoreCase(d) ? "Price (Highest first)" : "Price (Lowest first)";
        }
        if ("floor".equalsIgnoreCase(s)) {
            return "DESC".equalsIgnoreCase(d) ? "Floor (2 \u2192 1)" : "Floor (1 \u2192 2)";
        }
        if ("capacity".equalsIgnoreCase(s)) {
            return "DESC".equalsIgnoreCase(d) ? "Capacity (Highest first)" : "Capacity (Lowest first)";
        }
        return "Room A\u2013Z";
    }

    /**
     * Formats Date, LocalDate, or LocalDateTime as "dd/MM/yyyy".
     */
    public static String date(Object x) {
        if (x == null) {
            return "";
        }
        if (x instanceof java.sql.Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            return sdf.format((Date) x);
        }
        if (x instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            return sdf.format((Date) x);
        }
        if (x instanceof LocalDate) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return ((LocalDate) x).format(dtf);
        }
        if (x instanceof LocalDateTime) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return ((LocalDateTime) x).format(dtf);
        }
        String s = x.toString().trim();
        if (s.isEmpty()) {
            return "";
        }
        if (s.length() >= 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
            try {
                LocalDate ld = LocalDate.parse(s.substring(0, 10));
                return ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception ignored) {
            }
        }
        return s;
    }

    /**
     * URL-encodes an object value using UTF-8 for safe inclusion in query strings.
     */
    public static String url(Object x) {
        if (x == null) {
            return "";
        }
        try {
            return URLEncoder.encode(x.toString(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    /**
     * Parses comma-separated amenities into AmenityInfo instances with icon paths.
     */
    public static List<AmenityInfo> amenities(Object x) {
        if (x == null) {
            return Collections.emptyList();
        }
        return parseAmenities(x.toString());
    }

    /**
     * Returns a compact subset of amenities up to limit.
     */
    public static List<AmenityInfo> compactAmenities(Object x, Object limitObj) {
        if (x == null) {
            return Collections.emptyList();
        }
        int limit = 3;
        if (limitObj instanceof Number) {
            limit = ((Number) limitObj).intValue();
        } else if (limitObj != null) {
            try {
                limit = Integer.parseInt(limitObj.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return compactAmenities(x.toString(), limit);
    }

    /**
     * Returns 4 gallery thumbnail images for a room.
     */
    public static List<GalleryImage> gallery(Object roomNumber, Object displayName) {
        String num = roomNumber != null ? roomNumber.toString() : null;
        String name = displayName != null ? displayName.toString() : null;
        return getGalleryImages(num, name);
    }

    /**
     * Maps room number code to public display name.
     */
    public static String roomName(Object roomNumber) {
        if (roomNumber == null) {
            return "Room";
        }
        return getDisplayName(roomNumber.toString());
    }

    /**
     * Formats a timestamp, Date, or LocalDateTime as relative time (e.g. "3 min ago", "2 hours ago").
     */
    public static String timeAgo(Object x) {
        if (x == null) {
            return "-";
        }
        LocalDateTime dt = null;
        if (x instanceof LocalDateTime) {
            dt = (LocalDateTime) x;
        } else if (x instanceof java.sql.Timestamp) {
            dt = ((java.sql.Timestamp) x).toLocalDateTime();
        } else if (x instanceof java.util.Date) {
            dt = LocalDateTime.ofInstant(((java.util.Date) x).toInstant(), java.time.ZoneId.systemDefault());
        } else {
            String s = x.toString().trim();
            if (s.isEmpty()) {
                return "-";
            }
            try {
                dt = LocalDateTime.parse(s.replace(" ", "T"));
            } catch (Exception ignored) {
                return s;
            }
        }

        // Log / heartbeat timestamps are REAL time (see docs/time-inventory.md)
        return relativeTime(dt, LocalDateTime.now());
    }

    /**
     * Pure relative-time text used by timeAgo (R10, unit-tested): "just now", "5 min ago",
     * "2 hours ago", "3 days ago", "2 months ago". Future times read "just now".
     */
    public static String relativeTime(LocalDateTime dt, LocalDateTime now) {
        if (dt == null || now == null) {
            return "-";
        }
        java.time.Duration diff = java.time.Duration.between(dt, now);
        long seconds = diff.getSeconds();
        if (seconds < 0 || seconds < 60) {
            return "just now";
        }
        long minutes = seconds / 60;
        if (minutes == 1) {
            return "1 min ago";
        }
        if (minutes < 60) {
            return minutes + " min ago";
        }
        long hours = minutes / 60;
        if (hours == 1) {
            return "1 hour ago";
        }
        if (hours < 24) {
            return hours + " hours ago";
        }
        long days = hours / 24;
        if (days == 1) {
            return "1 day ago";
        }
        if (days < 30) {
            return days + " days ago";
        }
        long months = days / 30;
        if (months == 1) {
            return "1 month ago";
        }
        return months + " months ago";
    }

    // =========================================================================
    // CANCELLATION POLICY (Task R3)
    // =========================================================================

    /**
     * 48-hour cancellation info for a Booking or BookingDetail, using the virtual clock:
     * ${e:cancellationInfo(b).refundable}, .deadline, .outcome, .nothingPaid, .known.
     */
    public static CancellationPolicy.Info cancellationInfo(Object booking) {
        model.Booking b = null;
        if (booking instanceof model.Booking) {
            b = (model.Booking) booking;
        } else if (booking instanceof model.BookingDetail) {
            b = ((model.BookingDetail) booking).getBooking();
        }
        if (b == null) {
            return CancellationPolicy.info(null, null, ClockService.now());
        }
        return CancellationPolicy.info(b.getStatus(), b.getCheckinAt(), ClockService.now());
    }

    /** Hours of free cancellation before the first slot (AppConstants.CANCEL_FREE_HOURS). */
    public static int cancelFreeHours() {
        return AppConstants.CANCEL_FREE_HOURS;
    }

    // =========================================================================
    // GUEST BOOKING DISPLAY (Task R5)
    // =========================================================================

    /** "16:30 → 09:00 (+1 day)" from two date-times. */
    public static String stayTimes(Object start, Object end) {
        return BookingState.stayTimes(toLocalDateTime(start), toLocalDateTime(end));
    }

    /** "25/10 · 16:30 → 09:00 (+1 day)" from two date-times. */
    public static String stayLine(Object start, Object end) {
        return BookingState.stayLine(toLocalDateTime(start), toLocalDateTime(end));
    }

    /** "Evening + Overnight" from "S3+S4" or from a list of BookingDetail slot rows. */
    public static String slotNames(Object slots) {
        if (slots instanceof String) {
            return BookingState.slotNames((String) slots);
        }
        if (slots instanceof Iterable) {
            List<String> names = new ArrayList<>();
            for (Object o : (Iterable<?>) slots) {
                if (o instanceof model.BookingDetail.BookingSlotRow) {
                    names.add(((model.BookingDetail.BookingSlotRow) o).getSlotName());
                } else if (o instanceof model.RoomSlot) {
                    names.add(((model.RoomSlot) o).getSlotName());
                } else if (o != null) {
                    names.add(String.valueOf(o));
                }
            }
            return BookingState.joinNames(names);
        }
        return "";
    }

    /** ONE admin badge (booking + payment) for a Booking: ${e:adminState(b).label} (R7). */
    public static BookingState adminState(Object booking) {
        if (booking instanceof model.BookingDetail) {
            booking = ((model.BookingDetail) booking).getBooking();
        }
        if (booking instanceof model.Booking) {
            model.Booking b = (model.Booking) booking;
            return BookingState.admin(b.getStatus(), b.getPaymentStatus(), b.getExpireReason());
        }
        return BookingState.admin(null, null, null);
    }

    /** "26/10 · Morning + Afternoon · 09:30 → 19:30" from check-in, check-out and slot codes (R7). */
    public static String slotLine(Object start, Object end, Object slots) {
        return BookingState.slotLine(toLocalDateTime(start), toLocalDateTime(end), slotNames(slots));
    }

    /** One friendly guest state (label, tone, action) for a Booking: ${e:guestState(b).label}. */
    public static BookingState guestState(Object booking) {
        if (booking instanceof model.BookingDetail) {
            booking = ((model.BookingDetail) booking).getBooking();
        }
        if (booking instanceof model.Booking) {
            model.Booking b = (model.Booking) booking;
            return BookingState.of(b.getStatus(), b.getPaymentStatus());
        }
        return BookingState.of(null, null);
    }

    /** R12: plain-words alert title for EL (one central map: util.AlertTitles). */
    public static String alertTitle(Object alertType) {
        return AlertTitles.title(alertType == null ? null : alertType.toString());
    }
}
