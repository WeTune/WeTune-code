package sjtu.ipads.wtune.symsolver.queries;

import sjtu.ipads.wtune.common.utils.ISupplier;
import sjtu.ipads.wtune.symsolver.core.BaseQueryBuilder;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Scoped;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;
import sjtu.ipads.wtune.symsolver.utils.SimpleScoped;

import java.util.Set;
import java.util.function.Function;

import static com.google.common.collect.Sets.powerSet;
import static sjtu.ipads.wtune.common.utils.Commons.asArray;

public class SimpleProjQuery extends BaseQueryBuilder {
  @Override
  protected Function<Value, Proposition> semantic() {
    final Object scope = new Object();
    final ISupplier<Scoped> supplier = () -> new SimpleScoped(scope);

    final TableSym table = tableSym(supplier.get());
    final PickSym p0 = pickSym(supplier.get());

    p0.setVisibleSources(asArray(table));
    p0.setViableSources(powerSet(Set.of(table)));

    final Value a = newTuple();
    final Proposition from = ctx().tupleFrom(a, table);

    return x -> ctx().makeExists(a, x.equalsTo(p0.apply(a)).and(from));
  }
}
