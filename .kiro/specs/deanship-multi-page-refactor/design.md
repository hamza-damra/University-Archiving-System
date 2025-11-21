# Design Document: Deanship Dashboard Multi-Page Refactor

## Overview

This document outlines the technical design for refactoring the Deanship Dashboard from a single-page tabbed application into a multi-page application with dedicated routes. The refactor will improve navigation, maintainability, and user experience while preserving all existing functionality.

### Goals

1. **Separation of Concerns**: Each functional area gets its own page with dedicated HTML, CSS, and JavaScript
2. **Improved Navigation**: Users can bookmark specific pages and use browser navigation naturally
3. **Better Maintainability**: Smaller, focused code files instead of one large monolithic file
4. **Enhanced UX**: Larger fonts, better spacing, and a central dashboard for quick access
5. **Backward Compatibility**: All existing features and APIs remain functional

### Non-Goals

- Changing the authentication/authorization mechanism
- Modifying existing API contracts or data models
- Implementing new features beyond the refactor scope
- Changing the overall visual design language (colors, branding)

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Browser (Client)                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Shared Layout (header, nav, filters)                │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │  Page-Specific Content                               │  │
│  │  - Dashboard / Academic Years / Professors / etc.    │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↕                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  JavaScript Modules                                   │  │
│  │  - api.js (shared API client)                        │  │
│  │  - ui.js (shared UI utilities)                       │  │
│  │  - file-explorer.js (existing component)             │  │
│  │  - deanship-common.js (NEW: shared layout logic)     │  │
│  │  - dashboard.js, academic-years.js, etc. (NEW)       │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                          ↕ HTTP/REST
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot Backend                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  View Controllers (NEW)                              │  │
│  │  - DeanshipViewController                            │  │
│  │    - /deanship/dashboard → dashboard.html            │  │
│  │    - /deanship/academic-years → academic-years.html  │  │
│  │    - /deanship/professors → professors.html          │  │
│  │    - etc.                                            │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  REST API Controllers (EXISTING - unchanged)         │  │
│  │  - DeanshipController                                │  │
│  │    - /api/deanship/academic-years                    │  │
│  │    - /api/deanship/professors                        │  │
│  │    - /api/deanship/courses                           │  │
│  │    - etc.                                            │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Routing Strategy

**Frontend Routes** (handled by Spring MVC view controllers):
- `/deanship/dashboard` → Main dashboard landing page
- `/deanship/academic-years` → Academic years management
- `/deanship/professors` → Professors management
- `/deanship/courses` → Courses management
- `/deanship/course-assignments` → Course assignments
- `/deanship/reports` → Reports and analytics
- `/deanship/file-explorer` → File system browser

**API Routes** (existing, unchanged):
- `/api/deanship/*` → All REST API endpoints remain at their current paths

### State Management

**Client-Side State** (localStorage):
- `deanship_selected_academic_year`: Currently selected academic year ID
- `deanship_selected_semester`: Currently selected semester ID
- `deanship_last_page`: Last visited page for restoration
- `auth_token`: Authentication token (existing)
- `user_info`: User information (existing)

**Session State** (server-side):
- Authentication session (existing, unchanged)
- User role and permissions (existing, unchanged)

## Components and Interfaces

### 1. Shared Layout Component

**Purpose**: Provide consistent header, navigation, and filters across all pages

**HTML Structure**:
```html
<div id="deanship-layout">
  <!-- Header -->
  <header class="deanship-header">
    <div class="header-title">Al-Quds University / Deanship Dashboard</div>
    <div class="header-user">
      <span id="deanshipName">Dean User</span>
      <button id="logoutBtn">Logout</button>
    </div>
  </header>
  
  <!-- Navigation -->
  <nav class="deanship-nav">
    <a href="/deanship/dashboard" class="nav-link">Dashboard</a>
    <a href="/deanship/academic-years" class="nav-link">Academic Years</a>
    <a href="/deanship/professors" class="nav-link">Professors</a>
    <a href="/deanship/courses" class="nav-link">Courses</a>
    <a href="/deanship/course-assignments" class="nav-link">Assignments</a>
    <a href="/deanship/reports" class="nav-link">Reports</a>
    <a href="/deanship/file-explorer" class="nav-link">File Explorer</a>
  </nav>
  
  <!-- Global Filters -->
  <div class="deanship-filters">
    <select id="academicYearSelect"></select>
    <select id="semesterSelect"></select>
  </div>
  
  <!-- Page Content Slot -->
  <main id="page-content">
    <!-- Page-specific content goes here -->
  </main>
</div>
```

