@echo off
setlocal
cd /d "%~dp0"
echo ===============================================================
echo  FastGrab Launcher - Bit-Perfect Uncompressed Screen Grabber
echo ===============================================================

REM Pass all arguments directly to the Maven exec plugin
mvn exec:java -Dexec.mainClass="fastgrab.FastGrab" -Dexec.args="%*"
