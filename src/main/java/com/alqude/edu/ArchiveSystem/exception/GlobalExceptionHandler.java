package com.alqude.edu.ArchiveSystem.exception;

import com.alqude.edu.ArchiveSystem.dto.common.ApiResponse;
import com.alqude.edu.ArchiveSystem.dto.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for the application.
 * 
 * NOTE: This handler includes support for legacy DocumentRequestException
 * for backward compatibility during the transition period.
 */
@RestControllerAdvice
@Slf4j
@SuppressWarnings("deprecation")
public class GlobalExceptionHandler {
    
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    private String getRequestPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
    
    // ========== Archive System Exceptions ==========
    
    @ExceptionHandler(ArchiveSystemException.class)
    public ResponseEntity<ApiResponse<Object>> handleArchiveSystemException(
            ArchiveSystemException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        HttpStatus status = ex.getHttpStatus();
        log.warn("Archive system exception [{}]: {} - {} (Status: {})", 
                requestId, ex.getErrorCode(), ex.getMessage(), status.value());
        
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), ex.getMessage())
                .withPath(getRequestPath(request))
                .withStatus(status.value())
                .withSuggestions(ex.getSuggestions());
        
        return ResponseEntity.status(status)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Resource not found [{}]: {} - {}", requestId, ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), ex.getMessage())
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.NOT_FOUND.value())
                .withSuggestions(ex.getSuggestions());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedAccessException(
            UnauthorizedAccessException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Unauthorized access [{}]: {} - {}", requestId, ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), ex.getMessage())
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.FORBIDDEN.value())
                .withSuggestions(ex.getSuggestions());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    // ========== Business Logic Exceptions ==========
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(
            BusinessException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Business exception [{}]: {} - {}", requestId, ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), ex.getMessage())
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.BAD_REQUEST.value())
                .withSuggestions(ex.getSuggestions());
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(DocumentRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleDocumentRequestException(
            DocumentRequestException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Document request exception [{}]: {} - {}", requestId, ex.getErrorCode(), ex.getMessage());
        
        HttpStatus status = determineHttpStatus(ex.getErrorCode());
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), ex.getMessage())
                .withPath(getRequestPath(request))
                .withStatus(status.value())
                .withSuggestions(ex.getSuggestions());
        
        return ResponseEntity.status(status)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ApiResponse<Object>> handleFileUploadException(
            FileUploadException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        HttpStatus status = ex.getHttpStatus();
        log.warn("File upload exception [{}]: {} - {} (Status: {})", 
                requestId, ex.getErrorCode(), ex.getMessage(), status.value());
        
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), ex.getMessage())
                .withPath(getRequestPath(request))
                .withStatus(status.value())
                .withSuggestions(ex.getSuggestions());
        
        return ResponseEntity.status(status)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserException(
            UserException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("User exception [{}]: {} - {}", requestId, ex.getErrorCode(), ex.getMessage());
        
        HttpStatus status = determineUserExceptionStatus(ex.getErrorCode());
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), ex.getMessage())
                .withPath(getRequestPath(request))
                .withStatus(status.value())
                .withSuggestions(ex.getSuggestions());
        
        return ResponseEntity.status(status)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            ValidationException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Validation exception [{}]: {} - {}", requestId, ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.validation(ex.getValidationErrors())
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.BAD_REQUEST.value())
                .withSuggestions(ex.getSuggestions());
        
        // Override error code if provided
        if (ex.getValidationErrors() != null && !ex.getValidationErrors().isEmpty()) {
            errorResponse.setErrorCode(ex.getErrorCode());
        }
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    // ========== Spring Framework Exceptions ==========
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation error [{}]: {}", requestId, errors);
        
        ErrorResponse errorResponse = ErrorResponse.validation(errors)
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.BAD_REQUEST.value());
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Invalid JSON format [{}]: {}", requestId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of("INVALID_JSON", "Invalid JSON format in request body")
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.BAD_REQUEST.value())
                .withSuggestions(List.of("Check JSON syntax", "Verify all required fields", "Ensure proper data types"));
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        Class<?> requiredType = ex.getRequiredType();
        String requiredTypeName = requiredType != null ? requiredType.getSimpleName() : "Unknown";
        log.warn("Type mismatch [{}]: Parameter '{}' should be of type '{}'", 
                requestId, ex.getName(), requiredTypeName);
        
        ErrorResponse errorResponse = ErrorResponse.of("TYPE_MISMATCH", 
                String.format("Parameter '%s' should be of type '%s'", ex.getName(), requiredTypeName))
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.BAD_REQUEST.value())
                .withSuggestions(List.of("Check parameter data type", "Verify URL parameters"));
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Missing parameter [{}]: {}", requestId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of("MISSING_PARAMETER", 
                String.format("Required parameter '%s' is missing", ex.getParameterName()))
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.BAD_REQUEST.value())
                .withSuggestions(List.of("Add the required parameter", "Check API documentation"));
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Method not supported [{}]: {}", requestId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of("METHOD_NOT_SUPPORTED", 
                String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()))
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.METHOD_NOT_ALLOWED.value())
                .withSuggestions(List.of("Use supported HTTP methods: " + String.join(", ", ex.getSupportedMethods())));
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    // ========== Security Exceptions ==========
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Bad credentials attempt [{}]: {}", requestId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of("INVALID_CREDENTIALS", "Invalid email or password")
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.UNAUTHORIZED.value())
                .withSuggestions(List.of("Check your email and password", "Reset password if forgotten"));
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsernameNotFoundException(
            UsernameNotFoundException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Username not found [{}]: {}", requestId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of("USER_NOT_FOUND", "User not found")
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.UNAUTHORIZED.value())
                .withSuggestions(List.of("Check your email address", "Register if you don't have an account"));
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Access denied [{}]: {}", requestId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of("ACCESS_DENIED", "Access denied")
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.FORBIDDEN.value())
                .withSuggestions(List.of("Check your permissions", "Contact administrator", "Login with appropriate role"));
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    // ========== File Upload Exceptions ==========
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("File upload size exceeded [{}]: {}", requestId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of("FILE_TOO_LARGE", "File size exceeds maximum allowed limit")
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .withSuggestions(List.of("Compress the file", "Split into smaller files", "Contact administrator"));
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<Object>> handleIOException(
            IOException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.error("IO exception [{}]: {}", requestId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.of("IO_ERROR", "File operation failed")
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .withSuggestions(List.of("Try again later", "Check file permissions", "Contact support with request ID: " + requestId));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    // ========== Database Exceptions ==========
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Data integrity violation [{}]: {}", requestId, ex.getMessage());
        
        String message = "Data integrity violation";
        String errorCode = "DATA_INTEGRITY_ERROR";
        
        if (ex.getMessage().contains("Duplicate entry")) {
            message = "Duplicate entry - record already exists";
            errorCode = "DUPLICATE_ENTRY";
        } else if (ex.getMessage().contains("foreign key constraint")) {
            message = "Referenced record does not exist";
            errorCode = "FOREIGN_KEY_VIOLATION";
        }
        
        ErrorResponse errorResponse = ErrorResponse.of(errorCode, message)
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.CONFLICT.value())
                .withSuggestions(List.of("Check for existing records", "Verify referenced data exists"));
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    // ========== Legacy Custom Exceptions ==========
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleEntityNotFoundException(
            EntityNotFoundException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Entity not found [{}]: {}", requestId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of("ENTITY_NOT_FOUND", ex.getMessage())
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.NOT_FOUND.value())
                .withSuggestions(List.of("Verify the ID", "Check if the resource exists"));
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateEntityException(
            DuplicateEntityException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Duplicate entity [{}]: {}", requestId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of("DUPLICATE_ENTITY", ex.getMessage())
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.CONFLICT.value())
                .withSuggestions(List.of("Use different values", "Check existing records"));
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedOperationException(
            UnauthorizedOperationException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Unauthorized operation [{}]: {}", requestId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of("UNAUTHORIZED_OPERATION", ex.getMessage())
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.FORBIDDEN.value())
                .withSuggestions(List.of("Check your permissions", "Contact administrator"));
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    // ========== Generic Exceptions ==========
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.warn("Illegal argument [{}]: {}", requestId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of("INVALID_ARGUMENT", ex.getMessage())
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.BAD_REQUEST.value())
                .withSuggestions(List.of("Check input parameters", "Verify data format"));
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.error("Runtime exception [{}]: {}", requestId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.of("RUNTIME_ERROR", "An unexpected error occurred")
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .withSuggestions(List.of("Try again later", "Contact support with request ID: " + requestId));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(
            Exception ex, WebRequest request) {
        
        String requestId = generateRequestId();
        log.error("Unexpected error [{}]: {}", requestId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred")
                .withPath(getRequestPath(request))
                .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .withSuggestions(List.of("Try again later", "Contact support with request ID: " + requestId));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse).withRequestId(requestId));
    }
    
    // ========== Helper Methods ==========
    
    private HttpStatus determineHttpStatus(String errorCode) {
        return switch (errorCode) {
            case DocumentRequestException.REQUEST_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DocumentRequestException.PROFESSOR_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DocumentRequestException.UNAUTHORIZED_ACCESS -> HttpStatus.FORBIDDEN;
            case DocumentRequestException.DEADLINE_PASSED -> HttpStatus.GONE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
    

    private HttpStatus determineUserExceptionStatus(String errorCode) {
        return switch (errorCode) {
            case UserException.USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UserException.DEPARTMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UserException.EMAIL_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case UserException.USER_HAS_DEPENDENCIES -> HttpStatus.CONFLICT;
            case UserException.CANNOT_DELETE_SELF -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
