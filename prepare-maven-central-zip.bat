@echo off
SETLOCAL

:: =============================
:: User Configuration
:: =============================
set GROUP_ID=com.hibuka.soda
set ARTIFACT_ID=soda-core

:: =============================
:: Get project root directory
:: =============================
cd /d "%~dp0%"
set PROJECT_ROOT=%cd%
set WORK_DIR=%PROJECT_ROOT%\.zip-workspace
set MODULE_PATH=%WORK_DIR%\com\hibuka\soda\%ARTIFACT_ID%\1.0.0
set ZIP_FILE=%ARTIFACT_ID%-1.0.0-upload.zip
set ZIP_FULL_PATH=%PROJECT_ROOT%\%ZIP_FILE%

:: =============================
:: Extract version from pom.xml
:: =============================
for /f "tokens=2 delims=>" %%a in ('findstr "<version>" "%PROJECT_ROOT%\pom.xml" ^| findstr /v "<?xml"') do (
    for /f "tokens=1 delims=<" %%b in ("%%a") do set VERSION=%%b
)
echo [INFO] Extracted version: %VERSION%

:: =============================
:: Clean up old data
:: =============================
if exist "%WORK_DIR%" rd /s /q "%WORK_DIR%"

:: 删除旧的 ZIP 文件，增加等待和重试，确保文件被释放
set RETRY=0
:DELZIP
if exist "%ZIP_FULL_PATH%" (
    del /q "%ZIP_FULL_PATH%"
    timeout /t 1 >nul
    set /a RETRY+=1
    if %RETRY% lss 5 goto DELZIP
)

mkdir "%MODULE_PATH%"

:: =============================
:: Check target directory exists
:: =============================
if not exist "target" (
    echo [ERROR] target/ directory does not exist. Please run 'mvn clean package source:jar javadoc:jar' first.
    exit /b 1
)

:: =============================
:: Create .pom file if not exists
:: =============================
set POM_TARGET=target\%ARTIFACT_ID%-%VERSION%.pom
if not exist "%POM_TARGET%" (
    echo [INFO] No .pom file found, generating from pom.xml...
    copy pom.xml "%POM_TARGET%" >nul
)

:: =============================
:: Copy build artifacts to target directory
:: =============================
copy target\*.jar "%MODULE_PATH%" >nul
copy target\*-sources.jar "%MODULE_PATH%" >nul
copy target\*-javadoc.jar "%MODULE_PATH%" >nul
copy target\*.pom "%MODULE_PATH%" >nul

:: =============================
:: Sign all artifacts (requires GPG4Win installed and in PATH)
:: =============================
cd /d "%MODULE_PATH%"
for %%F in (*.jar *.pom) do (
    if exist "%%F" (
        echo [INFO] Signing %%F...
        gpg --armor --detach-sign "%%F"
        if errorlevel 1 (
            echo [ERROR] GPG signing failed. Please make sure GPG is configured properly with a key.
            exit /b 1
        )
    )
)

:: =============================
:: Generate checksum files (MD5 / SHA1)
:: =============================
for %%F in (*.jar *.pom) do (
    if exist "%%F" (
        echo [INFO] Generating checksums for %%F...
        certutil -hashfile "%%F" MD5 | findstr /v "hash" > "%%F.md5"
        certutil -hashfile "%%F" SHA1 | findstr /v "hash" > "%%F.sha1"
    )
)

:: =============================
:: Return to project root and create ZIP
:: =============================
cd /d "%PROJECT_ROOT%"
echo [INFO] Packaging ZIP file...

:: Use PowerShell to compress only the 'com' directory inside .zip-workspace
powershell.exe -Command "Compress-Archive -Path '%WORK_DIR%\com' -DestinationPath '%ZIP_FULL_PATH%' -Force"

:: =============================
:: Verify ZIP file exists and list content
:: =============================
if not exist "%ZIP_FULL_PATH%" (
    echo [ERROR] ZIP file was not generated. Please check path or permissions.
    exit /b 1
)

echo [INFO] ZIP file generated at: %ZIP_FULL_PATH%
echo [INFO] ZIP contents:
:: 解压并列出内容
powershell.exe -Command "Expand-Archive -Path '%ZIP_FULL_PATH%' -DestinationPath '%TEMP%\zipcontent' -Force"
dir "%TEMP%\zipcontent"

:: =============================
:: Optional cleanup of work directory
:: =============================
rd /s /q "%WORK_DIR%"

echo.
echo [SUCCESS] ZIP preparation completed! You can now upload to Maven Central.

ENDLOCAL