@echo off
setlocal
cd /d "%~dp0"
echo ===============================================================
echo  FastScreenCapture Launcher - Bit-Perfect Uncompressed Screen Grabber
echo ===============================================================

REM Pass all arguments directly to the Maven exec plugin
mvn exec:java -Dexec.mainClass="fastscreencapture.FastScreenCapture" -Dexec.args="%*"
