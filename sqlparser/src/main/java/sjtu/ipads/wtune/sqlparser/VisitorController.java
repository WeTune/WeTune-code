package sjtu.ipads.wtune.sqlparser;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.Type.EXPR;
import static sjtu.ipads.wtune.sqlparser.SQLNode.Type.TABLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.*;

abstract class VisitorController {
  private static void safeAccept(SQLNode n, SQLVisitor v) {
    if (n != null) n.accept(v);
  }

  private static boolean safeVisitChild(Key<SQLNode> key, SQLNode n, SQLVisitor v) {
    final SQLNode child = n.get(key);
    final boolean isMutator = v.isMutator();
    final boolean visitChild = v.enterChild(n, key, child);

    if (isMutator && n.structChanged()) return false; // require re-visit
    if (!visitChild) return true;

    // if the child has been replaced, re-enter
    final SQLNode newChild = n.get(key);
    if (newChild != child) return safeVisitChild(key, n, v);

    // otherwise, we don't care child's structChanged since we haven't visited it yet
    safeAccept(child, v);
    if (isMutator && n.structChanged()) return false;

    v.leaveChild(n, key, child);
    return !isMutator || !n.structChanged();
  }

  private static boolean safeVisitList(Key<List<SQLNode>> key, SQLNode n, SQLVisitor v) {
    final List<SQLNode> children = n.get(key);
    final boolean isMutator = v.isMutator();
    final boolean visitChildren = v.enterChildren(n, key, children);

    if (isMutator && n.structChanged()) return false;
    if (!visitChildren) return true;

    final List<SQLNode> newChildren = n.get(key);
    if (newChildren != children) return safeVisitList(key, n, v);

    if (children != null)
      for (SQLNode child : children) {
        safeAccept(child, v);
        if (isMutator && n.structChanged()) return false;
      }

    v.leaveChildren(n, key, children);
    return !isMutator || !n.structChanged();
  }

  static boolean enter(SQLNode n, SQLVisitor v) {
    if (n == null) return false;
    if (!v.enter(n)) return false;
    if (n.type() == null) System.out.println();

    switch (n.type()) {
      case INVALID:
        return false;

      case EXPR:
        return enterExpr(n, v);

      case TABLE_SOURCE:
        return enterTableSource(n, v);

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

      case WINDOW_SPEC:
        return v.enterWindowSpec(n);

      case WINDOW_FRAME:
        return v.enterWindowFrame(n);

      case FRAME_BOUND:
        return v.enterFrameBound(n);

      case ORDER_ITEM:
        return v.enterOrderItem(n);

      case SELECT_ITEM:
        return v.enterSelectItem(n);

      case INDEX_HINT:
        return v.enterIndexHint(n);

      case QUERY_SPEC:
        return v.enterQuerySpec(n);

      case QUERY:
        return v.enterQuery(n);

      case SET_OP:
        return v.enterSetOp(n);

      case NAME_3:
        return v.enterCommonName(n);
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
      case QUERY_EXPR:
        return v.enterQueryExpr(n);
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
      case INDIRECTION:
        return v.enterIndirection(n);
      case INDIRECTION_COMP:
        return v.enterIndirectionComp(n);
      case ARRAY:
        return v.enterArray(n);

      case UNKNOWN:
    }

    return false;
  }

  static boolean enterTableSource(SQLNode n, SQLVisitor v) {
    assert n.type() == TABLE_SOURCE;

    switch (n.get(TABLE_SOURCE_KIND)) {
      case SIMPLE:
        return v.enterSimpleTableSource(n);
      case JOINED:
        return v.enterJoinedTableSource(n);
      case DERIVED:
        return v.enterDerivedTableSource(n);
    }

    return false;
  }

