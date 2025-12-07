#!/usr/bin/env bash
# Enumerate Java Flight Recorder (JFR) captures and garbage-collection (GC) logs
# produced by the lab exercises. This script uses verbose logging to aid
# troubleshooting and follows defensive bash practices for predictable behavior.
# Usage: run from any directory; the script will relocate itself to the project
# root and print relative paths for each discovered artifact.
set -euo pipefail
set -x

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

log_heading() {
  local heading="$1"
  echo "===> ${heading}"
}

enumerate_artifacts() {
  local description="$1"
  local pattern="$2"
  mapfile -t artifacts < <(find "$ROOT_DIR" -maxdepth 2 -type f -name "${pattern}" | sort)

  if (( ${#artifacts[@]} == 0 )); then
    echo "No ${description} files found."
  else
    for artifact in "${artifacts[@]}"; do
      # Trim the repository prefix to keep the output concise and reproducible.
      printf "%s: %s\n" "${description}" "${artifact#${ROOT_DIR}/}"
    done
  fi
}

log_heading "Enumerating JFR recordings within ${ROOT_DIR}"
enumerate_artifacts "JFR" "*.jfr"

log_heading "Enumerating GC logs within ${ROOT_DIR}"
enumerate_artifacts "GC log" "*-gc.log"

log_heading "Artifact scan complete."
