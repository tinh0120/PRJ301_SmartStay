/*
================================================================================
SmartStay Seed Data
Database: SmartStayDB
Task: T12 - Seed data + hash/crypto tools
================================================================================
*/

USE SmartStayDB;
GO
-- Required for inserts into tables with filtered indexes (sqlcmd defaults QUOTED_IDENTIFIER to OFF)
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
GO

-- ============================================================================
-- 1. ROLE, PERMISSION, ROLE_PERMISSION
-- ============================================================================
INSERT INTO ROLE (role_name, description) VALUES
('OWNER', 'Homestay property owner with unrestricted system authority'),
('ADMIN', 'Operational administrator managing reservations, staff, and hardware'),
('STAFF', 'Housekeeping and operations staff executing cleaning tasks'),
('GUEST', 'Customer booking rooms and viewing access PINs');
GO

INSERT INTO PERMISSION (permission_code, description) VALUES
('ROOM_MANAGE', 'Create, update, and manage rental rooms'),
('ROOM_SLOT_MANAGE', 'Configure time slot schedules and base slot pricing'),
('HOMESTAY_MANAGE', 'Update homestay profile, contact info, and policies'),
('CONFIG_VIEW', 'View system configuration parameters'),
('CONFIG_MANAGE', 'Modify system configuration parameters and policies'),
('DASHBOARD_VIEW', 'View administrative dashboard and operational statistics'),
('USER_MANAGE', 'Manage staff and guest user accounts and lock status'),
('ASSIGNMENT_MANAGE', 'Create and dispatch room cleaning assignments'),
('ASSIGNMENT_OWN', 'View and claim assigned cleaning assignments'),
('CLEANING_PERFORM', 'Start and complete room cleaning sessions'),
('BOOKING_VIEW_ALL', 'View all guest reservations across all rooms'),
('BOOKING_MANAGE', 'Perform manual check-in, check-out, and cancellation'),
('BOOKING_OWN', 'Create and view own guest bookings'),
('NOSHOW_INTERVENE', 'Release slots or reinstate no-show expired bookings'),
('PAYMENT_VIEW', 'View payment transaction logs and order codes'),
('PAYMENT_RESOLVE', 'Manually resolve late, failed, or pending refund payments'),
('CREDENTIAL_VIEW_META', 'View PIN metadata and access credential validity windows'),
('CREDENTIAL_REVOKE', 'Manually revoke live door PIN credentials'),
('DEVICE_MANAGE', 'Register, update, and inspect smart locks and cameras'),
('THRESHOLD_MANAGE', 'Configure sensor metric thresholds and alert severity'),
('LOG_ACCESS_VIEW', 'Inspect physical door unlock history and failed attempts'),
('LOG_MOTION_VIEW', 'Inspect lobby motion logs and distance telemetry'),
('SENSOR_VIEW', 'View IoT sensor telemetry metrics and charts'),
('ALERT_VIEW', 'View security alerts and active operational incidents'),
('ALERT_HANDLE', 'Acknowledge and resolve security and operational alerts'),
('SNAPSHOT_VIEW', 'View lobby surveillance camera snapshot images'),
('REPORT_REVENUE', 'View financial revenue reports and slot occupancy charts'),
('AUDIT_VIEW', 'Inspect system administrative audit trail'),
('REVIEW_MODERATE', 'Moderate and hide inappropriate guest reviews'),
('REVIEW_CREATE', 'Submit ratings and reviews for completed stays');
GO

