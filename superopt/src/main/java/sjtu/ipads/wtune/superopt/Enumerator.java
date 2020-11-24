package sjtu.ipads.wtune.superopt;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.*;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.TRACE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.superopt.Helper.prepend;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.refEq;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.refNonEq;

public class Enumerator {
  private static final System.Logger LOG = System.getLogger("Enumerator");

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

  private static class Logger {
    private final Graph source;
    private final Graph target;

    private Logger(Graph source, Graph target) {
      this.source = source;
      this.target = target;
    }

    private void log(System.Logger.Level level, String format, Object... args) {
      if (LOG.isLoggable(level))
        LOG.log(level, "{0}-{1} " + format, prepend(args, source.name(), target.name()));
    }
  }

  public static Set<Substitution> enumSubstitution(Graph source, Graph target) {
    if (source == target) return null;
    final Logger l = new Logger(source, target);
    l.log(TRACE, "BEGIN {2} | {3}", source, target);

    final List<Interpretation> sParams = source.interpretations();
    final List<Interpretation> tParams = target.interpretations();
    l.log(TRACE, "#parameterized {2} {3}", sParams.size(), tParams.size());

    final ConstraintSet precondition = initGlobalConstraints(source, target);

    final List<ConstraintSet> sLocalInputs = enumLocalInputConstraint(source);
    final List<ConstraintSet> tLocalInputs = enumLocalInputConstraint(target);
    l.log(TRACE, "#input_local {2} {3}", sLocalInputs.size(), tLocalInputs.size());

    final List<ConstraintSet> inputMappings = enumInputMappingConstraint(source, target);
    l.log(TRACE, "#input_mapping {2}", inputMappings.size());

    final List<List<ConstraintSet>> sLocalColumnSets = enumLocalColumnConstraint(source);
    final List<List<ConstraintSet>> tLocalColumnSets = enumLocalColumnConstraint(target);

    final Set<Substitution> ret = new HashSet<>();

    int loops = 0;
    for (List<ConstraintSet> inputs :
        Lists.cartesianProduct(sLocalInputs, tLocalInputs, inputMappings)) {
      final ConstraintSet inputConstraint = ConstraintSet.fromCopy(precondition);
      if (!inputConstraint.addAll(inputs.get(0))) continue;
      if (!inputConstraint.addAll(inputs.get(1))) continue;
      if (!inputConstraint.addAll(inputs.get(2))) continue;

      for (int i = 0; i < sParams.size(); i++) {
        for (int j = 0; j < tParams.size(); j++) {
          final Interpretation sParam = sParams.get(i);
          final Interpretation tParam = tParams.get(j);

          final List<ConstraintSet> sLocalColumns = sLocalColumnSets.get(i);
          final List<ConstraintSet> tLocalColumns = tLocalColumnSets.get(j);

          final List<ConstraintSet> columnMappings = enumColumnMappingConstraint(sParam, tParam);
          for (List<ConstraintSet> columns :
              Lists.cartesianProduct(sLocalColumns, tLocalColumns, columnMappings)) {
            final ConstraintSet finalConstraint = ConstraintSet.fromCopy(inputConstraint);
            if (!finalConstraint.addAll(columns.get(0))) continue;
            if (!finalConstraint.addAll(columns.get(1))) continue;
            if (!finalConstraint.addAll(columns.get(2))) continue;
            if (!finalConstraint.checkInterpretation(sParam)) continue;
            if (!finalConstraint.checkInterpretation(tParam)) continue;

            final Substitution substitution =
                Substitution.create(source, target, sParam, tParam, finalConstraint);
            ret.add(substitution);
            loops++;
          }
        }
      }
    }

    l.log(TRACE, "#loops {2}", loops);
    l.log(TRACE, "#substituion {2}", ret.size());
    return ret;
    //    return emptyList();
  }

  private static ConstraintSet initGlobalConstraints(Graph source, Graph target) {
    final RelationSchema leftOut = source.head().outSchema();
    final RelationSchema rightOut = target.head().outSchema();

    final List<Constraint> constraints = Constraint.schemaEq(leftOut, rightOut);

    final ConstraintSet set = ConstraintSet.empty();
    set.addAll(constraints);
    set.addAll(source.constraints());
    set.addAll(target.constraints());
    return set;
  }

  private static List<ConstraintSet> enumLocalInputConstraint(Graph graph) {
    return PartitionConstraintEnumerator.enumerate(
        graph.inputs(),
        (x, y, same) ->
            singleton(same ? refEq(x.source(), y.source()) : refNonEq(x.source(), y.source())));
  }

  private static List<ConstraintSet> enumInputMappingConstraint(Graph source, Graph target) {
    return MatchConstraintEnumerator.enumerate(
        source.inputs(), target.inputs(), (x, y) -> singleton(refEq(x.source(), y.source())));
  }

  private static List<List<ConstraintSet>> enumLocalColumnConstraint(Graph graph) {
    return graph.interpretations().stream()
        .map(Enumerator::enumLocalColumnConstraint0)
        .collect(Collectors.toList());
  }

