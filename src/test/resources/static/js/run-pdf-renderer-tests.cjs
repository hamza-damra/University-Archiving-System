/**
 * Node.js test runner for PDF Renderer Property-Based Tests
 * 
 * This script runs the PDF renderer property-based tests in a Node.js environment.
 * It uses JSDOM to simulate a browser environment for DOM operations.
 */

const fs = require('fs');
const path = require('path');

// Read the test file
const testFilePath = path.join(__dirname, 'pdf-renderer-pbt.test.js');
const testCode = fs.readFileSync(testFilePath, 'utf-8');

// Create a minimal DOM environment
global.document = {
    createElement: function(tagName) {
        return {
            tagName: tagName.toUpperCase(),
            className: '',
            src: '',
            type: '',
            value: '',
            min: '',
            max: '',
            textContent: '',
            innerHTML: '',
            appendChild: function(child) {
                if (!this.children) this.children = [];
                this.children.push(child);
            },
            querySelector: function(selector) {
                if (!this.children) return null;
                
                // Simple selector matching
                for (const child of this.children) {
                    if (selector.startsWith('.') && child.className === selector.substring(1)) {
                        return child;
                    }
                    if (selector.startsWith('#') && child.id === selector.substring(1)) {
                        return child;
                    }
                }
                return null;
            }
        };
    }
};

global.window = {
    mockFetch: null,
    runPDFRendererTests: null
};

// Evaluate the test code
eval(testCode);

// Run the tests
async function runTests() {
    console.log('Starting PDF Renderer Property-Based Tests...\n');
    
    try {
        const allPassed = await window.runPDFRendererTests();
        
        console.log('\n');
        if (allPassed) {
            console.log('✅ All property-based tests passed!\n');
            process.exit(0);
        } else {
            console.log('❌ Some property-based tests failed!\n');
            process.exit(1);
        }
    } catch (error) {
        console.error('❌ Error running tests:', error);
        process.exit(1);
    }
}

runTests();
