# Browser Testing Guide for Unified File Explorer

## Quick Start

This guide provides step-by-step instructions for manually testing the unified File Explorer across all three dashboards in different browsers.

---

## Test Setup

### 1. Start the Application
```bash
# Make sure the application is running
# Navigate to http://localhost:8080
```

### 2. Test Accounts
- **Professor:** `prof1` / `password`
- **HOD:** `hod1` / `password`
- **Deanship:** `dean1` / `password`

---

## Chrome Testing (Primary Browser)

### Professor Dashboard - Chrome

1. **Login and Navigate**
   - Open Chrome
   - Go to `http://localhost:8080`
   - Login as `prof1` / `password`
   - Click on "File Explorer" tab
   - ✅ Verify File Explorer loads

2. **Select Academic Year and Semester**
   - Select an academic year from dropdown
   - ✅ Verify semester dropdown becomes enabled
   - Select a semester
   - ✅ Verify File Explorer shows your courses

3. **Browse Folders**
   - Click on a course folder (blue card)
   - ✅ Verify breadcrumb shows: Home → Course Name
   - ✅ Verify "Your Folder" label appears on your course
   - Click on a document type folder (e.g., "Syllabus")
   - ✅ Verify breadcrumb updates
   - ✅ Verify files are displayed in table

4. **Test File Upload**
   - Click "Upload Files" button
   - Select one or more test files
   - ✅ Verify upload progress shows
   - ✅ Verify files appear in list after upload

5. **Test File Download**
   - Click download button (blue button with down arrow)
   - ✅ Verify file downloads successfully
   - ✅ Check downloaded file opens correctly

6. **Test Breadcrumb Navigation**
   - Click "Home" in breadcrumb
   - ✅ Verify returns to course list
   - Navigate back into folders
   - Click intermediate breadcrumb segment
   - ✅ Verify jumps to that level

7. **Visual Verification**
   - ✅ Folder cards are blue (bg-blue-50)
   - ✅ Hover effect works (turns darker blue)
   - ✅ Arrow icon animates on hover
   - ✅ File table has proper columns
   - ✅ Action buttons are styled correctly

### HOD Dashboard - Chrome

1. **Login and Navigate**
   - Logout from professor account
   - Login as `hod1` / `password`
   - Click on "File Explorer" tab
   - ✅ Verify File Explorer loads

2. **Verify Header Message**
   - ✅ Verify "Browse department files (Read-only)" message displays

3. **Select Academic Year and Semester**
   - Select an academic year
   - Select a semester
   - ✅ Verify File Explorer shows department professors

4. **Browse Folders**
   - Click on a professor folder
   - ✅ Verify breadcrumb shows: Home → Professor Name
   - Click on a course folder
   - Click on a document type folder
   - ✅ Verify files are displayed

5. **Verify Read-Only Access**
   - ✅ Verify NO "Upload Files" button exists
   - ✅ Verify NO "Replace" buttons on files
   - ✅ Verify only "Download" and "View" buttons available

6. **Test File Download**
   - Click download button on a file
   - ✅ Verify file downloads successfully

7. **Verify Department Filtering**
   - ✅ Verify only professors from HOD's department are visible
   - ✅ Verify cannot access other departments

8. **Visual Verification**
   - ✅ Compare with Professor Dashboard
   - ✅ Folder cards look identical
   - ✅ File table looks identical
   - ✅ Breadcrumbs look identical

### Deanship Dashboard - Chrome

1. **Login and Navigate**
   - Logout from HOD account
   - Login as `dean1` / `password`
   - Click on "File Explorer" tab
   - ✅ Verify File Explorer loads

2. **Select Academic Year and Semester**
   - Select an academic year
   - Select a semester
   - ✅ Verify File Explorer shows ALL professors

3. **Verify All Departments Visible**
   - ✅ Count professors from multiple departments
   - ✅ Verify professors from all departments are visible

4. **Browse Folders**
   - Click on a professor folder from Department A
   - ✅ Verify navigation works
   - Go back to root
   - Click on a professor folder from Department B
   - ✅ Verify navigation works

5. **Verify Professor Labels**
   - ✅ Verify professor name labels display on folders
   - ✅ Verify labels show full professor name

