package service;

import dao.UserDao;
import model.User;
import util.Validator;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Authentication and registration business service.
 */
public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());
    private final UserDao userDao;

    public AuthService() {
        this.userDao = new UserDao();
    }

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public static final class Result {
        public final boolean ok;
        public final Long userId;
        public final String error;
        public final Map<String, String> fieldErrors;

        public Result(boolean ok, Long userId, String error, Map<String, String> fieldErrors) {
            this.ok = ok;
            this.userId = userId;
            this.error = error;
            this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.<String, String>emptyMap();
        }
    }

    public Result register(String fullName, String email, String phone, String password, String confirm, String ip) {
        Validator validator = new Validator();
        validator.required("fullName", fullName, "Full name")
                 .maxLength("fullName", fullName, 100, "Full name")
                 .required("email", email, "Email")
                 .email("email", email)
                 .required("phone", phone, "Phone number")
                 .phone("phone", phone)
                 .required("password", password, "Password")
                 .password("password", password)
                 .matches("confirm", confirm, password, "Passwords do not match.");

        if (validator.hasErrors()) {
            return new Result(false, null, null, validator.getErrors());
        }

        String cleanEmail = email.trim().toLowerCase();
        String cleanPhone = phone.trim();
        String cleanName = fullName.trim();

        try {
            if (userDao.findByEmail(cleanEmail) != null) {
                validator.addError("email", "This email is already registered.");
            }
            if (userDao.findByPhone(cleanPhone) != null) {
                validator.addError("phone", "This phone number is already registered.");
            }
            if (validator.hasErrors()) {
                return new Result(false, null, null, validator.getErrors());
            }

            String hash = BCrypt.hashpw(password, BCrypt.gensalt(10));
            User u = new User();
            u.setFullName(cleanName);
            u.setEmail(cleanEmail);
            u.setPhone(cleanPhone);
            u.setPasswordHash(hash);

            long newId = userDao.insertGuest(u);
            LOGGER.info("User registered: id=" + newId + " ip=" + ip);
            return new Result(true, newId, null, null);
        } catch (SQLException e) {
            if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                Map<String, String> conflictErrors = new LinkedHashMap<>();
                try {
                    if (userDao.findByEmail(cleanEmail) != null) {
                        conflictErrors.put("email", "This email is already registered.");
                    }
                    if (userDao.findByPhone(cleanPhone) != null) {
                        conflictErrors.put("phone", "This phone number is already registered.");
                    }
                } catch (SQLException ignored) {
                }
                if (conflictErrors.isEmpty()) {
                    conflictErrors.put("email", "This email or phone number is already registered.");
                }
                return new Result(false, null, null, conflictErrors);
            }
            LOGGER.log(Level.SEVERE, "Database error during guest registration", e);
            return new Result(false, null, "Registration failed. Please try again later.", null);
        }
    }

    public Result login(String email, String password, String ip) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            String loggedEmail = (email != null) ? email.trim().toLowerCase() : "";
            LOGGER.warning("Login failed: missing email or password, ip=" + ip);
            return new Result(false, null, "Invalid email or password.", null);
        }

        String cleanEmail = email.trim().toLowerCase();
        try {
            User user = userDao.findByEmail(cleanEmail);
            if (user == null) {
                LOGGER.warning("Login failed: unknown email " + cleanEmail + ", ip=" + ip);
                return new Result(false, null, "Invalid email or password.", null);
            }

            if ("LOCKED".equalsIgnoreCase(user.getStatus()) || "INACTIVE".equalsIgnoreCase(user.getStatus())) {
                LOGGER.warning("Login failed: user " + user.getUserId() + " status=" + user.getStatus() + ", ip=" + ip);
                return new Result(false, null, "This account is not active.", null);
            }

            if (user.getPasswordHash() == null || user.getPasswordHash().trim().isEmpty()) {
                LOGGER.warning("Login failed: user " + user.getUserId() + " no password set, ip=" + ip);
                return new Result(false, null, "Invalid email or password.", null);
            }

            boolean matched = false;
            try {
                matched = BCrypt.checkpw(password, user.getPasswordHash());
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Invalid BCrypt hash stored for user " + user.getUserId(), e);
                matched = false;
            }

            if (!matched) {
                LOGGER.warning("Login failed: user " + user.getUserId() + " wrong password, ip=" + ip);
                return new Result(false, null, "Invalid email or password.", null);
            }

            LOGGER.info("Login success: user " + user.getUserId() + ", ip=" + ip);
            return new Result(true, user.getUserId(), null, null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during login authentication", e);
            return new Result(false, null, "Authentication service is temporarily unavailable. Please try again.", null);
        }
    }
}
