# Design Document

## Overview

This design document outlines the technical approach for unifying the File Explorer UI across all three dashboards (Deanship, HOD, and Professor) in the Al-Quds University Document Archiving System. The design leverages the existing `FileExplorer` class from `file-explorer.js` as the foundation and extends it to support role-specific behaviors while maintaining a consistent visual appearance based on the Professor Dashboard's File Explorer design.

### Design Goals

1. **Single Source of Truth**: Use the Professor Dashboard File Explorer as the canonical design reference
2. **Component Reusability**: Leverage the existing `FileExplorer` class to eliminate code duplication
3. **Role-Based Configuration**: Support different permissions and behaviors through configuration options
4. **Visual Consistency**: Ensure identical HTML structure and Tailwind CSS classes across all dashboards
5. **Backward Compatibility**: Maintain all existing API endpoints, routing, and permission logic
6. **Minimal Code Changes**: Refactor existing implementations to use the shared component with minimal disruption

## Architecture

### High-Level Component Structure

```
┌─────────────────────────────────────────────────────────────┐
│                    Dashboard Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Professor   │  │     HOD      │  │   Deanship   │      │
│  │  Dashboard   │  │  Dashboard   │  │  Dashboard   │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│         └──────────────────┼──────────────────┘              │
│                            │                                 │
└────────────────────────────┼─────────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────────┐
│                  Shared Component Layer                      │
│                            │                                 │
│                   ┌────────▼────────┐                        │
│                   │  FileExplorer   │                        │
│                   │     Class       │                        │
│                   │ (file-explorer  │                        │
│                   │      .js)       │                        │
│                   └────────┬────────┘                        │
│                            │                                 │
└────────────────────────────┼─────────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────────┐
│                      API Layer                               │
│                            │                                 │
│                   ┌────────▼────────┐                        │
│                   │   fileExplorer  │                        │
│                   │   API Module    │                        │
│                   │    (api.js)     │                        │
│                   └────────┬────────┘                        │
│                            │                                 │
└────────────────────────────┼─────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │  Backend APIs   │
                    │  (Spring Boot)  │
                    └─────────────────┘
```

### Component Responsibilities

#### FileExplorer Class (file-explorer.js)
- **Purpose**: Reusable component for hierarchical file navigation
- **Responsibilities**:
  - Render consistent HTML structure and Tailwind CSS classes
  - Manage breadcrumb navigation
  - Handle tree view and file list rendering
  - Support lazy loading of folder contents
  - Provide file download and view functionality
  - Accept configuration options for role-specific behavior

#### Dashboard-Specific JavaScript (prof.js, hod.js, deanship.js)
- **Purpose**: Initialize and configure the FileExplorer component
- **Responsibilities**:
  - Instantiate FileExplorer with role-specific options
  - Handle academic year and semester selection
  - Manage tab switching
  - Provide role-specific event handlers

## Components and Interfaces

### FileExplorer Class Interface

```javascript
class FileExplorer {
    /**
     * Constructor
     * @param {string} containerId - ID of the DOM element to render into
     * @param {Object} options - Configuration options
     * @param {boolean} options.readOnly - Whether the explorer is read-only
     * @param {string} options.role - User role ('PROFESSOR', 'HOD', 'DEANSHIP')
     * @param {Function} options.onFileClick - Callback for file clicks
     * @param {Function} options.onNodeExpand - Callback for node expansion
     */
    constructor(containerId, options = {})
    
    /**
     * Load root node for a semester
     * @param {number} academicYearId - Academic year ID
     * @param {number} semesterId - Semester ID
     */
    async loadRoot(academicYearId, semesterId)
    
    /**
     * Load a specific node by path
     * @param {string} path - Node path
     */
    async loadNode(path)
    
    /**
     * Render the file explorer UI
     */
    render()
    
    /**
     * Render breadcrumbs
     */
    renderBreadcrumbs()
    
    /**
     * Render tree view
     * @param {Object} node - Root node data
     */
    renderTree(node)
    
    /**
     * Render file list
     * @param {Object} node - Current node data
     */
    renderFileList(node)
}
```

### Configuration Options by Role

