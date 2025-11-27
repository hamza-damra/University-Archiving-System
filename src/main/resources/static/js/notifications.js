/**
 * Notification Module for Dean and HOD Dashboards
 * Handles notification icon, dropdown, unread count badge, and mark as read functionality.
 * 
 * Requirements: 9.5, 9.6, 9.7
 */

const NotificationManager = {
    // Configuration
    config: {
        apiBaseUrl: '',  // Will be set based on role (deanship or hod)
        pollInterval: 30000,  // Poll for new notifications every 30 seconds
        maxDisplayed: 10  // Maximum notifications to show in dropdown
    },
    
    // State
    state: {
        notifications: [],
        unreadCount: 0,
        isDropdownOpen: false,
        pollTimer: null
    },
    
    /**
     * Initialize the notification manager
     * @param {string} role - 'deanship' or 'hod'
     */
    init(role) {
        this.config.apiBaseUrl = `/api/${role}`;
        this.createNotificationUI();
        this.bindEvents();
        this.loadNotifications();
        this.startPolling();
    },
    
    /**
     * Create the notification UI elements (icon, badge, dropdown)
     */
    createNotificationUI() {
        // Find the header user section to insert notification icon
        const headerUser = document.querySelector('.header-user') || 
                          document.querySelector('.flex.items-center.space-x-4') ||
                          document.querySelector('header .flex.items-center');
        
        if (!headerUser) {
            console.warn('NotificationManager: Could not find header user section');
            return;
        }
        
        // Create notification container
        const notificationContainer = document.createElement('div');
        notificationContainer.className = 'notification-container';
        notificationContainer.innerHTML = `
            <button class="notification-btn" id="notificationBtn" aria-label="Notifications" title="Notifications">
                <svg class="notification-icon" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" width="24" height="24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>
                <span class="notification-badge" id="notificationBadge" style="display: none;">0</span>
            </button>
            <div class="notification-dropdown" id="notificationDropdown" style="display: none;">
                <div class="notification-header">
                    <h3>Notifications</h3>
                    <button class="mark-all-read-btn" id="markAllReadBtn" title="Mark all as read">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" width="16" height="16">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                        </svg>
                    </button>
                </div>
                <div class="notification-list" id="notificationList">
                    <div class="notification-empty">No notifications</div>
                </div>
            </div>
        `;
        
        // Insert before the theme toggle or first child
        const themeToggle = headerUser.querySelector('.theme-toggle');
        if (themeToggle) {
            headerUser.insertBefore(notificationContainer, themeToggle);
        } else {
            headerUser.insertBefore(notificationContainer, headerUser.firstChild);
        }
        
        // Add styles
        this.addStyles();
    },
    
    /**
     * Add CSS styles for notification UI
     */
    addStyles() {
        if (document.getElementById('notification-styles')) return;
        
        const styles = document.createElement('style');
        styles.id = 'notification-styles';
        styles.textContent = `
            .notification-container {
                position: relative;
                display: inline-flex;
                align-items: center;
                margin-right: 12px;
            }
            
            .notification-btn {
                position: relative;
                background: transparent;
                border: none;
                cursor: pointer;
                padding: 8px;
                border-radius: var(--radius-md, 8px);
                color: var(--color-text-secondary, #6b7280);
                transition: background-color 0.2s, color 0.2s;
                display: flex;
                align-items: center;
                justify-content: center;
            }
            
            .notification-btn:hover {
                background-color: var(--color-bg-secondary, #f3f4f6);
                color: var(--color-text-primary, #111827);
            }
            
            .notification-btn:focus {
                outline: 2px solid var(--color-primary, #3b82f6);
                outline-offset: 2px;
            }
            
            .notification-icon {
                width: 24px;
                height: 24px;
            }
            
            .notification-badge {
                position: absolute;
                top: 2px;
                right: 2px;
                min-width: 18px;
                height: 18px;
                padding: 0 5px;
                font-size: 11px;
                font-weight: 600;
                color: white;
                background-color: #ef4444;
                border-radius: 9px;
                display: flex;
                align-items: center;
                justify-content: center;
                line-height: 1;
            }
            
            .notification-dropdown {
                position: absolute;
                top: calc(100% + 8px);
                right: 0;
                width: 360px;
                max-height: 480px;
                background-color: var(--color-bg-primary, white);
                border: 1px solid var(--color-border, #e5e7eb);
                border-radius: var(--radius-lg, 12px);
                box-shadow: var(--shadow-lg, 0 10px 15px -3px rgba(0, 0, 0, 0.1));
                z-index: 1000;
                overflow: hidden;
            }
            
            .notification-header {
                display: flex;
                align-items: center;
                justify-content: space-between;
                padding: 16px;
                border-bottom: 1px solid var(--color-border, #e5e7eb);
                background-color: var(--color-bg-secondary, #f9fafb);
            }
            
            .notification-header h3 {
                margin: 0;
                font-size: 16px;
                font-weight: 600;
                color: var(--color-text-primary, #111827);
            }
            
            .mark-all-read-btn {
                background: transparent;
                border: none;
                cursor: pointer;
                padding: 6px;
                border-radius: var(--radius-sm, 4px);
                color: var(--color-text-secondary, #6b7280);
                transition: background-color 0.2s, color 0.2s;
            }
            
            .mark-all-read-btn:hover {
                background-color: var(--color-bg-tertiary, #e5e7eb);
                color: var(--color-primary, #3b82f6);
            }
            
            .notification-list {
                max-height: 400px;
                overflow-y: auto;
            }
            
            .notification-item {
                display: flex;
                padding: 12px 16px;
                border-bottom: 1px solid var(--color-border, #e5e7eb);
                cursor: pointer;
                transition: background-color 0.2s;
            }
            
            .notification-item:hover {
                background-color: var(--color-bg-secondary, #f9fafb);
            }
            
            .notification-item:last-child {
                border-bottom: none;
            }
            
            .notification-item.unread {
                background-color: var(--color-primary-light, #eff6ff);
            }
            
            .notification-item.unread:hover {
                background-color: var(--color-primary-lighter, #dbeafe);
            }
            
            .notification-dot {
                width: 8px;
                height: 8px;
                border-radius: 50%;
                background-color: var(--color-primary, #3b82f6);
                margin-right: 12px;
                margin-top: 6px;
                flex-shrink: 0;
            }
            
            .notification-item:not(.unread) .notification-dot {
                background-color: transparent;
            }
            
            .notification-content {
                flex: 1;
                min-width: 0;
            }
            
            .notification-title {
                font-size: 14px;
                font-weight: 500;
                color: var(--color-text-primary, #111827);
                margin-bottom: 4px;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
            
            .notification-message {
                font-size: 13px;
                color: var(--color-text-secondary, #6b7280);
                line-height: 1.4;
                display: -webkit-box;
                -webkit-line-clamp: 2;
                -webkit-box-orient: vertical;
                overflow: hidden;
            }
            
            .notification-time {
                font-size: 12px;
                color: var(--color-text-tertiary, #9ca3af);
                margin-top: 4px;
            }
            
            .notification-empty {
                padding: 32px 16px;
                text-align: center;
                color: var(--color-text-secondary, #6b7280);
                font-size: 14px;
            }
            
            .notification-loading {
                padding: 24px 16px;
                text-align: center;
            }
            
            .notification-loading::after {
                content: '';
                display: inline-block;
                width: 20px;
                height: 20px;
                border: 2px solid var(--color-border, #e5e7eb);
                border-top-color: var(--color-primary, #3b82f6);
                border-radius: 50%;
                animation: notification-spin 0.8s linear infinite;
            }
            
            @keyframes notification-spin {
                to { transform: rotate(360deg); }
            }
            
            /* Dark mode support */
            [data-theme="dark"] .notification-dropdown {
                background-color: var(--color-bg-primary, #1f2937);
                border-color: var(--color-border, #374151);
            }
            
            [data-theme="dark"] .notification-header {
                background-color: var(--color-bg-secondary, #111827);
                border-color: var(--color-border, #374151);
            }
            
            [data-theme="dark"] .notification-item {
                border-color: var(--color-border, #374151);
            }
            
            [data-theme="dark"] .notification-item.unread {
                background-color: rgba(59, 130, 246, 0.1);
            }
            
            [data-theme="dark"] .notification-item.unread:hover {
                background-color: rgba(59, 130, 246, 0.15);
            }
            
            /* Responsive */
            @media (max-width: 480px) {
                .notification-dropdown {
                    width: calc(100vw - 32px);
                    right: -8px;
                }
            }
        `;
        document.head.appendChild(styles);
    },

    
    /**
     * Bind event listeners
     */
    bindEvents() {
        const notificationBtn = document.getElementById('notificationBtn');
        const notificationDropdown = document.getElementById('notificationDropdown');
        const markAllReadBtn = document.getElementById('markAllReadBtn');
        
        if (notificationBtn) {
            notificationBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                this.toggleDropdown();
            });
        }
        
        if (markAllReadBtn) {
            markAllReadBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                this.markAllAsRead();
            });
        }
        
        // Close dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (this.state.isDropdownOpen && 
                !e.target.closest('.notification-container')) {
                this.closeDropdown();
            }
        });
        
        // Close dropdown on escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.state.isDropdownOpen) {
                this.closeDropdown();
            }
        });
    },
    
    /**
     * Toggle dropdown visibility
     */
    toggleDropdown() {
        if (this.state.isDropdownOpen) {
            this.closeDropdown();
        } else {
            this.openDropdown();
        }
    },
    
    /**
     * Open the dropdown
     */
    openDropdown() {
        const dropdown = document.getElementById('notificationDropdown');
        if (dropdown) {
            dropdown.style.display = 'block';
            this.state.isDropdownOpen = true;
            // Refresh notifications when opening
            this.loadNotifications();
        }
    },
    
    /**
     * Close the dropdown
     */
    closeDropdown() {
        const dropdown = document.getElementById('notificationDropdown');
        if (dropdown) {
            dropdown.style.display = 'none';
            this.state.isDropdownOpen = false;
        }
    },
    
    /**
     * Load notifications from API
     */
    async loadNotifications() {
        const token = localStorage.getItem('token');
        if (!token) {
            console.warn('NotificationManager: No auth token found');
            return;
        }
        
        try {
            const response = await fetch(`${this.config.apiBaseUrl}/notifications`, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            const result = await response.json();
            if (result.success && result.data) {
                this.state.notifications = result.data;
                this.renderNotifications();
            }
        } catch (error) {
            console.error('NotificationManager: Error loading notifications', error);
        }
        
        // Also update unread count
        this.loadUnreadCount();
    },
    
    /**
     * Load unread notification count
     */
    async loadUnreadCount() {
        const token = localStorage.getItem('token');
        if (!token) return;
        
        try {
            const response = await fetch(`${this.config.apiBaseUrl}/notifications/unread-count`, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            const result = await response.json();
            if (result.success && typeof result.data === 'number') {
                this.state.unreadCount = result.data;
                this.updateBadge();
            }
        } catch (error) {
            console.error('NotificationManager: Error loading unread count', error);
        }
    },
    
    /**
     * Update the notification badge
     */
    updateBadge() {
        const badge = document.getElementById('notificationBadge');
        if (badge) {
            if (this.state.unreadCount > 0) {
                badge.textContent = this.state.unreadCount > 99 ? '99+' : this.state.unreadCount;
                badge.style.display = 'flex';
            } else {
                badge.style.display = 'none';
            }
        }
    },
    
    /**
     * Render notifications in the dropdown
     */
    renderNotifications() {
        const list = document.getElementById('notificationList');
        if (!list) return;
        
        if (this.state.notifications.length === 0) {
            list.innerHTML = '<div class="notification-empty">No notifications</div>';
            return;
        }
        
        const displayedNotifications = this.state.notifications.slice(0, this.config.maxDisplayed);
        
        list.innerHTML = displayedNotifications.map(notification => `
            <div class="notification-item ${notification.seen ? '' : 'unread'}" 
                 data-id="${notification.id}"
                 role="button"
                 tabindex="0"
                 aria-label="${notification.title}">
                <div class="notification-dot"></div>
                <div class="notification-content">
                    <div class="notification-title">${this.escapeHtml(notification.title)}</div>
                    <div class="notification-message">${this.escapeHtml(notification.message)}</div>
                    <div class="notification-time">${this.formatTime(notification.createdAt)}</div>
                </div>
            </div>
        `).join('');
        
        // Add click handlers to notification items
        list.querySelectorAll('.notification-item').forEach(item => {
            item.addEventListener('click', () => {
                const id = parseInt(item.dataset.id);
                this.markAsRead(id);
            });
            
            // Keyboard support
            item.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    const id = parseInt(item.dataset.id);
                    this.markAsRead(id);
                }
            });
        });
    },
    
    /**
     * Mark a notification as read
     * @param {number} id - Notification ID
     */
    async markAsRead(id) {
        const token = localStorage.getItem('token');
        if (!token) return;
        
        // Find the notification
        const notification = this.state.notifications.find(n => n.id === id);
        if (!notification || notification.seen) return;
        
        try {
            const response = await fetch(`${this.config.apiBaseUrl}/notifications/${id}/read`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            // Update local state
            notification.seen = true;
            this.state.unreadCount = Math.max(0, this.state.unreadCount - 1);
            
            // Update UI
            this.updateBadge();
            this.renderNotifications();
            
        } catch (error) {
            console.error('NotificationManager: Error marking notification as read', error);
        }
    },
    
    /**
     * Mark all notifications as read
     */
    async markAllAsRead() {
        const token = localStorage.getItem('token');
        if (!token) return;
        
        const unreadNotifications = this.state.notifications.filter(n => !n.seen);
        if (unreadNotifications.length === 0) return;
        
        // Mark each notification as read
        const promises = unreadNotifications.map(notification => 
            fetch(`${this.config.apiBaseUrl}/notifications/${notification.id}/read`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            }).catch(err => console.error('Error marking notification as read:', err))
        );
        
        await Promise.all(promises);
        
        // Update local state
        this.state.notifications.forEach(n => n.seen = true);
        this.state.unreadCount = 0;
        
        // Update UI
        this.updateBadge();
        this.renderNotifications();
    },
    
    /**
     * Start polling for new notifications
     */
    startPolling() {
        if (this.state.pollTimer) {
            clearInterval(this.state.pollTimer);
        }
        
        this.state.pollTimer = setInterval(() => {
            this.loadUnreadCount();
        }, this.config.pollInterval);
    },
    
    /**
     * Stop polling
     */
    stopPolling() {
        if (this.state.pollTimer) {
            clearInterval(this.state.pollTimer);
            this.state.pollTimer = null;
        }
    },
    
    /**
     * Format timestamp to relative time
     * @param {string} timestamp - ISO timestamp
     * @returns {string} Formatted time string
     */
    formatTime(timestamp) {
        if (!timestamp) return '';
        
        const date = new Date(timestamp);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);
        
        if (diffMins < 1) return 'Just now';
        if (diffMins < 60) return `${diffMins} min${diffMins > 1 ? 's' : ''} ago`;
        if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
        if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
        
        return date.toLocaleDateString();
    },
    
    /**
     * Escape HTML to prevent XSS
     * @param {string} text - Text to escape
     * @returns {string} Escaped text
     */
    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
};

// Export for module usage
if (typeof module !== 'undefined' && module.exports) {
    module.exports = NotificationManager;
}