  static boolean visitChildren(SQLNode n, SQLVisitor v) {
    switch (n.type()) {
      case EXPR:
        return visitExprChildren(n, v);
      case TABLE_SOURCE:
        return visitTableSourceChildren(n, v);
      case CREATE_TABLE:
        return safeVisitChild(CREATE_TABLE_NAME, n, v)
            && safeVisitList(CREATE_TABLE_COLUMNS, n, v)
            && safeVisitList(CREATE_TABLE_CONSTRAINTS, n, v);
      case COLUMN_DEF:
        return safeVisitChild(COLUMN_DEF_NAME, n, v) && safeVisitChild(COLUMN_DEF_REF, n, v);
      case REFERENCES:
        return safeVisitChild(REFERENCES_TABLE, n, v) && safeVisitList(REFERENCES_COLUMNS, n, v);
      case INDEX_DEF:
        return safeVisitList(INDEX_DEF_KEYS, n, v) && safeVisitList(INDEX_DEF_KEYS, n, v);
      case WINDOW_SPEC:
        return safeVisitList(WINDOW_SPEC_PARTITION, n, v)
            && safeVisitList(WINDOW_SPEC_ORDER, n, v)
            && safeVisitChild(WINDOW_SPEC_FRAME, n, v);
      case WINDOW_FRAME:
        return safeVisitChild(WINDOW_FRAME_START, n, v) && safeVisitChild(WINDOW_FRAME_END, n, v);
      case FRAME_BOUND:
        return safeVisitChild(FRAME_BOUND_EXPR, n, v);
      case ORDER_ITEM:
        return safeVisitChild(ORDER_ITEM_EXPR, n, v);
      case SELECT_ITEM:
        return safeVisitChild(SELECT_ITEM_EXPR, n, v);
      case QUERY_SPEC:
        return safeVisitList(QUERY_SPEC_SELECT_ITEMS, n, v)
            && safeVisitChild(QUERY_SPEC_FROM, n, v)
            && safeVisitChild(QUERY_SPEC_WHERE, n, v)
            && safeVisitList(QUERY_SPEC_GROUP_BY, n, v)
            && safeVisitChild(QUERY_SPEC_HAVING, n, v)
            && safeVisitList(QUERY_SPEC_WINDOWS, n, v);
      case QUERY:
        return safeVisitChild(QUERY_BODY, n, v)
            && safeVisitList(QUERY_ORDER_BY, n, v)
            && safeVisitChild(QUERY_OFFSET, n, v)
            && safeVisitChild(QUERY_LIMIT, n, v);
      case SET_OP:
        return safeVisitChild(SET_OP_LEFT, n, v) && safeVisitChild(SET_OP_RIGHT, n, v);
      case INDEX_HINT:
      case KEY_PART:
      case COLUMN_NAME:
      case TABLE_NAME:
      case NAME_3:
      default:
        return true;
    }
  }

