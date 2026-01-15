package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.dto.fileexplorer.FileMetadataDTO;
import com.alquds.edu.ArchiveSystem.entity.academic.Department;
import com.alquds.edu.ArchiveSystem.entity.file.Folder;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.repository.file.UploadedFileRepository;
import com.alquds.edu.ArchiveSystem.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FilePreviewService Unit Tests")
class FilePreviewServiceTest {

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @Mock
    private FileExplorerService fileExplorerService;

    @Mock
    private OfficeDocumentConverter officeDocumentConverter;

    @InjectMocks
    private FilePreviewServiceImpl filePreviewService;

    @TempDir
    Path tempDir;

    private User professor1;
    private User professor2;
    private User hodUser;
    private User deanshipUser;
    private User adminUser;
    private Department department1;
    private Department department2;
    private UploadedFile pdfFile;
    private UploadedFile imageFile;
    private UploadedFile textFile;
    private UploadedFile officeFile;
    private UploadedFile unsupportedFile;
    private Folder testFolder;

    @BeforeEach
    void setUp() throws IOException {
        // Set upload directory to temp directory using reflection
        ReflectionTestUtils.setField(filePreviewService, "uploadDirectory", tempDir.toString());

        // Setup departments
        department1 = TestDataBuilder.createDepartment();
        department1.setId(1L);
        department1.setName("Computer Science");

        department2 = TestDataBuilder.createDepartment();
        department2.setId(2L);
        department2.setName("Mathematics");

        // Setup professor1
        professor1 = TestDataBuilder.createProfessorUser();
        professor1.setId(1L);
        professor1.setEmail("prof1@staff.alquds.edu");
        professor1.setFirstName("John");
        professor1.setLastName("Doe");
        professor1.setDepartment(department1);
        professor1.setProfessorId("PROF1");

        // Setup professor2 (different department)
        professor2 = TestDataBuilder.createProfessorUser();
        professor2.setId(2L);
        professor2.setEmail("prof2@staff.alquds.edu");
        professor2.setFirstName("Jane");
        professor2.setLastName("Smith");
        professor2.setDepartment(department2);
        professor2.setProfessorId("PROF2");

        // Setup HOD user
        hodUser = TestDataBuilder.createHodUser();
        hodUser.setId(3L);
        hodUser.setEmail("hod@hod.alquds.edu");
        hodUser.setFirstName("HOD");
        hodUser.setLastName("User");
        hodUser.setDepartment(department1);

        // Setup deanship user
        deanshipUser = TestDataBuilder.createUser();
        deanshipUser.setId(4L);
        deanshipUser.setEmail("dean@deanship.alquds.edu");
        deanshipUser.setRole(Role.ROLE_DEANSHIP);
        deanshipUser.setDepartment(null);

        // Setup admin user
        adminUser = TestDataBuilder.createAdminUser();
        adminUser.setId(5L);
        adminUser.setEmail("admin@admin.alquds.edu");
        adminUser.setDepartment(null);

        // Setup folder
        testFolder = new Folder();
        testFolder.setId(1L);
        testFolder.setPath("2024-2025/first/John Doe/CS101/Syllabus");
        testFolder.setName("Syllabus");

        // Create test files in temp directory
        Path pdfPath = tempDir.resolve("test.pdf");
        Files.write(pdfPath, "PDF content".getBytes());

        Path imagePath = tempDir.resolve("test.png");
        Files.write(imagePath, "PNG content".getBytes());

        Path textPath = tempDir.resolve("test.txt");
        Files.write(textPath, "Line 1\nLine 2\nLine 3\nLine 4\nLine 5".getBytes());

        Path officePath = tempDir.resolve("test.docx");
        Files.write(officePath, "DOCX content".getBytes());

        Path unsupportedPath = tempDir.resolve("test.bin");
        Files.write(unsupportedPath, "Binary content".getBytes());

        // Setup PDF file
        pdfFile = UploadedFile.builder()
                .id(1L)
                .originalFilename("test.pdf")
                .storedFilename("test.pdf")
                .fileUrl("test.pdf")
                .fileSize(1024L)
                .fileType("application/pdf")
                .uploader(professor1)
                .folder(testFolder)
                .createdAt(LocalDateTime.now())
                .build();

        // Setup image file
        imageFile = UploadedFile.builder()
                .id(2L)
                .originalFilename("test.png")
                .storedFilename("test.png")
                .fileUrl("test.png")
                .fileSize(2048L)
                .fileType("image/png")
                .uploader(professor1)
                .folder(testFolder)
                .createdAt(LocalDateTime.now())
                .build();

        // Setup text file
        textFile = UploadedFile.builder()
                .id(3L)
                .originalFilename("test.txt")
                .storedFilename("test.txt")
                .fileUrl("test.txt")
                .fileSize(512L)
                .fileType("text/plain")
                .uploader(professor1)
                .folder(testFolder)
                .createdAt(LocalDateTime.now())
                .build();

        // Setup Office file
        officeFile = UploadedFile.builder()
                .id(4L)
                .originalFilename("test.docx")
                .storedFilename("test.docx")
                .fileUrl("test.docx")
                .fileSize(3072L)
                .fileType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .uploader(professor1)
                .folder(testFolder)
                .createdAt(LocalDateTime.now())
                .build();

        // Setup unsupported file
        unsupportedFile = UploadedFile.builder()
                .id(5L)
                .originalFilename("test.bin")
                .storedFilename("test.bin")
                .fileUrl("test.bin")
                .fileSize(4096L)
                .fileType("application/octet-stream")
                .uploader(professor1)
                .folder(testFolder)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== getFileMetadata Tests ====================

    @Test
    @DisplayName("Should get file metadata for PDF file")
    void shouldGetFileMetadataForPdfFile() {
        // Arrange
        when(uploadedFileRepository.findById(1L)).thenReturn(Optional.of(pdfFile));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(1L))
                .thenReturn(Optional.of(pdfFile));

        // Act
        FileMetadataDTO metadata = filePreviewService.getFileMetadata(1L, professor1);

        // Assert
        assertThat(metadata).isNotNull();
        assertThat(metadata.getId()).isEqualTo(1L);
        assertThat(metadata.getFileName()).isEqualTo("test.pdf");
        assertThat(metadata.getMimeType()).isEqualTo("application/pdf");
        assertThat(metadata.getFileSize()).isEqualTo(1024L);
        assertThat(metadata.isPreviewable()).isTrue();
        assertThat(metadata.getPreviewType()).isEqualTo("pdf");
        verify(uploadedFileRepository).findById(1L);
    }

