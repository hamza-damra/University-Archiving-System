# Department-Scoped Filtering Implementation Summary

## Task: 21.2 Implement department-scoped filtering in services

### Requirements Addressed

- **Requirement 7.1**: HOD department-scoped access
  - "WHERE the user has HOD role, THE System SHALL restrict file explorer access to professors and courses belonging to the HOD's department"

- **Requirement 9.1**: Professor department-scoped read access
  - "WHERE the user has Professor role, THE System SHALL grant read access to files of other professors in the same department and semester"

### Implementation Overview

Implemented comprehensive department-scoped filtering across all service layers to ensure:
- **Deanship**: Can access all departments (no filtering)
- **HOD**: Can only access data from their own department (read-only)
- **Professor**: Can read data from their own department, write only to their own assignments

## Files Created

### 1. DepartmentScopedFilterService.java
**Location**: `src/main/java/com/alqude/edu/ArchiveSystem/service/DepartmentScopedFilterService.java`

**Purpose**: Centralized service for applying department-scoped filtering based on user role.

**Key Methods**:
- `filterCourseAssignments(assignments, currentUser)` - Filters course assignments by department
- `filterProfessors(professors, currentUser)` - Filters professor lists by department
- `filterCourses(courses, currentUser)` - Filters course lists by department
- `filterDocumentSubmissions(submissions, currentUser)` - Filters submissions by department
- `validateDepartmentAccess(departmentId, currentUser)` - Validates department access, throws exception if denied
- `validateWriteAccess(assignment, currentUser)` - Validates write access for professors
- `getDepartmentIdForFiltering(currentUser)` - Returns department ID for filtering based on role

### 2. DEPARTMENT_SCOPED_FILTERING.md
**Location**: `src/main/java/com/alqude/edu/ArchiveSystem/service/DEPARTMENT_SCOPED_FILTERING.md`

**Purpose**: Comprehensive documentation of the department-scoped filtering implementation.

**Contents**:
- Filtering rules by role
- Implementation details
- Service integration examples
- Controller integration examples
- Security considerations
- Testing guidelines
- Migration notes

## Files Modified

### 1. CourseServiceImpl.java
**Changes**:
- Added `DepartmentScopedFilterService` dependency injection
- Added new method: `getAssignmentsBySemester(Long semesterId, User currentUser)`
  - Returns course assignments filtered by user's department
  - Deanship sees all, HOD/Professor see only their department

**Usage Example**:
```java
List<CourseAssignment> assignments = courseService.getAssignmentsBySemester(semesterId, currentUser);
```

### 2. SubmissionServiceImpl.java
**Changes**:
- Added `DepartmentScopedFilterService` dependency injection
- Added new method: `getSubmissionsBySemester(Long semesterId, User currentUser)`
  - Returns submissions filtered by user's department
- Added new method: `getStatisticsBySemester(Long semesterId, User currentUser)`
  - Returns statistics with automatic department filtering
- Enhanced existing method: `getStatisticsBySemester(Long semesterId, Long departmentId)`
  - Added null check for department to prevent NPE

**Usage Example**:
```java
List<DocumentSubmission> submissions = submissionService.getSubmissionsBySemester(semesterId, currentUser);
SubmissionStatistics stats = submissionService.getStatisticsBySemester(semesterId, currentUser);
```

### 3. ProfessorServiceImpl.java
**Changes**:
- Added `DepartmentScopedFilterService` dependency injection
- Added new method: `getAllProfessors(User currentUser)`
  - Returns all professors for Deanship
  - Returns only department professors for HOD/Professor

**Usage Example**:
```java
List<User> professors = professorService.getAllProfessors(currentUser);
```

### 4. SemesterReportServiceImpl.java
**Changes**:
- Added `DepartmentScopedFilterService` dependency injection
- Enhanced method: `generateProfessorSubmissionReport(Long semesterId, Long departmentId)`
  - Added department access validation
  - Throws `UnauthorizedOperationException` if HOD/Professor tries to access another department

**Usage Example**:
```java
// Automatically validates that currentUser has access to departmentId
ProfessorSubmissionReport report = reportService.generateProfessorSubmissionReport(semesterId, departmentId);
```

### 5. FileExplorerServiceImpl.java
**Status**: Already implements department-scoped filtering
- `getProfessorsForUser()` method already filters by department
- Permission checks in `canRead()`, `canWrite()`, `canDelete()` already enforce department boundaries
- No changes needed

## Filtering Logic Summary

