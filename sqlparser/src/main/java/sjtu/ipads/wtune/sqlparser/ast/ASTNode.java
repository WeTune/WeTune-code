package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.sqlparser.ASTContext;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprType;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType;
import sjtu.ipads.wtune.sqlparser.ast.internal.NodeImpl;

import java.util.Map;

import static sjtu.ipads.wtune.sqlparser.ast.ASTVistor.topDownVisit;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;

public interface ASTNode extends Fields {
  String POSTGRESQL = "postgresql";
  String MYSQL = "mysql";

  ASTContext context();

  void setContext(ASTContext ctx);

  /**
   * Copy attributes from `replacement` to this node.
   *
   * <p>Attributes that are incompatible with the node's type/expr kind/table source kind will not
   * be copied.
   *
   * <p>Note: take extra care to ensure the resultant AST acyclic.
   */
  void update(ASTNode replacement);

  void accept(ASTVistor visitor);

  String toString(boolean oneline);

  default ASTNode copy() {
    final ASTNode newNode = node(nodeType());
    newNode.update(this);
    return newNode;
  }

  default String dbType() {
    return context() == null ? MYSQL : context().dbType();
  }

  default NodeType nodeType() {
    return get(NODE_TYPE);
  }

  default ASTNode parent() {
    return get(PARENT);
  }

  default Map<FieldKey, Object> attrs() {
    final FieldManager mgr = attrMgr();
    return mgr != null ? mgr.getFields(this) : directAttrs();
  }

  default <T> T manager(Class<T> cls) {
    final ASTContext ctx = context();
    return ctx != null ? ctx.manager(cls) : null;
  }

  default FieldManager attrMgr() {
    return manager(FieldManager.class);
  }

  static ASTNode node(NodeType nodeType) {
    return NodeImpl.build(nodeType);
  }

  static ASTNode expr(ExprType exprKind) {
    final ASTNode node = node(NodeType.EXPR);
    node.set(EXPR_KIND, exprKind);
    return node;
  }

  static ASTNode tableSource(TableSourceType tableSourceKind) {
    final ASTNode node = node(NodeType.TABLE_SOURCE);
    node.set(TABLE_SOURCE_KIND, tableSourceKind);
    return node;
  }

  static void setParent(Object obj, ASTNode parent) {
    if (obj instanceof ASTNode) ((ASTNode) obj).set(PARENT, parent);
    else if (obj instanceof Iterable) ((Iterable<?>) obj).forEach(it -> setParent(it, parent));
  }

  static void setContext(Object obj, ASTContext ctx) {
    if (obj instanceof ASTNode) ((ASTNode) obj).accept(topDownVisit(it -> it.setContext(ctx)));
    else if (obj instanceof Iterable) ((Iterable<?>) obj).forEach(it -> setContext(it, ctx));
  }
}
