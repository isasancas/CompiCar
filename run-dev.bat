@echo off
setlocal

:: 1. Comprobar si el motor de Docker está activo; si no, iniciar Docker Desktop y esperar
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo Iniciando Docker Desktop...
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    
    :wait_docker
    echo Esperando a que el motor de Docker responda...
    timeout /t 4 /nobreak >nul
    docker info >nul 2>&1
    if %errorlevel% neq 0 goto wait_docker
    echo Docker esta listo.
)

:: 2. Iniciar Mailpit
docker start mailpit 2>nul || docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit

:: 3. Levantar el resto del entorno
start "Backend" cmd /k "cd /d %~dp0backend && mvn spring-boot:run"
start "Frontend" cmd /k "cd /d %~dp0frontend && npm run dev"
start "Stripe CLI" cmd /k "stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe"

endlocal