package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Command-line utility for generating key material, encrypting PINs with AES-256-GCM,
 * and computing HMAC-SHA256 digests.
 * <p>
 * Supported modes:
 * <ul>
 *   <li><code>genkeys</code>: Generates and prints fresh Base64-encoded 256-bit AES and HMAC keys.</li>
 *   <li><code>genpin [--aes &lt;b64&gt;] [--hmac &lt;b64&gt;] [--pin &lt;6digits&gt;]</code>: Encrypts a PIN and prints SQL hex literals.</li>
 *   <li><code>decrypt --aes &lt;b64&gt; --data &lt;hex&gt;</code>: Decrypts hex-encoded AES-GCM ciphertext back to plaintext PIN.</li>
 * </ul>
 * <p>
 * Key resolution order when CLI arguments are omitted:
 * 1. <code>secrets.properties</code> (classpath or working directory / src/java)
 * 2. <code>secrets.demo.properties</code> (fallback, logs WARNING "Using DEMO key material")
 * <p>
 * Property keys:
 * <ul>
 *   <li><code>pin.aes.key.base64</code> (256-bit AES key, Base64)</li>
 *   <li><code>pin.hmac.key.base64</code> (256-bit HMAC key, Base64)</li>
 * </ul>
 */
public class PinCryptoTool {

    private static final Logger LOGGER = Logger.getLogger(PinCryptoTool.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String mode = args[0].toLowerCase();
        try {
            switch (mode) {
                case "genkeys":
                    handleGenKeys();
                    break;
                case "genpin":
                    handleGenPin(args);
                    break;
                case "decrypt":
                    handleDecrypt(args);
                    break;
                default:
                    System.err.println("Unknown mode: " + mode);
                    printUsage();
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java tools.PinCryptoTool <mode> [options]");
        System.out.println("Modes:");
        System.out.println("  genkeys");
        System.out.println("  genpin [--aes <b64>] [--hmac <b64>] [--pin <6digits>]");
        System.out.println("  decrypt --aes <b64> --data <hex>");
    }

    private static void handleGenKeys() {
        byte[] aesBytes = new byte[32];
        byte[] hmacBytes = new byte[32];
        RANDOM.nextBytes(aesBytes);
        RANDOM.nextBytes(hmacBytes);

        String aesB64 = Base64.getEncoder().encodeToString(aesBytes);
        String hmacB64 = Base64.getEncoder().encodeToString(hmacBytes);

        System.out.println("pin.aes.key.base64=" + aesB64);
        System.out.println("pin.hmac.key.base64=" + hmacB64);
    }

    private static void handleGenPin(String[] args) throws Exception {
        String aesB64 = getArgValue(args, "--aes");
        String hmacB64 = getArgValue(args, "--hmac");
        String pin = getArgValue(args, "--pin");

        if (aesB64 == null || hmacB64 == null) {
            Properties props = loadKeyProperties();
            if (aesB64 == null) {
                aesB64 = props.getProperty("pin.aes.key.base64");
            }
            if (hmacB64 == null) {
                hmacB64 = props.getProperty("pin.hmac.key.base64");
            }
        }

        if (aesB64 == null || hmacB64 == null) {
            throw new IllegalArgumentException("AES and HMAC keys must be provided via CLI arguments or secrets properties.");
        }

        if (pin == null || pin.trim().isEmpty()) {
            int num = RANDOM.nextInt(1000000);
            pin = String.format("%06d", num);
        }

        byte[] aesKey = Base64.getDecoder().decode(aesB64.trim());
        byte[] hmacKey = Base64.getDecoder().decode(hmacB64.trim());

        byte[] ciphertextWithIv = encryptPin(pin, aesKey);
        byte[] hmac = computeHmac(pin, hmacKey);

        String cipherHex = bytesToHex(ciphertextWithIv);
        String hmacHex = bytesToHex(hmac);

        System.out.println("PIN: " + pin);
        System.out.println("Ciphertext Hex: " + cipherHex);
        System.out.println("HMAC Hex: " + hmacHex);
        System.out.println("SQL: CONVERT(VARBINARY(256), '" + cipherHex + "', 2), CONVERT(VARBINARY(64), '" + hmacHex + "', 2)");
    }

    private static void handleDecrypt(String[] args) throws Exception {
        String aesB64 = getArgValue(args, "--aes");
        String hexData = getArgValue(args, "--data");

        if (aesB64 == null) {
            Properties props = loadKeyProperties();
            aesB64 = props.getProperty("pin.aes.key.base64");
        }

        if (aesB64 == null || hexData == null) {
            throw new IllegalArgumentException("Usage: decrypt --aes <b64> --data <hex>");
        }

        byte[] aesKey = Base64.getDecoder().decode(aesB64.trim());
        byte[] cipherBytes = hexToBytes(hexData.trim());

        String decryptedPin = decryptPin(cipherBytes, aesKey);
        System.out.println("Decrypted PIN: " + decryptedPin);
    }

    public static byte[] encryptPin(String pin, byte[] aesKeyBytes) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(aesKeyBytes, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

        byte[] plaintextBytes = pin.getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext = cipher.doFinal(plaintextBytes);

        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        return combined;
    }

    public static String decryptPin(byte[] combined, byte[] aesKeyBytes) throws Exception {
        if (combined.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Invalid ciphertext length");
        }
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

        int ctLen = combined.length - GCM_IV_LENGTH;
        byte[] ciphertext = new byte[ctLen];
        System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ctLen);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(aesKeyBytes, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        byte[] decrypted = cipher.doFinal(ciphertext);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static byte[] computeHmac(String pin, byte[] hmacKeyBytes) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(hmacKeyBytes, "HmacSHA256");
        mac.init(keySpec);
        return mac.doFinal(pin.getBytes(StandardCharsets.UTF_8));
    }

    private static Properties loadKeyProperties() {
        Properties props = new Properties();

        // 1. Try secrets.properties
        if (tryLoad(props, "secrets.properties")) {
            return props;
        }

        // 2. Try secrets.demo.properties
        if (tryLoad(props, "secrets.demo.properties")) {
            LOGGER.warning("Using DEMO key material");
            return props;
        }

        return props;
    }

    private static boolean tryLoad(Properties props, String fileName) {
        // Try classpath
        InputStream is = PinCryptoTool.class.getClassLoader().getResourceAsStream(fileName);
        if (is != null) {
            try {
                props.load(is);
                is.close();
                return true;
            } catch (Exception ignored) {
            }
        }

        // Try local files
        String[] candidatePaths = {
            fileName,
            "src/java/" + fileName,
            "../src/java/" + fileName,
            "web/WEB-INF/classes/" + fileName
        };
        for (String path : candidatePaths) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    props.load(fis);
                    return true;
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    private static String getArgValue(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equalsIgnoreCase(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
