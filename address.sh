#!/usr/bin/env bash
# Единый адрес подключения (как IP на VPS)
set -euo pipefail
cd "$(dirname "$0")"

HOST_FILE="public-host.txt"

echo "════════════════════════════════════════"
echo "  Адрес для друзей (как на VPS)"
echo "════════════════════════════════════════"
echo ""

if [[ -f "${HOST_FILE}" ]]; then
  ADDR=$(grep -v '^#' "${HOST_FILE}" | grep -v '^[[:space:]]*$' | head -1 | tr -d '[:space:]')
  if [[ -n "${ADDR}" && "${ADDR}" != *"REPLACE"* && "${ADDR}" != *"your-server"* && "${ADDR}" != "100.64.0.1" ]]; then
    echo "  ${ADDR}"
    echo ""
    echo "Minecraft: Прямое подключение → ${ADDR}"
    echo "Версия: Java 1.21.4"
  else
    echo "  ⚠ Заполни ${HOST_FILE} (см. public-host.example.txt)"
  fi
else
  echo "  ⚠ Нет ${HOST_FILE}"
  echo "    cp public-host.example.txt public-host.txt"
  echo "    и впиши адрес playit или IP VPS"
fi

echo ""
if lsof -nP -iTCP:25565 -sTCP:LISTEN 2>/dev/null | grep -q java; then
  echo "Сервер: запущен (порт 25565)"
else
  echo "Сервер: НЕ запущен → ./start.sh"
fi

LAN=$(ifconfig 2>/dev/null | awk '/inet / && $2 ~ /^192\.168\./ {print $2; exit}')
if [[ -n "${LAN}" ]]; then
  echo "Только твой Mac в Wi‑Fi: ${LAN} (временно, на VPS не нужно)"
fi
echo ""
