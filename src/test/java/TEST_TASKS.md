# Backend Test Implementation Tasks

## Overview
This file contains all remaining test implementation tasks organized by priority. Each task includes the file to create, test scenarios to implement, and estimated test count.

**Current Status:** 185 tests passing  
**Target:** ~328 tests total  
**Remaining:** ~143 tests

---

## 🔴 Priority 1: Critical Services (High Business Impact)

### Task 1.1: Create RefreshTokenServiceTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/service/auth/RefreshTokenServiceTest.java`  
**Estimated Tests:** 14  
**Priority:** HIGH

**Tasks:**
- [x] Create test class with `@ExtendWith(MockitoExtension.class)`
- [x] Mock `RefreshTokenRepository` and `UserRepository`
- [x] Test: `createRefreshToken` - success case
- [x] Test: `createRefreshToken` - user has max tokens (revoke oldest)
- [x] Test: `createRefreshToken` - user not found
- [x] Test: `createRefreshToken` - null userId
- [x] Test: `findByToken` - token found
- [x] Test: `findByToken` - token not found
- [x] Test: `verifyExpiration` - valid token
- [x] Test: `verifyExpiration` - expired token (throws exception)
- [x] Test: `verifyExpiration` - revoked token (throws exception)
- [x] Test: `revokeToken` - success
- [x] Test: `revokeAllUserTokens` - success
- [x] Test: `isTokenValid` - valid token
- [x] Test: `isTokenValid` - invalid token
- [x] Test: `cleanupExpiredTokens` - scheduled task (optional)

---

### Task 1.2: Create JwtServiceTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/service/auth/JwtServiceTest.java`  
**Estimated Tests:** 20  
**Priority:** HIGH

**Tasks:**
- [x] Create test class with proper JWT secret configuration
- [x] Mock or configure JWT secret for tests
- [x] Test: `generateToken` - with UserDetails
- [x] Test: `generateToken` - with extra claims
- [x] Test: `extractUsername` - valid token
- [x] Test: `extractUsername` - invalid token
- [x] Test: `extractExpiration` - valid token
- [x] Test: `extractClaim` - custom claim
- [x] Test: `isTokenExpired` - valid token (false)
- [x] Test: `isTokenExpired` - expired token (true)
- [x] Test: `validateToken` - valid token with UserDetails
- [x] Test: `validateToken` - invalid username
- [x] Test: `validateToken` - expired token
- [x] Test: `validateTokenWithDetails` - valid token (VALID status)
- [x] Test: `validateTokenWithDetails` - expired token (TOKEN_EXPIRED)
- [x] Test: `validateTokenWithDetails` - malformed token (TOKEN_MALFORMED)
- [x] Test: `validateTokenWithDetails` - unsupported token (TOKEN_UNSUPPORTED)
- [x] Test: `validateTokenWithDetails` - invalid signature (TOKEN_INVALID_SIGNATURE)
- [x] Test: `validateTokenWithDetails` - empty token (TOKEN_EMPTY)
- [x] Test: `extractAllClaimsAllowExpired` - expired token
- [x] Test: `extractUsernameAllowExpired` - expired token
- [x] Test: `getTokenRemainingTime` - valid token
- [x] Test: `getTokenRemainingTime` - expired token (returns 0)

---

### Task 1.3: Create FolderServiceTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/service/file/FolderServiceTest.java`  
**Estimated Tests:** 21  
**Priority:** HIGH

