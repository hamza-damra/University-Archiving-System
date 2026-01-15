package com.alquds.edu.ArchiveSystem.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PathParser Unit Tests")
class PathParserTest {

    @Test
    @DisplayName("Should parse valid folder path successfully")
    void shouldParseValidFolderPath() {
        // Arrange
        String path = "/2024-2025/first/PROF6/CS101/lecture_notes";

        // Act
        PathParser.PathComponents components = PathParser.parse(path);

        // Assert
        assertThat(components).isNotNull();
        assertThat(components.getAcademicYearCode()).isEqualTo("2024-2025");
        assertThat(components.getSemesterType()).isEqualTo("first");
        assertThat(components.getProfessorId()).isEqualTo("PROF6");
        assertThat(components.getCourseCode()).isEqualTo("CS101");
        assertThat(components.getDocumentType()).isEqualTo("lecture_notes");
    }

    @Test
    @DisplayName("Should parse path without leading slash")
    void shouldParsePathWithoutLeadingSlash() {
        // Arrange
        String path = "2024-2025/first/PROF6/CS101/lecture_notes";

        // Act
        PathParser.PathComponents components = PathParser.parse(path);

        // Assert
        assertThat(components).isNotNull();
        assertThat(components.getAcademicYearCode()).isEqualTo("2024-2025");
        assertThat(components.getSemesterType()).isEqualTo("first");
        assertThat(components.getProfessorId()).isEqualTo("PROF6");
        assertThat(components.getCourseCode()).isEqualTo("CS101");
        assertThat(components.getDocumentType()).isEqualTo("lecture_notes");
    }

    @Test
    @DisplayName("Should parse path with trailing slash")
    void shouldParsePathWithTrailingSlash() {
        // Arrange
        String path = "/2024-2025/first/PROF6/CS101/lecture_notes/";

        // Act
        PathParser.PathComponents components = PathParser.parse(path);

        // Assert
        assertThat(components).isNotNull();
        assertThat(components.getAcademicYearCode()).isEqualTo("2024-2025");
        assertThat(components.getSemesterType()).isEqualTo("first");
        assertThat(components.getProfessorId()).isEqualTo("PROF6");
        assertThat(components.getCourseCode()).isEqualTo("CS101");
        assertThat(components.getDocumentType()).isEqualTo("lecture_notes");
    }

    @Test
    @DisplayName("Should parse path with special characters in components")
    void shouldParsePathWithSpecialCharacters() {
        // Arrange
        String path = "/2024-2025/second/PROF-123/CS-101/exam_solutions";

        // Act
        PathParser.PathComponents components = PathParser.parse(path);

        // Assert
        assertThat(components).isNotNull();
        assertThat(components.getAcademicYearCode()).isEqualTo("2024-2025");
        assertThat(components.getSemesterType()).isEqualTo("second");
        assertThat(components.getProfessorId()).isEqualTo("PROF-123");
        assertThat(components.getCourseCode()).isEqualTo("CS-101");
        assertThat(components.getDocumentType()).isEqualTo("exam_solutions");
    }

    @Test
    @DisplayName("Should throw exception for null path")
    void shouldThrowExceptionForNullPath() {
        // Arrange
        String path = null;

        // Act & Assert
        assertThatThrownBy(() -> PathParser.parse(path))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Path cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw exception for empty path")
    void shouldThrowExceptionForEmptyPath() {
        // Arrange
        String path = "";

        // Act & Assert
        assertThatThrownBy(() -> PathParser.parse(path))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Path cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw exception for path with too few components")
    void shouldThrowExceptionForPathWithTooFewComponents() {
        // Arrange
        String path = "/2024-2025/first/PROF6/CS101";

        // Act & Assert
        assertThatThrownBy(() -> PathParser.parse(path))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid path format");
    }

    @Test
    @DisplayName("Should throw exception for path with too many components")
    void shouldThrowExceptionForPathWithTooManyComponents() {
        // Arrange
        String path = "/2024-2025/first/PROF6/CS101/lecture_notes/extra";

        // Act & Assert
        assertThatThrownBy(() -> PathParser.parse(path))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid path format");
    }

    @Test
    @DisplayName("Should extract all components from path correctly")
    void shouldExtractAllComponentsFromPath() {
        // Arrange
        String path = "/2025-2026/second/PROF10/MATH201/assignments";

        // Act
        PathParser.PathComponents components = PathParser.parse(path);

        // Assert
        assertThat(components.getAcademicYearCode()).isEqualTo("2025-2026");
        assertThat(components.getSemesterType()).isEqualTo("second");
        assertThat(components.getProfessorId()).isEqualTo("PROF10");
        assertThat(components.getCourseCode()).isEqualTo("MATH201");
        assertThat(components.getDocumentType()).isEqualTo("assignments");
    }

    @Test
    @DisplayName("Should format document type name correctly")
    void shouldFormatDocumentTypeName() {
        // Arrange
        String documentType = "lecture_notes";

        // Act
        String formatted = PathParser.formatDocumentTypeName(documentType);

        // Assert
        assertThat(formatted).isEqualTo("Lecture notes");
    }

    @Test
    @DisplayName("Should format document type with multiple underscores")
    void shouldFormatDocumentTypeWithMultipleUnderscores() {
        // Arrange
        String documentType = "exam_solutions_final";

        // Act
        String formatted = PathParser.formatDocumentTypeName(documentType);

        // Assert
        assertThat(formatted).isEqualTo("Exam solutions final");
    }

    @Test
    @DisplayName("Should return null for null document type")
    void shouldReturnNullForNullDocumentType() {
        // Arrange
        String documentType = null;

        // Act
        String formatted = PathParser.formatDocumentTypeName(documentType);

        // Assert
        assertThat(formatted).isNull();
    }

    @Test
    @DisplayName("Should return empty string for empty document type")
    void shouldReturnEmptyStringForEmptyDocumentType() {
        // Arrange
        String documentType = "";

        // Act
        String formatted = PathParser.formatDocumentTypeName(documentType);

        // Assert
        assertThat(formatted).isEmpty();
    }

    @Test
    @DisplayName("Should handle single character document type")
    void shouldHandleSingleCharacterDocumentType() {
        // Arrange
        String documentType = "a";

        // Act
        String formatted = PathParser.formatDocumentTypeName(documentType);

        // Assert
        assertThat(formatted).isEqualTo("A");
    }
}
