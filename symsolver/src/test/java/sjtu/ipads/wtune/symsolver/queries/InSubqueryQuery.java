package sjtu.ipads.wtune.symsolver.queries;

import sjtu.ipads.wtune.common.utils.ISupplier;
import sjtu.ipads.wtune.symsolver.core.BaseQueryBuilder;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Scoped;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;
import sjtu.ipads.wtune.symsolver.utils.SimpleScoped;

import java.util.function.Function;

import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.common.utils.Commons.asArray;
import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayMap;

public class InSubqueryQuery extends BaseQueryBuilder {
  @Override
  protected Function<Value, Proposition> semantic() {
    final Object scope = new Object();
    final ISupplier<Scoped> supplier = () -> new SimpleScoped(scope);

    final TableSym[] tables = arrayMap(this::tableSym, TableSym.class, supplier.repeat(2));
    final PickSym[] picks = arrayMap(this::pickSym, PickSym.class, supplier.repeat(3));

    final TableSym t0 = tables[0];
    final TableSym t1 = tables[1];
    final PickSym p0 = picks[0];
    final PickSym p1 = picks[1];
    final PickSym p2 = picks[2];

    p0.setVisibleSources(asArray(t0));
    p1.setVisibleSources(asArray(t0));
    p2.setVisibleSources(asArray(t1));
    p0.setViableSources(singleton(singleton(t0)));
    p1.setViableSources(singleton(singleton(t0)));
    p2.setViableSources(singleton(singleton(t1)));

    p1.setJoined(p2);

    final Value a = newTuple(), b = newTuple();
    final Proposition from = ctx().tupleFrom(a, t0).and(ctx().tupleFrom(b, t1));
    final Proposition join = p1.apply(a).equalsTo(p2.apply(b));

    return x -> ctx().makeExists(asArray(a, b), x.equalsTo(p0.apply(a)).and(from).and(join));
  }
}
