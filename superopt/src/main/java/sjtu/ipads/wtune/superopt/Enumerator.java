package sjtu.ipads.wtune.superopt;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.nCopies;
import static java.util.Collections.singleton;

public class Enumerator {
  public static Set<Graph> enumSkeleton() {
    return enumSkeleton0(0, singleton(Graph.createEmpty())).parallelStream()
        .filter(Heuristic::pruneSkeleton)
        .map(Graph::freeze)
        .filter(Objects::nonNull)
        .filter(it -> it.inputs().size() < 5)
        .collect(Collectors.toSet());
  }

  private static Set<Graph> enumSkeleton0(int depth, Set<Graph> graphs) {
    if (depth >= Heuristic.SKELETON_MAX_OPS) return graphs;
    final Set<Graph> newGraphs = new HashSet<>();
    for (Graph g : graphs)
      for (Hole<Operator> hole : g.holes())
        for (Operator template : Operator.templates())
          if (hole.fill(template)) {
            newGraphs.add(g.copy());
            hole.unfill();
          }

    return Sets.union(newGraphs, enumSkeleton0(depth + 1, newGraphs));
  }

  public static List<Substitution> enumSubstitution(Graph source, Graph target) {
    if (source == target) return null;
    if (source.constraints() == null) System.out.println();

    final List<Constraint> globalConstraints = initGlobalConstraints(source, target);
    if (globalConstraints == null) return null;

    final Interpretation ctx = Interpretation.create();
    ctx.addConstraints(globalConstraints);
    ctx.addConstraints(source.constraints());
    ctx.addConstraints(target.constraints());

    //    final List<Abstraction<?>> sAbstractions = source.abstractions();
    //    final List<Abstraction<?>> tAbstractions = target.abstractions();
    final List<Interpretation> sCols = source.interpretations();
    final List<Interpretation> tCols = target.interpretations();

    //    System.out.println(source);
    //    System.out.println(sCols);
    return nCopies(sCols.size() * tCols.size(), null);
    //    return emptyList();
  }

  private static List<Constraint> initGlobalConstraints(Graph source, Graph target) {
    final RelationSchema leftOut = source.head().outSchema();
    final RelationSchema rightOut = target.head().outSchema();
    return Constraint.schemaEq(leftOut, rightOut);
  }
}
