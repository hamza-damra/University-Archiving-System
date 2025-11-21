/**
 * Theme Manager
 * Handles dark/light theme switching and persistence
 */

class ThemeManager {
    constructor() {
        this.STORAGE_KEY = 'app-theme';
        this.THEME_DARK = 'dark';
        this.THEME_LIGHT = 'light';

        // Initialize theme on page load
        this.init();
    }

    /**
     * Initialize theme from localStorage or system preference
     */
    init() {
        const savedTheme = this.getSavedTheme();
        const theme = savedTheme || this.getSystemPreference();
        this.setTheme(theme, false); // false = no transition on initial load
    }

    /**
     * Get saved theme from localStorage
     */
    getSavedTheme() {
        return localStorage.getItem(this.STORAGE_KEY);
    }

    /**
     * Get system color scheme preference
     */
    getSystemPreference() {
        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            return this.THEME_DARK;
        }
        return this.THEME_LIGHT;
    }

    /**
     * Set theme
     * @param {string} theme - 'dark' or 'light'
     * @param {boolean} withTransition - Whether to apply transition animation
     */
    setTheme(theme, withTransition = true) {
        const html = document.documentElement;

        // Add transition class if requested
        if (withTransition) {
            html.classList.add('theme-transitioning');
        }

        // Apply theme
        if (theme === this.THEME_DARK) {
            html.classList.add('dark');
        } else {
            html.classList.remove('dark');
        }

        // Save to localStorage
        localStorage.setItem(this.STORAGE_KEY, theme);

        // Remove transition class after animation completes
        if (withTransition) {
            setTimeout(() => {
                html.classList.remove('theme-transitioning');
            }, 300);
        }

        // Dispatch custom event for other components to react
        window.dispatchEvent(new CustomEvent('themeChanged', { detail: { theme } }));
    }

    /**
     * Toggle between dark and light theme
     */
    toggle() {
        const currentTheme = this.getCurrentTheme();
        const newTheme = currentTheme === this.THEME_DARK ? this.THEME_LIGHT : this.THEME_DARK;
        this.setTheme(newTheme, true);
    }

    /**
     * Get current theme
     */
    getCurrentTheme() {
        return document.documentElement.classList.contains('dark') ? this.THEME_DARK : this.THEME_LIGHT;
    }

    /**
     * Check if dark mode is active
     */
    isDark() {
        return this.getCurrentTheme() === this.THEME_DARK;
    }
}

// Create global instance
const themeManager = new ThemeManager();

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = themeManager;
}
