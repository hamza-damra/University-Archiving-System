# Simple verification script for Task 8
Write-Host "Verifying Task 8: Deanship Dashboard File Explorer Migration" -ForegroundColor Cyan
Write-Host ""

$passed = 0
$failed = 0

# Check 1: HTML has correct container
Write-Host "1. Checking HTML container..." -NoNewline
$html = Get-Content "src/main/resources/static/deanship-dashboard.html" -Raw
if ($html -like '*id="fileExplorerContainer"*') {
    Write-Host " PASS" -ForegroundColor Green
    $passed++
} else {
    Write-Host " FAIL" -ForegroundColor Red
    $failed++
}

# Check 2: JS imports FileExplorer
Write-Host "2. Checking FileExplorer import..." -NoNewline
$js = Get-Content "src/main/resources/static/js/deanship.js" -Raw
if ($js -like '*import*FileExplorer*file-explorer.js*') {
    Write-Host " PASS" -ForegroundColor Green
    $passed++
} else {
    Write-Host " FAIL" -ForegroundColor Red
    $failed++
}

# Check 3: Correct configuration
Write-Host "3. Checking Deanship configuration..." -NoNewline
if ($js -like "*role: 'DEANSHIP'*" -and $js -like "*readOnly: true*" -and $js -like "*showProfessorLabels: true*") {
    Write-Host " PASS" -ForegroundColor Green
    $passed++
} else {
    Write-Host " FAIL" -ForegroundColor Red
    $failed++
}

# Check 4: initializeFileExplorer exists
Write-Host "4. Checking initializeFileExplorer..." -NoNewline
if ($js -like "*function initializeFileExplorer*") {
    Write-Host " PASS" -ForegroundColor Green
    $passed++
} else {
    Write-Host " FAIL" -ForegroundColor Red
    $failed++
}

# Check 5: loadFileExplorer exists
Write-Host "5. Checking loadFileExplorer..." -NoNewline
if ($js -like "*function loadFileExplorer*") {
    Write-Host " PASS" -ForegroundColor Green
    $passed++
} else {
    Write-Host " FAIL" -ForegroundColor Red
    $failed++
}

Write-Host ""
Write-Host "Results: $passed passed, $failed failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Yellow" })

if ($failed -eq 0) {
    Write-Host ""
    Write-Host "Task 8 implementation verified successfully!" -ForegroundColor Green
}
