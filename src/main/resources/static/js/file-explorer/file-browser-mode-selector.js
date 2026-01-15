/**
 * File Browser Mode Selector
 * 
 * A unified component for switching between Classic and Modern file browser modes.
 * Works identically across all roles (Professor, Dean, HOD, Admin).
 * 
 * Features:
 * - Persists user preference in localStorage
 * - Provides fallback to Classic mode if Modern fails
 * - Feature-flag friendly for safe deployment
 * - Independent preference per user
 */

const FILE_BROWSER_MODE_KEY = 'fileBrowserMode';
const VALID_MODES = ['classic', 'modern'];
const DEFAULT_MODE = 'classic';

/**
 * FileBrowserModeSelector class
 * Manages file browser mode switching and persistence
 */
export class FileBrowserModeSelector {
    /**
     * Create a new mode selector
     * @param {Object} options - Configuration options
     * @param {Function} options.onModeChange - Callback when mode changes
     * @param {string} options.containerId - ID of container to render selector into
     * @param {string} options.role - User role for logging purposes
     */
    constructor(options = {}) {
        this.onModeChange = options.onModeChange || null;
        this.containerId = options.containerId || null;
        this.role = options.role || 'unknown';
        this.currentMode = this.loadMode();
        
        // Bind methods
        this.handleModeChange = this.handleModeChange.bind(this);
    }
    
    /**
     * Load saved mode from localStorage
     * @returns {string} The saved mode or default
     */
    loadMode() {
        try {
            const savedMode = localStorage.getItem(FILE_BROWSER_MODE_KEY);
            if (savedMode && VALID_MODES.includes(savedMode)) {
                return savedMode;
            }
        } catch (e) {
            console.warn('[FileBrowserModeSelector] Could not access localStorage:', e);
        }
        return DEFAULT_MODE;
    }
    
    /**
     * Save mode to localStorage
     * @param {string} mode - Mode to save
     */
    saveMode(mode) {
        try {
            if (VALID_MODES.includes(mode)) {
                localStorage.setItem(FILE_BROWSER_MODE_KEY, mode);
            }
        } catch (e) {
            console.warn('[FileBrowserModeSelector] Could not save to localStorage:', e);
        }
    }
    
    /**
     * Get current mode
     * @returns {string} Current mode ('classic' or 'modern')
     */
    getMode() {
        return this.currentMode;
    }
    
    /**
     * Check if modern mode is active
     * @returns {boolean} True if modern mode is active
     */
    isModernMode() {
        return this.currentMode === 'modern';
    }
    
    /**
     * Check if classic mode is active
     * @returns {boolean} True if classic mode is active
     */
    isClassicMode() {
        return this.currentMode === 'classic';
    }
    
    /**
     * Set mode programmatically
     * @param {string} mode - Mode to set
     * @param {boolean} notify - Whether to trigger callback
     */
    setMode(mode, notify = true) {
        if (!VALID_MODES.includes(mode)) {
            console.warn('[FileBrowserModeSelector] Invalid mode:', mode);
            return;
        }
        
        const previousMode = this.currentMode;
        this.currentMode = mode;
        this.saveMode(mode);
        
        // Update UI if rendered
        this.updateSelectorUI();
        
        // Notify listeners
        if (notify && this.onModeChange && previousMode !== mode) {
            this.onModeChange(mode, previousMode);
        }
        
        console.log(`[FileBrowserModeSelector] Mode changed: ${previousMode} → ${mode} (Role: ${this.role})`);
    }
    
    /**
     * Handle mode change from UI
     * @param {Event} event - Change event
     */
    handleModeChange(event) {
        const newMode = event.target.value || event.target.dataset.mode;
        if (newMode) {
            this.setMode(newMode);
        }
    }
    
    /**
     * Fallback to classic mode (called when modern mode fails)
     * @param {string} reason - Reason for fallback
     */
    fallbackToClassic(reason = 'Unknown error') {
        console.warn(`[FileBrowserModeSelector] Falling back to Classic mode. Reason: ${reason}`);
        this.setMode('classic', true);
        
        // Show user notification
        if (typeof showToast === 'function') {
            showToast('Switched to Classic file browser due to an error', 'warning');
        }
    }
    
    /**
     * Render the mode selector UI
     * @param {string} containerId - Optional container ID override
     * @returns {HTMLElement} The rendered selector element
     */
    render(containerId = null) {
        const targetId = containerId || this.containerId;
        const container = targetId ? document.getElementById(targetId) : null;
        
        // Create selector element
        const selectorHtml = `
            <div class="file-browser-mode-selector" data-component="file-browser-mode-selector">
                <div class="mode-selector-wrapper">
                    <label class="mode-selector-label">
                        <svg class="mode-selector-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/>
                        </svg>
                        <span>File Browser Mode</span>
                    </label>
                    <div class="mode-selector-controls">
                        <button type="button" 
                                class="mode-btn ${this.currentMode === 'classic' ? 'active' : ''}" 
                                data-mode="classic"
                                title="Classic File Browser - Stable, reliable, familiar interface">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <rect x="3" y="3" width="18" height="18" rx="2"/>
                                <path d="M9 3v18M3 9h6"/>
                            </svg>
                            <span>Classic</span>
                        </button>
                        <button type="button" 
                                class="mode-btn ${this.currentMode === 'modern' ? 'active' : ''}" 
                                data-mode="modern"
                                title="Modern File Browser - Desktop-style, interactive, professional">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <rect x="2" y="3" width="20" height="14" rx="2"/>
                                <path d="M8 21h8M12 17v4"/>
                                <path d="M6 8h4M6 11h8"/>
                            </svg>
                            <span>Modern</span>
                        </button>
                    </div>
                </div>
            </div>
        `;
        
        // Create DOM element
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = selectorHtml.trim();
        const selectorElement = tempDiv.firstChild;
        
        // Attach event listeners
        const buttons = selectorElement.querySelectorAll('.mode-btn');
        buttons.forEach(btn => {
            btn.addEventListener('click', (e) => {
                const mode = e.currentTarget.dataset.mode;
                this.setMode(mode);
            });
        });
        
        // Insert into container if provided
        if (container) {
            container.innerHTML = '';
            container.appendChild(selectorElement);
        }
        
        return selectorElement;
    }
    
    /**
     * Update the selector UI to reflect current mode
     */
    updateSelectorUI() {
        const selector = document.querySelector('[data-component="file-browser-mode-selector"]');
        if (!selector) return;
        
        const buttons = selector.querySelectorAll('.mode-btn');
        buttons.forEach(btn => {
            const isActive = btn.dataset.mode === this.currentMode;
            btn.classList.toggle('active', isActive);
        });
    }
    
    /**
     * Destroy the selector and cleanup
     */
    destroy() {
        const selector = document.querySelector('[data-component="file-browser-mode-selector"]');
        if (selector) {
            selector.remove();
        }
        this.onModeChange = null;
    }
}

// Export singleton factory
let selectorInstance = null;

/**
 * Get or create the mode selector singleton
 * @param {Object} options - Options passed to constructor
 * @returns {FileBrowserModeSelector} The selector instance
 */
export function getFileBrowserModeSelector(options = {}) {
    if (!selectorInstance) {
        selectorInstance = new FileBrowserModeSelector(options);
    } else if (options.onModeChange) {
        selectorInstance.onModeChange = options.onModeChange;
    }
    return selectorInstance;
}

/**
 * Reset the singleton (for testing or when user logs out)
 */
export function resetFileBrowserModeSelector() {
    if (selectorInstance) {
        selectorInstance.destroy();
        selectorInstance = null;
    }
}

export default FileBrowserModeSelector;
