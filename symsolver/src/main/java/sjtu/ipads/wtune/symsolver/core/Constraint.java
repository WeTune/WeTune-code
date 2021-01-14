package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.*;

public interface Constraint extends Comparable<Constraint> {
  // !!! Impl Note !!!
  // Don't change the impl of compareTo,
  // some optimization depends on the current behaviour.
  // see TraceImpl::fastCheckConflict.
  Kind kind();

  Indexed[] targets();

  default <T extends Indexed> Constraint unwrap(Class<T> cls) {
    return this;
  }

  enum Kind {
    TableEq,
    PickEq,
    PredicateEq,
    PickFrom,
    Reference
  }

  static Constraint tableEq(Indexed tx, Indexed ty) {
    return BaseTableEq.build(tx, ty);
  }

  static Constraint pickEq(Indexed px, Indexed py) {
    return BasePickEq.build(px, py);
  }

  static Constraint predicateEq(Indexed px, Indexed py) {
    return BasePredicateEq.build(px, py);
  }

  static Constraint pickFrom(Indexed p, Indexed... ts) {
    return BasePickFrom.build(p, ts);
  }

  static Constraint reference(Indexed tx, Indexed px, Indexed ty, Indexed py) {
    return BaseReference.build(tx, px, ty, py);
  }
}
