# Quick Start: HOD Submission Report

## Build & Run

### 1. Install Dependencies
```bash
mvn clean install
```

This will download the iText7 PDF library and compile all new classes.

### 2. Start Application
```bash
mvn spring-boot:run
```

Or run from your IDE.

### 3. Access HOD Dashboard
Navigate to: `http://localhost:8080/hod-dashboard.html`

## Quick Test

### Option 1: Use Existing Data
If you already have HOD users and professors with requests:

1. **Login as HOD**
   - Email: `hod@example.com` (or your HOD email)
   - Password: Your password

2. **View Report**
   - Click "View Report" button
   - Modal opens with statistics

3. **Download PDF**
   - Click "Download PDF" button
   - PDF downloads automatically

### Option 2: Create Test Data

If starting fresh, create test data via API or database:

#### Create Department (if needed)
```sql
INSERT INTO departments (name, created_at, updated_at) 
VALUES ('Computer Science', NOW(), NOW());
```

#### Create HOD User
```sql
INSERT INTO users (email, password, first_name, last_name, role, department_id, is_active, created_at, updated_at)
VALUES ('hod@test.com', '$2a$10$...', 'John', 'Doe', 'ROLE_HOD', 1, true, NOW(), NOW());
```

#### Create Professors
```sql
INSERT INTO users (email, password, first_name, last_name, role, department_id, is_active, created_at, updated_at)
VALUES 
('prof1@test.com', '$2a$10$...', 'Alice', 'Smith', 'ROLE_PROFESSOR', 1, true, NOW(), NOW()),
('prof2@test.com', '$2a$10$...', 'Bob', 'Johnson', 'ROLE_PROFESSOR', 1, true, NOW(), NOW());
```

#### Create Document Requests
```sql
INSERT INTO document_requests (course_name, document_type, deadline, professor_id, created_by, created_at, updated_at)
VALUES 
('Data Structures', 'SYLLABUS', '2025-12-01 23:59:59', 2, 1, NOW(), NOW()),
('Algorithms', 'EXAM', '2025-11-20 23:59:59', 2, 1, NOW(), NOW()),
('Database Systems', 'SYLLABUS', '2025-11-15 23:59:59', 3, 1, NOW(), NOW());
```

#### Create Some Submissions
```sql
INSERT INTO submitted_documents (document_request_id, professor_id, file_name, file_path, file_size, submitted_at, is_late_submission, created_at, updated_at)
VALUES 
(1, 2, 'syllabus.pdf', '/uploads/syllabus.pdf', 102400, NOW(), false, NOW(), NOW());
```

## Verify Installation

### Check Logs
Look for these log messages on startup:
```
INFO  c.a.e.A.service.ReportService - Generating department submission report for current HOD
INFO  c.a.e.A.service.PdfReportService - Generating PDF report for department: Computer Science
```

### Check Endpoints
Test endpoints are accessible:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Report endpoint (requires auth)
curl -X GET http://localhost:8080/api/hod/reports/submission-summary \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Troubleshooting

### Build Fails
**Error:** Cannot resolve iText dependency
**Solution:** 
```bash
mvn clean
mvn dependency:purge-local-repository
mvn install
```

### PDF Generation Error
**Error:** ClassNotFoundException for iText classes
**Solution:** Verify pom.xml has:
```xml
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>7.2.5</version>
    <type>pom</type>
</dependency>
```

### Report Shows No Data
**Possible Causes:**
1. HOD not assigned to department
2. No professors in department
3. No document requests created

**Solution:** Check database:
```sql
SELECT u.*, d.name as dept_name 
FROM users u 
LEFT JOIN departments d ON u.department_id = d.id 
WHERE u.role = 'ROLE_HOD';
```

### 403 Forbidden Error
**Cause:** User doesn't have HOD role
**Solution:** Verify user role in database or login with correct HOD account

## Feature Overview

### What You'll See

**1. Dashboard Section**
- New "Professor Submission Report" card at top
- Two buttons: "View Report" and "Download PDF"

**2. Report Modal**
- Overall statistics (professors, requests, completion rates)
- Detailed professor table with:
  - Name and email
  - Request counts
  - Completion percentage with progress bar
- Scrollable for many professors

**3. PDF Report**
- Professional layout
- Color-coded statistics
- Complete professor breakdown
- Suitable for printing or sharing

## API Endpoints

### GET /api/hod/reports/submission-summary
Returns JSON report data

**Response:**
```json
{
  "success": true,
  "data": {
    "departmentName": "Computer Science",
    "totalProfessors": 5,
    "totalRequests": 25,
    "totalSubmitted": 18,
    "professorSummaries": [...]
  }
}
```

### GET /api/hod/reports/submission-summary/pdf
Downloads PDF file

**Response:** Binary PDF file

## Files Changed

### Backend (Java)
- `dto/report/ProfessorSubmissionSummary.java` ✨ NEW
- `dto/report/DepartmentSubmissionReport.java` ✨ NEW
- `service/ReportService.java` 📝 ENHANCED
- `service/PdfReportService.java` ✨ NEW
- `controller/HodController.java` 📝 ENHANCED
- `pom.xml` 📝 UPDATED

### Frontend (HTML/JS)
- `static/hod-dashboard.html` 📝 ENHANCED
- `static/js/hod.js` 📝 ENHANCED
- `static/js/api.js` 📝 ENHANCED

## Need Help?

Check these resources:
1. `HOD_SUBMISSION_REPORT_FEATURE.md` - Complete feature documentation
2. `TESTING_SUBMISSION_REPORT.md` - Detailed testing guide
3. Application logs in `logs/archive-system.log`
4. Browser console for frontend errors

## Success Indicators

✅ Application starts without errors
✅ HOD dashboard loads successfully
✅ "View Report" button opens modal with data
✅ "Download PDF" button downloads file
✅ PDF opens and displays correctly
✅ All metrics are accurate
✅ No console errors

## Next Steps

1. Test with your actual data
2. Review PDF formatting
3. Verify all metrics are accurate
4. Share with stakeholders for feedback
5. Deploy to production when ready

---

**Feature Status:** ✅ Complete and Ready for Testing
**Last Updated:** November 14, 2025
