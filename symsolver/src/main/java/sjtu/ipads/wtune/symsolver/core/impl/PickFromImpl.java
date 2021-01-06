package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Sym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Reactor;
import sjtu.ipads.wtune.symsolver.utils.Indexed;

import java.util.Arrays;
import java.util.Objects;

import static java.util.function.Predicate.not;
import static sjtu.ipads.wtune.common.utils.FuncUtils.sorted;
import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;

public class PickFromImpl implements Constraint {
  private final PickSym p;
  private final TableSym[] ts;

  private PickFromImpl(PickSym p, TableSym[] ts) {
    this.p = p;
    this.ts = sorted(ts, Indexed::compareTo);
  }

  public static Constraint build(PickSym p, TableSym[] ts) {
    if (p == null || ts == null || !p.isIndexed() || stream(ts).anyMatch(not(Indexed::isIndexed)))
      throw new IllegalArgumentException();

    return new PickFromImpl(p, ts);
  }

  @Override
  public Kind kind() {
    return Kind.PickFrom;
  }

  @Override
  public void decide(Reactor reactor) {
    reactor.pickFrom(this, p, ts);
  }

  @Override
  public Sym[] targets() {
    final Sym[] syms = new Sym[1 + ts.length];
    syms[0] = p;

    System.arraycopy(ts, 0, syms, 1, ts.length);
    return syms;
  }

  @Override
  public boolean ignorable() {
    return ts.length == p.visibleSources().length;
  }

  @Override
  public int compareTo(Constraint o) {
    int res = kind().compareTo(o.kind());
    if (res != 0) return res;

    final PickFromImpl other = (PickFromImpl) o;
    res = p.compareTo(other.p);
    return res != 0 ? res : Arrays.compare(ts, other.ts);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PickFromImpl pickFrom = (PickFromImpl) o;
    return Objects.equals(p, pickFrom.p) && Arrays.equals(ts, pickFrom.ts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(p) + Arrays.hashCode(ts);
  }

  @Override
  public String toString() {
    return "PickFrom(" + p + "," + Arrays.toString(ts) + ")";
  }
}
