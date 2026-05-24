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

./gradlew clean jar --no-daemon
JAR=$(ls -1t build/libs/PveAuction-*.jar 2>/dev/null | head -1)
if [[ -z "${JAR}" || ! -f "${JAR}" ]]; then
  echo "JAR не найден в build/libs/"
  exit 1
fi

mkdir -p ../plugins
rm -f ../plugins/PveAuction-*.jar
cp "${JAR}" ../plugins/
echo "Скопировано: ../plugins/$(basename "${JAR}")"
echo "MD5: $(md5 -q "${JAR}" 2>/dev/null || md5sum "${JAR}" | cut -d' ' -f1)"
echo ""
echo "Локальный Paper: cd ../.. && ./start.sh  (или перезапуск уже запущенного сервера)"
echo "Удалённый VPS:   ./deploy-remote.sh root@IP   (не боты 4narek-1.12)"
echo "В игре проверка: /shop → чат «сборка 0.1.3», заголовок «Скупка [0.1.3]»"