6. **Verify Read-Only Access**
   - ✅ Verify NO "Upload Files" button exists
   - ✅ Verify NO "Replace" buttons on files
   - ✅ Verify only "Download" and "View" buttons available

7. **Test File Download**
   - Download files from different departments
   - ✅ Verify all downloads work

8. **Visual Verification**
   - ✅ Compare with Professor Dashboard
   - ✅ Folder cards look identical
   - ✅ File table looks identical
   - ✅ Breadcrumbs look identical

---

## Firefox Testing

### Quick Test (All Dashboards)

1. **Open Firefox**
   - Go to `http://localhost:8080`

2. **Test Professor Dashboard**
   - Login as `prof1` / `password`
   - Navigate to File Explorer
   - ✅ Verify layout renders correctly
   - ✅ Test file upload
   - ✅ Test file download
   - ✅ Test breadcrumb navigation
   - ✅ Verify all animations work

3. **Test HOD Dashboard**
   - Logout and login as `hod1` / `password`
   - Navigate to File Explorer
   - ✅ Verify layout renders correctly
   - ✅ Test file download
   - ✅ Verify read-only access

4. **Test Deanship Dashboard**
   - Logout and login as `dean1` / `password`
   - Navigate to File Explorer
   - ✅ Verify layout renders correctly
   - ✅ Test file download
   - ✅ Verify all departments visible

5. **Firefox-Specific Checks**
   - ✅ Verify Tailwind CSS renders correctly
   - ✅ Verify file download dialog works
   - ✅ Verify no console errors
   - ✅ Verify hover effects work

---

## Safari Testing (macOS)

### Quick Test (All Dashboards)

1. **Open Safari**
   - Go to `http://localhost:8080`

2. **Test Professor Dashboard**
   - Login as `prof1` / `password`
   - Navigate to File Explorer
   - ✅ Verify layout renders correctly
   - ✅ Test file upload
   - ✅ Test file download
   - ✅ Test breadcrumb navigation

3. **Test HOD Dashboard**
   - Logout and login as `hod1` / `password`
   - Navigate to File Explorer
   - ✅ Verify layout renders correctly
   - ✅ Test file download

4. **Test Deanship Dashboard**
   - Logout and login as `dean1` / `password`
   - Navigate to File Explorer
   - ✅ Verify layout renders correctly
   - ✅ Test file download

5. **Safari-Specific Checks**
   - ✅ Verify WebKit rendering is correct
   - ✅ Verify flexbox layouts work
   - ✅ Verify grid layouts work
   - ✅ Verify transitions and animations work
   - ✅ Verify file upload/download works

---

## Edge Testing (Windows)

### Quick Test (All Dashboards)

1. **Open Edge**
   - Go to `http://localhost:8080`

2. **Test Professor Dashboard**
   - Login as `prof1` / `password`
   - Navigate to File Explorer
   - ✅ Verify layout renders correctly
   - ✅ Test file upload
   - ✅ Test file download
   - ✅ Test breadcrumb navigation

3. **Test HOD Dashboard**
   - Logout and login as `hod1` / `password`
   - Navigate to File Explorer
   - ✅ Verify layout renders correctly
   - ✅ Test file download

4. **Test Deanship Dashboard**
   - Logout and login as `dean1` / `password`
   - Navigate to File Explorer
   - ✅ Verify layout renders correctly
   - ✅ Test file download

5. **Edge-Specific Checks**
   - ✅ Verify Chromium Edge renders correctly
   - ✅ Verify no Edge-specific issues
   - ✅ Verify file operations work

---

## Responsive Design Testing

### Desktop (1920x1080)
1. Set browser window to 1920x1080
2. Test all three dashboards
3. ✅ Verify layout is optimal
4. ✅ Verify no unnecessary scrolling

### Laptop (1366x768)
1. Set browser window to 1366x768
2. Test all three dashboards
3. ✅ Verify layout adapts correctly
4. ✅ Verify all elements accessible

### Tablet (768x1024)
1. Use browser dev tools to simulate tablet
2. Test all three dashboards
3. ✅ Verify grid changes to single column
4. ✅ Verify touch interactions work

### Mobile (375x667)
1. Use browser dev tools to simulate mobile
2. Test all three dashboards
3. ✅ Verify layout is mobile-friendly
4. ✅ Verify navigation works on small screen

