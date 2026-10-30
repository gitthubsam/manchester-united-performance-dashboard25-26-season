@echo off
setlocal
cd /d "%~dp0"
call build-windows.bat
if errorlevel 1 (
    echo Compilation failed. Review the messages above.
    pause
    exit /b 1
)
java -cp out ManchesterUnitedDashboard
if errorlevel 1 pause
