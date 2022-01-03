package sjtu.ipads.wtune.sql.parser;

import sjtu.ipads.wtune.sql.ast1.*;
import sjtu.ipads.wtune.sql.ast1.constants.*;

import java.util.List;

import static sjtu.ipads.wtune.sql.ast1.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast1.ExprKind.*;
import static sjtu.ipads.wtune.sql.ast1.SqlKind.Void;
import static sjtu.ipads.wtune.sql.ast1.SqlKind.*;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.ast1.TableSourceFields.*;

public interface AstBuilderMixin {
  SqlContext ast();

  default SqlNode mkVoid() {
    return mkNode(Void);
  }

  default SqlNode mkNode(SqlKind kind) {
    return SqlNode.mk(ast(), ast().mkNode(kind));
  }

  default SqlNode mkName2(String piece0, String piece1) {
    final SqlNode name = mkNode(Name2);
    name.$(Name2_0, piece0);
    name.$(Name2_1, piece1);
    return name;
  }

  default SqlNode mkName3(String piece0, String piece1, String piece2) {
    final SqlNode name = mkNode(Name3);
    name.$(Name3_0, piece0);
    name.$(Name3_1, piece1);
    name.$(Name3_2, piece2);
    return name;
  }

  default SqlNode mkTableName(String schemaName, String tableName) {
    final SqlNode name = mkNode(TableName);
    if (schemaName != null) name.$(TableName_Schema, schemaName);
    name.$(TableName_Table, tableName);
    return name;
  }

  default SqlNode mkColName(String schemaName, String tableName, String colName) {
    final SqlNode name = mkNode(ColName);
    if (schemaName != null) name.$(ColName_Schema, schemaName);
    if (tableName != null) name.$(ColName_Table, tableName);
    name.$(ColName_Col, colName);
    return name;
  }

  default SqlNode mkExpr(ExprKind kind) {
    final SqlNode node = mkNode(Expr);
    node.setField(Expr_Kind, kind);
    return node;
  }

  default SqlNode mkColRef(String schemaName, String tableName, String colName) {
    final SqlNode name = mkColName(schemaName, tableName, colName);
    final SqlNode ref = mkExpr(ColRef);
    ref.$(ColRef_ColName, name);
    return ref;
  }

  default SqlNode mkColRef(SqlNode colName) {
    final SqlNode colRef = mkExpr(ColRef);
    colRef.$(ColRef_ColName, colName);
    return colRef;
  }

  default SqlNode mkLiteral(LiteralKind kind, Object value) {
    final SqlNode literal = mkExpr(ExprKind.Literal);
    literal.$(Literal_Kind, kind);
    literal.$(Literal_Value, value);
    return literal;
  }

  default SqlNode mkSymbol(String text) {
    final SqlNode symbol = mkExpr(Symbol);
    symbol.$(Symbol_Text, text);
    return symbol;
  }

  default SqlNode mkWildcard(SqlNode tableName) {
    final SqlNode wildcard = mkExpr(Wildcard);
    wildcard.$(Wildcard_Table, tableName);
    return wildcard;
  }

  default SqlNode mkParam() {
    return mkExpr(Param);
  }

  default SqlNode mkParam(int index) {
    final SqlNode param = mkParam();
    param.$(Param_Number, index);
    return param;
  }

  default SqlNode mkUnary(SqlNode expr, UnaryOpKind op) {
    final SqlNode unary = mkExpr(Unary);
    unary.$(Unary_Expr, expr);
    unary.$(Unary_Op, op);
    return unary;
  }

  default SqlNode mkBinary(SqlNode left, SqlNode right, BinaryOpKind op) {
    final SqlNode binary = mkExpr(Binary);
    binary.$(Binary_Left, left);
    binary.$(Binary_Right, right);
    binary.$(Binary_Op, op);
    return binary;
  }

  default SqlNode mkInterval(SqlNode expr, IntervalUnit unit) {
    final SqlNode interval = mkExpr(Interval);
    interval.$(Interval_Expr, expr);
    interval.$(Interval_Unit, unit);
    return interval;
  }

  default SqlNode mkIndirection(SqlNode expr, SqlNodes indirections) {
    final SqlNode indirection = mkExpr(Indirection);
    indirection.$(Indirection_Expr, expr);
    indirection.$(Indirection_Comps, indirections);
    return indirection;
  }

  default SqlNode mkSelectItem(SqlNode expr, String alias) {
    final SqlNode selectItem = mkNode(SelectItem);
    selectItem.$(SelectItem_Expr, expr);
    selectItem.$(SelectItem_Alias, alias);
    return selectItem;
  }

  default SqlNode mkGroupItem(SqlNode expr) {
    final SqlNode groupItem = mkNode(GroupItem);
    groupItem.$(GroupItem_Expr, expr);
    return groupItem;
  }

  default SqlNode mkTableSource(TableSourceKind kind) {
    final SqlNode node = mkNode(TableSource);
    node.$(TableSource_Kind, kind);
    return node;
  }

  default SqlNode mkJoined(SqlNode left, SqlNode right, JoinKind kind) {
    final SqlNode joined = mkTableSource(TableSourceKind.JoinedSource);
    joined.$(Joined_Left, left);
    joined.$(Joined_Right, right);
    joined.$(Joined_Kind, kind);
    return joined;
  }

  default SqlNodes mkNodes(List<SqlNode> nodes) {
    return SqlNodes.mk(ast(), nodes);
  }

  default SqlNode wrapAsQuery(SqlNode node) {
    final SqlKind kind = node.kind();
    if (kind == SqlKind.QuerySpec || kind == SetOp) {
      final SqlNode queryNode = mkNode(SqlKind.Query);
      queryNode.$(Query_Body, node);
      return queryNode;
    } else {
      return node;
    }
  }

  default SqlNode wrapAsQueryExpr(SqlNode node) {
    assert Query.isInstance(node);
    final SqlNode queryExpr = mkExpr(QueryExpr);
    queryExpr.setField(QueryExpr_Query, node);
    return queryExpr;
  }
}
