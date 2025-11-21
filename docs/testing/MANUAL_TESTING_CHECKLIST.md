# Manual Testing Checklist

This document provides comprehensive manual testing scenarios for the University Archive System using the generated mock data. Use this checklist to verify all features work correctly with realistic test data.

## Prerequisites

- Application is running with mock data enabled (`mock.data.enabled=true`)
- Database has been initialized with mock data
- Default password for all accounts: `password123`
- Refer to [MOCK_ACCOUNTS.md](MOCK_ACCOUNTS.md) for account credentials

---

## 1. Login Testing Scenarios

### 1.1 HOD Login Tests

**Test Case 1.1.1: Login as Computer Science HOD**
- [ ] Navigate to login page
- [ ] Enter email: `hod.cs@alquds.edu`
- [ ] Enter password: `password123`
- [ ] Click login
- [ ] **Expected**: Successfully logged in, redirected to HOD dashboard
- [ ] **Expected**: Dashboard shows Computer Science department data

**Test Case 1.1.2: Login as Mathematics HOD**
- [ ] Navigate to login page
- [ ] Enter email: `hod.math@alquds.edu`
- [ ] Enter password: `password123`
- [ ] Click login
- [ ] **Expected**: Successfully logged in, redirected to HOD dashboard
- [ ] **Expected**: Dashboard shows Mathematics department data

**Test Case 1.1.3: Login as Physics HOD**
- [ ] Navigate to login page
- [ ] Enter email: `hod.physics@alquds.edu`
- [ ] Enter password: `password123`
- [ ] Click login
- [ ] **Expected**: Successfully logged in, redirected to HOD dashboard
- [ ] **Expected**: Dashboard shows Physics department data

**Test Case 1.1.4: Login as Engineering HOD**
- [ ] Navigate to login page
- [ ] Enter email: `hod.eng@alquds.edu`
- [ ] Enter password: `password123`
- [ ] Click login
- [ ] **Expected**: Successfully logged in, redirected to HOD dashboard
- [ ] **Expected**: Dashboard shows Engineering department data

**Test Case 1.1.5: Login as Business Administration HOD**
- [ ] Navigate to login page
- [ ] Enter email: `hod.bus@alquds.edu`
- [ ] Enter password: `password123`
- [ ] Click login
- [ ] **Expected**: Successfully logged in, redirected to HOD dashboard
- [ ] **Expected**: Dashboard shows Business Administration department data

### 1.2 Professor Login Tests

**Test Case 1.2.1: Login as Active Professor**
- [ ] Navigate to login page
- [ ] Select any professor email from MOCK_ACCOUNTS.md with "Active" status
- [ ] Enter password: `password123`
- [ ] Click login
- [ ] **Expected**: Successfully logged in, redirected to Professor dashboard
- [ ] **Expected**: Dashboard shows assigned courses and submissions

**Test Case 1.2.2: Login as Inactive Professor**
- [ ] Navigate to login page
- [ ] Select any professor email from MOCK_ACCOUNTS.md with "Inactive" status
- [ ] Enter password: `password123`
- [ ] Click login
- [ ] **Expected**: Login fails or shows "Account inactive" message

**Test Case 1.2.3: Invalid Credentials**
- [ ] Navigate to login page
- [ ] Enter email: `hod.cs@alquds.edu`
- [ ] Enter password: `wrongpassword`
- [ ] Click login
- [ ] **Expected**: Login fails with "Invalid credentials" message

**Test Case 1.2.4: Empty Credentials**
- [ ] Navigate to login page
- [ ] Leave email and password empty
- [ ] Click login
- [ ] **Expected**: Validation errors shown for required fields

---

## 2. Data Browsing Scenarios

### 2.1 HOD - Browse Professors

**Test Case 2.1.1: View All Professors**
- [ ] Login as HOD (any department)
- [ ] Navigate to Professors page
- [ ] **Expected**: See list of 25 professors total
- [ ] **Expected**: Each professor shows name, email, department, professor ID, status
- [ ] **Expected**: Pagination controls visible if more than page size

