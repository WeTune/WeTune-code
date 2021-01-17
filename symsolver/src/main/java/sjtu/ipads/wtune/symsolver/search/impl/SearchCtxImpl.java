package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.*;
import sjtu.ipads.wtune.symsolver.logic.LogicCtx;
import sjtu.ipads.wtune.symsolver.search.*;

import java.util.*;

public class SearchCtxImpl implements SearchCtx {
  private final Prover prover;
  private final Tracer tracer;
  private final Searcher searcher;

  // states
  private final Map<Summary, Result> knownResults;
  private final List<Summary> survivors;

  private final Statistics stat;

  private SearchCtxImpl(
      TableSym[] tables,
      PickSym[] picks,
      PredicateSym[] preds,
      LogicCtx ctx,
      Query q0,
      Query q1,
      long timeout) {
    prover = Prover.incremental(ctx, q0, q1);
    tracer = Tracer.bindTo(tables, picks, preds);
    searcher = Searcher.bindTo(this, timeout);

    knownResults = new HashMap<>();
    survivors = new LinkedList<>();
    stat = new Statistics();
  }

  public static SearchCtx build(
      TableSym[] tables, PickSym[] picks, PredicateSym[] preds, LogicCtx ctx, Query q0, Query q1) {
    return new SearchCtxImpl(tables, picks, preds, ctx, q0, q1, -1);
  }

  public static SearchCtx build(
      TableSym[] tables,
      PickSym[] picks,
      PredicateSym[] preds,
      LogicCtx ctx,
      Query q0,
      Query q1,
      long timeout) {
    return new SearchCtxImpl(tables, picks, preds, ctx, q0, q1, timeout);
  }

  @Override
  public void tableEq(DecidableConstraint constraint, TableSym tx, TableSym ty) {
    tracer.tableEq(constraint, tx, ty);
    prover.tableEq(constraint, tx, ty);
  }

  @Override
  public void pickEq(DecidableConstraint constraint, PickSym px, PickSym py) {
    tracer.pickEq(constraint, px, py);
    prover.pickEq(constraint, px, py);
  }

  @Override
  public void predicateEq(DecidableConstraint constraint, PredicateSym px, PredicateSym py) {
    tracer.predicateEq(constraint, px, py);
    prover.predicateEq(constraint, px, py);
  }

  @Override
  public void pickFrom(DecidableConstraint constraint, PickSym p, TableSym... src) {
    tracer.pickFrom(constraint, p, src);
    prover.pickFrom(constraint, p, src);
  }

  @Override
  public void reference(
      DecidableConstraint constraint, TableSym tx, PickSym px, TableSym ty, PickSym py) {
    tracer.reference(constraint, tx, px, ty, py);
    prover.reference(constraint, tx, px, ty, py);
  }

  @Override
  public void prepare(Decision[] choices) {
    prover.prepare(choices);
  }

  @Override
  public void decide(Decision... decisions) {
    tracer.decide(decisions);
    prover.decide(decisions);
  }

  @Override
  public List<Summary> search(DecisionTree tree) {
    final long t0 = System.currentTimeMillis();
    searcher.search(tree);
    final long t1 = System.currentTimeMillis();

    stat.numFastConflict = tracer.numFastConflict();
    stat.numFastIncomplete = tracer.numFastIncomplete();
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

    //    prover.decide(summary.constraints());
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
      if (summary.equals(survivor)) {
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

  @Override
  public int numFastConflict() {
    return tracer.numFastConflict();
  }

  @Override
  public int numFastIncomplete() {
    return tracer.numFastIncomplete();
  }

  private static class Statistics {
    private int numSearched = 0;
    private int numSkipped = 0;
    private int numConflict = 0;
    private int numFastConflict = 0;
    private int numFastIncomplete = 0;
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
          + "\n#FastConflict="
          + numFastConflict
          + "\n#Incomplete="
          + numIncomplete
          + "\n#FastIncomplete="
          + numFastIncomplete
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
