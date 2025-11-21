# Test script for Course Assignments page
# Tests the new multi-page course assignments functionality

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Course Assignments Page Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$baseUrl = "http://localhost:8080"
$deanshipEmail = "dean@alquds.edu"
$deanshipPassword = "dean123"

Write-Host "Step 1: Authenticating as Deanship user..." -ForegroundColor Yellow

# Login
$loginBody = @{
    email = $deanshipEmail
    password = $deanshipPassword
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.data.token
    Write-Host "✓ Authentication successful" -ForegroundColor Green
    Write-Host "  Token: $($token.Substring(0, 20))..." -ForegroundColor Gray
} catch {
    Write-Host "✗ Authentication failed: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 2: Testing Course Assignments page route..." -ForegroundColor Yellow

# Test page route
try {
    $headers = @{
        "Authorization" = "Bearer $token"
    }
    
    $pageResponse = Invoke-WebRequest -Uri "$baseUrl/deanship/course-assignments" -Method Get -Headers $headers
    
    if ($pageResponse.StatusCode -eq 200) {
        Write-Host "✓ Page route accessible" -ForegroundColor Green
        
        # Check if page contains expected elements
        $content = $pageResponse.Content
        if ($content -match "Course Assignments") {
            Write-Host "✓ Page title found" -ForegroundColor Green
        } else {
            Write-Host "✗ Page title not found" -ForegroundColor Red
        }
        
        if ($content -match "course-assignments\.js") {
            Write-Host "✓ JavaScript module loaded" -ForegroundColor Green
        } else {
            Write-Host "✗ JavaScript module not found" -ForegroundColor Red
        }
        
        if ($content -match "assignCourseBtn") {
            Write-Host "✓ Assign Course button found" -ForegroundColor Green
        } else {
            Write-Host "✗ Assign Course button not found" -ForegroundColor Red
        }
    }
} catch {
    Write-Host "✗ Failed to access page: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "Step 3: Testing API endpoints..." -ForegroundColor Yellow

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Test getting academic years
try {
    $academicYearsResponse = Invoke-RestMethod -Uri "$baseUrl/api/deanship/academic-years" -Method Get -Headers $headers
    
    if ($academicYearsResponse.success) {
        Write-Host "✓ Academic years API working" -ForegroundColor Green
        $academicYears = $academicYearsResponse.data
        Write-Host "  Found $($academicYears.Count) academic year(s)" -ForegroundColor Gray
        
        if ($academicYears.Count -gt 0) {
            $firstYear = $academicYears[0]
            Write-Host "  Testing semesters endpoint for year: $($firstYear.yearCode)" -ForegroundColor Gray
            
            # Test semesters endpoint
            try {
                $semestersResponse = Invoke-RestMethod -Uri "$baseUrl/api/deanship/academic-years/$($firstYear.id)/semesters" -Method Get -Headers $headers
                
                if ($semestersResponse.success) {
                    Write-Host "✓ Semesters API working" -ForegroundColor Green
                    $semesters = $semestersResponse.data
                    Write-Host "  Found $($semesters.Count) semester(s)" -ForegroundColor Gray
                    
                    if ($semesters.Count -gt 0) {
                        $firstSemester = $semesters[0]
                        
                        # Test course assignments endpoint
                        try {
                            $assignmentsUrl = "$baseUrl/api/deanship/course-assignments?semesterId=$($firstSemester.id)"
                            $assignmentsResponse = Invoke-RestMethod -Uri $assignmentsUrl -Method Get -Headers $headers
                            
                            if ($assignmentsResponse.success) {
                                Write-Host "✓ Course assignments API working" -ForegroundColor Green
                                $assignments = $assignmentsResponse.data
                                Write-Host "  Found $($assignments.Count) assignment(s)" -ForegroundColor Gray
                            }
                        } catch {
                            Write-Host "✗ Course assignments API failed: $_" -ForegroundColor Red
                        }
                    }
                }
            } catch {
                Write-Host "✗ Semesters API failed: $_" -ForegroundColor Red
            }
        }
    }
} catch {
    Write-Host "✗ Academic years API failed: $_" -ForegroundColor Red
}

# Test professors endpoint
try {
    $professorsResponse = Invoke-RestMethod -Uri "$baseUrl/api/deanship/professors" -Method Get -Headers $headers
    
    if ($professorsResponse.success) {
        Write-Host "✓ Professors API working" -ForegroundColor Green
        Write-Host "  Found $($professorsResponse.data.Count) professor(s)" -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ Professors API failed: $_" -ForegroundColor Red
}

# Test courses endpoint
try {
    $coursesResponse = Invoke-RestMethod -Uri "$baseUrl/api/deanship/courses" -Method Get -Headers $headers
    
    if ($coursesResponse.success) {
        Write-Host "✓ Courses API working" -ForegroundColor Green
        Write-Host "  Found $($coursesResponse.data.Count) course(s)" -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ Courses API failed: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Manual Testing Steps:" -ForegroundColor Yellow
Write-Host "1. Navigate to: $baseUrl/deanship/course-assignments" -ForegroundColor White
Write-Host "2. Login with: $deanshipEmail / $deanshipPassword" -ForegroundColor White
Write-Host "3. Select an academic year and semester from the filters" -ForegroundColor White
Write-Host "4. Verify the assignments table loads" -ForegroundColor White
Write-Host "5. Click 'Assign Course' to test the modal" -ForegroundColor White
Write-Host "6. Test professor and course filters" -ForegroundColor White
Write-Host "7. Test unassign functionality" -ForegroundColor White
Write-Host ""
