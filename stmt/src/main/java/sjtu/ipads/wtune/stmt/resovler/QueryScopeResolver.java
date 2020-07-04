package sjtu.ipads.wtune.stmt.resovler;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.attrs.SimpleQueryScope;
import sjtu.ipads.wtune.stmt.attrs.UnionQueryScope;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
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

  private static final Map<Key<?>, QueryScope.Clause> CLAUSE_KEYS =
      Arrays.stream(QueryScope.Clause.values())
          .collect(Collectors.toMap(QueryScope.Clause::key, identity()));

  @Override
  public boolean enterChild(SQLNode parent, Key<SQLNode> key, SQLNode child) {
    final SimpleQueryScope.Clause clause = CLAUSE_KEYS.get(key);
    if (clause != null) clauses.push(clause);
    return true;
  }

  @Override
  public boolean enterChildren(SQLNode parent, Key<List<SQLNode>> key, List<SQLNode> child) {
    final SimpleQueryScope.Clause clause = CLAUSE_KEYS.get(key);
    if (clause != null) clauses.push(clause);
    return true;
  }

  @Override
  public void leaveChild(SQLNode parent, Key<SQLNode> key, SQLNode child) {
    final SimpleQueryScope.Clause clause = CLAUSE_KEYS.get(key);
    if (clause != null) {
      if (clauses.peek() != clause) throw new ConcurrentModificationException();
      clauses.pop();
    }
  }

  @Override
  public void leaveChildren(SQLNode parent, Key<List<SQLNode>> key, List<SQLNode> child) {
    final SimpleQueryScope.Clause clause = CLAUSE_KEYS.get(key);
    if (clause != null) {
      if (clauses.peek() != clause) throw new ConcurrentModificationException();
      clauses.pop();
    }
  }

  @Override
  public boolean resolve(Statement stmt) {
    LOG.log(
        System.Logger.Level.TRACE,
        "resolving query scope for <{0}, {1}>",
        stmt.appName(),
        stmt.stmtId());

    stmt.parsed().accept(new QueryScopeResolver());
    return true;
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES = Set.of(IdResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }
}
