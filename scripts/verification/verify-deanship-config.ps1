# Simple Configuration Verification Script for Deanship File Explorer

Write-Host "Verifying Deanship File Explorer Configuration..." -ForegroundColor Cyan
Write-Host ""

# Check if deanship.js exists
if (Test-Path "src/main/resources/static/js/deanship.js") {
    Write-Host "[OK] deanship.js file found" -ForegroundColor Green
    
    $content = Get-Content "src/main/resources/static/js/deanship.js" -Raw
    
    # Check for FileExplorer initialization
    if ($content -match "new FileExplorer") {
        Write-Host "[OK] FileExplorer instantiation found" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] FileExplorer instantiation not found" -ForegroundColor Red
    }
    
    # Check configuration options
    if ($content -match "role:\s*'DEANSHIP'") {
        Write-Host "[OK] role: 'DEANSHIP' configured" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] role: 'DEANSHIP' not found" -ForegroundColor Red
    }
    
    if ($content -match "readOnly:\s*true") {
        Write-Host "[OK] readOnly: true configured" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] readOnly: true not found" -ForegroundColor Red
    }
    
    if ($content -match "showAllDepartments:\s*true") {
        Write-Host "[OK] showAllDepartments: true configured" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] showAllDepartments: true not found" -ForegroundColor Red
    }
    
    if ($content -match "showProfessorLabels:\s*true") {
        Write-Host "[OK] showProfessorLabels: true configured" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] showProfessorLabels: true not found" -ForegroundColor Red
    }
    
} else {
    Write-Host "[FAIL] deanship.js file not found" -ForegroundColor Red
}

Write-Host ""
Write-Host "Configuration verification complete!" -ForegroundColor Cyan
Write-Host "See TASK_9_DEANSHIP_FILE_EXPLORER_TEST_REPORT.md for full test details" -ForegroundColor Yellow
