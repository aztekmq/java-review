#!/usr/bin/env bash
# CI helper script that compiles all labs and builds the analyzer with verbose tracing.
set -euo pipefail
set -x

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "===> CI: Compiling beginner labs..."
./scripts/compile_beginner.sh

echo "===> CI: Compiling intermediate labs..."
./scripts/compile_intermediate.sh

echo "===> CI: Compiling advanced labs..."
./scripts/compile_advanced.sh

echo "===> CI: Building analyzer..."
./scripts/build_analyzer.sh

echo "===> CI: All compilation steps succeeded."
