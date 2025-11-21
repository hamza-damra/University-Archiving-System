# Professor Dashboard User Guide

## Welcome to the Document Archiving System

This guide will help you navigate and use the Professor Dashboard to manage your course documents, view assignments, and interact with the semester-based archiving system.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Dashboard Overview](#dashboard-overview)
3. [Viewing Assigned Courses](#viewing-assigned-courses)
4. [Uploading Documents](#uploading-documents)
5. [Replacing Documents](#replacing-documents)
6. [Using the File Explorer](#using-the-file-explorer)
7. [Managing Notifications](#managing-notifications)
8. [Understanding Document Status](#understanding-document-status)
9. [Troubleshooting](#troubleshooting)
10. [Frequently Asked Questions](#frequently-asked-questions)

---

## Getting Started

### Logging In

1. Navigate to the system login page
2. Enter your email address and password
3. Click the "Login" button
4. You will be redirected to the Professor Dashboard

**Note**: If you forget your password, contact your system administrator for a password reset.

### First Time Login

When you log in for the first time:
1. You'll see the Dashboard tab with an overview of your courses
2. The system will automatically select the current academic year and semester
3. If you have no courses assigned yet, you'll see an empty state message

---

## Dashboard Overview

The Professor Dashboard consists of three main tabs:

### 1. Dashboard Tab
Shows a summary of your submission status:
- **Total Courses**: Number of courses assigned to you this semester
- **Submitted Documents**: Number of documents you've uploaded
- **Pending Documents**: Number of documents still waiting for upload
- **Overdue Documents**: Number of documents past their deadline
- **Upcoming Deadlines**: List of approaching deadlines

### 2. My Courses Tab
Displays all courses assigned to you with:
- Course code and name
- Department information
- Required document types
- Submission status for each document type
- Upload/Replace buttons

### 3. File Explorer Tab
Provides a hierarchical view of:
- Your uploaded files
- Files from other professors in your department (read-only)
- Organized by academic year, semester, professor, course, and document type

---

## Viewing Assigned Courses

### Selecting Academic Year and Semester

1. At the top of the dashboard, you'll see two dropdown menus:
   - **Academic Year**: Select the year (e.g., "2024-2025")
   - **Semester**: Select the semester (First, Second, or Summer)

2. The system automatically selects the active academic year
3. When you change the academic year, the semester dropdown updates
4. When you change the semester, all data refreshes to show that semester's information

### Understanding Course Cards

Each course is displayed as a card showing:

**Course Header**:
- Course code (e.g., "CS101")
- Course name (e.g., "Introduction to Computer Science")
- Department name
- Course level (Undergraduate/Graduate)

**Document Types Section**:
Each required document type is shown with:
- Document type name (SYLLABUS, EXAM, ASSIGNMENT, etc.)
- Status badge (Not Uploaded, Uploaded, Overdue)
- Deadline date and time
- File count and size limits
- Upload or Replace button

### Empty State

If you have no courses assigned for the selected semester, you'll see:
- A message: "No courses assigned for this semester"
- An icon indicating no data
- A suggestion to contact your department if you believe this is an error

---

## Uploading Documents

### Step-by-Step Upload Process

1. **Navigate to My Courses Tab**
   - Click on "My Courses" in the tab navigation

2. **Find Your Course**
   - Locate the course card for which you want to upload documents
   - Find the document type row (e.g., SYLLABUS, EXAM)

3. **Click Upload Button**
   - Click the "Upload Files" button for the document type
   - An upload modal will appear

4. **Select Files**
   - **Option A**: Click "Choose Files" and browse your computer
   - **Option B**: Drag and drop files into the upload area

5. **Review File Validation**
   - The system will validate your files automatically
   - You'll see checkmarks for valid files
   - You'll see error messages for invalid files

6. **Add Notes (Optional)**
   - Enter any notes about your submission in the notes field
   - Example: "Updated version with corrections"

7. **Upload**
   - Click the "Upload" button
   - A progress bar will show upload progress
   - Wait for the success message

8. **Confirmation**
   - You'll see a success toast notification
   - The course card will update to show "Uploaded" status
   - The file count and upload timestamp will be displayed

### File Requirements

**Allowed File Types**:
- PDF files (.pdf)
- ZIP archives (.zip)

**File Limits** (varies by document type):
- Maximum number of files: Typically 1-5 files
- Maximum total size: Typically 10-50 MB
- Specific limits are shown in the upload modal

**File Naming Best Practices**:
- Use descriptive names (e.g., "CS101_Syllabus_Fall2024.pdf")
- Avoid special characters
- Keep names under 100 characters

### Common Upload Errors

**"Invalid file type"**
- Solution: Only PDF and ZIP files are allowed. Convert your file to PDF or ZIP.

**"File count exceeds limit"**
- Solution: You're trying to upload too many files. Check the maximum file count for this document type.

**"File size exceeds limit"**
- Solution: Your files are too large. Compress them or split into multiple submissions if allowed.

**"Network error"**
- Solution: Check your internet connection and try again.

---

## Replacing Documents

### When to Replace Documents

You might want to replace documents when:
- You uploaded the wrong file
- You need to correct errors in the document
- You want to update the document with new information
- The deadline hasn't passed yet (or you have permission to submit late)

### Step-by-Step Replace Process

1. **Navigate to My Courses Tab**
   - Find the course with the document you want to replace

2. **Locate the Document**
   - Find the document type row that shows "Uploaded" status
   - You'll see a "Replace Files" button

3. **Click Replace Files Button**
   - The upload modal will open
   - You'll see information about the current submission

4. **Select New Files**
   - Choose new files using the same methods as uploading
   - The old files will be deleted when you upload new ones

5. **Add Notes (Optional)**
   - Explain why you're replacing the files
   - Example: "Corrected typos on page 3"

6. **Confirm Replacement**
   - Click the "Replace" button
   - Wait for the success message

7. **Verification**
   - The submission timestamp will update
   - If you're replacing after the deadline, it will be marked as a late submission
   - Old files are permanently deleted

**Important**: Replacing files is permanent. The old files cannot be recovered after replacement.

---

## Using the File Explorer

### Navigating the File Explorer

1. **Access File Explorer**
   - Click on the "File Explorer" tab

2. **Select Academic Year and Semester**
   - Use the dropdowns at the top to select the period you want to view

3. **Browse Folders**
   - Click on folder names to expand them
   - Navigate through the hierarchy:
     - Academic Year → Semester → Professor → Course → Document Type → Files

4. **Breadcrumbs**
   - Use the breadcrumb trail at the top to navigate back
   - Click any level in the breadcrumb to jump to that folder

### Understanding Permissions

**Your Own Folders** (indicated with "You" label):
- ✅ Read access - You can view and download files
- ✅ Write access - You can upload and replace files
- ✅ Delete access - You can delete your own files
- Icon: Folder with a pencil/edit indicator

**Department Colleagues' Folders**:
- ✅ Read access - You can view and download files
- ❌ Write access - You cannot upload or modify files
- ❌ Delete access - You cannot delete files
- Icon: Folder with a lock/read-only indicator

**Other Departments' Folders**:
- ❌ Not visible - You cannot see folders from other departments

### Downloading Files

1. **Navigate to the File**
   - Use the file explorer to find the file you want to download

2. **Click Download Button**
   - Click the download icon next to the file name
   - The file will download to your browser's default download location

3. **Verify Download**
   - Check your downloads folder
   - Open the file to verify it downloaded correctly

### File Information

When viewing files in the explorer, you'll see:
- **File name**: Original filename
- **File size**: Size in KB or MB
- **File type**: PDF, ZIP, etc.
- **Upload date**: When the file was uploaded
- **Uploader**: Who uploaded the file (for department files)

---

## Managing Notifications

### Viewing Notifications

1. **Notification Badge**
   - Look for the bell icon in the top-right corner
   - A red badge shows the number of unseen notifications

2. **Open Notifications**
   - Click the bell icon
   - A dropdown will show your recent notifications

3. **Notification Types**
   - **Document Requests**: New documents requested by administration
   - **Deadline Reminders**: Upcoming or overdue deadlines
   - **System Announcements**: Important system messages

### Managing Notifications

**Mark as Seen**:
- Click on a notification to mark it as seen
- The badge count will decrease
- Seen notifications remain in the list but are visually dimmed

**Notification Polling**:
- The system checks for new notifications every 30 seconds
- You don't need to refresh the page to see new notifications

**Notification Details**:
- Each notification shows:
  - Message text
  - Timestamp
  - Related course or document (if applicable)
  - Link to relevant section (click to navigate)

---

## Understanding Document Status

### Status Badges

**Not Uploaded** (Gray Badge):
- No files have been submitted for this document type
- Action required: Upload files before the deadline

**Uploaded** (Green Badge):
- Files have been successfully submitted
- Shows file count and upload timestamp
- You can replace files if needed

**Overdue** (Red Badge):
- Deadline has passed and no files were submitted
- You may still be able to upload (marked as late submission)
- Contact your department if you need an extension

### Late Submissions

When you upload after the deadline:
- The submission is marked as "Late Submission"
- A warning icon appears next to the status
- The submission is still recorded but flagged as late
- Your department will be notified of the late submission

### Deadline Information

For each document type, you'll see:
- **Deadline date and time**: When the document is due
- **Time remaining**: Countdown to deadline (e.g., "3 days remaining")
- **Overdue indicator**: If the deadline has passed

**Deadline Colors**:
- Green: More than 7 days remaining
- Yellow: 3-7 days remaining
- Orange: 1-3 days remaining
- Red: Less than 24 hours or overdue

---

## Troubleshooting

### Common Issues and Solutions

#### Issue: "I can't see my courses"

**Possible Causes**:
1. No courses assigned for the selected semester
2. Wrong academic year or semester selected
3. Course assignments not yet created by administration

**Solutions**:
- Verify you've selected the correct academic year and semester
- Contact your department head to confirm course assignments
- Check if you're viewing a future semester that hasn't been set up yet

---

#### Issue: "File upload fails"

**Possible Causes**:
1. File type not allowed
2. File size too large
3. Network connection issues
4. Browser issues

**Solutions**:
- Verify file is PDF or ZIP format
- Check file size against the limits shown
- Try a different browser (Chrome, Firefox, Edge recommended)
- Check your internet connection
- Clear browser cache and try again
- Try uploading one file at a time

---

#### Issue: "I can't download a file"

**Possible Causes**:
1. File was deleted
2. Permission issues
3. Network issues

**Solutions**:
- Verify the file still exists in the file explorer
- Check if you have permission to access the file
- Try refreshing the page
- Contact support if the issue persists

---

#### Issue: "Session expired" message

**Cause**: Your login session has timed out after 30 minutes of inactivity

**Solution**:
- Click "OK" on the message
- You'll be redirected to the login page
- Log in again to continue
- Your unsaved work may be lost

**Prevention**:
- Save your work frequently
- Keep the browser tab active
- Refresh the page periodically if working on long tasks

---

#### Issue: "Notifications not updating"

**Possible Causes**:
1. Browser blocking background updates
2. Network issues
3. System maintenance

**Solutions**:
- Refresh the page manually
- Check browser console for errors (F12 key)
- Verify your internet connection
- Try a different browser
- Contact support if issue persists

---

#### Issue: "Can't see department colleagues' files"

**Possible Causes**:
1. Colleagues haven't uploaded files yet
2. Different department
3. Permission configuration issue

**Solutions**:
- Verify your colleagues have uploaded files
- Confirm you're in the same department
- Contact your system administrator if you should have access

---

### Browser Compatibility

**Recommended Browsers**:
- Google Chrome (latest version)
- Mozilla Firefox (latest version)
- Microsoft Edge (latest version)
- Safari (latest version)

**Not Recommended**:
- Internet Explorer (not supported)
- Outdated browser versions

---

### Getting Help

If you encounter issues not covered in this guide:

1. **Check System Status**
   - Look for system announcements in notifications
   - Check if maintenance is scheduled

2. **Contact Your Department**
   - Department Head: For course assignment issues
   - IT Support: For technical issues

3. **Contact System Support**
   - Email: support@archivesystem.edu
   - Phone: (555) 123-4567
   - Hours: Monday-Friday, 8:00 AM - 5:00 PM

4. **Provide Details**
   - Your name and email
   - Course code and semester
   - Description of the issue
   - Screenshots if possible
   - Error messages (exact text)

---

## Frequently Asked Questions

### General Questions

**Q: How do I know which documents I need to upload?**

A: Navigate to the "My Courses" tab. Each course card shows all required document types with their deadlines. Document types marked as "Required" must be uploaded.

---

**Q: Can I upload documents before the semester starts?**

A: Yes, you can upload documents as soon as courses are assigned to you. Early submission is encouraged.

---

**Q: What happens if I miss a deadline?**

A: You can still upload documents after the deadline, but they will be marked as "Late Submission." Your department will be notified. Contact your department head if you need an extension.

---

**Q: Can I delete uploaded documents?**

A: You cannot delete documents directly. Instead, use the "Replace Files" feature to upload new files. The old files will be automatically deleted. If you need to remove a submission entirely, contact your department head.

---

**Q: How long are my uploaded documents stored?**

A: Documents are stored permanently in the archive system. They remain accessible for historical reference and compliance purposes.

---

### File Upload Questions

**Q: Why can I only upload PDF and ZIP files?**

A: These formats are standardized for archival purposes. PDF ensures documents look the same on all devices. ZIP allows you to bundle multiple files together.

---

**Q: How do I convert my document to PDF?**

A: Most word processors have a "Save as PDF" or "Export to PDF" option. Alternatively, use online PDF converters or print to PDF.

---

**Q: Can I upload multiple files for one document type?**

A: It depends on the document type. Some allow multiple files (e.g., EXAM might allow up to 5 files), while others allow only one (e.g., SYLLABUS). The limit is shown in the upload modal.

---

**Q: What if my file is too large?**

A: Try these solutions:
- Compress images in your PDF
- Split the document into multiple files (if allowed)
- Use ZIP compression
- Reduce PDF quality using PDF tools
- Contact support if you need a higher limit

---

### Permission Questions

**Q: Why can I see other professors' files?**

A: Professors in the same department can view each other's files for reference and consistency. This is read-only access - you cannot modify their files.

---

**Q: Can professors from other departments see my files?**

A: No, only professors in your department and administrators can see your files.

---

**Q: Can I share files with specific colleagues?**

A: The system uses department-based permissions. All professors in your department can view your files. For more specific sharing, contact your system administrator.

---

### Technical Questions

**Q: What browsers are supported?**

A: Chrome, Firefox, Edge, and Safari (latest versions). Internet Explorer is not supported.

---

**Q: Can I use the system on my mobile device?**

A: The system is optimized for desktop browsers. Mobile access is possible but may have limited functionality. We recommend using a desktop or laptop computer.

---

**Q: Is my data secure?**

A: Yes, the system uses:
- Encrypted connections (HTTPS)
- Secure authentication
- Role-based access control
- Regular security audits
- Secure file storage

---

**Q: What if I forget my password?**

A: Contact your system administrator for a password reset. For security reasons, passwords cannot be recovered - they must be reset.

---

### Workflow Questions

**Q: Do I need to upload documents in a specific order?**

A: No, you can upload documents in any order. However, some documents (like syllabi) are typically due earlier in the semester.

---

**Q: Can I save a draft and finish uploading later?**

A: No, uploads must be completed in one session. Prepare all your files before starting the upload process.

---

**Q: Will I receive reminders about upcoming deadlines?**

A: Yes, the system sends notifications for upcoming deadlines. Check your notifications regularly.

---

**Q: Can I upload documents for future semesters?**

A: Only if courses have been assigned for those semesters. Contact your department head if you need to upload documents for future semesters.

---

## Best Practices

### Document Preparation

1. **Prepare files in advance**
   - Don't wait until the deadline
   - Have all files ready before starting upload

2. **Use clear file names**
   - Include course code and document type
   - Example: "CS101_Syllabus_Fall2024.pdf"

3. **Verify file content**
   - Open and review files before uploading
   - Check for completeness and accuracy

4. **Keep backups**
   - Save copies of uploaded files on your computer
   - Use cloud storage for additional backup

### Time Management

1. **Upload early**
   - Don't wait until the last minute
   - Technical issues can occur

2. **Check deadlines regularly**
   - Review the Dashboard tab weekly
   - Set personal reminders before deadlines

3. **Monitor notifications**
   - Check notifications daily
   - Respond to requests promptly

### Organization

1. **Use consistent naming**
   - Develop a naming convention for your files
   - Stick to it across all courses

2. **Add meaningful notes**
   - Use the notes field to document versions
   - Explain changes when replacing files

3. **Review before submitting**
   - Double-check file selection
   - Verify file names and sizes

---

## Glossary

**Academic Year**: A period spanning multiple semesters (e.g., 2024-2025)

**Course Assignment**: The association of a professor to a specific course within a semester

**Document Type**: A categorized type of academic document (SYLLABUS, EXAM, ASSIGNMENT, etc.)

**Document Submission**: A record of files uploaded by a professor for a specific course and document type

**File Explorer**: The hierarchical navigation interface for browsing the academic folder structure

**Late Submission**: A document uploaded after its deadline

**Semester**: One of three fixed periods within an academic year (FIRST, SECOND, SUMMER)

**Status Badge**: Visual indicator showing the submission status (Not Uploaded, Uploaded, Overdue)

---

## Appendix: Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Tab | Navigate between form fields |
| Enter | Submit form / Confirm action |
| Esc | Close modal / Cancel action |
| Ctrl+Click | Open link in new tab |
| F5 | Refresh page |
| Ctrl+F | Search on page |

---

## Document Information

**Version**: 1.0  
**Last Updated**: November 19, 2024  
**Maintained By**: Archive System Team  
**Feedback**: support@archivesystem.edu

---

**Thank you for using the Document Archiving System!**

We're committed to making document management simple and efficient. If you have suggestions for improving this guide or the system, please contact us.
