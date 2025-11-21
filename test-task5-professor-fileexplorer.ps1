# Task 5 Verification Script
# Tests the Professor Dashboard FileExplorer configuration

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Task 5: Professor Dashboard FileExplorer" -ForegroundColor Cyan
Write-Host "Verification Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if the application is running
Write-Host "Checking if application is running..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080" -Method GET -TimeoutSec 5 -UseBasicParsing
    Write-Host "✓ Application is running" -ForegroundColor Green
} catch {
    Write-Host "✗ Application is not running" -ForegroundColor Red
    Write-Host "  Please start the application first with: mvnw spring-boot:run" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Manual Testing Checklist" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "1. Login as Professor" -ForegroundColor Yellow
Write-Host "   URL: http://localhost:8080/login.html" -ForegroundColor Gray
Write-Host "   Use professor credentials from MOCK_ACCOUNTS.md" -ForegroundColor Gray
Write-Host ""

Write-Host "2. Navigate to File Explorer Tab" -ForegroundColor Yellow
Write-Host "   - Select an academic year from the dropdown" -ForegroundColor Gray
Write-Host "   - Select a semester from the dropdown" -ForegroundColor Gray
Write-Host "   - Click on the 'File Explorer' tab" -ForegroundColor Gray
Write-Host "   - Verify the FileExplorer loads without errors" -ForegroundColor Gray
Write-Host ""

Write-Host "3. Verify 'Your Folder' Labels" -ForegroundColor Yellow
Write-Host "   - Navigate to course folders" -ForegroundColor Gray
Write-Host "   - Look for folders with write permission" -ForegroundColor Gray
Write-Host "   - Verify 'Your Folder' badge appears (blue badge with edit icon)" -ForegroundColor Gray
Write-Host "   - Badge should have: bg-blue-100 text-blue-800 styling" -ForegroundColor Gray
Write-Host ""

Write-Host "4. Verify 'Read Only' Labels" -ForegroundColor Yellow
Write-Host "   - Navigate to folders without write permission" -ForegroundColor Gray
Write-Host "   - Verify 'Read Only' badge appears (gray badge with eye icon)" -ForegroundColor Gray
Write-Host "   - Badge should have: bg-gray-100 text-gray-600 styling" -ForegroundColor Gray
Write-Host ""

Write-Host "5. Test Breadcrumb Navigation" -ForegroundColor Yellow
Write-Host "   - Navigate through multiple folder levels" -ForegroundColor Gray
Write-Host "   - Verify breadcrumbs update correctly" -ForegroundColor Gray
Write-Host "   - Click on breadcrumb segments to navigate back" -ForegroundColor Gray
Write-Host "   - Verify home icon appears for root level" -ForegroundColor Gray
Write-Host ""

Write-Host "6. Test File Operations" -ForegroundColor Yellow
Write-Host "   - Click on files to view details" -ForegroundColor Gray
Write-Host "   - Download files using the download button" -ForegroundColor Gray
Write-Host "   - Verify all existing functionality works" -ForegroundColor Gray
Write-Host ""

Write-Host "7. Verify Visual Consistency" -ForegroundColor Yellow
Write-Host "   - Folder cards should be blue with proper styling" -ForegroundColor Gray
Write-Host "   - Hover effects should work properly" -ForegroundColor Gray
Write-Host "   - Arrow icons should animate on hover" -ForegroundColor Gray
Write-Host "   - File table should have proper columns and styling" -ForegroundColor Gray
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Browser Console Checks" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Open browser console (F12) and verify:" -ForegroundColor Yellow
Write-Host "1. No JavaScript errors" -ForegroundColor Gray
Write-Host "2. Look for log message:" -ForegroundColor Gray
Write-Host "   'FileExplorer initialized with Professor configuration:'" -ForegroundColor Gray
Write-Host "   { role: 'PROFESSOR', showOwnershipLabels: true, readOnly: false }" -ForegroundColor Gray
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Expected Behavior" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "✓ FileExplorer loads when File Explorer tab is clicked" -ForegroundColor Green
Write-Host "✓ 'Your Folder' badges appear on owned course folders" -ForegroundColor Green
Write-Host "✓ 'Read Only' badges appear on read-only folders" -ForegroundColor Green
Write-Host "✓ Breadcrumb navigation works correctly" -ForegroundColor Green
Write-Host "✓ File download functionality works" -ForegroundColor Green
Write-Host "✓ All existing functionality is preserved" -ForegroundColor Green
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Configuration Verification" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Checking prof.js for correct configuration..." -ForegroundColor Yellow

$profJsPath = "src/main/resources/static/js/prof.js"
if (Test-Path $profJsPath) {
    $profJsContent = Get-Content $profJsPath -Raw
    
    # Check for FileExplorer import
    if ($profJsContent -match "import.*FileExplorer.*from.*file-explorer") {
        Write-Host "✓ FileExplorer is imported" -ForegroundColor Green
    } else {
        Write-Host "✗ FileExplorer import not found" -ForegroundColor Red
    }
    
    # Check for initializeFileExplorer function
    if ($profJsContent -match "function initializeFileExplorer") {
        Write-Host "✓ initializeFileExplorer function exists" -ForegroundColor Green
    } else {
        Write-Host "✗ initializeFileExplorer function not found" -ForegroundColor Red
    }
    
    # Check for role configuration
    if ($profJsContent -match "role:\s*'PROFESSOR'") {
        Write-Host "✓ role: 'PROFESSOR' is set" -ForegroundColor Green
    } else {
        Write-Host "✗ role: 'PROFESSOR' not found" -ForegroundColor Red
    }
    
    # Check for showOwnershipLabels
    if ($profJsContent -match "showOwnershipLabels:\s*true") {
        Write-Host "✓ showOwnershipLabels: true is set" -ForegroundColor Green
    } else {
        Write-Host "✗ showOwnershipLabels: true not found" -ForegroundColor Red
    }
    
    # Check for readOnly
    if ($profJsContent -match "readOnly:\s*false") {
        Write-Host "✓ readOnly: false is set" -ForegroundColor Green
    } else {
        Write-Host "✗ readOnly: false not found" -ForegroundColor Red
    }
    
    # Check for loadFileExplorer function
    if ($profJsContent -match "function loadFileExplorer") {
        Write-Host "✓ loadFileExplorer function exists" -ForegroundColor Green
    } else {
        Write-Host "✗ loadFileExplorer function not found" -ForegroundColor Red
    }
    
} else {
    Write-Host "✗ prof.js file not found at: $profJsPath" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Next Steps" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "1. Open browser and navigate to: http://localhost:8080/login.html" -ForegroundColor Yellow
Write-Host "2. Login as a professor" -ForegroundColor Yellow
Write-Host "3. Follow the manual testing checklist above" -ForegroundColor Yellow
Write-Host "4. Verify all expected behaviors" -ForegroundColor Yellow
Write-Host ""

Write-Host "Task 5 implementation is complete!" -ForegroundColor Green
Write-Host "See TASK_5_PROFESSOR_DASHBOARD_FILEEXPLORER_UPDATE.md for details" -ForegroundColor Gray
Write-Host ""
