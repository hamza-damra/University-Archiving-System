/**
 * Admin Dashboard Page Module
 * Unified admin dashboard with integrated management for users, departments, courses, and reports
 */

import { AdminLayout } from './admin-common.js';
import { apiRequest, getUserInfo } from './api.js';
import { showToast, setButtonLoading } from './ui.js';

// Minimum loading time for smooth UX
const MIN_LOADING_TIME = 800;

/**
 * Utility function to ensure minimum loading time
 */
async function withMinLoadingTime(asyncFn, minTime = MIN_LOADING_TIME) {
    const startTime = Date.now();
    const result = await asyncFn();
    const elapsed = Date.now() - startTime;
    if (elapsed < minTime) {
        await new Promise(resolve => setTimeout(resolve, minTime - elapsed));
    }
    return result;
}

/**
 * Admin Dashboard Page Class
 */
class AdminDashboardPage {
    constructor() {
        this.layout = new AdminLayout();
        this.currentTab = 'dashboard';
        
        // Current logged-in admin user ID
        const userInfo = getUserInfo();
        this.currentUserId = userInfo ? userInfo.id : null;
        
        // Dashboard stats
        this.stats = {
            totalProfessors: 0,
            totalHods: 0,
            totalDepartments: 0,
            totalCourses: 0,
            totalSubmissions: 0,
            recentSubmissions: 0,
            pendingSubmissions: 0,
        };
        
        // Users tab state
        this.users = [];
        this.usersCurrentPage = 0;
        this.usersPageSize = 20;
        this.usersTotalPages = 0;
        this.usersTotalElements = 0;
        this.usersFilters = {
            search: '',
            role: '',
            isActive: '',
            departmentId: '',
        };
        this.editingUserId = null;
        this.deletingUserId = null;
        
        // Departments tab state
        this.departments = [];
        this.editingDepartmentId = null;
        this.deletingDepartmentId = null;
        
        // Courses tab state
        this.courses = [];
        this.coursesFilters = {
            search: '',
            departmentId: '',
            isActive: '',
        };
        this.editingCourseId = null;
        this.deletingCourseId = null;
        
        // Tabs that have been loaded
        this.loadedTabs = new Set(['dashboard']);
    }

    /**
     * Initialize the dashboard page
     */
    async initialize() {
        try {
            // Initialize shared layout
            await this.layout.initialize();
            
            // Set up tab navigation
            this.setupTabNavigation();
            
            // Set up mobile sidebar toggle
            this.setupMobileSidebar();
            
            // Set up card navigation
            this.setupCardNavigation();
            
            // Initialize modern dropdowns for professional styling
            this.initializeModernDropdowns();
            
            // Load dashboard statistics first
            await this.loadDashboardStats();
            
            // Register context change listener to reload stats
            this.layout.onContextChange(async () => {
                await this.loadDashboardStats();
            });
            
            // Restore saved tab from localStorage (if any)
            const savedTab = localStorage.getItem('adminCurrentTab');
            if (savedTab && ['dashboard', 'users', 'departments', 'courses', 'reports'].includes(savedTab)) {
                this.switchTab(savedTab);
            }
            
            console.log('[AdminDashboard] Initialized successfully');
        } catch (error) {
            console.error('[AdminDashboard] Initialization error:', error);
            this.handleError('Failed to initialize dashboard. Please refresh the page.', error);
        }
    }

    /**
     * Set up tab navigation in sidebar
     */
    setupTabNavigation() {
        const navTabs = document.querySelectorAll('.nav-tab');
        navTabs.forEach(tab => {
            tab.addEventListener('click', () => {
                const tabName = tab.dataset.tab;
                if (tabName) {
                    this.switchTab(tabName);
                }
            });
        });
    }

    /**
     * Set up mobile sidebar toggle
     */
    setupMobileSidebar() {
        const sidebarToggle = document.getElementById('sidebarToggle');
        const sidebar = document.getElementById('sidebar');
        const sidebarOverlay = document.getElementById('sidebarOverlay');

        if (sidebarToggle && sidebar) {
            sidebarToggle.addEventListener('click', () => {
                sidebar.classList.toggle('open');
                if (sidebarOverlay) {
                    sidebarOverlay.classList.toggle('open');
                }
            });
        }

        if (sidebarOverlay && sidebar) {
            sidebarOverlay.addEventListener('click', () => {
                sidebar.classList.remove('open');
                sidebarOverlay.classList.remove('open');
            });
        }
    }

    /**
     * Set up card navigation (clicking cards to switch tabs)
     */
    setupCardNavigation() {
        const cards = document.querySelectorAll('[data-navigate]');
        cards.forEach(card => {
            card.addEventListener('click', () => {
                const tabName = card.dataset.navigate;
                if (tabName) {
                    this.switchTab(tabName);
                }
            });
        });
    }

    /**
     * Initialize modern dropdowns for professional UI
     */
    initializeModernDropdowns() {
        // Delay initialization slightly to ensure DOM is ready
        setTimeout(() => {
            if (typeof window.initModernDropdowns === 'function') {
                // Initialize all dropdowns with the modern-dropdown class
                window.initModernDropdowns('#userRoleFilter, #userStatusFilter, #userDepartmentFilter, #courseDepartmentFilter, #userRole, #userDepartment, #courseDepartment');
                console.log('[AdminDashboard] Modern dropdowns initialized');
            } else {
                console.warn('[AdminDashboard] Modern dropdown library not loaded');
            }
        }, 150);
    }

    /**
     * Refresh a specific modern dropdown after options change
     */
    refreshDropdown(selectElement) {
        if (selectElement && selectElement._modernDropdown) {
            selectElement._modernDropdown.refresh();
        } else if (typeof window.refreshModernDropdown === 'function') {
            window.refreshModernDropdown(selectElement);
        }
    }

    /**
     * Switch to a different tab
     */
    switchTab(tabName) {
        // Update nav tabs
        const navTabs = document.querySelectorAll('.nav-tab');
        navTabs.forEach(tab => {
            if (tab.dataset.tab === tabName) {
                tab.classList.add('active');
            } else {
                tab.classList.remove('active');
            }
        });

        // Update tab content
        const tabContents = document.querySelectorAll('.tab-content');
        tabContents.forEach(content => {
            if (content.id === `${tabName}-tab`) {
                content.classList.remove('hidden');
            } else {
                content.classList.add('hidden');
            }
        });

        // Update page title
        const pageTitle = document.getElementById('pageTitle');
        if (pageTitle) {
            const titles = {
                'dashboard': 'Dashboard',
                'users': 'User Management',
                'departments': 'Departments',
                'courses': 'Courses',
                'reports': 'Reports'
            };
            pageTitle.textContent = titles[tabName] || 'Dashboard';
        }

        // Close mobile sidebar if open
        const sidebar = document.getElementById('sidebar');
        const sidebarOverlay = document.getElementById('sidebarOverlay');
        if (sidebar) sidebar.classList.remove('open');
        if (sidebarOverlay) sidebarOverlay.classList.remove('open');

        // Load tab-specific data if not already loaded
        if (!this.loadedTabs.has(tabName)) {
            this.loadTabData(tabName);
        }

        this.currentTab = tabName;
        
        // Save current tab to localStorage for persistence on refresh
        localStorage.setItem('adminCurrentTab', tabName);
    }

    /**
     * Load data for a specific tab
     */
    async loadTabData(tabName) {
        switch (tabName) {
            case 'users':
                await this.initializeUsersTab();
                break;
            case 'departments':
                await this.initializeDepartmentsTab();
                break;
            case 'courses':
                await this.initializeCoursesTab();
                break;
            case 'reports':
                await this.initializeReportsTab();
                break;
        }
        this.loadedTabs.add(tabName);
    }

    // ========================================
    // DASHBOARD TAB
    // ========================================

    /**
     * Load dashboard statistics from API
     */
    async loadDashboardStats() {
        try {
            this.showStatsLoading(true);
            
            await withMinLoadingTime(async () => {
                const context = this.layout.getSelectedContext();
                
                const params = new URLSearchParams();
                if (context.academicYearId) {
                    params.append('academicYearId', context.academicYearId);
                }
                if (context.semesterId) {
                    params.append('semesterId', context.semesterId);
                }
                
                const queryString = params.toString();
                const url = `/admin/dashboard/statistics${queryString ? '?' + queryString : ''}`;
                
                const response = await apiRequest(url, { method: 'GET' });
                
                if (response) {
                    this.stats = {
                        totalProfessors: response.totalProfessors || 0,
                        totalHods: response.totalHods || 0,
                        totalDepartments: response.totalDepartments || 0,
                        totalCourses: response.totalCourses || 0,
                        totalSubmissions: response.totalSubmissions || 0,
                        recentSubmissions: response.recentSubmissions || 0,
                        pendingSubmissions: response.pendingSubmissions || 0,
                    };
                }
            });
            
            this.updateStatDisplays();
            this.showStatsLoading(false);
            
        } catch (error) {
            console.error('[AdminDashboard] Failed to load stats:', error);
            this.showStatsLoading(false);
        }
    }

    /**
     * Update stat card displays
     */
    updateStatDisplays() {
        // Dashboard tab stats
        const statElements = {
            'statProfessors': this.stats.totalProfessors,
            'statHods': this.stats.totalHods,
            'statDepartments': this.stats.totalDepartments,
            'statCourses': this.stats.totalCourses,
            'statSubmissions': this.stats.totalSubmissions,
            'statRecentSubmissions': this.stats.recentSubmissions,
        };
        
        Object.entries(statElements).forEach(([id, value]) => {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = value.toLocaleString();
            }
        });

        // Dashboard quick access cards
        const cardElements = {
            'cardUsers': this.stats.totalProfessors + this.stats.totalHods,
            'cardDepartments': this.stats.totalDepartments,
            'cardCourses': this.stats.totalCourses,
            'cardSubmissions': this.stats.totalSubmissions,
        };

