# JVM Tuning Lab – Advanced Track

This track covers:

- Low-latency GC (ZGC / Shenandoah)
- Deep JFR profiling

---

## Labs

1. **A1_low_latency_gc** – Compare ZGC vs G1 pause behavior
2. **A2_jfr_profiling** – JFR-based profiling of a service

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

