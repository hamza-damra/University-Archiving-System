#!/usr/bin/env pwsh

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Typography & UI Improvements Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if CSS file exists
$cssFile = "src/main/resources/static/css/deanship-layout.css"
if (Test-Path $cssFile) {
    Write-Host "✓ CSS file found: $cssFile" -ForegroundColor Green
} else {
    Write-Host "✗ CSS file not found: $cssFile" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Verifying CSS Requirements:" -ForegroundColor Yellow
Write-Host ""

# Read CSS content
$cssContent = Get-Content $cssFile -Raw

# Check requirements
$passCount = 0
$failCount = 0

# Test 1: Base font size
if ($cssContent -like "*--font-size-base: 16px*") {
    Write-Host "  ✓ Base font size (16px)" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "  ✗ Base font size (16px)" -ForegroundColor Red
    $failCount++
}

# Test 2: Table header font-size
if ($cssContent -like "*.data-table th*font-size: 18px*") {
    Write-Host "  ✓ Table header font-size (18px)" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "  ✗ Table header font-size (18px)" -ForegroundColor Red
    $failCount++
}

# Test 3: Table header font-weight
if ($cssContent -like "*.data-table th*font-weight: 600*") {
    Write-Host "  ✓ Table header font-weight (600)" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "  ✗ Table header font-weight (600)" -ForegroundColor Red
    $failCount++
}

# Test 4: Section title font-size
if ($cssContent -like "*.section-title*font-size: 24px*") {
    Write-Host "  ✓ Section title font-size (24px)" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "  ✗ Section title font-size (24px)" -ForegroundColor Red
    $failCount++
}

# Test 5: Section title font-weight
if ($cssContent -like "*.section-title*font-weight: 700*") {
    Write-Host "  ✓ Section title font-weight (700)" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "  ✗ Section title font-weight (700)" -ForegroundColor Red
    $failCount++
}

# Test 6: Card title font-size
if ($cssContent -like "*.card-title*font-size: 20px*") {
    Write-Host "  ✓ Card title font-size (20px)" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "  ✗ Card title font-size (20px)" -ForegroundColor Red
    $failCount++
}

# Test 7: Card title font-weight
if ($cssContent -like "*.card-title*font-weight: 600*") {
    Write-Host "  ✓ Card title font-weight (600)" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "  ✗ Card title font-weight (600)" -ForegroundColor Red
    $failCount++
}

# Test 8: Card padding
if ($cssContent -like "*.card*padding: 24px*") {
    Write-Host "  ✓ Card padding (24px)" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "  ✗ Card padding (24px)" -ForegroundColor Red
    $failCount++
}

# Test 9: Section margin-bottom
if ($cssContent -like "*.section*margin-bottom: 24px*") {
    Write-Host "  ✓ Section margin-bottom (24px)" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "  ✗ Section margin-bottom (24px)" -ForegroundColor Red
    $failCount++
}

# Test 10: Table row height
if ($cssContent -like "*.data-table td*height: 56px*") {
    Write-Host "  ✓ Table row height (56px)" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "  ✗ Table row height (56px)" -ForegroundColor Red
    $failCount++
}

# Test 11: Button minimum height
if ($cssContent -like "*.btn*min-height: 40px*") {
    Write-Host "  ✓ Button minimum height (40px)" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "  ✗ Button minimum height (40px)" -ForegroundColor Red
    $failCount++
}

# Test 12: WCAG AA comment
if ($cssContent -like "*WCAG AA*") {
    Write-Host "  ✓ WCAG AA contrast documentation" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "  ✗ WCAG AA contrast documentation" -ForegroundColor Red
    $failCount++
}

# Test 13: Horizontal scrolling
if ($cssContent -like "*overflow-x: auto*") {
    Write-Host "  ✓ Horizontal scrolling support" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "  ✗ Horizontal scrolling support" -ForegroundColor Red
    $failCount++
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($failCount -eq 0) {
    Write-Host "Results: $passCount passed, $failCount failed" -ForegroundColor Green
} else {
    Write-Host "Results: $passCount passed, $failCount failed" -ForegroundColor Yellow
}
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($failCount -eq 0) {
    Write-Host "✓ All typography and UI requirements verified!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Opening test HTML file in browser..." -ForegroundColor Cyan
    
    # Open test file in default browser
    $testFile = "test-typography-improvements.html"
    if (Test-Path $testFile) {
        Start-Process $testFile
        Write-Host "✓ Test file opened: $testFile" -ForegroundColor Green
        Write-Host ""
        Write-Host "Please verify the following in your browser:" -ForegroundColor Yellow
        Write-Host "  1. Base font size is comfortable to read (16px)" -ForegroundColor White
        Write-Host "  2. Section titles are prominent (24px, bold)" -ForegroundColor White
        Write-Host "  3. Card titles are clear (20px, semibold)" -ForegroundColor White
        Write-Host "  4. Table headers are readable (18px, semibold)" -ForegroundColor White
        Write-Host "  5. Table rows have adequate height (56px)" -ForegroundColor White
        Write-Host "  6. Cards have proper padding (24px)" -ForegroundColor White
        Write-Host "  7. Buttons are prominent (40px height)" -ForegroundColor White
        Write-Host "  8. Text contrast is good (WCAG AA compliant)" -ForegroundColor White
        Write-Host "  9. Sections have proper spacing (24px)" -ForegroundColor White
        Write-Host "  10. Resize to 1366px width to test horizontal scrolling" -ForegroundColor White
    } else {
        Write-Host "✗ Test file not found: $testFile" -ForegroundColor Red
    }
} else {
    Write-Host "✗ Some requirements not met. Please review the CSS file." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Test complete!" -ForegroundColor Cyan
