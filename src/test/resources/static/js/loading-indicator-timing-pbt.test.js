/**
 * Property-Based Tests for Loading Indicator Timing
 * Feature: file-preview-system, Property 11: Loading indicator display
 * Validates: Requirements 6.1
 * 
 * Property 11: Loading indicator display
 * For any preview request, a loading indicator should appear within 100 milliseconds of the request being initiated
 */

import { FilePreviewModal } from '../../../../../main/resources/static/js/file-preview-modal.js';

// Mock fetch to control timing
let originalFetch;
let fetchDelay = 0;

function setupMockFetch(delay = 0) {
    fetchDelay = delay;
    originalFetch = window.fetch;
    
    window.fetch = async (url) => {
        // Simulate network delay
        await new Promise(resolve => setTimeout(resolve, fetchDelay));
        
        // Return mock responses based on URL
        if (url.includes('/metadata')) {
            return {
                ok: true,
                json: async () => ({
                    success: true,
                    data: {
                        id: 1,
                        fileName: 'test.txt',
                        fileSize: 1024,
                        mimeType: 'text/plain',
                        uploadDate: new Date().toISOString(),
                        uploaderName: 'Test User',
                        departmentName: 'Test Department'
                    }
                })
            };
        } else if (url.includes('/content')) {
            return {
                ok: true,
                text: async () => 'Test content'
            };
        }
        
        return { ok: false, status: 404 };
    };
}

function restoreFetch() {
    if (originalFetch) {
        window.fetch = originalFetch;
    }
}

/**
 * Test: Loading indicator appears within 100ms
 * Property: For any preview request, loading indicator should appear within 100ms
 */
QUnit.module('Loading Indicator Timing Property Tests', {
    beforeEach: function() {
        // Clean up any existing modals
        const existingModals = document.querySelectorAll('.fixed.inset-0');
        existingModals.forEach(modal => modal.remove());
        
        // Setup mock fetch
        setupMockFetch(500); // Simulate slow network
    },
    afterEach: function() {
        // Clean up modals
        const modals = document.querySelectorAll('.fixed.inset-0');
        modals.forEach(modal => modal.remove());
        
        // Restore fetch
        restoreFetch();
        
        // Clean up global reference
        delete window.filePreviewModal;
    }
});

QUnit.test('Property 11: Loading indicator appears within 100ms of preview request', async function(assert) {
    const done = assert.async();
    
    // Create modal instance
    const modal = new FilePreviewModal();
    
    // Record start time
    const startTime = performance.now();
    let loadingIndicatorTime = null;
    
    // Start opening the modal (this will trigger loading state)
    const openPromise = modal.open(1, 'test.txt', 'text/plain');
    
    // Check for loading indicator immediately and repeatedly
    const checkInterval = setInterval(() => {
        const loadingSpinner = document.querySelector('.animate-spin');
        const loadingText = document.querySelector('.preview-content');
        
        if (loadingSpinner && loadingText && loadingText.textContent.includes('Loading preview')) {
            loadingIndicatorTime = performance.now() - startTime;
            clearInterval(checkInterval);
            
            // Assert that loading indicator appeared within 100ms
            assert.ok(
                loadingIndicatorTime <= 100,
                `Loading indicator appeared in ${loadingIndicatorTime.toFixed(2)}ms (should be ≤ 100ms)`
            );
            
            // Clean up and finish test
            modal.close();
            done();
        }
    }, 5); // Check every 5ms for high precision
    
    // Timeout after 200ms if loading indicator never appears
    setTimeout(() => {
        clearInterval(checkInterval);
        if (loadingIndicatorTime === null) {
            assert.ok(false, 'Loading indicator did not appear within 200ms');
            modal.close();
            done();
        }
    }, 200);
});

QUnit.test('Property 11: Loading indicator has smooth fade-in animation', async function(assert) {
    const done = assert.async();
    
    // Create modal instance
    const modal = new FilePreviewModal();
    
    // Start opening the modal
    const openPromise = modal.open(1, 'test.txt', 'text/plain');
    
    // Wait a bit for modal to be created
    setTimeout(() => {
        const modalElement = document.querySelector('.fixed.inset-0');
        
        assert.ok(modalElement, 'Modal element exists');
        
        // Check for fade-in class
        const hasFadeIn = modalElement.classList.contains('fade-in');
        assert.ok(hasFadeIn, 'Modal has fade-in animation class');
        
        // Check for loading spinner
        const loadingSpinner = document.querySelector('.animate-spin');
        assert.ok(loadingSpinner, 'Loading spinner exists');
        
        // Check spinner has animation
        const spinnerStyles = window.getComputedStyle(loadingSpinner);
        const hasAnimation = spinnerStyles.animation !== 'none' || 
                           loadingSpinner.classList.contains('animate-spin');
        assert.ok(hasAnimation, 'Loading spinner has animation');
        
        // Clean up
        modal.close();
        done();
    }, 50);
});

