# Professor Dashboard Deployment Checklist

## Overview

This checklist ensures a smooth deployment of the Professor Dashboard feature to production. Follow each step carefully and mark items as complete.

**Deployment Date**: _______________  
**Deployed By**: _______________  
**Environment**: ☐ Staging  ☐ Production

---

## Pre-Deployment Checklist

### 1. Database Preparation

#### 1.1 Database Migrations

- [ ] Review all database migration scripts
- [ ] Test migrations on a copy of production database
- [ ] Verify all tables exist:
  - [ ] `course_assignments`
  - [ ] `document_submissions`
  - [ ] `uploaded_files`
  - [ ] `required_document_types`
  - [ ] `academic_years`
  - [ ] `semesters`
- [ ] Verify all indexes are created (see list below)
- [ ] Verify foreign key constraints are in place
- [ ] Backup production database before migration

**Required Indexes**:
```sql
-- Course assignments
CREATE INDEX IF NOT EXISTS idx_course_assignments_professor ON course_assignments(professor_id);
CREATE INDEX IF NOT EXISTS idx_course_assignments_semester ON course_assignments(semester_id);
CREATE INDEX IF NOT EXISTS idx_course_assignments_course ON course_assignments(course_id);
CREATE INDEX IF NOT EXISTS idx_course_assignments_active ON course_assignments(is_active);

-- Document submissions
CREATE INDEX IF NOT EXISTS idx_document_submissions_course_assignment ON document_submissions(course_assignment_id);
CREATE INDEX IF NOT EXISTS idx_document_submissions_professor ON document_submissions(professor_id);
CREATE INDEX IF NOT EXISTS idx_document_submissions_type ON document_submissions(document_type);

-- Uploaded files
CREATE INDEX IF NOT EXISTS idx_uploaded_files_submission ON uploaded_files(document_submission_id);
```

#### 1.2 Data Integrity

- [ ] Verify all professors have department assignments
- [ ] Verify all courses have departments
- [ ] Verify academic years and semesters are configured
- [ ] Check for orphaned records
- [ ] Verify data consistency between related tables



### 2. Environment Configuration

#### 2.1 Application Properties

- [ ] Review `application.properties` for production settings
- [ ] Verify database connection string
- [ ] Set appropriate session timeout (recommended: 30m)
- [ ] Configure file upload limits:
  - [ ] `spring.servlet.multipart.max-file-size=50MB`
  - [ ] `spring.servlet.multipart.max-request-size=100MB`
- [ ] Set upload directory path: `app.upload.dir=/var/app/uploads`
- [ ] Configure logging level (INFO for production)
- [ ] Set logging file path
- [ ] Verify CORS configuration (if applicable)
- [ ] Enable HTTPS/SSL configuration
- [ ] Set secure session cookie flags

**Example Production Configuration**:
```properties
# Server
server.port=8080
server.servlet.session.timeout=30m
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true

# Database
spring.datasource.url=jdbc:postgresql://prod-db-server:5432/archive_system
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# File Upload
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=100MB
app.upload.dir=/var/app/uploads

# Logging
logging.level.root=WARN
logging.level.com.alqude.edu.ArchiveSystem=INFO
logging.file.name=/var/log/archive-system/application.log
```

#### 2.2 File Storage

- [ ] Create upload directory: `/var/app/uploads`
- [ ] Set appropriate permissions (755 for directories, 644 for files)
- [ ] Verify disk space available (minimum 50GB recommended)
- [ ] Set up backup strategy for uploaded files
- [ ] Configure file retention policy
- [ ] Test write permissions with application user

**Commands**:
```bash
# Create directory
sudo mkdir -p /var/app/uploads

# Set ownership
sudo chown app-user:app-group /var/app/uploads

# Set permissions
sudo chmod 755 /var/app/uploads

# Verify
ls -la /var/app/
```

#### 2.3 Security Configuration

- [ ] Review Spring Security configuration
- [ ] Verify role-based access control (RBAC) settings
- [ ] Enable CSRF protection
- [ ] Configure secure session management
- [ ] Set up HTTPS/TLS certificates
- [ ] Configure firewall rules
- [ ] Review and update security headers
- [ ] Enable rate limiting (if applicable)

