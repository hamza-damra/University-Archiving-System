/**
 * Node.js test runner for Office Renderer Property-Based Tests
 * 
 * This script runs the Office renderer property-based tests in a Node.js environment.
 * It uses JSDOM to simulate a browser environment.
 * 
 * Usage: node run-office-renderer-tests.cjs
 */

const fs = require('fs');
const path = require('path');

// Read the test file
const testFilePath = path.join(__dirname, 'office-renderer-pbt.test.js');
const testCode = fs.readFileSync(testFilePath, 'utf8');

// Create a minimal browser-like environment
global.window = {
    mockFetch: null,
    runOfficeRendererTests: null
};

global.document = {
    createElement: function(tagName) {
        return {
            tagName: tagName.toUpperCase(),
            className: '',
            innerHTML: '',
            textContent: '',
            style: {},
            attributes: {},
            children: [],
            appendChild: function(child) {
                this.children.push(child);
            },
            querySelector: function(selector) {
                // Simple mock implementation
                if (selector === 'iframe') {
                    return this.children.find(c => c.tagName === 'IFRAME');
                }
                if (selector.startsWith('.')) {
                    const className = selector.substring(1);
                    return this.children.find(c => c.className && c.className.includes(className));
                }
                return null;
            },
            setAttribute: function(name, value) {
                this.attributes[name] = value;
            },
            getAttribute: function(name) {
                return this.attributes[name];
            }
        };
    }
};

global.URL = {
    createObjectURL: function(blob) {
        return 'blob:mock-url-' + Math.random();
    },
    revokeObjectURL: function(url) {
        // Mock implementation
    }
};

global.Blob = class Blob {
    constructor(parts, options) {
        this.parts = parts;
        this.options = options || {};
        this.size = parts.reduce((acc, part) => acc + (part.length || 0), 0);
        this.type = options.type || '';
    }
    
    async text() {
        return this.parts.join('');
    }
};

// Execute the test code
eval(testCode);

// Run the tests
async function main() {
    console.log('\n' + '='.repeat(70));
    console.log('OFFICE RENDERER PROPERTY-BASED TESTS (Node.js Runner)');
    console.log('='.repeat(70) + '\n');
    
    try {
        const allPassed = await window.runOfficeRendererTests();
        
        console.log('\n' + '='.repeat(70));
        if (allPassed) {
            console.log('✓ ALL TESTS PASSED');
            console.log('='.repeat(70) + '\n');
            process.exit(0);
        } else {
            console.log('✗ SOME TESTS FAILED');
            console.log('='.repeat(70) + '\n');
            process.exit(1);
        }
    } catch (error) {
        console.error('\n' + '='.repeat(70));
        console.error('✗ ERROR RUNNING TESTS');
        console.error('='.repeat(70));
        console.error(error);
        process.exit(1);
    }
}

main();
