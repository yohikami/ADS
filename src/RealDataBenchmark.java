import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class RealDataBenchmark {

    private static final int RUNS = 10;
    private static final int QUERY_COUNT = 8000;

    public static void main(String[] args) {
        String filename = "facebook_combined.txt";
        System.out.println("Loading Real Dataset: " + filename + "...");

        // 1. Read the real data
        int[][] edges = loadRealSNAPDataset(filename);
        if (edges == null || edges.length == 0) return;

        // Find N (the highest User ID) to size our arrays correctly
        int maxUser = 0;
        for (int[] edge : edges) {
            maxUser = Math.max(maxUser, Math.max(edge[0], edge[1]));
        }
        int n = maxUser + 1; // Facebook nodes start at 0, so size is max + 1

        System.out.println("Real Network Loaded! Users: " + n + " | Total Friendships: " + edges.length);
        System.out.println("\nRunning Benchmarks (Averaged over " + RUNS + " runs)...");

        System.out.printf("%-15s | %-15s | %-15s | %-15s | %-15s%n", 
                "Dataset", "BFS Time (ms)", "UF Time (ms)", "BFS Mem (MB)", "UF Mem (MB)");
        System.out.println("-".repeat(85));

        runRealBenchmark(n, edges, "Facebook_SNAP");
    }

    private static void runRealBenchmark(int n, int[][] edges, String datasetName) {
        long totalTimeBFS = 0, totalTimeUF = 0;
        long totalMemBFS = 0, totalMemUF = 0;

        for (int i = 0; i < RUNS; i++) {
            // Generate random queries based on real user IDs
            int[][] queries = selectQueries(n);

            // Setup Data Structures
            GraphBFS bfsGraph = new GraphBFS(n);
            for (int[] edge : edges) bfsGraph.addEdge(edge[0], edge[1]);

            UnionFind ufGraph = new UnionFind(n);
            for (int[] edge : edges) ufGraph.union(edge[0], edge[1]);

            // Test Baseline (BFS)
            Runtime.getRuntime().gc();
            long startMemBFS = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long startTimeBFS = System.nanoTime();
            
            for (int[] q : queries) bfsGraph.isConnected(q[0], q[1]);
            
            long endTimeBFS = System.nanoTime();
            Runtime.getRuntime().gc();
            long endMemBFS = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            totalTimeBFS += (endTimeBFS - startTimeBFS);
            totalMemBFS += Math.max(0, endMemBFS - startMemBFS);

            // Test Enhanced (Union-Find)
            Runtime.getRuntime().gc();
            long startMemUF = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long startTimeUF = System.nanoTime();
            
            for (int[] q : queries) ufGraph.isConnected(q[0], q[1]);
            
            long endTimeUF = System.nanoTime();
            Runtime.getRuntime().gc();
            long endMemUF = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            totalTimeUF += (endTimeUF - startTimeUF);
            totalMemUF += Math.max(0, endMemUF - startMemUF);
        }

        // Calculate and Print Averages
        double avgTimeBFS = (totalTimeBFS / (double) RUNS) / 1_000_000.0;
        double avgTimeUF = (totalTimeUF / (double) RUNS) / 1_000_000.0;
        double avgMemBFS = (totalMemBFS / (double) RUNS) / (1024.0 * 1024.0);
        double avgMemUF = (totalMemUF / (double) RUNS) / (1024.0 * 1024.0);

        System.out.printf("%-15s | %-15.3f | %-15.3f | %-15.3f | %-15.3f%n", 
                datasetName, avgTimeBFS, avgTimeUF, avgMemBFS, avgMemUF);
    }

    private static int[][] loadRealSNAPDataset(String filename) {
        ArrayList<int[]> edgeList = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(new File(filename));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                // Skip comments in SNAP files
                if (line.isEmpty()) continue; 
                
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    edgeList.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("ERROR: Could not find '" + filename + "'. Make sure it is in your project folder!");
            return null;
        }
        return edgeList.toArray(new int[0][]);
    }

    private static int[][] selectQueries(int n) {
        int[][] queries = new int[QUERY_COUNT][2];
        Random rand = new Random();
        for (int i = 0; i < QUERY_COUNT; i++) {
            queries[i][0] = rand.nextInt(n);
            queries[i][1] = rand.nextInt(n);
        }
        return queries;
    }
}