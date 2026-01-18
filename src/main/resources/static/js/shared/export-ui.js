/**
 * Export UI Components
 * Modal dialogs, progress indicators, and toast notifications for export functionality
 * 
 * @requires report-export-service.js
 */

// ============================================================================
// EXPORT MODAL COMPONENT
// ============================================================================

class ExportModal {
    constructor() {
        this.modal = null;
        this.progressBar = null;
        this.progressText = null;
        this.progressPercent = null;
        this.progressIcon = null;
        this.isOpen = false;
    }
    
    /**
     * Create and inject the modal HTML
     */
    init() {
        if (this.modal) return;
        
        const modalHTML = `
            <div id="exportModal" class="export-modal-overlay">
                <div class="export-modal">
                    <div class="export-modal-header">
                        <h3 class="export-modal-title">Exporting Report</h3>
                        <button class="export-modal-close" onclick="window.exportModal.close()" aria-label="Close">
                            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                            </svg>
                        </button>
                    </div>
                    <div class="export-modal-body">
                        <div class="export-progress-container">
                            <div class="export-progress-icon" id="exportProgressIcon">
                                <div class="export-progress-spinner"></div>
                                <div class="export-progress-checkmark">
                                    <svg width="32" height="32" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/>
                                    </svg>
                                </div>
                            </div>
                            <div class="export-progress-percent" id="exportProgressPercent">0%</div>
                            <div class="export-progress-bar-container">
                                <div class="export-progress-bar" id="exportProgressBar" style="width: 0%"></div>
                            </div>
                            <div class="export-progress-text" id="exportProgressText">Initializing...</div>
                        </div>
                        <div id="exportSuccessMessage" class="export-success-message" style="display: none;">
                            Export completed successfully!
                        </div>
                        <div id="exportErrorMessage" class="export-error-message" style="display: none;"></div>
                    </div>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', modalHTML);
        
        this.modal = document.getElementById('exportModal');
        this.progressBar = document.getElementById('exportProgressBar');
        this.progressText = document.getElementById('exportProgressText');
        this.progressPercent = document.getElementById('exportProgressPercent');
        this.progressIcon = document.getElementById('exportProgressIcon');
        this.successMessage = document.getElementById('exportSuccessMessage');
        this.errorMessage = document.getElementById('exportErrorMessage');
        
        // Close on overlay click
        this.modal.addEventListener('click', (e) => {
            if (e.target === this.modal) {
                this.close();
            }
        });
        
        // Close on escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.isOpen) {
                this.close();
            }
        });
    }
    
    /**
     * Show the modal
     */
    open(title = 'Exporting Report') {
        this.init();
        
        // Reset state
        this.progressBar.style.width = '0%';
        this.progressPercent.textContent = '0%';
        this.progressText.textContent = 'Initializing...';
        this.progressIcon.className = 'export-progress-icon';
        this.successMessage.style.display = 'none';
        this.errorMessage.style.display = 'none';
        
        // Update title
        this.modal.querySelector('.export-modal-title').textContent = title;
        
        // Show modal
        this.modal.classList.add('show');
        this.isOpen = true;
        
        // Prevent body scroll
        document.body.style.overflow = 'hidden';
    }
    
    /**
     * Close the modal
     */
    close() {
        if (!this.modal) return;
        
        this.modal.classList.remove('show');
        this.isOpen = false;
        
        // Restore body scroll
        document.body.style.overflow = '';
    }
    
    /**
     * Update progress
     * @param {number} percent - Progress percentage (0-100)
     * @param {string} message - Progress message
     */
    updateProgress(percent, message) {
        if (!this.progressBar) return;
        
        this.progressBar.style.width = `${percent}%`;
        this.progressPercent.textContent = `${Math.round(percent)}%`;
        
        if (message) {
            this.progressText.textContent = message;
        }
    }
    
    /**
     * Show success state
     * @param {string} message - Success message
     */
    showSuccess(message = 'Export completed successfully!') {
        if (!this.progressIcon) return;
        
        this.progressIcon.classList.add('success');
        this.progressBar.style.width = '100%';
        this.progressPercent.textContent = '100%';
        this.progressText.style.display = 'none';
        this.successMessage.textContent = message;
        this.successMessage.style.display = 'block';
        
        // Auto-close after delay
        setTimeout(() => {
            this.close();
        }, 2000);
    }
    
    /**
     * Show error state
     * @param {string} message - Error message
     */
    showError(message) {
        if (!this.progressIcon) return;
        
        this.progressIcon.classList.add('error');
        this.progressText.style.display = 'none';
        this.errorMessage.textContent = message;
        this.errorMessage.style.display = 'block';
    }
}

// ============================================================================
// EXPORT DROPDOWN COMPONENT
// ============================================================================

class ExportDropdown {
    /**
     * Create an export dropdown
     * @param {HTMLElement} container - Container element
     * @param {Object} options - Configuration options
     */
    constructor(container, options = {}) {
        this.container = container;
        this.options = {
            onPdfExport: null,
            onExcelExport: null,
            buttonText: 'Export',
            ...options,
        };
        
        this.isOpen = false;
        this.render();
    }
    
    render() {
        this.container.innerHTML = `
            <div class="export-dropdown">
                <button class="export-btn export-btn-outline" id="exportDropdownBtn">
                    <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                            d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                    </svg>
                    ${this.options.buttonText}
                    <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                    </svg>
                </button>
                <div class="export-dropdown-menu">
                    <div class="export-dropdown-item pdf" data-format="pdf">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                                d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"/>
                        </svg>
                        <div>
                            <div class="font-medium">Export as PDF</div>
                            <div class="text-xs text-gray-500">Professional formatted report</div>
                        </div>
                    </div>
                    <div class="export-dropdown-divider"></div>
                    <div class="export-dropdown-item excel" data-format="excel">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                                d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                        </svg>
                        <div>
                            <div class="font-medium">Export as Excel</div>
                            <div class="text-xs text-gray-500">Spreadsheet with raw data</div>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        this.attachListeners();
    }
    
