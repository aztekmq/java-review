import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * HTTP service designed for JFR profiling labs. Verbose startup messaging helps
 * validate configuration before recordings are taken.
 */
public class MyServiceAppJfr {
    private static final Logger LOGGER = Logger.getLogger(MyServiceAppJfr.class.getName());

    /**
     * Application entry point that configures verbose logging, starts the HTTP
     * server, and blocks to keep the service alive for profiling.
     *
     * @param args command line arguments (unused)
     * @throws IOException          when the HTTP server fails to bind or start
     * @throws InterruptedException if the keep-alive latch is interrupted
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        configureLogging();

        LOGGER.info("Starting MyServiceAppJfr on port 8081...");
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/heavy", new HeavyHandler());
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        LOGGER.info("Hit http://localhost:8081/heavy to generate profiling load");
        LOGGER.info("HTTP server started successfully and is ready to accept traffic.");

        CountDownLatch keepAlive = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown requested; stopping HTTP server.");
            server.stop(0);
            keepAlive.countDown();
        }, "server-shutdown-hook"));

        LOGGER.fine("Blocking main thread to keep server process alive for profiling.");
        keepAlive.await();
    }

    static class HeavyHandler implements HttpHandler {
        /**
         * Handles heavy CPU and allocation work to generate profiling activity on
         * demand, logging request lifecycle details for easier diagnostics.
         *
         * @param exchange HTTP exchange representing the incoming request and
         *                 outgoing response
         * @throws IOException if the response cannot be written
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            LOGGER.info(() -> String.format("Handling %s request from %s to %s", exchange.getRequestMethod(),
                    exchange.getRemoteAddress(), exchange.getRequestURI()));
            long start = System.nanoTime();

            // CPU + allocations
            List<double[]> junk = new ArrayList<>();
            for (int i = 0; i < 5_000; i++) {
                double[] arr = new double[512];
                for (int j = 0; j < arr.length; j++) {
                    arr[j] = Math.sin(j) * Math.cos(i);
                }
                junk.add(arr);
            }

            long durationMs = (System.nanoTime() - start) / 1_000_000;
            String response = "Heavy work done in " + durationMs + " ms\n";
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

            LOGGER.info(() -> String.format("Computed heavy workload in %d ms; response size=%d bytes", durationMs,
                    responseBytes.length));

            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        }
    }

    private static void configureLogging() {
        Logger rootLogger = Logger.getLogger("");
        for (var handler : rootLogger.getHandlers()) {
            handler.setLevel(Level.FINE);
        }

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINE);
        rootLogger.addHandler(consoleHandler);

        rootLogger.setLevel(Level.FINE);
        LOGGER.config("Verbose logging configured to Level.FINE for debugging and profiling visibility.");
    }
}
