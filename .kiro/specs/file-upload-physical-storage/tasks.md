# Implementation Plan

## Overview

This implementation plan breaks down the file upload fix and physical storage feature into discrete, manageable coding tasks. Each task builds incrementally on previous work, ensuring the system remains functional throughout development.

All context documents (requirements.md, design.md) are available during implementation.

---

## Phase 1: Backend Foundation - File Upload Service

### Task 1: Create Exception Classes for File Upload ✅ COMPLETED
- ✅ Create `FileUploadException` base exception class (already existed)
- ✅ Create `FileValidationException` for validation errors
- ✅ Create `FileStorageException` for storage errors
- ✅ Create `FolderNotFoundException` for missing folders
- ✅ Create `UnauthorizedException` for permission errors
- ✅ Add appropriate constructors and error messages
- _Requirements: 2.9, 9.1, 9.2, 9.3, 9.4_

### Task 2: Enhance UploadedFile Entity ✅ COMPLETED
- ✅ Add `folder` relationship (ManyToOne to Folder entity)
- ✅ Add `storedFilename` field for sanitized filename
- ✅ Add `uploader` relationship (ManyToOne to User entity)
- ✅ Add `notes` field for optional upload notes
- ✅ Keep `documentSubmission` relationship (made optional for backward compatibility)
- ✅ Add database indexes on `folder_id` and `uploader_id`
- ✅ Update entity with `@Builder` annotation for easier construction
- _Requirements: 1.5, 2.7_

### Task 3: Create UploadedFileRepository ✅ COMPLETED
- ✅ Create `UploadedFileRepository` interface extending `JpaRepository`
- ✅ Add `findByFolderId()` method to get files in a folder
- ✅ Add `findByUploaderId()` method to get files by uploader
- ✅ Add `findByFolderIdAndStoredFilename()` method for duplicate checking
- ✅ Add `countByFolderId()` method for file count
- _Requirements: 2.7_

### Task 4: Create FileUploadService Interface ✅ COMPLETED
- ✅ Create `FolderFileUploadService` interface
- ✅ Define `uploadFiles()` method signature with parameters: files, folderId, notes, uploaderId
- ✅ Define `validateFile()` method for single file validation
- ✅ Define `generateSafeFilename()` method for filename sanitization
- ✅ Define `canUploadToFolder()` method for authorization check
- ✅ Add comprehensive JavaDoc comments for each method
- _Requirements: 2.1, 2.2, 2.6, 3.1, 3.2, 9.1, 9.2_

### Task 5: Implement FileUploadServiceImpl - Core Upload Logic ✅ COMPLETED
- ✅ Create `FolderFileUploadServiceImpl` class implementing `FolderFileUploadService`
- ✅ Inject required dependencies: FolderRepository, UploadedFileRepository, UserRepository
- ✅ Inject configuration properties: `file.upload-dir`, `file.max-size`, `file.allowed-types`
- ✅ Implement `uploadFiles()` method:
  - ✅ Validate inputs (files not empty)
  - ✅ Get and validate folder exists
  - ✅ Get and validate user exists
  - ✅ Check authorization with `canUploadToFolder()`
  - ✅ Validate all files before uploading any
  - ✅ Create physical directory if needed
  - ✅ Loop through files and save each to disk
  - ✅ Create UploadedFile entity for each file
  - ✅ Save entities to database
  - ✅ Return list of uploaded files
- ✅ Add transaction management with `@Transactional`
- ✅ Add comprehensive logging
- _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8_

### Task 6: Implement FileUploadServiceImpl - Validation Logic ✅ COMPLETED
- ✅ Implement `validateFile()` method:
  - ✅ Check file is not empty
  - ✅ Check file size doesn't exceed maximum
  - ✅ Check filename is not null or empty
  - ✅ Extract file extension
  - ✅ Check extension is in allowed types list
  - ✅ Throw `FileValidationException` with clear message on failure
- ✅ Add helper method `getFileExtension()` to extract extension from filename
- _Requirements: 9.1, 9.2, 9.3_

### Task 7: Implement FileUploadServiceImpl - Filename Sanitization ✅ COMPLETED
- ✅ Implement `generateSafeFilename()` method:
  - ✅ Sanitize filename by replacing special characters with underscore
  - ✅ Keep only alphanumeric, dot, underscore, and hyphen
  - ✅ Check if file already exists at target path
  - ✅ If exists, append counter: `filename(1).ext`, `filename(2).ext`, etc.
  - ✅ Return safe filename
