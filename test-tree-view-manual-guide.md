# Manual Testing Guide: Tree View Removal

## Task 7: Test Tree View Removal

This guide provides step-by-step instructions for manually testing the tree view removal feature in the Dean Dashboard File Explorer.

---

## Test 7.1: Verify Tree Panel is Not Visible in Dean Dashboard

### Prerequisites
- Application is running
- Dean account credentials available
- Academic year and semester data exists in database

### Steps

1. **Open the Dean Dashboard**
   - Navigate to: `http://localhost:8080/deanship/file-explorer.html`
   - Or use the navigation menu to access File Explorer

2. **Log in as Dean**
   - Use Dean credentials
   - Verify successful login

3. **Select Academic Context**
   - Select an Academic Year from the dropdown
   - Select a Semester from the dropdown
   - Wait for File Explorer to load

4. **Verify Tree Panel Absence**
   - ✓ **PASS**: No tree panel visible on the left side
   - ✓ **PASS**: Only breadcrumbs and file list are visible
   - ✗ **FAIL**: Tree panel with "Folder Structure" heading is visible

5. **Verify Single-Column Layout**
   - ✓ **PASS**: File list takes full width of container
   - ✓ **PASS**: No left sidebar with folder tree
   - ✗ **FAIL**: Layout is split into two columns

### Expected Results
- Tree panel should NOT be visible
- Layout should be single-column (full width)
- Only breadcrumbs and file list should be displayed

---

## Test 7.2: Test Folder Navigation with Card View

### Prerequisites
- Completed Test 7.1
- File Explorer is loaded with data

### Steps

1. **Identify Folder Cards**
   - Look for blue folder cards in the file list
   - Each card should have:
     - Folder icon (blue)
     - Folder name
     - Arrow icon on the right
     - Hover effect (darker blue background)

2. **Click on a Folder Card**
   - Click on any folder card
   - Observe the navigation behavior

3. **Verify Navigation**
   - ✓ **PASS**: File Explorer loads the folder contents
   - ✓ **PASS**: Breadcrumbs update to show current path
   - ✓ **PASS**: URL or state reflects the navigation
   - ✗ **FAIL**: Nothing happens or error occurs

4. **Test Multiple Levels**
   - Navigate into nested folders (if available)
   - Verify each level loads correctly
   - Verify breadcrumbs update at each level

5. **Verify Folder Card Styling**
   - ✓ **PASS**: Cards have blue background (bg-blue-50)
   - ✓ **PASS**: Cards have blue border (border-blue-200)
   - ✓ **PASS**: Hover effect changes background (hover:bg-blue-100)
   - ✓ **PASS**: Arrow icon animates on hover (translate-x-1)

### Expected Results
- Clicking folder cards should navigate into folders
- Breadcrumbs should update to reflect current path
- Navigation should work smoothly without errors

---

## Test 7.3: Test Breadcrumb Navigation

### Prerequisites
- Completed Test 7.2
- Currently navigated into a nested folder (depth > 1)

### Steps

1. **Verify Breadcrumb Display**
   - Look at the breadcrumb navigation bar
   - Verify it shows the full path from root to current location
   - ✓ **PASS**: Breadcrumbs show complete path
   - ✓ **PASS**: Home icon appears for first breadcrumb
   - ✓ **PASS**: Chevron separators between segments
   - ✗ **FAIL**: Breadcrumbs missing or incomplete

2. **Test Breadcrumb Click Navigation**
   - Click on a breadcrumb segment (not the last one)
   - Observe the navigation behavior

3. **Verify Navigation to Clicked Level**
   - ✓ **PASS**: File Explorer navigates to the clicked folder level
   - ✓ **PASS**: Breadcrumbs update to reflect new location
   - ✓ **PASS**: File list shows contents of clicked folder
   - ✗ **FAIL**: Navigation doesn't work or goes to wrong location

4. **Test Back Button (if visible)**
   - If a back button appears in breadcrumbs
   - Click it to go to parent folder
   - ✓ **PASS**: Navigates to parent folder
   - ✗ **FAIL**: Back button doesn't work

5. **Test Multiple Breadcrumb Clicks**
   - Navigate deep into folder structure
   - Click on various breadcrumb segments
   - Verify each click navigates correctly

### Expected Results
- Breadcrumbs should display complete path hierarchy
- Clicking breadcrumb segments should navigate to that level
- Current location should be shown in gray (non-clickable)
- All other segments should be blue links

