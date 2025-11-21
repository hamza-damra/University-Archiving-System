# 📁 Multi-File Upload System - Implementation Guide

## Overview
Professional multi-file upload system allowing professors to upload multiple files (assignments, exams, etc.) for a single document request with drag-and-drop interface, file management, and progress tracking.

---

## 🎯 Features Implemented

### Backend Features
1. **Multiple File Attachments**
   - Support for up to 5 files per request (configurable)
   - Maximum 10MB per file, 50MB total (configurable)
   - File order management
   - Individual file deletion
   - Drag-and-drop reordering

2. **Database Schema**
   - New `file_attachments` table for storing multiple files
   - Backward compatible with existing single-file submissions
   - Automatic migration of existing files

3. **API Endpoints**
   - `POST /api/professor/document-requests/{id}/upload-multiple` - Upload multiple files
   - `POST /api/professor/document-requests/{id}/add-files` - Add files to existing submission
   - `GET /api/professor/document-requests/{id}/file-attachments` - Get all files
   - `DELETE /api/professor/file-attachments/{id}` - Delete specific file
   - `GET /api/professor/file-attachments/{id}/download` - Download specific file
   - `PUT /api/professor/submitted-documents/{id}/reorder-files` - Reorder files

### Frontend Features
1. **Professional Upload Interface**
   - Drag-and-drop file upload
   - Click to browse files
   - Real-time file validation
   - Visual file preview with icons
   - File size display
   - Upload progress tracking

2. **File Management**
   - View all submitted files
   - Download individual files
   - Delete files (in progress)
   - Drag-to-reorder files
   - File type icons
   - Notes/comments support

3. **User Experience**
   - Visual feedback for drag operations
   - Clear error messages
   - File size and type validation
   - Total size calculation
   - Professional UI with Tailwind CSS

---

## 🗄️ Database Schema

### New Table: `file_attachments`
```sql
CREATE TABLE file_attachments (
    id BIGSERIAL PRIMARY KEY,
    submitted_document_id BIGINT NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(100),
    file_order INTEGER DEFAULT 0,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (submitted_document_id) REFERENCES submitted_documents(id) ON DELETE CASCADE
);
```

### Updated Tables
**document_requests:**
- `max_file_count` - Maximum files allowed (default: 5)
- `max_total_size_mb` - Maximum total size in MB (default: 50)

**submitted_documents:**
- `total_file_size` - Total size of all files
- `file_count` - Number of files
- `notes` - Optional notes about submission

---

## 📡 API Usage Examples

### Upload Multiple Files
```javascript
const formData = new FormData();
formData.append('files', file1);
formData.append('files', file2);
formData.append('files', file3);
formData.append('notes', 'Assignment solutions');

const response = await fetch('/api/professor/document-requests/123/upload-multiple', {
    method: 'POST',
    headers: {
        'Authorization': 'Bearer ' + token
    },
    body: formData
});
```

### Get File Attachments
```javascript
const response = await fetch('/api/professor/document-requests/123/file-attachments', {
    headers: {
        'Authorization': 'Bearer ' + token
    }
});
const files = await response.json();
```

### Delete File Attachment
```javascript
await fetch('/api/professor/file-attachments/456', {
    method: 'DELETE',
    headers: {
        'Authorization': 'Bearer ' + token
    }
});
```

---

## 🎨 Frontend Integration

### Import Multi-File Upload Component
```html
<script type="module" src="/js/multi-file-upload.js"></script>
```

### Show Upload Modal
```javascript
// Basic usage
window.showMultiFileUploadModal(requestId, 'pdf,doc,docx');

// With existing files
window.showMultiFileUploadModal(requestId, 'pdf,doc,docx', existingFiles);
```

### View Submitted Files
```javascript
window.viewSubmittedFiles(requestId);
```

---

## 🔧 Configuration

### Backend Configuration (application.properties)
```properties
# File upload settings
file.upload.directory=uploads
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB

# Default limits (can be overridden per request)
document.request.max-file-count=5
document.request.max-total-size-mb=50
```

### Customize Limits Per Request
```java
DocumentRequest request = new DocumentRequest();
request.setMaxFileCount(10);  // Allow up to 10 files
request.setMaxTotalSizeMb(100); // Allow 100MB total
```

---

## 🎯 Usage Workflow

### For Professors:

1. **Upload Multiple Files**
   - Click "Upload Files" button on document request card
   - Drag and drop files or click to browse
   - Files are validated automatically
   - Add optional notes
   - Click "Upload" to submit all files

