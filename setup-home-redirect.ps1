$ErrorActionPreference = 'Stop'
$address = Read-Host 'Fire TV IP address (Settings > My Fire TV > About > Network)'
$parsed = $null
if (-not [System.Net.IPAddress]::TryParse($address, [ref]$parsed) -or $parsed.AddressFamily -ne [System.Net.Sockets.AddressFamily]::InterNetwork) { throw 'Enter an IPv4 address, such as 192.168.1.50.' }
$taskDir = Join-Path $env:LOCALAPPDATA 'SharkyHome\setup'
New-Item -ItemType Directory -Force -Path $taskDir | Out-Null
$adb = Join-Path $taskDir 'platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $adb)) {
    Write-Host 'Downloading Android platform tools from Google...'
    Invoke-WebRequest 'https://dl.google.com/android/repository/platform-tools-latest-windows.zip' -OutFile (Join-Path $taskDir 'tools.zip')
    Expand-Archive -LiteralPath (Join-Path $taskDir 'tools.zip') -DestinationPath $taskDir -Force
}
$serial = "${address}:5555"
& $adb connect $serial
Read-Host 'Accept the debugging prompt on the Fire TV, then press Enter here'
& $adb -s $serial get-state
if ($LASTEXITCODE -ne 0) { throw 'Fire TV is not connected. Enable ADB debugging and accept the prompt.' }
$installed = & $adb -s $serial shell pm path io.github.toolicious.homeonfire
if (-not ($installed -match 'package:')) { throw 'Install Home on Fire with Downloader first: https://github.com/toolicious/home-on-fire/releases/latest/download/home-on-fire.apk' }
Write-Host 'Granting Home on Fire permission to manage its accessibility switch, as requested for Home/startup redirect.'
& $adb -s $serial shell pm grant io.github.toolicious.homeonfire android.permission.WRITE_SECURE_SETTINGS
if ($LASTEXITCODE -ne 0) { throw 'Fire OS refused the permission grant.' }
& $adb -s $serial shell am start -n io.github.toolicious.homeonfire/.MainActivity
Write-Host 'On the TV: enable Accessibility service, choose Sharky Home as target, and enable Launch on boot.'
