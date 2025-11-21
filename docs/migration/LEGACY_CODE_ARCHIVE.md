# Legacy Code Archive Documentation

## Overview

This document describes the legacy code that has been archived as part of the migration from the request-based document system to the semester-based file exploration system.

**Migration Date:** November 18, 2025  
**System Version:** 2.0

## Purpose of Archiving

The legacy code is preserved for the following reasons:

1. **Historical Data Access**: Allows querying and viewing historical data from the old system
2. **Rollback Capability**: Enables system rollback if critical issues are discovered
3. **Data Migration Reference**: Serves as reference for understanding data migration logic
4. **Audit Trail**: Maintains complete history of system evolution

## Archived Components

### 1. Legacy Entities

#### DocumentRequest
- **Location**: `src/main/java/com/alqude/edu/ArchiveSystem/entity/DocumentRequest.java`
- **Status**: Deprecated, marked with `@Deprecated(since = "2.0", forRemoval = false)`
- **Database Table**: `document_requests` (archived with comments)
- **Replaced By**: 
  - `CourseAssignment` - Links professors to courses within semesters
  - `RequiredDocumentType` - Defines required document types per course

**Key Differences:**
- Old: HOD creates ad-hoc document requests for professors
- New: Deanship manages academic structure; courses are assigned to professors per semester

#### SubmittedDocument
- **Location**: `src/main/java/com/alqude/edu/ArchiveSystem/entity/SubmittedDocument.java`
- **Status**: Deprecated, marked with `@Deprecated(since = "2.0", forRemoval = false)`
- **Database Table**: `submitted_documents` (archived with comments)
- **Replaced By**: `DocumentSubmission` - Tracks submissions within semester-based structure

**Key Differences:**
- Old: Submissions linked to document requests
- New: Submissions linked to course assignments and organized by semester

#### FileAttachment
- **Location**: `src/main/java/com/alqude/edu/ArchiveSystem/entity/FileAttachment.java`
- **Status**: Deprecated, marked with `@Deprecated(since = "2.0", forRemoval = false)`
- **Database Table**: `file_attachments` (archived with comments)
- **Replaced By**: `UploadedFile` - Stores file metadata in hierarchical structure

**Key Differences:**
- Old: Files linked to submitted documents
- New: Files organized in hierarchical folder structure (Year/Semester/Professor/Course/DocumentType)

### 2. Legacy Repositories

#### DocumentRequestRepository
- **Location**: `src/main/java/com/alqude/edu/ArchiveSystem/repository/DocumentRequestRepository.java`
- **Status**: Deprecated
- **Replaced By**: `CourseAssignmentRepository`, `RequiredDocumentTypeRepository`

#### SubmittedDocumentRepository
- **Location**: `src/main/java/com/alqude/edu/ArchiveSystem/repository/SubmittedDocumentRepository.java`
- **Status**: Deprecated
- **Replaced By**: `DocumentSubmissionRepository`

#### FileAttachmentRepository
- **Location**: `src/main/java/com/alqude/edu/ArchiveSystem/repository/FileAttachmentRepository.java`
- **Status**: Deprecated
- **Replaced By**: `UploadedFileRepository`

### 3. Legacy Services

#### DocumentRequestService
- **Location**: `src/main/java/com/alqude/edu/ArchiveSystem/service/DocumentRequestService.java`
- **Status**: Deprecated
- **Replaced By**: 
  - `CourseService` - Manages courses and assignments
  - `SubmissionService` - Manages document submissions
  - `FileService` - Handles file operations

**Key Methods (Archived):**
- `createDocumentRequest()` → Use `CourseService.assignCourse()` + `CourseService.addRequiredDocumentType()`
- `getDocumentRequestsByProfessor()` → Use `CourseService.getAssignmentsByProfessor()`
- `deleteDocumentRequest()` → Use `CourseService.unassignCourse()`

### 4. Legacy Mappers

#### DocumentRequestMapper
- **Location**: `src/main/java/com/alqude/edu/ArchiveSystem/mapper/DocumentRequestMapper.java`
- **Status**: Deprecated
- **Type**: MapStruct interface

#### DocumentRequestMapperManual
- **Location**: `src/main/java/com/alqude/edu/ArchiveSystem/mapper/DocumentRequestMapperManual.java`
- **Status**: Deprecated
- **Type**: Manual implementation

### 5. Legacy Exceptions

#### DocumentRequestException
- **Location**: `src/main/java/com/alqude/edu/ArchiveSystem/exception/DocumentRequestException.java`
- **Status**: Deprecated
- **Replaced By**: Standard exceptions (`ResourceNotFoundException`, `ValidationException`, etc.)

