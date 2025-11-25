# Design Document

## Overview

The File Preview System is a comprehensive feature that enables users across all three dashboards (Professor, Dean, and HOD) to preview textual file contents directly within the browser without downloading files. The system will integrate seamlessly with the existing file explorer component and provide support for multiple file formats including PDFs, Office documents, code files, and plain text.

The design follows a modular architecture with clear separation between frontend preview rendering, backend file retrieval, and format-specific handlers. The system prioritizes performance, user experience, and maintainability while reusing existing authentication and file management infrastructure.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    User Interface Layer                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Professor   │  │     Dean     │  │     HOD      │      │
│  │  Dashboard   │  │  Dashboard   │  │  Dashboard   │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│         └──────────────────┼──────────────────┘              │
│                            │                                 │
└────────────────────────────┼─────────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────────┐
│              File Preview Component Layer                    │
│                            │                                 │
│  ┌─────────────────────────▼──────────────────────────────┐ │
│  │         FilePreviewModal (Main Component)              │ │
│  │  - Modal UI Management                                 │ │
│  │  - File Type Detection                                 │ │
│  │  - Preview Renderer Selection                          │ │
│  │  - Loading & Error States                              │ │
│  └─────────────────────────┬──────────────────────────────┘ │
│                            │                                 │
│  ┌─────────────────────────▼──────────────────────────────┐ │
│  │         Format-Specific Renderers                      │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │ │
│  │  │   PDF    │ │  Office  │ │   Code   │ │   Text   │  │ │
│  │  │ Renderer │ │ Renderer │ │ Renderer │ │ Renderer │  │ │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │ │
│  └────────────────────────────────────────────────────────┘ │
└────────────────────────────┬─────────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────────┐
│                  Backend API Layer                           │
│                            │                                 │
│  ┌─────────────────────────▼──────────────────────────────┐ │
│  │         FilePreviewController                          │ │
│  │  - GET /api/file-explorer/files/{id}/preview          │ │
│  │  - GET /api/file-explorer/files/{id}/content          │ │
│  │  - GET /api/file-explorer/files/{id}/metadata         │ │
│  └─────────────────────────┬──────────────────────────────┘ │
│                            │                                 │
│  ┌─────────────────────────▼──────────────────────────────┐ │
│  │         FilePreviewService                             │ │
│  │  - File Content Retrieval                              │ │
│  │  - Format Detection                                    │ │
│  │  - Content Conversion (Office → HTML/PDF)             │ │
│  │  - Permission Validation                               │ │
│  └─────────────────────────┬──────────────────────────────┘ │
│                            │                                 │
│  ┌─────────────────────────▼──────────────────────────────┐ │
│  │         Existing Services                              │ │
│  │  - FileStorageService (file retrieval)                │ │
│  │  - AuthenticationService (permissions)                │ │
│  │  - UploadedFileRepository (metadata)                  │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

1. **User Triggers Preview**: User clicks preview icon/button on a file in the file explorer
2. **Modal Opens**: FilePreviewModal component opens with loading state
3. **Metadata Request**: Frontend requests file metadata to determine file type
4. **Content Request**: Frontend requests file content based on detected type
5. **Rendering**: Appropriate renderer displays the content in the modal
6. **User Interaction**: User can scroll, search, navigate pages, or download

## Components and Interfaces

### Frontend Components

#### 1. FilePreviewModal

Main component responsible for managing the preview modal UI.

**Responsibilities:**
- Display modal overlay with file preview
- Manage modal lifecycle (open, close, ESC key handling)
- Show loading and error states
- Coordinate between different renderers
- Provide download and close actions

**Public API:**
```javascript
class FilePreviewModal {
    constructor(options = {})
    
    // Open preview for a file
    async open(fileId, fileName, fileType)
    
    // Close the modal
    close()
    
    // Download the current file
    async downloadFile()
    
    // Show error state
    showError(message)
    
    // Show loading state
    showLoading()
}
```

**Events:**
- `preview:opened` - Fired when modal opens
- `preview:closed` - Fired when modal closes
- `preview:error` - Fired when preview fails
- `preview:loaded` - Fired when content loads successfully