**JavaScript Module** (`deanship-common.js`):
```javascript
export class DeanshipLayout {
  constructor() {
    this.selectedAcademicYearId = null;
    this.selectedSemesterId = null;
    this.academicYears = [];
    this.semesters = [];
  }
  
  async initialize() {
    // Check authentication
    // Load academic years
    // Restore selections from localStorage
    // Set up event listeners
    // Highlight active nav link
  }
  
  onAcademicYearChange(callback) { }
  onSemesterChange(callback) { }
  getSelectedContext() { }
}
```

### 2. Page-Specific Modules

Each page will have its own JavaScript module that:
1. Imports the shared layout
2. Implements page-specific data loading
3. Handles page-specific user interactions
4. Responds to context changes (academic year/semester)

**Example Structure** (`academic-years.js`):
```javascript
import { DeanshipLayout } from './deanship-common.js';
import { apiRequest } from './api.js';
import { showToast, showModal } from './ui.js';

class AcademicYearsPage {
  constructor() {
    this.layout = new DeanshipLayout();
    this.academicYears = [];
  }
  
  async initialize() {
    await this.layout.initialize();
    await this.loadAcademicYears();
    this.setupEventListeners();
  }
  
  async loadAcademicYears() { }
  renderTable() { }
  setupEventListeners() { }
  showAddModal() { }
  showEditModal(id) { }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
  const page = new AcademicYearsPage();
  page.initialize();
});
```

### 3. Backend View Controller

**New Controller** (`DeanshipViewController.java`):
```java
@Controller
@RequestMapping("/deanship")
@PreAuthorize("hasRole('DEANSHIP')")
public class DeanshipViewController {
    
    @GetMapping("/dashboard")
    public String dashboard() {
        return "deanship/dashboard";
    }
    
    @GetMapping("/academic-years")
    public String academicYears() {
        return "deanship/academic-years";
    }
    
    @GetMapping("/professors")
    public String professors() {
        return "deanship/professors";
    }
    
    @GetMapping("/courses")
    public String courses() {
        return "deanship/courses";
    }
    
    @GetMapping("/course-assignments")
    public String courseAssignments() {
        return "deanship/course-assignments";
    }
    
    @GetMapping("/reports")
    public String reports() {
        return "deanship/reports";
    }
    
    @GetMapping("/file-explorer")
    public String fileExplorer() {
        return "deanship/file-explorer";
    }
}
```

**Security Configuration**:
- All `/deanship/*` routes require `ROLE_DEANSHIP`
- Unauthorized access redirects to login page
- Uses existing Spring Security configuration

### 4. Main Dashboard Page

**Purpose**: Landing page with overview cards and quick navigation

**Layout**:
```
┌─────────────────────────────────────────────────────┐
│  Welcome to Deanship Dashboard                      │
│  Manage academic structure and monitor submissions  │
├─────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐         │
│  │ Academic │  │Professors│  │ Courses  │         │
│  │  Years   │  │          │  │          │         │
│  │  [icon]  │  │  [icon]  │  │  [icon]  │         │
│  │  5 active│  │  42 total│  │  128 total│        │
│  │  [Open]  │  │  [Open]  │  │  [Open]  │         │
│  └──────────┘  └──────────┘  └──────────┘         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐         │
│  │  Course  │  │ Reports  │  │   File   │         │
│  │Assignments│  │          │  │ Explorer │         │
│  │  [icon]  │  │  [icon]  │  │  [icon]  │         │
│  │ 215 total│  │ View data│  │  Browse  │         │
│  │  [Open]  │  │  [Open]  │  │  [Open]  │         │
│  └──────────┘  └──────────┘  └──────────┘         │
└─────────────────────────────────────────────────────┘
```

