package sjtu.ipads.wtune.superopt;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.impl.Hole;
import sjtu.ipads.wtune.superopt.operators.Agg;
import sjtu.ipads.wtune.superopt.operators.Input;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.Proj;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;
import sjtu.ipads.wtune.superopt.relational.impl.AggSchema;
import sjtu.ipads.wtune.superopt.relational.impl.InputSchema;
import sjtu.ipads.wtune.superopt.relational.impl.JoinSchema;
import sjtu.ipads.wtune.superopt.relational.impl.ProjSchema;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.constEq;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.refEq;
import static sjtu.ipads.wtune.superopt.relational.Projections.selectAll;

public class Enumerator {
  public static Set<Graph> enumSkeleton() {
    return Sets.filter(enumSkeleton0(0, singleton(Graph.createEmpty())), Heuristic::pruneSkeleton)
        .stream()
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
    final Set<Constraint> globalConstraints = new HashSet<>();
    final RelationSchema leftOut = source.head().outSchema();
    final RelationSchema rightOut = target.head().outSchema();
    globalConstraints.add(Constraint.schemaEq(leftOut, rightOut));
    if (addFineGrainedSchemaEqConstraint(leftOut, rightOut, globalConstraints) == null) return null;

    return emptyList();
  }

  private static Set<Constraint> addFineGrainedSchemaEqConstraint(
      RelationSchema left, RelationSchema right, Set<Constraint> ret) {
    if (ret == null) ret = new HashSet<>();

    left = left.nonTrivialSource();
    right = right.nonTrivialSource();

    if (left instanceof ProjSchema && right instanceof ProjSchema) {
      ret.add(refEq(((Proj) left.op()).projs(), ((Proj) right.op()).projs()));

    } else if (left instanceof InputSchema && right instanceof InputSchema) {
      ret.add(refEq(((Input) left.op()).relation(), ((Input) right.op()).relation()));

    } else if (left instanceof ProjSchema && right instanceof InputSchema) {
      ret.add(constEq(((Proj) left.op()).projs(), selectAll(((Input) right.op()).relation())));

    } else if (left instanceof InputSchema && right instanceof ProjSchema) {
      ret.add(constEq(((Proj) right.op()).projs(), selectAll(((Input) left.op()).relation())));

    } else if (left instanceof AggSchema && right instanceof AggSchema) {
      ret.add(refEq(((Agg) left.op()).groupKeys(), ((Agg) right.op()).groupKeys()));
      ret.add(refEq(((Agg) left.op()).aggFuncs(), ((Agg) right.op()).aggFuncs()));

    } else if (left instanceof AggSchema && right instanceof InputSchema) {
      return null;

    } else if (left instanceof InputSchema && right instanceof AggSchema) {
      return null;

    } else if (left instanceof JoinSchema && right instanceof InputSchema) {
      return null;

    } else if (left instanceof InputSchema && right instanceof JoinSchema) {
      return null;
    }
    return ret;
  }
}
