package sjtu.ipads.wtune.common.graph.impl;

import sjtu.ipads.wtune.common.graph.Edge;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DirectedEdge implements Edge {
  private final int v0;
  private final int v1;

  DirectedEdge(int v0, int v1) {
    this.v0 = v0;
    this.v1 = v1;
  }

  @Override
  public int _this() {
    return v0;
  }

  @Override
  public int _that() {
    return v1;
  }

  private final Map<String, Object> directAttrs = new HashMap<>();

  @Override
  public Map<String, Object> directAttrs() {
    return directAttrs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Edge)) return false;

    final Edge edge = (Edge) o;
    return _this() == edge._this() && _that() == edge._that();
  }

  @Override
  public int hashCode() {
    return Objects.hash(_this(), _that());
  }
}
