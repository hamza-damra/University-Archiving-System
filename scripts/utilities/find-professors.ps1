# Script to help find professor email addresses
# This script provides instructions and opens the H2 console

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Finding Professor Email Addresses" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "The mock data generator creates professors with randomly generated Arabic names." -ForegroundColor Yellow
Write-Host "Email format: prof.{firstname}.{lastname}@alquds.edu" -ForegroundColor Yellow
Write-Host ""

Write-Host "To find actual professor emails, follow these steps:" -ForegroundColor Green
Write-Host ""

Write-Host "Option 1: Use H2 Console (Recommended)" -ForegroundColor Cyan
Write-Host "  1. Make sure your Spring Boot application is running" -ForegroundColor White
Write-Host "  2. Open your browser and go to: http://localhost:8080/h2-console" -ForegroundColor White
Write-Host "  3. Use these connection settings:" -ForegroundColor White
Write-Host "     - JDBC URL: jdbc:h2:mem:testdb" -ForegroundColor Gray
Write-Host "     - Username: sa" -ForegroundColor Gray
Write-Host "     - Password: (leave empty)" -ForegroundColor Gray
Write-Host "  4. Click 'Connect'" -ForegroundColor White
Write-Host "  5. Run this query:" -ForegroundColor White
Write-Host ""
Write-Host "     SELECT email, first_name, last_name, professor_id" -ForegroundColor Yellow
Write-Host "     FROM users" -ForegroundColor Yellow
Write-Host "     WHERE role = 'ROLE_PROFESSOR' AND is_active = true" -ForegroundColor Yellow
Write-Host "     LIMIT 5;" -ForegroundColor Yellow
Write-Host ""

Write-Host "Option 2: Check Application Logs" -ForegroundColor Cyan
Write-Host "  Look for log entries when the application starts that show created users" -ForegroundColor White
Write-Host ""

Write-Host "Option 3: Use Default Test Accounts" -ForegroundColor Cyan
Write-Host "  Try these common test accounts (if they exist):" -ForegroundColor White
Write-Host "  - hod.cs@alquds.edu (HOD account - can view professors)" -ForegroundColor Gray
Write-Host "  - dean@alquds.edu (Dean account - can view all)" -ForegroundColor Gray
Write-Host "  Password for all accounts: password123" -ForegroundColor Gray
Write-Host ""

Write-Host "Would you like to open the H2 Console now? (Y/N): " -ForegroundColor Green -NoNewline
$response = Read-Host

if ($response -eq 'Y' -or $response -eq 'y') {
    Write-Host ""
    Write-Host "Opening H2 Console in your default browser..." -ForegroundColor Green
    Start-Process "http://localhost:8080/h2-console"
    Write-Host ""
    Write-Host "Remember to use:" -ForegroundColor Yellow
    Write-Host "  JDBC URL: jdbc:h2:mem:testdb" -ForegroundColor White
    Write-Host "  Username: sa" -ForegroundColor White
    Write-Host "  Password: (empty)" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "No problem! You can manually open: http://localhost:8080/h2-console" -ForegroundColor White
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Once you have a professor email:" -ForegroundColor Green
Write-Host "  1. Go to http://localhost:8080" -ForegroundColor White
Write-Host "  2. Enter the professor email" -ForegroundColor White
Write-Host "  3. Enter password: password123" -ForegroundColor White
Write-Host "  4. Click 'Sign In'" -ForegroundColor White
Write-Host "  5. You'll be redirected to the professor dashboard" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
