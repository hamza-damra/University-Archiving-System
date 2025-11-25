package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.repository.UploadedFileRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for file type detection and renderer selection.
 * 
 * **Feature: file-preview-system, Property 3: Format-specific renderer selection**
 * 
 * **Validates: Requirements 1.3, 4.1, 4.2, 4.3, 4.4**
 */
class FilePreviewTypeDetectionPropertyTest {
    
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
     * Property 3: Format-specific renderer selection (PDF files)
     * For any PDF file, the system should select the PDF renderer.
     */
    @Property(tries = 100)
    void pdfFilesGetPdfRenderer(@ForAll("pdfMimeTypes") String mimeType) {
        // Act
        String previewType = filePreviewService.getPreviewType(mimeType);
        
        // Assert
        assertEquals("pdf", previewType, 
            "PDF MIME types should be assigned the 'pdf' preview type");
        assertTrue(filePreviewService.isPreviewable(mimeType),
            "PDF files should be previewable");
    }
    
    /**
     * Property 3: Format-specific renderer selection (Office files)
     * For any Office document file, the system should select the Office renderer.
     */
    @Property(tries = 100)
    void officeFilesGetOfficeRenderer(@ForAll("officeMimeTypes") String mimeType) {
        // Act
        String previewType = filePreviewService.getPreviewType(mimeType);
        
        // Assert
        assertEquals("office", previewType,
            "Office MIME types should be assigned the 'office' preview type");
        assertTrue(filePreviewService.isPreviewable(mimeType),
            "Office files should be previewable");
    }
    
    /**
     * Property 3: Format-specific renderer selection (Code files)
     * For any code file, the system should select the Code renderer.
     */
    @Property(tries = 100)
    void codeFilesGetCodeRenderer(@ForAll("codeMimeTypes") String mimeType) {
        // Act
        String previewType = filePreviewService.getPreviewType(mimeType);
        
        // Assert
        assertEquals("code", previewType,
            "Code MIME types should be assigned the 'code' preview type");
        assertTrue(filePreviewService.isPreviewable(mimeType),
            "Code files should be previewable");
    }
    
    /**
     * Property 3: Format-specific renderer selection (Text files)
     * For any text file, the system should select the Text renderer.
     */
    @Property(tries = 100)
    void textFilesGetTextRenderer(@ForAll("textMimeTypes") String mimeType) {
        // Act
        String previewType = filePreviewService.getPreviewType(mimeType);
        
        // Assert
        assertEquals("text", previewType,
            "Text MIME types should be assigned the 'text' preview type");
        assertTrue(filePreviewService.isPreviewable(mimeType),
            "Text files should be previewable");
    }
    
    /**
     * Property 3: Format-specific renderer selection (Image files)
     * For any image file, the system should select the Image renderer.
     */
    @Property(tries = 100)
    void imageFilesGetImageRenderer(@ForAll("imageMimeTypes") String mimeType) {
        // Act
        String previewType = filePreviewService.getPreviewType(mimeType);
        
        // Assert
        assertEquals("image", previewType,
            "Image MIME types should be assigned the 'image' preview type");
        assertTrue(filePreviewService.isPreviewable(mimeType),
            "Image files should be previewable");
    }
    
    /**
     * Property: Unsupported file types are marked as unsupported
     * For any unsupported MIME type, the system should return "unsupported".
     */
    @Property(tries = 100)
    void unsupportedFilesMarkedAsUnsupported(@ForAll("unsupportedMimeTypes") String mimeType) {
        // Act
        String previewType = filePreviewService.getPreviewType(mimeType);
        
        // Assert
        assertEquals("unsupported", previewType,
            "Unsupported MIME types should be assigned the 'unsupported' preview type");
        assertFalse(filePreviewService.isPreviewable(mimeType),
            "Unsupported files should not be previewable");
    }
    
    /**
     * Property: File extension detection works correctly
     * For any supported file extension, the system should detect the correct MIME type.
     */
    @Property(tries = 100)
    void fileExtensionDetectionWorks(@ForAll("supportedExtensions") ExtensionMimePair pair) {
        // Arrange
        String filePath = "test/file." + pair.extension;
        
        // Act
        String detectedMimeType = filePreviewService.detectMimeType(filePath);
        
        // Assert
        assertEquals(pair.expectedMimeType, detectedMimeType,
            "File extension '" + pair.extension + "' should be detected as '" + pair.expectedMimeType + "'");
    }
    
