package com.alquds.edu.ArchiveSystem.service.core;

import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear;
import com.alquds.edu.ArchiveSystem.repository.academic.CourseRepository;
import com.alquds.edu.ArchiveSystem.service.file.FolderService;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.entity.academic.CourseAssignment;
import com.alquds.edu.ArchiveSystem.entity.submission.SubmittedDocument;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.entity.file.FileAttachment;
import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentRequest;
import com.alquds.edu.ArchiveSystem.repository.submission.SubmittedDocumentRepository;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.repository.file.FileAttachmentRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.AcademicYearRepository;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.alquds.edu.ArchiveSystem.repository.submission.RequiredDocumentTypeRepository;
import com.alquds.edu.ArchiveSystem.entity.academic.SemesterType;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.repository.file.UploadedFileRepository;
import com.alquds.edu.ArchiveSystem.entity.submission.RequiredDocumentType;
import com.alquds.edu.ArchiveSystem.repository.academic.SemesterRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentRequestRepository;
import com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus;
import com.alquds.edu.ArchiveSystem.entity.file.Folder;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for migrating data from old request-based schema to new semester-based schema
 * 
 * NOTE: This service intentionally uses legacy entities (DocumentRequest, SubmittedDocument, FileAttachment)
 * to perform data migration from the old system to the new semester-based structure.
 * Deprecation warnings are suppressed as this is the intended purpose of this service.
 * 
 * This service is critical for:
 * - Initial data migration from legacy system
 * - Rollback procedures
 * - Data validation and integrity checks
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
public class DataMigrationService {

    private final DocumentRequestRepository documentRequestRepository;
    private final SubmittedDocumentRepository submittedDocumentRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final UserRepository userRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final DocumentSubmissionRepository documentSubmissionRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final RequiredDocumentTypeRepository requiredDocumentTypeRepository;
    private final FolderService folderService;

    @Value("${app.upload.base-path:./uploads}")
    private String uploadBasePath;

    /**
     * Analyzes existing data to determine migration scope
     */
    public MigrationAnalysis analyzeExistingData() {
        log.info("Starting data analysis for migration...");
        
        MigrationAnalysis analysis = new MigrationAnalysis();
        
        // Analyze document requests
        List<DocumentRequest> allRequests = documentRequestRepository.findAll();
        analysis.setTotalRequests(allRequests.size());
        
        // Extract year ranges from deadlines
        Set<Integer> years = allRequests.stream()
                .map(dr -> dr.getDeadline().getYear())
                .collect(Collectors.toSet());
        analysis.setYearRange(years);
        
        // Extract unique course names
        Set<String> courseNames = allRequests.stream()
                .map(DocumentRequest::getCourseName)
                .collect(Collectors.toSet());
        analysis.setUniqueCourseNames(courseNames);
        
        // Extract unique professors
        Set<Long> professorIds = allRequests.stream()
                .map(dr -> dr.getProfessor().getId())
                .collect(Collectors.toSet());
        analysis.setUniqueProfessorIds(professorIds);
        
        // Count submitted documents
        long submittedCount = submittedDocumentRepository.count();
        analysis.setTotalSubmittedDocuments(submittedCount);
        
        // Count file attachments
        long fileCount = fileAttachmentRepository.count();
        analysis.setTotalFileAttachments(fileCount);
        
        // Extract document types
        Set<String> documentTypes = allRequests.stream()
                .map(DocumentRequest::getDocumentType)
                .collect(Collectors.toSet());
        analysis.setUniqueDocumentTypes(documentTypes);
        
        log.info("Analysis complete: {} requests, {} courses, {} professors, {} files",
                analysis.getTotalRequests(), courseNames.size(), professorIds.size(), fileCount);
        
        return analysis;
    }

    /**
     * Creates academic years and semesters based on existing request deadlines
     */
    @Transactional
    public Map<Integer, AcademicYear> createAcademicYearsFromData() {
        log.info("Creating academic years from existing data...");
        
        List<DocumentRequest> allRequests = documentRequestRepository.findAll();
        Map<Integer, AcademicYear> academicYearMap = new HashMap<>();
        
        // Group requests by year
        Set<Integer> years = allRequests.stream()
                .map(dr -> dr.getDeadline().getYear())
                .collect(Collectors.toSet());
        
        for (Integer year : years) {
            // Create academic year (e.g., 2024-2025)
            String yearCode = year + "-" + (year + 1);
            
            // Check if already exists
            Optional<AcademicYear> existing = academicYearRepository.findByYearCode(yearCode);
            if (existing.isPresent()) {
                log.info("Academic year {} already exists, skipping", yearCode);
                academicYearMap.put(year, existing.get());
                continue;
            }
            
            AcademicYear academicYear = new AcademicYear();
            academicYear.setYearCode(yearCode);
            academicYear.setStartYear(year);
            academicYear.setEndYear(year + 1);
            academicYear.setIsActive(false); // Will be set manually by admin
            
            academicYear = academicYearRepository.save(academicYear);
            log.info("Created academic year: {}", yearCode);
            
            // Create three semesters for this year
            createSemestersForYear(academicYear, year);
            
            academicYearMap.put(year, academicYear);
        }
        
        log.info("Created {} academic years", academicYearMap.size());
        return academicYearMap;
    }

