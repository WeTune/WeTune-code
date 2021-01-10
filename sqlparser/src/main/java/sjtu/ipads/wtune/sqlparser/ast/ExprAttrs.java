package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.constants.*;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.EXPR_KIND;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;

public interface ExprAttrs {
  String EXPR_ATTR_PREFIX = NodeAttrs.SQL_ATTR_PREFIX + "expr.";

  // Unknown
  Attrs.Key<Object> EXPR_UNKNOWN_RAW = UNKNOWN.attr("raw", Object.class);
  // Variable
  Attrs.Key<VariableScope> VARIABLE_SCOPE = VARIABLE.attr("scope", VariableScope.class);
  Attrs.Key<String> VARIABLE_NAME = VARIABLE.strAttr("name");
  Attrs.Key<SQLNode> VARIABLE_ASSIGNMENT = VARIABLE.nodeAttr("assignment");
  // Col Ref
  Attrs.Key<SQLNode> COLUMN_REF_COLUMN = COLUMN_REF.nodeAttr("column");
  // Func Call
  Attrs.Key<SQLNode> FUNC_CALL_NAME = FUNC_CALL.nodeAttr("name");
  Attrs.Key<List<SQLNode>> FUNC_CALL_ARGS = FUNC_CALL.nodesAttr("args");
  // Collate
  Attrs.Key<SQLNode> COLLATE_EXPR = COLLATE.nodeAttr("expr");
  Attrs.Key<SQLNode> COLLATE_COLLATION = COLLATE.nodeAttr("collation");
  // Interval
  Attrs.Key<SQLNode> INTERVAL_EXPR = INTERVAL.nodeAttr("expr");
  Attrs.Key<IntervalUnit> INTERVAL_UNIT = INTERVAL.attr("unit", IntervalUnit.class);
  // Symbol
  Attrs.Key<String> SYMBOL_TEXT = SYMBOL.strAttr("text");
  // Literal
  Attrs.Key<LiteralType> LITERAL_TYPE = LITERAL.attr("type", LiteralType.class);
  Attrs.Key<Object> LITERAL_VALUE = LITERAL.attr("value", Object.class);
  Attrs.Key<String> LITERAL_UNIT = LITERAL.strAttr("unit");
  // Aggregate
  Attrs.Key<String> AGGREGATE_NAME = AGGREGATE.strAttr("name");
  Attrs.Key<Boolean> AGGREGATE_DISTINCT = AGGREGATE.boolAttr("distinct");
  Attrs.Key<List<SQLNode>> AGGREGATE_ARGS = AGGREGATE.nodesAttr("args");
  Attrs.Key<String> AGGREGATE_WINDOW_NAME = AGGREGATE.strAttr("windowName");
  Attrs.Key<SQLNode> AGGREGATE_WINDOW_SPEC = AGGREGATE.nodeAttr("windowSpec");
  Attrs.Key<SQLNode> AGGREGATE_FILTER = AGGREGATE.nodeAttr("filter");
  Attrs.Key<List<SQLNode>> AGGREGATE_WITHIN_GROUP_ORDER = AGGREGATE.nodesAttr("withinGroupOrder");
  Attrs.Key<List<SQLNode>> AGGREGATE_ORDER = AGGREGATE.nodesAttr("order");
  Attrs.Key<String> AGGREGATE_SEP = AGGREGATE.strAttr("sep");
  // Wildcard
  Attrs.Key<SQLNode> WILDCARD_TABLE = WILDCARD.nodeAttr("table");
  // Grouping
  Attrs.Key<List<SQLNode>> GROUPING_OP_EXPRS = GROUPING_OP.nodesAttr("exprs");
  // Unary
  Attrs.Key<UnaryOp> UNARY_OP = UNARY.attr("op", UnaryOp.class);
  Attrs.Key<SQLNode> UNARY_EXPR = UNARY.nodeAttr("expr");
  // Binary
  Attrs.Key<BinaryOp> BINARY_OP = BINARY.attr("op", BinaryOp.class);
  Attrs.Key<SQLNode> BINARY_LEFT = BINARY.nodeAttr("left");
  Attrs.Key<SQLNode> BINARY_RIGHT = BINARY.nodeAttr("right");
  Attrs.Key<SubqueryOption> BINARY_SUBQUERY_OPTION =
      BINARY.attr("subqueryOption", SubqueryOption.class);
  // TERNARY
  Attrs.Key<TernaryOp> TERNARY_OP = TERNARY.attr("op", TernaryOp.class);
  Attrs.Key<SQLNode> TERNARY_LEFT = TERNARY.nodeAttr("left");
  Attrs.Key<SQLNode> TERNARY_MIDDLE = TERNARY.nodeAttr("middle");
  Attrs.Key<SQLNode> TERNARY_RIGHT = TERNARY.nodeAttr("right");
  // Tuple
  Attrs.Key<List<SQLNode>> TUPLE_EXPRS = TUPLE.nodesAttr("exprs");
  Attrs.Key<Boolean> TUPLE_AS_ROW = TUPLE.boolAttr("asRow");
  // Exists
  Attrs.Key<SQLNode> EXISTS_SUBQUERY_EXPR = EXISTS.nodeAttr("subquery");
  // MatchAgainst
  Attrs.Key<List<SQLNode>> MATCH_COLS = MATCH.nodesAttr("columns");
  Attrs.Key<SQLNode> MATCH_EXPR = MATCH.nodeAttr("expr");
  Attrs.Key<MatchOption> MATCH_OPTION = MATCH.attr("option", MatchOption.class);
  // Cast
  Attrs.Key<SQLNode> CAST_EXPR = CAST.nodeAttr("expr");
  Attrs.Key<SQLDataType> CAST_TYPE = CAST.attr("type", SQLDataType.class);
  Attrs.Key<Boolean> CAST_IS_ARRAY = CAST.boolAttr("isArray");
  // Case
  Attrs.Key<SQLNode> CASE_COND = CASE.nodeAttr("condition");
  Attrs.Key<List<SQLNode>> CASE_WHENS = CASE.nodesAttr("when");
  Attrs.Key<SQLNode> CASE_ELSE = CASE.nodeAttr("else");
  // When
  Attrs.Key<SQLNode> WHEN_COND = WHEN.nodeAttr("condition");
  Attrs.Key<SQLNode> WHEN_EXPR = WHEN.nodeAttr("expr");
  // ConvertUsing
  Attrs.Key<SQLNode> CONVERT_USING_EXPR = CONVERT_USING.nodeAttr("expr");
  Attrs.Key<SQLNode> CONVERT_USING_CHARSET = CONVERT_USING.nodeAttr("charset");
  // Default
  Attrs.Key<SQLNode> DEFAULT_COL = DEFAULT.nodeAttr("col");
  // Values
  Attrs.Key<SQLNode> VALUES_EXPR = VALUES.nodeAttr("expr");
  // QueryExpr
  Attrs.Key<SQLNode> QUERY_EXPR_QUERY = QUERY_EXPR.nodeAttr("query");
  // Indirection
  Attrs.Key<SQLNode> INDIRECTION_EXPR = INDIRECTION.nodeAttr("expr");
  Attrs.Key<List<SQLNode>> INDIRECTION_COMPS = INDIRECTION.nodesAttr("comps");
  // IndirectionComp
  Attrs.Key<Boolean> INDIRECTION_COMP_SUBSCRIPT = INDIRECTION_COMP.boolAttr("subscript");
  Attrs.Key<SQLNode> INDIRECTION_COMP_START = INDIRECTION_COMP.nodeAttr("start");
  Attrs.Key<SQLNode> INDIRECTION_COMP_END = INDIRECTION_COMP.nodeAttr("end");
  // ParamMarker
  Attrs.Key<Integer> PARAM_MARKER_NUMBER = PARAM_MARKER.attr("number", Integer.class);
  // ComparisonMod
  Attrs.Key<SubqueryOption> COMPARISON_MOD_OPTION =
      COMPARISON_MOD.attr("option", SubqueryOption.class);
  Attrs.Key<SQLNode> COMPARISON_MOD_EXPR = COMPARISON_MOD.nodeAttr("expr");
  Attrs.Key<List<SQLNode>> ARRAY_ELEMENTS = ARRAY.nodesAttr("elements");
  Attrs.Key<SQLDataType> TYPE_COERCION_TYPE = TYPE_COERCION.attr("type", SQLDataType.class);
  Attrs.Key<String> TYPE_COERCION_STRING = TYPE_COERCION.strAttr("type");
  Attrs.Key<SQLNode> DATETIME_OVERLAP_LEFT_START = DATETIME_OVERLAP.nodeAttr("leftStart");
  Attrs.Key<SQLNode> DATETIME_OVERLAP_LEFT_END = DATETIME_OVERLAP.nodeAttr("leftEnd");
  Attrs.Key<SQLNode> DATETIME_OVERLAP_RIGHT_START = DATETIME_OVERLAP.nodeAttr("rightStart");
  Attrs.Key<SQLNode> DATETIME_OVERLAP_RIGHT_END = DATETIME_OVERLAP.nodeAttr("rightEnd");

  static int getOperatorPrecedence(SQLNode node) {
    if (!EXPR.isInstance(node)) return -1;
    final ExprType exprKind = node.get(EXPR_KIND);
    switch (exprKind) {
      case UNARY:
        return node.get(UNARY_OP).precedence();
      case BINARY:
        return node.get(BINARY_OP).precedence();
      case TERNARY:
        return node.get(TERNARY_OP).precedence();
      case CASE:
      case WHEN:
        return 5;
      case COLLATE:
        return 13;
      case INTERVAL:
        return 14;
      default:
        return -1;
    }
  }
}
