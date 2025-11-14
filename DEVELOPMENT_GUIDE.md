# 🛠️ Development Guide - University Archive System

## 📋 Table of Contents
1. [Getting Started](#getting-started)
2. [Development Workflow](#development-workflow)
3. [Creating New Features](#creating-new-features)
4. [Code Examples](#code-examples)
5. [Common Tasks](#common-tasks)
6. [Troubleshooting](#troubleshooting)
7. [Best Practices](#best-practices)

---

## 🚀 Getting Started

### Prerequisites
- **Java 17** or higher
- **Maven 3.6+**
- **MySQL 8.0** (or H2 for testing)
- **IDE:** IntelliJ IDEA, Eclipse, or VS Code with Java extensions
- **Git** for version control

### Initial Setup

#### 1. Clone and Build
```bash
# Clone repository
git clone <repository-url>
cd ArchiveSystem

# Build project
mvn clean install

# Run application
mvn spring-boot:run
```

#### 2. Database Setup
```sql
-- Create database
CREATE DATABASE archive_system;

-- Application will auto-create tables on first run
-- Default HOD account: hod@alquds.edu / password123
```

#### 3. Access Application
- **Frontend:** http://localhost:8080
- **API Base:** http://localhost:8080/api
- **H2 Console (if enabled):** http://localhost:8080/h2-console

---

## 🔄 Development Workflow

### 1. Create Feature Branch
```bash
git checkout -b feature/your-feature-name
```

### 2. Implement Feature
Follow the layered architecture:
1. **Entity** - Define domain model
2. **Repository** - Create data access interface
3. **DTO** - Define request/response objects
4. **Mapper** - Create entity-DTO mapper
5. **Service** - Implement business logic
6. **Controller** - Expose REST endpoints
7. **Exception** - Add custom exceptions if needed
8. **Frontend** - Update UI (if applicable)

### 3. Test Feature
```bash
# Run unit tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run with coverage
mvn clean test jacoco:report
```

### 4. Commit Changes
```bash
git add .
git commit -m "feat: Add feature description"
git push origin feature/your-feature-name
```

### 5. Create Pull Request
- Ensure all tests pass
- Update documentation if needed
- Request code review

---

## 🎯 Creating New Features

### Example: Adding a New Entity (Course)

#### Step 1: Create Entity
```java
// src/main/java/com/alqude/edu/ArchiveSystem/entity/Course.java
package com.alqude.edu.ArchiveSystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String code;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
    
    @Column(name = "credit_hours")
    private Integer creditHours;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

#### Step 2: Create Repository
```java
// src/main/java/com/alqude/edu/ArchiveSystem/repository/CourseRepository.java
package com.alqude.edu.ArchiveSystem.repository;

import com.alqude.edu.ArchiveSystem.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    Optional<Course> findByCode(String code);
    
    List<Course> findByDepartmentId(Long departmentId);
    
    @Query("SELECT c FROM Course c WHERE c.department.id = :departmentId AND c.name LIKE %:name%")
    List<Course> searchByDepartmentAndName(Long departmentId, String name);
}
```

#### Step 3: Create DTOs
```java
// src/main/java/com/alqude/edu/ArchiveSystem/dto/course/CourseCreateRequest.java
package com.alqude.edu.ArchiveSystem.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseCreateRequest {
    
    @NotBlank(message = "Course code is required")
    private String code;
    
    @NotBlank(message = "Course name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Department ID is required")
    private Long departmentId;
    
    @Positive(message = "Credit hours must be positive")
    private Integer creditHours;
}

// src/main/java/com/alqude/edu/ArchiveSystem/dto/course/CourseResponse.java
package com.alqude.edu.ArchiveSystem.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Long departmentId;
    private String departmentName;
    private Integer creditHours;
    private LocalDateTime createdAt;
}
```

#### Step 4: Create Mapper
```java
// src/main/java/com/alqude/edu/ArchiveSystem/mapper/CourseMapper.java
package com.alqude.edu.ArchiveSystem.mapper;

import com.alqude.edu.ArchiveSystem.dto.course.CourseCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.course.CourseResponse;
import com.alqude.edu.ArchiveSystem.entity.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Course toEntity(CourseCreateRequest request);
    
    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    CourseResponse toResponse(Course course);
}
```

#### Step 5: Create Service
```java
// src/main/java/com/alqude/edu/ArchiveSystem/service/CourseService.java
package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.course.CourseCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.course.CourseResponse;
import com.alqude.edu.ArchiveSystem.entity.Course;
import com.alqude.edu.ArchiveSystem.entity.Department;
import com.alqude.edu.ArchiveSystem.exception.DuplicateEntityException;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.mapper.CourseMapper;
import com.alqude.edu.ArchiveSystem.repository.CourseRepository;
import com.alqude.edu.ArchiveSystem.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {
    
    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseMapper courseMapper;
    
    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request) {
        log.info("Creating course with code: {}", request.getCode());
        
        // Check for duplicate code
        if (courseRepository.findByCode(request.getCode()).isPresent()) {
            throw new DuplicateEntityException("Course with code " + request.getCode() + " already exists");
        }
        
        // Verify department exists
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + request.getDepartmentId()));
        
        // Create course
        Course course = courseMapper.toEntity(request);
        course.setDepartment(department);
        
        Course savedCourse = courseRepository.save(course);
        log.info("Course created successfully with id: {}", savedCourse.getId());
        
        return courseMapper.toResponse(savedCourse);
    }
    
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        log.debug("Fetching course with id: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + id));
        return courseMapper.toResponse(course);
    }
    
    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByDepartment(Long departmentId) {
        log.debug("Fetching courses for department: {}", departmentId);
        return courseRepository.findByDepartmentId(departmentId).stream()
                .map(courseMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void deleteCourse(Long id) {
        log.info("Deleting course with id: {}", id);
        if (!courseRepository.existsById(id)) {
            throw new EntityNotFoundException("Course not found with id: " + id);
        }
        courseRepository.deleteById(id);
        log.info("Course deleted successfully: {}", id);
    }
}
```

#### Step 6: Create Controller
```java
// src/main/java/com/alqude/edu/ArchiveSystem/controller/CourseController.java
package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alqude.edu.ArchiveSystem.dto.course.CourseCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.course.CourseResponse;
import com.alqude.edu.ArchiveSystem.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hod/courses")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('HOD')")
public class CourseController {
    
    private final CourseService courseService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CourseCreateRequest request) {
        log.info("Request to create course: {}", request.getCode());
        CourseResponse course = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Course created successfully", course));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(@PathVariable Long id) {
        CourseResponse course = courseService.getCourseById(id);
        return ResponseEntity.ok(ApiResponse.success("Course retrieved", course));
    }
    
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getCoursesByDepartment(
            @PathVariable Long departmentId) {
        List<CourseResponse> courses = courseService.getCoursesByDepartment(departmentId);
        return ResponseEntity.ok(ApiResponse.success("Courses retrieved", courses));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.success("Course deleted successfully", null));
    }
}
```

#### Step 7: Create Tests
```java
// src/test/java/com/alqude/edu/ArchiveSystem/service/CourseServiceTest.java
package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.course.CourseCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.course.CourseResponse;
import com.alqude.edu.ArchiveSystem.entity.Course;
import com.alqude.edu.ArchiveSystem.entity.Department;
import com.alqude.edu.ArchiveSystem.exception.DuplicateEntityException;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.mapper.CourseMapper;
import com.alqude.edu.ArchiveSystem.repository.CourseRepository;
import com.alqude.edu.ArchiveSystem.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {
    
    @Mock
    private CourseRepository courseRepository;
    
    @Mock
    private DepartmentRepository departmentRepository;
    
    @Mock
    private CourseMapper courseMapper;
    
    @InjectMocks
    private CourseService courseService;
    
    private CourseCreateRequest createRequest;
    private Course course;
    private Department department;
    
    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setName("Computer Science");
        
        createRequest = new CourseCreateRequest();
        createRequest.setCode("CS101");
        createRequest.setName("Introduction to Programming");
        createRequest.setDepartmentId(1L);
        createRequest.setCreditHours(3);
        
        course = new Course();
        course.setId(1L);
        course.setCode("CS101");
        course.setName("Introduction to Programming");
        course.setDepartment(department);
        course.setCreditHours(3);
    }
    
    @Test
    void createCourse_Success() {
        // Given
        when(courseRepository.findByCode(createRequest.getCode())).thenReturn(Optional.empty());
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(courseMapper.toEntity(createRequest)).thenReturn(course);
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(courseMapper.toResponse(course)).thenReturn(new CourseResponse());
        
        // When
        CourseResponse response = courseService.createCourse(createRequest);
        
        // Then
        assertThat(response).isNotNull();
        verify(courseRepository).save(any(Course.class));
    }
    
    @Test
    void createCourse_DuplicateCode_ThrowsException() {
        // Given
        when(courseRepository.findByCode(createRequest.getCode())).thenReturn(Optional.of(course));
        
        // When & Then
        assertThatThrownBy(() -> courseService.createCourse(createRequest))
                .isInstanceOf(DuplicateEntityException.class)
                .hasMessageContaining("already exists");
    }
    
    @Test
    void createCourse_DepartmentNotFound_ThrowsException() {
        // Given
        when(courseRepository.findByCode(createRequest.getCode())).thenReturn(Optional.empty());
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> courseService.createCourse(createRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Department not found");
    }
}
```

---

## 💡 Code Examples

### Adding Custom Query to Repository
```java
@Repository
public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, Long> {
    
    // Method name query
    List<DocumentRequest> findByProfessorIdAndStatus(Long professorId, RequestStatus status);
    
    // JPQL query
    @Query("SELECT dr FROM DocumentRequest dr WHERE dr.deadline < :date AND dr.status = 'PENDING'")
    List<DocumentRequest> findOverdueRequests(@Param("date") LocalDate date);
    
    // Native SQL query
    @Query(value = "SELECT * FROM document_requests WHERE professor_id = ?1 ORDER BY deadline DESC LIMIT 10", 
           nativeQuery = true)
    List<DocumentRequest> findRecentRequestsByProfessor(Long professorId);
}
```

### Creating Custom Exception
```java
// src/main/java/com/alqude/edu/ArchiveSystem/exception/CourseException.java
package com.alqude.edu.ArchiveSystem.exception;

