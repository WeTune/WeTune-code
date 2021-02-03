package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.superopt.plan.internal.*;

import java.util.List;

public enum OperatorType {
  // Replace this by sealed interface after google-java-format plugin support the future.
  Input(0),
  InnerJoin(2),
  LeftJoin(2),
  PlainFilter(1),
  SubqueryFilter(2),
  Proj(1),
  Agg(1),
  Sort(1),
  Limit(1),
  Union(2),
  Distinct(1);

  private final int numPredecessors;

  OperatorType(int numPredecessors) {
    this.numPredecessors = numPredecessors;
  }

  public static List<PlanNode> templates() {
    return List.of(
        //        Agg.create(),
        //        Distinct.create(),
        InnerJoin.create(),
        LeftJoin.create(),
        //        Limit.create(),
        PlainFilter.create(),
        Proj.create(),
        //        Sort.create(),
        SubqueryFilter.create()
        //        Union.create()
        );
  }

  public int numPredecessors() {
    return numPredecessors;
  }

  public PlanNode create() {
    return switch (this) {
      case Input -> InputImpl.create();
      case InnerJoin -> InnerJoinImpl.create();
      case LeftJoin -> LeftJoinImpl.create();
      case PlainFilter -> PlainFilterImpl.create();
      case SubqueryFilter -> SubqueryFilterImpl.create();
      case Proj -> ProjImpl.create();
      case Agg -> AggImpl.create();
      case Sort -> SortImpl.create();
      case Limit -> LimitImpl.create();
      case Union -> UnionImpl.create();
      case Distinct -> DistinctImpl.create();
    };
  }

  public boolean isValidOutput() {
    return this != InnerJoin && this != LeftJoin && this != PlainFilter && this != SubqueryFilter;
  }

  public boolean isJoin() {
    return this == LeftJoin || this == InnerJoin;
  }

  public boolean isFilter() {
    return this == PlainFilter || this == SubqueryFilter;
  }
}
