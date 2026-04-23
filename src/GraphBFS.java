import java.util.*;

public class GraphBFS {
    // Adjacency List: Maps a User ID to a List of their friends
    private Map<Integer, List<Integer>> adjList;

    public GraphBFS(int n) {
        adjList = new HashMap<>(n);
    }

    // Adds an undirected friendship: O(1) Time
    public void addEdge(int u, int v) {
        adjList.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
        adjList.computeIfAbsent(v, k -> new ArrayList<>()).add(u);
    }

    // Breadth-First Search Traversal
    // Time Complexity: O(V + E)
    public boolean isConnected(int start, int target) {
        if (start == target) return true;
        if (!adjList.containsKey(start) || !adjList.containsKey(target)) return false;

        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            
            if (current == target) return true;

            // Loop Frequency: V + 2E
            for (int friend : adjList.getOrDefault(current, Collections.emptyList())) {
                if (visited.add(friend)) {
                    queue.add(friend);
                }
            }
        }
        return false;
    }
}