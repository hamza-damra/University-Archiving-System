/**
 * Modern Dropdown Component
 * Transforms native select elements into custom styled dropdowns
 * Inspired by CodePen ig_design
 */

class ModernDropdown {
    constructor(selectElement, options = {}) {
        this.select = selectElement;
        this.options = {
            searchable: options.searchable || false,
            placeholder: options.placeholder || 'Select an option',
            ...options
        };
        
        this.isOpen = false;
        this.selectedIndex = this.select.selectedIndex;
        this.init();
    }

    init() {
        // Hide original select
        this.select.style.display = 'none';
        this.select.setAttribute('aria-hidden', 'true');
        
        // Create custom dropdown
        this.createDropdown();
        
        // Bind events
        this.bindEvents();
        
        // Set initial value
        this.updateDisplay();
    }

    createDropdown() {
        // Main wrapper
        this.wrapper = document.createElement('div');
        this.wrapper.className = 'modern-dropdown-wrapper';
        this.wrapper.setAttribute('role', 'combobox');
        this.wrapper.setAttribute('aria-expanded', 'false');
        this.wrapper.setAttribute('aria-haspopup', 'listbox');
        
        // Toggle button
        this.toggle = document.createElement('button');
        this.toggle.type = 'button';
        this.toggle.className = 'modern-dropdown-toggle';
        this.toggle.setAttribute('aria-label', 'Open dropdown');
        
        // Display text
        this.displayText = document.createElement('span');
        this.displayText.className = 'modern-dropdown-text';
        
        // Arrow icon
        this.arrow = document.createElement('span');
        this.arrow.className = 'modern-dropdown-arrow';
        this.arrow.innerHTML = `
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M6 9l6 6 6-6"/>
            </svg>
        `;
        
        this.toggle.appendChild(this.displayText);
        this.toggle.appendChild(this.arrow);
        
        // Dropdown menu
        this.menu = document.createElement('div');
        this.menu.className = 'modern-dropdown-menu';
        this.menu.setAttribute('role', 'listbox');
        
        // Create options
        this.createOptions();
        
        // Assemble
        this.wrapper.appendChild(this.toggle);
        this.wrapper.appendChild(this.menu);
        
        // Insert after select
        this.select.parentNode.insertBefore(this.wrapper, this.select.nextSibling);
    }

    createOptions() {
        this.menu.innerHTML = '';
        this.optionElements = [];
        
        Array.from(this.select.options).forEach((option, index) => {
            const optionEl = document.createElement('button');
            optionEl.type = 'button';
            optionEl.className = 'modern-dropdown-option';
            optionEl.setAttribute('role', 'option');
            optionEl.setAttribute('data-value', option.value);
            optionEl.setAttribute('data-index', index);
            
            if (option.disabled) {
                optionEl.disabled = true;
                optionEl.classList.add('disabled');
            }
            
            if (option.value === '' || option.disabled) {
                optionEl.classList.add('placeholder');
            }
            
            if (index === this.selectedIndex) {
                optionEl.classList.add('selected');
                optionEl.setAttribute('aria-selected', 'true');
            }
            
            // Option text with optional check icon
            const textSpan = document.createElement('span');
            textSpan.className = 'option-text';
            textSpan.textContent = option.textContent;
            
            const checkIcon = document.createElement('span');
            checkIcon.className = 'option-check';
            checkIcon.innerHTML = `
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20 6L9 17l-5-5"/>
                </svg>
            `;
            
            optionEl.appendChild(textSpan);
            optionEl.appendChild(checkIcon);
            
            this.optionElements.push(optionEl);
            this.menu.appendChild(optionEl);
        });
    }

