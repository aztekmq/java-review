import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates lock contention by synchronizing a heavy increment operation.
 * Verbose output reports final timing to aid benchmarking and tuning.
 */
public class LockContentionLab {
    private int counter = 0;

    public synchronized void increment() {
        // Heavy work inside synchronized method
        for (int i = 0; i < 1_000_000; i++) {
            counter++;
        }
    }

    public static void main(String[] args) {
        LockContentionLab lab = new LockContentionLab();
        ExecutorService pool = Executors.newFixedThreadPool(16);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            pool.submit(lab::increment);
        }
        pool.shutdown();
        try {
            pool.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException ignored) {
        }

        long end = System.currentTimeMillis();
        System.out.println("Final counter=" + lab.counter + ", time=" + (end - start) + " ms");
    }
}