-- Role permissions mapping
-- OWNER owns all 30 permissions
INSERT INTO ROLE_PERMISSION (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM ROLE r CROSS JOIN PERMISSION p
WHERE r.role_name = 'OWNER';

-- ADMIN owns 23 permissions: all except the Owner-only codes
-- (HOMESTAY_MANAGE, ROOM_SLOT_MANAGE, CONFIG_MANAGE) and the Staff/Guest-only codes
-- (ASSIGNMENT_OWN, CLEANING_PERFORM, BOOKING_OWN, REVIEW_CREATE)
INSERT INTO ROLE_PERMISSION (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM ROLE r CROSS JOIN PERMISSION p
WHERE r.role_name = 'ADMIN'
  AND p.permission_code NOT IN ('HOMESTAY_MANAGE', 'ROOM_SLOT_MANAGE', 'CONFIG_MANAGE',
                                'ASSIGNMENT_OWN', 'CLEANING_PERFORM', 'BOOKING_OWN', 'REVIEW_CREATE');

-- STAFF owns ASSIGNMENT_OWN, CLEANING_PERFORM
INSERT INTO ROLE_PERMISSION (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM ROLE r CROSS JOIN PERMISSION p
WHERE r.role_name = 'STAFF'
  AND p.permission_code IN ('ASSIGNMENT_OWN', 'CLEANING_PERFORM');

-- GUEST owns BOOKING_OWN, REVIEW_CREATE
INSERT INTO ROLE_PERMISSION (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM ROLE r CROSS JOIN PERMISSION p
WHERE r.role_name = 'GUEST'
  AND p.permission_code IN ('BOOKING_OWN', 'REVIEW_CREATE');
GO

-- ============================================================================
-- 2. APP_USER & STAFF_INFORMATION
-- ============================================================================
DECLARE @rOwner INT = (SELECT role_id FROM ROLE WHERE role_name = 'OWNER');
DECLARE @rAdmin INT = (SELECT role_id FROM ROLE WHERE role_name = 'ADMIN');
DECLARE @rStaff INT = (SELECT role_id FROM ROLE WHERE role_name = 'STAFF');
DECLARE @rGuest INT = (SELECT role_id FROM ROLE WHERE role_name = 'GUEST');

INSERT INTO APP_USER (role_id, full_name, email, phone, password_hash, status) VALUES
(@rOwner, 'Olivia Bennett', 'owner@gmail.com', '0900000001', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rAdmin, 'Adam Miller', 'admin1@gmail.com', '0900000002', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rAdmin, 'Alicia Davis', 'admin2@gmail.com', '0900000003', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rStaff, 'Sam Wilson', 'staff1@gmail.com', '0900000004', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rStaff, 'Sophie Clark', 'staff2@gmail.com', '0900000005', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Grace Nguyen', 'guest1@smartstay.local', '0900000010', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE');

-- 34 additional active guest accounts
INSERT INTO APP_USER (role_id, full_name, email, phone, password_hash, status) VALUES
(@rGuest, 'Liam Anderson', 'guest2@smartstay.local', '0900000011', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Emma Thomas', 'guest3@smartstay.local', '0900000012', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Noah Jackson', 'guest4@smartstay.local', '0900000013', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Ava White', 'guest5@smartstay.local', '0900000014', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'William Harris', 'guest6@smartstay.local', '0900000015', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Sophia Martin', 'guest7@smartstay.local', '0900000016', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'James Thompson', 'guest8@smartstay.local', '0900000017', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Isabella Garcia', 'guest9@smartstay.local', '0900000018', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Benjamin Martinez', 'guest10@smartstay.local', '0900000019', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Mia Robinson', 'guest11@smartstay.local', '0900000020', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Lucas Clark', 'guest12@smartstay.local', '0900000021', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Charlotte Rodriguez', 'guest13@smartstay.local', '0900000022', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Henry Lewis', 'guest14@smartstay.local', '0900000023', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Amelia Lee', 'guest15@smartstay.local', '0900000024', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Alexander Walker', 'guest16@smartstay.local', '0900000025', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Harper Hall', 'guest17@smartstay.local', '0900000026', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Daniel Allen', 'guest18@smartstay.local', '0900000027', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Evelyn Young', 'guest19@smartstay.local', '0900000028', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Matthew Hernandez', 'guest20@smartstay.local', '0900000029', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Abigail King', 'guest21@smartstay.local', '0900000030', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Jackson Wright', 'guest22@smartstay.local', '0900000031', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Emily Lopez', 'guest23@smartstay.local', '0900000032', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Sebastian Hill', 'guest24@smartstay.local', '0900000033', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Elizabeth Scott', 'guest25@smartstay.local', '0900000034', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'David Green', 'guest26@smartstay.local', '0900000035', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Avery Adams', 'guest27@smartstay.local', '0900000036', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Carter Baker', 'guest28@smartstay.local', '0900000037', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Ella Gonzalez', 'guest29@smartstay.local', '0900000038', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Wyatt Nelson', 'guest30@smartstay.local', '0900000039', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Scarlett Carter', 'guest31@smartstay.local', '0900000040', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Jayden Mitchell', 'guest32@smartstay.local', '0900000041', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Chloe Perez', 'guest33@smartstay.local', '0900000042', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Gabriel Roberts', 'guest34@smartstay.local', '0900000043', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE'),
(@rGuest, 'Victoria Turner', 'guest35@smartstay.local', '0900000044', '$2a$10$Pz90qZghzJQ33uE5sP5cO.p.zch3tVvtZsTvtv1j4gEXnDdCBnKe6', 'ACTIVE');
GO

-- Staff profile details merged into APP_USER
UPDATE APP_USER SET position = 'Operations Manager', employment_status = 'ACTIVE', hire_date = '2024-01-15' WHERE email = 'admin1@gmail.com';
UPDATE APP_USER SET position = 'Assistant Manager', employment_status = 'ACTIVE', hire_date = '2024-06-01' WHERE email = 'admin2@gmail.com';
UPDATE APP_USER SET position = 'Housekeeper', employment_status = 'ACTIVE', hire_date = '2025-02-10' WHERE email = 'staff1@gmail.com';
UPDATE APP_USER SET position = 'Housekeeper', employment_status = 'ACTIVE', hire_date = '2025-03-01' WHERE email = 'staff2@gmail.com';
GO

-- ============================================================================
-- 3. ROOM, ROOM_TIME_SLOT
-- ============================================================================
INSERT INTO ROOM (room_number, display_name, floor_number, location_description, capacity, description, amenities, image_url, status) VALUES
('A101', 'Butter Beige', 1, 'Floor 1, West Wing', 2,
 'Warm, creamy tones with a cozy kitchen, HD projector, and plush king bed designed for restful relaxation.',
 'Kitchen, Projector, Netflix, King Bed, Full-length Mirror, Private Bathroom',
 '/assets/images/rooms/butter-beige/main.jpeg', 'AVAILABLE'),
('A102', 'Vanilla Zen', 1, 'Floor 1, West Wing', 2,
 'Minimalist peaceful sanctuary featuring a fully equipped kitchen, Netflix streaming projector, and calm natural ambience.',
 'Kitchen, Projector, Netflix, King Bed, Full-length Mirror, Private Bathroom',
 '/assets/images/rooms/vanilla-zen/main.jpeg', 'OCCUPIED'),
('A103', 'Fresh Tale', 1, 'Floor 1, East Wing', 3,
 'Spacious refreshing suite equipped with a deep soaking bathtub, versatile sofa bed, bean bag lounge, and cinema projector.',
 'Bean Bag Chair, Bathtub, Projector, Netflix, King Bed, Full-length Mirror, Sofa Bed, Private Bathroom',
 '/assets/images/rooms/fresh-tale/main.jpeg', 'AVAILABLE'),
('A104', 'Power Rush', 2, 'Floor 2, West Wing', 2,
 'Vibrant and modern living space with an ergonomic bean bag chair, high-definition projector, and private bathroom.',
 'Bean Bag Chair, Projector, Netflix, King Bed, Full-length Mirror, Private Bathroom',
 '/assets/images/rooms/power-rush/main.jpeg', 'AVAILABLE'),
('A105', 'Sunny Love', 2, 'Floor 2, East Wing', 2,
 'Bright, romantic aesthetic with cinema projector, comfortable king bed, and full-length vanity mirror.',
 'Projector, Netflix, King Bed, Full-length Mirror, Private Bathroom',
 '/assets/images/rooms/sunny-love/main.jpeg', 'NEEDS_CLEANING'),
('A106', 'Warm House', 2, 'Floor 2, East Wing', 2,
 'Inviting and playful atmosphere complete with a mini game console, plush bean bag, Netflix projector, and comfortable king bed.',
 'Bean Bag Chair, Mini Game Console, Projector, Netflix, King Bed, Full-length Mirror, Private Bathroom',
 '/assets/images/rooms/warm-house/main.png', 'MAINTENANCE');
GO

-- 24 Room Time Slots (spec 2.2 exact schedule)
DECLARE @rA101 BIGINT = (SELECT room_id FROM ROOM WHERE room_number = 'A101');
DECLARE @rA102 BIGINT = (SELECT room_id FROM ROOM WHERE room_number = 'A102');
DECLARE @rA103 BIGINT = (SELECT room_id FROM ROOM WHERE room_number = 'A103');
DECLARE @rA104 BIGINT = (SELECT room_id FROM ROOM WHERE room_number = 'A104');
DECLARE @rA105 BIGINT = (SELECT room_id FROM ROOM WHERE room_number = 'A105');
DECLARE @rA106 BIGINT = (SELECT room_id FROM ROOM WHERE room_number = 'A106');

INSERT INTO ROOM_TIME_SLOT (room_id, slot_code, slot_name, start_time, end_time, crosses_midnight, sort_order, list_price) VALUES
-- A101 (Butter Beige)
(@rA101, 'S1', 'Morning',   '09:30:00', '12:30:00', 0, 1, 259000),
(@rA101, 'S2', 'Afternoon', '13:00:00', '16:00:00', 0, 2, 259000),
(@rA101, 'S3', 'Evening',   '16:30:00', '19:30:00', 0, 3, 259000),
(@rA101, 'S4', 'Overnight', '20:00:00', '09:00:00', 1, 4, 359000),
-- A102 (Vanilla Zen)
(@rA102, 'S1', 'Morning',   '09:50:00', '12:50:00', 0, 1, 299000),
(@rA102, 'S2', 'Afternoon', '13:20:00', '16:20:00', 0, 2, 299000),
(@rA102, 'S3', 'Evening',   '16:50:00', '19:50:00', 0, 3, 299000),
(@rA102, 'S4', 'Overnight', '20:20:00', '09:20:00', 1, 4, 399000),
-- A103 (Fresh Tale)
(@rA103, 'S1', 'Morning',   '10:10:00', '13:10:00', 0, 1, 199000),
(@rA103, 'S2', 'Afternoon', '13:40:00', '16:40:00', 0, 2, 199000),
(@rA103, 'S3', 'Evening',   '17:10:00', '20:10:00', 0, 3, 199000),
(@rA103, 'S4', 'Overnight', '20:40:00', '09:40:00', 1, 4, 299000),
-- A104 (Power Rush)
(@rA104, 'S1', 'Morning',   '10:30:00', '13:30:00', 0, 1, 259000),
(@rA104, 'S2', 'Afternoon', '14:00:00', '17:00:00', 0, 2, 259000),
(@rA104, 'S3', 'Evening',   '17:30:00', '20:30:00', 0, 3, 259000),
(@rA104, 'S4', 'Overnight', '21:00:00', '10:00:00', 1, 4, 359000),
-- A105 (Sunny Love)
(@rA105, 'S1', 'Morning',   '10:50:00', '13:50:00', 0, 1, 249000),
(@rA105, 'S2', 'Afternoon', '14:20:00', '17:20:00', 0, 2, 249000),
(@rA105, 'S3', 'Evening',   '17:50:00', '20:50:00', 0, 3, 249000),
(@rA105, 'S4', 'Overnight', '21:20:00', '10:20:00', 1, 4, 349000),
-- A106 (Warm House)
(@rA106, 'S1', 'Morning',   '11:10:00', '14:10:00', 0, 1, 249000),
(@rA106, 'S2', 'Afternoon', '14:40:00', '17:40:00', 0, 2, 249000),
(@rA106, 'S3', 'Evening',   '18:10:00', '21:10:00', 0, 3, 249000),
(@rA106, 'S4', 'Overnight', '21:40:00', '10:40:00', 1, 4, 349000);
GO



-- ============================================================================
-- 5. DEVICE & ALERT_THRESHOLD
-- ============================================================================
INSERT INTO DEVICE (device_id, device_name, device_type, zone_id, status, last_seen_at) VALUES
('MAIN_DOOR_LOCK_01', 'Main Entrance Smart Lock', 'SMART_LOCK', 'MAIN_ENTRANCE', 'ONLINE', SYSDATETIME()),
('MAIN_DOOR_LOCK_02', 'Auxiliary Entrance Smart Lock', 'SMART_LOCK', 'MAIN_ENTRANCE', 'FAULT', DATEADD(DAY, -1, SYSDATETIME())),
('LOBBY_CAM_01', 'Lobby Security Camera 1', 'SECURITY_CAMERA', 'LOBBY', 'ONLINE', DATEADD(MINUTE, -2, SYSDATETIME())),
('LOBBY_CAM_02', 'Lobby Security Camera 2', 'SECURITY_CAMERA', 'LOBBY', 'OFFLINE', DATEADD(HOUR, -2, SYSDATETIME()));

DECLARE @ownerUserId BIGINT = (SELECT user_id FROM APP_USER WHERE email = 'owner@gmail.com');
INSERT INTO ALERT_THRESHOLD (device_id, metric_type, operator, threshold_value, alert_type, severity, enabled, description, updated_by)
VALUES
(NULL, 'DISTANCE_CM', 'LT', 50.00, 'LOBBY_PROXIMITY', 'LOW', 1, 'Triggers when a person approaches within 50cm of lobby sensor', @ownerUserId),
(NULL, 'DOOR_OPEN_SECONDS', 'GT', 30.00, 'DOOR_OPEN_TOO_LONG', 'HIGH', 1, 'Triggers when main door remains physically open longer than 30 seconds', @ownerUserId);
GO

-- ============================================================================
-- 6. BOOKING, BOOKING_SLOT, PAYMENT
-- ============================================================================
DECLARE @now DATETIME2(0) = SYSDATETIME();
DECLARE @today DATE = CAST(@now AS DATE);
DECLARE @orderSeq BIGINT = 1000001;

CREATE TABLE #SeedBookings (
    idx INT IDENTITY(1,1),
    guest_email VARCHAR(100),
    room_code VARCHAR(10),
    day_offset INT,
    slot_pattern VARCHAR(10),
    status VARCHAR(30),
    expire_reason VARCHAR(30) NULL,
    cancel_reason NVARCHAR(255) NULL,
    payment_status VARCHAR(30)
);

INSERT INTO #SeedBookings (guest_email, room_code, day_offset, slot_pattern, status, expire_reason, cancel_reason, payment_status) VALUES
('guest1@smartstay.local', 'A101', -58, '1', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest2@smartstay.local', 'A102', -57, '1,2', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest3@smartstay.local', 'A103', -56, '2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest4@smartstay.local', 'A104', -55, '3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest5@smartstay.local', 'A105', -54, '1,2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest6@smartstay.local', 'A106', -53, '2,3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest7@smartstay.local', 'A101', -52, '1,2,3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest8@smartstay.local', 'A102', -51, '1', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest9@smartstay.local', 'A103', -50, '1,2', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest10@smartstay.local', 'A104', -49, '2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest11@smartstay.local', 'A105', -48, '3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest12@smartstay.local', 'A106', -47, '1,2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest13@smartstay.local', 'A101', -46, '2,3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest14@smartstay.local', 'A102', -45, '1,2,3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest15@smartstay.local', 'A103', -44, '1', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest16@smartstay.local', 'A104', -43, '1,2', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest17@smartstay.local', 'A105', -42, '2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest18@smartstay.local', 'A106', -41, '3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest19@smartstay.local', 'A101', -40, '1,2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest20@smartstay.local', 'A102', -39, '2,3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest21@smartstay.local', 'A103', -38, '1,2,3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest22@smartstay.local', 'A104', -37, '1', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest23@smartstay.local', 'A105', -36, '1,2', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest24@smartstay.local', 'A106', -35, '2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest25@smartstay.local', 'A101', -34, '3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest26@smartstay.local', 'A102', -33, '1,2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest27@smartstay.local', 'A103', -32, '2,3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest28@smartstay.local', 'A104', -31, '1,2,3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest29@smartstay.local', 'A105', -30, '1', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest30@smartstay.local', 'A106', -29, '1,2', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest1@smartstay.local', 'A101', -28, '2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest2@smartstay.local', 'A102', -27, '3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest3@smartstay.local', 'A103', -26, '1,2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest4@smartstay.local', 'A104', -25, '2,3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest5@smartstay.local', 'A105', -24, '1,2,3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest6@smartstay.local', 'A106', -23, '1', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest7@smartstay.local', 'A101', -22, '1,2', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest8@smartstay.local', 'A102', -21, '2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest9@smartstay.local', 'A103', -20, '3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest10@smartstay.local', 'A104', -19, '1,2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest11@smartstay.local', 'A105', -18, '2,3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest12@smartstay.local', 'A106', -17, '1,2,3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest13@smartstay.local', 'A101', -16, '1', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest14@smartstay.local', 'A102', -15, '1,2', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest15@smartstay.local', 'A103', -14, '2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest16@smartstay.local', 'A101', -13, '1,2', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest17@smartstay.local', 'A102', -12, '2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest18@smartstay.local', 'A103', -11, '3,4', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest19@smartstay.local', 'A104', -10, '1,2', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest20@smartstay.local', 'A105', -9, '2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest21@smartstay.local', 'A106', -8, '1,2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest22@smartstay.local', 'A101', -7, '1,2', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest23@smartstay.local', 'A102', -6, '1,2', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest24@smartstay.local', 'A103', -5, '2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest25@smartstay.local', 'A105', -4, '1,2', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest26@smartstay.local', 'A106', -3, '2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest27@smartstay.local', 'A104', -2, '2,3', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest28@smartstay.local', 'A105', -1, '1,2', 'CHECKED_OUT', NULL, NULL, 'PAID'),
('guest4@smartstay.local', 'A102', 0, '1,2', 'CHECKED_IN', NULL, NULL, 'PAID'),
('guest1@smartstay.local', 'A101', 2, '1,2,3', 'CONFIRMED', NULL, NULL, 'PAID'),
('guest2@smartstay.local', 'A103', 4, '1,2', 'CONFIRMED', NULL, NULL, 'PAID'),
('guest3@smartstay.local', 'A104', 6, '1', 'CONFIRMED', NULL, NULL, 'PAID'),
('guest5@smartstay.local', 'A105', 0, '3', 'PENDING_PAYMENT', NULL, NULL, 'PENDING'),
('guest6@smartstay.local', 'A106', -5, '1,2', 'CANCELLED', NULL, 'Guest requested cancellation due to travel schedule change', 'REFUND_PENDING'),
('guest7@smartstay.local', 'A101', -4, '1', 'CANCELLED', NULL, 'Guest cancelled reservation before payment', 'FAILED'),
('guest8@smartstay.local', 'A103', -3, '2', 'CANCELLED', NULL, 'Cancelled due to payment declined', 'FAILED'),
('guest15@smartstay.local', 'A104', -7, '1,2', 'EXPIRED', 'NO_SHOW', NULL, 'PAID'),
('guest16@smartstay.local', 'A105', -6, '3,4', 'EXPIRED', 'NO_SHOW', NULL, 'PAID'),
('guest9@smartstay.local', 'A102', -2, '1', 'EXPIRED', 'PAYMENT_TIMEOUT', NULL, 'EXPIRED'),
('guest10@smartstay.local', 'A106', -1, '2,3', 'EXPIRED', 'PAYMENT_TIMEOUT', NULL, 'EXPIRED');

DECLARE @curIdx INT = 1;
DECLARE @maxIdx INT = (SELECT MAX(idx) FROM #SeedBookings);

WHILE @curIdx <= @maxIdx
BEGIN
    DECLARE @guestEmail VARCHAR(100), @roomCode VARCHAR(10), @dayOffset INT, @pattern VARCHAR(10);
    DECLARE @bStatus VARCHAR(30), @expReason VARCHAR(30), @cancReason NVARCHAR(255), @payStatus VARCHAR(30);
    SELECT @guestEmail = guest_email, @roomCode = room_code, @dayOffset = day_offset,
           @pattern = slot_pattern, @bStatus = status, @expReason = expire_reason,
           @cancReason = cancel_reason, @payStatus = payment_status
    FROM #SeedBookings WHERE idx = @curIdx;

    DECLARE @gId BIGINT = (SELECT user_id FROM APP_USER WHERE email = @guestEmail);
    DECLARE @rId BIGINT = (SELECT room_id FROM ROOM WHERE room_number = @roomCode);
    DECLARE @sDate DATE = DATEADD(DAY, @dayOffset, @today);

    DECLARE @subtotal DECIMAL(12,0) = 0;
    DECLARE @slotCount INT = 0;
    DECLARE @minStart TIME(0), @maxEnd TIME(0);

    SELECT @subtotal = SUM(list_price), @slotCount = COUNT(*),
           @minStart = MIN(start_time), @maxEnd = MAX(end_time)
    FROM ROOM_TIME_SLOT
    WHERE room_id = @rId AND CHARINDEX(CAST(sort_order AS VARCHAR(1)), @pattern) > 0;

    DECLARE @discPercent TINYINT = CASE @slotCount WHEN 1 THEN 0 WHEN 2 THEN 10 WHEN 3 THEN 20 WHEN 4 THEN 50 ELSE 0 END;
    DECLARE @discAmount DECIMAL(12,0) = ROUND(@subtotal * @discPercent / 100.0, 0);
    DECLARE @totAmount DECIMAL(12,0) = @subtotal - @discAmount;

    DECLARE @checkinAt DATETIME2(0) = DATEADD(SECOND, DATEDIFF(SECOND, '00:00:00', @minStart), CAST(@sDate AS DATETIME2(0)));
    DECLARE @lastSlotSort INT = (SELECT MAX(sort_order) FROM ROOM_TIME_SLOT WHERE room_id = @rId AND CHARINDEX(CAST(sort_order AS VARCHAR(1)), @pattern) > 0);
    DECLARE @lastSlotEnd TIME(0) = (SELECT end_time FROM ROOM_TIME_SLOT WHERE room_id = @rId AND sort_order = @lastSlotSort);
    DECLARE @lastCross BIT = (SELECT crosses_midnight FROM ROOM_TIME_SLOT WHERE room_id = @rId AND sort_order = @lastSlotSort);
    DECLARE @checkoutBase DATE = DATEADD(DAY, CASE WHEN @lastCross = 1 THEN 1 ELSE 0 END, @sDate);
    DECLARE @checkoutAt DATETIME2(0) = DATEADD(SECOND, DATEDIFF(SECOND, '00:00:00', @lastSlotEnd), CAST(@checkoutBase AS DATETIME2(0)));

    DECLARE @actCheckin DATETIME2(0) = NULL;
    DECLARE @actCheckout DATETIME2(0) = NULL;
    IF @bStatus = 'CHECKED_OUT'
    BEGIN
        SET @actCheckin = DATEADD(MINUTE, 5, @checkinAt);
        SET @actCheckout = DATEADD(MINUTE, -10, @checkoutAt);
    END
    ELSE IF @bStatus = 'CHECKED_IN'
    BEGIN
        SET @actCheckin = @now;
    END;

    DECLARE @payDeadline DATETIME2(0) = CASE WHEN @bStatus = 'PENDING_PAYMENT' THEN DATEADD(MINUTE, 15, @now) ELSE NULL END;

    INSERT INTO BOOKING (guest_id, room_id, slot_date, status, expire_reason,
                         subtotal_amount, discount_percent, discount_amount, total_amount,
                         checkin_at, checkout_at, actual_checkin_at, actual_checkout_at,
                         payment_deadline, cancel_reason, created_at)
    VALUES (@gId, @rId, @sDate, @bStatus, @expReason,
            @subtotal, @discPercent, @discAmount, @totAmount,
            @checkinAt, @checkoutAt, @actCheckin, @actCheckout,
            @payDeadline, @cancReason, DATEADD(DAY, -1, @checkinAt));

    DECLARE @newBookingId BIGINT = SCOPE_IDENTITY();

    DECLARE @holdStatus VARCHAR(10) = CASE WHEN @bStatus = 'CANCELLED' OR (@bStatus = 'EXPIRED' AND @expReason = 'PAYMENT_TIMEOUT') THEN 'RELEASED' ELSE 'HELD' END;
    DECLARE @relAt DATETIME2(0) = CASE WHEN @holdStatus = 'RELEASED' THEN @checkinAt ELSE NULL END;

    INSERT INTO BOOKING_SLOT (booking_id, room_id, room_slot_id, slot_date, start_at, end_at, list_price, hold_status, released_at)
    SELECT @newBookingId, @rId, rts.room_slot_id, @sDate,
           DATEADD(SECOND, DATEDIFF(SECOND, '00:00:00', rts.start_time), CAST(@sDate AS DATETIME2(0))),
           DATEADD(SECOND, DATEDIFF(SECOND, '00:00:00', rts.end_time), CAST(DATEADD(DAY, CASE WHEN rts.crosses_midnight = 1 THEN 1 ELSE 0 END, @sDate) AS DATETIME2(0))),
           rts.list_price, @holdStatus, @relAt
    FROM ROOM_TIME_SLOT rts
    WHERE rts.room_id = @rId AND CHARINDEX(CAST(rts.sort_order AS VARCHAR(1)), @pattern) > 0;

    DECLARE @paidAt DATETIME2(0) = CASE WHEN @payStatus = 'PAID' THEN DATEADD(MINUTE, 5, DATEADD(DAY, -1, @checkinAt)) ELSE NULL END;
    DECLARE @payExpireAt DATETIME2(0) = DATEADD(MINUTE, 15, DATEADD(DAY, -1, @checkinAt));
    INSERT INTO PAYMENT (booking_id, provider, provider_order_code, amount, status, expires_at, paid_at, created_at)
    VALUES (@newBookingId, 'MOCK', @orderSeq, @totAmount, @payStatus, @payExpireAt, @paidAt, DATEADD(DAY, -1, @checkinAt));

    DECLARE @newPaymentId BIGINT = SCOPE_IDENTITY();

    SET @orderSeq = @orderSeq + 1;
    SET @curIdx = @curIdx + 1;
END;

DROP TABLE #SeedBookings;
GO

-- Additional Payment & Webhook fixture cases (Late payment, Duplicate, Mismatch, Unknown order)
DECLARE @anyBooking BIGINT = (SELECT TOP 1 booking_id FROM BOOKING WHERE status = 'CHECKED_OUT');
DECLARE @anyAmount DECIMAL(12,0) = (SELECT total_amount FROM BOOKING WHERE booking_id = @anyBooking);
INSERT INTO PAYMENT (booking_id, provider, provider_order_code, amount, status, expires_at, resolution_note, created_at)
VALUES (@anyBooking, 'MOCK', 1000999, @anyAmount, 'REVIEW_REQUIRED', DATEADD(DAY, -4, SYSDATETIME()), 'Payment arrived 10 minutes past deadline', DATEADD(DAY, -5, SYSDATETIME()));
GO

-- ============================================================================
-- 7. ACCESS_CREDENTIAL (AES-256-GCM + HMAC-SHA256 from PinCryptoTool)
-- ============================================================================
/*
DOCUMENTED PIN MAPPINGS:
  PIN 1:  482910 -> guest1 CONFIRMED future booking
  PIN 2:  591823 -> guest2 CONFIRMED future booking
  PIN 3:  602941 -> guest3 CONFIRMED future booking
  PIN 4:  713052 -> guest4 CHECKED_IN booking (today, OCCUPIED)
  PIN 5:  102938 -> past CHECKED_OUT booking 1
  PIN 6:  213049 -> past CHECKED_OUT booking 2
  PIN 7:  324150 -> past CHECKED_OUT booking 3
  PIN 8:  435261 -> past CHECKED_OUT booking 4
  PIN 9:  546372 -> past CHECKED_OUT booking 5
  PIN 10: 657483 -> past CHECKED_OUT booking 6
  PIN 11: 768594 -> past CHECKED_OUT booking 7
  PIN 12: 879605 -> past CHECKED_OUT booking 8
  PIN 13: 980716 -> past CHECKED_OUT booking 9
  PIN 14: 091827 -> past CHECKED_OUT booking 10
  PIN 15: 314253 -> guest15 EXPIRED NO_SHOW 1
  PIN 16: 425364 -> guest16 EXPIRED NO_SHOW 2
  PIN 17: 536475 -> guest6 CANCELLED 1
  PIN 18: 647586 -> guest7 CANCELLED 2
  PIN 19: 758697 -> guest8 CANCELLED 3
  PIN 20: 869708 -> staff1 STAFF_PIN for cleaning assignment (today)
*/

DECLARE @mainDoor VARCHAR(50) = 'MAIN_DOOR_LOCK_01';
DECLARE @bConf1 BIGINT = (SELECT booking_id FROM BOOKING WHERE guest_id = (SELECT user_id FROM APP_USER WHERE email = 'guest1@smartstay.local') AND status = 'CONFIRMED');
DECLARE @bConf2 BIGINT = (SELECT booking_id FROM BOOKING WHERE guest_id = (SELECT user_id FROM APP_USER WHERE email = 'guest2@smartstay.local') AND status = 'CONFIRMED');
DECLARE @bConf3 BIGINT = (SELECT booking_id FROM BOOKING WHERE guest_id = (SELECT user_id FROM APP_USER WHERE email = 'guest3@smartstay.local') AND status = 'CONFIRMED');
DECLARE @bCheckedIn BIGINT = (SELECT booking_id FROM BOOKING WHERE guest_id = (SELECT user_id FROM APP_USER WHERE email = 'guest4@smartstay.local') AND status = 'CHECKED_IN');

DECLARE @bNoShow1 BIGINT = (SELECT booking_id FROM BOOKING WHERE guest_id = (SELECT user_id FROM APP_USER WHERE email = 'guest15@smartstay.local') AND status = 'EXPIRED' AND expire_reason = 'NO_SHOW');
DECLARE @bNoShow2 BIGINT = (SELECT booking_id FROM BOOKING WHERE guest_id = (SELECT user_id FROM APP_USER WHERE email = 'guest16@smartstay.local') AND status = 'EXPIRED' AND expire_reason = 'NO_SHOW');

DECLARE @bCanc1 BIGINT = (SELECT booking_id FROM BOOKING WHERE guest_id = (SELECT user_id FROM APP_USER WHERE email = 'guest6@smartstay.local') AND status = 'CANCELLED');
DECLARE @bCanc2 BIGINT = (SELECT booking_id FROM BOOKING WHERE guest_id = (SELECT user_id FROM APP_USER WHERE email = 'guest7@smartstay.local') AND status = 'CANCELLED');
DECLARE @bCanc3 BIGINT = (SELECT booking_id FROM BOOKING WHERE guest_id = (SELECT user_id FROM APP_USER WHERE email = 'guest8@smartstay.local') AND status = 'CANCELLED');

INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status)
SELECT 'GUEST_PIN', @bConf1, @mainDoor,
       CONVERT(VARBINARY(256), '7B044E50D415E2623EAC60D7D78C0DE766D155212EA960F495E420ABC4B0A044D7E6', 2),
       CONVERT(VARBINARY(64), 'D0C69F85AF99F898F99CD0FC1241BD036E65964A995DCF5F254626E037637484', 2),
       checkin_at, checkout_at, 'PENDING'
FROM BOOKING WHERE booking_id = @bConf1;

INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status)
SELECT 'GUEST_PIN', @bConf2, @mainDoor,
       CONVERT(VARBINARY(256), 'ED79F518D81B44546639D862DAD7159825CC37AB9C41D6B9704FBE3FBBE2CD89C879', 2),
       CONVERT(VARBINARY(64), '54F3560B41506A681D19C9FF937B41A1560A13F9D67B5FD4AC9BCD2D5140E719', 2),
       checkin_at, checkout_at, 'PENDING'
FROM BOOKING WHERE booking_id = @bConf2;

INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status)
SELECT 'GUEST_PIN', @bConf3, @mainDoor,
       CONVERT(VARBINARY(256), 'CB9806FF120326207E745A078D811AB66DAF6C984E067F7B76DA2D12F4AC38BAD292', 2),
       CONVERT(VARBINARY(64), '7D8F74D75044BCA530C2395970DED1AC62C5BA09B74B4538E666B066BCFFC1C7', 2),
       checkin_at, checkout_at, 'PENDING'
FROM BOOKING WHERE booking_id = @bConf3;

INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, used_count)
SELECT 'GUEST_PIN', @bCheckedIn, @mainDoor,
       CONVERT(VARBINARY(256), '8B16A17ADF0111CEB7E2FD13A654D81F33C5E58F1D40E3520D966731423F9A9C0767', 2),
       CONVERT(VARBINARY(64), 'DD2B61DF6DC670DE7562C2A9A8DFE60EBEB7537ABDD99169EA36E45BA6C6A354', 2),
       checkin_at, checkout_at, 'ACTIVE', 2
FROM BOOKING WHERE booking_id = @bCheckedIn;

INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, used_count)
SELECT 'GUEST_PIN', booking_id, @mainDoor,
       CONVERT(VARBINARY(256), 'CE3E6CC11EA26AAF709CC7A95BA4A75B3A8D5C7F3CAEDDC8E7F923BBFB5DEAD82632', 2),
       CONVERT(VARBINARY(64), 'B0D617EB3E64FB446659DA3ABDFBD46978BDF3306ACF8C50CDA45D0041CE8E28', 2),
       checkin_at, checkout_at, 'EXPIRED', 3
