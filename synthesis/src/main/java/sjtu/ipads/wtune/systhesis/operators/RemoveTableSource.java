package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLTableSource;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.attrs.TableSource;

import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_FROM;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class RemoveTableSource implements Operator {
  private String target;

  public RemoveTableSource(String target) {
    this.target = target;
  }

  @Override
  public SQLNode apply(SQLNode sqlNode) {
    final QueryScope scope = sqlNode.get(RESOLVED_QUERY_SCOPE);
    final TableSource tableSource = scope.resolveTable(target);
    if (tableSource == null) return sqlNode;

    final SQLNode node = tableSource.node();
    final SQLNode parent = node.parent();

    if (parent.type() == SQLNode.Type.TABLE_SOURCE) {
      assert parent.get(TABLE_SOURCE_KIND) == SQLTableSource.Kind.JOINED;

      final SQLNode left = parent.get(JOINED_LEFT);
      final SQLNode right = parent.get(JOINED_RIGHT);

      if (left == node) parent.replaceThis(right);
      else if (right == node) parent.replaceThis(left);
      else assert false;

    } else if (parent.type() == SQLNode.Type.QUERY_SPEC) {
      parent.put(QUERY_SPEC_FROM, null);

    } else assert false;

    scope.removeTable(tableSource);
    return sqlNode;
  }
}
