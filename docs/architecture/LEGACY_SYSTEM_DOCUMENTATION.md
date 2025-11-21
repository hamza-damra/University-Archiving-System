# Legacy System Documentation

## ⚠️ ARCHIVED SYSTEM - DO NOT USE FOR NEW DEVELOPMENT

## Overview

This document describes the legacy request-based document archiving system that was replaced by the semester-based file exploration system. The legacy system is archived but tables are retained for rollback capability and historical data access.

**Migration Date:** November 18, 2025  
**Migration Version:** V6__archive_legacy_tables.sql  
**System Status:** ARCHIVED - Code deprecated, tables preserved  
**Current System Version:** 2.0 (Semester-based)

## Archival Status

All legacy code components have been marked with `@Deprecated(since = "2.0", forRemoval = false)` annotations and comprehensive documentation comments. The following components are archived:

- ✅ **Entities**: DocumentRequest, SubmittedDocument, FileAttachment
- ✅ **Repositories**: DocumentRequestRepository, SubmittedDocumentRepository, FileAttachmentRepository
- ✅ **Services**: DocumentRequestService
- ✅ **Mappers**: DocumentRequestMapper, DocumentRequestMapperManual
- ✅ **Exceptions**: DocumentRequestException
- ✅ **Database Tables**: Marked with comments and archived_at timestamps

**For detailed archival information, see:** `LEGACY_CODE_ARCHIVE.md`

---

## Legacy System Architecture

### Request-Based Model

The legacy system used a request-based workflow:

1. **HOD creates document request** → Professor receives request
2. **Professor uploads document** → HOD reviews submission
3. **HOD tracks compliance** → Reports generated

### Legacy Database Tables

#### 1. document_requests

**Purpose:** Stored document requests created by HODs for professors

**Key Columns:**
- `id`: Primary key
- `course_name`: Name of the course (free text, not normalized)
- `document_type`: Type of document requested
- `deadline`: Submission deadline
- `professor_id`: Foreign key to users table
- `hod_id`: Foreign key to users table (HOD who created request)
- `status`: Request status (PENDING, SUBMITTED, OVERDUE)
- `max_file_count`: Maximum number of files allowed
- `max_total_size_mb`: Maximum total size in MB

**Limitations:**
- Course names were free text, leading to inconsistencies
- No academic year or semester structure
- No course code standardization
- Difficult to track historical data across semesters

#### 2. submitted_documents

**Purpose:** Stored documents uploaded by professors in response to requests

**Key Columns:**
- `id`: Primary key
- `document_request_id`: Foreign key to document_requests
- `professor_id`: Foreign key to users table
- `file_url`: Path to uploaded file
- `original_filename`: Original name of uploaded file
- `file_size`: Size of file in bytes
- `file_type`: MIME type
- `file_count`: Number of files in submission
- `total_file_size`: Total size of all files
- `notes`: Optional notes from professor
- `submitted_at`: Submission timestamp
- `is_late_submission`: Boolean flag for late submissions

**Limitations:**
- Single submission per request (no versioning)
- Flat file storage structure
- No hierarchical organization
- Difficult to browse files by semester or course

#### 3. file_attachments

**Purpose:** Stored multiple files for a single submission (multi-file support)

**Key Columns:**
- `id`: Primary key
- `submitted_document_id`: Foreign key to submitted_documents
- `file_url`: Path to uploaded file
- `original_filename`: Original name of file
- `file_size`: Size of file in bytes
- `file_type`: MIME type
- `file_order`: Order of file in submission
- `description`: Optional file description

**Limitations:**
- Tied to request-based model
- No semester-based organization
- Difficult to implement file explorer

---

## New Semester-Based System

### Improvements

The new system addresses legacy limitations:

1. **Academic Structure:**
   - Formal academic years and semesters
   - Standardized course codes and names
   - Department-based organization

2. **Hierarchical File Organization:**
   - Year → Semester → Professor → Course → Document Type → Files
   - Easy navigation and browsing
   - Consistent file paths

