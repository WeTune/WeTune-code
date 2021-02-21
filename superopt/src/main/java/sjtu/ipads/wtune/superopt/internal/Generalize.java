package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Input;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Numbering;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;
import sjtu.ipads.wtune.superopt.optimization.Substitution;
import sjtu.ipads.wtune.superopt.optimization.SubstitutionRepo;
import sjtu.ipads.wtune.symsolver.core.Constraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;

public class Generalize {
  public static boolean canGeneralize(Substitution sub, SubstitutionRepo repo) {
    final Substitution copy = sub.copy();
    return canGeneralize(copy, s -> s != copy && (repo.contains(s) || repo.contains(s.flip())));
  }

  private static boolean canGeneralize(Substitution sub, Predicate<Substitution> continuation) {
    if (continuation.test(sub)) return true;

    final Fragment g0 = sub.g0(), g1 = sub.g1();
    return shrink(g0, g -> canGeneralize(renewSubstitution(sub, g), continuation))
        || shrink(g1, g -> canGeneralize(renewSubstitution(sub, g), continuation));
  }

  private static boolean shrink(Fragment g, Predicate<Fragment> continuation) {
    for (Input input : collectInputs(g)) {
      final Operator parent = input.successor();
      final Operator grandparent = parent.successor();
      if (grandparent == null) continue;

      grandparent.replacePredecessor(parent, input);

      if (continuation.test(g)) return true;

      grandparent.replacePredecessor(input, parent);
    }

    return false;
  }

  private static Substitution renewSubstitution(Substitution sub, Fragment g) {
    final Fragment g0 = sub.g0() == g ? g : sub.g0();
    final Fragment g1 = sub.g1() == g ? g : sub.g1();

    final Numbering numbering = Numbering.make().number(g0, g1);
    return Substitution.build(
        g0, g1, numbering, listFilter(it -> isValidConstraint(it, numbering), sub.constraints()));
  }

  private static boolean isValidConstraint(Constraint constraint, Numbering numbering) {
    return Arrays.stream(constraint.targets())
        .map(Placeholder.class::cast)
        .mapToInt(numbering::numberOf)
        .noneMatch(it -> it == -1);
  }

  private static List<Input> collectInputs(Fragment g) {
    final InputCollector collector = new InputCollector();
    g.acceptVisitor(collector);
    return collector.inputs;
  }

  private static class InputCollector implements OperatorVisitor {
    private final List<Input> inputs = new ArrayList<>();

    @Override
    public void leaveInput(Input input) {
      inputs.add(input);
    }
  }
}
