package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.entity.academic.CourseAssignment;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.alquds.edu.ArchiveSystem.entity.file.Folder;
import com.alquds.edu.ArchiveSystem.entity.file.FolderType;
import com.alquds.edu.ArchiveSystem.repository.submission.RequiredDocumentTypeRepository;
import com.alquds.edu.ArchiveSystem.repository.file.UploadedFileRepository;
import com.alquds.edu.ArchiveSystem.repository.file.FolderRepository;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.entity.submission.RequiredDocumentType;
import com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus;


import com.alquds.edu.ArchiveSystem.exception.file.FileUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FileServiceImpl implements FileService {

    private final CourseAssignmentRepository courseAssignmentRepository;
    private final DocumentSubmissionRepository documentSubmissionRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final UserRepository userRepository;
    private final RequiredDocumentTypeRepository requiredDocumentTypeRepository;
    private final FolderRepository folderRepository;

    @Value("${file.upload.directory:uploads/}")
    private String uploadDirectory;

    // Allowed file extensions for assignment/document uploads
    // Supports common university document types (PDF, Office docs, text, archives, images, CSV)
    private static final List<String> DEFAULT_ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx",
            "txt", "zip", "rar", "jpg", "jpeg", "png", "gif", "csv");
    // Maximum size for a single file in assignment/document uploads (100MB)
    private static final long MAX_SINGLE_FILE_SIZE = 100 * 1024 * 1024; // 100MB per file

    @Override
    @Transactional
    public List<UploadedFile> uploadFiles(Long courseAssignmentId, DocumentTypeEnum documentType,
            List<MultipartFile> files, String notes, Long professorId) {
        log.info("Uploading {} files for course assignment {} and document type {}",
                files.size(), courseAssignmentId, documentType);

        // Validate course assignment exists
        CourseAssignment courseAssignment = courseAssignmentRepository.findById(courseAssignmentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Course assignment not found with ID: " + courseAssignmentId));

        // Check write permission
        User currentUser = getCurrentUser();
        if (!canWriteToCourseAssignment(courseAssignment, currentUser)) {
            throw new com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException(
                    "User does not have permission to upload files to this course assignment");
        }

        // Validate files
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided for upload");
        }

        // Get required document type configuration for validation limits
        List<RequiredDocumentType> requiredDocs = requiredDocumentTypeRepository
                .findByCourseIdAndDocumentType(courseAssignment.getCourse().getId(), documentType);

        RequiredDocumentType requiredDoc = requiredDocs.isEmpty() ? null : requiredDocs.get(0);

        // Determine validation limits
        Integer maxFileCount = requiredDoc != null && requiredDoc.getMaxFileCount() != null
                ? requiredDoc.getMaxFileCount()
                : 10; // Default 10 files
        Integer maxTotalSizeMb = requiredDoc != null && requiredDoc.getMaxTotalSizeMb() != null
                ? requiredDoc.getMaxTotalSizeMb()
                : 50; // Default 50MB

        // Validate file count
        if (files.size() > maxFileCount) {
            throw new IllegalArgumentException(
                    String.format("File count (%d) exceeds maximum allowed (%d)",
                            files.size(), maxFileCount));
        }

        // Validate file types and sizes
        validateFiles(files, DEFAULT_ALLOWED_EXTENSIONS, maxTotalSizeMb);

        // Find or create document submission
        DocumentSubmission submission = documentSubmissionRepository
                .findByCourseAssignmentIdAndDocumentType(courseAssignmentId, documentType)
                .orElse(null);

        if (submission == null) {
            // Create new submission
            submission = new DocumentSubmission();
            submission.setCourseAssignment(courseAssignment);
            submission.setDocumentType(documentType);
            submission.setProfessor(courseAssignment.getProfessor());
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setStatus(SubmissionStatus.UPLOADED);

            // Check if submission is late by comparing with deadline
            boolean isLate = false;
            if (requiredDoc != null && requiredDoc.getDeadline() != null) {
                LocalDateTime deadline = requiredDoc.getDeadline();
                isLate = LocalDateTime.now().isAfter(deadline);

                if (isLate) {
                    log.warn("Late submission detected for course assignment {} and document type {}. Deadline was: {}",
                            courseAssignmentId, documentType, deadline);
                }
            }
            submission.setIsLateSubmission(isLate);

            submission.setNotes(notes);
            submission = documentSubmissionRepository.save(submission);
            log.debug("Created new document submission with ID: {}", submission.getId());
        } else {
            // Update existing submission
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setNotes(notes);
            submission.setStatus(SubmissionStatus.UPLOADED);
            log.debug("Updating existing document submission with ID: {}", submission.getId());
        }

        // Save files
        List<UploadedFile> uploadedFiles = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            UploadedFile uploadedFile = saveFile(file, submission, courseAssignment, i, currentUser);
            uploadedFiles.add(uploadedFile);
        }

        // Update submission metadata
        submission.setFileCount(uploadedFiles.size());
        submission.setTotalFileSize(uploadedFiles.stream()
                .mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0L)
                .sum());
        documentSubmissionRepository.save(submission);

        log.info("Successfully uploaded {} files for submission ID: {}", uploadedFiles.size(), submission.getId());
        return uploadedFiles;
    }

    @Override
    @Transactional
    public void replaceFiles(Long submissionId, List<MultipartFile> files, String notes) {
        log.info("Replacing files for submission ID: {}", submissionId);

        // Find submission
        DocumentSubmission submission = documentSubmissionRepository.findById(submissionId)
                .orElseThrow(
                        () -> new EntityNotFoundException("Document submission not found with ID: " + submissionId));

        // Check write permission (professor must own the submission)
        User currentUser = getCurrentUser();
        if (currentUser == null || !submission.getProfessor().getId().equals(currentUser.getId())) {
            throw new com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException(
                    "User does not have permission to replace files for this submission");
        }

        // Validate files
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided for replacement");
        }

        // Get required document type configuration for validation limits
        CourseAssignment courseAssignment = submission.getCourseAssignment();
        List<RequiredDocumentType> requiredDocs = requiredDocumentTypeRepository
                .findByCourseIdAndDocumentType(courseAssignment.getCourse().getId(), submission.getDocumentType());

        RequiredDocumentType requiredDoc = requiredDocs.isEmpty() ? null : requiredDocs.get(0);

        // Determine validation limits
        Integer maxFileCount = requiredDoc != null && requiredDoc.getMaxFileCount() != null
                ? requiredDoc.getMaxFileCount()
                : 10; // Default 10 files
        Integer maxTotalSizeMb = requiredDoc != null && requiredDoc.getMaxTotalSizeMb() != null
                ? requiredDoc.getMaxTotalSizeMb()
                : 50; // Default 50MB

        // Validate file count
        if (files.size() > maxFileCount) {
            throw new IllegalArgumentException(
                    String.format("File count (%d) exceeds maximum allowed (%d)",
                            files.size(), maxFileCount));
        }

        validateFiles(files, DEFAULT_ALLOWED_EXTENSIONS, maxTotalSizeMb);

        // Delete old files
        List<UploadedFile> oldFiles = uploadedFileRepository.findByDocumentSubmissionId(submissionId);
        for (UploadedFile oldFile : oldFiles) {
            deletePhysicalFile(oldFile.getFileUrl());
        }
        uploadedFileRepository.deleteAll(oldFiles);
        log.debug("Deleted {} old files for submission ID: {}", oldFiles.size(), submissionId);

        // Upload new files
        List<UploadedFile> newFiles = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            UploadedFile uploadedFile = saveFile(file, submission, courseAssignment, i, currentUser);
            newFiles.add(uploadedFile);
        }

        // Update submission metadata
        submission.setFileCount(newFiles.size());
        submission.setTotalFileSize(newFiles.stream()
                .mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0L)
                .sum());
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setNotes(notes);
        documentSubmissionRepository.save(submission);

        log.info("Successfully replaced files for submission ID: {}. New file count: {}",
                submissionId, newFiles.size());
    }

    @Override
    @Transactional
    public UploadedFile replaceFile(Long fileId, MultipartFile newFile, String notes, Long userId) {
        log.info("Replacing file ID: {} with new file: {}", fileId, newFile.getOriginalFilename());

        // Find the existing file with folder eagerly loaded
        UploadedFile existingFile = uploadedFileRepository.findByIdWithUploaderAndFolder(fileId)
                .orElseThrow(() -> FileUploadException.fileNotFound(fileId));

        // Get the uploader/owner for permission check
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        // Check ownership
        if (existingFile.getUploader() == null || !existingFile.getUploader().getId().equals(userId)) {
            throw new com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException(
                    "User does not have permission to replace this file");
        }

        // Validate the new file
        if (newFile == null || newFile.isEmpty()) {
            throw new IllegalArgumentException("No file provided for replacement");
        }

        // Get the folder and path info from existing file
        Folder folder = existingFile.getFolder();
        String oldFileUrl = existingFile.getFileUrl();
        
        // Delete the old physical file
        deletePhysicalFile(oldFileUrl);

        // Generate new filename and save the new file
        String sanitizedFilename = sanitizeFilename(newFile.getOriginalFilename());
        String newFileUrl;
        
        if (folder != null) {
            // Folder-based file storage
            newFileUrl = folder.getPath() + "/" + sanitizedFilename;
        } else if (oldFileUrl != null && !oldFileUrl.isEmpty()) {
            // Use existing file path structure - replace just the filename
            int lastSlash = oldFileUrl.lastIndexOf('/');
            if (lastSlash > 0) {
                newFileUrl = oldFileUrl.substring(0, lastSlash + 1) + sanitizedFilename;
            } else {
                newFileUrl = sanitizedFilename;
            }
        } else {
            throw new IllegalStateException("Cannot determine storage location for file");
        }

        // Save the new file to disk
        Path targetPath = Paths.get(uploadDirectory, newFileUrl);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(newFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to save replacement file: {}", e.getMessage(), e);
            throw FileUploadException.storageError("Failed to save replacement file: " + e.getMessage());
        }

        // Update the file entity
        existingFile.setOriginalFilename(newFile.getOriginalFilename());
        existingFile.setStoredFilename(sanitizedFilename);
        existingFile.setFileUrl(newFileUrl);
        existingFile.setFileSize(newFile.getSize());
        existingFile.setFileType(newFile.getContentType());
        existingFile.setNotes(notes);
        existingFile.setUploader(uploader);

        // Save and return
        UploadedFile savedFile = uploadedFileRepository.save(existingFile);
        log.info("Successfully replaced file ID: {} with new file at: {}", fileId, newFileUrl);

        return savedFile;
    }

    @Override
    @Transactional(readOnly = true)
    public UploadedFile getFile(Long fileId) {
        log.debug("Fetching file with ID: {}", fileId);
        UploadedFile file = uploadedFileRepository.findByIdWithUploader(fileId)
                .orElseThrow(() -> FileUploadException.fileNotFound(fileId));

        // Check read permission
        User currentUser = getCurrentUser();
        if (currentUser != null && !canReadFile(file, currentUser)) {
            throw new com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException(
                    "User does not have permission to access this file");
        }

        return file;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UploadedFile> getFilesBySubmission(Long submissionId) {
        log.debug("Fetching files for submission ID: {}", submissionId);

        // Verify submission exists
        DocumentSubmission submission = documentSubmissionRepository.findById(submissionId)
                .orElseThrow(
                        () -> new EntityNotFoundException("Document submission not found with ID: " + submissionId));

        // Check read permission for the submission
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            // Get any file from the submission to check permissions
            List<UploadedFile> files = uploadedFileRepository
                    .findByDocumentSubmissionIdOrderByFileOrderAsc(submissionId);

            // If there are files, check permission on the first one
            // If no files yet, check if user can read based on department
            if (!files.isEmpty()) {
                if (!canReadFile(files.get(0), currentUser)) {
                    throw new com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException(
                            "User does not have permission to access files for this submission");
                }
            } else {
                // No files yet, check department access
                User submissionOwner = submission.getProfessor();
                if (currentUser.getRole() != Role.ROLE_DEANSHIP) {
                    if (currentUser.getDepartment() == null || submissionOwner.getDepartment() == null ||
                            !currentUser.getDepartment().getId().equals(submissionOwner.getDepartment().getId())) {
                        throw new com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException(
                                "User does not have permission to access files for this submission");
                    }
                }
            }

            return files;
        }

        return uploadedFileRepository.findByDocumentSubmissionIdOrderByFileOrderAsc(submissionId);
    }

    @Override
    public Resource loadFileAsResource(String fileUrl) {
        try {
            // Note: Permission checking for file download should be done at the controller
            // level
            // where we have the fileId and can check canRead permission

            // SECURITY: Resolve base path and target path for path traversal protection
            Path basePath = Paths.get(uploadDirectory).toAbsolutePath().normalize();
            Path filePath = basePath.resolve(fileUrl).normalize();

            // SECURITY: Prevent path traversal attacks by ensuring resolved path is within base directory
            if (!filePath.startsWith(basePath)) {
                log.error("Path traversal attempt detected - requested path: {}, resolved: {}", fileUrl, filePath);
                throw FileUploadException.storageError("Invalid file path: access denied");
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                log.error("Physical file not found on disk: {}", fileUrl);
                throw FileUploadException.physicalFileNotFound(fileUrl);
            }
        } catch (MalformedURLException e) {
            log.error("Error loading file as resource: {}", fileUrl, e);
            throw FileUploadException.storageError("Could not load file: " + fileUrl);
        }
    }

    /**
     * Load file as resource with permission checking.
     * This method should be used by controllers to ensure proper access control.
     *
     * @param fileId      the file ID
     * @param currentUser the current authenticated user
     * @return the file resource
     * @throws com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException if
     *                                                                               user
     *                                                                               lacks
     *                                                                               read
     *                                                                               permission
     */
    public Resource loadFileAsResourceWithPermissionCheck(Long fileId, User currentUser) {
        log.debug("Loading file {} with permission check for user {}", fileId, currentUser.getEmail());

        // Get file and check read permission
        UploadedFile file = getFile(fileId); // This already checks read permission

        // Load the resource
        return loadFileAsResource(file.getFileUrl());
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId) {
        log.info("Deleting file with ID: {}", fileId);

        UploadedFile file = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> FileUploadException.fileNotFound(fileId));

        // Check delete permission
        User currentUser = getCurrentUser();
        if (!canDeleteFile(file, currentUser)) {
            throw new com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException(
                    "User does not have permission to delete this file");
        }

        DocumentSubmission submission = file.getDocumentSubmission();

        // Delete physical file
        deletePhysicalFile(file.getFileUrl());

        // Delete database record
        uploadedFileRepository.delete(file);

        // Update submission metadata only if file was part of a submission
        if (submission != null) {
            List<UploadedFile> remainingFiles = uploadedFileRepository.findByDocumentSubmissionId(submission.getId());
            submission.setFileCount(remainingFiles.size());
            submission.setTotalFileSize(remainingFiles.stream()
                    .mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0L)
                    .sum());
            documentSubmissionRepository.save(submission);
        }

        log.info("Successfully deleted file ID: {}", fileId);
    }

    @Override
    public boolean validateFileType(MultipartFile file, List<String> allowedExtensions) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return false;
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        List<String> extensionsToCheck = (allowedExtensions != null && !allowedExtensions.isEmpty())
                ? allowedExtensions
                : DEFAULT_ALLOWED_EXTENSIONS;

        return extensionsToCheck.stream()
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));
    }

    @Override
    public boolean validateFileSize(List<MultipartFile> files, Integer maxTotalSizeMb) {
        if (files == null || files.isEmpty()) {
            return false;
        }

        long totalSize = files.stream()
                .mapToLong(MultipartFile::getSize)
                .sum();

        long maxSizeBytes = (maxTotalSizeMb != null ? maxTotalSizeMb : 50) * 1024L * 1024L;

        return totalSize <= maxSizeBytes;
    }

    @Override
    public String generateFilePath(String yearCode, String semesterType, String professorId,
            String courseCode, DocumentTypeEnum documentType, String filename) {
        // Generate path:
        // {year}/{semester}/{professorId}/{courseCode}/{documentType}/{filename}
        String sanitizedFilename = sanitizeFilename(filename);
        String uniqueFilename = generateUniqueFilename(sanitizedFilename);
        
        // Use the formatted folder name to match standard subfolder names
        String folderName = formatDocumentTypeForFolder(documentType);

        return String.format("%s/%s/%s/%s/%s/%s",
                yearCode,
                semesterType.toLowerCase(),
                professorId,
                courseCode,
                folderName,
                uniqueFilename);
    }

    // ========== Private Helper Methods ==========

    private void validateFiles(List<MultipartFile> files, List<String> allowedExtensions, Integer maxTotalSizeMb) {
        for (MultipartFile file : files) {
            // Validate file is not empty
            if (file.isEmpty()) {
                throw new IllegalArgumentException(
                        String.format("File '%s' is empty. Please select a valid file.",
                                file.getOriginalFilename()));
            }

            // Validate single file size
            if (file.getSize() > MAX_SINGLE_FILE_SIZE) {
                throw new IllegalArgumentException(
                        String.format("File '%s' size (%.2f MB) exceeds maximum allowed per file (%.2f MB)",
                                file.getOriginalFilename(),
                                file.getSize() / (1024.0 * 1024.0),
                                MAX_SINGLE_FILE_SIZE / (1024.0 * 1024.0)));
            }

            // Validate file type
            if (!validateFileType(file, allowedExtensions)) {
                List<String> exts = allowedExtensions != null ? allowedExtensions : DEFAULT_ALLOWED_EXTENSIONS;
                throw new IllegalArgumentException(
                        String.format("File '%s' has invalid type. Only %s files are allowed.",
                                file.getOriginalFilename(),
                                String.join(", ", exts).toUpperCase()));
            }
        }

        // Validate total size
        if (!validateFileSize(files, maxTotalSizeMb)) {
            long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
            throw new IllegalArgumentException(
                    String.format("Total file size (%.2f MB) exceeds maximum allowed (%d MB)",
                            totalSize / (1024.0 * 1024.0), maxTotalSizeMb));
        }
    }

    /**
     * Generate a safe folder name from professor's name.
     * Sanitizes the name to be filesystem-safe while remaining readable.
     */
    private String generateProfessorFolderName(User professor) {
        String firstName = professor.getFirstName() != null ? professor.getFirstName().trim() : "";
        String lastName = professor.getLastName() != null ? professor.getLastName().trim() : "";
        
        // Combine names
        String fullName = (firstName + " " + lastName).trim();
        
        // If name is empty, use fallback
        if (fullName.isEmpty()) {
            return "prof_" + professor.getId();
        }
        
        // Sanitize: remove characters invalid in file paths: \ / : * ? " < > |
        String sanitized = fullName.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        // Collapse multiple spaces/underscores into single ones
        sanitized = sanitized.replaceAll("\\s+", " ").replaceAll("_+", "_").trim();
        
        return sanitized;
    }

    private UploadedFile saveFile(MultipartFile file, DocumentSubmission submission,
            CourseAssignment courseAssignment, int order, User uploader) {
        try {
            // Get metadata for path generation
            Semester semester = courseAssignment.getSemester();
            AcademicYear academicYear = semester.getAcademicYear();
            Course course = courseAssignment.getCourse();
            User professor = courseAssignment.getProfessor();

            // Use professor's full name for folder path (sanitized for filesystem)
            String professorFolderName = generateProfessorFolderName(professor);
            log.info("Using professor folder name: {} for professor {}", professorFolderName, professor.getEmail());

            // Generate file path
            String filePath = generateFilePath(
                    academicYear.getYearCode(),
                    semester.getType().name(),
                    professorFolderName,
                    course.getCourseCode(),
                    submission.getDocumentType(),
                    file.getOriginalFilename());

            // Save physical file
            savePhysicalFile(file, filePath);
            
            // Find the document type subfolder to link the file properly
            // This ensures files appear in the File Explorer
            Folder documentTypeFolder = findOrCreateDocumentTypeFolder(
                    professor, course, academicYear, semester, submission.getDocumentType());

            // Create database record
            UploadedFile uploadedFile = new UploadedFile();
            uploadedFile.setDocumentSubmission(submission);
            uploadedFile.setFileUrl(filePath);
            uploadedFile.setOriginalFilename(file.getOriginalFilename());
            uploadedFile.setStoredFilename(sanitizeFilenameForStorage(file.getOriginalFilename()));
            uploadedFile.setFileSize(file.getSize());
            uploadedFile.setFileType(file.getContentType());
            uploadedFile.setFileOrder(order);
            uploadedFile.setUploader(uploader); // Set the uploader for permission checking
            uploadedFile.setFolder(documentTypeFolder); // Link to folder for File Explorer visibility

            uploadedFile = uploadedFileRepository.save(uploadedFile);
            log.debug("Saved file: {} with ID: {} linked to folder ID: {}", 
                    file.getOriginalFilename(), uploadedFile.getId(), 
                    documentTypeFolder != null ? documentTypeFolder.getId() : "null");

            return uploadedFile;

        } catch (IOException e) {
            log.error("Failed to save file: {}", file.getOriginalFilename(), e);
            throw FileUploadException.uploadFailed(file.getOriginalFilename(), e.getMessage());
        }
    }

    private void savePhysicalFile(MultipartFile file, String relativePath) throws IOException {
        // Create full path
        Path fullPath = Paths.get(uploadDirectory, relativePath);

        // Create directories if they don't exist
        Files.createDirectories(fullPath.getParent());

        // Save file
        Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);

        log.debug("Saved physical file to: {}", fullPath);
    }
    
    /**
     * Find or create the document type folder for linking uploaded files.
     * This ensures files appear in the File Explorer by having a proper folder linkage.
     * 
     * @param professor the professor who owns the folder
     * @param course the course
     * @param academicYear the academic year
     * @param semester the semester
     * @param documentType the document type
     * @return the folder entity, or null if not found/created
     */
    private Folder findOrCreateDocumentTypeFolder(User professor, Course course, 
            AcademicYear academicYear, Semester semester, DocumentTypeEnum documentType) {
        try {
            // First, find the course folder
            java.util.Optional<Folder> courseFolderOpt = folderRepository.findCourseFolder(
                    professor.getId(), course.getId(), academicYear.getId(), semester.getId(), FolderType.COURSE);
            
            if (!courseFolderOpt.isPresent()) {
                log.warn("Course folder not found for professor {} and course {}", 
                        professor.getEmail(), course.getCourseCode());
                return null;
            }
            
            Folder courseFolder = courseFolderOpt.get();
            
            // Get the display name for this document type
            String folderName = formatDocumentTypeForFolder(documentType);
            
            // Find the subfolder for this document type
            java.util.List<Folder> subfolders = folderRepository.findByParentId(courseFolder.getId());
            java.util.Optional<Folder> documentTypeFolderOpt = subfolders.stream()
                    .filter(f -> f.getName().equalsIgnoreCase(folderName))
                    .findFirst();
            
            if (documentTypeFolderOpt.isPresent()) {
                return documentTypeFolderOpt.get();
            }
            
            // If not found, create the subfolder
            log.info("Creating document type folder: {} under course folder ID: {}", folderName, courseFolder.getId());
            
            String professorFolderName = generateProfessorFolderName(professor);
            String folderPath = academicYear.getYearCode() + "/" + semester.getType().name().toLowerCase() + 
                    "/" + professorFolderName + "/" + course.getCourseCode() + "/" + folderName;
            
            Folder newFolder = new Folder();
            newFolder.setName(folderName);
            newFolder.setPath(folderPath);
            newFolder.setType(FolderType.SUBFOLDER);
            newFolder.setOwner(professor);
            newFolder.setCourse(course);
            newFolder.setAcademicYear(academicYear);
            newFolder.setSemester(semester);
            newFolder.setParent(courseFolder);
            
            // Create physical directory
            Path physicalPath = Paths.get(uploadDirectory, folderPath);
            Files.createDirectories(physicalPath);
            
            newFolder = folderRepository.save(newFolder);
            log.info("Created document type folder with ID: {}", newFolder.getId());
            
            return newFolder;
            
        } catch (Exception e) {
            log.error("Error finding/creating document type folder: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Format document type enum to display name for folder
     */
    private String formatDocumentTypeForFolder(DocumentTypeEnum type) {
        switch (type) {
            case SYLLABUS:
                return "Syllabus";
            case EXAM:
                return "Exams";
            case ASSIGNMENT:
                return "Assignments";
            case PROJECT_DOCS:
                return "Project Documents";
            case LECTURE_NOTES:
                return "Course Notes";
            case OTHER:
                return "Other";
            default:
                return type.name();
        }
    }
    
    /**
     * Sanitize filename for storage (more permissive than sanitizeFilename)
     */
    private String sanitizeFilenameForStorage(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "file";
        }
        // Keep most characters, just replace dangerous ones
        return filename.replaceAll("[<>:\"/\\\\|?*]", "_");
    }

    private void deletePhysicalFile(String relativePath) {
        try {
            Path filePath = Paths.get(uploadDirectory, relativePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.debug("Deleted physical file: {}", relativePath);
            } else {
                log.warn("Physical file not found for deletion: {}", relativePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete physical file: {}", relativePath, e);
            // Don't throw exception, just log the error
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "file";
        }

        // Remove any path separators and special characters
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        String nameWithoutExtension = originalFilename;

        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
            nameWithoutExtension = originalFilename.substring(0, lastDotIndex);
        }

        // Generate unique filename using UUID and timestamp
        String uniquePart = UUID.randomUUID().toString().substring(0, 8) + "_" + System.currentTimeMillis();

        return nameWithoutExtension + "_" + uniquePart + extension;
    }

    /**
     * Check if user can read a file.
     * Permission rules (Requirements 5.5, 7.5, 9.4):
     * - Deanship: can read all files across all departments
     * - HOD: can read files only within their department (read-only)
     * - Professor: can read files within their department (own and colleagues)
     * 
     * @param file the file to check
     * @param user the user requesting access
     * @return true if user has read permission, false otherwise
     */
    private boolean canReadFile(UploadedFile file, User user) {
        if (user == null) {
            log.warn("Cannot check read permission for null user");
            return false;
        }

        // Deanship can read all files
        if (user.getRole() == Role.ROLE_DEANSHIP) {
            log.debug("Deanship user {} has read access to all files", user.getEmail());
            return true;
        }

        // Determine file owner
        User fileOwner = null;
        if (file.getUploader() != null) {
            fileOwner = file.getUploader();
        } else if (file.getDocumentSubmission() != null) {
            fileOwner = file.getDocumentSubmission().getProfessor();
        }

        if (fileOwner == null) {
            log.warn("Cannot determine owner for file {}", file.getId());
            return false;
        }

        // Check if user is in the same department
        if (user.getDepartment() != null && fileOwner.getDepartment() != null) {
            boolean sameDepart = user.getDepartment().getId().equals(fileOwner.getDepartment().getId());
            log.debug("User {} department access check: {}", user.getEmail(), sameDepart);
            return sameDepart;
        }

        log.warn("User {} or file owner has no department assigned", user.getEmail());
        return false;
    }

    /**
     * Check if user can write/upload files for a course assignment.
     * Permission rules (Requirements 5.5, 9.4):
     * - Only professors can write/upload files
     * - Professors can only write to their own course assignments
     * - Deanship and HOD have read-only access
     * 
     * @param courseAssignment the course assignment to check
     * @param user             the user requesting access
     * @return true if user has write permission, false otherwise
     */
    private boolean canWriteToCourseAssignment(CourseAssignment courseAssignment, User user) {
        if (user == null) {
            log.warn("Cannot check write permission for null user");
            return false;
        }

        if (user.getRole() != Role.ROLE_PROFESSOR) {
            log.debug("User {} with role {} cannot write files (only professors can write)",
                    user.getEmail(), user.getRole());
            return false;
        }

        // Professor can only write to their own course assignments
        boolean isOwnCourse = courseAssignment.getProfessor().getId().equals(user.getId());
        log.debug("User {} write access to course assignment {}: {}",
                user.getEmail(), courseAssignment.getId(), isOwnCourse);
        return isOwnCourse;
    }

    /**
     * Check if user can delete a file.
     * Permission rules (Requirements 5.5, 7.5, 9.4):
     * - Only professors can delete files
     * - Professors can only delete their own files
     * - Deanship and HOD cannot delete any files
     * - Additional deadline checks may apply (not implemented here)
     * 
     * @param file the file to check
     * @param user the user requesting access
     * @return true if user has delete permission, false otherwise
     */
    private boolean canDeleteFile(UploadedFile file, User user) {
        if (user == null) {
            log.warn("Cannot check delete permission for null user");
            return false;
        }

        if (user.getRole() != Role.ROLE_PROFESSOR) {
            log.debug("User {} with role {} cannot delete files (only professors can delete)",
                    user.getEmail(), user.getRole());
            return false;
        }

        // Determine file owner
        User fileOwner = null;
        if (file.getUploader() != null) {
            fileOwner = file.getUploader();
        } else if (file.getDocumentSubmission() != null) {
            fileOwner = file.getDocumentSubmission().getProfessor();
        }

        if (fileOwner == null) {
            log.warn("Cannot determine owner for file {}", file.getId());
            return false;
        }

        // Professor can only delete their own files
        boolean isOwnFile = fileOwner.getId().equals(user.getId());
        log.debug("User {} delete access to file {}: {}",
                user.getEmail(), file.getId(), isOwnFile);
        return isOwnFile;
    }

    /**
     * Get the current authenticated user.
     */
    private User getCurrentUser() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    // ========== Public Permission Checking Methods ==========

    @Override
    public boolean canUserReadFile(Long fileId, User user) {
        if (user == null || fileId == null) {
            return false;
        }

        try {
            UploadedFile file = uploadedFileRepository.findById(fileId)
                    .orElse(null);

            if (file == null) {
                log.warn("File not found with ID: {}", fileId);
                return false;
            }

            return canReadFile(file, user);
        } catch (Exception e) {
            log.error("Error checking read permission for file {}: {}", fileId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean canUserWriteToCourseAssignment(Long courseAssignmentId, User user) {
        if (user == null || courseAssignmentId == null) {
            return false;
        }

        try {
            CourseAssignment courseAssignment = courseAssignmentRepository.findById(courseAssignmentId)
                    .orElse(null);

            if (courseAssignment == null) {
                log.warn("Course assignment not found with ID: {}", courseAssignmentId);
                return false;
            }

            return canWriteToCourseAssignment(courseAssignment, user);
        } catch (Exception e) {
            log.error("Error checking write permission for course assignment {}: {}",
                    courseAssignmentId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean canUserDeleteFile(Long fileId, User user) {
        if (user == null || fileId == null) {
            return false;
        }

        try {
            UploadedFile file = uploadedFileRepository.findById(fileId)
                    .orElse(null);

            if (file == null) {
                log.warn("File not found with ID: {}", fileId);
                return false;
            }

            return canDeleteFile(file, user);
        } catch (Exception e) {
            log.error("Error checking delete permission for file {}: {}", fileId, e.getMessage());
            return false;
        }
    }
}
