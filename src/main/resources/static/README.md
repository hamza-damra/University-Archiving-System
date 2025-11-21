# Al-Quds University Archiving System - Frontend

A clean, accessible, and responsive frontend for the Al-Quds University Document Archiving System, built with HTML, Tailwind CSS, and Vanilla JavaScript.

## 📋 Overview

This frontend integrates with the existing Spring Boot backend and provides separate interfaces for:
- **Deanship**: Multi-page dashboard for managing academic structure (academic years, professors, courses, assignments, reports, file explorer)
- **HOD (Head of Department)**: Manage professors and document requests
- **Professors**: View assigned requests and submit documents

## 🛠️ Tech Stack

- **HTML5**: Semantic markup for accessibility
- **Tailwind CSS**: Utility-first CSS framework (via CDN)
- **Vanilla JavaScript (ES6+)**: Modular, async/await, fetch API
- **No build tools required**: Static files served directly by Spring Boot

## 📁 Project Structure

```
src/main/resources/static/
├── index.html                      # Login page
├── hod-dashboard.html              # HOD dashboard (single-page)
├── prof-dashboard.html             # Professor dashboard (single-page)
├── deanship-dashboard.html         # DEPRECATED: Old single-page deanship dashboard (kept for rollback)
├── deanship/                       # NEW: Multi-page deanship dashboard
│   ├── dashboard.html              # Main landing page with overview cards
│   ├── academic-years.html         # Academic years management
│   ├── professors.html             # Professors management
│   ├── courses.html                # Courses management
│   ├── course-assignments.html     # Course assignments
│   ├── reports.html                # Reports and analytics
│   └── file-explorer.html          # File system browser
├── css/
│   ├── custom.css                  # Global supplementary styles
│   └── deanship-layout.css         # NEW: Shared deanship layout styles
└── js/
    ├── api.js                      # Centralized API service
    ├── ui.js                       # UI helper functions (modals, toasts)
    ├── auth.js                     # Login page logic
    ├── file-explorer.js            # Unified File Explorer component
    ├── hod.js                      # HOD dashboard logic
    ├── prof.js                     # Professor dashboard logic
    ├── deanship.js                 # DEPRECATED: Old deanship logic (kept for rollback)
    ├── deanship-common.js          # NEW: Shared deanship layout and context management
    ├── dashboard.js                # NEW: Dashboard page logic
    ├── academic-years.js           # NEW: Academic years page logic
    ├── professors.js               # NEW: Professors page logic
    ├── courses.js                  # NEW: Courses page logic
    ├── course-assignments.js       # NEW: Course assignments page logic
    ├── reports.js                  # NEW: Reports page logic
    └── file-explorer-page.js       # NEW: File explorer page wrapper
```

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Spring Boot application running on port 8080 (default)

### Running the Application

1. **Start the Spring Boot backend**:
   ```bash
   mvn spring-boot:run
   ```
   Or run the application from your IDE.

2. **Access the frontend**:
   Open your browser and navigate to:
   ```
   http://localhost:8080/index.html
   ```

3. **Login with credentials**:
   - Use the credentials configured in your backend
   - HOD users will be redirected to `/hod-dashboard.html`
   - Professor users will be redirected to `/prof-dashboard.html`

### CORS Configuration

Ensure your Spring Boot backend has CORS enabled for local development. The backend should allow:
- Origins: `http://localhost:8080`
- Methods: GET, POST, PUT, DELETE
- Headers: Authorization, Content-Type

If testing from a different port, update the `API_BASE_URL` in `js/api.js`:
```javascript
const API_BASE_URL = 'http://localhost:8080/api';
```

## 🎯 Features

### File Explorer Component (`file-explorer.js`)

The File Explorer is a **unified, reusable component** used across all dashboards (Professor, HOD, Deanship) for hierarchical file navigation. It provides a consistent user experience while supporting role-specific behaviors.