#### Professor Dashboard Configuration
```javascript
const fileExplorer = new FileExplorer('fileExplorerContainer', {
    readOnly: false,
    role: 'PROFESSOR',
    showOwnershipLabels: true,
    enableUpload: true
});
```

#### HOD Dashboard Configuration
```javascript
const fileExplorer = new FileExplorer('hodFileExplorer', {
    readOnly: true,
    role: 'HOD',
    showDepartmentContext: true,
    headerMessage: 'Browse department files (Read-only)'
});
```

#### Deanship Dashboard Configuration
```javascript
const fileExplorer = new FileExplorer('deanshipFileExplorer', {
    readOnly: true,
    role: 'DEANSHIP',
    showAllDepartments: true,
    showProfessorLabels: true
});
```

## Data Models

### Node Structure
```javascript
{
    name: string,           // Display name
    path: string,           // Full path for navigation
    type: string,           // 'YEAR', 'SEMESTER', 'PROFESSOR', 'COURSE', 'DOCUMENT_TYPE', 'FILE'
    canRead: boolean,       // Read permission
    canWrite: boolean,      // Write permission
    children: Array<Node>,  // Child nodes
    metadata: {             // Additional metadata
        description: string,
        fileSize: number,
        fileType: string,
        uploadedAt: string,
        uploaderName: string,
        originalFilename: string,
        fileId: number,
        ...
    },
    entityId: number        // Database entity ID
}
```

### Breadcrumb Structure
```javascript
{
    name: string,   // Display name
    path: string    // Path for navigation
}
```

## Master Design Reference: Professor Dashboard File Explorer

### HTML Structure

#### Container Layout
```html
<div class="file-explorer">
    <!-- Breadcrumbs -->
    <div id="fileExplorerBreadcrumbs" class="bg-gray-50 px-4 py-3 border-b border-gray-200">
        <nav class="flex items-center overflow-x-auto" aria-label="Breadcrumb">
            <ol class="inline-flex items-center space-x-1 whitespace-nowrap">
                <!-- Breadcrumb items -->
            </ol>
        </nav>
    </div>

    <!-- File Explorer Content -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 p-4">
        <!-- Tree View (Left Panel) -->
        <div class="md:col-span-1 bg-white border border-gray-200 rounded-lg p-4">
            <h3 class="text-sm font-semibold text-gray-700 mb-3">Folder Structure</h3>
            <div id="fileExplorerTree" class="space-y-1">
                <!-- Tree nodes -->
            </div>
        </div>

        <!-- File List (Right Panel) -->
        <div class="md:col-span-2 bg-white border border-gray-200 rounded-lg p-4">
            <h3 class="text-sm font-semibold text-gray-700 mb-3">Files</h3>
            <div id="fileExplorerFileList" class="overflow-x-auto">
                <!-- File list content -->
            </div>
        </div>
    </div>
</div>
```

#### Breadcrumb Item
```html
<li class="inline-flex items-center">
    <!-- Separator (not for first item) -->
    <svg class="w-5 h-5 text-gray-400 mx-1" fill="currentColor" viewBox="0 0 20 20">
        <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"></path>
    </svg>
    
    <!-- Home icon (first item only) -->
    <svg class="w-4 h-4 text-gray-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
        <path d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z"></path>
    </svg>
    
    <!-- Link or current text -->
    <a href="#" class="text-sm font-medium text-blue-600 hover:text-blue-800 hover:underline transition-colors">
        Breadcrumb Name
    </a>
    <!-- OR for current location -->
    <span class="text-sm font-medium text-gray-700">Current Location</span>
</li>
```

#### Folder Card
```html
<div class="flex items-center justify-between p-4 bg-blue-50 rounded-lg border border-blue-200 hover:bg-blue-100 cursor-pointer transition-all group">
    <div class="flex items-center space-x-3 flex-1">
        <!-- Folder Icon -->
        <svg class="w-7 h-7 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
        </svg>
        
        <div class="flex-1">
            <div class="flex items-center flex-wrap">
                <p class="text-sm font-semibold text-gray-900">Folder Name</p>
                
                <!-- Role-specific label (Professor: "Your Folder") -->
                <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-blue-100 text-blue-800 ml-2">
                    <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path>
                    </svg>
                    Your Folder
                </span>
                
                <!-- OR for read-only -->
                <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-600 ml-2">
                    <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                    </svg>
                    Read Only
                </span>
            </div>
            
            <!-- Optional description -->
            <p class="text-xs text-gray-500 mt-1">Optional description text</p>
        </div>
    </div>
    
    <!-- Arrow icon -->
    <svg class="w-5 h-5 text-gray-400 group-hover:text-gray-700 group-hover:translate-x-1 transition-all" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
    </svg>
</div>
```

