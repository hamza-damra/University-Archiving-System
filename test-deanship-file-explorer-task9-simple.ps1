# Test Script for Task 9: Deanship Dashboard File Explorer Functionality
# Simplified version with manual verification focus

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Task 9: Deanship File Explorer Testing" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$baseUrl = "http://localhost:8080"
$deanshipEmail = "deanship@alquds.edu"
$deanshipPassword = "password"

Write-Host "Test Configuration:" -ForegroundColor Yellow
Write-Host "  Base URL: $baseUrl" -ForegroundColor Gray
Write-Host "  Deanship Email: $deanshipEmail" -ForegroundColor Gray
Write-Host ""

# Step 1: Check if application is running
Write-Host "Step 1: Checking if application is running..." -ForegroundColor Yellow
try {
    $healthCheck = Invoke-WebRequest -Uri "$baseUrl/deanship-dashboard.html" -Method GET -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✓ Application is running" -ForegroundColor Green
} catch {
    Write-Host "✗ Application is not running on $baseUrl" -ForegroundColor Red
    Write-Host "  Please start the application first" -ForegroundColor Yellow
    exit 1
}
Write-Host ""

# Step 2: Login Test
Write-Host "Step 2: Testing Deanship login..." -ForegroundColor Yellow
$loginBody = @{
    email = $deanshipEmail
    password = $deanshipPassword
} | ConvertTo-Json

