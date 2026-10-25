/*
================================================================================
SmartStay Database Schema Definition (SQL Server 2017+)
Database: SmartStayDB
Specification: SmartStay_Web_Spec.md (Sections 2, 6, 7)

Execution Order:
  Run 01_create_schema.sql -> 02_seed_data.sql -> 03_verification_queries.sql

WARNING: This script DROPS and recreates SmartStayDB. All existing data in
SmartStayDB will be permanently replaced.
================================================================================
*/

USE master;
GO

IF DB_ID(N'SmartStayDB') IS NOT NULL
BEGIN
    ALTER DATABASE SmartStayDB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE SmartStayDB;
END
GO

CREATE DATABASE SmartStayDB;
GO

USE SmartStayDB;
GO

SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

/* ============================================================================
   1. ROLE
   System security roles for access control and authorization.
   ============================================================================ */
CREATE TABLE ROLE (
    role_id INT IDENTITY(1,1) NOT NULL,
    role_name VARCHAR(20) NOT NULL,
    description NVARCHAR(255) NULL,
    CONSTRAINT PK_ROLE PRIMARY KEY (role_id),
    CONSTRAINT UQ_ROLE_role_name UNIQUE (role_name),
    CONSTRAINT CK_ROLE_role_name CHECK (role_name IN ('OWNER','ADMIN','STAFF','GUEST'))
);
GO

/* ============================================================================
   2. PERMISSION
   Granular security permissions assigned to roles.
   All 30 permission codes (spec 7.2, seeded in T12):
     HOMESTAY_MANAGE, ROOM_MANAGE, ROOM_SLOT_MANAGE, BOOKING_VIEW_ALL,
     BOOKING_MANAGE, BOOKING_OWN, NOSHOW_INTERVENE, PAYMENT_VIEW,
     PAYMENT_RESOLVE, USER_MANAGE, ASSIGNMENT_MANAGE, ASSIGNMENT_OWN,
     CLEANING_PERFORM, CREDENTIAL_VIEW_META, CREDENTIAL_REVOKE, DEVICE_MANAGE,
     THRESHOLD_MANAGE, LOG_ACCESS_VIEW, LOG_MOTION_VIEW, SENSOR_VIEW,
     ALERT_VIEW, ALERT_HANDLE, SNAPSHOT_VIEW, REPORT_REVENUE, DASHBOARD_VIEW,
     AUDIT_VIEW, CONFIG_VIEW, CONFIG_MANAGE, REVIEW_CREATE, REVIEW_MODERATE
   ============================================================================ */
CREATE TABLE PERMISSION (
    permission_id INT IDENTITY(1,1) NOT NULL,
    permission_code VARCHAR(80) NOT NULL,
    description NVARCHAR(255) NULL,
    CONSTRAINT PK_PERMISSION PRIMARY KEY (permission_id),
    CONSTRAINT UQ_PERMISSION_permission_code UNIQUE (permission_code)
);
GO

/* ============================================================================
   3. ROLE_PERMISSION
   Mapping between security roles and granular permissions.
   ============================================================================ */
CREATE TABLE ROLE_PERMISSION (
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    CONSTRAINT PK_ROLE_PERMISSION PRIMARY KEY (role_id, permission_id),
    CONSTRAINT FK_ROLE_PERMISSION_ROLE FOREIGN KEY (role_id) REFERENCES ROLE(role_id),
    CONSTRAINT FK_ROLE_PERMISSION_PERMISSION FOREIGN KEY (permission_id) REFERENCES PERMISSION(permission_id)
);
GO

/* ============================================================================
   4. APP_USER
   System accounts for guests, staff, admins, and owner.
   [APP] login = email + BCrypt; only one OWNER (not creatable from UI).
   ============================================================================ */
CREATE TABLE APP_USER (
    user_id BIGINT IDENTITY(1,1) NOT NULL,
    role_id INT NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    email NVARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL CONSTRAINT DF_APP_USER_status DEFAULT 'ACTIVE',
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_APP_USER_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(0) NULL,
    position NVARCHAR(80) NULL,
    employment_status VARCHAR(20) NULL CONSTRAINT DF_APP_USER_employment_status DEFAULT 'ACTIVE',
    hire_date DATE NULL,
    CONSTRAINT PK_APP_USER PRIMARY KEY (user_id),
    CONSTRAINT FK_APP_USER_ROLE FOREIGN KEY (role_id) REFERENCES ROLE(role_id),
    CONSTRAINT UQ_APP_USER_email UNIQUE (email),
    CONSTRAINT UQ_APP_USER_phone UNIQUE (phone),
    CONSTRAINT CK_APP_USER_status CHECK (status IN ('ACTIVE','LOCKED','INACTIVE')),
    CONSTRAINT CK_APP_USER_employment_status CHECK (employment_status IS NULL OR employment_status IN ('ACTIVE','ON_LEAVE','RESIGNED')),
    CONSTRAINT CK_APP_USER_phone CHECK (phone NOT LIKE '%[^0-9+]%' AND LEN(phone) BETWEEN 10 AND 13),
    CONSTRAINT CK_APP_USER_email CHECK (email LIKE '%_@_%._%')
);
GO