FROM (SELECT booking_id, checkin_at, checkout_at, ROW_NUMBER() OVER (ORDER BY booking_id) AS rn FROM BOOKING WHERE status = 'CHECKED_OUT') sub
WHERE sub.rn = 1;
INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, used_count)
SELECT 'GUEST_PIN', booking_id, @mainDoor,
       CONVERT(VARBINARY(256), 'FBBDA1635C75F1FCB635CEC2F98DB1D274A73DD156EA7DFED15A135E55DBB536F294', 2),
       CONVERT(VARBINARY(64), '96D74A8F6C1D5060B4A2D6E12114405780F685140F3DEDCCB3CFDBA04A7C027F', 2),
       checkin_at, checkout_at, 'EXPIRED', 3
FROM (SELECT booking_id, checkin_at, checkout_at, ROW_NUMBER() OVER (ORDER BY booking_id) AS rn FROM BOOKING WHERE status = 'CHECKED_OUT') sub
WHERE sub.rn = 2;
INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, used_count)
SELECT 'GUEST_PIN', booking_id, @mainDoor,
       CONVERT(VARBINARY(256), '730CCF4076826D3E5DA0DED37D181AAC5BD31631E87E23361C76CB9EE8785F07B4A7', 2),
       CONVERT(VARBINARY(64), '7760DF332EA1A224D281A77EF03C74D452B04D408B38ADA298D562C6144B0045', 2),
       checkin_at, checkout_at, 'EXPIRED', 3
