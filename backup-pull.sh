#!/usr/bin/env bash
# С Mac: бекап на VPS + скачать архив локально.
#   ./backup-pull.sh
#   ./backup-pull.sh root@159.194.200.114
set -euo pipefail
cd "$(dirname "$0")"

HOST_FILE="${HOST_FILE:-public-host.txt}"
DEFAULT_REMOTE=""
if [[ -f "${HOST_FILE}" ]]; then
    IP="$(grep -v '^#' "${HOST_FILE}" | grep -v '^[[:space:]]*$' | head -1 | tr -d '\r')"
    [[ -n "${IP}" ]] && DEFAULT_REMOTE="root@${IP}"
fi
REMOTE="${1:-${REMOTE:-${DEFAULT_REMOTE}}}"
if [[ -z "${REMOTE}" ]]; then
    echo "Укажи: ./backup-pull.sh root@159.194.200.114"
    exit 1
fi

LOCAL_DIR="${LOCAL_DIR:-$(cd .. && pwd)/minecraft-backups}"
mkdir -p "${LOCAL_DIR}"

echo "==> Бекап на ${REMOTE}…"
ssh -o StrictHostKeyChecking=accept-new "${REMOTE}" 'cd ~/server && bash backup.sh'

echo "==> Скачиваю latest.tar.gz → ${LOCAL_DIR}/"
scp -o StrictHostKeyChecking=accept-new \
    "${REMOTE}:~/server/backups/latest.tar.gz" \
    "${LOCAL_DIR}/minecraft-$(date +%Y%m%d-%H%M%S).tar.gz"

echo "✅ Локально: ${LOCAL_DIR}/"
ls -lh "${LOCAL_DIR}" | tail -5
