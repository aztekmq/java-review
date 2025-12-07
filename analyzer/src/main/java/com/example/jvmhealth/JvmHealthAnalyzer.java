package com.example.jvmhealth;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;

/**
 * JVM Health Analyzer prints summary statistics from JFR recordings and GC logs.
 * <p>
 * The reporting is intentionally verbose and action oriented so that entry-level engineers can
 * triage JVM incidents without needing a senior JVM specialist. Each section captures evidence,
 * computes friendly averages, and surfaces next steps in plain language following widely adopted
 * international programming standards for documentation.
 */
public class JvmHealthAnalyzer {

    private static class GcStats {
        long count = 0;
        double totalPauseMillis = 0.0;
        double maxPauseMillis = 0.0;
    }

    private record JfrSummary(long eventCount,
                               GcStats gcStats,
                               long allocationEvents,
                               long totalAllocatedBytes,
                               long cpuSamples,
                               double cpuSumPercent) {
    }

    private record GcLogSummary(long gcCount, double totalPauseMs, double maxPauseMs) {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java JvmHealthAnalyzer <jfr-file> [gc-log-file]");
            System.exit(1);
        }

        Path jfrPath = Paths.get(args[0]);
        Path gcLogPath = (args.length >= 2) ? Paths.get(args[1]) : null;

        System.out.println("=== JVM HEALTH ANALYSIS REPORT ===");
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

    private static JfrSummary analyzeJfr(Path jfrPath) throws IOException {
        System.out.println("--- JFR SUMMARY (verbose) ---");
        
        long eventCount = 0;
        GcStats gcStats = new GcStats();
        long allocationEvents = 0;
        long totalAllocatedBytes = 0;
        long cpuSamples = 0;
        double cpuSumPercent = 0.0;

        try (RecordingFile rf = new RecordingFile(jfrPath)) {
            while (rf.hasMoreEvents()) {
                RecordedEvent e = rf.readEvent();
                eventCount++;

                switch (e.getEventType().getName()) {
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
                        allocationEvents++;
                        Long size = e.getLong("allocationSize");
                        if (size != null) {
                            totalAllocatedBytes += size;
                        }
                    }
                    case "jdk.CPULoad" -> {
                        Double jvmUser = e.getDouble("jvmUser");
                        Double jvmSystem = e.getDouble("jvmSystem");
                        if (jvmUser != null && jvmSystem != null) {
                            cpuSamples++;
                            cpuSumPercent += (jvmUser + jvmSystem) * 100.0;
                        }
                    }
                    default -> {
                        // ignore others for now
                    }
                }
            }
        }

        System.out.println("Total events: " + eventCount);

        if (gcStats.count > 0) {
            System.out.printf("GC pauses: count=%d, total=%.2f ms, avg=%.2f ms, max=%.2f ms%n",
                    gcStats.count,
                    gcStats.totalPauseMillis,
                    gcStats.totalPauseMillis / gcStats.count,
                    gcStats.maxPauseMillis);
        } else {
            System.out.println("GC pauses: no GCPhasePause events found.");
        }

        if (allocationEvents > 0) {
            double mb = totalAllocatedBytes / (1024.0 * 1024.0);
            System.out.printf("Allocations: events=%d, totalAllocated=%.2f MB%n",
                    allocationEvents, mb);
        } else {
            System.out.println("Allocations: no ObjectAllocation events found.");
        }

        if (cpuSamples > 0) {
            double avgCpu = cpuSumPercent / cpuSamples;
            System.out.printf("CPU load: samples=%d, avgJVM_CPU=%.2f%%%n",
                    cpuSamples, avgCpu);
        } else {
            System.out.println("CPU load: no CPULoad events found.");
        }