#### 2. Format-Specific Renderers

##### PDFRenderer
```javascript
class PDFRenderer {
    // Render PDF using browser's native PDF viewer or PDF.js
    async render(fileId, container)
    
    // Navigate to specific page
    goToPage(pageNumber)
    
    // Get total page count
    getPageCount()
}
```

##### OfficeRenderer
```javascript
class OfficeRenderer {
    // Render Office documents (converted to HTML or PDF on backend)
    async render(fileId, container)
    
    // Handle different Office formats
    supportsFormat(mimeType)
}
```

##### CodeRenderer
```javascript
class CodeRenderer {
    // Render code files with syntax highlighting
    async render(fileId, container, language)
    
    // Detect language from file extension
    detectLanguage(fileName)
    
    // Apply syntax highlighting
    applySyntaxHighlighting(code, language)
}
```

##### TextRenderer
```javascript
class TextRenderer {
    // Render plain text files
    async render(fileId, container)
    
    // Implement virtual scrolling for large files
    enableVirtualScrolling(content)
    
    // Search within text
    search(query)
}
```

#### 3. FilePreviewButton

Component to add preview buttons to file explorer rows.

```javascript
class FilePreviewButton {
    // Check if file type is previewable
    static isPreviewable(fileType)
    
    // Render preview button for a file
    static renderButton(file)
    
    // Get appropriate icon for file type
    static getFileIcon(fileType)
}
```

### Backend Components

#### 1. FilePreviewController

REST controller for file preview endpoints.

```java
@RestController
@RequestMapping("/api/file-explorer/files")
public class FilePreviewController {
    
    // Get file metadata for preview
    @GetMapping("/{fileId}/metadata")
    public ResponseEntity<FileMetadataDTO> getFileMetadata(@PathVariable Long fileId)
    
    // Get file content for preview (text, code)
    @GetMapping("/{fileId}/content")
    public ResponseEntity<String> getFileContent(@PathVariable Long fileId)
    
    // Get file preview (converted format for Office docs)
    @GetMapping("/{fileId}/preview")
    public ResponseEntity<byte[]> getFilePreview(@PathVariable Long fileId)
    
    // Check if file is previewable
    @GetMapping("/{fileId}/previewable")
    public ResponseEntity<Boolean> isPreviewable(@PathVariable Long fileId)
}
```

#### 2. FilePreviewService

Service layer for file preview logic.

```java
@Service
public class FilePreviewService {
    
    // Get file metadata
    FileMetadataDTO getFileMetadata(Long fileId, User currentUser)
    
    // Get file content as string (for text files)
    String getFileContent(Long fileId, User currentUser)
    
    // Get file preview (converted format)
    byte[] getFilePreview(Long fileId, User currentUser)
    
    // Check if file type is previewable
    boolean isPreviewable(String mimeType)
    
    // Validate user has permission to preview file
    boolean canUserPreviewFile(Long fileId, User user)
    
    // Convert Office document to HTML/PDF
    byte[] convertOfficeDocument(File file, String targetFormat)
    
    // Detect file MIME type
    String detectMimeType(File file)
}
```

#### 3. FileMetadataDTO

Data transfer object for file metadata.

```java
public class FileMetadataDTO {
    private Long id;
    private String fileName;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
    private LocalDateTime uploadDate;
    private String uploaderName;
    private String uploaderEmail;
    private String departmentName;
    private boolean previewable;
    private String previewType; // "pdf", "office", "code", "text", "image", "unsupported"
}
```

## Data Models

### Existing Models (Reused)

The system will reuse existing data models:

- **UploadedFile**: Contains file metadata (id, filename, path, size, mimeType, uploadDate)
- **User**: Contains user information (id, name, email, role, department)
- **CourseAssignment**: Links files to courses and professors
- **Department**: Department information for access control

### New Models

#### FilePreviewCache (Optional - Future Enhancement)

```java
@Entity
public class FilePreviewCache {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long fileId;
    private String previewType;
    private String cachedPath;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
```

This model can be added later to cache converted Office documents and improve performance.

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*


### Property Reflection

Before defining the final correctness properties, I've reviewed all testable properties from the prework to eliminate redundancy:

