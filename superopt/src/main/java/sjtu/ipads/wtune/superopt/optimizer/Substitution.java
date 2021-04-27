package sjtu.ipads.wtune.superopt.optimizer;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;
import static sjtu.ipads.wtune.symsolver.core.Constraint.Kind.PickEq;
import static sjtu.ipads.wtune.symsolver.core.Constraint.Kind.PredicateEq;
import static sjtu.ipads.wtune.symsolver.core.Constraint.Kind.TableEq;

import java.util.List;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Numbering;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholders;
import sjtu.ipads.wtune.superopt.optimizer.internal.SubstitutionImpl;
import sjtu.ipads.wtune.superopt.util.Constraints;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.EqConstraint;

public interface Substitution {
  int index();

  void setIndex(int i);

  Fragment g0();

  Fragment g1();

  Numbering numbering();

  Constraints constraints();

  default Substitution flip() {
    return make(
        g1(),
        g0(),
        Numbering.make().number(g1(), g0()),
        listMap(Substitution::flip, constraints()));
  }

  default Substitution copy() {
    return rebuild(toString());
  }

  static Substitution make(
      Fragment g0, Fragment g1, Numbering numbering, List<Constraint> constraints) {
    return SubstitutionImpl.build(g0, g1, numbering, constraints);
  }

  static Substitution rebuild(String str) {
    return SubstitutionImpl.build(str);
  }

  /** Check if a substitution have all the placeholders in target constrained by Eq relation. */
  static boolean isValid(Substitution sub) {
    final Placeholders placeholders = sub.g1().placeholders();
    final Constraints constraints = sub.constraints();

    for (Placeholder table : placeholders.tables())
      if (stream(constraints).noneMatch(it -> it.kind() == TableEq && it.involves(table)))
        return false;

    for (Placeholder pick : placeholders.picks())
      if (stream(constraints).noneMatch(it -> it.kind() == PickEq && it.involves(pick)))
        return false;

    for (Placeholder pred : placeholders.predicates())
      if (stream(constraints).noneMatch(it -> it.kind() == PredicateEq && it.involves(pred)))
        return false;

    return true;
  }

  private static Constraint flip(Constraint constraint) {
    if (constraint instanceof EqConstraint) {
      final EqConstraint<Placeholder> eq = (EqConstraint<Placeholder>) constraint;
      final Placeholder left = eq.left(), right = eq.right();
      if (left.owner().fragment() != right.owner().fragment()) return constraint.flip();
    }

    return constraint;
  }
}
