package sjtu.ipads.wtune.superopt.fragment1;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.fragment1.pruning.Rule;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.common.utils.FuncUtils.any;
import static sjtu.ipads.wtune.superopt.fragment1.FragmentUtils.gatherHoles;
import static sjtu.ipads.wtune.superopt.fragment1.FragmentUtils.structuralCompare;

class FragmentEnumerator {
  private final int maxOps;
  private final List<Op> opSet;
  private final Set<Class<? extends Rule>> pruningRules;

  FragmentEnumerator(List<Op> opSet, int maxOps) {
    this.maxOps = maxOps;
    this.opSet = opSet;
    this.pruningRules = new HashSet<>(8);
  }

  void setPruningRules(Iterable<Class<? extends Rule>> rules) {
    rules.forEach(pruningRules::add);
  }

  List<Fragment> enumerate() {
    return enumerate0(0, singleton(Fragment.mk(null))).parallelStream()
        .peek(FragmentUtils::setupFragment)
        .filter(f -> any(pruningRules, it -> Rule.match(it, f)))
        .sorted((x, y) -> structuralCompare(x.root(), y.root()))
        .collect(Collectors.toList());
  }

  private Set<Fragment> enumerate0(int depth, Set<Fragment> fragments) {
    if (depth >= maxOps) return fragments;

    final Set<Fragment> newFragments = new HashSet<>();
    for (Fragment g : fragments)
      for (Hole<Op> hole : gatherHoles(g))
        for (Op template : opSet)
          if (hole.fill(template)) {
            newFragments.add(g.copy());
            hole.unFill();
          }

    return Sets.union(newFragments, enumerate0(depth + 1, newFragments));
  }
}
