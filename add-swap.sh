#!/usr/bin/env bash
# Один раз на VPS с 1 GB RAM
set -euo pipefail
if [[ -f /swapfile ]]; then
  swapon --show
  exit 0
fi
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
echo "Swap включён:"
free -h