#### Key Features
- **Hierarchical Navigation**: Browse academic years → semesters → professors → courses → document types → files
- **Tree View**: Collapsible folder structure in left panel
- **File List**: Detailed file table with metadata in right panel
- **Breadcrumb Navigation**: Click to navigate back through hierarchy
- **Role-Specific Labels**: Visual indicators based on user role and permissions
- **Lazy Loading**: Folder contents loaded on-demand for performance
- **Empty/Loading/Error States**: Consistent feedback across all dashboards

#### Role-Specific Behavior

**Professor Dashboard**:
- "Your Folder" badge (blue) on owned course folders
- "Read Only" badge (gray) on shared folders
- Upload and file management actions enabled

**HOD Dashboard**:
- Header message: "Browse department files (Read-only)"
- "Read Only" badges on all folders
- Department-scoped view (only own department)
- No upload or edit actions

**Deanship Dashboard**:
- Professor name labels (purple) on professor folders
- Access to all departments
- No upload or edit actions

#### Usage Example

```javascript
import FileExplorer from './file-explorer.js';

// Initialize with role-specific configuration
const fileExplorer = new FileExplorer('fileExplorerContainer', {
    role: 'PROFESSOR',           // 'PROFESSOR', 'HOD', or 'DEANSHIP'
    showOwnershipLabels: true,   // Show "Your Folder" labels
    readOnly: false              // Enable upload actions
});

// Store globally for event handlers
window.fileExplorerInstance = fileExplorer;

// Load data for a semester
await fileExplorer.loadRoot(academicYearId, semesterId);
```

#### Documentation
- **Comprehensive Guide**: `FILE_EXPLORER_DEVELOPER_GUIDE.md`
- **Quick Reference**: `FILE_EXPLORER_QUICK_REFERENCE.md`
- **Master Design**: Professor Dashboard (`prof-dashboard.html`)

### Login Page (`index.html`)
- Email and password authentication
- Client-side validation
- Role-based redirection (HOD/Professor/Deanship)
- JWT token storage in localStorage
- Loading states and error handling

### HOD Dashboard (`hod-dashboard.html`)
- **Professor Management**:
  - View list of professors
  - Search/filter professors
  - Add new professors
  - Edit existing professors
  - Delete professors (with confirmation)
- **Document Request Management**:
  - Create new document requests
  - Assign requests to professors
  - Set deadlines and allowed file types
  - View recent requests with status
  - Generate reports for individual requests

### Professor Dashboard (`prof-dashboard.html`)
- **Assigned Requests**:
  - View all assigned document requests
  - Filter by status (All, Pending, Submitted, Overdue)
  - See deadline countdowns
  - Upload documents with progress tracking
  - Replace previously submitted documents
  - Client-side file validation (extension, size)
- **File Explorer**:
  - Browse own courses and folders hierarchically
  - "Your Folder" labels on owned folders
  - "Read Only" labels on shared folders
  - Upload files to owned folders
  - Download and view files
  - Tree view with lazy loading
- **Notifications**:
  - Real-time notification badge
  - View unseen notifications
  - Mark notifications as read

### Deanship Dashboard (Multi-Page Application)

The Deanship Dashboard has been refactored from a single-page tabbed interface into a **multi-page application** with dedicated routes for each functional area. This improves navigation, maintainability, and user experience.

#### Architecture

**Routes**: Each functional area has its own dedicated route:
- `/deanship/dashboard` - Main landing page with overview cards
- `/deanship/academic-years` - Academic years management
- `/deanship/professors` - Professors management
- `/deanship/courses` - Courses management
- `/deanship/course-assignments` - Course assignments
- `/deanship/reports` - Reports and analytics
- `/deanship/file-explorer` - File system browser

**Shared Layout**: All pages share common elements:
- Header with application title and user info
- Navigation bar with links to all sections
- Academic year and semester filters (global context)
- Logout button

**Academic Context**: The selected academic year and semester persist across pages using localStorage, allowing users to navigate between sections without losing their filter selections.

#### Features by Page

**Dashboard** (`/deanship/dashboard`):
- Overview cards for quick access to all sections
- Summary statistics (active academic years, professors, courses, assignments)
- Submission completion percentage
- Click cards to navigate to specific sections

**Academic Years** (`/deanship/academic-years`):
- View all academic years in a table
- Add new academic years
- Edit existing academic years
- Activate/deactivate academic years
- Empty state when no academic years exist

