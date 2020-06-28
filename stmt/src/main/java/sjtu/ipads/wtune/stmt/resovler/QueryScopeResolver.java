package sjtu.ipads.wtune.stmt.resovler;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.attrs.UnionQueryScope;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Deque;
import java.util.LinkedList;

import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class QueryScopeResolver implements SQLVisitor, Resolver {
  private Deque<QueryScope> scopes = new LinkedList<>();

  @Override
  public boolean enter(SQLNode node) {
    final SQLNode.Type type = node.type();

    if (type == SQLNode.Type.QUERY) {
      final QueryScope scope = new QueryScope();
      scope.setParent(scopes.peek());
      scope.setQueryNode(node);
      scope.setFirstSpecNode(node.get(QUERY_BODY));

      scopes.push(scope);

    } else if (type == SQLNode.Type.UNION) {
      final UnionQueryScope scope = new UnionQueryScope();
      scope.setParent(scopes.peek());
      scope.setQueryNode(node);
      scope.setFirstSpecNode(node.get(UNION_LEFT));
      scope.setSecondSpecNode(node.get(UNION_RIGHT));

      scopes.push(scope);
    }

    final QueryScope currentScope = scopes.peek();
    if (currentScope != null) node.put(RESOLVED_QUERY_SCOPE, currentScope);

    return true;
  }

  @Override
  public void resolve(Statement stmt) {
    stmt.parsed().accept(new QueryScopeResolver());
  }
}
