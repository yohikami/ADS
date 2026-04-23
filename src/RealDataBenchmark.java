import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class RealDataBenchmark {

    private static final int RUNS = 10;
    private static final int QUERY_COUNT = 500;
    
    // We will test the network as it grows from 1000 edges up to 60000 
    private static final int[] EDGE_LIMITS = {1000, 10000, 40000, 60000};

    public static void main(String[] args) {
        String filename = "facebook_combined.txt";
        System.out.println("Loading Real Dataset: " + filename + "...");

        // 1. Read all the real data once
        int[][] allEdges = loadRealSNAPDataset(filename);
        if (allEdges == null || allEdges.length == 0) return;

        System.out.println("Real Network Loaded Total Friendships Available: " + allEdges.length);
        System.out.println("\nRunning Scalability Benchmarks on Real Data (Averaged over " + RUNS + " runs)...");

        System.out.printf("%-15s | %-15s | %-15s | %-15s | %-15s%n", 
                "Edges (Volume)", "BFS Time (ms)", "UF Time (ms)", "BFS Mem (MB)", "UF Mem (MB)");
        System.out.println("-".repeat(85));

        // 2. Run the test on progressively larger chunks of the real Facebook network
        for (int limit : EDGE_LIMITS) {
            // Prevent going out of bounds if the file is slightly different
            int actualLimit = Math.min(limit, allEdges.length); 
            
            // Create the sub-graph for this test size
            int[][] subsetEdges = Arrays.copyOfRange(allEdges, 0, actualLimit);
            
            // Find N (the highest User ID in this specific subset)
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

            // --- 1. MEASURE BFS MEMORY ---
            Runtime.getRuntime().gc();
            long startMemBFS = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            GraphBFS bfsGraph = new GraphBFS(n);
            for (int[] edge : edges) bfsGraph.addEdge(edge[0], edge[1]);
            
            long endMemBFS = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            totalMemBFS += Math.max(0, endMemBFS - startMemBFS);

            // --- 2. MEASURE UNION-FIND MEMORY ---
            Runtime.getRuntime().gc();
            long startMemUF = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            UnionFind ufGraph = new UnionFind(n);
            for (int[] edge : edges) ufGraph.union(edge[0], edge[1]);
            
            long endMemUF = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            totalMemUF += Math.max(0, endMemUF - startMemUF);

            // --- 3. MEASURE EXECUTION TIME ---
            long startTimeBFS = System.nanoTime();
            for (int[] q : queries) bfsGraph.isConnected(q[0], q[1]);
            totalTimeBFS += (System.nanoTime() - startTimeBFS);

            long startTimeUF = System.nanoTime();
            for (int[] q : queries) ufGraph.isConnected(q[0], q[1]);
            totalTimeUF += (System.nanoTime() - startTimeUF);
        }

        // Calculate and Print Averages
        double avgTimeBFS = (totalTimeBFS / (double) RUNS) / 1_000_000.0;
        double avgTimeUF = (totalTimeUF / (double) RUNS) / 1_000_000.0;
        double avgMemBFS = (totalMemBFS / (double) RUNS) / (1024.0 * 1024.0);
        double avgMemUF = (totalMemUF / (double) RUNS) / (1024.0 * 1024.0);

        System.out.printf("%-15s | %-15.3f | %-15.3f | %-15.3f | %-15.3f%n", 
                datasetSize + " edges", avgTimeBFS, avgTimeUF, avgMemBFS, avgMemUF);
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
            System.out.println("ERROR: Could not find '" + filename + "'. Make sure it is in your project folder!");
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