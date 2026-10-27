package model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One row of the dashboard "Needs attention" list (Task R6): something an operator should act on
 * now, with ONE action. Lower rank = more urgent.
 */
public class AttentionItem implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int RANK_CRITICAL_ALERT = 0;
    public static final int RANK_DEVICE_FAULT = 1;
    public static final int RANK_DEVICES_OFFLINE = 2;
    public static final int RANK_OVERDUE_CHECKIN = 3;
    public static final int RANK_HIGH_ALERT = 4;
    public static final int RANK_REFUND = 5;
    public static final int RANK_NOSHOW_HELD = 6;
    public static final int RANK_CLEANING_EXPIRED = 7;

    private final int rank;
    private final String tone;
    private final String title;
    private final String detail;
    private final String actionLabel;
    private final String actionPath;
    private final LocalDateTime at;

    /**
     * @param tone       severity modifier for the left bar: critical, high, medium or low
     * @param actionPath path below the context root, e.g. "/admin/bookings/detail?id=5"
     */
    public AttentionItem(int rank, String tone, String title, String detail,
                         String actionLabel, String actionPath, LocalDateTime at) {
        this.rank = rank;
        this.tone = tone;
        this.title = title;
        this.detail = detail;
        this.actionLabel = actionLabel;
        this.actionPath = actionPath;
        this.at = at;
    }

    public int getRank() { return rank; }
    public String getTone() { return tone; }
    public String getTitle() { return title; }
    public String getDetail() { return detail; }
    public String getActionLabel() { return actionLabel; }
    public String getActionPath() { return actionPath; }
    public LocalDateTime getAt() { return at; }
}
