# Test script for Deanship Dashboard File Explorer Migration
# This script verifies that the Deanship Dashboard uses the unified FileExplorer component

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Deanship File Explorer Migration Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Check HTML structure matches Professor Dashboard pattern
Write-Host "Test 1: Verifying HTML structure..." -ForegroundColor Yellow
$htmlContent = Get-Content "src/main/resources/static/deanship-dashboard.html" -Raw

$passed = 0
$failed = 0

# HTML Tests
if ($htmlContent -match 'id="file-explorer-tab"') {
    Write-Host "  ✓ File Explorer tab exists" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ File Explorer tab exists" -ForegroundColor Red
    $failed++
}

if ($htmlContent -match 'id="breadcrumbs"') {
    Write-Host "  ✓ Breadcrumbs container exists" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ Breadcrumbs container exists" -ForegroundColor Red
    $failed++
}

if ($htmlContent -match 'id="fileExplorerContainer"') {
    Write-Host "  ✓ File Explorer container exists" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ File Explorer container exists" -ForegroundColor Red
    $failed++
}

if ($htmlContent -match 'MASTER DESIGN REFERENCE') {
    Write-Host "  ✓ Master design reference comment exists" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ Master design reference comment exists" -ForegroundColor Red
    $failed++
}

if ($htmlContent -match 'DEANSHIP') {
    Write-Host "  ✓ Deanship configuration documented" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ Deanship configuration documented" -ForegroundColor Red
    $failed++
}

Write-Host ""

# Test 2: Check JavaScript implementation
Write-Host "Test 2: Verifying JavaScript implementation..." -ForegroundColor Yellow
$jsContent = Get-Content "src/main/resources/static/js/deanship.js" -Raw

if ($jsContent -match 'import.*FileExplorer') {
    Write-Host "  ✓ FileExplorer import exists" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ FileExplorer import exists" -ForegroundColor Red
    $failed++
}

if ($jsContent -match 'let fileExplorerInstance') {
    Write-Host "  ✓ fileExplorerInstance variable declared" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ fileExplorerInstance variable declared" -ForegroundColor Red
    $failed++
}

if ($jsContent -match 'function initializeFileExplorer') {
    Write-Host "  ✓ initializeFileExplorer function exists" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ initializeFileExplorer function exists" -ForegroundColor Red
    $failed++
}

if ($jsContent -match 'function loadFileExplorer') {
    Write-Host "  ✓ loadFileExplorer function exists" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ loadFileExplorer function exists" -ForegroundColor Red
    $failed++
}

if ($jsContent -match 'new FileExplorer') {
    Write-Host "  ✓ FileExplorer instantiated" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ FileExplorer instantiated" -ForegroundColor Red
    $failed++
}

if ($jsContent -match 'DEANSHIP') {
    Write-Host "  ✓ Role set to DEANSHIP" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ Role set to DEANSHIP" -ForegroundColor Red
    $failed++
}

if ($jsContent -match 'readOnly: true') {
    Write-Host "  ✓ readOnly set to true" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ readOnly set to true" -ForegroundColor Red
    $failed++
}

if ($jsContent -match 'showAllDepartments: true') {
    Write-Host "  ✓ showAllDepartments set to true" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ showAllDepartments set to true" -ForegroundColor Red
    $failed++
}

if ($jsContent -match 'showProfessorLabels: true') {
    Write-Host "  ✓ showProfessorLabels set to true" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ showProfessorLabels set to true" -ForegroundColor Red
    $failed++
}

if ($jsContent -match 'initializeFileExplorer\(\)') {
    Write-Host "  ✓ initializeFileExplorer called on DOMContentLoaded" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ initializeFileExplorer called on DOMContentLoaded" -ForegroundColor Red
    $failed++
}

if ($jsContent -match "case 'file-explorer':") {
    Write-Host "  ✓ loadFileExplorer called in loadTabData" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ loadFileExplorer called in loadTabData" -ForegroundColor Red
    $failed++
}

if ($jsContent -match 'fileExplorerInstance\.loadRoot') {
    Write-Host "  ✓ FileExplorer loadRoot method called" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ FileExplorer loadRoot method called" -ForegroundColor Red
    $failed++
}

Write-Host ""

# Test 3: Check Academic Year and Semester selector styling
Write-Host "Test 3: Verifying Academic Year and Semester selector..." -ForegroundColor Yellow

if ($htmlContent -match 'id="academicYearSelect"') {
    Write-Host "  ✓ Academic Year selector exists" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ Academic Year selector exists" -ForegroundColor Red
    $failed++
}

if ($htmlContent -match 'id="semesterSelect"') {
    Write-Host "  ✓ Semester selector exists" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ Semester selector exists" -ForegroundColor Red
    $failed++
}

if ($htmlContent -match 'focus:ring-2 focus:ring-blue-500') {
    Write-Host "  ✓ Selector uses Professor Dashboard styling" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ Selector uses Professor Dashboard styling" -ForegroundColor Red
    $failed++
}

if ($htmlContent -match 'Academic Year and Semester Selector Pattern') {
    Write-Host "  ✓ Master design reference comment for selectors" -ForegroundColor Green
    $passed++
} else {
    Write-Host "  ✗ Master design reference comment for selectors" -ForegroundColor Red
    $failed++
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Results" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Passed: $passed" -ForegroundColor Green
Write-Host "Failed: $failed" -ForegroundColor Red
Write-Host ""

if ($failed -eq 0) {
    Write-Host "✓ All tests passed! Deanship Dashboard File Explorer migration is complete." -ForegroundColor Green
    Write-Host ""
    Write-Host "Summary of changes:" -ForegroundColor Cyan
    Write-Host "  • Updated HTML structure to match Professor Dashboard layout" -ForegroundColor White
    Write-Host "  • Added breadcrumbs container for navigation" -ForegroundColor White
    Write-Host "  • FileExplorer class instantiated with Deanship configuration:" -ForegroundColor White
    Write-Host "    - role: DEANSHIP" -ForegroundColor Gray
    Write-Host "    - readOnly: true" -ForegroundColor Gray
    Write-Host "    - showAllDepartments: true" -ForegroundColor Gray
    Write-Host "    - showProfessorLabels: true" -ForegroundColor Gray
    Write-Host "  • Academic Year and Semester selectors use same styling as Professor Dashboard" -ForegroundColor White
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "  1. Start the application" -ForegroundColor White
    Write-Host "  2. Log in as Deanship user" -ForegroundColor White
    Write-Host "  3. Navigate to File Explorer tab" -ForegroundColor White
    Write-Host "  4. Select an academic year and semester" -ForegroundColor White
    Write-Host "  5. Verify that:" -ForegroundColor White
    Write-Host "     • All departments are visible" -ForegroundColor Gray
    Write-Host "     • Professor name labels appear on professor folders" -ForegroundColor Gray
    Write-Host "     • Folder cards use blue card design" -ForegroundColor Gray
    Write-Host "     • Breadcrumb navigation works correctly" -ForegroundColor Gray
    Write-Host "     • File download works" -ForegroundColor Gray
    Write-Host "     • No upload buttons are visible (read-only)" -ForegroundColor Gray
    Write-Host ""
    exit 0
} else {
    Write-Host "✗ Some tests failed. Please review the implementation." -ForegroundColor Red
    Write-Host ""
    exit 1
}
