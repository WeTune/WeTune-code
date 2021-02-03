package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.*;

import static sjtu.ipads.wtune.common.utils.Commons.subArray;

public interface Constraint {
  // !!! Impl Note !!!
  // Don't change the impl of compareTo,
  // some optimization depends on the current behaviour.
  // see TraceImpl::fastCheckConflict, FastDecisionTree::new
  Kind kind();

  Object[] targets();

  enum Kind {
    TableEq,
    PickEq,
    PredicateEq,
    PickFrom,
    Reference
  }

  static Constraint tableEq(Object tx, Object ty) {
    return BaseTableEq.build(tx, ty);
  }

  static Constraint pickEq(Object px, Object py) {
    return BasePickEq.build(px, py);
  }

  static Constraint predicateEq(Object px, Object py) {
    return BasePredicateEq.build(px, py);
  }

  static Constraint pickFrom(Object p, Object... ts) {
    return BasePickFrom.build(p, ts);
  }

  static Constraint reference(Object tx, Object px, Object ty, Object py) {
    return BaseReference.build(tx, px, ty, py);
  }

  static Constraint make(Kind kind, Object[] targets) {
    return switch (kind) {
      case TableEq -> tableEq(targets[0], targets[1]);
      case PickEq -> pickEq(targets[0], targets[1]);
      case PredicateEq -> predicateEq(targets[0], targets[1]);
      case PickFrom -> pickFrom(targets[0], subArray(targets, 1));
      case Reference -> reference(targets[0], targets[1], targets[2], targets[3]);
    };
  }
}