**Test Case 2.1.2: View Department Professors Only**
- [ ] Login as Computer Science HOD
- [ ] Navigate to Professors page
- [ ] **Expected**: See only 5 professors from Computer Science department
- [ ] **Expected**: All professors belong to Computer Science

**Test Case 2.1.3: View Professor Details**
- [ ] Login as HOD
- [ ] Navigate to Professors page
- [ ] Click on any professor
- [ ] **Expected**: See detailed professor information
- [ ] **Expected**: See professor's course assignments
- [ ] **Expected**: See professor's submission history

### 2.2 HOD - Browse Courses

**Test Case 2.2.1: View All Courses**
- [ ] Login as HOD
- [ ] Navigate to Courses page
- [ ] **Expected**: See list of 15 courses total
- [ ] **Expected**: Each course shows code, name, department, level

**Test Case 2.2.2: View Department Courses**
- [ ] Login as Computer Science HOD
- [ ] Navigate to Courses page
- [ ] **Expected**: See 3 courses from Computer Science department
- [ ] **Expected**: Courses are CS101, CS201, CS301

**Test Case 2.2.3: View Course Assignments**
- [ ] Login as HOD
- [ ] Navigate to Courses page
- [ ] Click on any course
- [ ] **Expected**: See course details
- [ ] **Expected**: See list of professors assigned to this course
- [ ] **Expected**: See assignments across different semesters

### 2.3 Professor - Browse Assignments

**Test Case 2.3.1: View My Courses**
- [ ] Login as active professor
- [ ] Navigate to Dashboard or My Courses
- [ ] **Expected**: See list of assigned courses for current semester
- [ ] **Expected**: Each course shows course code, name, section

**Test Case 2.3.2: View Course Details**
- [ ] Login as active professor
- [ ] Navigate to My Courses
- [ ] Click on any course
- [ ] **Expected**: See course details
- [ ] **Expected**: See required document types for this course
- [ ] **Expected**: See submission status for each document type

**Test Case 2.3.3: View Submission History**
- [ ] Login as active professor
- [ ] Navigate to Submission History
- [ ] **Expected**: See list of all submissions across all semesters
- [ ] **Expected**: See submission dates, statuses, and file counts

### 2.4 Browse Academic Structure

**Test Case 2.4.1: View Academic Years**
- [ ] Login as HOD
- [ ] Navigate to Academic Years page
- [ ] **Expected**: See 3 academic years (2023-2024, 2024-2025, 2025-2026)
- [ ] **Expected**: Current year marked as active

**Test Case 2.4.2: View Semesters**
- [ ] Login as HOD
- [ ] Navigate to Semesters page
- [ ] **Expected**: See 9 semesters total (3 per academic year)
- [ ] **Expected**: Each semester shows type (FIRST, SECOND, SUMMER)
- [ ] **Expected**: Current semester marked as active

---

## 3. Filtering and Search Scenarios

### 3.1 Filter Professors

**Test Case 3.1.1: Filter by Department**
- [ ] Login as HOD
- [ ] Navigate to Professors page
- [ ] Select "Computer Science" from department filter
- [ ] **Expected**: See only 5 Computer Science professors
- [ ] Clear filter
- [ ] **Expected**: See all 25 professors again

**Test Case 3.1.2: Filter by Status**
- [ ] Login as HOD
- [ ] Navigate to Professors page
- [ ] Select "Active" from status filter
- [ ] **Expected**: See approximately 20 active professors (80%)
- [ ] Select "Inactive" from status filter
- [ ] **Expected**: See approximately 5 inactive professors (20%)

**Test Case 3.1.3: Combined Filters**
- [ ] Login as HOD
- [ ] Navigate to Professors page
- [ ] Select "Mathematics" department and "Active" status
- [ ] **Expected**: See only active Mathematics professors
- [ ] **Expected**: Results match both filter criteria

### 3.2 Search Professors

