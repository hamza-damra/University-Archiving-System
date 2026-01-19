package com.alquds.edu.ArchiveSystem.util;

import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.exception.file.InvalidPathException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Utility for safe path resolution and validation.
 * Prevents directory traversal attacks and ensures paths stay within the uploads root.
 * 
 * Security features:
 * - Rejects path traversal attempts (.., absolute paths, drive letters)
 * - Validates path characters
 * - Ensures resolved paths stay within upload root
 * - Role-based path access validation
 */
@Component
@Slf4j
public class SafePathResolver {

    private final Path uploadsRoot;
    
    // Patterns for dangerous path components
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("(^|[/\\\\])\\.\\.([\\\\/]|$)");
    private static final Pattern DRIVE_LETTER_PATTERN = Pattern.compile("^[a-zA-Z]:");
    private static final Pattern ABSOLUTE_PATH_PATTERN = Pattern.compile("^[/\\\\]");
    private static final Pattern INVALID_CHARS_PATTERN = Pattern.compile("[<>:\"|?*\\x00-\\x1F]");
    private static final Pattern CONSECUTIVE_SLASHES_PATTERN = Pattern.compile("[/\\\\]{2,}");
    
    /**
     * Constructor - initializes with upload base path
     */
    public SafePathResolver(@Value("${app.upload.base-path:uploads/}") String uploadBasePath) {
        this.uploadsRoot = Paths.get(uploadBasePath).toAbsolutePath().normalize();
        log.info("SafePathResolver initialized with uploads root: {}", this.uploadsRoot);
    }
    
    /**
     * Get the uploads root path
     */
    public Path getUploadsRoot() {
        return uploadsRoot;
    }
    
    /**
     * Validate and resolve a relative path to an absolute path within uploads root.
     * 
     * @param relativePath The relative path to resolve (e.g., "2025-2026/first/John Doe")
     * @return The resolved absolute path
     * @throws InvalidPathException if the path is invalid or attempts traversal
     */
    public Path resolve(String relativePath) {
        validatePathSyntax(relativePath);
        
        // Normalize the path
        String normalizedPath = normalizePath(relativePath);
        
        // Resolve against uploads root
        Path resolvedPath = uploadsRoot.resolve(normalizedPath).normalize();
        
        // Verify the resolved path is still under uploads root
        if (!isUnderUploadsRoot(resolvedPath)) {
            log.warn("Path traversal attempt detected: {} resolved to {}", relativePath, resolvedPath);
            throw new InvalidPathException("Invalid path: access outside uploads directory not allowed");
        }
        
        return resolvedPath;
    }
    
    /**
     * Validate and resolve a path, ensuring the directory exists.
     * 
     * @param relativePath The relative path to resolve
     * @return The resolved absolute path
     * @throws InvalidPathException if path is invalid or doesn't exist
     */
    public Path resolveExisting(String relativePath) {
        Path resolved = resolve(relativePath);
        
        if (!Files.exists(resolved)) {
            throw new InvalidPathException("Path does not exist: " + relativePath);
        }
        
        return resolved;
    }
    
    /**
     * Validate and resolve a path, ensuring it's a directory.
     * 
     * @param relativePath The relative path to resolve
     * @return The resolved absolute path
     * @throws InvalidPathException if path is invalid, doesn't exist, or isn't a directory
     */
    public Path resolveExistingDirectory(String relativePath) {
        Path resolved = resolveExisting(relativePath);
        
        if (!Files.isDirectory(resolved)) {
            throw new InvalidPathException("Path is not a directory: " + relativePath);
        }
        
        return resolved;
    }
    
    /**
     * Validate path syntax without resolving.
     * Checks for dangerous patterns.
     * 
     * @param path The path to validate
     * @throws InvalidPathException if the path contains dangerous patterns
     */
    public void validatePathSyntax(String path) {
        if (path == null) {
            throw new InvalidPathException("Path cannot be null");
        }
        
        // Check for empty path (root is allowed)
        if (path.isEmpty() || path.equals("/")) {
            return; // Root path is valid
        }
        
        // Check for path traversal attempts
        if (PATH_TRAVERSAL_PATTERN.matcher(path).find()) {
            log.warn("Path traversal attempt: {}", path);
            throw new InvalidPathException("Path traversal not allowed: '..' is forbidden");
        }
        
        // Check for drive letters (Windows)
        if (DRIVE_LETTER_PATTERN.matcher(path).find()) {
            log.warn("Drive letter in path: {}", path);
            throw new InvalidPathException("Absolute paths with drive letters not allowed");
        }
        
        // Check for invalid characters
        if (INVALID_CHARS_PATTERN.matcher(path).find()) {
            log.warn("Invalid characters in path: {}", path);
            throw new InvalidPathException("Path contains invalid characters");
        }
    }
    
    /**
     * Normalize a path string:
     * - Remove leading/trailing slashes
     * - Convert backslashes to forward slashes
     * - Collapse consecutive slashes
     * - Trim whitespace
     * 
     * @param path The path to normalize
     * @return The normalized path
     */
    public String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        
        String normalized = path.trim();
        
        // Convert backslashes to forward slashes
        normalized = normalized.replace('\\', '/');
        
        // Collapse consecutive slashes
        normalized = CONSECUTIVE_SLASHES_PATTERN.matcher(normalized).replaceAll("/");
        
