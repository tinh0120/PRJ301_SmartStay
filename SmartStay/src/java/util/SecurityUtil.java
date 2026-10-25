package util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Unified security utility combining CSRF protection and AES-256-GCM / HMAC-SHA256 cryptography.
 */
public final class SecurityUtil {

    private static final Logger LOGGER = Logger.getLogger(SecurityUtil.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();

    // =========================================================================
    // CSRF PROTECTION
    // =========================================================================

    public static final String CSRF_PARAM = "_csrf";
    public static final String CSRF_SESSION_ATTR = "_csrf";

    /**
     * Retrieves or generates a unique session-scoped CSRF token.
     */
    public static String tokenForSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        synchronized (session) {
            String token = (String) session.getAttribute(CSRF_SESSION_ATTR);
            if (token == null || token.trim().isEmpty()) {
                token = UUID.randomUUID().toString();
                session.setAttribute(CSRF_SESSION_ATTR, token);
            }
            return token;
        }
    }

    /**
     * Extracts the submitted CSRF token from request parameters or headers.
     */
    public static String tokenFromRequest(HttpServletRequest req) {
        if (req == null) {
            return null;
        }
        String token = req.getParameter(CSRF_PARAM);
        if (token == null || token.trim().isEmpty()) {
            token = req.getHeader("X-CSRF-TOKEN");
        }
        if ((token == null || token.trim().isEmpty()) && req.getQueryString() != null) {
            for (String param : req.getQueryString().split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2 && CSRF_PARAM.equals(pair[0])) {
                    try {
                        token = java.net.URLDecoder.decode(pair[1], "UTF-8");
                    } catch (Exception ignored) {
                        token = pair[1];
                    }
                    break;
                }
            }
        }
        return token != null ? token.trim() : null;
    }

    /**
     * Validates whether the incoming request contains a CSRF token matching the session token.
     */
    public static boolean valid(HttpServletRequest req) {
        if (req == null) {
            return false;
        }
        HttpSession session = req.getSession(false);
        if (session == null) {
            return false;
        }
        String sessionToken = (String) session.getAttribute(CSRF_SESSION_ATTR);
        String reqToken = tokenFromRequest(req);
        return sessionToken != null && sessionToken.equals(reqToken);
    }

    // =========================================================================
    // AES-256-GCM & HMAC-SHA256 CRYPTOGRAPHY
    // =========================================================================

    private static final int GCM_IV_LENGTH = 12; // 96-bit IV
    private static final int GCM_TAG_LENGTH = 128; // 128-bit authentication tag
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private static final byte[] AES_KEY;
    private static final byte[] HMAC_KEY;

    static {
        Properties props = new Properties();
        boolean loaded = false;

        // 1. Try loading from secrets.properties on classpath
        try (InputStream in = SecurityUtil.class.getResourceAsStream("/secrets.properties")) {
            if (in != null) {
                props.load(in);
                loaded = true;
            }
        } catch (IOException ignored) {
        }

        // 2. Fallback to secrets.demo.properties
        if (!loaded || getProp(props, "pin.aes.key.base64", "pin.aes.key") == null) {
            try (InputStream in = SecurityUtil.class.getResourceAsStream("/secrets.demo.properties")) {
                if (in != null) {
                    props.load(in);
                    LOGGER.warning("Using fallback demo cryptographic secrets from secrets.demo.properties. DO NOT use in production!");
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to load fallback demo secrets", e);
            }
        }

        String aesBase64 = getProp(props, "pin.aes.key.base64", "pin.aes.key");
        String hmacBase64 = getProp(props, "pin.hmac.key.base64", "pin.hmac.key");

        if (aesBase64 == null || hmacBase64 == null) {
            LOGGER.severe("Cryptographic keys missing from configuration! Generating volatile temporary keys.");
            AES_KEY = new byte[32];
            HMAC_KEY = new byte[32];
            RANDOM.nextBytes(AES_KEY);
            RANDOM.nextBytes(HMAC_KEY);
        } else {
            AES_KEY = Base64.getDecoder().decode(aesBase64.trim());
            HMAC_KEY = Base64.getDecoder().decode(hmacBase64.trim());
        }
    }

    private SecurityUtil() {
    }

    private static String getProp(Properties props, String key1, String key2) {
        String val = props.getProperty(key1);
        if (val == null || val.trim().isEmpty()) {
            val = props.getProperty(key2);
        }
        return (val != null && !val.trim().isEmpty()) ? val.trim() : null;
    }

    private static void validatePinFormat(String pin) {
        if (pin == null || !pin.matches("^\\d{6}$")) {
            throw new IllegalArgumentException("PIN must be exactly 6 digits.");
        }
    }

    /**
     * Encrypts a 6-digit PIN using AES/GCM/NoPadding.
     * Output format: 12-byte IV || ciphertext + 16-byte GCM tag.
     */
    public static byte[] encryptPin(String pin) {
        validatePinFormat(pin);
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] plaintext = pin.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] result = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, result, GCM_IV_LENGTH, ciphertext.length);
            return result;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to encrypt PIN", e);
        }
    }

    /**
     * Decrypts ciphertext back to the original 6-digit PIN string.
     */
    public static String decryptPin(byte[] encrypted) {
        if (encrypted == null || encrypted.length <= GCM_IV_LENGTH + 16) {
            throw new IllegalArgumentException("Invalid encrypted PIN data.");
        }
        try {
            byte[] iv = Arrays.copyOfRange(encrypted, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(encrypted, GCM_IV_LENGTH, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to decrypt PIN", e);
        }
    }

    /**
     * Computes the 32-byte HMAC-SHA256 hash for a 6-digit PIN.
     */
    public static byte[] hmacPin(String pin) {
        validatePinFormat(pin);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(HMAC_KEY, HMAC_ALGORITHM);
            mac.init(keySpec);
            return mac.doFinal(pin.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to compute PIN HMAC", e);
        }
    }

    /**
     * Constant-time comparison between a candidate PIN and an expected HMAC.
     */
    public static boolean verifyPin(String candidatePin, byte[] expectedHmac) {
        if (candidatePin == null || expectedHmac == null) {
            return false;
        }
        try {
            byte[] actual = hmacPin(candidatePin);
            return MessageDigest.isEqual(actual, expectedHmac);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
