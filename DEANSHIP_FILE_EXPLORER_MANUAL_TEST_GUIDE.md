# Deanship File Explorer Manual Testing Guide
## Quick Visual Verification Checklist

**Purpose:** Verify Deanship Dashboard File Explorer matches Professor Dashboard design  
**Time Required:** 5-10 minutes  
**Prerequisites:** Server running, test data available

---

## Setup

1. **Start the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

2. **Open two browser tabs:**
   - Tab 1: http://localhost:8080/prof-dashboard.html
   - Tab 2: http://localhost:8080/deanship-dashboard.html

3. **Login credentials:**
   - Professor: `prof1@alquds.edu` / `password123`
   - Deanship: `dean@alquds.edu` / `password123`

---

## Visual Comparison Checklist

### 1. Academic Year and Semester Selectors

**Location:** Top of page, below navigation tabs

**Check:**
- [ ] Both dashboards have identical selector layout
- [ ] Labels use same font size and color (text-sm, font-medium, text-gray-700)
- [ ] Dropdowns have same width and styling
- [ ] Spacing between selectors is identical
- [ ] Focus states match (blue ring)

**Expected:**
```
Academic Year: [Dropdown]    Semester: [Dropdown]
```

---

### 2. File Explorer Container

**Location:** File Explorer tab

**Check:**
- [ ] Both use white background with rounded corners
- [ ] Both have same padding (p-6)
- [ ] Both have same shadow (shadow-md)
- [ ] Header text size and color match

---

### 3. Breadcrumb Navigation

**Location:** Top of File Explorer content

**Check:**
- [ ] Both use gray background (bg-gray-50)
- [ ] Both have same border (border-b border-gray-200)
- [ ] Home icon appears at root level
- [ ] Separator arrows are identical
- [ ] Text color and size match
- [ ] Hover effects match (text-blue-600 hover:text-blue-800)

**Expected:**
```
🏠 Home > 2024-2025 > First > Prof. Name > Course Code
```

---

### 4. Folder Cards

**Location:** Main content area

**Check:**
- [ ] Both use blue background (bg-blue-50)
- [ ] Both have blue border (border-blue-200)
- [ ] Folder icon color matches (text-blue-600)
- [ ] Hover effect changes background (hover:bg-blue-100)
- [ ] Arrow icon animates on hover (translate-x-1)
- [ ] Card padding is identical (p-4)
- [ ] Border radius matches (rounded-lg)

**Expected Design:**
```
┌─────────────────────────────────────────────────┐
│ 📁  Course Name                            →    │
│     Course Code                                 │
│     [Your Folder] or [Prof. Name]               │
└─────────────────────────────────────────────────┘
```

