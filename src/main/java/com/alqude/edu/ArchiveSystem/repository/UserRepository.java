package com.alqude.edu.ArchiveSystem.repository;

import com.alqude.edu.ArchiveSystem.entity.Role;
import com.alqude.edu.ArchiveSystem.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.department WHERE u.email = :email")
    Optional<User> findByEmailWithDepartment(@Param("email") String email);
    
    /**
     * Find all users with department eagerly loaded to avoid LazyInitializationException.
     */
    @Query(value = "SELECT u FROM User u LEFT JOIN FETCH u.department",
           countQuery = "SELECT COUNT(u) FROM User u")
    Page<User> findAllWithDepartment(Pageable pageable);
    
    /**
     * Find users by role with department eagerly loaded.
     */
    @Query(value = "SELECT u FROM User u LEFT JOIN FETCH u.department WHERE u.role = :role",
           countQuery = "SELECT COUNT(u) FROM User u WHERE u.role = :role")
    Page<User> findByRoleWithDepartment(@Param("role") Role role, Pageable pageable);
    
    /**
     * Find users by role and active status with department eagerly loaded.
     */
    @Query(value = "SELECT u FROM User u LEFT JOIN FETCH u.department WHERE u.role = :role AND u.isActive = :isActive",
           countQuery = "SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.isActive = :isActive")
    Page<User> findByRoleAndIsActiveWithDepartment(@Param("role") Role role, @Param("isActive") Boolean isActive, Pageable pageable);
    
    /**
     * Find users by active status with department eagerly loaded.
     */
    @Query(value = "SELECT u FROM User u LEFT JOIN FETCH u.department WHERE u.isActive = :isActive",
           countQuery = "SELECT COUNT(u) FROM User u WHERE u.isActive = :isActive")
    Page<User> findByIsActiveWithDepartment(@Param("isActive") Boolean isActive, Pageable pageable);
    
    /**
     * Find users by department with department eagerly loaded.
     */
    @Query(value = "SELECT u FROM User u LEFT JOIN FETCH u.department WHERE u.department.id = :departmentId",
           countQuery = "SELECT COUNT(u) FROM User u WHERE u.department.id = :departmentId")
    Page<User> findByDepartmentIdWithDepartment(@Param("departmentId") Long departmentId, Pageable pageable);
    
    /**
     * Find users by role and department with department eagerly loaded.
     */
    @Query(value = "SELECT u FROM User u LEFT JOIN FETCH u.department WHERE u.role = :role AND u.department.id = :departmentId",
           countQuery = "SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.department.id = :departmentId")
    Page<User> findByRoleAndDepartmentIdWithDepartment(@Param("role") Role role, @Param("departmentId") Long departmentId, Pageable pageable);
    
    /**
     * Find users by role, department, and active status with department eagerly loaded.
     */
    @Query(value = "SELECT u FROM User u LEFT JOIN FETCH u.department WHERE u.role = :role AND u.department.id = :departmentId AND u.isActive = :isActive",
           countQuery = "SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.department.id = :departmentId AND u.isActive = :isActive")
    Page<User> findByRoleAndDepartmentIdAndIsActiveWithDepartment(@Param("role") Role role, @Param("departmentId") Long departmentId, @Param("isActive") Boolean isActive, Pageable pageable);
    
    /**
     * Find users by department and active status with department eagerly loaded.
     */
    @Query(value = "SELECT u FROM User u LEFT JOIN FETCH u.department WHERE u.department.id = :departmentId AND u.isActive = :isActive",
           countQuery = "SELECT COUNT(u) FROM User u WHERE u.department.id = :departmentId AND u.isActive = :isActive")
    Page<User> findByDepartmentIdAndIsActiveWithDepartment(@Param("departmentId") Long departmentId, @Param("isActive") Boolean isActive, Pageable pageable);
    
    boolean existsByEmail(String email);
    
    List<User> findByDepartmentIdAndRole(Long departmentId, Role role);
    
    Page<User> findByDepartmentIdAndRole(Long departmentId, Role role, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.department.id = :departmentId AND u.role = :role AND u.isActive = true")
    List<User> findActiveProfessorsByDepartment(@Param("departmentId") Long departmentId, @Param("role") Role role);
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    Page<User> findActiveUsersByRole(@Param("role") Role role, Pageable pageable);
    
    Optional<User> findByProfessorId(String professorId);
    
    List<User> findByRole(Role role);
    
    long countByRole(Role role);
    
    // ==================== Dashboard Analytics Queries ====================
    
    /**
     * Count professors (active) by department
     */
    @Query("SELECT u.department.id, u.department.name, COUNT(u) " +
           "FROM User u " +
           "WHERE u.role = 'ROLE_PROFESSOR' AND u.isActive = true " +
           "GROUP BY u.department.id, u.department.name")
    List<Object[]> countActiveProfessorsByDepartment();
    
    /**
     * Count active professors
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'ROLE_PROFESSOR' AND u.isActive = true")
    long countActiveProfessors();
}
