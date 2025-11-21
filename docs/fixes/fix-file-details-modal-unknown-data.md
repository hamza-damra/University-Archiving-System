# Fix: File Details Modal Showing "Unknown" Data

## Issue
The File Details modal in the Professor Dashboard was showing "Unknown" for all file properties (File Name, Size, Type, Uploaded, Uploaded By) instead of displaying the actual file metadata.

## Root Cause
The frontend JavaScript code in `file-explorer.js` was trying to access file properties using an incorrect structure:
- Expected: `file.metadata.fileSize`, `file.metadata.fileType`, etc.
- Actual backend structure: `file.fileSize`, `file.fileType`, etc.

The `UploadedFile` entity returns properties directly on the object, not nested in a `metadata` object.

Additionally, the `uploader` field was lazy-loaded, causing it to not be included in the API response.

## Changes Made

### 1. Frontend Fix (`src/main/resources/static/js/file-explorer.js`)
Updated the `handleFileView` method to correctly access file properties:

**Before:**
```javascript
const metadata = file.metadata || {};
const content = `
    <div>
        <label>File Name:</label>
        <p>${file.name || metadata.originalFilename || 'Unknown'}</p>
    </div>
    <div>
        <label>Size:</label>
        <p>${metadata.fileSize ? this.formatFileSize(metadata.fileSize) : 'Unknown'}</p>
    </div>
    // ... more fields using metadata.*
`;
```

**After:**
```javascript
const uploaderName = file.uploader ? 
    (file.uploader.fullName || file.uploader.name || file.uploader.email || 'Unknown') : 
    'Unknown';

const content = `
    <div>
        <label>File Name:</label>
        <p>${file.originalFilename || 'Unknown'}</p>
    </div>
    <div>
        <label>Size:</label>
        <p>${file.fileSize ? this.formatFileSize(file.fileSize) : 'Unknown'}</p>
    </div>
    <div>
        <label>Type:</label>
        <p>${file.fileType || 'Unknown'}</p>
    </div>
    <div>
        <label>Uploaded:</label>
        <p>${file.createdAt ? formatDate(file.createdAt) : 'Unknown'}</p>
    </div>
    <div>
        <label>Uploaded By:</label>
        <p>${uploaderName}</p>
    </div>
    // ... notes if available
`;
```

### 2. Backend Fix (`src/main/java/com/alqude/edu/ArchiveSystem/service/FileServiceImpl.java`)
Updated the `getFile` method to eagerly fetch the uploader relationship:

**Before:**
```java
@Override
@Transactional(readOnly = true)
public UploadedFile getFile(Long fileId) {
    UploadedFile file = uploadedFileRepository.findById(fileId)
            .orElseThrow(() -> FileUploadException.fileNotFound(fileId));
    // ... permission check
    return file;
}
```

**After:**
```java
@Override
@Transactional(readOnly = true)
public UploadedFile getFile(Long fileId) {
    UploadedFile file = uploadedFileRepository.findById(fileId)
            .orElseThrow(() -> FileUploadException.fileNotFound(fileId));
    // ... permission check
    
    // Eagerly fetch uploader to avoid lazy loading issues in API response
    if (file.getUploader() != null) {
        file.getUploader().getFullName(); // Trigger lazy loading
    }
    
    return file;
}
```

## Testing
1. Navigate to Professor Dashboard
2. Go to File Explorer tab
3. Click on any file's "View" (eye icon) button
4. Verify the File Details modal shows:
   - Actual file name (not "Unknown")
   - File size in readable format (e.g., "2.5 MB")
   - File type (e.g., "application/pdf")
   - Upload date (formatted)
   - Uploader name (professor's full name)
   - Notes (if any were added during upload)

## Files Modified
- `src/main/resources/static/js/file-explorer.js` - Fixed property access in `handleFileView` method
- `src/main/java/com/alqude/edu/ArchiveSystem/service/FileServiceImpl.java` - Added eager loading for uploader

## Related
- File Explorer implementation
- UploadedFile entity structure
- API response serialization