**Colors:**
- Background: Light blue (#EFF6FF)
- Border: Blue (#BFDBFE)
- Icon: Blue (#2563EB)
- Hover: Slightly darker blue (#DBEAFE)

---

### 5. Role-Specific Labels

**Professor Dashboard:**
- [ ] "Your Folder" badge on own courses
- [ ] Badge color: bg-blue-100 text-blue-800
- [ ] Edit icon present

**Deanship Dashboard:**
- [ ] Professor name displayed on folders
- [ ] Badge color: bg-blue-100 text-blue-800
- [ ] No edit icon (read-only)

**Expected Badge Style:**
```
[📝 Your Folder]  or  [Prof. John Smith]
```

---

### 6. File Table

**Location:** When viewing files in a folder

**Check:**
- [ ] Both have same column headers
- [ ] Column order matches: Name, Size, Uploaded, Uploader, Actions
- [ ] Header styling matches (bg-gray-50, text-xs, uppercase)
- [ ] Row hover effect matches (hover:bg-gray-50)
- [ ] File icons match (PDF=red, ZIP=amber, etc.)
- [ ] Metadata badges match (bg-gray-100, text-gray-700)

**Expected Columns:**
```
| Name          | Size    | Uploaded    | Uploader    | Actions |
|---------------|---------|-------------|-------------|---------|
| 📄 file.pdf   | 2.5 MB  | Nov 15 2024 | Prof. Name  | 👁 ⬇   |
```

---

### 7. Action Buttons

**Professor Dashboard:**
- [ ] Upload button visible (blue)
- [ ] Replace button visible (yellow)
- [ ] Download button visible (blue)
- [ ] View button visible (gray)

**Deanship Dashboard:**
- [ ] NO upload button
- [ ] NO replace button
- [ ] Download button visible (blue)
- [ ] View button visible (gray)

**Expected (Deanship):**
```
Actions: [👁 View] [⬇ Download]
```

---

### 8. Empty States

**Location:** When folder is empty

**Check:**
- [ ] Both show folder icon
- [ ] Icon color matches (text-gray-300)
- [ ] Text color matches (text-gray-500)
- [ ] Message text matches
- [ ] Vertical spacing matches (py-8)

**Expected:**
```
    📁
    
This folder is empty
```

---

### 9. Loading States

**Location:** While data is loading

**Check:**
- [ ] Both show skeleton loaders
- [ ] Skeleton color matches (bg-gray-200)
- [ ] Animation matches (animate-pulse)
- [ ] Number of skeleton items matches

**Expected:**
```
▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
```

---

### 10. Responsive Behavior

**Check at different screen sizes:**

**Desktop (>768px):**
- [ ] Tree view and file list side-by-side
- [ ] Selectors in one row
- [ ] Folder cards in grid layout

**Tablet (768px):**
- [ ] Tree view and file list stack
- [ ] Selectors in one row
- [ ] Folder cards in single column

**Mobile (<640px):**
- [ ] All elements stack vertically
- [ ] Selectors stack
- [ ] Folder cards full width

---

## Functional Testing

### 1. Navigation Flow

**Test Steps:**
1. Select academic year
2. Select semester
3. Click on a professor folder
4. Click on a course folder
5. Click on a document type folder
6. View files

**Verify:**
- [ ] Breadcrumb updates at each step
- [ ] Back navigation works via breadcrumb
- [ ] Folder cards load correctly
- [ ] File table displays correctly

### 2. All Departments Access (Deanship Only)

**Test Steps:**
1. Login as Deanship
2. Navigate to File Explorer
3. Select academic year and semester
4. Count visible professor folders

**Verify:**
- [ ] Can see professors from ALL departments
- [ ] More professors visible than HOD dashboard
- [ ] No department filtering applied

**Compare with HOD:**
1. Login as HOD
2. Navigate to File Explorer
3. Count visible professor folders
4. Verify Deanship sees MORE professors

### 3. Read-Only Verification (Deanship Only)

**Test Steps:**
1. Navigate to a course folder
2. Look for action buttons
3. Try to upload a file (should not be possible)

**Verify:**
- [ ] NO "Upload Files" button
- [ ] NO "Replace File" button
- [ ] NO "Delete" button
- [ ] ONLY "Download" and "View" buttons
- [ ] Cannot drag-and-drop files

### 4. File Download

**Test Steps:**
1. Navigate to a folder with files
2. Click "Download" button
3. Wait for download to complete
4. Open downloaded file

**Verify:**
- [ ] Download starts immediately
- [ ] File downloads successfully
- [ ] File opens correctly
- [ ] File content is correct

### 5. Professor Labels (Deanship Only)

**Test Steps:**
1. Navigate to root level (after selecting semester)
2. Look at professor folder cards
3. Check for professor name labels

**Verify:**
- [ ] Professor name displays on each folder
- [ ] Label uses badge styling
- [ ] Label color matches Professor Dashboard badges
- [ ] Department name included (if available)

---

## Color Reference

Use this reference to verify exact colors:

### Blue (Primary)
- `bg-blue-50`: #EFF6FF (folder card background)
- `bg-blue-100`: #DBEAFE (folder card hover)
- `border-blue-200`: #BFDBFE (folder card border)
- `text-blue-600`: #2563EB (folder icon, links)
- `text-blue-800`: #1E40AF (badge text)

### Gray (Neutral)
- `bg-gray-50`: #F9FAFB (breadcrumb background, table header)
- `bg-gray-100`: #F3F4F6 (metadata badges)
- `border-gray-200`: #E5E7EB (borders)
- `text-gray-500`: #6B7280 (secondary text)
- `text-gray-600`: #4B5563 (labels)
- `text-gray-700`: #374151 (badge text)
- `text-gray-900`: #111827 (primary text)

---

## Common Issues and Solutions

### Issue: Folder cards look different
**Check:**
- Background color (should be bg-blue-50)
- Border color (should be border-blue-200)
- Padding (should be p-4)
- Border radius (should be rounded-lg)

### Issue: Breadcrumbs don't match
**Check:**
- Background color (should be bg-gray-50)
- Border (should be border-b border-gray-200)
- Text size (should be text-sm)
- Icon size (should be w-5 h-5)

### Issue: Selectors look different
**Check:**
- Label styling (text-sm font-medium text-gray-700)
- Dropdown styling (w-full px-3 py-2 border border-gray-300 rounded-md)
- Focus ring (focus:ring-2 focus:ring-blue-500)

### Issue: No professor labels showing
**Check:**
- JavaScript configuration has `showProfessorLabels: true`
- Metadata includes professor name
- Badge styling is applied

### Issue: Upload buttons visible in Deanship
**Check:**
- JavaScript configuration has `readOnly: true`
- FileExplorer class respects readOnly flag
- No custom upload buttons added

---

## Sign-Off

After completing all checks, sign off on the testing:

**Tester Name:** ___________________________  
**Date:** ___________________________  
**Result:** ☐ PASS  ☐ FAIL  ☐ NEEDS REVIEW  

**Notes:**
_____________________________________________
_____________________________________________
_____________________________________________

**Issues Found:**
_____________________________________________
_____________________________________________
_____________________________________________

---

## Quick Reference: Side-by-Side Comparison

| Feature | Professor Dashboard | Deanship Dashboard |
|---------|-------------------|-------------------|
| **Access Scope** | Own courses only | All departments |
| **Upload Files** | ✅ Yes | ❌ No |
| **Download Files** | ✅ Yes | ✅ Yes |
| **Replace Files** | ✅ Yes | ❌ No |
| **Delete Files** | ✅ Yes | ❌ No |
| **Folder Labels** | "Your Folder" | Professor names |
| **Visual Design** | Blue cards | Blue cards (same) |
| **Breadcrumbs** | Yes | Yes (same) |
| **Selectors** | Yes | Yes (same) |

---

**Testing Complete!** 🎉

If all checks pass, the Deanship File Explorer is ready for production deployment.