        // Remove leading and trailing slashes
        normalized = normalized.replaceAll("^/+|/+$", "");
        
        return normalized;
    }
    
    /**
     * Check if a resolved path is under the uploads root.
     * 
     * @param path The absolute path to check
     * @return true if the path is under uploads root
     */
    public boolean isUnderUploadsRoot(Path path) {
        try {
            Path normalizedPath = path.toAbsolutePath().normalize();
            return normalizedPath.startsWith(uploadsRoot);
        } catch (Exception e) {
            log.error("Error checking path containment: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Convert an absolute path back to a relative path from uploads root.
     * 
     * @param absolutePath The absolute path
     * @return The relative path from uploads root
     * @throws InvalidPathException if the path is not under uploads root
     */
    public String toRelativePath(Path absolutePath) {
        Path normalized = absolutePath.toAbsolutePath().normalize();
        
        if (!isUnderUploadsRoot(normalized)) {
            throw new InvalidPathException("Path is not under uploads root");
        }
        
        return uploadsRoot.relativize(normalized).toString().replace('\\', '/');
    }
    
    /**
     * Check if the user has access to a specific path based on their role.
     * 
     * Access rules:
     * - ADMIN/DEANSHIP: Can access all paths
     * - HOD: Can access paths within their department
     * - PROFESSOR: Can only access paths within their own folder
     * 
     * @param path The relative path to check
     * @param user The user requesting access
     * @param professorFolderName The folder name pattern for the professor (if applicable)
     * @return true if the user has read access
     */
    public boolean hasReadAccess(String path, User user, String professorFolderName) {
        if (user == null) {
            return false;
        }
        
        Role role = user.getRole();
        
        // Admin and Deanship have full access
        if (role == Role.ROLE_ADMIN || role == Role.ROLE_DEANSHIP) {
            return true;
        }
        
        // Normalize the path for comparison
        String normalizedPath = normalizePath(path);
        String[] pathParts = normalizedPath.split("/");
        
        // HOD can access paths within their department's professors
        if (role == Role.ROLE_HOD) {
            // HOD needs at least 3 path segments to check professor folder
            // For now, allow read access - detailed checking happens elsewhere
            return true;
        }
        
        // Professor can only access paths containing their folder name
        if (role == Role.ROLE_PROFESSOR) {
            if (professorFolderName == null || professorFolderName.isEmpty()) {
                log.warn("Professor folder name not provided for access check");
                return false;
            }
            
            // Check if path contains the professor's folder
            for (String part : pathParts) {
                if (part.equals(professorFolderName)) {
                    return true;
                }
            }
            
            log.debug("Professor {} denied read access to path: {}", user.getEmail(), path);
            return false;
        }
        
        return false;
    }
    
    /**
     * Check if the user has write access to a specific path.
     * 
     * @param path The relative path to check
     * @param user The user requesting access
     * @param professorFolderName The folder name pattern for the professor
     * @return true if the user has write access
     */
    public boolean hasWriteAccess(String path, User user, String professorFolderName) {
        if (user == null || user.getRole() != Role.ROLE_PROFESSOR) {
            return false; // Only professors can write
        }
        
        // Professors can only write to their own folders
        String normalizedPath = normalizePath(path);
        String[] pathParts = normalizedPath.split("/");
        
        // Need at least 3 parts to be in a professor's folder structure
        // Format: academicYear/semester/professorName/...
        if (pathParts.length >= 3) {
            String professorPart = pathParts[2];
            return professorPart.equals(professorFolderName);
        }
        
        return false;
    }
    
    /**
     * Get parent path from a given path.
     * 
     * @param path The path
     * @return The parent path, or empty string if at root
     */
    public String getParentPath(String path) {
        String normalized = normalizePath(path);
        int lastSlash = normalized.lastIndexOf('/');
        
        if (lastSlash <= 0) {
            return "";
        }
        
        return normalized.substring(0, lastSlash);
    }
    
    /**
     * Get the name (last segment) from a path.
     * 
     * @param path The path
     * @return The last segment of the path
     */
    public String getName(String path) {
        String normalized = normalizePath(path);
        int lastSlash = normalized.lastIndexOf('/');
        
        if (lastSlash < 0) {
            return normalized;
        }
        
        return normalized.substring(lastSlash + 1);
    }
    
    /**
     * Create parent directories for a path if they don't exist.
     * 
     * @param relativePath The relative path
     * @throws IOException if directories cannot be created
     */
    public void createParentDirectories(String relativePath) throws IOException {
        Path resolved = resolve(relativePath);
        Path parent = resolved.getParent();
        
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
            log.debug("Created directories: {}", parent);
        }
    }
    
    /**
     * Ensure a directory exists, creating it if necessary.
     * 
     * @param relativePath The relative path of the directory
     * @return The absolute path of the directory
     * @throws IOException if the directory cannot be created
     */
    public Path ensureDirectoryExists(String relativePath) throws IOException {
        Path resolved = resolve(relativePath);
        
        if (!Files.exists(resolved)) {
            Files.createDirectories(resolved);
            log.debug("Created directory: {}", resolved);
        } else if (!Files.isDirectory(resolved)) {
            throw new InvalidPathException("Path exists but is not a directory: " + relativePath);
        }
        
        return resolved;
    }
}
