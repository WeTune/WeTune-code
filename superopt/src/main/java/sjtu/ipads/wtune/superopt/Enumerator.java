package sjtu.ipads.wtune.superopt;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Input;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.*;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.nCopies;
import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.superopt.Helper.listMap;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.refEq;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.refNonEq;

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

    final ConstraintSet globalConstraints = initGlobalConstraints(source, target);
    if (globalConstraints == null) return null;

    globalConstraints.addAll(source.constraints());
    globalConstraints.addAll(target.constraints());

    final List<ConstraintSet> sLocalInputConstraints =
        enumLocalInputConstraint(source, globalConstraints);
    final List<ConstraintSet> tLocalInputConstraints =
        enumLocalInputConstraint(target, globalConstraints);

    final List<ConstraintSet> inputMappingConstraints = enumInputMappingConstraint(source, target);

    final List<Interpretation> sInterpretations = source.interpretations();
    final List<Interpretation> tInterpretations = target.interpretations();

    final List<List<ConstraintSet>> sLocalColumnConstraints = enumLocalColumnConstraint(source);
    final List<List<ConstraintSet>> tLocalColumnConstraints = enumLocalColumnConstraint(target);

    final List<Substitution> ret = new ArrayList<>();

    int loops = 0;
    for (ConstraintSet sLocalInput : sLocalInputConstraints) {
      for (ConstraintSet tLocalInput : tLocalInputConstraints) {
        for (ConstraintSet inputMapping : inputMappingConstraints) {
          final ConstraintSet inputConstraint = ConstraintSet.fromCopy(globalConstraints);
          if (!inputConstraint.addAll(sLocalInput)) continue;
          if (!inputConstraint.addAll(tLocalInput)) continue;
          if (!inputMapping.addAll(inputMapping)) continue;

          for (int i = 0; i < sInterpretations.size(); i++) {
            for (int j = 0; j < tInterpretations.size(); j++) {

              final Interpretation sInterpretation = sInterpretations.get(i);
              final Interpretation tInterpretation = tInterpretations.get(j);

              final List<ConstraintSet> columnMappingConstraint =
                  enumColumnMappingConstraint(sInterpretation, tInterpretation);

              final List<ConstraintSet> sLocalColumnConstraint = sLocalColumnConstraints.get(i);
              final List<ConstraintSet> tLocalColumnConstraint = tLocalColumnConstraints.get(j);

              for (ConstraintSet sLocalColumn : sLocalColumnConstraint) {
                for (ConstraintSet tLocalColumn : tLocalColumnConstraint) {
                  for (ConstraintSet columnMapping : columnMappingConstraint) {
                    final ConstraintSet finalConstraints = ConstraintSet.fromCopy(inputConstraint);
                    if (!finalConstraints.addAll(sLocalColumn)) continue;
                    if (!finalConstraints.addAll(tLocalColumn)) continue;
                    if (!finalConstraints.addAll(columnMapping)) continue;
                    if (!finalConstraints.checkInterpretation(sInterpretation)) continue;
                    if (!finalConstraints.checkInterpretation(tInterpretation)) continue;

                    final Substitution substitution =
                        Substitution.create(
                            source, target, sInterpretation, tInterpretation, finalConstraints);

                    ret.add(substitution);

                    loops++;
                  }
                }
              }
            }
          }
        }
      }
    }

    System.out.println("== source params ==");
    sInterpretations.forEach(System.out::println);
    System.out.println(source);
    System.out.println(sInterpretations.size());

    System.out.println("== target params ==");
    tInterpretations.forEach(System.out::println);
    System.out.println(target);
    System.out.println(tInterpretations.size());

    System.out.println("== global constraint ==");
    System.out.println(globalConstraints);

    System.out.println("== local input constraint ==");
    System.out.println(sLocalInputConstraints);
    System.out.println(tLocalInputConstraints);

    System.out.println("== input mapping constraints ==");
    System.out.println(inputMappingConstraints);

    System.out.println(loops);

    return ret;
    //        return emptyList();
  }

  private static ConstraintSet initGlobalConstraints(Graph source, Graph target) {
    final RelationSchema leftOut = source.head().outSchema();
    final RelationSchema rightOut = target.head().outSchema();

    final List<Constraint> constraints = Constraint.schemaEq(leftOut, rightOut);
    if (constraints == null) return null;

    final ConstraintSet set = ConstraintSet.empty();
    set.addAll(constraints);
    return set;
  }

  private static List<ConstraintSet> enumLocalInputConstraint(Graph graph, ConstraintSet global) {
    final List<Abstraction<Relation>> inputs = listMap(Input::relation, graph.inputs());
    final int[][] bits = Helper.setPartition(inputs.size());

    final List<ConstraintSet> constraints = new ArrayList<>(bits.length);

    outer:
    for (int[] bit : bits) {
      final ConstraintSet set = ConstraintSet.empty();

      for (int i = 0; i < bit.length; i++)
        for (int j = i + 1; j < bit.length; j++) {
          final Constraint constraint =
              bit[i] == bit[j]
                  ? refEq(inputs.get(i), inputs.get(j))
                  : refNonEq(inputs.get(i), inputs.get(j));

          if (!global.checkNonConflict(constraint)) continue outer;
          set.add(constraint);
        }
      constraints.add(set);
    }

    return constraints;
  }

  private static List<ConstraintSet> enumInputMappingConstraint(Graph source, Graph target) {
    final List<Input> sInputs = source.inputs();
    final List<Input> tInputs = target.inputs();

    final List<ConstraintSet> ret = new ArrayList<>((int) Math.pow(sInputs.size(), tInputs.size()));
    for (List<Input> inputs : Lists.cartesianProduct(nCopies(tInputs.size(), sInputs))) {
      final ConstraintSet constraints = ConstraintSet.empty();

      for (int i = 0; i < tInputs.size(); i++) {
        final Input sInput = inputs.get(i);
        final Input tInput = tInputs.get(i);
        constraints.add(refEq(sInput.relation(), tInput.relation()));
      }

      ret.add(constraints);
    }

    return ret;
  }

  private static List<List<ConstraintSet>> enumLocalColumnConstraint(Graph graph) {
    return graph.interpretations().stream()
        .map(Enumerator::enumLocalColumnConstraint0)
        .collect(Collectors.toList());
  }

  private static List<ConstraintSet> enumColumnMappingConstraint(
      Interpretation sourceInterpretation, Interpretation targetInterpretation) {
    final List<SymbolicColumns> sColumns = collectColumns(sourceInterpretation);
    final List<SymbolicColumns> tColumns = collectColumns(targetInterpretation);

    final List<ConstraintSet> ret =
        new ArrayList<>((int) Math.pow(sColumns.size(), tColumns.size()));

    for (List<SymbolicColumns> columns :
        Lists.cartesianProduct(nCopies(tColumns.size(), sColumns))) {
      final ConstraintSet set = ConstraintSet.empty();
      ret.add(set);

      for (int i = 0; i < columns.size(); i++) {
        final SymbolicColumns sColumn = columns.get(i);
        final SymbolicColumns tColumn = tColumns.get(i);
        columnEq(set, sColumn, tColumn);
      }
    }

    return ret;
  }

  private static List<ConstraintSet> enumLocalColumnConstraint0(Interpretation interpretation) {
    final List<SymbolicColumns> columns = collectColumns(interpretation);
    final int[][] bits = Helper.setPartition(columns.size());
    final List<ConstraintSet> ret = new ArrayList<>();

    outer:
    for (int[] bit : bits) {
      final ConstraintSet set = ConstraintSet.empty();

      for (int i = 0; i < bit.length; i++)
        for (int j = i + 1; j < bit.length; j++)
          if (bit[i] == bit[j]) {
            if (!columnEq(set, columns.get(i), columns.get(j))) continue outer;
          } else {
            if (!columnNonEq(set, columns.get(i), columns.get(j))) continue outer;
          }

      ret.add(set);
    }

    return ret;
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

  private static boolean columnEq(ConstraintSet existing, SymbolicColumns c0, SymbolicColumns c1) {
    final Abstraction<Relation> rel0 = c0.relation();
    final Abstraction<Relation> rel1 = c1.relation();
    // TODO
    if (rel0 == null || rel1 == null) return true;
    return existing.add(refEq(rel0, rel1))
        && existing.add(refEq(c0.abstractions(), c1.abstractions()));
  }

  private static boolean columnNonEq(
      ConstraintSet existing, SymbolicColumns c0, SymbolicColumns c1) {
    final Abstraction<Relation> rel0 = c0.relation();
    final Abstraction<Relation> rel1 = c1.relation();
    // TODO
    if (rel0 == null || rel1 == null) return true;
    if (Objects.equals(rel0, rel1))
      return existing.add(refNonEq(c0.abstractions(), c1.abstractions()));
    else return existing.add(refNonEq(rel0, rel1));
  }
}
