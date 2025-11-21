# PowerShell script to query MySQL and get professor emails
# This script connects to your MySQL database and retrieves professor accounts

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Finding Professor Email Addresses" -ForegroundColor Cyan
Write-Host "  (MySQL Database)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Database configuration
$mysqlHost = "localhost"
$mysqlUser = "root"
$mysqlPassword = ""
$mysqlDatabase = "archive_system"  # Change this if your database name is different

Write-Host "Connecting to MySQL database..." -ForegroundColor Yellow
Write-Host "Host: $mysqlHost" -ForegroundColor Gray
Write-Host "User: $mysqlUser" -ForegroundColor Gray
Write-Host "Database: $mysqlDatabase" -ForegroundColor Gray
Write-Host ""

# Check if MySQL is accessible
try {
    # Try to query the database using mysql command
    $query = "SELECT email, first_name, last_name, professor_id FROM users WHERE role = 'ROLE_PROFESSOR' AND is_active = true LIMIT 10;"
    
    Write-Host "Executing query..." -ForegroundColor Yellow
    Write-Host ""
    
    # Execute MySQL query
    $result = & mysql -h $mysqlHost -u $mysqlUser --password="" -D $mysqlDatabase -e $query 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Professor Accounts Found:" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host $result
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "To log in:" -ForegroundColor Green
        Write-Host "  1. Copy any email from the list above" -ForegroundColor White
        Write-Host "  2. Go to http://localhost:8080" -ForegroundColor White
        Write-Host "  3. Enter the email" -ForegroundColor White
        Write-Host "  4. Password: password123" -ForegroundColor White
        Write-Host "  5. Click 'Sign In'" -ForegroundColor White
    } else {
        throw "MySQL query failed"
    }
} catch {
    Write-Host "Could not connect to MySQL using command line." -ForegroundColor Red
    Write-Host ""
    Write-Host "Alternative methods to find professor emails:" -ForegroundColor Yellow
    Write-Host ""
    
    Write-Host "Method 1: Use MySQL Workbench" -ForegroundColor Cyan
    Write-Host "  1. Open MySQL Workbench" -ForegroundColor White
    Write-Host "  2. Connect to localhost with user 'root' and blank password" -ForegroundColor White
    Write-Host "  3. Select database: $mysqlDatabase" -ForegroundColor White
    Write-Host "  4. Run this query:" -ForegroundColor White
    Write-Host ""
    Write-Host "     SELECT email, first_name, last_name" -ForegroundColor Yellow
    Write-Host "     FROM users" -ForegroundColor Yellow
    Write-Host "     WHERE role = 'ROLE_PROFESSOR'" -ForegroundColor Yellow
    Write-Host "     LIMIT 10;" -ForegroundColor Yellow
    Write-Host ""
    
    Write-Host "Method 2: Use phpMyAdmin" -ForegroundColor Cyan
    Write-Host "  1. Open phpMyAdmin (usually at http://localhost/phpmyadmin)" -ForegroundColor White
    Write-Host "  2. Select database: $mysqlDatabase" -ForegroundColor White
    Write-Host "  3. Click 'SQL' tab" -ForegroundColor White
    Write-Host "  4. Paste and run the query above" -ForegroundColor White
    Write-Host ""
    
    Write-Host "Method 3: Use MySQL Command Line" -ForegroundColor Cyan
    Write-Host "  Run these commands:" -ForegroundColor White
    Write-Host ""
    Write-Host "     mysql -u root" -ForegroundColor Yellow
    Write-Host "     USE $mysqlDatabase;" -ForegroundColor Yellow
    Write-Host "     SELECT email FROM users WHERE role = 'ROLE_PROFESSOR' LIMIT 5;" -ForegroundColor Yellow
    Write-Host ""
    
    Write-Host "Method 4: Use HOD Account" -ForegroundColor Cyan
    Write-Host "  Log in as HOD to see all professors:" -ForegroundColor White
    Write-Host "  - Email: hod.cs@alquds.edu" -ForegroundColor Gray
    Write-Host "  - Password: password123" -ForegroundColor Gray
    Write-Host ""
}

Write-Host ""
Write-Host "Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