---

## Test 7.4-7.6: Property-Based Tests (Automated)

These tests are automated and run via the test file:
- `src/test/resources/static/js/file-explorer-page-pbt.test.js`

### Running the Tests

```bash
node src/test/resources/static/js/file-explorer-page-pbt.test.js
```

### Expected Output
```
✓ PASS: Property 15: Folder navigation (Validates: Requirements 5.2)
✓ PASS: Property 16: Breadcrumb display (Validates: Requirements 5.3)
✓ PASS: Property 17: Breadcrumb navigation (Validates: Requirements 5.4)
```

---

## Visual Verification Checklist

### Layout Comparison

**Dean Dashboard (hideTree: true)**
```
┌─────────────────────────────────────────┐
│ Breadcrumbs: Home > Folder1 > Folder2  │
├─────────────────────────────────────────┤
│                                         │
│  Folders (Blue Cards)                   │
│  ┌─────────────────────────────────┐   │
│  │ 📁 Folder Name              →   │   │
│  └─────────────────────────────────┘   │
│                                         │
│  Files (Table)                          │
│  ┌─────────────────────────────────┐   │
│  │ Name | Size | Date | Actions    │   │
│  └─────────────────────────────────┘   │
│                                         │
└─────────────────────────────────────────┘
```

**Professor Dashboard (hideTree: false) - For Comparison**
```
┌─────────────────────────────────────────┐
│ Breadcrumbs: Home > Folder1 > Folder2  │
├───────────┬─────────────────────────────┤
│ Tree View │  Files                      │
│           │                             │
│ ▼ Root    │  Folders (Blue Cards)       │
│   ▼ F1    │  ┌───────────────────────┐ │
│     F2    │  │ 📁 Folder Name    →   │ │
│           │  └───────────────────────┘ │
│           │                             │
│           │  Files (Table)              │
│           │  ┌───────────────────────┐ │
│           │  │ Name | Size | Actions │ │
│           │  └───────────────────────┘ │
└───────────┴─────────────────────────────┘
```

---

## Troubleshooting

### Issue: Tree panel still visible
- **Check**: Verify `hideTree: true` is set in FileExplorerPage initialization
- **Location**: `src/main/resources/static/js/file-explorer-page.js`
- **Line**: Look for `new FileExplorer('fileExplorerContainer', { ... hideTree: true })`

### Issue: Folder cards not clickable
- **Check**: Verify onclick handler is present: `onclick="window.fileExplorerInstance.handleNodeClick(...)"`
- **Check**: Verify FileExplorer instance is assigned to `window.fileExplorerInstance`

### Issue: Breadcrumbs not updating
- **Check**: Verify `loadBreadcrumbs()` is called after navigation
- **Check**: Verify breadcrumb API endpoint is working
- **Check**: Browser console for errors

### Issue: Layout looks wrong
- **Check**: Verify grid classes: `grid-cols-1` (not `grid-cols-3`)
- **Check**: Verify file list doesn't have `md:col-span-2` class
- **Check**: Clear browser cache and reload

---

## Success Criteria

All tests pass when:
- ✓ Tree panel is not visible in Dean Dashboard
- ✓ Single-column layout is used
- ✓ Folder cards are clickable and navigate correctly
- ✓ Breadcrumbs display complete path
- ✓ Breadcrumb segments are clickable and navigate correctly
- ✓ All property-based tests pass (100 iterations each)
- ✓ Visual layout matches expected design

---

## Test Results Template

```
Test 7.1: Verify Tree Panel Not Visible
Status: [ ] PASS [ ] FAIL
Notes: _________________________________

Test 7.2: Folder Navigation with Card View
Status: [ ] PASS [ ] FAIL
Notes: _________________________________

Test 7.3: Breadcrumb Navigation
Status: [ ] PASS [ ] FAIL
Notes: _________________________________

Test 7.4: Property Test - Folder Navigation
Status: [ ] PASS [ ] FAIL
Iterations: 100
Failures: ___

Test 7.5: Property Test - Breadcrumb Display
Status: [ ] PASS [ ] FAIL
Iterations: 100
Failures: ___

Test 7.6: Property Test - Breadcrumb Navigation
Status: [ ] PASS [ ] FAIL
Iterations: 100
Failures: ___
```
