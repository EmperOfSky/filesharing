# Windows Defender Firewall hardening for production deployment
# Run this script in an elevated PowerShell session.

$ErrorActionPreference = "Stop"

# Remove old custom rules (safe idempotency)
Get-NetFirewallRule -DisplayName "FileSharing-*" -ErrorAction SilentlyContinue | Remove-NetFirewallRule -ErrorAction SilentlyContinue

# Allow inbound HTTP/HTTPS
New-NetFirewallRule -DisplayName "FileSharing-Allow-HTTP" -Direction Inbound -Protocol TCP -LocalPort 80 -Action Allow
New-NetFirewallRule -DisplayName "FileSharing-Allow-HTTPS" -Direction Inbound -Protocol TCP -LocalPort 443 -Action Allow

# Optional: allow SSH (if OpenSSH is enabled)
New-NetFirewallRule -DisplayName "FileSharing-Allow-SSH" -Direction Inbound -Protocol TCP -LocalPort 22 -Action Allow

# Block backend/internal ports from public network
New-NetFirewallRule -DisplayName "FileSharing-Block-Backend-8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Block
New-NetFirewallRule -DisplayName "FileSharing-Block-MySQL-3306" -Direction Inbound -Protocol TCP -LocalPort 3306 -Action Block
New-NetFirewallRule -DisplayName "FileSharing-Block-MinIO-9000" -Direction Inbound -Protocol TCP -LocalPort 9000 -Action Block
New-NetFirewallRule -DisplayName "FileSharing-Block-MinIO-Console-9001" -Direction Inbound -Protocol TCP -LocalPort 9001 -Action Block

Write-Host "Firewall rules applied successfully." -ForegroundColor Green
