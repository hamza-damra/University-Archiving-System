package com.alqude.edu.ArchiveSystem.controller;

import com.alqude.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestResponse;
import com.alqude.edu.ArchiveSystem.dto.user.UserCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.user.UserResponse;
import com.alqude.edu.ArchiveSystem.dto.user.UserUpdateRequest;
import com.alqude.edu.ArchiveSystem.service.DocumentRequestService;
import com.alqude.edu.ArchiveSystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<UserResponse> professors = userService.getAllProfessors(pageable);
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
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<DocumentRequestResponse> requests = documentRequestService.getDocumentRequestsByCurrentUser(pageable);
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
}
