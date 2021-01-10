package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import java.util.Map;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class SQLContextImpl implements InternalSQLContext {
  private final String dbType;
  private int version;
  private int nextVersion;
  private NodeMapping nodeMapping = NodeMapping.empty();

  private SQLContextImpl(String dbType) {
    this.dbType = dbType;
  }

  public static InternalSQLContext build(String dbType) {
    return new SQLContextImpl(dbType);
  }

  @Override
  public String dbType() {
    return dbType;
  }

  @Override
  public SQLNode manage(SQLNode node) {
    return manage0(node, null);
  }

  @Override
  public boolean containsReferee(SQLNode node) {
    return nodeMapping.containsReferee(node);
  }

  @Override
  public boolean containsReferred(SQLNode node) {
    return nodeMapping.containsReferred(node);
  }

  @Override
  public SQLNode getRef(SQLNode node) {
    return nodeMapping.getRef(node);
  }

  @Override
  public SQLNode getInvertedRef(SQLNode node) {
    return nodeMapping.getInvertedRef(node);
  }

  @Override
  public SQLNode getParent(SQLNode child) {
    return nodeMapping.getInvertedRef(nodeMapping.getParent(child));
  }

  @Override
  public void setRef(SQLNode n0, SQLNode n1) {
    if (n0 instanceof SimpleNode) n0 = manage(n0);

    if (n1.context() == this) n1 = getRef(n1);
    else manage(n1);

    nodeMapping.setRef(n0, n1);
  }

  @Override
  public void setParent(SQLNode n0, SQLNode n1) {
    if (n0 instanceof SimpleNode) n0 = manage(n0);

    if (n1.context() == this) n1 = getRef(n1);
    else manage(n1);

    nodeMapping.setParent(n0, n1);
  }

  @Override
  public int version() {
    return version;
  }

  @Override
  public void derive() {
    version = nextVersion++;
    nodeMapping = NodeMapping.basedOn(nodeMapping);
  }

  @Override
  public Snapshot snapshot() {
    return new SnapshotImpl(version, nodeMapping);
  }

  @Override
  public void setSnapshot(Snapshot snapshot) {
    this.version = snapshot.version();
    this.nodeMapping = snapshot.key();
  }

  private SQLNode manage0(SQLNode node) {
    final SQLNode managed = ManagedNode.build(this);
    nodeMapping.setRef(managed, node);
    return managed;
  }

  @SuppressWarnings("unchecked")
  private SQLNode manage0(SQLNode child, SQLNode parent) {
    if (child == null) return null;
    final SQLNode node = (child instanceof ManagedNode) ? ((ManagedNode) child).node() : child;

    final SQLNode managed = manage0(node);
    if (parent != null) nodeMapping.setParent(managed, parent);

    final Map<String, Object> attrs = node.directAttrs();
    for (var pair : attrs.entrySet()) {
      final String key = pair.getKey();
      final Object value = pair.getValue();

      if (SQLNode.isNode(value)) attrs.put(key, manage0((SQLNode) value, node));
      else if (SQLNode.isNodes(value))
        attrs.put(key, listMap(n -> manage0(n, node), (Iterable<SQLNode>) value));
    }

    return managed;
  }

  private static class SnapshotImpl implements Snapshot {
    private final int version;
    private final NodeMapping nodeMapping;

    private SnapshotImpl(int version, NodeMapping nodeMapping) {
      this.nodeMapping = nodeMapping;
      this.version = version;
    }

    @Override
    public int version() {
      return version;
    }

    @Override
    public NodeMapping key() {
      return nodeMapping;
    }
  }
}
