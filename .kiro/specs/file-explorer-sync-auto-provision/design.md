# Design Document

## Overview

This design document outlines the architecture and implementation approach for enhancing the Deanship Archiving System's File Explorer module. The enhancement focuses on four key areas:

1. **Automatic Folder Provisioning**: Auto-create professor and course folder structures when entities are created or assigned
2. **Global Synchronization**: Ensure consistent File Explorer state across all dashboards (Dean, Professor, HOD)
3. **UI Stability**: Eliminate layout shifts during data loading with proper loading states
4. **Centralized State Management**: Implement a robust state management layer for predictable UI behavior

The system uses Spring Boot for the backend and vanilla JavaScript for the frontend, with a shared File Explorer component already in use across multiple dashboards.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Dean       │  │  Professor   │  │     HOD      │      │
│  │  Dashboard   │  │  Dashboard   │  │  Dashboard   │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│         └──────────────────┼──────────────────┘              │
│                            │                                 │
│                   ┌────────▼────────┐                        │
│                   │ FileExplorerState│                       │
│                   │  (Centralized)   │                       │
│                   └────────┬────────┘                        │
│                            │                                 │
│                   ┌────────▼────────┐                        │
│                   │  FileExplorer   │                        │
│                   │   Component     │                        │
│                   └────────┬────────┘                        │
└────────────────────────────┼──────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │   API Layer     │
                    │  (api.js)       │
                    └────────┬────────┘
                             │
┌────────────────────────────┼──────────────────────────────────┐
│                    Backend Layer                              │
│                            │                                  │
│         ┌──────────────────┼──────────────────┐              │
│         │                  │                  │              │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐      │
│  │  Professor   │  │    Course    │  │     File     │      │
│  │  Controller  │  │  Assignment  │  │   Explorer   │      │
│  │              │  │  Controller  │  │  Controller  │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐      │
│  │  Professor   │  │  Assignment  │  │     File     │      │
│  │   Service    │  │   Service    │  │   Service    │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│         └──────────────────┼──────────────────┘              │
│                            │                                 │
│                   ┌────────▼────────┐                        │
│                   │  FolderService  │                        │
│                   │  (NEW)          │                        │
│                   └────────┬────────┘                        │
│                            │                                 │
│         ┌──────────────────┼──────────────────┐              │
│         │                  │                  │              │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐      │
│  │   Folder     │  │     File     │  │   Database   │      │
│  │  Repository  │  │  Repository  │  │  (Entities)  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

**Professor Creation Flow:**
```
Dean Dashboard → ProfessorController.createProfessor()
                 ↓
              ProfessorService.createProfessor()
                 ↓
              FolderService.createProfessorFolder()
                 ↓
              Database + File System
                 ↓
              Return Professor + Folder Info
                 ↓
              Frontend Updates → FileExplorerState.refresh()
```

**Course Assignment Flow:**
```
Dean Dashboard → CourseAssignmentController.createAssignment()
                 ↓
              CourseAssignmentService.createAssignment()
                 ↓
              FolderService.createCourseFolderStructure()
                 ↓
              Database + File System
                 ↓
              Return Assignment + Folder Info
                 ↓
              Frontend Updates → FileExplorerState.refresh()
```

## Components and Interfaces

### Backend Components

#### 1. FolderService (NEW)

**Purpose**: Centralized service for managing folder creation, ensuring idempotency and consistency.

**Interface**:
```java
public interface FolderService {
    /**
     * Create professor root folder for a specific academic year and semester
     * Idempotent: returns existing folder if already created
     */
    Folder createProfessorFolder(Long professorId, Long academicYearId, Long semesterId);
    
    /**
     * Create course folder structure under professor folder
     * Creates: <CourseCode> - <CourseName>/Syllabus/Exams/Course Notes/Assignments
     * Idempotent: skips existing folders
     */
    List<Folder> createCourseFolderStructure(Long professorId, Long courseId, 
                                             Long academicYearId, Long semesterId);
    
    /**
     * Check if professor folder exists
     */
    boolean professorFolderExists(Long professorId, Long academicYearId, Long semesterId);
    
    /**
     * Check if course folder exists
     */
    boolean courseFolderExists(Long professorId, Long courseId, 
                               Long academicYearId, Long semesterId);
    
    /**
     * Get folder by path
     */
    Optional<Folder> getFolderByPath(String path);
    
    /**
     * Create folder if not exists (utility method)
     */
    Folder createFolderIfNotExists(String path, String name, Folder parent, 
                                   FolderType type, User owner);
}
```

