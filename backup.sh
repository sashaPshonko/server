#!/usr/bin/env bash
# Бекап Minecraft (Paper) на VPS. Запуск: cd ~/server && bash backup.sh
set -euo pipefail
cd "$(dirname "$0")"

SERVER_DIR="$(pwd)"
BACKUP_DIR="${BACKUP_DIR:-${SERVER_DIR}/backups}"
KEEP="${KEEP:-7}"
TS="$(date +%Y%m%d-%H%M%S)"
ARCHIVE="${BACKUP_DIR}/minecraft-${TS}.tar.gz"
LATEST_LINK="${BACKUP_DIR}/latest.tar.gz"

mkdir -p "${BACKUP_DIR}"

if pgrep -f 'paper\.jar' >/dev/null; then
    echo "==> Сервер запущен — save-all"
    if screen -ls 2>/dev/null | grep -qE '[.]minecraft[[:space:]]'; then
        screen -S minecraft -p 0 -X stuff "save-all flush$(printf '\r')"
        sleep "${SAVE_WAIT_SEC:-8}"
    else
        echo "    (нет screen minecraft — бекап на горячую, возможны мелкие рассинхроны)"
        sleep 2
    fi
else
    echo "==> Сервер остановлен — бекап холодный"
fi

TMP_LIST="$(mktemp)"
cleanup() { rm -f "${TMP_LIST}"; }
trap cleanup EXIT

add_path() {
    local p="$1"
    if [[ -e "${SERVER_DIR}/${p}" ]]; then
        printf '%s\n' "${p}" >> "${TMP_LIST}"
    fi
}

# Миры
for w in world rtp world_nether world_the_end; do
    add_path "${w}"
done

# Конфиги сервера
for f in server.properties spigot.yml bukkit.yml paper.yml eula.txt \
    whitelist.json ops.json banned-players.json banned-ips.json; do
    add_path "${f}"
done
add_path config

# Плагины: данные и конфиги (без .paper-remapped и тяжёлых кэшей)
for plug in PveAuction Essentials Vault Citizens FastAsyncWorldEdit WorldEdit; do
    add_path "plugins/${plug}"
done

# Остальные jar — только список имён (не сами jar)
ls -1 plugins/*.jar 2>/dev/null | sed "s|^|plugins/|" >> "${TMP_LIST}" || true

if [[ ! -s "${TMP_LIST}" ]]; then
    echo "❌ Нечего бэкапить — нет world/ и plugins/"
    exit 1
fi

echo "==> Архив: ${ARCHIVE}"
tar -czf "${ARCHIVE}" \
    --exclude='plugins/.paper-remapped' \
    --exclude='*.log' \
    --exclude='session.lock' \
    -C "${SERVER_DIR}" \
    -T "${TMP_LIST}"

ln -sfn "$(basename "${ARCHIVE}")" "${LATEST_LINK}"
SIZE="$(du -h "${ARCHIVE}" | awk '{print $1}')"
echo "✅ Готово: ${ARCHIVE} (${SIZE})"

# Ротация: оставить последние KEEP архивов
mapfile -t OLD < <(ls -1t "${BACKUP_DIR}"/minecraft-*.tar.gz 2>/dev/null || true)
if ((${#OLD[@]} > KEEP)); then
    for ((i = KEEP; i < ${#OLD[@]}; i++)); do
        rm -f "${OLD[$i]}"
        echo "   удалён старый: $(basename "${OLD[$i]}")"
    done
fi

echo "   хранится бекапов: $(ls -1 "${BACKUP_DIR}"/minecraft-*.tar.gz 2>/dev/null | wc -l | tr -d ' ') (KEEP=${KEEP})"
