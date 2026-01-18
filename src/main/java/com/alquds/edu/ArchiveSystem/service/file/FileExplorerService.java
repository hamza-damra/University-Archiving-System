package com.alquds.edu.ArchiveSystem.service.file;

import com.alquds.edu.ArchiveSystem.entity.user.User;

import com.alquds.edu.ArchiveSystem.dto.fileexplorer.BreadcrumbItem;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.CreateFolderRequest;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.CreateFolderResponse;
import com.alquds.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode;

import java.util.List;

/**
 * Service for hierarchical file navigation and permission management.
 * Provides role-based access to the academic file structure:
 * Year ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ Semester ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ Professor ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ Course ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ Document Type ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ Files
 */
public interface FileExplorerService {
    
    /**
     * Get the root node for file explorer based on academic year and semester.
     * Applies role-based filtering:
     * - Deanship: all professors in semester
     * - HOD: professors in HOD's department only
     * - Professor: all professors in same department
     *
     * @param academicYearId the academic year ID
     * @param semesterId the semester ID
     * @param currentUser the authenticated user
     * @return root node with filtered children
     */
    FileExplorerNode getRootNode(Long academicYearId, Long semesterId, User currentUser);
    
    /**
     * Get a specific node in the hierarchy by path.
     * Path format: /year/semester/professor/course/documentType
     *
     * @param nodePath the node path
     * @param currentUser the authenticated user
     * @return the node with its children
     */
    FileExplorerNode getNode(String nodePath, User currentUser);
    
    /**
     * Get children of a parent node.
     *
     * @param parentPath the parent node path
     * @param currentUser the authenticated user
     * @return list of child nodes
     */
    List<FileExplorerNode> getChildren(String parentPath, User currentUser);
    
    /**
     * Check if user can read a node.
     * - Deanship: all nodes
     * - HOD: department nodes only
     * - Professor: department nodes only
     *
     * @param nodePath the node path
     * @param user the user
     * @return true if user can read
     */
    boolean canRead(String nodePath, User user);
    
    /**
     * Check if user can write to a node.
     * - Professor: only for own courses
     *
     * @param nodePath the node path
     * @param user the user
     * @return true if user can write
     */
    boolean canWrite(String nodePath, User user);
    
    /**
     * Check if user can delete from a node.
     * - Professor: only for own files
     *
     * @param nodePath the node path
     * @param user the user
     * @return true if user can delete
     */
    boolean canDelete(String nodePath, User user);
    
    /**
     * Generate breadcrumb navigation for a path.
     *
     * @param nodePath the node path
     * @return list of breadcrumb items
     */
    List<BreadcrumbItem> generateBreadcrumbs(String nodePath);
    
    /**
     * Create a new folder at the specified path.
     * Only professors can create folders within their own namespace.
     *
     * @param request the folder creation request containing path and folder name
     * @param currentUser the authenticated user (must be a professor)
     * @return response containing the created folder details
     * @throws com.alquds.edu.ArchiveSystem.exception.file.InvalidFolderNameException if folder name is invalid
     * @throws com.alquds.edu.ArchiveSystem.exception.file.FolderAlreadyExistsException if folder already exists
     * @throws com.alquds.edu.ArchiveSystem.exception.auth.UnauthorizedOperationException if user lacks permission
     */
    CreateFolderResponse createFolder(CreateFolderRequest request, User currentUser);
}
