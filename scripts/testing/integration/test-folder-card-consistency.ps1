# Folder Card Design Consistency Verification Script
# Tests folder card styling across Professor, HOD, and Deanship dashboards
# Task 15: Verify consistent folder card design across all dashboards

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Folder Card Design Consistency Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test results tracking
$testResults = @()

function Add-TestResult {
    param(
        [string]$Dashboard,
        [string]$TestName,
        [string]$Status,
        [string]$Details = ""
    )
    
    $testResults += [PSCustomObject]@{
        Dashboard = $Dashboard
        Test = $TestName
        Status = $Status
        Details = $Details
    }
    
    $color = switch ($Status) {
        "PASS" { "Green" }
        "FAIL" { "Red" }
        "MANUAL" { "Yellow" }
        default { "White" }
    }
    
    Write-Host "[$Status] $Dashboard - $TestName" -ForegroundColor $color
    if ($Details) {
        Write-Host "  Details: $Details" -ForegroundColor Gray
    }
}

Write-Host "Checking FileExplorer class implementation..." -ForegroundColor Yellow
Write-Host ""

# Test 1: Verify FileExplorer class has correct folder card styling
Write-Host "Test 1: FileExplorer Class Folder Card Styling" -ForegroundColor Cyan
Write-Host "-----------------------------------------------" -ForegroundColor Cyan

$fileExplorerPath = "src/main/resources/static/js/file-explorer.js"
if (Test-Path $fileExplorerPath) {
    $fileExplorerContent = Get-Content $fileExplorerPath -Raw
    
    # Check for blue card styling
    if ($fileExplorerContent -match 'bg-blue-50.*border.*border-blue-200.*hover:bg-blue-100') {
        Add-TestResult -Dashboard "FileExplorer" -TestName "Blue card styling (bg-blue-50, border-blue-200, hover:bg-blue-100)" -Status "PASS"
    } else {
        Add-TestResult -Dashboard "FileExplorer" -TestName "Blue card styling" -Status "FAIL" -Details "Missing correct blue card classes"
    }
    
    # Check for folder icon styling
    if ($fileExplorerContent -match 'w-7 h-7 text-blue-600') {
        Add-TestResult -Dashboard "FileExplorer" -TestName "Folder icon styling (w-7 h-7 text-blue-600)" -Status "PASS"
    } else {
        Add-TestResult -Dashboard "FileExplorer" -TestName "Folder icon styling" -Status "FAIL" -Details "Missing correct folder icon classes"
    }
    
    # Check for arrow animation
    if ($fileExplorerContent -match 'group-hover:translate-x-1') {
        Add-TestResult -Dashboard "FileExplorer" -TestName "Arrow animation (group-hover:translate-x-1)" -Status "PASS"
    } else {
        Add-TestResult -Dashboard "FileExplorer" -TestName "Arrow animation" -Status "FAIL" -Details "Missing arrow animation class"
    }
    
    # Check for padding
    if ($fileExplorerContent -match 'p-4.*bg-blue-50') {
        Add-TestResult -Dashboard "FileExplorer" -TestName "Card padding (p-4)" -Status "PASS"
    } else {
        Add-TestResult -Dashboard "FileExplorer" -TestName "Card padding" -Status "FAIL" -Details "Missing correct padding"
    }
    
    # Check for rounded corners
    if ($fileExplorerContent -match 'rounded-lg.*border.*border-blue-200') {
        Add-TestResult -Dashboard "FileExplorer" -TestName "Rounded corners (rounded-lg)" -Status "PASS"
    } else {
        Add-TestResult -Dashboard "FileExplorer" -TestName "Rounded corners" -Status "FAIL" -Details "Missing rounded-lg class"
    }
    
    # Check for transition
    if ($fileExplorerContent -match 'transition-all') {
        Add-TestResult -Dashboard "FileExplorer" -TestName "Smooth transitions (transition-all)" -Status "PASS"
    } else {
        Add-TestResult -Dashboard "FileExplorer" -TestName "Smooth transitions" -Status "FAIL" -Details "Missing transition-all class"
    }
    
    # Check for group class for hover effects
    if ($fileExplorerContent -match 'group.*onclick') {
        Add-TestResult -Dashboard "FileExplorer" -TestName "Group hover support (group class)" -Status "PASS"
    } else {
        Add-TestResult -Dashboard "FileExplorer" -TestName "Group hover support" -Status "FAIL" -Details "Missing group class"
    }
    
} else {
    Add-TestResult -Dashboard "FileExplorer" -TestName "File exists" -Status "FAIL" -Details "FileExplorer class not found"
}

