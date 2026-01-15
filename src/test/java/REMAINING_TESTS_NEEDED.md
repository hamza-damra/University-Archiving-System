# Remaining Tests Needed for Backend

## Overview
This document outlines all remaining tests needed to achieve comprehensive test coverage for the University Archiving System backend. Tests are organized by priority and type.

**Current Status:** 129 tests passing (Unit: ~109, Integration: 8, E2E: 3)

---

## Priority 1: Critical Services (High Business Impact)

### 1. RefreshTokenService Tests ⚠️ **HIGH PRIORITY**
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/service/auth/RefreshTokenServiceTest.java`

**Test Scenarios:**
- ✅ Create refresh token successfully
- ✅ Create refresh token when user has max tokens (should revoke oldest)
- ✅ Find refresh token by token string
- ✅ Verify expiration - valid token
- ✅ Verify expiration - expired token (should throw exception)
- ✅ Verify expiration - revoked token (should throw exception)
- ✅ Revoke single token
- ✅ Revoke all user tokens
- ✅ Check token validity - valid token
- ✅ Check token validity - invalid token
- ✅ Cleanup expired tokens (scheduled task)
- ✅ Cleanup revoked tokens (scheduled task)
- ✅ Throw exception when user not found
- ✅ Handle null userId gracefully

**Estimated Tests:** 14

---

### 2. JwtService Tests ⚠️ **HIGH PRIORITY**
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/service/auth/JwtServiceTest.java`

**Test Scenarios:**
- ✅ Generate token with UserDetails
- ✅ Generate token with extra claims
- ✅ Extract username from token
- ✅ Extract expiration from token
- ✅ Extract custom claim from token
- ✅ Check if token is expired - valid token
- ✅ Check if token is expired - expired token
- ✅ Validate token with UserDetails - valid
- ✅ Validate token with UserDetails - invalid username
- ✅ Validate token with UserDetails - expired token
- ✅ Validate token with details - valid token
- ✅ Validate token with details - expired token
- ✅ Validate token with details - malformed token
- ✅ Validate token with details - unsupported token
- ✅ Validate token with details - invalid signature
- ✅ Validate token with details - empty token
- ✅ Extract claims from expired token (allow expired)
- ✅ Extract username from expired token (allow expired)
- ✅ Get token remaining time - valid token
- ✅ Get token remaining time - expired token

**Estimated Tests:** 20

---

### 3. FolderService Tests ⚠️ **HIGH PRIORITY**
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/service/file/FolderServiceTest.java`

**Test Scenarios:**
- ✅ Create professor folder successfully
- ✅ Create professor folder - idempotent (returns existing)
- ✅ Create professor folder - professor not found
- ✅ Create professor folder - academic year not found
- ✅ Create professor folder - semester not found
- ✅ Create course folder structure successfully
- ✅ Create course folder structure - idempotent
- ✅ Create course folder structure - creates standard subfolders
- ✅ Check professor folder exists - exists
- ✅ Check professor folder exists - not exists
- ✅ Check course folder exists - exists
- ✅ Check course folder exists - not exists
- ✅ Get folder by path - found
- ✅ Get folder by path - not found
- ✅ Create folder if not exists - new folder
- ✅ Create folder if not exists - existing folder (idempotent)
- ✅ Get or create folder by path - creates hierarchy
- ✅ Get or create folder by path - validates path components
- ✅ Generate professor folder name - normal name
- ✅ Generate professor folder name - sanitizes special characters
- ✅ Generate professor folder name - empty name (fallback)

**Estimated Tests:** 21

---

### 4. ProfessorService Tests ⚠️ **HIGH PRIORITY**
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/service/academic/ProfessorServiceTest.java`

**Test Scenarios:**
- ✅ Create professor successfully
- ✅ Create professor - email validation
- ✅ Create professor - duplicate email
- ✅ Create professor - department not found
- ✅ Create professor - password required
- ✅ Create professor - generates professor ID
- ✅ Update professor successfully
- ✅ Update professor - professor not found
- ✅ Update professor - department not found
- ✅ Get professor by ID
- ✅ Get professor - not found
- ✅ Get professors by department
- ✅ Get all professors
- ✅ Deactivate professor
- ✅ Activate professor
- ✅ Generate professor ID format
- ✅ Get professor courses by semester
- ✅ Get professor courses with status
- ✅ Get professor dashboard overview
- ✅ Create professor folder manually