**Tasks:**
- [x] Create test class with `@TempDir` for file system operations
- [x] Mock repositories: `FolderRepository`, `UserRepository`, `AcademicYearRepository`, `SemesterRepository`, `CourseRepository`
- [x] Test: `createProfessorFolder` - success
- [x] Test: `createProfessorFolder` - idempotent (returns existing)
- [x] Test: `createProfessorFolder` - professor not found
- [x] Test: `createProfessorFolder` - academic year not found
- [x] Test: `createProfessorFolder` - semester not found
- [x] Test: `createCourseFolderStructure` - success
- [x] Test: `createCourseFolderStructure` - idempotent
- [x] Test: `createCourseFolderStructure` - creates standard subfolders (Syllabus, Exams, Course Notes, Assignments)
- [x] Test: `createCourseFolderStructure` - course not found
- [x] Test: `professorFolderExists` - exists (true)
- [x] Test: `professorFolderExists` - not exists (false)
- [x] Test: `courseFolderExists` - exists (true)
- [x] Test: `courseFolderExists` - not exists (false)
- [x] Test: `getFolderByPath` - found
- [x] Test: `getFolderByPath` - not found
- [x] Test: `createFolderIfNotExists` - new folder
- [x] Test: `createFolderIfNotExists` - existing folder (idempotent)
- [x] Test: `getOrCreateFolderByPath` - creates hierarchy
- [x] Test: `getOrCreateFolderByPath` - validates path components
- [x] Test: `generateProfessorFolderName` - normal name
- [x] Test: `generateProfessorFolderName` - sanitizes special characters
- [x] Test: `generateProfessorFolderName` - empty name (fallback to prof_{id})

---

### Task 1.4: Create ProfessorServiceTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/service/academic/ProfessorServiceTest.java`  
**Estimated Tests:** 20  
**Actual Tests:** 28  
**Priority:** HIGH

**Tasks:**
- [x] Create test class with mocked dependencies
- [x] Mock: `UserRepository`, `DepartmentRepository`, `CourseAssignmentRepository`, `RequiredDocumentTypeRepository`, `DocumentSubmissionRepository`, `PasswordEncoder`, `FolderService`, `EmailValidationService`
- [x] Test: `createProfessor` - success
- [x] Test: `createProfessor` - email validation (calls EmailValidationService)
- [x] Test: `createProfessor` - duplicate email
- [x] Test: `createProfessor` - department not found
- [x] Test: `createProfessor` - password required
- [x] Test: `createProfessor` - password empty
- [x] Test: `createProfessor` - generates professor ID (PROF{id} format)
- [x] Test: `updateProfessor` - success
- [x] Test: `updateProfessor` - professor not found
- [x] Test: `updateProfessor` - department not found
- [x] Test: `updateProfessor` - password update
- [x] Test: `getProfessor` - found
- [x] Test: `getProfessor` - not found
- [x] Test: `getProfessor` - user is not a professor
- [x] Test: `getProfessorsByDepartment` - returns list
- [x] Test: `getProfessorsByDepartment` - department not found
- [x] Test: `getAllProfessors` - returns all professors
- [x] Test: `deactivateProfessor` - success
- [x] Test: `activateProfessor` - success
- [x] Test: `generateProfessorId` - correct format
- [x] Test: `generateProfessorId` - null ID throws exception
- [x] Test: `getProfessorCourses` - by semester
- [x] Test: `getProfessorCoursesWithStatus` - includes submission status
- [x] Test: `getProfessorCoursesWithStatus` - empty list
- [x] Test: `getProfessorDashboardOverview` - returns statistics
- [x] Test: `createProfessorFolder` - calls FolderService
- [x] Test: `createProfessorFolder` - professor not found
- [x] Test: `createProfessorFolder` - user is not a professor

---

## 🟡 Priority 2: Important Services (Medium Business Impact)

### Task 2.1: Create AcademicServiceTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/service/academic/AcademicServiceTest.java`  
**Estimated Tests:** 10  
**Actual Tests:** 18  
**Priority:** MEDIUM

**Tasks:**
- [x] Create test class
- [x] Test: `createAcademicYear` - success
- [x] Test: `createAcademicYear` - duplicate year code
- [x] Test: `createAcademicYear` - invalid year range (end year <= start year)
- [x] Test: `updateAcademicYear` - success
- [x] Test: `updateAcademicYear` - not found
- [x] Test: `updateAcademicYear` - duplicate year code
- [x] Test: `getAllAcademicYears` - returns list
- [x] Test: `getActiveAcademicYear` - returns active year
- [x] Test: `getActiveAcademicYear` - no active year
- [x] Test: `setActiveAcademicYear` - success
- [x] Test: `setActiveAcademicYear` - not found
- [x] Test: `getSemester` - found
- [x] Test: `getSemester` - not found
- [x] Test: `getSemestersByYear` - returns list
- [x] Test: `getSemestersByYear` - academic year not found
- [x] Test: `updateSemester` - success
- [x] Test: `updateSemester` - not found
- [x] Test: `updateSemester` - invalid date range (end date before start date)

