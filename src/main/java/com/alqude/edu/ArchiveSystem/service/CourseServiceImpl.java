package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.academic.CourseAssignmentDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.CourseDTO;
import com.alqude.edu.ArchiveSystem.dto.academic.RequiredDocumentTypeDTO;
import com.alqude.edu.ArchiveSystem.entity.*;
import com.alqude.edu.ArchiveSystem.exception.BusinessException;
import com.alqude.edu.ArchiveSystem.exception.DuplicateEntityException;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourseServiceImpl implements CourseService {
    
    private final CourseRepository courseRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final RequiredDocumentTypeRepository requiredDocumentTypeRepository;
    private final DepartmentRepository departmentRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    
    // ==================== Course Management ====================
    
    @Override
    public Course createCourse(CourseDTO dto) {
        log.info("Creating course with code: {}", dto.getCourseCode());
        
        // Check if course code already exists
        if (courseRepository.findByCourseCode(dto.getCourseCode()).isPresent()) {
            throw new DuplicateEntityException("Course with code " + dto.getCourseCode() + " already exists");
        }
        
        // Validate department exists
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + dto.getDepartmentId()));
        
        Course course = new Course();
        course.setCourseCode(dto.getCourseCode());
        course.setCourseName(dto.getCourseName());
        course.setDepartment(department);
        course.setLevel(dto.getLevel());
        course.setDescription(dto.getDescription());
        course.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        Course savedCourse = courseRepository.save(course);
        log.info("Course created successfully with id: {}", savedCourse.getId());
        
        return savedCourse;
    }
    
    @Override
    public Course updateCourse(Long id, CourseDTO dto) {
        log.info("Updating course with id: {}", id);
        
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + id));
        
        // Check if course code is being changed and if it already exists
        if (!course.getCourseCode().equals(dto.getCourseCode())) {
            if (courseRepository.findByCourseCode(dto.getCourseCode()).isPresent()) {
                throw new DuplicateEntityException("Course with code " + dto.getCourseCode() + " already exists");
            }
            course.setCourseCode(dto.getCourseCode());
        }
        
        // Update department if changed
        if (!course.getDepartment().getId().equals(dto.getDepartmentId())) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + dto.getDepartmentId()));
            course.setDepartment(department);
        }
        
        course.setCourseName(dto.getCourseName());
        course.setLevel(dto.getLevel());
        course.setDescription(dto.getDescription());
        
        if (dto.getIsActive() != null) {
            course.setIsActive(dto.getIsActive());
        }
        
        Course updatedCourse = courseRepository.save(course);
        log.info("Course updated successfully with id: {}", updatedCourse.getId());
        
        return updatedCourse;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Course getCourse(Long id) {
        log.debug("Fetching course with id: {}", id);
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + id));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Course> getCoursesByDepartment(Long departmentId) {
        log.debug("Fetching courses for department id: {}", departmentId);
        
        // Validate department exists
        if (!departmentRepository.existsById(departmentId)) {
            throw new EntityNotFoundException("Department not found with id: " + departmentId);
        }
        
        return courseRepository.findByDepartmentId(departmentId);
    }
    
    @Override
    public void deactivateCourse(Long id) {
        log.info("Deactivating course with id: {}", id);
        
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + id));
        
        course.setIsActive(false);
        courseRepository.save(course);
        
        log.info("Course deactivated successfully with id: {}", id);
    }
    
    // ==================== Course Assignment Management ====================
    
    @Override
    public CourseAssignment assignCourse(CourseAssignmentDTO dto) {
        log.info("Assigning course {} to professor {} for semester {}", 
                dto.getCourseId(), dto.getProfessorId(), dto.getSemesterId());
        
        // Validate semester exists
        Semester semester = semesterRepository.findById(dto.getSemesterId())
                .orElseThrow(() -> new EntityNotFoundException("Semester not found with id: " + dto.getSemesterId()));
        
        // Validate course exists and is active
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + dto.getCourseId()));
        
        if (!course.getIsActive()) {
            throw new BusinessException("INACTIVE_COURSE", "Cannot assign inactive course");
        }
        
        // Validate professor exists and has PROFESSOR role
        User professor = userRepository.findById(dto.getProfessorId())
                .orElseThrow(() -> new EntityNotFoundException("Professor not found with id: " + dto.getProfessorId()));
        
        if (professor.getRole() != Role.ROLE_PROFESSOR) {
            throw new BusinessException("INVALID_ROLE", "User is not a professor");
        }
        
        if (!professor.getIsActive()) {
            throw new BusinessException("INACTIVE_PROFESSOR", "Cannot assign course to inactive professor");
        }
        
        // Check if assignment already exists
        try {
            CourseAssignment assignment = new CourseAssignment();
            assignment.setSemester(semester);
            assignment.setCourse(course);
            assignment.setProfessor(professor);
            assignment.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
            
            CourseAssignment savedAssignment = courseAssignmentRepository.save(assignment);
            log.info("Course assignment created successfully with id: {}", savedAssignment.getId());
            
            return savedAssignment;
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEntityException("Course assignment already exists for this semester, course, and professor combination");
        }
    }
    
    @Override
    public void unassignCourse(Long assignmentId) {
        log.info("Unassigning course with assignment id: {}", assignmentId);
        
        CourseAssignment assignment = courseAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Course assignment not found with id: " + assignmentId));
        
        courseAssignmentRepository.delete(assignment);
        
        log.info("Course assignment deleted successfully with id: {}", assignmentId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CourseAssignment> getAssignmentsBySemester(Long semesterId) {
        log.debug("Fetching course assignments for semester id: {}", semesterId);
        
        // Validate semester exists
        if (!semesterRepository.existsById(semesterId)) {
            throw new EntityNotFoundException("Semester not found with id: " + semesterId);
        }
        
        return courseAssignmentRepository.findBySemesterId(semesterId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CourseAssignment> getAssignmentsByProfessor(Long professorId, Long semesterId) {
        log.debug("Fetching course assignments for professor id: {} and semester id: {}", professorId, semesterId);
        
        // Validate professor exists
        if (!userRepository.existsById(professorId)) {
            throw new EntityNotFoundException("Professor not found with id: " + professorId);
        }
        
        // Validate semester exists
        if (!semesterRepository.existsById(semesterId)) {
            throw new EntityNotFoundException("Semester not found with id: " + semesterId);
        }
        
        return courseAssignmentRepository.findByProfessorIdAndSemesterId(professorId, semesterId);
    }
    
    // ==================== Required Document Type Management ====================
    
    @Override
    public RequiredDocumentType addRequiredDocumentType(Long courseId, RequiredDocumentTypeDTO dto) {
        log.info("Adding required document type {} for course id: {}", dto.getDocumentType(), courseId);
        
        // Validate course exists
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));
        
        // Validate semester if provided
        Semester semester = null;
        if (dto.getSemesterId() != null) {
            semester = semesterRepository.findById(dto.getSemesterId())
                    .orElseThrow(() -> new EntityNotFoundException("Semester not found with id: " + dto.getSemesterId()));
        }
        
        RequiredDocumentType requiredDocumentType = new RequiredDocumentType();
        requiredDocumentType.setCourse(course);
        requiredDocumentType.setSemester(semester);
        requiredDocumentType.setDocumentType(dto.getDocumentType());
        requiredDocumentType.setDeadline(dto.getDeadline());
        requiredDocumentType.setIsRequired(dto.getIsRequired() != null ? dto.getIsRequired() : true);
        requiredDocumentType.setMaxFileCount(dto.getMaxFileCount() != null ? dto.getMaxFileCount() : 5);
        requiredDocumentType.setMaxTotalSizeMb(dto.getMaxTotalSizeMb() != null ? dto.getMaxTotalSizeMb() : 50);
        
        if (dto.getAllowedFileExtensions() != null && !dto.getAllowedFileExtensions().isEmpty()) {
            requiredDocumentType.setAllowedFileExtensions(dto.getAllowedFileExtensions());
        }
        
        RequiredDocumentType savedDocumentType = requiredDocumentTypeRepository.save(requiredDocumentType);
        log.info("Required document type created successfully with id: {}", savedDocumentType.getId());
        
        return savedDocumentType;
    }
    
    @Override
    public RequiredDocumentType updateRequiredDocumentType(Long id, RequiredDocumentTypeDTO dto) {
        log.info("Updating required document type with id: {}", id);
        
        RequiredDocumentType requiredDocumentType = requiredDocumentTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Required document type not found with id: " + id));
        
        // Update semester if provided
        if (dto.getSemesterId() != null) {
            Semester semester = semesterRepository.findById(dto.getSemesterId())
                    .orElseThrow(() -> new EntityNotFoundException("Semester not found with id: " + dto.getSemesterId()));
            requiredDocumentType.setSemester(semester);
        }
        
        requiredDocumentType.setDocumentType(dto.getDocumentType());
        requiredDocumentType.setDeadline(dto.getDeadline());
        
        if (dto.getIsRequired() != null) {
            requiredDocumentType.setIsRequired(dto.getIsRequired());
        }
        
        if (dto.getMaxFileCount() != null) {
            requiredDocumentType.setMaxFileCount(dto.getMaxFileCount());
        }
        
        if (dto.getMaxTotalSizeMb() != null) {
            requiredDocumentType.setMaxTotalSizeMb(dto.getMaxTotalSizeMb());
        }
        
        if (dto.getAllowedFileExtensions() != null) {
            requiredDocumentType.setAllowedFileExtensions(dto.getAllowedFileExtensions());
        }
        
        RequiredDocumentType updatedDocumentType = requiredDocumentTypeRepository.save(requiredDocumentType);
        log.info("Required document type updated successfully with id: {}", updatedDocumentType.getId());
        
        return updatedDocumentType;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RequiredDocumentType> getRequiredDocumentTypes(Long courseId, Long semesterId) {
        log.debug("Fetching required document types for course id: {} and semester id: {}", courseId, semesterId);
        
        // Validate course exists
        if (!courseRepository.existsById(courseId)) {
            throw new EntityNotFoundException("Course not found with id: " + courseId);
        }
        
        if (semesterId != null) {
            // Validate semester exists
            if (!semesterRepository.existsById(semesterId)) {
                throw new EntityNotFoundException("Semester not found with id: " + semesterId);
            }
            return requiredDocumentTypeRepository.findByCourseIdAndSemesterId(courseId, semesterId);
        } else {
            return requiredDocumentTypeRepository.findByCourseId(courseId);
        }
    }
}
