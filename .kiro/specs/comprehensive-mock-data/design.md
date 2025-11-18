# Design Document

## Overview

This design document outlines the comprehensive mock data generation system for the University Archive System. The system will enhance the existing `DataInitializer` component to create realistic test data covering all entities in both the legacy request-based system and the new semester-based system. The mock data will enable thorough testing, demonstration, and development of all system features.

## Architecture

### Component Structure

```
DataInitializer (Spring Boot CommandLineRunner)
├── Entity Creation Methods
│   ├── Academic Structure
│   │   ├── createAcademicYears()
│   │   ├── createSemesters()
│   │   ├── createDepartments()
│   │   └── createCourses()
│   ├── User Management
│   │   ├── createHODUsers()
│   │   ├── createProfessorUsers()
│   │   └── createDeanshipUsers()
│   ├── Course System
│   │   ├── createCourseAssignments()
│   │   └── createRequiredDocumentTypes()
│   ├── Document System
│   │   ├── createDocumentSubmissions()
│   │   └── createUploadedFiles()
│   └── Notifications
│       └── createNotifications()
├── Utility Methods
│   ├── checkExistingData()
│   ├── generateRealisticNames()
│   ├── calculateDeadlines()
│   └── logCreationSummary()
└── Configuration
    └── MockDataProperties
```

### Execution Flow

```
Application Startup
        │
        ▼
CommandLineRunner.run()
        │
        ▼
Check if data exists
        │
        ├─[Data exists]──> Skip creation, log message
        │
        └─[No data]──────> Begin creation
                           │
                           ▼
                    Create in order:
                    1. Academic Years (3 years)
                    2. Semesters (9 total: 3 per year)
                    3. Departments (5 departments)
                    4. Courses (15 courses)
                    5. HOD Users (5 users, 1 per dept)
                    6. Professor Users (25 users)
                    7. Course Assignments (60 assignments)
                    8. Required Document Types (90 types)
                    9. Document Submissions (100 submissions)
                    10. Uploaded Files (150 files)
                    11. Notifications (75 notifications)
                           │
                           ▼
                    Log summary statistics
                           │
                           ▼
                    Complete
```

## Components and Interfaces

### 1. DataInitializer Enhancement

**Purpose:** Enhanced Spring Boot component that creates comprehensive mock data on application startup

**Dependencies:**
- All entity repositories (AcademicYearRepository, SemesterRepository, etc.)
- PasswordEncoder for user password hashing
- Logger for tracking creation progress

**Key Methods:**

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    // Check if data already exists
    private boolean hasExistingData();
    
    // Academic structure creation
    private List<AcademicYear> createAcademicYears();
    private List<Semester> createSemesters(List<AcademicYear> years);
    private List<Department> createDepartments();
    private List<Course> createCourses(List<Department> departments);
    
    // User creation
    private List<User> createHODUsers(List<Department> departments);
    private List<User> createProfessorUsers(List<Department> departments);
    
    // Course system
    private List<CourseAssignment> createCourseAssignments(
        List<Semester> semesters, 
        List<Course> courses, 
        List<User> professors
    );
    private List<RequiredDocumentType> createRequiredDocumentTypes(
        List<Course> courses, 
        List<Semester> semesters
    );
    
    // Document system
    private List<DocumentSubmission> createDocumentSubmissions(
        List<CourseAssignment> assignments, 
        List<User> professors
    );
    private List<UploadedFile> createUploadedFiles(
        List<DocumentSubmission> submissions
    );
    
    // Notifications
    private List<Notification> createNotifications(
        List<User> professors, 
        List<CourseAssignment> assignments
    );
    
    // Utilities
    private void logCreationSummary(Map<String, Integer> counts);
}
```

### 2. Mock Data Configuration

**Purpose:** Configuration properties to control mock data generation

**Properties:**
```properties
# application.properties
mock.data.enabled=true
mock.data.academic-years=3
mock.data.departments=5
mock.data.courses-per-department=3
mock.data.professors-per-department=5
mock.data.assignments-per-semester=20
mock.data.submissions-percentage=70
```

### 3. Name Generator Utility

**Purpose:** Generate realistic Arabic and English names for users

**Implementation:**
```java
public class NameGenerator {
    private static final List<String> FIRST_NAMES_MALE = Arrays.asList(
        "Ahmad", "Omar", "Hassan", "Khalid", "Tariq", "Yusuf", "Ibrahim", "Mahmoud"
    );
    