3. **Role-Based Access:**
   - Three distinct roles (Deanship, HOD, Professor)
   - Clear permission boundaries
   - Department-scoped access for HOD and Professor

4. **Centralized Management:**
   - Deanship manages academic structure
   - HOD monitors department compliance
   - Professor uploads to assigned courses

### New Database Tables

#### Replacement Mapping

| Legacy Table | New Table(s) | Purpose |
|--------------|--------------|---------|
| `document_requests` | `course_assignments` + `required_document_types` | Separate course assignments from document requirements |
| `submitted_documents` | `document_submissions` | Link submissions to course assignments |
| `file_attachments` | `uploaded_files` | Store files with hierarchical paths |

#### New Tables

1. **academic_years**: Academic year records (e.g., 2024-2025)
2. **semesters**: Semester records (First, Second, Summer)
3. **courses**: Standardized course records with codes
4. **course_assignments**: Links professors to courses for semesters
5. **required_document_types**: Defines required documents per course
6. **document_submissions**: Tracks document submissions
7. **uploaded_files**: Stores individual files with metadata

---

## Data Migration

### Migration Process

The migration from legacy to new system involved:

1. **Academic Year Creation:**
   - Analyzed document_requests to determine year ranges
   - Created academic year records
   - Created three semesters per year

2. **Professor Migration:**
   - Generated professor_id for existing professors
   - Updated user records with new field

3. **Course Extraction:**
   - Extracted unique course names from document_requests
   - Generated course codes
   - Created course records

4. **Course Assignment Creation:**
   - Mapped document_requests to course_assignments
   - Determined semester based on deadline dates
   - Linked professors to courses for semesters

5. **Document Submission Migration:**
   - Converted submitted_documents to document_submissions
   - Linked to course_assignments
   - Preserved submission dates and late flags

6. **File Migration:**
   - Moved files to hierarchical structure
   - Created uploaded_files records
   - Updated file paths in database

7. **Required Document Type Extraction:**
   - Extracted document types from document_requests
   - Created required_document_type records per course

### Migration Script

The migration was executed via:
- **Endpoint:** `/api/admin/migrate`
- **Service:** `MigrationService`
- **Methods:** See `MigrationController.java`

---

## Legacy Data Access

### Accessing Archived Data

Legacy tables are retained and can be accessed if needed:

**Direct Table Access:**
```sql
SELECT * FROM document_requests WHERE archived_at IS NOT NULL;
SELECT * FROM submitted_documents WHERE archived_at IS NOT NULL;
SELECT * FROM file_attachments WHERE archived_at IS NOT NULL;
```

**Using Views:**
```sql
-- View legacy document requests
SELECT * FROM v_legacy_document_requests;

-- View legacy submitted documents
SELECT * FROM v_legacy_submitted_documents;
```

### Legacy Data Queries

**Find all requests for a professor:**
```sql
SELECT * FROM v_legacy_document_requests 
WHERE professor_id = ?
ORDER BY created_at DESC;
```

**Find all submissions for a request:**
```sql
SELECT * FROM v_legacy_submitted_documents 
WHERE document_request_id = ?;
```

**Find all file attachments for a submission:**
```sql
SELECT * FROM file_attachments 
WHERE submitted_document_id = ?
ORDER BY file_order ASC;
```

---

## Rollback Capability

### Why Keep Legacy Tables?

Legacy tables are retained for:

1. **Historical Data:** Preserve all historical submissions
2. **Rollback:** Ability to revert to legacy system if needed
3. **Audit Trail:** Maintain complete record of system evolution
4. **Data Verification:** Compare legacy and new data for accuracy

### Rollback Process

If rollback is necessary:

1. **Disable New System:**
   - Comment out new controller endpoints
   - Disable new frontend pages

2. **Re-enable Legacy System:**
   - Uncomment legacy controller methods
   - Restore legacy frontend pages
   - Update navigation

