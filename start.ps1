param([switch]$BuildOnly, [switch]$TestOnly, [switch]$NoBrowser, [int]$Port = 8080)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
try {
    $runtimeDir = Join-Path $PSScriptRoot '.runtime'
    $javaDir = Join-Path $runtimeDir 'java'
    $javaExe = Get-ChildItem -LiteralPath $javaDir -Filter java.exe -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '\\bin\\java.exe$' } | Select-Object -First 1
    if (-not $javaExe) {
        Write-Host 'Elso inditas: hordozhato Java 21 letoltese (~200 MB).'
        Write-Host 'A rendszer Java-beallitasai nem valtoznak.'
        New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null
        $metadata = Invoke-RestMethod 'https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows'
        $package = $metadata[0].binary.package
        $archive = Join-Path $runtimeDir 'java-download.zip'
        Invoke-WebRequest -UseBasicParsing $package.link -OutFile $archive
        if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -ne $package.checksum) {
            throw 'A Java letoltes ellenorzese sikertelen. Inditsd ujra a programot.'
        }
        Expand-Archive -LiteralPath $archive -DestinationPath $javaDir -Force
        Remove-Item -LiteralPath $archive
        $javaExe = Get-ChildItem -LiteralPath $javaDir -Filter java.exe -Recurse |
            Where-Object { $_.FullName -match '\\bin\\java.exe$' } | Select-Object -First 1
    }
    if (-not $javaExe) { throw 'A Java nem talalhato.' }
    $env:JAVA_HOME = Split-Path (Split-Path $javaExe.FullName -Parent) -Parent
    $env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:PATH
    $env:MAVEN_USER_HOME = Join-Path $runtimeDir 'maven'
    $repo = Join-Path $runtimeDir 'repository'
    $appJar = Join-Path $PSScriptRoot 'movieclub.jar'
    if ($BuildOnly -or $TestOnly -or -not (Test-Path -LiteralPath $appJar)) {
        Write-Host 'Forditas es automatikus tesztek. Az elso alkalom tobb perc lehet.'
        & .\mvnw.cmd "-Dmaven.repo.local=$repo" -B -ntp clean verify
        if ($LASTEXITCODE -ne 0) { throw 'A forditas vagy egy teszt sikertelen. A hiba fent olvashato.' }
        Copy-Item -LiteralPath 'movieclub-web\target\movieclub-web-1.0.0.jar' -Destination $appJar -Force
    }
    if ($BuildOnly -or $TestOnly) { Write-Host 'Sikeres forditas es teszteles.'; exit 0 }
    if ($Port -lt 1024 -or $Port -gt 65535) { throw 'A port 1024 es 65535 kozotti szam legyen.' }
    $probe = New-Object Net.Sockets.TcpClient
    try {
        $probe.Connect('127.0.0.1',$Port)
        throw "A $Port port foglalt. Hasznald: INDITAS.cmd -Port 8081"
    } catch [Net.Sockets.SocketException] {
        # A szabad port elvart eredmenye a visszautasitott kapcsolat.
    } finally { $probe.Dispose() }
    $url = "http://localhost:$Port"
    Write-Host ""
    Write-Host "MovieClub indul: $url"
    Write-Host 'Leallitas: Ctrl+C ebben az ablakban.'
    $opener = if (-not $NoBrowser) { Start-Job -ArgumentList $url -ScriptBlock {
        param($targetUrl)
        for ($i=0; $i -lt 90; $i++) {
            try {
                $response=Invoke-WebRequest -UseBasicParsing $targetUrl -TimeoutSec 2
                if ($response.StatusCode -eq 200) { Start-Process $targetUrl -WindowStyle Hidden; return }
            } catch {}
            Start-Sleep -Seconds 1
        }
    } }
    try { & $javaExe.FullName -jar $appJar "--server.port=$Port" }
    finally { if ($opener) { Stop-Job $opener -ErrorAction SilentlyContinue; Remove-Job $opener -Force -ErrorAction SilentlyContinue } }
    if ($LASTEXITCODE -ne 0) { throw 'Az alkalmazas hibaval leallt. A reszletek fent olvashatok.' }
} catch {
    Write-Host ""
    Write-Host ("HIBA: " + $_.Exception.Message) -ForegroundColor Red
    Write-Host 'Segitseg: OLVASS-EL.md'
    exit 1
}
