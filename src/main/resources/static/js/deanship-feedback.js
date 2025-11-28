/**
 * Feedback Components Module
 * Handles skeleton loaders, empty states, and loading indicators
 */

// ============================================================================
// LOADING UTILITIES
// ============================================================================

/**
 * Minimum loading time in milliseconds to prevent flickering shimmer effect
 * When data loads too quickly, the skeleton/shimmer effect appears as a flash
 * This ensures a smooth visual experience
 */
export const MIN_LOADING_TIME = 800;

/**
 * Execute an async operation with a minimum loading time
 * Prevents flickering when data loads too quickly
 * 
 * @param {Function|Promise} asyncOperation - Async function or promise to execute
 * @param {number} minTime - Minimum time in milliseconds (default: MIN_LOADING_TIME)
 * @returns {Promise} The result of the async operation
 * 
 * @example
 * // With async function
 * const data = await withMinLoadingTime(() => fetchData());
 * 
 * @example
 * // With promise
 * const data = await withMinLoadingTime(fetchData());
 * 
 * @example
 * // With custom minimum time
 * const data = await withMinLoadingTime(() => fetchData(), 1000);
 */
export async function withMinLoadingTime(asyncOperation, minTime = MIN_LOADING_TIME) {
    const startTime = Date.now();
    
    // Handle both functions and promises
    const promise = typeof asyncOperation === 'function' 
        ? asyncOperation() 
        : asyncOperation;
    
    const result = await promise;
    
    // Calculate remaining time to meet minimum loading duration
    const elapsedTime = Date.now() - startTime;
    const remainingTime = minTime - elapsedTime;
    
    // Wait for remaining time if data loaded too quickly
    if (remainingTime > 0) {
        await new Promise(resolve => setTimeout(resolve, remainingTime));
    }
    
    return result;
}

// ============================================================================
// SKELETON LOADERS
// ============================================================================

/**
 * Skeleton loader component with shimmer effect
 */
export class SkeletonLoader {
    /**
     * Create table skeleton loader
     * @param {number} rows - Number of skeleton rows to display
     * @param {number} columns - Number of columns in the table
     * @returns {string} HTML string for skeleton table rows
     */
    static table(rows = 5, columns = 6) {
        const skeletonRows = Array(rows).fill(0).map((_, rowIndex) => `
            <tr class="skeleton-table-row" style="display: table-row;">
                ${Array(columns).fill(0).map((_, colIndex) => {
                    // Vary widths for natural look
                    const widthPercent = colIndex === 0 ? 80 : 50 + Math.random() * 30;
                    return `
                        <td class="px-6 py-4">
                            <div class="skeleton-shimmer skeleton-text" style="width: ${widthPercent}%"></div>
                        </td>
                    `;
                }).join('')}
            </tr>
        `).join('');
        
        return skeletonRows;
    }
    
    /**
     * Create card skeleton loader for dashboard stats
     * @returns {string} HTML string for skeleton card
     */
    static card() {
        return `
            <div class="skeleton-stat-card">
                <div class="skeleton-stat-icon"></div>
                <div class="skeleton-stat-value"></div>
                <div class="skeleton-stat-label"></div>
            </div>
        `;
    }
    
    /**
     * Create chart skeleton loader
     * @param {number} height - Height in pixels (default 256)
     * @returns {string} HTML string for skeleton chart
     */
    static chart(height = 256) {
        const barWidths = [85, 72, 95, 60, 78, 88, 65, 92];
        return `
            <div class="skeleton-chart-container" style="height: ${height}px;">
                ${barWidths.map(width => `
                    <div class="skeleton-bar-row">
                        <div class="skeleton-bar-label"></div>
                        <div class="skeleton-bar-track">
                            <div class="skeleton-bar-fill" style="width: ${width}%"></div>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    }
    
    /**
     * Create list skeleton loader for activity feeds
     * @param {number} items - Number of list items
     * @returns {string} HTML string for skeleton list
     */
    static list(items = 5) {
        return Array(items).fill(0).map(() => `
            <div class="flex items-center space-x-3 py-3 border-b border-gray-100 dark:border-gray-700">
                <div class="skeleton-shimmer skeleton-circle w-10 h-10 flex-shrink-0"></div>
                <div class="flex-1 space-y-2">
                    <div class="skeleton-shimmer skeleton-text" style="width: 75%"></div>
                    <div class="skeleton-shimmer skeleton-text-sm" style="width: 50%"></div>
                </div>
            </div>
        `).join('');
    }

