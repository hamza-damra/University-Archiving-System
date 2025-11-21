# FolderService API Documentation

## Overview

The `FolderService` provides automated folder provisioning and management for the Archive System. It handles the creation of professor folders and course folder structures with standard subfolders, ensuring idempotent operations and proper file system synchronization.

**Package**: `com.alqude.edu.ArchiveSystem.service`  
**Implementation**: `FolderServiceImpl`  
**Version**: 1.0.0

---

## Core Methods

### createProfessorFolder

Creates a folder for a professor within a specific academic year and semester.

**Signature**:
```java
Folder createProfessorFolder(Long professorId, Long academicYearId, Long semesterId)
```

**Parameters**:
- `professorId` (Long, required): The ID of the professor
- `academicYearId` (Long, required): The ID of the academic year
- `semesterId` (Long, required): The ID of the semester

**Returns**: `Folder` - The created or existing professor folder

**Behavior**:
- **Idempotent**: If the folder already exists, returns the existing folder without creating a duplicate
- **Path Convention**: `{yearCode}/{semesterType}/{professorId}`
  - Example: `2024-2025/Fall/123`
- **Database**: Creates a `Folder` entity with type `PROFESSOR_FOLDER`
- **File System**: Creates the physical directory using `Files.createDirectories()`
- **Metadata**: Sets owner, academic year, semester, and timestamps

**Exceptions**:
- `EntityNotFoundException`: If professor, academic year, or semester not found
- `IOException`: If file system operation fails (wrapped in RuntimeException)

**Example**:
```java
Folder professorFolder = folderService.createProfessorFolder(123L, 1L, 2L);
// Returns: Folder with path "2024-2025/Fall/123"
```

**Auto-Creation Trigger**:
- Currently triggered manually via endpoint
- Future: May be triggered automatically on professor creation

---

### createCourseFolderStructure

Creates a complete folder structure for a course assignment, including standard subfolders.

**Signature**:
```java
List<Folder> createCourseFolderStructure(Long assignmentId)
```

**Parameters**:
- `assignmentId` (Long, required): The ID of the course assignment

**Returns**: `List<Folder>` - List of all created/existing folders (course folder + subfolders)

**Behavior**:
- **Idempotent**: Skips creation if folders already exist
- **Path Convention**: `{yearCode}/{semesterType}/{professorId}/{courseCode} - {courseName}`
  - Example: `2024-2025/Fall/123/CS101 - Introduction to Programming`
- **Standard Subfolders**: Creates 4 subfolders automatically:
  1. `Syllabus`
  2. `Exams`
  3. `Course Notes`
  4. `Assignments`
- **Database**: Creates `Folder` entities for course folder and all subfolders
- **File System**: Creates physical directories for all folders
- **Metadata**: Sets proper parent-child relationships, owner, academic year, semester

**Exceptions**:
- `EntityNotFoundException`: If assignment, professor, course, academic year, or semester not found
- `IOException`: If file system operation fails (wrapped in RuntimeException)

**Example**:
```java
List<Folder> courseFolders = folderService.createCourseFolderStructure(456L);
// Returns: List of 5 folders (1 course folder + 4 subfolders)
```

**Auto-Creation Trigger**:
- Automatically triggered when a course is assigned to a professor
- Triggered in `CourseServiceImpl.assignCourse()` method

---

### professorFolderExists

Checks if a professor folder exists for the given parameters.

**Signature**:
```java
boolean professorFolderExists(Long professorId, Long academicYearId, Long semesterId)
```

**Parameters**:
- `professorId` (Long, required): The ID of the professor
- `academicYearId` (Long, required): The ID of the academic year
- `semesterId` (Long, required): The ID of the semester

**Returns**: `boolean` - `true` if folder exists, `false` otherwise

**Example**:
```java
boolean exists = folderService.professorFolderExists(123L, 1L, 2L);
```

---

### courseFolderExists

Checks if a course folder exists for the given assignment.

**Signature**:
```java
boolean courseFolderExists(Long assignmentId)
```

**Parameters**:
- `assignmentId` (Long, required): The ID of the course assignment

**Returns**: `boolean` - `true` if folder exists, `false` otherwise

**Example**:
```java
boolean exists = folderService.courseFolderExists(456L);
```

---

### getFolderByPath

Retrieves a folder by its path.

**Signature**:
```java
Optional<Folder> getFolderByPath(String path)
```

**Parameters**:
- `path` (String, required): The folder path

**Returns**: `Optional<Folder>` - The folder if found, empty otherwise

**Example**:
```java
Optional<Folder> folder = folderService.getFolderByPath("2024-2025/Fall/123");
```

---

## REST API Endpoints

### Create Professor Folder (Manual)

**Endpoint**: `POST /api/deanship/professors/{id}/create-folder`

