package util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Flash message helper for one-shot Post-Redirect-Get feedback alerts.
 */
public final class Flash {

    private static final String FLASH_SESSION_KEY = "flash";

    private Flash() {
    }

    private static void addMessage(HttpServletRequest req, String type, String text) {
        if (req == null || text == null || text.trim().isEmpty()) {
            return;
        }
        HttpSession session = req.getSession(true);
        synchronized (session) {
            @SuppressWarnings("unchecked")
            List<Message> list = (List<Message>) session.getAttribute(FLASH_SESSION_KEY);
            if (list == null) {
                list = new ArrayList<>();
                session.setAttribute(FLASH_SESSION_KEY, list);
            }
            list.add(new Message(type, text.trim()));
        }
    }

    public static void success(HttpServletRequest req, String message) {
        addMessage(req, "success", message);
    }

    public static void error(HttpServletRequest req, String message) {
        addMessage(req, "danger", message);
    }

    public static void danger(HttpServletRequest req, String message) {
        addMessage(req, "danger", message);
    }

    public static void info(HttpServletRequest req, String message) {
        addMessage(req, "info", message);
    }

    public static void warning(HttpServletRequest req, String message) {
        addMessage(req, "warning", message);
    }

    /**
     * Consumes and removes all flash messages from the session.
     */
    public static List<Message> consume(HttpServletRequest req) {
        if (req == null) {
            return Collections.emptyList();
        }
        HttpSession session = req.getSession(false);
        if (session == null) {
            return Collections.emptyList();
        }
        synchronized (session) {
            @SuppressWarnings("unchecked")
            List<Message> list = (List<Message>) session.getAttribute(FLASH_SESSION_KEY);
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            session.removeAttribute(FLASH_SESSION_KEY);
            return Collections.unmodifiableList(new ArrayList<>(list));
        }
    }

    public static final class Message implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String type; // success | danger | info | warning
        private final String text;

        public Message(String type, String text) {
            this.type = type;
            this.text = text;
        }

        public String getType() {
            return type;
        }

        public String getText() {
            return text;
        }
    }
}
