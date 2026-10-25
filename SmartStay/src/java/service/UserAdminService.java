package service;

import dao.UserDao;
import util.PageResult.PageRequest;
import util.PageResult;
import model.User;
import util.DBUtil;
import util.Validator;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Service handling administration of users and staff profiles.
 */
public class UserAdminService {

    private static final Logger LOGGER = Logger.getLogger(UserAdminService.class.getName());

    private static final Set<String> ALLOWED_STATUSES = new HashSet<>(
            Arrays.asList("ACTIVE", "LOCKED", "INACTIVE")
    );

    private static final Set<String> ALLOWED_EMPLOYMENT_STATUSES = new HashSet<>(
            Arrays.asList("ACTIVE", "ON_LEAVE", "RESIGNED")
    );

    private final UserDao userDao;

    public UserAdminService() {
        this.userDao = new UserDao();
    }

    public UserAdminService(UserDao userDao) {
        this.userDao = userDao;
    }

    public static final class SaveResult {
        public final boolean ok;
        public final Long userId;
        public final String globalError;
        public final Map<String, String> fieldErrors;

        public SaveResult(boolean ok, Long userId, String globalError, Map<String, String> fieldErrors) {
            this.ok = ok;
            this.userId = userId;
            this.globalError = globalError;
            this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.<String, String>emptyMap();
        }
    }

    public PageResult<User> getUserPage(PageRequest pr) throws SQLException {
        return userDao.findAdminPage(pr);
    }

    public User getUserWithStaff(long userId) throws SQLException {
        return userDao.findByIdWithStaff(userId);
    }