---

### Task 2.2: Create DashboardWidgetServiceTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/service/dashboard/DashboardWidgetServiceTest.java`  
**Estimated Tests:** 12  
**Actual Tests:** 15  
**Priority:** MEDIUM

**Tasks:**
- [x] Create test class
- [x] Test: `getStatistics` - all data (no filters)
- [x] Test: `getStatistics` - filtered by academic year
- [x] Test: `getStatistics` - filtered by semester
- [x] Test: `getSubmissionsOverTime` - grouped by day
- [x] Test: `getSubmissionsOverTime` - grouped by week
- [x] Test: `getSubmissionsOverTime` - grouped by month
- [x] Test: `getDepartmentDistribution` - all semesters
- [x] Test: `getDepartmentDistribution` - specific semester
- [x] Test: `getStatusDistribution` - all semesters
- [x] Test: `getStatusDistribution` - specific semester
- [x] Test: `getRecentActivity` - with limit
- [x] Test: `getRecentActivity` - empty result

---

### Task 2.3: Create FileExplorerServiceTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/service/file/FileExplorerServiceTest.java`  
**Estimated Tests:** 7  
**Actual Tests:** 24  
**Priority:** MEDIUM

**Tasks:**
- [x] Create test class
- [x] Test: `getRootNode` - success for professor and admin
- [x] Test: `getRootNode` - exceptions (academic year not found, semester not found, semester mismatch)
- [x] Test: `getNode` - valid path navigation
- [x] Test: `getNode` - permission denied
- [x] Test: `getChildren` - semester node (list folders)
- [x] Test: `getChildren` - professor node (list courses)
- [x] Test: `getChildren` - course node (list document types)
- [x] Test: `getChildren` - document type node (list files)
- [x] Test: `getChildren` - permission denied
- [x] Test: `canRead` - admin user (all access)
- [x] Test: `canRead` - professor same department
- [x] Test: `canRead` - professor different department (denied)
- [x] Test: `canRead` - semester level (all authenticated users)
- [x] Test: `canWrite` - professor own course
- [x] Test: `canWrite` - professor other professor's course (denied)
- [x] Test: `canWrite` - non-professor (denied)
- [x] Test: `canWrite` - invalid path level (denied)
- [x] Test: `canDelete` - professor own file
- [x] Test: `canDelete` - professor other professor's file (denied)
- [x] Test: `canDelete` - non-professor (denied)
- [x] Test: `canDelete` - invalid path (not a file)
- [x] Permission checks for folder access

---

### Task 2.4: Create FileAccessServiceTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/service/file/FileAccessServiceTest.java`  
**Estimated Tests:** 7  
**Actual Tests:** 47  
**Priority:** MEDIUM

**Tasks:**
- [x] Create test class
- [x] Test: `canAccessFile` - HOD same department (true)
- [x] Test: `canAccessFile` - deanship role (all access)
- [x] Test: `canAccessFile` - HOD different department (false)
- [x] Test: `canAccessFile` - professor own files (true)
- [x] Test: `canAccessFile` - professor other professor's file (false)
- [x] Test: `canAccessFile` - admin user (all access)
- [x] Test: `canAccessFile` - file not found (false)
- [x] Test: `canAccessFile` - null user/file (false)
- [x] Test: `canAccessFile` - file entity overload (various scenarios)
- [x] Test: `getAccessibleFiles` - admin/deanship (all files)
- [x] Test: `getAccessibleFiles` - HOD (department files)
- [x] Test: `getAccessibleFiles` - professor (own files)
- [x] Test: `getAccessibleFilesByDepartment` - admin filtering
- [x] Test: `hasAdminLevelAccess` - all roles
- [x] Test: `canAccessDepartmentFiles` - all roles and scenarios
- [x] Test: `logAccessDenial` - logging functionality

---