**Key Responsibilities**:
- Folder creation with idempotency checks
- Path generation following convention: `{yearCode}/{semester}/{professorId}/{courseCode}/{subfolder}`
- Physical file system directory creation
- Database entity persistence
- Transaction management

#### 2. Enhanced ProfessorService

**Modifications**:
```java
@Service
public class ProfessorServiceImpl implements ProfessorService {
    
    private final FolderService folderService;
    
    @Override
    @Transactional
    public User createProfessor(ProfessorDTO dto) {
        // Existing professor creation logic...
        User professor = // ... create professor
        
        // NEW: Auto-create professor folder for current academic year/semester
        if (currentAcademicYear != null && currentSemester != null) {
            try {
                folderService.createProfessorFolder(
                    professor.getId(), 
                    currentAcademicYear.getId(), 
                    currentSemester.getId()
                );
                log.info("Auto-created folder for professor: {}", professor.getProfessorId());
            } catch (Exception e) {
                log.error("Failed to auto-create professor folder", e);
                // Don't fail professor creation if folder creation fails
            }
        }
        
        return professor;
    }
}
```

#### 3. Enhanced CourseAssignmentService

**Modifications**:
```java
@Service
public class CourseAssignmentServiceImpl implements CourseAssignmentService {
    
    private final FolderService folderService;
    
    @Override
    @Transactional
    public CourseAssignment createAssignment(CourseAssignmentDTO dto) {
        // Existing assignment creation logic...
        CourseAssignment assignment = // ... create assignment
        
        // NEW: Auto-create course folder structure
        try {
            folderService.createCourseFolderStructure(
                assignment.getProfessor().getId(),
                assignment.getCourse().getId(),
                assignment.getSemester().getAcademicYear().getId(),
                assignment.getSemester().getId()
            );
            log.info("Auto-created course folders for assignment: {}", assignment.getId());
        } catch (Exception e) {
            log.error("Failed to auto-create course folders", e);
            // Don't fail assignment creation if folder creation fails
        }
        
        return assignment;
    }
}
```

#### 4. Folder Entity (Existing - May Need Enhancements)

**Potential Enhancements**:
```java
@Entity
@Table(name = "folders", indexes = {
    @Index(name = "idx_folder_path", columnList = "path", unique = true),
    @Index(name = "idx_folder_parent", columnList = "parent_id"),
    @Index(name = "idx_folder_owner", columnList = "owner_id"),
    @Index(name = "idx_folder_context", columnList = "academic_year_id, semester_id, owner_id")
})
public class Folder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 500)
    private String path; // Full path: "2024-2025/first/PROF123/CS101"
    
    @Column(nullable = false)
    private String name; // Display name: "CS101 - Data Structures"
    
    @Enumerated(EnumType.STRING)
    private FolderType type; // PROFESSOR_ROOT, COURSE, SUBFOLDER
    
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Folder parent;
    
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner; // Professor who owns this folder
    
    @ManyToOne
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;
    
    @ManyToOne
    @JoinColumn(name = "semester_id")
    private Semester semester;
    
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course; // Null for non-course folders
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Getters, setters, constructors...
}
```

### Frontend Components

#### 1. FileExplorerState (NEW)

**Purpose**: Centralized state management for File Explorer across all dashboards.

