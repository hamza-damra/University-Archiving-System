package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear;
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
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.NodeType;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.UploadedFileDTO;

import com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final DocumentSubmissionRepository documentSubmissionRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final FolderRepository folderRepository;
    private final FileAccessService fileAccessService;

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
            default:
                return new ArrayList<>();
        }
    }

    /**
     * Parse path to extract components and determine node type
     * Path format: /yearCode/semesterType/professorId/courseCode/documentType
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
            info.setDocumentType(parts[4]);
            info.setType(NodeType.DOCUMENT_TYPE);
        }

        return info;
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

        return subfolders.stream()
                .map(subfolder -> {
                    // Map folder name to document type enum for URL-safe path
                    DocumentTypeEnum docType = mapFolderNameToDocumentType(subfolder.getName());
                    String pathSegment = docType != null ? docType.name().toLowerCase()
                            : subfolder.getName().toLowerCase().replace(" ", "-");
                    String docTypePath = parentPath + "/" + pathSegment;

                    FileExplorerNode node = FileExplorerNode.builder()
                            .path(docTypePath)
                            .name(subfolder.getName())
                            .type(NodeType.DOCUMENT_TYPE)
                            .entityId(subfolder.getId())
                            .canRead(true)
                            .canWrite(isOwnCourse && currentUser.getRole() == Role.ROLE_PROFESSOR)
                            .canDelete(false)
                            .build();

                    node.getMetadata().put("folderId", subfolder.getId());
                    node.getMetadata().put("folderName", subfolder.getName());
                    node.getMetadata().put("assignmentId", assignment.getId());
                    node.getMetadata().put("isOwnCourse", isOwnCourse);

                    // Count files in this subfolder
                    if (docType != null) {
                        long fileCount = submissions.stream()
                                .filter(s -> s.getDocumentType() == docType)
                                .mapToLong(s -> s.getUploadedFiles().size())
                                .sum();
                        node.getMetadata().put("fileCount", fileCount);
                        node.getMetadata().put("documentType", docType.name());
                    } else {
                        node.getMetadata().put("fileCount", 0L);
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
     * Get file children for a document type node
     */
    private List<FileExplorerNode> getFileChildren(PathInfo pathInfo, User currentUser) {
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

        DocumentTypeEnum docType = DocumentTypeEnum.valueOf(pathInfo.getDocumentType().toUpperCase());

        // Find submission for this document type
        Optional<DocumentSubmission> submissionOpt = documentSubmissionRepository
                .findByCourseAssignmentIdAndDocumentType(assignment.getId(), docType);

        if (!submissionOpt.isPresent()) {
            return new ArrayList<>();
        }

        DocumentSubmission submission = submissionOpt.get();

        // Get uploaded files with uploader data
        List<UploadedFile> files = uploadedFileRepository
                .findByDocumentSubmissionIdWithUploaderOrderByFileOrderAsc(submission.getId());

        String parentPath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() +
                "/" + pathInfo.getProfessorId() + "/" + pathInfo.getCourseCode() +
                "/" + pathInfo.getDocumentType();

        boolean isOwnCourse = professor.getId().equals(currentUser.getId());

        return files.stream()
                .map(file -> {
                    String filePath = parentPath + "/" + file.getId();

                    FileExplorerNode node = FileExplorerNode.builder()
                            .path(filePath)
                            .name(file.getOriginalFilename())
                            .type(NodeType.FILE)
                            .entityId(file.getId())
                            .canRead(true)
                            .canWrite(false)
                            .canDelete(isOwnCourse && currentUser.getRole() == Role.ROLE_PROFESSOR)
                            .build();

                    node.getMetadata().put("fileId", file.getId());
                    node.getMetadata().put("fileName", file.getOriginalFilename());
                    node.getMetadata().put("fileSize", file.getFileSize());
                    node.getMetadata().put("fileType", file.getFileType());
                    node.getMetadata().put("fileUrl", file.getFileUrl());
                    node.getMetadata().put("uploadedAt", file.getCreatedAt());
                    node.getMetadata().put("submissionId", submission.getId());
                    node.getMetadata().put("isOwnFile", isOwnCourse);
                    
                    // Include uploader name
                    if (file.getUploader() != null) {
                        String uploaderName = file.getUploader().getFirstName() + " " + file.getUploader().getLastName();
                        node.getMetadata().put("uploaderName", uploaderName);
                    } else {
                        String fallbackUploaderName = professor.getFirstName() + " " + professor.getLastName();
                        node.getMetadata().put("uploaderName", fallbackUploaderName);
                    }

                    return node;
                })
                .collect(Collectors.toList());
    }

    /**
     * Format document type for display
     */
    private String formatDocumentType(DocumentTypeEnum type) {
        switch (type) {
            case SYLLABUS:
                return "Syllabus";
            case EXAM:
                return "Exam";
            case ASSIGNMENT:
                return "Assignment";
            case PROJECT_DOCS:
                return "Project Documents";
            case LECTURE_NOTES:
                return "Lecture Notes";
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

            // Must be at course or document type level
            if (pathInfo.getType() != NodeType.COURSE && pathInfo.getType() != NodeType.DOCUMENT_TYPE) {
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

            // Convert to DTOs ensuring uploader name is always populated
            return files.stream()
                    .map(file -> {
                        UploadedFileDTO dto = convertToUploadedFileDTO(file);
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
        UploadedFileDTO.UploadedFileDTOBuilder builder = UploadedFileDTO.builder()
                .id(file.getId())
                .originalFilename(file.getOriginalFilename())
                .storedFilename(file.getStoredFilename())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .uploadedAt(file.getCreatedAt())
                .notes(file.getNotes())
                .fileUrl(file.getFileUrl());

        // Add uploader name if available
        if (file.getUploader() != null) {
            String uploaderName = file.getUploader().getFirstName() + " " + file.getUploader().getLastName();
            builder.uploaderName(uploaderName);
        }

        return builder.build();
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
        private NodeType type;
    }
}
