@echo off
:: Check for administrative privileges
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo =======================================================
    echo ERROR: Please right-click this file and select:
    echo "Run as administrator"
    echo =======================================================
    pause
    exit /b 1
)

echo Installing Android Emulator Hypervisor Driver...
cd /d "%~dp0aehd_driver"
sc stop gvm >nul 2>&1
sc delete gvm >nul 2>&1
sc stop aehd >nul 2>&1
sc delete aehd >nul 2>&1

RUNDLL32.EXE SETUPAPI.DLL,InstallHinfSection DefaultInstall 132 .\aehd.Inf
sc start aehd

echo.
echo =======================================================
echo SUCCESS! Android Emulator Driver installed successfully.
echo You can now go to Android Studio and click Run (Play)!
echo =======================================================
pause

