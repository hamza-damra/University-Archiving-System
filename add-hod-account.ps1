# Add HOD Account to Local MySQL Database
# For XAMPP MySQL with root user and no password

Write-Host "Adding HOD account to database..." -ForegroundColor Cyan

# MySQL connection details
$mysqlPath = "C:\xampp\mysql\bin\mysql.exe"
$username = "root"
$password = ""
$sqlFile = "sql/accounts/create_hod_account.sql"

# Check if MySQL executable exists
if (-not (Test-Path $mysqlPath)) {
    Write-Host "Error: MySQL not found at $mysqlPath" -ForegroundColor Red
    Write-Host "Please update the path to your XAMPP MySQL installation" -ForegroundColor Yellow
    exit 1
}

# Check if SQL file exists
if (-not (Test-Path $sqlFile)) {
    Write-Host "Error: SQL file not found at $sqlFile" -ForegroundColor Red
    exit 1
}

# Execute SQL file
Write-Host "Executing SQL script..." -ForegroundColor Yellow
& $mysqlPath -u $username -e "source $sqlFile"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✓ HOD account created successfully!" -ForegroundColor Green
    Write-Host "`nLogin Credentials:" -ForegroundColor Cyan
    Write-Host "  Email: hod@alqude.edu" -ForegroundColor White
    Write-Host "  Password: hod123" -ForegroundColor White
    Write-Host "`nYou can now log in to the HOD dashboard!" -ForegroundColor Green
} else {
    Write-Host "`nError: Failed to create HOD account" -ForegroundColor Red
    Write-Host "Please check your MySQL connection and database" -ForegroundColor Yellow
}
