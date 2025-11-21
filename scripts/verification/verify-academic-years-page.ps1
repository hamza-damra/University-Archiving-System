# Verify Academic Years Page Implementation
# Checks that all required files and components are in place

Write-Host "=== Academic Years Page Implementation Verification ===" -ForegroundColor Cyan
Write-Host ""

$allPassed = $true

# Check if academic-years.html exists
$htmlFile = "src/main/resources/static/deanship/academic-years.html"
Write-Host "Checking HTML file..." -ForegroundColor Yellow
if (Test-Path $htmlFile) {
    Write-Host "[OK] academic-years.html exists" -ForegroundColor Green
    
    # Check for required elements in HTML
    $htmlContent = Get-Content $htmlFile -Raw
    
    $checks = @(
        @{ Pattern = "Academic Years Management"; Name = "Page title" },
        @{ Pattern = 'id="addAcademicYearBtn"'; Name = "Add button" },
        @{ Pattern = 'id="academicYearsTableBody"'; Name = "Table body" },
        @{ Pattern = 'id="loadingState"'; Name = "Loading state" },
        @{ Pattern = 'id="emptyState"'; Name = "Empty state" },
        @{ Pattern = 'id="academicYearSelect"'; Name = "Academic year select" },
        @{ Pattern = 'id="semesterSelect"'; Name = "Semester select" },
        @{ Pattern = 'id="logoutBtn"'; Name = "Logout button" },
        @{ Pattern = 'class="nav-link"'; Name = "Navigation links" },
        @{ Pattern = 'href="/deanship/dashboard"'; Name = "Dashboard link" },
        @{ Pattern = 'href="/deanship/academic-years"'; Name = "Academic years link" },
        @{ Pattern = 'href="/deanship/professors"'; Name = "Professors link" },
        @{ Pattern = 'href="/deanship/courses"'; Name = "Courses link" },
        @{ Pattern = 'href="/deanship/course-assignments"'; Name = "Assignments link" },
        @{ Pattern = 'href="/deanship/reports"'; Name = "Reports link" },
        @{ Pattern = 'href="/deanship/file-explorer"'; Name = "File explorer link" },
        @{ Pattern = 'src="/js/academic-years.js"'; Name = "Academic years JS module" },
        @{ Pattern = 'href="/css/deanship-layout.css"'; Name = "Layout CSS" },
        @{ Pattern = 'id="toastContainer"'; Name = "Toast container" },
        @{ Pattern = 'id="modalsContainer"'; Name = "Modals container" }
    )
    
    foreach ($check in $checks) {
        if ($htmlContent -match [regex]::Escape($check.Pattern)) {
            Write-Host "  [OK] $($check.Name)" -ForegroundColor Green
        } else {
            Write-Host "  [FAIL] $($check.Name) not found" -ForegroundColor Red
            $allPassed = $false
        }
    }
} else {
    Write-Host "[FAIL] academic-years.html not found" -ForegroundColor Red
    $allPassed = $false
}

Write-Host ""

# Check if academic-years.js exists
$jsFile = "src/main/resources/static/js/academic-years.js"
Write-Host "Checking JavaScript file..." -ForegroundColor Yellow
if (Test-Path $jsFile) {
    Write-Host "[OK] academic-years.js exists" -ForegroundColor Green
    
    $jsContent = Get-Content $jsFile -Raw
    
    $checks = @(
        @{ Pattern = "class AcademicYearsPage"; Name = "AcademicYearsPage class" },
        @{ Pattern = "import.*DeanshipLayout.*from.*deanship-common"; Name = "DeanshipLayout import" },
        @{ Pattern = "import.*apiRequest.*from.*api"; Name = "apiRequest import" },
        @{ Pattern = "import.*showToast.*showModal.*from.*ui"; Name = "UI utilities import" },
        @{ Pattern = "async initialize\(\)"; Name = "initialize method" },
        @{ Pattern = "async loadAcademicYears\(\)"; Name = "loadAcademicYears method" },
        @{ Pattern = "renderAcademicYearsTable\(\)"; Name = "renderAcademicYearsTable method" },
        @{ Pattern = "showAddAcademicYearModal\(\)"; Name = "showAddAcademicYearModal method" },
        @{ Pattern = "showEditAcademicYearModal\("; Name = "showEditAcademicYearModal method" },
        @{ Pattern = "handleCreateAcademicYear\("; Name = "handleCreateAcademicYear method" },
        @{ Pattern = "handleUpdateAcademicYear\("; Name = "handleUpdateAcademicYear method" },
        @{ Pattern = "activateAcademicYear\("; Name = "activateAcademicYear method" },
        @{ Pattern = "'/deanship/academic-years'"; Name = "API endpoint" },
        @{ Pattern = "showLoading\("; Name = "showLoading method" },
        @{ Pattern = "showEmptyState\("; Name = "showEmptyState method" },
        @{ Pattern = "DOMContentLoaded"; Name = "DOM ready listener" }
    )
    
    foreach ($check in $checks) {
        if ($jsContent -match $check.Pattern) {
            Write-Host "  [OK] $($check.Name)" -ForegroundColor Green
        } else {
            Write-Host "  [FAIL] $($check.Name) not found" -ForegroundColor Red
            $allPassed = $false
        }
    }
} else {
    Write-Host "[FAIL] academic-years.js not found" -ForegroundColor Red
    $allPassed = $false
}

