/**
 * Utility functions for Dean Dashboard
 * Common operations for data transformation, validation, and formatting
 */

// ============================================================================
// DATA TRANSFORMATION
// ============================================================================

/**
 * Transform professor data for display
 * @param {Object} professor - Raw professor data
 * @returns {Object} Transformed professor data
 */
export function transformProfessorData(professor) {
    return {
        ...professor,
        displayName: professor.name || `${professor.firstName} ${professor.lastName}`,
        initials: generateInitials(professor.name || `${professor.firstName} ${professor.lastName}`),
        departmentName: professor.department?.name || 'N/A',
        statusBadge: professor.isActive ? 'Active' : 'Inactive',
        statusClass: professor.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
    };
}

/**
 * Transform course data for display
 * @param {Object} course - Raw course data
 * @returns {Object} Transformed course data
 */
export function transformCourseData(course) {
    return {
        ...course,
        displayName: `${course.courseCode} - ${course.courseName}`,
        departmentName: course.department?.name || 'N/A',
        levelDisplay: course.level || 'N/A',
        statusBadge: course.isActive ? 'Active' : 'Inactive',
        statusClass: course.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
    };
}

/**
 * Transform assignment data for display
 * @param {Object} assignment - Raw assignment data
 * @returns {Object} Transformed assignment data
 */
export function transformAssignmentData(assignment) {
    return {
        ...assignment,
        professorName: assignment.professor?.name || 'N/A',
        courseDisplay: `${assignment.course?.courseCode} - ${assignment.course?.courseName}`,
        departmentName: assignment.course?.department?.name || 'N/A',
        semesterType: assignment.semester?.type || 'N/A',
        statusBadge: assignment.isActive ? 'Active' : 'Inactive',
        statusClass: assignment.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
    };
}

/**
 * Generate initials from a name
 * @param {string} name - Full name
 * @returns {string} Initials (e.g., "John Smith" -> "JS")
 */
export function generateInitials(name) {
    if (!name) return '??';
    
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) {
        return parts[0].substring(0, 2).toUpperCase();
    }
    
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

/**
 * Generate consistent color for avatar based on name
 * @param {string} name - Name to generate color for
 * @returns {string} Hex color code
 */
export function generateAvatarColor(name) {
    if (!name) return '#6B7280'; // gray-500
    
    const colors = [
        '#3B82F6', // blue-500
        '#10B981', // green-500
        '#F59E0B', // yellow-500
        '#EF4444', // red-500
        '#8B5CF6', // purple-500
        '#EC4899', // pink-500
        '#14B8A6', // teal-500
        '#F97316'  // orange-500
    ];
    
    // Simple hash function
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    
    return colors[Math.abs(hash) % colors.length];
}

/**
 * Calculate progress percentage
 * @param {number} completed - Number of completed items
 * @param {number} total - Total number of items
 * @returns {number} Percentage (0-100)
 */
export function calculateProgress(completed, total) {
    if (total === 0) return 0;
    return Math.round((completed / total) * 100);
}

/**
 * Get progress color class based on percentage
 * @param {number} percentage - Progress percentage
 * @returns {string} Tailwind color class
 */
export function getProgressColorClass(percentage) {
    if (percentage >= 80) return 'bg-green-500';
    if (percentage >= 50) return 'bg-yellow-500';
    return 'bg-red-500';
}

// ============================================================================
// VALIDATION
// ============================================================================

/**
 * Validate email format
 * @param {string} email - Email address
 * @returns {boolean} True if valid
 */
export function validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

/**
 * Validate password strength
 * @param {string} password - Password
 * @returns {Object} Validation result with isValid and message
 */
export function validatePassword(password) {
    if (!password || password.length < 6) {
        return {
            isValid: false,
            message: 'Password must be at least 6 characters long'
        };
    }
    
    return {
        isValid: true,
        message: 'Password is valid'
    };
}

/**
 * Validate year range
 * @param {number} startYear - Start year
 * @param {number} endYear - End year
 * @returns {Object} Validation result
 */
export function validateYearRange(startYear, endYear) {
    if (!startYear || !endYear) {
        return {
            isValid: false,
            message: 'Both start and end years are required'
        };
    }
    
    if (endYear <= startYear) {
        return {
            isValid: false,
            message: 'End year must be greater than start year'
        };
    }
    
    if (endYear - startYear > 2) {
        return {
            isValid: false,
            message: 'Academic year span should not exceed 2 years'
        };
    }
    
    return {
        isValid: true,
        message: 'Year range is valid'
    };
}

/**
 * Validate name format (first and last name)
 * @param {string} fullName - Full name
 * @returns {Object} Validation result with firstName and lastName
 */
export function validateFullName(fullName) {
    if (!fullName || !fullName.trim()) {
        return {
            isValid: false,
            message: 'Name is required'
        };
    }
    
    const parts = fullName.trim().split(/\s+/);
    if (parts.length < 2) {
        return {
            isValid: false,
            message: 'Please enter both first name and last name'
        };
    }
    
    return {
        isValid: true,
        firstName: parts[0],
        lastName: parts.slice(1).join(' ')
    };
}

// ============================================================================
// FORMATTING
// ============================================================================

/**
 * Format date to relative time (e.g., "2 hours ago")
 * @param {string|Date} date - Date to format
 * @returns {string} Relative time string
 */
