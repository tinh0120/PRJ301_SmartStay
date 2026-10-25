package service;

import util.AppConstants;
import util.SecurityUtil;
import util.DBUtil;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Service managing access credentials, encrypted PIN generation, and guest decryption.
 */
public class CredentialService {

    private static final Logger LOGGER = Logger.getLogger(CredentialService.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();


    /**
     * DTO containing decrypted guest PIN, 3+3 formatted presentation, and validity window.
     */
    public static class GuestPinDetails {
        private String rawPin;
        private String formattedPin;
        private String status;
        private LocalDateTime validFrom;
        private LocalDateTime validUntil;

        public String getRawPin() {
            return rawPin;
        }

        public void setRawPin(String rawPin) {
            this.rawPin = rawPin;
        }

        public String getFormattedPin() {
            return formattedPin;
        }

        public void setFormattedPin(String formattedPin) {
            this.formattedPin = formattedPin;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getValidFrom() {
            return validFrom;
        }

        public void setValidFrom(LocalDateTime validFrom) {
            this.validFrom = validFrom;
        }

        public LocalDateTime getValidUntil() {
            return validUntil;
        }

        public void setValidUntil(LocalDateTime validUntil) {
            this.validUntil = validUntil;
        }
    }

    /**
     * DTO containing credential status and validity metadata (without the secret PIN).
     */
    public static class CredentialInfo {
        private long credentialId;
        private String status;
        private LocalDateTime validFrom;
        private LocalDateTime validUntil;

        public long getCredentialId() {
            return credentialId;
        }

        public void setCredentialId(long credentialId) {
            this.credentialId = credentialId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getValidFrom() {
            return validFrom;
        }

        public void setValidFrom(LocalDateTime validFrom) {
            this.validFrom = validFrom;
        }

        public LocalDateTime getValidUntil() {
            return validUntil;
        }

        public void setValidUntil(LocalDateTime validUntil) {
            this.validUntil = validUntil;
        }
    }

    /**
     * Creates exactly one GUEST_PIN credential inside the caller's database transaction.
     * Generates a random PIN, validates uniqueness on device, encrypts via AES-GCM and HMAC-SHA256,
     * and logs audit entry CREDENTIAL_CREATED.
     */
    public long createGuestPin(Connection conn, long bookingId, String deviceId,
                               LocalDateTime validFrom, LocalDateTime validUntil) throws SQLException {

        if (deviceId == null || deviceId.trim().isEmpty()) {
            deviceId = AppConstants.MAIN_DOOR_DEVICE_ID;
        }
        int pinLength = AppConstants.PIN_LENGTH;
        int maxBound = 1;
        for (int i = 0; i < pinLength; i++) {
            maxBound *= 10;
        }

        String chosenPin = null;
        byte[] chosenHmac = null;
        byte[] chosenCiphertext = null;

        String checkSql = "SELECT 1 FROM ACCESS_CREDENTIAL WHERE device_id = ? AND pin_hmac = ? AND status IN ('PENDING', 'ACTIVE')";

        for (int attempt = 0; attempt < 20; attempt++) {
            int num = RANDOM.nextInt(maxBound);
            String candidatePin = String.format("%0" + pinLength + "d", num);
            byte[] hmac = SecurityUtil.hmacPin(candidatePin);

            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, deviceId);
                ps.setBytes(2, hmac);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Unique pin found!
                        chosenPin = candidatePin;
                        chosenHmac = hmac;
                        chosenCiphertext = SecurityUtil.encryptPin(candidatePin);
                        break;
                    }
                }
            }
        }

        if (chosenPin == null) {
            throw new SQLException("Could not allocate a unique PIN. Please try again.");
        }

        String insertSql = "INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, "
                + "pin_ciphertext, pin_hmac, valid_from, valid_until, use_policy, status, used_count) "
                + "VALUES ('GUEST_PIN', ?, ?, ?, ?, ?, ?, 'MULTI_USE', 'PENDING', 0)";

