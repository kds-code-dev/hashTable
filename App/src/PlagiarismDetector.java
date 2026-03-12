import java.util.*;

public class PlagiarismDetector {
    // Stores: N-gram -> List of Document IDs
    private Map<String, Set<String>> nGramDatabase = new HashMap<>();
    private int n; // n-gram size

    public PlagiarismDetector(int n) {
        this.n = n;
    }

    // Preprocessing: Clean text, tokenize, and generate n-grams
    private List<String> generateNGrams(String text) {
        String[] words = text.toLowerCase().replaceAll("[^a-zA-Z0-9 ]", "").split("\\s+");
        List<String> nGrams = new ArrayList<>();
        if (words.length < n) return nGrams;
        for (int i = 0; i <= words.length - n; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                sb.append(words[i + j]).append(" ");
            }
            nGrams.add(sb.toString().trim());
        }
        return nGrams;
    }

    // Add a document to the index
    public void addDocument(String docId, String text) {
        List<String> nGrams = generateNGrams(text);
        for (String gram : nGrams) {
            nGramDatabase.computeIfAbsent(gram, k -> new HashSet<>()).add(docId);
        }
    }

    // Analyze a document against the database
    public void analyzeDocument(String currentDocId, String text) {
        List<String> newNGrams = generateNGrams(text);
        Map<String, Integer> matchCounts = new HashMap<>();

        for (String gram : newNGrams) {
            if (nGramDatabase.containsKey(gram)) {
                for (String docId : nGramDatabase.get(gram)) {
                    matchCounts.put(docId, matchCounts.getOrDefault(docId, 0) + 1);
                }
            }
        }

        // Output Results
        System.out.println("Extracted " + newNGrams.size() + " n-grams from " + currentDocId);
        for (Map.Entry<String, Integer> entry : matchCounts.entrySet()) {
            double similarity = ((double) entry.getValue() / newNGrams.size()) * 100;
            if (similarity > 10.0) { // Threshold for reporting
                System.out.printf("-> Found %d matching n-grams with %s. Similarity: %.1f%%%s%n",
                        entry.getValue(), entry.getKey(), similarity,
                        (similarity > 50.0 ? " (PLAGIARISM DETECTED)" : " (suspicious)"));
            }
        }
    }

    public static void main(String[] args) {
        PlagiarismDetector detector = new PlagiarismDetector(5); // Using 5-grams
        // Indexing...
        detector.addDocument("essay_089.txt", "The quick brown fox jumps over the lazy dog.");
        detector.addDocument("essay_092.txt", "Machine learning is a subset of artificial intelligence.");

        // Analyze...
        String newText = "The quick brown fox jumps over the lazy dog."; // Plagiarized
        detector.analyzeDocument("essay_123.txt", newText);
    }
}
