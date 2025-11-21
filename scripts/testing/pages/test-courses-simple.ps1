# Simple test for Courses Management Page

Write-Host "Testing Courses Page..." -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$loginUrl = "$baseUrl/api/auth/login"
$coursesPageUrl = "$baseUrl/deanship/courses"
$coursesApiUrl = "$baseUrl/api/deanship/courses"

# Login
$cred = @{ email = "deanship@alquds.edu"; password = "deanship123" } | ConvertTo-Json
$login = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $cred -ContentType "application/json"
$token = $login.data.token
$headers = @{ "Authorization" = "Bearer $token"; "Content-Type" = "application/json" }

Write-Host "✓ Logged in" -ForegroundColor Green

# Test page access
$page = Invoke-WebRequest -Uri $coursesPageUrl -Headers $headers -UseBasicParsing
Write-Host "✓ Page accessible (Status: $($page.StatusCode))" -ForegroundColor Green

# Test API
$courses = Invoke-RestMethod -Uri $coursesApiUrl -Headers $headers -Method Get
Write-Host "✓ API works: $($courses.data.Count) courses found" -ForegroundColor Green

Write-Host ""
Write-Host "Manual test: $coursesPageUrl" -ForegroundColor Yellow
Write-Host ""
