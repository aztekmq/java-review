/**
 * Spawns threads in different states to demonstrate RUNNABLE, TIMED_WAITING,
 * and BLOCKED behaviors. Verbose messages allow inspection via jstack.
 */
public class ThreadStatesDemo {
    private static final Object LOCK = new Object();

    public static void main(String[] args) {
        Thread busy = new Thread(() -> {
            // RUNNABLE CPU loop
            while (true) {
                Math.sqrt(Math.random());
            }
        }, "busy-thread");

        Thread sleeper = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(10_000); // TIMED_WAITING
                }
            } catch (InterruptedException ignored) {
            }
        }, "sleeping-thread");

        Thread locker1 = new Thread(() -> {
            synchronized (LOCK) {
                try {
                    System.out.println("locker-1 acquired lock and will hold it");
                    Thread.sleep(60_000); // holds LOCK
                } catch (InterruptedException ignored) {
                }
            }
        }, "locker-1");

        Thread locker2 = new Thread(() -> {
            synchronized (LOCK) { // will BLOCK here
                System.out.println("locker-2 got the lock");
            }
        }, "locker-2");

        busy.start();
        sleeper.start();
        locker1.start();
        locker2.start();

        try {
            Thread.sleep(120_000);
        } catch (InterruptedException ignored) {
        }
    }
}
