# Deanship Dashboard Rollback Plan

## Overview

This document provides a detailed rollback plan for the Deanship Dashboard multi-page refactor. If critical issues are discovered after deployment, this plan ensures the system can be quickly restored to the previous single-page dashboard.

## When to Rollback

### Critical Triggers

Rollback should be **immediately** triggered if any of the following occur:

1. **Functional Failures**:
   - CRUD operations fail (cannot add/edit/delete academic years, professors, courses, or assignments)
   - Authentication/authorization failures preventing deanship users from accessing the system
   - Data corruption or loss detected
   - File explorer completely non-functional

2. **Performance Issues**:
   - Page load times exceed 5 seconds consistently
   - Performance degradation > 50% compared to old dashboard
   - Server resource exhaustion (CPU > 90%, memory issues)

3. **Security Issues**:
   - Security vulnerability discovered in new code
   - Unauthorized access to deanship functions
   - Data exposure or privacy breach

4. **User Impact**:
   - Error rate > 10% of requests
   - User complaints > 20% of active deanship users
   - Critical workflow blocked (e.g., cannot assign courses before semester start)

### Non-Critical Issues (Do Not Rollback)

The following issues should be fixed forward rather than rolled back:

- Minor UI inconsistencies or styling issues
- Non-critical features not working (e.g., search on one page)
- Performance issues affecting < 10% of users
- Browser-specific issues in non-primary browsers
- Minor usability complaints

## Rollback Options

### Option 1: Quick Rollback (Redirect to Old Dashboard)

**Time to Complete**: 10-15 minutes  
**Downtime**: < 5 minutes  
**Risk Level**: Low  
**Recommended For**: Most situations

This is the fastest rollback method and should be attempted first.

#### Steps

1. **Update Login Redirect**

   Edit `src/main/java/com/alqude/edu/ArchiveSystem/controller/AuthController.java`:

   ```java
   // Find the login success redirect (around line 50-60)
   // Change from:
   if (userInfo.getRole().equals("ROLE_DEANSHIP")) {
       return "redirect:/deanship/dashboard";
   }
   
   // To:
   if (userInfo.getRole().equals("ROLE_DEANSHIP")) {
       return "redirect:/deanship-dashboard.html";
   }
   ```

2. **Update View Controller to Redirect All Routes**

   Edit `src/main/java/com/alqude/edu/ArchiveSystem/controller/DeanshipViewController.java`:

   ```java
   // Add this method at the top of the class
   @GetMapping("/**")
   public String redirectToOldDashboard() {
       log.info("Redirecting to old deanship dashboard");
       return "redirect:/deanship-dashboard.html";
   }
   
   // Comment out or remove all other @GetMapping methods
   ```

3. **Rebuild Application**

   ```bash
   # Clean and build
   mvn clean package -DskipTests
   ```

4. **Stop Application**

   ```bash
   # Linux/Mac
   systemctl stop archive-system
   
   # Or if running manually
   # Find process: ps aux | grep ArchiveSystem
   # Kill process: kill -9 <PID>
   
   # Windows
   # Stop the service or close the terminal running the application
   ```

5. **Deploy Updated JAR**

   ```bash
   # Backup current JAR
   cp /path/to/deployment/ArchiveSystem-0.0.1-SNAPSHOT.jar /path/to/backup/ArchiveSystem-rollback-$(date +%Y%m%d-%H%M%S).jar
   
   # Copy new JAR
   cp target/ArchiveSystem-0.0.1-SNAPSHOT.jar /path/to/deployment/
   ```

6. **Start Application**

   ```bash
   # Linux/Mac
   systemctl start archive-system
   
   # Or if running manually
   java -jar /path/to/deployment/ArchiveSystem-0.0.1-SNAPSHOT.jar
   
   # Windows
   # Start the service or run: java -jar ArchiveSystem-0.0.1-SNAPSHOT.jar
   ```

7. **Verify Rollback**

   - Open browser and navigate to login page
   - Login as deanship user
   - Verify redirect to `/deanship-dashboard.html`
   - Verify old dashboard loads and functions correctly
   - Test one CRUD operation (e.g., view professors)
   - Test file explorer functionality

8. **Monitor**

   - Watch application logs for errors
   - Monitor user feedback
   - Check error rates return to normal

#### Verification Checklist

- [ ] Login redirects to old dashboard
- [ ] Old dashboard loads without errors
- [ ] All tabs visible (Academic Years, Professors, Courses, etc.)
- [ ] Can switch between tabs
- [ ] Can view data in tables
- [ ] Can perform CRUD operations
- [ ] File explorer works
- [ ] Logout works

### Option 2: Full Rollback (Restore Previous Version)

**Time to Complete**: 15-20 minutes  
**Downtime**: 5-10 minutes  
**Risk Level**: Low  
**Recommended For**: If Option 1 fails or more extensive rollback needed

This option restores the entire application to the previous version before the refactor was deployed.

#### Prerequisites

- Backup of previous deployment exists
- Backup location known and accessible
- Database backup exists (if schema changes were made - not applicable for this refactor)

#### Steps

