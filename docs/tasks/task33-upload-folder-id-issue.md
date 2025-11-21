# Task 33: Upload Folder ID Issue

## Current Status

✅ Upload button click event is working
✅ Modal opens correctly
❌ Upload fails because folder has no entityId

## Issue Description

When clicking the upload button for a document type folder (e.g., "Lecture Notes"), the folder node has `entityId: null`.

### Console Output:
```
Current node: {
  path: '/2024-2025/first/PROF6/CS101/lecture_notes',
  name: 'Lecture Notes',
  type: 'DOCUMENT_TYPE',
  entityId: null,  // ← THE PROBLEM
  metadata: {...},
  ...
}
✓ Current node exists
✗ Validation failed: Current node has no ID
```

## Root Cause

### Backend Behavior (FileExplorerServiceImpl.java)

**Line 431** - Document type folders created dynamically:
```java
.entityId(null) // No specific entity ID for document type folder
```

**Line 604** - Document type folders that exist in database:
```java
.entityId(subfolder.getId())
```

### Why entityId is null:

1. **Virtual Folders**: Document type folders (LECTURE_NOTES, EXAMS, etc.) are created dynamically by the backend
2. **Not in Database**: These folders don't exist as Folder entities until files are uploaded
3. **Auto-Creation**: The backend should auto-create these folders when files are uploaded

## Solutions

### Option 1: Backend Auto-Creation (RECOMMENDED)
Modify the upload endpoint to accept a path and auto-create folders:

**Current**:
```java
@PostMapping("/upload")
public ResponseEntity<...> uploadFiles(
    @RequestParam("files[]") MultipartFile[] files,
    @RequestParam("folderId") Long folderId,  // ← Requires existing folder
    ...
)
```

**Proposed**:
```java
@PostMapping("/upload")
public ResponseEntity<...> uploadFiles(
    @RequestParam("files[]") MultipartFile[] files,
    @RequestParam(value = "folderId", required = false) Long folderId,
    @RequestParam(value = "folderPath", required = false) String folderPath,  // ← NEW
    ...
) {
    // If folderId is null, create folder from path
    if (folderId == null && folderPath != null) {
        folderId = getOrCreateFolderByPath(folderPath);
    }
    ...
}
```

### Option 2: Frontend Pre-Creation
Add an API call to create the folder before opening the upload modal:

```javascript
// Create folder endpoint
POST /api/folders/create-by-path
Body: { path: "/2024-2025/first/PROF6/CS101/lecture_notes" }
Response: { id: 123, path: "...", ... }

// Then use the returned ID for upload
```

### Option 3: Use Parent Folder ID
Use the parent course's assignment ID instead:

```javascript
// Get parent course node
const parentNode = findParentCourseNode(currentNode);
const folderId = parentNode.entityId;  // Use course assignment ID
```

## Current Workaround

The frontend now shows a helpful message:
```
"This folder needs to be initialized first. Please contact your administrator."
```

## Recommended Implementation

### Step 1: Add Folder Creation Endpoint
```java
@PostMapping("/folders/get-or-create")
public ResponseEntity<Folder> getOrCreateFolder(
    @RequestParam("path") String path,
    @AuthenticationPrincipal UserDetails userDetails
) {
    // Parse path to extract components
    // Create folder hierarchy if doesn't exist
    // Return folder with ID
}
```

### Step 2: Update Frontend
```javascript
window.addEventListener('fileExplorerUpload', async (event) => {
    const currentNode = fileExplorerState.getCurrentNode();
    
    if (!currentNode.entityId) {
        // Create folder first
        const response = await fetch('/api/folders/get-or-create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ path: currentNode.path })
        });
        
        const folder = await response.json();
        
        // Update node with new entityId
        currentNode.entityId = folder.id;
        fileExplorerState.setCurrentNode(currentNode);
    }
    
    // Now open modal
    window.openFileExplorerUploadModal();
});
```

### Step 3: Update Upload Service
Modify `FolderFileUploadService` to handle folder creation:

```java
public List<UploadedFile> uploadFiles(
    MultipartFile[] files,
    Long folderId,
    String folderPath,  // NEW parameter
    String notes,
    Long uploaderId
) {
    // If folderId is null, create from path
    if (folderId == null && folderPath != null) {
        Folder folder = getOrCreateFolderByPath(folderPath, uploaderId);
        folderId = folder.getId();
    }
    
    // Continue with existing logic
    ...
}
```

## Files Modified

1. `src/main/resources/static/js/prof.js`
   - Updated validation to accept `id` or `entityId`
   - Added check for null entityId
   - Show helpful error message

## Next Steps

1. **Decide on solution approach** (Option 1 recommended)
2. **Implement folder creation endpoint**
3. **Update upload service to handle paths**
4. **Test with folders that don't exist yet**
5. **Verify auto-creation works correctly**

## Testing Checklist

- [ ] Upload to existing folder (with entityId)
- [ ] Upload to new document type folder (no entityId)
- [ ] Verify folder is created automatically
- [ ] Verify files are uploaded successfully
- [ ] Check folder permissions are set correctly
- [ ] Test with different document types

## Status

⏸️ Blocked - Waiting for backend folder creation implementation
📝 Workaround in place - Shows helpful error message
🔄 Ready for backend changes
