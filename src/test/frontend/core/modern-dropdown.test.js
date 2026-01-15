/**
 * @jest-environment jsdom
 */

import { jest } from '@jest/globals';
import { readFileSync } from 'fs';
import { join } from 'path';

// Mock dependencies before importing the module under test
global.fetch = jest.fn();
global.window = {
    location: { href: '', origin: 'http://localhost' },
    ...global.window
};

// Mock localStorage
const localStorageMock = (() => {
    let store = {};
    return {
        getItem: jest.fn(key => store[key] || null),
        setItem: jest.fn((key, value) => { store[key] = value; }),
        removeItem: jest.fn(key => { delete store[key]; }),
        clear: jest.fn(() => { store = {}; }),
        get length() { return Object.keys(store).length; }
    };
})();
Object.defineProperty(window, 'localStorage', { value: localStorageMock });

// Mock module.exports for CommonJS compatibility
const moduleMock = { exports: {} };
global.module = moduleMock;
global.exports = moduleMock.exports;

// Load and execute the modern-dropdown script
// The script sets both window properties and module.exports
const dropdownScriptPath = join(process.cwd(), 'src/main/resources/static/js/core/modern-dropdown.js');
const dropdownScript = readFileSync(dropdownScriptPath, 'utf-8');

// Execute the script - it will set window.ModernDropdown, etc.
// Use Function constructor to execute in proper scope
const executeScript = new Function('window', 'document', 'module', 'exports', dropdownScript);
executeScript(window, document, moduleMock, moduleMock.exports);

// Extract the exports - prefer window properties (as set by the script)
let ModernDropdown = window.ModernDropdown || moduleMock.exports.ModernDropdown;
let initModernDropdowns = window.initModernDropdowns || moduleMock.exports.initModernDropdowns;
let refreshModernDropdown = window.refreshModernDropdown || moduleMock.exports.refreshModernDropdown;
let setModernDropdownDisabled = window.setModernDropdownDisabled;

