# Implementation Plan

- [x] 1. Create utility classes for mock data generation





  - Create NameGenerator utility class with Arabic/English name lists and generation methods
  - Create DateCalculator utility class for semester dates and deadline calculations
  - Create MockDataConstants class with all configuration constants
  - _Requirements: 2.3, 9.5_

- [x] 2. Add configuration properties for mock data



  - Add mock data configuration properties to application.properties
  - Create @ConfigurationProperties class for type-safe configuration access
  - Add profile-based configuration to disable in production
  - _Requirements: 8.4, 9.5_

- [-] 3. Implement academic structure creation methods







  - [x] 3.1 Implement createAcademicYears() method to create 3 academic years


    - Create academic years with sequential year codes (2023-2024, 2024-2025, 2025-2026)
    - Set current year as active, others as inactive
    - Use batch save for performance
    - _Requirements: 1.1_



  - [x] 3.2 Implement createSemesters() method to create 9 semesters





    - Create 3 semesters per academic year (FIRST, SECOND, SUMMER)
    - Calculate start and end dates using DateCalculator
    - Set current semester as active
    - Link semesters to academic years


    - _Requirements: 1.2_

  - [x] 3.3 Implement createDepartments() method to create 5 departments



    - Create departments: Computer Science, Mathematics, Physics, Engineering, Business Administration

    - Add descriptive text for each department
    - Check for existing departments before creation
    - _Requirements: 1.3_

  - [x] 3.4 Implement createCourses() method to create 15 courses

    - Create 3 courses per department with realistic course codes
    - Set course names, levels (Undergraduate/Graduate), and descriptions
    - Link courses to departments
    - Mark all courses as active
    - _Requirements: 1.4_

- [x] 4. Implement user creation methods




  - [x] 4.1 Implement createHODUsers() method to create 5 HOD accounts


    - Create one HOD per department
    - Use NameGenerator for realistic names
    - Generate email addresses following pattern: hod.{dept}@alquds.edu
    - Hash password using BCrypt
    - Set role to ROLE_HOD
    - _Requirements: 1.5, 2.5_

  - [x] 4.2 Implement createProfessorUsers() method to create 25 professor accounts


    - Create 5 professors per department
    - Use NameGenerator for realistic Arabic/English names
    - Generate email addresses following pattern: prof.{firstName}.{lastName}@alquds.edu
    - Generate professor IDs following pattern: P{deptCode}{number}
    - Set 80% as active, 20% as inactive for testing
    - Hash passwords using BCrypt
    - Use batch save for performance
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 10.1_

- [x] 5. Implement course assignment creation




  - [x] 5.1 Implement createCourseAssignments() method to create 60+ assignments


    - Assign each course to professors across all semesters
    - Ensure professors only get courses from their department
    - Distribute assignments evenly (2-4 courses per professor per semester)
    - Ensure unique combinations of (semester, course, professor)
    - Mark all assignments as active
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 10.2_
-

- [x] 6. Implement required document type creation



  - [x] 6.1 Implement createRequiredDocumentTypes() method to create 90+ document types


    - Create 6 document types per course (SYLLABUS, EXAM, ASSIGNMENT, PROJECT_DOCS, LECTURE_NOTES, OTHER)
    - Calculate realistic deadlines based on semester dates and document type
    - Set allowed file extensions based on document type
    - Set max file count to 5 and max size to 50MB
    - Mark all as required
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 7. Implement document submission creation





  - [x] 7.1 Implement createDocumentSubmissions() method to create 100+ submissions


    - Create submissions for course assignments with varied statuses
    - Set status distribution: 70% UPLOADED, 20% NOT_UPLOADED, 10% OVERDUE
    - Calculate submission dates based on status (on-time vs late)
    - Set isLateSubmission flag for 15% of submissions
    - Add optional notes to submissions
    - Link submissions to course assignments and professors
    - _Requirements: 5.1, 5.2, 5.3, 10.3, 10.4_

