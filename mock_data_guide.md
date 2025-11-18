# Mock Data Guide

## Overview

The University Archive System includes a comprehensive mock data generation system that automatically populates the database with realistic test data. This enables thorough testing and demonstration of all system features without manual data entry.

The `DataInitializer` component runs on application startup and creates a complete dataset covering all entities in both the legacy request-based system and the new semester-based system.

---

## Entity Types and Counts

### Academic Structure

| Entity | Count | Description |
|--------|-------|-------------|
| **Academic Years** | 3 | Sequential years (2023-2024, 2024-2025, 2025-2026) |
| **Semesters** | 9 | 3 per academic year (Fall, Spring, Summer) |
| **Departments** | 5 | Computer Science, Mathematics, Physics, Engineering, Business Administration |
| **Courses** | 15 | 3 courses per department with realistic codes and names |

### Users

| Entity | Count | Description |
|--------|-------|-------------|
| **HOD Users** | 5 | One Head of Department per department |
| **Professor Users** | 25 | 5 professors per department (80% active, 20% inactive) |
| **Total Users** | 30 | All with default password: `password123` |

### Course System

| Entity | Count | Description |
|--------|-------|-------------|
| **Course Assignments** | 60+ | Professors assigned to courses across all semesters |
| **Required Document Types** | 90+ | 6 document types per course (Syllabus, Exam, Assignment, etc.) |

### Document System

| Entity | Count | Description |
|--------|-------|-------------|
| **Document Submissions** | 100+ | Varied statuses: 70% uploaded, 20% not uploaded, 10% overdue |
| **Uploaded Files** | 150+ | 1-3 files per submission with realistic metadata |

### Notifications

| Entity | Count | Description |
|--------|-------|-------------|
| **Notifications** | 75+ | Various types distributed across professors (60% read, 40% unread) |

---

## Data Relationships and Structure

### Academic Hierarchy

```
Academic Year (2024-2025)
├── Semester: Fall 2024
│   ├── Course Assignment: CS101 - Prof. Ahmad
│   │   ├── Required Document: Syllabus (due Sep 15)
│   │   ├── Required Document: Midterm Exam (due Nov 1)
│   │   └── Required Document: Final Exam (due Jan 10)
│   └── Course Assignment: CS201 - Prof. Fatima
│       └── ...
├── Semester: Spring 2025
│   └── ...
└── Semester: Summer 2025
    └── ...
```

### Department Structure

```
Department: Computer Science
├── HOD: hod.cs@alquds.edu
├── Courses:
│   ├── CS101: Introduction to Programming
│   ├── CS201: Data Structures and Algorithms
│   └── CS301: Advanced Software Engineering
└── Professors:
    ├── PCS001: prof.{name}@alquds.edu (Active)
    ├── PCS002: prof.{name}@alquds.edu (Active)
    ├── PCS003: prof.{name}@alquds.edu (Active)
    ├── PCS004: prof.{name}@alquds.edu (Active)
    └── PCS005: prof.{name}@alquds.edu (Inactive)
```

### Submission Workflow

```
Course Assignment
├── Required Document Type: SYLLABUS
│   └── Document Submission
│       ├── Status: UPLOADED
│       ├── Submitted At: 2024-09-10
│       ├── Is Late: false
│       └── Uploaded Files:
│           ├── syllabus.pdf (1.2 MB)
│           └── course_outline.pdf (800 KB)
└── Required Document Type: EXAM
    └── Document Submission
        ├── Status: NOT_UPLOADED
        └── Deadline: 2024-12-20
```

---

## Configuration

### Enabling/Disabling Mock Data

Mock data generation can be controlled through application properties:

```properties
# application.properties
mock.data.enabled=true
```

For production environments, disable mock data:

```properties
# application-prod.properties
mock.data.enabled=false
```

### Idempotency

The DataInitializer checks for existing data before creating mock data:

- If any academic years, departments, or users exist, mock data generation is skipped
- This prevents duplicate data on application restarts
- Logs existing data counts if found

### Logging

Monitor mock data creation through application logs:

```
INFO  DataInitializer - Starting comprehensive mock data generation...
INFO  DataInitializer - Creating academic years...
INFO  DataInitializer - Created 3 academic years
INFO  DataInitializer - Creating semesters...
INFO  DataInitializer - Created 9 semesters
INFO  DataInitializer - Creating departments...
INFO  DataInitializer - Created 5 departments
...
```

---

## Departments and Courses

### Computer Science (CS)

**HOD:** hod.cs@alquds.edu

**Courses:**
- CS101: Introduction to Programming (Undergraduate)
- CS201: Data Structures and Algorithms (Undergraduate)
- CS301: Advanced Software Engineering (Graduate)

**Professors:** 5 (PCS001 - PCS005)

### Mathematics (MATH)

**HOD:** hod.math@alquds.edu

