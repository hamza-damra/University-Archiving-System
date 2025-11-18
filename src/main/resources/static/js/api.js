/**
 * API Service
 * Centralized API calls with token management and error handling
 */

// Default base URL - adjust this to match your backend
const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Get authentication token from localStorage
 */
function getToken() {
    return localStorage.getItem('token');
}

/**
 * Get user info from localStorage
 */
export function getUserInfo() {
    const userInfo = localStorage.getItem('userInfo');
    return userInfo ? JSON.parse(userInfo) : null;
}

/**
 * Save authentication data to localStorage
 */
export function saveAuthData(token, userInfo) {
    localStorage.setItem('token', token);
    localStorage.setItem('userInfo', JSON.stringify(userInfo));
}

/**
 * Clear authentication data from localStorage
 */
export function clearAuthData() {
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
}

/**
 * Check if user is authenticated
 */
export function isAuthenticated() {
    return !!getToken();
}

/**
 * Redirect to login page
 */
export function redirectToLogin() {
    clearAuthData();
    window.location.href = '/index.html';
}

/**
 * Make an API request with authentication
 * @param {string} endpoint - API endpoint (relative to base URL)
 * @param {object} options - Fetch options
 * @returns {Promise} Response data
 */
export async function apiRequest(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    // Add authorization header if token exists
    const token = getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    // Remove Content-Type for FormData (browser sets it with boundary)
    if (options.body instanceof FormData) {
        delete headers['Content-Type'];
    }

    const config = {
        ...options,
        headers,
    };

    try {
        const response = await fetch(url, config);

        // Handle 401 Unauthorized
        if (response.status === 401) {
            redirectToLogin();
            throw new Error('Unauthorized - Please log in again');
        }

        // Handle 403 Forbidden
        if (response.status === 403) {
            throw new Error('Access denied - Insufficient permissions');
        }

        // Parse JSON response
        const responseData = await response.json();

        // Check if response is successful
        if (!response.ok) {
            // Extract error message from response
            const errorMessage = responseData.message || responseData.error || `Request failed with status ${response.status}`;
            throw new Error(errorMessage);
        }

        // Extract data from ApiResponse wrapper
        // Backend returns: { success: true, message: "...", data: [...] }
        if (responseData && typeof responseData === 'object' && 'data' in responseData) {
            return responseData.data;
        }

        return responseData;
    } catch (error) {
        // Network or parsing error
        if (error.name === 'TypeError' && error.message === 'Failed to fetch') {
            throw new Error('Network error - Please check your connection');
        }
        throw error;
    }
}

/**
 * Upload file with progress tracking
 * @param {string} endpoint - API endpoint
 * @param {FormData} formData - Form data with file
 * @param {function} onProgress - Progress callback (optional)
 * @returns {Promise} Response data
 */
export async function uploadFile(endpoint, formData, onProgress = null) {
    const url = `${API_BASE_URL}${endpoint}`;
    
    return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();

        // Track upload progress
        if (onProgress) {
            xhr.upload.addEventListener('progress', (e) => {
                if (e.lengthComputable) {
                    const percentComplete = (e.loaded / e.total) * 100;
                    onProgress(percentComplete);
                }
            });
        }

        // Handle completion
        xhr.addEventListener('load', () => {
            if (xhr.status === 401) {
                redirectToLogin();
                reject(new Error('Unauthorized - Please log in again'));
                return;
            }

            try {
                let data = null;
                const raw = xhr.responseText ?? '';
                if (raw && raw.trim().length > 0) {
                    try {
                        data = JSON.parse(raw);
                    } catch (e) {
                        // Fallback: non-JSON body (e.g., HTML error page)
                        data = { message: raw };
                    }
                } else {
                    data = {};
                }

                if (xhr.status >= 200 && xhr.status < 300) {
                    // Extract data from ApiResponse wrapper
                    if (data && typeof data === 'object' && 'data' in data) {
                        resolve(data.data);
                    } else {
                        resolve(data);
                    }
                } else {
                    const errorMessage = (data && (data.message || data.error)) || `Upload failed with status ${xhr.status}`;
                    reject(new Error(errorMessage));
                }
            } catch (error) {
                reject(new Error('Failed to parse server response'));
            }
        });

        // Handle errors
        xhr.addEventListener('error', () => {
            reject(new Error('Network error - Please check your connection'));
        });

        xhr.addEventListener('abort', () => {
            reject(new Error('Upload cancelled'));
        });

        // Open connection and set headers
        xhr.open('POST', url);
        
        const token = getToken();
        if (token) {
            xhr.setRequestHeader('Authorization', `Bearer ${token}`);
        }

        // Send the form data
        xhr.send(formData);
    });
}