**Authorization**: Deanship role required

**Path Parameters**:
- `id` (Long): Professor ID

**Query Parameters**:
- `academicYearId` (Long, required): Academic year ID
- `semesterId` (Long, required): Semester ID

**Response**: `ApiResponse<Folder>`

**Example Request**:
```http
POST /api/deanship/professors/123/create-folder?academicYearId=1&semesterId=2
Authorization: Bearer {token}
```

**Example Response**:
```json
{
  "success": true,
  "message": "Professor folder created successfully",
  "data": {
    "id": 789,
    "name": "123",
    "path": "2024-2025/Fall/123",
    "type": "PROFESSOR_FOLDER",
    "createdAt": "2025-11-21T10:30:00"
  },
  "timestamp": "2025-11-21T10:30:00"
}
```

---

### Create Course Folders (Manual)

**Endpoint**: `POST /api/deanship/course-assignments/{id}/create-folders`

**Authorization**: Deanship role required

**Path Parameters**:
- `id` (Long): Course assignment ID

**Response**: `ApiResponse<List<Folder>>`

**Example Request**:
```http
POST /api/deanship/course-assignments/456/create-folders
Authorization: Bearer {token}
```

**Example Response**:
```json
{
  "success": true,
  "message": "Course folders created successfully",
  "data": [
    {
      "id": 790,
      "name": "CS101 - Introduction to Programming",
      "path": "2024-2025/Fall/123/CS101 - Introduction to Programming",
      "type": "COURSE_FOLDER"
    },
    {
      "id": 791,
      "name": "Syllabus",
      "path": "2024-2025/Fall/123/CS101 - Introduction to Programming/Syllabus",
      "type": "SUBFOLDER"
    }
    // ... 3 more subfolders
  ],
  "timestamp": "2025-11-21T10:31:00"
}
```

---

### Refresh File Explorer

**Endpoint**: `POST /api/file-explorer/refresh`

**Authorization**: Authenticated user

**Query Parameters**:
- `academicYearId` (Long, optional): Academic year ID
- `semesterId` (Long, optional): Semester ID

**Response**: `ApiResponse<FileExplorerNode>`

**Behavior**:
- Fetches fresh data from database (no caching)
- Returns updated folder tree structure
- Filters by academic year and semester if provided

**Example Request**:
```http
POST /api/file-explorer/refresh?academicYearId=1&semesterId=2
Authorization: Bearer {token}
```

**Example Response**:
```json
{
  "success": true,
  "message": "File explorer refreshed successfully",
  "data": {
    "name": "Root",
    "path": "/",
    "type": "ROOT",
    "children": [
      {
        "name": "2024-2025",
        "path": "2024-2025",
        "type": "YEAR_FOLDER",
        "children": [...]
      }
    ]
  },
  "timestamp": "2025-11-21T10:32:00"
}
```

---

## Auto-Creation Behavior

### Professor Folder Auto-Creation

**Trigger**: Manual endpoint call (future: automatic on professor creation)

**Process**:
1. Professor is created via `ProfessorService.createProfessor()`
2. Deanship user manually calls `/api/deanship/professors/{id}/create-folder`
3. `FolderService.createProfessorFolder()` is invoked
4. Folder is created in database and file system
5. File Explorer is refreshed (if on File Explorer tab)
6. Toast notification: "Professor folder created"

**Idempotency**: Calling the endpoint multiple times returns the same folder without duplicates

---

### Course Folder Auto-Creation

**Trigger**: Automatic on course assignment

**Process**:
1. Course is assigned to professor via `CourseService.assignCourse()`
2. After successful assignment creation, `FolderService.createCourseFolderStructure()` is automatically invoked
3. Course folder and 4 standard subfolders are created
4. File Explorer is refreshed (if on File Explorer tab)
5. Toast notification: "Course folders created"

**Idempotency**: Assigning the same course multiple times does not create duplicate folders

**Error Handling**: If folder creation fails, the assignment is still created (folder creation failure does not rollback assignment)

---

## Folder Path Conventions

### Professor Folder Path
```
{yearCode}/{semesterType}/{professorId}
```

**Example**: `2024-2025/Fall/123`

**Components**:
- `yearCode`: Academic year code (e.g., "2024-2025")
- `semesterType`: Semester type (e.g., "Fall", "Spring", "Summer")
- `professorId`: Professor's database ID

---

### Course Folder Path
```
{yearCode}/{semesterType}/{professorId}/{courseCode} - {courseName}
```

**Example**: `2024-2025/Fall/123/CS101 - Introduction to Programming`

**Components**:
- `yearCode`: Academic year code
- `semesterType`: Semester type
- `professorId`: Professor's database ID
- `courseCode`: Course code (e.g., "CS101")
- `courseName`: Course name (e.g., "Introduction to Programming")