**Courses:**
- MATH101: Calculus I (Undergraduate)
- MATH201: Linear Algebra (Undergraduate)
- MATH301: Advanced Mathematical Analysis (Graduate)

**Professors:** 5 (PMATH001 - PMATH005)

### Physics (PHYS)

**HOD:** hod.phys@alquds.edu

**Courses:**
- PHYS101: General Physics I (Undergraduate)
- PHYS201: Electromagnetism (Undergraduate)
- PHYS301: Quantum Mechanics (Graduate)

**Professors:** 5 (PPHYS001 - PPHYS005)

### Engineering (ENG)

**HOD:** hod.eng@alquds.edu

**Courses:**
- ENG101: Engineering Fundamentals (Undergraduate)
- ENG201: Circuit Analysis (Undergraduate)
- ENG301: Advanced Control Systems (Graduate)

**Professors:** 5 (PENG001 - PENG005)

### Business Administration (BUS)

**HOD:** hod.bus@alquds.edu

**Courses:**
- BUS101: Introduction to Business (Undergraduate)
- BUS201: Financial Management (Undergraduate)
- BUS301: Strategic Management (Graduate)

**Professors:** 5 (PBUS001 - PBUS005)

---

## User Accounts

### Default Password

All mock accounts use the same default password:
- **Password:** `password123`

⚠️ **Security Warning:** Never use these accounts in production! See `MOCK_ACCOUNTS.md` for security guidelines.

### HOD Accounts

| Email | Department | Role |
|-------|------------|------|
| hod.cs@alquds.edu | Computer Science | ROLE_HOD |
| hod.math@alquds.edu | Mathematics | ROLE_HOD |
| hod.phys@alquds.edu | Physics | ROLE_HOD |
| hod.eng@alquds.edu | Engineering | ROLE_HOD |
| hod.bus@alquds.edu | Business Administration | ROLE_HOD |

### Professor Accounts

**Format:**
- Email: `prof.{firstname}.{lastname}@alquds.edu`
- Professor ID: `P{DEPT}{###}` (e.g., PCS001, PMATH002)
- Names: Randomly generated Arabic names

**Distribution:**
- 25 total professors (5 per department)
- 20 active professors (80%)
- 5 inactive professors (20%) - for testing filters

**Example Professors:**
- prof.ahmad.alnajjar@alquds.edu (PCS001, Active, Computer Science)
- prof.fatima.almasri@alquds.edu (PCS002, Active, Computer Science)
- prof.omar.alkhouri@alquds.edu (PCS005, Inactive, Computer Science)

For complete list, see `MOCK_ACCOUNTS.md` or check application logs.

---

## Document Types and Deadlines

Each course has 6 required document types with calculated deadlines:

| Document Type | Deadline Calculation | Allowed Extensions | Max Files | Max Size |
|---------------|---------------------|-------------------|-----------|----------|
| **SYLLABUS** | 2 weeks after semester start | pdf, docx | 5 | 50 MB |
| **EXAM** | Mid-semester / End of semester | pdf | 5 | 50 MB |
| **ASSIGNMENT** | Throughout semester (weekly) | pdf, zip | 5 | 50 MB |
| **PROJECT_DOCS** | End of semester | zip, pdf | 5 | 50 MB |
| **LECTURE_NOTES** | Weekly throughout semester | pdf, pptx | 5 | 50 MB |
| **OTHER** | Flexible | pdf, docx, zip | 5 | 50 MB |

---

## Submission Status Distribution

Mock submissions are distributed across different statuses to enable comprehensive testing:

| Status | Percentage | Description |
|--------|-----------|-------------|
| **UPLOADED** | 70% | Documents submitted on time or late |
| **NOT_UPLOADED** | 20% | No documents submitted yet |
| **OVERDUE** | 10% | Deadline passed, no submission or late submission |

**Additional Characteristics:**
- 15% of submissions are marked as late
- Uploaded submissions have 1-3 files each
- File sizes range from 100 KB to 10 MB
- Submission dates are distributed throughout semester periods

---

## Notification Types

Notifications are distributed across professors with various types:

| Type | Percentage | Description |
|------|-----------|-------------|
| **NEW_REQUEST** | 30% | New course assignment created |
| **REQUEST_REMINDER** | 20% | Periodic reminder about pending submissions |
| **DEADLINE_APPROACHING** | 25% | Deadline is 3 days away |
| **DOCUMENT_SUBMITTED** | 15% | Confirmation after submission |
| **DOCUMENT_OVERDUE** | 10% | Deadline has passed |

**Characteristics:**
- 60% of notifications are marked as read
- 40% are unread (for testing notification badges)
- Timestamps distributed over last 30 days
- Each notification links to related course assignment or submission

---

## How to Use Mock Data

### 1. Automatic Initialization

Simply start the application:

```bash
mvn spring-boot:run
```

The DataInitializer runs automatically and creates all mock data if the database is empty.