// API endpoint methods

// Auth endpoints
export const auth = {
    login: (credentials) => apiRequest('/auth/login', {
        method: 'POST',
        body: JSON.stringify(credentials),
    }),
    
    logout: () => apiRequest('/auth/logout', {
        method: 'POST',
    }),
    
    getCurrentUser: () => apiRequest('/auth/me', {
        method: 'GET',
    }),
};

// HOD endpoints
export const hod = {
    // Academic Years (read-only)
    getAcademicYears: () => apiRequest('/hod/academic-years', {
        method: 'GET',
    }),
    
    // Professors (legacy - kept for backward compatibility)
    getProfessors: () => apiRequest('/hod/professors', {
        method: 'GET',
    }),
    
    createProfessor: (professorData) => apiRequest('/hod/professors', {
        method: 'POST',
        body: JSON.stringify(professorData),
    }),
    
    updateProfessor: (id, professorData) => apiRequest(`/hod/professors/${id}`, {
        method: 'PUT',
        body: JSON.stringify(professorData),
    }),
    
    deleteProfessor: (id) => apiRequest(`/hod/professors/${id}`, {
        method: 'DELETE',
    }),
    
    // Requests (legacy - kept for backward compatibility)
    getRequests: (params = {}) => {
        const queryString = new URLSearchParams(params).toString();
        const endpoint = queryString ? `/hod/document-requests?${queryString}` : '/hod/document-requests';
        return apiRequest(endpoint, {
            method: 'GET',
        });
    },
    
    createRequest: (requestData) => apiRequest('/hod/document-requests', {
        method: 'POST',
        body: JSON.stringify(requestData),
    }),
    
    getRequestDetails: (requestId) => apiRequest(`/hod/document-requests/${requestId}`, {
        method: 'GET',
    }),
    
    deleteRequest: (requestId) => apiRequest(`/hod/document-requests/${requestId}`, {
        method: 'DELETE',
    }),
    
    // Semester-based Dashboard
    getDashboardOverview: (semesterId) => apiRequest(`/hod/dashboard/overview?semesterId=${semesterId}`, {
        method: 'GET',
    }),
    
    // Semester-based Submission Status
    getSubmissionStatus: (semesterId, filters = {}) => {
        const params = new URLSearchParams({ semesterId, ...filters });
        return apiRequest(`/hod/submissions/status?${params}`, {
            method: 'GET',
        });
    },
    
    // Semester-based Reports
    getProfessorSubmissionReport: (semesterId) => apiRequest(`/hod/reports/professor-submissions?semesterId=${semesterId}`, {
        method: 'GET',
    }),
    
    exportReportToPdf: (semesterId) => {
        const token = getToken();
        return fetch(`${API_BASE_URL}/hod/reports/professor-submissions/pdf?semesterId=${semesterId}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
            },
        });
    },
    
    // File Explorer
    getFileExplorerRoot: (academicYearId, semesterId) => 
        apiRequest(`/hod/file-explorer/root?academicYearId=${academicYearId}&semesterId=${semesterId}`, {
            method: 'GET',
        }),
    
    getFileExplorerNode: (path) => 
        apiRequest(`/hod/file-explorer/node?path=${encodeURIComponent(path)}`, {
            method: 'GET',
        }),
    
    downloadFile: (fileId) => 
        fetch(`${API_BASE_URL}/hod/files/${fileId}/download`, {
            headers: {
                'Authorization': `Bearer ${getToken()}`,
            },
        }),
    
    // Legacy Reports (kept for backward compatibility)
    getSubmissionSummaryReport: () => apiRequest('/hod/reports/submission-summary', {
        method: 'GET',
    }),
    
    downloadSubmissionSummaryPdf: () => {
        const token = getToken();
        return fetch(`${API_BASE_URL}/hod/reports/submission-summary/pdf`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
            },
        });
    },
};

// Professor endpoints
export const professor = {
    getRequests: (page = 0, size = 10, sortBy = 'createdAt', sortDir = 'desc') => 
        apiRequest(`/professor/document-requests?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`, {
            method: 'GET',
        }),
    
    submitDocument: (requestId, formData, onProgress) => 
        uploadFile(`/professor/document-requests/${requestId}/submit`, formData, onProgress),
    
    // Multi-file upload endpoints
    uploadMultipleDocuments: (requestId, formData, onProgress) =>
        uploadFile(`/professor/document-requests/${requestId}/upload-multiple`, formData, onProgress),
    
    addFilesToSubmission: (requestId, formData, onProgress) =>
        uploadFile(`/professor/document-requests/${requestId}/add-files`, formData, onProgress),
    
    getFileAttachments: (requestId) => 
        apiRequest(`/professor/document-requests/${requestId}/file-attachments`, {
            method: 'GET',
        }),
    
    deleteFileAttachment: (attachmentId) =>
        apiRequest(`/professor/file-attachments/${attachmentId}`, {
            method: 'DELETE',
        }),
    
    downloadFileAttachment: (attachmentId) =>
        fetch(`${API_BASE_URL}/professor/file-attachments/${attachmentId}/download`, {
            headers: {
                'Authorization': `Bearer ${getToken()}`,
            },
        }),
    
    reorderFileAttachments: (submittedDocumentId, attachmentIds) =>
        apiRequest(`/professor/submitted-documents/${submittedDocumentId}/reorder-files`, {
            method: 'PUT',
            body: JSON.stringify(attachmentIds),
        }),
    
    getNotifications: () => apiRequest('/professor/notifications', {
        method: 'GET',
    }),
    
    markNotificationSeen: (notificationId) => apiRequest(`/professor/notifications/${notificationId}/seen`, {
        method: 'PUT',
    }),
    
    // Semester-based Dashboard
    getMyCourses: (semesterId) => apiRequest(`/professor/dashboard/courses?semesterId=${semesterId}`, {
        method: 'GET',
    }),
    
    getDashboardOverview: (semesterId) => apiRequest(`/professor/dashboard/overview?semesterId=${semesterId}`, {
        method: 'GET',
    }),
    
    // Semester-based File Upload
    uploadFiles: (courseAssignmentId, documentType, formData, onProgress) =>
        uploadFile(`/professor/submissions/upload?courseAssignmentId=${courseAssignmentId}&documentType=${documentType}`, formData, onProgress),
    
    replaceFiles: (submissionId, formData, onProgress) =>
        uploadFile(`/professor/submissions/${submissionId}/replace`, formData, onProgress),
    
    // Submissions
    getMySubmissions: (semesterId) => apiRequest(`/professor/submissions?semesterId=${semesterId}`, {
        method: 'GET',
    }),
    
    getSubmission: (submissionId) => apiRequest(`/professor/submissions/${submissionId}`, {
        method: 'GET',
    }),
    
    getSubmissionFiles: (submissionId) => apiRequest(`/professor/submissions/${submissionId}/files`, {
        method: 'GET',
    }),
    
    downloadSubmissionFile: (fileId) =>
        fetch(`${API_BASE_URL}/professor/files/${fileId}/download`, {
            headers: {
                'Authorization': `Bearer ${getToken()}`,
            },
        }),
    
    // File Explorer
    getFileExplorerRoot: (academicYearId, semesterId) => 
        apiRequest(`/professor/file-explorer/root?academicYearId=${academicYearId}&semesterId=${semesterId}`, {
            method: 'GET',
        }),
    
    getFileExplorerNode: (path) => 
        apiRequest(`/professor/file-explorer/node?path=${encodeURIComponent(path)}`, {
            method: 'GET',
        }),
};

// Export multi-file functions for easy import
export const uploadMultipleFiles = (requestId, formData, onProgress) => 
    professor.uploadMultipleDocuments(requestId, formData, onProgress);

export const addFilesToSubmission = (requestId, formData, onProgress) =>
    professor.addFilesToSubmission(requestId, formData, onProgress);

export const getFileAttachments = (requestId) => 
    professor.getFileAttachments(requestId);

export const deleteFileAttachment = (attachmentId) =>
    professor.deleteFileAttachment(attachmentId);

export const reorderFileAttachments = (submittedDocumentId, attachmentIds) =>
    professor.reorderFileAttachments(submittedDocumentId, attachmentIds);


// Deanship endpoints
export const deanship = {
    // Academic Years
    getAcademicYears: () => apiRequest('/deanship/academic-years', {
        method: 'GET',
    }),
    
    createAcademicYear: (data) => apiRequest('/deanship/academic-years', {
        method: 'POST',
        body: JSON.stringify(data),
    }),
    
    updateAcademicYear: (id, data) => apiRequest(`/deanship/academic-years/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data),
    }),
    
    activateAcademicYear: (id) => apiRequest(`/deanship/academic-years/${id}/activate`, {
        method: 'PUT',
    }),
    
    // Semesters
    getSemesters: (academicYearId) => apiRequest(`/deanship/academic-years/${academicYearId}/semesters`, {
        method: 'GET',
    }),
    
    updateSemester: (id, data) => apiRequest(`/deanship/semesters/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data),
    }),
    
    // Professors
    getProfessors: (departmentId = null) => {
        const params = departmentId ? `?departmentId=${departmentId}` : '';
        return apiRequest(`/deanship/professors${params}`, {
            method: 'GET',
        });
    },
    
    createProfessor: (data) => apiRequest('/deanship/professors', {
        method: 'POST',
        body: JSON.stringify(data),
    }),
    
    updateProfessor: (id, data) => apiRequest(`/deanship/professors/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data),
    }),
    
    deactivateProfessor: (id) => apiRequest(`/deanship/professors/${id}/deactivate`, {
        method: 'PUT',
    }),
    
    activateProfessor: (id) => apiRequest(`/deanship/professors/${id}/activate`, {
        method: 'PUT',
    }),
    
    // Courses
    getCourses: (departmentId = null) => {
        const params = departmentId ? `?departmentId=${departmentId}` : '';
        return apiRequest(`/deanship/courses${params}`, {
            method: 'GET',
        });
    },
    
    createCourse: (data) => apiRequest('/deanship/courses', {
        method: 'POST',
        body: JSON.stringify(data),
    }),
    
    updateCourse: (id, data) => apiRequest(`/deanship/courses/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data),
    }),
    
    deactivateCourse: (id) => apiRequest(`/deanship/courses/${id}/deactivate`, {
        method: 'PUT',
    }),
    
    // Course Assignments
    getCourseAssignments: (semesterId, professorId = null) => {
        const params = new URLSearchParams({ semesterId });
        if (professorId) params.append('professorId', professorId);
        return apiRequest(`/deanship/course-assignments?${params}`, {
            method: 'GET',
        });
    },
    
    createCourseAssignment: (data) => apiRequest('/deanship/course-assignments', {
        method: 'POST',
        body: JSON.stringify(data),
    }),
    
    deleteCourseAssignment: (id) => apiRequest(`/deanship/course-assignments/${id}`, {
        method: 'DELETE',
    }),
    
    // Departments
    getDepartments: () => apiRequest('/deanship/departments', {
        method: 'GET',
    }),
    
    // Reports
    getSystemWideReport: (semesterId) => apiRequest(`/deanship/reports/system-wide?semesterId=${semesterId}`, {
        method: 'GET',
    }),
};

// File Explorer endpoints (shared)
export const fileExplorer = {
    getRoot: (academicYearId, semesterId) => 
        apiRequest(`/file-explorer/root?academicYearId=${academicYearId}&semesterId=${semesterId}`, {
            method: 'GET',
        }),
    
    getNode: (path) => 
        apiRequest(`/file-explorer/node?path=${encodeURIComponent(path)}`, {
            method: 'GET',
        }),
    
    getBreadcrumbs: (path) => 
        apiRequest(`/file-explorer/breadcrumbs?path=${encodeURIComponent(path)}`, {
            method: 'GET',
        }),
    
    getFileMetadata: (fileId) => 
        apiRequest(`/file-explorer/files/${fileId}`, {
            method: 'GET',
        }),
    
    downloadFile: (fileId) => 
        fetch(`${API_BASE_URL}/file-explorer/files/${fileId}/download`, {
            headers: {
                'Authorization': `Bearer ${getToken()}`,
            },
        }),
};

export default {
    auth,
    hod,
    professor,
    deanship,
    fileExplorer,
    getUserInfo,
    saveAuthData,
    clearAuthData,
    isAuthenticated,
    redirectToLogin,
};