/* ============================================================================
   6. ROOM
   Rental rooms within the property.
   ============================================================================ */
CREATE TABLE ROOM (
    room_id BIGINT IDENTITY(1,1) NOT NULL,
    room_number VARCHAR(20) NOT NULL,
    display_name NVARCHAR(100) NOT NULL,
    floor_number INT NOT NULL,
    location_description NVARCHAR(255) NULL,
    capacity INT NOT NULL CONSTRAINT DF_ROOM_capacity DEFAULT 2,
    description NVARCHAR(500) NULL,
    amenities NVARCHAR(500) NULL,
    image_url NVARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL CONSTRAINT DF_ROOM_status DEFAULT 'AVAILABLE',
    is_active BIT NOT NULL CONSTRAINT DF_ROOM_is_active DEFAULT 1,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_ROOM_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(0) NULL,
    CONSTRAINT PK_ROOM PRIMARY KEY (room_id),
    CONSTRAINT UQ_ROOM_room_number UNIQUE (room_number),
    CONSTRAINT CK_ROOM_capacity CHECK (capacity >= 1),
    CONSTRAINT CK_ROOM_floor_number CHECK (floor_number >= 0),
    CONSTRAINT CK_ROOM_status CHECK (status IN ('AVAILABLE','OCCUPIED','NEEDS_CLEANING','MAINTENANCE'))
);
GO

CREATE INDEX IX_ROOM_status ON ROOM(status);
GO

/* ============================================================================
   8. ROOM_TIME_SLOT
   Predefined daily time slots and pricing per room (S1 to S4).
   [APP] no overlap between slots of one room (incl. S4 into next morning vs next day's S1) - checked in Java (T07) and in 03 verification.
   ============================================================================ */
CREATE TABLE ROOM_TIME_SLOT (
    room_slot_id BIGINT IDENTITY(1,1) NOT NULL,
    room_id BIGINT NOT NULL,
    slot_code CHAR(2) NOT NULL,
    slot_name NVARCHAR(50) NOT NULL,
    start_time TIME(0) NOT NULL,
    end_time TIME(0) NOT NULL,
    crosses_midnight BIT NOT NULL CONSTRAINT DF_ROOM_TIME_SLOT_crosses_midnight DEFAULT 0,
    list_price DECIMAL(12,0) NOT NULL,
    sort_order TINYINT NOT NULL,
    is_active BIT NOT NULL CONSTRAINT DF_ROOM_TIME_SLOT_is_active DEFAULT 1,
    status VARCHAR(20) NOT NULL CONSTRAINT DF_ROOM_TIME_SLOT_status DEFAULT 'AVAILABLE',
    updated_by BIGINT NULL,
    updated_at DATETIME2(0) NULL,
    CONSTRAINT PK_ROOM_TIME_SLOT PRIMARY KEY (room_slot_id),
    CONSTRAINT FK_ROOM_TIME_SLOT_ROOM FOREIGN KEY (room_id) REFERENCES ROOM(room_id),
    CONSTRAINT FK_ROOM_TIME_SLOT_APP_USER FOREIGN KEY (updated_by) REFERENCES APP_USER(user_id),
    CONSTRAINT UQ_ROOM_TIME_SLOT_room_slot_code UNIQUE (room_id, slot_code),
    CONSTRAINT UQ_ROOM_TIME_SLOT_room_sort_order UNIQUE (room_id, sort_order),
    CONSTRAINT CK_ROOM_TIME_SLOT_slot_code CHECK (slot_code IN ('S1','S2','S3','S4')),
    CONSTRAINT CK_ROOM_TIME_SLOT_status CHECK (status IN ('AVAILABLE','NEEDS_CLEANING','MAINTENANCE')),
    CONSTRAINT CK_ROOM_TIME_SLOT_sort_order CHECK (sort_order BETWEEN 1 AND 4),
    CONSTRAINT CK_ROOM_TIME_SLOT_slot_code_sort_order CHECK (slot_code = 'S' + CAST(sort_order AS VARCHAR(1))),
    CONSTRAINT CK_ROOM_TIME_SLOT_list_price CHECK (list_price > 0),
    CONSTRAINT CK_ROOM_TIME_SLOT_times CHECK (
        (crosses_midnight = 0 AND end_time > start_time) OR
        (crosses_midnight = 1 AND end_time < start_time)
    )
);
GO


/* ============================================================================
   10. BOOKING
   Room booking transactions with multi-slot discount rules.
   [APP] discount comes from application constants at booking time (snapshot);
         discount_amount = ROUND HALF_UP(subtotal * percent / 100);
         slots must be consecutive (spec 2.2/2.3).
   [APP] "No-show held" = status='EXPIRED' AND expire_reason='NO_SHOW' with at least one BOOKING_SLOT HELD.
   ============================================================================ */
