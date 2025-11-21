# Integration Testing Quick Start Guide

## Quick Start (3 Steps)

### 1. Start the Backend Server
```bash
# Make sure the server is running on http://localhost:8080
# If not running, start it with:
./mvnw spring-boot:run
```

### 2. Launch the Test Interface
**Option A - Double-click:**
```
run-integration-tests.bat
```

**Option B - Open directly:**
```
Open test-integration-manual.html in your browser
```

### 3. Login and Test
- **URL**: http://localhost:8080/login
- **Username**: dean@alquds.edu
- **Password**: dean123

Then work through the test checklist in the browser!

## What Gets Tested?

✅ **Navigation** - All 7 pages load correctly  
✅ **State Preservation** - Academic year/semester persist across pages  
✅ **CRUD Operations** - Create, edit, delete functionality  
✅ **Search & Filters** - Search and filter on professors/courses pages  
✅ **File Explorer** - File navigation and operations  
✅ **Dashboard** - All cards display and navigate correctly  
✅ **Logout** - Logout works from all pages  
✅ **Responsive Design** - Works on 1366x768 and 1920x1080  
✅ **Typography** - Font sizes and spacing are correct  
✅ **Error Handling** - Errors display properly  
✅ **Security** - Authentication and authorization work  

## Test Interface Features

- **Progress Tracking**: Your progress is saved automatically
- **Statistics**: See total, completed, and remaining tests
- **Progress Bar**: Visual indicator of completion
- **Quick Links**: Jump directly to any deanship page
- **Organized Categories**: Tests grouped by functionality

## Test Execution Tips

1. **Work Systematically**: Complete one category at a time
2. **Document Issues**: Note any failures or unexpected behavior
3. **Take Breaks**: 196 tests is a lot - pace yourself!
4. **Use Multiple Sessions**: Progress is saved, come back anytime
5. **Test Thoroughly**: Don't just check boxes, verify functionality

## Common Issues

### Backend Not Running
**Symptom**: Pages don't load, 404 errors  
**Solution**: Start the backend server with `./mvnw spring-boot:run`

### Authentication Fails
**Symptom**: Can't login, redirected to login page  
**Solution**: Verify credentials are dean@alquds.edu / dean123

### No Test Data
**Symptom**: Empty tables, no data to test with  
**Solution**: Run database initialization scripts or create test data

### Browser Cache Issues
**Symptom**: Old version of pages loading  
**Solution**: Hard refresh (Ctrl+F5) or clear browser cache

## Test Report

After completing tests, fill out the comprehensive test report:
```
INTEGRATION_TEST_REPORT.md
```

This template includes:
- Detailed test results for all 196 tests
- Space for documenting issues
- Sign-off section for approval
- Recommendations section

## Files Reference

| File | Purpose |
|------|---------|
| `test-integration-manual.html` | Interactive test checklist (main tool) |
| `run-integration-tests.bat` | Quick launcher |
| `INTEGRATION_TEST_REPORT.md` | Detailed test report template |
| `TASK_14_INTEGRATION_TESTING_COMPLETE.md` | Task completion summary |
| `INTEGRATION_TESTING_QUICK_START.md` | This guide |

## Need Help?

- Review `TASK_14_INTEGRATION_TESTING_COMPLETE.md` for detailed information
- Check `INTEGRATION_TEST_REPORT.md` for the full test list
- Refer to the requirements and design documents in `.kiro/specs/deanship-multi-page-refactor/`

## Quick Test Checklist

Before starting comprehensive testing, verify these basics:

- [ ] Backend server is running
- [ ] Can access http://localhost:8080
- [ ] Can login as deanship user
- [ ] Dashboard page loads
- [ ] At least one academic year exists
- [ ] At least one professor exists
- [ ] At least one course exists

If all basics pass, proceed with comprehensive testing!

---

**Ready to test?** Run `run-integration-tests.bat` or open `test-integration-manual.html`
