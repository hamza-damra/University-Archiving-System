# Cross-Browser Testing Guide

## Overview
This guide provides step-by-step instructions for testing the Dean File Explorer filter fix and tree view removal across Chrome, Firefox, and Edge browsers.

## Prerequisites
- Application running on http://localhost:8080
- Dean account credentials (rania.alqude@example.com)
- Test data available in the system

## Test Scenarios

### Scenario 1: Filter Change - Academic Year
**Purpose**: Verify File Explorer clears when Academic Year changes

**Steps**:
1. Login as Dean user
2. Navigate to File Explorer page
3. Select an Academic Year from dropdown
4. Select a Semester from dropdown
5. Verify folders/files are displayed
6. Change Academic Year to a different value
7. **Expected**: File Explorer should clear, semester dropdown should reset
8. **Expected**: Context message should appear prompting to select semester

### Scenario 2: Filter Change - Semester
**Purpose**: Verify File Explorer loads new data when Semester changes

**Steps**:
1. Login as Dean user
2. Navigate to File Explorer page
3. Select an Academic Year
4. Select a Semester (e.g., "First")
5. Note the folders displayed
6. Change Semester to different value (e.g., "Second")
7. **Expected**: File Explorer should reload with new data
8. **Expected**: Folders should correspond to new semester

### Scenario 3: Tree View Removal
**Purpose**: Verify tree panel is not visible for Dean role

**Steps**:
1. Login as Dean user
2. Navigate to File Explorer page
3. Select Academic Year and Semester
4. **Expected**: No tree panel on left side
5. **Expected**: Single-column layout with folder cards
6. **Expected**: Layout matches Professor Dashboard style

### Scenario 4: Folder Navigation
**Purpose**: Verify folder navigation works without tree view

**Steps**:
1. Login as Dean user
2. Navigate to File Explorer page
3. Select Academic Year and Semester
4. Click on a folder card
5. **Expected**: Navigate into folder
6. **Expected**: Breadcrumbs update to show path
7. Click on another nested folder
8. **Expected**: Navigate deeper
9. **Expected**: Breadcrumbs show full path

### Scenario 5: Breadcrumb Navigation
**Purpose**: Verify breadcrumb navigation works correctly

**Steps**:
1. Login as Dean user
2. Navigate to File Explorer page
3. Select Academic Year and Semester
4. Navigate to a nested folder (2-3 levels deep)
5. Click on a breadcrumb segment (not the last one)
6. **Expected**: Navigate back to that folder level
7. **Expected**: Content updates to show that folder's contents

### Scenario 6: Loading Indicators
**Purpose**: Verify loading states during filter changes

**Steps**:
1. Login as Dean user
2. Navigate to File Explorer page
3. Select Academic Year
4. Select Semester
5. **Expected**: Loading indicator appears briefly
6. **Expected**: File Explorer becomes interactive after load
7. Change Semester
8. **Expected**: Loading indicator appears again
9. **Expected**: Previous content is cleared before new content loads

### Scenario 7: Empty State
**Purpose**: Verify empty state when no filters selected

**Steps**:
1. Login as Dean user
2. Navigate to File Explorer page
3. **Expected**: Context message displayed
4. **Expected**: No folders or files shown
5. Select Academic Year only (no semester)
6. **Expected**: Context message still displayed
7. **Expected**: Prompt to select semester

### Scenario 8: Error Handling
**Purpose**: Verify error handling during filter changes

**Steps**:
1. Login as Dean user
2. Navigate to File Explorer page
3. Open browser DevTools Network tab
4. Set network to "Offline" mode
5. Select Academic Year and Semester
6. **Expected**: Error message displayed
7. **Expected**: Loading indicator removed
8. Set network back to "Online"
9. Try again
10. **Expected**: Should work normally

## Browser-Specific Checks

### Chrome
- [ ] All scenarios pass
- [ ] Layout renders correctly
- [ ] No console errors
- [ ] Smooth animations
- [ ] Proper font rendering

### Firefox
- [ ] All scenarios pass
- [ ] Layout renders correctly
- [ ] No console errors
- [ ] Grid layout works properly
- [ ] Event handlers work correctly

### Edge
- [ ] All scenarios pass
- [ ] Layout renders correctly
- [ ] No console errors
- [ ] Tailwind CSS classes render properly
- [ ] No compatibility warnings

## Layout Verification Checklist

For each browser, verify:
- [ ] Single-column layout (no tree panel)
- [ ] Folder cards display in grid
- [ ] Breadcrumbs at top of File Explorer
- [ ] Proper spacing and padding
- [ ] Responsive design works (if applicable)
- [ ] Icons render correctly
- [ ] Colors match design system

## Performance Checks

For each browser, verify:
- [ ] Filter changes are responsive (< 1 second)
- [ ] No memory leaks after multiple filter changes
- [ ] Smooth scrolling in file list
- [ ] No layout shifts during load

## Console Error Checks

For each browser:
1. Open DevTools Console
2. Perform all test scenarios
3. **Expected**: No JavaScript errors
4. **Expected**: No network errors (except in offline test)
5. **Expected**: No deprecation warnings

## Test Results Template

### Chrome Results
- Version: _______
- Date: _______
- Tester: _______

| Scenario | Pass/Fail | Notes |
|----------|-----------|-------|
| 1. Academic Year Filter | | |
| 2. Semester Filter | | |
| 3. Tree View Removal | | |
| 4. Folder Navigation | | |
| 5. Breadcrumb Navigation | | |
| 6. Loading Indicators | | |
| 7. Empty State | | |
| 8. Error Handling | | |

### Firefox Results
- Version: _______
- Date: _______
- Tester: _______

| Scenario | Pass/Fail | Notes |
|----------|-----------|-------|
| 1. Academic Year Filter | | |
| 2. Semester Filter | | |
| 3. Tree View Removal | | |
| 4. Folder Navigation | | |
| 5. Breadcrumb Navigation | | |
| 6. Loading Indicators | | |
| 7. Empty State | | |
| 8. Error Handling | | |

### Edge Results
- Version: _______
- Date: _______
- Tester: _______

| Scenario | Pass/Fail | Notes |
|----------|-----------|-------|
| 1. Academic Year Filter | | |
| 2. Semester Filter | | |
| 3. Tree View Removal | | |
| 4. Folder Navigation | | |
| 5. Breadcrumb Navigation | | |
| 6. Loading Indicators | | |
| 7. Empty State | | |
| 8. Error Handling | | |

## Known Issues

Document any browser-specific issues discovered during testing:

### Chrome
- None known

### Firefox
- None known

### Edge
- None known

## Automated Testing Script

For quick verification, you can use the browser console to run these checks:

```javascript
// Verify no tree panel exists
console.assert(
    document.querySelector('#fileExplorerTree') === null,
    'Tree panel should not exist for Dean role'
);

// Verify single-column layout
const fileExplorer = document.querySelector('.file-explorer .grid');
console.assert(
    fileExplorer.classList.contains('grid-cols-1'),
    'Should use single-column layout'
);

// Verify FileExplorer instance exists
console.assert(
    window.fileExplorerInstance !== undefined,
    'FileExplorer instance should be available'
);

// Verify hideTree option is set
console.assert(
    window.fileExplorerInstance.options.hideTree === true,
    'hideTree option should be true for Dean role'
);

console.log('✓ All automated checks passed');
```

## Conclusion

After completing all tests across all three browsers, document:
1. Overall pass/fail status
2. Any browser-specific issues
3. Recommendations for fixes (if needed)
4. Sign-off for deployment
