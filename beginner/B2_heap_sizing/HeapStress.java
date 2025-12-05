/**
 * Continuously allocates large int arrays to stress heap sizing and trigger
 * OutOfMemoryError. Verbose allocation count is printed to trace progress.
 */
public class HeapStress {
    public static void main(String[] args) {
        java.util.List<int[]> holder = new java.util.ArrayList<>();
        int size = 1_000_000; // ~4MB per array
        int count = 0;

        while (true) {
            holder.add(new int[size]);
            count++;
            System.out.println("Allocated arrays: " + count);
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
