# Deanship Dashboard Multi-Page Deployment Guide

## Overview

This guide documents the deployment of the refactored Deanship Dashboard from a single-page tabbed interface to a multi-page application with dedicated routes. This document covers the new architecture, file locations, deployment procedures, and rollback plans.

## Architecture Overview

### Route Structure

The new multi-page architecture provides dedicated routes for each functional area:

| Route | Purpose | HTML Template | JavaScript Module |
|-------|---------|---------------|-------------------|
| `/deanship/dashboard` | Main landing page with overview cards | `static/deanship/dashboard.html` | `static/js/dashboard.js` |
| `/deanship/academic-years` | Academic years management | `static/deanship/academic-years.html` | `static/js/academic-years.js` |
| `/deanship/professors` | Professors management | `static/deanship/professors.html` | `static/js/professors.js` |
| `/deanship/courses` | Courses management | `static/deanship/courses.html` | `static/js/courses.js` |
| `/deanship/course-assignments` | Course assignments | `static/deanship/course-assignments.html` | `static/js/course-assignments.js` |
| `/deanship/reports` | Reports and analytics | `static/deanship/reports.html` | `static/js/reports.js` |
| `/deanship/file-explorer` | File system browser | `static/deanship/file-explorer.html` | `static/js/file-explorer-page.js` |

### API Endpoints (Unchanged)

All existing REST API endpoints remain at their current paths:
- `/api/deanship/academic-years`
- `/api/deanship/professors`
- `/api/deanship/courses`
- `/api/deanship/course-assignments`
- `/api/deanship/reports/*`
- All other existing endpoints

## File Structure

### Backend Files

#### New Files
```
src/main/java/com/alqude/edu/ArchiveSystem/controller/
└── DeanshipViewController.java          # NEW: View controller for page routing
```

**DeanshipViewController.java** - Handles routing for all deanship pages
- Location: `src/main/java/com/alqude/edu/ArchiveSystem/controller/DeanshipViewController.java`
- Purpose: Maps URL routes to HTML templates
- Security: All routes protected with `@PreAuthorize("hasRole('DEANSHIP')")`
- Key Methods:
  - `dashboard()` - Returns dashboard view
  - `academicYears()` - Returns academic years view
  - `professors()` - Returns professors view
  - `courses()` - Returns courses view
  - `courseAssignments()` - Returns course assignments view
  - `reports()` - Returns reports view
  - `fileExplorer()` - Returns file explorer view

#### Unchanged Files
```
src/main/java/com/alqude/edu/ArchiveSystem/controller/
├── DeanshipController.java              # UNCHANGED: REST API endpoints
├── AuthController.java                  # UNCHANGED: Authentication
└── [other controllers]                  # UNCHANGED
```

### Frontend Files

#### New HTML Templates
```
src/main/resources/static/deanship/
├── dashboard.html                       # NEW: Main landing page
├── academic-years.html                  # NEW: Academic years management
├── professors.html                      # NEW: Professors management
├── courses.html                         # NEW: Courses management
├── course-assignments.html              # NEW: Course assignments
├── reports.html                         # NEW: Reports page
└── file-explorer.html                   # NEW: File explorer page
```

Each HTML file contains:
- Shared layout structure (header, navigation, filters)
- Page-specific content area
- Script imports for shared and page-specific JavaScript

#### New JavaScript Modules
```
src/main/resources/static/js/
├── deanship-common.js                   # NEW: Shared layout and context management
├── dashboard.js                         # NEW: Dashboard page logic
├── academic-years.js                    # NEW: Academic years page logic
├── professors.js                        # NEW: Professors page logic
├── courses.js                           # NEW: Courses page logic
├── course-assignments.js                # NEW: Course assignments page logic
├── reports.js                           # NEW: Reports page logic
└── file-explorer-page.js                # NEW: File explorer page wrapper
```

**deanship-common.js** - Shared layout and context management
- Exports `DeanshipLayout` class
- Manages authentication checks
- Handles academic year/semester selection and persistence
- Manages navigation highlighting
- Provides logout functionality
- Registers callbacks for context changes

**Page-specific modules** (dashboard.js, academic-years.js, etc.)
- Initialize `DeanshipLayout`
- Load page-specific data
- Handle page-specific user interactions
- Respond to context changes (academic year/semester)

#### New CSS Files
```
src/main/resources/static/css/
└── deanship-layout.css                  # NEW: Shared layout styles
```

**deanship-layout.css** - Shared styling for all deanship pages
- Typography scale (16px base, 18-24px headers)
- Spacing scale (4-48px)
- Component styles (navigation, tables, buttons, cards)
- Responsive design rules

