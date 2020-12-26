package sjtu.ipads.wtune.symsolver.search.impl;

import com.google.common.collect.Collections2;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Prover;
import sjtu.ipads.wtune.symsolver.smt.Func;
import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtCtx;
import sjtu.ipads.wtune.symsolver.smt.Value;

import java.util.*;

import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.common.utils.FuncUtils.dumb;

public abstract class BaseProver implements Prover {
  protected final SmtCtx ctx;
  protected final Proposition problem;
  protected final Map<Decision, Collection<Proposition>> assertions;
  protected Proposition[] baseAssertions;
  protected Decision[] decisions;

  protected BaseProver(SmtCtx ctx, Proposition problem) {
    this.ctx = ctx;
    this.problem = problem;
    this.assertions = new HashMap<>();
  }

  @Override
  public void tableEq(Constraint constraint, TableSym tx, TableSym ty) {
    final Proposition assertion = ctx.makeFunc(tx).equalsTo(ctx.makeFunc(ty));
    addAssertion(constraint, assertion);
  }

  @Override
  public void pickEq(Constraint constraint, PickSym px, PickSym py) {
    final Proposition assertion = ctx.makeFunc(px).equalsTo(ctx.makeFunc(py));
    addAssertion(constraint, assertion);
  }

  @Override
  public void pickFrom(Constraint constraint, PickSym p, Collection<TableSym> tables) {
    final Value[] tuples = ctx.makeTuples(p.visibleSources().size(), "x");
    final Value[] masked = pickTuples(tuples, p.visibleSources(), tables);
    final Proposition condition = ctx.tuplesFrom(masked, tables);
    final Proposition eqAssertion = ctx.pick(p, tuples).equalsTo(ctx.pick(p, masked));

    final Proposition assertion = ctx.makeForAll(tuples, condition.implies(eqAssertion));
    addAssertion(constraint, assertion);
  }

  @Override
  public void reference(Constraint constraint, TableSym tx, PickSym px, TableSym ty, PickSym py) {
    pickFrom(constraint, px, singleton(tx));
    pickFrom(constraint, py, singleton(ty));

    final Value x = ctx.makeTuple("x"), y = ctx.makeTuple("y");
    final Proposition eqAssertion = ctx.pick(px, x).equalsTo(ctx.pick(py, y));

    final Proposition assertion =
        ctx.makeForAll(
            x,
            ctx.tupleFrom(x, tx).implies(ctx.makeExists(y, ctx.tupleFrom(y, ty).and(eqAssertion))));
    addAssertion(constraint, assertion);
  }

  protected void addAssertion(Constraint constraint, Proposition assertion) {
    assertions.computeIfAbsent(constraint, dumb(ArrayList::new)).add(assertion);
  }

  private Value[] pickTuples(Value[] tuples, List<TableSym> sources, Collection<TableSym> mask) {
    final Value[] maskedTuples = new Value[mask.size()];

    for (int i = 0, j = 0, bound = sources.size(); i < bound; i++)
      if (mask.contains(sources.get(i))) maskedTuples[j] = tuples[i];

    return maskedTuples;
  }

  @Override
  public void prepare(Decision[] choices) {
    for (Decision choice : choices) choice.decide(this);

    baseAssertions =
        ctx.declaredFuncs().stream()
            .filter(it -> it.name().startsWith("combine"))
            .map(this::assertPositionAgnostic)
            .toArray(Proposition[]::new);
  }

  @Override
  public void decide(Decision[] decisions) {
    this.decisions = decisions;
  }

  private Proposition assertPositionAgnostic(Func combine) {
    final int arity = combine.arity();
    if (arity == 1) return null;

    final Value[] tuples = ctx.makeTuples(arity, "x");

    final Proposition eqAssertion =
        Collections2.permutations(Arrays.asList(tuples)).stream()
            .map(combine::apply)
            .reduce(Proposition.tautology(), Value::equalsTo, Proposition::and);

    return ctx.makeForAll(tuples, eqAssertion);
  }
}
