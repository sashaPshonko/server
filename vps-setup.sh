#!/usr/bin/env bash
# Запуск на VPS из папки сервера:  cd ~/server && bash vps-setup.sh
# Без nano: чистит старые jar, качает Citizens, дописывает trader в config.
set -euo pipefail

SERVER_DIR="${SERVER_DIR:-$(cd "$(dirname "$0")" && pwd)}"
PLUGINS="${SERVER_DIR}/plugins"
CFG="${PLUGINS}/PveAuction/config.yml"
CITIZENS_JAR="${PLUGINS}/Citizens.jar"

echo "==> Сервер: ${SERVER_DIR}"
echo "==> plugins: ${PLUGINS}"

mkdir -p "${PLUGINS}/PveAuction"

echo "==> Один PveAuction.jar (удаляем дубликаты и кэш Paper)"
rm -f "${PLUGINS}/PveAuction.jar"
rm -rf "${PLUGINS}/.paper-remapped"
shopt -s nullglob
jars=("${PLUGINS}"/PveAuction-*.jar)
if (( ${#jars[@]} == 0 )); then
  echo ""
  echo "НЕТ PveAuction-*.jar в ${PLUGINS}"
  echo "С Mac: scp .../PveAuction-0.1.21.jar root@IP:${PLUGINS}/"
  echo ""
  exit 1
fi
if (( ${#jars[@]} > 1 )); then
  newest=$(ls -1t "${PLUGINS}"/PveAuction-*.jar | head -1)
  echo "Оставляем: $(basename "${newest}")"
  for f in "${PLUGINS}"/PveAuction-*.jar; do
    [[ "${f}" == "${newest}" ]] || rm -f "${f}"
  done
fi

echo "==> JAR:"
ls -lah "${PLUGINS}"/PveAuction*.jar

download_citizens() {
  # Paper 1.21.4 (у тебя build 232) — НЕ совместим с Citizens 2.0.42-b4186.
  # Берём сборку 4138 (2.0.41), там ещё есть модуль v1_21_R5 под 1.21.4.
  local url="https://ci.citizensnpcs.co/job/Citizens2/4138/artifact/dist/target/Citizens-2.0.41-b4138.jar"
  echo "==> Качаем Citizens для Paper 1.21.4: ${url}"
  wget -q -O "${CITIZENS_JAR}.tmp" "${url}" || return 1
  if [[ ! -s "${CITIZENS_JAR}.tmp" ]]; then
    rm -f "${CITIZENS_JAR}.tmp"
    return 1
  fi
  if ! file "${CITIZENS_JAR}.tmp" | grep -qi 'zip archive\|java archive'; then
    echo "Ошибка: скачался не jar (битый файл / HTML). Удали и качай снова."
    rm -f "${CITIZENS_JAR}.tmp"
    return 1
  fi
  if ! unzip -t "${CITIZENS_JAR}.tmp" >/dev/null 2>&1; then
    echo "Ошибка: jar повреждён (zip END header not found)."
    rm -f "${CITIZENS_JAR}.tmp"
    return 1
  fi
  mv "${CITIZENS_JAR}.tmp" "${CITIZENS_JAR}"
  return 0
}

if [[ -f "${CITIZENS_JAR}" ]]; then
  if ! unzip -t "${CITIZENS_JAR}" >/dev/null 2>&1; then
    echo "==> Старый Citizens.jar битый — перекачиваем"
    rm -f "${CITIZENS_JAR}"
  else
    echo "==> Citizens уже есть и jar целый"
    ls -lah "${CITIZENS_JAR}"
  fi
fi

if [[ ! -f "${CITIZENS_JAR}" ]]; then
  if ! download_citizens; then
    echo ""
    echo "Не удалось скачать Citizens."
    echo "Вручную (1.21.4): https://ci.citizensnpcs.co/job/Citizens2/4138/artifact/dist/target/Citizens-2.0.41-b4138.jar"
    echo "Или обнови Paper до 1.21.6+ и возьми последний Citizens с ci.citizensnpcs.co"
    echo "Сохрани как: ${CITIZENS_JAR}"
    echo ""
    exit 1
  fi
  echo "Citizens OK: $(ls -lah "${CITIZENS_JAR}" | awk '{print $5, $9}')"
fi

if [[ -f "${CFG}" ]] && grep -q '^trader:' "${CFG}"; then
  echo "==> trader: уже в config.yml"
else
  echo "==> Дописываем trader в ${CFG}"
  cat >> "${CFG}" <<'EOF'

# Торгаш (добавлено vps-setup.sh)
trader:
  enabled: true
  skin-name: "Steve"
  look-range: 16
  look-interval-ticks: 5
  location:
    world: world
    x: -4.5
    y: -21
    z: -7.5
    yaw: 90
  prices:
    spheres: 12
    weapons: 20
    armor: 18
    tools: 15
    enchantments: 25
EOF
fi

echo ""
echo "=============================================="
echo "ГОТОВО. Дальше вручную:"
echo ""
echo "1) Останови сервер в консоли Minecraft:"
echo "     stop"
echo ""
echo "2) Запусти снова:"
echo "     cd ${SERVER_DIR} && ./start.sh"
echo ""
echo "3) В логе должно быть:"
echo "     - PveAuction (0.1.21) — ОДИН раз, без Ambiguous plugin"
echo "     - [PveAuction] Торгаш (житель) заспавнен"
echo "     Citizens не обязателен (торгаш — житель)"
echo ""
echo "4) В игре: /spawn, ПКМ торгаш, /givesilver give <ник> 64"
echo "=============================================="