    /**
     * Property: Previewable consistency
     * For any MIME type, if it's previewable, it should have a non-unsupported preview type.
     */
    @Property(tries = 100)
    void previewableConsistency(@ForAll("allMimeTypes") String mimeType) {
        // Act
        boolean isPreviewable = filePreviewService.isPreviewable(mimeType);
        String previewType = filePreviewService.getPreviewType(mimeType);
        
        // Assert
        if (isPreviewable) {
            assertNotEquals("unsupported", previewType,
                "Previewable files should not have 'unsupported' preview type");
        } else {
            assertEquals("unsupported", previewType,
                "Non-previewable files should have 'unsupported' preview type");
        }
    }
    
    // ========== Arbitraries (Generators) ==========
    
    @Provide
    Arbitrary<String> pdfMimeTypes() {
        return Arbitraries.of("application/pdf");
    }
    
    @Provide
    Arbitrary<String> officeMimeTypes() {
        return Arbitraries.of(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        );
    }
    
    @Provide
    Arbitrary<String> codeMimeTypes() {
        return Arbitraries.of(
            "text/x-java-source",
            "text/javascript",
            "application/javascript",
            "text/x-python",
            "text/x-c",
            "text/x-c++",
            "text/css",
            "text/html",
            "application/x-sql"
        );
    }
    
    @Provide
    Arbitrary<String> textMimeTypes() {
        return Arbitraries.of(
            "text/plain",
            "text/markdown",
            "text/csv",
            "application/json",
            "application/xml",
            "text/xml"
        );
    }
    
    @Provide
    Arbitrary<String> imageMimeTypes() {
        return Arbitraries.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/svg+xml"
        );
    }
    
    @Provide
    Arbitrary<String> unsupportedMimeTypes() {
        return Arbitraries.of(
            "video/mp4",
            "audio/mpeg",
            "application/zip",
            "application/x-tar",
            "application/octet-stream",
            "text/calendar",
            "application/vnd.ms-fontobject"
        );
    }
    
    @Provide
    Arbitrary<String> allMimeTypes() {
        return Arbitraries.oneOf(
            pdfMimeTypes(),
            officeMimeTypes(),
            codeMimeTypes(),
            textMimeTypes(),
            imageMimeTypes(),
            unsupportedMimeTypes()
        );
    }
    
    @Provide
    Arbitrary<ExtensionMimePair> supportedExtensions() {
        return Arbitraries.of(
            // Text files
            new ExtensionMimePair("txt", "text/plain"),
            new ExtensionMimePair("md", "text/markdown"),
            new ExtensionMimePair("csv", "text/csv"),
            new ExtensionMimePair("log", "text/plain"),
            
            // Code files
            new ExtensionMimePair("java", "text/x-java-source"),
            new ExtensionMimePair("js", "text/javascript"),
            new ExtensionMimePair("py", "text/x-python"),
            new ExtensionMimePair("c", "text/x-c"),
            new ExtensionMimePair("cpp", "text/x-c++"),
            new ExtensionMimePair("css", "text/css"),
            new ExtensionMimePair("html", "text/html"),
            new ExtensionMimePair("sql", "application/x-sql"),
            new ExtensionMimePair("json", "application/json"),
            new ExtensionMimePair("xml", "application/xml"),
            
            // PDF
            new ExtensionMimePair("pdf", "application/pdf"),
            
            // Office documents
            new ExtensionMimePair("doc", "application/msword"),
            new ExtensionMimePair("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            new ExtensionMimePair("xls", "application/vnd.ms-excel"),
            new ExtensionMimePair("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            new ExtensionMimePair("ppt", "application/vnd.ms-powerpoint"),
            new ExtensionMimePair("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            
            // Images
            new ExtensionMimePair("jpg", "image/jpeg"),
            new ExtensionMimePair("jpeg", "image/jpeg"),
            new ExtensionMimePair("png", "image/png"),
            new ExtensionMimePair("gif", "image/gif"),
            new ExtensionMimePair("webp", "image/webp"),
            new ExtensionMimePair("svg", "image/svg+xml")
        );
    }
    
    // Helper class for extension-MIME type pairs
    static class ExtensionMimePair {
        final String extension;
        final String expectedMimeType;
        
        ExtensionMimePair(String extension, String expectedMimeType) {
            this.extension = extension;
            this.expectedMimeType = expectedMimeType;
        }
        
        @Override
        public String toString() {
            return extension + " -> " + expectedMimeType;
        }
    }
}
