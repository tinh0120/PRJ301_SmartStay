package websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * WebSocket Server Endpoint for realtime events across SmartStay Web Clients.
 * URL: ws://<host>:<port>/SmartStay/ws/realtime
 */
@ServerEndpoint("/ws/realtime")
public class RealtimeWebSocketEndpoint {

    private static final Logger LOGGER = Logger.getLogger(RealtimeWebSocketEndpoint.class.getName());
    private static final Set<Session> SESSIONS = Collections.synchronizedSet(new HashSet<>());
    private static final Gson GSON = new Gson();

    @OnOpen
    public void onOpen(Session session) {
        SESSIONS.add(session);
        LOGGER.info("WebSocket client connected: " + session.getId() + " (Total active: " + SESSIONS.size() + ")");
        try {
            JsonObject welcome = new JsonObject();
            welcome.addProperty("event", "CONNECTED");
            welcome.addProperty("sessionId", session.getId());
            welcome.addProperty("timestamp", System.currentTimeMillis());
            session.getBasicRemote().sendText(welcome.toString());
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to send welcome message", e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        SESSIONS.remove(session);
        LOGGER.info("WebSocket client disconnected: " + session.getId() + " (Remaining: " + SESSIONS.size() + ")");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        SESSIONS.remove(session);
        LOGGER.log(Level.WARNING, "WebSocket error on session " + (session != null ? session.getId() : "null"), throwable);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        if ("ping".equalsIgnoreCase(message.trim())) {
            try {
                session.getBasicRemote().sendText("{\"event\":\"PONG\"}");
            } catch (IOException ignored) {}
        }
    }

    /**
     * Broadcasts a raw JSON string to all currently connected clients.
     */
    public static void broadcast(String jsonPayload) {
        synchronized (SESSIONS) {
            for (Session session : SESSIONS) {
                if (session != null && session.isOpen()) {
                    try {
                        session.getAsyncRemote().sendText(jsonPayload);
                    } catch (Exception e) {
                        LOGGER.log(Level.FINE, "Failed to send message to session " + session.getId(), e);
                    }
                }
            }
        }
    }

    /**
     * Broadcasts a typed event with payload attributes.
     */
    public static void broadcastEvent(String eventType, Map<String, Object> data) {
        JsonObject obj = new JsonObject();
        obj.addProperty("event", eventType);
        obj.addProperty("timestamp", System.currentTimeMillis());
        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    obj.addProperty(entry.getKey(), (Number) entry.getValue());
                } else if (entry.getValue() instanceof Boolean) {
                    obj.addProperty(entry.getKey(), (Boolean) entry.getValue());
                } else if (entry.getValue() != null) {
                    obj.addProperty(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        broadcast(obj.toString());
    }

    /**
     * Convenience helper to broadcast booking state changes.
     */
    public static void broadcastBookingUpdated(String bookingCode, String status, Long roomId) {
        JsonObject obj = new JsonObject();
        obj.addProperty("event", "BOOKING_UPDATED");
        obj.addProperty("bookingCode", bookingCode);
        obj.addProperty("status", status);
        if (roomId != null) {
            obj.addProperty("roomId", roomId);
        }
        obj.addProperty("timestamp", System.currentTimeMillis());
        broadcast(obj.toString());
    }

    /**
     * Convenience helper to broadcast room state changes.
     */
    public static void broadcastRoomStatusChanged(long roomId, String roomNumber, String newStatus) {
        JsonObject obj = new JsonObject();
        obj.addProperty("event", "ROOM_STATUS_CHANGED");
        obj.addProperty("roomId", roomId);
        obj.addProperty("roomNumber", roomNumber);
        obj.addProperty("newStatus", newStatus);
        obj.addProperty("timestamp", System.currentTimeMillis());
        broadcast(obj.toString());
    }
}
