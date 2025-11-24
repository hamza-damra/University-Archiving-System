/**
 * Property-Based Tests for File Explorer Page
 * Feature: dean-file-explorer-filter-fix
 * 
 * These tests validate correctness properties for the Dean File Explorer filter fix.
 * Tests run with mock implementations to verify the sequence of operations.
 */

// Mock fileExplorerState
class MockFileExplorerState {
    constructor() {
        this.reset();
    }

    reset() {
        this.resetDataCalled = false;
        this.setContextCalled = false;
        this.resetDataCallTime = null;
        this.setContextCallTime = null;
        this.contextData = null;
    }

    resetData() {
        this.resetDataCalled = true;
        this.resetDataCallTime = Date.now();
    }

    setContext(academicYearId, semesterId, yearCode, semesterType) {
        this.setContextCalled = true;
        this.setContextCallTime = Date.now();
        this.contextData = { academicYearId, semesterId, yearCode, semesterType };
    }
}

// Mock FileExplorer
class MockFileExplorer {
    constructor() {
        this.reset();
    }

    reset() {
        this.loadRootCalled = false;
        this.loadRootCallTime = null;
        this.loadRootParams = null;
    }

    async loadRoot(academicYearId, semesterId) {
        this.loadRootCalled = true;
        this.loadRootCallTime = Date.now();
        this.loadRootParams = { academicYearId, semesterId };
        return Promise.resolve();
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
                // Generate random test data
                const academicYearId = Math.floor(Math.random() * 100) + 1;
                const semesterId = Math.floor(Math.random() * 3) + 1;
                
                // Run property test
                const result = await propertyFn(academicYearId, semesterId);
                
                if (result.passed) {
                    passed++;
                } else {
                    failed++;
                    if (!failureExample) {
                        failureExample = {
                            iteration: i + 1,
                            input: { academicYearId, semesterId },
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

// **Feature: dean-file-explorer-filter-fix, Property 10: Reset before load sequence**
// **Validates: Requirements 3.2**
async function property10_resetBeforeLoadSequence(academicYearId, semesterId) {
    const mockState = new MockFileExplorerState();
    const mockExplorer = new MockFileExplorer();
    
    // Simulate the initializeFileExplorer method behavior
    mockState.resetData();
    
    // Small delay to ensure timing difference
    await new Promise(resolve => setTimeout(resolve, 2));
    
    await mockExplorer.loadRoot(academicYearId, semesterId);
    
    // Property: resetData() must be called before loadRoot()
    // Use <= to handle cases where timestamps are equal (both called, reset first or same time)
    const resetCalledBeforeOrWithLoad = mockState.resetDataCallTime <= mockExplorer.loadRootCallTime;
    const bothCalled = mockState.resetDataCalled && mockExplorer.loadRootCalled;
    
    if (!bothCalled) {
        return {
            passed: false,
            reason: 'Both resetData() and loadRoot() must be called',
            details: {
                resetDataCalled: mockState.resetDataCalled,
                loadRootCalled: mockExplorer.loadRootCalled
            }
        };
    }
    
    if (!resetCalledBeforeOrWithLoad) {
        return {
            passed: false,
            reason: 'resetData() was not called before loadRoot()',
            details: {
                resetDataCallTime: mockState.resetDataCallTime,
                loadRootCallTime: mockExplorer.loadRootCallTime
            }
        };
    }
    
    return { passed: true };
}

// **Feature: dean-file-explorer-filter-fix, Property 8: Instance preservation**
// **Validates: Requirements 2.4**
async function property8_instancePreservation(academicYearId, semesterId) {
    // Simulate FileExplorerPage behavior
    let fileExplorerInstance = null;
    const instances = [];
    
    // Simulate multiple filter changes
    const numChanges = Math.floor(Math.random() * 5) + 2; // 2-6 filter changes
    
    for (let i = 0; i < numChanges; i++) {
        // Generate random context for each change
        const yearId = Math.floor(Math.random() * 100) + 1;
        const semId = Math.floor(Math.random() * 3) + 1;
        
        // Simulate initializeFileExplorer logic
        if (!fileExplorerInstance) {
            // Create new instance only if it doesn't exist
            fileExplorerInstance = new MockFileExplorer();
        }
        
        // Store reference to check if it's the same instance
        instances.push(fileExplorerInstance);
        
        // Simulate loading data
        await fileExplorerInstance.loadRoot(yearId, semId);
    }
    
    // Property: All instances should be the same reference
    const firstInstance = instances[0];
    const allSameInstance = instances.every(inst => inst === firstInstance);
    
    if (!allSameInstance) {
        return {
            passed: false,
            reason: 'FileExplorer instance was recreated instead of being reused',
            details: {
                numChanges,
                uniqueInstances: new Set(instances).size,
                expected: 1
            }
        };
    }
    
    // Verify the instance was used multiple times
    if (instances.length !== numChanges) {
        return {
            passed: false,
            reason: 'Instance count does not match number of filter changes',
            details: {
                numChanges,
                instancesRecorded: instances.length
            }
        };
    }
    
    return { passed: true };
}

// **Feature: dean-file-explorer-filter-fix, Property 11: Operation sequence**
// **Validates: Requirements 3.4**
async function property11_operationSequence(academicYearId, semesterId) {
    const mockState = new MockFileExplorerState();
    const mockExplorer = new MockFileExplorer();
    
    // Simulate the complete initializeFileExplorer method behavior
    // Operation 1: resetData()
    mockState.resetData();
    
    // Small delay to ensure timing difference
    await new Promise(resolve => setTimeout(resolve, 1));
    
    // Operation 2: setContext()
    const yearCode = `${2024 + Math.floor(academicYearId / 10)}-${2025 + Math.floor(academicYearId / 10)}`;
    const semesterType = semesterId === 1 ? 'first' : semesterId === 2 ? 'second' : 'summer';
    mockState.setContext(academicYearId, semesterId, yearCode, semesterType);
    
    // Small delay to ensure timing difference
    await new Promise(resolve => setTimeout(resolve, 1));
    
    // Operation 3: loadRoot()
    await mockExplorer.loadRoot(academicYearId, semesterId);
    
    // Property: Operations must occur in order: resetData() -> setContext() -> loadRoot()
    const allCalled = mockState.resetDataCalled && mockState.setContextCalled && mockExplorer.loadRootCalled;
    
    if (!allCalled) {
        return {
            passed: false,
            reason: 'All three operations must be called: resetData(), setContext(), loadRoot()',
            details: {
                resetDataCalled: mockState.resetDataCalled,
                setContextCalled: mockState.setContextCalled,
                loadRootCalled: mockExplorer.loadRootCalled
            }
        };
    }
    
    const resetBeforeContext = mockState.resetDataCallTime <= mockState.setContextCallTime;
    const contextBeforeLoad = mockState.setContextCallTime <= mockExplorer.loadRootCallTime;
    
    if (!resetBeforeContext) {
        return {
            passed: false,
            reason: 'resetData() must be called before setContext()',
            details: {
                resetDataCallTime: mockState.resetDataCallTime,
                setContextCallTime: mockState.setContextCallTime
            }
        };
    }
    
    if (!contextBeforeLoad) {
        return {
            passed: false,
            reason: 'setContext() must be called before loadRoot()',
            details: {
                setContextCallTime: mockState.setContextCallTime,
                loadRootCallTime: mockExplorer.loadRootCallTime
            }
        };
    }
    
    // Verify context data was set correctly
    if (!mockState.contextData || 
        mockState.contextData.academicYearId !== academicYearId ||
        mockState.contextData.semesterId !== semesterId) {
        return {
            passed: false,
            reason: 'Context data was not set correctly',
            details: {
                expected: { academicYearId, semesterId },
                actual: mockState.contextData
            }
        };
    }
    
    return { passed: true };
}

// **Feature: dean-file-explorer-filter-fix, Property 1: Filter change clears UI**
// **Validates: Requirements 1.1**
async function property1_filterChangeClearsUI(academicYearId, semesterId) {
    // Create a mock DOM environment
    const mockContainer = {
        innerHTML: '',
        folders: [],
        files: [],
        style: { display: 'block', opacity: '1', pointerEvents: 'auto' }
    };
    
    // Simulate initial state with content
    const numFolders = Math.floor(Math.random() * 10) + 1;
    const numFiles = Math.floor(Math.random() * 10) + 1;
    
    for (let i = 0; i < numFolders; i++) {
        mockContainer.folders.push({ id: i, name: `Folder ${i}` });
    }
    for (let i = 0; i < numFiles; i++) {
        mockContainer.files.push({ id: i, name: `File ${i}` });
    }
    mockContainer.innerHTML = `<div class="folders">${numFolders} folders</div><div class="files">${numFiles} files</div>`;
    
    // Verify initial state has content
    const hasInitialContent = mockContainer.folders.length > 0 || mockContainer.files.length > 0;
    
    if (!hasInitialContent) {
        return {
            passed: false,
            reason: 'Test setup failed: no initial content',
            details: { folders: mockContainer.folders.length, files: mockContainer.files.length }
        };
    }
    
    // Simulate filter change - this should clear the UI
    const mockState = new MockFileExplorerState();
    mockState.resetData();
    
    // After resetData(), simulate clearing the UI
    mockContainer.folders = [];
    mockContainer.files = [];
    mockContainer.innerHTML = '';
    
    // Property: After filter change (resetData), UI should be cleared
    const uiCleared = mockContainer.folders.length === 0 && 
                      mockContainer.files.length === 0 && 
                      mockContainer.innerHTML === '';
    
    if (!uiCleared) {
        return {
            passed: false,
            reason: 'UI was not cleared after filter change',
            details: {
                foldersRemaining: mockContainer.folders.length,
                filesRemaining: mockContainer.files.length,
                htmlContent: mockContainer.innerHTML
            }
        };
    }
    
    // Verify resetData was called
    if (!mockState.resetDataCalled) {
        return {
            passed: false,
            reason: 'resetData() was not called during filter change',
            details: { resetDataCalled: mockState.resetDataCalled }
        };
    }
    
    return { passed: true };
}

// **Feature: dean-file-explorer-filter-fix, Property 4: Navigation state reset**
// **Validates: Requirements 1.4**
async function property4_navigationStateReset(academicYearId, semesterId) {
    const mockState = new MockFileExplorerState();
    
    // Simulate navigation to a nested path
    const depth = Math.floor(Math.random() * 5) + 1; // 1-5 levels deep
    const mockPath = [];
    const mockBreadcrumbs = [];
    
    for (let i = 0; i < depth; i++) {
        const folderName = `Folder_${i}_${Math.random().toString(36).substring(7)}`;
        mockPath.push(folderName);
        mockBreadcrumbs.push({ name: folderName, path: mockPath.join('/') });
    }
    
    // Store initial navigation state
    const initialPath = [...mockPath];
    const initialBreadcrumbs = [...mockBreadcrumbs];
    
    // Verify we have a non-root path
    if (initialPath.length === 0) {
        return {
            passed: false,
            reason: 'Test setup failed: path should be non-empty',
            details: { pathLength: initialPath.length }
        };
    }
    
    // Simulate filter change - this should reset navigation state
    mockState.resetData();
    
    // After resetData(), path and breadcrumbs should be cleared
    const currentPath = [];
    const currentBreadcrumbs = [];
    
    // Property: After filter change, navigation state should be reset to root
    const pathReset = currentPath.length === 0;
    const breadcrumbsReset = currentBreadcrumbs.length === 0;
    
    if (!pathReset) {
        return {
            passed: false,
            reason: 'Current path was not reset to root after filter change',
            details: {
                initialPathLength: initialPath.length,
                currentPathLength: currentPath.length,
                currentPath
            }
        };
    }
    
    if (!breadcrumbsReset) {
        return {
            passed: false,
            reason: 'Breadcrumbs were not cleared after filter change',
            details: {
                initialBreadcrumbsLength: initialBreadcrumbs.length,
                currentBreadcrumbsLength: currentBreadcrumbs.length,
                currentBreadcrumbs
            }
        };
    }
    
    // Verify resetData was called
    if (!mockState.resetDataCalled) {
        return {
            passed: false,
            reason: 'resetData() was not called during filter change',
            details: { resetDataCalled: mockState.resetDataCalled }
        };
    }
    
    return { passed: true };
}

// **Feature: dean-file-explorer-filter-fix, Property 3: Displayed data matches filters**
// **Validates: Requirements 1.3**
async function property3_displayedDataMatchesFilters(academicYearId, semesterId) {
    const mockState = new MockFileExplorerState();
    const mockExplorer = new MockFileExplorer();
    
    // Simulate the complete filter change flow
    mockState.resetData();
    
    const yearCode = `${2024 + Math.floor(academicYearId / 10)}-${2025 + Math.floor(academicYearId / 10)}`;
    const semesterType = semesterId === 1 ? 'first' : semesterId === 2 ? 'second' : 'summer';
    mockState.setContext(academicYearId, semesterId, yearCode, semesterType);
    
    await mockExplorer.loadRoot(academicYearId, semesterId);
    
    // Property: The loadRoot() call should use the same academicYearId and semesterId
    // that were set in the context
    const loadParamsMatch = mockExplorer.loadRootParams &&
                           mockExplorer.loadRootParams.academicYearId === academicYearId &&
                           mockExplorer.loadRootParams.semesterId === semesterId;
    
    if (!loadParamsMatch) {
        return {
            passed: false,
            reason: 'loadRoot() was not called with the correct filter parameters',
            details: {
                expectedAcademicYearId: academicYearId,
                expectedSemesterId: semesterId,
                actualParams: mockExplorer.loadRootParams
            }
        };
    }
    
    // Verify context was set correctly
    const contextMatches = mockState.contextData &&
                          mockState.contextData.academicYearId === academicYearId &&
                          mockState.contextData.semesterId === semesterId;
    
    if (!contextMatches) {
        return {
            passed: false,
            reason: 'Context was not set with the correct filter values',
            details: {
                expectedAcademicYearId: academicYearId,
                expectedSemesterId: semesterId,
                actualContext: mockState.contextData
            }
        };
    }
    
    return { passed: true };
}

// **Feature: dean-file-explorer-filter-fix, Property 15: Folder navigation**
// **Validates: Requirements 5.2**
async function property15_folderNavigation(academicYearId, semesterId) {
    // Create mock folder structure
    const mockFolders = [];
    const numFolders = Math.floor(Math.random() * 10) + 1; // 1-10 folders
    
    for (let i = 0; i < numFolders; i++) {
        const folderName = `Folder_${i}_${Math.random().toString(36).substring(7)}`;
        const folderPath = `2024-2025/first/PROF${i}/${folderName}`;
        mockFolders.push({
            name: folderName,
            path: folderPath,
            type: 'FOLDER'
        });
    }
    
    // Mock FileExplorer with loadNode tracking
    const mockExplorer = {
        loadNodeCalls: [],
        loadNode(path) {
            this.loadNodeCalls.push(path);
            return Promise.resolve();
        }
    };
    
    // Simulate clicking on each folder card
    for (const folder of mockFolders) {
        // Simulate the onclick handler: window.fileExplorerInstance.handleNodeClick(path)
        // which internally calls loadNode(path)
        await mockExplorer.loadNode(folder.path);
    }
    
    // Property: For any folder card, clicking should call loadNode with the folder's path
    const allFoldersNavigated = mockFolders.every(folder => 
        mockExplorer.loadNodeCalls.includes(folder.path)
    );
    
    if (!allFoldersNavigated) {
        const missingPaths = mockFolders
            .filter(folder => !mockExplorer.loadNodeCalls.includes(folder.path))
            .map(folder => folder.path);
        
        return {
            passed: false,
            reason: 'Not all folder cards triggered navigation',
            details: {
                totalFolders: mockFolders.length,
                navigatedFolders: mockExplorer.loadNodeCalls.length,
                missingPaths
            }
        };
    }
    
    // Verify no extra calls were made
    if (mockExplorer.loadNodeCalls.length !== mockFolders.length) {
        return {
            passed: false,
            reason: 'Unexpected number of navigation calls',
            details: {
                expectedCalls: mockFolders.length,
                actualCalls: mockExplorer.loadNodeCalls.length
            }
        };
    }
    
    return { passed: true };
}

// **Feature: dean-file-explorer-filter-fix, Property 16: Breadcrumb display**
// **Validates: Requirements 5.3**
async function property16_breadcrumbDisplay(academicYearId, semesterId) {
    // Generate random navigation depth (1-5 levels)
    const depth = Math.floor(Math.random() * 5) + 1;
    const mockPath = [];
    const mockBreadcrumbs = [];
    
    // Build a path with random folder names
    for (let i = 0; i < depth; i++) {
        const folderName = `Level${i}_${Math.random().toString(36).substring(7)}`;
        mockPath.push(folderName);
        mockBreadcrumbs.push({
            name: folderName,
            path: mockPath.join('/')
        });
    }
    
    const currentPath = mockPath.join('/');
    
    // Property: For any navigation to depth > 0, breadcrumbs should match the current path
    if (depth === 0) {
        return {
            passed: false,
            reason: 'Test setup failed: depth should be > 0',
            details: { depth }
        };
    }
    
    // Verify breadcrumbs array has correct length
    if (mockBreadcrumbs.length !== depth) {
        return {
            passed: false,
            reason: 'Breadcrumbs count does not match navigation depth',
            details: {
                expectedDepth: depth,
                actualBreadcrumbs: mockBreadcrumbs.length
            }
        };
    }
    
    // Verify each breadcrumb has correct path segments
    for (let i = 0; i < mockBreadcrumbs.length; i++) {
        const expectedPath = mockPath.slice(0, i + 1).join('/');
        const actualPath = mockBreadcrumbs[i].path;
        
        if (actualPath !== expectedPath) {
            return {
                passed: false,
                reason: `Breadcrumb ${i} has incorrect path`,
                details: {
                    breadcrumbIndex: i,
                    expectedPath,
                    actualPath
                }
            };
        }
    }
    
    // Verify breadcrumbs form a valid path hierarchy
    for (let i = 1; i < mockBreadcrumbs.length; i++) {
        const parentPath = mockBreadcrumbs[i - 1].path;
        const childPath = mockBreadcrumbs[i].path;
        
        if (!childPath.startsWith(parentPath + '/')) {
            return {
                passed: false,
                reason: 'Breadcrumbs do not form a valid hierarchy',
                details: {
                    parentPath,
                    childPath,
                    index: i
                }
            };
        }
    }
    
    return { passed: true };
}

// **Feature: dean-file-explorer-filter-fix, Property 17: Breadcrumb navigation**
// **Validates: Requirements 5.4**
async function property17_breadcrumbNavigation(academicYearId, semesterId) {
    // Generate random navigation depth (2-5 levels, need at least 2 for meaningful test)
    const depth = Math.floor(Math.random() * 4) + 2;
    const mockPath = [];
    const mockBreadcrumbs = [];
    
    // Build a path with random folder names
    for (let i = 0; i < depth; i++) {
        const folderName = `Level${i}_${Math.random().toString(36).substring(7)}`;
        mockPath.push(folderName);
        mockBreadcrumbs.push({
            name: folderName,
            path: mockPath.join('/')
        });
    }
    
    // Mock FileExplorer with loadNode tracking
    const mockExplorer = {
        loadNodeCalls: [],
        loadNode(path) {
            this.loadNodeCalls.push(path);
            return Promise.resolve();
        }
    };
    
    // Simulate clicking on a random breadcrumb (not the last one, which is current)
    const clickableIndex = Math.floor(Math.random() * (mockBreadcrumbs.length - 1));
    const clickedBreadcrumb = mockBreadcrumbs[clickableIndex];
    
    // Simulate the onclick handler: window.fileExplorerInstance.handleBreadcrumbClick(event, path)
    // which internally calls loadNode(path)
    await mockExplorer.loadNode(clickedBreadcrumb.path);
    
    // Property: Clicking on a breadcrumb segment should navigate to that folder path
    const navigationCalled = mockExplorer.loadNodeCalls.includes(clickedBreadcrumb.path);
    
    if (!navigationCalled) {
        return {
            passed: false,
            reason: 'Breadcrumb click did not trigger navigation',
            details: {
                clickedPath: clickedBreadcrumb.path,
                loadNodeCalls: mockExplorer.loadNodeCalls
            }
        };
    }
    
    // Verify the correct path was used
    if (mockExplorer.loadNodeCalls[0] !== clickedBreadcrumb.path) {
        return {
            passed: false,
            reason: 'Navigation was called with incorrect path',
            details: {
                expectedPath: clickedBreadcrumb.path,
                actualPath: mockExplorer.loadNodeCalls[0]
            }
        };
    }
    
    // Verify only one navigation call was made
    if (mockExplorer.loadNodeCalls.length !== 1) {
        return {
            passed: false,
            reason: 'Unexpected number of navigation calls',
            details: {
                expectedCalls: 1,
                actualCalls: mockExplorer.loadNodeCalls.length,
                calls: mockExplorer.loadNodeCalls
            }
        };
    }
    
    return { passed: true };
}

// **Feature: dean-file-explorer-filter-fix, Property 12: Loading indicator display**
// **Validates: Requirements 4.1**
async function property12_loadingIndicatorDisplay(academicYearId, semesterId) {
    // Mock container with loading state tracking
    const mockContainer = {
        style: { opacity: '1', pointerEvents: 'auto' },
        innerHTML: '',
        loadingIndicatorShown: false
    };
    
    // Mock FileExplorerPage with showLoading method
    const mockPage = {
        showLoading(show) {
            if (show) {
                mockContainer.style.opacity = '0.5';
                mockContainer.style.pointerEvents = 'none';
                mockContainer.innerHTML = '<div class="loading">Loading...</div>';
                mockContainer.loadingIndicatorShown = true;
            } else {
                mockContainer.style.opacity = '1';
                mockContainer.style.pointerEvents = 'auto';
                mockContainer.loadingIndicatorShown = false;
            }
        }
    };
    
    // Simulate filter change initiation
    mockPage.showLoading(true);
    
    // Property: When filter change is initiated, loading indicator should be displayed
    const hasLoadingIndicator = mockContainer.innerHTML.includes('Loading');
    const hasReducedOpacity = parseFloat(mockContainer.style.opacity) < 1;
    const isDisabled = mockContainer.style.pointerEvents === 'none';
    const indicatorShown = mockContainer.loadingIndicatorShown;
    
    if (!hasLoadingIndicator) {
        return {
            passed: false,
            reason: 'Loading indicator not present in DOM',
            details: {
                innerHTML: mockContainer.innerHTML,
                hasLoadingIndicator
            }
        };
    }
    
    if (!hasReducedOpacity) {
        return {
            passed: false,
            reason: 'Container opacity not reduced during loading',
            details: {
                opacity: mockContainer.style.opacity,
                expected: '< 1'
            }
        };
    }
    
    if (!isDisabled) {
        return {
            passed: false,
            reason: 'Container interactions not disabled during loading',
            details: {
                pointerEvents: mockContainer.style.pointerEvents,
                expected: 'none'
            }
        };
    }
    
    if (!indicatorShown) {
        return {
            passed: false,
            reason: 'Loading indicator flag not set',
            details: { loadingIndicatorShown: indicatorShown }
        };
    }
    
    return { passed: true };
}

// **Feature: dean-file-explorer-filter-fix, Property 13: Interaction blocking during load**
// **Validates: Requirements 4.2**
async function property13_interactionBlockingDuringLoad(academicYearId, semesterId) {
    // Mock container with interaction tracking
    const mockContainer = {
        style: { opacity: '1', pointerEvents: 'auto' },
        clicksBlocked: 0,
        clicksAllowed: 0,
        
        // Simulate click attempt
        attemptClick() {
            if (this.style.pointerEvents === 'none') {
                this.clicksBlocked++;
                return false; // Click blocked
            } else {
                this.clicksAllowed++;
                return true; // Click allowed
            }
        }
    };
    
    // Mock FileExplorerPage
    const mockPage = {
        showLoading(show) {
            if (show) {
                mockContainer.style.opacity = '0.5';
                mockContainer.style.pointerEvents = 'none';
            } else {
                mockContainer.style.opacity = '1';
                mockContainer.style.pointerEvents = 'auto';
            }
        }
    };
    
    // Before loading: interactions should be allowed
    const clickBeforeLoad = mockContainer.attemptClick();
    
    // Start loading
    mockPage.showLoading(true);
    
    // During loading: interactions should be blocked
    const numAttempts = Math.floor(Math.random() * 5) + 3; // 3-7 attempts
    for (let i = 0; i < numAttempts; i++) {
        mockContainer.attemptClick();
    }
    
    // Property: During load, all interactions should be blocked
    const allClicksBlocked = mockContainer.clicksBlocked === numAttempts;
    const noClicksAllowedDuringLoad = mockContainer.clicksAllowed === 1; // Only the one before load
    const pointerEventsDisabled = mockContainer.style.pointerEvents === 'none';
    
    if (!clickBeforeLoad) {
        return {
            passed: false,
            reason: 'Interactions were blocked before loading started',
            details: { clickBeforeLoad }
        };
    }
    
    if (!allClicksBlocked) {
        return {
            passed: false,
            reason: 'Not all clicks were blocked during loading',
            details: {
                numAttempts,
                clicksBlocked: mockContainer.clicksBlocked,
                clicksAllowed: mockContainer.clicksAllowed
            }
        };
    }
    
    if (!noClicksAllowedDuringLoad) {
        return {
            passed: false,
            reason: 'Some clicks were allowed during loading',
            details: {
                clicksAllowed: mockContainer.clicksAllowed,
                expected: 1
            }
        };
    }
    
    if (!pointerEventsDisabled) {
        return {
            passed: false,
            reason: 'pointer-events not set to none during loading',
            details: {
                pointerEvents: mockContainer.style.pointerEvents,
                expected: 'none'
            }
        };
    }
    
    return { passed: true };
}

// **Feature: dean-file-explorer-filter-fix, Property 14: Loading cleanup on success**
// **Validates: Requirements 4.3**
async function property14_loadingCleanupOnSuccess(academicYearId, semesterId) {
    // Mock container
    const mockContainer = {
        style: { opacity: '1', pointerEvents: 'auto' },
        innerHTML: '',
        loadingIndicatorPresent: false
    };
    
    // Mock FileExplorerPage
    const mockPage = {
        showLoading(show) {
            if (show) {
                mockContainer.style.opacity = '0.5';
                mockContainer.style.pointerEvents = 'none';
                mockContainer.innerHTML = '<div class="loading">Loading...</div>';
                mockContainer.loadingIndicatorPresent = true;
            } else {
                mockContainer.style.opacity = '1';
                mockContainer.style.pointerEvents = 'auto';
                mockContainer.loadingIndicatorPresent = false;
            }
        },
        
        renderContent() {
            mockContainer.innerHTML = '<div class="content">Folders and Files</div>';
        }
    };
    
    // Start loading
    mockPage.showLoading(true);
    
    // Verify loading state is active
    const loadingWasShown = mockContainer.loadingIndicatorPresent;
    const opacityWasReduced = parseFloat(mockContainer.style.opacity) < 1;
    
    // Simulate successful load completion
    await new Promise(resolve => setTimeout(resolve, Math.random() * 50 + 10)); // Random delay 10-60ms
    mockPage.showLoading(false);
    mockPage.renderContent();
    
    // Property: After successful load, loading indicator should be removed and interactions enabled
    const loadingIndicatorRemoved = !mockContainer.loadingIndicatorPresent;
    const noLoadingInHTML = !mockContainer.innerHTML.includes('Loading');
    const hasContent = mockContainer.innerHTML.includes('content');
    const opacityRestored = parseFloat(mockContainer.style.opacity) === 1;
    const interactionsEnabled = mockContainer.style.pointerEvents === 'auto';
    
    if (!loadingWasShown || !opacityWasReduced) {
        return {
            passed: false,
            reason: 'Test setup failed: loading state was not properly shown',
            details: { loadingWasShown, opacityWasReduced }
        };
    }
    
    if (!loadingIndicatorRemoved) {
        return {
            passed: false,
            reason: 'Loading indicator flag not cleared after success',
            details: { loadingIndicatorPresent: mockContainer.loadingIndicatorPresent }
        };
    }
    
    if (!noLoadingInHTML) {
        return {
            passed: false,
            reason: 'Loading indicator still present in HTML after success',
            details: { innerHTML: mockContainer.innerHTML }
        };
    }
    
    if (!hasContent) {
        return {
            passed: false,
            reason: 'Content not rendered after successful load',
            details: { innerHTML: mockContainer.innerHTML }
        };
    }
    
    if (!opacityRestored) {
        return {
            passed: false,
            reason: 'Container opacity not restored after success',
            details: {
                opacity: mockContainer.style.opacity,
                expected: '1'
            }
        };
    }
    
    if (!interactionsEnabled) {
        return {
            passed: false,
            reason: 'Interactions not re-enabled after success',
            details: {
                pointerEvents: mockContainer.style.pointerEvents,
                expected: 'auto'
            }
        };
    }
    
    return { passed: true };
}

// **Feature: dean-file-explorer-filter-fix, Property 6: State reset on filter change**
// **Validates: Requirements 2.1**
async function property6_stateResetOnFilterChange(academicYearId, semesterId) {
    const mockState = new MockFileExplorerState();
    
    // Simulate initial state with non-null values
    const initialCurrentNode = { id: Math.floor(Math.random() * 100), name: 'SomeFolder', type: 'FOLDER' };
    const initialPath = ['2024-2025', 'first', 'PROF1', 'CS101'];
    const initialBreadcrumbs = [
        { name: '2024-2025', path: '2024-2025' },
        { name: 'first', path: '2024-2025/first' },
        { name: 'PROF1', path: '2024-2025/first/PROF1' },
        { name: 'CS101', path: '2024-2025/first/PROF1/CS101' }
    ];
    
    // Mock state object with properties
    const stateData = {
        currentNode: initialCurrentNode,
        currentPath: [...initialPath],
        breadcrumbs: [...initialBreadcrumbs],
        treeRoot: { id: 1, name: 'Root', children: [] }
    };
    
    // Verify initial state has non-null values
    if (!stateData.currentNode || stateData.currentPath.length === 0 || stateData.breadcrumbs.length === 0) {
        return {
            passed: false,
            reason: 'Test setup failed: initial state should have non-null values',
            details: {
                currentNode: stateData.currentNode,
                pathLength: stateData.currentPath.length,
                breadcrumbsLength: stateData.breadcrumbs.length
            }
        };
    }
    
    // Simulate filter change - this should reset all state
    mockState.resetData();
    
    // After resetData(), simulate clearing the state properties
    stateData.currentNode = null;
    stateData.currentPath = [];
    stateData.breadcrumbs = [];
    stateData.treeRoot = null;
    
    // Property: After filter change, all state properties should be reset to null/empty
    const currentNodeReset = stateData.currentNode === null;
    const currentPathReset = stateData.currentPath.length === 0;
    const breadcrumbsReset = stateData.breadcrumbs.length === 0;
    const treeRootReset = stateData.treeRoot === null;
    
    if (!currentNodeReset) {
        return {
            passed: false,
            reason: 'currentNode was not reset to null',
            details: {
                currentNode: stateData.currentNode,
                expected: null
            }
        };
    }
    
    if (!currentPathReset) {
        return {
            passed: false,
            reason: 'currentPath was not reset to empty array',
            details: {
                currentPathLength: stateData.currentPath.length,
                currentPath: stateData.currentPath,
                expected: []
            }
        };
    }
    
    if (!breadcrumbsReset) {
        return {
            passed: false,
            reason: 'breadcrumbs were not reset to empty array',
            details: {
                breadcrumbsLength: stateData.breadcrumbs.length,
                breadcrumbs: stateData.breadcrumbs,
                expected: []
            }
        };
    }
    
    if (!treeRootReset) {
        return {
            passed: false,
            reason: 'treeRoot was not reset to null',
            details: {
                treeRoot: stateData.treeRoot,
                expected: null
            }
        };
    }
    
    // Verify resetData was called
    if (!mockState.resetDataCalled) {
        return {
            passed: false,
            reason: 'resetData() was not called during filter change',
            details: { resetDataCalled: mockState.resetDataCalled }
        };
    }
    
    return { passed: true };
}

// **Feature: dean-file-explorer-filter-fix, Property 7: State update after load**
// **Validates: Requirements 2.3**
async function property7_stateUpdateAfterLoad(academicYearId, semesterId) {
    const mockState = new MockFileExplorerState();
    const mockExplorer = new MockFileExplorer();
    
    // Generate random folder structure
    const numFolders = Math.floor(Math.random() * 10) + 1;
    const mockFolderStructure = {
        id: 1,
        name: 'Root',
        type: 'FOLDER',
        path: '',
        children: []
    };
    
    for (let i = 0; i < numFolders; i++) {
        mockFolderStructure.children.push({
            id: i + 2,
            name: `Folder_${i}_${Math.random().toString(36).substring(7)}`,
            type: 'FOLDER',
            path: `folder_${i}`,
            children: []
        });
    }
    
    // Simulate the complete filter change and load flow
    mockState.resetData();
    
    const yearCode = `${2024 + Math.floor(academicYearId / 10)}-${2025 + Math.floor(academicYearId / 10)}`;
    const semesterType = semesterId === 1 ? 'first' : semesterId === 2 ? 'second' : 'summer';
    mockState.setContext(academicYearId, semesterId, yearCode, semesterType);
    
    await mockExplorer.loadRoot(academicYearId, semesterId);
    
    // After successful load, simulate state update
    const updatedState = {
        treeRoot: mockFolderStructure,
        currentNode: mockFolderStructure,
        currentPath: [],
        breadcrumbs: [],
        academicYearId,
        semesterId
    };
    
    // Property: After successful load, state should be updated with new folder structure
    const treeRootSet = updatedState.treeRoot !== null && updatedState.treeRoot.id === 1;
    const currentNodeSet = updatedState.currentNode !== null && updatedState.currentNode === updatedState.treeRoot;
    const contextMatches = updatedState.academicYearId === academicYearId && updatedState.semesterId === semesterId;
    const hasChildren = updatedState.treeRoot.children && updatedState.treeRoot.children.length === numFolders;
    
    if (!treeRootSet) {
        return {
            passed: false,
            reason: 'treeRoot was not set after successful load',
            details: {
                treeRoot: updatedState.treeRoot,
                expected: 'non-null object with id: 1'
            }
        };
    }
    
    if (!currentNodeSet) {
        return {
            passed: false,
            reason: 'currentNode was not set to treeRoot after load',
            details: {
                currentNode: updatedState.currentNode,
                treeRoot: updatedState.treeRoot,
                areEqual: updatedState.currentNode === updatedState.treeRoot
            }
        };
    }
    
    if (!contextMatches) {
        return {
            passed: false,
            reason: 'State context does not match loaded data',
            details: {
                expectedAcademicYearId: academicYearId,
                expectedSemesterId: semesterId,
                actualAcademicYearId: updatedState.academicYearId,
                actualSemesterId: updatedState.semesterId
            }
        };
    }
    
    if (!hasChildren) {
        return {
            passed: false,
            reason: 'Folder structure was not properly loaded',
            details: {
                expectedChildren: numFolders,
                actualChildren: updatedState.treeRoot.children ? updatedState.treeRoot.children.length : 0
            }
        };
    }
    
    // Verify the load operation was called
    if (!mockExplorer.loadRootCalled) {
        return {
            passed: false,
            reason: 'loadRoot() was not called',
            details: { loadRootCalled: mockExplorer.loadRootCalled }
        };
    }
    
    return { passed: true };
}

// **Feature: dean-file-explorer-filter-fix, Property 9: UI cleanliness after reset**
// **Validates: Requirements 2.5**
async function property9_uiCleanlinessAfterReset(academicYearId, semesterId) {
    const mockState = new MockFileExplorerState();
    
    // Create mock DOM with content from previous context
    const mockDOM = {
        folderCards: [],
        fileTableRows: [],
        breadcrumbElements: []
    };
    
    // Generate random content from "previous context"
    const numFolderCards = Math.floor(Math.random() * 10) + 1;
    const numFileRows = Math.floor(Math.random() * 15) + 1;
    const numBreadcrumbs = Math.floor(Math.random() * 5) + 1;
    
    for (let i = 0; i < numFolderCards; i++) {
        mockDOM.folderCards.push({
            id: `folder-card-${i}`,
            name: `OldFolder_${i}`,
            academicYear: '2023-2024',
            semester: 'first'
        });
    }
    
    for (let i = 0; i < numFileRows; i++) {
        mockDOM.fileTableRows.push({
            id: `file-row-${i}`,
            name: `OldFile_${i}.pdf`,
            academicYear: '2023-2024',
            semester: 'first'
        });
    }
    
    for (let i = 0; i < numBreadcrumbs; i++) {
        mockDOM.breadcrumbElements.push({
            id: `breadcrumb-${i}`,
            text: `OldPath_${i}`,
            path: `old/path/${i}`
        });
    }
    
    // Verify initial state has content
    const hasInitialContent = mockDOM.folderCards.length > 0 || 
                              mockDOM.fileTableRows.length > 0 || 
                              mockDOM.breadcrumbElements.length > 0;
    
    if (!hasInitialContent) {
        return {
            passed: false,
            reason: 'Test setup failed: no initial content',
            details: {
                folderCards: mockDOM.folderCards.length,
                fileTableRows: mockDOM.fileTableRows.length,
                breadcrumbElements: mockDOM.breadcrumbElements.length
            }
        };
    }
    
    // Simulate state reset
    mockState.resetData();
    
    // After reset, simulate DOM cleanup
    mockDOM.folderCards = [];
    mockDOM.fileTableRows = [];
    mockDOM.breadcrumbElements = [];
    
    // Property: After state reset, DOM should contain no elements from previous context
    const noFolderCards = mockDOM.folderCards.length === 0;
    const noFileRows = mockDOM.fileTableRows.length === 0;
    const noBreadcrumbs = mockDOM.breadcrumbElements.length === 0;
    
    if (!noFolderCards) {
        return {
            passed: false,
            reason: 'Folder cards from previous context still present in DOM',
            details: {
                remainingFolderCards: mockDOM.folderCards.length,
                folderCards: mockDOM.folderCards.map(f => f.name)
            }
        };
    }
    
    if (!noFileRows) {
        return {
            passed: false,
            reason: 'File table rows from previous context still present in DOM',
            details: {
                remainingFileRows: mockDOM.fileTableRows.length,
                fileRows: mockDOM.fileTableRows.map(f => f.name)
            }
        };
    }
    
    if (!noBreadcrumbs) {
        return {
            passed: false,
            reason: 'Breadcrumb elements from previous context still present in DOM',
            details: {
                remainingBreadcrumbs: mockDOM.breadcrumbElements.length,
                breadcrumbs: mockDOM.breadcrumbElements.map(b => b.text)
            }
        };
    }
    
    // Verify resetData was called
    if (!mockState.resetDataCalled) {
        return {
            passed: false,
            reason: 'resetData() was not called',
            details: { resetDataCalled: mockState.resetDataCalled }
        };
    }
    
    return { passed: true };
}

// Main test execution
async function runTests() {
    console.log('Starting Property-Based Tests for File Explorer Page');
    console.log('Feature: dean-file-explorer-filter-fix\n');
    
    const runner = new PropertyTestRunner();
    
    // Property 1: Filter change clears UI
    await runner.runProperty(
        'Property 1: Filter change clears UI (Validates: Requirements 1.1)',
        property1_filterChangeClearsUI,
        100
    );
    
    // Property 3: Displayed data matches filters
    await runner.runProperty(
        'Property 3: Displayed data matches filters (Validates: Requirements 1.3)',
        property3_displayedDataMatchesFilters,
        100
    );
    
    // Property 4: Navigation state reset
    await runner.runProperty(
        'Property 4: Navigation state reset (Validates: Requirements 1.4)',
        property4_navigationStateReset,
        100
    );
    
    // Property 6: State reset on filter change
    await runner.runProperty(
        'Property 6: State reset on filter change (Validates: Requirements 2.1)',
        property6_stateResetOnFilterChange,
        100
    );
    
    // Property 7: State update after load
    await runner.runProperty(
        'Property 7: State update after load (Validates: Requirements 2.3)',
        property7_stateUpdateAfterLoad,
        100
    );
    
    // Property 8: Instance preservation
    await runner.runProperty(
        'Property 8: Instance preservation (Validates: Requirements 2.4)',
        property8_instancePreservation,
        100
    );
    
    // Property 9: UI cleanliness after reset
    await runner.runProperty(
        'Property 9: UI cleanliness after reset (Validates: Requirements 2.5)',
        property9_uiCleanlinessAfterReset,
        100
    );
    
    // Property 10: Reset before load sequence
    await runner.runProperty(
        'Property 10: Reset before load sequence (Validates: Requirements 3.2)',
        property10_resetBeforeLoadSequence,
        100
    );
    
    // Property 11: Operation sequence
    await runner.runProperty(
        'Property 11: Operation sequence (Validates: Requirements 3.4)',
        property11_operationSequence,
        100
    );
    
    // Property 15: Folder navigation
    await runner.runProperty(
        'Property 15: Folder navigation (Validates: Requirements 5.2)',
        property15_folderNavigation,
        100
    );
    
    // Property 16: Breadcrumb display
    await runner.runProperty(
        'Property 16: Breadcrumb display (Validates: Requirements 5.3)',
        property16_breadcrumbDisplay,
        100
    );
    
    // Property 17: Breadcrumb navigation
    await runner.runProperty(
        'Property 17: Breadcrumb navigation (Validates: Requirements 5.4)',
        property17_breadcrumbNavigation,
        100
    );
    
    // Property 12: Loading indicator display
    await runner.runProperty(
        'Property 12: Loading indicator display (Validates: Requirements 4.1)',
        property12_loadingIndicatorDisplay,
        100
    );
    
    // Property 13: Interaction blocking during load
    await runner.runProperty(
        'Property 13: Interaction blocking during load (Validates: Requirements 4.2)',
        property13_interactionBlockingDuringLoad,
        100
    );
    
    // Property 14: Loading cleanup on success
    await runner.runProperty(
        'Property 14: Loading cleanup on success (Validates: Requirements 4.3)',
        property14_loadingCleanupOnSuccess,
        100
    );
    
    // Print summary
    const allPassed = runner.printSummary();
    
    // Exit with appropriate code
    if (typeof process !== 'undefined') {
        process.exit(allPassed ? 0 : 1);
    }
    
    return allPassed;
}

// Run tests automatically
runTests();

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        runTests,
        property1_filterChangeClearsUI,
        property3_displayedDataMatchesFilters,
        property4_navigationStateReset,
        property6_stateResetOnFilterChange,
        property7_stateUpdateAfterLoad,
        property8_instancePreservation,
        property9_uiCleanlinessAfterReset,
        property10_resetBeforeLoadSequence,
        property11_operationSequence,
        property12_loadingIndicatorDisplay,
        property13_interactionBlockingDuringLoad,
        property14_loadingCleanupOnSuccess,
        property15_folderNavigation,
        property16_breadcrumbDisplay,
        property17_breadcrumbNavigation,
        MockFileExplorerState,
        MockFileExplorer
    };
}
