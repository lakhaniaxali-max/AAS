@echo off
title Accurate Accounts - Mobile Server & Cloud Tunnel
echo ========================================================
echo   Starting Accurate Accounts Server & Mobile Tunnel
echo ========================================================

echo.
echo [1/2] Starting Node.js Backend Server on port 3000...
start "AAS Backend Server" cmd /k "cd Server && node index.js"

timeout /t 2 /nobreak >nul

echo [2/2] Starting Cloudflare Public Mobile Tunnel...
echo Any mobile phone can connect securely via the public HTTPS URL.
npx cloudflared tunnel --url http://localhost:3000

pause
