package service;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing real-time MQTT messaging for IoT hardware triggers (Buzzer, Alarm).
 */
public class MqttService {

    private static final Logger LOGGER = Logger.getLogger(MqttService.class.getName());
    // Public free MQTT broker for IoT demo
    private static final String BROKER_URL = "tcp://broker.emqx.io:1883";
    private static final String CLIENT_ID = "SmartStay_Server_" + System.currentTimeMillis();
    private static MqttClient client;

    private static synchronized MqttClient getClient() {
        if (client == null || !client.isConnected()) {
            try {
                client = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                options.setConnectionTimeout(10);
                options.setKeepAliveInterval(60);
                options.setAutomaticReconnect(true);
                client.connect(options);
                LOGGER.info("Connected to MQTT Broker: " + BROKER_URL);
            } catch (MqttException e) {
                LOGGER.log(Level.WARNING, "Could not connect to MQTT Broker: " + e.getMessage());
            }
        }
        return client;
    }

    /** Opens the broker connection in the background at start-up so the first remote command is not slow. */
    public static void warmUp() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                getClient();
            }
        }, "mqtt-warmup");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Sends a signed remote command (UNLOCK or SILENCE) to topic smartstay/device/{deviceId}/cmd.
     * Payload {"cmd","ts","nonce","sig"}: sig = HMAC-SHA256(key = api_key_hash, "cmd|ts|nonce")
     * (see util.IotDevices.signCommand). Never retained, QoS 1; the ESP32 rejects old or reused commands.
     *
     * @return true when the broker accepted the message
     */
    public static synchronized boolean publishCommand(String deviceId, String command, String apiKeyHash) {
        MqttClient mqtt = getClient();
        if (mqtt == null || !mqtt.isConnected()) {
            LOGGER.warning("MQTT client not connected, command " + command + " for " + deviceId + " not sent");
            return false;
        }
        try {
            long ts = java.time.Instant.now().getEpochSecond();
            String nonce = Long.toHexString(new java.security.SecureRandom().nextLong());
            String sig = util.IotDevices.signCommand(apiKeyHash, command, ts, nonce);
            String payload = "{\"cmd\":\"" + command + "\",\"ts\":" + ts + ",\"nonce\":\"" + nonce
                    + "\",\"sig\":\"" + sig + "\"}";
            MqttMessage message = new MqttMessage(payload.getBytes("UTF-8"));
            message.setQos(1);
            message.setRetained(false);
            mqtt.publish("smartstay/device/" + deviceId + "/cmd", message);
            LOGGER.info("Published remote command " + command + " to " + deviceId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to publish command " + command + " for " + deviceId, e);
            return false;
        }
    }

    /**
     * Publishes a buzzer alarm command to the specified IoT device topic.
     *
     * @param deviceId target device ID (e.g. MAIN_DOOR_LOCK_01)
     * @param durationSeconds duration for buzzer sound
     * @param reason trigger reason (e.g. CHECKOUT_OVERDUE)
     */
    public static void triggerBuzzer(String deviceId, int durationSeconds, String reason) {
        new Thread(() -> {
            try {
                MqttClient mqtt = getClient();
                if (mqtt != null && mqtt.isConnected()) {
                    String topic = "smartstay/device/" + deviceId + "/buzzer";
                    String payload = "{\"action\":\"ALARM\",\"duration\":" + durationSeconds
                            + ",\"reason\":\"" + (reason != null ? reason : "CHECKOUT_OVERDUE") + "\"}";

                    MqttMessage message = new MqttMessage(payload.getBytes("UTF-8"));
                    message.setQos(1);
                    mqtt.publish(topic, message);
                    LOGGER.info("Published MQTT Buzzer trigger to topic " + topic + ": " + payload);
                } else {
                    LOGGER.warning("MQTT client not connected, skipped buzzer trigger for " + deviceId);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to publish MQTT buzzer trigger for " + deviceId, e);
            }
        }).start();
    }
}
