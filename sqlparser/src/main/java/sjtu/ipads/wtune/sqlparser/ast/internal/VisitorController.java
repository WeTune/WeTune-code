package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_SOURCE;

public interface VisitorController {
  static boolean enter(ASTNode n, ASTVistor v) {
    if (n == null) return false;
    if (!v.enter(n)) return false;

    return switch (n.nodeType()) {
      case EXPR -> enterExpr(n, v);
      case TABLE_SOURCE -> enterTableSource(n, v);
      case TABLE_NAME -> v.enterTableName(n);
      case COLUMN_NAME -> v.enterColumnName(n);
      case CREATE_TABLE -> v.enterCreateTable(n);
      case COLUMN_DEF -> v.enterColumnDef(n);
      case REFERENCES -> v.enterReferences(n);
      case INDEX_DEF -> v.enterIndexDef(n);
      case KEY_PART -> v.enterKeyPart(n);
      case WINDOW_SPEC -> v.enterWindowSpec(n);
      case WINDOW_FRAME -> v.enterWindowFrame(n);
      case FRAME_BOUND -> v.enterFrameBound(n);
      case GROUP_ITEM -> v.enterGroupItem(n);
      case ORDER_ITEM -> v.enterOrderItem(n);
      case SELECT_ITEM -> v.enterSelectItem(n);
      case INDEX_HINT -> v.enterIndexHint(n);
      case QUERY_SPEC -> v.enterQuerySpec(n);
      case QUERY -> v.enterQuery(n);
      case SET_OP -> v.enterSetOp(n);
      case NAME_2 -> v.enterName2(n);
      case NAME_3 -> v.enterCommonName(n);
      default -> false;
    };

  }

  static void visitChildren(ASTNode n, ASTVistor v) {
    switch (n.nodeType()) {
      case EXPR -> visitExprChildren(n, v);
      case TABLE_SOURCE -> visitTableSourceChildren(n, v);
      case CREATE_TABLE -> {
        safeVisitChild(CREATE_TABLE_NAME, n, v);
        safeVisitList(CREATE_TABLE_COLUMNS, n, v);
        safeVisitList(CREATE_TABLE_CONSTRAINTS, n, v);
      }
      case COLUMN_DEF -> {
        safeVisitChild(COLUMN_DEF_NAME, n, v);
        safeVisitChild(COLUMN_DEF_REF, n, v);
      }
      case REFERENCES -> {
        safeVisitChild(REFERENCES_TABLE, n, v);
        safeVisitList(REFERENCES_COLUMNS, n, v);
      }
      case INDEX_DEF -> {
        safeVisitList(INDEX_DEF_KEYS, n, v);
        safeVisitList(INDEX_DEF_KEYS, n, v);
      }
      case WINDOW_SPEC -> {
        safeVisitList(WINDOW_SPEC_PARTITION, n, v);
        safeVisitList(WINDOW_SPEC_ORDER, n, v);
        safeVisitChild(WINDOW_SPEC_FRAME, n, v);
      }
      case WINDOW_FRAME -> {
        safeVisitChild(WINDOW_FRAME_START, n, v);
        safeVisitChild(WINDOW_FRAME_END, n, v);
      }
      case FRAME_BOUND -> safeVisitChild(FRAME_BOUND_EXPR, n, v);
      case ORDER_ITEM -> safeVisitChild(ORDER_ITEM_EXPR, n, v);
      case GROUP_ITEM -> safeVisitChild(GROUP_ITEM_EXPR, n, v);
      case SELECT_ITEM -> safeVisitChild(SELECT_ITEM_EXPR, n, v);
      case QUERY_SPEC -> {
        safeVisitList(QUERY_SPEC_DISTINCT_ON, n, v);
        safeVisitList(QUERY_SPEC_SELECT_ITEMS, n, v);
        safeVisitChild(QUERY_SPEC_FROM, n, v);
        safeVisitChild(QUERY_SPEC_WHERE, n, v);
        safeVisitList(QUERY_SPEC_GROUP_BY, n, v);
        safeVisitChild(QUERY_SPEC_HAVING, n, v);
        safeVisitList(QUERY_SPEC_WINDOWS, n, v);
      }
      case QUERY -> {
        safeVisitChild(QUERY_BODY, n, v);
        safeVisitList(QUERY_ORDER_BY, n, v);
        safeVisitChild(QUERY_OFFSET, n, v);
        safeVisitChild(QUERY_LIMIT, n, v);
      }
      case SET_OP -> {
        safeVisitChild(SET_OP_LEFT, n, v);
        safeVisitChild(SET_OP_RIGHT, n, v);
      }
    }
  }