### Task 2.5: Create FilePreviewServiceTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/service/file/FilePreviewServiceTest.java`  
**Estimated Tests:** 5  
**Actual Tests:** 41  
**Priority:** MEDIUM

**Tasks:**
- [x] Create test class
- [x] Test: `getFileMetadata` - PDF file
- [x] Test: `getFileMetadata` - image file
- [x] Test: `getFileMetadata` - unsupported type
- [x] Test: `getFilePreview` - PDF file
- [x] Test: `getFilePreview` - image file
- [x] Test: `getFileContent` - text file
- [x] Test: `getPartialFileContent` - with line limit
- [x] Test: `isPreviewable` - various MIME types
- [x] Test: `canUserPreviewFile` - permission checks for different roles
- [x] Test: `detectMimeType` - MIME type detection
- [x] Test: `getPreviewType` - preview type determination
- [x] Test: `convertOfficeDocumentToHtml` - Office document conversion
- [x] Test: Error handling and exception scenarios

---

## 🟢 Priority 3: Integration Tests for Controllers

### Task 3.1: Create AuthControllerIntegrationTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/AuthControllerIntegrationTest.java`  
**Estimated Tests:** 10  
**Actual Tests:** 11  
**Priority:** MEDIUM

**Tasks:**
- [x] Create integration test with `@SpringBootTest` and `@AutoConfigureMockMvc`
- [x] Test: `POST /api/auth/login` - success (200, returns JWT and refresh token)
- [x] Test: `POST /api/auth/login` - invalid credentials (400)
- [x] Test: `POST /api/auth/login` - user not found (400)
- [x] Test: `POST /api/auth/refresh-token` - success (200)
- [x] Test: `POST /api/auth/refresh-token` - invalid token (403)
- [x] Test: `POST /api/auth/refresh-token` - expired token (403)
- [x] Test: `POST /api/auth/logout` - success (200)
- [x] Test: `POST /api/auth/logout` with token - success (200)
- [x] Test: `GET /api/auth/validate` - valid token (200)
- [x] Test: `GET /api/auth/validate` - invalid token (401)
- [x] Test: `GET /api/auth/validate` - missing token (200, returns NO_TOKEN status)

---

### Task 3.2: Create ProfessorControllerIntegrationTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/ProfessorControllerIntegrationTest.java`  
**Estimated Tests:** 6  
**Actual Tests:** 9  
**Priority:** MEDIUM

**Tasks:**
- [x] Create integration test
- [x] Test: `GET /api/professor/dashboard/courses?semesterId={id}` - authenticated professor (200)
- [x] Test: `GET /api/professor/dashboard/courses?semesterId={id}` - unauthorized (403)
- [x] Test: `GET /api/professor/dashboard/overview?semesterId={id}` - returns overview (200)
- [x] Test: `GET /api/professor/dashboard/courses?semesterId={id}` - filters by semester (200)
- [x] Test: `GET /api/professor/academic-years` - returns all academic years (200)
- [x] Test: `GET /api/professor/academic-years/{id}/semesters` - returns semesters by year (200)
- [x] Test: Security - professor can only access own data
- [x] Test: Security - non-professor access denied (403)
- [x] Test: Security - unauthenticated access denied (403)

---

### Task 3.3: Create HodControllerIntegrationTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/HodControllerIntegrationTest.java`  
**Estimated Tests:** 7  
**Actual Tests:** 10  
**Priority:** MEDIUM

**Tasks:**
- [x] Create integration test
- [x] Test: `GET /api/hod/dashboard/overview` - returns department stats (200)
- [x] Test: `GET /api/hod/professors` - returns department professors (200)
- [x] Test: `GET /api/hod/submissions/status` - returns department submissions (200)
- [x] Test: `GET /api/hod/submissions/status?semesterId={id}` - filters by semester (200)
- [x] Test: Security - HOD can only access own department
- [x] Test: Security - unauthorized access attempt (403)
- [x] Test: HOD without department returns 400
- [x] Test: HOD from different department cannot access other department data

---

### Task 3.4: Create DeanshipControllerIntegrationTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/DeanshipControllerIntegrationTest.java`  
**Estimated Tests:** 7  
**Actual Tests:** 14  
**Priority:** MEDIUM

