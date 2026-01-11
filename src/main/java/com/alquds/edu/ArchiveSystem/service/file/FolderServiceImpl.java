package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear;
import com.alquds.edu.ArchiveSystem.repository.file.FolderRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.CourseRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.entity.file.FolderType;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.repository.academic.AcademicYearRepository;
import com.alquds.edu.ArchiveSystem.entity.academic.SemesterType;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.repository.academic.SemesterRepository;
import com.alquds.edu.ArchiveSystem.entity.file.Folder;



import com.alquds.edu.ArchiveSystem.util.PathParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of FolderService for managing folder creation and
 * organization.
 * Provides idempotent folder creation with database and file system
 * synchronization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FolderServiceImpl implements FolderService {

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Standard subfolder names for course folders
     */
    private static final String[] STANDARD_SUBFOLDERS = {
            "Syllabus",
            "Exams",
            "Course Notes",
            "Assignments"
    };

    /**
     * Generate a safe folder name from professor's name.
     * Sanitizes the name to be filesystem-safe while remaining readable.
     * 
     * @param professor The professor user
     * @return A sanitized folder name in format "firstName lastName"
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
        
        // Sanitize: replace problematic characters with underscore, keep spaces and common chars
        // Remove characters that are invalid in file paths: \ / : * ? " < > |
        String sanitized = fullName.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        // Collapse multiple spaces/underscores into single ones
        sanitized = sanitized.replaceAll("\\s+", " ").replaceAll("_+", "_").trim();
        
        return sanitized;
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
        if (professorIdentifier == null || professorIdentifier.isEmpty() || "null".equals(professorIdentifier)) {
            throw new EntityNotFoundException("Invalid professor identifier: " + professorIdentifier);
        }
        
        // 1. Check if this is a legacy fallback identifier (prof_<id>)
        if (professorIdentifier.startsWith("prof_")) {
            try {
                Long userId = Long.parseLong(professorIdentifier.substring(5));
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    log.debug("Found professor by legacy fallback ID: {}", professorIdentifier);
                    return userOpt.get();
                }
            } catch (NumberFormatException e) {
                // Not a valid prof_<id> format, continue to other lookups
            }
        }
        
        // 2. Try looking up by professorId field
        Optional<User> byProfessorId = userRepository.findByProfessorId(professorIdentifier);
        if (byProfessorId.isPresent()) {
            log.debug("Found professor by professorId: {}", professorIdentifier);
            return byProfessorId.get();
        }
        
        // 3. Try looking up by name (new format) - search for professors whose generated folder name matches
        List<User> allProfessors = userRepository.findByRole(Role.ROLE_PROFESSOR);
        for (User professor : allProfessors) {
            String folderName = generateProfessorFolderName(professor);
            if (folderName.equals(professorIdentifier)) {
                log.debug("Found professor by folder name match: {} -> {} {}", 
                        professorIdentifier, professor.getFirstName(), professor.getLastName());
                return professor;
            }
        }
        
        throw new EntityNotFoundException("Professor not found: " + professorIdentifier);
    }

    @Override
    @Transactional
    public Folder createProfessorFolder(Long professorId, Long academicYearId, Long semesterId) {
        log.info("Creating professor folder for professorId={}, academicYearId={}, semesterId={}",
                professorId, academicYearId, semesterId);

        // Validate and fetch entities
        User professor = userRepository.findById(java.util.Objects.requireNonNull(professorId))
                .orElseThrow(() -> new EntityNotFoundException("Professor not found with id: " + professorId));

        if (professor.getRole() != Role.ROLE_PROFESSOR) {
            throw new IllegalArgumentException("User is not a professor: " + professorId);
        }

        AcademicYear academicYear = academicYearRepository.findById(java.util.Objects.requireNonNull(academicYearId))
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found with id: " + academicYearId));

        Semester semester = semesterRepository.findById(java.util.Objects.requireNonNull(semesterId))
                .orElseThrow(() -> new EntityNotFoundException("Semester not found with id: " + semesterId));

        // Verify semester belongs to academic year
        if (!semester.getAcademicYear().getId().equals(academicYearId)) {
            throw new IllegalArgumentException("Semester does not belong to the specified academic year");
        }

        // Check if professor folder already exists (idempotency)
        Optional<Folder> existingFolder = folderRepository.findProfessorRootFolder(
                professorId, academicYearId, semesterId, FolderType.PROFESSOR_ROOT);

        if (existingFolder.isPresent()) {
            log.info("Professor folder already exists: {}", existingFolder.get().getPath());
            return existingFolder.get();
        }

        // Generate folder path: {yearCode}/{semesterType}/{professorFolderName}
        String yearCode = academicYear.getYearCode();
        String semesterType = semester.getType().name().toLowerCase();
        // Use professor's full name for folder (sanitized for filesystem)
        String professorFolderName = generateProfessorFolderName(professor);
        log.info("Using professor folder name: {} for professor {}", professorFolderName, professor.getEmail());

        String folderPath = yearCode + "/" + semesterType + "/" + professorFolderName;
        String folderName = professor.getFirstName() + " " + professor.getLastName();

        // Create physical directory
        createPhysicalDirectory(folderPath);

        // Create database entity
        Folder folder = Folder.builder()
                .path(folderPath)
                .name(folderName)
                .type(FolderType.PROFESSOR_ROOT)
                .parent(null)
                .owner(professor)
                .academicYear(academicYear)
                .semester(semester)
                .course(null)
                .build();

        folder = folderRepository.save(java.util.Objects.requireNonNull(folder));

        log.info("Successfully created professor folder: {}", folderPath);
        return folder;
    }

    @Override
    @Transactional
    public List<Folder> createCourseFolderStructure(Long professorId, Long courseId,
            Long academicYearId, Long semesterId) {
        log.info("Creating course folder structure for professorId={}, courseId={}, academicYearId={}, semesterId={}",
                professorId, courseId, academicYearId, semesterId);

        List<Folder> createdFolders = new ArrayList<>();

        // Validate and fetch entities
        User professor = userRepository.findById(java.util.Objects.requireNonNull(professorId))
                .orElseThrow(() -> new EntityNotFoundException("Professor not found with id: " + professorId));

        Course course = courseRepository.findById(java.util.Objects.requireNonNull(courseId))
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        AcademicYear academicYear = academicYearRepository.findById(java.util.Objects.requireNonNull(academicYearId))
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found with id: " + academicYearId));

        Semester semester = semesterRepository.findById(java.util.Objects.requireNonNull(semesterId))
                .orElseThrow(() -> new EntityNotFoundException("Semester not found with id: " + semesterId));

        // Ensure professor root folder exists
        Folder professorFolder = createProfessorFolder(professorId, academicYearId, semesterId);

        // Check if course folder already exists (idempotency)
        Optional<Folder> existingCourseFolder = folderRepository.findCourseFolder(
                professorId, courseId, academicYearId, semesterId, FolderType.COURSE);

        Folder courseFolder;
        if (existingCourseFolder.isPresent()) {
            log.info("Course folder already exists: {}", existingCourseFolder.get().getPath());
            courseFolder = existingCourseFolder.get();
            createdFolders.add(courseFolder);
        } else {
            // Generate course folder path:
            // {yearCode}/{semesterType}/{professorId}/{courseCode} - {courseName}
            String courseFolderName = course.getCourseCode() + " - " + course.getCourseName();
            String courseFolderPath = professorFolder.getPath() + "/" + courseFolderName;

            // Create physical directory
            createPhysicalDirectory(courseFolderPath);

            // Create database entity
            courseFolder = Folder.builder()
                    .path(courseFolderPath)
                    .name(courseFolderName)
                    .type(FolderType.COURSE)
                    .parent(professorFolder)
                    .owner(professor)
                    .academicYear(academicYear)
                    .semester(semester)
                    .course(course)
                    .build();

            courseFolder = folderRepository.save(java.util.Objects.requireNonNull(courseFolder));
            createdFolders.add(courseFolder);

            log.info("Successfully created course folder: {}", courseFolderPath);
        }

        // Create standard subfolders
        for (String subfolderName : STANDARD_SUBFOLDERS) {
            String subfolderPath = courseFolder.getPath() + "/" + subfolderName;

            // Check if subfolder already exists (idempotency)
            Optional<Folder> existingSubfolder = folderRepository.findByPath(subfolderPath);

            if (existingSubfolder.isPresent()) {
                log.debug("Subfolder already exists: {}", subfolderPath);
                createdFolders.add(existingSubfolder.get());
            } else {
                // Create physical directory
                createPhysicalDirectory(subfolderPath);

                // Create database entity
                Folder subfolder = Folder.builder()
                        .path(subfolderPath)
                        .name(subfolderName)
                        .type(FolderType.SUBFOLDER)
                        .parent(courseFolder)
                        .owner(professor)
                        .academicYear(academicYear)
                        .semester(semester)
                        .course(course)
                        .build();

                subfolder = folderRepository.save(java.util.Objects.requireNonNull(subfolder));
                createdFolders.add(subfolder);

                log.debug("Successfully created subfolder: {}", subfolderPath);
            }
        }

        log.info("Successfully created course folder structure with {} folders", createdFolders.size());
        return createdFolders;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean professorFolderExists(Long professorId, Long academicYearId, Long semesterId) {
        log.debug("Checking if professor folder exists for professorId={}, academicYearId={}, semesterId={}",
                professorId, academicYearId, semesterId);

        Optional<Folder> folder = folderRepository.findProfessorRootFolder(
                professorId, academicYearId, semesterId, FolderType.PROFESSOR_ROOT);

        return folder.isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean courseFolderExists(Long professorId, Long courseId,
            Long academicYearId, Long semesterId) {
        log.debug("Checking if course folder exists for professorId={}, courseId={}, academicYearId={}, semesterId={}",
                professorId, courseId, academicYearId, semesterId);

        Optional<Folder> folder = folderRepository.findCourseFolder(
                professorId, courseId, academicYearId, semesterId, FolderType.COURSE);

        return folder.isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Folder> getFolderByPath(String path) {
        log.debug("Getting folder by path: {}", path);
        return folderRepository.findByPath(path);
    }

    @Override
    @Transactional
    public Folder createFolderIfNotExists(String path, String name, Folder parent,
            FolderType type, User owner) {
        log.debug("Creating folder if not exists: path={}, name={}, type={}", path, name, type);

        // Check if folder already exists (idempotency)
        Optional<Folder> existingFolder = folderRepository.findByPath(path);

        if (existingFolder.isPresent()) {
            log.debug("Folder already exists: {}", path);
            return existingFolder.get();
        }

        // Create physical directory
        createPhysicalDirectory(path);

        // Determine academic year and semester from parent or owner
        AcademicYear academicYear = parent != null ? parent.getAcademicYear() : null;
        Semester semester = parent != null ? parent.getSemester() : null;
        Course course = parent != null ? parent.getCourse() : null;

        // Create database entity
        Folder folder = Folder.builder()
                .path(path)
                .name(name)
                .type(type)
                .parent(parent)
                .owner(owner)
                .academicYear(academicYear)
                .semester(semester)
                .course(course)
                .build();

        folder = folderRepository.save(java.util.Objects.requireNonNull(folder));

        log.info("Successfully created folder: {}", path);
        return folder;
    }

    @Override
    @Transactional
    public Folder getOrCreateFolderByPath(String path, Long userId) {
        log.info("Getting or creating folder by path: {}, userId: {}", path, userId);

        // Check if folder already exists
        Optional<Folder> existingFolder = folderRepository.findByPath(path);
        if (existingFolder.isPresent()) {
            log.info("Folder already exists: {}", path);
            return existingFolder.get();
        }

        // Parse the path
        PathParser.PathComponents components;
        try {
            components = PathParser.parse(path);
            log.debug("Parsed path components: {}", components);
        } catch (IllegalArgumentException e) {
            log.error("Invalid path format: {}", path, e);
            throw new IllegalArgumentException("Invalid folder path format: " + path, e);
        }

        // Validate and fetch entities
        AcademicYear academicYear = academicYearRepository.findByYearCode(components.getAcademicYearCode())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Academic year not found: " + components.getAcademicYearCode()));

        SemesterType semesterType;
        try {
            semesterType = SemesterType.valueOf(components.getSemesterType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new EntityNotFoundException("Invalid semester type: " + components.getSemesterType());
        }

        Semester semester = semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Semester not found: " + components.getSemesterType() + " for year "
                                + components.getAcademicYearCode()));

        // Find professor by name format, professorId, or legacy "prof_<id>" format
        User professor = findProfessorByIdentifier(components.getProfessorId());

        // Verify user has permission (user must be the professor)
        if (!professor.getId().equals(userId)) {
            throw new SecurityException(
                    "User does not have permission to create folder for professor: " + components.getProfessorId());
        }

        Course course = courseRepository.findByCourseCode(components.getCourseCode())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Course not found: " + components.getCourseCode()));

        // Ensure professor root folder exists
        createProfessorFolder(professor.getId(), academicYear.getId(), semester.getId());

        // Ensure course folder exists
        List<Folder> courseFolders = createCourseFolderStructure(
                professor.getId(), course.getId(), academicYear.getId(), semester.getId());
        Folder courseFolder = courseFolders.get(0); // First folder is the course folder

        // Format document type name
        String documentTypeName = PathParser.formatDocumentTypeName(components.getDocumentType());

        // Create the document type folder
        String folderPath = path.startsWith("/") ? path.substring(1) : path;

        // Create physical directory
        createPhysicalDirectory(folderPath);

        // Create database entity
        Folder folder = Folder.builder()
                .path(folderPath)
                .name(documentTypeName)
                .type(FolderType.SUBFOLDER)
                .parent(courseFolder)
                .owner(professor)
                .academicYear(academicYear)
                .semester(semester)
                .course(course)
                .build();

        folder = folderRepository.save(java.util.Objects.requireNonNull(folder));

        log.info("Successfully created folder: {}", folderPath);
        return folder;
    }

    /**
     * Create physical directory on the file system.
     * Creates all parent directories if they don't exist.
     *
     * @param folderPath the relative folder path
     */
    private void createPhysicalDirectory(String folderPath) {
        try {
            Path fullPath = Paths.get(uploadDir, folderPath);

            if (!Files.exists(fullPath)) {
                Files.createDirectories(fullPath);
                log.debug("Created physical directory: {}", fullPath);
            } else {
                log.debug("Physical directory already exists: {}", fullPath);
            }
        } catch (IOException e) {
            log.error("Failed to create physical directory: {}", folderPath, e);
            throw new RuntimeException("Failed to create physical directory: " + folderPath, e);
        }
    }
}
