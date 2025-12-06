package com.example.jvmhealth;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;

/**
 * JVM Health Analyzer prints summary statistics from JFR recordings and GC logs.
 * Verbose reporting assists diagnostics during performance investigations.
 */
public class JvmHealthAnalyzer {

    private static class GcStats {
        long count = 0;
        double totalPauseMillis = 0.0;
        double maxPauseMillis = 0.0;
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

        analyzeJfr(jfrPath);
        if (gcLogPath != null && Files.exists(gcLogPath)) {
            analyzeGcLog(gcLogPath);
        }

        System.out.println("=== END OF REPORT ===");
    }

    private static void analyzeJfr(Path jfrPath) throws IOException {
        System.out.println("--- JFR SUMMARY ---");

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
    }

    private static void analyzeGcLog(Path gcLogPath) throws IOException {
        System.out.println("--- GC LOG SUMMARY (heuristic) ---");
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
    }
}
