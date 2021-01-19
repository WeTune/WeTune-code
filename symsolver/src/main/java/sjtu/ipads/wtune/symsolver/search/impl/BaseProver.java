package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.PredicateSym;
import sjtu.ipads.wtune.symsolver.core.Query;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.logic.*;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Prover;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static sjtu.ipads.wtune.common.utils.FuncUtils.dumb;

public abstract class BaseProver implements Prover {
  protected final LogicCtx ctx;
  protected final SmtSolver smtSolver;
  protected final Proposition[] targetProperties;
  protected final Map<Decision, Collection<Proposition>> assertions;

  protected Decision[] decisions;

  protected BaseProver(LogicCtx ctx, Query q0, Query q1) {
    this.ctx = ctx;
    this.smtSolver = ctx.makeSolver();
    this.targetProperties = makeNonEqProperties(ctx, q0, q1);
    this.assertions = new HashMap<>();
  }

  private static Proposition[] makeNonEqProperties(LogicCtx ctx, Query q0, Query q1) {
    final Value out = ctx.makeTuple("out");
    final Func func0 = ctx.makeQuery("q0"), func1 = ctx.makeQuery("q1");

    return new Proposition[] {
      func0.apply(out).equalsTo(q0.contains(out)),
      func1.apply(out).equalsTo(q1.contains(out)),
      func0.apply(out).equalsTo(func1.apply(out)).not()
    };
  }

  @Override
  public void tableEq(DecidableConstraint constraint, TableSym tx, TableSym ty) {
    addAssertion(constraint, tx.func().equalsTo(ty.func()));
  }

  @Override
  public void pickEq(DecidableConstraint constraint, PickSym px, PickSym py) {
    addAssertion(constraint, px.func().equalsTo(py.func()));
  }

  @Override
  public void predicateEq(DecidableConstraint constraint, PredicateSym px, PredicateSym py) {
    addAssertion(constraint, px.func().equalsTo(py.func()));
  }

  @Override
  public void pickFrom(DecidableConstraint constraint, PickSym p, TableSym... mask) {
    if (mask.length == p.visibleSources().length) return;

    final TableSym[] vs = p.visibleSources();

    final Value[] args0 = ctx.makeTuples(vs.length, "x");
    final Value[] args1 = pickTuples(args0, vs, mask);

    addAssertion(constraint, ctx.makeForAll(args0, p.apply(args0).equalsTo(p.apply(args1))));
  }

  @Override
  public void reference(
      DecidableConstraint constraint, TableSym tx, PickSym px, TableSym ty, PickSym py) {
    pickFrom(constraint, px, tx);
    pickFrom(constraint, py, ty);

    final Value x = ctx.makeTuple("x"), y = ctx.makeTuple("y");
    final Proposition ref = px.apply(x).equalsTo(py.apply(y));

    final Proposition assertion =
        ctx.makeForAll(
            x, ctx.tupleFrom(x, tx).implies(ctx.makeExists(y, ctx.tupleFrom(y, ty).and(ref))));
    addAssertion(constraint, assertion);
  }

  @Override
  public void prepare(Decision[] choices) {
    smtSolver.reset();
    assertions.clear();
    for (Decision choice : choices) choice.decide(this);
  }

  @Override
  public void decide(Decision... decisions) {
    this.decisions = decisions;
  }

  protected void addAssertion(DecidableConstraint constraint, Proposition assertion) {
    assertions.computeIfAbsent(constraint, dumb(ArrayList::new)).add(assertion);
  }

  private static Value[] pickTuples(Value[] tuples, TableSym[] sources, TableSym[] mask) {
    final Value[] maskedTuples = new Value[mask.length];

    for (int i = 0, j = 0; i < sources.length && j < mask.length; i++)
      if (mask[j] == sources[i]) maskedTuples[j++] = tuples[i];

    return maskedTuples;
  }
}