FROM (SELECT booking_id, checkin_at, checkout_at, ROW_NUMBER() OVER (ORDER BY booking_id) AS rn FROM BOOKING WHERE status = 'CHECKED_OUT') sub
WHERE sub.rn = 3;
INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, used_count)
SELECT 'GUEST_PIN', booking_id, @mainDoor,
       CONVERT(VARBINARY(256), '629A78F911CA7378B42E55D0BBE8B25CF271C6935948689CC362EC55BB8FFC2D3CCF', 2),
       CONVERT(VARBINARY(64), '58FBBE78F2AF26563519710C67AFD5E3B0CF631395511DEE520438FD89E0F199', 2),
       checkin_at, checkout_at, 'EXPIRED', 3
FROM (SELECT booking_id, checkin_at, checkout_at, ROW_NUMBER() OVER (ORDER BY booking_id) AS rn FROM BOOKING WHERE status = 'CHECKED_OUT') sub
WHERE sub.rn = 4;
INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, used_count)
SELECT 'GUEST_PIN', booking_id, @mainDoor,
       CONVERT(VARBINARY(256), 'CDB321CE0340F22F2FE74312967738FDE6AE148962321FF913FD85BA497C58B94877', 2),
       CONVERT(VARBINARY(64), 'ED8ADC1CA68528CF0B797E3E13CCFBFC03EC10C4BA6911624F51D0860873D19E', 2),
       checkin_at, checkout_at, 'EXPIRED', 3