Write-Host ""
Write-Host "Test 2: Professor Dashboard Integration" -ForegroundColor Cyan
Write-Host "---------------------------------------" -ForegroundColor Cyan

$profJsPath = "src/main/resources/static/js/prof.js"
if (Test-Path $profJsPath) {
    $profJsContent = Get-Content $profJsPath -Raw
    
    # Check if Professor Dashboard uses FileExplorer class
    if ($profJsContent -match 'new FileExplorer' -or $profJsContent -match 'FileExplorer\(') {
        Add-TestResult -Dashboard "Professor" -TestName "Uses FileExplorer class" -Status "PASS"
    } else {
        # Check if it has custom folder card implementation
        if ($profJsContent -match 'bg-blue-50.*border-blue-200') {
            Add-TestResult -Dashboard "Professor" -TestName "Has blue card styling" -Status "PASS" -Details "Custom implementation with correct styling"
        } else {
            Add-TestResult -Dashboard "Professor" -TestName "Folder card styling" -Status "FAIL" -Details "No FileExplorer class and no blue card styling found"
        }
    }
    
    # Check for role-specific labels
    if ($profJsContent -match 'Your Folder' -or $profJsContent -match 'showOwnershipLabels') {
        Add-TestResult -Dashboard "Professor" -TestName "Role-specific labels (Your Folder)" -Status "PASS"
    } else {
        Add-TestResult -Dashboard "Professor" -TestName "Role-specific labels" -Status "MANUAL" -Details "Verify 'Your Folder' labels appear on owned folders"
    }
    
} else {
    Add-TestResult -Dashboard "Professor" -TestName "File exists" -Status "FAIL" -Details "prof.js not found"
}

Write-Host ""
Write-Host "Test 3: HOD Dashboard Integration" -ForegroundColor Cyan
Write-Host "---------------------------------" -ForegroundColor Cyan

$hodJsPath = "src/main/resources/static/js/hod.js"
if (Test-Path $hodJsPath) {
    $hodJsContent = Get-Content $hodJsPath -Raw
    
    # Check if HOD Dashboard uses FileExplorer class
    if ($hodJsContent -match 'new FileExplorer' -or $hodJsContent -match 'FileExplorer\(') {
        Add-TestResult -Dashboard "HOD" -TestName "Uses FileExplorer class" -Status "PASS"
        
        # Check for HOD-specific configuration
        if ($hodJsContent -match "role.*HOD" -or $hodJsContent -match "showDepartmentContext") {
            Add-TestResult -Dashboard "HOD" -TestName "HOD-specific configuration" -Status "PASS"
        } else {
            Add-TestResult -Dashboard "HOD" -TestName "HOD-specific configuration" -Status "MANUAL" -Details "Verify role configuration is set"
        }
    } else {
        Add-TestResult -Dashboard "HOD" -TestName "Uses FileExplorer class" -Status "FAIL" -Details "HOD Dashboard not using unified FileExplorer"
    }
    
    # Check for read-only header message
    if ($hodJsContent -match 'Browse department files' -or $hodJsContent -match 'headerMessage') {
        Add-TestResult -Dashboard "HOD" -TestName "Header message (Read-only)" -Status "PASS"
    } else {
        Add-TestResult -Dashboard "HOD" -TestName "Header message" -Status "MANUAL" -Details "Verify header message displays"
    }
    
} else {
    Add-TestResult -Dashboard "HOD" -TestName "File exists" -Status "FAIL" -Details "hod.js not found"
}

