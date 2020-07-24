package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.AGGREGATE_DISTINCT;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.AGGREGATE_NAME;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_SELECT_ITEMS;
import static sjtu.ipads.wtune.sqlparser.SQLNode.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class DropCountDistinct implements Operator {

  private static final DropCountDistinct INSTANCE = new DropCountDistinct();

  public static DropCountDistinct build() {
    return INSTANCE;
  }

  @Override
  public SQLNode apply(SQLNode node) {
    final SQLNode specNode = node.get(RESOLVED_QUERY_SCOPE).specNode();
    assert specNode != null;
    for (SQLNode item : specNode.get(QUERY_SPEC_SELECT_ITEMS)) {
      final SQLNode expr = item.get(SELECT_ITEM_EXPR);
      if (expr.isFlagged(AGGREGATE_DISTINCT)) expr.unFlag(AGGREGATE_DISTINCT);
    }
    return node;
  }
}