FROM (SELECT booking_id, checkin_at, checkout_at, ROW_NUMBER() OVER (ORDER BY booking_id) AS rn FROM BOOKING WHERE status = 'CHECKED_OUT') sub
WHERE sub.rn = 5;
INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, used_count)
SELECT 'GUEST_PIN', booking_id, @mainDoor,
       CONVERT(VARBINARY(256), 'D209358D71509DDE07F244E00B12399E4D99B05F65153366F0318A136CE9C6D466B8', 2),
       CONVERT(VARBINARY(64), '2E9E473D79ED7582A2CFFD3EFF92305ED1583525FB2A5268EA2FD720FCE418BE', 2),
       checkin_at, checkout_at, 'EXPIRED', 3
FROM (SELECT booking_id, checkin_at, checkout_at, ROW_NUMBER() OVER (ORDER BY booking_id) AS rn FROM BOOKING WHERE status = 'CHECKED_OUT') sub
WHERE sub.rn = 6;
INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, used_count)
SELECT 'GUEST_PIN', booking_id, @mainDoor,
       CONVERT(VARBINARY(256), '90E24F9AC4B79E2F1AB58CBFAEE9BEE7C9CC57AFC7B916536F7FF67BE18D784567BD', 2),
       CONVERT(VARBINARY(64), 'F8FD43F1EADFD53A34ABD031B57A35FE15F5F3DF274119AD16D5F602F56503BB', 2),
       checkin_at, checkout_at, 'EXPIRED', 3
