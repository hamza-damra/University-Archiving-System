# Quick Test Guide - Professor Dashboard Frontend

## 🚀 Quick Start

### Prerequisites
- Backend server running on `localhost:8080`
- Professor user account created in the system
- Test data populated (academic years, semesters, courses, assignments)

### Run Automated Tests (Recommended)

1. **Start the backend**:
   ```bash
   ./mvnw spring-boot:run
   ```

2. **Log in as professor**:
   - Navigate to: `http://localhost:8080/index.html`
   - Enter professor credentials
   - Click "Login"

3. **Run automated tests**:
   - Navigate to: `http://localhost:8080/test-prof-dashboard.html`
   - Tests run automatically
   - View results on the page

### Expected Results
- ✅ Green checkmarks for passing tests
- ❌ Red X marks for failing tests
- Test summary shows total passed/failed

---

## 📋 Manual Testing Checklist

### Quick Smoke Test (5 minutes)

1. **Login & Page Load**
   - [ ] Page loads without errors
   - [ ] Professor name displays in header
   - [ ] Academic year dropdown is populated

2. **Course Display**
   - [ ] Select academic year and semester
   - [ ] Courses display as cards
   - [ ] Document types show with status badges

3. **File Upload**
   - [ ] Click "Upload Files" button
   - [ ] Modal opens
   - [ ] Select a PDF file
   - [ ] Upload succeeds
   - [ ] Success toast appears

4. **File Explorer**
   - [ ] Switch to "File Explorer" tab
   - [ ] Folders display
   - [ ] Click a folder to navigate
   - [ ] Breadcrumbs update

5. **Dashboard**
   - [ ] Switch to "Dashboard" tab
   - [ ] Statistics display (courses, submitted, pending, overdue)
   - [ ] Summary text is accurate

6. **Notifications**
   - [ ] Click notification bell
   - [ ] Dropdown opens
   - [ ] Notifications display (if any)

---

## 🔍 Detailed Testing

For comprehensive testing, see: `PROFESSOR_DASHBOARD_VERIFICATION.md`

---

## 🐛 Troubleshooting

### Tests Fail with "Not authenticated"
**Solution**: Log in as a professor user first, then navigate to the test page.

### Tests Fail with "No academic years"
**Solution**: Ensure test data is populated in the database.

### Page Shows "Access Denied"
**Solution**: Ensure you're logged in with a ROLE_PROFESSOR account, not HOD or DEANSHIP.

### File Upload Fails
**Solution**: 
- Check file type (must be PDF or ZIP)
- Check file size (must be within limits)
- Check backend logs for errors

### File Explorer Shows Empty
**Solution**: 
- Ensure academic year and semester are selected
- Ensure course assignments exist for the professor
- Ensure files have been uploaded

---

## 📊 Test Coverage

### Automated Tests Cover:
- ✅ Page structure and DOM elements
- ✅ API integration and endpoint calls
- ✅ Course rendering and status display
- ✅ File upload modal functionality
- ✅ File explorer navigation
- ✅ Dashboard overview statistics
- ✅ Notification system

### Manual Tests Required:
- File upload with actual files
- File download functionality
- Cross-browser compatibility
- Mobile responsiveness
- Performance with large datasets

---

## 📁 Test Files

| File | Purpose | Location |
|------|---------|----------|
| `test-prof-dashboard.html` | Automated test suite | `src/main/resources/static/` |
| `PROFESSOR_DASHBOARD_VERIFICATION.md` | Manual testing guide | Root directory |
| `FRONTEND_VERIFICATION_SUMMARY.md` | Verification results | Root directory |
| `QUICK_TEST_GUIDE.md` | This file | Root directory |

---

## ✅ Success Criteria

The frontend is working correctly if:
- All automated tests pass (green checkmarks)
- Manual smoke test completes without errors
- Professor can view courses, upload files, and navigate file explorer
- Dashboard statistics are accurate
- Notifications display and update

---

## 🎯 Next Steps

After frontend verification:
1. Run automated tests
2. Perform manual smoke test
3. Review detailed verification document
4. Proceed to Task 3: End-to-End Integration Testing

---

## 📞 Support

If you encounter issues:
1. Check browser console for JavaScript errors
2. Check backend logs for API errors
3. Verify test data exists in database
4. Review `PROFESSOR_DASHBOARD_VERIFICATION.md` for detailed troubleshooting

---

**Last Updated**: November 19, 2025  
**Task**: 2. Frontend Verification and Testing  
**Status**: ✅ COMPLETE
