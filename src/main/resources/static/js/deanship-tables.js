/**
 * Deanship Dashboard - Enhanced Table Components
 * Provides advanced table features including filtering, bulk actions, and visual enhancements
 */

/**
 * Utility: Debounce function (Task 12.3: Debouncing)
 * Delays function execution until after wait milliseconds have elapsed since last call
 * @param {Function} func - Function to debounce
 * @param {number} wait - Milliseconds to wait
 * @returns {Function} Debounced function
 */
function debounce(func, wait = 300) {
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
 * Utility: Throttle function (Task 12.3: Throttling)
 * Ensures function is called at most once per specified time period
 * @param {Function} func - Function to throttle
 * @param {number} limit - Milliseconds between calls
 * @returns {Function} Throttled function
 */
function throttle(func, limit = 100) {
    let inThrottle;
    return function executedFunction(...args) {
        if (!inThrottle) {
            func(...args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

/**
 * MultiSelectFilter Component
 * Provides a dropdown with checkboxes for multi-value filtering
 */
class MultiSelectFilter {
    constructor(options) {
        this.options = options || [];
        this.selectedValues = new Set();
        this.onChange = null;
        this.debounceTimer = null;
        this.container = null;
        this.isOpen = false;
        // Task 12.3: Debounce filter changes with 300ms delay
        this.applyFilterDebounced = debounce(() => this.applyFilter(), 300);
    }

    /**
     * Render the multi-select filter
     * @param {HTMLElement} container - Container element
     * @param {string} label - Filter label
     * @param {Array} options - Array of {value, label} objects
     * @param {Function} onChange - Callback when selection changes
     */
    render(container, label, options, onChange) {
        this.options = options;
        this.onChange = onChange;
        this.container = container;

        const filterHtml = `
            <div class="multi-select-filter">
                <button class="filter-toggle" type="button" aria-haspopup="true" aria-expanded="false">
                    <i class="fas fa-filter"></i>
                    ${label}
                    <span class="selected-count" style="display: none;">0</span>
                    <i class="fas fa-chevron-down"></i>
                </button>
                <div class="filter-dropdown" style="display: none;">
                    <div class="filter-header">
                        <button class="filter-action" data-action="select-all" type="button">Select All</button>
                        <button class="filter-action" data-action="clear-all" type="button">Clear All</button>
                    </div>
                    <div class="filter-options">
                        ${options.map(opt => `
                            <label class="filter-option">
                                <input type="checkbox" value="${opt.value}" />
                                <span>${opt.label}</span>
                            </label>
                        `).join('')}
                    </div>
                </div>
            </div>
        `;

        container.innerHTML = filterHtml;
        this.attachEventListeners();
    }

    attachEventListeners() {
        const toggle = this.container.querySelector('.filter-toggle');
        const dropdown = this.container.querySelector('.filter-dropdown');
        const checkboxes = this.container.querySelectorAll('input[type="checkbox"]');
        const selectAll = this.container.querySelector('[data-action="select-all"]');
        const clearAll = this.container.querySelector('[data-action="clear-all"]');

        // Toggle dropdown
        toggle.addEventListener('click', (e) => {
            e.stopPropagation();
            this.isOpen = !this.isOpen;
            dropdown.style.display = this.isOpen ? 'block' : 'none';
            toggle.setAttribute('aria-expanded', this.isOpen);
        });

        // Close on outside click
        document.addEventListener('click', (e) => {
            if (this.isOpen && !this.container.contains(e.target)) {
                this.isOpen = false;
                dropdown.style.display = 'none';
                toggle.setAttribute('aria-expanded', 'false');
            }
        });

        // Handle checkbox changes
        checkboxes.forEach(cb => {
            cb.addEventListener('change', () => {
                if (cb.checked) {
                    this.selectedValues.add(cb.value);
                } else {
                    this.selectedValues.delete(cb.value);
                }
                this.updateSelectedCount();
                this.applyFilterDebounced();
            });
        });

        // Select all
        selectAll.addEventListener('click', () => {
            checkboxes.forEach(cb => {
                cb.checked = true;
                this.selectedValues.add(cb.value);
            });
            this.updateSelectedCount();
            this.applyFilterDebounced();
        });

        // Clear all
        clearAll.addEventListener('click', () => {
            checkboxes.forEach(cb => {
                cb.checked = false;
            });
            this.selectedValues.clear();
            this.updateSelectedCount();
            this.applyFilterDebounced();
        });
    }

    updateSelectedCount() {
        const badge = this.container.querySelector('.selected-count');
        if (this.selectedValues.size > 0) {
            badge.textContent = this.selectedValues.size;
            badge.style.display = 'inline-block';
        } else {
            badge.style.display = 'none';
        }
    }

    applyFilterDebounced() {
        clearTimeout(this.debounceTimer);
        this.debounceTimer = setTimeout(() => {
            if (this.onChange) {
                this.onChange(Array.from(this.selectedValues));
            }
        }, 300);
    }

    getSelectedValues() {
        return Array.from(this.selectedValues);
    }

    reset() {
        this.selectedValues.clear();
        const checkboxes = this.container.querySelectorAll('input[type="checkbox"]');
        checkboxes.forEach(cb => cb.checked = false);
        this.updateSelectedCount();
    }
}

/**
 * DateRangeFilter Component
 * Provides date range filtering with presets
 */
class DateRangeFilter {
    constructor() {
        this.startDate = null;
        this.endDate = null;
        this.onChange = null;
        this.container = null;
    }

    /**
     * Render the date range filter
     * @param {HTMLElement} container - Container element
     * @param {Function} onChange - Callback when date range changes
     */
    render(container, onChange) {
        this.onChange = onChange;
        this.container = container;

        const filterHtml = `
            <div class="date-range-filter">
                <div class="date-inputs">
                    <input type="date" class="date-input" id="start-date" aria-label="Start date" />
                    <span>to</span>
                    <input type="date" class="date-input" id="end-date" aria-label="End date" />
                    <button class="clear-date-btn" type="button" title="Clear dates">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="date-presets">
                    <button class="preset-btn" data-preset="7days" type="button">Last 7 days</button>
                    <button class="preset-btn" data-preset="30days" type="button">Last 30 days</button>
                    <button class="preset-btn" data-preset="semester" type="button">This semester</button>
                </div>
            </div>
        `;

        container.innerHTML = filterHtml;
        this.attachEventListeners();
    }

    attachEventListeners() {
        const startInput = this.container.querySelector('#start-date');
        const endInput = this.container.querySelector('#end-date');
        const clearBtn = this.container.querySelector('.clear-date-btn');
        const presetBtns = this.container.querySelectorAll('.preset-btn');

        // Date input changes
        startInput.addEventListener('change', () => {
            this.startDate = startInput.value;
            this.validateAndApply();
        });

        endInput.addEventListener('change', () => {
            this.endDate = endInput.value;
            this.validateAndApply();
        });

        // Clear button
        clearBtn.addEventListener('click', () => {
            this.reset();
            if (this.onChange) {
                this.onChange(null, null);
            }
        });

        // Preset buttons
        presetBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const preset = btn.dataset.preset;
                this.applyPreset(preset);
            });
        });
    }

    validateAndApply() {
        if (this.startDate && this.endDate) {
            const start = new Date(this.startDate);
            const end = new Date(this.endDate);

            if (start > end) {
                showToast('Start date must be before end date', 'error');
                return;
            }
        }

        if (this.onChange) {
            this.onChange(this.startDate, this.endDate);
        }
    }

    applyPreset(preset) {
        const today = new Date();
        const startInput = this.container.querySelector('#start-date');
        const endInput = this.container.querySelector('#end-date');

        switch (preset) {
            case '7days':
                this.startDate = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
                this.endDate = today.toISOString().split('T')[0];
                break;
            case '30days':
                this.startDate = new Date(today.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
                this.endDate = today.toISOString().split('T')[0];
                break;
            case 'semester':
                // Assume semester starts in September or February
                const month = today.getMonth();
                const year = today.getFullYear();
                if (month >= 8) { // Sep-Dec
                    this.startDate = `${year}-09-01`;
                } else if (month >= 1) { // Feb-Aug
                    this.startDate = `${year}-02-01`;
                } else { // January
                    this.startDate = `${year - 1}-09-01`;
                }
                this.endDate = today.toISOString().split('T')[0];
                break;
        }

        startInput.value = this.startDate;
        endInput.value = this.endDate;
        this.validateAndApply();
    }

    reset() {
        this.startDate = null;
        this.endDate = null;
        const startInput = this.container.querySelector('#start-date');
        const endInput = this.container.querySelector('#end-date');
        if (startInput) startInput.value = '';
        if (endInput) endInput.value = '';
    }

    getDateRange() {
        return {
            start: this.startDate,
            end: this.endDate
        };
    }
}

/**
 * BulkActionsToolbar Component
 * Provides bulk operations for selected table rows
 */
class BulkActionsToolbar {
    constructor() {
        this.selectedCount = 0;
        this.container = null;
        this.onAction = null;
    }

    /**
     * Render the bulk actions toolbar
     * @param {HTMLElement} container - Container element
     * @param {Function} onAction - Callback when action is triggered
     */
    render(container, onAction) {
        this.container = container;
        this.onAction = onAction;

        const toolbarHtml = `
            <div class="bulk-actions-toolbar" style="display: none;">
                <div class="toolbar-content">
                    <span class="selected-count-text">
                        <strong class="count">0</strong> items selected
                    </span>
                    <div class="toolbar-actions">
                        <button class="bulk-action-btn" data-action="activate" type="button">
                            <i class="fas fa-check-circle"></i> Activate
                        </button>
                        <button class="bulk-action-btn" data-action="deactivate" type="button">
                            <i class="fas fa-ban"></i> Deactivate
                        </button>
                        <button class="bulk-action-btn danger" data-action="delete" type="button">
                            <i class="fas fa-trash"></i> Delete
                        </button>
                    </div>
                    <button class="close-toolbar-btn" type="button" title="Clear selection">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
            </div>
        `;

        container.innerHTML = toolbarHtml;
        this.attachEventListeners();
    }

    attachEventListeners() {
        const actionBtns = this.container.querySelectorAll('.bulk-action-btn');
        const closeBtn = this.container.querySelector('.close-toolbar-btn');

        actionBtns.forEach(btn => {
            btn.addEventListener('click', async () => {
                const action = btn.dataset.action;

                // Confirmation for destructive actions
                if (action === 'delete') {
                    const confirmed = await this.showConfirmDialog(
                        `Are you sure you want to delete ${this.selectedCount} item(s)?`,
                        'This action cannot be undone.'
                    );
                    if (!confirmed) return;
                }

                if (this.onAction) {
                    this.onAction(action);
                }
            });
        });

        closeBtn.addEventListener('click', () => {
            this.hide();
            if (this.onAction) {
                this.onAction('clear');
            }
        });
    }

    show(count) {
        this.selectedCount = count;
        const toolbar = this.container.querySelector('.bulk-actions-toolbar');
        const countEl = this.container.querySelector('.count');

        if (count > 0) {
            countEl.textContent = count;
            toolbar.style.display = 'block';
            // Slide in animation
            setTimeout(() => toolbar.classList.add('visible'), 10);
        } else {
            this.hide();
        }
    }

    hide() {
        const toolbar = this.container.querySelector('.bulk-actions-toolbar');
        toolbar.classList.remove('visible');
        setTimeout(() => toolbar.style.display = 'none', 300);
        this.selectedCount = 0;
    }

    async showConfirmDialog(title, message) {
        return new Promise((resolve) => {
            const confirmed = confirm(`${title}\n\n${message}`);
            resolve(confirmed);
        });
    }
}

/**
 * UserAvatar Component
 * Displays user avatars with initials and color coding
 */
class UserAvatar {
    /**
     * Generate avatar HTML
     * @param {string} name - User's full name
     * @param {string} size - Avatar size (sm, md, lg)
     * @param {string} imageUrl - Optional image URL
     * @returns {string} Avatar HTML
     */
    static generate(name, size = 'md', imageUrl = null) {
        const initials = this.getInitials(name);
        const color = this.getColorFromName(name);
        const sizeClass = `avatar-${size}`;

        if (imageUrl) {
            return `
                <div class="user-avatar ${sizeClass}" style="background-color: ${color};">
                    <img src="${imageUrl}" alt="${name}" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';" />
                    <span class="avatar-initials" style="display: none;">${initials}</span>
                </div>
            `;
        }

        return `
            <div class="user-avatar ${sizeClass}" style="background-color: ${color};">
                <span class="avatar-initials">${initials}</span>
            </div>
        `;
    }

    static getInitials(name) {
        if (!name) return '?';
        const parts = name.trim().split(' ');
        if (parts.length >= 2) {
            return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
        }
        return name.substring(0, 2).toUpperCase();
    }

    static getColorFromName(name) {
        // Generate consistent color based on name hash
        const colors = [
            '#3498db', '#e74c3c', '#2ecc71', '#f39c12', '#9b59b6',
            '#1abc9c', '#34495e', '#e67e22', '#95a5a6', '#16a085'
        ];

        // Handle null or undefined names
        if (!name || typeof name !== 'string' || name.length === 0) {
            return colors[0]; // Default to first color
        }

        let hash = 0;
        for (let i = 0; i < name.length; i++) {
            hash = name.charCodeAt(i) + ((hash << 5) - hash);
        }

        return colors[Math.abs(hash) % colors.length];
    }
}

/**
 * TableProgressBar Component
 * Displays color-coded progress bars for completion tracking
 */
class TableProgressBar {
    /**
     * Generate progress bar HTML
     * @param {number} percentage - Completion percentage (0-100)
     * @param {string} tooltip - Optional tooltip text
     * @returns {string} Progress bar HTML
     */
    static generate(percentage, tooltip = null) {
        const color = this.getColorForPercentage(percentage);
        const tooltipAttr = tooltip ? `title="${tooltip}"` : '';

        return `
            <div class="table-progress-bar" ${tooltipAttr}>
                <div class="flex items-center gap-3">
                    <div class="flex-1 bg-gray-200 rounded-full h-2.5 overflow-hidden">
                        <div class="h-2.5 rounded-full transition-all duration-500" 
                             style="width: ${percentage}%; background-color: ${color};"></div>
                    </div>
                    <span class="text-xs font-medium text-gray-700 w-12 text-right">${percentage}%</span>
                </div>
            </div>
        `;
    }

    static getColorForPercentage(percentage) {
        if (percentage < 50) return '#e74c3c'; // Red
        if (percentage < 80) return '#f39c12'; // Yellow
        return '#2ecc71'; // Green
    }

    /**
     * Animate progress bar on render
     * @param {HTMLElement} element - Progress bar element
     */
    static animate(element) {
        const fill = element.querySelector('.progress-bar-fill');
        if (fill) {
            const targetWidth = fill.style.width;
            fill.style.width = '0%';
            setTimeout(() => {
                fill.style.transition = 'width 0.8s ease-out';
                fill.style.width = targetWidth;
            }, 100);
        }
    }
}

// Export components
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        MultiSelectFilter,
        DateRangeFilter,
        BulkActionsToolbar,
        UserAvatar,
        TableProgressBar
    };
}


