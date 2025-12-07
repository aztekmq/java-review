# JVM Tuning Lab – Advanced Track

This track covers:

- Low-latency GC (ZGC / Shenandoah)
- Deep JFR profiling
- JVM behavior in containers

---

## Labs

1. **A1_low_latency_gc** – Compare ZGC vs G1 pause behavior
2. **A2_jfr_profiling** – JFR-based profiling of a service
3. **A3_container_aware_jvm** – Running JVM in Docker with resource limits

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

## Lab A3 – JVM in Containers

**Folder:** `A3_container_aware_jvm/`
**Files:** `MyContainerApp.java`, `Dockerfile`

### Build Docker Image (verbose)

```bash
# From the repo root; emits verbose output for every build step.
scripts/build_container_image.sh
```

### Run with Limits and Capture Evidence

```bash
docker run --rm -m512m --cpus=2 -v "$(pwd)":/workspace java-review-container:latest \
  java -Xms512m -Xmx512m \
       -XX:StartFlightRecording=filename=/workspace/advanced-a3.jfr,dumponexit=true,settings=profile \
       -Xlog:gc*:file=/workspace/advanced-a3-gc.log:uptime,time,level,tags \
       -XX:+HeapDumpOnOutOfMemoryError \
       -XshowSettings:vm \
       -jar /app/MyContainerApp.jar
```

Observe heap sizing, container-aware ergonomics, and verbose GC/JFR evidence. The JFR/GC artifacts land in the current working
directory for later analysis with the JVM Health Analyzer.