public class CourseException extends BusinessException {
    
    public CourseException(String message) {
        super(message);
    }
    
    public static CourseException courseNotFound(Long id) {
        return new CourseException("Course not found with id: " + id);
    }
    
    public static CourseException duplicateCourseCode(String code) {
        return new CourseException("Course with code " + code + " already exists");
    }
    
    public static CourseException invalidCreditHours(Integer hours) {
        return new CourseException("Invalid credit hours: " + hours);
    }
}
```

### Adding Validation
```java
// Custom validator annotation
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CourseCodeValidator.class)
public @interface ValidCourseCode {
    String message() default "Invalid course code format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Validator implementation
public class CourseCodeValidator implements ConstraintValidator<ValidCourseCode, String> {
    
    private static final Pattern COURSE_CODE_PATTERN = Pattern.compile("^[A-Z]{2,4}\\d{3}$");
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Use @NotNull for null check
        }
        return COURSE_CODE_PATTERN.matcher(value).matches();
    }
}

// Usage in DTO
@Data
public class CourseCreateRequest {
    
    @NotBlank
    @ValidCourseCode(message = "Course code must be in format: XX000 (e.g., CS101)")
    private String code;
}
```

---

## 🔧 Common Tasks

### Adding New API Endpoint

1. **Define in Controller:**
```java
@GetMapping("/statistics")
@PreAuthorize("hasRole('HOD')")
public ResponseEntity<ApiResponse<StatisticsResponse>> getStatistics() {
    StatisticsResponse stats = statisticsService.getOverallStatistics();
    return ResponseEntity.ok(ApiResponse.success("Statistics retrieved", stats));
}
```

2. **Update Frontend API Module:**
```javascript
// js/api.js
export const api = {
    hod: {
        // ... existing methods
        getStatistics: async () => {
            return await request('/api/hod/statistics', { method: 'GET' });
        }
    }
};
```

3. **Use in Frontend:**
```javascript
// js/hod.js
async function loadStatistics() {
    try {
        const response = await api.hod.getStatistics();
        if (response.success) {
            displayStatistics(response.data);
        }
    } catch (error) {
        ui.showToast('Failed to load statistics', 'error');
    }
}
```

### Database Migration with Flyway

1. **Create Migration File:**
```sql
-- src/main/resources/db/migration/V3__add_courses_table.sql
CREATE TABLE courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    department_id BIGINT NOT NULL,
    credit_hours INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
);