**Redundancies Identified:**
1. Properties 1.2, 2.2, and 3.2 all test metadata display but for different roles - these can be combined into one comprehensive property that validates metadata display includes role-appropriate information
2. Properties 2.4, 3.3, and 10.1 all test error message display for different error conditions - these share the same underlying behavior of showing error messages
3. Properties 5.1, 5.2, 5.3, and 5.4 all test visual indicators for previewable files - these can be combined into a comprehensive property about preview button rendering
4. Properties 4.1, 4.2, 4.3, 4.4 all test format-specific rendering - while they test different formats, they share the same underlying property: correct renderer selection based on file type

**Consolidation Strategy:**
- Combine metadata display properties into one that validates all required fields are present
- Keep error handling properties separate as they test different error scenarios (permissions, missing files, service errors)
- Combine visual indicator properties into one comprehensive property about preview button state
- Keep format-specific rendering properties separate as each format requires different rendering logic

### Correctness Properties

Property 1: Preview modal displays file content
*For any* supported file type, when a user triggers preview, the system should open a modal containing the file content
**Validates: Requirements 1.1**

Property 2: Preview modal displays complete metadata
*For any* file being previewed, the modal header should display file name, size, type, upload date, and role-appropriate uploader information (professor name for Dean/HOD, department for Dean)
**Validates: Requirements 1.2, 2.2, 3.2**

Property 3: Format-specific renderer selection
*For any* file type, the system should select and apply the appropriate renderer (PDF renderer for PDFs, code renderer for code files, text renderer for text files, Office renderer for Office documents)
**Validates: Requirements 1.3, 4.1, 4.2, 4.3, 4.4**

Property 4: Modal dismissal behavior
*For any* open preview modal, clicking outside the modal or pressing the Escape key should close the modal
**Validates: Requirements 1.4**

Property 5: Download action in preview
*For any* file being previewed, clicking the download button should trigger a download of that specific file
**Validates: Requirements 1.5**

Property 6: Permission-based preview availability
*For any* file in the file explorer, the preview button should be enabled if and only if the current user has permission to view that file
**Validates: Requirements 2.1, 3.1**

Property 7: Unauthorized preview attempt handling
*For any* file that a user does not have permission to view, attempting to preview should result in an error message being displayed
**Validates: Requirements 2.4, 3.3**

Property 8: Department-scoped access for HOD
*For any* file, an HOD should be able to preview it if and only if the file belongs to their department
**Validates: Requirements 3.1, 3.3**

Property 9: Unsupported format handling
*For any* unsupported file type, attempting to preview should display a message indicating preview is unavailable and offer a download option
**Validates: Requirements 4.5**

Property 10: Preview button rendering
*For any* file in the file explorer, a preview button should be rendered if the file type is supported, and the button should have appropriate visual indicators and tooltips
**Validates: Requirements 5.1, 5.2, 5.3, 5.4**

Property 11: Loading indicator display
*For any* preview request, a loading indicator should appear within 100 milliseconds of the request being initiated
**Validates: Requirements 6.1**

Property 12: Large file warning
*For any* file larger than 5MB, attempting to preview should display a warning message with options for partial preview or download
**Validates: Requirements 6.3**

Property 13: Network error handling
*For any* preview request that fails due to network error, the system should display an error message with a retry option
**Validates: Requirements 6.4**

Property 14: Multi-page document navigation
*For any* multi-page document (PDF or converted Office doc), the preview should provide pagination or scroll controls
**Validates: Requirements 7.1, 7.3**

Property 15: Virtual scrolling for large text files
*For any* text file with more than 1000 lines, the preview should implement virtual scrolling
**Validates: Requirements 7.2**

Property 16: Search functionality in preview
*For any* text-based preview, searching for a query string should highlight all matches and provide navigation between them
**Validates: Requirements 7.4**

Property 17: Cross-dashboard compatibility
*For any* dashboard role (Professor, Dean, HOD), the preview system should function correctly with role-appropriate permissions and metadata display
**Validates: Requirements 8.4**

Property 18: Responsive layout adaptation
*For any* viewport size below tablet breakpoint, the preview modal should adapt its layout for smaller screens
**Validates: Requirements 9.2**

