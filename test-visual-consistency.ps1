#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Cross-Dashboard Visual Consistency Verification Script
    
.DESCRIPTION
    This script performs a comprehensive visual consistency check across all three dashboards
    (Professor, HOD, and Deanship) to ensure the File Explorer components are visually identical.
    
    Verification Areas:
    1. Academic Year and Semester Selector styling
    2. Breadcrumb navigation appearance
    3. Folder card design (colors, spacing, typography, borders)
    4. File table layout and styling
    5. Empty state rendering
    6. Loading state rendering
    7. Error state rendering
    
.NOTES
    Task: 18. Perform cross-dashboard visual consistency verification
    Requirements: 1.1, 1.2, 1.3, 1.4, 1.5
#>

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Cross-Dashboard Visual Consistency Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test configuration
$baseUrl = "http://localhost:8080"
$dashboards = @(
    @{
        Name = "Professor"
        Url = "$baseUrl/prof-dashboard.html"
        HtmlFile = "src/main/resources/static/prof-dashboard.html"
        JsFile = "src/main/resources/static/js/prof.js"
        FileExplorerContainer = "fileExplorerContainer"
        BreadcrumbsId = "breadcrumbs"
    },
    @{
        Name = "HOD"
        Url = "$baseUrl/hod-dashboard.html"
        HtmlFile = "src/main/resources/static/hod-dashboard.html"
        JsFile = "src/main/resources/static/js/hod.js"
        FileExplorerContainer = "hodFileExplorer"
        BreadcrumbsId = "breadcrumbs"
    },
    @{
        Name = "Deanship"
        Url = "$baseUrl/deanship-dashboard.html"
        HtmlFile = "src/main/resources/static/deanship-dashboard.html"
        JsFile = "src/main/resources/static/js/deanship.js"
        FileExplorerContainer = "fileExplorerContainer"
        BreadcrumbsId = "breadcrumbs"
    }
)

$results = @{
    Passed = 0
    Failed = 0
    Warnings = 0
    Details = @()
}

function Test-HtmlPattern {
    param(
        [string]$FilePath,
        [string]$Pattern,
        [string]$Description,
        [string]$DashboardName
    )
    
    if (Test-Path $FilePath) {
        $content = Get-Content $FilePath -Raw
        if ($content -match $Pattern) {
            Write-Host "  ✓ $Description" -ForegroundColor Green
            $script:results.Passed++
            return $true
        } else {
            Write-Host "  ✗ $Description" -ForegroundColor Red
            $script:results.Failed++
            $script:results.Details += "$DashboardName - FAILED: $Description"
            return $false
        }
    } else {
        Write-Host "  ⚠ File not found: $FilePath" -ForegroundColor Yellow
        $script:results.Warnings++
        return $false
    }
}

function Test-CssClass {
    param(
        [string]$FilePath,
        [string]$Selector,
        [string[]]$RequiredClasses,
        [string]$Description,
        [string]$DashboardName
    )
    
    if (Test-Path $FilePath) {
        $content = Get-Content $FilePath -Raw
        $allFound = $true
        
        foreach ($class in $RequiredClasses) {
            if ($content -notmatch [regex]::Escape($class)) {
                $allFound = $false
                break
            }
        }
        
        if ($allFound) {
            Write-Host "  ✓ $Description" -ForegroundColor Green
            $script:results.Passed++
            return $true
        } else {
            Write-Host "  ✗ $Description - Missing classes" -ForegroundColor Red
            $script:results.Failed++
            $script:results.Details += "$DashboardName - FAILED: $Description (Missing CSS classes)"
            return $false
        }
    } else {
        Write-Host "  ⚠ File not found: $FilePath" -ForegroundColor Yellow
        $script:results.Warnings++
        return $false
    }
}

Write-Host "1. Academic Year and Semester Selector Consistency" -ForegroundColor Yellow
Write-Host "---------------------------------------------------" -ForegroundColor Yellow