    private static final List<String> FIRST_NAMES_FEMALE = Arrays.asList(
        "Fatima", "Layla", "Nour", "Amira", "Zainab", "Huda", "Rania", "Maryam"
    );
    
    private static final List<String> LAST_NAMES = Arrays.asList(
        "Al-Rashid", "Al-Khouri", "Al-Mansouri", "Al-Tamimi", "Al-Qasemi",
        "Al-Najjar", "Al-Masri", "Al-Shami", "Al-Halabi", "Al-Baghdadi"
    );
    
    public static String generateFullName(boolean isMale);
    public static String generateEmail(String firstName, String lastName, String dept);
}
```

## Data Models

### Academic Year Data

```java
// 3 Academic Years
AcademicYear {
    yearCode: "2023-2024", "2024-2025", "2025-2026"
    startYear: 2023, 2024, 2025
    endYear: 2024, 2025, 2026
    isActive: true (current year), false (others)
}
```

### Semester Data

```java
// 9 Semesters (3 per academic year)
Semester {
    academicYear: Reference to AcademicYear
    type: FIRST, SECOND, SUMMER
    startDate: Calculated based on type
    endDate: Calculated based on type
    isActive: true (current semester), false (others)
}

// Date Calculations:
// FIRST (Fall): September 1 - January 15
// SECOND (Spring): February 1 - June 15
// SUMMER: July 1 - August 31
```

### Department Data

```java
// 5 Departments
Department {
    name: "Computer Science", "Mathematics", "Physics", 
          "Engineering", "Business Administration"
    description: Detailed description of department
}
```

### Course Data

```java
// 15 Courses (3 per department)
Course {
    courseCode: "CS101", "CS201", "CS301", "MATH101", etc.
    courseName: "Introduction to Programming", "Data Structures", etc.
    department: Reference to Department
    level: "Undergraduate" or "Graduate"
    description: Course description
    isActive: true
}

