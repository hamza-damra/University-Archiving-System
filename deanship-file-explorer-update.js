// ============================================================================
// FILE EXPLORER - Updated to use FileExplorer component
// ============================================================================
// Replace the old file explorer implementation in deanship.js with this code

/**
 * Initialize file explorer component
 */
function initializeFileExplorer() {
    try {
        fileExplorerInstance = new FileExplorer('fileExplorerContent', {
            readOnly: false // Deanship has full access
        });
        
        // Make it globally accessible for event handlers
        window.fileExplorerInstance = fileExplorerInstance;
    } catch (error) {
        console.error('Error initializing file explorer:', error);
        showToast('Failed to initialize file explorer', 'error');
    }
}

/**
 * Load file explorer for selected semester
 */
async function loadFileExplorer() {
    if (!selectedAcademicYear || !selectedSemester || !fileExplorerInstance) {
        const container = document.getElementById('fileExplorerContent');
        if (container) {
            container.innerHTML = `
                <div class="text-center py-12 text-gray-500">
                    <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
                    </svg>
                    <p class="mt-2">Select an academic year and semester to browse files</p>
                </div>
            `;
        }
        return;
    }
    
    try {
        // Find the semester object
        const year = academicYears.find(y => y.id === selectedAcademicYear.id);
        const semester = year?.semesters?.find(s => s.type === selectedSemester);
        
        if (semester) {
            await fileExplorerInstance.loadRoot(selectedAcademicYear.id, semester.id);
        }
    } catch (error) {
        console.error('Error loading file explorer:', error);
        showToast('Failed to load file explorer', 'error');
    }
}

// Remove these old functions - they are no longer needed:
// - renderFileExplorer()
// - updateBreadcrumbs()
// - window.deanship.navigateToFolder()
// - window.deanship.downloadFile()
// - formatFileSize()
//
// The FileExplorer component handles all of this automatically!
