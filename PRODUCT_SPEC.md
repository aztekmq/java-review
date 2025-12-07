# Project Spec – java-review

> SINGLE SOURCE OF TRUTH for what this repo is, does, and must never break.
> **CONTEXT:** This repository is designed to be the definitive hands-on lab environment for learning advanced JVM tuning and diagnostics, specifically leveraging commercial-grade features similar to those found in JvmHealthAnalyzer Java Profiler (e.g., deep memory leak detection, detailed thread contention analysis, and low-overhead profiling).

---

## 1. Overview

**Project name:** `java-review`  
**Purpose:** Hands-on JVM tuning & diagnostics lab with three difficulty levels (beginner, intermediate, advanced) plus a JVM health analyzer tool, focusing on **production-ready profiling scenarios**.

**Primary goals:**
- Provide runnable Java examples that illustrate:
  - **Low-overhead data collection** techniques suitable for production (e.g., JFR, low-impact sampling).
  - Advanced heap analysis (retained size, memory leak root finding).
  - Complex thread synchronization issues (deadlocks, lock contention).
  - **Remote-friendly** JVM configuration for profiling in container environments.
- Provide a JVM health analysis CLI that can summarize JFR + GC logs into a “JVM Health Report”.

**Non-goals (for now):**
- No production-grade web UI.
- No distributed / clustered orchestration.
- No multi-language support beyond Java and a bit of shell/Make/Maven.

---

## 2. Repository Structure (High-Level)

- `beginner/`
  - B1 – GC basics & Telemetry
  - B2 – Heap sizing & OOM + basic heap dumps
  - B3 – Thread states & basic thread dumps
- `intermediate/`
  - I1 – G1 GC tuning & GC logs
  - I2 – **Memory leak lab (Retained Size analysis)**
  - I3 – **Advanced Lock Contention lab (Deadlocks)**
- `advanced/`
  - A1 – Low-latency GC (ZGC vs G1)
  - A2 – JFR profiling & Hotspot analysis (Flame Graph source)
  - A3 – Container-aware JVM & **Remote Profiling Setup (Simulated)**
  - A4 – **I/O and Database Contention lab (Simulated)**
- `analyzer/`
  - Maven project with `JvmHealthAnalyzer`:
    - Input: JFR file (+ optional GC log)
    - Output: text-based JVM Health Report

---

## 3. Cross-Cutting Invariants (MUST ALWAYS HOLD)

These are the things that **AI tools must not break**:

1. **Labs are self-contained and runnable.**
   - Each lab folder (`B1_gc_basics`, `I2_memory_leak_lab`, etc.) must compile independently.
   - All Java examples must compile with `javac` on **JDK 17+** without extra dependencies.

2. **Code and README must stay in sync.**
   - Every code sample mentioned in a README must exist and match filenames exactly.
   - If code changes substantially, the README for that lab must be updated in the same change.

3. **Analyzer module behavior contract (Advanced Diagnostics):**
   - `JvmHealthAnalyzer`:
     - Takes at least one argument: `JFR` path.
     - Optional second argument: GC log path.
     - **CPU Summary (Must Always Print if JFR present):**
       - Total CPU time (user/system) and maximum CPU usage observed.
       - **Top 5 methods by self-time/execution time (Simulating Hotspots/Flame Graph Data).**
     - **Memory Summary (Must Always Print if JFR present):**
       - Total JFR events and Total Allocation volume.
       - GC pause stats (count, total, avg, max).
       - **Top 5 classes by total allocated bytes (Simulating Allocation Recording).**
     - **Concurrency Summary (Must Always Print if JFR present):**
       - Total threads and maximum concurrent running threads.
       - **Deadlock Count (if $>0$).**
       - **Top 5 monitors/locks by contention time (Simulating Lock Contention Analysis).**
   - Future changes must not remove these basic outputs.

4. **Remote/Container Readiness:**
   - The `advanced/A3` lab must contain the necessary configuration (e.g., Dockerfile snippet) to simulate running a profiler agent **remotely** via a specific port or mechanism.

5. **Simple build flow:**
   - Top-level `Makefile` must continue to support:
     - `make beginner`
     - `make intermediate`
     - `make advanced`
     - `make analyzer`

---

## 4. Module-Level Responsibilities

### 4.1 Beginner Labs

- **B1_gc_basics**
  - Show impact of heap size on GC frequency and pause times, visible through basic **telemetry** output.
- **B2_heap_sizing**
  - Reproduce OutOfMemoryError.
  - Generate heap dump via `-XX:+HeapDumpOnOutOfMemoryError`.
  - README explains how to open and inspect dump in VisualVM/MAT (or similar profiler).
- **B3_thread_states**
  - Demonstrate all major thread states, including a simple **BLOCKED** scenario.
  - README shows how to generate and read thread dumps.

### 4.2 Intermediate Labs

- **I1_gc_tuning_g1**
  - Demonstrate G1 GC with GC logs.
  - Show how to interpret pauses and heap behavior.
- **I2_memory_leak_lab**
  - Simulate a static map memory leak.
  - **Focus on analyzing Retained Size to find the GC root,** explaining the "path to GC root" concept used by advanced profilers.
- **I3_thread_dump_lock_contention**
  - Simulate **deadlock** with two synchronized methods.
  - Show how profilers/thread dumps identify the threads and the specific locks involved (like JvmHealthAnalyzer's Deadlock Detection).

### 4.3 Advanced Labs

- **A1_low_latency_gc**
  - Compare ZGC vs G1 using the same workload, focusing on **minimum and maximum pause times**.
- **A2_jfr_profiling**
  - Start JFR recording from JVM startup.
  - Generate data to identify **Allocation Hotspots** and CPU-intensive call paths, as visualized by Flame Graphs.
- **A3_container_aware_jvm**
  - Show how JVM reacts to container memory/CPU limits.
  - Include documentation on how to set up the JVM for **remote profiling in a container** (e.g., `-agentpath:` config).
- **A4_io_and_db_contention**
  - Simulate threads waiting on external resources (I/O, database mock).
  - Show how to use thread profiling to differentiate between **CPU-bound (RUNNABLE)** and **I/O-bound (WAITING/BLOCKED)** time, a core feature of advanced profilers.

### 4.4 Analyzer

- **analyzer/JvmHealthAnalyzer**
  - Read JFR events and produce a textual summary:
    - **CPU Analysis:** Top methods, max CPU usage.
    - **Memory Analysis:** GC pause stats, Allocation Volume, and Top allocators.
    - **Concurrency Analysis:** Deadlock count and top contention points.
  - Optionally parse GC log for simple pause stats.
  - **Output must be a multi-section, detailed report usable as a quick "JVM Health Report" in CLI environments, reflecting JvmHealthAnalyzer-level deep diagnostics.**

---

## 5. How to Work With AI/LLMs on This Project

Whenever using ChatGPT / Codex / Copilot Chat:

1. **Always remind the AI of invariants (Section 3).**
2. **If changing any code in a lab folder, update the corresponding README.**
3. **Never remove core behaviors listed in Section 4 without explicitly stating why.**
4. For bigger refactors, ask:
   > “Before coding, restate the responsibilities from Section 4 that must be preserved, especially the **Retained Size** and **Deadlock** demonstration goals.”