---

## Performance Testing

### Load Time Test
1. Open browser dev tools (F12)
2. Go to Network tab
3. Clear cache
4. Load each dashboard
5. ✅ Verify page loads in < 3 seconds
6. ✅ Verify File Explorer initializes quickly

### Navigation Performance
1. Open browser dev tools
2. Go to Performance tab
3. Start recording
4. Navigate through folders
5. Stop recording
6. ✅ Verify navigation feels instant (< 500ms)

---

## API Testing with Browser Dev Tools

### Test API Endpoints

1. **Open Browser Dev Tools (F12)**
2. **Go to Network Tab**
3. **Filter by XHR**

4. **Test Professor APIs**
   - Login as professor
   - Navigate File Explorer
   - ✅ Verify `/api/file-explorer/academic-years` returns 200
   - ✅ Verify `/api/file-explorer/academic-years/{id}/semesters` returns 200
   - ✅ Verify `/api/file-explorer/root` returns 200
   - ✅ Verify `/api/file-explorer/node` returns 200

5. **Test HOD APIs**
   - Login as HOD
   - Navigate File Explorer
   - ✅ Verify same endpoints return 200
   - ✅ Verify response contains only department data

6. **Test Deanship APIs**
   - Login as Deanship
   - Navigate File Explorer
   - ✅ Verify same endpoints return 200
   - ✅ Verify response contains all departments

7. **Test File Operations**
   - ✅ Verify file upload POST request succeeds (Professor only)
   - ✅ Verify file download GET request succeeds (All roles)
   - ✅ Verify file view GET request succeeds (All roles)

---

## Console Error Check

### For Each Dashboard and Browser

1. Open browser dev tools (F12)
2. Go to Console tab
3. Clear console
4. Navigate through File Explorer
5. ✅ Verify NO JavaScript errors
6. ✅ Verify NO 404 errors
7. ✅ Verify NO CORS errors
8. ✅ Verify NO permission errors (except expected ones)

---

## Test Results Template

### Browser: ___________
### Date: ___________
### Tester: ___________

| Dashboard | Login | Browse | Upload | Download | Breadcrumb | Visual | Overall |
|-----------|-------|--------|--------|----------|------------|--------|---------|
| Professor | ☐     | ☐      | ☐      | ☐        | ☐          | ☐      | ☐       |
| HOD       | ☐     | ☐      | N/A    | ☐        | ☐          | ☐      | ☐       |
| Deanship  | ☐     | ☐      | N/A    | ☐        | ☐          | ☐      | ☐       |

### Issues Found:
1. 
2. 
3. 

### Notes:


---

## Requirements Coverage

✅ **9.1:** All existing API endpoints maintained without modification
✅ **9.2:** Backend routing and permission logic preserved
✅ **9.3:** File download mechanism unchanged
✅ **9.4:** Data fetching methods from api.js module used
✅ **9.5:** All event handlers and callback functions preserved

---

## Quick Reference: What to Look For

### Visual Consistency Checklist
- [ ] Folder cards: Blue background (bg-blue-50), blue border (border-blue-200)
- [ ] Folder cards: Hover effect changes to darker blue (hover:bg-blue-100)
- [ ] Folder cards: Arrow icon animates on hover (translate-x-1)
- [ ] File table: Consistent column layout across all dashboards
- [ ] File icons: Correct colors (red for PDF, amber for ZIP, etc.)
- [ ] Breadcrumbs: Same styling and behavior across all dashboards
- [ ] Empty states: Same icon, text, and layout
- [ ] Loading states: Same skeleton animation
- [ ] Error states: Same icon and message styling

### Functional Consistency Checklist
- [ ] Academic Year selector works the same way
- [ ] Semester selector works the same way
- [ ] Breadcrumb navigation works the same way
- [ ] Folder navigation works the same way
- [ ] File download works the same way
- [ ] Permission checks work correctly
- [ ] Role-specific features work as expected

### Performance Checklist
- [ ] Page loads quickly (< 3 seconds)
- [ ] File Explorer initializes quickly
- [ ] Folder navigation feels instant (< 500ms)
- [ ] File operations complete in reasonable time
- [ ] No lag or stuttering in animations
