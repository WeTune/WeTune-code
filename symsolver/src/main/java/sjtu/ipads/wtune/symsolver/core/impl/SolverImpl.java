package sjtu.ipads.wtune.symsolver.core.impl;

import com.google.common.collect.Lists;
import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.*;
import sjtu.ipads.wtune.symsolver.logic.LogicCtx;
import sjtu.ipads.wtune.symsolver.search.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static sjtu.ipads.wtune.common.utils.Commons.arrayConcat;
import static sjtu.ipads.wtune.common.utils.Commons.sorted;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;
import static sjtu.ipads.wtune.symsolver.DecidableConstraint.*;

public class SolverImpl implements Solver {
  private final TableSym[] tables;
  private final PickSym[] picks;
  private final PredicateSym[] preds;
  private final LogicCtx logicCtx;
  private final SearchCtx searchCtx;

  private SolverImpl(QueryBuilder b0, QueryBuilder b1, long timeout) {
    logicCtx = LogicCtx.z3();

    final Query q0 = b0.build(logicCtx, 0, 0, 0);
    final Query q1 = b1.build(logicCtx, q0.tables().length, q0.picks().length, q0.preds().length);

    tables = sorted(arrayConcat(q0.tables(), q1.tables()), Indexed::compareTo);
    picks = sorted(arrayConcat(q0.picks(), q1.picks()), Indexed::compareTo);
    preds = sorted(arrayConcat(q0.preds(), q1.preds()), Indexed::compareTo);

    searchCtx = SearchCtx.make(tables, picks, preds, logicCtx, q0, q1, timeout);
  }

  public static Solver build(QueryBuilder q0, QueryBuilder q1) {
    return new SolverImpl(q0, q1, -1);
  }

  public static Solver build(QueryBuilder q0, QueryBuilder q1, long timeout) {
    return new SolverImpl(q0, q1, timeout);
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
  public PredicateSym[] predicates() {
    return preds;
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
    final DecisionTree tree = makeDecisionTree(tables, picks, preds);
    return tree == null ? null : solve(tree);
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

  @Override
  public void close() {
    logicCtx.close();
  }

  private static DecisionTree makeDecisionTree(
      TableSym[] tables, PickSym[] picks, PredicateSym[] preds) {
    final List<DecidableConstraint> choices = new ArrayList<>(32);

    for (int i = 0, bound = tables.length; i < bound; i++)
      for (int j = i + 1; j < bound; j++) choices.add(tableEq(tables[i], tables[j]));

    for (int i = 0, bound = picks.length; i < bound; i++)
      for (int j = i + 1; j < bound; j++) choices.add(pickEq(picks[i], picks[j]));

    for (int i = 0, bound = preds.length; i < bound; i++)
      for (int j = i + 1; j < bound; j++) choices.add(predicateEq(preds[i], preds[j]));

    for (PickSym pick : picks) {
      choices.addAll(listMap(src -> pickFrom(pick, src), pick.viableSources()));

      final PickSym joined = pick.joined();
      if (joined == null) continue;

      if (!checkValidJoinKey(pick, joined))
        throw new IllegalArgumentException("invalid join key " + pick + " " + joined);

      Lists.cartesianProduct(asList(pick.viableSources()), asList(joined.viableSources())).stream()
          .map(xs -> reference(xs.get(0)[0], pick, xs.get(1)[0], joined))
          .forEach(choices::add);
    }

    return DecisionTree.fast(tables.length, picks.length, preds.length, choices);
  }

  private static boolean checkValidJoinKey(PickSym x, PickSym y) {
    return stream(x.viableSources()).allMatch(it -> it.length == 1)
        && stream(y.viableSources()).allMatch(it -> it.length == 1);
  }
}
