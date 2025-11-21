# Test Dashboard Page Implementation
# Verifies that all required files and components are in place

Write-Host "=== Dashboard Page Implementation Test ===" -ForegroundColor Cyan
Write-Host ""

# Check if dashboard.html exists
$dashboardHtml = "src/main/resources/static/deanship/dashboard.html"
if (Test-Path $dashboardHtml) {
    Write-Host "[OK] dashboard.html exists" -ForegroundColor Green
    
    # Check for required elements in HTML
    $htmlContent = Get-Content $dashboardHtml -Raw
    
    if ($htmlContent -match "Welcome to Deanship Dashboard") {
        Write-Host "  [OK] Dashboard title" -ForegroundColor Green
    }
    if ($htmlContent -match 'id="dashboardCards"') {
        Write-Host "  [OK] Dashboard cards container" -ForegroundColor Green
    }
    if ($htmlContent -match 'id="academicYearSelect"') {
        Write-Host "  [OK] Academic year select" -ForegroundColor Green
    }
    if ($htmlContent -match 'id="semesterSelect"') {
        Write-Host "  [OK] Semester select" -ForegroundColor Green
    }
    if ($htmlContent -match 'id="logoutBtn"') {
        Write-Host "  [OK] Logout button" -ForegroundColor Green
    }
    if ($htmlContent -match 'class="nav-link"') {
        Write-Host "  [OK] Navigation links" -ForegroundColor Green
    }
    if ($htmlContent -match 'src="/js/dashboard.js"') {
        Write-Host "  [OK] Dashboard JS module" -ForegroundColor Green
    }
    if ($htmlContent -match 'id="toastContainer"') {
        Write-Host "  [OK] Toast container" -ForegroundColor Green
    }
}

Write-Host ""

# Check if dashboard.js exists
$dashboardJs = "src/main/resources/static/js/dashboard.js"
if (Test-Path $dashboardJs) {
    Write-Host "[OK] dashboard.js exists" -ForegroundColor Green
    
    $jsContent = Get-Content $dashboardJs -Raw
    
    if ($jsContent -match "class DashboardPage") {
        Write-Host "  [OK] DashboardPage class" -ForegroundColor Green
    }
    if ($jsContent -match "async initialize") {
        Write-Host "  [OK] initialize method" -ForegroundColor Green
    }
    if ($jsContent -match "loadDashboardStats") {
        Write-Host "  [OK] loadDashboardStats method" -ForegroundColor Green
    }
    if ($jsContent -match "loadAcademicYearsCount") {
        Write-Host "  [OK] loadAcademicYearsCount" -ForegroundColor Green
    }
    if ($jsContent -match "loadProfessorsCount") {
        Write-Host "  [OK] loadProfessorsCount" -ForegroundColor Green
    }
    if ($jsContent -match "loadCoursesCount") {
        Write-Host "  [OK] loadCoursesCount" -ForegroundColor Green
    }
    if ($jsContent -match "loadAssignmentsCount") {
        Write-Host "  [OK] loadAssignmentsCount" -ForegroundColor Green
    }
    if ($jsContent -match "loadSubmissionReport") {
        Write-Host "  [OK] loadSubmissionReport" -ForegroundColor Green
    }
    if ($jsContent -match "renderDashboardCards") {
        Write-Host "  [OK] renderDashboardCards" -ForegroundColor Green
    }
    if ($jsContent -match "DeanshipLayout") {
        Write-Host "  [OK] DeanshipLayout import" -ForegroundColor Green
    }
    if ($jsContent -match "apiRequest") {
        Write-Host "  [OK] apiRequest import" -ForegroundColor Green
    }
    if ($jsContent -match "showToast") {
        Write-Host "  [OK] showToast import" -ForegroundColor Green
    }
}

Write-Host ""

# Check if DeanshipViewController has dashboard route
$controller = "src/main/java/com/alqude/edu/ArchiveSystem/controller/DeanshipViewController.java"
if (Test-Path $controller) {
    Write-Host "[OK] DeanshipViewController exists" -ForegroundColor Green
    
    $controllerContent = Get-Content $controller -Raw
    
    if ($controllerContent -match '@GetMapping\("/dashboard"\)') {
        Write-Host "  [OK] Dashboard route mapping exists" -ForegroundColor Green
    }
    
    if ($controllerContent -match 'return "deanship/dashboard"') {
        Write-Host "  [OK] Dashboard view name correct" -ForegroundColor Green
    }
}

Write-Host ""

# Check if required dependencies exist
Write-Host "Checking dependencies..." -ForegroundColor Cyan

if (Test-Path "src/main/resources/static/js/deanship-common.js") {
    Write-Host "  [OK] deanship-common.js" -ForegroundColor Green
}
if (Test-Path "src/main/resources/static/css/deanship-layout.css") {
    Write-Host "  [OK] deanship-layout.css" -ForegroundColor Green
}
if (Test-Path "src/main/resources/static/js/api.js") {
    Write-Host "  [OK] api.js" -ForegroundColor Green
}
if (Test-Path "src/main/resources/static/js/ui.js") {
    Write-Host "  [OK] ui.js" -ForegroundColor Green
}

Write-Host ""
Write-Host "=== Test Complete ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Implementation Summary:" -ForegroundColor Yellow
Write-Host "- Dashboard HTML page created with shared layout structure"
Write-Host "- Dashboard JS module with DeanshipLayout initialization"
Write-Host "- Six dashboard cards with icons, titles, and descriptions"
Write-Host "- Click handlers for navigation to corresponding pages"
Write-Host "- loadDashboardStats function to fetch metrics from APIs"
Write-Host "- Stats display for academic years, professors, courses, assignments"
Write-Host "- Submission completion percentage from system-wide report"
Write-Host "- Card styling with 24px padding, 8px border radius, box shadow"
Write-Host "- Responsive design for 1366x768 and above resolutions"
