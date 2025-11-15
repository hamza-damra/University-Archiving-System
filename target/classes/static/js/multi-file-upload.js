/**
 * Professional Multi-File Upload Component
 * Provides drag-and-drop interface, file preview, progress tracking, and file management
 */

import { uploadMultipleFiles, addFilesToSubmission, deleteFileAttachment, reorderFileAttachments, getFileAttachments } from './api.js';
import { showToast, showModal } from './ui.js';

/**
 * Create and show multi-file upload modal
 */
export function showMultiFileUploadModal(requestId, allowedExtensions, existingFiles = []) {
    const maxFiles = 5;
    const maxSizePerFile = 10; // MB
    const maxTotalSize = 50; // MB
    
    const files = new Map(); // Map to track selected files
    let fileOrder = 0;
    
    const content = `
        <div class="space-y-4">
            <!-- Drop Zone -->
            <div id="dropZone" class="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center cursor-pointer hover:border-blue-500 transition-colors bg-gray-50 hover:bg-blue-50">
                <div class="space-y-2">
                    <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
                    </svg>
                    <div class="text-sm text-gray-600">
                        <label for="fileInput" class="cursor-pointer text-blue-600 hover:text-blue-700 font-medium">
                            Click to upload
                        </label>
                        or drag and drop
                    </div>
                    <p class="text-xs text-gray-500">
                        ${allowedExtensions ? `Allowed: ${allowedExtensions}` : 'Multiple files supported'} 
                        (Max ${maxFiles} files, ${maxSizePerFile}MB each, ${maxTotalSize}MB total)
                    </p>
                </div>
                <input 
                    type="file" 
                    id="fileInput" 
                    class="hidden" 
                    multiple
                    accept=".${allowedExtensions.split(',').join(',.')}"
                >
            </div>
            
            <!-- File List -->
            <div id="fileList" class="space-y-2 max-h-60 overflow-y-auto"></div>
            
            <!-- Notes -->
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Notes (Optional)</label>
                <textarea 
                    id="uploadNotes" 
                    rows="3" 
                    class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Add any notes about these files..."
                ></textarea>
            </div>
            
            <!-- Upload Progress -->
            <div id="uploadProgress" class="hidden space-y-2">
                <div class="progress-bar">
                    <div id="progressFill" class="progress-bar-fill" style="width: 0%"></div>
                </div>
                <p id="progressText" class="text-sm text-gray-600 text-center">Uploading...</p>
            </div>
            
            <!-- Error Message -->
            <p id="uploadError" class="text-red-600 text-sm hidden"></p>
        </div>
    `;
    
    const modal = showModal('Upload Multiple Files', content, {
        size: 'lg',
        buttons: [
            {
                text: 'Cancel',
                className: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
                action: 'cancel',
                onClick: (close) => close(),
            },
            {
                text: 'Upload',
                className: 'bg-blue-600 text-white hover:bg-blue-700',
                action: 'upload',
                onClick: async (close) => {
                    if (files.size === 0) {
                        showError('Please select at least one file');
                        return;
                    }
                    
                    await handleUpload(requestId, Array.from(files.values()), close);
                },
            },
        ],
    });
    
    // Setup event handlers
    const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('fileInput');
    const fileList = document.getElementById('fileList');
    const uploadError = document.getElementById('uploadError');

    let isProcessingFiles = false; // Prevent double processing

    // Click to select files
    dropZone.addEventListener('click', (e) => {
        if (e.target.id !== 'fileInput' && e.target.tagName !== 'INPUT') {
            e.preventDefault();
            e.stopPropagation();
            fileInput.click();
        }
    });

    // File selection
    fileInput.addEventListener('change', (e) => {
        if (!isProcessingFiles && e.target.files.length > 0) {
            isProcessingFiles = true;
            handleFileSelection(e.target.files);
            // Reset file input to allow selecting the same files again if needed
            setTimeout(() => {
                fileInput.value = '';
                isProcessingFiles = false;
            }, 100);
        }
    });
    
    // Drag and drop
    dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropZone.classList.add('border-blue-500', 'bg-blue-50');
    });
    
    dropZone.addEventListener('dragleave', (e) => {
        e.preventDefault();
        dropZone.classList.remove('border-blue-500', 'bg-blue-50');
    });
    
    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropZone.classList.remove('border-blue-500', 'bg-blue-50');
        handleFileSelection(e.dataTransfer.files);
    });
    
    function handleFileSelection(selectedFiles) {
        uploadError.classList.add('hidden');
        
        // Validate file count
        if (files.size + selectedFiles.length > maxFiles) {
            showError(`Maximum ${maxFiles} files allowed. Currently selected: ${files.size}`);
            return;
        }
        
        // Add files
        Array.from(selectedFiles).forEach(file => {
            // Validate file
            const validation = validateFile(file, allowedExtensions, maxSizePerFile);
            if (!validation.valid) {
                showError(validation.error);
                return;
            }
            
            // Check if file already added
            const fileKey = `${file.name}_${file.size}`;
            if (files.has(fileKey)) {
                showError(`File "${file.name}" already added`);
                return;
            }
            
            files.set(fileKey, file);
            renderFileItem(file, fileKey);
        });
        
        // Validate total size
        validateTotalSize();
    }
    
    function renderFileItem(file, fileKey) {
        const fileItem = document.createElement('div');
        fileItem.className = 'flex items-center justify-between p-3 bg-gray-50 rounded-lg border border-gray-200 hover:bg-gray-100 transition-colors';
        fileItem.id = `file-${fileKey}`;
        fileItem.draggable = true;
        
        const fileIcon = getFileIcon(file.type);
        const fileSize = formatFileSize(file.size);
        
        fileItem.innerHTML = `
            <div class="flex items-center space-x-3 flex-1">
                <div class="text-2xl">${fileIcon}</div>
                <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium text-gray-900 truncate">${file.name}</p>
                    <p class="text-xs text-gray-500">${fileSize}</p>
                </div>
            </div>
            <div class="flex items-center space-x-2">
                <span class="text-xs text-gray-400 drag-handle cursor-move">☰</span>
                <button class="text-red-600 hover:text-red-700 p-1" onclick="removeFile('${fileKey}')">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                </button>
            </div>
        `;
        
        // Drag and drop for reordering
        fileItem.addEventListener('dragstart', (e) => {
            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/plain', fileKey);
            fileItem.classList.add('opacity-50');
        });
        
        fileItem.addEventListener('dragend', () => {
            fileItem.classList.remove('opacity-50');
        });
        
        fileItem.addEventListener('dragover', (e) => {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            fileItem.classList.add('border-blue-500');
        });
        
        fileItem.addEventListener('dragleave', () => {
            fileItem.classList.remove('border-blue-500');
        });
        
        fileItem.addEventListener('drop', (e) => {
            e.preventDefault();
            fileItem.classList.remove('border-blue-500');
            
            const draggedKey = e.dataTransfer.getData('text/plain');
            if (draggedKey === fileKey) return;
            
            const draggedElement = document.getElementById(`file-${draggedKey}`);
            const rect = fileItem.getBoundingClientRect();
            const midpoint = rect.top + rect.height / 2;
            
            if (e.clientY < midpoint) {
                fileList.insertBefore(draggedElement, fileItem);
            } else {
                fileList.insertBefore(draggedElement, fileItem.nextSibling);
            }
        });
        
        fileList.appendChild(fileItem);
    }
    
    window.removeFile = function(fileKey) {
        files.delete(fileKey);
        document.getElementById(`file-${fileKey}`)?.remove();
        validateTotalSize();
    };
    
    function validateTotalSize() {
        const totalSize = Array.from(files.values()).reduce((sum, file) => sum + file.size, 0);
        const totalSizeMB = totalSize / (1024 * 1024);
        
        if (totalSizeMB > maxTotalSize) {
            showError(`Total file size (${totalSizeMB.toFixed(2)}MB) exceeds maximum allowed (${maxTotalSize}MB)`);
            return false;
        }
        
        return true;
    }
    
    function showError(message) {
        uploadError.textContent = message;
        uploadError.classList.remove('hidden');
    }
    
    async function handleUpload(requestId, filesToUpload, close) {
        if (!validateTotalSize()) return;
        
        const uploadProgress = document.getElementById('uploadProgress');
        const progressFill = document.getElementById('progressFill');
        const progressText = document.getElementById('progressText');
        const uploadBtn = modal.querySelector('[data-action="upload"]');
        const cancelBtn = modal.querySelector('[data-action="cancel"]');
        const notes = document.getElementById('uploadNotes').value;
        
        uploadProgress.classList.remove('hidden');
        uploadBtn.disabled = true;
        cancelBtn.disabled = true;
        uploadBtn.textContent = 'Uploading...';
        
        try {
            const formData = new FormData();
            filesToUpload.forEach(file => formData.append('files', file));
            if (notes) formData.append('notes', notes);
            
            await uploadMultipleFiles(
                requestId,
                formData,
                (percent) => {
                    progressFill.style.width = `${percent}%`;
                    progressText.textContent = `Uploading... ${Math.round(percent)}%`;
                }
            );
            
            showToast(`Successfully uploaded ${filesToUpload.length} file(s)`, 'success');
            
            // Reload the requests to update UI
            if (window.loadRequests) {
                window.loadRequests();
            }
            
            close();
        } catch (error) {
            console.error('Upload error:', error);
            showError(error.message || 'Upload failed');
            uploadBtn.disabled = false;
            cancelBtn.disabled = false;
            uploadBtn.textContent = 'Upload';
            uploadProgress.classList.add('hidden');
        }
    }
}

