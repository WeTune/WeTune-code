package sjtu.ipads.wtune.stmt.support;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.BINARY;
import static sjtu.ipads.wtune.sqlparser.relational.Attribute.ATTRIBUTE;
import static sjtu.ipads.wtune.sqlparser.schema.Column.Flag.FOREIGN_KEY;
import static sjtu.ipads.wtune.sqlparser.schema.Column.Flag.PRIMARY;

import java.util.HashMap;
import java.util.Map;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.schema.Column;

public class InferForeignKey {
  public static Map<Column, Column> analyze(ASTNode node) {
    final Map<Column, Column> inferred = new HashMap<>();
    node.accept(ASTVistor.topDownVisit(it -> infer(it, inferred), BINARY));
    return inferred;
  }

  private static void infer(ASTNode binary, Map<Column, Column> inferred) {
    if (binary.get(BINARY_OP) != BinaryOp.EQUAL) return;
    final Attribute leftAttr = binary.get(BINARY_LEFT).get(ATTRIBUTE);
    final Attribute rightAttr = binary.get(BINARY_RIGHT).get(ATTRIBUTE);
    if (leftAttr != null && rightAttr != null) {
      final Column leftCol = leftAttr.column(true), rightCol = rightAttr.column(true);
      if (leftCol != null && rightCol != null) {
        final boolean isLeftSuspected = isSuspected(leftCol);
        final boolean isRightSuspected = isSuspected(rightCol);
        if (isLeftSuspected && !isRightSuspected) inferred.put(leftCol, rightCol);
        else if (!isLeftSuspected && isRightSuspected) inferred.put(rightCol, leftCol);
      }
    }
  }

  private static boolean isSuspected(Column column) {
    return !column.isFlag(FOREIGN_KEY)
        && !column.isFlag(PRIMARY)
        && (column.dataType().category() == Category.INTEGRAL
            || column.dataType().category() == Category.STRING);
  }
}