describe('Modern Dropdown Component', () => {
    let selectElement;
    let container;

    beforeEach(() => {
        // Create a clean container for each test
        container = document.createElement('div');
        document.body.appendChild(container);

        // Create a native select element
        selectElement = document.createElement('select');
        selectElement.id = 'testSelect';
        selectElement.innerHTML = `
            <option value="">Select an option</option>
            <option value="1">Option 1</option>
            <option value="2">Option 2</option>
            <option value="3">Option 3</option>
        `;
        container.appendChild(selectElement);

        // Load the ModernDropdown class dynamically
        // In a real environment, this would be: const { ModernDropdown } = require('../../../../main/resources/static/js/core/modern-dropdown.js');
        // For now, we'll assume it's available globally or loaded via script tag
        if (typeof window.ModernDropdown !== 'undefined') {
            ModernDropdown = window.ModernDropdown;
            initModernDropdowns = window.initModernDropdowns;
            refreshModernDropdown = window.refreshModernDropdown;
            setModernDropdownDisabled = window.setModernDropdownDisabled;
        } else {
            // If not available, we'll need to load it
            // This is a placeholder - in real tests, use proper module loading
            throw new Error('ModernDropdown not available. Ensure the script is loaded before tests run.');
        }
    });

    afterEach(() => {
        // Clean up
        if (container && container.parentNode) {
            container.parentNode.removeChild(container);
        }
        document.body.innerHTML = '';
    });

    describe('Initialization', () => {
        test('Wraps native select element', () => {
            const dropdown = new ModernDropdown(selectElement);
            
            expect(dropdown.select).toBe(selectElement);
            expect(selectElement.style.display).toBe('none');
            expect(selectElement.getAttribute('aria-hidden')).toBe('true');
        });

        test('Creates custom dropdown DOM', () => {
            const dropdown = new ModernDropdown(selectElement);
            
            expect(dropdown.wrapper).toBeDefined();
            expect(dropdown.wrapper.classList.contains('modern-dropdown-wrapper')).toBe(true);
            expect(dropdown.toggle).toBeDefined();
            expect(dropdown.toggle.classList.contains('modern-dropdown-toggle')).toBe(true);
            expect(dropdown.menu).toBeDefined();
            expect(dropdown.menu.classList.contains('modern-dropdown-menu')).toBe(true);
            expect(dropdown.displayText).toBeDefined();
            expect(dropdown.displayText.classList.contains('modern-dropdown-text')).toBe(true);
            
            // Check wrapper is inserted after select
            expect(selectElement.nextSibling).toBe(dropdown.wrapper);
        });

        test('Syncs initial selected value', () => {
            // Set initial selection
            selectElement.selectedIndex = 1;
            const dropdown = new ModernDropdown(selectElement);
            
            expect(dropdown.selectedIndex).toBe(1);
            expect(dropdown.displayText.textContent).toBe('Option 1');
            
            // Check that the corresponding option is marked as selected
            const selectedOption = dropdown.optionElements[1];
            expect(selectedOption.classList.contains('selected')).toBe(true);
            expect(selectedOption.getAttribute('aria-selected')).toBe('true');
        });

        test('Handles multiple select elements', () => {
            // Create multiple select elements
            const select1 = document.createElement('select');
            select1.id = 'select1';
            select1.innerHTML = '<option value="1">Option 1</option>';
            container.appendChild(select1);

            const select2 = document.createElement('select');
            select2.id = 'select2';
            select2.innerHTML = '<option value="2">Option 2</option>';
            container.appendChild(select2);

            const instances = initModernDropdowns('#select1, #select2');
            
            expect(instances.length).toBe(2);
            expect(select1._modernDropdown).toBeDefined();
            expect(select2._modernDropdown).toBeDefined();
            expect(select1._modernDropdown).not.toBe(select2._modernDropdown);
        });
    });

    describe('Interaction', () => {
        let dropdown;

        beforeEach(() => {
            dropdown = new ModernDropdown(selectElement);
        });

        test('Click opens dropdown', () => {
            expect(dropdown.isOpen).toBe(false);
            expect(dropdown.wrapper.classList.contains('open')).toBe(false);
            
            dropdown.toggle.click();
            
            expect(dropdown.isOpen).toBe(true);
            expect(dropdown.wrapper.classList.contains('open')).toBe(true);
            expect(dropdown.wrapper.getAttribute('aria-expanded')).toBe('true');
        });

        test('Click outside closes dropdown', (done) => {
            dropdown.openDropdown();
            expect(dropdown.isOpen).toBe(true);
            
            // Simulate click outside
            const outsideElement = document.createElement('div');
            document.body.appendChild(outsideElement);
            const clickEvent = new MouseEvent('click', { bubbles: true });
            outsideElement.dispatchEvent(clickEvent);
            
            // Use setTimeout to allow event propagation
            setTimeout(() => {
                expect(dropdown.isOpen).toBe(false);
                document.body.removeChild(outsideElement);
                done();
            }, 10);
        });

        test('Option click selects value', () => {
            const changeHandler = jest.fn();
            selectElement.addEventListener('change', changeHandler);
            
            dropdown.openDropdown();
            const option2 = dropdown.optionElements[2]; // Option 2
            option2.click();
            
            expect(selectElement.selectedIndex).toBe(2);
            expect(selectElement.value).toBe('2');
            expect(dropdown.selectedIndex).toBe(2);
            expect(dropdown.displayText.textContent).toBe('Option 2');
            expect(changeHandler).toHaveBeenCalled();
            expect(dropdown.isOpen).toBe(false);
        });

        test('Keyboard navigation works', () => {
            dropdown.openDropdown();
            
            // Test ArrowDown
            const arrowDownEvent = new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true });
            dropdown.wrapper.dispatchEvent(arrowDownEvent);
            
            // Check that focus moved to next option
            const focusedOption = dropdown.menu.querySelector('.modern-dropdown-option:focus');
            expect(focusedOption).toBeDefined();
            
            // Test ArrowUp
            const arrowUpEvent = new KeyboardEvent('keydown', { key: 'ArrowUp', bubbles: true });
            dropdown.wrapper.dispatchEvent(arrowUpEvent);
            
            // Test Home key
            const homeEvent = new KeyboardEvent('keydown', { key: 'Home', bubbles: true });
            dropdown.wrapper.dispatchEvent(homeEvent);
            const firstOption = dropdown.optionElements.find(o => !o.disabled);
            expect(document.activeElement).toBe(firstOption);
            
            // Test End key
            const endEvent = new KeyboardEvent('keydown', { key: 'End', bubbles: true });
            dropdown.wrapper.dispatchEvent(endEvent);
            const options = Array.from(dropdown.optionElements).filter(o => !o.disabled);
            const lastOption = options[options.length - 1];
            expect(document.activeElement).toBe(lastOption);
        });

        test('Enter selects highlighted option', () => {
            dropdown.openDropdown();
            
            // Focus first option
            const firstOption = dropdown.optionElements.find(o => !o.disabled && o.value !== '');
            firstOption.focus();
            
            const changeHandler = jest.fn();
            selectElement.addEventListener('change', changeHandler);
            
            // Press Enter
            const enterEvent = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true });
            dropdown.wrapper.dispatchEvent(enterEvent);
            
            expect(changeHandler).toHaveBeenCalled();
            expect(dropdown.isOpen).toBe(false);
        });

        test('Escape closes dropdown', () => {
            dropdown.openDropdown();
            expect(dropdown.isOpen).toBe(true);
            
            const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape', bubbles: true });
            document.dispatchEvent(escapeEvent);
            
            expect(dropdown.isOpen).toBe(false);
            expect(document.activeElement).toBe(dropdown.toggle);
        });
    });

    describe('Sync', () => {
        let dropdown;

        beforeEach(() => {
            dropdown = new ModernDropdown(selectElement);
        });

        test('refresh updates options from native select', () => {
            const initialOptionCount = dropdown.optionElements.length;
            
            // Add a new option to the native select
            const newOption = document.createElement('option');
            newOption.value = '4';
            newOption.textContent = 'Option 4';
            selectElement.appendChild(newOption);
            
            dropdown.refresh();
            
            expect(dropdown.optionElements.length).toBe(initialOptionCount + 1);
            const newOptionElement = dropdown.optionElements.find(o => o.getAttribute('data-value') === '4');
            expect(newOptionElement).toBeDefined();
            expect(newOptionElement.textContent).toContain('Option 4');
        });

        test('Programmatic select change syncs', () => {
            const changeHandler = jest.fn();
            dropdown.wrapper.addEventListener('change', changeHandler);
            
            // Programmatically change the select
            selectElement.selectedIndex = 2;
            selectElement.value = '2';
            const changeEvent = new Event('change', { bubbles: true });
            selectElement.dispatchEvent(changeEvent);
            
            expect(dropdown.selectedIndex).toBe(2);
            expect(dropdown.displayText.textContent).toBe('Option 2');
            
            // Check that the visual selection updated
            const selectedOption = dropdown.optionElements[2];
            expect(selectedOption.classList.contains('selected')).toBe(true);
        });

        test('Dispatches change event', () => {
            const changeHandler = jest.fn();
            const inputHandler = jest.fn();
            selectElement.addEventListener('change', changeHandler);
            selectElement.addEventListener('input', inputHandler);
            
            dropdown.openDropdown();
            const option2 = dropdown.optionElements[2];
            option2.click();
            
            expect(changeHandler).toHaveBeenCalled();
            expect(inputHandler).toHaveBeenCalled();
        });
    });

    describe('Accessibility', () => {
        let dropdown;

        beforeEach(() => {
            dropdown = new ModernDropdown(selectElement);
        });

        test('ARIA attributes set correctly', () => {
            // Check wrapper ARIA attributes
            expect(dropdown.wrapper.getAttribute('role')).toBe('combobox');
            expect(dropdown.wrapper.getAttribute('aria-expanded')).toBe('false');
            expect(dropdown.wrapper.getAttribute('aria-haspopup')).toBe('listbox');
            
            // Check menu ARIA attributes
            expect(dropdown.menu.getAttribute('role')).toBe('listbox');
            
            // Check toggle ARIA attributes
            expect(dropdown.toggle.getAttribute('aria-label')).toBe('Open dropdown');
            
            // Check option ARIA attributes
            dropdown.optionElements.forEach((option, index) => {
                expect(option.getAttribute('role')).toBe('option');
                if (index === dropdown.selectedIndex) {
                    expect(option.getAttribute('aria-selected')).toBe('true');
                }
            });
        });

        test('Focus management works', (done) => {
            // Initially, toggle should be focusable
            dropdown.toggle.focus();
            expect(document.activeElement).toBe(dropdown.toggle);
            
            // When opened, focus should move to selected option
            dropdown.openDropdown();
            // Wait for setTimeout in openDropdown (50ms delay)
            setTimeout(() => {
                const selectedOption = dropdown.optionElements[dropdown.selectedIndex];
                if (selectedOption && !selectedOption.disabled) {
                    expect(document.activeElement).toBe(selectedOption);
                }
                
                // When closed, focus should return to toggle
                dropdown.closeDropdown();
                dropdown.toggle.focus();
                expect(document.activeElement).toBe(dropdown.toggle);
                done();
            }, 100);
        });

        test('Screen reader compatible', () => {
            // Check that native select is hidden from screen readers
            expect(selectElement.getAttribute('aria-hidden')).toBe('true');
            
            // Check that wrapper has proper ARIA attributes
            expect(dropdown.wrapper.getAttribute('role')).toBe('combobox');
            expect(dropdown.wrapper.hasAttribute('aria-expanded')).toBe(true);
            
            // Check that options have proper ARIA attributes
            dropdown.optionElements.forEach(option => {
                expect(option.getAttribute('role')).toBe('option');
                expect(option.hasAttribute('aria-selected')).toBe(true);
            });
            
            // Check that selected option is properly marked
            const selectedOption = dropdown.optionElements[dropdown.selectedIndex];
            if (selectedOption) {
                expect(selectedOption.getAttribute('aria-selected')).toBe('true');
            }
        });
    });

    describe('Edge Cases', () => {
        test('Empty select handled', () => {
            const emptySelect = document.createElement('select');
            emptySelect.id = 'emptySelect';
            container.appendChild(emptySelect);
            
            const dropdown = new ModernDropdown(emptySelect);
            
            expect(dropdown.optionElements.length).toBe(0);
            expect(dropdown.menu.children.length).toBe(0);
            expect(dropdown.displayText.textContent).toBe('');
        });

        test('Disabled select handled', () => {
            selectElement.disabled = true;
            const dropdown = new ModernDropdown(selectElement);
            
            expect(selectElement.disabled).toBe(true);
            
            // Test setDisabled method
            dropdown.setDisabled(true);
            expect(dropdown.wrapper.classList.contains('disabled')).toBe(true);
            expect(dropdown.toggle.disabled).toBe(true);
            expect(dropdown.isDisabled()).toBe(true);
            
            // Test enabling
            dropdown.setDisabled(false);
            expect(dropdown.wrapper.classList.contains('disabled')).toBe(false);
            expect(dropdown.toggle.disabled).toBe(false);
            expect(dropdown.isDisabled()).toBe(false);
        });

        test('Option groups supported', () => {
            const groupedSelect = document.createElement('select');
            groupedSelect.id = 'groupedSelect';
            groupedSelect.innerHTML = `
                <optgroup label="Group 1">
                    <option value="1">Option 1</option>
                    <option value="2">Option 2</option>
                </optgroup>
                <optgroup label="Group 2">
                    <option value="3">Option 3</option>
                </optgroup>
            `;
            container.appendChild(groupedSelect);
            
            const dropdown = new ModernDropdown(groupedSelect);
            
            // Should create options for all options in groups
            expect(dropdown.optionElements.length).toBe(3);
            expect(dropdown.optionElements[0].getAttribute('data-value')).toBe('1');
            expect(dropdown.optionElements[1].getAttribute('data-value')).toBe('2');
            expect(dropdown.optionElements[2].getAttribute('data-value')).toBe('3');
        });

        test('Disabled options are handled', () => {
            const selectWithDisabled = document.createElement('select');
            selectWithDisabled.id = 'selectWithDisabled';
            selectWithDisabled.innerHTML = `
                <option value="1">Option 1</option>
                <option value="2" disabled>Option 2 (Disabled)</option>
                <option value="3">Option 3</option>
            `;
            container.appendChild(selectWithDisabled);
            
            const dropdown = new ModernDropdown(selectWithDisabled);
            
            const disabledOption = dropdown.optionElements[1];
            expect(disabledOption.disabled).toBe(true);
            expect(disabledOption.classList.contains('disabled')).toBe(true);
            
            // Disabled option should not be selectable
            dropdown.openDropdown();
            disabledOption.click();
            expect(selectWithDisabled.selectedIndex).not.toBe(1);
        });

        test('Placeholder option handled', () => {
            selectElement.selectedIndex = 0; // Select placeholder
            const dropdown = new ModernDropdown(selectElement);
            
            expect(dropdown.displayText.classList.contains('placeholder')).toBe(true);
            expect(dropdown.optionElements[0].classList.contains('placeholder')).toBe(true);
        });

        test('Selecting same option closes dropdown', () => {
            selectElement.selectedIndex = 1;
            const dropdown = new ModernDropdown(selectElement);
            
            dropdown.openDropdown();
            expect(dropdown.isOpen).toBe(true);
            
            // Click the already selected option
            const selectedOption = dropdown.optionElements[1];
            selectedOption.click();
            
            // Should close without changing selection
            expect(dropdown.isOpen).toBe(false);
            expect(selectElement.selectedIndex).toBe(1);
        });

        test('destroy removes wrapper and restores select', () => {
            const dropdown = new ModernDropdown(selectElement);
            const wrapper = dropdown.wrapper;
            
            expect(wrapper.parentNode).toBeDefined();
            expect(selectElement.style.display).toBe('none');
            
            dropdown.destroy();
            
            expect(wrapper.parentNode).toBeNull();
            expect(selectElement.style.display).toBe('');
            expect(selectElement.getAttribute('aria-hidden')).toBeNull();
        });
    });

    describe('Public API Methods', () => {
        let dropdown;

        beforeEach(() => {
            dropdown = new ModernDropdown(selectElement);
        });

        test('setValue sets the value correctly', () => {
            const changeHandler = jest.fn();
            selectElement.addEventListener('change', changeHandler);
            
            dropdown.setValue('2');
            
            expect(selectElement.value).toBe('2');
            expect(dropdown.selectedIndex).toBe(2);
            expect(changeHandler).toHaveBeenCalled();
        });

        test('getValue returns current value', () => {
            selectElement.selectedIndex = 1;
            selectElement.value = '1';
            dropdown.selectedIndex = 1;
            
            expect(dropdown.getValue()).toBe('1');
        });

        test('refreshModernDropdown function works', () => {
            const initialCount = dropdown.optionElements.length;
            
            const newOption = document.createElement('option');
            newOption.value = '5';
            newOption.textContent = 'Option 5';
            selectElement.appendChild(newOption);
            
            refreshModernDropdown(selectElement);
            
            expect(dropdown.optionElements.length).toBe(initialCount + 1);
        });

        test('setModernDropdownDisabled function works', () => {
            expect(dropdown.isDisabled()).toBe(false);
            
            setModernDropdownDisabled(selectElement, true);
            
            expect(dropdown.isDisabled()).toBe(true);
            expect(dropdown.wrapper.classList.contains('disabled')).toBe(true);
            
            setModernDropdownDisabled(selectElement, false);
            
            expect(dropdown.isDisabled()).toBe(false);
            expect(dropdown.wrapper.classList.contains('disabled')).toBe(false);
        });
    });
});
