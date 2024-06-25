package com.crpt;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;

public class CrptApi {
    final static int MAX_REQUEST_PER_TIME = 10;
    final static TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    public static class RequestSubmitter implements Runnable {
        private final BlockingQueue<Request> requests;

        public RequestSubmitter(final BlockingQueue<Request> requests) {
            this.requests = requests;
        }

        @Override
        public void run() {
            try {
                //Blocks until there is will be space
                requests.put(new Request());
            }
            catch (Exception exception) {
                System.err.println(exception.getMessage());
            }
        }
    }

    public static class Request {
        private static final String URL_API = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        public void process(String json) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(URL_API))
                        .headers("Content-Type", "application/json;charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

                future.thenApply(HttpResponse::body)
                        .thenAccept(System.out::println)
                        .join();
                System.out.println("Current request time in millis: " + System.currentTimeMillis());
            }
            catch (Exception exception) {
                System.err.println();
            }
        }
    }

    public static class RequestImplementor implements Runnable {
        private final BlockingQueue<Request> requests;
        private final String json;

        public RequestImplementor(BlockingQueue<Request> requests, String json) {
            this.requests = requests;
            this.json = json;
        }

        @Override
        public void run() {
            try {
                requests.take().process(json);
            }
            catch (Exception exception) {
                System.err.println(exception.getMessage());
            }
        }
    }

    public static void main(final String[] args) {
        final BlockingQueue<Request> requests = new ArrayBlockingQueue<>(MAX_REQUEST_PER_TIME, true);

        final ExecutorService pool = Executors.newFixedThreadPool(1);
        for (int i = 0; i < 100000; ++i) {
            pool.submit(new RequestSubmitter(requests));
        }

        final long delayMicroseconds = TimeUnit.MICROSECONDS.convert(1, TIME_UNIT) / MAX_REQUEST_PER_TIME / 10000;

        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(new RequestImplementor(requests,"{}"), 0L, delayMicroseconds, TimeUnit.MILLISECONDS);
    }
}