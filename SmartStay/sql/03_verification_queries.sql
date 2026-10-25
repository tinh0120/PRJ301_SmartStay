/*
================================================================================
SmartStay Verification Queries (SELECT only)
Database: SmartStayDB
Run after 01_create_schema.sql (and after 02_seed_data.sql in T12).
================================================================================
*/

USE SmartStayDB;
GO

-- ============================================================================
-- 1. Row count per table (18 rows) and tables with 0 rows
-- ============================================================================
SELECT t.name AS table_name,
       SUM(CASE WHEN p.index_id IN (0,1) THEN p.rows ELSE 0 END) AS row_count
FROM sys.tables t
JOIN sys.partitions p ON p.object_id = t.object_id
WHERE t.is_ms_shipped = 0
GROUP BY t.name
ORDER BY t.name ASC;

-- Tables with 0 rows (expect 0 rows after seed data in T12)
SELECT t.name AS empty_table_name
FROM sys.tables t
JOIN sys.partitions p ON p.object_id = t.object_id
WHERE t.is_ms_shipped = 0
GROUP BY t.name
HAVING SUM(CASE WHEN p.index_id IN (0,1) THEN p.rows ELSE 0 END) = 0
ORDER BY t.name ASC;
GO

-- ============================================================================
-- 2. Foreign key validity and orphan checks
-- ============================================================================
-- Untrusted or disabled foreign keys (expect none)
SELECT name AS untrusted_fk_name
FROM sys.foreign_keys
WHERE is_not_trusted = 1 OR is_disabled = 1;

-- Orphan check: BOOKING -> APP_USER (expect 0)
SELECT b.booking_id, b.guest_id
FROM BOOKING b
LEFT JOIN APP_USER u ON b.guest_id = u.user_id
WHERE u.user_id IS NULL;

-- Orphan check: BOOKING_SLOT -> BOOKING (expect 0)
SELECT bs.booking_slot_id, bs.booking_id
FROM BOOKING_SLOT bs
LEFT JOIN BOOKING b ON bs.booking_id = b.booking_id
WHERE b.booking_id IS NULL;

-- Orphan check: PAYMENT -> BOOKING (expect 0)
SELECT p.payment_id, p.booking_id
FROM PAYMENT p
LEFT JOIN BOOKING b ON p.booking_id = b.booking_id
WHERE b.booking_id IS NULL;
GO

-- ============================================================================
-- 3. Duplicate HELD slots check (Anti double-booking verification)
-- ============================================================================
-- Expect 0 rows
SELECT room_slot_id, slot_date, COUNT(*) AS held_count
FROM BOOKING_SLOT
WHERE hold_status = 'HELD'
GROUP BY room_slot_id, slot_date
HAVING COUNT(*) > 1;
GO

-- ============================================================================
-- 4. Duplicate live PIN check
-- ============================================================================
-- Expect 0 rows
SELECT device_id, pin_hmac, COUNT(*) AS live_pin_count
FROM ACCESS_CREDENTIAL
WHERE status IN ('PENDING', 'ACTIVE')
GROUP BY device_id, pin_hmac
HAVING COUNT(*) > 1;
GO

-- ============================================================================
-- 5. Money formula and discount consistency check
-- ============================================================================
-- 5a. Total amount formula mismatch (expect 0)
SELECT booking_id, subtotal_amount, discount_amount, total_amount
FROM BOOKING
WHERE total_amount <> (subtotal_amount - discount_amount);

-- 5b. Subtotal mismatch against sum of reserved slot prices (expect 0)
SELECT b.booking_id, b.subtotal_amount,
       ISNULL(SUM(bs.list_price), 0) AS calculated_subtotal
FROM BOOKING b
LEFT JOIN BOOKING_SLOT bs ON b.booking_id = bs.booking_id
GROUP BY b.booking_id, b.subtotal_amount
HAVING b.subtotal_amount <> ISNULL(SUM(bs.list_price), 0);

-- 5c. Discount percentage rule mismatch (1 slot -> 0%, 2 -> 10%, 3 -> 20%, 4 -> 50%) (expect 0)
SELECT b.booking_id, b.discount_percent, COUNT(bs.booking_slot_id) AS slot_count
FROM BOOKING b
JOIN BOOKING_SLOT bs ON b.booking_id = bs.booking_id
GROUP BY b.booking_id, b.discount_percent
HAVING (COUNT(bs.booking_slot_id) = 1 AND b.discount_percent <> 0)
    OR (COUNT(bs.booking_slot_id) = 2 AND b.discount_percent <> 10)
    OR (COUNT(bs.booking_slot_id) = 3 AND b.discount_percent <> 20)
    OR (COUNT(bs.booking_slot_id) = 4 AND b.discount_percent <> 50);

