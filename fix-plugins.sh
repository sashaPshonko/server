#!/usr/bin/env bash
# На VPS: cd ~/server && bash fix-plugins.sh
set -euo pipefail
cd "$(dirname "$0")"
PLUGINS="${PWD}/plugins"

echo "==> Останавли сервер (если запущен — в консоли Minecraft: stop)"
echo "==> Чистим дубликаты PveAuction и кэш Paper"
rm -f "${PLUGINS}/PveAuction.jar"
rm -f "${PLUGINS}/PveAuction-0.1.1"*.jar
rm -f "${PLUGINS}/PveAuction-0.1.16.jar"
rm -rf "${PLUGINS}/.paper-remapped"

echo "==> Citizens не нужен (торгаш — житель). Удаляем?"
rm -f "${PLUGINS}/Citizens.jar" "${PLUGINS}/Citizens-"*.jar 2>/dev/null || true

echo "==> Осталось:"
ls -la "${PLUGINS}"/PveAuction*.jar 2>/dev/null || echo "  НЕТ JAR — залей PveAuction-0.1.21.jar с Mac"

if ! ls "${PLUGINS}"/PveAuction-*.jar >/dev/null 2>&1; then
  echo ""
  echo "С Mac:"
  echo "  scp .../server/plugins/PveAuction-0.1.21.jar root@IP:${PLUGINS}/"
  exit 1
fi

COUNT=$(ls -1 "${PLUGINS}"/PveAuction-*.jar 2>/dev/null | wc -l | tr -d ' ')
if [[ "${COUNT}" != "1" ]]; then
  echo "ВНИМАНИЕ: должно быть ровно 1 jar, сейчас: ${COUNT}"
  ls -la "${PLUGINS}"/PveAuction-*.jar
  exit 1
fi

echo "OK. Запуск: ./start.sh"
