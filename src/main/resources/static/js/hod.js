/**
 * HOD Dashboard
 */

import { hod, deanship, getUserInfo, isAuthenticated, redirectToLogin, clearAuthData } from './api.js';
import { showToast, showModal, formatDate } from './ui.js';
import { FileExplorer } from './file-explorer.js';

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
let requests = [];
let academicYears = [];
let semesters = [];
let selectedAcademicYear = null;
let selectedSemester = null;
let fileExplorerInstance = null;

// DOM Elements
const hodName = document.getElementById('hodName');
const logoutBtn = document.getElementById('logoutBtn');
const academicYearSelect = document.getElementById('academicYearSelect');
const semesterSelect = document.getElementById('semesterSelect');
const dashboardOverview = document.getElementById('dashboardOverview');
const submissionStatusSection = document.getElementById('submissionStatusSection');
const submissionStatusTableBody = document.getElementById('submissionStatusTableBody');
const fileExplorerSection = document.getElementById('fileExplorerSection');
const requestsTableBody = document.getElementById('requestsTableBody');
const viewReportBtn = document.getElementById('viewReportBtn');
const downloadReportBtn = document.getElementById('downloadReportBtn');
const filterCourse = document.getElementById('filterCourse');
const filterDocType = document.getElementById('filterDocType');
const filterStatus = document.getElementById('filterStatus');

// Initialize
hodName.textContent = userInfo.fullName;
loadAcademicYears();
loadLegacyRequests();
initializeFileExplorer();
initializeTabSwitching();
initializeReportButtons();

// Tab Switching
function initializeTabSwitching() {
    const navTabs = document.querySelectorAll('.nav-tab');
    navTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const tabName = tab.getAttribute('data-tab');
            switchTab(tabName);
        });
    });
}

// Report Buttons
function initializeReportButtons() {
    const viewReportBtnTab = document.getElementById('viewReportBtnTab');
    const downloadReportBtnTab = document.getElementById('downloadReportBtnTab');
    
    if (viewReportBtnTab) {
        viewReportBtnTab.addEventListener('click', viewReport);
    }
    if (downloadReportBtnTab) {
        downloadReportBtnTab.addEventListener('click', downloadReport);
    }
    
    // Also handle the buttons in submission status section
    if (viewReportBtn) {
        viewReportBtn.addEventListener('click', viewReport);
    }
    if (downloadReportBtn) {
        downloadReportBtn.addEventListener('click', downloadReport);
    }
}

async function viewReport() {
    if (!selectedSemester) {
        showToast('Please select a semester first', 'warning');
        return;
    }
    
    try {
        const response = await hod.getProfessorSubmissionReport(selectedSemester);
        const report = response.data;
        
        // Display report in a modal or new section
        displayReportModal(report);
    } catch (error) {
        console.error('Error viewing report:', error);
        showToast('Failed to load report', 'error');
    }
}

