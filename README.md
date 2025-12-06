# java-review – JVM Tuning & Diagnostics Lab (A–Z Guide)

`java-review` is a **hands-on JVM tuning and diagnostics lab** that walks you through everything from first heap inspections to low-latency garbage collectors and JFR-based production forensics. It is organized into three progressive learning tracks plus a JVM Health Analyzer utility. All scripts and builds ship with verbose logging so you can trace every action for repeatable diagnostics.

## Prerequisites

- **JDK 17+** for compiling and running the exercises (adjust `JAVAC` if needed).
- **Maven 3.8+** for the `analyzer/` module.
- **Docker** (optional) for the container-aware JVM lab.
- **GNU Make** for the provided `Makefile` automation.

## Repository Layout

- `beginner/` – JVM fundamentals (heap behavior, GC basics, thread states, dumps).
- `intermediate/` – GC tuning, leak simulation, and lock contention diagnostics.
- `advanced/` – Low-latency collectors, deep JFR profiling, JVM-in-container behavior.
- `analyzer/` – Maven module that reads JFR recordings and GC logs to emit a JVM Health Report with verbose summaries.
- `scripts/` – Bash utilities (all `set -x`) to compile every track and build the analyzer for CI-style runs.
- `tips/`, `garbage-collection.md` – Supplemental reading on JVM performance concepts.

## Quickstart (A–Z)

1. **Clone the repository**

   ```bash
   git clone https://github.com/<your-user>/java-review.git
   cd java-review
   ```

2. **Build everything with verbose automation**

   - `make all` – Compiles beginner, intermediate, advanced labs and builds the analyzer via Maven.
   - `scripts/ci_compile_all.sh` – Equivalent CI helper that traces each step (uses `set -x`).

3. **Run a specific lab (per track)**

   - Beginner example: `java -Xms256m -Xmx256m -Xlog:gc* beginner/B1_gc_basics/GcBasics`
   - Intermediate example: `java -Xlog:gc*:file=gc.log -XX:+UseG1GC intermediate/I1_gc_tuning_g1/MyServiceApp`
   - Advanced example: `java -XX:+UseZGC -Xlog:gc*:file=zgc.log advanced/A1_low_latency_gc/LowLatencyApp`

4. **Analyze runtime evidence**

   Use the JVM Health Analyzer after capturing a JFR + GC log (all steps keep verbose logging enabled for traceability):

   ```bash
   cd analyzer
   mvn -q -DskipTests package

   java --add-exports jdk.jfr/jdk.jfr.consumer=ALL-UNNAMED \
        -cp target/jvm-health-analyzer-1.0-SNAPSHOT.jar \
        com.example.jvmhealth.JvmHealthAnalyzer ./app.jfr ./gc.log
   ```

   The report prints GC pause statistics, allocation volume, and CPU load summaries, all with explicit tracing to aid troubleshooting.

## Runtime evidence (JFR + GC logs)

This repository expects **verbose, reproducible evidence** for every lab. The commands below show exactly how to launch each track's representative scenario, capture JFR + GC logs, and then feed them to the analyzer. All samples keep explicit file names so you can compare runs between students or CI jobs.

### Beginner (B1_gc_basics)

Collect a lightweight profile and GC log while running the GC basics lab:

```bash
java -Xms256m -Xmx256m \
     -XX:StartFlightRecording=filename=beginner-b1.jfr,dumponexit=true,settings=profile \
     -Xlog:gc*:file=beginner-b1-gc.log:uptime,time,level,tags \
     -XX:+HeapDumpOnOutOfMemoryError \
     beginner/B1_gc_basics/GcBasics
```

### Intermediate (I1_gc_tuning_g1)

Capture a tuned G1 run with correlated JFR and structured GC logging:

```bash
java -Xms512m -Xmx512m \
     -XX:MaxGCPauseMillis=200 \
     -XX:StartFlightRecording=filename=intermediate-i1.jfr,dumponexit=true,settings=profile \
     -Xlog:gc*:file=intermediate-i1-gc.log:uptime,time,level,tags \
     -XX:+HeapDumpOnOutOfMemoryError \
     intermediate/I1_gc_tuning_g1/MyServiceApp
```

