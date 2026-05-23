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

# Paper 1.21.4 — Java 21 (минимум 17)
find_java() {
  local c
  local candidates=()
  if [[ -n "${JAVA_HOME:-}" ]]; then
    candidates+=("${JAVA_HOME}/bin/java")
  fi
  if command -v java >/dev/null 2>&1; then
    candidates+=("$(command -v java)")
  fi
  # Linux (Ubuntu/Debian VPS)
  for c in /usr/lib/jvm/java-21-openjdk-amd64/bin/java \
           /usr/lib/jvm/java-21-openjdk-arm64/bin/java \
           /usr/lib/jvm/java-17-openjdk-amd64/bin/java; do
    candidates+=("${c}")
  done
  # macOS Homebrew
  for c in /opt/homebrew/opt/openjdk@21/bin/java \
           /opt/homebrew/opt/openjdk@17/bin/java \
           /usr/local/opt/openjdk@21/bin/java; do
    candidates+=("${c}")
  done
  if [[ "$(uname -s)" == "Darwin" ]] && /usr/libexec/java_home -v 21 &>/dev/null; then
    candidates+=("$(/usr/libexec/java_home -v 21)/bin/java")
  fi
  for c in "${candidates[@]}"; do
    if [[ -x "${c}" ]] && "${c}" -version 2>&1 | grep -qE 'version "(21|1[7-9]|[2-9][0-9])'; then
      echo "${c}"
      return 0
    fi
  done
  return 1
}

JAVA_BIN=$(find_java) || {
  echo "Нужна Java 21 (минимум 17) для Paper 1.21.4."
  echo "  VPS:  apt install -y openjdk-21-jdk"
  echo "  Mac:  brew install openjdk@21"
  exit 1
}

JAVA_VER=$("${JAVA_BIN}" -version 2>&1 | head -1)

# VPS 1 GB: MIN_RAM=512M MAX_RAM=768M ./start.sh
if [[ -z "${MIN_RAM:-}" && "$(uname -s)" == "Linux" ]]; then
  mem_kb=$(grep MemTotal /proc/meminfo 2>/dev/null | awk '{print $2}')
  if [[ -n "${mem_kb}" && "${mem_kb}" -lt 2500000 ]]; then
    MIN_RAM=512M
    MAX_RAM=768M
  fi
fi
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