    /**
     * Create professional reports skeleton with stats, chart, and table
     * @param {number} rows - Number of table rows
     * @returns {string} HTML string for reports skeleton
     */
    static reports(rows = 8) {
        const barWidths = [85, 72, 95, 60, 78, 88, 65, 92, 70, 55];
        const progressWidths = [75, 88, 62, 95, 70, 82, 58, 90];
        
        return `
            <div class="reports-skeleton-container">
                <!-- Stats Summary Cards -->
                <div class="skeleton-stats-grid">
                    ${[1,2,3,4].map(() => `
                        <div class="skeleton-stat-card">
                            <div class="skeleton-stat-icon"></div>
                            <div class="skeleton-stat-value"></div>
                            <div class="skeleton-stat-label"></div>
                        </div>
                    `).join('')}
                </div>

                <!-- Bar Chart Skeleton -->
                <div class="skeleton-chart-container mb-8">
                    ${barWidths.slice(0, 8).map(width => `
                        <div class="skeleton-bar-row">
                            <div class="skeleton-bar-label"></div>
                            <div class="skeleton-bar-track">
                                <div class="skeleton-bar-fill" style="width: ${width}%"></div>
                            </div>
                        </div>
                    `).join('')}
                </div>

                <!-- Table Skeleton -->
                <div class="skeleton-table-container">
                    <div class="skeleton-table-header">
                        <div class="skeleton-table-header-cell" style="width: 70%"></div>
                        <div class="skeleton-table-header-cell" style="width: 60%"></div>
                        <div class="skeleton-table-header-cell" style="width: 60%"></div>
                        <div class="skeleton-table-header-cell" style="width: 60%"></div>
                        <div class="skeleton-table-header-cell" style="width: 60%"></div>
                        <div class="skeleton-table-header-cell" style="width: 80%"></div>
                    </div>
                    ${Array(rows).fill(0).map((_, i) => `
                        <div class="skeleton-table-row">
                            <div class="skeleton-table-cell" style="width: ${70 + Math.random() * 20}%"></div>
                            <div class="skeleton-table-cell" style="width: ${50 + Math.random() * 30}%"></div>
                            <div class="skeleton-table-cell" style="width: ${40 + Math.random() * 40}%"></div>
                            <div class="skeleton-table-cell" style="width: ${45 + Math.random() * 35}%"></div>
                            <div class="skeleton-table-cell" style="width: ${50 + Math.random() * 30}%"></div>
                            <div class="skeleton-progress-cell">
                                <div class="skeleton-progress-bar">
                                    <div class="skeleton-progress-fill" style="width: ${progressWidths[i] || 70}%"></div>
                                </div>
                                <div class="skeleton-progress-text"></div>
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    }
    
    /**
     * Create full table skeleton with header
     * @param {number} rows - Number of skeleton rows
     * @param {Array<string>} headers - Array of header labels
     * @returns {string} HTML string for complete skeleton table
     */
    static fullTable(rows = 5, headers = []) {
        const headerRow = headers.length > 0 ? `
            <thead class="bg-gray-50 dark:bg-gray-800">
                <tr>
                    ${headers.map(header => `
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                            ${header}
                        </th>
                    `).join('')}
                </tr>
            </thead>
        ` : '';
        
        return `
            ${headerRow}
            <tbody class="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700">
                ${this.table(rows, headers.length || 6)}
            </tbody>
        `;
    }
    
    /**
     * Show skeleton loader in a container
     * @param {string} containerId - ID of the container element
     * @param {string} type - Type of skeleton (table, card, chart, list, reports)
     * @param {Object} options - Additional options
     */
    static show(containerId, type = 'table', options = {}) {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        let skeletonHtml = '';
        
        switch (type) {
            case 'table':
                skeletonHtml = this.table(options.rows || 5, options.columns || 6);
                break;
            case 'card':
                skeletonHtml = this.card();
                break;
            case 'reports':
                skeletonHtml = this.reports(options.rows || 8);
                break;
            case 'chart':
                skeletonHtml = this.chart(options.height || 256);
                break;
            case 'list':
                skeletonHtml = this.list(options.items || 5);
                break;
            default:
                skeletonHtml = this.table();
        }
        
        container.innerHTML = skeletonHtml;
    }
}

