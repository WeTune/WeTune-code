package sjtu.ipads.wtune.symsolver.core.impl;

import com.google.common.collect.Lists;
import sjtu.ipads.wtune.symsolver.core.*;
import sjtu.ipads.wtune.symsolver.search.*;
import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;
import sjtu.ipads.wtune.symsolver.smt.Value;
import sjtu.ipads.wtune.symsolver.utils.Indexed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayConcat;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.symsolver.core.Constraint.*;

public class SolverImpl implements Solver {
  private final TableSym[] tables;
  private final PickSym[] picks;
  private final SearchCtx searchCtx;

  private SolverImpl(Query q0, Query q1) {
    this.tables = arrayConcat(q0.tables(), q1.tables());
    this.picks = arrayConcat(q0.picks(), q1.picks());

    final SmtCtx smtCtx = SmtCtx.z3();
    final Proposition problem0 = makeProblem(smtCtx, q0, q1);
    final Proposition problem1 = makeProblem(smtCtx, q1, q0);
    this.searchCtx = SearchCtx.make(tables, picks, smtCtx, problem0, problem1);
  }

  public static Solver build(Query q0, Query q1) {
    q0.setName("x");
    q1.setName("y");

    number(q0.tables(), 0);
    number(q1.tables(), q0.tables().length);

    number(q0.picks(), 0);
    number(q1.picks(), q0.picks().length);

    return new SolverImpl(q0, q1);
  }

  @Override
  public TableSym[] tables() {
    return tables;
  }

  @Override
  public PickSym[] picks() {
    return picks;
  }

  @Override
  public Collection<Summary> solve() {
    return solve(makeDecisionTree(tables, picks));
  }

  @Override
  public Collection<Summary> solve(DecisionTree tree) {
    return searchCtx.search(tree);
  }

  @Override
  public Result check(Decision... decisions) {
    searchCtx.prepare(decisions);
    searchCtx.decide(decisions);
    if (searchCtx.isConflict() || searchCtx.isIncomplete()) return Result.NON_EQUIVALENT;
    else return searchCtx.prove();
  }

  private static void number(Indexed[] indexes, int start) {
    for (Indexed index : indexes) index.setIndex(start++);
  }

  private static Proposition makeProblem(SmtCtx ctx, Query q0, Query q1) {
    final TableSym[] tablesX = q0.tables(), tablesY = q1.tables();

    final Value[] tuplesX = ctx.makeTuples(tablesX.length, q0.name());
    final Value[] tuplesY = ctx.makeTuples(tablesY.length, q1.name());

    final Proposition tuplesXFrom = ctx.tuplesFrom(tuplesX, Arrays.asList(tablesX));
    final Proposition tuplesYFrom = ctx.tuplesFrom(tuplesY, Arrays.asList(tablesY));

    final Proposition condX = q0.condition(ctx, tuplesX), condY = q1.condition(ctx, tuplesY);

    final Proposition eqCond = q0.output(ctx, tuplesX).equalsTo(q1.output(ctx, tuplesY));

    return ctx.makeForAll(
        tuplesX,
        tuplesXFrom
            .and(condX)
            .implies(ctx.makeExists(tuplesY, tuplesYFrom.and(condY).and(eqCond))));
  }

  private static Iterable<DecisionTree> makeDecisionTrees(TableSym[] tables, PickSym[] picks) {
    final List<List<Constraint>> choices = new ArrayList<>(32);

    for (int i = 0, bound = tables.length; i < bound; i++)
      for (int j = i + 1; j < bound; j++) choices.add(singletonList(tableEq(tables[i], tables[j])));

    for (int i = 0, bound = picks.length; i < bound; i++)
      for (int j = i + 1; j < bound; j++) choices.add(singletonList(pickEq(picks[i], picks[j])));

    for (PickSym pick : picks) {
      choices.add(listMap(src -> pickFrom(pick, src), pick.viableSources()));

      final PickSym joined = pick.joined();
      if (joined == null) continue;

      if (!checkValidJoinKey(pick, joined))
        throw new IllegalArgumentException("invalid join key " + pick + " " + joined);

      final TableSym joinedSrc = getOnlyElement(getOnlyElement(joined.viableSources()));
      choices.add(
          listMap(
              src -> reference(getOnlyElement(src), pick, joinedSrc, joined),
              pick.viableSources()));
    }

    return listMap(DecisionTree::from, Lists.cartesianProduct(choices));
  }

  private static DecisionTree makeDecisionTree(TableSym[] tables, PickSym[] picks) {
    final List<Constraint> choices = new ArrayList<>(32);

    for (int i = 0, bound = tables.length; i < bound; i++)
      for (int j = i + 1; j < bound; j++) choices.add(tableEq(tables[i], tables[j]));

    for (int i = 0, bound = picks.length; i < bound; i++)
      for (int j = i + 1; j < bound; j++) choices.add(pickEq(picks[i], picks[j]));

    for (PickSym pick : picks) {
      choices.addAll(listMap(src -> pickFrom(pick, src), pick.viableSources()));

      final PickSym joined = pick.joined();
      if (joined == null) continue;

      if (!checkValidJoinKey(pick, joined))
        throw new IllegalArgumentException("invalid join key " + pick + " " + joined);

      final TableSym joinedSrc = getOnlyElement(getOnlyElement(joined.viableSources()));
      choices.addAll(
          listMap(
              src -> reference(getOnlyElement(src), pick, joinedSrc, joined),
              pick.viableSources()));
    }

    return DecisionTree.from(choices);
  }

  private static boolean checkValidJoinKey(PickSym x, PickSym y) {
    return x.viableSources().stream().allMatch(it -> it.size() == 1)
        && y.viableSources().size() == 1
        && getOnlyElement(y.viableSources()).size() == 1;
  }
}
