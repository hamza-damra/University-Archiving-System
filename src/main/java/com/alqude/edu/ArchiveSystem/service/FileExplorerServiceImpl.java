package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.fileexplorer.BreadcrumbItem;
import com.alqude.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode;
import com.alqude.edu.ArchiveSystem.dto.fileexplorer.NodeType;
import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.exception.UnauthorizedOperationException;
import com.alqude.edu.ArchiveSystem.repository.*;
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
public class FileExplorerServiceImpl implements FileExplorerService {
    
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final DocumentSubmissionRepository documentSubmissionRepository;
    private final UploadedFileRepository uploadedFileRepository;
    
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
     * - Deanship: all professors in semester
     * - HOD: professors in HOD's department only
     * - Professor: all professors in same department
     */
    private List<User> getProfessorsForUser(Long semesterId, User currentUser) {
        // Get all course assignments for the semester
        List<CourseAssignment> assignments = courseAssignmentRepository.findBySemesterId(semesterId);
        
        // Extract unique professors
        Set<User> professorSet = new HashSet<>();
        for (CourseAssignment assignment : assignments) {
            professorSet.add(assignment.getProfessor());
        }
        
        List<User> professors = new ArrayList<>(professorSet);
        
        // Apply role-based filtering
        switch (currentUser.getRole()) {
            case ROLE_DEANSHIP:
                // Deanship sees all professors
                log.debug("Deanship user - returning all {} professors", professors.size());
                break;
                
            case ROLE_HOD:
                // HOD sees only professors in their department
                if (currentUser.getDepartment() != null) {
                    professors = professors.stream()
                            .filter(p -> p.getDepartment() != null && 
                                       p.getDepartment().getId().equals(currentUser.getDepartment().getId()))
                            .collect(Collectors.toList());
                    log.debug("HOD user - filtered to {} professors in department", professors.size());
                } else {
                    log.warn("HOD user has no department assigned");
                    professors = new ArrayList<>();
                }
                break;
                
            case ROLE_PROFESSOR:
                // Professor sees all professors in same department
                if (currentUser.getDepartment() != null) {
                    professors = professors.stream()
                            .filter(p -> p.getDepartment() != null && 
                                       p.getDepartment().getId().equals(currentUser.getDepartment().getId()))
                            .collect(Collectors.toList());
                    log.debug("Professor user - filtered to {} professors in department", professors.size());
                } else {
                    log.warn("Professor user has no department assigned");
                    professors = new ArrayList<>();
                }
                break;
                
            default:
                log.warn("Unknown role: {}", currentUser.getRole());
                professors = new ArrayList<>();
        }
        
        // Sort by name
        professors.sort(Comparator.comparing(User::getFirstName)
                .thenComparing(User::getLastName));
        
        return professors;
    }
    
    /**
     * Build a professor node
     */
    private FileExplorerNode buildProfessorNode(String parentPath, User professor, User currentUser) {
        String professorPath = parentPath + "/" + professor.getProfessorId();
        
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
        node.getMetadata().put("email", professor.getEmail());
        node.getMetadata().put("departmentId", professor.getDepartment() != null ? professor.getDepartment().getId() : null);
        node.getMetadata().put("departmentName", professor.getDepartment() != null ? professor.getDepartment().getName() : null);
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
        User professor = userRepository.findByProfessorId(pathInfo.getProfessorId())
                .orElseThrow(() -> new EntityNotFoundException("Professor not found: " + pathInfo.getProfessorId()));
        
        String nodePath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() + "/" + pathInfo.getProfessorId();
        
        return buildProfessorNode(nodePath.substring(0, nodePath.lastIndexOf("/")), professor, currentUser);
    }
    
