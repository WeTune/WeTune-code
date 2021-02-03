package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;

import java.util.Arrays;

import static java.util.function.Predicate.not;
import static sjtu.ipads.wtune.common.utils.Commons.sorted;
import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;

public class DecidablePickFrom extends BasePickFrom<TableSym, PickSym>
    implements DecidableConstraint {

  private DecidablePickFrom(PickSym p, TableSym[] ts) {
    super(p, ts);
  }

  protected static void checkIndex(Indexed p, Indexed[] ts) {
    if (p == null || ts == null || !p.isIndexed() || stream(ts).anyMatch(not(Indexed::isIndexed)))
      throw new IllegalArgumentException();
  }

  public static DecidableConstraint build(PickSym p, TableSym[] ts) {
    checkIndex(p, ts);

    return new DecidablePickFrom(p, sorted(ts, Indexed::compareTo));
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.pickFrom(this, p(), ts());
  }

  @Override
  public boolean ignorable() {
    return ts().length == p().visibleSources().length;
  }

  @Override
  public int compareTo(DecidableConstraint o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final DecidablePickFrom other = (DecidablePickFrom) o;
    res = p().compareTo(other.p());
    return res != 0 ? res : Arrays.compare(ts(), other.ts());
  }
}
