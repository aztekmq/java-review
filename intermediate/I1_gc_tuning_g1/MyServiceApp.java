import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Simple HTTP service that performs allocation-heavy work to make GC tuning
 * observable. Verbose server messages aid diagnostics during load tests.
 */
public class MyServiceApp {
    public static void main(String[] args) throws IOException {
        System.out.println("Starting MyServiceApp on port 8080...");
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/work", new WorkHandler());
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Hit http://localhost:8080/work to generate load");
    }

    static class WorkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Simulated CPU + allocation work
            List<byte[]> data = new ArrayList<>();
            for (int i = 0; i < 10_000; i++) {
                data.add(new byte[1024]); // 1KB each
            }
            String response = "OK, processed " + data.size() + " chunks\n";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        }
    }
}