1. **Stop Application**

   ```bash
   # Linux/Mac
   systemctl stop archive-system
   
   # Windows
   # Stop the service or close the terminal
   ```

2. **Backup Current Version** (for investigation)

   ```bash
   cp /path/to/deployment/ArchiveSystem-0.0.1-SNAPSHOT.jar /path/to/backup/ArchiveSystem-failed-$(date +%Y%m%d-%H%M%S).jar
   ```

3. **Restore Previous JAR**

   ```bash
   # Find the backup (should be timestamped)
   ls -lt /path/to/backup/
   
   # Copy previous version
   cp /path/to/backup/deployment-YYYYMMDD-HHMMSS/ArchiveSystem-0.0.1-SNAPSHOT.jar /path/to/deployment/
   ```

4. **Restore Database** (if needed - NOT required for this refactor)

   ```bash
   # Only if database schema changes were made
   # mysql -u username -p database_name < backup-YYYYMMDD-HHMMSS.sql
   ```

5. **Start Application**

   ```bash
   # Linux/Mac
   systemctl start archive-system
   
   # Windows
   # Start the service or run the JAR
   ```

6. **Verify Rollback**

   - Login as deanship user
   - Verify old dashboard loads
   - Test all major functions
   - Check application logs

7. **Monitor**

   - Watch logs for 30 minutes
   - Monitor user feedback
   - Verify error rates are normal

#### Verification Checklist

- [ ] Application starts without errors
- [ ] Login works correctly
- [ ] Old dashboard loads
- [ ] All functionality works
- [ ] No database errors
- [ ] Performance is normal

### Option 3: Emergency Rollback (Manual File Replacement)

**Time to Complete**: 5-10 minutes  
**Downtime**: 2-5 minutes  
**Risk Level**: Medium  
**Recommended For**: Emergency situations when Options 1 and 2 fail

This option manually replaces the new files with old files without rebuilding.

#### Steps

1. **Stop Application**

   ```bash
   systemctl stop archive-system
   ```

2. **Replace HTML Files**

   ```bash
   # Navigate to static directory
   cd /path/to/deployment/static/
   
   # Remove new deanship directory
   rm -rf deanship/
   
   # Verify old dashboard file exists
   ls -la deanship-dashboard.html
   ```

3. **Replace JavaScript Files**

   ```bash
   cd /path/to/deployment/static/js/
   
   # Remove new JavaScript modules
   rm -f deanship-common.js dashboard.js academic-years.js professors.js courses.js course-assignments.js reports.js file-explorer-page.js
   
   # Verify old deanship.js exists
   ls -la deanship.js
   ```

4. **Replace CSS Files**

   ```bash
   cd /path/to/deployment/static/css/
   
   # Remove new CSS file
   rm -f deanship-layout.css
   ```

5. **Update Login Redirect** (if possible)

   If you can quickly edit the JAR or have access to source:
   - Change login redirect to `/deanship-dashboard.html`
   - Otherwise, proceed without this step

6. **Start Application**

   ```bash
   systemctl start archive-system
   ```

7. **Verify**

   - Test old dashboard loads
   - If login still redirects to new routes, use Option 1 or 2

## Post-Rollback Actions

### Immediate Actions (Within 1 Hour)

1. **Notify Stakeholders**
   - Send email to deanship users explaining rollback
   - Notify development team
   - Update status page if applicable

2. **Document Issues**
   - Record all errors and issues that triggered rollback
   - Capture screenshots of errors
   - Save application logs
   - Save browser console logs
   - Document user complaints

3. **Verify System Stability**
   - Monitor application for 1 hour
   - Check error rates
   - Verify all functions work
   - Monitor performance metrics

### Short-Term Actions (Within 24 Hours)

1. **Root Cause Analysis**
   - Review all captured errors and logs
   - Identify root cause of failure
   - Determine if issue was in code, configuration, or deployment
   - Document findings

2. **Create Fix Plan**
   - Identify specific fixes needed
   - Estimate time to fix
   - Determine if fixes can be made incrementally
   - Plan testing strategy

3. **Communicate Timeline**
   - Inform stakeholders of issue and fix timeline
   - Set expectations for when new version will be redeployed
   - Provide workarounds if needed

### Long-Term Actions (Within 1 Week)

1. **Fix Issues**
   - Implement fixes in development environment
   - Add tests to prevent regression
   - Perform thorough testing

2. **Test Thoroughly**
   - Unit tests
   - Integration tests
   - Manual testing on all pages
   - Performance testing
   - Browser compatibility testing
   - User acceptance testing

3. **Plan Redeployment**
   - Schedule deployment during low-usage period
   - Prepare rollback plan (this document)
   - Notify stakeholders of deployment
   - Have team available during deployment

4. **Update Documentation**
   - Document what went wrong
   - Update deployment procedures
   - Add additional verification steps
   - Update rollback plan with lessons learned

## Rollback Testing

### Pre-Production Testing

Before deploying to production, the rollback procedure should be tested in a staging environment:

1. **Deploy New Version to Staging**
   - Deploy multi-page refactor
   - Verify it works

