/**
 * DashboardState - Centralized state management for Dean Dashboard
 * Implements observer pattern for reactive state updates
 */

class DashboardState {
    constructor() {
        // Data state
        this._academicYears = [];
        this._professors = [];
        this._courses = [];
        this._departments = [];
        this._semesters = [];
        this._assignments = [];
        
        // UI state
        this._currentTab = 'dashboard';
        this._selectedAcademicYearId = null;
        this._selectedAcademicYear = null;
        this._selectedSemesterId = null;
        this._sidebarCollapsed = false;
        this._filters = {};
        
        // Selection state
        this._selectedRows = new Set();
        
        // Observers
        this._observers = {};
        
        // Initialize from localStorage
        this._loadPersistedState();
    }
    
    // ============================================================================
    // DATA STATE MANAGEMENT
    // ============================================================================
    
    getAcademicYears() {
        return this._academicYears;
    }
    
    setAcademicYears(years) {
        this._academicYears = years;
        this.notify('academicYears');
    }
    
    getProfessors() {
        return this._professors;
    }
    
    setProfessors(professors) {
        this._professors = professors;
        this.notify('professors');
    }
    
    getCourses() {
        return this._courses;
    }
    
    setCourses(courses) {
        this._courses = courses;
        this.notify('courses');
    }
    
    getDepartments() {
        return this._departments;
    }
    
    setDepartments(departments) {
        this._departments = departments;
        this.notify('departments');
    }
    
    getSemesters() {
        return this._semesters;
    }
    
    setSemesters(semesters) {
        this._semesters = semesters;
        this.notify('semesters');
    }
    
    getAssignments() {
        return this._assignments;
    }
    
    setAssignments(assignments) {
        this._assignments = assignments;
        this.notify('assignments');
    }
    
    // ============================================================================
    // UI STATE MANAGEMENT
    // ============================================================================
    
    getCurrentTab() {
        return this._currentTab;
    }
    
    setCurrentTab(tab) {
        this._currentTab = tab;
        localStorage.setItem('deanship-active-tab', tab);
        this.notify('currentTab');
    }
    
    getSidebarCollapsed() {
        return this._sidebarCollapsed;
    }
    
    setSidebarCollapsed(collapsed) {
        this._sidebarCollapsed = collapsed;
        localStorage.setItem('deanship_sidebar_collapsed', collapsed);
        this.notify('sidebarCollapsed');
    }
    
    getFilters() {
        return this._filters;
    }
    
    setFilters(filters) {
        this._filters = { ...this._filters, ...filters };
        this.notify('filters');
    }
    
    clearFilters() {
        this._filters = {};
        this.notify('filters');
    }
    
    // ============================================================================
    // ACADEMIC YEAR & SEMESTER SELECTION
    // ============================================================================
    
    getSelectedAcademicYearId() {
        return this._selectedAcademicYearId;
    }
    
    getSelectedAcademicYear() {
        return this._selectedAcademicYear;
    }
    
    setSelectedAcademicYear(yearId) {
        this._selectedAcademicYearId = yearId;
        this._selectedAcademicYear = this._academicYears.find(y => y.id === yearId) || null;
        localStorage.setItem('deanship_selected_academic_year', yearId || '');
        
        // Reset semester when year changes
        if (yearId) {
            const year = this._academicYears.find(y => y.id === yearId);
            if (year && year.semesters) {
                this.setSemesters(year.semesters);
            }
        } else {
            this._selectedSemesterId = null;
            this.setSemesters([]);
        }
        
        this.notify('selectedAcademicYear');
    }
    
    getSelectedSemesterId() {
        return this._selectedSemesterId;
    }
    
    getSelectedSemester() {
        return this._semesters.find(s => s.id === this._selectedSemesterId) || null;
    }
    
    setSelectedSemester(semesterId) {
        this._selectedSemesterId = semesterId;
        localStorage.setItem('deanship_selected_semester', semesterId || '');
        this.notify('selectedSemester');
    }
    
    // ============================================================================
    // SELECTION STATE MANAGEMENT
    // ============================================================================
    
    getSelectedRows() {
        return Array.from(this._selectedRows);
    }
    
    setSelectedRows(rows) {
        this._selectedRows = new Set(rows);
        this.notify('selectedRows');
    }
    
    addSelectedRow(rowId) {
        this._selectedRows.add(rowId);
        this.notify('selectedRows');
    }
    
