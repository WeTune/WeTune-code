package sjtu.ipads.wtune.sqlparser.plan;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.AggNodeImpl;

public interface AggNode extends PlanNode {
  List<ASTNode> groupKeys();

  List<ASTNode> aggregations();

  ASTNode having();

  @Override
  default OperatorType type() {
    return OperatorType.Agg;
  }

  static AggNode make(String qualification, List<ASTNode> aggs, List<ASTNode> groupKeys, ASTNode having) {
    return AggNodeImpl.build(qualification, aggs, groupKeys, having);
  }
}
