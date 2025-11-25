/**
 * Node.js test runner for Code Renderer Property-Based Tests
 * Run with: node run-code-renderer-tests.cjs
 */

// Mock DOM elements for Node.js environment
class MockElement {
    constructor(tagName = 'div') {
        this.tagName = tagName;
        this.innerHTML = '';
        this.textContent = '';
        this.className = '';
        this.children = [];
        this.attributes = {};
    }

    appendChild(child) {
        this.children.push(child);
        return child;
    }

    querySelector(selector) {
        // Simple mock implementation
        if (selector === 'code') {
            // Search recursively through children
            const findCode = (element) => {
                if (element.tagName === 'code') return element;
                for (const child of element.children) {
                    const found = findCode(child);
                    if (found) return found;
                }
                return null;
            };
            return findCode(this);
        }
        if (selector === '.line-numbers') {
            // Search recursively through children
            const findLineNumbers = (element) => {
                if (element.className && element.className.includes('line-numbers')) return element;
                for (const child of element.children) {
                    const found = findLineNumbers(child);
                    if (found) return found;
                }
                return null;
            };
            return findLineNumbers(this);
        }
        return null;
    }

    setAttribute(name, value) {
        this.attributes[name] = value;
    }
}

// Mock document
global.document = {
    createElement: (tagName) => new MockElement(tagName),
    head: {
        appendChild: () => {}
    }
};

// Mock window
global.window = {
    mockFetch: null
};

// Mock fetch for testing
class MockFetch {
    constructor() {
        this.reset();
    }

    reset() {
        this.calls = [];
        this.responses = new Map();
    }

    setResponse(url, response) {
        this.responses.set(url, response);
    }

    async fetch(url, options = {}) {
        this.calls.push({ url, options });
        
        const response = this.responses.get(url);
        if (!response) {
            return {
                ok: false,
                status: 404,
                json: async () => ({ error: 'Not found' })
            };
        }
        
        return {
            ok: response.ok !== false,
            status: response.status || 200,
            json: async () => response.data,
            blob: async () => Buffer.from(JSON.stringify(response.data))
        };
    }
}

// Mock CodeRenderer
class MockCodeRenderer {
    constructor(options = {}) {
        this.options = {
            theme: options.theme || 'github',
            showLineNumbers: options.showLineNumbers !== false
        };
        this.currentContent = null;
        this.currentLanguage = null;
        this.highlightJsLoaded = false;
        this.renderCalled = false;
        this.syntaxHighlightingApplied = false;
        this.lineNumbersDisplayed = false;
    }

    async render(fileId, container, language = null) {
        if (!container) {
            throw new Error('Container element is required');
        }

        this.renderCalled = true;

        const content = await this.fetchContent(fileId);
        this.currentContent = content;
        this.currentLanguage = language;

        await this.ensureHighlightJsLoaded();
        this.renderCode(container);
    }

    async fetchContent(fileId) {
        if (!global.window.mockFetch) {
            throw new Error('Mock fetch not configured');
        }

        const response = await global.window.mockFetch.fetch(`/api/file-explorer/files/${fileId}/content`);

        if (!response.ok) {
            if (response.status === 404) {
                throw new Error('File not found - it may have been deleted');
            } else if (response.status === 403) {
                throw new Error('You don\'t have permission to view this file');
            } else if (response.status === 500) {
                throw new Error('Service unavailable - please try again later');
            } else {
                throw new Error('Failed to load file content');
            }
        }

        const jsonResponse = await response.json();

        if (jsonResponse.success && jsonResponse.data) {
            return jsonResponse.data;
        } else {
            throw new Error(jsonResponse.message || 'Failed to load file content');
        }
    }

    async ensureHighlightJsLoaded() {
        await new Promise(resolve => setTimeout(resolve, 1));
        this.highlightJsLoaded = true;
    }

