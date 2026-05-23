#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

find_java_home() {
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    return 0
  fi
  local mac=(
    /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
    /usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
  )
  local p
  for p in "${mac[@]}"; do
    if [[ -x "${p}/bin/java" ]]; then
      export JAVA_HOME="${p}"
      export PATH="${JAVA_HOME}/bin:${PATH}"
      return 0
    fi
  done
  if command -v java >/dev/null 2>&1; then
    return 0
  fi
  echo "Нужна Java 21: Ubuntu → sudo apt install openjdk-21-jdk"
  exit 1
}

find_java_home

if [[ ! -x ./gradlew ]]; then
  echo "Нет gradlew — залей папку server-plugin целиком с Mac"
  exit 1
fi

./gradlew jar --no-daemon
JAR="build/libs/PveAuction-0.1.0.jar"
if [[ ! -f "${JAR}" ]]; then
  JAR=$(ls -1 build/libs/PveAuction-*.jar 2>/dev/null | head -1)
fi

mkdir -p ../plugins
cp "${JAR}" ../plugins/
echo "Скопировано: ../plugins/$(basename "${JAR}")"
echo "Перезапусти сервер и в игре: /ah"