---

### Subfolder Paths
```
{courseFolderPath}/{subfolderName}
```

**Standard Subfolders**:
1. `Syllabus`
2. `Exams`
3. `Course Notes`
4. `Assignments`

**Example**: `2024-2025/Fall/123/CS101 - Introduction to Programming/Syllabus`

---

## Database Schema

### Folder Entity

**Table**: `folders`

**Key Fields**:
- `id` (Long): Primary key
- `name` (String): Folder name
- `path` (String): Full folder path (unique, indexed)
- `type` (FolderType): Folder type enum
- `parent_id` (Long): Parent folder ID (nullable)
- `owner_id` (Long): Owner user ID (nullable)
- `academic_year_id` (Long): Academic year ID (nullable)
- `semester_id` (Long): Semester ID (nullable)
- `created_at` (Timestamp): Creation timestamp
- `updated_at` (Timestamp): Last update timestamp

**Indexes**:
- `path` (unique index for fast lookups)
- `owner_id` (for filtering by owner)
- `academic_year_id` (for filtering by year)
- `semester_id` (for filtering by semester)

---

## File System Structure

### Physical Directory Layout

```
uploads/
└── 2024-2025/
    ├── Fall/
    │   ├── 123/                                    (Professor folder)
    │   │   ├── CS101 - Introduction to Programming/ (Course folder)
    │   │   │   ├── Syllabus/
    │   │   │   ├── Exams/
    │   │   │   ├── Course Notes/
    │   │   │   └── Assignments/
    │   │   └── CS102 - Data Structures/
    │   │       ├── Syllabus/
    │   │       ├── Exams/
    │   │       ├── Course Notes/
    │   │       └── Assignments/
    │   └── 124/
    │       └── ...
    └── Spring/
        └── ...
```

---

## Error Handling

### Common Exceptions

1. **EntityNotFoundException**
   - Thrown when professor, course, assignment, academic year, or semester not found
   - HTTP Status: 404 Not Found

2. **IOException** (wrapped in RuntimeException)
   - Thrown when file system operation fails
   - HTTP Status: 500 Internal Server Error

3. **DataIntegrityViolationException**
   - Thrown when database constraint violated (rare due to idempotency checks)
   - HTTP Status: 409 Conflict

### Error Response Format

```json
{
  "success": false,
  "message": "Error message",
  "data": null,
  "timestamp": "2025-11-21T10:35:00"
}
```

---

## Performance Considerations

### Database Queries

- Path-based lookups use indexed `path` column for fast retrieval
- Existence checks use `EXISTS` queries for efficiency
- Batch operations minimize database round trips

### File System Operations

- Uses `Files.createDirectories()` for atomic directory creation
- Idempotency checks prevent unnecessary file system operations
- No file system scans - all operations are targeted

### Caching

- No caching implemented (always fetches fresh data)
- Future enhancement: Consider caching folder trees with invalidation on changes

---

## Testing

### Unit Tests

**Location**: `src/test/java/com/alqude/edu/ArchiveSystem/service/FolderServiceTest.java`

**Coverage**:
- Professor folder creation (success case)
- Professor folder creation (idempotency)
- Course folder structure creation (success case)
- Course folder structure creation (partial existing folders)
- Folder existence checks
- Error handling

### Integration Tests

**Location**: `src/test/java/com/alqude/edu/ArchiveSystem/integration/FolderAutoProvisioningIntegrationTest.java`

**Coverage**:
- End-to-end professor folder creation
- End-to-end course folder creation
- Database and file system synchronization
- Idempotency verification
- Cross-dashboard visibility

---

## Migration Guide

### Existing Systems

If migrating from a system without auto-provisioning:

1. **No Breaking Changes**: Existing folders remain unchanged
2. **Gradual Adoption**: Auto-provisioning only affects new professors and courses
3. **Manual Migration**: Use manual endpoints to create folders for existing data
4. **Backward Compatible**: File Explorer works with both auto-created and manually created folders

### Database Migration

No schema changes required - uses existing `folders` table.

---

## Future Enhancements

### Planned Features

1. **Automatic Professor Folder Creation**: Trigger on professor creation (not just manual)
2. **Customizable Subfolders**: Allow departments to define custom subfolder structures
3. **Folder Templates**: Support for different folder templates per department
4. **Bulk Operations**: Create folders for multiple professors/courses at once
5. **Folder Archiving**: Archive folders for past semesters
6. **Folder Permissions**: Fine-grained access control per folder

---

## Support

For questions or issues related to FolderService:
- Check unit tests for usage examples
- Review integration tests for end-to-end scenarios
- Consult developer guide for integration instructions

---

**Last Updated**: November 21, 2025  
**Version**: 1.0.0
