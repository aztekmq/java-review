#!/usr/bin/env bash
#
# Title: run-standalone-app.sh
# Purpose: Compile and run MyContainerApp directly on the host JVM without any
# container tooling. All steps are logged verbosely to support debugging in
# alignment with international programming standards for diagnostic
# traceability.
#
# Usage examples:
#   ./run-standalone-app.sh                 # auto-detect host Java version
#   TARGET_RELEASE=20 ./run-standalone-app.sh  # force a specific target level
#
# Environment overrides:
#   TARGET_RELEASE   - Optional java --release level to compile against.
#   JAVA_OPTS        - Additional JVM options appended to the defaults.
#   JFR_DUMP_PATH    - Custom path for the JFR dump file (default advanced-a3.jfr).
#   GC_LOG_PATH      - Custom path for the GC log file (default advanced-a3-gc.log).

set -euo pipefail
set -x

cd "$(dirname "$0")"

log() {
  printf '[run-standalone-app] %s\n' "$*"
}

print_java_diagnostics() {
  log "Diagnosing host Java toolchain for compatibility"
  log "java executable: $(command -v java)"
  java -version 2>&1 | sed 's/^/[run-standalone-app] java -version: /'

  log "javac executable: $(command -v javac)"
  javac -version 2>&1 | sed 's/^/[run-standalone-app] javac -version: /'
}

detect_release() {
  if [[ -n "${TARGET_RELEASE-}" ]]; then
    log "Using user-specified target release: ${TARGET_RELEASE}"
    echo "${TARGET_RELEASE}"
    return
  fi

  local version_line
  version_line=$(java -version 2>&1 | head -n1)
  if [[ ${version_line} =~ ([0-9]+) ]]; then
    local detected=${BASH_REMATCH[1]}
    log "Auto-detected target release from host JVM: ${detected}"
    echo "${detected}"
    return
  fi

  log "Warning: unable to detect host JVM version; defaulting to release 21"
  echo "21"
}

print_java_diagnostics

target_release=$(detect_release)

log "Compiling MyContainerApp.java with --release ${target_release}"
javac --release "${target_release}" MyContainerApp.java

jfr_path=${JFR_DUMP_PATH:-advanced-a3.jfr}
gc_log_path=${GC_LOG_PATH:-advanced-a3-gc.log}

default_opts=(
  -Xms512m
  -Xmx512m
  -XX:ActiveProcessorCount=2
  -XX:StartFlightRecording=filename="${jfr_path}",dumponexit=true,settings=profile
  -Xlog:gc*:file="${gc_log_path}":uptime,time,level,tags
  -XX:+HeapDumpOnOutOfMemoryError
  -XshowSettings:vm
)

log "Launching MyContainerApp on host JVM with verbose diagnostics"
java "${default_opts[@]}" ${JAVA_OPTS-} MyContainerApp
