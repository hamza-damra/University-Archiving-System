/**
 * Property-Based Tests for Text Search Functionality
 * 
 * Feature: file-preview-system, Property 16: Search functionality in preview
 * Validates: Requirements 7.4
 * 
 * Property 16: Search functionality in preview
 * For any text-based preview, searching for a query string should highlight all matches 
 * and provide navigation between them
 */

import { TextRenderer } from '../../../../main/resources/static/js/text-renderer.js';

// Simple property-based testing framework
class PropertyTest {
    constructor(name, property, options = {}) {
        this.name = name;
        this.property = property;
        this.iterations = options.iterations || 100;
        this.results = [];
    }

    async run() {
        console.log(`Running property test: ${this.name}`);
        
        for (let i = 0; i < this.iterations; i++) {
            try {
                await this.property(i);
                this.results.push({ iteration: i, passed: true });
            } catch (error) {
                this.results.push({ 
                    iteration: i, 
                    passed: false, 
                    error: error.message,
                    stack: error.stack 
                });
                console.error(`Iteration ${i} failed:`, error);
                return false; // Stop on first failure
            }
        }
        
        return true;
    }

    getResults() {
        const passed = this.results.filter(r => r.passed).length;
        const failed = this.results.filter(r => !r.passed).length;
        
        return {
            name: this.name,
            passed,
            failed,
            total: this.iterations,
            success: failed === 0,
            failures: this.results.filter(r => !r.passed)
        };
    }
}

// Generators for property-based testing
const generators = {
    // Generate random text content
    randomText(seed) {
        const words = ['test', 'search', 'content', 'file', 'preview', 'text', 'document', 'line', 'word', 'match'];
        const sentences = [];
        const numSentences = 5 + (seed % 20);
        
        for (let i = 0; i < numSentences; i++) {
            const numWords = 3 + ((seed + i) % 10);
            const sentence = [];
            for (let j = 0; j < numWords; j++) {
                sentence.push(words[(seed + i + j) % words.length]);
            }
            sentences.push(sentence.join(' ') + '.');
        }
        
        return sentences.join('\n');
    },

    // Generate random search query from content
    randomQuery(content, seed) {
        const words = content.split(/\s+/).filter(w => w.length > 0);
        if (words.length === 0) return 'test';
        
        const index = seed % words.length;
        return words[index].replace(/[.,!?;:]/, '');
    },

    // Generate text with known number of matches
    textWithMatches(query, matchCount, seed) {
        const filler = ['Lorem ipsum', 'dolor sit amet', 'consectetur adipiscing', 'elit sed do'];
        const parts = [];
        
        for (let i = 0; i < matchCount; i++) {
            parts.push(filler[i % filler.length]);
            parts.push(query);
        }
        parts.push(filler[0]); // Add one more filler at the end
        
        return parts.join(' ');
    },

    // Generate large text content
    largeText(seed) {
        const lines = [];
        const numLines = 100 + (seed % 900); // 100-1000 lines
        
        for (let i = 0; i < numLines; i++) {
            lines.push(`Line ${i}: ${this.randomText(seed + i)}`);
        }
        
        return lines.join('\n');
    }
};

// Property Tests

/**
 * Property 1: Search finds all occurrences
 * For any text content and search query, the number of matches found should equal 
 * the actual number of occurrences in the text
 */
const property1 = new PropertyTest(
    'Property 1: Search finds all occurrences',
    async (seed) => {
        const query = 'test';
        const matchCount = 3 + (seed % 10);
        const content = generators.textWithMatches(query, matchCount, seed);
        
        const renderer = new TextRenderer();
        renderer.currentContent = content;
        renderer.currentLines = content.split('\n');
        
        const foundMatches = renderer.search(query);
        
        if (foundMatches !== matchCount) {
            throw new Error(`Expected ${matchCount} matches, found ${foundMatches}`);
        }
    },
    { iterations: 100 }
);

/**
 * Property 2: Search is case-insensitive
 * For any text content, searching for a query should find matches regardless of case
 */
const property2 = new PropertyTest(
    'Property 2: Search is case-insensitive',
    async (seed) => {
        const content = 'Test TEST test TeSt';
        
        const renderer = new TextRenderer();
        renderer.currentContent = content;
        renderer.currentLines = content.split('\n');
        
        const foundMatches = renderer.search('test');
        
        if (foundMatches !== 4) {
            throw new Error(`Expected 4 matches (case-insensitive), found ${foundMatches}`);
        }
    },
    { iterations: 50 }
);

/**
 * Property 3: Navigation cycles through matches
 * For any text with multiple matches, navigating next from the last match should 
 * return to the first match
 */
