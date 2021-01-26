package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.sqlparser.SQLContext;
import sjtu.ipads.wtune.sqlparser.ast.Formatter;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLVisitor;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;

import java.util.HashMap;
import java.util.Map;

import static sjtu.ipads.wtune.sqlparser.ast.NodeAttr.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_SOURCE;

public class NodeImpl implements SQLNode {
  private final Map<AttrKey, Object> directAttrs;
  private SQLContext context;

  private NodeImpl(NodeType type) {
    this(type, new HashMap<>(8));
  }

  private NodeImpl(NodeType type, Map<AttrKey, Object> directAttrs) {
    this.directAttrs = directAttrs;
    this.directAttrs.put(NODE_TYPE, type);
  }

  public static SQLNode build(NodeType nodeType) {
    return new NodeImpl(nodeType);
  }

  @Override
  public Map<AttrKey, Object> directAttrs() {
    return directAttrs;
  }

  @Override
  public SQLContext context() {
    return context;
  }

  @Override
  public void setContext(SQLContext context) {
    this.context = context;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void update(SQLNode other) {
    // avoid ConcurrentModificationException
    final AttrKey[] keys = this.directAttrs().keySet().toArray(AttrKey[]::new);
    for (AttrKey key : keys) key.unset(this);

    this.set(NODE_TYPE, other.nodeType());

    if (EXPR.isInstance(other)) this.set(EXPR_KIND, other.get(EXPR_KIND));
    else if (TABLE_SOURCE.isInstance(other))
      this.set(TABLE_SOURCE_KIND, other.get(TABLE_SOURCE_KIND));

    for (var pair : other.attrs().entrySet()) this.set(pair.getKey(), pair.getValue());
  }

  @Override
  public void accept(SQLVisitor visitor) {
    if (VisitorController.enter(this, visitor)) VisitorController.visitChildren(this, visitor);
    VisitorController.leave(this, visitor);
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
}
