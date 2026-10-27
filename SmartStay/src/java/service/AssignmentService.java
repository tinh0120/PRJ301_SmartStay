package service;

import dao.AssignmentDao;
import dao.RoomDao;
import dao.UserDao;
import model.Assignment;
import model.Room;
import model.User;
import util.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing staff room assignments and staff credentials.
 */
public class AssignmentService {

    private static final Logger LOGGER = Logger.getLogger(AssignmentService.class.getName());

    private final AssignmentDao assignmentDao = new AssignmentDao();
    private final RoomDao roomDao = new RoomDao();
    private final UserDao userDao = new UserDao();
    private final CredentialService credentialService = new CredentialService();

    /**
     * Creates an assignment for a room in NEEDS_CLEANING status.
     * Auto-creates PENDING cleaning session and STAFF_PIN credential.
     */
    public long createAssignment(long adminUserId, long staffId, long roomId,
                                 LocalDateTime shiftStart, LocalDateTime shiftEnd,
                                 String note, String ip) throws SQLException {

        if (shiftStart == null || shiftEnd == null) {
            throw new IllegalArgumentException("Shift start and end times are required.");
        }
        if (!shiftEnd.isAfter(shiftStart)) {
            throw new IllegalArgumentException("Shift end time must be after shift start time.");
        }

        String cleanNote = (note != null) ? note.trim() : "";
        if (cleanNote.length() > 255) {
            cleanNote = cleanNote.substring(0, 255);
        }

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Validate staff
                User staff = userDao.findById(staffId);
                if (staff == null || !"STAFF".equalsIgnoreCase(staff.getRoleName()) || !"ACTIVE".equalsIgnoreCase(staff.getStatus())) {
                    throw new IllegalStateException("Selected user must be an active staff member.");
                }

                // 2. Validate room status (MUST be NEEDS_CLEANING)
                String lockRoomSql = "SELECT room_id, room_number, status FROM ROOM WITH (UPDLOCK, HOLDLOCK) WHERE room_id = ?";
                String roomNumber;
                String roomStatus;
                try (PreparedStatement psR = conn.prepareStatement(lockRoomSql)) {
                    psR.setLong(1, roomId);
                    try (ResultSet rsR = psR.executeQuery()) {
                        if (!rsR.next()) {
                            throw new IllegalStateException("Room not found.");
                        }
                        roomNumber = rsR.getString("room_number");
                        roomStatus = rsR.getString("status");
                    }
                }

                if (!"NEEDS_CLEANING".equalsIgnoreCase(roomStatus)) {
                    throw new IllegalStateException("Assignments can only be created for rooms needing cleaning.");
                }

                // 3. Overlap check for staff
                if (assignmentDao.hasOverlap(conn, staffId, shiftStart, shiftEnd, null)) {
                    throw new IllegalStateException("Staff already has an overlapping active assignment for this shift window.");
                }

                // 4. Create assignment
                long assignmentId = assignmentDao.create(conn, staffId, roomId, adminUserId, shiftStart, shiftEnd, cleanNote);

                // 5. Create staff PIN credential
                credentialService.createStaffPin(conn, assignmentId, null, shiftStart, shiftEnd);

                // 7. Audit log
                String auditDetail = "Created cleaning assignment #" + assignmentId + " for room " + roomNumber
                        + " assigned to staff " + staff.getFullName() + " (shift: " + shiftStart + " - " + shiftEnd + ")";
                LOGGER.info("Audit: " + auditDetail);

                conn.commit();
                return assignmentId;

            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to create staff assignment", e);
                if (e instanceof SQLException) throw (SQLException) e;
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new SQLException("Failed to create assignment.", e);
            }
        }
    }

    /**
     * Updates an existing ASSIGNED assignment.
     * If shift times change, reissues credential validity.
     */
    public void updateAssignment(long adminUserId, long assignmentId, long staffId,
                                 LocalDateTime shiftStart, LocalDateTime shiftEnd,
                                 String note, String ip) throws SQLException {

        if (shiftStart == null || shiftEnd == null) {
            throw new IllegalArgumentException("Shift start and end times are required.");
        }
        if (!shiftEnd.isAfter(shiftStart)) {
            throw new IllegalArgumentException("Shift end time must be after shift start time.");
        }

        String cleanNote = (note != null) ? note.trim() : "";
        if (cleanNote.length() > 255) {
            cleanNote = cleanNote.substring(0, 255);
        }

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Assignment current = assignmentDao.findById(conn, assignmentId);
                if (current == null) {
                    throw new IllegalStateException("Assignment not found.");
                }
                if (!Assignment.STATUS_ASSIGNED.equalsIgnoreCase(current.getStatus())) {
                    throw new IllegalStateException("Only assignments in ASSIGNED status can be edited.");
                }

                // Validate staff
                User staff = userDao.findById(staffId);
                if (staff == null || !"STAFF".equalsIgnoreCase(staff.getRoleName()) || !"ACTIVE".equalsIgnoreCase(staff.getStatus())) {
                    throw new IllegalStateException("Selected user must be an active staff member.");
                }

                // Overlap check
                if (assignmentDao.hasOverlap(conn, staffId, shiftStart, shiftEnd, assignmentId)) {
                    throw new IllegalStateException("Staff already has an overlapping active assignment for this shift window.");
                }

                boolean shiftChanged = !shiftStart.equals(current.getShiftStart()) || !shiftEnd.equals(current.getShiftEnd());

                // Update assignment row
                assignmentDao.update(conn, assignmentId, staffId, shiftStart, shiftEnd, cleanNote);

                if (shiftChanged) {
                    // Revoke old live credential
                    String revokeOldSql = "UPDATE ACCESS_CREDENTIAL SET status = 'REVOKED', revoke_reason = 'ASSIGNMENT_ENDED', "
                            + "revoked_at = SYSDATETIME() WHERE staff_assignment_id = ? AND status IN ('PENDING', 'ACTIVE')";
                    try (PreparedStatement psRev = conn.prepareStatement(revokeOldSql)) {
                        psRev.setLong(1, assignmentId);
                        psRev.executeUpdate();
                    }

                    // Create new credential
                    credentialService.createStaffPin(conn, assignmentId, null, shiftStart, shiftEnd);
                }

                String auditDetail = "Updated assignment #" + assignmentId + ": staff #" + staffId
                        + ", shift " + shiftStart + " - " + shiftEnd;
                LOGGER.info("Audit: " + auditDetail);

                conn.commit();

            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to update staff assignment #" + assignmentId, e);
                if (e instanceof SQLException) throw (SQLException) e;
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new SQLException("Failed to update assignment.", e);
            }
        }
    }

    /**
     * Cancels an ASSIGNED assignment, cancelling its cleaning session and revoking credential.
     */
    public void cancelAssignment(long adminUserId, long assignmentId, String ip) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Assignment current = assignmentDao.findById(conn, assignmentId);
                if (current == null) {
                    throw new IllegalStateException("Assignment not found.");
                }
                if (!Assignment.STATUS_ASSIGNED.equalsIgnoreCase(current.getStatus())) {
                    throw new IllegalStateException("Only assignments in ASSIGNED status can be cancelled.");
                }

                assignmentDao.cancel(conn, assignmentId);

                // Revoke live credential
                String revokeOldSql = "UPDATE ACCESS_CREDENTIAL SET status = 'REVOKED', revoke_reason = 'ASSIGNMENT_ENDED', "
                        + "revoked_at = SYSDATETIME() WHERE staff_assignment_id = ? AND status IN ('PENDING', 'ACTIVE')";
                try (PreparedStatement psRev = conn.prepareStatement(revokeOldSql)) {
                    psRev.setLong(1, assignmentId);
                    psRev.executeUpdate();
                }

                LOGGER.info("Cancelled assignment #" + assignmentId + " by admin #" + adminUserId);

                conn.commit();

            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to cancel staff assignment #" + assignmentId, e);
                if (e instanceof SQLException) throw (SQLException) e;
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new SQLException("Failed to cancel assignment.", e);
            }
        }
    }

    /**
     * Starts cleaning for an assignment during its shift window.
     * Returns true if started, false if already active (no-op).
     */
    /**
     * Starts cleaning for an assignment during its shift window.
     * Returns true if started, false if already active (no-op).
     */
    public boolean startCleaning(long staffId, long assignmentId, String ip) throws SQLException {
        LocalDateTime now = util.ClockService.now();

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Assignment a = assignmentDao.findById(conn, assignmentId);
                if (a == null || a.getStaffId() != staffId) {
                    throw new IllegalStateException("Assignment not found or does not belong to you.");
                }

                if (!Assignment.STATUS_ASSIGNED.equalsIgnoreCase(a.getStatus())) {
                    throw new IllegalStateException("Assignment cannot be started from status " + a.getStatus() + ".");
                }

                if (a.getStartedAt() != null) {
                    conn.rollback();
                    return false; // Already in progress (no-op)
                }

                // Check shift window: shift_start <= now <= shift_end
                if (now.isBefore(a.getShiftStart()) || now.isAfter(a.getShiftEnd())) {
                    throw new IllegalStateException("Cleaning can only start during your shift.");
                }

                assignmentDao.startAssignment(conn, assignmentId, now);

                LOGGER.info("Staff #" + staffId + " started cleaning for assignment #" + assignmentId + " for room " + a.getRoomNumber());

                conn.commit();
                return true;

            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to start cleaning for assignment #" + assignmentId, e);
                if (e instanceof SQLException) throw (SQLException) e;
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new SQLException("Failed to start cleaning session.", e);
            }
        }
    }

    /**
     * Completes cleaning: assignment COMPLETED, room AVAILABLE, credential EXPIRED.
     */
    public void completeCleaning(long staffId, long assignmentId, String ip) throws SQLException {
        LocalDateTime now = util.ClockService.now();

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Assignment a = assignmentDao.findById(conn, assignmentId);
                if (a == null || a.getStaffId() != staffId) {
                    throw new IllegalStateException("Assignment not found or does not belong to you.");
                }

                if (!Assignment.STATUS_ASSIGNED.equalsIgnoreCase(a.getStatus())) {
                    throw new IllegalStateException("Assignment cannot be completed from status " + a.getStatus() + ".");
                }

                if (a.getStartedAt() == null) {
                    throw new IllegalStateException("Cleaning must be started before completing.");
                }

                // 1. Complete assignment
                assignmentDao.completeAssignment(conn, assignmentId, now);

                // 2. Room status -> AVAILABLE
                String updRoomSql = "UPDATE ROOM SET status = 'AVAILABLE', updated_at = SYSDATETIME() "
                        + "WHERE room_id = ? AND status = 'NEEDS_CLEANING'";
                try (PreparedStatement psR = conn.prepareStatement(updRoomSql)) {
                    psR.setLong(1, a.getRoomId());
                    psR.executeUpdate();
                }

                // 3. Credential -> EXPIRED
                String updCredSql = "UPDATE ACCESS_CREDENTIAL SET status = 'EXPIRED', revoked_at = SYSDATETIME() "
                        + "WHERE staff_assignment_id = ? AND status IN ('PENDING', 'ACTIVE')";
                try (PreparedStatement psC = conn.prepareStatement(updCredSql)) {
                    psC.setLong(1, assignmentId);
                    psC.executeUpdate();
                }

                // 4. Audit log
                String auditDetail = "Cleaning completed by staff #" + staffId + " for room " + a.getRoomNumber() + " at " + now;
                LOGGER.info("Audit: " + auditDetail);

                conn.commit();

            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to complete cleaning for assignment #" + assignmentId, e);
                if (e instanceof SQLException) throw (SQLException) e;
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new SQLException("Failed to complete cleaning session.", e);
            }
        }
    }
}
