package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.sqlparser.ast.ExprAttr;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.TableSourceAttr;
import sjtu.ipads.wtune.sqlparser.ast.constants.*;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.NodeAttr.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.JOINED;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.SIMPLE_SOURCE;

public interface SQLNodeFactory {
  default SQLNode newNode(NodeType type) {
    return SQLNode.simple(type);
  }

  default SQLNode newNode(ExprType exprKind) {
    return SQLNode.simple(exprKind);
  }

  default SQLNode newNode(TableSourceType kind) {
    final SQLNode node = SQLNode.simple(NodeType.TABLE_SOURCE);
    node.set(TABLE_SOURCE_KIND, kind);
    return node;
  }

  default SQLNode selectItem(SQLNode expr, String alias) {
    final SQLNode node = SQLNode.simple(SELECT_ITEM);
    node.set(SELECT_ITEM_EXPR, expr);
    node.set(SELECT_ITEM_ALIAS, alias);
    return node;
  }

  default SQLNode groupItem(SQLNode expr) {
    final SQLNode node = SQLNode.simple(GROUP_ITEM);
    node.set(GROUP_ITEM_EXPR, expr);
    return node;
  }

  default SQLNode tableName(String name) {
    final SQLNode node = SQLNode.simple(TABLE_NAME);
    node.set(TABLE_NAME_TABLE, name);
    return node;
  }

  default SQLNode columnName(String table, String column) {
    final SQLNode node = SQLNode.simple(COLUMN_NAME);
    node.set(COLUMN_NAME_TABLE, table);
    node.set(COLUMN_NAME_COLUMN, column);
    return node;
  }

  default SQLNode name2(String _0, String _1) {
    final SQLNode node = SQLNode.simple(NodeType.NAME_2);
    node.set(NAME_2_0, _0);
    node.set(NAME_2_1, _1);
    return node;
  }

  default SQLNode name2(String[] triple) {
    final SQLNode node = SQLNode.simple(NodeType.NAME_2);
    node.set(NAME_2_0, triple[0]);
    node.set(NAME_2_1, triple[1]);
    return node;
  }

  default SQLNode name3(String[] triple) {
    final SQLNode node = SQLNode.simple(NodeType.NAME_3);
    node.set(NAME_3_0, triple[0]);
    node.set(NAME_3_1, triple[1]);
    node.set(NAME_3_2, triple[2]);
    return node;
  }

  default SQLNode paramMarker() {
    return newNode(ExprType.PARAM_MARKER);
  }

  default SQLNode symbol(String text) {
    final SQLNode node = newNode(ExprType.SYMBOL);
    node.set(ExprAttr.SYMBOL_TEXT, text);
    return node;
  }

  default SQLNode literal(LiteralType type, Object value) {
    final SQLNode node = newNode(ExprType.LITERAL);
    node.set(ExprAttr.LITERAL_TYPE, type);
    if (value != null) node.set(ExprAttr.LITERAL_VALUE, value);
    return node;
  }

  default SQLNode wildcard() {
    return newNode(ExprType.WILDCARD);
  }

  default SQLNode wildcard(SQLNode table) {
    final SQLNode node = newNode(WILDCARD);
    node.set(ExprAttr.WILDCARD_TABLE, table);
    return node;
  }

  default SQLNode unary(SQLNode expr, UnaryOp op) {
    final SQLNode node = newNode(UNARY);
    node.set(ExprAttr.UNARY_EXPR, expr);
    node.set(ExprAttr.UNARY_OP, op);
    return node;
  }

  default SQLNode binary(SQLNode left, SQLNode right, BinaryOp op) {
    final SQLNode binary = newNode(BINARY);
    binary.set(ExprAttr.BINARY_LEFT, left);
    binary.set(ExprAttr.BINARY_RIGHT, right);
    binary.set(ExprAttr.BINARY_OP, op);
    return binary;
  }

  default SQLNode columnRef(String tableName, String columnName) {
    final SQLNode columnId = newNode(NodeType.COLUMN_NAME);
    columnId.set(COLUMN_NAME_TABLE, tableName);
    columnId.set(COLUMN_NAME_COLUMN, columnName);

    final SQLNode columnRef = newNode(COLUMN_REF);
    columnRef.set(ExprAttr.COLUMN_REF_COLUMN, columnId);
    return columnRef;
  }

  default SQLNode columnRef(String schemaName, String tableName, String columnName) {
    final SQLNode columnId = newNode(NodeType.COLUMN_NAME);
    columnId.set(COLUMN_NAME_SCHEMA, schemaName);
    columnId.set(COLUMN_NAME_TABLE, tableName);
    columnId.set(COLUMN_NAME_COLUMN, columnName);

    final SQLNode columnRef = newNode(COLUMN_REF);
    columnRef.set(ExprAttr.COLUMN_REF_COLUMN, columnId);
    return columnRef;
  }

  default SQLNode columnRef(SQLNode columnName) {
    assert columnName.nodeType() == NodeType.COLUMN_NAME;
    final SQLNode node = newNode(COLUMN_REF);
    node.set(ExprAttr.COLUMN_REF_COLUMN, columnName);
    return node;
  }

  default SQLNode paramMarker(int number) {
    final SQLNode node = newNode(PARAM_MARKER);
    node.set(ExprAttr.PARAM_MARKER_NUMBER, number);
    return node;
  }

  default SQLNode indirection(SQLNode expr, List<SQLNode> indirections) {
    final SQLNode indirection = newNode(INDIRECTION);
    indirection.set(ExprAttr.INDIRECTION_EXPR, expr);
    indirection.set(ExprAttr.INDIRECTION_COMPS, indirections);
    return indirection;
  }

  default SQLNode simple(String name, String alias) {
    final SQLNode node = newNode(SIMPLE_SOURCE);
    node.set(TableSourceAttr.SIMPLE_TABLE, tableName(name));
    node.set(TableSourceAttr.SIMPLE_ALIAS, alias);
    return node;
  }

  default SQLNode joined(SQLNode left, SQLNode right, JoinType type) {
    final SQLNode tableSource = newNode(JOINED);
    tableSource.set(TableSourceAttr.JOINED_LEFT, left);
    tableSource.set(TableSourceAttr.JOINED_RIGHT, right);
    tableSource.set(TableSourceAttr.JOINED_TYPE, type);
    return tableSource;
  }
}
