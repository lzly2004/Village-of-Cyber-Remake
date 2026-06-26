 @echo off
setlocal enabledelayedexpansion

:: ============================================================
::  VOC Remake - Build Script v5.0
::  Features:
::    - Auto-detects JDK 21 from IDEA JBR
::    - Correctly bundles dependencies (extracts JARs)
::    - jlink slim JRE (~50MB) instead of full JDK
::    - jpackage portable app-image (no WiX needed)
::    - Copies config/ to exe folder for filesystem-loaded resources
::    - Interactive with pauses for user choices
:: ============================================================

title VOC Builder v5.0

:: ==================== CONFIG ====
set VERSION=1.0.3.4
set APP_NAME=VOC_Remake
set MAIN_CLASS=Game
set OUTPUT_DIR=portable_app

:: Personal data files to EXCLUDE from distribution (users get clean install)
:: Note: Config JSONs (probability, settings, etc.) ARE included!
set EXCLUDE_FILES=replay_saves.json game_record.json

:: Directories to EXCLUDE from packaging (prevents jpackage recursion!)
:: CRITICAL: Without this, .git causes 20GB+ recursive loops!
set EXCLUDE_DIRS=.git .idea .gradle build out node_modules __pycache__ .vs
:: ==============================

echo.
echo ============================================
echo   Village of Cyber Remake Builder v%VERSION%
echo ============================================
echo.

:: Go to script directory first
cd /d "%~dp0"
if errorlevel 1 (
    echo [ERROR] Cannot change to script directory!
    pause
    exit /b 1
)
echo   [OK] Working dir: %CD%

echo ============================================
echo   VOC Remake Builder v5.0
echo ============================================
echo.

:: Find JDK 21 (check IDEA JBR first, then JAVA_HOME, then PATH)
echo [1/7] Detecting JDK 21...
set "JDK_PATH="

:: Check IDEA installations (most likely location)
if exist "C:\Program Files\JetBrains\IntelliJ IDEA*\jbr\bin\java.exe" (
    for /d %%i in ("C:\Program Files\JetBrains\IntelliJ IDEA*") do (
        if exist "%%i\jbr\bin\java.exe" set "JDK_PATH=%%i\jbr"
    )
)

:: Check JAVA_HOME environment variable
if "%JDK_PATH%"=="" (
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\java.exe" set "JDK_PATH=%JAVA_HOME%"
    )
)

:: Last resort: check PATH
if "%JDK_PATH%"=="" (
    where java >nul 2>&1
    if not errorlevel 1 (
        for /f "delims=" %%i in ('where java') do (
            set "JAVA_EXE=%%i"
        )
        :: Extract directory from path
        for /f "delims=" %%j in ("%JAVA_EXE:~0,-9%") do (
            set "JDK_PATH=%%j"
        )
    )
)

:: Final check
if "%JDK_PATH%"=="" (
    echo   [ERROR] JDK 21 not found!
    echo   Please install JDK 21 or ensure IntelliJ IDEA is installed
    echo.
    pause
    exit /b 1
)

echo   [OK] Found JDK at: %JDK_PATH%

:: Show Java version (safe method)
for /f "tokens=3 delims= " %%a in ('"%JDK_PATH%\bin\java.exe" -version 2^>^&1') do (
    if not defined JAVA_VER set "JAVA_VER=%%a"
)
echo   Java version: %JAVA_VER%

:: Check compiled classes
echo [2/7] Checking compiled classes...
if not exist "vocr\out\production\vocr\%MAIN_CLASS%.class" (
    echo   [ERROR] %MAIN_CLASS%.class not found!
    echo   Please run: Build -^> Rebuild Project in IDEA
    echo   Then run this script again.
    pause
    exit /b 1
)
echo   [OK] Classes found

:: Clean old files
echo [3/7] Cleaning old files...
if exist "%APP_NAME%_v%VERSION%.jar" (
    del /f "%APP_NAME%_v%VERSION%.jar" >nul 2>&1
    echo   [OK] Removed old JAR
) else (
    echo   [OK] No old JAR to clean
)

