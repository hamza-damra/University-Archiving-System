/**
 * API Service
 * Centralized API calls with token management, auto-refresh, and error handling
 */

// Dynamic base URL - uses current host to support external access
const API_BASE_URL = `${window.location.origin}/api`;

// Token refresh state to prevent multiple simultaneous refresh attempts
let isRefreshing = false;
let refreshSubscribers = [];

/**
 * Get authentication token from localStorage
 */
function getToken() {
    return localStorage.getItem('token');
}

/**
 * Get refresh token from localStorage
 */
function getRefreshToken() {
    return localStorage.getItem('refreshToken');
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
export function saveAuthData(token, userInfo, refreshToken = null) {
    localStorage.setItem('token', token);
    localStorage.setItem('userInfo', JSON.stringify(userInfo));
    if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken);
    }
}

/**
 * Update only the access token (used after refresh)
 */
function updateAccessToken(newToken) {
    localStorage.setItem('token', newToken);
}

/**
 * Clear authentication data from localStorage
 * Also clears any cached role-specific data that could cause navigation issues
 */
export function clearAuthData() {
    // Core auth data
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userInfo');
    
    // Clear all role-specific cached data to prevent stale state
    // Admin-specific
    localStorage.removeItem('admin_selected_academic_year');
    localStorage.removeItem('admin_selected_semester');
    localStorage.removeItem('admin_last_page');
    localStorage.removeItem('adminCurrentTab');
    
    // Deanship-specific
    localStorage.removeItem('deanship_selected_academic_year');
    localStorage.removeItem('deanship_selected_semester');
    localStorage.removeItem('deanship_academic_years_options');
    localStorage.removeItem('deanship_semesters_options');
    localStorage.removeItem('deanship_user_name');
    localStorage.removeItem('deanship-active-tab');
    localStorage.removeItem('deanship_file_explorer_html');
    
    // HOD-specific (if any)
    localStorage.removeItem('hod_selected_academic_year');
    localStorage.removeItem('hod_selected_semester');
    
    // Professor-specific (if any)
    localStorage.removeItem('prof_selected_academic_year');
    localStorage.removeItem('prof_selected_semester');
}

/**
 * Check if user is authenticated (has token)
 */
export function isAuthenticated() {
    return !!getToken();
}

/**
 * Redirect to login page with optional error message
 */
export function redirectToLogin(error = null) {
    clearAuthData();
    const params = new URLSearchParams();
    if (error) {
        params.set('error', error);
    }
    const queryString = params.toString();
    window.location.href = '/index.html' + (queryString ? '?' + queryString : '');
}

/**
 * Subscribe to token refresh completion
 */
function subscribeTokenRefresh(callback) {
    refreshSubscribers.push(callback);
}

/**
 * Notify all subscribers that token has been refreshed
 */
function onTokenRefreshed(newToken) {
    refreshSubscribers.forEach(callback => callback(newToken));
    refreshSubscribers = [];
}

/**
 * Attempt to refresh the access token using the refresh token
 * @returns {Promise<string|null>} New access token or null if refresh failed
 */
async function attemptTokenRefresh() {
    const refreshToken = getRefreshToken();
    
    if (!refreshToken) {
        console.log('No refresh token available');
        return null;
    }
    
    // If already refreshing, wait for it to complete
    if (isRefreshing) {
        return new Promise((resolve) => {
            subscribeTokenRefresh((newToken) => {
                resolve(newToken);
            });
        });
    }
    
    isRefreshing = true;
    
    try {
        const response = await fetch(`${API_BASE_URL}/auth/refresh-token`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ refreshToken }),
        });
        
        if (response.ok) {
            const data = await response.json();
            if (data.success && data.data) {
                const newAccessToken = data.data.accessToken;
                updateAccessToken(newAccessToken);
                onTokenRefreshed(newAccessToken);
                console.log('Token refreshed successfully');
                return newAccessToken;
            }
        }
        
        // Refresh failed
        console.log('Token refresh failed');
        return null;
        
    } catch (error) {
        console.error('Token refresh error:', error);
        return null;
    } finally {
        isRefreshing = false;
    }
}

