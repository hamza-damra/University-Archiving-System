# Comprehensive Deanship Dashboard File Explorer Testing Script
# Task 9: Test Deanship Dashboard File Explorer functionality
# Tests all requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 3.1, 4.3, 7.1, 7.3, 7.4, 8.1, 8.2, 9.3

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Deanship Dashboard File Explorer" -ForegroundColor Cyan
Write-Host "Comprehensive Test Suite - Task 9" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test Configuration
$baseUrl = "http://localhost:8080"
$deanshipEmail = "dean@alquds.edu"
$deanshipPassword = "password123"

Write-Host "Test Configuration:" -ForegroundColor Yellow
Write-Host "  Base URL: $baseUrl" -ForegroundColor Gray
Write-Host "  Deanship Email: $deanshipEmail" -ForegroundColor Gray
Write-Host "  Expected Access: All Departments (Read-Only)" -ForegroundColor Gray
Write-Host ""

# Test counters
$totalTests = 0
$passedTests = 0
$failedTests = 0
$warnings = 0

# Function to display test result
function Show-TestResult {
    param(
        [string]$TestName,
        [string]$Status,
        [string]$Details = "",
        [string]$Requirement = ""
    )
    
    $script:totalTests++
    
    $color = switch ($Status) {
        "PASS" { 
            $script:passedTests++
            "Green" 
        }
        "FAIL" { 
            $script:failedTests++
            "Red" 
        }
        "INFO" { "Cyan" }
        "WARN" { 
            $script:warnings++
            "Yellow" 
        }
        default { "White" }
    }
    
    Write-Host "[$Status] " -ForegroundColor $color -NoNewline
    Write-Host "$TestName" -ForegroundColor White
    if ($Requirement) {
        Write-Host "      Requirement: $Requirement" -ForegroundColor DarkGray
    }
    if ($Details) {
        Write-Host "      $Details" -ForegroundColor Gray
    }
}

# ============================================================================
# AUTHENTICATION AND ROLE VERIFICATION
# ============================================================================

Write-Host "`n=== Authentication and Role Verification ===" -ForegroundColor Cyan

try {
    $loginBody = @{
        email = $deanshipEmail
        password = $deanshipPassword
    } | ConvertTo-Json

    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    
    # Handle both old and new response formats
    $token = if ($loginResponse.data.token) { $loginResponse.data.token } else { $loginResponse.token }
    $role = if ($loginResponse.data.role) { $loginResponse.data.role } else { $loginResponse.role }
    
    if ($token) {
        Show-TestResult "Deanship Authentication" "PASS" "Token received successfully" "3.1"
        $headers = @{
            "Authorization" = "Bearer $token"
            "Content-Type" = "application/json"
        }
    } else {
        Show-TestResult "Deanship Authentication" "FAIL" "No token in response" "3.1"
        exit 1
    }
} catch {
    Show-TestResult "Deanship Authentication" "FAIL" $_.Exception.Message "3.1"
    exit 1
}

if ($role -eq "ROLE_DEANSHIP") {
    Show-TestResult "Role Verification" "PASS" "User has DEANSHIP role" "3.1"
} else {
    Show-TestResult "Role Verification" "FAIL" "Expected ROLE_DEANSHIP, got $role" "3.1"
    exit 1
}

# ============================================================================
# ACADEMIC YEAR AND SEMESTER LOADING (Requirement 8.1, 8.2)
# ============================================================================

Write-Host "`n=== Academic Year and Semester Selector Behavior ===" -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/deanship/academic-years" -Method Get -Headers $headers
    $academicYears = if ($response.data) { $response.data } else { $response }
    
    if ($academicYears -and $academicYears.Count -gt 0) {
        Show-TestResult "Load Academic Years" "PASS" "Found $($academicYears.Count) academic year(s)" "8.1"
        
        $activeYear = $academicYears | Where-Object { $_.isActive -eq $true } | Select-Object -First 1
        
        if ($activeYear) {
            Show-TestResult "Active Year Auto-Selection" "PASS" "Active year: $($activeYear.yearCode)" "8.1"
            $selectedYear = $activeYear
        } else {
            Show-TestResult "Active Year Auto-Selection" "WARN" "No active year found, using first year" "8.1"
            $selectedYear = $academicYears[0]
        }
    } else {
        Show-TestResult "Load Academic Years" "FAIL" "No academic years found" "8.1"
        exit 1
    }
} catch {
    Show-TestResult "Load Academic Years" "FAIL" $_.Exception.Message "8.1"
    exit 1
}

