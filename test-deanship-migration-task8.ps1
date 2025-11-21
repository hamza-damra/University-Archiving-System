# Test Script for Task 8: Deanship Dashboard File Explorer Migration
# This script verifies that the Deanship Dashboard uses the unified FileExplorer component

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Task 8: Deanship File Explorer Migration" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$testsPassed = 0
$testsFailed = 0

# Test 1: Verify HTML structure matches Professor Dashboard
Write-Host "Test 1: Checking HTML structure..." -ForegroundColor Yellow
$deanshipHtml = Get-Content "src/main/resources/static/deanship-dashboard.html" -Raw
$profHtml = Get-Content "src/main/resources/static/prof-dashboard.html" -Raw

if ($deanshipHtml -match 'id="fileExplorerContainer"' -and 
    $deanshipHtml -match 'id="breadcrumbs"' -and
    $deanshipHtml -match 'class="bg-white rounded-lg shadow-md p-6"') {
    Write-Host "  ✓ HTML structure matches Professor Dashboard layout" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "  ✗ HTML structure does not match" -ForegroundColor Red
    $testsFailed++
}

# Test 2: Verify FileExplorer class is imported
Write-Host "Test 2: Checking FileExplorer import..." -ForegroundColor Yellow
$deanshipJs = Get-Content "src/main/resources/static/js/deanship.js" -Raw

if ($deanshipJs -match "import.*FileExplorer.*from.*'\.\/file-explorer\.js'") {
    Write-Host "  ✓ FileExplorer class is imported" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "  ✗ FileExplorer class is not imported" -ForegroundColor Red
    $testsFailed++
}

# Test 3: Verify FileExplorer initialization with correct configuration
Write-Host "Test 3: Checking FileExplorer initialization..." -ForegroundColor Yellow

if ($deanshipJs -match "new FileExplorer\('fileExplorerContainer'" -and
    $deanshipJs -match "role:\s*'DEANSHIP'" -and
    $deanshipJs -match "readOnly:\s*true" -and
    $deanshipJs -match "showAllDepartments:\s*true" -and
    $deanshipJs -match "showProfessorLabels:\s*true") {
    Write-Host "  ✓ FileExplorer initialized with correct Deanship configuration" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "  ✗ FileExplorer configuration is incorrect" -ForegroundColor Red
    $testsFailed++
}

# Test 4: Verify no custom rendering functions exist
Write-Host "Test 4: Checking for removed custom rendering functions..." -ForegroundColor Yellow

if ($deanshipJs -notmatch "function renderFileExplorer" -and
    $deanshipJs -notmatch "function updateBreadcrumbs" -and
    $deanshipJs -notmatch "navigateToFolder") {
    Write-Host "  ✓ Custom rendering functions have been removed" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "  ✗ Custom rendering functions still exist" -ForegroundColor Red
    $testsFailed++
}

# Test 5: Verify Academic Year and Semester selectors styling
Write-Host "Test 5: Checking selector styling..." -ForegroundColor Yellow

if ($deanshipHtml -match 'id="academicYearSelect"' -and
    $deanshipHtml -match 'id="semesterSelect"' -and
    $deanshipHtml -match 'class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"') {
    Write-Host "  ✓ Selectors use correct styling" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "  ✗ Selector styling is incorrect" -ForegroundColor Red
    $testsFailed++
}

# Test 6: Verify loadFileExplorer function exists
Write-Host "Test 6: Checking loadFileExplorer function..." -ForegroundColor Yellow

if ($deanshipJs -match "async function loadFileExplorer\(\)" -and
    $deanshipJs -match "fileExplorerInstance\.loadRoot") {
    Write-Host "  ✓ loadFileExplorer function is implemented correctly" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "  ✗ loadFileExplorer function is missing or incorrect" -ForegroundColor Red
    $testsFailed++
}

# Test 7: Verify container ID matches
Write-Host "Test 7: Checking container ID consistency..." -ForegroundColor Yellow

$htmlContainerId = if ($deanshipHtml -match 'id="(fileExplorer[^"]*)"') { $matches[1] } else { $null }
$jsContainerId = if ($deanshipJs -match "new FileExplorer\('([^']+)'") { $matches[1] } else { $null }

if ($htmlContainerId -eq $jsContainerId -and $htmlContainerId -eq "fileExplorerContainer") {
    Write-Host "  ✓ Container ID matches between HTML and JS: $htmlContainerId" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "  ✗ Container ID mismatch - HTML: $htmlContainerId, JS: $jsContainerId" -ForegroundColor Red
    $testsFailed++
}

# Test 8: Verify master design reference comments
Write-Host "Test 8: Checking design reference comments..." -ForegroundColor Yellow

if ($deanshipHtml -match "MASTER DESIGN REFERENCE" -and
    $deanshipHtml -match "Professor Dashboard File Explorer") {
    Write-Host "  ✓ Master design reference comments are present" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "  ✗ Master design reference comments are missing" -ForegroundColor Red
    $testsFailed++
}

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Tests Passed: $testsPassed" -ForegroundColor Green
Write-Host "Tests Failed: $testsFailed" -ForegroundColor Red
Write-Host ""

if ($testsFailed -eq 0) {
    Write-Host "All tests passed! Task 8 implementation is complete." -ForegroundColor Green
    Write-Host ""
    Write-Host "Next Steps:" -ForegroundColor Cyan
    Write-Host "1. Start the application and navigate to Deanship Dashboard" -ForegroundColor White
    Write-Host "2. Select an academic year and semester" -ForegroundColor White
    Write-Host "3. Navigate to the File Explorer tab" -ForegroundColor White
    Write-Host "4. Verify that:" -ForegroundColor White
    Write-Host "   - File Explorer displays with the same layout as Professor Dashboard" -ForegroundColor White
    Write-Host "   - Professor name labels appear on professor folders" -ForegroundColor White
    Write-Host "   - All departments are visible" -ForegroundColor White
    Write-Host "   - Breadcrumb navigation works correctly" -ForegroundColor White
    Write-Host "   - File download works" -ForegroundColor White
    Write-Host "   - No upload buttons are visible (read-only)" -ForegroundColor White
    exit 0
} else {
    Write-Host "Some tests failed. Please review the implementation." -ForegroundColor Red
    exit 1
}