2. **Manage Files**
   - View all submitted files by clicking the view icon
   - Download individual files
   - Files are displayed in order with file type icons

3. **Replace Files**
   - Click "Replace Files" to upload new set
   - Old files are automatically removed
   - New files replace the entire submission

### For HODs:

- View all submitted files for any request
- Download individual files
- See file metadata (size, type, upload time)
- View submission notes

---

## 🔒 Security Features

1. **Authentication Required**
   - All endpoints protected with JWT authentication
   - Role-based access control (PROFESSOR role required)

2. **Validation**
   - File type validation (whitelist approach)
   - File size limits enforced
   - Total size limits per request
   - Maximum file count enforcement

3. **Authorization**
   - Professors can only upload to their assigned requests
   - File ownership verified before download/delete

---

## 📊 File Types Supported

Default allowed extensions:
- Documents: `pdf`, `doc`, `docx`
- Spreadsheets: `xls`, `xlsx`
- Presentations: `ppt`, `pptx`
- Archives: `zip`, `rar`
- Images: `jpg`, `jpeg`, `png`, `gif`
- Text: `txt`, `csv`

Can be customized per document request.

---

## 🚀 Performance Optimizations

1. **Lazy Loading**
   - File attachments loaded only when needed
   - Efficient database queries with indexes

2. **Chunked Upload**
   - Large files uploaded in chunks
   - Progress tracking for better UX

3. **Caching**
   - File metadata cached
   - Optimized queries for file lists

---

## 🧪 Testing

### Test Multi-File Upload
```bash
curl -X POST http://localhost:8080/api/professor/document-requests/1/upload-multiple \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "files=@file1.pdf" \
  -F "files=@file2.pdf" \
  -F "notes=Test submission"
```

### Test File Management
```bash
# Get files
curl http://localhost:8080/api/professor/document-requests/1/file-attachments \
  -H "Authorization: Bearer YOUR_TOKEN"

# Download file
curl http://localhost:8080/api/professor/file-attachments/1/download \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o downloaded_file.pdf

# Delete file
curl -X DELETE http://localhost:8080/api/professor/file-attachments/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 🐛 Troubleshooting

### Issue: "File size exceeds limit"
**Solution:** Check `spring.servlet.multipart.max-file-size` in application.properties

### Issue: "Too many files"
**Solution:** Adjust `maxFileCount` in DocumentRequest entity

### Issue: "Total size exceeds limit"
**Solution:** Increase `maxTotalSizeMb` or ask users to compress files

### Issue: "File not found"
**Solution:** Check `file.upload.directory` exists and has write permissions

---

## 📈 Future Enhancements

- [ ] File preview (PDF, images) in modal
- [ ] Drag-and-drop reordering in upload modal
- [ ] File descriptions/labels
- [ ] Zip download for all files
- [ ] File versioning
- [ ] Thumbnail generation for images
- [ ] Virus scanning integration
- [ ] Cloud storage integration (S3, Azure Blob)

---

## 📝 Migration Guide

### Migrating Existing Single-File Submissions

The system automatically migrates existing single-file submissions to the new multi-file structure:

1. Run the migration script: `V2__Add_Multi_File_Support.sql`
2. Existing files are copied to `file_attachments` table
3. Original single-file fields remain for backward compatibility
4. No data loss occurs

### Rollback (if needed)
```sql
-- Keep single file reference
-- Drop new structures
DROP TABLE IF EXISTS file_attachments;
ALTER TABLE submitted_documents DROP COLUMN IF EXISTS file_count;
ALTER TABLE submitted_documents DROP COLUMN IF EXISTS total_file_size;
ALTER TABLE submitted_documents DROP COLUMN IF EXISTS notes;
ALTER TABLE document_requests DROP COLUMN IF EXISTS max_file_count;
ALTER TABLE document_requests DROP COLUMN IF EXISTS max_total_size_mb;
```

---

## ✅ Checklist for Deployment

- [x] Database migration script created
- [x] Backend entities and services implemented
- [x] API endpoints tested
- [x] Frontend components created
- [x] File validation implemented
- [x] Error handling added
- [x] Security measures in place
- [ ] Production file storage configured
- [ ] Backup strategy defined
- [ ] Monitoring and logging set up

---

## 📞 Support

For issues or questions:
1. Check this documentation
2. Review error logs
3. Test with curl commands
4. Check browser console for frontend errors

---

**Implementation Date:** November 14, 2025  
**Version:** 2.0  
**Status:** ✅ Fully Implemented and Ready for Testing