- ✅ Add helper method `getFilenameWithoutExtension()` to split name and extension
- _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 9.4_

### Task 8: Implement FileUploadServiceImpl - Authorization Logic ✅ COMPLETED
- ✅ Implement `canUploadToFolder()` method:
  - ✅ Allow DEANSHIP role to upload anywhere
  - ✅ Allow PROFESSOR role only to their own folders (check folder.owner.id == user.id)
  - ✅ Allow HOD role to upload to their own folders
  - ✅ Return false for other roles
- _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

### Task 9: Write Unit Tests for FileUploadService ✅ COMPLETED
- ✅ Test `uploadFiles()` success case with single file
- ✅ Test `uploadFiles()` success case with multiple files
- ✅ Test `uploadFiles()` error when no files provided
- ✅ Test `uploadFiles()` error when folder not found
- ✅ Test `uploadFiles()` error when user not authorized
- ✅ Test `validateFile()` success case
- ✅ Test `validateFile()` error for empty file
- ✅ Test `validateFile()` error for file too large
- ✅ Test `validateFile()` error for invalid file type
- ✅ Test `generateSafeFilename()` sanitization
- ✅ Test `generateSafeFilename()` duplicate handling
- ✅ Test `canUploadToFolder()` for professor (own folder)
- ✅ Test `canUploadToFolder()` for professor (other's folder)
- ✅ Test `canUploadToFolder()` for dean
- ✅ Mock all repository dependencies
- _Requirements: 10.3_

---

## Phase 2: Backend API - Upload Controller

### Task 10: Create UploadedFileDTO ✅ COMPLETED
- ✅ Create `UploadedFileDTO` class for API responses
- ✅ Add fields: id, originalFilename, storedFilename, fileSize, fileType, uploadedAt
- ✅ Add `@Builder` annotation
- ✅ Add validation annotations if needed
- _Requirements: 2.8_

### Task 11: Create FileUploadController ✅ COMPLETED
- ✅ Create `FileUploadController` class with `@RestController` annotation
- ✅ Set base path to `/api/professor/files`
- ✅ Inject `FileUploadService` dependency
- ✅ Inject `UserRepository` for getting current user
- ✅ Add `@PreAuthorize` for role-based security
- _Requirements: 2.1, 3.1_

### Task 12: Implement Upload Endpoint ✅ COMPLETED
- ✅ Create `uploadFiles()` endpoint method with `@PostMapping("/upload")`
- ✅ Accept parameters: `@RequestParam("files[]") MultipartFile[]`, `@RequestParam("folderId") Long`, `@RequestParam("notes") String` (optional)
- ✅ Get current user from `@AuthenticationPrincipal UserDetails`
- ✅ Call `fileUploadService.uploadFiles()` with parameters
- ✅ Convert UploadedFile entities to DTOs
- ✅ Return `ResponseEntity<ApiResponse<List<UploadedFileDTO>>>`
- ✅ Add comprehensive logging
- _Requirements: 2.1, 2.2, 2.8_

### Task 13: Implement Error Handling in Controller ✅ COMPLETED
- ✅ Add try-catch blocks for different exception types
- ✅ Handle `FolderNotFoundException` → return 404 with error message
- ✅ Handle `UnauthorizedException` → return 403 with error message
- ✅ Handle `FileValidationException` → return 400 with error message
- ✅ Handle `FileStorageException` → return 500 with error message
- ✅ Handle generic `Exception` → return 500 with generic message
- ✅ Return consistent `ApiResponse` format for all errors
- ✅ Log all errors with appropriate level
- _Requirements: 2.9, 8.1, 8.2, 8.3, 8.4, 8.5_

### Task 14: Write Integration Tests for FileUploadController ✅ COMPLETED
- ✅ Test upload endpoint with authenticated professor (success)
- ✅ Test upload endpoint with multiple files (success)
- ✅ Test upload endpoint with notes (success)
- ✅ Test upload endpoint without authentication (403 error due to CSRF)
- ✅ Test upload endpoint with wrong professor (403 error)
- ✅ Test upload endpoint with non-existent folder (404 error)
- ✅ Test upload endpoint with invalid file (400 error)
- ✅ Test upload endpoint with file too large (400 error)
- ✅ Test upload endpoint with special characters in filename (sanitization)
- ✅ Test upload endpoint with duplicate filename (rename with counter)
- ✅ Test upload endpoint with various allowed file types
- ✅ Test deanship can upload to any folder
- ✅ Use mock authentication for testing
- ✅ Use `MockMultipartFile` for file uploads
- ✅ Verify response status codes and messages
- ✅ Verify physical files are created on disk
- ✅ Verify database records are created
- _Requirements: 10.3_

---

## Phase 3: Frontend - Upload Modal

### Task 15: Add Upload Modal HTML to Professor Dashboard ✅ COMPLETED
- ✅ Open professor dashboard HTML file
- ✅ Add upload modal structure with:
  - ✅ Modal container with id `uploadModal`
  - ✅ Modal header with dynamic title id `uploadModalTitle`
  - ✅ Close button
  - ✅ File input with id `uploadFiles` (multiple, with accept attribute)
  - ✅ Notes textarea with id `uploadNotes`
  - ✅ Error message div with id `uploadError` (hidden by default)
  - ✅ Cancel button
  - ✅ Upload button with id `uploadBtn`
- ✅ Add appropriate CSS classes for styling
- ✅ Ensure modal is hidden by default
- _Requirements: 4.1, 8.1_

### Task 16: Implement openUploadModal() Function ✅ COMPLETED
- ✅ Create `openUploadModal()` function in prof.js
- ✅ Get current node from `fileExplorerState.getCurrentNode()`
- ✅ Validate current node exists and has an id
- ✅ Validate current node type is 'SUBFOLDER' (category folder)
- ✅ Show error toast if validation fails
- ✅ Set modal title based on folder name
- ✅ Clear previous file input and notes textarea
- ✅ Hide error message div
- ✅ Show modal by removing 'hidden' class
- _Requirements: 4.1, 4.2, 5.2_

### Task 17: Implement closeUploadModal() Function ✅ COMPLETED
- ✅ Create `closeUploadModal()` function in prof.js
- ✅ Hide modal by adding 'hidden' class
- ✅ Clear file input
- ✅ Clear notes textarea
- ✅ Hide error message div
- _Requirements: 4.1_

### Task 18: Implement handleUpload() Function - Validation ✅ COMPLETED
- ✅ Create `handleUpload()` async function in prof.js
- ✅ Get references to: uploadBtn, errorDiv, filesInput, notesInput
- ✅ Get current node from FileExplorerState
- ✅ Validate current node exists (show error if not)
- ✅ Get selected files from file input
- ✅ Validate at least one file selected (show error if not)
- ✅ Validate each file size doesn't exceed 50MB (show error if any exceed)
- _Requirements: 4.2, 4.3, 9.1, 9.2_

### Task 19: Implement handleUpload() Function - Upload Logic ✅ COMPLETED
- ✅ Construct FormData object
- ✅ Append each file with key 'files[]'
- ✅ Append folderId from current node
- ✅ Append notes if provided
- ✅ Set loading state: disable button, change text to "Uploading...", hide error
- ✅ Send POST request to `/api/professor/files/upload` with FormData
- ✅ Parse JSON response
- _Requirements: 4.2, 4.3, 4.4_

### Task 20: Implement handleUpload() Function - Response Handling ✅ COMPLETED
- ✅ Check if response is ok and result.success is true
- ✅ On success:
  - ✅ Show success toast with file count
  - ✅ Close modal
  - ✅ Call `refreshCurrentFolderFiles()` to update file list
- ✅ On error:
  - ✅ Show error message in modal error div
  - ✅ Keep modal open
- ✅ On network error (catch block):
  - ✅ Show network error message
  - ✅ Keep modal open
- ✅ Always reset button state in finally block
- _Requirements: 4.5, 4.6, 4.7, 8.3, 8.4_

### Task 21: Implement refreshCurrentFolderFiles() Function ✅ COMPLETED
- ✅ Create `refreshCurrentFolderFiles()` async function
- ✅ Get current node from FileExplorerState
- ✅ Return early if no current node
- ✅ Set file list loading state to true
- ✅ Fetch updated node data from `/api/file-explorer/node?path={path}`
- ✅ Parse JSON response
- ✅ Update FileExplorerState with new current node data
- ✅ Set file list loading state to false
- ✅ Handle errors gracefully (log but don't show to user)
- _Requirements: 4.5, 6.1, 6.2, 6.3_

---

## Phase 4: Frontend - File Explorer Integration

### Task 22: Add Upload Button to File List View ✅ COMPLETED
- ✅ Modify `renderFileList()` function in file-explorer.js
- ✅ Check if current node type is 'SUBFOLDER' (DOCUMENT_TYPE in API)
- ✅ If yes, add upload button at top of file list
- ✅ Button dispatches 'fileExplorerUpload' custom event on click
- ✅ Button has icon and text: "Upload Files"
- ✅ Add appropriate styling
- _Requirements: 5.1, 5.2, 5.5_

### Task 23: Implement File Card Rendering ✅ COMPLETED
- ✅ Create `renderFileCard()` function
- ✅ Accept file object as parameter
- ✅ Get appropriate icon based on file type using `getFileIconClass()`
- ✅ Format file size using `formatFileSize()`
- ✅ Format upload date using `formatDate()`
- ✅ Render file card HTML with:
  - ✅ File icon (SVG with color based on file type)
  - ✅ Original filename (escaped)
  - ✅ File size and date (in metadata badges)
  - ✅ Notes (if present, escaped and truncated)
  - ✅ Download button (blue with icon)
  - ✅ View button (gray with icon)
  - ✅ Delete button (not implemented - optional for this phase)
- ✅ Return HTML string (table row format)
- ✅ Refactored `renderFileList()` to use `renderFileCard()`
- _Requirements: 6.1, 6.2, 6.3_

### Task 24: Implement Helper Functions for File Display ✅ COMPLETED
- ✅ Create `getFileIcon()` function:
  - ✅ Accept fileType (MIME type) as parameter
  - ✅ Return appropriate icon identifier based on type
  - ✅ Handle: PDF, Word, PowerPoint, Excel, images, archives, text, generic file
- ✅ Create `formatFileSize()` function:
  - ✅ Accept bytes as parameter
  - ✅ Convert to appropriate unit (B, KB, MB, GB)
  - ✅ Return formatted string with 1 decimal place
- ✅ Create `formatDate()` function:
  - ✅ Already exists in ui.js and is imported
- ✅ Create `escapeHtml()` function:
  - ✅ Already exists in FileExplorer class
  - ✅ Escapes HTML to prevent XSS
- _Requirements: 6.1, 6.2, 6.3_

### Task 25: Update File List Rendering to Show Uploaded Files ✅ COMPLETED
- ✅ Modify `renderFileList()` function to check if node has files array
- ✅ If files exist and length > 0:
  - ✅ Create file grid container
  - ✅ Loop through files and call `renderFileCard()` for each
  - ✅ Append to container
- ✅ If no files:
  - ✅ Show "No files in this folder" message
- ✅ Ensure upload button appears above file list
- _Requirements: 6.1, 6.2, 6.3, 6.4_

---

## Phase 5: Backend API - File Explorer Enhancement

### Task 26: Update File Explorer API to Include Files ✅ COMPLETED
- ✅ Modify File Explorer node endpoint to include files in response
- ✅ Query UploadedFile repository for files in current folder
- ✅ Convert UploadedFile entities to DTOs
- ✅ Add files array to node response
- ✅ Ensure files are included for SUBFOLDER type nodes
- _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

### Task 27: Create File List Endpoint (Optional) ✅ COMPLETED
- ✅ Create dedicated endpoint for getting files in a folder: `GET /api/professor/files?folderId={id}`
- ✅ Accept folderId as query parameter
- ✅ Get folder and validate authorization
- ✅ Query files for folder
- ✅ Convert to DTOs and return
- ✅ Add integration tests for the endpoint
- _Requirements: 6.1, 6.2_

---

## Phase 6: Testing and Validation

### Task 28: Manual Testing - Upload Modal ✅ COMPLETED
- ✅ Test opening upload modal from different category folders
- ✅ Test modal title updates correctly
- ✅ Test file input accepts multiple files
- ✅ Test notes textarea is optional
- ✅ Test cancel button closes modal
- ✅ Test close (X) button closes modal
- ✅ Test validation error for no files selected
- ✅ Test validation error for file too large
- ✅ Test validation error for no folder selected
- _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 8.1, 8.2, 8.3, 8.4, 8.5_

### Task 29: Manual Testing - Upload Process ✅ COMPLETED
- ✅ Test uploading single file to Course Notes
- ✅ Test uploading multiple files to Exams
- ✅ Test uploading with notes to Syllabus
- ✅ Test uploading without notes to Assignments
- ✅ Verify upload button shows "Uploading..." during upload
- ✅ Verify upload button is disabled during upload
- ✅ Verify modal closes on success
- ✅ Verify success toast appears
- ✅ Verify file list refreshes automatically
- ✅ Verify uploaded files appear in list with correct metadata
- _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3_

### Task 30: Manual Testing - Error Handling
- Test error for invalid file type
- Test error for file too large (>50MB)
- Test error for uploading to wrong folder type
- Test error for unauthorized upload (different professor's folder)
- Verify error messages display in modal
- Verify modal stays open on error
- Verify can retry after error
- Verify network error handling
- _Requirements: 4.7, 8.1, 8.2, 8.3, 8.4, 8.5, 9.1, 9.2, 9.3, 9.4_

### Task 31: Manual Testing - Physical Storage Verification
- Upload file and verify it exists in database
- Check physical file system and verify file exists at correct path
- Verify path matches: `{uploadDir}/{folderPath}/{storedFilename}`
- Upload file with special characters in name and verify sanitization
- Upload duplicate filename and verify rename with counter
- Delete file from database and verify orphaned file on disk (document behavior)
- _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 7.1, 7.2, 7.3, 7.4, 7.5_

### Task 32: Manual Testing - Cross-Dashboard Synchronization
- Upload file as professor
- Login as dean and verify file appears in File Explorer
- Verify file metadata is correct in both views
- Upload file as dean to professor's folder
- Login as professor and verify file appears
- _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

### Task 33: Browser Compatibility Testing
- Test upload functionality in Chrome (latest)
- Test upload functionality in Firefox (latest)
- Test upload functionality in Edge (latest)
- Test upload functionality in Safari (latest, if available)
- Verify file input behavior is consistent
- Verify FormData submission works
- Verify modal behavior is consistent
- Verify error display is consistent
- Check console for errors in each browser
- _Requirements: 10.3_

---

## Phase 7: Documentation and Cleanup

### Task 34: Update API Documentation
- Document FileUploadService methods with parameters and return types
- Document upload endpoint with request/response formats
- Document error responses and status codes
- Document file validation rules (size, types)
- Document authorization rules
- Add code examples for upload requests
- _Requirements: 10.2_

### Task 35: Update Developer Guide
- Document how to configure file storage directory
- Document how to add new allowed file types
- Document how to change maximum file size
- Document filename sanitization logic
- Document duplicate file handling policy
- Document how to integrate upload modal in new dashboards
- Add troubleshooting section for common issues
- _Requirements: 10.2_

### Task 36: Create Deployment Guide
- Document production storage directory setup
- Document file system permissions required
- Document configuration properties for production
- Document backup strategy recommendations
- Document monitoring recommendations (disk space, upload errors)
- Document security considerations
- _Requirements: 10.2_

### Task 37: Code Review and Cleanup
- Review all backend code for quality and consistency
- Verify all methods have JavaDoc comments
- Verify proper exception handling throughout
- Remove any debug logging or commented code
- Review all frontend code for quality and consistency
- Verify all functions have JSDoc comments
- Remove any console.log statements (except intentional logging)
- Remove any commented code
- Verify consistent code style
- _Requirements: 10.1, 10.2, 10.4_

### Task 38: Verify No Regressions
- Test existing File Explorer functionality still works
- Test folder navigation still works
- Test breadcrumb navigation still works
- Test file download still works (if implemented)
- Test folder creation still works
- Test course assignment folder creation still works
- Run all existing unit tests and verify they pass
- Run all existing integration tests and verify they pass
- _Requirements: 10.1_

---

## Summary

This implementation plan consists of **38 tasks** organized into **7 phases**:

1. **Phase 1** (Tasks 1-9): Backend foundation - FileUploadService
2. **Phase 2** (Tasks 10-14): Backend API - FileUploadController
3. **Phase 3** (Tasks 15-21): Frontend - Upload modal
4. **Phase 4** (Tasks 22-25): Frontend - File Explorer integration
5. **Phase 5** (Tasks 26-27): Backend API - File Explorer enhancement
6. **Phase 6** (Tasks 28-33): Testing and validation
7. **Phase 7** (Tasks 34-38): Documentation and cleanup

Each task is focused on a specific coding activity and builds incrementally on previous tasks. The plan ensures that:
- Files are stored physically on disk matching the folder structure
- Upload endpoint is properly implemented with validation and authorization
- Frontend upload modal is fully functional
- File list displays uploaded files
- All functionality is thoroughly tested
- Code is well-documented and production-ready

**Key Deliverables**:
1. FileUploadService with validation, sanitization, and authorization
2. FileUploadController with upload endpoint
3. Enhanced UploadedFile entity with folder relationship
4. Upload modal in professor dashboard
5. File list display in File Explorer
6. Comprehensive tests (unit, integration, manual)
7. Complete documentation (API, developer guide, deployment guide)