**Data Loading**:
- Fetch summary statistics from existing APIs
- `/api/deanship/academic-years` → count active years
- `/api/deanship/professors` → count total professors
- `/api/deanship/courses` → count total courses
- `/api/deanship/course-assignments?semesterId=X` → count assignments
- `/api/deanship/reports/system-wide?semesterId=X` → get submission percentage

## Data Models

### No Changes to Existing Models

All existing entity classes, DTOs, and database schemas remain unchanged:
- `AcademicYear`
- `Semester`
- `User` (Professor)
- `Course`
- `CourseAssignment`
- `Department`
- `DocumentSubmission`
- etc.

### New Frontend Models

**PageContext** (TypeScript-style interface for documentation):
```typescript
interface PageContext {
  academicYearId: number | null;
  semesterId: number | null;
  academicYear: AcademicYear | null;
  semester: Semester | null;
}
```

**DashboardStats** (for main dashboard):
```typescript
interface DashboardStats {
  activeAcademicYears: number;
  totalProfessors: number;
  activeProfessors: number;
  totalCourses: number;
  activeCourses: number;
  totalAssignments: number;
  submissionCompletionRate: number;
}
```

## Error Handling

### Client-Side Error Handling

**Network Errors**:
```javascript
try {
  const data = await apiRequest('/api/deanship/professors');
  renderProfessors(data);
} catch (error) {
  console.error('Failed to load professors:', error);
  showToast('Failed to load professors. Please try again.', 'error');
  renderEmptyState('Unable to load data');
}
```

**Validation Errors**:
- Form validation before submission
- Display field-specific error messages
- Prevent submission until valid

**Authentication Errors**:
- 401 responses trigger redirect to login
- Clear local storage and session
- Display "Session expired" message

### Server-Side Error Handling

**Existing Error Handling** (unchanged):
- `@ControllerAdvice` for global exception handling
- `ApiResponse<T>` wrapper for consistent error responses
- HTTP status codes: 400 (bad request), 401 (unauthorized), 403 (forbidden), 500 (server error)

**New View Controller Error Handling**:
```java
@ExceptionHandler(AccessDeniedException.class)
public String handleAccessDenied() {
    return "redirect:/login?error=access_denied";
}
```

## Testing Strategy

### Unit Testing

**Frontend Unit Tests** (optional, using Jest):
- Test `DeanshipLayout` class methods
- Test page-specific data transformation functions
- Test form validation logic
- Mock API calls

**Backend Unit Tests** (existing framework):
- Test new `DeanshipViewController` route mappings
- Verify security annotations
- Test view name resolution

### Integration Testing

**Frontend Integration Tests**:
1. **Navigation Flow**:
   - Load dashboard → click card → verify correct page loads
   - Use browser back button → verify previous page loads
   - Bookmark a page → reload → verify page loads correctly

2. **Context Preservation**:
   - Select academic year on page A
   - Navigate to page B
   - Verify academic year selection is preserved
   - Verify data loads with correct filters

3. **Authentication**:
   - Access page without authentication → verify redirect to login
   - Login as non-dean user → verify access denied
   - Login as dean → verify access granted

**Backend Integration Tests**:
- Test view controller endpoints with security context
- Verify unauthorized access returns 403
- Verify correct template names are returned

### Manual Testing Checklist

**Cross-Page Functionality**:
- [ ] All navigation links work correctly
- [ ] Active nav link highlights on each page
- [ ] Academic year/semester filters persist across pages
- [ ] Logout works from all pages
- [ ] Browser back/forward navigation works
- [ ] Page refresh preserves state

**Individual Page Testing**:
- [ ] Dashboard: All cards display, all links work, stats load
- [ ] Academic Years: CRUD operations work, table renders
- [ ] Professors: CRUD operations work, filters work, search works
- [ ] Courses: CRUD operations work, filters work, search works
- [ ] Assignments: Create/delete works, filters work
- [ ] Reports: Report loads with correct data
- [ ] File Explorer: Navigation works, file operations work

**UI/UX Testing**:
- [ ] Fonts are larger and more readable
- [ ] Spacing is adequate between elements
- [ ] Tables have good row height
- [ ] Buttons are prominent and easy to click
- [ ] No horizontal scrolling on 1366x768 resolution
- [ ] Text contrast meets accessibility standards

