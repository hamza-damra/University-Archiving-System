/**
 * HOD Dashboard
 */

import { hod, auth, getUserInfo, isAuthenticated, redirectToLogin, clearAuthData } from './api.js';
import { showToast, showModal, showConfirm, formatDate, debounce } from './ui.js';

// Check authentication
if (!isAuthenticated()) {
    redirectToLogin();
}

const userInfo = getUserInfo();
if (userInfo.role !== 'ROLE_HOD') {
    showToast('Access denied - HOD privileges required', 'error');
    setTimeout(() => redirectToLogin(), 2000);
}

// State
let professors = [];
let requests = [];
let currentPage = 1;
const pageSize = 10;

// DOM Elements
const hodName = document.getElementById('hodName');
const logoutBtn = document.getElementById('logoutBtn');
const professorsList = document.getElementById('professorsList');
const professorSearch = document.getElementById('professorSearch');
const addProfessorBtn = document.getElementById('addProfessorBtn');
const createRequestForm = document.getElementById('createRequestForm');
const professorIdSelect = document.getElementById('professorId');
const requestsTableBody = document.getElementById('requestsTableBody');
const viewReportBtn = document.getElementById('viewReportBtn');
const downloadReportBtn = document.getElementById('downloadReportBtn');

// Initialize
hodName.textContent = userInfo.fullName;
loadProfessors();
loadRequests();

// Logout
logoutBtn.addEventListener('click', () => {
    clearAuthData();
    showToast('Logged out successfully', 'success');
    redirectToLogin();
});

// Load professors
async function loadProfessors() {
    try {
        const response = await hod.getProfessors();
        const pageData = response.data || {};
        professors = Array.isArray(pageData) ? pageData : (pageData.content || []);
        renderProfessors();
        populateProfessorSelect();
    } catch (error) {
        console.error('Error loading professors:', error);
        showToast('Failed to load professors', 'error');
        professorsList.innerHTML = '<p class="text-red-600 text-sm">Error loading professors</p>';
    }
}

