# Design Document

## Overview

This design document outlines the architecture and implementation approach for fixing the broken file upload functionality in the Deanship Archiving System and establishing a robust physical file storage system. The solution addresses three critical issues:

1. **Broken Upload Flow**: The "Upload" button in the Professor dashboard does nothing
2. **Missing Physical Storage**: Files need to be stored on disk matching the folder structure
3. **Incomplete Backend**: Upload endpoint needs to be implemented or fixed

The system uses Spring Boot for the backend with existing Folder entities and FolderService, and vanilla JavaScript for the frontend with FileExplorerState for state management.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend Layer                           │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         Professor Dashboard (prof.js)                │  │
│  │                                                       │  │
│  │  ┌────────────────────────────────────────────────┐ │  │
│  │  │  Upload Modal (Upload Lecture Notes)          │ │  │
│  │  │  - File input (multiple)                      │ │  │
│  │  │  - Notes textarea                             │ │  │
│  │  │  - Upload button                              │ │  │
│  │  └────────────────────────────────────────────────┘ │  │
│  │                                                       │  │
│  │  ┌────────────────────────────────────────────────┐ │  │
│  │  │  FileExplorerState                            │ │  │
│  │  │  - currentNode (selected folder)              │ │  │
│  │  │  - folderId                                    │ │  │
│  │  └────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │   API Layer     │
                    │  (fetch/AJAX)   │
                    └────────┬────────┘
                             │
┌─────────────────────────────────────────────────────────────┐
│                    Backend Layer                             │
│                             │                                │
│                    ┌────────▼────────┐                       │
│                    │ FileUpload      │                       │
│                    │ Controller      │                       │
│                    │ (NEW/FIXED)     │                       │
│                    └────────┬────────┘                       │
│                             │                                │
│                    ┌────────▼────────┐                       │
│                    │ FileUpload      │                       │
│                    │ Service         │                       │
│                    │ (NEW)           │                       │
│                    └────────┬────────┘                       │
│                             │                                │
│         ┌───────────────────┼───────────────────┐           │
│         │                   │                   │           │
│  ┌──────▼───────┐  ┌────────▼────────┐  ┌──────▼───────┐  │
│  │  Folder      │  │  UploadedFile   │  │  File System │  │
│  │  Service     │  │  Repository     │  │  (Physical)  │  │
│  │  (Existing)  │  │  (NEW)          │  │              │  │
│  └──────────────┘  └─────────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────┘
```


### Component Interaction Flow

**File Upload Flow:**
```
Professor Dashboard → Select Folder (Course Notes)
                      ↓
                   Click Upload Button
                      ↓
                   Open Upload Modal
                      ↓
                   Select Files + Enter Notes
                      ↓
                   Click Upload
                      ↓
              Construct FormData (files, notes, folderId)
                      ↓
              POST /api/professor/files/upload
                      ↓
              FileUploadController.uploadFiles()
                      ↓
              FileUploadService.uploadFiles()
                      ↓
         ┌────────────┴────────────┐
         │                         │
    Validate Files          Get Folder from DB
         │                         │
    Check Size/Type         Get Physical Path
         │                         │
         └────────────┬────────────┘
                      ↓
              Create Physical Directory
                      ↓
              Save Files to Disk
                      ↓
              Create UploadedFile Entities
                      ↓
              Save to Database
                      ↓
              Return Success Response
                      ↓
              Frontend: Close Modal
                      ↓
              Refresh File List
                      ↓
              Show Success Toast
```

## Components and Interfaces

### Backend Components

#### 1. Physical Storage Configuration

**Base Storage Directory**: 
- Production: `/data/archive` or configurable via `application.properties`
- Development: `uploads/` (relative to project root)
- Configured via: `file.upload-dir` property

**Directory Structure Convention**:
```
{base}/
  └── {yearCode}/                    # e.g., "2024-2025"
      └── {semesterType}/            # e.g., "first", "second"
          └── {professorId}/         # e.g., "PROF123"
              └── {courseCode} - {courseName}/  # e.g., "CS101 - Intro to Programming"
                  ├── Syllabus/
                  ├── Exams/
                  ├── Course Notes/
                  └── Assignments/
