# Design Document

## Overview

This design document outlines the technical approach to fix two critical issues in the Dean Dashboard File Explorer:

1. **Filter Reactivity Issue**: Implementing proper state management to ensure the File Explorer updates immediately when Academic Year or Semester filters change
2. **UI Simplification**: Removing the tree view panel and adopting a single-column card-based layout similar to the Professor Dashboard

The solution involves modifying the `file-explorer-page.js` module to properly reset state before loading new data, and updating the `FileExplorer` component configuration to disable the tree view for the Dean role.

## Architecture

### Current Architecture (Problematic)

```
DeanshipLayout (deanship-common.js)
  ├─ Manages filter dropdowns
  ├─ Notifies callbacks on filter change
  └─ Persists selections to localStorage

FileExplorerPage (file-explorer-page.js)
  ├─ Listens to filter changes
  ├─ Creates FileExplorer instance (once)
  └─ Calls loadRoot() on filter change
      └─ PROBLEM: Doesn't reset state first!

FileExplorer (file-explorer.js)
  ├─ Renders tree view (left panel) + file list (right panel)
  ├─ Manages internal state
  └─ Subscribes to FileExplorerState
      └─ PROBLEM: Tree view unnecessary for Dean role!
```

### Proposed Architecture (Fixed)

```
DeanshipLayout (deanship-common.js)
  ├─ Manages filter dropdowns
  ├─ Notifies callbacks on filter change
  └─ Persists selections to localStorage

FileExplorerPage (file-explorer-page.js)
  ├─ Listens to filter changes
  ├─ Creates FileExplorer instance (once) with hideTree: true
  └─ On filter change:
      ├─ 1. Reset state (fileExplorerState.resetData())
      ├─ 2. Update context (fileExplorerState.setContext())
      └─ 3. Load new data (fileExplorer.loadRoot())

FileExplorer (file-explorer.js)
  ├─ Renders single-column layout (no tree panel)
  ├─ Shows folders as cards
  ├─ Manages internal state
  └─ Subscribes to FileExplorerState
```

## Components and Interfaces

### Modified Components

#### 1. FileExplorerPage Class (`file-explorer-page.js`)

**Current Implementation:**
```javascript
async initializeFileExplorer(academicYearId, semesterId) {
    if (!this.fileExplorer) {
        this.fileExplorer = new FileExplorer('fileExplorerContainer', {
            role: 'DEANSHIP',
            showAllDepartments: true,
            showProfessorLabels: true,
            readOnly: true
        });
    }
    await this.fileExplorer.loadRoot(academicYearId, semesterId);
}
```

**Proposed Implementation:**
```javascript
async initializeFileExplorer(academicYearId, semesterId) {
    // Import fileExplorerState
    const { fileExplorerState } = await import('./file-explorer-state.js');
    
    // Reset state before loading new data (matching Professor Dashboard pattern)
    fileExplorerState.resetData();
    
    // Create FileExplorer instance if it doesn't exist
    if (!this.fileExplorer) {
        this.fileExplorer = new FileExplorer('fileExplorerContainer', {
            role: 'DEANSHIP',
            showAllDepartments: true,
            showProfessorLabels: true,
            readOnly: true,
            hideTree: true  // NEW: Hide tree view for Dean role
        });
        window.fileExplorerInstance = this.fileExplorer;
    }
    
    // Update context in state
    const context = this.layout.getSelectedContext();
    if (context.academicYear && context.semester) {
        fileExplorerState.setContext(
            academicYearId,
            semesterId,
            context.academicYear.yearCode,
            context.semester.name
        );
    }
    
    // Load root for the selected academic year and semester
    await this.fileExplorer.loadRoot(academicYearId, semesterId);
}
```

#### 2. FileExplorer Class (`file-explorer.js`)

**New Configuration Option:**
```javascript
constructor(containerId, options = {}) {
    this.options = {
        // ... existing options ...
        hideTree: options.hideTree || false,  // NEW: Option to hide tree view
        ...options
    };
}
```

**Modified render() Method:**
```javascript
render() {
    const headerMessageHtml = this.options.headerMessage ? `
        <div class="bg-blue-50 border-b border-blue-200 px-4 py-2">
            <p class="text-sm text-gray-600">${this.escapeHtml(this.options.headerMessage)}</p>
        </div>
    ` : '';

    // Determine layout based on hideTree option
    const layoutClass = this.options.hideTree ? 'grid-cols-1' : 'grid-cols-1 md:grid-cols-3';
    const treeViewHtml = this.options.hideTree ? '' : `
        <!-- Tree View -->
        <div class="md:col-span-1 bg-white border border-gray-200 rounded-lg p-4">
            <h3 class="text-sm font-semibold text-gray-700 mb-3">Folder Structure</h3>
            <div id="fileExplorerTree" class="space-y-1">
                ${this.renderNoSemesterSelected()}
            </div>
        </div>
    `;
    
    const fileListColSpan = this.options.hideTree ? '' : 'md:col-span-2';

    this.container.innerHTML = `
        <div class="file-explorer">
            ${headerMessageHtml}
            
            <!-- Breadcrumbs -->
            <div id="fileExplorerBreadcrumbs" class="bg-gray-50 px-4 py-3 border-b border-gray-200">
                <nav class="flex" aria-label="Breadcrumb">
                    <ol class="inline-flex items-center space-x-1 md:space-x-3">
                        <li class="inline-flex items-center">
                            <span class="text-sm text-gray-500">Select a semester to browse files</span>
                        </li>
                    </ol>
                </nav>
            </div>

            <!-- File Explorer Content -->
            <div class="grid ${layoutClass} gap-4 p-4">
                ${treeViewHtml}
                
                <!-- File List -->
                <div class="${fileListColSpan} bg-white border border-gray-200 rounded-lg p-4">
                    <h3 class="text-sm font-semibold text-gray-700 mb-3">Files</h3>
                    <div id="fileExplorerFileList" class="overflow-x-auto">
                        ${this.renderNoSemesterSelected()}
                    </div>
                </div>
            </div>
        </div>
    `;
}
```