/**
 * Empty state component with friendly illustrations
 */
export class EmptyState {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.options = {
            title: options.title || 'No Data Found',
            message: options.message || 'There is no data to display.',
            illustration: options.illustration || 'empty-box',
            actionLabel: options.actionLabel || null,
            actionCallback: options.actionCallback || null,
            actionId: options.actionId || null
        };
    }
    
    /**
     * Render empty state
     */
    render() {
        const container = document.getElementById(this.containerId);
        if (!container) return;
        
        const illustrationSvg = this._getIllustration(this.options.illustration);
        
        let html = `
            <div class="text-center py-12 px-4">
                <div class="mb-6">
                    ${illustrationSvg}
                </div>
                <h3 class="text-lg font-semibold text-gray-900 mb-2">${this.options.title}</h3>
                <p class="text-sm text-gray-500 max-w-md mx-auto mb-6">${this.options.message}</p>
        `;
        
        if (this.options.actionLabel && this.options.actionCallback) {
            const buttonId = this.options.actionId || `${this.containerId}_action`;
            html += `
                <button 
                    id="${buttonId}"
                    class="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors"
                >
                    <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
                    </svg>
                    ${this.options.actionLabel}
                </button>
            `;
        }
        
        html += `</div>`;
        
        container.innerHTML = html;
        
        // Attach event listener if action button exists
        if (this.options.actionLabel && this.options.actionCallback) {
            const buttonId = this.options.actionId || `${this.containerId}_action`;
            const actionBtn = document.getElementById(buttonId);
            if (actionBtn) {
                actionBtn.addEventListener('click', this.options.actionCallback);
            }
        }
    }
    
    /**
     * Get illustration SVG (undraw.co style)
     */
    _getIllustration(illustrationType) {
        const illustrations = {
            'empty-box': `
                <svg class="mx-auto h-48 w-48" viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <rect x="40" y="80" width="120" height="80" rx="4" fill="#E5E7EB" stroke="#9CA3AF" stroke-width="2"/>
                    <path d="M40 100 L100 80 L160 100" stroke="#9CA3AF" stroke-width="2" fill="none"/>
                    <line x1="100" y1="80" x2="100" y2="160" stroke="#9CA3AF" stroke-width="2"/>
                    <circle cx="100" cy="120" r="8" fill="#6B7280"/>
                </svg>
            `,
            'no-professors': `
                <svg class="mx-auto h-48 w-48" viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="100" cy="70" r="25" fill="#E5E7EB" stroke="#9CA3AF" stroke-width="2"/>
                    <path d="M60 140 Q60 110 100 110 Q140 110 140 140 L140 160 L60 160 Z" fill="#E5E7EB" stroke="#9CA3AF" stroke-width="2"/>
                    <line x1="75" y1="75" x2="125" y2="65" stroke="#9CA3AF" stroke-width="3" stroke-linecap="round"/>
                </svg>
            `,
            'no-courses': `
                <svg class="mx-auto h-48 w-48" viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <rect x="50" y="60" width="100" height="80" rx="4" fill="#E5E7EB" stroke="#9CA3AF" stroke-width="2"/>
                    <line x1="70" y1="80" x2="130" y2="80" stroke="#9CA3AF" stroke-width="2"/>
                    <line x1="70" y1="100" x2="130" y2="100" stroke="#9CA3AF" stroke-width="2"/>
                    <line x1="70" y1="120" x2="110" y2="120" stroke="#9CA3AF" stroke-width="2"/>
                    <path d="M50 60 L100 40 L150 60" stroke="#9CA3AF" stroke-width="2" fill="none"/>
                </svg>
            `,
            'no-assignments': `
                <svg class="mx-auto h-48 w-48" viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <rect x="60" y="50" width="80" height="100" rx="4" fill="#E5E7EB" stroke="#9CA3AF" stroke-width="2"/>
                    <rect x="70" y="40" width="60" height="20" rx="2" fill="#9CA3AF"/>
                    <line x1="75" y1="70" x2="125" y2="70" stroke="#9CA3AF" stroke-width="2"/>
                    <line x1="75" y1="90" x2="125" y2="90" stroke="#9CA3AF" stroke-width="2"/>
                    <line x1="75" y1="110" x2="125" y2="110" stroke="#9CA3AF" stroke-width="2"/>
                    <line x1="75" y1="130" x2="105" y2="130" stroke="#9CA3AF" stroke-width="2"/>
                </svg>
            `,
            'no-reports': `
                <svg class="mx-auto h-48 w-48" viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <rect x="50" y="40" width="100" height="120" rx="4" fill="#E5E7EB" stroke="#9CA3AF" stroke-width="2"/>
                    <rect x="70" y="60" width="20" height="40" fill="#9CA3AF"/>
                    <rect x="100" y="80" width="20" height="20" fill="#9CA3AF"/>
                    <rect x="70" y="110" width="60" height="2" fill="#9CA3AF"/>
                    <rect x="70" y="120" width="60" height="2" fill="#9CA3AF"/>
                    <rect x="70" y="130" width="40" height="2" fill="#9CA3AF"/>
                </svg>
            `,
            'no-files': `
                <svg class="mx-auto h-48 w-48" viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M60 50 L60 150 L140 150 L140 80 L110 50 Z" fill="#E5E7EB" stroke="#9CA3AF" stroke-width="2"/>
                    <path d="M110 50 L110 80 L140 80" fill="#D1D5DB" stroke="#9CA3AF" stroke-width="2"/>
                    <line x1="75" y1="100" x2="125" y2="100" stroke="#9CA3AF" stroke-width="2"/>
                    <line x1="75" y1="115" x2="125" y2="115" stroke="#9CA3AF" stroke-width="2"/>
                    <line x1="75" y1="130" x2="105" y2="130" stroke="#9CA3AF" stroke-width="2"/>
                </svg>
            `,
            'no-search-results': `
                <svg class="mx-auto h-48 w-48" viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="85" cy="85" r="35" fill="none" stroke="#9CA3AF" stroke-width="3"/>
                    <line x1="110" y1="110" x2="140" y2="140" stroke="#9CA3AF" stroke-width="3" stroke-linecap="round"/>
                    <line x1="70" y1="85" x2="100" y2="85" stroke="#E5E7EB" stroke-width="3" stroke-linecap="round"/>
                    <line x1="85" y1="70" x2="85" y2="100" stroke="#E5E7EB" stroke-width="3" stroke-linecap="round"/>
                </svg>
            `,
            'no-academic-years': `
                <svg class="mx-auto h-48 w-48" viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <rect x="50" y="60" width="100" height="90" rx="4" fill="#E5E7EB" stroke="#9CA3AF" stroke-width="2"/>
                    <rect x="50" y="60" width="100" height="20" fill="#9CA3AF"/>
                    <circle cx="75" cy="90" r="3" fill="#6B7280"/>
                    <circle cx="100" cy="90" r="3" fill="#6B7280"/>
                    <circle cx="125" cy="90" r="3" fill="#6B7280"/>
                    <circle cx="75" cy="110" r="3" fill="#6B7280"/>
                    <circle cx="100" cy="110" r="3" fill="#6B7280"/>
                    <circle cx="125" cy="110" r="3" fill="#6B7280"/>
                    <circle cx="75" cy="130" r="3" fill="#6B7280"/>
                    <circle cx="100" cy="130" r="3" fill="#6B7280"/>
                </svg>
            `
        };
        
        return illustrations[illustrationType] || illustrations['empty-box'];
    }
    
    /**
     * Static method to quickly render an empty state
     * @param {string} containerId - Container element ID
     * @param {Object} options - Empty state options
     */
    static render(containerId, options = {}) {
        const emptyState = new EmptyState(containerId, options);
        emptyState.render();
    }
}