#### File Table Row
```html
<tr class="hover:bg-gray-50 transition-all group">
    <td class="px-4 py-3 whitespace-nowrap">
        <div class="flex items-center">
            <!-- File Icon -->
            <div class="w-8 h-8 flex items-center justify-center bg-gray-50 rounded mr-3 flex-shrink-0">
                <svg class="w-5 h-5 text-red-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"></path>
                </svg>
            </div>
            <span class="text-sm font-medium text-gray-900 group-hover:text-blue-600 transition-colors">filename.pdf</span>
        </div>
    </td>
    <td class="px-4 py-3 whitespace-nowrap">
        <span class="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-700">
            2.5 MB
        </span>
    </td>
    <td class="px-4 py-3 whitespace-nowrap">
        <span class="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-700">
            Nov 15, 2024
        </span>
    </td>
    <td class="px-4 py-3 whitespace-nowrap">
        <span class="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-700">
            Prof. Name
        </span>
    </td>
    <td class="px-4 py-3 whitespace-nowrap text-right text-sm font-medium">
        <div class="flex items-center justify-end space-x-2">
            <!-- View button -->
            <button class="text-gray-600 hover:text-gray-900 p-1.5 rounded hover:bg-gray-100 transition-all" title="View file details">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                </svg>
            </button>
            
            <!-- Download button -->
            <button class="text-white bg-blue-600 hover:bg-blue-700 p-1.5 rounded shadow-sm hover:shadow-md transition-all" title="Download file">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                </svg>
            </button>
        </div>
    </td>
</tr>
```

### Tailwind CSS Classes Reference

#### Color Scheme
- **Primary Blue**: `bg-blue-50`, `border-blue-200`, `text-blue-600`, `hover:bg-blue-100`
- **Gray Neutrals**: `bg-gray-50`, `border-gray-200`, `text-gray-600`, `text-gray-700`, `text-gray-900`
- **Success Green**: `bg-green-50`, `text-green-600`
- **Warning Yellow**: `bg-yellow-50`, `text-yellow-600`
- **Danger Red**: `bg-red-50`, `text-red-600`

#### Spacing
- **Container Padding**: `p-4`, `p-6`
- **Card Padding**: `p-3`, `p-4`
- **Gap Between Items**: `space-y-2`, `space-y-3`, `gap-4`
- **Icon Margins**: `mr-2`, `mr-3`, `ml-2`

#### Typography
- **Headers**: `text-xl font-semibold`, `text-sm font-semibold`
- **Body Text**: `text-sm font-medium`, `text-sm text-gray-900`
- **Metadata**: `text-xs text-gray-500`, `text-xs font-medium`

#### Borders and Shadows
- **Card Borders**: `border border-gray-200`, `rounded-lg`
- **Hover Shadows**: `hover:shadow-lg`
- **Button Shadows**: `shadow-sm hover:shadow-md`

#### Transitions
- **Standard**: `transition-all`, `transition-colors`
- **Transform**: `group-hover:translate-x-1`, `group-hover:scale-110`

## Error Handling

### Empty States
```html
<div class="text-sm text-gray-500 py-8 text-center">
    <svg class="mx-auto h-12 w-12 text-gray-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
    </svg>
    <p>This folder is empty</p>
</div>
```

### Loading States
```html
<div class="space-y-3">
    <div class="flex items-center justify-between p-3 bg-gray-50 rounded-lg border border-gray-200">
        <div class="flex items-center space-x-3 flex-1">
            <div class="skeleton-line skeleton-circle h-6" style="width: 1.5rem;"></div>
            <div class="flex-1">
                <div class="skeleton-line h-4 w-1-2"></div>
            </div>
        </div>
        <div class="skeleton-line h-4" style="width: 1.25rem;"></div>
    </div>
</div>
```

