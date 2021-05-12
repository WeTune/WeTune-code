package sjtu.ipads.wtune.superopt.fragment;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.internal.AggImpl;
import sjtu.ipads.wtune.superopt.fragment.internal.DistinctImpl;
import sjtu.ipads.wtune.superopt.fragment.internal.InnerJoinImpl;
import sjtu.ipads.wtune.superopt.fragment.internal.InputImpl;
import sjtu.ipads.wtune.superopt.fragment.internal.LeftJoinImpl;
import sjtu.ipads.wtune.superopt.fragment.internal.LimitImpl;
import sjtu.ipads.wtune.superopt.fragment.internal.PlainFilterImpl;
import sjtu.ipads.wtune.superopt.fragment.internal.ProjImpl;
import sjtu.ipads.wtune.superopt.fragment.internal.SortImpl;
import sjtu.ipads.wtune.superopt.fragment.internal.SubqueryFilterImpl;
import sjtu.ipads.wtune.superopt.fragment.internal.UnionImpl;

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
        , create(OperatorType.Union)
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