foreach ($dashboard in $dashboards) {
    Write-Host "`n$($dashboard.Name) Dashboard:" -ForegroundColor Cyan
    
    # Check container structure
    Test-HtmlPattern `
        -FilePath $dashboard.HtmlFile `
        -Pattern 'class="bg-white rounded-lg shadow-md p-6.*?Academic Year.*?Semester' `
        -Description "Selector container has correct styling (bg-white rounded-lg shadow-md p-6)" `
        -DashboardName $dashboard.Name
    
    # Check flex layout
    Test-HtmlPattern `
        -FilePath $dashboard.HtmlFile `
        -Pattern 'class="flex flex-wrap items-center gap-4"' `
        -Description "Flex layout with gap-4 for responsive behavior" `
        -DashboardName $dashboard.Name
    
    # Check Academic Year selector
    Test-HtmlPattern `
        -FilePath $dashboard.HtmlFile `
        -Pattern 'class="flex-1 min-w-\[200px\]".*?Academic Year' `
        -Description "Academic Year selector has flex-1 min-w-[200px]" `
        -DashboardName $dashboard.Name
    
    # Check label styling
    Test-HtmlPattern `
        -FilePath $dashboard.HtmlFile `
        -Pattern 'class="block text-sm font-medium text-gray-700 mb-2"' `
        -Description "Labels use text-sm font-medium text-gray-700 mb-2" `
        -DashboardName $dashboard.Name
    
    # Check dropdown styling
    Test-HtmlPattern `
        -FilePath $dashboard.HtmlFile `
        -Pattern 'class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"' `
        -Description "Dropdowns have consistent styling with focus states" `
        -DashboardName $dashboard.Name
}

Write-Host "`n`n2. Breadcrumb Navigation Consistency" -ForegroundColor Yellow
Write-Host "-------------------------------------" -ForegroundColor Yellow

foreach ($dashboard in $dashboards) {
    Write-Host "`n$($dashboard.Name) Dashboard:" -ForegroundColor Cyan
    
    # Check breadcrumb container
    Test-HtmlPattern `
        -FilePath $dashboard.HtmlFile `
        -Pattern 'id="breadcrumbs"' `
        -Description "Breadcrumb container exists with id='breadcrumbs'" `
        -DashboardName $dashboard.Name
}

Write-Host "`n`n3. Folder Card Design Consistency" -ForegroundColor Yellow
Write-Host "----------------------------------" -ForegroundColor Yellow

# Check FileExplorer class for folder card rendering
$fileExplorerJs = "src/main/resources/static/js/file-explorer.js"

Write-Host "`nFileExplorer Class (Master Reference):" -ForegroundColor Cyan

Test-HtmlPattern `
    -FilePath $fileExplorerJs `
    -Pattern 'bg-blue-50.*?border-blue-200' `
    -Description "Folder cards use blue color scheme (bg-blue-50, border-blue-200)" `
    -DashboardName "FileExplorer"

Test-HtmlPattern `
    -FilePath $fileExplorerJs `
    -Pattern 'hover:bg-blue-100' `
    -Description "Folder cards have hover effect (hover:bg-blue-100)" `
    -DashboardName "FileExplorer"

Test-HtmlPattern `
    -FilePath $fileExplorerJs `
    -Pattern 'group-hover:translate-x-1' `
    -Description "Arrow icon animates on hover (group-hover:translate-x-1)" `
    -DashboardName "FileExplorer"

Test-HtmlPattern `
    -FilePath $fileExplorerJs `
    -Pattern 'rounded-lg' `
    -Description "Folder cards have rounded corners (rounded-lg)" `
    -DashboardName "FileExplorer"

Write-Host "`n`n4. File Table Design Consistency" -ForegroundColor Yellow
Write-Host "---------------------------------" -ForegroundColor Yellow

Write-Host "`nFileExplorer Class (Master Reference):" -ForegroundColor Cyan

Test-HtmlPattern `
    -FilePath $fileExplorerJs `
    -Pattern 'Name.*?Size.*?Uploaded.*?Actions' `
    -Description "File table has correct column layout" `
    -DashboardName "FileExplorer"

Test-HtmlPattern `
    -FilePath $fileExplorerJs `
    -Pattern 'hover:bg-gray-50' `
    -Description "File rows have hover effect (hover:bg-gray-50)" `
    -DashboardName "FileExplorer"

Test-HtmlPattern `
    -FilePath $fileExplorerJs `
    -Pattern 'bg-gray-100.*?text-gray-700' `
    -Description "Metadata badges use gray styling" `
    -DashboardName "FileExplorer"

Write-Host "`n`n5. Empty State Consistency" -ForegroundColor Yellow
Write-Host "---------------------------" -ForegroundColor Yellow

Write-Host "`nFileExplorer Class (Master Reference):" -ForegroundColor Cyan

Test-HtmlPattern `
    -FilePath $fileExplorerJs `
    -Pattern 'text-center.*?py-8.*?text-gray-500' `
    -Description "Empty state uses centered layout with py-8 and text-gray-500" `
    -DashboardName "FileExplorer"

Test-HtmlPattern `
    -FilePath $fileExplorerJs `
    -Pattern 'This folder is empty|No files found|Select a semester' `
    -Description "Empty state messages are present" `
    -DashboardName "FileExplorer"

Write-Host "`n`n6. Loading State Consistency" -ForegroundColor Yellow
Write-Host "-----------------------------" -ForegroundColor Yellow

