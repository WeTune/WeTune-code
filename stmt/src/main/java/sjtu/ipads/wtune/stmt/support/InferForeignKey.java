package sjtu.ipads.wtune.stmt.support;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.HashSet;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.BINARY;
import static sjtu.ipads.wtune.sqlparser.relational.Attribute.ATTRIBUTE;
import static sjtu.ipads.wtune.sqlparser.schema.Column.Flag.FOREIGN_KEY;
import static sjtu.ipads.wtune.sqlparser.schema.Column.Flag.PRIMARY;

public class InferForeignKey {
  public static Set<Column> analyze(ASTNode node) {
    final Set<Column> inferred = new HashSet<>();
    node.accept(ASTVistor.topDownVisit(it -> infer(it, inferred), BINARY));
    return inferred;
  }

  private static void infer(ASTNode binary, Set<Column> inferred) {
    final Attribute leftAttr = binary.get(BINARY_LEFT).get(ATTRIBUTE);
    final Attribute rightAttr = binary.get(BINARY_RIGHT).get(ATTRIBUTE);
    if (leftAttr != null && rightAttr != null) {
      final Column leftCol = leftAttr.column(true), rightCol = rightAttr.column(true);
      if (leftCol != null && rightCol != null) {
        addSuspect(leftCol, inferred);
        addSuspect(rightCol, inferred);
      }
    }
  }

  private static void addSuspect(Column column, Set<Column> inferred) {
    if (!column.isFlag(FOREIGN_KEY)
        && !column.isFlag(PRIMARY)
        && (column.dataType().category() == Category.INTEGRAL
            || column.dataType().category() == Category.STRING)) inferred.add(column);
  }
}
