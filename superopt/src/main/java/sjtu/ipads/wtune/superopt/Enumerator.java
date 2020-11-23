package sjtu.ipads.wtune.superopt;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.InterpretationContext;
import sjtu.ipads.wtune.superopt.operators.Agg;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.Proj;
import sjtu.ipads.wtune.superopt.operators.Sort;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.fineGrainedSchemaEqConstraint;

public class Enumerator {
  public static Set<Graph> enumSkeleton() {
    return Sets.filter(enumSkeleton0(0, singleton(Graph.createEmpty())), Heuristic::pruneSkeleton)
        .parallelStream()
        .peek(Graph::freeze)
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

    final Set<Constraint> globalConstraints = initGlobalConstraints(source, target);
    if (globalConstraints == null) return null;

    final InterpretationContext ctx = InterpretationContext.empty();
    ctx.addConstraints(globalConstraints);
    ctx.addConstraints(source.interpretation().constraints());
    ctx.addConstraints(target.interpretation().constraints());

    final List<Abstraction<?>> sAbstractions = source.abstractions();
    final List<Abstraction<?>> tAbstractions = target.abstractions();

    return emptyList();
  }

  private static Set<Constraint> initGlobalConstraints(Graph source, Graph target) {
    final RelationSchema leftOut = source.head().outSchema();
    final RelationSchema rightOut = target.head().outSchema();

    final Set<Constraint> constraints = fineGrainedSchemaEqConstraint(leftOut, rightOut);
    if (constraints == null) return null;

    constraints.add(Constraint.schemaEq(leftOut, rightOut));
    return constraints;
  }

  private static class ColumnEnumerator implements GraphVisitor {
    private final List<Interpretation> interpretations;

    private ColumnEnumerator(Set<Constraint> constraints) {
      interpretations = new LinkedList<>();
      final Interpretation interpretation = Interpretation.create();
      interpretation.addConstraints(constraints);
      interpretations.add(interpretation);
    }

    @Override
    public void leaveProj(Proj op) {}

    @Override
    public void leaveSort(Sort op) {}

    @Override
    public void leaveAgg(Agg op) {}
  }
}
