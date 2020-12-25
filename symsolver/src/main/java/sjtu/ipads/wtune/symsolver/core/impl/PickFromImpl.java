package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Reactor;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;

import java.util.Collection;
import java.util.Objects;

public class PickFromImpl implements Constraint {
  private final PickSym p;
  private final Collection<TableSym> ts;

  private PickFromImpl(PickSym p, Collection<TableSym> ts) {
    this.p = p;
    this.ts = ts;
  }

  public static Constraint build(PickSym p, Collection<TableSym> ts) {
    if (p == null || ts == null) throw new IllegalArgumentException();
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PickFromImpl pickFrom = (PickFromImpl) o;
    return Objects.equals(p, pickFrom.p) && Objects.equals(ts, pickFrom.ts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(p, ts);
  }

  @Override
  public String toString() {
    return "PickFrom(" + p + "," + ts + ")";
  }
}
