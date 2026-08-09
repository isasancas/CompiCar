@echo off
setlocal

start "Backend" cmd /k "cd /d %~dp0backend && mvn spring-boot:run"
start "Frontend" cmd /k "cd /d %~dp0frontend && npm run dev"
start "Stripe CLI" cmd /k "stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe"

endlocal