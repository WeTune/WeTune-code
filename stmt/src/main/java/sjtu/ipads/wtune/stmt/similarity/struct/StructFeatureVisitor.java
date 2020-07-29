package sjtu.ipads.wtune.stmt.similarity.struct;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;

import static sjtu.ipads.wtune.common.utils.Commons.isEmpty;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;

public class StructFeatureVisitor implements SQLVisitor {
  private final SQLNode rootQuery;
  private final StructFeature feature;
  private boolean flip = false;

  public StructFeatureVisitor(SQLNode rootQuery) {
    this(rootQuery, new StructFeature());
  }

  public StructFeatureVisitor(SQLNode rootQuery, StructFeature feature) {
    this.rootQuery = rootQuery;
    this.feature = feature;
  }

  @Override
  public boolean enterUnary(SQLNode unary) {
    if (unary.get(UNARY_OP) == SQLExpr.UnaryOp.NOT) flip = !flip;
    return true;
  }

  @Override
  public void leaveUnary(SQLNode unary) {
    if (unary.get(UNARY_OP) == SQLExpr.UnaryOp.NOT) flip = !flip;
  }

  @Override
  public boolean enterBinary(SQLNode binary) {
    final SQLExpr.BinaryOp op = binary.get(BINARY_OP);
    if (!op.isRelation()) return true;

    switch (op) {
      case EQUAL:
      case IS:
      case NULL_SAFE_EQUAL:
        feature.increaseOperator(flip ? OpCategory.NOT_EQUAL : OpCategory.EQUAL);
        break;

      case IS_DISTINCT_FROM:
      case NOT_EQUAL:
        feature.increaseOperator(flip ? OpCategory.EQUAL : OpCategory.NOT_EQUAL);
        break;

      case GREATER_OR_EQUAL:
      case GREATER_THAN:
      case LESS_OR_EQUAL:
      case LESS_THAN:
        feature.increaseOperator(OpCategory.NUMERIC_COMPARE);
        break;

      case IN_LIST:
      case ARRAY_CONTAINED_BY:
      case ARRAY_CONTAINS:
      case MEMBER_OF:
        feature.increaseOperator(OpCategory.CONTAINS_BY);
        break;

      case LIKE:
      case ILIKE:
      case SIMILAR_TO:
      case REGEXP:
      case REGEXP_PG:
      case REGEXP_I_PG:
      case SOUNDS_LIKE:
        feature.increaseOperator(OpCategory.STRING_MATCH);
        break;

      case IN_SUBQUERY:
        feature.increaseOperator(OpCategory.IN_SUBQUERY);
        break;
    }

    return true;
  }

  @Override
  public boolean enterTernary(SQLNode ternary) {
    if (ternary.get(TERNARY_OP) == TernaryOp.BETWEEN_AND) {
      feature.increaseOperator(OpCategory.NUMERIC_COMPARE);
      feature.increaseOperator(OpCategory.NUMERIC_COMPARE);
    }
    return true;
  }

  @Override
  public boolean enterExists(SQLNode exists) {
    feature.increaseOperator(OpCategory.EXISTS);
    return true;
  }

  @Override
  public boolean enterQuery(SQLNode query) {
    if (query != rootQuery) {
      final StructFeatureVisitor subVisitor = new StructFeatureVisitor(query);
      query.accept(subVisitor);
      feature.subqueries.add(subVisitor.feature);
      return false;
    }
    if (!isEmpty(query.get(QUERY_ORDER_BY))) feature.hasOrderBy = true;
    if (query.get(QUERY_OFFSET) != null) feature.hasOffset = true;
    if (query.get(QUERY_LIMIT) != null) feature.hasLimit = true;
    return true;
  }

  @Override
  public boolean enterQuerySpec(SQLNode querySpec) {
    if (querySpec.isFlagged(QUERY_SPEC_DISTINCT)
        || !isEmpty(querySpec.get(QUERY_SPEC_DISTINCT_ON))) feature.hasDistinct = true;
    if (!isEmpty(querySpec.get(QUERY_SPEC_GROUP_BY))) feature.hasGroupBy = true;

    return true;
  }

  @Override
  public boolean enterAggregate(SQLNode aggregate) {
    ++feature.numAgg;
    return true;
  }

  @Override
  public boolean enter(SQLNode node) {
    if (node.type() == Type.TABLE_SOURCE) ++feature.numTables;
    return true;
  }

  public StructFeature feature() {
    return feature;
  }
}
