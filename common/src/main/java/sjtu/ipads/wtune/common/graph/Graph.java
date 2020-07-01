package sjtu.ipads.wtune.common.graph;

import sjtu.ipads.wtune.common.graph.impl.DirectedGraph;

import java.util.Collection;

public interface Graph {
  Vertex createVertex();

  Edge createEdge(int id0, int id1);

  Vertex vertex(int id);

  Edge edge(int id0, int id1);

  Collection<Edge> outEdges(int id);

  boolean removeVertex(int id);

  boolean removeEdge(int id0, int id1);

  Collection<Vertex> vertexes();

  static Graph directed() {
    return new DirectedGraph();
  }
}
