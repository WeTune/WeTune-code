package sjtu.ipads.wtune.learning.support;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.analyzer.Analyzer;
import sjtu.ipads.wtune.stmt.attrs.BoolExpr;
import sjtu.ipads.wtune.stmt.resolver.BoolExprResolver;
import sjtu.ipads.wtune.stmt.resolver.Resolver;

import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.BOOL_EXPR;

public class FeatureExtractor implements SQLVisitor, Analyzer<QueryFeature> {
  private final QueryFeature feature = new QueryFeature();

  @Override
  public boolean enterQuery(SQLNode query) {
    final List<SQLNode> orderBy = query.get(QUERY_ORDER_BY);
    if (orderBy != null && !orderBy.isEmpty()) ++feature.orderBys;

    return true;
  }

  @Override
  public boolean enterQuerySpec(SQLNode querySpec) {
    ++feature.queries;

    final List<SQLNode> groupBy = querySpec.get(QUERY_SPEC_GROUP_BY);
    if (groupBy != null && !groupBy.isEmpty()) ++feature.groupBys;

    final List<SQLNode> distinctOn = querySpec.get(QUERY_SPEC_DISTINCT_ON);
    if (querySpec.isFlagged(QUERY_SPEC_DISTINCT) || (distinctOn != null && !distinctOn.isEmpty()))
      ++feature.distincts;

    return true;
  }

  @Override
  public boolean enterSimpleTableSource(SQLNode simpleTableSource) {
    ++feature.tables;
    return true;
  }

  @Override
  public boolean enter(SQLNode node) {
    final BoolExpr boolExpr = node.get(BOOL_EXPR);
    if (boolExpr != null && boolExpr.isPrimitive()) ++feature.predicates;

    return true;
  }

  @Override
  public QueryFeature analyze(SQLNode node) {
    node.accept(this);
    return feature;
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES = Set.of(BoolExprResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }
}
