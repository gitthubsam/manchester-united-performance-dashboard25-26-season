@echo off
setlocal
cd /d "%~dp0"
call build-windows.bat
if errorlevel 1 exit /b 1
if not exist test-out mkdir test-out
(for %%F in (tests\*.java) do @echo "%%~fF") > tests.list
javac -encoding UTF-8 -cp out -d test-out @tests.list
set RESULT=%ERRORLEVEL%
del tests.list
if not "%RESULT%"=="0" exit /b %RESULT%
java -cp out;test-out SeasonRegressionTest
if errorlevel 1 exit /b 1
java -cp out;test-out FontThemeTest
if errorlevel 1 exit /b 1
java -cp out;test-out DashboardUiSmokeTest
pause
