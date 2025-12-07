# JVM Tuning Lab – Advanced Track

This track covers:

- Low-latency GC (ZGC / Shenandoah)
- Deep JFR profiling
- JVM behavior under resource limits without containers

---

## Labs

1. **A1_low_latency_gc** – Compare ZGC vs G1 pause behavior
2. **A2_jfr_profiling** – JFR-based profiling of a service
3. **A3_container_aware_jvm** – JVM resource limits without Docker

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

## Lab A3 – JVM Resource Limits without Docker

**Folder:** `A3_container_aware_jvm/`
**File:** `MyContainerApp.java`

### Run with Local Limits and Capture Evidence (outputs to repo root)

```bash
javac advanced/A3_container_aware_jvm/MyContainerApp.java

java -Xms512m -Xmx512m \
     -XX:ActiveProcessorCount=2 \
     -XX:StartFlightRecording=filename=advanced-a3.jfr,dumponexit=true,settings=profile \
     -Xlog:gc*:file=advanced-a3-gc.log:uptime,time,level,tags \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XshowSettings:vm \
     -cp advanced/A3_container_aware_jvm \
     MyContainerApp
```

> **Troubleshooting – missing `advanced-a3.jfr` or `advanced-a3-gc.log`:** both
> artifacts are written to the repository root (the same location used by the
> other labs). If the files are absent, make sure the JVM has write permissions
> in that folder and re-run the command. The app prints verbose diagnostics
> about the resolved paths so you can verify where the evidence should appear.

Observe heap sizing, CPU accounting via `-XX:ActiveProcessorCount`, and verbose
GC/JFR evidence. The JFR/GC artifacts land in the repository root for analysis
with the JVM Health Analyzer.
