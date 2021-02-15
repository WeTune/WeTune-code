package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.internal.*;

import java.util.List;

public class Operators {
  public static List<Operator> templates() {
    return List.of(
        //        Agg.create(),
        //        Distinct.create(),
        create(OperatorType.InnerJoin),
        create(OperatorType.LeftJoin),
        //        Limit.create(),
        create(OperatorType.PlainFilter),
        create(OperatorType.Proj),
        //        Sort.create(),
        create(OperatorType.SubqueryFilter)
        //        Union.create()
        );
  }

  public static Operator create(OperatorType type) {
    return switch (type) {
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
}
