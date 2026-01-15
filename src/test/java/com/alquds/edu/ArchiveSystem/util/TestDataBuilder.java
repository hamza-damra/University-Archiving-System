package com.alquds.edu.ArchiveSystem.util;

import com.alquds.edu.ArchiveSystem.entity.academic.*;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.user.Notification;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.entity.auth.RefreshToken;
import com.alquds.edu.ArchiveSystem.entity.file.Folder;
import com.alquds.edu.ArchiveSystem.entity.file.FolderType;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.alquds.edu.ArchiveSystem.dto.user.UserCreateRequest;
import com.alquds.edu.ArchiveSystem.dto.user.UserUpdateRequest;
import com.alquds.edu.ArchiveSystem.dto.user.ProfessorDTO;
import com.alquds.edu.ArchiveSystem.dto.academic.CourseDTO;
import com.alquds.edu.ArchiveSystem.dto.academic.CourseAssignmentDTO;
import com.alquds.edu.ArchiveSystem.dto.academic.DepartmentDTO;
import com.alquds.edu.ArchiveSystem.dto.academic.AcademicYearDTO;
import com.alquds.edu.ArchiveSystem.dto.academic.SemesterDTO;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Test data builder utility following the Test Data Builder pattern.
 * Makes tests more readable and maintainable.
 * Uses constructors and setters since entities don't use @Builder.
 */
public class TestDataBuilder {
    
    // ==================== User Builders ====================
    
    public static User createUser() {
        User user = new User();
        user.setEmail("test@staff.alquds.edu");
        user.setPassword("$2a$10$dummyEncodedPassword");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(Role.ROLE_PROFESSOR);
        user.setIsActive(true);
        return user;
    }
    
    public static User createAdminUser() {
        User user = createUser();
        user.setEmail("admin@admin.alquds.edu");
        user.setRole(Role.ROLE_ADMIN);
        return user;
    }
    
    public static User createHodUser() {
        User user = createUser();
        user.setEmail("hod.cs@hod.alquds.edu");
        user.setRole(Role.ROLE_HOD);
        return user;
    }
    
    public static User createProfessorUser() {
        User user = createUser();
        user.setEmail("professor@staff.alquds.edu");
        user.setRole(Role.ROLE_PROFESSOR);
        return user;
    }
    
    public static UserCreateRequest createUserCreateRequest() {
        UserCreateRequest request = new UserCreateRequest();
        request.setEmail("newuser@staff.alquds.edu");
        request.setPassword("TestPass123!");
        request.setFirstName("New");
        request.setLastName("User");
        request.setRole(Role.ROLE_PROFESSOR);
        return request;
    }
    