CREATE TABLE BOOKING (
    booking_id BIGINT IDENTITY(1,1) NOT NULL,
    booking_code AS ('BK' + RIGHT('000000' + CAST(booking_id AS VARCHAR(10)), 6)) PERSISTED,
    guest_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    slot_date DATE NOT NULL,
    checkin_at DATETIME2(0) NOT NULL,
    checkout_at DATETIME2(0) NOT NULL,
    actual_checkin_at DATETIME2(0) NULL,
    actual_checkout_at DATETIME2(0) NULL,
    subtotal_amount DECIMAL(12,0) NOT NULL,
    discount_percent TINYINT NOT NULL CONSTRAINT DF_BOOKING_discount_percent DEFAULT 0,
    discount_amount DECIMAL(12,0) NOT NULL CONSTRAINT DF_BOOKING_discount_amount DEFAULT 0,
    total_amount DECIMAL(12,0) NOT NULL,
    status VARCHAR(30) NOT NULL CONSTRAINT DF_BOOKING_status DEFAULT 'PENDING_PAYMENT',
    allow_early_checkin BIT NOT NULL CONSTRAINT DF_BOOKING_allow_early_checkin DEFAULT 0,
    expire_reason VARCHAR(20) NULL,
    payment_deadline DATETIME2(0) NULL,
    note NVARCHAR(500) NULL,
    cancel_reason NVARCHAR(255) NULL,
    cancelled_at DATETIME2(0) NULL,
    cancelled_by BIGINT NULL,
    created_by BIGINT NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_BOOKING_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(0) NULL,
    CONSTRAINT PK_BOOKING PRIMARY KEY (booking_id),
    CONSTRAINT UQ_BOOKING_booking_code UNIQUE (booking_code),
    CONSTRAINT FK_BOOKING_APP_USER_guest FOREIGN KEY (guest_id) REFERENCES APP_USER(user_id),
    CONSTRAINT FK_BOOKING_ROOM FOREIGN KEY (room_id) REFERENCES ROOM(room_id),
    CONSTRAINT FK_BOOKING_APP_USER_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES APP_USER(user_id),
    CONSTRAINT FK_BOOKING_APP_USER_created_by FOREIGN KEY (created_by) REFERENCES APP_USER(user_id),
    CONSTRAINT CK_BOOKING_status CHECK (status IN ('PENDING_PAYMENT','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED','EXPIRED')),
    CONSTRAINT CK_BOOKING_expire_reason CHECK (
        (expire_reason IS NULL AND status <> 'EXPIRED') OR
        (status = 'EXPIRED' AND expire_reason IN ('PAYMENT_TIMEOUT','NO_SHOW'))
    ),
    CONSTRAINT CK_BOOKING_discount_percent CHECK (discount_percent IN (0,10,20,50)),
    CONSTRAINT CK_BOOKING_amounts CHECK (
        subtotal_amount > 0 AND
        discount_amount >= 0 AND
        total_amount = subtotal_amount - discount_amount
    ),
    CONSTRAINT CK_BOOKING_schedule CHECK (checkout_at > checkin_at),
    CONSTRAINT CK_BOOKING_actual_times CHECK (
        actual_checkout_at IS NULL OR
        actual_checkin_at IS NULL OR
        actual_checkout_at >= actual_checkin_at
    )
);
GO

CREATE INDEX IX_BOOKING_room_slot_date ON BOOKING(room_id, slot_date);
CREATE INDEX IX_BOOKING_guest_created ON BOOKING(guest_id, created_at DESC);
CREATE INDEX IX_BOOKING_status ON BOOKING(status);
GO

/* ============================================================================
   11. BOOKING_SLOT
   Individual slots reserved by a booking.
   Anti double-booking: UX_BOOKING_SLOT_held filtered unique index prevents
   multiple HELD bookings for the same room_slot_id and slot_date.
   [APP] room_id/slot_date duplicate BOOKING's values on purpose (index needs them);
         service must keep them equal. No-show bookings keep HELD until Admin releases.
   ============================================================================ */
CREATE TABLE BOOKING_SLOT (
    booking_slot_id BIGINT IDENTITY(1,1) NOT NULL,
    booking_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    room_slot_id BIGINT NOT NULL,
    slot_date DATE NOT NULL,
    start_at DATETIME2(0) NOT NULL,
    end_at DATETIME2(0) NOT NULL,
    list_price DECIMAL(12,0) NOT NULL,
    hold_status VARCHAR(10) NOT NULL CONSTRAINT DF_BOOKING_SLOT_hold_status DEFAULT 'HELD',
    released_at DATETIME2(0) NULL,
    CONSTRAINT PK_BOOKING_SLOT PRIMARY KEY (booking_slot_id),
    CONSTRAINT FK_BOOKING_SLOT_BOOKING FOREIGN KEY (booking_id) REFERENCES BOOKING(booking_id),
    CONSTRAINT FK_BOOKING_SLOT_ROOM FOREIGN KEY (room_id) REFERENCES ROOM(room_id),
    CONSTRAINT FK_BOOKING_SLOT_ROOM_TIME_SLOT FOREIGN KEY (room_slot_id) REFERENCES ROOM_TIME_SLOT(room_slot_id),
    CONSTRAINT UQ_BOOKING_SLOT_booking_room_slot UNIQUE (booking_id, room_slot_id),
    CONSTRAINT CK_BOOKING_SLOT_hold_status CHECK (hold_status IN ('HELD','RELEASED')),
    CONSTRAINT CK_BOOKING_SLOT_times CHECK (end_at > start_at),
    CONSTRAINT CK_BOOKING_SLOT_list_price CHECK (list_price > 0)
);
GO