---

## Build and Testing

### 3. Build Process

#### 3.1 Code Compilation

- [ ] Pull latest code from version control
- [ ] Verify correct branch (e.g., `main`, `release/v2.0`)
- [ ] Run full build: `mvn clean package`
- [ ] Verify no compilation errors
- [ ] Check for any warnings that need attention
- [ ] Review dependency versions for security vulnerabilities

#### 3.2 Unit Tests

- [ ] Run all unit tests: `mvn test`
- [ ] Verify all tests pass
- [ ] Review test coverage report
- [ ] Address any failing tests
- [ ] Check for flaky tests

#### 3.3 Integration Tests

- [ ] Run integration tests: `mvn verify`
- [ ] Test database connectivity
- [ ] Test file upload/download operations
- [ ] Test API endpoints
- [ ] Verify authentication and authorization
- [ ] Test error handling scenarios

---

## Staging Environment Testing

### 4. Staging Deployment

#### 4.1 Deploy to Staging

- [ ] Deploy application to staging environment
- [ ] Verify application starts successfully
- [ ] Check application logs for errors
- [ ] Verify database connection
- [ ] Test file upload directory access

#### 4.2 Functional Testing

**Authentication & Authorization**:
- [ ] Professor can log in successfully
- [ ] Non-professor users are denied access
- [ ] Session timeout works correctly
- [ ] Logout clears session

**Academic Year & Semester Selection**:
- [ ] Academic years load correctly
- [ ] Semesters load for selected year
- [ ] Active year is auto-selected
- [ ] Data refreshes when semester changes

**My Courses Tab**:
- [ ] Courses load for selected semester
- [ ] Empty state displays when no courses
- [ ] Course cards show correct information
- [ ] Document statuses are accurate (NOT_UPLOADED, UPLOADED, OVERDUE)
- [ ] Deadlines display correctly

**File Upload**:
- [ ] Upload modal opens correctly
- [ ] File selection works (click and drag-drop)
- [ ] File validation works:
  - [ ] PDF files accepted
  - [ ] ZIP files accepted
  - [ ] Other file types rejected
  - [ ] File count limit enforced
  - [ ] File size limit enforced
- [ ] Progress bar updates during upload
- [ ] Success message displays
- [ ] Files stored in correct location
- [ ] Database records created correctly
- [ ] Course list refreshes after upload

**File Replacement**:
- [ ] Replace button shows for uploaded documents
- [ ] Replace modal opens with current submission info
- [ ] Old files are deleted
- [ ] New files are stored
- [ ] Submission timestamp updates
- [ ] Late submission flag set if past deadline

**Dashboard Tab**:
- [ ] Overview statistics load correctly
- [ ] Counts match actual data
- [ ] Upcoming deadlines display
- [ ] Summary text is accurate

**File Explorer Tab**:
- [ ] Root node loads for selected semester
- [ ] Folder navigation works
- [ ] Own folders show write indicator
- [ ] Department folders show as read-only
- [ ] Other department folders not visible
- [ ] Breadcrumbs update correctly
- [ ] File download works
- [ ] File metadata displays correctly

**Notifications**:
- [ ] Notification badge shows unseen count
- [ ] Dropdown opens on click
- [ ] Notifications list displays
- [ ] Mark as seen works
- [ ] Badge updates when marked as seen
- [ ] Polling works (30 second interval)

#### 4.3 Performance Testing

- [ ] Page load time < 2 seconds
- [ ] API response time < 500ms for most endpoints
- [ ] File upload completes in reasonable time
- [ ] No N+1 query problems
- [ ] Database query performance acceptable
- [ ] Memory usage within limits
- [ ] CPU usage within limits

#### 4.4 Security Testing

- [ ] SQL injection attempts blocked
- [ ] XSS attempts blocked
- [ ] CSRF protection working
- [ ] Path traversal attempts blocked
- [ ] Unauthorized access attempts blocked
- [ ] File type validation working
- [ ] File size validation working
- [ ] Session security working

