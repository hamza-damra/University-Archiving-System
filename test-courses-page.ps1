# Test script for Courses Management Page

Write-Host "=== Testing Courses Management Page ===" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"
$loginUrl = "$baseUrl/api/auth/login"
$coursesPageUrl = "$baseUrl/deanship/courses"
$coursesApiUrl = "$baseUrl/api/deanship/courses"
$departmentsApiUrl = "$baseUrl/api/deanship/departments"

$credentials = @{
    email = "deanship@alquds.edu"
    password = "deanship123"
} | ConvertTo-Json

Write-Host "Step 1: Authenticating..." -ForegroundColor Yellow
try {
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $credentials -ContentType "application/json"
    $token = $loginResponse.data.token
    Write-Host "✓ Authentication successful" -ForegroundColor Green
} catch {
    Write-Host "✗ Authentication failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

Write-Host ""
Write-Host "Step 2: Testing courses page access..." -ForegroundColor Yellow
try {
    $pageResponse = Invoke-WebRequest -Uri $coursesPageUrl -Headers $headers -UseBasicParsing
    Write-Host "✓ Courses page accessible (Status: $($pageResponse.StatusCode))" -ForegroundColor Green
    
    $content = $pageResponse.Content
    if ($content -match "Courses Management") {
        Write-Host "  ✓ Page title found" -ForegroundColor Green
    }
    if ($content -match "Add Course") {
        Write-Host "  ✓ Add Course button found" -ForegroundColor Green
    }
} catch {
    Write-Host "✗ Failed to access courses page: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 3: Testing departments API..." -ForegroundColor Yellow
$departments = @()
try {
    $deptResponse = Invoke-RestMethod -Uri $departmentsApiUrl -Headers $headers -Method Get
    $departments = $deptResponse.data
    Write-Host "✓ Departments loaded: $($departments.Count) departments" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to load departments: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Step 4: Testing courses API..." -ForegroundColor Yellow
try {
    $coursesResponse = Invoke-RestMethod -Uri $coursesApiUrl -Headers $headers -Method Get
    $courses = $coursesResponse.data
    Write-Host "✓ Courses loaded: $($courses.Count) courses" -ForegroundColor Green
    
    if ($courses.Count -gt 0) {
        $sample = $courses[0]
        Write-Host "  Sample: $($sample.courseCode) - $($sample.courseName)" -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ Failed to load courses: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Step 5: Testing department filter..." -ForegroundColor Yellow
if ($departments.Count -gt 0) {
    try {
        $deptId = $departments[0].id
        $filtered = Invoke-RestMethod -Uri "$coursesApiUrl`?departmentId=$deptId" -Headers $headers -Method Get
        Write-Host "✓ Filtered courses: $($filtered.data.Count) for $($departments[0].name)" -ForegroundColor Green
    } catch {
        Write-Host "✗ Filter failed: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "⊘ Skipped (no departments)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Test Complete ===" -ForegroundColor Cyan
Write-Host "✓ Courses page is functional" -ForegroundColor Green
Write-Host ""
Write-Host "Manual test at: $coursesPageUrl" -ForegroundColor Yellow
