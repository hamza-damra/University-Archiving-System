package com.alquds.edu.ArchiveSystem.service.task;

import com.alquds.edu.ArchiveSystem.dto.task.*;
import com.alquds.edu.ArchiveSystem.entity.academic.Course;
import com.alquds.edu.ArchiveSystem.entity.academic.Semester;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.alquds.edu.ArchiveSystem.entity.task.Task;
import com.alquds.edu.ArchiveSystem.entity.task.TaskAuditLog;
import com.alquds.edu.ArchiveSystem.entity.task.TaskStatus;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.exception.core.BusinessException;
import com.alquds.edu.ArchiveSystem.exception.core.EntityNotFoundException;
import com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException;
import com.alquds.edu.ArchiveSystem.exception.core.ValidationException;
import com.alquds.edu.ArchiveSystem.repository.academic.CourseRepository;
import com.alquds.edu.ArchiveSystem.repository.academic.SemesterRepository;
import com.alquds.edu.ArchiveSystem.repository.file.UploadedFileRepository;
import com.alquds.edu.ArchiveSystem.repository.task.TaskAuditLogRepository;
import com.alquds.edu.ArchiveSystem.repository.task.TaskRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of TaskService for managing academic tasks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskServiceImpl implements TaskService {
    
    private final TaskRepository taskRepository;
    private final TaskAuditLogRepository taskAuditLogRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final UploadedFileRepository uploadedFileRepository;
    
    @Override
    public TaskDTO createTask(TaskCreateRequest request, Long professorId) {
        log.info("Creating task for professor {}: {}", professorId, request.getTitle());
        
        // Validate professor exists
        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> new EntityNotFoundException("Professor not found with ID: " + professorId));
        
        // Validate course exists
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new EntityNotFoundException("Course not found with ID: " + request.getCourseId()));
        
        // Validate semester exists
        Semester semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new EntityNotFoundException("Semester not found with ID: " + request.getSemesterId()));
        
        // Validate weight percentage
        validateWeightPercentage(professorId, request.getCourseId(), request.getSemesterId(), 
                request.getWeightPercentage(), null);
        
        // Validate file reference if provided
        UploadedFile fileReference = null;
        if (request.getFileReferenceId() != null) {
            fileReference = uploadedFileRepository.findById(request.getFileReferenceId())
                    .orElseThrow(() -> new EntityNotFoundException("File not found with ID: " + request.getFileReferenceId()));
        }
        
        // Create task
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setWeightPercentage(request.getWeightPercentage());
        task.setProgressPercentage(0);
        task.setStatus(TaskStatus.PENDING);
        task.setDeadline(request.getDeadline());
        task.setFileReference(fileReference);
        task.setProfessor(professor);
        task.setCourse(course);
        task.setSemester(semester);
        
        task = taskRepository.save(task);
        
        // Log status change
        logStatusChange(task, null, TaskStatus.PENDING, professor, "Task created");
        
        log.info("Task created successfully with ID: {}", task.getId());
        return mapToDTO(task);
    }
    
    @Override
    public TaskDTO updateTask(Long taskId, TaskUpdateRequest request, Long professorId) {
        log.info("Updating task {} for professor {}", taskId, professorId);
        
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with ID: " + taskId));
        
        // Validate professor owns the task
        if (!task.getProfessor().getId().equals(professorId)) {
            throw new UnauthorizedOperationException("You can only update your own tasks");
        }
        
        TaskStatus oldStatus = task.getStatus();
        
        // Update fields
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getWeightPercentage() != null) {
            // Validate weight percentage
            validateWeightPercentage(professorId, task.getCourse().getId(), task.getSemester().getId(),
                    request.getWeightPercentage(), taskId);
            task.setWeightPercentage(request.getWeightPercentage());
        }
        if (request.getProgressPercentage() != null) {
            if (request.getProgressPercentage() < 0 || request.getProgressPercentage() > 100) {
                throw new ValidationException("Progress percentage must be between 0 and 100", 
                        Map.of("progressPercentage", "Value must be between 0 and 100"));
            }
            task.setProgressPercentage(request.getProgressPercentage());
            
            // Update status based on progress
            if (request.getProgressPercentage() > 0 && task.getStatus() == TaskStatus.PENDING) {
                task.setStatus(TaskStatus.IN_PROGRESS);
            }
            if (request.getProgressPercentage() == 100 && task.getStatus() != TaskStatus.COMPLETED) {
                task.setStatus(TaskStatus.COMPLETED);
            }
        }
        if (request.getDeadline() != null) {
            task.setDeadline(request.getDeadline());
        }
        if (request.getFileReferenceId() != null) {
            if (request.getFileReferenceId() == 0) {
                // Remove file reference
                task.setFileReference(null);
            } else {
                UploadedFile fileReference = uploadedFileRepository.findById(request.getFileReferenceId())
                        .orElseThrow(() -> new EntityNotFoundException("File not found with ID: " + request.getFileReferenceId()));
                task.setFileReference(fileReference);
            }
        }
        
        task = taskRepository.save(task);
        
        // Log status change if status changed
        if (oldStatus != task.getStatus()) {
            logStatusChange(task, oldStatus, task.getStatus(), task.getProfessor(), "Task updated");
        }
        
        log.info("Task {} updated successfully", taskId);
        return mapToDTO(task);
    }
    
    @Override
    public void deleteTask(Long taskId, Long professorId) {
        log.info("Deleting task {} for professor {}", taskId, professorId);
        
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with ID: " + taskId));
        
        // Validate professor owns the task
        if (!task.getProfessor().getId().equals(professorId)) {
            throw new UnauthorizedOperationException("You can only delete your own tasks");
        }
        
        // Only PENDING tasks can be deleted
        if (task.getStatus() != TaskStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "Only PENDING tasks can be deleted");
        }
        
        taskRepository.delete(task);
        log.info("Task {} deleted successfully", taskId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public TaskDTO getTaskById(Long taskId, Long professorId) {
        Task task = taskRepository.findByIdWithRelations(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with ID: " + taskId));
        
        // Validate professor owns the task or is in same department
        if (!task.getProfessor().getId().equals(professorId)) {
            if (task.getProfessor().getDepartment() == null || 
                task.getProfessor().getDepartment().getId() == null) {
                throw new UnauthorizedOperationException("You do not have access to this task");
            }
            User professor = userRepository.findById(professorId)
                    .orElseThrow(() -> new EntityNotFoundException("Professor not found"));
            if (professor.getDepartment() == null || 
                !professor.getDepartment().getId().equals(task.getProfessor().getDepartment().getId())) {
                throw new UnauthorizedOperationException("You do not have access to this task");
            }
        }
        
        return mapToDTO(task);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksForProfessor(Long professorId, TaskFilterRequest filters) {
        List<Task> tasks;
        
        if (filters != null && filters.getCourseId() != null && filters.getSemesterId() != null) {
            tasks = taskRepository.findByProfessorIdAndCourseIdAndSemesterId(
                    professorId, filters.getCourseId(), filters.getSemesterId());
        } else if (filters != null && filters.getSemesterId() != null) {
            tasks = taskRepository.findByProfessorIdAndSemesterId(professorId, filters.getSemesterId());
        } else {
            // Get all tasks for professor (may need to add this query)
            tasks = taskRepository.findAll().stream()
                    .filter(t -> t.getProfessor().getId().equals(professorId))
                    .collect(Collectors.toList());
        }
        
        // Apply additional filters
        if (filters != null) {
            if (filters.getStatuses() != null && !filters.getStatuses().isEmpty()) {
                tasks = tasks.stream()
                        .filter(t -> filters.getStatuses().contains(t.getStatus()))
                        .collect(Collectors.toList());
            }
            if (filters.getOverdueOnly() != null && filters.getOverdueOnly()) {
                tasks = tasks.stream()
                        .filter(Task::isOverdue)
                        .collect(Collectors.toList());
            }
        }
        
        return tasks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public WeightSummaryDTO getWeightSummary(Long professorId, Long courseId, Long semesterId) {
        List<Task> tasks = taskRepository.findByProfessorIdAndCourseIdAndSemesterId(
                professorId, courseId, semesterId);
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));
        
        Map<Long, Integer> taskWeights = tasks.stream()
                .collect(Collectors.toMap(Task::getId, Task::getWeightPercentage));
        
        Integer totalWeight = tasks.stream()
                .mapToInt(Task::getWeightPercentage)
                .sum();
        
        return WeightSummaryDTO.builder()
                .courseId(courseId)
                .courseCode(course.getCourseCode())
                .courseName(course.getCourseName())
                .semesterId(semesterId)
                .taskWeights(taskWeights)
                .totalWeightPercentage(totalWeight)
                .isValid(totalWeight == 100)
                .validationMessage(totalWeight == 100 ? 
                        "Weight distribution is valid" : 
                        String.format("Total weight is %d%%, must be 100%%", totalWeight))
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksForDepartment(Long departmentId, TaskFilterRequest filters) {
        List<Task> tasks = taskRepository.findByDepartmentIdWithFilters(
                departmentId,
                filters != null ? filters.getSemesterId() : null,
                filters != null ? filters.getCourseId() : null,
                filters != null ? filters.getProfessorId() : null,
                filters != null && filters.getStatuses() != null && !filters.getStatuses().isEmpty() 
                        ? filters.getStatuses().get(0) : null);
        
        return tasks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public TaskDTO getTaskByIdForHod(Long taskId, Long departmentId) {
        Task task = taskRepository.findByIdWithRelations(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with ID: " + taskId));
        
        // Validate task is in HOD's department
        if (task.getProfessor().getDepartment() == null || 
            !task.getProfessor().getDepartment().getId().equals(departmentId)) {
            throw new UnauthorizedOperationException("You do not have access to this task");
        }
        
        return mapToDTO(task);
    }
    
    @Override
    public TaskDTO approveTask(Long taskId, TaskApprovalRequest request, User hodUser) {
        log.info("HOD {} approving task {}", hodUser.getId(), taskId);
        
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with ID: " + taskId));
        
        // Validate task is in HOD's department
        if (task.getProfessor().getDepartment() == null || 
            !task.getProfessor().getDepartment().getId().equals(hodUser.getDepartment().getId())) {
            throw new UnauthorizedOperationException("You can only approve tasks in your department");
        }
        
        // Only COMPLETED tasks can be approved
        if (task.getStatus() != TaskStatus.COMPLETED) {
            throw new BusinessException("INVALID_STATUS", "Only COMPLETED tasks can be approved");
        }
        
        TaskStatus oldStatus = task.getStatus();
        task.setStatus(TaskStatus.APPROVED);
        task = taskRepository.save(task);
        
        // Log approval
        logStatusChange(task, oldStatus, TaskStatus.APPROVED, hodUser, 
                request.getFeedback() != null ? request.getFeedback() : "Task approved by HOD");
        
        log.info("Task {} approved by HOD {}", taskId, hodUser.getId());
        return mapToDTO(task);
    }
    
    @Override
    public TaskDTO rejectTask(Long taskId, TaskApprovalRequest request, User hodUser) {
        log.info("HOD {} rejecting task {}", hodUser.getId(), taskId);
        
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with ID: " + taskId));
        
        // Validate task is in HOD's department
        if (task.getProfessor().getDepartment() == null || 
            !task.getProfessor().getDepartment().getId().equals(hodUser.getDepartment().getId())) {
            throw new UnauthorizedOperationException("You can only reject tasks in your department");
        }
        
        // Only COMPLETED tasks can be rejected
        if (task.getStatus() != TaskStatus.COMPLETED) {
            throw new BusinessException("INVALID_STATUS", "Only COMPLETED tasks can be rejected");
        }
        
        TaskStatus oldStatus = task.getStatus();
        task.setStatus(TaskStatus.REJECTED);
        task = taskRepository.save(task);
        
        // Log rejection
        logStatusChange(task, oldStatus, TaskStatus.REJECTED, hodUser, 
                request.getFeedback() != null ? request.getFeedback() : "Task rejected by HOD");
        
        log.info("Task {} rejected by HOD {}", taskId, hodUser.getId());
        return mapToDTO(task);
    }
    
    @Override
    @Transactional(readOnly = true)
    public TaskStatisticsDTO getTaskStatistics(Long departmentId, Long semesterId) {
        List<Task> allTasks = taskRepository.findByDepartmentIdWithFilters(
                departmentId, semesterId, null, null, null);
        
        TaskStatisticsDTO.TaskStatisticsDTOBuilder builder = TaskStatisticsDTO.builder();
        
        // Count by status
        builder.totalTasks((long) allTasks.size())
                .pendingTasks(allTasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count())
                .inProgressTasks(allTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count())
                .completedTasks(allTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count())
                .overdueTasks(allTasks.stream().filter(t -> t.getStatus() == TaskStatus.OVERDUE).count())
                .approvedTasks(allTasks.stream().filter(t -> t.getStatus() == TaskStatus.APPROVED).count())
                .rejectedTasks(allTasks.stream().filter(t -> t.getStatus() == TaskStatus.REJECTED).count());
        
        // Course completion stats
        Map<Long, TaskStatisticsDTO.CourseCompletionStats> courseCompletion = new HashMap<>();
        Map<Long, List<Task>> tasksByCourse = allTasks.stream()
                .collect(Collectors.groupingBy(t -> t.getCourse().getId()));
        
        for (Map.Entry<Long, List<Task>> entry : tasksByCourse.entrySet()) {
            List<Task> courseTasks = entry.getValue();
            Course course = courseTasks.get(0).getCourse();
            
            int completionWeight = courseTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.APPROVED)
                    .mapToInt(Task::getWeightPercentage)
                    .sum();
            
            long completedCount = courseTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.APPROVED)
                    .count();
            
            courseCompletion.put(entry.getKey(), TaskStatisticsDTO.CourseCompletionStats.builder()
                    .courseId(course.getId())
                    .courseCode(course.getCourseCode())
                    .courseName(course.getCourseName())
                    .completionPercentage(completionWeight)
                    .totalTasks((long) courseTasks.size())
                    .completedTasks(completedCount)
                    .build());
        }
        builder.courseCompletion(courseCompletion);
        
        // Professor completion stats
        Map<Long, TaskStatisticsDTO.ProfessorCompletionStats> professorCompletion = new HashMap<>();
        Map<Long, List<Task>> tasksByProfessor = allTasks.stream()
                .collect(Collectors.groupingBy(t -> t.getProfessor().getId()));
        
        for (Map.Entry<Long, List<Task>> entry : tasksByProfessor.entrySet()) {
            List<Task> profTasks = entry.getValue();
            User professor = profTasks.get(0).getProfessor();
            
            long totalTasks = profTasks.size();
            long completedCount = profTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.APPROVED)
                    .count();
            long approvedCount = profTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.APPROVED)
                    .count();
            
            // Calculate average completion percentage
            int totalWeight = profTasks.stream().mapToInt(Task::getWeightPercentage).sum();
            int completedWeight = profTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.APPROVED)
                    .mapToInt(Task::getWeightPercentage)
                    .sum();
            int avgCompletion = totalWeight > 0 ? (completedWeight * 100 / totalWeight) : 0;
            
            professorCompletion.put(entry.getKey(), TaskStatisticsDTO.ProfessorCompletionStats.builder()
                    .professorId(professor.getId())
                    .professorName(professor.getFirstName() + " " + professor.getLastName())
                    .completionPercentage(avgCompletion)
                    .totalTasks(totalTasks)
                    .completedTasks(completedCount)
                    .approvedTasks(approvedCount)
                    .build());
        }
        builder.professorCompletion(professorCompletion);
        
        return builder.build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksForProfessorInDepartment(Long departmentId, Long professorId, Long semesterId) {
        List<Task> tasks = taskRepository.findByDepartmentIdAndProfessorId(departmentId, professorId, semesterId);
        return tasks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public int checkAndUpdateOverdueTasks() {
        log.info("Checking for overdue tasks");
        LocalDate today = LocalDate.now();
        int updated = taskRepository.markTasksAsOverdue(today);
        log.info("Updated {} tasks to OVERDUE status", updated);
        return updated;
    }
    
    /**
     * Validate that weight percentage sum doesn't exceed 100% for a professor+course+semester combination.
     */
    private void validateWeightPercentage(Long professorId, Long courseId, Long semesterId, 
                                         Integer newWeight, Long excludeTaskId) {
        Integer currentTotal;
        if (excludeTaskId != null) {
            currentTotal = taskRepository.sumWeightPercentageExcludingTask(
                    professorId, courseId, semesterId, excludeTaskId);
        } else {
            currentTotal = taskRepository.sumWeightPercentageByProfessorAndCourseAndSemester(
                    professorId, courseId, semesterId);
        }
        
        int newTotal = currentTotal + newWeight;
        if (newTotal > 100) {
            throw new ValidationException(
                    String.format("Total weight percentage would be %d%%, must not exceed 100%%. Current total: %d%%", 
                            newTotal, currentTotal),
                    Map.of("weightPercentage", String.format("Adding %d%% would exceed the 100%% limit", newWeight)));
        }
    }
    
    /**
     * Log a status change to the audit log.
     */
    private void logStatusChange(Task task, TaskStatus oldStatus, TaskStatus newStatus, 
                                 User changedBy, String reason) {
        TaskAuditLog auditLog = new TaskAuditLog();
        auditLog.setTask(task);
        auditLog.setOldStatus(oldStatus);
        auditLog.setNewStatus(newStatus);
        auditLog.setChangedBy(changedBy);
        auditLog.setChangeReason(reason);
        taskAuditLogRepository.save(auditLog);
    }
    
    /**
     * Map Task entity to TaskDTO.
     */
    private TaskDTO mapToDTO(Task task) {
        TaskDTO.TaskDTOBuilder builder = TaskDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .weightPercentage(task.getWeightPercentage())
                .progressPercentage(task.getProgressPercentage())
                .status(task.getStatus())
                .deadline(task.getDeadline())
                .professorId(task.getProfessor().getId())
                .professorName(task.getProfessor().getFirstName() + " " + task.getProfessor().getLastName())
                .professorEmail(task.getProfessor().getEmail())
                .courseId(task.getCourse().getId())
                .courseCode(task.getCourse().getCourseCode())
                .courseName(task.getCourse().getCourseName())
                .semesterId(task.getSemester().getId())
                .semesterType(task.getSemester().getType().name())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .isOverdue(task.isOverdue())
                .canBeDeleted(task.canBeDeleted());
        
        // Department info
        if (task.getCourse().getDepartment() != null) {
            builder.departmentId(task.getCourse().getDepartment().getId())
                    .departmentName(task.getCourse().getDepartment().getName());
        }
        
        // Academic year info
        if (task.getSemester().getAcademicYear() != null) {
            builder.academicYear(task.getSemester().getAcademicYear().getYearCode());
        }
        
        // File reference
        if (task.getFileReference() != null) {
            builder.fileReferenceId(task.getFileReference().getId())
                    .fileReferenceName(task.getFileReference().getOriginalFilename())
                    .fileReferenceUrl(task.getFileReference().getFileUrl());
        }
        
        // Calculate days until deadline or days overdue
        if (task.getDeadline() != null) {
            LocalDate today = LocalDate.now();
            if (today.isAfter(task.getDeadline())) {
                builder.daysOverdue((int) ChronoUnit.DAYS.between(task.getDeadline(), today));
            } else {
                builder.daysUntilDeadline((int) ChronoUnit.DAYS.between(today, task.getDeadline()));
            }
        }
        
        return builder.build();
    }
}
