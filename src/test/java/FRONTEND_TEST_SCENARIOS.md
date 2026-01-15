# Frontend Test Scenarios - Detailed User Flow Specifications

## Overview
This file provides detailed test scenarios for complex user flows that require step-by-step validation. Each scenario includes preconditions, test steps, expected outcomes, and edge cases.

---

## 🔐 Authentication Scenarios

### Scenario A1: Complete Login Flow
**Preconditions:**
- User exists in database with valid credentials
- User is not currently authenticated
- Browser has no stored tokens

**Test Steps:**
1. Navigate to login page (`/index.html`)
2. Verify login form is displayed
3. Enter valid email address
4. Enter valid password
5. Click "Sign In" button
6. Verify loading spinner appears on button
7. Wait for response
8. Verify success toast appears
9. Verify redirect to correct dashboard based on role

**Expected Outcomes:**
- Token stored in localStorage
- RefreshToken stored in localStorage
- UserInfo stored in localStorage
- Correct dashboard loaded
- User name displayed in header

**Edge Cases:**
- [ ] Network timeout during login
- [ ] Server returns 500 error
- [ ] Response missing required fields
- [ ] Very slow network (>5s response)

---

### Scenario A2: Token Refresh Flow
**Preconditions:**
- User has valid refresh token
- Access token is expired

**Test Steps:**
1. Make authenticated API request
2. Receive 401 response
3. Automatically attempt token refresh
4. Verify new access token received
5. Retry original request with new token
6. Verify request succeeds

**Expected Outcomes:**
- New access token stored
- Original request completes successfully
- User remains logged in
- No redirect to login

**Edge Cases:**
- [ ] Refresh token also expired
- [ ] Refresh endpoint returns error
- [ ] Multiple simultaneous requests during refresh
- [ ] Network error during refresh

---

### Scenario A3: Session Timeout Handling
**Preconditions:**
- User is authenticated
- Session is about to expire

**Test Steps:**
1. User is idle for extended period
2. Session timeout triggers
3. User attempts action requiring authentication
4. Verify appropriate error message shown
5. Verify redirect to login page
6. Verify error parameter in URL

**Expected Outcomes:**
- User sees "session_expired" message
- All auth data cleared
- Redirect to login with error parameter
- Protected content not accessible

---

### Scenario A4: Rate Limiting Handling
**Preconditions:**
- User has exceeded login attempt limit

**Test Steps:**
1. Submit login form (triggering rate limit)
2. Receive 429 response
3. Verify error message shows countdown
4. Verify submit button disabled
5. Wait for countdown to complete
6. Verify button re-enabled
7. Verify can attempt login again

**Expected Outcomes:**
- Clear countdown display (minutes:seconds)
- Button stays disabled during countdown
- Error message updates every second
- Button re-enables after countdown

---

## 📁 File Explorer Scenarios

### Scenario F1: Navigate Folder Hierarchy
**Preconditions:**
- User is authenticated as Professor
- Academic year and semester selected
- User has courses with uploaded documents

**Test Steps:**
1. Navigate to File Explorer tab
2. Verify root folder cards displayed (course folders)
3. Click on a course folder
4. Verify navigation to course contents
5. Verify breadcrumbs updated
6. Click on document type folder (e.g., "Syllabus")
7. Verify files table displayed
8. Click breadcrumb to go back
9. Verify navigation to previous level

**Expected Outcomes:**
- Smooth navigation transitions
- Breadcrumbs always accurate
- Loading indicator during fetches
- Empty state shown for empty folders

**Edge Cases:**
- [ ] Folder with many items (100+)
- [ ] Very deep folder hierarchy (5+ levels)
- [ ] Folder deleted while viewing
- [ ] Permission revoked while viewing

---

### Scenario F2: Upload File Flow
**Preconditions:**
- User is authenticated as Professor
- Viewing own course folder
- Upload button visible