### Performance Testing

**Metrics to Monitor**:
- Page load time (target: < 1 second)
- Time to interactive (target: < 2 seconds)
- API response times (existing baseline)
- Bundle size (JavaScript files should be < 100KB each)

**Optimization Strategies**:
- Lazy load page-specific JavaScript
- Cache academic years and departments in localStorage
- Debounce search inputs
- Use existing API pagination where available

## Implementation Phases

### Phase 1: Backend Setup
1. Create `DeanshipViewController`
2. Configure view resolver for new templates
3. Test route mappings and security
4. Verify existing API endpoints still work

### Phase 2: Shared Layout
1. Create `deanship-common.js` module
2. Implement `DeanshipLayout` class
3. Create shared CSS for layout
4. Test authentication and context management

### Phase 3: Individual Pages
1. Create HTML templates for each page
2. Create JavaScript modules for each page
3. Migrate existing functionality from `deanship.js`
4. Test each page independently

### Phase 4: Main Dashboard
1. Create dashboard HTML template
2. Implement dashboard.js with stats loading
3. Design and style dashboard cards
4. Test navigation from dashboard

### Phase 5: UI Improvements
1. Update CSS for larger fonts
2. Improve spacing and padding
3. Enhance table styling
4. Test on target resolutions

### Phase 6: Integration Testing
1. Test cross-page navigation
2. Test state preservation
3. Test browser navigation
4. Fix any integration issues

### Phase 7: Deployment
1. Update deployment documentation
2. Create rollback plan
3. Deploy to staging environment
4. Perform final testing
5. Deploy to production

## File Structure

### New File Organization

```
src/main/resources/
├── static/
│   ├── css/
│   │   ├── custom.css (existing, enhanced)
│   │   └── deanship-layout.css (NEW)
│   ├── js/
│   │   ├── api.js (existing, unchanged)
│   │   ├── ui.js (existing, unchanged)
│   │   ├── file-explorer.js (existing, unchanged)
│   │   ├── deanship-common.js (NEW - shared layout)
│   │   ├── dashboard.js (NEW)
│   │   ├── academic-years.js (NEW)
│   │   ├── professors.js (NEW)
│   │   ├── courses.js (NEW)
│   │   ├── course-assignments.js (NEW)
│   │   ├── reports.js (NEW)
│   │   └── file-explorer-page.js (NEW)
│   ├── deanship/
│   │   ├── dashboard.html (NEW)
│   │   ├── academic-years.html (NEW)
│   │   ├── professors.html (NEW)
│   │   ├── courses.html (NEW)
│   │   ├── course-assignments.html (NEW)
│   │   ├── reports.html (NEW)
│   │   └── file-explorer.html (NEW)
│   └── deanship-dashboard.html (DEPRECATED - keep for rollback)
└── java/
    └── com/alqude/edu/ArchiveSystem/
        └── controller/
            ├── DeanshipController.java (existing, unchanged)
            └── DeanshipViewController.java (NEW)
```

### Migration Strategy

1. **Keep existing files**: Don't delete `deanship-dashboard.html` or `deanship.js` initially
2. **Create new files**: Build new multi-page structure alongside existing
3. **Update entry point**: Change login redirect to point to new dashboard
4. **Test thoroughly**: Ensure all functionality works in new structure
5. **Archive old files**: Move old files to `deprecated/` folder after successful deployment

## CSS Design System

### Typography Scale

```css
:root {
  /* Base */
  --font-size-base: 16px;
  --line-height-base: 1.5;
  
  /* Scale */
  --font-size-sm: 14px;
  --font-size-md: 16px;
  --font-size-lg: 18px;
  --font-size-xl: 20px;
  --font-size-2xl: 24px;
  --font-size-3xl: 30px;
  
  /* Weights */
  --font-weight-normal: 400;
  --font-weight-medium: 500;
  --font-weight-semibold: 600;
  --font-weight-bold: 700;
}
```

### Spacing Scale

```css
:root {
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  --spacing-2xl: 48px;
}
```

### Component Styles