**Estimated Tests:** 20

---

## Priority 2: Important Services (Medium Business Impact)

### 5. AcademicService Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/service/academic/AcademicServiceTest.java`

**Test Scenarios:**
- ✅ Create academic year
- ✅ Update academic year
- ✅ Get all academic years
- ✅ Get active academic year
- ✅ Set active academic year
- ✅ Get semester by ID
- ✅ Get semesters by year
- ✅ Update semester
- ✅ Validation - duplicate year code
- ✅ Validation - invalid date ranges

**Estimated Tests:** 10

---

### 6. DashboardWidgetService Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/service/dashboard/DashboardWidgetServiceTest.java`

**Test Scenarios:**
- ✅ Get statistics - all data
- ✅ Get statistics - filtered by academic year
- ✅ Get statistics - filtered by semester
- ✅ Get submissions over time - by day
- ✅ Get submissions over time - by week
- ✅ Get submissions over time - by month
- ✅ Get department distribution - all semesters
- ✅ Get department distribution - specific semester
- ✅ Get status distribution - all semesters
- ✅ Get status distribution - specific semester
- ✅ Get recent activity - with limit
- ✅ Get recent activity - empty result

**Estimated Tests:** 12

---

### 7. FileExplorerService Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/service/file/FileExplorerServiceTest.java`

**Test Scenarios:**
- ✅ List folders in directory
- ✅ List files in directory
- ✅ Navigate folder hierarchy
- ✅ Search files by name
- ✅ Filter files by type
- ✅ Get folder metadata
- ✅ Permission checks for folder access

**Estimated Tests:** 7

---

### 8. FileAccessService Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/service/file/FileAccessServiceTest.java`

**Test Scenarios:**
- ✅ Check read permission - same department
- ✅ Check read permission - deanship (all access)
- ✅ Check read permission - different department
- ✅ Check write permission - professor own files
- ✅ Check write permission - unauthorized
- ✅ Check delete permission - professor own files
- ✅ Check delete permission - unauthorized

**Estimated Tests:** 7

---

### 9. FilePreviewService Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/service/file/FilePreviewServiceTest.java`

**Test Scenarios:**
- ✅ Generate preview for PDF
- ✅ Generate preview for image
- ✅ Generate preview - unsupported type
- ✅ Get preview URL
- ✅ Cache preview generation

**Estimated Tests:** 5

---

## Priority 3: Integration Tests for Controllers

### 10. AuthController Integration Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/AuthControllerIntegrationTest.java`

**Test Scenarios:**
- ✅ Login successfully - returns JWT and refresh token
- ✅ Login - invalid credentials (401)
- ✅ Login - user not found (401)
- ✅ Refresh token successfully
- ✅ Refresh token - invalid token (400)
- ✅ Refresh token - expired token (400)
- ✅ Logout successfully
- ✅ Logout with token successfully
- ✅ Validate token - valid (200)
- ✅ Validate token - invalid (400)

**Estimated Tests:** 10

---

### 11. ProfessorController Integration Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/ProfessorControllerIntegrationTest.java`

**Test Scenarios:**
- ✅ Get professor courses - authenticated professor
- ✅ Get professor courses - unauthorized (403)
- ✅ Get professor dashboard overview
- ✅ Get course assignments with status
- ✅ Filter courses by semester
- ✅ Access own data only (security)

**Estimated Tests:** 6

---

### 12. HodController Integration Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/HodControllerIntegrationTest.java`

**Test Scenarios:**
- ✅ Get department statistics
- ✅ Get department professors
- ✅ Get department courses
- ✅ Get department submissions
- ✅ Filter by semester
- ✅ Access only own department (security)
- ✅ Unauthorized access attempt (403)

**Estimated Tests:** 7

---

### 13. DeanshipController Integration Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/DeanshipControllerIntegrationTest.java`

**Test Scenarios:**
- ✅ Get all departments
- ✅ Get all professors
- ✅ Get all courses
- ✅ Get dashboard statistics
- ✅ Get department distribution
- ✅ Get status distribution
- ✅ Unauthorized access (403)

**Estimated Tests:** 7

---

### 14. FileUploadController Integration Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/FileUploadControllerIntegrationTest.java`

