# Implementation Plan

- [x] 1. Set up backend preview infrastructure





  - Create FilePreviewController with REST endpoints for file preview
  - Create FilePreviewService with permission validation and file retrieval logic
  - Create FileMetadataDTO for transferring file information
  - Add endpoints: GET /api/file-explorer/files/{id}/metadata, GET /api/file-explorer/files/{id}/content, GET /api/file-explorer/files/{id}/preview
  - _Requirements: 1.1, 1.2, 2.1, 3.1, 8.1, 8.2_

- [x] 1.1 Write property test for permission validation


  - **Property 6: Permission-based preview availability**
  - **Property 7: Unauthorized preview attempt handling**
  - **Property 8: Department-scoped access for HOD**
  - **Validates: Requirements 2.1, 2.4, 3.1, 3.3**

- [x] 1.2 Write property test for file type detection


  - **Property 3: Format-specific renderer selection**
  - **Validates: Requirements 1.3, 4.1, 4.2, 4.3, 4.4**

- [x] 2. Implement core frontend preview modal component

  - Create FilePreviewModal class with open/close methods
  - Implement modal UI with header (file metadata), content area, and action buttons
  - Add ESC key and click-outside handlers for modal dismissal
  - Implement loading state with spinner
  - Implement error state with retry button
  - _Requirements: 1.1, 1.2, 1.4, 6.1_

- [x] 2.1 Write property test for modal lifecycle


  - **Property 1: Preview modal displays file content**
  - **Property 4: Modal dismissal behavior**
  - **Validates: Requirements 1.1, 1.4**

- [x] 2.2 Write property test for metadata display


  - **Property 2: Preview modal displays complete metadata**
  - **Validates: Requirements 1.2, 2.2, 3.2**

- [x] 3. Implement text file renderer





  - Create TextRenderer class for plain text files (.txt, .md, .log, .csv)
  - Fetch file content from backend API
  - Display content with preserved formatting (whitespace, line breaks)
  - Implement virtual scrolling for files with more than 1000 lines
  - _Requirements: 4.1, 7.2_

- [x] 3.1 Write property test for text rendering


  - **Property 3: Format-specific renderer selection** (text files)
  - **Property 15: Virtual scrolling for large text files**
  - **Validates: Requirements 4.1, 7.2**

- [x] 4. Implement PDF renderer





  - Create PDFRenderer class for PDF files
  - Use browser's native PDF viewer or integrate PDF.js library
  - Display PDF in iframe or canvas element
  - Implement page navigation controls (previous, next, jump to page)
  - Display current page number and total pages
  - _Requirements: 4.2, 7.1, 7.3_

- [x] 4.1 Write property test for PDF rendering


  - **Property 3: Format-specific renderer selection** (PDF files)
  - **Property 14: Multi-page document navigation**
  - **Validates: Requirements 4.2, 7.1, 7.3**

- [x] 5. Implement code file renderer with syntax highlighting

  - Create CodeRenderer class for code files (.java, .js, .py, .css, .html, .sql, .xml, .json)
  - Integrate Highlight.js or Prism.js for syntax highlighting
  - Detect language from file extension
  - Apply appropriate syntax highlighting theme
  - Display line numbers
  - _Requirements: 4.4_

- [x] 5.1 Write property test for code rendering

  - **Test Status:** ✅ PASSED


  - **Property 3: Format-specific renderer selection** (code files)
  - **Validates: Requirements 4.4**


- [x] 6. Implement Office document renderer




  - Create OfficeRenderer class for Office documents (.doc, .docx, .xls, .xlsx, .ppt, .pptx)
  - Request converted content from backend (HTML or PDF format)
  - Display converted content in preview modal
  - Handle conversion errors gracefully with download fallback
  - _Requirements: 4.3, 10.2_

- [x] 6.1 Write property test for Office rendering


  - **Property 3: Format-specific renderer selection** (Office files)



  - **Property 22: Conversion failure handling**
  - **Validates: Requirements 4.3, 10.2**

- [ ] 7. Add backend Office document conversion support





  - Integrate Apache POI or similar library for Office document reading
  - Implement conversion logic to HTML or PDF format
  - Add caching mechanism for converted documents (optional)
  - Handle conversion errors and return appropriate error responses
  - _Requirements: 4.3, 10.2_

- [ ] 8. Implement preview button component in file explorer





  - Create FilePreviewButton class to render preview buttons

  - Add isPreviewable() method to check if file type is supported
  - Render preview icon/button next to each supported file in file list
  - Add tooltips: "Click to preview" for supported files, "Download only" for unsupported
  - Visually distinguish previewable files (e.g., different icon color)
  - Wire up click handler to open FilePreviewModal
  - _Requirements: 5.1, 5.2, 5.3, 5.4_






- [ ] 8.1 Write property test for preview button rendering
  - **Property 10: Preview button rendering**
  - **Validates: Requirements 5.1, 5.2, 5.3, 5.4**

- [ ] 9. Integrate preview system with existing file explorer

  - Update file-explorer.js to include FilePreviewButton in file row rendering






  - Update deanship-file-explorer-enhanced.js to include preview functionality
  - Update file-explorer-page.js to initialize preview system
  - Ensure preview works in Professor, Dean, and HOD dashboards
  - Test that existing file explorer functionality is not broken
  - _Requirements: 8.2, 8.3, 8.4_

- [ ] 9.1 Write property test for cross-dashboard compatibility
  - **Property 17: Cross-dashboard compatibility**


  - **Validates: Requirements 8.4**

