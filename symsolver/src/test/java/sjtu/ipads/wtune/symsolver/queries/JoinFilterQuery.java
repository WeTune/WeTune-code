package sjtu.ipads.wtune.symsolver.queries;

import sjtu.ipads.wtune.symsolver.core.BaseQueryBuilder;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.PredicateSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;

import java.util.function.Function;

import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.common.utils.Commons.asArray;
import static sjtu.ipads.wtune.common.utils.FuncUtils.supplier;

public class JoinFilterQuery extends BaseQueryBuilder {
  @Override
  protected Function<Value, Proposition> semantic() {
    final TableSym[] tables = supplier(this::makeTable).repeat(2).toArray(TableSym[]::new);
    final PickSym[] picks = supplier(this::makePick).repeat(4).toArray(PickSym[]::new);
    final PredicateSym pred = makePredicate();

    final TableSym t0 = tables[0];
    final TableSym t1 = tables[1];
    final PickSym p0 = picks[0];
    final PickSym p1 = picks[1];
    final PickSym p2 = picks[2];
    final PickSym p3 = picks[3];

    p0.setVisibleSources(asArray(t0, t1));
    p1.setVisibleSources(asArray(t0, t1));
    p2.setVisibleSources(asArray(t1));
    p3.setVisibleSources(asArray(t1));
    p0.setViableSources(singleton(singleton(t0)));
    p1.setViableSources(singleton(singleton(t1)));
    p2.setViableSources(singleton(singleton(t1)));
    p3.setViableSources(singleton(singleton(t1)));

    p0.setJoined(p1);

    final Value a = makeTuple(), b = makeTuple();
    final Proposition from = ctx().tupleFrom(a, t0).and(ctx().tupleFrom(b, t1));
    final Proposition filter = (Proposition) pred.apply(p3.apply(b));
    final Proposition join = p0.apply(a).equalsTo(p1.apply(p2.apply(b)));

    return x ->
        ctx()
            .makeExists(
                asArray(a, b),
                x.equalsTo(ctx().makeCombine(a, p2.apply(b))).and(from).and(filter).and(join));
  }
}
