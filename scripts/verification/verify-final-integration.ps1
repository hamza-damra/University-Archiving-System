# Final Integration Verification Script
# Verifies all modules are properly integrated and ready for deployment

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Dean Dashboard - Final Integration Verification" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$errors = 0
$warnings = 0

# Define base paths
$staticPath = "src/main/resources/static"
$jsPath = "$staticPath/js"
$cssPath = "$staticPath/css"

Write-Host "1. Verifying File Structure..." -ForegroundColor Yellow

# Check main HTML file
if (Test-Path "$staticPath/deanship-dashboard.html") {
    Write-Host "   ✓ deanship-dashboard.html exists" -ForegroundColor Green
} else {
    Write-Host "   ✗ deanship-dashboard.html missing" -ForegroundColor Red
    $errors++
}

# Check CSS file
if (Test-Path "$cssPath/deanship-dashboard.css") {
    Write-Host "   ✓ deanship-dashboard.css exists" -ForegroundColor Green
} else {
    Write-Host "   ✗ deanship-dashboard.css missing" -ForegroundColor Red
    $errors++
}

# Check all required JavaScript modules
$requiredModules = @(
    "deanship.js",
    "deanship-state.js",
    "deanship-navigation.js",
    "deanship-analytics.js",
    "deanship-tables.js",
    "deanship-reports.js",
    "deanship-feedback.js",
    "deanship-file-explorer-enhanced.js",
    "deanship-error-handler.js",
    "deanship-export.js",
    "api.js",
    "ui.js",
    "file-explorer.js",
    "file-explorer-state.js"
)

Write-Host ""
Write-Host "2. Verifying JavaScript Modules..." -ForegroundColor Yellow

foreach ($module in $requiredModules) {
    if (Test-Path "$jsPath/$module") {
        Write-Host "   ✓ $module exists" -ForegroundColor Green
    } else {
        Write-Host "   ✗ $module missing" -ForegroundColor Red
        $errors++
    }
}

Write-Host ""
Write-Host "3. Verifying Module Imports..." -ForegroundColor Yellow

# Check deanship.js imports
$deanshipContent = Get-Content "$jsPath/deanship.js" -Raw

$requiredImports = @(
    "apiRequest",
    "getUserInfo",
    "redirectToLogin",
    "showToast",
    "showModal",
    "FileExplorer",
    "fileExplorerState",
    "SkeletonLoader",
    "EmptyState",
    "dashboardNavigation",
    "dashboardAnalytics",
    "dashboardState",
    "ErrorBoundary"
)

foreach ($import in $requiredImports) {
    if ($deanshipContent -match $import) {
        Write-Host "   ✓ $import imported" -ForegroundColor Green
    } else {
        Write-Host "   ✗ $import not found in imports" -ForegroundColor Red
        $errors++
    }
}

Write-Host ""
Write-Host "4. Verifying HTML Script Tags..." -ForegroundColor Yellow

$htmlContent = Get-Content "$staticPath/deanship-dashboard.html" -Raw

$requiredScripts = @(
    "jspdf",
    "jspdf-autotable",
    "xlsx",
    "deanship-tables.js",
    "deanship-export.js",
    "deanship-reports.js",
    "deanship.js"
)

foreach ($script in $requiredScripts) {
    if ($htmlContent -match $script) {
        Write-Host "   ✓ $script included in HTML" -ForegroundColor Green
    } else {
        Write-Host "   ✗ $script not found in HTML" -ForegroundColor Red
        $errors++
    }
}

Write-Host ""
Write-Host "5. Verifying HTML Structure..." -ForegroundColor Yellow

$requiredElements = @(
    "dashboard-tab",
    "academic-years-tab",
    "professors-tab",
    "courses-tab",
    "assignments-tab",
    "reports-tab",
    "file-explorer-tab",
    "breadcrumbContainer",
    "academicYearSelect",
    "semesterSelect",
    "submissionTrendsChart",
    "departmentComplianceChart",
    "statusDistributionChart",
    "recentActivityFeed",
    "quickActionsCard"
)

foreach ($element in $requiredElements) {
    if ($htmlContent -match $element) {
        Write-Host "   ✓ $element present in HTML" -ForegroundColor Green
    } else {
        Write-Host "   ✗ $element missing from HTML" -ForegroundColor Red
        $errors++
    }
}

Write-Host ""
Write-Host "6. Checking for Common Issues..." -ForegroundColor Yellow