**Test Case 3.2.1: Search by Name**
- [ ] Login as HOD
- [ ] Navigate to Professors page
- [ ] Enter "Ahmad" in search box
- [ ] **Expected**: See professors with "Ahmad" in their name
- [ ] **Expected**: Search is case-insensitive

**Test Case 3.2.2: Search by Email**
- [ ] Login as HOD
- [ ] Navigate to Professors page
- [ ] Enter "prof.cs" in search box
- [ ] **Expected**: See Computer Science professors
- [ ] **Expected**: Email addresses contain search term

**Test Case 3.2.3: Search by Professor ID**
- [ ] Login as HOD
- [ ] Navigate to Professors page
- [ ] Enter "PCS001" in search box
- [ ] **Expected**: See specific professor with ID PCS001
- [ ] **Expected**: Only one result returned

### 3.3 Filter Submissions

**Test Case 3.3.1: Filter by Status**
- [ ] Login as HOD
- [ ] Navigate to Submissions page
- [ ] Select "UPLOADED" from status filter
- [ ] **Expected**: See approximately 70% of submissions
- [ ] Select "NOT_UPLOADED" from status filter
- [ ] **Expected**: See approximately 20% of submissions
- [ ] Select "OVERDUE" from status filter
- [ ] **Expected**: See approximately 10% of submissions

**Test Case 3.3.2: Filter by Semester**
- [ ] Login as HOD
- [ ] Navigate to Submissions page
- [ ] Select "Fall 2024-2025" from semester filter
- [ ] **Expected**: See only submissions for that semester
- [ ] Change to different semester
- [ ] **Expected**: Results update accordingly

**Test Case 3.3.3: Filter by Course**
- [ ] Login as HOD
- [ ] Navigate to Submissions page
- [ ] Select "CS101" from course filter
- [ ] **Expected**: See only submissions for CS101
- [ ] **Expected**: All submissions relate to CS101 course

### 3.4 Filter Notifications

**Test Case 3.4.1: Filter by Read Status**
- [ ] Login as professor
- [ ] Navigate to Notifications
- [ ] Select "Unread" filter
- [ ] **Expected**: See approximately 40% of notifications
- [ ] Select "Read" filter
- [ ] **Expected**: See approximately 60% of notifications

**Test Case 3.4.2: Filter by Type**
- [ ] Login as professor
- [ ] Navigate to Notifications
- [ ] Select "NEW_REQUEST" type
- [ ] **Expected**: See approximately 30% of notifications
- [ ] Select "DEADLINE_APPROACHING" type
- [ ] **Expected**: See approximately 25% of notifications

---

## 4. Reporting Scenarios

### 4.1 Department Reports

**Test Case 4.1.1: Generate Department Submission Report**
- [ ] Login as Computer Science HOD
- [ ] Navigate to Reports page
- [ ] Select "Department Submission Report"
- [ ] Select current semester
- [ ] Click "Generate Report"
- [ ] **Expected**: Report shows all Computer Science professors
- [ ] **Expected**: Report shows submission statistics per professor
- [ ] **Expected**: Report shows completion percentages

**Test Case 4.1.2: Export Department Report**
- [ ] Login as HOD
- [ ] Generate department submission report
- [ ] Click "Export to PDF" or "Export to Excel"
- [ ] **Expected**: File downloads successfully
- [ ] **Expected**: File contains all report data
- [ ] **Expected**: File is properly formatted

**Test Case 4.1.3: Department Report Across Semesters**
- [ ] Login as HOD
- [ ] Navigate to Reports page
- [ ] Select "Department Submission Report"
- [ ] Select multiple semesters or "All Semesters"
- [ ] Click "Generate Report"
- [ ] **Expected**: Report shows data across selected semesters
- [ ] **Expected**: Data is grouped by semester

### 4.2 Professor Reports

**Test Case 4.2.1: View My Submission Report**
- [ ] Login as active professor
- [ ] Navigate to My Reports
- [ ] Select current semester
- [ ] **Expected**: See personal submission statistics
- [ ] **Expected**: See list of all required documents
- [ ] **Expected**: See submission status for each document