    /**
     * Build course node from path info
     */
    private FileExplorerNode buildCourseNode(PathInfo pathInfo, User currentUser) {
        // Find professor
        User professor = userRepository.findByProfessorId(pathInfo.getProfessorId())
                .orElseThrow(() -> new EntityNotFoundException("Professor not found: " + pathInfo.getProfessorId()));
        
        // Find semester
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));
        SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());
        Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));
        
        // Find course assignment
        CourseAssignment assignment = courseAssignmentRepository
                .findBySemesterIdAndCourseCodeAndProfessorId(semester.getId(), pathInfo.getCourseCode(), professor.getId())
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
        User professor = userRepository.findByProfessorId(pathInfo.getProfessorId())
                .orElseThrow(() -> new EntityNotFoundException("Professor not found: " + pathInfo.getProfessorId()));
        
        // Find semester
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));
        SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());
        Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));
        
        // Find course assignment
        CourseAssignment assignment = courseAssignmentRepository
                .findBySemesterIdAndCourseCodeAndProfessorId(semester.getId(), pathInfo.getCourseCode(), professor.getId())
                .orElseThrow(() -> new EntityNotFoundException("Course assignment not found"));
        
        DocumentTypeEnum docType = DocumentTypeEnum.valueOf(pathInfo.getDocumentType().toUpperCase());
        
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
        // Find professor
        User professor = userRepository.findByProfessorId(pathInfo.getProfessorId())
                .orElseThrow(() -> new EntityNotFoundException("Professor not found: " + pathInfo.getProfessorId()));
        
        // Find semester
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));
        SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());
        Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));
        
        // Get course assignments for this professor in this semester
        List<CourseAssignment> assignments = courseAssignmentRepository
                .findByProfessorIdAndSemesterId(professor.getId(), semester.getId());
        
        String parentPath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() + "/" + pathInfo.getProfessorId();
        
        boolean isOwnProfile = professor.getId().equals(currentUser.getId());
        
        return assignments.stream()
                .map(assignment -> {
                    String coursePath = parentPath + "/" + assignment.getCourse().getCourseCode();
                    
                    FileExplorerNode node = FileExplorerNode.builder()
                            .path(coursePath)
                            .name(assignment.getCourse().getCourseCode() + " - " + assignment.getCourse().getCourseName())
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
     */
    private List<FileExplorerNode> getDocumentTypeChildren(PathInfo pathInfo, User currentUser) {
        // Find professor
        User professor = userRepository.findByProfessorId(pathInfo.getProfessorId())
                .orElseThrow(() -> new EntityNotFoundException("Professor not found: " + pathInfo.getProfessorId()));
        
        // Find semester
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));
        SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());
        Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));
        
        // Find course assignment
        CourseAssignment assignment = courseAssignmentRepository
                .findBySemesterIdAndCourseCodeAndProfessorId(semester.getId(), pathInfo.getCourseCode(), professor.getId())
                .orElseThrow(() -> new EntityNotFoundException("Course assignment not found"));
        
        // Get all document submissions for this assignment
        List<DocumentSubmission> submissions = documentSubmissionRepository
                .findByCourseAssignmentId(assignment.getId());
        
        // Get unique document types that have submissions
        Set<DocumentTypeEnum> documentTypes = submissions.stream()
                .map(DocumentSubmission::getDocumentType)
                .collect(Collectors.toSet());
        
        String parentPath = "/" + pathInfo.getYearCode() + "/" + pathInfo.getSemesterType() + 
                           "/" + pathInfo.getProfessorId() + "/" + pathInfo.getCourseCode();
        
        boolean isOwnCourse = professor.getId().equals(currentUser.getId());
        
        return documentTypes.stream()
                .map(docType -> {
                    String docTypePath = parentPath + "/" + docType.name().toLowerCase();
                    
                    FileExplorerNode node = FileExplorerNode.builder()
                            .path(docTypePath)
                            .name(formatDocumentType(docType))
                            .type(NodeType.DOCUMENT_TYPE)
                            .entityId(null)
                            .canRead(true)
                            .canWrite(isOwnCourse && currentUser.getRole() == Role.ROLE_PROFESSOR)
                            .canDelete(false)
                            .build();
                    
                    node.getMetadata().put("documentType", docType.name());
                    node.getMetadata().put("assignmentId", assignment.getId());
                    node.getMetadata().put("isOwnCourse", isOwnCourse);
                    
                    // Count files
                    long fileCount = submissions.stream()
                            .filter(s -> s.getDocumentType() == docType)
                            .mapToLong(s -> s.getUploadedFiles().size())
                            .sum();
                    node.getMetadata().put("fileCount", fileCount);
                    
                    return node;
                })
                .sorted(Comparator.comparing(FileExplorerNode::getName))
                .collect(Collectors.toList());
    }
    
    /**
     * Get file children for a document type node
     */
    private List<FileExplorerNode> getFileChildren(PathInfo pathInfo, User currentUser) {
        // Find professor
        User professor = userRepository.findByProfessorId(pathInfo.getProfessorId())
                .orElseThrow(() -> new EntityNotFoundException("Professor not found: " + pathInfo.getProfessorId()));
        
        // Find semester
        AcademicYear academicYear = academicYearRepository.findByYearCode(pathInfo.getYearCode())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + pathInfo.getYearCode()));
        SemesterType semesterType = SemesterType.valueOf(pathInfo.getSemesterType().toUpperCase());
        Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));
        
        // Find course assignment
        CourseAssignment assignment = courseAssignmentRepository
                .findBySemesterIdAndCourseCodeAndProfessorId(semester.getId(), pathInfo.getCourseCode(), professor.getId())
                .orElseThrow(() -> new EntityNotFoundException("Course assignment not found"));
        
        DocumentTypeEnum docType = DocumentTypeEnum.valueOf(pathInfo.getDocumentType().toUpperCase());
        
        // Find submission for this document type
        Optional<DocumentSubmission> submissionOpt = documentSubmissionRepository
                .findByCourseAssignmentIdAndDocumentType(assignment.getId(), docType);
        
        if (!submissionOpt.isPresent()) {
            return new ArrayList<>();
        }
        
        DocumentSubmission submission = submissionOpt.get();
        
        // Get uploaded files
        List<UploadedFile> files = uploadedFileRepository
                .findByDocumentSubmissionIdOrderByFileOrderAsc(submission.getId());
        
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
                Optional<User> professorOpt = userRepository.findByProfessorId(pathInfo.getProfessorId());
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
            
            // Deanship can read all
            if (user.getRole() == Role.ROLE_DEANSHIP) {
                return true;
            }
            
            // For HOD and Professor, check department access
            if (pathInfo.getProfessorId() != null) {
                // Find the professor whose folder this is
                Optional<User> professorOpt = userRepository.findByProfessorId(pathInfo.getProfessorId());
                if (!professorOpt.isPresent()) {
                    return false;
                }
                
                User professor = professorOpt.get();
                
                // Check if user is in same department
                if (user.getDepartment() != null && professor.getDepartment() != null) {
                    return user.getDepartment().getId().equals(professor.getDepartment().getId());
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
                Optional<User> professorOpt = userRepository.findByProfessorId(pathInfo.getProfessorId());
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
                Optional<User> professorOpt = userRepository.findByProfessorId(pathInfo.getProfessorId());
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
