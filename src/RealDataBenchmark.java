import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class RealDataBenchmark {

    // Settings
    private static final int RUNS = 10;        
    private static final int QUERY_COUNT = 10000; 
    private static final int[] EDGE_LIMITS = {20000, 40000, 60000, 88234}; 

    public static void main(String[] args) {
        String filename = "facebook_combined.txt";
        
        // Read dataset 
        int[][] allEdges = loadRealSNAPDataset(filename);
        if (allEdges == null || allEdges.length == 0) return;

        // Warm up JVM
        System.out.println("Preparing...");
        performWarmup(allEdges);
        System.out.println("Ready.\n");

        // Print headers
        System.out.println("Performance Test");
        System.out.printf("%-10s | %-12s | %-12s | %-14s | %-14s | %-12s | %-12s%n", 
                "Edges", "BFS Build(ms)", "UF Build(ms)", "BFS Query(ms)", "UF Query(ms)", "BFS Mem(MB)", "UF Mem(MB)");
        System.out.println("-".repeat(105));

        // Loop through limits
        for (int limit : EDGE_LIMITS) {
            int actualLimit = Math.min(limit, allEdges.length); 
            int[][] subsetEdges = Arrays.copyOfRange(allEdges, 0, actualLimit);
            
            // Find max user ID to set array sizes
            int maxUser = 0;
            for (int[] edge : subsetEdges) {
                maxUser = Math.max(maxUser, Math.max(edge[0], edge[1]));
            }
            int n = maxUser + 1; 

            runRealBenchmark(n, subsetEdges, String.valueOf(actualLimit));
        }
    }

    private static void runRealBenchmark(int n, int[][] edges, String datasetSize) {
        long totalBuildBFS = 0, totalBuildUF = 0;
        long totalQueryBFS = 0, totalQueryUF = 0;
        long totalMemBFS = 0, totalMemUF = 0;

        for (int i = 0; i < RUNS; i++) {
            // Create random queries
            int[][] queries = generateQueries(n);

            // 1. TEST BFS
            forceGC(); 
            long startMemBFS = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            // Time BFS build
            long startBuildBFS = System.nanoTime(); 
            GraphBFS bfsGraph = new GraphBFS(n);
            for (int[] edge : edges) bfsGraph.addEdge(edge[0], edge[1]); 
            long buildTimeBFS = System.nanoTime() - startBuildBFS;
            
            // Time BFS queries
            long startQueryBFS = System.nanoTime();
            for (int[] q : queries) bfsGraph.isConnected(q[0], q[1]); 
            long queryTimeBFS = System.nanoTime() - startQueryBFS;
            
            // Get memory used
            long endMemBFS = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            totalBuildBFS += buildTimeBFS;
            totalQueryBFS += queryTimeBFS;
            totalMemBFS += Math.max(0, endMemBFS - startMemBFS);

            // 2. TEST UNION-FIND
            forceGC(); 
            long startMemUF = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            // Time UF build
            long startBuildUF = System.nanoTime(); 
            UnionFind ufGraph = new UnionFind(n);
            for (int[] edge : edges) ufGraph.union(edge[0], edge[1]); 
            long buildTimeUF = System.nanoTime() - startBuildUF;
            
            // Time UF queries
            long startQueryUF = System.nanoTime(); 
            for (int[] q : queries) ufGraph.isConnected(q[0], q[1]);
            long queryTimeUF = System.nanoTime() - startQueryUF;
            
            // Get memory used
            long endMemUF = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            totalBuildUF += buildTimeUF;
            totalQueryUF += queryTimeUF;
            totalMemUF += Math.max(0, endMemUF - startMemUF);
        }

        // Calculate averages in ms
        double avgBuildBFS = (totalBuildBFS / (double) RUNS) / 1_000_000.0;
        double avgBuildUF  = (totalBuildUF / (double) RUNS) / 1_000_000.0;
        double avgQueryBFS = (totalQueryBFS / (double) RUNS) / 1_000_000.0;
        double avgQueryUF  = (totalQueryUF / (double) RUNS) / 1_000_000.0;
        
        // Convert to MB
        double avgMemBFS = (totalMemBFS / (double) RUNS) / (1024.0 * 1024.0);
        double avgMemUF = (totalMemUF / (double) RUNS) / (1024.0 * 1024.0);

        System.out.printf("%-10s | %-12.3f | %-12.3f | %-14.3f | %-14.3f | %-12.3f | %-12.3f%n", 
                datasetSize, avgBuildBFS, avgBuildUF, avgQueryBFS, avgQueryUF, avgMemBFS, avgMemUF);
    }

    private static void performWarmup(int[][] allEdges) {
        int warmupSize = Math.min(1000, allEdges.length);
        int[][] warmEdges = Arrays.copyOfRange(allEdges, 0, warmupSize);
        int n = 4039; 
        
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
            System.err.println("File not found: " + filename);
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

    // Run garbage collector twice to get accurate memory
    private static void forceGC() {
        System.gc();
        try { Thread.sleep(5); } catch (InterruptedException ignored) {}
        System.gc();
        try { Thread.sleep(5); } catch (InterruptedException ignored) {}
    }
}