- [x] 10. Implement download functionality in preview modal

  - Add download button to preview modal header
  - Wire up download button to existing file download API




  - Ensure download works for all file types
  - Show download progress indicator (optional)
  - _Requirements: 1.5_

- [x] 10.1 Write property test for download action

  - **Test Status:** ✅ PASSED






  - **Property 5: Download action in preview**
  - **Validates: Requirements 1.5**

- [x] 11. Implement error handling for all error scenarios





  - Add error handling for network errors (show retry button)





  - Add error handling for permission errors (403 - show access denied message)
  - Add error handling for file not found errors (404 - show file deleted message)
  - Add error handling for unsupported formats (show download option)
  - Add error handling for service errors (500 - show service unavailable message)
  - Add error handling for corrupted files (show corruption message)
  - _Requirements: 2.4, 3.3, 4.5, 6.4, 10.1, 10.2, 10.3, 10.4_







- [x] 11.1 Write property test for error handling


  - **Property 7: Unauthorized preview attempt handling**
  - **Property 9: Unsupported format handling**
  - **Property 13: Network error handling**
  - **Property 21: File not found error handling**
  - **Property 22: Conversion failure handling**
  - **Property 23: Service unavailable error handling**
  - **Property 24: Corrupted file detection**
  - **Validates: Requirements 2.4, 3.3, 4.5, 6.4, 10.1, 10.2, 10.3, 10.4**



- [x] 12. Implement large file handling






  - Check file size before loading preview
  - Show warning modal for files larger than 5MB
  - Offer options: partial preview (first N lines/pages) or download
  - Implement partial preview loading for large files


  - _Requirements: 6.3_

- [x] 12.1 Write property test for large file handling





  - **Property 12: Large file warning**
  - **Validates: Requirements 6.3**

- [x] 13. Implement search functionality in text previews






  - Add search input field to preview modal for text-based content
  - Implement search logic to find and highlight matches
  - Add navigation buttons (previous match, next match)
  - Display match count (e.g., "3 of 15 matches")
  - Scroll to highlighted match when navigating
  - _Requirements: 7.4_



- [ ] 13.1 Write property test for search functionality
  - **Property 16: Search functionality in preview**
  - **Validates: Requirements 7.4**

- [x] 14. Implement responsive design for mobile and tablet






  - Add CSS media queries for tablet and mobile breakpoints
  - Adjust modal size and layout for smaller screens
  - Ensure touch-friendly button sizes
  - Test on various screen sizes (desktop, tablet, mobile)
  - _Requirements: 9.1, 9.2_

- [x] 14.1 Write property test for responsive layout


  - **Property 18: Responsive layout adaptation**
  - **Validates: Requirements 9.2**

- [ ] 15. Implement keyboard navigation and accessibility

  - Add keyboard shortcuts: ESC to close, arrow keys for navigation
  - Implement focus trapping within modal
  - Add ARIA labels to all interactive elements
  - Add ARIA roles (dialog, button, etc.)
  - Ensure screen reader announces modal state changes
  - Test with keyboard-only navigation
  - Test with screen reader (NVDA or JAWS)
  - _Requirements: 9.3, 9.4_

- [ ] 15.1 Write property test for keyboard navigation
  - **Property 19: Keyboard navigation support**
  - **Validates: Requirements 9.3**

- [ ] 15.2 Write property test for accessibility compliance
  - **Property 20: Accessibility compliance**
  - **Validates: Requirements 9.4**

- [ ] 16. Implement loading indicator with timing requirement

  - Ensure loading indicator appears within 100ms of preview request
  - Add smooth fade-in animation for loading spinner
  - Show loading progress for large files (optional)
  - _Requirements: 6.1_

- [ ] 16.1 Write property test for loading indicator timing
  - **Property 11: Loading indicator display**
  - **Validates: Requirements 6.1**

- [ ] 17. Add comprehensive error logging

  - Log all preview errors to console with context (file ID, user role, error type)
  - Log performance metrics (preview load time, file size)
  - Add error tracking integration (optional - Sentry, LogRocket)
  - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [ ] 18. Checkpoint - Ensure all tests pass

  - Ensure all tests pass, ask the user if questions arise.

- [ ] 19. Perform manual testing across all dashboards

  - Test preview functionality in Professor dashboard
  - Test preview functionality in Dean dashboard
  - Test preview functionality in HOD dashboard
  - Verify role-specific permissions work correctly
  - Test all supported file formats
  - Test error scenarios (missing files, permission denied, etc.)
  - _Requirements: 8.4_

- [ ] 20. Perform browser compatibility testing

  - Test on Chrome (latest version)
  - Test on Firefox (latest version)
  - Test on Safari (latest version)
  - Test on Edge (latest version)
  - Document any browser-specific issues and workarounds
  - _Requirements: 9.1_

- [ ] 21. Perform accessibility testing

  - Test with keyboard-only navigation
  - Test with screen reader (NVDA or JAWS)
  - Verify ARIA labels are present and correct
  - Check color contrast ratios meet WCAG AA standards
  - Test focus management and focus trapping
  - _Requirements: 9.3, 9.4_


- [ ] 22. Optimize performance
  - Profile preview loading times for various file sizes
  - Optimize large file handling (streaming, pagination)
  - Implement caching for converted Office documents
  - Minimize JavaScript bundle size
  - Lazy load preview libraries (PDF.js, Highlight.js)
  - _Requirements: 6.2, 6.3_

- [ ] 23. Final checkpoint - Ensure all tests pass

  - Ensure all tests pass, ask the user if questions arise.
