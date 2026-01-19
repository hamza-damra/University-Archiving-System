package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.dto.fileexplorer.*;
import com.alquds.edu.ArchiveSystem.entity.auth.Role;
import com.alquds.edu.ArchiveSystem.entity.file.Folder;
import com.alquds.edu.ArchiveSystem.entity.file.UploadedFile;
import com.alquds.edu.ArchiveSystem.entity.user.User;
import com.alquds.edu.ArchiveSystem.exception.file.InvalidPathException;
import com.alquds.edu.ArchiveSystem.repository.file.FolderRepository;
import com.alquds.edu.ArchiveSystem.repository.file.UploadedFileRepository;
import com.alquds.edu.ArchiveSystem.repository.user.UserRepository;
import com.alquds.edu.ArchiveSystem.util.SafePathResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of FilesystemScanService.
 * Provides filesystem-based directory scanning with caching and metadata enrichment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FilesystemScanServiceImpl implements FilesystemScanService {

    private final SafePathResolver pathResolver;
    private final UploadedFileRepository uploadedFileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    
    // Simple in-memory cache with TTL
    private final Map<String, CachedDirectoryListing> listingCache = new ConcurrentHashMap<>();
    private final Map<String, CachedETag> etagCache = new ConcurrentHashMap<>();
    
    // Cache TTL in milliseconds (15 seconds)
    private static final long CACHE_TTL_MS = 15_000;
    
    // Previewable file extensions
    private static final Set<String> PREVIEWABLE_EXTENSIONS = Set.of(
            "pdf", "jpg", "jpeg", "png", "gif", "webp", "svg",
            "txt", "md", "json", "xml", "html", "css", "js"
    );

    @Override
    public DirectoryListingDTO listDirectory(String relativePath, User currentUser,
            int page, int pageSize, String sortBy, String sortOrder) {
        
        log.debug("Listing directory: path={}, page={}, pageSize={}, sort={} {}",
                relativePath, page, pageSize, sortBy, sortOrder);
        
        // Normalize and validate path
        String normalizedPath = pathResolver.normalizePath(relativePath);
        
        // Check cache first
        String cacheKey = buildCacheKey(normalizedPath, currentUser.getId(), page, pageSize, sortBy, sortOrder);
        CachedDirectoryListing cached = listingCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached listing for: {}", normalizedPath);
            return cached.getListing();
        }
        
        // Resolve to filesystem path
        Path dirPath;
        if (normalizedPath.isEmpty()) {
            dirPath = pathResolver.getUploadsRoot();
        } else {
            dirPath = pathResolver.resolveExistingDirectory(normalizedPath);
        }
        
        // Scan the directory
        List<FolderItemDTO> folders = new ArrayList<>();
        List<FileItemDTO> files = new ArrayList<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path entry : stream) {
                BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                String entryRelativePath = pathResolver.toRelativePath(entry);
                
                if (attrs.isDirectory()) {
                    // Check if user has access to this folder
                    if (hasAccessToPath(entryRelativePath, currentUser)) {
                        FolderItemDTO folderItem = buildFolderItem(entry, entryRelativePath, attrs, currentUser);
                        folders.add(folderItem);
                    }
                } else if (attrs.isRegularFile()) {
                    FileItemDTO fileItem = buildFileItem(entry, entryRelativePath, attrs, currentUser);
                    files.add(fileItem);
                }
            }
        } catch (IOException e) {
            log.error("Error scanning directory: {}", normalizedPath, e);
            throw new InvalidPathException("Unable to read directory: " + e.getMessage());
        }
        
        // Sort items
        sortFolders(folders, sortBy, sortOrder);
        sortFiles(files, sortBy, sortOrder);
        
        // Calculate pagination
        int totalFolders = folders.size();
        int totalFiles = files.size();
        int totalItems = totalFolders + totalFiles;
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        
        // Apply pagination - folders first, then files
        List<FolderItemDTO> pagedFolders = new ArrayList<>();
        List<FileItemDTO> pagedFiles = new ArrayList<>();
        
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalItems);
        
        for (int i = startIndex; i < endIndex; i++) {
            if (i < totalFolders) {
                pagedFolders.add(folders.get(i));
            } else {
                pagedFiles.add(files.get(i - totalFolders));
            }
        }
        
        // Compute ETag
        String etag = computeDirectoryETag(normalizedPath);
        
        // Build response
        DirectoryListingDTO listing = DirectoryListingDTO.builder()
                .path(normalizedPath)
                .name(normalizedPath.isEmpty() ? "Uploads" : pathResolver.getName(normalizedPath))
                .folders(pagedFolders)
                .files(pagedFiles)
                .totalItems(totalItems)
                .page(page)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .hasMore(page < totalPages)
                .etag(etag)
                .canWrite(hasWriteAccess(normalizedPath, currentUser))
                .canDelete(hasDeleteAccess(normalizedPath, currentUser))
                .canCreateFolder(hasWriteAccess(normalizedPath, currentUser))
                .parentPath(pathResolver.getParentPath(normalizedPath))
                .build();
        
        // Cache the result
        listingCache.put(cacheKey, new CachedDirectoryListing(listing));
        
        return listing;
    }

    @Override
    public DirectoryTreeDTO getDirectoryTree(String relativePath, User currentUser, int depth) {
        log.debug("Getting directory tree: path={}, depth={}", relativePath, depth);
        
        String normalizedPath = pathResolver.normalizePath(relativePath);
        
        Path dirPath;
        if (normalizedPath.isEmpty()) {
            dirPath = pathResolver.getUploadsRoot();
        } else {
            dirPath = pathResolver.resolveExistingDirectory(normalizedPath);
        }
        
        return buildTreeNode(dirPath, normalizedPath, currentUser, depth);
    }

    @Override
    public boolean hasDirectoryChanged(String relativePath, String etag) {
        if (etag == null || etag.isEmpty()) {
            return true;
        }
        
        String currentETag = computeDirectoryETag(relativePath);
        return !etag.equals(currentETag);
    }

    @Override
    public String computeDirectoryETag(String relativePath) {
        String normalizedPath = pathResolver.normalizePath(relativePath);
        
        // Check cache
        CachedETag cached = etagCache.get(normalizedPath);
        if (cached != null && !cached.isExpired()) {
            return cached.getEtag();
        }
        
        Path dirPath;
        if (normalizedPath.isEmpty()) {
            dirPath = pathResolver.getUploadsRoot();
        } else {
            try {
                dirPath = pathResolver.resolveExistingDirectory(normalizedPath);
            } catch (InvalidPathException e) {
                return "W/\"not-found\"";
            }
        }
        
        try {
            BasicFileAttributes dirAttrs = Files.readAttributes(dirPath, BasicFileAttributes.class);
            long lastModified = dirAttrs.lastModifiedTime().toMillis();
            
            // Collect names and sizes of immediate children
            StringBuilder contentBuilder = new StringBuilder();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
                List<String> entries = new ArrayList<>();
                for (Path entry : stream) {
                    BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                    entries.add(entry.getFileName().toString() + ":" + attrs.size() + ":" + attrs.lastModifiedTime().toMillis());
                }
                Collections.sort(entries);
                entries.forEach(e -> contentBuilder.append(e).append(";"));
            }
            
            // Compute hash
            String content = lastModified + "|" + contentBuilder.toString();
            String hash = computeHash(content);
            
            String etag = "W/\"" + hash + "\"";
            
            // Cache the etag
            etagCache.put(normalizedPath, new CachedETag(etag));
            
            return etag;
        } catch (IOException e) {
            log.error("Error computing ETag for: {}", normalizedPath, e);
            return "W/\"error-" + System.currentTimeMillis() + "\"";
        }
    }

    @Override
    public void invalidateCache(String relativePath) {
        String normalizedPath = pathResolver.normalizePath(relativePath);
        log.debug("Invalidating cache for: {}", normalizedPath);
        
        // Remove all cache entries starting with this path
        listingCache.entrySet().removeIf(entry -> entry.getKey().startsWith(normalizedPath + ":"));
        etagCache.remove(normalizedPath);
    }

    @Override
    public void invalidateCacheRecursive(String relativePath) {
        String normalizedPath = pathResolver.normalizePath(relativePath);
        log.debug("Invalidating cache recursively for: {}", normalizedPath);
        
        // Invalidate this path
        invalidateCache(normalizedPath);
        
        // Invalidate parent paths up to root
        String parentPath = pathResolver.getParentPath(normalizedPath);
        while (!parentPath.isEmpty()) {
            invalidateCache(parentPath);
            parentPath = pathResolver.getParentPath(parentPath);
        }
        
        // Invalidate root
        invalidateCache("");
    }

    @Override
    public boolean pathExists(String relativePath) {
        try {
            Path resolved = pathResolver.resolve(relativePath);
            return Files.exists(resolved);
        } catch (InvalidPathException e) {
            return false;
        }
    }

    @Override
    public boolean isDirectory(String relativePath) {
        try {
            Path resolved = pathResolver.resolve(relativePath);
            return Files.isDirectory(resolved);
        } catch (InvalidPathException e) {
            return false;
        }
    }

    // ==================== Private Helper Methods ====================

    private FolderItemDTO buildFolderItem(Path path, String relativePath, BasicFileAttributes attrs, User currentUser) {
        String folderName = path.getFileName().toString();
        LocalDateTime modifiedAt = LocalDateTime.ofInstant(
                attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
        
        // Try to find folder in database for additional metadata
        Optional<Folder> dbFolder = folderRepository.findByPath(relativePath);
        
        // Determine folder type based on path depth and content
        String folderType = determineFolderType(relativePath);
        
        // Check permissions
        boolean canWrite = hasWriteAccess(relativePath, currentUser);
        boolean canDelete = hasDeleteAccess(relativePath, currentUser);
        boolean isSystemFolder = isSystemFolder(folderType);
        
        // Count items (non-recursive)
        int itemCount = -1; // Default to -1 to indicate not computed
        try {
            itemCount = (int) Files.list(path).count();
        } catch (IOException e) {
            log.warn("Could not count items in folder: {}", relativePath);
        }
        
        // Build metadata
        FolderItemDTO.FolderMetadata metadata = buildFolderMetadata(relativePath, dbFolder.orElse(null), currentUser);
        
        return FolderItemDTO.builder()
                .name(folderName)
                .path(relativePath)
                .modifiedAt(modifiedAt)
                .id(dbFolder.map(Folder::getId).orElse(null))
                .folderType(folderType)
                .itemCount(itemCount)
                .canWrite(canWrite)
                .canDelete(canDelete && !isSystemFolder)
                .isSystemFolder(isSystemFolder)
                .metadata(metadata)
                .build();
    }

    private FileItemDTO buildFileItem(Path path, String relativePath, BasicFileAttributes attrs, User currentUser) {
        String fileName = path.getFileName().toString();
        LocalDateTime modifiedAt = LocalDateTime.ofInstant(
                attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
        
        // Get file extension
        String extension = getFileExtension(fileName);
        
        // Determine MIME type
        String mimeType = determineMimeType(path, extension);
        
        // Try to find file in database
        Optional<UploadedFile> dbFile = uploadedFileRepository.findByFileUrl(relativePath);
        if (!dbFile.isPresent()) {
            // Try alternative lookup by stored filename
            dbFile = uploadedFileRepository.findByStoredFilename(fileName);
        }
        
        // Get uploader info from database
        String uploaderName = null;
        Long uploaderId = null;
        LocalDateTime uploadedAt = null;
        String notes = null;
        Long fileId = null;
        
        if (dbFile.isPresent()) {
            UploadedFile file = dbFile.get();
            fileId = file.getId();
            uploadedAt = file.getCreatedAt();
            notes = file.getNotes();
            if (file.getUploader() != null) {
                uploaderName = file.getUploader().getFirstName() + " " + file.getUploader().getLastName();
                uploaderId = file.getUploader().getId();
            }
        }
        
        // Check permissions
        boolean canDelete = canDeleteFile(relativePath, currentUser, dbFile.orElse(null));
        boolean canReplace = canDelete; // Same permission for replace
        
        // Check if previewable
        boolean previewable = PREVIEWABLE_EXTENSIONS.contains(extension.toLowerCase());
        
        return FileItemDTO.builder()
                .name(dbFile.map(UploadedFile::getOriginalFilename).orElse(fileName))
                .storedName(fileName)
                .path(relativePath)
                .size(attrs.size())
                .sizeFormatted(formatFileSize(attrs.size()))
                .mimeType(mimeType)
                .extension(extension)
                .modifiedAt(modifiedAt)
                .uploadedAt(uploadedAt)
                .id(fileId)
                .uploaderName(uploaderName)
                .uploaderId(uploaderId)
                .notes(notes)
                .orphaned(!dbFile.isPresent())
                .canDelete(canDelete)
                .canReplace(canReplace)
                .previewable(previewable)
                .downloadUrl(fileId != null ? "/api/file-explorer/files/" + fileId + "/download" : null)
                .previewUrl(previewable && fileId != null ? "/api/file-explorer/files/" + fileId + "/preview" : null)
                .build();
    }

    private DirectoryTreeDTO buildTreeNode(Path path, String relativePath, User currentUser, int depth) {
        String name = relativePath.isEmpty() ? "Uploads" : path.getFileName().toString();
        
        BasicFileAttributes attrs;
        try {
            attrs = Files.readAttributes(path, BasicFileAttributes.class);
        } catch (IOException e) {
            log.error("Error reading attributes for: {}", relativePath, e);
            return null;
        }
        
        LocalDateTime modifiedAt = LocalDateTime.ofInstant(
                attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
        
        // Determine node type
        String nodeType = determineNodeType(relativePath);
        
        // Count immediate children
        int fileCount = 0;
        int folderCount = 0;
        boolean hasChildren = false;
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                hasChildren = true;
                if (Files.isDirectory(entry)) {
                    folderCount++;
                } else {
                    fileCount++;
                }
            }
        } catch (IOException e) {
            log.warn("Error counting children for: {}", relativePath);
        }
        
        // Build children if depth > 0
        List<DirectoryTreeDTO> children = new ArrayList<>();
        boolean childrenLoaded = false;
        
        if (depth > 0 && hasChildren) {
            childrenLoaded = true;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        String childPath = pathResolver.toRelativePath(entry);
                        if (hasAccessToPath(childPath, currentUser)) {
                            DirectoryTreeDTO childNode = buildTreeNode(entry, childPath, currentUser, depth - 1);
                            if (childNode != null) {
                                children.add(childNode);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                log.warn("Error loading children for: {}", relativePath);
            }
            
            // Sort children by name
            children.sort(Comparator.comparing(DirectoryTreeDTO::getName, String.CASE_INSENSITIVE_ORDER));
        }
        
        // Build metadata
        DirectoryTreeDTO.TreeNodeMetadata metadata = buildTreeMetadata(relativePath, currentUser);
        
        return DirectoryTreeDTO.builder()
                .name(name)
                .path(relativePath)
                .type(nodeType)
                .entityId(getFolderEntityId(relativePath))
                .modifiedAt(modifiedAt)
                .hasChildren(hasChildren)
                .children(children)
                .childrenLoaded(childrenLoaded)
                .canWrite(hasWriteAccess(relativePath, currentUser))
                .canDelete(hasDeleteAccess(relativePath, currentUser))
                .etag(computeDirectoryETag(relativePath))
                .fileCount(fileCount)
                .folderCount(folderCount)
                .metadata(metadata)
                .build();
    }

    private String determineFolderType(String relativePath) {
        String[] parts = relativePath.split("/");
        switch (parts.length) {
            case 1: return "YEAR";
            case 2: return "SEMESTER";
            case 3: return "PROFESSOR";
            case 4: return "COURSE";
            case 5: return isDocumentTypeFolder(parts[4]) ? "DOCUMENT_TYPE" : "CUSTOM";
            default: return "CUSTOM";
        }
    }

    private String determineNodeType(String relativePath) {
        if (relativePath.isEmpty()) return "ROOT";
        return determineFolderType(relativePath);
    }

    private boolean isDocumentTypeFolder(String name) {
        String normalized = name.toLowerCase().replace("-", "_");
        return normalized.equals("syllabus") || 
               normalized.equals("exams") || 
               normalized.equals("lecture_notes") ||
               normalized.equals("course_notes") ||
               normalized.equals("assignments");
    }

    private boolean isSystemFolder(String folderType) {
        return "YEAR".equals(folderType) || 
               "SEMESTER".equals(folderType) || 
               "PROFESSOR".equals(folderType) ||
               "COURSE".equals(folderType) ||
               "DOCUMENT_TYPE".equals(folderType);
    }

    private FolderItemDTO.FolderMetadata buildFolderMetadata(String relativePath, Folder dbFolder, User currentUser) {
        String[] parts = relativePath.split("/");
        
        FolderItemDTO.FolderMetadata.FolderMetadataBuilder builder = FolderItemDTO.FolderMetadata.builder();
        
        if (parts.length >= 3) {
            // Professor folder or deeper
            String professorFolderName = parts[2];
            Optional<User> professor = findProfessorByFolderName(professorFolderName);
            if (professor.isPresent()) {
                User prof = professor.get();
                builder.professorId(prof.getId())
                       .professorName(prof.getFirstName() + " " + prof.getLastName())
                       .isOwnFolder(prof.getId().equals(currentUser.getId()));
                
                if (prof.getDepartment() != null) {
                    builder.departmentId(prof.getDepartment().getId())
                           .departmentName(prof.getDepartment().getName());
                }
            }
        }
        
        return builder.build();
    }

    private DirectoryTreeDTO.TreeNodeMetadata buildTreeMetadata(String relativePath, User currentUser) {
        String[] parts = relativePath.split("/");
        
        DirectoryTreeDTO.TreeNodeMetadata.TreeNodeMetadataBuilder builder = 
                DirectoryTreeDTO.TreeNodeMetadata.builder();
        
        if (parts.length >= 1 && !parts[0].isEmpty()) {
            builder.academicYearCode(parts[0]);
        }
        if (parts.length >= 2) {
            builder.semesterType(parts[1]);
        }
        if (parts.length >= 3) {
            String professorFolderName = parts[2];
            Optional<User> professor = findProfessorByFolderName(professorFolderName);
            if (professor.isPresent()) {
                User prof = professor.get();
                builder.professorId(prof.getId())
                       .professorName(prof.getFirstName() + " " + prof.getLastName())
                       .professorEmail(prof.getEmail())
                       .isOwnFolder(prof.getId().equals(currentUser.getId()));
                
                if (prof.getDepartment() != null) {
                    builder.departmentId(prof.getDepartment().getId())
                           .departmentName(prof.getDepartment().getName());
                }
            }
        }
        if (parts.length >= 4) {
            builder.courseCode(parts[3]);
        }
        
        return builder.build();
    }

    private Optional<User> findProfessorByFolderName(String folderName) {
        // Try to find professor whose full name matches the folder name
        List<User> professors = userRepository.findByRole(Role.ROLE_PROFESSOR);
        for (User prof : professors) {
            String fullName = (prof.getFirstName() + " " + prof.getLastName()).trim();
            String sanitized = fullName.replaceAll("[\\\\/:*?\"<>|]", "_")
                    .replaceAll("\\s+", " ")
                    .replaceAll("_+", "_")
                    .trim();
            if (sanitized.equals(folderName) || fullName.equals(folderName)) {
                return Optional.of(prof);
            }
        }
        
        // Try prof_<id> format
        if (folderName.startsWith("prof_")) {
            try {
                Long id = Long.parseLong(folderName.substring(5));
                return userRepository.findById(id);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        
        return Optional.empty();
    }

    private Long getFolderEntityId(String relativePath) {
        Optional<Folder> folder = folderRepository.findByPath(relativePath);
        return folder.map(Folder::getId).orElse(null);
    }

    private boolean hasAccessToPath(String relativePath, User currentUser) {
        if (currentUser.getRole() == Role.ROLE_ADMIN || currentUser.getRole() == Role.ROLE_DEANSHIP) {
            return true;
        }
        
        String[] parts = relativePath.split("/");
        if (parts.length < 3) {
            return true; // Year/semester level is accessible to all authenticated users
        }
        
        // For HOD, check if professor is in their department
        if (currentUser.getRole() == Role.ROLE_HOD) {
            String professorFolderName = parts[2];
            Optional<User> professor = findProfessorByFolderName(professorFolderName);
            if (professor.isPresent() && currentUser.getDepartment() != null) {
                User prof = professor.get();
                return prof.getDepartment() != null && 
                       prof.getDepartment().getId().equals(currentUser.getDepartment().getId());
            }
            return false;
        }
        
        // For Professor, check if it's their own folder or same department
        if (currentUser.getRole() == Role.ROLE_PROFESSOR) {
            String professorFolderName = parts[2];
            Optional<User> professor = findProfessorByFolderName(professorFolderName);
            if (professor.isPresent() && currentUser.getDepartment() != null) {
                User prof = professor.get();
                return prof.getDepartment() != null && 
                       prof.getDepartment().getId().equals(currentUser.getDepartment().getId());
            }
            return false;
        }
        
        return false;
    }

    private boolean hasWriteAccess(String relativePath, User currentUser) {
        if (currentUser.getRole() != Role.ROLE_PROFESSOR) {
            return false;
        }
        
        String[] parts = relativePath.split("/");
        if (parts.length < 3) {
            return false; // Can't write at year/semester level
        }
        
        String professorFolderName = parts[2];
        String profFullName = (currentUser.getFirstName() + " " + currentUser.getLastName()).trim()
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", " ")
                .replaceAll("_+", "_")
                .trim();
        
        return professorFolderName.equals(profFullName) || 
               professorFolderName.equals("prof_" + currentUser.getId());
    }

    private boolean hasDeleteAccess(String relativePath, User currentUser) {
        // Same as write access for now
        return hasWriteAccess(relativePath, currentUser);
    }

    private boolean canDeleteFile(String relativePath, User currentUser, UploadedFile dbFile) {
        if (currentUser.getRole() != Role.ROLE_PROFESSOR) {
            return false;
        }
        
        // If we have DB record, check uploader
        if (dbFile != null && dbFile.getUploader() != null) {
            return dbFile.getUploader().getId().equals(currentUser.getId());
        }
        
        // Otherwise, check path ownership
        return hasWriteAccess(relativePath, currentUser);
    }

    private void sortFolders(List<FolderItemDTO> folders, String sortBy, String sortOrder) {
        Comparator<FolderItemDTO> comparator;
        
        switch (sortBy != null ? sortBy.toLowerCase() : "name") {
            case "modifiedat":
            case "modified":
                comparator = Comparator.comparing(FolderItemDTO::getModifiedAt, 
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "name":
            default:
                comparator = Comparator.comparing(FolderItemDTO::getName, String.CASE_INSENSITIVE_ORDER);
        }
        
        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }
        
        folders.sort(comparator);
    }

    private void sortFiles(List<FileItemDTO> files, String sortBy, String sortOrder) {
        Comparator<FileItemDTO> comparator;
        
        switch (sortBy != null ? sortBy.toLowerCase() : "name") {
            case "modifiedat":
            case "modified":
                comparator = Comparator.comparing(FileItemDTO::getModifiedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "size":
                comparator = Comparator.comparing(FileItemDTO::getSize,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "name":
            default:
                comparator = Comparator.comparing(FileItemDTO::getName, String.CASE_INSENSITIVE_ORDER);
        }
        
        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }
        
        files.sort(comparator);
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    private String determineMimeType(Path path, String extension) {
        try {
            String probed = Files.probeContentType(path);
            if (probed != null) {
                return probed;
            }
        } catch (IOException e) {
            // Fall through to extension-based detection
        }
        
        // Fallback to extension-based
        switch (extension.toLowerCase()) {
            case "pdf": return "application/pdf";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt": return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt": return "text/plain";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "zip": return "application/zip";
            case "rar": return "application/x-rar-compressed";
            default: return "application/octet-stream";
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    private String computeHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 12); // First 12 chars
        } catch (NoSuchAlgorithmException e) {
            return Long.toHexString(input.hashCode());
        }
    }

    private String buildCacheKey(String path, Long userId, int page, int pageSize, String sortBy, String sortOrder) {
        return path + ":" + userId + ":" + page + ":" + pageSize + ":" + sortBy + ":" + sortOrder;
    }

    // ==================== Cache Classes ====================

    private static class CachedDirectoryListing {
        private final DirectoryListingDTO listing;
        private final long timestamp;

        CachedDirectoryListing(DirectoryListingDTO listing) {
            this.listing = listing;
            this.timestamp = System.currentTimeMillis();
        }

        DirectoryListingDTO getListing() {
            return listing;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    private static class CachedETag {
        private final String etag;
        private final long timestamp;

        CachedETag(String etag) {
            this.etag = etag;
            this.timestamp = System.currentTimeMillis();
        }

        String getEtag() {
            return etag;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