/**
 * Loading indicator component
 */
export class LoadingIndicator {
    constructor(containerId, options = {}) {
        this.containerId = containerId;
        this.options = {
            message: options.message || 'Loading...',
            minDisplayTime: options.minDisplayTime || 500
        };
        this.startTime = null;
    }
    
    /**
     * Show loading indicator
     */
    show() {
        this.startTime = Date.now();
        const container = document.getElementById(this.containerId);
        if (!container) return;
        
        container.innerHTML = `
            <div class="flex items-center justify-center py-8">
                <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                <span class="ml-3 text-gray-600">${this.options.message}</span>
            </div>
        `;
    }
    
    /**
     * Hide loading indicator (respects minimum display time)
     */
    async hide() {
        if (this.startTime) {
            const elapsed = Date.now() - this.startTime;
            const remaining = this.options.minDisplayTime - elapsed;
            
            if (remaining > 0) {
                await new Promise(resolve => setTimeout(resolve, remaining));
            }
        }
        
        const container = document.getElementById(this.containerId);
        if (container) {
            container.innerHTML = '';
        }
    }
}

/**
 * Enhanced Toast Notification System
 * Supports stacking, progress bars, action buttons, and pause on hover
 */
export class EnhancedToast {
    static toasts = [];
    static maxToasts = 5;
    static toastIdCounter = 0;
    
