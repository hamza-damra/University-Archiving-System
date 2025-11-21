# Test Authentication Flow and Entry Points
# Tests for Task 12: Update authentication flow and entry points

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Authentication Flow Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"
$testsPassed = 0
$testsFailed = 0

# Test 1: Login and verify redirect to new dashboard
Write-Host "Test 1: Login success redirects to /deanship/dashboard" -ForegroundColor Yellow
Write-Host "Action: Check auth.js redirect logic" -ForegroundColor Gray

$authJsContent = Get-Content "src/main/resources/static/js/auth.js" -Raw
if ($authJsContent -match "/deanship/dashboard") {
    Write-Host "[PASS] auth.js redirects to /deanship/dashboard" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "[FAIL] auth.js does not redirect to /deanship/dashboard" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Test 2: Verify DeanshipViewController has security annotation
Write-Host "Test 2: DeanshipViewController requires DEANSHIP role" -ForegroundColor Yellow
Write-Host "Action: Check PreAuthorize annotation" -ForegroundColor Gray

$controllerContent = Get-Content "src/main/java/com/alqude/edu/ArchiveSystem/controller/DeanshipViewController.java" -Raw
if ($controllerContent -match "PreAuthorize.*DEANSHIP") {
    Write-Host "[PASS] Controller has PreAuthorize annotation" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "[FAIL] Controller missing PreAuthorize annotation" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Test 3: Verify AccessDeniedException handler exists
Write-Host "Test 3: Controller has AccessDeniedException handler" -ForegroundColor Yellow
Write-Host "Action: Check exception handler" -ForegroundColor Gray

if ($controllerContent -match "ExceptionHandler.*AccessDeniedException") {
    Write-Host "[PASS] Controller has AccessDeniedException handler" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "[FAIL] Controller missing AccessDeniedException handler" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Test 4: Verify deanship-common.js checks authentication
Write-Host "Test 4: deanship-common.js checks authentication" -ForegroundColor Yellow
Write-Host "Action: Check authentication logic" -ForegroundColor Gray

$commonJsContent = Get-Content "src/main/resources/static/js/deanship-common.js" -Raw
if ($commonJsContent -match "isAuthenticated") {
    Write-Host "[PASS] deanship-common.js checks authentication" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "[FAIL] deanship-common.js missing authentication check" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Test 5: Verify role check in deanship-common.js
Write-Host "Test 5: deanship-common.js verifies ROLE_DEANSHIP" -ForegroundColor Yellow
Write-Host "Action: Check role verification" -ForegroundColor Gray

if ($commonJsContent -match "ROLE_DEANSHIP") {
    Write-Host "[PASS] deanship-common.js checks for ROLE_DEANSHIP" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "[FAIL] deanship-common.js missing or incorrect role check" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Test 6: Verify logout clears authentication state
Write-Host "Test 6: Logout clears authentication state" -ForegroundColor Yellow
Write-Host "Action: Check logout function" -ForegroundColor Gray

if ($commonJsContent -match "clearAuthData") {
    Write-Host "[PASS] Logout calls clearAuthData()" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "[FAIL] Logout does not clear auth data" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Test 7: Verify logout clears deanship-specific localStorage
Write-Host "Test 7: Logout clears deanship localStorage" -ForegroundColor Yellow
Write-Host "Action: Check localStorage cleanup" -ForegroundColor Gray

if ($commonJsContent -match "deanship_selected_academic_year" -and
    $commonJsContent -match "deanship_selected_semester" -and
    $commonJsContent -match "deanship_last_page") {
    Write-Host "[PASS] Logout clears all deanship localStorage items" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "[FAIL] Logout does not clear all deanship localStorage" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Test 8: Verify SecurityConfig protects /deanship/* routes
Write-Host "Test 8: SecurityConfig protects /deanship/* routes" -ForegroundColor Yellow
Write-Host "Action: Check security configuration" -ForegroundColor Gray

$securityConfigContent = Get-Content "src/main/java/com/alqude/edu/ArchiveSystem/config/SecurityConfig.java" -Raw
if ($securityConfigContent -match "/deanship/\*\*.*hasRole.*DEANSHIP") {
    Write-Host "[PASS] SecurityConfig protects /deanship/** routes" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "[FAIL] SecurityConfig missing /deanship/** protection" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Test 9: Verify API endpoints are protected
Write-Host "Test 9: API endpoints require DEANSHIP role" -ForegroundColor Yellow
Write-Host "Action: Check API security configuration" -ForegroundColor Gray

if ($securityConfigContent -match "/api/deanship/\*\*.*hasRole.*DEANSHIP") {
    Write-Host "[PASS] API endpoints protected with DEANSHIP role" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "[FAIL] API endpoints not properly protected" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Test 10: Verify redirectToLogin function exists in api.js
Write-Host "Test 10: api.js has redirectToLogin function" -ForegroundColor Yellow
Write-Host "Action: Check api.js for redirect function" -ForegroundColor Gray

$apiJsContent = Get-Content "src/main/resources/static/js/api.js" -Raw
if ($apiJsContent -match "function redirectToLogin") {
    Write-Host "[PASS] api.js exports redirectToLogin function" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "[FAIL] api.js missing redirectToLogin function" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Test 11: Verify 401 responses trigger redirect to login
Write-Host "Test 11: 401 responses redirect to login" -ForegroundColor Yellow
Write-Host "Action: Check 401 handling in api.js" -ForegroundColor Gray

if ($apiJsContent -match "401" -and $apiJsContent -match "redirectToLogin") {
    Write-Host "[PASS] 401 responses trigger redirectToLogin()" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "[FAIL] 401 responses not properly handled" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Tests Passed: $testsPassed" -ForegroundColor Green
Write-Host "Tests Failed: $testsFailed" -ForegroundColor Red
Write-Host ""

if ($testsFailed -eq 0) {
    Write-Host "[SUCCESS] All authentication flow tests passed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Manual Testing Required:" -ForegroundColor Yellow
    Write-Host "1. Start the application: mvnw spring-boot:run" -ForegroundColor Gray
    Write-Host "2. Navigate to http://localhost:8080" -ForegroundColor Gray
    Write-Host "3. Login with deanship credentials" -ForegroundColor Gray
    Write-Host "4. Verify redirect to /deanship/dashboard" -ForegroundColor Gray
    Write-Host "5. Try accessing /deanship/professors without login (should redirect to login)" -ForegroundColor Gray
    Write-Host "6. Login as HOD or Professor and try accessing /deanship/dashboard (should show access denied)" -ForegroundColor Gray
    Write-Host "7. Click logout from any deanship page and verify redirect to login" -ForegroundColor Gray
    Write-Host "8. Verify session timeout redirects to login after inactivity" -ForegroundColor Gray
} else {
    Write-Host "[ERROR] Some tests failed. Please review the failures above." -ForegroundColor Red
    exit 1
}
