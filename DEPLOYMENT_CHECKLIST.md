# Database-Backed Session Management - Quick Deployment Checklist

## ⚡ Quick Start

### Prerequisites
- ✅ MySQL 8.0+ running
- ✅ Java 17+ installed
- ✅ Maven 3.6+ installed
- ✅ Database `archive_system` created

---

## 🚀 Deployment Steps

### 1. Update Dependencies

```bash
# Build project with new dependencies
./mvnw clean install
```

**What's Added:**
- `spring-session-jdbc`
- `flyway-core`
- `flyway-mysql`

---

### 2. Configure Environment

#### Development (Local)

Use default `application.properties` or create `application-dev.properties`:

```bash
# Run with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Production

Set environment variables:

```bash
export DB_URL="jdbc:mysql://prod-server:3306/archive_system?useSSL=true&serverTimezone=UTC"
export DB_USERNAME="archive_user"
export DB_PASSWORD="your_secure_password"
export SPRING_PROFILES_ACTIVE=prod
```

Or use `application-prod.properties`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

---

### 3. Run Database Migration

Flyway will automatically create session tables on first startup.

**Verify Migration:**

```sql
-- Check tables exist
SHOW TABLES LIKE 'SPRING_SESSION%';

-- Should show:
-- SPRING_SESSION
-- SPRING_SESSION_ATTRIBUTES

-- Check indexes
SHOW INDEX FROM SPRING_SESSION;
```

**Manual Migration (if needed):**

```bash
./mvnw flyway:migrate
```

---

### 4. Start Application

```bash
# Development
./mvnw spring-boot:run

# Production (JAR)
java -jar target/ArchiveSystem-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

### 5. Verify Installation

#### A. Health Check

```bash
curl http://localhost:8080/api/session/health
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "sessionStore": "JDBC"
  }
}
```

#### B. Test Login

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"your_email@alquds.edu","password":"your_password"}' \
  -c cookies.txt -v

# Check session
curl http://localhost:8080/api/session/info -b cookies.txt
```

**Expected:** Cookie `ARCHIVESESSION` set in response.

#### C. Check Database

```sql
-- View active sessions
SELECT SESSION_ID, PRINCIPAL_NAME, 
       FROM_UNIXTIME(CREATION_TIME/1000) AS created,
       FROM_UNIXTIME(LAST_ACCESS_TIME/1000) AS last_access
FROM SPRING_SESSION
WHERE EXPIRY_TIME > UNIX_TIMESTAMP() * 1000;
```

---

## 🔒 Security Checklist

### Development Environment

- [ ] `session.cookie.secure=false` (no HTTPS needed)
- [ ] `session.cookie.same-site=Lax`
- [ ] Database on `localhost`
- [ ] Debug logging enabled

### Production Environment

- [ ] **HTTPS Enabled** - Application runs on HTTPS
- [ ] `session.cookie.secure=true` - Cookies only over HTTPS
- [ ] `session.cookie.same-site=Strict` - Maximum CSRF protection
- [ ] **Database Secured:**
  - [ ] Strong password set
  - [ ] Firewall rules restrict MySQL access
  - [ ] Database user has minimal permissions:
    ```sql
    CREATE USER 'archive_user'@'app-server-ip' IDENTIFIED BY 'strong_password';
    GRANT SELECT, INSERT, UPDATE, DELETE ON archive_system.SPRING_SESSION* TO 'archive_user'@'app-server-ip';
    FLUSH PRIVILEGES;
    ```
- [ ] **TLS/SSL** enabled for database connection:
  ```properties
  spring.datasource.url=jdbc:mysql://server:3306/archive_system?useSSL=true&requireSSL=true
  ```
- [ ] **Connection Pool** tuned for production load:
  ```properties
  spring.datasource.hikari.maximum-pool-size=20
  spring.datasource.hikari.minimum-idle=10
  ```
- [ ] **Session Timeout** appropriate (30-60 minutes)
- [ ] **Backup Strategy** in place for session tables

---

## 🧪 Testing Checklist

### Run Integration Tests

```bash
# All tests
./mvnw test

