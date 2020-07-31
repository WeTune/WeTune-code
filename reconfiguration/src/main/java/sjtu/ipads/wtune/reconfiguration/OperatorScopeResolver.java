package sjtu.ipads.wtune.reconfiguration;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.resolver.Resolver;
import sjtu.ipads.wtune.stmt.similarity.struct.OpCategory;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.isEmpty;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;

public class OperatorScopeResolver implements Resolver, SQLVisitor {
  public static final Attrs.Key<OpCategory> RESOLVED_OPERATOR_SCOPE =
      Attrs.key("reconfig.attr.resolvedOpScope", OpCategory.class);

  private final Deque<OpCategory> currentOp = new LinkedList<>();
  private boolean flip = false;

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
        currentOp.push(flip ? OpCategory.NOT_EQUAL : OpCategory.EQUAL);
        break;

      case IS_DISTINCT_FROM:
      case NOT_EQUAL:
        currentOp.push(flip ? OpCategory.EQUAL : OpCategory.NOT_EQUAL);
        break;

      case GREATER_OR_EQUAL:
      case GREATER_THAN:
      case LESS_OR_EQUAL:
      case LESS_THAN:
        currentOp.push(OpCategory.NUMERIC_COMPARE);
        break;

      case IN_LIST:
      case ARRAY_CONTAINED_BY:
      case ARRAY_CONTAINS:
      case MEMBER_OF:
        currentOp.push(OpCategory.CONTAINS_BY);
        break;

      case LIKE:
      case ILIKE:
      case SIMILAR_TO:
      case REGEXP:
      case REGEXP_PG:
      case REGEXP_I_PG:
      case SOUNDS_LIKE:
        currentOp.push(OpCategory.STRING_MATCH);
        break;

      case IN_SUBQUERY:
        currentOp.push(null);
        break;
    }

    return true;
  }

  @Override
  public void leaveBinary(SQLNode binary) {
    final SQLExpr.BinaryOp op = binary.get(BINARY_OP);
    if (op.isRelation()) currentOp.pop();
  }

  @Override
  public boolean enterTernary(SQLNode ternary) {
    if (ternary.get(TERNARY_OP) == TernaryOp.BETWEEN_AND)
      currentOp.push(OpCategory.NUMERIC_COMPARE);
    return true;
  }

  @Override
  public void leaveTernary(SQLNode ternary) {
    if (ternary.get(TERNARY_OP) == TernaryOp.BETWEEN_AND) currentOp.pop();
  }

  @Override
  public boolean enterExists(SQLNode exists) {
    currentOp.push(null);
    return true;
  }

  @Override
  public void leaveExists(SQLNode exists) {
    currentOp.pop();
  }

  @Override
  public boolean enterChildren(SQLNode parent, Key<List<SQLNode>> key, List<SQLNode> child) {
    if (key == QUERY_SPEC_SELECT_ITEMS) return false;
    if (key == QUERY_ORDER_BY && !isEmpty(child)) currentOp.push(OpCategory.ORDER_BY);
    return true;
  }

  @Override
  public void leaveChildren(SQLNode parent, Key<List<SQLNode>> key, List<SQLNode> child) {
    if (key == QUERY_ORDER_BY && !isEmpty(child)) currentOp.pop();
  }

  @Override
  public boolean enterColumnRef(SQLNode columnRef) {
    final OpCategory peek = currentOp.peek();
    if (peek != null) columnRef.put(RESOLVED_OPERATOR_SCOPE, peek);
    return false;
  }

  @Override
  public boolean resolve(Statement stmt, SQLNode node) {
    node.accept(this);
    return true;
  }
}
