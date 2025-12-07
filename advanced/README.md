# JVM Tuning Lab – Advanced Track

This track covers:

- Low-latency GC (ZGC / Shenandoah)
- Deep JFR profiling

---

## Labs

1. **A1_low_latency_gc** – Compare ZGC vs G1 pause behavior
2. **A2_jfr_profiling** – JFR-based profiling of a service
3. **A3_async_profiler** – CPU + allocation flame graphs with async-profiler

---

## Lab A1 – Low-Latency GC (ZGC)

**Folder:** `A1_low_latency_gc/`  
**File:** `LowLatencyApp.java`

### Run with ZGC

```bash
cd advanced/A1_low_latency_gc
javac LowLatencyApp.java

java -Xms4g -Xmx4g \
     -XX:+UseZGC \
     -Xlog:gc*:file=zgc.log \
     LowLatencyApp
```

### Run with G1

```bash
java -Xms4g -Xmx4g \
     -XX:+UseG1GC \
     -Xlog:gc*:file=g1.log \
     LowLatencyApp
```

Compare pause times and throughput in the GC logs.

---

## Lab A2 – JFR Profiling

**Folder:** `A2_jfr_profiling/`
**File:** `MyServiceAppJfr.java`

### Run with JFR

```bash
cd advanced/A2_jfr_profiling
javac MyServiceAppJfr.java

java -XX:StartFlightRecording=name=MyApp,settings=profile,duration=120s,filename=myapp.jfr \
     MyServiceAppJfr
```

Open `myapp.jfr` in **Java Mission Control** and inspect:

* CPU hotspots
* Allocation hotspots
* GC pauses
* Safepoints

---

## Lab A3 – Async-Profiler Flame Graphs

**Folder:** `A3_async_profiler/`
**File:** `AsyncProfilerLab.java`

### Run with verbose GC logging and profiling-ready flags

```bash
cd advanced/A3_async_profiler
javac AsyncProfilerLab.java

java -Xms2g -Xmx2g \
     -Xlog:gc*:file=async-profiler-gc.log:tags,uptime,time,level \
     -XX:StartFlightRecording=name=async-profiler,settings=profile,dumponexit=true,filename=async-profiler.jfr \
     AsyncProfilerLab
```

Keep the process running and attach **async-profiler** once the banner appears:

```bash
# CPU flame graph (requires async-profiler's profiler.sh on PATH or $ASYNC_PROFILER_HOME)
profiler.sh -d 30 -e cpu -f cpu.svg <pid>

# Allocation flame graph
profiler.sh -d 30 -e alloc -f alloc.svg <pid>
```

Inspect `cpu.svg` and `alloc.svg` in a browser. The lab emits verbose lifecycle logs (Level.FINEST) so you can correlate cycles
to the captured samples and validate international programming standards for reproducible diagnostics.

---

