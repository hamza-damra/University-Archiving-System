# Manual Testing Guide for Task 6

This guide provides instructions for manually testing the filter change reactivity in the Dean Dashboard File Explorer.

## Prerequisites

1. Ensure the application is running
2. Log in as a Dean user
3. Navigate to the File Explorer page

## Task 6.1: Test Academic Year Filter Change

### Steps:

1. **Initial State**
   - Open the Dean Dashboard File Explorer page
   - Observe the initial state (should show context message if no filters selected)

2. **Select Academic Year**
   - Click on the Academic Year dropdown
   - Select an academic year (e.g., "2024-2025")
   - **Expected Result**: 
     - File Explorer should clear any existing content
     - Context message should be displayed (since semester is not yet selected)
     - Semester dropdown should be enabled

3. **Change Academic Year**
   - Select a different academic year from the dropdown
   - **Expected Result**:
     - File Explorer should clear
     - Context message should be shown
     - Semester dropdown should be cleared/reset
     - No stale data from previous academic year should be visible

4. **Verification Checklist**
   - [ ] File Explorer clears when Academic Year changes
   - [ ] Context message is displayed
   - [ ] Semester dropdown is cleared
   - [ ] No folders/files from previous context remain visible

### Requirements Validated: 1.1

---

## Task 6.2: Test Semester Filter Change

### Steps:

1. **Select Academic Year and Semester**
   - Select an academic year (e.g., "2024-2025")
   - Select a semester (e.g., "First Semester")
   - **Expected Result**:
     - File Explorer should load and display folders
     - Folders should be displayed as cards (no tree view)
     - Files should be displayed in a table

2. **Verify Initial Data Load**
   - Observe the folders and files displayed
   - Note the academic year and semester context
   - **Expected Result**:
     - Folders correspond to the selected academic year and semester
     - Data is displayed correctly

3. **Change Semester**
   - Select a different semester from the dropdown (e.g., "Second Semester")
   - **Expected Result**:
     - File Explorer should clear the old data
     - Loading indicator should appear briefly
     - New folders and files for the selected semester should load
     - Folders and files correspond to the new semester

4. **Change Back to Original Semester**
   - Select the original semester again
   - **Expected Result**:
     - File Explorer should reload the original data
     - No stale data from the second semester should remain

5. **Verification Checklist**
   - [ ] File Explorer loads new data when semester changes
   - [ ] Loading indicator is displayed during load
   - [ ] Folders and files are displayed correctly
   - [ ] Data matches the selected filters (academic year + semester)
   - [ ] No stale data from previous semester remains
   - [ ] Navigation state (breadcrumbs, current path) is reset

### Requirements Validated: 1.2, 1.3

---

## Additional Verification

### Browser Console Checks

Open the browser console (F12) and look for:

1. **State Reset Logs**
   - Should see `[FileExplorerPage] Context changed:` logs
   - Should see state reset operations

2. **No Errors**
   - No JavaScript errors should appear
   - No failed API requests (check Network tab)

3. **Correct API Calls**
   - API calls should use the correct academicYearId and semesterId
   - Check the Network tab to verify request parameters

### Visual Checks

1. **Layout**
   - No tree view panel on the left (single-column layout)
   - Folders displayed as cards
   - Files displayed in a table

2. **Loading States**
   - Loading indicator appears during data load
   - UI is disabled (reduced opacity) during load
   - Loading indicator disappears after load completes

3. **Empty States**
   - Context message is shown when no filters are selected
   - Appropriate message when no folders/files exist

---

## Troubleshooting

### Issue: File Explorer doesn't update when filters change

**Solution**: 
- Check browser console for errors
- Verify that `fileExplorerState.resetData()` is being called
- Check that the FileExplorer instance is being reused (not recreated)

### Issue: Stale data remains visible

**Solution**:
- Verify that `resetData()` is called before `loadRoot()`
- Check that the DOM is being cleared properly
- Verify that the correct academicYearId and semesterId are being passed

### Issue: Loading indicator doesn't appear

**Solution**:
- Check that `showLoading(true)` is called before data load
- Verify that `showLoading(false)` is called after load completes
- Check CSS styles for the loading indicator

---

## Completion Criteria

Both tasks 6.1 and 6.2 are complete when:

1. All verification checklist items are checked
2. No errors appear in the browser console
3. Filter changes work smoothly without requiring page refresh
4. Data displayed matches the selected filters
5. No stale data from previous contexts remains visible

---

## Notes

- These are manual tests that complement the automated property-based tests
- The property-based tests (6.3, 6.4, 6.5) have already passed and validate the core logic
- Manual testing verifies the end-to-end user experience
