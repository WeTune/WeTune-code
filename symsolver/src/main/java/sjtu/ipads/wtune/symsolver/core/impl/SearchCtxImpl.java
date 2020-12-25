package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.*;

import java.util.*;

public class SearchCtxImpl implements SearchCtx {
  private Prover[] provers;
  private ResettableTracer tracer;
  private Searcher searcher;

  // states
  private Decision[] decisions;
  private Map<Summary, Boolean> knownResults;
  private List<Summary> survivors;

  private SearchCtxImpl(TableSym[] tables, PickSym[] picks) {
    // provers =
    tracer = Tracer.resettable(tables, picks);
    searcher = Searcher.bindTo(this);
    knownResults = new HashMap<>();
    survivors = new LinkedList<>();
  }

  @Override
  public void tableEq(Constraint constraint, TableSym tx, TableSym ty) {
    tracer.tableEq(constraint, tx, ty);
    for (Prover prover : provers) prover.tableEq(constraint, tx, ty);
  }

  @Override
  public void pickEq(Constraint constraint, PickSym px, PickSym py) {
    tracer.pickEq(constraint, px, py);
    for (Prover prover : provers) prover.pickEq(constraint, px, py);
  }

  @Override
  public void pickFrom(Constraint constraint, PickSym p, Collection<TableSym> ts) {
    tracer.pickFrom(constraint, p, ts);
    for (Prover prover : provers) prover.pickFrom(constraint, p, ts);
  }

  @Override
  public void reference(Constraint constraint, TableSym tx, PickSym px, TableSym ty, PickSym py) {
    tracer.reference(constraint, tx, px, ty, py);
    for (Prover prover : provers) prover.reference(constraint, tx, px, ty, py);
  }

  @Override
  public void prepare(Decision[] choices) {
    for (Prover prover : provers) prover.prepare(choices);
  }

  @Override
  public void decide(Decision[] decisions) {
    this.decisions = decisions;
    tracer.reset();
    tracer.decide(decisions);
  }

  @Override
  public List<Summary> search(Iterable<DecisionTree> trees) {
    for (DecisionTree tree : trees) searcher.search(tree);
    return survivors;
  }

  @Override
  public boolean prove() {
    final Summary summary = tracer.summary();
    Boolean res = knownResults.get(summary);
    if (res != null) return res;

    res = true;
    for (Prover prover : provers) {
      prover.decide(decisions);
      if (!prover.prove()) {
        res = false;
        break;
      }
    }

    knownResults.put(summary, res);
    return res;
  }

  @Override
  public void record() {
    final Summary summary = tracer.summary();
    final ListIterator<Summary> iter = survivors.listIterator();
    while (iter.hasNext()) {
      final Summary survivor = iter.next();
      if (summary.implies(survivor)) return;
      if (survivor.implies(summary)) iter.remove();
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
}
