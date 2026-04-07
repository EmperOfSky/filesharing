#!/usr/bin/env bash
set -euo pipefail

# Ubuntu/Debian UFW hardening for production deployment.
# Run as root: sudo bash scripts/firewall-rules.sh

ufw --force reset
ufw default deny incoming
ufw default allow outgoing

# Allow public access only to edge ports.
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp

# Deny internal service ports.
ufw deny 8080/tcp
ufw deny 3306/tcp
ufw deny 9000/tcp
ufw deny 9001/tcp

ufw --force enable
ufw status verbose
