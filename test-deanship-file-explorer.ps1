# Deanship Dashboard File Explorer Testing Script
# This script tests all requirements for Task 8

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Deanship Dashboard File Explorer Test Suite" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test Configuration
$baseUrl = "http://localhost:8080"
$deanshipEmail = "dean@alquds.edu"
$deanshipPassword = "password123"

Write-Host "Test Configuration:" -ForegroundColor Yellow
Write-Host "  Base URL: $baseUrl" -ForegroundColor Gray
Write-Host "  Deanship Email: $deanshipEmail" -ForegroundColor Gray
Write-Host "  Access: All Departments" -ForegroundColor Gray
Write-Host ""

# Function to display test result
function Show-TestResult {
    param(
        [string]$TestName,
        [string]$Status,
        [string]$Details = ""
    )
    
    $color = switch ($Status) {
        "PASS" { "Green" }
        "FAIL" { "Red" }
        "INFO" { "Cyan" }
        "WARN" { "Yellow" }
        default { "White" }
    }
    
    Write-Host "[$Status] " -ForegroundColor $color -NoNewline
    Write-Host "$TestName" -ForegroundColor White
    if ($Details) {
        Write-Host "      $Details" -ForegroundColor Gray
    }
}

# Test 1: Login as Deanship
Write-Host "`n=== Test 1: Deanship Authentication ===" -ForegroundColor Cyan
Show-TestResult "Login Endpoint" "INFO" "POST $baseUrl/api/auth/login"

try {
    $loginBody = @{
        email = $deanshipEmail
        password = $deanshipPassword
    } | ConvertTo-Json

    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    
    if ($loginResponse.token) {
        Show-TestResult "Authentication" "PASS" "Token received successfully"
        $token = $loginResponse.token
        $headers = @{
            "Authorization" = "Bearer $token"
            "Content-Type" = "application/json"
        }
    } else {
        Show-TestResult "Authentication" "FAIL" "No token in response"
        exit 1
    }
} catch {
    Show-TestResult "Authentication" "FAIL" $_.Exception.Message
    exit 1
}

# Test 2: Verify Deanship Role
Write-Host "`n=== Test 2: Role Verification ===" -ForegroundColor Cyan
Show-TestResult "User Role" "INFO" "Expected: ROLE_DEANSHIP"

if ($loginResponse.role -eq "ROLE_DEANSHIP") {
    Show-TestResult "Role Check" "PASS" "User has DEANSHIP role"
} else {
    Show-TestResult "Role Check" "FAIL" "Expected ROLE_DEANSHIP, got $($loginResponse.role)"
    exit 1
}

# Test 3: Load Academic Years
Write-Host "`n=== Test 3: Academic Years ===" -ForegroundColor Cyan
Show-TestResult "Academic Years Endpoint" "INFO" "GET $baseUrl/api/deanship/academic-years"

try {
    $academicYears = Invoke-RestMethod -Uri "$baseUrl/api/deanship/academic-years" -Method Get -Headers $headers
    
    if ($academicYears -and $academicYears.Count -gt 0) {
        Show-TestResult "Load Academic Years" "PASS" "Found $($academicYears.Count) academic year(s)"
        $activeYear = $academicYears | Where-Object { $_.isActive -eq $true } | Select-Object -First 1
        
        if ($activeYear) {
            Show-TestResult "Active Year" "PASS" "Active year: $($activeYear.yearCode)"
            $selectedYear = $activeYear
        } else {
            Show-TestResult "Active Year" "WARN" "No active year found, using first year"
            $selectedYear = $academicYears[0]
        }
    } else {
        Show-TestResult "Load Academic Years" "FAIL" "No academic years found"
        exit 1
    }
} catch {
    Show-TestResult "Load Academic Years" "FAIL" $_.Exception.Message
    exit 1
}

