package sjtu.ipads.wtune.superopt.enumerator;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.Heuristic;
import sjtu.ipads.wtune.superopt.Hole;
import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.impl.OutputEstimation;
import sjtu.ipads.wtune.superopt.impl.OutputEstimator;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

public class SkeletonEnumerator {
  public static Set<Graph> enumerate() {
    return Sets.filter(enumerate(0, singleton(Graph.createEmpty())), Heuristic::pruneSkeleton)
        .stream()
        .peek(Graph::setupInputs)
        .filter(it -> it.inputs().size() < 5)
        .collect(Collectors.toSet());
  }

  private static Set<Graph> enumerate(int depth, Set<Graph> graphs) {
    if (depth >= Heuristic.SKELETON_MAX_OPS) return graphs;
    final Set<Graph> newGraphs = new HashSet<>();
    for (Graph g : graphs)
      for (Hole<Operator> hole : g.holes())
        for (Operator template : Operator.templates())
          if (hole.fill(template)) {
            newGraphs.add(g.copy());
            hole.unfill();
          }

    return Sets.union(newGraphs, enumerate(depth + 1, newGraphs));
  }

  public static void main(String[] args) {
    final Collection<Graph> skeletons = enumerate();
    System.out.println(skeletons.size());
    final Map<OutputEstimation, List<Graph>> collect =
        skeletons.stream().collect(Collectors.groupingBy(OutputEstimator::estimateOutput));
    int loopCount = 0;
    for (var pair : OutputEstimation.pairsMayMatch())
      loopCount +=
          collect.getOrDefault(pair.getLeft(), emptyList()).size()
              * collect.getOrDefault(pair.getRight(), emptyList()).size();

    System.out.println(skeletons.size() * skeletons.size());
    System.out.println(loopCount);
    System.out.println();
  }
}