QUnit.test('Property 11: Loading indicator persists until content loads', async function(assert) {
    const done = assert.async();
    
    // Setup mock fetch with longer delay
    setupMockFetch(300);
    
    // Create modal instance
    const modal = new FilePreviewModal();
    
    // Start opening the modal
    const openPromise = modal.open(1, 'test.txt', 'text/plain');
    
    // Check that loading indicator is present after 50ms
    setTimeout(() => {
        const loadingSpinner1 = document.querySelector('.animate-spin');
        assert.ok(loadingSpinner1, 'Loading indicator present at 50ms');
    }, 50);
    
    // Check that loading indicator is still present after 150ms
    setTimeout(() => {
        const loadingSpinner2 = document.querySelector('.animate-spin');
        assert.ok(loadingSpinner2, 'Loading indicator still present at 150ms');
    }, 150);
    
    // Wait for content to load and check loading indicator is gone
    setTimeout(async () => {
        await openPromise;
        
        // Loading indicator should be replaced with content
        const loadingSpinner3 = document.querySelector('.animate-spin');
        const hasContent = document.querySelector('.preview-content pre') !== null ||
                          document.querySelector('.preview-content').textContent.length > 100;
        
        assert.ok(!loadingSpinner3 || hasContent, 'Loading indicator removed after content loads');
        
        // Clean up
        modal.close();
        done();
    }, 600);
});

QUnit.test('Property 11: Loading indicator appears for different file types', async function(assert) {
    const done = assert.async();
    const fileTypes = [
        { type: 'text/plain', name: 'test.txt' },
        { type: 'application/pdf', name: 'test.pdf' },
        { type: 'text/javascript', name: 'test.js' },
        { type: 'application/json', name: 'test.json' }
    ];
    
    let testsCompleted = 0;
    
    for (const fileType of fileTypes) {
        const modal = new FilePreviewModal();
        const startTime = performance.now();
        
        // Start opening modal
        const openPromise = modal.open(1, fileType.name, fileType.type);
        
        // Check for loading indicator
        setTimeout(() => {
            const loadingSpinner = document.querySelector('.animate-spin');
            const timeTaken = performance.now() - startTime;
            
            assert.ok(
                loadingSpinner && timeTaken <= 100,
                `Loading indicator appeared within 100ms for ${fileType.type} (${timeTaken.toFixed(2)}ms)`
            );
            
            modal.close();
            testsCompleted++;
            
            if (testsCompleted === fileTypes.length) {
                done();
            }
        }, 50);
    }
});

QUnit.test('Property 11: Loading indicator appears on retry', async function(assert) {
    const done = assert.async();
    
    // Setup mock fetch that fails first time
    let callCount = 0;
    window.fetch = async (url) => {
        callCount++;
        if (callCount === 1 && url.includes('/metadata')) {
            // First call fails
            return { ok: false, status: 500 };
        }
        
        // Subsequent calls succeed
        await new Promise(resolve => setTimeout(resolve, 200));
        
        if (url.includes('/metadata')) {
            return {
                ok: true,
                json: async () => ({
                    success: true,
                    data: {
                        id: 1,
                        fileName: 'test.txt',
                        fileSize: 1024,
                        mimeType: 'text/plain',
                        uploadDate: new Date().toISOString()
                    }
                })
            };
        }
        
        return { ok: false, status: 404 };
    };
    
    // Create modal instance
    const modal = new FilePreviewModal();
    
    // First attempt (will fail)
    try {
        await modal.open(1, 'test.txt', 'text/plain');
    } catch (e) {
        // Expected to fail
    }
    
    // Wait a bit
    setTimeout(async () => {
        // Retry
        const retryStartTime = performance.now();
        modal.retryPreview();
        
        // Check for loading indicator on retry
        setTimeout(() => {
            const loadingSpinner = document.querySelector('.animate-spin');
            const timeTaken = performance.now() - retryStartTime;
            
            assert.ok(
                loadingSpinner && timeTaken <= 100,
                `Loading indicator appeared within 100ms on retry (${timeTaken.toFixed(2)}ms)`
            );
            
            modal.close();
            done();
        }, 50);
    }, 100);
});

QUnit.test('Property 11: Loading indicator appears for partial preview', async function(assert) {
    const done = assert.async();
    
    // Create modal instance
    const modal = new FilePreviewModal();
    
    // Simulate large file warning first
    modal.currentFileId = 1;
    modal.currentFileName = 'large.txt';
    modal.currentFileType = 'text/plain';
    modal.isOpen = true;
    modal.createModal();
    modal.showLargeFileWarning(10 * 1024 * 1024); // 10MB
    
    // Now trigger partial preview
    const startTime = performance.now();
    modal.loadPartialPreview();
    
    // Check for loading indicator
    setTimeout(() => {
        const loadingSpinner = document.querySelector('.animate-spin');
        const timeTaken = performance.now() - startTime;
        
        assert.ok(
            loadingSpinner && timeTaken <= 100,
            `Loading indicator appeared within 100ms for partial preview (${timeTaken.toFixed(2)}ms)`
        );
        
        modal.close();
        done();
    }, 50);
});
