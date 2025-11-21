# Archive System API Test Script
# This script tests all major endpoints of the Archive System

Write-Host "=== Archive System API Testing ===" -ForegroundColor Green
Write-Host "Testing endpoints on http://localhost:8080" -ForegroundColor Yellow

# Test Data
$hodCredentials = @{
    email = "hod.cs@alquds.edu"
    password = "password123"
}

$profCredentials = @{
    email = "prof.omar@alquds.edu" 
    password = "password123"
}

$baseUrl = "http://localhost:8080"

# Helper function to make API calls
function Invoke-ApiCall {
    param(
        [string]$Method,
        [string]$Endpoint,
        [string]$Body = $null,
        [hashtable]$Headers = @{"Content-Type" = "application/json"}
    )
    
    try {
        $uri = "$baseUrl$Endpoint"
        if ($Body) {
            $response = Invoke-WebRequest -Uri $uri -Method $Method -Body $Body -Headers $Headers
        } else {
            $response = Invoke-WebRequest -Uri $uri -Method $Method -Headers $Headers
        }
        return @{
            Success = $true
            StatusCode = $response.StatusCode
            Content = ($response.Content | ConvertFrom-Json)
        }
    } catch {
        return @{
            Success = $false
            StatusCode = $_.Exception.Response.StatusCode
            Error = $_.Exception.Message
        }
    }
}

# Test 1: Authentication Tests
Write-Host "`n1. Testing Authentication..." -ForegroundColor Cyan

# Valid HOD Login
$loginBody = $hodCredentials | ConvertTo-Json
$hodLogin = Invoke-ApiCall -Method "POST" -Endpoint "/api/auth/login" -Body $loginBody

if ($hodLogin.Success) {
    Write-Host "✅ HOD Login successful" -ForegroundColor Green
    $hodToken = $hodLogin.Content.data.token
    Write-Host "   User: $($hodLogin.Content.data.firstName) $($hodLogin.Content.data.lastName)"
    Write-Host "   Role: $($hodLogin.Content.data.role)"
} else {
    Write-Host "❌ HOD Login failed: $($hodLogin.Error)" -ForegroundColor Red
    exit 1
}

# Valid Professor Login
$profLoginBody = $profCredentials | ConvertTo-Json
$profLogin = Invoke-ApiCall -Method "POST" -Endpoint "/api/auth/login" -Body $profLoginBody

if ($profLogin.Success) {
    Write-Host "✅ Professor Login successful" -ForegroundColor Green
    $profToken = $profLogin.Content.data.token
    Write-Host "   User: $($profLogin.Content.data.firstName) $($profLogin.Content.data.lastName)"
} else {
    Write-Host "❌ Professor Login failed: $($profLogin.Error)" -ForegroundColor Red
}

# Invalid Login Test
$invalidLogin = @{email = "invalid@test.com"; password = "wrong"} | ConvertTo-Json
$invalidResult = Invoke-ApiCall -Method "POST" -Endpoint "/api/auth/login" -Body $invalidLogin

if (!$invalidResult.Success) {
    Write-Host "✅ Invalid login properly rejected" -ForegroundColor Green
} else {
    Write-Host "❌ Invalid login should have failed" -ForegroundColor Red
}

# Test 2: HOD Endpoints
Write-Host "`n2. Testing HOD Endpoints..." -ForegroundColor Cyan

$hodHeaders = @{
    "Authorization" = "Bearer $hodToken"
    "Content-Type" = "application/json"
}

# Get all professors
$professors = Invoke-ApiCall -Method "GET" -Endpoint "/api/hod/professors" -Headers $hodHeaders

if ($professors.Success) {
    Write-Host "✅ Get professors successful" -ForegroundColor Green
    Write-Host "   Found $($professors.Content.data.totalElements) professors"
} else {
    Write-Host "❌ Get professors failed: $($professors.Error)" -ForegroundColor Red
}

# Get HOD's document requests
$hodRequests = Invoke-ApiCall -Method "GET" -Endpoint "/api/hod/document-requests" -Headers $hodHeaders

if ($hodRequests.Success) {
    Write-Host "✅ Get HOD document requests successful" -ForegroundColor Green
    Write-Host "   Found $($hodRequests.Content.data.totalElements) requests"
} else {
    Write-Host "❌ Get HOD document requests failed: $($hodRequests.Error)" -ForegroundColor Red
}

# Test 3: Professor Endpoints
Write-Host "`n3. Testing Professor Endpoints..." -ForegroundColor Cyan

$profHeaders = @{
    "Authorization" = "Bearer $profToken"
    "Content-Type" = "application/json"
}

# Get professor's document requests
$profRequests = Invoke-ApiCall -Method "GET" -Endpoint "/api/professor/document-requests" -Headers $profHeaders

if ($profRequests.Success) {
    Write-Host "✅ Get professor document requests successful" -ForegroundColor Green
    Write-Host "   Found $($profRequests.Content.data.totalElements) requests"
} else {
    Write-Host "❌ Get professor document requests failed: $($profRequests.Error)" -ForegroundColor Red
}

# Test 4: Authorization Tests
Write-Host "`n4. Testing Authorization..." -ForegroundColor Cyan

# Professor trying to access HOD endpoint (should fail)
$unauthorizedAccess = Invoke-ApiCall -Method "GET" -Endpoint "/api/hod/professors" -Headers $profHeaders

if (!$unauthorizedAccess.Success) {
    Write-Host "✅ Authorization properly enforced - Professor cannot access HOD endpoints" -ForegroundColor Green
} else {
    Write-Host "❌ Authorization failed - Professor should not access HOD endpoints" -ForegroundColor Red
}

# Test 5: Logout
Write-Host "`n5. Testing Logout..." -ForegroundColor Cyan

$logout = Invoke-ApiCall -Method "POST" -Endpoint "/api/auth/logout"

if ($logout.Success) {
    Write-Host "✅ Logout successful" -ForegroundColor Green
} else {
    Write-Host "❌ Logout failed: $($logout.Error)" -ForegroundColor Red
}

Write-Host "`n=== Testing Complete ===" -ForegroundColor Green
Write-Host "All major endpoints have been tested!" -ForegroundColor Yellow
