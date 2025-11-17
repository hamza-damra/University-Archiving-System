package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alqude.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode;
import com.alqude.edu.ArchiveSystem.dto.report.DashboardOverview;
import com.alqude.edu.ArchiveSystem.dto.report.DepartmentSubmissionReport;
import com.alqude.edu.ArchiveSystem.dto.report.ProfessorSubmissionReport;
import com.alqude.edu.ArchiveSystem.dto.report.ReportFilter;
import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestResponse;
import com.alqude.edu.ArchiveSystem.dto.user.UserCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.user.UserResponse;
import com.alqude.edu.ArchiveSystem.dto.user.UserUpdateRequest;
import com.alqude.edu.ArchiveSystem.entity.DocumentTypeEnum;
import com.alqude.edu.ArchiveSystem.entity.SubmissionStatus;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.service.*;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hod")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('HOD')")
public class HodController {
    
    private final UserService userService;
    private final DocumentRequestService documentRequestService;
    private final ReportService reportService;
    private final PdfReportService pdfReportService;
    private final UserRepository userRepository;
    private final SemesterReportService semesterReportService;
    private final FileExplorerService fileExplorerService;
    private final FileService fileService;
    
    // User Management Endpoints
    
    @PostMapping("/professors")
    public ResponseEntity<ApiResponse<UserResponse>> createProfessor(@Valid @RequestBody UserCreateRequest request) {
        log.info("HOD creating professor with email: {}", request.getEmail());
        
        try {
            UserResponse professor = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Professor created successfully", professor));
        } catch (Exception e) {
            log.error("Error creating professor", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/professors")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllProfessors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        // Get current HOD's department
        User currentUser = getCurrentUser();
        if (currentUser.getDepartment() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("HOD must be assigned to a department"));
        }
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<UserResponse> professors = userService.getProfessorsByDepartment(currentUser.getDepartment().getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Professors retrieved successfully", professors));
    }
    
    @GetMapping("/professors/department/{departmentId}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getProfessorsByDepartment(@PathVariable Long departmentId) {
        List<UserResponse> professors = userService.getProfessorsByDepartment(departmentId);
        return ResponseEntity.ok(ApiResponse.success("Department professors retrieved successfully", professors));
    }
    
    @GetMapping("/professors/{professorId}")
    public ResponseEntity<ApiResponse<UserResponse>> getProfessorById(@PathVariable Long professorId) {
        try {
            UserResponse professor = userService.getUserById(professorId);
            return ResponseEntity.ok(ApiResponse.success("Professor retrieved successfully", professor));
        } catch (Exception e) {
            log.error("Error retrieving professor with id: {}", professorId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/professors/{professorId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfessor(
            @PathVariable Long professorId,
            @Valid @RequestBody UserUpdateRequest request) {
        
        log.info("HOD updating professor with id: {}", professorId);
        
        try {
            UserResponse updatedProfessor = userService.updateUser(professorId, request);
            return ResponseEntity.ok(ApiResponse.success("Professor updated successfully", updatedProfessor));
        } catch (Exception e) {
            log.error("Error updating professor with id: {}", professorId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/professors/{professorId}")
    public ResponseEntity<ApiResponse<String>> deleteProfessor(@PathVariable Long professorId) {
        log.info("HOD deleting professor with id: {}", professorId);
        
        try {
            userService.deleteUser(professorId);
            return ResponseEntity.ok(ApiResponse.success("Professor deleted successfully", "User removed"));
        } catch (Exception e) {
            log.error("Error deleting professor with id: {}", professorId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // Document Request Management Endpoints
    
    @PostMapping("/document-requests")
    public ResponseEntity<ApiResponse<DocumentRequestResponse>> createDocumentRequest(
            @Valid @RequestBody DocumentRequestCreateRequest request) {
        
        log.info("HOD creating document request for professor id: {}", request.getProfessorId());
        
        try {
            DocumentRequestResponse documentRequest = documentRequestService.createDocumentRequest(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Document request created successfully", documentRequest));
        } catch (Exception e) {
            log.error("Error creating document request", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/document-requests")
    public ResponseEntity<ApiResponse<Page<DocumentRequestResponse>>> getMyDocumentRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        // Get current HOD's department
        User currentUser = getCurrentUser();
        if (currentUser.getDepartment() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("HOD must be assigned to a department"));
        }
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<DocumentRequestResponse> requests = documentRequestService.getDocumentRequestsByDepartment(currentUser.getDepartment().getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Document requests retrieved successfully", requests));
    }
    
    @GetMapping("/document-requests/department/{departmentId}")
    public ResponseEntity<ApiResponse<Page<DocumentRequestResponse>>> getDocumentRequestsByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<DocumentRequestResponse> requests = documentRequestService.getDocumentRequestsByDepartment(departmentId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Department document requests retrieved successfully", requests));
    }
    
    @GetMapping("/document-requests/{requestId}")
    public ResponseEntity<ApiResponse<DocumentRequestResponse>> getDocumentRequestById(@PathVariable Long requestId) {
        try {
            DocumentRequestResponse request = documentRequestService.getDocumentRequestById(requestId);
            return ResponseEntity.ok(ApiResponse.success("Document request retrieved successfully", request));
        } catch (Exception e) {
            log.error("Error retrieving document request with id: {}", requestId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/document-requests/{requestId}")
    public ResponseEntity<ApiResponse<String>> deleteDocumentRequest(@PathVariable Long requestId) {
        log.info("HOD deleting document request with id: {}", requestId);
        
        try {
            documentRequestService.deleteDocumentRequest(requestId);
            return ResponseEntity.ok(ApiResponse.success("Document request deleted successfully", "Request removed"));
        } catch (Exception e) {
            log.error("Error deleting document request with id: {}", requestId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/document-requests/overdue")
    public ResponseEntity<ApiResponse<List<DocumentRequestResponse>>> getOverdueRequests() {
        List<DocumentRequestResponse> overdueRequests = documentRequestService.getOverdueRequests();
        return ResponseEntity.ok(ApiResponse.success("Overdue requests retrieved successfully", overdueRequests));
    }
    
    @GetMapping("/document-requests/upcoming-deadline")
    public ResponseEntity<ApiResponse<List<DocumentRequestResponse>>> getRequestsWithUpcomingDeadline(
            @RequestParam(defaultValue = "24") int hours) {
        
        List<DocumentRequestResponse> upcomingRequests = documentRequestService.getRequestsWithUpcomingDeadline(hours);
        return ResponseEntity.ok(ApiResponse.success("Upcoming deadline requests retrieved successfully", upcomingRequests));
    }
    
    // Report Endpoints
    
    /**
     * Get department submission summary report
     * Shows which professors have submitted required documents
     */
    @GetMapping("/reports/submission-summary")
    public ResponseEntity<ApiResponse<DepartmentSubmissionReport>> getDepartmentSubmissionReport() {
        log.info("HOD requesting department submission summary report");
        
        try {
            DepartmentSubmissionReport report = reportService.generateDepartmentSubmissionReport();
            return ResponseEntity.ok(ApiResponse.success("Department submission report generated successfully", report));
        } catch (Exception e) {
            log.error("Error generating department submission report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate report: " + e.getMessage()));
        }
    }
    
    /**
     * Download department submission summary report as PDF
     */
    @GetMapping("/reports/submission-summary/pdf")
    public ResponseEntity<byte[]> downloadDepartmentSubmissionReportPdf() {
        log.info("HOD downloading department submission summary report as PDF");
        
        try {
            DepartmentSubmissionReport report = reportService.generateDepartmentSubmissionReport();
            byte[] pdfBytes = pdfReportService.generateDepartmentSubmissionReportPdf(report);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                    "department-submission-report-" + System.currentTimeMillis() + ".pdf");
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            log.error("Error generating PDF report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
    
    // ========================================
    // Semester-Based Operations (New System)
    // ========================================
    
    /**
     * Get dashboard overview for a semester
     * Returns total professors, courses, and submission statistics for HOD's department
     * Task 12.1
     */
    @GetMapping("/dashboard/overview")
    public ResponseEntity<ApiResponse<DashboardOverview>> getDashboardOverview(
            @RequestParam Long semesterId) {
        
        log.info("HOD requesting dashboard overview for semester: {}", semesterId);
        
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getDepartment() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("HOD must be assigned to a department"));
            }
            
            Long departmentId = currentUser.getDepartment().getId();
            
            // Generate the professor submission report which contains the statistics
            ProfessorSubmissionReport report = semesterReportService.generateProfessorSubmissionReport(
                    semesterId, departmentId);
            
            // Build dashboard overview from the report
            DashboardOverview overview = DashboardOverview.builder()
                    .semesterId(report.getSemesterId())
                    .semesterName(report.getSemesterName())
                    .departmentId(report.getDepartmentId())
                    .departmentName(report.getDepartmentName())
                    .generatedAt(report.getGeneratedAt())
                    .totalProfessors(report.getStatistics().getTotalProfessors())
                    .totalCourses(report.getStatistics().getTotalCourses())
                    .totalCourseAssignments(report.getRows().size())
                    .submissionStatistics(report.getStatistics())
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success("Dashboard overview retrieved successfully", overview));
            
        } catch (Exception e) {
            log.error("Error generating dashboard overview for semester: {}", semesterId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate dashboard overview: " + e.getMessage()));
        }
    }
    
    /**
     * Get submission status for professors in the department
     * Supports filtering by courseCode, documentType, and status
     * Task 12.2
     */
    @GetMapping("/submissions/status")
    public ResponseEntity<ApiResponse<ProfessorSubmissionReport>> getSubmissionStatus(
            @RequestParam Long semesterId,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) DocumentTypeEnum documentType,
            @RequestParam(required = false) SubmissionStatus status) {
        
        log.info("HOD requesting submission status for semester: {} with filters - courseCode: {}, documentType: {}, status: {}", 
                semesterId, courseCode, documentType, status);
        
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getDepartment() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("HOD must be assigned to a department"));
            }
            
            Long departmentId = currentUser.getDepartment().getId();
            
            // Generate the base report
            ProfessorSubmissionReport report = semesterReportService.generateProfessorSubmissionReport(
                    semesterId, departmentId);
            
            // Apply filters if provided
            if (courseCode != null || documentType != null || status != null) {
                ReportFilter filter = ReportFilter.builder()
                        .courseCode(courseCode)
                        .documentType(documentType)
                        .status(status)
                        .build();
                
                report = semesterReportService.filterReport(report, filter);
            }
            
            return ResponseEntity.ok(ApiResponse.success("Submission status retrieved successfully", report));
            
        } catch (Exception e) {
            log.error("Error retrieving submission status for semester: {}", semesterId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve submission status: " + e.getMessage()));
        }
    }
    
    /**
     * Get professor submission report for a semester
     * Task 12.3
     */
    @GetMapping("/reports/professor-submissions")
    public ResponseEntity<ApiResponse<ProfessorSubmissionReport>> getProfessorSubmissionReport(
            @RequestParam Long semesterId) {
        
        log.info("HOD requesting professor submission report for semester: {}", semesterId);
        
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getDepartment() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("HOD must be assigned to a department"));
            }
            
            Long departmentId = currentUser.getDepartment().getId();
            
            ProfessorSubmissionReport report = semesterReportService.generateProfessorSubmissionReport(
                    semesterId, departmentId);
            
            return ResponseEntity.ok(ApiResponse.success("Professor submission report generated successfully", report));
            
        } catch (Exception e) {
            log.error("Error generating professor submission report for semester: {}", semesterId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate report: " + e.getMessage()));
        }
    }
    
    /**
     * Export professor submission report to PDF
     * Task 12.3
     */
    @GetMapping("/reports/professor-submissions/pdf")
    public ResponseEntity<byte[]> exportReportToPdf(@RequestParam Long semesterId) {
        log.info("HOD exporting professor submission report to PDF for semester: {}", semesterId);
        
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getDepartment() == null) {
                log.error("HOD must be assigned to a department");
                return ResponseEntity.badRequest().body(null);
            }
            
            Long departmentId = currentUser.getDepartment().getId();
            
            // Generate the report
            ProfessorSubmissionReport report = semesterReportService.generateProfessorSubmissionReport(
                    semesterId, departmentId);
            
            // Export to PDF
            byte[] pdfBytes = semesterReportService.exportReportToPdf(report);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                    "professor-submission-report-" + semesterId + "-" + System.currentTimeMillis() + ".pdf");
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            log.error("Error exporting professor submission report to PDF for semester: {}", semesterId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
    
    /**
     * Get file explorer root for HOD (department-scoped)
     * Task 12.4
     */
    @GetMapping("/file-explorer/root")
    public ResponseEntity<ApiResponse<FileExplorerNode>> getFileExplorerRoot(
            @RequestParam Long academicYearId,
            @RequestParam Long semesterId) {
        
        log.info("HOD requesting file explorer root for year: {}, semester: {}", academicYearId, semesterId);
        
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getDepartment() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("HOD must be assigned to a department"));
            }
            
            FileExplorerNode rootNode = fileExplorerService.getRootNode(academicYearId, semesterId, currentUser);
            
            return ResponseEntity.ok(ApiResponse.success("File explorer root retrieved successfully", rootNode));
            
        } catch (Exception e) {
            log.error("Error retrieving file explorer root", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve file explorer root: " + e.getMessage()));
        }
    }
    
    /**
     * Get file explorer node (folder or file details)
     * Task 12.4
     */
    @GetMapping("/file-explorer/node")
    public ResponseEntity<ApiResponse<FileExplorerNode>> getFileExplorerNode(@RequestParam String path) {
        log.info("HOD requesting file explorer node for path: {}", path);
        
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getDepartment() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("HOD must be assigned to a department"));
            }
            
            FileExplorerNode node = fileExplorerService.getNode(path, currentUser);
            
            return ResponseEntity.ok(ApiResponse.success("File explorer node retrieved successfully", node));
            
        } catch (Exception e) {
            log.error("Error retrieving file explorer node for path: {}", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve file explorer node: " + e.getMessage()));
        }
    }
    
    /**
     * Download a file (read-only access for HOD)
     * Task 12.4
     */
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        log.info("HOD downloading file with id: {}", fileId);
        
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getDepartment() == null) {
                log.error("HOD must be assigned to a department");
                return ResponseEntity.badRequest().build();
            }
            
            // Get file metadata
            var uploadedFile = fileService.getFile(fileId);
            
            // Check if HOD has permission to access this file (department-scoped)
            // This check is performed in the FileExplorerService
            String filePath = uploadedFile.getFileUrl();
            if (!fileExplorerService.canRead(filePath, currentUser)) {
                log.error("HOD does not have permission to access file: {}", fileId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Load file as resource
            Resource resource = fileService.loadFileAsResource(uploadedFile.getFileUrl());
            
            // Determine content type
            String contentType = uploadedFile.getFileType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + uploadedFile.getOriginalFilename() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error downloading file with id: {}", fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        String email = authentication.getName();
        return userRepository.findByEmail(email).orElse(null);
    }
}