// Course Distribution:
// - Computer Science: CS101, CS201, CS301
// - Mathematics: MATH101, MATH201, MATH301
// - Physics: PHYS101, PHYS201, PHYS301
// - Engineering: ENG101, ENG201, ENG301
// - Business: BUS101, BUS201, BUS301
```

### User Data

```java
// 5 HOD Users (1 per department)
User {
    email: "hod.cs@alquds.edu", "hod.math@alquds.edu", etc.
    password: BCrypt("password123")
    firstName: Generated from NameGenerator
    lastName: Generated from NameGenerator
    role: ROLE_HOD
    department: Reference to Department
    professorId: null (HODs don't have professor IDs)
    isActive: true
}

// 25 Professor Users (5 per department)
User {
    email: "prof.{firstName}.{lastName}@alquds.edu"
    password: BCrypt("password123")
    firstName: Generated from NameGenerator
    lastName: Generated from NameGenerator
    role: ROLE_PROFESSOR
    department: Reference to Department
    professorId: "P{departmentCode}{sequentialNumber}" (e.g., "PCS001")
    isActive: 80% true, 20% false (for testing filters)
}
```

### Course Assignment Data

```java
// 60 Course Assignments
// Distribution: Each course assigned 2 times per semester
// Each professor gets 2-4 courses per semester
CourseAssignment {
    semester: Reference to Semester
    course: Reference to Course
    professor: Reference to User (ROLE_PROFESSOR)
    isActive: true
}

// Constraints:
// - Unique combination of (semester, course, professor)
// - Professors only assigned to courses in their department
// - Even distribution across semesters
```

### Required Document Type Data

```java
// 90 Required Document Types
// Distribution: 6 document types per course
RequiredDocumentType {
    course: Reference to Course
    semester: Reference to Semester (optional, null for course-wide)
    documentType: SYLLABUS, EXAM, ASSIGNMENT, PROJECT_DOCS, 
                  LECTURE_NOTES, OTHER
    deadline: Calculated based on semester dates and document type
    isRequired: true
    maxFileCount: 5
    maxTotalSizeMb: 50
    allowedFileExtensions: ["pdf", "docx", "zip"] (varies by type)
}

// Deadline Calculations:
// - SYLLABUS: 2 weeks after semester start
// - EXAM: Mid-semester for midterm, end for final
// - ASSIGNMENT: Throughout semester (weekly)
// - PROJECT_DOCS: End of semester
// - LECTURE_NOTES: Weekly throughout semester
// - OTHER: Flexible
```

### Document Submission Data

```java
// 100 Document Submissions
// Distribution: 70% submitted, 20% not uploaded, 10% overdue
DocumentSubmission {
    courseAssignment: Reference to CourseAssignment
    documentType: SYLLABUS, EXAM, ASSIGNMENT, etc.
    professor: Reference to User (same as courseAssignment.professor)
    uploadedFiles: List of UploadedFile references
    submittedAt: Calculated based on status
    isLateSubmission: 15% true, 85% false
    status: NOT_UPLOADED (20%), UPLOADED (70%), OVERDUE (10%)
    notes: Optional submission notes
    fileCount: 1-5 files
    totalFileSize: Sum of all file sizes
}

// Status Distribution Logic:
// - NOT_UPLOADED: No files, submittedAt is null
// - UPLOADED: Has files, submittedAt before deadline
// - OVERDUE: Has files, submittedAt after deadline OR no files after deadline
```

### Uploaded File Data

```java
// 150 Uploaded Files
// Distribution: 1-3 files per submission (only for UPLOADED/OVERDUE)
UploadedFile {
    documentSubmission: Reference to DocumentSubmission
    fileUrl: "uploads/{submissionId}_{uuid}_{timestamp}.{ext}"
    originalFilename: "{documentType}_{courseCode}.{ext}"
    fileSize: Random between 100KB and 10MB
    fileType: MIME type based on extension
    fileOrder: Sequential order within submission
    description: Optional file description
}

// File Extensions by Document Type:
// - SYLLABUS: pdf, docx
// - EXAM: pdf
// - ASSIGNMENT: pdf, zip
// - PROJECT_DOCS: zip, pdf
// - LECTURE_NOTES: pdf, pptx
// - OTHER: pdf, docx, zip
```

### Notification Data

```java
// 75 Notifications
// Distribution: 3 notifications per professor on average
Notification {
    user: Reference to User (ROLE_PROFESSOR)
    title: Generated based on type
    message: Generated based on type and related entity
    type: NEW_REQUEST, REQUEST_REMINDER, DEADLINE_APPROACHING,
          DOCUMENT_SUBMITTED, DOCUMENT_OVERDUE
    isRead: 60% true, 40% false
    relatedEntityId: ID of related CourseAssignment or DocumentSubmission
    relatedEntityType: "CourseAssignment" or "DocumentSubmission"
    createdAt: Distributed over last 30 days
}

// Notification Type Distribution:
// - NEW_REQUEST: 30% (when course assigned)
// - REQUEST_REMINDER: 20% (periodic reminders)
// - DEADLINE_APPROACHING: 25% (3 days before deadline)
// - DOCUMENT_SUBMITTED: 15% (after submission)
// - DOCUMENT_OVERDUE: 10% (after deadline passed)
```

## Error Handling

### Idempotency Strategy

```java
private boolean hasExistingData() {
    // Check if any core entities exist
    long academicYearCount = academicYearRepository.count();
    long departmentCount = departmentRepository.count();
    long userCount = userRepository.count();
    
    if (academicYearCount > 0 || departmentCount > 0 || userCount > 0) {
        log.info("Existing data detected. Skipping mock data generation.");
        log.info("Academic Years: {}, Departments: {}, Users: {}", 
                 academicYearCount, departmentCount, userCount);
        return true;
    }
    return false;
}
```

### Error Recovery

```java
@Override
public void run(String... args) {
    try {
        if (hasExistingData()) {
            return;
        }
        
        log.info("Starting comprehensive mock data generation...");
        
        // Create entities in dependency order with error handling
        List<AcademicYear> years = createAcademicYears();
        List<Semester> semesters = createSemesters(years);
        List<Department> departments = createDepartments();
        List<Course> courses = createCourses(departments);
        
        // Continue with remaining entities...
        
        logCreationSummary(counts);
        
    } catch (Exception e) {
        log.error("Error during mock data generation", e);
        log.error("Partial data may have been created. " +
                  "Clear database and restart to regenerate.");
    }
}
```

### Validation

```java
private void validateEntityCreation(String entityType, int expected, int actual) {
    if (actual < expected) {
        log.warn("Expected {} {} but only created {}", 
                 expected, entityType, actual);
    } else {
        log.info("Successfully created {} {}", actual, entityType);
    }
}
```

## Testing Strategy

### Unit Testing

```java
@SpringBootTest
class DataInitializerTest {
    
    @Autowired
    private DataInitializer dataInitializer;
    
    @Autowired
    private List<JpaRepository> allRepositories;
    
    @Test
    void testMockDataGeneration() {
        // Given: Clean database
        clearAllData();
        
        // When: Run data initializer
        dataInitializer.run();
        
        // Then: Verify all entities created
        assertThat(academicYearRepository.count()).isEqualTo(3);
        assertThat(semesterRepository.count()).isEqualTo(9);
        assertThat(departmentRepository.count()).isEqualTo(5);
        assertThat(courseRepository.count()).isEqualTo(15);
        assertThat(userRepository.count()).isEqualTo(30); // 5 HODs + 25 Professors
        assertThat(courseAssignmentRepository.count()).isGreaterThanOrEqualTo(60);
        assertThat(documentSubmissionRepository.count()).isGreaterThanOrEqualTo(100);
    }
    
    @Test
    void testIdempotency() {
        // Given: Existing data
        dataInitializer.run();
        long initialCount = userRepository.count();
        
        // When: Run again
        dataInitializer.run();
        
        // Then: No duplicate data
        assertThat(userRepository.count()).isEqualTo(initialCount);
    }
    
    @Test
    void testDataRelationships() {
        // Given: Generated data
        dataInitializer.run();
        
        // When: Query relationships
        List<CourseAssignment> assignments = courseAssignmentRepository.findAll();
        
        // Then: All relationships valid
        for (CourseAssignment assignment : assignments) {
            assertThat(assignment.getSemester()).isNotNull();
            assertThat(assignment.getCourse()).isNotNull();
            assertThat(assignment.getProfessor()).isNotNull();
            assertThat(assignment.getProfessor().getRole())
                .isEqualTo(Role.ROLE_PROFESSOR);
            assertThat(assignment.getCourse().getDepartment())
                .isEqualTo(assignment.getProfessor().getDepartment());
        }
    }
}
```

### Integration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
class MockDataIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @WithMockUser(roles = "HOD")
    void testHODCanAccessAllProfessors() throws Exception {
        // Given: Mock data exists
        
        // When: HOD requests professor list
        mockMvc.perform(get("/api/hod/professors"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(25));
    }
    
    @Test
    @WithMockUser(username = "prof.omar@alquds.edu", roles = "PROFESSOR")
    void testProfessorCanAccessAssignedCourses() throws Exception {
        // When: Professor requests their assignments
        mockMvc.perform(get("/api/professor/assignments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").isGreaterThan(0));
    }
}
```

### Manual Testing Scenarios

1. **Login Testing**
   - Test login with each HOD account
   - Test login with multiple professor accounts
   - Verify role-based dashboard access

2. **Data Browsing**
   - Browse all departments and verify professors
   - View course assignments per semester
   - Check document submission statuses

3. **Filtering and Search**
   - Filter professors by department
   - Filter submissions by status
   - Search courses by code or name

4. **Reporting**
   - Generate department submission reports
   - Generate professor-specific reports
   - Verify data accuracy in reports

## Documentation

### Mock Accounts Documentation

Create `MOCK_ACCOUNTS.md`:

```markdown
# Mock Accounts

## Default Password
All accounts use password: `password123`

## HOD Accounts
| Email | Name | Department |
|-------|------|------------|
| hod.cs@alquds.edu | Ahmad Al-Rashid | Computer Science |
| hod.math@alquds.edu | Fatima Al-Zahra | Mathematics |
| hod.physics@alquds.edu | Hassan Al-Tamimi | Physics |
| hod.eng@alquds.edu | Omar Al-Khouri | Engineering |
| hod.bus@alquds.edu | Layla Al-Mansouri | Business Administration |

## Professor Accounts (Sample)
| Email | Name | Department | Professor ID | Status |
|-------|------|------------|--------------|--------|
| prof.ahmad.alnajjar@alquds.edu | Ahmad Al-Najjar | Computer Science | PCS001 | Active |
| prof.fatima.almasri@alquds.edu | Fatima Al-Masri | Computer Science | PCS002 | Active |
...

## Testing Scenarios
1. Login as HOD to manage professors and view reports
2. Login as Professor to view assignments and submit documents
3. Test department filtering with different HOD accounts
4. Test submission workflows with various professor accounts
```

### API Testing Guide

Create `MOCK_DATA_API_TESTING.md`:

```markdown
# API Testing with Mock Data

## Authentication
```bash
# Login as HOD
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"hod.cs@alquds.edu","password":"password123"}'

