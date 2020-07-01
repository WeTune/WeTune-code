package sjtu.ipads.wtune.common.graph;

import sjtu.ipads.wtune.common.attrs.Attrs;

import java.util.Objects;

public interface Edge extends Attrs<Edge> {
  int _this();

  int _that();
}