    /**
     * Creates or updates a user and their staff profile in a single transaction.
     */
    public SaveResult saveUser(Long targetUserId,
                               String fullName,
                               String email,
                               String phone,
                               String targetRoleName,
                               String status,
                               String password,
                               String confirmPassword,
                               String position,
                               String employmentStatus,
                               String hireDateStr,
                               long actorUserId,
                               String actorRoleName,
                               String ip) throws SQLException {

        // 1. Role assignment permissions (Spec M8)
        if ("OWNER".equalsIgnoreCase(targetRoleName)) {
            throw new SecurityException("Owner accounts cannot be created or modified from the web interface.");
        }

        if ("ADMIN".equalsIgnoreCase(actorRoleName)) {
            // Admins may only assign GUEST or STAFF
            if (!"STAFF".equalsIgnoreCase(targetRoleName) && !"GUEST".equalsIgnoreCase(targetRoleName)) {
                LOGGER.warning("Admin attempted unauthorized role assignment: " + targetRoleName + " for user " + targetUserId);
                throw new SecurityException("Administrators are not authorized to assign role " + targetRoleName + ".");
            }
        }

        // 2. Validate actor editing an existing OWNER (ADDENDUM Rule 4: nobody can edit its role, deactivate or delete it)
        User existingUser = null;
        if (targetUserId != null) {
            existingUser = userDao.findByIdWithStaff(targetUserId);
            if (existingUser == null) {
                return new SaveResult(false, null, "User not found.", null);
            }
            if ("OWNER".equalsIgnoreCase(existingUser.getRoleName())) {
                if (!"OWNER".equalsIgnoreCase(actorRoleName)) {
                    throw new SecurityException("Only owners may view or modify owner accounts.");
                }
                if (!"OWNER".equalsIgnoreCase(targetRoleName)) {
                    throw new SecurityException("Owner account role cannot be modified.");
                }
                if (!"ACTIVE".equalsIgnoreCase(status)) {
                    throw new SecurityException("Owner account cannot be deactivated or locked.");
                }
            }
        }

        // 3. Self-lock guard: cannot delete or lock your own account
        if (targetUserId != null && targetUserId.equals(actorUserId)) {
            if (!"ACTIVE".equalsIgnoreCase(status)) {
                throw new IllegalStateException("You cannot delete or lock your own account.");
            }
        }

        // 4. Input validation
        Validator validator = new Validator();
        validator.required("fullName", fullName, "Full name")
                 .maxLength("fullName", fullName, 100, "Full name")
                 .required("email", email, "Email")
                 .email("email", email)
                 .required("phone", phone, "Phone number")
                 .phone("phone", phone);

        if (status == null || !ALLOWED_STATUSES.contains(status.trim().toUpperCase())) {
            validator.addError("status", "Invalid status selected.");
        }

        boolean updatePassword = false;
        if (targetUserId == null) {
            // Create: password is required
            validator.required("password", password, "Password")
                     .password("password", password)
                     .matches("confirmPassword", confirmPassword, password, "Passwords do not match.");
            updatePassword = true;
        } else {
            // Edit: optional password
            if (password != null && !password.trim().isEmpty()) {
                validator.password("password", password)
                         .matches("confirmPassword", confirmPassword, password, "Passwords do not match.");
                updatePassword = true;
            }
        }

        // Staff profile validation for STAFF and ADMIN
        boolean isStaffOrAdmin = "STAFF".equalsIgnoreCase(targetRoleName) || "ADMIN".equalsIgnoreCase(targetRoleName);
        Date hireDate = null;
        if (isStaffOrAdmin) {
            validator.required("position", position, "Position")
                     .maxLength("position", position, 80, "Position");

            if (employmentStatus == null || !ALLOWED_EMPLOYMENT_STATUSES.contains(employmentStatus.trim().toUpperCase())) {
                validator.addError("employmentStatus", "Invalid employment status selected.");
            }

            if (hireDateStr != null && !hireDateStr.trim().isEmpty()) {
                try {
                    LocalDate ld = LocalDate.parse(hireDateStr.trim());
                    if (ld.isAfter(LocalDate.now())) {
                        validator.addError("hireDate", "Hire date cannot be in the future.");
                    } else {
                        hireDate = Date.valueOf(ld);
                    }
                } catch (DateTimeParseException e) {
                    validator.addError("hireDate", "Invalid hire date format.");
                }
            }
        }

        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        String cleanPhone = phone != null ? phone.trim() : "";

        // Uniqueness pre-checks
        if (!cleanEmail.isEmpty() && userDao.existsEmailExcluding(cleanEmail, targetUserId)) {
            validator.addError("email", "This email is already registered.");
        }
        if (!cleanPhone.isEmpty() && userDao.existsPhoneExcluding(cleanPhone, targetUserId)) {
            validator.addError("phone", "This phone number is already registered.");
        }

        if (validator.hasErrors()) {
            return new SaveResult(false, null, null, validator.getErrors());
        }

        int roleId = userDao.getRoleIdByName(targetRoleName.trim().toUpperCase());
        if (roleId <= 0) {
            validator.addError("role", "Invalid role selected.");
            return new SaveResult(false, null, null, validator.getErrors());
        }

        // 5. Database transaction
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                User u = new User();
                u.setUserId(targetUserId);
                u.setRoleId(roleId);
                u.setRoleName(targetRoleName.trim().toUpperCase());
                u.setFullName(fullName.trim());
                u.setEmail(cleanEmail);
                u.setPhone(cleanPhone);
                u.setStatus(status.trim().toUpperCase());

                if (updatePassword) {
                    u.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt(10)));
                }

                long savedUserId;
                if (targetUserId == null) {
                    savedUserId = userDao.insert(conn, u);
                    if (isStaffOrAdmin) {
                        userDao.upsertStaffInfo(conn, savedUserId, position.trim(),
                                employmentStatus.trim().toUpperCase(), hireDate);
                    }
                    LOGGER.info("Created " + u.getRoleName() + " " + u.getEmail());
                } else {
                    savedUserId = targetUserId;
                    userDao.update(conn, u, updatePassword);
                    if (isStaffOrAdmin) {
                        userDao.upsertStaffInfo(conn, savedUserId, position.trim(),
                                employmentStatus.trim().toUpperCase(), hireDate);
                    } else {
                        userDao.deleteStaffInfo(conn, savedUserId);
                    }
                    LOGGER.info("Updated " + u.getRoleName() + " " + u.getEmail() + " (" + u.getStatus() + ")");
                }

                conn.commit();
                return new SaveResult(true, savedUserId, null, null);
            } catch (SQLException e) {
                conn.rollback();
                if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                    Map<String, String> conflictErrors = new LinkedHashMap<>();
                    if (userDao.existsEmailExcluding(cleanEmail, targetUserId)) {
                        conflictErrors.put("email", "This email is already registered.");
                    }
                    if (userDao.existsPhoneExcluding(cleanPhone, targetUserId)) {
                        conflictErrors.put("phone", "This phone number is already registered.");
                    }
                    if (conflictErrors.isEmpty()) {
                        conflictErrors.put("email", "This email or phone number is already registered.");
                    }
                    return new SaveResult(false, null, null, conflictErrors);
                }
                LOGGER.log(Level.SEVERE, "Failed to save user", e);
                throw e;
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Unexpected error saving user", e);
                throw new SQLException("Failed to save user", e);
            }
        }
    }

    /**
     * Deletes a user with foreign key and last-owner protection.
     */
    public void deleteUser(long targetUserId, long actorUserId, String actorRoleName, String ip) throws SQLException {
        if (targetUserId == actorUserId) {
            throw new IllegalStateException("You cannot delete or lock your own account.");
        }

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                User u = userDao.findById(targetUserId);
                if (u == null) {
                    throw new IllegalArgumentException("User not found.");
                }

                if ("OWNER".equalsIgnoreCase(u.getRoleName())) {
                    throw new SecurityException("Owner accounts are protected and cannot be deleted.");
                }

                userDao.delete(conn, targetUserId);

                LOGGER.info("Deleted user " + u.getEmail() + " (" + u.getRoleName() + ")");

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                if (e.getErrorCode() == 547) { // Foreign key constraint violation
                    throw new IllegalStateException("This user has related records and cannot be deleted. Lock the account instead.");
                }
                LOGGER.log(Level.SEVERE, "Database error deleting user " + targetUserId, e);
                throw e;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof IllegalStateException || e instanceof IllegalArgumentException || e instanceof SecurityException) {
                    throw (RuntimeException) e;
                }
                throw new SQLException("Failed to delete user", e);
            }
        }
    }
}