CREATE UNIQUE INDEX UX_BOOKING_SLOT_held
    ON BOOKING_SLOT(room_slot_id, slot_date)
    WHERE hold_status = 'HELD';

CREATE INDEX IX_BOOKING_SLOT_room_date
    ON BOOKING_SLOT(room_id, slot_date)
    INCLUDE (hold_status);
GO

/* ============================================================================
   12. PAYMENT
   Payment transaction attempts via Mock Gateway or PayOS.
   [APP] amount must equal BOOKING.total_amount; each attempt has its own order code.
   ============================================================================ */
CREATE TABLE PAYMENT (
    payment_id BIGINT IDENTITY(1,1) NOT NULL,
    booking_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL CONSTRAINT DF_PAYMENT_provider DEFAULT 'MOCK',
    provider_order_code BIGINT NOT NULL,
    payment_link_id VARCHAR(100) NULL,
    amount DECIMAL(12,0) NOT NULL,
    currency CHAR(3) NOT NULL CONSTRAINT DF_PAYMENT_currency DEFAULT 'VND',
    status VARCHAR(30) NOT NULL CONSTRAINT DF_PAYMENT_status DEFAULT 'PENDING',
    transaction_reference VARCHAR(100) NULL,
    expires_at DATETIME2(0) NOT NULL,
    paid_at DATETIME2(0) NULL,
    resolution_note NVARCHAR(500) NULL,
    resolved_by BIGINT NULL,
    resolved_at DATETIME2(0) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_PAYMENT_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(0) NULL,
    CONSTRAINT PK_PAYMENT PRIMARY KEY (payment_id),
    CONSTRAINT FK_PAYMENT_BOOKING FOREIGN KEY (booking_id) REFERENCES BOOKING(booking_id),
    CONSTRAINT FK_PAYMENT_APP_USER_resolved_by FOREIGN KEY (resolved_by) REFERENCES APP_USER(user_id),
    CONSTRAINT UQ_PAYMENT_provider_order_code UNIQUE (provider_order_code),
    CONSTRAINT CK_PAYMENT_provider CHECK (provider IN ('MOCK','PAYOS')),
    CONSTRAINT CK_PAYMENT_currency CHECK (currency = 'VND'),
    CONSTRAINT CK_PAYMENT_amount CHECK (amount > 0),
    CONSTRAINT CK_PAYMENT_status CHECK (status IN ('PENDING','PAID','FAILED','EXPIRED','REVIEW_REQUIRED','REFUND_PENDING','REFUNDED')),
    CONSTRAINT CK_PAYMENT_paid_at CHECK (status <> 'PAID' OR paid_at IS NOT NULL)
);
GO

CREATE INDEX IX_PAYMENT_booking ON PAYMENT(booking_id);

CREATE INDEX IX_PAYMENT_paid_at
    ON PAYMENT(paid_at)
    INCLUDE (amount)
    WHERE status = 'PAID';
GO


/* ============================================================================
   14. DEVICE
   Smart hardware devices (Smart Locks, Security Cameras).
   ============================================================================ */
CREATE TABLE DEVICE (
    device_id VARCHAR(50) NOT NULL,
    device_name NVARCHAR(100) NOT NULL,
    device_type VARCHAR(30) NOT NULL,
    zone_id VARCHAR(30) NOT NULL,
    room_id BIGINT NULL,
    status VARCHAR(20) NOT NULL CONSTRAINT DF_DEVICE_status DEFAULT 'OFFLINE',
    api_key_hash VARCHAR(100) NULL,
    is_active BIT NOT NULL CONSTRAINT DF_DEVICE_is_active DEFAULT 1,
    last_seen_at DATETIME2(0) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_DEVICE_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(0) NULL,
    CONSTRAINT PK_DEVICE PRIMARY KEY (device_id),
    CONSTRAINT FK_DEVICE_ROOM FOREIGN KEY (room_id) REFERENCES ROOM(room_id),
    CONSTRAINT CK_DEVICE_device_id CHECK (
        device_id COLLATE Latin1_General_BIN NOT LIKE '%[^A-Z0-9_]%' AND
        LEN(device_id) BETWEEN 3 AND 50
    ),
    CONSTRAINT CK_DEVICE_device_type CHECK (device_type IN ('SMART_LOCK','SECURITY_CAMERA')),
    CONSTRAINT CK_DEVICE_status CHECK (status IN ('ONLINE','OFFLINE','FAULT')),
    CONSTRAINT CK_DEVICE_zone_id CHECK (zone_id IN ('MAIN_ENTRANCE','LOBBY'))
);
GO

/* ============================================================================
   15. STAFF_ASSIGNMENT
   Cleaning task assignments for staff members.
   [APP] staff must have role STAFF; room must be NEEDS_CLEANING at creation.
   ============================================================================ */
