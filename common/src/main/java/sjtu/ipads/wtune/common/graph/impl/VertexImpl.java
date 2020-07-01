package sjtu.ipads.wtune.common.graph.impl;

import sjtu.ipads.wtune.common.graph.Graph;
import sjtu.ipads.wtune.common.graph.Vertex;

import java.util.HashMap;
import java.util.Map;

public class VertexImpl implements Vertex {
  private final Graph graph;
  private final int id;

  public VertexImpl(Graph graph, int id) {
    this.graph = graph;
    this.id = id;
  }

  @Override
  public Graph graph() {
    return graph;
  }

  @Override
  public int id() {
    return id;
  }

  private final Map<String, Object> directAttrs = new HashMap<>();

  @Override
  public Map<String, Object> directAttrs() {
    return directAttrs;
  }
}
