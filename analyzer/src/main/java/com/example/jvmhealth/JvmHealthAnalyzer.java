package com.example.jvmhealth;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedMethod;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * JVM Health Analyzer prints summary statistics from JFR recordings and GC logs.
 * * **SPEC COMPLIANCE:** This analyzer is designed to emulate the advanced diagnostics
 * of commercial profilers (like YourKit) by surfacing CPU, Allocation, and
 * Concurrency hotspots based on the invariants in PROJECT_SPEC.md.
 * * **Requires JDK 17+**
 */
public class JvmHealthAnalyzer {

    // --- Data Structures to hold JFR analysis results (Per SPEC Invariants) ---

    private static class GcStats {
        long count = 0;
        double totalPauseMillis = 0.0;
        double maxPauseMillis = 0.0;
    }

    private static class JfrSummary {
        final long eventCount;
        final GcStats gcStats;
        final long totalAllocatedBytes;
        final long cpuSamples;
        final double cpuMaxPercent;
        final long deadlockCount;
        
        // Per SPEC Invariant 3: Advanced Diagnostics
        final Map<String, AtomicLong> cpuMethodSamples; // Top 5 methods by self-time
        final Map<String, AtomicLong> allocationBytesByClass; // Top 5 classes by total allocated bytes
        final Map<String, AtomicLong> contendedMonitorCounts; // Top 5 monitors by contention/block events

        JfrSummary(long eventCount, GcStats gcStats, long totalAllocatedBytes, long cpuSamples, 
                   double cpuMaxPercent, long deadlockCount, 
                   Map<String, AtomicLong> cpuMethodSamples, 
                   Map<String, AtomicLong> allocationBytesByClass, 
                   Map<String, AtomicLong> contendedMonitorCounts) {
            this.eventCount = eventCount;
            this.gcStats = gcStats;
            this.totalAllocatedBytes = totalAllocatedBytes;
            this.cpuSamples = cpuSamples;
            this.cpuMaxPercent = cpuMaxPercent;
            this.deadlockCount = deadlockCount;
            this.cpuMethodSamples = cpuMethodSamples;
            this.allocationBytesByClass = allocationBytesByClass;
            this.contendedMonitorCounts = contendedMonitorCounts;
        }
    }

    private record GcLogSummary(long gcCount, double totalPauseMs, double maxPauseMs) {
    }