CREATE INDEX idx_courses_department ON courses(department_id);
CREATE INDEX idx_courses_code ON courses(code);
```

2. **Enable Flyway:**
```properties
# application.properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
```

### Adding Logging

```java
@Slf4j
@Service
public class CourseService {
    
    public CourseResponse createCourse(CourseCreateRequest request) {
        // Info level - important business events
        log.info("Creating course: code={}, department={}", 
                 request.getCode(), request.getDepartmentId());
        
        try {
            // Debug level - detailed flow information
            log.debug("Validating course code: {}", request.getCode());
            
            Course course = courseRepository.save(mappedCourse);
            
            // Info level - successful completion
            log.info("Course created successfully: id={}, code={}", 
                     course.getId(), course.getCode());
            
            return courseMapper.toResponse(course);
            
        } catch (Exception e) {
            // Error level - exceptions and failures
            log.error("Failed to create course: code={}, error={}", 
                      request.getCode(), e.getMessage(), e);
            throw e;
        }
    }
}
```

---

## 🐛 Troubleshooting

### Common Issues

#### 1. Circular Dependency Error
**Problem:** `BeanCurrentlyInCreationException`

**Solution:**
- Use method parameter injection instead of constructor injection for @Bean methods
- Extract beans to separate @Configuration classes
- Use `@Lazy` annotation (last resort)

```java
// ❌ Bad - causes circular dependency
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    // Uses passwordEncoder from constructor
}

