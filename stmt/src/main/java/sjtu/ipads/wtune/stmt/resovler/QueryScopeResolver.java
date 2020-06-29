package sjtu.ipads.wtune.stmt.resovler;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.attrs.SimpleQueryScope;
import sjtu.ipads.wtune.stmt.attrs.UnionQueryScope;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.*;

import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.JOINED_ON;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_CLAUSE_SCOPE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class QueryScopeResolver implements SQLVisitor, Resolver {
  private static final System.Logger LOG = System.getLogger("Stmt.Resolver.QueryScope");

  private final Deque<QueryScope> scopes = new LinkedList<>();
  private final Deque<QueryScope.Clause> clauses = new LinkedList<>();

  @Override
  public boolean enter(SQLNode node) {
    final SQLNode.Type type = node.type();

    final QueryScope scope;
    if (type == Type.QUERY) {
      final SQLNode body = node.get(QUERY_BODY);
      if (body.type() == Type.QUERY_SPEC) {
        scope = new SimpleQueryScope();
        scope.setSpecNode(body);

      } else if (body.type() == Type.UNION) {
        scope = new UnionQueryScope();
        scope.setLeftChild(body.get(UNION_LEFT));
        scope.setRightChild(body.get(UNION_RIGHT));

      } else throw new IllegalStateException();

      scope.setParent(scopes.peek());
      scope.setQueryNode(node);

      scopes.push(scope);
    }

    final QueryScope currentScope = scopes.peek();
    final QueryScope.Clause currentClause = clauses.peek();
    if (currentScope != null) node.put(RESOLVED_QUERY_SCOPE, currentScope);
    if (currentClause != null) node.put(RESOLVED_CLAUSE_SCOPE, currentClause);

    return true;
  }

  @Override
  public void leave(SQLNode node) {
    final SQLNode.Type type = node.type();

    if (type == SQLNode.Type.QUERY) {
      assert node.get(RESOLVED_QUERY_SCOPE) == scopes.peek();
      scopes.pop();
    }
  }

  private static final Map<Key<?>, SimpleQueryScope.Clause> CLAUSE_KEYS =
      Map.of(
          QUERY_LIMIT,
          SimpleQueryScope.Clause.LIMIT,
          QUERY_OFFSET,
          SimpleQueryScope.Clause.OFFSET,
          QUERY_ORDER_BY,
          SimpleQueryScope.Clause.ORDER_BY,
          QUERY_SPEC_SELECT_ITEMS,
          SimpleQueryScope.Clause.SELECT_ITEM,
          QUERY_SPEC_FROM,
          SimpleQueryScope.Clause.FROM,
          JOINED_ON,
          SimpleQueryScope.Clause.ON,
          QUERY_SPEC_WHERE,
          SimpleQueryScope.Clause.WHERE,
          QUERY_SPEC_HAVING,
          SimpleQueryScope.Clause.HAVING,
          QUERY_SPEC_GROUP_BY,
          SimpleQueryScope.Clause.GROUP_BY);

  @Override
  public boolean enterChild(Key<SQLNode> key, SQLNode child) {
    final SimpleQueryScope.Clause clause = CLAUSE_KEYS.get(key);
    if (clause != null) clauses.push(clause);
    return true;
  }

  @Override
  public boolean enterChildren(Key<List<SQLNode>> key, List<SQLNode> child) {
    final SimpleQueryScope.Clause clause = CLAUSE_KEYS.get(key);
    if (clause != null) clauses.push(clause);
    return true;
  }

  @Override
  public void leaveChild(Key<SQLNode> key, SQLNode child) {
    final SimpleQueryScope.Clause clause = CLAUSE_KEYS.get(key);
    if (clause != null) {
      if (clauses.peek() != clause) throw new ConcurrentModificationException();
      clauses.pop();
    }
  }

  @Override
  public void leaveChildren(Key<List<SQLNode>> key, List<SQLNode> child) {
    final SimpleQueryScope.Clause clause = CLAUSE_KEYS.get(key);
    if (clause != null) {
      if (clauses.peek() != clause) throw new ConcurrentModificationException();
      clauses.pop();
    }
  }

  @Override
  public void resolve(Statement stmt) {
    LOG.log(
        System.Logger.Level.TRACE,
        "resolving query scope for <{0}, {1}>",
        stmt.appName(),
        stmt.stmtId());

    stmt.parsed().accept(new QueryScopeResolver());
  }
}