        Object.entries(cardElements).forEach(([id, value]) => {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = value.toLocaleString();
            }
        });

        // Reports tab stats
        const reportElements = {
            'reportTotalSubmissions': this.stats.totalSubmissions,
            'reportRecentSubmissions': this.stats.recentSubmissions,
            'reportPendingSubmissions': this.stats.pendingSubmissions,
            'reportProfessors': this.stats.totalProfessors,
            'reportHods': this.stats.totalHods,
            'reportDepartments': this.stats.totalDepartments,
        };

        Object.entries(reportElements).forEach(([id, value]) => {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = value.toLocaleString();
            }
        });
    }

    /**
     * Show/hide loading state on stats
     */
    showStatsLoading(show) {
        const statElements = document.querySelectorAll('[id^="stat"], [id^="card"], [id^="report"]');
        statElements.forEach(el => {
            if (show) {
                el.style.opacity = '0.5';
            } else {
                el.style.opacity = '1';
            }
        });
    }

    // ========================================
    // USERS TAB
    // ========================================

    /**
     * Initialize users tab
     */
    async initializeUsersTab() {
        await this.loadDepartmentsForFilters();
        await this.loadUsers();
        this.setupUsersEventListeners();
        
        // Re-initialize modern dropdowns for users tab after content is loaded
        setTimeout(() => {
            if (typeof window.initModernDropdowns === 'function') {
                window.initModernDropdowns('#userRoleFilter, #userStatusFilter, #userDepartmentFilter');
            }
        }, 100);
    }

    /**
     * Load departments for users filters and form
     */
    async loadDepartmentsForFilters() {
        try {
            const response = await apiRequest('/admin/departments', { method: 'GET' });
            this.departments = response || [];
            console.log('[AdminDashboard] Loaded departments:', this.departments);
            this.renderUserDepartmentOptions();
        } catch (error) {
            console.error('[AdminDashboard] Failed to load departments:', error);
        }
    }

    /**
     * Render department options in users filters and form
     */
    renderUserDepartmentOptions() {
        const filterSelect = document.getElementById('userDepartmentFilter');
        const formSelect = document.getElementById('userDepartment');
        
        const filterOptions = this.departments.map(dept => 
            `<option value="${dept.id}">${this.escapeHtml(dept.name)}</option>`
        ).join('');
        
        // For the user form, include the shortcut as a data attribute for HOD email generation
        const formOptions = this.departments.map(dept => 
            `<option value="${dept.id}" data-shortcut="${this.escapeHtml(dept.shortcut || '')}">${this.escapeHtml(dept.name)}</option>`
        ).join('');
        
        if (filterSelect) {
            filterSelect.innerHTML = '<option value="">All Departments</option>' + filterOptions;
            this.refreshDropdown(filterSelect);
        }
        
        if (formSelect) {
            formSelect.innerHTML = '<option value="">No Department</option>' + formOptions;
            this.refreshDropdown(formSelect);
        }
    }

    /**
     * Load users from API
     */
    async loadUsers() {
        try {
            this.showUsersTableLoading(true);
            
            await withMinLoadingTime(async () => {
                const params = new URLSearchParams();
                params.append('page', this.usersCurrentPage);
                params.append('size', this.usersPageSize);
                
                if (this.usersFilters.role) {
                    params.append('role', this.usersFilters.role);
                }
                if (this.usersFilters.departmentId) {
                    params.append('departmentId', this.usersFilters.departmentId);
                }
                if (this.usersFilters.isActive !== '') {
                    params.append('isActive', this.usersFilters.isActive);
                }
                
                const response = await apiRequest(`/admin/users?${params.toString()}`, { method: 'GET' });
                console.log('[AdminDashboard] Users API response:', response);
                
                // apiRequest already extracts data from ApiResponse wrapper
                // So response is directly the Page object
                if (response && response.content) {
                    this.users = response.content || [];
                    this.usersTotalPages = response.totalPages || 0;
                    this.usersTotalElements = response.totalElements || 0;
                } else if (response && Array.isArray(response)) {
                    // If response is an array directly
                    this.users = response;
                    this.usersTotalPages = 1;
                    this.usersTotalElements = response.length;
                } else {
                    this.users = [];
                    this.usersTotalPages = 0;
                    this.usersTotalElements = 0;
                }
                
                // Apply client-side search filter
                if (this.usersFilters.search) {
                    const searchLower = this.usersFilters.search.toLowerCase();
                    this.users = this.users.filter(user => 
                        (user.firstName && user.firstName.toLowerCase().includes(searchLower)) ||
                        (user.lastName && user.lastName.toLowerCase().includes(searchLower)) ||
                        (user.email && user.email.toLowerCase().includes(searchLower))
                    );
                }
            });
            
            this.renderUsersTable();
            this.renderUsersPagination();
            this.showUsersTableLoading(false);
            
        } catch (error) {
            console.error('[AdminDashboard] Failed to load users:', error);
            this.showUsersTableLoading(false);
            showToast('Failed to load users', 'error');
        }
    }

    /**
     * Render users table
     */
    renderUsersTable() {
        const tbody = document.getElementById('usersTableBody');
        if (!tbody) return;
        
        if (this.users.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center py-8">
                        <div class="text-4xl mb-2">👥</div>
                        <div class="font-medium text-gray-900 dark:text-white">No users found</div>
                        <div class="text-sm text-gray-500 dark:text-gray-400">Try adjusting your filters or create a new user</div>
                    </td>
                </tr>
            `;
            return;
        }
        
        tbody.innerHTML = this.users.map(user => this.createUserRowHTML(user)).join('');
        
        // Add event listeners to action buttons
        this.users.forEach(user => {
            const editBtn = document.getElementById(`editUser-${user.id}`);
            const deleteBtn = document.getElementById(`deleteUser-${user.id}`);
            
            if (editBtn) {
                editBtn.addEventListener('click', () => this.openEditUserModal(user));
            }
            if (deleteBtn) {
                deleteBtn.addEventListener('click', () => this.openDeleteUserModal(user.id));
            }
        });
    }

    /**
     * Create HTML for a user row
     */
    createUserRowHTML(user) {
        const roleBadges = {
            'ROLE_ADMIN': 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200',
            'ROLE_DEANSHIP': 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200',
            'ROLE_HOD': 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
            'ROLE_PROFESSOR': 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200',
        };
        const roleLabels = {
            'ROLE_ADMIN': 'Admin',
            'ROLE_DEANSHIP': 'Dean',
            'ROLE_HOD': 'HOD',
            'ROLE_PROFESSOR': 'Professor',
        };
        
        const roleBadgeClass = roleBadges[user.role] || 'bg-gray-100 text-gray-800';
        const roleLabel = roleLabels[user.role] || user.role;
        const statusClass = user.isActive 
            ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200' 
            : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200';
        const statusLabel = user.isActive ? 'Active' : 'Inactive';
        
        const isSelf = user.id === this.currentUserId;
        const editTitle = isSelf ? 'Change Password' : 'Edit';
        const deleteDisabledClass = isSelf ? 'opacity-30 cursor-not-allowed' : '';
        const deleteTitle = isSelf ? 'Cannot delete your own account' : 'Delete';
        
        return `
            <tr class="border-b border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800">
                <td class="px-4 py-3 text-sm text-gray-900 dark:text-white">
                    ${this.escapeHtml(user.firstName || '')} ${this.escapeHtml(user.lastName || '')}
                    ${isSelf ? '<span class="ml-2 px-1.5 py-0.5 text-xs bg-purple-100 text-purple-700 dark:bg-purple-900 dark:text-purple-300 rounded">You</span>' : ''}
                </td>
                <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-300">${this.escapeHtml(user.email || '')}</td>
                <td class="px-4 py-3"><span class="px-2 py-1 text-xs font-medium rounded-full ${roleBadgeClass}">${roleLabel}</span></td>
                <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-300">${this.escapeHtml(user.departmentName || '-')}</td>
                <td class="px-4 py-3"><span class="px-2 py-1 text-xs font-medium rounded-full ${statusClass}">${statusLabel}</span></td>
                <td class="px-4 py-3">
                    <div class="flex items-center gap-2">
                        <button id="editUser-${user.id}" class="p-1.5 text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/30 rounded" title="${editTitle}">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                        </button>
                        <button id="deleteUser-${user.id}" class="p-1.5 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/30 rounded ${deleteDisabledClass}" title="${deleteTitle}" ${isSelf ? 'disabled' : ''}>
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }

    /**
     * Render users pagination
     */
    renderUsersPagination() {
        const pagination = document.getElementById('usersPagination');
        if (!pagination) return;
        
        if (this.usersTotalPages <= 1) {
            pagination.innerHTML = '';
            return;
        }
        
        pagination.innerHTML = `
            <div class="flex items-center justify-between">
                <span class="text-sm text-gray-600 dark:text-gray-400">
                    Page ${this.usersCurrentPage + 1} of ${this.usersTotalPages} (${this.usersTotalElements} users)
                </span>
                <div class="flex gap-2">
                    <button id="usersPrevPage" class="px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed" ${this.usersCurrentPage === 0 ? 'disabled' : ''}>
                        Previous
                    </button>
                    <button id="usersNextPage" class="px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed" ${this.usersCurrentPage >= this.usersTotalPages - 1 ? 'disabled' : ''}>
                        Next
                    </button>
                </div>
            </div>
        `;
        
        const prevBtn = document.getElementById('usersPrevPage');
        const nextBtn = document.getElementById('usersNextPage');
        
        if (prevBtn) {
            prevBtn.addEventListener('click', () => {
                if (this.usersCurrentPage > 0) {
                    this.usersCurrentPage--;
                    this.loadUsers();
                }
            });
        }
        
        if (nextBtn) {
            nextBtn.addEventListener('click', () => {
                if (this.usersCurrentPage < this.usersTotalPages - 1) {
                    this.usersCurrentPage++;
                    this.loadUsers();
                }
            });
        }
    }

    /**
     * Set up users tab event listeners
     */
    setupUsersEventListeners() {
        // Create user button
        const createBtn = document.getElementById('addUserBtn');
        if (createBtn) {
            createBtn.addEventListener('click', () => this.openCreateUserModal());
        }
        
        // Search input
        const searchInput = document.getElementById('userSearchInput');
        if (searchInput) {
            let debounceTimer;
            searchInput.addEventListener('input', (e) => {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(() => {
                    this.usersFilters.search = e.target.value;
                    this.usersCurrentPage = 0;
                    this.loadUsers();
                }, 300);
            });
        }
        
        // Role filter
        const roleFilter = document.getElementById('userRoleFilter');
        if (roleFilter) {
            roleFilter.addEventListener('change', (e) => {
                this.usersFilters.role = e.target.value;
                this.usersCurrentPage = 0;
                this.loadUsers();
            });
        }
        
        // Status filter
        const statusFilter = document.getElementById('userStatusFilter');
        if (statusFilter) {
            statusFilter.addEventListener('change', (e) => {
                this.usersFilters.isActive = e.target.value;
                this.usersCurrentPage = 0;
                this.loadUsers();
            });
        }
        
        // Department filter
        const departmentFilter = document.getElementById('userDepartmentFilter');
        if (departmentFilter) {
            departmentFilter.addEventListener('change', (e) => {
                this.usersFilters.departmentId = e.target.value;
                this.usersCurrentPage = 0;
                this.loadUsers();
            });
        }
        
        // Modal close buttons
        const userModalClose = document.getElementById('userModalClose');
        const userModalCancel = document.getElementById('userModalCancel');
        if (userModalClose) userModalClose.addEventListener('click', () => this.closeUserModal());
        if (userModalCancel) userModalCancel.addEventListener('click', () => this.closeUserModal());
        
        // Role change handler for email domain and field visibility
        const userRoleSelect = document.getElementById('userRole');
        if (userRoleSelect) {
            // Handle change event (native select)
            userRoleSelect.addEventListener('change', (e) => {
                console.log('[AdminDashboard] Role change event fired. New value:', e.target.value);
                this.handleRoleChangeForEmail(e.target.value);
            });
            
            // Also use input event as some dropdowns trigger that
            userRoleSelect.addEventListener('input', (e) => {
                console.log('[AdminDashboard] Role input event fired. New value:', e.target.value);
                this.handleRoleChangeForEmail(e.target.value);
            });
            
            // Use event delegation on document to catch modern dropdown clicks
            document.addEventListener('click', (e) => {
                const option = e.target.closest('.modern-dropdown-option');
                if (option) {
                    // Find which dropdown wrapper this option belongs to
                    const wrapper = option.closest('.modern-dropdown-wrapper');
                    if (wrapper) {
                        // The original select is the previous sibling of the wrapper
                        const select = wrapper.previousElementSibling;
                        // Check if this is the userRole select
                        if (select && select.id === 'userRole') {
                            // Wait a tick for the value to update
                            setTimeout(() => {
                                const role = userRoleSelect.value;
                                console.log('[AdminDashboard] Modern dropdown option clicked for userRole. Role value:', role);
                                if (role) {
                                    this.handleRoleChangeForEmail(role);
                                }
                            }, 50);
                        }
                    }
                }
            });
        }
        
        // Email prefix input handler
        const emailPrefixInput = document.getElementById('userEmailPrefix');
        if (emailPrefixInput) {
            emailPrefixInput.addEventListener('input', () => this.updateFullEmail());
        }
        
        // Department change handler for HOD email
        const userDepartmentSelect = document.getElementById('userDepartment');
        if (userDepartmentSelect) {
            userDepartmentSelect.addEventListener('change', (e) => {
                console.log('[AdminDashboard] Department change event fired. New value:', e.target.value);
                this.updateEmailForHod();
            });
            
            // Also use input event as some dropdowns trigger that
            userDepartmentSelect.addEventListener('input', (e) => {
                console.log('[AdminDashboard] Department input event fired. New value:', e.target.value);
                this.updateEmailForHod();
            });
        }
        
        // Listen for clicks on modern dropdown options for department (event delegation)
        document.addEventListener('click', (e) => {
            const option = e.target.closest('.modern-dropdown-option');
            if (option) {
                const wrapper = option.closest('.modern-dropdown-wrapper');
                if (wrapper) {
                    const select = wrapper.previousElementSibling;
                    // Check if this is the userDepartment select
                    if (select && select.id === 'userDepartment') {
                        // Wait a tick for the value to update
                        setTimeout(() => {
                            console.log('[AdminDashboard] Department dropdown option clicked, value:', select.value);
                            this.updateEmailForHod();
                        }, 50);
                    }
                }
            }
        });
        
        // Form submission
        const userForm = document.getElementById('userForm');
        if (userForm) {
            userForm.addEventListener('submit', (e) => this.handleUserFormSubmit(e));
        }
    }
    
    /**
     * Handle role change to update department field visibility and email domain suffix
     */
    handleRoleChangeForEmail(role) {
        // First, update department field visibility based on role
        this.updateDepartmentFieldForRole(role);
        
        const emailDomainSpan = document.getElementById('userEmailDomain');
        const emailPrefixInput = document.getElementById('userEmailPrefix');
        const emailFormatHint = document.getElementById('emailFormatHint');
        const emailHintText = document.getElementById('emailHintText');
        const emailInputWrapper = document.getElementById('emailInputWrapper');
        const emailAutoDisplay = document.getElementById('emailAutoDisplay');
        
        if (!emailDomainSpan || !emailPrefixInput) return;
        
        // Get pending display element
        const emailPendingDisplay = document.getElementById('emailPendingDisplay');
        
        // Reset hint styling
        if (emailFormatHint) {
            emailFormatHint.classList.remove('text-red-500', 'dark:text-red-400', 'text-amber-500', 'dark:text-amber-400', 'text-green-500', 'dark:text-green-400');
            emailFormatHint.classList.add('text-gray-500', 'dark:text-gray-400');
        }
        
        // Define email domains for each role
        const emailConfig = {
            'ROLE_ADMIN': { domain: '@admin.alquds.edu', hint: 'Format: username@admin.alquds.edu', placeholder: 'username', prefix: '' },
            'ROLE_DEANSHIP': { domain: '@dean.alquds.edu', hint: 'Format: username@dean.alquds.edu', placeholder: 'username', prefix: '' },
            'ROLE_HOD': { domain: '@hod.alquds.edu', hint: 'Email auto-generated based on department', placeholder: 'department_shortcut', prefix: 'hod.' },
            'ROLE_PROFESSOR': { domain: '@staff.alquds.edu', hint: 'Format: username@staff.alquds.edu', placeholder: 'username', prefix: '' }
        };
        
        const config = emailConfig[role] || { domain: '@alquds.edu', hint: 'Select a role to see the required email format', placeholder: 'username', prefix: '' };
        
        // Store the current config for email generation
        this.currentEmailConfig = config;
        
        // For HOD, hide the input and show auto-generated display
        if (role === 'ROLE_HOD') {
            // Hide input wrapper
            if (emailInputWrapper) emailInputWrapper.classList.add('hidden');
            
            // Check if departments are available
            if (!this.departments || this.departments.length === 0) {
                if (emailAutoDisplay) emailAutoDisplay.classList.add('hidden');
                if (emailPendingDisplay) emailPendingDisplay.classList.add('hidden');
                if (emailInputWrapper) emailInputWrapper.classList.remove('hidden');
                emailPrefixInput.style.display = 'none';
                emailDomainSpan.textContent = 'hod.<shortcut>@hod.alquds.edu';
                if (emailHintText) emailHintText.textContent = '⚠️ No departments available. Please create a department first.';
                if (emailFormatHint) {
                    emailFormatHint.classList.remove('text-gray-500', 'dark:text-gray-400');
                    emailFormatHint.classList.add('text-red-500', 'dark:text-red-400');
                }
            } else {
                this.updateEmailForHod();
            }
        } else {
            // For other roles, show the input wrapper with prefix input and domain
            if (emailInputWrapper) emailInputWrapper.classList.remove('hidden');
            if (emailAutoDisplay) emailAutoDisplay.classList.add('hidden');
            if (emailPendingDisplay) emailPendingDisplay.classList.add('hidden');
            
            emailPrefixInput.style.display = 'block';
            emailPrefixInput.placeholder = config.placeholder;
            emailDomainSpan.textContent = config.domain;
            if (emailHintText) emailHintText.textContent = config.hint;
            this.updateFullEmail();
        }
    }
    
    /**
     * Update email for HOD based on department selection
     */
    updateEmailForHod() {
        const roleSelect = document.getElementById('userRole');
        const role = roleSelect?.value;
        
        console.log('[AdminDashboard] updateEmailForHod called - Role:', role, 'selectedIndex:', roleSelect?.selectedIndex);
        
        if (role !== 'ROLE_HOD') {
            console.log('[AdminDashboard] updateEmailForHod - Not HOD role, skipping');
            return;
        }
        
        const departmentSelect = document.getElementById('userDepartment');
        const emailDomainSpan = document.getElementById('userEmailDomain');
        const emailPrefixInput = document.getElementById('userEmailPrefix');
        const emailFormatHint = document.getElementById('emailFormatHint');
        const emailHintText = document.getElementById('emailHintText');
        const emailHidden = document.getElementById('userEmail');
        const emailInputWrapper = document.getElementById('emailInputWrapper');
        const emailAutoDisplay = document.getElementById('emailAutoDisplay');
        const emailPendingDisplay = document.getElementById('emailPendingDisplay');
        const emailAutoValue = document.getElementById('emailAutoValue');
        
        if (!departmentSelect || !emailDomainSpan) return;
        
        const selectedDepartmentId = departmentSelect.value;
        const selectedOption = departmentSelect.options[departmentSelect.selectedIndex];
        
        console.log('[AdminDashboard] HOD email update - Selected department ID:', selectedDepartmentId);
        console.log('[AdminDashboard] Selected option:', selectedOption?.text, 'data-shortcut:', selectedOption?.dataset?.shortcut);
        console.log('[AdminDashboard] Available departments:', this.departments);
        
        // Find department shortcut - try multiple sources
        let departmentShortcut = '';
        
        // First try from the cached departments array
        if (selectedDepartmentId && this.departments && this.departments.length > 0) {
            const selectedDept = this.departments.find(d => String(d.id) === String(selectedDepartmentId));
            console.log('[AdminDashboard] Found department from cache:', selectedDept);
            if (selectedDept && selectedDept.shortcut) {
                departmentShortcut = selectedDept.shortcut.toLowerCase();
            }
        }
        
        // Fallback: try from the data-shortcut attribute on the option
        if (!departmentShortcut && selectedOption && selectedOption.dataset && selectedOption.dataset.shortcut) {
            departmentShortcut = selectedOption.dataset.shortcut.toLowerCase();
            console.log('[AdminDashboard] Got shortcut from data attribute:', departmentShortcut);
        }
        
        console.log('[AdminDashboard] Final department shortcut:', departmentShortcut);
        
        // Reset hint styling
        if (emailFormatHint) {
            emailFormatHint.classList.remove('text-red-500', 'dark:text-red-400', 'text-green-500', 'dark:text-green-400');
            emailFormatHint.classList.add('text-gray-500', 'dark:text-gray-400');
        }
        
        // Check if there are no departments available at all
        if (!this.departments || this.departments.length === 0) {
            if (emailAutoDisplay) emailAutoDisplay.classList.add('hidden');
            if (emailPendingDisplay) emailPendingDisplay.classList.add('hidden');
            if (emailInputWrapper) emailInputWrapper.classList.remove('hidden');
            emailDomainSpan.textContent = 'hod.<shortcut>@hod.alquds.edu';
            emailPrefixInput.style.display = 'none';
            emailPrefixInput.value = '';
            if (emailHidden) emailHidden.value = '';
            if (emailHintText) emailHintText.textContent = '⚠️ No departments available. Please create a department first.';
            if (emailFormatHint) {
                emailFormatHint.classList.remove('text-gray-500', 'dark:text-gray-400');
                emailFormatHint.classList.add('text-red-500', 'dark:text-red-400');
            }
            return;
        }
        
        if (departmentShortcut && selectedDepartmentId) {
            // We have a department selected, show the auto-generated email in the nice display
            const fullEmail = `hod.${departmentShortcut}@hod.alquds.edu`;
            
            // Show the auto display with the email, hide others
            if (emailInputWrapper) emailInputWrapper.classList.add('hidden');
            if (emailPendingDisplay) emailPendingDisplay.classList.add('hidden');
            if (emailAutoDisplay) emailAutoDisplay.classList.remove('hidden');
            if (emailAutoValue) emailAutoValue.textContent = fullEmail;
            
            emailPrefixInput.style.display = 'none';
            emailPrefixInput.value = '';
            
            // Set the full email
            if (emailHidden) emailHidden.value = fullEmail;
            
            // Update hint with success styling
            if (emailHintText) emailHintText.textContent = '✓ Email auto-generated from department';
            if (emailFormatHint) {
                emailFormatHint.classList.remove('text-gray-500', 'dark:text-gray-400');
                emailFormatHint.classList.add('text-green-500', 'dark:text-green-400');
            }
            
            console.log('[AdminDashboard] Set HOD email to:', fullEmail);
        } else {
            // No department selected, show the pending display
            if (emailAutoDisplay) emailAutoDisplay.classList.add('hidden');
            if (emailInputWrapper) emailInputWrapper.classList.add('hidden');
            if (emailPendingDisplay) emailPendingDisplay.classList.remove('hidden');
            
            emailPrefixInput.style.display = 'none';
            emailPrefixInput.value = '';
            if (emailHidden) emailHidden.value = '';
            if (emailHintText) emailHintText.textContent = '👆 Select a department above to generate email';
            if (emailFormatHint) {
                emailFormatHint.classList.remove('text-gray-500', 'dark:text-gray-400', 'text-green-500', 'dark:text-green-400');
                emailFormatHint.classList.add('text-amber-500', 'dark:text-amber-400');
            }
        }
    }
    
    /**
     * Update the full email based on prefix and domain
     */
    updateFullEmail() {
        const emailPrefixInput = document.getElementById('userEmailPrefix');
        const emailHidden = document.getElementById('userEmail');
        const role = document.getElementById('userRole')?.value;
        
        if (!emailPrefixInput || !emailHidden) return;
        
        const prefix = emailPrefixInput.value.trim().toLowerCase();
        
        if (!prefix || !role) {
            emailHidden.value = '';
            return;
        }
        
        const emailConfig = {
            'ROLE_ADMIN': '@admin.alquds.edu',
            'ROLE_DEANSHIP': '@dean.alquds.edu',
            'ROLE_HOD': '@hod.alquds.edu',
            'ROLE_PROFESSOR': '@staff.alquds.edu'
        };
        
        const domain = emailConfig[role] || '@alquds.edu';
        
        // For HOD, the email is handled separately by updateEmailForHod
        if (role === 'ROLE_HOD') return;
        
        emailHidden.value = prefix + domain;
    }
    
    /**
     * Update department field and name fields based on selected role
     * - ADMIN and DEANSHIP (Dean): Disable department field (not applicable)
     * - HOD: Disable first/last name fields (auto-generated), require department
     * - PROFESSOR: Show department field as required, enable name fields
     */
    updateDepartmentFieldForRole(role) {
        console.log('[AdminDashboard] Updating fields for role:', role);
        
        const departmentContainer = document.getElementById('departmentFieldContainer');
        const departmentSelect = document.getElementById('userDepartment');
        const departmentRequired = document.getElementById('departmentRequired');
        const departmentHint = document.getElementById('departmentHint');
        const firstNameField = document.getElementById('userFirstName');
        const lastNameField = document.getElementById('userLastName');
        
        if (!departmentContainer) return;
        
        // Define role configurations
        const roleConfig = {
            'ROLE_ADMIN': { 
                departmentEnabled: false, 
                departmentRequired: false, 
                hint: 'Admin has system-wide access - no department needed',
                nameFieldsEnabled: true
            },
            'ROLE_DEANSHIP': { 
                departmentEnabled: false, 
                departmentRequired: false, 
                hint: 'Dean has faculty-wide access - no department needed',
                nameFieldsEnabled: true  // Dean name fields enabled
            },
            'ROLE_HOD': { 
                departmentEnabled: true, 
                departmentRequired: true, 
                hint: 'Select the department this HOD will manage',
                nameFieldsEnabled: true  // HOD name fields enabled
            },
            'ROLE_PROFESSOR': { 
                departmentEnabled: true, 
                departmentRequired: true, 
                hint: 'Select the department this professor belongs to',
                nameFieldsEnabled: true
            }
        };
        
        const config = roleConfig[role] || { departmentEnabled: true, departmentRequired: false, hint: '', nameFieldsEnabled: true };
        
        console.log('[AdminDashboard] Config for role:', config);
        
        // Always show the department container, just enable/disable it
        departmentContainer.classList.remove('hidden');
        departmentContainer.style.display = '';
        
        // Handle department dropdown (use modern dropdown helper if available)
        if (departmentSelect) {
            console.log('[AdminDashboard] About to disable department dropdown. departmentEnabled:', config.departmentEnabled);
            console.log('[AdminDashboard] setModernDropdownDisabled available:', typeof window.setModernDropdownDisabled === 'function');
            
            if (typeof window.setModernDropdownDisabled === 'function') {
                // Use the modern dropdown API
                window.setModernDropdownDisabled(departmentSelect, !config.departmentEnabled);
            }
            
            // Also set the native select properties
            departmentSelect.disabled = !config.departmentEnabled;
            // Don't set required on hidden select - we validate manually in form submit
            // departmentSelect.required = config.departmentRequired;
            // Store the required state as a data attribute for manual validation
            departmentSelect.dataset.required = config.departmentRequired ? 'true' : 'false';
            
            if (!config.departmentEnabled) {
                departmentSelect.value = ''; // Clear selection
            }
        }
        
        // Show/hide required asterisk based on whether department is required
        if (departmentRequired) {
            if (config.departmentRequired) {
                departmentRequired.classList.remove('hidden');
            } else {
                departmentRequired.classList.add('hidden');
            }
        }
        
        // Show hint
        if (departmentHint && config.hint) {
            departmentHint.textContent = config.hint;
            departmentHint.classList.remove('hidden');
        }
        
        // Get hint elements for name fields
        const firstNameHint = document.getElementById('firstNameHint');
        const lastNameHint = document.getElementById('lastNameHint');
        
        // Handle first/last name fields for HOD/Dean role
        if (firstNameField) {
            firstNameField.disabled = !config.nameFieldsEnabled;
            firstNameField.readOnly = !config.nameFieldsEnabled;
            if (!config.nameFieldsEnabled) {
                firstNameField.style.setProperty('pointer-events', 'none', 'important');
                firstNameField.style.setProperty('opacity', '0.5', 'important');
                // Use a distinct background color for disabled state
                // Dark mode: bg-gray-800 (#1f2937), Light mode: bg-gray-200 (#e5e7eb)
                const isDarkMode = document.documentElement.classList.contains('dark');
                firstNameField.style.setProperty('background-color', isDarkMode ? '#1f2937' : '#e5e7eb', 'important');
                firstNameField.style.setProperty('color', '#9ca3af', 'important'); // Dimmed text
                firstNameField.style.setProperty('cursor', 'not-allowed', 'important');
                
                // Show hint for HOD
                if (firstNameHint && role === 'ROLE_HOD') {
                    firstNameHint.textContent = 'Name is auto-generated from department (e.g., "HOD Computer Science")';
                    firstNameHint.classList.remove('hidden');
                } else if (firstNameHint) {
                    firstNameHint.classList.add('hidden');
                }
            } else {
                firstNameField.style.removeProperty('pointer-events');
                firstNameField.style.removeProperty('opacity');
                firstNameField.style.removeProperty('background-color');
                firstNameField.style.removeProperty('color');
                firstNameField.style.removeProperty('cursor');
                if (firstNameHint) firstNameHint.classList.add('hidden');
            }
        }
        
        if (lastNameField) {
            lastNameField.disabled = !config.nameFieldsEnabled;
            lastNameField.readOnly = !config.nameFieldsEnabled;
            if (!config.nameFieldsEnabled) {
                lastNameField.style.setProperty('pointer-events', 'none', 'important');
                lastNameField.style.setProperty('opacity', '0.5', 'important');
                const isDarkMode = document.documentElement.classList.contains('dark');
                lastNameField.style.setProperty('background-color', isDarkMode ? '#1f2937' : '#e5e7eb', 'important');
                lastNameField.style.setProperty('color', '#9ca3af', 'important'); // Dimmed text
                lastNameField.style.setProperty('cursor', 'not-allowed', 'important');
                
                // Show hint for HOD
                if (lastNameHint && role === 'ROLE_HOD') {
                    lastNameHint.textContent = 'Name is auto-generated from department';
                    lastNameHint.classList.remove('hidden');
                } else if (lastNameHint) {
                    lastNameHint.classList.add('hidden');
                }
            } else {
                lastNameField.style.removeProperty('pointer-events');
                lastNameField.style.removeProperty('opacity');
                lastNameField.style.removeProperty('background-color');
                lastNameField.style.removeProperty('color');
                lastNameField.style.removeProperty('cursor');
                if (lastNameHint) lastNameHint.classList.add('hidden');
            }
        }
        
        console.log('[AdminDashboard] Fields updated for role:', role, 'departmentEnabled:', config.departmentEnabled, 'nameFieldsEnabled:', config.nameFieldsEnabled);
    }
    
    /**
     * Reset department field and name fields to default state
     */
    resetDepartmentField() {
        const departmentContainer = document.getElementById('departmentFieldContainer');
        const departmentSelect = document.getElementById('userDepartment');
        const departmentRequired = document.getElementById('departmentRequired');
        const departmentHint = document.getElementById('departmentHint');
        const firstNameField = document.getElementById('userFirstName');
        const lastNameField = document.getElementById('userLastName');
        
        if (departmentContainer) {
            departmentContainer.classList.remove('hidden');
            departmentContainer.style.display = '';
        }
        if (departmentSelect) {
            departmentSelect.value = '';
            departmentSelect.removeAttribute('required'); // Remove required from hidden select
            departmentSelect.dataset.required = 'false';
            departmentSelect.disabled = false;
            
            // Use modern dropdown API if available
            if (typeof window.setModernDropdownDisabled === 'function') {
                window.setModernDropdownDisabled(departmentSelect, false);
            }
        }
        if (departmentRequired) {
            departmentRequired.classList.add('hidden');
        }
        if (departmentHint) {
            departmentHint.classList.add('hidden');
            departmentHint.textContent = '';
        }
        // Reset first/last name fields to enabled state
        if (firstNameField) {
            firstNameField.disabled = false;
            firstNameField.readOnly = false;
            firstNameField.style.removeProperty('pointer-events');
            firstNameField.style.removeProperty('opacity');
            firstNameField.style.removeProperty('background-color');
            firstNameField.style.removeProperty('color');
            firstNameField.style.removeProperty('cursor');
        }
        if (lastNameField) {
            lastNameField.disabled = false;
            lastNameField.readOnly = false;
            lastNameField.style.removeProperty('pointer-events');
            lastNameField.style.removeProperty('opacity');
            lastNameField.style.removeProperty('background-color');
            lastNameField.style.removeProperty('color');
            lastNameField.style.removeProperty('cursor');
        }
        
        // Hide name field hints
        const firstNameHint = document.getElementById('firstNameHint');
        const lastNameHint = document.getElementById('lastNameHint');
        if (firstNameHint) firstNameHint.classList.add('hidden');
        if (lastNameHint) lastNameHint.classList.add('hidden');
    }
    
    /**
     * Reset email fields to default state
     */
    resetEmailFields() {
        const emailPrefixInput = document.getElementById('userEmailPrefix');
        const emailDomainSpan = document.getElementById('userEmailDomain');
        const emailHidden = document.getElementById('userEmail');
        const emailFormatHint = document.getElementById('emailFormatHint');
        const emailHintText = document.getElementById('emailHintText');
        const emailInputWrapper = document.getElementById('emailInputWrapper');
        const emailAutoDisplay = document.getElementById('emailAutoDisplay');
        const emailPendingDisplay = document.getElementById('emailPendingDisplay');
        const emailAutoValue = document.getElementById('emailAutoValue');
        
        if (emailPrefixInput) {
            emailPrefixInput.value = '';
            emailPrefixInput.style.display = 'block';
            emailPrefixInput.placeholder = 'username';
        }
        if (emailDomainSpan) emailDomainSpan.textContent = '@alquds.edu';
        if (emailHidden) emailHidden.value = '';
        if (emailHintText) emailHintText.textContent = 'Select a role to see the required email format';
        if (emailFormatHint) {
            emailFormatHint.classList.remove('text-red-500', 'dark:text-red-400', 'text-green-500', 'dark:text-green-400', 'text-amber-500', 'dark:text-amber-400');
            emailFormatHint.classList.add('text-gray-500', 'dark:text-gray-400');
        }
        // Reset to show input wrapper, hide auto display and pending display
        if (emailInputWrapper) emailInputWrapper.classList.remove('hidden');
        if (emailAutoDisplay) emailAutoDisplay.classList.add('hidden');
        if (emailPendingDisplay) emailPendingDisplay.classList.add('hidden');
        if (emailAutoValue) emailAutoValue.textContent = '';
    }

    /**
     * Open create user modal
     */
    async openCreateUserModal() {
        this.editingUserId = null;
        
        // Reload departments to ensure we have the latest list
        await this.loadDepartmentsForFilters();
        
        const modal = document.getElementById('userModal');
        const modalTitle = document.getElementById('userModalTitle');
        const submitBtn = document.getElementById('userSubmitBtn');
        const form = document.getElementById('userForm');
        const passwordField = document.getElementById('userPassword');
        const passwordRequired = document.getElementById('passwordRequired');
        const passwordHelper = document.getElementById('passwordHelper');
        
        // Get form fields to reset disabled state
        const firstNameField = document.getElementById('userFirstName');
        const lastNameField = document.getElementById('userLastName');
        const emailField = document.getElementById('userEmail');
        const emailPrefixField = document.getElementById('userEmailPrefix');
        const roleField = document.getElementById('userRole');
        const departmentField = document.getElementById('userDepartment');
        const isActiveField = document.getElementById('userIsActive');
        
        if (modalTitle) modalTitle.textContent = 'Create User';
        if (submitBtn) submitBtn.textContent = 'Create User';
        if (form) form.reset();
        if (passwordField) passwordField.required = true;
        if (passwordRequired) passwordRequired.style.display = 'inline';
        if (passwordHelper) passwordHelper.textContent = 'At least 8 characters';
        
        // Reset email fields
        this.resetEmailFields();
        
        // Reset department field
        this.resetDepartmentField();
        
        // Enable all fields (in case they were disabled from self-edit)
        if (firstNameField) { firstNameField.disabled = false; firstNameField.classList.remove('opacity-50', 'cursor-not-allowed'); }
        if (lastNameField) { lastNameField.disabled = false; lastNameField.classList.remove('opacity-50', 'cursor-not-allowed'); }
        if (emailField) { emailField.disabled = false; emailField.classList.remove('opacity-50', 'cursor-not-allowed'); }
        if (emailPrefixField) { emailPrefixField.disabled = false; emailPrefixField.classList.remove('opacity-50', 'cursor-not-allowed'); }
        if (roleField) { roleField.disabled = false; roleField.classList.remove('opacity-50', 'cursor-not-allowed'); }
        if (departmentField) { departmentField.disabled = false; departmentField.classList.remove('opacity-50', 'cursor-not-allowed'); }
        if (isActiveField) { isActiveField.disabled = false; isActiveField.classList.remove('opacity-50', 'cursor-not-allowed'); }
        
        // Reset the role dropdown to empty value and refresh modern dropdown UI
        if (roleField) {
            roleField.value = '';
            roleField.selectedIndex = 0;
            this.refreshDropdown(roleField);
        }
        
        // Reset department dropdown
        if (departmentField) {
            departmentField.value = '';
            departmentField.selectedIndex = 0;
            this.refreshDropdown(departmentField);
        }
        
        // Set default active state
        const isActiveCheckbox = document.getElementById('userIsActive');
        if (isActiveCheckbox) isActiveCheckbox.checked = true;
        
        if (modal) {
            modal.classList.remove('hidden');
            modal.classList.add('active');
            
            // Initialize modern dropdowns in modal after it's visible (if not already initialized)
            setTimeout(() => {
                if (typeof window.initModernDropdowns === 'function') {
                    window.initModernDropdowns('#userRole, #userDepartment');
                }
                // Refresh dropdowns to show correct display text
                this.refreshDropdown(roleField);
                this.refreshDropdown(departmentField);
                // Re-apply reset after dropdowns are initialized to ensure proper state
                this.resetDepartmentField();
            }, 50);
        }
    }

    /**
     * Open edit user modal
     */
    openEditUserModal(user) {
        this.editingUserId = user.id;
        const isEditingSelf = user.id === this.currentUserId;
        
        const modal = document.getElementById('userModal');
        const modalTitle = document.getElementById('userModalTitle');
        const submitBtn = document.getElementById('userSubmitBtn');
        const passwordField = document.getElementById('userPassword');
        const passwordRequired = document.getElementById('passwordRequired');
        const passwordHelper = document.getElementById('passwordHelper');
        
        // Get form fields that should be restricted for self-edit
        const firstNameField = document.getElementById('userFirstName');
        const lastNameField = document.getElementById('userLastName');
        const emailField = document.getElementById('userEmail');
        const emailPrefixField = document.getElementById('userEmailPrefix');
        const roleField = document.getElementById('userRole');
        const departmentField = document.getElementById('userDepartment');
        const isActiveField = document.getElementById('userIsActive');
        
        if (isEditingSelf) {
            // Admin editing their own profile - only allow password change
            if (modalTitle) modalTitle.textContent = 'Change Your Password';
            if (submitBtn) submitBtn.textContent = 'Update Password';
            if (passwordHelper) passwordHelper.textContent = 'Enter a new password (at least 8 characters)';
            
            // Disable all fields except password
            if (firstNameField) { firstNameField.disabled = true; firstNameField.classList.add('opacity-50', 'cursor-not-allowed'); }
            if (lastNameField) { lastNameField.disabled = true; lastNameField.classList.add('opacity-50', 'cursor-not-allowed'); }
            if (emailField) { emailField.disabled = true; emailField.classList.add('opacity-50', 'cursor-not-allowed'); }
            if (emailPrefixField) { emailPrefixField.disabled = true; emailPrefixField.classList.add('opacity-50', 'cursor-not-allowed'); }
            if (roleField) { roleField.disabled = true; roleField.classList.add('opacity-50', 'cursor-not-allowed'); }
            if (departmentField) { departmentField.disabled = true; departmentField.classList.add('opacity-50', 'cursor-not-allowed'); }
            if (isActiveField) { isActiveField.disabled = true; isActiveField.classList.add('opacity-50', 'cursor-not-allowed'); }
            
            // Password is required for self-edit
            if (passwordField) passwordField.required = true;
            if (passwordRequired) passwordRequired.style.display = 'inline';
        } else {
            // Admin editing another user - full access
            if (modalTitle) modalTitle.textContent = 'Edit User';
            if (submitBtn) submitBtn.textContent = 'Update User';
            if (passwordHelper) passwordHelper.textContent = 'Leave blank to keep current password';
            
            // Enable all fields
            if (firstNameField) { firstNameField.disabled = false; firstNameField.classList.remove('opacity-50', 'cursor-not-allowed'); }
            if (lastNameField) { lastNameField.disabled = false; lastNameField.classList.remove('opacity-50', 'cursor-not-allowed'); }
            if (emailField) { emailField.disabled = false; emailField.classList.remove('opacity-50', 'cursor-not-allowed'); }
            if (emailPrefixField) { emailPrefixField.disabled = false; emailPrefixField.classList.remove('opacity-50', 'cursor-not-allowed'); }
            if (roleField) { roleField.disabled = false; roleField.classList.remove('opacity-50', 'cursor-not-allowed'); }
            if (departmentField) { departmentField.disabled = false; departmentField.classList.remove('opacity-50', 'cursor-not-allowed'); }
            if (isActiveField) { isActiveField.disabled = false; isActiveField.classList.remove('opacity-50', 'cursor-not-allowed'); }
            
            // Password is optional for editing others
            if (passwordField) passwordField.required = false;
            if (passwordRequired) passwordRequired.style.display = 'none';
        }
        
        // Fill form
        document.getElementById('userFirstName').value = user.firstName || '';
        document.getElementById('userLastName').value = user.lastName || '';
        document.getElementById('userPassword').value = '';
        document.getElementById('userRole').value = user.role || '';
        document.getElementById('userDepartment').value = user.departmentId || '';
        document.getElementById('userIsActive').checked = user.isActive !== false;
        
        // Update department field visibility based on user's role
        // Note: This will be called again after modern dropdowns are initialized
        this.updateDepartmentFieldForRole(user.role);
        
        // Set the hidden email field and parse email prefix for display
        const fullEmail = user.email || '';
        document.getElementById('userEmail').value = fullEmail;
        
        // Parse email to show prefix and domain correctly
        this.setEmailFieldsForEdit(fullEmail, user.role);
        
        if (modal) {
            modal.classList.remove('hidden');
            modal.classList.add('active');
            
            // Store the role for later use
            const userRole = user.role;
            
            // Initialize modern dropdowns in modal after it's visible
            setTimeout(() => {
                if (typeof window.initModernDropdowns === 'function') {
                    window.initModernDropdowns('#userRole, #userDepartment');
                }
                // Refresh dropdowns with current values
                this.refreshDropdown(document.getElementById('userRole'));
                this.refreshDropdown(document.getElementById('userDepartment'));
                
                // Re-apply field states after dropdowns are initialized
                this.updateDepartmentFieldForRole(userRole);
            }, 50);
        }
    }
    
    /**
     * Set email fields for editing an existing user
     */
    setEmailFieldsForEdit(email, role) {
        const emailPrefixInput = document.getElementById('userEmailPrefix');
        const emailDomainSpan = document.getElementById('userEmailDomain');
        const emailFormatHint = document.getElementById('emailFormatHint');
        
        if (!emailPrefixInput || !emailDomainSpan || !email) return;
        
        // Email domain patterns for each role
        const domainPatterns = {
            'ROLE_ADMIN': '@admin.alquds.edu',
            'ROLE_DEANSHIP': '@dean.alquds.edu',
            'ROLE_HOD': '@hod.alquds.edu',
            'ROLE_PROFESSOR': '@staff.alquds.edu'
        };
        
        const domain = domainPatterns[role] || '@alquds.edu';
        const atIndex = email.indexOf('@');
        
        if (atIndex > 0) {
            const prefix = email.substring(0, atIndex);
            const emailDomain = email.substring(atIndex);
            
            if (role === 'ROLE_HOD') {
                // For HOD, show full email format
                emailPrefixInput.style.display = 'none';
                emailDomainSpan.textContent = email;
                if (emailFormatHint) emailFormatHint.textContent = 'HOD email is auto-generated based on department';
            } else {
                emailPrefixInput.value = prefix;
                emailPrefixInput.style.display = 'block';
                emailDomainSpan.textContent = emailDomain;
                if (emailFormatHint) emailFormatHint.textContent = `Current email: ${email}`;
            }
        }
    }

    /**
     * Close user modal
     */
    closeUserModal() {
        const modal = document.getElementById('userModal');
        const form = document.getElementById('userForm');
        
        if (modal) {
            modal.classList.remove('active');
            modal.classList.add('hidden');
        }
        
        // Reset the form completely
        if (form) {
            form.reset();
        }
        
        // Reset role dropdown FIRST (before resetting department)
        const roleField = document.getElementById('userRole');
        if (roleField) {
            roleField.value = '';
            roleField.selectedIndex = 0;
            this.refreshDropdown(roleField);
        }
        
        // Reset all fields and dropdowns
        this.resetEmailFields();
        this.resetDepartmentField();
        
        // Reset department dropdown and ensure it's enabled
        const departmentField = document.getElementById('userDepartment');
        if (departmentField) {
            departmentField.value = '';
            departmentField.selectedIndex = 0;
            departmentField.disabled = false;
            
            // Ensure modern dropdown is enabled
            if (typeof window.setModernDropdownDisabled === 'function') {
                window.setModernDropdownDisabled(departmentField, false);
            }
            
            // Also reset the visual state of the wrapper directly
            const wrapper = departmentField.nextElementSibling;
            if (wrapper && wrapper.classList.contains('modern-dropdown-wrapper')) {
                wrapper.classList.remove('disabled');
                wrapper.style.pointerEvents = '';
                wrapper.style.opacity = '';
                const toggle = wrapper.querySelector('.modern-dropdown-toggle');
                if (toggle) {
                    toggle.disabled = false;
                    toggle.style.cursor = '';
                    toggle.style.backgroundColor = '';
                }
            }
            
            this.refreshDropdown(departmentField);
        }
        
        // Reset first/last name fields to enabled state
        const firstNameField = document.getElementById('userFirstName');
        const lastNameField = document.getElementById('userLastName');
        if (firstNameField) {
            firstNameField.value = '';
            firstNameField.disabled = false;
            firstNameField.readOnly = false;
            firstNameField.style.removeProperty('pointer-events');
            firstNameField.style.removeProperty('opacity');
            firstNameField.style.removeProperty('background-color');
            firstNameField.style.removeProperty('color');
            firstNameField.style.removeProperty('cursor');
        }
        if (lastNameField) {
            lastNameField.value = '';
            lastNameField.disabled = false;
            lastNameField.readOnly = false;
            lastNameField.style.removeProperty('pointer-events');
            lastNameField.style.removeProperty('opacity');
            lastNameField.style.removeProperty('background-color');
            lastNameField.style.removeProperty('color');
            lastNameField.style.removeProperty('cursor');
        }
        
        // Clear password field
        const passwordField = document.getElementById('userPassword');
        if (passwordField) {
            passwordField.value = '';
        }
        
        // Reset active checkbox
        const isActiveCheckbox = document.getElementById('userIsActive');
        if (isActiveCheckbox) {
            isActiveCheckbox.checked = true;
        }
        
        // Clear hidden email field
        const emailHidden = document.getElementById('userEmail');
        if (emailHidden) emailHidden.value = '';
        
        // Clear email prefix field
        const emailPrefixField = document.getElementById('userEmailPrefix');
        if (emailPrefixField) {
            emailPrefixField.value = '';
        }
        
        this.editingUserId = null;
    }

    /**
     * Open delete user modal
     */
    openDeleteUserModal(userId) {
        // Prevent admin from deleting themselves
        if (userId === this.currentUserId) {
            showToast('You cannot delete your own account', 'error');
            return;
        }
        
        this.deletingUserId = userId;
        const modal = document.getElementById('deleteModal');
        const title = document.getElementById('deleteModalTitle');
        const message = document.getElementById('deleteModalMessage');
        
        if (title) title.textContent = 'Delete User?';
        if (message) message.textContent = 'This action cannot be undone. The user will be permanently deleted.';
        
        // Set up confirm button
        const confirmBtn = document.getElementById('deleteModalConfirm');
        const cancelBtn = document.getElementById('deleteModalCancel');
        
        if (confirmBtn) {
            confirmBtn.onclick = () => this.deleteUser();
        }
        if (cancelBtn) {
            cancelBtn.onclick = () => this.closeDeleteModal();
        }
        
        if (modal) {
            modal.classList.remove('hidden');
            modal.classList.add('active');
        }
    }

    /**
     * Close delete modal
     */
    closeDeleteModal() {
        const modal = document.getElementById('deleteModal');
        if (modal) {
            modal.classList.remove('active');
            modal.classList.add('hidden');
        }
        this.deletingUserId = null;
        this.deletingDepartmentId = null;
        this.deletingCourseId = null;
    }

    /**
     * Handle user form submission
     */
    async handleUserFormSubmit(e) {
        e.preventDefault();
        
        const password = document.getElementById('userPassword').value;
        const isEditingSelf = this.editingUserId === this.currentUserId;
        
        // For self-edit, only password is allowed
        if (isEditingSelf) {
            if (!password) {
                showToast('Please enter a new password', 'error');
                return;
            }
            if (password.length < 8) {
                showToast('Password must be at least 8 characters', 'error');
                return;
            }
            
            try {
                await apiRequest(`/admin/users/${this.editingUserId}/password`, {
                    method: 'PUT',
                    body: JSON.stringify({ password }),
                });
                showToast('Password updated successfully', 'success');
                this.closeUserModal();
                return;
            } catch (error) {
                console.error('[AdminDashboard] Password update error:', error);
                showToast(error.message || 'Failed to update password', 'error');
                return;
            }
        }
        
        // Validate role is selected
        const roleSelect = document.getElementById('userRole');
        const role = roleSelect.value;
        console.log('[AdminDashboard] Form submit - Role validation:', {
            roleSelectElement: roleSelect,
            roleValue: role,
            selectedIndex: roleSelect.selectedIndex,
            options: Array.from(roleSelect.options).map(o => ({ value: o.value, text: o.text, selected: o.selected }))
        });
        if (!role) {
            showToast('Please select a role', 'error');
            return;
        }
        
        // Validate email based on role
        const email = document.getElementById('userEmail').value;
        if (!email) {
            if (role === 'ROLE_HOD') {
                showToast('Please select a department for HOD email', 'error');
            } else {
                showToast('Please enter an email username', 'error');
            }
            return;
        }
        
        // Validate department for roles that require it
        const departmentId = document.getElementById('userDepartment').value || null;
        if ((role === 'ROLE_HOD' || role === 'ROLE_PROFESSOR') && !departmentId) {
            showToast(`Please select a department for ${role === 'ROLE_HOD' ? 'HOD' : 'Professor'}`, 'error');
            return;
        }
        
        // For editing other users or creating new users
        const formData = {
            firstName: document.getElementById('userFirstName').value,
            lastName: document.getElementById('userLastName').value,
            email: email,
            role: role,
            // Only include departmentId for roles that require it (HOD and Professor)
            // For Admin and Dean, explicitly set to null
            departmentId: (role === 'ROLE_HOD' || role === 'ROLE_PROFESSOR') ? departmentId : null,
            isActive: document.getElementById('userIsActive').checked,
        };
        
        if (password) {
            formData.password = password;
        }
        
        // Get submit button and show loading state
        const submitBtn = document.getElementById('userSubmitBtn');
        const { restore: restoreButton } = setButtonLoading(submitBtn, this.editingUserId ? 'Updating...' : 'Creating...');
        
        try {
            if (this.editingUserId) {
                await apiRequest(`/admin/users/${this.editingUserId}`, {
                    method: 'PUT',
                    body: JSON.stringify(formData),
                });
                showToast('User updated successfully', 'success');
            } else {
                if (!password) {
                    restoreButton();
                    showToast('Password is required for new users', 'error');
                    return;
                }
                await apiRequest('/admin/users', {
                    method: 'POST',
                    body: JSON.stringify(formData),
                });
                showToast('User created successfully', 'success');
            }
            
            this.closeUserModal();
            this.loadUsers();
            
        } catch (error) {
            console.error('[AdminDashboard] User form error:', error);
            // Use formatted validation errors for better user feedback
            const errorMessage = error.formattedMessage || error.message || 'Failed to save user';
            // Check for password-related validation errors and show professional message
            if (errorMessage.toLowerCase().includes('password')) {
                showToast('⚠️ Password Requirements\n\n• Minimum 8 characters\n• Include uppercase & lowercase letters\n• Include at least one number\n• Include a special character (!@#$%^&*)', 'warning', 10000);
            } else {
                showToast(errorMessage, 'error');
            }
        } finally {
            restoreButton();
        }
    }

    /**
     * Delete user
     */
    async deleteUser() {
        if (!this.deletingUserId) return;
        
        // Get delete button and show loading state
        const deleteBtn = document.getElementById('deleteModalConfirm');
        const { restore: restoreButton } = setButtonLoading(deleteBtn, 'Deleting...');
        
        try {
            await apiRequest(`/admin/users/${this.deletingUserId}`, {
                method: 'DELETE',
            });
            
            showToast('User deleted successfully', 'success');
            this.closeDeleteModal();
            this.loadUsers();
            
        } catch (error) {
            console.error('[AdminDashboard] Delete user error:', error);
            showToast('Failed to delete user', 'error');
        } finally {
            restoreButton();
        }
    }

    /**
     * Show/hide users table loading state
     */
    showUsersTableLoading(show) {
        const tbody = document.getElementById('usersTableBody');
        if (tbody && show) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center py-8">
                        <div class="inline-block w-8 h-8 border-4 border-purple-200 border-t-purple-600 rounded-full animate-spin"></div>
                        <p class="mt-2 text-gray-500 dark:text-gray-400">Loading users...</p>
                    </td>
                </tr>
            `;
        }
    }

    // ========================================
    // DEPARTMENTS TAB
    // ========================================

    /**
     * Initialize departments tab
     */
    async initializeDepartmentsTab() {
        await this.loadDepartments();
        this.setupDepartmentsEventListeners();
    }

    /**
     * Load departments from API
     */
    async loadDepartments() {
        try {
            this.showDepartmentsTableLoading(true);
            
            await withMinLoadingTime(async () => {
                const response = await apiRequest('/admin/departments', { method: 'GET' });
                this.departments = response || [];
            });
            
            this.renderDepartmentsTable();
            this.showDepartmentsTableLoading(false);
            
        } catch (error) {
            console.error('[AdminDashboard] Failed to load departments:', error);
            this.showDepartmentsTableLoading(false);
            showToast('Failed to load departments', 'error');
        }
    }

    /**
     * Render departments table
     */
    renderDepartmentsTable() {
        const tbody = document.getElementById('departmentsTableBody');
        if (!tbody) return;
        
        if (this.departments.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center py-8">
                        <div class="text-4xl mb-2">🏛️</div>
                        <div class="font-medium text-gray-900 dark:text-white">No departments found</div>
                        <div class="text-sm text-gray-500 dark:text-gray-400">Create a new department to get started</div>
                    </td>
                </tr>
            `;
            return;
        }
        
        tbody.innerHTML = this.departments.map(dept => this.createDepartmentRowHTML(dept)).join('');
        
        // Add event listeners to action buttons
        this.departments.forEach(dept => {
            const editBtn = document.getElementById(`editDept-${dept.id}`);
            const deleteBtn = document.getElementById(`deleteDept-${dept.id}`);
            
            if (editBtn) {
                editBtn.addEventListener('click', () => this.openEditDepartmentModal(dept));
            }
            if (deleteBtn) {
                deleteBtn.addEventListener('click', () => this.openDeleteDepartmentModal(dept.id));
            }
        });
    }

    /**
     * Create HTML for a department row
     */
    createDepartmentRowHTML(dept) {
        return `
            <tr class="border-b border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800">
                <td class="px-4 py-3 text-sm font-medium text-gray-900 dark:text-white">${this.escapeHtml(dept.name || '')}</td>
                <td class="px-4 py-3"><span class="px-2 py-1 text-xs font-mono bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200 rounded">${this.escapeHtml(dept.shortcut || '')}</span></td>
                <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-300">${this.escapeHtml(dept.description || '-')}</td>
                <td class="px-4 py-3">
                    <div class="flex items-center gap-2">
                        <button id="editDept-${dept.id}" class="p-1.5 text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/30 rounded" title="Edit">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                        </button>
                        <button id="deleteDept-${dept.id}" class="p-1.5 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/30 rounded" title="Delete">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }

    /**
     * Set up departments tab event listeners
     */
    setupDepartmentsEventListeners() {
        // Create department button
        const createBtn = document.getElementById('addDepartmentBtn');
        if (createBtn) {
            createBtn.addEventListener('click', () => this.openCreateDepartmentModal());
        }
        
        // Modal close buttons
        const deptModalClose = document.getElementById('departmentModalClose');
        const deptModalCancel = document.getElementById('departmentModalCancel');
        if (deptModalClose) deptModalClose.addEventListener('click', () => this.closeDepartmentModal());
        if (deptModalCancel) deptModalCancel.addEventListener('click', () => this.closeDepartmentModal());
        
        // Form submission
        const deptForm = document.getElementById('departmentForm');
        if (deptForm) {
            deptForm.addEventListener('submit', (e) => this.handleDepartmentFormSubmit(e));
        }
        
        // Shortcut input - enforce lowercase
        const shortcutInput = document.getElementById('departmentShortcut');
        if (shortcutInput) {
            shortcutInput.addEventListener('input', (e) => {
                e.target.value = e.target.value.toLowerCase().replace(/[^a-z0-9]/g, '');
            });
        }
    }

    /**
     * Open create department modal
     */
    openCreateDepartmentModal() {
        this.editingDepartmentId = null;
        const modal = document.getElementById('departmentModal');
        const modalTitle = document.getElementById('departmentModalTitle');
        const submitBtn = document.getElementById('departmentSubmitBtn');
        const form = document.getElementById('departmentForm');
        
        if (modalTitle) modalTitle.textContent = 'Create Department';
        if (submitBtn) submitBtn.textContent = 'Create Department';
        if (form) form.reset();
        
        if (modal) {
            modal.classList.remove('hidden');
            modal.classList.add('active');
        }
    }

    /**
     * Open edit department modal
     */
    openEditDepartmentModal(dept) {
        this.editingDepartmentId = dept.id;
        const modal = document.getElementById('departmentModal');
        const modalTitle = document.getElementById('departmentModalTitle');
        const submitBtn = document.getElementById('departmentSubmitBtn');
        
        if (modalTitle) modalTitle.textContent = 'Edit Department';
        if (submitBtn) submitBtn.textContent = 'Update Department';
        
        // Fill form
        document.getElementById('departmentName').value = dept.name || '';
        document.getElementById('departmentShortcut').value = dept.shortcut || '';
        document.getElementById('departmentDescription').value = dept.description || '';
        
        if (modal) {
            modal.classList.remove('hidden');
            modal.classList.add('active');
        }
    }

    /**
     * Close department modal
     */
    closeDepartmentModal() {
        const modal = document.getElementById('departmentModal');
        if (modal) {
            modal.classList.remove('active');
            modal.classList.add('hidden');
        }
        this.editingDepartmentId = null;
    }

    /**
     * Open delete department modal
     */
    openDeleteDepartmentModal(departmentId) {
        this.deletingDepartmentId = departmentId;
        const modal = document.getElementById('deleteModal');
        const title = document.getElementById('deleteModalTitle');
        const message = document.getElementById('deleteModalMessage');
        
        if (title) title.textContent = 'Delete Department?';
        if (message) message.textContent = 'This action cannot be undone. All associated data may be affected.';
        
        // Set up confirm button
        const confirmBtn = document.getElementById('deleteModalConfirm');
        const cancelBtn = document.getElementById('deleteModalCancel');
        
        if (confirmBtn) {
            confirmBtn.onclick = () => this.deleteDepartment();
        }
        if (cancelBtn) {
            cancelBtn.onclick = () => this.closeDeleteModal();
        }
        
        if (modal) {
            modal.classList.remove('hidden');
            modal.classList.add('active');
        }
    }

    /**
     * Handle department form submission
     */
    async handleDepartmentFormSubmit(e) {
        e.preventDefault();
        
        const formData = {
            name: document.getElementById('departmentName').value,
            shortcut: document.getElementById('departmentShortcut').value,
            description: document.getElementById('departmentDescription').value || null,
        };
        
        // Get submit button and show loading state
        const submitBtn = document.getElementById('departmentSubmitBtn');
        const { restore: restoreButton } = setButtonLoading(submitBtn, this.editingDepartmentId ? 'Updating...' : 'Creating...');
        
        try {
            if (this.editingDepartmentId) {
                await apiRequest(`/admin/departments/${this.editingDepartmentId}`, {
                    method: 'PUT',
                    body: JSON.stringify(formData),
                });
                showToast('Department updated successfully', 'success');
            } else {
                await apiRequest('/admin/departments', {
                    method: 'POST',
                    body: JSON.stringify(formData),
                });
                showToast('Department created successfully', 'success');
            }
            
            this.closeDepartmentModal();
            this.loadDepartments();
            // Also refresh department dropdowns in users and courses tabs
            this.renderUserDepartmentOptions();
            this.renderCourseDepartmentOptions();
            
        } catch (error) {
            console.error('[AdminDashboard] Department form error:', error);
            showToast(error.message || 'Failed to save department', 'error');
        } finally {
            restoreButton();
        }
    }

    /**
     * Delete department
     */
    async deleteDepartment() {
        if (!this.deletingDepartmentId) return;
        
        // Get delete button and show loading state
        const deleteBtn = document.getElementById('deleteDepartmentModalConfirm');
        const { restore: restoreButton } = setButtonLoading(deleteBtn, 'Deleting...');
        
        try {
            await apiRequest(`/admin/departments/${this.deletingDepartmentId}`, {
                method: 'DELETE',
            });
            
            showToast('Department deleted successfully', 'success');
            this.closeDeleteModal();
            this.loadDepartments();
            
            // Refresh users tab data if it has been loaded
            if (this.loadedTabs.has('users')) {
                await this.loadDepartmentsForFilters();
                await this.loadUsers();
            }
            
        } catch (error) {
            console.error('[AdminDashboard] Delete department error:', error);
            showToast(error.message || 'Failed to delete department', 'error');
        } finally {
            restoreButton();
        }
    }

    /**
     * Show/hide departments table loading state
     */
    showDepartmentsTableLoading(show) {
        const tbody = document.getElementById('departmentsTableBody');
        if (tbody && show) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center py-8">
                        <div class="inline-block w-8 h-8 border-4 border-purple-200 border-t-purple-600 rounded-full animate-spin"></div>
                        <p class="mt-2 text-gray-500 dark:text-gray-400">Loading departments...</p>
                    </td>
                </tr>
            `;
        }
    }

    // ========================================
    // COURSES TAB
    // ========================================

    /**
     * Initialize courses tab
     */
    async initializeCoursesTab() {
        await this.loadDepartmentsForCourses();
        await this.loadCourses();
        this.setupCoursesEventListeners();
        
        // Re-initialize modern dropdowns for courses tab after content is loaded
        setTimeout(() => {
            if (typeof window.initModernDropdowns === 'function') {
                window.initModernDropdowns('#courseDepartmentFilter');
            }
        }, 100);
    }

    /**
     * Load departments for courses filters and form
     */
    async loadDepartmentsForCourses() {
        try {
            if (this.departments.length === 0) {
                const response = await apiRequest('/admin/departments', { method: 'GET' });
                this.departments = response || [];
            }
            this.renderCourseDepartmentOptions();
        } catch (error) {
            console.error('[AdminDashboard] Failed to load departments for courses:', error);
        }
    }

    /**
     * Render department options in courses filters and form
     */
    renderCourseDepartmentOptions() {
        const filterSelect = document.getElementById('courseDepartmentFilter');
        const formSelect = document.getElementById('courseDepartment');
        
        const options = this.departments.map(dept => 
            `<option value="${dept.id}">${this.escapeHtml(dept.name)}</option>`
        ).join('');
        
        if (filterSelect) {
            filterSelect.innerHTML = '<option value="">All Departments</option>' + options;
            this.refreshDropdown(filterSelect);
        }
        
        if (formSelect) {
            formSelect.innerHTML = '<option value="">Select Department</option>' + options;
            this.refreshDropdown(formSelect);
        }
    }

    /**
     * Load courses from API
     */
    async loadCourses() {
        try {
            this.showCoursesTableLoading(true);
            
            await withMinLoadingTime(async () => {
                const params = new URLSearchParams();
                if (this.coursesFilters.departmentId) {
                    params.append('departmentId', this.coursesFilters.departmentId);
                }
                
                const queryString = params.toString();
                const url = `/admin/courses${queryString ? '?' + queryString : ''}`;
                
                const response = await apiRequest(url, { method: 'GET' });
                this.courses = response || [];
                
                // Apply client-side filters
                if (this.coursesFilters.search) {
                    const searchLower = this.coursesFilters.search.toLowerCase();
                    this.courses = this.courses.filter(course => 
                        (course.courseCode && course.courseCode.toLowerCase().includes(searchLower)) ||
                        (course.name && course.name.toLowerCase().includes(searchLower))
                    );
                }
                
                if (this.coursesFilters.isActive !== '') {
                    const isActive = this.coursesFilters.isActive === 'true';
                    this.courses = this.courses.filter(course => course.active === isActive);
                }
            });
            
            this.renderCoursesTable();
            this.showCoursesTableLoading(false);
            
        } catch (error) {
            console.error('[AdminDashboard] Failed to load courses:', error);
            this.showCoursesTableLoading(false);
            showToast('Failed to load courses', 'error');
        }
    }

    /**
     * Render courses table
     */
    renderCoursesTable() {
        const tbody = document.getElementById('coursesTableBody');
        if (!tbody) return;
        
        if (this.courses.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center py-8">
                        <div class="text-4xl mb-2">📚</div>
                        <div class="font-medium text-gray-900 dark:text-white">No courses found</div>
                        <div class="text-sm text-gray-500 dark:text-gray-400">Try adjusting your filters or create a new course</div>
                    </td>
                </tr>
            `;
            return;
        }
        
        tbody.innerHTML = this.courses.map(course => this.createCourseRowHTML(course)).join('');
        
        // Add event listeners to action buttons
        this.courses.forEach(course => {
            const editBtn = document.getElementById(`editCourse-${course.id}`);
            const deleteBtn = document.getElementById(`deleteCourse-${course.id}`);
            
            if (editBtn) {
                editBtn.addEventListener('click', () => this.openEditCourseModal(course));
            }
            if (deleteBtn) {
                deleteBtn.addEventListener('click', () => this.openDeleteCourseModal(course.id));
            }
        });
    }

    /**
     * Create HTML for a course row
     */
    createCourseRowHTML(course) {
        const statusClass = course.active 
            ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200' 
            : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200';
        const statusLabel = course.active ? 'Active' : 'Inactive';
        const departmentName = course.department ? course.department.name : '-';
        
        return `
            <tr class="border-b border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800">
                <td class="px-4 py-3"><span class="px-2 py-1 text-xs font-mono bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 rounded">${this.escapeHtml(course.courseCode || '')}</span></td>
                <td class="px-4 py-3 text-sm font-medium text-gray-900 dark:text-white">${this.escapeHtml(course.name || '')}</td>
                <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-300">${this.escapeHtml(departmentName)}</td>
                <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-300">${course.creditHours || '-'}</td>
                <td class="px-4 py-3"><span class="px-2 py-1 text-xs font-medium rounded-full ${statusClass}">${statusLabel}</span></td>
                <td class="px-4 py-3">
                    <div class="flex items-center gap-2">
                        <button id="editCourse-${course.id}" class="p-1.5 text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/30 rounded" title="Edit">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                        </button>
                        <button id="deleteCourse-${course.id}" class="p-1.5 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/30 rounded" title="Deactivate">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }

    /**
     * Set up courses tab event listeners
     */
    setupCoursesEventListeners() {
        // Create course button
        const createBtn = document.getElementById('addCourseBtn');
        if (createBtn) {
            createBtn.addEventListener('click', () => this.openCreateCourseModal());
        }
        
        // Search input
        const searchInput = document.getElementById('courseSearchInput');
        if (searchInput) {
            let debounceTimer;
            searchInput.addEventListener('input', (e) => {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(() => {
                    this.coursesFilters.search = e.target.value;
                    this.loadCourses();
                }, 300);
            });
        }
        
        // Department filter
        const departmentFilter = document.getElementById('courseDepartmentFilter');
        if (departmentFilter) {
            departmentFilter.addEventListener('change', (e) => {
                this.coursesFilters.departmentId = e.target.value;
                this.loadCourses();
            });
        }
        
        // Modal close buttons
        const courseModalClose = document.getElementById('courseModalClose');
        const courseModalCancel = document.getElementById('courseModalCancel');
        if (courseModalClose) courseModalClose.addEventListener('click', () => this.closeCourseModal());
        if (courseModalCancel) courseModalCancel.addEventListener('click', () => this.closeCourseModal());
        
        // Form submission
        const courseForm = document.getElementById('courseForm');
        if (courseForm) {
            courseForm.addEventListener('submit', (e) => this.handleCourseFormSubmit(e));
        }
    }

    /**
     * Open create course modal
     */
    openCreateCourseModal() {
        this.editingCourseId = null;
        const modal = document.getElementById('courseModal');
        const modalTitle = document.getElementById('courseModalTitle');
        const submitBtn = document.getElementById('courseSubmitBtn');
        const form = document.getElementById('courseForm');
        
        if (modalTitle) modalTitle.textContent = 'Create Course';
        if (submitBtn) submitBtn.textContent = 'Create Course';
        if (form) form.reset();
        
        if (modal) {
            modal.classList.remove('hidden');
            modal.classList.add('active');
            
            // Initialize modern dropdowns in modal after it's visible
            setTimeout(() => {
                if (typeof window.initModernDropdowns === 'function') {
                    window.initModernDropdowns('#courseDepartment');
                }
            }, 50);
        }
    }

    /**
     * Open edit course modal
     */
    openEditCourseModal(course) {
        this.editingCourseId = course.id;
        const modal = document.getElementById('courseModal');
        const modalTitle = document.getElementById('courseModalTitle');
        const submitBtn = document.getElementById('courseSubmitBtn');
        
        if (modalTitle) modalTitle.textContent = 'Edit Course';
        if (submitBtn) submitBtn.textContent = 'Update Course';
        
        // Fill form
        document.getElementById('courseCode').value = course.courseCode || '';
        document.getElementById('courseName').value = course.name || '';
        document.getElementById('courseDepartment').value = course.department ? course.department.id : '';
        document.getElementById('courseCredits').value = course.creditHours || '';
        document.getElementById('courseDescription').value = course.description || '';
        
        if (modal) {
            modal.classList.remove('hidden');
            modal.classList.add('active');
            
            // Initialize modern dropdowns in modal after it's visible
            setTimeout(() => {
                if (typeof window.initModernDropdowns === 'function') {
                    window.initModernDropdowns('#courseDepartment');
                }
                // Refresh dropdown with current value
                this.refreshDropdown(document.getElementById('courseDepartment'));
            }, 50);
        }
    }

    /**
     * Close course modal
     */
    closeCourseModal() {
        const modal = document.getElementById('courseModal');
        if (modal) {
            modal.classList.remove('active');
            modal.classList.add('hidden');
        }
        this.editingCourseId = null;
    }

    /**
     * Open delete course modal
     */
    openDeleteCourseModal(courseId) {
        this.deletingCourseId = courseId;
        const modal = document.getElementById('deleteModal');
        const title = document.getElementById('deleteModalTitle');
        const message = document.getElementById('deleteModalMessage');
        
        if (title) title.textContent = 'Deactivate Course?';
        if (message) message.textContent = 'The course will be deactivated and hidden from active listings.';
        
        // Set up confirm button
        const confirmBtn = document.getElementById('deleteModalConfirm');
        const cancelBtn = document.getElementById('deleteModalCancel');
        
        if (confirmBtn) {
            confirmBtn.textContent = 'Deactivate';
            confirmBtn.onclick = () => this.deactivateCourse();
        }
        if (cancelBtn) {
            cancelBtn.onclick = () => this.closeDeleteModal();
        }
        
        if (modal) {
            modal.classList.remove('hidden');
            modal.classList.add('active');
        }
    }

    /**
     * Handle course form submission
     */
    async handleCourseFormSubmit(e) {
        e.preventDefault();
        
        const formData = {
            courseCode: document.getElementById('courseCode').value,
            courseName: document.getElementById('courseName').value,
            departmentId: parseInt(document.getElementById('courseDepartment').value),
            description: document.getElementById('courseDescription').value || null,
            isActive: true,
        };
        
        // Add credit hours
        const creditHoursInput = document.getElementById('courseCredits');
        if (creditHoursInput && creditHoursInput.value) {
            formData.level = creditHoursInput.value + ' Credit Hours';
        }
        
        // Get submit button and show loading state
        const submitBtn = document.getElementById('courseSubmitBtn');
        const { restore: restoreButton } = setButtonLoading(submitBtn, this.editingCourseId ? 'Updating...' : 'Creating...');
        
        try {
            if (this.editingCourseId) {
                await apiRequest(`/admin/courses/${this.editingCourseId}`, {
                    method: 'PUT',
                    body: JSON.stringify(formData),
                });
                showToast('Course updated successfully', 'success');
            } else {
                await apiRequest('/admin/courses', {
                    method: 'POST',
                    body: JSON.stringify(formData),
                });
                showToast('Course created successfully', 'success');
            }
            
            this.closeCourseModal();
            this.loadCourses();
            
        } catch (error) {
            console.error('[AdminDashboard] Course form error:', error);
            showToast(error.message || 'Failed to save course', 'error');
        } finally {
            restoreButton();
        }
    }

    /**
     * Deactivate course
     */
    async deactivateCourse() {
        if (!this.deletingCourseId) return;
        
        // Get deactivate button and show loading state
        const deactivateBtn = document.getElementById('deleteCourseModalConfirm');
        const { restore: restoreButton } = setButtonLoading(deactivateBtn, 'Deactivating...');
        
        try {
            await apiRequest(`/admin/courses/${this.deletingCourseId}/deactivate`, {
                method: 'PUT',
            });
            
            showToast('Course deactivated successfully', 'success');
            this.closeDeleteCourseModal();
            this.loadCourses();
            
        } catch (error) {
            console.error('[AdminDashboard] Deactivate course error:', error);
            showToast(error.message || 'Failed to deactivate course', 'error');
        } finally {
            restoreButton();
        }
    }

    /**
     * Show/hide courses table loading state
     */
    showCoursesTableLoading(show) {
        const tbody = document.getElementById('coursesTableBody');
        if (tbody && show) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center py-8">
                        <div class="inline-block w-8 h-8 border-4 border-purple-200 border-t-purple-600 rounded-full animate-spin"></div>
                        <p class="mt-2 text-gray-500 dark:text-gray-400">Loading courses...</p>
                    </td>
                </tr>
            `;
        }
    }

    // ========================================
    // REPORTS TAB
    // ========================================

    /**
     * Initialize reports tab
     */
    async initializeReportsTab() {
        // Load fresh statistics when opening reports tab
        await this.loadDashboardStats();
        console.log('[AdminDashboard] Reports tab initialized');
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

    /**
     * Handle general errors with user feedback
     */
    handleError(message, error) {
        console.error('[AdminDashboard] Error:', error);
        showToast(message, 'error');
    }

    /**
     * Escape HTML to prevent XSS
     */
    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Make switchTab available globally for inline onclick handlers
window.switchTab = function(tabName) {
    if (window.adminDashboard) {
        window.adminDashboard.switchTab(tabName);
    }
};

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    const page = new AdminDashboardPage();
    window.adminDashboard = page;
    page.initialize();
});

export default AdminDashboardPage;