**Test Steps:**
1. Click upload button
2. File picker opens
3. Select valid file (e.g., PDF < 10MB)
4. Verify upload progress indicator
5. Wait for upload completion
6. Verify success toast
7. Verify file appears in list
8. Verify file metadata correct

**Expected Outcomes:**
- Progress bar shows accurate percentage
- File appears without page refresh
- Correct file icon displayed
- File size and date shown

**Edge Cases:**
- [ ] Upload cancelled mid-progress
- [ ] Network disconnection during upload
- [ ] File too large (>10MB)
- [ ] Invalid file type
- [ ] Duplicate filename
- [ ] Special characters in filename

---

### Scenario F3: File Preview Flow
**Preconditions:**
- User has access to files
- File is previewable (PDF, image, text)

**Test Steps:**
1. Click preview button on a PDF file
2. Verify modal opens
3. Verify loading state shown
4. Verify PDF rendered correctly
5. Navigate between pages
6. Use zoom controls
7. Click close button
8. Verify modal closes
9. Verify focus returns to trigger button

**Expected Outcomes:**
- Modal opens with animation
- PDF pages render clearly
- Navigation controls work
- Escape key closes modal
- Click outside closes modal

**Edge Cases:**
- [ ] Large PDF (50+ pages)
- [ ] Corrupted PDF file
- [ ] Non-previewable file type
- [ ] File larger than 5MB warning
- [ ] Modal opened while another open

---

### Scenario F4: File Download Flow
**Preconditions:**
- User has access to file
- File exists on server

**Test Steps:**
1. Click download button
2. Verify download starts
3. Verify file downloads with correct name
4. Verify file content matches original

**Expected Outcomes:**
- Browser download dialog (or auto-download)
- Correct filename in download
- Correct file contents
- No errors in console

**Edge Cases:**
- [ ] Very large file (100MB+)
- [ ] Download timeout
- [ ] File deleted during download
- [ ] Concurrent downloads

---

## 📊 Dashboard Scenarios

### Scenario D1: HOD Dashboard Data Load
**Preconditions:**
- User is authenticated as HOD
- Department has professors and courses

**Test Steps:**
1. Navigate to HOD dashboard
2. Verify academic year dropdown populated
3. Verify current year auto-selected
4. Verify semester dropdown populated
5. Verify current semester auto-selected
6. Verify dashboard stats load
7. Verify submission status table loads
8. Change semester
9. Verify data refreshes

**Expected Outcomes:**
- All statistics accurate
- Submission status shows all professors
- Status badges color-coded correctly
- Filters work correctly

**Edge Cases:**
- [ ] Department with no professors
- [ ] No courses assigned this semester
- [ ] No submissions yet
- [ ] All documents submitted

---

### Scenario D2: Professor Course Upload
**Preconditions:**
- User is authenticated as Professor
- Has courses assigned for current semester

**Test Steps:**
1. Navigate to Professor dashboard
2. Select academic year and semester
3. View assigned courses
4. Click on a course card
5. Expand course details
6. Click upload button for Syllabus
7. Select PDF file
8. Verify upload progress
9. Verify upload success
10. Verify status updates to "Uploaded"

**Expected Outcomes:**
- Course card shows correct status
- Upload progress accurate
- Status badge changes color
- File count updates

**Edge Cases:**
- [ ] Replace existing file
- [ ] Upload multiple files
- [ ] Deadline passed (late submission)
- [ ] Course removed mid-upload

---

### Scenario D3: Deanship Report Generation
**Preconditions:**
- User is authenticated as Deanship
- Data exists for selected semester

**Test Steps:**
1. Navigate to Reports tab
2. Select academic year
3. Select semester
4. Click "Generate Report"
5. Verify report modal opens
6. Verify statistics displayed
7. Click "Export PDF"
8. Verify PDF downloads
9. Verify PDF contains correct data

**Expected Outcomes:**
- Report shows accurate statistics
- All professors/courses included
- PDF formatted correctly
- Charts rendered (if included)

**Edge Cases:**
- [ ] No data for semester
- [ ] Very large report (1000+ rows)
- [ ] PDF generation timeout
- [ ] Browser blocks download