    /**
     * Show an enhanced toast notification
     * @param {string} message - Toast message
     * @param {string} type - Toast type (success, error, info, warning)
     * @param {Object} options - Additional options
     */
    static show(message, type = 'info', options = {}) {
        const {
            duration = 5000,
            action = null,
            actionLabel = null,
            onAction = null,
            pauseOnHover = true
        } = options;
        
        const toastContainer = document.getElementById('toastContainer');
        if (!toastContainer) return null;
        
        // Remove oldest toast if we've reached the limit
        if (this.toasts.length >= this.maxToasts) {
            const oldestToast = this.toasts.shift();
            if (oldestToast && oldestToast.element) {
                this.removeToast(oldestToast.element, oldestToast.id);
            }
        }
        
        const toastId = ++this.toastIdCounter;
        const toast = document.createElement('div');
        toast.className = `toast-notification toast-${type}`;
        toast.setAttribute('role', 'alert');
        toast.setAttribute('data-toast-id', toastId);
        
        const icon = this.getToastIcon(type);
        const formattedMessage = message.replace(/\n/g, '<br>');
        
        let actionButton = '';
        if (action && actionLabel && onAction) {
            actionButton = `
                <button class="toast-action-btn" data-action="${action}">
                    ${actionLabel}
                </button>
            `;
        }
        
        toast.innerHTML = `
            <div class="toast-content">
                <div class="toast-icon">${icon}</div>
                <div class="toast-message">${formattedMessage}</div>
                ${actionButton}
                <button class="toast-close-btn" aria-label="Close notification">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                </button>
            </div>
            <div class="toast-progress-bar">
                <div class="toast-progress-fill"></div>
            </div>
        `;
        
        toastContainer.appendChild(toast);
        
        // Trigger slide-in animation
        setTimeout(() => toast.classList.add('toast-visible'), 10);
        
        // Store toast reference
        const toastData = {
            id: toastId,
            element: toast,
            duration: duration,
            startTime: Date.now(),
            remainingTime: duration,
            isPaused: false,
            timer: null,
            progressInterval: null
        };
        this.toasts.push(toastData);
        
        // Setup close button
        const closeBtn = toast.querySelector('.toast-close-btn');
        closeBtn.addEventListener('click', () => this.removeToast(toast, toastId));
        
        // Setup action button if exists
        if (action && onAction) {
            const actionBtn = toast.querySelector('.toast-action-btn');
            if (actionBtn) {
                actionBtn.addEventListener('click', () => {
                    onAction();
                    this.removeToast(toast, toastId);
                });
            }
        }
        
        // Setup pause on hover
        if (pauseOnHover) {
            toast.addEventListener('mouseenter', () => this.pauseToast(toastId));
            toast.addEventListener('mouseleave', () => this.resumeToast(toastId));
        }
        
        // Start auto-dismiss timer and progress bar
        this.startToastTimer(toastId);
        
        return toastId;
    }
    
    /**
     * Start toast timer and progress bar animation
     */
    static startToastTimer(toastId) {
        const toastData = this.toasts.find(t => t.id === toastId);
        if (!toastData) return;
        
        const progressFill = toastData.element.querySelector('.toast-progress-fill');
        if (!progressFill) return;
        
        // Start progress bar animation
        progressFill.style.transition = `width ${toastData.remainingTime}ms linear`;
        progressFill.style.width = '0%';
        
        // Set auto-dismiss timer
        toastData.timer = setTimeout(() => {
            this.removeToast(toastData.element, toastId);
        }, toastData.remainingTime);
    }
    