**Structure**:
```javascript
// file-explorer-state.js

class FileExplorerState {
    constructor() {
        this.state = {
            // Context
            academicYearId: null,
            semesterId: null,
            academicYearCode: null,
            semesterType: null,
            
            // Data
            treeRoot: null,
            currentNode: null,
            currentPath: '',
            breadcrumbs: [],
            expandedNodes: new Set(),
            
            // UI State
            isLoading: false,
            isTreeLoading: false,
            isFileListLoading: false,
            error: null,
            
            // Metadata
            lastUpdated: null,
        };
        
        this.listeners = [];
    }
    
    // State getters
    getState() {
        return { ...this.state };
    }
    
    getContext() {
        return {
            academicYearId: this.state.academicYearId,
            semesterId: this.state.semesterId,
            academicYearCode: this.state.academicYearCode,
            semesterType: this.state.semesterType,
        };
    }
    
    // State setters
    setContext(academicYearId, semesterId, academicYearCode, semesterType) {
        this.state.academicYearId = academicYearId;
        this.state.semesterId = semesterId;
        this.state.academicYearCode = academicYearCode;
        this.state.semesterType = semesterType;
        this.state.lastUpdated = Date.now();
        this.notify();
    }
    
    setTreeRoot(treeRoot) {
        this.state.treeRoot = treeRoot;
        this.state.lastUpdated = Date.now();
        this.notify();
    }
    
    setCurrentNode(node, path) {
        this.state.currentNode = node;
        this.state.currentPath = path;
        this.state.lastUpdated = Date.now();
        this.notify();
    }
    
    setBreadcrumbs(breadcrumbs) {
        this.state.breadcrumbs = breadcrumbs;
        this.notify();
    }
    
    toggleNodeExpansion(path) {
        if (this.state.expandedNodes.has(path)) {
            this.state.expandedNodes.delete(path);
        } else {
            this.state.expandedNodes.add(path);
        }
        this.notify();
    }
    
    setLoading(isLoading) {
        this.state.isLoading = isLoading;
        this.notify();
    }
    
    setTreeLoading(isLoading) {
        this.state.isTreeLoading = isLoading;
        this.notify();
    }
    
    setFileListLoading(isLoading) {
        this.state.isFileListLoading = isLoading;
        this.notify();
    }
    
    setError(error) {
        this.state.error = error;
        this.notify();
    }
    
    clearError() {
        this.state.error = null;
        this.notify();
    }
    
    // State reset
    reset() {
        this.state = {
            academicYearId: null,
            semesterId: null,
            academicYearCode: null,
            semesterType: null,
            treeRoot: null,
            currentNode: null,
            currentPath: '',
            breadcrumbs: [],
            expandedNodes: new Set(),
            isLoading: false,
            isTreeLoading: false,
            isFileListLoading: false,
            error: null,
            lastUpdated: null,
        };
        this.notify();
    }
    
    // Observer pattern
    subscribe(listener) {
        this.listeners.push(listener);
        return () => {
            this.listeners = this.listeners.filter(l => l !== listener);
        };
    }
    
    notify() {
        this.listeners.forEach(listener => listener(this.getState()));
    }
}

// Singleton instance
export const fileExplorerState = new FileExplorerState();
```

#### 2. Enhanced FileExplorer Component

**Modifications**:
```javascript
// file-explorer.js

import { fileExplorerState } from './file-explorer-state.js';

export class FileExplorer {
    constructor(containerId, options = {}) {
        // ... existing constructor code
        
        // Subscribe to state changes
        this.unsubscribe = fileExplorerState.subscribe((state) => {
            this.onStateChange(state);
        });
    }
    
    /**
     * Handle state changes from FileExplorerState
     */
    onStateChange(state) {
        // Update local references
        this.currentPath = state.currentPath;
        this.currentNode = state.currentNode;
        this.treeRoot = state.treeRoot;
        this.breadcrumbs = state.breadcrumbs;
        this.expandedNodes = state.expandedNodes;
        
        // Update UI based on loading states
        if (state.isLoading) {
            this.showLoading();
        } else if (state.isTreeLoading) {
            this.showTreeLoading();
        } else if (state.isFileListLoading) {
            this.showFileListLoading();
        } else {
            // Render normal state
            this.renderTree(state.treeRoot);
            this.renderFileList(state.currentNode);
            this.renderBreadcrumbs();
        }
        
        // Handle errors
        if (state.error) {
            this.renderError(state.error);
        }
    }
    
    /**
     * Load root with stable loading state
     */
    async loadRoot(academicYearId, semesterId, isBackground = false) {
        if (!isBackground) {
            fileExplorerState.setLoading(true);
        }
        
        try {
            const response = await fileExplorer.getRoot(academicYearId, semesterId);
            const treeRoot = response.data || response;
            
            fileExplorerState.setTreeRoot(treeRoot);
            fileExplorerState.setCurrentNode(treeRoot, treeRoot.path || '');
            
            await this.loadBreadcrumbs(treeRoot.path || '');
            
            fileExplorerState.setLoading(false);
        } catch (error) {
            console.error('Error loading file explorer root:', error);
            fileExplorerState.setError(error.message);
            fileExplorerState.setLoading(false);
        }
    }
    
    /**
     * Show loading state with skeleton (no layout shift)
     */
    showLoading() {
        const treeContainer = document.getElementById('fileExplorerTree');
        const fileListContainer = document.getElementById('fileExplorerFileList');
        
        if (treeContainer) {
            treeContainer.innerHTML = this.renderLoadingSkeleton('tree');
        }
        
        if (fileListContainer) {
            fileListContainer.innerHTML = this.renderLoadingSkeleton('fileList');
        }
    }
    
    /**
     * Render loading skeleton (maintains layout)
     */
    renderLoadingSkeleton(type) {
        if (type === 'tree') {
            return `
                <div class="space-y-2 animate-pulse">
                    ${Array(5).fill(0).map(() => `
                        <div class="flex items-center space-x-2">
                            <div class="w-4 h-4 bg-gray-200 rounded"></div>
                            <div class="h-4 bg-gray-200 rounded flex-1"></div>
                        </div>
                    `).join('')}
                </div>
            `;
        } else {
            return `
                <div class="space-y-3 animate-pulse">
                    ${Array(3).fill(0).map(() => `
                        <div class="border border-gray-200 rounded-lg p-4">
                            <div class="flex items-center space-x-3">
                                <div class="w-12 h-12 bg-gray-200 rounded-lg"></div>
                                <div class="flex-1 space-y-2">
                                    <div class="h-4 bg-gray-200 rounded w-3/4"></div>
                                    <div class="h-3 bg-gray-200 rounded w-1/2"></div>
                                </div>
                            </div>
                        </div>
                    `).join('')}
                </div>
            `;
        }
    }
    
    /**
     * Cleanup on destroy
     */
    destroy() {
        if (this.unsubscribe) {
            this.unsubscribe();
        }
    }
}
```

