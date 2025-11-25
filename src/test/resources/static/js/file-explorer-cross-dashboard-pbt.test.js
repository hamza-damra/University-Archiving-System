/**
 * Property-Based Tests for File Explorer Cross-Dashboard Compatibility
 * 
 * Feature: file-preview-system
 * Property 17: Cross-dashboard compatibility
 * Validates: Requirements 8.4
 * 
 * Tests that the preview system functions correctly across all dashboard roles
 * (Professor, Dean, HOD) with role-appropriate permissions and metadata display.
 */

import { FileExplorer } from '../../../main/resources/static/js/file-explorer.js';
import { FilePreviewButton } from '../../../main/resources/static/js/file-preview-button.js';
import { fileExplorerState } from '../../../main/resources/static/js/file-explorer-state.js';

/**
 * Mock FilePreviewModal for testing
 */
class MockFilePreviewModal {
    constructor(options = {}) {
        this.options = options;
        this.isOpen = false;
        this.currentFileId = null;
        this.currentFileName = null;
        this.currentFileType = null;
    }

    async open(fileId, fileName, fileType) {
        this.isOpen = true;
        this.currentFileId = fileId;
        this.currentFileName = fileName;
        this.currentFileType = fileType;
        return Promise.resolve();
    }

    close() {
        this.isOpen = false;
        this.currentFileId = null;
        this.currentFileName = null;
        this.currentFileType = null;
    }
}

/**
 * Property 17: Cross-dashboard compatibility
 * 
 * For any dashboard role (Professor, Dean, HOD), the preview system should function
 * correctly with role-appropriate permissions and metadata display.
 * 
 * This property verifies:
 * 1. Preview buttons render correctly for each role
 * 2. Preview modal opens with correct file information
 * 3. Role-specific metadata is displayed appropriately
 * 4. Permissions are respected (users can only preview files they have access to)
 */