# Login as Professor
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"prof.ahmad.alnajjar@alquds.edu","password":"password123"}'
```

## HOD Endpoints
```bash
# Get all professors
curl -X GET http://localhost:8080/api/hod/professors \
  -H "Authorization: Bearer {token}"

# Get department report
curl -X GET http://localhost:8080/api/hod/reports/department?departmentId=1 \
  -H "Authorization: Bearer {token}"
```

## Professor Endpoints
```bash
# Get my assignments
curl -X GET http://localhost:8080/api/professor/assignments \
  -H "Authorization: Bearer {token}"

# Submit document
curl -X POST http://localhost:8080/api/professor/submissions/{assignmentId} \
  -H "Authorization: Bearer {token}" \
  -F "file=@document.pdf"
```
```

## Configuration

### Application Properties

```properties
# Mock Data Configuration
mock.data.enabled=${MOCK_DATA_ENABLED:true}
mock.data.skip-if-exists=true

# Logging
logging.level.com.alqude.edu.ArchiveSystem.config.DataInitializer=INFO

# Development Profile
spring.profiles.active=dev
```

### Environment-Specific Behavior

```java
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!prod") // Don't run in production
public class DataInitializer implements CommandLineRunner {
    
    @Value("${mock.data.enabled:true}")
    private boolean mockDataEnabled;
    
    @Override
    public void run(String... args) {
        if (!mockDataEnabled) {
            log.info("Mock data generation is disabled");
            return;
        }
        
        // Proceed with data generation
        initializeData();
    }
}
```

