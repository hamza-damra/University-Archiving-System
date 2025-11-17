# Implementation Plan

This implementation plan breaks down the semester-based file system refactoring into discrete, manageable coding tasks. Each task builds incrementally on previous steps and references specific requirements from the requirements document.

## Task List

- [x] 1. Create new domain entities for academic structure





- [x] 1.1 Create AcademicYear entity with year_code, start_year, end_year fields


  - Add JPA annotations and relationships to Semester
  - Add validation constraints
  - _Requirements: 2.2, 4.1_



- [x] 1.2 Create Semester entity with type enum (FIRST, SECOND, SUMMER)

  - Link to AcademicYear via ManyToOne relationship
  - Add start_date and end_date fields
  - Create SemesterType enum


  - _Requirements: 2.2, 4.1_

- [x] 1.3 Create Course entity with course_code, course_name, department

  - Add unique constraint on course_code


  - Link to Department via ManyToOne
  - Add level and description fields
  - _Requirements: 2.3, 4.5_



- [x] 1.4 Create CourseAssignment entity linking Semester, Course, and Professor

  - Add ManyToOne relationships to Semester, Course, and User (professor)
  - Add unique constraint on (semester_id, course_id, professor_id)
  - _Requirements: 2.4, 4.1_


- [x] 1.5 Create RequiredDocumentType entity with document_type enum and deadline

  - Link to Course and optionally to Semester
  - Create DocumentTypeEnum (SYLLABUS, EXAM, ASSIGNMENT, PROJECT_DOCS, LECTURE_NOTES, OTHER)
  - Add max_file_count, max_total_size_mb fields
  - Add ElementCollection for allowed_file_extensions
  - _Requirements: 2.5, 4.5_

- [x] 1.6 Create DocumentSubmission entity replacing SubmittedDocument

  - Link to CourseAssignment via ManyToOne
  - Add document_type, professor, submitted_at, is_late_submission fields
  - Create SubmissionStatus enum (NOT_UPLOADED, UPLOADED, OVERDUE)
  - Add OneToMany relationship to UploadedFile
  - _Requirements: 4.2, 11.4, 12.1_

- [x] 1.7 Create UploadedFile entity replacing FileAttachment


  - Link to DocumentSubmission via ManyToOne
  - Add file_url, original_filename, file_size, file_type, file_order fields
  - _Requirements: 4.2, 11.4_

- [x] 2. Update existing entities for three-role system






- [x] 2.1 Update Role enum to include ROLE_DEANSHIP

  - Add ROLE_DEANSHIP to existing ROLE_HOD and ROLE_PROFESSOR
  - _Requirements: 1.1, 3.1_


- [x] 2.2 Update User entity with professor_id field and new relationships

  - Add professor_id field with unique constraint
  - Update role field to support ROLE_DEANSHIP
  - Add OneToMany relationships to CourseAssignment and DocumentSubmission
  - Remove or deprecate old documentRequests relationship
  - _Requirements: 1.1, 3.2, 4.4_

- [x] 3. Create repository interfaces for new entities





- [x] 3.1 Create AcademicYearRepository with custom query methods


  - Add findByYearCode method
  - Add findByIsActiveTrue method
  - _Requirements: 2.1_

- [x] 3.2 Create SemesterRepository with filtering methods


  - Add findByAcademicYearId method
  - Add findByAcademicYearIdAndType method
  - _Requirements: 2.1_

- [x] 3.3 Create CourseRepository with department filtering


  - Add findByDepartmentId method
  - Add findByCourseCode method
  - Add findByIsActiveTrue method
  - _Requirements: 2.3_

- [x] 3.4 Create CourseAssignmentRepository with complex queries


  - Add findBySemesterId method
  - Add findByProfessorIdAndSemesterId method
  - Add findBySemesterIdAndCourseId method
  - _Requirements: 2.4_

- [x] 3.5 Create RequiredDocumentTypeRepository


  - Add findByCourseId method
  - Add findByCourseIdAndSemesterId method
  - _Requirements: 2.5_

- [x] 3.6 Create DocumentSubmissionRepository with status queries


  - Add findByProfessorIdAndCourseAssignment_SemesterId method
  - Add findByCourseAssignmentId method
  - Add findByStatusAndCourseAssignment_Semester_Id method
  - _Requirements: 12.3_