// Render professors list
function renderProfessors(filterText = '') {
    const filtered = professors.filter(prof => 
        prof.firstName.toLowerCase().includes(filterText.toLowerCase()) ||
        prof.lastName.toLowerCase().includes(filterText.toLowerCase()) ||
        prof.email.toLowerCase().includes(filterText.toLowerCase())
    );

    if (filtered.length === 0) {
        professorsList.innerHTML = '<p class="text-gray-500 text-sm">No professors found</p>';
        return;
    }

    professorsList.innerHTML = filtered.map(prof => `
        <div class="border border-gray-200 rounded-lg p-3 hover:bg-gray-50 transition">
            <div class="flex justify-between items-start">
                <div class="flex-1">
                    <h4 class="font-medium text-gray-900">${prof.firstName} ${prof.lastName}</h4>
                    <p class="text-sm text-gray-600">${prof.email}</p>
                    ${prof.department ? `<p class="text-xs text-gray-500 mt-1">${prof.department.name}</p>` : ''}
                </div>
                <div class="flex space-x-1">
                    <button 
                        class="p-1 text-blue-600 hover:bg-blue-50 rounded"
                        onclick="window.editProfessor(${prof.id})"
                        title="Edit"
                    >
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                        </svg>
                    </button>
                    <button 
                        class="p-1 text-red-600 hover:bg-red-50 rounded"
                        onclick="window.deleteProfessor(${prof.id})"
                        title="Delete"
                    >
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                        </svg>
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

// Populate professor select in form
function populateProfessorSelect() {
    professorIdSelect.innerHTML = '<option value="">Select professor...</option>' +
        professors.map(prof => `
            <option value="${prof.id}">${prof.firstName} ${prof.lastName}</option>
        `).join('');
}

// Search professors
professorSearch.addEventListener('input', debounce((e) => {
    renderProfessors(e.target.value);
}, 300));

// Add professor
addProfessorBtn.addEventListener('click', () => {
    showProfessorModal();
});

// Edit professor
window.editProfessor = (id) => {
    const professor = professors.find(p => p.id === id);
    if (professor) {
        showProfessorModal(professor);
    }
};

// Delete professor
window.deleteProfessor = (id) => {
    const professor = professors.find(p => p.id === id);
    if (!professor) return;

    showConfirm(
        'Delete Professor',
        `Are you sure you want to delete ${professor.firstName} ${professor.lastName}? This action cannot be undone.`,
        async () => {
            try {
                await hod.deleteProfessor(id);
                showToast('Professor deleted successfully', 'success');
                loadProfessors();
            } catch (error) {
                console.error('Error deleting professor:', error);
                showToast(error.message || 'Failed to delete professor', 'error');
            }
        },
        { danger: true, confirmText: 'Delete' }
    );
};

function bindPasswordToggle(formElement) {
    const passwordInput = formElement?.querySelector('input[name="password"]');
    const toggleBtn = formElement?.querySelector('[data-toggle-password]');

    if (!passwordInput || !toggleBtn) {
        return;
    }

    const eyeOpen = toggleBtn.querySelector('[data-eye-open]');
    const eyeClosed = toggleBtn.querySelector('[data-eye-closed]');

    const updateToggleState = () => {
        const isHidden = passwordInput.type === 'password';
        toggleBtn.setAttribute('aria-label', `${isHidden ? 'Show' : 'Hide'} password`);
        toggleBtn.setAttribute('title', `${isHidden ? 'Show' : 'Hide'} password`);
        if (eyeOpen && eyeClosed) {
            eyeOpen.classList.toggle('hidden', !isHidden);
            eyeClosed.classList.toggle('hidden', isHidden);
        }
    };

    toggleBtn.addEventListener('click', (event) => {
        event.preventDefault();
        passwordInput.type = passwordInput.type === 'password' ? 'text' : 'password';
        updateToggleState();
    });

    updateToggleState();
}

// Show professor modal
function showProfessorModal(professor = null) {
    const isEdit = !!professor;
    const title = isEdit ? 'Edit Professor' : 'Add Professor';

    const content = `
        <form id="professorForm" class="space-y-4">
            <div class="grid grid-cols-2 gap-4">
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">First Name *</label>
                    <input type="text" name="firstName" required value="${professor?.firstName || ''}"
                        class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none">
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Last Name *</label>
                    <input type="text" name="lastName" required value="${professor?.lastName || ''}"
                        class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none">
                </div>
            </div>
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Email *</label>
                <input type="email" name="email" required value="${professor?.email || ''}"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none">
                <p class="mt-1 text-xs text-red-600 hidden" data-email-error>Professor with this email already exists.</p>
            </div>
            ${!isEdit ? `
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Password *</label>
                <div class="relative" data-password-wrapper>
                    <input type="password" name="password" required minlength="8"
                        pattern="(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])[^\\s]{8,}"
                        title="8+ chars with uppercase, lowercase, number, and special character"
                        class="w-full px-3 py-2 pr-12 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none">
                    <button type="button" data-toggle-password
                        class="absolute inset-y-0 right-2 flex items-center text-gray-500 hover:text-blue-600 focus:outline-none"
                        aria-label="Show password"
                        title="Show password">
                        <svg data-eye-open class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M1.5 12C2.7 7.5 6.9 4 12 4s9.3 3.5 10.5 8c-1.2 4.5-5.4 8-10.5 8s-9.3-3.5-10.5-8z"></path>
                            <circle cx="12" cy="12" r="3" stroke-width="2"></circle>
                        </svg>
                        <svg data-eye-closed class="w-5 h-5 hidden" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3l18 18M10.584 10.59A2.5 2.5 0 0113.42 13.4m4.042.892C17.955 14.735 18.5 14 19.5 12c-1.2-4.5-5.4-8-10.5-8-1.41 0-2.76.27-4 .76"></path>
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6.122 6.13C4.046 7.327 2.568 9.305 1.5 12c1.2 4.5 5.4 8 10.5 8 1.79 0 3.48-.4 5-.96"></path>
                        </svg>
                    </button>
                </div>
                <p class="mt-1 text-xs text-gray-500">Use at least 8 characters with uppercase, lowercase, number, and special symbol.</p>
            </div>
            ` : ''}
        </form>
    `;

    const modal = showModal(title, content, {
        buttons: [
            {
                text: 'Cancel',
                className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                action: 'cancel',
                onClick: (close) => close(),
            },
            {
                text: isEdit ? 'Update' : 'Create',
                className: 'bg-blue-600 text-white hover:bg-blue-700',
                action: 'save',
                onClick: async (close) => {
                    const form = document.getElementById('professorForm');
                    const emailInput = form.querySelector('input[name="email"]');
                    if (emailInput) {
                        emailInput.dispatchEvent(new Event('input'));
                    }

                    if (!form.checkValidity()) {
                        form.reportValidity();
                        return;
                    }

                    const formData = new FormData(form);
                    const data = {
                        firstName: formData.get('firstName'),
                        lastName: formData.get('lastName'),
                        email: formData.get('email'),
                    };

                    if (!isEdit) {
                        data.password = formData.get('password');
                        data.role = 'ROLE_PROFESSOR';
                        
                        // Get department ID from userInfo or fetch from API
                        let departmentId = userInfo?.departmentId;
                        
                        if (!departmentId) {
                            try {
                                const currentUserResp = await auth.getCurrentUser();
                                const currentUser = currentUserResp?.data;
                                departmentId = currentUser?.departmentId;
                            } catch (error) {
                                console.error('Failed to fetch current user:', error);
                            }
                        }
                        
                        if (!departmentId) {
                            showToast('Unable to determine department. Please log out and log in again.', 'error');
                            return;
                        }
                        
                        data.departmentId = departmentId;
                    }

                    try {
                        if (isEdit) {
                            await hod.updateProfessor(professor.id, data);
                            showToast('Professor updated successfully', 'success');
                        } else {
                            await hod.createProfessor(data);
                            showToast('Professor created successfully', 'success');
                        }
                        loadProfessors();
                        close();
                    } catch (error) {
                        console.error('Error saving professor:', error);
                        showToast(error.message || 'Failed to save professor', 'error');
                    }
                },
            },
        ],
    });

    // Initialize password toggle once modal content is in DOM
    const formElement = document.getElementById('professorForm');
    bindPasswordToggle(formElement);

    if (formElement) {
        const emailInput = formElement.querySelector('input[name="email"]');
        const emailError = formElement.querySelector('[data-email-error]');

        const validateDuplicateEmail = () => {
            if (!emailInput) return;
            const value = emailInput.value.trim().toLowerCase();
            const isDuplicate = professors.some(prof => {
                const sameEmail = (prof.email || '').toLowerCase() === value;
                const isSameProfessor = professor && prof.id === professor.id;
                return sameEmail && !isSameProfessor;
            });

            if (isDuplicate) {
                emailInput.setCustomValidity('Professor with this email already exists');
                emailError?.classList.remove('hidden');
            } else {
                emailInput.setCustomValidity('');
                emailError?.classList.add('hidden');
            }
        };

        emailInput?.addEventListener('input', validateDuplicateEmail);
        validateDuplicateEmail();
    }
}

// Create request form
createRequestForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const formData = new FormData(createRequestForm);
    const data = {
        courseName: formData.get('courseName'),
        documentType: formData.get('documentType'),
        requiredFileExtensions: ['pdf', 'doc', 'docx'],
        deadline: new Date(formData.get('deadline')).toISOString(),
        professorId: parseInt(formData.get('professorId')),
    };

    try {
        await hod.createRequest(data);
        showToast('Request created successfully', 'success');
        createRequestForm.reset();
        loadRequests();
    } catch (error) {
        console.error('Error creating request:', error);
        showToast(error.message || 'Failed to create request', 'error');
    }
});

// Load requests
async function loadRequests() {
    try {
        const response = await hod.getRequests();
        const pageData = response.data || {};
        requests = Array.isArray(pageData) ? pageData : (pageData.content || []);
        renderRequests();
    } catch (error) {
        console.error('Error loading requests:', error);
        showToast('Failed to load requests', 'error');
        requestsTableBody.innerHTML = '<tr><td colspan="6" class="px-4 py-8 text-center text-red-600">Error loading requests</td></tr>';
    }
}

// Render requests table
function renderRequests() {
    const items = Array.isArray(requests) ? requests : [];

    if (items.length === 0) {
        requestsTableBody.innerHTML = '<tr><td colspan="6" class="px-4 py-8 text-center text-gray-500">No requests yet</td></tr>';
        return;
    }

    requestsTableBody.innerHTML = items.map(req => {
        const deadline = new Date(req.deadline);
        const isOverdue = deadline < new Date();
        const statusBadge = getStatusBadge(req.submittedDocument, isOverdue);

        return `
            <tr>
                <td class="px-4 py-3 text-sm text-gray-900">${req.courseName}</td>
                <td class="px-4 py-3 text-sm text-gray-600">${req.documentType}</td>
                <td class="px-4 py-3 text-sm text-gray-600">${req.professorName || 'N/A'}</td>
                <td class="px-4 py-3 text-sm text-gray-600">${formatDate(req.deadline)}</td>
                <td class="px-4 py-3">${statusBadge}</td>
                <td class="px-4 py-3">
                    <button 
                        class="text-blue-600 hover:text-blue-800 text-sm font-medium"
                        onclick="window.viewReport(${req.id})"
                    >
                        View Report
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

// Get status badge
function getStatusBadge(submittedDoc, isOverdue) {
    if (!submittedDoc) {
        return `<span class="badge ${isOverdue ? 'badge-danger' : 'badge-gray'}">
            ${isOverdue ? 'Not Submitted (Late)' : 'Pending'}
        </span>`;
    }

    const submittedOnTime = !submittedDoc.submittedLate;
    return `<span class="badge ${submittedOnTime ? 'badge-success' : 'badge-warning'}">
        ${submittedOnTime ? 'Submitted (On Time)' : 'Submitted (Late)'}
    </span>`;
}

// View report
window.viewReport = async (requestId) => {
    try {
        const response = await hod.getRequestDetails(requestId);
        const report = response.data;

        const content = `
            <div class="space-y-4">
                <div>
                    <h4 class="font-medium text-gray-900 mb-2">Request Details</h4>
                    <p class="text-sm text-gray-600">Course: ${report.courseName}</p>
                    <p class="text-sm text-gray-600">Type: ${report.documentType}</p>
                    <p class="text-sm text-gray-600">Deadline: ${formatDate(report.deadline)}</p>
                </div>
                <div>
                    <h4 class="font-medium text-gray-900 mb-2">Submission Status</h4>
                    ${report.submittedDocument ? `
                        <div class="bg-green-50 border border-green-200 rounded-lg p-3">
                            <p class="text-sm text-green-800">
                                <strong>Status:</strong> Submitted ${report.submittedDocument.submittedLate ? '(Late)' : '(On Time)'}
                            </p>
                            <p class="text-sm text-green-800">
                                <strong>Submitted at:</strong> ${formatDate(report.submittedDocument.submittedAt)}
                            </p>
                            <p class="text-sm text-green-800">
                                <strong>File:</strong> ${report.submittedDocument.fileName}
                            </p>
                        </div>
                    ` : `
                        <div class="bg-red-50 border border-red-200 rounded-lg p-3">
                            <p class="text-sm text-red-800">Not submitted yet</p>
                        </div>
                    `}
                </div>
            </div>
        `;

        showModal('Request Report', content, { size: 'lg' });
    } catch (error) {
        console.error('Error loading report:', error);
        showToast(error.message || 'Failed to load report', 'error');
    }
};

// View Submission Summary Report
viewReportBtn.addEventListener('click', async () => {
    try {
        showToast('Loading report...', 'info');
        const response = await hod.getSubmissionSummaryReport();
        const report = response.data;
        
        displaySubmissionReport(report);
    } catch (error) {
        console.error('Error loading submission report:', error);
        showToast(error.message || 'Failed to load submission report', 'error');
    }
});

// Download Submission Summary Report as PDF
downloadReportBtn.addEventListener('click', async () => {
    try {
        showToast('Generating PDF...', 'info');
        const response = await hod.downloadSubmissionSummaryPdf();
        
        if (!response.ok) {
            throw new Error('Failed to download PDF');
        }
        
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `department-submission-report-${new Date().getTime()}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        
        showToast('PDF downloaded successfully', 'success');
    } catch (error) {
        console.error('Error downloading PDF:', error);
        showToast(error.message || 'Failed to download PDF', 'error');
    }
});

// Display submission report in modal
function displaySubmissionReport(report) {
    const content = `
        <div class="space-y-6">
            <!-- Overall Statistics -->
            <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <h3 class="font-semibold text-blue-900 mb-3">Overall Statistics</h3>
                <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div class="text-center">
                        <div class="text-2xl font-bold text-blue-600">${report.totalProfessors}</div>
                        <div class="text-xs text-gray-600">Professors</div>
                    </div>
                    <div class="text-center">
                        <div class="text-2xl font-bold text-blue-600">${report.totalRequests}</div>
                        <div class="text-xs text-gray-600">Total Requests</div>
                    </div>
                    <div class="text-center">
                        <div class="text-2xl font-bold text-green-600">${report.totalSubmitted}</div>
                        <div class="text-xs text-gray-600">Submitted</div>
                    </div>
                    <div class="text-center">
                        <div class="text-2xl font-bold text-yellow-600">${report.totalPending}</div>
                        <div class="text-xs text-gray-600">Pending</div>
                    </div>
                </div>
                <div class="grid grid-cols-2 gap-4 mt-4">
                    <div class="text-center">
                        <div class="text-xl font-bold text-red-600">${report.totalOverdue}</div>
                        <div class="text-xs text-gray-600">Overdue</div>
                    </div>
                    <div class="text-center">
                        <div class="text-xl font-bold text-blue-600">${report.overallCompletionRate.toFixed(1)}%</div>
                        <div class="text-xs text-gray-600">Completion Rate</div>
                    </div>
                </div>
            </div>

            <!-- Professor Details -->
            <div>
                <h3 class="font-semibold text-gray-900 mb-3">Professor Submission Details</h3>
                <div class="overflow-x-auto max-h-96">
                    <table class="min-w-full divide-y divide-gray-200 text-sm">
                        <thead class="bg-gray-50 sticky top-0">
                            <tr>
                                <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Professor</th>
                                <th class="px-3 py-2 text-center text-xs font-medium text-gray-500 uppercase">Total</th>
                                <th class="px-3 py-2 text-center text-xs font-medium text-gray-500 uppercase">Submitted</th>
                                <th class="px-3 py-2 text-center text-xs font-medium text-gray-500 uppercase">Pending</th>
                                <th class="px-3 py-2 text-center text-xs font-medium text-gray-500 uppercase">Overdue</th>
                                <th class="px-3 py-2 text-center text-xs font-medium text-gray-500 uppercase">Completion</th>
                            </tr>
                        </thead>
                        <tbody class="bg-white divide-y divide-gray-200">
                            ${report.professorSummaries.map(prof => `
                                <tr class="hover:bg-gray-50">
                                    <td class="px-3 py-2">
                                        <div class="font-medium text-gray-900">${prof.professorName}</div>
                                        <div class="text-xs text-gray-500">${prof.professorEmail}</div>
                                    </td>
                                    <td class="px-3 py-2 text-center">${prof.totalRequests}</td>
                                    <td class="px-3 py-2 text-center">
                                        <span class="text-green-600 font-medium">${prof.submittedRequests}</span>
                                    </td>
                                    <td class="px-3 py-2 text-center">
                                        <span class="text-yellow-600 font-medium">${prof.pendingRequests}</span>
                                    </td>
                                    <td class="px-3 py-2 text-center">
                                        <span class="text-red-600 font-medium">${prof.overdueRequests}</span>
                                    </td>
                                    <td class="px-3 py-2 text-center">
                                        <div class="flex items-center justify-center">
                                            <div class="w-16 bg-gray-200 rounded-full h-2 mr-2">
                                                <div class="bg-blue-600 h-2 rounded-full" style="width: ${prof.completionRate}%"></div>
                                            </div>
                                            <span class="text-xs font-medium">${prof.completionRate.toFixed(1)}%</span>
                                        </div>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Report Footer -->
            <div class="text-xs text-gray-500 text-center pt-4 border-t">
                Generated on ${formatDate(report.generatedAt)} by ${report.generatedBy}
            </div>
        </div>
    `;

    showModal('Department Submission Report', content, { size: 'xl' });
}
