/**
 * Demonstrates basic GC behavior by allocating 1KB arrays repeatedly and
 * periodically clearing references to encourage garbage collection. Verbose
 * output is printed so activity can be monitored in GC logs.
 */
public class GcBasics {
    public static void main(String[] args) {
        System.out.println("Starting GC Basics demo...");
        var list = new java.util.ArrayList<byte[]>();
        long end = System.currentTimeMillis() + 2 * 60 * 1000; // 2 minutes

        while (System.currentTimeMillis() < end) {
            list.add(new byte[1024]); // 1 KB
            if (list.size() > 50_000) {
                list.clear(); // drop references so GC can reclaim
                System.out.println("Cleared allocation list to trigger GC");
            }
        }
        System.out.println("Done.");
    }
}