- [x] 3.7 Create UploadedFileRepository


  - Add findByDocumentSubmissionId method
  - Add findByDocumentSubmissionIdOrderByFileOrderAsc method
  - _Requirements: 11.4_

- [x] 4. Implement AcademicService for year and semester management





- [x] 4.1 Implement createAcademicYear method that auto-creates three semesters


  - Create AcademicYear record
  - Automatically create FIRST, SECOND, and SUMMER semester records
  - Set default start/end dates for each semester
  - _Requirements: 2.2_

- [x] 4.2 Implement academic year CRUD methods


  - Implement updateAcademicYear, getAllAcademicYears, getActiveAcademicYear
  - Implement setActiveAcademicYear to mark one year as active
  - _Requirements: 2.1_

- [x] 4.3 Implement semester management methods


  - Implement getSemester, getSemestersByYear, updateSemester
  - _Requirements: 2.1_

- [x] 5. Implement CourseService for course and assignment management





- [x] 5.1 Implement course CRUD methods


  - Implement createCourse, updateCourse, getCourse, getCoursesByDepartment
  - Implement deactivateCourse (soft delete)
  - _Requirements: 2.3_

- [x] 5.2 Implement course assignment methods

  - Implement assignCourse to link professor, course, and semester
  - Implement unassignCourse to remove assignment
  - Implement getAssignmentsBySemester and getAssignmentsByProfessor
  - _Requirements: 2.4_

- [x] 5.3 Implement required document type management

  - Implement addRequiredDocumentType for courses
  - Implement updateRequiredDocumentType
  - Implement getRequiredDocumentTypes with semester filtering
  - _Requirements: 2.5_

- [x] 6. Implement ProfessorService for professor management





- [x] 6.1 Implement professor CRUD methods (Deanship only)


  - Implement createProfessor with automatic professor_id generation
  - Implement updateProfessor, getProfessor, getProfessorsByDepartment
  - Implement deactivateProfessor and activateProfessor
  - _Requirements: 3.1, 3.2, 3.4_



- [x] 6.2 Implement generateProfessorId method




  - Generate unique professor_id in format "prof_{id}" or similar


  - _Requirements: 4.4_

- [x] 6.3 Implement getProfessorCourses method





  - Retrieve all course assignments for a professor in a semester
  - _Requirements: 10.2_

- [x] 7. Implement FileService for file operations





- [x] 7.1 Implement generateFilePath method for hierarchical storage
  - Generate path: {year}/{semester}/{professorId}/{courseCode}/{documentType}/{filename}
  - Ensure unique filenames using UUID or timestamp
  - _Requirements: 4.1, 11.3_

- [x] 7.2 Implement file validation methods

  - Implement validateFileType checking against allowed extensions
  - Implement validateFileSize checking total size against limit
  - _Requirements: 11.1, 11.2_

- [x] 7.3 Implement uploadFiles method

  - Validate files using validateFileType and validateFileSize
  - Generate file paths using generateFilePath
  - Store files to filesystem
  - Create UploadedFile records in database
  - Update DocumentSubmission with file count and total size
  - _Requirements: 8.4, 11.3, 11.4_

- [x] 7.4 Implement replaceFiles method

  - Delete old files from filesystem and database
  - Upload new files using same logic as uploadFiles
  - _Requirements: 8.5_

- [x] 7.5 Implement file retrieval methods

  - Implement getFile, getFilesBySubmission
  - Implement loadFileAsResource for file download
  - _Requirements: 5.5, 6.5, 9.3_

- [x] 7.6 Implement deleteFile method


  - Remove file from filesystem
  - Remove UploadedFile record from database
  - Update DocumentSubmission file count and size
  - _Requirements: 9.4_

- [x] 8. Implement SubmissionService for submission management





- [x] 8.1 Implement createSubmission method


  - Create DocumentSubmission record for a course assignment and document type
  - Set initial status to NOT_UPLOADED
  - _Requirements: 8.3, 11.5_

- [x] 8.2 Implement submission status calculation


  - Implement calculateSubmissionStatus comparing submitted_at with deadline
  - Set status to UPLOADED, OVERDUE, or NOT_UPLOADED
  - Set is_late_submission flag if submitted after deadline
  - _Requirements: 12.1, 12.2, 12.3, 12.5_