---

## 🔄 State Management Scenarios

### Scenario S1: Tab Persistence
**Preconditions:**
- User on multi-tab dashboard (e.g., Deanship)

**Test Steps:**
1. Click different tab
2. Verify tab content changes
3. Refresh page
4. Verify same tab is active
5. Close and reopen browser
6. Navigate to dashboard
7. Verify tab state restored

**Expected Outcomes:**
- Tab state saved to localStorage
- State restored on page load
- Correct content shown

---

### Scenario S2: Form Data Preservation
**Preconditions:**
- User filling out a form (e.g., Create Professor)

**Test Steps:**
1. Start filling form
2. Enter partial data
3. Navigate away (accidentally or intentionally)
4. Receive warning about unsaved changes
5. Click "Stay"
6. Verify form data preserved
7. Complete and submit form

**Expected Outcomes:**
- Navigation warning shown
- Data not lost if user stays
- Successful submission after warning

---

### Scenario S3: Multi-Tab Session Sync
**Preconditions:**
- User logged in
- Multiple tabs open

**Test Steps:**
1. Open dashboard in Tab A
2. Open dashboard in Tab B
3. Logout from Tab A
4. Switch to Tab B
5. Perform action requiring auth
6. Verify appropriate behavior

**Expected Outcomes:**
- Tab B detects logout
- User redirected to login
- No stale data displayed

---

## 🎨 UI/UX Scenarios

### Scenario U1: Dark Mode Toggle
**Preconditions:**
- User on any page with theme toggle

**Test Steps:**
1. Click theme toggle button
2. Verify theme changes immediately
3. Refresh page
4. Verify theme preference persisted
5. Toggle back
6. Verify original theme restored

**Expected Outcomes:**
- Smooth transition animation
- All components properly themed
- Preference saved in localStorage
- System preference respected on first load

---

### Scenario U2: Responsive Layout
**Preconditions:**
- Testing on various viewport sizes

**Test Steps:**
1. View page at desktop width (1920px)
2. Verify full layout displayed
3. Resize to tablet (768px)
4. Verify layout adapts (sidebar collapses)
5. Resize to mobile (375px)
6. Verify mobile layout
7. Test hamburger menu
8. Verify all content accessible

**Expected Outcomes:**
- No horizontal scrolling
- Touch targets adequate size
- Content readable at all sizes
- Navigation accessible

---

### Scenario U3: Loading States
**Preconditions:**
- Slow network simulation (throttled)

**Test Steps:**
1. Navigate to dashboard
2. Verify skeleton loaders shown
3. Wait for data load
4. Verify smooth transition to content
5. Trigger another data load
6. Verify loading indicator shown

**Expected Outcomes:**
- No content flash (shimmer effect)
- Minimum loading time enforced
- Smooth transitions
- No layout shift

---

### Scenario U4: Error State Display
**Preconditions:**
- Backend returning errors

**Test Steps:**
1. Trigger action that fails (e.g., API down)
2. Verify error toast shown
3. Verify error message is user-friendly
4. Verify retry option available (if applicable)
5. Retry action
6. Verify success after retry

**Expected Outcomes:**
- Clear error message
- No technical jargon
- Actionable guidance
- Recovery possible

---

## 🔒 Security Scenarios

### Scenario X1: XSS Prevention
**Preconditions:**
- User input fields available

**Test Steps:**
1. Enter script tag in input field: `<script>alert('XSS')</script>`
2. Submit form
3. View rendered content
4. Verify script not executed
5. Verify content safely escaped

**Expected Outcomes:**
- No script execution
- Content displayed as text
- HTML entities escaped

---

### Scenario X2: CSRF Protection
**Preconditions:**
- User authenticated

**Test Steps:**
1. Perform state-changing action
2. Verify request includes proper headers
3. Attempt action from different origin
4. Verify request rejected

**Expected Outcomes:**
- All mutations include auth header
- Cross-origin requests blocked
- Proper error response