Write-Host ""
Write-Host "Test 4: Deanship Dashboard Integration" -ForegroundColor Cyan
Write-Host "--------------------------------------" -ForegroundColor Cyan

$deanshipJsPath = "src/main/resources/static/js/deanship.js"
if (Test-Path $deanshipJsPath) {
    $deanshipJsContent = Get-Content $deanshipJsPath -Raw
    
    # Check if Deanship Dashboard uses FileExplorer class
    if ($deanshipJsContent -match 'new FileExplorer' -or $deanshipJsContent -match 'FileExplorer\(') {
        Add-TestResult -Dashboard "Deanship" -TestName "Uses FileExplorer class" -Status "PASS"
        
        # Check for Deanship-specific configuration
        if ($deanshipJsContent -match "role.*DEANSHIP" -or $deanshipJsContent -match "showProfessorLabels") {
            Add-TestResult -Dashboard "Deanship" -TestName "Deanship-specific configuration" -Status "PASS"
        } else {
            Add-TestResult -Dashboard "Deanship" -TestName "Deanship-specific configuration" -Status "MANUAL" -Details "Verify role configuration is set"
        }
    } else {
        Add-TestResult -Dashboard "Deanship" -TestName "Uses FileExplorer class" -Status "FAIL" -Details "Deanship Dashboard not using unified FileExplorer"
    }
    
    # Check for professor labels
    if ($deanshipJsContent -match 'showProfessorLabels' -or $deanshipJsContent -match 'professorName') {
        Add-TestResult -Dashboard "Deanship" -TestName "Professor name labels" -Status "PASS"
    } else {
        Add-TestResult -Dashboard "Deanship" -TestName "Professor name labels" -Status "MANUAL" -Details "Verify professor labels display on folders"
    }
    
} else {
    Add-TestResult -Dashboard "Deanship" -TestName "File exists" -Status "FAIL" -Details "deanship.js not found"
}

Write-Host ""
Write-Host "Test 5: Design Specification Compliance" -ForegroundColor Cyan
Write-Host "---------------------------------------" -ForegroundColor Cyan

