@echo off
echo ========================================
echo    SUNDAY ANDROID - BUILD SCRIPT
echo ========================================
echo.

echo 1. Limpiando proyecto...
call clean_project.bat

echo.
echo 2. Esperando que se liberen los archivos...
timeout /t 5 /nobreak > nul

echo.
echo 3. Iniciando build con configuraciones optimizadas...
echo.

REM Build con configuraciones optimizadas para Java 21
gradlew.bat clean assembleDebug --no-daemon --parallel --build-cache --configuration-cache-problems=warn

echo.
if %ERRORLEVEL% EQU 0 (
    echo ========================================
    echo    ¡BUILD EXITOSO! 🎉
    echo ========================================
    echo La APK se encuentra en: app\build\outputs\apk\debug\
) else (
    echo ========================================
    echo    BUILD FALLÓ ❌
    echo ========================================
    echo Revisa los errores arriba y ejecuta de nuevo.
)

echo.
pause
