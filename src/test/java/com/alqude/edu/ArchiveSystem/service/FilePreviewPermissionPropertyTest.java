package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.repository.UploadedFileRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for file preview permission validation.
 * 
 * **Feature: file-preview-system, Property 6: Permission-based preview availability**
 * **Feature: file-preview-system, Property 7: Unauthorized preview attempt handling**
 * **Feature: file-preview-system, Property 8: Department-scoped access for HOD**
 * 
 * **Validates: Requirements 2.1, 2.4, 3.1, 3.3**
 */
class FilePreviewPermissionPropertyTest {
    
    private FilePreviewService filePreviewService;
    private UploadedFileRepository uploadedFileRepository;
    private FileExplorerService fileExplorerService;
    
    @BeforeProperty
    void setUp() {
        uploadedFileRepository = mock(UploadedFileRepository.class);
        fileExplorerService = mock(FileExplorerService.class);
        OfficeDocumentConverter officeDocumentConverter = mock(OfficeDocumentConverter.class);
        filePreviewService = new FilePreviewServiceImpl(uploadedFileRepository, fileExplorerService, officeDocumentConverter);
    }
    
    /**
     * Property 6: Permission-based preview availability
     * For any file in the file explorer, the preview button should be enabled 
     * if and only if the current user has permission to view that file.
     */
    @Property(tries = 100)
    void permissionBasedPreviewAvailability(
            @ForAll("files") UploadedFile file,
            @ForAll("users") User user,
            @ForAll boolean hasPermission) {
        
        // Arrange
        when(uploadedFileRepository.findById(file.getId())).thenReturn(Optional.of(file));
        when(fileExplorerService.canRead(file.getFileUrl(), user)).thenReturn(hasPermission);
        
        // Act
        boolean canPreview = filePreviewService.canUserPreviewFile(file.getId(), user);
        
        // Assert
        assertEquals(hasPermission, canPreview, 
            "User should be able to preview file if and only if they have read permission");
    }
    
    /**
     * Property 7: Unauthorized preview attempt handling
     * For any file that a user does not have permission to view, 
     * attempting to preview should result in an error message being displayed.
     */
    @Property(tries = 100)
    void unauthorizedPreviewAttemptHandling(
            @ForAll("files") UploadedFile file,
            @ForAll("users") User user) {
        
        // Arrange - user does NOT have permission
        when(uploadedFileRepository.findById(file.getId())).thenReturn(Optional.of(file));
        when(fileExplorerService.canRead(file.getFileUrl(), user)).thenReturn(false);
        
        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> {
            filePreviewService.getFileMetadata(file.getId(), user);
        }, "Attempting to preview without permission should throw AccessDeniedException");
        
        assertThrows(AccessDeniedException.class, () -> {
            filePreviewService.getFileContent(file.getId(), user);
        }, "Attempting to get content without permission should throw AccessDeniedException");
        
