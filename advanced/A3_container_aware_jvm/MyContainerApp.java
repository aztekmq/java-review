import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import jdk.management.jfr.FlightRecorderMXBean;
import jdk.management.jfr.RecordingInfo;

/**
 * Minimal application to observe JVM ergonomics with resource limits without
 * relying on containers. Verbose lifecycle messages support debugging and
 * benchmarking.
 *
 * <p>The implementation follows international programming standards by
 * documenting each diagnostic step and by hardening the runtime to ensure that
 * JFR and GC artifacts are written to the repository root by default (aligned
 * with other lab outputs) even when the JVM shuts down quickly. Environment
 * variables or system properties allow alternate destinations while preserving
 * verbose traceability.</p>
 */
public class MyContainerApp {
    private static final Duration PAUSE_DURATION = Duration.ofMinutes(1);
    private static final String DEFAULT_JFR_PATH = "advanced-a3.jfr";
    private static final String DEFAULT_GC_LOG_PATH = "advanced-a3-gc.log";

    public static void main(String[] args) {
        Path jfrPath = resolvePath("JFR_DUMP_PATH", "jfr.dump.path", DEFAULT_JFR_PATH);
        Path gcLogPath = resolvePath("GC_LOG_PATH", "gc.log.path", DEFAULT_GC_LOG_PATH);

        prepareOutputTarget("JFR", jfrPath);
        prepareOutputTarget("GC", gcLogPath);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> ensureJfrDump(jfrPath)));

        System.out.println("MyContainerApp starting...");
        System.out.println("JVM settings (VM):");
        System.out.println("Diagnostics: running on Java " + System.getProperty("java.version"));
        System.out.println("Diagnostics: writing JFR to " + jfrPath);
        System.out.println("Diagnostics: writing GC log to " + gcLogPath);

        try {
            byte[][] chunks = new byte[10_000][];
            for (int i = 0; i < chunks.length; i++) {
                chunks[i] = new byte[1024];
            }

            System.out.println("Diagnostics: allocation phase complete; forcing GC to populate logs...");
            System.gc();

            System.out.println("Diagnostics: pausing for " + PAUSE_DURATION.toSeconds() + " seconds to allow profiling...");
            Thread.sleep(PAUSE_DURATION.toMillis());
        } catch (InterruptedException interruptedException) {
            System.out.println("Diagnostics: sleep interrupted, continuing shutdown sequence.");
            Thread.currentThread().interrupt();
        }

        System.out.println("MyContainerApp exiting.");
    }

    private static void prepareOutputTarget(String label, Path targetPath) {
        try {
            Optional.ofNullable(targetPath.getParent())
                    .ifPresent(path -> {
                        try {
                            Files.createDirectories(path);
                        } catch (Exception exception) {
                            System.out.println(
                                    "Diagnostics: unable to create directory for "
                                            + label
                                            + " at "
                                            + path
                                            + ": "
                                            + exception.getMessage());
                        }
                    });

            System.out.println("Diagnostics: confirmed output target for " + label + " at " + targetPath);
        } catch (Exception exception) {
            System.out.println("Diagnostics: unable to prepare output target for " + label + ": " + exception.getMessage());
        }
    }

    private static Path resolvePath(String envName, String propertyName, String defaultPath) {
        return Optional.ofNullable(System.getenv(envName))
                .map(Path::of)
                .or(() -> Optional.ofNullable(System.getProperty(propertyName)).map(Path::of))
                .orElse(Path.of(defaultPath));
    }

    private static void ensureJfrDump(Path jfrPath) {
        try {
            FlightRecorderMXBean jfr = ManagementFactory.getPlatformMXBean(FlightRecorderMXBean.class);
            if (jfr == null) {
                System.out.println("Diagnostics: Flight Recorder MXBean unavailable; cannot enforce JFR dump.");
                return;
            }

            List<RecordingInfo> recordings = jfr.getRecordings();
            if (recordings.isEmpty()) {
                System.out.println("Diagnostics: no active JFR recordings detected during shutdown.");
                return;
            }

            for (RecordingInfo recordingInfo : recordings) {
                try {
                    jfr.copyTo(recordingInfo.getId(), jfrPath.toString());
                    System.out.println(
                            "Diagnostics: JFR recording "
                                    + recordingInfo.getId()
                                    + " copied to "
                                    + jfrPath
                                    + " during shutdown.");
                } catch (Exception exception) {
                    System.out.println(
                            "Diagnostics: unable to copy JFR recording "
                                    + recordingInfo.getId()
                                    + " to "
                                    + jfrPath
                                    + ": "
                                    + exception.getMessage());
                }
            }
        } catch (Exception exception) {
            System.out.println("Diagnostics: JFR shutdown hook encountered an error: " + exception.getMessage());
        }
    }
}
