package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.NodeType;
import com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear;
import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.entity.academic.CourseAssignment;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.entity.academic.SemesterType;
import com.alquds.edu.ArchiveSystem.entity.file.Folder;
import com.alquds.edu.ArchiveSystem.entity.file.FolderType;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentSubmission;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentTypeEnum;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.repository.academic.AcademicYearRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.CourseAssignmentRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.SemesterRepository;
import com.alquds.edu.ArchiveSystem.repository.file.FolderRepository;
import com.alquds.edu.ArchiveSystem.repository.file.UploadedFileRepository;
import com.alquds.edu.ArchiveSystem.repository.submission.DocumentSubmissionRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FileExplorerService Unit Tests")
class FileExplorerServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseAssignmentRepository courseAssignmentRepository;

    @Mock
    private DocumentSubmissionRepository documentSubmissionRepository;

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private FileAccessService fileAccessService;

    @InjectMocks
    private FileExplorerServiceImpl fileExplorerService;

    private User professor1;
    private User professor2;
    private User hodUser;
    private User adminUser;
    private AcademicYear academicYear;
    private Semester semester;
    private Department department;
    private Course course;
    private CourseAssignment assignment;
    private Folder courseFolder;
    private Folder syllabusFolder;
    private DocumentSubmission submission;
    private UploadedFile uploadedFile;

    @BeforeEach
    void setUp() {
        // Setup department
        department = TestDataBuilder.createDepartment();
        department.setId(1L);
        department.setName("Computer Science");

        // Setup academic year
        academicYear = TestDataBuilder.createAcademicYear();
        academicYear.setId(1L);
        academicYear.setYearCode("2024-2025");

        // Setup semester
        semester = TestDataBuilder.createSemester();
        semester.setId(1L);
        semester.setAcademicYear(academicYear);
        semester.setType(SemesterType.FIRST);

        // Setup professors
        professor1 = TestDataBuilder.createProfessorUser();
        professor1.setId(1L);
        professor1.setFirstName("John");
        professor1.setLastName("Doe");
        professor1.setProfessorId("PROF1");
        professor1.setDepartment(department);
        professor1.setIsActive(true);

        professor2 = TestDataBuilder.createProfessorUser();
        professor2.setId(2L);
        professor2.setFirstName("Jane");
        professor2.setLastName("Smith");
        professor2.setProfessorId("PROF2");
        professor2.setDepartment(department);
        professor2.setIsActive(true);

        // Setup HOD user
        hodUser = TestDataBuilder.createHodUser();
        hodUser.setId(3L);
        hodUser.setFirstName("HOD");
        hodUser.setLastName("User");
        hodUser.setDepartment(department);

        // Setup admin user
        adminUser = TestDataBuilder.createAdminUser();
        adminUser.setId(4L);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");

        // Setup course
        course = TestDataBuilder.createCourse();
        course.setId(1L);
        course.setCourseCode("CS101");
        course.setCourseName("Introduction to Computer Science");

        // Setup course assignment
        assignment = TestDataBuilder.createCourseAssignment();
        assignment.setId(1L);
        assignment.setCourse(course);
        assignment.setProfessor(professor1);
        assignment.setSemester(semester);
        assignment.setIsActive(true);

        // Setup folders
        courseFolder = Folder.builder()
                .id(1L)
                .path("2024-2025/first/John Doe/CS101")
                .name("CS101 - Introduction to Computer Science")
                .type(FolderType.COURSE)
                .owner(professor1)
                .academicYear(academicYear)
                .semester(semester)
                .course(course)
                .build();

        syllabusFolder = Folder.builder()
                .id(2L)
                .path("2024-2025/first/John Doe/CS101/Syllabus")
                .name("Syllabus")
                .type(FolderType.SUBFOLDER)
                .parent(courseFolder)
                .owner(professor1)
                .academicYear(academicYear)
                .semester(semester)
                .course(course)
                .build();

        // Setup document submission
        submission = new DocumentSubmission();
        submission.setId(1L);
        submission.setCourseAssignment(assignment);
        submission.setProfessor(professor1);
        submission.setDocumentType(DocumentTypeEnum.SYLLABUS);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus(com.alquds.edu.ArchiveSystem.entity.submission.SubmissionStatus.UPLOADED);
        submission.setFileCount(1);
        submission.setUploadedFiles(new ArrayList<>());

        // Setup uploaded file
        uploadedFile = UploadedFile.builder()
                .id(1L)
                .originalFilename("syllabus.pdf")
                .storedFilename("syllabus_123.pdf")
                .fileUrl("/uploads/2024-2025/first/John Doe/CS101/Syllabus/syllabus_123.pdf")
                .fileSize(1024L)
                .fileType("application/pdf")
                .uploader(professor1)
                .documentSubmission(submission)
                .folder(syllabusFolder)
                .createdAt(LocalDateTime.now())
                .build();

        submission.getUploadedFiles().add(uploadedFile);
    }

    // ==================== getRootNode Tests ====================

    @Test
    @DisplayName("Should get root node successfully for professor")
    void shouldGetRootNodeSuccessfullyForProfessor() {
        // Arrange
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(fileAccessService.hasAdminLevelAccess(professor1)).thenReturn(false);
        List<User> professors = new ArrayList<>();
        professors.add(professor1);
        professors.add(professor2);
        when(userRepository.findActiveProfessorsByDepartment(1L, Role.ROLE_PROFESSOR))
                .thenReturn(professors);

        // Act
        FileExplorerNode result = fileExplorerService.getRootNode(1L, 1L, professor1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(NodeType.SEMESTER);
        assertThat(result.getPath()).isEqualTo("/2024-2025/first");
        assertThat(result.getChildren()).hasSize(2); // Two professors in department
        assertThat(result.getChildren().get(0).getType()).isEqualTo(NodeType.PROFESSOR);
        verify(academicYearRepository).findById(1L);
        verify(semesterRepository).findById(1L);
    }

    @Test
    @DisplayName("Should get root node successfully for admin user")
    void shouldGetRootNodeSuccessfullyForAdminUser() {
        // Arrange
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(fileAccessService.hasAdminLevelAccess(adminUser)).thenReturn(true);
        List<User> professors = new ArrayList<>();
        professors.add(professor1);
        professors.add(professor2);
        when(userRepository.findByRole(Role.ROLE_PROFESSOR))
                .thenReturn(professors);

        // Act
        FileExplorerNode result = fileExplorerService.getRootNode(1L, 1L, adminUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(NodeType.SEMESTER);
        assertThat(result.getChildren()).hasSize(2);
        verify(fileAccessService).hasAdminLevelAccess(adminUser);
    }

    @Test
    @DisplayName("Should throw exception when academic year not found")
    void shouldThrowExceptionWhenAcademicYearNotFound() {
        // Arrange
        when(academicYearRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> fileExplorerService.getRootNode(1L, 1L, professor1))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Academic year not found");
    }

    @Test
    @DisplayName("Should throw exception when semester not found")
    void shouldThrowExceptionWhenSemesterNotFound() {
        // Arrange
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> fileExplorerService.getRootNode(1L, 1L, professor1))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Semester not found");
    }

    @Test
    @DisplayName("Should throw exception when semester does not belong to academic year")
    void shouldThrowExceptionWhenSemesterDoesNotBelongToAcademicYear() {
        // Arrange
        AcademicYear otherYear = TestDataBuilder.createAcademicYear();
        otherYear.setId(2L);
        otherYear.setYearCode("2025-2026");
        
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        semester.setAcademicYear(otherYear);

        // Act & Assert
        assertThatThrownBy(() -> fileExplorerService.getRootNode(1L, 1L, professor1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Semester does not belong to the specified academic year");
    }

    // ==================== getNode Tests ====================

    @Test
    @DisplayName("Should get node successfully for valid path")
    void shouldGetNodeSuccessfullyForValidPath() {
        // Arrange
        String nodePath = "/2024-2025/first/John Doe";
        when(fileAccessService.hasAdminLevelAccess(professor1)).thenReturn(false);
        when(fileAccessService.canAccessDepartmentFiles(professor1, 1L)).thenReturn(true);
        List<User> professors = new ArrayList<>();
        professors.add(professor1);
        when(userRepository.findByRole(Role.ROLE_PROFESSOR)).thenReturn(professors);
        when(userRepository.findByProfessorId("John Doe")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor1));
        when(academicYearRepository.findByYearCode("2024-2025")).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findByAcademicYearIdAndType(1L, SemesterType.FIRST))
                .thenReturn(Optional.of(semester));
        when(courseAssignmentRepository.findByProfessorIdAndSemesterId(1L, 1L))
                .thenReturn(new ArrayList<>());

        // Act
        FileExplorerNode result = fileExplorerService.getNode(nodePath, professor1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(NodeType.PROFESSOR);
        assertThat(result.getPath()).isEqualTo(nodePath);
    }

    @Test
    @DisplayName("Should throw exception when user does not have read permission")
    void shouldThrowExceptionWhenUserDoesNotHaveReadPermission() {
        // Arrange
        String nodePath = "/2024-2025/first/John Doe";
        when(fileAccessService.hasAdminLevelAccess(professor2)).thenReturn(false);
        when(fileAccessService.canAccessDepartmentFiles(professor2, 1L)).thenReturn(false);
        List<User> professors = new ArrayList<>();
        professors.add(professor1);
        when(userRepository.findByRole(Role.ROLE_PROFESSOR)).thenReturn(professors);
        when(userRepository.findByProfessorId("John Doe")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor1));

        // Act & Assert
        assertThatThrownBy(() -> fileExplorerService.getNode(nodePath, professor2))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("does not have permission");
    }

    // ==================== getChildren Tests ====================

    @Test
    @DisplayName("Should get children successfully for semester node")
    void shouldGetChildrenSuccessfullyForSemesterNode() {
        // Arrange
        String parentPath = "/2024-2025/first";
        when(fileAccessService.hasAdminLevelAccess(professor1)).thenReturn(false);
        when(fileAccessService.canAccessDepartmentFiles(professor1, 1L)).thenReturn(true);
        when(academicYearRepository.findByYearCode("2024-2025")).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findByAcademicYearIdAndType(1L, SemesterType.FIRST))
                .thenReturn(Optional.of(semester));
        List<User> professors = new ArrayList<>();
        professors.add(professor1);
        professors.add(professor2);
        when(userRepository.findActiveProfessorsByDepartment(1L, Role.ROLE_PROFESSOR))
                .thenReturn(professors);
        when(userRepository.findByRole(Role.ROLE_PROFESSOR)).thenReturn(professors);
        when(userRepository.findByProfessorId(anyString())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(professor2));

        // Act
        List<FileExplorerNode> result = fileExplorerService.getChildren(parentPath, professor1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getType()).isEqualTo(NodeType.PROFESSOR);
    }

    @Test
    @DisplayName("Should get course children for professor node")
    void shouldGetCourseChildrenForProfessorNode() {
        // Arrange
        String parentPath = "/2024-2025/first/John Doe";
        when(fileAccessService.hasAdminLevelAccess(professor1)).thenReturn(false);
        when(fileAccessService.canAccessDepartmentFiles(professor1, 1L)).thenReturn(true);
        List<User> professors = new ArrayList<>();
        professors.add(professor1);
        when(userRepository.findByRole(Role.ROLE_PROFESSOR)).thenReturn(professors);
        when(userRepository.findByProfessorId("John Doe")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor1));
        when(academicYearRepository.findByYearCode("2024-2025")).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findByAcademicYearIdAndType(1L, SemesterType.FIRST))
                .thenReturn(Optional.of(semester));
        List<CourseAssignment> assignments = new ArrayList<>();
        assignments.add(assignment);
        when(courseAssignmentRepository.findByProfessorIdAndSemesterId(1L, 1L))
                .thenReturn(assignments);

        // Act
        List<FileExplorerNode> result = fileExplorerService.getChildren(parentPath, professor1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getType()).isEqualTo(NodeType.COURSE);
        assertThat(result.get(0).getName()).contains("CS101");
    }

    @Test
    @DisplayName("Should get document type children for course node")
    void shouldGetDocumentTypeChildrenForCourseNode() {
        // Arrange
        String parentPath = "/2024-2025/first/John Doe/CS101";
        when(fileAccessService.hasAdminLevelAccess(professor1)).thenReturn(false);
        when(fileAccessService.canAccessDepartmentFiles(professor1, 1L)).thenReturn(true);
        List<User> professors = new ArrayList<>();
        professors.add(professor1);
        when(userRepository.findByRole(Role.ROLE_PROFESSOR)).thenReturn(professors);
        when(userRepository.findByProfessorId("John Doe")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor1));
        when(academicYearRepository.findByYearCode("2024-2025")).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findByAcademicYearIdAndType(1L, SemesterType.FIRST))
                .thenReturn(Optional.of(semester));
        when(courseAssignmentRepository.findBySemesterIdAndCourseCodeAndProfessorId(1L, "CS101", 1L))
                .thenReturn(Optional.of(assignment));
        when(folderRepository.findCourseFolder(1L, 1L, 1L, 1L, FolderType.COURSE))
                .thenReturn(Optional.of(courseFolder));
        List<Folder> subfolders = new ArrayList<>();
        subfolders.add(syllabusFolder);
        when(folderRepository.findByParentId(1L)).thenReturn(subfolders);
        List<DocumentSubmission> submissions = new ArrayList<>();
        submissions.add(submission);
        when(documentSubmissionRepository.findByCourseAssignmentId(1L))
                .thenReturn(submissions);

        // Act
        List<FileExplorerNode> result = fileExplorerService.getChildren(parentPath, professor1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getType()).isEqualTo(NodeType.DOCUMENT_TYPE);
    }

    @Test
    @DisplayName("Should get file children for document type node")
    void shouldGetFileChildrenForDocumentTypeNode() {
        // Arrange
        String parentPath = "/2024-2025/first/John Doe/CS101/syllabus";
        when(fileAccessService.hasAdminLevelAccess(professor1)).thenReturn(false);
        when(fileAccessService.canAccessDepartmentFiles(professor1, 1L)).thenReturn(true);
        List<User> professors = new ArrayList<>();
        professors.add(professor1);
        when(userRepository.findByRole(Role.ROLE_PROFESSOR)).thenReturn(professors);
        when(userRepository.findByProfessorId("John Doe")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor1));
        when(academicYearRepository.findByYearCode("2024-2025")).thenReturn(Optional.of(academicYear));
        when(semesterRepository.findByAcademicYearIdAndType(1L, SemesterType.FIRST))
                .thenReturn(Optional.of(semester));
        when(courseAssignmentRepository.findBySemesterIdAndCourseCodeAndProfessorId(1L, "CS101", 1L))
                .thenReturn(Optional.of(assignment));
        when(documentSubmissionRepository.findByCourseAssignmentIdAndDocumentType(1L, DocumentTypeEnum.SYLLABUS))
                .thenReturn(Optional.of(submission));
        List<UploadedFile> files = new ArrayList<>();
        files.add(uploadedFile);
        when(uploadedFileRepository.findByDocumentSubmissionIdWithUploaderOrderByFileOrderAsc(1L))
                .thenReturn(files);

        // Act
        List<FileExplorerNode> result = fileExplorerService.getChildren(parentPath, professor1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getType()).isEqualTo(NodeType.FILE);
        assertThat(result.get(0).getName()).isEqualTo("syllabus.pdf");
    }

    @Test
    @DisplayName("Should throw exception when user does not have permission to get children")
    void shouldThrowExceptionWhenUserDoesNotHavePermissionToGetChildren() {
        // Arrange
        String parentPath = "/2024-2025/first/John Doe";
        when(fileAccessService.hasAdminLevelAccess(professor2)).thenReturn(false);
        when(fileAccessService.canAccessDepartmentFiles(professor2, 1L)).thenReturn(false);
        List<User> professors = new ArrayList<>();
        professors.add(professor1);
        when(userRepository.findByRole(Role.ROLE_PROFESSOR)).thenReturn(professors);
        when(userRepository.findByProfessorId("John Doe")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor1));

        // Act & Assert
        assertThatThrownBy(() -> fileExplorerService.getChildren(parentPath, professor2))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("does not have permission");
        
        // Verify canRead was called but getChildren logic was not executed
        verify(fileAccessService).canAccessDepartmentFiles(professor2, 1L);
    }

    // ==================== canRead Tests ====================

    @Test
    @DisplayName("Should return true when admin user can read any path")
    void shouldReturnTrueWhenAdminUserCanReadAnyPath() {
        // Arrange
        String nodePath = "/2024-2025/first/John Doe";
        when(fileAccessService.hasAdminLevelAccess(adminUser)).thenReturn(true);

        // Act
        boolean result = fileExplorerService.canRead(nodePath, adminUser);

        // Assert
        assertThat(result).isTrue();
        verify(fileAccessService).hasAdminLevelAccess(adminUser);
    }

    @Test
    @DisplayName("Should return true when professor can read same department path")
    void shouldReturnTrueWhenProfessorCanReadSameDepartmentPath() {
        // Arrange
        String nodePath = "/2024-2025/first/John Doe";
        when(fileAccessService.hasAdminLevelAccess(professor1)).thenReturn(false);
        List<User> professors = new ArrayList<>();
        professors.add(professor1);
        when(userRepository.findByRole(Role.ROLE_PROFESSOR)).thenReturn(professors);
        when(userRepository.findByProfessorId("John Doe")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor1));
        when(fileAccessService.canAccessDepartmentFiles(professor1, 1L)).thenReturn(true);

        // Act
        boolean result = fileExplorerService.canRead(nodePath, professor1);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when professor cannot read different department path")
    void shouldReturnFalseWhenProfessorCannotReadDifferentDepartmentPath() {
        // Arrange
        Department otherDept = TestDataBuilder.createDepartment();
        otherDept.setId(2L);
        otherDept.setName("Mathematics");
        professor1.setDepartment(otherDept);

        String nodePath = "/2024-2025/first/John Doe";
        when(fileAccessService.hasAdminLevelAccess(professor1)).thenReturn(false);
        List<User> professors = new ArrayList<>();
        professors.add(professor1);
        when(userRepository.findByRole(Role.ROLE_PROFESSOR)).thenReturn(professors);
        when(userRepository.findByProfessorId("John Doe")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor1));
        when(fileAccessService.canAccessDepartmentFiles(professor1, 1L)).thenReturn(false);

        // Act
        boolean result = fileExplorerService.canRead(nodePath, professor1);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true for semester level path (all authenticated users)")
    void shouldReturnTrueForSemesterLevelPath() {
        // Arrange
        String nodePath = "/2024-2025/first";
        when(fileAccessService.hasAdminLevelAccess(professor1)).thenReturn(false);

        // Act
        boolean result = fileExplorerService.canRead(nodePath, professor1);

        // Assert
        assertThat(result).isTrue();
    }

    // ==================== canWrite Tests ====================

    @Test
    @DisplayName("Should return true when professor can write to own course")
    void shouldReturnTrueWhenProfessorCanWriteToOwnCourse() {
        // Arrange
        String nodePath = "/2024-2025/first/John Doe/CS101";
        List<User> professors = new ArrayList<>();
        professors.add(professor1);
        when(userRepository.findByRole(Role.ROLE_PROFESSOR)).thenReturn(professors);
        when(userRepository.findByProfessorId("John Doe")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor1));

        // Act
        boolean result = fileExplorerService.canWrite(nodePath, professor1);

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).findByRole(Role.ROLE_PROFESSOR);
    }

    @Test
    @DisplayName("Should return false when professor cannot write to other professor's course")
    void shouldReturnFalseWhenProfessorCannotWriteToOtherProfessorsCourse() {
        // Arrange
        String nodePath = "/2024-2025/first/Jane Smith/CS101";
        List<User> professors = new ArrayList<>();
        professors.add(professor1);
        professors.add(professor2);
        when(userRepository.findByRole(Role.ROLE_PROFESSOR)).thenReturn(professors);
        when(userRepository.findByProfessorId("Jane Smith")).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(professor2));

        // Act
        boolean result = fileExplorerService.canWrite(nodePath, professor1);

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).findByRole(Role.ROLE_PROFESSOR);
    }

    @Test
    @DisplayName("Should return false when non-professor tries to write")
    void shouldReturnFalseWhenNonProfessorTriesToWrite() {
        // Arrange
        String nodePath = "/2024-2025/first/John Doe/CS101";

        // Act
        boolean result = fileExplorerService.canWrite(nodePath, hodUser);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when path is not at course or document type level")
    void shouldReturnFalseWhenPathIsNotAtCourseOrDocumentTypeLevel() {
        // Arrange
        String nodePath = "/2024-2025/first/John Doe";

        // Act
        boolean result = fileExplorerService.canWrite(nodePath, professor1);

        // Assert
        assertThat(result).isFalse();
    }

    // ==================== canDelete Tests ====================

    @Test
    @DisplayName("Should return true when professor can delete own file")
    void shouldReturnTrueWhenProfessorCanDeleteOwnFile() {
        // Arrange
        // Path format: /year/semester/professor/course/documentType/fileId
        // This should parse to FILE type (6 parts)
        String nodePath = "/2024-2025/first/John Doe/CS101/syllabus/1";
        List<User> professors = new ArrayList<>();
        professors.add(professor1);
        when(userRepository.findByRole(Role.ROLE_PROFESSOR)).thenReturn(professors);
        when(userRepository.findByProfessorId("John Doe")).thenReturn(Optional.empty());
        // The service will iterate through professors and match by generated folder name
        // Since professor1 has firstName="John" and lastName="Doe", folder name will be "John Doe"
        when(userRepository.findById(1L)).thenReturn(Optional.of(professor1));

        // Act
        boolean result = fileExplorerService.canDelete(nodePath, professor1);

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).findByRole(Role.ROLE_PROFESSOR);
    }

    @Test
    @DisplayName("Should return false when professor cannot delete other professor's file")
    void shouldReturnFalseWhenProfessorCannotDeleteOtherProfessorsFile() {
        // Arrange
        String nodePath = "/2024-2025/first/Jane Smith/CS101/syllabus/1";
        List<User> professors = new ArrayList<>();
        professors.add(professor1);
        professors.add(professor2);
        when(userRepository.findByRole(Role.ROLE_PROFESSOR)).thenReturn(professors);
        when(userRepository.findByProfessorId("Jane Smith")).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(professor2));

        // Act
        boolean result = fileExplorerService.canDelete(nodePath, professor1);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when non-professor tries to delete")
    void shouldReturnFalseWhenNonProfessorTriesToDelete() {
        // Arrange
        String nodePath = "/2024-2025/first/John Doe/CS101/syllabus/1";

        // Act
        boolean result = fileExplorerService.canDelete(nodePath, hodUser);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when path is not a file")
    void shouldReturnFalseWhenPathIsNotAFile() {
        // Arrange
        String nodePath = "/2024-2025/first/John Doe/CS101";

        // Act
        boolean result = fileExplorerService.canDelete(nodePath, professor1);

        // Assert
        assertThat(result).isFalse();
    }
}
