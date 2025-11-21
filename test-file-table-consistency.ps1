# Test File Table Design Consistency Across All Dashboards
# This script verifies that file tables use consistent styling across Professor, HOD, and Deanship dashboards

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "File Table Consistency Verification" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$testsPassed = 0
$testsFailed = 0
$testResults = @()

function Test-FileTableElement {
    param(
        [string]$TestName,
        [string]$FilePath,
        [string]$Pattern,
        [string]$Description
    )
    
    Write-Host "Testing: $TestName" -ForegroundColor Yellow
    
    if (Test-Path $FilePath) {
        $content = Get-Content $FilePath -Raw
        
        if ($content -match [regex]::Escape($Pattern)) {
            Write-Host "  PASS: $Description" -ForegroundColor Green
            $script:testsPassed++
            $script:testResults += [PSCustomObject]@{
                Test = $TestName
                Status = "PASS"
                Description = $Description
            }
            return $true
        } else {
            Write-Host "  FAIL: $Description" -ForegroundColor Red
            Write-Host "    Expected pattern not found: $Pattern" -ForegroundColor Red
            $script:testsFailed++
            $script:testResults += [PSCustomObject]@{
                Test = $TestName
                Status = "FAIL"
                Description = $Description
            }
            return $false
        }
    } else {
        Write-Host "  FAIL: File not found - $FilePath" -ForegroundColor Red
        $script:testsFailed++
        $script:testResults += [PSCustomObject]@{
            Test = $TestName
            Status = "FAIL"
            Description = "File not found"
        }
        return $false
    }
}

Write-Host "1. Verifying File Table Column Layout" -ForegroundColor Cyan
Write-Host "   Checking for consistent column headers" -ForegroundColor Gray
Write-Host ""

# Test file-explorer.js for table structure
Test-FileTableElement `
    -TestName "File Table Headers" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider' `
    -Description "Table headers use consistent styling"

Test-FileTableElement `
    -TestName "Name Column Header" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'Name' `
    -Description "Name column header exists"

Test-FileTableElement `
    -TestName "Size Column Header" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'Size' `
    -Description "Size column header exists"

Test-FileTableElement `
    -TestName "Uploaded Column Header" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'Uploaded' `
    -Description "Uploaded column header exists"

Test-FileTableElement `
    -TestName "Uploader Column Header" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'Uploader' `
    -Description "Uploader column header exists"

Test-FileTableElement `
    -TestName "Actions Column Header" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'Actions' `
    -Description "Actions column header exists"

Write-Host ""
Write-Host "2. Verifying File Row Typography and Spacing" -ForegroundColor Cyan
Write-Host "   Checking for consistent row styling and hover effects" -ForegroundColor Gray
Write-Host ""

Test-FileTableElement `
    -TestName "File Row Hover Effect" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'hover:bg-gray-50 transition-all group' `
    -Description "File rows have hover effect"

Test-FileTableElement `
    -TestName "File Row Cell Padding" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'px-4 py-3 whitespace-nowrap' `
    -Description "File row cells use consistent padding"

Test-FileTableElement `
    -TestName "File Name Typography" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'text-sm font-medium text-gray-900 group-hover:text-blue-600 transition-colors' `
    -Description "File names use consistent typography"

Write-Host ""
Write-Host "3. Verifying File Icon Color Coding" -ForegroundColor Cyan
Write-Host "   Checking for consistent file type icon colors" -ForegroundColor Gray
Write-Host ""

Test-FileTableElement `
    -TestName "PDF Icon Color" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern "text-red-600" `
    -Description "PDF files use red icon color"

Test-FileTableElement `
    -TestName "ZIP Icon Color" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern "text-amber-600" `
    -Description "ZIP files use amber icon color"

Test-FileTableElement `
    -TestName "Word Document Icon Color" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern "text-blue-600" `
    -Description "Word documents use blue icon color"

Test-FileTableElement `
    -TestName "Image Icon Color" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern "text-green-600" `
    -Description "Images use green icon color"

Test-FileTableElement `
    -TestName "File Icon Container" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'w-8 h-8 flex items-center justify-center bg-gray-50 rounded mr-3 flex-shrink-0' `
    -Description "File icon container uses consistent styling"

Write-Host ""
Write-Host "4. Verifying Metadata Badge Styling" -ForegroundColor Cyan
Write-Host "   Checking for consistent badge styling" -ForegroundColor Gray
Write-Host ""