        System.out.println();
        return new JfrSummary(eventCount, gcStats, allocationEvents, totalAllocatedBytes, cpuSamples, cpuSumPercent);
    }

    private static GcLogSummary analyzeGcLog(Path gcLogPath) throws IOException {
        System.out.println("--- GC LOG SUMMARY (heuristic, verbose) ---");
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

        System.out.println();
        return new GcLogSummary(gcCount, totalPauseMs, maxPauseMs);
    }

    private static void printFindings(JfrSummary jfrSummary, GcLogSummary gcSummary) {
        System.out.println("--- ACTIONABLE FINDINGS (entry-level ready) ---");

        // GC pauses
        if (jfrSummary.gcStats.count > 0) {
            double avgPause = jfrSummary.gcStats.totalPauseMillis / jfrSummary.gcStats.count;
            if (avgPause > 200 || jfrSummary.gcStats.maxPauseMillis > 500) {
                System.out.println("[HIGH] GC pauses are elevated. Consider lowering allocation rate, enabling a low-latency collector (e.g., ZGC), or adjusting -XX:MaxGCPauseMillis. Capture fresh GC logs for confirmation.");
            } else {
                System.out.println("[OK] GC pauses look healthy for typical JVM services. Keep existing GC flags and continue monitoring with verbose GC logging.");
            }
        } else {
            System.out.println("[INFO] No GCPhasePause events detected in JFR. Verify GC logging is enabled and rerun if pauses are expected.");
        }

        // Allocation pressure
        double allocatedMb = jfrSummary.totalAllocatedBytes / (1024.0 * 1024.0);
        if (allocatedMb > 500) {
            System.out.println("[MEDIUM] High allocation volume detected. Profile hot methods for object churn and prefer object reuse or primitive collections where safe.");
        } else if (allocatedMb > 0) {
            System.out.println("[OK] Allocation volume is modest. Continue capturing JFR during peak traffic to validate this remains steady.");
        } else {
            System.out.println("[INFO] Allocation events were not present. Ensure the recording used the 'profile' JFR settings for allocation insight.");
        }

        // CPU load
        if (jfrSummary.cpuSamples > 0) {
            double avgCpu = jfrSummary.cpuSumPercent / jfrSummary.cpuSamples;
            if (avgCpu > 80) {
                System.out.println("[HIGH] JVM CPU load is above 80%. Check for runaway threads, review JFR stack samples, and consider right-sizing CPU limits.");
            } else if (avgCpu > 60) {
                System.out.println("[MEDIUM] JVM CPU load is moderately high. Validate thread pool sizing and confirm GC is not CPU bound.");
            } else {
                System.out.println("[OK] JVM CPU load is within normal range. Keep current sizing but retain GC and JFR logs for future comparisons.");
            }
        } else {
            System.out.println("[INFO] No CPU samples found. Run with -XX:StartFlightRecording=...settings=profile to capture CPU data.");
        }

        // GC log heuristics
        if (gcSummary != null) {
            if (gcSummary.gcCount == 0) {
                System.out.println("[INFO] GC log parser did not find pause entries. Confirm the log format uses 'ms' and includes 'Pause'.");
            } else {
                double avgPause = gcSummary.totalPauseMs / gcSummary.gcCount;
                if (avgPause > 250 || gcSummary.maxPauseMs > 750) {
                    System.out.println("[HIGH] GC log shows long pauses. Capture heap dump on next run and explore ZGC or Shenandoah for lower latency.");
                } else {
                    System.out.println("[OK] GC log pause times are within expected range. Continue using current configuration.");
                }
            }
        }

        // Next steps banner for junior engineers
        System.out.println();
        System.out.println("Suggested next steps (entry-level checklist):");
        System.out.println("1) Keep verbose GC logging enabled (-Xlog:gc*) and rerun during peak traffic.");
        System.out.println("2) If pauses are high, try increasing heap size modestly (e.g., +256m) and re-measure.");
        System.out.println("3) If CPU is high, capture a new JFR with 'profile' settings and inspect top stack traces.");
        System.out.println("4) Store JFR + GC logs with timestamps so a senior engineer can review if issues persist.");
        System.out.println();
    }
}