- [x] 8.3 Implement submission retrieval methods


  - Implement getSubmission, getSubmissionsByProfessor, getSubmissionsByCourse
  - _Requirements: 10.5, 12.3_

- [x] 8.4 Implement submission statistics methods


  - Implement getStatisticsBySemester aggregating submission counts
  - Implement getStatisticsByProfessor for individual professor stats
  - Calculate total required, submitted, missing, overdue counts
  - _Requirements: 7.3, 13.5_

- [x] 9. Implement FileExplorerService for hierarchical navigation





- [x] 9.1 Implement getRootNode method with role-based filtering


  - For Deanship: return all professors in semester
  - For HOD: return professors in HOD's department only
  - For Professor: return all professors in same department
  - _Requirements: 5.1, 5.2, 7.1, 9.1_

- [x] 9.2 Implement getNode and getChildren methods


  - Parse node path to determine type (year/semester/professor/course/documentType)
  - Fetch appropriate children based on node type
  - Apply permission filtering based on user role
  - _Requirements: 5.2, 7.2, 9.2_

- [x] 9.3 Implement permission checking methods


  - Implement canRead: Deanship all, HOD department, Professor department
  - Implement canWrite: Professor only for own courses
  - Implement canDelete: Professor only for own files
  - _Requirements: 5.5, 7.5, 9.4_

- [x] 9.4 Implement generateBreadcrumbs method


  - Parse path and create breadcrumb items for each level
  - _Requirements: 5.4_

- [x] 10. Implement ReportService for submission reports







- [x] 10.1 Implement generateProfessorSubmissionReport for HOD



  - Fetch all course assignments for semester and department
  - For each assignment, fetch required document types and submission status
  - Build ProfessorSubmissionReport with rows and statistics
  - _Requirements: 7.4, 13.2_


- [x] 10.2 Implement report filtering

  - Implement filterReport to filter by course, document type, status
  - _Requirements: 7.4, 13.3_



- [x] 10.3 Implement exportReportToPdf method

  - Use PDF library (e.g., iText, Apache PDFBox) to generate PDF
  - Format report with tables and summary statistics
  - _Requirements: 13.4_



- [x] 10.4 Implement generateSystemWideReport for Deanship

  - Aggregate data across all departments for a semester
  - _Requirements: 6.3_

- [x] 11. Create DeanshipController with academic management endpoints





- [x] 11.1 Implement academic year endpoints


  - POST /api/deanship/academic-years (createAcademicYear)
  - PUT /api/deanship/academic-years/{id} (updateAcademicYear)
  - GET /api/deanship/academic-years (getAllAcademicYears)
  - Add @PreAuthorize("hasRole('DEANSHIP')") to all methods
  - _Requirements: 2.1, 2.2_

- [x] 11.2 Implement professor management endpoints

  - POST /api/deanship/professors (createProfessor)
  - PUT /api/deanship/professors/{id} (updateProfessor)
  - GET /api/deanship/professors (getAllProfessors with optional departmentId filter)
  - PUT /api/deanship/professors/{id}/deactivate (deactivateProfessor)
  - _Requirements: 3.1, 3.2, 3.5_

- [x] 11.3 Implement course management endpoints

  - POST /api/deanship/courses (createCourse)
  - PUT /api/deanship/courses/{id} (updateCourse)
  - GET /api/deanship/courses (getAllCourses with optional departmentId filter)
  - _Requirements: 2.3_

- [x] 11.4 Implement course assignment endpoints

  - POST /api/deanship/course-assignments (assignCourse)
  - DELETE /api/deanship/course-assignments/{id} (unassignCourse)
  - GET /api/deanship/course-assignments (getAssignments with semesterId and optional professorId)
  - _Requirements: 2.4_

- [x] 11.5 Implement required document type endpoints

  - POST /api/deanship/courses/{courseId}/required-documents (addRequiredDocumentType)
  - _Requirements: 2.5_

- [x] 11.6 Implement Deanship reports endpoint

  - GET /api/deanship/reports/system-wide (getSystemWideReport)
  - _Requirements: 6.3_

