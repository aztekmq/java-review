# JVM Tuning Lab – Beginner Track

This track teaches **JVM fundamentals**: heap basics, GC behavior, thread states, and basic dump analysis.

---

## Labs

1. **B1_gc_basics** – GC basics: allocations and pauses
2. **B2_heap_sizing** – Heap sizing, OutOfMemoryError, heap dumps
3. **B3_thread_states** – Thread states and thread dumps

See individual subfolders for code.

---

## Lab B1 – GC Basics: Allocations & Pauses

**Folder:** `B1_gc_basics/`  
**File:** `GcBasics.java`

### Run

```bash
cd beginner/B1_gc_basics
javac GcBasics.java
java -cp ../../ -Xms256m -Xmx256m -Xlog:gc* beginner.B1_gc_basics.GcBasics
java -cp ../../ -Xms64m -Xmx64m -Xlog:gc* beginner.B1_gc_basics.GcBasics
```

Observe:

* GC frequency and pause times in logs
* How heap size changes GC behavior

Optional: Attach **VisualVM** and watch heap usage.

---

## Lab B2 – Heap Sizing, OOM & Heap Dump

**Folder:** `B2_heap_sizing/`
**File:** `HeapStress.java`

### Run

```bash
cd beginner/B2_heap_sizing
javac HeapStress.java
java -Xms64m -Xmx64m \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=./heapstress.hprof \
     HeapStress
```

When it crashes, open `heapstress.hprof` in **Eclipse MAT** and run *Leak Suspects*.
Look at:

* Largest object types
* Dominator tree
* Retained size vs shallow size

---

## Lab B3 – Thread States & Dumps

**Folder:** `B3_thread_states/`
**File:** `ThreadStatesDemo.java`

### Run

```bash
cd beginner/B3_thread_states
javac ThreadStatesDemo.java
java ThreadStatesDemo
```

Find PID (`jps` or OS tools), then:

```bash
jstack <pid> > threads.txt
# or
jcmd <pid> Thread.print > threads.txt
```

Identify:

* RUNNABLE CPU-bound thread
* BLOCKED thread on a lock
* TIMED_WAITING thread sleeping

---

After completing this track, move on to:
`../intermediate/README.md`
