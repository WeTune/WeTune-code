package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;
import sjtu.ipads.wtune.superopt.operators.Input;
import sjtu.ipads.wtune.superopt.operators.PlainFilter;
import sjtu.ipads.wtune.superopt.operators.Proj;
import sjtu.ipads.wtune.superopt.operators.SubqueryFilter;
import sjtu.ipads.wtune.superopt.relational.*;
import sjtu.ipads.wtune.superopt.relational.impl.InSubqueryPredicate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.superopt.Helper.cartesianProductStream;
import static sjtu.ipads.wtune.superopt.Helper.pack;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.refEq;
import static sjtu.ipads.wtune.superopt.operators.Operator.*;

public class Main {
  private static final System.Logger LOG = System.getLogger("Enumerator");

  private static final String LOGGER_CONFIG =
      ".level = FINER\n"
          + "java.util.logging.ConsoleHandler.level = FINER\n"
          + "handlers=java.util.logging.ConsoleHandler\n"
          + "java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\n"
          + "java.util.logging.SimpleFormatter.format=[%1$tm/%1$td %1$tT][%3$10s][%4$s] %5$s %n\n";

  static {
    try {
      LogManager.getLogManager()
          .readConfiguration(new ByteArrayInputStream(LOGGER_CONFIG.getBytes()));
    } catch (IOException ignored) {
    }
  }

  public static void main(String[] args) {
    //    main0();
    //    test0();
    //    test1();
    test2();
    //    test3();
  }

  private static void main0() {
    final Set<Graph> skeletons = Enumerator.enumSkeleton();
    LOG.log(System.Logger.Level.INFO, "#skeletons = {0}", skeletons.size());

    final List<Substitution> substitutions =
        cartesianProductStream(skeletons, skeletons)
            .map(pack(Enumerator::enumSubstitution))
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    LOG.log(System.Logger.Level.INFO, "#constraints = {0}", substitutions.size());
  }

  public static void test0() {
    final Graph source = subqueryFilter(null, proj(plainFilter(null))).toGraph("S");
    final Graph target = subqueryFilter(null, proj(null)).toGraph("T");

    int count = 0;
    final Set<Substitution> substitutions = Enumerator.enumSubstitution(source, target);

    final SubqueryFilter sSubquery = ((SubqueryFilter) source.head());
    final Proj sProj = ((Proj) sSubquery.prev()[1]);
    final PlainFilter sFilter = (PlainFilter) sProj.prev()[0];
    final Input sInput1 = (Input) sFilter.prev()[0];
    final Input sInput0 = (Input) sSubquery.prev()[0];

    final SubqueryFilter tSubquery = ((SubqueryFilter) target.head());
    final Proj tProj = ((Proj) tSubquery.prev()[1]);
    final Input tInput1 = (Input) tProj.prev()[0];
    final Input tInput0 = (Input) tSubquery.prev()[0];

    final List<Substitution> filtered = new ArrayList<>();
    for (Substitution substitution : substitutions) {
      final Interpretation sInterpretation = substitution.sourceInterpretation();
      final Interpretation tInterpretation = substitution.targetInterpretation();
      final ConstraintSet constraints = substitution.constraints();

      final SubqueryPredicate sPredicate = sInterpretation.interpret(sSubquery.predicate());
      final SubqueryPredicate tPredicate = tInterpretation.interpret(tSubquery.predicate());
      if (!(sPredicate instanceof InSubqueryPredicate)) continue;
      if (!(tPredicate instanceof InSubqueryPredicate)) continue;

      final MonoSourceColumnSet sSubqueryCol = sPredicate.columns().flatten().iterator().next();
      final MonoSourceColumnSet tSubqueryCol = tPredicate.columns().flatten().iterator().next();

      final Constraint c0 = refEq(sInput0.source(), tInput0.source());
      final Constraint c1 = refEq(sInput1.source(), tInput1.source());
      final Constraint c2 = refEq(sSubqueryCol.abstractions(), tSubqueryCol.abstractions());
      final Constraint c3 = refEq(sSubqueryCol.source(), tSubqueryCol.source());

      if (!(constraints.contains(c0))) continue;
      if (!(constraints.contains(c1))) continue;
      if (!(constraints.contains(c2))) continue;
      if (!(constraints.contains(c3))) continue;

      final Projections tProjections = tInterpretation.interpret(tProj.projs());
      final Projections sProjections = sInterpretation.interpret(sProj.projs());

      final MonoSourceColumnSet tProjCol = tProjections.columns().flatten().iterator().next();
      final MonoSourceColumnSet sProjCol = sProjections.columns().flatten().iterator().next();

      final Constraint c4 = refEq(tProjCol.abstractions(), sProjCol.abstractions());
      final Constraint c5 = refEq(tProjCol.source(), sProjCol.source());
      final Constraint c6 = refEq(tProjCol.source(), tInput1.source());

      if (!(constraints.contains(c4))) continue;
      if (!(constraints.contains(c5))) continue;
      if (!(constraints.contains(c6))) continue;

      final PlainPredicate pred = sInterpretation.interpret(sFilter.predicate());
      final List<MonoSourceColumnSet> columns =
          pred.columns().stream()
              .map(ColumnSet::flatten)
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
      if (columns.size() != 2) continue;
      final MonoSourceColumnSet sFilterCol0 = columns.get(0);
      final MonoSourceColumnSet sFilterCol1 = columns.get(1);

      final Constraint c7 = refEq(sFilterCol0.source(), sProjCol.source());
      final Constraint c8 = refEq(sFilterCol0.abstractions(), sProjCol.abstractions());
      if (!constraints.contains(c7)) continue;
      if (!constraints.contains(c8)) continue;

      final Constraint c9 = refEq(sFilterCol1.source(), sSubqueryCol.source());
      final Constraint c10 = refEq(sFilterCol1.abstractions(), sSubqueryCol.abstractions());
      if (!constraints.contains(c9)) continue;
      if (!constraints.contains(c10)) continue;
      count++;
      filtered.add(substitution);
    }
    System.out.println(count);
    filtered.forEach(Substitution::decorated);
    System.out.println(filtered);
    //    System.out.println(filtered.get(0).decorated());

    //    final Set<Graph> skeletons = Enumerator.enumSkeleton();
    //    System.out.println("#skeletons: " + skeletons.size());
    //    final int sum = skeletons.stream().map(Graph::interpretations).mapToInt(List::size).sum();
    //    System.out.println("#count: " + sum);
  }

