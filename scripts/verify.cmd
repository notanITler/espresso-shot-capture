@echo off
setlocal EnableExtensions EnableDelayedExpansion

pushd "%~dp0.."
set "exitCode=!ERRORLEVEL!"
if not "!exitCode!"=="0" exit /b !exitCode!

set "REPO_ROOT=%CD%"
set "GRADLE_USER_HOME=C:\Users\david\.gradle"
set "CACHED_GRADLE=C:\Users\david\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat"

if exist "!CACHED_GRADLE!" (
    set "GRADLE_CMD=!CACHED_GRADLE!"
) else (
    set "GRADLE_CMD=!REPO_ROOT!\gradlew.bat"
)

echo Using Gradle command: !GRADLE_CMD!

echo Running unit tests...
call "!GRADLE_CMD!" test
set "exitCode=!ERRORLEVEL!"
if not "!exitCode!"=="0" (
    popd
    exit /b !exitCode!
)

echo Running assembleDebug...
call "!GRADLE_CMD!" assembleDebug
set "exitCode=!ERRORLEVEL!"
if not "!exitCode!"=="0" (
    popd
    exit /b !exitCode!
)

echo Verification passed.
popd
exit /b 0
