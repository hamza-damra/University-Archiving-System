/**
 * Navigation Components Module
 * Handles breadcrumbs, sidebar collapse, and navigation state
 */

import { dashboardState } from './deanship-state.js';

/**
 * CollapsibleSidebar class for sidebar collapse/expand functionality
 */
export class CollapsibleSidebar {
    /**
     * @param {HTMLElement} sidebarElement - The sidebar element to make collapsible
     */
    constructor(sidebarElement) {
        this.sidebar = sidebarElement;
        this.isCollapsed = false;
        this.storageKey = 'deanship_sidebar_collapsed';
        this.collapseButton = null;
        
        this.initialize();
    }
    
    /**
     * Initialize the collapsible sidebar
     */
    initialize() {
        // Load persisted state
        this.loadState();
        
        // Create and add collapse button
        this.createCollapseButton();
        
        // Apply initial state
        if (this.isCollapsed) {
            this.applyCollapsedState();
        }
        
        // Add tooltips to nav items when collapsed
        this.updateTooltips();
    }
    
    /**
     * Create the collapse button and add it to sidebar footer
     */
    createCollapseButton() {
        const footer = this.sidebar.querySelector('.p-4.border-t');
        if (!footer) {
            console.error('Sidebar footer not found');
            return;
        }
        
        // Create collapse button
        this.collapseButton = document.createElement('button');
        this.collapseButton.id = 'sidebarCollapseBtn';
        this.collapseButton.className = 'flex items-center w-full px-4 py-2 text-sm font-medium text-gray-600 dark:text-gray-400 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors mb-2';
        this.collapseButton.setAttribute('aria-label', 'Toggle sidebar');
        this.collapseButton.title = 'Collapse sidebar';
        
        this.collapseButton.innerHTML = `
            <svg class="w-5 h-5 mr-3 transition-transform duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
            </svg>
            <span class="sidebar-label">Collapse</span>
        `;
        
        // Insert before logout button
        const logoutBtn = footer.querySelector('#logoutBtn');
        if (logoutBtn) {
            footer.insertBefore(this.collapseButton, logoutBtn);
        } else {
            footer.appendChild(this.collapseButton);
        }
        
        // Add click event
        this.collapseButton.addEventListener('click', () => this.toggle());
    }
    
    /**
     * Toggle sidebar collapsed state
     */
    toggle() {
        if (this.isCollapsed) {
            this.expand();
        } else {
            this.collapse();
        }
    }
    
    /**
     * Collapse the sidebar
     */
    collapse() {
        this.isCollapsed = true;
        this.applyCollapsedState();
        this.persistState();
        this.updateTooltips();
    }
    
    /**
     * Expand the sidebar
     */
    expand() {
        this.isCollapsed = false;
        this.applyExpandedState();
        this.persistState();
        this.updateTooltips();
    }
    
    /**
     * Apply collapsed state styles
     */
    applyCollapsedState() {
        this.sidebar.classList.add('sidebar-collapsed');
        this.sidebar.style.width = '64px';
        
        // Hide all labels
        const labels = this.sidebar.querySelectorAll('.sidebar-label, .nav-tab span:not(.sr-only)');
        labels.forEach(label => {
            if (!label.closest('svg')) {
                label.style.display = 'none';
            }
        });
        
        // Hide text in header using ID
        const headerText = document.getElementById('sidebarHeaderText');
        if (headerText) {
            headerText.style.display = 'none';
        }
        
        // Center the logo and adjust padding
        const headerWrapper = document.getElementById('sidebarHeader');
        const headerContent = document.getElementById('sidebarHeaderContent');
        
        if (headerWrapper) {
            headerWrapper.classList.remove('p-6');
            headerWrapper.classList.add('p-4');
        }
        if (headerContent) {
            headerContent.classList.add('justify-center');
        }
        
        // Adjust footer padding to allow buttons to fit
        const footer = document.getElementById('sidebarFooter');
        if (footer) {
            footer.classList.remove('p-4');
            footer.classList.add('p-2');
        }

        // Update collapse button icon
        if (this.collapseButton) {
            const svg = this.collapseButton.querySelector('svg');
            if (svg) {
                svg.style.transform = 'rotate(180deg)';
                svg.style.marginRight = '0';
            }
            this.collapseButton.title = 'Expand sidebar';
            this.collapseButton.classList.add('justify-center');
            this.collapseButton.classList.remove('px-4'); // Remove horizontal padding
            this.collapseButton.classList.add('px-0');
        }

        // Update logout button icon
        const logoutBtn = this.sidebar.querySelector('#logoutBtn');
        if (logoutBtn) {
            const svg = logoutBtn.querySelector('svg');
            if (svg) {
                svg.style.marginRight = '0';
            }
            logoutBtn.title = 'Logout';
            logoutBtn.classList.add('justify-center');
            logoutBtn.classList.remove('px-4'); // Remove horizontal padding
            logoutBtn.classList.add('px-0');
        }
    }
    
