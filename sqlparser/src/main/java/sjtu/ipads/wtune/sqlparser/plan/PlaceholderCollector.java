package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;

class PlaceholderCollector implements ASTVistor {
  List<ASTNode> placeholders = new ArrayList<>();

  @Override
  public boolean enter(ASTNode node) {
    if (COLUMN_REF.isInstance(node)
        && "?".equals(node.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_COLUMN))) {
      placeholders.add(node);
    }
    return true;
  }
}