-- 5d. Discount amount rounding mismatch (expect 0)
SELECT booking_id, subtotal_amount, discount_percent, discount_amount,
       ROUND(subtotal_amount * discount_percent / 100.0, 0) AS expected_discount
FROM BOOKING
WHERE discount_amount <> ROUND(subtotal_amount * discount_percent / 100.0, 0);
GO

-- ============================================================================
-- 6. Consecutive slots rule verification
-- ============================================================================
-- Expect 0 rows: A booking with N slots must have MAX(sort_order) - MIN(sort_order) + 1 = N
SELECT b.booking_id,
       MIN(rts.sort_order) AS min_order,
       MAX(rts.sort_order) AS max_order,
       COUNT(bs.booking_slot_id) AS slot_count
FROM BOOKING b
JOIN BOOKING_SLOT bs ON b.booking_id = bs.booking_id
JOIN ROOM_TIME_SLOT rts ON bs.room_slot_id = rts.room_slot_id
GROUP BY b.booking_id
HAVING (MAX(rts.sort_order) - MIN(rts.sort_order) + 1) <> COUNT(bs.booking_slot_id);
GO

-- ============================================================================
-- 7. Slot schedule overlap per room
-- ============================================================================
-- 7a. Non-midnight slots overlap: next.start_time < current.end_time (expect 0)
SELECT s1.room_id, s1.slot_code AS current_slot, s1.end_time AS current_end,
       s2.slot_code AS next_slot, s2.start_time AS next_start
FROM ROOM_TIME_SLOT s1
JOIN ROOM_TIME_SLOT s2 ON s1.room_id = s2.room_id AND s2.sort_order = s1.sort_order + 1
WHERE s1.crosses_midnight = 0 AND s2.start_time < s1.end_time;

-- 7b. Overnight slot S4 end vs next day's S1 start (expect 0)
SELECT s4.room_id, s4.end_time AS s4_morning_end, s1.start_time AS s1_morning_start
FROM ROOM_TIME_SLOT s4
JOIN ROOM_TIME_SLOT s1 ON s4.room_id = s1.room_id AND s1.slot_code = 'S1'
WHERE s4.slot_code = 'S4' AND s4.crosses_midnight = 1 AND s4.end_time > s1.start_time;
GO

-- ============================================================================
-- 8. BOOKING_SLOT room_id and slot_date integrity check
-- ============================================================================
-- Expect 0 rows: Denormalized room_id and slot_date in BOOKING_SLOT must match BOOKING
SELECT bs.booking_slot_id, bs.booking_id,
       bs.room_id AS slot_room, b.room_id AS booking_room,
       bs.slot_date AS slot_date, b.slot_date AS booking_date
FROM BOOKING_SLOT bs
JOIN BOOKING b ON bs.booking_id = b.booking_id
WHERE bs.room_id <> b.room_id OR bs.slot_date <> b.slot_date;
GO

-- ============================================================================
-- 9. Status consistency checks
-- ============================================================================
-- 9a. Cancelled or payment timeout bookings that still hold slots (expect 0)
SELECT b.booking_id, b.status, b.expire_reason, bs.booking_slot_id, bs.hold_status
FROM BOOKING b
JOIN BOOKING_SLOT bs ON b.booking_id = bs.booking_id
WHERE (b.status = 'CANCELLED' OR (b.status = 'EXPIRED' AND b.expire_reason = 'PAYMENT_TIMEOUT'))
  AND bs.hold_status = 'HELD';

-- 9b. No-show bookings with held slots (operational count)
SELECT b.booking_id, b.booking_code, b.status, b.expire_reason,
       COUNT(bs.booking_slot_id) AS held_slot_count
FROM BOOKING b
JOIN BOOKING_SLOT bs ON b.booking_id = bs.booking_id
WHERE b.status = 'EXPIRED' AND b.expire_reason = 'NO_SHOW' AND bs.hold_status = 'HELD'
GROUP BY b.booking_id, b.booking_code, b.status, b.expire_reason;
GO