## Performance Considerations

### Batch Processing

```java
private List<User> createProfessorUsers(List<Department> departments) {
    List<User> professors = new ArrayList<>();
    
    // Create all users in memory first
    for (Department dept : departments) {
        for (int i = 0; i < 5; i++) {
            User professor = new User();
            // Set properties...
            professors.add(professor);
        }
    }
    
    // Batch save all at once
    List<User> saved = userRepository.saveAll(professors);
    log.info("Created {} professors in batch", saved.size());
    
    return saved;
}
```

### Transaction Management

```java
@Transactional
public void initializeData() {
    // All data creation happens in single transaction
    // Rollback if any error occurs
}
```

### Lazy Loading Considerations

```java
// Avoid N+1 queries when creating relationships
@Transactional
private List<CourseAssignment> createCourseAssignments(
    List<Semester> semesters,
    List<Course> courses,
    List<User> professors) {
    
    // Fetch all entities with necessary relationships
    List<Semester> semestersWithYear = semesterRepository
        .findAllWithAcademicYear();
    List<Course> coursesWithDept = courseRepository
        .findAllWithDepartment();
    
    // Create assignments efficiently
    // ...
}
```

## Security Considerations

### Password Security

```java
// All mock passwords are hashed with BCrypt
private User createUser(...) {
    user.setPassword(passwordEncoder.encode("password123"));
    // Never store plain text passwords
}
```

### Production Safety

```java
@Profile("!prod") // Prevent running in production
@ConditionalOnProperty(name = "mock.data.enabled", havingValue = "true")
public class DataInitializer implements CommandLineRunner {
    // Implementation
}
```

### Documentation Warning

```markdown
# ⚠️ SECURITY WARNING

The mock accounts use a default password: `password123`

**NEVER use these accounts in production!**

- Change all passwords before deploying to production
- Disable mock data generation in production environment
- Use strong, unique passwords for all real accounts
```

## Summary

This design provides a comprehensive mock data generation system that:

1. Creates realistic test data for all entities
2. Maintains referential integrity across relationships
3. Supports idempotent execution
4. Provides detailed logging and documentation
5. Enables thorough testing of all system features
6. Follows Spring Boot best practices
7. Includes safety measures for production environments

The implementation will enhance the existing `DataInitializer` class while maintaining backward compatibility with the legacy request-based system.
