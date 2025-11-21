# Test script for Deanship File Explorer Page
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Deanship File Explorer Page Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"
$loginUrl = "$baseUrl/api/auth/login"
$fileExplorerPageUrl = "$baseUrl/deanship/file-explorer"

$deanshipCredentials = @{
    email = "dean@alquds.edu"
    password = "dean123"
}

Write-Host "Step 1: Login as Deanship user" -ForegroundColor Yellow
Write-Host "Credentials: $($deanshipCredentials.email)" -ForegroundColor Gray

try {
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method Post -Body ($deanshipCredentials | ConvertTo-Json) -ContentType "application/json" -SessionVariable session
    
    if ($loginResponse.token) {
        Write-Host "Login successful" -ForegroundColor Green
        $token = $loginResponse.token
    } else {
        Write-Host "Login failed - No token received" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 2: Access File Explorer Page" -ForegroundColor Yellow

try {
    $headers = @{
        "Authorization" = "Bearer $token"
    }
    
    $pageResponse = Invoke-WebRequest -Uri $fileExplorerPageUrl -Method Get -Headers $headers -WebSession $session
    
    if ($pageResponse.StatusCode -eq 200) {
        Write-Host "File Explorer page loaded successfully" -ForegroundColor Green
        
        $content = $pageResponse.Content
        
        if ($content -match "File Explorer - Deanship Dashboard") {
            Write-Host "  Page title found" -ForegroundColor Green
        }
        if ($content -match 'id="fileExplorerContainer"') {
            Write-Host "  File Explorer container found" -ForegroundColor Green
        }
        if ($content -match 'src="/js/file-explorer-page.js"') {
            Write-Host "  JavaScript module found" -ForegroundColor Green
        }
    }
} catch {
    Write-Host "Failed to access page: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Test completed successfully!" -ForegroundColor Green
Write-Host "Open browser to: $fileExplorerPageUrl" -ForegroundColor Yellow
Write-Host ""
