package com.alquds.edu.ArchiveSystem.repository.task;

import com.alquds.edu.ArchiveSystem.entity.task.Task;
import com.alquds.edu.ArchiveSystem.entity.task.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    /**
     * Find all tasks for a professor in a semester.
     */
    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.course c " +
           "JOIN FETCH t.semester s " +
           "WHERE t.professor.id = :professorId AND t.semester.id = :semesterId " +
           "ORDER BY t.createdAt DESC")
    List<Task> findByProfessorIdAndSemesterId(
            @Param("professorId") Long professorId,
            @Param("semesterId") Long semesterId);
    
    /**
     * Find all tasks for a professor in a course and semester.
     */
    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.course c " +
           "JOIN FETCH t.semester s " +
           "WHERE t.professor.id = :professorId " +
           "AND t.course.id = :courseId " +
           "AND t.semester.id = :semesterId " +
           "ORDER BY t.createdAt DESC")
    List<Task> findByProfessorIdAndCourseIdAndSemesterId(
            @Param("professorId") Long professorId,
            @Param("courseId") Long courseId,
            @Param("semesterId") Long semesterId);
    
    /**
     * Calculate total weight percentage for a professor+course+semester combination.
     * Used for validation to ensure weights sum to 100%.
     */
    @Query("SELECT COALESCE(SUM(t.weightPercentage), 0) FROM Task t " +
           "WHERE t.professor.id = :professorId " +
           "AND t.course.id = :courseId " +
           "AND t.semester.id = :semesterId")
    Integer sumWeightPercentageByProfessorAndCourseAndSemester(
            @Param("professorId") Long professorId,
            @Param("courseId") Long courseId,
            @Param("semesterId") Long semesterId);
    
    /**
     * Calculate total weight percentage excluding a specific task.
     * Used when updating a task to validate the new weight doesn't exceed 100%.
     */
    @Query("SELECT COALESCE(SUM(t.weightPercentage), 0) FROM Task t " +
           "WHERE t.professor.id = :professorId " +
           "AND t.course.id = :courseId " +
           "AND t.semester.id = :semesterId " +
           "AND t.id != :excludeTaskId")
    Integer sumWeightPercentageExcludingTask(
            @Param("professorId") Long professorId,
            @Param("courseId") Long courseId,
            @Param("semesterId") Long semesterId,
            @Param("excludeTaskId") Long excludeTaskId);
    
    /**
     * Find all tasks in a department (for HOD view).
     */
    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.professor p " +
           "JOIN FETCH p.department d " +
           "JOIN FETCH t.course c " +
           "JOIN FETCH t.semester s " +
           "WHERE d.id = :departmentId " +
           "AND (:semesterId IS NULL OR t.semester.id = :semesterId) " +
           "AND (:courseId IS NULL OR t.course.id = :courseId) " +
           "AND (:professorId IS NULL OR t.professor.id = :professorId) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "ORDER BY t.createdAt DESC")
    List<Task> findByDepartmentIdWithFilters(
            @Param("departmentId") Long departmentId,
            @Param("semesterId") Long semesterId,
            @Param("courseId") Long courseId,
            @Param("professorId") Long professorId,
            @Param("status") TaskStatus status);
    
    /**
     * Find tasks by status for a department.
     */
    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.professor p " +
           "JOIN FETCH p.department d " +
           "JOIN FETCH t.course c " +
           "JOIN FETCH t.semester s " +
           "WHERE d.id = :departmentId AND t.status = :status " +
           "ORDER BY t.deadline ASC, t.createdAt DESC")
    List<Task> findByDepartmentIdAndStatus(
            @Param("departmentId") Long departmentId,
            @Param("status") TaskStatus status);
    
    /**
     * Find overdue tasks (deadline passed and status not COMPLETED/APPROVED/OVERDUE).
     */
    @Query("SELECT t FROM Task t " +
           "WHERE t.deadline < :today " +
           "AND t.status NOT IN (com.alquds.edu.ArchiveSystem.entity.task.TaskStatus.COMPLETED, " +
           "                      com.alquds.edu.ArchiveSystem.entity.task.TaskStatus.APPROVED, " +
           "                      com.alquds.edu.ArchiveSystem.entity.task.TaskStatus.OVERDUE)")
    List<Task> findOverdueTasks(@Param("today") LocalDate today);
    
    /**
     * Find tasks for a specific professor in a department.
     */
    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.professor p " +
           "JOIN FETCH p.department d " +
           "JOIN FETCH t.course c " +
           "JOIN FETCH t.semester s " +
           "WHERE d.id = :departmentId AND t.professor.id = :professorId " +
           "AND (:semesterId IS NULL OR t.semester.id = :semesterId) " +
           "ORDER BY t.createdAt DESC")
    List<Task> findByDepartmentIdAndProfessorId(
            @Param("departmentId") Long departmentId,
            @Param("professorId") Long professorId,
            @Param("semesterId") Long semesterId);
    
    /**
     * Count tasks by status for a department.
     */
    @Query("SELECT COUNT(t) FROM Task t " +
           "JOIN t.professor p " +
           "JOIN p.department d " +
           "WHERE d.id = :departmentId AND t.status = :status")
    Long countByDepartmentIdAndStatus(
            @Param("departmentId") Long departmentId,
            @Param("status") TaskStatus status);
    
    /**
     * Calculate course completion percentage (sum of completed task weights).
     */
    @Query("SELECT COALESCE(SUM(t.weightPercentage), 0) FROM Task t " +
           "WHERE t.professor.id = :professorId " +
           "AND t.course.id = :courseId " +
           "AND t.semester.id = :semesterId " +
           "AND t.status IN (com.alquds.edu.ArchiveSystem.entity.task.TaskStatus.COMPLETED, " +
           "                 com.alquds.edu.ArchiveSystem.entity.task.TaskStatus.APPROVED)")
    Integer calculateCourseCompletion(
            @Param("professorId") Long professorId,
            @Param("courseId") Long courseId,
            @Param("semesterId") Long semesterId);
    
    /**
     * Update task status to OVERDUE for overdue tasks.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Task t SET t.status = com.alquds.edu.ArchiveSystem.entity.task.TaskStatus.OVERDUE " +
           "WHERE t.deadline < :today " +
           "AND t.status NOT IN (com.alquds.edu.ArchiveSystem.entity.task.TaskStatus.COMPLETED, " +
           "                      com.alquds.edu.ArchiveSystem.entity.task.TaskStatus.APPROVED, " +
           "                      com.alquds.edu.ArchiveSystem.entity.task.TaskStatus.OVERDUE)")
    int markTasksAsOverdue(@Param("today") LocalDate today);
    
    /**
     * Find task with all relationships loaded (for detailed view).
     */
    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.fileReference " +
           "JOIN FETCH t.professor p " +
           "LEFT JOIN FETCH p.department " +
           "JOIN FETCH t.course c " +
           "LEFT JOIN FETCH c.department " +
           "JOIN FETCH t.semester s " +
           "LEFT JOIN FETCH s.academicYear " +
           "WHERE t.id = :taskId")
    Optional<Task> findByIdWithRelations(@Param("taskId") Long taskId);
}
