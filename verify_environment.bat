@echo off
echo ==========================================
echo Behavior Simulation Platform - Logic Verification
echo ==========================================

echo.
echo [1/3] Checking Java Environment...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [FAIL] Java is not installed or not in PATH.
    echo        Please install JDK 11 or higher.
) else (
    echo [PASS] Java is available.
)

echo.
echo [2/3] Checking Maven Environment...
call mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [FAIL] Maven is not installed or not in PATH.
    echo        Maven is required to build the Java simulation engine.
    echo        Please install Maven: https://maven.apache.org/download.cgi
    echo        OR use Chocolatey: choco install maven
) else (
    echo [PASS] Maven is available.
)

echo.
echo [3/3] Running Python Logic Verification...
python tests/test_continuous_emotions.py
if %errorlevel% neq 0 (
    echo [FAIL] Python tests failed. Check logs.
) else (
    echo [PASS] Python logic verification successful.
)

echo.
echo ==========================================
echo Verification Summary
echo ==========================================
echo.
echo To run full integration tests (Java + Spark), you must fix any [FAIL] items above.
echo.
pause
