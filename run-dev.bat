@echo off
setlocal

start "Backend" cmd /k "cd /d %~dp0backend && mvn spring-boot:run"
start "Frontend" cmd /k "cd /d %~dp0frontend && npm run dev"

endlocal