@echo off
echo Limpiando proyecto Sunday Android...

REM Detener procesos Gradle
echo Deteniendo procesos Gradle...
taskkill /f /im java.exe 2>nul

REM Esperar un poco
timeout /t 3 /nobreak > nul

REM Limpiar directorios
echo Limpiando directorios de build...
if exist "build" rmdir /s /q "build" 2>nul
if exist "app\build" rmdir /s /q "app\build" 2>nul
if exist ".gradle" rmdir /s /q ".gradle" 2>nul

REM Limpiar cache de Gradle del usuario
echo Limpiando cache de Gradle...
if exist "%USERPROFILE%\.gradle\caches" rmdir /s /q "%USERPROFILE%\.gradle\caches" 2>nul

echo.
echo ¡Limpieza completada!
echo Ahora puedes intentar compilar de nuevo.
echo.
pause