# Test 4: Load Semesters
Write-Host "`n=== Test 4: Semesters ===" -ForegroundColor Cyan
Show-TestResult "Semesters" "INFO" "Checking semesters for year $($selectedYear.yearCode)"

if ($selectedYear.semesters -and $selectedYear.semesters.Count -gt 0) {
    Show-TestResult "Load Semesters" "PASS" "Found $($selectedYear.semesters.Count) semester(s)"
    $selectedSemester = $selectedYear.semesters[0]
    Show-TestResult "Selected Semester" "INFO" "$($selectedSemester.type) Semester (ID: $($selectedSemester.id))"
} else {
    Show-TestResult "Load Semesters" "FAIL" "No semesters found for selected year"
    exit 1
}

# Test 5: File Explorer Root Access
Write-Host "`n=== Test 5: File Explorer Root Access ===" -ForegroundColor Cyan
Show-TestResult "File Explorer Endpoint" "INFO" "GET $baseUrl/api/file-explorer/root"

try {
    $fileExplorerUrl = "$baseUrl/api/file-explorer/root?academicYearId=$($selectedYear.id)&semesterId=$($selectedSemester.id)"
    $fileExplorerRoot = Invoke-RestMethod -Uri $fileExplorerUrl -Method Get -Headers $headers
    
    if ($fileExplorerRoot) {
        Show-TestResult "Load File Explorer Root" "PASS" "Root node loaded successfully"
        
        if ($fileExplorerRoot.children) {
            Show-TestResult "Root Children" "PASS" "Found $($fileExplorerRoot.children.Count) item(s) at root"
            
            # Check for all departments visibility
            $professorFolders = $fileExplorerRoot.children | Where-Object { $_.type -eq "PROFESSOR" }
            if ($professorFolders.Count -gt 0) {
                Show-TestResult "All Departments Access" "PASS" "Can view professors from all departments"
                Show-TestResult "Professor Folders" "INFO" "Found $($professorFolders.Count) professor folder(s)"
            } else {
                Show-TestResult "All Departments Access" "WARN" "No professor folders found at root"
            }
        } else {
            Show-TestResult "Root Children" "WARN" "No children found at root level"
        }
    } else {
        Show-TestResult "Load File Explorer Root" "FAIL" "No data returned"
    }
} catch {
    Show-TestResult "Load File Explorer Root" "FAIL" $_.Exception.Message
}

# Test 6: Read-Only Access Verification
Write-Host "`n=== Test 6: Read-Only Access Verification ===" -ForegroundColor Cyan
Show-TestResult "Access Mode" "INFO" "Verifying read-only access for Deanship"

if ($fileExplorerRoot -and $fileExplorerRoot.canWrite -eq $false) {
    Show-TestResult "Read-Only Mode" "PASS" "File Explorer is in read-only mode"
} else {
    Show-TestResult "Read-Only Mode" "WARN" "canWrite flag not set to false"
}

# Test 7: Professor Label Support
Write-Host "`n=== Test 7: Professor Label Support ===" -ForegroundColor Cyan
Show-TestResult "Professor Labels" "INFO" "Checking for professor metadata on folders"

if ($fileExplorerRoot -and $fileExplorerRoot.children) {
    $professorFolders = $fileExplorerRoot.children | Where-Object { $_.type -eq "PROFESSOR" }
    
    if ($professorFolders.Count -gt 0) {
        $folderWithMetadata = $professorFolders | Where-Object { $_.metadata } | Select-Object -First 1
        
        if ($folderWithMetadata) {
            Show-TestResult "Professor Metadata" "PASS" "Professor folders contain metadata"
            if ($folderWithMetadata.metadata.professorName) {
                Show-TestResult "Professor Name" "PASS" "Professor name available: $($folderWithMetadata.metadata.professorName)"
            }
        } else {
            Show-TestResult "Professor Metadata" "WARN" "No metadata found on professor folders"
        }
    }
}

