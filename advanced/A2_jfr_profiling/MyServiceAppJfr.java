import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * HTTP service designed for JFR profiling labs. Verbose startup messaging helps
 * validate configuration before recordings are taken.
 */
public class MyServiceAppJfr {
    public static void main(String[] args) throws IOException {
        System.out.println("Starting MyServiceAppJfr on port 8081...");
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/heavy", new HeavyHandler());
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Hit http://localhost:8081/heavy to generate profiling load");
    }

    static class HeavyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
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
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        }
    }
}
