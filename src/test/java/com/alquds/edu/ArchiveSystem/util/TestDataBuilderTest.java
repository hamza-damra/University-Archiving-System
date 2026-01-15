package com.alquds.edu.ArchiveSystem.util;

import com.alquds.edu.ArchiveSystem.entity.academic.AcademicYear;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.entity.academic.SemesterType;
import com.alquds.edu.ArchiveSystem.entity.auth.RefreshToken;
import com.alquds.edu.ArchiveSystem.entity.file.Folder;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.alquds.edu.ArchiveSystem.entity.submission.DocumentRequest;
import com.alquds.edu.ArchiveSystem.entity.user.Notification;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for TestDataBuilder utility methods.
 * Verifies that all builder methods create valid test data.
 */
@DisplayName("TestDataBuilder Tests")
class TestDataBuilderTest {

    @Test
    @DisplayName("Should create RefreshToken with default values")
    void testCreateRefreshToken() {
        RefreshToken token = TestDataBuilder.createRefreshToken();
        
        assertNotNull(token);
        assertNotNull(token.getToken());
        assertNotNull(token.getUser());
        assertNotNull(token.getExpiryDate());
        assertTrue(token.getExpiryDate().isAfter(Instant.now()));
        assertFalse(token.isRevoked());
        assertNotNull(token.getDeviceInfo());
    }

    @Test
    @DisplayName("Should create RefreshToken with custom user")
    void testCreateRefreshTokenWithUser() {
        User user = TestDataBuilder.createProfessorUser();
        RefreshToken token = TestDataBuilder.createRefreshToken(user);
        
        assertNotNull(token);
        assertEquals(user, token.getUser());
        assertTrue(token.isValid());
    }

    @Test
    @DisplayName("Should create AcademicYear with default values")
    void testCreateAcademicYear() {
        AcademicYear academicYear = TestDataBuilder.createAcademicYear();
        
        assertNotNull(academicYear);
        assertEquals("2024-2025", academicYear.getYearCode());
        assertEquals(2024, academicYear.getStartYear());
        assertEquals(2025, academicYear.getEndYear());
        assertTrue(academicYear.getIsActive());
    }

    @Test
    @DisplayName("Should create AcademicYear with custom year code")
    void testCreateAcademicYearWithYearCode() {
        AcademicYear academicYear = TestDataBuilder.createAcademicYear("2025-2026");
        
        assertNotNull(academicYear);
        assertEquals("2025-2026", academicYear.getYearCode());
        assertEquals(2025, academicYear.getStartYear());
        assertEquals(2026, academicYear.getEndYear());
        assertTrue(academicYear.getIsActive());
    }

    @Test
    @DisplayName("Should create Semester with default values")
    void testCreateSemester() {
        Semester semester = TestDataBuilder.createSemester();
        
        assertNotNull(semester);
        assertEquals(SemesterType.FIRST, semester.getType());
        assertNotNull(semester.getStartDate());
        assertNotNull(semester.getEndDate());
        assertTrue(semester.getIsActive());
    }

    @Test
    @DisplayName("Should create Semester with AcademicYear")
    void testCreateSemesterWithAcademicYear() {
        AcademicYear academicYear = TestDataBuilder.createAcademicYear();
        Semester semester = TestDataBuilder.createSemester(academicYear);
        
        assertNotNull(semester);
        assertEquals(academicYear, semester.getAcademicYear());
        assertEquals(SemesterType.FIRST, semester.getType());
    }

    @Test
    @DisplayName("Should create Semester with AcademicYear and SemesterType")
    void testCreateSemesterWithAcademicYearAndType() {
        AcademicYear academicYear = TestDataBuilder.createAcademicYear("2024-2025");
        Semester semester = TestDataBuilder.createSemester(academicYear, SemesterType.SECOND);
        
        assertNotNull(semester);
        assertEquals(academicYear, semester.getAcademicYear());
        assertEquals(SemesterType.SECOND, semester.getType());
        assertNotNull(semester.getStartDate());
        assertNotNull(semester.getEndDate());
    }

    @Test
    @DisplayName("Should create DocumentRequest with default values")
    @SuppressWarnings("deprecation")
    void testCreateDocumentRequest() {
        DocumentRequest request = TestDataBuilder.createDocumentRequest();
        
        assertNotNull(request);
        assertNotNull(request.getCourseName());
        assertNotNull(request.getDocumentType());
        assertNotNull(request.getRequiredFileExtensions());
        assertFalse(request.getRequiredFileExtensions().isEmpty());
        assertNotNull(request.getDeadline());
        assertTrue(request.getDeadline().isAfter(LocalDateTime.now()));
        assertNotNull(request.getProfessor());
        assertNotNull(request.getCreatedBy());
        assertNotNull(request.getDescription());
        assertNotNull(request.getMaxFileCount());
        assertNotNull(request.getMaxTotalSizeMb());
    }

