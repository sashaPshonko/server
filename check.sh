#!/usr/bin/env bash
cd "$(dirname "$0")"
echo "=== Память ==="
free -h
echo ""
echo "=== Java / сервер ==="
if pgrep -f 'paper.jar' >/dev/null; then
  echo "paper.jar: ЗАПУЩЕН"
  pgrep -af 'paper.jar'
else
  echo "paper.jar: НЕ ЗАПУЩЕН"
fi
echo ""
echo "=== Порт 25565 ==="
ss -tlnp | grep 25565 || echo "порт не слушается"
echo ""
echo "=== Плагины ==="
ls -1 plugins/*.jar 2>/dev/null || true
echo ""
echo "=== Мир ==="
if [[ -f world/level.dat ]]; then
  echo "world/level.dat — OK"
else
  echo "НЕТ world/level.dat — положи карту в ~/server/world/"
fi
echo ""
echo "=== Последние строки лога ==="
tail -5 logs/latest.log 2>/dev/null || echo "нет лога"
