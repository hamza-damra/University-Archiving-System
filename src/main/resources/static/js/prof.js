/**
 * Professor Dashboard
 */

import { professor, getUserInfo, isAuthenticated, redirectToLogin, clearAuthData } from './api.js';
import { showToast, showModal, formatDate, getTimeUntil, isOverdue, isValidFileExtension, formatFileSize } from './ui.js';

// Check authentication
if (!isAuthenticated()) {
    redirectToLogin();
}

const userInfo = getUserInfo();
if (userInfo.role !== 'ROLE_PROFESSOR') {
    showToast('Access denied - Professor privileges required', 'error');
    setTimeout(() => redirectToLogin(), 2000);
}

// State
let requests = [];
let notifications = [];
let currentFilter = 'all';
let currentPage = 0;
let totalPages = 0;
let pageSize = 9;
let totalElements = 0;

// DOM Elements
const professorName = document.getElementById('professorName');
const logoutBtn = document.getElementById('logoutBtn');
const notificationsBtn = document.getElementById('notificationsBtn');
const notificationBadge = document.getElementById('notificationBadge');
const notificationsDropdown = document.getElementById('notificationsDropdown');
const closeNotificationsDropdown = document.getElementById('closeNotificationsDropdown');
const notificationsList = document.getElementById('notificationsList');
const requestsGrid = document.getElementById('requestsGrid');
const emptyState = document.getElementById('emptyState');
const filterBtns = document.querySelectorAll('.filter-btn');

// Initialize
professorName.textContent = userInfo.fullName;
loadRequests();
loadNotifications();

// Poll notifications every 30 seconds
setInterval(loadNotifications, 30000);

// Logout
logoutBtn.addEventListener('click', () => {
    clearAuthData();
    showToast('Logged out successfully', 'success');
    redirectToLogin();
});

// Notifications dropdown toggle
notificationsBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    notificationsDropdown.classList.toggle('hidden');
    if (!notificationsDropdown.classList.contains('hidden')) {
        loadNotifications();
    }
});

closeNotificationsDropdown.addEventListener('click', () => {
    notificationsDropdown.classList.add('hidden');
});

// Close dropdown when clicking outside
document.addEventListener('click', (e) => {
    if (!notificationsDropdown.classList.contains('hidden') && 
        !notificationsDropdown.contains(e.target) && 
        !notificationsBtn.contains(e.target)) {
        notificationsDropdown.classList.add('hidden');
    }
});

// Filter buttons
filterBtns.forEach(btn => {
    btn.addEventListener('click', () => {
        filterBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        currentFilter = btn.dataset.filter;
        renderRequests();
    });
});

// Load requests with pagination
async function loadRequests(page = 0) {
    try {
        const response = await professor.getRequests(page, pageSize);
        const pageData = response.data;
        requests = pageData.content || [];
        currentPage = pageData.number || 0;
        totalPages = pageData.totalPages || 0;
        totalElements = pageData.totalElements || 0;
        renderRequests();
        renderPagination();
    } catch (error) {
        console.error('Error loading requests:', error);
        showToast('Failed to load requests', 'error');
        requestsGrid.innerHTML = '<p class="text-red-600 text-sm col-span-full text-center">Error loading requests</p>';
    }
}

// Render requests
function renderRequests() {
    let filtered = [...requests];

    // Apply filter
    if (currentFilter === 'pending') {
        filtered = filtered.filter(req => !req.submittedDocument);
    } else if (currentFilter === 'submitted') {
        filtered = filtered.filter(req => req.submittedDocument);
    } else if (currentFilter === 'overdue') {
        filtered = filtered.filter(req => !req.submittedDocument && isOverdue(req.deadline));
    }

    if (filtered.length === 0) {
        requestsGrid.innerHTML = '';
        emptyState.classList.remove('hidden');
        return;
    }

    emptyState.classList.add('hidden');
    requestsGrid.innerHTML = filtered.map(req => createRequestCard(req)).join('');
}

