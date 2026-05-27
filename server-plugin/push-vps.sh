#!/usr/bin/env bash
# Сборка PveAuction и заливка на VPS. Перезапуск — вручную в консоли Minecraft.
set -euo pipefail
cd "$(dirname "$0")"

HOST="${1:-}"
if [[ -z "${HOST}" ]]; then
  HOST_FILE="$(cd .. && pwd)/public-host.txt"
  if [[ -f "${HOST_FILE}" ]]; then
    HOST=$(grep -v '^#' "${HOST_FILE}" | grep -v '^[[:space:]]*$' | head -1 | tr -d '\r')
  fi
fi
if [[ -z "${HOST}" ]]; then
  echo "Использование: ./push-vps.sh root@159.194.200.114"
  exit 1
fi

REMOTE="${HOST#*@}"
REMOTE_USER="${HOST%%@*}"
[[ "${REMOTE_USER}" == "${HOST}" ]] && REMOTE_USER=root && HOST="${REMOTE_USER}@${REMOTE}"

PLUGINS_DIR="${PLUGINS_DIR:-/root/server/plugins}"

echo "=== Сборка ==="
./build.sh

JAR=$(ls -1t build/libs/PveAuction-*.jar | head -1)
JAR_NAME=$(basename "${JAR}")
VER=$(unzip -p "${JAR}" plugin.yml | awk '/^version:/ {print $2}')

echo ""
echo "=== Заливка ${JAR_NAME} (version ${VER}) ==="
scp -o StrictHostKeyChecking=accept-new "${JAR}" "${HOST}:${PLUGINS_DIR}/${JAR_NAME}"

echo ""
echo "=== На VPS выполни (скопируй блок) ==="
cat <<EOF

ssh ${HOST}
cd ${PLUGINS_DIR}
ls -la PveAuction*.jar
# Удалить только СТАРЫЕ версии (не трогай ${JAR_NAME}):
for f in PveAuction-*.jar; do
  [[ "\$f" == "${JAR_NAME}" ]] && continue
  rm -f "\$f"
done
rm -rf .paper-remapped
ls -la PveAuction*.jar

# Найти сервер:
find /root -maxdepth 5 -name 'paper.jar' 2>/dev/null
screen -ls

# Перезапуск: зайди в screen с Minecraft, введи stop, затем из папки с paper.jar:
#   cd /root/server   # или путь из find
#   ./start.sh
# Не используй /reload

EOF

echo "В логе после рестарта: PveAuction v${VER} — скупка, раскладка ${VER}"
echo "В игре: /shop → «сборка ${VER}»"