const property3 = new PropertyTest(
    'Property 3: Navigation cycles through matches',
    async (seed) => {
        const query = 'match';
        const matchCount = 3 + (seed % 7);
        const content = generators.textWithMatches(query, matchCount, seed);
        
        const renderer = new TextRenderer();
        renderer.currentContent = content;
        renderer.currentLines = content.split('\n');
        
        renderer.search(query);
        
        // Navigate to last match
        for (let i = 0; i < matchCount - 1; i++) {
            renderer.nextMatch();
        }
        
        const lastMatch = renderer.getCurrentMatch();
        if (lastMatch.index !== matchCount - 1) {
            throw new Error(`Expected to be at last match (${matchCount - 1}), at ${lastMatch.index}`);
        }
        
        // Navigate one more time should cycle to first
        renderer.nextMatch();
        const firstMatch = renderer.getCurrentMatch();
        
        if (firstMatch.index !== 0) {
            throw new Error(`Expected to cycle to first match (0), at ${firstMatch.index}`);
        }
    },
    { iterations: 100 }
);

/**
 * Property 4: Previous navigation cycles backwards
 * For any text with multiple matches, navigating previous from the first match 
 * should go to the last match
 */
const property4 = new PropertyTest(
    'Property 4: Previous navigation cycles backwards',
    async (seed) => {
        const query = 'word';
        const matchCount = 3 + (seed % 7);
        const content = generators.textWithMatches(query, matchCount, seed);
        
        const renderer = new TextRenderer();
        renderer.currentContent = content;
        renderer.currentLines = content.split('\n');
        
        renderer.search(query);
        
        // Should start at first match (index 0)
        const firstMatch = renderer.getCurrentMatch();
        if (firstMatch.index !== 0) {
            throw new Error(`Expected to start at first match (0), at ${firstMatch.index}`);
        }
        
        // Navigate previous should go to last match
        renderer.previousMatch();
        const lastMatch = renderer.getCurrentMatch();
        
        if (lastMatch.index !== matchCount - 1) {
            throw new Error(`Expected to cycle to last match (${matchCount - 1}), at ${lastMatch.index}`);
        }
    },
    { iterations: 100 }
);

/**
 * Property 5: Empty query returns zero matches
 * For any text content, searching with an empty query should return zero matches
 */
const property5 = new PropertyTest(
    'Property 5: Empty query returns zero matches',
    async (seed) => {
        const content = generators.randomText(seed);
        
        const renderer = new TextRenderer();
        renderer.currentContent = content;
        renderer.currentLines = content.split('\n');
        
        const foundMatches = renderer.search('');
        
        if (foundMatches !== 0) {
            throw new Error(`Expected 0 matches for empty query, found ${foundMatches}`);
        }
    },
    { iterations: 50 }
);

/**
 * Property 6: Match positions are correct
 * For any text content and query, each match should have correct start and end positions
 */
const property6 = new PropertyTest(
    'Property 6: Match positions are correct',
    async (seed) => {
        const query = 'find';
        const content = `Start find middle find end find finish`;
        
        const renderer = new TextRenderer();
        renderer.currentContent = content;
        renderer.currentLines = content.split('\n');
        
        renderer.search(query);
        
        // Check each match
        const matches = renderer.searchMatches;
        for (const match of matches) {
            const actualText = content.substring(match.start, match.end);
            if (actualText.toLowerCase() !== query.toLowerCase()) {
                throw new Error(`Match text "${actualText}" doesn't match query "${query}"`);
            }
        }
    },
    { iterations: 50 }
);

/**
 * Property 7: Clear search removes all matches
 * For any text content with search results, clearing search should reset match count to zero
 */
const property7 = new PropertyTest(
    'Property 7: Clear search removes all matches',
    async (seed) => {
        const content = generators.randomText(seed);
        const query = generators.randomQuery(content, seed);
        
        const renderer = new TextRenderer();
        renderer.currentContent = content;
        renderer.currentLines = content.split('\n');
        
        // Perform search
        const initialMatches = renderer.search(query);
        
        if (initialMatches === 0) {
            // Skip if no matches found
            return;
        }
        
        // Clear search
        renderer.clearSearch();
        
        // Check that matches are cleared
        if (renderer.searchMatches.length !== 0) {
            throw new Error(`Expected 0 matches after clear, found ${renderer.searchMatches.length}`);
        }
        
        if (renderer.currentMatchIndex !== -1) {
            throw new Error(`Expected currentMatchIndex to be -1 after clear, was ${renderer.currentMatchIndex}`);
        }
    },
    { iterations: 100 }
);

/**
 * Property 8: getCurrentMatch returns correct match info
 * For any text with matches, getCurrentMatch should return the match at currentMatchIndex
 */
