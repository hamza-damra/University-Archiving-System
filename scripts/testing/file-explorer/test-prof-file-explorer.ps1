# Test Professor Dashboard File Explorer Configuration
# This script verifies that the FileExplorer is properly configured with Professor role settings

Write-Host "Testing Professor Dashboard File Explorer Configuration..." -ForegroundColor Cyan
Write-Host ""

# Check if the prof.js file has the correct imports
Write-Host "1. Checking FileExplorer import..." -ForegroundColor Yellow
$profJs = Get-Content "src/main/resources/static/js/prof.js" -Raw
if ($profJs -match "import.*FileExplorer.*from.*file-explorer\.js") {
    Write-Host "   OK FileExplorer class is imported" -ForegroundColor Green
} else {
    Write-Host "   FAIL FileExplorer class is NOT imported" -ForegroundColor Red
    exit 1
}

# Check if initializeFileExplorer function exists with correct configuration
Write-Host "2. Checking initializeFileExplorer function..." -ForegroundColor Yellow
if ($profJs -match "function initializeFileExplorer") {
    Write-Host "   OK initializeFileExplorer function exists" -ForegroundColor Green
    
    # Check for role: 'PROFESSOR'
    if ($profJs -match "role:\s*['""]PROFESSOR['""]") {
        Write-Host "   OK role: PROFESSOR is set" -ForegroundColor Green
    } else {
        Write-Host "   FAIL role: PROFESSOR is NOT set" -ForegroundColor Red
        exit 1
    }
    
    # Check for showOwnershipLabels: true
    if ($profJs -match "showOwnershipLabels:\s*true") {
        Write-Host "   OK showOwnershipLabels: true is set" -ForegroundColor Green
    } else {
        Write-Host "   FAIL showOwnershipLabels: true is NOT set" -ForegroundColor Red
        exit 1
    }
    
    # Check for readOnly: false
    if ($profJs -match "readOnly:\s*false") {
        Write-Host "   OK readOnly: false is set" -ForegroundColor Green
    } else {
        Write-Host "   FAIL readOnly: false is NOT set" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "   FAIL initializeFileExplorer function does NOT exist" -ForegroundColor Red
    exit 1
}

# Check if fileExplorerInstance variable is declared
Write-Host "3. Checking fileExplorerInstance variable..." -ForegroundColor Yellow
if ($profJs -match "let fileExplorerInstance") {
    Write-Host "   OK fileExplorerInstance variable is declared" -ForegroundColor Green
} else {
    Write-Host "   FAIL fileExplorerInstance variable is NOT declared" -ForegroundColor Red
    exit 1
}

# Check if loadFileExplorer function uses the instance
Write-Host "4. Checking loadFileExplorer function..." -ForegroundColor Yellow
if ($profJs -match "async function loadFileExplorer") {
    Write-Host "   OK loadFileExplorer function exists" -ForegroundColor Green
    
    if ($profJs -match "fileExplorerInstance\.loadRoot" -or $profJs -match "fileExplorerInstance\.loadNode") {
        Write-Host "   OK loadFileExplorer uses fileExplorerInstance" -ForegroundColor Green
    } else {
        Write-Host "   FAIL loadFileExplorer does NOT use fileExplorerInstance" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "   FAIL loadFileExplorer function does NOT exist" -ForegroundColor Red
    exit 1
}

# Check if HTML has the correct container
Write-Host "5. Checking HTML container..." -ForegroundColor Yellow
$profHtml = Get-Content "src/main/resources/static/prof-dashboard.html" -Raw
if ($profHtml -match "fileExplorerContainer") {
    Write-Host "   OK fileExplorerContainer element exists in HTML" -ForegroundColor Green
} else {
    Write-Host "   FAIL fileExplorerContainer element does NOT exist in HTML" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "All checks passed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Professor Dashboard File Explorer Configuration Summary:" -ForegroundColor Cyan
Write-Host "  - FileExplorer class: Imported" -ForegroundColor White
Write-Host "  - Role: PROFESSOR" -ForegroundColor White
Write-Host "  - Show Ownership Labels: true" -ForegroundColor White
Write-Host "  - Read Only: false" -ForegroundColor White
Write-Host "  - Container: fileExplorerContainer" -ForegroundColor White
Write-Host ""
Write-Host "The Professor Dashboard is now using the enhanced FileExplorer" -ForegroundColor Green
Write-Host "configuration with role-specific settings." -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Start the application" -ForegroundColor White
Write-Host "  2. Login as a professor" -ForegroundColor White
Write-Host "  3. Navigate to the File Explorer tab" -ForegroundColor White
Write-Host "  4. Verify Your Folder labels appear on writable folders" -ForegroundColor White
Write-Host "  5. Test breadcrumb navigation and file operations" -ForegroundColor White
