package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.sqlparser.SQLContext;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprType;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType;
import sjtu.ipads.wtune.sqlparser.ast.internal.NodeImpl;

import java.util.Map;

import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.SQLVisitor.topDownVisit;

public interface SQLNode extends Fields {
  String POSTGRESQL = "postgresql";
  String MYSQL = "mysql";

  SQLContext context();

  void setContext(SQLContext ctx);

  /**
   * Copy attributes from `replacement` to this node.
   *
   * <p>Attributes that are incompatible with the node's type/expr kind/table source kind will not
   * be copied.
   *
   * <p>Note: take extra care to ensure the resultant AST acyclic.
   */
  void update(SQLNode replacement);

  void accept(SQLVisitor visitor);

  String toString(boolean oneline);

  default SQLNode copy() {
    final SQLNode newNode = node(nodeType());
    newNode.update(this);
    return newNode;
  }

  default String dbType() {
    return context() == null ? MYSQL : context().dbType();
  }

  default NodeType nodeType() {
    return get(NODE_TYPE);
  }

  default SQLNode parent() {
    return get(PARENT);
  }

  default Map<FieldKey, Object> attrs() {
    final FieldManager mgr = attrMgr();
    return mgr != null ? mgr.getFields(this) : directAttrs();
  }

  default <T> T manager(Class<T> cls) {
    final SQLContext ctx = context();
    return ctx != null ? ctx.manager(cls) : null;
  }

  default FieldManager attrMgr() {
    return manager(FieldManager.class);
  }

  static SQLNode node(NodeType nodeType) {
    return NodeImpl.build(nodeType);
  }

  static SQLNode expr(ExprType exprKind) {
    final SQLNode node = node(NodeType.EXPR);
    node.set(EXPR_KIND, exprKind);
    return node;
  }

  static SQLNode tableSource(TableSourceType tableSourceKind) {
    final SQLNode node = node(NodeType.TABLE_SOURCE);
    node.set(TABLE_SOURCE_KIND, tableSourceKind);
    return node;
  }

  static void setParent(Object obj, SQLNode parent) {
    if (obj instanceof SQLNode) ((SQLNode) obj).set(PARENT, parent);
    else if (obj instanceof Iterable) ((Iterable<?>) obj).forEach(it -> setParent(it, parent));
  }

  static void setContext(Object obj, SQLContext ctx) {
    if (obj instanceof SQLNode) ((SQLNode) obj).accept(topDownVisit(it -> it.setContext(ctx)));
    else if (obj instanceof Iterable) ((Iterable<?>) obj).forEach(it -> setContext(it, ctx));
  }
}