2. **Test Option 1 Rollback**
   - Perform Option 1 rollback steps
   - Verify old dashboard loads
   - Time the process
   - Document any issues

3. **Redeploy New Version**
   - Deploy multi-page refactor again
   - Verify it works

4. **Test Option 2 Rollback**
   - Perform Option 2 rollback steps
   - Verify old dashboard loads
   - Time the process
   - Document any issues

5. **Document Results**
   - Record time for each option
   - Note any issues encountered
   - Update rollback procedures if needed

### Rollback Drill

Perform a rollback drill with the team:

1. **Simulate Failure Scenario**
   - Introduce a simulated critical issue
   - Trigger rollback decision

2. **Execute Rollback**
   - Team member executes rollback following this plan
   - Other team members observe and time

3. **Verify Success**
   - Confirm old dashboard works
   - Check all functions

4. **Debrief**
   - Discuss what went well
   - Identify improvements to process
   - Update documentation

## Communication Templates

### Rollback Notification Email

```
Subject: Deanship Dashboard - Temporary Rollback to Previous Version

Dear Deanship Users,

We have temporarily rolled back the Deanship Dashboard to the previous version due to [brief description of issue].

What this means:
- You will see the previous single-page dashboard when you log in
- All functionality remains available
- No data has been lost
- You may need to clear your browser cache if you experience issues

We are working to resolve the issue and will redeploy the updated dashboard once it has been thoroughly tested.

Expected timeline: [provide estimate]

If you experience any issues, please contact [support contact].

Thank you for your patience.

[Your Name]
[Your Title]
```

### Status Update Email

```
Subject: Deanship Dashboard - Rollback Status Update

Dear Deanship Users,

Update on the Deanship Dashboard rollback:

Current Status: [Rolled back / Being fixed / Testing in progress]

What we found: [Brief explanation of issue]

What we're doing: [Brief explanation of fix]

Next steps:
1. [Step 1]
2. [Step 2]
3. [Step 3]

Expected redeployment: [Date and time]

The current dashboard (previous version) continues to work normally.

Thank you for your patience.

[Your Name]
[Your Title]
```

## Rollback Decision Matrix

Use this matrix to decide whether to rollback:

| Issue Severity | User Impact | Workaround Available | Decision |
|---------------|-------------|---------------------|----------|
| Critical | High (>50%) | No | **ROLLBACK IMMEDIATELY** |
| Critical | High (>50%) | Yes | **ROLLBACK** (within 1 hour) |
| Critical | Medium (20-50%) | No | **ROLLBACK** (within 2 hours) |
| Critical | Medium (20-50%) | Yes | Fix forward or rollback (team decision) |
| Critical | Low (<20%) | Yes | Fix forward |
| Major | High (>50%) | No | **ROLLBACK** (within 4 hours) |
| Major | High (>50%) | Yes | Fix forward or rollback (team decision) |
| Major | Medium (20-50%) | No | Fix forward or rollback (team decision) |
| Major | Medium (20-50%) | Yes | Fix forward |
| Major | Low (<20%) | Yes/No | Fix forward |
| Minor | Any | Yes/No | Fix forward |

**Definitions**:
- **Critical**: System unusable, data loss, security issue
- **Major**: Core functionality broken, significant performance degradation
- **Minor**: UI issues, non-critical features broken

## Rollback Checklist

Print this checklist and keep it accessible during deployment:

### Pre-Rollback
- [ ] Rollback decision made by authorized person
- [ ] Stakeholders notified of impending rollback
- [ ] Backup of current (failed) version created
- [ ] Previous version backup location confirmed
- [ ] Team members available to assist

### During Rollback
- [ ] Application stopped
- [ ] Files backed up or replaced
- [ ] Application restarted
- [ ] Basic smoke test passed
- [ ] Logs checked for errors

### Post-Rollback
- [ ] Full verification completed
- [ ] Users notified of rollback
- [ ] Issues documented
- [ ] Monitoring in place
- [ ] Root cause analysis started
- [ ] Fix plan created
- [ ] Timeline communicated

## Contact Information

### Rollback Authority

List people authorized to make rollback decision:
- [Name], [Title], [Phone], [Email]
- [Name], [Title], [Phone], [Email]

### Technical Team

List people who can execute rollback:
- [Name], [Role], [Phone], [Email]
- [Name], [Role], [Phone], [Email]

### Stakeholder Notification

List people to notify of rollback:
- [Name], [Title], [Email]
- [Name], [Title], [Email]

## Lessons Learned Template

After any rollback, document lessons learned:

**Date of Rollback**: [Date]

**Issue Description**: [What went wrong]

**Root Cause**: [Why it happened]

**Rollback Method Used**: [Option 1/2/3]

**Time to Rollback**: [Minutes]

**Issues During Rollback**: [Any problems encountered]

**What Went Well**: [Positive aspects]

**What Could Be Improved**: [Areas for improvement]

**Action Items**:
1. [Action item 1]
2. [Action item 2]
3. [Action item 3]

**Documentation Updates Needed**: [List any updates to this plan]

---

**Document Version**: 1.0  
**Last Updated**: November 2025  
**Next Review**: After first deployment or rollback event
