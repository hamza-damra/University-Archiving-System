/**
 * FileExplorerState - Centralized state management for File Explorer
 * 
 * This module provides a singleton state manager for the File Explorer component,
 * ensuring consistent state across all dashboards (Dean, Professor, HOD).
 * 
 * Features:
 * - Centralized state storage
 * - Observer pattern for reactive updates
 * - Loading state management
 * - Academic context tracking
 * - Tree expansion state
 */

class FileExplorerState {
    constructor() {
        this.state = {
            // Academic context
            academicYearId: null,
            semesterId: null,
            yearCode: null,
            semesterType: null,
            
            // Tree data
            treeRoot: null,
            currentNode: null,
            currentPath: '',
            breadcrumbs: [],
            expandedNodes: new Set(),
            
            // Loading states
            isLoading: false,
            isTreeLoading: false,
            isFileListLoading: false,
            
            // Error state
            error: null,
            
            // Metadata
            lastUpdated: null,
        };
        
        // Observer pattern - list of subscribers
        this.listeners = [];
    }
    
    // ==================== State Getters ====================
    
    /**
     * Get immutable copy of entire state
     * @returns {Object} Copy of current state
     */
    getState() {
        return {
            ...this.state,
            expandedNodes: new Set(this.state.expandedNodes), // Clone Set
        };
    }
    
    /**
     * Get academic context (year and semester)
     * @returns {Object} Context object with academicYearId, semesterId, yearCode, semesterType
     */
    getContext() {
        return {
            academicYearId: this.state.academicYearId,
            semesterId: this.state.semesterId,
            yearCode: this.state.yearCode,
            semesterType: this.state.semesterType,
        };
    }
    
    /**
     * Get tree root
     * @returns {Object|null} Tree root node
     */
    getTreeRoot() {
        return this.state.treeRoot;
    }
    
    /**
     * Get current node
     * @returns {Object|null} Current node
     */
    getCurrentNode() {
        return this.state.currentNode;
    }
    
    /**
     * Get current path
     * @returns {string} Current path
     */
    getCurrentPath() {
        return this.state.currentPath;
    }
    
    /**
     * Get breadcrumbs
     * @returns {Array} Breadcrumbs array
     */
    getBreadcrumbs() {
        return this.state.breadcrumbs;
    }
    
    /**
     * Get expanded nodes
     * @returns {Set} Set of expanded node paths
     */
    getExpandedNodes() {
        return new Set(this.state.expandedNodes);
    }
    
    /**
     * Check if a node is expanded
     * @param {string} path - Node path
     * @returns {boolean} True if node is expanded
     */
    isNodeExpanded(path) {
        return this.state.expandedNodes.has(path);
    }
    
    // ==================== State Setters ====================
    
    /**
     * Set academic context (year and semester)
     * @param {number} academicYearId - Academic year ID
     * @param {number} semesterId - Semester ID
     * @param {string} yearCode - Year code (e.g., "2024-2025")
     * @param {string} semesterType - Semester type (e.g., "first", "second")
     */
    setContext(academicYearId, semesterId, yearCode, semesterType) {
        this.state.academicYearId = academicYearId;
        this.state.semesterId = semesterId;
        this.state.yearCode = yearCode;
        this.state.semesterType = semesterType;
        this.state.lastUpdated = Date.now();
        this.notify();
    }
    
    /**
     * Set tree root
     * @param {Object} treeRoot - Tree root node
     */
    setTreeRoot(treeRoot) {
        this.state.treeRoot = treeRoot;
        this.state.lastUpdated = Date.now();
        this.notify();
    }
    
    /**
     * Set current node and path
     * @param {Object} node - Current node
     * @param {string} path - Current path
     */
    setCurrentNode(node, path) {
        this.state.currentNode = node;
        this.state.currentPath = path || '';
        this.state.lastUpdated = Date.now();
        this.notify();
    }
    
    /**
     * Set breadcrumbs
     * @param {Array} breadcrumbs - Breadcrumbs array
     */
    setBreadcrumbs(breadcrumbs) {
        this.state.breadcrumbs = breadcrumbs || [];
        this.notify();
    }
    
