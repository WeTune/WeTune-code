package sjtu.ipads.wtune.sqlparser.plan1;

import java.util.List;

import static java.util.Objects.requireNonNull;

public interface SortNode extends PlanNode {
  List<Expression> sortSpec();

  @Override
  default PlanKind kind() {
    return PlanKind.Sort;
  }

  static SortNode mk(List<Expression> sortSpec) {
    return new SortNodeImpl(requireNonNull(sortSpec));
  }
}