FROM (SELECT booking_id, checkin_at, checkout_at, ROW_NUMBER() OVER (ORDER BY booking_id) AS rn FROM BOOKING WHERE status = 'CHECKED_OUT') sub
WHERE sub.rn = 7;
INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, used_count)
SELECT 'GUEST_PIN', booking_id, @mainDoor,
       CONVERT(VARBINARY(256), '0291C31118CE70A0E7DE88871507A0FE4B6D97AD5E712CD5D8F9712385445989970D', 2),
       CONVERT(VARBINARY(64), 'E57BF9414E0F0A395BF047F58B6353CD1A186999E10B64C01894C70F5B0C2427', 2),
       checkin_at, checkout_at, 'EXPIRED', 3
FROM (SELECT booking_id, checkin_at, checkout_at, ROW_NUMBER() OVER (ORDER BY booking_id) AS rn FROM BOOKING WHERE status = 'CHECKED_OUT') sub
WHERE sub.rn = 8;
INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, used_count)
SELECT 'GUEST_PIN', booking_id, @mainDoor,
       CONVERT(VARBINARY(256), 'D63087AF31416F710D06DB3B418169C13EA2045696B6D672093F723382D9296D48A6', 2),
       CONVERT(VARBINARY(64), '5E258056E9ED2B10A33EB4ED7569A2ED00A439B82BCACAF638A054F1118DC3F9', 2),
       checkin_at, checkout_at, 'EXPIRED', 3
FROM (SELECT booking_id, checkin_at, checkout_at, ROW_NUMBER() OVER (ORDER BY booking_id) AS rn FROM BOOKING WHERE status = 'CHECKED_OUT') sub
WHERE sub.rn = 9;
INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, used_count)
SELECT 'GUEST_PIN', booking_id, @mainDoor,
       CONVERT(VARBINARY(256), '99C971C17D7E044F72DC8F9EDEA86C278F04FBDC1029BA87003B48DC4D1463A66A12', 2),
       CONVERT(VARBINARY(64), 'BE4FF12C03A7E7CAA97C02942BB436C051110966240541D72CB821CA99836728', 2),
       checkin_at, checkout_at, 'EXPIRED', 3
FROM (SELECT booking_id, checkin_at, checkout_at, ROW_NUMBER() OVER (ORDER BY booking_id) AS rn FROM BOOKING WHERE status = 'CHECKED_OUT') sub
WHERE sub.rn = 10;

INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, revoked_at, revoke_reason)
SELECT 'GUEST_PIN', @bNoShow1, @mainDoor,
       CONVERT(VARBINARY(256), 'C3BB0D604FDE6B822F41919241D77DA1BEF44C904564FBD12FDA739EBB0103BD9464', 2),
       CONVERT(VARBINARY(64), 'D9153CDC762DFDDF29522899D04D6E9EE092917792802477518FF506660D36BD', 2),
       checkin_at, checkout_at, 'REVOKED', DATEADD(MINUTE, 35, checkin_at), 'NO_SHOW'
FROM BOOKING WHERE booking_id = @bNoShow1;

INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, revoked_at, revoke_reason)
SELECT 'GUEST_PIN', @bNoShow2, @mainDoor,
       CONVERT(VARBINARY(256), '122FBCE2CE678A4DAE6E4F3D497241E6C63473FE067B856215A7A0244611D1A0D418', 2),
       CONVERT(VARBINARY(64), '86E9B7A754B3E99F69FD18B2A3DE74101B5040ECBEDA4F7D070D4FFA8E84BB2E', 2),
       checkin_at, checkout_at, 'REVOKED', DATEADD(MINUTE, 35, checkin_at), 'NO_SHOW'
FROM BOOKING WHERE booking_id = @bNoShow2;

INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, revoked_at, revoke_reason)
SELECT 'GUEST_PIN', @bCanc1, @mainDoor,
       CONVERT(VARBINARY(256), '05949537E7EE54E80CC50560063026165B5AA4134426F44B8A4D745448B9162D15C5', 2),
       CONVERT(VARBINARY(64), 'DC4CE54E9B245896B825BB859D85697436C3B4FD059012FB3996C2FBCC4D3829', 2),
       checkin_at, checkout_at, 'REVOKED', created_at, 'CANCELLED'
FROM BOOKING WHERE booking_id = @bCanc1;

INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, revoked_at, revoke_reason)
SELECT 'GUEST_PIN', @bCanc2, @mainDoor,
       CONVERT(VARBINARY(256), 'E0603A0FFA6C7468FB448712CBAAED80A88D8C7B9ED7543C9717604454A8144541A6', 2),
       CONVERT(VARBINARY(64), 'CE23C9B6C9A6485DE74531D82802019E6013DCC15E4FCE0286112197AF39F26F', 2),
       checkin_at, checkout_at, 'REVOKED', created_at, 'CANCELLED'
FROM BOOKING WHERE booking_id = @bCanc2;

INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status, revoked_at, revoke_reason)
SELECT 'GUEST_PIN', @bCanc3, @mainDoor,
       CONVERT(VARBINARY(256), '55787D256320961EA6BFD123ED040F57A859FDBE450B4DE8ACD4176B7A592510B113', 2),
       CONVERT(VARBINARY(64), 'FB4360FB25FC7B310D1642BA213D95780FDCE7169D289CAF996818105CA098FD', 2),
       checkin_at, checkout_at, 'REVOKED', created_at, 'ADMIN_REVOKE'
FROM BOOKING WHERE booking_id = @bCanc3;
GO

-- ============================================================================
-- 8. STAFF_ASSIGNMENT & CLEANING_SESSION
-- ============================================================================
DECLARE @staff1Id BIGINT = (SELECT user_id FROM APP_USER WHERE email = 'staff1@gmail.com');
DECLARE @staff2Id BIGINT = (SELECT user_id FROM APP_USER WHERE email = 'staff2@gmail.com');
DECLARE @adminId BIGINT = (SELECT user_id FROM APP_USER WHERE email = 'admin1@gmail.com');
DECLARE @roomA105 BIGINT = (SELECT room_id FROM ROOM WHERE room_number = 'A105');
DECLARE @roomA101 BIGINT = (SELECT room_id FROM ROOM WHERE room_number = 'A101');
DECLARE @roomA103 BIGINT = (SELECT room_id FROM ROOM WHERE room_number = 'A103');
DECLARE @roomA104 BIGINT = (SELECT room_id FROM ROOM WHERE room_number = 'A104');
DECLARE @today DATE = CAST(SYSDATETIME() AS DATE);

DECLARE @shiftStart1 DATETIME2(0) = DATEADD(SECOND, DATEDIFF(SECOND, '00:00:00', '14:00:00'), CAST(@today AS DATETIME2(0)));
DECLARE @shiftEnd1 DATETIME2(0) = DATEADD(SECOND, DATEDIFF(SECOND, '00:00:00', '16:00:00'), CAST(@today AS DATETIME2(0)));

INSERT INTO STAFF_ASSIGNMENT (staff_id, room_id, assigned_by, shift_start, shift_end, status, note, started_at, completed_at)
VALUES (@staff1Id, @roomA105, @adminId, @shiftStart1, @shiftEnd1, 'ASSIGNED', 'Routine turnover cleaning', NULL, NULL);
DECLARE @activeAssignId BIGINT = SCOPE_IDENTITY();

-- 1 STAFF_PIN credential for active assignment
DECLARE @staffPinFrom DATETIME2(0) = DATEADD(MINUTE, -10, @shiftStart1);
DECLARE @staffPinUntil DATETIME2(0) = DATEADD(MINUTE, 10, @shiftEnd1);
INSERT INTO ACCESS_CREDENTIAL (credential_type, staff_assignment_id, device_id, pin_ciphertext, pin_hmac, valid_from, valid_until, status)
VALUES ('STAFF_PIN', @activeAssignId, 'MAIN_DOOR_LOCK_01',
       CONVERT(VARBINARY(256), 'B0A4CA18A615D9AA37000AF604AE0E89207AF73B7F6CE00C4149E2C49021627B8DD1', 2),
       CONVERT(VARBINARY(64), '24E927BBEB57C0D9934687E1132B16E454C7E64DB9E9B4B9444540DB0E9BCA77', 2),
       @staffPinFrom, @staffPinUntil, 'PENDING');

