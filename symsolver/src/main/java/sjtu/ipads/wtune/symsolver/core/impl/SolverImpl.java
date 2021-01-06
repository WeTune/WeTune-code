package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.*;
import sjtu.ipads.wtune.symsolver.logic.SmtCtx;
import sjtu.ipads.wtune.symsolver.search.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.symsolver.core.Constraint.*;
import static sjtu.ipads.wtune.symsolver.utils.Indexed.number;

public class SolverImpl implements Solver {
  private final TableSym[] tables;
  private final PickSym[] picks;
  private final SearchCtx searchCtx;

  private SolverImpl(Query q0, Query q1) {
    this.tables = arrayConcat(q0.tables(), q1.tables());
    this.picks = arrayConcat(q0.picks(), q1.picks());

    final SmtCtx smtCtx = SmtCtx.z3();

    bindFuncs(tables, smtCtx);
    bindFuncs(picks, smtCtx);

    this.searchCtx = SearchCtx.make(tables, picks, smtCtx, q0, q1);
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

  private static void bindFuncs(Sym[] syms, SmtCtx ctx) {
    for (Sym sym : syms) sym.setFunc(ctx.makeFunc(sym));
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
  public Tracer tracer() {
    return searchCtx;
  }

  @Override
  public Prover prover() {
    return searchCtx;
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

      final TableSym joinedSrc = joined.viableSources()[0][0];
      choices.addAll(
          listMap(src -> reference(src[0], pick, joinedSrc, joined), pick.viableSources()));
    }

    return DecisionTree.from(choices);
  }

  private static boolean checkValidJoinKey(PickSym x, PickSym y) {
    return stream(x.viableSources()).allMatch(it -> it.length == 1)
        && y.viableSources().length == 1
        && y.viableSources()[0].length == 1;
  }
}
