package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.SQLContext;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprType;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType;
import sjtu.ipads.wtune.sqlparser.ast.internal.Root;
import sjtu.ipads.wtune.sqlparser.rel.Relation;

import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.EXPR_KIND;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.TABLE_SOURCE_KIND;

public interface SQLNode extends Attrs<SQLNode> {
  String POSTGRESQL = "postgresql";
  String MYSQL = "mysql";

  SQLContext context();

  Relation relation();

  SQLNode parent();

  NodeType nodeType();

  void setParent(SQLNode parent);

  void setRelation(Relation relation);

  void setNodeType(NodeType type);

  // inplace update
  // Note: cycle-free is not checked internally, please take extra care to ensure that
  // the descendant of `replacement` does not contains `this`
  void update(SQLNode replacement);

  void accept(SQLVisitor visitor);

  String toString(boolean oneline);

  default String dbType() {
    return context() == null ? MYSQL : context().dbType();
  }

  static SQLNode simple(SQLNode other) {
    return Root.build(other);
  }

  static SQLNode simple(NodeType nodeType) {
    return Root.build(nodeType);
  }

  static SQLNode simple(ExprType exprKind) {
    final SQLNode node = simple(NodeType.EXPR);
    node.put(EXPR_KIND, exprKind);
    return node;
  }

  static SQLNode simple(TableSourceType tableSourceKind) {
    final SQLNode node = simple(NodeType.TABLE_SOURCE);
    node.put(TABLE_SOURCE_KIND, tableSourceKind);
    return node;
  }
}
