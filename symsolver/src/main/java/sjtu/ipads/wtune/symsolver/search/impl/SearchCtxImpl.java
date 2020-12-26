package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.*;
import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayMap;

public class SearchCtxImpl implements SearchCtx {
  private final Prover prover;
  private final Tracer tracer;
  private final Searcher searcher;

  // states
  private final Map<Summary, Boolean> knownResults;
  private final List<Summary> survivors;

  private final Map<String, Object> statistics;
  private final SearchCtxStat stat;

  private SearchCtxImpl(
      TableSym[] tables, PickSym[] picks, SmtCtx smtCtx, Proposition... problems) {
    prover = Prover.combine(arrayMap(problems, problem -> Prover.incremental(smtCtx, problem)));
    tracer = Tracer.bindTo(tables, picks);
    searcher = Searcher.bindTo(this);

    knownResults = new HashMap<>();
    survivors = new LinkedList<>();
    statistics = new HashMap<>();
    stat = new SearchCtxStat();
  }

  public static SearchCtx build(
      TableSym[] tables, PickSym[] picks, SmtCtx smtCtx, Proposition... problems) {
    return new SearchCtxImpl(tables, picks, smtCtx, problems);
  }

  @Override
  public void tableEq(Constraint constraint, TableSym tx, TableSym ty) {
    tracer.tableEq(constraint, tx, ty);
    prover.tableEq(constraint, tx, ty);
  }

  @Override
  public void pickEq(Constraint constraint, PickSym px, PickSym py) {
    tracer.pickEq(constraint, px, py);
    prover.pickEq(constraint, px, py);
  }

  @Override
  public void pickFrom(Constraint constraint, PickSym p, Collection<TableSym> ts) {
    tracer.pickFrom(constraint, p, ts);
    prover.pickFrom(constraint, p, ts);
  }

  @Override
  public void reference(Constraint constraint, TableSym tx, PickSym px, TableSym ty, PickSym py) {
    tracer.reference(constraint, tx, px, ty, py);
    prover.reference(constraint, tx, px, ty, py);
  }

  @Override
  public void prepare(Decision[] choices) {
    prover.prepare(choices);
  }

  @Override
  public void decide(Decision[] decisions) {
    tracer.decide(decisions);
    prover.decide(decisions);
  }

  @Override
  public List<Summary> search(Iterable<DecisionTree> trees) {
    for (DecisionTree tree : trees) searcher.search(tree);

    statistic().compute("context", (k, v) -> stat.merge((SearchCtxStat) v));
    return survivors;
  }

  @Override
  public boolean prove() {
    final Summary summary = tracer.summary();
    Boolean res = knownResults.get(summary);
    if (res != null) {
      ++stat.numCacheHit;
      return res;
    }

    ++stat.numProveCall;
    res = prover.prove();

    knownResults.put(summary, res);
    return res;
  }

  @Override
  public void record() {
    ++stat.numRecordCall;

    final Summary summary = tracer.summary();
    final ListIterator<Summary> iter = survivors.listIterator();
    while (iter.hasNext()) {
      final Summary survivor = iter.next();
      if (summary.implies(survivor)) return;

      if (survivor.implies(summary)) {
        ++stat.numRelax;
        iter.remove();
      }
    }
    survivors.add(summary);
  }

  @Override
  public boolean isConflict() {
    return tracer.isConflict();
  }

  @Override
  public boolean isIncomplete() {
    return tracer.isIncomplete();
  }

  @Override
  public Summary summary() {
    return tracer.summary();
  }

  @Override
  public Map<String, Object> statistic() {
    return statistics;
  }

  private static class SearchCtxStat {
    private int numProveCall = 0;
    private int numCacheHit = 0;
    private int numRecordCall = 0;
    private int numRelax = 0;

    private SearchCtxStat merge(SearchCtxStat other) {
      if (other != null) {
        numProveCall += other.numProveCall;
        numCacheHit += other.numCacheHit;
        numRecordCall += other.numRecordCall;
        numRelax += other.numRelax;
      }

      return this;
    }

    @Override
    public String toString() {
      return "#ProveCall="
          + numProveCall
          + " #CacheHit="
          + numCacheHit
          + " #RecordCall="
          + numRecordCall
          + " #Relax="
          + numRelax;
    }
  }
}
