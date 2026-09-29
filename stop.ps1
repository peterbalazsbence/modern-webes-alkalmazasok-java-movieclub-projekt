$ErrorActionPreference = 'Stop'
$expectedJar = Join-Path $PSScriptRoot 'movieclub.jar'
$runtimeRoot = (Join-Path $PSScriptRoot '.runtime\java') + '\'
$processes = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object {
    $_.CommandLine -and $_.ExecutablePath -and
    $_.CommandLine.IndexOf($expectedJar,[StringComparison]::OrdinalIgnoreCase) -ge 0 -and
    $_.ExecutablePath.StartsWith($runtimeRoot,[StringComparison]::OrdinalIgnoreCase)
}
if (-not $processes) { Write-Host 'Ez a MovieClub-peldany nem fut.'; exit 0 }
foreach ($movieProcess in $processes) { Stop-Process -Id $movieProcess.ProcessId -ErrorAction Stop }
Write-Host 'MovieClub leallitva. Az elmentett adatok megmaradnak.'
