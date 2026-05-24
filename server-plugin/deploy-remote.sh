#!/usr/bin/env bash
# Опционально: залить JAR на УДАЛЁННЫЙ Minecraft-сервер (не боты 4narek-1.12).
#
# Локальный Paper (Mac): достаточно ./build.sh → server/plugins/ → перезапуск ./start.sh
#
# Удалённый VPS:
#   ./deploy-remote.sh root@159.194.200.114
#   REMOTE=root@IP ./deploy-remote.sh
#   REMOTE_DIR=/home/user/mc/plugins ./deploy-remote.sh root@IP
#
set -euo pipefail
cd "$(dirname "$0")"

./build.sh

JAR=$(ls -1t build/libs/PveAuction-*.jar 2>/dev/null | head -1)
HOST_FILE="$(cd .. && pwd)/public-host.txt"
DEFAULT_REMOTE=""
if [[ -f "${HOST_FILE}" ]]; then
  IP=$(grep -v '^#' "${HOST_FILE}" | grep -v '^[[:space:]]*$' | head -1 | tr -d '\r')
  [[ -n "${IP}" ]] && DEFAULT_REMOTE="root@${IP}"
fi

REMOTE="${REMOTE:-${1:-${DEFAULT_REMOTE}}}"
REMOTE_DIR="${REMOTE_DIR:-/root/server/plugins}"

if [[ -z "${REMOTE}" ]]; then
  echo ""
  echo "Локальный сервер: JAR уже в server/plugins/ — перезапусти ./start.sh в папке server/"
  echo "Удалённый: укажи SSH, например: ./deploy-remote.sh root@ТВОЙ_IP_MINECRAFT"
  exit 0
fi

echo "JAR: ${JAR}"
echo "Куда: ${REMOTE}:${REMOTE_DIR}/"

scp -o StrictHostKeyChecking=accept-new "${JAR}" "${REMOTE}:${REMOTE_DIR}/"
ssh "${REMOTE}" "rm -f ${REMOTE_DIR}/PveAuction-0.1.0.jar 2>/dev/null; ls -la ${REMOTE_DIR}/PveAuction*.jar"
echo "Готово. Перезапусти Minecraft на ${REMOTE}."
