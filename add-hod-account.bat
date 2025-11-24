@echo off
REM Add HOD Account to Local MySQL Database
REM For XAMPP MySQL with root user and no password

echo Adding HOD account to database...
echo.

REM Execute SQL file using MySQL
C:\xampp\mysql\bin\mysql.exe -u root < sql\accounts\create_hod_account.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✓ HOD account created successfully!
    echo.
    echo Login Credentials:
    echo   Email: hod@alqude.edu
    echo   Password: hod123
    echo.
    echo You can now log in to the HOD dashboard!
) else (
    echo.
    echo Error: Failed to create HOD account
    echo Please check your MySQL connection and database
)

pause
