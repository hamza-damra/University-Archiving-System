# Dean Dashboard API Endpoint Testing Script (PowerShell)
# This script tests all the Dean dashboard endpoints to verify they're working

param(
    [Parameter(Mandatory=$true, HelpMessage="JWT Token from localStorage")]
    [string]$Token
)

$BaseUrl = "http://localhost:8080/api"

Write-Host "`nTesting Dean Dashboard API Endpoints" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host ""

# Function to test an endpoint
function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Endpoint,
        [string]$Method = "GET"
    )
    
    Write-Host "Testing $Name... " -NoNewline
    
    try {
        $headers = @{
            "Authorization" = "Bearer $Token"
            "Content-Type" = "application/json"
        }
        
        $response = Invoke-WebRequest -Uri "$BaseUrl$Endpoint" `
                                       -Method $Method `
                                       -Headers $headers `
                                       -UseBasicParsing
        
        if ($response.StatusCode -eq 200 -or $response.StatusCode -eq 201) {
            Write-Host "✓ OK" -ForegroundColor Green -NoNewline
            Write-Host " (HTTP $($response.StatusCode))" -ForegroundColor Green
            
            # Pretty print JSON
            $json = $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
            Write-Host $json
        }
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "✗ FAILED" -ForegroundColor Red -NoNewline
        Write-Host " (HTTP $statusCode)" -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor Red
    }
    
    Write-Host ""
}

# Test endpoints
Write-Host "1. Academic Years" -ForegroundColor Yellow
Test-Endpoint -Name "Get Academic Years" -Endpoint "/deanship/academic-years"

Write-Host "2. Departments" -ForegroundColor Yellow
Test-Endpoint -Name "Get Departments" -Endpoint "/deanship/departments"

Write-Host "3. Professors" -ForegroundColor Yellow
Test-Endpoint -Name "Get All Professors" -Endpoint "/deanship/professors"

Write-Host "4. Courses" -ForegroundColor Yellow
Test-Endpoint -Name "Get All Courses" -Endpoint "/deanship/courses"

Write-Host "5. Course Assignments (requires semesterId)" -ForegroundColor Yellow
Write-Host "Note: This endpoint requires a semesterId parameter"
Write-Host "Example: /deanship/course-assignments?semesterId=1"
Write-Host ""

Write-Host "6. Reports (requires semesterId)" -ForegroundColor Yellow
Write-Host "Note: This endpoint requires a semesterId parameter"
Write-Host "Example: /deanship/reports/system-wide?semesterId=1"
Write-Host ""

Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Testing Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "If you see 401 errors, your token may have expired."
Write-Host "If you see 403 errors, your user may not have DEANSHIP role."
Write-Host "If you see 404 errors, check that the backend is running."
Write-Host ""
Write-Host "To get your JWT token:" -ForegroundColor Cyan
Write-Host "1. Open browser DevTools (F12)"
Write-Host "2. Go to Console tab"
Write-Host "3. Type: localStorage.getItem('token')"
Write-Host "4. Copy the token (without quotes)"
Write-Host ""
Write-Host "Example usage:" -ForegroundColor Cyan
Write-Host '  .\test-dean-endpoints.ps1 -Token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."'
