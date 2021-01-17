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
import java.util.function.IntFunction;

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
//    final Value[] output0 = q0.output(), output1 = q1.output();
//    if (output0.length != output1.length || output0.length != 1)
//      return new Proposition[] {ctx.makeTautology()};
//
//    final Value out = ctx.makeTuple("out");
//    Proposition o0 = q0.condition().and(out.equalsTo(q0.output()[0]));
//    Proposition o1 = q1.condition().and(out.equalsTo(q1.output()[0]));
//
//    final Func func0 = ctx.makeQuery("q0"), func1 = ctx.makeQuery("q1");
//    o0 = func0.apply(out).equalsTo(ctx.makeExists(q0.tuples(), o0));
//    o1 = func1.apply(out).equalsTo(ctx.makeExists(q1.tuples(), o1));
//
//    final Proposition nonEq = func0.apply(out).equalsTo(func1.apply(out)).not();
//
//    final Proposition[] properties = new Proposition[3];
//    properties[0] = o0;
//    properties[1] = o1;
//    properties[2] = nonEq;
//    return properties;
    final Value[] output0 = q0.output(), output1 = q1.output();
    if (output0.length != output1.length || output0.length == 0)
      return new Proposition[] {ctx.makeTautology()};

    Proposition outputEq = null;
    for (int i = 0, bound = output0.length; i < bound; i++)
      outputEq = output0[i].equalsTo(output1[i]).and(outputEq);

    final TableSym[] tables0 = q0.tables(), tables1 = q1.tables();
    final Value[] tuples0 = q0.tuples(), tuples1 = q1.tuples();

    final Proposition cond0 = ctx.tuplesFrom(tuples0, tables0).and(q0.condition());
    final Proposition cond1 = ctx.tuplesFrom(tuples1, tables1).and(q1.condition());

    final Proposition[] properties = new Proposition[2];
    properties[0] = cond0;
    properties[1] = ctx.makeForAll(tuples1, cond1.implies(outputEq.not()));
    return properties;
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
    final TableSym[] vs = p.visibleSources();

    final Value[] args0 = ctx.makeTuples(vs.length, "x");
    final Value[] args1 = pickTuples(args0, vs, mask);

    addAssertion(constraint, ctx.makeForAll(args0, p.apply(args0).equalsTo(p.apply(args1))));
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

  private static Value[] maskTuples(
      TableSym[] tables,
      TableSym[] mask,
      IntFunction<Value> makeBoundedTuple,
      IntFunction<Value> makeFreeTuple) {
    final Value[] tuples = new Value[tables.length];
    for (int i = 0, j = 0, k = 0, bound = tuples.length; i < bound; i++)
      if (j < mask.length && tables[i] == mask[j]) tuples[i] = makeBoundedTuple.apply(j++);
      else tuples[i] = makeFreeTuple.apply(k++);
    return tuples;
  }
}
