# Test State Preservation and Navigation
# Tests for Task 10: Implement state preservation and navigation

Write-Host "=== Testing State Preservation and Navigation ===" -ForegroundColor Cyan
Write-Host ""

# Test configuration
$baseUrl = "http://localhost:8080"
$loginUrl = "$baseUrl/index.html"

Write-Host "Test Checklist:" -ForegroundColor Yellow
Write-Host ""

Write-Host "1. Academic Year Persistence:" -ForegroundColor Green
Write-Host "   - Open any deanship page (e.g., /deanship/dashboard)"
Write-Host "   - Select an academic year from the dropdown"
Write-Host "   - Navigate to another page (e.g., /deanship/professors)"
Write-Host "   - Verify: Academic year selection is preserved"
Write-Host "   - Check localStorage key: deanship_selected_academic_year"
Write-Host ""

Write-Host "2. Semester Persistence:" -ForegroundColor Green
Write-Host "   - Select an academic year"
Write-Host "   - Select a semester from the dropdown"
Write-Host "   - Navigate to another page"
Write-Host "   - Verify: Semester selection is preserved"
Write-Host "   - Check localStorage key: deanship_selected_semester"
Write-Host ""

Write-Host "3. Page Refresh Preservation:" -ForegroundColor Green
Write-Host "   - Select academic year and semester"
Write-Host "   - Press F5 or Ctrl+R to refresh the page"
Write-Host "   - Verify: Both selections are restored after refresh"
Write-Host ""

Write-Host "4. Last Page Tracking:" -ForegroundColor Green
Write-Host "   - Navigate to /deanship/courses"
Write-Host "   - Open browser DevTools > Application > Local Storage"
Write-Host "   - Verify: deanship_last_page = '/deanship/courses'"
Write-Host "   - Navigate to /deanship/reports"
Write-Host "   - Verify: deanship_last_page = '/deanship/reports'"
Write-Host ""

Write-Host "5. Browser Back/Forward Navigation:" -ForegroundColor Green
Write-Host "   - Navigate: Dashboard -> Professors -> Courses"
Write-Host "   - Click browser back button twice"
Write-Host "   - Verify: Returns to Dashboard with state preserved"
Write-Host "   - Click browser forward button"
Write-Host "   - Verify: Goes to Professors with state preserved"
Write-Host ""

Write-Host "6. Context Persistence Across Pages:" -ForegroundColor Green
Write-Host "   - On Dashboard: Select Academic Year 2024-2025, Semester 1"
Write-Host "   - Navigate to Course Assignments page"
Write-Host "   - Verify: Same academic year and semester are selected"
Write-Host "   - Verify: Data loads with correct filters"
Write-Host "   - Navigate to Reports page"
Write-Host "   - Verify: Same context is maintained"
Write-Host ""

Write-Host "7. Logout Clears State:" -ForegroundColor Green
Write-Host "   - Select academic year and semester"
Write-Host "   - Click Logout button"
Write-Host "   - Login again as deanship user"
Write-Host "   - Verify: Previous selections are cleared"
Write-Host "   - Check localStorage: All deanship_* keys should be removed"
Write-Host ""

Write-Host "8. Active Navigation Highlighting:" -ForegroundColor Green
Write-Host "   - Navigate to each page"
Write-Host "   - Verify: Corresponding nav link has 'active' class"
Write-Host "   - Verify: Only one nav link is active at a time"
Write-Host ""

Write-Host "Manual Testing Instructions:" -ForegroundColor Cyan
Write-Host "1. Start the application: mvnw spring-boot:run"
Write-Host "2. Open browser to: $loginUrl"
Write-Host "3. Login with deanship credentials"
Write-Host "4. Follow the test checklist above"
Write-Host "5. Use browser DevTools to inspect localStorage"
Write-Host ""

Write-Host "Browser DevTools Commands:" -ForegroundColor Yellow
Write-Host "  console.log(localStorage);"
Write-Host "  localStorage.getItem('deanship_selected_academic_year')"
Write-Host "  localStorage.getItem('deanship_selected_semester')"
Write-Host "  localStorage.getItem('deanship_last_page')"
Write-Host ""

Write-Host "Expected Behavior:" -ForegroundColor Cyan
Write-Host "  - Academic year persists across all pages"
Write-Host "  - Semester persists across all pages"
Write-Host "  - Selections survive page refresh"
Write-Host "  - Last visited page is tracked"
Write-Host "  - Browser back/forward works correctly"
Write-Host "  - Logout clears all deanship state"
Write-Host ""

# Check if application is running
Write-Host "Checking if application is running..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri $baseUrl -Method Head -TimeoutSec 5 -ErrorAction Stop
    Write-Host "Application is running at $baseUrl" -ForegroundColor Green
    Write-Host ""
    Write-Host "Ready to test! Open browser to: $loginUrl" -ForegroundColor Green
} catch {
    Write-Host "Application is not running" -ForegroundColor Red
    Write-Host "Start the application with: mvnw spring-boot:run" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Test Complete ===" -ForegroundColor Cyan