```

**Example Physical Path**:
```
/data/archive/2024-2025/first/PROF123/CS101 - Intro to Programming/Course Notes/lecture1.pdf
```

#### 2. FileUploadService (NEW)

**Purpose**: Handle file upload operations including validation, storage, and metadata persistence.

**Interface**:
```java
public interface FileUploadService {
    /**
     * Upload multiple files to a specific folder
     * 
     * @param files Array of uploaded files
     * @param folderId Target folder ID
     * @param notes Optional notes about the upload
     * @param uploaderId User ID of the uploader
     * @return List of created UploadedFile entities
     * @throws FolderNotFoundException if folder doesn't exist
     * @throws UnauthorizedException if user doesn't have permission
     * @throws FileValidationException if files fail validation
     */
    List<UploadedFile> uploadFiles(MultipartFile[] files, Long folderId, 
                                   String notes, Long uploaderId);
    
    /**
     * Validate a single file
     * 
     * @param file File to validate
     * @throws FileValidationException if validation fails
     */
    void validateFile(MultipartFile file);
    
    /**
     * Generate safe filename (sanitize and handle duplicates)
     * 
     * @param originalFilename Original filename
     * @param targetPath Target directory path
     * @return Safe filename
     */
    String generateSafeFilename(String originalFilename, Path targetPath);
    
    /**
     * Check if user has permission to upload to folder
     * 
     * @param folder Target folder
     * @param user Uploader user
     * @return true if authorized
     */
    boolean canUploadToFolder(Folder folder, User user);
}
```


**Implementation Details**:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {
    
    private final FolderRepository folderRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final UserRepository userRepository;
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    
    @Value("${file.max-size:52428800}") // 50MB default
    private long maxFileSize;
    
    @Value("${file.allowed-types:pdf,doc,docx,ppt,pptx,xls,xlsx,jpg,jpeg,png,gif}")
    private String allowedTypes;
    
    @Override
    @Transactional
    public List<UploadedFile> uploadFiles(MultipartFile[] files, Long folderId, 
                                         String notes, Long uploaderId) {
        // 1. Validate inputs
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No files provided");
        }
        
        // 2. Get folder and validate existence
        Folder folder = folderRepository.findById(folderId)
            .orElseThrow(() -> new FolderNotFoundException("Folder not found: " + folderId));
        
        // 3. Get uploader and validate authorization
        User uploader = userRepository.findById(uploaderId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + uploaderId));
        
        if (!canUploadToFolder(folder, uploader)) {
            throw new UnauthorizedException("User not authorized to upload to this folder");
        }
        
        // 4. Validate all files first
        for (MultipartFile file : files) {
            validateFile(file);
        }
        
        // 5. Prepare physical directory
        Path targetDir = Paths.get(uploadDir, folder.getPath());
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw new FileStorageException("Failed to create directory: " + targetDir, e);
        }
        
        // 6. Upload files and create entities
        List<UploadedFile> uploadedFiles = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                // Generate safe filename
                String safeFilename = generateSafeFilename(file.getOriginalFilename(), targetDir);
                Path targetPath = targetDir.resolve(safeFilename);
                
                // Save file to disk
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Create database entity
                UploadedFile uploadedFile = new UploadedFile();
                uploadedFile.setFolder(folder);
                uploadedFile.setOriginalFilename(file.getOriginalFilename());
                uploadedFile.setStoredFilename(safeFilename);
                uploadedFile.setFileUrl(folder.getPath() + "/" + safeFilename);
                uploadedFile.setFileSize(file.getSize());
                uploadedFile.setFileType(file.getContentType());
                uploadedFile.setUploader(uploader);
                uploadedFile.setNotes(notes);
                
                uploadedFile = uploadedFileRepository.save(uploadedFile);
                uploadedFiles.add(uploadedFile);
                
                log.info("Uploaded file: {} to {}", safeFilename, folder.getPath());
                
            } catch (IOException e) {
                log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                throw new FileStorageException("Failed to upload file: " + file.getOriginalFilename(), e);
            }
        }
        
        return uploadedFiles;
    }
    
    @Override
    public void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new FileValidationException("File is empty");
        }
        
        // Check file size
        if (file.getSize() > maxFileSize) {
            throw new FileValidationException(
                String.format("File size exceeds maximum allowed size of %d bytes", maxFileSize));
        }
        
        // Check file type
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new FileValidationException("Filename is empty");
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        List<String> allowedExtensions = Arrays.asList(allowedTypes.split(","));
        
        if (!allowedExtensions.contains(extension)) {
            throw new FileValidationException(
                String.format("File type '%s' is not allowed. Allowed types: %s", 
                             extension, allowedTypes));
        }
    }
    
    @Override
    public String generateSafeFilename(String originalFilename, Path targetPath) {
        // Sanitize filename
        String sanitized = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Handle duplicates by appending number
        Path targetFile = targetPath.resolve(sanitized);
        if (!Files.exists(targetFile)) {
            return sanitized;
        }
        
        // File exists, append number
        String nameWithoutExt = getFilenameWithoutExtension(sanitized);
        String extension = getFileExtension(sanitized);
        
        int counter = 1;
        while (Files.exists(targetPath.resolve(nameWithoutExt + "(" + counter + ")." + extension))) {
            counter++;
        }
        
        return nameWithoutExt + "(" + counter + ")." + extension;
    }
    
    @Override
    public boolean canUploadToFolder(Folder folder, User user) {
        // Admins and Deans can upload anywhere
        if (user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_DEANSHIP) {
            return true;
        }
        
        // Professors can only upload to their own folders
        if (user.getRole() == Role.ROLE_PROFESSOR) {
            return folder.getOwner().getId().equals(user.getId());
        }
        
        return false;
    }
    
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }
    
    private String getFilenameWithoutExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(0, lastDot) : filename;
    }
}
```


