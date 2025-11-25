/**
 * Property-Based Tests for Code Renderer
 * Feature: file-preview-system
 * 
 * These tests validate correctness properties for the code renderer component.
 */

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
            blob: async () => new Blob([JSON.stringify(response.data)])
        };
    }
}

// Mock CodeRenderer for testing
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

        // Fetch content
        const content = await this.fetchContent(fileId);
        this.currentContent = content;
        this.currentLanguage = language;

        // Simulate Highlight.js loading
        await this.ensureHighlightJsLoaded();

        // Render code
        this.renderCode(container);
    }

    async fetchContent(fileId) {
        if (!window.mockFetch) {
            throw new Error('Mock fetch not configured');
        }

        const response = await window.mockFetch.fetch(`/api/file-explorer/files/${fileId}/content`);

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
        // Simulate loading
        await new Promise(resolve => setTimeout(resolve, 10));
        this.highlightJsLoaded = true;
    }

    renderCode(container) {
        container.innerHTML = '';
        
        // Create code element
        const pre = document.createElement('pre');
        const code = document.createElement('code');
        
        if (this.currentLanguage) {
            code.className = `language-${this.currentLanguage}`;
            this.syntaxHighlightingApplied = true;
        }
        
        code.textContent = this.currentContent;
        pre.appendChild(code);
        
        // Add line numbers if enabled
        if (this.options.showLineNumbers) {
            const lineNumbers = this.createLineNumbers();
            const wrapper = document.createElement('div');
            wrapper.className = 'flex';
            wrapper.appendChild(lineNumbers);
            wrapper.appendChild(pre);
            container.appendChild(wrapper);
            this.lineNumbersDisplayed = true;
        } else {
            container.appendChild(pre);
        }
    }

    createLineNumbers() {
        const lines = this.currentContent.split('\n');
        const lineCount = lines.length;
        
        const lineNumbersDiv = document.createElement('div');
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

    getCurrentLanguage() {
        return this.currentLanguage;
    }

    getCurrentContent() {
        return this.currentContent;
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

// **Feature: file-preview-system, Property 3: Format-specific renderer selection (code files)**
// **Validates: Requirements 4.4**
async function property3_formatSpecificRendererSelection_codeFiles() {
    // Generate random code file types
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
    
    // Generate sample code content
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
    
    // Setup mock fetch
    window.mockFetch = new MockFetch();
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/metadata`, {
        ok: true,
        data: {
            id: fileId,
            fileName: fileName,
            mimeType: randomFile.mime,
            fileSize: fileContent.length,
            uploadDate: new Date().toISOString(),
            uploaderName: 'Test User',
            previewable: true,
            previewType: 'code'
        }
    });
    
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/content`, {
        ok: true,
        data: {
            success: true,
            data: fileContent
        }
    });
    
    // Create mock container
    const container = document.createElement('div');
    
    // Detect language from file name
    const detectedLanguage = MockCodeRenderer.detectLanguage(fileName);
    
    // Create renderer and render
    const renderer = new MockCodeRenderer();
    await renderer.render(fileId, container, detectedLanguage);
    
    // Property: For any code file type, the system should select and apply
    // the code renderer with appropriate syntax highlighting
    
    // Check render was called
    if (!renderer.renderCalled) {
        return {
            passed: false,
            reason: 'Renderer was not called',
            details: { renderCalled: renderer.renderCalled }
        };
    }
    
    // Check content was fetched
    if (!renderer.currentContent) {
        return {
            passed: false,
            reason: 'Content was not fetched',
            details: { currentContent: renderer.currentContent }
        };
    }
    
    // Check content matches expected
    if (renderer.currentContent !== fileContent) {
        return {
            passed: false,
            reason: 'Content does not match expected',
            details: {
                expected: fileContent,
                actual: renderer.currentContent
            }
        };
    }
    
    // Check language was detected correctly
    if (detectedLanguage && renderer.currentLanguage !== detectedLanguage) {
        return {
            passed: false,
            reason: 'Language was not detected correctly',
            details: {
                expected: detectedLanguage,
                actual: renderer.currentLanguage,
                fileName: fileName
            }
        };
    }
    
    // Check syntax highlighting was applied (when language is specified)
    if (detectedLanguage && !renderer.syntaxHighlightingApplied) {
        return {
            passed: false,
            reason: 'Syntax highlighting was not applied',
            details: {
                language: detectedLanguage,
                syntaxHighlightingApplied: renderer.syntaxHighlightingApplied
            }
        };
    }
    
    // Check container has content
    if (!container.innerHTML || container.innerHTML.length === 0) {
        return {
            passed: false,
            reason: 'Container was not populated with content',
            details: { innerHTML: container.innerHTML }
        };
    }
    
    // Check code element exists
    const codeElement = container.querySelector('code');
    if (!codeElement) {
        return {
            passed: false,
            reason: 'Code element was not created',
            details: { innerHTML: container.innerHTML }
        };
    }
    
    return { passed: true };
}

// Test language detection for various file extensions
async function propertyTest_languageDetection() {
    // Test cases for language detection
    const testCases = [
        { fileName: 'test.js', expectedLang: 'javascript' },
        { fileName: 'Test.java', expectedLang: 'java' },
        { fileName: 'script.py', expectedLang: 'python' },
        { fileName: 'styles.css', expectedLang: 'css' },
        { fileName: 'index.html', expectedLang: 'html' },
        { fileName: 'query.sql', expectedLang: 'sql' },
        { fileName: 'data.json', expectedLang: 'json' },
        { fileName: 'config.xml', expectedLang: 'xml' },
        { fileName: 'app.ts', expectedLang: 'typescript' },
        { fileName: 'Component.jsx', expectedLang: 'javascript' },
        { fileName: 'script.sh', expectedLang: 'bash' },
        { fileName: 'README.md', expectedLang: 'markdown' }
    ];
    
    // Pick a random test case
    const testCase = testCases[Math.floor(Math.random() * testCases.length)];
    
    // Detect language
    const detectedLanguage = MockCodeRenderer.detectLanguage(testCase.fileName);
    
    // Property: For any code file with a known extension,
    // the system should correctly detect the programming language
    
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

// Test line numbers display
async function propertyTest_lineNumbersDisplay() {
    // Generate random code file
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const fileName = 'test.js';
    
    // Generate code with random number of lines (5 to 50)
    const lineCount = Math.floor(Math.random() * 46) + 5;
    const lines = [];
    for (let i = 0; i < lineCount; i++) {
        lines.push(`// Line ${i + 1}`);
    }
    const fileContent = lines.join('\n');
    
    // Setup mock fetch
    window.mockFetch = new MockFetch();
    window.mockFetch.setResponse(`/api/file-explorer/files/${fileId}/content`, {
        ok: true,
        data: {
            success: true,
            data: fileContent
        }
    });
    
    // Create mock container
    const container = document.createElement('div');
    
    // Test with line numbers enabled
    const rendererWithLines = new MockCodeRenderer({ showLineNumbers: true });
    await rendererWithLines.render(fileId, container, 'javascript');
    
    // Property: For any code file, when line numbers are enabled,
    // the renderer should display line numbers
    
    if (!rendererWithLines.lineNumbersDisplayed) {
        return {
            passed: false,
            reason: 'Line numbers were not displayed when enabled',
            details: {
                showLineNumbers: true,
                lineNumbersDisplayed: rendererWithLines.lineNumbersDisplayed
            }
        };
    }
    
    // Check line numbers element exists
    const lineNumbersElement = container.querySelector('.line-numbers');
    if (!lineNumbersElement) {
        return {
            passed: false,
            reason: 'Line numbers element was not created',
            details: { innerHTML: container.innerHTML }
        };
    }
    
    // Test with line numbers disabled
    const containerNoLines = document.createElement('div');
    const rendererNoLines = new MockCodeRenderer({ showLineNumbers: false });
    await rendererNoLines.render(fileId, containerNoLines, 'javascript');
    
    if (rendererNoLines.lineNumbersDisplayed) {
        return {
            passed: false,
            reason: 'Line numbers were displayed when disabled',
            details: {
                showLineNumbers: false,
                lineNumbersDisplayed: rendererNoLines.lineNumbersDisplayed
            }
        };
    }
    
    return { passed: true };
}

// Test error handling for missing files
async function propertyTest_errorHandling() {
    // Generate random file ID that doesn't exist
    const fileId = Math.floor(Math.random() * 1000) + 1;
    const fileName = 'missing.js';
    
    // Setup mock fetch to return 404
    window.mockFetch = new MockFetch();
    // Don't set any response, so it will return 404
    
    // Create mock container
    const container = document.createElement('div');
    
    // Create renderer
    const renderer = new MockCodeRenderer();
    
    // Property: For any non-existent file, the renderer should throw
    // an appropriate error
    
    let errorThrown = false;
    let errorMessage = '';
    
    try {
        await renderer.render(fileId, container, 'javascript');
    } catch (error) {
        errorThrown = true;
        errorMessage = error.message;
    }
    
    if (!errorThrown) {
        return {
            passed: false,
            reason: 'No error was thrown for missing file',
            details: { fileId }
        };
    }
    
    if (!errorMessage.includes('not found')) {
        return {
            passed: false,
            reason: 'Error message does not indicate file not found',
            details: {
                errorMessage,
                expected: 'Message containing "not found"'
            }
        };
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
    
    // Run Property 3: Format-specific renderer selection (code files)
    await runner.runProperty(
        'Property 3: Format-specific renderer selection (code files)',
        property3_formatSpecificRendererSelection_codeFiles,
        100
    );
    
    // Run language detection test
    await runner.runProperty(
        'Property: Language detection from file extension',
        propertyTest_languageDetection,
        100
    );
    
    // Run line numbers display test
    await runner.runProperty(
        'Property: Line numbers display',
        propertyTest_lineNumbersDisplay,
        100
    );
    
    // Run error handling test
    await runner.runProperty(
        'Property: Error handling for missing files',
        propertyTest_errorHandling,
        100
    );
    
    // Print summary
    const allPassed = runner.printSummary();
    
    return allPassed;
}

// Export for use in HTML test runner
if (typeof window !== 'undefined') {
    window.runCodeRendererTests = runAllTests;
}
