package sjtu.ipads.wtune.superopt.internal;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.operator.Operator;
import sjtu.ipads.wtune.superopt.operator.OperatorType;
import sjtu.ipads.wtune.superopt.rules.Rule;
import sjtu.ipads.wtune.superopt.rules.simplify.DoubleProj;
import sjtu.ipads.wtune.superopt.rules.simplify.NonLeftDeepJoin;
import sjtu.ipads.wtune.superopt.rules.support.AllJoin;
import sjtu.ipads.wtune.superopt.rules.support.AllUnion;
import sjtu.ipads.wtune.superopt.rules.validation.*;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;

public class Enumerate {
  public static final int SKELETON_MAX_OPS = 4;

  public static List<Graph> enumFragments() {
    return enumerate(0, singleton(Graph.empty())).parallelStream()
        .filter(Enumerate::prune)
        .peek(Graph::setup)
        .collect(Collectors.toList());
  }

  private static Set<Graph> enumerate(int depth, Set<Graph> graphs) {
    if (depth >= SKELETON_MAX_OPS) return graphs;
    final Set<Graph> newGraphs = new HashSet<>();
    for (Graph g : graphs)
      for (Hole<Operator> hole : g.holes())
        for (Operator template : OperatorType.templates())
          if (hole.fill(template)) {
            newGraphs.add(g.copy());
            hole.unFill();
          }

    return Sets.union(newGraphs, enumerate(depth + 1, newGraphs));
  }

  private static boolean prune(Graph graph) {
    return !Rule.match(MalformedDistinct.class, graph)
        && !Rule.match(MalformedSubqueryFilter.class, graph)
        && !Rule.match(MalformedSort.class, graph)
        && !Rule.match(MalformedJoin.class, graph)
        && !Rule.match(MalformedLimit.class, graph)
        && !Rule.match(MalformedUnion.class, graph)
        && !Rule.match(DoubleProj.class, graph)
        && !Rule.match(NonLeftDeepJoin.class, graph)
        && !Rule.match(AllUnion.class, graph)
        && !Rule.match(AllJoin.class, graph);
  }
}
