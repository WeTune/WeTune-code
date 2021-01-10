package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLContext;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;

import java.util.List;
import java.util.Map;

public class ManagedNode extends BaseNode {
  private final InternalSQLContext ctx;
  private int expectedVersion;
  private SQLNode node;

  private ManagedNode(InternalSQLContext ctx) {
    this.ctx = ctx;
  }

  static SQLNode build(InternalSQLContext ctx) {
    return new ManagedNode(ctx);
  }

  SQLNode node() {
    if (node == null || ctx.version() != expectedVersion) {
      expectedVersion = ctx.version();
      node = ctx.getRef(this);
    }
    return node;
  }

  @Override
  public Map<String, Object> directAttrs() {
    return node().directAttrs();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T put(String attrName, T obj) {
    if (SQLNode.isNode(obj)) {
      final SQLNode managed = ctx.manage((SQLNode) obj);
      ctx.setParent(managed, this);
      return (T) super.put(attrName, managed);

    } else if (SQLNode.isNodes(obj)) {
      final List<SQLNode> managed = ctx.manage((Iterable<SQLNode>) obj);
      managed.forEach(it -> ctx.setParent(it, this));
      return (T) super.put(attrName, managed);

    } else return super.put(attrName, obj);
  }

  @Override
  public NodeType nodeType() {
    return node().nodeType();
  }

  @Override
  public SQLNode parent() {
    return ctx.getParent(this);
  }

  @Override
  public SQLContext context() {
    return ctx;
  }

  @Override
  public void setParent(SQLNode parent) {
    ctx.setParent(this, parent);
  }

  @Override
  public void setNodeType(NodeType type) {
    node().setNodeType(type);
  }

  @Override
  public void update(SQLNode other) {
    ctx.setRef(this, other);
    node = null;
  }
}
