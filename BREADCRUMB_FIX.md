# Breadcrumb Path Not Updating Fix

## Issue

When changing filters (e.g., from 2024-2025 to 2025-2026), the breadcrumb path was not updating. It continued to show the old academic year path even after the filter changed.

Example:
- Changed filter from "2024-2025" to "2025-2026"
- Breadcrumb still showed: "2024-2025 > First Semester > Sanad Damra > CS101"
- Should show: "2025-2026 > First Semester > Sanad Damra > CS101"

## Root Cause

The `loadRoot` method was not clearing the breadcrumbs immediately when called. The old breadcrumbs remained visible until new breadcrumbs were loaded from the API via `loadBreadcrumbs(this.currentPath)`.

This created a brief period where stale breadcrumbs from the previous filter were visible, and if the API call failed or was slow, the old breadcrumbs would persist.

## Solution

Added immediate breadcrumb clearing at the start of `loadRoot()`, right after clearing the folder structure and files section:

```javascript
async loadRoot(academicYearId, semesterId, isBackground = false) {
    // Clear folder structure and files section
    const treeContainer = document.getElementById('fileExplorerTree');
    const fileListContainer = document.getElementById('fileExplorerFileList');
    
    if (treeContainer) {
        treeContainer.innerHTML = this.renderEmptyState('Loading folders...', 'info');
    }
    if (fileListContainer) {
        fileListContainer.innerHTML = this.renderEmptyState('Loading files...', 'info');
    }

    // CRITICAL FIX: Clear breadcrumbs immediately to prevent showing stale path
    this.breadcrumbs = [];
    fileExplorerState.setBreadcrumbs([]);
    this.renderBreadcrumbs();

    // ... rest of loadRoot
}
```

## What This Does

1. **Clears breadcrumbs array**: `this.breadcrumbs = []`
2. **Updates state**: `fileExplorerState.setBreadcrumbs([])`
3. **Renders empty breadcrumbs**: `this.renderBreadcrumbs()` shows "Select a folder to navigate"

This ensures that:
- Old breadcrumbs are immediately removed when filter changes
- User sees "Select a folder to navigate" message at root level
- New breadcrumbs are loaded when user navigates into folders
- No stale path information is ever visible

## Testing

1. Open Dean Dashboard
2. Select Academic Year: 2024-2025, Semester: First
3. Navigate into a course (e.g., CS101)
4. Note the breadcrumb path
5. **Change filter** to Academic Year: 2025-2026
6. **Expected**: Breadcrumbs clear immediately, showing "Select a folder to navigate"
7. Navigate into a course again
8. **Expected**: Breadcrumbs show correct path with "2025-2026"

## Complete Fix Summary

The complete fix for the filter change issue now includes THREE parts:

### 1. Show Empty State in Files Section (loadRoot)
```javascript
fileListContainer.innerHTML = this.renderEmptyState(
    'Select a folder to view its contents',
    'folder'
);
```

### 2. Prevent onStateChange from Overriding Empty State
```javascript
const isAtRoot = this.currentNode === this.treeRoot;
if (this.currentNode && !isAtRoot) {
    this.renderFileList(this.currentNode);
}
```

### 3. Clear Breadcrumbs Immediately (NEW)
```javascript
this.breadcrumbs = [];
fileExplorerState.setBreadcrumbs([]);
this.renderBreadcrumbs();
```

All three fixes work together to ensure:
- ✅ Files section shows empty state after filter change
- ✅ Old files are never visible
- ✅ Breadcrumbs are cleared and updated correctly
- ✅ User has clear visual feedback that filter has changed

---

**File Modified:** `src/main/resources/static/js/file-explorer.js`  
**Method:** `loadRoot(academicYearId, semesterId, isBackground)`  
**Lines Added:** 3 lines to clear breadcrumbs immediately
