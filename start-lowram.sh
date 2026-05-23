#!/usr/bin/env bash
# VPS 1 GB — сначала: ./add-swap.sh
set -euo pipefail
cd "$(dirname "$0")"
export MIN_RAM=384M
export MAX_RAM=512M
exec ./start.sh
