package com.baskettecase.embedProc.processor;

import org.springframework.stereotype.Component;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;

@Component
public class VectorQueryProcessor {
    private final VectorStore vectorStore;

    public VectorQueryProcessor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Runs a similarity search query and prints the results.
     * @param queryText The query string to search for.
     * @param topK Number of top results to return.
     */
    public void runQuery(String queryText, int topK) {
        if (queryText == null || queryText.isBlank()) {
            System.out.println("[INFO] No query text provided. Skipping query.");
            return;
        }
        System.out.println("\n================ QUERY EXECUTION ================");
        System.out.println("[INFO] Running deploy query with parameters:");
        System.out.println("         Query Text: '" + queryText + "'");
        System.out.println("         Top K: " + topK);
        try {
            var results = vectorStore.similaritySearch(
                SearchRequest.builder().query(queryText).topK(topK).build()
            );
            if (results == null || results.isEmpty()) {
                System.out.println("[INFO] No results found for query.");
            } else {
                System.out.println("[INFO] Query results (top " + topK + "):");
                for (var doc : results) {
                    System.out.println(doc);
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to run deploy query: " + e.getMessage());
        }
        System.out.println("================ END QUERY EXECUTION ================\n");
    }
}
