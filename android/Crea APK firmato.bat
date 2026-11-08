@echo off
rem Doppio clic: crea l'APK di Imash firmato con la chiave vera (chiede la password).
rem L'APK finisce in android\app\build\outputs\apk\release\app-release.apk
rem e una copia va sul Desktop come Imash.apk.
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0firma-release.ps1"
echo.
pause
