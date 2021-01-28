package sjtu.ipads.wtune.stmt.support;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLVisitor;
import sjtu.ipads.wtune.sqlparser.rel.Attribute;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.HashSet;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.BINARY;
import static sjtu.ipads.wtune.sqlparser.rel.Attribute.ATTRIBUTE;
import static sjtu.ipads.wtune.sqlparser.schema.Column.Flag.FOREIGN_KEY;
import static sjtu.ipads.wtune.sqlparser.schema.Column.Flag.UNIQUE;

public class InferForeignKey {
  public static Set<Column> analyze(SQLNode node) {
    final Set<Column> inferred = new HashSet<>();
    node.accept(SQLVisitor.topDownVisit(it -> infer(it, inferred), BINARY));
    return inferred;
  }

  private static void infer(SQLNode binary, Set<Column> inferred) {
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
    if (!column.isFlag(FOREIGN_KEY) && !column.isFlag(UNIQUE)) inferred.add(column);
  }
}