    /**
     * Pause toast auto-dismiss
     */
    static pauseToast(toastId) {
        const toastData = this.toasts.find(t => t.id === toastId);
        if (!toastData || toastData.isPaused) return;
        
        toastData.isPaused = true;
        const elapsed = Date.now() - toastData.startTime;
        toastData.remainingTime = Math.max(0, toastData.duration - elapsed);
        
        // Clear timer
        if (toastData.timer) {
            clearTimeout(toastData.timer);
            toastData.timer = null;
        }
        
        // Pause progress bar
        const progressFill = toastData.element.querySelector('.toast-progress-fill');
        if (progressFill) {
            const computedStyle = window.getComputedStyle(progressFill);
            const currentWidth = computedStyle.width;
            progressFill.style.transition = 'none';
            progressFill.style.width = currentWidth;
        }
    }
    
    /**
     * Resume toast auto-dismiss
     */
    static resumeToast(toastId) {
        const toastData = this.toasts.find(t => t.id === toastId);
        if (!toastData || !toastData.isPaused) return;
        
        toastData.isPaused = false;
        toastData.startTime = Date.now();
        
        // Resume timer and progress bar
        this.startToastTimer(toastId);
    }
    
    /**
     * Remove toast with animation
     */
    static removeToast(toastElement, toastId) {
        if (!toastElement) return;
        
        // Clear timers
        const toastData = this.toasts.find(t => t.id === toastId);
        if (toastData) {
            if (toastData.timer) clearTimeout(toastData.timer);
            if (toastData.progressInterval) clearInterval(toastData.progressInterval);
            this.toasts = this.toasts.filter(t => t.id !== toastId);
        }
        
        // Slide out animation
        toastElement.classList.remove('toast-visible');
        toastElement.classList.add('toast-removing');
        
        setTimeout(() => {
            if (toastElement.parentNode) {
                toastElement.remove();
            }
        }, 300);
    }
    
    /**
     * Get toast icon SVG
     */
    static getToastIcon(type) {
        const icons = {
            success: `<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
            </svg>`,
            error: `<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>`,
            warning: `<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
            </svg>`,
            info: `<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>`
        };
        return icons[type] || icons.info;
    }
    
    /**
     * Clear all toasts
     */
    static clearAll() {
        this.toasts.forEach(toastData => {
            if (toastData.element) {
                this.removeToast(toastData.element, toastData.id);
            }
        });
        this.toasts = [];
    }
}

/**
 * Tooltip component for action buttons
 */
export class Tooltip {
    static init() {
        // Add tooltips to all elements with title attribute
        document.addEventListener('mouseover', (e) => {
            const target = e.target.closest('[title], [data-tooltip]');
            if (!target) return;
            
            const tooltipText = target.getAttribute('title') || target.getAttribute('data-tooltip');
            if (!tooltipText) return;
            
            // Remove title to prevent default browser tooltip
            if (target.hasAttribute('title')) {
                target.setAttribute('data-tooltip', tooltipText);
                target.removeAttribute('title');
            }
            
            this.show(target, tooltipText);
        });
        
        document.addEventListener('mouseout', (e) => {
            const target = e.target.closest('[data-tooltip]');
            if (!target) return;
            
            this.hide();
        });
    }
    
    /**
     * Show tooltip
     */
    static show(element, text) {
        // Remove existing tooltip
        this.hide();
        
        const tooltip = document.createElement('div');
        tooltip.className = 'custom-tooltip';
        tooltip.textContent = text;
        tooltip.id = 'activeTooltip';
        
        document.body.appendChild(tooltip);
        
        // Position tooltip
        const rect = element.getBoundingClientRect();
        const tooltipRect = tooltip.getBoundingClientRect();
        
        let top = rect.top - tooltipRect.height - 8;
        let left = rect.left + (rect.width / 2) - (tooltipRect.width / 2);
        
        // Adjust if tooltip goes off screen
        if (top < 0) {
            top = rect.bottom + 8;
            tooltip.classList.add('tooltip-bottom');
        }
        
        if (left < 0) {
            left = 8;
        } else if (left + tooltipRect.width > window.innerWidth) {
            left = window.innerWidth - tooltipRect.width - 8;
        }
        
        tooltip.style.top = `${top}px`;
        tooltip.style.left = `${left}px`;
        
        // Fade in
        setTimeout(() => tooltip.classList.add('tooltip-visible'), 10);
    }
    
    /**
     * Hide tooltip
     */
    static hide() {
        const tooltip = document.getElementById('activeTooltip');
        if (tooltip) {
            tooltip.remove();
        }
    }
}

// Initialize tooltips on DOM load
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => Tooltip.init());
} else {
    Tooltip.init();
}

export { SkeletonLoader as default };
