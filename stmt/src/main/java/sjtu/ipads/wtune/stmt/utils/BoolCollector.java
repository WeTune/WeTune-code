package sjtu.ipads.wtune.stmt.utils;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.CASE_COND;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.WHEN_COND;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_HAVING;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_WHERE;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_ON;

public class BoolCollector implements ASTVistor {
  private final List<ASTNode> boolExprs = new ArrayList<>();

  @Override
  public boolean enterCase(ASTNode _case) {
    // ignore the form CASE cond WHEN val0 THEN ... END,
    // because val0 is not boolean
    return _case.get(CASE_COND) == null;
  }

  @Override
  public boolean enterWhen(ASTNode when) {
    if (when != null) boolExprs.add(when.get(WHEN_COND));
    return false;
  }

  @Override
  public boolean enterChild(ASTNode parent, FieldKey<ASTNode> key, ASTNode child) {
    if (child != null
        && (key == JOINED_ON || key == QUERY_SPEC_WHERE || key == QUERY_SPEC_HAVING)) {
      boolExprs.add(child);
    }
    return true;
  }

  public static List<ASTNode> collect(ASTNode node) {
    final BoolCollector collector = new BoolCollector();
    node.accept(collector);
    return collector.boolExprs;
  }
}