**Professors** (`/deanship/professors`):
- View all professors in a table
- Search by name, email, or professor ID
- Filter by department
- Add new professors
- Edit existing professors
- Activate/deactivate professors

**Courses** (`/deanship/courses`):
- View all courses in a table
- Search by course code or name
- Filter by department
- Add new courses
- Edit existing courses
- Deactivate courses

**Course Assignments** (`/deanship/course-assignments`):
- View assignments for selected semester
- Filter by professor or course
- Assign professors to courses
- Unassign courses (with confirmation)
- Context-aware: requires academic year/semester selection

**Reports** (`/deanship/reports`):
- View submission status reports
- System-wide submission statistics
- Completion percentages
- Context-aware: requires academic year/semester selection

**File Explorer** (`/deanship/file-explorer`):
- Browse all departments, professors, and courses
- Professor name labels on professor folders
- Read-only access (no upload/edit)
- Download and view files
- Full system visibility
- Context-aware: requires academic year/semester selection

#### Technical Implementation

**Backend**: New `DeanshipViewController` handles route mappings:
```java
@Controller
@RequestMapping("/deanship")
@PreAuthorize("hasRole('DEANSHIP')")
public class DeanshipViewController {
    @GetMapping("/dashboard")
    public String dashboard() { return "deanship/dashboard"; }
    // ... other routes
}
```

**Frontend**: Shared layout managed by `DeanshipLayout` class:
```javascript
import { DeanshipLayout } from './deanship-common.js';

const layout = new DeanshipLayout();
await layout.initialize();

// Register callback for context changes
layout.onContextChange(() => loadData());
```

**State Persistence**: Academic context stored in localStorage:
- `deanship_selected_academic_year` - Selected academic year ID
- `deanship_selected_semester` - Selected semester ID
- `deanship_last_page` - Last visited page

#### Documentation

For detailed information about the deanship multi-page refactor:
- **Deployment Guide**: `DEANSHIP_DEPLOYMENT_GUIDE.md` - Comprehensive deployment, rollback, and troubleshooting guide
- **Developer Reference**: `DEANSHIP_DEVELOPER_REFERENCE.md` - Quick reference for developers
- **Design Document**: `.kiro/specs/deanship-multi-page-refactor/design.md` - Detailed architecture and design decisions
- **Requirements**: `.kiro/specs/deanship-multi-page-refactor/requirements.md` - Complete requirements specification

## 🔌 API Integration

### Authentication Flow
1. User submits login form
2. POST `/api/auth/login` with credentials
3. Backend returns JWT token and user info
4. Token stored in localStorage
5. All subsequent requests include `Authorization: Bearer {token}` header

### API Endpoints Used

#### Auth
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `GET /api/auth/me` - Get current user info

#### HOD
- `GET /api/hod/professors` - List professors
- `POST /api/hod/professors` - Create professor
- `PUT /api/hod/professors/{id}` - Update professor
- `DELETE /api/hod/professors/{id}` - Delete professor
- `GET /api/hod/requests` - List document requests
- `POST /api/hod/requests` - Create document request
- `GET /api/hod/report/{requestId}` - Get request report

#### Professor
- `GET /api/prof/requests` - List assigned requests
- `POST /api/prof/requests/{id}/submit` - Upload document (multipart)
- `GET /api/prof/notifications` - Get notifications
- `PUT /api/prof/notifications/{id}/seen` - Mark notification as seen

#### File Explorer
- `GET /api/file-explorer/root?academicYearId={id}&semesterId={id}` - Get root node
- `GET /api/file-explorer/node?path={path}` - Get node by path
- `GET /api/file-explorer/breadcrumbs?path={path}` - Get breadcrumb trail
- `GET /api/file-explorer/file/{fileId}/metadata` - Get file metadata
- `GET /api/file-explorer/file/{fileId}/download` - Download file

### Customizing API Endpoints

If your backend uses different endpoint paths, update the `js/api.js` file:

```javascript
// Change the base URL
const API_BASE_URL = 'http://your-server:port/api';

// Or modify individual endpoint paths in the exported objects
export const hod = {
    getProfessors: () => apiRequest('/your/custom/path', {
        method: 'GET',
    }),
    // ...
};
```

