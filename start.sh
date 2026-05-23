#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if [[ ! -f paper.jar ]]; then
  echo "Нет paper.jar — сначала: ./setup.sh"
  exit 1
fi

if [[ ! -f eula.txt ]] || ! grep -q '^eula=true' eula.txt; then
  echo "Примите EULA: eula=true в eula.txt"
  exit 1
fi

# macOS: /usr/bin/java — заглушка; Paper нужен Java 17+ (для 1.21 лучше 21)
find_java() {
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    echo "${JAVA_HOME}/bin/java"
    return 0
  fi
  local candidates=(
    /opt/homebrew/opt/openjdk@21/bin/java
    /opt/homebrew/opt/openjdk@17/bin/java
    /usr/local/opt/openjdk@21/bin/java
    /usr/local/opt/openjdk@17/bin/java
  )
  local c
  for c in "${candidates[@]}"; do
    if [[ -x "${c}" ]]; then
      echo "${c}"
      return 0
    fi
  done
  if /usr/libexec/java_home -v 21 &>/dev/null; then
    echo "$(/usr/libexec/java_home -v 21)/bin/java"
    return 0
  fi
  if /usr/libexec/java_home -v 17 &>/dev/null; then
    echo "$(/usr/libexec/java_home -v 17)/bin/java"
    return 0
  fi
  return 1
}

JAVA_BIN=$(find_java) || {
  echo "Нужна Java 17+ (для Paper 1.21.4 — Java 21)."
  echo "  brew install openjdk@21"
  echo "  export PATH=\"/opt/homebrew/opt/openjdk@21/bin:\$PATH\""
  exit 1
}

JAVA_VER=$("${JAVA_BIN}" -version 2>&1 | head -1)

# Локально: 2G. На VPS: MIN_RAM=4G MAX_RAM=4G ./start.sh
MIN_RAM="${MIN_RAM:-2G}"
MAX_RAM="${MAX_RAM:-2G}"

mkdir -p plugins logs

JAVA_OPTS=(
  -Xms"${MIN_RAM}"
  -Xmx"${MAX_RAM}"
  -Dfile.encoding=UTF-8
  -Djava.net.preferIPv4Stack=true
)

echo "==> ${JAVA_VER}"
echo "==> Старт Paper (RAM ${MIN_RAM}–${MAX_RAM}), порт 25565"
echo "    Остановка: stop (в консоли) или Ctrl+C"
if [[ -f public-host.txt ]]; then
  PUB=$(grep -v '^#' public-host.txt | grep -v '^[[:space:]]*$' | head -1 | tr -d '[:space:]')
  if [[ -n "${PUB}" && "${PUB}" != *"your-server"* ]]; then
    echo "    Друзья подключаются: ${PUB}  (./address.sh)"
  else
    echo "    Заполни public-host.txt — адрес playit или IP VPS"
  fi
fi
exec "${JAVA_BIN}" "${JAVA_OPTS[@]}" -jar paper.jar --nogui