### Error States
```html
<div class="text-center py-8">
    <svg class="mx-auto h-12 w-12 text-red-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
    </svg>
    <p class="text-red-600 text-sm font-medium">Failed to load file explorer</p>
    <p class="text-gray-500 text-xs mt-2">Please try again or select a different semester</p>
</div>
```

## Testing Strategy

### Unit Testing
- Test FileExplorer class methods in isolation
- Mock API responses for different scenarios
- Verify correct HTML structure generation
- Test role-specific configuration options

### Integration Testing
- Test FileExplorer integration with each dashboard
- Verify breadcrumb navigation works correctly
- Test file download functionality
- Verify permission checks are enforced

### Visual Regression Testing
- Compare rendered File Explorer across all three dashboards
- Verify identical visual appearance (colors, spacing, typography)
- Test responsive behavior on different screen sizes
- Verify hover states and transitions

### User Acceptance Testing
- Professor: Verify "Your Folder" labels appear correctly
- HOD: Verify read-only access and department filtering
- Deanship: Verify access to all departments and professors
- All roles: Verify consistent navigation and file operations

## Implementation Phases

### Phase 1: FileExplorer Class Enhancement
1. Review existing FileExplorer class in file-explorer.js
2. Add role-specific configuration options
3. Implement role-specific label rendering
4. Add header message support for HOD dashboard
5. Document configuration options

### Phase 2: Professor Dashboard Integration
1. Verify Professor Dashboard already uses FileExplorer class
2. Update configuration to explicitly set role options
3. Test all existing functionality
4. Document as the master reference implementation

### Phase 3: HOD Dashboard Migration
1. Remove custom File Explorer implementation from hod.js
2. Instantiate FileExplorer class with HOD configuration
3. Update HTML container structure to match Professor Dashboard
4. Test read-only access and department filtering
5. Verify "Browse department files (Read-only)" message displays

### Phase 4: Deanship Dashboard Migration
1. Identify current File Explorer implementation in deanship.js
2. Remove custom implementation
3. Instantiate FileExplorer class with Deanship configuration
4. Update HTML container structure to match Professor Dashboard
5. Test access to all departments and professors
6. Verify professor labels display correctly

### Phase 5: Testing and Refinement
1. Conduct visual regression testing
2. Perform cross-browser testing
3. Test responsive behavior
4. Gather user feedback
5. Make refinements as needed

### Phase 6: Documentation and Deployment
1. Update code comments
2. Create developer documentation
3. Update user guides if needed
4. Deploy to production
5. Monitor for issues

## Migration Strategy

### Backward Compatibility Approach
- Keep existing API endpoints unchanged
- Maintain existing permission logic
- Preserve all event handlers
- No database schema changes required

### Rollback Plan
- Keep backup of original dashboard files
- Use feature flags to enable/disable unified File Explorer
- Monitor error logs for issues
- Quick rollback capability if critical issues arise

## Performance Considerations

### Lazy Loading
- Load folder contents only when expanded
- Cache loaded nodes to avoid redundant API calls
- Implement pagination for large file lists

### Optimization
- Minimize DOM manipulations
- Use event delegation for click handlers
- Debounce search and filter operations
- Optimize CSS for smooth transitions

## Security Considerations

### Permission Enforcement
- All permission checks remain on backend
- Frontend only displays UI based on permissions
- No sensitive data exposed in client-side code
- File downloads go through authenticated endpoints

### XSS Prevention
- Escape all user-generated content
- Use `textContent` instead of `innerHTML` where possible
- Sanitize file names and metadata
- Validate all input data

## Accessibility

### ARIA Labels
- Add `aria-label` to navigation elements
- Use semantic HTML elements
- Provide keyboard navigation support
- Ensure sufficient color contrast

### Screen Reader Support
- Add descriptive labels to buttons
- Provide alternative text for icons
- Announce state changes
- Support keyboard-only navigation

## Browser Compatibility

### Supported Browsers
- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

### Polyfills
- None required (using modern JavaScript features supported by target browsers)

## Conclusion

This design provides a comprehensive approach to unifying the File Explorer UI across all dashboards while maintaining role-specific functionality. By leveraging the existing FileExplorer class and the Professor Dashboard as the master design reference, we can achieve visual consistency with minimal code changes and maximum reusability.
