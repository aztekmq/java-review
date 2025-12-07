import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AsyncProfilerLab drives CPU-intensive and allocation-heavy work so you can
 * capture flame graphs with async-profiler. The application keeps verbose
 * logging enabled to expose every lifecycle transition per international
 * programming standards for diagnosable labs.
 */
public final class AsyncProfilerLab {
    private static final Logger LOGGER = Logger.getLogger(AsyncProfilerLab.class.getName());
    private static final int DEFAULT_WORKERS = 8;
    private static final int DEFAULT_CYCLE_COUNT = 5_000;
    private static final long WORKLOAD_DURATION_MILLIS = 5 * 60 * 1000; // 5 minutes

    private AsyncProfilerLab() {
        // Utility class: do not instantiate.
    }

    /**
     * Entry point that configures verbose logging, spins CPU and allocation
     * heavy tasks, and keeps the JVM alive for profiling attachments.
     *
     * @param args command line arguments (unused)
     * @throws InterruptedException if shutdown waits are interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        configureLogging();

        LOGGER.info(() -> String.format("Starting AsyncProfilerLab with %d workers and %d workload cycles, running for %d minutes.", 
                DEFAULT_WORKERS, DEFAULT_CYCLE_COUNT, WORKLOAD_DURATION_MILLIS / 60000));
        
        // Define the time when the program should stop
        final long endTimeMillis = System.currentTimeMillis() + WORKLOAD_DURATION_MILLIS;

        AtomicBoolean running = new AtomicBoolean(true);
        ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_WORKERS);

        // This shutdown hook is still necessary for external signals (like Ctrl+C)
        // and will also be triggered when the main thread loop finishes.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown requested; signalling workers to stop.");
            running.set(false);
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.warning("Forcing executor shutdown after timeout.");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.SEVERE, "Interrupted during shutdown", e);
            }
        }, "async-profiler-lab-shutdown"));

        for (int i = 0; i < DEFAULT_WORKERS; i++) {
            int workerId = i;
            executor.submit(() -> runWorkload(workerId, running, endTimeMillis));
        }

        LOGGER.info("Workers started. Attach async-profiler (cpu + alloc) now.");

        // Keep the main thread alive while workers loop, but also check the time limit.
        // It will exit when the time limit is hit and the AtomicBoolean is set to false.
        while (running.get() && System.currentTimeMillis() < endTimeMillis) {
            Thread.sleep(2_000L);
        }
        
        // Time limit reached; signal workers to stop via the running flag and then the shutdown hook.
        if (System.currentTimeMillis() >= endTimeMillis) {
            LOGGER.info("Time limit reached. Signalling workers to stop and initiating JVM shutdown.");
            running.set(false);
        }

        // Wait a short time for the shutdown hook to execute its cleanup
        executor.awaitTermination(6, TimeUnit.SECONDS); 
        LOGGER.info("AsyncProfilerLab main thread exiting. Goodbye.");
    }

    private static void runWorkload(int workerId, AtomicBoolean running, long endTimeMillis) {
        LOGGER.fine(() -> String.format("Worker-%d started workload loop with verbose logging enabled.", workerId));
        Random random = new Random(workerId * 17L + System.nanoTime());
        int cycleCounter = 0;

        // The workload loop now uses the time constraint AND the atomic boolean
        while (running.get() && System.currentTimeMillis() < endTimeMillis) {
            Instant cycleStart = Instant.now();
            double cpuResult = spinCpu(random);
            byte[] payload = allocatePayload(random);
            simulateBusinessLogic(random, payload);
            cycleCounter++;

            Duration cycleDuration = Duration.between(cycleStart, Instant.now());
            LOGGER.fine(() -> String.format("Worker-%d completed cycle in %d ms (checksum=%.4f, payload=%d bytes)", workerId,
                    cycleDuration.toMillis(), cpuResult, payload.length));

            if (cycleCounter % DEFAULT_CYCLE_COUNT == 0) {
                // This local variable 'cycles' is effectively final for the lambda inside the loop.
                final int cycles = cycleCounter; 
                LOGGER.info(() -> String.format(
                        "Worker-%d reached %,d cycles; async-profiler should show stable flame shapes now.",
                        workerId, cycles));
            }
        }
        
        // FIX: Create a final/effectively final copy of cycleCounter's final value
        // before referencing it in the final logging lambda.
        final int finalCycleCount = cycleCounter;
        LOGGER.info(() -> String.format("Worker-%d finished after %,d cycles.", workerId, finalCycleCount));
    }

    private static double spinCpu(Random random) {
        // Simulate CPU-bound math using small matrix multiplications to create
        // predictable hot spots for async-profiler flame graphs.
        int size = 20;
        double[][] a = new double[size][size];
        double[][] b = new double[size][size];
        double[][] c = new double[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                a[i][j] = random.nextDouble();
                b[i][j] = random.nextDouble();
            }
        }

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                double sum = 0.0;
                for (int k = 0; k < size; k++) {
                    sum += a[i][k] * b[k][j];
                }
                c[i][j] = sum;
            }
        }

        double checksum = 0.0;
        for (double[] row : c) {
            for (double value : row) {
                checksum += value;
            }
        }
        return checksum;
    }

    private static byte[] allocatePayload(Random random) {
        // Allocate several arrays and concatenate to trigger allocation
        // profiling samples. Content is deterministic for reproducibility.
        List<byte[]> blocks = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            byte[] block = new byte[1_024];
            random.nextBytes(block);
            blocks.add(block);
        }

        int total = blocks.size() * blocks.get(0).length;
        byte[] combined = new byte[total];
        int offset = 0;
        for (byte[] block : blocks) {
            System.arraycopy(block, 0, combined, offset, block.length);
            offset += block.length;
        }
        return combined;
    }

    private static void simulateBusinessLogic(Random random, byte[] payload) {
        // Convert the payload into a simple UTF-8 string and perform string
        // manipulations to create additional allocation and CPU pressure.
        String message = new String(payload, StandardCharsets.UTF_8);
        String filtered = message.replace('\0', 'x');
        if (filtered.length() > 512 && random.nextBoolean()) {
            String slice = filtered.substring(0, 512);
            double math = slice.chars().mapToDouble(c -> c * 0.37d).sum();
            LOGGER.finest(() -> String.format("Business logic math result: %.2f", math));
        }
    }

    private static void configureLogging() {
        Logger rootLogger = Logger.getLogger("");
        for (var handler : rootLogger.getHandlers()) {
            handler.setLevel(Level.FINEST);
        }

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINEST);
        rootLogger.addHandler(consoleHandler);

        rootLogger.setLevel(Level.FINEST);
        LOGGER.config("Verbose logging configured to Level.FINEST for async-profiler lab diagnostics.");
    }
}