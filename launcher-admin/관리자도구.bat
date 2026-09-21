@echo off
chcp 65001 >nul
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0관리자도구.ps1" %*
exit /b %ERRORLEVEL%
