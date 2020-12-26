package sjtu.ipads.wtune.symsolver;

import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Solver;
import sjtu.ipads.wtune.symsolver.core.impl.BaseQuery;
import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;
import sjtu.ipads.wtune.symsolver.smt.Value;

import java.util.Set;

import static com.google.common.collect.Sets.combinations;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

public class Main {
  public static void main(String[] args) {
    final Query0 q0 = new Query0();
    final Query1 q1 = new Query1();
    final Solver solver = Solver.make(q0, q1);
    System.out.println(solver.solve());
  }

  private static class Query0 extends BaseQuery {
    private Query0() {
      super(2, 3);
      for (PickSym pick : picks) pick.setVisibleSources(asList(tables));
      picks[0].setViableSources(combinations(Set.of(tables), tables.length));
      picks[1].setViableSources(singleton(singleton(tables[0])));
      picks[2].setViableSources(singleton(singleton(tables[1])));
    }

    @Override
    public Value output(SmtCtx ctx, Value[] tuples) {
      return ctx.makeFunc(picks[0]).apply(ctx.combine(tuples));
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
      return ctx.makeFunc(picks[0]).apply(ctx.combine(tuples));
    }

    @Override
    public Proposition condition(SmtCtx ctx, Value[] tuples) {
      return Proposition.tautology();
    }
  }
}
