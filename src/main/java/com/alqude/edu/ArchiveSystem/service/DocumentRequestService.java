package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestCreateRequest;
import com.alqude.edu.ArchiveSystem.dto.request.DocumentRequestResponse;
import com.alqude.edu.ArchiveSystem.entity.DocumentRequest;
import com.alqude.edu.ArchiveSystem.entity.Notification;
import com.alqude.edu.ArchiveSystem.entity.Role;
import com.alqude.edu.ArchiveSystem.entity.User;
import com.alqude.edu.ArchiveSystem.exception.DocumentRequestException;
import com.alqude.edu.ArchiveSystem.exception.EntityNotFoundException;
import com.alqude.edu.ArchiveSystem.exception.ValidationException;
import com.alqude.edu.ArchiveSystem.mapper.DocumentRequestMapper;
import com.alqude.edu.ArchiveSystem.repository.DocumentRequestRepository;
import com.alqude.edu.ArchiveSystem.repository.NotificationRepository;
import com.alqude.edu.ArchiveSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DocumentRequestService {
    
    private final DocumentRequestRepository documentRequestRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final DocumentRequestMapper documentRequestMapper;
    
    public DocumentRequestResponse createDocumentRequest(DocumentRequestCreateRequest request) {
        log.info("Creating document request for professor id: {}", request.getProfessorId());
        
        // Validate the request
        validateDocumentRequestCreate(request);
        
        // Find professor and validate
        Long professorId = request.getProfessorId();
        if (professorId == null) {
            throw new IllegalArgumentException("Professor ID cannot be null");
        }
        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> DocumentRequestException.professorNotFound(professorId));
        
        // Validate professor role
        validateProfessorRole(professor);
        
        User currentUser = getCurrentUser();
        
        // Validate deadline
        validateDeadline(request.getDeadline());
        
        // Validate file extensions
        validateFileExtensions(request.getRequiredFileExtensions());
        
        DocumentRequest documentRequest = documentRequestMapper.toEntity(request);
        documentRequest.setProfessor(professor);
        documentRequest.setCreatedBy(currentUser);
        
        DocumentRequest savedRequest = documentRequestRepository.save(documentRequest);
        
        // Create notification for professor
        createNotificationForProfessor(professor, savedRequest);
        
        log.info("Document request created successfully with id: {}", savedRequest.getId());
        return documentRequestMapper.toResponse(savedRequest);
    }
    
    public DocumentRequestResponse getDocumentRequestById(Long requestId) {
        log.debug("Retrieving document request with id: {}", requestId);
        
        if (requestId == null) {
            throw new IllegalArgumentException("Request ID cannot be null");
        }
        
        DocumentRequest documentRequest = documentRequestRepository.findById(requestId)
                .orElseThrow(() -> DocumentRequestException.requestNotFound(requestId));
        
        // Verify access permissions
        User currentUser = getCurrentUser();
        validateAccessToDocumentRequest(documentRequest, currentUser);
        
        return documentRequestMapper.toResponse(documentRequest);
    }
    
    public List<DocumentRequestResponse> getDocumentRequestsByProfessor(Long professorId) {
        List<DocumentRequest> requests = documentRequestRepository.findByProfessorId(professorId);
        return requests.stream()
                .map(documentRequestMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    public Page<DocumentRequestResponse> getDocumentRequestsByProfessor(Long professorId, Pageable pageable) {
        Page<DocumentRequest> requests = documentRequestRepository.findByProfessorId(professorId, pageable);
        return requests.map(documentRequestMapper::toResponse);
    }
    
    public List<DocumentRequestResponse> getDocumentRequestsByCurrentUser() {
        User currentUser = getCurrentUser();
        List<DocumentRequest> requests = documentRequestRepository.findByCreatedById(currentUser.getId());
        return requests.stream()
                .map(documentRequestMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    public Page<DocumentRequestResponse> getDocumentRequestsByCurrentUser(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<DocumentRequest> requests = documentRequestRepository.findByCreatedById(currentUser.getId(), pageable);
        return requests.map(documentRequestMapper::toResponse);
    }
    
    public List<DocumentRequestResponse> getDocumentRequestsByDepartment(Long departmentId) {
        List<DocumentRequest> requests = documentRequestRepository.findByDepartmentId(departmentId);
        return requests.stream()
                .map(documentRequestMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    public Page<DocumentRequestResponse> getDocumentRequestsByDepartment(Long departmentId, Pageable pageable) {
        Page<DocumentRequest> requests = documentRequestRepository.findByDepartmentId(departmentId, pageable);
        return requests.map(documentRequestMapper::toResponse);
    }
    
    public List<DocumentRequestResponse> getOverdueRequests() {
        List<DocumentRequest> requests = documentRequestRepository.findOverdueRequests(LocalDateTime.now());
        return requests.stream()
                .map(documentRequestMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    public List<DocumentRequestResponse> getRequestsWithUpcomingDeadline(int hours) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(hours);
        
        List<DocumentRequest> requests = documentRequestRepository.findRequestsWithUpcomingDeadline(now, deadline);
        return requests.stream()
                .map(documentRequestMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    public void deleteDocumentRequest(Long requestId) {
        log.info("Deleting document request with id: {}", requestId);
        
        if (requestId == null) {
            throw new IllegalArgumentException("Request ID cannot be null");
        }
        
        DocumentRequest documentRequest = documentRequestRepository.findById(requestId)
                .orElseThrow(() -> DocumentRequestException.requestNotFound(requestId));
        
        User currentUser = getCurrentUser();
        
        // Validate deletion permissions
        validateDeletionPermissions(documentRequest, currentUser);
        
        // Check if request has submitted documents
        if (documentRequest.getSubmittedDocument() != null) {
            log.warn("Attempting to delete document request {} that has submitted documents", requestId);
            throw new DocumentRequestException(
                DocumentRequestException.REQUEST_ALREADY_SUBMITTED,
                "Cannot delete document request that has submitted documents",
                List.of("Remove submitted documents first", "Contact administrator for assistance")
            );
        }
        
        documentRequestRepository.delete(documentRequest);
        log.info("Document request deleted successfully with id: {}", requestId);
    }
    
    public long getPendingRequestsCountByProfessor(Long professorId) {
        return documentRequestRepository.countPendingRequestsByProfessor(professorId);
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new DocumentRequestException(
                DocumentRequestException.UNAUTHORIZED_ACCESS,
                "User is not authenticated",
                List.of("Please login to access this resource")
            );
        }
        
        String email = authentication.getName();
        if (!StringUtils.hasText(email)) {
            throw new DocumentRequestException(
                DocumentRequestException.UNAUTHORIZED_ACCESS,
                "Invalid authentication token",
                List.of("Please login again")
            );
        }
        
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found with email: " + email));
    }
    
    private void createNotificationForProfessor(User professor, DocumentRequest documentRequest) {
        Notification notification = new Notification();
        notification.setUser(professor);
        notification.setTitle("New Document Request");
        notification.setMessage(String.format("You have a new document request for %s - %s. Deadline: %s", 
                documentRequest.getCourseName(), 
                documentRequest.getDocumentType(),
                documentRequest.getDeadline()));
        notification.setType(Notification.NotificationType.NEW_REQUEST);
        notification.setRelatedEntityType("DocumentRequest");
        notification.setRelatedEntityId(documentRequest.getId());
        
        notificationRepository.save(notification);
        log.info("Notification created for professor: {}", professor.getEmail());
    }
    
    // ========== Validation Methods ==========
    
    private void validateDocumentRequestCreate(DocumentRequestCreateRequest request) {
        Map<String, String> errors = new HashMap<>();
        
        if (request.getProfessorId() == null) {
            errors.put("professorId", "Professor ID is required");
        }
        
        if (!StringUtils.hasText(request.getCourseName())) {
            errors.put("courseName", "Course name is required");
        } else if (request.getCourseName().length() > 100) {
            errors.put("courseName", "Course name must not exceed 100 characters");
        }
        
        if (!StringUtils.hasText(request.getDocumentType())) {
            errors.put("documentType", "Document type is required");
        } else if (request.getDocumentType().length() > 50) {
            errors.put("documentType", "Document type must not exceed 50 characters");
        }
        
        if (request.getDeadline() == null) {
            errors.put("deadline", "Deadline is required");
        }
        
        if (request.getDescription() != null && request.getDescription().length() > 1000) {
            errors.put("description", "Description must not exceed 1000 characters");
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("Document request validation failed", errors);
        }
    }
    
    private void validateProfessorRole(User professor) {
        if (professor.getRole() == null) {
            throw new DocumentRequestException(
                DocumentRequestException.PROFESSOR_NOT_FOUND,
                "User role is not defined",
                List.of("Contact administrator to assign proper role")
            );
        }
        
        // Check if user has professor or HOD role
        if (professor.getRole() != Role.ROLE_PROFESSOR && professor.getRole() != Role.ROLE_HOD) {
            throw new DocumentRequestException(
                DocumentRequestException.PROFESSOR_NOT_FOUND,
                "User is not a professor",
                List.of("Only professors can receive document requests", "Contact administrator if this is incorrect")
            );
        }
    }
    
    private void validateDeadline(LocalDateTime deadline) {
        if (deadline == null) {
            throw DocumentRequestException.invalidDeadline("Deadline cannot be null");
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (deadline.isBefore(now)) {
            throw DocumentRequestException.invalidDeadline("Deadline cannot be in the past");
        }
        
        // Deadline should be at least 1 hour from now
        if (deadline.isBefore(now.plusHours(1))) {
            throw DocumentRequestException.invalidDeadline("Deadline must be at least 1 hour from now");
        }
        
        // Deadline should not be more than 1 year from now
        if (deadline.isAfter(now.plusYears(1))) {
            throw DocumentRequestException.invalidDeadline("Deadline cannot be more than 1 year from now");
        }
    }
    
    private void validateFileExtensions(List<String> fileExtensions) {
        if (fileExtensions == null || fileExtensions.isEmpty()) {
            return; // File extensions are optional
        }
        
        List<String> allowedExtensions = List.of("pdf", "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx");
        List<String> invalidExtensions = fileExtensions.stream()
                .filter(ext -> !allowedExtensions.contains(ext.toLowerCase()))
                .collect(Collectors.toList());
        
        if (!invalidExtensions.isEmpty()) {
            throw new DocumentRequestException(
                DocumentRequestException.INVALID_FILE_EXTENSIONS,
                "Invalid file extensions: " + String.join(", ", invalidExtensions),
                List.of("Allowed extensions: " + String.join(", ", allowedExtensions))
            );
        }
        
        if (fileExtensions.size() > 10) {
            throw new DocumentRequestException(
                DocumentRequestException.INVALID_FILE_EXTENSIONS,
                "Too many file extensions specified",
                List.of("Maximum 10 file extensions allowed")
            );
        }
    }
    
    private void validateAccessToDocumentRequest(DocumentRequest documentRequest, User currentUser) {
        // Professor can access their own requests
        if (documentRequest.getProfessor().getId().equals(currentUser.getId())) {
            return;
        }
        
        // Creator can access their created requests
        if (documentRequest.getCreatedBy().getId().equals(currentUser.getId())) {
            return;
        }
        
        // HOD can access requests in their department
        if (currentUser.getRole() == Role.ROLE_HOD && 
            currentUser.getDepartment() != null &&
            documentRequest.getProfessor().getDepartment() != null &&
            documentRequest.getProfessor().getDepartment().getId().equals(currentUser.getDepartment().getId())) {
            return;
        }
        
        throw DocumentRequestException.unauthorizedAccess("You don't have permission to access this document request");
    }
    
    private void validateDeletionPermissions(DocumentRequest documentRequest, User currentUser) {
        // Creator can delete their own requests if no documents are submitted
        if (documentRequest.getCreatedBy().getId().equals(currentUser.getId())) {
            return;
        }
        
        // HOD can delete requests in their department
        if (currentUser.getRole() == Role.ROLE_HOD && 
            currentUser.getDepartment() != null &&
            documentRequest.getProfessor().getDepartment() != null &&
            documentRequest.getProfessor().getDepartment().getId().equals(currentUser.getDepartment().getId())) {
            return;
        }
        
        throw DocumentRequestException.unauthorizedAccess("You don't have permission to delete this document request");
    }
}
