package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.binary;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_BODY;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class AppendPredicateToClause implements Operator {
  private final SQLNode predicate;
  private final QueryScope.Clause clause;
  private final SQLExpr.BinaryOp op;

  private AppendPredicateToClause(SQLNode predicate, QueryScope.Clause clause, SQLExpr.BinaryOp op) {
    this.predicate = predicate;
    this.clause = clause;
    this.op = op;
  }

  /** Take care to make sure `node` is a bool expr! */
  public static Operator build(SQLNode node, QueryScope.Clause clause, SQLExpr.BinaryOp op) {
    if (op == null || (clause != QueryScope.Clause.WHERE && clause != QueryScope.Clause.HAVING))
      return null;
    return new AppendPredicateToClause(node, clause, op);
  }

  @Override
  public SQLNode apply(SQLNode sqlNode) {
    final SQLNode querySpecNode = sqlNode.get(RESOLVED_QUERY_SCOPE).queryNode().get(QUERY_BODY);
    if (querySpecNode == null) return sqlNode;

    final SQLNode clauseNode = (SQLNode) querySpecNode.get(clause.key());
    if (clauseNode == null) querySpecNode.put(clause.key().name(), predicate);
    else querySpecNode.put(clause.key().name(), binary(clauseNode, predicate, op));

    return sqlNode;
  }
}