#### 4.5 Browser Compatibility

- [ ] Chrome (latest version)
- [ ] Firefox (latest version)
- [ ] Edge (latest version)
- [ ] Safari (latest version)
- [ ] Mobile browsers (iOS Safari, Chrome Mobile)

#### 4.6 Error Handling

- [ ] 400 errors display user-friendly messages
- [ ] 401 errors redirect to login
- [ ] 403 errors display access denied message
- [ ] 404 errors display not found message
- [ ] 500 errors display generic error message
- [ ] Network errors handled gracefully
- [ ] Validation errors display inline

---

## Production Deployment

### 5. Pre-Deployment Steps

#### 5.1 Communication

- [ ] Notify stakeholders of deployment schedule
- [ ] Send maintenance window notification to users
- [ ] Prepare rollback plan
- [ ] Assign deployment team roles
- [ ] Schedule deployment during low-usage period

#### 5.2 Backup

- [ ] Backup production database
- [ ] Backup current application version
- [ ] Backup uploaded files
- [ ] Verify backups are restorable
- [ ] Document backup locations

#### 5.3 Monitoring Setup

- [ ] Set up application monitoring (e.g., Prometheus, New Relic)
- [ ] Configure log aggregation (e.g., ELK, Splunk)
- [ ] Set up alerts for:
  - [ ] High error rates
  - [ ] Slow response times
  - [ ] High memory usage
  - [ ] High CPU usage
  - [ ] Disk space low
  - [ ] Database connection issues
- [ ] Test alert notifications

### 6. Deployment Execution

#### 6.1 Deploy Application

- [ ] Stop current application (if not using rolling deployment)
- [ ] Deploy new application version
- [ ] Start application
- [ ] Verify application starts successfully
- [ ] Check application logs for errors
- [ ] Verify no startup exceptions

#### 6.2 Database Migration

- [ ] Run database migration scripts
- [ ] Verify migrations completed successfully
- [ ] Check for any migration errors
- [ ] Verify data integrity after migration

#### 6.3 Smoke Tests

- [ ] Application health check endpoint responds
- [ ] Login page loads
- [ ] Professor can log in
- [ ] Dashboard loads
- [ ] API endpoints respond
- [ ] File upload works
- [ ] File download works
- [ ] Database queries execute

---

## Post-Deployment

### 7. Verification

#### 7.1 Functional Verification

- [ ] Perform critical path testing:
  - [ ] Login → View Courses → Upload File → Verify Upload
  - [ ] Login → File Explorer → Navigate → Download File
  - [ ] Login → Dashboard → View Statistics
- [ ] Test with real user accounts
- [ ] Verify data consistency
- [ ] Check file storage locations

#### 7.2 Performance Monitoring

- [ ] Monitor application logs for errors
- [ ] Check response times
- [ ] Monitor database performance
- [ ] Check memory usage
- [ ] Check CPU usage
- [ ] Monitor disk space

#### 7.3 User Acceptance

- [ ] Notify users that system is available
- [ ] Provide user guide link
- [ ] Monitor for user-reported issues
- [ ] Collect initial feedback

### 8. Documentation

#### 8.1 Deployment Documentation

- [ ] Document deployment date and time
- [ ] Document deployed version/commit hash
- [ ] Document any issues encountered
- [ ] Document configuration changes
- [ ] Update deployment history

#### 8.2 User Documentation

- [ ] Verify user guide is accessible
- [ ] Update any outdated screenshots
- [ ] Publish release notes
- [ ] Update FAQ if needed

#### 8.3 Developer Documentation

- [ ] Update API documentation
- [ ] Update developer guide
- [ ] Document any new environment variables
- [ ] Update README if needed

---

## Rollback Plan

### 9. Rollback Procedure

**If deployment fails or critical issues are discovered:**

#### 9.1 Immediate Actions

- [ ] Stop new application
- [ ] Restore previous application version
- [ ] Rollback database migrations (if necessary)
- [ ] Restore backed-up files (if necessary)
- [ ] Verify rollback successful
- [ ] Notify stakeholders

