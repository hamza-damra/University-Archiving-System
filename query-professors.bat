@echo off
REM Batch script to query MySQL for professor emails
REM Usage: Just double-click this file

echo ========================================
echo   Finding Professor Email Addresses
echo   (MySQL Database)
echo ========================================
echo.

REM Change this if your database name is different
set DB_NAME=archive_system

echo Querying MySQL database: %DB_NAME%
echo.

REM Query MySQL (assuming mysql is in PATH)
mysql -u root -e "USE %DB_NAME%; SELECT email, first_name, last_name FROM users WHERE role = 'ROLE_PROFESSOR' AND is_active = true LIMIT 10;"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Could not connect to MySQL
    echo.
    echo Make sure:
    echo   1. MySQL is running
    echo   2. Database '%DB_NAME%' exists
    echo   3. mysql command is in your PATH
    echo.
    echo Alternative: Run this command manually:
    echo   mysql -u root
    echo   USE %DB_NAME%;
    echo   SELECT email FROM users WHERE role = 'ROLE_PROFESSOR' LIMIT 5;
    echo.
) else (
    echo.
    echo ========================================
    echo To log in:
    echo   1. Copy any email from above
    echo   2. Go to http://localhost:8080
    echo   3. Password: password123
    echo ========================================
)

echo.
pause
