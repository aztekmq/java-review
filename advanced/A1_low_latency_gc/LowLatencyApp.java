import java.util.ArrayList;
import java.util.List;

/**
 * Generates bursty allocations and CPU work to compare pause behavior between
 * ZGC and G1. Logs progress so GC and runtime observations are repeatable.
 */
public class LowLatencyApp {
    public static void main(String[] args) {
        System.out.println("Starting LowLatencyApp workload...");
        long end = System.currentTimeMillis() + 5 * 60 * 1000; // 5 mins

        while (System.currentTimeMillis() < end) {
            // burst allocations
            List<byte[]> list = new ArrayList<>();
            for (int i = 0; i < 50_000; i++) {
                list.add(new byte[1024]); // 1KB
            }
            // drop references to allow GC
            list.clear();

            // some CPU work
            double acc = 0;
            for (int i = 0; i < 100_000; i++) {
                acc += Math.sqrt(i);
            }
            if ((System.currentTimeMillis() / 1000) % 10 == 0) {
                System.out.println("Acc=" + acc);
            }
        }

        System.out.println("LowLatencyApp finished.");
    }
}