        long credentialId;
        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, bookingId);
            ps.setString(2, deviceId);
            ps.setBytes(3, chosenCiphertext);
            ps.setBytes(4, chosenHmac);
            ps.setTimestamp(5, Timestamp.valueOf(validFrom));
            ps.setTimestamp(6, Timestamp.valueOf(validUntil));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    credentialId = rs.getLong(1);
                } else {
                    throw new SQLException("Failed to retrieve generated credential_id.");
                }
            }
        }

        // Audit CREDENTIAL_CREATED (never include the raw PIN)
        LOGGER.info("Created guest PIN credential for booking #" + bookingId);

        return credentialId;
    }

    /**
     * Decrypts the 6-digit PIN for an authenticated guest owning the booking.
     * Verifies ownership and active/pending status. Returns null if invalid or inactive.
     */
    public String decryptPinForGuest(long credentialId, long guestId) throws SQLException {
        String sql = "SELECT ac.credential_id, ac.status, ac.pin_ciphertext, b.guest_id "
                + "FROM ACCESS_CREDENTIAL ac "
                + "JOIN BOOKING b ON ac.booking_id = b.booking_id "
                + "WHERE ac.credential_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, credentialId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                long ownerGuestId = rs.getLong("guest_id");
                if (ownerGuestId != guestId) {
                    return null;
                }
                String status = rs.getString("status");
                if (!"PENDING".equalsIgnoreCase(status) && !"ACTIVE".equalsIgnoreCase(status)) {
                    return null;
                }
                byte[] ciphertext = rs.getBytes("pin_ciphertext");
                if (ciphertext == null || ciphertext.length == 0) {
                    return null;
                }
                return SecurityUtil.decryptPin(ciphertext);
            }
        }
    }

    /**
     * Decrypts the active/pending PIN for an authenticated booking owner (Task 7 Option A).
     * Strictly verifies guest ownership and validity window. Never logs the plaintext PIN.
     * Returns null if unauthorized, inactive, expired, or non-existent.
     */
    public GuestPinDetails getGuestPinForBooking(long bookingId, long guestId) throws SQLException {
        String sql = "SELECT TOP 1 ac.credential_id, ac.status, ac.pin_ciphertext, ac.valid_from, ac.valid_until, b.guest_id "
                + "FROM ACCESS_CREDENTIAL ac "
                + "JOIN BOOKING b ON ac.booking_id = b.booking_id "
                + "WHERE ac.booking_id = ? AND ac.credential_type = 'GUEST_PIN' "
                + "ORDER BY ac.credential_id DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                long ownerGuestId = rs.getLong("guest_id");
                if (ownerGuestId != guestId) {
                    return null;
                }
                String status = rs.getString("status");
                if (!"PENDING".equalsIgnoreCase(status) && !"ACTIVE".equalsIgnoreCase(status)) {
                    return null;
                }
                Timestamp vu = rs.getTimestamp("valid_until");
                if (vu != null && LocalDateTime.now().isAfter(vu.toLocalDateTime())) {
                    return null;
                }

                byte[] ciphertext = rs.getBytes("pin_ciphertext");
                if (ciphertext == null || ciphertext.length == 0) {
                    return null;
                }

                String rawPin = SecurityUtil.decryptPin(ciphertext);
                if (rawPin == null || rawPin.trim().isEmpty()) {
                    return null;
                }

                GuestPinDetails details = new GuestPinDetails();
                String trimmed = rawPin.trim();
                details.setRawPin(trimmed);
                if (trimmed.length() == 6) {
                    details.setFormattedPin(trimmed.substring(0, 3) + " " + trimmed.substring(3));
                } else {
                    details.setFormattedPin(trimmed);
                }
                details.setStatus(status);
                Timestamp vf = rs.getTimestamp("valid_from");
                if (vf != null) {
                    details.setValidFrom(vf.toLocalDateTime());
                }
                if (vu != null) {
                    details.setValidUntil(vu.toLocalDateTime());
                }

                return details;
            }
        }
    }

    /**
     * Retrieves credential metadata for a booking (for display on detail page).
     */
    public CredentialInfo getCredentialForBooking(long bookingId) throws SQLException {
        String sql = "SELECT TOP 1 credential_id, status, valid_from, valid_until "
                + "FROM ACCESS_CREDENTIAL "
                + "WHERE booking_id = ? "
                + "ORDER BY credential_id DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    CredentialInfo info = new CredentialInfo();
                    info.setCredentialId(rs.getLong("credential_id"));
                    info.setStatus(rs.getString("status"));
                    Timestamp vf = rs.getTimestamp("valid_from");
                    if (vf != null) {
                        info.setValidFrom(vf.toLocalDateTime());
                    }
                    Timestamp vu = rs.getTimestamp("valid_until");
                    if (vu != null) {
                        info.setValidUntil(vu.toLocalDateTime());
                    }
                    return info;
                }
            }
        }
        return null;
    }

    /**
     * Creates exactly one STAFF_PIN credential inside the caller's database transaction.
     * Generates a random PIN, validates uniqueness on device, encrypts via AES-GCM and HMAC-SHA256,
     * and logs audit entry CREDENTIAL_CREATED.
     */
    public long createStaffPin(Connection conn, long assignmentId, String deviceId,
                               LocalDateTime validFrom, LocalDateTime validUntil) throws SQLException {

        if (deviceId == null || deviceId.trim().isEmpty()) {
            deviceId = AppConstants.MAIN_DOOR_DEVICE_ID;
        }
        int pinLength = AppConstants.PIN_LENGTH;
        int maxBound = 1;
        for (int i = 0; i < pinLength; i++) {
            maxBound *= 10;
        }

        String chosenPin = null;
        byte[] chosenHmac = null;
        byte[] chosenCiphertext = null;

        String checkSql = "SELECT 1 FROM ACCESS_CREDENTIAL WHERE device_id = ? AND pin_hmac = ? AND status IN ('PENDING', 'ACTIVE')";

        for (int attempt = 0; attempt < 20; attempt++) {
            int num = RANDOM.nextInt(maxBound);
            String candidatePin = String.format("%0" + pinLength + "d", num);
            byte[] hmac = SecurityUtil.hmacPin(candidatePin);

            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, deviceId);
                ps.setBytes(2, hmac);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        chosenPin = candidatePin;
                        chosenHmac = hmac;
                        chosenCiphertext = SecurityUtil.encryptPin(candidatePin);
                        break;
                    }
                }
            }
        }

        if (chosenPin == null) {
            throw new SQLException("Could not allocate a unique PIN. Please try again.");
        }

        String insertSql = "INSERT INTO ACCESS_CREDENTIAL (credential_type, staff_assignment_id, device_id, "
                + "pin_ciphertext, pin_hmac, valid_from, valid_until, use_policy, status, used_count) "
                + "VALUES ('STAFF_PIN', ?, ?, ?, ?, ?, ?, 'MULTI_USE', 'PENDING', 0)";

        long credentialId;
        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, assignmentId);
            ps.setString(2, deviceId);
            ps.setBytes(3, chosenCiphertext);
            ps.setBytes(4, chosenHmac);
            ps.setTimestamp(5, Timestamp.valueOf(validFrom));
            ps.setTimestamp(6, Timestamp.valueOf(validUntil));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    credentialId = rs.getLong(1);
                } else {
                    throw new SQLException("Failed to retrieve generated credential_id.");
                }
            }
        }

        // Audit CREDENTIAL_CREATED (never include raw PIN)
        LOGGER.info("Created staff PIN credential for assignment #" + assignmentId);

        return credentialId;
    }

    /**
     * Decrypts the 6-digit PIN for an authenticated staff assigned to the assignment.
     */
    public String decryptPinForStaff(long credentialId, long staffId) throws SQLException {
        String sql = "SELECT ac.credential_id, ac.status, ac.pin_ciphertext, sa.staff_id "
                + "FROM ACCESS_CREDENTIAL ac "
                + "JOIN STAFF_ASSIGNMENT sa ON ac.staff_assignment_id = sa.assignment_id "
                + "WHERE ac.credential_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, credentialId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                long ownerStaffId = rs.getLong("staff_id");
                if (ownerStaffId != staffId) {
                    return null;
                }
                String status = rs.getString("status");
                if (!"PENDING".equalsIgnoreCase(status) && !"ACTIVE".equalsIgnoreCase(status)) {
                    return null;
                }
                byte[] ciphertext = rs.getBytes("pin_ciphertext");
                if (ciphertext == null || ciphertext.length == 0) {
                    return null;
                }
                return SecurityUtil.decryptPin(ciphertext);
            }
        }
    }

    /**
     * Decrypts the 6-digit PIN for an authenticated staff by assignment ID.
     */
    public String decryptPinForStaffAssignment(long assignmentId, long staffId) throws SQLException {
        String sql = "SELECT ac.credential_id, ac.status, ac.pin_ciphertext, sa.staff_id "
                + "FROM ACCESS_CREDENTIAL ac "
                + "JOIN STAFF_ASSIGNMENT sa ON ac.staff_assignment_id = sa.assignment_id "
                + "WHERE sa.assignment_id = ? AND ac.status IN ('PENDING', 'ACTIVE')";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                long ownerStaffId = rs.getLong("staff_id");
                if (ownerStaffId != staffId) {
                    return null;
                }
                byte[] ciphertext = rs.getBytes("pin_ciphertext");
                if (ciphertext == null || ciphertext.length == 0) {
                    return null;
                }
                return SecurityUtil.decryptPin(ciphertext);
            }
        }
    }

    /**
     * Retrieves credential metadata for a staff assignment (for display on detail page).
     */
    public CredentialInfo getCredentialForAssignment(long assignmentId) throws SQLException {
        String sql = "SELECT TOP 1 credential_id, status, valid_from, valid_until "
                + "FROM ACCESS_CREDENTIAL "
                + "WHERE staff_assignment_id = ? "
                + "ORDER BY credential_id DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    CredentialInfo info = new CredentialInfo();
                    info.setCredentialId(rs.getLong("credential_id"));
                    info.setStatus(rs.getString("status"));
                    Timestamp vf = rs.getTimestamp("valid_from");
                    if (vf != null) {
                        info.setValidFrom(vf.toLocalDateTime());
                    }
                    Timestamp vu = rs.getTimestamp("valid_until");
                    if (vu != null) {
                        info.setValidUntil(vu.toLocalDateTime());
                    }
                    return info;
                }
            }
        }
        return null;
    }
}

