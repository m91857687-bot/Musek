@echo off
setlocal EnableDelayedExpansion
set GRADLE_VERSION=8.13
set BASE_DIR=%USERPROFILE%\.gradle\music-player-promax-bootstrap
set INSTALL_DIR=%BASE_DIR%\gradle-%GRADLE_VERSION%
set GRADLE_BIN=%INSTALL_DIR%\bin\gradle.bat
if not exist "%GRADLE_BIN%" (
  if not exist "%BASE_DIR%" mkdir "%BASE_DIR%"
  set ARCHIVE=%BASE_DIR%\gradle-%GRADLE_VERSION%-bin.zip
  if not exist "!ARCHIVE!" powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '!ARCHIVE!'"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '!ARCHIVE!' '%BASE_DIR%'"
)
call "%GRADLE_BIN%" %*
endlocal
