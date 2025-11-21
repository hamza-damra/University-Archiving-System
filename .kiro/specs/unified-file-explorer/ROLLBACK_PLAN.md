# Unified File Explorer - Rollback Plan

## Document Overview

This document provides a comprehensive rollback plan for the Unified File Explorer implementation. It includes detailed information about all changes made, backup procedures, and step-by-step rollback instructions in case of critical issues.

**Last Updated:** November 20, 2025  
**Feature:** Unified File Explorer  
**Spec Location:** `.kiro/specs/unified-file-explorer/`

---

## Table of Contents

1. [Changes Summary](#changes-summary)
2. [Backup Procedures](#backup-procedures)
3. [Rollback Procedures](#rollback-procedures)
4. [Verification Steps](#verification-steps)
5. [Emergency Contacts](#emergency-contacts)

---

## Changes Summary

### Files Modified

#### 1. **src/main/resources/static/js/file-explorer.js**
**Status:** Enhanced with role-specific configuration  
**Changes Made:**
- Added comprehensive JSDoc documentation identifying this as the master design reference
- Enhanced constructor to accept role-specific configuration options:
  - `role`: 'PROFESSOR' | 'HOD' | 'DEANSHIP'
  - `showOwnershipLabels`: boolean (Professor)
  - `showDepartmentContext`: boolean (HOD)
  - `headerMessage`: string (HOD)
  - `showProfessorLabels`: boolean (Deanship)
  - `showAllDepartments`: boolean (Deanship)
- Added `generateRoleSpecificLabels()` method for rendering role-specific badges
- Added header message support in `render()` method
- Implemented consistent empty state, loading state, and error state rendering methods
- Enhanced breadcrumb navigation with home icon and improved styling
- Added tree view expansion/collapse functionality with lazy loading
- Implemented file list rendering with folder cards and file tables

**Impact:** Core component - affects all three dashboards  
**Risk Level:** Medium (shared component)

#### 2. **src/main/resources/static/js/prof.js**
**Status:** Updated to use enhanced FileExplorer configuration  
**Changes Made:**
- Added comprehensive header documentation identifying this as the master design reference
- Updated FileExplorer instantiation with explicit role configuration:
  ```javascript
  new FileExplorer('fileExplorerContainer', {
      role: 'PROFESSOR',
      showOwnershipLabels: true,
      readOnly: false
  });
  ```
- Maintained all existing functionality (no breaking changes)
- Added code comments explaining role-specific patterns

**Impact:** Professor Dashboard only  
**Risk Level:** Low (isolated to one dashboard)

#### 3. **src/main/resources/static/js/hod.js**
**Status:** Migrated to use unified FileExplorer component  
**Changes Made:**
- Removed custom File Explorer rendering logic
- Added `initializeFileExplorer()` function with HOD configuration:
  ```javascript
  new FileExplorer('hodFileExplorer', {
      role: 'HOD',
      readOnly: true,
      showDepartmentContext: true,
      headerMessage: 'Browse department files (Read-only)'
  });
  ```
- Added `loadFileExplorerData()` function to load root node
- Added comprehensive documentation explaining HOD-specific configuration
- Maintained all existing permission checks and data filtering

**Impact:** HOD Dashboard only  
**Risk Level:** Medium (significant refactoring)

#### 4. **src/main/resources/static/js/deanship.js**
**Status:** Migrated to use unified FileExplorer component  
**Changes Made:**
- Removed custom File Explorer rendering logic
- Added `initializeFileExplorer()` function with Deanship configuration:
  ```javascript
  new FileExplorer('deanshipFileExplorer', {
      role: 'DEANSHIP',
      readOnly: true,
      showAllDepartments: true,
      showProfessorLabels: true
  });
  ```
- Added `loadFileExplorer()` function to load root node
- Added comprehensive documentation explaining Deanship-specific configuration
- Maintained all existing permission checks and cross-department access

**Impact:** Deanship Dashboard only  
**Risk Level:** Medium (significant refactoring)

#### 5. **src/main/resources/static/hod-dashboard.html**
**Status:** Updated HTML structure to match Professor Dashboard  
**Changes Made:**
- Updated File Explorer tab HTML structure to match Professor Dashboard layout
- Changed container ID to `hodFileExplorer` for consistency
- Added Academic Year and Semester selector styling matching Professor Dashboard
- Maintained all existing tabs and navigation structure

**Impact:** HOD Dashboard UI only  
**Risk Level:** Low (HTML structure changes)

#### 6. **src/main/resources/static/deanship-dashboard.html**
**Status:** Updated HTML structure to match Professor Dashboard  
**Changes Made:**
- Updated File Explorer tab HTML structure to match Professor Dashboard layout
- Changed container ID to `deanshipFileExplorer` for consistency
- Added Academic Year and Semester selector styling matching Professor Dashboard
- Maintained all existing tabs and navigation structure

**Impact:** Deanship Dashboard UI only  
**Risk Level:** Low (HTML structure changes)

### Files NOT Modified

The following files were **NOT** modified and remain unchanged:

- **Backend API endpoints** - All existing endpoints remain unchanged
- **Database schema** - No database changes required
- **Permission logic** - All permission checks remain on backend
- **Authentication** - No changes to authentication flow
- **src/main/resources/static/prof-dashboard.html** - Already using correct structure
- **src/main/resources/static/js/api.js** - No changes to API module
- **src/main/resources/static/js/ui.js** - No changes to UI utilities
- **src/main/resources/static/css/custom.css** - No CSS changes required (using Tailwind)

---

## Backup Procedures

### Pre-Deployment Backup

Before deploying the unified File Explorer, create backups of all modified files:

```powershell
# Create backup directory with timestamp
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupDir = ".kiro/specs/unified-file-explorer/backups/$timestamp"
New-Item -ItemType Directory -Path $backupDir -Force

# Backup JavaScript files
Copy-Item "src/main/resources/static/js/file-explorer.js" "$backupDir/file-explorer.js.backup"
Copy-Item "src/main/resources/static/js/prof.js" "$backupDir/prof.js.backup"
Copy-Item "src/main/resources/static/js/hod.js" "$backupDir/hod.js.backup"
Copy-Item "src/main/resources/static/js/deanship.js" "$backupDir/deanship.js.backup"

# Backup HTML files
Copy-Item "src/main/resources/static/hod-dashboard.html" "$backupDir/hod-dashboard.html.backup"
Copy-Item "src/main/resources/static/deanship-dashboard.html" "$backupDir/deanship-dashboard.html.backup"

Write-Host "Backup completed: $backupDir"
```

### Backup Verification

After creating backups, verify they exist and are readable:

```powershell
# Verify backup files exist
$backupFiles = @(
    "$backupDir/file-explorer.js.backup",
    "$backupDir/prof.js.backup",
    "$backupDir/hod.js.backup",
    "$backupDir/deanship.js.backup",
    "$backupDir/hod-dashboard.html.backup",
    "$backupDir/deanship-dashboard.html.backup"
)

foreach ($file in $backupFiles) {
    if (Test-Path $file) {
        $size = (Get-Item $file).Length
        Write-Host "✓ $file ($size bytes)"
    } else {
        Write-Host "✗ MISSING: $file" -ForegroundColor Red
    }
}
```

### Git Backup (Recommended)

If using Git version control, create a backup branch:

```bash
# Create backup branch before deployment
git checkout -b backup/unified-file-explorer-pre-deployment
git add .
git commit -m "Backup before unified file explorer deployment"
git push origin backup/unified-file-explorer-pre-deployment

# Return to main branch
git checkout main
```

---

## Rollback Procedures

### When to Rollback

Consider rolling back if you encounter:

1. **Critical Issues:**
   - File Explorer not loading in any dashboard
   - JavaScript errors preventing page load
   - Complete loss of file browsing functionality
   - Data corruption or loss

2. **Major Functional Issues:**
   - Incorrect permission enforcement (users seeing unauthorized data)
   - File upload/download failures
   - Navigation broken across all dashboards
   - Role-specific features not working

3. **Performance Issues:**
   - Severe performance degradation (>5 second load times)
   - Browser crashes or freezes
   - Memory leaks causing browser slowdown

### Rollback Decision Matrix

| Issue Severity | User Impact | Rollback Decision |
|---------------|-------------|-------------------|
| Critical | All users affected | **Immediate rollback** |
| Major | >50% users affected | **Rollback within 1 hour** |
| Moderate | 10-50% users affected | **Attempt fix first, rollback if not resolved in 2 hours** |
| Minor | <10% users affected | **Fix forward, no rollback needed** |

### Rollback Procedure - Method 1: File Restoration

**Time Required:** 5-10 minutes  
**Downtime:** Minimal (page refresh required)

```powershell
# Step 1: Navigate to backup directory
$backupDir = ".kiro/specs/unified-file-explorer/backups/[TIMESTAMP]"
cd $backupDir

# Step 2: Restore JavaScript files
Copy-Item "file-explorer.js.backup" "../../../../../../src/main/resources/static/js/file-explorer.js" -Force
Copy-Item "prof.js.backup" "../../../../../../src/main/resources/static/js/prof.js" -Force
Copy-Item "hod.js.backup" "../../../../../../src/main/resources/static/js/hod.js" -Force
Copy-Item "deanship.js.backup" "../../../../../../src/main/resources/static/js/deanship.js" -Force

# Step 3: Restore HTML files
Copy-Item "hod-dashboard.html.backup" "../../../../../../src/main/resources/static/hod-dashboard.html" -Force
Copy-Item "deanship-dashboard.html.backup" "../../../../../../src/main/resources/static/deanship-dashboard.html" -Force

# Step 4: Clear browser cache (instruct users)
Write-Host "Files restored. Instruct users to clear browser cache and refresh."
```

### Rollback Procedure - Method 2: Git Revert

**Time Required:** 2-5 minutes  
**Downtime:** Minimal (page refresh required)

```bash
# Step 1: Identify the commit to revert
git log --oneline --grep="unified file explorer"

# Step 2: Revert the commit(s)
git revert [COMMIT_HASH]

# Step 3: Push the revert
git push origin main

# Step 4: Rebuild and redeploy (if using build process)
mvn clean package
# Deploy the reverted version
```

### Rollback Procedure - Method 3: Branch Rollback

**Time Required:** 5 minutes  
**Downtime:** Minimal (page refresh required)

```bash
# Step 1: Switch to backup branch
git checkout backup/unified-file-explorer-pre-deployment

# Step 2: Force push to main (use with caution)
git push origin backup/unified-file-explorer-pre-deployment:main --force

# Step 3: Update local main branch
git checkout main
git pull origin main --force
```

### Post-Rollback Verification

After rolling back, verify the system is functioning correctly:

1. **Professor Dashboard:**
   - [ ] File Explorer loads successfully
   - [ ] Can browse folders and files
   - [ ] Can upload files
   - [ ] Can download files
   - [ ] "Your Folder" labels appear correctly

2. **HOD Dashboard:**
   - [ ] File Explorer loads successfully
   - [ ] Can browse department files (read-only)
   - [ ] Cannot upload files (read-only enforced)
   - [ ] Can download files
   - [ ] Department filtering works correctly

3. **Deanship Dashboard:**
   - [ ] File Explorer loads successfully
   - [ ] Can browse all departments
   - [ ] Professor labels appear correctly
   - [ ] Cannot upload files (read-only enforced)
   - [ ] Can download files

### Rollback Communication Template

```
Subject: [URGENT] File Explorer Rollback Notification

Dear Team,

We have rolled back the Unified File Explorer deployment due to [REASON].

Impact:
- The system has been restored to the previous version
- All functionality should be working as before the deployment
- Users may need to clear their browser cache and refresh

Timeline:
- Issue detected: [TIME]
- Rollback initiated: [TIME]
- Rollback completed: [TIME]
- System verified: [TIME]

Next Steps:
- We are investigating the root cause
- A fix will be developed and tested
- A new deployment will be scheduled after thorough testing

If you experience any issues, please contact [SUPPORT EMAIL/PHONE].

Thank you for your patience.

[YOUR NAME]
[YOUR TITLE]
```

---

## Verification Steps

### Pre-Deployment Verification

Before deploying to production, verify in a staging environment:

1. **Functional Testing:**
   - [ ] All three dashboards load without errors
   - [ ] File Explorer renders correctly in each dashboard
   - [ ] Breadcrumb navigation works
   - [ ] Folder navigation works
   - [ ] File upload works (Professor only)
   - [ ] File download works (all roles)
   - [ ] Role-specific labels display correctly
   - [ ] Read-only enforcement works (HOD, Deanship)

2. **Visual Testing:**
   - [ ] Folder cards match Professor Dashboard design
   - [ ] File tables match Professor Dashboard design
   - [ ] Colors and spacing are consistent
   - [ ] Hover effects work correctly
   - [ ] Responsive design works on mobile

3. **Performance Testing:**
   - [ ] Page load time <3 seconds
   - [ ] File Explorer initialization <1 second
   - [ ] Folder navigation <500ms
   - [ ] No memory leaks after 30 minutes of use

4. **Browser Compatibility:**
   - [ ] Chrome 90+
   - [ ] Firefox 88+
   - [ ] Safari 14+
   - [ ] Edge 90+

### Post-Deployment Verification

After deploying to production, verify immediately:

1. **Smoke Tests (5 minutes):**
   - [ ] Professor Dashboard loads
   - [ ] HOD Dashboard loads
   - [ ] Deanship Dashboard loads
   - [ ] File Explorer visible in each dashboard
   - [ ] No JavaScript console errors

2. **Critical Path Tests (15 minutes):**
   - [ ] Professor can upload a file
   - [ ] Professor can download a file
   - [ ] HOD can browse department files
   - [ ] HOD cannot upload files
   - [ ] Deanship can browse all departments
   - [ ] Deanship cannot upload files

3. **Monitoring (First 24 hours):**
   - Monitor error logs for JavaScript errors
   - Monitor API error rates
   - Monitor page load times
   - Collect user feedback

### Verification Checklist

Use this checklist for each deployment:

```
Deployment Verification Checklist
Date: _______________
Deployed By: _______________

Pre-Deployment:
[ ] Backups created and verified
[ ] Staging environment tested
[ ] All tests passed
[ ] Rollback plan reviewed
[ ] Team notified of deployment

Deployment:
[ ] Files deployed successfully
[ ] No deployment errors
[ ] Application restarted (if needed)

Post-Deployment:
[ ] Smoke tests passed
[ ] Critical path tests passed
[ ] No console errors
[ ] Performance acceptable
[ ] Users notified

Sign-off:
Developer: _______________ Date: _______________
QA: _______________ Date: _______________
Manager: _______________ Date: _______________
```

---

## Emergency Contacts

### Technical Team

| Role | Name | Email | Phone | Availability |
|------|------|-------|-------|--------------|
| Lead Developer | [NAME] | [EMAIL] | [PHONE] | 24/7 |
| Backend Developer | [NAME] | [EMAIL] | [PHONE] | Business hours |
| Frontend Developer | [NAME] | [EMAIL] | [PHONE] | Business hours |
| DevOps Engineer | [NAME] | [EMAIL] | [PHONE] | 24/7 |

### Management

| Role | Name | Email | Phone |
|------|------|-------|-------|
| Technical Manager | [NAME] | [EMAIL] | [PHONE] |
| Product Owner | [NAME] | [EMAIL] | [PHONE] |
| IT Director | [NAME] | [EMAIL] | [PHONE] |

### Escalation Path

1. **Level 1:** Developer on call (immediate response)
2. **Level 2:** Lead Developer (within 15 minutes)
3. **Level 3:** Technical Manager (within 30 minutes)
4. **Level 4:** IT Director (within 1 hour)

### Communication Channels

- **Slack:** #unified-file-explorer-deployment
- **Email:** dev-team@alquds.edu
- **Phone:** [EMERGENCY HOTLINE]
- **Incident Management:** [TICKET SYSTEM URL]

---

## Appendix

### Known Issues and Workarounds

| Issue | Workaround | Status |
|-------|-----------|--------|
| None identified | N/A | N/A |

### Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-11-20 | 1.0 | Initial rollback plan created | [YOUR NAME] |

### Related Documents

- [Requirements Document](.kiro/specs/unified-file-explorer/requirements.md)
- [Design Document](.kiro/specs/unified-file-explorer/design.md)
- [Implementation Tasks](.kiro/specs/unified-file-explorer/tasks.md)
- [Deployment Checklist](.kiro/specs/unified-file-explorer/DEPLOYMENT_CHECKLIST.md)

---

**Document Status:** Active  
**Next Review Date:** [DATE]  
**Document Owner:** [YOUR NAME]
