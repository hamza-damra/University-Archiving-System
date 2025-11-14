-- =====================================================
-- Session Management Utility Queries
-- =====================================================
-- Useful SQL queries for monitoring and maintaining
-- Spring Session JDBC tables
-- =====================================================

-- =====================================================
-- MONITORING QUERIES
-- =====================================================

-- 1. Count active sessions
SELECT COUNT(*) AS active_sessions
FROM SPRING_SESSION
WHERE EXPIRY_TIME > UNIX_TIMESTAMP() * 1000;

-- 2. List all active sessions with user details
SELECT 
    SESSION_ID,
    PRINCIPAL_NAME AS username,
    FROM_UNIXTIME(CREATION_TIME/1000) AS created_at,
    FROM_UNIXTIME(LAST_ACCESS_TIME/1000) AS last_active,
    FROM_UNIXTIME(EXPIRY_TIME/1000) AS expires_at,
    MAX_INACTIVE_INTERVAL AS timeout_seconds,
    TIMESTAMPDIFF(SECOND, FROM_UNIXTIME(LAST_ACCESS_TIME/1000), NOW()) AS inactive_seconds
FROM SPRING_SESSION
WHERE EXPIRY_TIME > UNIX_TIMESTAMP() * 1000
ORDER BY LAST_ACCESS_TIME DESC;

-- 3. Count sessions per user
SELECT 
    PRINCIPAL_NAME AS username,
    COUNT(*) AS session_count,
    FROM_UNIXTIME(MAX(LAST_ACCESS_TIME)/1000) AS most_recent_activity
FROM SPRING_SESSION
WHERE EXPIRY_TIME > UNIX_TIMESTAMP() * 1000
GROUP BY PRINCIPAL_NAME
ORDER BY session_count DESC;

-- 4. Find expired but not cleaned sessions
SELECT COUNT(*) AS expired_not_cleaned
FROM SPRING_SESSION
WHERE EXPIRY_TIME <= UNIX_TIMESTAMP() * 1000;

-- 5. Session age distribution
SELECT 
    CASE 
        WHEN age_minutes < 5 THEN '0-5 minutes'
        WHEN age_minutes < 15 THEN '5-15 minutes'
        WHEN age_minutes < 30 THEN '15-30 minutes'
        WHEN age_minutes < 60 THEN '30-60 minutes'
        ELSE '60+ minutes'
    END AS age_range,
    COUNT(*) AS session_count
FROM (
    SELECT TIMESTAMPDIFF(MINUTE, FROM_UNIXTIME(CREATION_TIME/1000), NOW()) AS age_minutes
    FROM SPRING_SESSION
    WHERE EXPIRY_TIME > UNIX_TIMESTAMP() * 1000
) AS ages
GROUP BY age_range
ORDER BY 
    CASE age_range
        WHEN '0-5 minutes' THEN 1
        WHEN '5-15 minutes' THEN 2
        WHEN '15-30 minutes' THEN 3
        WHEN '30-60 minutes' THEN 4
        ELSE 5
    END;

-- 6. Most active users (by session count)
SELECT 
    PRINCIPAL_NAME AS username,
    COUNT(*) AS total_sessions,
    SUM(CASE WHEN EXPIRY_TIME > UNIX_TIMESTAMP() * 1000 THEN 1 ELSE 0 END) AS active_sessions,
    FROM_UNIXTIME(MAX(CREATION_TIME)/1000) AS last_login
FROM SPRING_SESSION
GROUP BY PRINCIPAL_NAME
ORDER BY total_sessions DESC
LIMIT 10;

-- 7. Session attributes summary
SELECT 
    s.SESSION_ID,
    s.PRINCIPAL_NAME AS username,
    COUNT(sa.ATTRIBUTE_NAME) AS attribute_count,
    GROUP_CONCAT(sa.ATTRIBUTE_NAME SEPARATOR ', ') AS attributes
FROM SPRING_SESSION s
LEFT JOIN SPRING_SESSION_ATTRIBUTES sa ON s.PRIMARY_ID = sa.SESSION_PRIMARY_ID
WHERE s.EXPIRY_TIME > UNIX_TIMESTAMP() * 1000
GROUP BY s.SESSION_ID, s.PRINCIPAL_NAME
LIMIT 20;

-- 8. Database table sizes
SELECT 
    table_name,
    table_rows,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb,
    ROUND((index_length / 1024 / 1024), 2) AS index_size_mb,
    ROUND((data_length / 1024 / 1024), 2) AS data_size_mb
