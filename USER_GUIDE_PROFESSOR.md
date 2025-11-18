# Professor User Guide

## Overview

Welcome to the Document Archiving System Professor Dashboard. As a Professor, you can upload required documents for your assigned courses, view submission status, and browse files from other professors in your department.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Dashboard Overview](#dashboard-overview)
3. [Uploading Documents](#uploading-documents)
4. [Managing Submissions](#managing-submissions)
5. [File Explorer](#file-explorer)
6. [Understanding Deadlines](#understanding-deadlines)
7. [Best Practices](#best-practices)

---

## Getting Started

### Logging In

1. Navigate to the login page
2. Enter your professor credentials (email and password)
3. Click "Login"
4. You will be redirected to the Professor Dashboard

### First-Time Login

If this is your first time logging in:
1. Review your assigned courses for the current semester
2. Check the required document types for each course
3. Note the submission deadlines
4. Prepare your documents for upload

### Dashboard Layout

The Professor Dashboard provides access to:
- **Dashboard**: View your courses and submission status
- **My Courses**: Detailed view of course assignments
- **File Explorer**: Browse files from your department
- **Notifications**: View system notifications and reminders

---

## Dashboard Overview

### Selecting a Semester

Your dashboard is organized by semester. To view a different semester:

1. Use the **Academic Year** dropdown to select the year
2. Use the **Semester** dropdown to select the semester (First, Second, or Summer)
3. The dashboard will update to show your courses for that semester

### Dashboard Widgets

**My Courses**
- Shows all courses assigned to you for the selected semester
- Displays submission status for each required document type

**Submission Statistics**
- Total required documents across all your courses
- Number of documents you've submitted
- Number of missing documents
- Number of overdue documents

**Upcoming Deadlines**
- Lists documents with approaching deadlines
- Shows days remaining until deadline
- Prioritizes urgent submissions

### Course Cards

Each course is displayed as a card showing:
- **Course Code and Name**: e.g., "CS101 - Introduction to Computer Science"
- **Semester**: e.g., "First Semester 2024-2025"
- **Required Documents**: List of document types you need to submit
- **Status Indicators**: Visual indicators for each document type

**Status Indicators:**
- ✅ **Green (Uploaded)**: Document submitted successfully
- ⏳ **Yellow (Pending)**: Document not yet submitted, deadline approaching
- ⚠️ **Red (Overdue)**: Deadline passed, document not submitted

---


## Uploading Documents

### Preparing Your Documents

Before uploading, ensure your documents meet the requirements:

**File Format:**
- PDF files (.pdf) - Recommended for most documents
- ZIP files (.zip) - For multiple files or large documents

**File Size:**
- Individual file size: Check course-specific limits (typically 10-50 MB)
- Total upload size: Check course-specific limits (typically 50-100 MB)

**File Naming:**
- Use descriptive filenames (e.g., "CS101_Syllabus_Fall2024.pdf")
- Avoid special characters in filenames
- Use underscores or hyphens instead of spaces

### Uploading Files for a Document Type

**Steps:**

1. Navigate to your **Dashboard** or **My Courses**
2. Find the course you want to upload documents for
3. Locate the document type (e.g., SYLLABUS, EXAM, ASSIGNMENT)
4. Click the **"Upload"** button next to the document type

**Upload Modal:**

The upload modal will display:
- Course name and code
- Document type
- File requirements (allowed types, max count, max size)
- Drag-and-drop zone
- Notes field (optional)

**Uploading Files:**

**Method 1: Drag and Drop**
1. Drag files from your computer to the drop zone
2. Files will appear in the preview list
3. Add optional notes
4. Click **"Upload"**

**Method 2: Browse Files**
1. Click the drop zone or **"Browse"** button
2. Select files from your computer
3. Files will appear in the preview list
4. Add optional notes
5. Click **"Upload"**

**Multiple Files:**
You can upload multiple files at once (up to the maximum file count):
- Select multiple files when browsing
- Drag multiple files to the drop zone
- Files will be uploaded together as one submission

### Upload Validation

The system validates your upload:

**File Type Check:**
- Only PDF and ZIP files are allowed
- Other file types will be rejected

**File Size Check:**
- Individual files must not exceed the limit
- Total size of all files must not exceed the limit

**File Count Check:**
- Number of files must not exceed the maximum allowed

**If Validation Fails:**
- Error message will explain the issue
- Remove or replace problematic files
- Try uploading again

### Adding Notes

The notes field allows you to provide additional information:
- Explain the contents of your submission
- Note any special circumstances
- Provide context for multiple files

**Example Notes:**
```
"Syllabus includes updated course schedule and grading policy."
"Midterm and final exam papers included in separate PDFs."
"Assignment templates for all 10 weekly assignments."
```

### Upload Confirmation

After successful upload:
- Success message appears
- Document status changes to "Uploaded"
- Upload date and time are recorded
- Files are stored in the system
- HOD is notified of your submission

---

## Managing Submissions

### Viewing Your Submissions

**From Dashboard:**
1. View course cards to see submission status
2. Click on a course to see detailed submission information

**From My Courses:**
1. Navigate to **My Courses** section
2. Select a semester
3. View all your submissions for that semester

### Submission Details

Click on a submitted document to view:
- **Files Uploaded**: List of all files in the submission
- **Upload Date**: When you submitted the documents
- **File Count**: Number of files uploaded
- **Total Size**: Combined size of all files
- **Notes**: Any notes you provided
- **Status**: UPLOADED or OVERDUE (if submitted late)
- **Late Submission Flag**: Indicates if submitted after deadline

### Replacing Files

If you need to update your submission (e.g., corrected version, updated content):

**Steps:**

1. Navigate to the course and document type
2. Click **"Replace Files"** button
3. The replace modal opens (similar to upload modal)
4. Select new files to upload
5. Add notes explaining the replacement (optional)
6. Click **"Replace"**

**Important Notes:**
- Replacing files **deletes the old files** and uploads new ones
- Original upload date is preserved
- Replacement date is recorded
- Late submission flag is updated if applicable

**When to Replace:**
- Found an error in the original document
- Need to upload a newer version
- Forgot to include important information
- File was corrupted or incomplete

### Downloading Your Submissions

To download files you've uploaded:

1. Navigate to the submission
2. Click on a file in the file list
3. Click **"Download"** button
4. File will be downloaded to your computer

**Use Cases:**
- Verify what you uploaded
- Keep a local backup
- Share with students or colleagues

---


## File Explorer

The File Explorer allows you to browse and download files from other professors in your department.

### Accessing the File Explorer

1. Navigate to **File Explorer** section
2. Select an academic year and semester
3. Browse the hierarchical file structure

### What You Can See

**Your Department:**
- You can see files from all professors in your department
- This includes your own files and files from colleagues

**Other Departments:**
- You cannot see files from other departments
- Access is restricted to your department only

### File Explorer Hierarchy

```
Semester
└── Professor (Your Department)
    └── Course
        └── Document Type
            └── Files
```

### Navigating the File Explorer

**Tree View (Left Panel):**
- Shows the folder structure
- Your own folders are highlighted or marked
- Click on folders to expand/collapse
- Click on a folder to view its contents

**File List (Right Panel):**
- Shows files in the selected folder
- Displays file details:
  - Filename
  - File size
  - Upload date and time
  - Uploaded by (professor name)
  - Notes

### Permissions

**Your Own Files:**
- **Read**: View and download
- **Write**: Upload and replace
- **Delete**: Delete files (if allowed by policy)

**Other Professors' Files:**
- **Read**: View and download only
- **No Write**: Cannot modify or replace
- **No Delete**: Cannot delete

### Downloading Files from Colleagues

**To Download:**

1. Navigate to a colleague's folder
2. Browse to the document type
3. Click on a file
4. Click **"Download"** button

**Use Cases:**
- Reference examples from experienced professors
- Maintain consistency across course sections
- Learn from colleagues' teaching materials
- Collaborate on shared course content

### Breadcrumb Navigation

Use the breadcrumb trail for quick navigation:

```
Home > 2024-2025 > First Semester > Your Name > CS101 > Syllabus
```

Click any breadcrumb to jump to that level.

---

## Understanding Deadlines

### Deadline Types

**Hard Deadlines:**
- Set by Deanship for required document types
- Displayed in your dashboard
- System tracks compliance

**Soft Deadlines:**
- Departmental guidelines
- Not enforced by system
- Good practice to follow

### Deadline Notifications

The system provides deadline reminders:

**Dashboard Indicators:**
- Documents with approaching deadlines are highlighted
- Days remaining shown for each deadline
- Urgent deadlines appear in red

**Notifications:**
- System may send email reminders
- Check your notifications panel regularly

### Submitting Before Deadline

**Best Practice:**
- Submit documents at least 2-3 days before deadline
- Allows time to fix any technical issues
- Ensures compliance
- Reduces stress

### Late Submissions

**What Happens:**
- System accepts late submissions
- Submission is marked as "Late"
- Late flag appears in reports
- HOD is notified

**Consequences:**
- Depends on department policy
- May require explanation
- Could affect performance reviews

**If You'll Be Late:**
1. Contact your HOD in advance
2. Explain the situation
3. Provide expected submission date
4. Submit as soon as possible

### Deadline Extensions

**Requesting an Extension:**
- Contact your HOD or Deanship
- Provide valid reason
- Request before deadline if possible

**Note:** The system does not automatically grant extensions. Deadlines are set at the system level and require administrative action to change.

---

## Best Practices

### Document Preparation

1. **Prepare documents early**: Don't wait until the deadline
2. **Review before uploading**: Check for errors and completeness
3. **Use standard formats**: PDF is preferred for most documents
4. **Name files clearly**: Use descriptive, professional filenames
5. **Keep backups**: Maintain local copies of all uploaded documents

### Upload Strategy

1. **Upload early**: Submit well before deadlines
2. **Verify uploads**: Download and check files after uploading
3. **Add meaningful notes**: Explain your submission
4. **Check file size**: Compress large files if needed
5. **Test uploads**: Do a test upload if unsure about file format

### Submission Management

1. **Track your submissions**: Regularly check dashboard for status
2. **Respond to notifications**: Act on system reminders promptly
3. **Keep records**: Maintain a log of what you've submitted and when
4. **Update when needed**: Replace files if you find errors
5. **Communicate issues**: Contact HOD if you encounter problems

### File Organization

1. **Organize locally first**: Have a clear folder structure on your computer
2. **Use consistent naming**: Follow a naming convention for all files
3. **Version control**: Keep track of document versions
4. **Archive old files**: Maintain historical records of submissions

### Collaboration

1. **Review colleagues' files**: Learn from others in your department
2. **Share best practices**: Discuss effective document formats
3. **Maintain consistency**: Align with department standards
4. **Respect privacy**: Don't share downloaded files outside the department

---

## Troubleshooting

### Cannot Upload Files

**Problem:** Upload button doesn't work or upload fails

**Solutions:**
- Check file format (must be PDF or ZIP)
- Verify file size is within limits
- Ensure you're assigned to the course
- Check if deadline has passed (may require HOD approval)
- Try a different browser
- Clear browser cache and cookies
- Contact IT support if problem persists

### Upload Fails with Error

**Problem:** Error message during upload

**Common Errors:**

**"File type not allowed"**
- Solution: Convert file to PDF or ZIP format

**"File size exceeds limit"**
- Solution: Compress file or split into multiple files

**"Maximum file count exceeded"**
- Solution: Combine files into a ZIP or remove unnecessary files

**"Course assignment not found"**
- Solution: Verify you're assigned to the course, contact Deanship

### Cannot See My Courses

**Problem:** Dashboard shows no courses

**Solutions:**
- Verify correct semester is selected
- Check if you've been assigned courses by Deanship
- Contact HOD or Deanship to verify course assignments
- Ensure you're logged in with correct account

### Cannot Download Files

**Problem:** Download button doesn't work

**Solutions:**
- Check browser's download settings
- Disable popup blockers
- Try a different browser
- Verify file still exists in system
- Contact IT support if problem persists

### Forgot to Upload Before Deadline

**Problem:** Deadline passed, haven't uploaded

**Solutions:**
- Upload immediately (system accepts late submissions)
- Contact HOD to explain situation
- Provide reason for delay
- Follow department policy for late submissions

---

## Frequently Asked Questions

**Q: Can I upload files after the deadline?**
A: Yes, the system accepts late submissions, but they will be marked as late. Contact your HOD regarding department policy on late submissions.

**Q: How many files can I upload for each document type?**
A: The maximum file count varies by course and document type. Check the upload modal for specific limits (typically 5-10 files).

**Q: Can I delete a submission after uploading?**
A: You can replace files, which deletes the old ones. Complete deletion may require HOD or Deanship approval.

**Q: What file formats are accepted?**
A: PDF and ZIP files are accepted. Convert other formats (Word, Excel, etc.) to PDF before uploading.

**Q: Can I see files from other departments?**
A: No, you can only see files from professors in your own department.

**Q: How do I know if my upload was successful?**
A: You'll see a success message, and the document status will change to "Uploaded" in your dashboard.

**Q: Can I upload multiple versions of the same document?**
A: Use the "Replace Files" feature to update your submission. The system keeps only the latest version.

**Q: What happens if I upload the wrong file?**
A: Use the "Replace Files" feature to upload the correct file. The old file will be deleted.

**Q: How long are files stored in the system?**
A: Files are stored indefinitely for archival purposes. Contact Deanship if you need files removed.

**Q: Can I download files I uploaded in previous semesters?**
A: Yes, use the semester selector to view and download files from any semester.

---

## Support

For technical support or questions about the Professor Dashboard, contact:
- IT Support: support@university.edu
- System Administrator: admin@university.edu
- Your HOD: [Your HOD's email]
- Deanship Office: deanship@university.edu

**Last Updated:** November 18, 2025