    removeSelectedRow(rowId) {
        this._selectedRows.delete(rowId);
        this.notify('selectedRows');
    }
    
    clearSelection() {
        this._selectedRows.clear();
        this.notify('selectedRows');
    }
    
    isRowSelected(rowId) {
        return this._selectedRows.has(rowId);
    }
    
    // ============================================================================
    // OBSERVER PATTERN
    // ============================================================================
    
    /**
     * Subscribe to state changes
     * @param {string} key - State key to observe
     * @param {Function} callback - Callback function to execute on change
     * @returns {Function} Unsubscribe function
     */
    subscribe(key, callback) {
        if (!this._observers[key]) {
            this._observers[key] = [];
        }
        this._observers[key].push(callback);
        
        // Return unsubscribe function
        return () => this.unsubscribe(key, callback);
    }
    
    /**
     * Unsubscribe from state changes
     * @param {string} key - State key
     * @param {Function} callback - Callback function to remove
     */
    unsubscribe(key, callback) {
        if (!this._observers[key]) return;
        this._observers[key] = this._observers[key].filter(cb => cb !== callback);
    }
    
    /**
     * Notify observers of state change
     * @param {string} key - State key that changed
     */
    notify(key) {
        if (!this._observers[key]) return;
        this._observers[key].forEach(callback => {
            try {
                callback(this._getStateValue(key));
            } catch (error) {
                console.error(`Error in observer callback for ${key}:`, error);
            }
        });
    }
    
    /**
     * Get state value by key
     * @param {string} key - State key
     * @returns {*} State value
     */
    _getStateValue(key) {
        switch (key) {
            case 'academicYears': return this._academicYears;
            case 'professors': return this._professors;
            case 'courses': return this._courses;
            case 'departments': return this._departments;
            case 'semesters': return this._semesters;
            case 'assignments': return this._assignments;
            case 'currentTab': return this._currentTab;
            case 'selectedAcademicYear': return { id: this._selectedAcademicYearId, data: this._selectedAcademicYear };
            case 'selectedSemester': return this._selectedSemesterId;
            case 'sidebarCollapsed': return this._sidebarCollapsed;
            case 'filters': return this._filters;
            case 'selectedRows': return Array.from(this._selectedRows);
            default: return null;
        }
    }
    
    // ============================================================================
    // PERSISTENCE
    // ============================================================================
    
    /**
     * Load persisted state from localStorage
     */
    _loadPersistedState() {
        // Load tab
        const savedTab = localStorage.getItem('deanship-active-tab');
        if (savedTab) {
            this._currentTab = savedTab;
        }
        
        // Load sidebar state
        const sidebarCollapsed = localStorage.getItem('deanship_sidebar_collapsed');
        if (sidebarCollapsed !== null) {
            this._sidebarCollapsed = sidebarCollapsed === 'true';
        }
        
        // Load selected academic year
        const savedYearId = localStorage.getItem('deanship_selected_academic_year');
        if (savedYearId) {
            this._selectedAcademicYearId = parseInt(savedYearId);
        }
        
        // Load selected semester
        const savedSemesterId = localStorage.getItem('deanship_selected_semester');
        if (savedSemesterId) {
            this._selectedSemesterId = parseInt(savedSemesterId);
        }
    }
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Check if context (academic year and semester) is set
     * @returns {boolean}
     */
    hasContext() {
        return this._selectedAcademicYearId !== null && this._selectedSemesterId !== null;
    }
    
    /**
     * Get current context
     * @returns {Object}
     */
    getContext() {
        return {
            academicYearId: this._selectedAcademicYearId,
            academicYear: this._selectedAcademicYear,
            semesterId: this._selectedSemesterId,
            semester: this.getSelectedSemester()
        };
    }
    
    /**
     * Reset all state
     */
    reset() {
        this._academicYears = [];
        this._professors = [];
        this._courses = [];
        this._departments = [];
        this._semesters = [];
        this._assignments = [];
        this._selectedRows.clear();
        this._filters = {};
        
        this.notify('reset');
    }
}

// Create singleton instance
const dashboardState = new DashboardState();

// Export for ES6 modules
export { dashboardState, DashboardState };

// Also make available globally for non-module scripts
if (typeof window !== 'undefined') {
    window.dashboardState = dashboardState;
}
