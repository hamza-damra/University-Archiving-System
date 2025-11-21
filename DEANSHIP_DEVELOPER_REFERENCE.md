# Deanship Dashboard Developer Reference

## Quick Start

This is a quick reference for developers working on the Deanship Dashboard multi-page application.

## File Locations Quick Reference

### Backend
- **View Controller**: `src/main/java/com/alqude/edu/ArchiveSystem/controller/DeanshipViewController.java`
- **API Controller**: `src/main/java/com/alqude/edu/ArchiveSystem/controller/DeanshipController.java` (unchanged)

### Frontend HTML
All HTML files in `src/main/resources/static/deanship/`:
- `dashboard.html` - Main landing page
- `academic-years.html` - Academic years management
- `professors.html` - Professors management
- `courses.html` - Courses management
- `course-assignments.html` - Course assignments
- `reports.html` - Reports page
- `file-explorer.html` - File explorer page

### Frontend JavaScript
All JavaScript files in `src/main/resources/static/js/`:
- `deanship-common.js` - **Shared layout and context management**
- `dashboard.js` - Dashboard page logic
- `academic-years.js` - Academic years page logic
- `professors.js` - Professors page logic
- `courses.js` - Courses page logic
- `course-assignments.js` - Course assignments page logic
- `reports.js` - Reports page logic
- `file-explorer-page.js` - File explorer page wrapper

### Frontend CSS
- `src/main/resources/static/css/deanship-layout.css` - **Shared layout styles**
- `src/main/resources/static/css/custom.css` - Global styles (unchanged)

## Architecture Overview

### Request Flow

```
User Request: /deanship/professors
    ↓
DeanshipViewController.professors()
    ↓
Returns view name: "deanship/professors"
    ↓
Spring resolves to: static/deanship/professors.html
    ↓
HTML loads: static/js/professors.js
    ↓
professors.js imports: static/js/deanship-common.js
    ↓
DeanshipLayout initializes (auth, filters, nav)
    ↓
Page loads data via API: /api/deanship/professors
    ↓
Page renders content
```

### Shared Layout Pattern

Every page follows this pattern:

1. **HTML Structure**: Includes shared layout elements
2. **JavaScript Module**: Imports and initializes `DeanshipLayout`
3. **Data Loading**: Fetches page-specific data from API
4. **Context Awareness**: Registers callbacks for academic year/semester changes (if needed)

## Code Patterns

### Creating a New Page

If you need to add a new page to the deanship dashboard:

#### 1. Add Route to View Controller

```java
// In DeanshipViewController.java
@GetMapping("/new-page")
public String newPage() {
    return "deanship/new-page";
}
```

#### 2. Create HTML Template

```html
<!-- static/deanship/new-page.html -->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>New Page - Deanship Dashboard</title>
    <link rel="stylesheet" href="/css/custom.css">
    <link rel="stylesheet" href="/css/deanship-layout.css">
</head>
<body>
    <div id="deanship-layout">
        <!-- Shared header -->
        <header class="deanship-header">
            <div class="header-title">Al-Quds University / Deanship Dashboard</div>
            <div class="header-user">
                <span id="deanshipName"></span>
                <button id="logoutBtn" class="btn btn-secondary">Logout</button>
            </div>
        </header>

        <!-- Shared navigation -->
        <nav class="deanship-nav">
            <a href="/deanship/dashboard" class="nav-link">Dashboard</a>
            <a href="/deanship/academic-years" class="nav-link">Academic Years</a>
            <a href="/deanship/professors" class="nav-link">Professors</a>
            <a href="/deanship/courses" class="nav-link">Courses</a>
            <a href="/deanship/course-assignments" class="nav-link">Assignments</a>
            <a href="/deanship/reports" class="nav-link">Reports</a>
            <a href="/deanship/file-explorer" class="nav-link">File Explorer</a>
            <a href="/deanship/new-page" class="nav-link">New Page</a>
        </nav>

        <!-- Shared filters -->
        <div class="deanship-filters">
            <div class="filter-group">
                <label for="academicYearSelect">Academic Year:</label>
                <select id="academicYearSelect" class="form-control"></select>
            </div>
            <div class="filter-group">
                <label for="semesterSelect">Semester:</label>
                <select id="semesterSelect" class="form-control"></select>
            </div>
        </div>

        <!-- Page-specific content -->
        <main id="page-content">
            <div class="page-header">
                <h1>New Page Title</h1>
                <button id="addBtn" class="btn btn-primary">Add Item</button>
            </div>

            <div id="content-area">
                <!-- Your page content here -->
            </div>
        </main>
    </div>

    <script type="module" src="/js/new-page.js"></script>
</body>
</html>
```

#### 3. Create JavaScript Module