CREATE TABLE STAFF_ASSIGNMENT (
    assignment_id BIGINT IDENTITY(1,1) NOT NULL,
    staff_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    assigned_by BIGINT NOT NULL,
    shift_start DATETIME2(0) NOT NULL,
    shift_end DATETIME2(0) NOT NULL,
    status VARCHAR(20) NOT NULL CONSTRAINT DF_STAFF_ASSIGNMENT_status DEFAULT 'ASSIGNED',
    note NVARCHAR(255) NULL,
    started_at DATETIME2(0) NULL,
    completed_at DATETIME2(0) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_STAFF_ASSIGNMENT_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(0) NULL,
    CONSTRAINT PK_STAFF_ASSIGNMENT PRIMARY KEY (assignment_id),
    CONSTRAINT FK_STAFF_ASSIGNMENT_APP_USER_staff FOREIGN KEY (staff_id) REFERENCES APP_USER(user_id),
    CONSTRAINT FK_STAFF_ASSIGNMENT_ROOM FOREIGN KEY (room_id) REFERENCES ROOM(room_id),
    CONSTRAINT FK_STAFF_ASSIGNMENT_APP_USER_assigned_by FOREIGN KEY (assigned_by) REFERENCES APP_USER(user_id),
    CONSTRAINT CK_STAFF_ASSIGNMENT_shift CHECK (shift_end > shift_start),
    CONSTRAINT CK_STAFF_ASSIGNMENT_status CHECK (status IN ('ASSIGNED','COMPLETED','CANCELLED','EXPIRED')),
    CONSTRAINT CK_STAFF_ASSIGNMENT_completed CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL)
);
GO

CREATE INDEX IX_STAFF_ASSIGNMENT_staff_shift ON STAFF_ASSIGNMENT(staff_id, shift_start DESC);
CREATE INDEX IX_STAFF_ASSIGNMENT_room ON STAFF_ASSIGNMENT(room_id);
GO

/* ============================================================================
   17. ACCESS_CREDENTIAL
   Hashed and encrypted access PIN credentials for guests and staff.
   Live credentials must have secrets and cannot have duplicate live PINs on device.
   ============================================================================ */
CREATE TABLE ACCESS_CREDENTIAL (
    credential_id BIGINT IDENTITY(1,1) NOT NULL,
    credential_type VARCHAR(20) NOT NULL,
    booking_id BIGINT NULL,
    staff_assignment_id BIGINT NULL,
    device_id VARCHAR(50) NOT NULL,
    pin_ciphertext VARBINARY(256) NULL,
    pin_hmac VARBINARY(64) NULL,
    valid_from DATETIME2(0) NOT NULL,
    valid_until DATETIME2(0) NOT NULL,
    use_policy VARCHAR(20) NOT NULL CONSTRAINT DF_ACCESS_CREDENTIAL_use_policy DEFAULT 'MULTI_USE',
    status VARCHAR(20) NOT NULL CONSTRAINT DF_ACCESS_CREDENTIAL_status DEFAULT 'PENDING',
    used_count INT NOT NULL CONSTRAINT DF_ACCESS_CREDENTIAL_used_count DEFAULT 0,
    revoked_at DATETIME2(0) NULL,
    revoke_reason VARCHAR(50) NULL,
    secret_purged_at DATETIME2(0) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_ACCESS_CREDENTIAL_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_ACCESS_CREDENTIAL PRIMARY KEY (credential_id),
    CONSTRAINT FK_ACCESS_CREDENTIAL_BOOKING FOREIGN KEY (booking_id) REFERENCES BOOKING(booking_id),
    CONSTRAINT FK_ACCESS_CREDENTIAL_STAFF_ASSIGNMENT FOREIGN KEY (staff_assignment_id) REFERENCES STAFF_ASSIGNMENT(assignment_id),
    CONSTRAINT FK_ACCESS_CREDENTIAL_DEVICE FOREIGN KEY (device_id) REFERENCES DEVICE(device_id),
    CONSTRAINT CK_ACCESS_CREDENTIAL_type CHECK (credential_type IN ('GUEST_PIN','STAFF_PIN')),
    CONSTRAINT CK_ACCESS_CREDENTIAL_use_policy CHECK (use_policy IN ('MULTI_USE')),
    CONSTRAINT CK_ACCESS_CREDENTIAL_status CHECK (status IN ('PENDING','ACTIVE','EXPIRED','REVOKED')),
    CONSTRAINT CK_ACCESS_CREDENTIAL_owner CHECK (
        (booking_id IS NOT NULL AND staff_assignment_id IS NULL AND credential_type = 'GUEST_PIN') OR
        (booking_id IS NULL AND staff_assignment_id IS NOT NULL AND credential_type = 'STAFF_PIN')
    ),
    CONSTRAINT CK_ACCESS_CREDENTIAL_validity CHECK (valid_until > valid_from),
    CONSTRAINT CK_ACCESS_CREDENTIAL_used_count CHECK (used_count >= 0),
    CONSTRAINT CK_ACCESS_CREDENTIAL_revoke_reason CHECK (
        revoke_reason IS NULL OR
        revoke_reason IN ('NO_SHOW','CANCELLED','CHECKED_OUT','ADMIN_REVOKE','REINSTATED','ASSIGNMENT_ENDED')
    ),
    CONSTRAINT CK_ACCESS_CREDENTIAL_secrets CHECK (
        status NOT IN ('PENDING','ACTIVE') OR
        (pin_ciphertext IS NOT NULL AND pin_hmac IS NOT NULL)
    )
);
GO