- [x] 12. Update HodController for semester-based operations










- [x] 12.1 Implement dashboard overview endpoint




  - GET /api/hod/dashboard/overview (getDashboardOverview)
  - Return total professors, courses, submission statistics for semester
  - Filter by HOD's department

  - _Requirements: 7.3_

- [x] 12.2 Implement submission status endpoint





  - GET /api/hod/submissions/status (getSubmissionStatus)
  - Support filtering by courseCode, documentType, status

  - Return ProfessorSubmissionReport for department
  - _Requirements: 7.4_

- [x] 12.3 Implement report endpoints

  - GET /api/hod/reports/professor-submissions (getProfessorSubmissionReport)
  - GET /api/hod/reports/professor-submissions/pdf (exportReportToPdf)
  - _Requirements: 13.1, 13.4_

- [x] 12.4 Implement HOD file explorer endpoints





  - GET /api/hod/file-explorer/root (getFileExplorerRoot)
  - GET /api/hod/file-explorer/node (getFileExplorerNode)
  - GET /api/hod/files/{fileId}/download (downloadFile)
  - Apply department-scoped filtering
  - _Requirements: 7.2, 7.5_

- [x] 13. Update ProfessorController for semester-based operations





- [x] 13.1 Implement professor dashboard endpoints


  - GET /api/professor/dashboard/courses (getMyCourses)
  - GET /api/professor/dashboard/overview (getDashboardOverview)
  - Return courses with required document types and submission status
  - _Requirements: 10.2, 10.3, 10.5_



- [x] 13.2 Implement file upload endpoint
  - POST /api/professor/submissions/upload (uploadFiles)
  - Accept courseAssignmentId, documentType, files, notes
  - Validate professor is assigned to the course
  - Call FileService.uploadFiles and SubmissionService.createSubmission


  - _Requirements: 8.3, 8.4, 11.1, 11.2, 11.3_

- [x] 13.3 Implement file replacement endpoint
  - PUT /api/professor/submissions/{submissionId}/replace (replaceFiles)

  - Validate professor owns the submission
  - Call FileService.replaceFiles
  - _Requirements: 8.5_

- [x] 13.4 Implement submission retrieval endpoints

  - GET /api/professor/submissions (getMySubmissions)
  - GET /api/professor/submissions/{submissionId} (getSubmission)
  - _Requirements: 10.5_

- [x] 13.5 Implement professor file explorer endpoints


  - GET /api/professor/file-explorer/root (getFileExplorerRoot)
  - GET /api/professor/file-explorer/node (getFileExplorerNode)
  - GET /api/professor/files/{fileId}/download (downloadFile)
  - Apply department-scoped filtering with read-only for other professors
  - _Requirements: 9.1, 9.2, 9.3, 9.5_

- [x] 14. Create shared FileExplorerController




- [x] 14.1 Implement shared file explorer endpoints


  - GET /api/file-explorer/root (getRoot)
  - GET /api/file-explorer/node (getNode)
  - GET /api/file-explorer/breadcrumbs (getBreadcrumbs)
  - GET /api/file-explorer/files/{fileId} (getFileMetadata)
  - GET /api/file-explorer/files/{fileId}/download (downloadFile)
  - Apply role-based permissions using FileExplorerService
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 15. Implement data migration from old schema to new schema





- [x] 15.1 Create migration service to analyze existing data


  - Analyze document_requests to determine year ranges
  - Extract unique course names and professors
  - _Requirements: 15.1, 15.2_

- [x] 15.2 Implement academic year and semester creation from existing data


  - Create academic year records based on request deadlines
  - Create three semesters per year
  - _Requirements: 15.2_

- [x] 15.3 Implement professor migration


  - Generate professor_id for existing ROLE_PROFESSOR users
  - Update user records
  - _Requirements: 15.3_

- [x] 15.4 Implement course extraction and creation


  - Extract unique course names from document_requests
  - Generate course codes
  - Create course records
  - _Requirements: 15.4_

- [x] 15.5 Implement course assignment creation from requests


  - For each document_request, determine semester from deadline
  - Create course_assignment linking professor, course, semester
  - _Requirements: 15.4_

