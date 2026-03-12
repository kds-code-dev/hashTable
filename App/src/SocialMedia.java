import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SocialMedia {
    // Stores taken usernames and their user IDs (O(1) lookup)
    private ConcurrentHashMap<String, Long> takenUsernames;
    // Tracks popularity of attempted usernames
    private ConcurrentHashMap<String, AtomicLong> attemptFrequency;

    public SocialMedia() {
        takenUsernames = new ConcurrentHashMap<>();
        attemptFrequency = new ConcurrentHashMap<>();
    }

    // Check if username exists in O(1) time
    public boolean checkAvailability(String username) {
        trackAttempt(username);
        return !takenUsernames.containsKey(username);
    }

    private void trackAttempt(String username) {
        attemptFrequency.putIfAbsent(username, new AtomicLong(0));
        attemptFrequency.get(username).incrementAndGet();
    }

    // Suggest alternatives
    public String[] suggestAlternatives(String username) {
        String[] suggestions = new String[3];
        suggestions[0] = username + "1";
        suggestions[1] = username + "_";
        suggestions[2] = "the" + username;
        // Verify availability of suggestions before returning
        return suggestions;
    }

    // Get most attempted username
    public String getMostAttempted() {
        return attemptFrequency.entrySet().stream()
                .max((entry1, entry2) -> (int) (entry1.getValue().get() - entry2.getValue().get()))
                .map(entry -> entry.getKey())
                .orElse(null);
    }
}
