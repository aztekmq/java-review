#!/usr/bin/env bash
# Compile all advanced-level labs with verbose logging to aid troubleshooting.
set -euo pipefail
set -x

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/advanced"

echo "===> Compiling advanced labs..."
make all
echo "Advanced labs compiled successfully."
