package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.sqlparser.ast.ExprAttr;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLVisitor;
import sjtu.ipads.wtune.sqlparser.ast.TableSourceAttr;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.NodeAttr.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_SOURCE;

public interface VisitorController {
  static boolean enter(SQLNode n, SQLVisitor v) {
    if (n == null) return false;
    if (!v.enter(n)) return false;

    switch (n.nodeType()) {
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

      case GROUP_ITEM:
        return v.enterGroupItem(n);

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

      case NAME_2:
        return v.enterName2(n);

      case NAME_3:
        return v.enterCommonName(n);
    }

    return false;
  }

  static void visitChildren(SQLNode n, SQLVisitor v) {
    switch (n.nodeType()) {
      case EXPR:
        visitExprChildren(n, v);
        break;
      case TABLE_SOURCE:
        visitTableSourceChildren(n, v);
        break;
      case CREATE_TABLE:
        safeVisitChild(CREATE_TABLE_NAME, n, v);
        safeVisitList(CREATE_TABLE_COLUMNS, n, v);
        safeVisitList(CREATE_TABLE_CONSTRAINTS, n, v);
        break;
      case COLUMN_DEF:
        safeVisitChild(COLUMN_DEF_NAME, n, v);
        safeVisitChild(COLUMN_DEF_REF, n, v);
        break;
      case REFERENCES:
        safeVisitChild(REFERENCES_TABLE, n, v);
        safeVisitList(REFERENCES_COLUMNS, n, v);
        break;
      case INDEX_DEF:
        safeVisitList(INDEX_DEF_KEYS, n, v);
        safeVisitList(INDEX_DEF_KEYS, n, v);
        break;
      case WINDOW_SPEC:
        safeVisitList(WINDOW_SPEC_PARTITION, n, v);
        safeVisitList(WINDOW_SPEC_ORDER, n, v);
        safeVisitChild(WINDOW_SPEC_FRAME, n, v);
        break;
      case WINDOW_FRAME:
        safeVisitChild(WINDOW_FRAME_START, n, v);
        safeVisitChild(WINDOW_FRAME_END, n, v);
        break;
      case FRAME_BOUND:
        safeVisitChild(FRAME_BOUND_EXPR, n, v);
        break;
      case ORDER_ITEM:
        safeVisitChild(ORDER_ITEM_EXPR, n, v);
        break;
      case GROUP_ITEM:
        safeVisitChild(GROUP_ITEM_EXPR, n, v);
        break;
      case SELECT_ITEM:
        safeVisitChild(SELECT_ITEM_EXPR, n, v);
        break;
      case QUERY_SPEC:
        safeVisitList(QUERY_SPEC_SELECT_ITEMS, n, v);
        safeVisitChild(QUERY_SPEC_FROM, n, v);
        safeVisitChild(QUERY_SPEC_WHERE, n, v);
        safeVisitList(QUERY_SPEC_GROUP_BY, n, v);
        safeVisitChild(QUERY_SPEC_HAVING, n, v);
        safeVisitList(QUERY_SPEC_WINDOWS, n, v);
        break;
      case QUERY:
        safeVisitChild(QUERY_BODY, n, v);
        safeVisitList(QUERY_ORDER_BY, n, v);
        safeVisitChild(QUERY_OFFSET, n, v);
        safeVisitChild(QUERY_LIMIT, n, v);
        break;
      case SET_OP:
        safeVisitChild(SET_OP_LEFT, n, v);
        safeVisitChild(SET_OP_RIGHT, n, v);
    }
  }

  static void leave(SQLNode n, SQLVisitor v) {
    if (n == null) return;
    switch (n.nodeType()) {
      case INVALID:
        break;

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

      case NAME_2:
        v.leaveName2(n);
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

      case GROUP_ITEM:
        v.leaveGroupItem(n);
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
        break;
    }

    v.leave(n);
  }

  private static void safeAccept(SQLNode n, SQLVisitor v) {
    if (n != null) n.accept(v);
  }

  private static void safeVisitChild(AttrKey<SQLNode> key, SQLNode n, SQLVisitor v) {
    final SQLNode child = n.get(key);
    if (v.enterChild(n, key, child)) safeAccept(child, v);
    v.leaveChild(n, key, child);
  }

  private static void safeVisitList(AttrKey<List<SQLNode>> key, SQLNode n, SQLVisitor v) {
    final List<SQLNode> children = n.get(key);
    if (v.enterChildren(n, key, children))
      if (children != null) for (SQLNode child : children) safeAccept(child, v);
    v.leaveChildren(n, key, children);
  }

