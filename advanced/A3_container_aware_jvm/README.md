# A3: Container-Aware JVM Lab (Host-First Execution)

This lab now runs directly on the host JVM—no containers required. The helper
script `run-standalone-app.sh` compiles and launches `MyContainerApp` with
diagnostic settings that mirror the original lab while keeping all actions
fully visible through verbose logging. By default, both the JFR and GC log
artifacts are written to the repository root (for example, `./advanced-a3.jfr`
and `./advanced-a3-gc.log`) so they live alongside the other lab outputs.

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
- `JFR_DUMP_PATH` and `GC_LOG_PATH` override the default artifact locations;
  the defaults point at the repository root to keep artifacts aligned with
  other labs (`./advanced-a3.jfr` and `./advanced-a3-gc.log`).

These defaults keep the lab transparent and debuggable without any container
runtime involvement.

## Troubleshooting class-version errors

If you see an error similar to:

```
Error: LinkageError occurred while loading main class MyContainerApp
java.lang.UnsupportedClassVersionError: MyContainerApp has been compiled by a more recent version of the Java Runtime (class file version 65.0), this version of the Java Runtime only recognizes class file versions up to 64.0
```

the host `javac` created bytecode for a newer Java release than your `java`
runtime can execute. Resolve this by:

- Running `./run-standalone-app.sh`, which automatically compiles with
  `--release` set to the detected host JVM version and logs both `java -version`
  and `javac -version` for visibility.
- Manually recompiling with a matching target level, for example `javac --release 20 MyContainerApp.java`,
  when your runtime is Java 20.
- Upgrading the runtime to the version used for compilation.

Verbose shell tracing and version logging in the helper script make the chosen
toolchain explicit, helping avoid the mismatch shown above.