if exist "temp_build" rmdir /s /q "temp_build" >nul 2>&1
if exist "%OUTPUT_DIR%" (
    echo   Removing old portable_app - may take a moment...
    rmdir /s /q "%OUTPUT_DIR%" >nul 2>&1
)
echo   [OK] Cleanup complete

:: Verify paths and locate Jackson libraries
echo [4/7] Locating dependency libraries...
set "JACKSON_DIR="

if exist "bin\jackson-databind-2.17.1.jar" (
    set "JACKSON_DIR=bin"
    echo   [OK] Found Jackson libs in bin\
) else if exist "vocr\bin\jackson-databind-2.17.1.jar" (
    set "JACKSON_DIR=vocr\bin"
    echo   [OK] Found Jackson libs in vocr\bin\
) else (
    echo   [ERROR] Jackson libraries not found!
    echo   Expected locations:
    echo     - bin\jackson-*.jar
    echo     - vocr\bin\jackson-*.jar
    pause
    exit /b 1
)

:: Create working directory and extract all files
echo.
echo [5/7] Preparing build files - this may take a minute...
echo   Creating temp directory structure...

mkdir "temp_build" 2>nul

:: Copy compiled classes
echo   Copying compiled classes...
xcopy /E /I /Y /Q "vocr\out\production\vocr\*" "temp_build\" >nul

:: Copy resources (all config JSONs included!)
echo   Copying resources and configs...
xcopy /E /I /Y /Q "vocr\resources\*" "temp_build" >nul

:: Log which personal data files are being excluded
echo   Personal data exclusion list: %EXCLUDE_FILES%

:: [CRITICAL] Remove dev directories to prevent jpackage recursion disaster!
echo   Excluding dev directories: %EXCLUDE_DIRS%
for %%D in (%EXCLUDE_DIRS%) do (
    if exist "temp_build\%%D" (
        echo     [REMOVING] %%D (prevents 20GB+ bloat)
        rmdir /s /q "temp_build\%%D" >nul 2>&1
    )
)

:: Extract Jackson libraries (simple copy + local extract method)
echo   Extracting Jackson dependencies...

:: Copy JARs to temp location first
copy "%JACKSON_DIR%\jackson-databind-2.17.1.jar" temp_build\
copy "%JACKSON_DIR%\jackson-core-2.17.1.jar" temp_build\
copy "%JACKSON_DIR%\jackson-annotations-2.17.1.jar" temp_build\

:: Change to temp directory and extract using simple relative paths
cd /d temp_build

echo     Extracting jackson-databind...
"%JDK_PATH%\bin\jar.exe" xf jackson-databind-2.17.1.jar
if errorlevel 1 (
    echo     [WARN] Issue with jackson-databind
) else (
    echo     [OK] jackson-databind done
)

echo     Extracting jackson-core...
"%JDK_PATH%\bin\jar.exe" xf jackson-core-2.17.1.jar
if errorlevel 1 (
    echo     [WARN] Issue with jackson-core
) else (
    echo     [OK] jackson-core done
)

echo     Extracting jackson-annotations...
"%JDK_PATH%\bin\jar.exe" xf jackson-annotations-2.17.1.jar
if errorlevel 1 (
    echo     [WARN] Issue with jackson-annotations
) else (
    echo     [OK] jackson-annotations done
)

:: Clean up the copied JAR files (no longer needed)
del jackson-databind-2.17.1.jar jackson-core-2.17.1.jar jackson-annotations-2.17.1.jar

:: [CRITICAL] Jackson's META-INF would overwrite our MANIFEST.MF
:: Delete it so we can create our own clean one
if exist "META-INF" rmdir /s /q "META-INF"
mkdir "META-INF"
echo Manifest-Version: 1.0> "META-INF\MANIFEST.MF"
echo Main-Class: Game>> "META-INF\MANIFEST.MF"
echo   [OK] Clean MANIFEST.MF created

:: Return to project root
cd /d ..

echo   [OK] All dependencies extracted

echo   [OK] All files prepared

:: Create the JAR file (with extracted dependencies, excluding personal data)
echo [6/7] Creating JAR file...
echo   Removing personal data files from package...
cd temp_build

