# Script to add @SuppressWarnings("deprecation") to generated MapStruct implementation
# Run this after 'mvn clean compile' to suppress IDE warnings for deprecated legacy code

$file = "target/generated-sources/annotations/com/alqude/edu/ArchiveSystem/mapper/DocumentRequestMapperImpl.java"

if (Test-Path $file) {
    $content = Get-Content $file -Raw
    
    # Check if @SuppressWarnings is already present
    if ($content -notmatch '@SuppressWarnings\("deprecation"\)') {
        # Add @SuppressWarnings before the class declaration with proper newline
        $content = $content -replace '(@Component\s*\r?\n)(public class)', "`$1@SuppressWarnings(`"deprecation`")`r`n`$2"
        
        Set-Content -Path $file -Value $content -NoNewline
        Write-Host "✓ Added @SuppressWarnings annotation to DocumentRequestMapperImpl" -ForegroundColor Green
    } else {
        Write-Host "✓ @SuppressWarnings annotation already present" -ForegroundColor Yellow
    }
} else {
    Write-Host "✗ Generated file not found. Run 'mvnw clean compile' first." -ForegroundColor Red
}
