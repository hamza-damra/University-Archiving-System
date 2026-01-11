package com.alquds.edu.ArchiveSystem.util;

import lombok.Builder;
import lombok.Data;

/**
 * Utility class for parsing folder paths
 * Path format: /academicYear/semester/professorId/courseCode/documentType
 * Example: /2024-2025/first/PROF6/CS101/lecture_notes
 */
public class PathParser {

    @Data
    @Builder
    public static class PathComponents {
        private String academicYearCode;
        private String semesterType;
        private String professorId;
        private String courseCode;
        private String documentType;
    }

    /**
     * Parse a folder path into its components
     * 
     * @param path Full folder path
     * @return PathComponents containing parsed values
     * @throws IllegalArgumentException if path format is invalid
     */
    public static PathComponents parse(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        // Remove leading/trailing slashes
        String cleanPath = path.trim();
        if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }
        if (cleanPath.endsWith("/")) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 1);
        }

        // Split path
        String[] parts = cleanPath.split("/");

        // Validate path has correct number of components
        if (parts.length != 5) {
            throw new IllegalArgumentException(
                "Invalid path format. Expected: /academicYear/semester/professorId/courseCode/documentType, got: " + path
            );
        }

        return PathComponents.builder()
            .academicYearCode(parts[0])
            .semesterType(parts[1])
            .professorId(parts[2])
            .courseCode(parts[3])
            .documentType(parts[4])
            .build();
    }

    /**
     * Format document type name for display
     * Converts: lecture_notes -> Lecture Notes
     */
    public static String formatDocumentTypeName(String documentType) {
        if (documentType == null || documentType.isEmpty()) {
            return documentType;
        }

        return documentType.replace("_", " ")
            .substring(0, 1).toUpperCase() + 
            documentType.replace("_", " ").substring(1);
    }
}