Test-FileTableElement `
    -TestName "Metadata Badge Styling" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-700' `
    -Description "Metadata badges use consistent styling"

Test-FileTableElement `
    -TestName "File Size Badge" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'file-metadata-badge' `
    -Description "File metadata badges have consistent class name"

Write-Host ""
Write-Host "5. Verifying Action Button Styling" -ForegroundColor Cyan
Write-Host "   Checking for consistent Download and View button styling" -ForegroundColor Gray
Write-Host ""

Test-FileTableElement `
    -TestName "View Button Styling" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'text-gray-600 hover:text-gray-900 p-1.5 rounded hover:bg-gray-100 transition-all' `
    -Description "View button uses consistent styling"

Test-FileTableElement `
    -TestName "Download Button Styling" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'text-white bg-blue-600 hover:bg-blue-700 p-1.5 rounded shadow-sm hover:shadow-md transition-all' `
    -Description "Download button uses consistent styling"

Test-FileTableElement `
    -TestName "Action Buttons Container" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'flex items-center justify-end space-x-2' `
    -Description "Action buttons container uses consistent layout"

Write-Host ""
Write-Host "6. Verifying Table Structure" -ForegroundColor Cyan
Write-Host "   Checking for consistent table layout and borders" -ForegroundColor Gray
Write-Host ""

Test-FileTableElement `
    -TestName "Table Container" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'min-w-full divide-y divide-gray-200 border border-gray-200 rounded-lg' `
    -Description "Table uses consistent container styling"

Test-FileTableElement `
    -TestName "Table Header Background" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'bg-gray-50' `
    -Description "Table header uses gray background"

Test-FileTableElement `
    -TestName "Table Body Background" `
    -FilePath "src/main/resources/static/js/file-explorer.js" `
    -Pattern 'bg-white divide-y divide-gray-200' `
    -Description "Table body uses white background with dividers"

Write-Host ""
Write-Host "7. Verifying Dashboard Integration" -ForegroundColor Cyan
Write-Host "   Checking that all dashboards use the FileExplorer component" -ForegroundColor Gray
Write-Host ""

Test-FileTableElement `
    -TestName "Professor Dashboard Uses FileExplorer" `
    -FilePath "src/main/resources/static/js/prof.js" `
    -Pattern "FileExplorer" `
    -Description "Professor dashboard imports FileExplorer component"

Test-FileTableElement `
    -TestName "HOD Dashboard Uses FileExplorer" `
    -FilePath "src/main/resources/static/js/hod.js" `
    -Pattern "FileExplorer" `
    -Description "HOD dashboard imports FileExplorer component"

Test-FileTableElement `
    -TestName "Deanship Dashboard Uses FileExplorer" `
    -FilePath "src/main/resources/static/js/deanship.js" `
    -Pattern "FileExplorer" `
    -Description "Deanship dashboard imports FileExplorer component"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Total Tests: $($testsPassed + $testsFailed)" -ForegroundColor White
Write-Host "Passed: $testsPassed" -ForegroundColor Green
Write-Host "Failed: $testsFailed" -ForegroundColor $(if ($testsFailed -eq 0) { "Green" } else { "Red" })
Write-Host ""

if ($testsFailed -eq 0) {
    Write-Host "All file table consistency tests passed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "File tables across all dashboards use:" -ForegroundColor Cyan
    Write-Host "  - Consistent column layout (Name, Size, Uploaded, Uploader, Actions)" -ForegroundColor Gray
    Write-Host "  - Consistent typography and spacing" -ForegroundColor Gray
    Write-Host "  - Consistent file icon colors (red for PDF, amber for ZIP, etc.)" -ForegroundColor Gray
    Write-Host "  - Consistent metadata badge styling" -ForegroundColor Gray
    Write-Host "  - Consistent action button styling" -ForegroundColor Gray
    Write-Host ""
    exit 0
} else {
    Write-Host "Some tests failed. Please review the failures above." -ForegroundColor Red
    Write-Host ""
    Write-Host "Failed Tests:" -ForegroundColor Yellow
    $testResults | Where-Object { $_.Status -eq "FAIL" } | ForEach-Object {
        Write-Host "  - $($_.Test): $($_.Description)" -ForegroundColor Red
    }
    Write-Host ""
    exit 1
}