#### 3. Enhanced Dashboard Integration

**Deanship Dashboard Modifications**:
```javascript
// deanship.js

import { fileExplorerState } from './file-explorer-state.js';

// When academic year or semester changes
function onContextChange() {
    const academicYear = selectedAcademicYear;
    const semester = semesters.find(s => s.id === selectedSemesterId);
    
    if (academicYear && semester) {
        // Update File Explorer state
        fileExplorerState.setContext(
            academicYear.id,
            semester.id,
            academicYear.yearCode,
            semester.type
        );
        
        // Reload File Explorer if on that tab
        if (currentTab === 'file-explorer') {
            loadFileExplorer();
        }
    }
}

// After creating professor
async function handleCreateProfessor(closeModal) {
    // ... existing professor creation code
    
    try {
        const newProfessor = await apiRequest('/deanship/professors', {
            method: 'POST',
            body: JSON.stringify(professorData)
        });
        
        showToast('Professor created successfully', 'success');
        closeModal();
        loadProfessors();
        
        // Refresh File Explorer if on that tab
        if (currentTab === 'file-explorer' && selectedAcademicYearId && selectedSemesterId) {
            await fileExplorerInstance.loadRoot(selectedAcademicYearId, selectedSemesterId, true);
        }
    } catch (error) {
        // ... error handling
    }
}

// After creating course assignment
async function handleCreateAssignment(closeModal) {
    // ... existing assignment creation code
    
    try {
        const newAssignment = await apiRequest('/deanship/course-assignments', {
            method: 'POST',
            body: JSON.stringify(assignmentData)
        });
        
        showToast('Course assignment created successfully', 'success');
        closeModal();
        loadAssignments();
        
        // Refresh File Explorer if on that tab
        if (currentTab === 'file-explorer' && selectedAcademicYearId && selectedSemesterId) {
            await fileExplorerInstance.loadRoot(selectedAcademicYearId, selectedSemesterId, true);
        }
    } catch (error) {
        // ... error handling
    }
}
```

## Data Models

### Folder Path Convention

**Format**: `{yearCode}/{semesterType}/{professorId}/{courseCode}/{subfolder}`

**Examples**:
- Professor root: `2024-2025/first/PROF123`
- Course folder: `2024-2025/first/PROF123/CS101 - Data Structures`
- Subfolder: `2024-2025/first/PROF123/CS101 - Data Structures/Syllabus`

### Standard Course Subfolders

1. **Syllabus** - Course syllabus documents
2. **Exams** - Exam papers and solutions
3. **Course Notes** - Lecture notes and materials
4. **Assignments** - Assignment documents and submissions

### Folder Types Enum

```java
public enum FolderType {
    YEAR_ROOT,          // Academic year root (e.g., "2024-2025")
    SEMESTER_ROOT,      // Semester root (e.g., "first")
    PROFESSOR_ROOT,     // Professor root folder
    COURSE,             // Course folder
    SUBFOLDER           // Standard subfolder (Syllabus, Exams, etc.)
}
```

## Error Handling

### Backend Error Handling

**Idempotency Strategy**:
- Check if folder exists before creation
- Return existing folder if found
- Log warnings for duplicate attempts
- Never throw exceptions for duplicate folder creation

**Transaction Management**:
- Wrap folder creation in transactions
- Rollback on database errors
- Clean up file system on transaction rollback
- Log all errors with context

**Error Responses**:
```java
// Success with existing folder
{
    "success": true,
    "message": "Professor folder already exists",
    "data": { /* folder info */ }
}

// Success with new folder
{
    "success": true,
    "message": "Professor folder created successfully",
    "data": { /* folder info */ }
}

// Error
{
    "success": false,
    "message": "Failed to create professor folder",
    "error": "Detailed error message"
}
```