### 2. Manual Reset

To regenerate mock data:

1. Clear the database:
   ```sql
   -- Delete all data (be careful!)
   DELETE FROM uploaded_files;
   DELETE FROM document_submissions;
   DELETE FROM required_document_types;
   DELETE FROM course_assignments;
   DELETE FROM courses;
   DELETE FROM semesters;
   DELETE FROM academic_years;
   DELETE FROM notifications;
   DELETE FROM submitted_documents;
   DELETE FROM document_requests;
   DELETE FROM users;
   DELETE FROM departments;
   ```

2. Restart the application - mock data will be regenerated

### 3. Check Existing Data

Query the database to see what was created:

```sql
-- Count entities
SELECT 'Academic Years' as entity, COUNT(*) as count FROM academic_years
UNION ALL
SELECT 'Semesters', COUNT(*) FROM semesters
UNION ALL
SELECT 'Departments', COUNT(*) FROM departments
UNION ALL
SELECT 'Courses', COUNT(*) FROM courses
UNION ALL
SELECT 'Users', COUNT(*) FROM users
UNION ALL
SELECT 'Course Assignments', COUNT(*) FROM course_assignments
UNION ALL
SELECT 'Document Submissions', COUNT(*) FROM document_submissions
UNION ALL
SELECT 'Uploaded Files', COUNT(*) FROM uploaded_files
UNION ALL
SELECT 'Notifications', COUNT(*) FROM notifications;
```

---

## Testing Scenarios

### Scenario 1: HOD Department Management

1. Login as HOD: `hod.cs@alquds.edu` / `password123`
2. View all professors in Computer Science department
3. Filter by active/inactive status
4. View professor submission statistics
5. Generate department reports

### Scenario 2: Professor Document Submission

1. Login as Professor: Use any active professor email / `password123`
2. View assigned courses across semesters
3. See required document types and deadlines
4. Submit documents with file uploads
5. View submission history

### Scenario 3: Multi-Semester Testing

1. Login as any professor
2. View assignments for Fall 2024-2025
3. Switch to Spring 2024-2025
4. Compare document requirements across semesters
5. Test semester-based filtering

### Scenario 4: Notification System

1. Login as any professor
2. Check notification dropdown
3. View unread notifications (40% of total)
4. Mark notifications as read
5. Verify notification types and links

### Scenario 5: Submission Status Testing

1. Login as HOD
2. Generate department report
3. Filter by submission status:
   - View uploaded submissions (70%)
   - View pending submissions (20%)
   - View overdue submissions (10%)
4. Drill down to specific professors

### Scenario 6: Cross-Department Testing

1. Login as `hod.cs@alquds.edu`
2. Verify you only see CS professors
3. Logout and login as `hod.math@alquds.edu`
4. Verify you only see Math professors
5. Test department isolation

---

## API Testing

For detailed API testing examples, see `MOCK_DATA_API_TESTING.md`.

Quick examples:

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"hod.cs@alquds.edu","password":"password123"}'

# Get professors (as HOD)
curl -X GET http://localhost:8080/api/hod/professors \
  -H "Authorization: Bearer {token}"

# Get assignments (as Professor)
curl -X GET http://localhost:8080/api/professor/assignments \
  -H "Authorization: Bearer {token}"
```

---

## Troubleshooting

### Mock Data Not Created

**Problem:** Application starts but no mock data appears

**Solutions:**
1. Check if data already exists (DataInitializer skips if data found)
2. Check application logs for errors
3. Verify database connection is working
4. Check `mock.data.enabled` property is true

### Duplicate Data

**Problem:** Running application multiple times creates duplicate data

**Solution:** DataInitializer checks for existing data and skips creation. If you see duplicates, there may be an issue with the idempotency check.

### Wrong Password

**Problem:** Cannot login with mock accounts

**Solution:** All mock accounts use password `password123`. Ensure you're using the correct email format.

### Missing Professors

**Problem:** HOD cannot see all professors

**Solution:** Verify you're logged in as HOD for the correct department. HODs can only see professors in their own department.

---

## Additional Resources

- **MOCK_ACCOUNTS.md** - Complete list of all mock accounts with testing scenarios
- **MOCK_DATA_API_TESTING.md** - Detailed API testing guide with curl examples
- **Application Logs** - Check logs for detailed information about mock data creation
- **Database Schema** - Review entity relationships in the database

---

## Summary

The mock data system provides:

✅ **Comprehensive Coverage** - All entities from academic structure to notifications
✅ **Realistic Data** - Names, dates, and relationships mirror production scenarios
✅ **Testing Ready** - Varied statuses and distributions for thorough testing
✅ **Idempotent** - Safe to restart application without duplicating data
✅ **Configurable** - Easy to enable/disable for different environments
✅ **Well-Documented** - Complete guides for accounts, API testing, and usage

Start testing immediately with 30 ready-to-use accounts and 300+ entities!
