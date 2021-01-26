package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.SQLContext;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprType;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType;
import sjtu.ipads.wtune.sqlparser.ast.internal.NodeImpl;
import sjtu.ipads.wtune.sqlparser.rel.Relation;

import java.util.Map;

import static sjtu.ipads.wtune.sqlparser.ast.NodeAttr.*;
import static sjtu.ipads.wtune.sqlparser.rel.Relation.RELATION;

public interface SQLNode extends Attrs {
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

  default String dbType() {
    return context() == null ? MYSQL : context().dbType();
  }

  default NodeType nodeType() {
    return get(NODE_TYPE);
  }

  default SQLNode parent() {
    return get(PARENT);
  }

  default Map<AttrKey, Object> attrs() {
    final AttributeManager mgr = attrMgr();
    return mgr != null ? mgr.getAttrs(this) : directAttrs();
  }

  default Relation relation() {
    return get(RELATION);
  }

  default <T> T manager(Class<T> cls) {
    final SQLContext ctx = context();
    return ctx != null ? ctx.manager(cls) : null;
  }

  default SQLNode copy() {
    final SQLNode newNode = simple(nodeType());
    newNode.update(this);
    return newNode;
  }

  default AttributeManager attrMgr() {
    return manager(AttributeManager.class);
  }

  static SQLNode simple(NodeType nodeType) {
    return NodeImpl.build(nodeType);
  }

  static SQLNode simple(ExprType exprKind) {
    final SQLNode node = simple(NodeType.EXPR);
    node.set(EXPR_KIND, exprKind);
    return node;
  }

  static SQLNode simple(TableSourceType tableSourceKind) {
    final SQLNode node = simple(NodeType.TABLE_SOURCE);
    node.set(TABLE_SOURCE_KIND, tableSourceKind);
    return node;
  }

  static void setParent(Object obj, SQLNode parent) {
    if (obj instanceof SQLNode) ((SQLNode) obj).set(PARENT, parent);
    else if (obj instanceof Iterable) ((Iterable<?>) obj).forEach(it -> setParent(it, parent));
  }
}
