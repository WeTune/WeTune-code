package sjtu.ipads.wtune.common.graph.impl;

import sjtu.ipads.wtune.common.graph.Edge;
import sjtu.ipads.wtune.common.graph.Graph;
import sjtu.ipads.wtune.common.graph.Vertex;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DirectedGraph implements Graph {
  private final Map<Integer, Vertex> vertexes = new HashMap<>();
  private final Map<Integer, Map<Integer, Edge>> edges = new HashMap<>();
  private int nextId = 0;

  @Override
  public Vertex createVertex() {
    final int id = nextId++;
    final Vertex newVertex = new VertexImpl(this, id);
    vertexes.put(id, newVertex);
    return newVertex;
  }

  @Override
  public Edge createEdge(int id0, int id1) {
    return edges
        .computeIfAbsent(id0, ignored -> new HashMap<>())
        .computeIfAbsent(id1, ignored -> new DirectedEdge(id0, id1));
  }

  @Override
  public Vertex vertex(int id) {
    return vertexes.get(id);
  }

  @Override
  public Edge edge(int id0, int id1) {
    return outEdges0(id0).get(id1);
  }

  @Override
  public Collection<Edge> outEdges(int id) {
    return outEdges0(id).values();
  }

  @Override
  public boolean removeVertex(int id) {
    boolean succeed = vertexes.remove(id) != null;
    edges.remove(id);
    return succeed;
  }

  @Override
  public boolean removeEdge(int id0, int id1) {
    final Map<Integer, Edge> edges = outEdges0(id0);
    if (!edges.isEmpty()) return edges.remove(id1) != null;
    return false;
  }

  @Override
  public Collection<Vertex> vertexes() {
    return vertexes.values();
  }

  private Map<Integer, Edge> outEdges0(int id) {
    return edges.getOrDefault(id, Collections.emptyMap());
  }
}
