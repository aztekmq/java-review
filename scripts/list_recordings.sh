#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# list_recordings.sh
# -----------------------------------------------------------------------------
# Enumerate Java Flight Recorder (JFR) captures and garbage-collection (GC)
# logs produced by the lab exercises. This script uses verbose logging to aid
# troubleshooting and follows defensive Bash practices for predictable
# behavior, aligning with international programming standards for diagnostic
# tooling.
#
# Usage:
#   - Run from any directory; the script relocates itself to the project root
#     and prints relative paths for each discovered artifact.
#   - Optionally set SEARCH_DEPTH to control how deep the search should walk
#     from the repository root. Use "unbounded" to scan the full tree. The
#     default depth of 3 favors speed while still including lab directories.
#     Example: SEARCH_DEPTH=unbounded ./scripts/list_recordings.sh
#
# All actions are traced (`set -x`) so the search flow can be debugged easily.
set -euo pipefail
set -x

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

SEARCH_DEPTH=${SEARCH_DEPTH:-3}
if [[ "$SEARCH_DEPTH" != "unbounded" && ! "$SEARCH_DEPTH" =~ ^[0-9]+$ ]]; then
  echo "SEARCH_DEPTH must be an integer or 'unbounded'." >&2
  exit 1
fi

FIND_DEPTH_ARGS=()
SEARCH_DEPTH_LABEL="unbounded search"
if [[ "$SEARCH_DEPTH" != "unbounded" ]]; then
  FIND_DEPTH_ARGS=(-maxdepth "$SEARCH_DEPTH")
  SEARCH_DEPTH_LABEL="max depth ${SEARCH_DEPTH}"
fi

log_heading() {
  local heading="$1"
  echo "===> ${heading}"
}

enumerate_artifacts() {
  local description="$1"
  local pattern="$2"
  echo "Searching for ${description} files matching '${pattern}' under ${ROOT_DIR} (${SEARCH_DEPTH_LABEL})."
  mapfile -t artifacts < <(find "$ROOT_DIR" "${FIND_DEPTH_ARGS[@]}" -type f -name "${pattern}" | sort)

  if (( ${#artifacts[@]} == 0 )); then
    echo "No ${description} files found."
  else
    for artifact in "${artifacts[@]}"; do
      # Trim the repository prefix to keep the output concise and reproducible.
      printf "%s: %s\n" "${description}" "${artifact#${ROOT_DIR}/}"
    done
  fi
}

log_heading "Enumerating JFR recordings within ${ROOT_DIR} (${SEARCH_DEPTH_LABEL})"
enumerate_artifacts "JFR" "*.jfr"

log_heading "Enumerating GC logs within ${ROOT_DIR} (${SEARCH_DEPTH_LABEL})"
enumerate_artifacts "GC log" "*-gc.log"

log_heading "Artifact scan complete."
