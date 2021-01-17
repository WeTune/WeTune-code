package sjtu.ipads.wtune.symsolver;

import sjtu.ipads.wtune.symsolver.core.*;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static com.google.common.collect.Sets.powerSet;
import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.common.utils.Commons.asArray;
import static sjtu.ipads.wtune.symsolver.DecidableConstraint.*;

public class Main {
  public static void main(String[] args) {
    test();
  }

  private static void test() {
    final QueryBuilder q0 = new Query2();
    final QueryBuilder q1 = new Query3();

    final Solver solver = Solver.make(q0, q1);
    final TableSym[] tables = solver.tables();
    final PickSym[] picks = solver.picks();
    final PredicateSym[] preds = solver.predicates();

    final DecisionTree tree =
        DecisionTree.from(
            //            tableEq(tables[0], tables[1]),
            tableEq(tables[0], tables[2]),
            //            tableEq(tables[1], tables[2]),
            //            pickEq(picks[0], picks[1]),
            //            pickEq(picks[0], picks[2]),
            pickEq(picks[0], picks[3]),
            //            pickEq(picks[0], picks[4]),
            //            pickEq(picks[1], picks[2]),
            //            pickEq(picks[3], picks[5]),
            //            pickEq(picks[2], picks[3]),
            pickFrom(picks[0], tables[0]),
            //            DecidableConstraint.pickFrom(picks[3], tables[0]),
            //            pickFrom(picks[0], tables[1]),
            //            pickFrom(picks[3], tables[0]),
            //            pickFrom(picks[0], tables[1]),
            //            pickFrom(picks[0], tables[0], tables[1]),
            //            pickFrom(picks[1], tables[0]),
            //            pickFrom(picks[2], tables[0]),
            //            predicateEq(preds[0], preds[1]),
            reference(tables[0], picks[1], tables[1], picks[2]));

    //    System.out.println(solver.check(tree.choices()));
    //    final Collection<Summary> summaries = solver.solve(tree);
    final Collection<Summary> summaries = solver.solve();

    for (Summary summary : summaries) {
      System.out.println(summary);
      System.out.println(Arrays.toString(summary.constraints()));
    }

    solver.close();
  }

  private static class Query2 extends BaseQueryBuilder {
    @Override
    public int numTables() {
      return 2;
    }

    @Override
    public int numPicks() {
      return 3;
    }

    @Override
    public int numPreds() {
      return 0;
    }

    @Override
    protected void prepare() {
      for (PickSym pick : picks) pick.setVisibleSources(tables);
      picks[0].setViableSources(powerSet(Set.of(tables)));
      picks[1].setViableSources(singleton(singleton(tables[0])));
      picks[2].setViableSources(singleton(singleton(tables[1])));
      picks[1].setJoined(picks[2]);
    }

    @Override
    public Value[] output() {
      return asArray(picks[0].apply(tuples));
    }

    @Override
    public Proposition condition() {
      return ctx()
          .tuplesFrom(tuples, tables)
          .and(picks[1].apply(tuples).equalsTo(picks[2].apply(tuples)));
    }
  }

  private static class Query3 extends BaseQueryBuilder {
    @Override
    public int numTables() {
      return 1;
    }

    @Override
    public int numPicks() {
      return 1;
    }

    @Override
    public int numPreds() {
      return 0;
    }

    @Override
    protected void prepare() {
      for (PickSym pick : picks) pick.setVisibleSources(tables);
      picks[0].setViableSources(powerSet(Set.of(tables)));
    }

    @Override
    public Value[] output() {
      return asArray(picks[0].apply(tuples));
    }

    @Override
    public Proposition condition() {
      return ctx().tuplesFrom(tuples, tables);
    }
  }

  private static class Query0 extends BaseQueryBuilder {
    @Override
    public int numTables() {
      return 2;
    }

    @Override
    public int numPicks() {
      return 4;
    }

    @Override
    public int numPreds() {
      return 1;
    }

    @Override
    protected void prepare() {
      for (PickSym pick : picks) pick.setVisibleSources(tables);
      picks[0].setViableSources(powerSet(Set.of(tables)));
      picks[1].setViableSources(singleton(singleton(tables[0])));
      picks[2].setViableSources(singleton(singleton(tables[1])));
      picks[3].setViableSources(powerSet(Set.of(tables)));
      picks[1].setJoined(picks[2]);
    }

    @Override
    public Value[] output() {
      return asArray(picks[0].apply(tuples));
    }

    @Override
    public Proposition condition() {
      return picks[1]
          .apply(tuples)
          .equalsTo(picks[2].apply(tuples))
          .and((Proposition) preds[0].apply(picks[3].apply(tuples)));
    }
  }

  private static class Query1 extends BaseQueryBuilder {
    @Override
    public int numTables() {
      return 1;
    }

    @Override
    public int numPicks() {
      return 2;
    }

    @Override
    public int numPreds() {
      return 1;
    }

    @Override
    protected void prepare() {
      for (PickSym pick : picks) pick.setVisibleSources(tables);
      picks[0].setViableSources(powerSet(Set.of(tables)));
      picks[1].setViableSources(powerSet(Set.of(tables)));
    }

    @Override
    public Value[] output() {
      return asArray(picks[0].apply(tuples));
    }

    @Override
    public Proposition condition() {
      return (Proposition) preds[0].apply(picks[1].apply(tuples));
    }
  }
}
