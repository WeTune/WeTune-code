package sjtu.ipads.wtune.solver.sql;

import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.node.TableNode;
import sjtu.ipads.wtune.solver.schema.Column;

public interface NativeColumnRef extends ColumnRef {
  TableNode source();

  Column column();

  @Override
  NativeColumnRef setOwner(AlgNode owner);

  @Override
  NativeColumnRef copy();
}
