package com.alquds.edu.ArchiveSystem.repository.file;

import com.alquds.edu.ArchiveSystem.entity.file.FolderType;
import com.alquds.edu.ArchiveSystem.entity.file.Folder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Folder entity operations.
 */
@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    
    /**
     * Find a folder by its unique path.
     *
     * @param path the folder path
     * @return Optional containing the folder if found
     */
    Optional<Folder> findByPath(String path);
    
    /**
     * Check if a folder exists with the given path.
     *
     * @param path the folder path
     * @return true if folder exists
     */
    boolean existsByPath(String path);
    
    /**
     * Find all folders owned by a specific user in a given academic year and semester.
     *
     * @param ownerId the owner user ID
     * @param academicYearId the academic year ID
     * @param semesterId the semester ID
     * @return list of folders
     */
    @Query("SELECT f FROM Folder f WHERE f.owner.id = :ownerId " +
           "AND f.academicYear.id = :academicYearId " +
           "AND f.semester.id = :semesterId")
    List<Folder> findByOwnerAndAcademicYearAndSemester(
        @Param("ownerId") Long ownerId,
        @Param("academicYearId") Long academicYearId,
        @Param("semesterId") Long semesterId
    );
    
    /**
     * Find professor root folder for a specific professor in a given academic year and semester.
     *
     * @param ownerId the professor user ID
     * @param academicYearId the academic year ID
     * @param semesterId the semester ID
     * @param type the folder type (should be PROFESSOR_ROOT)
     * @return Optional containing the professor root folder if found
     */
    @Query("SELECT f FROM Folder f WHERE f.owner.id = :ownerId " +
           "AND f.academicYear.id = :academicYearId " +
           "AND f.semester.id = :semesterId " +
           "AND f.type = :type")
    Optional<Folder> findProfessorRootFolder(
        @Param("ownerId") Long ownerId,
        @Param("academicYearId") Long academicYearId,
        @Param("semesterId") Long semesterId,
        @Param("type") FolderType type
    );
    
    /**
     * Find course folder for a specific professor and course in a given academic year and semester.
     *
     * @param ownerId the professor user ID
     * @param courseId the course ID
     * @param academicYearId the academic year ID
     * @param semesterId the semester ID
     * @param type the folder type (should be COURSE)
     * @return Optional containing the course folder if found
     */
    @Query("SELECT f FROM Folder f WHERE f.owner.id = :ownerId " +
           "AND f.course.id = :courseId " +
           "AND f.academicYear.id = :academicYearId " +
           "AND f.semester.id = :semesterId " +
           "AND f.type = :type")
    Optional<Folder> findCourseFolder(
        @Param("ownerId") Long ownerId,
        @Param("courseId") Long courseId,
        @Param("academicYearId") Long academicYearId,
        @Param("semesterId") Long semesterId,
        @Param("type") FolderType type
    );
    
    /**
     * Find all child folders of a parent folder.
     *
     * @param parentId the parent folder ID
     * @return list of child folders
     */
    List<Folder> findByParentId(Long parentId);
    
    /**
     * Find all folders owned by a specific user.
     *
     * @param ownerId the owner user ID
     * @return list of folders owned by the user
     */
    List<Folder> findByOwnerId(Long ownerId);
    
    /**
     * Find a child folder by name and parent ID.
     * Used to check for duplicate folder names within the same parent.
     *
     * @param name the folder name
     * @param parentId the parent folder ID
     * @return Optional containing the folder if found
     */
    Optional<Folder> findByNameAndParentId(String name, Long parentId);
    
    /**
     * Check if a folder with the given name exists under the specified parent.
     *
     * @param name the folder name
     * @param parentId the parent folder ID
     * @return true if folder exists
     */
    boolean existsByNameAndParentId(String name, Long parentId);
}