    @Test
    @DisplayName("Should get file metadata for image file")
    void shouldGetFileMetadataForImageFile() {
        // Arrange
        when(uploadedFileRepository.findById(2L)).thenReturn(Optional.of(imageFile));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(2L))
                .thenReturn(Optional.of(imageFile));

        // Act
        FileMetadataDTO metadata = filePreviewService.getFileMetadata(2L, professor1);

        // Assert
        assertThat(metadata).isNotNull();
        assertThat(metadata.getId()).isEqualTo(2L);
        assertThat(metadata.getFileName()).isEqualTo("test.png");
        assertThat(metadata.getMimeType()).isEqualTo("image/png");
        assertThat(metadata.isPreviewable()).isTrue();
        assertThat(metadata.getPreviewType()).isEqualTo("image");
        verify(uploadedFileRepository).findById(2L);
    }

    @Test
    @DisplayName("Should get file metadata for unsupported type")
    void shouldGetFileMetadataForUnsupportedType() {
        // Arrange
        when(uploadedFileRepository.findById(5L)).thenReturn(Optional.of(unsupportedFile));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(5L))
                .thenReturn(Optional.of(unsupportedFile));

        // Act
        FileMetadataDTO metadata = filePreviewService.getFileMetadata(5L, professor1);

        // Assert
        assertThat(metadata).isNotNull();
        assertThat(metadata.getId()).isEqualTo(5L);
        assertThat(metadata.getMimeType()).isEqualTo("application/octet-stream");
        assertThat(metadata.isPreviewable()).isFalse();
        assertThat(metadata.getPreviewType()).isEqualTo("unsupported");
        verify(uploadedFileRepository).findById(5L);
    }

    @Test
    @DisplayName("Should detect MIME type when not set in file")
    void shouldDetectMimeTypeWhenNotSet() {
        // Arrange
        UploadedFile fileWithoutMimeType = UploadedFile.builder()
                .id(6L)
                .originalFilename("test.pdf")
                .fileUrl("test.pdf")
                .fileType(null)
                .uploader(professor1)
                .folder(testFolder)
                .build();
        when(uploadedFileRepository.findById(6L)).thenReturn(Optional.of(fileWithoutMimeType));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(6L))
                .thenReturn(Optional.of(fileWithoutMimeType));

        // Act
        FileMetadataDTO metadata = filePreviewService.getFileMetadata(6L, professor1);

        // Assert
        assertThat(metadata).isNotNull();
        assertThat(metadata.getMimeType()).isNotNull();
        verify(uploadedFileRepository).findById(6L);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when file not found")
    void shouldThrowExceptionWhenFileNotFound() {
        // Arrange
        when(uploadedFileRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> filePreviewService.getFileMetadata(999L, professor1))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("File not found");
        verify(uploadedFileRepository).findById(999L);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when user lacks permission")
    void shouldThrowAccessDeniedExceptionWhenUserLacksPermission() {
        // Arrange
        when(uploadedFileRepository.findById(1L)).thenReturn(Optional.of(pdfFile));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(1L))
                .thenReturn(Optional.of(pdfFile));
        when(fileExplorerService.canRead(anyString(), any(User.class))).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> filePreviewService.getFileMetadata(1L, professor2))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");
    }

    // ==================== getFilePreview Tests ====================

    @Test
    @DisplayName("Should get file preview for PDF file")
    void shouldGetFilePreviewForPdfFile() throws IOException {
        // Arrange
        when(uploadedFileRepository.findById(1L)).thenReturn(Optional.of(pdfFile));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(1L))
                .thenReturn(Optional.of(pdfFile));

        // Act
        byte[] preview = filePreviewService.getFilePreview(1L, professor1);

        // Assert
        assertThat(preview).isNotNull();
        assertThat(preview).isEqualTo("PDF content".getBytes());
        verify(uploadedFileRepository).findById(1L);
    }

    @Test
    @DisplayName("Should get file preview for image file")
    void shouldGetFilePreviewForImageFile() throws IOException {
        // Arrange
        when(uploadedFileRepository.findById(2L)).thenReturn(Optional.of(imageFile));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(2L))
                .thenReturn(Optional.of(imageFile));

        // Act
        byte[] preview = filePreviewService.getFilePreview(2L, professor1);

        // Assert
        assertThat(preview).isNotNull();
        assertThat(preview).isEqualTo("PNG content".getBytes());
        verify(uploadedFileRepository).findById(2L);
    }

    @Test
    @DisplayName("Should throw RuntimeException when file cannot be read")
    void shouldThrowRuntimeExceptionWhenFileCannotBeRead() {
        // Arrange
        UploadedFile invalidFile = UploadedFile.builder()
                .id(7L)
                .fileUrl("nonexistent.pdf")
                .uploader(professor1)
                .build();
        when(uploadedFileRepository.findById(7L)).thenReturn(Optional.of(invalidFile));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(7L))
                .thenReturn(Optional.of(invalidFile));

        // Act & Assert
        assertThatThrownBy(() -> filePreviewService.getFilePreview(7L, professor1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to read file preview");
    }

    // ==================== getFileContent Tests ====================

    @Test
    @DisplayName("Should get file content for text file")
    void shouldGetFileContentForTextFile() {
        // Arrange
        when(uploadedFileRepository.findById(3L)).thenReturn(Optional.of(textFile));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(3L))
                .thenReturn(Optional.of(textFile));

        // Act
        String content = filePreviewService.getFileContent(3L, professor1);

        // Assert
        assertThat(content).isNotNull();
        assertThat(content).contains("Line 1");
        assertThat(content).contains("Line 5");
        verify(uploadedFileRepository).findById(3L);
    }

    @Test
    @DisplayName("Should throw RuntimeException when text file cannot be read")
    void shouldThrowRuntimeExceptionWhenTextFileCannotBeRead() {
        // Arrange
        UploadedFile invalidFile = UploadedFile.builder()
                .id(8L)
                .fileUrl("nonexistent.txt")
                .uploader(professor1)
                .build();
        when(uploadedFileRepository.findById(8L)).thenReturn(Optional.of(invalidFile));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(8L))
                .thenReturn(Optional.of(invalidFile));

        // Act & Assert
        assertThatThrownBy(() -> filePreviewService.getFileContent(8L, professor1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to read file content");
    }

    // ==================== getPartialFileContent Tests ====================

    @Test
    @DisplayName("Should get partial file content with line limit")
    void shouldGetPartialFileContentWithLineLimit() {
        // Arrange
        when(uploadedFileRepository.findById(3L)).thenReturn(Optional.of(textFile));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(3L))
                .thenReturn(Optional.of(textFile));

        // Act
        String content = filePreviewService.getPartialFileContent(3L, professor1, 3);

        // Assert
        assertThat(content).isNotNull();
        assertThat(content).contains("Line 1");
        assertThat(content).contains("Line 3");
        assertThat(content).doesNotContain("Line 4");
        assertThat(content).doesNotContain("Line 5");
        verify(uploadedFileRepository).findById(3L);
    }

    @Test
    @DisplayName("Should get all lines when maxLines exceeds file lines")
    void shouldGetAllLinesWhenMaxLinesExceedsFileLines() {
        // Arrange
        when(uploadedFileRepository.findById(3L)).thenReturn(Optional.of(textFile));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(3L))
                .thenReturn(Optional.of(textFile));

        // Act
        String content = filePreviewService.getPartialFileContent(3L, professor1, 10);

        // Assert
        assertThat(content).isNotNull();
        assertThat(content).contains("Line 1");
        assertThat(content).contains("Line 5");
        verify(uploadedFileRepository).findById(3L);
    }

    // ==================== isPreviewable Tests ====================

    @Test
    @DisplayName("Should return true for PDF MIME type")
    void shouldReturnTrueForPdfMimeType() {
        // Act
        boolean result = filePreviewService.isPreviewable("application/pdf");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true for image MIME type")
    void shouldReturnTrueForImageMimeType() {
        // Act
        boolean result = filePreviewService.isPreviewable("image/png");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true for text MIME type")
    void shouldReturnTrueForTextMimeType() {
        // Act
        boolean result = filePreviewService.isPreviewable("text/plain");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true for Office document MIME type")
    void shouldReturnTrueForOfficeDocumentMimeType() {
        // Act
        boolean result = filePreviewService.isPreviewable(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for unsupported MIME type")
    void shouldReturnFalseForUnsupportedMimeType() {
        // Act
        boolean result = filePreviewService.isPreviewable("application/octet-stream");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false for null MIME type")
    void shouldReturnFalseForNullMimeType() {
        // Act
        boolean result = filePreviewService.isPreviewable(null);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false for empty MIME type")
    void shouldReturnFalseForEmptyMimeType() {
        // Act
        boolean result = filePreviewService.isPreviewable("");

        // Assert
        assertThat(result).isFalse();
    }

    // ==================== canUserPreviewFile Tests ====================

    @Test
    @DisplayName("Should return true when deanship user previews file")
    void shouldReturnTrueWhenDeanshipUserPreviewsFile() {
        // Arrange
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(1L))
                .thenReturn(Optional.of(pdfFile));

        // Act
        boolean result = filePreviewService.canUserPreviewFile(1L, deanshipUser);

        // Assert
        assertThat(result).isTrue();
        verify(uploadedFileRepository).findByIdWithUploaderAndFolder(1L);
    }

    @Test
    @DisplayName("Should return true when user is the uploader")
    void shouldReturnTrueWhenUserIsTheUploader() {
        // Arrange
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(1L))
                .thenReturn(Optional.of(pdfFile));

        // Act
        boolean result = filePreviewService.canUserPreviewFile(1L, professor1);

        // Assert
        assertThat(result).isTrue();
        verify(uploadedFileRepository).findByIdWithUploaderAndFolder(1L);
    }

    @Test
    @DisplayName("Should return true when HOD accesses department file")
    void shouldReturnTrueWhenHodAccessesDepartmentFile() {
        // Arrange
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(1L))
                .thenReturn(Optional.of(pdfFile));
        when(fileExplorerService.canRead(anyString(), any(User.class))).thenReturn(true);

        // Act
        boolean result = filePreviewService.canUserPreviewFile(1L, hodUser);

        // Assert
        assertThat(result).isTrue();
        verify(uploadedFileRepository).findByIdWithUploaderAndFolder(1L);
    }

    @Test
    @DisplayName("Should return false when professor from different department tries to access")
    void shouldReturnFalseWhenProfessorFromDifferentDepartmentTriesToAccess() {
        // Arrange
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(1L))
                .thenReturn(Optional.of(pdfFile));
        when(fileExplorerService.canRead(anyString(), any(User.class))).thenReturn(false);

        // Act
        boolean result = filePreviewService.canUserPreviewFile(1L, professor2);

        // Assert
        assertThat(result).isFalse();
        verify(uploadedFileRepository).findByIdWithUploaderAndFolder(1L);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when file not found")
    void shouldThrowEntityNotFoundExceptionWhenFileNotFoundForPreview() {
        // Arrange
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> filePreviewService.canUserPreviewFile(999L, professor1))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("File not found");
    }

    // ==================== detectMimeType Tests ====================

    @Test
    @DisplayName("Should detect PDF MIME type from extension")
    void shouldDetectPdfMimeTypeFromExtension() {
        // Act
        String mimeType = filePreviewService.detectMimeType("document.pdf");

        // Assert
        assertThat(mimeType).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Should detect image MIME type from extension")
    void shouldDetectImageMimeTypeFromExtension() {
        // Act
        String mimeType = filePreviewService.detectMimeType("image.png");

        // Assert
        assertThat(mimeType).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Should detect text MIME type from extension")
    void shouldDetectTextMimeTypeFromExtension() {
        // Act
        String mimeType = filePreviewService.detectMimeType("readme.txt");

        // Assert
        assertThat(mimeType).isEqualTo("text/plain");
    }

    @Test
    @DisplayName("Should return octet-stream for unknown extension")
    void shouldReturnOctetStreamForUnknownExtension() {
        // Act
        String mimeType = filePreviewService.detectMimeType("file.unknown");

        // Assert
        assertThat(mimeType).isEqualTo("application/octet-stream");
    }

    @Test
    @DisplayName("Should handle file path without extension")
    void shouldHandleFilePathWithoutExtension() {
        // Act
        String mimeType = filePreviewService.detectMimeType("file_without_extension");

        // Assert
        assertThat(mimeType).isEqualTo("application/octet-stream");
    }

    // ==================== getPreviewType Tests ====================

    @Test
    @DisplayName("Should return pdf for PDF MIME type")
    void shouldReturnPdfForPdfMimeType() {
        // Act
        String previewType = filePreviewService.getPreviewType("application/pdf");

        // Assert
        assertThat(previewType).isEqualTo("pdf");
    }

    @Test
    @DisplayName("Should return image for image MIME type")
    void shouldReturnImageForImageMimeType() {
        // Act
        String previewType = filePreviewService.getPreviewType("image/jpeg");

        // Assert
        assertThat(previewType).isEqualTo("image");
    }

    @Test
    @DisplayName("Should return office for Office document MIME type")
    void shouldReturnOfficeForOfficeDocumentMimeType() {
        // Act
        String previewType = filePreviewService.getPreviewType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        // Assert
        assertThat(previewType).isEqualTo("office");
    }

    @Test
    @DisplayName("Should return text for text MIME type")
    void shouldReturnTextForTextMimeType() {
        // Act
        String previewType = filePreviewService.getPreviewType("text/plain");

        // Assert
        assertThat(previewType).isEqualTo("text");
    }

    @Test
    @DisplayName("Should return code for code MIME type")
    void shouldReturnCodeForCodeMimeType() {
        // Act
        String previewType = filePreviewService.getPreviewType("text/javascript");

        // Assert
        assertThat(previewType).isEqualTo("code");
    }

    @Test
    @DisplayName("Should return unsupported for unknown MIME type")
    void shouldReturnUnsupportedForUnknownMimeType() {
        // Act
        String previewType = filePreviewService.getPreviewType("application/octet-stream");

        // Assert
        assertThat(previewType).isEqualTo("unsupported");
    }

    @Test
    @DisplayName("Should return unsupported for null MIME type")
    void shouldReturnUnsupportedForNullMimeType() {
        // Act
        String previewType = filePreviewService.getPreviewType(null);

        // Assert
        assertThat(previewType).isEqualTo("unsupported");
    }

    // ==================== convertOfficeDocumentToHtml Tests ====================

    @Test
    @DisplayName("Should convert Office document to HTML")
    void shouldConvertOfficeDocumentToHtml() throws IOException {
        // Arrange
        byte[] expectedHtml = "<html>Converted content</html>".getBytes();
        when(uploadedFileRepository.findById(4L)).thenReturn(Optional.of(officeFile));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(4L))
                .thenReturn(Optional.of(officeFile));
        when(officeDocumentConverter.convertToHtml(anyString(), anyString()))
                .thenReturn(expectedHtml);

        // Act
        byte[] result = filePreviewService.convertOfficeDocumentToHtml(4L, professor1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedHtml);
        verify(uploadedFileRepository).findById(4L);
        verify(officeDocumentConverter).convertToHtml(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for non-Office document")
    void shouldThrowIllegalArgumentExceptionForNonOfficeDocument() {
        // Arrange
        when(uploadedFileRepository.findById(1L)).thenReturn(Optional.of(pdfFile));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(1L))
                .thenReturn(Optional.of(pdfFile));

        // Act & Assert
        assertThatThrownBy(() -> filePreviewService.convertOfficeDocumentToHtml(1L, professor1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not an Office document");
    }

    @Test
    @DisplayName("Should throw RuntimeException when conversion fails")
    void shouldThrowRuntimeExceptionWhenConversionFails() throws IOException {
        // Arrange
        when(uploadedFileRepository.findById(4L)).thenReturn(Optional.of(officeFile));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(4L))
                .thenReturn(Optional.of(officeFile));
        when(officeDocumentConverter.convertToHtml(anyString(), anyString()))
                .thenThrow(new IOException("Conversion failed"));

        // Act & Assert
        assertThatThrownBy(() -> filePreviewService.convertOfficeDocumentToHtml(4L, professor1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to convert Office document");
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when user lacks permission for conversion")
    void shouldThrowAccessDeniedExceptionWhenUserLacksPermissionForConversion() {
        // Arrange
        when(uploadedFileRepository.findById(4L)).thenReturn(Optional.of(officeFile));
        when(uploadedFileRepository.findByIdWithUploaderAndFolder(4L))
                .thenReturn(Optional.of(officeFile));
        when(fileExplorerService.canRead(anyString(), any(User.class))).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> filePreviewService.convertOfficeDocumentToHtml(4L, professor2))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");
    }
}
