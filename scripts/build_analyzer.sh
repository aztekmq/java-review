#!/usr/bin/env bash
# Build the JVM Health Analyzer module using Maven with verbose command tracing.
set -euo pipefail
set -x

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/analyzer"

echo "===> Building JVM Health Analyzer (Maven)..."
# Pre-flight Java version check to avoid the "release version 17 not supported" compiler
# failure when an older JDK is picked up. The parser mirrors common `java -version`
# output formats (e.g., "17.0.10" or "21"), and emits a verbose hint when the
# requirement is not satisfied.
JAVA_VERSION_STRING=$(java -version 2>&1 | head -n 1)
JAVA_MAJOR=$(printf '%s' "$JAVA_VERSION_STRING" | sed -E 's/.*"?([0-9]+)(\.[0-9]+)?(\.[0-9]+)?"?.*/\1/')

if [[ -z "$JAVA_MAJOR" || "$JAVA_MAJOR" -lt 17 ]]; then
  echo "Detected Java runtime: $JAVA_VERSION_STRING" >&2
  echo "This build requires JDK 17 or newer. Please point JAVA_HOME to a compatible JDK before rerunning." >&2
  exit 1
fi

echo "Java $JAVA_MAJOR detected; proceeding with --release 17 build."

# Run Maven with full debug output (-X) to satisfy verbose logging requirements and
# simplify troubleshooting in automated environments.
mvn --batch-mode -X -DskipTests package
echo "Analyzer built successfully."
