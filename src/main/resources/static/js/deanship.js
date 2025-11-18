/**
 * Deanship Dashboard
 * Manages academic years, professors, courses, assignments, reports, and file explorer
 */

import { apiRequest, getUserInfo, redirectToLogin, clearAuthData } from './api.js';
import { showToast, showModal, showConfirm, formatDate } from './ui.js';

// State
let currentTab = 'academic-years';
let selectedAcademicYear = null;
let selectedSemester = 'FIRST';
let academicYears = [];
let professors = [];
let courses = [];
let departments = [];

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    initializeEventListeners();
    loadInitialData();
});

/**
 * Check authentication and role
 */
function checkAuth() {
    const userInfo = getUserInfo();
    if (!userInfo) {
        redirectToLogin();
        return;
    }

    if (userInfo.role !== 'ROLE_DEANSHIP') {
        showToast('Access denied - Deanship role required', 'error');
        setTimeout(() => redirectToLogin(), 2000);
        return;
    }

    // Display user name
    document.getElementById('deanshipName').textContent = userInfo.fullName || userInfo.email;
}

/**
 * Initialize event listeners
 */
function initializeEventListeners() {
    // Logout
    document.getElementById('logoutBtn').addEventListener('click', handleLogout);

    // Tab navigation
    document.querySelectorAll('.nav-tab').forEach(tab => {
        tab.addEventListener('click', () => switchTab(tab.dataset.tab));
    });

    // Semester tabs
    document.querySelectorAll('.semester-tab').forEach(tab => {
        tab.addEventListener('click', () => switchSemester(tab.dataset.semester));
    });

    // Academic year selector
    document.getElementById('academicYearSelect').addEventListener('change', (e) => {
        selectedAcademicYear = e.target.value ? JSON.parse(e.target.value) : null;
        onContextChange();
    });

    // Academic Years tab
    document.getElementById('addAcademicYearBtn').addEventListener('click', showAddAcademicYearModal);

    // Professors tab
    document.getElementById('addProfessorBtn').addEventListener('click', showAddProfessorModal);
    document.getElementById('professorSearch').addEventListener('input', filterProfessors);
    document.getElementById('professorDepartmentFilter').addEventListener('change', filterProfessors);

    // Courses tab
    document.getElementById('addCourseBtn').addEventListener('click', showAddCourseModal);
    document.getElementById('courseSearch').addEventListener('input', filterCourses);
    document.getElementById('courseDepartmentFilter').addEventListener('change', filterCourses);

    // Assignments tab
    document.getElementById('addAssignmentBtn').addEventListener('click', showAddAssignmentModal);
    document.getElementById('assignmentProfessorFilter').addEventListener('change', loadAssignments);
    document.getElementById('assignmentCourseFilter').addEventListener('change', loadAssignments);

    // Reports tab
    document.getElementById('viewSystemReportBtn').addEventListener('click', loadSystemReport);
}

/**
 * Load initial data
 */
async function loadInitialData() {
    try {
        await Promise.all([
            loadAcademicYears(),
            loadDepartments()
        ]);
    } catch (error) {
        console.error('Error loading initial data:', error);
        showToast('Failed to load initial data', 'error');
    }
}

/**
 * Switch tab
 */
function switchTab(tabName) {
    currentTab = tabName;

    // Update tab buttons
    document.querySelectorAll('.nav-tab').forEach(tab => {
        if (tab.dataset.tab === tabName) {
            tab.classList.add('active', 'border-blue-600', 'text-blue-600');
            tab.classList.remove('border-transparent', 'text-gray-500');
        } else {
            tab.classList.remove('active', 'border-blue-600', 'text-blue-600');
            tab.classList.add('border-transparent', 'text-gray-500');
        }
    });

    // Show/hide tab content
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.add('hidden');
    });
    document.getElementById(`${tabName}-tab`).classList.remove('hidden');

    // Load tab data
    loadTabData(tabName);
}

/**
 * Switch semester
 */
function switchSemester(semester) {
    selectedSemester = semester;

    // Update semester buttons
    document.querySelectorAll('.semester-tab').forEach(tab => {
        if (tab.dataset.semester === semester) {
            tab.classList.add('active', 'bg-blue-600', 'text-white');
            tab.classList.remove('bg-gray-200', 'text-gray-700');
        } else {
            tab.classList.remove('active', 'bg-blue-600', 'text-white');
            tab.classList.add('bg-gray-200', 'text-gray-700');
        }
    });

    onContextChange();
}

/**
 * Handle context change (academic year or semester)
 */
function onContextChange() {
    if (currentTab === 'assignments' || currentTab === 'reports' || currentTab === 'file-explorer') {
        loadTabData(currentTab);
    }
}

/**
 * Load tab data
 */
function loadTabData(tabName) {
    switch (tabName) {
        case 'academic-years':
            loadAcademicYears();
            break;
        case 'professors':
            loadProfessors();
            break;
        case 'courses':
            loadCourses();
            break;
        case 'assignments':
            // Load professors and courses first if not already loaded
            Promise.all([
                professors.length === 0 ? loadProfessors() : Promise.resolve(),
                courses.length === 0 ? loadCourses() : Promise.resolve()
            ]).then(() => {
                loadAssignments();
            });
            break;
        case 'reports':
            // Reports are loaded on demand
            break;
        case 'file-explorer':
            loadFileExplorer();
            break;
    }
}

/**
 * Handle logout
 */
async function handleLogout() {
    try {
        await apiRequest('/auth/logout', { method: 'POST' });
    } catch (error) {
        console.error('Logout error:', error);
    } finally {
        clearAuthData();
        redirectToLogin();
    }
}

