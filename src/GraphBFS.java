import java.util.*;

public class GraphBFS {
    // Map to store users and their friends
    private Map<Integer, List<Integer>> adjList;

    public GraphBFS(int n) {
        adjList = new HashMap<>(n);
    }

    // Add a 2-way friendship
    public void addEdge(int u, int v) {
        
        // If user u is new, make an empty list
        if (!adjList.containsKey(u)) {
            adjList.put(u, new ArrayList<>());
        }
        // Add v to u's list
        adjList.get(u).add(v);

        // If user v is new, make an empty list
        if (!adjList.containsKey(v)) {
            adjList.put(v, new ArrayList<>());
        }
        // Add u to v's list
        adjList.get(v).add(u);
    }

    // BFS search
    public boolean isConnected(int start, int target) {
        // Check if it's the same person
        if (start == target) return true;
        
        // Check if users actually exist in graph
        if (!adjList.containsKey(start) || !adjList.containsKey(target)) return false;

        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        // Start queue
        queue.add(start);
        visited.add(start);

        // Loop until queue is empty
        while (!queue.isEmpty()) {
            int current = queue.poll();
            
            if (current == target) return true; // Found them

            // Check all friends
            for (int friend : adjList.getOrDefault(current, Collections.emptyList())) {
                if (visited.add(friend)) { // If not visited yet
                    queue.add(friend);
                }
            }
        }
        return false; // Not connected
    }
}