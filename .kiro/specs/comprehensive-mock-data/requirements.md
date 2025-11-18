# Requirements Document

## Introduction

This document defines the requirements for creating a comprehensive mock data generation system for the University Archive System. The system needs realistic test data covering all entities including academic years, semesters, departments, courses, users (HODs and professors), course assignments, document submissions, and notifications to enable thorough testing and demonstration of all system features.

## Glossary

- **Archive System**: The University Archive System application for managing academic document submissions
- **Mock Data Generator**: A component that creates realistic test data for all entities in the system
- **HOD**: Head of Department - a user role with administrative privileges
- **Professor**: A faculty member who submits documents for courses
- **Academic Year**: A yearly period containing multiple semesters (e.g., 2024-2025)
- **Semester**: A term within an academic year (Fall, Spring, Summer)
- **Course Assignment**: The assignment of a professor to teach a specific course in a semester
- **Document Submission**: Files submitted by professors for their assigned courses
- **Required Document Type**: Types of documents required for each course (syllabus, exams, etc.)
- **DataInitializer**: The Spring Boot component that runs on application startup to seed data

## Requirements

### Requirement 1

**User Story:** As a developer, I want comprehensive mock data for all system entities, so that I can test and demonstrate all features without manual data entry

#### Acceptance Criteria

1. WHEN the Archive System starts, THE DataInitializer SHALL create at least 3 academic years with sequential year ranges
2. WHEN the Archive System starts, THE DataInitializer SHALL create at least 3 semesters per academic year covering Fall, Spring, and Summer terms
3. WHEN the Archive System starts, THE DataInitializer SHALL create at least 5 departments with unique names and descriptions
4. WHEN the Archive System starts, THE DataInitializer SHALL create at least 10 courses distributed across departments with realistic course codes and names
5. WHEN the Archive System starts, THE DataInitializer SHALL create at least 5 HOD users with one HOD assigned per department

### Requirement 2

**User Story:** As a developer, I want realistic professor accounts with varied attributes, so that I can test role-based access control and department filtering

#### Acceptance Criteria

1. WHEN the Archive System starts, THE DataInitializer SHALL create at least 20 professor users distributed across all departments
2. WHEN creating professor users, THE DataInitializer SHALL assign unique professor IDs following a consistent format
3. WHEN creating professor users, THE DataInitializer SHALL assign realistic Arabic and English names
4. WHEN creating professor users, THE DataInitializer SHALL set varied active/inactive statuses to test filtering
5. WHEN creating all users, THE DataInitializer SHALL use a consistent default password that is documented

### Requirement 3

**User Story:** As a developer, I want course assignments linking professors to courses across semesters, so that I can test the semester-based document system

#### Acceptance Criteria

1. WHEN the Archive System starts, THE DataInitializer SHALL create at least 30 course assignments across all semesters
2. WHEN creating course assignments, THE DataInitializer SHALL ensure each professor has assignments in multiple semesters
3. WHEN creating course assignments, THE DataInitializer SHALL ensure each course has at least one assignment per semester
4. WHEN creating course assignments, THE DataInitializer SHALL distribute assignments evenly across departments
5. WHEN creating course assignments, THE DataInitializer SHALL include section numbers for courses with multiple sections

### Requirement 4

**User Story:** As a developer, I want required document types defined for courses, so that I can test document submission requirements

#### Acceptance Criteria

1. WHEN the Archive System starts, THE DataInitializer SHALL create required document types for all courses
2. WHEN creating required document types, THE DataInitializer SHALL include all standard document types (syllabus, midterm exam, final exam, assignments, lab materials)
3. WHEN creating required document types, THE DataInitializer SHALL set realistic deadlines relative to semester dates
4. WHEN creating required document types, THE DataInitializer SHALL specify allowed file extensions for each document type
5. WHEN creating required document types, THE DataInitializer SHALL set appropriate file size limits

### Requirement 5

**User Story:** As a developer, I want document submissions with varied statuses and timestamps, so that I can test submission workflows and reporting