    bindEvents() {
        // Toggle click
        this.toggle.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            this.toggleDropdown();
        });
        
        // Option click
        this.menu.addEventListener('click', (e) => {
            const option = e.target.closest('.modern-dropdown-option');
            if (option && !option.disabled) {
                const index = parseInt(option.getAttribute('data-index'));
                this.selectOption(index);
            }
        });
        
        // Keyboard navigation
        this.wrapper.addEventListener('keydown', (e) => this.handleKeydown(e));
        
        // Close on outside click
        document.addEventListener('click', (e) => {
            if (!this.wrapper.contains(e.target)) {
                this.closeDropdown();
            }
        });
        
        // Close on escape
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.isOpen) {
                this.closeDropdown();
                this.toggle.focus();
            }
        });
        
        // Sync with original select changes
        this.select.addEventListener('change', () => {
            this.selectedIndex = this.select.selectedIndex;
            this.updateDisplay();
            this.updateSelectedOption();
        });
    }

    handleKeydown(e) {
        if (!this.isOpen && (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown')) {
            e.preventDefault();
            this.openDropdown();
            return;
        }
        
        if (this.isOpen) {
            switch (e.key) {
                case 'ArrowDown':
                    e.preventDefault();
                    this.focusNextOption();
                    break;
                case 'ArrowUp':
                    e.preventDefault();
                    this.focusPreviousOption();
                    break;
                case 'Enter':
                case ' ':
                    e.preventDefault();
                    const focused = this.menu.querySelector('.modern-dropdown-option:focus');
                    if (focused) {
                        const index = parseInt(focused.getAttribute('data-index'));
                        this.selectOption(index);
                    }
                    break;
                case 'Home':
                    e.preventDefault();
                    this.focusFirstOption();
                    break;
                case 'End':
                    e.preventDefault();
                    this.focusLastOption();
                    break;
            }
        }
    }

    focusNextOption() {
        const focused = this.menu.querySelector('.modern-dropdown-option:focus');
        const options = Array.from(this.optionElements).filter(o => !o.disabled);
        if (!focused) {
            options[0]?.focus();
        } else {
            const index = options.indexOf(focused);
            if (index < options.length - 1) {
                options[index + 1].focus();
            }
        }
    }

    focusPreviousOption() {
        const focused = this.menu.querySelector('.modern-dropdown-option:focus');
        const options = Array.from(this.optionElements).filter(o => !o.disabled);
        if (!focused) {
            options[options.length - 1]?.focus();
        } else {
            const index = options.indexOf(focused);
            if (index > 0) {
                options[index - 1].focus();
            }
        }
    }

    focusFirstOption() {
        const options = Array.from(this.optionElements).filter(o => !o.disabled);
        options[0]?.focus();
    }

    focusLastOption() {
        const options = Array.from(this.optionElements).filter(o => !o.disabled);
        options[options.length - 1]?.focus();
    }

    toggleDropdown() {
        if (this.isOpen) {
            this.closeDropdown();
        } else {
            this.openDropdown();
        }
    }

    openDropdown() {
        this.isOpen = true;
        this.wrapper.classList.add('open');
        this.wrapper.setAttribute('aria-expanded', 'true');
        this.toggle.classList.add('active');
        
        // Check if there's enough space below, if not, open above
        // Use requestAnimationFrame to ensure DOM is updated before measuring
        requestAnimationFrame(() => {
            this.positionDropdown();
        });
        
        // Focus selected option
        const selectedOption = this.optionElements[this.selectedIndex];
        if (selectedOption) {
            setTimeout(() => selectedOption.focus(), 50);
        }
    }

    /**
     * Position dropdown above or below based on available viewport space
     */
    positionDropdown() {
        // Remove any existing position classes first
        this.wrapper.classList.remove('dropdown-above', 'dropdown-below');
        this.menu.style.top = '';
        this.menu.style.bottom = '';
        
        // Temporarily make menu visible to get accurate height measurement
        const originalVisibility = this.menu.style.visibility;
        const originalOpacity = this.menu.style.opacity;
        const originalDisplay = this.menu.style.display;
        
        this.menu.style.visibility = 'hidden';
        this.menu.style.opacity = '0';
        this.menu.style.display = 'block';
        
        // Get the menu height
        const menuHeight = Math.min(this.menu.offsetHeight || this.menu.scrollHeight || 280, 280);
        
        // Restore original styles
        this.menu.style.visibility = originalVisibility;
        this.menu.style.opacity = originalOpacity;
        this.menu.style.display = originalDisplay;
        
        // Get viewport and element dimensions
        const toggleRect = this.toggle.getBoundingClientRect();
        const viewportHeight = window.innerHeight;
        
        // Calculate space below and above the toggle button
        const spaceBelow = viewportHeight - toggleRect.bottom;
        const spaceAbove = toggleRect.top;
        
        // Add padding for visual comfort
        const padding = 16;
        
        console.log('[ModernDropdown] Positioning:', {
            menuHeight,
            spaceBelow,
            spaceAbove,
            toggleBottom: toggleRect.bottom,
            viewportHeight,
            needsAbove: spaceBelow < menuHeight + padding
        });
        
        // Check if there's NOT enough space below - if so, try to open above
        if (spaceBelow < menuHeight + padding && spaceAbove > spaceBelow) {
            // Open above - there's more space above than below
            this.wrapper.classList.add('dropdown-above');
            this.menu.style.bottom = 'calc(100% + 8px)';
            this.menu.style.top = 'auto';
            console.log('[ModernDropdown] Opening ABOVE');
        } else {
            // Open below (default)
            this.wrapper.classList.add('dropdown-below');
            this.menu.style.top = 'calc(100% + 8px)';
            this.menu.style.bottom = 'auto';
            console.log('[ModernDropdown] Opening BELOW');
        }
    }

    closeDropdown() {
        this.isOpen = false;
        this.wrapper.classList.remove('open');
        this.wrapper.classList.remove('dropdown-above', 'dropdown-below');
        this.wrapper.setAttribute('aria-expanded', 'false');
        this.toggle.classList.remove('active');
        
        // Reset menu position styles
        this.menu.style.top = '';
        this.menu.style.bottom = '';
    }

    selectOption(index) {
        if (index === this.selectedIndex) {
            this.closeDropdown();
            return;
        }
        
        this.selectedIndex = index;
        this.select.selectedIndex = index;
        
        // Also explicitly set the value property
        const selectedOption = this.select.options[index];
        if (selectedOption) {
            this.select.value = selectedOption.value;
        }
        
        console.log('[ModernDropdown] Option selected:', {
            index,
            value: this.select.value,
            selectedIndex: this.select.selectedIndex,
            selectId: this.select.id
        });
        
        // Trigger both change and input events on original select
        const changeEvent = new Event('change', { bubbles: true });
        this.select.dispatchEvent(changeEvent);
        
        const inputEvent = new Event('input', { bubbles: true });
        this.select.dispatchEvent(inputEvent);
        
        this.updateDisplay();
        this.updateSelectedOption();
        this.closeDropdown();
        this.toggle.focus();
    }

    updateDisplay() {
        const selectedOption = this.select.options[this.selectedIndex];
        if (selectedOption) {
            this.displayText.textContent = selectedOption.textContent;
            
            if (selectedOption.value === '' || selectedOption.disabled) {
                this.displayText.classList.add('placeholder');
            } else {
                this.displayText.classList.remove('placeholder');
            }
        }
    }

    updateSelectedOption() {
        this.optionElements.forEach((el, index) => {
            if (index === this.selectedIndex) {
                el.classList.add('selected');
                el.setAttribute('aria-selected', 'true');
            } else {
                el.classList.remove('selected');
                el.setAttribute('aria-selected', 'false');
            }
        });
    }

    // Public methods
    refresh() {
        this.createOptions();
        this.selectedIndex = this.select.selectedIndex;
        this.updateDisplay();
    }

    setValue(value) {
        const index = Array.from(this.select.options).findIndex(o => o.value === value);
        if (index !== -1) {
            this.selectOption(index);
        }
    }

    getValue() {
        return this.select.value;
    }

    /**
     * Enable or disable the dropdown
     * @param {boolean} disabled - Whether to disable the dropdown
     */
    setDisabled(disabled) {
        this.select.disabled = disabled;
        if (disabled) {
            this.wrapper.classList.add('disabled');
            this.wrapper.style.pointerEvents = 'none';
            this.wrapper.style.opacity = '0.5';
            this.toggle.disabled = true;
            this.toggle.style.cursor = 'not-allowed';
            // Use darker background for dark mode compatibility
            const isDarkMode = document.documentElement.classList.contains('dark');
            this.toggle.style.backgroundColor = isDarkMode ? '#374151' : '#e5e7eb';
            this.closeDropdown();
        } else {
            this.wrapper.classList.remove('disabled');
            this.wrapper.style.pointerEvents = '';
            this.wrapper.style.opacity = '';
            this.toggle.disabled = false;
            this.toggle.style.cursor = '';
            this.toggle.style.backgroundColor = '';
        }
    }

    /**
     * Check if dropdown is disabled
     * @returns {boolean}
     */
    isDisabled() {
        return this.select.disabled;
    }

    destroy() {
        this.wrapper.remove();
        this.select.style.display = '';
        this.select.removeAttribute('aria-hidden');
    }
}