CREATE UNIQUE INDEX UX_ACCESS_CREDENTIAL_live_pin
    ON ACCESS_CREDENTIAL(device_id, pin_hmac)
    WHERE status IN ('PENDING','ACTIVE');

CREATE UNIQUE INDEX UX_ACCESS_CREDENTIAL_live_booking
    ON ACCESS_CREDENTIAL(booking_id)
    WHERE status IN ('PENDING','ACTIVE') AND booking_id IS NOT NULL;

CREATE INDEX IX_ACCESS_CREDENTIAL_status_valid
    ON ACCESS_CREDENTIAL(status, valid_from, valid_until);
GO

/* ============================================================================
   18. ACCESS_LOG
   Door unlock attempt history. Never stores raw PINs.
   ============================================================================ */
CREATE TABLE ACCESS_LOG (
    access_log_id BIGINT IDENTITY(1,1) NOT NULL,
    credential_id BIGINT NULL,
    device_id VARCHAR(50) NOT NULL,
    request_id VARCHAR(80) NOT NULL,
    attempted_at DATETIME2(3) NOT NULL CONSTRAINT DF_ACCESS_LOG_attempted_at DEFAULT SYSDATETIME(),
    result VARCHAR(10) NOT NULL,
    reason_code VARCHAR(30) NOT NULL,
    CONSTRAINT PK_ACCESS_LOG PRIMARY KEY (access_log_id),
    CONSTRAINT FK_ACCESS_LOG_ACCESS_CREDENTIAL FOREIGN KEY (credential_id) REFERENCES ACCESS_CREDENTIAL(credential_id),
    CONSTRAINT FK_ACCESS_LOG_DEVICE FOREIGN KEY (device_id) REFERENCES DEVICE(device_id),
    CONSTRAINT UQ_ACCESS_LOG_request_id UNIQUE (request_id),
    CONSTRAINT CK_ACCESS_LOG_result CHECK (result IN ('ALLOWED','DENIED')),
    CONSTRAINT CK_ACCESS_LOG_reason_code CHECK (reason_code IN ('OK','WRONG_PIN','EXPIRED','NOT_YET_VALID','REVOKED','LOCKED_OUT')),
    CONSTRAINT CK_ACCESS_LOG_result_reason CHECK (
        (result = 'ALLOWED' AND reason_code = 'OK') OR
        (result = 'DENIED' AND reason_code <> 'OK')
    )
);
GO

CREATE INDEX IX_ACCESS_LOG_device_time ON ACCESS_LOG(device_id, attempted_at DESC);
CREATE INDEX IX_ACCESS_LOG_credential ON ACCESS_LOG(credential_id);
GO

/* ============================================================================
   19. DOOR_EVENT
   Physical door contact sensor state changes (OPEN, CLOSED).
   ============================================================================ */
CREATE TABLE DOOR_EVENT (
    door_event_id BIGINT IDENTITY(1,1) NOT NULL,
    device_id VARCHAR(50) NOT NULL,
    access_log_id BIGINT NULL,
    request_id VARCHAR(80) NULL,
    event_type VARCHAR(10) NOT NULL,
    occurred_at DATETIME2(3) NOT NULL,
    received_at DATETIME2(3) NOT NULL CONSTRAINT DF_DOOR_EVENT_received_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_DOOR_EVENT PRIMARY KEY (door_event_id),
    CONSTRAINT FK_DOOR_EVENT_DEVICE FOREIGN KEY (device_id) REFERENCES DEVICE(device_id),
    CONSTRAINT FK_DOOR_EVENT_ACCESS_LOG FOREIGN KEY (access_log_id) REFERENCES ACCESS_LOG(access_log_id),
    CONSTRAINT CK_DOOR_EVENT_event_type CHECK (event_type IN ('OPEN','CLOSED'))
);
GO

CREATE INDEX IX_DOOR_EVENT_device_time ON DOOR_EVENT(device_id, occurred_at DESC);
GO

/* ============================================================================
   20. MOTION_LOG
   PIR motion sensor detections with ultrasonic distance measurements.
   ============================================================================ */
CREATE TABLE MOTION_LOG (
    motion_log_id BIGINT IDENTITY(1,1) NOT NULL,
    device_id VARCHAR(50) NOT NULL,
    zone_id VARCHAR(30) NOT NULL CONSTRAINT DF_MOTION_LOG_zone_id DEFAULT 'LOBBY',
    event_id VARCHAR(80) NOT NULL,
    detected_at DATETIME2(3) NOT NULL,
    distance_cm DECIMAL(10,2) NULL,
    snapshot_path NVARCHAR(500) NULL,
    received_at DATETIME2(3) NOT NULL CONSTRAINT DF_MOTION_LOG_received_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_MOTION_LOG PRIMARY KEY (motion_log_id),
    CONSTRAINT FK_MOTION_LOG_DEVICE FOREIGN KEY (device_id) REFERENCES DEVICE(device_id),
    CONSTRAINT UQ_MOTION_LOG_event_id UNIQUE (event_id),
    CONSTRAINT CK_MOTION_LOG_zone_id CHECK (zone_id IN ('MAIN_ENTRANCE','LOBBY')),
    CONSTRAINT CK_MOTION_LOG_distance_cm CHECK (distance_cm IS NULL OR distance_cm >= 0)
);
GO

