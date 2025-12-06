/**
 * Simulates a memory leak by storing 1KB byte arrays in a static map. Verbose
 * progress logging helps correlate heap growth with allocation milestones.
 */
public class LeakLab {
    private static final java.util.Map<String, byte[]> LEAK_MAP = new java.util.HashMap<>();

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 10_000_000; i++) {
            LEAK_MAP.put("key-" + i, new byte[1024]); // 1KB per entry
            if (i % 100_000 == 0) {
                System.out.println("Stored " + i + " entries");
                Thread.sleep(100);
            }
        }
    }
}
