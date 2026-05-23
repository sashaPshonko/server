#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

echo "════════════════════════════════════════"
echo "  Как дать друзьям адрес (бесплатно)"
echo "════════════════════════════════════════"
echo ""
echo "  1) Tailscale — проще всего, Mac ↔ Mac"
echo "     cat TAILSCALE.md"
echo ""
echo "  2) Проброс порта на роутере — как VPS"
echo "     cat PORT-FORWARD.md"
echo ""
echo "  playit — не обязателен (есть платные фичи)."
echo ""

if command -v tailscale >/dev/null 2>&1; then
  TS=$(tailscale ip -4 2>/dev/null || true)
  if [[ -n "${TS}" ]]; then
    echo "Tailscale IP (можно в public-host.txt): ${TS}"
  fi
fi

LAN=$(ifconfig 2>/dev/null | awk '/inet / && $2 ~ /^192\.168\./ {print $2; exit}')
[[ -n "${LAN}" ]] && echo "Локальная Wi‑Fi (только дома): ${LAN}"

EXT=$(curl -4 -s --max-time 3 ifconfig.me 2>/dev/null || true)
[[ -n "${EXT}" ]] && echo "Внешний IP (если настроен проброс порта): ${EXT}"

echo ""
./address.sh 2>/dev/null || true