    @Test
    @DisplayName("Should create DocumentRequest with custom professor and creator")
    @SuppressWarnings("deprecation")
    void testCreateDocumentRequestWithUsers() {
        User professor = TestDataBuilder.createProfessorUser();
        User creator = TestDataBuilder.createAdminUser();
        DocumentRequest request = TestDataBuilder.createDocumentRequest(professor, creator);
        
        assertNotNull(request);
        assertEquals(professor, request.getProfessor());
        assertEquals(creator, request.getCreatedBy());
    }

    @Test
    @DisplayName("Should create Notification with default values")
    void testCreateNotification() {
        Notification notification = TestDataBuilder.createNotification();
        
        assertNotNull(notification);
        assertNotNull(notification.getUser());
        assertNotNull(notification.getTitle());
        assertNotNull(notification.getMessage());
        assertNotNull(notification.getType());
        assertFalse(notification.getIsRead());
    }

    @Test
    @DisplayName("Should create Notification with custom user")
    void testCreateNotificationWithUser() {
        User user = TestDataBuilder.createHodUser();
        Notification notification = TestDataBuilder.createNotification(user);
        
        assertNotNull(notification);
        assertEquals(user, notification.getUser());
    }

    @Test
    @DisplayName("Should create Notification with custom user and type")
    void testCreateNotificationWithUserAndType() {
        User user = TestDataBuilder.createProfessorUser();
        Notification.NotificationType type = Notification.NotificationType.DEADLINE_APPROACHING;
        Notification notification = TestDataBuilder.createNotification(user, type);
        
        assertNotNull(notification);
        assertEquals(user, notification.getUser());
        assertEquals(type, notification.getType());
    }

    @Test
    @DisplayName("Should create Folder with default values")
    void testCreateFolder() {
        Folder folder = TestDataBuilder.createFolder();
        
        assertNotNull(folder);
        assertNotNull(folder.getPath());
        assertNotNull(folder.getName());
        assertNotNull(folder.getType());
        assertNotNull(folder.getOwner());
        assertNotNull(folder.getAcademicYear());
        assertNotNull(folder.getSemester());
    }

    @Test
    @DisplayName("Should create Folder with custom owner, academic year, and semester")
    void testCreateFolderWithParameters() {
        User owner = TestDataBuilder.createProfessorUser();
        AcademicYear academicYear = TestDataBuilder.createAcademicYear("2025-2026");
        Semester semester = TestDataBuilder.createSemester(academicYear, SemesterType.FIRST);
        Folder folder = TestDataBuilder.createFolder(owner, academicYear, semester);
        
        assertNotNull(folder);
        assertEquals(owner, folder.getOwner());
        assertEquals(academicYear, folder.getAcademicYear());
        assertEquals(semester, folder.getSemester());
        assertTrue(folder.getPath().contains("2025-2026"));
    }

    @Test
    @DisplayName("Should create UploadedFile with default values")
    void testCreateUploadedFile() {
        UploadedFile file = TestDataBuilder.createUploadedFile();
        
        assertNotNull(file);
        assertNotNull(file.getFolder());
        assertNotNull(file.getOriginalFilename());
        assertNotNull(file.getStoredFilename());
        assertNotNull(file.getFileUrl());
        assertNotNull(file.getFileSize());
        assertTrue(file.getFileSize() > 0);
        assertNotNull(file.getFileType());
        assertNotNull(file.getUploader());
    }

    @Test
    @DisplayName("Should create UploadedFile with custom folder and uploader")
    void testCreateUploadedFileWithFolderAndUploader() {
        Folder folder = TestDataBuilder.createFolder();
        User uploader = TestDataBuilder.createProfessorUser();
        UploadedFile file = TestDataBuilder.createUploadedFile(folder, uploader);
        
        assertNotNull(file);
        assertEquals(folder, file.getFolder());
        assertEquals(uploader, file.getUploader());
        assertTrue(file.getFileUrl().contains(folder.getPath()));
    }

    @Test
    @DisplayName("Should create UploadedFile with custom filename and file type")
    void testCreateUploadedFileWithFilenameAndType() {
        Folder folder = TestDataBuilder.createFolder();
        User uploader = TestDataBuilder.createProfessorUser();
        String filename = "syllabus.docx";
        String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        UploadedFile file = TestDataBuilder.createUploadedFile(folder, uploader, filename, fileType);
        
        assertNotNull(file);
        assertEquals(filename, file.getOriginalFilename());
        assertEquals(fileType, file.getFileType());
        assertTrue(file.getStoredFilename().contains("syllabus"));
    }
}