#### 9.2 Post-Rollback

- [ ] Investigate root cause of failure
- [ ] Document issues encountered
- [ ] Fix issues in development environment
- [ ] Re-test in staging
- [ ] Schedule new deployment

---

## Monitoring and Maintenance

### 10. Ongoing Monitoring

#### 10.1 Daily Checks (First Week)

- [ ] Review application logs
- [ ] Check error rates
- [ ] Monitor response times
- [ ] Review user feedback
- [ ] Check disk space usage

#### 10.2 Weekly Checks

- [ ] Review performance metrics
- [ ] Analyze usage patterns
- [ ] Check for any security issues
- [ ] Review and address user feedback
- [ ] Update documentation as needed

#### 10.3 Monthly Checks

- [ ] Review and optimize database queries
- [ ] Clean up old log files
- [ ] Review and update dependencies
- [ ] Perform security audit
- [ ] Review backup strategy

---

## Troubleshooting Guide

### Common Issues and Solutions

#### Issue: Application won't start

**Check**:
- [ ] Database connection string correct
- [ ] Database server running
- [ ] Upload directory exists and has permissions
- [ ] Port 8080 not already in use
- [ ] Java version correct (17+)

**Solution**:
```bash
# Check logs
tail -f /var/log/archive-system/application.log

# Check port
netstat -tuln | grep 8080

# Check Java version
java -version
```

#### Issue: File uploads failing

**Check**:
- [ ] Upload directory exists
- [ ] Write permissions on upload directory
- [ ] Disk space available
- [ ] File size within limits
- [ ] File type allowed

**Solution**:
```bash
# Check directory
ls -la /var/app/uploads

# Check disk space
df -h

# Check permissions
sudo chmod 755 /var/app/uploads
sudo chown app-user:app-group /var/app/uploads
```

#### Issue: Slow performance

**Check**:
- [ ] Database indexes created
- [ ] N+1 query problems
- [ ] Memory usage
- [ ] CPU usage
- [ ] Network latency

**Solution**:
- Review slow query log
- Add missing indexes
- Optimize queries with JOIN FETCH
- Increase memory allocation if needed

---

## Sign-Off

### Deployment Team

**Deployment Lead**: _______________  
**Signature**: _______________  
**Date**: _______________

**Database Administrator**: _______________  
**Signature**: _______________  
**Date**: _______________

**QA Lead**: _______________  
**Signature**: _______________  
**Date**: _______________

**System Administrator**: _______________  
**Signature**: _______________  
**Date**: _______________

### Approval

**Project Manager**: _______________  
**Signature**: _______________  
**Date**: _______________

**Technical Lead**: _______________  
**Signature**: _______________  
**Date**: _______________

---

## Appendix

### A. Environment Variables

```bash
# Database
export DB_USERNAME=archive_user
export DB_PASSWORD=secure_password
export DB_HOST=prod-db-server
export DB_PORT=5432
export DB_NAME=archive_system

# Application
export APP_UPLOAD_DIR=/var/app/uploads
export APP_LOG_DIR=/var/log/archive-system

# Security
export SESSION_TIMEOUT=30m
export CSRF_ENABLED=true
```

### B. Useful Commands

```bash
# Start application
java -jar archive-system.jar

# Check application status
systemctl status archive-system

# View logs
tail -f /var/log/archive-system/application.log

# Check disk space
df -h

# Check memory usage
free -h

# Check CPU usage
top

# Database backup
pg_dump -U archive_user archive_system > backup_$(date +%Y%m%d).sql

# File backup
tar -czf uploads_backup_$(date +%Y%m%d).tar.gz /var/app/uploads
```

### C. Contact Information

**Technical Support**: support@archivesystem.edu  
**Emergency Contact**: +1-555-123-4567  
**On-Call Engineer**: oncall@archivesystem.edu

---

**Document Version**: 1.0  
**Last Updated**: November 19, 2024  
**Next Review Date**: December 19, 2024
