# 🏗️ University Archive System - Complete Architecture

## 📋 Table of Contents
1. [Technology Stack](#technology-stack)
2. [Backend Architecture](#backend-architecture)
3. [Frontend Architecture](#frontend-architecture)
4. [Security Architecture](#security-architecture)
5. [Development Standards](#development-standards)
6. [Deployment](#deployment)

---

## 🛠️ Technology Stack

### Backend
- **Framework:** Spring Boot 3.5.7
- **Language:** Java 17
- **Database:** MySQL 8.0 (H2 for testing)
- **Security:** Spring Security 6.x, JWT (io.jsonwebtoken 0.11.5)
- **Session:** Spring Session JDBC
- **Migration:** Flyway
- **Mapping:** MapStruct 1.5.5
- **Utilities:** Lombok, iText7 (PDF generation)
- **Build Tool:** Maven

### Frontend
- **Framework:** Vanilla JavaScript (ES6 Modules)
- **Styling:** Tailwind CSS 3.x (CDN)
- **Icons:** Lucide Icons
- **Build:** None required (browser-native modules)

### Infrastructure
- **Server:** Embedded Tomcat (Spring Boot)
- **File Storage:** Local filesystem (uploads/)
- **Logging:** SLF4J + Logback

---

## 🏛️ Backend Architecture

### Package Structure

```
com.alqude.edu.ArchiveSystem/
│
├── 📁 config/                      # Configuration Classes
│   ├── SecurityConfig.java         # Security filter chain, CORS, auth provider
│   ├── JwtAuthenticationFilter.java # JWT token validation filter
│   ├── PasswordConfig.java         # BCrypt password encoder bean
│   ├── SessionConfig.java          # Session management configuration
│   ├── MapperConfig.java           # MapStruct component model
│   └── DataInitializer.java        # Initial data seeding (HOD, departments)
│
├── 📁 controller/                  # REST API Controllers (@RestController)
│   ├── AuthController.java         # POST /api/auth/login, /logout, GET /me
│   ├── HodController.java          # /api/hod/** (professors, requests, reports)
│   ├── ProfessorController.java    # /api/professor/** (requests, submissions)
│   └── SessionController.java      # /api/session/** (session info)
│
├── 📁 service/                     # Business Logic Layer (@Service)
│   ├── AuthService.java            # Authentication, session rotation
│   ├── UserService.java            # User CRUD, UserDetailsService impl
│   ├── DocumentRequestService.java # Request lifecycle management
│   ├── FileStorageService.java     # File upload/download/deletion
│   ├── NotificationService.java    # Notification creation & retrieval
│   ├── ReportService.java          # PDF report generation (iText7)
│   └── JwtService.java             # JWT token generation & validation
│
├── 📁 repository/                  # Data Access Layer (Spring Data JPA)
│   ├── UserRepository.java
│   ├── DocumentRequestRepository.java
│   ├── DepartmentRepository.java
│   ├── NotificationRepository.java
│   ├── SubmittedDocumentRepository.java
│   └── FileAttachmentRepository.java
│
├── 📁 entity/                      # JPA Entities (@Entity)
│   ├── User.java                   # Implements UserDetails
│   ├── DocumentRequest.java        # Request entity with status enum
│   ├── SubmittedDocument.java      # Professor submissions
│   ├── FileAttachment.java         # File metadata
│   ├── Notification.java           # User notifications
│   ├── Department.java             # Academic departments
│   └── Role.java                   # Enum: ROLE_HOD, ROLE_PROFESSOR
│
├── 📁 dto/                         # Data Transfer Objects
│   ├── auth/
│   │   ├── LoginRequest.java       # Email + password
│   │   └── JwtResponse.java        # JWT token + user info
│   ├── user/
│   │   ├── UserCreateRequest.java  # @Valid annotations
│   │   ├── UserUpdateRequest.java
│   │   └── UserResponse.java       # Public user data
│   ├── request/
│   │   ├── DocumentRequestCreateRequest.java
│   │   └── DocumentRequestResponse.java
│   ├── common/
│   │   ├── ApiResponse.java        # Generic wrapper <T>
│   │   ├── ErrorResponse.java
│   │   ├── NotificationResponse.java
│   │   ├── FileAttachmentResponse.java
│   │   └── SubmittedDocumentResponse.java
│   └── report/
│       ├── DepartmentSubmissionReport.java
│       └── ProfessorSubmissionSummary.java
│
├── 📁 mapper/                      # MapStruct Mappers (@Mapper)
│   ├── UserMapper.java             # Entity ↔ DTO conversion
│   └── DocumentRequestMapper.java
│
├── 📁 exception/                   # Exception Handling
│   ├── GlobalExceptionHandler.java # @RestControllerAdvice
│   ├── BusinessException.java      # Base business exception
│   ├── EntityNotFoundException.java
│   ├── DuplicateEntityException.java
│   ├── UnauthorizedOperationException.java
│   ├── ValidationException.java
│   ├── UserException.java
│   ├── DocumentRequestException.java
│   └── FileUploadException.java
│
└── 📁 util/                        # Utility Classes
```

### Design Patterns

#### 1. Layered Architecture
```
┌─────────────────────────────────────────┐
│         Controller Layer                │  ← REST endpoints, request validation
│  (@RestController, @RequestMapping)     │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│         Service Layer                   │  ← Business logic, transactions
│  (@Service, @Transactional)            │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│         Repository Layer                │  ← Data access, queries
│  (Spring Data JPA)                      │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│         Entity Layer                    │  ← Domain models, JPA entities
│  (@Entity, @Table)                      │
└─────────────────────────────────────────┘
```

#### 2. DTO Pattern
- **Purpose:** Separate API contracts from domain models
- **Implementation:** MapStruct for automatic mapping
- **Benefits:** API stability, security (hide sensitive fields), validation

#### 3. Repository Pattern
- **Implementation:** Spring Data JPA interfaces
- **Custom Queries:** @Query annotations, method naming conventions
- **Example:** `Optional<User> findByEmail(String email)`

#### 4. Dependency Injection
- **Method:** Constructor injection with `@RequiredArgsConstructor` (Lombok)
- **Avoids:** Field injection, circular dependencies
- **Example:**
  ```java
  @Service
  @RequiredArgsConstructor
  public class UserService {
      private final UserRepository userRepository;
      private final PasswordEncoder passwordEncoder;
  }
  ```

#### 5. Exception Handling
- **Centralized:** `@RestControllerAdvice` in GlobalExceptionHandler
- **Custom Exceptions:** Extend BusinessException
- **Response Format:** Always returns `ApiResponse<T>`

#### 6. Session Rotation (Security)
- **When:** On successful login
- **Process:**
  1. Invalidate old session (if exists)
  2. Authenticate user
  3. Create new session with new ID
  4. Store authentication in new session
- **Prevents:** Session fixation attacks

### API Endpoint Structure

#### Authentication Endpoints
```
POST   /api/auth/login      # Public - returns JWT token
POST   /api/auth/logout     # Authenticated - invalidates session
GET    /api/auth/me         # Authenticated - returns current user
```

#### HOD Endpoints (ROLE_HOD required)
```
GET    /api/hod/professors                    # List all professors
POST   /api/hod/professors                    # Create professor
PUT    /api/hod/professors/{id}               # Update professor
DELETE /api/hod/professors/{id}               # Delete professor
POST   /api/hod/professors/{id}/toggle-status # Activate/deactivate

GET    /api/hod/requests                      # List all requests
POST   /api/hod/requests                      # Create request
GET    /api/hod/requests/{id}                 # Get request details
GET    /api/hod/requests/{id}/submissions     # Get submissions

GET    /api/hod/reports/department            # Department report (PDF)
GET    /api/hod/reports/professor/{id}        # Professor report (PDF)
```

#### Professor Endpoints (ROLE_PROFESSOR required)
```
GET    /api/professor/requests                # My assigned requests
GET    /api/professor/requests/{id}           # Request details
POST   /api/professor/requests/{id}/submit    # Submit document
GET    /api/professor/notifications           # My notifications
PUT    /api/professor/notifications/{id}/read # Mark as read
```

### API Response Format

All endpoints return a standardized response:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    // Response payload
  }
}
```

Error response:
```json
{
  "success": false,
  "message": "Error description",
  "error": "Detailed error message"
}
```

---

# 🎨 Frontend Architecture

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

## 🔒 Security Architecture

### Authentication Flow

```
1. User submits credentials (email + password)
                │
                ▼
2. AuthController receives LoginRequest
                │
                ▼
3. AuthService.login() performs:
   a. Invalidate old session (if exists)
   b. Authenticate via AuthenticationManager
   c. Create new session (session rotation)
   d. Generate JWT token
                │
                ▼
4. Return JwtResponse (token + user info)
                │
                ▼
5. Frontend stores token in localStorage
                │
                ▼
6. All subsequent requests include:
   Authorization: Bearer {token}
                │
                ▼
7. JwtAuthenticationFilter validates token
                │
                ▼
8. SecurityContextHolder stores authentication
                │
                ▼
9. Controller method executes with @PreAuthorize check
```

### Security Features

#### Backend Security
- ✅ **JWT Authentication:** Stateless token-based auth
- ✅ **BCrypt Password Hashing:** Secure password storage
- ✅ **Role-Based Access Control (RBAC):** Method-level with @PreAuthorize
- ✅ **Session Rotation:** Prevents session fixation attacks
- ✅ **CORS Configuration:** Restricts allowed origins
- ✅ **Input Validation:** @Valid annotations on DTOs
- ✅ **SQL Injection Prevention:** JPA parameterized queries
- ✅ **File Upload Validation:** Extension, size, MIME type checks
- ✅ **Exception Sanitization:** No sensitive data in error responses

#### Frontend Security
- ✅ **JWT Storage:** localStorage (HttpOnly not possible with CDN approach)
- ✅ **Automatic Token Injection:** Authorization header on all API calls
- ✅ **401 Handling:** Auto-redirect to login on unauthorized
- ✅ **Role-Based UI:** Show/hide features based on user role
- ✅ **Input Validation:** Client-side validation before submission
- ✅ **File Validation:** Extension and size checks before upload
- ✅ **XSS Prevention:** Sanitize user input in UI

### Security Configuration

#### JWT Configuration
```properties
jwt.secret=mySecretKey123456789012345678901234567890
jwt.expiration=86400000  # 24 hours in milliseconds
```

#### CORS Configuration
```java
// Development: Allow all origins
configuration.setAllowedOriginPatterns(Arrays.asList("*"));

// Production: Restrict to specific domain
configuration.setAllowedOrigins(Arrays.asList("https://yourdomain.com"));
```

#### File Upload Limits
```properties
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
file.upload.directory=uploads/
```

### Circular Dependency Resolution

**Problem:** SecurityConfig → PasswordEncoder → UserService → SecurityConfig

**Solution:**
1. Created separate `PasswordConfig.java` for PasswordEncoder bean
2. Removed constructor injection in SecurityConfig
3. Used method parameter injection in @Bean methods
4. Result: Clean dependency graph, no circular references

---

## 📝 Development Standards

### Backend Coding Standards

#### Naming Conventions
- **Classes:** PascalCase (e.g., `UserService`, `DocumentRequestController`)
- **Methods:** camelCase (e.g., `getUserById`, `createDocumentRequest`)
- **Constants:** UPPER_SNAKE_CASE (e.g., `MAX_FILE_SIZE`, `API_BASE_URL`)
- **Packages:** lowercase (e.g., `com.alqude.edu.ArchiveSystem.service`)
- **DTOs:** Suffix with purpose (e.g., `UserCreateRequest`, `UserResponse`)

#### Annotations Best Practices
```java
// Lombok
@Data                    // Getters, setters, toString, equals, hashCode
@RequiredArgsConstructor // Constructor for final fields
@Slf4j                   // Logger instance
@ToString.Exclude        // Exclude from toString (prevent lazy loading)

// Validation
@Valid                   // Enable validation on method parameters
@NotNull, @NotBlank      // Field validation
@Email, @Size            // Format validation

// Security
@PreAuthorize("hasRole('ROLE_HOD')")
@PreAuthorize("hasAnyRole('ROLE_HOD', 'ROLE_PROFESSOR')")

// JPA
@Entity, @Table          // Entity mapping
@ManyToOne(fetch = FetchType.LAZY)  // Lazy loading for performance
@JsonIgnoreProperties    // Prevent circular serialization
```

#### Error Handling Pattern
```java
// Service layer - throw custom exceptions
public User getUserById(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
}

// Controller layer - return ApiResponse
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
    UserResponse user = userService.getUserById(id);
    return ResponseEntity.ok(ApiResponse.success("User retrieved", user));
}

// GlobalExceptionHandler catches all exceptions
@ExceptionHandler(EntityNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(ex.getMessage()));
}
```

#### Logging Standards
```java
@Slf4j
public class UserService {
    public User createUser(UserCreateRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        try {
            // Business logic
            log.debug("User created successfully: {}", user.getId());
            return user;
        } catch (Exception e) {
            log.error("Failed to create user: {}", request.getEmail(), e);
            throw e;
        }
    }
}
```

### Frontend Coding Standards

#### Module Pattern
```javascript
// api.js - Export API functions
export const api = {
    auth: {
        login: async (credentials) => { /* ... */ }
    },
    hod: {
        getProfessors: async () => { /* ... */ }
    }
};

// hod.js - Import and use
import { api } from './api.js';
import { ui } from './ui.js';

const professors = await api.hod.getProfessors();
ui.showToast('Professors loaded', 'success');
```

#### Async/Await Pattern
```javascript
// ✅ Good - async/await with try-catch
async function loadProfessors() {
    try {
        const response = await api.hod.getProfessors();
        if (response.success) {
            displayProfessors(response.data);
        }
    } catch (error) {
        ui.showToast('Failed to load professors', 'error');
        console.error(error);
    }
}

// ❌ Bad - .then() chains
api.hod.getProfessors()
    .then(response => displayProfessors(response.data))
    .catch(error => console.error(error));
```

#### Error Handling
```javascript
// API layer - handle HTTP errors
const response = await fetch(url, options);
if (response.status === 401) {
    redirectToLogin();
    throw new Error('Unauthorized');
}
if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
}

// UI layer - show user-friendly messages
catch (error) {
    if (error.message.includes('401')) {
        ui.showToast('Session expired. Please login again.', 'warning');
    } else {
        ui.showToast('An error occurred. Please try again.', 'error');
    }
}
```

### Testing Standards

#### Backend Testing
```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    @WithMockUser(roles = "HOD")
    void testGetUser() throws Exception {
        // Given
        UserResponse user = new UserResponse(1L, "test@example.com", ...);
        when(userService.getUserById(1L)).thenReturn(user);
        
        // When & Then
        mockMvc.perform(get("/api/hod/professors/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }
}
```

#### Frontend Testing (Manual Checklist)
- [ ] Login with valid credentials (HOD and Professor)
- [ ] Login with invalid credentials (error message)
- [ ] Token expiration handling (401 redirect)
- [ ] CRUD operations for professors (HOD)
- [ ] Document request creation (HOD)
- [ ] Document submission (Professor)
- [ ] File upload validation (size, extension)
- [ ] Notification polling (Professor)
- [ ] Search and filter functionality
- [ ] Responsive design (mobile, tablet, desktop)
- [ ] Browser compatibility (Chrome, Firefox, Safari, Edge)

### Git Workflow

#### Branch Naming
- `feature/feature-name` - New features
- `bugfix/bug-description` - Bug fixes
- `hotfix/critical-issue` - Production hotfixes
- `refactor/component-name` - Code refactoring
- `docs/documentation-update` - Documentation changes

#### Commit Messages
```
# Format: <type>: <description>

# Types:
feat: Add professor submission report endpoint
fix: Resolve circular dependency in SecurityConfig
refactor: Extract password encoder to separate config
docs: Update architecture documentation
test: Add unit tests for UserService
chore: Update dependencies to latest versions

# Bad examples:
❌ "Fixed bug"
❌ "Updated code"
❌ "WIP"

# Good examples:
✅ "feat: Add PDF report generation for department submissions"
✅ "fix: Prevent session fixation with session rotation on login"
✅ "refactor: Use MapStruct for entity-DTO conversion"
```

---

## 🚀 Deployment

### Pre-Deployment Checklist

#### Backend
- [ ] Update `jwt.secret` to strong random value (64+ characters)
- [ ] Set `spring.profiles.active=prod` in environment
- [ ] Configure production database credentials
- [ ] Enable Flyway migrations (`spring.flyway.enabled=true`)
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate` (not update/create)
- [ ] Configure CORS for production domain only
- [ ] Set appropriate `jwt.expiration` (e.g., 1 hour for high security)
- [ ] Enable HTTPS (TLS 1.2+)
- [ ] Configure file upload directory with proper permissions
- [ ] Set up log rotation and monitoring
- [ ] Review and update `application-prod.properties`

#### Frontend
- [ ] Update `API_BASE_URL` in `api.js` to production backend URL
- [ ] Test all features in production-like environment
- [ ] Verify CORS configuration allows frontend domain
- [ ] Test file upload with production file size limits
- [ ] Verify JWT token expiration handling
- [ ] Test on all supported browsers
- [ ] Verify responsive design on mobile devices
- [ ] Add Content-Security-Policy headers
- [ ] Enable gzip compression for static assets
- [ ] Set up CDN for Tailwind CSS (or self-host for reliability)

#### Security
- [ ] Change default HOD password immediately after deployment
- [ ] Enable HTTPS for all endpoints
- [ ] Configure firewall rules (allow only necessary ports)
- [ ] Set up rate limiting for API endpoints
- [ ] Enable SQL query logging for audit trail
- [ ] Configure session timeout appropriately
- [ ] Review and test all @PreAuthorize annotations
- [ ] Verify file upload security (extension whitelist, virus scanning)

#### Infrastructure
- [ ] Set up database backups (daily recommended)
- [ ] Configure application monitoring (e.g., Spring Boot Actuator)
- [ ] Set up error alerting (e.g., email on critical errors)
- [ ] Configure log aggregation (e.g., ELK stack)
- [ ] Set up health check endpoint
- [ ] Configure auto-restart on failure
- [ ] Document server specifications and scaling plan

### Environment Configuration

#### Development
```properties
spring.profiles.active=dev
spring.jpa.show-sql=true
spring.flyway.enabled=false
jwt.expiration=86400000  # 24 hours
```

#### Production
```properties
spring.profiles.active=prod
spring.jpa.show-sql=false
spring.flyway.enabled=true
jwt.expiration=3600000  # 1 hour
logging.level.root=WARN
logging.level.com.alqude.edu.ArchiveSystem=INFO
```

### Deployment Commands

```bash
# Build application
mvn clean package -DskipTests

# Run with production profile
java -jar target/ArchiveSystem-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# Or use Maven
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

---

**Architecture designed for:**
- ✅ **Maintainability:** Modular design, clear separation of concerns, comprehensive documentation
- ✅ **Scalability:** Stateless architecture, API-driven, connection pooling, lazy loading
- ✅ **Security:** JWT authentication, RBAC, session rotation, input validation, secure defaults
- ✅ **Accessibility:** WCAG 2.1 AA compliance, semantic HTML, keyboard navigation
- ✅ **Performance:** Lazy loading, pagination, debounced search, optimized queries, CDN
- ✅ **Reliability:** Exception handling, logging, session management, data validation
- ✅ **Testability:** Dependency injection, mock-friendly design, clear interfaces
- ✅ **Production-Ready:** Configuration management, deployment checklist, monitoring hooks