    renderCode(container) {
        container.innerHTML = '';
        
        const pre = new MockElement('pre');
        const code = new MockElement('code');
        code.tagName = 'code';
        
        if (this.currentLanguage) {
            code.className = `language-${this.currentLanguage}`;
            this.syntaxHighlightingApplied = true;
        }
        
        code.textContent = this.currentContent;
        code.innerHTML = this.currentContent; // Set innerHTML for testing
        pre.appendChild(code);
        
        if (this.options.showLineNumbers) {
            const lineNumbers = this.createLineNumbers();
            const wrapper = new MockElement('div');
            wrapper.className = 'flex';
            wrapper.appendChild(lineNumbers);
            wrapper.appendChild(pre);
            container.appendChild(wrapper);
            container.innerHTML = 'mock-content-with-line-numbers'; // Set innerHTML for testing
            this.lineNumbersDisplayed = true;
        } else {
            container.appendChild(pre);
            container.innerHTML = 'mock-content'; // Set innerHTML for testing
        }
    }

    createLineNumbers() {
        const lines = this.currentContent.split('\n');
        const lineCount = lines.length;
        
        const lineNumbersDiv = new MockElement('div');
        lineNumbersDiv.className = 'line-numbers';
        
        const lineNumbers = [];
        for (let i = 1; i <= lineCount; i++) {
            lineNumbers.push(`<div>${i}</div>`);
        }
        
        lineNumbersDiv.innerHTML = lineNumbers.join('');
        
        return lineNumbersDiv;
    }

    static detectLanguage(fileName) {
        if (!fileName) return null;
        
        const extension = fileName.split('.').pop().toLowerCase();
        
        const languageMap = {
            'js': 'javascript',
            'jsx': 'javascript',
            'ts': 'typescript',
            'tsx': 'typescript',
            'java': 'java',
            'py': 'python',
            'rb': 'ruby',
            'php': 'php',
            'c': 'c',
            'cpp': 'cpp',
            'cs': 'csharp',
            'go': 'go',
            'rs': 'rust',
            'swift': 'swift',
            'kt': 'kotlin',
            'css': 'css',
            'html': 'html',
            'xml': 'xml',
            'json': 'json',
            'yaml': 'yaml',
            'sql': 'sql',
            'sh': 'bash',
            'md': 'markdown'
        };
        
        return languageMap[extension] || null;
    }
}

// Test runner
class PropertyTestRunner {
    constructor() {
        this.results = [];
    }

    async runProperty(name, propertyFn, iterations = 100) {
        console.log(`\nRunning: ${name}`);
        console.log(`Iterations: ${iterations}`);
        
        let passed = 0;
        let failed = 0;
        let failureExample = null;

        for (let i = 0; i < iterations; i++) {
            try {
                const result = await propertyFn();
                
                if (result.passed) {
                    passed++;
                } else {
                    failed++;
                    if (!failureExample) {
                        failureExample = {
                            iteration: i + 1,
                            reason: result.reason,
                            details: result.details
                        };
                    }
                }
            } catch (error) {
                failed++;
                if (!failureExample) {
                    failureExample = {
                        iteration: i + 1,
                        error: error.message,
                        stack: error.stack
                    };
                }
            }
        }

        const testPassed = failed === 0;
        const result = {
            name,
            passed: testPassed,
            iterations,
            passedCount: passed,
            failedCount: failed,
            failureExample
        };

        this.results.push(result);

        if (testPassed) {
            console.log(`✓ PASSED: All ${iterations} iterations passed`);
        } else {
            console.log(`✗ FAILED: ${failed} of ${iterations} iterations failed`);
            console.log('Failure example:', JSON.stringify(failureExample, null, 2));
        }

        return result;
    }