/**
 * Validate the current token with the server
 * @returns {Promise<object>} Validation result with status and details
 */
export async function validateToken() {
    const token = getToken();
    
    if (!token) {
        return { valid: false, status: 'NO_TOKEN', message: 'No token found' };
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/auth/validate`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
            },
        });
        
        if (response.ok) {
            const data = await response.json();
            return data.data || { valid: false, status: 'INVALID_RESPONSE' };
        }
        
        return { valid: false, status: 'VALIDATION_FAILED', message: 'Server validation failed' };
        
    } catch (error) {
        console.error('Token validation error:', error);
        return { valid: false, status: 'NETWORK_ERROR', message: error.message };
    }
}

/**
 * Initialize authentication - validate token and refresh if needed
 * Call this on page load to ensure valid authentication
 * @returns {Promise<boolean>} True if authenticated, false otherwise
 */
export async function initializeAuth() {
    const token = getToken();
    const refreshToken = getRefreshToken();
    const userInfo = getUserInfo();
    
    // No credentials at all
    if (!token && !refreshToken) {
        console.log('No authentication credentials found');
        return false;
    }
    
    // No user info stored
    if (!userInfo) {
        console.log('No user info found, clearing auth');
        clearAuthData();
        return false;
    }
    
    // Validate current token
    const validation = await validateToken();
    
    if (validation.valid) {
        console.log('Token is valid');
        return true;
    }
    
    // Token is invalid or expired, try to refresh
    if (validation.status === 'TOKEN_EXPIRED' || validation.status === 'NO_TOKEN') {
        console.log('Token expired or missing, attempting refresh...');
        const newToken = await attemptTokenRefresh();
        
        if (newToken) {
            console.log('Authentication restored with new token');
            return true;
        }
    }
    
    // All refresh attempts failed
    console.log('Authentication failed, clearing credentials');
    clearAuthData();
    return false;
}

/**
 * Make an API request with authentication and auto token refresh
 * @param {string} endpoint - API endpoint (relative to base URL)
 * @param {object} options - Fetch options
 * @param {boolean} retryOnExpire - Whether to retry with refreshed token on 401
 * @returns {Promise} Response data
 */
export async function apiRequest(endpoint, options = {}, retryOnExpire = true) {
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

        // Handle 401 Unauthorized - try to refresh token
        if (response.status === 401) {
            // Check if the error is specifically about token expiration
            let errorData;
            try {
                errorData = await response.clone().json();
            } catch (e) {
                errorData = null;
            }
            
            const errorCode = errorData?.error?.code;
            
            // If token expired and we should retry, attempt refresh
            if (retryOnExpire && (errorCode === 'TOKEN_EXPIRED' || !errorCode)) {
                console.log('Token expired, attempting refresh...');
                const newToken = await attemptTokenRefresh();
                
                if (newToken) {
                    // Retry the request with new token
                    headers['Authorization'] = `Bearer ${newToken}`;
                    const retryConfig = { ...config, headers };
                    const retryResponse = await fetch(url, retryConfig);
                    
                    if (retryResponse.ok) {
                        const retryData = await retryResponse.json();
                        if (retryData && typeof retryData === 'object' && 'data' in retryData) {
                            return retryData.data;
                        }
                        return retryData;
                    }
                    
                    // Retry also failed
                    if (retryResponse.status === 401) {
                        redirectToLogin('session_expired');
                        throw new Error('Session expired - Please log in again');
                    }
                }
            }
            
            // Refresh failed or not applicable, redirect to login
            redirectToLogin('session_expired');
            throw new Error('Unauthorized - Please log in again');
        }

        // Parse JSON response for all non-OK responses to extract error details
        let responseData;
        try {
            responseData = await response.json();
        } catch (parseError) {
            // If JSON parsing fails and it's a 403, throw generic forbidden error
            if (response.status === 403) {
                throw new Error('Access denied - Insufficient permissions');
            }
            // For other parse failures, throw generic server error
            throw new Error(`Server error (${response.status}) - Unable to parse response`);
        }

        // Check if response is successful
        if (!response.ok) {
            // Handle rate limit errors (429) - MUST be checked first
            if (response.status === 429) {
                const errorCode = responseData.error?.code || 'RATE_LIMIT_EXCEEDED';
                const errorMessage = responseData.error?.message || 'Too many requests. Please wait before trying again.';
                const retryAfterSeconds = responseData.error?.retryAfterSeconds || parseInt(response.headers.get('Retry-After')) || 60;
                
                console.log('Rate limit detected:', { errorCode, errorMessage, retryAfterSeconds });
                
                const error = new Error(errorMessage);
                error.code = errorCode;
                error.retryAfterSeconds = retryAfterSeconds;
                throw error;
            }
            
            // Handle account disabled/locked errors (403)
            if (response.status === 403) {
                const errorCode = responseData.error?.code;
                const errorMessage = responseData.error?.message || 'Access denied - Insufficient permissions';
                
                console.log('403 error detected:', { errorCode, errorMessage });
                
                const error = new Error(errorMessage);
                if (errorCode) {
                    error.code = errorCode;
                }
                throw error;
            }
            
            // Handle validation errors (400 with error.validationErrors object)
            // Backend returns: { success: false, error: { validationErrors: {...}, message: "..." } }
            const validationErrors = responseData.error?.validationErrors || responseData.errors;
            if (response.status === 400 && validationErrors) {
                const formattedErrors = formatValidationErrors(validationErrors);
                const error = new Error('Validation failed');
                error.validationErrors = validationErrors;
                error.formattedMessage = formattedErrors;
                throw error;
            }

            // Handle general errors - check all possible message locations
            const errorMessage = responseData.error?.message || responseData.message || responseData.error || `Request failed with status ${response.status}`;
            const error = new Error(errorMessage);
            error.status = response.status; // Include status code for error handling
            if (responseData.error?.code) {
                error.code = responseData.error.code;
            }
            throw error;
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
 * Format validation errors into a readable message
 * @param {object} errors - Validation errors object
 * @returns {string} Formatted error message
 */
function formatValidationErrors(errors) {
    if (!errors || typeof errors !== 'object') {
        return 'Validation failed';
    }

    const errorMessages = Object.entries(errors)
        .map(([field, message]) => `${formatFieldName(field)}: ${message}`)
        .join('\n');

    return errorMessages || 'Validation failed';
}

/**
 * Format field name for display
 * @param {string} field - Field name
 * @returns {string} Formatted field name
 */
function formatFieldName(field) {
    // Convert camelCase to Title Case with spaces
    return field
        .replace(/([A-Z])/g, ' $1')
        .replace(/^./, str => str.toUpperCase())
        .trim();
}

/**
 * Extract readable error message from error object
 * @param {Error} error - Error object
 * @returns {string} Readable error message
 */
export function getErrorMessage(error) {
    if (!error) {
        return 'An unknown error occurred';
    }

    // Handle validation errors with formatted message
    if (error.formattedMessage) {
        return error.formattedMessage;
    }

    // Handle standard error message
    if (error.message) {
        return error.message;
    }

    // Fallback
    return 'An unexpected error occurred';
}

/**
 * Upload file with progress tracking and auto token refresh
 * @param {string} endpoint - API endpoint
 * @param {FormData} formData - Form data with file
 * @param {function} onProgress - Progress callback (optional)
 * @returns {Promise} Response data
 */
export async function uploadFile(endpoint, formData, onProgress = null, method = 'POST') {
    const url = `${API_BASE_URL}${endpoint}`;

    const executeUpload = (token) => {
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
            xhr.addEventListener('load', async () => {
                // Handle 401 - try to refresh token
                if (xhr.status === 401) {
                    try {
                        // Parse error to check if it's token expiration
                        let errorData;
                        try {
                            errorData = JSON.parse(xhr.responseText);
                        } catch (e) {
                            errorData = null;
                        }
                        
                        const errorCode = errorData?.error?.code;
                        if (errorCode === 'TOKEN_EXPIRED' || !errorCode) {
                            // Try to refresh token
                            const newToken = await attemptTokenRefresh();
                            if (newToken) {
                                // Resolve with retry signal
                                resolve({ _retry: true, newToken });
                                return;
                            }
                        }
                    } catch (e) {
                        // Refresh failed
                    }
                    
                    redirectToLogin('session_expired');
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
            xhr.open(method, url);

            if (token) {
                xhr.setRequestHeader('Authorization', `Bearer ${token}`);
            }

            // Send the form data
            xhr.send(formData);
        });
    };

    // First attempt
    let result = await executeUpload(getToken());
    
    // If we need to retry with new token
    if (result && result._retry && result.newToken) {
        // Need to recreate FormData as it can only be used once
        // The caller should handle retry if FormData can't be reused
        result = await executeUpload(result.newToken);
    }
    
    return result;
}

// API endpoint methods

// Auth endpoints
export const auth = {
    login: (credentials) => apiRequest('/auth/login', {
        method: 'POST',
        body: JSON.stringify(credentials),
    }),

    logout: (refreshToken = null) => apiRequest('/auth/logout', {
        method: 'POST',
        body: refreshToken ? JSON.stringify({ refreshToken }) : '{}',
    }),

    getCurrentUser: () => apiRequest('/auth/me', {
        method: 'GET',
    }),

    refreshToken: (refreshToken) => apiRequest('/auth/refresh-token', {
        method: 'POST',
        body: JSON.stringify({ refreshToken }),
    }, false), // Don't retry on expire for refresh endpoint

    validate: () => apiRequest('/auth/validate', {
        method: 'GET',
    }, false), // Don't retry on expire for validate endpoint
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

    // Report Filter Options
    getReportFilterOptions: () => apiRequest('/hod/reports/filter-options', {
        method: 'GET',
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

    // Task Management
    getTasks: (courseId = null, semesterId = null, professorId = null, status = null) => {
        const params = new URLSearchParams();
        if (courseId) params.append('courseId', courseId);
        if (semesterId) params.append('semesterId', semesterId);
        if (professorId) params.append('professorId', professorId);
        if (status) params.append('status', status);
        return apiRequest(`/hod/tasks?${params}`, {
            method: 'GET',
        });
    },

    getTask: (taskId) => apiRequest(`/hod/tasks/${taskId}`, {
        method: 'GET',
    }),

    approveTask: (taskId, feedback = null) => apiRequest(`/hod/tasks/${taskId}/approve`, {
        method: 'PUT',
        body: JSON.stringify({ feedback }),
    }),

    rejectTask: (taskId, feedback = null) => apiRequest(`/hod/tasks/${taskId}/reject`, {
        method: 'PUT',
        body: JSON.stringify({ feedback }),
    }),

    getTaskStatistics: (semesterId = null) => {
        const params = semesterId ? `?semesterId=${semesterId}` : '';
        return apiRequest(`/hod/tasks/statistics${params}`, {
            method: 'GET',
        });
    },

    getTasksForProfessor: (professorId, semesterId = null) => {
        const params = new URLSearchParams({ professorId });
        if (semesterId) params.append('semesterId', semesterId);
        return apiRequest(`/hod/tasks/professor/${professorId}?${params}`, {
            method: 'GET',
        });
    },

    // Task Evidence (for review)
    getTaskEvidence: (taskId) => apiRequest(`/hod/tasks/${taskId}/evidence`, {
        method: 'GET',
    }),
};

// Professor endpoints
export const professor = {
    // Academic Years & Semesters
    getAcademicYears: () => apiRequest('/professor/academic-years', {
        method: 'GET',
    }),

    getSemesters: (academicYearId) => apiRequest(`/professor/academic-years/${academicYearId}/semesters`, {
        method: 'GET',
    }),

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
        uploadFile(`/professor/submissions/${submissionId}/replace`, formData, onProgress, 'PUT'),

    // Submissions
    getMySubmissions: (semesterId) => apiRequest(`/professor/submissions?semesterId=${semesterId}`, {
        method: 'GET',
    }),

    getSubmission: (submissionId) => apiRequest(`/professor/submissions/${submissionId}`, {
        method: 'GET',
    }),

    // Note: Use getSubmission() to get submission with files included

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

    // Task Management
    getTasks: (courseId = null, semesterId = null, status = null) => {
        const params = new URLSearchParams();
        if (courseId) params.append('courseId', courseId);
        if (semesterId) params.append('semesterId', semesterId);
        if (status) params.append('status', status);
        return apiRequest(`/professor/tasks?${params}`, {
            method: 'GET',
        });
    },

    getTask: (taskId) => apiRequest(`/professor/tasks/${taskId}`, {
        method: 'GET',
    }),

    createTask: (data) => apiRequest('/professor/tasks', {
        method: 'POST',
        body: JSON.stringify(data),
    }),

    updateTask: (taskId, data) => apiRequest(`/professor/tasks/${taskId}`, {
        method: 'PUT',
        body: JSON.stringify(data),
    }),

    deleteTask: (taskId) => apiRequest(`/professor/tasks/${taskId}`, {
        method: 'DELETE',
    }),

    getWeightSummary: (courseId, semesterId) =>
        apiRequest(`/professor/tasks/weight-summary?courseId=${courseId}&semesterId=${semesterId}`, {
            method: 'GET',
        }),

    // Task Evidence
    getTaskEvidence: (taskId) => apiRequest(`/professor/tasks/${taskId}/evidence`, {
        method: 'GET',
    }),

    addTaskEvidence: (taskId, fileIds) => apiRequest(`/professor/tasks/${taskId}/evidence`, {
        method: 'POST',
        body: JSON.stringify(fileIds),
    }),

    removeTaskEvidence: (taskId, evidenceId) => apiRequest(`/professor/tasks/${taskId}/evidence/${evidenceId}`, {
        method: 'DELETE',
    }),

    getAvailableFilesForEvidence: (semesterId = null) => {
        const params = semesterId ? `?semesterId=${semesterId}` : '';
        return apiRequest(`/professor/tasks/available-files${params}`, {
            method: 'GET',
        });
    },
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

    uploadFiles: (path, formData, onProgress) =>
        uploadFile(`/file-explorer/upload?path=${encodeURIComponent(path)}`, formData, onProgress),

    downloadFile: (fileId) =>
        fetch(`${API_BASE_URL}/file-explorer/files/${fileId}/download`, {
            headers: {
                'Authorization': `Bearer ${getToken()}`,
            },
        }),
    
    /**
     * Create a new folder in the file explorer.
     * Only professors can create folders within their own namespace.
     * 
     * @param {string} path - The parent path where the folder will be created
     * @param {string} folderName - The name of the new folder
     * @returns {Promise<Object>} The created folder response
     */
    createFolder: (path, folderName) =>
        apiRequest('/file-explorer/folder', {
            method: 'POST',
            body: JSON.stringify({ path, folderName }),
        }),
    
    /**
     * Delete a folder and all its contents (files and subfolders).
     * Only professors can delete folders within their own namespace.
     * This operation also deletes all physical files and folders from the uploads directory.
     * 
     * @param {string} folderPath - The full path of the folder to delete
     * @returns {Promise<Object>} The delete response with statistics
     */
    deleteFolder: (folderPath) =>
        apiRequest('/file-explorer/folder', {
            method: 'DELETE',
            body: JSON.stringify({ folderPath }),
        }),
    
    /**
     * Delete a file from the file explorer.
     * Only professors can delete their own files.
     * 
     * @param {number} fileId - The ID of the file to delete
     * @returns {Promise<Object>} The delete response
     */
    deleteFile: (fileId) =>
        apiRequest(`/file-explorer/files/${fileId}`, {
            method: 'DELETE',
        }),
    
    /**
     * Replace a file with a new version.
     * Only professors can replace their own files.
     * 
     * @param {number} fileId - The ID of the file to replace
     * @param {FormData} formData - FormData containing the new file and optional notes
     * @param {function} onProgress - Optional progress callback
     * @returns {Promise<Object>} The replace response with new file info
     */
    replaceFile: (fileId, formData, onProgress) =>
        uploadFile(`/file-explorer/files/${fileId}/replace`, formData, onProgress),
    
    // ==================== New Filesystem-Based API Endpoints ====================
    
    /**
     * List directory contents from filesystem.
     * This is the primary endpoint for browsing the file system.
     * Supports pagination, sorting, and ETag caching.
     * 
     * @param {string} path - Relative path from uploads root (empty for root)
     * @param {Object} options - Query options
     * @param {number} [options.page=1] - Page number (1-indexed)
     * @param {number} [options.pageSize=50] - Items per page
     * @param {string} [options.sortBy='name'] - Sort field: 'name', 'modifiedAt', 'size'
     * @param {string} [options.sortOrder='asc'] - Sort order: 'asc' or 'desc'
     * @param {string} [options.etag] - ETag from previous request for 304 handling
     * @returns {Promise<Object>} DirectoryListingDTO with folders and files
     */
    listDirectory: async (path = '', options = {}) => {
        const {
            page = 1,
            pageSize = 50,
            sortBy = 'name',
            sortOrder = 'asc',
            etag = null
        } = options;
        
        const params = new URLSearchParams({
            path: path,
            page: page.toString(),
            pageSize: pageSize.toString(),
            sortBy: sortBy,
            sortOrder: sortOrder
        });
        
        const headers = {};
        if (etag) {
            headers['If-None-Match'] = etag;
        }
        
        try {
            const response = await fetch(`${API_BASE_URL}/file-explorer/list?${params}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${getToken()}`,
                    ...headers
                }
            });
            
            // Handle 304 Not Modified
            if (response.status === 304) {
                return { notModified: true, etag: etag };
            }
            
            const data = await response.json();
            
            if (!response.ok) {
                throw new Error(data.error?.message || data.message || 'Failed to list directory');
            }
            
            // Extract new ETag from response
            const newEtag = response.headers.get('ETag');
            
            return {
                notModified: false,
                etag: newEtag,
                data: data.data || data
            };
        } catch (error) {
            console.error('Error listing directory:', error);
            throw error;
        }
    },
    
    /**
     * Get directory tree with lazy loading support.
     * 
     * @param {string} path - Relative path from uploads root
     * @param {number} [depth=1] - Levels to load (1-3)
     * @param {string} [etag] - ETag from previous request
     * @returns {Promise<Object>} DirectoryTreeDTO with children
     */
    getDirectoryTree: async (path = '', depth = 1, etag = null) => {
        const params = new URLSearchParams({
            path: path,
            depth: depth.toString()
        });
        
        const headers = {};
        if (etag) {
            headers['If-None-Match'] = etag;
        }
        
        try {
            const response = await fetch(`${API_BASE_URL}/file-explorer/tree?${params}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${getToken()}`,
                    ...headers
                }
            });
            
            if (response.status === 304) {
                return { notModified: true, etag: etag };
            }
            
            const data = await response.json();
            
            if (!response.ok) {
                throw new Error(data.error?.message || data.message || 'Failed to get directory tree');
            }
            
            const newEtag = response.headers.get('ETag');
            
            return {
                notModified: false,
                etag: newEtag,
                data: data.data || data
            };
        } catch (error) {
            console.error('Error getting directory tree:', error);
            throw error;
        }
    },
    
    /**
     * Check if a path exists on the filesystem.
     * 
     * @param {string} path - Relative path to check
     * @returns {Promise<boolean>} True if path exists
     */
    checkPathExists: (path) =>
        apiRequest(`/file-explorer/exists?path=${encodeURIComponent(path)}`, {
            method: 'GET',
        }),
    
    /**
     * Refresh cache for a directory path.
     * Call after write operations if automatic refresh isn't sufficient.
     * 
     * @param {string} path - Relative path to refresh
     * @param {boolean} [recursive=true] - Whether to invalidate parent caches
     * @returns {Promise<void>}
     */
    refreshCache: (path = '', recursive = true) =>
        apiRequest(`/file-explorer/refresh-cache?path=${encodeURIComponent(path)}&recursive=${recursive}`, {
            method: 'POST',
        }),
    
    /**
     * Get current ETag for a directory.
     * Useful for checking if a refresh is needed.
     * 
     * @param {string} path - Relative path
     * @returns {Promise<string>} Current ETag
     */
    getDirectoryETag: (path = '') =>
        apiRequest(`/file-explorer/etag?path=${encodeURIComponent(path)}`, {
            method: 'GET',
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