## ♿ Accessibility

The frontend follows accessibility best practices:

- ✅ Semantic HTML5 elements
- ✅ ARIA attributes for dynamic content
- ✅ Keyboard navigation support
- ✅ Focus management in modals
- ✅ Form labels and error messages
- ✅ Sufficient color contrast ratios
- ✅ Screen reader friendly alerts

### Accessibility Checklist

- [ ] All images have alt text (if applicable)
- [ ] Forms have proper labels
- [ ] Error messages are announced
- [ ] Modals trap focus
- [ ] Keyboard navigation works throughout
- [ ] Color is not the only indicator of status

## 📱 Responsive Design

The interface is fully responsive:
- **Desktop**: Multi-column layouts, tables
- **Tablet**: Adjusted grid columns
- **Mobile**: Stacked layouts, touch-friendly buttons

Breakpoints (Tailwind defaults):
- `sm`: 640px
- `md`: 768px
- `lg`: 1024px
- `xl`: 1280px

## 🧪 Testing

### Manual Testing Checklist

#### Login Flow
- [ ] Valid credentials log in successfully
- [ ] Invalid credentials show error message
- [ ] HOD redirected to HOD dashboard
- [ ] Professor redirected to Professor dashboard
- [ ] Token persists across page refreshes
- [ ] Logout clears token and redirects to login

#### HOD Features
- [ ] Professors list loads correctly
- [ ] Search filters professors
- [ ] Add professor form validates input
- [ ] Edit professor updates data
- [ ] Delete professor requires confirmation
- [ ] Create request form validates fields
- [ ] Request list shows correct statuses
- [ ] Report modal displays submission details

#### Professor Features
- [ ] Assigned requests load correctly
- [ ] Filters work (All, Pending, Submitted, Overdue)
- [ ] File upload validates extension
- [ ] File upload shows progress
- [ ] Large files rejected (>10MB)
- [ ] Notifications display correctly
- [ ] Notification badge updates

#### Cross-browser Testing
- [ ] Chrome/Edge (Chromium)
- [ ] Firefox
- [ ] Safari
- [ ] Mobile browsers

## 🎨 Customization

### Colors
The UI uses a neutral, official color palette. To customize:

Edit `css/custom.css` or use Tailwind classes:
- **Primary**: Blue (#3b82f6) - CTAs, links
- **Success**: Green - On-time submissions
- **Warning**: Yellow/Orange - Late submissions
- **Danger**: Red - Errors, overdue
- **Gray**: Neutral backgrounds and text

### Typography
Tailwind's default font stack is used. To change:

Add to `<head>` in HTML files:
```html
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
<style>
  body {
    font-family: 'Inter', sans-serif;
  }
</style>
```

## 🐛 Troubleshooting

### Issue: "Network error - Please check your connection"
- **Cause**: Backend not running or incorrect API_BASE_URL
- **Fix**: Ensure Spring Boot is running on port 8080, or update API_BASE_URL in `js/api.js`

### Issue: "Unauthorized - Please log in again"
- **Cause**: JWT token expired or invalid
- **Fix**: Log out and log in again. Check backend token expiration settings.

### Issue: "CORS error in browser console"
- **Cause**: Backend not configured to allow frontend origin
- **Fix**: Add `@CrossOrigin` annotation in backend controllers or configure global CORS policy

### Issue: File upload fails
- **Cause**: Backend not accepting multipart/form-data or file size limit
- **Fix**: Check backend configuration for `spring.servlet.multipart.max-file-size`

### Issue: Modals or toasts not appearing
- **Cause**: Missing container elements in HTML
- **Fix**: Ensure `<div id="modalsContainer"></div>` and `<div id="toastContainer"></div>` exist

## 📝 License

This project is part of the Al-Quds University Archiving System.

## 👥 Support

For issues or questions:
1. Check the troubleshooting section above
2. Review backend logs for API errors
3. Check browser console for frontend errors
4. Verify network requests in browser DevTools

---

**Al-Quds University — Archiving System**