FROM information_schema.TABLES
WHERE table_schema = DATABASE()
AND table_name LIKE 'SPRING_SESSION%'
ORDER BY (data_length + index_length) DESC;

-- 9. Session creation rate (last hour)
SELECT 
    DATE_FORMAT(FROM_UNIXTIME(CREATION_TIME/1000), '%Y-%m-%d %H:%i') AS time_bucket,
    COUNT(*) AS sessions_created
FROM SPRING_SESSION
WHERE FROM_UNIXTIME(CREATION_TIME/1000) >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY time_bucket
ORDER BY time_bucket DESC;

-- 10. Index usage statistics
SELECT 
    INDEX_NAME,
    SEQ_IN_INDEX,
    COLUMN_NAME,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'SPRING_SESSION'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- =====================================================
-- MAINTENANCE QUERIES
-- =====================================================

-- 11. Manual cleanup of expired sessions
DELETE FROM SPRING_SESSION
WHERE EXPIRY_TIME <= UNIX_TIMESTAMP() * 1000;

-- 12. Delete sessions older than 7 days (regardless of expiry)
DELETE FROM SPRING_SESSION
WHERE CREATION_TIME < (UNIX_TIMESTAMP() - 604800) * 1000;

-- 13. Delete all sessions for a specific user
-- DELETE FROM SPRING_SESSION
-- WHERE PRINCIPAL_NAME = 'user@example.com';

-- 14. Invalidate all sessions (force global logout)
-- WARNING: This logs out ALL users!
-- DELETE FROM SPRING_SESSION;

-- 15. Optimize tables (run during maintenance window)
OPTIMIZE TABLE SPRING_SESSION;
OPTIMIZE TABLE SPRING_SESSION_ATTRIBUTES;

-- 16. Analyze tables (update statistics)
ANALYZE TABLE SPRING_SESSION;
ANALYZE TABLE SPRING_SESSION_ATTRIBUTES;

