package sjtu.ipads.wtune.solver.node;

import sjtu.ipads.wtune.solver.node.impl.TableNodeImpl;
import sjtu.ipads.wtune.solver.schema.Table;

public interface TableNode extends AlgNode {
  Table table();

    static TableNode create(Table table) {
    return TableNodeImpl.create(table);
  }
}
