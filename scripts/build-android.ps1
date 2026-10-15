param([switch]$Run)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$socketTemp = Join-Path $projectRoot '.tools\socket-temp'
New-Item -ItemType Directory -Force -Path $socketTemp | Out-Null
$previousOptions = $env:JAVA_TOOL_OPTIONS
Push-Location -LiteralPath $projectRoot
try {
    # Keep Windows Java Unix-domain sockets in a predictable, writable directory.
    $env:JAVA_TOOL_OPTIONS = $previousOptions + ' "-Djdk.net.unixdomain.tmpdir=' + $socketTemp + '"'
    & .\gradlew.bat --gradle-user-home .tools\gradle-home :app:assembleDebug --no-daemon
    if ($LASTEXITCODE -ne 0) { throw 'Build failed. APK was not installed. See the Gradle error above.' }

    $apk = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) { throw "APK not found: $apk" }
    Write-Host "APK: $apk"

    if ($Run) {
        $sdkLine = Get-Content -LiteralPath (Join-Path $projectRoot 'local.properties') |
            Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if (-not $sdkLine) { throw 'Set sdk.dir in local.properties first.' }
        $sdkPath = ($sdkLine -replace '^sdk\.dir=', '').Trim()
        $adb = Join-Path $sdkPath 'platform-tools\adb.exe'
        if (-not (Test-Path -LiteralPath $adb)) { throw "ADB not found: $adb" }
        & $adb -e get-state
        if ($LASTEXITCODE -ne 0) { throw 'Start exactly one Android emulator, wait for Android to boot, and retry.' }
        & $adb -e install -r $apk
        if ($LASTEXITCODE -ne 0) { throw 'APK installation failed. App was not started.' }
        & $adb -e shell am start -W -n vn.quietcall/.MainActivity
        if ($LASTEXITCODE -ne 0) { throw 'App launch failed. See ADB output above.' }
    }
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousOptions
    Pop-Location
}
