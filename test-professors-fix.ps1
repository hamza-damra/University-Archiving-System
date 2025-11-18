# Test the professors endpoint to verify name and department are showing
$baseUrl = "http://localhost:8080/api"

Write-Host "Testing Professors Name and Department Fix" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host ""

# First, let's get a list of users to find a deanship account
Write-Host "Finding deanship account..." -ForegroundColor Yellow

# Try common deanship emails
$deanshipEmails = @(
    "dean@alquds.edu",
    "deanship@alquds.edu",
    "admin@alquds.edu"
)

$token = $null
$loginEmail = $null

foreach ($email in $deanshipEmails) {
    try {
        Write-Host "  Trying $email..." -ForegroundColor Gray
        $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body (@{
            email = $email
            password = "password123"
        } | ConvertTo-Json) -ErrorAction Stop
        
        if ($loginResponse -and $loginResponse.token) {
            $token = $loginResponse.token
            $loginEmail = $email
            Write-Host "  SUCCESS! Logged in as $email" -ForegroundColor Green
            break
        }
    } catch {
        Write-Host "  Failed: $email" -ForegroundColor Gray
    }
}

if (-not $token) {
    Write-Host "ERROR: Could not find a valid deanship account" -ForegroundColor Red
    Write-Host "Please check the database for available accounts" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "Testing GET /api/deanship/professors..." -ForegroundColor Yellow

try {
    $headers = @{
        "Authorization" = "Bearer $token"
    }
    
    $response = Invoke-RestMethod -Uri "$baseUrl/deanship/professors" -Method Get -Headers $headers
    
    Write-Host "SUCCESS! Retrieved $($response.data.Count) professors" -ForegroundColor Green
    Write-Host ""
    Write-Host "Sample professors:" -ForegroundColor Cyan
    Write-Host "==================" -ForegroundColor Cyan
    
    $response.data | Select-Object -First 5 | ForEach-Object {
        $name = if ($_.name) { $_.name } else { "UNDEFINED" }
        $dept = if ($_.department -and $_.department.name) { $_.department.name } else { "N/A" }
        $profId = if ($_.professorId) { $_.professorId } else { "N/A" }
        
        Write-Host ""
        Write-Host "  Professor ID: $profId" -ForegroundColor White
        Write-Host "  Name: $name" -ForegroundColor $(if ($name -eq "UNDEFINED") { "Red" } else { "Green" })
        Write-Host "  Email: $($_.email)" -ForegroundColor White
        Write-Host "  Department: $dept" -ForegroundColor $(if ($dept -eq "N/A") { "Red" } else { "Green" })
        Write-Host "  Status: $(if ($_.isActive) { 'Active' } else { 'Inactive' })" -ForegroundColor White
    }
    
    Write-Host ""
    Write-Host "Checking for issues..." -ForegroundColor Yellow
    $undefinedNames = ($response.data | Where-Object { -not $_.name }).Count
    $missingDepts = ($response.data | Where-Object { -not $_.department -or -not $_.department.name }).Count
    
    if ($undefinedNames -gt 0) {
        Write-Host "  WARNING: $undefinedNames professors have undefined names" -ForegroundColor Red
    } else {
        Write-Host "  ✓ All professors have names" -ForegroundColor Green
    }
    
    if ($missingDepts -gt 0) {
        Write-Host "  WARNING: $missingDepts professors have missing departments" -ForegroundColor Red
    } else {
        Write-Host "  ✓ All professors have departments" -ForegroundColor Green
    }
    
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "Response: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Test completed!" -ForegroundColor Cyan