const property8 = new PropertyTest(
    'Property 8: getCurrentMatch returns correct match info',
    async (seed) => {
        const query = 'item';
        const matchCount = 5;
        const content = generators.textWithMatches(query, matchCount, seed);
        
        const renderer = new TextRenderer();
        renderer.currentContent = content;
        renderer.currentLines = content.split('\n');
        
        renderer.search(query);
        
        // Check each match
        for (let i = 0; i < matchCount; i++) {
            const currentMatch = renderer.getCurrentMatch();
            
            if (currentMatch.index !== i) {
                throw new Error(`Expected match index ${i}, got ${currentMatch.index}`);
            }
            
            if (currentMatch.total !== matchCount) {
                throw new Error(`Expected total ${matchCount}, got ${currentMatch.total}`);
            }
            
            renderer.nextMatch();
        }
    },
    { iterations: 50 }
);

/**
 * Property 9: Line numbers are calculated correctly
 * For any multi-line text, match line numbers should correspond to actual line positions
 */
const property9 = new PropertyTest(
    'Property 9: Line numbers are calculated correctly',
    async (seed) => {
        const lines = [];
        const numLines = 5 + (seed % 10);
        
        for (let i = 0; i < numLines; i++) {
            lines.push(`Line ${i}: ${i % 2 === 0 ? 'target' : 'other'}`);
        }
        
        const content = lines.join('\n');
        
        const renderer = new TextRenderer();
        renderer.currentContent = content;
        renderer.currentLines = content.split('\n');
        
        renderer.search('target');
        
        // Check that matches are on even-numbered lines
        const matches = renderer.searchMatches;
        for (const match of matches) {
            if (match.line % 2 !== 0) {
                throw new Error(`Expected match on even line, found on line ${match.line}`);
            }
        }
    },
    { iterations: 100 }
);

/**
 * Property 10: Search works with special characters
 * For any text containing special regex characters, search should treat them as literals
 */
const property10 = new PropertyTest(
    'Property 10: Search works with special characters',
    async (seed) => {
        const specialChars = ['.', '*', '+', '?', '[', ']', '(', ')', '{', '}', '|', '^', '$'];
        const char = specialChars[seed % specialChars.length];
        const content = `Start ${char} middle ${char} end ${char}`;
        
        const renderer = new TextRenderer();
        renderer.currentContent = content;
        renderer.currentLines = content.split('\n');
        
        // This should find literal occurrences, not treat as regex
        // Note: Current implementation uses indexOf which treats as literal
        const foundMatches = renderer.search(char);
        
        if (foundMatches !== 3) {
            throw new Error(`Expected 3 matches for "${char}", found ${foundMatches}`);
        }
    },
    { iterations: 50 }
);

// Run all tests
async function runAllTests() {
    const tests = [
        property1,
        property2,
        property3,
        property4,
        property5,
        property6,
        property7,
        property8,
        property9,
        property10
    ];

    const resultsContainer = document.getElementById('test-results');
    const statusContainer = document.getElementById('test-status');
    
    statusContainer.textContent = 'Running tests...';
    
    const allResults = [];
    
    for (const test of tests) {
        const success = await test.run();
        const results = test.getResults();
        allResults.push(results);
        
        // Display results
        const resultDiv = document.createElement('div');
        resultDiv.className = `p-4 rounded-lg ${results.success ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'}`;
        
        let html = `
            <h3 class="font-semibold ${results.success ? 'text-green-900' : 'text-red-900'} mb-2">
                ${results.success ? '✓' : '✗'} ${results.name}
            </h3>
            <p class="${results.success ? 'text-green-700' : 'text-red-700'} text-sm">
                ${results.passed} / ${results.total} iterations passed
            </p>
        `;
        
        if (!results.success && results.failures.length > 0) {
            html += `
                <div class="mt-2 p-2 bg-red-100 rounded text-xs text-red-800">
                    <strong>First failure (iteration ${results.failures[0].iteration}):</strong><br>
                    ${results.failures[0].error}
                </div>
            `;
        }
        
        resultDiv.innerHTML = html;
        resultsContainer.appendChild(resultDiv);
    }
    
    // Update status
    const totalTests = allResults.length;
    const passedTests = allResults.filter(r => r.success).length;
    const failedTests = totalTests - passedTests;
    
    statusContainer.innerHTML = `
        <div class="font-semibold ${failedTests === 0 ? 'text-green-700' : 'text-red-700'}">
            ${failedTests === 0 ? '✓ All tests passed!' : `✗ ${failedTests} test(s) failed`}
        </div>
        <div class="text-sm mt-1">
            ${passedTests} / ${totalTests} property tests passed
        </div>
    `;
    
    console.log('All tests completed');
    console.log(`Passed: ${passedTests} / ${totalTests}`);
    
    return failedTests === 0;
}

// Auto-run tests when page loads
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', runAllTests);
} else {
    runAllTests();
}

export { runAllTests };
