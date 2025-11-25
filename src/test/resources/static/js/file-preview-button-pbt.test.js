/**
 * Property-Based Tests for File Preview Button
 * Feature: file-preview-system
 * Task: 8.1 Write property test for preview button rendering
 * 
 * **Property 10: Preview button rendering**
 * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
 * 
 * These tests validate that:
 * - Preview buttons are rendered for supported file types (5.1)
 * - Tooltips indicate preview availability for supported files (5.2)
 * - Tooltips indicate download-only for unsupported files (5.3)
 * - Previewable files are visually distinguished (5.4)
 */

import { FilePreviewButton } from '../../../../../main/resources/static/js/file-preview-button.js';

/**
 * Test suite for FilePreviewButton
 */
describe('FilePreviewButton Property-Based Tests', () => {
    
    /**
     * Property 10: Preview button rendering
     * For any file in the file explorer, a preview button should be rendered 
     * if the file type is supported, and the button should have appropriate 
     * visual indicators and tooltips
     */
    describe('Property 10: Preview button rendering', () => {
        
        /**
         * Test: Supported file types should render active preview button
         * Validates Requirements 5.1, 5.2, 5.4
         */
        it('should render active preview button for supported file types', () => {
            fc.assert(
                fc.property(
                    fc.oneof(
                        // Text files
                        fc.record({
                            id: fc.integer({ min: 1, max: 10000 }),
                            originalFilename: fc.constantFrom('document.txt', 'readme.md', 'data.csv', 'app.log'),
                            fileType: fc.constantFrom('text/plain', 'text/markdown', 'text/csv', 'text/log')
                        }),
                        // PDF files
                        fc.record({
                            id: fc.integer({ min: 1, max: 10000 }),
                            originalFilename: fc.constant('document.pdf'),
                            fileType: fc.constant('application/pdf')
                        }),
                        // Office documents
                        fc.record({
                            id: fc.integer({ min: 1, max: 10000 }),
                            originalFilename: fc.constantFrom('report.docx', 'spreadsheet.xlsx', 'presentation.pptx'),
                            fileType: fc.constantFrom(
                                'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                                'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
                                'application/vnd.openxmlformats-officedocument.presentationml.presentation'
                            )
                        }),
                        // Code files
                        fc.record({
                            id: fc.integer({ min: 1, max: 10000 }),
                            originalFilename: fc.constantFrom('script.js', 'Main.java', 'app.py', 'style.css', 'index.html'),
                            fileType: fc.constantFrom('text/javascript', 'text/x-java-source', 'text/x-python', 'text/css', 'text/html')
                        })
                    ),
                    (file) => {
                        // Render button
                        const buttonHtml = FilePreviewButton.renderButton(file);
                        
                        // Verify button is rendered
                        expect(buttonHtml).toBeTruthy();
                        expect(buttonHtml.length).toBeGreaterThan(0);
                        
                        // Verify it's an active button (not disabled)
                        expect(buttonHtml).not.toContain('disabled');
                        expect(buttonHtml).not.toContain('cursor-not-allowed');
                        
                        // Verify it has the preview-button class (Requirement 5.4: Visual distinction)
                        expect(buttonHtml).toContain('preview-button');
                        
                        // Verify it has blue color for visual distinction (Requirement 5.4)
                        expect(buttonHtml).toContain('text-blue-600');
                        
                        // Verify it has the correct tooltip (Requirement 5.2)
                        expect(buttonHtml).toContain('Click to preview');
                        
                        // Verify it has onclick handler (Requirement 5.1)
                        expect(buttonHtml).toContain('onclick');
                        expect(buttonHtml).toContain('handlePreviewClick');
                        
                        // Verify it has data attributes for file info
                        expect(buttonHtml).toContain(`data-file-id="${file.id}"`);
                        expect(buttonHtml).toContain('data-file-name');
                        expect(buttonHtml).toContain('data-file-type');
                    }
                ),
                { numRuns: 100 }
            );
        });
        
        /**
         * Test: Unsupported file types should render disabled button
         * Validates Requirements 5.3
         */
        it('should render disabled button for unsupported file types', () => {
            fc.assert(
                fc.property(
                    fc.record({
                        id: fc.integer({ min: 1, max: 10000 }),
                        originalFilename: fc.constantFrom('image.png', 'video.mp4', 'archive.zip', 'binary.exe'),
                        fileType: fc.constantFrom('image/png', 'video/mp4', 'application/zip', 'application/octet-stream')
                    }),
                    (file) => {
                        // Render button
                        const buttonHtml = FilePreviewButton.renderButton(file);
                        
                        // Verify button is rendered
                        expect(buttonHtml).toBeTruthy();
                        expect(buttonHtml.length).toBeGreaterThan(0);
                        
                        // Verify it's disabled (Requirement 5.3)
                        expect(buttonHtml).toContain('disabled');
                        expect(buttonHtml).toContain('cursor-not-allowed');
                        
                        // Verify it has gray color (not blue) to show it's disabled
                        expect(buttonHtml).toContain('text-gray-400');
                        expect(buttonHtml).not.toContain('text-blue-600');
                        
                        // Verify it has the correct tooltip (Requirement 5.3)
                        expect(buttonHtml).toContain('Download only');
                        expect(buttonHtml).toContain('Preview not available');
                        
                        // Verify it does NOT have onclick handler
                        expect(buttonHtml).not.toContain('onclick');
                        expect(buttonHtml).not.toContain('handlePreviewClick');
                    }
                ),
                { numRuns: 100 }
            );
        });
        
        /**
         * Test: isPreviewable correctly identifies supported types
         * Validates Requirements 5.1
         */
        it('should correctly identify previewable file types', () => {
            fc.assert(
                fc.property(
                    fc.oneof(
                        // Supported MIME types
                        fc.constantFrom(
                            'text/plain',
                            'text/markdown',
                            'text/csv',
                            'application/pdf',
                            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                            'text/javascript',
                            'text/x-java-source',
                            'text/x-python',
                            'text/css',
                            'text/html',
                            'application/json'
                        )
                    ),
                    (mimeType) => {
                        // Check if type is previewable
                        const isPreviewable = FilePreviewButton.isPreviewable(mimeType);
                        
                        // Should be true for all supported types
                        expect(isPreviewable).toBe(true);
                    }
                ),
                { numRuns: 100 }
            );
        });
        
        /**
         * Test: isPreviewable correctly identifies unsupported types
         * Validates Requirements 5.3
         */
        it('should correctly identify non-previewable file types', () => {
            fc.assert(
                fc.property(
                    fc.constantFrom(
                        'image/png',
                        'image/jpeg',
                        'video/mp4',
                        'audio/mp3',
                        'application/zip',
                        'application/octet-stream',
                        'application/x-executable'
                    ),
                    (mimeType) => {
                        // Check if type is previewable
                        const isPreviewable = FilePreviewButton.isPreviewable(mimeType);
                        
                        // Should be false for all unsupported types
                        expect(isPreviewable).toBe(false);
                    }
                ),
                { numRuns: 100 }
            );
        });
        
        /**
         * Test: Extension fallback works when MIME type is missing
         * Validates Requirements 5.1
         */
        it('should use file extension as fallback when MIME type is missing', () => {
            fc.assert(
                fc.property(
                    fc.record({
                        fileName: fc.constantFrom('document.pdf', 'script.js', 'readme.md', 'data.csv', 'Main.java'),
                        mimeType: fc.constant('')
                    }),
                    ({ fileName, mimeType }) => {
                        // Check if type is previewable using extension fallback
                        const isPreviewable = FilePreviewButton.isPreviewable(mimeType, fileName);
                        
                        // Should be true because extension is supported
                        expect(isPreviewable).toBe(true);
                    }
                ),
                { numRuns: 100 }
            );
        });
        
        /**
         * Test: Button HTML is properly escaped to prevent XSS
         * Security property
         */
        it('should properly escape file names to prevent XSS', () => {
            fc.assert(
                fc.property(
                    fc.record({
                        id: fc.integer({ min: 1, max: 10000 }),
                        originalFilename: fc.constantFrom(
                            '<script>alert("xss")</script>.pdf',
                            'file"with"quotes.txt',
                            "file'with'quotes.md",
                            'file&with&ampersands.js'
                        ),
                        fileType: fc.constant('application/pdf')
                    }),
                    (file) => {
                        // Render button
                        const buttonHtml = FilePreviewButton.renderButton(file);
                        
                        // Verify dangerous characters are escaped
                        expect(buttonHtml).not.toContain('<script>');
                        expect(buttonHtml).not.toContain('alert(');
                        
                        // Verify HTML entities are used
                        if (file.originalFilename.includes('<')) {
                            expect(buttonHtml).toContain('&lt;');
                        }
                        if (file.originalFilename.includes('>')) {
                            expect(buttonHtml).toContain('&gt;');
                        }
                        if (file.originalFilename.includes('&')) {
                            expect(buttonHtml).toContain('&amp;');
                        }
                    }
                ),
                { numRuns: 100 }
            );
        });
        
        /**
         * Test: Visual distinction between previewable and non-previewable files
         * Validates Requirement 5.4
         */
        it('should visually distinguish previewable from non-previewable files', () => {
            fc.assert(
                fc.property(
                    fc.record({
                        id: fc.integer({ min: 1, max: 10000 }),
                        previewableFile: fc.record({
                            originalFilename: fc.constant('document.pdf'),
                            fileType: fc.constant('application/pdf')
                        }),
                        nonPreviewableFile: fc.record({
                            originalFilename: fc.constant('image.png'),
                            fileType: fc.constant('image/png')
                        })
                    }),
                    ({ id, previewableFile, nonPreviewableFile }) => {
                        // Render both buttons
                        const previewableHtml = FilePreviewButton.renderButton({
                            id,
                            ...previewableFile
                        });
                        const nonPreviewableHtml = FilePreviewButton.renderButton({
                            id,
                            ...nonPreviewableFile
                        });
                        
                        // Previewable should have blue color
                        expect(previewableHtml).toContain('text-blue-600');
                        
                        // Non-previewable should have gray color
                        expect(nonPreviewableHtml).toContain('text-gray-400');
                        
                        // They should have different visual states
                        expect(previewableHtml).not.toEqual(nonPreviewableHtml);
                        
                        // Previewable should not be disabled
                        expect(previewableHtml).not.toContain('disabled');
                        
                        // Non-previewable should be disabled
                        expect(nonPreviewableHtml).toContain('disabled');
                    }
                ),
                { numRuns: 100 }
            );
        });
    });
});
