import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DNSCacheSystem {

    // 1. DNSEntry Class
    static class DNSEntry {
        String domain;
        String ipAddress;
        long expiryTime; // Timestamp in milliseconds

        public DNSEntry(String domain, String ipAddress, long ttlSeconds) {
            this.domain = domain;
            this.ipAddress = ipAddress;
            this.expiryTime = System.currentTimeMillis() + (ttlSeconds * 1000);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    private final int capacity;
    private final LinkedHashMap<String, DNSEntry> cache;
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final ScheduledExecutorService cleaner;

    public DNSCacheSystem(int capacity) {
        this.capacity = capacity;
        // 4. Implement LRU using LinkedHashMap (accessOrder = true)
        this.cache = new LinkedHashMap<String, DNSEntry>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, DNSEntry> eldest) {
                return size() > DNSCacheSystem.this.capacity;
            }
        };

        // 3. Background thread to clean expired entries
        this.cleaner = Executors.newSingleThreadScheduledExecutor();
        this.cleaner.scheduleAtFixedRate(this::cleanExpiredEntries, 1, 1, TimeUnit.SECONDS);
    }

    public String resolve(String domain) {
        synchronized (cache) {
            DNSEntry entry = cache.get(domain);
            if (entry != null && !entry.isExpired()) {
                hits.incrementAndGet();
                System.out.println("resolve(\"" + domain + "\") → Cache HIT → " + entry.ipAddress);
                return entry.ipAddress;
            }

            // Cache MISS or EXPIRED
            if (entry != null && entry.isExpired()) {
                System.out.println("resolve(\"" + domain + "\") → Cache EXPIRED → Query upstream");
            } else {
                System.out.println("resolve(\"" + domain + "\") → Cache MISS → Query upstream");
            }

            misses.incrementAndGet();
            String ip = queryUpstreamDNS(domain);
            // 2. Add/Update Mapping with TTL
            cache.put(domain, new DNSEntry(domain, ip, 300)); // 300s TTL
            return ip;
        }
    }

    private String queryUpstreamDNS(String domain) {
        // Simulated upstream lookup
        if (domain.equals("google.com")) return "172.217.14.206";
        return "192.0.2.1";
    }

    private void cleanExpiredEntries() {
        synchronized (cache) {
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }

    public void getCacheStats() {
        long h = hits.get();
        long m = misses.get();
        double total = h + m;
        double rate = (total == 0) ? 0 : (h / total) * 100;
        System.out.printf("Stats → Hit Rate: %.1f%%, Hits: %d, Misses: %d%n", rate, h, m);
    }

    public static void main(String[] args) throws InterruptedException {
        DNSCacheSystem dnsCache = new DNSCacheSystem(2);

        // Scenario Simulation
        dnsCache.resolve("google.com"); // MISS
        Thread.sleep(100);
        dnsCache.resolve("google.com"); // HIT

        dnsCache.resolve("example.com"); // MISS
        dnsCache.getCacheStats();

        // Wait for expiration (set to 1s for testing in real scenario)
        // In real test change TTL to 1s, currently set to 300s.
    }
}