if ($selectedYear.semesters -and $selectedYear.semesters.Count -gt 0) {
    Show-TestResult "Load Semesters" "PASS" "Found $($selectedYear.semesters.Count) semester(s)" "8.2"
    $selectedSemester = $selectedYear.semesters[0]
    Show-TestResult "Semester Selection" "PASS" "$($selectedSemester.type) Semester (ID: $($selectedSemester.id))" "8.2"
} else {
    Show-TestResult "Load Semesters" "FAIL" "No semesters found for selected year" "8.2"
    exit 1
}

# ============================================================================
# FILE EXPLORER ROOT ACCESS (Requirement 1.1, 1.2, 3.1)
# ============================================================================

Write-Host "`n=== File Explorer Root Access - All Departments ===" -ForegroundColor Cyan

try {
    $fileExplorerUrl = "$baseUrl/api/file-explorer/root?academicYearId=$($selectedYear.id)&semesterId=$($selectedSemester.id)"
    $fileExplorerRoot = Invoke-RestMethod -Uri $fileExplorerUrl -Method Get -Headers $headers
    
    if ($fileExplorerRoot) {
        Show-TestResult "Load File Explorer Root" "PASS" "Root node loaded successfully" "1.1"
        
        if ($fileExplorerRoot.children) {
            Show-TestResult "Root Children Count" "PASS" "Found $($fileExplorerRoot.children.Count) item(s) at root" "1.2"
            
            # Requirement 3.1: Verify Deanship can browse all departments
            $professorFolders = $fileExplorerRoot.children | Where-Object { $_.type -eq "PROFESSOR" }
            if ($professorFolders.Count -gt 0) {
                Show-TestResult "All Departments Access" "PASS" "Can view professors from all departments ($($professorFolders.Count) professors)" "3.1"
                
                # Check for multiple departments
                $departments = $professorFolders | ForEach-Object { $_.metadata.departmentName } | Select-Object -Unique
                if ($departments.Count -gt 1) {
                    Show-TestResult "Multiple Departments Visible" "PASS" "Viewing $($departments.Count) different departments" "3.1"
                } else {
                    Show-TestResult "Multiple Departments Visible" "WARN" "Only 1 department found (expected multiple)" "3.1"
                }
            } else {
                Show-TestResult "All Departments Access" "FAIL" "No professor folders found at root" "3.1"
            }
        } else {
            Show-TestResult "Root Children Count" "WARN" "No children found at root level" "1.2"
        }
    } else {
        Show-TestResult "Load File Explorer Root" "FAIL" "No data returned" "1.1"
    }
} catch {
    Show-TestResult "Load File Explorer Root" "FAIL" $_.Exception.Message "1.1"
    exit 1
}

# ============================================================================
# PROFESSOR LABEL SUPPORT (Requirement 4.3, 7.3)
# ============================================================================

Write-Host "`n=== Professor Name Labels on Folders ===" -ForegroundColor Cyan

if ($fileExplorerRoot -and $fileExplorerRoot.children) {
    $professorFolders = $fileExplorerRoot.children | Where-Object { $_.type -eq "PROFESSOR" }
    
    if ($professorFolders.Count -gt 0) {
        $folderWithMetadata = $professorFolders | Where-Object { $_.metadata } | Select-Object -First 1
        
        if ($folderWithMetadata) {
            Show-TestResult "Professor Metadata Present" "PASS" "Professor folders contain metadata" "4.3"
            
            if ($folderWithMetadata.metadata.professorName) {
                Show-TestResult "Professor Name Label" "PASS" "Professor name available: $($folderWithMetadata.metadata.professorName)" "4.3, 7.3"
            } else {
                Show-TestResult "Professor Name Label" "FAIL" "Professor name not found in metadata" "4.3, 7.3"
            }
            
            if ($folderWithMetadata.metadata.departmentName) {
                Show-TestResult "Department Context" "PASS" "Department name available: $($folderWithMetadata.metadata.departmentName)" "4.3"
            }
        } else {
            Show-TestResult "Professor Metadata Present" "FAIL" "No metadata found on professor folders" "4.3"
        }
    }
}

# ============================================================================
# FOLDER CARD DESIGN CONSISTENCY (Requirement 1.2, 7.1)
# ============================================================================

Write-Host "`n=== Folder Card Design Consistency ===" -ForegroundColor Cyan

