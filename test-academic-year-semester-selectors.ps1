# Test Script: Academic Year and Semester Selector Standardization
# Task 14: Verify consistent styling and behavior across all dashboards

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Academic Year & Semester Selector Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test Configuration
$baseUrl = "http://localhost:8080"
$testResults = @()

function Test-SelectorStandardization {
    param(
        [string]$DashboardName,
        [string]$HtmlFile,
        [string]$JsFile
    )
    
    Write-Host "Testing $DashboardName Dashboard..." -ForegroundColor Yellow
    Write-Host "-----------------------------------" -ForegroundColor Yellow
    
    $htmlPath = "src/main/resources/static/$HtmlFile"
    $jsPath = "src/main/resources/static/js/$JsFile"
    
    $htmlContent = Get-Content $htmlPath -Raw
    $jsContent = Get-Content $jsPath -Raw
    
    $results = @{
        Dashboard = $DashboardName
        Tests = @()
    }
    
    # Test 1: HTML Structure - Container
    Write-Host "  [1] Checking container structure..." -NoNewline
    if ($htmlContent -match 'class="bg-white rounded-lg shadow-md p-6') {
        Write-Host " PASS" -ForegroundColor Green
        $results.Tests += @{ Name = "Container Structure"; Status = "PASS" }
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $results.Tests += @{ Name = "Container Structure"; Status = "FAIL" }
    }
    
    # Test 2: HTML Structure - Flex Layout
    Write-Host "  [2] Checking flex layout..." -NoNewline
    if ($htmlContent -match 'class="flex flex-wrap items-center gap-4') {
        Write-Host " PASS" -ForegroundColor Green
        $results.Tests += @{ Name = "Flex Layout"; Status = "PASS" }
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $results.Tests += @{ Name = "Flex Layout"; Status = "FAIL" }
    }
    
    # Test 3: Academic Year Selector - Label
    Write-Host "  [3] Checking Academic Year label..." -NoNewline
    if ($htmlContent -match 'class="block text-sm font-medium text-gray-700 mb-2"[^>]*>\s*Academic Year') {
        Write-Host " PASS" -ForegroundColor Green
        $results.Tests += @{ Name = "Academic Year Label"; Status = "PASS" }
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $results.Tests += @{ Name = "Academic Year Label"; Status = "FAIL" }
    }
    
    # Test 4: Academic Year Selector - Dropdown Styling
    Write-Host "  [4] Checking Academic Year dropdown styling..." -NoNewline
    if ($htmlContent -match 'id="academicYearSelect"[^>]*class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"') {
        Write-Host " PASS" -ForegroundColor Green
        $results.Tests += @{ Name = "Academic Year Dropdown Styling"; Status = "PASS" }
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $results.Tests += @{ Name = "Academic Year Dropdown Styling"; Status = "FAIL" }
    }
    
    # Test 5: Semester Selector - Label
    Write-Host "  [5] Checking Semester label..." -NoNewline
    if ($htmlContent -match 'class="block text-sm font-medium text-gray-700 mb-2"[^>]*>\s*Semester') {
        Write-Host " PASS" -ForegroundColor Green
        $results.Tests += @{ Name = "Semester Label"; Status = "PASS" }
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $results.Tests += @{ Name = "Semester Label"; Status = "FAIL" }
    }
    
    # Test 6: Semester Selector - Dropdown Styling
    Write-Host "  [6] Checking Semester dropdown styling..." -NoNewline
    if ($htmlContent -match 'id="semesterSelect"[^>]*class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"') {
        Write-Host " PASS" -ForegroundColor Green
        $results.Tests += @{ Name = "Semester Dropdown Styling"; Status = "PASS" }
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $results.Tests += @{ Name = "Semester Dropdown Styling"; Status = "FAIL" }
    }
    
    # Test 7: Responsive Sizing
    Write-Host "  [7] Checking responsive sizing (flex-1 min-w-[200px])..." -NoNewline
    if ($htmlContent -match 'class="flex-1 min-w-\[200px\]') {
        Write-Host " PASS" -ForegroundColor Green
        $results.Tests += @{ Name = "Responsive Sizing"; Status = "PASS" }
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $results.Tests += @{ Name = "Responsive Sizing"; Status = "FAIL" }
    }
    
    # Test 8: JavaScript - Academic Year Change Handler
    Write-Host "  [8] Checking Academic Year change handler..." -NoNewline
    if ($jsContent -match "academicYearSelect\.addEventListener\('change'|getElementById\('academicYearSelect'\)\.addEventListener\('change'") {
        Write-Host " PASS" -ForegroundColor Green
        $results.Tests += @{ Name = "Academic Year Change Handler"; Status = "PASS" }
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $results.Tests += @{ Name = "Academic Year Change Handler"; Status = "FAIL" }
    }
    
    # Test 9: JavaScript - Semester Change Handler
    Write-Host "  [9] Checking Semester change handler..." -NoNewline
    if ($jsContent -match "semesterSelect\.addEventListener\('change'|getElementById\('semesterSelect'\)\.addEventListener\('change'") {
        Write-Host " PASS" -ForegroundColor Green
        $results.Tests += @{ Name = "Semester Change Handler"; Status = "PASS" }
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $results.Tests += @{ Name = "Semester Change Handler"; Status = "FAIL" }
    }
    
    # Test 10: JavaScript - Load Academic Years Function
    Write-Host " [10] Checking loadAcademicYears function..." -NoNewline
    if ($jsContent -match "async function loadAcademicYears|function loadAcademicYears") {
        Write-Host " PASS" -ForegroundColor Green
        $results.Tests += @{ Name = "Load Academic Years Function"; Status = "PASS" }
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $results.Tests += @{ Name = "Load Academic Years Function"; Status = "FAIL" }
    }
    
    # Test 11: JavaScript - Load Semesters Function
    Write-Host " [11] Checking loadSemesters function..." -NoNewline
    if ($jsContent -match "async function loadSemesters|function loadSemesters") {
        Write-Host " PASS" -ForegroundColor Green
        $results.Tests += @{ Name = "Load Semesters Function"; Status = "PASS" }
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $results.Tests += @{ Name = "Load Semesters Function"; Status = "FAIL" }
    }
    
    # Test 12: Master Design Reference Comment
    Write-Host " [12] Checking for master design reference comment..." -NoNewline
    if ($htmlContent -match "MASTER DESIGN REFERENCE.*Academic Year and Semester Selector") {
        Write-Host " PASS" -ForegroundColor Green
        $results.Tests += @{ Name = "Master Design Reference Comment"; Status = "PASS" }
    } else {
        Write-Host " FAIL" -ForegroundColor Red
        $results.Tests += @{ Name = "Master Design Reference Comment"; Status = "FAIL" }
    }
    
    Write-Host ""
    
    return $results
}

