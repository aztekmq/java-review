# java-review – JVM Tuning & Diagnostics Lab (A–Z Guide)

![Java 17+](https://img.shields.io/badge/JDK-17%2B-5382a1?logo=java&logoColor=white)
![Maven 3.8+](https://img.shields.io/badge/Maven-3.8%2B-C71A36?logo=apache-maven&logoColor=white)
![CI Ready](https://img.shields.io/badge/CI-ready-success?logo=githubactions&logoColor=white)
![Verbose Logging](https://img.shields.io/badge/logging-verbose-blueviolet)
![License](https://img.shields.io/badge/license-Apache--2.0-lightgrey)

`java-review` is a **hands-on JVM tuning and diagnostics lab** that walks you through everything from first heap inspections to low-latency garbage collectors and JFR-based production forensics. It is organized into three progressive learning tracks plus a JVM Health Analyzer utility. All scripts and builds ship with verbose logging so you can trace every action for repeatable diagnostics.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Repository Layout](#repository-layout)
- [Quickstart (A–Z)](#quickstart-a–z)
- [Runtime evidence matrix (JFR + GC logs)](#runtime-evidence-matrix-jfr--gc-logs)
- [JVM Health Analyzer (analyzer/)](#jvm-health-analyzer-analyzer)
- [Automation & Scripts (all verbose)](#automation--scripts-all-verbose)
- [Logging & Diagnostics Conventions](#logging--diagnostics-conventions)
- [Graphical JVM Monitoring on Ubuntu WSL (VisualVM)](#graphical-jvm-monitoring-on-ubuntu-wsl-visualvm)
- [Appendix – Detailed Lab Explanations (Beginner to Advanced)](#appendix--detailed-lab-explanations-beginner-to-advanced)

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

   - Beginner example: `java -Xms256m -Xmx256m -Xlog:gc* -cp beginner/B1_gc_basics GcBasics`
   - Intermediate example: `java -Xlog:gc*:file=gc.log -XX:+UseG1GC -cp intermediate/I1_gc_tuning_g1 MyServiceApp`
   - Advanced example: `java -XX:+UseZGC -Xlog:gc*:file=zgc.log -cp advanced/A1_low_latency_gc LowLatencyApp`

   *Note:* the labs deliberately use the default package so the **class name on the command line is just the file name** (for example, `HeapStress`), while the `-cp` option points to the folder containing that class file.

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

## Runtime evidence matrix (JFR + GC logs)

Every lab ships with verbose, reproducible commands so you can launch the scenario and capture JFR + GC logs for the JVM Health Analyzer. Use the table below to locate the correct invocation per track, keeping explicit artifact names for side-by-side comparison in classrooms or CI jobs.

| Lab case | Runtime evidence capture (JFR + GC logs) |
| --- | --- |
| Beginner – GC Basics (`B1_gc_basics`) – *intent:* see how a tiny heap triggers frequent collections and how verbose GC logging records each pause. | <code>java -Xms256m -Xmx256m -XX:StartFlightRecording=filename=beginner-b1.jfr,dumponexit=true,settings=profile -Xlog:gc*:file=beginner-b1-gc.log:uptime,time,level,tags -XX:+HeapDumpOnOutOfMemoryError -cp beginner/B1_gc_basics GcBasics</code> |
| Beginner – Heap Sizing (`B2_heap_sizing`) – *intent:* deliberately exhaust memory to learn how heap size and dumps capture an `OutOfMemoryError` event. | <code>java -Xms256m -Xmx256m -XX:+HeapDumpOnOutOfMemoryError -XX:StartFlightRecording=filename=beginner-b2.jfr,dumponexit=true,settings=profile -Xlog:gc*:file=beginner-b2-gc.log:uptime,time,level,tags -cp beginner/B2_heap_sizing HeapStress</code> |
| Beginner – Thread States (`B3_thread_states`) – *intent:* watch threads move between runnable, blocked, and waiting so you can map log output to `jstack` thread-state names. | <code> java -Xms256m -Xmx256m -XX:StartFlightRecording=filename=beginner-b3.jfr,dumponexit=true,settings=profile -Xlog:gc*:file=beginner-b3-gc.log:uptime,time,level,tags -cp beginner/B3_thread_states ThreadStatesDemo</code> |
| Intermediate – G1 Tuning (`I1_gc_tuning_g1`) – *intent:* practice setting pause targets and reading G1 log entries to see how the collector reacts. | <code>java -Xms512m -Xmx512m -XX:MaxGCPauseMillis=200 -XX:StartFlightRecording=filename=intermediate-i1.jfr,dumponexit=true,settings=profile -Xlog:gc*:file=intermediate-i1-gc.log:uptime,time,level,tags -XX:+HeapDumpOnOutOfMemoryError -cp intermediate/I1_gc_tuning_g1 MyServiceApp</code> |
| Intermediate – Memory Leak Lab (`I2_memory_leak_lab`) – *intent:* create a controlled leak so beginners can practice capturing heap dumps and spotting retaining objects. | <code>java -Xms512m -Xmx512m -XX:+HeapDumpOnOutOfMemoryError -XX:StartFlightRecording=filename=intermediate-i2.jfr,dumponexit=true,settings=profile -Xlog:gc*:file=intermediate-i2-gc.log:uptime,time,level,tags -cp intermediate/I2_memory_leak_lab LeakLab</code> |
| Intermediate – Lock Contention (`I3_thread_dump_lock_contention`) – *intent:* trigger threads fighting over a lock so you can tie thread dump stacks to poor throughput. | <code>java -Xms512m -Xmx512m -XX:StartFlightRecording=filename=intermediate-i3.jfr,dumponexit=true,settings=profile -Xlog:gc*:file=intermediate-i3-gc.log:uptime,time,level,tags -cp intermediate/I3_thread_dump_lock_contention LockContentionLab</code> |
| Advanced – Low Latency GC (`A1_low_latency_gc`) – *intent:* compare pause times between ZGC and other collectors to understand low-latency trade-offs. | <code>java -Xms1g -Xmx1g -XX:+UseZGC -XX:StartFlightRecording=filename=advanced-a1.jfr,dumponexit=true,settings=profile -Xlog:gc*:file=advanced-a1-gc.log:uptime,time,level,tags -XX:+HeapDumpOnOutOfMemoryError -cp advanced/A1_low_latency_gc LowLatencyApp</code> |
| Advanced – JFR Profiling (`A2_jfr_profiling`) – *intent:* capture a profiling JFR to identify CPU and allocation hot spots with verbose logging enabled. | <code>java -Xms1g -Xmx1g -XX:+UseZGC -XX:StartFlightRecording=filename=advanced-a2.jfr,dumponexit=true,settings=profile -Xlog:gc*:file=advanced-a2-gc.log:uptime,time,level,tags -XX:+HeapDumpOnOutOfMemoryError -Djava.util.logging.config.file=logging.properties -cp advanced/A2_jfr_profiling MyServiceAppJfr</code> |
| Advanced – Container-Aware JVM (`A3_container_aware_jvm`) – *intent:* observe how JVM ergonomics change under container CPU and memory limits with verbose evidence. | <code>docker run --rm -m512m --cpus=2 -v "$(pwd)":/workspace java-review-container:latest java -Xms512m -Xmx512m -XX:StartFlightRecording=filename=/workspace/advanced-a3.jfr,dumponexit=true,settings=profile -Xlog:gc*:file=/workspace/advanced-a3-gc.log:uptime,time,level,tags -XX:+HeapDumpOnOutOfMemoryError -XshowSettings:vm -cp /workspace/advanced/A3_container_aware_jvm MyContainerApp</code> |

After capturing the artifacts for any lab, use the JVM Health Analyzer with verbose Maven output and explicit artifact names. Each scenario below aligns with the runtime evidence table so you can trace diagnostics end-to-end following international programming standards:

| Lab case | JFR artifact | GC log artifact | Analyzer invocation (verbose build + run) |
| --- | --- | --- | --- |
| Beginner – GC Basics (`B1_gc_basics`) – *purpose:* translate the frequent GC pauses you saw in the run command into a human-readable report. | `beginner-b1.jfr` | `beginner-b1-gc.log` | <code>cd analyzer && mvn -DskipTests -X package && java --add-exports jdk.jfr/jdk.jfr.consumer=ALL-UNNAMED -cp target/jvm-health-analyzer-1.0-SNAPSHOT.jar com.example.jvmhealth.JvmHealthAnalyzer ../beginner-b1.jfr ../beginner-b1-gc.log</code> |
| Beginner – Heap Sizing (`B2_heap_sizing`) – *purpose:* convert the crash evidence from an `OutOfMemoryError` into clear leak suspects and pause stats. | `beginner-b2.jfr` | `beginner-b2-gc.log` | <code>cd analyzer && mvn -DskipTests -X package && java --add-exports jdk.jfr/jdk.jfr.consumer=ALL-UNNAMED -cp target/jvm-health-analyzer-1.0-SNAPSHOT.jar com.example.jvmhealth.JvmHealthAnalyzer ../beginner-b2.jfr ../beginner-b2-gc.log</code> |
| Beginner – Thread States (`B3_thread_states`) – *purpose:* summarize thread blocks/waits from the long-running demo into counts and durations beginners can read. | `beginner-b3.jfr` | `beginner-b3-gc.log` | <code>cd analyzer && mvn -DskipTests -X package && java --add-exports jdk.jfr/jdk.jfr.consumer=ALL-UNNAMED -cp target/jvm-health-analyzer-1.0-SNAPSHOT.jar com.example.jvmhealth.JvmHealthAnalyzer ../beginner-b3.jfr ../beginner-b3-gc.log</code> |
| Intermediate – G1 Tuning (`I1_gc_tuning_g1`) – *purpose:* relate the tuned pause target to actual pause statistics to validate the setting. | `intermediate-i1.jfr` | `intermediate-i1-gc.log` | <code>cd analyzer && mvn -DskipTests -X package && java --add-exports jdk.jfr/jdk.jfr.consumer=ALL-UNNAMED -cp target/jvm-health-analyzer-1.0-SNAPSHOT.jar com.example.jvmhealth.JvmHealthAnalyzer ../intermediate-i1.jfr ../intermediate-i1-gc.log</code> |
| Intermediate – Memory Leak Lab (`I2_memory_leak_lab`) – *purpose:* convert leak-driven heap growth into a report that highlights allocation and pause pressure. | `intermediate-i2.jfr` | `intermediate-i2-gc.log` | <code>cd analyzer && mvn -DskipTests -X package && java --add-exports jdk.jfr/jdk.jfr.consumer=ALL-UNNAMED -cp target/jvm-health-analyzer-1.0-SNAPSHOT.jar com.example.jvmhealth.JvmHealthAnalyzer ../intermediate-i2.jfr ../intermediate-i2-gc.log</code> |
| Intermediate – Lock Contention (`I3_thread_dump_lock_contention`) – *purpose:* distill the contention run into metrics that highlight blocked time per thread. | `intermediate-i3.jfr` | `intermediate-i3-gc.log` | <code>cd analyzer && mvn -DskipTests -X package && java --add-exports jdk.jfr/jdk.jfr.consumer=ALL-UNNAMED -cp target/jvm-health-analyzer-1.0-SNAPSHOT.jar com.example.jvmhealth.JvmHealthAnalyzer ../intermediate-i3.jfr ../intermediate-i3-gc.log</code> |
| Advanced – Low Latency GC (`A1_low_latency_gc`) | `advanced-a1.jfr` | `advanced-a1-gc.log` | <code>cd analyzer && mvn -DskipTests -X package && java --add-exports jdk.jfr/jdk.jfr.consumer=ALL-UNNAMED -cp target/jvm-health-analyzer-1.0-SNAPSHOT.jar com.example.jvmhealth.JvmHealthAnalyzer ../advanced-a1.jfr ../advanced-a1-gc.log</code> |
| Advanced – JFR Profiling (`A2_jfr_profiling`) | `advanced-a2.jfr` | `advanced-a2-gc.log` | <code>cd analyzer && mvn -DskipTests -X package && java --add-exports jdk.jfr/jdk.jfr.consumer=ALL-UNNAMED -cp target/jvm-health-analyzer-1.0-SNAPSHOT.jar com.example.jvmhealth.JvmHealthAnalyzer ../advanced-a2.jfr ../advanced-a2-gc.log</code> |
| Advanced – Container-Aware JVM (`A3_container_aware_jvm`) | `advanced-a3.jfr` | `advanced-a3-gc.log` | <code>cd analyzer && mvn -DskipTests -X package && java --add-exports jdk.jfr/jdk.jfr.consumer=ALL-UNNAMED -cp target/jvm-health-analyzer-1.0-SNAPSHOT.jar com.example.jvmhealth.JvmHealthAnalyzer ../advanced-a3.jfr ../advanced-a3-gc.log</code> |

The report prints allocation, pause, and CPU summaries with verbose banners so you can correlate findings with the original run while preserving transparent logging for debugging.

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
        -cp beginner/B1_gc_basics \
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


## Appendix – Detailed Lab Explanations (Beginner to Advanced)

Below is a detailed, novice-friendly walkthrough for the third beginner lab (`B3_thread_states`). The program is intentionally quiet because the threads are designed to sit in `RUNNABLE`, `TIMED_WAITING`, and `BLOCKED` states for observation—so “hanging” is expected. The commands include verbose logging (JFR + GC) so you get rich artifacts to inspect later.

### What the program actually does

Starts four threads:

- **busy-thread**: loops forever, keeping the CPU busy (`RUNNABLE`).
- **sleeping-thread**: repeatedly sleeps 10 seconds (`TIMED_WAITING`).
- **locker-1**: acquires a lock, prints a message, then holds it for 60 seconds (`RUNNABLE` while holding the monitor).
- **locker-2**: tries to take the same lock and blocks until `locker-1` releases it (`BLOCKED`).

The main thread sleeps for 2 minutes, keeping the JVM alive so you can attach tools (e.g., `jstack`, Mission Control).

Because the threads intentionally wait or loop, you will not see frequent console output after the initial lock messages. This is normal and follows international programming standards for deterministic diagnostics.

### One-terminal quick start (minimal steps)

Open a terminal and ensure you are in the repo root (`/workspace/java-review`).

Run with verbose logging and JFR (copy/paste the whole command):

```bash
java -Xms256m -Xmx256m \
  -XX:StartFlightRecording=filename=beginner-b3.jfr,dumponexit=true,settings=profile \
  -Xlog:gc*:file=beginner-b3-gc.log:uptime,time,level,tags \
  -cp beginner/B3_thread_states ThreadStatesDemo
```

- JFR recording goes to `beginner-b3.jfr` automatically on exit because of `dumponexit=true`.
- GC verbose log streams to `beginner-b3-gc.log` with timestamps and tags for easier debugging.

What you will see: a couple of lines like “locker-1 acquired lock…” and “locker-2 got the lock” once the monitor contention happens. After that the process idles so you can inspect it.

Let it run for up to two minutes (or longer if you want). The threads will keep their states steady.

Stop it cleanly with `Ctrl+C`. This flushes both the JFR and GC log files to disk—no extra commands needed.

### Two-terminal workflow (recommended for observing “live”)

Terminal A (run the program): start it with the command above. Leave it running.

Terminal B (observe):

- Run `jps` to see the Java process ID.
- Run `jstack <pid>` to view thread states; you should see `RUNNABLE`, `TIMED_WAITING`, and `BLOCKED` threads as described above.
- Tail the GC log while it runs: `tail -f beginner-b3-gc.log`.

When finished, return to Terminal A and press `Ctrl+C` to finalize the JFR file.

### Where to find your artifacts

- `beginner-b3.jfr` — ready to open in JDK Mission Control or any JFR viewer.
- `beginner-b3-gc.log` — unified GC logging with timestamps and tags for correlation with JFR.

Both files are written to whatever directory you launched the command from (use the repo root to match the lab docs).

### Why it “hangs” and what to expect

The “hang” is intentional; the demo is built so threads sit in specific states for inspection. There is no continuous console output after the initial lock messages.

If you need proof of activity, use `jstack` or a profiler/Mission Control session; the busy loop and sleeping threads will show up immediately.

### Common pitfalls to avoid

- **Running in the wrong directory:** start the command from `/workspace/java-review` so the classpath and output paths are correct.
- **Expecting automatic termination:** the main thread sleeps for 2 minutes; you must stop it with `Ctrl+C` if you are done early.
- **Missing artifacts:** if you kill the shell abruptly (not with `Ctrl+C`), you might interrupt flushing; `dumponexit=true` protects against most cases, but a hard kill could still lose data.

### If you want extra verbosity

- The provided `-Xlog:gc*` already enables verbose GC logging with timestamps and tags for easy debugging.
- To add JVM diagnostic output, you can extend the `-Xlog` selectors (e.g., `-Xlog:gc*,safepoint,classhisto=info:file=beginner-b3-gc.log:uptime,time,level,tags`)—but the default flags are already suitable for a first pass.

You now have a clear, step-by-step way to run the lab, capture the JFR/GC artifacts, and verify that the threads are in the intended states with transparent, verbose logging for compliance with international programming standards.


