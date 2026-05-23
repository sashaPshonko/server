#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if [[ -d "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

if ! command -v gradle >/dev/null 2>&1; then
  echo "Нужен Gradle: brew install gradle"
  exit 1
fi

gradle jar
JAR="build/libs/PveAuction-0.1.0.jar"
if [[ ! -f "${JAR}" ]]; then
  JAR=$(ls -1 build/libs/PveAuction-*.jar 2>/dev/null | head -1)
fi

mkdir -p ../plugins
cp "${JAR}" ../plugins/
echo "Скопировано: ../plugins/$(basename "${JAR}")"
echo "Перезапусти сервер и в игре: /ah"
