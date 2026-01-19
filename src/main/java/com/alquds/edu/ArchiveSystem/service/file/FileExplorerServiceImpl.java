package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.CourseRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear;
import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.repository.file.FolderRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.entity.academic.CourseAssignment;
import com.alquds.edu.ArchiveSystem.entity.file.FolderType;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.repository.academic.AcademicYearRepository;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.alquds.edu.ArchiveSystem.entity.academic.SemesterType;
import com.alquds.edu.ArchiveSystem.repository.file.UploadedFileRepository;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.repository.academic.SemesterRepository;
import com.alquds.edu.ArchiveSystem.entity.file.Folder;

import com.alquds.edu.ArchiveSystem.dto.fileexplorer.BreadcrumbItem;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.CreateFolderRequest;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.CreateFolderResponse;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.DeleteFolderRequest;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.DeleteFolderResponse;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.NodeType;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.UploadedFileDTO;

import com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException;
import com.alquds.edu.ArchiveSystem.exception.file.FolderAlreadyExistsException;
import com.alquds.edu.ArchiveSystem.exception.file.InvalidFolderNameException;

import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
@SuppressWarnings("null")
public class FileExplorerServiceImpl implements FileExplorerService {

    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final CourseRepository courseRepository;
    private final DocumentSubmissionRepository documentSubmissionRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final FolderRepository folderRepository;
    private final FileAccessService fileAccessService;
    private final com.alquds.edu.ArchiveSystem.util.SafePathResolver safePathResolver;
    
    @Value("${app.upload.base-path:uploads/}")
    private String uploadBasePath;