// Create request card
function createRequestCard(req) {
    const deadline = new Date(req.deadline);
    const isPastDue = isOverdue(deadline);
    const isSubmitted = !!req.submittedDocument;
    const timeUntil = getTimeUntil(deadline);

    let statusClass = 'badge-gray';
    let statusText = 'Pending';

    if (isSubmitted) {
        statusClass = req.submittedDocument.submittedLate ? 'badge-warning' : 'badge-success';
        statusText = req.submittedDocument.submittedLate ? 'Submitted (Late)' : 'Submitted';
    } else if (isPastDue) {
        statusClass = 'badge-danger';
        statusText = 'Overdue';
    }

    return `
        <div class="border border-gray-200 rounded-lg p-4 card-hover">
            <div class="flex justify-between items-start mb-3">
                <div>
                    <h3 class="font-semibold text-gray-900">${req.courseName}</h3>
                    <p class="text-sm text-gray-600">${req.documentType}</p>
                </div>
                <span class="badge ${statusClass}">${statusText}</span>
            </div>

            <div class="space-y-2 mb-4">
                <div class="flex items-center text-sm text-gray-600">
                    <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                    </svg>
                    ${formatDate(deadline)}
                </div>
                <div class="flex items-center text-sm ${isPastDue ? 'text-red-600' : 'text-blue-600'}">
                    <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                    ${timeUntil}
                </div>
                <div class="flex items-center text-sm text-gray-600">
                    <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                    </svg>
                    Allowed: ${req.requiredFileExtensions}
                </div>
                ${isSubmitted ? `
                    <div class="flex items-center text-sm text-green-600">
                        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                        </svg>
                        ${req.submittedDocument.fileName}
                    </div>
                ` : ''}
            </div>

            <button 
                class="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 transition font-medium text-sm"
                onclick="window.uploadDocument(${req.id}, '${req.requiredFileExtensions}')"
            >
                ${isSubmitted ? 'Replace Document' : 'Upload Document'}
            </button>
        </div>
    `;
}

