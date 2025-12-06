/**
 * Minimal application to run inside containers and observe JVM ergonomics with
 * resource limits. Verbose lifecycle messages support debugging and benchmarking.
 */
public class MyContainerApp {
    public static void main(String[] args) {
        System.out.println("MyContainerApp starting...");
        System.out.println("JVM settings (VM):");
        // Just block the main thread for a bit to inspect settings
        try {
            // show some allocations too
            byte[][] chunks = new byte[10_000][];
            for (int i = 0; i < chunks.length; i++) {
                chunks[i] = new byte[1024];
            }
            Thread.sleep(60_000); // 1 minute
        } catch (InterruptedException ignored) {
        }

        System.out.println("MyContainerApp exiting.");
    }
}
