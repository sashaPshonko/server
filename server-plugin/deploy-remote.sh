#!/usr/bin/env bash
# Залить PveAuction на Minecraft VPS (159.194.200.114 из public-host.txt).
set -euo pipefail
cd "$(dirname "$0")"

./build.sh

JAR=$(ls -1t build/libs/PveAuction-*.jar 2>/dev/null | head -1)
JAR_NAME=$(basename "${JAR}")
HOST_FILE="$(cd .. && pwd)/public-host.txt"
DEFAULT_REMOTE=""
if [[ -f "${HOST_FILE}" ]]; then
  IP=$(grep -v '^#' "${HOST_FILE}" | grep -v '^[[:space:]]*$' | head -1 | tr -d '\r')
  [[ -n "${IP}" ]] && DEFAULT_REMOTE="root@${IP}"
fi

REMOTE="${REMOTE:-${1:-${DEFAULT_REMOTE}}}"
REMOTE_DIR="${REMOTE_DIR:-/root/server/plugins}"
SERVER_DIR="${REMOTE_DIR%/plugins}"

if [[ -z "${REMOTE}" ]]; then
  echo "Укажи: ./deploy-remote.sh root@159.194.200.114"
  exit 1
fi

echo "JAR: ${JAR} (${JAR_NAME})"
echo "VPS: ${REMOTE}:${REMOTE_DIR}/"
echo ""

ssh "${REMOTE}" bash -s -- "${REMOTE_DIR}" "${SERVER_DIR}" "${JAR_NAME}" <<'REMOTE_SCRIPT'
set -euo pipefail
PLUGINS="$1"
SERVER="$2"
JAR_NAME="$3"

rm -rf "${PLUGINS}/.paper-remapped"
rm -f "${PLUGINS}"/PveAuction.jar "${PLUGINS}"/PveAuction-*.jar 2>/dev/null || true
REMOTE_SCRIPT

scp -o StrictHostKeyChecking=accept-new "${JAR}" "${REMOTE}:${REMOTE_DIR}/"

FAWE_JAR=$(ls -1t "$(cd .. && pwd)/plugins"/FastAsyncWorldEdit-Paper-*.jar 2>/dev/null | head -1)
if [[ -n "${FAWE_JAR}" && -f "${FAWE_JAR}" ]]; then
  echo "FAWE: $(basename "${FAWE_JAR}")"
  scp -o StrictHostKeyChecking=accept-new "${FAWE_JAR}" "${REMOTE}:${REMOTE_DIR}/"
fi

ssh "${REMOTE}" bash -s -- "${REMOTE_DIR}" "${SERVER_DIR}" "${JAR_NAME}" <<'REMOTE_SCRIPT'
set -euo pipefail
PLUGINS="$1"
SERVER="$2"
JAR_NAME="$3"

echo "=== JAR на VPS ==="
ls -la "${PLUGINS}"/PveAuction*.jar

echo "=== Проверка сборки внутри JAR ==="
if command -v unzip >/dev/null 2>&1; then
  unzip -p "${PLUGINS}/${JAR_NAME}" dev/narek/pveauction/gui/shop/ShopSellMenu.class 2>/dev/null \
    | strings 2>/dev/null | grep -E '0\.1\.[0-9]' | head -3 || true
fi

echo ""
echo "=== Перезапуск (если сервер в screen/tmux) ==="
if [[ -d "${SERVER}" ]]; then
  cd "${SERVER}"
  if screen -ls 2>/dev/null | grep -qi minecraft; then
    echo "Найден screen — зайди: screen -r minecraft → stop → ./start.sh"
  fi
fi
echo "ОБЯЗАТЕЛЬНО: stop в консоли сервера, потом ./start.sh (не /reload)"
REMOTE_SCRIPT

echo ""
echo "Готово. В игре: /shop — в чате и «Скупка [версия]» должна быть ${JAR_NAME#PveAuction-}"
echo "       (версия без .jar, например 0.1.75). Потом stop → ./start.sh на VPS."
