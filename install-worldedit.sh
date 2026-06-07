#!/usr/bin/env bash
# WorldEdit (FAWE) только на сервер — игрокам ничего ставить не нужно.
# Права: OP или pveauction.admin / pveauction.worldedit (см. PveAuction plugin.yml).
set -euo pipefail
cd "$(dirname "$0")"
PLUGINS="${PWD}/plugins"
JAR="${PLUGINS}/FastAsyncWorldEdit-Paper-2.15.1.jar"
URL="https://cdn.modrinth.com/data/z4HZZnLr/versions/zAlVhTdU/FastAsyncWorldEdit-Paper-2.15.1.jar"

mkdir -p "${PLUGINS}"

echo "==> FastAsyncWorldEdit для Paper 1.21.4"
if [[ -f "${JAR}" ]] && unzip -t "${JAR}" >/dev/null 2>&1; then
  echo "    Уже есть: $(basename "${JAR}")"
else
  echo "    Качаем ${URL}"
  curl -fL#o "${JAR}.tmp" "${URL}"
  if ! unzip -t "${JAR}.tmp" >/dev/null 2>&1; then
    echo "Ошибка: скачался битый jar"
    rm -f "${JAR}.tmp"
    exit 1
  fi
  mv "${JAR}.tmp" "${JAR}"
  echo "    OK: $(basename "${JAR}")"
fi

# Старые WorldEdit jar — убрать дубликаты
rm -f "${PLUGINS}/worldedit-bukkit-"*.jar "${PLUGINS}/WorldEdit.jar" 2>/dev/null || true

echo ""
echo "Готово. Перезапуск сервера: stop → ./start.sh"
echo "Доступ: OP или право pveauction.worldedit (у pveauction.admin оно уже есть)."
echo "Приваты: /wand и /claim — по-прежнему PveAuction; //wand — WorldEdit."