## Database Migration

### Archived Tables

The following tables have been archived with comments and metadata:

1. **document_requests**
   - Comment added indicating legacy status
   - `archived_at` column added with timestamp
   - View created: `v_legacy_document_requests`

2. **submitted_documents**
   - Comment added indicating legacy status
   - `archived_at` column added with timestamp
   - View created: `v_legacy_submitted_documents`

3. **file_attachments**
   - Comment added indicating legacy status
   - `archived_at` column added with timestamp

### Migration Script

**Location**: `src/main/resources/db/migration/V6__archive_legacy_tables.sql`

**Key Actions:**
- Adds table comments marking them as legacy
- Creates `archived_at` columns
- Creates views for legacy data access
- Adds indexes for performance
- Records migration metadata in `system_metadata` table

### Accessing Legacy Data

Legacy data can be accessed through:

1. **Direct Repository Access** (for migration/admin purposes only):
   ```java
   @Autowired
   private DocumentRequestRepository legacyRepository; // Deprecated
   ```

2. **Database Views** (recommended for read-only access):
   ```sql
   SELECT * FROM v_legacy_document_requests WHERE professor_id = ?;
   SELECT * FROM v_legacy_submitted_documents WHERE document_request_id = ?;
   ```

3. **System Metadata**:
   ```sql
   SELECT * FROM system_metadata WHERE metadata_key = 'legacy_tables_archived';
   ```

## Migration Mapping

### Conceptual Mapping

| Legacy Concept | New Concept | Notes |
|----------------|-------------|-------|
| Document Request | Course Assignment + Required Document Type | Requests are now structured by academic calendar |
| HOD creates request | Deanship manages structure | Centralized academic management |
| Professor receives request | Professor assigned to course | Assignment-based workflow |
| Submitted Document | Document Submission | Linked to course assignment |
| File Attachment | Uploaded File | Hierarchical folder structure |

### Data Migration

The data migration process (implemented in `DataMigrationService`) performs:

1. **Academic Year Creation**: Extracts years from request deadlines
2. **Semester Creation**: Creates three semesters per academic year
3. **Professor Migration**: Generates `professor_id` for existing professors
4. **Course Extraction**: Creates courses from unique course names in requests
5. **Course Assignment**: Links professors to courses within semesters
6. **Submission Migration**: Converts submitted documents to new structure
7. **File Migration**: Moves files to hierarchical folder structure
8. **Document Type Extraction**: Creates required document types per course

## Usage Guidelines

### DO NOT Use Legacy Code For:
- ❌ New feature development
- ❌ Bug fixes in new functionality
- ❌ API endpoint creation
- ❌ UI component development

### DO Use Legacy Code For:
- ✅ Historical data queries (read-only)
- ✅ Data migration operations
- ✅ Rollback procedures
- ✅ Understanding system evolution
- ✅ Audit and compliance reporting

## Rollback Procedure

If rollback to the legacy system is required:

1. **Stop Application**: Shut down all application instances
2. **Database Rollback**: 
   ```sql
   -- Restore from backup or use Flyway rollback
   flyway undo
   ```
3. **Code Rollback**: Revert to previous Git tag/commit
4. **File System**: Restore file structure from backup
5. **Verification**: Run integration tests
6. **Documentation**: Update system status

## Removal Timeline

The legacy code is marked with `forRemoval = false`, indicating it will be retained indefinitely for historical purposes. However, the following timeline applies:

- **Current (v2.0)**: Legacy code deprecated but functional
- **v2.1-v2.5**: Legacy code maintained for rollback capability
- **v3.0 (Future)**: Evaluate removal based on:
  - Data migration success rate
  - System stability
  - Regulatory requirements
  - Business needs

## Support and Questions

For questions about legacy code or migration:

1. Review this documentation
2. Check `LEGACY_SYSTEM_DOCUMENTATION.md` for detailed legacy system description
3. Review migration logs in `DataMigrationService`
4. Contact system administrators

## Related Documentation

- `LEGACY_SYSTEM_DOCUMENTATION.md` - Detailed description of old system
- `ARCHITECTURE.md` - New system architecture
- `API_DOCUMENTATION.md` - New API endpoints
- `MIGRATION_GUIDE.md` - Step-by-step migration guide
- Database migration scripts in `src/main/resources/db/migration/`

## Conclusion

The legacy code archive ensures system continuity while enabling modern functionality. All deprecated components are clearly marked and documented, allowing developers to understand the system's evolution while preventing accidental use of outdated patterns.

**Remember**: When in doubt, use the new semester-based system components. Legacy code is for reference and rollback only.