- [x] 15.6 Implement document submission migration


  - For each submitted_document, find corresponding course_assignment
  - Create document_submission record
  - _Requirements: 15.5_

- [x] 15.7 Implement file migration to new folder structure


  - For each file_attachment, create uploaded_file record
  - Move physical files to new hierarchical structure
  - Update file_url in database
  - _Requirements: 15.6_

- [x] 15.8 Implement required document type extraction


  - Extract document types from document_requests
  - Create required_document_type records per course
  - _Requirements: 15.7_

- [ ] 16. Create Deanship frontend dashboard
- [ ] 16.1 Create deanship-dashboard.html page structure
  - Add navigation menu with sections: Academic Years, Professors, Courses, Assignments, Reports, File Explorer
  - Add academic year selector dropdown
  - Add semester tabs (First, Second, Summer)
  - _Requirements: 14.1_

- [ ] 16.2 Implement academic year management UI
  - Create form to add/edit academic years
  - Display table of academic years with edit/activate actions
  - Wire to /api/deanship/academic-years endpoints
  - _Requirements: 2.1, 2.2_

- [ ] 16.3 Implement professor management UI
  - Create form to add/edit professors
  - Display table of professors with edit/deactivate actions
  - Wire to /api/deanship/professors endpoints
  - _Requirements: 3.1, 3.2_

- [ ] 16.4 Implement course management UI
  - Create form to add/edit courses
  - Display table of courses with edit actions
  - Wire to /api/deanship/courses endpoints
  - _Requirements: 2.3_

- [ ] 16.5 Implement course assignment UI
  - Create form with professor, course, semester selectors
  - Display table of assignments with unassign action
  - Wire to /api/deanship/course-assignments endpoints
  - _Requirements: 2.4_

- [ ] 16.6 Implement Deanship file explorer UI
  - Integrate file explorer component (tree view + file list)
  - Wire to /api/file-explorer endpoints
  - _Requirements: 6.1, 6.5_

- [ ] 17. Update HOD frontend dashboard
- [ ] 17.1 Update hod-dashboard.html with semester selector
  - Add academic year and semester dropdown selectors
  - Remove old professor management UI
  - _Requirements: 7.1, 14.2_

- [ ] 17.2 Implement HOD overview widgets
  - Display cards for total professors, courses, submitted, missing, overdue
  - Wire to /api/hod/dashboard/overview endpoint
  - _Requirements: 7.3_

- [ ] 17.3 Implement professor submission status table
  - Display table with professor, course, document types, status columns
  - Add filters for course, document type, status
  - Wire to /api/hod/submissions/status endpoint
  - _Requirements: 7.4_

- [ ] 17.4 Implement HOD report generation and export
  - Add "Generate Report" button
  - Add "Export to PDF" button
  - Wire to /api/hod/reports endpoints
  - _Requirements: 13.1, 13.4_

- [ ] 17.5 Implement HOD file explorer UI
  - Integrate file explorer component (read-only)
  - Wire to /api/hod/file-explorer endpoints
  - _Requirements: 7.2_

- [ ] 18. Update Professor frontend dashboard
- [ ] 18.1 Update prof-dashboard.html with semester selector
  - Add academic year and semester dropdown selectors
  - Remove old request-based card view
  - _Requirements: 10.1, 14.3_

- [ ] 18.2 Implement semester-based course view
  - Display assigned courses for selected semester
  - For each course, show required document types with status indicators
  - Show deadlines for each document type
  - Add Upload/Replace buttons per document type
  - Wire to /api/professor/dashboard/courses endpoint
  - _Requirements: 10.2, 10.3, 10.4, 10.5_

- [ ] 18.3 Update upload modal for multi-file upload
  - Display course name and document type
  - Show file requirements (allowed types, max count, max size)
  - Add drag-and-drop file zone
  - Display file preview list
  - Add notes textarea
  - Wire to /api/professor/submissions/upload endpoint
  - _Requirements: 8.3, 8.4, 11.1, 11.2_

- [ ] 18.4 Implement file replacement functionality
  - Add "Replace Files" button for uploaded document types
  - Reuse upload modal with replace mode
  - Wire to /api/professor/submissions/{id}/replace endpoint
  - _Requirements: 8.5_