Write-Host ""

# Check if DeanshipViewController has the route
$controllerFile = "src/main/java/com/alqude/edu/ArchiveSystem/controller/DeanshipViewController.java"
Write-Host "Checking backend controller..." -ForegroundColor Yellow
if (Test-Path $controllerFile) {
    Write-Host "[OK] DeanshipViewController.java exists" -ForegroundColor Green
    
    $controllerContent = Get-Content $controllerFile -Raw
    
    $checks = @(
        @{ Pattern = '@GetMapping\("/academic-years"\)'; Name = "Academic years route mapping" },
        @{ Pattern = 'public String academicYears\(\)'; Name = "academicYears method" },
        @{ Pattern = 'return "deanship/academic-years"'; Name = "Return view name" },
        @{ Pattern = '@PreAuthorize\("hasRole\(''DEANSHIP''\)"\)'; Name = "Security annotation" }
    )
    
    foreach ($check in $checks) {
        if ($controllerContent -match $check.Pattern) {
            Write-Host "  [OK] $($check.Name)" -ForegroundColor Green
        } else {
            Write-Host "  [FAIL] $($check.Name) not found" -ForegroundColor Red
            $allPassed = $false
        }
    }
} else {
    Write-Host "[FAIL] DeanshipViewController.java not found" -ForegroundColor Red
    $allPassed = $false
}

Write-Host ""

# Check if shared layout files exist
Write-Host "Checking shared layout files..." -ForegroundColor Yellow

$sharedFiles = @(
    @{ Path = "src/main/resources/static/js/deanship-common.js"; Name = "deanship-common.js" },
    @{ Path = "src/main/resources/static/css/deanship-layout.css"; Name = "deanship-layout.css" },
    @{ Path = "src/main/resources/static/js/api.js"; Name = "api.js" },
    @{ Path = "src/main/resources/static/js/ui.js"; Name = "ui.js" }
)

foreach ($file in $sharedFiles) {
    if (Test-Path $file.Path) {
        Write-Host "  [OK] $($file.Name) exists" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] $($file.Name) not found" -ForegroundColor Red
        $allPassed = $false
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan

if ($allPassed) {
    Write-Host "All checks passed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Implementation Summary:" -ForegroundColor Yellow
    Write-Host "- HTML page created with shared layout" -ForegroundColor Gray
    Write-Host "- JavaScript module with full CRUD functionality" -ForegroundColor Gray
    Write-Host "- Backend route configured" -ForegroundColor Gray
    Write-Host "- Loading and empty states implemented" -ForegroundColor Gray
    Write-Host "- Add, Edit, and Activate modals implemented" -ForegroundColor Gray
    Write-Host ""
    Write-Host "To test manually:" -ForegroundColor Yellow
    Write-Host "1. Start the application: ./mvnw spring-boot:run" -ForegroundColor Gray
    Write-Host "2. Open browser to: http://localhost:8080/deanship/academic-years" -ForegroundColor Gray
    Write-Host "3. Login with dean@alquds.edu / dean123" -ForegroundColor Gray
    Write-Host "4. Test Add, Edit, and Activate functionality" -ForegroundColor Gray
} else {
    Write-Host "Some checks failed!" -ForegroundColor Red
    Write-Host "Please review the errors above." -ForegroundColor Yellow
}

Write-Host ""
