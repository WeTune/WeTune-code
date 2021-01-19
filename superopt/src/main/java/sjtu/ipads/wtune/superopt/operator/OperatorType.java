package sjtu.ipads.wtune.superopt.operator;

import sjtu.ipads.wtune.superopt.operator.impl.*;

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

  public static List<Operator> templates() {
    return List.of(
        //        Agg.create(),
        //        Distinct.create(),
        InnerJoin.create(),
        //        LeftJoin.create(),
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

  public Operator create() {
    switch (this) {
      case Input:
        return InputImpl.create();
      case InnerJoin:
        return InnerJoinImpl.create();
      case LeftJoin:
        return LeftJoinImpl.create();
      case PlainFilter:
        return PlainFilterImpl.create();
      case SubqueryFilter:
        return SubqueryFilterImpl.create();
      case Proj:
        return ProjImpl.create();
      case Agg:
        return AggImpl.create();
      case Sort:
        return SortImpl.create();
      case Limit:
        return LimitImpl.create();
      case Union:
        return UnionImpl.create();
      case Distinct:
        return DistinctImpl.create();
      default:
        throw new IllegalArgumentException();
    }
  }

  public boolean isValidOutput() {
    return this != InnerJoin && this != LeftJoin && this != PlainFilter && this != SubqueryFilter;
  }
}
