package com.alqude.edu.ArchiveSystem.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private String errorCode;
    private String message;
    private String details;
    private LocalDateTime timestamp;
    private String path;
    private Integer status;
    private Map<String, String> validationErrors;
    private List<String> suggestions;
    
    public static ErrorResponse of(String errorCode, String message) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse of(String errorCode, String message, String details) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse validation(Map<String, String> validationErrors) {
        return ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message("Validation failed")
                .validationErrors(validationErrors)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public ErrorResponse withPath(String path) {
        this.path = path;
        return this;
    }
    
    public ErrorResponse withStatus(Integer status) {
        this.status = status;
        return this;
    }
    
    public ErrorResponse withSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
        return this;
    }
}
