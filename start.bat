@echo off
echo ============================================
echo   ReDiscoverU v34 — Backend Startup
echo ============================================
echo.

cd /d "%~dp0backend"

:: ── EDIT THESE VALUES ────────────────────────────────────────
set DB_USER=root
set DB_PASS=root
set MAIL_USERNAME=yourname@gmail.com
set MAIL_PASSWORD=your_16char_app_password
set ADMIN_EMAIL=admin@rediscoveru.life
set ADMIN_PASSWORD=Admin@2024
set ADMIN_NAME=Jayashankar Lingaiah
:: ─────────────────────────────────────────────────────────────

set JWT_SECRET=ReDiscoverU-SuperSecure-JWT-Key-2026
set PORT=8080
set APP_BASE_URL=http://localhost:5500

echo Starting backend on http://localhost:8080 ...
echo.
mvn spring-boot:run
pause
