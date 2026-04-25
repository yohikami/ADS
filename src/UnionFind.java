public class UnionFind {
    private int[] parent;

    public UnionFind(int n) {
        parent = new int[n]; 
        
        // IDs start at 0
        for (int i = 0; i < n; i++) {
            parent[i] = i; // Everyone is their own boss at first
        }
    }

    // Find the root boss
    public int find(int i) {
        if (parent[i] == i) {
            return i;
        }
        // Path compression to flatten tree
        return parent[i] = find(parent[i]); 
    }

    // Connect two users
    public void union(int i, int j) {
        int rootI = find(i);
        int rootJ = find(j);
        
        // Connect if they have different bosses
        if (rootI != rootJ) {
            parent[rootI] = rootJ; 
        }
    }

    // Check if connected
    public boolean isConnected(int i, int j) {
        return find(i) == find(j); // Same boss means connected
    }
}