package sjtu.ipads.wtune.superopt.internal;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Input;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Numbering;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;
import sjtu.ipads.wtune.superopt.optimizer.Substitution;
import sjtu.ipads.wtune.superopt.optimizer.SubstitutionBank;
import sjtu.ipads.wtune.superopt.util.Constraints;
import sjtu.ipads.wtune.symsolver.core.Constraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.cartesianProduct;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.superopt.fragment.Fragment.wrap;

public class Generalization {
  private final SubstitutionBank bank;

  public Generalization(SubstitutionBank bank) {
    this.bank = bank;
  }

  public boolean canGeneralize(Substitution sub) {
    final List<Operator> ops0 = operators(sub.g0().head(), new ArrayList<>(4));
    final List<Operator> ops1 = operators(sub.g1().head(), new ArrayList<>(4));

    for (List<Operator> pair : cartesianProduct(ops0, ops1)) {
      final Operator op0 = pair.get(0), op1 = pair.get(1);

      if (op0.successor() == null
          || op1.successor() == null
          || op0.type() == OperatorType.INPUT
          || op1.type() == OperatorType.INPUT) continue;

      final Pair<Substitution, Substitution> cut = cut(sub, op0, op1);
      if (isProved(cut.getLeft()) && isProved(cut.getRight())) return true;
    }

    return false;
  }

  private static Pair<Substitution, Substitution> cut(
      Substitution sub, Operator cutPoint0, Operator cutPoint1) {
    final Input input0 = Input.create(), input1 = Input.create();
    input0.setFragment(sub.g0());
    input1.setFragment(sub.g1());
    final Constraint extraConstraint = Constraint.tableEq(input0.table(), input1.table());

    cutPoint0.successor().replacePredecessor(cutPoint0, input0);
    cutPoint1.successor().replacePredecessor(cutPoint1, input1);

    final Substitution newSub0 = renew(sub.g0(), sub.g1(), sub.constraints(), extraConstraint);
    final Substitution newSub1 = renew(wrap(cutPoint0), wrap(cutPoint1), sub.constraints());

    // restore
    cutPoint0.successor().replacePredecessor(input0, cutPoint0);
    cutPoint1.successor().replacePredecessor(input1, cutPoint1);
    sub.g0().placeholders().remove(input0);
    sub.g1().placeholders().remove(input1);

    return Pair.of(newSub0, newSub1);
  }

  private static Substitution renew(
      Fragment g0, Fragment g1, Constraints constraints, Constraint... extra) {
    final Numbering numbering = Numbering.make().number(g0, g1);
    final List<Constraint> newConstraints = listFilter(constraints, it -> isPresent(it, numbering));
    newConstraints.addAll(Arrays.asList(extra));
    return Substitution.make(g0, g1, numbering, newConstraints).copy();
  }

  private static List<Operator> operators(Operator op, List<Operator> ops) {
    ops.add(op);
    for (Operator predecessor : op.predecessors()) operators(predecessor, ops);
    return ops;
  }

  private static boolean isPresent(Constraint constraint, Numbering numbering) {
    return Arrays.stream(constraint.targets())
        .map(Placeholder.class::cast)
        .mapToInt(numbering::numberOf)
        .noneMatch(it -> it == -1);
  }

  private boolean isProved(Substitution sub) {
    return isIdentity(sub) || bank.contains(sub);
  }

  private static boolean isIdentity(Substitution sub) {
    return sub.equals(sub.flip()) && Substitution.isValid(sub);
  }
}