/**
 * Table Export Helper
 * Adds export buttons and functionality to data tables
 */
class TableExportHelper {
    /**
     * Add export buttons to a table container
     * @param {string} containerId - Container element ID
     * @param {string} tableId - Table element ID
     * @param {string} tableName - Name for export file
     */
    static addExportButtons(containerId, tableId, tableName) {
        const container = document.getElementById(containerId);
        if (!container) return;

        // Check if export buttons already exist
        if (container.querySelector('.table-export-buttons')) {
            return;
        }

        // Create export buttons container
        const exportDiv = document.createElement('div');
        exportDiv.className = 'table-export-buttons flex gap-2 mb-4';
        exportDiv.innerHTML = `
            <button class="export-table-pdf px-3 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors flex items-center gap-2 text-sm"
                    title="Export table to PDF">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                </svg>
                Export PDF
            </button>
            <button class="export-table-excel px-3 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors flex items-center gap-2 text-sm"
                    title="Export table to Excel">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                Export Excel
            </button>
        `;

        // Insert before the table
        const table = document.getElementById(tableId);
        if (table) {
            table.parentElement.insertBefore(exportDiv, table);
        } else {
            container.insertBefore(exportDiv, container.firstChild);
        }

        // Attach event listeners
        exportDiv.querySelector('.export-table-pdf').addEventListener('click', () => {
            this.exportTable(tableId, tableName, 'pdf');
        });

        exportDiv.querySelector('.export-table-excel').addEventListener('click', () => {
            this.exportTable(tableId, tableName, 'excel');
        });
    }