  public static void test1() {
    final Graph source = distinct(subqueryFilter(null, proj(plainFilter(null)))).toGraph("S");
    final Graph target = distinct(proj(plainFilter(plainFilter(join(null, null))))).toGraph("T");

    selectSource(source);
    selectTarget(target);

    System.out.println();
    final Set<Substitution> substitutions = Enumerator.enumSubstitution(source, target);
    final Substitution next = substitutions.iterator().next();
    System.out.println(next.decorated());
  }

  public static void test2() {
    final Graph source = proj(join(null, null)).toGraph("S");
    final Graph target = proj(null).toGraph("T");

    System.out.println(Enumerator.enumSubstitution(source, target));
  }

  public static void test3() {
    final ConstraintSet set = ConstraintSet.empty();
    final Abstraction<Object> abs0 = Abstraction.create(Interpreter.global(), "0");
    final Abstraction<Object> abs1 = Abstraction.create(Interpreter.global(), "1");
    final Abstraction<Object> abs2 = Abstraction.create(Interpreter.global(), "2");
    final Abstraction<Object> abs3 = Abstraction.create(Interpreter.global(), "3");
    set.add(refEq(abs0, abs1));
    set.add(refEq(abs2, abs3));
    //    set.transitiveOf(refEq(abs0, abs2));
    set.add(refEq(abs0, abs2));
    System.out.println(set);
  }

  private static void selectSource(Graph graph) {
    final List<Interpretation> interpretations = graph.interpretations();
    final Abstraction<SubqueryPredicate> predicate =
        ((SubqueryFilter) graph.head().prev()[0]).predicate();
    final Abstraction<PlainPredicate> p =
        ((PlainFilter) graph.head().prev()[0].prev()[1].prev()[0]).predicate();
    final ListIterator<Interpretation> iter = interpretations.listIterator();
    while (iter.hasNext()) {
      final Interpretation inter = iter.next();
      if (!(inter.interpret(predicate) instanceof InSubqueryPredicate)) {
        iter.remove();
        continue;
      }
      final List<MonoSourceColumnSet> flatten =
          inter.interpret(p).columns().stream()
              .map(ColumnSet::flatten)
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
      if (flatten.size() != 1) {
        iter.remove();
        continue;
      }
      final MonoSourceColumnSet c = flatten.iterator().next();
      if (c.source() != ((Input) graph.head().prev()[0].prev()[0]).source()) {
        iter.remove();
        continue;
      }
    }
    System.out.println();
  }

  private static void selectTarget(Graph graph) {
    final List<Interpretation> interpretations = graph.interpretations();
    final Abstraction<Projections> absProjs = ((Proj) graph.head().prev()[0]).projs();
    final Abstraction<PlainPredicate> absP0 =
        ((PlainFilter) graph.head().prev()[0].prev()[0]).predicate();
    final Abstraction<PlainPredicate> absP1 =
        ((PlainFilter) graph.head().prev()[0].prev()[0].prev()[0]).predicate();
    final Abstraction<InputSource> i0 =
        ((Input) graph.head().prev()[0].prev()[0].prev()[0].prev()[0].prev()[0]).source();
    final Abstraction<InputSource> i1 =
        ((Input) graph.head().prev()[0].prev()[0].prev()[0].prev()[0].prev()[1]).source();

    final ListIterator<Interpretation> iter = interpretations.listIterator();
    while (iter.hasNext()) {
      final Interpretation inter = iter.next();
      final Abstraction<InputSource> projSrc =
          inter.interpret(absProjs).columns().flatten().iterator().next().source();
      if (projSrc != i0) {
        iter.remove();
        continue;
      }

      final List<MonoSourceColumnSet> flatten0 =
          inter.interpret(absP0).columns().stream()
              .map(ColumnSet::flatten)
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
      final List<MonoSourceColumnSet> flatten1 =
          inter.interpret(absP1).columns().stream()
              .map(ColumnSet::flatten)
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
      if (flatten0.size() != 1) {
        iter.remove();
        continue;
      }
      if (flatten0.iterator().next().source() != i1) {
        iter.remove();
        continue;
      }
      if (flatten1.size() != 2) {
        iter.remove();
        continue;
      }
    }
    System.out.println();
  }
}