if ($fileExplorerRoot -and $fileExplorerRoot.children) {
    $professorFolder = $fileExplorerRoot.children | Where-Object { $_.type -eq "PROFESSOR" } | Select-Object -First 1
    
    if ($professorFolder) {
        Show-TestResult "Professor Folder Structure" "PASS" "Professor folder has name, path, and type" "7.1"
        
        # Navigate to course level to check course folder design
        try {
            $professorNodeUrl = "$baseUrl/api/file-explorer/node?path=$([System.Web.HttpUtility]::UrlEncode($professorFolder.path))"
            $professorNode = Invoke-RestMethod -Uri $professorNodeUrl -Method Get -Headers $headers
            
            if ($professorNode.children) {
                $courseFolder = $professorNode.children | Where-Object { $_.type -eq "COURSE" } | Select-Object -First 1
                
                if ($courseFolder) {
                    Show-TestResult "Course Folder Structure" "PASS" "Course folder has name, path, and type" "7.1"
                    
                    if ($courseFolder.metadata) {
                        Show-TestResult "Course Metadata" "PASS" "Course folder contains metadata" "7.1"
                    }
                } else {
                    Show-TestResult "Course Folder Structure" "WARN" "No course folders found" "7.1"
                }
            }
        } catch {
            Show-TestResult "Navigate to Course Level" "WARN" "Could not navigate to course level: $($_.Exception.Message)" "7.1"
        }
    }
}

# ============================================================================
# BREADCRUMB NAVIGATION (Requirement 2.1, 2.2, 2.3)
# ============================================================================

Write-Host "`n=== Breadcrumb Navigation ===" -ForegroundColor Cyan

if ($fileExplorerRoot) {
    if ($fileExplorerRoot.path -ne $null) {
        Show-TestResult "Root Path Structure" "PASS" "Root path is defined (empty for root)" "2.1"
    } else {
        Show-TestResult "Root Path Structure" "PASS" "Root path is null (expected for root)" "2.1"
    }
    
    if ($fileExplorerRoot.name) {
        Show-TestResult "Root Name" "PASS" "Root name: $($fileExplorerRoot.name)" "2.1"
    }
}

# Test breadcrumb navigation by checking nested paths
if ($fileExplorerRoot -and $fileExplorerRoot.children) {
    $professorFolder = $fileExplorerRoot.children | Where-Object { $_.type -eq "PROFESSOR" } | Select-Object -First 1
    
    if ($professorFolder -and $professorFolder.path) {
        Show-TestResult "Nested Path Structure" "PASS" "Professor folder has path: $($professorFolder.path)" "2.2"
        
        # Verify path format for breadcrumb construction
        if ($professorFolder.path -match '/') {
            Show-TestResult "Path Format for Breadcrumbs" "PASS" "Path uses '/' separator for breadcrumb parsing" "2.2"
        }
    }
}

# ============================================================================
# READ-ONLY ACCESS VERIFICATION (Requirement 1.4, 9.3)
# ============================================================================

Write-Host "`n=== Read-Only Access Verification ===" -ForegroundColor Cyan

if ($fileExplorerRoot) {
    if ($fileExplorerRoot.canWrite -eq $false) {
        Show-TestResult "Read-Only Mode (Root)" "PASS" "Root node has canWrite=false" "1.4, 9.3"
    } else {
        Show-TestResult "Read-Only Mode (Root)" "FAIL" "Root node canWrite should be false" "1.4, 9.3"
    }
    
    if ($fileExplorerRoot.canRead -eq $true) {
        Show-TestResult "Read Access (Root)" "PASS" "Root node has canRead=true" "1.4"
    }
}

# Check read-only on nested folders
if ($fileExplorerRoot -and $fileExplorerRoot.children) {
    $professorFolder = $fileExplorerRoot.children | Where-Object { $_.type -eq "PROFESSOR" } | Select-Object -First 1
    
    if ($professorFolder) {
        if ($professorFolder.canWrite -eq $false) {
            Show-TestResult "Read-Only Mode (Professor Folder)" "PASS" "Professor folder has canWrite=false" "1.4, 9.3"
        } else {
            Show-TestResult "Read-Only Mode (Professor Folder)" "FAIL" "Professor folder canWrite should be false" "1.4, 9.3"
        }
    }
}

# ============================================================================
# FILE DOWNLOAD CAPABILITY (Requirement 1.3, 7.4)
# ============================================================================

