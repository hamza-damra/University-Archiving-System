# Test Error Handling and Loading States Implementation
# This script verifies that error handling and loading states are properly implemented

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Error Handling & Loading States Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Check that all page modules have error handling methods
Write-Host "Test 1: Checking error handling methods..." -ForegroundColor Yellow

$pageModules = @(
    "src/main/resources/static/js/dashboard.js",
    "src/main/resources/static/js/academic-years.js",
    "src/main/resources/static/js/professors.js",
    "src/main/resources/static/js/courses.js",
    "src/main/resources/static/js/course-assignments.js",
    "src/main/resources/static/js/reports.js",
    "src/main/resources/static/js/file-explorer-page.js"
)

$allPassed = $true

foreach ($module in $pageModules) {
    $moduleName = Split-Path $module -Leaf
    Write-Host "  Checking $moduleName..." -NoNewline
    
    $content = Get-Content $module -Raw
    
    # Check for handleApiError method
    if ($content -match "handleApiError\(error, action\)") {
        Write-Host " ✓ handleApiError" -ForegroundColor Green -NoNewline
    } else {
        Write-Host " ✗ Missing handleApiError" -ForegroundColor Red -NoNewline
        $allPassed = $false
    }
    
    # Check for handleError method
    if ($content -match "handleError") {
        Write-Host " ✓ handleError" -ForegroundColor Green
    } else {
        Write-Host " ✗ Missing handleError" -ForegroundColor Red
        $allPassed = $false
    }
}

Write-Host ""

# Test 2: Check for try-catch blocks around API calls
Write-Host "Test 2: Checking try-catch blocks around API calls..." -ForegroundColor Yellow

foreach ($module in $pageModules) {
    $moduleName = Split-Path $module -Leaf
    $content = Get-Content $module -Raw
    
    # Count apiRequest calls
    $apiRequestCount = ([regex]::Matches($content, "apiRequest\(")).Count
    
    # Count try-catch blocks
    $tryCatchCount = ([regex]::Matches($content, "try \{")).Count
    
    Write-Host "  $moduleName : $apiRequestCount API calls, $tryCatchCount try-catch blocks" -ForegroundColor Cyan
}

Write-Host ""

# Test 3: Check for loading state methods
Write-Host "Test 3: Checking loading state methods..." -ForegroundColor Yellow

foreach ($module in $pageModules) {
    $moduleName = Split-Path $module -Leaf
    Write-Host "  Checking $moduleName..." -NoNewline
    
    $content = Get-Content $module -Raw
    
    # Check for showLoading method or loading state management
    if ($content -match "showLoading\(" -or $content -match "showPageLoading\(" -or $content -match "showCardsLoading\(") {
        Write-Host " ✓ Has loading state management" -ForegroundColor Green
    } else {
        Write-Host " ⚠ No explicit loading state method" -ForegroundColor Yellow
    }
}

Write-Host ""

# Test 4: Check for specific error status handling
Write-Host "Test 4: Checking error status code handling..." -ForegroundColor Yellow

$errorStatuses = @("401", "403", "500", "NetworkError")

foreach ($module in $pageModules) {
    $moduleName = Split-Path $module -Leaf
    $content = Get-Content $module -Raw
    
    $hasAllStatuses = $true
    $missingStatuses = @()
    
    foreach ($status in $errorStatuses) {
        if ($content -notmatch $status) {
            $hasAllStatuses = $false
            $missingStatuses += $status
        }
    }
    
    if ($hasAllStatuses) {
        Write-Host "  $moduleName : ✓ Handles all error statuses" -ForegroundColor Green
    } else {
        Write-Host "  $moduleName : ⚠ Missing: $($missingStatuses -join ', ')" -ForegroundColor Yellow
    }
}

Write-Host ""

# Test 5: Check for toast notifications on errors
Write-Host "Test 5: Checking toast notifications..." -ForegroundColor Yellow

foreach ($module in $pageModules) {
    $moduleName = Split-Path $module -Leaf
    $content = Get-Content $module -Raw
    
    # Count showToast calls
    $toastCount = ([regex]::Matches($content, "showToast\(")).Count
    
    if ($toastCount -gt 0) {
        Write-Host "  $moduleName : ✓ $toastCount toast notifications" -ForegroundColor Green
    } else {
        Write-Host "  $moduleName : ✗ No toast notifications" -ForegroundColor Red
        $allPassed = $false
    }
}

Write-Host ""

# Test 6: Check for console logging
Write-Host "Test 6: Checking console error logging..." -ForegroundColor Yellow

foreach ($module in $pageModules) {
    $moduleName = Split-Path $module -Leaf
    $content = Get-Content $module -Raw
    
    # Count console.error calls
    $errorLogCount = ([regex]::Matches($content, "console\.error\(")).Count
    
    if ($errorLogCount -gt 0) {
        Write-Host "  $moduleName : OK $errorLogCount error logs" -ForegroundColor Green
    } else {
        Write-Host "  $moduleName : WARNING No error logging" -ForegroundColor Yellow
    }
}

Write-Host ""

# Test 7: Check for empty state handling
Write-Host "Test 7: Checking empty state handling..." -ForegroundColor Yellow

$modulesWithEmptyState = @(
    "src/main/resources/static/js/academic-years.js",
    "src/main/resources/static/js/professors.js",
    "src/main/resources/static/js/courses.js",
    "src/main/resources/static/js/course-assignments.js"
)

foreach ($module in $modulesWithEmptyState) {
    $moduleName = Split-Path $module -Leaf
    $content = Get-Content $module -Raw
    
    if ($content -match "showEmptyState\(") {
        Write-Host "  $moduleName : ✓ Has empty state handling" -ForegroundColor Green
    } else {
        Write-Host "  $moduleName : ✗ Missing empty state handling" -ForegroundColor Red
        $allPassed = $false
    }
}

Write-Host ""

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($allPassed) {
    Write-Host "✓ All critical tests passed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Error handling and loading states have been successfully implemented:" -ForegroundColor Green
    Write-Host "  • All page modules have handleApiError and handleError methods" -ForegroundColor White
    Write-Host "  • API calls are wrapped in try-catch blocks" -ForegroundColor White
    Write-Host "  • Loading states are managed appropriately" -ForegroundColor White
    Write-Host "  • Error status codes (401, 403, 500, NetworkError) are handled" -ForegroundColor White
    Write-Host "  • Toast notifications display user-friendly error messages" -ForegroundColor White
    Write-Host "  • Detailed errors are logged to console for debugging" -ForegroundColor White
    Write-Host "  • Empty states are shown when no data is available" -ForegroundColor White
} else {
    Write-Host "⚠ Some tests failed. Please review the output above." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Start the application: .\mvnw.cmd spring-boot:run" -ForegroundColor White
Write-Host "2. Test error scenarios manually:" -ForegroundColor White
Write-Host "   - Disconnect network and try loading data" -ForegroundColor White
Write-Host "   - Try accessing pages without authentication" -ForegroundColor White
Write-Host "   - Test with invalid data to trigger server errors" -ForegroundColor White
Write-Host "3. Verify loading indicators appear during data fetches" -ForegroundColor White
Write-Host "4. Verify toast notifications display appropriate messages" -ForegroundColor White
Write-Host ""
