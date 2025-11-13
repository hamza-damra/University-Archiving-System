package com.alqude.edu.ArchiveSystem.config;

import com.alqude.edu.ArchiveSystem.entity.Department;
import com.alqude.edu.ArchiveSystem.entity.Role;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.repository.DepartmentRepository;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        initializeData();
    }
    
    private void initializeData() {
        log.info("Initializing application data...");
        
        // Create departments if they don't exist
        Department csDepartment = createDepartmentIfNotExists("Computer Science", "Department of Computer Science");
        Department mathDepartment = createDepartmentIfNotExists("Mathematics", "Department of Mathematics");
        Department physicsDepartment = createDepartmentIfNotExists("Physics", "Department of Physics");
        
        // Create default HOD users
        createUserIfNotExists(
                "hod.cs@alquds.edu", 
                "password123", 
                "Ahmad", 
                "Al-Rashid", 
                Role.ROLE_HOD, 
                csDepartment
        );
        
        createUserIfNotExists(
                "hod.math@alquds.edu", 
                "password123", 
                "Fatima", 
                "Al-Zahra", 
                Role.ROLE_HOD, 
                mathDepartment
        );
        
        // Create default professor users
        createUserIfNotExists(
                "prof.omar@alquds.edu", 
                "password123", 
                "Omar", 
                "Al-Khouri", 
                Role.ROLE_PROFESSOR, 
                csDepartment
        );
        
        createUserIfNotExists(
                "prof.layla@alquds.edu", 
                "password123", 
                "Layla", 
                "Al-Mansouri", 
                Role.ROLE_PROFESSOR, 
                csDepartment
        );
        
        createUserIfNotExists(
                "prof.hassan@alquds.edu", 
                "password123", 
                "Hassan", 
                "Al-Tamimi", 
                Role.ROLE_PROFESSOR, 
                mathDepartment
        );
        
        createUserIfNotExists(
                "prof.nour@alquds.edu", 
                "password123", 
                "Nour", 
                "Al-Qasemi", 
                Role.ROLE_PROFESSOR, 
                physicsDepartment
        );
        
        log.info("Data initialization completed successfully");
    }
    
    private Department createDepartmentIfNotExists(String name, String description) {
        return departmentRepository.findByName(name)
                .orElseGet(() -> {
                    Department department = new Department();
                    department.setName(name);
                    department.setDescription(description);
                    Department saved = departmentRepository.save(department);
                    log.info("Created department: {}", name);
                    return saved;
                });
    }
    
    private User createUserIfNotExists(String email, String password, String firstName, 
                                     String lastName, Role role, Department department) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail(email);
                    user.setPassword(passwordEncoder.encode(password));
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    user.setRole(role);
                    user.setDepartment(department);
                    user.setIsActive(true);
                    User saved = userRepository.save(user);
                    log.info("Created user: {} ({}) in department: {}", 
                            email, role, department.getName());
                    return saved;
                });
    }
}
