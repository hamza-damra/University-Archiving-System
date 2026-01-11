Get-ChildItem -Path "c:\Users\Hamza Damra\Documents\University-Archiving-System\src\main\java" -Filter "*.java" -Recurse | ForEach-Object {
    $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        $newBytes = New-Object byte[] ($bytes.Length - 3)
        [Array]::Copy($bytes, 3, $newBytes, 0, $newBytes.Length)
        [System.IO.File]::WriteAllBytes($_.FullName, $newBytes)
        Write-Host "Fixed: $($_.Name)"
    }
}
