# Simple test for Course Assignments page

Write-Host "Course Assignments Page Test" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"
$email = "dean@alquds.edu"
$password = "dean123"

# Login
Write-Host "Logging in..." -ForegroundColor Yellow
$loginBody = @{ email = $email; password = $password } | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.data.token
    Write-Host "✓ Login successful" -ForegroundColor Green
} catch {
    Write-Host "✗ Login failed" -ForegroundColor Red
    exit 1
}

# Test page
Write-Host ""
Write-Host "Testing page route..." -ForegroundColor Yellow
$headers = @{ "Authorization" = "Bearer $token" }

try {
    $page = Invoke-WebRequest -Uri "$baseUrl/deanship/course-assignments" -Headers $headers
    Write-Host "✓ Page accessible (Status: $($page.StatusCode))" -ForegroundColor Green
    
    if ($page.Content -match "Course Assignments") {
        Write-Host "✓ Page content correct" -ForegroundColor Green
    }
} catch {
    Write-Host "✗ Page failed: $_" -ForegroundColor Red
}

# Test APIs
Write-Host ""
Write-Host "Testing APIs..." -ForegroundColor Yellow
$headers["Content-Type"] = "application/json"

try {
    $years = Invoke-RestMethod -Uri "$baseUrl/api/deanship/academic-years" -Headers $headers
    Write-Host "✓ Academic years: $($years.data.Count) found" -ForegroundColor Green
    
    if ($years.data.Count -gt 0) {
        $yearId = $years.data[0].id
        $semesters = Invoke-RestMethod -Uri "$baseUrl/api/deanship/academic-years/$yearId/semesters" -Headers $headers
        Write-Host "✓ Semesters: $($semesters.data.Count) found" -ForegroundColor Green
        
        if ($semesters.data.Count -gt 0) {
            $semesterId = $semesters.data[0].id
            $assignments = Invoke-RestMethod -Uri "$baseUrl/api/deanship/course-assignments?semesterId=$semesterId" -Headers $headers
            Write-Host "✓ Assignments: $($assignments.data.Count) found" -ForegroundColor Green
        }
    }
} catch {
    Write-Host "✗ API test failed: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "Test complete!" -ForegroundColor Cyan
Write-Host "Visit: $baseUrl/deanship/course-assignments" -ForegroundColor White
