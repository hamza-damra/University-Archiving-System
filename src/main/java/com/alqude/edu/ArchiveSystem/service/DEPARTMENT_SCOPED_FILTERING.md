# Department-Scoped Filtering Implementation

## Overview

This document describes the department-scoped filtering implementation for the Archive System, which ensures that users can only access data within their authorized scope based on their role.

## Filtering Rules

### Role-Based Access

1. **Deanship (ROLE_DEANSHIP)**
   - **Read Access**: All departments, all data
   - **Write Access**: Can manage academic structure, professors, courses
   - **Filtering**: No filtering applied

2. **HOD (ROLE_HOD)**
   - **Read Access**: Only their own department
   - **Write Access**: Cannot manage professors or courses (read-only)
   - **Filtering**: All queries filtered by department

3. **Professor (ROLE_PROFESSOR)**
   - **Read Access**: Only their own department (can view other professors' files)
   - **Write Access**: Only their own course assignments
   - **Filtering**: Read operations filtered by department, write operations filtered by assignment

## Implementation

### DepartmentScopedFilterService

The `DepartmentScopedFilterService` provides centralized filtering logic for all services.

#### Key Methods

1. **filterCourseAssignments(assignments, currentUser)**
   - Filters course assignments based on user role and department
   - Used by: CourseService, SubmissionService, ReportService

2. **filterProfessors(professors, currentUser)**
   - Filters professor lists based on user role and department
   - Used by: ProfessorService, FileExplorerService

3. **filterCourses(courses, currentUser)**
   - Filters course lists based on user role and department
   - Used by: CourseService

4. **filterDocumentSubmissions(submissions, currentUser)**
   - Filters document submissions based on user role and department
   - Used by: SubmissionService, ReportService

5. **validateDepartmentAccess(departmentId, currentUser)**
   - Validates that a user has access to a specific department
   - Throws UnauthorizedOperationException if access is denied
   - Used by: All services that accept departmentId parameter

6. **validateWriteAccess(assignment, currentUser)**
   - Validates that a professor has write access to a course assignment
   - Throws UnauthorizedOperationException if access is denied
   - Used by: FileService, SubmissionService

7. **getDepartmentIdForFiltering(currentUser)**
   - Returns the department ID for filtering based on user role
   - Returns null for Deanship (no filtering)
   - Returns user's department ID for HOD and Professor

## Service Integration

### CourseServiceImpl

**New Methods:**
- `getAssignmentsBySemester(semesterId, currentUser)` - Returns filtered course assignments

**Usage:**
```java
// In controller
List<CourseAssignment> assignments = courseService.getAssignmentsBySemester(semesterId, currentUser);
```

### SubmissionServiceImpl

**New Methods:**
- `getSubmissionsBySemester(semesterId, currentUser)` - Returns filtered submissions
- `getStatisticsBySemester(semesterId, currentUser)` - Returns statistics with automatic filtering

**Usage:**
```java
// In controller
List<DocumentSubmission> submissions = submissionService.getSubmissionsBySemester(semesterId, currentUser);
SubmissionStatistics stats = submissionService.getStatisticsBySemester(semesterId, currentUser);
```

### ProfessorServiceImpl

**New Methods:**
- `getAllProfessors(currentUser)` - Returns filtered professor list

**Usage:**
```java
// In controller
List<User> professors = professorService.getAllProfessors(currentUser);
```

### SemesterReportServiceImpl

**Enhanced Methods:**
- `generateProfessorSubmissionReport(semesterId, departmentId)` - Now validates department access

**Usage:**
```java
// In controller - department access is automatically validated
ProfessorSubmissionReport report = reportService.generateProfessorSubmissionReport(semesterId, departmentId);
```

### FileExplorerServiceImpl

**Existing Implementation:**
- Already implements department-scoped filtering in `getProfessorsForUser()`
- Permission checks in `canRead()`, `canWrite()`, `canDelete()`

## Controller Integration

Controllers should pass the current authenticated user to service methods that require department-scoped filtering.

### Example: HodController

```java
@GetMapping("/submissions/status")
public ResponseEntity<ProfessorSubmissionReport> getSubmissionStatus(
        @RequestParam Long semesterId,
        Authentication authentication) {
    
    User currentUser = (User) authentication.getPrincipal();
    
    // Service automatically filters by HOD's department
    Long departmentId = currentUser.getDepartment().getId();
    ProfessorSubmissionReport report = reportService.generateProfessorSubmissionReport(
            semesterId, departmentId);
    
    return ResponseEntity.ok(report);
}
```

### Example: ProfessorController

```java
@PostMapping("/submissions/upload")
public ResponseEntity<DocumentSubmission> uploadFiles(
        @RequestParam Long courseAssignmentId,
        @RequestParam DocumentTypeEnum documentType,
        @RequestPart("files") List<MultipartFile> files,
        Authentication authentication) {
    
    User currentUser = (User) authentication.getPrincipal();
    
    // Validate write access before upload
    CourseAssignment assignment = courseAssignmentRepository.findById(courseAssignmentId)
            .orElseThrow(() -> new EntityNotFoundException("Course assignment not found"));
    
    departmentScopedFilterService.validateWriteAccess(assignment, currentUser);
    
    // Proceed with upload
    return fileService.uploadFiles(courseAssignmentId, documentType, files, currentUser.getId());
}
```

## Security Considerations

1. **Always validate department access** when accepting departmentId as a parameter
2. **Use the current authenticated user** from Spring Security context
3. **Apply filtering at the service layer**, not just at the controller layer
4. **Log unauthorized access attempts** for security auditing
5. **Throw UnauthorizedOperationException** for access violations

## Testing

### Unit Tests

Test the filtering logic in `DepartmentScopedFilterService`:

```java
@Test
void testFilterCourseAssignments_HOD_OnlySeesOwnDepartment() {
    // Given
    User hodUser = createHODUser(department1);
    List<CourseAssignment> assignments = Arrays.asList(
        createAssignment(professor1, department1),
        createAssignment(professor2, department2)
    );
    
    // When
    List<CourseAssignment> filtered = filterService.filterCourseAssignments(assignments, hodUser);
    
    // Then
    assertEquals(1, filtered.size());
    assertEquals(department1.getId(), filtered.get(0).getProfessor().getDepartment().getId());
}
```

### Integration Tests

Test the complete flow from controller to service:

```java
@Test
@WithMockUser(username = "hod@example.com", roles = "HOD")
void testHOD_CannotAccessOtherDepartmentReport() {
    // Given
    Long otherDepartmentId = 2L;
    
    // When/Then
    mockMvc.perform(get("/api/hod/reports/professor-submissions")
            .param("semesterId", "1")
            .param("departmentId", otherDepartmentId.toString()))
            .andExpect(status().isForbidden());
}
```

## Migration Notes

### Backward Compatibility

Existing methods without user parameter are preserved for backward compatibility:
- `getAssignmentsBySemester(semesterId)` - No filtering
- `getStatisticsBySemester(semesterId, departmentId)` - Manual department filtering

New methods with user parameter provide automatic filtering:
- `getAssignmentsBySemester(semesterId, currentUser)` - Automatic filtering
- `getStatisticsBySemester(semesterId, currentUser)` - Automatic filtering

### Controller Updates

Controllers should be updated to use the new methods with automatic filtering:

**Before:**
```java
List<CourseAssignment> assignments = courseService.getAssignmentsBySemester(semesterId);
// Manual filtering required
```

**After:**
```java
User currentUser = (User) authentication.getPrincipal();
List<CourseAssignment> assignments = courseService.getAssignmentsBySemester(semesterId, currentUser);
// Automatic filtering applied
```

## Requirements Satisfied

This implementation satisfies the following requirements:

- **Requirement 7.1**: HOD department-scoped access
  - "WHERE the user has HOD role, THE System SHALL restrict file explorer access to professors and courses belonging to the HOD's department"

- **Requirement 9.1**: Professor department-scoped read access
  - "WHERE the user has Professor role, THE System SHALL grant read access to files of other professors in the same department and semester"

## Future Enhancements

1. **Caching**: Cache department filtering results for improved performance
2. **Audit Logging**: Log all department access attempts for security auditing
3. **Fine-grained Permissions**: Add more granular permissions beyond department-level
4. **Dynamic Role Assignment**: Support users with multiple roles or departments