-- 3 past COMPLETED assignments
DECLARE @pStartA DATETIME2(0) = DATEADD(SECOND, DATEDIFF(SECOND, '00:00:00', '12:30:00'), CAST(DATEADD(DAY, -4, @today) AS DATETIME2(0)));
DECLARE @pEndA DATETIME2(0) = DATEADD(SECOND, DATEDIFF(SECOND, '00:00:00', '13:30:00'), CAST(DATEADD(DAY, -4, @today) AS DATETIME2(0)));
DECLARE @pStartB DATETIME2(0) = DATEADD(SECOND, DATEDIFF(SECOND, '00:00:00', '13:10:00'), CAST(DATEADD(DAY, -3, @today) AS DATETIME2(0)));
DECLARE @pEndB DATETIME2(0) = DATEADD(SECOND, DATEDIFF(SECOND, '00:00:00', '14:10:00'), CAST(DATEADD(DAY, -3, @today) AS DATETIME2(0)));
DECLARE @pStartC DATETIME2(0) = DATEADD(SECOND, DATEDIFF(SECOND, '00:00:00', '13:30:00'), CAST(DATEADD(DAY, -2, @today) AS DATETIME2(0)));
DECLARE @pEndC DATETIME2(0) = DATEADD(SECOND, DATEDIFF(SECOND, '00:00:00', '14:30:00'), CAST(DATEADD(DAY, -2, @today) AS DATETIME2(0)));

INSERT INTO STAFF_ASSIGNMENT (staff_id, room_id, assigned_by, shift_start, shift_end, status, note, started_at, completed_at)
VALUES
(@staff1Id, @roomA101, @adminId, @pStartA, @pEndA, 'COMPLETED', 'Completed on schedule', @pStartA, @pEndA),
(@staff2Id, @roomA103, @adminId, @pStartB, @pEndB, 'COMPLETED', 'Completed on schedule', @pStartB, @pEndB),
(@staff1Id, @roomA104, @adminId, @pStartC, @pEndC, 'COMPLETED', 'Completed on schedule', @pStartC, @pEndC);
GO

-- ============================================================================
-- 9. ACCESS_LOG, DOOR_EVENT, MOTION_LOG
-- ============================================================================
DECLARE @mainDoor VARCHAR(50) = 'MAIN_DOOR_LOCK_01';
DECLARE @auxDoor VARCHAR(50) = 'MAIN_DOOR_LOCK_02';
DECLARE @activeCred BIGINT = (SELECT TOP 1 credential_id FROM ACCESS_CREDENTIAL WHERE status = 'ACTIVE');
DECLARE @expiredCred BIGINT = (SELECT TOP 1 credential_id FROM ACCESS_CREDENTIAL WHERE status = 'EXPIRED');
DECLARE @revokedCred BIGINT = (SELECT TOP 1 credential_id FROM ACCESS_CREDENTIAL WHERE status = 'REVOKED');
DECLARE @pendingCred BIGINT = (SELECT TOP 1 credential_id FROM ACCESS_CREDENTIAL WHERE status = 'PENDING');

WITH N AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM N WHERE n < 95
),
GuestCreds AS (
    SELECT credential_id, ROW_NUMBER() OVER (ORDER BY credential_id) AS rn
    FROM ACCESS_CREDENTIAL WHERE credential_type = 'GUEST_PIN'
)
INSERT INTO ACCESS_LOG (credential_id, device_id, request_id, attempted_at, result, reason_code)
SELECT
    CASE 
        WHEN n % 5 <> 0 THEN 
            COALESCE((SELECT credential_id FROM GuestCreds WHERE rn = ((n % 15) + 1)), @activeCred)
        WHEN n % 40 = 0 THEN @expiredCred
        WHEN n % 40 = 20 THEN @revokedCred
        WHEN n % 40 = 10 THEN @pendingCred
        ELSE NULL -- WRONG_PIN or LOCKED_OUT with no matched active credential
    END,
    CASE WHEN n % 6 = 0 THEN @auxDoor ELSE @mainDoor END,
    'REQ-' + RIGHT('00000' + CAST(n AS VARCHAR(10)), 5),
    DATEADD(MINUTE, -(n * 320 + ((n * 23 + 7) % 75)), SYSDATETIME()),
    CASE WHEN n % 5 = 0 THEN 'DENIED' ELSE 'ALLOWED' END,
    CASE 
        WHEN n % 5 = 0 THEN
            CASE 
                WHEN n % 10 = 5 THEN 'WRONG_PIN'
                WHEN n % 40 = 0 THEN 'EXPIRED'
                WHEN n % 40 = 20 THEN 'REVOKED'
                WHEN n % 40 = 10 THEN 'NOT_YET_VALID'
                WHEN n % 40 = 30 THEN 'LOCKED_OUT'
                ELSE 'WRONG_PIN'
            END
        ELSE 'OK'
    END
FROM N OPTION (MAXRECURSION 100);

WITH N AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM N WHERE n < 65
)
INSERT INTO DOOR_EVENT (device_id, access_log_id, request_id, event_type, occurred_at)
SELECT
    @mainDoor,
    (SELECT TOP 1 access_log_id FROM ACCESS_LOG WHERE reason_code = 'OK'),
    'REQ-' + RIGHT('00000' + CAST(n AS VARCHAR(10)), 5),
    CASE WHEN n % 2 = 1 THEN 'OPEN' ELSE 'CLOSED' END,
    DATEADD(MINUTE, -(n * 135), SYSDATETIME())
FROM N OPTION (MAXRECURSION 100);

WITH N AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM N WHERE n < 85
)
INSERT INTO MOTION_LOG (device_id, zone_id, event_id, detected_at, distance_cm, snapshot_path)
SELECT
    CASE WHEN n % 4 = 0 THEN 'MAIN_DOOR_LOCK_01' ELSE 'LOBBY_CAM_01' END,
    CASE WHEN n % 4 = 0 THEN 'MAIN_ENTRANCE' ELSE 'LOBBY' END,
    'MOT-' + RIGHT('00000' + CAST(n AS VARCHAR(10)), 5),
    DATEADD(MINUTE, -(n * 230 + ((n * 29 + 13) % 85)), SYSDATETIME()),
    CAST(15.0 + ((n * 47 + 13) % 275) + ((n % 9) * 0.45) AS DECIMAL(10,2)),
    CASE 
        WHEN n % 8 = 0 THEN 'assets/images/snapshots/motion_snap_1.jpg'
        WHEN n % 8 = 2 THEN 'assets/images/snapshots/motion_snap_2.jpg'
        WHEN n % 8 = 4 THEN 'assets/images/snapshots/motion_snap_3.jpg'
        WHEN n % 8 = 6 THEN 'assets/images/snapshots/motion_snap_4.jpg'
        ELSE NULL 
    END
FROM N OPTION (MAXRECURSION 100);
GO

-- ============================================================================
-- 10. ALERT_LOG
-- ============================================================================
DECLARE @adminUser BIGINT = (SELECT user_id FROM APP_USER WHERE email = 'admin1@gmail.com');
DECLARE @bNoShow1 BIGINT = (SELECT booking_id FROM BOOKING WHERE guest_id = (SELECT user_id FROM APP_USER WHERE email = 'guest15@smartstay.local') AND status = 'EXPIRED' AND expire_reason = 'NO_SHOW');
DECLARE @bNoShow2 BIGINT = (SELECT booking_id FROM BOOKING WHERE guest_id = (SELECT user_id FROM APP_USER WHERE email = 'guest16@smartstay.local') AND status = 'EXPIRED' AND expire_reason = 'NO_SHOW');
DECLARE @threshDist BIGINT = (SELECT threshold_id FROM ALERT_THRESHOLD WHERE alert_type = 'LOBBY_PROXIMITY');
DECLARE @threshDoor BIGINT = (SELECT threshold_id FROM ALERT_THRESHOLD WHERE alert_type = 'DOOR_OPEN_TOO_LONG');

