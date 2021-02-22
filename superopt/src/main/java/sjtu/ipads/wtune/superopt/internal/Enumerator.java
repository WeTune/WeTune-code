package sjtu.ipads.wtune.superopt.internal;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.Operators;
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
import static sjtu.ipads.wtune.superopt.internal.Canonicalization.canonicalize;

public class Enumerator {
  public static final int MAX_FRAGMENT_SIZE = 4;

  public static List<Fragment> enumPlans() {
    return enumPlans0(0, singleton(Fragment.empty())).parallelStream()
        .peek(Fragment::setup)
        .sorted(Fragment::compareTo)
        .filter(Enumerator::prune)
        .collect(Collectors.toList());
  }

  private static Set<Fragment> enumPlans0(int depth, Set<Fragment> fragments) {
    if (depth >= MAX_FRAGMENT_SIZE) return fragments;
    final Set<Fragment> newFragments = new HashSet<>();
    for (Fragment g : fragments)
      for (Hole<Operator> hole : g.holes())
        for (Operator template : Operators.templates())
          if (hole.fill(template)) {
            newFragments.add(canonicalize(g.copy()));
            hole.unFill();
          }

    return Sets.union(newFragments, enumPlans0(depth + 1, newFragments));
  }

  private static boolean prune(Fragment fragment) {
    return //        !Rule.match(MalformedDistinct.class, graph) &&
    !Rule.match(MalformedSubqueryFilter.class, fragment)
        //        && !Rule.match(MalformedSort.class, graph)
        && !Rule.match(MalformedJoin.class, fragment)
        //        && !Rule.match(MalformedLimit.class, graph)
        //        && !Rule.match(MalformedUnion.class, graph)
        //        && !Rule.match(DoubleProj.class, graph)
        && !Rule.match(NonLeftDeepJoin.class, fragment)
        //                && !Rule.match(AllUnion.class, graph)
        && !Rule.match(AllJoin.class, fragment);
  }
}