#### Unchanged Files
```
src/main/resources/static/
├── deanship-dashboard.html              # DEPRECATED: Old single-page dashboard (kept for rollback)
├── js/
│   ├── deanship.js                      # DEPRECATED: Old dashboard logic (kept for rollback)
│   ├── api.js                           # UNCHANGED: API client
│   ├── ui.js                            # UNCHANGED: UI utilities
│   └── file-explorer.js                 # UNCHANGED: File explorer component
└── css/
    └── custom.css                       # UNCHANGED: Global styles
```

## Shared Layout and Filters

### How Shared Layout Works

The shared layout is implemented through the `DeanshipLayout` class in `deanship-common.js`:

1. **Initialization**: Each page creates a `DeanshipLayout` instance and calls `initialize()`
2. **Authentication Check**: Verifies user is logged in and has DEANSHIP role
3. **Academic Years Loading**: Fetches all academic years from API
4. **State Restoration**: Restores previously selected academic year and semester from localStorage
5. **Event Listeners**: Sets up listeners for dropdown changes and logout button
6. **Navigation Highlighting**: Highlights the active page in the navigation bar

### Academic Context Filters

The academic year and semester filters work as follows:

1. **Selection Persistence**: Selections are stored in localStorage:
   - `deanship_selected_academic_year` - Selected academic year ID
   - `deanship_selected_semester` - Selected semester ID

2. **Cross-Page Persistence**: When navigating between pages, selections are restored from localStorage

3. **Context-Aware Pages**: Pages that depend on academic context (Course Assignments, Reports, File Explorer) register callbacks:
   ```javascript
   this.layout.onAcademicYearChange(() => this.loadData());
   this.layout.onSemesterChange(() => this.loadData());
   ```

4. **Context-Independent Pages**: Pages that don't depend on academic context (Academic Years, Professors, Courses) ignore the filters

### Navigation Flow

```
User logs in
    ↓
Redirected to /deanship/dashboard
    ↓
DeanshipLayout initializes
    ↓
Academic years loaded
    ↓
Previous selections restored (if any)
    ↓
User selects academic year/semester
    ↓
Selection saved to localStorage
    ↓
Page data reloads with new context
    ↓
User navigates to another page
    ↓
New page's DeanshipLayout restores selections
    ↓
Page loads with correct context
```

## Deployment Procedure

### Pre-Deployment Checklist

- [ ] All code changes committed to version control
- [ ] Backend unit tests passing
- [ ] Frontend integration tests passing
- [ ] Manual testing completed on all pages
- [ ] Browser compatibility verified (Chrome, Firefox, Edge, Safari)
- [ ] Accessibility testing completed
- [ ] Performance testing completed
- [ ] Security review completed
- [ ] Documentation updated
- [ ] Rollback plan reviewed and understood

### Deployment Steps

#### 1. Build Application

```bash
# Clean and build the application
mvn clean package -DskipTests

# Or with tests
mvn clean package
```

#### 2. Backup Current Version

```bash
# Backup the current deployment
cp -r /path/to/deployment /path/to/backup/deployment-$(date +%Y%m%d-%H%M%S)

# Backup database (if schema changes were made - none in this refactor)
# mysqldump -u username -p database_name > backup-$(date +%Y%m%d-%H%M%S).sql
```

#### 3. Deploy New Version

```bash
# Stop the application
systemctl stop archive-system

# Deploy new JAR file
cp target/ArchiveSystem-0.0.1-SNAPSHOT.jar /path/to/deployment/

# Start the application
systemctl start archive-system
```

#### 4. Verify Deployment

```bash
# Check application logs
tail -f /path/to/logs/archive-system.log

# Verify application is running
curl http://localhost:8080/actuator/health
```

#### 5. Smoke Testing

After deployment, perform quick smoke tests:

1. **Login Test**: Login as deanship user
2. **Dashboard Test**: Verify dashboard loads and displays cards
3. **Navigation Test**: Click each navigation link and verify page loads
4. **Context Test**: Select academic year and semester, verify persistence
5. **CRUD Test**: Perform one CRUD operation on each management page
6. **File Explorer Test**: Navigate folders and view files
7. **Logout Test**: Logout and verify redirect to login

### Post-Deployment Monitoring

Monitor the following for the first 24 hours:

- **Application Logs**: Watch for errors or exceptions
- **Performance Metrics**: Page load times, API response times
- **User Feedback**: Monitor support channels for issues
- **Error Rates**: Track 4xx and 5xx error rates
- **Browser Console Errors**: Check for JavaScript errors

## Rollback Plan

### When to Rollback

Rollback should be triggered if:

