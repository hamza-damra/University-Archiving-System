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
        
        // Focus selected option
        const selectedOption = this.optionElements[this.selectedIndex];
        if (selectedOption) {
            setTimeout(() => selectedOption.focus(), 50);
        }
    }

    closeDropdown() {
        this.isOpen = false;
        this.wrapper.classList.remove('open');
        this.wrapper.setAttribute('aria-expanded', 'false');
        this.toggle.classList.remove('active');
    }

    selectOption(index) {
        if (index === this.selectedIndex) {
            this.closeDropdown();
            return;
        }
        
        this.selectedIndex = index;
        this.select.selectedIndex = index;
        
        // Trigger change event on original select
        const event = new Event('change', { bubbles: true });
        this.select.dispatchEvent(event);
        
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

// Auto-initialize on DOMContentLoaded if not using modules
if (typeof window !== 'undefined') {
    window.ModernDropdown = ModernDropdown;
    window.initModernDropdowns = initModernDropdowns;
    window.refreshModernDropdown = refreshModernDropdown;
}

// Export for module usage
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { ModernDropdown, initModernDropdowns, refreshModernDropdown };
}
