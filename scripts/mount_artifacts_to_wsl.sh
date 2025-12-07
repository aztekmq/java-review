#!/usr/bin/env bash
# Purpose: Bind-mount a host/WSL-accessible directory into the repository so
# JVM artifacts (JFR, GC logs, heap dumps) land directly on the Windows-visible
# path without needing a copy/export step. Uses verbose logging for debuggability
# and follows portable shell conventions.

set -euo pipefail
set -x

usage() {
  cat <<'USAGE'
Usage: mount_artifacts_to_wsl.sh <host-artifact-dir>

Bind-mounts the given host/WSL directory (e.g., /mnt/c/Users/<you>/Documents/java-review-artifacts)
onto the repository path ./wsl-artifacts so that any file written there is immediately
visible in Windows/WSL. Requires mount permissions (run with sudo if needed).
USAGE
}

if [[ ${1-} == "-h" || ${1-} == "--help" ]]; then
  usage
  exit 0
fi

if [[ $# -lt 1 ]]; then
  echo "[ERROR] Host artifact directory is required." >&2
  usage
  exit 1
fi

HOST_DIR="$1"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MOUNT_POINT="$REPO_ROOT/wsl-artifacts"

mkdir -p "$HOST_DIR"
mkdir -p "$MOUNT_POINT"

if mountpoint -q "$MOUNT_POINT"; then
  echo "[INFO] Unmounting existing mount at $MOUNT_POINT for a clean rebind" >&2
  umount "$MOUNT_POINT"
fi

if ! mount --bind "$HOST_DIR" "$MOUNT_POINT"; then
  cat <<'ERR' >&2
[ERROR] Bind mount failed. Ensure you have mount privileges in this environment.
- If running inside a restricted container, retry with sudo from the host/WSL shell
  or run the labs directly from WSL where /workspace maps to /mnt/c/...
- For CI environments without mount support, fall back to the export helper script.
ERR
  exit 1
fi

cat <<INFO
[INFO] Bind mount established.
[INFO] Host directory: $HOST_DIR
[INFO] Repository mount: $MOUNT_POINT
[INFO] Write artifacts to: $MOUNT_POINT (appears at the host path above immediately)
[INFO] To verify: touch "$MOUNT_POINT/test.txt" then ls "$HOST_DIR"
INFO