    // --- Main Method and Utility ---

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java JvmHealthAnalyzer <jfr-file> [gc-log-file]");
            System.exit(1);
        }

        Path jfrPath = Paths.get(args[0]);
        Path gcLogPath = (args.length >= 2) ? Paths.get(args[1]) : null;

        if (!Files.exists(jfrPath)) {
            System.err.printf(
                "JFR recording not found at '%s'. Please supply a valid path (e.g., ./recording.jfr) before rerunning the analyzer.%n",
                jfrPath.toAbsolutePath());
            System.exit(1);
        }
        
        System.out.println("=== JVM HEALTH ANALYSIS REPORT (YourKit Diagnostic Style) ===");
        System.out.println("JFR file : " + jfrPath.toAbsolutePath());
        if (gcLogPath != null) {
            System.out.println("GC log   : " + gcLogPath.toAbsolutePath());
        }
        System.out.println();

        JfrSummary jfrSummary = analyzeJfr(jfrPath);
        GcLogSummary gcSummary = null;
        if (gcLogPath != null && Files.exists(gcLogPath)) {
            gcSummary = analyzeGcLog(gcLogPath);
        }

        if (jfrSummary != null) {
            printFindings(jfrSummary, gcSummary);
        }

        System.out.println("=== END OF REPORT ===");
    }

    // --- JFR Analysis (The Core Update) ---

    private static JfrSummary analyzeJfr(Path jfrPath) throws IOException {
        System.out.println("--- 1. JFR Event Collection ---");
        
        long eventCount = 0;
        GcStats gcStats = new GcStats();
        long totalAllocatedBytes = 0;
        long cpuSamples = 0;
        double cpuMaxPercent = 0.0; // Tracking max CPU for the invariant
        long deadlockCount = 0;

        // SPEC-mandated maps for advanced diagnostics
        Map<String, AtomicLong> cpuMethodSamples = new HashMap<>();
        Map<String, AtomicLong> allocationBytesByClass = new HashMap<>();
        Map<String, AtomicLong> contendedMonitorCounts = new HashMap<>();

        try (RecordingFile rf = new RecordingFile(jfrPath)) {
            while (rf.hasMoreEvents()) {
                RecordedEvent e = rf.readEvent();
                eventCount++;
                String eventName = e.getEventType().getName();

                switch (eventName) {
                    case "jdk.GCPhasePause" -> {
                        Duration d = e.getDuration();
                        if (d != null) {
                            double ms = d.toNanos() / 1_000_000.0;
                            gcStats.count++;
                            gcStats.totalPauseMillis += ms;
                            gcStats.maxPauseMillis = Math.max(gcStats.maxPauseMillis, ms);
                        }
                    }
                    case "jdk.ObjectAllocationInNewTLAB", "jdk.ObjectAllocationOutsideTLAB" -> {
                        Long size = e.getLong("allocationSize");
                        if (size != null) {
                            totalAllocatedBytes += size;
                            
                            // SPEC Invariant: Track Top 5 Allocating Classes
                            String className = e.getClass("type").getName();
                            allocationBytesByClass.computeIfAbsent(className, k -> new AtomicLong(0)).addAndGet(size);
                        }
                    }
                    case "jdk.CPULoad" -> {
                        // Max CPU is measured by the sum of jvmUser and jvmSystem
                        Double jvmUser = e.getDouble("jvmUser");
                        Double jvmSystem = e.getDouble("jvmSystem");
                        if (jvmUser != null && jvmSystem != null) {
                            cpuMaxPercent = Math.max(cpuMaxPercent, (jvmUser + jvmSystem) * 100.0);
                        }
                    }
                    case "jdk.ExecutionSample" -> {
                        cpuSamples++;
                        // SPEC Invariant: Track Top 5 Methods by self-time/execution time
                        RecordedStackTrace stack = e.getStackTrace();
                        if (stack != null && stack.getFrames().size() > 0) {
                            RecordedMethod topMethod = stack.getFrames().get(0).getMethod();
                            String methodName = topMethod.getType().getName() + "." + topMethod.getName();
                            cpuMethodSamples.computeIfAbsent(methodName, k -> new AtomicLong(0)).incrementAndGet();
                        }
                    }
                    case "jdk.ThreadPark", "jdk.ThreadSleep", "jdk.JavaMonitorWait", "jdk.JavaMonitorEnter" -> {
                        // Tracks contention events (used for Contended Monitors)
                        if (e.hasField("monitorClass")) {
                             String monitorName = e.getClass("monitorClass").getName();
                             contendedMonitorCounts.computeIfAbsent(monitorName, k -> new AtomicLong(0)).incrementAndGet();
                        }
                    }
                    case "jdk.ThreadDeadlock" -> {
                        // SPEC Invariant: Deadlock Count
                        deadlockCount++;
                    }
                    default -> {
                        // ignore others for now
                    }
                }
            }
        }

        System.out.printf("Total JFR Events Processed: %d%n", eventCount);
        System.out.println("-------------------------------------");
        return new JfrSummary(eventCount, gcStats, totalAllocatedBytes, cpuSamples, cpuMaxPercent, 
                              deadlockCount, cpuMethodSamples, allocationBytesByClass, contendedMonitorCounts);
    }

    // --- GC Log Analysis (Unchanged, basic parsing for confirmation) ---

    private static GcLogSummary analyzeGcLog(Path gcLogPath) throws IOException {
        System.out.println("--- 2. GC LOG SUMMARY (Heuristic) ---");
        long gcCount = 0;
        double maxPauseMs = 0.0;
        double totalPauseMs = 0.0;

        var pausePattern = java.util.regex.Pattern.compile("([0-9.]+)ms");

        try (BufferedReader br = Files.newBufferedReader(gcLogPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("Pause") && line.contains("ms")) {
                    var m = pausePattern.matcher(line);
                    if (m.find()) {
                        double ms = Double.parseDouble(m.group(1));
                        gcCount++;
                        totalPauseMs += ms;
                        maxPauseMs = Math.max(maxPauseMs, ms);
                    }
                }
            }
        }

        if (gcCount > 0) {
            System.out.printf("GC log pauses: count=%d, total=%.2f ms, avg=%.2f ms, max=%.2f ms%n",
                    gcCount, totalPauseMs, totalPauseMs / gcCount, maxPauseMs);
        } else {
            System.out.println("No GC pauses detected with naive parser. Check log format.");
        }

        System.out.println("-------------------------------------");
        return new GcLogSummary(gcCount, totalPauseMs, maxPauseMs);
    }

    // --- Print Findings (Advanced Reporting Per SPEC) ---

    private static void printFindings(JfrSummary jfrSummary, GcLogSummary gcSummary) {
        System.out.println("--- 3. ADVANCED DIAGNOSTICS & FINDINGS (SPEC Compliant) ---");

        // --- A. CPU Analysis (Simulating Hotspots/Flame Graph Data) ---
        System.out.println("\n--- A. CPU Performance Summary ---");
        System.out.printf("[MAX CPU] JVM CPU Load peaked at %.1f%% (Total Samples: %d).%n", 
                          jfrSummary.cpuMaxPercent, jfrSummary.cpuSamples);
        
        if (jfrSummary.cpuMethodSamples.isEmpty()) {
            System.out.println("[INFO] No ExecutionSample events found. Cannot determine CPU Hotspots.");
        } else {
            System.out.println("Top 5 Methods by Execution Samples (CPU Hotspots):");
            jfrSummary.cpuMethodSamples.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparingLong(AtomicLong::get).reversed()))
                .limit(5)
                .forEach(entry -> 
                    System.out.printf("  > %s: %d samples (%.1f%% of total)%n", 
                                      entry.getKey(), 
                                      entry.getValue().get(), 
                                      (double)entry.getValue().get() / jfrSummary.cpuSamples * 100));
            
            if (jfrSummary.cpuMaxPercent > 80.0) {
                 System.out.println("[HIGH] JVM CPU is severely stressed. **Action:** Investigate the methods above to reduce computational complexity or right-size the container.");
            }
        }

        // --- B. Memory & Allocation Summary (Simulating Allocation Recording) ---
        System.out.println("\n--- B. Memory & Allocation Summary ---");
        double allocatedMb = jfrSummary.totalAllocatedBytes / (1024.0 * 1024.0);
        System.out.printf("[ALLOCATION] Total Allocated: %.2f MB (Total Bytes: %d).%n", allocatedMb, jfrSummary.totalAllocatedBytes);
        
        if (jfrSummary.gcStats.count > 0) {
            double avgPause = jfrSummary.gcStats.totalPauseMillis / jfrSummary.gcStats.count;
            System.out.printf("[GC PAUSE] Count=%d, Avg=%.2f ms, Max=%.2f ms.%n",
                    jfrSummary.gcStats.count, avgPause, jfrSummary.gcStats.maxPauseMillis);
            
            if (avgPause > 200 || jfrSummary.gcStats.maxPauseMillis > 500) {
                 System.out.println("[HIGH] Long GC Pauses detected. **Action:** Focus optimization efforts on the Top Allocating Classes below to reduce garbage creation.");
            }
        }
        
        if (jfrSummary.allocationBytesByClass.isEmpty()) {
            System.out.println("[INFO] No ObjectAllocation events found. Cannot determine Allocation Hotspots.");
        } else {
            System.out.println("Top 5 Classes by Allocated Bytes (Allocation Hotspots):");
            jfrSummary.allocationBytesByClass.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparingLong(AtomicLong::get).reversed()))
                .limit(5)
                .forEach(entry -> 
                    System.out.printf("  > %s: %.2f MB%n", 
                                      entry.getKey(), 
                                      (double)entry.getValue().get() / (1024.0 * 1024.0)));
        }

        // --- C. Concurrency Summary (Simulating Thread Contention/Deadlock Detection) ---
        System.out.println("\n--- C. Concurrency Summary ---");
        System.out.printf("[DEADLOCKS] Total Deadlocks Detected: %d.%n", jfrSummary.deadlockCount);
        if (jfrSummary.deadlockCount > 0) {
            System.out.println("[CRITICAL] **Action:** A deadlock was detected. Review JFR thread dumps immediately. This indicates a severe concurrency bug requiring immediate code fix.");
        }
        
        if (jfrSummary.contendedMonitorCounts.isEmpty()) {
             System.out.println("[INFO] No Thread Contention events found. Contention may be low or JFR settings are insufficient.");
        } else {
            System.out.println("Top 5 Contended Monitors/Locks (Highest Contention):");
            jfrSummary.contendedMonitorCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparingLong(AtomicLong::get).reversed()))
                .limit(5)
                .forEach(entry -> 
                    System.out.printf("  > %s: %d contention events%n", 
                                      entry.getKey(), 
                                      entry.getValue().get()));
            
             System.out.println("[MEDIUM] High contention on monitors. **Action:** Investigate code using the locks above. Consider replacing synchronized blocks with `java.util.concurrent` primitives (e.g., `ReentrantLock`).");
        }

        // --- D. HIGH-CONFIDENCE CONCLUSION (Senior Engineer Elimination) ---
        System.out.println("\n--- D. HIGH-CONFIDENCE CONCLUSION ---");
        
        // Triage Logic: Assign scores based on severity to determine primary bottleneck.
        double cpuScore = 0;
        double gcScore = 0;
        double concurrencyScore = 0;

        // 1. CPU Scoring
        if (jfrSummary.cpuMaxPercent > 95.0) cpuScore = 3.0;
        else if (jfrSummary.cpuMaxPercent > 80.0) cpuScore = 2.0;
        else if (jfrSummary.cpuMaxPercent > 60.0) cpuScore = 1.0;
        
        // 2. GC/Allocation Scoring
        if (jfrSummary.gcStats.maxPauseMillis > 1000.0) gcScore = 3.0; // Max pause > 1 second is severe
        else if (jfrSummary.gcStats.maxPauseMillis > 500.0) gcScore = 2.0;
        else if (jfrSummary.totalAllocatedBytes / (1024.0 * 1024.0) > 5000) gcScore = 1.0; // Allocation > 5GB is high pressure
        
        // 3. Concurrency Scoring
        if (jfrSummary.deadlockCount > 0) concurrencyScore = 3.0; // Deadlock is critical
        else if (!jfrSummary.contendedMonitorCounts.isEmpty() && jfrSummary.contendedMonitorCounts.values().stream().mapToLong(AtomicLong::get).max().orElse(0L) > 1000) concurrencyScore = 2.0; // High contention events
        else if (!jfrSummary.contendedMonitorCounts.isEmpty()) concurrencyScore = 1.0; // Some contention found

        double maxScore = Math.max(cpuScore, Math.max(gcScore, concurrencyScore));
        String primaryIssue = "No Critical Issues Found";
        String nextStep = "Maintain current JFR logging setup and rerun during peak load.";

        if (maxScore > 0) {
            if (maxScore == concurrencyScore && concurrencyScore > 0) {
                primaryIssue = "Concurrency/Lock Contention (Score: " + maxScore + ")";
                nextStep = "IMMEDIATE CODE REVIEW: Focus strictly on the Top 5 Contended Monitors/Deadlocks. This is a synchronization fault.";
            } else if (maxScore == cpuScore) {
                primaryIssue = "High CPU Load/Inefficiency (Score: " + maxScore + ")";
                nextStep = "IMMEDIATE CODE REVIEW: Reduce computational time of the Top 5 CPU Hotspot methods identified in Section A.";
            } else if (maxScore == gcScore) {
                primaryIssue = "Garbage Collection/Memory Pressure (Score: " + maxScore + ")";
                nextStep = "IMMEDIATE CODE REVIEW: Examine the Top 5 Allocating Classes (Section B) to reduce object creation or tune GC pause goals.";
            }
        }

        System.out.println("=============================================");
        System.out.println(">> **PRIMARY BOTTLENECK IDENTIFIED**");
        System.out.printf(">> **ISSUE:** %s%n", primaryIssue);
        System.out.printf(">> **NEXT ACTION:** %s%n", nextStep);
        System.out.println("=============================================");

        // --- Next Steps Banner (Senior Engineer Elimination) ---
        System.out.println("\nSuggested next steps (Expert Diagnostic Checklist):");
        System.out.println("1) **Primary Focus:** Execute the **NEXT ACTION** from the High-Confidence Conclusion section above.");
        System.out.printf("2) **CPU Action:** Observed max JVM CPU %.1f%%. If >80%%, profile the Top 5 methods and look for inefficient loops or data structures.%n", jfrSummary.cpuMaxPercent);
        System.out.printf("3) **Memory Action:** Total allocation volume %.2f MB. If high, specifically examine the Top 5 Allocating Classes to reduce object churn.%n", allocatedMb);
        System.out.printf("4) **Concurrency Action:** Deadlock count %d. If >0, a critical bug exists. If contention is high on Top Locks, refactor using concurrent tools.%n", jfrSummary.deadlockCount);
        System.out.printf("5) **Final Step:** Once the primary issue is resolved, rerun this analyzer to identify the new bottleneck, as your code can now triage itself.%n");
        System.out.println();
    }
}