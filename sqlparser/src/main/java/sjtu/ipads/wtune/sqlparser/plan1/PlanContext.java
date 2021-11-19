package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.common.tree.UniformTreeContext;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

public interface PlanContext extends UniformTreeContext<PlanKind> {
  PlanNode nodeAt(int id);

  int nodeIdOf(PlanNode node);

  int bindNode(PlanNode node);

  ValuesRegistry valuesReg();

  PlanContext copy();

  default Values valuesOf(PlanNode node) {
    return valuesReg().valuesOf(nodeIdOf(node));
  }

  default PlanNode planRoot() {
    return nodeAt(root());
  }

  static PlanContext mk(Schema schema) {
    return new PlanContextImpl(16, schema);
  }
}
