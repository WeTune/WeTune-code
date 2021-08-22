package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.fragment1.*;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.flip;

// Check if a substitution can be implied by other substitution.
// We consider a substitution as duplicated if a subtree in one side is proved to be equivalent to a
// subtree in another side.
class DuplicationChecker {
  private final SubstitutionBank bank;
  private Substitution substitution;

  DuplicationChecker(SubstitutionBank bank) {
    this.bank = bank;
  }

  boolean isDuplicated(Substitution substitution) {
    this.substitution = substitution;

    final Fragment f0 = substitution._0(), f1 = substitution._1();
    final Op tree0 = f0.root(), tree1 = f1.root();
    final List<Fragment> subtrees0 =
        listMap(collectSubtrees(tree0), DuplicationChecker::mkPartialFragment);
    final List<Fragment> subtrees1 =
        listMap(collectSubtrees(tree1), DuplicationChecker::mkPartialFragment);

    for (Fragment subtree0 : subtrees0)
      for (Fragment subtree1 : subtrees1)
        if (!(subtree0.root() == f0.root() && subtree1.root() == f1.root())
            && isIndependent(subtree0, subtree1)
            && isImplied(subtree0, subtree1)) {
          return true;
        }

    return false;
  }

  private List<Op> collectSubtrees(Op root) {
    final List<Op> subtrees = new ArrayList<>(5);
    // Exclude the root itself.
    root.acceptVisitor(OpVisitor.traverse(it -> collectSubtree(it, subtrees)));
    return subtrees;
  }

  private void collectSubtree(Op op, List<Op> subtrees) {
    if (op.kind() != OperatorType.INPUT) subtrees.add(op);
  }

  private boolean isImplied(Fragment f0, Fragment f1) {
    if (isImplied(mkPartialSubstitution(f0, f1))) return true;

    boolean result = false;

    final boolean isDedupProjLhs = isEffectiveDedupProj(f0);
    if (isDedupProjLhs) {
      toggleDedup(f0);
      result = isImplied(mkPartialSubstitution(f0, f1));
      toggleDedup(f0);
    }

    final boolean isDedupProjRhs = isEffectiveDedupProj(f1);
    if (!result && isDedupProjRhs) {
      toggleDedup(f1);
      result = isImplied(mkPartialSubstitution(f0, f1));
      toggleDedup(f1);
    }

    if (!result && isDedupProjLhs && isDedupProjRhs) {
      toggleDedup(f0);
      toggleDedup(f1);
      result = isImplied(mkPartialSubstitution(f0, f1));
      toggleDedup(f0);
      toggleDedup(f1);
    }

    return result;
  }

  private boolean isImplied(Substitution substitution) {
    final Substitution flipped = flip(substitution);
    final String s0 = substitution.canonicalStringify(), s1 = flipped.canonicalStringify();
    return s0.equals(s1) || bank.contains(s0) || bank.contains(s1);
  }

  private boolean isIndependent(Fragment f0, Fragment f1) {
    final Symbols lhs = substitution._0().symbols(), rhs = substitution._1().symbols();
    final Symbols syms0 = f0.symbols(), syms1 = f1.symbols();

    for (Constraint constraint : substitution.constraints()) {
      if (!constraint.kind().isEq()) continue;

      boolean inF0 = false, inF1 = false, inLhs = false, inRhs = false;
      for (Symbol sym : constraint.symbols()) {
        if (sym.ctx() == lhs && syms0.contains(sym)) inF0 = true;
        else inLhs = true;
        if (sym.ctx() == rhs && syms1.contains(sym)) inF1 = true;
        else inRhs = true;

        if (!inF0 && !inF1) continue;
        if (!inLhs && !inRhs) continue;

        return false;
      }
    }

    return true;
  }

  private Substitution mkPartialSubstitution(Fragment f0, Fragment f1) {
    final List<Constraint> partialConstraints =
        listFilter(substitution.constraints(), it -> isRelevant(it, f0.symbols(), f1.symbols()));

    return Substitution.mk(f0, f1, partialConstraints);
  }

  private static Fragment mkPartialFragment(Op tree) {
    final Symbols partialSyms = Symbols.mk();
    final Fragment f = Fragment.mk(tree, partialSyms);
    tree.acceptVisitor(OpVisitor.traverse(node -> gatherSym(node, partialSyms)));
    return f;
  }

  private static void gatherSym(Op op, Symbols symbols) {
    symbols.reBindSymbol(op);
  }

  private static boolean isRelevant(Constraint constraint, Symbols symbols0, Symbols symbols1) {
    for (Symbol symbol : constraint.symbols()) {
      if (!symbols0.contains(symbol) && !symbols1.contains(symbol)) return false;
    }
    return true;
  }

  private static boolean isEffectiveDedupProj(Fragment f) {
    final Op op = f.root();
    return op.successor() != null
        && op.successor().kind().isSubquery()
        && op.kind() == OperatorType.PROJ
        && op.successor().predecessors()[1] == op
        && !((Proj) op).isDeduplicated();
  }

  private static void toggleDedup(Fragment f) {
    final Proj proj = (Proj) f.root();
    proj.setDeduplicated(!proj.isDeduplicated());
  }
}