/**
 * Initialize modern dropdowns on elements
 * @param {string} selector - CSS selector for select elements to transform
 * @param {object} options - Configuration options
 */
function initModernDropdowns(selector = 'select.modern-dropdown, #academicYearSelect, #semesterSelect, #professorDepartmentFilter, #courseDepartmentFilter, #assignmentProfessorFilter, #assignmentCourseFilter, #filterCourse, #filterDocType, #filterStatus, #reportFilterCourse, #reportFilterProfessor, #reportFilterDocType, #reportFilterStatus, #reportPageSize', options = {}) {
    // Also initialize any select with class .modern-dropdown
    const allSelectors = selector + ', select.modern-dropdown';
    const selects = document.querySelectorAll(allSelectors);
    const instances = [];
    
    selects.forEach(select => {
        // Skip if already initialized
        if (select.hasAttribute('data-modern-dropdown-initialized')) {
            return;
        }
        
        select.setAttribute('data-modern-dropdown-initialized', 'true');
        const instance = new ModernDropdown(select, options);
        instances.push(instance);
        
        // Store reference on element
        select._modernDropdown = instance;
    });
    
    return instances;
}

/**
 * Refresh a modern dropdown (useful after options change)
 * @param {HTMLSelectElement} select - The original select element
 */
function refreshModernDropdown(select) {
    if (select._modernDropdown) {
        select._modernDropdown.refresh();
    }
}

