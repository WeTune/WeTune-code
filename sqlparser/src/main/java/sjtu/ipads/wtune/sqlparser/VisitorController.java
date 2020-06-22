package sjtu.ipads.wtune.sqlparser;

import java.util.List;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.Type.EXPR;

abstract class VisitorController {
  private static void safeAccept(SQLNode n, SQLVisitor v) {
    if (n != null) n.accept(v);
  }

  private static void safeVisit(Key<SQLNode> key, SQLNode n, SQLVisitor v) {
    safeAccept(n.get(key), v);
  }

  private static void safeVisitList(Key<List<SQLNode>> key, SQLNode n, SQLVisitor v) {
    n.getOr(key, emptyList()).forEach(it -> safeAccept(it, v));
  }

  static boolean enter(SQLNode n, SQLVisitor v) {
    if (n == null) return false;
    if (!v.enter(n)) return false;

    switch (n.type()) {
      case INVALID:
        return false;

      case TABLE_NAME:
        return v.enterTableName(n);

      case COLUMN_NAME:
        return v.enterColumnName(n);

      case CREATE_TABLE:
        return v.enterCreateTable(n);

      case COLUMN_DEF:
        return v.enterColumnDef(n);

      case REFERENCES:
        return v.enterReferences(n);

      case INDEX_DEF:
        return v.enterIndexDef(n);

      case KEY_PART:
        return v.enterKeyPart(n);

      case EXPR:
        return enterExpr(n, v);

      case WINDOW_SPEC:
        return v.enterWindowSpec(n);

      case WINDOW_FRAME:
        return v.enterWindowFrame(n);

      case FRAME_BOUND:
        return v.enterFrameBound(n);

      case ORDER_ITEM:
        return v.enterOrderItem(n);

      case SELECT_ITEM:
      case QUERY_SPEC:
      case QUERY:
    }

    return false;
  }

  static boolean enterExpr(SQLNode n, SQLVisitor v) {
    assert n.type() == EXPR;

    switch (n.get(EXPR_KIND)) {
      case VARIABLE:
        return v.enterVariable(n);
      case COLUMN_REF:
        return v.enterColumnRef(n);
      case LITERAL:
        return v.enterLiteral(n);
      case FUNC_CALL:
        return v.enterFuncCall(n);
      case COLLATE:
        return v.enterCollation(n);
      case PARAM_MARKER:
        return v.enterParamMarker(n);
      case UNARY:
        return v.enterUnary(n);
      case GROUPING_OP:
        return v.enterGroupingOp(n);
      case TUPLE:
        return v.enterTuple(n);
      case MATCH:
        return v.enterMatch(n);
      case CAST:
        return v.enterCast(n);
      case SYMBOL:
        return v.enterSymbol(n);
      case DEFAULT:
        return v.enterDefault(n);
      case VALUES:
        return v.enterValues(n);
      case INTERVAL:
        return v.enterInterval(n);
      case EXISTS:
        return v.enterExists(n);
      case SUBQUERY:
        return v.enterSubquery(n);
      case WILDCARD:
        return v.enterWildcard(n);
      case AGGREGATE:
        return v.enterAggregate(n);
      case CONVERT_USING:
        return v.enterConvertUsing(n);
      case CASE:
        return v.enterCase(n);
      case WHEN:
        return v.enterWhen(n);
      case BINARY:
        return v.enterBinary(n);
      case TERNARY:
        return v.enterTernary(n);

      case UNKNOWN:
    }

    return false;
  }

  static void visitChildren(SQLNode n, SQLVisitor v) {
    switch (n.type()) {
      case CREATE_TABLE:
        safeVisit(CREATE_TABLE_NAME, n, v);
        safeVisitList(CREATE_TABLE_COLUMNS, n, v);
        safeVisitList(CREATE_TABLE_CONSTRAINTS, n, v);
        break;

      case COLUMN_DEF:
        safeVisit(COLUMN_DEF_NAME, n, v);
        safeVisit(COLUMN_DEF_REF, n, v);
        break;

      case REFERENCES:
        safeVisit(REFERENCES_TABLE, n, v);
        safeVisitList(REFERENCES_COLUMNS, n, v);
        break;

      case INDEX_DEF:
        safeVisitList(INDEX_DEF_KEYS, n, v);
        safeVisitList(INDEX_DEF_KEYS, n, v);
        break;

      case EXPR:
        visitExprChildren(n, v);
        break;

      case WINDOW_SPEC:
        safeVisitList(WINDOW_SPEC_PARTITION, n, v);
        safeVisitList(WINDOW_SPEC_ORDER, n, v);
        safeVisit(WINDOW_SPEC_FRAME, n, v);
        break;

      case WINDOW_FRAME:
        safeVisit(WINDOW_FRAME_START, n, v);
        safeVisit(WINDOW_FRAME_END, n, v);
        break;

      case FRAME_BOUND:
        safeVisit(FRAME_BOUND_EXPR, n, v);
        break;

      case ORDER_ITEM:
        safeVisit(ORDER_ITEM_EXPR, n, v);
        break;

      case SELECT_ITEM:
      case QUERY_SPEC:
      case QUERY:

      case KEY_PART:
      case COLUMN_NAME:
      case TABLE_NAME:
        break;
    }
  }