    attachListeners() {
        const dropdown = this.container.querySelector('.export-dropdown');
        const button = dropdown.querySelector('#exportDropdownBtn');
        const items = dropdown.querySelectorAll('.export-dropdown-item');
        
        // Toggle dropdown
        button.addEventListener('click', (e) => {
            e.stopPropagation();
            this.toggle();
        });
        
        // Handle item clicks
        items.forEach(item => {
            item.addEventListener('click', () => {
                const format = item.dataset.format;
                this.close();
                
                if (format === 'pdf' && this.options.onPdfExport) {
                    this.options.onPdfExport();
                } else if (format === 'excel' && this.options.onExcelExport) {
                    this.options.onExcelExport();
                }
            });
        });
        
        // Close on outside click
        document.addEventListener('click', () => {
            if (this.isOpen) {
                this.close();
            }
        });
    }
    
    toggle() {
        if (this.isOpen) {
            this.close();
        } else {
            this.open();
        }
    }
    
    open() {
        const dropdown = this.container.querySelector('.export-dropdown');
        dropdown.classList.add('open');
        this.isOpen = true;
    }
    
    close() {
        const dropdown = this.container.querySelector('.export-dropdown');
        dropdown.classList.remove('open');
        this.isOpen = false;
    }
}

// ============================================================================
// EXPORT TOAST NOTIFICATIONS
// ============================================================================

class ExportToast {
    constructor() {
        this.container = null;
        this.init();
    }
    
    init() {
        if (this.container) return;
        
        this.container = document.createElement('div');
        this.container.id = 'exportToastContainer';
        this.container.style.cssText = 'position: fixed; bottom: 2rem; right: 2rem; z-index: 200;';
        document.body.appendChild(this.container);
    }
    
    /**
     * Show a toast notification
     * @param {Object} options - Toast options
     */
    show(options) {
        const {
            type = 'info',
            title = '',
            message = '',
            duration = 5000,
        } = options;
        
        const icons = {
            success: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>',
            error: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>',
            info: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>',
        };
        
        const toast = document.createElement('div');
        toast.className = `export-toast ${type}`;
        toast.innerHTML = `
            <svg class="export-toast-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                ${icons[type]}
            </svg>
            <div class="export-toast-content">
                ${title ? `<div class="export-toast-title">${title}</div>` : ''}
                ${message ? `<div class="export-toast-message">${message}</div>` : ''}
            </div>
            <button class="export-toast-close" aria-label="Close">
                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
            </button>
        `;
        
        this.container.appendChild(toast);
        
        // Trigger animation
        requestAnimationFrame(() => {
            toast.classList.add('show');
        });
        
        // Close button
        toast.querySelector('.export-toast-close').addEventListener('click', () => {
            this.hide(toast);
        });
        
        // Auto-hide
        if (duration > 0) {
            setTimeout(() => {
                this.hide(toast);
            }, duration);
        }
        
        return toast;
    }
    
