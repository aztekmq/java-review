# JVM Tuning Lab – Intermediate Track

This track focuses on **GC tuning, memory leaks, and thread contention**.

---

## Labs

1. **I1_gc_tuning_g1** – G1 GC tuning and GC log analysis
2. **I2_memory_leak_lab** – Leak simulation and heap dump analysis
3. **I3_thread_dump_lock_contention** – Lock contention with thread dumps

---

## Lab I1 – G1 GC Tuning

**Folder:** `I1_gc_tuning_g1/`  
**File:** `MyServiceApp.java`

This is a simple service that allocates memory periodically to simulate load.

### Run with G1 & GC logging

```bash
cd intermediate/I1_gc_tuning_g1
javac MyServiceApp.java

java -Xms1g -Xmx1g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -Xlog:gc*:file=gc.log:tags,uptime,time,level \
     MyServiceApp
```

Generate load by repeatedly hitting the HTTP endpoint or just let it run if using a workload loop.

Load `gc.log` into a GC viewer and evaluate:

* Pause times
* Young vs mixed GCs
* Heap usage over time

---

## Lab I2 – Memory Leak Lab

**Folder:** `I2_memory_leak_lab/`
**File:** `LeakLab.java`

### Run

```bash
cd intermediate/I2_memory_leak_lab
javac LeakLab.java
java -Xms256m -Xmx256m \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=leak.hprof \
     LeakLab
```

Open `leak.hprof` in **Eclipse MAT**, run *Leak Suspects*, and inspect `LEAK_MAP` as a root cause.

---

## Lab I3 – Thread Contention & Lock Analysis

**Folder:** `I3_thread_dump_lock_contention/`
**File:** `LockContentionLab.java`

### Run

```bash
cd intermediate/I3_thread_dump_lock_contention
javac LockContentionLab.java
java LockContentionLab
```

Take several thread dumps with `jstack <pid>` and note:

* Most threads BLOCKED on `increment()`
* Only one active at a time

Refactor to use `java.util.concurrent.atomic.AtomicInteger`, rerun, and compare runtime.

---

When comfortable here, move on to `../advanced/README.md`
