# 🗂️ Frontend Architecture

## Project Structure

```
ArchiveSystem/
│
├── src/main/resources/static/          # Frontend Root
│   ├── index.html                      # Entry point (Login)
│   ├── hod-dashboard.html              # HOD interface
│   ├── prof-dashboard.html             # Professor interface
│   ├── README.md                       # Technical documentation
│   │
│   ├── css/
│   │   └── custom.css                  # Supplementary styles
│   │
│   └── js/
│       ├── api.js                      # API Service Layer
│       ├── ui.js                       # UI Components
│       ├── auth.js                     # Login Page
│       ├── hod.js                      # HOD Dashboard
│       └── prof.js                     # Professor Dashboard
│
├── src/main/java/.../config/
│   └── SecurityConfig.java             # CORS & static resources ✅ Updated
│
├── DELIVERY_SUMMARY.md                 # This summary
├── FRONTEND_QUICKSTART.md              # Quick start guide
└── test-frontend.ps1                   # Test script
```

## Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                        Browser                              │
│  ┌────────────┐   ┌────────────┐   ┌────────────────────┐ │
│  │ index.html │   │   HOD      │   │    Professor       │ │
│  │  (Login)   │──>│ Dashboard  │   │    Dashboard       │ │
│  └────────────┘   └────────────┘   └────────────────────┘ │
│         │                 │                    │            │
│         └─────────────────┴────────────────────┘            │
│                           │                                 │
│                    ┌──────▼──────┐                          │
│                    │  JS Modules │                          │
│                    │             │                          │
│                    │  auth.js    │                          │
│                    │  hod.js     │                          │
│                    │  prof.js    │                          │
│                    └──────┬──────┘                          │
│                           │                                 │
│                    ┌──────▼──────┐                          │
│                    │   ui.js     │  (Modals, Toasts)       │
│                    └──────┬──────┘                          │
│                           │                                 │
│                    ┌──────▼──────┐                          │
│                    │   api.js    │  (HTTP + JWT)           │
│                    └──────┬──────┘                          │
│                           │                                 │
└───────────────────────────┼─────────────────────────────────┘
                            │
                    ┌───────▼────────┐
                    │   fetch() API  │
                    │   + JWT Token  │
                    └───────┬────────┘
                            │
            ┌───────────────▼────────────────┐
            │    Spring Boot Backend         │
            │                                │
            │  ┌──────────────────────────┐ │
            │  │   SecurityConfig         │ │
            │  │   (CORS + Auth)          │ │
            │  └────────────┬─────────────┘ │
            │               │                │
            │  ┌────────────▼─────────────┐ │
            │  │   REST Controllers       │ │
            │  │   /api/auth/**           │ │
            │  │   /api/hod/**            │ │
            │  │   /api/prof/**           │ │
            │  └────────────┬─────────────┘ │
            │               │                │
            │  ┌────────────▼─────────────┐ │
            │  │   Services & Repository  │ │
            │  └────────────┬─────────────┘ │
            │               │                │
            │  ┌────────────▼─────────────┐ │
            │  │   Database (H2/MySQL)    │ │
            │  └──────────────────────────┘ │
            └────────────────────────────────┘
```

## Authentication Flow

```
1. User enters credentials in index.html
                │
                ▼
2. auth.js validates input
                │
                ▼
3. api.js sends POST /api/auth/login
                │
                ▼
4. Backend validates & returns JWT + user info
                │
                ▼
5. api.js saves token to localStorage
                │
                ▼
6. Redirect based on role:
   - ROLE_HOD ──────────> hod-dashboard.html
   - ROLE_PROFESSOR ───> prof-dashboard.html
                │
                ▼
7. All subsequent API calls include:
   Authorization: Bearer {token}
```

## Module Responsibilities

### 📄 HTML Pages
- **index.html**: Login form, auth validation UI
- **hod-dashboard.html**: Professor & request management UI
- **prof-dashboard.html**: Request viewing & file upload UI

### 🎨 CSS
- **custom.css**: 
  - Animations (fade, slide, spin)
  - Badge styles
  - Focus states
  - Custom scrollbar
  - Loading skeletons

### 📜 JavaScript Modules

#### api.js (API Layer)
```javascript
Responsibilities:
- HTTP request wrapper (fetch)
- JWT token management
- Error handling (401, 403, network)
- File upload with progress
- Endpoint definitions

Exports:
- auth.login(credentials)
- hod.getProfessors()
- professor.submitDocument(id, file)
- getUserInfo()
- redirectToLogin()
```

#### ui.js (UI Layer)
```javascript
Responsibilities:
- Show/hide modals
- Toast notifications
- Date formatting
- File validation
- Debounce utility

Exports:
- showToast(message, type)
- showModal(title, content, options)
- showConfirm(title, message, callback)
- formatDate(date)
- isValidFileExtension(filename, allowed)
```

#### auth.js (Login Page)
```javascript
Responsibilities:
- Form validation
- Login submission
- Role-based redirect
- Error display

Uses:
- api.auth.login()
- ui.showToast()
```

#### hod.js (HOD Dashboard)
```javascript
Responsibilities:
- Load professors list
- CRUD operations for professors
- Create document requests
- View request reports
- Search/filter

Uses:
- api.hod.*
- ui.showModal()
- ui.showConfirm()
- ui.showToast()
```

#### prof.js (Professor Dashboard)
```javascript
Responsibilities:
- Load assigned requests
- Filter requests by status
- Upload documents
- Handle notifications
- Drag & drop file upload

Uses:
- api.professor.*
- ui.showModal()
- ui.showToast()
```

## State Management

### localStorage
```javascript
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userInfo": {
    "id": 1,
    "email": "hod@alquds.edu",
    "firstName": "John",
    "lastName": "Doe",
    "role": "ROLE_HOD",
    "fullName": "John Doe"
  }
}
```

### In-Memory State
Each page maintains:
- `professors[]` - List of professors (HOD)
- `requests[]` - List of requests (HOD/Prof)
- `notifications[]` - List of notifications (Prof)
- `currentPage` - Pagination state
- `currentFilter` - Active filter

## API Response Format

### Success Response
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}
```

### Error Response
```json
{
  "success": false,
  "message": "Error description",
  "error": "Detailed error"
}
```

## Security Features

### Client-Side
- ✅ JWT stored in localStorage (HttpOnly not possible)
- ✅ Token included in all API requests
- ✅ Auto-redirect on 401 (Unauthorized)
- ✅ Role-based page access checks
- ✅ Input validation before submission
- ✅ File type and size validation

### Server-Side (Backend)
- ✅ JWT validation on each request
- ✅ Role-based access control (@PreAuthorize)
- ✅ CORS configuration for frontend origin
- ✅ Password encryption (BCrypt)
- ✅ Request validation (@Valid)

## Performance Optimizations

- ✅ Tailwind via CDN (no build step)
- ✅ ES6 modules (browser-native)
- ✅ Debounced search (300ms delay)
- ✅ Pagination for large lists
- ✅ Loading skeletons for perceived speed
- ✅ Notification polling (30s interval, not on every render)

## Browser Compatibility

| Browser | Version | Status |
|---------|---------|--------|
| Chrome  | Latest  | ✅     |
| Firefox | Latest  | ✅     |
| Safari  | Latest  | ✅     |
| Edge    | Latest  | ✅     |

**Required Features:**
- ES6 Modules
- Fetch API
- LocalStorage
- Async/Await

## Deployment Checklist

- [ ] Update `API_BASE_URL` in `api.js` for production
- [ ] Configure production CORS in `SecurityConfig.java`
- [ ] Enable HTTPS for secure JWT transmission
- [ ] Set appropriate JWT expiration time
- [ ] Configure file size limits in backend
- [ ] Test all features in production environment
- [ ] Set up error logging/monitoring
- [ ] Configure CDN for Tailwind (optional)
- [ ] Add Content-Security-Policy headers
- [ ] Enable gzip compression for static assets

---

**Architecture designed for:**
- ✅ Maintainability (modular, documented)
- ✅ Scalability (stateless, API-driven)
- ✅ Security (JWT, validation)
- ✅ Accessibility (WCAG 2.1 AA)
- ✅ Performance (optimized, lazy-loaded)