    /**
     * Export table data
     * @param {string} tableId - Table element ID
     * @param {string} tableName - Name for export file
     * @param {string} format - 'pdf' or 'excel'
     */
    static async exportTable(tableId, tableName, format) {
        const table = document.getElementById(tableId);
        if (!table) {
            window.showToast?.('Table not found', 'error');
            return;
        }

        try {
            // Extract table data
            const { columns, data } = this.extractTableData(table);

            if (data.length === 0) {
                window.showToast?.('No data to export', 'warning');
                return;
            }

            // Show loading indicator
            const loadingToast = window.showToast?.('Generating export...', 'info', 0);

            // Export using ExportService
            if (format === 'pdf') {
                await window.ExportService.exportTableToPDF(data, columns, tableName);
            } else if (format === 'excel') {
                await window.ExportService.exportTableToExcel(data, columns, tableName);
            }

            // Close loading toast
            if (loadingToast && typeof loadingToast.close === 'function') {
                loadingToast.close();
            }

            window.showToast?.(`Table exported successfully as ${format.toUpperCase()}`, 'success');
        } catch (error) {
            console.error('Table export error:', error);
            window.showToast?.(`Export failed: ${error.message}`, 'error');
        }
    }

    /**
     * Extract data from HTML table
     * @param {HTMLElement} table - Table element
     * @returns {Object} Object with columns and data arrays
     */
    static extractTableData(table) {
        const columns = [];
        const data = [];

        // Extract headers (skip checkbox column if present)
        const headerRow = table.querySelector('thead tr');
        if (headerRow) {
            headerRow.querySelectorAll('th').forEach((th, index) => {
                // Skip checkbox columns
                if (!th.querySelector('input[type="checkbox"]')) {
                    const text = th.textContent.trim();
                    if (text && text !== 'Actions') {
                        columns.push(text);
                    }
                }
            });
        }

        // Extract data rows
        const bodyRows = table.querySelectorAll('tbody tr');
        bodyRows.forEach(row => {
            const rowData = [];
            row.querySelectorAll('td').forEach((td, index) => {
                // Skip checkbox and action columns
                if (!td.querySelector('input[type="checkbox"]') && !td.classList.contains('actions-cell')) {
                    // Get text content, handling special cases
                    let text = td.textContent.trim();

                    // Handle progress bars - extract percentage
                    const progressBar = td.querySelector('.progress-bar-text');
                    if (progressBar) {
                        text = progressBar.textContent.trim();
                    }

                    // Handle badges
                    const badge = td.querySelector('.badge');
                    if (badge) {
                        text = badge.textContent.trim();
                    }

                    if (text) {
                        rowData.push(text);
                    }
                }
            });

            if (rowData.length > 0) {
                data.push(rowData);
            }
        });

        return { columns, data };
    }

    /**
     * Add export buttons to professors table
     */
    static addToProfessorsTable() {
        this.addExportButtons('professorsTableContainer', 'professorsTable', 'Professors List');
    }

    /**
     * Add export buttons to courses table
     */
    static addToCoursesTable() {
        this.addExportButtons('coursesTableContainer', 'coursesTable', 'Courses List');
    }

    /**
     * Add export buttons to assignments table
     */
    static addToAssignmentsTable() {
        this.addExportButtons('assignmentsTableContainer', 'assignmentsTable', 'Assignments List');
    }
}

// Make TableExportHelper available globally
window.TableExportHelper = TableExportHelper;
