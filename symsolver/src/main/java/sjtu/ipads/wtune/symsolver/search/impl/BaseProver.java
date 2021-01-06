package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Query;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Prover;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.SmtCtx;
import sjtu.ipads.wtune.symsolver.logic.SmtSolver;
import sjtu.ipads.wtune.symsolver.logic.Value;

import java.util.*;
import java.util.function.IntFunction;

import static java.util.Arrays.asList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.dumb;

public abstract class BaseProver implements Prover {
  protected final SmtCtx ctx;
  protected final SmtSolver smtSolver;
  protected final Proposition[] targetProperties;
  protected final Map<Decision, Collection<Proposition>> assertions;

  protected Decision[] decisions;

  protected BaseProver(SmtCtx ctx, Query q0, Query q1) {
    this.ctx = ctx;
    this.smtSolver = ctx.makeSolver();
    this.targetProperties = makeNonEqProperties(ctx, q0, q1);
    this.assertions = new HashMap<>();
  }

  private static Proposition[] makeNonEqProperties(SmtCtx ctx, Query q0, Query q1) {
    final TableSym[] tables0 = q0.tables(), tables1 = q1.tables();
    final Value[] tuples0 = ctx.makeTuples(tables0.length, "a");
    final Value[] tuples1 = ctx.makeTuples(tables1.length, "b");

    final Proposition cond0 = ctx.tuplesFrom(tuples0, tables0).and(q0.condition(ctx, tuples0));
    final Proposition cond1 = ctx.tuplesFrom(tuples1, tables1).and(q1.condition(ctx, tuples1));

    final Value output0 = q0.output(ctx, tuples0), output1 = q1.output(ctx, tuples1);

    final Proposition[] properties = new Proposition[2];
    properties[0] = cond0;
    properties[1] = ctx.makeForAll(tuples1, cond1.implies(output0.equalsTo(output1).not()));
    return properties;
  }

  @Override
  public void tableEq(Constraint constraint, TableSym tx, TableSym ty) {
    addAssertion(constraint, tx.func().equalsTo(ty.func()));
  }

  @Override
  public void pickEq(Constraint constraint, PickSym px, PickSym py) {
    addAssertion(constraint, px.func().equalsTo(py.func()));
  }

  @Override
  public void pickFrom(Constraint constraint, PickSym p, TableSym... mask) {
    final TableSym[] vs = p.visibleSources();

    final Value[] tuples = ctx.makeTuples((vs.length << 1) - mask.length, "x");
    final Value[] boundedTuples = pickTuples(tuples, vs, mask);

    final Value[] args0 = Arrays.copyOf(tuples, vs.length);
    final Value[] args1 = maskTuples(vs, mask, i -> boundedTuples[i], i -> tuples[i + vs.length]);

    addAssertion(constraint, ctx.makeForAll(tuples, p.apply(args0).equalsTo(p.apply(args1))));
  }

  @Override
  public void reference(Constraint constraint, TableSym tx, PickSym px, TableSym ty, PickSym py) {
    pickFrom(constraint, px, tx);
    pickFrom(constraint, py, ty);

    final TableSym[] visibleX = px.visibleSources(), visibleY = py.visibleSources();
    final Value[] argsX = ctx.makeTuples(visibleX.length, "x");
    final Value[] argsY = ctx.makeTuples(visibleY.length, "y");

    final Proposition eqAssertion = px.apply(argsX).equalsTo(py.apply(argsY));

    final Value boundedTupleX = argsX[asList(visibleX).indexOf(tx)];
    final Value boundedTupleY = argsY[asList(visibleY).indexOf(ty)];

    final Proposition assertion =
        ctx.makeForAll(
            argsX,
            ctx.tupleFrom(boundedTupleX, tx)
                .implies(ctx.makeExists(argsY, ctx.tupleFrom(boundedTupleY, ty).and(eqAssertion))));
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

  protected void addAssertion(Constraint constraint, Proposition assertion) {
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
