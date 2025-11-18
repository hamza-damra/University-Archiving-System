package com.alqude.edu.ArchiveSystem.config;

import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.repository.*;
import com.alqude.edu.ArchiveSystem.util.DateCalculator;
import com.alqude.edu.ArchiveSystem.util.MockDataConstants;
import com.alqude.edu.ArchiveSystem.util.NameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data initializer for development and testing purposes.
 * 
 * NOTE: This class intentionally uses legacy entities (DocumentRequest, SubmittedDocument)
 * to create sample data for testing the migration and backward compatibility.
 * Deprecation warnings are suppressed as this is for development/testing only.
 * 
 * This component is disabled in production environments and can be controlled via
 * the mock.data.enabled configuration property.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("deprecation")
@org.springframework.context.annotation.Profile("!prod")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "mock.data.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class DataInitializer implements CommandLineRunner {
    
    // Legacy repositories
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final SubmittedDocumentRepository submittedDocumentRepository;
    private final NotificationRepository notificationRepository;
    
    // New semester-based system repositories
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final RequiredDocumentTypeRepository requiredDocumentTypeRepository;
    private final DocumentSubmissionRepository documentSubmissionRepository;
    private final UploadedFileRepository uploadedFileRepository;
    
    // Utilities
    private final PasswordEncoder passwordEncoder;
    
    // Configuration properties
    @Value("${mock.data.enabled:true}")
    private boolean mockDataEnabled;
    
    @Value("${mock.data.skip-if-exists:true}")
    private boolean skipIfExists;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        long startTime = System.currentTimeMillis();
        
        try {
            // Configuration check
            if (!mockDataEnabled) {
                log.info("Mock data generation is disabled via configuration (mock.data.enabled=false)");
                return;
            }
            
            log.info("=".repeat(80));
            log.info("Starting comprehensive mock data generation...");
            log.info("=".repeat(80));
            
            // Check for existing data
            if (skipIfExists && hasExistingData()) {
                log.info("Skipping mock data generation - data already exists");
                return;
            }
            
            // Track entity counts for summary
            Map<String, Integer> entityCounts = new HashMap<>();
            
            // Create entities in correct dependency order
            log.info("\n--- Phase 1: Academic Structure ---");
            List<AcademicYear> academicYears = createAcademicYears();
            entityCounts.put("Academic Years", academicYears.size());
            validateEntityCreation("Academic Years", MockDataConstants.ACADEMIC_YEARS_COUNT, academicYears.size());
            
            List<Semester> semesters = createSemesters(academicYears);
            entityCounts.put("Semesters", semesters.size());
            validateEntityCreation("Semesters", MockDataConstants.ACADEMIC_YEARS_COUNT * 3, semesters.size());
            
            List<Department> departments = createDepartments();
            entityCounts.put("Departments", departments.size());
            validateEntityCreation("Departments", MockDataConstants.DEPARTMENT_NAMES.size(), departments.size());
            
            List<Course> courses = createCourses(departments);
            entityCounts.put("Courses", courses.size());
            validateEntityCreation("Courses", departments.size() * MockDataConstants.COURSES_PER_DEPARTMENT, courses.size());
            
            // Create users
            log.info("\n--- Phase 2: User Management ---");
            User deanUser = createDeanUser();
            entityCounts.put("Dean User", deanUser != null ? 1 : 0);
            
            List<User> hodUsers = createHODUsers(departments);
            entityCounts.put("HOD Users", hodUsers.size());
            validateEntityCreation("HOD Users", departments.size(), hodUsers.size());
            
            List<User> professorUsers = createProfessorUsers(departments);
            entityCounts.put("Professor Users", professorUsers.size());
            validateEntityCreation("Professor Users", departments.size() * MockDataConstants.PROFESSORS_PER_DEPARTMENT, professorUsers.size());
            
            // Create course system
            log.info("\n--- Phase 3: Course Assignments ---");
            List<CourseAssignment> courseAssignments = createCourseAssignments(semesters, courses, professorUsers);
            entityCounts.put("Course Assignments", courseAssignments.size());
            validateEntityCreation("Course Assignments", 30, courseAssignments.size());
            
            List<RequiredDocumentType> requiredDocumentTypes = createRequiredDocumentTypes(courses, semesters);
            entityCounts.put("Required Document Types", requiredDocumentTypes.size());
            validateEntityCreation("Required Document Types", 50, requiredDocumentTypes.size());
            
            // Create document system
            log.info("\n--- Phase 4: Document Submissions ---");
            List<DocumentSubmission> documentSubmissions = createDocumentSubmissions(courseAssignments, requiredDocumentTypes);
            entityCounts.put("Document Submissions", documentSubmissions.size());
            validateEntityCreation("Document Submissions", 50, documentSubmissions.size());
            
            List<UploadedFile> uploadedFiles = createUploadedFiles(documentSubmissions);
            entityCounts.put("Uploaded Files", uploadedFiles.size());
            validateEntityCreation("Uploaded Files", 50, uploadedFiles.size());
            
            // Create notifications
            log.info("\n--- Phase 5: Notifications ---");
            List<Notification> notifications = createNotifications(professorUsers, courseAssignments, documentSubmissions);
            entityCounts.put("Notifications", notifications.size());
            validateEntityCreation("Notifications", 30, notifications.size());
            
            // Validate relationships
            log.info("\n--- Phase 6: Relationship Validation ---");
            validateEntityRelationships(courseAssignments, documentSubmissions);
            
            // Create legacy data for backward compatibility
            log.info("\n--- Phase 7: Legacy System Data ---");
            initializeLegacyData();
            
            // Log creation summary
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            logCreationSummary(entityCounts, executionTime);
            
            log.info("=".repeat(80));
            log.info("Mock data generation completed successfully!");
            log.info("=".repeat(80));
            
        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("ERROR: Mock data generation failed!", e);
            log.error("=".repeat(80));
            log.error("Partial data may have been created. Clear database and restart to regenerate.");
            throw e;
        }
    }
    
    /**
     * Checks if existing data is present in the database.
     * Checks counts of academic years, departments, and users to determine if mock data already exists.
     * Logs existing data counts if found.
     * 
     * @return true if any data exists, false otherwise
     */
    private boolean hasExistingData() {
        long academicYearCount = academicYearRepository.count();
        long departmentCount = departmentRepository.count();
        long userCount = userRepository.count();
        
        if (academicYearCount > 0 || departmentCount > 0 || userCount > 0) {
            log.info("Existing data detected. Skipping comprehensive mock data generation.");
            log.info("Academic Years: {}, Departments: {}, Users: {}", 
                     academicYearCount, departmentCount, userCount);
            return true;
        }
        
        log.info("No existing data found. Proceeding with mock data generation.");
        return false;
    }
    
    /**
     * Creates 3 academic years with sequential year codes.
     * Sets the current year (2024-2025) as active, others as inactive.
     * Uses batch save for performance.
     * 
     * @return List of created academic years
     */
    private List<AcademicYear> createAcademicYears() {
        log.info("Creating academic years...");
        
        List<AcademicYear> academicYears = new ArrayList<>();
        int currentYearStart = MockDataConstants.CURRENT_ACADEMIC_YEAR_START;
        
        for (int i = 0; i < MockDataConstants.ACADEMIC_YEARS_COUNT; i++) {
            int startYear = currentYearStart - 1 + i; // 2023, 2024, 2025
            int endYear = startYear + 1; // 2024, 2025, 2026
            String yearCode = startYear + "-" + endYear;
            
            // Check if academic year already exists
            if (academicYearRepository.findByYearCode(yearCode).isPresent()) {
                log.info("Academic year {} already exists, skipping", yearCode);
                academicYears.add(academicYearRepository.findByYearCode(yearCode).get());
                continue;
            }
            
            AcademicYear academicYear = new AcademicYear();
            academicYear.setYearCode(yearCode);
            academicYear.setStartYear(startYear);
            academicYear.setEndYear(endYear);
            // Set 2024-2025 as active (current year)
            academicYear.setIsActive(startYear == currentYearStart);
            
            academicYears.add(academicYear);
        }
        
        // Batch save all academic years
        List<AcademicYear> savedYears = academicYearRepository.saveAll(academicYears);
        log.info("Created {} academic years", savedYears.size());
        
        return savedYears;
    }
    
    /**
     * Creates 9 semesters (3 per academic year: FIRST, SECOND, SUMMER).
     * Calculates start and end dates using DateCalculator.
     * Sets the current semester as active.
     * Links semesters to academic years.
     * 
     * @param academicYears List of academic years to create semesters for
     * @return List of created semesters
     */
    private List<Semester> createSemesters(List<AcademicYear> academicYears) {
        log.info("Creating semesters...");
        
        List<Semester> semesters = new ArrayList<>();
        int currentYearStart = MockDataConstants.CURRENT_ACADEMIC_YEAR_START;
        SemesterType currentSemesterType = DateCalculator.getCurrentSemesterType(java.time.LocalDate.now());
        
        for (AcademicYear academicYear : academicYears) {
            // Create 3 semesters per academic year
            for (SemesterType semesterType : SemesterType.values()) {
                // Check if semester already exists
                if (semesterRepository.findByAcademicYearIdAndType(academicYear.getId(), semesterType).isPresent()) {
                    log.info("Semester {} for year {} already exists, skipping", 
                            semesterType, academicYear.getYearCode());
                    semesters.add(semesterRepository.findByAcademicYearIdAndType(
                            academicYear.getId(), semesterType).get());
                    continue;
                }
                
                Semester semester = new Semester();
                semester.setAcademicYear(academicYear);
                semester.setType(semesterType);
                
                // Calculate start and end dates using DateCalculator
                semester.setStartDate(DateCalculator.calculateSemesterStartDate(
                        academicYear.getStartYear(), semesterType));
                semester.setEndDate(DateCalculator.calculateSemesterEndDate(
                        academicYear.getStartYear(), semesterType));
                
                // Set current semester as active (current year + current semester type)
                boolean isCurrentSemester = academicYear.getStartYear() == currentYearStart 
                        && semesterType == currentSemesterType;
                semester.setIsActive(isCurrentSemester);
                
                semesters.add(semester);
            }
        }
        
        // Batch save all semesters
        List<Semester> savedSemesters = semesterRepository.saveAll(semesters);
        log.info("Created {} semesters", savedSemesters.size());
        
        return savedSemesters;
    }
    
    /**
     * Creates 5 departments with descriptive text.
     * Checks for existing departments before creation.
     * Departments: Computer Science, Mathematics, Physics, Engineering, Business Administration
     * 
     * @return List of created departments
     */
    private List<Department> createDepartments() {
        log.info("Creating departments...");
        
        List<Department> departments = new ArrayList<>();
        
        // Iterate through department codes and create departments
        for (String deptCode : MockDataConstants.DEPARTMENT_NAMES.keySet()) {
            String deptName = MockDataConstants.DEPARTMENT_NAMES.get(deptCode);
            String deptDescription = MockDataConstants.DEPARTMENT_DESCRIPTIONS.get(deptCode);
            
            // Check if department already exists
            if (departmentRepository.findByName(deptName).isPresent()) {
                log.info("Department {} already exists, skipping", deptName);
                departments.add(departmentRepository.findByName(deptName).get());
                continue;
            }
            
            Department department = new Department();
            department.setName(deptName);
            department.setDescription(deptDescription);
            
            departments.add(department);
        }
        
        // Batch save all departments
        List<Department> savedDepartments = departmentRepository.saveAll(departments);
        log.info("Created {} departments", savedDepartments.size());
        
        return savedDepartments;
    }
    
    /**
     * Creates 15 courses (3 per department) with realistic course codes.
     * Sets course names, levels (Undergraduate/Graduate), and descriptions.
     * Links courses to departments and marks all as active.
     * 
     * @param departments List of departments to create courses for
     * @return List of created courses
     */
    private List<Course> createCourses(List<Department> departments) {
        log.info("Creating courses...");
        
        List<Course> courses = new ArrayList<>();
        
        for (Department department : departments) {
            // Find the department code from the department name
            String deptCode = MockDataConstants.DEPARTMENT_NAMES.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(department.getName()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            
            if (deptCode == null) {
                log.warn("Department code not found for department: {}", department.getName());
                continue;
            }
            
            // Get course information for this department
            List<MockDataConstants.CourseInfo> courseInfos = MockDataConstants.DEPARTMENT_COURSES.get(deptCode);
            
            if (courseInfos == null) {
                log.warn("No courses defined for department: {}", department.getName());
                continue;
            }
            
            for (MockDataConstants.CourseInfo courseInfo : courseInfos) {
                // Check if course already exists
                if (courseRepository.findByCourseCode(courseInfo.code).isPresent()) {
                    log.info("Course {} already exists, skipping", courseInfo.code);
                    courses.add(courseRepository.findByCourseCode(courseInfo.code).get());
                    continue;
                }
                
                Course course = new Course();
                course.setCourseCode(courseInfo.code);
                course.setCourseName(courseInfo.name);
                course.setDepartment(department);
                course.setLevel(courseInfo.level);
                course.setDescription(courseInfo.description);
                course.setIsActive(true);
                
                courses.add(course);
            }
        }
        
        // Batch save all courses
        List<Course> savedCourses = courseRepository.saveAll(courses);
        log.info("Created {} courses", savedCourses.size());
        
        return savedCourses;
    }
    
    /**
     * Creates a dean user with university-wide access.
     * Email: dean@alquds.edu
     * Password: password123 (hashed)
     * Role: ROLE_DEANSHIP
     * No department association (can access all departments)
     * 
     * @return Created dean user or existing dean user if already exists
     */
    private User createDeanUser() {
        log.info("Creating dean user...");
        
        String email = "dean@alquds.edu";
        
        // Check if dean already exists
        Optional<User> existingDean = userRepository.findByEmail(email);
        if (existingDean.isPresent()) {
            log.info("Dean user {} already exists, skipping", email);
            return existingDean.get();
        }
        
        User deanUser = new User();
        deanUser.setEmail(email);
        deanUser.setPassword(passwordEncoder.encode(MockDataConstants.DEFAULT_PASSWORD));
        deanUser.setFirstName("Dean");
        deanUser.setLastName("User");
        deanUser.setRole(Role.ROLE_DEANSHIP);
        deanUser.setDepartment(null); // Dean has no specific department
        deanUser.setProfessorId(null);
        deanUser.setIsActive(true);
        
        User savedDean = userRepository.save(deanUser);
        log.info("Created dean user: {}", email);
        
        return savedDean;
    }
    
    /**
     * Creates 5 HOD users (one per department).
     * Uses NameGenerator for realistic names.
     * Generates email addresses following pattern: hod.{dept}@alquds.edu
     * Hashes password using BCrypt.
     * Sets role to ROLE_HOD.
     * 
     * @param departments List of departments to create HODs for
     * @return List of created HOD users
     */
    private List<User> createHODUsers(List<Department> departments) {
        log.info("Creating HOD users...");
        
        List<User> hodUsers = new ArrayList<>();
        
        for (Department department : departments) {
            // Find the department code from the department name
            String deptCode = MockDataConstants.DEPARTMENT_NAMES.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(department.getName()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            
            if (deptCode == null) {
                log.warn("Department code not found for department: {}", department.getName());
                continue;
            }
            
            // Generate email address: hod.{dept}@alquds.edu
            String email = NameGenerator.generateDepartmentEmail(deptCode);
            
            // Check if HOD already exists
            if (userRepository.findByEmail(email).isPresent()) {
                log.info("HOD user {} already exists, skipping", email);
                hodUsers.add(userRepository.findByEmail(email).get());
                continue;
            }
            
            // Generate realistic name
            String fullName = NameGenerator.generateFullName();
            String[] nameParts = fullName.split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "";
            
            User hodUser = new User();
            hodUser.setEmail(email);
            hodUser.setPassword(passwordEncoder.encode(MockDataConstants.DEFAULT_PASSWORD));
            hodUser.setFirstName(firstName);
            hodUser.setLastName(lastName);
            hodUser.setRole(Role.ROLE_HOD);
            hodUser.setDepartment(department);
            hodUser.setProfessorId(null); // HODs don't have professor IDs
            hodUser.setIsActive(true);
            
            hodUsers.add(hodUser);
        }
        
        // Batch save all HOD users
        List<User> savedHODs = userRepository.saveAll(hodUsers);
        log.info("Created {} HOD users", savedHODs.size());
        
        return savedHODs;
    }
    
    /**
     * Creates 25 professor users (5 per department).
     * Uses NameGenerator for realistic Arabic/English names.
     * Generates email addresses following pattern: prof.{firstName}.{lastName}@alquds.edu
     * Generates professor IDs following pattern: P{deptCode}{number}
     * Sets 80% as active, 20% as inactive for testing.
     * Hashes passwords using BCrypt.
     * Uses batch save for performance.
     * 
     * @param departments List of departments to create professors for
     * @return List of created professor users
     */
    private List<User> createProfessorUsers(List<Department> departments) {
        log.info("Creating professor users...");
        
        List<User> professorUsers = new ArrayList<>();
        int professorSequence = 1;
        
        for (Department department : departments) {
            // Find the department code from the department name
            String deptCode = MockDataConstants.DEPARTMENT_NAMES.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(department.getName()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            
            if (deptCode == null) {
                log.warn("Department code not found for department: {}", department.getName());
                continue;
            }
            
            // Create 5 professors per department
            for (int i = 1; i <= MockDataConstants.PROFESSORS_PER_DEPARTMENT; i++) {
                // Generate realistic name
                String fullName = NameGenerator.generateFullName();
                String[] nameParts = fullName.split(" ", 2);
                String firstName = nameParts[0];
                String lastName = nameParts.length > 1 ? nameParts[1] : "";
                
                // Generate email address: prof.{firstName}.{lastName}@alquds.edu
                String email = NameGenerator.generateEmailWithPrefix("prof", firstName, lastName);
                
                // Check if professor already exists
                if (userRepository.findByEmail(email).isPresent()) {
                    log.info("Professor user {} already exists, skipping", email);
                    professorUsers.add(userRepository.findByEmail(email).get());
                    professorSequence++;
                    continue;
                }
                
                // Generate professor ID: P{deptCode}{number}
                String professorId = NameGenerator.generateProfessorId(deptCode, i);
                
                // Set 80% as active, 20% as inactive
                boolean isActive = (professorSequence % 5) != 0; // Every 5th professor is inactive
                
                User professorUser = new User();
                professorUser.setEmail(email);
                professorUser.setPassword(passwordEncoder.encode(MockDataConstants.DEFAULT_PASSWORD));
                professorUser.setFirstName(firstName);
                professorUser.setLastName(lastName);
                professorUser.setRole(Role.ROLE_PROFESSOR);
                professorUser.setDepartment(department);
                professorUser.setProfessorId(professorId);
                professorUser.setIsActive(isActive);
                
                professorUsers.add(professorUser);
                professorSequence++;
            }
        }
        
        // Batch save all professor users
        List<User> savedProfessors = userRepository.saveAll(professorUsers);
        log.info("Created {} professor users", savedProfessors.size());
        
        return savedProfessors;
    }
    
    /**
     * Creates 60+ course assignments across all semesters.
     * Assigns each course to professors across all semesters.
     * Ensures professors only get courses from their department.
     * Distributes assignments evenly (2-4 courses per professor per semester).
     * Ensures unique combinations of (semester, course, professor).
     * Marks all assignments as active.
     * 
     * @param semesters List of semesters
     * @param courses List of courses
     * @param professors List of professor users
     * @return List of created course assignments
     */
    private List<CourseAssignment> createCourseAssignments(
            List<Semester> semesters, 
            List<Course> courses, 
            List<User> professors) {
        log.info("Creating course assignments...");
        
        List<CourseAssignment> courseAssignments = new ArrayList<>();
        
        // Filter only active professors for assignments
        List<User> activeProfessors = professors.stream()
                .filter(User::getIsActive)
                .toList();
        
        // For each semester, assign courses to professors
        for (Semester semester : semesters) {
            // Group professors by department
            Map<Long, List<User>> professorsByDepartment = activeProfessors.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            prof -> prof.getDepartment().getId()));
            
            // For each course, assign it to professors from the same department
            for (Course course : courses) {
                Long courseDeptId = course.getDepartment().getId();
                List<User> deptProfessors = professorsByDepartment.get(courseDeptId);
                
                if (deptProfessors == null || deptProfessors.isEmpty()) {
                    log.warn("No active professors found for department: {}", 
                            course.getDepartment().getName());
                    continue;
                }
                
                // Assign course to 1-2 professors per semester (to create multiple sections)
                int professorsToAssign = Math.min(2, deptProfessors.size());
                
                for (int i = 0; i < professorsToAssign; i++) {
                    // Use modulo to cycle through professors evenly
                    User professor = deptProfessors.get(i % deptProfessors.size());
                    
                    // Check if this assignment already exists
                    if (courseAssignmentRepository.findBySemesterIdAndCourseIdAndProfessorId(
                            semester.getId(), course.getId(), professor.getId()).isPresent()) {
                        log.info("Course assignment already exists: {} - {} - {}", 
                                semester.getType(), course.getCourseCode(), professor.getEmail());
                        continue;
                    }
                    
                    CourseAssignment assignment = new CourseAssignment();
                    assignment.setSemester(semester);
                    assignment.setCourse(course);
                    assignment.setProfessor(professor);
                    assignment.setIsActive(true);
                    
                    courseAssignments.add(assignment);
                }
            }
        }
        
        // Batch save all course assignments
        List<CourseAssignment> savedAssignments = courseAssignmentRepository.saveAll(courseAssignments);
        log.info("Created {} course assignments", savedAssignments.size());
        
        // Log distribution statistics
        Map<String, Long> assignmentsByProfessor = savedAssignments.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ca -> ca.getProfessor().getEmail(),
                        java.util.stream.Collectors.counting()));
        
        log.info("Course assignments per professor: min={}, max={}, avg={}", 
                assignmentsByProfessor.values().stream().min(Long::compareTo).orElse(0L),
                assignmentsByProfessor.values().stream().max(Long::compareTo).orElse(0L),
                assignmentsByProfessor.values().stream().mapToLong(Long::longValue).average().orElse(0.0));
        
        return savedAssignments;
    }
    
    /**
     * Initializes legacy system data for backward compatibility.
     * Creates sample document requests and submitted documents using the deprecated entities.
     */
    private void initializeLegacyData() {
        log.info("Initializing legacy system data for backward compatibility...");
        
        // Get existing departments and users
        Optional<Department> csDepartmentOpt = departmentRepository.findByName("Computer Science");
        Optional<Department> mathDepartmentOpt = departmentRepository.findByName("Mathematics");
        Optional<Department> physicsDepartmentOpt = departmentRepository.findByName("Physics");
        
        if (csDepartmentOpt.isEmpty() || mathDepartmentOpt.isEmpty() || physicsDepartmentOpt.isEmpty()) {
            log.warn("Required departments not found for legacy data creation, skipping");
            return;
        }
        
        Department csDepartment = csDepartmentOpt.get();
        Department mathDepartment = mathDepartmentOpt.get();
        Department physicsDepartment = physicsDepartmentOpt.get();
        
        // Get HOD users
        Optional<User> hodCsOpt = userRepository.findByEmail("hod.cs@alquds.edu");
        if (hodCsOpt.isEmpty()) {
            log.warn("HOD CS user not found for legacy data creation, skipping");
            return;
        }
        User hodCs = hodCsOpt.get();
        
        // Get professor users
        List<User> csProfessors = userRepository.findByDepartmentIdAndRole(csDepartment.getId(), Role.ROLE_PROFESSOR);
        List<User> mathProfessors = userRepository.findByDepartmentIdAndRole(mathDepartment.getId(), Role.ROLE_PROFESSOR);
        List<User> physicsProfessors = userRepository.findByDepartmentIdAndRole(physicsDepartment.getId(), Role.ROLE_PROFESSOR);
        
        if (csProfessors.isEmpty() || mathProfessors.isEmpty() || physicsProfessors.isEmpty()) {
            log.warn("Required professors not found for legacy data creation, skipping");
            return;
        }
        
        User profOmar = csProfessors.get(0);
        User profLayla = csProfessors.size() > 1 ? csProfessors.get(1) : csProfessors.get(0);
        User profHassan = mathProfessors.get(0);
        User profNour = physicsProfessors.get(0);
        
        // Create sample document requests
        createSampleDocumentRequests(hodCs, profOmar, profLayla, profHassan, profNour);
        
        // Create sample notifications for legacy system
        createSampleNotifications(profOmar, profLayla, profHassan);
        
        log.info("Legacy system data initialization completed");
    }
    
    @SuppressWarnings("unused")
    private Department createDepartmentIfNotExists(String name, String description) {
        return departmentRepository.findByName(name)
                .orElseGet(() -> {
                    Department department = new Department();
                    department.setName(name);
                    department.setDescription(description);
                    Department saved = departmentRepository.save(department);
                    log.info("Created department: {}", name);
                    return saved;
                });
    }
    
    @SuppressWarnings("unused")
    private User createUserIfNotExists(String email, String password, String firstName, 
                                     String lastName, Role role, Department department) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail(email);
                    user.setPassword(passwordEncoder.encode(password));
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    user.setRole(role);
                    user.setDepartment(department);
                    user.setIsActive(true);
                    User saved = userRepository.save(user);
                    log.info("Created user: {} ({}) in department: {}", 
                            email, role, department.getName());
                    return saved;
                });
    }
    
    private void createSampleDocumentRequests(User hodCs, User profOmar, User profLayla, User profHassan, User profNour) {
        log.info("Creating sample document requests...");
        
        // Computer Science requests
        createDocumentRequest(
            "Advanced Algorithms", 
            "Research Paper", 
            Arrays.asList("pdf", "doc", "docx"), 
            LocalDateTime.now().plusDays(7),
            profOmar,
            hodCs,
            "Submit your final research paper on advanced algorithms",
            3,
            25
        );
        
        createDocumentRequest(
            "Database Systems", 
            "Project Documentation", 
            Arrays.asList("pdf", "zip"), 
            LocalDateTime.now().plusDays(14),
            profLayla,
            hodCs,
            "Complete project documentation with ER diagrams and schemas",
            5,
            50
        );
        
        // Mathematics requests  
        createDocumentRequest(
            "Linear Algebra", 
            "Assignment Solutions", 
            Arrays.asList("pdf", "docx"), 
            LocalDateTime.now().plusDays(5),
            profHassan,
            hodCs,
            "Submit solutions for chapters 1-5",
            2,
            15
        );
        
        // Physics requests
        createDocumentRequest(
            "Quantum Mechanics", 
            "Lab Report", 
            Arrays.asList("pdf", "xlsx", "csv"), 
            LocalDateTime.now().plusDays(10),
            profNour,
            hodCs,
            "Lab report with experimental data and analysis",
            4,
            30
        );
        
        // Create some submitted documents
        createSampleSubmittedDocuments();
    }
    
    private void createDocumentRequest(String courseName, String documentType, List<String> extensions, 
                                     LocalDateTime deadline, User professor, User createdBy, 
                                     String description, Integer maxFileCount, Integer maxSizeMb) {
        DocumentRequest request = new DocumentRequest();
        request.setCourseName(courseName);
        request.setDocumentType(documentType);
        request.setRequiredFileExtensions(extensions);
        request.setDeadline(deadline);
        request.setProfessor(professor);
        request.setCreatedBy(createdBy);
        request.setDescription(description);
        request.setMaxFileCount(maxFileCount);
        request.setMaxTotalSizeMb(maxSizeMb);
        
        documentRequestRepository.save(request);
        log.info("Created document request: {} for professor: {}", courseName, professor.getEmail());
    }
    
    private void createSampleSubmittedDocuments() {
        log.info("Creating sample submitted documents...");
        
        List<DocumentRequest> requests = documentRequestRepository.findAll();
        if (requests.size() >= 2) {
            // Get all request IDs and check existing submissions in bulk
            List<Long> requestIds = requests.stream()
                    .map(DocumentRequest::getId)
                    .toList();
            
            List<SubmittedDocument> existingSubmissions = submittedDocumentRepository.findByDocumentRequestIds(requestIds);
            List<Long> existingRequestIds = existingSubmissions.stream()
                    .map(sd -> sd.getDocumentRequest().getId())
                    .toList();
            
            // Create a submitted document for the first request that doesn't already have one
            DocumentRequest request1 = requests.stream()
                    .filter(req -> !existingRequestIds.contains(req.getId()))
                    .findFirst()
                    .orElse(null);
            
            if (request1 != null) {
                SubmittedDocument submittedDoc1 = new SubmittedDocument();
                submittedDoc1.setDocumentRequest(request1);
                submittedDoc1.setProfessor(request1.getProfessor());
                submittedDoc1.setOriginalFilename("algorithms_research.pdf");
                submittedDoc1.setFileUrl("submitted/algorithms_research_" + System.currentTimeMillis() + ".pdf");
                submittedDoc1.setFileSize(2048576L); // 2MB
                submittedDoc1.setFileType("application/pdf");
                submittedDoc1.setFileCount(1);
                submittedDoc1.setTotalFileSize(2048576L);
                submittedDoc1.setNotes("Final research paper submission");
                submittedDoc1.setSubmittedAt(LocalDateTime.now().minusDays(2));
                submittedDoc1.setIsLateSubmission(false);
                
                submittedDocumentRepository.save(submittedDoc1);
                log.info("Created submitted document for request: {}", request1.getCourseName());
            }
            
            // Create a late submission for the second request that doesn't already have one
            DocumentRequest request2 = requests.stream()
                    .filter(req -> !existingRequestIds.contains(req.getId()))
                    .skip(1)
                    .findFirst()
                    .orElse(null);
            
            if (request2 != null) {
                SubmittedDocument submittedDoc2 = new SubmittedDocument();
                submittedDoc2.setDocumentRequest(request2);
                submittedDoc2.setProfessor(request2.getProfessor());
                submittedDoc2.setOriginalFilename("database_project.zip");
                submittedDoc2.setFileUrl("submitted/database_project_" + System.currentTimeMillis() + ".zip");
                submittedDoc2.setFileSize(5242880L); // 5MB
                submittedDoc2.setFileType("application/zip");
                submittedDoc2.setFileCount(3);
                submittedDoc2.setTotalFileSize(8388608L); // 8MB total
                submittedDoc2.setNotes("Project files with documentation");
                submittedDoc2.setSubmittedAt(LocalDateTime.now().plusDays(1)); // Late submission
                submittedDoc2.setIsLateSubmission(true);
                
                submittedDocumentRepository.save(submittedDoc2);
                log.info("Created late submitted document for request: {}", request2.getCourseName());
            }
        }
    }
    
    /**
     * Creates 75+ notifications distributed across all professors.
     * Creates 3 notifications per professor on average.
     * Distributes notification types: NEW_REQUEST (30%), REQUEST_REMINDER (20%), 
     * DEADLINE_APPROACHING (25%), DOCUMENT_SUBMITTED (15%), DOCUMENT_OVERDUE (10%)
     * Sets 60% as read, 40% as unread.
     * Links notifications to related course assignments or submissions.
     * Distributes creation timestamps over last 30 days.
     * Generates appropriate titles and messages based on type.
     * 
     * @param professors List of professor users to create notifications for
     * @param courseAssignments List of course assignments to link notifications to
     * @param documentSubmissions List of document submissions to link notifications to
     * @return List of created notifications
     */
    private List<Notification> createNotifications(
            List<User> professors,
            List<CourseAssignment> courseAssignments,
            List<DocumentSubmission> documentSubmissions) {
        log.info("Creating notifications...");
        
        List<Notification> notifications = new ArrayList<>();
        
        // Create a map of course assignments by professor for quick lookup
        Map<Long, List<CourseAssignment>> assignmentsByProfessor = courseAssignments.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ca -> ca.getProfessor().getId()));
        
        // Create a map of document submissions by professor for quick lookup
        Map<Long, List<DocumentSubmission>> submissionsByProfessor = documentSubmissions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ds -> ds.getProfessor().getId()));
        
        int notificationCounter = 0;
        
        // Create notifications for each professor
        for (User professor : professors) {
            List<CourseAssignment> professorAssignments = assignmentsByProfessor.get(professor.getId());
            List<DocumentSubmission> professorSubmissions = submissionsByProfessor.get(professor.getId());
            
            if (professorAssignments == null || professorAssignments.isEmpty()) {
                log.debug("No assignments found for professor: {}, skipping notifications", 
                         professor.getEmail());
                continue;
            }
            
            // Create 3 notifications per professor on average
            int notificationsForProfessor = MockDataConstants.NOTIFICATIONS_PER_PROFESSOR;
            
            for (int i = 0; i < notificationsForProfessor; i++) {
                // Determine notification type based on distribution percentages
                Notification.NotificationType notificationType = determineNotificationType(notificationCounter);
                
                // Determine if this notification is read (60% read, 40% unread)
                boolean isRead = (notificationCounter % 100) < MockDataConstants.NOTIFICATION_READ_PERCENTAGE;
                
                // Calculate creation timestamp distributed over last 30 days
                LocalDateTime createdAt = calculateNotificationTimestamp(notificationCounter);
                
                // Select a related entity based on notification type
                CourseAssignment relatedAssignment = null;
                DocumentSubmission relatedSubmission = null;
                
                if (notificationType == Notification.NotificationType.NEW_REQUEST ||
                    notificationType == Notification.NotificationType.REQUEST_REMINDER ||
                    notificationType == Notification.NotificationType.DEADLINE_APPROACHING) {
                    // These types relate to course assignments
                    relatedAssignment = professorAssignments.get(i % professorAssignments.size());
                } else if (professorSubmissions != null && !professorSubmissions.isEmpty()) {
                    // These types relate to document submissions
                    relatedSubmission = professorSubmissions.get(i % professorSubmissions.size());
                } else {
                    // Fallback to assignment if no submissions available
                    relatedAssignment = professorAssignments.get(i % professorAssignments.size());
                }
                
                // Generate title and message based on notification type
                String title = generateNotificationTitle(notificationType, relatedAssignment, relatedSubmission);
                String message = generateNotificationMessage(notificationType, relatedAssignment, relatedSubmission);
                
                // Create notification entity
                Notification notification = new Notification();
                notification.setUser(professor);
                notification.setTitle(title);
                notification.setMessage(message);
                notification.setType(notificationType);
                notification.setIsRead(isRead);
                notification.setCreatedAt(createdAt);
                
                // Set related entity information
                if (relatedAssignment != null) {
                    notification.setRelatedEntityId(relatedAssignment.getId());
                    notification.setRelatedEntityType("CourseAssignment");
                } else if (relatedSubmission != null) {
                    notification.setRelatedEntityId(relatedSubmission.getId());
                    notification.setRelatedEntityType("DocumentSubmission");
                }
                
                notifications.add(notification);
                notificationCounter++;
            }
        }
        
        // Batch save all notifications
        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
        log.info("Created {} notifications", savedNotifications.size());
        
        // Log distribution statistics
        Map<Notification.NotificationType, Long> notificationsByType = savedNotifications.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Notification::getType,
                        java.util.stream.Collectors.counting()));
        
        long readCount = savedNotifications.stream()
                .filter(Notification::getIsRead)
                .count();
        
        long unreadCount = savedNotifications.size() - readCount;
        
        log.info("Notification type distribution: {}", notificationsByType);
        log.info("Read notifications: {} ({}%), Unread: {} ({}%)", 
                readCount, (readCount * 100) / savedNotifications.size(),
                unreadCount, (unreadCount * 100) / savedNotifications.size());
        
        return savedNotifications;
    }
    
    /**
     * Determines the notification type based on distribution percentages.
     * NEW_REQUEST (30%), REQUEST_REMINDER (20%), DEADLINE_APPROACHING (25%),
     * DOCUMENT_SUBMITTED (15%), DOCUMENT_OVERDUE (10%)
     * 
     * @param counter notification counter for distribution
     * @return the notification type
     */
    private Notification.NotificationType determineNotificationType(int counter) {
        int percentage = counter % 100;
        
        if (percentage < MockDataConstants.NOTIFICATION_TYPE_DISTRIBUTION.get("NEW_REQUEST")) {
            return Notification.NotificationType.NEW_REQUEST;
        } else if (percentage < MockDataConstants.NOTIFICATION_TYPE_DISTRIBUTION.get("NEW_REQUEST") +
                                MockDataConstants.NOTIFICATION_TYPE_DISTRIBUTION.get("REQUEST_REMINDER")) {
            return Notification.NotificationType.REQUEST_REMINDER;
        } else if (percentage < MockDataConstants.NOTIFICATION_TYPE_DISTRIBUTION.get("NEW_REQUEST") +
                                MockDataConstants.NOTIFICATION_TYPE_DISTRIBUTION.get("REQUEST_REMINDER") +
                                MockDataConstants.NOTIFICATION_TYPE_DISTRIBUTION.get("DEADLINE_APPROACHING")) {
            return Notification.NotificationType.DEADLINE_APPROACHING;
        } else if (percentage < MockDataConstants.NOTIFICATION_TYPE_DISTRIBUTION.get("NEW_REQUEST") +
                                MockDataConstants.NOTIFICATION_TYPE_DISTRIBUTION.get("REQUEST_REMINDER") +
                                MockDataConstants.NOTIFICATION_TYPE_DISTRIBUTION.get("DEADLINE_APPROACHING") +
                                MockDataConstants.NOTIFICATION_TYPE_DISTRIBUTION.get("DOCUMENT_SUBMITTED")) {
            return Notification.NotificationType.DOCUMENT_SUBMITTED;
        } else {
            return Notification.NotificationType.DOCUMENT_OVERDUE;
        }
    }
    
    /**
     * Calculates notification timestamp distributed over last 30 days.
     * 
     * @param counter notification counter for distribution
     * @return the notification timestamp
     */
    private LocalDateTime calculateNotificationTimestamp(int counter) {
        // Distribute notifications over the last 30 days
        int daysBack = (int) (Math.random() * MockDataConstants.NOTIFICATION_DAYS_BACK);
        int hoursBack = (int) (Math.random() * 24);
        int minutesBack = (int) (Math.random() * 60);
        
        return LocalDateTime.now()
                .minusDays(daysBack)
                .minusHours(hoursBack)
                .minusMinutes(minutesBack);
    }
    
    /**
     * Generates notification title based on type and related entities.
     * 
     * @param type the notification type
     * @param assignment the related course assignment (may be null)
     * @param submission the related document submission (may be null)
     * @return the notification title
     */
    private String generateNotificationTitle(
            Notification.NotificationType type,
            CourseAssignment assignment,
            DocumentSubmission submission) {
        
        switch (type) {
            case NEW_REQUEST:
                if (assignment != null) {
                    return "New Course Assignment: " + assignment.getCourse().getCourseName();
                }
                return "New Course Assignment";
                
            case REQUEST_REMINDER:
                if (assignment != null) {
                    return "Reminder: " + assignment.getCourse().getCourseName();
                }
                return "Document Submission Reminder";
                
            case DEADLINE_APPROACHING:
                if (assignment != null) {
                    return "Deadline Approaching: " + assignment.getCourse().getCourseName();
                }
                return "Deadline Approaching";
                
            case DOCUMENT_SUBMITTED:
                if (submission != null) {
                    return "Document Submitted: " + submission.getCourseAssignment().getCourse().getCourseName();
                }
                return "Document Submitted Successfully";
                
            case DOCUMENT_OVERDUE:
                if (submission != null) {
                    return "Overdue: " + submission.getCourseAssignment().getCourse().getCourseName();
                }
                return "Document Submission Overdue";
                
            default:
                return "Notification";
        }
    }
    
    /**
     * Generates notification message based on type and related entities.
     * 
     * @param type the notification type
     * @param assignment the related course assignment (may be null)
     * @param submission the related document submission (may be null)
     * @return the notification message
     */
    private String generateNotificationMessage(
            Notification.NotificationType type,
            CourseAssignment assignment,
            DocumentSubmission submission) {
        
        switch (type) {
            case NEW_REQUEST:
                if (assignment != null) {
                    return String.format("You have been assigned to teach %s (%s) for %s semester. " +
                                       "Please review the course requirements and submit required documents.",
                                       assignment.getCourse().getCourseName(),
                                       assignment.getCourse().getCourseCode(),
                                       assignment.getSemester().getType());
                }
                return "You have been assigned a new course. Please review the requirements.";
                
            case REQUEST_REMINDER:
                if (assignment != null) {
                    return String.format("This is a reminder to submit required documents for %s (%s). " +
                                       "Please ensure all materials are uploaded before the deadline.",
                                       assignment.getCourse().getCourseName(),
                                       assignment.getCourse().getCourseCode());
                }
                return "Please remember to submit your pending documents.";
                
            case DEADLINE_APPROACHING:
                if (assignment != null) {
                    return String.format("The deadline for submitting documents for %s (%s) is approaching. " +
                                       "Please submit your materials as soon as possible to avoid late submission.",
                                       assignment.getCourse().getCourseName(),
                                       assignment.getCourse().getCourseCode());
                }
                return "A document submission deadline is approaching. Please submit your materials soon.";
                
            case DOCUMENT_SUBMITTED:
                if (submission != null) {
                    return String.format("Your %s document for %s (%s) has been successfully submitted. " +
                                       "You can view your submission in the course details.",
                                       submission.getDocumentType(),
                                       submission.getCourseAssignment().getCourse().getCourseName(),
                                       submission.getCourseAssignment().getCourse().getCourseCode());
                }
                return "Your document has been successfully submitted.";
                
            case DOCUMENT_OVERDUE:
                if (submission != null) {
                    return String.format("The %s document for %s (%s) is now overdue. " +
                                       "Please submit it as soon as possible. Late submissions may require approval.",
                                       submission.getDocumentType(),
                                       submission.getCourseAssignment().getCourse().getCourseName(),
                                       submission.getCourseAssignment().getCourse().getCourseCode());
                }
                return "A required document submission is overdue. Please submit it immediately.";
                
            default:
                return "You have a new notification.";
        }
    }
    
    private void createSampleNotifications(User profOmar, User profLayla, User profHassan) {
        log.info("Creating sample notifications...");
        
        // Notification for Omar
        createNotification(
            profOmar,
            "New Document Request Assigned",
            "You have been assigned a new document request: Advanced Algorithms",
            Notification.NotificationType.NEW_REQUEST
        );
        
        createNotification(
            profOmar,
            "Deadline Approaching",
            "Database Systems project deadline is in 3 days",
            Notification.NotificationType.DEADLINE_APPROACHING
        );
        
        // Notification for Layla
        createNotification(
            profLayla,
            "Submission Received",
            "A new submission has been received for Database Systems",
            Notification.NotificationType.DOCUMENT_SUBMITTED
        );
        
        // Notification for Hassan
        createNotification(
            profHassan,
            "Document Request Created",
            "Linear Algebra assignment request has been created",
            Notification.NotificationType.NEW_REQUEST
        );
    }
    
    private void createNotification(User user, String title, String message, Notification.NotificationType type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now().minusHours((long)(Math.random() * 24)));
        
        notificationRepository.save(notification);
        log.info("Created notification for user: {} - {}", user.getEmail(), title);
    }
    
    /**
     * Creates 90+ required document types (6 per course).
     * Creates document types: SYLLABUS, EXAM, ASSIGNMENT, PROJECT_DOCS, LECTURE_NOTES, OTHER
     * Calculates realistic deadlines based on semester dates and document type.
     * Sets allowed file extensions based on document type.
     * Sets max file count to 5 and max size to 50MB.
     * Marks all as required.
     * 
     * @param courses List of courses to create document types for
     * @param semesters List of semesters to link document types to
     * @return List of created required document types
     */
    private List<RequiredDocumentType> createRequiredDocumentTypes(
            List<Course> courses,
            List<Semester> semesters) {
        log.info("Creating required document types...");
        
        List<RequiredDocumentType> requiredDocumentTypes = new ArrayList<>();
        
        // For each course, create 6 document types (one for each DocumentTypeEnum)
        for (Course course : courses) {
            for (DocumentTypeEnum documentType : MockDataConstants.ALL_DOCUMENT_TYPES) {
                // For each semester, create a required document type
                for (Semester semester : semesters) {
                    // Check if this document type already exists for this course and semester
                    List<RequiredDocumentType> existing = requiredDocumentTypeRepository
                            .findByCourseIdAndSemesterId(course.getId(), semester.getId());
                    
                    boolean alreadyExists = existing.stream()
                            .anyMatch(rdt -> rdt.getDocumentType() == documentType);
                    
                    if (alreadyExists) {
                        log.debug("Required document type {} for course {} and semester {} already exists, skipping",
                                documentType, course.getCourseCode(), semester.getType());
                        continue;
                    }
                    
                    RequiredDocumentType requiredDocType = new RequiredDocumentType();
                    requiredDocType.setCourse(course);
                    requiredDocType.setSemester(semester);
                    requiredDocType.setDocumentType(documentType);
                    
                    // Calculate realistic deadline based on semester dates and document type
                    java.time.LocalDate deadline = DateCalculator.calculateDocumentDeadline(
                            semester.getStartDate(),
                            semester.getEndDate(),
                            documentType
                    );
                    requiredDocType.setDeadline(deadline.atStartOfDay());
                    
                    // Set allowed file extensions based on document type
                    List<String> allowedExtensions = MockDataConstants.ALLOWED_FILE_EXTENSIONS.get(documentType);
                    if (allowedExtensions != null) {
                        requiredDocType.setAllowedFileExtensions(new ArrayList<>(allowedExtensions));
                    }
                    
                    // Set max file count and size
                    requiredDocType.setMaxFileCount(MockDataConstants.MAX_FILE_COUNT);
                    requiredDocType.setMaxTotalSizeMb(MockDataConstants.MAX_FILE_SIZE_MB);
                    
                    // Mark as required
                    requiredDocType.setIsRequired(true);
                    
                    requiredDocumentTypes.add(requiredDocType);
                }
            }
        }
        
        // Batch save all required document types
        List<RequiredDocumentType> savedDocTypes = requiredDocumentTypeRepository.saveAll(requiredDocumentTypes);
        log.info("Created {} required document types", savedDocTypes.size());
        
        // Log distribution statistics
        Map<DocumentTypeEnum, Long> docTypesByType = savedDocTypes.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        RequiredDocumentType::getDocumentType,
                        java.util.stream.Collectors.counting()));
        
        log.info("Required document types by type: {}", docTypesByType);
        
        return savedDocTypes;
    }
    
    /**
     * Creates 100+ document submissions for course assignments with varied statuses.
     * Sets status distribution: 70% UPLOADED, 20% NOT_UPLOADED, 10% OVERDUE
     * Calculates submission dates based on status (on-time vs late).
     * Sets isLateSubmission flag for 15% of submissions.
     * Adds optional notes to submissions.
     * Links submissions to course assignments and professors.
     * 
     * @param courseAssignments List of course assignments to create submissions for
     * @param requiredDocumentTypes List of required document types
     * @return List of created document submissions
     */
    private List<DocumentSubmission> createDocumentSubmissions(
            List<CourseAssignment> courseAssignments,
            List<RequiredDocumentType> requiredDocumentTypes) {
        log.info("Creating document submissions...");
        
        List<DocumentSubmission> documentSubmissions = new ArrayList<>();
        
        // Create a map of required document types by course and semester for quick lookup
        Map<String, RequiredDocumentType> requiredDocTypeMap = new HashMap<>();
        for (RequiredDocumentType rdt : requiredDocumentTypes) {
            String key = rdt.getCourse().getId() + "_" + rdt.getSemester().getId() + "_" + rdt.getDocumentType();
            requiredDocTypeMap.put(key, rdt);
        }
        
        int submissionCounter = 0;
        
        // For each course assignment, create submissions for various document types
        for (CourseAssignment assignment : courseAssignments) {
            Long courseId = assignment.getCourse().getId();
            Long semesterId = assignment.getSemester().getId();
            User professor = assignment.getProfessor();
            
            // Create submissions for each document type (not all assignments will have all types)
            // We'll create 2-4 submissions per assignment to reach 100+ total
            int submissionsForThisAssignment = 2 + (int) (Math.random() * 3); // 2-4 submissions
            
            for (int i = 0; i < submissionsForThisAssignment && i < MockDataConstants.ALL_DOCUMENT_TYPES.size(); i++) {
                DocumentTypeEnum documentType = MockDataConstants.ALL_DOCUMENT_TYPES.get(i);
                
                // Check if this submission already exists
                Optional<DocumentSubmission> existing = documentSubmissionRepository
                        .findByCourseAssignmentIdAndDocumentType(assignment.getId(), documentType);
                
                if (existing.isPresent()) {
                    log.debug("Document submission already exists for assignment {} and type {}, skipping",
                            assignment.getId(), documentType);
                    continue;
                }
                
                // Get the required document type for deadline information
                String key = courseId + "_" + semesterId + "_" + documentType;
                RequiredDocumentType requiredDocType = requiredDocTypeMap.get(key);
                
                if (requiredDocType == null) {
                    log.warn("No required document type found for course {}, semester {}, type {}",
                            courseId, semesterId, documentType);
                    continue;
                }
                
                // Determine status based on distribution percentages
                SubmissionStatus status = determineSubmissionStatus(submissionCounter);
                
                // Determine if this is a late submission (15% of all submissions)
                boolean isLate = (submissionCounter % 100) < MockDataConstants.LATE_SUBMISSION_PERCENTAGE;
                
                DocumentSubmission submission = new DocumentSubmission();
                submission.setCourseAssignment(assignment);
                submission.setDocumentType(documentType);
                submission.setProfessor(professor);
                submission.setStatus(status);
                submission.setIsLateSubmission(isLate);
                
                // Calculate submission date based on status and deadline
                LocalDateTime submittedAt = calculateSubmissionDateTime(
                        requiredDocType.getDeadline().toLocalDate(),
                        status,
                        isLate
                );
                submission.setSubmittedAt(submittedAt);
                
                // Add optional notes (50% of submissions have notes)
                if (Math.random() < 0.5) {
                    submission.setNotes(generateSubmissionNotes(documentType, status));
                }
                
                // Initialize file count and size (will be updated when files are created)
                submission.setFileCount(0);
                submission.setTotalFileSize(0L);
                
                documentSubmissions.add(submission);
                submissionCounter++;
            }
        }
        
        // Batch save all document submissions
        List<DocumentSubmission> savedSubmissions = documentSubmissionRepository.saveAll(documentSubmissions);
        log.info("Created {} document submissions", savedSubmissions.size());
        
        // Log distribution statistics
        Map<SubmissionStatus, Long> submissionsByStatus = savedSubmissions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        DocumentSubmission::getStatus,
                        java.util.stream.Collectors.counting()));
        
        long lateSubmissionsCount = savedSubmissions.stream()
                .filter(DocumentSubmission::getIsLateSubmission)
                .count();
        
        log.info("Submission status distribution: {}", submissionsByStatus);
        log.info("Late submissions: {} ({}%)", lateSubmissionsCount, 
                (lateSubmissionsCount * 100) / savedSubmissions.size());
        
        return savedSubmissions;
    }
    
    /**
     * Determines the submission status based on distribution percentages.
     * 70% UPLOADED, 20% NOT_UPLOADED, 10% OVERDUE
     * 
     * @param counter submission counter for distribution
     * @return the submission status
     */
    private SubmissionStatus determineSubmissionStatus(int counter) {
        int percentage = counter % 100;
        
        if (percentage < MockDataConstants.UPLOADED_PERCENTAGE) {
            return SubmissionStatus.UPLOADED;
        } else if (percentage < MockDataConstants.UPLOADED_PERCENTAGE + MockDataConstants.NOT_UPLOADED_PERCENTAGE) {
            return SubmissionStatus.NOT_UPLOADED;
        } else {
            return SubmissionStatus.OVERDUE;
        }
    }
    
    /**
     * Calculates the submission date/time based on deadline, status, and whether it's late.
     * 
     * @param deadline the deadline date
     * @param status the submission status
     * @param isLate whether this is a late submission
     * @return the submission date/time
     */
    private LocalDateTime calculateSubmissionDateTime(
            java.time.LocalDate deadline,
            SubmissionStatus status,
            boolean isLate) {
        
        if (status == SubmissionStatus.NOT_UPLOADED) {
            // Not uploaded yet, set to current time as placeholder
            // (In real system, this would be set when actually uploaded)
            return LocalDateTime.now();
        }
        
        java.time.LocalDate submissionDate;
        
        if (status == SubmissionStatus.OVERDUE || isLate) {
            // Late submission: 1-7 days after deadline
            submissionDate = DateCalculator.calculateSubmissionDate(deadline, true);
        } else {
            // On-time submission: 0-3 days before deadline
            submissionDate = DateCalculator.calculateSubmissionDate(deadline, false);
        }
        
        // Add random time component (between 8 AM and 6 PM)
        int hour = 8 + (int) (Math.random() * 10);
        int minute = (int) (Math.random() * 60);
        
        return submissionDate.atTime(hour, minute);
    }
    
    /**
     * Generates realistic submission notes based on document type and status.
     * 
     * @param documentType the type of document
     * @param status the submission status
     * @return submission notes
     */
    private String generateSubmissionNotes(DocumentTypeEnum documentType, SubmissionStatus status) {
        String[] uploadedNotes = {
            "Submitted as per requirements",
            "All materials included",
            "Please review and provide feedback",
            "Updated version with corrections",
            "Final submission"
        };
        
        String[] overdueNotes = {
            "Apologies for the delay, technical issues",
            "Late submission due to illness",
            "Extended deadline approved by HOD",
            "Resubmission after corrections"
        };
        
        if (status == SubmissionStatus.OVERDUE) {
            return overdueNotes[(int) (Math.random() * overdueNotes.length)];
        } else {
            return uploadedNotes[(int) (Math.random() * uploadedNotes.length)];
        }
    }
    
    /**
     * Creates 150+ uploaded file records for document submissions.
     * Creates 1-3 files per submission (only for UPLOADED/OVERDUE status).
     * Generates realistic file names based on document type and course.
     * Generates file URLs following pattern: uploads/{submissionId}_{uuid}_{timestamp}.{ext}
     * Sets random file sizes between 100KB and 10MB.
     * Sets MIME types based on file extensions.
     * Sets sequential file order within each submission.
     * Updates submission fileCount and totalFileSize.
     * 
     * @param documentSubmissions List of document submissions to create files for
     * @return List of created uploaded files
     */
    private List<UploadedFile> createUploadedFiles(List<DocumentSubmission> documentSubmissions) {
        log.info("Creating uploaded files...");
        
        List<UploadedFile> uploadedFiles = new ArrayList<>();
        
        // Only create files for submissions with UPLOADED or OVERDUE status
        List<DocumentSubmission> submissionsWithFiles = documentSubmissions.stream()
                .filter(submission -> submission.getStatus() == SubmissionStatus.UPLOADED 
                                   || submission.getStatus() == SubmissionStatus.OVERDUE)
                .toList();
        
        for (DocumentSubmission submission : submissionsWithFiles) {
            // Determine number of files for this submission (1-3 files)
            int fileCount = MockDataConstants.MIN_FILES_PER_SUBMISSION + 
                           (int) (Math.random() * (MockDataConstants.MAX_FILES_PER_SUBMISSION - 
                                                   MockDataConstants.MIN_FILES_PER_SUBMISSION + 1));
            
            // Get allowed extensions for this document type
            List<String> allowedExtensions = MockDataConstants.ALLOWED_FILE_EXTENSIONS.get(submission.getDocumentType());
            if (allowedExtensions == null || allowedExtensions.isEmpty()) {
                log.warn("No allowed extensions found for document type: {}", submission.getDocumentType());
                continue;
            }
            
            long totalFileSize = 0L;
            
            // Create files for this submission
            for (int fileOrder = 1; fileOrder <= fileCount; fileOrder++) {
                // Select a random file extension from allowed extensions
                String extension = allowedExtensions.get((int) (Math.random() * allowedExtensions.size()));
                
                // Generate realistic file name based on document type and course
                String courseCode = submission.getCourseAssignment().getCourse().getCourseCode();
                String fileName = generateFileName(submission.getDocumentType(), courseCode, fileOrder, extension);
                
                // Generate file URL following pattern: uploads/{submissionId}_{uuid}_{timestamp}.{ext}
                String uuid = java.util.UUID.randomUUID().toString();
                long timestamp = System.currentTimeMillis();
                String fileUrl = String.format("uploads/%d_%s_%d.%s", 
                                              submission.getId(), uuid, timestamp, extension);
                
                // Generate random file size between 100KB and 10MB
                long fileSize = MockDataConstants.MIN_FILE_SIZE_BYTES + 
                               (long) (Math.random() * (MockDataConstants.MAX_FILE_SIZE_BYTES - 
                                                        MockDataConstants.MIN_FILE_SIZE_BYTES));
                totalFileSize += fileSize;
                
                // Get MIME type based on extension
                String mimeType = MockDataConstants.FILE_MIME_TYPES.get(extension);
                if (mimeType == null) {
                    mimeType = "application/octet-stream"; // Default MIME type
                }
                
                // Create uploaded file entity
                UploadedFile uploadedFile = new UploadedFile();
                uploadedFile.setDocumentSubmission(submission);
                uploadedFile.setFileUrl(fileUrl);
                uploadedFile.setOriginalFilename(fileName);
                uploadedFile.setFileSize(fileSize);
                uploadedFile.setFileType(mimeType);
                uploadedFile.setFileOrder(fileOrder);
                
                // Add optional description (30% of files have descriptions)
                if (Math.random() < 0.3) {
                    uploadedFile.setDescription(generateFileDescription(submission.getDocumentType(), fileOrder));
                }
                
                uploadedFiles.add(uploadedFile);
            }
            
            // Update submission with file count and total size
            submission.setFileCount(fileCount);
            submission.setTotalFileSize(totalFileSize);
        }
        
        // Batch save all uploaded files
        List<UploadedFile> savedFiles = uploadedFileRepository.saveAll(uploadedFiles);
        log.info("Created {} uploaded files", savedFiles.size());
        
        // Update all submissions with file counts
        @SuppressWarnings("null")
        List<DocumentSubmission> updatedSubmissions = documentSubmissionRepository.saveAll(submissionsWithFiles);
        log.info("Updated {} submissions with file counts and sizes", updatedSubmissions.size());
        
        // Log distribution statistics
        Map<DocumentTypeEnum, Long> filesByDocType = savedFiles.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        file -> file.getDocumentSubmission().getDocumentType(),
                        java.util.stream.Collectors.counting()));
        
        long totalSize = savedFiles.stream()
                .mapToLong(UploadedFile::getFileSize)
                .sum();
        
        log.info("Files by document type: {}", filesByDocType);
        log.info("Total file size: {} MB", totalSize / (1024 * 1024));
        log.info("Average files per submission: {}", 
                savedFiles.size() / (double) submissionsWithFiles.size());
        
        return savedFiles;
    }
    
    /**
     * Generates a realistic file name based on document type, course code, and file order.
     * 
     * @param documentType the type of document
     * @param courseCode the course code
     * @param fileOrder the order of the file within the submission
     * @param extension the file extension
     * @return the generated file name
     */
    private String generateFileName(DocumentTypeEnum documentType, String courseCode, 
                                    int fileOrder, String extension) {
        String baseName;
        
        switch (documentType) {
            case SYLLABUS:
                baseName = String.format("%s_Syllabus", courseCode);
                break;
            case EXAM:
                String examType = (fileOrder == 1) ? "Midterm" : "Final";
                baseName = String.format("%s_%s_Exam", courseCode, examType);
                break;
            case ASSIGNMENT:
                baseName = String.format("%s_Assignment_%d", courseCode, fileOrder);
                break;
            case PROJECT_DOCS:
                baseName = String.format("%s_Project_Part_%d", courseCode, fileOrder);
                break;
            case LECTURE_NOTES:
                baseName = String.format("%s_Lecture_%d", courseCode, fileOrder);
                break;
            case OTHER:
                baseName = String.format("%s_Document_%d", courseCode, fileOrder);
                break;
            default:
                baseName = String.format("%s_File_%d", courseCode, fileOrder);
        }
        
        return baseName + "." + extension;
    }
    
    /**
     * Validates that the expected number of entities were created.
     * Logs warnings if actual count is less than expected.
     * 
     * @param entityType the type of entity being validated
     * @param expected the expected count
     * @param actual the actual count created
     */
    private void validateEntityCreation(String entityType, int expected, int actual) {
        if (actual < expected) {
            log.warn("⚠️  Expected at least {} {} but only created {}", expected, entityType, actual);
        } else if (actual == expected) {
            log.info("✓  Successfully created {} {}", actual, entityType);
        } else {
            log.info("✓  Successfully created {} {} (expected at least {})", actual, entityType, expected);
        }
    }
    
    /**
     * Validates relationships between key entities.
     * Checks that course assignments have valid professors from the same department,
     * and that document submissions are properly linked to course assignments.
     * 
     * @param courseAssignments list of course assignments to validate
     * @param documentSubmissions list of document submissions to validate
     */
    private void validateEntityRelationships(
            List<CourseAssignment> courseAssignments,
            List<DocumentSubmission> documentSubmissions) {
        
        log.info("Validating entity relationships...");
        
        int invalidAssignments = 0;
        int invalidSubmissions = 0;
        
        // Validate course assignments
        for (CourseAssignment assignment : courseAssignments) {
            if (assignment.getCourse() == null || assignment.getProfessor() == null || assignment.getSemester() == null) {
                log.warn("Invalid course assignment found: missing required relationships");
                invalidAssignments++;
                continue;
            }
            
            // Check that professor is from the same department as the course
            if (!assignment.getCourse().getDepartment().getId().equals(assignment.getProfessor().getDepartment().getId())) {
                log.warn("Course assignment has professor from different department: {} assigned to {}",
                        assignment.getProfessor().getEmail(), assignment.getCourse().getCourseCode());
                invalidAssignments++;
            }
        }
        
        // Validate document submissions
        for (DocumentSubmission submission : documentSubmissions) {
            if (submission.getCourseAssignment() == null || submission.getProfessor() == null) {
                log.warn("Invalid document submission found: missing required relationships");
                invalidSubmissions++;
                continue;
            }
            
            // Check that submission professor matches course assignment professor
            if (!submission.getProfessor().getId().equals(submission.getCourseAssignment().getProfessor().getId())) {
                log.warn("Document submission has different professor than course assignment");
                invalidSubmissions++;
            }
        }
        
        if (invalidAssignments == 0 && invalidSubmissions == 0) {
            log.info("✓  All entity relationships are valid");
        } else {
            log.warn("⚠️  Found {} invalid course assignments and {} invalid document submissions",
                    invalidAssignments, invalidSubmissions);
        }
    }
    
    /**
     * Logs a comprehensive summary of all created entities.
     * Displays entity counts in a formatted table and total execution time.
     * 
     * @param entityCounts map of entity types to their counts
     * @param executionTimeMs total execution time in milliseconds
     */
    private void logCreationSummary(Map<String, Integer> entityCounts, long executionTimeMs) {
        log.info("\n" + "=".repeat(80));
        log.info("MOCK DATA GENERATION SUMMARY");
        log.info("=".repeat(80));
        
        // Calculate total entities
        int totalEntities = entityCounts.values().stream().mapToInt(Integer::intValue).sum();
        
        // Log entity counts in a formatted table
        log.info("\n{:<40} {:>10}", "Entity Type", "Count");
        log.info("-".repeat(80));
        
        entityCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> log.info("{:<40} {:>10}", entry.getKey(), entry.getValue()));
        
        log.info("-".repeat(80));
        log.info("{:<40} {:>10}", "TOTAL ENTITIES", totalEntities);
        log.info("=".repeat(80));
        
        // Log execution time
        long seconds = executionTimeMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            log.info("Total execution time: {} minutes {} seconds", minutes, seconds);
        } else {
            log.info("Total execution time: {} seconds", seconds);
        }
        
        log.info("=".repeat(80));
        
        // Log account information
        log.info("\n📋 MOCK ACCOUNTS CREATED:");
        log.info("   - {} HOD accounts (default password: {})", 
                entityCounts.getOrDefault("HOD Users", 0), MockDataConstants.DEFAULT_PASSWORD);
        log.info("   - {} Professor accounts (default password: {})", 
                entityCounts.getOrDefault("Professor Users", 0), MockDataConstants.DEFAULT_PASSWORD);
        log.info("\n📄 For detailed account information, see: MOCK_ACCOUNTS.md");
        log.info("🔧 For API testing examples, see: MOCK_DATA_API_TESTING.md");
        log.info("📖 For data structure guide, see: mock_data_guide.md");
        log.info("\n⚠️  SECURITY WARNING: Change all passwords before deploying to production!");
    }
    
    /**
     * Generates a description for an uploaded file.
     * 
     * @param documentType the type of document
     * @param fileOrder the order of the file
     * @return the file description
     */
    private String generateFileDescription(DocumentTypeEnum documentType, int fileOrder) {
        String[] syllabusDescriptions = {
            "Course syllabus with learning objectives",
            "Updated syllabus for current semester",
            "Detailed course outline and schedule"
        };
        
        String[] examDescriptions = {
            "Exam questions and answer key",
            "Comprehensive exam with solutions",
            "Assessment materials and rubric"
        };
        
        String[] assignmentDescriptions = {
            "Assignment instructions and requirements",
            "Problem set with solutions",
            "Homework assignment materials"
        };
        
        String[] projectDescriptions = {
            "Project documentation and source code",
            "Project report and analysis",
            "Implementation files and documentation"
        };
        
        String[] lectureDescriptions = {
            "Lecture slides and notes",
            "Presentation materials",
            "Class notes and examples"
        };
        
        String[] otherDescriptions = {
            "Supporting materials",
            "Additional resources",
            "Supplementary documentation"
        };
        
        String[] descriptions;
        switch (documentType) {
            case SYLLABUS:
                descriptions = syllabusDescriptions;
                break;
            case EXAM:
                descriptions = examDescriptions;
                break;
            case ASSIGNMENT:
                descriptions = assignmentDescriptions;
                break;
            case PROJECT_DOCS:
                descriptions = projectDescriptions;
                break;
            case LECTURE_NOTES:
                descriptions = lectureDescriptions;
                break;
            case OTHER:
                descriptions = otherDescriptions;
                break;
            default:
                return "Document file";
        }
        
        return descriptions[(int) (Math.random() * descriptions.length)];
    }
}
