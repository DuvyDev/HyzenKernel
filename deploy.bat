@echo off
setlocal

set "ROOT=%~dp0"
cd /d "%ROOT%"

call gradlew.bat shadowJar :hyzenkernel-early:jar
if errorlevel 1 exit /b 1

set "OUTPUT_DIR=%ROOT%output"
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

call :pickNewest "build\libs" runtimeJar
if not defined runtimeJar (
  echo No se encontro el JAR del plugin en build\libs
  exit /b 1
)
copy /y "%runtimeJar%" "%OUTPUT_DIR%" >nul

call :pickNewest "hyzenkernel-early\build\libs" earlyJar
if not defined earlyJar (
  echo No se encontro el JAR del earlyplugin en hyzenkernel-early\build\libs
  exit /b 1
)
copy /y "%earlyJar%" "%OUTPUT_DIR%" >nul

echo Deploy completo. JARs copiados a %OUTPUT_DIR%
exit /b 0

:pickNewest
set "dir=%~1"
set "varName=%~2"
for /f "delims=" %%F in ('dir /b /a:-d /o:-d "%dir%\*.jar" ^| findstr /v /i "sources javadoc plain"') do (
  set "%varName%=%dir%\%%F"
  goto :eof
)
set "%varName%="
exit /b 0
