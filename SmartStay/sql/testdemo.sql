UPDATE BOOKING 
SET checkin_at = DATEADD(minute, 15, SYSDATETIME()) 
WHERE booking_code = 'BK-20261021-0005'; 

UPDATE BOOKING 
SET checkout_at = DATEADD(minute, 15, SYSDATETIME()) 
WHERE booking_code = 'BK-20261021-0005'; 

UPDATE BOOKING 
SET checkout_at = DATEADD(minute, -1, SYSDATETIME()) 
WHERE booking_code = 'BK-20261021-0005'; 