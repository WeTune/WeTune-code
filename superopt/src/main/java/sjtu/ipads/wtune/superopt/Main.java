package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Input;
import sjtu.ipads.wtune.superopt.operators.PlainFilter;
import sjtu.ipads.wtune.superopt.operators.Proj;
import sjtu.ipads.wtune.superopt.operators.SubqueryFilter;
import sjtu.ipads.wtune.superopt.relational.PlainPredicate;
import sjtu.ipads.wtune.superopt.relational.Projections;
import sjtu.ipads.wtune.superopt.relational.SubqueryPredicate;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;
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
    test0();
    //      test1();
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

      final SymbolicColumns sSubqueryCol = sPredicate.columns().flatten().iterator().next();
      final SymbolicColumns tSubqueryCol = tPredicate.columns().flatten().iterator().next();

      final Constraint c0 = refEq(sInput0.source(), tInput0.source());
      final Constraint c1 = refEq(sInput1.source(), tInput1.source());
      final Constraint c2 = refEq(sSubqueryCol.abstractions(), tSubqueryCol.abstractions());
      final Constraint c3 = refEq(sSubqueryCol.relation(), tSubqueryCol.relation());

      if (!(constraints.contains(c0))) continue;
      if (!(constraints.contains(c1))) continue;
      if (!(constraints.contains(c2))) continue;
      if (!(constraints.contains(c3))) continue;

      final Projections tProjections = tInterpretation.interpret(tProj.projs());
      final Projections sProjections = sInterpretation.interpret(sProj.projs());

      final SymbolicColumns tProjCol = tProjections.columns().flatten().iterator().next();
      final SymbolicColumns sProjCol = sProjections.columns().flatten().iterator().next();

      final Constraint c4 = refEq(tProjCol.abstractions(), sProjCol.abstractions());
      final Constraint c5 = refEq(tProjCol.relation(), sProjCol.relation());

      if (!(constraints.contains(c4))) continue;
      if (!(constraints.contains(c5))) continue;

      final PlainPredicate pred = sInterpretation.interpret(sFilter.predicate());
      final List<SymbolicColumns> columns =
          pred.columns().stream()
              .map(SymbolicColumns::flatten)
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
      if (columns.size() != 2) continue;
      final SymbolicColumns sFilterCol0 = columns.get(0);
      final SymbolicColumns sFilterCol1 = columns.get(0);

      final Constraint c6 = refEq(sFilterCol0.relation(), sProjCol.relation());
      final Constraint c7 = refEq(sFilterCol0.abstractions(), sProjCol.abstractions());
      if (!constraints.contains(c6)) continue;
      if (!constraints.contains(c7)) continue;

      final Constraint c8 = refEq(sFilterCol1.relation(), sSubqueryCol.relation());
      final Constraint c9 = refEq(sFilterCol1.abstractions(), sSubqueryCol.abstractions());
      if (!constraints.contains(c8)) continue;
      if (!constraints.contains(c9)) continue;
      count++;
      filtered.add(substitution);
    }
    System.out.println(count);
    System.out.println(filtered.get(0));

    //    final Set<Graph> skeletons = Enumerator.enumSkeleton();
    //    System.out.println("#skeletons: " + skeletons.size());
    //    final int sum = skeletons.stream().map(Graph::interpretations).mapToInt(List::size).sum();
    //    System.out.println("#count: " + sum);
  }
}