- [ ] 18.5 Implement professor file explorer UI
  - Integrate file explorer component
  - Show own folders with write access, other folders read-only
  - Wire to /api/professor/file-explorer endpoints
  - _Requirements: 9.1, 9.2, 9.5_

- [ ] 19. Create shared file explorer JavaScript component
- [ ] 19.1 Implement FileExplorer class with tree view rendering
  - Render hierarchical tree structure (year → semester → professor → course → document type)
  - Support lazy loading of tree nodes on expand
  - Handle node selection and navigation
  - _Requirements: 5.1, 5.2_

- [ ] 19.2 Implement breadcrumb navigation
  - Display breadcrumbs showing current path
  - Support clicking breadcrumb items to navigate
  - Wire to /api/file-explorer/breadcrumbs endpoint
  - _Requirements: 5.4_

- [ ] 19.3 Implement file list table rendering
  - Display files with columns: name, size, uploaded date, uploader, actions
  - Show View and Download buttons based on permissions
  - Wire to /api/file-explorer/node endpoint
  - _Requirements: 5.3, 5.5_

- [ ] 19.4 Implement file download functionality
  - Handle file download via /api/file-explorer/files/{id}/download
  - _Requirements: 5.5, 6.5, 9.3_

- [ ] 20. Update authentication and navigation
- [ ] 20.1 Update login page to support three roles
  - Update authentication logic to handle ROLE_DEANSHIP
  - Redirect to appropriate dashboard based on role
  - _Requirements: 1.2, 14.5_

- [ ] 20.2 Update navigation menus for each role
  - Deanship: show Academic Years, Professors, Courses, Assignments, Reports, File Explorer
  - HOD: show Dashboard, Submission Status, Reports, File Explorer
  - Professor: show Dashboard, My Courses, File Explorer, Notifications
  - Hide unauthorized menu items
  - _Requirements: 14.1, 14.2, 14.3, 14.4_

- [ ] 20.3 Display current user role and name in header
  - Update header to show role badge
  - _Requirements: 14.5_

- [ ] 21. Implement security and permission enforcement
- [ ] 21.1 Add method-level security annotations to all controller methods
  - Add @PreAuthorize annotations for role-based access
  - Add custom security expressions for ownership checks (e.g., professor owns submission)
  - _Requirements: 1.3, 1.4, 1.5_

- [ ] 21.2 Implement department-scoped filtering in services
  - For HOD: filter all queries by department
  - For Professor: filter by department for read, by assignment for write
  - _Requirements: 7.1, 9.1_

- [ ] 21.3 Implement file permission checks in FileService
  - Check canRead, canWrite, canDelete before file operations
  - Throw UnauthorizedAccessException if permission denied
  - _Requirements: 5.5, 7.5, 9.4_

- [ ] 22. Add database indexes for performance
- [ ] 22.1 Create indexes on foreign keys and frequently queried columns
  - Add indexes: course_assignments(semester_id), course_assignments(professor_id)
  - Add indexes: document_submissions(course_assignment_id), document_submissions(professor_id)
  - Add indexes: uploaded_files(document_submission_id)
  - Add indexes: users(department_id), users(role)
  - _Requirements: Performance optimization_

- [ ] 23. Implement error handling and validation
- [ ] 23.1 Create custom exception classes
  - Create ArchiveSystemException base class
  - Create ResourceNotFoundException, UnauthorizedAccessException, ValidationException, FileUploadException
  - _Requirements: 1.4, 1.5_

- [ ] 23.2 Implement global exception handler
  - Create @ControllerAdvice class to handle exceptions
  - Return consistent error response format with timestamp, status, errorCode, message, path
  - _Requirements: Error handling_

- [ ] 23.3 Add validation to DTOs and entities
  - Add @Valid annotations to controller parameters
  - Add validation constraints to entity fields
  - _Requirements: 11.1, 11.2_

- [ ] 24. Write unit tests for services
- [ ] 24.1 Write unit tests for AcademicService
  - Test createAcademicYear auto-creates three semesters
  - Test setActiveAcademicYear
  - _Requirements: 2.2_

- [ ] 24.2 Write unit tests for CourseService
  - Test assignCourse creates CourseAssignment
  - Test getAssignmentsByProfessor filters correctly
  - _Requirements: 2.4_

