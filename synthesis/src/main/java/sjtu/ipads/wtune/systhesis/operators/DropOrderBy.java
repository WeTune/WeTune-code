package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;

import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_ORDER_BY;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class DropOrderBy implements Operator {
  @Override
  public SQLNode apply(SQLNode sqlNode) {
    sqlNode.get(RESOLVED_QUERY_SCOPE).queryNode().remove(QUERY_ORDER_BY);
    return sqlNode;
  }
}