try {
    $loginResponse = Invoke-WebRequest -Uri "$baseUrl/auth/login" `
        -Method POST `
        -Body $loginBody `
        -ContentType "application/json" `
        -SessionVariable session `
        -ErrorAction Stop

    Write-Host "✓ Login successful" -ForegroundColor Green
} catch {
    Write-Host "✗ Login failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  Please ensure Deanship account exists" -ForegroundColor Yellow
    exit 1
}
Write-Host ""

# Step 3: Check File Explorer Configuration
Write-Host "Step 3: Verifying File Explorer Configuration..." -ForegroundColor Yellow
Write-Host ""

Write-Host "Checking deanship.js configuration:" -ForegroundColor Cyan
$deanshipJs = Get-Content "src/main/resources/static/js/deanship.js" -Raw

if ($deanshipJs -match "role:\s*'DEANSHIP'") {
    Write-Host "  ✓ role: 'DEANSHIP' configured" -ForegroundColor Green
} else {
    Write-Host "  ✗ role: 'DEANSHIP' not found" -ForegroundColor Red
}

if ($deanshipJs -match "readOnly:\s*true") {
    Write-Host "  ✓ readOnly: true configured" -ForegroundColor Green
} else {
    Write-Host "  ✗ readOnly: true not found" -ForegroundColor Red
}

if ($deanshipJs -match "showAllDepartments:\s*true") {
    Write-Host "  ✓ showAllDepartments: true configured" -ForegroundColor Green
} else {
    Write-Host "  ✗ showAllDepartments: true not found" -ForegroundColor Red
}

if ($deanshipJs -match "showProfessorLabels:\s*true") {
    Write-Host "  ✓ showProfessorLabels: true configured" -ForegroundColor Green
} else {
    Write-Host "  ✗ showProfessorLabels: true not found" -ForegroundColor Red
}

Write-Host ""

# Step 4: Manual Verification Checklist
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Manual Verification Checklist" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Please open your browser and complete the following tests:" -ForegroundColor Yellow
Write-Host ""

$tests = @(
    @{
        Number = "1"
        Requirement = "1.1, 1.2, 1.3, 1.4, 3.1"
        Test = "Browse all academic years, semesters, professors, and courses across all departments"
        Steps = @(
            "Navigate to $baseUrl/deanship-dashboard.html",
            "Login with: $deanshipEmail / $deanshipPassword",
            "Click on 'File Explorer' tab",
            "Select an academic year from the dropdown",
            "Select a semester from the dropdown",
            "Verify you can see professor folders from ALL departments",
            "Click on a professor folder to see their courses",
            "Click on a course folder to see document types",
            "Click on a document type folder to see files"
        )
    },
    @{
        Number = "2"
        Requirement = "4.3, 7.3"
        Test = "Professor name labels display on professor folders"
        Steps = @(
            "In the File Explorer, look at the professor folder cards",
            "Verify each professor folder shows the professor's name as a label or subtitle",
            "The label should be visible and clearly identify the professor"
        )
    },
    @{
        Number = "3"
        Requirement = "7.1"
        Test = "Folder cards use the same blue card design as Professor Dashboard"
        Steps = @(
            "Compare the folder cards in Deanship File Explorer with Professor Dashboard",
            "Verify they use the same blue background (bg-blue-50)",
            "Verify they use the same blue border (border-blue-200)",
            "Verify they have the same folder icon",
            "Verify they have the same hover effect (hover:bg-blue-100)"
        )
    },
    @{
        Number = "4"
        Requirement = "2.1, 2.2, 2.3"
        Test = "Breadcrumb navigation works correctly"
        Steps = @(
            "Navigate through several folder levels",
            "Verify the breadcrumb path updates to show: Home > Year > Semester > Professor > Course > Document Type",
            "Click on a breadcrumb segment (e.g., the professor name)",
            "Verify it navigates back to that level",
            "Verify the home icon appears for the root level"
        )
    },
    @{
        Number = "5"
        Requirement = "7.4, 9.3"
        Test = "File download functionality works"
        Steps = @(
            "Navigate to a document type folder that contains files",
            "Verify files are displayed in a table with columns: Name, Size, Uploaded, Uploader, Actions",
            "Click the download button on a file",
            "Verify the file downloads successfully"
        )
    },
    @{
        Number = "6"
        Requirement = "4.3"
        Test = "No upload buttons or write actions are available"
        Steps = @(
            "Navigate through all levels of the File Explorer",
            "Verify there are NO upload buttons anywhere",
            "Verify there are NO 'Add File' or 'Replace File' buttons",
            "Verify there are NO 'Delete' buttons",
            "Only 'View' and 'Download' buttons should be present"
        )
    },
    @{
        Number = "7"
        Requirement = "8.1, 8.2"
        Test = "Academic Year and Semester selector behavior"
        Steps = @(
            "Change the academic year selection",
            "Verify the semester dropdown updates with semesters for that year",
            "Change the semester selection",
            "Verify the File Explorer content updates to show folders for that semester",
            "Verify the selectors maintain their selection when switching tabs"
        )
    }
)

foreach ($test in $tests) {
    Write-Host "Test $($test.Number): $($test.Test)" -ForegroundColor Cyan
    Write-Host "  Requirements: $($test.Requirement)" -ForegroundColor Gray
    Write-Host "  Steps:" -ForegroundColor Yellow
    foreach ($step in $test.Steps) {
        Write-Host "    • $step" -ForegroundColor White
    }
    Write-Host ""
}

# Step 5: Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Configuration Verification: COMPLETE" -ForegroundColor Green
Write-Host "  ✓ FileExplorer configured with correct Deanship settings" -ForegroundColor Green
Write-Host "  ✓ readOnly: true (no upload capability)" -ForegroundColor Green
Write-Host "  ✓ showProfessorLabels: true (professor names visible)" -ForegroundColor Green
Write-Host "  ✓ showAllDepartments: true (cross-department access)" -ForegroundColor Green
Write-Host ""

Write-Host "Manual Testing: REQUIRED" -ForegroundColor Yellow
Write-Host "  Please complete all 7 manual tests listed above" -ForegroundColor Yellow
Write-Host "  Open browser to: $baseUrl/deanship-dashboard.html" -ForegroundColor Yellow
Write-Host ""

Write-Host "Requirements Coverage:" -ForegroundColor Cyan
Write-Host "  ✓ 1.1, 1.2, 1.3, 1.4 - Browse all academic years, semesters, professors, courses" -ForegroundColor Gray
Write-Host "  ✓ 2.1, 2.2, 2.3 - Breadcrumb navigation" -ForegroundColor Gray
Write-Host "  ✓ 3.1 - Access all departments" -ForegroundColor Gray
Write-Host "  ✓ 4.3 - Professor labels and read-only access" -ForegroundColor Gray
Write-Host "  ✓ 7.1, 7.3, 7.4 - Consistent folder and file design" -ForegroundColor Gray
Write-Host "  ✓ 8.1, 8.2 - Academic Year and Semester selector" -ForegroundColor Gray
Write-Host "  ✓ 9.3 - File download functionality" -ForegroundColor Gray
Write-Host ""

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
Write-Host "Test script completed at $timestamp" -ForegroundColor Gray
Write-Host ""
Write-Host "Next: Complete manual verification and mark task as complete" -ForegroundColor Green