- [x] 8. Implement uploaded file creation




  - [x] 8.1 Implement createUploadedFiles() method to create 150+ file records


    - Create 1-3 files per submission (only for UPLOADED/OVERDUE status)
    - Generate realistic file names based on document type and course
    - Generate file URLs following pattern: uploads/{submissionId}_{uuid}_{timestamp}.{ext}
    - Set random file sizes between 100KB and 10MB
    - Set MIME types based on file extensions
    - Set sequential file order within each submission
    - Update submission fileCount and totalFileSize
    - _Requirements: 5.4, 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 9. Implement notification creation




  - [x] 9.1 Implement createNotifications() method to create 75+ notifications


    - Create 3 notifications per professor on average
    - Distribute notification types: NEW_REQUEST (30%), REQUEST_REMINDER (20%), DEADLINE_APPROACHING (25%), DOCUMENT_SUBMITTED (15%), DOCUMENT_OVERDUE (10%)
    - Set 60% as read, 40% as unread
    - Link notifications to related course assignments or submissions
    - Distribute creation timestamps over last 30 days
    - Generate appropriate titles and messages based on type
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 10. Implement idempotency and error handling




  - [x] 10.1 Implement hasExistingData() method to check for existing data


    - Check counts of academic years, departments, and users
    - Log existing data counts if found
    - Return true if any data exists
    - _Requirements: 8.1, 8.3_


  - [x] 10.2 Add error handling and logging to run() method

    - Wrap data creation in try-catch block
    - Log detailed error messages on failure
    - Continue with remaining data if one section fails
    - Log summary of what was created before failure
    - _Requirements: 8.3, 8.5_

  - [x] 10.3 Implement logCreationSummary() method

    - Collect counts of all created entities
    - Log summary table with entity types and counts
    - Log total execution time
    - _Requirements: 8.3, 9.1_

- [x] 11. Create documentation files




  - [x] 11.1 Create MOCK_ACCOUNTS.md documentation


    - List all HOD accounts with emails, names, and departments
    - List all professor accounts with emails, names, departments, professor IDs, and statuses
    - Document the default password
    - Add security warning about not using in production
    - Include testing scenarios
    - _Requirements: 9.2, 9.5_


  - [x] 11.2 Create MOCK_DATA_API_TESTING.md documentation



    - Provide example curl commands for authentication
    - Provide example API calls for HOD endpoints
    - Provide example API calls for Professor endpoints
    - Include sample responses
    - Document how to use mock accounts for testing
    - _Requirements: 9.4_


  - [x] 11.3 Update existing mock_data_guide.md



    - Update with new comprehensive mock data information
    - Document all entity types and counts
    - Explain data relationships and structure
    - Add configuration instructions
    - _Requirements: 9.3_

- [x] 12. Enhance DataInitializer main class


  - [x] 12.1 Update DataInitializer with new dependencies


    - Add all new repository dependencies
    - Add configuration properties injection
    - Add utility class dependencies
    - Update class-level annotations for profile and conditional execution
    - _Requirements: 8.4_

  - [x] 12.2 Refactor run() method with new execution flow









    - Add configuration check at start
    - Add existing data check
    - Call all creation methods in correct dependency order
    - Add validation after each creation step
    - Call logCreationSummary at end
    - Wrap in transaction for atomicity
    - _Requirements: 8.1, 8.2, 8.3, 10.5_

  - [x] 12.3 Add data validation methods


    - Implement validateEntityCreation() to check expected vs actual counts
    - Add relationship validation for key entities
    - Log warnings for any discrepancies
    - _Requirements: 10.5_

- [x] 13. Add comprehensive testing










  - [x] 13.1 Create DataInitializerTest unit tests




    - Test mock data generation with clean database
    - Test idempotency (running twice doesn't duplicate data)
    - Test data relationships are valid
    - Test entity counts match expectations


    - _Requirements: 8.1, 8.2_

  - [x] 13.2 Create MockDataIntegrationTest integration tests






    - Test HOD can access all professors via API
    - Test Professor can access their assignments via API

    - Test filtering and search with mock data
    - Test report generation with mock data
    - _Requirements: All requirements_

  - [x] 13.3 Create manual testing checklist


    - Document login testing scenarios for each role

    - Document data browsing scenarios
    - Document filtering and search scenarios
    - Document reporting scenarios
    - _Requirements: 9.2, 9.4_


- [x] 14. Update application configuration




  - [x] 14.1 Update application.properties with mock data settings

    - Add mock.data.enabled property
    - Add logging configuration for DataInitializer
    - Add profile-specific settings
    - _Requirements: 8.4_


  - [x] 14.2 Create application-prod.properties

    - Set mock.data.enabled=false for production
    - Add production-specific settings
    - Document why mock data is disabled
    - _Requirements: 8.4, 9.5_