#### 3. FileUploadController (NEW/FIXED)

**Purpose**: REST endpoint for file uploads from professor dashboard.

**Endpoint**: `POST /api/professor/files/upload`

**Request Format**:
- Content-Type: `multipart/form-data`
- Parameters:
  - `files[]`: Array of files (MultipartFile[])
  - `folderId`: Target folder ID (Long)
  - `notes`: Optional notes (String)

**Response Format**:
```json
{
  "success": true,
  "message": "3 files uploaded successfully",
  "data": [
    {
      "id": 123,
      "originalFilename": "lecture1.pdf",
      "storedFilename": "lecture1.pdf",
      "fileSize": 1048576,
      "fileType": "application/pdf",
      "uploadedAt": "2025-11-21T10:30:00"
    }
  ]
}
```

**Error Response**:
```json
{
  "success": false,
  "message": "File validation failed",
  "error": "File size exceeds maximum allowed size of 52428800 bytes"
}
```

**Implementation**:
```java
@RestController
@RequestMapping("/api/professor/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {
    
    private final FileUploadService fileUploadService;
    
    @PostMapping("/upload")
    @PreAuthorize("hasRole('PROFESSOR') or hasRole('DEANSHIP') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UploadedFileDTO>>> uploadFiles(
            @RequestParam("files[]") MultipartFile[] files,
            @RequestParam("folderId") Long folderId,
            @RequestParam(value = "notes", required = false) String notes,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            log.info("Upload request: {} files to folder {}", files.length, folderId);
            
            // Get current user ID
            Long uploaderId = getCurrentUserId(userDetails);
            
            // Upload files
            List<UploadedFile> uploadedFiles = fileUploadService.uploadFiles(
                files, folderId, notes, uploaderId);
            
            // Convert to DTOs
            List<UploadedFileDTO> dtos = uploadedFiles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            
            String message = String.format("%d file(s) uploaded successfully", uploadedFiles.size());
            
            return ResponseEntity.ok(ApiResponse.success(message, dtos));
            
        } catch (FolderNotFoundException e) {
            log.error("Folder not found: {}", folderId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Folder not found", e.getMessage()));
                
        } catch (UnauthorizedException e) {
            log.error("Unauthorized upload attempt to folder: {}", folderId, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Unauthorized", e.getMessage()));
                
        } catch (FileValidationException e) {
            log.error("File validation failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("File validation failed", e.getMessage()));
                
        } catch (FileStorageException e) {
            log.error("File storage failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("File storage failed", e.getMessage()));
                
        } catch (Exception e) {
            log.error("Unexpected error during file upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Upload failed", "An unexpected error occurred"));
        }
    }
    
    private Long getCurrentUserId(UserDetails userDetails) {
        // Implementation depends on UserDetails structure
        // Assuming username is the user ID or email
        return userRepository.findByUsername(userDetails.getUsername())
            .map(User::getId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
    
    private UploadedFileDTO convertToDTO(UploadedFile file) {
        return UploadedFileDTO.builder()
            .id(file.getId())
            .originalFilename(file.getOriginalFilename())
            .storedFilename(file.getStoredFilename())
            .fileSize(file.getFileSize())
            .fileType(file.getFileType())
            .uploadedAt(file.getCreatedAt())
            .build();
    }
}
```