// Helper functions
function validateFile(file, allowedExtensions, maxSizePerFile) {
    if (!file) {
        return { valid: false, error: 'No file provided' };
    }
    
    const extension = file.name.split('.').pop().toLowerCase();
    const extensions = allowedExtensions ? allowedExtensions.split(',').map(e => e.trim().toLowerCase()) : [];
    
    if (extensions.length > 0 && !extensions.includes(extension)) {
        return { valid: false, error: `File type .${extension} not allowed. Allowed: ${allowedExtensions}` };
    }
    
    const fileSizeMB = file.size / (1024 * 1024);
    if (fileSizeMB > maxSizePerFile) {
        return { valid: false, error: `File "${file.name}" exceeds ${maxSizePerFile}MB limit (${fileSizeMB.toFixed(2)}MB)` };
    }
    
    return { valid: true };
}

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
}

function getFileIcon(mimeType) {
    if (mimeType.includes('pdf')) return '📄';
    if (mimeType.includes('word') || mimeType.includes('document')) return '📝';
    if (mimeType.includes('excel') || mimeType.includes('spreadsheet')) return '📊';
    if (mimeType.includes('powerpoint') || mimeType.includes('presentation')) return '📽️';
    if (mimeType.includes('image')) return '🖼️';
    if (mimeType.includes('video')) return '🎥';
    if (mimeType.includes('audio')) return '🎵';
    if (mimeType.includes('zip') || mimeType.includes('compressed')) return '📦';
    return '📎';
}

// Make function globally available
window.showMultiFileUploadModal = showMultiFileUploadModal;