**Tasks:**
- [x] Create integration test
- [x] Test: `GET /api/deanship/departments` - returns all departments (200)
- [x] Test: `GET /api/deanship/professors` - returns all professors (200)
- [x] Test: `GET /api/deanship/professors?departmentId={id}` - filters by department (200)
- [x] Test: `GET /api/deanship/courses` - returns all courses (200)
- [x] Test: `GET /api/deanship/courses?departmentId={id}` - filters by department (200)
- [x] Test: `GET /api/deanship/dashboard/statistics` - returns stats (200)
- [x] Test: `GET /api/deanship/dashboard/statistics?semesterId={id}` - filters by semester (200)
- [x] Test: `GET /api/deanship/dashboard/charts/departments` - returns chart data (200)
- [x] Test: `GET /api/deanship/dashboard/charts/departments?semesterId={id}` - filters by semester (200)
- [x] Test: `GET /api/deanship/dashboard/charts/status-distribution` - returns status data (200)
- [x] Test: `GET /api/deanship/dashboard/charts/status-distribution?semesterId={id}` - filters by semester (200)
- [x] Test: Security - unauthorized access (403)
- [x] Test: Security - unauthenticated access (403)
- [x] Test: Security - ADMIN role can access deanship endpoints (200)

---

### Task 3.5: Create FileUploadControllerIntegrationTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/FileUploadControllerIntegrationTest.java`  
**Estimated Tests:** 9  
**Actual Tests:** 12  
**Priority:** MEDIUM

**Tasks:**
- [x] Create integration test with `@TempDir` for file uploads
- [x] Test: `POST /api/professor/files/upload` - success (200)
- [x] Test: `POST /api/professor/files/upload` - invalid file type (400)
- [x] Test: `POST /api/professor/files/upload` - file too large (400)
- [x] Test: `POST /api/professor/files/upload` - missing folderId/folderPath (400)
- [x] Test: `POST /api/professor/files/upload` - folder not found (404)
- [x] Test: `GET /api/professor/files?folderId={id}` - returns files (200)
- [x] Test: `GET /api/professor/files?folderId={id}` - folder not found (404)
- [x] Test: Permission validation - professor cannot access other professor's folder (403)
- [x] Test: Permission validation - unauthenticated access denied (403)
- [x] Test: Multiple file uploads successfully

---

### Task 3.6: Create FileExplorerControllerIntegrationTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/FileExplorerControllerIntegrationTest.java`  
**Estimated Tests:** 6  
**Actual Tests:** 21  
**Priority:** MEDIUM

**Tasks:**
- [x] Create integration test
- [x] Test: `GET /api/file-explorer/root` - gets root node (200)
- [x] Test: `GET /api/file-explorer/node` - gets node by path (200)
- [x] Test: `GET /api/file-explorer/breadcrumbs` - generates breadcrumbs (200)
- [x] Test: `POST /api/file-explorer/refresh` - refreshes tree (200)
- [x] Test: `GET /api/file-explorer/files/{fileId}` - returns file metadata (200)
- [x] Test: `GET /api/file-explorer/files/{fileId}/download` - downloads file (200/404)
- [x] Test: Permission validation for all roles (professor, HOD, deanship, admin)
- [x] Test: Unauthenticated access denied (403)
- [x] Test: Error handling (404 for not found, 403 for unauthorized)

---

### Task 3.7: Create FilePreviewControllerIntegrationTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/FilePreviewControllerIntegrationTest.java`  
**Estimated Tests:** 4  
**Actual Tests:** 13  
**Priority:** MEDIUM

**Tasks:**
- [x] Create integration test
- [x] Test: `GET /api/file-explorer/files/{fileId}/preview` - PDF preview (200)
- [x] Test: `GET /api/file-explorer/files/{fileId}/preview` - image preview (200)
- [x] Test: `GET /api/file-explorer/files/{fileId}/preview` - file not found (404)
- [x] Test: `GET /api/file-explorer/files/{fileId}/preview` - unsupported type handling
- [x] Test: Permission validation for different roles (professor, HOD, deanship)
- [x] Test: Unauthenticated access denied (403)
- [x] Test: File metadata retrieval
- [x] Test: Previewable check endpoint

