# File Explorer Developer Guide

## Overview

The File Explorer is a reusable JavaScript component that provides hierarchical file navigation across all dashboards in the Al-Quds University Document Archiving System. This guide explains how to use, configure, and extend the File Explorer component.

## Table of Contents

1. [Architecture](#architecture)
2. [Getting Started](#getting-started)
3. [Configuration Options](#configuration-options)
4. [Role-Specific Behavior](#role-specific-behavior)
5. [API Integration](#api-integration)
6. [Customization](#customization)
7. [Master Design Reference](#master-design-reference)
8. [Troubleshooting](#troubleshooting)

---

## Architecture

### Component Structure

```
FileExplorer Class (file-explorer.js)
├── Constructor - Initialize with role-specific configuration
├── Rendering Methods
│   ├── render() - Main UI structure
│   ├── renderBreadcrumbs() - Navigation breadcrumbs
│   ├── renderTree() - Left panel folder tree
│   ├── renderFileList() - Right panel file/folder list
│   ├── renderEmptyState() - Empty folder states
│   ├── renderLoadingState() - Skeleton loaders
│   └── renderErrorState() - Error messages
├── Navigation Methods
│   ├── loadRoot() - Load semester root
│   ├── loadNode() - Load specific folder
│   ├── loadBreadcrumbs() - Fetch breadcrumb trail
│   ├── handleNodeClick() - Tree node navigation
│   └── handleBreadcrumbClick() - Breadcrumb navigation
├── Tree Management
│   ├── toggleNode() - Expand/collapse with lazy loading
│   ├── findNodeByPath() - Search tree structure
│   └── expandPathInTree() - Expand parent nodes
├── File Operations
│   ├── handleFileView() - Show file details modal
│   └── handleFileDownload() - Download file
└── Utility Methods
    ├── generateRoleSpecificLabels() - Create role badges
    ├── formatFileSize() - Format bytes to KB/MB/GB
    ├── getFileIconClass() - Get icon color by file type
    └── escapeHtml() - Prevent XSS attacks
```

### Design Philosophy

The File Explorer follows these principles:

1. **Single Source of Truth**: Professor Dashboard File Explorer is the master design reference
2. **Role-Based Configuration**: Different behaviors through configuration, not code duplication
3. **Visual Consistency**: Identical HTML structure and Tailwind CSS classes across all dashboards
4. **Lazy Loading**: Folder contents loaded on-demand for performance
5. **Accessibility**: Semantic HTML, ARIA labels, keyboard navigation support

---

## Getting Started

### Basic Usage

```javascript
import FileExplorer from './file-explorer.js';

// 1. Create a container in your HTML
<div id="fileExplorerContainer"></div>

// 2. Initialize the File Explorer
const fileExplorer = new FileExplorer('fileExplorerContainer', {
    role: 'PROFESSOR',
    readOnly: false
});

// 3. Load data for a semester
await fileExplorer.loadRoot(academicYearId, semesterId);

// 4. Store instance globally for event handlers
window.fileExplorerInstance = fileExplorer;
```

### HTML Container Requirements

The File Explorer requires a container element with a unique ID:

```html
<div id="fileExplorerContainer" class="bg-white rounded-lg shadow-sm">
    <!-- File Explorer will render here -->
</div>
```

### Dependencies

The File Explorer requires these modules:

```javascript
import { fileExplorer } from './api.js';      // API methods
import { showToast, showModal, formatDate } from './ui.js';  // UI utilities
```

---

## Configuration Options

### Core Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `readOnly` | boolean | `false` | Disables upload/edit actions |
| `onFileClick` | Function | `null` | Callback when file is clicked |
| `onNodeExpand` | Function | `null` | Callback when tree node expands |

### Role-Specific Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `role` | string | `'PROFESSOR'` | User role: `'PROFESSOR'`, `'HOD'`, or `'DEANSHIP'` |
| `showOwnershipLabels` | boolean | `false` | Show "Your Folder" labels (Professor) |
| `showDepartmentContext` | boolean | `false` | Show department context (HOD) |
| `headerMessage` | string | `null` | Header message above breadcrumbs |
| `showProfessorLabels` | boolean | `false` | Show professor names on folders (Deanship) |
| `showAllDepartments` | boolean | `false` | Allow viewing all departments (Deanship) |

---

## Role-Specific Behavior

### Professor Dashboard

**Purpose**: Full read-write access to own courses

**Configuration**:
```javascript
const fileExplorer = new FileExplorer('fileExplorerContainer', {
    role: 'PROFESSOR',
    showOwnershipLabels: true,
    readOnly: false
});
```

**Features**:
- "Your Folder" badge on owned course folders (blue)
- "Read Only" badge on shared folders (gray)
- Upload and file management actions enabled
- Only sees own courses and assigned folders

**Visual Indicators**:
- 🔵 **Your Folder** - Folders the professor owns (can upload/edit)
- ⚪ **Read Only** - Folders the professor can view but not edit

---

### HOD Dashboard

**Purpose**: Read-only view of department submissions

**Configuration**:
```javascript
const fileExplorer = new FileExplorer('hodFileExplorer', {
    role: 'HOD',
    showDepartmentContext: true,
    headerMessage: 'Browse department files (Read-only)',
    readOnly: true
});
```

**Features**:
- Header message explaining read-only access
- "Read Only" badges on all folders
- No upload or edit actions
- Only sees professors and courses in their department

**Visual Indicators**:
- ℹ️ Header: "Browse department files (Read-only)"
- ⚪ **Read Only** - All folders are read-only

---

### Deanship Dashboard

**Purpose**: Read-only view of all departments and submissions

**Configuration**:
```javascript
const fileExplorer = new FileExplorer('deanshipFileExplorer', {
    role: 'DEANSHIP',
    showAllDepartments: true,
    showProfessorLabels: true,
    readOnly: true
});
```

**Features**:
- Professor name labels on professor folders (purple)
- Access to all departments
- No upload or edit actions
- Full system visibility

**Visual Indicators**:
- 🟣 **Professor Name** - Shows which professor owns each folder

---

## API Integration

### Required API Methods

The File Explorer expects these methods in `api.js`:

```javascript
// api.js
export const fileExplorer = {
    /**
     * Get root node for a semester
     * @param {number} academicYearId
     * @param {number} semesterId
     * @returns {Promise<Object>} Root node with children
     */
    async getRoot(academicYearId, semesterId) {
        // Implementation
    },

    /**
     * Get node by path
     * @param {string} path - Node path (e.g., "2024-2025/first/PBUS001")
     * @returns {Promise<Object>} Node with children
     */
    async getNode(path) {
        // Implementation
    },

    /**
     * Get breadcrumbs for path
     * @param {string} path
     * @returns {Promise<Array>} Breadcrumb array
     */
    async getBreadcrumbs(path) {
        // Implementation
    },

    /**
     * Get file metadata
     * @param {number} fileId
     * @returns {Promise<Object>} File metadata
     */
    async getFileMetadata(fileId) {
        // Implementation
    },

    /**
     * Download file
     * @param {number} fileId
     * @returns {Promise<Response>} File blob response
     */
    async downloadFile(fileId) {
        // Implementation
    }
};
```

### Node Data Structure

```javascript
{
    name: "Course Name",           // Display name
    path: "2024-2025/first/PBUS001", // Full path
    type: "COURSE",                // Node type
    canRead: true,                 // Read permission
    canWrite: false,               // Write permission
    children: [...],               // Child nodes
    metadata: {                    // Additional data
        description: "...",
        fileSize: 1024,
        fileType: "application/pdf",
        uploadedAt: "2024-11-20T10:00:00Z",
        uploaderName: "Prof. Name",
        originalFilename: "file.pdf",
        fileId: 123,
        professorName: "Prof. Name" // For Deanship view
    },
    entityId: 42                   // Database ID
}
```

### Node Types

- `YEAR` - Academic year (e.g., "2024-2025")
- `SEMESTER` - Semester (e.g., "first", "second")
- `PROFESSOR` - Professor folder
- `COURSE` - Course folder
- `DOCUMENT_TYPE` - Document type folder (e.g., "Syllabus", "Exams")
- `FILE` - Individual file

---

## Customization

### Adding Custom Labels

To add custom role-specific labels, modify `generateRoleSpecificLabels()`:

```javascript
generateRoleSpecificLabels(folder) {
    let labels = '';
    
    // Your custom role logic
    if (this.options.role === 'CUSTOM_ROLE') {
        if (folder.metadata && folder.metadata.customProperty) {
            labels += `
                <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-700 ml-2">
                    Custom Label
                </span>
            `;
        }
    }
    
    return labels;
}
```

### Custom Empty States

Override the empty state message:

```javascript
// In your dashboard initialization
fileExplorer.renderEmptyState = function(message, iconType) {
    return `
        <div class="text-center py-8">
            <p class="text-gray-500">${message}</p>
            <button class="mt-4 btn-primary">Custom Action</button>
        </div>
    `;
};
```

### Custom File Actions

Add custom actions to file rows by modifying `renderFileList()` or using callbacks:

```javascript
const fileExplorer = new FileExplorer('container', {
    role: 'PROFESSOR',
    onFileClick: (fileId, metadata) => {
        console.log('File clicked:', fileId, metadata);
        // Custom action
    }
});
```

---

## Master Design Reference

### Professor Dashboard as Authority

The Professor Dashboard File Explorer (`prof-dashboard.html` + `prof.js`) is the **canonical design reference**. All visual styling, HTML structure, and Tailwind classes are defined there.

### Key Design Elements

#### 1. Folder Cards

```html
<div class="flex items-center justify-between p-4 bg-blue-50 rounded-lg border border-blue-200 hover:bg-blue-100 cursor-pointer transition-all group">
    <div class="flex items-center space-x-3 flex-1">
        <svg class="w-7 h-7 text-blue-600"><!-- Folder icon --></svg>
        <div class="flex-1">
            <p class="text-sm font-semibold text-gray-900">Folder Name</p>
        </div>
    </div>
    <svg class="w-5 h-5 text-gray-400 group-hover:text-gray-700 group-hover:translate-x-1 transition-all">
        <!-- Arrow icon -->
    </svg>
</div>
```

**Tailwind Classes**:
- Container: `p-4 bg-blue-50 rounded-lg border border-blue-200 hover:bg-blue-100`
- Icon: `w-7 h-7 text-blue-600`
- Name: `text-sm font-semibold text-gray-900`
- Arrow: `w-5 h-5 group-hover:translate-x-1 transition-all`

#### 2. Role-Specific Labels

**Your Folder (Professor ownership)**:
```html
<span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-blue-100 text-blue-800 ml-2">
    <svg class="w-3 h-3 mr-1"><!-- Edit icon --></svg>
    Your Folder
</span>
```

**Read Only**:
```html
<span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-600 ml-2">
    <svg class="w-3 h-3 mr-1"><!-- Eye icon --></svg>
    Read Only
</span>
```

**Professor Name (Deanship)**:
```html
<span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-700 ml-2">
    <svg class="w-3 h-3 mr-1"><!-- User icon --></svg>
    Prof. Name
</span>
```

#### 3. File Table

```html
<table class="min-w-full divide-y divide-gray-200 border border-gray-200 rounded-lg">
    <thead class="bg-gray-50">
        <tr>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Size</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Uploaded</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Uploader</th>
            <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Actions</th>
        </tr>
    </thead>
    <tbody class="bg-white divide-y divide-gray-200">
        <!-- File rows -->
    </tbody>
</table>
```

#### 4. Breadcrumbs

```html
<nav class="flex items-center overflow-x-auto" aria-label="Breadcrumb">
    <ol class="inline-flex items-center space-x-1 whitespace-nowrap">
        <li class="inline-flex items-center">
            <svg class="w-4 h-4 text-gray-400 mr-2"><!-- Home icon --></svg>
            <a href="#" class="text-sm font-medium text-blue-600 hover:text-blue-800 hover:underline">
                Home
            </a>
        </li>
        <li class="inline-flex items-center">
            <svg class="w-5 h-5 text-gray-400 mx-1"><!-- Chevron --></svg>
            <span class="text-sm font-medium text-gray-700">Current</span>
        </li>
    </ol>
</nav>
```

### Color Scheme

| Element | Background | Border | Text | Hover |
|---------|-----------|--------|------|-------|
| Folder Card | `bg-blue-50` | `border-blue-200` | `text-gray-900` | `hover:bg-blue-100` |
| Ownership Label | `bg-blue-100` | - | `text-blue-800` | - |
| Read-Only Label | `bg-gray-100` | - | `text-gray-600` | - |
| Professor Label | `bg-purple-100` | - | `text-purple-700` | - |
| Metadata Badge | `bg-gray-100` | - | `text-gray-700` | - |
| Download Button | `bg-blue-600` | - | `text-white` | `hover:bg-blue-700` |

---

## Troubleshooting

### Common Issues

#### 1. Container Not Found Error

**Error**: `Container element with id "..." not found`

**Solution**: Ensure the container element exists in the DOM before initializing:

```javascript
document.addEventListener('DOMContentLoaded', () => {
    const fileExplorer = new FileExplorer('fileExplorerContainer', options);
});
```

#### 2. Breadcrumb Click Not Working

**Problem**: Clicking breadcrumbs doesn't navigate

**Solution**: Store the FileExplorer instance globally:

```javascript
window.fileExplorerInstance = fileExplorer;
```

The breadcrumb HTML uses `window.fileExplorerInstance.handleBreadcrumbClick()`.

#### 3. Role Labels Not Showing

**Problem**: "Your Folder" or professor labels don't appear

**Solution**: Check configuration options:

```javascript
// For Professor ownership labels
showOwnershipLabels: true

// For Deanship professor labels
showProfessorLabels: true
```

Also verify that the node has the correct permissions:
- `canWrite: true` for "Your Folder"
- `metadata.professorName` exists for professor labels

#### 4. Files Not Downloading

**Problem**: Download button doesn't work or shows errors

**Solution**: 
1. Check that `fileId` is present in `metadata.fileId`
2. Verify API endpoint returns proper Content-Disposition header
3. Check browser console for CORS or permission errors

#### 5. Skeleton Loaders Not Animating

**Problem**: Loading states show but don't animate

**Solution**: Ensure `custom.css` includes skeleton animation:

```css
.skeleton-line {
    background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s ease-in-out infinite;
}

@keyframes skeleton-loading {
    0% { background-position: 200% 0; }
    100% { background-position: -200% 0; }
}
```

### Debugging Tips

#### Enable Verbose Logging

```javascript
// Add to constructor
this.debug = true;

// Add to methods
if (this.debug) {
    console.log('Loading node:', path);
}
```

#### Inspect Node Structure

```javascript
// In browser console
console.log('Current node:', window.fileExplorerInstance.currentNode);
console.log('Breadcrumbs:', window.fileExplorerInstance.breadcrumbs);
console.log('Expanded nodes:', window.fileExplorerInstance.expandedNodes);
```

#### Test API Responses

```javascript
// Test API methods directly
import { fileExplorer } from './api.js';

const root = await fileExplorer.getRoot(1, 1);
console.log('Root node:', root);

const node = await fileExplorer.getNode('2024-2025/first');
console.log('Node:', node);
```

---

## Best Practices

### 1. Always Store Instance Globally

```javascript
window.fileExplorerInstance = new FileExplorer('container', options);
```

This allows event handlers in HTML to access the instance.

### 2. Handle Errors Gracefully

```javascript
try {
    await fileExplorer.loadRoot(yearId, semesterId);
} catch (error) {
    console.error('Failed to load file explorer:', error);
    showToast('Failed to load files. Please try again.', 'error');
}
```

### 3. Clean Up on Page Unload

```javascript
window.addEventListener('beforeunload', () => {
    if (window.fileExplorerInstance) {
        window.fileExplorerInstance = null;
    }
});
```

### 4. Use Semantic HTML

The File Explorer uses semantic HTML for accessibility:
- `<nav>` for breadcrumbs
- `<table>` for file lists
- `<button>` for actions
- ARIA labels for screen readers

### 5. Test Across Roles

Always test File Explorer behavior with all three roles:
- Professor (read-write)
- HOD (read-only, department-scoped)
- Deanship (read-only, all departments)

---

## Examples

### Complete Professor Dashboard Integration

```javascript
// prof.js
import FileExplorer from './file-explorer.js';

let fileExplorer = null;

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    // Create File Explorer instance
    fileExplorer = new FileExplorer('fileExplorerContainer', {
        role: 'PROFESSOR',
        showOwnershipLabels: true,
        readOnly: false
    });
    
    // Store globally for event handlers
    window.fileExplorerInstance = fileExplorer;
    
    // Load academic years and semesters
    loadAcademicYears();
});

// Handle semester selection
async function handleSemesterChange(academicYearId, semesterId) {
    if (!academicYearId || !semesterId) return;
    
    try {
        await fileExplorer.loadRoot(academicYearId, semesterId);
    } catch (error) {
        console.error('Error loading file explorer:', error);
        showToast('Failed to load files', 'error');
    }
}
```

### Complete HOD Dashboard Integration

```javascript
// hod.js
import FileExplorer from './file-explorer.js';

let fileExplorer = null;

document.addEventListener('DOMContentLoaded', () => {
    fileExplorer = new FileExplorer('hodFileExplorer', {
        role: 'HOD',
        showDepartmentContext: true,
        headerMessage: 'Browse department files (Read-only)',
        readOnly: true
    });
    
    window.fileExplorerInstance = fileExplorer;
    
    loadAcademicYears();
});

async function handleSemesterChange(academicYearId, semesterId) {
    if (!academicYearId || !semesterId) return;
    
    try {
        await fileExplorer.loadRoot(academicYearId, semesterId);
    } catch (error) {
        console.error('Error loading file explorer:', error);
        showToast('Failed to load files', 'error');
    }
}
```

### Complete Deanship Dashboard Integration

```javascript
// deanship.js
import FileExplorer from './file-explorer.js';

let fileExplorer = null;

document.addEventListener('DOMContentLoaded', () => {
    fileExplorer = new FileExplorer('deanshipFileExplorer', {
        role: 'DEANSHIP',
        showAllDepartments: true,
        showProfessorLabels: true,
        readOnly: true
    });
    
    window.fileExplorerInstance = fileExplorer;
    
    loadAcademicYears();
});

async function handleSemesterChange(academicYearId, semesterId) {
    if (!academicYearId || !semesterId) return;
    
    try {
        await fileExplorer.loadRoot(academicYearId, semesterId);
    } catch (error) {
        console.error('Error loading file explorer:', error);
        showToast('Failed to load files', 'error');
    }
}
```

---

## Maintenance

### When to Update the Master Design

If you need to change the File Explorer visual design:

1. **Update Professor Dashboard first** (`prof-dashboard.html` + `prof.js`)
2. Test thoroughly in Professor Dashboard
3. Update `file-explorer.js` to match the new design
4. Test in all three dashboards (Professor, HOD, Deanship)
5. Update this documentation

### Version History

- **v1.0** - Initial unified File Explorer implementation
- **v1.1** - Added role-specific labels and configuration options
- **v1.2** - Added comprehensive JSDoc documentation

---

## Support

For questions or issues:

1. Check this documentation
2. Review the Professor Dashboard implementation (master reference)
3. Check browser console for errors
4. Review API responses in Network tab
5. Contact the development team

---

## License

Internal use only - Al-Quds University Document Archiving System
