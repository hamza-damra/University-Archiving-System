# Integration Testing Script for Deanship Multi-Page Refactor
Write-Host "Deanship Multi-Page Integration Tests" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$testResults = @()

# Test credentials
$username = "dean@alquds.edu"
$password = "dean123"

Write-Host "`nAuthenticating..." -ForegroundColor Yellow
try {
    $loginBody = @{ username = $username; password = $password } | ConvertTo-Json
    $authResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    $token = $authResponse.token
    Write-Host "Authentication successful" -ForegroundColor Green
} catch {
    Write-Host "Authentication failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$headers = @{ "Authorization" = "Bearer $token" }

Write-Host "`nTesting Page Routes..." -ForegroundColor Yellow
$pages = @(
    "dashboard",
    "academic-years",
    "professors",
    "courses",
    "course-assignments",
    "reports",
    "file-explorer"
)

foreach ($page in $pages) {
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/deanship/$page" -Headers $headers -UseBasicParsing
        if ($response.StatusCode -eq 200) {
            Write-Host "  ✓ $page" -ForegroundColor Green
            $testResults += @{ Test = $page; Status = "PASS" }
        }
    } catch {
        Write-Host "  ✗ $page - $($_.Exception.Message)" -ForegroundColor Red
        $testResults += @{ Test = $page; Status = "FAIL" }
    }
}

Write-Host "`nTesting API Endpoints..." -ForegroundColor Yellow
$apis = @(
    "academic-years",
    "professors",
    "courses",
    "departments"
)

foreach ($api in $apis) {
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/api/deanship/$api" -Headers $headers
        Write-Host "  ✓ $api API" -ForegroundColor Green
        $testResults += @{ Test = "$api API"; Status = "PASS" }
    } catch {
        Write-Host "  ✗ $api API - $($_.Exception.Message)" -ForegroundColor Red
        $testResults += @{ Test = "$api API"; Status = "FAIL" }
    }
}

Write-Host "`nTest Summary" -ForegroundColor Cyan
$total = $testResults.Count
$passed = ($testResults | Where-Object { $_.Status -eq "PASS" }).Count
$failed = $total - $passed
Write-Host "Total: $total | Passed: $passed | Failed: $failed" -ForegroundColor White

Write-Host "`nFor comprehensive testing, open test-integration-manual.html in a browser" -ForegroundColor Yellow
