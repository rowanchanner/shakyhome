@echo off
setlocal
cd /d "%~dp0"
where git >nul 2>&1
if errorlevel 1 (
  echo Git for Windows is required: https://git-scm.com/download/win
  exit /b 1
)
if not exist .git git init
git branch -M main
git remote remove origin >nul 2>&1
git remote add origin https://github.com/rowanchanner/shakyhome.git
git add .
git commit -m "Initial Sharky Home Fire TV launcher"
git push -u origin main
if errorlevel 1 (
  echo Push needs GitHub sign-in. Complete the Git credential prompt, then run this file again.
  exit /b 1
)
echo Published to GitHub.
pause
