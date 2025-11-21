# Testing Checklist for File Upload Fix

## Pre-Test Verification

- [x] Jackson Hibernate6 module dependency added to `pom.xml`
- [x] `JacksonConfig.java` created with Hibernate6Module configuration
- [x] `User.java` entity updated with `hibernateLazyInitializer` and `handler` in `@JsonIgnoreProperties`
- [x] `SubmittedDocumentResponse.java` DTO created
- [x] `ProfessorController.java` updated to return DTOs instead of entities
- [x] No compilation errors

## Step-by-Step Testing

### 1. Application Startup
- [ ] Stop the currently running application (if running)
- [ ] Rebuild the application
- [ ] Start the Spring Boot application
- [ ] Verify no errors in the console logs
- [ ] Check that the application starts successfully on port 8080

### 2. Authentication Test
- [ ] Open browser to `http://localhost:8080`
- [ ] Login as a professor (e.g., `professor.cs@alquds.edu` / `password123`)
- [ ] Verify successful login and redirect to professor dashboard

### 3. File Upload Test - Happy Path
- [ ] Navigate to document requests list
- [ ] Find a pending request (or create one if needed)
- [ ] Click "Upload Document" button
- [ ] Select a valid file (PDF, DOC, etc.) within size limit
- [ ] Click "Upload" button
- [ ] **Expected Result**: 
  - Upload progress bar shows
  - Success toast message appears: "Document uploaded successfully"
  - Request status updates to "Submitted"
  - No 500 error
  - No console errors

### 4. Verify Response Structure
- [ ] Open browser Developer Tools (F12)
- [ ] Go to Network tab
- [ ] Upload a document
- [ ] Check the response from `POST /api/professor/document-requests/{id}/submit`
- [ ] **Expected Response**:
```json
{
  "success": true,
  "message": "Document uploaded successfully",
  "data": {
    "id": <number>,
    "requestId": <number>,
    "originalFilename": "<filename>",
    "fileUrl": "<stored_filename>",
    "fileSize": <number>,
    "fileType": "<mime_type>",
    "professorId": <number>,
    "professorName": "<full_name>",
    "professorEmail": "<email>",
    "submittedAt": "<timestamp>",
    "isLateSubmission": <boolean>,
    "createdAt": "<timestamp>",
    "updatedAt": "<timestamp>"
  }
}
```
- [ ] Verify HTTP status is `201 Created` (for upload) or `200 OK` (for replace)
- [ ] Verify no Jackson serialization errors in response

### 5. View Submitted Document Test
- [ ] After uploading, view the document request details
- [ ] Verify submitted document information displays correctly
- [ ] Check that professor name and email are shown
- [ ] **Expected**: No 500 errors when viewing document details

### 6. Get My Submitted Documents Test
- [ ] Navigate to "My Submissions" or similar section (if available)
- [ ] **Expected**: List of submitted documents loads without errors
- [ ] Verify all document fields display correctly

### 7. Replace Document Test
- [ ] Find a request with an already submitted document
- [ ] Click "Replace Document" or similar button
- [ ] Select a new file
- [ ] Upload the replacement
- [ ] **Expected**:
  - Old file is deleted
  - New file is uploaded
  - Response is successful (200 OK)
  - No serialization errors

### 8. Error Handling Tests
- [ ] **Test invalid file type**: Upload a `.exe` or non-allowed file
  - **Expected**: Error message about invalid file type
- [ ] **Test file too large**: Upload a file > 10MB
  - **Expected**: Error message about file size limit
- [ ] **Test missing file**: Try to submit without selecting a file
  - **Expected**: Validation error asking to select a file

### 9. Backend Logs Verification
- [ ] Check application console for any errors during tests
- [ ] Verify no `InvalidDefinitionException` errors
- [ ] Verify no `ByteBuddyInterceptor` serialization errors
- [ ] Check that file operations are logged correctly

### 10. Database Verification
- [ ] Connect to MySQL database
- [ ] Check `submitted_documents` table
- [ ] Verify new records are created with:
  - Correct `request_id`
  - Correct `professor_id`
  - Correct `file_url`
  - Correct timestamps

### 11. File System Verification
- [ ] Navigate to the uploads directory (check `application.properties` for path)
- [ ] Verify uploaded files exist with UUID-based names
- [ ] Verify old files are deleted when replaced

## PowerShell Test Script (Optional)
Run the provided test script for automated testing:

```powershell
# In PowerShell terminal
cd "C:\Users\Hamza Damra\Documents\ArchiveSystem\ArchiveSystem"
.\test-file-upload.ps1
```

- [ ] Script runs without errors
- [ ] All test cases pass
- [ ] File upload returns 201 status
- [ ] Response contains proper JSON structure

## Known Issues to Watch For

### ✅ FIXED Issues:
- ~~500 Internal Server Error on file upload~~
- ~~Jackson serialization error with Hibernate proxies~~
- ~~ByteBuddyInterceptor not found error~~

### Potential Issues (if they occur):

1. **"No serializer found" error still appears**
   - Verify Jackson Hibernate6 module is in classpath
   - Check Spring Boot version compatibility
   - Ensure application was restarted after changes

2. **Response is null or empty**
   - Check if DTO mapping is working correctly
   - Verify professor and documentRequest are not null

3. **Performance issues**
   - Monitor N+1 queries in logs
   - Verify lazy loading is not being forced

## Success Criteria

✅ **All tests pass when**:
1. File upload completes successfully (201 Created)
2. Response returns proper JSON with all fields
3. No Jackson serialization errors in logs
4. No 500 errors on any endpoint
5. Frontend displays success message
6. File is stored on disk with correct naming
7. Database record is created correctly
8. Frontend can view and download the submitted document

## Rollback Plan (If Issues Occur)

If critical issues are found:
1. Revert changes in `ProfessorController.java`
2. Remove `JacksonConfig.java`
3. Remove `SubmittedDocumentResponse.java`
4. Revert `User.java` changes
5. Revert `pom.xml` changes
6. Rebuild and restart application

## Notes
- Test with different file types: PDF, DOC, DOCX, PNG, ZIP
- Test with various file sizes: small (<1MB), medium (5MB), large (9MB)
- Test as different professors to verify authorization
- Test concurrent uploads if possible
