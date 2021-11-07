package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType;
import sjtu.ipads.wtune.sqlparser.ast.constants.TernaryOp;
import sjtu.ipads.wtune.sqlparser.plan.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;

class EffectiveInnerJoinInference {
  static PlanNode inference(PlanNode node) {
    final List<Ref> nonNullAttrs = switch (node.kind()) {
      case SIMPLE_FILTER -> gatherRequiredNonNullAttrs(((SimpleFilterNode) node).predicate());
      case IN_SUB_FILTER -> ((InSubFilterNode) node).lhsRefs();
      case INNER_JOIN -> node.refs();
      default -> Collections.emptyList();
    };

    final PlanContext ctx = node.context();
    nonNullAttrs.forEach(it -> enforceNonNull(it, ctx));

    for (PlanNode predecessor : node.predecessors()) inference(predecessor);

    return node;
  }

  private static void enforceNonNull(Ref ref, PlanContext ctx) {
    final PlanNode surface = ctx.ownerOf(ref);
    PlanNode path = ctx.ownerOf(ctx.deRef(ref));
    while (path.successor() != surface) {
      final PlanNode successor = path.successor();
      // A tentative patch.
      // `successor` shouldn't be null by design. But some uncovered bugs makes it happen.
      if (successor == null) return;

      if (successor.kind() == OperatorType.LEFT_JOIN && successor.predecessors()[1] == path) {
        ((JoinNode) successor).setJoinType(OperatorType.INNER_JOIN);
      }
      path = path.successor();
    }
  }

  private static List<Ref> gatherRequiredNonNullAttrs(Expr expr) {
    final RefBag refs = expr.refs();
    final List<ASTNode> holes = expr.holes();

    final List<Ref> nonNullAttrs = new ArrayList<>(refs.size());
    for (int i = 0, bound = holes.size(); i < bound; i++)
      if (isRequiredNonNull(holes.get(i))) {
        nonNullAttrs.add(refs.get(i));
      }

    return nonNullAttrs;
  }

  private static boolean isRequiredNonNull(ASTNode hole) {
    final ASTNode parent = hole.parent();
    final BinaryOp binOp = parent.get(BINARY_OP);

    if (binOp != null) {
      final LiteralType literalType = parent.get(BINARY_RIGHT).get(LITERAL_TYPE);
      return binOp != BinaryOp.IS
          || (literalType != LiteralType.NULL && literalType != LiteralType.UNKNOWN);
    }

    final TernaryOp ternaryOp = parent.get(TERNARY_OP);
    if (ternaryOp != null) return hole == parent.get(TERNARY_LEFT);

    return false;
  }
}