# Test all three dashboards
$professorResults = Test-SelectorStandardization -DashboardName "Professor" -HtmlFile "prof-dashboard.html" -JsFile "prof.js"
$hodResults = Test-SelectorStandardization -DashboardName "HOD" -HtmlFile "hod-dashboard.html" -JsFile "hod.js"
$deanshipResults = Test-SelectorStandardization -DashboardName "Deanship" -HtmlFile "deanship-dashboard.html" -JsFile "deanship.js"

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$allResults = @($professorResults, $hodResults, $deanshipResults)

foreach ($result in $allResults) {
    $passCount = ($result.Tests | Where-Object { $_.Status -eq "PASS" }).Count
    $failCount = ($result.Tests | Where-Object { $_.Status -eq "FAIL" }).Count
    $total = $result.Tests.Count
    
    Write-Host "$($result.Dashboard) Dashboard: " -NoNewline
    Write-Host "$passCount/$total tests passed" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Yellow" })
    
    if ($failCount -gt 0) {
        Write-Host "  Failed tests:" -ForegroundColor Red
        $result.Tests | Where-Object { $_.Status -eq "FAIL" } | ForEach-Object {
            Write-Host "    - $($_.Name)" -ForegroundColor Red
        }
    }
}

Write-Host ""

# Overall result
$totalTests = ($allResults | ForEach-Object { $_.Tests.Count } | Measure-Object -Sum).Sum
$totalPassed = ($allResults | ForEach-Object { ($_.Tests | Where-Object { $_.Status -eq "PASS" }).Count } | Measure-Object -Sum).Sum
$totalFailed = $totalTests - $totalPassed

Write-Host "Overall: $totalPassed/$totalTests tests passed" -ForegroundColor $(if ($totalFailed -eq 0) { "Green" } else { "Yellow" })

if ($totalFailed -eq 0) {
    Write-Host ""
    Write-Host "All tests passed! Academic Year and Semester selectors are standardized." -ForegroundColor Green
    exit 0
} else {
    Write-Host ""
    Write-Host "Some tests failed. Please review the failed tests above." -ForegroundColor Red
    exit 1
}
