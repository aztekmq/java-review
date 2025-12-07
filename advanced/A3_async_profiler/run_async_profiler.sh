#!/usr/bin/env bash
# Compile and launch AsyncProfilerLab with verbose logging so async-profiler can attach cleanly.
# This script follows international programming standards by enabling strict flags and tracing every step.
set -euo pipefail
set -x

LAB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$LAB_DIR"

javac AsyncProfilerLab.java

echo "===> Starting AsyncProfilerLab with verbose GC and JFR logging (awaiting async-profiler attach)"
JAVA_OPTS=(
  -Xms2g
  -Xmx2g
  -XX:StartFlightRecording=filename=advanced-a3.jfr,dumponexit=true,settings=profile
  -Xlog:gc*:file=advanced-a3-gc.log:uptime,time,level,tags
  -XX:+HeapDumpOnOutOfMemoryError
  -cp "$LAB_DIR"
)

# Launch the workload. Leave this running, then attach async-profiler from another terminal:
#   profiler.sh -d 30 -e cpu -f cpu.svg <pid>
#   profiler.sh -d 30 -e alloc -f alloc.svg <pid>
# The console output includes FINEST-level lifecycle events to correlate with flame graphs.
java "${JAVA_OPTS[@]}" AsyncProfilerLab
