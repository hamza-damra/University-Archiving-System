package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.entity.file.FolderType;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.entity.file.Folder;


import java.util.List;
import java.util.Optional;

/**
 * Service for managing folder creation and organization in the file explorer system.
 * Provides idempotent folder creation operations to support automatic provisioning.
 */
public interface FolderService {
    
    /**
     * Create a professor root folder for a specific academic year and semester.
     * This operation is idempotent - if the folder already exists, it returns the existing folder.
     * 
     * The folder path follows the convention: {yearCode}/{semesterType}/{professorId}
     * Example: "2024-2025/first/PROF123"
     *
     * @param professorId the professor user ID
     * @param academicYearId the academic year ID
     * @param semesterId the semester ID
     * @return the created or existing professor root folder
     * @throws com.alquds.edu.ArchiveSystem.exception.EntityNotFoundException if professor, academic year, or semester not found
     */
    Folder createProfessorFolder(Long professorId, Long academicYearId, Long semesterId);
    
    /**
     * Create a complete course folder structure under a professor's folder.
     * This operation is idempotent - existing folders are skipped.
     * 
     * Creates the following structure:
     * - Course folder: {yearCode}/{semesterType}/{professorId}/{courseCode} - {courseName}
     * - Standard subfolders: Syllabus, Exams, Course Notes, Assignments
     *
     * @param professorId the professor user ID
     * @param courseId the course ID
     * @param academicYearId the academic year ID
     * @param semesterId the semester ID
     * @return list of created or existing folders (course folder + subfolders)
     * @throws com.alquds.edu.ArchiveSystem.exception.EntityNotFoundException if any required entity not found
     */
    List<Folder> createCourseFolderStructure(Long professorId, Long courseId, 
                                             Long academicYearId, Long semesterId);
    
    /**
     * Check if a professor root folder exists for the given context.
     *
     * @param professorId the professor user ID
     * @param academicYearId the academic year ID
     * @param semesterId the semester ID
     * @return true if the professor folder exists
     */
    boolean professorFolderExists(Long professorId, Long academicYearId, Long semesterId);
    
    /**
     * Check if a course folder exists for the given context.
     *
     * @param professorId the professor user ID
     * @param courseId the course ID
     * @param academicYearId the academic year ID
     * @param semesterId the semester ID
     * @return true if the course folder exists
     */
    boolean courseFolderExists(Long professorId, Long courseId, 
                               Long academicYearId, Long semesterId);
    
    /**
     * Get a folder by its unique path.
     *
     * @param path the folder path
     * @return Optional containing the folder if found
     */
    Optional<Folder> getFolderByPath(String path);
    
    /**
     * Create a folder if it doesn't already exist.
     * This is a utility method for creating individual folders with full control over properties.
     * 
     * This operation is idempotent - if a folder with the same path exists, it returns the existing folder.
     *
     * @param path the full folder path
     * @param name the display name
     * @param parent the parent folder (null for root folders)
     * @param type the folder type
     * @param owner the folder owner
     * @return the created or existing folder
     */
    Folder createFolderIfNotExists(String path, String name, Folder parent, 
                                   FolderType type, User owner);
    
    /**
     * Get or create a folder by path.
     * Creates the folder hierarchy if it doesn't exist.
     * 
     * Path format: /academicYear/semester/professorId/courseCode/documentType
     * Example: /2024-2025/first/PROF6/CS101/lecture_notes
     * 
     * This operation:
     * 1. Parses the path to extract components
     * 2. Validates each component exists in the database
     * 3. Checks if folder already exists
     * 4. Creates folder if it doesn't exist
     * 
     * @param path Full folder path
     * @param userId User requesting the folder (for permission validation)
     * @return Folder entity with ID
     * @throws com.alquds.edu.ArchiveSystem.exception.EntityNotFoundException if any path component is invalid
     * @throws com.alquds.edu.ArchiveSystem.exception.UnauthorizedException if user doesn't have permission
     */
    Folder getOrCreateFolderByPath(String path, Long userId);
}
