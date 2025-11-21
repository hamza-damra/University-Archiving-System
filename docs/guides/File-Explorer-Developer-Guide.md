# File Explorer Developer Guide

## Overview

This guide provides comprehensive documentation for developers working with the File Explorer feature, including state management, dashboard integration, and folder auto-provisioning.

**Target Audience**: Frontend and backend developers  
**Prerequisites**: Basic knowledge of JavaScript, Spring Boot, and the Archive System architecture

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [FileExplorerState Module](#fileexplorerstate-module)
3. [Integrating File Explorer in Dashboards](#integrating-file-explorer-in-dashboards)
4. [Folder Path Conventions](#folder-path-conventions)
5. [Standard Subfolder Structure](#standard-subfolder-structure)
6. [Auto-Provisioning Integration](#auto-provisioning-integration)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

### Component Structure

```
Frontend:
├── file-explorer-state.js      (Centralized state management)
├── file-explorer.js             (File Explorer component)
├── deanship.js                  (Deanship dashboard)
├── prof.js                      (Professor dashboard)
└── hod.js                       (HOD dashboard)

Backend:
├── FolderService                (Folder management service)
├── FolderServiceImpl            (Implementation)
├── FileExplorerController       (REST API endpoints)
└── FolderRepository             (Database access)
```

### Data Flow

```
User Action → Dashboard → FileExplorerState → FileExplorer Component → API → Backend → Database/FileSystem
                ↓                                      ↑
                └──────── State Change Notification ──┘
```

---

## FileExplorerState Module

### Purpose

`FileExplorerState` is a centralized state management module that maintains the File Explorer's state across all dashboards. It uses the observer pattern to notify components of state changes.

### Location

**File**: `src/main/resources/static/js/file-explorer-state.js`

### Key Features

- **Singleton Pattern**: Single source of truth for File Explorer state
- **Observer Pattern**: Components subscribe to state changes
- **Immutable State**: State is read-only; changes only through setter methods
- **Context Management**: Tracks academic year and semester selection
- **Loading States**: Separate loading states for tree and file list
- **Error Handling**: Centralized error state management

---

### State Structure

```javascript
{
  // Context
  academicYearId: null,
  semesterId: null,
  yearCode: null,
  semesterType: null,
  
  // Tree State
  treeRoot: null,              // Root node of folder tree
  expandedNodes: Set(),        // Set of expanded folder IDs
  
  // Navigation State
  currentNode: null,           // Currently selected folder
  currentPath: '',             // Current folder path
  breadcrumbs: [],             // Breadcrumb navigation array
  
  // Loading States
  isLoading: false,            // Overall loading state
  isTreeLoading: false,        // Tree-specific loading
  isFileListLoading: false,    // File list-specific loading
  
  // Error State
  error: null,                 // Error message if any
  
  // Metadata
  lastUpdated: null            // Timestamp of last update
}
```

---

### Usage Examples

#### Importing the Module

```javascript
import { fileExplorerState } from './file-explorer-state.js';
```

#### Setting Context

```javascript
// When user selects academic year and semester
fileExplorerState.setContext({
  academicYearId: 1,
  semesterId: 2,
  yearCode: '2024-2025',
  semesterType: 'Fall'
});
```

#### Getting Current State

```javascript
const state = fileExplorerState.getState();
console.log('Current path:', state.currentPath);
console.log('Is loading:', state.isLoading);
```

#### Subscribing to State Changes

```javascript
// Subscribe to state changes
const unsubscribe = fileExplorerState.subscribe((state) => {
  console.log('State changed:', state);
  // Update UI based on new state
  updateUI(state);
});

// Later, unsubscribe when component is destroyed
unsubscribe();
```

#### Setting Tree Root

```javascript
// After fetching folder tree from API
fileExplorerState.setTreeRoot(treeData);
```

#### Setting Current Node

```javascript
// When user navigates to a folder
fileExplorerState.setCurrentNode(folderNode);
```

#### Managing Loading States

```javascript
// Start loading
fileExplorerState.setLoading(true);

// Fetch data...

// Stop loading
fileExplorerState.setLoading(false);
```

#### Handling Errors

```javascript
try {
  // Fetch data...
} catch (error) {
  fileExplorerState.setError('Failed to load folders');
}

// Clear error
fileExplorerState.clearError();
```

#### Toggling Node Expansion

```javascript
// Expand or collapse a folder in the tree
fileExplorerState.toggleNodeExpansion(folderId);
```

#### Resetting State

```javascript
// Reset all state (e.g., on logout)
fileExplorerState.reset();
```

---

### API Reference

#### Getter Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getState()` | `Object` | Returns immutable copy of entire state |
| `getContext()` | `Object` | Returns academic year/semester context |

#### Setter Methods

| Method | Parameters | Description |
|--------|------------|-------------|
| `setContext(context)` | `{academicYearId, semesterId, yearCode, semesterType}` | Sets academic context |
| `setTreeRoot(root)` | `Object` | Sets folder tree root |
| `setCurrentNode(node)` | `Object` | Sets currently selected folder |
| `setBreadcrumbs(breadcrumbs)` | `Array` | Sets breadcrumb navigation |
| `toggleNodeExpansion(nodeId)` | `Number` | Toggles folder expansion state |
| `setLoading(isLoading)` | `Boolean` | Sets overall loading state |
| `setTreeLoading(isLoading)` | `Boolean` | Sets tree loading state |
| `setFileListLoading(isLoading)` | `Boolean` | Sets file list loading state |
| `setError(error)` | `String` | Sets error message |
| `clearError()` | - | Clears error state |
| `reset()` | - | Resets all state to initial values |

#### Observer Methods

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `subscribe(listener)` | `Function` | `Function` | Subscribes to state changes, returns unsubscribe function |

---

## Integrating File Explorer in Dashboards

### Step-by-Step Integration

#### 1. Import Required Modules

```javascript
import { FileExplorer } from './file-explorer.js';
import { fileExplorerState } from './file-explorer-state.js';
```

#### 2. Initialize File Explorer Component

```javascript
class DashboardManager {
  constructor() {
    this.fileExplorerInstance = null;
    this.initializeFileExplorer();
  }
  
  initializeFileExplorer() {
    this.fileExplorerInstance = new FileExplorer(
      'file-explorer-tree',      // Tree container ID
      'file-explorer-file-list', // File list container ID
      'file-explorer-breadcrumb' // Breadcrumb container ID
    );
  }
}
```

#### 3. Handle Context Changes

```javascript
onContextChange(academicYearId, semesterId, yearCode, semesterType) {
  // Update FileExplorerState
  fileExplorerState.setContext({
    academicYearId,
    semesterId,
    yearCode,
    semesterType
  });
  
  // Reload File Explorer if on file-explorer tab
  if (this.currentTab === 'file-explorer') {
    this.fileExplorerInstance.loadRoot();
  }
}
```

#### 4. Handle Tab Switching

```javascript
switchTab(tabName) {
  this.currentTab = tabName;
  
  if (tabName === 'file-explorer') {
    // Load File Explorer if context is set
    const context = fileExplorerState.getContext();
    if (context.academicYearId && context.semesterId) {
      this.fileExplorerInstance.loadRoot();
    } else {
      // Show message to select context
      this.showMessage('Please select academic year and semester');
    }
  }
}
```

#### 5. Refresh After Auto-Provisioning

```javascript
async handleCreateProfessor(professorData) {
  try {
    // Create professor
    const response = await api.post('/api/deanship/professors', professorData);
    
    // Refresh File Explorer if on file-explorer tab
    if (this.currentTab === 'file-explorer') {
      const context = fileExplorerState.getContext();
      if (context.academicYearId && context.semesterId) {
        // Background refresh (no loading spinner)
        await this.fileExplorerInstance.loadRoot(true);
        this.showToast('Professor folder created');
      }
    }
    
    return response;
  } catch (error) {
    console.error('Failed to create professor:', error);
    throw error;
  }
}
```

#### 6. Handle Course Assignment

```javascript
async handleAssignCourse(assignmentData) {
  try {
    // Assign course
    const response = await api.post('/api/deanship/course-assignments', assignmentData);
    
    // Refresh File Explorer if on file-explorer tab
    if (this.currentTab === 'file-explorer') {
      const context = fileExplorerState.getContext();
      if (context.academicYearId && context.semesterId) {
        // Background refresh
        await this.fileExplorerInstance.loadRoot(true);
        this.showToast('Course folders created');
      }
    }
    
    return response;
  } catch (error) {
    console.error('Failed to assign course:', error);
    throw error;
  }
}
```

---

### Complete Integration Example

```javascript
// deanship.js
import { FileExplorer } from './file-explorer.js';
import { fileExplorerState } from './file-explorer-state.js';

class DeanshipDashboard {
  constructor() {
    this.currentTab = 'professors';
    this.fileExplorerInstance = null;
    this.init();
  }
  
  init() {
    this.initializeFileExplorer();
    this.setupEventListeners();
  }
  
  initializeFileExplorer() {
    this.fileExplorerInstance = new FileExplorer(
      'file-explorer-tree',
      'file-explorer-file-list',
      'file-explorer-breadcrumb'
    );
  }
  
  setupEventListeners() {
    // Academic year/semester selection
    document.getElementById('academic-year-select').addEventListener('change', (e) => {
      this.onContextChange();
    });
    
    document.getElementById('semester-select').addEventListener('change', (e) => {
      this.onContextChange();
    });
    
    // Tab switching
    document.querySelectorAll('.tab-button').forEach(button => {
      button.addEventListener('click', (e) => {
        this.switchTab(e.target.dataset.tab);
      });
    });
  }
  
  onContextChange() {
    const academicYearId = document.getElementById('academic-year-select').value;
    const semesterId = document.getElementById('semester-select').value;
    const yearCode = document.getElementById('academic-year-select').selectedOptions[0]?.text;
    const semesterType = document.getElementById('semester-select').selectedOptions[0]?.text;
    
    if (academicYearId && semesterId) {
      fileExplorerState.setContext({
        academicYearId: parseInt(academicYearId),
        semesterId: parseInt(semesterId),
        yearCode,
        semesterType
      });
      
      if (this.currentTab === 'file-explorer') {
        this.fileExplorerInstance.loadRoot();
      }
    }
  }
  
  switchTab(tabName) {
    this.currentTab = tabName;
    
    // Hide all tabs
    document.querySelectorAll('.tab-content').forEach(tab => {
      tab.classList.add('hidden');
    });
    
    // Show selected tab
    document.getElementById(`${tabName}-tab`).classList.remove('hidden');
    
    // Load File Explorer if needed
    if (tabName === 'file-explorer') {
      const context = fileExplorerState.getContext();
      if (context.academicYearId && context.semesterId) {
        this.fileExplorerInstance.loadRoot();
      }
    }
  }
  
  async createProfessor(professorData) {
    try {
      const response = await fetch('/api/deanship/professors', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(professorData)
      });
      
      if (response.ok) {
        // Refresh File Explorer
        if (this.currentTab === 'file-explorer') {
          await this.fileExplorerInstance.loadRoot(true);
          this.showToast('Professor folder created');
        }
      }
    } catch (error) {
      console.error('Error creating professor:', error);
    }
  }
}

// Initialize dashboard
const dashboard = new DeanshipDashboard();
```

---

## Folder Path Conventions

### Overview

The Archive System uses a hierarchical folder structure based on academic year, semester, professor, and course.

### Path Format

```
{yearCode}/{semesterType}/{professorId}/{courseCode} - {courseName}/{subfolderName}
```

### Components

| Component | Description | Example |
|-----------|-------------|---------|
| `yearCode` | Academic year code | `2024-2025` |
| `semesterType` | Semester type | `Fall`, `Spring`, `Summer` |
| `professorId` | Professor's database ID | `123` |
| `courseCode` | Course code | `CS101` |
| `courseName` | Course name | `Introduction to Programming` |
| `subfolderName` | Subfolder name | `Syllabus`, `Exams`, etc. |

### Examples

#### Professor Folder
```
2024-2025/Fall/123
```

#### Course Folder
```
2024-2025/Fall/123/CS101 - Introduction to Programming
```

#### Subfolder
```
2024-2025/Fall/123/CS101 - Introduction to Programming/Syllabus
```

### Path Generation

#### Backend (Java)

```java
// Professor folder path
String professorPath = String.format("%s/%s/%d",
    academicYear.getYearCode(),
    semester.getSemesterType(),
    professor.getId()
);

// Course folder path
String coursePath = String.format("%s/%s/%d/%s - %s",
    academicYear.getYearCode(),
    semester.getSemesterType(),
    professor.getId(),
    course.getCourseCode(),
    course.getCourseName()
);

// Subfolder path
String subfolderPath = coursePath + "/" + subfolderName;
```

#### Frontend (JavaScript)

```javascript
// Professor folder path
const professorPath = `${yearCode}/${semesterType}/${professorId}`;

// Course folder path
const coursePath = `${yearCode}/${semesterType}/${professorId}/${courseCode} - ${courseName}`;

// Subfolder path
const subfolderPath = `${coursePath}/${subfolderName}`;
```

---

## Standard Subfolder Structure

### Overview

Every course folder automatically includes four standard subfolders for organizing course materials.

### Standard Subfolders

1. **Syllabus**
   - Purpose: Course syllabus and curriculum documents
   - Typical contents: PDF syllabus, course outline, learning objectives

2. **Exams**
   - Purpose: Exam papers, solutions, and grading rubrics
   - Typical contents: Midterm exams, final exams, quizzes, answer keys

3. **Course Notes**
   - Purpose: Lecture notes, slides, and supplementary materials
   - Typical contents: PowerPoint slides, PDF notes, reading materials

4. **Assignments**
   - Purpose: Homework assignments, projects, and submissions
   - Typical contents: Assignment PDFs, project descriptions, student submissions

### Folder Structure Diagram

```
CS101 - Introduction to Programming/
├── Syllabus/
├── Exams/
├── Course Notes/
└── Assignments/
```

### Creating Custom Subfolders

While the system automatically creates standard subfolders, professors can create additional custom subfolders as needed through the File Explorer UI.

### Subfolder Naming Conventions

- Use clear, descriptive names
- Capitalize first letter of each word
- Avoid special characters except hyphens and spaces
- Keep names concise (under 50 characters)

---

## Auto-Provisioning Integration

### Overview

Auto-provisioning automatically creates folder structures when professors are created or courses are assigned.

### Professor Folder Auto-Provisioning

#### Trigger

Currently manual via endpoint (future: automatic on professor creation)

#### Implementation

```java
// In ProfessorController
@PostMapping("/{id}/create-folder")
public ResponseEntity<ApiResponse<Folder>> createProfessorFolder(
    @PathVariable Long id,
    @RequestParam Long academicYearId,
    @RequestParam Long semesterId
) {
    Folder folder = folderService.createProfessorFolder(id, academicYearId, semesterId);
    return ResponseEntity.ok(ApiResponse.success("Professor folder created", folder));
}
```

#### Frontend Integration

```javascript
async createProfessorFolder(professorId, academicYearId, semesterId) {
  const response = await fetch(
    `/api/deanship/professors/${professorId}/create-folder?academicYearId=${academicYearId}&semesterId=${semesterId}`,
    { method: 'POST' }
  );
  return response.json();
}
```

### Course Folder Auto-Provisioning

#### Trigger

Automatic when course is assigned to professor

#### Implementation

```java
// In CourseServiceImpl
@Override
@Transactional
public CourseAssignment assignCourse(CourseAssignmentRequest request) {
    // Create assignment
    CourseAssignment assignment = createAssignment(request);
    
    // Auto-create folders
    try {
        folderService.createCourseFolderStructure(assignment.getId());
        log.info("Course folders created for assignment: {}", assignment.getId());
    } catch (Exception e) {
        log.error("Failed to create course folders", e);
        // Don't fail assignment creation if folder creation fails
    }
    
    return assignment;
}
```

#### Frontend Integration

```javascript
async assignCourse(assignmentData) {
  // Assignment creation automatically triggers folder creation
  const response = await fetch('/api/deanship/course-assignments', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(assignmentData)
  });
  
  if (response.ok) {
    // Refresh File Explorer to show new folders
    await this.fileExplorerInstance.loadRoot(true);
    this.showToast('Course folders created');
  }
  
  return response.json();
}
```

---

## Best Practices

### State Management

1. **Always use FileExplorerState**: Never maintain local File Explorer state in dashboards
2. **Subscribe in constructor**: Subscribe to state changes when component initializes
3. **Unsubscribe on destroy**: Clean up subscriptions to prevent memory leaks
4. **Use context**: Always set context before loading File Explorer

### Loading States

1. **Show loading indicators**: Use loading states to provide user feedback
2. **Background refresh**: Use `loadRoot(true)` for background updates without loading spinner
3. **Separate loading states**: Use tree-specific and file-list-specific loading states when appropriate

### Error Handling

1. **Catch all errors**: Always wrap API calls in try-catch blocks
2. **Set error state**: Use `fileExplorerState.setError()` for centralized error handling
3. **Clear errors**: Clear error state when retrying operations
4. **User-friendly messages**: Display clear, actionable error messages

### Performance

1. **Lazy loading**: Only load File Explorer when tab is active
2. **Debounce searches**: Debounce search inputs to reduce API calls
3. **Cache context**: Cache academic year/semester selection
4. **Optimize queries**: Use indexed database queries for fast lookups

### Accessibility

1. **Keyboard navigation**: Ensure all File Explorer features are keyboard accessible
2. **ARIA labels**: Add appropriate ARIA labels for screen readers
3. **Focus management**: Manage focus when navigating folders
4. **Color contrast**: Ensure sufficient color contrast for readability

---

## Troubleshooting

### File Explorer Not Loading

**Symptom**: File Explorer shows "Select academic year and semester" message

**Solution**:
1. Verify academic year and semester are selected
2. Check that `fileExplorerState.setContext()` is called
3. Verify context values are not null

### State Not Persisting Across Tabs

**Symptom**: Expanded folders collapse when switching tabs

**Solution**:
1. Verify FileExplorerState is imported correctly
2. Check that component subscribes to state changes
3. Ensure state is not being reset unintentionally

### Folders Not Appearing After Creation

**Symptom**: New folders don't appear in File Explorer

**Solution**:
1. Call `fileExplorerInstance.loadRoot(true)` after folder creation
2. Verify folder was created successfully (check API response)
3. Check that context matches the created folder's year/semester

### Loading Spinner Stuck

**Symptom**: Loading spinner never disappears

**Solution**:
1. Check for JavaScript errors in console
2. Verify API endpoint is responding
3. Ensure `setLoading(false)` is called in finally block

### Layout Shifts During Loading

**Symptom**: UI jumps when loading completes

**Solution**:
1. Verify loading skeletons match content dimensions
2. Check that containers maintain fixed heights
3. Use CSS transitions for smooth state changes

---

## Additional Resources

- [FolderService API Documentation](../api/FolderService-API-Documentation.md)
- [File Explorer Component Source](../../src/main/resources/static/js/file-explorer.js)
- [FileExplorerState Source](../../src/main/resources/static/js/file-explorer-state.js)
- [Integration Tests](../../src/test/java/com/alqude/edu/ArchiveSystem/integration/)

---

**Last Updated**: November 21, 2025  
**Version**: 1.0.0