**Test Case 4.2.2: Professor Report by HOD**
- [ ] Login as HOD
- [ ] Navigate to Reports page
- [ ] Select "Professor Report"
- [ ] Select specific professor
- [ ] Select semester
- [ ] Click "Generate Report"
- [ ] **Expected**: See detailed report for selected professor
- [ ] **Expected**: See all course assignments
- [ ] **Expected**: See submission history

### 4.3 Course Reports

**Test Case 4.3.1: Generate Course Submission Report**
- [ ] Login as HOD
- [ ] Navigate to Reports page
- [ ] Select "Course Submission Report"
- [ ] Select specific course (e.g., CS101)
- [ ] Select semester
- [ ] Click "Generate Report"
- [ ] **Expected**: See all professors teaching this course
- [ ] **Expected**: See submission status for each professor
- [ ] **Expected**: See document type breakdown

**Test Case 4.3.2: Course Report Across Semesters**
- [ ] Login as HOD
- [ ] Navigate to Reports page
- [ ] Select "Course Submission Report"
- [ ] Select course
- [ ] Select "All Semesters"
- [ ] Click "Generate Report"
- [ ] **Expected**: See historical data for the course
- [ ] **Expected**: Data grouped by semester

### 4.4 System-Wide Reports

**Test Case 4.4.1: Overall Submission Statistics**
- [ ] Login as HOD
- [ ] Navigate to Dashboard
- [ ] **Expected**: See overall submission statistics
- [ ] **Expected**: See percentage of completed submissions
- [ ] **Expected**: See number of overdue submissions
- [ ] **Expected**: See number of pending submissions

**Test Case 4.4.2: Late Submission Report**
- [ ] Login as HOD
- [ ] Navigate to Reports page
- [ ] Select "Late Submissions Report"
- [ ] Select semester
- [ ] Click "Generate Report"
- [ ] **Expected**: See list of late submissions (approximately 15%)
- [ ] **Expected**: See how late each submission was
- [ ] **Expected**: See professor and course information

---

## 5. Document Submission Scenarios

### 5.1 View Required Documents

**Test Case 5.1.1: View Required Documents for Course**
- [ ] Login as professor
- [ ] Navigate to My Courses
- [ ] Click on any course
- [ ] **Expected**: See list of 6 required document types
- [ ] **Expected**: Document types include SYLLABUS, EXAM, ASSIGNMENT, PROJECT_DOCS, LECTURE_NOTES, OTHER
- [ ] **Expected**: Each shows deadline and submission status

**Test Case 5.1.2: View Deadline Information**
- [ ] Login as professor
- [ ] Navigate to course details
- [ ] **Expected**: See deadline for each document type
- [ ] **Expected**: Deadlines are realistic based on semester dates
- [ ] **Expected**: Overdue documents highlighted in red

### 5.2 View Submitted Documents

**Test Case 5.2.1: View Submission Details**
- [ ] Login as professor
- [ ] Navigate to course with UPLOADED status
- [ ] Click on submitted document
- [ ] **Expected**: See submission date and time
- [ ] **Expected**: See list of uploaded files (1-3 files)
- [ ] **Expected**: See file names, sizes, and types
- [ ] **Expected**: See submission notes if any

**Test Case 5.2.2: View Uploaded Files**
- [ ] Login as professor
- [ ] Navigate to submitted document
- [ ] **Expected**: See file list with proper metadata
- [ ] **Expected**: File sizes between 100KB and 10MB
- [ ] **Expected**: File extensions match document type requirements

### 5.3 Submission Status Indicators

**Test Case 5.3.1: View NOT_UPLOADED Status**
- [ ] Login as professor
- [ ] Navigate to My Courses
- [ ] **Expected**: See approximately 20% of documents with NOT_UPLOADED status
- [ ] **Expected**: Status clearly indicated with icon/color
- [ ] **Expected**: Deadline information visible

**Test Case 5.3.2: View UPLOADED Status**
- [ ] Login as professor
- [ ] Navigate to My Courses
- [ ] **Expected**: See approximately 70% of documents with UPLOADED status
- [ ] **Expected**: Status clearly indicated with icon/color
- [ ] **Expected**: Submission date visible

