package com.alqude.edu.ArchiveSystem.service;

import com.alqude.edu.ArchiveSystem.dto.fileexplorer.BreadcrumbItem;
import com.alqude.edu.ArchiveSystem.dto.fileexplorer.FileExplorerNode;
import com.alqude.edu.ArchiveSystem.entity.User;

import java.util.List;

/**
 * Service for hierarchical file navigation and permission management.
 * Provides role-based access to the academic file structure:
 * Year → Semester → Professor → Course → Document Type → Files
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
}