#### 4. Enhanced UploadedFile Entity

**Modifications Needed**:
```java
@Entity
@Table(name = "uploaded_files", indexes = {
    @Index(name = "idx_uploaded_files_folder", columnList = "folder_id"),
    @Index(name = "idx_uploaded_files_uploader", columnList = "uploader_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Folder where this file is stored
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;
    
    /**
     * Original filename as uploaded by user
     */
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;
    
    /**
     * Sanitized filename stored on disk
     */
    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;
    
    /**
     * Full file URL/path: {folderPath}/{storedFilename}
     */
    @Column(name = "file_url", nullable = false)
    private String fileUrl;
    
    /**
     * File size in bytes
     */
    @Column(name = "file_size")
    private Long fileSize;
    
    /**
     * MIME type
     */
    @Column(name = "file_type")
    private String fileType;
    
    /**
     * User who uploaded the file
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;
    
    /**
     * Optional notes about the file
     */
    @Column(length = 1000)
    private String notes;
    
    /**
     * Upload timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

#### 5. UploadedFileRepository (NEW)

```java
@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    
    /**
     * Find all files in a specific folder
     */
    List<UploadedFile> findByFolderId(Long folderId);
    
    /**
     * Find all files uploaded by a specific user
     */
    List<UploadedFile> findByUploaderId(Long uploaderId);
    
    /**
     * Find file by folder and stored filename
     */
    Optional<UploadedFile> findByFolderIdAndStoredFilename(Long folderId, String storedFilename);
    
    /**
     * Count files in a folder
     */
    long countByFolderId(Long folderId);
}
```

### Frontend Components

#### 1. Upload Modal Structure

**HTML Structure** (to be added to professor dashboard):
```html
<!-- Upload Modal -->
<div id="uploadModal" class="modal hidden">
    <div class="modal-content">
        <div class="modal-header">
            <h3 id="uploadModalTitle">Upload Lecture Notes</h3>
            <button class="close-btn" onclick="closeUploadModal()">&times;</button>
        </div>
        <div class="modal-body">
            <div class="form-group">
                <label for="uploadFiles">Select Files</label>
                <input type="file" id="uploadFiles" multiple accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.jpg,.jpeg,.png,.gif">
                <small class="text-muted">Max 50MB per file. Allowed: PDF, DOC, DOCX, PPT, PPTX, XLS, XLSX, images</small>
            </div>
            <div class="form-group">
                <label for="uploadNotes">Notes (Optional)</label>
                <textarea id="uploadNotes" rows="3" placeholder="Add any notes about these files..."></textarea>
            </div>
            <div id="uploadError" class="error-message hidden"></div>
        </div>
        <div class="modal-footer">
            <button class="btn btn-secondary" onclick="closeUploadModal()">Cancel</button>
            <button id="uploadBtn" class="btn btn-primary" onclick="handleUpload()">Upload</button>
        </div>
    </div>
</div>
```


#### 2. Upload Modal JavaScript

**Implementation in prof.js**:
```javascript
/**
 * Open upload modal for the currently selected folder
 */
function openUploadModal() {
    const currentNode = fileExplorerState.getState().currentNode;
    
    if (!currentNode || !currentNode.id) {
        showToast('Please select a folder first', 'error');
        return;
    }
    
    // Only allow uploads to category folders (Syllabus, Exams, Course Notes, Assignments)
    if (currentNode.type !== 'SUBFOLDER') {
        showToast('Please select a category folder (Syllabus, Exams, Course Notes, or Assignments)', 'error');
        return;
    }
    
    // Set modal title based on folder name
    const modalTitle = document.getElementById('uploadModalTitle');
    modalTitle.textContent = `Upload to ${currentNode.name}`;
    
    // Clear previous inputs
    document.getElementById('uploadFiles').value = '';
    document.getElementById('uploadNotes').value = '';
    document.getElementById('uploadError').classList.add('hidden');
    
    // Show modal
    document.getElementById('uploadModal').classList.remove('hidden');
}

/**
 * Close upload modal
 */
