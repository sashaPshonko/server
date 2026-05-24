#!/usr/bin/env bash
# Залить свежий PveAuction на VPS с Minecraft (см. 4narek-1.12/servers/502).
set -euo pipefail
cd "$(dirname "$0")"

./build.sh

CREDS="${1:-../4narek-1.12/servers/502}"
if [[ ! -f "${CREDS}" ]]; then
  echo "Нет файла ${CREDS} (host + пароль на двух строках)"
  exit 1
fi

HOST=$(sed -n '1p' "${CREDS}" | tr -d '\r')
PASS=$(sed -n '2p' "${CREDS}" | tr -d '\r')
JAR=$(ls -1 build/libs/PveAuction-*.jar | head -1)
REMOTE_DIR="${REMOTE_DIR:-/root/server/plugins}"

echo "JAR: ${JAR}"
echo "VPS: ${HOST}:${REMOTE_DIR}"

if ! command -v sshpass >/dev/null 2>&1; then
  echo "Установи sshpass: brew install hudochenkov/sshpass/sshpass"
  echo "Или вручную:"
  echo "  scp ${JAR} root@${HOST}:${REMOTE_DIR}/"
  echo "  ssh root@${HOST} 'rm -f ${REMOTE_DIR}/PveAuction-0.1.0.jar && systemctl restart minecraft || ./start.sh'"
  exit 0
fi

sshpass -p "${PASS}" scp -o StrictHostKeyChecking=no "${JAR}" "root@${HOST}:${REMOTE_DIR}/"
sshpass -p "${PASS}" ssh -o StrictHostKeyChecking=no "root@${HOST}" \
  "rm -f ${REMOTE_DIR}/PveAuction-0.1.0.jar && ls -la ${REMOTE_DIR}/PveAuction*.jar"
echo "Готово. Перезапусти Minecraft на VPS (systemctl / start.sh)."