Property 19: Keyboard navigation support
*For any* preview modal, keyboard shortcuts (ESC to close, arrow keys for navigation) should trigger the expected actions
**Validates: Requirements 9.3**

Property 20: Accessibility compliance
*For any* interactive element in the preview modal, appropriate ARIA labels and roles should be present
**Validates: Requirements 9.4**

Property 21: File not found error handling
*For any* preview request for a non-existent file, the system should display an error message indicating the file may have been deleted
**Validates: Requirements 10.1**

Property 22: Conversion failure handling
*For any* Office document that fails to convert, the system should display an error message and offer a download option
**Validates: Requirements 10.2**

Property 23: Service unavailable error handling
*For any* preview request that fails due to backend service unavailability, the system should display a service error message with retry option
**Validates: Requirements 10.3**

Property 24: Corrupted file detection
*For any* corrupted file, the system should detect the corruption and display an error message indicating the file cannot be previewed
**Validates: Requirements 10.4**

## Error Handling

### Frontend Error Handling

The preview system will implement comprehensive error handling at multiple levels:

#### 1. Network Errors
- **Detection**: Catch fetch failures and timeout errors
- **User Feedback**: Display "Network error - please check your connection" message
- **Recovery**: Provide retry button that re-attempts the preview request
- **Logging**: Log error details to console for debugging

#### 2. Permission Errors (403)
- **Detection**: Check HTTP 403 status code from API
- **User Feedback**: Display "You don't have permission to preview this file" message
- **Recovery**: Offer download option if user has download permission
- **Logging**: Log permission denial with file ID and user role

#### 3. File Not Found Errors (404)
- **Detection**: Check HTTP 404 status code from API
- **User Feedback**: Display "File not found - it may have been deleted" message
- **Recovery**: Provide close button and suggest refreshing the file list
- **Logging**: Log missing file ID

#### 4. Unsupported Format Errors
- **Detection**: Check file MIME type against supported formats list
- **User Feedback**: Display "Preview not available for this file type" message
- **Recovery**: Provide download button as alternative
- **Logging**: Log unsupported MIME type

#### 5. Conversion Errors (Office Documents)
- **Detection**: Check for conversion failure response from backend
- **User Feedback**: Display "Unable to convert document for preview" message
- **Recovery**: Provide download button to get original file
- **Logging**: Log conversion failure with file type and error details

#### 6. Large File Warnings
- **Detection**: Check file size before requesting content
- **User Feedback**: Display warning modal with file size and options
- **Recovery**: Offer partial preview (first N lines/pages) or download
- **Logging**: Log large file preview attempts

#### 7. Corrupted File Errors
- **Detection**: Backend detects corruption during read/conversion
- **User Feedback**: Display "File appears to be corrupted" message
- **Recovery**: Suggest contacting administrator or re-uploading
- **Logging**: Log corruption detection with file ID

### Backend Error Handling

#### 1. File Access Validation
```java
// Validate user has permission to access file
if (!filePreviewService.canUserPreviewFile(fileId, currentUser)) {
    throw new AccessDeniedException("User does not have permission to preview this file");
}
```

#### 2. File Existence Validation
```java
// Check if file exists
UploadedFile file = uploadedFileRepository.findById(fileId)
    .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
```

#### 3. File Size Validation
```java
// Check file size before processing
if (file.getFileSize() > MAX_PREVIEW_SIZE) {
    throw new FileTooLargeException("File exceeds maximum preview size of " + MAX_PREVIEW_SIZE);
}
```

#### 4. Format Support Validation
```java
// Check if format is supported
if (!filePreviewService.isPreviewable(file.getMimeType())) {
    throw new UnsupportedFormatException("File type not supported for preview: " + file.getMimeType());
}
```

#### 5. Conversion Error Handling
```java
try {
    byte[] converted = officeConverter.convert(file);
    return converted;
} catch (ConversionException e) {
    log.error("Failed to convert Office document: {}", file.getId(), e);
    throw new PreviewGenerationException("Unable to generate preview for this document");
}
```

