package util;

/**
 * Small SQL helpers shared by DAOs.
 */
public final class SqlUtil {

    private SqlUtil() {
    }

    /**
     * Builds a SQL Server LIKE pattern "%keyword%" with the wildcard characters [, % and _
     * escaped so user input is matched literally. Blank input returns "%".
     */
    public static String escapeLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return "%";
        }
        String kw = keyword.trim();
        StringBuilder sb = new StringBuilder();
        sb.append("%");
        for (int i = 0; i < kw.length(); i++) {
            char c = kw.charAt(i);
            if (c == '[') {
                sb.append("[[]");
            } else if (c == '%') {
                sb.append("[%]");
            } else if (c == '_') {
                sb.append("[_]");
            } else {
                sb.append(c);
            }
        }
        sb.append("%");
        return sb.toString();
    }

    /**
     * Returns "?, ?, ?" with the given number of placeholders (count must be at least 1).
     */
    public static String placeholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("?");
        }
        return sb.toString();
    }
}