for %%F in (%EXCLUDE_FILES%) do (
    del /s /q "%%F" >nul 2>&1
    if not errorlevel 1 echo     [EXCLUDED] %%F
)
if exist "data" (
    dir /b "data" >nul 2>&1
    if errorlevel 1 rmdir /q "data" >nul 2>&1
)

:: Recreate MANIFEST.MF to guarantee it exists (cd /d can be flaky)
echo     Recreating MANIFEST.MF...
if not exist "META-INF" mkdir "META-INF"
echo Manifest-Version: 1.0> "META-INF\MANIFEST.MF"
echo Main-Class: Game>> "META-INF\MANIFEST.MF"
echo     [OK] MANIFEST ready

echo     Running jar.exe...
"%JDK_PATH%\bin\jar.exe" cfm "..\%APP_NAME%_v%VERSION%.jar" "META-INF\MANIFEST.MF" .
set JAR_ERR=%ERRORLEVEL%
echo     jar.exe exit code: %JAR_ERR%

cd ..

if %JAR_ERR% neq 0 (
    echo   [ERROR] JAR creation failed - exit code: %JAR_ERR%
    pause
    exit /b 1
)

:: Get file size
for %%A in ("%APP_NAME%_v%VERSION%.jar") do set JAR_SIZE=%%~zA
set /a JAR_SIZE_MB=%JAR_SIZE% / 1048576

echo.
echo ==========================================
echo   JAR CREATED SUCCESSFULLY
echo ==========================================
echo   File: %APP_NAME%_v%VERSION%.jar
echo   Size: ~%JAR_SIZE_MB% MB

:: Clean up temp directory
rmdir /s /q "temp_build" >nul 2>&1
echo   [OK] Cleaned temporary files

:: Ask user what to do next
echo.
echo [7/7] What would you like to do?
echo.
echo   1. Test run the JAR now
echo   2. Generate portable app - recommended for distribution
echo   3. Both test and generate app
echo   4. Exit - just keep the JAR
echo.
set /p NEXT_ACTION="Enter choice (1/2/3/4, default 1): "

if "%NEXT_ACTION%"=="" set NEXT_ACTION=1

:: Option 1: Test run
if "%NEXT_ACTION%"=="1" goto :test_jar
if "%NEXT_ACTION%"=="3" goto :test_jar

:: Option 2: Just generate app
if "%NEXT_ACTION%"=="2" goto :gen_app

:: Option 4: Exit
if "%NEXT_ACTION%"=="4" goto :done

:test_jar
echo.
echo Testing JAR execution...
echo   Starting game with bundled JDK 21...
echo   (Game window should appear shortly)
echo.
start "" /wait "%JDK_PATH%\bin\java.exe" -jar "%APP_NAME%_v%VERSION%.jar"

echo.
echo   Game closed. Return to this window to continue.
pause

:: If user chose option 3, also generate app
if "%NEXT_ACTION%"=="3" goto :gen_app
if "%NEXT_ACTION%"=="1" goto :done

:gen_app
echo.
echo Generating portable application...
echo   This will bundle JRE so users need NOTHING installed
echo   Output folder: %OUTPUT_DIR%\%APP_NAME%\
echo   Please wait 2-5 minutes...
echo/

if exist "%OUTPUT_DIR%\%APP_NAME%" (
    echo   Removing old portable app...
    rmdir /s /q "%OUTPUT_DIR%\%APP_NAME%" >nul 2>&1
    echo   [OK] Old portable app removed
)

echo   [A] Creating slim JRE with jlink...
set RUNTIME_DIR=runtime_jre
if exist "%RUNTIME_DIR%" rmdir /s /q "%RUNTIME_DIR%"

"%JDK_PATH%\bin\jlink.exe" --add-modules java.desktop,java.logging --output "%RUNTIME_DIR%" --strip-debug --no-header-files --no-man-pages --compress=2

if errorlevel 1 (
    echo   [ERROR] jlink failed^^^
    echo   You can still use the JAR file directly.
    goto :done
)
echo   [OK] Slim JRE created at %RUNTIME_DIR%\