**Test Case 5.3.3: View OVERDUE Status**
- [ ] Login as professor
- [ ] Navigate to My Courses
- [ ] **Expected**: See approximately 10% of documents with OVERDUE status
- [ ] **Expected**: Status clearly indicated with warning icon/color
- [ ] **Expected**: Days overdue information visible

---

## 6. Notification Scenarios

### 6.1 View Notifications

**Test Case 6.1.1: View All Notifications**
- [ ] Login as professor
- [ ] Click on notifications icon/menu
- [ ] **Expected**: See list of notifications (approximately 3 per professor)
- [ ] **Expected**: Unread notifications highlighted (40%)
- [ ] **Expected**: Read notifications shown normally (60%)

**Test Case 6.1.2: View Notification Details**
- [ ] Login as professor
- [ ] Click on any notification
- [ ] **Expected**: See full notification message
- [ ] **Expected**: See notification type and timestamp
- [ ] **Expected**: See related entity link (course/submission)
- [ ] **Expected**: Notification marked as read

**Test Case 6.1.3: Notification Types**
- [ ] Login as professor
- [ ] View notifications list
- [ ] **Expected**: See NEW_REQUEST notifications (30%)
- [ ] **Expected**: See REQUEST_REMINDER notifications (20%)
- [ ] **Expected**: See DEADLINE_APPROACHING notifications (25%)
- [ ] **Expected**: See DOCUMENT_SUBMITTED notifications (15%)
- [ ] **Expected**: See DOCUMENT_OVERDUE notifications (10%)

### 6.2 Notification Actions

**Test Case 6.2.1: Mark as Read**
- [ ] Login as professor
- [ ] Click on unread notification
- [ ] **Expected**: Notification marked as read
- [ ] **Expected**: Visual indicator changes
- [ ] **Expected**: Unread count decreases

**Test Case 6.2.2: Navigate from Notification**
- [ ] Login as professor
- [ ] Click on notification with related entity
- [ ] Click on entity link
- [ ] **Expected**: Navigate to related course or submission
- [ ] **Expected**: Relevant information displayed

---

## 7. Data Integrity Scenarios

### 7.1 Verify Relationships

**Test Case 7.1.1: Professor-Department Relationship**
- [ ] Login as HOD
- [ ] View any professor
- [ ] **Expected**: Professor belongs to correct department
- [ ] **Expected**: Professor only sees courses from their department

**Test Case 7.1.2: Course-Department Relationship**
- [ ] Login as HOD
- [ ] View any course
- [ ] **Expected**: Course belongs to correct department
- [ ] **Expected**: Course code matches department (e.g., CS for Computer Science)

**Test Case 7.1.3: Assignment-Semester Relationship**
- [ ] Login as professor
- [ ] View assignments for specific semester
- [ ] **Expected**: All assignments belong to selected semester
- [ ] **Expected**: No assignments from other semesters shown

**Test Case 7.1.4: Submission-Assignment Relationship**
- [ ] Login as professor
- [ ] View any submission
- [ ] **Expected**: Submission linked to correct course assignment
- [ ] **Expected**: Submission professor matches assignment professor

### 7.2 Verify Data Consistency

**Test Case 7.2.1: File Count Consistency**
- [ ] Login as professor
- [ ] View any submission with files
- [ ] **Expected**: File count matches number of uploaded files
- [ ] **Expected**: File count between 1-3 for UPLOADED submissions

**Test Case 7.2.2: File Size Consistency**
- [ ] Login as professor
- [ ] View any submission with files
- [ ] **Expected**: Total file size equals sum of individual file sizes
- [ ] **Expected**: Total size within allowed limits (50MB max)

**Test Case 7.2.3: Status Consistency**
- [ ] Login as professor
- [ ] View submissions
- [ ] **Expected**: NOT_UPLOADED submissions have no files
- [ ] **Expected**: UPLOADED submissions have files and submission date
- [ ] **Expected**: OVERDUE submissions either have late files or no files past deadline

