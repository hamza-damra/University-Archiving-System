# Quick Fix: Professor Dashboard Login Issue

## Problem
You tried to log in with `prof1@alquds.edu` but that account doesn't exist. The mock data generator creates professors with randomly generated Arabic names.

## Solution (3 Easy Steps)

### Step 1: Find a Real Professor Email

**Option A: Use the Batch Script (Easiest)**
Just double-click: `query-professors.bat`

**Option B: Use MySQL Command Line**
```bash
mysql -u root
USE archive_system;
SELECT email, first_name, last_name 
FROM users 
WHERE role = 'ROLE_PROFESSOR' 
LIMIT 5;
```

**Option C: Use MySQL Workbench**
1. Open MySQL Workbench
2. Connect to localhost (user: root, password: blank)
3. Select database: `archive_system`
4. Run the query above

**Option D: Use PowerShell Script**
```powershell
.\get-professor-emails-mysql.ps1
```

### Step 2: Log In

1. Go to: `http://localhost:8080`
2. Enter the email you copied
3. Password: `password123`
4. Click "Sign In"

### Step 3: Success!

You'll be automatically redirected to the professor dashboard, and it will load properly with:
- Academic years populated
- Semesters available
- Your courses displayed

## Alternative: Use HOD Account

If H2 Console doesn't work, use the HOD account:
- Email: `hod.cs@alquds.edu`
- Password: `password123`

Once logged in as HOD, you can see all professor emails in the department.

## Why This Happened

The professor dashboard requires authentication. When you accessed it directly without logging in:
1. The page tried to load academic years
2. No authentication token was present
3. The API returned 401 Unauthorized
4. The dropdown stayed on "Loading..."

The fix I made ensures the dropdown now shows "Authentication required" instead of "Loading..." when accessed without login.

## Files Created to Help You

1. `find-professors.ps1` - Interactive script to help find professor emails
2. `find-professor-emails.sql` - SQL query to run in H2 Console
3. `PROFESSOR_DASHBOARD_LOGIN_GUIDE.md` - Detailed guide

## Quick Test

Run this to open H2 Console and get instructions:
```powershell
.\find-professors.ps1
```