  static void visitExprChildren(SQLNode n, SQLVisitor v) {
    assert n.type() == EXPR;
    switch (n.get(EXPR_KIND)) {
      case VARIABLE:
        safeVisit(VARIABLE_ASSIGNMENT, n, v);
        return;

      case COLUMN_REF:
        safeVisit(COLUMN_REF_COLUMN, n, v);
        return;

      case FUNC_CALL:
        safeVisitList(FUNC_CALL_ARGS, n, v);
        return;

      case COLLATE:
        safeVisit(COLLATE_EXPR, n, v);
        return;

      case UNARY:
        safeVisit(UNARY_EXPR, n, v);
        return;

      case GROUPING_OP:
        safeVisitList(GROUPING_OP_EXPRS, n, v);
        return;

      case TUPLE:
        safeVisitList(TUPLE_EXPRS, n, v);
        return;

      case MATCH:
        safeVisitList(MATCH_COLS, n, v);
        safeVisit(MATCH_EXPR, n, v);
        return;

      case CAST:
        safeVisit(CAST_EXPR, n, v);
        return;

      case DEFAULT:
        safeVisit(DEFAULT_COL, n, v);
        return;

      case VALUES:
        safeVisit(VALUES_EXPR, n, v);
        return;

      case INTERVAL:
        safeVisit(INTERVAL_EXPR, n, v);
        return;

      case EXISTS:
        safeVisit(EXISTS_SUBQUERY, n, v);
        return;

      case SUBQUERY:
        safeVisit(SUBQUERY_QUERY, n, v);
        return;

      case AGGREGATE:
        safeVisitList(AGGREGATE_ARGS, n, v);
        safeVisitList(AGGREGATE_ORDER, n, v);
        return;

      case CONVERT_USING:
        safeVisit(CONVERT_USING_EXPR, n, v);
        return;

      case CASE:
        safeVisit(CASE_COND, n, v);
        safeVisitList(CASE_WHENS, n, v);
        safeVisit(CASE_ELSE, n, v);
        return;

      case WHEN:
        safeVisit(WHEN_COND, n, v);
        safeVisit(WHEN_EXPR, n, v);
        return;

      case BINARY:
        safeVisit(BINARY_LEFT, n, v);
        safeVisit(BINARY_RIGHT, n, v);
        return;

      case TERNARY:
        safeVisit(TERNARY_LEFT, n, v);
        safeVisit(TERNARY_MIDDLE, n, v);
        safeVisit(TERNARY_RIGHT, n, v);
        return;

      case UNKNOWN:

      case WILDCARD:
      case SYMBOL:
      case PARAM_MARKER:
      case LITERAL:
        return;
    }
  }

  static void leave(SQLNode n, SQLVisitor v) {
    if (n == null) return;
    v.leave(n);

    switch (n.type()) {
      case INVALID:
        return;

      case TABLE_NAME:
        v.leaveTableName(n);
        break;

      case COLUMN_NAME:
        v.leaveColumnName(n);
        break;

      case CREATE_TABLE:
        v.leaveCreateTable(n);
        break;

      case COLUMN_DEF:
        v.leaveColumnDef(n);
        break;

      case REFERENCES:
        v.leaveReferences(n);
        break;

      case INDEX_DEF:
        v.leaveIndexDef(n);
        break;

      case KEY_PART:
        v.leaveKeyPart(n);
        break;

      case EXPR:
        leaveExpr(n, v);
        break;

      case WINDOW_SPEC:
        v.leaveWindowSpec(n);
        break;

      case WINDOW_FRAME:
        v.leaveWindowFrame(n);
        break;

      case FRAME_BOUND:
        v.leaveFrameBound(n);
        break;

      case ORDER_ITEM:
        v.leaveOrderItem(n);
        break;

      case SELECT_ITEM:
      case QUERY_SPEC:
      case QUERY:
    }
  }

  static void leaveExpr(SQLNode n, SQLVisitor v) {
    assert n.type() == EXPR;

    switch (n.get(EXPR_KIND)) {
      case VARIABLE:
        v.leaveColumnDef(n);
        return;

      case COLUMN_REF:
        v.leaveColumnRef(n);
        return;

      case FUNC_CALL:
        v.leaveFuncCall(n);

      case LITERAL:
        v.leaveLiteral(n);
        return;

      case COLLATE:
        v.leaveCollation(n);
        return;

      case PARAM_MARKER:
        v.leaveParamMarker(n);
        return;

      case UNARY:
        v.leaveUnary(n);
        return;

      case GROUPING_OP:
        v.leaveGroupingOp(n);
        return;

      case TUPLE:
        v.leaveTuple(n);
        return;

      case MATCH:
        v.leaveMatch(n);
        return;

      case CAST:
        v.leaveCast(n);
        return;

      case SYMBOL:
        v.leaveSymbol(n);
        return;

      case DEFAULT:
        v.leaveDefault(n);
        return;

      case VALUES:
        v.leaveValues(n);
        return;

      case INTERVAL:
        v.leaveInterval(n);
        return;

      case EXISTS:
        v.leaveExists(n);
        return;

      case SUBQUERY:
        v.leaveSubquery(n);
        return;

      case WILDCARD:
        v.leaveWildcard(n);
        return;

      case AGGREGATE:
        v.leaveAggregate(n);
        return;

      case CONVERT_USING:
        v.leaveConvertUsing(n);
        return;

      case CASE:
        v.leaveCase(n);
        return;

      case WHEN:
        v.leaveWhen(n);
        return;

      case BINARY:
        v.leaveBinary(n);
        return;

      case TERNARY:
        v.leaveTernary(n);
        return;

      case UNKNOWN:
    }
  }
}