/**
 * Set the disabled state of a modern dropdown
 * @param {HTMLSelectElement} select - The original select element
 * @param {boolean} disabled - Whether to disable the dropdown
 */
function setModernDropdownDisabled(select, disabled) {
    console.log('[ModernDropdown] setModernDropdownDisabled called', { selectId: select?.id, disabled, hasModernDropdown: !!select?._modernDropdown });
    
    if (select._modernDropdown) {
        select._modernDropdown.setDisabled(disabled);
    } else {
        // Fallback for non-modern dropdowns - try to find the wrapper
        const wrapper = select.nextElementSibling;
        if (wrapper && wrapper.classList.contains('modern-dropdown-wrapper')) {
            console.log('[ModernDropdown] Found wrapper via DOM, applying disabled state directly');
            const toggle = wrapper.querySelector('.modern-dropdown-toggle');
            if (disabled) {
                wrapper.classList.add('disabled');
                wrapper.style.pointerEvents = 'none';
                wrapper.style.opacity = '0.5';
                if (toggle) {
                    toggle.disabled = true;
                    toggle.style.cursor = 'not-allowed';
                    const isDarkMode = document.documentElement.classList.contains('dark');
                    toggle.style.backgroundColor = isDarkMode ? '#374151' : '#e5e7eb';
                }
            } else {
                wrapper.classList.remove('disabled');
                wrapper.style.pointerEvents = '';
                wrapper.style.opacity = '';
                if (toggle) {
                    toggle.disabled = false;
                    toggle.style.cursor = '';
                    toggle.style.backgroundColor = '';
                }
            }
        }
        // Also set the native select
        select.disabled = disabled;
    }
}

// Auto-initialize on DOMContentLoaded if not using modules
if (typeof window !== 'undefined') {
    window.ModernDropdown = ModernDropdown;
    window.initModernDropdowns = initModernDropdowns;
    window.refreshModernDropdown = refreshModernDropdown;
    window.setModernDropdownDisabled = setModernDropdownDisabled;
}

// Export for module usage
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { ModernDropdown, initModernDropdowns, refreshModernDropdown };
}
