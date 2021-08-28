package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Op;
import sjtu.ipads.wtune.superopt.fragment.OpVisitor;
import sjtu.ipads.wtune.superopt.fragment.Proj;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.INPUT;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.cutSubstitution;
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
    for (Op subtree0 : collectSubtrees(tree0)) {
      for (Op subtree1 : collectSubtrees(tree1)) {
        if (subtree0 == tree0 || subtree1 == tree1) continue;
        if (subtree0.kind() == INPUT && subtree1.kind() == INPUT) continue;
        return isImplied(subtree0, subtree1);
      }
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
    if (op.kind() != INPUT) subtrees.add(op);
  }

  private boolean isImplied(Op cutPoint0, Op cutPoint1) {
    final var pair = cutSubstitution(substitution, cutPoint0, cutPoint1);
    final Substitution s0 = pair.getLeft(), s1 = pair.getRight();
    return isImplied(s0) && isImplied(s1);
  }

  private boolean isImplied(Substitution substitution) {
    final Substitution flipped = flip(substitution);
    final String s0 = substitution.canonicalStringify(), s1 = flipped.canonicalStringify();
    return s0.equals(s1) || bank.contains(s0) || bank.contains(s1);
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