#### Acceptance Criteria

1. WHEN the Archive System starts, THE DataInitializer SHALL create at least 50 document submissions across all course assignments
2. WHEN creating document submissions, THE DataInitializer SHALL include submissions with all possible statuses (pending, submitted, late, approved, rejected)
3. WHEN creating document submissions, THE DataInitializer SHALL create submissions with varied submission dates including on-time and late submissions
4. WHEN creating document submissions, THE DataInitializer SHALL attach multiple uploaded files to submissions
5. WHEN creating document submissions, THE DataInitializer SHALL include submissions with review comments and feedback

### Requirement 6

**User Story:** As a developer, I want uploaded files with realistic metadata, so that I can test file storage and retrieval functionality

#### Acceptance Criteria

1. WHEN the Archive System starts, THE DataInitializer SHALL create uploaded file records for all document submissions
2. WHEN creating uploaded files, THE DataInitializer SHALL set realistic file names with appropriate extensions
3. WHEN creating uploaded files, THE DataInitializer SHALL set file sizes within allowed limits
4. WHEN creating uploaded files, THE DataInitializer SHALL set storage paths following the system's file organization structure
5. WHEN creating uploaded files, THE DataInitializer SHALL set upload timestamps consistent with submission dates

### Requirement 7

**User Story:** As a developer, I want notifications for various system events, so that I can test the notification system

#### Acceptance Criteria

1. WHEN the Archive System starts, THE DataInitializer SHALL create at least 30 notifications distributed across all professors
2. WHEN creating notifications, THE DataInitializer SHALL include notifications for all event types (new assignment, deadline reminder, submission received, document approved, document rejected)
3. WHEN creating notifications, THE DataInitializer SHALL create both read and unread notifications
4. WHEN creating notifications, THE DataInitializer SHALL set notification timestamps in chronological order
5. WHEN creating notifications, THE DataInitializer SHALL link notifications to relevant entities (course assignments, submissions)

### Requirement 8

**User Story:** As a developer, I want the mock data generator to be idempotent and configurable, so that I can control when and how test data is created

#### Acceptance Criteria

1. WHEN the Archive System starts with existing data, THE DataInitializer SHALL detect existing data and skip creation
2. WHEN the Archive System starts with a clean database, THE DataInitializer SHALL create all mock data in the correct dependency order
3. WHEN the Archive System starts, THE DataInitializer SHALL log the creation of each entity type with counts
4. WHERE a configuration property is set, THE DataInitializer SHALL allow disabling mock data generation for production environments
5. WHEN mock data creation fails, THE DataInitializer SHALL log detailed error messages and continue with remaining data

### Requirement 9

**User Story:** As a developer, I want documentation of all mock accounts and data, so that I can easily use them for testing

#### Acceptance Criteria

1. WHEN mock data is created, THE DataInitializer SHALL log all user credentials to the console
2. WHEN the system includes mock data, THE Archive System SHALL provide a markdown document listing all mock accounts with roles and departments
3. WHEN the system includes mock data, THE Archive System SHALL provide a markdown document describing the data structure and relationships
4. WHEN the system includes mock data, THE Archive System SHALL provide example API calls for testing each user role
5. WHEN the system includes mock data, THE Archive System SHALL document the default password and security considerations

### Requirement 10

**User Story:** As a developer, I want realistic data distributions and relationships, so that the mock data accurately represents production scenarios

#### Acceptance Criteria

1. WHEN creating mock data, THE DataInitializer SHALL ensure department sizes vary realistically (some large, some small)
2. WHEN creating mock data, THE DataInitializer SHALL ensure professor workloads vary (some with many courses, some with few)
3. WHEN creating mock data, THE DataInitializer SHALL ensure submission rates vary by professor (some complete, some partial)
4. WHEN creating mock data, THE DataInitializer SHALL ensure temporal distribution of submissions throughout semester periods
5. WHEN creating mock data, THE DataInitializer SHALL ensure referential integrity for all entity relationships