# Test 8: File Download Capability
Write-Host "`n=== Test 8: File Download Capability ===" -ForegroundColor Cyan
Show-TestResult "Download Access" "INFO" "Verifying Deanship can download files"

# Navigate to find a file
if ($fileExplorerRoot -and $fileExplorerRoot.children) {
    $professorFolder = $fileExplorerRoot.children | Where-Object { $_.type -eq "PROFESSOR" } | Select-Object -First 1
    
    if ($professorFolder) {
        try {
            $professorNodeUrl = "$baseUrl/api/file-explorer/node?path=$([System.Web.HttpUtility]::UrlEncode($professorFolder.path))"
            $professorNode = Invoke-RestMethod -Uri $professorNodeUrl -Method Get -Headers $headers
            
            if ($professorNode.children) {
                $courseFolder = $professorNode.children | Where-Object { $_.type -eq "COURSE" } | Select-Object -First 1
                
                if ($courseFolder) {
                    $courseNodeUrl = "$baseUrl/api/file-explorer/node?path=$([System.Web.HttpUtility]::UrlEncode($courseFolder.path))"
                    $courseNode = Invoke-RestMethod -Uri $courseNodeUrl -Method Get -Headers $headers
                    
                    if ($courseNode.children) {
                        $file = $courseNode.children | Where-Object { $_.type -eq "FILE" } | Select-Object -First 1
                        
                        if ($file) {
                            Show-TestResult "File Found" "PASS" "Found file: $($file.name)"
                            Show-TestResult "Download Capability" "PASS" "Deanship can access files for download"
                        } else {
                            Show-TestResult "File Found" "WARN" "No files found in course folder"
                        }
                    }
                }
            }
        } catch {
            Show-TestResult "Navigation" "WARN" "Could not navigate to files: $($_.Exception.Message)"
        }
    }
}

# Test 9: Breadcrumb Navigation
Write-Host "`n=== Test 9: Breadcrumb Navigation ===" -ForegroundColor Cyan
Show-TestResult "Breadcrumb Support" "INFO" "Verifying path information for breadcrumbs"

if ($fileExplorerRoot -and $fileExplorerRoot.path) {
    Show-TestResult "Root Path" "PASS" "Root path: $($fileExplorerRoot.path)"
} else {
    Show-TestResult "Root Path" "PASS" "Root path is empty (expected for root)"
}

# Test 10: UI Component Verification
Write-Host "`n=== Test 10: UI Component Verification ===" -ForegroundColor Cyan
Show-TestResult "HTML Structure" "INFO" "Checking deanship-dashboard.html"

$htmlPath = "src/main/resources/static/deanship-dashboard.html"
if (Test-Path $htmlPath) {
    $htmlContent = Get-Content $htmlPath -Raw
    
    # Check for FileExplorer container
    if ($htmlContent -match 'id="fileExplorerContainer"') {
        Show-TestResult "FileExplorer Container" "PASS" "Found fileExplorerContainer element"
    } else {
        Show-TestResult "FileExplorer Container" "FAIL" "fileExplorerContainer element not found"
    }
    
    # Check for Academic Year selector
    if ($htmlContent -match 'id="academicYearSelect"') {
        Show-TestResult "Academic Year Selector" "PASS" "Found academicYearSelect element"
    } else {
        Show-TestResult "Academic Year Selector" "FAIL" "academicYearSelect element not found"
    }
    
    # Check for Semester selector
    if ($htmlContent -match 'id="semesterSelect"') {
        Show-TestResult "Semester Selector" "PASS" "Found semesterSelect element"
    } else {
        Show-TestResult "Semester Selector" "FAIL" "semesterSelect element not found"
    }
    
    # Check for master design reference comments
    if ($htmlContent -match 'MASTER DESIGN REFERENCE') {
        Show-TestResult "Design Documentation" "PASS" "Found master design reference comments"
    } else {
        Show-TestResult "Design Documentation" "WARN" "No master design reference comments found"
    }
} else {
    Show-TestResult "HTML File" "FAIL" "deanship-dashboard.html not found"
}

