#!/usr/bin/env bash
# Purpose: Copy JVM artifacts (JFR recordings, GC logs, heap dumps) from the repository
# inside the container (mounted at /workspace/...) to a WSL-accessible path such as
# /mnt/c/Users/<you>/Documents/githubdev/java-review-artifacts. Verbose logging is
# enabled for traceability, and the script follows portable shell practices.

set -euo pipefail
set -x

usage() {
  cat <<'USAGE'
Usage: export_artifacts_to_wsl.sh <destination-directory>

Copies *.jfr, *.log, and *.hprof files from the repository into the specified
WSL-accessible directory (e.g., /mnt/c/... on Windows). The destination
folder will be created if it does not already exist.
USAGE
}

if [[ ${1-} == "-h" || ${1-} == "--help" ]]; then
  usage
  exit 0
fi

if [[ $# -lt 1 ]]; then
  echo "[ERROR] Destination directory is required." >&2
  usage
  exit 1
fi

DEST_DIR="$1"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

mkdir -p "$DEST_DIR"

copy_group() {
  local label="$1"
  local pattern="$2"
  local found="false"

  while IFS= read -r -d '' file; do
    found="true"
    cp -v "$file" "$DEST_DIR"/
  done < <(find "$REPO_ROOT" -type f -name "$pattern" -print0)

  if [[ "$found" == "false" ]]; then
    echo "[INFO] No $label artifacts matching '$pattern' were found under $REPO_ROOT" >&2
  fi
}

copy_group "JFR" "*.jfr"
copy_group "GC log" "*.log"
copy_group "Heap dump" "*.hprof"

cat <<INFO
[INFO] Artifact copy complete.
[INFO] Repository root: $REPO_ROOT
[INFO] Destination:     $DEST_DIR
[INFO] Verify from WSL: ls "$DEST_DIR"
INFO
