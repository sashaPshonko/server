#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

echo "1) Запусти сервер (если ещё не):  ./start.sh"
echo "2) Скачай playit: https://playit.gg/download"
echo "3) Туннель: Minecraft Java → localhost:25565"
echo ""
echo "Подробно: cat PLAYIT-MAC.md"
echo ""

if lsof -nP -iTCP:25565 -sTCP:LISTEN 2>/dev/null | grep -q java; then
  echo "✓ Minecraft-сервер на порту 25565 запущен"
else
  echo "✗ Сначала ./start.sh — playit проксирует на 25565"
fi

if command -v open >/dev/null 2>&1; then
  open "https://playit.gg/download"
  open "https://playit.gg/login" 2>/dev/null || true
fi
