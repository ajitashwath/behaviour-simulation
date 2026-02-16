@echo off
echo ==========================================
echo Behavior Simulation Platform - Dependency Installer
echo ==========================================

echo.
echo This script will install required dependencies using Chocolatey.
echo IMPORTANT: You may need to run this script as Administrator.
echo.

echo [1/2] Installing OpenJDK 17...
choco install openjdk17 -y
if %errorlevel% neq 0 (
    echo [FAIL] Failed to install OpenJDK 17. Ensure you are running as Administrator.
) else (
    echo [PASS] OpenJDK 17 installed.
)

echo.
echo [2/2] Installing Maven...
choco install maven -y
if %errorlevel% neq 0 (
    echo [FAIL] Failed to install Maven.
) else (
    echo [PASS] Maven installed.
)

echo.
echo ==========================================
echo Installation Complete
echo ==========================================
echo Please restart your terminal/IDE to refresh PATH variables.
echo Then run 'mvn clean compile' to build the project.
echo.
pause