---

### Scenario X3: Role-Based Access
**Preconditions:**
- User logged in with specific role

**Test Steps:**
1. Try to access page for different role (URL manipulation)
2. Verify access denied
3. Verify redirect to appropriate page
4. Verify no data leakage

**Expected Outcomes:**
- Access denied message
- Redirect to login or own dashboard
- No sensitive data exposed

---

## 📱 Accessibility Scenarios

### Scenario ACC1: Keyboard Navigation
**Preconditions:**
- User navigating via keyboard only

**Test Steps:**
1. Start at page top
2. Tab through all interactive elements
3. Verify focus visible on each element
4. Verify logical tab order
5. Enter to activate buttons/links
6. Escape to close modals/dropdowns
7. Arrow keys for menu navigation

**Expected Outcomes:**
- All elements reachable
- Focus ring visible
- Logical order maintained
- All actions possible

---

### Scenario ACC2: Screen Reader Compatibility
**Preconditions:**
- Screen reader enabled (NVDA, VoiceOver, etc.)

**Test Steps:**
1. Navigate to dashboard
2. Verify page title announced
3. Navigate through landmarks
4. Verify form labels announced
5. Trigger error state
6. Verify error announced
7. Open modal
8. Verify modal announced

**Expected Outcomes:**
- All content accessible
- Live regions announce updates
- Form instructions clear
- Error messages announced

---

### Scenario ACC3: Color Contrast
**Preconditions:**
- Testing tool for contrast ratio

**Test Steps:**
1. Check all text against background
2. Verify 4.5:1 minimum for normal text
3. Verify 3:1 minimum for large text
4. Check both light and dark modes
5. Verify status badges readable

**Expected Outcomes:**
- WCAG AA compliant
- All text readable
- Interactive states distinguishable

---

## 🧪 Test Data Requirements

### User Accounts for Testing
```javascript
const testUsers = {
    admin: {
        email: 'admin@alquds.edu',
        password: 'AdminPass123!',
        role: 'ROLE_ADMIN'
    },
    deanship: {
        email: 'deanship@alquds.edu',
        password: 'DeanPass123!',
        role: 'ROLE_DEANSHIP'
    },
    hod: {
        email: 'hod@alquds.edu',
        password: 'HodPass123!',
        role: 'ROLE_HOD',
        departmentId: 1
    },
    professor: {
        email: 'professor@alquds.edu',
        password: 'ProfPass123!',
        role: 'ROLE_PROFESSOR',
        departmentId: 1
    }
};
```

### Test Files
- `test-document.pdf` - Valid PDF (1MB)
- `large-document.pdf` - Large PDF (15MB)
- `test-image.png` - Valid image (500KB)
- `test-text.txt` - Plain text file
- `test-code.js` - Code file
- `corrupted.pdf` - Invalid PDF file
- `malicious.exe` - Disallowed file type

### Academic Year/Semester Data
```javascript
const testAcademicData = {
    academicYear: {
        id: 1,
        yearCode: '2024-2025',
        isActive: true
    },
    semesters: [
        { id: 1, type: 'FIRST', isActive: true },
        { id: 2, type: 'SECOND', isActive: false },
        { id: 3, type: 'SUMMER', isActive: false }
    ]
};
```

---

## 📋 Test Environment Setup

### Browser Coverage
- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)
- Mobile Safari (iOS)
- Chrome Mobile (Android)

### Viewport Sizes
- Desktop: 1920x1080, 1366x768
- Tablet: 768x1024, 1024x768
- Mobile: 375x667, 414x896

### Network Conditions
- Fast 3G (for loading state tests)
- Slow 3G (for timeout tests)
- Offline (for error handling)

---

## Notes

- Always reset test database/state between scenarios
- Use data-testid attributes for reliable element selection
- Mock time-sensitive operations (countdowns, timeouts)
- Test both happy path and error paths
- Document any flaky tests and their root causes
- Screenshot on failure for debugging
- Use realistic test data sizes