function closeUploadModal() {
    document.getElementById('uploadModal').classList.add('hidden');
}

/**
 * Handle file upload
 */
async function handleUpload() {
    const uploadBtn = document.getElementById('uploadBtn');
    const errorDiv = document.getElementById('uploadError');
    const filesInput = document.getElementById('uploadFiles');
    const notesInput = document.getElementById('uploadNotes');
    
    // Get current folder
    const currentNode = fileExplorerState.getState().currentNode;
    
    if (!currentNode || !currentNode.id) {
        showError(errorDiv, 'No folder selected');
        return;
    }
    
    // Get selected files
    const files = filesInput.files;
    
    if (files.length === 0) {
        showError(errorDiv, 'Please select at least one file');
        return;
    }
    
    // Validate file sizes
    const maxSize = 50 * 1024 * 1024; // 50MB
    for (let file of files) {
        if (file.size > maxSize) {
            showError(errorDiv, `File "${file.name}" exceeds maximum size of 50MB`);
            return;
        }
    }
    
    // Prepare FormData
    const formData = new FormData();
    
    for (let file of files) {
        formData.append('files[]', file);
    }
    
    formData.append('folderId', currentNode.id);
    
    const notes = notesInput.value.trim();
    if (notes) {
        formData.append('notes', notes);
    }
    
    // Show loading state
    uploadBtn.disabled = true;
    uploadBtn.textContent = 'Uploading...';
    errorDiv.classList.add('hidden');
    
    try {
        const response = await fetch('/api/professor/files/upload', {
            method: 'POST',
            body: formData,
            headers: {
                // Don't set Content-Type - browser will set it with boundary
            }
        });
        
        const result = await response.json();
        
        if (response.ok && result.success) {
            // Success
            showToast(`${files.length} file(s) uploaded successfully`, 'success');
            
            // Close modal
            closeUploadModal();
            
            // Refresh file list for current folder
            await refreshCurrentFolderFiles();
            
        } else {
            // Error from server
            showError(errorDiv, result.error || result.message || 'Upload failed');
        }
        
    } catch (error) {
        console.error('Upload error:', error);
        showError(errorDiv, 'Network error. Please try again.');
        
    } finally {
        // Reset button state
        uploadBtn.disabled = false;
        uploadBtn.textContent = 'Upload';
    }
}

/**
 * Refresh files for the currently selected folder
 */
async function refreshCurrentFolderFiles() {
    const currentNode = fileExplorerState.getState().currentNode;
    
    if (!currentNode || !currentNode.id) {
        return;
    }
    
    try {
        // Set loading state
        fileExplorerState.setFileListLoading(true);
        
        // Fetch updated file list
        const response = await fetch(`/api/file-explorer/node?path=${encodeURIComponent(currentNode.path)}`);
        const result = await response.json();
        
        if (response.ok && result.success) {
            // Update current node with new data
            fileExplorerState.setCurrentNode(result.data, currentNode.path);
        }
        
    } catch (error) {
        console.error('Error refreshing files:', error);
        
    } finally {
        fileExplorerState.setFileListLoading(false);
    }
}

/**
 * Show error message in modal
 */
function showError(errorDiv, message) {
    errorDiv.textContent = message;
    errorDiv.classList.remove('hidden');
}
```


#### 3. File Explorer Integration

**Add Upload Button to File Explorer**:
```javascript
/**
 * Render file list with upload button
 */
function renderFileList(node) {
    const container = document.getElementById('fileExplorerFileList');
    
    if (!node) {
        container.innerHTML = '<p class="text-gray-500">Select a folder to view files</p>';
        return;
    }
    
    let html = '';
    
    // Add upload button for category folders
    if (node.type === 'SUBFOLDER') {
        html += `
            <div class="mb-4">
                <button onclick="openUploadModal()" class="btn btn-primary">
                    <i class="fas fa-upload"></i> Upload Files
                </button>
            </div>
        `;
    }
    
    // Render files
    if (node.files && node.files.length > 0) {
        html += '<div class="file-grid">';
        
        for (const file of node.files) {
            html += renderFileCard(file);
        }
        
        html += '</div>';
    } else {
        html += '<p class="text-gray-500">No files in this folder</p>';
    }
    
    container.innerHTML = html;
}

/**
 * Render individual file card
 */
