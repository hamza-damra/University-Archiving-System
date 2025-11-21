# Professor Dashboard Login Guide

## Issue: Dashboard Loading Infinitely

The professor dashboard was showing "Loading..." infinitely because you were accessing it directly without logging in first.

## Root Cause

When you access `http://localhost:8080/prof-dashboard.html` directly:
1. The page loads and tries to fetch academic years from the API
2. Since you're not logged in, there's no authentication token
3. The API returns a 401 Unauthorized error
4. The page redirects to login, but the dropdown stays on "Loading..."

## Solution

### Step 1: Access the Login Page

Go to the login page first:
- `http://localhost:8080` OR
- `http://localhost:8080/index.html`

### Step 2: Find a Professor Account

Since professor names are randomly generated, you need to find an actual email address. 

**EASIEST METHOD: Use the Batch Script**

Just double-click this file:
```
query-professors.bat
```

**Alternative: Use MySQL Command Line**

1. Open Command Prompt or PowerShell
2. Run these commands:
   ```bash
   mysql -u root
   USE archive_system;
   SELECT email, first_name, last_name FROM users WHERE role = 'ROLE_PROFESSOR' LIMIT 5;
   ```
3. Copy one of the email addresses from the results

**Alternative: Use MySQL Workbench**

1. Open MySQL Workbench
2. Connect to localhost (Username: `root`, Password: blank)
3. Select database: `archive_system`
4. Click "SQL" tab and run:
   ```sql
   SELECT email, first_name, last_name, professor_id
   FROM users
   WHERE role = 'ROLE_PROFESSOR' AND is_active = true
   LIMIT 5;
   ```
5. Copy one of the email addresses from the results

**Alternative: Use PowerShell Script**

Run this PowerShell script which will query MySQL:
```powershell
.\get-professor-emails-mysql.ps1
```

**Alternative: Use HOD Account First**

If you can't find a professor email, log in as HOD first:
- Email: `hod.cs@alquds.edu`
- Password: `password123`
- Once logged in, you can see all professors in the Computer Science department and their email addresses

### Step 3: Log In

Use the credentials:
- **Email**: (one of the professor emails from above)
- **Password**: `password123`

### Step 4: Automatic Redirect

After successful login, you'll be automatically redirected to the professor dashboard at:
- `http://localhost:8080/prof-dashboard.html`

The dashboard will now load properly with:
- Academic years dropdown populated
- Semesters dropdown enabled after selecting a year
- Your courses displayed
- File explorer accessible

## What Was Fixed

I've improved the error handling in `prof.js` so that:
1. If you access the dashboard without logging in, the dropdowns show "Authentication required" instead of "Loading..."
2. The page still redirects to login, but with a clearer message
3. The authentication check now throws an error to stop further execution

## Testing the Fix

1. Clear your browser cache and localStorage:
   ```javascript
   // In browser console:
   localStorage.clear();
   ```

2. Try accessing the dashboard directly:
   - Go to `http://localhost:8080/prof-dashboard.html`
   - You should see "Authentication required" in the dropdowns
   - You'll be redirected to the login page

3. Log in properly:
   - Go to `http://localhost:8080`
   - Log in with a professor account
   - You'll be redirected to the dashboard
   - Everything should load correctly

## Quick Test Command

To find a professor email quickly, run this in your terminal:

```powershell
# This will show you the first 5 active professors
# (You'll need to have database access configured)
```

Or check the application startup logs for lines containing "Created professor users".

## Summary

**The dashboard is not broken** - you just need to log in first! The system is working as designed with proper authentication checks. Always access the application through the login page at `http://localhost:8080` rather than going directly to dashboard pages.
