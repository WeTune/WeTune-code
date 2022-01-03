package sjtu.ipads.wtune.sql.ast;

import static sjtu.ipads.wtune.sql.ast.NodeFields.EXPR_KIND;
import static sjtu.ipads.wtune.sql.ast.NodeFields.NODE_TYPE;
import static sjtu.ipads.wtune.sql.ast.NodeFields.PARENT;
import static sjtu.ipads.wtune.sql.ast.NodeFields.TABLE_SOURCE_KIND;

import java.util.Map;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.sql.ASTContext;
import sjtu.ipads.wtune.sql.ast.constants.ExprKind;
import sjtu.ipads.wtune.sql.ast.constants.NodeType;
import sjtu.ipads.wtune.sql.ast.constants.TableSourceKind;
import sjtu.ipads.wtune.sql.ast.internal.ASTNodeImpl;

public interface ASTNode extends Fields {
  String POSTGRESQL = "postgresql";
  String MYSQL = "mysql";
  String SQLSERVER = "sqlserver";

  ASTContext context();

  void setContext(ASTContext ctx);

  /**
   * Copy fields from `replacement` to this node.
   *
   * <p>Conceptually, the method replaces the whole sub-tree rooted by the this node with the
   * sub-tree rooted by `replacement`, and setup all necessary context and parent relationship.
   *
   * <ul>
   *   <b>NOTE</b>
   *   <li>All existing fields of this node will be unset.
   *   <li>All existing fields & field values of `replacement` will be set to this node as-is,
   *       except those incompatible with the node type/expr kind/table source kind.
   *   <li>Children will not be copied recursively.
   *   <li>Children's parent will be updated as this node.
   *   <li>Children's context will be updated as this node's context recursively.
   *   <li>PLEASE ENSURE THE RESULTANT AST ACYCLIC.
   * </ul>
   */
  void update(ASTNode replacement);

  /**
   * Shallow copy the node.
   *
   * <ul>
   *   <b>NOTE</b>
   *   <li>Children will not be copied recursively.
   *   <li>Children's parent will not point to the copy. Thus, never use the copy as a tree root.
   *   <li>Context will not be inherited.
   * </ul>
   *
   * In short, keep in mind the following two snippets are totally different:
   *
   * <pre>ASTNode copy = node.copy()</pre>
   *
   * <pre>ASTNode copy = newNode(); copy.update(node);</pre>
   */
  ASTNode shallowCopy();

  /**
   * Deep copy the node, i.e. copy the entirely subtree.
   *
   * <ul>
   *   <b>NOTE</b>
   *   <li>Children will be copied recursively.
   *   <li>All node's parent will be updated.
   *   <li>ASTContext will not be inherited.
   * </ul>
   */
  ASTNode deepCopy();

  void accept(ASTVistor visitor);

  String toString(boolean oneline);

  default String dbType() {
    return context() == null ? MYSQL : context().dbType();
  }

  default NodeType nodeType() {
    return get(NODE_TYPE);
  }

  default ASTNode parent() {
    return get(PARENT);
  }

  default Map<FieldKey, Object> fields() {
    final FieldManager mgr = fieldMgr();
    return mgr != null ? mgr.getFields(this) : directAttrs();
  }

  default <T> T manager(Class<T> cls) {
    final ASTContext ctx = context();
    return ctx != null ? ctx.manager(cls) : null;
  }

  default FieldManager fieldMgr() {
    return manager(FieldManager.class);
  }

  static ASTNode node(NodeType nodeType) {
    return ASTNodeImpl.build(nodeType);
  }

  static ASTNode expr(ExprKind exprKind) {
    final ASTNode node = node(NodeType.EXPR);
    node.set(EXPR_KIND, exprKind);
    return node;
  }

  static ASTNode tableSource(TableSourceKind tableSourceKind) {
    final ASTNode node = node(NodeType.TABLE_SOURCE);
    node.set(TABLE_SOURCE_KIND, tableSourceKind);
    return node;
  }

  static void setParent(Object obj, ASTNode parent) {
    if (obj instanceof ASTNode) ((ASTNode) obj).set(PARENT, parent);
    else if (obj instanceof Iterable) ((Iterable<?>) obj).forEach(it -> setParent(it, parent));
  }
}
