package sjtu.ipads.wtune.solver.node;

import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.node.impl.UnionNodeImpl;

import java.util.Arrays;
import java.util.List;

public interface UnionNode extends AlgNode {
  @Override
  default List<SymbolicColumnRef> projected() {
    return filtered();
  }

  static UnionNode union(AlgNode... inputs) {
    return UnionNodeImpl.create(true, Arrays.asList(inputs));
  }
}