function renderFileCard(file) {
    const icon = getFileIcon(file.fileType);
    const size = formatFileSize(file.fileSize);
    const date = formatDate(file.uploadedAt);
    
    return `
        <div class="file-card">
            <div class="file-icon">
                <i class="${icon}"></i>
            </div>
            <div class="file-info">
                <h4 class="file-name">${escapeHtml(file.originalFilename)}</h4>
                <p class="file-meta">${size} • ${date}</p>
                ${file.notes ? `<p class="file-notes">${escapeHtml(file.notes)}</p>` : ''}
            </div>
            <div class="file-actions">
                <button onclick="downloadFile(${file.id})" class="btn-icon" title="Download">
                    <i class="fas fa-download"></i>
                </button>
                <button onclick="deleteFile(${file.id})" class="btn-icon" title="Delete">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        </div>
    `;
}

/**
 * Get appropriate icon for file type
 */
function getFileIcon(fileType) {
    if (!fileType) return 'fas fa-file';
    
    if (fileType.includes('pdf')) return 'fas fa-file-pdf text-red-500';
    if (fileType.includes('word') || fileType.includes('document')) return 'fas fa-file-word text-blue-500';
    if (fileType.includes('powerpoint') || fileType.includes('presentation')) return 'fas fa-file-powerpoint text-orange-500';
    if (fileType.includes('excel') || fileType.includes('spreadsheet')) return 'fas fa-file-excel text-green-500';
    if (fileType.includes('image')) return 'fas fa-file-image text-purple-500';
    
    return 'fas fa-file';
}

/**
 * Format file size for display
 */
function formatFileSize(bytes) {
    if (!bytes) return '0 B';
    
    const units = ['B', 'KB', 'MB', 'GB'];
    let size = bytes;
    let unitIndex = 0;
    
    while (size >= 1024 && unitIndex < units.length - 1) {
        size /= 1024;
        unitIndex++;
    }
    
    return `${size.toFixed(1)} ${units[unitIndex]}`;
}

/**
 * Format date for display
 */
