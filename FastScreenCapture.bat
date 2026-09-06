@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul

REM Build classpath cache if missing or compile if classes not found
if not exist "target\classes\fastscreencapture\FastScreenCapture.class" (
    echo [FastScreenCapture] Compiling classes...
    call mvn clean compile -q
)

if not exist "target\cp.txt" (
    echo [FastScreenCapture] Resolving dependency classpath...
    call mvn dependency:build-classpath -Dmdep.outputFile=target\cp.txt -q
)

set /p FSC_CP=<target\cp.txt

java -Dfile.encoding=UTF-8 -cp "target\classes;%FSC_CP%" fastscreencapture.FastScreenCapture %*

