# Data Migration Guide

## Overview

This guide explains how to migrate data from the old request-based schema to the new semester-based file system structure.

## Migration Service

The `DataMigrationService` provides comprehensive migration functionality to transform existing data into the new hierarchical academic structure.

## What Gets Migrated

1. **Academic Years & Semesters**: Created from existing document request deadlines
2. **Professors**: Existing ROLE_PROFESSOR users get unique professor_id values
3. **Courses**: Extracted from unique course names in document requests
4. **Course Assignments**: Links professors to courses within specific semesters
5. **Document Submissions**: Converts old submitted_documents to new structure
6. **Files**: Moves physical files to new hierarchical folder structure
7. **Required Document Types**: Extracts document type requirements per course

## Migration Process

### Step 1: Analyze Existing Data

Before running the migration, analyze your data to understand the scope:

```bash
GET /api/migration/analyze
```

This returns:
- Total document requests
- Year range (extracted from deadlines)
- Unique course names
- Unique professors
- Total submitted documents
- Total file attachments
- Unique document types

### Step 2: Execute Full Migration

Run the complete migration process:

```bash
POST /api/migration/execute
```

This executes all migration steps in sequence:
1. Creates academic years and semesters
2. Migrates professors (generates professor_id)
3. Extracts and creates courses
4. Creates course assignments
5. Migrates document submissions
6. Migrates files to new folder structure
7. Extracts required document types

### Step 3: Verify Migration

After migration completes, verify:
- Academic years exist in the database
- Professors have professor_id values
- Courses are created with proper codes
- Course assignments link professors to courses
- Document submissions reference course assignments
- Files are in new folder structure: `{year}/{semester}/{professorId}/{courseCode}/{documentType}/{filename}`

## Individual Migration Steps

You can also run individual migration steps:

### Create Academic Years
```bash
POST /api/migration/academic-years
```

### Migrate Professors
```bash
POST /api/migration/professors
```

### Extract Courses
```bash
POST /api/migration/courses
```

## File Structure

### Old Structure
```
uploads/
├── file1.pdf
├── file2.pdf
└── file3.pdf
```

### New Structure
```
uploads/
├── 2024-2025/
│   ├── first/
│   │   ├── prof_1_jd/
│   │   │   ├── CS101/
│   │   │   │   ├── syllabus/
│   │   │   │   │   └── syllabus.pdf
│   │   │   │   ├── exam/
│   │   │   │   └── assignment/
│   │   │   └── CS102/
│   │   └── prof_2_ms/
│   ├── second/
│   └── summer/
└── 2025-2026/
```

## Migration Result

The migration returns a `MigrationResult` object containing:

```json
{
  "success": true,
  "errorMessage": null,
  "analysis": {
    "totalRequests": 150,
    "yearRange": [2024, 2025],
    "uniqueCourseNames": ["Database Systems", "Web Development", ...],
    "uniqueProfessorIds": [1, 2, 3, ...],
    "totalSubmittedDocuments": 120,
    "totalFileAttachments": 240,
    "uniqueDocumentTypes": ["Syllabus", "Exam", "Assignment", ...]
  },
  "academicYearsCreated": 2,
  "professorsMigrated": 15,
  "coursesCreated": 25,
  "courseAssignmentsCreated": 45,
  "documentSubmissionsMigrated": 120,
  "filesMigrated": 240,
  "requiredDocumentTypesCreated": 75
}
```

## Important Notes

### Semester Determination

The migration determines which semester a request belongs to based on its deadline month:
- **FIRST Semester (Fall)**: September - December
- **SECOND Semester (Spring)**: January - May
- **SUMMER Semester**: June - August

### Course Code Generation

Course codes are generated from:
- Department prefix (first 2-3 letters)
- Numbers extracted from course name
- Unique suffix if needed to avoid duplicates

Example: "Database Systems" in "Computer Science" → "CS101"

### Professor ID Format

Professor IDs are generated as: `prof_{userId}_{firstInitial}{lastInitial}`

Example: User ID 5, John Doe → `prof_5_jd`

### Document Type Mapping

Old document type strings are mapped to the new enum:
- "Syllabus" → SYLLABUS
- "Exam" → EXAM
- "Assignment" → ASSIGNMENT
- "Project Documents" → PROJECT_DOCS
- "Lecture Notes" → LECTURE_NOTES
- Others → OTHER

### File Migration Safety

- If a physical file cannot be moved, the old path is preserved
- Original file metadata (size, type, order) is maintained
- File attachments are linked to new document submissions

## Rollback Considerations

The migration does NOT delete old data:
- Old tables (document_requests, submitted_documents, file_attachments) remain intact
- Old files remain in their original locations if move fails
- You can manually verify and clean up old data after confirming migration success

## Access Control

All migration endpoints require **DEANSHIP** role:
```java
@PreAuthorize("hasRole('DEANSHIP')")
```

Only Deanship users can execute migrations.

## Troubleshooting

### Migration Fails Partway

Check the logs for specific error messages. The migration is transactional, so partial failures will rollback database changes.

### Files Not Moving

Ensure:
- Upload directory has write permissions
- Sufficient disk space
- File paths are correct in database

### Duplicate Data

The migration checks for existing records before creating new ones. Running migration multiple times is safe - it will skip already migrated data.

## Post-Migration Steps

1. **Verify Data**: Check that all data migrated correctly
2. **Test New System**: Test file upload, download, and navigation
3. **Update Active Year**: Set the current academic year as active
4. **Archive Old Tables**: After verification, you can archive old tables
5. **Update Documentation**: Update user guides for new system

## Example Usage

```bash
# 1. Analyze data
curl -X GET http://localhost:8080/api/migration/analyze \
  -H "Authorization: Bearer {token}"

# 2. Execute migration
curl -X POST http://localhost:8080/api/migration/execute \
  -H "Authorization: Bearer {token}"

# 3. Check result
# Review the returned MigrationResult JSON
```

## Support

For issues or questions about the migration process, contact the system administrator or refer to the technical documentation.
