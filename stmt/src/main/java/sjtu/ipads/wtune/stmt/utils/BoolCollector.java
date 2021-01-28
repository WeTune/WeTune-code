package sjtu.ipads.wtune.stmt.utils;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLVisitor;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.CASE_COND;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.WHEN_COND;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_HAVING;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_WHERE;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_ON;

public class BoolCollector implements SQLVisitor {
  private final List<SQLNode> boolExprs = new ArrayList<>();

  @Override
  public boolean enterCase(SQLNode _case) {
    // ignore the form CASE cond WHEN val0 THEN ... END,
    // because val0 is not boolean
    return _case.get(CASE_COND) == null;
  }

  @Override
  public boolean enterWhen(SQLNode when) {
    if (when != null) boolExprs.add(when.get(WHEN_COND));
    return false;
  }

  @Override
  public boolean enterChild(SQLNode parent, FieldKey<SQLNode> key, SQLNode child) {
    if (child != null
        && (key == JOINED_ON || key == QUERY_SPEC_WHERE || key == QUERY_SPEC_HAVING)) {
      boolExprs.add(child);
      return false;
    }
    return true;
  }

  public static List<SQLNode> collect(SQLNode node) {
    final BoolCollector collector = new BoolCollector();
    node.accept(collector);
    return collector.boolExprs;
  }
}