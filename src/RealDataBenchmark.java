import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class RealDataBenchmark {

    private static final int RUNS = 10;
    private static final int QUERY_COUNT = 500;
    private static final int[] EDGE_LIMITS = {20000, 40000, 60000, 88234};

    public static void main(String[] args) {
        String filename = "facebook_combined.txt";
        
        // 1. Load the master dataset
        int[][] allEdges = loadRealSNAPDataset(filename);
        if (allEdges == null || allEdges.length == 0) return;

        // 2. JVM WARM-UP PHASE
        // This ensures the 20,000-edge test doesn't suffer from a "cold start"
        System.out.println("Priming JVM and performing Just-In-Time (JIT) optimizations...");
        performWarmup(allEdges);
        System.out.println("Warm-up complete. Starting formal empirical evaluation...\n");

        System.out.println("Total System Evaluation (Build + 500 Queries)");
        System.out.printf("%-15s | %-12s | %-12s | %-12s | %-12s | %-12s | %-12s%n", 
                "Edges", "BFS Total(ms)", "UF Total(ms)", "Time Imp.", "BFS Mem(MB)", "UF Mem(MB)", "Mem Red.");
        System.out.println("-".repeat(105));

        // 3. Scalability Loop
        for (int limit : EDGE_LIMITS) {
            int actualLimit = Math.min(limit, allEdges.length); 
            int[][] subsetEdges = Arrays.copyOfRange(allEdges, 0, actualLimit);
            
            // Calculate N for this subset to size arrays correctly
            int maxUser = 0;
            for (int[] edge : subsetEdges) {
                maxUser = Math.max(maxUser, Math.max(edge[0], edge[1]));
            }
            int n = maxUser + 1; 

            runRealBenchmark(n, subsetEdges, String.valueOf(actualLimit));
        }
    }

    private static void runRealBenchmark(int n, int[][] edges, String datasetSize) {
        long totalTimeBFS = 0, totalTimeUF = 0;
        long totalMemBFS = 0, totalMemUF = 0;

        for (int i = 0; i < RUNS; i++) {
            int[][] queries = generateQueries(n);

            // --- BFS Evaluation (Resets every run) ---
            Runtime.getRuntime().gc();
            long startMemBFS = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            long startTotalBFS = System.nanoTime();
            GraphBFS bfsGraph = new GraphBFS(n);
            for (int[] edge : edges) bfsGraph.addEdge(edge[0], edge[1]);
            for (int[] q : queries) bfsGraph.isConnected(q[0], q[1]);
            long endTotalBFS = System.nanoTime();
            
            long endMemBFS = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            totalTimeBFS += (endTotalBFS - startTotalBFS);
            totalMemBFS += Math.max(0, endMemBFS - startMemBFS);

            // --- Union-Find Evaluation (Resets every run) ---
            Runtime.getRuntime().gc();
            long startMemUF = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            long startTotalUF = System.nanoTime();
            UnionFind ufGraph = new UnionFind(n);
            for (int[] edge : edges) ufGraph.union(edge[0], edge[1]);
            for (int[] q : queries) ufGraph.isConnected(q[0], q[1]);
            long endTotalUF = System.nanoTime();
            
            long endMemUF = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            totalTimeUF += (endTotalUF - startTotalUF);
            totalMemUF += Math.max(0, endMemUF - startMemUF);
        }

        // Calculation of Averages
        double avgTimeBFS = (totalTimeBFS / (double) RUNS) / 1_000_000.0;
        double avgTimeUF = (totalTimeUF / (double) RUNS) / 1_000_000.0;
        double avgMemBFS = (totalMemBFS / (double) RUNS) / (1024.0 * 1024.0);
        double avgMemUF = (totalMemUF / (double) RUNS) / (1024.0 * 1024.0);

        // Calculation of Percentage Improvements
        double timeImprovement = ((avgTimeBFS - avgTimeUF) / avgTimeBFS) * 100.0;
        double memReduction = ((avgMemBFS - avgMemUF) / avgMemBFS) * 100.0;

        System.out.printf("%-15s | %-12.3f | %-12.3f | %-11.1f%% | %-12.3f | %-12.3f | %-11.1f%%%n", 
                datasetSize, avgTimeBFS, avgTimeUF, timeImprovement, avgMemBFS, avgMemUF, memReduction);
    }

    private static void performWarmup(int[][] allEdges) {
        // Runs a few iterations on a small subset to "wake up" the CPU and JVM
        int warmupSize = 1000;
        int[][] warmEdges = Arrays.copyOfRange(allEdges, 0, warmupSize);
        int n = 4039; // Fixed max for Facebook data
        
        for (int i = 0; i < 5; i++) {
            UnionFind warmUF = new UnionFind(n);
            GraphBFS warmBFS = new GraphBFS(n);
            for (int[] edge : warmEdges) {
                warmUF.union(edge[0], edge[1]);
                warmBFS.addEdge(edge[0], edge[1]);
            }
            warmUF.isConnected(0, 1);
            warmBFS.isConnected(0, 1);
        }
    }

    private static int[][] loadRealSNAPDataset(String filename) {
        ArrayList<int[]> edgeList = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(new File(filename));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) continue; 
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    edgeList.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) { 
            System.err.println("Dataset file not found.");
            return null; 
        }
        return edgeList.toArray(new int[0][]);
    }

    private static int[][] generateQueries(int n) {
        int[][] queries = new int[QUERY_COUNT][2];
        Random rand = new Random(2026); 
        for (int i = 0; i < QUERY_COUNT; i++) {
            queries[i][0] = rand.nextInt(n);
            queries[i][1] = rand.nextInt(n);
        }
        return queries;
    }
}