    @Override
    public FileExplorerNode getRootNode(Long academicYearId, Long semesterId, User currentUser) {
        log.debug("Getting root node for academicYear={}, semester={}, user={}",
                academicYearId, semesterId, currentUser.getEmail());

        // Validate academic year and semester
        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found with id: " + academicYearId));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found with id: " + semesterId));

        // Verify semester belongs to academic year
        if (!semester.getAcademicYear().getId().equals(academicYearId)) {
            throw new IllegalArgumentException("Semester does not belong to the specified academic year");
        }

        // Build root path
        String rootPath = "/" + academicYear.getYearCode() + "/" + semester.getType().name().toLowerCase();

        // Create root node
        FileExplorerNode rootNode = FileExplorerNode.builder()
                .path(rootPath)
                .name(academicYear.getYearCode() + " - " + formatSemesterType(semester.getType()))
                .type(NodeType.SEMESTER)
                .entityId(semesterId)
                .canRead(true)
                .canWrite(false)
                .canDelete(false)
                .build();

        // Add metadata
        rootNode.getMetadata().put("academicYearId", academicYearId);
        rootNode.getMetadata().put("semesterId", semesterId);
        rootNode.getMetadata().put("yearCode", academicYear.getYearCode());
        rootNode.getMetadata().put("semesterType", semester.getType().name());

        // Get professors based on role
        List<User> professors = getProfessorsForUser(semesterId, currentUser);

        // Build professor nodes
        List<FileExplorerNode> professorNodes = professors.stream()
                .map(professor -> buildProfessorNode(rootPath, professor, currentUser))
                .collect(Collectors.toList());

        rootNode.setChildren(professorNodes);

        log.debug("Root node built with {} professor nodes", professorNodes.size());
        return rootNode;
    }

    /**
     * Get professors based on user role:
     * - Admin: all active professors (uses FileAccessService for admin-level check)
     * - Deanship: all active professors
     * - HOD: active professors in HOD's department only
     * - Professor: active professors in same department
     */
    private List<User> getProfessorsForUser(Long semesterId, User currentUser) {
        List<User> professors;

        // Use FileAccessService to check for admin-level access (Admin or Dean)
        if (fileAccessService.hasAdminLevelAccess(currentUser)) {
            // Admin and Dean see all active professors
            professors = userRepository.findByRole(Role.ROLE_PROFESSOR).stream()
                    .filter(User::getIsActive)
                    .collect(Collectors.toList());
            log.debug("Admin-level user ({}) - returning all {} active professors", 
                    currentUser.getRole(), professors.size());
        } else {
            // Apply role-based filtering for non-admin users
            switch (currentUser.getRole()) {
                case ROLE_HOD:
                    // HOD sees only professors in their department
                    if (currentUser.getDepartment() != null) {
                        professors = userRepository.findActiveProfessorsByDepartment(
                                currentUser.getDepartment().getId(), Role.ROLE_PROFESSOR);
                        log.debug("HOD user - filtered to {} active professors in department", professors.size());
                    } else {
                        log.warn("HOD user has no department assigned");
                        fileAccessService.logAccessDenial(currentUser, null, "HOD has no department assigned");
                        professors = new ArrayList<>();
                    }
                    break;

                case ROLE_PROFESSOR:
                    // Professor sees all professors in same department
                    if (currentUser.getDepartment() != null) {
                        professors = userRepository.findActiveProfessorsByDepartment(
                                currentUser.getDepartment().getId(), Role.ROLE_PROFESSOR);
                        log.debug("Professor user - filtered to {} active professors in department", professors.size());
                    } else {
                        log.warn("Professor user has no department assigned");
                        professors = new ArrayList<>();
                    }
                    break;

                default:
                    log.warn("Unknown role: {}", currentUser.getRole());
                    professors = new ArrayList<>();
            }
        }

        // Sort by name
        professors.sort(Comparator.comparing(User::getFirstName)
                .thenComparing(User::getLastName));

        return professors;
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

    /**
     * Build a professor node
     */
    private FileExplorerNode buildProfessorNode(String parentPath, User professor, User currentUser) {
        // Use professor's full name for folder path (sanitized for filesystem)
        String professorFolderName = generateProfessorFolderName(professor);
        log.debug("Using professor folder name: {} for professor {} {}", 
                professorFolderName, professor.getFirstName(), professor.getLastName());
        
        String professorPath = parentPath + "/" + professorFolderName;

        boolean isOwnProfile = professor.getId().equals(currentUser.getId());

        FileExplorerNode node = FileExplorerNode.builder()
                .path(professorPath)
                .name(professor.getFirstName() + " " + professor.getLastName())
                .type(NodeType.PROFESSOR)
                .entityId(professor.getId())
                .canRead(true)
                .canWrite(isOwnProfile && currentUser.getRole() == Role.ROLE_PROFESSOR)
                .canDelete(false)
                .build();

        // Add metadata
        node.getMetadata().put("professorId", professor.getProfessorId());
        node.getMetadata().put("professorFolderName", professorFolderName);
        node.getMetadata().put("email", professor.getEmail());
        node.getMetadata().put("departmentId",
                professor.getDepartment() != null ? professor.getDepartment().getId() : null);
        node.getMetadata().put("departmentName",
                professor.getDepartment() != null ? professor.getDepartment().getName() : null);
        node.getMetadata().put("isOwnProfile", isOwnProfile);

        return node;
    }

    /**
     * Format semester type for display
     */
    private String formatSemesterType(SemesterType type) {
        switch (type) {
            case FIRST:
                return "First Semester";
            case SECOND:
                return "Second Semester";
            case SUMMER:
                return "Summer Semester";
            default:
                return type.name();
        }
    }

    /**
     * Find professor by identifier - supports multiple formats:
     * 1. Professor's full name (new format): "firstName lastName"
     * 2. Legacy fallback format: "prof_<id>"
     * 3. Legacy professorId field value
     * 
     * @param professorIdentifier The professor identifier from path
     * @return The professor User entity
     * @throws EntityNotFoundException if professor not found
     */
    private User findProfessorByIdentifier(String professorIdentifier) {
        return findProfessorByIdentifierOptional(professorIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Professor not found: " + professorIdentifier));
    }
    
    /**
     * Find professor by identifier - supports multiple formats:
     * 1. Professor's full name (new format): "firstName lastName"
     * 2. Legacy fallback format: "prof_<id>"
     * 3. Legacy professorId field value
     * 
     * @param professorIdentifier The professor identifier from path
     * @return Optional containing the professor User entity, or empty if not found
     */
    private Optional<User> findProfessorByIdentifierOptional(String professorIdentifier) {
        if (professorIdentifier == null || professorIdentifier.isEmpty() || "null".equals(professorIdentifier)) {
            log.warn("Invalid professor identifier: {}", professorIdentifier);
            return Optional.empty();
        }
        
        // 1. Check if this is a legacy fallback identifier (prof_<id>)
        if (professorIdentifier.startsWith("prof_")) {
            try {
                Long userId = Long.parseLong(professorIdentifier.substring(5));
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    log.debug("Found professor by legacy fallback ID: {}", professorIdentifier);
                    return userOpt;
                }
            } catch (NumberFormatException e) {
                // Not a valid prof_<id> format, continue to other lookups
            }
        }
        
        // 2. Try looking up by professorId field
        Optional<User> byProfessorId = userRepository.findByProfessorId(professorIdentifier);
        if (byProfessorId.isPresent()) {
            log.debug("Found professor by professorId: {}", professorIdentifier);
            return byProfessorId;
        }
        
        // 3. Try looking up by name (new format) - search for professors whose generated folder name matches
        List<User> allProfessors = userRepository.findByRole(Role.ROLE_PROFESSOR);
        for (User professor : allProfessors) {
            String folderName = generateProfessorFolderName(professor);
            if (folderName.equals(professorIdentifier)) {
                log.debug("Found professor by folder name match: {} -> {} {}", 
                        professorIdentifier, professor.getFirstName(), professor.getLastName());
                return Optional.of(professor);
            }
        }
        
        log.warn("Could not find professor for identifier: {}", professorIdentifier);
        return Optional.empty();
    }

    @Override
    public FileExplorerNode getNode(String nodePath, User currentUser) {
        log.debug("Getting node for path={}, user={}", nodePath, currentUser.getEmail());

        // Check read permission
        if (!canRead(nodePath, currentUser)) {
            throw new UnauthorizedOperationException("User does not have permission to access this path");
        }

        // Parse path to determine node type and fetch appropriate data
        PathInfo pathInfo = parsePath(nodePath);

        FileExplorerNode node;

        switch (pathInfo.getType()) {
            case YEAR:
                node = buildYearNode(pathInfo, currentUser);
                break;
            case SEMESTER:
                node = buildSemesterNode(pathInfo, currentUser);
                break;
            case PROFESSOR:
                node = buildProfessorNodeFromPath(pathInfo, currentUser);
                break;
            case COURSE:
                node = buildCourseNode(pathInfo, currentUser);
                break;
            case DOCUMENT_TYPE:
                node = buildDocumentTypeNode(pathInfo, currentUser);
                break;
            case CUSTOM:
                node = buildCustomFolderNode(pathInfo, currentUser);
                break;
            default:
                throw new IllegalArgumentException("Invalid node path: " + nodePath);
        }

        // Fetch children
        node.setChildren(getChildren(nodePath, currentUser));

        return node;
    }

    @Override
    public List<FileExplorerNode> getChildren(String parentPath, User currentUser) {
        log.debug("Getting children for path={}, user={}", parentPath, currentUser.getEmail());

        // Check read permission
        if (!canRead(parentPath, currentUser)) {
            throw new UnauthorizedOperationException("User does not have permission to access this path");
        }

        PathInfo pathInfo = parsePath(parentPath);

        switch (pathInfo.getType()) {
            case YEAR:
                return getSemesterChildren(pathInfo, currentUser);
            case SEMESTER:
                return getProfessorChildren(pathInfo, currentUser);
            case PROFESSOR:
                return getCourseChildren(pathInfo, currentUser);
            case COURSE:
                return getDocumentTypeChildren(pathInfo, currentUser);
            case DOCUMENT_TYPE:
                return getFileChildren(pathInfo, currentUser);
            case CUSTOM:
                return getCustomFolderChildren(pathInfo, currentUser);
            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * Get children of a custom folder (only sub-folders, not files).
     * Files are included separately in the node.files property via buildCustomFolderNode().
     * This prevents duplicate file display in the frontend.
     * 
     * Also scans the filesystem for physical folders that don't have DB records
     * and creates DB records for them automatically.
     */
    @Transactional
    private List<FileExplorerNode> getCustomFolderChildren(PathInfo pathInfo, User currentUser) {
        // Find professor
        User professor = findProfessorByIdentifier(pathInfo.getProfessorId());
        
        // Find academic context
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));
        SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());
        Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));
        
        // Find course
        Course course = courseRepository.findByCourseCode(pathInfo.getCourseCode())
                .orElseThrow(() -> new EntityNotFoundException("Course not found: " + pathInfo.getCourseCode()));
        
        // Find the custom folder - prefer ID-based lookup
        Optional<Folder> customFolderOpt = Optional.empty();
        
        // Try to find by ID first (new format: custom-{id})
        if (pathInfo.getCustomFolderId() != null) {
            customFolderOpt = folderRepository.findById(pathInfo.getCustomFolderId());
        }
        
        // Fallback to name-based lookup
        if (!customFolderOpt.isPresent()) {
            Optional<Folder> courseFolderOpt = folderRepository.findCourseFolder(
                    professor.getId(), course.getId(), academicYear.getId(), semester.getId(), FolderType.COURSE);
            
            if (!courseFolderOpt.isPresent()) {
                log.warn("Course folder not found for path: {}", pathInfo.getPath());
                return new ArrayList<>();
            }
            
            Folder courseFolder = courseFolderOpt.get();
            String customFolderName = pathInfo.getCustomFolderName();
            customFolderOpt = folderRepository.findByNameAndParentId(customFolderName, courseFolder.getId());
            
            if (!customFolderOpt.isPresent()) {
                // Try URL-decoded name
                try {
                    String decodedName = java.net.URLDecoder.decode(customFolderName, "UTF-8").replace("-", " ");
                    customFolderOpt = folderRepository.findByNameAndParentId(decodedName, courseFolder.getId());
                } catch (Exception e) {
                    log.warn("Failed to decode folder name: {}", customFolderName);
                }
            }
        }
        
        Folder customFolder;
        if (!customFolderOpt.isPresent()) {
            // Check if folder exists physically but not in DB
            String expectedPath = pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() +
                    "/" + pathInfo.getProfessorId() + "/" + pathInfo.getCourseCode() +
                    "/" + pathInfo.getCustomFolderName();
            
            Path physicalPath = Paths.get(uploadBasePath, expectedPath);
            if (Files.exists(physicalPath) && Files.isDirectory(physicalPath)) {
                // Physical folder exists but no DB record - create it
                log.info("Physical folder found without DB record, creating: {}", expectedPath);
                customFolder = createFolderRecordForPhysicalFolder(
                        expectedPath, pathInfo.getCustomFolderName(), professor, course, academicYear, semester);
            } else {
                log.warn("Custom folder not found: {} (ID: {})", pathInfo.getCustomFolderName(), pathInfo.getCustomFolderId());
                return new ArrayList<>();
            }
        } else {
            customFolder = customFolderOpt.get();
        }
        
        // Use ID-based path for consistency
        String parentPath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() +
                "/" + pathInfo.getProfessorId() + "/" + pathInfo.getCourseCode() +
                "/custom-" + customFolder.getId();
        
        boolean isOwnFolder = professor.getId().equals(currentUser.getId());
        
        // Get sub-folders from database
        List<Folder> childFolders = folderRepository.findByParentId(customFolder.getId());
        
        // Scan filesystem for physical folders that don't have DB records
        String customFolderPath = customFolder.getPath();
        Path physicalCustomFolderPath = Paths.get(uploadBasePath, customFolderPath);
        
        Set<String> dbFolderNames = childFolders.stream()
                .map(Folder::getName)
                .collect(Collectors.toSet());
        
        if (Files.exists(physicalCustomFolderPath) && Files.isDirectory(physicalCustomFolderPath)) {
            try (var folderStream = Files.list(physicalCustomFolderPath)) {
                List<Path> directories = folderStream
                        .filter(Files::isDirectory)
                        .collect(Collectors.toList());
                
                for (Path entry : directories) {
                    String folderName = entry.getFileName().toString();
                    
                    // Skip if already in database
                    if (!dbFolderNames.contains(folderName)) {
                        // Physical folder exists but no DB record - create it
                        log.info("Physical subfolder found without DB record, creating: {}/{}", customFolderPath, folderName);
                        String subfolderPath = customFolderPath + "/" + folderName;
                        createFolderRecordForPhysicalFolder(
                                subfolderPath, folderName, professor, course, academicYear, semester, customFolder);
                    }
                }
                
                // Refresh the list after creating new records
                childFolders = folderRepository.findByParentId(customFolder.getId());
            } catch (IOException e) {
                log.warn("Error scanning physical folder for subfolders: {}", customFolderPath, e);
            }
        }
        
        return childFolders.stream()
                .map(childFolder -> {
                    // Use ID-based path for child folders too
                    String folderPath = parentPath + "/custom-" + childFolder.getId();
                    
                    FileExplorerNode node = FileExplorerNode.builder()
                            .path(folderPath)
                            .name(childFolder.getName())
                            .type(NodeType.CUSTOM)
                            .entityId(childFolder.getId())
                            .canRead(true)
                            .canWrite(isOwnFolder && currentUser.getRole() == Role.ROLE_PROFESSOR)
                            .canDelete(isOwnFolder && currentUser.getRole() == Role.ROLE_PROFESSOR)
                            .build();
                    
                    node.getMetadata().put("folderId", childFolder.getId());
                    node.getMetadata().put("folderName", childFolder.getName());
                    node.getMetadata().put("isOwnFolder", isOwnFolder);
                    node.getMetadata().put("isCustomFolder", true);
                    
                    return node;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Create a database record for a physical folder that exists on disk but not in DB.
     * This handles the case when an admin adds folders directly to the filesystem.
     */
    @Transactional
    private Folder createFolderRecordForPhysicalFolder(
            String folderPath, String folderName, User professor, Course course,
            AcademicYear academicYear, Semester semester) {
        return createFolderRecordForPhysicalFolder(folderPath, folderName, professor, course, academicYear, semester, null);
    }
    
    /**
     * Create a database record for a physical folder that exists on disk but not in DB.
     * This handles the case when an admin adds folders directly to the filesystem.
     */
    @Transactional
    private Folder createFolderRecordForPhysicalFolder(
            String folderPath, String folderName, User professor, Course course,
            AcademicYear academicYear, Semester semester, Folder parentFolder) {
        
        // Find parent folder if not provided
        if (parentFolder == null) {
            // Extract parent path
            int lastSlash = folderPath.lastIndexOf('/');
            if (lastSlash > 0) {
                String parentPath = folderPath.substring(0, lastSlash);
                parentFolder = folderRepository.findByPath(parentPath).orElse(null);
            }
        }
        
        Folder folder = Folder.builder()
                .path(folderPath)
                .name(folderName)
                .type(FolderType.CUSTOM)
                .parent(parentFolder)
                .owner(professor)
                .academicYear(academicYear)
                .semester(semester)
                .course(course)
                .build();
        
        folder = folderRepository.save(folder);
        log.info("Created DB record for physical folder: {} (ID: {})", folderPath, folder.getId());
        
        return folder;
    }

    /**
     * Parse path to extract components and determine node type
     * Path format: /yearCode/semesterType/professorId/courseCode/documentType
     * Custom folders at level 5 are detected by checking if the segment matches a known document type
     */
    private PathInfo parsePath(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            throw new IllegalArgumentException("Invalid path");
        }

        // Remove leading/trailing slashes
        path = path.replaceAll("^/+|/+$", "");

        String[] parts = path.split("/");

        PathInfo info = new PathInfo();
        info.setPath(path);

        if (parts.length >= 1) {
            info.setYearCode(parts[0]);
            info.setType(NodeType.YEAR);
        }

        if (parts.length >= 2) {
            info.setSemesterType(parts[1]);
            info.setType(NodeType.SEMESTER);
        }

        if (parts.length >= 3) {
            info.setProfessorId(parts[2]);
            info.setType(NodeType.PROFESSOR);
        }

        if (parts.length >= 4) {
            info.setCourseCode(parts[3]);
            info.setType(NodeType.COURSE);
        }

        if (parts.length >= 5) {
            String fifthPart = parts[4];
            info.setDocumentType(fifthPart);
            
            // Check if this is a custom folder (format: custom-{id}) or a known document type
            if (fifthPart.startsWith("custom-")) {
                // Custom folder identified by ID
                info.setType(NodeType.CUSTOM);
                info.setCustomFolderName(fifthPart);
                // Extract folder ID for lookup
                try {
                    String idStr = fifthPart.substring(7); // Remove "custom-" prefix
                    info.setCustomFolderId(Long.parseLong(idStr));
                } catch (NumberFormatException e) {
                    // Fall back to name-based lookup
                    log.warn("Invalid custom folder ID format: {}", fifthPart);
                }
            } else if (isKnownDocumentType(fifthPart)) {
                info.setType(NodeType.DOCUMENT_TYPE);
            } else {
                // Legacy: custom folder identified by name (for backward compatibility)
                info.setType(NodeType.CUSTOM);
                info.setCustomFolderName(fifthPart);
            }
        }

        if (parts.length >= 6) {
            // File ID is the 6th part
            info.setType(NodeType.FILE);
        }

        return info;
    }
    
    /**
     * Check if a path segment corresponds to a known document type
     */
    private boolean isKnownDocumentType(String segment) {
        if (segment == null) return false;
        
        // Check direct enum match
        try {
            DocumentTypeEnum.valueOf(segment.toUpperCase().replace("-", "_"));
            return true;
        } catch (IllegalArgumentException e) {
            // Not a direct enum match, check mapped names
        }
        
        // Check mapped folder names
        String normalized = segment.toLowerCase().replace("-", " ");
        switch (normalized) {
            case "syllabus":
            case "exams":
            case "exam":
            case "course notes":
            case "lecture notes":
            case "assignments":
            case "assignment":
                return true;
            default:
                return false;
        }
    }

    /**
     * Build semester node from path info
     */
    private FileExplorerNode buildSemesterNode(PathInfo pathInfo, User currentUser) {
        // Find academic year by year code
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));

        // Find semester by type
        SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());
        Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));

        String nodePath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType();

        FileExplorerNode node = FileExplorerNode.builder()
                .path(nodePath)
                .name(academicYear.getYearCode() + " - " + formatSemesterType(semesterType))
                .type(NodeType.SEMESTER)
                .entityId(semester.getId())
                .canRead(true)
                .canWrite(false)
                .canDelete(false)
                .build();

        node.getMetadata().put("academicYearId", academicYear.getId());
        node.getMetadata().put("semesterId", semester.getId());

        return node;
    }

    /**
     * Build professor node from path info
     */
    private FileExplorerNode buildProfessorNodeFromPath(PathInfo pathInfo, User currentUser) {
        User professor = findProfessorByIdentifier(pathInfo.getProfessorId());

        String nodePath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() + "/"
                + pathInfo.getProfessorId();

        return buildProfessorNode(nodePath.substring(0, nodePath.lastIndexOf("/")), professor, currentUser);
    }

    /**
     * Build course node from path info
     */
    private FileExplorerNode buildCourseNode(PathInfo pathInfo, User currentUser) {
        // Find professor
        User professor = findProfessorByIdentifier(pathInfo.getProfessorId());

        // Find semester
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));
        SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());
        Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));

        // Find course assignment
        CourseAssignment assignment = courseAssignmentRepository
                .findBySemesterIdAndCourseCodeAndProfessorId(semester.getId(), pathInfo.getCourseCode(),
                        professor.getId())
                .orElseThrow(() -> new EntityNotFoundException("Course assignment not found"));

        String nodePath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() +
                "/" + pathInfo.getProfessorId() + "/" + pathInfo.getCourseCode();

        boolean isOwnCourse = professor.getId().equals(currentUser.getId());

        FileExplorerNode node = FileExplorerNode.builder()
                .path(nodePath)
                .name(assignment.getCourse().getCourseCode() + " - " + assignment.getCourse().getCourseName())
                .type(NodeType.COURSE)
                .entityId(assignment.getId())
                .canRead(true)
                .canWrite(isOwnCourse && currentUser.getRole() == Role.ROLE_PROFESSOR)
                .canDelete(false)
                .build();

        node.getMetadata().put("courseId", assignment.getCourse().getId());
        node.getMetadata().put("courseCode", assignment.getCourse().getCourseCode());
        node.getMetadata().put("courseName", assignment.getCourse().getCourseName());
        node.getMetadata().put("assignmentId", assignment.getId());
        node.getMetadata().put("isOwnCourse", isOwnCourse);

        return node;
    }

    /**
     * Build document type node from path info
     */
    private FileExplorerNode buildDocumentTypeNode(PathInfo pathInfo, User currentUser) {
        // Find professor
        User professor = findProfessorByIdentifier(pathInfo.getProfessorId());

        // Find semester
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));
        SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());
        Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));

        // Find course assignment
        CourseAssignment assignment = courseAssignmentRepository
                .findBySemesterIdAndCourseCodeAndProfessorId(semester.getId(), pathInfo.getCourseCode(),
                        professor.getId())
                .orElseThrow(() -> new EntityNotFoundException("Course assignment not found"));

        // Parse document type from path - handle both enum names and URL-safe formats
        DocumentTypeEnum docType;
        try {
            // Try direct enum parsing first (for backward compatibility)
            docType = DocumentTypeEnum.valueOf(pathInfo.getDocumentType().toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            // If that fails, try mapping from folder name
            docType = mapPathSegmentToDocumentType(pathInfo.getDocumentType());
            if (docType == null) {
                throw new IllegalArgumentException("Invalid document type: " + pathInfo.getDocumentType());
            }
        }

        String nodePath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() +
                "/" + pathInfo.getProfessorId() + "/" + pathInfo.getCourseCode() +
                "/" + pathInfo.getDocumentType();

        boolean isOwnCourse = professor.getId().equals(currentUser.getId());

        FileExplorerNode node = FileExplorerNode.builder()
                .path(nodePath)
                .name(formatDocumentType(docType))
                .type(NodeType.DOCUMENT_TYPE)
                .entityId(null) // No specific entity ID for document type folder
                .canRead(true)
                .canWrite(isOwnCourse && currentUser.getRole() == Role.ROLE_PROFESSOR)
                .canDelete(false)
                .build();

        node.getMetadata().put("documentType", docType.name());
        node.getMetadata().put("assignmentId", assignment.getId());
        node.getMetadata().put("isOwnCourse", isOwnCourse);

        // Query and include files for this document type folder
        List<UploadedFileDTO> files = getFilesForDocumentType(pathInfo, currentUser);
        node.setFiles(files);

        return node;
    }

    /**
     * Map URL path segment to DocumentTypeEnum
     * Handles both enum names (lecture_notes) and folder names (course-notes)
     */
    private DocumentTypeEnum mapPathSegmentToDocumentType(String pathSegment) {
        if (pathSegment == null) {
            return null;
        }

        String normalized = pathSegment.toLowerCase().replace("-", "_");

        switch (normalized) {
            case "syllabus":
                return DocumentTypeEnum.SYLLABUS;
            case "exams":
            case "exam":
                return DocumentTypeEnum.EXAM;
            case "course_notes":
            case "lecture_notes":
                return DocumentTypeEnum.LECTURE_NOTES;
            case "assignments":
            case "assignment":
                return DocumentTypeEnum.ASSIGNMENT;
            default:
                return null;
        }
    }
    
    /**
     * Build custom folder node from path info
     * Custom folders are created by professors inside their course folders
     */
    private FileExplorerNode buildCustomFolderNode(PathInfo pathInfo, User currentUser) {
        // Find professor
        User professor = findProfessorByIdentifier(pathInfo.getProfessorId());
        
        // Find academic context
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));
        SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());
        Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));
        
        // Find course
        Course course = courseRepository.findByCourseCode(pathInfo.getCourseCode())
                .orElseThrow(() -> new EntityNotFoundException("Course not found: " + pathInfo.getCourseCode()));
        
        // Find the custom folder - prefer ID-based lookup for uniqueness
        Optional<Folder> customFolderOpt = Optional.empty();
        
        // Try to find by ID first (new format: custom-{id})
        if (pathInfo.getCustomFolderId() != null) {
            customFolderOpt = folderRepository.findById(pathInfo.getCustomFolderId());
            // Verify the folder belongs to this course context
            if (customFolderOpt.isPresent()) {
                Folder folder = customFolderOpt.get();
                if (folder.getCourse() == null || !folder.getCourse().getId().equals(course.getId())) {
                    customFolderOpt = Optional.empty();
                    log.warn("Folder ID {} does not belong to course {}", pathInfo.getCustomFolderId(), course.getCourseCode());
                }
            }
        }
        
        // Fallback to name-based lookup for backward compatibility
        if (!customFolderOpt.isPresent()) {
            // Find course folder first
            Optional<Folder> courseFolderOpt = folderRepository.findCourseFolder(
                    professor.getId(), course.getId(), academicYear.getId(), semester.getId(), FolderType.COURSE);
            
            if (!courseFolderOpt.isPresent()) {
                throw new EntityNotFoundException("Course folder not found");
            }
            
            Folder courseFolder = courseFolderOpt.get();
            String customFolderName = pathInfo.getCustomFolderName();
            customFolderOpt = folderRepository.findByNameAndParentId(customFolderName, courseFolder.getId());
            
            if (!customFolderOpt.isPresent()) {
                // Try URL-decoded name with spaces instead of dashes
                try {
                    String decodedName = java.net.URLDecoder.decode(customFolderName, "UTF-8").replace("-", " ");
                    customFolderOpt = folderRepository.findByNameAndParentId(decodedName, courseFolder.getId());
                } catch (Exception e) {
                    log.warn("Failed to decode folder name: {}", customFolderName);
                }
            }
        }
        
        Folder customFolder;
        if (!customFolderOpt.isPresent()) {
            // Check if folder exists physically but not in DB
            String expectedPath = pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() +
                    "/" + pathInfo.getProfessorId() + "/" + pathInfo.getCourseCode() +
                    "/" + pathInfo.getCustomFolderName();
            
            Path physicalPath = Paths.get(uploadBasePath, expectedPath);
            if (Files.exists(physicalPath) && Files.isDirectory(physicalPath)) {
                // Physical folder exists but no DB record - create it
                log.info("Physical folder found without DB record in buildCustomFolderNode, creating: {}", expectedPath);
                customFolder = createFolderRecordForPhysicalFolder(
                        expectedPath, pathInfo.getCustomFolderName(), professor, course, academicYear, semester);
            } else {
                throw new EntityNotFoundException("Custom folder not found: " + pathInfo.getCustomFolderName());
            }
        } else {
            customFolder = customFolderOpt.get();
        }
        
        // Use the ID-based path format for consistency
        String nodePath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() +
                "/" + pathInfo.getProfessorId() + "/" + pathInfo.getCourseCode() +
                "/custom-" + customFolder.getId();
        
        boolean isOwnFolder = professor.getId().equals(currentUser.getId());
        
        FileExplorerNode node = FileExplorerNode.builder()
                .path(nodePath)
                .name(customFolder.getName())
                .type(NodeType.CUSTOM)
                .entityId(customFolder.getId())
                .canRead(true)
                .canWrite(isOwnFolder && currentUser.getRole() == Role.ROLE_PROFESSOR)
                .canDelete(isOwnFolder && currentUser.getRole() == Role.ROLE_PROFESSOR)
                .build();
        
        node.getMetadata().put("folderId", customFolder.getId());
        node.getMetadata().put("folderName", customFolder.getName());
        node.getMetadata().put("isOwnFolder", isOwnFolder);
        node.getMetadata().put("isCustomFolder", true);
        
        // Get files in this custom folder from database
        List<UploadedFile> dbFiles = uploadedFileRepository.findByFolderIdWithUploader(customFolder.getId());
        final User finalUser = currentUser;
        List<UploadedFileDTO> fileDTOs = dbFiles.stream()
                .map(f -> convertToUploadedFileDTO(f, finalUser))
                .collect(Collectors.toList());
        
        // Also scan filesystem for physical files that don't have DB records
        String customFolderPath = customFolder.getPath();
        Path physicalCustomFolderPath = Paths.get(uploadBasePath, customFolderPath);
        
        Set<String> dbFilePaths = dbFiles.stream()
                .map(UploadedFile::getFileUrl)
                .filter(url -> url != null)
                .collect(Collectors.toSet());
        
        if (Files.exists(physicalCustomFolderPath) && Files.isDirectory(physicalCustomFolderPath)) {
            try (var fileStream = Files.list(physicalCustomFolderPath)) {
                fileStream.forEach(entry -> {
                    if (Files.isRegularFile(entry)) {
                        String relativePath = customFolderPath + "/" + entry.getFileName().toString();
                        
                        // Skip if already in database
                        if (!dbFilePaths.contains(relativePath)) {
                            // Physical file exists but no DB record - create a basic DTO for it
                            try {
                                long fileSize = Files.size(entry);
                                java.nio.file.attribute.FileTime fileTime = Files.getLastModifiedTime(entry);
                                LocalDateTime modifiedAt = LocalDateTime.ofInstant(
                                        fileTime.toInstant(), ZoneId.systemDefault());
                                
                                String fileName = entry.getFileName().toString();
                                String fileType = determineMimeTypeFromFileName(fileName);
                                
                                // Give full permissions to folder owner (professor)
                                boolean hasFullAccess = isOwnFolder && currentUser.getRole() == Role.ROLE_PROFESSOR;
                                
                                UploadedFileDTO orphanedFile = UploadedFileDTO.builder()
                                        .id(null) // No DB ID
                                        .originalFilename(fileName)
                                        .storedFilename(fileName)
                                        .fileUrl(relativePath)
                                        .fileSize(fileSize)
                                        .fileType(fileType)
                                        .createdAt(modifiedAt)
                                        .updatedAt(modifiedAt)
                                        .uploaderName(hasFullAccess ? currentUser.getFirstName() + " " + currentUser.getLastName() : null)
                                        .uploaderId(hasFullAccess ? currentUser.getId() : null)
                                        .notes("File added directly to filesystem")
                                        .orphaned(true) // Mark as orphaned
                                        .canDelete(hasFullAccess)
                                        .canReplace(hasFullAccess)
                                        .build();
                                
                                fileDTOs.add(orphanedFile);
                                log.debug("Found orphaned file: {}", relativePath);
                            } catch (IOException e) {
                                log.warn("Error reading file attributes: {}", entry, e);
                            }
                        }
                    }
                });
            } catch (IOException e) {
                log.warn("Error scanning physical folder for files: {}", customFolderPath, e);
            }
        }
        
        node.setFiles(fileDTOs);
        
        return node;
    }

    /**
     * Get professor children for a semester node
     */
    private List<FileExplorerNode> getProfessorChildren(PathInfo pathInfo, User currentUser) {
        // Find semester
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));
        SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());
        Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));

        String parentPath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType();

        List<User> professors = getProfessorsForUser(semester.getId(), currentUser);

        return professors.stream()
                .map(professor -> buildProfessorNode(parentPath, professor, currentUser))
                .collect(Collectors.toList());
    }

    /**
     * Get course children for a professor node
     */
    private List<FileExplorerNode> getCourseChildren(PathInfo pathInfo, User currentUser) {
        log.debug("=== Getting course children for professor path: {} ===", pathInfo.getPath());

        // Find professor
        User professor = findProfessorByIdentifier(pathInfo.getProfessorId());

        log.debug("Found professor: ID={}, Name={} {}, ProfessorId={}",
                professor.getId(), professor.getFirstName(), professor.getLastName(), professor.getProfessorId());

        // Find semester
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));
        SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());
        Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));

        log.debug("Found semester: ID={}, Type={}, AcademicYear={}",
                semester.getId(), semesterType, academicYear.getYearCode());

        // Get course assignments for this professor in this semester
        List<CourseAssignment> assignments = courseAssignmentRepository
                .findByProfessorIdAndSemesterId(professor.getId(), semester.getId());

        log.debug("Found {} course assignments for professor {} in semester {}",
                assignments.size(), professor.getId(), semester.getId());

        if (assignments.isEmpty()) {
            log.warn("No course assignments found for professor {} (ID: {}) in semester {} (ID: {}). " +
                    "Please verify that course assignments exist in the database for this professor and semester.",
                    professor.getProfessorId(), professor.getId(), semesterType, semester.getId());
        }

        String parentPath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() + "/"
                + pathInfo.getProfessorId();

        boolean isOwnProfile = professor.getId().equals(currentUser.getId());

        return assignments.stream()
                .map(assignment -> {
                    String coursePath = parentPath + "/" + assignment.getCourse().getCourseCode();

                    FileExplorerNode node = FileExplorerNode.builder()
                            .path(coursePath)
                            .name(assignment.getCourse().getCourseCode() + " - "
                                    + assignment.getCourse().getCourseName())
                            .type(NodeType.COURSE)
                            .entityId(assignment.getId())
                            .canRead(true)
                            .canWrite(isOwnProfile && currentUser.getRole() == Role.ROLE_PROFESSOR)
                            .canDelete(false)
                            .build();

                    node.getMetadata().put("courseId", assignment.getCourse().getId());
                    node.getMetadata().put("courseCode", assignment.getCourse().getCourseCode());
                    node.getMetadata().put("courseName", assignment.getCourse().getCourseName());
                    node.getMetadata().put("assignmentId", assignment.getId());
                    node.getMetadata().put("isOwnCourse", isOwnProfile);

                    return node;
                })
                .sorted(Comparator.comparing(FileExplorerNode::getName))
                .collect(Collectors.toList());
    }

    /**
     * Get document type children for a course node
     * Returns folders created by FolderService (Syllabus, Exams, Course Notes,
     * Assignments)
     */
    private List<FileExplorerNode> getDocumentTypeChildren(PathInfo pathInfo, User currentUser) {
        // Find professor
        User professor = findProfessorByIdentifier(pathInfo.getProfessorId());

        // Find semester
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));
        SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());
        Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));

        // Find course assignment
        CourseAssignment assignment = courseAssignmentRepository
                .findBySemesterIdAndCourseCodeAndProfessorId(semester.getId(), pathInfo.getCourseCode(),
                        professor.getId())
                .orElseThrow(() -> new EntityNotFoundException("Course assignment not found"));

        // Find course folder in Folder table
        Optional<Folder> courseFolderOpt = folderRepository.findCourseFolder(
                professor.getId(), assignment.getCourse().getId(),
                academicYear.getId(), semester.getId(), FolderType.COURSE);

        if (!courseFolderOpt.isPresent()) {
            log.warn("Course folder not found for assignment: {}", assignment.getId());
            return new ArrayList<>();
        }

        Folder courseFolder = courseFolderOpt.get();

        // Get all child folders (subfolders) of the course folder
        List<Folder> subfolders = folderRepository.findByParentId(courseFolder.getId());

        String parentPath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() +
                "/" + pathInfo.getProfessorId() + "/" + pathInfo.getCourseCode();

        boolean isOwnCourse = professor.getId().equals(currentUser.getId());

        // Get all document submissions for file count
        List<DocumentSubmission> submissions = documentSubmissionRepository
                .findByCourseAssignmentId(assignment.getId());

        // Filter out custom folders that don't exist on disk anymore
        // and clean up their database records
        List<Folder> validSubfolders = subfolders.stream()
                .filter(subfolder -> {
                    // Always keep non-custom folders (system folders)
                    if (subfolder.getType() != FolderType.CUSTOM) {
                        return true;
                    }
                    // For custom folders, check if they exist on disk
                    return folderExistsOnDisk(subfolder);
                })
                .collect(Collectors.toList());

        return validSubfolders.stream()
                .map(subfolder -> {
                    // Check if this is a custom folder (created by professor)
                    boolean isCustomFolder = subfolder.getType() == FolderType.CUSTOM;
                    
                    // Map folder name to document type enum for URL-safe path (only for document type folders)
                    DocumentTypeEnum docType = isCustomFolder ? null : mapFolderNameToDocumentType(subfolder.getName());
                    
                    // For custom folders, use folder ID in path to ensure uniqueness
                    // For document type folders, use the enum name
                    String pathSegment;
                    if (docType != null) {
                        pathSegment = docType.name().toLowerCase();
                    } else if (isCustomFolder) {
                        // Use "custom-{id}" format for unique identification
                        pathSegment = "custom-" + subfolder.getId();
                    } else {
                        pathSegment = subfolder.getName().toLowerCase().replace(" ", "-");
                    }
                    String docTypePath = parentPath + "/" + pathSegment;

                    // Use CUSTOM node type for custom folders, DOCUMENT_TYPE for standard folders
                    NodeType nodeType = isCustomFolder ? NodeType.CUSTOM : NodeType.DOCUMENT_TYPE;
                    
                    FileExplorerNode node = FileExplorerNode.builder()
                            .path(docTypePath)
                            .name(subfolder.getName())
                            .type(nodeType)
                            .entityId(subfolder.getId())
                            .canRead(true)
                            .canWrite(isOwnCourse && currentUser.getRole() == Role.ROLE_PROFESSOR)
                            .canDelete(isCustomFolder && isOwnCourse && currentUser.getRole() == Role.ROLE_PROFESSOR)
                            .build();

                    node.getMetadata().put("folderId", subfolder.getId());
                    node.getMetadata().put("folderName", subfolder.getName());
                    node.getMetadata().put("assignmentId", assignment.getId());
                    node.getMetadata().put("isOwnCourse", isOwnCourse);
                    node.getMetadata().put("isCustomFolder", isCustomFolder);

                    // Count files in this subfolder
                    if (docType != null) {
                        long fileCount = submissions.stream()
                                .filter(s -> s.getDocumentType() == docType)
                                .mapToLong(s -> s.getUploadedFiles().size())
                                .sum();
                        node.getMetadata().put("fileCount", fileCount);
                        node.getMetadata().put("documentType", docType.name());
                    } else {
                        // For custom folders, count files uploaded to this folder (if any)
                        List<UploadedFile> customFolderFiles = uploadedFileRepository.findByFolderId(subfolder.getId());
                        node.getMetadata().put("fileCount", (long) customFolderFiles.size());
                    }

                    return node;
                })
                .sorted(Comparator.comparing(FileExplorerNode::getName))
                .collect(Collectors.toList());
    }

    /**
     * Map folder name to DocumentTypeEnum for file counting and URL generation
     */
    private DocumentTypeEnum mapFolderNameToDocumentType(String folderName) {
        if (folderName == null) {
            return null;
        }

        switch (folderName.toLowerCase()) {
            case "syllabus":
                return DocumentTypeEnum.SYLLABUS;
            case "exams":
            case "exam":
                return DocumentTypeEnum.EXAM;
            case "course notes":
            case "lecture notes":
                return DocumentTypeEnum.LECTURE_NOTES;
            case "assignments":
            case "assignment":
                return DocumentTypeEnum.ASSIGNMENT;
            default:
                return null;
        }
    }

    /**
     * Get file children for a document type node.
     * Returns an empty list because files are already included in node.files via buildDocumentTypeNode().
     * This prevents duplicate file display in the frontend.
     */
    private List<FileExplorerNode> getFileChildren(PathInfo pathInfo, User currentUser) {
        // Files are now included in node.files via buildDocumentTypeNode()
        // Return empty list to prevent duplicate file display
        return new ArrayList<>();
    }

    /**
     * Format document type for display.
     * IMPORTANT: These must match the standard subfolder names created by FolderServiceImpl.
     * Standard folders: "Syllabus", "Exams", "Course Notes", "Assignments"
     */
    private String formatDocumentType(DocumentTypeEnum type) {
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

    @Override
    public List<BreadcrumbItem> generateBreadcrumbs(String nodePath) {
        log.debug("Generating breadcrumbs for path={}", nodePath);

        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();

        if (nodePath == null || nodePath.isEmpty() || nodePath.equals("/")) {
            return breadcrumbs;
        }

        try {
            PathInfo pathInfo = parsePath(nodePath);

            // Add year breadcrumb
            if (pathInfo.getYearCode() != null) {
                breadcrumbs.add(BreadcrumbItem.builder()
                        .name(pathInfo.getYearCode())
                        .path("/" + pathInfo.getYearCode())
                        .type(NodeType.YEAR)
                        .build());
            }

            // Add semester breadcrumb
            if (pathInfo.getSemesterType() != null) {
                String semesterPath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType();
                SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());

                breadcrumbs.add(BreadcrumbItem.builder()
                        .name(formatSemesterType(semesterType))
                        .path(semesterPath)
                        .type(NodeType.SEMESTER)
                        .build());
            }

            // Add professor breadcrumb
            if (pathInfo.getProfessorId() != null) {
                String professorPath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() +
                        "/" + pathInfo.getProfessorId();

                // Try to get professor name
                String professorName = pathInfo.getProfessorId();
                Optional<User> professorOpt = findProfessorByIdentifierOptional(pathInfo.getProfessorId());
                if (professorOpt.isPresent()) {
                    User professor = professorOpt.get();
                    professorName = professor.getFirstName() + " " + professor.getLastName();
                }

                breadcrumbs.add(BreadcrumbItem.builder()
                        .name(professorName)
                        .path(professorPath)
                        .type(NodeType.PROFESSOR)
                        .build());
            }

            // Add course breadcrumb
            if (pathInfo.getCourseCode() != null) {
                String coursePath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() +
                        "/" + pathInfo.getProfessorId() + "/" + pathInfo.getCourseCode();

                breadcrumbs.add(BreadcrumbItem.builder()
                        .name(pathInfo.getCourseCode())
                        .path(coursePath)
                        .type(NodeType.COURSE)
                        .build());
            }

            // Add document type breadcrumb
            if (pathInfo.getDocumentType() != null) {
                String docTypePath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() +
                        "/" + pathInfo.getProfessorId() + "/" + pathInfo.getCourseCode() +
                        "/" + pathInfo.getDocumentType();

                DocumentTypeEnum docType = DocumentTypeEnum.valueOf(pathInfo.getDocumentType().toUpperCase());

                breadcrumbs.add(BreadcrumbItem.builder()
                        .name(formatDocumentType(docType))
                        .path(docTypePath)
                        .type(NodeType.DOCUMENT_TYPE)
                        .build());
            }

        } catch (Exception e) {
            log.error("Error generating breadcrumbs: {}", e.getMessage());
        }

        return breadcrumbs;
    }

    @Override
    public boolean canRead(String nodePath, User user) {
        log.debug("Checking read permission for path={}, user={}", nodePath, user.getEmail());

        try {
            PathInfo pathInfo = parsePath(nodePath);

            // Admin and Dean can read all (using FileAccessService)
            if (fileAccessService.hasAdminLevelAccess(user)) {
                return true;
            }

            // For HOD and Professor, check department access
            if (pathInfo.getProfessorId() != null) {
                // Find the professor whose folder this is
                Optional<User> professorOpt = findProfessorByIdentifierOptional(pathInfo.getProfessorId());
                if (!professorOpt.isPresent()) {
                    fileAccessService.logAccessDenial(user, null, 
                        "Professor not found for path: " + nodePath);
                    return false;
                }

                User professor = professorOpt.get();

                // Use FileAccessService to check department access
                if (professor.getDepartment() != null) {
                    boolean canAccess = fileAccessService.canAccessDepartmentFiles(user, professor.getDepartment().getId());
                    if (!canAccess) {
                        fileAccessService.logAccessDenial(user, null, 
                            "User cannot access department files for path: " + nodePath);
                    }
                    return canAccess;
                }

                return false;
            }

            // Semester level - all authenticated users can read
            return true;

        } catch (Exception e) {
            log.error("Error checking read permission: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean canWrite(String nodePath, User user) {
        log.debug("Checking write permission for path={}, user={}", nodePath, user.getEmail());

        // Only professors can write
        if (user.getRole() != Role.ROLE_PROFESSOR) {
            return false;
        }

        try {
            PathInfo pathInfo = parsePath(nodePath);

            // Must be at course, document type, or custom folder level
            if (pathInfo.getType() != NodeType.COURSE 
                && pathInfo.getType() != NodeType.DOCUMENT_TYPE
                && pathInfo.getType() != NodeType.CUSTOM) {
                return false;
            }

            // Check if this is the professor's own course
            if (pathInfo.getProfessorId() != null) {
                Optional<User> professorOpt = findProfessorByIdentifierOptional(pathInfo.getProfessorId());
                if (!professorOpt.isPresent()) {
                    return false;
                }

                User professor = professorOpt.get();
                return professor.getId().equals(user.getId());
            }

            return false;

        } catch (Exception e) {
            log.error("Error checking write permission: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean canDelete(String nodePath, User user) {
        log.debug("Checking delete permission for path={}, user={}", nodePath, user.getEmail());

        // Only professors can delete
        if (user.getRole() != Role.ROLE_PROFESSOR) {
            return false;
        }

        try {
            PathInfo pathInfo = parsePath(nodePath);

            // Can only delete files
            if (pathInfo.getType() != NodeType.FILE) {
                return false;
            }

            // Check if this is the professor's own file
            if (pathInfo.getProfessorId() != null) {
                Optional<User> professorOpt = findProfessorByIdentifierOptional(pathInfo.getProfessorId());
                if (!professorOpt.isPresent()) {
                    return false;
                }

                User professor = professorOpt.get();
                return professor.getId().equals(user.getId());
            }

            return false;

        } catch (Exception e) {
            log.error("Error checking delete permission: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Build year node from path info
     */
    private FileExplorerNode buildYearNode(PathInfo pathInfo, User currentUser) {
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));

        String nodePath = "/" + pathInfo.getYearCode();

        FileExplorerNode node = FileExplorerNode.builder()
                .path(nodePath)
                .name(academicYear.getYearCode())
                .type(NodeType.YEAR)
                .entityId(academicYear.getId())
                .canRead(true)
                .canWrite(false)
                .canDelete(false)
                .build();

        node.getMetadata().put("academicYearId", academicYear.getId());
        node.getMetadata().put("yearCode", academicYear.getYearCode());

        return node;
    }

    /**
     * Get semester children for a year node
     */
    private List<FileExplorerNode> getSemesterChildren(PathInfo pathInfo, User currentUser) {
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));

        List<Semester> semesters = semesterRepository.findByAcademicYearId(academicYear.getId());

        String parentPath = "/" + pathInfo.getYearCode();

        return semesters.stream()
                .map(semester -> {
                    String semesterPath = parentPath + "/" + semester.getType().name().toLowerCase();

                    FileExplorerNode node = FileExplorerNode.builder()
                            .path(semesterPath)
                            .name(formatSemesterType(semester.getType()))
                            .type(NodeType.SEMESTER)
                            .entityId(semester.getId())
                            .canRead(true)
                            .canWrite(false)
                            .canDelete(false)
                            .build();

                    node.getMetadata().put("academicYearId", academicYear.getId());
                    node.getMetadata().put("semesterId", semester.getId());
                    node.getMetadata().put("semesterType", semester.getType().name());

                    return node;
                })
                .sorted(Comparator.comparing(node -> {
                    // Sort order: FIRST, SECOND, SUMMER
                    String type = (String) node.getMetadata().get("semesterType");
                    if ("FIRST".equals(type))
                        return 1;
                    if ("SECOND".equals(type))
                        return 2;
                    if ("SUMMER".equals(type))
                        return 3;
                    return 4;
                }))
                .collect(Collectors.toList());
    }

    /**
     * Get files for a document type folder
     */
    private List<UploadedFileDTO> getFilesForDocumentType(PathInfo pathInfo, User currentUser) {
        try {
            // Find professor
            User professor = findProfessorByIdentifier(pathInfo.getProfessorId());

            // Find semester
            AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                    .orElseThrow(
                            () -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));
            SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());
            Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                    .orElseThrow(() -> new EntityNotFoundException("Semester not found"));

            // Find course assignment
            CourseAssignment assignment = courseAssignmentRepository
                    .findBySemesterIdAndCourseCodeAndProfessorId(semester.getId(), pathInfo.getCourseCode(),
                            professor.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Course assignment not found"));

            // Find course folder in Folder table
            Optional<Folder> courseFolderOpt = folderRepository.findCourseFolder(
                    professor.getId(), assignment.getCourse().getId(),
                    academicYear.getId(), semester.getId(), FolderType.COURSE);

            if (!courseFolderOpt.isPresent()) {
                log.debug("Course folder not found for assignment: {}", assignment.getId());
                return new ArrayList<>();
            }

            Folder courseFolder = courseFolderOpt.get();

            // Parse document type from path
            DocumentTypeEnum docType;
            try {
                docType = DocumentTypeEnum.valueOf(pathInfo.getDocumentType().toUpperCase().replace("-", "_"));
            } catch (IllegalArgumentException e) {
                docType = mapPathSegmentToDocumentType(pathInfo.getDocumentType());
                if (docType == null) {
                    log.warn("Invalid document type: {}", pathInfo.getDocumentType());
                    return new ArrayList<>();
                }
            }

            // Map document type to folder name
            String folderName = formatDocumentType(docType);

            // Find the subfolder for this document type
            List<Folder> subfolders = folderRepository.findByParentId(courseFolder.getId());
            Optional<Folder> documentTypeFolderOpt = subfolders.stream()
                    .filter(f -> f.getName().equalsIgnoreCase(folderName))
                    .findFirst();

            if (!documentTypeFolderOpt.isPresent()) {
                log.debug("Document type folder not found: {}", folderName);
                return new ArrayList<>();
            }

            Folder documentTypeFolder = documentTypeFolderOpt.get();

            // Query files from this folder with uploader data
            List<UploadedFile> files = uploadedFileRepository.findByFolderIdWithUploader(documentTypeFolder.getId());

            final String fallbackUploaderName = professor.getFirstName() + " " + professor.getLastName();
            final User finalCurrentUser = currentUser;

            // Convert to DTOs ensuring uploader name is always populated
            return files.stream()
                    .map(file -> {
                        UploadedFileDTO dto = convertToUploadedFileDTO(file, finalCurrentUser);
                        if (dto.getUploaderName() == null) {
                            dto.setUploaderName(fallbackUploaderName);
                        }
                        return dto;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting files for document type: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Convert UploadedFile entity to UploadedFileDTO
     */
    private UploadedFileDTO convertToUploadedFileDTO(UploadedFile file) {
        return convertToUploadedFileDTO(file, null);
    }
    
    /**
     * Convert UploadedFile entity to UploadedFileDTO with permission checking
     * @param file the uploaded file entity
     * @param currentUser the current user for permission checking (can be null)
     */
    private UploadedFileDTO convertToUploadedFileDTO(UploadedFile file, User currentUser) {
        UploadedFileDTO.UploadedFileDTOBuilder builder = UploadedFileDTO.builder()
                .id(file.getId())
                .originalFilename(file.getOriginalFilename())
                .storedFilename(file.getStoredFilename())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .uploadedAt(file.getCreatedAt())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .notes(file.getNotes())
                .fileUrl(file.getFileUrl())
                .orphaned(false); // Files in DB are not orphaned

        // Add uploader info if available
        if (file.getUploader() != null) {
            String uploaderName = file.getUploader().getFirstName() + " " + file.getUploader().getLastName();
            builder.uploaderName(uploaderName);
            builder.uploaderId(file.getUploader().getId());
        }
        
        // Set permission flags based on current user
        if (currentUser != null && file.getUploader() != null) {
            boolean isOwner = file.getUploader().getId().equals(currentUser.getId());
            boolean isProfessor = currentUser.getRole() == Role.ROLE_PROFESSOR;
            // Professors can only delete/replace their own files
            builder.canDelete(isOwner && isProfessor);
            builder.canReplace(isOwner && isProfessor);
        } else {
            builder.canDelete(false);
            builder.canReplace(false);
        }

        return builder.build();
    }
    
    /**
     * Create a new folder at the specified path.
     * Only professors can create folders within their own course folders.
     * 
     * @param request the folder creation request containing path and folder name
     * @param currentUser the authenticated user (must be a professor)
     * @return response containing the created folder details
     */
    @Override
    @Transactional
    public CreateFolderResponse createFolder(CreateFolderRequest request, User currentUser) {
        log.info("Creating folder '{}' at path '{}' for user {}", 
                request.getFolderName(), request.getPath(), currentUser.getEmail());
        
        // 1. Validate user role - only professors can create folders
        if (currentUser.getRole() != Role.ROLE_PROFESSOR) {
            log.warn("User {} with role {} attempted to create folder - access denied", 
                    currentUser.getEmail(), currentUser.getRole());
            throw new UnauthorizedOperationException("Only professors can create folders");
        }
        
        // 2. Validate folder name
        String folderName = validateAndSanitizeFolderName(request.getFolderName());
        
        // 3. Parse and validate path
        String path = request.getPath();
        if (path == null || path.isEmpty()) {
            throw new InvalidFolderNameException("Path cannot be empty");
        }
        
        // Normalize path - remove leading/trailing slashes
        path = path.replaceAll("^/+|/+$", "");
        
        // 4. Check write permission for the path
        if (!canWrite("/" + path, currentUser)) {
            log.warn("User {} does not have write permission for path: {}", 
                    currentUser.getEmail(), path);
            throw new UnauthorizedOperationException(
                "You do not have permission to create folders at this location");
        }
        
        // 5. Parse path to extract context (yearCode/semesterType/professorId/courseCode)
        PathInfo pathInfo = parsePath("/" + path);
        
        // 6. Validate path has required components for COURSE level
        if (pathInfo.getType() != NodeType.COURSE) {
            log.warn("Invalid path type for folder creation: {}. Expected COURSE level path.", pathInfo.getType());
            throw new InvalidFolderNameException("Folders can only be created inside course folders");
        }
        
        // 7. Verify the professor owns this path
        User professor = findProfessorByIdentifier(pathInfo.getProfessorId());
        if (!professor.getId().equals(currentUser.getId())) {
            throw new UnauthorizedOperationException(
                "You can only create folders in your own directory");
        }
        
        // 8. Find academic year and semester
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));
        
        SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());
        Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));
        
        // 9. Find course from course code
        Course course = courseRepository.findByCourseCode(pathInfo.getCourseCode())
                .orElseThrow(() -> new EntityNotFoundException("Course not found: " + pathInfo.getCourseCode()));
        
        // 10. Find or create the course folder in database
        Folder parentFolder = folderRepository.findCourseFolder(
                currentUser.getId(), course.getId(), academicYear.getId(), semester.getId(), FolderType.COURSE)
                .orElseGet(() -> {
                    // Course folder doesn't exist in DB yet - create it via FolderService
                    log.info("Course folder not found in database, creating it...");
                    return createCourseFolderForUser(currentUser, course, academicYear, semester);
                });
        
        // 11. Check if folder already exists
        if (folderRepository.existsByNameAndParentId(folderName, parentFolder.getId())) {
            throw new FolderAlreadyExistsException(folderName, parentFolder.getPath());
        }
        
        // 12. Construct full path for new folder
        String fullPath = parentFolder.getPath() + "/" + folderName;
        
        // 13. Check if path already exists in database
        if (folderRepository.existsByPath(fullPath)) {
            throw new FolderAlreadyExistsException(folderName, parentFolder.getPath());
        }
        
        // 14. Create folder entity
        Folder newFolder = Folder.builder()
                .path(fullPath)
                .name(folderName)
                .type(FolderType.CUSTOM)
                .parent(parentFolder)
                .owner(currentUser)
                .academicYear(academicYear)
                .semester(semester)
                .course(course)
                .build();
        
        // 15. Create physical directory on filesystem
        createPhysicalFolder(fullPath);
        
        // 16. Save folder to database
        Folder savedFolder = folderRepository.save(newFolder);
        log.info("Folder '{}' created successfully with ID {} at path: {}", 
                folderName, savedFolder.getId(), fullPath);
        
        // 17. Return success response
        return CreateFolderResponse.success("/" + fullPath, folderName, savedFolder.getId());
    }
    
    /**
     * Create a course folder for the user if it doesn't exist.
     * This ensures the course folder hierarchy exists in the database.
     */
    private Folder createCourseFolderForUser(User professor, Course course, AcademicYear academicYear, Semester semester) {
        // First ensure professor root folder exists
        Folder professorFolder = folderRepository.findProfessorRootFolder(
                professor.getId(), academicYear.getId(), semester.getId(), FolderType.PROFESSOR_ROOT)
                .orElseGet(() -> {
                    // Create professor root folder
                    String yearCode = academicYear.getYearCode();
                    String semesterTypeName = semester.getType().name().toLowerCase();
                    String professorFolderName = professor.getFirstName() + " " + professor.getLastName();
                    String profPath = yearCode + "/" + semesterTypeName + "/" + professorFolderName;
                    
                    createPhysicalFolder(profPath);
                    
                    Folder profFolder = Folder.builder()
                            .path(profPath)
                            .name(professorFolderName)
                            .type(FolderType.PROFESSOR_ROOT)
                            .parent(null)
                            .owner(professor)
                            .academicYear(academicYear)
                            .semester(semester)
                            .course(null)
                            .build();
                    
                    return folderRepository.save(profFolder);
                });
        
        // Now create the course folder
        String courseFolderName = course.getCourseCode() + " - " + course.getCourseName();
        String courseFolderPath = professorFolder.getPath() + "/" + courseFolderName;
        
        createPhysicalFolder(courseFolderPath);
        
        Folder courseFolder = Folder.builder()
                .path(courseFolderPath)
                .name(courseFolderName)
                .type(FolderType.COURSE)
                .parent(professorFolder)
                .owner(professor)
                .academicYear(academicYear)
                .semester(semester)
                .course(course)
                .build();
        
        return folderRepository.save(courseFolder);
    }
    
    /**
     * Validate and sanitize folder name according to validation rules.
     * 
     * @param folderName the folder name to validate
     * @return sanitized folder name
     * @throws InvalidFolderNameException if validation fails
     */
    private String validateAndSanitizeFolderName(String folderName) {
        // Check for null or empty
        if (folderName == null || folderName.trim().isEmpty()) {
            throw InvalidFolderNameException.empty();
        }
        
        // Trim whitespace
        folderName = folderName.trim();
        
        // Check max length
        if (folderName.length() > 128) {
            throw InvalidFolderNameException.tooLong(folderName);
        }
        
        // Check for invalid filesystem characters: / \ ? * : < > | "
        if (folderName.matches(".*[/\\\\?*:<>|\"].*")) {
            throw InvalidFolderNameException.invalidCharacters(folderName);
        }
        
        // Check for reserved names (Windows)
        String[] reservedNames = {"CON", "PRN", "AUX", "NUL", 
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};
        for (String reserved : reservedNames) {
            if (folderName.equalsIgnoreCase(reserved)) {
                throw new InvalidFolderNameException(folderName, "'" + reserved + "' is a reserved name");
            }
        }
        
        // Check for dots only or spaces only
        if (folderName.matches("^[\\.\\s]+$")) {
            throw new InvalidFolderNameException(folderName, "Folder name cannot consist of only dots or spaces");
        }
        
        return folderName;
    }
    
    /**
     * Create physical folder on the filesystem.
     * 
     * @param folderPath the logical folder path
     * @throws RuntimeException if folder creation fails
     */
    private void createPhysicalFolder(String folderPath) {
        try {
            // Construct physical path: uploads/{folderPath}
            Path physicalPath = Paths.get(uploadBasePath, folderPath);
            
            // Create directory including all parent directories
            Files.createDirectories(physicalPath);
            
            log.debug("Physical folder created at: {}", physicalPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create physical folder at path: {}", folderPath, e);
            throw new RuntimeException("Failed to create folder on filesystem: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete a folder and all its contents (files and subfolders).
     * This method also deletes all physical files and folders from the uploads directory.
     * 
     * @param request the folder deletion request
     * @param currentUser the authenticated user
     * @return response containing deletion statistics
     */
    @Override
    @Transactional
    public DeleteFolderResponse deleteFolder(DeleteFolderRequest request, User currentUser) {
        log.info("Deleting folder at path '{}' for user {}", 
                request.getFolderPath(), currentUser.getEmail());
        
        // 1. Validate user role - only professors can delete folders
        if (currentUser.getRole() != Role.ROLE_PROFESSOR) {
            log.warn("User {} with role {} attempted to delete folder - access denied", 
                    currentUser.getEmail(), currentUser.getRole());
            throw new UnauthorizedOperationException("Only professors can delete folders");
        }
        
        // 2. Normalize path - remove leading/trailing slashes
        String rawPath = request.getFolderPath();
        if (rawPath == null || rawPath.isEmpty()) {
            throw new EntityNotFoundException("Folder path cannot be empty");
        }
        final String folderPath = rawPath.replaceAll("^/+|/+$", "");
        
        // 3. Check write permission for the path
        if (!canWrite("/" + folderPath, currentUser)) {
            log.warn("User {} does not have write permission for path: {}", 
                    currentUser.getEmail(), folderPath);
            throw new UnauthorizedOperationException(
                "You do not have permission to delete this folder");
        }
        
        // 4. Find the folder in the database
        // Handle ID-based path format (custom-{id})
        Folder folder = null;
        
        // Parse the path to check if it's an ID-based custom folder
        PathInfo pathInfo = parsePath("/" + folderPath);
        log.debug("Parsed path - Type: {}, CustomFolderId: {}, Path: {}", 
                pathInfo.getType(), pathInfo.getCustomFolderId(), folderPath);
        
        if (pathInfo.getType() == NodeType.CUSTOM && pathInfo.getCustomFolderId() != null) {
            // ID-based lookup
            log.debug("Looking up folder by ID: {}", pathInfo.getCustomFolderId());
            folder = folderRepository.findById(pathInfo.getCustomFolderId())
                    .orElseThrow(() -> new EntityNotFoundException("Folder not found with ID: " + pathInfo.getCustomFolderId()));
        } else {
            // Name-based lookup (legacy)
            log.debug("Looking up folder by path: {}", folderPath);
            folder = folderRepository.findByPath(folderPath)
                    .orElseThrow(() -> new EntityNotFoundException("Folder not found: " + folderPath));
        }
        
        // 5. Verify the professor owns this folder
        if (!folder.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedOperationException(
                "You can only delete folders in your own directory");
        }
        
        // 6. Validate that this is a CUSTOM folder (not a system folder like COURSE, PROFESSOR_ROOT, etc.)
        if (folder.getType() != FolderType.CUSTOM) {
            log.warn("Attempted to delete non-custom folder: {} of type {}", folderPath, folder.getType());
            throw new UnauthorizedOperationException(
                "Only custom folders can be deleted. System folders (Year, Semester, Professor, Course, Document Type) cannot be deleted.");
        }
        
        // 7. Count files and subfolders before deletion
        int filesDeleted = countFilesInFolder(folder);
        int subfoldersDeleted = countSubfoldersInFolder(folder);
        
        String folderName = folder.getName();
        String actualFolderPath = folder.getPath(); // Use the actual path from database
        
        log.info("Deleting folder '{}' with {} files and {} subfolders at path: {}", 
                folderName, filesDeleted, subfoldersDeleted, actualFolderPath);
        
        // 8. Delete all files and subfolders from database first (to avoid FK constraint violations)
        deleteFolderContentsRecursively(folder);
        
        // 9. Delete physical folder and all its contents from filesystem
        deletePhysicalFolder(actualFolderPath);
        
        // 10. Delete folder from database (now safe since all children are deleted)
        folderRepository.delete(folder);
        
        log.info("Folder '{}' deleted successfully from database and filesystem", folderName);
        
        // 11. Return success response
        return DeleteFolderResponse.success("/" + folderPath, folderName, filesDeleted, subfoldersDeleted);
    }
    
    /**
     * Count the number of files in a folder and its subfolders.
     */
    private int countFilesInFolder(Folder folder) {
        int count = (int) uploadedFileRepository.countByFolderId(folder.getId());
        
        // Add files from subfolders
        List<Folder> subfolders = folderRepository.findByParentId(folder.getId());
        for (Folder subfolder : subfolders) {
            count += countFilesInFolder(subfolder);
        }
        
        return count;
    }
    
    /**
     * Count the number of subfolders in a folder (recursively).
     */
    private int countSubfoldersInFolder(Folder folder) {
        List<Folder> subfolders = folderRepository.findByParentId(folder.getId());
        int count = subfolders.size();
        
        // Add subfolders from nested folders
        for (Folder subfolder : subfolders) {
            count += countSubfoldersInFolder(subfolder);
        }
        
        return count;
    }
    
    /**
     * Recursively delete all files and subfolders from a folder before deleting the folder itself.
     * This prevents foreign key constraint violations.
     * 
     * @param folder the folder to delete contents from
     */
    private void deleteFolderContentsRecursively(Folder folder) {
        // 1. Get all subfolders (children)
        List<Folder> subfolders = folderRepository.findByParentId(folder.getId());
        
        // 2. Recursively delete each subfolder (children first)
        for (Folder subfolder : subfolders) {
            deleteFolderContentsRecursively(subfolder);
            // Delete the subfolder after its contents are deleted
            folderRepository.delete(subfolder);
            log.debug("Deleted subfolder: {} (ID: {})", subfolder.getPath(), subfolder.getId());
        }
        
        // 3. Delete all files in this folder
        List<UploadedFile> files = uploadedFileRepository.findByFolderId(folder.getId());
        if (!files.isEmpty()) {
            uploadedFileRepository.deleteAll(files);
            log.debug("Deleted {} files from folder: {} (ID: {})", files.size(), folder.getPath(), folder.getId());
        }
    }
    
    /**
     * Delete physical folder and all its contents from the filesystem.
     * 
     * @param folderPath the logical folder path
     * @throws RuntimeException if folder deletion fails
     */
    private void deletePhysicalFolder(String folderPath) {
        try {
            // Construct physical path: uploads/{folderPath}
            Path physicalPath = Paths.get(uploadBasePath, folderPath);
            
            if (!Files.exists(physicalPath)) {
                log.warn("Physical folder does not exist at path: {}", physicalPath.toAbsolutePath());
                return;
            }
            
            // Delete directory and all its contents recursively
            deleteDirectoryRecursively(physicalPath);
            
            log.info("Physical folder deleted at: {}", physicalPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to delete physical folder at path: {}", folderPath, e);
            throw new RuntimeException("Failed to delete folder from filesystem: " + e.getMessage(), e);
        }
    }
    
    /**
     * Recursively delete a directory and all its contents.
     * 
     * @param directory the directory to delete
     * @throws IOException if deletion fails
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (Files.isDirectory(directory)) {
            // Delete all files and subdirectories first
            try (var stream = Files.list(directory)) {
                for (Path path : stream.collect(Collectors.toList())) {
                    deleteDirectoryRecursively(path);
                }
            }
        }
        
        // Delete the file or empty directory
        Files.delete(directory);
    }

    /**
     * Check if a folder exists on the physical filesystem.
     * For custom folders that have been deleted from disk, this method also
     * triggers cleanup of the database record asynchronously.
     * 
     * @param folder the folder entity to check
     * @return true if the folder exists on disk, false otherwise
     */
    @org.springframework.transaction.annotation.Transactional
    private boolean folderExistsOnDisk(Folder folder) {
        if (folder == null || folder.getPath() == null || folder.getPath().isEmpty()) {
            return false;
        }
        
        try {
            Path physicalPath = safePathResolver.resolve(folder.getPath());
            boolean exists = Files.exists(physicalPath) && Files.isDirectory(physicalPath);
            
            if (!exists && folder.getType() == FolderType.CUSTOM) {
                // Custom folder no longer exists on disk - clean up the database record
                log.info("Custom folder no longer exists on disk, removing DB record: {} (ID: {})", 
                        folder.getPath(), folder.getId());
                
                // Delete associated files from database first
                List<UploadedFile> files = uploadedFileRepository.findByFolderId(folder.getId());
                if (!files.isEmpty()) {
                    log.info("Removing {} orphaned file records from custom folder {}", 
                            files.size(), folder.getId());
                    uploadedFileRepository.deleteAll(files);
                }
                
                // Delete the folder record
                folderRepository.delete(folder);
                log.info("Removed orphaned custom folder record: {}", folder.getId());
            }
            
            return exists;
        } catch (Exception e) {
            log.warn("Error checking folder existence on disk: {} - {}", folder.getPath(), e.getMessage());
            return false;
        }
    }

    /**
     * Determine MIME type from file name extension.
     */
    private String determineMimeTypeFromFileName(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            String extension = fileName.substring(lastDot + 1).toLowerCase();
            switch (extension) {
                case "pdf": return "application/pdf";
                case "doc": return "application/msword";
                case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                case "xls": return "application/vnd.ms-excel";
                case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                case "ppt": return "application/vnd.ms-powerpoint";
                case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                case "txt": return "text/plain";
                case "jpg":
                case "jpeg": return "image/jpeg";
                case "png": return "image/png";
                case "gif": return "image/gif";
                case "zip": return "application/zip";
                case "rar": return "application/x-rar-compressed";
                default: return "application/octet-stream";
            }
        }
        return "application/octet-stream";
    }

    /**
     * Helper class to hold parsed path information
     */
    @lombok.Data
    private static class PathInfo {
        private String path;
        private String yearCode;
        private String semesterType;
        private String professorId;
        private String courseCode;
        private String documentType;
        private String customFolderName;  // For custom folders created by professors
        private Long customFolderId;      // For custom folders identified by ID
        private NodeType type;
    }
}
