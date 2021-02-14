package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.*;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.JOINED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.SIMPLE_SOURCE;

public interface ASTNodeFactory {
  default ASTNode newNode(NodeType type) {
    return ASTNode.node(type);
  }

  default ASTNode newNode(ExprKind exprKind) {
    return ASTNode.expr(exprKind);
  }

  default ASTNode newNode(TableSourceKind kind) {
    final ASTNode node = ASTNode.node(NodeType.TABLE_SOURCE);
    node.set(TABLE_SOURCE_KIND, kind);
    return node;
  }

  default ASTNode selectItem(ASTNode expr, String alias) {
    final ASTNode node = ASTNode.node(SELECT_ITEM);
    node.set(SELECT_ITEM_EXPR, expr);
    node.set(SELECT_ITEM_ALIAS, alias);
    return node;
  }

  default ASTNode groupItem(ASTNode expr) {
    final ASTNode node = ASTNode.node(GROUP_ITEM);
    node.set(GROUP_ITEM_EXPR, expr);
    return node;
  }

  default ASTNode tableName(String name) {
    final ASTNode node = ASTNode.node(TABLE_NAME);
    node.set(TABLE_NAME_TABLE, name);
    return node;
  }

  default ASTNode columnName(String table, String column) {
    final ASTNode node = ASTNode.node(COLUMN_NAME);
    node.set(COLUMN_NAME_TABLE, table);
    node.set(COLUMN_NAME_COLUMN, column);
    return node;
  }

  default ASTNode name2(String _0, String _1) {
    final ASTNode node = ASTNode.node(NodeType.NAME_2);
    node.set(NAME_2_0, _0);
    node.set(NAME_2_1, _1);
    return node;
  }

  default ASTNode name2(String[] triple) {
    final ASTNode node = ASTNode.node(NodeType.NAME_2);
    node.set(NAME_2_0, triple[0]);
    node.set(NAME_2_1, triple[1]);
    return node;
  }

  default ASTNode name3(String[] triple) {
    final ASTNode node = ASTNode.node(NodeType.NAME_3);
    node.set(NAME_3_0, triple[0]);
    node.set(NAME_3_1, triple[1]);
    node.set(NAME_3_2, triple[2]);
    return node;
  }

  default ASTNode paramMarker() {
    return newNode(ExprKind.PARAM_MARKER);
  }

  default ASTNode symbol(String text) {
    final ASTNode node = newNode(ExprKind.SYMBOL);
    node.set(SYMBOL_TEXT, text);
    return node;
  }

  default ASTNode literal(LiteralType type, Object value) {
    final ASTNode node = newNode(ExprKind.LITERAL);
    node.set(LITERAL_TYPE, type);
    if (value != null) node.set(LITERAL_VALUE, value);
    return node;
  }

  default ASTNode wildcard() {
    return newNode(ExprKind.WILDCARD);
  }

  default ASTNode wildcard(ASTNode table) {
    final ASTNode node = newNode(WILDCARD);
    node.set(WILDCARD_TABLE, table);
    return node;
  }

  default ASTNode unary(ASTNode expr, UnaryOp op) {
    final ASTNode node = newNode(UNARY);
    node.set(UNARY_EXPR, expr);
    node.set(UNARY_OP, op);
    return node;
  }

  default ASTNode binary(ASTNode left, ASTNode right, BinaryOp op) {
    final ASTNode binary = newNode(BINARY);
    binary.set(BINARY_LEFT, left);
    binary.set(BINARY_RIGHT, right);
    binary.set(BINARY_OP, op);
    return binary;
  }

  default ASTNode columnRef(String tableName, String columnName) {
    final ASTNode columnId = newNode(NodeType.COLUMN_NAME);
    columnId.set(COLUMN_NAME_TABLE, tableName);
    columnId.set(COLUMN_NAME_COLUMN, columnName);

    final ASTNode columnRef = newNode(COLUMN_REF);
    columnRef.set(COLUMN_REF_COLUMN, columnId);
    return columnRef;
  }

  default ASTNode columnRef(String schemaName, String tableName, String columnName) {
    final ASTNode columnId = newNode(NodeType.COLUMN_NAME);
    columnId.set(COLUMN_NAME_SCHEMA, schemaName);
    columnId.set(COLUMN_NAME_TABLE, tableName);
    columnId.set(COLUMN_NAME_COLUMN, columnName);

    final ASTNode columnRef = newNode(COLUMN_REF);
    columnRef.set(COLUMN_REF_COLUMN, columnId);
    return columnRef;
  }

  default ASTNode columnRef(ASTNode columnName) {
    assert columnName.nodeType() == NodeType.COLUMN_NAME;
    final ASTNode node = newNode(COLUMN_REF);
    node.set(COLUMN_REF_COLUMN, columnName);
    return node;
  }

  default ASTNode paramMarker(int number) {
    final ASTNode node = newNode(PARAM_MARKER);
    node.set(PARAM_MARKER_NUMBER, number);
    return node;
  }

  default ASTNode indirection(ASTNode expr, List<ASTNode> indirections) {
    final ASTNode indirection = newNode(INDIRECTION);
    indirection.set(INDIRECTION_EXPR, expr);
    indirection.set(INDIRECTION_COMPS, indirections);
    return indirection;
  }

  default ASTNode simple(String name, String alias) {
    final ASTNode node = newNode(SIMPLE_SOURCE);
    node.set(SIMPLE_TABLE, tableName(name));
    node.set(SIMPLE_ALIAS, alias);
    return node;
  }

  default ASTNode joined(ASTNode left, ASTNode right, JoinType type) {
    final ASTNode tableSource = newNode(JOINED_SOURCE);
    tableSource.set(JOINED_LEFT, left);
    tableSource.set(JOINED_RIGHT, right);
    tableSource.set(JOINED_TYPE, type);
    return tableSource;
  }
}