#### 6. File Corruption Detection
```java
try {
    // Attempt to read file header
    byte[] header = Files.readAllBytes(Paths.get(file.getFilePath()).subpath(0, 512));
    if (!isValidFileHeader(header, file.getMimeType())) {
        throw new CorruptedFileException("File appears to be corrupted");
    }
} catch (IOException e) {
    throw new CorruptedFileException("Unable to read file - it may be corrupted");
}
```

### Error Response Format

All API errors will follow a consistent format:

```json
{
    "success": false,
    "message": "Human-readable error message",
    "error": "ERROR_CODE",
    "details": {
        "fileId": 123,
        "fileName": "document.pdf",
        "reason": "Additional context"
    },
    "timestamp": "2025-11-25T10:30:00Z"
}
```

## Testing Strategy

### Unit Testing

Unit tests will verify individual components and functions work correctly in isolation:

#### Frontend Unit Tests
1. **FilePreviewModal Tests**
   - Modal opens and closes correctly
   - Loading state displays properly
   - Error state displays with correct message
   - Download button triggers correct action
   - ESC key closes modal

2. **Renderer Tests**
   - PDFRenderer loads PDF correctly
   - CodeRenderer applies syntax highlighting
   - TextRenderer preserves formatting
   - OfficeRenderer handles conversion response

3. **FilePreviewButton Tests**
   - Button renders for supported formats
   - Button hidden for unsupported formats
   - Tooltip displays correct message
   - Click triggers preview modal

#### Backend Unit Tests
1. **FilePreviewController Tests**
   - Endpoints return correct status codes
   - Authentication required for all endpoints
   - Error responses follow correct format

2. **FilePreviewService Tests**
   - Permission validation works correctly
   - File type detection is accurate
   - Conversion logic handles errors gracefully
   - Large file detection works correctly

3. **Permission Tests**
   - Professor can preview own files
   - Dean can preview all files
   - HOD can preview department files only
   - HOD cannot preview other department files

### Property-Based Testing

Property-based tests will verify universal properties hold across all inputs:

#### Frontend Property Tests

1. **Property Test: Modal Lifecycle**
   - Generate random file IDs and types
   - Open preview for each
   - Verify modal opens, displays content, and closes correctly
   - Test with ESC key and click-outside

2. **Property Test: Renderer Selection**
   - Generate random file types (supported and unsupported)
   - Verify correct renderer is selected for each type
   - Verify unsupported types show error message

3. **Property Test: Metadata Display**
   - Generate random file metadata
   - Verify all required fields are displayed
   - Verify role-specific fields appear for appropriate roles

4. **Property Test: Error Handling**
   - Generate random error conditions (404, 403, 500, network error)
   - Verify appropriate error message is displayed
   - Verify retry/download options are available

#### Backend Property Tests

1. **Property Test: Permission Validation**
   - Generate random user-file combinations
   - Verify permission checks return correct results
   - Verify unauthorized access is blocked

2. **Property Test: File Type Detection**
   - Generate files with various extensions and MIME types
   - Verify correct type detection
   - Verify previewable flag is set correctly

3. **Property Test: Large File Handling**
   - Generate files of various sizes
   - Verify files over threshold are flagged
   - Verify appropriate response is returned

### Integration Testing

Integration tests will verify components work together correctly:

1. **End-to-End Preview Flow**
   - User clicks preview button
   - Modal opens with loading state
   - Content loads and displays
   - User can download or close

2. **Cross-Dashboard Compatibility**
   - Test preview from Professor dashboard
   - Test preview from Dean dashboard
   - Test preview from HOD dashboard
   - Verify role-specific behavior

3. **Error Recovery Flow**
   - Trigger network error
   - Verify error message displays
   - Click retry button
   - Verify preview loads successfully

4. **Format-Specific Flows**
   - Test PDF preview end-to-end
   - Test Office document preview end-to-end
   - Test code file preview end-to-end
   - Test text file preview end-to-end

### Manual Testing Checklist

1. **Visual Testing**
   - Modal appears centered and properly sized
   - Loading spinner is visible and animated
   - Content renders with correct formatting
   - Buttons are properly styled and positioned

