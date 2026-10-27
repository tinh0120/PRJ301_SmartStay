-- ============================================================================
-- SMARTSTAY ROUND 2 MIGRATION SCRIPT: 03_round2_migration.sql
-- Description: Grants ADMIN role all permissions currently held by OWNER.
-- Requirements: Re-runnable (idempotent), safe to execute multiple times.
-- DO NOT RUN AUTOMATICALLY: To be reviewed and executed manually by DBA/User.
-- ============================================================================

-- Grant ADMIN all permissions possessed by OWNER role
INSERT INTO ROLE_PERMISSION (role_id, permission_id)
SELECT admin_r.role_id, op.permission_id
FROM ROLE admin_r
CROSS JOIN (
    SELECT DISTINCT rp.permission_id
    FROM ROLE_PERMISSION rp
    JOIN ROLE owner_r ON rp.role_id = owner_r.role_id
    WHERE owner_r.role_name = 'OWNER'
) op
WHERE admin_r.role_name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 
      FROM ROLE_PERMISSION existing
      WHERE existing.role_id = admin_r.role_id
        AND existing.permission_id = op.permission_id
  );
GO

-- Verification query (display count of ADMIN permissions vs OWNER permissions)
PRINT 'Permissions migration completed successfully.';
DECLARE @ownerCount INT = (
    SELECT COUNT(*) 
    FROM ROLE_PERMISSION rp 
    JOIN ROLE r ON rp.role_id = r.role_id 
    WHERE r.role_name = 'OWNER'
);
DECLARE @adminCount INT = (
    SELECT COUNT(*) 
    FROM ROLE_PERMISSION rp 
    JOIN ROLE r ON rp.role_id = r.role_id 
    WHERE r.role_name = 'ADMIN'
);
PRINT 'Total permissions for OWNER: ' + CAST(@ownerCount AS VARCHAR(10));
PRINT 'Total permissions for ADMIN: ' + CAST(@adminCount AS VARCHAR(10));
GO

-- ============================================================================
-- R9 (Activity logs): no seeded sample snapshots. Clears ONLY the sample paths
-- inserted by 02_seed_data.sql (assets/images/snapshots/motion_snap_*.jpg),
-- so real snapshot paths written by the ESP32 are kept. Re-runnable.
-- ============================================================================
UPDATE MOTION_LOG
SET snapshot_path = NULL
WHERE snapshot_path LIKE 'assets/images/snapshots/motion[_]snap[_]%';
GO

UPDATE ALERT_LOG
SET snapshot_path = NULL
WHERE snapshot_path LIKE 'assets/images/snapshots/motion[_]snap[_]%';
GO

PRINT 'R9: sample motion snapshots cleared.';
GO

-- ============================================================================
-- IoT (Blynk removed): the ESP32-CAM at the main entrance gets its own DEVICE row
-- so /api/iot/snapshot and /api/iot/heartbeat can accept it. Insert only, never
-- overwrites an existing row. Re-runnable.
-- ============================================================================
MERGE DEVICE AS target
USING (SELECT 'MAIN_DOOR_CAM_01' AS device_id) AS src
ON target.device_id = src.device_id
WHEN NOT MATCHED THEN
    INSERT (device_id, device_name, device_type, zone_id, status, is_active, last_seen_at)
    VALUES ('MAIN_DOOR_CAM_01', 'Main Entrance Camera', 'SECURITY_CAMERA', 'MAIN_ENTRANCE', 'OFFLINE', 1, NULL);
GO

PRINT 'IoT: MAIN_DOOR_CAM_01 ready.';
GO