# Session-specific tests
./mvnw test -Dtest=SessionManagementIntegrationTest
```

**All 9 tests should pass:**

1. ✅ Session persisted to database
2. ✅ Session ID rotates on login
3. ✅ Session invalidated on logout
4. ✅ Session persists across requests
5. ✅ Cookie has secure attributes
6. ✅ Attributes stored in database
7. ✅ Health check works
8. ✅ Unauthorized access blocked
9. ✅ Expired sessions cleaned up

### Manual Tests

- [ ] Login creates session in database
- [ ] Session cookie set with correct attributes
- [ ] Session survives app restart
- [ ] Logout invalidates session
- [ ] Multiple logins create separate sessions
- [ ] Session expires after timeout period

---

## 📊 Monitoring Setup

### Database Monitoring

Add to cron (Linux) or Task Scheduler (Windows):

```bash
# Monitor active sessions (every 5 minutes)
*/5 * * * * mysql -u root -p'password' -e "SELECT COUNT(*) FROM archive_system.SPRING_SESSION WHERE EXPIRY_TIME > UNIX_TIMESTAMP() * 1000;" >> /var/log/session-count.log
```

### Application Logs

Monitor for session-related errors:

```bash
# Tail logs
tail -f logs/archive-system.log | grep -i "session"

# Check for errors
grep -i "error" logs/archive-system.log | grep -i "session"
```

### Alerts Setup

Set up alerts for:

- High session count (> 1000 active)
- Low database connection pool
- Session creation failures
- Cleanup job failures

---

## 🛠️ Troubleshooting Quick Fixes

### Issue: Application won't start

```bash
# Check Flyway status
./mvnw flyway:info

# Baseline if needed
./mvnw flyway:baseline -Dflyway.baselineVersion=0

# Try again
./mvnw spring-boot:run
```

### Issue: Sessions not persisting

```sql
-- Verify tables exist
SHOW TABLES LIKE 'SPRING_SESSION%';

-- If missing, run migration manually
SOURCE src/main/resources/db/migration/V1__Create_Spring_Session_Tables.sql;
```

### Issue: Cookie not being set

Check `SecurityConfig.java`:

```java
// Should be IF_REQUIRED, not STATELESS
.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
```

### Issue: Session expires immediately

Check application.properties:

```properties
# Should be 30m or 60m, not seconds
spring.session.timeout=30m  
```

---

## 📦 Rollback Plan

If issues occur, rollback:

### 1. Stop Application

```bash
# Find and kill process
ps aux | grep ArchiveSystem
kill <PID>
```

### 2. Restore Database

```bash
# Restore from backup
mysql -u root -p archive_system < backup_20251114.sql
```

### 3. Revert Code

```bash
# Git revert
git revert HEAD
git push

# Or checkout previous version
git checkout previous-tag
```

### 4. Remove Session Tables (if needed)

```sql
DROP TABLE IF EXISTS SPRING_SESSION_ATTRIBUTES;
DROP TABLE IF EXISTS SPRING_SESSION;
```

---

## 🎯 Success Criteria

✅ **Deployment Successful If:**

1. Application starts without errors
2. `/api/session/health` returns `UP`
3. Login creates session in database
4. Session persists after app restart
5. Logout removes session from database
6. Integration tests pass
7. No database connection errors in logs

---

## 📞 Support

**Common Issues:** See `SESSION_MANAGEMENT_GUIDE.md` Troubleshooting section

**Database Issues:**
```bash
# Check MySQL status
systemctl status mysql  # Linux
# or
net start mysql  # Windows

# Check connections
mysql -u root -p -e "SHOW PROCESSLIST;"
```

**Application Issues:**
```bash
# Check logs
tail -100 logs/archive-system.log

# Check Java process
jps -v

# Check port availability
netstat -an | grep 8080
```

---

## ✅ Final Verification

Before considering deployment complete:

```bash
# 1. Health check
curl http://localhost:8080/api/session/health

# 2. Login test
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@alquds.edu","password":"password"}' \
  -c cookies.txt

# 3. Session info
curl http://localhost:8080/api/session/info -b cookies.txt

# 4. Database check
mysql -u root -p archive_system -e "SELECT COUNT(*) FROM SPRING_SESSION;"

# 5. Logout test
curl -X POST http://localhost:8080/api/auth/logout -b cookies.txt
```

**All commands should succeed!**

---

## 📚 Next Steps

After successful deployment:

1. Monitor logs for 24 hours
2. Check session table growth
3. Verify cleanup job runs every 15 minutes
4. Test with real users
5. Tune connection pool if needed
6. Set up automated backups
7. Document any production-specific configs

---

**Deployment Date**: _____________  
**Deployed By**: _____________  
**Environment**: ☐ Development  ☐ Staging  ☐ Production  
**Verified By**: _____________  

---

✨ **Deployment Complete!** Your application now has enterprise-grade session management with database persistence, security features, and horizontal scaling support.
