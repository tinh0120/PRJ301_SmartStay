package service;

/**
 * Common domain exception for business validation, pricing, payment, and review errors.
 */
public class ServiceException extends Exception {

    private static final long serialVersionUID = 1L;

    public static final String EMPTY_SELECTION = "EMPTY_SELECTION";
    public static final String DUPLICATE_SLOT = "DUPLICATE_SLOT";
    public static final String TOO_MANY_SLOTS = "TOO_MANY_SLOTS";
    public static final String UNKNOWN_SLOT = "UNKNOWN_SLOT";
    public static final String NON_CONSECUTIVE_SLOTS = "NON_CONSECUTIVE_SLOTS";
    public static final String ROOM_NOT_FOUND = "ROOM_NOT_FOUND";
    public static final String ROOM_NOT_BOOKABLE = "ROOM_NOT_BOOKABLE";
    public static final String DATE_OUT_OF_RANGE = "DATE_OUT_OF_RANGE";
    public static final String SLOT_UNAVAILABLE = "SLOT_UNAVAILABLE";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String UNAVAILABLE = "UNAVAILABLE";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String INVALID_STATUS = "INVALID_STATUS";
    public static final String NOT_CANCELLABLE = "NOT_CANCELLABLE";
    public static final String REASON_REQUIRED = "REASON_REQUIRED";
    public static final String NOTE_REQUIRED = "NOTE_REQUIRED";
    public static final String INVALID_ACTION = "INVALID_ACTION";
    public static final String HOLD_EXPIRED = "HOLD_EXPIRED";
    public static final String INVALID_RATING = "INVALID_RATING";
    public static final String COMMENT_TOO_LONG = "COMMENT_TOO_LONG";
    public static final String NOT_ELIGIBLE = "NOT_ELIGIBLE";
    public static final String ALREADY_REVIEWED = "ALREADY_REVIEWED";

    private final String code;

    public ServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
