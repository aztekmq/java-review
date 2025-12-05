#!/usr/bin/env bash
# Compile all beginner-level labs with verbose logging for diagnostics.
set -euo pipefail
set -x

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/beginner"

echo "===> Compiling beginner labs..."
make all
echo "Beginner labs compiled successfully."
