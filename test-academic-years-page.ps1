# Test Academic Years Management Page
# Tests the new multi-page academic years management functionality

$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Academic Years Page Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Function to display test results
function Show-TestResult {
    param(
        [string]$TestName,
        [string]$Status,
        [string]$Message = ""
    )
    
    $color = switch ($Status) {
        "PASS" { "Green" }
        "FAIL" { "Red" }
        "INFO" { "Cyan" }
        default { "Yellow" }
    }
    
    Write-Host "[$Status] " -ForegroundColor $color -NoNewline
    Write-Host "$TestName" -NoNewline
    if ($Message) {
        Write-Host " - $Message" -ForegroundColor Gray
    } else {
        Write-Host ""
    }
}

# Test 1: Login as Deanship user
Write-Host "`n=== Test 1: Authentication ===" -ForegroundColor Cyan
Show-TestResult "Login Endpoint" "INFO" "POST $baseUrl/auth/login"

try {
    $loginBody = @{
        email = "dean@alquds.edu"
        password = "dean123"
    } | ConvertTo-Json

    $loginResponse = Invoke-WebRequest -Uri "$baseUrl/auth/login" `
        -Method Post `
        -Body $loginBody `
        -ContentType "application/json" `
        -SessionVariable session `
        -UseBasicParsing

    if ($loginResponse.StatusCode -eq 200) {
        Show-TestResult "Login" "PASS" "Successfully authenticated as deanship user"
        
        # Extract session cookie
        $sessionCookie = $session.Cookies.GetCookies($baseUrl) | Where-Object { $_.Name -eq "ARCHIVESESSION" }
        if ($sessionCookie) {
            Show-TestResult "Session Cookie" "PASS" "Cookie: $($sessionCookie.Value.Substring(0, 20))..."
        }
    } else {
        Show-TestResult "Login" "FAIL" "Status: $($loginResponse.StatusCode)"
        exit 1
    }
} catch {
    Show-TestResult "Login" "FAIL" $_.Exception.Message
    exit 1
}

# Prepare headers with session cookie
$headers = @{
    "Cookie" = "ARCHIVESESSION=$($sessionCookie.Value)"
}

# Test 2: Access Academic Years Page
Write-Host "`n=== Test 2: Page Access ===" -ForegroundColor Cyan
Show-TestResult "Academic Years Page" "INFO" "GET $baseUrl/deanship/academic-years"

try {
    $pageResponse = Invoke-WebRequest -Uri "$baseUrl/deanship/academic-years" `
        -Method Get `
        -Headers $headers `
        -UseBasicParsing

    if ($pageResponse.StatusCode -eq 200) {
        Show-TestResult "Page Load" "PASS" "Page loaded successfully"
        
        # Check for key elements in HTML
        $content = $pageResponse.Content
        
        if ($content -match "Academic Years Management") {
            Show-TestResult "Page Title" "PASS" "Found page title"
        } else {
            Show-TestResult "Page Title" "FAIL" "Page title not found"
        }
        
        if ($content -match "addAcademicYearBtn") {
            Show-TestResult "Add Button" "PASS" "Found add academic year button"
        } else {
            Show-TestResult "Add Button" "FAIL" "Add button not found"
        }
        
        if ($content -match "academicYearsTableBody") {
            Show-TestResult "Table Element" "PASS" "Found table body element"
        } else {
            Show-TestResult "Table Element" "FAIL" "Table body not found"
        }
        
        if ($content -match "academic-years\.js") {
            Show-TestResult "JavaScript Module" "PASS" "Found academic-years.js reference"
        } else {
            Show-TestResult "JavaScript Module" "FAIL" "JavaScript module not found"
        }
        
    } else {
        Show-TestResult "Page Load" "FAIL" "Status: $($pageResponse.StatusCode)"
    }
} catch {
    Show-TestResult "Page Load" "FAIL" $_.Exception.Message
}

# Test 3: Load Academic Years Data
Write-Host "`n=== Test 3: API Endpoints ===" -ForegroundColor Cyan
Show-TestResult "Academic Years API" "INFO" "GET $baseUrl/api/deanship/academic-years"

try {
    $apiResponse = Invoke-RestMethod -Uri "$baseUrl/api/deanship/academic-years" `
        -Method Get `
        -Headers $headers

    if ($apiResponse) {
        $count = if ($apiResponse -is [Array]) { $apiResponse.Count } else { 1 }
        Show-TestResult "Load Academic Years" "PASS" "Found $count academic year(s)"
        
        if ($count -gt 0) {
            $firstYear = if ($apiResponse -is [Array]) { $apiResponse[0] } else { $apiResponse }
            Show-TestResult "Year Data" "INFO" "Year Code: $($firstYear.yearCode), Active: $($firstYear.isActive)"
        }
    } else {
        Show-TestResult "Load Academic Years" "INFO" "No academic years found (empty database)"
    }
} catch {
    Show-TestResult "Load Academic Years" "FAIL" $_.Exception.Message
}

# Test 4: Navigation Links
Write-Host "`n=== Test 4: Navigation ===" -ForegroundColor Cyan

try {
    $pageResponse = Invoke-WebRequest -Uri "$baseUrl/deanship/academic-years" `
        -Method Get `
        -Headers $headers `
        -UseBasicParsing
    
    $content = $pageResponse.Content
    
    $navLinks = @(
        @{ Name = "Dashboard"; Path = "/deanship/dashboard" },
        @{ Name = "Academic Years"; Path = "/deanship/academic-years" },
        @{ Name = "Professors"; Path = "/deanship/professors" },
        @{ Name = "Courses"; Path = "/deanship/courses" },
        @{ Name = "Assignments"; Path = "/deanship/course-assignments" },
        @{ Name = "Reports"; Path = "/deanship/reports" },
        @{ Name = "File Explorer"; Path = "/deanship/file-explorer" }
    )
    
    foreach ($link in $navLinks) {
        if ($content -match [regex]::Escape($link.Path)) {
            Show-TestResult "$($link.Name) Link" "PASS" "Found navigation link"
        } else {
            Show-TestResult "$($link.Name) Link" "FAIL" "Navigation link not found"
        }
    }
} catch {
    Show-TestResult "Navigation Check" "FAIL" $_.Exception.Message
}

# Test 5: Shared Layout Elements
Write-Host "`n=== Test 5: Shared Layout ===" -ForegroundColor Cyan

try {
    $pageResponse = Invoke-WebRequest -Uri "$baseUrl/deanship/academic-years" `
        -Method Get `
        -Headers $headers `
        -UseBasicParsing
    
    $content = $pageResponse.Content
    
    # Check for header
    if ($content -match "deanship-header") {
        Show-TestResult "Header" "PASS" "Found shared header"
    } else {
        Show-TestResult "Header" "FAIL" "Header not found"
    }
    
    # Check for navigation
    if ($content -match "deanship-nav") {
        Show-TestResult "Navigation Bar" "PASS" "Found navigation bar"
    } else {
        Show-TestResult "Navigation Bar" "FAIL" "Navigation bar not found"
    }
    
    # Check for filters
    if ($content -match "deanship-filters") {
        Show-TestResult "Global Filters" "PASS" "Found academic year/semester filters"
    } else {
        Show-TestResult "Global Filters" "FAIL" "Filters not found"
    }
    
    # Check for logout button
    if ($content -match "logoutBtn") {
        Show-TestResult "Logout Button" "PASS" "Found logout button"
    } else {
        Show-TestResult "Logout Button" "FAIL" "Logout button not found"
    }
    
    # Check for CSS
    if ($content -match "deanship-layout\.css") {
        Show-TestResult "Layout CSS" "PASS" "Found deanship-layout.css"
    } else {
        Show-TestResult "Layout CSS" "FAIL" "Layout CSS not found"
    }
    
    # Check for common JS
    if ($content -match "deanship-common\.js") {
        Show-TestResult "Common JS" "PASS" "Found deanship-common.js reference"
    } else {
        Show-TestResult "Common JS" "INFO" "Common JS loaded via module import"
    }
    
} catch {
    Show-TestResult "Layout Check" "FAIL" $_.Exception.Message
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Open browser to: $baseUrl/deanship/academic-years" -ForegroundColor Gray
Write-Host "2. Login with dean@alquds.edu / dean123" -ForegroundColor Gray
Write-Host "3. Verify page loads and displays academic years" -ForegroundColor Gray
Write-Host "4. Test Add, Edit, and Activate functionality" -ForegroundColor Gray
Write-Host ""