// Upload document
window.uploadDocument = (requestId, allowedExtensions) => {
    const content = `
        <form id="uploadForm" class="space-y-4">
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                    Select Document
                </label>
                <div class="file-upload-area" id="fileUploadArea">
                    <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
                    </svg>
                    <p class="mt-2 text-sm text-gray-600">Click to upload or drag and drop</p>
                    <p class="text-xs text-gray-500 mt-1">Allowed: ${allowedExtensions}</p>
                    <input 
                        type="file" 
                        id="fileInput" 
                        name="file" 
                        required 
                        class="hidden"
                        accept=".${allowedExtensions.split(',').join(',.')}"
                    >
                </div>
                <div id="fileInfo" class="mt-2 hidden">
                    <div class="flex items-center justify-between p-2 bg-gray-50 rounded">
                        <span id="fileName" class="text-sm text-gray-700"></span>
                        <span id="fileSize" class="text-xs text-gray-500"></span>
                    </div>
                </div>
                <p id="fileError" class="text-red-600 text-sm mt-1 hidden"></p>
            </div>
            <div id="uploadProgress" class="hidden">
                <div class="progress-bar">
                    <div id="progressFill" class="progress-bar-fill" style="width: 0%"></div>
                </div>
                <p id="progressText" class="text-sm text-gray-600 mt-1 text-center">Uploading...</p>
            </div>
        </form>
    `;

    const modal = showModal('Upload Document', content, {
        size: 'md',
        buttons: [
            {
                text: 'Cancel',
                className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                action: 'cancel',
                onClick: (close) => close(),
            },
            {
                text: 'Upload',
                className: 'bg-blue-600 text-white hover:bg-blue-700',
                action: 'upload',
                onClick: async (close) => {
                    const fileInput = document.getElementById('fileInput');
                    const fileError = document.getElementById('fileError');
                    const file = fileInput.files[0];

                    if (!file) {
                        fileError.textContent = 'Please select a file';
                        fileError.classList.remove('hidden');
                        return;
                    }

                    // Validate file extension
                    if (!isValidFileExtension(file.name, allowedExtensions)) {
                        fileError.textContent = `Invalid file type. Allowed: ${allowedExtensions}`;
                        fileError.classList.remove('hidden');
                        return;
                    }

                    // Validate file size (10MB max)
                    if (file.size > 10 * 1024 * 1024) {
                        fileError.textContent = 'File size must be less than 10MB';
                        fileError.classList.remove('hidden');
                        return;
                    }

                    const formData = new FormData();
                    formData.append('file', file);

                    // Show progress
                    const uploadProgress = document.getElementById('uploadProgress');
                    const progressFill = document.getElementById('progressFill');
                    const progressText = document.getElementById('progressText');
                    uploadProgress.classList.remove('hidden');

                    // Disable upload button
                    const uploadBtn = modal.querySelector('[data-action="upload"]');
                    uploadBtn.disabled = true;
                    uploadBtn.textContent = 'Uploading...';

                    try {
                        await professor.submitDocument(
                            requestId,
                            formData,
                            (percent) => {
                                progressFill.style.width = `${percent}%`;
                                progressText.textContent = `Uploading... ${Math.round(percent)}%`;
                            }
                        );

                        showToast('Document uploaded successfully', 'success');
                        loadRequests();
                        close();
                    } catch (error) {
                        console.error('Upload error:', error);
                        fileError.textContent = error.message || 'Upload failed';
                        fileError.classList.remove('hidden');
                        uploadBtn.disabled = false;
                        uploadBtn.textContent = 'Upload';
                        uploadProgress.classList.add('hidden');
                    }
                },
            },
        ],
    });

    // File input handling
    const fileInput = document.getElementById('fileInput');
    const fileUploadArea = document.getElementById('fileUploadArea');
    const fileInfo = document.getElementById('fileInfo');
    const fileName = document.getElementById('fileName');
    const fileSize = document.getElementById('fileSize');
    const fileError = document.getElementById('fileError');

    fileUploadArea.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) {
            fileName.textContent = file.name;
            fileSize.textContent = formatFileSize(file.size);
            fileInfo.classList.remove('hidden');
            fileError.classList.add('hidden');
        }
    });

    // Drag and drop
    fileUploadArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        fileUploadArea.classList.add('dragover');
    });

    fileUploadArea.addEventListener('dragleave', () => {
        fileUploadArea.classList.remove('dragover');
    });

    fileUploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        fileUploadArea.classList.remove('dragover');
        
        const file = e.dataTransfer.files[0];
        if (file) {
            const dataTransfer = new DataTransfer();
            dataTransfer.items.add(file);
            fileInput.files = dataTransfer.files;
            
            fileName.textContent = file.name;
            fileSize.textContent = formatFileSize(file.size);
            fileInfo.classList.remove('hidden');
        }
    });
};

// Load notifications
async function loadNotifications() {
    try {
        const response = await professor.getNotifications();
        // Sort notifications by date (newest first)
        notifications = (response.data || []).sort((a, b) => 
            new Date(b.createdAt) - new Date(a.createdAt)
        );
        
        const unseenCount = notifications.filter(n => !n.seen).length;
        if (unseenCount > 0) {
            notificationBadge.classList.remove('hidden');
        } else {
            notificationBadge.classList.add('hidden');
        }

        renderNotifications();
    } catch (error) {
        console.error('Error loading notifications:', error);
    }
}

// Render notifications
function renderNotifications() {
    if (notifications.length === 0) {
        notificationsList.innerHTML = '<div class="p-8 text-center"><p class="text-gray-500 text-sm">No notifications</p></div>';
        return;
    }

    notificationsList.innerHTML = notifications.map(notif => `
        <div class="p-4 border-b border-gray-100 ${notif.seen ? 'bg-white' : 'bg-blue-50'} cursor-pointer hover:bg-gray-50 transition"
            onclick="window.markNotificationSeen(${notif.id})">
            <div class="flex items-start gap-3">
                <div class="flex-shrink-0 mt-1">
                    <div class="w-2 h-2 ${notif.seen ? 'bg-gray-300' : 'bg-blue-600'} rounded-full"></div>
                </div>
                <div class="flex-1 min-w-0">
                    <p class="text-sm ${notif.seen ? 'text-gray-700' : 'text-gray-900 font-medium'}">${notif.message}</p>
                    <p class="text-xs text-gray-500 mt-1">${formatDate(notif.createdAt)}</p>
                </div>
            </div>
        </div>
    `).join('');
}

