package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.stmt.collector.Collector;

import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.ARRAY_CONTAINS;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.IN_LIST;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.*;

class NormalizeTuple {
  public static SQLNode normalize(SQLNode node) {
    Collector.collect(node, NormalizeTuple::isTuple).forEach(NormalizeTuple::normalizeTuple);
    return node;
  }

  private static boolean isTuple(SQLNode node) {
    final SQLNode parent = node.parent();
    if (parent == null) return false;

    final BinaryOp op = parent.get(BINARY_OP);
    final SQLNode right = parent.get(BINARY_RIGHT);
    return (op == ARRAY_CONTAINS || op == IN_LIST) && right == node;
  }

  private static void normalizeTuple(SQLNode node) {
    final List<SQLNode> paramMarker = Collections.singletonList(SQLNode.simple(PARAM_MARKER));
    if (ARRAY.isInstance(node)) node.put(ARRAY_ELEMENTS, paramMarker);
    else if (TUPLE.isInstance(node)) node.put(TUPLE_EXPRS, paramMarker);
    else throw new IllegalArgumentException();
  }
}
