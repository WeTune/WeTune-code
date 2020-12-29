package sjtu.ipads.wtune.symsolver;

import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Solver;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.core.impl.BaseQuery;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;
import sjtu.ipads.wtune.symsolver.search.Summary;
import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;
import sjtu.ipads.wtune.symsolver.smt.Value;

import java.util.Set;

import static com.google.common.collect.Sets.powerSet;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static sjtu.ipads.wtune.symsolver.core.Constraint.*;

public class Main {
  public static void main(String[] args) {
    test();
  }

  private static void test() {
    final Query0 q0 = new Query0();
    final Query1 q1 = new Query1();

    final Solver solver = Solver.make(q0, q1);
    final TableSym[] tables = solver.tables();
    final PickSym[] picks = solver.picks();

    final DecisionTree tree =
        DecisionTree.from(
            tableEq(tables[0], tables[1]),
            tableEq(tables[0], tables[2]),
            pickEq(picks[0], picks[1]),
            pickEq(picks[2], picks[3]),
            pickFrom(picks[0], emptyList()),
            reference(tables[0], picks[1], tables[1], picks[2]));

    //    System.out.println(solver.check(tree.choices()));
    //        System.out.println(solver.solve(tree));
    for (Summary summary : solver.solve()) {
      System.out.println(summary);
      System.out.println(summary.constraints());
    }
  }

  private static class Query0 extends BaseQuery {
    private Query0() {
      super(2, 3);
      for (PickSym pick : picks) pick.setVisibleSources(asList(tables));
      picks[0].setViableSources(powerSet(Set.of(tables)));
      picks[1].setViableSources(singleton(singleton(tables[0])));
      picks[2].setViableSources(singleton(singleton(tables[1])));
      picks[1].setJoined(picks[2]);
    }

    @Override
    public Value output(SmtCtx ctx, Value[] tuples) {
      return ctx.makeFunc(picks[0]).apply(ctx.makeCombine(tuples));
    }

    @Override
    public Proposition condition(SmtCtx ctx, Value[] tuples) {
      return ctx.pick(picks[1], tuples).equalsTo(ctx.pick(picks[2], tuples));
    }
  }

  private static class Query1 extends BaseQuery {
    private Query1() {
      super(1, 1);
      picks[0].setVisibleSources(asList(tables));
      picks[0].setViableSources(singleton(singleton(tables[0])));
    }

    @Override
    public Value output(SmtCtx ctx, Value[] tuples) {
      return ctx.makeFunc(picks[0]).apply(ctx.makeCombine(tuples));
    }

    @Override
    public Proposition condition(SmtCtx ctx, Value[] tuples) {
      return Proposition.tautology();
    }
  }
}