// Mark notification as seen
window.markNotificationSeen = async (notificationId) => {
    try {
        await professor.markNotificationSeen(notificationId);
        loadNotifications();
    } catch (error) {
        console.error('Error marking notification as seen:', error);
    }
};

// Render pagination
function renderPagination() {
    const paginationContainer = document.getElementById('paginationContainer');
    const pageInfo = document.getElementById('pageInfo');
    const paginationButtons = document.getElementById('paginationButtons');
    
    // Hide pagination if no requests or filtering
    if (totalElements === 0 || currentFilter !== 'all') {
        paginationContainer.classList.add('hidden');
        return;
    }
    
    paginationContainer.classList.remove('hidden');
    
    // Update page info
    const start = currentPage * pageSize + 1;
    const end = Math.min((currentPage + 1) * pageSize, totalElements);
    pageInfo.textContent = `${start}-${end} of ${totalElements}`;
    
    // Clear existing buttons
    paginationButtons.innerHTML = '';
    
    // Previous button
    const prevBtn = document.createElement('button');
    prevBtn.innerHTML = `
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
        </svg>
    `;
    prevBtn.className = `px-3 py-2 rounded-md ${currentPage === 0 ? 'bg-gray-100 text-gray-400 cursor-not-allowed' : 'bg-white text-gray-700 hover:bg-gray-50 border border-gray-300'}`;
    prevBtn.disabled = currentPage === 0;
    prevBtn.onclick = () => {
        if (currentPage > 0) {
            loadRequests(currentPage - 1);
        }
    };
    paginationButtons.appendChild(prevBtn);
    
    // Page numbers
    const maxVisiblePages = 5;
    let startPage = Math.max(0, currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages - 1, startPage + maxVisiblePages - 1);
    
    if (endPage - startPage < maxVisiblePages - 1) {
        startPage = Math.max(0, endPage - maxVisiblePages + 1);
    }
    
    if (startPage > 0) {
        const firstBtn = createPageButton(0, '1');
        paginationButtons.appendChild(firstBtn);
        
        if (startPage > 1) {
            const dots = document.createElement('span');
            dots.textContent = '...';
            dots.className = 'px-3 py-2 text-gray-500';
            paginationButtons.appendChild(dots);
        }
    }
    
    for (let i = startPage; i <= endPage; i++) {
        const pageBtn = createPageButton(i, (i + 1).toString());
        paginationButtons.appendChild(pageBtn);
    }
    
    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            const dots = document.createElement('span');
            dots.textContent = '...';
            dots.className = 'px-3 py-2 text-gray-500';
            paginationButtons.appendChild(dots);
        }
        
        const lastBtn = createPageButton(totalPages - 1, totalPages.toString());
        paginationButtons.appendChild(lastBtn);
    }
    
    // Next button
    const nextBtn = document.createElement('button');
    nextBtn.innerHTML = `
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
        </svg>
    `;
    nextBtn.className = `px-3 py-2 rounded-md ${currentPage === totalPages - 1 ? 'bg-gray-100 text-gray-400 cursor-not-allowed' : 'bg-white text-gray-700 hover:bg-gray-50 border border-gray-300'}`;
    nextBtn.disabled = currentPage === totalPages - 1;
    nextBtn.onclick = () => {
        if (currentPage < totalPages - 1) {
            loadRequests(currentPage + 1);
        }
    };
    paginationButtons.appendChild(nextBtn);
}

function createPageButton(pageNum, label) {
    const btn = document.createElement('button');
    btn.textContent = label;
    btn.className = pageNum === currentPage 
        ? 'px-3 py-2 rounded-md bg-blue-600 text-white font-medium'
        : 'px-3 py-2 rounded-md bg-white text-gray-700 hover:bg-gray-50 border border-gray-300';
    btn.onclick = () => {
        if (pageNum !== currentPage) {
            loadRequests(pageNum);
        }
    };
    return btn;
}
