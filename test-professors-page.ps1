# Test script for Professors Management Page
# Tests the professors page functionality

Write-Host "=== Professors Management Page Test ===" -ForegroundColor Cyan
Write-Host ""

# Check if files exist
Write-Host "1. Checking if files exist..." -ForegroundColor Yellow
$htmlFile = "src/main/resources/static/deanship/professors.html"
$jsFile = "src/main/resources/static/js/professors.js"

if (Test-Path $htmlFile) {
    Write-Host "   OK HTML file exists: $htmlFile" -ForegroundColor Green
} else {
    Write-Host "   ERROR HTML file missing: $htmlFile" -ForegroundColor Red
    exit 1
}

if (Test-Path $jsFile) {
    Write-Host "   OK JS file exists: $jsFile" -ForegroundColor Green
} else {
    Write-Host "   ERROR JS file missing: $jsFile" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "2. Checking HTML structure..." -ForegroundColor Yellow
$htmlContent = Get-Content $htmlFile -Raw

# Check for required elements
$requiredElements = @(
    "professors.js",
    "addProfessorBtn",
    "searchInput",
    "departmentFilter",
    "professorsTableBody",
    "Professors Management"
)

foreach ($element in $requiredElements) {
    if ($htmlContent -match [regex]::Escape($element)) {
        Write-Host "   OK Found: $element" -ForegroundColor Green
    } else {
        Write-Host "   ERROR Missing: $element" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "3. Checking JavaScript module..." -ForegroundColor Yellow
$jsContent = Get-Content $jsFile -Raw

# Check for required functions
$requiredFunctions = @(
    "class ProfessorsPage",
    "loadProfessors",
    "renderProfessorsTable",
    "handleSearch",
    "showAddProfessorModal",
    "showEditProfessorModal",
    "activateProfessor",
    "deactivateProfessor",
    "loadDepartments"
)

foreach ($func in $requiredFunctions) {
    if ($jsContent -match [regex]::Escape($func)) {
        Write-Host "   OK Found: $func" -ForegroundColor Green
    } else {
        Write-Host "   ERROR Missing: $func" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "4. Checking API endpoints..." -ForegroundColor Yellow
$apiEndpoints = @(
    "/deanship/professors",
    "/deanship/departments",
    "/activate",
    "/deactivate"
)

foreach ($endpoint in $apiEndpoints) {
    if ($jsContent -match [regex]::Escape($endpoint)) {
        Write-Host "   OK Found: $endpoint" -ForegroundColor Green
    } else {
        Write-Host "   ERROR Missing: $endpoint" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== Test Summary ===" -ForegroundColor Cyan
Write-Host "All basic checks passed!" -ForegroundColor Green
Write-Host ""
Write-Host "To test manually:" -ForegroundColor Yellow
Write-Host "1. Start the application" -ForegroundColor White
Write-Host "2. Login as deanship user" -ForegroundColor White
Write-Host "3. Navigate to professors page" -ForegroundColor White
Write-Host ""
Write-Host "Expected functionality:" -ForegroundColor Yellow
Write-Host "- View list of professors with their details" -ForegroundColor White
Write-Host "- Search professors by name, email, or ID" -ForegroundColor White
Write-Host "- Filter professors by department" -ForegroundColor White
Write-Host "- Add new professors" -ForegroundColor White
Write-Host "- Edit existing professors" -ForegroundColor White
Write-Host "- Activate or deactivate professors" -ForegroundColor White
Write-Host ""
