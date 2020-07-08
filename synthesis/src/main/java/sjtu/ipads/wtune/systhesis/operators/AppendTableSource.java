package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLTableSource;

import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_BODY;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_FROM;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class AppendTableSource implements Operator {
  private final SQLNode source;
  private final SQLNode joinCondition;
  private final SQLTableSource.JoinType joinType;

  private AppendTableSource(
      SQLNode source, SQLNode joinCondition, SQLTableSource.JoinType joinType) {
    this.source = source;
    this.joinCondition = joinCondition;
    this.joinType = joinType;
  }

  public static AppendTableSource build(
      SQLNode source, SQLNode joinCondition, SQLTableSource.JoinType joinType) {
    assert source != null;
    return new AppendTableSource(source, joinCondition, joinType);
  }

  @Override
  public SQLNode apply(SQLNode sqlNode) {
    final SQLNode querySpecNode = sqlNode.get(RESOLVED_QUERY_SCOPE).queryNode().get(QUERY_BODY);
    if (querySpecNode == null) return sqlNode;

    final SQLNode fromClause = querySpecNode.get(QUERY_SPEC_FROM);
    SQLNode newFromClause = source;
    if (fromClause != null) {
      SQLNode joinPoint = source;
      while (joinPoint.get(JOINED_LEFT) != null) joinPoint = joinPoint.get(JOINED_LEFT);

      final SQLNode joined = joined(fromClause, joinPoint, joinType);
      joined.put(JOINED_ON, joinCondition);

      final SQLNode parent = joinPoint.parent();
      if (parent != null) {
        if (parent.get(JOINED_LEFT) == joinPoint) parent.put(JOINED_LEFT, joined);
        else if (parent.get(JOINED_RIGHT) == joinPoint) parent.put(JOINED_RIGHT, joined);
        else newFromClause = joined;
      } else newFromClause = joined;

      //      joinPoint.replaceThis(joined);
    }

    querySpecNode.put(QUERY_SPEC_FROM, newFromClause);

    return sqlNode;
  }
}
