package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.attrs.SimpleQueryScope;

import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_DISTINCT;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class DropDistinct implements Operator {
  private static final DropDistinct INSTANCE = new DropDistinct();

  public static DropDistinct build() {
    return INSTANCE;
  }

  @Override
  public SQLNode apply(SQLNode sqlNode) {
    final SQLNode specNode = sqlNode.get(RESOLVED_QUERY_SCOPE).specNode();
    if (specNode != null) specNode.unFlag(QUERY_SPEC_DISTINCT);
    return sqlNode;
  }
}
