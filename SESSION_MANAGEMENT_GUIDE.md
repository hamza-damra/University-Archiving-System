# Database-Backed Session Management - Implementation Guide

## Overview

This document provides a comprehensive guide to the database-backed HTTP session management implementation for the Archive System. The system uses Spring Session JDBC to persist sessions to MySQL, providing secure, scalable session management that survives application restarts and supports horizontal scaling.

## Table of Contents

1. [Features](#features)
2. [Architecture](#architecture)
3. [Configuration](#configuration)
4. [Database Schema](#database-schema)
5. [Security Features](#security-features)
6. [API Endpoints](#api-endpoints)
7. [Testing](#testing)
8. [Deployment Checklist](#deployment-checklist)
9. [Monitoring & Maintenance](#monitoring--maintenance)
10. [Troubleshooting](#troubleshooting)

---

## Features

### ✅ Implemented Features

- **Database Persistence**: Sessions stored in MySQL using Spring Session JDBC
- **Restart Resilience**: Sessions survive application restarts
- **Horizontal Scaling**: Multiple application instances share session state
- **Secure Cookies**: HttpOnly, Secure (in production), SameSite attributes
- **Session Fixation Protection**: Automatic session ID rotation on authentication
- **Configurable Timeout**: Environment-specific session timeout (30 min dev, 60 min prod)
- **Automatic Cleanup**: Scheduled job removes expired sessions every 15 minutes
- **Connection Pooling**: HikariCP for optimal database performance
- **Indexed Queries**: Efficient session lookup via database indexes
- **Session Management API**: RESTful endpoints for session operations
- **Comprehensive Tests**: Integration tests verify all security features

---

## Architecture

### Session Flow

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Browser   │────────▶│ Spring Boot  │────────▶│   MySQL     │
│             │ HTTPS   │ Application  │  JDBC   │  Database   │
│  (Cookie)   │◀────────│ (Session)    │◀────────│  (Tables)   │
└─────────────┘         └──────────────┘         └─────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ Spring Security  │
                    │ Session Fixation │
                    │    Protection    │
                    └──────────────────┘
```

### Components

1. **SessionConfig**: Configures Spring Session JDBC and cookie serializer
2. **SecurityConfig**: Enables session management with fixation protection
3. **AuthService**: Implements session rotation on login
4. **SessionController**: Provides session management API endpoints
5. **Flyway Migration**: Creates required database tables
6. **Integration Tests**: Verifies session behavior

---

## Configuration

### 1. Maven Dependencies

Already added to `pom.xml`:

```xml
<!-- Spring Session JDBC -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-jdbc</artifactId>
</dependency>

<!-- Flyway for migrations -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

### 2. Application Properties

#### Base Configuration (`application.properties`)

```properties
# Database connection (HikariCP pooling)
spring.datasource.url=jdbc:mysql://localhost:3306/archive_system?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration

# Spring Session
spring.session.store-type=jdbc
spring.session.jdbc.initialize-schema=never
spring.session.jdbc.table-name=SPRING_SESSION
spring.session.timeout=30m
spring.session.jdbc.cleanup-cron=0 */15 * * * *
```

#### Development Profile (`application-dev.properties`)

```properties
# Use with: --spring.profiles.active=dev

session.cookie.secure=false  # No HTTPS required locally
session.cookie.same-site=Lax
logging.level.org.springframework.session=DEBUG
```

#### Production Profile (`application-prod.properties`)

```properties
# Use with: --spring.profiles.active=prod

session.cookie.secure=true  # HTTPS required
session.cookie.same-site=Strict
spring.session.timeout=60m
spring.datasource.url=${DB_URL}  # Use environment variables
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

---

## Database Schema

### Tables Created by Flyway Migration

The migration file `V1__Create_Spring_Session_Tables.sql` creates:

#### 1. SPRING_SESSION

Stores session metadata.

```sql
CREATE TABLE SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    PRIMARY KEY (PRIMARY_ID)
);

-- Indexes for performance
CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);
```

#### 2. SPRING_SESSION_ATTRIBUTES

Stores session attributes (e.g., security context, user data).

```sql
CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BLOB NOT NULL,
    PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);
```

### Migration Execution

Flyway automatically runs migrations on application startup:

```bash
# Check migration status
./mvnw flyway:info

# Run migrations manually (if needed)
./mvnw flyway:migrate

# Clean database (dev only - DANGEROUS!)
./mvnw flyway:clean
```

---

## Security Features

### 1. Secure Cookie Configuration

Configured in `SessionConfig.java`:

```java
@Bean
public CookieSerializer cookieSerializer() {
    DefaultCookieSerializer serializer = new DefaultCookieSerializer();
    serializer.setCookieName("ARCHIVESESSION");  // Custom name
    serializer.setUseHttpOnlyCookie(true);       // XSS protection
    serializer.setUseSecureCookie(secureCookie); // HTTPS only (prod)
    serializer.setSameSite(sameSite);            // CSRF protection
    serializer.setCookiePath("/");               // Application-wide
    return serializer;
}
```

### 2. Session Fixation Protection

Configured in `SecurityConfig.java`:

```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
    .sessionFixation().newSession()  // Create new session on auth
    .maximumSessions(5)              // Max concurrent sessions
    .maxSessionsPreventsLogin(false) // Invalidate oldest
)
```

### 3. Session Rotation on Login

Implemented in `AuthService.login()`:

1. Invalidate old session (if exists)
2. Authenticate user
3. Create new session with new ID
4. Store authentication in new session

This prevents session fixation attacks.

### 4. Session Cleanup

Automatic cleanup configured:

```properties
spring.session.jdbc.cleanup-cron=0 */15 * * * *
```

Runs every 15 minutes to remove expired sessions.

---

## API Endpoints

### Session Management Endpoints

All endpoints are under `/api/session`:

#### 1. Get Session Info

```http
GET /api/session/info
```

Returns current session details:

```json
{
  "success": true,
  "message": "Session information retrieved",
  "data": {
    "active": true,
    "sessionId": "abc123...",
    "creationTime": "2025-11-14T10:30:00Z",
    "lastAccessedTime": "2025-11-14T10:45:00Z",
    "maxInactiveInterval": 1800,
    "expiresInSeconds": 600,
    "isNew": false
  }
}
```

#### 2. Check Session Status

```http
GET /api/session/status
```

Returns whether session is active:

```json
{
  "success": true,
  "data": {
    "active": true
  }
}
```

#### 3. Refresh Session

```http
POST /api/session/refresh
```

Extends session timeout by updating last access time.

#### 4. Invalidate Session

```http
POST /api/session/invalidate
```

Manually invalidates session (logout).

#### 5. Session Statistics (HOD only)

```http
GET /api/session/stats
```

Returns session statistics (requires HOD role).

#### 6. Health Check

```http
GET /api/session/health
```

Checks session repository health:

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "sessionStore": "JDBC",
    "repository": "JdbcIndexedSessionRepository"
  }
}
```

---

## Testing

### Running Tests

```bash
# Run all tests
./mvnw test

# Run only session tests
./mvnw test -Dtest=SessionManagementIntegrationTest

# Run with specific profile
./mvnw test -Dspring.profiles.active=test
```

### Test Coverage

The `SessionManagementIntegrationTest` verifies:

1. ✅ Session persisted to database after login
2. ✅ Session ID rotates on authentication
3. ✅ Session invalidated and removed on logout
4. ✅ Session persists across multiple requests
5. ✅ Cookie has correct security attributes
6. ✅ Session attributes stored in database
7. ✅ Session repository health check
8. ✅ Unauthorized access without session
9. ✅ Expired sessions cleaned up

### Manual Testing

#### Test Session Creation

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}' \
  -c cookies.txt

# Check session info
curl -X GET http://localhost:8080/api/session/info \
  -b cookies.txt

# Check database
mysql -u root -p archive_system -e "SELECT SESSION_ID, PRINCIPAL_NAME, EXPIRY_TIME FROM SPRING_SESSION;"
```

---

## Deployment Checklist

### Pre-Deployment

- [ ] **Environment Variables**: Set production database credentials
  ```bash
  export DB_URL="jdbc:mysql://prod-db:3306/archive_system?useSSL=true"
  export DB_USERNAME="archive_user"
  export DB_PASSWORD="secure_password"
  ```

- [ ] **Profile Selection**: Enable production profile
  ```bash
  --spring.profiles.active=prod
  ```

- [ ] **Database Backup**: Backup production database before migration
  ```bash
  mysqldump -u root -p archive_system > backup_$(date +%Y%m%d).sql
  ```

- [ ] **Flyway Baseline**: If existing database, baseline Flyway
  ```bash
  ./mvnw flyway:baseline -Dflyway.baselineVersion=0
  ```

### Security Checklist

- [ ] **HTTPS Enabled**: Ensure application runs on HTTPS
- [ ] **Secure Cookie**: Verify `session.cookie.secure=true` in production
- [ ] **SameSite**: Set to `Strict` for maximum CSRF protection
- [ ] **Database Access**: Restrict database user permissions
  ```sql
  GRANT SELECT, INSERT, UPDATE, DELETE ON archive_system.SPRING_SESSION* TO 'archive_user'@'%';
  ```
- [ ] **Connection Pooling**: Verify HikariCP settings for production load
- [ ] **Firewall Rules**: Restrict MySQL access to application servers only

### Post-Deployment

- [ ] **Health Check**: Verify session repository is healthy
  ```bash
  curl https://your-domain.com/api/session/health
  ```

- [ ] **Monitor Sessions**: Check session table for activity
  ```sql
  SELECT COUNT(*) AS active_sessions FROM SPRING_SESSION WHERE EXPIRY_TIME > UNIX_TIMESTAMP() * 1000;
  ```

- [ ] **Test Login/Logout**: Verify session rotation works
- [ ] **Test Restart**: Restart application and verify sessions persist
- [ ] **Load Testing**: Test with expected concurrent users

---

## Monitoring & Maintenance

### Database Queries

#### Active Sessions Count

```sql
SELECT COUNT(*) AS active_sessions 
FROM SPRING_SESSION 
WHERE EXPIRY_TIME > UNIX_TIMESTAMP() * 1000;
```

#### Sessions by User

```sql
SELECT PRINCIPAL_NAME, COUNT(*) AS session_count, MAX(LAST_ACCESS_TIME) AS last_active
FROM SPRING_SESSION
WHERE EXPIRY_TIME > UNIX_TIMESTAMP() * 1000
GROUP BY PRINCIPAL_NAME
ORDER BY session_count DESC;
```

#### Expired Sessions

```sql
SELECT COUNT(*) AS expired_sessions
FROM SPRING_SESSION
WHERE EXPIRY_TIME <= UNIX_TIMESTAMP() * 1000;
```

#### Session Table Size

```sql
SELECT 
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb
FROM information_schema.TABLES
WHERE table_schema = 'archive_system'
AND table_name LIKE 'SPRING_SESSION%';
```

### Performance Tuning

#### Index Optimization

```sql
-- Check index usage
SHOW INDEX FROM SPRING_SESSION;

-- Analyze query performance
EXPLAIN SELECT * FROM SPRING_SESSION WHERE SESSION_ID = 'test-id';
```

#### Connection Pool Tuning

Adjust based on load:

```properties
# For high-traffic production
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
```

### Cleanup Maintenance

#### Manual Cleanup (if needed)

```sql
-- Delete expired sessions
DELETE FROM SPRING_SESSION 
WHERE EXPIRY_TIME <= UNIX_TIMESTAMP() * 1000;

-- Delete old sessions (older than 7 days)
DELETE FROM SPRING_SESSION
WHERE EXPIRY_TIME < (UNIX_TIMESTAMP() - 604800) * 1000;
```

#### Verify Cleanup Job

Check logs for cleanup execution:

```bash
grep "spring.session.jdbc.cleanup" logs/archive-system.log
```

---

## Troubleshooting

### Issue: Sessions Not Persisting

**Symptoms**: Sessions lost after application restart

**Solutions**:
1. Verify Flyway migration ran successfully
   ```bash
   ./mvnw flyway:info
   ```
2. Check database tables exist
   ```sql
   SHOW TABLES LIKE 'SPRING_SESSION%';
   ```
3. Verify `spring.session.store-type=jdbc` is set
4. Check logs for errors:
   ```bash
   tail -f logs/archive-system.log | grep session
   ```

### Issue: Session Fixation Not Working

**Symptoms**: Session ID doesn't change after login

**Solutions**:
1. Verify `SecurityConfig` has `sessionFixation().newSession()`
2. Check `AuthService.login()` invalidates old session
3. Enable debug logging:
   ```properties
   logging.level.org.springframework.security=DEBUG
   ```

### Issue: Cookie Not Being Set

**Symptoms**: `ARCHIVESESSION` cookie not in browser

**Solutions**:
1. Check browser console for cookie errors
2. Verify `sessionCreationPolicy` is not `STATELESS`
3. Check CORS configuration allows credentials:
   ```java
   configuration.setAllowCredentials(true);
   ```
4. For local dev, ensure `session.cookie.secure=false`

### Issue: Sessions Expiring Too Quickly

**Symptoms**: Users logged out unexpectedly

**Solutions**:
1. Increase timeout:
   ```properties
   spring.session.timeout=60m
   ```
2. Check for aggressive cleanup:
   ```properties
   spring.session.jdbc.cleanup-cron=0 */30 * * * *
   ```
3. Monitor last access time:
   ```sql
   SELECT SESSION_ID, FROM_UNIXTIME(LAST_ACCESS_TIME/1000) 
   FROM SPRING_SESSION;
   ```

### Issue: Database Connection Pool Exhausted

**Symptoms**: "Connection pool exhausted" errors

**Solutions**:
1. Increase pool size:
   ```properties
   spring.datasource.hikari.maximum-pool-size=20
   ```
2. Check for connection leaks in code
3. Monitor connection usage:
   ```sql
   SHOW PROCESSLIST;
   ```

### Issue: High Database Load

**Symptoms**: Slow session operations

**Solutions**:
1. Verify indexes are present:
   ```sql
   SHOW INDEX FROM SPRING_SESSION;
   ```
2. Analyze slow queries:
   ```sql
   SET GLOBAL slow_query_log = 'ON';
   ```
3. Consider session timeout reduction
4. Implement session caching (future enhancement)

---

## Performance Benchmarks

### Expected Performance

- **Session Creation**: < 50ms
- **Session Lookup**: < 10ms (with indexes)
- **Session Update**: < 30ms
- **Cleanup Job**: < 5 seconds (for 10,000 expired sessions)

### Load Testing

Use Apache JMeter or similar tool:

```bash
# Example: 100 concurrent users, 1000 requests
jmeter -n -t session_load_test.jmx -l results.jtl
```

---

## Future Enhancements

### Potential Improvements

1. **Redis Cache**: Add Redis for faster session access
   ```xml
   <dependency>
       <groupId>org.springframework.session</groupId>
       <artifactId>spring-session-data-redis</artifactId>
   </dependency>
   ```

2. **Session Analytics**: Track session usage patterns
3. **Concurrent Session Control**: Limit sessions per user
4. **Remember Me**: Persistent login tokens
5. **Session Replication**: Active-active clustering

---

## References

- [Spring Session Documentation](https://docs.spring.io/spring-session/reference/)
- [Spring Security Session Management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)
- [OWASP Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)
- [Flyway Documentation](https://flywaydb.org/documentation/)

---

## Support

For issues or questions:

1. Check logs: `logs/archive-system.log`
2. Run health check: `GET /api/session/health`
3. Check database: `SELECT * FROM SPRING_SESSION LIMIT 10;`
4. Contact development team

---

**Last Updated**: November 14, 2025  
**Version**: 1.0.0  
**Author**: Archive System Team
