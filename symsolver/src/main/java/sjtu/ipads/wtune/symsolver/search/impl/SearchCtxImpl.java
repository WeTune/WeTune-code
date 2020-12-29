package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Result;
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
  private final Map<Summary, Result> knownResults;
  private final List<Summary> survivors;

  private final Statistics stat;

  private SearchCtxImpl(
      TableSym[] tables, PickSym[] picks, SmtCtx smtCtx, Proposition... problems) {
    prover = Prover.combine(arrayMap(p -> Prover.incremental(smtCtx, p), Prover.class, problems));
    tracer = Tracer.bindTo(tables, picks);
    searcher = Searcher.bindTo(this);

    knownResults = new HashMap<>();
    survivors = new LinkedList<>();
    stat = new Statistics();
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
  public List<Summary> search(DecisionTree tree) {
    final long t0 = System.currentTimeMillis();
    searcher.search(tree);
    final long t1 = System.currentTimeMillis();

    stat.numSearched = searcher.numSearched();
    stat.numSkipped = searcher.numSkipped();
    stat.timeTotal += t1 - t0;

    System.out.println(stat);
    return survivors;
  }

  @Override
  public Result prove() {
    final Summary summary = tracer.summary();
    Result res = knownResults.get(summary);
    if (res != null) {
      ++stat.numCacheHit;
      return res;
    }

    final long t0 = System.currentTimeMillis();
    res = prover.prove();
    final long t1 = System.currentTimeMillis();

    updateProveStatistic(res, t1 - t0);

    if (res != Result.UNKNOWN) knownResults.put(summary, res);
    return res;
  }

  @Override
  public void record() {
    ++stat.numRecordCall;

    final Summary summary = tracer.summary();
    final ListIterator<Summary> iter = survivors.listIterator();
    while (iter.hasNext()) {
      final Summary survivor = iter.next();
      if (summary.equals(survivor) || summary.implies(survivor)) {
        ++stat.numDuplicate;
        return;
      }

      if (survivor.implies(summary)) {
        ++stat.numRelax;
        iter.remove();
      }
    }
    survivors.add(summary);
  }

  @Override
  public boolean isConflict() {
    final boolean ret;
    if (ret = (tracer.isConflict())) ++stat.numConflict;
    return ret;
  }

  @Override
  public boolean isIncomplete() {
    final boolean ret;
    if (ret = (tracer.isIncomplete())) ++stat.numIncomplete;
    return ret;
  }

  @Override
  public Summary summary() {
    return tracer.summary();
  }

  private void updateProveStatistic(Result result, long spent) {
    ++stat.numProveCall;
    if (result == Result.NON_EQUIVALENT) {
      ++stat.numNonEq;
      stat.timeForNonEq += spent;
    } else if (result == Result.EQUIVALENT) {
      ++stat.numEq;
      stat.timeForEq += spent;
    } else {
      ++stat.numUnknown;
      stat.timeForUnknown += spent;
    }
  }

  private static class Statistics {
    private int numSearched = 0;
    private int numSkipped = 0;
    private int numConflict = 0;
    private int numIncomplete = 0;
    private int numProveCall = 0;
    private int numCacheHit = 0;
    private int numUnknown = 0;
    private int numNonEq = 0;
    private int numEq = 0;
    private int numRecordCall = 0;
    private int numDuplicate = 0;
    private int numRelax = 0;
    private long timeForNonEq = 0;
    private long timeForEq = 0;
    private long timeForUnknown = 0;
    private long timeTotal = 0;

    @Override
    public String toString() {
      return "#Searched="
          + numSearched
          + "\n#Skipped="
          + numSkipped
          + "\n#Conflict="
          + numConflict
          + "\n#Incomplete="
          + numIncomplete
          + "\n#ProveCall="
          + numProveCall
          + "\n#CacheHit="
          + numCacheHit
          + "\n#NonEq="
          + numNonEq
          + "\n#Eq="
          + numEq
          + "\n#Unknown="
          + numUnknown
          + "\n#RecordCall="
          + numRecordCall
          + "\n#Duplicate="
          + numDuplicate
          + "\n#Relax="
          + numRelax
          + "\nTimeForNonEq="
          + timeForNonEq
          + "\nTimeForEq="
          + timeForEq
          + "\nTimeForUnknown="
          + timeForUnknown
          + "\nTimeTotal="
          + timeTotal;
    }
  }
}