function formatDate(dateString) {
    if (!dateString) return '';
    
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
```

## Data Models

### Physical Storage Path Mapping

**Database Folder Path** → **Physical File System Path**

| Folder.path (DB) | Physical Path |
|------------------|---------------|
| `2024-2025/first/PROF123` | `/data/archive/2024-2025/first/PROF123/` |
| `2024-2025/first/PROF123/CS101 - Intro` | `/data/archive/2024-2025/first/PROF123/CS101 - Intro/` |
| `2024-2025/first/PROF123/CS101 - Intro/Course Notes` | `/data/archive/2024-2025/first/PROF123/CS101 - Intro/Course Notes/` |

**File Storage**:
- UploadedFile.fileUrl: `2024-2025/first/PROF123/CS101 - Intro/Course Notes/lecture1.pdf`
- Physical location: `/data/archive/2024-2025/first/PROF123/CS101 - Intro/Course Notes/lecture1.pdf`

### File Validation Rules

**Maximum File Size**: 50MB (configurable via `file.max-size`)

**Allowed File Types** (configurable via `file.allowed-types`):
- Documents: PDF, DOC, DOCX
- Presentations: PPT, PPTX
- Spreadsheets: XLS, XLSX
- Images: JPG, JPEG, PNG, GIF

**Filename Sanitization**:
- Remove special characters except: `.`, `_`, `-`
- Replace spaces and other characters with `_`
- Preserve file extension

**Duplicate Handling**:
- Policy: Rename with counter
- Example: `lecture1.pdf` → `lecture1(1).pdf` → `lecture1(2).pdf`


## Error Handling

### Backend Error Handling

**Exception Hierarchy**:
```java
// Base exception
public class FileUploadException extends RuntimeException {
    public FileUploadException(String message) {
        super(message);
    }
    
    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Specific exceptions
public class FileValidationException extends FileUploadException {
    public FileValidationException(String message) {
        super(message);
    }
}

public class FileStorageException extends FileUploadException {
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class FolderNotFoundException extends FileUploadException {
    public FolderNotFoundException(String message) {
        super(message);
    }
}

public class UnauthorizedException extends FileUploadException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
```

**Error Response Format**:
```json
{
  "success": false,
  "message": "User-friendly error message",
  "error": "Detailed technical error message",
  "timestamp": "2025-11-21T10:30:00"
}
```

**HTTP Status Codes**:
- 200 OK: Upload successful
- 400 Bad Request: Validation failed (file size, type, etc.)
- 403 Forbidden: Authorization failed
- 404 Not Found: Folder not found
- 500 Internal Server Error: Storage or unexpected errors

### Frontend Error Handling

**Error Display Strategy**:
1. **Validation Errors**: Show in modal error div (keep modal open)
2. **Network Errors**: Show in modal error div with retry option
3. **Success**: Close modal and show toast notification

**Error Messages**:
```javascript
const ERROR_MESSAGES = {
    NO_FILES: 'Please select at least one file',
    FILE_TOO_LARGE: 'File "{filename}" exceeds maximum size of 50MB',
    INVALID_TYPE: 'File type "{type}" is not allowed',
    NO_FOLDER: 'Please select a folder first',
    WRONG_FOLDER_TYPE: 'Please select a category folder (Syllabus, Exams, Course Notes, or Assignments)',
    NETWORK_ERROR: 'Network error. Please check your connection and try again.',
    UNAUTHORIZED: 'You do not have permission to upload to this folder',
    FOLDER_NOT_FOUND: 'The selected folder no longer exists',
    UPLOAD_FAILED: 'Upload failed. Please try again.'
};
```

**Loading States**:
```javascript
// Before upload
uploadBtn.disabled = true;
uploadBtn.textContent = 'Uploading...';
uploadBtn.classList.add('loading');

// After upload (success or error)
uploadBtn.disabled = false;
uploadBtn.textContent = 'Upload';
uploadBtn.classList.remove('loading');
```

## Testing Strategy

### Backend Testing

#### Unit Tests

**FileUploadService Tests**:
```java
@Test
void testUploadFiles_Success() {
    // Test successful file upload
}

@Test
void testUploadFiles_EmptyFiles() {
    // Test error when no files provided
}

@Test
void testUploadFiles_FolderNotFound() {
    // Test error when folder doesn't exist
}

@Test
void testUploadFiles_Unauthorized() {
    // Test error when user doesn't have permission
}

@Test
void testValidateFile_FileTooLarge() {
    // Test validation fails for large files
}

@Test
void testValidateFile_InvalidType() {
    // Test validation fails for disallowed file types
}

@Test
void testGenerateSafeFilename_Sanitization() {
    // Test filename sanitization
}

@Test
void testGenerateSafeFilename_DuplicateHandling() {
    // Test duplicate filename handling
}

@Test
void testCanUploadToFolder_Professor() {
    // Test professor can upload to own folder
}

@Test
void testCanUploadToFolder_WrongProfessor() {
    // Test professor cannot upload to another's folder
}

@Test
void testCanUploadToFolder_Dean() {
    // Test dean can upload anywhere
}
```

#### Integration Tests

**FileUploadController Tests**:
```java
@Test
@WithMockUser(roles = "PROFESSOR")
void testUploadEndpoint_Success() {
    // Test successful upload via REST endpoint
}

@Test
@WithMockUser(roles = "PROFESSOR")
void testUploadEndpoint_ValidationError() {
    // Test validation error response
}

@Test
@WithMockUser(roles = "PROFESSOR")
void testUploadEndpoint_Unauthorized() {
    // Test unauthorized access
}

@Test
void testUploadEndpoint_NoAuthentication() {
    // Test unauthenticated access is rejected
}
```

**End-to-End Tests**:
```java
@Test
@Transactional
void testCompleteUploadFlow() {
    // 1. Create professor, course, assignment
    // 2. Create folder structure
    // 3. Upload file via service
    // 4. Verify file exists in database
    // 5. Verify file exists on disk
    // 6. Verify file appears in File Explorer API
}

@Test
@Transactional
void testUploadAndRetrieve() {
    // 1. Upload file
    // 2. Retrieve file list for folder
    // 3. Verify uploaded file is in list
}
```

### Frontend Testing

#### Manual Testing Checklist

**Upload Modal**:
- [ ] Click upload button opens modal
- [ ] Modal title shows correct folder name
- [ ] File input accepts multiple files
- [ ] Notes textarea is optional
- [ ] Cancel button closes modal without uploading
- [ ] Close (X) button closes modal without uploading

**File Selection**:
- [ ] Can select single file
- [ ] Can select multiple files
- [ ] File input shows selected file count
- [ ] Can clear selection and select different files

**Upload Process**:
- [ ] Upload button disabled during upload
- [ ] Upload button shows "Uploading..." text
- [ ] Progress indication visible
- [ ] Cannot close modal during upload

**Success Flow**:
- [ ] Modal closes on successful upload
- [ ] Success toast appears
- [ ] File list refreshes automatically
- [ ] Uploaded files appear in list
- [ ] File metadata (name, size, date) displays correctly

**Error Handling**:
- [ ] Error for no files selected
- [ ] Error for file too large (>50MB)
- [ ] Error for invalid file type
- [ ] Error for no folder selected
- [ ] Error for wrong folder type
- [ ] Error message displays in modal
- [ ] Modal stays open on error
- [ ] Can retry after error

**Cross-Category Upload**:
- [ ] Upload to Course Notes works
- [ ] Upload to Exams works
- [ ] Upload to Syllabus works
- [ ] Upload to Assignments works
- [ ] Modal title updates for each category

**Authorization**:
- [ ] Professor can upload to own folders
- [ ] Professor cannot upload to other's folders
- [ ] Dean can upload to any folder

#### Browser Testing

Test in:
- Chrome (latest)
- Firefox (latest)
- Edge (latest)
- Safari (latest)

**Focus Areas**:
- File input behavior
- FormData submission
- Progress indication
- Error display
- Modal behavior

### Performance Testing

**Metrics to Monitor**:
- Upload time for 1MB file (should be < 2s)
- Upload time for 10MB file (should be < 10s)
- Upload time for 50MB file (should be < 60s)
- Multiple file upload (5 files, 5MB each) (should be < 30s)

**Load Testing**:
- 10 concurrent uploads
- 50 concurrent uploads
- Verify no file corruption
- Verify no database deadlocks

## Implementation Notes

### Phase 1: Backend Foundation
1. Create FileUploadService interface and implementation
2. Create FileUploadController with upload endpoint
3. Enhance UploadedFile entity with folder relationship
4. Create UploadedFileRepository
5. Write unit tests for service
6. Write integration tests for controller

### Phase 2: Frontend Upload Modal
1. Add upload modal HTML to professor dashboard
2. Implement openUploadModal() function
3. Implement handleUpload() function
4. Implement refreshCurrentFolderFiles() function
5. Add error handling and loading states

### Phase 3: File Explorer Integration
1. Add upload button to file list view
2. Implement file card rendering
3. Add file download functionality
4. Add file delete functionality
5. Test file list refresh after upload

### Phase 4: Testing & Validation
1. Complete manual testing checklist
2. Perform browser compatibility testing
3. Perform performance testing
4. Fix any identified issues

### Phase 5: Documentation
1. Update API documentation
2. Update user guide
3. Create deployment guide for storage configuration
4. Document file validation rules

## Security Considerations

**File Upload Security**:
1. Validate file size on both client and server
2. Validate file type by extension and MIME type
3. Sanitize filenames to prevent directory traversal
4. Store files outside web root
5. Use authorization checks before upload
6. Log all upload attempts

**Path Traversal Prevention**:
```java
// Ensure filename doesn't contain path separators
if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
    throw new FileValidationException("Invalid filename");
}
```

**MIME Type Validation**:
```java
// Verify MIME type matches extension
String declaredType = file.getContentType();
String expectedType = getExpectedMimeType(extension);

if (!declaredType.equals(expectedType)) {
    log.warn("MIME type mismatch: declared={}, expected={}", declaredType, expectedType);
    // Optionally reject or log for review
}
```

**Authorization**:
- Always verify user identity from authentication context
- Never trust folderId from client without authorization check
- Log unauthorized access attempts

## Configuration

**application.properties**:
```properties
# File upload configuration
file.upload-dir=/data/archive
file.max-size=52428800
file.allowed-types=pdf,doc,docx,ppt,pptx,xls,xlsx,jpg,jpeg,png,gif

# Spring multipart configuration
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=200MB
```

**Development vs Production**:
- Development: `file.upload-dir=uploads` (relative path)
- Production: `file.upload-dir=/data/archive` (absolute path)

**Deployment Checklist**:
1. Create base storage directory with proper permissions
2. Configure `file.upload-dir` in application.properties
3. Ensure application has write permissions to storage directory
4. Configure backup strategy for uploaded files
5. Set up monitoring for disk space usage
