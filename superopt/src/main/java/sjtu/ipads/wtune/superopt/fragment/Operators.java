package sjtu.ipads.wtune.superopt.fragment;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.internal.AggImpl;
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
        create(OperatorType.INNER_JOIN),
        create(OperatorType.LEFT_JOIN),
        //        Limit.create(),
        create(OperatorType.SIMPLE_FILTER),
        create(OperatorType.PROJ),
        //        Sort.create(),
        create(OperatorType.IN_SUB_FILTER)
        , create(OperatorType.UNION)
        );
  }

  public static Operator create(OperatorType type) {
    return switch (type) {
      case INPUT -> InputImpl.create();
      case INNER_JOIN -> InnerJoinImpl.create();
      case LEFT_JOIN -> LeftJoinImpl.create();
      case SIMPLE_FILTER -> PlainFilterImpl.create();
      case IN_SUB_FILTER -> SubqueryFilterImpl.create();
      case EXISTS_FILTER -> null;
      case PROJ -> ProjImpl.create();
      case AGG -> AggImpl.create();
      case SORT -> SortImpl.create();
      case LIMIT -> LimitImpl.create();
      case UNION -> UnionImpl.create();
    };
  }
}