export function formatRelativeTime(date) {
    const now = new Date();
    const then = new Date(date);
    const diffMs = now - then;
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);
    
    if (diffSec < 60) return 'just now';
    if (diffMin < 60) return `${diffMin} minute${diffMin !== 1 ? 's' : ''} ago`;
    if (diffHour < 24) return `${diffHour} hour${diffHour !== 1 ? 's' : ''} ago`;
    if (diffDay < 7) return `${diffDay} day${diffDay !== 1 ? 's' : ''} ago`;
    if (diffDay < 30) return `${Math.floor(diffDay / 7)} week${Math.floor(diffDay / 7) !== 1 ? 's' : ''} ago`;
    if (diffDay < 365) return `${Math.floor(diffDay / 30)} month${Math.floor(diffDay / 30) !== 1 ? 's' : ''} ago`;
    return `${Math.floor(diffDay / 365)} year${Math.floor(diffDay / 365) !== 1 ? 's' : ''} ago`;
}

/**
 * Format file size to human-readable format
 * @param {number} bytes - File size in bytes
 * @returns {string} Formatted file size
 */
export function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

/**
 * Format number with thousands separator
 * @param {number} num - Number to format
 * @returns {string} Formatted number
 */
export function formatNumber(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

/**
 * Truncate text to specified length
 * @param {string} text - Text to truncate
 * @param {number} maxLength - Maximum length
 * @returns {string} Truncated text
 */
export function truncateText(text, maxLength = 50) {
    if (!text || text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
}

// ============================================================================
// FILTERING & SORTING
// ============================================================================

/**
 * Filter array by search term
 * @param {Array} items - Items to filter
 * @param {string} searchTerm - Search term
 * @param {Array<string>} fields - Fields to search in
 * @returns {Array} Filtered items
 */
export function filterBySearch(items, searchTerm, fields) {
    if (!searchTerm || !searchTerm.trim()) return items;
    
    const term = searchTerm.toLowerCase().trim();
    return items.filter(item => {
        return fields.some(field => {
            const value = getNestedValue(item, field);
            return value && value.toString().toLowerCase().includes(term);
        });
    });
}

/**
 * Get nested object value by path
 * @param {Object} obj - Object to get value from
 * @param {string} path - Path to value (e.g., "department.name")
 * @returns {*} Value at path
 */
export function getNestedValue(obj, path) {
    return path.split('.').reduce((current, key) => current?.[key], obj);
}

/**
 * Sort array by field
 * @param {Array} items - Items to sort
 * @param {string} field - Field to sort by
 * @param {string} direction - Sort direction ('asc' or 'desc')
 * @returns {Array} Sorted items
 */
export function sortByField(items, field, direction = 'asc') {
    return [...items].sort((a, b) => {
        const aVal = getNestedValue(a, field);
        const bVal = getNestedValue(b, field);
        
        if (aVal === bVal) return 0;
        
        const comparison = aVal < bVal ? -1 : 1;
        return direction === 'asc' ? comparison : -comparison;
    });
}

// ============================================================================
// DEBOUNCING & THROTTLING
// ============================================================================

/**
 * Debounce function execution
 * @param {Function} func - Function to debounce
 * @param {number} wait - Wait time in milliseconds
 * @returns {Function} Debounced function
 */
export function debounce(func, wait = 300) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * Throttle function execution
 * @param {Function} func - Function to throttle
 * @param {number} limit - Time limit in milliseconds
 * @returns {Function} Throttled function
 */
export function throttle(func, limit = 100) {
    let inThrottle;
    return function executedFunction(...args) {
        if (!inThrottle) {
            func(...args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

// ============================================================================
// DOM UTILITIES
// ============================================================================

/**
 * Escape HTML to prevent XSS
 * @param {string} text - Text to escape
 * @returns {string} Escaped text
 */
export function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Create element with attributes and children
 * @param {string} tag - HTML tag
 * @param {Object} attrs - Attributes
 * @param {Array|string} children - Children elements or text
 * @returns {HTMLElement} Created element
 */
export function createElement(tag, attrs = {}, children = []) {
    const element = document.createElement(tag);
    
    Object.entries(attrs).forEach(([key, value]) => {
        if (key === 'className') {
            element.className = value;
        } else if (key === 'dataset') {
            Object.entries(value).forEach(([dataKey, dataValue]) => {
                element.dataset[dataKey] = dataValue;
            });
        } else if (key.startsWith('on')) {
            const event = key.substring(2).toLowerCase();
            element.addEventListener(event, value);
        } else {
            element.setAttribute(key, value);
        }
    });
    
    if (typeof children === 'string') {
        element.textContent = children;
    } else if (Array.isArray(children)) {
        children.forEach(child => {
            if (typeof child === 'string') {
                element.appendChild(document.createTextNode(child));
            } else if (child instanceof HTMLElement) {
                element.appendChild(child);
            }
        });
    }
    
    return element;
}

// ============================================================================
// ARRAY UTILITIES
// ============================================================================

/**
 * Group array by field
 * @param {Array} items - Items to group
 * @param {string} field - Field to group by
 * @returns {Object} Grouped items
 */
export function groupBy(items, field) {
    return items.reduce((groups, item) => {
        const key = getNestedValue(item, field);
        if (!groups[key]) {
            groups[key] = [];
        }
        groups[key].push(item);
        return groups;
    }, {});
}

/**
 * Remove duplicates from array
 * @param {Array} items - Items array
 * @param {string} key - Key to check for uniqueness
 * @returns {Array} Array without duplicates
 */
export function uniqueBy(items, key) {
    const seen = new Set();
    return items.filter(item => {
        const value = getNestedValue(item, key);
        if (seen.has(value)) {
            return false;
        }
        seen.add(value);
        return true;
    });
}