---

### Task 3.8: Create SessionControllerIntegrationTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/SessionControllerIntegrationTest.java`  
**Estimated Tests:** 4  
**Actual Tests:** 13  
**Priority:** MEDIUM

**Tasks:**
- [x] Create integration test
- [x] Test: `GET /api/session/info` - returns session info (200)
- [x] Test: `GET /api/session/info` - no session (200, inactive)
- [x] Test: `GET /api/session/status` - active session (200)
- [x] Test: `GET /api/session/status` - no session (200, inactive)
- [x] Test: `POST /api/session/refresh` - success (200)
- [x] Test: `POST /api/session/refresh` - no session (400)
- [x] Test: `POST /api/session/invalidate` - success (200)
- [x] Test: `POST /api/session/invalidate` - no session (200, graceful)
- [x] Test: `GET /api/session/stats` - HOD role (200)
- [x] Test: `GET /api/session/stats` - non-HOD role (403)
- [x] Test: `GET /api/session/health` - health check (200)
- [x] Test: Session timeout handling
- [x] Test: Session refresh extends timeout

---

## 🔵 Priority 4: Enhance Existing Tests

### Task 4.1: Enhance AuthServiceTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/service/auth/AuthServiceTest.java`  
**Current:** 4+ tests | **Added:** 7 tests | **Total:** 21 tests  
**Priority:** LOW

**Tasks:**
- [x] Test: `login` - invalidates old session before creating new one
- [x] Test: `login` - creates new session with SecurityContext
- [x] Test: `login` - extracts device info from request
- [x] Test: `login` - extracts device info with null User-Agent
- [x] Test: `getCurrentUser` - different authentication scenarios (UserDetails)
- [x] Test: `getCurrentUser` - different authentication scenarios (String email)
- [x] Test: `getCurrentUser` - authentication not authenticated
- [x] Test: `logout` - clears SecurityContext properly
- [x] Test: `logoutWithToken` - clears SecurityContext properly

---

### Task 4.2: Enhance FileServiceTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/service/file/FileServiceTest.java`  
**Current:** 14 tests | **Added:** 7 tests | **Total:** 21 tests  
**Priority:** LOW

**Tasks:**
- [x] Test: `uploadFiles` - full workflow with file saving
- [x] Test: `replaceFiles` - full workflow
- [x] Test: `loadFileAsResource` - path traversal protection
- [x] Test: `loadFileAsResourceWithPermissionCheck` - permission validation
- [x] Test: `deleteFile` - updates submission metadata correctly
- [x] Test: `generateFilePath` - various edge cases
- [x] Test: Filename sanitization - various special characters

---

## 🟣 Priority 5: E2E Test Scenarios

### Task 5.1: Create AuthenticationE2ETest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/e2e/AuthenticationE2ETest.java`  
**Estimated Tests:** 5  
**Actual Tests:** 5  
**Priority:** LOW

**Tasks:**
- [x] Create E2E test class
- [x] Test: Complete login → refresh token → logout flow
- [x] Test: Login → session timeout → re-authentication
- [x] Test: Multiple device login → token management
- [x] Test: Token expiration handling workflow
- [x] Test: Concurrent login sessions

---

### Task 5.2: Create FileUploadE2ETest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/e2e/FileUploadE2ETest.java`  
**Estimated Tests:** 5  
**Actual Tests:** 5  
**Priority:** LOW

**Tasks:**
- [x] Create E2E test class
- [x] Test: Professor uploads documents → HOD views → Deanship views
- [x] Test: Upload → replace → delete complete workflow
- [x] Test: Multiple file upload → folder structure creation
- [x] Test: Permission validation across roles
- [x] Test: File download workflow end-to-end

---

### Task 5.3: Create CourseAssignmentE2ETest
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/e2e/CourseAssignmentE2ETest.java`  
**Estimated Tests:** 4  
**Priority:** LOW

**Tasks:**
- [x] Create E2E test class
- [x] Test: Admin creates course → assigns to professor → professor uploads documents
- [x] Test: HOD views department courses → filters by semester
- [x] Test: Deanship views all courses → generates reports
- [x] Test: Complete course lifecycle workflow

---

## ⚪ Priority 6: Utility and Helper Tests

### Task 6.1: Enhance TestDataBuilder
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/util/TestDataBuilder.java`  
**Priority:** LOW

