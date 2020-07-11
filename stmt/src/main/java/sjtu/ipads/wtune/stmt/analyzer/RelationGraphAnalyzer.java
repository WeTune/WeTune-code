package sjtu.ipads.wtune.stmt.analyzer;

import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.common.utils.Pair;
import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLNode.Type;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.*;
import sjtu.ipads.wtune.stmt.resolver.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.common.attrs.Attrs.key;
import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.Kind.QUERY_EXPR;
import static sjtu.ipads.wtune.sqlparser.SQLNode.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_BODY;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.*;

/**
 * Relation Graph represents relations and join between them in a query.
 *
 * <p>Each query scope has its local relation graph, in which vertexes are relations, and edges are
 * join conditions.
 *
 * <p>In most case it is acyclic graph, though SQL spec doesn't prevent such case. e.g.
 *
 * <p>SELECT * FROM a JOIN b ON a.i = b.i JOIN c ON b.j = c.j WHERE a.k = c.k
 *
 * <p>We assume such query is not present for now.
 */
public class RelationGraphAnalyzer implements Analyzer<RelationGraph> {
  private static final Attrs.Key<Relation> RELATION_KEY =
      key(ATTR_PREFIX + ".analyzer.relation", Relation.class);
  private static final System.Logger LOG = System.getLogger("[Stmt.Analyzer.RelationGraph]");

  private static class RelationCollector implements SQLVisitor {
    private final Set<Relation> relations = new HashSet<>();

    @Override
    public boolean enterQueryExpr(SQLNode queryExpr) {
      final SQLNode parent = queryExpr.parent();
      if (!isExpr(parent)) return true;

      final Kind paretKind = exprKind(parent);
      if (paretKind != Kind.BINARY) return true;

      final BinaryOp op = parent.get(BINARY_OP);
      if (op.isLogic()) return true;
      if (op != BinaryOp.IN_SUBQUERY && op != BinaryOp.EQUAL) return true;

      final SQLNode otherSide =
          parent.get(BINARY_LEFT) == queryExpr ? parent.get(BINARY_RIGHT) : parent.get(BINARY_LEFT);
      if (otherSide.get(RESOLVED_COLUMN_REF) == null) return true;

      // check path to expr root
      SQLNode ascent = parent.parent();
      while (ascent != null && isExpr(ascent)) {
        final SQLExpr.Kind ascentKind = exprKind(ascent);

        if (ascentKind == SQLExpr.Kind.BINARY) {
          final BinaryOp ascentOp = ascent.get(BINARY_OP);
          if (ascentOp == BinaryOp.XOR_SYMBOL || ascentOp == BinaryOp.OR) return true;
        } else if (ascentKind == SQLExpr.Kind.UNARY)
          if (ascent.get(UNARY_OP) == UnaryOp.NOT) return true;

        ascent = ascent.parent();
      }

      asRelation(queryExpr.get(QUERY_EXPR_QUERY));

      return true;
    }

    @Override
    public boolean enterSimpleTableSource(SQLNode simpleTableSource) {
      asRelation(simpleTableSource);
      return false;
    }

    @Override
    public boolean enterDerivedTableSource(SQLNode derivedTableSource) {
      asRelation(derivedTableSource);
      return true;
    }

    private void asRelation(SQLNode node) {
      final Relation rel = Relation.of(node);
      node.put(RELATION_KEY, rel);
      relations.add(rel);
    }

    private static Set<Relation> collect(QueryScope scope) {
      if (scope == null) return null;

      final SQLNode body = scope.queryNode().get(QUERY_BODY);
      if (body.type() == Type.SET_OP) return singleton(Relation.of(scope.queryNode()));
      assert body.type() == Type.QUERY_SPEC;

      final RelationCollector collector = new RelationCollector();
      scope.queryNode().accept(collector);

      return collector.relations;
    }

    private static Set<Relation> collect(SQLNode node) {
      return collect(node.get(RESOLVED_QUERY_SCOPE));
    }
  }

  private static class JoinConditionCollector implements SQLVisitor {
    private final Set<SQLNode> joinConditions = new HashSet<>();

    @Override
    public boolean enterBinary(SQLNode binary) {
      if (isJoinCondition(binary)) joinConditions.add(binary);
      return true;
    }