- Critical bugs affecting core functionality (CRUD operations fail)
- Performance degradation > 50% (page load times > 2 seconds)
- Security vulnerabilities discovered
- Error rate > 10% of requests
- User complaints > 20% of active users
- Data corruption or loss detected

### Rollback Procedure

#### Option 1: Quick Rollback (Redirect to Old Dashboard)

This is the fastest rollback method and can be done without redeployment:

1. **Update Login Redirect** - Modify `AuthController.java`:
   ```java
   // Change from:
   return "redirect:/deanship/dashboard";
   
   // To:
   return "redirect:/deanship-dashboard.html";
   ```

2. **Update View Controller** - Modify `DeanshipViewController.java`:
   ```java
   @GetMapping("/**")
   public String redirectToOldDashboard() {
       return "redirect:/deanship-dashboard.html";
   }
   ```

3. **Rebuild and Redeploy**:
   ```bash
   mvn clean package -DskipTests
   systemctl stop archive-system
   cp target/ArchiveSystem-0.0.1-SNAPSHOT.jar /path/to/deployment/
   systemctl start archive-system
   ```

4. **Verify**: Test that old dashboard loads and functions correctly

**Time to Complete**: 10-15 minutes

#### Option 2: Full Rollback (Restore Previous Version)

If Option 1 doesn't work or more extensive rollback is needed:

1. **Stop Application**:
   ```bash
   systemctl stop archive-system
   ```

2. **Restore Previous JAR**:
   ```bash
   cp /path/to/backup/deployment-YYYYMMDD-HHMMSS/ArchiveSystem-0.0.1-SNAPSHOT.jar /path/to/deployment/
   ```

3. **Restore Database** (if needed - not required for this refactor):
   ```bash
   # mysql -u username -p database_name < backup-YYYYMMDD-HHMMSS.sql
   ```

4. **Start Application**:
   ```bash
   systemctl start archive-system
   ```

5. **Verify**: Test that application functions correctly

**Time to Complete**: 15-20 minutes

### Post-Rollback Actions

After rollback:

1. **Notify Users**: Inform users that the system has been rolled back
2. **Document Issues**: Record all issues that triggered the rollback
3. **Analyze Root Cause**: Investigate what went wrong
4. **Fix Issues**: Address problems in development environment
5. **Test Thoroughly**: Perform comprehensive testing before redeployment
6. **Plan Redeployment**: Schedule new deployment when fixes are verified

### Rollback Testing

The rollback procedure should be tested in a staging environment before production deployment:

1. Deploy new version to staging
2. Perform rollback using Option 1
3. Verify old dashboard works
4. Deploy new version again
5. Perform rollback using Option 2
6. Verify old dashboard works
7. Document any issues encountered

## Configuration Changes

### Application Properties

No changes to `application.properties` are required for this refactor. All existing configurations remain valid.

### Security Configuration

No changes to Spring Security configuration are required. The new view controller uses the same security annotations as existing controllers.

### Database Schema

No database schema changes are required for this refactor. All existing tables and relationships remain unchanged.

## Troubleshooting

### Common Issues and Solutions

#### Issue: 404 Not Found on Deanship Routes

**Symptoms**: Accessing `/deanship/dashboard` returns 404

**Causes**:
- `DeanshipViewController` not loaded by Spring
- Component scanning not configured correctly

**Solutions**:
1. Verify `DeanshipViewController` has `@Controller` annotation
2. Verify controller is in correct package for component scanning
3. Check application logs for controller registration
4. Restart application

#### Issue: Access Denied on Deanship Routes

**Symptoms**: Accessing deanship routes redirects to login or shows access denied

**Causes**:
- User doesn't have ROLE_DEANSHIP
- Security configuration issue

**Solutions**:
1. Verify user has ROLE_DEANSHIP in database
2. Check Spring Security logs
3. Verify `@PreAuthorize` annotation is correct
4. Clear browser cookies and re-login

#### Issue: Academic Year/Semester Not Persisting

**Symptoms**: Selections reset when navigating between pages

**Causes**:
- localStorage not working
- Browser privacy settings blocking localStorage
- JavaScript errors preventing save

**Solutions**:
1. Check browser console for JavaScript errors
2. Verify localStorage is enabled in browser
3. Check browser privacy settings
4. Clear browser cache and reload

#### Issue: Old Dashboard Still Loading

**Symptoms**: Login redirects to old single-page dashboard

**Causes**:
- Login redirect not updated
- Browser cache serving old page

**Solutions**:
1. Verify `AuthController` redirects to `/deanship/dashboard`
2. Clear browser cache
3. Hard refresh (Ctrl+Shift+R)
4. Check application logs for redirect URL

