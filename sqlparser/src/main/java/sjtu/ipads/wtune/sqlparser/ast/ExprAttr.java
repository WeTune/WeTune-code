package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.sqlparser.ast.constants.*;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.NodeAttr.EXPR_KIND;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;

public interface ExprAttr {
  // Variable
  AttrKey<VariableScope> VARIABLE_SCOPE = VARIABLE.attr("scope", VariableScope.class);
  AttrKey<String> VARIABLE_NAME = VARIABLE.strAttr("name");
  AttrKey<SQLNode> VARIABLE_ASSIGNMENT = VARIABLE.nodeAttr("assignment");
  // Col Ref
  AttrKey<SQLNode> COLUMN_REF_COLUMN = COLUMN_REF.nodeAttr("column");
  // Func Call
  AttrKey<SQLNode> FUNC_CALL_NAME = FUNC_CALL.nodeAttr("name");
  AttrKey<List<SQLNode>> FUNC_CALL_ARGS = FUNC_CALL.nodesAttr("args");
  // Collate
  AttrKey<SQLNode> COLLATE_EXPR = COLLATE.nodeAttr("expr");
  AttrKey<SQLNode> COLLATE_COLLATION = COLLATE.nodeAttr("collation");
  // Interval
  AttrKey<SQLNode> INTERVAL_EXPR = INTERVAL.nodeAttr("expr");
  AttrKey<IntervalUnit> INTERVAL_UNIT = INTERVAL.attr("unit", IntervalUnit.class);
  // Symbol
  AttrKey<String> SYMBOL_TEXT = SYMBOL.strAttr("text");
  // Literal
  AttrKey<LiteralType> LITERAL_TYPE = LITERAL.attr("type", LiteralType.class);
  AttrKey<Object> LITERAL_VALUE = LITERAL.attr("value", Object.class);
  AttrKey<String> LITERAL_UNIT = LITERAL.strAttr("unit");
  // Aggregate
  AttrKey<String> AGGREGATE_NAME = AGGREGATE.strAttr("name");
  AttrKey<Boolean> AGGREGATE_DISTINCT = AGGREGATE.boolAttr("distinct");
  AttrKey<List<SQLNode>> AGGREGATE_ARGS = AGGREGATE.nodesAttr("args");
  AttrKey<String> AGGREGATE_WINDOW_NAME = AGGREGATE.strAttr("windowName");
  AttrKey<SQLNode> AGGREGATE_WINDOW_SPEC = AGGREGATE.nodeAttr("windowSpec");
  AttrKey<SQLNode> AGGREGATE_FILTER = AGGREGATE.nodeAttr("filter");
  AttrKey<List<SQLNode>> AGGREGATE_WITHIN_GROUP_ORDER = AGGREGATE.nodesAttr("withinGroupOrder");
  AttrKey<List<SQLNode>> AGGREGATE_ORDER = AGGREGATE.nodesAttr("order");
  AttrKey<String> AGGREGATE_SEP = AGGREGATE.strAttr("sep");
  // Wildcard
  AttrKey<SQLNode> WILDCARD_TABLE = WILDCARD.nodeAttr("table");
  // Grouping
  AttrKey<List<SQLNode>> GROUPING_OP_EXPRS = GROUPING_OP.nodesAttr("exprs");
  // Unary
  AttrKey<UnaryOp> UNARY_OP = UNARY.attr("op", UnaryOp.class);
  AttrKey<SQLNode> UNARY_EXPR = UNARY.nodeAttr("expr");
  // Binary
  AttrKey<BinaryOp> BINARY_OP = BINARY.attr("op", BinaryOp.class);
  AttrKey<SQLNode> BINARY_LEFT = BINARY.nodeAttr("left");
  AttrKey<SQLNode> BINARY_RIGHT = BINARY.nodeAttr("right");
  AttrKey<SubqueryOption> BINARY_SUBQUERY_OPTION =
      BINARY.attr("subqueryOption", SubqueryOption.class);
  // TERNARY
  AttrKey<TernaryOp> TERNARY_OP = TERNARY.attr("op", TernaryOp.class);
  AttrKey<SQLNode> TERNARY_LEFT = TERNARY.nodeAttr("left");
  AttrKey<SQLNode> TERNARY_MIDDLE = TERNARY.nodeAttr("middle");
  AttrKey<SQLNode> TERNARY_RIGHT = TERNARY.nodeAttr("right");
  // Tuple
  AttrKey<List<SQLNode>> TUPLE_EXPRS = TUPLE.nodesAttr("exprs");
  AttrKey<Boolean> TUPLE_AS_ROW = TUPLE.boolAttr("asRow");
  // Exists
  AttrKey<SQLNode> EXISTS_SUBQUERY_EXPR = EXISTS.nodeAttr("subquery");
  // MatchAgainst
  AttrKey<List<SQLNode>> MATCH_COLS = MATCH.nodesAttr("columns");
  AttrKey<SQLNode> MATCH_EXPR = MATCH.nodeAttr("expr");
  AttrKey<MatchOption> MATCH_OPTION = MATCH.attr("option", MatchOption.class);
  // Cast
  AttrKey<SQLNode> CAST_EXPR = CAST.nodeAttr("expr");
  AttrKey<SQLDataType> CAST_TYPE = CAST.attr("type", SQLDataType.class);
  AttrKey<Boolean> CAST_IS_ARRAY = CAST.boolAttr("isArray");
  // Case
  AttrKey<SQLNode> CASE_COND = CASE.nodeAttr("condition");
  AttrKey<List<SQLNode>> CASE_WHENS = CASE.nodesAttr("when");
  AttrKey<SQLNode> CASE_ELSE = CASE.nodeAttr("else");
  // When
  AttrKey<SQLNode> WHEN_COND = WHEN.nodeAttr("condition");
  AttrKey<SQLNode> WHEN_EXPR = WHEN.nodeAttr("expr");
  // ConvertUsing
  AttrKey<SQLNode> CONVERT_USING_EXPR = CONVERT_USING.nodeAttr("expr");
  AttrKey<SQLNode> CONVERT_USING_CHARSET = CONVERT_USING.nodeAttr("charset");
  // Default
  AttrKey<SQLNode> DEFAULT_COL = DEFAULT.nodeAttr("col");
  // Values
  AttrKey<SQLNode> VALUES_EXPR = VALUES.nodeAttr("expr");
  // QueryExpr
  AttrKey<SQLNode> QUERY_EXPR_QUERY = QUERY_EXPR.nodeAttr("query");
  // Indirection
  AttrKey<SQLNode> INDIRECTION_EXPR = INDIRECTION.nodeAttr("expr");
  AttrKey<List<SQLNode>> INDIRECTION_COMPS = INDIRECTION.nodesAttr("comps");
  // IndirectionComp
  AttrKey<Boolean> INDIRECTION_COMP_SUBSCRIPT = INDIRECTION_COMP.boolAttr("subscript");
  AttrKey<SQLNode> INDIRECTION_COMP_START = INDIRECTION_COMP.nodeAttr("start");
  AttrKey<SQLNode> INDIRECTION_COMP_END = INDIRECTION_COMP.nodeAttr("end");
  // ParamMarker
  AttrKey<Integer> PARAM_MARKER_NUMBER = PARAM_MARKER.attr("number", Integer.class);
  // ComparisonMod
  AttrKey<SubqueryOption> COMPARISON_MOD_OPTION =
      COMPARISON_MOD.attr("option", SubqueryOption.class);
  AttrKey<SQLNode> COMPARISON_MOD_EXPR = COMPARISON_MOD.nodeAttr("expr");
  AttrKey<List<SQLNode>> ARRAY_ELEMENTS = ARRAY.nodesAttr("elements");
  AttrKey<SQLDataType> TYPE_COERCION_TYPE = TYPE_COERCION.attr("type", SQLDataType.class);
  AttrKey<String> TYPE_COERCION_STRING = TYPE_COERCION.strAttr("type");
  AttrKey<SQLNode> DATETIME_OVERLAP_LEFT_START = DATETIME_OVERLAP.nodeAttr("leftStart");
  AttrKey<SQLNode> DATETIME_OVERLAP_LEFT_END = DATETIME_OVERLAP.nodeAttr("leftEnd");
  AttrKey<SQLNode> DATETIME_OVERLAP_RIGHT_START = DATETIME_OVERLAP.nodeAttr("rightStart");
  AttrKey<SQLNode> DATETIME_OVERLAP_RIGHT_END = DATETIME_OVERLAP.nodeAttr("rightEnd");

  static int getOperatorPrecedence(SQLNode node) {
    if (!EXPR.isInstance(node)) return -1;
    final ExprType exprKind = node.get(EXPR_KIND);
    return switch (exprKind) {
      case UNARY -> node.get(UNARY_OP).precedence();
      case BINARY -> node.get(BINARY_OP).precedence();
      case TERNARY -> node.get(TERNARY_OP).precedence();
      case CASE, WHEN -> 5;
      case COLLATE -> 13;
      case INTERVAL -> 14;
      default -> -1;
    };
  }
}