    hide(toast) {
        toast.classList.remove('show');
        setTimeout(() => {
            toast.remove();
        }, 300);
    }
    
    success(title, message) {
        return this.show({ type: 'success', title, message });
    }
    
    error(title, message) {
        return this.show({ type: 'error', title, message });
    }
    
    info(title, message) {
        return this.show({ type: 'info', title, message });
    }
}

// ============================================================================
// EXPORT HELPER FUNCTIONS
// ============================================================================

/**
 * Execute export with modal progress
 * @param {string} format - 'pdf' or 'excel'
 * @param {string} reportType - 'professor', 'systemWide', or 'department'
 * @param {Object} reportData - Report data from API
 * @param {Object} options - Export options
 */
async function executeExportWithProgress(format, reportType, reportData, options = {}) {
    const modal = window.exportModal;
    const toast = window.exportToast;
    
    const formatTitle = format.toUpperCase();
    modal.open(`Exporting ${formatTitle}`);
    
    try {
        // Set progress callback
        window.reportExportService.setProgressCallback((percent, message) => {
            modal.updateProgress(percent, message);
        });
        
        let filename;
        
        if (format === 'pdf') {
            switch (reportType) {
                case 'professor':
                    filename = await window.reportExportService.exportProfessorSubmissionPDF(reportData, options);
                    break;
                case 'systemWide':
                    filename = await window.reportExportService.exportSystemWidePDF(reportData, options);
                    break;
                case 'department':
                    filename = await window.reportExportService.exportDepartmentPDF(reportData, options);
                    break;
                default:
                    // Generic export
                    filename = await window.reportExportService.exportSystemWidePDF(reportData, options);
            }
        } else if (format === 'excel') {
            filename = await window.reportExportService.exportToExcel(reportData, options);
        }
        
        modal.showSuccess(`${formatTitle} exported successfully!`);
        toast.success('Export Complete', `Your ${formatTitle} has been downloaded.`);
        
        return filename;
        
    } catch (error) {
        console.error('[ExportUI] Export failed:', error);
        modal.showError(error.message || 'Export failed. Please try again.');
        toast.error('Export Failed', error.message || 'Please try again.');
        throw error;
    }
}

/**
 * Create export buttons for a report section
 * @param {HTMLElement} container - Container for buttons
 * @param {Function} getReportData - Function to get current report data
 * @param {string} reportType - Type of report
 * @param {Object} options - Additional options
 */
function createExportButtons(container, getReportData, reportType, options = {}) {
    container.innerHTML = `
        <div class="export-btn-group">
            <button class="export-btn export-btn-pdf" id="exportPdfBtn" title="Export as PDF">
                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"/>
                </svg>
                Export PDF
            </button>
            <button class="export-btn export-btn-excel" id="exportExcelBtn" title="Export as Excel">
                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                </svg>
                Export Excel
            </button>
        </div>
    `;
    
    const pdfBtn = container.querySelector('#exportPdfBtn');
    const excelBtn = container.querySelector('#exportExcelBtn');
    
    pdfBtn.addEventListener('click', async () => {
        const data = getReportData();
        if (!data) {
            window.exportToast?.info('No Data', 'Please generate a report first.');
            return;
        }
        await executeExportWithProgress('pdf', reportType, data, options);
    });
    
    excelBtn.addEventListener('click', async () => {
        const data = getReportData();
        if (!data) {
            window.exportToast?.info('No Data', 'Please generate a report first.');
            return;
        }
        await executeExportWithProgress('excel', reportType, data, options);
    });
}

// ============================================================================
// INITIALIZE GLOBAL INSTANCES
// ============================================================================

// Create singleton instances
window.exportModal = new ExportModal();
window.exportToast = new ExportToast();

// Export for ES modules
export {
    ExportModal,
    ExportDropdown,
    ExportToast,
    executeExportWithProgress,
    createExportButtons,
};

// Make available globally
window.ExportModal = ExportModal;
window.ExportDropdown = ExportDropdown;
window.ExportToast = ExportToast;
window.executeExportWithProgress = executeExportWithProgress;
window.createExportButtons = createExportButtons;

console.log('[ExportUI] Loaded successfully');