  private static List<ConstraintSet> enumColumnMappingConstraint(
      Interpretation sInterpretation, Interpretation tInterpretation) {
    return MatchConstraintEnumerator.enumerate(
        collectColumns(sInterpretation), collectColumns(tInterpretation), Enumerator::columnEq);
  }

  private static List<ConstraintSet> enumLocalColumnConstraint0(Interpretation interpretation) {
    return PartitionConstraintEnumerator.enumerate(
        collectColumns(interpretation), (x, y, same) -> same ? columnEq(x, y) : columnNonEq(x, y));
  }

  private static List<SymbolicColumns> collectColumns(Interpretation interpretation) {
    final Set<Abstraction<?>> abstractions = interpretation.abstractions();
    final List<SymbolicColumns> columns = new ArrayList<>();

    for (Abstraction<?> abstraction : abstractions) {
      final Object assignment = interpretation.interpret(abstraction);
      if (assignment instanceof SubqueryPredicate)
        addSymbolicColumns(columns, ((SubqueryPredicate) assignment).columns());
      else if (assignment instanceof PlainPredicate)
        addSymbolicColumns(columns, ((PlainPredicate) assignment).columns());
      else if (assignment instanceof Projections)
        addSymbolicColumns(columns, ((Projections) assignment).columns());
    }

    return columns;
  }

  private static void addSymbolicColumns(List<SymbolicColumns> columns, SymbolicColumns c) {
    if (c != null) columns.addAll(c.flatten());
  }

  private static void addSymbolicColumns(
      List<SymbolicColumns> columns, Collection<SymbolicColumns> toAdd) {
    for (SymbolicColumns c : toAdd) addSymbolicColumns(columns, c);
  }

  private static Collection<Constraint> columnEq(SymbolicColumns c0, SymbolicColumns c1) {
    final Abstraction<InputSource> rel0 = c0.relation();
    final Abstraction<InputSource> rel1 = c1.relation();
    // TODO
    if (rel0 == null || rel1 == null) return emptyList();
    return List.of(refEq(rel0, rel1), refEq(c0.abstractions(), c1.abstractions()));
  }

  private static Collection<Constraint> columnNonEq(SymbolicColumns c0, SymbolicColumns c1) {
    final Abstraction<InputSource> rel0 = c0.relation();
    final Abstraction<InputSource> rel1 = c1.relation();
    // TODO
    if (rel0 == null || rel1 == null) return emptyList();
    return List.of(refNonEq(c0.abstractions(), c1.abstractions()));
  }

  @FunctionalInterface
  private interface PartitionEnforcer<T> {
    Collection<Constraint> enforce(T x, T y, boolean inSamePartition);
  }

  private interface MatchEnforcer<T> {
    Collection<Constraint> enforce(T x, T y);
  }

  private static class PartitionConstraintEnumerator<T>
      implements Helper.SetPartitionEnumerator<T> {
    private final PartitionEnforcer<T> enforcer;
    private List<ConstraintSet> constraints;
    private ConstraintSet currentConstraint;

    private PartitionConstraintEnumerator(PartitionEnforcer<T> enforcer) {
      this.enforcer = enforcer;
    }

    @Override
    public boolean checkPartition(T x, T y, boolean inSamePartition) {
      final Collection<Constraint> constraint = enforcer.enforce(x, y, inSamePartition);
      return currentConstraint.addAll(constraint);
    }

    @Override
    public void beginEnum(int size) {
      constraints = new ArrayList<>(size);
    }

    @Override
    public void beginPartition() {
      currentConstraint = ConstraintSet.empty();
    }

    @Override
    public void endPartition() {
      constraints.add(currentConstraint);
    }

    public static <T> List<ConstraintSet> enumerate(List<T> ts, PartitionEnforcer<T> enforcer) {
      final PartitionConstraintEnumerator<T> enumerator =
          new PartitionConstraintEnumerator<>(enforcer);
      enumerator.enumPartitions(ts);
      return enumerator.constraints;
    }
  }

  private static class MatchConstraintEnumerator<T> implements Helper.MatchEnumerator<T> {
    private final MatchEnforcer<T> enforcer;
    private List<ConstraintSet> constraints;
    private ConstraintSet currentConstraint;

    private MatchConstraintEnumerator(MatchEnforcer<T> enforcer) {
      this.enforcer = enforcer;
    }

    @Override
    public void beginEnum(int size) {
      constraints = new ArrayList<>(size);
    }

    @Override
    public void beginMatch() {
      currentConstraint = ConstraintSet.empty();
    }

    @Override
    public void endMatch() {
      constraints.add(currentConstraint);
    }

    @Override
    public boolean checkMatch(T x, T y) {
      final Collection<Constraint> constraints = enforcer.enforce(x, y);
      return currentConstraint.addAll(constraints);
    }

    public static <T> List<ConstraintSet> enumerate(
        List<T> xs, List<T> ys, MatchEnforcer<T> enforcer) {
      final MatchConstraintEnumerator<T> enumerator = new MatchConstraintEnumerator<>(enforcer);
      enumerator.enumerate(xs, ys);
      return enumerator.constraints;
    }
  }
}
