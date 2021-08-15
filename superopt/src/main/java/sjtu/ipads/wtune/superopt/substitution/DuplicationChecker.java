package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.fragment1.*;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
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
    final List<Op> subtrees0 = collectSubtrees(f0.root()), subtrees1 = collectSubtrees(f1.root());

    for (Op subtree0 : subtrees0)
      for (Op subtree1 : subtrees1)
        if (isImplied(subtree0, subtree1)) {
          return true;
        }

    return false;
  }

  private List<Op> collectSubtrees(Op root) {
    final List<Op> subtrees = new ArrayList<>(5);
    // Exclude the root itself.
    for (Op predecessor : root.predecessors())
      predecessor.acceptVisitor(OpVisitor.traverse(it -> collectSubtree(it, subtrees)));
    return subtrees;
  }

  private void collectSubtree(Op op, List<Op> subtrees) {
    if (op.kind() != OperatorType.INPUT) subtrees.add(op);
  }

  private boolean isImplied(Op tree0, Op tree1) {
    final Substitution partialSub = mkPartialSubstitution(tree0, tree1);
    final Substitution flipped = flip(partialSub);
    final String s0 = partialSub.canonicalStringify(), s1 = flipped.canonicalStringify();
    return s0.equals(s1) || bank.contains(s0) || bank.contains(s1);
  }

  private Substitution mkPartialSubstitution(Op tree0, Op tree1) {
    final Symbols partialSyms0 = Symbols.mk(), partialSyms1 = Symbols.mk();
    final Fragment f0 = Fragment.mk(tree0, partialSyms0);
    final Fragment f1 = Fragment.mk(tree1, partialSyms1);

    tree0.acceptVisitor(OpVisitor.traverse(node -> gatherSym(node, partialSyms0)));
    tree1.acceptVisitor(OpVisitor.traverse(node -> gatherSym(node, partialSyms1)));
    final List<Constraint> partialConstraints =
        listFilter(substitution.constraints(), it -> isRelevant(it, partialSyms0, partialSyms1));

    return Substitution.mk(f0, f1, partialConstraints);
  }

  private boolean isRelevant(Constraint constraint, Symbols symbols0, Symbols symbols1) {
    for (Symbol symbol : constraint.symbols()) {
      if (!symbols0.contains(symbol) && !symbols1.contains(symbol)) return false;
    }
    return true;
  }

  private void gatherSym(Op op, Symbols symbols) {
    symbols.reBindSymbol(op);
  }
}