CREATE INDEX IX_MOTION_LOG_device_time ON MOTION_LOG(device_id, detected_at DESC);
GO

/* ============================================================================
   20. ALERT_THRESHOLD
   Configurable rules triggering security alerts based on telemetry metrics.
   ============================================================================ */
CREATE TABLE ALERT_THRESHOLD (
    threshold_id BIGINT IDENTITY(1,1) NOT NULL,
    device_id VARCHAR(50) NULL,
    metric_type VARCHAR(30) NOT NULL,
    operator VARCHAR(3) NOT NULL,
    threshold_value DECIMAL(10,2) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(10) NOT NULL,
    enabled BIT NOT NULL CONSTRAINT DF_ALERT_THRESHOLD_enabled DEFAULT 1,
    description NVARCHAR(255) NULL,
    updated_by BIGINT NULL,
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_ALERT_THRESHOLD_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_ALERT_THRESHOLD PRIMARY KEY (threshold_id),
    CONSTRAINT FK_ALERT_THRESHOLD_DEVICE FOREIGN KEY (device_id) REFERENCES DEVICE(device_id),
    CONSTRAINT FK_ALERT_THRESHOLD_APP_USER FOREIGN KEY (updated_by) REFERENCES APP_USER(user_id),
    CONSTRAINT CK_ALERT_THRESHOLD_metric_type CHECK (metric_type IN ('DISTANCE_CM','MOTION','DOOR_OPEN_SECONDS','PIR_TRIGGER')),
    CONSTRAINT CK_ALERT_THRESHOLD_operator CHECK (operator IN ('GT','GTE','LT','LTE')),
    CONSTRAINT CK_ALERT_THRESHOLD_severity CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT CK_ALERT_THRESHOLD_alert_type CHECK (
        alert_type IN (
            'PIN_BRUTE_FORCE','DOOR_OPEN_ABNORMAL','DOOR_OPEN_TOO_LONG','LOBBY_PROXIMITY',
            'SUSPICIOUS_MOTION','CLEANING_SESSION_EXPIRED','PAYMENT_REVIEW_REQUIRED',
            'BOOKING_NO_SHOW','DEVICE_OFFLINE','DEVICE_FAULT'
        )
    )
);
GO

CREATE INDEX IX_ALERT_THRESHOLD_metric_enabled ON ALERT_THRESHOLD(metric_type, enabled);
GO

/* ============================================================================
   21. ALERT_LOG
   Security alerts and lifecycle management (NEW -> ACKNOWLEDGED -> RESOLVED).
   ============================================================================ */
CREATE TABLE ALERT_LOG (
    alert_id BIGINT IDENTITY(1,1) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL CONSTRAINT DF_ALERT_LOG_status DEFAULT 'NEW',
    message NVARCHAR(500) NOT NULL,
    device_id VARCHAR(50) NULL,
    room_id BIGINT NULL,
    zone_id VARCHAR(30) NULL,
    booking_id BIGINT NULL,
    motion_log_id BIGINT NULL,
    threshold_id BIGINT NULL,
    snapshot_path NVARCHAR(500) NULL,
    acknowledged_by BIGINT NULL,
    acknowledged_at DATETIME2(0) NULL,
    resolved_by BIGINT NULL,
    resolved_at DATETIME2(0) NULL,
    resolution_note NVARCHAR(500) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_ALERT_LOG_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_ALERT_LOG PRIMARY KEY (alert_id),
    CONSTRAINT FK_ALERT_LOG_DEVICE FOREIGN KEY (device_id) REFERENCES DEVICE(device_id),
    CONSTRAINT FK_ALERT_LOG_ROOM FOREIGN KEY (room_id) REFERENCES ROOM(room_id),
    CONSTRAINT FK_ALERT_LOG_BOOKING FOREIGN KEY (booking_id) REFERENCES BOOKING(booking_id),
    CONSTRAINT FK_ALERT_LOG_MOTION_LOG FOREIGN KEY (motion_log_id) REFERENCES MOTION_LOG(motion_log_id),
    CONSTRAINT FK_ALERT_LOG_ALERT_THRESHOLD FOREIGN KEY (threshold_id) REFERENCES ALERT_THRESHOLD(threshold_id),
    CONSTRAINT FK_ALERT_LOG_APP_USER_acknowledged_by FOREIGN KEY (acknowledged_by) REFERENCES APP_USER(user_id),
    CONSTRAINT FK_ALERT_LOG_APP_USER_resolved_by FOREIGN KEY (resolved_by) REFERENCES APP_USER(user_id),
    CONSTRAINT CK_ALERT_LOG_alert_type CHECK (
        alert_type IN (
            'PIN_BRUTE_FORCE','DOOR_OPEN_ABNORMAL','DOOR_OPEN_TOO_LONG','LOBBY_PROXIMITY',
            'SUSPICIOUS_MOTION','CLEANING_SESSION_EXPIRED','PAYMENT_REVIEW_REQUIRED',
            'BOOKING_NO_SHOW','DEVICE_OFFLINE','DEVICE_FAULT'
        )
    ),
    CONSTRAINT CK_ALERT_LOG_severity CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT CK_ALERT_LOG_status CHECK (status IN ('NEW','ACKNOWLEDGED','RESOLVED')),
    CONSTRAINT CK_ALERT_LOG_acknowledged CHECK (status = 'NEW' OR acknowledged_at IS NOT NULL),
    CONSTRAINT CK_ALERT_LOG_resolved CHECK (status <> 'RESOLVED' OR resolved_at IS NOT NULL)
);
GO