### Advanced (A2_jfr_profiling)

Record a full-profile JFR with verbose GC logging for the advanced profiling lab:

```bash
java -Xms1g -Xmx1g \
     -XX:+UseZGC \
     -XX:StartFlightRecording=filename=advanced-a2.jfr,dumponexit=true,settings=profile \
     -Xlog:gc*:file=advanced-a2-gc.log:uptime,time,level,tags \
     -XX:+HeapDumpOnOutOfMemoryError \
     -Djava.util.logging.config.file=logging.properties \
     advanced/A2_jfr_profiling/MyServiceAppJfr
```

### Analyze captured evidence (all tracks)

After any of the above runs, feed the recorded artifacts to the JVM Health Analyzer. The verbose Maven build keeps progress traceable:

```bash
cd analyzer
mvn -q -DskipTests package

java --add-exports jdk.jfr/jdk.jfr.consumer=ALL-UNNAMED \
     -cp target/jvm-health-analyzer-1.0-SNAPSHOT.jar \
     com.example.jvmhealth.JvmHealthAnalyzer ./advanced-a2.jfr ./advanced-a2-gc.log
```

Swap the filenames to match the track you executed (e.g., `beginner-b1.jfr` with `beginner-b1-gc.log`). The analyzer prints allocation, pause, and CPU summaries with verbose banners so you can correlate findings with the original run.

5. **Inspect lab-specific guides**

   Each track includes a dedicated `README.md` with precise steps, JVM flags, and expected observations. Follow them sequentially (`beginner` → `intermediate` → `advanced`).

## Track Details

### Beginner – JVM Fundamentals
- **B1_gc_basics** (`GcBasics.java`): Observe allocation patterns and GC pauses at varying heap sizes with `-Xlog:gc*` for verbose GC diagnostics.
- **B2_heap_sizing** (`HeapStress.java`): Trigger `OutOfMemoryError`, capture heap dumps (`-XX:+HeapDumpOnOutOfMemoryError`), and analyze in Eclipse MAT.
- **B3_thread_states** (`ThreadStatesDemo.java`): Generate runnable, blocked, and waiting threads, then inspect states via `jstack` or `jcmd Thread.print`.

### Intermediate – Tuning & Contention
- **I1_gc_tuning_g1** (`MyServiceApp.java`): Tune G1 with `-XX:MaxGCPauseMillis`, emit structured GC logs (`-Xlog:gc*:file=gc.log`), and review pause distribution.
- **I2_memory_leak_lab** (`LeakLab.java`): Simulate a leak, capture `leak.hprof`, and locate retained references (e.g., `LEAK_MAP`) in Eclipse MAT.
- **I3_thread_dump_lock_contention** (`LockContentionLab.java`): Produce lock contention visible in thread dumps; refactor to `AtomicInteger` to compare throughput.

### Advanced – Low Latency & Production Profiling
- **A1_low_latency_gc** (`LowLatencyApp.java`): Compare ZGC vs. G1 pause behavior by reviewing `zgc.log` and `g1.log` with verbose GC tags.
- **A2_jfr_profiling** (`MyServiceAppJfr.java`): Record JFR sessions (`-XX:StartFlightRecording=...`) to locate CPU/allocation hotspots, safepoints, and GC pauses.
- **A3_container_aware_jvm** (`MyContainerApp.java`, `Dockerfile`): Package a runnable JAR and observe JVM ergonomics under container CPU/memory limits (`-XshowSettings:vm`).

## JVM Health Analyzer (analyzer/)
- **Purpose:** Consolidated reporting for JFR files and GC logs to accelerate incident triage.
- **Inputs:**
  - JFR events (counts GC pauses, allocations, CPU load).
  - GC log pauses (heuristic ms parsing for quick summaries).