    /**
     * Apply expanded state styles
     */
    applyExpandedState() {
        this.sidebar.classList.remove('sidebar-collapsed');
        this.sidebar.style.width = '260px';
        
        // Show all labels
        const labels = this.sidebar.querySelectorAll('.sidebar-label, .nav-tab span:not(.sr-only)');
        labels.forEach(label => {
            label.style.display = '';
        });
        
        // Show text in header using ID
        const headerText = document.getElementById('sidebarHeaderText');
        if (headerText) {
            headerText.style.display = '';
        }
        
        // Reset header padding
        const headerWrapper = document.getElementById('sidebarHeader');
        const headerContent = document.getElementById('sidebarHeaderContent');
        
        if (headerWrapper) {
            headerWrapper.classList.remove('p-4');
            headerWrapper.classList.add('p-6');
        }
        if (headerContent) {
            headerContent.classList.remove('justify-center');
        }
        
        // Reset footer padding
        const footer = document.getElementById('sidebarFooter');
        if (footer) {
            footer.classList.remove('p-2');
            footer.classList.add('p-4');
        }
        
        // Update collapse button icon
        if (this.collapseButton) {
            const svg = this.collapseButton.querySelector('svg');
            if (svg) {
                svg.style.transform = 'rotate(0deg)';
                svg.style.marginRight = '';
            }
            this.collapseButton.title = 'Collapse sidebar';
            this.collapseButton.classList.remove('justify-center');
            this.collapseButton.classList.remove('px-0');
            this.collapseButton.classList.add('px-4');
        }

        // Update logout button icon
        const logoutBtn = this.sidebar.querySelector('#logoutBtn');
        if (logoutBtn) {
            const svg = logoutBtn.querySelector('svg');
            if (svg) {
                svg.style.marginRight = '';
            }
            logoutBtn.removeAttribute('title');
            logoutBtn.classList.remove('justify-center');
            logoutBtn.classList.remove('px-0');
            logoutBtn.classList.add('px-4');
        }
    }
    
    /**
     * Update tooltips for nav items
     */
    updateTooltips() {
        const navTabs = this.sidebar.querySelectorAll('.nav-tab');
        
        navTabs.forEach(tab => {
            if (this.isCollapsed) {
                // Get the text content for tooltip
                const textContent = tab.textContent.trim();
                tab.title = textContent;
            } else {
                // Remove tooltip when expanded
                tab.removeAttribute('title');
            }
        });
        
        // Update logout button tooltip
        const logoutBtn = this.sidebar.querySelector('#logoutBtn');
        if (logoutBtn) {
            if (this.isCollapsed) {
                logoutBtn.title = 'Logout';
            } else {
                logoutBtn.removeAttribute('title');
            }
        }
    }
    
    /**
     * Get current collapsed state
     * @returns {boolean} True if sidebar is collapsed
     */
    getState() {
        return this.isCollapsed;
    }
    
    /**
     * Persist collapsed state to localStorage
     */
    persistState() {
        try {
            localStorage.setItem(this.storageKey, JSON.stringify(this.isCollapsed));
        } catch (error) {
            console.error('Failed to persist sidebar state:', error);
        }
    }
    
    /**
     * Load persisted state from localStorage
     */
    loadState() {
        try {
            const savedState = localStorage.getItem(this.storageKey);
            if (savedState !== null) {
                this.isCollapsed = JSON.parse(savedState);
            }
        } catch (error) {
            console.error('Failed to load sidebar state:', error);
            this.isCollapsed = false;
        }
    }
}

/**
 * BreadcrumbNavigation component for hierarchical navigation
 */
export class BreadcrumbNavigation {
    /**
     * @param {HTMLElement} containerElement - The container element for breadcrumbs
     */
    constructor(containerElement) {
        this.container = containerElement;
        this.breadcrumbs = [];
        this.onNavigateCallback = null;
    }
    
    /**
     * Set breadcrumb items
     * @param {Array<{label: string, path?: string, icon?: string}>} items - Breadcrumb items
     */
    setBreadcrumbs(items) {
        this.breadcrumbs = items;
        this.render();
    }
    
    /**
     * Add a breadcrumb item
     * @param {Object} item - Breadcrumb item {label, path, icon}
     */
    addBreadcrumb(item) {
        this.breadcrumbs.push(item);
        this.render();
    }
    
