package sjtu.ipads.wtune.stmt.resolver;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLVisitor;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.UnaryOp;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.Statement;

import static sjtu.ipads.wtune.sqlparser.ast.ExprAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.BOOL_EXPR;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;

class ResolveJoinCondition implements SQLVisitor {
  private final Multimap<ColumnRef, ColumnRef> map =
      MultimapBuilder.hashKeys().arrayListValues().build();

  public static void resolve(Statement stmt) {
    stmt.parsed().accept(new ResolveJoinCondition());
  }

  @Override
  public boolean enterBinary(SQLNode binary) {
    final BinaryOp op = binary.get(BINARY_OP);
    final SQLNode left = binary.get(BINARY_LEFT);
    final SQLNode right = binary.get(BINARY_RIGHT);

    if (op == BinaryOp.EQUAL && isColumn(left) && isColumn(right)) {
      SQLNode parent = binary.parent();
      while (EXPR.isInstance(parent)) {
        if (parent.get(UNARY_OP) == UnaryOp.NOT || parent.get(BINARY_OP) == BinaryOp.OR)
          return false;
        parent = parent.parent();
      }

      binary.get(BOOL_EXPR).setJoinCondition(true);
      final ColumnRef leftRef = left.get(RESOLVED_COLUMN_REF);
      final ColumnRef rightRef = right.get(RESOLVED_COLUMN_REF);
      map.put(leftRef, rightRef);
      map.put(rightRef, leftRef);

      return false;
    }

    return true;
  }

  private static boolean isColumn(SQLNode node) {
    final ColumnRef ref = node.get(RESOLVED_COLUMN_REF);
    assert ref == null || ref.refColumn() != null || ref.refItem() != null;
    return ref != null;
  }
}