  private static boolean enterExpr(SQLNode n, SQLVisitor v) {
    assert n.nodeType() == EXPR;

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

  private static boolean enterTableSource(SQLNode n, SQLVisitor v) {
    assert n.nodeType() == TABLE_SOURCE;

    switch (n.get(TABLE_SOURCE_KIND)) {
      case SIMPLE_SOURCE:
        return v.enterSimpleTableSource(n);
      case JOINED:
        return v.enterJoinedTableSource(n);
      case DERIVED_SOURCE:
        return v.enterDerivedTableSource(n);
    }

    return false;
  }

  private static void visitExprChildren(SQLNode n, SQLVisitor v) {
    assert n.nodeType() == EXPR;
    switch (n.get(EXPR_KIND)) {
      case VARIABLE:
        safeVisitChild(ExprAttr.VARIABLE_ASSIGNMENT, n, v);
        break;
      case COLUMN_REF:
        safeVisitChild(ExprAttr.COLUMN_REF_COLUMN, n, v);
        break;
      case FUNC_CALL:
        safeVisitList(ExprAttr.FUNC_CALL_ARGS, n, v);
        break;
      case COLLATE:
        safeVisitChild(ExprAttr.COLLATE_EXPR, n, v);
        break;
      case UNARY:
        safeVisitChild(ExprAttr.UNARY_EXPR, n, v);
        break;
      case GROUPING_OP:
        safeVisitList(ExprAttr.GROUPING_OP_EXPRS, n, v);
        break;
      case TUPLE:
        safeVisitList(ExprAttr.TUPLE_EXPRS, n, v);
        break;
      case MATCH:
        safeVisitList(ExprAttr.MATCH_COLS, n, v);
        safeVisitChild(ExprAttr.MATCH_EXPR, n, v);
        break;
      case CAST:
        safeVisitChild(ExprAttr.CAST_EXPR, n, v);
        break;
      case DEFAULT:
        safeVisitChild(ExprAttr.DEFAULT_COL, n, v);
        break;
      case VALUES:
        safeVisitChild(ExprAttr.VALUES_EXPR, n, v);
        break;
      case INTERVAL:
        safeVisitChild(ExprAttr.INTERVAL_EXPR, n, v);
        break;
      case EXISTS:
        safeVisitChild(ExprAttr.EXISTS_SUBQUERY_EXPR, n, v);
        break;
      case QUERY_EXPR:
        safeVisitChild(ExprAttr.QUERY_EXPR_QUERY, n, v);
        break;
      case AGGREGATE:
        safeVisitList(ExprAttr.AGGREGATE_ARGS, n, v);
        safeVisitList(ExprAttr.AGGREGATE_ORDER, n, v);
        break;
      case CONVERT_USING:
        safeVisitChild(ExprAttr.CONVERT_USING_EXPR, n, v);
        break;
      case CASE:
        safeVisitChild(ExprAttr.CASE_COND, n, v);
        safeVisitList(ExprAttr.CASE_WHENS, n, v);
        safeVisitChild(ExprAttr.CASE_ELSE, n, v);
        break;
      case WHEN:
        safeVisitChild(ExprAttr.WHEN_COND, n, v);
        safeVisitChild(ExprAttr.WHEN_EXPR, n, v);
        break;
      case BINARY:
        safeVisitChild(ExprAttr.BINARY_LEFT, n, v);
        safeVisitChild(ExprAttr.BINARY_RIGHT, n, v);
        break;
      case TERNARY:
        safeVisitChild(ExprAttr.TERNARY_LEFT, n, v);
        safeVisitChild(ExprAttr.TERNARY_MIDDLE, n, v);
        safeVisitChild(ExprAttr.TERNARY_RIGHT, n, v);
        break;
      case WILDCARD:
        safeVisitChild(ExprAttr.WILDCARD_TABLE, n, v);
        break;
      case INDIRECTION:
        safeVisitChild(ExprAttr.INDIRECTION_EXPR, n, v);
        safeVisitList(ExprAttr.INDIRECTION_COMPS, n, v);
        break;
      case INDIRECTION_COMP:
        safeVisitChild(ExprAttr.INDIRECTION_COMP_START, n, v);
        safeVisitChild(ExprAttr.INDIRECTION_COMP_END, n, v);
        break;
      case ARRAY:
        safeVisitList(ExprAttr.ARRAY_ELEMENTS, n, v);
    }
  }

  private static void visitTableSourceChildren(SQLNode n, SQLVisitor v) {
    assert n.nodeType() == TABLE_SOURCE;
    switch (n.get(TABLE_SOURCE_KIND)) {
      case SIMPLE_SOURCE:
        safeVisitChild(TableSourceAttr.SIMPLE_TABLE, n, v);
        safeVisitList(TableSourceAttr.SIMPLE_HINTS, n, v);
        break;
      case JOINED:
        safeVisitChild(TableSourceAttr.JOINED_LEFT, n, v);
        safeVisitChild(TableSourceAttr.JOINED_RIGHT, n, v);
        safeVisitChild(TableSourceAttr.JOINED_ON, n, v);
        break;
      case DERIVED_SOURCE:
        safeVisitChild(TableSourceAttr.DERIVED_SUBQUERY, n, v);
    }
  }

  private static void leaveExpr(SQLNode n, SQLVisitor v) {
    assert n.nodeType() == EXPR;

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

  private static void leaveTableSource(SQLNode n, SQLVisitor v) {
    assert n.nodeType() == TABLE_SOURCE;

    switch (n.get(TABLE_SOURCE_KIND)) {
      case SIMPLE_SOURCE:
        v.leaveSimpleTableSource(n);
        return;
      case JOINED:
        v.leaveJoinedTableSource(n);
        return;
      case DERIVED_SOURCE:
        v.leaveDerivedTableSource(n);
    }
  }
}