  static void leave(ASTNode n, ASTVistor v) {
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

  private static void safeAccept(ASTNode n, ASTVistor v) {
    if (n != null) n.accept(v);
  }

  private static void safeVisitChild(FieldKey<ASTNode> key, ASTNode n, ASTVistor v) {
    final ASTNode child = n.get(key);
    if (v.enterChild(n, key, child)) safeAccept(child, v);
    v.leaveChild(n, key, child);
  }

  private static void safeVisitList(FieldKey<List<ASTNode>> key, ASTNode n, ASTVistor v) {
    final List<ASTNode> children = n.get(key);
    if (v.enterChildren(n, key, children))
      if (children != null) for (ASTNode child : children) safeAccept(child, v);
    v.leaveChildren(n, key, children);
  }

  private static boolean enterExpr(ASTNode n, ASTVistor v) {
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

  private static boolean enterTableSource(ASTNode n, ASTVistor v) {
    assert n.nodeType() == TABLE_SOURCE;

    return switch (n.get(TABLE_SOURCE_KIND)) {
      case SIMPLE_SOURCE -> v.enterSimpleTableSource(n);
      case JOINED -> v.enterJoinedTableSource(n);
      case DERIVED_SOURCE -> v.enterDerivedTableSource(n);
    };

  }

  private static void visitExprChildren(ASTNode n, ASTVistor v) {
    assert n.nodeType() == EXPR;
    switch (n.get(EXPR_KIND)) {
      case VARIABLE -> safeVisitChild(VARIABLE_ASSIGNMENT, n, v);
      case COLUMN_REF -> safeVisitChild(COLUMN_REF_COLUMN, n, v);
      case FUNC_CALL -> safeVisitList(FUNC_CALL_ARGS, n, v);
      case COLLATE -> safeVisitChild(COLLATE_EXPR, n, v);
      case UNARY -> safeVisitChild(UNARY_EXPR, n, v);
      case GROUPING_OP -> safeVisitList(GROUPING_OP_EXPRS, n, v);
      case TUPLE -> safeVisitList(TUPLE_EXPRS, n, v);
      case MATCH -> {
        safeVisitList(MATCH_COLS, n, v);
        safeVisitChild(MATCH_EXPR, n, v);
      }
      case CAST -> safeVisitChild(CAST_EXPR, n, v);
      case DEFAULT -> safeVisitChild(DEFAULT_COL, n, v);
      case VALUES -> safeVisitChild(VALUES_EXPR, n, v);
      case INTERVAL -> safeVisitChild(INTERVAL_EXPR, n, v);
      case EXISTS -> safeVisitChild(EXISTS_SUBQUERY_EXPR, n, v);
      case QUERY_EXPR -> safeVisitChild(QUERY_EXPR_QUERY, n, v);
      case AGGREGATE -> {
        safeVisitList(AGGREGATE_ARGS, n, v);
        safeVisitList(AGGREGATE_ORDER, n, v);
      }
      case CONVERT_USING -> safeVisitChild(CONVERT_USING_EXPR, n, v);
      case CASE -> {
        safeVisitChild(CASE_COND, n, v);
        safeVisitList(CASE_WHENS, n, v);
        safeVisitChild(CASE_ELSE, n, v);
      }
      case WHEN -> {
        safeVisitChild(WHEN_COND, n, v);
        safeVisitChild(WHEN_EXPR, n, v);
      }
      case BINARY -> {
        safeVisitChild(BINARY_LEFT, n, v);
        safeVisitChild(BINARY_RIGHT, n, v);
      }
      case TERNARY -> {
        safeVisitChild(TERNARY_LEFT, n, v);
        safeVisitChild(TERNARY_MIDDLE, n, v);
        safeVisitChild(TERNARY_RIGHT, n, v);
      }
      case WILDCARD -> safeVisitChild(WILDCARD_TABLE, n, v);
      case INDIRECTION -> {
        safeVisitChild(INDIRECTION_EXPR, n, v);
        safeVisitList(INDIRECTION_COMPS, n, v);
      }
      case INDIRECTION_COMP -> {
        safeVisitChild(INDIRECTION_COMP_START, n, v);
        safeVisitChild(INDIRECTION_COMP_END, n, v);
      }
      case ARRAY -> safeVisitList(ARRAY_ELEMENTS, n, v);
    }
  }

  private static void visitTableSourceChildren(ASTNode n, ASTVistor v) {
    assert n.nodeType() == TABLE_SOURCE;
    switch (n.get(TABLE_SOURCE_KIND)) {
      case SIMPLE_SOURCE -> {
        safeVisitChild(SIMPLE_TABLE, n, v);
        safeVisitList(SIMPLE_HINTS, n, v);
      }
      case JOINED -> {
        safeVisitChild(JOINED_LEFT, n, v);
        safeVisitChild(JOINED_RIGHT, n, v);
        safeVisitChild(JOINED_ON, n, v);
      }
      case DERIVED_SOURCE -> safeVisitChild(DERIVED_SUBQUERY, n, v);
    }
  }

  private static void leaveExpr(ASTNode n, ASTVistor v) {
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

  private static void leaveTableSource(ASTNode n, ASTVistor v) {
    assert n.nodeType() == TABLE_SOURCE;

    switch (n.get(TABLE_SOURCE_KIND)) {
      case SIMPLE_SOURCE -> v.leaveSimpleTableSource(n);
      case JOINED -> v.leaveJoinedTableSource(n);
      case DERIVED_SOURCE -> v.leaveDerivedTableSource(n);
    }
  }
}