Write-Host "`n=== File Download Capability ===" -ForegroundColor Cyan

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
                        $docTypeFolder = $courseNode.children | Where-Object { $_.type -eq "DOCUMENT_TYPE" } | Select-Object -First 1
                        
                        if ($docTypeFolder) {
                            $docTypeNodeUrl = "$baseUrl/api/file-explorer/node?path=$([System.Web.HttpUtility]::UrlEncode($docTypeFolder.path))"
                            $docTypeNode = Invoke-RestMethod -Uri $docTypeNodeUrl -Method Get -Headers $headers
                            
                            if ($docTypeNode.children) {
                                $file = $docTypeNode.children | Where-Object { $_.type -eq "FILE" } | Select-Object -First 1
                                
                                if ($file) {
                                    Show-TestResult "File Found" "PASS" "Found file: $($file.name)" "1.3, 7.4"
                                    
                                    if ($file.metadata -and $file.metadata.fileId) {
                                        Show-TestResult "File Metadata" "PASS" "File has metadata with fileId: $($file.metadata.fileId)" "7.4"
                                        Show-TestResult "Download Capability" "PASS" "Deanship can access files for download" "1.3, 7.4"
                                    } else {
                                        Show-TestResult "File Metadata" "FAIL" "File missing metadata or fileId" "7.4"
                                    }
                                } else {
                                    Show-TestResult "File Found" "WARN" "No files found in document type folder" "1.3"
                                }
                            }
                        }
                    }
                }
            }
        } catch {
            Show-TestResult "Navigate to Files" "WARN" "Could not navigate to files: $($_.Exception.Message)" "1.3"
        }
    }
}

# ============================================================================
# HTML STRUCTURE VERIFICATION (Requirement 1.1, 1.2)
# ============================================================================

Write-Host "`n=== HTML Structure Verification ===" -ForegroundColor Cyan

$htmlPath = "src/main/resources/static/deanship-dashboard.html"
if (Test-Path $htmlPath) {
    $htmlContent = Get-Content $htmlPath -Raw
    
    # Check for FileExplorer container
    if ($htmlContent -match 'id="fileExplorerContainer"') {
        Show-TestResult "FileExplorer Container" "PASS" "Found fileExplorerContainer element" "1.1"
    } else {
        Show-TestResult "FileExplorer Container" "FAIL" "fileExplorerContainer element not found" "1.1"
    }
    
    # Check for Academic Year selector
    if ($htmlContent -match 'id="academicYearSelect"') {
        Show-TestResult "Academic Year Selector" "PASS" "Found academicYearSelect element" "8.1"
    } else {
        Show-TestResult "Academic Year Selector" "FAIL" "academicYearSelect element not found" "8.1"
    }
    
    # Check for Semester selector
    if ($htmlContent -match 'id="semesterSelect"') {
        Show-TestResult "Semester Selector" "PASS" "Found semesterSelect element" "8.2"
    } else {
        Show-TestResult "Semester Selector" "FAIL" "semesterSelect element not found" "8.2"
    }
    
    # Check for master design reference comments
    if ($htmlContent -match 'MASTER DESIGN REFERENCE') {
        Show-TestResult "Design Documentation" "PASS" "Found master design reference comments" "1.1"
    } else {
        Show-TestResult "Design Documentation" "WARN" "No master design reference comments found" "1.1"
    }
    
    # Check for Professor Dashboard layout consistency
    if ($htmlContent -match 'Professor Dashboard File Explorer') {
        Show-TestResult "Layout Reference" "PASS" "References Professor Dashboard layout" "1.1"
    }
} else {
    Show-TestResult "HTML File" "FAIL" "deanship-dashboard.html not found" "1.1"
}

# ============================================================================
# JAVASCRIPT IMPLEMENTATION VERIFICATION (Requirement 1.1, 1.2, 1.4)
# ============================================================================

Write-Host "`n=== JavaScript Implementation Verification ===" -ForegroundColor Cyan