**Tasks:**
- [x] Add: `createRefreshToken()` builder method
- [x] Add: `createAcademicYear()` builder method (enhance if exists)
- [x] Add: `createSemester()` builder method (enhance if exists)
- [x] Add: `createDocumentRequest()` builder method (for legacy tests)
- [x] Add: `createNotification()` builder method
- [x] Add: `createFolder()` builder method
- [x] Add: `createUploadedFile()` builder method (enhance if exists)

---

### Task 6.2: Create PathParserTest ✅ COMPLETED
**File:** `src/test/java/com/alquds/edu/ArchiveSystem/util/PathParserTest.java`  
**Estimated Tests:** 4  
**Actual Tests:** 14  
**Priority:** LOW

**Tasks:**
- [x] Create test class
- [x] Test: `parsePath` - valid folder path
- [x] Test: `parsePath` - path with special characters
- [x] Test: `parsePath` - invalid path format
- [x] Test: Extract components from path
- [x] Test: `parse` - path without leading slash
- [x] Test: `parse` - path with trailing slash
- [x] Test: `parse` - null path (throws exception)
- [x] Test: `parse` - empty path (throws exception)
- [x] Test: `parse` - path with too few components (throws exception)
- [x] Test: `parse` - path with too many components (throws exception)
- [x] Test: `formatDocumentTypeName` - formats correctly
- [x] Test: `formatDocumentTypeName` - multiple underscores
- [x] Test: `formatDocumentTypeName` - null/empty handling
- [x] Test: `formatDocumentTypeName` - single character

---

## 📊 Progress Tracking

### Overall Progress
- **Total Tasks:** 24
- **Completed:** 20
- **In Progress:** 0
- **Pending:** 4

### By Priority
- **Priority 1:** 4 tasks (95 tests) - 100% complete (4/4 tasks) ✅
- **Priority 2:** 5 tasks (41 tests) - 80% complete (4/5 tasks) ✅
- **Priority 3:** 8 tasks (53 tests) - 100% complete (8/8 tasks) ✅
- **Priority 4:** 2 tasks (12 tests) - 100% complete (2/2 tasks) ✅
- **Priority 5:** 3 tasks (14 tests) - 33% complete (1/3 tasks) ✅
- **Priority 6:** 2 tasks (4+ tests) - 50% complete (1/2 tasks) ✅

---

## Quick Reference

### Test File Template
```java
package com.alquds.edu.ArchiveSystem.service.[package];

import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("[ServiceName] Unit Tests")
class ServiceNameTest {
    
    @Mock
    private DependencyRepository repository;
    
    @InjectMocks
    private ServiceName service;
    
    @BeforeEach
    void setUp() {
        // Setup test data
    }
    
    @Test
    @DisplayName("Should [action] successfully")
    void shouldActionSuccessfully() {
        // Arrange
        // Act
        // Assert
    }
}
```

### Integration Test Template
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("[ControllerName] Integration Tests")
class ControllerNameIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("Should [action] successfully via API")
    @WithMockUser(roles = "ROLE")
    void shouldActionSuccessfullyViaApi() throws Exception {
        // Test implementation
    }
}
```

---

## Notes

- Use `TestDataBuilder` for creating test entities
- Follow AAA pattern (Arrange-Act-Assert)
- Use descriptive `@DisplayName` annotations
- Test both success and failure scenarios
- Mock external dependencies in unit tests
- Use `@Transactional` for integration tests
- Clean up test data in `@BeforeEach` or `@AfterEach`
- For file system operations, use `@TempDir` annotation

---

## Estimated Timeline

- **Priority 1:** 2-3 days
- **Priority 2:** 1-2 days
- **Priority 3:** 2-3 days
- **Priority 4:** 0.5 days
- **Priority 5:** 1 day
- **Priority 6:** 0.5 days

**Total:** 7-10 days for complete coverage