    /**
     * Creates three semesters (FIRST, SECOND, SUMMER) for an academic year
     */
    private void createSemestersForYear(AcademicYear academicYear, int startYear) {
        // First Semester (Fall): September - December
        Semester firstSemester = new Semester();
        firstSemester.setAcademicYear(academicYear);
        firstSemester.setType(SemesterType.FIRST);
        firstSemester.setStartDate(LocalDate.of(startYear, Month.SEPTEMBER, 1));
        firstSemester.setEndDate(LocalDate.of(startYear, Month.DECEMBER, 31));
        firstSemester.setIsActive(true);
        semesterRepository.save(firstSemester);
        log.debug("Created FIRST semester for {}", academicYear.getYearCode());
        
        // Second Semester (Spring): January - May
        Semester secondSemester = new Semester();
        secondSemester.setAcademicYear(academicYear);
        secondSemester.setType(SemesterType.SECOND);
        secondSemester.setStartDate(LocalDate.of(startYear + 1, Month.JANUARY, 1));
        secondSemester.setEndDate(LocalDate.of(startYear + 1, Month.MAY, 31));
        secondSemester.setIsActive(true);
        semesterRepository.save(secondSemester);
        log.debug("Created SECOND semester for {}", academicYear.getYearCode());
        
        // Summer Semester: June - August
        Semester summerSemester = new Semester();
        summerSemester.setAcademicYear(academicYear);
        summerSemester.setType(SemesterType.SUMMER);
        summerSemester.setStartDate(LocalDate.of(startYear + 1, Month.JUNE, 1));
        summerSemester.setEndDate(LocalDate.of(startYear + 1, Month.AUGUST, 31));
        summerSemester.setIsActive(true);
        semesterRepository.save(summerSemester);
        log.debug("Created SUMMER semester for {}", academicYear.getYearCode());
    }

    /**
     * Migrates professors by generating professor_id for existing ROLE_PROFESSOR users
     */
    @Transactional
    public int migrateProfessors() {
        log.info("Migrating professors...");
        
        List<User> professors = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_PROFESSOR)
                .filter(u -> u.getProfessorId() == null) // Only migrate those without professor_id
                .collect(Collectors.toList());
        
        int migratedCount = 0;
        for (User professor : professors) {
            String professorId = generateProfessorId(professor);
            professor.setProfessorId(professorId);
            userRepository.save(professor);
            log.debug("Generated professor_id {} for user {}", professorId, professor.getEmail());
            migratedCount++;
        }
        
