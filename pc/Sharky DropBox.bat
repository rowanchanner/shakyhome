@echo off
setlocal
set "DROPBOX_URL=http://192.168.0.39:8787"
if exist "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" (
    start "" "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" --app="%DROPBOX_URL%"
    exit /b
)
if exist "%ProgramFiles%\Microsoft\Edge\Application\msedge.exe" (
    start "" "%ProgramFiles%\Microsoft\Edge\Application\msedge.exe" --app="%DROPBOX_URL%"
    exit /b
)
start "" "%DROPBOX_URL%"
