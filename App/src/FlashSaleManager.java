import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class FlashSaleManager {

    // HashMap<productId, stockCount> - atomic for thread safety
    private final ConcurrentHashMap<String, AtomicInteger> inventory = new ConcurrentHashMap<>();

    // Waiting list for each product (FIFO)
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> waitingLists = new ConcurrentHashMap<>();

    // Initialize stock for a product
    public void addProduct(String productId, int stock) {
        inventory.put(productId, new AtomicInteger(stock));
        waitingLists.put(productId, new ConcurrentLinkedQueue<>());
    }

    // Instant stock check - O(1)
    public int checkStock(String productId) {
        AtomicInteger stock = inventory.get(productId);
        return (stock != null) ? stock.get() : 0;
    }

    // Purchase item - O(1)
    public String purchaseItem(String productId, String userId) {
        if (!inventory.containsKey(productId)) {
            return "Product not found";
        }

        AtomicInteger stock = inventory.get(productId);

        // Atomic decrement operation
        while (true) {
            int currentStock = stock.get();
            if (currentStock <= 0) {
                // Add to waiting list
                waitingLists.get(productId).add(userId);
                return "Added to waiting list, user: " + userId + ", position #" + waitingLists.get(productId).size();
            }

            // compareAndSet ensures only one thread succeeds
            if (stock.compareAndSet(currentStock, currentStock - 1)) {
                return "Success, " + (currentStock - 1) + " units remaining, user: " + userId;
            }
            // If compareAndSet fails, another thread changed the stock, retry loop
        }
    }

    // Main method to demonstrate the scenario
    public static void main(String[] args) {
        FlashSaleManager manager = new FlashSaleManager();
        String productId = "IPHONE15_256GB";
        manager.addProduct(productId, 100);

        System.out.println("Initial Stock: " + manager.checkStock(productId));

        // Simulate 105 customers trying to buy 100 items
        for (int i = 0; i < 105; i++) {
            final String userId = "user_" + i;
            System.out.println(manager.purchaseItem(productId, userId));
        }
    }
}