```javascript
// static/js/new-page.js
import { DeanshipLayout } from './deanship-common.js';
import { apiRequest } from './api.js';
import { showToast, showModal } from './ui.js';

class NewPage {
    constructor() {
        this.layout = new DeanshipLayout();
        this.data = [];
    }

    async initialize() {
        // Initialize shared layout
        await this.layout.initialize();

        // If page needs academic context, register callbacks
        this.layout.onAcademicYearChange(() => this.loadData());
        this.layout.onSemesterChange(() => this.loadData());

        // Load initial data
        await this.loadData();

        // Setup event listeners
        this.setupEventListeners();
    }

    async loadData() {
        try {
            // Show loading indicator
            document.getElementById('content-area').innerHTML = '<p>Loading...</p>';

            // Get academic context if needed
            const context = this.layout.getSelectedContext();
            
            // Fetch data from API
            const response = await apiRequest('/api/deanship/new-endpoint', {
                method: 'GET',
                params: {
                    semesterId: context.semesterId
                }
            });

            this.data = response.data;
            this.render();
        } catch (error) {
            console.error('Failed to load data:', error);
            showToast('Failed to load data. Please try again.', 'error');
            document.getElementById('content-area').innerHTML = '<p>Failed to load data.</p>';
        }
    }

    render() {
        // Render your page content
        const html = this.data.map(item => `
            <div class="item">
                <h3>${item.name}</h3>
                <p>${item.description}</p>
            </div>
        `).join('');

        document.getElementById('content-area').innerHTML = html;
    }

    setupEventListeners() {
        document.getElementById('addBtn').addEventListener('click', () => {
            this.showAddModal();
        });
    }

    showAddModal() {
        // Show modal for adding new item
    }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    const page = new NewPage();
    page.initialize();
});
```

### Using DeanshipLayout

The `DeanshipLayout` class provides shared functionality:

```javascript
import { DeanshipLayout } from './deanship-common.js';

const layout = new DeanshipLayout();

// Initialize (call this first)
await layout.initialize();

// Get selected academic context
const context = layout.getSelectedContext();
// Returns: { academicYearId, semesterId, academicYear, semester }

// Register callback for academic year changes
layout.onAcademicYearChange(() => {
    console.log('Academic year changed');
    // Reload your data
});

// Register callback for semester changes
layout.onSemesterChange(() => {
    console.log('Semester changed');
    // Reload your data
});

// Get all academic years
const academicYears = layout.academicYears;

// Get semesters for selected academic year
const semesters = layout.semesters;
```

### Making API Calls

Use the existing `api.js` module:

```javascript
import { apiRequest } from './api.js';

// GET request
const response = await apiRequest('/api/deanship/professors');
const professors = response.data;

// GET with query parameters
const response = await apiRequest('/api/deanship/professors', {
    method: 'GET',
    params: { departmentId: 5 }
});

// POST request
const response = await apiRequest('/api/deanship/professors', {
    method: 'POST',
    body: {
        name: 'John Doe',
        email: 'john@example.com',
        departmentId: 5
    }
});

// PUT request
const response = await apiRequest(`/api/deanship/professors/${id}`, {
    method: 'PUT',
    body: updatedData
});

// DELETE request
const response = await apiRequest(`/api/deanship/professors/${id}`, {
    method: 'DELETE'
});
```

### Showing Modals

Use the existing `ui.js` module:

```javascript
import { showModal } from './ui.js';

// Show modal with form
showModal('Add Professor', `
    <form id="addProfessorForm">
        <div class="form-group">
            <label for="name">Name:</label>
            <input type="text" id="name" class="form-control" required>
        </div>
        <div class="form-group">
            <label for="email">Email:</label>
            <input type="email" id="email" class="form-control" required>
        </div>
        <button type="submit" class="btn btn-primary">Save</button>
    </form>
`, () => {
    // Callback when modal is shown
    document.getElementById('addProfessorForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        // Handle form submission
    });
});
```

### Showing Toast Notifications

```javascript
import { showToast } from './ui.js';

// Success message
showToast('Professor added successfully', 'success');

// Error message
showToast('Failed to add professor', 'error');

// Info message
showToast('Loading data...', 'info');

// Warning message
showToast('Please select an academic year', 'warning');
```

## Key Architectural Decisions

### 1. Shared Layout via JavaScript Class

**Decision**: Use a JavaScript class (`DeanshipLayout`) to manage shared layout instead of a template engine.

**Rationale**:
- Keeps frontend logic in JavaScript
- Easier to maintain and test
- Allows dynamic behavior (callbacks, state management)
- No need for server-side template rendering

**Trade-offs**:
- Some HTML duplication across pages
- Requires JavaScript to be enabled

### 2. LocalStorage for State Persistence

**Decision**: Use browser localStorage to persist academic year/semester selections.

**Rationale**:
- Simple and fast
- Works across page navigation
- No server-side session management needed
- Survives page refreshes

**Trade-offs**:
- Limited to single browser
- Can be cleared by user
- Not synchronized across devices

### 3. ES6 Modules

**Decision**: Use ES6 modules (`import`/`export`) for JavaScript code organization.

**Rationale**:
- Modern JavaScript standard
- Better code organization
- Explicit dependencies
- Supported by all target browsers

**Trade-offs**:
- Requires `type="module"` in script tags
- Slightly different loading behavior

### 4. Separate Routes for Each Page

**Decision**: Create dedicated routes (`/deanship/dashboard`, `/deanship/professors`, etc.) instead of client-side routing.

