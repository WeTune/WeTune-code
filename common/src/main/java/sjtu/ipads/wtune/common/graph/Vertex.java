package sjtu.ipads.wtune.common.graph;

import sjtu.ipads.wtune.common.attrs.Attrs;

public interface Vertex extends Attrs<Vertex> {
  Graph graph();

  int id();
}