-- ============================================================================
-- 10. Core inventory counts
-- ============================================================================
SELECT COUNT(*) AS room_count FROM ROOM;                      -- Expect 6
SELECT COUNT(*) AS room_time_slot_count FROM ROOM_TIME_SLOT;  -- Expect 24
GO

-- ============================================================================
-- 11. Demonstration and reporting queries
-- ============================================================================
-- 11a. Daily paid revenue in the last 30 days
SELECT CAST(paid_at AS DATE) AS revenue_date,
       COUNT(*) AS payment_count,
       SUM(amount) AS total_revenue_vnd
FROM PAYMENT
WHERE status = 'PAID' AND paid_at >= DATEADD(DAY, -30, SYSDATETIME())
GROUP BY CAST(paid_at AS DATE)
ORDER BY revenue_date DESC;

-- 11b. Alerts summary by type and status
SELECT alert_type, status, severity, COUNT(*) AS alert_count
FROM ALERT_LOG
GROUP BY alert_type, status, severity
ORDER BY alert_count DESC;

-- 11c. Top 10 latest access logs
SELECT TOP 10 access_log_id, device_id, attempted_at, result, reason_code
FROM ACCESS_LOG
ORDER BY attempted_at DESC;
GO

/*
================================================================================
Optional negative tests (run manually on an empty DB, then rerun 01)
Uncomment individual statements below to verify constraint enforcement.
================================================================================


-- 2. Negative Test: BOOKING with illegal discount_percent = 25 (Violates CK_BOOKING_discount_percent)
-- INSERT INTO BOOKING (guest_id, room_id, slot_date, checkin_at, checkout_at, subtotal_amount, discount_percent, discount_amount, total_amount)
-- VALUES (1, 1, '2026-10-15', '2026-10-15 07:00', '2026-10-15 11:30', 300000, 25, 75000, 225000);

-- 3. Negative Test: BOOKING math mismatch (total_amount <> subtotal - discount) (Violates CK_BOOKING_amounts)
-- INSERT INTO BOOKING (guest_id, room_id, slot_date, checkin_at, checkout_at, subtotal_amount, discount_percent, discount_amount, total_amount)
-- VALUES (1, 1, '2026-10-15', '2026-10-15 07:00', '2026-10-15 11:30', 300000, 0, 0, 290000);

-- 4. Negative Test: Two BOOKING_SLOT rows with same slot_date and room_slot_id held (Violates UX_BOOKING_SLOT_held)
-- INSERT INTO BOOKING_SLOT (booking_id, room_id, room_slot_id, slot_date, start_at, end_at, list_price, hold_status)
-- VALUES (1, 1, 1, '2026-10-15', '2026-10-15 07:00', '2026-10-15 11:30', 300000, 'HELD');
-- INSERT INTO BOOKING_SLOT (booking_id, room_id, room_slot_id, slot_date, start_at, end_at, list_price, hold_status)
-- VALUES (2, 1, 1, '2026-10-15', '2026-10-15 07:00', '2026-10-15 11:30', 300000, 'HELD');

-- 5. Negative Test: ROOM_TIME_SLOT S4 with crosses_midnight = 0 and end_time < start_time (Violates CK_ROOM_TIME_SLOT_times)
-- INSERT INTO ROOM_TIME_SLOT (room_id, slot_code, slot_name, start_time, end_time, crosses_midnight, list_price, sort_order)
-- VALUES (1, 'S4', 'Overnight', '22:00', '06:00', 0, 450000, 4);

-- 6. Negative Test: ACCESS_CREDENTIAL with both booking_id and staff_assignment_id set (Violates CK_ACCESS_CREDENTIAL_owner)
-- INSERT INTO ACCESS_CREDENTIAL (credential_type, booking_id, staff_assignment_id, device_id, valid_from, valid_until)
-- VALUES ('GUEST_PIN', 1, 1, 'MAIN_DOOR_LOCK_01', '2026-10-15 07:00', '2026-10-15 11:30');

-- 7. Negative Test: DEVICE with lowercase id 'main_door' (Violates CK_DEVICE_device_id)
-- INSERT INTO DEVICE (device_id, device_name, device_type, zone_id)
-- VALUES ('main_door', 'Main Entrance Lock', 'SMART_LOCK', 'MAIN_ENTRANCE');
*/
