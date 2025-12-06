#!/usr/bin/env bash
# Build the JVM Health Analyzer module using Maven with verbose command tracing.
set -euo pipefail
set -x

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/analyzer"

echo "===> Building JVM Health Analyzer (Maven)..."
# Run Maven with full debug output (-X) to satisfy verbose logging requirements and
# simplify troubleshooting in automated environments.
mvn --batch-mode -X -DskipTests package
echo "Analyzer built successfully."
