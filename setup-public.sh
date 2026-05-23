#!/usr/bin/env bash
# Подсказка: как открыть сервер в интернет
set -euo pipefail
cd "$(dirname "$0")"

echo "════════════════════════════════════════"
echo "  Публичный доступ к Minecraft-серверу"
echo "════════════════════════════════════════"
echo ""

if lsof -nP -iTCP:25565 -sTCP:LISTEN 2>/dev/null | grep -q java; then
  echo "✓ Сервер слушает порт 25565"
else
  echo "✗ Сервер не запущен — сначала: ./start.sh"
fi
echo ""

LAN=$(ifconfig 2>/dev/null | awk '/inet / && $2 ~ /^192\.168\./ {print $2; exit}')
echo "Локально (только Wi‑Fi):  ${LAN:-?}"
echo "В интернет (друзья):       адрес от playit.gg (см. ниже)"
echo ""
echo "── playit.gg (проще всего) ──"
echo "  1. https://playit.gg/download  → macOS"
echo "  2. Туннель: Minecraft Java, порт 25565"
echo "  3. Друзья подключаются к выданному адресу (*.playit.gg / *.joinmc.link)"
echo ""
echo "Подробно: cat PUBLIC.md"
echo ""
if command -v open >/dev/null 2>&1; then
  read -r -p "Открыть playit.gg в браузере? [y/N] " ans
  if [[ "${ans,,}" == "y" || "${ans,,}" == "yes" ]]; then
    open "https://playit.gg/download"
  fi
fi
