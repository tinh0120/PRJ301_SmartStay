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