-- ============================================================================
-- IoT clean-up: the building has exactly two real devices, MAIN_DOOR_LOCK_01
-- (ESP32 keypad lock) and MAIN_DOOR_CAM_01 (ESP32-CAM). Every other DEVICE row
-- (seeded MAIN_DOOR_LOCK_02, LOBBY_CAM_01, LOBBY_CAM_02 or ones added on the
-- Devices page) is removed together with its own log rows, child rows first.
-- Rows of the two kept devices are not touched. One transaction: any error or a
-- live PIN on a removed device rolls everything back. Re-runnable (second run
-- finds nothing to delete). Take a backup first.
-- ============================================================================
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @gone TABLE (device_id VARCHAR(50) PRIMARY KEY);
    INSERT INTO @gone (device_id)
    SELECT device_id FROM DEVICE
    WHERE device_id NOT IN ('MAIN_DOOR_LOCK_01', 'MAIN_DOOR_CAM_01');

    -- Safety: never delete a PIN a guest or staff member can still use
    IF EXISTS (SELECT 1 FROM ACCESS_CREDENTIAL
               WHERE device_id IN (SELECT device_id FROM @gone)
                 AND status IN ('PENDING', 'ACTIVE'))
        RAISERROR('IoT clean-up stopped: a PENDING/ACTIVE PIN is bound to a device being removed. Nothing was changed.', 16, 1);

    DECLARE @creds TABLE (credential_id BIGINT PRIMARY KEY);
    INSERT INTO @creds SELECT credential_id FROM ACCESS_CREDENTIAL
    WHERE device_id IN (SELECT device_id FROM @gone);

    DECLARE @access TABLE (access_log_id BIGINT PRIMARY KEY);
    INSERT INTO @access SELECT access_log_id FROM ACCESS_LOG
    WHERE device_id IN (SELECT device_id FROM @gone)
       OR credential_id IN (SELECT credential_id FROM @creds);

    DECLARE @motion TABLE (motion_log_id BIGINT PRIMARY KEY);
    INSERT INTO @motion SELECT motion_log_id FROM MOTION_LOG
    WHERE device_id IN (SELECT device_id FROM @gone);

    DECLARE @thresholds TABLE (threshold_id BIGINT PRIMARY KEY);
    INSERT INTO @thresholds SELECT threshold_id FROM ALERT_THRESHOLD
    WHERE device_id IN (SELECT device_id FROM @gone);

    DELETE FROM ALERT_LOG
    WHERE device_id IN (SELECT device_id FROM @gone)
       OR motion_log_id IN (SELECT motion_log_id FROM @motion);
    PRINT 'IoT clean-up: ALERT_LOG rows deleted = ' + CAST(@@ROWCOUNT AS VARCHAR(10));

    -- Alerts of the kept devices may point at a rule that is removed: keep the alert, drop the link
    UPDATE ALERT_LOG SET threshold_id = NULL
    WHERE threshold_id IN (SELECT threshold_id FROM @thresholds);

    DELETE FROM DOOR_EVENT
    WHERE device_id IN (SELECT device_id FROM @gone)
       OR access_log_id IN (SELECT access_log_id FROM @access);
    PRINT 'IoT clean-up: DOOR_EVENT rows deleted = ' + CAST(@@ROWCOUNT AS VARCHAR(10));

    DELETE FROM ACCESS_LOG WHERE access_log_id IN (SELECT access_log_id FROM @access);
    PRINT 'IoT clean-up: ACCESS_LOG rows deleted = ' + CAST(@@ROWCOUNT AS VARCHAR(10));

    DELETE FROM MOTION_LOG WHERE motion_log_id IN (SELECT motion_log_id FROM @motion);
    PRINT 'IoT clean-up: MOTION_LOG rows deleted = ' + CAST(@@ROWCOUNT AS VARCHAR(10));

    DELETE FROM ALERT_THRESHOLD WHERE threshold_id IN (SELECT threshold_id FROM @thresholds);
    PRINT 'IoT clean-up: ALERT_THRESHOLD rows deleted = ' + CAST(@@ROWCOUNT AS VARCHAR(10));

    DELETE FROM ACCESS_CREDENTIAL WHERE credential_id IN (SELECT credential_id FROM @creds);
    PRINT 'IoT clean-up: ACCESS_CREDENTIAL rows (expired/revoked) deleted = ' + CAST(@@ROWCOUNT AS VARCHAR(10));

    DELETE FROM DEVICE WHERE device_id IN (SELECT device_id FROM @gone);
    PRINT 'IoT clean-up: DEVICE rows deleted = ' + CAST(@@ROWCOUNT AS VARCHAR(10));

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    PRINT 'IoT clean-up rolled back: ' + ERROR_MESSAGE();
END CATCH;
GO

SELECT device_id, device_name, device_type, zone_id, status FROM DEVICE ORDER BY device_id;
GO