CREATE INDEX IX_ALERT_LOG_status_created ON ALERT_LOG(status, created_at DESC);
CREATE INDEX IX_ALERT_LOG_type_created ON ALERT_LOG(alert_type, created_at DESC);
GO

/* ============================================================================
   24. REVIEW
   Guest ratings and reviews for completed bookings.
   [APP] booking must be CHECKED_OUT and owned by guest_id; room_id copied from booking.
   ============================================================================ */
CREATE TABLE REVIEW (
    review_id BIGINT IDENTITY(1,1) NOT NULL,
    booking_id BIGINT NOT NULL,
    guest_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    comment NVARCHAR(1000) NULL,
    status VARCHAR(10) NOT NULL CONSTRAINT DF_REVIEW_status DEFAULT 'VISIBLE',
    moderated_by BIGINT NULL,
    moderated_at DATETIME2(0) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_REVIEW_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_REVIEW PRIMARY KEY (review_id),
    CONSTRAINT FK_REVIEW_BOOKING FOREIGN KEY (booking_id) REFERENCES BOOKING(booking_id),
    CONSTRAINT FK_REVIEW_APP_USER_guest FOREIGN KEY (guest_id) REFERENCES APP_USER(user_id),
    CONSTRAINT FK_REVIEW_ROOM FOREIGN KEY (room_id) REFERENCES ROOM(room_id),
    CONSTRAINT FK_REVIEW_APP_USER_moderated_by FOREIGN KEY (moderated_by) REFERENCES APP_USER(user_id),
    CONSTRAINT UQ_REVIEW_booking UNIQUE (booking_id),
    CONSTRAINT CK_REVIEW_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT CK_REVIEW_status CHECK (status IN ('VISIBLE','HIDDEN'))
);
GO

CREATE INDEX IX_REVIEW_room_status ON REVIEW(room_id, status);
GO

/* ============================================================================
   SUMMARY CHECK
   ============================================================================ */
SELECT COUNT(*) AS table_count FROM sys.tables WHERE is_ms_shipped = 0;  -- must be 18
GO

/*
================================================================================
SUMMARY OF OBJECTS

1. Tables Created (18 total):
   - Identity & Access: ROLE, PERMISSION, ROLE_PERMISSION, APP_USER
   - Property & Rooms:  ROOM, ROOM_TIME_SLOT
   - Booking & Payment: BOOKING, BOOKING_SLOT, PAYMENT
   - IoT & Hardware:    DEVICE, STAFF_ASSIGNMENT, ACCESS_CREDENTIAL,
                        ACCESS_LOG, DOOR_EVENT, MOTION_LOG,
                        ALERT_THRESHOLD, ALERT_LOG
   - Feedback:          REVIEW

2. Filtered Unique Indexes:
   - UX_BOOKING_SLOT_held: Prevents double-booking by enforcing uniqueness on
     (room_slot_id, slot_date) only for active 'HELD' reservations.
   - UX_ACCESS_CREDENTIAL_live_pin: Prevents identical active PINs on the same device.
   - UX_ACCESS_CREDENTIAL_live_booking: Allows exactly one live PIN per booking.


4. Application-Enforced [APP] Rules:
   - APP_USER: Login via email and BCrypt hash; exactly one OWNER account, not
     creatable from public UI. Staff fields (position, employment_status, hire_date)
     stored directly on APP_USER for staff and admin accounts.
   - ROOM_TIME_SLOT: Non-overlapping slot intervals per room, including overnight
     slot S4 transition against next day's S1.
   - BOOKING: Slot selections must be consecutive; multi-slot discount rules (0%,
     10%, 20%, 50%) applied via half-up rounding snapshot from AppConstants.
   - BOOKING_SLOT: room_id and slot_date mirrored from parent BOOKING for indexing;
     no-show bookings maintain HELD status until explicit administrative release.
   - PAYMENT: Charged amount must equal BOOKING.total_amount; each payment attempt
     generates an independent order code.
   - STAFF_ASSIGNMENT: Cleaning assignments require staff users with role STAFF;
     target rooms must be in NEEDS_CLEANING status upon assignment creation.
     Started and completed timestamps track cleaning progress directly on assignment.
   - ACCESS_CREDENTIAL: Live credentials require non-null ciphertext and HMAC; raw
     PINs are never persisted in plaintext.
   - ACCESS_LOG: Door unlock attempts record status and reason codes; user-entered
     PIN is never logged.
   - REVIEW: Reviews permitted only for bookings in CHECKED_OUT status belonging
     to the reviewing guest.
================================================================================
*/
