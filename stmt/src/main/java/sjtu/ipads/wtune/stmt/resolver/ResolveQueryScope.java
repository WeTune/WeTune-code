package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.NodeAttrs;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLVisitor;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.attrs.SimpleQueryScope;
import sjtu.ipads.wtune.stmt.attrs.UnionQueryScope;
import sjtu.ipads.wtune.stmt.Statement;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_CLAUSE_SCOPE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

class ResolveQueryScope implements SQLVisitor {
  private static final Map<Attrs.Key<?>, QueryScope.Clause> CLAUSE_KEYS =
      Arrays.stream(QueryScope.Clause.values())
          .collect(Collectors.toMap(QueryScope.Clause::key, identity()));

  private final Deque<QueryScope> scopes = new LinkedList<>();
  private final Deque<QueryScope.Clause> clauses = new LinkedList<>();

  public static void resolve(Statement stmt) {
    stmt.parsed().accept(new ResolveQueryScope());
  }

  @Override
  public boolean enter(SQLNode node) {
    final NodeType type = node.nodeType();

    final QueryScope scope;
    if (type == NodeType.QUERY) {
      final SQLNode body = node.get(NodeAttrs.QUERY_BODY);
      if (body.nodeType() == NodeType.QUERY_SPEC) {
        scope = new SimpleQueryScope();
        scope.setSpecNode(body);

      } else if (body.nodeType() == NodeType.SET_OP) {
        scope = new UnionQueryScope();
        scope.setLeftChild(body.get(NodeAttrs.SET_OP_LEFT));
        scope.setRightChild(body.get(NodeAttrs.SET_OP_RIGHT));

      } else throw new StmtException("unexpected query scope");

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
    final NodeType type = node.nodeType();

    if (type == NodeType.QUERY) {
      assert node.get(RESOLVED_QUERY_SCOPE) == scopes.peek();
      scopes.pop();
    }
  }

  @Override
  public boolean enterChild(SQLNode parent, Attrs.Key<SQLNode> key, SQLNode child) {
    final SimpleQueryScope.Clause clause = CLAUSE_KEYS.get(key);
    if (clause != null) clauses.push(clause);
    return true;
  }

  @Override
  public boolean enterChildren(SQLNode parent, Attrs.Key<List<SQLNode>> key, List<SQLNode> child) {
    final SimpleQueryScope.Clause clause = CLAUSE_KEYS.get(key);
    if (clause != null) clauses.push(clause);
    return true;
  }

  @Override
  public void leaveChild(SQLNode parent, Attrs.Key<SQLNode> key, SQLNode child) {
    final SimpleQueryScope.Clause clause = CLAUSE_KEYS.get(key);
    if (clause != null) {
      if (clauses.peek() != clause) throw new ConcurrentModificationException();
      clauses.pop();
    }
  }

  @Override
  public void leaveChildren(SQLNode parent, Attrs.Key<List<SQLNode>> key, List<SQLNode> child) {
    final SimpleQueryScope.Clause clause = CLAUSE_KEYS.get(key);
    if (clause != null) {
      if (clauses.peek() != clause) throw new ConcurrentModificationException();
      clauses.pop();
    }
  }
}
