@echo off
setlocal
cd /d "%~dp0"

echo ===============================================================
echo  FastGrab JMH Benchmark Runner
echo ===============================================================

call mvn -f benchmark/pom.xml clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven build failed!
    exit /b %ERRORLEVEL%
)

echo.
echo [INFO] Running JMH Benchmarks...
java --enable-native-access=ALL-UNNAMED -jar benchmark/target/benchmarks.jar -wi 2 -i 3 -f 1
