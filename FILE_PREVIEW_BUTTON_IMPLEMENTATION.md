# File Preview Button Implementation

## Task 8: Implement preview button component in file explorer

**Status:** ✅ Completed

**Requirements Validated:** 5.1, 5.2, 5.3, 5.4

---

## Overview

Implemented a comprehensive file preview button component that integrates with the existing file explorer to provide visual indicators for previewable files and enable quick access to the file preview modal.

## Implementation Details

### 1. FilePreviewButton Component (`file-preview-button.js`)

Created a new component that handles:

#### Supported File Types
- **Text files:** .txt, .md, .log, .csv
- **PDF files:** .pdf
- **Office documents:** .doc, .docx, .xls, .xlsx, .ppt, .pptx
- **Code files:** .java, .js, .py, .css, .html, .sql, .xml, .json

#### Key Features

**Requirement 5.1: Preview Button Rendering**
- Renders preview buttons next to supported file types in the file explorer
- Uses eye icon SVG for visual consistency
- Includes onclick handler to open FilePreviewModal

**Requirement 5.2: Tooltip for Supported Files**
- Displays "Click to preview" tooltip on hover for supported files
- Provides clear indication that preview is available

**Requirement 5.3: Tooltip for Unsupported Files**
- Displays "Download only - Preview not available for this file type" tooltip
- Shows disabled state with crossed-out eye icon
- No onclick handler for unsupported files

**Requirement 5.4: Visual Distinction**
- **Previewable files:** Blue color (`text-blue-600`) with hover effects
- **Non-previewable files:** Gray color (`text-gray-400`) with disabled cursor
- Clear visual difference between states

#### Methods

```javascript
// Check if file type is previewable
static isPreviewable(fileType, fileName = '')

// Get file extension from filename
static getFileExtension(fileName)

// Render preview button HTML
static renderButton(file)

// Handle preview button click
async handlePreviewClick(fileId, fileName, fileType)

// Get appropriate icon for file type
static getFileIcon(fileType, fileName = '')

// Escape HTML to prevent XSS
static escapeHtml(str)
```

### 2. File Explorer Integration

Updated `file-explorer.js` to include the preview button:

```javascript
import { FilePreviewButton } from './file-preview-button.js';

// In renderFileCard method:
${fileId ? FilePreviewButton.renderButton(file) : ''}
```

The preview button is now rendered as the first action button in each file row, before the "View Details" and "Download" buttons.

### 3. Button Rendering Logic

The component uses a two-tier detection system:

1. **Primary:** MIME type matching against `SUPPORTED_TYPES` map
2. **Fallback:** File extension matching against `SUPPORTED_EXTENSIONS` map

This ensures files are correctly identified even when MIME type information is missing or incorrect.

### 4. Security

- All user-provided data (file names, types) is escaped using `escapeHtml()`
- Prevents XSS attacks through malicious file names
- HTML entities used for special characters: `<`, `>`, `&`, `"`, `'`

## Property-Based Tests

### Task 8.1: Property Test Implementation

**Property 10: Preview button rendering**

Created comprehensive property-based tests that validate:

1. **Supported file types render active buttons**
   - Verifies blue color, tooltip, onclick handler
   - Tests across 100 random file combinations
   - Validates all supported MIME types and extensions

2. **Unsupported file types render disabled buttons**
   - Verifies gray color, disabled state, appropriate tooltip
   - Tests across 100 random unsupported file types
   - Ensures no onclick handler present

3. **Type detection accuracy**
   - Validates `isPreviewable()` correctly identifies supported types
   - Tests both MIME type and extension fallback
   - Covers edge cases with missing MIME types

4. **XSS protection**
   - Tests with malicious file names containing scripts
   - Verifies proper HTML escaping
   - Ensures no executable code in rendered HTML

5. **Visual distinction**
   - Compares previewable vs non-previewable rendering
   - Validates color differences (blue vs gray)
   - Confirms different button states

### Test Files Created

1. **`file-preview-button-pbt.test.js`** - Jasmine/fast-check property tests
2. **`file-preview-button-pbt.test.html`** - Browser-based test runner
3. **`run-preview-button-tests.cjs`** - Node.js test runner
4. **`test-file-preview-button.html`** - Manual test page with visual verification

## Testing

### Manual Testing

Open `test-file-preview-button.html` in a browser to:
- View rendered preview buttons for various file types
- Verify visual distinction between previewable and non-previewable files
- Test tooltips and hover states
- Confirm button behavior

### Property-Based Testing

The tests validate the correctness properties across 100+ random inputs per test case, ensuring:
- Consistent behavior across all supported file types
- Proper handling of edge cases
- Security against XSS attacks
- Visual consistency

## Integration Points

### File Explorer
- Integrated into `renderFileCard()` method
- Appears as first action button in file rows
- Works across all dashboards (Professor, Dean, HOD)

### File Preview Modal
- Clicking preview button opens `FilePreviewModal`
- Delegates download action to file explorer's handler
- Maintains consistent user experience

## Files Modified

1. **Created:**
   - `src/main/resources/static/js/file-preview-button.js`
   - `src/test/resources/static/js/file-preview-button-pbt.test.js`
   - `src/test/resources/static/js/file-preview-button-pbt.test.html`
   - `src/test/resources/static/js/run-preview-button-tests.cjs`
   - `test-file-preview-button.html`

2. **Modified:**
   - `src/main/resources/static/js/file-explorer.js` (added import and button rendering)

## Usage Example

```javascript
// Check if file is previewable
const canPreview = FilePreviewButton.isPreviewable('application/pdf');
// Returns: true

// Render preview button
const file = {
  id: 123,
  originalFilename: 'document.pdf',
  fileType: 'application/pdf'
};
const buttonHtml = FilePreviewButton.renderButton(file);
// Returns: HTML string with active preview button

// Handle preview click (called automatically via onclick)
await window.filePreviewButton.handlePreviewClick(123, 'document.pdf', 'application/pdf');
// Opens FilePreviewModal
```

## Design Decisions

1. **Static Methods:** Used static methods for utility functions that don't require instance state
2. **Global Instance:** Created `window.filePreviewButton` for onclick handlers in rendered HTML
3. **Two-Tier Detection:** MIME type primary, extension fallback for robustness
4. **Visual Consistency:** Followed Tailwind CSS patterns from existing file explorer
5. **Security First:** All user input escaped before rendering

## Next Steps

The preview button component is now ready for integration with the complete file preview system. When users click the preview button:

1. `handlePreviewClick()` is called
2. `FilePreviewModal` instance is created (if not exists)
3. Modal opens with file content
4. User can view, download, or close the preview

## Requirements Validation

✅ **5.1:** Preview icon/button rendered next to supported files  
✅ **5.2:** Tooltip "Click to preview" for supported files  
✅ **5.3:** Tooltip "Download only" for unsupported files  
✅ **5.4:** Visual distinction (blue vs gray) between file types  

---

**Implementation Date:** 2025-11-25  
**Feature:** file-preview-system  
**Task:** 8. Implement preview button component in file explorer