        log.info("Migrated {} professors", migratedCount);
        return migratedCount;
    }

    /**
     * Generates a unique professor ID
     */
    private String generateProfessorId(User professor) {
        // Format: prof_{id}_{firstLetter}{lastLetter}
        String firstLetter = professor.getFirstName().substring(0, 1).toLowerCase();
        String lastLetter = professor.getLastName().substring(0, 1).toLowerCase();
        return String.format("prof_%d_%s%s", professor.getId(), firstLetter, lastLetter);
    }

    /**
     * Extracts unique courses from document requests and creates course records
     */
    @Transactional
    public Map<String, Course> extractAndCreateCourses() {
        log.info("Extracting and creating courses...");
        
        List<DocumentRequest> allRequests = documentRequestRepository.findAll();
        Map<String, Course> courseMap = new HashMap<>();
        
        // Group by course name and department
        Map<String, List<DocumentRequest>> courseGroups = allRequests.stream()
                .collect(Collectors.groupingBy(dr -> 
                    dr.getCourseName() + "_" + dr.getProfessor().getDepartment().getId()
                ));
        
        for (Map.Entry<String, List<DocumentRequest>> entry : courseGroups.entrySet()) {
            List<DocumentRequest> requests = entry.getValue();
            DocumentRequest firstRequest = requests.get(0);
            
            String courseName = firstRequest.getCourseName();
            Department department = firstRequest.getProfessor().getDepartment();
            
            // Generate course code from course name
            String courseCode = generateCourseCode(courseName, department);
            
            // Check if course already exists
            Optional<Course> existing = courseRepository.findByCourseCode(courseCode);
            if (existing.isPresent()) {
                log.debug("Course {} already exists, skipping", courseCode);
                courseMap.put(courseName + "_" + department.getId(), existing.get());
                continue;
            }
            
            // Create new course
            Course course = new Course();
            course.setCourseCode(courseCode);
            course.setCourseName(courseName);
            course.setDepartment(department);
            course.setLevel("Undergraduate"); // Default level
            course.setDescription("Migrated from legacy system");
            course.setIsActive(true);
            
            course = courseRepository.save(course);
            log.info("Created course: {} - {}", courseCode, courseName);
            
            courseMap.put(courseName + "_" + department.getId(), course);
        }
        
        log.info("Created {} courses", courseMap.size());
        return courseMap;
    }

    /**
     * Generates a course code from course name
     */
    private String generateCourseCode(String courseName, Department department) {
        // Extract department prefix (first 2-3 letters)
        String deptPrefix = department.getName().substring(0, Math.min(3, department.getName().length()))
                .toUpperCase().replaceAll("[^A-Z]", "");
        
        // Extract numbers from course name if any
        String numbers = courseName.replaceAll("[^0-9]", "");
        if (numbers.isEmpty()) {
            numbers = String.valueOf(Math.abs(courseName.hashCode()) % 1000);
        }
        
        // Ensure unique by checking existing codes
        String baseCode = deptPrefix + numbers;
        String courseCode = baseCode;
        int suffix = 1;
        
        while (courseRepository.findByCourseCode(courseCode).isPresent()) {
            courseCode = baseCode + "_" + suffix;
            suffix++;
        }
        
        return courseCode;
    }

    /**
     * Creates course assignments from document requests
     */
    @Transactional
    public int createCourseAssignmentsFromRequests(
            Map<Integer, AcademicYear> academicYearMap,
            Map<String, Course> courseMap) {
        log.info("Creating course assignments from requests...");
        
        List<DocumentRequest> allRequests = documentRequestRepository.findAll();
        int assignmentCount = 0;
        
        for (DocumentRequest request : allRequests) {
            try {
                // Determine semester from deadline
                LocalDateTime deadline = request.getDeadline();
                int year = deadline.getYear();
                Month month = deadline.getMonth();
                
                AcademicYear academicYear = academicYearMap.get(year);
                if (academicYear == null) {
                    log.warn("No academic year found for year {}, skipping request {}", year, request.getId());
                    continue;
                }
                
                // Determine semester type based on month
                SemesterType semesterType = determineSemesterType(month);
                
                // Find the semester
                Semester semester = semesterRepository
                        .findByAcademicYearIdAndType(academicYear.getId(), semesterType)
                        .orElse(null);
                
                if (semester == null) {
                    log.warn("No semester found for year {} and type {}, skipping request {}", 
                            year, semesterType, request.getId());
                    continue;
                }
                
                // Find the course
                String courseKey = request.getCourseName() + "_" + 
                        request.getProfessor().getDepartment().getId();
                Course course = courseMap.get(courseKey);
                
                if (course == null) {
                    log.warn("No course found for key {}, skipping request {}", courseKey, request.getId());
                    continue;
                }
                
                // Check if assignment already exists
                Optional<CourseAssignment> existing = courseAssignmentRepository
                        .findBySemesterIdAndCourseIdAndProfessorId(
                                semester.getId(), 
                                course.getId(), 
                                request.getProfessor().getId()
                        );
                
                if (existing.isPresent()) {
                    log.debug("Course assignment already exists for professor {} in course {} for semester {}", 
                            request.getProfessor().getEmail(), course.getCourseCode(), semester.getType());
                    continue;
                }
                
                // Create course assignment
                CourseAssignment assignment = new CourseAssignment();
                assignment.setSemester(semester);
                assignment.setCourse(course);
                assignment.setProfessor(request.getProfessor());
                assignment.setIsActive(true);
                
                courseAssignmentRepository.save(assignment);
                log.debug("Created course assignment: {} - {} - {}", 
                        course.getCourseCode(), request.getProfessor().getEmail(), semester.getType());
                
                assignmentCount++;
            } catch (Exception e) {
                log.error("Error creating course assignment for request {}: {}", 
                        request.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Created {} course assignments", assignmentCount);
        return assignmentCount;
    }

    /**
     * Determines semester type based on month
     */
    private SemesterType determineSemesterType(Month month) {
        switch (month) {
            case SEPTEMBER:
            case OCTOBER:
            case NOVEMBER:
            case DECEMBER:
                return SemesterType.FIRST;
            case JANUARY:
            case FEBRUARY:
            case MARCH:
            case APRIL:
            case MAY:
                return SemesterType.SECOND;
            case JUNE:
            case JULY:
            case AUGUST:
                return SemesterType.SUMMER;
            default:
                return SemesterType.FIRST; // Default fallback
        }
    }

    /**
     * Migrates submitted documents to document submissions
     */
    @Transactional
    public int migrateDocumentSubmissions(Map<String, Course> courseMap) {
        log.info("Migrating document submissions...");
        
        List<SubmittedDocument> allSubmissions = submittedDocumentRepository.findAll();
        int migratedCount = 0;
        
        for (SubmittedDocument oldSubmission : allSubmissions) {
            try {
                DocumentRequest request = oldSubmission.getDocumentRequest();
                
                // Find corresponding course assignment
                LocalDateTime deadline = request.getDeadline();
                int year = deadline.getYear();
                Month month = deadline.getMonth();
                SemesterType semesterType = determineSemesterType(month);
                
                // Find academic year
                String yearCode = year + "-" + (year + 1);
                Optional<AcademicYear> academicYearOpt = academicYearRepository.findByYearCode(yearCode);
                if (!academicYearOpt.isPresent()) {
                    log.warn("No academic year found for {}, skipping submission {}", 
                            yearCode, oldSubmission.getId());
                    continue;
                }
                
                // Find semester
                Semester semester = semesterRepository
                        .findByAcademicYearIdAndType(academicYearOpt.get().getId(), semesterType)
                        .orElse(null);
                
                if (semester == null) {
                    log.warn("No semester found, skipping submission {}", oldSubmission.getId());
                    continue;
                }
                
                // Find course
                String courseKey = request.getCourseName() + "_" + 
                        request.getProfessor().getDepartment().getId();
                Course course = courseMap.get(courseKey);
                
                if (course == null) {
                    log.warn("No course found for key {}, skipping submission {}", 
                            courseKey, oldSubmission.getId());
                    continue;
                }
                
                // Find course assignment
                Optional<CourseAssignment> assignmentOpt = courseAssignmentRepository
                        .findBySemesterIdAndCourseIdAndProfessorId(
                                semester.getId(), 
                                course.getId(), 
                                request.getProfessor().getId()
                        );
                
                if (!assignmentOpt.isPresent()) {
                    log.warn("No course assignment found, skipping submission {}", oldSubmission.getId());
                    continue;
                }
                
                CourseAssignment assignment = assignmentOpt.get();
                
                // Map document type to enum
                DocumentTypeEnum documentType = mapDocumentType(request.getDocumentType());
                
                // Check if document submission already exists
                List<DocumentSubmission> existingSubmissions = documentSubmissionRepository
                        .findAllByCourseAssignmentIdAndDocumentType(assignment.getId(), documentType);
                
                if (!existingSubmissions.isEmpty()) {
                    log.debug("Document submission already exists for assignment {} and type {}", 
                            assignment.getId(), documentType);
                    continue;
                }
                
                // Create new document submission
                DocumentSubmission newSubmission = new DocumentSubmission();
                newSubmission.setCourseAssignment(assignment);
                newSubmission.setDocumentType(documentType);
                newSubmission.setProfessor(oldSubmission.getProfessor());
                newSubmission.setSubmittedAt(oldSubmission.getSubmittedAt());
                newSubmission.setIsLateSubmission(oldSubmission.getIsLateSubmission());
                newSubmission.setNotes(oldSubmission.getNotes());
                newSubmission.setFileCount(oldSubmission.getFileCount());
                newSubmission.setTotalFileSize(oldSubmission.getTotalFileSize());
                
                // Set status based on submission
                if (oldSubmission.getIsLateSubmission()) {
                    newSubmission.setStatus(SubmissionStatus.UPLOADED);
                } else {
                    newSubmission.setStatus(SubmissionStatus.UPLOADED);
                }
                
                documentSubmissionRepository.save(newSubmission);
                log.debug("Migrated submission {} to new document submission", oldSubmission.getId());
                
                migratedCount++;
            } catch (Exception e) {
                log.error("Error migrating submission {}: {}", 
                        oldSubmission.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Migrated {} document submissions", migratedCount);
        return migratedCount;
    }

    /**
     * Maps old document type string to new enum
     */
    private DocumentTypeEnum mapDocumentType(String oldType) {
        if (oldType == null) {
            return DocumentTypeEnum.OTHER;
        }
        
        String normalized = oldType.toUpperCase().replaceAll("[\\s-]", "_");
        
        try {
            return DocumentTypeEnum.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try partial matches
            if (normalized.contains("SYLLABUS")) return DocumentTypeEnum.SYLLABUS;
            if (normalized.contains("EXAM")) return DocumentTypeEnum.EXAM;
            if (normalized.contains("ASSIGNMENT")) return DocumentTypeEnum.ASSIGNMENT;
            if (normalized.contains("PROJECT")) return DocumentTypeEnum.PROJECT_DOCS;
            if (normalized.contains("LECTURE") || normalized.contains("NOTE")) 
                return DocumentTypeEnum.LECTURE_NOTES;
            
            return DocumentTypeEnum.OTHER;
        }
    }

    /**
     * Migrates files to new hierarchical folder structure
     */
    @Transactional
    public int migrateFilesToNewStructure(Map<String, Course> courseMap) {
        log.info("Migrating files to new folder structure...");
        
        List<FileAttachment> allFiles = fileAttachmentRepository.findAll();
        int migratedCount = 0;
        
        for (FileAttachment oldFile : allFiles) {
            try {
                SubmittedDocument oldSubmission = oldFile.getSubmittedDocument();
                DocumentRequest request = oldSubmission.getDocumentRequest();
                
                // Find the new document submission
                LocalDateTime deadline = request.getDeadline();
                int year = deadline.getYear();
                Month month = deadline.getMonth();
                SemesterType semesterType = determineSemesterType(month);
                
                String yearCode = year + "-" + (year + 1);
                Optional<AcademicYear> academicYearOpt = academicYearRepository.findByYearCode(yearCode);
                if (!academicYearOpt.isPresent()) {
                    log.warn("No academic year found for {}, skipping file {}", yearCode, oldFile.getId());
                    continue;
                }
                
                Semester semester = semesterRepository
                        .findByAcademicYearIdAndType(academicYearOpt.get().getId(), semesterType)
                        .orElse(null);
                
                if (semester == null) {
                    log.warn("No semester found, skipping file {}", oldFile.getId());
                    continue;
                }
                
                String courseKey = request.getCourseName() + "_" + 
                        request.getProfessor().getDepartment().getId();
                Course course = courseMap.get(courseKey);
                
                if (course == null) {
                    log.warn("No course found, skipping file {}", oldFile.getId());
                    continue;
                }
                
                Optional<CourseAssignment> assignmentOpt = courseAssignmentRepository
                        .findBySemesterIdAndCourseIdAndProfessorId(
                                semester.getId(), 
                                course.getId(), 
                                request.getProfessor().getId()
                        );
                
                if (!assignmentOpt.isPresent()) {
                    log.warn("No course assignment found, skipping file {}", oldFile.getId());
                    continue;
                }
                
                DocumentTypeEnum documentType = mapDocumentType(request.getDocumentType());
                
                List<DocumentSubmission> submissions = documentSubmissionRepository
                        .findAllByCourseAssignmentIdAndDocumentType(
                                assignmentOpt.get().getId(), 
                                documentType
                        );
                
                if (submissions.isEmpty()) {
                    log.warn("No document submission found, skipping file {}", oldFile.getId());
                    continue;
                }
                
                DocumentSubmission newSubmission = submissions.get(0);
                
                // Generate new file path
                String professorId = request.getProfessor().getProfessorId();
                if (professorId == null) {
                    professorId = "prof_" + request.getProfessor().getId();
                }
                
                String newFilePath = generateNewFilePath(
                        yearCode,
                        semesterType.name().toLowerCase(),
                        professorId,
                        course.getCourseCode(),
                        documentType.name().toLowerCase(),
                        oldFile.getOriginalFilename()
                );
                
                // Move physical file
                boolean fileMoved = movePhysicalFile(oldFile.getFileUrl(), newFilePath);
                
                if (!fileMoved) {
                    log.warn("Failed to move physical file for {}, using old path", oldFile.getId());
                    newFilePath = oldFile.getFileUrl(); // Keep old path if move fails
                }
                
                // Create new uploaded file record
                UploadedFile newFile = new UploadedFile();
                newFile.setDocumentSubmission(newSubmission);
                newFile.setFileUrl(newFilePath);
                newFile.setOriginalFilename(oldFile.getOriginalFilename());
                newFile.setFileSize(oldFile.getFileSize());
                newFile.setFileType(oldFile.getFileType());
                newFile.setFileOrder(oldFile.getFileOrder());
                newFile.setDescription(oldFile.getDescription());
                
                uploadedFileRepository.save(newFile);
                log.debug("Migrated file {} to new structure at {}", 
                        oldFile.getOriginalFilename(), newFilePath);
                
                migratedCount++;
            } catch (Exception e) {
                log.error("Error migrating file {}: {}", oldFile.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Migrated {} files to new structure", migratedCount);
        return migratedCount;
    }

    /**
     * Generates new hierarchical file path
     */
    private String generateNewFilePath(String yearCode, String semesterType, 
                                      String professorId, String courseCode, 
                                      String documentType, String filename) {
        // Format: {year}/{semester}/{professorId}/{courseCode}/{documentType}/{filename}
        return String.format("%s/%s/%s/%s/%s/%s", 
                yearCode, semesterType, professorId, courseCode, documentType, filename);
    }

    /**
     * Moves physical file from old location to new location
     */
    private boolean movePhysicalFile(String oldPath, String newPath) {
        try {
            Path oldFilePath = Paths.get(uploadBasePath, oldPath);
            Path newFilePath = Paths.get(uploadBasePath, newPath);
            
            // Check if old file exists
            if (!Files.exists(oldFilePath)) {
                log.warn("Old file does not exist: {}", oldFilePath);
                return false;
            }
            
            // Create parent directories for new path
            Files.createDirectories(newFilePath.getParent());
            
            // Move file
            Files.move(oldFilePath, newFilePath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Moved file from {} to {}", oldFilePath, newFilePath);
            
            return true;
        } catch (IOException e) {
            log.error("Error moving file from {} to {}: {}", oldPath, newPath, e.getMessage());
            return false;
        }
    }

    /**
     * Extracts required document types from document requests
     */
    @Transactional
    public int extractRequiredDocumentTypes(Map<String, Course> courseMap) {
        log.info("Extracting required document types...");
        
        List<DocumentRequest> allRequests = documentRequestRepository.findAll();
        int createdCount = 0;
        
        // Group by course and document type
        Map<String, List<DocumentRequest>> groupedRequests = allRequests.stream()
                .collect(Collectors.groupingBy(dr -> 
                    dr.getCourseName() + "_" + 
                    dr.getProfessor().getDepartment().getId() + "_" + 
                    dr.getDocumentType()
                ));
        
        for (Map.Entry<String, List<DocumentRequest>> entry : groupedRequests.entrySet()) {
            try {
                List<DocumentRequest> requests = entry.getValue();
                DocumentRequest firstRequest = requests.get(0);
                
                // Find course
                String courseKey = firstRequest.getCourseName() + "_" + 
                        firstRequest.getProfessor().getDepartment().getId();
                Course course = courseMap.get(courseKey);
                
                if (course == null) {
                    log.warn("No course found for key {}, skipping", courseKey);
                    continue;
                }
                
                DocumentTypeEnum documentType = mapDocumentType(firstRequest.getDocumentType());
                
                // Check if required document type already exists
                List<RequiredDocumentType> existing = requiredDocumentTypeRepository
                        .findByCourseIdAndDocumentType(course.getId(), documentType);
                
                if (!existing.isEmpty()) {
                    log.debug("Required document type already exists for course {} and type {}", 
                            course.getCourseCode(), documentType);
                    continue;
                }
                
                // Create required document type
                RequiredDocumentType requiredDocType = new RequiredDocumentType();
                requiredDocType.setCourse(course);
                requiredDocType.setDocumentType(documentType);
                requiredDocType.setIsRequired(true);
                
                // Use the most recent deadline from the requests
                LocalDateTime latestDeadline = requests.stream()
                        .map(DocumentRequest::getDeadline)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);
                requiredDocType.setDeadline(latestDeadline);
                
                // Set file constraints from first request
                requiredDocType.setMaxFileCount(firstRequest.getMaxFileCount());
                requiredDocType.setMaxTotalSizeMb(firstRequest.getMaxTotalSizeMb());
                
                // Set allowed extensions
                if (firstRequest.getRequiredFileExtensions() != null && 
                    !firstRequest.getRequiredFileExtensions().isEmpty()) {
                    requiredDocType.setAllowedFileExtensions(
                            new ArrayList<>(firstRequest.getRequiredFileExtensions())
                    );
                } else {
                    // Default extensions
                    requiredDocType.setAllowedFileExtensions(Arrays.asList("pdf", "zip"));
                }
                
                requiredDocumentTypeRepository.save(requiredDocType);
                log.debug("Created required document type: {} for course {}", 
                        documentType, course.getCourseCode());
                
                createdCount++;
            } catch (Exception e) {
                log.error("Error creating required document type: {}", e.getMessage(), e);
            }
        }
        
        log.info("Created {} required document types", createdCount);
        return createdCount;
    }

    /**
     * Executes the complete migration process
     */
    @Transactional
    public MigrationResult executeFullMigration() {
        log.info("Starting full data migration...");
        
        MigrationResult result = new MigrationResult();
        
        try {
            // Step 1: Analyze data
            MigrationAnalysis analysis = analyzeExistingData();
            result.setAnalysis(analysis);
            
            // Step 2: Create academic years and semesters
            Map<Integer, AcademicYear> academicYearMap = createAcademicYearsFromData();
            result.setAcademicYearsCreated(academicYearMap.size());
            
            // Step 3: Migrate professors
            int professorsCount = migrateProfessors();
            result.setProfessorsMigrated(professorsCount);
            
            // Step 4: Extract and create courses
            Map<String, Course> courseMap = extractAndCreateCourses();
            result.setCoursesCreated(courseMap.size());
            
            // Step 5: Create course assignments
            int assignmentsCount = createCourseAssignmentsFromRequests(academicYearMap, courseMap);
            result.setCourseAssignmentsCreated(assignmentsCount);
            
            // Step 6: Migrate document submissions
            int submissionsCount = migrateDocumentSubmissions(courseMap);
            result.setDocumentSubmissionsMigrated(submissionsCount);
            
            // Step 7: Migrate files
            int filesCount = migrateFilesToNewStructure(courseMap);
            result.setFilesMigrated(filesCount);
            
            // Step 8: Extract required document types
            int requiredDocTypesCount = extractRequiredDocumentTypes(courseMap);
            result.setRequiredDocumentTypesCreated(requiredDocTypesCount);
            
            result.setSuccess(true);
            log.info("Migration completed successfully!");
            
        } catch (Exception e) {
            log.error("Migration failed: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    /**
     * Rebuilds folder structures for all existing course assignments.
     * Creates professor folders and course subfolders (Syllabus, Exams, Course Notes, Assignments)
     * for assignments that don't have them yet.
     * 
     * This method is idempotent - it will skip folders that already exist.
     * 
     * @return FolderRebuildResult containing statistics about the rebuild operation
     */
    @Transactional
    public FolderRebuildResult rebuildCourseFolders() {
        log.info("Starting folder rebuild for all course assignments...");
        
        FolderRebuildResult result = new FolderRebuildResult();
        
        try {
            // Get all course assignments
            List<CourseAssignment> allAssignments = courseAssignmentRepository.findAll();
            result.setTotalAssignments(allAssignments.size());
            
            log.info("Found {} course assignments to process", allAssignments.size());
            
            // Track unique professors
            Set<Long> processedProfessors = new HashSet<>();
            int professorFoldersCreated = 0;
            int courseFoldersCreated = 0;
            int subfoldersCreated = 0;
            
            for (CourseAssignment assignment : allAssignments) {
                try {
                    Long professorId = assignment.getProfessor().getId();
                    Long courseId = assignment.getCourse().getId();
                    Long academicYearId = assignment.getSemester().getAcademicYear().getId();
                    Long semesterId = assignment.getSemester().getId();
                    
                    // Create professor folder if not exists
                    if (!processedProfessors.contains(professorId)) {
                        boolean professorFolderExisted = folderService.professorFolderExists(
                                professorId, academicYearId, semesterId);
                        
                        folderService.createProfessorFolder(professorId, academicYearId, semesterId);
                        
                        if (!professorFolderExisted) {
                            professorFoldersCreated++;
                            log.debug("Created professor folder for professor ID: {}", professorId);
                        }
                        
                        processedProfessors.add(professorId);
                    }
                    
                    // Create course folder structure
                    boolean courseFolderExisted = folderService.courseFolderExists(
                            professorId, courseId, academicYearId, semesterId);
                    
                    List<Folder> createdFolders = folderService.createCourseFolderStructure(
                            professorId, courseId, academicYearId, semesterId);
                    
                    if (!courseFolderExisted) {
                        // Count course folder + subfolders
                        courseFoldersCreated++;
                        // Subtract 1 for the course folder itself to get subfolder count
                        subfoldersCreated += (createdFolders.size() - 1);
                        
                        log.debug("Created course folder structure for assignment ID: {} ({} folders)", 
                                assignment.getId(), createdFolders.size());
                    }
                    
                } catch (Exception e) {
                    String error = String.format("Failed to create folders for assignment ID %d: %s", 
                            assignment.getId(), e.getMessage());
                    log.error(error, e);
                    result.addError(error);
                }
            }
            
            result.setProfessorsProcessed(processedProfessors.size());
            result.setProfessorFoldersCreated(professorFoldersCreated);
            result.setCourseFoldersCreated(courseFoldersCreated);
            result.setSubfoldersCreated(subfoldersCreated);
            result.setSuccess(true);
            
            log.info("Folder rebuild completed: {} professors, {} professor folders created, " +
                    "{} course folders created, {} subfolders created, {} errors",
                    processedProfessors.size(), professorFoldersCreated, 
                    courseFoldersCreated, subfoldersCreated, result.getErrors().size());
            
        } catch (Exception e) {
            log.error("Folder rebuild failed: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    /**
     * Data class to hold migration results
     */
    public static class MigrationResult {
        private boolean success;
        private String errorMessage;
        private MigrationAnalysis analysis;
        private int academicYearsCreated;
        private int professorsMigrated;
        private int coursesCreated;
        private int courseAssignmentsCreated;
        private int documentSubmissionsMigrated;
        private int filesMigrated;
        private int requiredDocumentTypesCreated;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public MigrationAnalysis getAnalysis() { return analysis; }
        public void setAnalysis(MigrationAnalysis analysis) { this.analysis = analysis; }
        
        public int getAcademicYearsCreated() { return academicYearsCreated; }
        public void setAcademicYearsCreated(int academicYearsCreated) { 
            this.academicYearsCreated = academicYearsCreated; 
        }
        
        public int getProfessorsMigrated() { return professorsMigrated; }
        public void setProfessorsMigrated(int professorsMigrated) { 
            this.professorsMigrated = professorsMigrated; 
        }
        
        public int getCoursesCreated() { return coursesCreated; }
        public void setCoursesCreated(int coursesCreated) { this.coursesCreated = coursesCreated; }
        
        public int getCourseAssignmentsCreated() { return courseAssignmentsCreated; }
        public void setCourseAssignmentsCreated(int courseAssignmentsCreated) { 
            this.courseAssignmentsCreated = courseAssignmentsCreated; 
        }
        
        public int getDocumentSubmissionsMigrated() { return documentSubmissionsMigrated; }
        public void setDocumentSubmissionsMigrated(int documentSubmissionsMigrated) { 
            this.documentSubmissionsMigrated = documentSubmissionsMigrated; 
        }
        
        public int getFilesMigrated() { return filesMigrated; }
        public void setFilesMigrated(int filesMigrated) { this.filesMigrated = filesMigrated; }
        
        public int getRequiredDocumentTypesCreated() { return requiredDocumentTypesCreated; }
        public void setRequiredDocumentTypesCreated(int requiredDocumentTypesCreated) { 
            this.requiredDocumentTypesCreated = requiredDocumentTypesCreated; 
        }
    }

    /**
     * Data class to hold migration analysis results
     */
    public static class MigrationAnalysis {
        private int totalRequests;
        private Set<Integer> yearRange;
        private Set<String> uniqueCourseNames;
        private Set<Long> uniqueProfessorIds;
        private long totalSubmittedDocuments;
        private long totalFileAttachments;
        private Set<String> uniqueDocumentTypes;

        // Getters and setters
        public int getTotalRequests() { return totalRequests; }
        public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
        
        public Set<Integer> getYearRange() { return yearRange; }
        public void setYearRange(Set<Integer> yearRange) { this.yearRange = yearRange; }
        
        public Set<String> getUniqueCourseNames() { return uniqueCourseNames; }
        public void setUniqueCourseNames(Set<String> uniqueCourseNames) { 
            this.uniqueCourseNames = uniqueCourseNames; 
        }
        
        public Set<Long> getUniqueProfessorIds() { return uniqueProfessorIds; }
        public void setUniqueProfessorIds(Set<Long> uniqueProfessorIds) { 
            this.uniqueProfessorIds = uniqueProfessorIds; 
        }
        
        public long getTotalSubmittedDocuments() { return totalSubmittedDocuments; }
        public void setTotalSubmittedDocuments(long totalSubmittedDocuments) { 
            this.totalSubmittedDocuments = totalSubmittedDocuments; 
        }
        
        public long getTotalFileAttachments() { return totalFileAttachments; }
        public void setTotalFileAttachments(long totalFileAttachments) { 
            this.totalFileAttachments = totalFileAttachments; 
        }
        
        public Set<String> getUniqueDocumentTypes() { return uniqueDocumentTypes; }
        public void setUniqueDocumentTypes(Set<String> uniqueDocumentTypes) { 
            this.uniqueDocumentTypes = uniqueDocumentTypes; 
        }
    }

    /**
     * Data class to hold folder rebuild results
     */
    public static class FolderRebuildResult {
        private boolean success;
        private String errorMessage;
        private int totalAssignments;
        private int professorsProcessed;
        private int professorFoldersCreated;
        private int courseFoldersCreated;
        private int subfoldersCreated;
        private List<String> errors;

        public FolderRebuildResult() {
            this.errors = new ArrayList<>();
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public int getTotalAssignments() { return totalAssignments; }
        public void setTotalAssignments(int totalAssignments) { this.totalAssignments = totalAssignments; }
        
        public int getProfessorsProcessed() { return professorsProcessed; }
        public void setProfessorsProcessed(int professorsProcessed) { 
            this.professorsProcessed = professorsProcessed; 
        }
        
        public int getProfessorFoldersCreated() { return professorFoldersCreated; }
        public void setProfessorFoldersCreated(int professorFoldersCreated) { 
            this.professorFoldersCreated = professorFoldersCreated; 
        }
        
        public int getCourseFoldersCreated() { return courseFoldersCreated; }
        public void setCourseFoldersCreated(int courseFoldersCreated) { 
            this.courseFoldersCreated = courseFoldersCreated; 
        }
        
        public int getSubfoldersCreated() { return subfoldersCreated; }
        public void setSubfoldersCreated(int subfoldersCreated) { 
            this.subfoldersCreated = subfoldersCreated; 
        }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        
        public void addError(String error) {
            this.errors.add(error);
        }
    }
}
