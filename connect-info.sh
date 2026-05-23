#!/usr/bin/env bash
# Показать IP и порт для подключения с другого ПК в той же сети
set -euo pipefail
cd "$(dirname "$0")"

PORT=25565
echo "=== Подключение к серверу ==="
echo ""

if lsof -nP -iTCP:"${PORT}" -sTCP:LISTEN 2>/dev/null | grep -q java; then
  echo "Сервер: ЗАПУЩЕН (порт ${PORT})"
else
  echo "Сервер: НЕ ЗАПУЩЕН — сначала ./start.sh"
fi
echo ""

FOUND=0
while IFS= read -r line; do
  ip="${line%% *}"
  echo "  → ${ip}"
  FOUND=1
done < <(ifconfig 2>/dev/null | awk '/inet / && $2 != "127.0.0.1" {print $2}' | grep -E '^(192\.168\.|10\.)')

if [[ "${FOUND}" -eq 0 ]]; then
  echo "  (не нашёл LAN IP — проверь Wi‑Fi)"
fi

LAN_IP=$(ifconfig 2>/dev/null | awk '/inet / && $2 ~ /^192\.168\./ {print $2; exit}')
if [[ -n "${LAN_IP}" ]]; then
  echo "Главный адрес для друга: ${LAN_IP}"
  echo "  (в Minecraft: Прямое подключение → ${LAN_IP})"
fi
echo ""
echo "Версия на втором ПК: Java 1.21.4 (не Bedrock, не 1.20)"
echo ""
echo "Проверка СО ВТОРОГО ПК (Windows — cmd, Mac — Terminal):"
echo "  ping ${LAN_IP:-192.168.0.85}"
echo "Если ping не идёт — роутер режет связь (см. ниже)."
echo ""
echo "Пока друг подключается — в logs/latest.log должна быть строка"
echo "  logged in .../192.168.0.XX:..."
echo "Если в логе только 127.0.0.1 — пакеты не дошли до Mac."
echo ""
echo "Частые причины на одном Wi‑Fi:"
echo "  • Ввели localhost на втором ПК (надо IP Mac: ${LAN_IP:-192.168.0.85})"
echo "  • Гостевая сеть / «изоляция клиентов» в роутере (192.168.0.1 → Wi‑Fi → выкл.)"
echo "  • VPN на втором ПК"
echo "  • Другая подсеть (у друга IP не 192.168.0.XXX)"
echo ""
