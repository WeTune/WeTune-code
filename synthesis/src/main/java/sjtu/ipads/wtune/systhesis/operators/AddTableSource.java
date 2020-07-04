package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLTableSource;

import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_BODY;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_FROM;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class AddTableSource implements Operator {
  private final SQLNode source;
  private final SQLNode joinCondition;
  private final SQLTableSource.JoinType joinType;

  private SQLNode pointer;

  private AddTableSource(SQLNode source, SQLNode joinCondition, SQLTableSource.JoinType joinType) {
    this.source = source;
    this.joinCondition = joinCondition;
    this.joinType = joinType;
    this.pointer = source;
  }

  public static Operator build(
      SQLNode source, SQLNode joinCondition, SQLTableSource.JoinType joinType) {
    assert source != null;
    return new AddTableSource(source, joinCondition, joinType);
  }

  public SQLNode pointer() {
    return pointer;
  }

  @Override
  public SQLNode apply(SQLNode sqlNode) {
    final SQLNode querySpecNode = sqlNode.get(RESOLVED_QUERY_SCOPE).queryNode().get(QUERY_BODY);
    if (querySpecNode == null) return sqlNode;

    final SQLNode fromClause = querySpecNode.get(QUERY_SPEC_FROM);
    if (fromClause != null) {
      SQLNode joinPoint = source;
      while (joinPoint.get(JOINED_LEFT) != null) joinPoint = joinPoint.get(JOINED_LEFT);

      final SQLNode joined = joined(fromClause, pointer = joinPoint.copy(), joinType);
      joined.put(JOINED_ON, joinCondition);

      joinPoint.replaceThis(joined);
    }

    querySpecNode.put(QUERY_SPEC_FROM, source);

    return sqlNode;
  }
}
