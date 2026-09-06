@echo off
setlocal
set "FIRE_TV_IP=%~1"
if "%FIRE_TV_IP%"=="" set /p "FIRE_TV_IP=Enter your Fire TV IP address (for example 192.168.1.50): "
if "%FIRE_TV_IP%"=="" (
  echo No IP address supplied.
  exit /b 1
)

set "ADB=adb"
where adb >nul 2>&1
if errorlevel 1 (
  if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
)
"%ADB%" version >nul 2>&1
if errorlevel 1 (
  echo ADB was not found. Install Android Studio, then install Android SDK Platform-Tools.
  echo Restart this file after adding platform-tools to PATH.
  exit /b 1
)

call "%~dp0gradlew.bat" assembleDebug
if errorlevel 1 (
  echo Build failed. Open this folder in Android Studio and install Android SDK Platform 35.
  exit /b 1
)

echo Connecting to Fire TV at %FIRE_TV_IP%:5555...
"%ADB%" connect %FIRE_TV_IP%:5555
"%ADB%" install -r "%~dp0app\build\outputs\apk\debug\app-debug.apk"
if errorlevel 1 (
  echo Install failed. Check ADB debugging is enabled and accept the prompt on the Fire TV.
  exit /b 1
)
echo.
echo Sharky Home installed. Open it from Your Apps, then configure your Home redirect as described in README.md.
pause