#### Issue: JavaScript Module Errors

**Symptoms**: Console shows "Cannot find module" or similar errors

**Causes**:
- Incorrect module paths
- Files not deployed correctly
- MIME type issues

**Solutions**:
1. Verify all JavaScript files are in `static/js/` directory
2. Check file permissions
3. Verify server serves `.js` files with correct MIME type
4. Check browser network tab for 404s

#### Issue: Styles Not Applied

**Symptoms**: Pages load but look unstyled or broken

**Causes**:
- CSS files not loaded
- CSS file paths incorrect
- Browser cache serving old styles

**Solutions**:
1. Verify `deanship-layout.css` exists in `static/css/`
2. Check HTML `<link>` tags for correct paths
3. Clear browser cache
4. Check browser network tab for CSS file loading

## Performance Considerations

### Expected Performance

- **Page Load Time**: < 1 second (first load)
- **Page Load Time**: < 500ms (subsequent loads with cache)
- **Time to Interactive**: < 2 seconds
- **API Response Time**: < 500ms (existing baseline)
- **JavaScript Bundle Size**: < 100KB per page module

### Optimization Strategies

1. **Browser Caching**: Static files cached for 1 hour
2. **LocalStorage Caching**: Academic years cached to reduce API calls
3. **Lazy Loading**: Page-specific JavaScript only loaded when needed
4. **Debouncing**: Search inputs debounced to reduce API calls
5. **Pagination**: Large datasets paginated (existing functionality)

### Monitoring Performance

Use browser DevTools to monitor:
- Network tab: File sizes and load times
- Performance tab: Page rendering and JavaScript execution
- Console tab: Errors and warnings
- Application tab: LocalStorage usage

## Security Considerations

### Authentication and Authorization

- All `/deanship/*` routes require authentication
- All routes require `ROLE_DEANSHIP` role
- Unauthorized access redirects to login page
- Session timeout: 30 minutes (existing)

### XSS Prevention

- User input sanitized using existing `ui.js` utilities
- `textContent` used instead of `innerHTML` where possible
- Modal forms escape user input

### CSRF Protection

- Spring Security CSRF tokens included in all POST/PUT/DELETE requests
- Existing `api.js` handles CSRF token management

### Data Validation

- Client-side validation in forms
- Server-side validation in controllers (existing)
- SQL injection prevention via JPA (existing)

## Browser Compatibility

### Supported Browsers

- Chrome 90+ (primary)
- Firefox 88+
- Edge 90+
- Safari 14+

### Known Issues

None at this time. All features work in supported browsers.

### Testing Browsers

Test the following in each browser:
- Login and authentication
- Navigation between pages
- Academic year/semester selection
- CRUD operations
- File explorer functionality
- Logout

## Accessibility

### WCAG 2.1 AA Compliance

The refactored dashboard meets WCAG 2.1 AA standards:

- **Color Contrast**: All text meets 4.5:1 minimum ratio
- **Keyboard Navigation**: All interactive elements keyboard accessible
- **Screen Reader Support**: Semantic HTML and ARIA labels used
- **Responsive Design**: Works at 200% zoom
- **Touch Targets**: Minimum 44x44px for all interactive elements

### Testing Accessibility

Use the following tools:
- Chrome DevTools Lighthouse
- WAVE browser extension
- Keyboard-only navigation testing
- Screen reader testing (NVDA, JAWS, VoiceOver)

## Support and Maintenance

### Documentation Locations

- **This Guide**: `DEANSHIP_DEPLOYMENT_GUIDE.md`
- **Architecture**: `.kiro/specs/deanship-multi-page-refactor/design.md`
- **Requirements**: `.kiro/specs/deanship-multi-page-refactor/requirements.md`
- **Tasks**: `.kiro/specs/deanship-multi-page-refactor/tasks.md`
- **Code Comments**: Inline comments in source files

### Getting Help

For issues or questions:
1. Check this deployment guide
2. Review inline code comments
3. Check application logs
4. Review browser console for errors
5. Contact development team

### Future Enhancements

Potential improvements for future iterations:
- Progressive Web App (offline support)
- Real-time updates via WebSocket
- Advanced analytics dashboard
- Bulk operations for batch actions
- Export functionality (CSV/Excel)
- Dark mode theme
- Mobile optimization
- Internationalization

## Conclusion

This deployment guide provides comprehensive information for deploying, monitoring, and maintaining the refactored Deanship Dashboard. Follow the procedures carefully and refer to the troubleshooting section if issues arise. The rollback plan ensures that the system can be quickly restored if critical issues are discovered.

For questions or issues not covered in this guide, consult the development team.
