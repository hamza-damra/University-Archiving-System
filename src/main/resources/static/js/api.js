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
        const data = await response.json();

        // Check if response is successful
        if (!response.ok) {
            // Extract error message from response
            const errorMessage = data.message || data.error || `Request failed with status ${response.status}`;
            throw new Error(errorMessage);
        }

        return data;
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
                    resolve(data);
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
    // Professors
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
    
    // Requests
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
    
    // Reports
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

export default {
    auth,
    hod,
    professor,
    getUserInfo,
    saveAuthData,
    clearAuthData,
    isAuthenticated,
    redirectToLogin,
};
