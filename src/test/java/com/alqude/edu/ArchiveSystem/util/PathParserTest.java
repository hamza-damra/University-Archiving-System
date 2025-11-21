package com.alqude.edu.ArchiveSystem.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PathParser utility class
 */
class PathParserTest {

    @Test
    void testParse_ValidPath() {
        // Given
        String path = "/2024-2025/first/PROF6/CS101/lecture_notes";
        
        // When
        PathParser.PathComponents components = PathParser.parse(path);
        
        // Then
        assertNotNull(components);
        assertEquals("2024-2025", components.getAcademicYearCode());
        assertEquals("first", components.getSemesterType());
        assertEquals("PROF6", components.getProfessorId());
        assertEquals("CS101", components.getCourseCode());
        assertEquals("lecture_notes", components.getDocumentType());
    }
    
    @Test
    void testParse_ValidPathWithoutLeadingSlash() {
        // Given
        String path = "2024-2025/first/PROF6/CS101/exams";
        
        // When
        PathParser.PathComponents components = PathParser.parse(path);
        
        // Then
        assertNotNull(components);
        assertEquals("2024-2025", components.getAcademicYearCode());
        assertEquals("first", components.getSemesterType());
        assertEquals("PROF6", components.getProfessorId());
        assertEquals("CS101", components.getCourseCode());
        assertEquals("exams", components.getDocumentType());
    }
    
    @Test
    void testParse_ValidPathWithTrailingSlash() {
        // Given
        String path = "/2024-2025/second/PROF10/MATH201/assignments/";
        
        // When
        PathParser.PathComponents components = PathParser.parse(path);
        
        // Then
        assertNotNull(components);
        assertEquals("2024-2025", components.getAcademicYearCode());
        assertEquals("second", components.getSemesterType());
        assertEquals("PROF10", components.getProfessorId());
        assertEquals("MATH201", components.getCourseCode());
        assertEquals("assignments", components.getDocumentType());
    }
    
    @Test
    void testParse_NullPath() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PathParser.parse(null)
        );
        
        assertEquals("Path cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void testParse_EmptyPath() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PathParser.parse("")
        );
        
        assertEquals("Path cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void testParse_InvalidPathTooFewComponents() {
        // Given
        String path = "/2024-2025/first/PROF6";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PathParser.parse(path)
        );
        
        assertTrue(exception.getMessage().contains("Invalid path format"));
    }
    
    @Test
    void testParse_InvalidPathTooManyComponents() {
        // Given
        String path = "/2024-2025/first/PROF6/CS101/lecture_notes/extra";
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PathParser.parse(path)
        );
        
        assertTrue(exception.getMessage().contains("Invalid path format"));
    }
    
    @Test
    void testFormatDocumentTypeName_LectureNotes() {
        // Given
        String documentType = "lecture_notes";
        
        // When
        String formatted = PathParser.formatDocumentTypeName(documentType);
        
        // Then
        assertEquals("Lecture notes", formatted);
    }
    
    @Test
    void testFormatDocumentTypeName_Exams() {
        // Given
        String documentType = "exams";
        
        // When
        String formatted = PathParser.formatDocumentTypeName(documentType);
        
        // Then
        assertEquals("Exams", formatted);
    }
    
    @Test
    void testFormatDocumentTypeName_Assignments() {
        // Given
        String documentType = "course_assignments";
        
        // When
        String formatted = PathParser.formatDocumentTypeName(documentType);
        
        // Then
        assertEquals("Course assignments", formatted);
    }
    
    @Test
    void testFormatDocumentTypeName_NullInput() {
        // When
        String formatted = PathParser.formatDocumentTypeName(null);
        
        // Then
        assertNull(formatted);
    }
    
    @Test
    void testFormatDocumentTypeName_EmptyInput() {
        // When
        String formatted = PathParser.formatDocumentTypeName("");
        
        // Then
        assertEquals("", formatted);
    }
}
