import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        ConcurrentHashMap<Integer, String> columnResults = new ConcurrentHashMap<>();

        CompletableFuture<int[][]> matrixFuture = CompletableFuture.supplyAsync(() -> {
            int[][] matrix = new int[3][3];
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    matrix[i][j] = ThreadLocalRandom.current().nextInt(1, 100);
                }
            }
            return matrix;
        }, executor);

        CompletableFuture<Void> processedColumns = matrixFuture.thenAcceptAsync(matrix -> {
            CompletableFuture[] columnFutures = new CompletableFuture[3];
            for (int j = 0; j < 3; j++) {
                int finalJ = j;
                columnFutures[finalJ] = CompletableFuture.runAsync(() -> {
                            long startTime = System.nanoTime();
                            StringBuilder output = new StringBuilder();
                            for (int i = 0; i < 3; i++) {
                                output.append(matrix[i][finalJ]).append(" ");
                            }
                            long duration = System.nanoTime() - startTime;
                            columnResults.put(finalJ, output + "\nTime taken to process column " + (finalJ + 1) + ": " + duration + " ns");
                        }, executor)
                        .thenRunAsync(() -> {
                            System.out.println("Completed processing for column " + (finalJ + 1));
                        }, executor);
            }
            CompletableFuture.allOf(columnFutures).join();
            columnResults.forEach((key, value) -> {
                System.out.println("Column " + (key + 1) + ": " + value);
            });
        }, executor);

        processedColumns.join();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