INSERT INTO ALERT_LOG (alert_type, severity, status, message, device_id, zone_id, booking_id, threshold_id, acknowledged_by, acknowledged_at, resolved_by, resolved_at, resolution_note, created_at)
VALUES
('BOOKING_NO_SHOW', 'HIGH', 'NEW', 'Guest did not check in within 30 minutes grace period for booking BK-NOSHOW-01', NULL, NULL, @bNoShow1, NULL, NULL, NULL, NULL, NULL, NULL, DATEADD(DAY, -7, SYSDATETIME())),
('BOOKING_NO_SHOW', 'HIGH', 'ACKNOWLEDGED', 'Guest did not check in within 30 minutes grace period for booking BK-NOSHOW-02', NULL, NULL, @bNoShow2, NULL, @adminUser, DATEADD(DAY, -6, SYSDATETIME()), NULL, NULL, NULL, DATEADD(DAY, -6, SYSDATETIME())),
('LOBBY_PROXIMITY', 'LOW', 'NEW', 'Close proximity detected in front lobby (under 50cm)', NULL, 'LOBBY', NULL, @threshDist, NULL, NULL, NULL, NULL, NULL, DATEADD(DAY, -3, SYSDATETIME())),
('DOOR_OPEN_TOO_LONG', 'HIGH', 'RESOLVED', 'Main entrance door held open for more than 30 seconds', 'MAIN_DOOR_LOCK_01', 'MAIN_ENTRANCE', NULL, @threshDoor, @adminUser, DATEADD(DAY, -2, SYSDATETIME()), @adminUser, DATEADD(DAY, -2, SYSDATETIME()), 'Guest held door to carry luggage into lobby', DATEADD(DAY, -2, SYSDATETIME())),
('PIN_BRUTE_FORCE', 'CRITICAL', 'RESOLVED', 'Keypad locked out after 5 consecutive incorrect PIN entries', 'MAIN_DOOR_LOCK_01', 'MAIN_ENTRANCE', NULL, NULL, @adminUser, DATEADD(DAY, -4, SYSDATETIME()), @adminUser, DATEADD(DAY, -4, SYSDATETIME()), 'False alarm; guest verified identity and got correct PIN', DATEADD(DAY, -4, SYSDATETIME())),
('PAYMENT_REVIEW_REQUIRED', 'MEDIUM', 'ACKNOWLEDGED', 'Webhook payment arrived late after order timeout', NULL, NULL, NULL, NULL, @adminUser, DATEADD(DAY, -5, SYSDATETIME()), NULL, NULL, NULL, DATEADD(DAY, -5, SYSDATETIME())),
('CLEANING_SESSION_EXPIRED', 'LOW', 'RESOLVED', 'Housekeeping session exceeded standard cleaning window', NULL, NULL, NULL, NULL, @adminUser, DATEADD(DAY, -3, SYSDATETIME()), @adminUser, DATEADD(DAY, -3, SYSDATETIME()), 'Deep cleaning required extra sanitation time', DATEADD(DAY, -3, SYSDATETIME())),
('DOOR_OPEN_ABNORMAL', 'MEDIUM', 'NEW', 'Door contact sensor opened without valid unlock signal', 'MAIN_DOOR_LOCK_01', 'MAIN_ENTRANCE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, DATEADD(HOUR, -12, SYSDATETIME())),
('SUSPICIOUS_MOTION', 'LOW', 'NEW', 'Unusual motion pattern detected during quiet overnight hours', 'LOBBY_CAM_01', 'LOBBY', NULL, NULL, NULL, NULL, NULL, NULL, NULL, DATEADD(HOUR, -6, SYSDATETIME())),
('DEVICE_OFFLINE', 'HIGH', 'RESOLVED', 'Lobby security camera 2 failed heartbeat check (historical 18/10)', 'LOBBY_CAM_02', 'LOBBY', NULL, NULL, @adminUser, DATEADD(DAY, -6, SYSDATETIME()), @adminUser, DATEADD(DAY, -6, SYSDATETIME()), 'Network port reset; camera reconnected', DATEADD(DAY, -6, SYSDATETIME())),
('DEVICE_OFFLINE', 'HIGH', 'NEW', 'Lobby security camera 2 failed heartbeat check and is offline', 'LOBBY_CAM_02', 'LOBBY', NULL, NULL, NULL, NULL, NULL, NULL, NULL, DATEADD(HOUR, -2, SYSDATETIME())),
('DEVICE_FAULT', 'MEDIUM', 'ACKNOWLEDGED', 'Auxiliary entrance smart lock reported hardware tamper / motor fault', 'MAIN_DOOR_LOCK_02', 'MAIN_ENTRANCE', NULL, NULL, @adminUser, DATEADD(DAY, -1, SYSDATETIME()), NULL, NULL, NULL, DATEADD(DAY, -1, SYSDATETIME())),
('DOOR_OPEN_TOO_LONG', 'HIGH', 'NEW', 'Main entrance door held open for 45 seconds', 'MAIN_DOOR_LOCK_01', 'MAIN_ENTRANCE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, DATEADD(HOUR, -2, SYSDATETIME()));
GO

-- ============================================================================
-- 11. REVIEW
-- ============================================================================
DECLARE @adminUserRev BIGINT = (SELECT TOP 1 user_id FROM APP_USER WHERE role_id = (SELECT role_id FROM ROLE WHERE role_name = 'ADMIN'));

WITH SeedReviews AS (
    SELECT 1 AS rn, 5 AS rating,
           N'Wonderful stay! The digital PIN access made checking in at 10 PM effortless. Room was spotless and the bed was super comfortable.' AS comment,
           'VISIBLE' AS status, NULL AS mod_by, NULL AS mod_at
    UNION ALL SELECT 2, 4, N'Great location right near the lake. Self-service check-in was very convenient. Wi-Fi was fast and reliable for remote work.', 'VISIBLE', NULL, NULL
    UNION ALL SELECT 3, 5, N'Loved the boutique aesthetic and caramel theme! The high-definition projector in Butter Beige was perfect for movie night.', 'VISIBLE', NULL, NULL
    UNION ALL SELECT 4, 3, N'Room was cozy and clean, though street noise was slightly noticeable in the early evening. Automated smart lock worked well.', 'VISIBLE', NULL, NULL
    UNION ALL SELECT 5, 4, N'Booked an evening slot for rest between flights. Extremely smooth process, no front desk lines. Would book again.', 'VISIBLE', NULL, NULL
    UNION ALL SELECT 6, 2, N'The room itself was clean, but the AC took nearly an hour to cool down. Keypad worked fine though.', 'VISIBLE', NULL, NULL
    UNION ALL SELECT 7, 5, N'Exceptional design and attention to detail. Having smart door locks meant no physical keys to worry about while jogging around the lake.', 'VISIBLE', NULL, NULL
    UNION ALL SELECT 8, 4, N'Very good value for money. Fresh Tale room was bright and airy. Minor point: could use an extra hook in the bathroom.', 'VISIBLE', NULL, NULL
    UNION ALL SELECT 9, 3, N'Decent place for a short business stay. Digital check-in is efficient, but parking space was a bit limited.', 'VISIBLE', NULL, NULL
    UNION ALL SELECT 10, 5, N'Impressed by the seamless self check-in! The room smelled fresh, mattress was firm, and Netflix streaming was instantaneous.', 'VISIBLE', NULL, NULL
    UNION ALL SELECT 11, 4, N'Quiet atmosphere and comfortable workspace. Appreciated the clear entrance instructions sent to my email.', 'VISIBLE', NULL, NULL
    UNION ALL SELECT 12, 5, N'Best flexible hourly stay in town. The morning slot was ideal for freshening up before my morning meeting.', 'VISIBLE', NULL, NULL
    UNION ALL SELECT 13, 3, N'Adequate amenities and modern look. The water pressure in the shower could be slightly stronger.', 'VISIBLE', NULL, NULL
    UNION ALL SELECT 14, 4, N'Sunny Love room had wonderful natural lighting. Smart lock responsive on first attempt. Highly recommended.', 'VISIBLE', NULL, NULL
    UNION ALL SELECT 15, 1, N'CHEAP LUXURY WATCHES & CRYPTO SIGNALS! Visit http://t.me/cryptospam2026 for 80% discount codes and free VIP bonuses!', 'HIDDEN', @adminUserRev, DATEADD(DAY, -1, SYSDATETIME())
)
INSERT INTO REVIEW (booking_id, guest_id, room_id, rating, comment, status, moderated_by, moderated_at, created_at)
SELECT
    b.booking_id, b.guest_id, b.room_id,
    sr.rating, sr.comment, sr.status, sr.mod_by, sr.mod_at,
    DATEADD(DAY, - (sr.rn * 2), SYSDATETIME())
FROM (
    SELECT booking_id, guest_id, room_id, ROW_NUMBER() OVER (ORDER BY booking_id) AS rn
    FROM BOOKING WHERE status = 'CHECKED_OUT'
) b
JOIN SeedReviews sr ON b.rn = sr.rn
WHERE b.rn <= 15;
GO

-- ============================================================================
-- SEED SCRIPT COMPLETED
-- ============================================================================
