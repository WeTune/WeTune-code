package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.stmt.utils.Collector;

import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.ARRAY_CONTAINS;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.IN_LIST;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.*;

class NormalizeTuple {
  public static ASTNode normalize(ASTNode node) {
    Collector.collect(node, NormalizeTuple::isTuple).forEach(NormalizeTuple::normalizeTuple);
    return node;
  }

  private static boolean isTuple(ASTNode node) {
    final ASTNode parent = node.parent();
    if (parent == null) return false;

    final BinaryOp op = parent.get(BINARY_OP);
    final ASTNode right = parent.get(BINARY_RIGHT);
    return (op == ARRAY_CONTAINS || op == IN_LIST) && right == node;
  }

  private static void normalizeTuple(ASTNode node) {
    final List<ASTNode> paramMarker = Collections.singletonList(ASTNode.expr(PARAM_MARKER));
    if (ARRAY.isInstance(node)) node.set(ARRAY_ELEMENTS, paramMarker);
    else if (TUPLE.isInstance(node)) node.set(TUPLE_EXPRS, paramMarker);
    else throw new IllegalArgumentException();
  }
}