**Test Scenarios:**
- ✅ Upload files successfully
- ✅ Upload files - invalid file type (400)
- ✅ Upload files - file too large (400)
- ✅ Upload files - too many files (400)
- ✅ Replace files successfully
- ✅ Get files by submission
- ✅ Delete file successfully
- ✅ Download file successfully
- ✅ Permission checks

**Estimated Tests:** 9

---

### 15. FileExplorerController Integration Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/FileExplorerControllerIntegrationTest.java`

**Test Scenarios:**
- ✅ List folders
- ✅ List files in folder
- ✅ Navigate folder path
- ✅ Search files
- ✅ Get folder metadata
- ✅ Permission validation

**Estimated Tests:** 6

---

### 16. FilePreviewController Integration Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/FilePreviewControllerIntegrationTest.java`

**Test Scenarios:**
- ✅ Get preview for PDF
- ✅ Get preview for image
- ✅ Get preview - file not found (404)
- ✅ Get preview - unsupported type (400)

**Estimated Tests:** 4

---

### 17. SessionController Integration Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/controller/api/SessionControllerIntegrationTest.java`

**Test Scenarios:**
- ✅ Get current session
- ✅ Validate session
- ✅ Invalidate session
- ✅ Session timeout handling

**Estimated Tests:** 4

---

## Priority 4: Additional Test Scenarios for Existing Tests

### 18. AuthServiceTest - Additional Scenarios
**Current:** 4+ tests | **Needed:** Complete coverage

**Additional Test Scenarios:**
- ✅ Login - invalidate old session
- ✅ Login - create new session
- ✅ Login - device info extraction
- ✅ Get current user - different scenarios
- ✅ Logout - clear SecurityContext

**Estimated Additional Tests:** 5

---

### 19. FileServiceTest - Additional Scenarios
**Current:** 14 tests | **Needed:** More comprehensive coverage

**Additional Test Scenarios:**
- ✅ Upload files - full workflow
- ✅ Replace files - full workflow
- ✅ Load file as resource - path traversal protection
- ✅ Load file as resource with permission check
- ✅ Delete file - updates submission metadata
- ✅ File path generation - edge cases
- ✅ Filename sanitization - various special characters

**Estimated Additional Tests:** 7

---

## Priority 5: E2E Test Scenarios

### 20. Authentication E2E Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/e2e/AuthenticationE2ETest.java`

**Test Scenarios:**
- ✅ Complete login → refresh token → logout flow
- ✅ Login → session timeout → re-authentication
- ✅ Multiple device login → token management
- ✅ Token expiration handling
- ✅ Concurrent login sessions

**Estimated Tests:** 5

---

### 21. File Upload E2E Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/e2e/FileUploadE2ETest.java`

**Test Scenarios:**
- ✅ Professor uploads documents → HOD views → Deanship views
- ✅ Upload → replace → delete workflow
- ✅ Multiple file upload → folder structure creation
- ✅ Permission validation across roles
- ✅ File download workflow

**Estimated Tests:** 5

---

