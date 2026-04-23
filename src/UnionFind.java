public class UnionFind {
    private int[] parent;

    // O(V) Time
    public UnionFind(int n) {
        parent = new int[n + 1];
        for (int i = 1; i <= n; i++) {
            parent[i] = i; // Every user starts as their own boss
        }
    }

    // O(1) Time
    public int find(int i) {
        if (parent[i] == i) {
            return i;
        }
        // Flattens the tree to speed up future queries
        return parent[i] = find(parent[i]); 
    }

    // Connects two users: O(1) Time
    public void union(int i, int j) {
        int rootI = find(i);
        int rootJ = find(j);
        if (rootI != rootJ) {
            parent[rootI] = rootJ;
        }
    }

    // Checks if two users are in the same network: O(1) Time
    public boolean isConnected(int i, int j) {
        return find(i) == find(j);
    }
}