import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * sentinel-ms 通知闭环本地压测：经 Gateway 提交唯一订单通知。
 *
 * 参数：baseUrl orderPrefix startIndex requests concurrency token
 * 示例：java ClosedLoopLoadTest.java http://localhost:8080 BENCH20260911- 0 1000 100 TOKEN
 */
public class ClosedLoopLoadTest {
    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            System.err.println("用法: baseUrl orderPrefix startIndex requests concurrency token");
            System.exit(2);
        }

        String baseUrl = args[0];
        String orderPrefix = args[1];
        int startIndex = Integer.parseInt(args[2]);
        int requests = Integer.parseInt(args[3]);
        int concurrency = Integer.parseInt(args[4]);
        String token = args[5];

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(executor)
                .build();

        Semaphore permits = new Semaphore(concurrency);
        CountDownLatch done = new CountDownLatch(requests);
        LongAdder success = new LongAdder();
        LongAdder failed = new LongAdder();
        AtomicInteger httpOther = new AtomicInteger();
        List<Long> latenciesNanos = Collections.synchronizedList(new ArrayList<>(requests));

        long started = System.nanoTime();
        for (int i = 0; i < requests; i++) {
            final int orderIndex = startIndex + i;
            permits.acquire();
            Thread.startVirtualThread(() -> {
                long requestStarted = System.nanoTime();
                try {
                    String orderNo = orderPrefix + String.format("%04d", orderIndex);
                    String url = baseUrl + "/api/logistics/notify/send?orderNo=" + orderNo
                            + "&node=LOADTEST&role=buyer&channel=sms";
                    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                            .timeout(Duration.ofSeconds(20))
                            .header("Authorization", "Bearer " + token)
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();
                    HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                    if (response.statusCode() == 200) {
                        success.increment();
                    } else {
                        failed.increment();
                        httpOther.incrementAndGet();
                    }
                } catch (Exception e) {
                    failed.increment();
                } finally {
                    latenciesNanos.add(System.nanoTime() - requestStarted);
                    permits.release();
                    done.countDown();
                }
            });
        }

        done.await();
        long elapsedNanos = System.nanoTime() - started;
        executor.shutdown();

        List<Long> latencies = new ArrayList<>(latenciesNanos);
        Collections.sort(latencies);
        long total = success.sum() + failed.sum();
        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        double qps = elapsedSeconds == 0 ? 0 : success.sum() / elapsedSeconds;

        System.out.println("requests=" + requests);
        System.out.println("concurrency=" + concurrency);
        System.out.println("success=" + success.sum());
        System.out.println("failed=" + failed.sum());
        System.out.println("http_other=" + httpOther.get());
        System.out.printf("elapsed_ms=%.2f%n", elapsedNanos / 1_000_000.0);
        System.out.printf("success_qps=%.2f%n", qps);
        System.out.printf("p50_ms=%.2f%n", percentile(latencies, 50));
        System.out.printf("p95_ms=%.2f%n", percentile(latencies, 95));
        System.out.printf("p99_ms=%.2f%n", percentile(latencies, 99));
        System.out.printf("max_ms=%.2f%n", total == 0 ? 0 : percentile(latencies, 100));
    }

    private static double percentile(List<Long> values, int percentile) {
        if (values.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * values.size()) - 1;
        index = Math.max(0, Math.min(index, values.size() - 1));
        return values.get(index) / 1_000_000.0;
    }
}