---

## 8. Performance and Pagination Scenarios

### 8.1 Pagination

**Test Case 8.1.1: Professor List Pagination**
- [ ] Login as HOD
- [ ] Navigate to Professors page
- [ ] Set page size to 10
- [ ] **Expected**: See 10 professors on first page
- [ ] Click "Next" page
- [ ] **Expected**: See next 10 professors
- [ ] **Expected**: Total shows 25 professors

**Test Case 8.1.2: Submission List Pagination**
- [ ] Login as HOD
- [ ] Navigate to Submissions page
- [ ] **Expected**: Pagination controls visible
- [ ] Navigate through pages
- [ ] **Expected**: All submissions accessible

### 8.2 Performance

**Test Case 8.2.1: Large Dataset Loading**
- [ ] Login as HOD
- [ ] Navigate to Professors page (25 records)
- [ ] **Expected**: Page loads within 2 seconds
- [ ] Navigate to Submissions page (100+ records)
- [ ] **Expected**: Page loads within 3 seconds

**Test Case 8.2.2: Report Generation Performance**
- [ ] Login as HOD
- [ ] Generate department report
- [ ] **Expected**: Report generates within 5 seconds
- [ ] **Expected**: No timeout errors

---

## 9. Edge Cases and Error Handling

### 9.1 Empty States

**Test Case 9.1.1: Professor with No Assignments**
- [ ] Login as inactive professor
- [ ] Navigate to My Courses
- [ ] **Expected**: See "No courses assigned" message
- [ ] **Expected**: Helpful message displayed

**Test Case 9.1.2: No Notifications**
- [ ] Login as professor with no notifications
- [ ] Click notifications
- [ ] **Expected**: See "No notifications" message

### 9.2 Boundary Conditions

**Test Case 9.2.1: Maximum Files per Submission**
- [ ] Login as professor
- [ ] View submission with maximum files (5 files)
- [ ] **Expected**: All 5 files displayed correctly
- [ ] **Expected**: File count shows 5

**Test Case 9.2.2: Large File Sizes**
- [ ] Login as professor
- [ ] View submission with large files (close to 10MB)
- [ ] **Expected**: File size displayed correctly
- [ ] **Expected**: Total size calculated correctly

---

## 10. Cross-Role Scenarios

### 10.1 HOD and Professor Interaction

**Test Case 10.1.1: HOD Views Professor's Submissions**
- [ ] Login as HOD
- [ ] Navigate to specific professor's profile
- [ ] View their submissions
- [ ] **Expected**: See all professor's submissions
- [ ] **Expected**: See submission statuses and dates
- [ ] Logout and login as that professor
- [ ] **Expected**: See same submissions from professor view

**Test Case 10.1.2: Department Filtering Consistency**
- [ ] Login as Computer Science HOD
- [ ] Note number of professors and courses
- [ ] Logout and login as Computer Science professor
- [ ] **Expected**: Professor belongs to Computer Science
- [ ] **Expected**: Professor only sees Computer Science courses

---

## Testing Completion Checklist

After completing all test cases above, verify:

- [ ] All 5 HOD accounts tested
- [ ] At least 5 different professor accounts tested (mix of active/inactive)
- [ ] All major features tested (login, browsing, filtering, searching, reporting)
- [ ] All entity relationships verified
- [ ] Data integrity confirmed
- [ ] Performance acceptable
- [ ] No critical errors encountered
- [ ] Mock data provides realistic testing environment

---

## Notes

- Document any issues found during testing
- Note any unexpected behavior
- Verify that mock data matches the specifications in MOCK_ACCOUNTS.md
- Ensure all test scenarios can be repeated consistently
- Report any data inconsistencies or relationship errors

---

## References

- [MOCK_ACCOUNTS.md](MOCK_ACCOUNTS.md) - List of all mock accounts
- [MOCK_DATA_API_TESTING.md](MOCK_DATA_API_TESTING.md) - API testing examples
- [mock_data_guide.md](mock_data_guide.md) - Mock data structure documentation
