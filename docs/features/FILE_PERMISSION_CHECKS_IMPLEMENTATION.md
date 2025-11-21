# File Permission Checks Implementation - Task 21.3

## Overview
This document describes the implementation of comprehensive file permission checks in the FileService, as specified in task 21.3 of the semester-based file system refactoring.

## Requirements Addressed
- **Requirement 5.5**: File Explorer Interface - "Where a user has read permission for a file, THE System SHALL provide view and download actions for that file"
- **Requirement 7.5**: HOD Department-Scoped Access - "Where the user has HOD role, THE System SHALL prevent file deletion, editing, or upload actions on any files"
- **Requirement 9.4**: Professor Read Access - "Where the user has Professor role, THE System SHALL grant full upload and replace permissions only for folders corresponding to courses assigned to that professor"

## Implementation Details

### Permission Rules

#### Read Permission (canReadFile)
- **Deanship**: Can read all files across all departments
- **HOD**: Can read files only within their department (read-only)
- **Professor**: Can read files within their department (own and colleagues)

#### Write Permission (canWriteToCourseAssignment)
- **Deanship**: No write access (read-only)
- **HOD**: No write access (read-only)
- **Professor**: Can write/upload only to their own course assignments

#### Delete Permission (canDeleteFile)
- **Deanship**: Cannot delete any files
- **HOD**: Cannot delete any files
- **Professor**: Can delete only their own files

### Methods Enhanced

#### Private Permission Checking Methods
1. **canReadFile(UploadedFile file, User user)**
   - Checks if user can read a specific file
   - Called by: `getFile()`, `getFilesBySubmission()`
   - Throws: `UnauthorizedOperationException` if permission denied

2. **canWriteToCourseAssignment(CourseAssignment courseAssignment, User user)**
   - Checks if user can upload files to a course assignment
   - Called by: `uploadFiles()`
   - Throws: `UnauthorizedOperationException` if permission denied

3. **canDeleteFile(UploadedFile file, User user)**
   - Checks if user can delete a specific file
   - Called by: `deleteFile()`
   - Throws: `UnauthorizedOperationException` if permission denied

#### Public Permission Checking Methods (New)
These methods are exposed in the FileService interface for use by controllers and other services:

1. **canUserReadFile(Long fileId, User user)**
   - Public wrapper for read permission checking
   - Returns boolean instead of throwing exception
   - Safe to call with null parameters

2. **canUserWriteToCourseAssignment(Long courseAssignmentId, User user)**
   - Public wrapper for write permission checking
   - Returns boolean instead of throwing exception
   - Safe to call with null parameters

3. **canUserDeleteFile(Long fileId, User user)**
   - Public wrapper for delete permission checking
   - Returns boolean instead of throwing exception
   - Safe to call with null parameters

### File Operations with Permission Checks

#### 1. uploadFiles()
- **Permission Check**: `canWriteToCourseAssignment()`
- **When**: Before validating and uploading files
- **Exception**: `UnauthorizedOperationException` if user cannot write to course assignment

#### 2. replaceFiles()
- **Permission Check**: Verifies professor owns the submission
- **When**: Before deleting old files and uploading new ones
- **Exception**: `UnauthorizedOperationException` if user doesn't own the submission

#### 3. getFile()
- **Permission Check**: `canReadFile()`
- **When**: After fetching file from database
- **Exception**: `UnauthorizedOperationException` if user cannot read the file

#### 4. getFilesBySubmission()
- **Permission Check**: `canReadFile()` on first file, or department check if no files
- **When**: After fetching submission from database
- **Exception**: `UnauthorizedOperationException` if user cannot access submission

#### 5. deleteFile()
- **Permission Check**: `canDeleteFile()`
- **When**: After fetching file from database
- **Exception**: `UnauthorizedOperationException` if user cannot delete the file

#### 6. loadFileAsResource()
- **Note**: Permission checking should be done at controller level using `getFile()` first
- **New Method**: `loadFileAsResourceWithPermissionCheck()` combines both operations

### Error Handling

All permission violations throw `UnauthorizedOperationException` with descriptive messages:
- "User does not have permission to upload files to this course assignment"
- "User does not have permission to replace files for this submission"
- "User does not have permission to access this file"
- "User does not have permission to access files for this submission"
- "User does not have permission to delete this file"

### Logging

All permission checks include detailed logging:
- **DEBUG**: Permission check results (granted/denied)
- **WARN**: Null users, missing departments, or missing entities
- **ERROR**: Exceptions during permission checking

### Integration with FileExplorerService

The FileExplorerService has its own permission checking methods that work at the path level:
- `canRead(String nodePath, User user)`
- `canWrite(String nodePath, User user)`
- `canDelete(String nodePath, User user)`

These methods complement the FileService permission checks by providing path-based access control for the file explorer UI.

## Testing Recommendations

### Unit Tests
1. Test each permission method with all three roles (Deanship, HOD, Professor)
2. Test permission checks with same department vs. different department
3. Test permission checks with null users and null entities
4. Test that exceptions are thrown when permissions are denied

### Integration Tests
1. Test file upload with unauthorized user
2. Test file download with unauthorized user
3. Test file deletion with unauthorized user
4. Test cross-department access restrictions
5. Test that Deanship can access all files
6. Test that HOD cannot upload or delete files

## Security Considerations

1. **Defense in Depth**: Permission checks are performed at multiple levels:
   - Service layer (FileService)
   - Controller layer (via @PreAuthorize annotations)
   - File explorer layer (FileExplorerService)

2. **Fail-Safe Defaults**: All permission methods return `false` or throw exceptions when:
   - User is null
   - Entity is not found
   - Department information is missing
   - Any error occurs during checking

3. **Audit Trail**: All permission checks are logged for security auditing

4. **Consistent Enforcement**: Permission rules are consistently applied across all file operations

## Future Enhancements

1. **Deadline-Based Restrictions**: Add checks to prevent file deletion after deadlines
2. **Approval Workflows**: Add permission checks for HOD approval of submissions
3. **Bulk Operations**: Extend permission checks to bulk file operations
4. **File Versioning**: Add permission checks for viewing file history
5. **Temporary Access**: Implement time-limited access tokens for file sharing
