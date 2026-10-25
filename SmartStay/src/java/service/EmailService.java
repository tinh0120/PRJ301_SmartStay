package service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing automated transactional email notifications via Gmail SMTP.
 * Configured with full UTF-8 encoding support for Vietnamese text.
 */
public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    private static final String GMAIL_USERNAME = "kimthu0120657@gmail.com";
    private static final String GMAIL_PASSWORD = "anxbflwvxruxviqg"; // App Password

    private static Session getSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(GMAIL_USERNAME, GMAIL_PASSWORD);
            }
        });
    }

    private static void sendHtmlEmail(String toEmail, String subject, String bodyHtml) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return;
        }
        new Thread(() -> {
            try {
                MimeMessage message = new MimeMessage(getSession());
                // UTF-8 encoding for sender name and subject line
                message.setFrom(new InternetAddress(GMAIL_USERNAME, "SmartStay Homestay", "UTF-8"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail.trim()));
                message.setSubject(subject, "UTF-8");
                message.setContent(bodyHtml, "text/html; charset=UTF-8");

                Transport.send(message);
                LOGGER.info("Email sent successfully to: " + toEmail + " | Subject: " + subject);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to send email to " + toEmail + ": " + e.getMessage(), e);
            }
        }).start();
    }

    /**
     * Formats database datetime string into display format: HH:mm - dd/MM/yyyy
     */
    public static String formatDateTime(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.trim().isEmpty()) {
            return "";
        }
        try {
            String clean = rawDateTime.replace("T", " ").trim();
            if (clean.contains(".")) {
                clean = clean.substring(0, clean.indexOf('.'));
            }
            if (clean.length() == 16) { // yyyy-MM-dd HH:mm
                clean += ":00";
            }
            java.text.SimpleDateFormat inFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.text.SimpleDateFormat outFormat = new java.text.SimpleDateFormat("HH:mm - dd/MM/yyyy");
            return outFormat.format(inFormat.parse(clean));
        } catch (Exception e) {
            if (rawDateTime.contains(".")) {
                return rawDateTime.substring(0, rawDateTime.indexOf('.'));
            }
            return rawDateTime;
        }
    }

    /**
     * 1. Booking confirmation email with door access PIN.
     */
    public static void sendBookingSuccess(String toEmail, String guestName, String bookingCode,
                                          String roomNumber, String pin, String checkin, String checkout) {
        String formattedCheckin = formatDateTime(checkin);
        String formattedCheckout = formatDateTime(checkout);
        String subject = "SmartStay - Booking Confirmation #" + bookingCode;

        String content = "<div style='font-family: Arial, sans-serif; padding: 25px; border: 1px solid #e0e0e0; border-radius: 10px; max-width: 600px; margin: 0 auto; color: #333;'>"
                + "<h2 style='color: #1a73e8; margin-top: 0; font-size: 22px;'>🎉 Booking Confirmed!</h2>"
                + "<p>Dear <b>" + (guestName != null ? guestName : "Valued Guest") + "</b>,</p>"
                + "<p>Thank you for choosing SmartStay. Your booking has been successfully paid and confirmed.</p>"
                + "<table style='width: 100%; border-collapse: collapse; margin: 20px 0; background: #fafafa; border-radius: 6px; overflow: hidden;'>"
                + "<tr><td style='padding: 10px 15px; border-bottom: 1px solid #eee; color: #666;'>Booking Code:</td><td style='padding: 10px 15px; border-bottom: 1px solid #eee; font-weight: bold; color: #222;'>" + bookingCode + "</td></tr>"
                + "<tr><td style='padding: 10px 15px; border-bottom: 1px solid #eee; color: #666;'>Room Number:</td><td style='padding: 10px 15px; border-bottom: 1px solid #eee; font-weight: bold; color: #1a73e8;'>" + roomNumber + "</td></tr>"
                + "<tr><td style='padding: 10px 15px; border-bottom: 1px solid #eee; color: #666;'>Check-in Time:</td><td style='padding: 10px 15px; border-bottom: 1px solid #eee; font-weight: bold;'>" + formattedCheckin + "</td></tr>"
                + "<tr><td style='padding: 10px 15px; border-bottom: 1px solid #eee; color: #666;'>Check-out Time:</td><td style='padding: 10px 15px; border-bottom: 1px solid #eee; font-weight: bold;'>" + formattedCheckout + "</td></tr>"
                + "</table>"
                + "<div style='background-color: #f0f7ff; border-left: 5px solid #1a73e8; padding: 18px; border-radius: 6px; margin: 20px 0; text-align: center;'>"
                + "<div style='color: #555; font-size: 13px; font-weight: bold; text-transform: uppercase; letter-spacing: 1px;'>YOUR HOMESTAY ENTRANCE PIN</div>"
                + "<div style='font-size: 38px; font-weight: bold; color: #d93025; letter-spacing: 8px; margin: 10px 0; font-family: monospace;'>" + pin + "</div>"
                + "<div style='color: #666; font-size: 13px;'>Enter these 6 digits on the main door keypad and press <b>#</b> to unlock.</div>"
                + "</div>"
                + "<p style='color: #888; font-size: 12px; margin-top: 25px; line-height: 1.5;'>We wish you a pleasant and memorable stay at SmartStay!</p>"
                + "</div>";

        sendHtmlEmail(toEmail, subject, content);
    }

    /**
     * 2. 30-minute check-in reminder email.
     */
    public static void sendCheckinReminder(String toEmail, String guestName, String bookingCode,
                                           String roomNumber, String pin) {
        String subject = "SmartStay - Reminder: 30 minutes until check-in #" + bookingCode;

        String content = "<div style='font-family: Arial, sans-serif; padding: 25px; border: 1px solid #c8e6c9; border-radius: 10px; max-width: 600px; margin: 0 auto; color: #333;'>"
                + "<h2 style='color: #2e7d32; margin-top: 0; font-size: 22px;'>⏰ Check-in Time Approaching!</h2>"
                + "<p>Dear <b>" + (guestName != null ? guestName : "Valued Guest") + "</b>,</p>"
                + "<p>There are approximately <b>30 minutes</b> remaining before your scheduled check-in at <b>Room " + roomNumber + "</b> (Booking: <b>" + bookingCode + "</b>).</p>"
                + "<p>Your main entrance door PIN is ready for access:</p>"
                + "<div style='background-color: #f1f8e9; border: 2px dashed #4caf50; padding: 15px; border-radius: 8px; text-align: center; margin: 20px 0;'>"
                + "<div style='font-size: 34px; font-weight: bold; color: #2e7d32; letter-spacing: 8px; font-family: monospace;'>" + pin + "</div>"
                + "<div style='color: #555; font-size: 12px; margin-top: 6px;'>Enter the PIN and press <b>#</b> on the entrance door keypad.</div>"
                + "</div>"
                + "<p style='color: #555; font-size: 14px;'>📌 <b>Check-in Guide:</b> After unlocking the main entrance, proceed to the lobby key locker to retrieve the physical key for <b>Room " + roomNumber + "</b>.</p>"
                + "<p style='color: #888; font-size: 12px; margin-top: 25px;'>SmartStay looks forward to welcoming you!</p>"
                + "</div>";

        sendHtmlEmail(toEmail, subject, content);
    }

    /**
     * 3. 30-minute check-out reminder email.
     */
    public static void sendCheckoutReminder(String toEmail, String guestName, String bookingCode,
                                            String roomNumber, String checkoutTime) {
        String formattedTime = formatDateTime(checkoutTime);
        String subject = "SmartStay - Reminder: 30 minutes until check-out #" + bookingCode;

        String content = "<div style='font-family: Arial, sans-serif; padding: 25px; border: 1px solid #ffe082; border-radius: 10px; max-width: 600px; margin: 0 auto; color: #333;'>"
                + "<h2 style='color: #e65100; margin-top: 0; font-size: 22px;'>🔔 Check-out Notice</h2>"
                + "<p>Dear <b>" + (guestName != null ? guestName : "Valued Guest") + "</b>,</p>"
                + "<p>Your stay at <b>Room " + roomNumber + "</b> will conclude at <b>" + formattedTime + "</b> (in 30 minutes).</p>"
                + "<p>Please ensure you collect all personal belongings and complete check-out on time.</p>"
                + "<div style='background-color: #fff8e1; border-left: 5px solid #ff9800; padding: 15px; margin: 20px 0; border-radius: 6px;'>"
                + "<p style='margin: 0; color: #c62828; font-size: 14px; font-weight: bold;'>⚠️ IMPORTANT NOTICE:</p>"
                + "<p style='margin: 6px 0 0 0; color: #555; font-size: 13px; line-height: 1.5;'>When the scheduled check-out time expires, door credentials will be deactivated automatically.</p>"
                + "</div>"
                + "<p style='color: #888; font-size: 12px; margin-top: 25px;'>Thank you for staying with SmartStay Homestay!</p>"
                + "</div>";

        sendHtmlEmail(toEmail, subject, content);
    }
}