async function downloadReport() {
    if (!selectedSemester) {
        showToast('Please select a semester first', 'warning');
        return;
    }
    
    try {
        showToast('Generating PDF report...', 'info');
        const blob = await hod.downloadProfessorSubmissionReportPdf(selectedSemester);
        
        // Create download link
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `professor-submission-report-${selectedSemester}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        
        showToast('Report downloaded successfully', 'success');
    } catch (error) {
        console.error('Error downloading report:', error);
        showToast('Failed to download report', 'error');
    }
}

function switchTab(tabName) {
    // Update nav tabs
    document.querySelectorAll('.nav-tab').forEach(tab => {
        if (tab.getAttribute('data-tab') === tabName) {
            tab.classList.add('active', 'border-blue-600', 'text-blue-600');
            tab.classList.remove('border-transparent', 'text-gray-500');
        } else {
            tab.classList.remove('active', 'border-blue-600', 'text-blue-600');
            tab.classList.add('border-transparent', 'text-gray-500');
        }
    });
    
    // Update tab content
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.add('hidden');
    });
    
    const activeTab = document.getElementById(`${tabName}-tab`);
    if (activeTab) {
        activeTab.classList.remove('hidden');
    }
    
    // Load data for specific tabs
    if (tabName === 'submission-status' && selectedSemester) {
        loadSubmissionStatus();
    } else if (tabName === 'file-explorer' && selectedSemester) {
        loadFileExplorerData();
    }
}

// Logout
logoutBtn.addEventListener('click', () => {
    clearAuthData();
    showToast('Logged out successfully', 'success');
    redirectToLogin();
});

// Load academic years
async function loadAcademicYears() {
    try {
        const response = await hod.getAcademicYears();
        academicYears = response.data || [];
        
        if (academicYears.length === 0) {
            academicYearSelect.innerHTML = '<option value="">No academic years available</option>';
            showToast('No academic years found. Please contact Deanship to set up academic structure.', 'warning');
            return;
        }
        
        // Find active academic year or use the first one
        const activeYear = academicYears.find(year => year.isActive) || academicYears[0];
        
        academicYearSelect.innerHTML = academicYears.map(year => 
            `<option value="${year.id}" ${year.id === activeYear.id ? 'selected' : ''}>
                ${year.yearCode}
            </option>`
        ).join('');
        
        selectedAcademicYear = activeYear.id;
        await loadSemesters(activeYear.id);
    } catch (error) {
        console.error('Error loading academic years:', error);
        showToast('Failed to load academic years', 'error');
        academicYearSelect.innerHTML = '<option value="">Error loading academic years</option>';
    }
}

// Load semesters for selected academic year
async function loadSemesters(academicYearId) {
    try {
        semesterSelect.disabled = true;
        semesterSelect.innerHTML = '<option value="">Loading semesters...</option>';
        
        const year = academicYears.find(y => y.id === academicYearId);
        if (!year || !year.semesters) {
            semesterSelect.innerHTML = '<option value="">No semesters available</option>';
            return;
        }
        
        semesters = year.semesters;
        
        if (semesters.length === 0) {
            semesterSelect.innerHTML = '<option value="">No semesters available</option>';
            return;
        }
        
        // Sort semesters by type (FIRST, SECOND, SUMMER)
        const semesterOrder = { 'FIRST': 1, 'SECOND': 2, 'SUMMER': 3 };
        semesters.sort((a, b) => semesterOrder[a.type] - semesterOrder[b.type]);
        
        semesterSelect.innerHTML = semesters.map(semester => 
            `<option value="${semester.id}">
                ${formatSemesterName(semester.type)}
            </option>`
        ).join('');
        
        semesterSelect.disabled = false;
        
        // Auto-select first semester and load data
        if (semesters.length > 0) {
            selectedSemester = semesters[0].id;
            await loadDashboardData();
        }
    } catch (error) {
        console.error('Error loading semesters:', error);
        showToast('Failed to load semesters', 'error');
        semesterSelect.innerHTML = '<option value="">Error loading semesters</option>';
    }
}

// Format semester name
function formatSemesterName(type) {
    const names = {
        'FIRST': 'First Semester (Fall)',
        'SECOND': 'Second Semester (Spring)',
        'SUMMER': 'Summer Semester'
    };
    return names[type] || type;
}

// Academic year change handler
academicYearSelect.addEventListener('change', async (e) => {
    selectedAcademicYear = parseInt(e.target.value);
    if (selectedAcademicYear) {
        await loadSemesters(selectedAcademicYear);
    }
});

// Semester change handler
semesterSelect.addEventListener('change', async (e) => {
    selectedSemester = parseInt(e.target.value);
    if (selectedSemester) {
        await loadDashboardData();
    }
});

// Load dashboard data for selected semester
async function loadDashboardData() {
    if (!selectedSemester) {
        return;
    }
    
    try {
        // Show loading state
        dashboardOverview.classList.remove('hidden');
        submissionStatusSection.classList.remove('hidden');
        fileExplorerSection.classList.remove('hidden');
        
        // Load dashboard overview
        await loadDashboardOverview();
        
        // Load submission status
        await loadSubmissionStatus();
        
        // Load file explorer
        await loadFileExplorerData();
    } catch (error) {
        console.error('Error loading dashboard data:', error);
        showToast('Failed to load dashboard data', 'error');
    }
}

// Load dashboard overview
async function loadDashboardOverview() {
    try {
        const response = await hod.getDashboardOverview(selectedSemester);
        const overview = response.data;
        
        // Update overview cards with data from API
        document.getElementById('totalProfessors').textContent = overview.totalProfessors || 0;
        document.getElementById('totalCourses').textContent = overview.totalCourses || 0;
        
        // Get submission statistics from the nested object
        const stats = overview.submissionStatistics || {};
        document.getElementById('submittedCount').textContent = stats.submittedDocuments || 0;
        document.getElementById('missingCount').textContent = stats.missingDocuments || 0;
        document.getElementById('overdueCount').textContent = stats.overdueDocuments || 0;
    } catch (error) {
        console.error('Error loading dashboard overview:', error);
        // Set default values on error
        document.getElementById('totalProfessors').textContent = '0';
        document.getElementById('totalCourses').textContent = '0';
        document.getElementById('submittedCount').textContent = '0';
        document.getElementById('missingCount').textContent = '0';
        document.getElementById('overdueCount').textContent = '0';
    }
}

// Load submission status
async function loadSubmissionStatus() {
    try {
        const filters = {
            courseCode: filterCourse?.value || undefined,
            documentType: filterDocType?.value || undefined,
            status: filterStatus?.value || undefined
        };
        
        // Remove undefined values
        Object.keys(filters).forEach(key => filters[key] === undefined && delete filters[key]);
        
        const response = await hod.getSubmissionStatus(selectedSemester, filters);
        const report = response.data;
        
        renderSubmissionStatus(report);
        populateCourseFilter(report);
    } catch (error) {
        console.error('Error loading submission status:', error);
        submissionStatusTableBody.innerHTML = 
            '<tr><td colspan="5" class="px-4 py-8 text-center text-red-600">Error loading submission status</td></tr>';
    }
}

// Render submission status table
function renderSubmissionStatus(report) {
    if (!report || !report.rows || report.rows.length === 0) {
        submissionStatusTableBody.innerHTML = 
            '<tr><td colspan="5" class="px-4 py-8 text-center text-gray-500">No submission data available for this semester</td></tr>';
        return;
    }
    
    const rows = [];
    report.rows.forEach(row => {
        // Create a row for each document type
        Object.entries(row.documentStatuses || {}).forEach(([docType, status]) => {
            rows.push({
                professorName: row.professorName,
                courseCode: row.courseCode,
                courseName: row.courseName,
                documentType: docType,
                status: status.status,
                deadline: status.deadline
            });
        });
    });
    
    submissionStatusTableBody.innerHTML = rows.map(row => {
        const statusBadge = getStatusBadgeNew(row.status);
        return `
            <tr class="hover:bg-gray-50">
                <td class="px-4 py-3 text-sm text-gray-900">${row.professorName}</td>
                <td class="px-4 py-3 text-sm text-gray-600">
                    <div>${row.courseCode}</div>
                    <div class="text-xs text-gray-500">${row.courseName}</div>
                </td>
                <td class="px-4 py-3 text-sm text-gray-600">${formatDocumentType(row.documentType)}</td>
                <td class="px-4 py-3">${statusBadge}</td>
                <td class="px-4 py-3 text-sm text-gray-600">${row.deadline ? formatDate(row.deadline) : 'No deadline'}</td>
            </tr>
        `;
    }).join('');
}

// Get status badge for new system
function getStatusBadgeNew(status) {
    const badges = {
        'UPLOADED': '<span class="badge badge-success">Uploaded</span>',
        'NOT_UPLOADED': '<span class="badge badge-gray">Not Uploaded</span>',
        'OVERDUE': '<span class="badge badge-danger">Overdue</span>'
    };
    return badges[status] || '<span class="badge badge-gray">Unknown</span>';
}

// Format document type
function formatDocumentType(type) {
    const types = {
        'SYLLABUS': 'Syllabus',
        'EXAM': 'Exam',
        'ASSIGNMENT': 'Assignment',
        'PROJECT_DOCS': 'Project Docs',
        'LECTURE_NOTES': 'Lecture Notes',
        'OTHER': 'Other'
    };
    return types[type] || type;
}

// Populate course filter
function populateCourseFilter(report) {
    if (!filterCourse || !report || !report.rows) return;
    
    const courses = new Set();
    report.rows.forEach(row => {
        if (row.courseCode) {
            courses.add(`${row.courseCode}|${row.courseName}`);
        }
    });
    
    const options = Array.from(courses).map(course => {
        const [code, name] = course.split('|');
        return `<option value="${code}">${code} - ${name}</option>`;
    }).join('');
    
    filterCourse.innerHTML = '<option value="">All Courses</option>' + options;
}

// Filter change handlers
if (filterCourse) {
    filterCourse.addEventListener('change', () => loadSubmissionStatus());
}
if (filterDocType) {
    filterDocType.addEventListener('change', () => loadSubmissionStatus());
}
if (filterStatus) {
    filterStatus.addEventListener('change', () => loadSubmissionStatus());
}

// ============================================================================
// FILE EXPLORER
// ============================================================================

/**
 * Initialize file explorer component
 */
function initializeFileExplorer() {
    try {
        fileExplorerInstance = new FileExplorer('hodFileExplorer', {
            readOnly: true
        });
        
        // Make it globally accessible for event handlers
        window.fileExplorerInstance = fileExplorerInstance;
    } catch (error) {
        console.error('Error initializing file explorer:', error);
        showToast('Failed to initialize file explorer', 'error');
    }
}

/**
 * Load file explorer data for selected semester
 */
async function loadFileExplorerData() {
    if (!selectedAcademicYear || !selectedSemester || !fileExplorerInstance) {
        return;
    }
    
    try {
        await fileExplorerInstance.loadRoot(selectedAcademicYear, selectedSemester);
    } catch (error) {
        console.error('Error loading file explorer data:', error);
        showToast('Failed to load file explorer', 'error');
    }
}

// Load legacy requests (kept for backward compatibility)
async function loadLegacyRequests() {
    try {
        const response = await hod.getRequests();
        const pageData = response.data || {};
        requests = Array.isArray(pageData) ? pageData : (pageData.content || []);
        renderLegacyRequests();
    } catch (error) {
        console.error('Error loading legacy requests:', error);
        requestsTableBody.innerHTML = '<tr><td colspan="6" class="px-4 py-8 text-center text-red-600">Error loading requests</td></tr>';
    }
}

// Render legacy requests table
function renderLegacyRequests() {
    const items = Array.isArray(requests) ? requests : [];

    if (items.length === 0) {
        requestsTableBody.innerHTML = '<tr><td colspan="6" class="px-4 py-8 text-center text-gray-500">No legacy requests</td></tr>';
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
                        onclick="window.viewLegacyReport(${req.id})"
                    >
                        View
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

// View legacy report
window.viewLegacyReport = async (requestId) => {
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

        showModal('Legacy Request Report', content, { size: 'lg' });
    } catch (error) {
        console.error('Error loading report:', error);
        showToast(error.message || 'Failed to load report', 'error');
    }
};

// View Semester-based Submission Report
viewReportBtn.addEventListener('click', async () => {
    if (!selectedSemester) {
        showToast('Please select a semester first', 'warning');
        return;
    }
    
    try {
        showToast('Loading report...', 'info');
        const response = await hod.getProfessorSubmissionReport(selectedSemester);
        const report = response.data;
        
        displaySubmissionReport(report);
    } catch (error) {
        console.error('Error loading submission report:', error);
        showToast(error.message || 'Failed to load submission report', 'error');
    }
});

// Download Semester-based Report as PDF
downloadReportBtn.addEventListener('click', async () => {
    if (!selectedSemester) {
        showToast('Please select a semester first', 'warning');
        return;
    }
    
    try {
        showToast('Generating PDF...', 'info');
        const response = await hod.exportReportToPdf(selectedSemester);
        
        if (!response.ok) {
            throw new Error('Failed to download PDF');
        }
        
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        
        const semester = semesters.find(s => s.id === selectedSemester);
        const semesterName = semester ? formatSemesterName(semester.type).replace(/\s+/g, '-') : 'semester';
        a.download = `professor-submission-report-${semesterName}-${new Date().getTime()}.pdf`;
        
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
    if (!report || !report.statistics) {
        showToast('No report data available', 'warning');
        return;
    }
    
    const stats = report.statistics;
    const rows = report.rows || [];
    
    const content = `
        <div class="space-y-6">
            <!-- Overall Statistics -->
            <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <h3 class="font-semibold text-blue-900 mb-3">Overall Statistics</h3>
                <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div class="text-center">
                        <div class="text-2xl font-bold text-blue-600">${stats.totalProfessors || 0}</div>
                        <div class="text-xs text-gray-600">Professors</div>
                    </div>
                    <div class="text-center">
                        <div class="text-2xl font-bold text-blue-600">${stats.totalCourses || 0}</div>
                        <div class="text-xs text-gray-600">Courses</div>
                    </div>
                    <div class="text-center">
                        <div class="text-2xl font-bold text-green-600">${stats.submittedDocuments || 0}</div>
                        <div class="text-xs text-gray-600">Submitted</div>
                    </div>
                    <div class="text-center">
                        <div class="text-2xl font-bold text-yellow-600">${stats.missingDocuments || 0}</div>
                        <div class="text-xs text-gray-600">Missing</div>
                    </div>
                </div>
                <div class="grid grid-cols-2 gap-4 mt-4">
                    <div class="text-center">
                        <div class="text-xl font-bold text-red-600">${stats.overdueDocuments || 0}</div>
                        <div class="text-xs text-gray-600">Overdue</div>
                    </div>
                    <div class="text-center">
                        <div class="text-xl font-bold text-blue-600">${stats.totalRequiredDocuments || 0}</div>
                        <div class="text-xs text-gray-600">Total Required</div>
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
                                <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase">Course</th>
                                <th class="px-3 py-2 text-center text-xs font-medium text-gray-500 uppercase">Document Types</th>
                            </tr>
                        </thead>
                        <tbody class="bg-white divide-y divide-gray-200">
                            ${rows.length > 0 ? rows.map(row => {
                                const docStatuses = Object.entries(row.documentStatuses || {}).map(([type, status]) => {
                                    const badge = getStatusBadgeNew(status.status);
                                    return `<div class="flex items-center justify-between mb-1">
                                        <span class="text-xs">${formatDocumentType(type)}</span>
                                        ${badge}
                                    </div>`;
                                }).join('');
                                
                                return `
                                    <tr class="hover:bg-gray-50">
                                        <td class="px-3 py-2">
                                            <div class="font-medium text-gray-900">${row.professorName}</div>
                                        </td>
                                        <td class="px-3 py-2">
                                            <div class="font-medium text-gray-700">${row.courseCode}</div>
                                            <div class="text-xs text-gray-500">${row.courseName}</div>
                                        </td>
                                        <td class="px-3 py-2">
                                            ${docStatuses || '<span class="text-xs text-gray-500">No documents</span>'}
                                        </td>
                                    </tr>
                                `;
                            }).join('') : `
                                <tr>
                                    <td colspan="3" class="px-3 py-8 text-center text-gray-500">
                                        No submission data available
                                    </td>
                                </tr>
                            `}
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Report Footer -->
            <div class="text-xs text-gray-500 text-center pt-4 border-t">
                Report for ${report.semesterName || 'Selected Semester'} - ${report.departmentName || 'Department'}
            </div>
        </div>
    `;

    showModal('Professor Submission Report', content, { size: 'xl' });
}
