# java-review – JVM Tuning & Diagnostics Lab

This repository is a **hands-on JVM tuning and diagnostics lab** organized into three levels:

- `beginner/` – JVM fundamentals (heap, GC basics, thread states, dumps)
- `intermediate/` – GC tuning, memory leaks, thread contention
- `advanced/` – ZGC/low-latency, JFR profiling, containers

There is also an `analyzer/` module that contains a **JVM Health Analyzer** tool which reads:
- Java Flight Recorder (JFR) files
- GC logs

…and outputs a simple **JVM Health Report**.

## How to Use

1. Clone this repo:

   ```bash
   git clone https://github.com/<your-user>/java-review.git
   cd java-review
   ```

2. Pick a level:

   * Start with `beginner/README.md`
   * Then `intermediate/README.md`
   * Finally `advanced/README.md`

3. Compile and run examples:

   ```bash
   cd beginner/B1_gc_basics
   javac GcBasics.java
   java -Xms256m -Xmx256m -Xlog:gc* GcBasics
   ```

4. Build and run the analyzer:

   ```bash
   cd analyzer
   mvn package
   # Example usage:
   java --add-exports jdk.jfr/jdk.jfr.consumer=ALL-UNNAMED \
        -cp target/jvm-health-analyzer-1.0-SNAPSHOT.jar \
        com.example.jvmhealth.JvmHealthAnalyzer /path/to/app.jfr /path/to/gc.log
   ```

See each subfolder’s `README.md` for detailed lab instructions.
