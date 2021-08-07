package sjtu.ipads.wtune.superopt.enumeration;

import sjtu.ipads.wtune.prover.logic.LogicCtx;
import sjtu.ipads.wtune.superopt.fragment1.Fragment;

public class EnumerationSupport {
  public static EnumerationTree mkTree(Fragment f0, Fragment f1, LogicCtx logicCtx) {
    final ConstraintsIndex constraints = ConstraintsIndex.mk(f0, f1);
    return new EnumerationTreeImpl(f0, f1, constraints, logicCtx);
  }
}