2. **Responsive Testing**
   - Test on desktop (1920x1080)
   - Test on tablet (768x1024)
   - Test on mobile (375x667)
   - Verify layout adapts appropriately

3. **Accessibility Testing**
   - Test with screen reader
   - Test keyboard navigation
   - Verify ARIA labels are present
   - Check color contrast ratios

4. **Browser Compatibility**
   - Test on Chrome
   - Test on Firefox
   - Test on Safari
   - Test on Edge

5. **Performance Testing**
   - Test with small files (<1MB)
   - Test with medium files (1-5MB)
   - Test with large files (>5MB)
   - Verify loading times are acceptable

## Implementation Notes

### Third-Party Libraries

#### Frontend
- **PDF.js**: For PDF rendering (if browser native viewer is insufficient)
- **Highlight.js** or **Prism.js**: For syntax highlighting in code files
- **Virtual Scroller**: For efficient rendering of large text files

#### Backend
- **Apache POI**: For reading Office documents (optional, for conversion)
- **Apache PDFBox**: For PDF manipulation (optional, for page extraction)
- **Tika**: For MIME type detection and content extraction

### Performance Considerations

1. **Lazy Loading**: Load preview content only when modal opens
2. **Caching**: Cache converted Office documents to avoid repeated conversion
3. **Streaming**: Stream large files instead of loading entirely into memory
4. **Compression**: Compress text content before sending to frontend
5. **Pagination**: Load multi-page documents page-by-page on demand

### Security Considerations

1. **Authentication**: All preview endpoints require authentication
2. **Authorization**: Verify user has permission to access file before serving content
3. **Path Traversal**: Validate file paths to prevent directory traversal attacks
4. **Content Type**: Set correct Content-Type headers to prevent XSS
5. **File Size Limits**: Enforce maximum file size for preview to prevent DoS
6. **Rate Limiting**: Limit preview requests per user to prevent abuse

### Accessibility Considerations

1. **ARIA Labels**: Add descriptive labels to all interactive elements
2. **Keyboard Navigation**: Support Tab, Enter, ESC, and arrow keys
3. **Focus Management**: Trap focus within modal when open
4. **Screen Reader**: Announce modal state changes
5. **Color Contrast**: Ensure text meets WCAG AA standards
6. **Alternative Text**: Provide text alternatives for icons

### Browser Compatibility

The preview system will support:
- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

Fallbacks will be provided for:
- PDF rendering: Use browser native viewer if PDF.js not supported
- Syntax highlighting: Show plain text if highlighting library fails
- Virtual scrolling: Use standard scrolling if virtual scroller not supported

## Migration Strategy

### Phase 1: Core Preview Infrastructure
1. Implement FilePreviewModal component
2. Add backend preview endpoints
3. Implement basic text and PDF rendering
4. Add preview buttons to file explorer

### Phase 2: Format Support Expansion
1. Add Office document conversion
2. Implement code syntax highlighting
3. Add image preview support
4. Implement format detection logic

### Phase 3: Enhanced Features
1. Add search functionality
2. Implement virtual scrolling for large files
3. Add pagination for multi-page documents
4. Implement preview caching

### Phase 4: Polish and Optimization
1. Add loading animations
2. Optimize performance for large files
3. Improve error messages
4. Add accessibility features

### Rollout Plan

1. **Development Environment**: Test with sample files
2. **Staging Environment**: Test with production-like data
3. **Beta Release**: Enable for select users (Dean role first)
4. **Full Release**: Enable for all users across all dashboards

### Rollback Plan

If issues are discovered:
1. Disable preview buttons via feature flag
2. Keep download functionality working
3. Fix issues in development
4. Re-enable after verification

## Future Enhancements

1. **Video Preview**: Add support for video files (mp4, webm)
2. **Audio Preview**: Add support for audio files (mp3, wav)
3. **Archive Preview**: Show contents of ZIP files
4. **Collaborative Annotations**: Allow users to add comments to previews
5. **Version Comparison**: Compare two versions of a document side-by-side
6. **Print from Preview**: Add print button to preview modal
7. **Full-Screen Mode**: Allow preview to expand to full screen
8. **Thumbnail Generation**: Generate thumbnails for quick preview