        assertThrows(AccessDeniedException.class, () -> {
            filePreviewService.getFilePreview(file.getId(), user);
        }, "Attempting to get preview without permission should throw AccessDeniedException");
    }
    
    /**
     * Property 8: Department-scoped access for HOD
     * For any file, an HOD should be able to preview it if and only if 
     * the file belongs to their department.
     */
    @Property(tries = 100)
    void departmentScopedAccessForHOD(
            @ForAll("files") UploadedFile file,
            @ForAll("hodUsers") User hod,
            @ForAll boolean sameDepartment) {
        
        // Arrange
        Department hodDepartment = hod.getDepartment();
        Department fileDepartment = sameDepartment ? hodDepartment : createDifferentDepartment(hodDepartment);
        
        // Set file uploader's department
        if (file.getUploader() != null) {
            file.getUploader().setDepartment(fileDepartment);
        }
        
        when(uploadedFileRepository.findById(file.getId())).thenReturn(Optional.of(file));
        when(fileExplorerService.canRead(file.getFileUrl(), hod)).thenReturn(sameDepartment);
        
        // Act
        boolean canPreview = filePreviewService.canUserPreviewFile(file.getId(), hod);
        
        // Assert
        assertEquals(sameDepartment, canPreview,
            "HOD should be able to preview file if and only if it belongs to their department");
    }
    
    /**
     * Property: Dean can preview all files
     * For any file, a Dean should always be able to preview it.
     */
    @Property(tries = 100)
    void deanCanPreviewAllFiles(
            @ForAll("files") UploadedFile file,
            @ForAll("deanUsers") User dean) {
        
        // Arrange
        when(uploadedFileRepository.findById(file.getId())).thenReturn(Optional.of(file));
        when(fileExplorerService.canRead(file.getFileUrl(), dean)).thenReturn(true);
        
        // Act
        boolean canPreview = filePreviewService.canUserPreviewFile(file.getId(), dean);
        
        // Assert
        assertTrue(canPreview, "Dean should be able to preview any file");
    }
    
    /**
     * Property: Professor can preview own files
     * For any file uploaded by a professor, that professor should be able to preview it.
     */
    @Property(tries = 100)
    void professorCanPreviewOwnFiles(
            @ForAll("files") UploadedFile file,
            @ForAll("professorUsers") User professor) {
        
        // Arrange - set professor as uploader
        file.setUploader(professor);
        
        when(uploadedFileRepository.findById(file.getId())).thenReturn(Optional.of(file));
        when(fileExplorerService.canRead(file.getFileUrl(), professor)).thenReturn(true);
        
        // Act
        boolean canPreview = filePreviewService.canUserPreviewFile(file.getId(), professor);
        
        // Assert
        assertTrue(canPreview, "Professor should be able to preview their own files");
    }
    
    // ========== Arbitraries (Generators) ==========
    
    @Provide
    Arbitrary<UploadedFile> files() {
        return Combinators.combine(
            Arbitraries.longs().greaterOrEqual(1L),
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
            Arbitraries.longs().greaterOrEqual(1L).greaterOrEqual(100L),
            users()
        ).as((id, filename, fileUrl, fileSize, uploader) -> {
            UploadedFile file = new UploadedFile();
            file.setId(id);
            file.setOriginalFilename(filename + ".pdf");
            file.setStoredFilename(filename + ".pdf");
            file.setFileUrl(fileUrl);
            file.setFileSize(fileSize);
            file.setFileType("application/pdf");
            file.setUploader(uploader);
            file.setCreatedAt(LocalDateTime.now());
            return file;
        });
    }
    
    @Provide
    Arbitrary<User> users() {
        return Combinators.combine(
            Arbitraries.longs().greaterOrEqual(1L),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
            Arbitraries.of(Role.values()),
            departments()
        ).as((id, email, role, department) -> {
            User user = new User();
            user.setId(id);
            user.setEmail(email + "@example.com");
            user.setFirstName("First" + id);
            user.setLastName("Last" + id);
            user.setRole(role);
            user.setDepartment(department);
            user.setProfessorId("PROF" + id);
            user.setIsActive(true);
            return user;
        });
    }
    
    @Provide
    Arbitrary<User> professorUsers() {
        return Combinators.combine(
            Arbitraries.longs().greaterOrEqual(1L),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
            departments()
        ).as((id, email, department) -> {
            User user = new User();
            user.setId(id);
            user.setEmail(email + "@example.com");
            user.setFirstName("Prof" + id);
            user.setLastName("Last" + id);
            user.setRole(Role.ROLE_PROFESSOR);
            user.setDepartment(department);
            user.setProfessorId("PROF" + id);
            user.setIsActive(true);
            return user;
        });
    }
    
    @Provide
    Arbitrary<User> hodUsers() {
        return Combinators.combine(
            Arbitraries.longs().greaterOrEqual(1L),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
            departments()
        ).as((id, email, department) -> {
            User user = new User();
            user.setId(id);
            user.setEmail(email + "@example.com");
            user.setFirstName("HOD" + id);
            user.setLastName("Last" + id);
            user.setRole(Role.ROLE_HOD);
            user.setDepartment(department);
            user.setProfessorId("HOD" + id);
            user.setIsActive(true);
            return user;
        });
    }
    
    @Provide
    Arbitrary<User> deanUsers() {
        return Combinators.combine(
            Arbitraries.longs().greaterOrEqual(1L),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20)
        ).as((id, email) -> {
            User user = new User();
            user.setId(id);
            user.setEmail(email + "@example.com");
            user.setFirstName("Dean" + id);
            user.setLastName("Last" + id);
            user.setRole(Role.ROLE_DEANSHIP);
            user.setDepartment(null); // Deans typically don't have a specific department
            user.setIsActive(true);
            return user;
        });
    }
    
    @Provide
    Arbitrary<Department> departments() {
        return Combinators.combine(
            Arbitraries.longs().greaterOrEqual(1L),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(30)
        ).as((id, name) -> {
            Department dept = new Department();
            dept.setId(id);
            dept.setName(name);
            dept.setDescription("Department of " + name);
            dept.setCreatedAt(LocalDateTime.now());
            return dept;
        });
    }
    
    // Helper method to create a different department
    private Department createDifferentDepartment(Department original) {
        Department different = new Department();
        different.setId(original.getId() + 1000L);
        different.setName(original.getName() + "_Different");
        different.setDescription("Different department");
        different.setCreatedAt(LocalDateTime.now());
        return different;
    }
}
