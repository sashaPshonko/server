#!/usr/bin/env bash
# Копирует maps/spawn → world/ (карта из git, без мусора от игры)
set -euo pipefail
cd "$(dirname "$0")"

SRC="maps/spawn"
DST="world"

if [[ ! -f "${SRC}/level.dat" ]]; then
  echo "Нет ${SRC}/level.dat"
  echo "Положи карту в maps/spawn/ (level.dat + region/)"
  exit 1
fi

if pgrep -f paper.jar >/dev/null; then
  echo "Сначала останови сервер: stop"
  exit 1
fi

rm -rf "${DST}"
mkdir -p "${DST}"
cp -a "${SRC}/." "${DST}/"
rm -f "${DST}/session.lock"
echo "Готово: ${SRC} → ${DST}/"
echo "Запуск: ./start.sh"