**Navigation**:
- Height: 56px
- Font size: 16px
- Active indicator: 3px bottom border
- Hover state: background color change

**Tables**:
- Row height: 56px (increased from 40px)
- Header font size: 18px, weight: 600
- Cell font size: 16px
- Cell padding: 16px

**Buttons**:
- Height: 40px (primary actions)
- Height: 36px (secondary actions)
- Font size: 16px
- Padding: 12px 24px

**Cards** (dashboard):
- Padding: 24px
- Border radius: 8px
- Shadow: 0 2px 8px rgba(0,0,0,0.1)
- Title font size: 20px
- Stat font size: 32px

## Security Considerations

### Authentication

- All `/deanship/*` routes require authentication
- Use existing Spring Security configuration
- Session timeout: 30 minutes (existing)
- Token refresh: handled by existing mechanism

### Authorization

- All routes require `ROLE_DEANSHIP`
- API endpoints maintain existing `@PreAuthorize` annotations
- No changes to permission model

### XSS Prevention

- Use existing sanitization in `ui.js`
- Escape user input in modal forms
- Use textContent instead of innerHTML where possible

### CSRF Protection

- Spring Security CSRF tokens (existing)
- Include CSRF token in all POST/PUT/DELETE requests
- Existing `api.js` handles this

## Accessibility

### WCAG 2.1 AA Compliance

**Color Contrast**:
- Text: minimum 4.5:1 ratio
- Large text (18px+): minimum 3:1 ratio
- Interactive elements: minimum 3:1 ratio

**Keyboard Navigation**:
- All interactive elements focusable
- Logical tab order
- Visible focus indicators
- Skip navigation link

**Screen Reader Support**:
- Semantic HTML elements
- ARIA labels where needed
- Alt text for icons
- Status announcements for dynamic content

**Responsive Design**:
- Minimum font size: 16px
- Touch targets: minimum 44x44px
- No horizontal scrolling on mobile
- Readable at 200% zoom

## Browser Support

**Target Browsers**:
- Chrome 90+ (primary)
- Firefox 88+
- Edge 90+
- Safari 14+

**Features Used**:
- ES6 modules (supported in all target browsers)
- CSS Grid and Flexbox (supported)
- Fetch API (supported)
- LocalStorage (supported)

**Polyfills**: None required for target browsers

## Monitoring and Logging

### Frontend Logging

```javascript
// Log page views
console.log('[Deanship] Page loaded:', window.location.pathname);

// Log user actions
console.log('[Deanship] User action:', action, data);

// Log errors
console.error('[Deanship] Error:', error);
```

### Backend Logging

```java
// Existing SLF4J logging
log.info("Deanship user accessing page: {}", pageName);
log.error("Error loading page: {}", pageName, exception);
```

### Analytics (Optional)

- Track page views per route
- Track time spent on each page
- Track most used features
- Track error rates

## Rollback Plan

### Rollback Triggers

- Critical bugs affecting core functionality
- Performance degradation > 50%
- Security vulnerabilities discovered
- User complaints > 20% of user base

### Rollback Procedure

1. **Immediate**: Change login redirect back to `/deanship-dashboard.html`
2. **Update**: Modify `DeanshipViewController` to redirect all routes to old dashboard
3. **Verify**: Test that old dashboard works correctly
4. **Communicate**: Notify users of temporary rollback
5. **Fix**: Address issues in development environment
6. **Redeploy**: Deploy fixed version after thorough testing

### Rollback Testing

- Keep old dashboard functional for 2 weeks after launch
- Test rollback procedure in staging environment
- Document rollback steps clearly
- Assign rollback responsibility to specific team member

## Future Enhancements

### Potential Improvements (Out of Scope)

1. **Progressive Web App**: Add service worker for offline support
2. **Real-time Updates**: WebSocket for live data updates
3. **Advanced Analytics**: More detailed dashboard metrics
4. **Bulk Operations**: Multi-select for batch actions
5. **Export Functionality**: Export tables to CSV/Excel
6. **Dark Mode**: Theme switcher
7. **Mobile Optimization**: Dedicated mobile layouts
8. **Internationalization**: Multi-language support

These enhancements can be considered in future iterations after the core refactor is stable and validated.
