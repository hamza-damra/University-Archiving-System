# Comprehensive Authentication Flow Test Script
# Tests all authentication and authorization scenarios for deanship multi-page dashboard

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Authentication Flow Comprehensive Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"
$testsPassed = 0
$testsFailed = 0

# Test 1: Login redirect points to /deanship/dashboard
Write-Host "Test 1: Verify login redirect to /deanship/dashboard" -ForegroundColor Yellow
Write-Host "Action: Check auth.js for correct redirect URL" -ForegroundColor Gray

$authJsContent = Get-Content "src/main/resources/static/js/auth.js" -Raw
if ($authJsContent -match "/deanship/dashboard") {
    Write-Host "PASS: Login redirects to /deanship/dashboard for DEANSHIP role" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "FAIL: Login does not redirect to /deanship/dashboard" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Test 2: SecurityConfig protects /deanship/* routes
Write-Host "Test 2: Verify SecurityConfig protects /deanship/* routes" -ForegroundColor Yellow
Write-Host "Action: Check SecurityConfig.java for proper authorization" -ForegroundColor Gray

$securityConfigContent = Get-Content "src/main/java/com/alqude/edu/ArchiveSystem/config/SecurityConfig.java" -Raw
if ($securityConfigContent -match 'requestMatchers.*deanship.*hasRole.*DEANSHIP') {
    Write-Host "PASS: /deanship/** routes require DEANSHIP role" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "FAIL: /deanship/** routes not properly protected" -ForegroundColor Red
    $testsFailed++
}

# Check that new deanship HTML files in /deanship/ folder are not publicly accessible
# Note: /deanship-dashboard.html is kept for backward compatibility/rollback
if ($securityConfigContent -notmatch "deanship/.*html.*permitAll") {
    Write-Host "PASS: New deanship HTML files in /deanship/ folder are protected" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "FAIL: Deanship HTML files in /deanship/ folder are publicly accessible" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Test 3: DeanshipViewController has @PreAuthorize annotation
Write-Host "Test 3: Verify DeanshipViewController has security annotations" -ForegroundColor Yellow
Write-Host "Action: Check DeanshipViewController.java for @PreAuthorize" -ForegroundColor Gray

$viewControllerContent = Get-Content "src/main/java/com/alqude/edu/ArchiveSystem/controller/DeanshipViewController.java" -Raw
if ($viewControllerContent -match "@PreAuthorize") {
    Write-Host "PASS: DeanshipViewController has @PreAuthorize annotation" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "FAIL: DeanshipViewController missing @PreAuthorize annotation" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Test 4: Access denied handler redirects to login
Write-Host "Test 4: Verify access denied handler redirects to login" -ForegroundColor Yellow
Write-Host "Action: Check DeanshipViewController for AccessDeniedException handler" -ForegroundColor Gray

if ($viewControllerContent -match "ExceptionHandler.*AccessDeniedException") {
    Write-Host "PASS: AccessDeniedException handler exists" -ForegroundColor Green
    $testsPassed++
    
    if ($viewControllerContent -match "redirect:/index.html.*error=access_denied") {
        Write-Host "PASS: Handler redirects to /index.html with error parameter" -ForegroundColor Green
        $testsPassed++
    } else {
        Write-Host "FAIL: Handler does not redirect to correct URL" -ForegroundColor Red
        $testsFailed++
    }
} else {
    Write-Host "FAIL: AccessDeniedException handler not found" -ForegroundColor Red
    $testsFailed += 2
}
Write-Host ""

# Test 5: Login page displays error messages
Write-Host "Test 5: Verify login page displays error messages from URL" -ForegroundColor Yellow
Write-Host "Action: Check auth.js for error parameter handling" -ForegroundColor Gray

if ($authJsContent -match "urlParams.get.*error") {
    Write-Host "PASS: auth.js reads error parameter from URL" -ForegroundColor Green
    $testsPassed++
    
    if ($authJsContent -match "access_denied") {
        Write-Host "PASS: Handles access_denied error" -ForegroundColor Green
        $testsPassed++
    } else {
        Write-Host "FAIL: Does not handle access_denied error" -ForegroundColor Red
        $testsFailed++
    }
} else {
    Write-Host "FAIL: auth.js does not read error parameter" -ForegroundColor Red
    $testsFailed += 2
}
Write-Host ""

# Test 6: Logout functionality clears auth data
Write-Host "Test 6: Verify logout clears authentication state" -ForegroundColor Yellow
Write-Host "Action: Check deanship-common.js logout method" -ForegroundColor Gray

$commonJsContent = Get-Content "src/main/resources/static/js/deanship-common.js" -Raw
if ($commonJsContent -match "clearAuthData") {
    Write-Host "PASS: Logout calls clearAuthData()" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "FAIL: Logout does not clear auth data" -ForegroundColor Red
    $testsFailed++
}

if ($commonJsContent -match "deanship_selected_academic_year") {
    Write-Host "PASS: Logout clears deanship-specific localStorage" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "FAIL: Logout does not clear deanship localStorage" -ForegroundColor Red
    $testsFailed++
}

if ($commonJsContent -match "/index.html") {
    Write-Host "PASS: Logout redirects to login page" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "FAIL: Logout does not redirect to login page" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Test 7: DeanshipLayout checks authentication
Write-Host "Test 7: Verify DeanshipLayout checks authentication on initialize" -ForegroundColor Yellow
Write-Host "Action: Check deanship-common.js initialize method" -ForegroundColor Gray

if ($commonJsContent -match "isAuthenticated") {
    Write-Host "PASS: DeanshipLayout checks if user is authenticated" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "FAIL: DeanshipLayout does not check authentication" -ForegroundColor Red
    $testsFailed++
}

if ($commonJsContent -match "ROLE_DEANSHIP") {
    Write-Host "PASS: DeanshipLayout verifies DEANSHIP role" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "FAIL: DeanshipLayout does not verify role" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Test 8: API request handles 401 responses
Write-Host "Test 8: Verify API handles 401 unauthorized responses" -ForegroundColor Yellow
Write-Host "Action: Check api.js for 401 handling" -ForegroundColor Gray

$apiJsContent = Get-Content "src/main/resources/static/js/api.js" -Raw
if ($apiJsContent -match "status.*401") {
    Write-Host "PASS: API checks for 401 status" -ForegroundColor Green
    $testsPassed++
    
    if ($apiJsContent -match "redirectToLogin") {
        Write-Host "PASS: API redirects to login on 401" -ForegroundColor Green
        $testsPassed++
    } else {
        Write-Host "FAIL: API does not redirect on 401" -ForegroundColor Red
        $testsFailed++
    }
} else {
    Write-Host "FAIL: API does not handle 401 responses" -ForegroundColor Red
    $testsFailed += 2
}
Write-Host ""

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Tests Passed: $testsPassed" -ForegroundColor Green
Write-Host "Tests Failed: $testsFailed" -ForegroundColor $(if ($testsFailed -eq 0) { "Green" } else { "Red" })
Write-Host ""

if ($testsFailed -eq 0) {
    Write-Host "All authentication flow tests passed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Manual Testing Required:" -ForegroundColor Yellow
    Write-Host "1. Start the application: mvnw spring-boot:run" -ForegroundColor Gray
    Write-Host "2. Try accessing http://localhost:8080/deanship/dashboard without login" -ForegroundColor Gray
    Write-Host "   Expected: Redirect to login page" -ForegroundColor Gray
    Write-Host "3. Login with non-deanship user (HOD or Professor)" -ForegroundColor Gray
    Write-Host "   Expected: Access denied message" -ForegroundColor Gray
    Write-Host "4. Login with deanship user" -ForegroundColor Gray
    Write-Host "   Expected: Redirect to /deanship/dashboard" -ForegroundColor Gray
    Write-Host "5. Click logout from any deanship page" -ForegroundColor Gray
    Write-Host "   Expected: Redirect to login, all auth data cleared" -ForegroundColor Gray
    Write-Host "6. After logout, try browser back button" -ForegroundColor Gray
    Write-Host "   Expected: Redirect to login (session expired)" -ForegroundColor Gray
} else {
    Write-Host "Some tests failed. Please review the output above." -ForegroundColor Red
    exit 1
}