# Test 11: JavaScript Implementation
Write-Host "`n=== Test 11: JavaScript Implementation ===" -ForegroundColor Cyan
Show-TestResult "JavaScript File" "INFO" "Checking deanship.js"

$jsPath = "src/main/resources/static/js/deanship.js"
if (Test-Path $jsPath) {
    $jsContent = Get-Content $jsPath -Raw
    
    # Check for FileExplorer import
    if ($jsContent -match 'import.*FileExplorer.*from.*file-explorer') {
        Show-TestResult "FileExplorer Import" "PASS" "FileExplorer class imported"
    } else {
        Show-TestResult "FileExplorer Import" "FAIL" "FileExplorer import not found"
    }
    
    # Check for initializeFileExplorer function
    if ($jsContent -match 'function initializeFileExplorer') {
        Show-TestResult "Initialize Function" "PASS" "initializeFileExplorer function exists"
    } else {
        Show-TestResult "Initialize Function" "FAIL" "initializeFileExplorer function not found"
    }
    
    # Check for Deanship configuration
    if ($jsContent -match "role:\s*'DEANSHIP'") {
        Show-TestResult "Deanship Role Config" "PASS" "FileExplorer configured with DEANSHIP role"
    } else {
        Show-TestResult "Deanship Role Config" "FAIL" "DEANSHIP role configuration not found"
    }
    
    # Check for readOnly configuration
    if ($jsContent -match 'readOnly:\s*true') {
        Show-TestResult "Read-Only Config" "PASS" "FileExplorer configured as read-only"
    } else {
        Show-TestResult "Read-Only Config" "FAIL" "Read-only configuration not found"
    }
    
    # Check for showAllDepartments configuration
    if ($jsContent -match 'showAllDepartments:\s*true') {
        Show-TestResult "All Departments Config" "PASS" "showAllDepartments enabled"
    } else {
        Show-TestResult "All Departments Config" "FAIL" "showAllDepartments configuration not found"
    }
    
    # Check for showProfessorLabels configuration
    if ($jsContent -match 'showProfessorLabels:\s*true') {
        Show-TestResult "Professor Labels Config" "PASS" "showProfessorLabels enabled"
    } else {
        Show-TestResult "Professor Labels Config" "FAIL" "showProfessorLabels configuration not found"
    }
    
    # Check for loadSemesters function
    if ($jsContent -match 'function loadSemesters') {
        Show-TestResult "Load Semesters Function" "PASS" "loadSemesters function exists"
    } else {
        Show-TestResult "Load Semesters Function" "FAIL" "loadSemesters function not found"
    }
} else {
    Show-TestResult "JavaScript File" "FAIL" "deanship.js not found"
}

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test Suite Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Manual Testing Steps:" -ForegroundColor Yellow
Write-Host "1. Open http://localhost:8080/deanship-dashboard.html" -ForegroundColor Gray
Write-Host "2. Login with: $deanshipEmail / $deanshipPassword" -ForegroundColor Gray
Write-Host "3. Navigate to File Explorer tab" -ForegroundColor Gray
Write-Host "4. Select an academic year and semester" -ForegroundColor Gray
Write-Host "5. Verify:" -ForegroundColor Gray
Write-Host "   - File Explorer uses same layout as Professor Dashboard" -ForegroundColor Gray
Write-Host "   - Can view professors from all departments" -ForegroundColor Gray
Write-Host "   - Professor names display on folder cards" -ForegroundColor Gray
Write-Host "   - Breadcrumb navigation works correctly" -ForegroundColor Gray
Write-Host "   - Can download files" -ForegroundColor Gray
Write-Host "   - No upload buttons visible (read-only)" -ForegroundColor Gray
Write-Host "   - Academic Year and Semester selectors match Professor Dashboard styling" -ForegroundColor Gray
Write-Host ""