### 22. Course Assignment E2E Tests
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/e2e/CourseAssignmentE2ETest.java`

**Test Scenarios:**
- ✅ Admin creates course → assigns to professor → professor uploads documents
- ✅ HOD views department courses → filters by semester
- ✅ Deanship views all courses → generates reports
- ✅ Complete course lifecycle

**Estimated Tests:** 4

---

## Priority 6: Utility and Helper Tests

### 23. TestDataBuilder Enhancements
**Current:** Basic builders | **Needed:** Complete coverage

**Additional Builders Needed:**
- ✅ RefreshToken builder
- ✅ AcademicYear builder
- ✅ Semester builder (complete)
- ✅ DocumentRequest builder (for legacy tests)
- ✅ Notification builder
- ✅ Folder builder
- ✅ UploadedFile builder (complete)

---

### 24. PathParser Tests (if exists as utility)
**Location:** `src/test/java/com/alquds/edu/ArchiveSystem/util/PathParserTest.java`

**Test Scenarios:**
- ✅ Parse valid folder path
- ✅ Parse path with special characters
- ✅ Parse invalid path format
- ✅ Extract components from path

**Estimated Tests:** 4

---

## Test Summary by Priority

### Priority 1 (Critical): 75 tests
- RefreshTokenService: 14 tests
- JwtService: 20 tests
- FolderService: 21 tests
- ProfessorService: 20 tests

### Priority 2 (Important): 41 tests
- AcademicService: 10 tests
- DashboardWidgetService: 12 tests
- FileExplorerService: 7 tests
- FileAccessService: 7 tests
- FilePreviewService: 5 tests

### Priority 3 (Integration): 53 tests
- AuthController: 10 tests
- ProfessorController: 6 tests
- HodController: 7 tests
- DeanshipController: 7 tests
- FileUploadController: 9 tests
- FileExplorerController: 6 tests
- FilePreviewController: 4 tests
- SessionController: 4 tests

### Priority 4 (Enhancements): 12 tests
- AuthServiceTest additions: 5 tests
- FileServiceTest additions: 7 tests

### Priority 5 (E2E): 14 tests
- Authentication E2E: 5 tests
- File Upload E2E: 5 tests
- Course Assignment E2E: 4 tests

### Priority 6 (Utilities): 4+ tests
- PathParser: 4 tests

---

## Total Remaining Tests: ~199 tests

**Current:** 129 tests  
**Remaining:** ~199 tests  
**Target Total:** ~328 tests

---

## Implementation Guidelines

### Test Structure
Follow the existing pattern:
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("[ServiceName] Unit Tests")
class ServiceNameTest {
    @Mock
    private DependencyRepository repository;
    
    @InjectMocks
    private ServiceName service;
    
    @Test
    @DisplayName("Should [action] successfully")
    void shouldActionSuccessfully() {
        // Arrange
        // Act
        // Assert
    }
}
```

### Test Coverage Goals
- **Unit Tests:** 80% coverage of business logic
- **Integration Tests:** All public API endpoints
- **E2E Tests:** Critical user workflows

### Best Practices
1. Use `TestDataBuilder` for creating test entities
2. Follow AAA pattern (Arrange-Act-Assert)
3. Use descriptive test names with `@DisplayName`
4. Test both success and failure scenarios
5. Test edge cases and boundary conditions
6. Mock external dependencies
7. Use `@Transactional` for integration tests
8. Clean up test data in `@BeforeEach` or `@AfterEach`

---

## Notes

### Deprecated Services
- **DocumentRequestService**: Marked as `@Deprecated` - consider skipping tests or minimal coverage for migration purposes only

### Services with Partial Coverage
- **AuthService**: Core functionality tested, but login flow needs SecurityContext setup
- **FileService**: Basic validation tested, but full upload/download workflows need coverage

### Complex Services
- **FolderService**: Involves file system operations - may need `@TempDir` for testing
- **JwtService**: Requires proper JWT secret configuration in tests
- **RefreshTokenService**: Involves scheduled tasks - may need to test cleanup separately

---

## Quick Start Guide

### To implement Priority 1 tests:

1. **RefreshTokenServiceTest**
   ```bash
   # Create test file
   touch src/test/java/com/alquds/edu/ArchiveSystem/service/auth/RefreshTokenServiceTest.java
   ```

2. **JwtServiceTest**
   ```bash
   touch src/test/java/com/alquds/edu/ArchiveSystem/service/auth/JwtServiceTest.java
   ```

3. **FolderServiceTest**
   ```bash
   touch src/test/java/com/alquds/edu/ArchiveSystem/service/file/FolderServiceTest.java
   ```

4. **ProfessorServiceTest**
   ```bash
   touch src/test/java/com/alquds/edu/ArchiveSystem/service/academic/ProfessorServiceTest.java
   ```

### Running Tests
```bash
# Run specific test class
mvn test -Dtest=RefreshTokenServiceTest

# Run all service tests
mvn test -Dtest=*ServiceTest

# Run with coverage
mvn test jacoco:report
```

---

## Estimated Implementation Time

- **Priority 1:** 2-3 days (75 tests)
- **Priority 2:** 1-2 days (41 tests)
- **Priority 3:** 2-3 days (53 tests)
- **Priority 4:** 0.5 days (12 tests)
- **Priority 5:** 1 day (14 tests)
- **Priority 6:** 0.5 days (4+ tests)

**Total Estimated Time:** 7-10 days for complete coverage

---

## Success Criteria

✅ All Priority 1 services have comprehensive unit tests  
✅ All public API endpoints have integration tests  
✅ Critical workflows have E2E tests  
✅ Test coverage > 80% for business logic  
✅ All tests passing in CI/CD pipeline  
✅ Tests follow consistent patterns and best practices