    private boolean isJoinCondition(SQLNode binary) {
      assert exprKind(binary) == Kind.BINARY;

      final BoolExpr boolExpr = binary.get(BOOL_EXPR);
      if (boolExpr == null) return false;
      if (boolExpr.isJoinCondtion()) return true;

      final SQLNode rightQuery = binary.get(BINARY_RIGHT).get(QUERY_EXPR_QUERY);
      final SQLNode leftQuery = binary.get(BINARY_LEFT).get(QUERY_EXPR_QUERY);
      return (rightQuery != null && rightQuery.get(RELATION_KEY) != null)
          || (leftQuery != null && leftQuery.get(RELATION_KEY) != null);
    }

    private static Set<SQLNode> collect(QueryScope scope) {
      if (scope == null) return null;

      final JoinConditionCollector collector = new JoinConditionCollector();
      scope.queryNode().accept(collector);
      return collector.joinConditions;
    }

    private static Set<SQLNode> collect(SQLNode node) {
      return collect(node.get(RESOLVED_QUERY_SCOPE));
    }
  }

  static Set<Relation> collectRelation(SQLNode node) {
    return RelationCollector.collect(node);
  }

  static Set<SQLNode> collectJoinCondition(SQLNode node) {
    collectRelation(node); // need mark relations first
    return JoinConditionCollector.collect(node);
  }

  private static RelationGraph buildGraph(SQLNode node) {
    //    final QueryScope rootScope = node.get(RESOLVED_QUERY_SCOPE);
    final Set<Relation> relations = RelationCollector.collect(node);
    final Set<SQLNode> conditions = JoinConditionCollector.collect(node);

    final MutableValueGraph<Relation, JoinCondition> graph =
        ValueGraphBuilder.undirected()
            .expectedNodeCount(relations.size())
            .allowsSelfLoops(false)
            .build();

    relations.forEach(graph::addNode);

    for (SQLNode condNode : conditions) {
      final JoinCondition cond = buildJoinCondition(relations, condNode);
      if (cond == null) {
        //        LOG.log(WARNING, "unresolved join condition: {0}", condNode);
        continue;
      }
      graph.putEdgeValue(cond.left(), cond.right(), cond);
    }

    return RelationGraph.build(graph);
  }

  private static Pair<SQLNode, SQLNode> sidesOf(SQLNode condition) {
    final SQLNode left = condition.get(BINARY_LEFT);
    final SQLNode right = condition.get(BINARY_RIGHT);
    if (exprKind(left) == Kind.COLUMN_REF) return Pair.of(left, right);
    else return Pair.of(right, left);
  }

  private static JoinCondition buildJoinCondition(Set<Relation> relations, SQLNode condition) {
    assert exprKind(condition) == Kind.BINARY;
    final BinaryOp op = condition.get(BINARY_OP);
    assert op == BinaryOp.EQUAL || op == BinaryOp.IN_SUBQUERY;

    final Pair<SQLNode, SQLNode> pair = sidesOf(condition);
    final SQLNode columnSide = pair.left();
    final SQLNode otherSide = pair.right();

    final Relation columnRel = relationOfColumnRef(columnSide);
    final String leftColumn = columnSide.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_COLUMN);
    if (leftColumn == null || !relations.contains(columnRel)) return null;

    final Relation rightRelation;
    final String rightColumn;

    if (exprKind(otherSide) == Kind.COLUMN_REF) {
      rightRelation = relationOfColumnRef(otherSide);
      rightColumn = otherSide.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_COLUMN);

    } else if (exprKind(otherSide) == QUERY_EXPR) {
      final SQLNode subquery = otherSide.get(QUERY_EXPR_QUERY);
      rightRelation = subquery.get(RELATION_KEY);
      rightColumn = singularSelectItemOf(subquery);

    } else return assertFalse();

    if (rightColumn == null || !relations.contains(rightRelation)) return null;

    return JoinCondition.of(condition, columnRel, rightRelation, leftColumn, rightColumn);
  }

  private static Relation relationOfColumnRef(SQLNode cRefNode) {
    final ColumnRef cRef = cRefNode.get(RESOLVED_COLUMN_REF);
    assert cRef != null && cRef.source() != null;
    return cRef.source().node().get(RELATION_KEY);
  }

  private static String singularSelectItemOf(SQLNode queryNode) {
    final List<SelectItem> items = queryNode.get(RESOLVED_QUERY_SCOPE).selectItems();
    if (items == null || items.size() != 1) return null;
    final SelectItem item = items.get(0);
    return item.alias() != null ? item.alias() : item.simpleName();
  }

  @Override
  public RelationGraph analyze(SQLNode node) {
    return buildGraph(node);
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES =
      Set.of(
          QueryScopeResolver.class,
          BoolExprResolver.class,
          ColumnResolver.class,
          JoinConditionResolver.class,
          SelectionResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }
}