- [ ] 24.3 Write unit tests for FileService
  - Test generateFilePath creates correct hierarchical path
  - Test validateFileType and validateFileSize
  - Test uploadFiles creates UploadedFile records
  - _Requirements: 11.1, 11.2, 11.3_

- [ ] 24.4 Write unit tests for SubmissionService
  - Test calculateSubmissionStatus with various deadline scenarios
  - Test getStatisticsBySemester aggregates correctly
  - _Requirements: 12.1, 12.2_

- [ ] 24.5 Write unit tests for FileExplorerService
  - Test getRootNode applies role-based filtering
  - Test canRead, canWrite, canDelete permission checks
  - _Requirements: 5.1, 9.3_

- [ ] 25. Write integration tests for controllers
- [ ] 25.1 Write integration tests for DeanshipController
  - Test POST /api/deanship/academic-years creates year and semesters
  - Test POST /api/deanship/professors creates professor with professor_id
  - Test POST /api/deanship/course-assignments creates assignment
  - _Requirements: 2.2, 3.2, 2.4_

- [ ] 25.2 Write integration tests for HodController
  - Test GET /api/hod/dashboard/overview returns department-scoped data
  - Test GET /api/hod/submissions/status filters by department
  - Test HOD cannot access other departments' data
  - _Requirements: 7.1, 7.3, 7.4_

- [ ] 25.3 Write integration tests for ProfessorController
  - Test POST /api/professor/submissions/upload creates submission and files
  - Test professor cannot upload to unassigned course
  - Test professor can read other professors' files in same department
  - _Requirements: 8.3, 8.4, 9.1, 9.2_

- [ ] 25.4 Write integration tests for FileExplorerController
  - Test file explorer returns correct nodes based on role
  - Test file download enforces permissions
  - _Requirements: 5.1, 5.5_

- [ ] 26. Create database migration scripts
- [ ] 26.1 Create Flyway migration for new tables
  - Create V2__create_academic_structure_tables.sql
  - Add tables: academic_years, semesters, courses, course_assignments, required_document_types, document_submissions, uploaded_files
  - _Requirements: 15.1_

- [ ] 26.2 Create Flyway migration to update existing tables
  - Create V3__update_users_table.sql
  - Add professor_id column to users
  - Update role enum to include ROLE_DEANSHIP
  - _Requirements: 15.1_

- [ ] 26.3 Create Flyway migration for indexes
  - Create V4__add_indexes.sql
  - Add performance indexes
  - _Requirements: Performance optimization_

- [ ] 27. Execute data migration
- [ ] 27.1 Create migration endpoint or command-line tool
  - Create /api/admin/migrate endpoint or Spring Boot CommandLineRunner
  - Call MigrationService methods in sequence
  - _Requirements: 15.1_

- [ ] 27.2 Run migration and verify data integrity
  - Execute migration on test database
  - Verify all document_requests converted to course_assignments
  - Verify all submitted_documents converted to document_submissions
  - Verify all files moved to new folder structure
  - _Requirements: 15.5_

- [ ] 28. Update configuration and deployment
- [ ] 28.1 Update application.properties with new settings
  - Add file upload settings (max-file-size, max-request-size)
  - Add app.upload.* settings (base-path, allowed-extensions, max-file-count, max-total-size-mb)
  - Add app.academic.* settings (default-year, auto-create-semesters)
  - _Requirements: Configuration_

- [ ] 28.2 Update Docker configuration if applicable
  - Update Dockerfile and docker-compose.yml for new file storage structure
  - Ensure uploads directory is mounted as volume
  - _Requirements: Deployment_

- [ ] 29. Documentation and cleanup
- [ ] 29.1 Update API documentation
  - Document all new endpoints with request/response examples
  - Update Swagger/OpenAPI spec if used
  - _Requirements: Documentation_

- [ ] 29.2 Update user guides
  - Create Deanship user guide for academic structure management
  - Update HOD user guide for semester-based reporting
  - Update Professor user guide for semester-based file upload
  - _Requirements: Documentation_

- [ ] 29.3 Archive old code and tables
  - Comment out or remove old controller methods
  - Keep old tables for rollback capability
  - _Requirements: 15.1_