echo   [B] Preparing jpackage input...
set JPINPUT=jpackage_input
if exist "%JPINPUT%" rmdir /s /q "%JPINPUT%"
mkdir "%JPINPUT%"
copy "%APP_NAME%_v%VERSION%.jar" "%JPINPUT%\" >nul
mkdir "%JPINPUT%\config"
if exist "config\probability.txt" (
    copy "config\probability.txt" "%JPINPUT%\config\" >nul
    echo   [OK] config/probability.txt copied
) else (
    echo   [WARN] config/probability.txt not found
)

echo   [C] Running jpackage - this may take 2-5 minutes...
set JPACKAGE_RESULT=0
"%JDK_PATH%\bin\jpackage.exe" --type app-image --name "%APP_NAME%" --app-version "%VERSION%" --input "%JPINPUT%" --main-jar "%APP_NAME%_v%VERSION%.jar" --main-class "%MAIN_CLASS%" --runtime-image "%RUNTIME_DIR%" --dest "%OUTPUT_DIR%" --win-console

set JPACKAGE_RESULT=%ERRORLEVEL%

if exist "%JPINPUT%" rmdir /s /q "%JPINPUT%"
if exist "%RUNTIME_DIR%" rmdir /s /q "%RUNTIME_DIR%"

if %JPACKAGE_RESULT% neq 0 (
    echo.
    echo   [WARNING] App generation failed - exit code: %JPACKAGE_RESULT%
    echo   You can still use the JAR file directly.
) else (
    if exist "%OUTPUT_DIR%\%APP_NAME%" (
        mkdir "%OUTPUT_DIR%\%APP_NAME%\config" 2>nul
        if exist "config\probability.txt" copy "config\probability.txt" "%OUTPUT_DIR%\%APP_NAME%\config\" >nul
        echo   [OK] config/ copied to app folder
    )
    echo.
    echo ==============================================
    echo   PORTABLE APP CREATED SUCCESSFULLY
    echo ==============================================
    echo   Location: %CD%\%OUTPUT_DIR%\%APP_NAME%\
    echo   To run: Double-click %APP_NAME%.exe inside that folder
    echo   To distribute: Zip the entire %APP_NAME% folder
    echo   Users need NO Java installation^^^
)

goto :done

:done
echo.
echo ============================================
echo   BUILD PROCESS COMPLETE!
echo ============================================
goto :end_script

:end_script
echo.
echo Output files:
echo   * JAR: %APP_NAME%_v%VERSION%.jar (~%JAR_SIZE_MB% MB)
if exist "%OUTPUT_DIR%\%APP_NAME%\%APP_NAME%.exe" (
    echo   * App: %OUTPUT_DIR%\%APP_NAME%\%APP_NAME%.exe
)
echo.
echo Quick commands:
echo   Test JAR: "%JDK_PATH%\bin\java.exe" -jar %APP_NAME%_v%VERSION%.jar
if exist "%OUTPUT_DIR%\%APP_NAME%\%APP_NAME%.exe" (
    echo   Run App: %OUTPUT_DIR%\%APP_NAME%\%APP_NAME%.exe
)
echo.

:: 清理临时文件
if exist "package_temp" rmdir /s /q "package_temp" >nul 2>&1


echo.
echo Next steps:
echo   1. Test JAR: "%JDK_PATH%\bin\java.exe" -jar %APP_NAME%_v%VERSION%.jar
if exist "%OUTPUT_DIR%\%APP_NAME%\%APP_NAME%.exe" (
    echo   2. Or test portable app: %OUTPUT_DIR%\%APP_NAME%\%APP_NAME%.exe
    echo   3. Distribute the entire %OUTPUT_DIR%\%APP_NAME%\ folder
)
echo.
pause
goto :eof

:show_help
echo.
echo Usage: build.bat [option]
echo.
echo Options:
echo   (none)    Full interactive build
echo   quick     JAR only, no questions
echo   help      Show this message
echo.
echo Examples:
echo   build.bat           Full build with options
echo   build.bat quick      Just create JAR fast
echo.
pause
goto :eof