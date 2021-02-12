package sjtu.ipads.wtune.superopt.internal;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.plan.OperatorType;
import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.util.Hole;
import sjtu.ipads.wtune.superopt.util.rules.Rule;
import sjtu.ipads.wtune.superopt.util.rules.simplify.NonLeftDeepJoin;
import sjtu.ipads.wtune.superopt.util.rules.support.AllJoin;
import sjtu.ipads.wtune.superopt.util.rules.validation.MalformedJoin;
import sjtu.ipads.wtune.superopt.util.rules.validation.MalformedSubqueryFilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.superopt.internal.Canonicalize.canonicalize;

public class Enumerate {
  public static final int MAX_FRAGMENT_SIZE = 4;

  public static List<Plan> enumPlans() {
    return enumPlans0(0, singleton(Plan.empty())).parallelStream()
        .peek(Plan::setup)
        .sorted(Plan::compareTo)
        .filter(Enumerate::prune)
        .collect(Collectors.toList());
  }

  private static Set<Plan> enumPlans0(int depth, Set<Plan> plans) {
    if (depth >= MAX_FRAGMENT_SIZE) return plans;
    final Set<Plan> newPlans = new HashSet<>();
    for (Plan g : plans)
      for (Hole<PlanNode> hole : g.holes())
        for (PlanNode template : OperatorType.templates())
          if (hole.fill(template)) {
            newPlans.add(canonicalize(g.copy()));
            hole.unFill();
          }

    return Sets.union(newPlans, enumPlans0(depth + 1, newPlans));
  }

  private static boolean prune(Plan plan) {
    return //        !Rule.match(MalformedDistinct.class, graph) &&
    !Rule.match(MalformedSubqueryFilter.class, plan)
        //        && !Rule.match(MalformedSort.class, graph)
        && !Rule.match(MalformedJoin.class, plan)
        //        && !Rule.match(MalformedLimit.class, graph)
        //        && !Rule.match(MalformedUnion.class, graph)
        //        && !Rule.match(DoubleProj.class, graph)
        && !Rule.match(NonLeftDeepJoin.class, plan)
        //                && !Rule.match(AllUnion.class, graph)
        && !Rule.match(AllJoin.class, plan);
  }
}