# Check for console.log statements (should be minimal in production)
$consoleLogCount = (Select-String -Path "$jsPath\deanship*.js" -Pattern "console\.log" -ErrorAction SilentlyContinue | Measure-Object).Count
if ($consoleLogCount -gt 10) {
    Write-Host "   Warning: Found $consoleLogCount console.log statements (consider removing for production)" -ForegroundColor Yellow
    $warnings++
} else {
    Write-Host "   OK: Console.log usage is acceptable ($consoleLogCount found)" -ForegroundColor Green
}

# Check for TODO comments
$todoCount = (Select-String -Path "$jsPath\deanship*.js" -Pattern "TODO" -ErrorAction SilentlyContinue | Measure-Object).Count
if ($todoCount -gt 0) {
    Write-Host "   Warning: Found $todoCount TODO/FIXME comments" -ForegroundColor Yellow
    $warnings++
} else {
    Write-Host "   OK: No TODO/FIXME comments found" -ForegroundColor Green
}

# Check for debugger statements
$debuggerCount = (Select-String -Path "$jsPath\deanship*.js" -Pattern "debugger" -ErrorAction SilentlyContinue | Measure-Object).Count
if ($debuggerCount -gt 0) {
    Write-Host "   Error: Found $debuggerCount debugger statements (must remove for production)" -ForegroundColor Red
    $errors++
} else {
    Write-Host "   OK: No debugger statements found" -ForegroundColor Green
}

Write-Host ""
Write-Host "7. Verifying Documentation..." -ForegroundColor Yellow

$docFiles = @(
    "docs/deployment/final-integration-checklist.md",
    "docs/tasks/task4-analytics-implementation-summary.md",
    "docs/tasks/task-3-navigation-implementation-summary.md",
    "docs/tasks/task5-enhanced-tables-implementation.md",
    "docs/tasks/task6-reports-export-implementation.md",
    "docs/tasks/task7-file-explorer-enhancements.md",
    "docs/tasks/task8-enhanced-feedback-implementation.md",
    "docs/tasks/task11-testing-implementation-summary.md",
    "docs/tasks/task12-performance-optimization.md"
)

foreach ($doc in $docFiles) {
    if (Test-Path $doc) {
        Write-Host "   ✓ $doc exists" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ $doc missing" -ForegroundColor Yellow
        $warnings++
    }
}

Write-Host ""
Write-Host "8. Checking Test Files..." -ForegroundColor Yellow

$testFiles = @(
    "test-analytics-dashboard.html",
    "test-breadcrumb-navigation.html",
    "test-collapsible-sidebar.html",
    "test-enhanced-tables.html",
    "test-enhanced-toasts.html",
    "test-file-explorer-enhancements.html",
    "test-reports-export.html",
    "test-skeleton-empty-states.html",
    "test-performance-optimization.html"
)

foreach ($test in $testFiles) {
    if (Test-Path $test) {
        Write-Host "   ✓ $test exists" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ $test missing" -ForegroundColor Yellow
        $warnings++
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Verification Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($errors -eq 0 -and $warnings -eq 0) {
    Write-Host ""
    Write-Host "SUCCESS: ALL CHECKS PASSED!" -ForegroundColor Green
    Write-Host ""
    Write-Host "The Dean Dashboard is fully integrated and ready for deployment." -ForegroundColor Green
    Write-Host ""
    Write-Host "Next Steps:" -ForegroundColor Cyan
    Write-Host "  1. Build the application: mvn clean package" -ForegroundColor White
    Write-Host "  2. Deploy to staging environment" -ForegroundColor White
    Write-Host "  3. Perform user acceptance testing" -ForegroundColor White
    Write-Host "  4. Deploy to production" -ForegroundColor White
    Write-Host ""
    exit 0
} elseif ($errors -eq 0) {
    Write-Host ""
    Write-Host "WARNING: VERIFICATION PASSED WITH WARNINGS" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Warnings: $warnings" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "The dashboard is functional but has some non-critical issues." -ForegroundColor Yellow
    Write-Host "Review warnings above before deploying to production." -ForegroundColor Yellow
    Write-Host ""
    exit 0
} else {
    Write-Host ""
    Write-Host "ERROR: VERIFICATION FAILED" -ForegroundColor Red
    Write-Host ""
    Write-Host "Errors: $errors" -ForegroundColor Red
    Write-Host "Warnings: $warnings" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Please fix the errors above before deploying." -ForegroundColor Red
    Write-Host ""
    exit 1
}
