#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

mkdir -p plugins

VAULT_URL="https://github.com/MilkBowl/Vault/releases/download/1.7.3/Vault.jar"
ESS_URL="https://github.com/EssentialsX/Essentials/releases/download/2.22.0/EssentialsX-2.22.0.jar"
SPAWN_URL="https://github.com/EssentialsX/Essentials/releases/download/2.22.0/EssentialsXSpawn-2.22.0.jar"

echo "==> Vault"
curl -fL#o plugins/Vault.jar "${VAULT_URL}"

echo "==> EssentialsX"
curl -fL#o plugins/EssentialsX-2.22.0.jar "${ESS_URL}"

echo "==> EssentialsXSpawn (только экономика; /spawn — в PveAuction)"
curl -fL#o plugins/EssentialsXSpawn-2.22.0.jar "${SPAWN_URL}"

echo ""
echo "Готово: Vault, EssentialsX, EssentialsXSpawn в plugins/"
echo "Перезапусти сервер. В логе PveAuction: «Экономика Vault подключена»."
echo ""
echo "Важно: отключи Essentials /spawn — см. config-templates/essentials-spawn.txt"
