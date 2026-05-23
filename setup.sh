#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

MC_VERSION="${MC_VERSION:-1.21.4}"
API="https://api.papermc.io/v2/projects/paper/versions/${MC_VERSION}"

echo "==> Paper ${MC_VERSION}"

if command -v java >/dev/null 2>&1 && java -version >/dev/null 2>&1; then
  echo "    $(java -version 2>&1 | head -1)"
else
  echo "    Java не найдена — для ./start.sh нужна Java 21+ (brew install openjdk@21)"
fi

echo "==> Последний билд Paper..."
BUILDS_JSON=$(curl -fsSL "${API}")
LATEST_BUILD=$(echo "${BUILDS_JSON}" | python3 -c "import sys,json; print(json.load(sys.stdin)['builds'][-1])")
JAR_NAME=$(curl -fsSL "${API}/builds/${LATEST_BUILD}" | python3 -c "import sys,json; print(json.load(sys.stdin)['downloads']['application']['name'])")
URL="${API}/builds/${LATEST_BUILD}/downloads/${JAR_NAME}"

if [[ -f paper.jar ]]; then
  echo "    paper.jar уже есть — пропуск (удали файл, чтобы скачать заново)"
else
  echo "==> Скачивание ${JAR_NAME}..."
  curl -fL#o paper.jar "${URL}"
fi

if [[ ! -f eula.txt ]] || ! grep -q '^eula=true' eula.txt; then
  echo "eula=true" > eula.txt
fi

mkdir -p plugins logs
echo ""
echo "Готово. Запуск: ./start.sh"
echo "В игре: Multiplayer → Direct Connect → localhost"
