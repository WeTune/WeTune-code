package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.common.utils.Copyable;
import sjtu.ipads.wtune.common.utils.NoContextTreeNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;

public interface Op extends NoContextTreeNode<Op>, Comparable<Op>, Copyable<Op> {
  static Op mk(OperatorType type) {
    return switch (type) {
      case INPUT -> new InputOp();
      case INNER_JOIN -> new InnerJoinOp();
      case LEFT_JOIN -> new LeftJoinOp();
      case SIMPLE_FILTER -> new SimpleFilterOp();
      case IN_SUB_FILTER -> new InSubFilterOp();
      case EXISTS_FILTER -> null;
      case PROJ -> new ProjOp();
      case AGG -> new AggOp();
      case SORT -> new SortOp();
      case LIMIT -> new LimitOp();
      case UNION -> new UnionOp();
    };
  }

  static Op parse(String typeName){
    final OperatorType opType = OperatorType.parse(typeName);
    final Op op = Op.mk(opType);
    if (opType == OperatorType.PROJ && typeName.endsWith("*")) {
      ((Proj)op).setDeduplicated(true);
    }
    return op;
  }

  OperatorType kind();

  Fragment fragment();

  void setFragment(Fragment fragment);

  void acceptVisitor(OpVisitor visitor);

  int shadowHash();

  default boolean match(PlanNode node, Model m) {
    throw new UnsupportedOperationException();
  }

  default PlanNode instantiate(Model m, PlanContext ctx) {
    throw new UnsupportedOperationException();
  }

  @Override
  default int compareTo(Op o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final Op[] preds = predecessors(), otherPreds = o.predecessors();
    assert preds.length == otherPreds.length;

    for (int i = 0, bound = preds.length; i < bound; i++) {
      final Op pred = preds[i], otherPred = otherPreds[i];
      if (pred == null && otherPred == null) continue;
      if (pred == null /* && otherPred != null */) return -1;
      if (/* pred != null && */ otherPred == null) return 1;

      res = pred.compareTo(otherPred);
      if (res != 0) return res;
    }

    return 0;
  }
}
