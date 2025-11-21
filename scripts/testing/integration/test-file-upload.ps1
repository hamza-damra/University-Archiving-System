# File Upload Testing Script for Archive System
Write-Host "=== File Upload Testing ===" -ForegroundColor Green

$baseUrl = "http://localhost:8080"

# Helper function for API calls
function Invoke-ApiCall {
    param(
        [string]$Method,
        [string]$Endpoint,
        [string]$Body = $null,
        [hashtable]$Headers = @{"Content-Type" = "application/json"},
        [string]$FilePath = $null
    )
    
    try {
        $uri = "$baseUrl$Endpoint"
        
        if ($FilePath) {
            # For file uploads, use multipart form data
            $form = @{
                file = Get-Item $FilePath
            }
            $response = Invoke-WebRequest -Uri $uri -Method $Method -Form $form -Headers @{"Authorization" = $Headers["Authorization"]}
        } elseif ($Body) {
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

# Step 1: Login as HOD to create a document request
Write-Host "`n1. Logging in as HOD..." -ForegroundColor Cyan
$hodLogin = '{"email":"hod.cs@alquds.edu","password":"password123"}'
$hodResult = Invoke-ApiCall -Method "POST" -Endpoint "/api/auth/login" -Body $hodLogin

if (!$hodResult.Success) {
    Write-Host "❌ HOD login failed" -ForegroundColor Red
    exit 1
}

$hodToken = $hodResult.Content.data.token
Write-Host "✅ HOD logged in successfully" -ForegroundColor Green

# Step 2: Create a document request
Write-Host "`n2. Creating document request..." -ForegroundColor Cyan
$requestBody = @{
    professorId = 3
    courseName = "Software Engineering"
    documentType = "Course Syllabus"
    description = "Please submit the updated course syllabus"
    deadline = "2025-12-15T23:59:59"
    requiredFileExtensions = @("pdf", "txt", "docx")
} | ConvertTo-Json

$hodHeaders = @{
    "Authorization" = "Bearer $hodToken"
    "Content-Type" = "application/json"
}

$createRequest = Invoke-ApiCall -Method "POST" -Endpoint "/api/hod/document-requests" -Body $requestBody -Headers $hodHeaders

if ($createRequest.Success) {
    $requestId = $createRequest.Content.data.id
    Write-Host "✅ Document request created with ID: $requestId" -ForegroundColor Green
} else {
    Write-Host "❌ Failed to create document request: $($createRequest.Error)" -ForegroundColor Red
    # Try to get existing requests instead
    $existingRequests = Invoke-ApiCall -Method "GET" -Endpoint "/api/hod/document-requests" -Headers $hodHeaders
    if ($existingRequests.Success -and $existingRequests.Content.data.content.Count -gt 0) {
        $requestId = $existingRequests.Content.data.content[0].id
        Write-Host "ℹ️ Using existing request ID: $requestId" -ForegroundColor Yellow
    } else {
        Write-Host "❌ No document requests available for testing" -ForegroundColor Red
        exit 1
    }
}

# Step 3: Login as Professor
Write-Host "`n3. Logging in as Professor..." -ForegroundColor Cyan
$profLogin = '{"email":"prof.omar@alquds.edu","password":"password123"}'
$profResult = Invoke-ApiCall -Method "POST" -Endpoint "/api/auth/login" -Body $profLogin

if (!$profResult.Success) {
    Write-Host "❌ Professor login failed" -ForegroundColor Red
    exit 1
}

$profToken = $profResult.Content.data.token
Write-Host "✅ Professor logged in successfully" -ForegroundColor Green

# Step 4: Check professor's document requests
Write-Host "`n4. Checking professor's document requests..." -ForegroundColor Cyan
$profHeaders = @{
    "Authorization" = "Bearer $profToken"
}

$profRequests = Invoke-ApiCall -Method "GET" -Endpoint "/api/professor/document-requests" -Headers $profHeaders

if ($profRequests.Success) {
    Write-Host "✅ Professor has $($profRequests.Content.data.totalElements) document requests" -ForegroundColor Green
    
    if ($profRequests.Content.data.totalElements -gt 0) {
        $availableRequest = $profRequests.Content.data.content[0]
        $requestId = $availableRequest.id
        Write-Host "   Using request: $($availableRequest.courseName) - $($availableRequest.documentType)" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ Failed to get professor requests: $($profRequests.Error)" -ForegroundColor Red
}

# Step 5: Test file upload
Write-Host "`n5. Testing file upload..." -ForegroundColor Cyan

if (Test-Path "test-document.txt") {
    $uploadResult = Invoke-ApiCall -Method "POST" -Endpoint "/api/professor/document-requests/$requestId/upload" -Headers $profHeaders -FilePath "test-document.txt"
    
    if ($uploadResult.Success) {
        Write-Host "✅ File uploaded successfully!" -ForegroundColor Green
        Write-Host "   Document ID: $($uploadResult.Content.data.id)" -ForegroundColor Yellow
        Write-Host "   File: $($uploadResult.Content.data.originalFilename)" -ForegroundColor Yellow
        Write-Host "   Size: $($uploadResult.Content.data.fileSize) bytes" -ForegroundColor Yellow
        $documentId = $uploadResult.Content.data.id
    } else {
        Write-Host "❌ File upload failed: $($uploadResult.Error)" -ForegroundColor Red
    }
} else {
    Write-Host "Test file 'test-document.txt' not found" -ForegroundColor Red
}

# Step 6: Test getting submitted documents
Write-Host "`n6. Testing submitted documents retrieval..." -ForegroundColor Cyan
$submittedDocs = Invoke-ApiCall -Method "GET" -Endpoint "/api/professor/submitted-documents" -Headers $profHeaders

if ($submittedDocs.Success) {
    Write-Host "✅ Retrieved $($submittedDocs.Content.data.Count) submitted documents" -ForegroundColor Green
    foreach ($doc in $submittedDocs.Content.data) {
        Write-Host "   - $($doc.originalFilename) (ID: $($doc.id))" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ Failed to get submitted documents: $($submittedDocs.Error)" -ForegroundColor Red
}

# Step 7: Test getting specific submitted document
if ($requestId) {
    Write-Host "`n7. Testing specific document retrieval..." -ForegroundColor Cyan
    $specificDoc = Invoke-ApiCall -Method "GET" -Endpoint "/api/professor/document-requests/$requestId/submitted-document" -Headers $profHeaders
    
    if ($specificDoc.Success) {
        Write-Host "✅ Retrieved submitted document for request $requestId" -ForegroundColor Green
        Write-Host "   File: $($specificDoc.Content.data.originalFilename)" -ForegroundColor Yellow
    } else {
        Write-Host "ℹ️ No document submitted for request $requestId yet" -ForegroundColor Yellow
    }
}

Write-Host "`n=== File Upload Testing Complete ===" -ForegroundColor Green