**Modified renderTree() Method:**
```javascript
renderTree(node) {
    // Skip rendering if tree is hidden
    if (this.options.hideTree) {
        return;
    }
    
    const container = document.getElementById('fileExplorerTree');
    if (!container) return;
    
    // ... existing tree rendering logic ...
}
```

## Data Models

### FileExplorerState

The existing `FileExplorerState` class already provides the necessary state management methods:

```javascript
class FileExplorerState {
    // Existing methods used in the fix:
    resetData()           // Clears all state
    setContext(...)       // Sets academic context
    setTreeRoot(...)      // Sets tree root node
    setCurrentNode(...)   // Sets current node and path
    setBreadcrumbs(...)   // Sets breadcrumb trail
}
```

### FileExplorer Configuration

```typescript
interface FileExplorerOptions {
    role: 'PROFESSOR' | 'HOD' | 'DEANSHIP';
    readOnly: boolean;
    showAllDepartments?: boolean;
    showProfessorLabels?: boolean;
    showOwnershipLabels?: boolean;
    headerMessage?: string;
    hideTree?: boolean;  // NEW: Hide tree view panel
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Filter change clears UI
*For any* File Explorer state with displayed content, when a filter change occurs, the File Explorer container should be cleared of all folder and file elements before new data is loaded.
**Validates: Requirements 1.1**

### Property 2: Semester change triggers load
*For any* valid semester selection, changing the semester filter should trigger a call to loadRoot() with the correct academicYearId and semesterId parameters.
**Validates: Requirements 1.2**

### Property 3: Displayed data matches filters
*For any* Academic Year and Semester combination, the folders and files displayed in the File Explorer should correspond to the selected filters (verified by checking API request parameters).
**Validates: Requirements 1.3**

### Property 4: Navigation state reset
*For any* File Explorer state with a non-root current path, when a filter change occurs, the current path should be reset to empty/root and breadcrumbs array should be empty.
**Validates: Requirements 1.4**

### Property 5: Automatic loading on filter selection
*For any* state where both Academic Year and Semester are selected, the File Explorer should automatically call loadRoot() without requiring additional user interaction.
**Validates: Requirements 1.5**

### Property 6: State reset on filter change
*For any* File Explorer state with non-null values, when a filter change occurs, all state properties (currentNode, currentPath, breadcrumbs) should be reset to null/empty.
**Validates: Requirements 2.1**

### Property 7: State update after load
*For any* successful data load operation, the File Explorer state should be updated with the new folder structure and current node matching the loaded data.
**Validates: Requirements 2.3**

### Property 8: Instance preservation
*For any* sequence of filter changes, the FileExplorer instance reference should remain the same (not recreated on each change).
**Validates: Requirements 2.4**

### Property 9: UI cleanliness after reset
*For any* File Explorer with displayed content, after state reset, the DOM should contain no folder cards or file table rows from the previous context.
**Validates: Requirements 2.5**

### Property 10: Reset before load sequence
*For any* filter change operation, the resetData() method should be called before the loadRoot() method.
**Validates: Requirements 3.2**

### Property 11: Operation sequence
*For any* filter change, the operations should occur in this order: resetData(), setContext(), loadRoot(), with no operations occurring between them.
**Validates: Requirements 3.4**

### Property 12: Loading indicator display
*For any* filter change initiation, a loading indicator element should be present in the DOM during the load operation.
**Validates: Requirements 4.1**

### Property 13: Interaction blocking during load
*For any* active load operation, the File Explorer container should have pointer-events disabled or reduced opacity.
**Validates: Requirements 4.2**

### Property 14: Loading cleanup on success
*For any* successful load completion, the loading indicator should be removed and pointer-events should be restored.
**Validates: Requirements 4.3**

### Property 15: Folder navigation
*For any* folder card in the File Explorer, clicking on it should call the navigation method with the folder's path.
**Validates: Requirements 5.2**

### Property 16: Breadcrumb display
*For any* navigation to a folder at depth > 0, breadcrumbs should be rendered with path segments matching the current path.
**Validates: Requirements 5.3**

### Property 17: Breadcrumb navigation
*For any* breadcrumb segment, clicking on it should navigate to the corresponding folder path.
**Validates: Requirements 5.4**

## Error Handling

### Filter Change Errors

1. **API Failure During Load**
   - Catch errors in `initializeFileExplorer()`
   - Display error toast with user-friendly message
   - Remove loading indicator
   - Keep File Explorer in previous valid state

2. **Invalid Filter Selection**
   - Validate that both academicYearId and semesterId are present
   - Show context message if either is missing
   - Hide File Explorer container

3. **State Reset Failure**
   - Wrap `resetData()` in try-catch
   - Log error but continue with load operation
   - Ensure UI is cleared even if state reset fails

### UI Rendering Errors

1. **Missing DOM Elements**
   - Check for container existence before rendering
   - Log warning if expected elements are missing
   - Gracefully degrade functionality

2. **Data Format Errors**
   - Validate data structure before rendering
   - Show empty state if data is malformed
   - Log detailed error for debugging

## Testing Strategy

### Unit Tests

1. **FileExplorerPage.initializeFileExplorer()**
   - Test that resetData() is called before loadRoot()
   - Test that FileExplorer instance is reused
   - Test that context is updated correctly
   - Test error handling for API failures

2. **FileExplorer.render()**
   - Test that tree view is hidden when hideTree: true
   - Test that single-column layout is used when hideTree: true
   - Test that breadcrumbs are rendered correctly

3. **Filter Change Handler**
   - Test that handleContextChange() is called on filter change
   - Test that loading indicator is shown/hidden correctly
   - Test that context message is shown when filters are cleared

### Property-Based Tests

Property-based tests will be implemented using a JavaScript PBT library (e.g., fast-check). Each test will run a minimum of 100 iterations with randomly generated inputs.

1. **Property 1: Filter change clears UI**
   - Generate random folder/file data
   - Render File Explorer
   - Trigger filter change
   - Verify DOM is cleared

2. **Property 4: Navigation state reset**
   - Generate random navigation paths
   - Navigate to random depth
   - Trigger filter change
   - Verify path is reset to root

3. **Property 8: Instance preservation**
   - Store FileExplorer instance reference
   - Trigger multiple filter changes
   - Verify instance reference unchanged

4. **Property 10: Reset before load sequence**
   - Mock resetData() and loadRoot()
   - Trigger filter change
   - Verify resetData() called before loadRoot()

5. **Property 15: Folder navigation**
   - Generate random folder structures
   - Click on random folders
   - Verify navigation method called with correct path

### Integration Tests

1. **End-to-End Filter Change Flow**
   - Select Academic Year
   - Select Semester
   - Verify File Explorer loads correct data
   - Change Semester
   - Verify File Explorer updates with new data

2. **Tree View Visibility**
   - Initialize File Explorer with hideTree: true
   - Verify tree panel is not in DOM
   - Verify single-column layout is used

3. **State Persistence**
   - Select filters
   - Refresh page
   - Verify filters are restored from localStorage
   - Verify File Explorer loads with restored context

## Implementation Plan

### Phase 1: Fix Filter Reactivity (Priority: High)

1. Import `fileExplorerState` in `file-explorer-page.js`
2. Add `resetData()` call before `loadRoot()` in `initializeFileExplorer()`
3. Add `setContext()` call to update state with new context
4. Test filter changes to verify UI updates correctly

### Phase 2: Remove Tree View (Priority: High)

1. Add `hideTree` option to FileExplorer constructor
2. Modify `render()` method to conditionally render tree panel
3. Update grid layout classes based on `hideTree` option
4. Modify `renderTree()` to skip rendering when `hideTree: true`
5. Update FileExplorerPage to pass `hideTree: true` for Dean role

### Phase 3: Testing and Validation (Priority: Medium)

1. Write unit tests for modified methods
2. Write property-based tests for key properties
3. Perform manual testing of filter changes
4. Verify layout matches Professor Dashboard
5. Test error scenarios

### Phase 4: Documentation and Cleanup (Priority: Low)

1. Update code comments
2. Document new configuration option
3. Add JSDoc for modified methods
4. Update README if necessary

## Migration Notes

### Breaking Changes

None. This is a bug fix and UI improvement that doesn't change any public APIs.

### Backward Compatibility

- Existing FileExplorer instances without `hideTree` option will continue to show tree view (default behavior)
- Professor and HOD dashboards are unaffected
- Only Dean Dashboard will use the new `hideTree: true` option

### Deployment Considerations

1. No database migrations required
2. No API changes required
3. Frontend-only changes
4. Can be deployed independently
5. No cache clearing required (JavaScript modules will be reloaded)

## Performance Considerations

### Improvements

1. **Reduced DOM Complexity**: Removing tree view reduces number of DOM elements by ~30-40%
2. **Faster Rendering**: Single-column layout renders faster than two-column layout
3. **Better State Management**: Proper state reset prevents memory leaks from stale data

### Potential Issues

None identified. The changes should improve performance overall.

## Security Considerations

No security implications. The changes are purely UI/UX improvements and don't affect:
- Authentication
- Authorization
- Data access controls
- API endpoints
- Role-based permissions

All existing security measures remain in place.