### For HOD Users:
1. **Course Assignments**: Only see assignments where professor belongs to HOD's department
2. **Professors**: Only see professors in their department
3. **Courses**: Only see courses in their department
4. **Submissions**: Only see submissions from professors in their department
5. **Reports**: Can only generate reports for their department
6. **File Explorer**: Can only browse files from their department

### For Professor Users:
1. **Course Assignments**: Only see assignments where professor belongs to their department (read access)
2. **Professors**: Only see professors in their department
3. **Courses**: Only see courses in their department
4. **Submissions**: Can read submissions from their department, can write only to their own assignments
5. **Reports**: Can only view their own statistics
6. **File Explorer**: Can browse department files, can upload/modify only their own files

### For Deanship Users:
1. **No Filtering**: Can access all departments, all data
2. **Full Access**: Can manage academic structure, professors, courses across all departments

## Security Enhancements

1. **Validation at Service Layer**: Department access is validated before data is returned
2. **Exception Handling**: `UnauthorizedOperationException` thrown for access violations
3. **Logging**: All filtering operations are logged for audit purposes
4. **Null Safety**: Handles cases where users have no department assigned
5. **Write Access Control**: Separate validation for write operations (professors only)

## Backward Compatibility

All existing methods are preserved:
- `getAssignmentsBySemester(Long semesterId)` - No filtering (for internal use)
- `getStatisticsBySemester(Long semesterId, Long departmentId)` - Manual filtering

New methods provide automatic filtering:
- `getAssignmentsBySemester(Long semesterId, User currentUser)` - Automatic filtering
- `getStatisticsBySemester(Long semesterId, User currentUser)` - Automatic filtering

Controllers should be updated to use the new methods with automatic filtering.

## Testing Recommendations

### Unit Tests
1. Test `DepartmentScopedFilterService` filtering methods with different roles
2. Test validation methods throw exceptions correctly
3. Test edge cases (null department, empty lists, etc.)

### Integration Tests
1. Test HOD cannot access other department's data
2. Test Professor cannot modify other professor's assignments
3. Test Deanship can access all departments
4. Test unauthorized access attempts are logged and blocked

### Example Test Cases
```java
@Test
void testHOD_CannotAccessOtherDepartment() {
    User hodUser = createHODUser(department1);
    assertThrows(UnauthorizedOperationException.class, () -> {
        departmentScopedFilterService.validateDepartmentAccess(department2.getId(), hodUser);
    });
}

@Test
void testProfessor_CannotWriteToOtherAssignment() {
    User professor1 = createProfessor(department1);
    CourseAssignment assignment2 = createAssignment(professor2, department1);
    
    assertThrows(UnauthorizedOperationException.class, () -> {
        departmentScopedFilterService.validateWriteAccess(assignment2, professor1);
    });
}
```

## Next Steps for Controllers

Controllers should be updated to use the new filtering methods:

### HodController
```java
@GetMapping("/dashboard/overview")
public ResponseEntity<DashboardOverview> getDashboardOverview(
        @RequestParam Long semesterId,
        Authentication authentication) {
    
    User currentUser = (User) authentication.getPrincipal();
    
    // Automatic department filtering
    SubmissionStatistics stats = submissionService.getStatisticsBySemester(semesterId, currentUser);
    
    return ResponseEntity.ok(buildOverview(stats));
}
```

### ProfessorController
```java
@PostMapping("/submissions/upload")
public ResponseEntity<DocumentSubmission> uploadFiles(
        @RequestParam Long courseAssignmentId,
        @RequestPart("files") List<MultipartFile> files,
        Authentication authentication) {
    
    User currentUser = (User) authentication.getPrincipal();
    
    // Validate write access
    CourseAssignment assignment = courseAssignmentRepository.findById(courseAssignmentId)
            .orElseThrow(() -> new EntityNotFoundException("Assignment not found"));
    
    departmentScopedFilterService.validateWriteAccess(assignment, currentUser);
    
    // Proceed with upload
    return fileService.uploadFiles(courseAssignmentId, files, currentUser.getId());
}
```

## Conclusion

The department-scoped filtering implementation provides:
- ✅ Centralized filtering logic
- ✅ Role-based access control
- ✅ Security validation at service layer
- ✅ Comprehensive logging
- ✅ Backward compatibility
- ✅ Clear documentation
- ✅ Testable design

This implementation satisfies requirements 7.1 and 9.1, ensuring that HOD and Professor users can only access data within their authorized department scope.