- **Outputs:** A human-readable report that includes event totals, pause count/avg/max, allocation totals (MB), and average JVM CPU load. Verbose console banners delineate each section for clarity during debugging.
- **Build & Run:**
  - `mvn -q -DskipTests package` inside `analyzer/`.
  - `java --add-exports jdk.jfr/jdk.jfr.consumer=ALL-UNNAMED -cp target/jvm-health-analyzer-1.0-SNAPSHOT.jar com.example.jvmhealth.JvmHealthAnalyzer <jfr> [gc.log]`.
  - If you encounter a `release version 17 not supported` message, ensure `JAVA_HOME` points to a JDK 17+ installation; the `scripts/build_analyzer.sh` helper performs this check up front with verbose guidance.

## Automation & Scripts (all verbose)
- `scripts/compile_beginner.sh` – Runs `make all` in `beginner/` with command tracing to show each compilation step.
- `scripts/compile_intermediate.sh` – Compiles intermediate labs with `set -x` and status banners for every file.
- `scripts/compile_advanced.sh` – Builds advanced labs, echoing progress and any generated artifacts.
- `scripts/build_analyzer.sh` – Maven package build for the analyzer with verbose shell tracing.
- `scripts/ci_compile_all.sh` – Orchestrates all of the above, useful for CI pipelines or local smoke checks.

## Logging & Diagnostics Conventions
- Prefer **verbose JVM flags** (`-Xlog:gc*`, `-XshowSettings:vm`, `-XX:+HeapDumpOnOutOfMemoryError`) to capture reproducible evidence.
- Keep **artifacts** (logs, `.hprof`, `.jfr`) alongside labs for postmortem analysis.
- Use the **provided Makefiles** to ensure consistent compilation across environments while adhering to international programming standards for repeatable builds and traceable output.

## Graphical JVM Monitoring on Ubuntu WSL (VisualVM)
Use VisualVM to observe heap usage, GC activity, and thread states in real time while keeping verbose diagnostics enabled for traceability.

1. **Enable GUI support in WSL**

   - **Windows 11/WSLg**: Graphics work out of the box; no extra display server needed.
   - **Windows 10 (or WSL without WSLg)**: Install an X server such as [VcXsrv](https://sourceforge.net/projects/vcxsrv/), then set the display in your WSL shell:

     ```bash
     export DISPLAY=$(grep -m1 nameserver /etc/resolv.conf | awk '{print $2}'):0
     export LIBGL_ALWAYS_INDIRECT=1
     ```

2. **Install the viewer with verbose logging enabled**

   ```bash
   sudo apt-get update
   sudo apt-get install -y visualvm

   # Optional: turn on verbose console logging for VisualVM to aid debugging
   export VISUALVM_LOGGING_OPTS="-J-Dnetbeans.logger.console=true -J-Dorg.netbeans.Logger.level=FINE"
   ```

3. **Start your JVM with JMX + verbose evidence**

   Run any lab with GC logging, JMX, and a heap dump trigger so VisualVM can attach:

   ```bash
   java -Xms512m -Xmx512m \
        -Xlog:gc*:file=gc.log:uptime,time,level,tags \
        -Dcom.sun.management.jmxremote \
        -Dcom.sun.management.jmxremote.port=9010 \
        -Dcom.sun.management.jmxremote.authenticate=false \
        -Dcom.sun.management.jmxremote.ssl=false \
        -XX:+HeapDumpOnOutOfMemoryError \
        beginner/B1_gc_basics/GcBasics
   ```

4. **Launch VisualVM from WSL**

   Start the GUI with the verbose logging options so you can debug any connection issues:

   ```bash
   visualvm $VISUALVM_LOGGING_OPTS --jdkhome "$JAVA_HOME" &
   ```

5. **Attach to the running JVM**

   - In VisualVM, locate the running process under **Local** (or add a remote JMX connection to `localhost:9010`).
   - Open the **Monitor** and **Threads** tabs to watch heap, GC pauses, and thread states in real time; use the **Sampler** or **Profiler** for CPU/allocation views.
   - Keep `gc.log` and any JFR captures alongside your run for correlation; the verbose VisualVM console output helps align GUI observations with logged events.

## Next Steps
- Work through the tracks in order, using the verbose scripts to compile and the per-lab guides to run scenarios.
- Capture GC logs and JFR recordings during experiments, then feed them to the JVM Health Analyzer for quick summaries.
- Explore the `tips/` directory and `garbage-collection.md` to deepen your understanding of JVM performance fundamentals.
