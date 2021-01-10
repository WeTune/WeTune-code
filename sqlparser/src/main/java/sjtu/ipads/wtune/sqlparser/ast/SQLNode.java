package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprType;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType;
import sjtu.ipads.wtune.sqlparser.ast.internal.SimpleNode;

import java.util.stream.StreamSupport;

import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.EXPR_KIND;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.TABLE_SOURCE_KIND;

public interface SQLNode extends Attrs<SQLNode> {
  String POSTGRESQL = "postgresql";
  String MYSQL = "mysql";

  SQLContext context();

  SQLNode parent();

  NodeType nodeType();

  void setParent(SQLNode parent);

  void setNodeType(NodeType type);

  void update(SQLNode other);

  void accept(SQLVisitor visitor);

  String toString(boolean oneline);

  default String dbType() {
    return context() == null ? MYSQL : context().dbType();
  }

  static boolean isNode(Object obj) {
    return obj instanceof SQLNode;
  }

  static boolean isNodes(Object obj) {
    if (!(obj instanceof Iterable)) return false;
    for (Object o : ((Iterable<?>) obj)) {
      if (o != null && !isNode(o)) return false;
    }
    return true;
  }

  static SQLNode simple(SQLNode other) {
    return SimpleNode.build(other);
  }

  static SQLNode simple(NodeType nodeType) {
    return SimpleNode.build(nodeType);
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
