import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

class PageAnalyticsSystem {
    // Thread-safe maps to handle high throughput
    private final ConcurrentHashMap<String, LongAdder> pageViewMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> uniqueVisitorMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> sourceMap = new ConcurrentHashMap<>();

    // Event ingestion: O(1) time complexity
    public void processEvent(String url, String userId, String source) {
        // Update page view counts
        pageViewMap.computeIfAbsent(url, k -> new LongAdder()).increment();

        // Update unique visitors (use ConcurrentHashMap.newKeySet for thread-safe sets)
        uniqueVisitorMap.computeIfAbsent(url, k -> ConcurrentHashMap.newKeySet()).add(userId);

        // Update traffic sources
        sourceMap.computeIfAbsent(source, k -> new LongAdder()).increment();
    }

    // Dashboard generation: O(N log K) where N is distinct pages, K=10
    public void getDashboard() {
        System.out.println("--- Dashboard Update [" + new Date() + "] ---");

        // Use PriorityQueue to find top 10 pages based on total views
        PriorityQueue<Map.Entry<String, LongAdder>> topPages = new PriorityQueue<>(
                (a, b) -> Long.compare(b.getValue().sum(), a.getValue().sum())
        );

        pageViewMap.entrySet().forEach(topPages::offer);

        System.out.println("Top 10 Pages:");
        for (int i = 0; i < 10 && !topPages.isEmpty(); i++) {
            Map.Entry<String, LongAdder> entry = topPages.poll();
            String url = entry.getKey();
            long views = entry.getValue().sum();
            int uniqueVisitors = uniqueVisitorMap.getOrDefault(url, Collections.emptySet()).size();
            System.out.printf("%d. %s - %d views (%d unique)\n", i + 1, url, views, uniqueVisitors);
        }

        System.out.println("Traffic Sources: " + sourceMap.toString());
        System.out.println("-----------------------------------\n");
    }

    public static void main(String[] args) throws InterruptedException {
        PageAnalyticsSystem system = new PageAnalyticsSystem();

        // Simulate incoming events
        Thread producer = new Thread(() -> {
            String[] sources = {"google", "facebook", "direct", "twitter"};
            Random r = new Random();
            for (int i = 0; i < 1000; i++) {
                system.processEvent("/article/" + r.nextInt(5), "user_" + r.nextInt(100), sources[r.nextInt(4)]);
                try { Thread.sleep(10); } catch (InterruptedException e) {}
            }
        });
        producer.start();

        // Batch update every 5 seconds
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(system::getDashboard, 0, 5, TimeUnit.SECONDS);

        producer.join();
        Thread.sleep(6000);
        scheduler.shutdown();
    }
}