    public static UserUpdateRequest createUserUpdateRequest() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");
        return request;
    }
    
    public static ProfessorDTO createProfessorDTO() {
        ProfessorDTO dto = new ProfessorDTO();
        dto.setEmail("newprofessor@staff.alquds.edu");
        dto.setPassword("TestPass123!");
        dto.setFirstName("New");
        dto.setLastName("Professor");
        dto.setDepartmentId(1L);
        dto.setIsActive(true);
        return dto;
    }
    
    // ==================== Department Builders ====================
    
    public static Department createDepartment() {
        Department department = new Department();
        department.setName("Computer Science");
        department.setShortcut("cs"); // Must be lowercase per validation
        return department;
    }
    
    public static DepartmentDTO createDepartmentDTO() {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setName("Computer Science");
        dto.setShortcut("cs"); // Must be lowercase per validation
        return dto;
    }
    
    // ==================== Course Builders ====================
    
    public static Course createCourse() {
        Course course = new Course();
        course.setCourseCode("CS101");
        course.setCourseName("Introduction to Computer Science");
        course.setLevel("Undergraduate");
        course.setDescription("Basic computer science concepts");
        course.setIsActive(true);
        return course;
    }
    
    public static CourseDTO createCourseDTO() {
        CourseDTO dto = new CourseDTO();
        dto.setCourseCode("CS101");
        dto.setCourseName("Introduction to Computer Science");
        dto.setLevel("Undergraduate");
        dto.setDescription("Basic computer science concepts");
        dto.setIsActive(true);
        return dto;
    }
    
    // ==================== Academic Year Builders ====================
    
    public static AcademicYear createAcademicYear() {
        AcademicYear academicYear = new AcademicYear();
        academicYear.setYearCode("2024-2025");
        academicYear.setStartYear(2024);
        academicYear.setEndYear(2025);
        academicYear.setIsActive(true);
        return academicYear;
    }
    
    /**
     * Enhanced version that allows custom year code.
     */
    public static AcademicYear createAcademicYear(String yearCode) {
        AcademicYear academicYear = new AcademicYear();
        academicYear.setYearCode(yearCode);
        String[] years = yearCode.split("-");
        if (years.length == 2) {
            academicYear.setStartYear(Integer.parseInt(years[0]));
            academicYear.setEndYear(Integer.parseInt(years[1]));
        } else {
            academicYear.setStartYear(2024);
            academicYear.setEndYear(2025);
        }
        academicYear.setIsActive(true);
        return academicYear;
    }
    
    public static AcademicYearDTO createAcademicYearDTO() {
        AcademicYearDTO dto = new AcademicYearDTO();
        dto.setYearCode("2024-2025");
        dto.setStartYear(2024);
        dto.setEndYear(2025);
        dto.setIsActive(true);
        return dto;
    }
    
    // ==================== Semester Builders ====================
    
    public static Semester createSemester() {
        Semester semester = new Semester();
        semester.setType(SemesterType.FIRST);
        semester.setStartDate(LocalDate.of(2024, 9, 1));
        semester.setEndDate(LocalDate.of(2025, 1, 15));
        semester.setIsActive(true);
        return semester;
    }
    
    /**
     * Enhanced version that accepts an AcademicYear.
     */
    public static Semester createSemester(AcademicYear academicYear) {
        Semester semester = createSemester();
        semester.setAcademicYear(academicYear);
        return semester;
    }
    
    /**
     * Enhanced version that accepts both AcademicYear and SemesterType.
     */
    public static Semester createSemester(AcademicYear academicYear, SemesterType type) {
        Semester semester = createSemester(academicYear);
        semester.setType(type);
        // Adjust dates based on semester type
        if (academicYear != null) {
            switch (type) {
                case FIRST:
                    semester.setStartDate(LocalDate.of(academicYear.getStartYear(), 9, 1));
                    semester.setEndDate(LocalDate.of(academicYear.getEndYear(), 1, 15));
                    break;
                case SECOND:
                    semester.setStartDate(LocalDate.of(academicYear.getEndYear(), 2, 1));
                    semester.setEndDate(LocalDate.of(academicYear.getEndYear(), 6, 15));
                    break;
                case SUMMER:
                    semester.setStartDate(LocalDate.of(academicYear.getEndYear(), 6, 20));
                    semester.setEndDate(LocalDate.of(academicYear.getEndYear(), 8, 20));
                    break;
            }
        }
        return semester;
    }
    
    public static SemesterDTO createSemesterDTO() {
        SemesterDTO dto = new SemesterDTO();
        dto.setType(SemesterType.FIRST);
        dto.setStartDate(LocalDate.of(2024, 9, 1));
        dto.setEndDate(LocalDate.of(2025, 1, 15));
        dto.setIsActive(true);
        return dto;
    }
    
    // ==================== Course Assignment Builders ====================
    
    public static CourseAssignment createCourseAssignment() {
        CourseAssignment assignment = new CourseAssignment();
        assignment.setIsActive(true);
        return assignment;
    }
    
    public static CourseAssignmentDTO createCourseAssignmentDTO() {
        CourseAssignmentDTO dto = new CourseAssignmentDTO();
        dto.setIsActive(true);
        return dto;
    }
    
    // ==================== Folder Builders ====================
    
    public static Folder createFolder() {
        Folder folder = Folder.builder()
                .path("2024-2025/first/Test User")
                .name("Test User")
                .type(FolderType.PROFESSOR_ROOT)
                .parent(null)
                .owner(createProfessorUser())
                .academicYear(createAcademicYear())
                .semester(createSemester())
                .course(null)
                .build();
        return folder;
    }
    
    /**
     * Enhanced version that accepts owner, academic year, and semester.
     */
    public static Folder createFolder(User owner, AcademicYear academicYear, Semester semester) {
        String yearCode = academicYear != null ? academicYear.getYearCode() : "2024-2025";
        String semesterType = semester != null ? semester.getType().name().toLowerCase() : "first";
        String ownerName = owner != null ? owner.getFirstName() + " " + owner.getLastName() : "Test User";
        String path = yearCode + "/" + semesterType + "/" + ownerName;
        
        return Folder.builder()
                .path(path)
                .name(ownerName)
                .type(FolderType.PROFESSOR_ROOT)
                .parent(null)
                .owner(owner != null ? owner : createProfessorUser())
                .academicYear(academicYear != null ? academicYear : createAcademicYear())
                .semester(semester != null ? semester : createSemester())
                .course(null)
                .build();
    }
    
    // ==================== Refresh Token Builders ====================
    
    public static RefreshToken createRefreshToken() {
        return RefreshToken.builder()
                .token("test-refresh-token-" + System.currentTimeMillis())
                .user(createUser())
                .expiryDate(Instant.now().plusSeconds(86400 * 7)) // 7 days from now
                .revoked(false)
                .deviceInfo("Test Device")
                .build();
    }
    
    /**
     * Enhanced version that accepts a user.
     */
    public static RefreshToken createRefreshToken(User user) {
        return RefreshToken.builder()
                .token("test-refresh-token-" + System.currentTimeMillis())
                .user(user != null ? user : createUser())
                .expiryDate(Instant.now().plusSeconds(86400 * 7)) // 7 days from now
                .revoked(false)
                .deviceInfo("Test Device")
                .build();
    }
    
    
    // ==================== Notification Builders ====================
    
    public static Notification createNotification() {
        Notification notification = new Notification();
        notification.setUser(createUser());
        notification.setTitle("Test Notification");
        notification.setMessage("This is a test notification message");
        notification.setType(Notification.NotificationType.NEW_REQUEST);
        notification.setIsRead(false);
        notification.setRelatedEntityId(null);
        notification.setRelatedEntityType(null);
        return notification;
    }
    
    /**
     * Enhanced version that accepts a user.
     */
    public static Notification createNotification(User user) {
        Notification notification = createNotification();
        if (user != null) {
            notification.setUser(user);
        }
        return notification;
    }
    
    /**
     * Enhanced version that accepts user and notification type.
     */
    public static Notification createNotification(User user, Notification.NotificationType type) {
        Notification notification = createNotification(user);
        if (type != null) {
            notification.setType(type);
        }
        return notification;
    }
    
    // ==================== Uploaded File Builders ====================
    
    public static UploadedFile createUploadedFile() {
        return UploadedFile.builder()
                .folder(createFolder())
                .originalFilename("test-document.pdf")
                .storedFilename("test-document-" + System.currentTimeMillis() + ".pdf")
                .fileUrl("2024-2025/first/Test User/test-document.pdf")
                .fileSize(1024L * 100) // 100 KB
                .fileType("application/pdf")
                .uploader(createProfessorUser())
                .notes("Test file upload")
                .documentSubmission(null)
                .fileOrder(1)
                .description("Test uploaded file")
                .build();
    }
    
    /**
     * Enhanced version that accepts folder and uploader.
     */
    public static UploadedFile createUploadedFile(Folder folder, User uploader) {
        String filename = "test-document.pdf";
        String storedFilename = "test-document-" + System.currentTimeMillis() + ".pdf";
        String fileUrl = folder != null ? folder.getPath() + "/" + storedFilename : "test/" + storedFilename;
        
        return UploadedFile.builder()
                .folder(folder != null ? folder : createFolder())
                .originalFilename(filename)
                .storedFilename(storedFilename)
                .fileUrl(fileUrl)
                .fileSize(1024L * 100) // 100 KB
                .fileType("application/pdf")
                .uploader(uploader != null ? uploader : createProfessorUser())
                .notes("Test file upload")
                .documentSubmission(null)
                .fileOrder(1)
                .description("Test uploaded file")
                .build();
    }
    
    /**
     * Enhanced version with custom filename and file type.
     */
    public static UploadedFile createUploadedFile(Folder folder, User uploader, String filename, String fileType) {
        String storedFilename = filename.replaceAll("[^a-zA-Z0-9.-]", "_") + "-" + System.currentTimeMillis();
        String fileUrl = folder != null ? folder.getPath() + "/" + storedFilename : "test/" + storedFilename;
        
        return UploadedFile.builder()
                .folder(folder != null ? folder : createFolder())
                .originalFilename(filename)
                .storedFilename(storedFilename)
                .fileUrl(fileUrl)
                .fileSize(1024L * 100) // 100 KB
                .fileType(fileType != null ? fileType : "application/pdf")
                .uploader(uploader != null ? uploader : createProfessorUser())
                .notes("Test file upload")
                .documentSubmission(null)
                .fileOrder(1)
                .description("Test uploaded file")
                .build();
    }
}