    /**
     * Toggle node expansion state
     * @param {string} path - Node path to toggle
     */
    toggleNodeExpansion(path) {
        if (this.state.expandedNodes.has(path)) {
            this.state.expandedNodes.delete(path);
        } else {
            this.state.expandedNodes.add(path);
        }
        this.notify();
    }
    
    /**
     * Expand a node
     * @param {string} path - Node path to expand
     */
    expandNode(path) {
        if (!this.state.expandedNodes.has(path)) {
            this.state.expandedNodes.add(path);
            this.notify();
        }
    }
    
    /**
     * Collapse a node
     * @param {string} path - Node path to collapse
     */
    collapseNode(path) {
        if (this.state.expandedNodes.has(path)) {
            this.state.expandedNodes.delete(path);
            this.notify();
        }
    }
    
    /**
     * Clear all expanded nodes
     */
    clearExpandedNodes() {
        this.state.expandedNodes.clear();
        this.notify();
    }
    
    // ==================== Loading State Setters ====================
    
    /**
     * Set general loading state (both tree and file list)
     * @param {boolean} isLoading - Loading state
     */
    setLoading(isLoading) {
        this.state.isLoading = isLoading;
        this.notify();
    }
    
    /**
     * Set tree loading state
     * @param {boolean} isLoading - Loading state
     */
    setTreeLoading(isLoading) {
        this.state.isTreeLoading = isLoading;
        this.notify();
    }
    
    /**
     * Set file list loading state
     * @param {boolean} isLoading - Loading state
     */
    setFileListLoading(isLoading) {
        this.state.isFileListLoading = isLoading;
        this.notify();
    }
    
    // ==================== Error State Setters ====================
    
    /**
     * Set error state
     * @param {string|Error} error - Error message or Error object
     */
    setError(error) {
        this.state.error = error instanceof Error ? error.message : error;
        this.notify();
    }
    
    /**
     * Clear error state
     */
    clearError() {
        this.state.error = null;
        this.notify();
    }
    
    // ==================== State Reset ====================
    
    /**
     * Reset all state to initial values
     */
    reset() {
        this.state = {
            academicYearId: null,
            semesterId: null,
            yearCode: null,
            semesterType: null,
            treeRoot: null,
            currentNode: null,
            currentPath: '',
            breadcrumbs: [],
            expandedNodes: new Set(),
            isLoading: false,
            isTreeLoading: false,
            isFileListLoading: false,
            error: null,
            lastUpdated: null,
        };
        this.notify();
    }
    
    /**
     * Reset only data (keep context)
     */
    resetData() {
        this.state.treeRoot = null;
        this.state.currentNode = null;
        this.state.currentPath = '';
        this.state.breadcrumbs = [];
        this.state.expandedNodes.clear();
        this.state.isLoading = false;
        this.state.isTreeLoading = false;
        this.state.isFileListLoading = false;
        this.state.error = null;
        this.state.lastUpdated = null;
        this.notify();
    }
    
    // ==================== Observer Pattern ====================
    
    /**
     * Subscribe to state changes
     * @param {Function} listener - Callback function to be called on state changes
     * @returns {Function} Unsubscribe function
     */
    subscribe(listener) {
        if (typeof listener !== 'function') {
            throw new Error('Listener must be a function');
        }
        
        this.listeners.push(listener);
        
        // Return unsubscribe function
        return () => {
            this.listeners = this.listeners.filter(l => l !== listener);
        };
    }
    
    /**
     * Notify all subscribers of state change
     * @private
     */
    notify() {
        const state = this.getState();
        this.listeners.forEach(listener => {
            try {
                listener(state);
            } catch (error) {
                console.error('Error in FileExplorerState listener:', error);
            }
        });
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Check if context is set
     * @returns {boolean} True if both academicYearId and semesterId are set
     */
    hasContext() {
        return this.state.academicYearId !== null && this.state.semesterId !== null;
    }
    
    /**
     * Check if any loading state is active
     * @returns {boolean} True if any loading state is true
     */
    isAnyLoading() {
        return this.state.isLoading || this.state.isTreeLoading || this.state.isFileListLoading;
    }
    
    /**
     * Get last updated timestamp
     * @returns {number|null} Timestamp of last update
     */
    getLastUpdated() {
        return this.state.lastUpdated;
    }
}

// Export singleton instance
export const fileExplorerState = new FileExplorerState();
