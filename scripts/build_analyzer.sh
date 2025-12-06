#!/usr/bin/env bash
# Build the JVM Health Analyzer module using Maven with verbose command tracing.
set -euo pipefail
set -x

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/analyzer"

echo "===> Building JVM Health Analyzer (Maven)..."
# Pre-flight Java version check to avoid the "release version 17 not supported" compiler
# failure when an older JDK is picked up. The parser mirrors common `java -version`
# output formats (for example, "openjdk version \"21.0.9\" 2025-10-21 LTS" or
# "openjdk 17.0.11 2024-04-16"), and emits a verbose hint when the requirement is
# not satisfied.
JAVA_VERSION_STRING=$(java -version 2>&1 | head -n 1)

# Extract the version digits by prioritising the content inside quotes (new-style
# output) and falling back to the first numeric token (old-style output). Awk is
# used here for its portability and to minimise quoting pitfalls that surfaced
# with more compact regex-only parsing when vendor-specific date suffixes were present.
JAVA_VERSION_RAW=$(printf '%s' "$JAVA_VERSION_STRING" | awk -F '"' 'NF>=2 {print $2; exit}')

if [[ -z "$JAVA_VERSION_RAW" ]]; then
  JAVA_VERSION_RAW=$(printf '%s' "$JAVA_VERSION_STRING" | awk '{for (i=1; i<=NF; i++) if ($i ~ /^[0-9]+(\.[0-9]+)*$/) {print $i; exit}}')
fi

JAVA_MAJOR=""
if [[ -n "$JAVA_VERSION_RAW" ]]; then
  JAVA_MAJOR=$(printf '%s' "$JAVA_VERSION_RAW" | cut -d'.' -f1)
fi

if [[ -z "$JAVA_MAJOR" || "$JAVA_MAJOR" -lt 17 ]]; then
  echo "Detected Java runtime: $JAVA_VERSION_STRING" >&2
  echo "Parsed Java major version: ${JAVA_MAJOR:-<none>} (expected >= 17)" >&2
  echo "This build requires JDK 17 or newer. Please point JAVA_HOME to a compatible JDK before rerunning." >&2
  exit 1
fi

echo "Java $JAVA_MAJOR detected from '$JAVA_VERSION_STRING'; proceeding with --release 17 build."

# Run Maven with full debug output (-X) to satisfy verbose logging requirements and
# simplify troubleshooting in automated environments.
mvn --batch-mode -X -DskipTests package
echo "Analyzer built successfully."
