package sjtu.ipads.wtune.sqlparser.ast.internal;

import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.EXPR_KIND;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.NODE_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.PARENT;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.TABLE_SOURCE_KIND;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_SOURCE;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ASTContext;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;
import sjtu.ipads.wtune.sqlparser.ast.Formatter;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind;

public class NodeImpl implements ASTNode {
  private final Map<FieldKey, Object> directAttrs;
  private ASTContext context;

  private NodeImpl(NodeType type) {
    this(type, new HashMap<>(8));
  }

  private NodeImpl(NodeType type, Map<FieldKey, Object> directAttrs) {
    this.directAttrs = directAttrs;
    this.directAttrs.put(NODE_TYPE, type);
  }

  public static ASTNode build(NodeType nodeType) {
    return new NodeImpl(nodeType);
  }

  @Override
  public Map<FieldKey, Object> directAttrs() {
    return directAttrs;
  }

  @Override
  public ASTContext context() {
    return context;
  }

  @Override
  public void setContext(ASTContext context) {
    this.context = context;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void update(ASTNode other) {
    for (FieldKey fieldKey : fields0()) if (fieldKey != PARENT) unset(fieldKey);

    set(NODE_TYPE, other.nodeType());
    if (EXPR.isInstance(other)) set(EXPR_KIND, other.get(EXPR_KIND));
    else if (TABLE_SOURCE.isInstance(other)) set(TABLE_SOURCE_KIND, other.get(TABLE_SOURCE_KIND));

    for (var pair : other.fields().entrySet())
      if (pair.getKey() != PARENT) set(pair.getKey(), pair.getValue());

    // extra care that make other.parent() != other
    final ASTNode parent = parent();
    if (parent == null) other.unset(PARENT);
    else other.set(PARENT, parent());
  }

  @Override
  public ASTNode shallowCopy() {
    return new NodeImpl(nodeType(), new HashMap<>(fields()));
  }

  @Override
  public ASTNode deepCopy() {
    final ASTNode copy = shallowCopy();
    for (var kv : copy.fields().entrySet())
      if (kv.getKey() != PARENT) copy.set(kv.getKey(), deepCopy0(kv.getValue()));

    ASTNode.setContext(copy, context);
    return copy;
  }

  @Override
  public void accept(ASTVistor visitor) {
    if (VisitorController.enter(this, visitor)) VisitorController.visitChildren(this, visitor);
    VisitorController.leave(this, visitor);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final NodeImpl other = (NodeImpl) o;

    final NodeType type = nodeType();
    if (other.nodeType() != type) return false;

    for (FieldKey fieldKey : fields0())
      if (!Objects.equals(this.get(fieldKey), other.get(fieldKey))) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int hash = nodeType().hashCode();
    for (FieldKey fieldKey : fields0()) hash = hash * 31 + Objects.hashCode(get(fieldKey));

    return hash;
  }

  private Iterable<FieldKey> fields0() {
    final List<FieldKey> nodeFields = nodeType().fields();

    final ExprKind exprKind = get(EXPR_KIND);
    final List<FieldKey> exprFields =
        exprKind == null ? Collections.emptyList() : exprKind.fields();

    final TableSourceKind tableSourceKind = get(TABLE_SOURCE_KIND);
    final List<FieldKey> tableSourceFields =
        tableSourceKind == null ? Collections.emptyList() : tableSourceKind.fields();

    return listJoin(nodeFields, exprFields, tableSourceFields);
  }

  @Override
  public String toString() {
    return toString(true);
  }

  @Override
  public String toString(boolean oneline) {
    final Formatter formatter = new Formatter(oneline);
    accept(formatter);
    return formatter.toString();
  }

  private static Object deepCopy0(Object obj) {
    if (obj instanceof ASTNode) return ((ASTNode) obj).deepCopy();
    else if (obj instanceof Iterable) return listMap(NodeImpl::deepCopy0, (Iterable<?>) obj);
    else return obj;
  }
}
