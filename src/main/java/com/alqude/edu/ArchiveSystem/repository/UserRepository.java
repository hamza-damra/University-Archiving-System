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
    
    boolean existsByEmail(String email);
    
    List<User> findByDepartmentIdAndRole(Long departmentId, Role role);
    
    Page<User> findByDepartmentIdAndRole(Long departmentId, Role role, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.department.id = :departmentId AND u.role = :role AND u.isActive = true")
    List<User> findActiveProfessorsByDepartment(@Param("departmentId") Long departmentId, @Param("role") Role role);
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    Page<User> findActiveUsersByRole(@Param("role") Role role, Pageable pageable);
    
    Optional<User> findByProfessorId(String professorId);
}