3. **Data Sync:**
   - Sync any new data from new system to legacy tables
   - Verify data integrity

4. **Testing:**
   - Test legacy workflows
   - Verify file access
   - Check reports

**Note:** Rollback should only be performed by system administrators with database backup.

---

## Legacy Code Location

### Backend Code

Legacy controller methods are commented out in:
- `HodController.java` - Legacy document request management
- `ProfessorController.java` - Legacy file upload methods

**Example:**
```java
// LEGACY METHOD - Archived
// Replaced by semester-based upload in uploadFiles()
/*
@PostMapping("/document-requests/{requestId}/upload")
public ResponseEntity<ApiResponse<SubmittedDocumentResponse>> uploadDocument(
        @PathVariable Long requestId,
        @RequestParam("file") MultipartFile file) throws IOException {
    // Legacy upload logic
}
*/
```

### Frontend Code

Legacy frontend pages (if any) are moved to:
- `src/main/resources/static/legacy/` directory

### Service Layer

Legacy service classes:
- `DocumentRequestService.java` - Still active for legacy data access
- `FileUploadService.java` - Still active for legacy file operations
- `MultiFileUploadService.java` - Still active for legacy multi-file support

**Note:** These services are retained to support legacy data queries and potential rollback.

---

## Comparison: Legacy vs New System

### Feature Comparison

| Feature | Legacy System | New System |
|---------|---------------|------------|
| **Academic Structure** | No formal structure | Academic years and semesters |
| **Course Management** | Free text course names | Standardized course codes |
| **File Organization** | Flat structure | Hierarchical (Year/Semester/Professor/Course) |
| **Role Management** | 2 roles (HOD, Professor) | 3 roles (Deanship, HOD, Professor) |
| **Access Control** | Request-based | Department-scoped |
| **File Explorer** | Not available | Full hierarchical navigation |
| **Reports** | Basic submission list | Comprehensive semester reports |
| **Professor Management** | HOD creates professors | Deanship creates professors |
| **Course Assignment** | Implicit in requests | Explicit course assignments |
| **Document Requirements** | Per request | Per course with semester override |

### Workflow Comparison

**Legacy Workflow:**
```
HOD creates request → Professor uploads → HOD reviews
```

**New Workflow:**
```
Deanship creates structure → Deanship assigns courses → 
Professor uploads → HOD monitors → Reports generated
```

---

## Best Practices for Legacy Data

### Do's

✅ **Keep legacy tables intact:** Do not drop or modify legacy tables  
✅ **Use views for queries:** Use provided views for legacy data access  
✅ **Document any changes:** If legacy data needs updating, document thoroughly  
✅ **Maintain backups:** Regular backups of legacy tables  
✅ **Test rollback:** Periodically test rollback procedures

### Don'ts

❌ **Don't delete legacy tables:** Needed for rollback and history  
❌ **Don't modify legacy data:** Could break rollback capability  
❌ **Don't use legacy tables for new data:** Use new system only  
❌ **Don't remove legacy code:** Keep commented for reference  
❌ **Don't bypass migration:** Always use proper migration scripts

---

## Future Considerations

### When to Remove Legacy Tables

Legacy tables can be removed when:

1. **Sufficient Time Passed:** At least 2-3 years after migration
2. **Data Archived:** All legacy data exported and archived externally
3. **Rollback Not Needed:** Confidence in new system stability
4. **Stakeholder Approval:** University administration approves removal

### Removal Process

When ready to remove legacy tables:

1. **Export Data:** Export all legacy data to external archive
2. **Create Backup:** Full database backup before removal
3. **Drop Tables:** Execute DROP TABLE statements
4. **Remove Code:** Delete commented legacy code
5. **Update Documentation:** Mark legacy system as fully retired

---

## Support and Questions

For questions about legacy data or migration:

- **System Administrator:** admin@university.edu
- **Database Administrator:** dba@university.edu
- **IT Support:** support@university.edu

**Last Updated:** November 18, 2025