**Rationale**:
- Better SEO (if needed in future)
- Bookmarkable URLs
- Browser back/forward works naturally
- Simpler to understand and maintain
- Easier to secure with Spring Security

**Trade-offs**:
- Full page reload on navigation
- More HTML files to maintain

### 5. Keep Existing API Endpoints Unchanged

**Decision**: Don't modify any existing REST API endpoints.

**Rationale**:
- Reduces risk of breaking changes
- Allows gradual migration
- Easier rollback if needed
- Other parts of system may depend on APIs

**Trade-offs**:
- Can't optimize APIs for new page structure
- May have some inefficient API calls

## Common Tasks

### Adding a New Navigation Link

1. Update all HTML files to include the new link in the `<nav>` section
2. Update `deanship-common.js` to highlight the new link when active

### Adding a New Filter

1. Add the filter HTML to all page templates
2. Update `DeanshipLayout` class to manage the filter state
3. Provide callback mechanism for pages to respond to filter changes
4. Persist filter value to localStorage

### Changing Styles

1. For shared styles: Edit `deanship-layout.css`
2. For page-specific styles: Add `<style>` block in page HTML or create page-specific CSS file
3. For global styles: Edit `custom.css`

### Adding a New API Endpoint

1. Add endpoint to `DeanshipController.java` (or appropriate controller)
2. Add service method if needed
3. Add repository method if needed
4. Test endpoint with Postman or curl
5. Call endpoint from frontend JavaScript

### Debugging Issues

1. **Check Browser Console**: Look for JavaScript errors
2. **Check Network Tab**: Look for failed API calls
3. **Check Application Logs**: Look for backend errors
4. **Check LocalStorage**: Verify state is being saved correctly
5. **Check Authentication**: Verify user has correct role

## Testing

### Manual Testing Checklist

For each page:
- [ ] Page loads without errors
- [ ] Navigation links work
- [ ] Active navigation link is highlighted
- [ ] Academic year/semester filters work (if applicable)
- [ ] Data loads correctly
- [ ] CRUD operations work (if applicable)
- [ ] Search/filter functionality works (if applicable)
- [ ] Modals open and close correctly
- [ ] Form validation works
- [ ] Error messages display correctly
- [ ] Logout works

### Browser Testing

Test in:
- [ ] Chrome (latest)
- [ ] Firefox (latest)
- [ ] Edge (latest)
- [ ] Safari (latest)

### Accessibility Testing

- [ ] Keyboard navigation works
- [ ] Screen reader announces content correctly
- [ ] Color contrast meets WCAG AA standards
- [ ] Focus indicators visible
- [ ] Forms have proper labels

## Performance Tips

1. **Minimize API Calls**: Cache data in memory when possible
2. **Debounce Search**: Use debouncing for search inputs
3. **Lazy Load**: Only load data when needed
4. **Use LocalStorage**: Cache reference data (departments, academic years)
5. **Optimize Images**: Use appropriate image sizes and formats
6. **Minimize JavaScript**: Keep page-specific modules small

## Security Best Practices

1. **Validate Input**: Always validate user input on client and server
2. **Escape Output**: Use `textContent` instead of `innerHTML` when possible
3. **Use CSRF Tokens**: Existing `api.js` handles this
4. **Check Authorization**: Backend enforces role-based access
5. **Sanitize Data**: Use existing `ui.js` sanitization functions
6. **HTTPS Only**: Always use HTTPS in production

## Troubleshooting

### Page Shows 404

- Check route is defined in `DeanshipViewController`
- Check HTML file exists in `static/deanship/`
- Check file name matches view name
- Restart application

### JavaScript Module Not Found

- Check file exists in `static/js/`
- Check import path is correct (relative to `static/`)
- Check file has `.js` extension
- Check browser console for exact error

### Academic Context Not Persisting

- Check localStorage is enabled in browser
- Check browser console for errors
- Verify `DeanshipLayout.initialize()` is called
- Check localStorage keys: `deanship_selected_academic_year`, `deanship_selected_semester`

### Styles Not Applied

- Check CSS file exists in `static/css/`
- Check `<link>` tag path is correct
- Clear browser cache
- Check browser network tab for CSS loading

### API Call Fails

- Check endpoint exists in backend controller
- Check user has correct role
- Check request format matches API expectations
- Check browser network tab for error details
- Check application logs for backend errors

## Resources

- **Deployment Guide**: `DEANSHIP_DEPLOYMENT_GUIDE.md`
- **Design Document**: `.kiro/specs/deanship-multi-page-refactor/design.md`
- **Requirements**: `.kiro/specs/deanship-multi-page-refactor/requirements.md`
- **Tasks**: `.kiro/specs/deanship-multi-page-refactor/tasks.md`

## Getting Help

1. Check this reference guide
2. Review inline code comments
3. Check browser console for errors
4. Check application logs
5. Review design and requirements documents
6. Contact development team

## Contributing

When making changes:

1. Follow existing code patterns
2. Add inline comments for complex logic
3. Update documentation if needed
4. Test thoroughly before committing
5. Update this reference if adding new patterns

---

**Last Updated**: November 2025
**Version**: 1.0