### Frontend Error Handling

**Loading State Errors**:
- Show error state in File Explorer without breaking layout
- Provide retry button
- Log errors to console for debugging

**Network Errors**:
- Detect network failures
- Show user-friendly message
- Offer manual refresh option

**Empty State vs Error State**:
- Empty state: No data available (normal condition)
- Error state: Request failed (abnormal condition)
- Use different UI for each case

## Testing Strategy

### Backend Testing

#### Unit Tests

**FolderService Tests**:
```java
@Test
void testCreateProfessorFolder_Success() {
    // Test successful folder creation
}

@Test
void testCreateProfessorFolder_Idempotent() {
    // Test that calling twice returns same folder
}

@Test
void testCreateCourseFolderStructure_Success() {
    // Test course folder structure creation
}

@Test
void testCreateCourseFolderStructure_PartialExists() {
    // Test when some subfolders already exist
}
```

**ProfessorService Tests**:
```java
@Test
void testCreateProfessor_WithFolderCreation() {
    // Test professor creation triggers folder creation
}

@Test
void testCreateProfessor_FolderCreationFails_ProfessorStillCreated() {
    // Test that professor creation succeeds even if folder creation fails
}
```

**CourseAssignmentService Tests**:
```java
@Test
void testCreateAssignment_WithCourseFolders() {
    // Test assignment creation triggers course folder creation
}
```

#### Integration Tests

**End-to-End Flow Tests**:
```java
@Test
@Transactional
void testProfessorCreationToFolderExplorer() {
    // 1. Create professor
    // 2. Verify folder exists in database
    // 3. Verify folder exists in file system
    // 4. Call File Explorer API
    // 5. Verify folder appears in tree
}

@Test
@Transactional
void testCourseAssignmentToFolderExplorer() {
    // 1. Create professor
    // 2. Create course
    // 3. Create assignment
    // 4. Verify course folder structure exists
    // 5. Call File Explorer API
    // 6. Verify folders appear in tree
}
```

### Frontend Testing

#### Manual Testing Checklist

**Professor Creation**:
- [ ] Create professor from Dean dashboard
- [ ] Verify folder appears in File Explorer tree (Dean view)
- [ ] Switch to Professor dashboard
- [ ] Verify folder appears in File Explorer tree (Professor view)
- [ ] Verify no layout shift during loading

**Course Assignment**:
- [ ] Assign course to professor
- [ ] Verify course folder structure appears in File Explorer
- [ ] Verify all standard subfolders are created
- [ ] Verify no duplicates if assigned twice
- [ ] Verify no layout shift during loading

**State Management**:
- [ ] Change academic year - verify File Explorer updates
- [ ] Change semester - verify File Explorer updates
- [ ] Rapidly switch between folders - verify no flickering
- [ ] Switch tabs and return - verify state preserved

**Loading States**:
- [ ] Verify skeleton appears during initial load
- [ ] Verify layout remains stable during loading
- [ ] Verify smooth transition from loading to loaded state
- [ ] Verify error state displays correctly

#### Browser Testing

Test in:
- Chrome (latest)
- Firefox (latest)
- Edge (latest)
- Safari (latest)

### Performance Testing

**Metrics to Monitor**:
- Folder creation time (should be < 500ms)
- File Explorer load time (should be < 1s)
- UI rendering time (should be < 100ms)
- Memory usage during rapid navigation

**Load Testing**:
- Create 100 professors simultaneously
- Create 500 course assignments
- Navigate File Explorer with 1000+ folders
- Verify no performance degradation

## Implementation Notes

### Phase 1: Backend Foundation
1. Create FolderService interface and implementation
2. Add folder creation to ProfessorService
3. Add folder creation to CourseAssignmentService
4. Write unit tests
5. Write integration tests

### Phase 2: Frontend State Management
1. Create FileExplorerState class
2. Integrate state into FileExplorer component
3. Add loading skeleton rendering
4. Test state synchronization

### Phase 3: Dashboard Integration
1. Update Deanship dashboard to use FileExplorerState
2. Update Professor dashboard to use FileExplorerState
3. Update HOD dashboard to use FileExplorerState
4. Add refresh logic after entity creation

### Phase 4: UI Polish
1. Enhance folder tree visual design (larger items)
2. Improve loading states
3. Add smooth transitions
4. Test across browsers

### Phase 5: Testing & Documentation
1. Complete manual testing checklist
2. Perform browser compatibility testing
3. Update API documentation
4. Create user guide updates
