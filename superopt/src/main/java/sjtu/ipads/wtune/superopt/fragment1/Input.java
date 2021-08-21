package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.common.utils.TreeNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;

public interface Input extends Op {
  Symbol table();

  @Override
  default OperatorType kind() {
    return OperatorType.INPUT;
  }

  @Override
  default boolean match(PlanNode node, Model m) {
    return m.assign(table(), node);
  }

  @Override
  default PlanNode instantiate(Model m, PlanContext ctx) {
    return TreeNode.copyTree(m.interpretTable(table()), ctx);
  }
}