-- 17. Check for orphaned attributes (shouldn't exist with FK constraint)
SELECT COUNT(*) AS orphaned_attributes
FROM SPRING_SESSION_ATTRIBUTES sa
LEFT JOIN SPRING_SESSION s ON sa.SESSION_PRIMARY_ID = s.PRIMARY_ID
WHERE s.PRIMARY_ID IS NULL;

-- =====================================================
-- PERFORMANCE QUERIES
-- =====================================================

-- 18. Slow query analysis (session lookups)
EXPLAIN SELECT * 
FROM SPRING_SESSION 
WHERE SESSION_ID = 'sample-session-id';

-- 19. Check for missing indexes
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    (SELECT COUNT(*) FROM SPRING_SESSION) AS row_count,
    CARDINALITY,
    ROUND((CARDINALITY / (SELECT COUNT(*) FROM SPRING_SESSION)) * 100, 2) AS selectivity_percent
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'SPRING_SESSION'
AND INDEX_NAME != 'PRIMARY';

-- 20. Find sessions with most attributes
SELECT 
    s.SESSION_ID,
    s.PRINCIPAL_NAME,
    COUNT(sa.ATTRIBUTE_NAME) AS attribute_count,
    SUM(LENGTH(sa.ATTRIBUTE_BYTES)) AS total_bytes
FROM SPRING_SESSION s
JOIN SPRING_SESSION_ATTRIBUTES sa ON s.PRIMARY_ID = sa.SESSION_PRIMARY_ID
GROUP BY s.SESSION_ID, s.PRINCIPAL_NAME
ORDER BY total_bytes DESC
LIMIT 10;

-- =====================================================
-- SECURITY QUERIES
-- =====================================================

-- 21. Find users with multiple concurrent sessions
SELECT 
    PRINCIPAL_NAME AS username,
    COUNT(*) AS concurrent_sessions,
    GROUP_CONCAT(SESSION_ID SEPARATOR ', ') AS session_ids
FROM SPRING_SESSION
WHERE EXPIRY_TIME > UNIX_TIMESTAMP() * 1000
GROUP BY PRINCIPAL_NAME
HAVING COUNT(*) > 1
ORDER BY concurrent_sessions DESC;

-- 22. Find sessions from different IPs (requires custom attribute storage)
-- This query template - customize based on your attribute storage
SELECT 
    s.SESSION_ID,
    s.PRINCIPAL_NAME,
    COUNT(DISTINCT sa.ATTRIBUTE_BYTES) AS unique_attributes
FROM SPRING_SESSION s
JOIN SPRING_SESSION_ATTRIBUTES sa ON s.PRIMARY_ID = sa.SESSION_PRIMARY_ID
WHERE sa.ATTRIBUTE_NAME LIKE '%IP%'
GROUP BY s.SESSION_ID, s.PRINCIPAL_NAME
HAVING unique_attributes > 1;

-- 23. Find recently created sessions (potential attack detection)
SELECT 
    PRINCIPAL_NAME AS username,
    COUNT(*) AS recent_sessions,
    MIN(FROM_UNIXTIME(CREATION_TIME/1000)) AS first_session,
    MAX(FROM_UNIXTIME(CREATION_TIME/1000)) AS last_session
FROM SPRING_SESSION
WHERE FROM_UNIXTIME(CREATION_TIME/1000) >= DATE_SUB(NOW(), INTERVAL 5 MINUTE)
GROUP BY PRINCIPAL_NAME
HAVING recent_sessions > 3
ORDER BY recent_sessions DESC;

-- =====================================================
-- REPORTING QUERIES
-- =====================================================

-- 24. Daily session statistics
SELECT 
    DATE(FROM_UNIXTIME(CREATION_TIME/1000)) AS date,
    COUNT(DISTINCT SESSION_ID) AS total_sessions,
    COUNT(DISTINCT PRINCIPAL_NAME) AS unique_users,
    AVG(MAX_INACTIVE_INTERVAL) AS avg_timeout_seconds,
    MIN(FROM_UNIXTIME(CREATION_TIME/1000)) AS first_session,
    MAX(FROM_UNIXTIME(CREATION_TIME/1000)) AS last_session
FROM SPRING_SESSION
WHERE FROM_UNIXTIME(CREATION_TIME/1000) >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(FROM_UNIXTIME(CREATION_TIME/1000))
ORDER BY date DESC;

-- 25. Peak concurrent sessions (approximation)
SELECT 
    FROM_UNIXTIME(FLOOR(CREATION_TIME/1000/3600)*3600) AS hour,
    COUNT(*) AS sessions_in_hour
FROM SPRING_SESSION
WHERE FROM_UNIXTIME(CREATION_TIME/1000) >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
GROUP BY hour
ORDER BY sessions_in_hour DESC
LIMIT 10;

-- =====================================================
-- BACKUP & RESTORE
-- =====================================================

-- 26. Export active sessions (for backup)
-- Run from command line:
-- mysqldump -u root -p archive_system SPRING_SESSION SPRING_SESSION_ATTRIBUTES > session_backup.sql

-- 27. Count records before backup
SELECT 
    'SPRING_SESSION' AS table_name,
    COUNT(*) AS record_count
FROM SPRING_SESSION
UNION ALL
SELECT 
    'SPRING_SESSION_ATTRIBUTES' AS table_name,
    COUNT(*) AS record_count
FROM SPRING_SESSION_ATTRIBUTES;

-- =====================================================
-- HEALTH CHECKS
-- =====================================================

-- 28. Overall session health
SELECT 
    (SELECT COUNT(*) FROM SPRING_SESSION WHERE EXPIRY_TIME > UNIX_TIMESTAMP() * 1000) AS active_sessions,
    (SELECT COUNT(*) FROM SPRING_SESSION WHERE EXPIRY_TIME <= UNIX_TIMESTAMP() * 1000) AS expired_sessions,
    (SELECT COUNT(DISTINCT PRINCIPAL_NAME) FROM SPRING_SESSION WHERE EXPIRY_TIME > UNIX_TIMESTAMP() * 1000) AS active_users,
    (SELECT ROUND(((data_length + index_length) / 1024 / 1024), 2) 
     FROM information_schema.TABLES 
     WHERE table_name = 'SPRING_SESSION' 
     AND table_schema = DATABASE()) AS table_size_mb,
    NOW() AS check_time;

-- 29. Connection pool status (run from application logs or JMX)
-- Check HikariCP metrics in application logs

-- 30. Quick health check (single query)
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'HEALTHY'
        ELSE 'NO_SESSIONS'
    END AS status,
    COUNT(*) AS active_sessions
FROM SPRING_SESSION
WHERE EXPIRY_TIME > UNIX_TIMESTAMP() * 1000;

-- =====================================================
-- NOTES
-- =====================================================
-- 
-- 1. Run monitoring queries regularly (every 5-15 minutes)
-- 2. Run maintenance queries during low-traffic periods
-- 3. Always backup before running DELETE queries
-- 4. Use EXPLAIN to verify query performance
-- 5. Monitor table growth and plan for archiving/cleanup
-- 6. Set up alerts for abnormal session counts
-- 7. Consider partitioning tables if > 1M sessions
-- 
-- =====================================================
