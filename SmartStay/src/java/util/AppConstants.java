package util;

/**
 * Global application constants replacing the previous SYSTEM_CONFIG database table.
 * All values match the original database seed values.
 */
public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    // --- PIN & Security (Spec 11.2) ---
    /** config_key: PIN_LENGTH */
    public static final int PIN_LENGTH = 6;

    /** config_key: PIN_FAILED_ATTEMPTS_THRESHOLD - 5 wrong PINs triggers keypad lockout */
    public static final int PIN_FAILED_ATTEMPTS_THRESHOLD = 5;

    /** config_key: PIN_FAILED_WINDOW_SECONDS */
    public static final int PIN_FAILED_WINDOW_SECONDS = 300;

    /** config_key: KEYPAD_LOCKOUT_SECONDS - Keypad locked for 5 minutes (300s) */
    public static final int KEYPAD_LOCKOUT_SECONDS = 300;

    /** config_key: ALARM_DURATION_SECONDS - Buzzer sounds for 10 seconds */
    public static final int ALARM_DURATION_SECONDS = 10;

    /** config_key: SERVO_UNLOCK_SECONDS */
    public static final int SERVO_UNLOCK_SECONDS = 5;

    /** config_key: DOOR_OPEN_TIMEOUT_SECONDS - Door-held-open alert after 30 seconds */
    public static final int DOOR_OPEN_TIMEOUT_SECONDS = 30;

    // --- Booking & Slots (Spec 2.1 & 2.2) ---
    /** config_key: BOOKING_HOLD_MINUTES */
    public static final int BOOKING_HOLD_MINUTES = 15;

    /** config_key: NOSHOW_GRACE_MINUTES */
    public static final int NOSHOW_GRACE_MINUTES = 30;

    /** config_key: MAX_ADVANCE_BOOKING_DAYS */
    public static final int MAX_ADVANCE_BOOKING_DAYS = 60;

    /** Free cancellation (100% refund) until this many hours before the first booked slot (Round 2, R3). */
    public static final int CANCEL_FREE_HOURS = 48;

    // --- Multi-Slot Discount Percentages (Spec 2.2) ---
    /** config_key: DISCOUNT_2_SLOTS_PERCENT */
    public static final int DISCOUNT_2_SLOTS_PERCENT = 10;

    /** config_key: DISCOUNT_3_SLOTS_PERCENT */
    public static final int DISCOUNT_3_SLOTS_PERCENT = 20;

    /** config_key: DISCOUNT_4_SLOTS_PERCENT */
    public static final int DISCOUNT_4_SLOTS_PERCENT = 50;

    // --- Payment & Gateways ---
    /** config_key: PAYMENT_LINK_TTL_SECONDS */
    public static final int PAYMENT_LINK_TTL_SECONDS = 300;

    /** config_key: payment.provider */
    public static final String PAYMENT_PROVIDER = "MOCK";

    /** config_key: MERCHANT_ACCOUNT_NAME */
    public static final String MERCHANT_ACCOUNT_NAME = "SMARTSTAY HOSPITALITY JSC";

    /** config_key: MERCHANT_BANK_NAME */
    public static final String MERCHANT_BANK_NAME = "Vietcombank (VCB)";

    /** config_key: MERCHANT_ACCOUNT_NUMBER */
    public static final String MERCHANT_ACCOUNT_NUMBER = "1029384756";

    // --- Staff & Operations ---
    /** config_key: CLEANING_SESSION_MINUTES */
    public static final int CLEANING_SESSION_MINUTES = 60;

    // --- Hardware & Devices ---
    /** config_key: MAIN_DOOR_DEVICE_ID */
    public static final String MAIN_DOOR_DEVICE_ID = "MAIN_DOOR_LOCK_01";

    /** config_key: KEY_AREA_INSTRUCTION */
    public static final String KEY_AREA_INSTRUCTION = "Key boxes are located in the lobby behind the front desk. Match the room number on your booking.";

    /** config_key: SNAPSHOT_RETENTION_DAYS */
    public static final int SNAPSHOT_RETENTION_DAYS = 30;

    /** config_key: DEVICE_OFFLINE_AFTER_SECONDS */
    public static final int DEVICE_OFFLINE_AFTER_SECONDS = 120;

    /** config_key: DEVICE_OFFLINE_SWEEP_ENABLED */
    public static final boolean DEVICE_OFFLINE_SWEEP_ENABLED = false;

    /** config_key: SIMULATOR_ENABLED */
    public static final boolean SIMULATOR_ENABLED = true;
}
