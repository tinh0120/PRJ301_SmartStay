package util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ONE central map from ALERT_LOG.alert_type codes to plain-words titles (Task R6, reused by R10).
 * Raw codes stay in detail views only; lists show these titles.
 */
public final class AlertTitles {

    private static final Map<String, String> TITLES;

    static {
        Map<String, String> m = new HashMap<>();
        m.put("DOOR_OPEN_TOO_LONG", "Door held open too long");
        m.put("DOOR_OPEN_ABNORMAL", "Door opened without unlocking");
        m.put("BOOKING_NO_SHOW", "Guest did not check in");
        m.put("DEVICE_OFFLINE", "Device offline");
        m.put("DEVICE_FAULT", "Device fault");
        m.put("PIN_BRUTE_FORCE", "Too many wrong PIN attempts");
        m.put("PAYMENT_REVIEW_REQUIRED", "Payment needs review");
        m.put("CLEANING_SESSION_EXPIRED", "Cleaning not finished in time");
        m.put("LOBBY_PROXIMITY", "Someone close to the lobby sensor");
        m.put("SUSPICIOUS_MOTION", "Unusual motion detected");
        TITLES = Collections.unmodifiableMap(m);
    }

    private AlertTitles() {
    }

    /** Plain-words title; unknown codes become "Some new type" from SOME_NEW_TYPE. */
    public static String title(String alertType) {
        if (alertType == null || alertType.trim().isEmpty()) {
            return "Alert";
        }
        String key = alertType.trim().toUpperCase();
        String t = TITLES.get(key);
        return (t != null) ? t : prettify(key);
    }

    /** "SOME_NEW_TYPE" -> "Some new type". */
    public static String prettify(String code) {
        String s = code.replace('_', ' ').trim().toLowerCase();
        if (s.isEmpty()) {
            return "Alert";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
