# Testing Guide: HOD Submission Report Feature

## Prerequisites
1. Application is running
2. Database has sample data:
   - At least one HOD user assigned to a department
   - Multiple professors in the same department
   - Document requests assigned to professors
   - Some submitted, some pending, some overdue

## Test Scenarios

### Scenario 1: View Report in Browser
**Steps:**
1. Log in as HOD user
2. Navigate to HOD Dashboard (`/hod-dashboard.html`)
3. Locate "Professor Submission Report" section at the top
4. Click **"View Report"** button

**Expected Results:**
- Loading toast appears briefly
- Modal opens with title "Department Submission Report"
- Overall statistics section displays:
  - Total Professors count
  - Total Requests count
  - Total Submitted (green)
  - Total Pending (yellow)
  - Total Overdue (red)
  - Overall Completion Rate percentage
- Professor details table shows:
  - Each professor's name and email
  - Request counts (total, submitted, pending, overdue)
  - Completion rate with progress bar
- Footer shows generation timestamp and HOD name
- Modal is scrollable if many professors

### Scenario 2: Download PDF Report
**Steps:**
1. Log in as HOD user
2. Navigate to HOD Dashboard
3. Click **"Download PDF"** button

**Expected Results:**
- "Generating PDF..." toast appears
- PDF file downloads automatically
- Filename format: `department-submission-report-[timestamp].pdf`
- "PDF downloaded successfully" toast appears
- Open PDF and verify:
  - Professional header with department name
  - Color-coded statistics cards
  - Detailed professor table with all metrics
  - Footer with university branding
  - All data matches the web view

### Scenario 3: Empty State (No Professors)
**Steps:**
1. Create a department with no professors
2. Assign HOD to that department
3. Log in as that HOD
4. Try to view/download report

**Expected Results:**
- Report generates successfully
- Shows 0 professors
- Empty professor table or appropriate message
- No errors in console

### Scenario 4: No Requests
**Steps:**
1. Department has professors but no document requests
2. View report

**Expected Results:**
- Shows correct professor count
- All request counts are 0
- Completion rate is 0% or N/A
- No errors

### Scenario 5: All Submitted
**Steps:**
1. Create scenario where all requests are submitted
2. View report

**Expected Results:**
- Total Submitted = Total Requests
- Total Pending = 0
- Total Overdue = 0
- Completion Rate = 100%
- All professors show 100% completion

### Scenario 6: Mixed Status
**Steps:**
1. Create realistic scenario:
   - Professor A: 5 requests, 5 submitted (100%)
   - Professor B: 4 requests, 2 submitted, 2 pending (50%)
   - Professor C: 6 requests, 3 submitted, 1 pending, 2 overdue (50%)
2. View report

**Expected Results:**
- Accurate counts for each category
- Professors sorted by completion rate (A, B/C, C/B)
- Overdue count only includes pending past deadline
- Completion rates calculated correctly

### Scenario 7: Security Test
**Steps:**
1. Try to access endpoints without authentication
2. Try to access as Professor role
3. Try to access as HOD from different department

**Expected Results:**
- Unauthenticated: 401 Unauthorized
- Wrong role: 403 Forbidden
- Different department: Only sees own department data

## API Endpoint Testing

### Test with cURL or Postman

#### Get Report JSON
```bash
curl -X GET \
  http://localhost:8080/api/hod/reports/submission-summary \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Department submission report generated successfully",
  "data": {
    "departmentName": "Computer Science",
    "generatedAt": "2025-11-14T02:42:00",
    "generatedBy": "John Doe",
    "totalProfessors": 5,
    "totalRequests": 25,
    "totalSubmitted": 18,
    "totalPending": 5,
    "totalOverdue": 2,
    "overallCompletionRate": 72.0,
    "overallOnTimeRate": 94.44,
    "professorSummaries": [...]
  }
}
```

#### Download PDF
```bash
curl -X GET \
  http://localhost:8080/api/hod/reports/submission-summary/pdf \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN' \
  --output report.pdf
```

**Expected Response:**
- HTTP 200 OK
- Content-Type: application/pdf
- File downloads successfully

## Browser Console Testing

Open browser console and check for:
- No JavaScript errors
- API calls return 200 status
- Report data structure is correct
- PDF blob is created successfully

## Database Verification

Run these queries to verify report accuracy:

```sql
-- Count professors in department
SELECT COUNT(*) FROM users 
WHERE department_id = ? AND role = 'ROLE_PROFESSOR';

-- Count total requests
SELECT COUNT(*) FROM document_requests dr
JOIN users u ON dr.professor_id = u.id
WHERE u.department_id = ?;

-- Count submitted
SELECT COUNT(*) FROM document_requests dr
JOIN users u ON dr.professor_id = u.id
WHERE u.department_id = ? AND dr.id IN (
  SELECT document_request_id FROM submitted_documents
);

-- Count overdue
SELECT COUNT(*) FROM document_requests dr
JOIN users u ON dr.professor_id = u.id
WHERE u.department_id = ? 
  AND dr.deadline < NOW() 
  AND dr.id NOT IN (
    SELECT document_request_id FROM submitted_documents
  );
```

## Common Issues & Solutions

### Issue: PDF download fails
**Solution:** 
- Check browser console for errors
- Verify iText7 dependency is in pom.xml
- Run `mvn clean install` to download dependencies

### Issue: Report shows wrong department
**Solution:**
- Verify HOD user has department assigned
- Check database: `SELECT * FROM users WHERE id = ?`

### Issue: Completion rate incorrect
**Solution:**
- Verify calculation logic in `ReportService.generateProfessorSummary()`
- Check if submitted_documents table has correct data

### Issue: Modal doesn't open
**Solution:**
- Check browser console for JavaScript errors
- Verify `showModal()` function exists in ui.js
- Check if modal container exists in HTML

## Performance Testing

### Large Dataset Test
1. Create 50+ professors with 10+ requests each
2. View report - should load within 2-3 seconds
3. Download PDF - should generate within 5 seconds

### Concurrent Access Test
1. Multiple HODs access report simultaneously
2. No conflicts or errors should occur

## Checklist

- [ ] View report displays correctly
- [ ] PDF downloads successfully
- [ ] PDF formatting is professional
- [ ] All metrics are accurate
- [ ] Empty states handled gracefully
- [ ] Security restrictions work
- [ ] No console errors
- [ ] Mobile responsive (modal scrolls)
- [ ] Loading states show appropriately
- [ ] Success/error toasts appear
- [ ] Report data matches database
- [ ] Professors sorted by completion rate
- [ ] Progress bars render correctly
- [ ] Date formatting is correct

## Next Steps After Testing

1. If all tests pass: Feature is ready for production
2. If issues found: Document in issue tracker
3. Consider adding automated tests for report generation
4. Monitor performance with real data volumes