// ✅ Good - method parameter injection
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http, PasswordEncoder passwordEncoder) {
    // Uses passwordEncoder from parameter
}
```

#### 2. LazyInitializationException
**Problem:** `could not initialize proxy - no Session`

**Solution:**
- Use `@Transactional` on service methods
- Use `FetchType.EAGER` (not recommended)
- Use JOIN FETCH in JPQL queries

```java
// ✅ Solution 1: @Transactional
@Transactional(readOnly = true)
public CourseResponse getCourse(Long id) {
    Course course = courseRepository.findById(id).orElseThrow();
    // Lazy-loaded department will be accessible
    return courseMapper.toResponse(course);
}

// ✅ Solution 2: JOIN FETCH
@Query("SELECT c FROM Course c JOIN FETCH c.department WHERE c.id = :id")
Optional<Course> findByIdWithDepartment(@Param("id") Long id);
```

#### 3. JWT Token Expired
**Problem:** 401 Unauthorized after some time

**Solution:**
- Implement token refresh mechanism
- Increase token expiration time
- Handle 401 in frontend and redirect to login

```javascript
// Frontend handling
if (response.status === 401) {
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    window.location.href = '/index.html';
}
```

#### 4. CORS Error
**Problem:** `Access-Control-Allow-Origin` error

**Solution:**
- Update CORS configuration in SecurityConfig
- Ensure frontend origin is allowed

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://yourdomain.com"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

---

## ✅ Best Practices

### 1. Always Use DTOs
```java
// ❌ Bad - exposing entity directly
@GetMapping("/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userRepository.findById(id).orElseThrow());
}

// ✅ Good - using DTO
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
    UserResponse user = userService.getUserById(id);
    return ResponseEntity.ok(ApiResponse.success("User retrieved", user));
}
```

### 2. Use Transactions Appropriately
```java
// ✅ Read-only for queries (optimization)
@Transactional(readOnly = true)
public List<CourseResponse> getAllCourses() {
    return courseRepository.findAll().stream()
            .map(courseMapper::toResponse)
            .collect(Collectors.toList());
}

// ✅ Default for writes
@Transactional
public CourseResponse createCourse(CourseCreateRequest request) {
    // Multiple database operations in one transaction
    Course course = courseMapper.toEntity(request);
    Course saved = courseRepository.save(course);
    notificationService.notifyNewCourse(saved);
    return courseMapper.toResponse(saved);
}
```

### 3. Validate Input
```java
// Backend validation
@PostMapping
public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
        @Valid @RequestBody CourseCreateRequest request) {
    // @Valid triggers validation
    CourseResponse course = courseService.createCourse(request);
    return ResponseEntity.ok(ApiResponse.success("Course created", course));
}

// Frontend validation
async function createCourse(courseData) {
    // Validate before sending
    if (!courseData.code || !courseData.name) {
        ui.showToast('Please fill all required fields', 'error');
        return;
    }
    
    if (!/^[A-Z]{2,4}\d{3}$/.test(courseData.code)) {
        ui.showToast('Invalid course code format', 'error');
        return;
    }
    
    // Send to backend
    const response = await api.hod.createCourse(courseData);
}
```

### 4. Handle Errors Gracefully
```java
// Service layer - throw specific exceptions
public CourseResponse getCourseById(Long id) {
    return courseRepository.findById(id)
            .map(courseMapper::toResponse)
            .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + id));
}

// GlobalExceptionHandler - catch and format
@ExceptionHandler(EntityNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException ex) {
    log.warn("Entity not found: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
}
```

### 5. Use Proper HTTP Status Codes
```java
// 200 OK - Successful GET, PUT, DELETE
return ResponseEntity.ok(ApiResponse.success("Retrieved", data));

// 201 Created - Successful POST
return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Created", data));

// 204 No Content - Successful DELETE with no response body
return ResponseEntity.noContent().build();

// 400 Bad Request - Validation error
return ResponseEntity.badRequest()
        .body(ApiResponse.error("Invalid input"));

// 404 Not Found - Resource not found
return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error("Not found"));

// 500 Internal Server Error - Unexpected error
return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("Internal error"));
```

---

## 📚 Additional Resources

- **Spring Boot Documentation:** https://spring.io/projects/spring-boot
- **Spring Security:** https://spring.io/projects/spring-security
- **MapStruct:** https://mapstruct.org/
- **Lombok:** https://projectlombok.org/
- **JUnit 5:** https://junit.org/junit5/
- **Tailwind CSS:** https://tailwindcss.com/

---

**Happy Coding! 🚀**
