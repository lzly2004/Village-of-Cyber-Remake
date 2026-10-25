@echo off
setlocal
cd /d "%~dp0"

set "APP_NAME=VOC_Remake"
set "APP_VERSION=1.0.3.4"
set "FAT_JAR=vocr-%APP_VERSION%-all.jar"

call :find_jdk
if errorlevel 1 exit /b 1

if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "GRADLE_OPTS=%GRADLE_OPTS% -Dfile.encoding=COMPAT"

if "%~1"=="" goto build
if /i "%~1"=="build" goto build
if /i "%~1"=="quick" goto quick
if /i "%~1"=="test" goto test
if /i "%~1"=="run" goto run
if /i "%~1"=="package" goto package
goto usage

:build
call gradlew.bat clean test fatJar --console=plain
if errorlevel 1 exit /b 1
echo [OK] build\libs\%FAT_JAR%
exit /b 0

:quick
call gradlew.bat test fatJar --console=plain
if errorlevel 1 exit /b 1
echo [OK] build\libs\%FAT_JAR%
exit /b 0

:test
call gradlew.bat test --console=plain
exit /b %ERRORLEVEL%

:run
call :quick
if errorlevel 1 exit /b 1
"%JAVA_HOME%\bin\java.exe" -jar "build\libs\%FAT_JAR%"
exit /b %ERRORLEVEL%

:package
call :build
if errorlevel 1 exit /b 1
if not exist "%JAVA_HOME%\bin\jpackage.exe" (
    echo [ERROR] jpackage.exe was not found in JDK 21.
    exit /b 1
)
if exist "portable_app\%APP_NAME%" rmdir /s /q "portable_app\%APP_NAME%"
"%JAVA_HOME%\bin\jpackage.exe" ^
    --type app-image ^
    --name "%APP_NAME%" ^
    --app-version "%APP_VERSION%" ^
    --input "build\libs" ^
    --main-jar "%FAT_JAR%" ^
    --dest "portable_app"
if errorlevel 1 exit /b 1
if not exist "portable_app\%APP_NAME%\config" mkdir "portable_app\%APP_NAME%\config"
copy /y "config\probability.json" "portable_app\%APP_NAME%\config\probability.json" >nul
echo [OK] portable_app\%APP_NAME%
exit /b 0

:usage
echo Usage: build.bat [build^|quick^|test^|run^|package]
exit /b 2

:find_jdk
set "JDK_CANDIDATE="
for /f "delims=" %%I in ('where javac.exe 2^>nul') do if not defined JDK_CANDIDATE (
    for %%J in ("%%~dpI..") do set "JDK_CANDIDATE=%%~fJ"
)
if not defined JDK_CANDIDATE if defined JAVA_HOME set "JDK_CANDIDATE=%JAVA_HOME%"
if not defined JDK_CANDIDATE (
    echo [ERROR] JDK 21 was not found. Set JAVA_HOME or add its bin directory to PATH.
    exit /b 1
)
for /f "tokens=2" %%V in ('"%JDK_CANDIDATE%\bin\javac.exe" -version 2^>^&1') do set "JAVAC_VERSION=%%V"
if not "%JAVAC_VERSION:~0,2%"=="21" (
    echo [ERROR] JDK 21 is required; found javac %JAVAC_VERSION% at %JDK_CANDIDATE%.
    exit /b 1
)
set "JAVA_HOME=%JDK_CANDIDATE%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo [OK] Using JDK %JAVAC_VERSION% from %JAVA_HOME%
exit /b 0
