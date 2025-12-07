# A3: Container-Aware JVM Lab (Host-First Execution)

This lab now runs directly on the host JVM—no containers required. The helper
script `run-standalone-app.sh` compiles and launches `MyContainerApp` with
diagnostic settings that mirror the original lab while keeping all actions
fully visible through verbose logging.

## Running the lab without containers

```bash
cd advanced/A3_container_aware_jvm
./run-standalone-app.sh
```

Key details:

- The script auto-detects the host JVM version and compiles with a matching
  `--release` level to avoid class-version errors.
- Verbose shell tracing (`set -x`) exposes every step for debugging.
- JVM diagnostics (JFR, GC logging, and heap dumps on OOM) remain enabled to
  support performance observations.

## Customizing the run

- `TARGET_RELEASE` sets an explicit `javac --release` level if you want to pin
  compilation to a specific Java version.
- `JAVA_OPTS` appends extra JVM flags (for example, `JAVA_OPTS="-Xms1g"`).
- `JFR_DUMP_PATH` and `GC_LOG_PATH` override the default artifact locations
  (`advanced-a3.jfr` and `advanced-a3-gc.log`).

These defaults keep the lab transparent and debuggable without any container
runtime involvement.