    /**
     * Clear all breadcrumbs
     */
    clear() {
        this.breadcrumbs = [];
        this.render();
    }
    
    /**
     * Set navigation callback
     * @param {Function} callback - Callback function when breadcrumb is clicked
     */
    onNavigate(callback) {
        this.onNavigateCallback = callback;
    }
    
    /**
     * Render breadcrumbs
     */
    render() {
        if (!this.container) {
            console.error('Breadcrumb container not found');
            return;
        }
        
        // Clear container
        this.container.innerHTML = '';
        
        // If no breadcrumbs, hide container
        if (this.breadcrumbs.length === 0) {
            this.container.style.display = 'none';
            return;
        }
        
        this.container.style.display = 'flex';
        
        // Create breadcrumb nav element
        const nav = document.createElement('nav');
        nav.className = 'flex items-center space-x-2 text-sm';
        nav.setAttribute('aria-label', 'Breadcrumb');
        
        const ol = document.createElement('ol');
        ol.className = 'flex items-center space-x-2';
        
        this.breadcrumbs.forEach((item, index) => {
            const isLast = index === this.breadcrumbs.length - 1;
            
            // Create list item
            const li = document.createElement('li');
            li.className = 'flex items-center';
            
            // Add separator (chevron) if not first item
            if (index > 0) {
                const separator = document.createElement('svg');
                separator.className = 'w-4 h-4 text-gray-400 mx-2';
                separator.setAttribute('fill', 'none');
                separator.setAttribute('stroke', 'currentColor');
                separator.setAttribute('viewBox', '0 0 24 24');
                separator.innerHTML = '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />';
                li.appendChild(separator);
            }
            
            // Create breadcrumb item
            if (isLast) {
                // Last item - non-clickable
                const span = document.createElement('span');
                span.className = 'text-gray-900 font-medium';
                span.setAttribute('aria-current', 'page');
                
                if (item.icon) {
                    span.innerHTML = `${item.icon} ${item.label}`;
                } else {
                    span.textContent = item.label;
                }
                
                li.appendChild(span);
            } else {
                // Clickable item
                const link = document.createElement('a');
                link.href = item.path || '#';
                link.className = 'text-gray-600 hover:text-blue-600 transition-colors';
                
                if (item.icon) {
                    link.innerHTML = `${item.icon} ${item.label}`;
                } else {
                    link.textContent = item.label;
                }
                
                // Add click handler
                link.addEventListener('click', (e) => {
                    e.preventDefault();
                    if (this.onNavigateCallback) {
                        this.onNavigateCallback(item, index);
                    }
                });
                
                li.appendChild(link);
            }
            
            ol.appendChild(li);
        });
        
        nav.appendChild(ol);
        this.container.appendChild(nav);
    }
    
    /**
     * Get current breadcrumbs
     * @returns {Array} Current breadcrumb items
     */
    getBreadcrumbs() {
        return this.breadcrumbs;
    }
}

/**
 * Navigation module for dashboard
 */
export class DashboardNavigation {
    constructor() {
        this.breadcrumbs = [];
        this.collapsibleSidebar = null;
        this.breadcrumbNavigation = null;
    }
    
    /**
     * Initialize navigation components
     */
    initialize() {
        // Initialize collapsible sidebar
        const sidebarElement = document.querySelector('aside');
        if (sidebarElement) {
            this.collapsibleSidebar = new CollapsibleSidebar(sidebarElement);
        }
        
        // Initialize breadcrumb navigation
        const breadcrumbContainer = document.getElementById('breadcrumbContainer');
        if (breadcrumbContainer) {
            this.breadcrumbNavigation = new BreadcrumbNavigation(breadcrumbContainer);
        }
    }
    
    /**
     * Update breadcrumbs
     * @param {Array<{label: string, path?: string, icon?: string}>} items - Breadcrumb items
     */
    updateBreadcrumbs(items) {
        this.breadcrumbs = items;
        if (this.breadcrumbNavigation) {
            this.breadcrumbNavigation.setBreadcrumbs(items);
        }
    }
    
    /**
     * Get current breadcrumbs
     */
    getBreadcrumbs() {
        return this.breadcrumbs;
    }
    
    /**
     * Get collapsible sidebar instance
     */
    getCollapsibleSidebar() {
        return this.collapsibleSidebar;
    }
    
    /**
     * Get breadcrumb navigation instance
     */
    getBreadcrumbNavigation() {
        return this.breadcrumbNavigation;
    }
}

// Create singleton instance
const dashboardNavigation = new DashboardNavigation();

export { dashboardNavigation };