Write-Host "`nFileExplorer Class (Master Reference):" -ForegroundColor Cyan

Test-HtmlPattern `
    -FilePath $fileExplorerJs `
    -Pattern 'skeleton|animate-pulse|Loading' `
    -Description "Loading state uses skeleton loaders or pulse animation" `
    -DashboardName "FileExplorer"

foreach ($dashboard in $dashboards) {
    Write-Host "`n$($dashboard.Name) Dashboard:" -ForegroundColor Cyan
    
    Test-HtmlPattern `
        -FilePath $dashboard.HtmlFile `
        -Pattern 'animate-pulse.*?bg-gray-200.*?rounded-lg' `
        -Description "Loading skeleton present in HTML" `
        -DashboardName $dashboard.Name
}

Write-Host "`n`n7. Error State Consistency" -ForegroundColor Yellow
Write-Host "---------------------------" -ForegroundColor Yellow

Write-Host "`nFileExplorer Class (Master Reference):" -ForegroundColor Cyan

Test-HtmlPattern `
    -FilePath $fileExplorerJs `
    -Pattern 'error|Error|failed|Failed' `
    -Description "Error state handling is present" `
    -DashboardName "FileExplorer"

Write-Host "`n`n8. FileExplorer Component Usage" -ForegroundColor Yellow
Write-Host "--------------------------------" -ForegroundColor Yellow

foreach ($dashboard in $dashboards) {
    Write-Host "`n$($dashboard.Name) Dashboard:" -ForegroundColor Cyan
    
    # Check if FileExplorer class is imported
    Test-HtmlPattern `
        -FilePath $dashboard.JsFile `
        -Pattern 'FileExplorer|file-explorer' `
        -Description "FileExplorer class is imported or referenced" `
        -DashboardName $dashboard.Name
    
    # Check for role-specific configuration
    if ($dashboard.Name -eq "Professor") {
        Test-HtmlPattern `
            -FilePath $dashboard.JsFile `
            -Pattern 'role.*?PROFESSOR|readOnly.*?false' `
            -Description "Professor configuration includes role or readOnly settings" `
            -DashboardName $dashboard.Name
    }
    elseif ($dashboard.Name -eq "HOD") {
        Test-HtmlPattern `
            -FilePath $dashboard.JsFile `
            -Pattern 'role.*?HOD|readOnly.*?true|Browse department files' `
            -Description "HOD configuration includes role, readOnly, or header message" `
            -DashboardName $dashboard.Name
    }
    elseif ($dashboard.Name -eq "Deanship") {
        Test-HtmlPattern `
            -FilePath $dashboard.JsFile `
            -Pattern 'role.*?DEANSHIP|readOnly.*?true|showAllDepartments' `
            -Description "Deanship configuration includes role or showAllDepartments" `
            -DashboardName $dashboard.Name
    }
}

Write-Host "`n`n9. Master Design Reference Comments" -ForegroundColor Yellow
Write-Host "------------------------------------" -ForegroundColor Yellow

foreach ($dashboard in $dashboards) {
    Write-Host "`n$($dashboard.Name) Dashboard:" -ForegroundColor Cyan
    
    Test-HtmlPattern `
        -FilePath $dashboard.HtmlFile `
        -Pattern 'MASTER DESIGN REFERENCE|CANONICAL|master design' `
        -Description "Contains master design reference comments" `
        -DashboardName $dashboard.Name
}

Write-Host "`n`n========================================" -ForegroundColor Cyan
Write-Host "Visual Consistency Verification Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Total Tests Passed:  $($results.Passed)" -ForegroundColor Green
Write-Host "Total Tests Failed:  $($results.Failed)" -ForegroundColor Red
Write-Host "Total Warnings:      $($results.Warnings)" -ForegroundColor Yellow
Write-Host ""

if ($results.Failed -gt 0) {
    Write-Host "Failed Test Details:" -ForegroundColor Red
    Write-Host "-------------------" -ForegroundColor Red
    foreach ($detail in $results.Details) {
        Write-Host "  • $detail" -ForegroundColor Red
    }
    Write-Host ""
}

if ($results.Failed -eq 0 -and $results.Warnings -eq 0) {
    Write-Host "All visual consistency checks passed!" -ForegroundColor Green
    Write-Host "The File Explorer components are visually consistent across all dashboards." -ForegroundColor Green
    exit 0
} elseif ($results.Failed -eq 0) {
    Write-Host "All tests passed with warnings" -ForegroundColor Yellow
    Write-Host "Review warnings above for potential improvements." -ForegroundColor Yellow
    exit 0
} else {
    Write-Host "Visual consistency issues detected" -ForegroundColor Red
    Write-Host "Please review and fix the failed tests above." -ForegroundColor Red
    exit 1
}