  static boolean visitExprChildren(SQLNode n, SQLVisitor v) {
    assert n.type() == EXPR;
    switch (n.get(EXPR_KIND)) {
      case VARIABLE:
        return safeVisitChild(VARIABLE_ASSIGNMENT, n, v);
      case COLUMN_REF:
        return safeVisitChild(COLUMN_REF_COLUMN, n, v);
      case FUNC_CALL:
        return safeVisitList(FUNC_CALL_ARGS, n, v);
      case COLLATE:
        return safeVisitChild(COLLATE_EXPR, n, v);
      case UNARY:
        return safeVisitChild(UNARY_EXPR, n, v);
      case GROUPING_OP:
        return safeVisitList(GROUPING_OP_EXPRS, n, v);
      case TUPLE:
        return safeVisitList(TUPLE_EXPRS, n, v);
      case MATCH:
        return safeVisitList(MATCH_COLS, n, v) && safeVisitChild(MATCH_EXPR, n, v);
      case CAST:
        return safeVisitChild(CAST_EXPR, n, v);
      case DEFAULT:
        return safeVisitChild(DEFAULT_COL, n, v);
      case VALUES:
        return safeVisitChild(VALUES_EXPR, n, v);
      case INTERVAL:
        return safeVisitChild(INTERVAL_EXPR, n, v);
      case EXISTS:
        return safeVisitChild(EXISTS_SUBQUERY, n, v);
      case QUERY_EXPR:
        return safeVisitChild(QUERY_EXPR_QUERY, n, v);
      case AGGREGATE:
        return safeVisitList(AGGREGATE_ARGS, n, v) && safeVisitList(AGGREGATE_ORDER, n, v);
      case CONVERT_USING:
        return safeVisitChild(CONVERT_USING_EXPR, n, v);
      case CASE:
        return safeVisitChild(CASE_COND, n, v)
            && safeVisitList(CASE_WHENS, n, v)
            && safeVisitChild(CASE_ELSE, n, v);
      case WHEN:
        return safeVisitChild(WHEN_COND, n, v) && safeVisitChild(WHEN_EXPR, n, v);
      case BINARY:
        return safeVisitChild(BINARY_LEFT, n, v) && safeVisitChild(BINARY_RIGHT, n, v);
      case TERNARY:
        return safeVisitChild(TERNARY_LEFT, n, v)
            && safeVisitChild(TERNARY_MIDDLE, n, v)
            && safeVisitChild(TERNARY_RIGHT, n, v);
      case WILDCARD:
        return safeVisitChild(WILDCARD_TABLE, n, v);
      case INDIRECTION:
        return safeVisitChild(INDIRECTION_EXPR, n, v) && safeVisitList(INDIRECTION_COMPS, n, v);
      case INDIRECTION_COMP:
        return safeVisitChild(INDIRECTION_COMP_START, n, v)
            && safeVisitChild(INDIRECTION_COMP_END, n, v);
      case ARRAY:
        return safeVisitList(ARRAY_ELEMENTS, n, v);

      case UNKNOWN:
      case SYMBOL:
      case PARAM_MARKER:
      case LITERAL:
      default:
        return true;
    }
  }

  static boolean visitTableSourceChildren(SQLNode n, SQLVisitor v) {
    assert n.type() == TABLE_SOURCE;
    switch (n.get(TABLE_SOURCE_KIND)) {
      case SIMPLE:
        return safeVisitChild(SIMPLE_TABLE, n, v) && safeVisitList(SIMPLE_HINTS, n, v);
      case JOINED:
        return safeVisitChild(JOINED_LEFT, n, v)
            && safeVisitChild(JOINED_RIGHT, n, v)
            && safeVisitChild(JOINED_ON, n, v);
      case DERIVED:
        return safeVisitChild(DERIVED_SUBQUERY, n, v);
      default:
        return true;
    }
  }

  static void leave(SQLNode n, SQLVisitor v) {
    if (n == null) return;
    v.leave(n);

    switch (n.type()) {
      case INVALID:
        return;

      case EXPR:
        leaveExpr(n, v);
        break;

      case TABLE_SOURCE:
        leaveTableSource(n, v);

      case TABLE_NAME:
        v.leaveTableName(n);
        break;

      case COLUMN_NAME:
        v.leaveColumnName(n);
        break;

      case NAME_3:
        v.leaveCommonName(n);
        return;

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
        v.leaveSelectItem(n);
        break;

      case INDEX_HINT:
        v.leaveIndexHint(n);
        break;

      case QUERY_SPEC:
        v.leaveQuerySpec(n);
        break;

      case QUERY:
        v.leaveQuery(n);
        break;

      case SET_OP:
        v.leaveUnion(n);
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

      case QUERY_EXPR:
        v.leaveQueryExpr(n);
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

      case INDIRECTION:
        v.leaveIndirection(n);
        return;

      case INDIRECTION_COMP:
        v.leaveIndirectionComp(n);
        return;

      case ARRAY:
        v.leaveArray(n);
        return;

      case UNKNOWN:
    }
  }

  static void leaveTableSource(SQLNode n, SQLVisitor v) {
    assert n.type() == TABLE_SOURCE;

    switch (n.get(TABLE_SOURCE_KIND)) {
      case SIMPLE:
        v.leaveSimpleTableSource(n);
        return;
      case JOINED:
        v.leaveJoinedTableSource(n);
        return;
      case DERIVED:
        v.leaveDerivedTableSource(n);
        return;
    }
  }
}
