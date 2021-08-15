package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.prover.logic.LogicCtx;
import sjtu.ipads.wtune.superopt.fragment1.Fragment;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class ConstraintSupport {
  public static ConstraintEnumerator mkConstraintEnumerator(
      Fragment f0, Fragment f1, LogicCtx logicCtx) {
    final ConstraintsIndex constraints = ConstraintsIndex.mk(f0, f1);
    return new ConstraintEnumeratorImpl(f0, f1, constraints, logicCtx);
  }

  public static List<Substitution> enumConstraints(Fragment f0, Fragment f1, LogicCtx logicCtx) {
    return listMap(
        mkConstraintEnumerator(f0, f1, logicCtx).enumerate(), it -> Substitution.mk(f0, f1, it));
  }
}
