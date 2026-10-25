package model;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Domain model representing an APP_USER database record with its associated role name.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private int roleId;
    private String roleName;
    private String fullName;
    private String email;
    private String phone;
    private String passwordHash;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String position;
    private String employmentStatus;
    private Date hireDate;

    public User() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public Date getHireDate() {
        return hireDate;
    }

    public void setHireDate(Date hireDate) {
        this.hireDate = hireDate;
    }

    public AuthUser toAuthUser() {
        return new AuthUser(userId, fullName, email, roleName);
    }

    // =========================================================================
    // INNER DTO: AuthUser (merged into User)
    // =========================================================================

    /**
     * Minimal immutable session representation of an authenticated user.
     */
    public static class AuthUser implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Long userId;
        private final String fullName;
        private final String email;
        private final String roleName;

        public AuthUser(Long userId, String fullName, String email, String roleName) {
            this.userId = userId;
            this.fullName = fullName;
            this.email = email;
            this.roleName = roleName;
        }

        public Long getUserId() {
            return userId;
        }

        public String getFullName() {
            return fullName;
        }

        public String getEmail() {
            return email;
        }

        public String getRoleName() {
            return roleName;
        }
    }
}
