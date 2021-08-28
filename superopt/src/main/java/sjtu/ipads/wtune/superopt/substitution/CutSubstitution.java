package sjtu.ipads.wtune.superopt.substitution;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.fragment1.*;

import java.util.List;

import static java.util.Arrays.asList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.all;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.common.utils.TreeNode.treeRootOf;
import static sjtu.ipads.wtune.common.utils.TreeScaffold.displaceGlobal;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.Kind.TableEq;

class CutSubstitution {
  static Pair<Substitution, Substitution> cut(Substitution sub, Op lhsCutPoint, Op rhsCutPoint) {
    final var tri0 = cutFragment(lhsCutPoint);
    final var tri1 = cutFragment(rhsCutPoint);
    final Substitution top =
        mkTop(sub, tri0.getLeft(), tri1.getLeft(), tri0.getMiddle(), tri1.getMiddle());
    final Substitution bottom = mkBottom(sub, tri0.getRight(), tri1.getRight());
    return Pair.of(top, bottom);
  }

  private static Triple<Fragment, Input, Fragment> cutFragment(Op cutPoint) {
    final Op newInput = Op.mk(OperatorType.INPUT);
    final Symbols topSyms = Symbols.mk(), bottomSyms = Symbols.mk();

    final Op topTree = treeRootOf(displaceGlobal(topSyms, cutPoint, newInput, false));
    topSyms.bindSymbol(newInput);
    final Fragment topFragment = Fragment.mk(topTree, topSyms, true);

    final Fragment bottomFragment = Fragment.mk(cutPoint, bottomSyms, false);
    bottomFragment.acceptVisitor(OpVisitor.traverse(bottomSyms::reBindSymbol));

    return Triple.of(topFragment, ((Input) newInput), bottomFragment);
  }

  private static Substitution mkTop(
      Substitution sub, Fragment f0, Fragment f1, Input newInput0, Input newInput1) {
    final Symbols lhsSyms = f0.symbols(), rhsSyms = f1.symbols();
    final List<Constraint> constraints =
        listFilter(sub.constraints(), it -> isRelevant(it, lhsSyms, rhsSyms));
    constraints.add(
        Constraint.mk(
            TableEq,
            lhsSyms.symbolAt(newInput0, Symbol.Kind.TABLE, 0),
            rhsSyms.symbolAt(newInput1, Symbol.Kind.TABLE, 0)));

    return Substitution.mk(f0, f1, constraints);
  }

  private static Substitution mkBottom(Substitution sub, Fragment f0, Fragment f1) {
    final Symbols lhsSyms = f0.symbols(), rhsSyms = f1.symbols();
    final List<Constraint> constraints =
        listFilter(sub.constraints(), it -> isRelevant(it, lhsSyms, rhsSyms));

    return Substitution.mk(f0, f1, constraints);
  }

  private static boolean isRelevant(Constraint constraint, Symbols syms0, Symbols syms1) {
    return all(asList(constraint.symbols()), it -> syms0.contains(it) || syms1.contains(it));
  }
}
