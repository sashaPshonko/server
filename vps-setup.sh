#!/usr/bin/env bash
# Один раз на VPS (Ubuntu/Debian). Локально на Mac не нужен.
set -euo pipefail
cd "$(dirname "$0")"

echo "==> Java 21 (Ubuntu/Debian)"
if command -v apt-get >/dev/null 2>&1; then
  sudo apt-get update -qq
  sudo apt-get install -y openjdk-21-jre-headless curl
else
  echo "Установи Java 21 вручную"
fi

if [[ ! -f paper.jar ]]; then
  echo "==> Paper"
  MC_VERSION="${MC_VERSION:-1.21.4}"
  chmod +x setup.sh
  ./setup.sh
fi

chmod +x start.sh install-economy.sh address.sh
[[ -f install-economy.sh ]] && ./install-economy.sh
if [[ -f server-plugin/gradlew ]]; then
  chmod +x server-plugin/gradlew server-plugin/build.sh
  echo "    Плагин: cd server-plugin && ./build.sh"
fi

echo ""
echo "==> Файрвол (ufw)"
if command -v ufw >/dev/null 2>&1; then
  sudo ufw allow 25565/tcp || true
  echo "    sudo ufw allow 25565/tcp"
fi

echo ""
echo "==> Дальше"
echo "  1. В public-host.txt впиши IP этого VPS"
echo "  2. ./address.sh"
echo "  3. MIN_RAM=4G MAX_RAM=4G ./start.sh"
echo "  4. Друзья: Прямое подключение → адрес из public-host.txt"
echo ""
echo "  systemd (24/7): см. systemd/minecraft.service.example"
