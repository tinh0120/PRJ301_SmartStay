package util;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Server-side form validation helper.
 * Collects field errors preserving insertion order, keeping only the first error per field.
 */
public class Validator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)\\d{9,10}$");
    private static final Pattern PASSWORD_LETTER_PATTERN = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern PASSWORD_DIGIT_PATTERN = Pattern.compile(".*\\d.*");

    private final Map<String, String> errors = new LinkedHashMap<>();

    public Validator() {
    }

    public static String trim(String s) {
        return (s == null) ? "" : s.trim();
    }

    public static Integer parseIntOrNull(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Validator addError(String field, String message) {
        if (field != null && !errors.containsKey(field)) {
            errors.put(field, message != null ? message : "");
        }
        return this;
    }

    public Validator required(String field, String value, String label) {
        if (errors.containsKey(field)) {
            return this;
        }
        if (value == null || value.trim().isEmpty()) {
            errors.put(field, label + " is required.");
        }
        return this;
    }

    public Validator maxLength(String field, String value, int max, String label) {
        if (errors.containsKey(field)) {
            return this;
        }
        if (value != null && value.length() > max) {
            errors.put(field, label + " must be at most " + max + " characters.");
        }
        return this;
    }

    public Validator email(String field, String value) {
        if (errors.containsKey(field)) {
            return this;
        }
        if (value == null || value.trim().isEmpty()) {
            return this;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 255 || !EMAIL_PATTERN.matcher(trimmed).matches()) {
            errors.put(field, "Please enter a valid email address.");
        }
        return this;
    }

    public Validator phone(String field, String value) {
        if (errors.containsKey(field)) {
            return this;
        }
        if (value == null || value.trim().isEmpty()) {
            return this;
        }
        String trimmed = value.trim();
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            errors.put(field, "Please enter a valid phone number (e.g. 0901234567 or +84901234567).");
        }
        return this;
    }

    public Validator password(String field, String value) {
        if (errors.containsKey(field)) {
            return this;
        }
        if (value == null || value.trim().isEmpty()) {
            return this;
        }
        if (value.length() < 8 || !PASSWORD_LETTER_PATTERN.matcher(value).matches() || !PASSWORD_DIGIT_PATTERN.matcher(value).matches()) {
            errors.put(field, "Password must be at least 8 characters and contain both letters and numbers.");
        }
        return this;
    }

    public Validator matches(String field, String a, String b, String message) {
        if (errors.containsKey(field)) {
            return this;
        }
        if (a == null && b == null) {
            return this;
        }
        if (a == null || !a.equals(b)) {
            errors.put(field, message);
        }
        return this;
    }

    public Validator positiveInt(String field, String value, String label) {
        if (errors.containsKey(field)) {
            return this;
        }
        if (value == null || value.trim().isEmpty()) {
            return this;
        }
        try {
            int num = Integer.parseInt(value.trim());
            if (num <= 0) {
                errors.put(field, label + " must be a whole number greater than 0.");
            }
        } catch (NumberFormatException e) {
            errors.put(field, label + " must be a whole number greater than 0.");
        }
        return this;
    }

    public Validator intRange(String field, String value, int min, int max, String label) {
        if (errors.containsKey(field)) {
            return this;
        }
        if (value == null || value.trim().isEmpty()) {
            return this;
        }
        try {
            int num = Integer.parseInt(value.trim());
            if (num < min || num > max) {
                errors.put(field, label + " must be between " + min + " and " + max + ".");
            }
        } catch (NumberFormatException e) {
            errors.put(field, label + " must be between " + min + " and " + max + ".");
        }
        return this;
    }

    public Validator positiveDecimal(String field, String value, String label) {
        if (errors.containsKey(field)) {
            return this;
        }
        if (value == null || value.trim().isEmpty()) {
            return this;
        }
        try {
            BigDecimal bd = new BigDecimal(value.trim());
            if (bd.compareTo(BigDecimal.ZERO) <= 0) {
                errors.put(field, label + " must be a number greater than 0.");
            }
        } catch (NumberFormatException e) {
            errors.put(field, label + " must be a number greater than 0.");
        }
        return this;
    }

    public Validator pattern(String field, String value, String regex, String message) {
        if (errors.containsKey(field)) {
            return this;
        }
        if (value == null || value.trim().isEmpty()) {
            return this;
        }
        if (!Pattern.matches(regex, value.trim())) {
            errors.put(field, message);
        }
        return this;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public Map<String, String> getErrors() {
        return Collections.unmodifiableMap(errors);
    }
}
