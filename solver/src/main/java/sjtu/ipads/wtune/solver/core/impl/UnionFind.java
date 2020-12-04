package sjtu.ipads.wtune.solver.core.impl;

public class UnionFind {
  Node[] node;

  private static class Node {
    int parent;
    boolean root;

    private Node() {
      parent = 1;
      root = true;
    }
  }

  public UnionFind(int n) {
    node = new Node[n + 1];
    for (int e = 0; e <= n; e++) {
      node[e] = new Node();
    }
  }

  public int find(int e) {
    int current = e, p, gp;
    if (node[current].root) {
      return current;
    }
    p = node[current].parent;
    if (node[current].root) {
      return p;
    }
    gp = node[current].parent;

    while (true) {
      node[current].parent = gp;
      if (node[gp].root) {
        return gp;
      }
      current = p;
      p = gp;
      gp = node[p].parent;
    }
  }

  public void union(int a, int b) {
    if (node[a].parent < node[b].parent) {
      node[b].parent += node[a].parent;
      node[a].root = false;
      node[a].parent = a;
    } else {
      node[a].parent += node[b].parent;
      node[b].root = false;
      node[b].parent = a;
    }
  }
}