// ============================================================================
// ACADEMIC YEARS MANAGEMENT
// ============================================================================

/**
 * Load academic years
 */
async function loadAcademicYears() {
    try {
        academicYears = await apiRequest('/deanship/academic-years', { method: 'GET' });
        renderAcademicYearsTable();
        updateAcademicYearSelector();
    } catch (error) {
        console.error('Error loading academic years:', error);
        showToast('Failed to load academic years', 'error');
    }
}

/**
 * Render academic years table
 */
function renderAcademicYearsTable() {
    const tbody = document.getElementById('academicYearsTableBody');
    
    if (academicYears.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="px-6 py-8 text-center text-gray-500">
                    No academic years found. Click "Add Academic Year" to create one.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = academicYears.map(year => `
        <tr>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${year.yearCode}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${year.startYear}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${year.endYear}</td>
            <td class="px-6 py-4 whitespace-nowrap">
                <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${year.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}">
                    ${year.isActive ? 'Active' : 'Inactive'}
                </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                <button 
                    onclick="window.deanship.editAcademicYear(${year.id})"
                    class="text-blue-600 hover:text-blue-900"
                >
                    Edit
                </button>
                ${!year.isActive ? `
                    <button 
                        onclick="window.deanship.activateAcademicYear(${year.id})"
                        class="text-green-600 hover:text-green-900"
                    >
                        Activate
                    </button>
                ` : ''}
            </td>
        </tr>
    `).join('');
}

/**
 * Update academic year selector
 */
function updateAcademicYearSelector() {
    const select = document.getElementById('academicYearSelect');
    
    if (academicYears.length === 0) {
        select.innerHTML = '<option value="">No academic years available</option>';
        return;
    }

    select.innerHTML = academicYears.map(year => 
        `<option value='${JSON.stringify(year)}'>${year.yearCode}</option>`
    ).join('');

    // Select active year or first year
    const activeYear = academicYears.find(y => y.isActive) || academicYears[0];
    if (activeYear) {
        select.value = JSON.stringify(activeYear);
        selectedAcademicYear = activeYear;
    }
}

/**
 * Show add academic year modal
 */
function showAddAcademicYearModal() {
    const content = `
        <form id="academicYearForm" class="space-y-4">
            <div>
                <label for="startYear" class="block text-sm font-medium text-gray-700 mb-1">Start Year *</label>
                <input 
                    type="number" 
                    id="startYear" 
                    name="startYear"
                    required
                    min="2000"
                    max="2100"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="e.g., 2024"
                >
            </div>
            <div>
                <label for="endYear" class="block text-sm font-medium text-gray-700 mb-1">End Year *</label>
                <input 
                    type="number" 
                    id="endYear" 
                    name="endYear"
                    required
                    min="2000"
                    max="2100"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="e.g., 2025"
                >
            </div>
            <p class="text-sm text-gray-600">
                The system will automatically create three semesters (First, Second, Summer) for this academic year.
            </p>
        </form>
    `;

    showModal('Add Academic Year', content, {
        buttons: [
            {
                text: 'Cancel',
                className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                action: 'cancel',
                onClick: (close) => close()
            },
            {
                text: 'Create',
                className: 'bg-blue-600 text-white hover:bg-blue-700',
                action: 'create',
                onClick: (close) => handleCreateAcademicYear(close)
            }
        ]
    });
}

/**
 * Handle create academic year
 */
async function handleCreateAcademicYear(closeModal) {
    const form = document.getElementById('academicYearForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const startYear = parseInt(document.getElementById('startYear').value);
    const endYear = parseInt(document.getElementById('endYear').value);

    if (endYear <= startYear) {
        showToast('End year must be greater than start year', 'error');
        return;
    }

    try {
        await apiRequest('/deanship/academic-years', {
            method: 'POST',
            body: JSON.stringify({ startYear, endYear })
        });

        showToast('Academic year created successfully', 'success');
        closeModal();
        loadAcademicYears();
    } catch (error) {
        console.error('Error creating academic year:', error);
        showToast(error.message || 'Failed to create academic year', 'error');
    }
}

/**
 * Edit academic year
 */
window.deanship = window.deanship || {};
window.deanship.editAcademicYear = function(yearId) {
    const year = academicYears.find(y => y.id === yearId);
    if (!year) return;

    const content = `
        <form id="editAcademicYearForm" class="space-y-4">
            <div>
                <label for="editStartYear" class="block text-sm font-medium text-gray-700 mb-1">Start Year *</label>
                <input 
                    type="number" 
                    id="editStartYear" 
                    name="startYear"
                    required
                    min="2000"
                    max="2100"
                    value="${year.startYear}"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
            </div>
            <div>
                <label for="editEndYear" class="block text-sm font-medium text-gray-700 mb-1">End Year *</label>
                <input 
                    type="number" 
                    id="editEndYear" 
                    name="endYear"
                    required
                    min="2000"
                    max="2100"
                    value="${year.endYear}"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
            </div>
        </form>
    `;

    showModal('Edit Academic Year', content, {
        buttons: [
            {
                text: 'Cancel',
                className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                action: 'cancel',
                onClick: (close) => close()
            },
            {
                text: 'Save',
                className: 'bg-blue-600 text-white hover:bg-blue-700',
                action: 'save',
                onClick: (close) => handleUpdateAcademicYear(yearId, close)
            }
        ]
    });
};

/**
 * Handle update academic year
 */
async function handleUpdateAcademicYear(yearId, closeModal) {
    const form = document.getElementById('editAcademicYearForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const startYear = parseInt(document.getElementById('editStartYear').value);
    const endYear = parseInt(document.getElementById('editEndYear').value);

    if (endYear <= startYear) {
        showToast('End year must be greater than start year', 'error');
        return;
    }

    try {
        await apiRequest(`/deanship/academic-years/${yearId}`, {
            method: 'PUT',
            body: JSON.stringify({ startYear, endYear })
        });

        showToast('Academic year updated successfully', 'success');
        closeModal();
        loadAcademicYears();
    } catch (error) {
        console.error('Error updating academic year:', error);
        showToast(error.message || 'Failed to update academic year', 'error');
    }
}

/**
 * Activate academic year
 */
window.deanship.activateAcademicYear = async function(yearId) {
    try {
        await apiRequest(`/deanship/academic-years/${yearId}/activate`, {
            method: 'PUT'
        });

        showToast('Academic year activated successfully', 'success');
        loadAcademicYears();
    } catch (error) {
        console.error('Error activating academic year:', error);
        showToast(error.message || 'Failed to activate academic year', 'error');
    }
};

// Export for global access
export { loadAcademicYears };

// ============================================================================
// PROFESSORS MANAGEMENT
// ============================================================================

/**
 * Load departments
 */
async function loadDepartments() {
    try {
        departments = await apiRequest('/deanship/departments', { method: 'GET' });
        updateDepartmentFilters();
    } catch (error) {
        console.error('Error loading departments:', error);
        // If endpoint doesn't exist, use mock data
        departments = [
            { id: 1, name: 'Computer Science' },
            { id: 2, name: 'Mathematics' },
            { id: 3, name: 'Physics' },
            { id: 4, name: 'Chemistry' }
        ];
        updateDepartmentFilters();
    }
}

/**
 * Update department filters
 */
function updateDepartmentFilters() {
    const filters = [
        document.getElementById('professorDepartmentFilter'),
        document.getElementById('courseDepartmentFilter')
    ];

    filters.forEach(filter => {
        if (filter) {
            const currentValue = filter.value;
            filter.innerHTML = '<option value="">All Departments</option>' +
                departments.map(dept => 
                    `<option value="${dept.id}">${dept.name}</option>`
                ).join('');
            filter.value = currentValue;
        }
    });
}

/**
 * Load professors
 */
async function loadProfessors() {
    try {
        const departmentId = document.getElementById('professorDepartmentFilter').value;
        const params = departmentId ? `?departmentId=${departmentId}` : '';
        
        professors = await apiRequest(`/deanship/professors${params}`, { method: 'GET' });
        renderProfessorsTable();
    } catch (error) {
        console.error('Error loading professors:', error);
        showToast('Failed to load professors', 'error');
    }
}

/**
 * Filter professors
 */
function filterProfessors() {
    loadProfessors();
}

/**
 * Render professors table
 */
function renderProfessorsTable() {
    const tbody = document.getElementById('professorsTableBody');
    const searchTerm = document.getElementById('professorSearch').value.toLowerCase();
    
    let filteredProfessors = professors;
    if (searchTerm) {
        filteredProfessors = professors.filter(prof => 
            prof.name.toLowerCase().includes(searchTerm) ||
            prof.email.toLowerCase().includes(searchTerm) ||
            (prof.professorId && prof.professorId.toLowerCase().includes(searchTerm))
        );
    }

    if (filteredProfessors.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="px-6 py-8 text-center text-gray-500">
                    No professors found. Click "Add Professor" to create one.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = filteredProfessors.map(prof => `
        <tr>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${prof.professorId || 'N/A'}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${prof.name}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${prof.email}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${prof.department?.name || 'N/A'}</td>
            <td class="px-6 py-4 whitespace-nowrap">
                <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${prof.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}">
                    ${prof.isActive ? 'Active' : 'Inactive'}
                </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                <button 
                    onclick="window.deanship.editProfessor(${prof.id})"
                    class="text-blue-600 hover:text-blue-900"
                >
                    Edit
                </button>
                ${prof.isActive ? `
                    <button 
                        onclick="window.deanship.deactivateProfessor(${prof.id})"
                        class="text-red-600 hover:text-red-900"
                    >
                        Deactivate
                    </button>
                ` : `
                    <button 
                        onclick="window.deanship.activateProfessor(${prof.id})"
                        class="text-green-600 hover:text-green-900"
                    >
                        Activate
                    </button>
                `}
            </td>
        </tr>
    `).join('');
}

/**
 * Show add professor modal
 */
function showAddProfessorModal() {
    const content = `
        <form id="professorForm" class="space-y-4">
            <div>
                <label for="profName" class="block text-sm font-medium text-gray-700 mb-1">Full Name *</label>
                <input 
                    type="text" 
                    id="profName" 
                    name="name"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="e.g., Dr. John Smith"
                >
            </div>
            <div>
                <label for="profEmail" class="block text-sm font-medium text-gray-700 mb-1">Email *</label>
                <input 
                    type="email" 
                    id="profEmail" 
                    name="email"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="e.g., john.smith@alquds.edu"
                >
            </div>
            <div>
                <label for="profPassword" class="block text-sm font-medium text-gray-700 mb-1">Password *</label>
                <input 
                    type="password" 
                    id="profPassword" 
                    name="password"
                    required
                    minlength="6"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="Minimum 6 characters"
                >
            </div>
            <div>
                <label for="profDepartment" class="block text-sm font-medium text-gray-700 mb-1">Department *</label>
                <select 
                    id="profDepartment" 
                    name="departmentId"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    <option value="">Select department...</option>
                    ${departments.map(dept => `<option value="${dept.id}">${dept.name}</option>`).join('')}
                </select>
            </div>
            <p class="text-sm text-gray-600">
                A unique professor ID will be automatically generated.
            </p>
        </form>
    `;

    showModal('Add Professor', content, {
        buttons: [
            {
                text: 'Cancel',
                className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                action: 'cancel',
                onClick: (close) => close()
            },
            {
                text: 'Create',
                className: 'bg-blue-600 text-white hover:bg-blue-700',
                action: 'create',
                onClick: (close) => handleCreateProfessor(close)
            }
        ]
    });
}

/**
 * Handle create professor
 */
async function handleCreateProfessor(closeModal) {
    const form = document.getElementById('professorForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const data = {
        name: document.getElementById('profName').value,
        email: document.getElementById('profEmail').value,
        password: document.getElementById('profPassword').value,
        departmentId: parseInt(document.getElementById('profDepartment').value)
    };

    try {
        await apiRequest('/deanship/professors', {
            method: 'POST',
            body: JSON.stringify(data)
        });

        showToast('Professor created successfully', 'success');
        closeModal();
        loadProfessors();
    } catch (error) {
        console.error('Error creating professor:', error);
        showToast(error.message || 'Failed to create professor', 'error');
    }
}

/**
 * Edit professor
 */
window.deanship.editProfessor = function(profId) {
    const prof = professors.find(p => p.id === profId);
    if (!prof) return;

    const content = `
        <form id="editProfessorForm" class="space-y-4">
            <div>
                <label for="editProfName" class="block text-sm font-medium text-gray-700 mb-1">Full Name *</label>
                <input 
                    type="text" 
                    id="editProfName" 
                    name="name"
                    required
                    value="${prof.name}"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
            </div>
            <div>
                <label for="editProfEmail" class="block text-sm font-medium text-gray-700 mb-1">Email *</label>
                <input 
                    type="email" 
                    id="editProfEmail" 
                    name="email"
                    required
                    value="${prof.email}"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
            </div>
            <div>
                <label for="editProfDepartment" class="block text-sm font-medium text-gray-700 mb-1">Department *</label>
                <select 
                    id="editProfDepartment" 
                    name="departmentId"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    ${departments.map(dept => 
                        `<option value="${dept.id}" ${prof.department?.id === dept.id ? 'selected' : ''}>${dept.name}</option>`
                    ).join('')}
                </select>
            </div>
            <div>
                <label for="editProfPassword" class="block text-sm font-medium text-gray-700 mb-1">New Password</label>
                <input 
                    type="password" 
                    id="editProfPassword" 
                    name="password"
                    minlength="6"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="Leave blank to keep current password"
                >
            </div>
        </form>
    `;

    showModal('Edit Professor', content, {
        buttons: [
            {
                text: 'Cancel',
                className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                action: 'cancel',
                onClick: (close) => close()
            },
            {
                text: 'Save',
                className: 'bg-blue-600 text-white hover:bg-blue-700',
                action: 'save',
                onClick: (close) => handleUpdateProfessor(profId, close)
            }
        ]
    });
};

/**
 * Handle update professor
 */
async function handleUpdateProfessor(profId, closeModal) {
    const form = document.getElementById('editProfessorForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const data = {
        name: document.getElementById('editProfName').value,
        email: document.getElementById('editProfEmail').value,
        departmentId: parseInt(document.getElementById('editProfDepartment').value)
    };

    const password = document.getElementById('editProfPassword').value;
    if (password) {
        data.password = password;
    }

    try {
        await apiRequest(`/deanship/professors/${profId}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });

        showToast('Professor updated successfully', 'success');
        closeModal();
        loadProfessors();
    } catch (error) {
        console.error('Error updating professor:', error);
        showToast(error.message || 'Failed to update professor', 'error');
    }
}

/**
 * Deactivate professor
 */
window.deanship.deactivateProfessor = function(profId) {
    const prof = professors.find(p => p.id === profId);
    if (!prof) return;

    showConfirm(
        'Deactivate Professor',
        `Are you sure you want to deactivate ${prof.name}? They will no longer be able to log in.`,
        async () => {
            try {
                await apiRequest(`/deanship/professors/${profId}/deactivate`, {
                    method: 'PUT'
                });

                showToast('Professor deactivated successfully', 'success');
                loadProfessors();
            } catch (error) {
                console.error('Error deactivating professor:', error);
                showToast(error.message || 'Failed to deactivate professor', 'error');
            }
        },
        { danger: true }
    );
};

/**
 * Activate professor
 */
window.deanship.activateProfessor = async function(profId) {
    try {
        await apiRequest(`/deanship/professors/${profId}/activate`, {
            method: 'PUT'
        });

        showToast('Professor activated successfully', 'success');
        loadProfessors();
    } catch (error) {
        console.error('Error activating professor:', error);
        showToast(error.message || 'Failed to activate professor', 'error');
    }
};

// ============================================================================
// COURSES MANAGEMENT
// ============================================================================

/**
 * Load courses
 */
async function loadCourses() {
    try {
        const departmentId = document.getElementById('courseDepartmentFilter').value;
        const params = departmentId ? `?departmentId=${departmentId}` : '';
        
        courses = await apiRequest(`/deanship/courses${params}`, { method: 'GET' });
        renderCoursesTable();
    } catch (error) {
        console.error('Error loading courses:', error);
        showToast('Failed to load courses', 'error');
    }
}

/**
 * Filter courses
 */
function filterCourses() {
    loadCourses();
}

/**
 * Render courses table
 */
function renderCoursesTable() {
    const tbody = document.getElementById('coursesTableBody');
    const searchTerm = document.getElementById('courseSearch').value.toLowerCase();
    
    let filteredCourses = courses;
    if (searchTerm) {
        filteredCourses = courses.filter(course => 
            course.courseCode.toLowerCase().includes(searchTerm) ||
            course.courseName.toLowerCase().includes(searchTerm)
        );
    }

    if (filteredCourses.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="px-6 py-8 text-center text-gray-500">
                    No courses found. Click "Add Course" to create one.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = filteredCourses.map(course => `
        <tr>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${course.courseCode}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${course.courseName}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${course.department?.name || 'N/A'}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${course.level || 'N/A'}</td>
            <td class="px-6 py-4 whitespace-nowrap">
                <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${course.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}">
                    ${course.isActive ? 'Active' : 'Inactive'}
                </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                <button 
                    onclick="window.deanship.editCourse(${course.id})"
                    class="text-blue-600 hover:text-blue-900"
                >
                    Edit
                </button>
                ${course.isActive ? `
                    <button 
                        onclick="window.deanship.deactivateCourse(${course.id})"
                        class="text-red-600 hover:text-red-900"
                    >
                        Deactivate
                    </button>
                ` : ''}
            </td>
        </tr>
    `).join('');
}

/**
 * Show add course modal
 */
function showAddCourseModal() {
    const content = `
        <form id="courseForm" class="space-y-4">
            <div>
                <label for="courseCode" class="block text-sm font-medium text-gray-700 mb-1">Course Code *</label>
                <input 
                    type="text" 
                    id="courseCode" 
                    name="courseCode"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="e.g., CS101"
                >
            </div>
            <div>
                <label for="courseName" class="block text-sm font-medium text-gray-700 mb-1">Course Name *</label>
                <input 
                    type="text" 
                    id="courseName" 
                    name="courseName"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="e.g., Introduction to Computer Science"
                >
            </div>
            <div>
                <label for="courseDepartment" class="block text-sm font-medium text-gray-700 mb-1">Department *</label>
                <select 
                    id="courseDepartment" 
                    name="departmentId"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    <option value="">Select department...</option>
                    ${departments.map(dept => `<option value="${dept.id}">${dept.name}</option>`).join('')}
                </select>
            </div>
            <div>
                <label for="courseLevel" class="block text-sm font-medium text-gray-700 mb-1">Level</label>
                <select 
                    id="courseLevel" 
                    name="level"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    <option value="">Select level...</option>
                    <option value="Undergraduate">Undergraduate</option>
                    <option value="Graduate">Graduate</option>
                    <option value="Doctoral">Doctoral</option>
                </select>
            </div>
            <div>
                <label for="courseDescription" class="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea 
                    id="courseDescription" 
                    name="description"
                    rows="3"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="Course description..."
                ></textarea>
            </div>
        </form>
    `;

    showModal('Add Course', content, {
        buttons: [
            {
                text: 'Cancel',
                className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                action: 'cancel',
                onClick: (close) => close()
            },
            {
                text: 'Create',
                className: 'bg-blue-600 text-white hover:bg-blue-700',
                action: 'create',
                onClick: (close) => handleCreateCourse(close)
            }
        ]
    });
}

/**
 * Handle create course
 */
async function handleCreateCourse(closeModal) {
    const form = document.getElementById('courseForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const data = {
        courseCode: document.getElementById('courseCode').value,
        courseName: document.getElementById('courseName').value,
        departmentId: parseInt(document.getElementById('courseDepartment').value),
        level: document.getElementById('courseLevel').value || null,
        description: document.getElementById('courseDescription').value || null
    };

    try {
        await apiRequest('/deanship/courses', {
            method: 'POST',
            body: JSON.stringify(data)
        });

        showToast('Course created successfully', 'success');
        closeModal();
        loadCourses();
    } catch (error) {
        console.error('Error creating course:', error);
        showToast(error.message || 'Failed to create course', 'error');
    }
}

/**
 * Edit course
 */
window.deanship.editCourse = function(courseId) {
    const course = courses.find(c => c.id === courseId);
    if (!course) return;

    const content = `
        <form id="editCourseForm" class="space-y-4">
            <div>
                <label for="editCourseCode" class="block text-sm font-medium text-gray-700 mb-1">Course Code *</label>
                <input 
                    type="text" 
                    id="editCourseCode" 
                    name="courseCode"
                    required
                    value="${course.courseCode}"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
            </div>
            <div>
                <label for="editCourseName" class="block text-sm font-medium text-gray-700 mb-1">Course Name *</label>
                <input 
                    type="text" 
                    id="editCourseName" 
                    name="courseName"
                    required
                    value="${course.courseName}"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
            </div>
            <div>
                <label for="editCourseDepartment" class="block text-sm font-medium text-gray-700 mb-1">Department *</label>
                <select 
                    id="editCourseDepartment" 
                    name="departmentId"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    ${departments.map(dept => 
                        `<option value="${dept.id}" ${course.department?.id === dept.id ? 'selected' : ''}>${dept.name}</option>`
                    ).join('')}
                </select>
            </div>
            <div>
                <label for="editCourseLevel" class="block text-sm font-medium text-gray-700 mb-1">Level</label>
                <select 
                    id="editCourseLevel" 
                    name="level"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    <option value="">Select level...</option>
                    <option value="Undergraduate" ${course.level === 'Undergraduate' ? 'selected' : ''}>Undergraduate</option>
                    <option value="Graduate" ${course.level === 'Graduate' ? 'selected' : ''}>Graduate</option>
                    <option value="Doctoral" ${course.level === 'Doctoral' ? 'selected' : ''}>Doctoral</option>
                </select>
            </div>
            <div>
                <label for="editCourseDescription" class="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea 
                    id="editCourseDescription" 
                    name="description"
                    rows="3"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >${course.description || ''}</textarea>
            </div>
        </form>
    `;

    showModal('Edit Course', content, {
        buttons: [
            {
                text: 'Cancel',
                className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                action: 'cancel',
                onClick: (close) => close()
            },
            {
                text: 'Save',
                className: 'bg-blue-600 text-white hover:bg-blue-700',
                action: 'save',
                onClick: (close) => handleUpdateCourse(courseId, close)
            }
        ]
    });
};

/**
 * Handle update course
 */
async function handleUpdateCourse(courseId, closeModal) {
    const form = document.getElementById('editCourseForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const data = {
        courseCode: document.getElementById('editCourseCode').value,
        courseName: document.getElementById('editCourseName').value,
        departmentId: parseInt(document.getElementById('editCourseDepartment').value),
        level: document.getElementById('editCourseLevel').value || null,
        description: document.getElementById('editCourseDescription').value || null
    };

    try {
        await apiRequest(`/deanship/courses/${courseId}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });

        showToast('Course updated successfully', 'success');
        closeModal();
        loadCourses();
    } catch (error) {
        console.error('Error updating course:', error);
        showToast(error.message || 'Failed to update course', 'error');
    }
}

/**
 * Deactivate course
 */
window.deanship.deactivateCourse = function(courseId) {
    const course = courses.find(c => c.id === courseId);
    if (!course) return;

    showConfirm(
        'Deactivate Course',
        `Are you sure you want to deactivate ${course.courseCode} - ${course.courseName}?`,
        async () => {
            try {
                await apiRequest(`/deanship/courses/${courseId}/deactivate`, {
                    method: 'PUT'
                });

                showToast('Course deactivated successfully', 'success');
                loadCourses();
            } catch (error) {
                console.error('Error deactivating course:', error);
                showToast(error.message || 'Failed to deactivate course', 'error');
            }
        },
        { danger: true }
    );
};

// ============================================================================
// COURSE ASSIGNMENTS MANAGEMENT
// ============================================================================

let assignments = [];

/**
 * Load assignments
 */
async function loadAssignments() {
    if (!selectedAcademicYear) {
        document.getElementById('assignmentsTableBody').innerHTML = `
            <tr>
                <td colspan="6" class="px-6 py-8 text-center text-gray-500">
                    Please select an academic year and semester
                </td>
            </tr>
        `;
        return;
    }

    try {
        // Find the semester ID for the selected academic year and semester type
        const semester = selectedAcademicYear.semesters?.find(s => s.type === selectedSemester);
        if (!semester) {
            document.getElementById('assignmentsTableBody').innerHTML = `
                <tr>
                    <td colspan="6" class="px-6 py-8 text-center text-gray-500">
                        Semester not found
                    </td>
                </tr>
            `;
            return;
        }

        const professorId = document.getElementById('assignmentProfessorFilter').value;
        const params = new URLSearchParams({ semesterId: semester.id });
        if (professorId) params.append('professorId', professorId);
        
        assignments = await apiRequest(`/deanship/course-assignments?${params}`, { method: 'GET' });
        renderAssignmentsTable();
        updateAssignmentFilters();
    } catch (error) {
        console.error('Error loading assignments:', error);
        showToast('Failed to load assignments', 'error');
    }
}

/**
 * Update assignment filters
 */
function updateAssignmentFilters() {
    // Update professor filter
    const profFilter = document.getElementById('assignmentProfessorFilter');
    const currentProfValue = profFilter.value;
    profFilter.innerHTML = '<option value="">All Professors</option>' +
        professors.map(prof => `<option value="${prof.id}">${prof.name}</option>`).join('');
    profFilter.value = currentProfValue;

    // Update course filter
    const courseFilter = document.getElementById('assignmentCourseFilter');
    const currentCourseValue = courseFilter.value;
    courseFilter.innerHTML = '<option value="">All Courses</option>' +
        courses.map(course => `<option value="${course.id}">${course.courseCode} - ${course.courseName}</option>`).join('');
    courseFilter.value = currentCourseValue;
}

/**
 * Render assignments table
 */
function renderAssignmentsTable() {
    const tbody = document.getElementById('assignmentsTableBody');
    const courseFilter = document.getElementById('assignmentCourseFilter').value;
    
    let filteredAssignments = assignments;
    if (courseFilter) {
        filteredAssignments = assignments.filter(a => a.course.id === parseInt(courseFilter));
    }

    if (filteredAssignments.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="px-6 py-8 text-center text-gray-500">
                    No course assignments found. Click "Assign Course" to create one.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = filteredAssignments.map(assignment => `
        <tr>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${assignment.professor?.name || 'N/A'}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${assignment.course?.courseCode} - ${assignment.course?.courseName}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${assignment.course?.department?.name || 'N/A'}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${assignment.semester?.type || 'N/A'}</td>
            <td class="px-6 py-4 whitespace-nowrap">
                <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${assignment.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}">
                    ${assignment.isActive ? 'Active' : 'Inactive'}
                </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <button 
                    onclick="window.deanship.unassignCourse(${assignment.id})"
                    class="text-red-600 hover:text-red-900"
                >
                    Unassign
                </button>
            </td>
        </tr>
    `).join('');
}

/**
 * Show add assignment modal
 */
function showAddAssignmentModal() {
    if (!selectedAcademicYear) {
        showToast('Please select an academic year first', 'warning');
        return;
    }

    const semester = selectedAcademicYear.semesters?.find(s => s.type === selectedSemester);
    if (!semester) {
        showToast('Semester not found', 'error');
        return;
    }

    const content = `
        <form id="assignmentForm" class="space-y-4">
            <div>
                <label for="assignProfessor" class="block text-sm font-medium text-gray-700 mb-1">Professor *</label>
                <select 
                    id="assignProfessor" 
                    name="professorId"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    <option value="">Select professor...</option>
                    ${professors.filter(p => p.isActive).map(prof => 
                        `<option value="${prof.id}">${prof.name} (${prof.department?.name || 'N/A'})</option>`
                    ).join('')}
                </select>
            </div>
            <div>
                <label for="assignCourse" class="block text-sm font-medium text-gray-700 mb-1">Course *</label>
                <select 
                    id="assignCourse" 
                    name="courseId"
                    required
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                >
                    <option value="">Select course...</option>
                    ${courses.filter(c => c.isActive).map(course => 
                        `<option value="${course.id}">${course.courseCode} - ${course.courseName}</option>`
                    ).join('')}
                </select>
            </div>
            <div class="bg-gray-50 p-3 rounded-md">
                <p class="text-sm text-gray-700">
                    <strong>Academic Year:</strong> ${selectedAcademicYear.yearCode}<br>
                    <strong>Semester:</strong> ${selectedSemester}
                </p>
            </div>
        </form>
    `;

    showModal('Assign Course', content, {
        buttons: [
            {
                text: 'Cancel',
                className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                action: 'cancel',
                onClick: (close) => close()
            },
            {
                text: 'Assign',
                className: 'bg-blue-600 text-white hover:bg-blue-700',
                action: 'assign',
                onClick: (close) => handleCreateAssignment(semester.id, close)
            }
        ]
    });
}

/**
 * Handle create assignment
 */
async function handleCreateAssignment(semesterId, closeModal) {
    const form = document.getElementById('assignmentForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const data = {
        semesterId: semesterId,
        courseId: parseInt(document.getElementById('assignCourse').value),
        professorId: parseInt(document.getElementById('assignProfessor').value)
    };

    try {
        await apiRequest('/deanship/course-assignments', {
            method: 'POST',
            body: JSON.stringify(data)
        });

        showToast('Course assigned successfully', 'success');
        closeModal();
        loadAssignments();
    } catch (error) {
        console.error('Error creating assignment:', error);
        showToast(error.message || 'Failed to assign course', 'error');
    }
}

/**
 * Unassign course
 */
window.deanship.unassignCourse = function(assignmentId) {
    const assignment = assignments.find(a => a.id === assignmentId);
    if (!assignment) return;

    showConfirm(
        'Unassign Course',
        `Are you sure you want to unassign ${assignment.course?.courseCode} from ${assignment.professor?.name}?`,
        async () => {
            try {
                await apiRequest(`/deanship/course-assignments/${assignmentId}`, {
                    method: 'DELETE'
                });

                showToast('Course unassigned successfully', 'success');
                loadAssignments();
            } catch (error) {
                console.error('Error unassigning course:', error);
                showToast(error.message || 'Failed to unassign course', 'error');
            }
        },
        { danger: true }
    );
};

// ============================================================================
// REPORTS
// ============================================================================

/**
 * Load system report
 */
async function loadSystemReport() {
    if (!selectedAcademicYear) {
        showToast('Please select an academic year first', 'warning');
        return;
    }

    const semester = selectedAcademicYear.semesters?.find(s => s.type === selectedSemester);
    if (!semester) {
        showToast('Semester not found', 'error');
        return;
    }

    try {
        const report = await apiRequest(`/deanship/reports/system-wide?semesterId=${semester.id}`, {
            method: 'GET'
        });

        renderSystemReport(report);
    } catch (error) {
        console.error('Error loading system report:', error);
        showToast('Failed to load system report', 'error');
    }
}

/**
 * Render system report
 */
function renderSystemReport(report) {
    const reportDisplay = document.getElementById('reportDisplay');
    const reportContent = document.getElementById('reportContent');

    if (!report || !report.departmentReports || report.departmentReports.length === 0) {
        reportContent.innerHTML = `
            <div class="text-center py-8 text-gray-500">
                No data available for this semester
            </div>
        `;
        reportDisplay.classList.remove('hidden');
        return;
    }

    let html = `
        <div class="space-y-6">
            <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div class="bg-blue-50 p-4 rounded-lg">
                    <p class="text-sm text-gray-600">Total Professors</p>
                    <p class="text-2xl font-bold text-blue-600">${report.totalProfessors || 0}</p>
                </div>
                <div class="bg-green-50 p-4 rounded-lg">
                    <p class="text-sm text-gray-600">Submitted</p>
                    <p class="text-2xl font-bold text-green-600">${report.totalSubmitted || 0}</p>
                </div>
                <div class="bg-yellow-50 p-4 rounded-lg">
                    <p class="text-sm text-gray-600">Missing</p>
                    <p class="text-2xl font-bold text-yellow-600">${report.totalMissing || 0}</p>
                </div>
                <div class="bg-red-50 p-4 rounded-lg">
                    <p class="text-sm text-gray-600">Overdue</p>
                    <p class="text-2xl font-bold text-red-600">${report.totalOverdue || 0}</p>
                </div>
            </div>
    `;

    // Department breakdown
    html += `
        <div>
            <h4 class="text-lg font-medium text-gray-900 mb-3">Department Breakdown</h4>
            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Department</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Professors</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Submitted</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Missing</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Overdue</th>
                        </tr>
                    </thead>
                    <tbody class="bg-white divide-y divide-gray-200">
    `;

    report.departmentReports.forEach(dept => {
        html += `
            <tr>
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${dept.departmentName}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${dept.professorCount || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-green-600">${dept.submittedCount || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-yellow-600">${dept.missingCount || 0}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-red-600">${dept.overdueCount || 0}</td>
            </tr>
        `;
    });

    html += `
                    </tbody>
                </table>
            </div>
        </div>
    </div>
    `;

    reportContent.innerHTML = html;
    reportDisplay.classList.remove('hidden');
}

// ============================================================================
// FILE EXPLORER
// ============================================================================

let currentPath = '';
let fileExplorerData = null;

/**
 * Load file explorer
 */
async function loadFileExplorer() {
    if (!selectedAcademicYear) {
        document.getElementById('fileExplorerContent').innerHTML = `
            <div class="text-center py-12 text-gray-500">
                <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
                </svg>
                <p class="mt-2">Please select an academic year and semester to browse files</p>
            </div>
        `;
        return;
    }

    const semester = selectedAcademicYear.semesters?.find(s => s.type === selectedSemester);
    if (!semester) {
        showToast('Semester not found', 'error');
        return;
    }

    try {
        if (!currentPath) {
            // Load root
            fileExplorerData = await apiRequest(
                `/file-explorer/root?academicYearId=${selectedAcademicYear.id}&semesterId=${semester.id}`,
                { method: 'GET' }
            );
        } else {
            // Load specific node
            fileExplorerData = await apiRequest(
                `/file-explorer/node?path=${encodeURIComponent(currentPath)}`,
                { method: 'GET' }
            );
        }

        renderFileExplorer();
        updateBreadcrumbs();
    } catch (error) {
        console.error('Error loading file explorer:', error);
        showToast('Failed to load file explorer', 'error');
    }
}

/**
 * Render file explorer
 */
function renderFileExplorer() {
    const content = document.getElementById('fileExplorerContent');

    if (!fileExplorerData || !fileExplorerData.children || fileExplorerData.children.length === 0) {
        content.innerHTML = `
            <div class="text-center py-12 text-gray-500">
                <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
                </svg>
                <p class="mt-2">No files or folders found</p>
            </div>
        `;
        return;
    }

    // Separate folders and files
    const folders = fileExplorerData.children.filter(item => item.type !== 'FILE');
    const files = fileExplorerData.children.filter(item => item.type === 'FILE');

    let html = '<div class="space-y-2">';

    // Render folders
    folders.forEach(folder => {
        html += `
            <div 
                class="flex items-center p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer"
                onclick="window.deanship.navigateToFolder('${folder.path}')"
            >
                <svg class="w-6 h-6 text-blue-500 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
                </svg>
                <div class="flex-1">
                    <p class="text-sm font-medium text-gray-900">${folder.name}</p>
                    <p class="text-xs text-gray-500">${folder.type}</p>
                </div>
                <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                </svg>
            </div>
        `;
    });

    // Render files
    if (files.length > 0) {
        html += `
            <div class="mt-6">
                <h4 class="text-sm font-medium text-gray-700 mb-2">Files</h4>
                <div class="overflow-x-auto">
                    <table class="min-w-full divide-y divide-gray-200">
                        <thead class="bg-gray-50">
                            <tr>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Size</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Uploaded</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
                            </tr>
                        </thead>
                        <tbody class="bg-white divide-y divide-gray-200">
        `;

        files.forEach(file => {
            const metadata = file.metadata || {};
            html += `
                <tr>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${file.name}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${formatFileSize(metadata.fileSize || 0)}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${metadata.uploadedAt ? formatDate(metadata.uploadedAt) : 'N/A'}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                        <button 
                            onclick="window.deanship.downloadFile(${file.entityId})"
                            class="text-blue-600 hover:text-blue-900"
                        >
                            Download
                        </button>
                    </td>
                </tr>
            `;
        });

        html += `
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }

    html += '</div>';
    content.innerHTML = html;
}

/**
 * Update breadcrumbs
 */
function updateBreadcrumbs() {
    const breadcrumbs = document.getElementById('breadcrumbs');
    
    if (!currentPath) {
        breadcrumbs.innerHTML = '<span class="text-gray-900 font-medium">Home</span>';
        return;
    }

    const parts = currentPath.split('/').filter(p => p);
    let html = `<button onclick="window.deanship.navigateToFolder('')" class="text-blue-600 hover:text-blue-900">Home</button>`;
    
    let accumulatedPath = '';
    parts.forEach((part, index) => {
        accumulatedPath += (accumulatedPath ? '/' : '') + part;
        const isLast = index === parts.length - 1;
        
        html += ' <span class="text-gray-400">/</span> ';
        
        if (isLast) {
            html += `<span class="text-gray-900 font-medium">${part}</span>`;
        } else {
            html += `<button onclick="window.deanship.navigateToFolder('${accumulatedPath}')" class="text-blue-600 hover:text-blue-900">${part}</button>`;
        }
    });

    breadcrumbs.innerHTML = html;
}

/**
 * Navigate to folder
 */
window.deanship.navigateToFolder = function(path) {
    currentPath = path;
    loadFileExplorer();
};

/**
 * Download file
 */
window.deanship.downloadFile = async function(fileId) {
    try {
        const response = await fetch(`http://localhost:8080/api/file-explorer/files/${fileId}/download`, {
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            }
        });

        if (!response.ok) {
            throw new Error('Download failed');
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = response.headers.get('Content-Disposition')?.split('filename=')[1] || 'download';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
    } catch (error) {
        console.error('Error downloading file:', error);
        showToast('Failed to download file', 'error');
    }
};

/**
 * Format file size
 */
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}
