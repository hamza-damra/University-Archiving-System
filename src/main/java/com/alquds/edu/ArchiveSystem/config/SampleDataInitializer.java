package com.alquds.edu.ArchiveSystem.config;

import com.alquds.edu.ArchiveSystem.entity.academic.*;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.entity.submission.*;
import com.alquds.edu.ArchiveSystem.entity.task.Task;
import com.alquds.edu.ArchiveSystem.entity.task.TaskStatus;
import com.alquds.edu.ArchiveSystem.entity.user.Notification;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.repository.academic.*;
import com.alquds.edu.ArchiveSystem.repository.submission.*;
import com.alquds.edu.ArchiveSystem.repository.task.TaskRepository;
import com.alquds.edu.ArchiveSystem.repository.user.NotificationRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Initializes comprehensive sample data for all project pages.
 * This includes departments, users (Deanship, HODs, Professors), academic years,
 * semesters, courses, course assignments, required document types, document submissions,
 * and notifications.
 * 
 * Runs after AdminInitializer (Order=2) to ensure database is ready.
 * Only runs if no departments exist (to avoid duplicate data).
 */
@Component
@Profile("dev")
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class SampleDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final RequiredDocumentTypeRepository requiredDocumentTypeRepository;
    private final DocumentSubmissionRepository documentSubmissionRepository;
    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initializeSampleData();
    }

    @Transactional
    public void initializeSampleData() {
        try {
            // Check if comprehensive sample data already exists
            // We check for course assignments as the main indicator of complete sample data
            long courseAssignmentCount = courseAssignmentRepository.count();
            long submissionCount = documentSubmissionRepository.count();
            
            // If we have substantial data (more than 10 assignments and 20 submissions), skip
            if (courseAssignmentCount > 10 && submissionCount > 20) {
                log.info("Sample data already exists ({} course assignments, {} submissions). Skipping sample data initialization.", 
                        courseAssignmentCount, submissionCount);
                return;
            }
            
            // If we have some data but not enough, log a warning but continue
            if (courseAssignmentCount > 0 || submissionCount > 0) {
                log.warn("Some data exists but may be incomplete ({} assignments, {} submissions). " +
                        "Adding additional sample data...", courseAssignmentCount, submissionCount);
            } else {
                log.info("No sample data found. Initializing comprehensive sample data...");
            }

            log.info("============================================");
            log.info("INITIALIZING SAMPLE DATA");
            log.info("============================================");

            // 1. Create Departments
            List<Department> departments = createDepartments();
            log.info("Created {} departments", departments.size());

            // 2. Create Users (Deanship, HODs, Professors)
            User deanshipUser = createDeanshipUser();
            List<User> hodUsers = createHodUsers(departments);
            List<User> professorUsers = createProfessorUsers(departments);
            log.info("Created 1 deanship user, {} HOD users, {} professor users", 
                    hodUsers.size(), professorUsers.size());

            // 3. Create Academic Years and Semesters
            List<AcademicYear> academicYears = createAcademicYears();
            List<Semester> semesters = createSemesters(academicYears);
            log.info("Created {} academic years, {} semesters", 
                    academicYears.size(), semesters.size());

            // 4. Create Courses
            List<Course> courses = createCourses(departments);
            log.info("Created {} courses", courses.size());

            // 5. Create Course Assignments
            List<CourseAssignment> courseAssignments = createCourseAssignments(
                    semesters, courses, professorUsers);
            log.info("Created {} course assignments", courseAssignments.size());

            // 6. Create Required Document Types
            List<RequiredDocumentType> requiredDocTypes = createRequiredDocumentTypes(
                    courses, semesters);
            log.info("Created {} required document types", requiredDocTypes.size());

            // 7. Create Document Submissions
            List<DocumentSubmission> submissions = createDocumentSubmissions(
                    courseAssignments, professorUsers);
            log.info("Created {} document submissions", submissions.size());

            // 8. Create Notifications
            List<Notification> notifications = createNotifications(
                    deanshipUser, hodUsers, professorUsers);
            log.info("Created {} notifications", notifications.size());

            // 9. Create Tasks
            List<Task> tasks = createTasks(courseAssignments, professorUsers, semesters);
            log.info("Created {} tasks", tasks.size());

            log.info("============================================");
            log.info("SAMPLE DATA INITIALIZATION COMPLETED");
            log.info("============================================");
            log.info("Sample Users Created:");
            log.info("  Deanship: deanship@alquds.edu / Deanship@123");
            log.info("  HODs: hod.cs@alquds.edu, hod.math@alquds.edu / Hod@123");
            log.info("  Professors: prof1@alquds.edu, prof2@alquds.edu, etc. / Prof@123");
            log.info("============================================");

        } catch (Exception e) {
            log.error("Failed to initialize sample data: {}", e.getMessage(), e);
            // Don't throw - allow application to continue starting
        }
    }

    private List<Department> createDepartments() {
        List<Department> departments = new ArrayList<>();
        
        String[][] deptData = {
            {"Computer Science", "cs", "Department of Computer Science"},
            {"Mathematics", "math", "Department of Mathematics"},
            {"Physics", "physics", "Department of Physics"},
            {"Chemistry", "chemistry", "Department of Chemistry"},
            {"Engineering", "eng", "Department of Engineering"}
        };

        // Get existing departments to avoid duplicates
        List<Department> existingDepts = departmentRepository.findAll();
        
        for (String[] data : deptData) {
            // Check if department with this shortcut or name already exists
            Department existing = existingDepts.stream()
                    .filter(d -> d.getShortcut().equalsIgnoreCase(data[1]) || d.getName().equalsIgnoreCase(data[0]))
                    .findFirst()
                    .orElse(null);
            
            if (existing != null) {
                log.debug("Department {} already exists, using existing", data[1]);
                departments.add(existing);
            } else {
                Department dept = new Department();
                dept.setName(data[0]);
                dept.setShortcut(data[1]);
                dept.setDescription(data[2]);
                departments.add(departmentRepository.save(dept));
                log.debug("Created department: {}", data[0]);
            }
        }

        return departments;
    }

    private User createDeanshipUser() {
        String email = "deanship@alquds.edu";
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            log.debug("Deanship user {} already exists, using existing", email);
            return existing;
        }
        
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Deanship@123"));
        user.setFirstName("Deanship");
        user.setLastName("Administrator");
        user.setRole(Role.ROLE_DEANSHIP);
        user.setIsActive(true);
        return userRepository.save(user);
    }

    private List<User> createHodUsers(List<Department> departments) {
        List<User> hodUsers = new ArrayList<>();
        
        String[][] hodData = {
            {"hod.cs@alquds.edu", "Ahmed", "Ali", "CS001"},
            {"hod.math@alquds.edu", "Fatima", "Hassan", "MATH001"},
            {"hod.physics@alquds.edu", "Mohammed", "Ibrahim", "PHYS001"}
        };

        for (int i = 0; i < hodData.length && i < departments.size(); i++) {
            String email = hodData[i][0];
            User existing = userRepository.findByEmail(email).orElse(null);
            if (existing != null) {
                log.debug("HOD user {} already exists, using existing", email);
                hodUsers.add(existing);
                continue;
            }
            
            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("Hod@123"));
            user.setFirstName(hodData[i][1]);
            user.setLastName(hodData[i][2]);
            user.setProfessorId(hodData[i][3]);
            user.setRole(Role.ROLE_HOD);
            user.setDepartment(departments.get(i));
            user.setIsActive(true);
            hodUsers.add(userRepository.save(user));
        }

        return hodUsers;
    }

    private List<User> createProfessorUsers(List<Department> departments) {
        List<User> professors = new ArrayList<>();
        
        Object[][] profData = {
            {"prof1@alquds.edu", "Omar", "Khalil", "PROF001", 0}, // CS
            {"prof2@alquds.edu", "Layla", "Mahmoud", "PROF002", 0}, // CS
            {"prof3@alquds.edu", "Youssef", "Nasser", "PROF003", 1}, // Math
            {"prof4@alquds.edu", "Nour", "Salem", "PROF004", 1}, // Math
            {"prof5@alquds.edu", "Khalid", "Omar", "PROF005", 2}, // Physics
            {"prof6@alquds.edu", "Sara", "Ahmed", "PROF006", 0}, // CS
            {"prof7@alquds.edu", "Hassan", "Mohammed", "PROF007", 3}, // Chemistry
            {"prof8@alquds.edu", "Mariam", "Ali", "PROF008", 4}  // Engineering
        };

        for (Object[] data : profData) {
            int deptIndex = (Integer) data[4];
            if (deptIndex < departments.size()) {
                String email = (String) data[0];
                User existing = userRepository.findByEmail(email).orElse(null);
                if (existing != null) {
                    log.debug("Professor user {} already exists, using existing", email);
                    professors.add(existing);
                    continue;
                }
                
                User user = new User();
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode("Prof@123"));
                user.setFirstName((String) data[1]);
                user.setLastName((String) data[2]);
                user.setProfessorId((String) data[3]);
                user.setRole(Role.ROLE_PROFESSOR);
                user.setDepartment(departments.get(deptIndex));
                user.setIsActive(true);
                professors.add(userRepository.save(user));
            }
        }

        return professors;
    }

    private List<AcademicYear> createAcademicYears() {
        List<AcademicYear> academicYears = new ArrayList<>();
        
        // Create multiple academic years: 2 past years, current year, and 2 future years
        int currentYear = LocalDate.now().getYear();
        
        // Create academic years from 2 years ago to 2 years in the future (5 years total)
        for (int i = -2; i <= 2; i++) {
            int startYear = currentYear + i;
            int endYear = startYear + 1;
            String yearCode = startYear + "-" + endYear;
            
            // Check if academic year already exists
            if (academicYearRepository.findByYearCode(yearCode).isPresent()) {
                log.debug("Academic year {} already exists, skipping", yearCode);
                continue;
            }
            
            AcademicYear year = new AcademicYear();
            year.setYearCode(yearCode);
            year.setStartYear(startYear);
            year.setEndYear(endYear);
            // Only the current academic year should be active
            year.setIsActive(i == 0);
            
            academicYears.add(academicYearRepository.save(year));
            log.debug("Created academic year: {} (Active: {})", yearCode, year.getIsActive());
        }

        return academicYears;
    }

    private List<Semester> createSemesters(List<AcademicYear> academicYears) {
        List<Semester> semesters = new ArrayList<>();
        
        for (AcademicYear year : academicYears) {
            int startYear = year.getStartYear();
            int endYear = year.getEndYear();
            
            // Check if semesters already exist for this academic year
            if (semesterRepository.findByAcademicYearId(year.getId()).size() > 0) {
                log.debug("Semesters already exist for academic year {}, skipping", year.getYearCode());
                continue;
            }
            
            // FIRST Semester (Fall): September - December
            Semester first = new Semester();
            first.setAcademicYear(year);
            first.setType(SemesterType.FIRST);
            first.setStartDate(LocalDate.of(startYear, 9, 1));
            first.setEndDate(LocalDate.of(startYear, 12, 31));
            // First semester is active if the academic year is active
            first.setIsActive(year.getIsActive());
            semesters.add(semesterRepository.save(first));
            log.debug("Created FIRST semester for academic year {}", year.getYearCode());
            
            // SECOND Semester (Spring): February - May
            Semester second = new Semester();
            second.setAcademicYear(year);
            second.setType(SemesterType.SECOND);
            second.setStartDate(LocalDate.of(endYear, 2, 1));
            second.setEndDate(LocalDate.of(endYear, 5, 31));
            // Second semester is active if the academic year is active
            second.setIsActive(year.getIsActive());
            semesters.add(semesterRepository.save(second));
            log.debug("Created SECOND semester for academic year {}", year.getYearCode());
            
            // SUMMER Semester: June - August
            Semester summer = new Semester();
            summer.setAcademicYear(year);
            summer.setType(SemesterType.SUMMER);
            summer.setStartDate(LocalDate.of(endYear, 6, 1));
            summer.setEndDate(LocalDate.of(endYear, 8, 31));
            // Summer semester is usually not active by default
            summer.setIsActive(false);
            semesters.add(semesterRepository.save(summer));
            log.debug("Created SUMMER semester for academic year {}", year.getYearCode());
        }

        log.info("Created {} semesters for {} academic years", semesters.size(), academicYears.size());
        return semesters;
    }

    private List<Course> createCourses(List<Department> departments) {
        List<Course> courses = new ArrayList<>();
        
        // CS Department Courses
        Department csDept = departments.stream()
                .filter(d -> d.getShortcut().equals("cs"))
                .findFirst().orElse(departments.get(0));
        
        String[][] csCourses = {
            {"CS101", "Introduction to Programming", "Undergraduate", "Basic programming concepts"},
            {"CS201", "Data Structures", "Undergraduate", "Fundamental data structures"},
            {"CS301", "Database Systems", "Undergraduate", "Database design and SQL"},
            {"CS401", "Software Engineering", "Undergraduate", "Software development lifecycle"},
            {"CS501", "Advanced Algorithms", "Graduate", "Advanced algorithmic techniques"}
        };

        for (String[] data : csCourses) {
            courses.add(getOrCreateCourse(data[0], data[1], csDept, data[2], data[3]));
        }

        // Math Department Courses
        Department mathDept = departments.stream()
                .filter(d -> d.getShortcut().equals("math"))
                .findFirst().orElse(departments.get(1));
        
        String[][] mathCourses = {
            {"MATH101", "Calculus I", "Undergraduate", "Differential and integral calculus"},
            {"MATH201", "Linear Algebra", "Undergraduate", "Vector spaces and matrices"},
            {"MATH301", "Differential Equations", "Undergraduate", "Ordinary and partial differential equations"}
        };

        for (String[] data : mathCourses) {
            courses.add(getOrCreateCourse(data[0], data[1], mathDept, data[2], data[3]));
        }

        // Physics Department Courses
        if (departments.size() > 2) {
            Department physicsDept = departments.get(2);
            String[][] physicsCourses = {
                {"PHYS101", "General Physics I", "Undergraduate", "Mechanics and thermodynamics"},
                {"PHYS201", "General Physics II", "Undergraduate", "Electricity and magnetism"}
            };

            for (String[] data : physicsCourses) {
                courses.add(getOrCreateCourse(data[0], data[1], physicsDept, data[2], data[3]));
            }
        }

        return courses;
    }
    
    private Course getOrCreateCourse(String courseCode, String courseName, Department department, String level, String description) {
        // Check if course already exists
        Course existing = courseRepository.findByCourseCode(courseCode).orElse(null);
        if (existing != null) {
            log.debug("Course {} already exists, using existing", courseCode);
            return existing;
        }
        
        // Create new course
        Course course = new Course();
        course.setCourseCode(courseCode);
        course.setCourseName(courseName);
        course.setDepartment(department);
        course.setLevel(level);
        course.setDescription(description);
        course.setIsActive(true);
        Course saved = courseRepository.save(course);
        log.debug("Created course: {}", courseCode);
        return saved;
    }

    private List<CourseAssignment> createCourseAssignments(
            List<Semester> semesters, List<Course> courses, List<User> professors) {
        
        List<CourseAssignment> assignments = new ArrayList<>();
        
        // Fetch professors with departments eagerly loaded to avoid LazyInitializationException
        List<User> professorsWithDept = userRepository.findByRoleWithDepartment(Role.ROLE_PROFESSOR);
        
        // Get active semesters (FIRST and SECOND of current year, and previous year's semesters)
        List<Semester> activeSemesters = semesters.stream()
                .filter(s -> s.getIsActive() || 
                           (s.getType() == SemesterType.FIRST || s.getType() == SemesterType.SECOND))
                .toList();
        
        // If no active semesters, use first 3 semesters
        List<Semester> semestersToUse = activeSemesters.isEmpty() ? 
                semesters.subList(0, Math.min(3, semesters.size())) : activeSemesters;
        
        // Assign professors to courses for multiple semesters
        // CS courses
        List<Course> csCourses = courses.stream()
                .filter(c -> c.getCourseCode().startsWith("CS"))
                .toList();
        
        List<User> csProfessors = professorsWithDept.stream()
                .filter(p -> p.getDepartment() != null && 
                            p.getDepartment().getShortcut().equals("cs"))
                .toList();
        
        // Create assignments for each semester
        for (Semester semester : semestersToUse) {
            for (int i = 0; i < csCourses.size(); i++) {
                CourseAssignment assignment = new CourseAssignment();
                assignment.setSemester(semester);
                assignment.setCourse(csCourses.get(i));
                assignment.setProfessor(csProfessors.get(i % csProfessors.size()));
                assignment.setIsActive(true);
                assignments.add(courseAssignmentRepository.save(assignment));
            }
        }
        
        // Math courses
        List<Course> mathCourses = courses.stream()
                .filter(c -> c.getCourseCode().startsWith("MATH"))
                .toList();
        
        List<User> mathProfessors = professorsWithDept.stream()
                .filter(p -> p.getDepartment() != null && 
                            p.getDepartment().getShortcut().equals("math"))
                .toList();
        
        for (Semester semester : semestersToUse) {
            for (int i = 0; i < mathCourses.size(); i++) {
                CourseAssignment assignment = new CourseAssignment();
                assignment.setSemester(semester);
                assignment.setCourse(mathCourses.get(i));
                assignment.setProfessor(mathProfessors.get(i % mathProfessors.size()));
                assignment.setIsActive(true);
                assignments.add(courseAssignmentRepository.save(assignment));
            }
        }
        
        // Physics courses
        List<Course> physicsCourses = courses.stream()
                .filter(c -> c.getCourseCode().startsWith("PHYS"))
                .toList();
        
        if (!physicsCourses.isEmpty()) {
            List<User> physicsProfessors = professorsWithDept.stream()
                    .filter(p -> p.getDepartment() != null && 
                                p.getDepartment().getShortcut().equals("physics"))
                    .toList();
            
            for (Semester semester : semestersToUse) {
                for (int i = 0; i < physicsCourses.size(); i++) {
                    CourseAssignment assignment = new CourseAssignment();
                    assignment.setSemester(semester);
                    assignment.setCourse(physicsCourses.get(i));
                    assignment.setProfessor(physicsProfessors.get(i % physicsProfessors.size()));
                    assignment.setIsActive(true);
                    assignments.add(courseAssignmentRepository.save(assignment));
                }
            }
        }

        return assignments;
    }

    private List<RequiredDocumentType> createRequiredDocumentTypes(
            List<Course> courses, List<Semester> semesters) {
        
        List<RequiredDocumentType> requiredDocTypes = new ArrayList<>();
        
        // Create required document types for multiple semesters (not just active one)
        List<Semester> semestersToUse = semesters.stream()
                .filter(s -> s.getIsActive() || 
                           (s.getType() == SemesterType.FIRST || s.getType() == SemesterType.SECOND))
                .toList();
        
        if (semestersToUse.isEmpty()) {
            semestersToUse = semesters.subList(0, Math.min(3, semesters.size()));
        }
        
        DocumentTypeEnum[] allDocTypes = {
            DocumentTypeEnum.SYLLABUS,
            DocumentTypeEnum.EXAM,
            DocumentTypeEnum.ASSIGNMENT,
            DocumentTypeEnum.PROJECT_DOCS,
            DocumentTypeEnum.LECTURE_NOTES
        };
        
        // Create required document types for each course and semester
        for (Course course : courses) {
            for (Semester semester : semestersToUse) {
                for (DocumentTypeEnum docType : allDocTypes) {
                    // Check if already exists
                    List<RequiredDocumentType> existing = requiredDocumentTypeRepository
                        .findByCourseIdAndSemesterId(course.getId(), semester.getId());
                    boolean exists = existing.stream()
                        .anyMatch(rdt -> rdt.getDocumentType() == docType);
                    
                    if (exists) {
                        log.debug("Required doc type already exists for course {}, semester {}, type {}, skipping", 
                                course.getCourseCode(), semester.getType(), docType);
                        continue;
                    }
                    
                    RequiredDocumentType reqDocType = new RequiredDocumentType();
                    reqDocType.setCourse(course);
                    reqDocType.setSemester(semester);
                    reqDocType.setDocumentType(docType);
                    reqDocType.setIsRequired(true);
                    reqDocType.setMaxFileCount(5);
                    reqDocType.setMaxTotalSizeMb(50);
                    
                    // Set allowed extensions based on document type
                    if (docType == DocumentTypeEnum.SYLLABUS || docType == DocumentTypeEnum.EXAM) {
                        reqDocType.setAllowedFileExtensions(Arrays.asList("pdf", "doc", "docx"));
                    } else if (docType == DocumentTypeEnum.ASSIGNMENT) {
                        reqDocType.setAllowedFileExtensions(Arrays.asList("pdf", "doc", "docx", "zip"));
                    } else {
                        reqDocType.setAllowedFileExtensions(Arrays.asList("pdf", "ppt", "pptx", "doc", "docx"));
                    }
                    
                    // Set deadlines (14 days for syllabus, 30 days for others)
                    LocalDateTime deadline = semester.getStartDate()
                            .atStartOfDay()
                            .plusDays(docType == DocumentTypeEnum.SYLLABUS ? 14 : 30);
                    reqDocType.setDeadline(deadline);
                    
                    requiredDocTypes.add(requiredDocumentTypeRepository.save(reqDocType));
                }
            }
        }

        return requiredDocTypes;
    }

    private List<DocumentSubmission> createDocumentSubmissions(
            List<CourseAssignment> assignments, List<User> professors) {
        
        List<DocumentSubmission> submissions = new ArrayList<>();
        
        // Create submissions for ALL course assignments with ALL document types
        // This ensures comprehensive data for reports
        DocumentTypeEnum[] allDocTypes = {
            DocumentTypeEnum.SYLLABUS,
            DocumentTypeEnum.EXAM,
            DocumentTypeEnum.ASSIGNMENT,
            DocumentTypeEnum.PROJECT_DOCS,
            DocumentTypeEnum.LECTURE_NOTES
        };
        
        int submissionCount = 0;
        for (CourseAssignment assignment : assignments) {
            // Create submissions for all document types
            for (DocumentTypeEnum docType : allDocTypes) {
                // Check if submission already exists
                boolean exists = documentSubmissionRepository
                    .findByCourseAssignmentIdAndDocumentType(assignment.getId(), docType)
                    .isPresent();
                
                if (exists) {
                    log.debug("Submission already exists for assignment {} and type {}, skipping", 
                            assignment.getId(), docType);
                    continue;
                }
                
                DocumentSubmission submission = new DocumentSubmission();
                submission.setCourseAssignment(assignment);
                submission.setDocumentType(docType);
                submission.setProfessor(assignment.getProfessor());
                
                // Distribute statuses: 40% UPLOADED, 35% NOT_UPLOADED, 25% OVERDUE
                int statusMod = submissionCount % 20;
                if (statusMod < 8) {
                    // UPLOADED (40%)
                    submission.setStatus(SubmissionStatus.UPLOADED);
                    submission.setSubmittedAt(LocalDateTime.now().minusDays(submissionCount % 30));
                    submission.setFileCount(1 + (submissionCount % 4)); // 1-4 files
                    submission.setTotalFileSize(1024L * 1024 * (5 + submissionCount % 45)); // 5-50 MB
                    submission.setIsLateSubmission(statusMod == 7); // Some are late
                } else if (statusMod < 15) {
                    // NOT_UPLOADED (35%)
                    submission.setStatus(SubmissionStatus.NOT_UPLOADED);
                    submission.setSubmittedAt(LocalDateTime.now());
                    submission.setFileCount(0);
                    submission.setTotalFileSize(0L);
                    submission.setIsLateSubmission(false);
                } else {
                    // OVERDUE (25%)
                    submission.setStatus(SubmissionStatus.OVERDUE);
                    submission.setSubmittedAt(null);
                    submission.setFileCount(0);
                    submission.setTotalFileSize(0L);
                    submission.setIsLateSubmission(true);
                }
                
                submission.setNotes(submissionCount % 3 == 0 ? 
                    "Sample submission notes for " + docType.name() : null);
                
                submissions.add(documentSubmissionRepository.save(submission));
                submissionCount++;
            }
        }

        log.info("Created {} document submissions across {} course assignments", 
                submissions.size(), assignments.size());
        return submissions;
    }

    private List<Notification> createNotifications(
            User deanshipUser, List<User> hodUsers, List<User> professors) {
        
        List<Notification> notifications = new ArrayList<>();
        
        // Notifications for Deanship
        String[] deanshipTitles = {
            "New Academic Year Started",
            "Semester Registration Open",
            "Document Submission Reminder"
        };
        
        for (String title : deanshipTitles) {
            Notification notification = new Notification();
            notification.setUser(deanshipUser);
            notification.setTitle(title);
            notification.setMessage("This is a sample notification for the deanship user.");
            notification.setType(Notification.NotificationType.DOCUMENT_SUBMITTED);
            notification.setIsRead(false);
            notifications.add(notificationRepository.save(notification));
        }
        
        // Notifications for HODs
        for (User hod : hodUsers) {
            Notification notification = new Notification();
            notification.setUser(hod);
            notification.setTitle("Department Activity Update");
            notification.setMessage("New document submissions in your department.");
            notification.setType(Notification.NotificationType.DOCUMENT_SUBMITTED);
            notification.setIsRead(false);
            notifications.add(notificationRepository.save(notification));
        }
        
        // Notifications for Professors
        for (int i = 0; i < Math.min(professors.size(), 5); i++) {
            User prof = professors.get(i);
            
            Notification.NotificationType[] types = {
                Notification.NotificationType.DEADLINE_APPROACHING,
                Notification.NotificationType.DOCUMENT_SUBMITTED,
                Notification.NotificationType.REQUEST_REMINDER
            };
            
            Notification notification = new Notification();
            notification.setUser(prof);
            notification.setTitle("Course Document Reminder");
            notification.setMessage("Please remember to submit required documents for your assigned courses.");
            notification.setType(types[i % types.length]);
            notification.setIsRead(i % 2 == 0); // Some read, some unread
            notifications.add(notificationRepository.save(notification));
        }

        return notifications;
    }

    private List<Task> createTasks(
            List<CourseAssignment> assignments, List<User> professors, List<Semester> semesters) {
        
        List<Task> tasks = new ArrayList<>();
        
        // Task templates with varied statuses and progress
        String[][] taskTemplates = {
            {"Syllabus Preparation", "Prepare and submit course syllabus", "20", "0", "PENDING"},
            {"Midterm Exam", "Create and submit midterm exam questions", "30", "50", "IN_PROGRESS"},
            {"Final Exam", "Create and submit final exam questions", "30", "100", "COMPLETED"},
            {"Project Documentation", "Submit project documentation and guidelines", "20", "75", "IN_PROGRESS"}
        };
        
        int taskCount = 0;
        
        // Create tasks for each course assignment
        for (CourseAssignment assignment : assignments) {
            // Create 2-4 tasks per course assignment
            int numTasks = 2 + (taskCount % 3); // 2, 3, or 4 tasks
            
            int totalWeight = 0;
            for (int i = 0; i < numTasks && i < taskTemplates.length; i++) {
                String[] template = taskTemplates[i];
                
                Task task = new Task();
                task.setTitle(template[0] + " - " + assignment.getCourse().getCourseCode());
                task.setDescription(template[1]);
                task.setWeightPercentage(Integer.parseInt(template[2]));
                task.setProgressPercentage(Integer.parseInt(template[3]));
                task.setStatus(TaskStatus.valueOf(template[4]));
                task.setProfessor(assignment.getProfessor());
                task.setCourse(assignment.getCourse());
                task.setSemester(assignment.getSemester());
                
                // Add deadline for some tasks (30-90 days from now)
                if (taskCount % 3 == 0) {
                    task.setDeadline(LocalDate.now().plusDays(30 + (taskCount % 60)));
                }
                
                // Mark some completed tasks as overdue if deadline passed
                if (task.getStatus() == TaskStatus.COMPLETED && task.getDeadline() != null) {
                    if (task.getDeadline().isBefore(LocalDate.now().minusDays(10))) {
                        // Keep as completed (not overdue) since it's already completed
                    }
                }
                
                tasks.add(taskRepository.save(task));
                totalWeight += task.getWeightPercentage();
                taskCount++;
            }
            
            // Adjust last task weight to ensure total is 100%
            if (tasks.size() > 0 && totalWeight != 100) {
                Task lastTask = tasks.get(tasks.size() - 1);
                int adjustment = 100 - totalWeight;
                lastTask.setWeightPercentage(lastTask.getWeightPercentage() + adjustment);
                taskRepository.save(lastTask);
            }
        }
        
        // Create some overdue tasks
        for (int i = 0; i < Math.min(5, tasks.size()); i++) {
            Task task = tasks.get(i);
            if (task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.APPROVED) {
                task.setDeadline(LocalDate.now().minusDays(5 + i));
                if (task.getDeadline().isBefore(LocalDate.now())) {
                    task.setStatus(TaskStatus.OVERDUE);
                }
                taskRepository.save(task);
            }
        }
        
        log.info("Created {} tasks across {} course assignments", tasks.size(), assignments.size());
        return tasks;
    }
}
