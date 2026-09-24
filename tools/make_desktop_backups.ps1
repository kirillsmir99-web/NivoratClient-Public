$ErrorActionPreference = "Stop"
$root = (Get-Item ".").FullName
$outFile = "C:\Users\Administrator\Desktop\NivoratClient_ALL_CODE.txt"
$zipFile = "C:\Users\Administrator\Desktop\NivoratClient-Source.zip"

Write-Host "Regenerating $outFile..."
$files = Get-ChildItem -Path "src" -Recurse -File | Where-Object { $_.FullName -notmatch '\\(build|\.gradle|\.git)\\' } | Sort-Object FullName
$sb = New-Object System.Text.StringBuilder
foreach ($f in $files) {
    $rel = Resolve-Path -Path $f.FullName -Relative
    $cleanRel = $rel.TrimStart(".\")
    [void]$sb.AppendLine("================================================================================")
    [void]$sb.AppendLine($cleanRel)
    [void]$sb.AppendLine("================================================================================")
    $content = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
    [void]$sb.AppendLine($content)
}
[System.IO.File]::WriteAllText($outFile, $sb.ToString(), [System.Text.Encoding]::UTF8)

Write-Host "Regenerating $zipFile..."
if (Test-Path $zipFile) {
    Remove-Item -Path $zipFile -Force
}

$tempZipDir = "$env:TEMP\NivoratClient_Source_Pack"
if (Test-Path $tempZipDir) {
    Remove-Item -Path $tempZipDir -Recurse -Force
}
New-Item -ItemType Directory -Path $tempZipDir | Out-Null

$includePaths = @("src", "tools", "build.gradle", "settings.gradle", "gradle.properties", "fabric.mod.json", "build_run.ps1", "gradle", "gradlew", "gradlew.bat")
foreach ($p in $includePaths) {
    if (Test-Path $p) {
        Copy-Item -Path $p -Destination $tempZipDir -Recurse -Force
    }
}

Compress-Archive -Path "$tempZipDir\*" -DestinationPath $zipFile -Force
Remove-Item -Path $tempZipDir -Recurse -Force

Write-Host "Backups updated successfully:"
Get-Item $outFile, $zipFile | Select-Object Name, Length, LastWriteTime