# Verify design document exists and has correct specifications
$designDocPath = ".kiro/specs/unified-file-explorer/design.md"
if (Test-Path $designDocPath) {
    $designContent = Get-Content $designDocPath -Raw
    
    if ($designContent -match 'bg-blue-50.*border-blue-200.*hover:bg-blue-100') {
        Add-TestResult -Dashboard "Design Spec" -TestName "Folder card specification documented" -Status "PASS"
    } else {
        Add-TestResult -Dashboard "Design Spec" -TestName "Folder card specification" -Status "FAIL" -Details "Design spec missing folder card details"
    }
    
    if ($designContent -match 'w-7 h-7 text-blue-600') {
        Add-TestResult -Dashboard "Design Spec" -TestName "Folder icon specification documented" -Status "PASS"
    } else {
        Add-TestResult -Dashboard "Design Spec" -TestName "Folder icon specification" -Status "FAIL" -Details "Design spec missing icon details"
    }
    
    if ($designContent -match 'group-hover:translate-x-1') {
        Add-TestResult -Dashboard "Design Spec" -TestName "Arrow animation specification documented" -Status "PASS"
    } else {
        Add-TestResult -Dashboard "Design Spec" -TestName "Arrow animation specification" -Status "FAIL" -Details "Design spec missing animation details"
    }
} else {
    Add-TestResult -Dashboard "Design Spec" -TestName "File exists" -Status "FAIL" -Details "Design document not found"
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Count results
$passCount = ($testResults | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($testResults | Where-Object { $_.Status -eq "FAIL" }).Count
$manualCount = ($testResults | Where-Object { $_.Status -eq "MANUAL" }).Count
$totalCount = $testResults.Count

Write-Host "Total Tests: $totalCount" -ForegroundColor White
Write-Host "Passed: $passCount" -ForegroundColor Green
Write-Host "Failed: $failCount" -ForegroundColor Red
Write-Host "Manual Verification Required: $manualCount" -ForegroundColor Yellow
Write-Host ""

# Display detailed results by dashboard
Write-Host "Results by Dashboard:" -ForegroundColor Cyan
Write-Host "--------------------" -ForegroundColor Cyan
$testResults | Group-Object Dashboard | ForEach-Object {
    Write-Host ""
    Write-Host "$($_.Name):" -ForegroundColor Yellow
    $_.Group | ForEach-Object {
        $color = switch ($_.Status) {
            "PASS" { "Green" }
            "FAIL" { "Red" }
            "MANUAL" { "Yellow" }
            default { "White" }
        }
        Write-Host "  [$($_.Status)] $($_.Test)" -ForegroundColor $color
        if ($_.Details) {
            Write-Host "    $($_.Details)" -ForegroundColor Gray
        }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Manual Verification Checklist" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Please manually verify the following in a browser:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Professor Dashboard (http://localhost:8080/prof-dashboard.html)" -ForegroundColor Cyan
Write-Host "   [ ] Course folders use wide blue cards (bg-blue-50, border-blue-200)" -ForegroundColor White
Write-Host "   [ ] Folder icon is blue (text-blue-600) and sized w-7 h-7" -ForegroundColor White
Write-Host "   [ ] Hover effect changes background to bg-blue-100" -ForegroundColor White
Write-Host "   [ ] Arrow icon animates on hover (translates right)" -ForegroundColor White
Write-Host "   [ ] 'Your Folder' label appears on owned folders" -ForegroundColor White
Write-Host "   [ ] 'Read Only' label appears on read-only folders" -ForegroundColor White
Write-Host ""
Write-Host "2. HOD Dashboard (http://localhost:8080/hod-dashboard.html)" -ForegroundColor Cyan
Write-Host "   [ ] Professor folders use same blue card design" -ForegroundColor White
Write-Host "   [ ] Course folders use same blue card design" -ForegroundColor White
Write-Host "   [ ] Hover effects work consistently" -ForegroundColor White
Write-Host "   [ ] Arrow animation works on hover" -ForegroundColor White
Write-Host "   [ ] Header message 'Browse department files (Read-only)' displays" -ForegroundColor White
Write-Host ""
Write-Host "3. Deanship Dashboard (http://localhost:8080/deanship-dashboard.html)" -ForegroundColor Cyan
Write-Host "   [ ] Professor folders use same blue card design" -ForegroundColor White
Write-Host "   [ ] Course folders use same blue card design" -ForegroundColor White
Write-Host "   [ ] Document type folders use same blue card design" -ForegroundColor White
Write-Host "   [ ] Hover effects work consistently" -ForegroundColor White
Write-Host "   [ ] Arrow animation works on hover" -ForegroundColor White
Write-Host "   [ ] Professor name labels appear on professor folders" -ForegroundColor White
Write-Host ""
Write-Host "4. Cross-Dashboard Comparison" -ForegroundColor Cyan
Write-Host "   [ ] All folder cards look identical across dashboards" -ForegroundColor White
Write-Host "   [ ] Same blue color (bg-blue-50, border-blue-200)" -ForegroundColor White
Write-Host "   [ ] Same padding (p-4)" -ForegroundColor White
Write-Host "   [ ] Same border radius (rounded-lg)" -ForegroundColor White
Write-Host "   [ ] Same hover effect (hover:bg-blue-100)" -ForegroundColor White
Write-Host "   [ ] Same icon size and color (w-7 h-7 text-blue-600)" -ForegroundColor White
Write-Host "   [ ] Same arrow animation (group-hover:translate-x-1)" -ForegroundColor White
Write-Host ""

# Exit with appropriate code
if ($failCount -gt 0) {
    Write-Host "RESULT: Some tests failed. Please review the failures above." -ForegroundColor Red
    exit 1
} elseif ($manualCount -gt 0) {
    Write-Host "RESULT: All automated tests passed. Manual verification required." -ForegroundColor Yellow
    exit 0
} else {
    Write-Host "RESULT: All tests passed!" -ForegroundColor Green
    exit 0
}
