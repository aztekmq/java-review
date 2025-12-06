package beginner.B1_gc_basics;

import java.util.ArrayList;

/**
 * Demonstrates basic garbage collection behavior by repeatedly allocating 1 KB
 * arrays. Verbose output is printed so activity can be monitored in the GC
 * logs and on standard out for easier debugging.
 */
public class GcBasics {

    /**
     * Runs a two-minute allocation loop that periodically clears references to
     * encourage garbage collection. Progress messages are emitted to make
     * activity observable when reviewing logs.
     *
     * @param args unused program arguments
     */
    public static void main(String[] args) {
        System.out.println("Starting GC Basics demo with verbose logging...");
        var allocations = new ArrayList<byte[]>();
        long end = System.currentTimeMillis() + 2 * 60 * 1000; // 2 minutes
        long allocationCount = 0;

        while (System.currentTimeMillis() < end) {
            allocations.add(new byte[1024]); // 1 KB
            allocationCount++;

            if (allocationCount % 10_000 == 0) {
                System.out.printf("Allocated %,d KB so far%n", allocationCount);
            }

            if (allocations.size() > 50_000) {
                allocations.clear(); // drop references so GC can reclaim
                System.out.println("Cleared allocation list to trigger GC");
            }
        }
        System.out.println("Completed GC Basics demo.");
    }
}
