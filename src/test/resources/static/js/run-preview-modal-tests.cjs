/**
 * Node.js test runner for File Preview Modal Property-Based Tests
 * Run with: node src/test/resources/static/js/run-preview-modal-tests.cjs
 */

// Mock browser globals for Node.js environment
global.window = global;
global.document = {
    createElement: () => ({ textContent: '', innerHTML: '' }),
    body: { appendChild: () => {}, removeChild: () => {} },
    getElementById: () => null,
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => {}
};
global.CustomEvent = class CustomEvent {
    constructor(name, options) {
        this.name = name;
        this.detail = options?.detail;
        this.bubbles = options?.bubbles || false;
        this.cancelable = options?.cancelable || false;
    }
};
global.Blob = class Blob {
    constructor(data) {
        this.data = data;
    }
};
global.URL = {
    createObjectURL: () => 'mock-url',
    revokeObjectURL: () => {}
};

// Import test file
const fs = require('fs');
const path = require('path');

const testFilePath = path.join(__dirname, 'file-preview-modal-pbt.test.js');
const testCode = fs.readFileSync(testFilePath, 'utf8');

// Remove ES6 imports/exports for Node.js
const modifiedCode = testCode
    .replace(/import .* from .*;\n/g, '')
    .replace(/export .*/g, '');

// Execute test code
eval(modifiedCode);

// Run tests
(async () => {
    try {
        console.log('Starting File Preview Modal Property-Based Tests...\n');
        const allPassed = await runAllTests();
        
        if (allPassed) {
            console.log('\n✅ All property-based tests passed!');
            process.exit(0);
        } else {
            console.log('\n❌ Some property-based tests failed!');
            process.exit(1);
        }
    } catch (error) {
        console.error('\n❌ Error running tests:', error);
        console.error(error.stack);
        process.exit(1);
    }
})();