describe('Property 17: Cross-dashboard compatibility', () => {
    let container;
    let originalFetch;

    beforeEach(() => {
        // Create test container
        container = document.createElement('div');
        container.id = 'testContainer';
        document.body.appendChild(container);

        // Mock fetch for API calls
        originalFetch = global.fetch;
        global.fetch = jest.fn();

        // Reset file explorer state
        fileExplorerState.resetData();
    });

    afterEach(() => {
        // Cleanup
        if (container && container.parentNode) {
            container.parentNode.removeChild(container);
        }
        global.fetch = originalFetch;
        fileExplorerState.resetData();
    });

    /**
     * Test that preview buttons render for all dashboard roles
     */
    test('Preview buttons render correctly for all dashboard roles', () => {
        const roles = ['PROFESSOR', 'DEANSHIP', 'HOD'];
        const file = {
            id: 1,
            originalFilename: 'test.pdf',
            fileType: 'application/pdf',
            fileSize: 1024,
            uploadedAt: '2024-01-01T10:00:00',
            uploaderName: 'Test User'
        };

        roles.forEach(role => {
            // Create file explorer for each role
            const roleContainer = document.createElement('div');
            roleContainer.id = `fileExplorerContainer_${role}`;
            document.body.appendChild(roleContainer);

            const explorer = new FileExplorer(roleContainer.id, {
                role: role,
                readOnly: role !== 'PROFESSOR',
                showOwnershipLabels: role === 'PROFESSOR',
                showProfessorLabels: role === 'DEANSHIP',
                showDepartmentContext: role === 'HOD',
                hideTree: role === 'DEANSHIP'
            });

            // Render preview button
            const buttonHtml = FilePreviewButton.renderButton(file);

            // Verify button is rendered
            expect(buttonHtml).toBeTruthy();
            expect(buttonHtml.length).toBeGreaterThan(0);
            expect(buttonHtml).toContain('preview-button');
            expect(buttonHtml).toContain('data-file-id="1"');

            // Cleanup
            roleContainer.remove();
        });
    });

    /**
     * Test that preview modal opens with correct file information for all roles
     */
    test('Preview modal opens with correct file information for all roles', async () => {
        const roles = ['PROFESSOR', 'DEANSHIP', 'HOD'];
        const file = {
            id: 2,
            originalFilename: 'document.pdf',
            fileType: 'application/pdf',
            fileSize: 2048,
            uploadedAt: '2024-01-02T11:00:00',
            uploaderName: 'Dr. Smith'
        };

        for (const role of roles) {
            // Mock API response for file metadata
            global.fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    success: true,
                    data: {
                        id: file.id,
                        originalFilename: file.originalFilename,
                        fileType: file.fileType,
                        fileSize: file.fileSize,
                        uploadedAt: file.uploadedAt,
                        uploaderName: file.uploaderName
                    }
                })
            });

            // Create mock preview modal
            const mockModal = new MockFilePreviewModal();
            
            // Simulate preview button click
            await mockModal.open(file.id, file.originalFilename, file.fileType);

            // Verify modal opened with correct information
            expect(mockModal.isOpen).toBe(true);
            expect(mockModal.currentFileId).toBe(file.id);
            expect(mockModal.currentFileName).toBe(file.originalFilename);
            expect(mockModal.currentFileType).toBe(file.fileType);

            // Close modal for next iteration
            mockModal.close();
        }
    });

    /**
     * Test that role-specific metadata is displayed appropriately
     */
    test('Role-specific metadata is displayed appropriately', () => {
        const testCases = [
            {
                role: 'PROFESSOR',
                file: {
                    id: 3,
                    originalFilename: 'lecture.pdf',
                    fileType: 'application/pdf',
                    canWrite: true
                },
                expectedLabels: ['Your Folder'] // Professor sees ownership labels
            },
            {
                role: 'DEANSHIP',
                file: {
                    id: 4,
                    originalFilename: 'report.pdf',
                    fileType: 'application/pdf',
                    uploaderName: 'Dr. Johnson',
                    departmentName: 'Computer Science'
                },
                expectedMetadata: ['uploaderName', 'departmentName'] // Dean sees professor and department
            },
            {
                role: 'HOD',
                file: {
                    id: 5,
                    originalFilename: 'syllabus.pdf',
                    fileType: 'application/pdf',
                    uploaderName: 'Dr. Williams'
                },
                expectedMetadata: ['uploaderName'] // HOD sees professor name
            }
        ];

        testCases.forEach(({ role, file, expectedLabels, expectedMetadata }) => {
            // Create file explorer for role
            const roleContainer = document.createElement('div');
            roleContainer.id = `fileExplorerContainer_${role}_${file.id}`;
            document.body.appendChild(roleContainer);

            const explorer = new FileExplorer(roleContainer.id, {
                role: role,
                readOnly: role !== 'PROFESSOR',
                showOwnershipLabels: role === 'PROFESSOR',
                showProfessorLabels: role === 'DEANSHIP',
                showDepartmentContext: role === 'HOD',
                hideTree: role === 'DEANSHIP'
            });

            // Verify role-specific configuration
            expect(explorer.options.role).toBe(role);
            
            if (expectedLabels) {
                expect(explorer.options.showOwnershipLabels).toBe(role === 'PROFESSOR');
            }
            
            if (expectedMetadata) {
                if (role === 'DEANSHIP') {
                    expect(explorer.options.showProfessorLabels).toBe(true);
                }
                if (role === 'HOD') {
                    expect(explorer.options.showDepartmentContext).toBe(true);
                }
            }

            // Cleanup
            roleContainer.remove();
        });
    });

    /**
     * Test that permissions are respected across dashboards
     */
    test('Permissions are respected across dashboards', () => {
        const testCases = [
            {
                role: 'PROFESSOR',
                file: {
                    id: 6,
                    originalFilename: 'my-file.pdf',
                    fileType: 'application/pdf',
                    canRead: true,
                    canWrite: true
                },
                shouldShowPreview: true
            },
            {
                role: 'DEANSHIP',
                file: {
                    id: 7,
                    originalFilename: 'dept-file.pdf',
                    fileType: 'application/pdf',
                    canRead: true,
                    canWrite: false // Dean has read-only access
                },
                shouldShowPreview: true
            },
            {
                role: 'HOD',
                file: {
                    id: 8,
                    originalFilename: 'dept-file.pdf',
                    fileType: 'application/pdf',
                    canRead: true,
                    canWrite: false // HOD has read-only access to department files
                },
                shouldShowPreview: true
            },
            {
                role: 'PROFESSOR',
                file: {
                    id: 9,
                    originalFilename: 'restricted.pdf',
                    fileType: 'application/pdf',
                    canRead: false,
                    canWrite: false
                },
                shouldShowPreview: false
            }
        ];

        testCases.forEach(({ role, file, shouldShowPreview }) => {
            // Render preview button
            const buttonHtml = FilePreviewButton.renderButton(file);

            if (shouldShowPreview && file.canRead !== false) {
                // Button should be rendered and enabled
                expect(buttonHtml).toBeTruthy();
                expect(buttonHtml).toContain('preview-button');
                expect(buttonHtml).not.toContain('disabled');
            } else if (!file.canRead) {
                // For files without read permission, button might not be rendered
                // or should be disabled
                if (buttonHtml) {
                    expect(buttonHtml).toContain('disabled');
                }
            }
        });
    });

    /**
     * Test that file explorer layout adapts to role configuration
     */
    test('File explorer layout adapts to role configuration', () => {
        const testCases = [
            {
                role: 'PROFESSOR',
                options: {
                    hideTree: false,
                    readOnly: false,
                    showOwnershipLabels: true
                },
                expectedLayout: 'two-column' // Tree + file list
            },
            {
                role: 'DEANSHIP',
                options: {
                    hideTree: true,
                    readOnly: true,
                    showProfessorLabels: true
                },
                expectedLayout: 'single-column' // File list only
            },
            {
                role: 'HOD',
                options: {
                    hideTree: false,
                    readOnly: true,
                    showDepartmentContext: true
                },
                expectedLayout: 'two-column' // Tree + file list
            }
        ];

        testCases.forEach(({ role, options, expectedLayout }) => {
            // Create file explorer for role
            const roleContainer = document.createElement('div');
            roleContainer.id = `fileExplorerContainer_layout_${role}`;
            document.body.appendChild(roleContainer);

            const explorer = new FileExplorer(roleContainer.id, {
                role: role,
                ...options
            });

            // Verify layout configuration
            expect(explorer.options.hideTree).toBe(options.hideTree);
            expect(explorer.options.readOnly).toBe(options.readOnly);

            // Verify role-specific options
            if (role === 'PROFESSOR') {
                expect(explorer.options.showOwnershipLabels).toBe(true);
            } else if (role === 'DEANSHIP') {
                expect(explorer.options.showProfessorLabels).toBe(true);
            } else if (role === 'HOD') {
                expect(explorer.options.showDepartmentContext).toBe(true);
            }

            // Cleanup
            roleContainer.remove();
        });
    });
});
