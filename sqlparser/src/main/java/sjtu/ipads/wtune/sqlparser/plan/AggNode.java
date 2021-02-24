package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.AggNodeImpl;

import java.util.List;

public interface AggNode extends PlanNode {
  List<ASTNode> groupKeys();

  List<ASTNode> aggregations();

  @Override
  default OperatorType type() {
    return OperatorType.Agg;
  }

  static AggNode make(List<PlanAttribute> aggs, List<ASTNode> groupKeys) {
    return AggNodeImpl.build(aggs, groupKeys);
  }
}
