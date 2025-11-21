# Test Frontend Integration Script
# Run this after starting the Spring Boot application

Write-Host "=== Al-Quds University Archiving System - Frontend Test ===" -ForegroundColor Cyan
Write-Host ""

# Check if server is running
Write-Host "1. Checking if Spring Boot server is running..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/index.html" -Method Head -TimeoutSec 5 -ErrorAction Stop
    Write-Host "   ✓ Server is running on http://localhost:8080" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Server is not responding on http://localhost:8080" -ForegroundColor Red
    Write-Host "   Please start the Spring Boot application first:" -ForegroundColor Red
    Write-Host "   mvn spring-boot:run" -ForegroundColor White
    exit 1
}

Write-Host ""

# Check static files
Write-Host "2. Checking static files..." -ForegroundColor Yellow

$files = @(
    "/index.html",
    "/hod-dashboard.html",
    "/prof-dashboard.html",
    "/css/custom.css",
    "/js/api.js",
    "/js/ui.js",
    "/js/auth.js",
    "/js/hod.js",
    "/js/prof.js"
)

$allFilesOk = $true
foreach ($file in $files) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080$file" -Method Head -TimeoutSec 5 -ErrorAction Stop
        Write-Host "   ✓ $file" -ForegroundColor Green
    } catch {
        Write-Host "   ✗ $file - Not found" -ForegroundColor Red
        $allFilesOk = $false
    }
}

Write-Host ""

if ($allFilesOk) {
    Write-Host "3. All files are accessible!" -ForegroundColor Green
    Write-Host ""
    Write-Host "=== Next Steps ===" -ForegroundColor Cyan
    Write-Host "1. Open your browser and navigate to:" -ForegroundColor White
    Write-Host "   http://localhost:8080/index.html" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "2. Login with your credentials:" -ForegroundColor White
    Write-Host "   - HOD users will be redirected to HOD Dashboard" -ForegroundColor Gray
    Write-Host "   - Professor users will be redirected to Professor Dashboard" -ForegroundColor Gray
    Write-Host ""
    Write-Host "3. Test the following features:" -ForegroundColor White
    Write-Host "   HOD Dashboard:" -ForegroundColor Yellow
    Write-Host "   - Add/Edit/Delete professors" -ForegroundColor Gray
    Write-Host "   - Create document requests" -ForegroundColor Gray
    Write-Host "   - View request reports" -ForegroundColor Gray
    Write-Host ""
    Write-Host "   Professor Dashboard:" -ForegroundColor Yellow
    Write-Host "   - View assigned requests" -ForegroundColor Gray
    Write-Host "   - Upload documents" -ForegroundColor Gray
    Write-Host "   - Check notifications" -ForegroundColor Gray
    Write-Host ""
} else {
    Write-Host "3. Some files are missing or inaccessible!" -ForegroundColor Red
    Write-Host "   Please ensure all files are in src/main/resources/static/" -ForegroundColor Red
}

Write-Host "=== Troubleshooting ===" -ForegroundColor Cyan
Write-Host "If you encounter issues:" -ForegroundColor White
Write-Host "1. Check the browser console for JavaScript errors" -ForegroundColor Gray
Write-Host "2. Check the Network tab for failed API requests" -ForegroundColor Gray
Write-Host "3. Verify CORS is enabled in SecurityConfig" -ForegroundColor Gray
Write-Host "4. Check backend logs for API errors" -ForegroundColor Gray
Write-Host ""
Write-Host "For more information, see:" -ForegroundColor White
Write-Host "src/main/resources/static/README.md" -ForegroundColor Yellow