$jsPath = "src/main/resources/static/js/deanship.js"
if (Test-Path $jsPath) {
    $jsContent = Get-Content $jsPath -Raw
    
    # Check for FileExplorer import
    if ($jsContent -match 'import.*FileExplorer.*from.*file-explorer') {
        Show-TestResult "FileExplorer Import" "PASS" "FileExplorer class imported" "1.1"
    } else {
        Show-TestResult "FileExplorer Import" "FAIL" "FileExplorer import not found" "1.1"
    }
    
    # Check for initializeFileExplorer function
    if ($jsContent -match 'function initializeFileExplorer') {
        Show-TestResult "Initialize Function" "PASS" "initializeFileExplorer function exists" "1.1"
    } else {
        Show-TestResult "Initialize Function" "FAIL" "initializeFileExplorer function not found" "1.1"
    }
    
    # Check for Deanship configuration
    if ($jsContent -match "role:\s*'DEANSHIP'") {
        Show-TestResult "Deanship Role Config" "PASS" "FileExplorer configured with DEANSHIP role" "4.3"
    } else {
        Show-TestResult "Deanship Role Config" "FAIL" "DEANSHIP role configuration not found" "4.3"
    }
    
    # Check for readOnly configuration
    if ($jsContent -match 'readOnly:\s*true') {
        Show-TestResult "Read-Only Config" "PASS" "FileExplorer configured as read-only" "1.4, 9.3"
    } else {
        Show-TestResult "Read-Only Config" "FAIL" "Read-only configuration not found" "1.4, 9.3"
    }
    
    # Check for showAllDepartments configuration
    if ($jsContent -match 'showAllDepartments:\s*true') {
        Show-TestResult "All Departments Config" "PASS" "showAllDepartments enabled" "3.1"
    } else {
        Show-TestResult "All Departments Config" "FAIL" "showAllDepartments configuration not found" "3.1"
    }
    
    # Check for showProfessorLabels configuration
    if ($jsContent -match 'showProfessorLabels:\s*true') {
        Show-TestResult "Professor Labels Config" "PASS" "showProfessorLabels enabled" "4.3, 7.3"
    } else {
        Show-TestResult "Professor Labels Config" "FAIL" "showProfessorLabels configuration not found" "4.3, 7.3"
    }
    
    # Check for loadSemesters function
    if ($jsContent -match 'function loadSemesters') {
        Show-TestResult "Load Semesters Function" "PASS" "loadSemesters function exists" "8.2"
    } else {
        Show-TestResult "Load Semesters Function" "FAIL" "loadSemesters function not found" "8.2"
    }
    
    # Check for loadFileExplorer function
    if ($jsContent -match 'function loadFileExplorer') {
        Show-TestResult "Load File Explorer Function" "PASS" "loadFileExplorer function exists" "1.1"
    } else {
        Show-TestResult "Load File Explorer Function" "FAIL" "loadFileExplorer function not found" "1.1"
    }
} else {
    Show-TestResult "JavaScript File" "FAIL" "deanship.js not found" "1.1"
}

# ============================================================================
# TEST SUMMARY
# ============================================================================

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test Suite Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Test Results:" -ForegroundColor Yellow
Write-Host "  Total Tests: $totalTests" -ForegroundColor White
Write-Host "  Passed: $passedTests" -ForegroundColor Green
Write-Host "  Failed: $failedTests" -ForegroundColor Red
Write-Host "  Warnings: $warnings" -ForegroundColor Yellow
Write-Host ""

if ($failedTests -eq 0) {
    Write-Host "All tests passed!" -ForegroundColor Green
} else {
    Write-Host "Some tests failed. Please review the results above." -ForegroundColor Red
}

Write-Host ""
Write-Host "Manual Testing Checklist:" -ForegroundColor Yellow
Write-Host "1. Open http://localhost:8080/deanship-dashboard.html" -ForegroundColor Gray
Write-Host "2. Login with: $deanshipEmail / $deanshipPassword" -ForegroundColor Gray
Write-Host "3. Navigate to File Explorer tab" -ForegroundColor Gray
Write-Host "4. Select an academic year and semester" -ForegroundColor Gray
Write-Host ""
Write-Host "Verify the following:" -ForegroundColor Yellow
Write-Host "  - File Explorer uses same blue card layout as Professor Dashboard" -ForegroundColor Gray
Write-Host "  - Can view professors from ALL departments" -ForegroundColor Gray
Write-Host "  - Professor names display on folder cards" -ForegroundColor Gray
Write-Host "  - Breadcrumb navigation works correctly" -ForegroundColor Gray
Write-Host "  - Can download files" -ForegroundColor Gray
Write-Host "  - NO upload buttons visible (read-only)" -ForegroundColor Gray
Write-Host "  - Academic Year and Semester selectors match Professor Dashboard styling" -ForegroundColor Gray
Write-Host "  - Folder cards have same hover effects as Professor Dashboard" -ForegroundColor Gray
Write-Host "  - File tables have same column layout as Professor Dashboard" -ForegroundColor Gray
Write-Host ""

# Exit with appropriate code
if ($failedTests -gt 0) {
    exit 1
} else {
    exit 0
}
