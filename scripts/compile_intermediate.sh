#!/usr/bin/env bash
# Compile all intermediate-level labs with verbose output for debugging.
set -euo pipefail
set -x

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/intermediate"

echo "===> Compiling intermediate labs..."
make all
echo "Intermediate labs compiled successfully."
