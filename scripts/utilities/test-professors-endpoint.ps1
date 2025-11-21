# Test the professors endpoint fix
$baseUrl = "http://localhost:8080/api"

Write-Host "Testing Professors Endpoint Fix" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan
Write-Host ""

# Login as dean
Write-Host "1. Logging in as dean..." -ForegroundColor Yellow
$loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body (@{
    email = "dean@alquds.edu"
    password = "dean123"
} | ConvertTo-Json) -SessionVariable session

Write-Host "   Login successful!" -ForegroundColor Green
Write-Host "   Token: $($loginResponse.token.substring(0, 20))..." -ForegroundColor Gray
Write-Host ""

# Test getting all professors (no department filter)
Write-Host "2. Testing GET /api/deanship/professors (no filter)..." -ForegroundColor Yellow
try {
    $headers = @{
        "Authorization" = "Bearer $($loginResponse.token)"
    }
    
    $professorsResponse = Invoke-RestMethod -Uri "$baseUrl/deanship/professors" -Method Get -Headers $headers
    
    Write-Host "   SUCCESS! Retrieved $($professorsResponse.data.Count) professors" -ForegroundColor Green
    Write-Host "   First 3 professors:" -ForegroundColor Gray
    $professorsResponse.data | Select-Object -First 3 | ForEach-Object {
        Write-Host "     - $($_.firstName) $($_.lastName) ($($_.email)) - Dept: $($_.department.name)" -ForegroundColor Gray
    }
} catch {
    Write-Host "   FAILED: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Response: $($_.ErrorDetails.Message)" -ForegroundColor Red
}
Write-Host ""

# Test getting professors with department filter
Write-Host "3. Testing GET /api/deanship/professors?departmentId=1..." -ForegroundColor Yellow
try {
    $professorsResponse = Invoke-RestMethod -Uri "$baseUrl/deanship/professors?departmentId=1" -Method Get -Headers $headers
    
    Write-Host "   SUCCESS! Retrieved $($professorsResponse.data.Count) professors from department 1" -ForegroundColor Green
    $professorsResponse.data | ForEach-Object {
        Write-Host "     - $($_.firstName) $($_.lastName) - Dept: $($_.department.name)" -ForegroundColor Gray
    }
} catch {
    Write-Host "   FAILED: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "Test completed!" -ForegroundColor Cyan