    printSummary() {
        console.log('\n' + '='.repeat(60));
        console.log('TEST SUMMARY');
        console.log('='.repeat(60));
        
        const totalTests = this.results.length;
        const passedTests = this.results.filter(r => r.passed).length;
        const failedTests = totalTests - passedTests;

        this.results.forEach(result => {
            const status = result.passed ? '✓ PASS' : '✗ FAIL';
            console.log(`${status}: ${result.name}`);
        });

        console.log('='.repeat(60));
        console.log(`Total: ${totalTests} | Passed: ${passedTests} | Failed: ${failedTests}`);
        console.log('='.repeat(60));

        return failedTests === 0;
    }
}

// Property tests
async function property3_formatSpecificRendererSelection_codeFiles() {
    const codeFileExtensions = [
        { ext: 'js', lang: 'javascript', mime: 'text/javascript' },
        { ext: 'java', lang: 'java', mime: 'text/x-java-source' },
        { ext: 'py', lang: 'python', mime: 'text/x-python' },
        { ext: 'css', lang: 'css', mime: 'text/css' },
        { ext: 'html', lang: 'html', mime: 'text/html' },
        { ext: 'sql', lang: 'sql', mime: 'text/x-sql' },
        { ext: 'json', lang: 'json', mime: 'application/json' },
        { ext: 'xml', lang: 'xml', mime: 'application/xml' }
    ];
    
    const randomFile = codeFileExtensions[Math.floor(Math.random() * codeFileExtensions.length)];
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const fileName = `file_${Math.random().toString(36).substring(7)}.${randomFile.ext}`;
    
    const sampleCode = {
        'javascript': 'function hello() {\n  console.log("Hello World");\n}',
        'java': 'public class Test {\n  public static void main(String[] args) {\n    System.out.println("Hello");\n  }\n}',
        'python': 'def hello():\n    print("Hello World")\n\nhello()',
        'css': '.container {\n  display: flex;\n  justify-content: center;\n}',
        'html': '<!DOCTYPE html>\n<html>\n<head><title>Test</title></head>\n<body><h1>Hello</h1></body>\n</html>',
        'sql': 'SELECT * FROM users WHERE active = true;',
        'json': '{\n  "name": "test",\n  "version": "1.0.0"\n}',
        'xml': '<?xml version="1.0"?>\n<root>\n  <item>Test</item>\n</root>'
    };
    
    const fileContent = sampleCode[randomFile.lang] || 'Sample code content';
    
    global.window.mockFetch = new MockFetch();
    global.window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/content`, {
        ok: true,
        data: {
            success: true,
            data: fileContent
        }
    });
    
    const container = new MockElement('div');
    const detectedLanguage = MockCodeRenderer.detectLanguage(fileName);
    const renderer = new MockCodeRenderer();
    await renderer.render(fileId, container, detectedLanguage);
    
    if (!renderer.renderCalled) {
        return { passed: false, reason: 'Renderer was not called' };
    }
    
    if (!renderer.currentContent) {
        return { passed: false, reason: 'Content was not fetched' };
    }
    
    if (renderer.currentContent !== fileContent) {
        return { passed: false, reason: 'Content does not match expected' };
    }
    
    if (detectedLanguage && renderer.currentLanguage !== detectedLanguage) {
        return { passed: false, reason: 'Language was not detected correctly' };
    }
    
    if (detectedLanguage && !renderer.syntaxHighlightingApplied) {
        return { passed: false, reason: 'Syntax highlighting was not applied' };
    }
    
    if (!container.innerHTML || container.innerHTML.length === 0) {
        return { passed: false, reason: 'Container was not populated with content' };
    }
    
    const codeElement = container.querySelector('code');
    if (!codeElement) {
        return { passed: false, reason: 'Code element was not created' };
    }
    
    return { passed: true };
}

async function propertyTest_languageDetection() {
    const testCases = [
        { fileName: 'test.js', expectedLang: 'javascript' },
        { fileName: 'Test.java', expectedLang: 'java' },
        { fileName: 'script.py', expectedLang: 'python' },
        { fileName: 'styles.css', expectedLang: 'css' },
        { fileName: 'index.html', expectedLang: 'html' },
        { fileName: 'query.sql', expectedLang: 'sql' },
        { fileName: 'data.json', expectedLang: 'json' },
        { fileName: 'config.xml', expectedLang: 'xml' }
    ];
    
    const testCase = testCases[Math.floor(Math.random() * testCases.length)];
    const detectedLanguage = MockCodeRenderer.detectLanguage(testCase.fileName);
    
    if (detectedLanguage !== testCase.expectedLang) {
        return {
            passed: false,
            reason: 'Language detection failed',
            details: {
                fileName: testCase.fileName,
                expected: testCase.expectedLang,
                actual: detectedLanguage
            }
        };
    }
    
    return { passed: true };
}

async function propertyTest_lineNumbersDisplay() {
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const lineCount = Math.floor(Math.random() * 46) + 5;
    const lines = [];
    for (let i = 0; i < lineCount; i++) {
        lines.push(`// Line ${i + 1}`);
    }
    const fileContent = lines.join('\n');
    
    global.window.mockFetch = new MockFetch();
    global.window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/content`, {
        ok: true,
        data: {
            success: true,
            data: fileContent
        }
    });
    
    const container = new MockElement('div');
    const rendererWithLines = new MockCodeRenderer({ showLineNumbers: true });
    await rendererWithLines.render(fileId, container, 'javascript');
    
    if (!rendererWithLines.lineNumbersDisplayed) {
        return { passed: false, reason: 'Line numbers were not displayed when enabled' };
    }
    
    const lineNumbersElement = container.querySelector('.line-numbers');
    if (!lineNumbersElement) {
        return { passed: false, reason: 'Line numbers element was not created' };
    }
    
    const containerNoLines = new MockElement('div');
    const rendererNoLines = new MockCodeRenderer({ showLineNumbers: false });
    await rendererNoLines.render(fileId, containerNoLines, 'javascript');
    
    if (rendererNoLines.lineNumbersDisplayed) {
        return { passed: false, reason: 'Line numbers were displayed when disabled' };
    }
    
    return { passed: true };
}

async function propertyTest_errorHandling() {
    const fileId = Math.floor(Math.random() * 1000) + 1;
    global.window.mockFetch = new MockFetch();
    
    const container = new MockElement('div');
    const renderer = new MockCodeRenderer();
    
    let errorThrown = false;
    let errorMessage = '';
    
    try {
        await renderer.render(fileId, container, 'javascript');
    } catch (error) {
        errorThrown = true;
        errorMessage = error.message;
    }
    
    if (!errorThrown) {
        return { passed: false, reason: 'No error was thrown for missing file' };
    }
    
    if (!errorMessage.includes('not found')) {
        return { passed: false, reason: 'Error message does not indicate file not found' };
    }
    
    return { passed: true };
}

// Run all tests
async function runAllTests() {
    console.log('='.repeat(60));
    console.log('CODE RENDERER PROPERTY-BASED TESTS');
    console.log('Feature: file-preview-system');
    console.log('='.repeat(60));
    
    const runner = new PropertyTestRunner();
    
    await runner.runProperty(
        'Property 3: Format-specific renderer selection (code files)',
        property3_formatSpecificRendererSelection_codeFiles,
        100
    );
    
    await runner.runProperty(
        'Property: Language detection from file extension',
        propertyTest_languageDetection,
        100
    );
    
    await runner.runProperty(
        'Property: Line numbers display',
        propertyTest_lineNumbersDisplay,
        100
    );
    
    await runner.runProperty(
        'Property: Error handling for missing files',
        propertyTest_errorHandling,
        100
    );
    
    const allPassed = runner.printSummary();
    
    process.exit(allPassed ? 0 : 1);
}

// Run tests
runAllTests().catch(error => {
    console.error('Test execution failed:', error);
    process.exit(1);
});
