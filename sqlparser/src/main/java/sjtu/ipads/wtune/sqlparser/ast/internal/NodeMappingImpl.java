package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class NodeMappingImpl implements NodeMapping {
  private final NodeMapping base;
  private final Map<SQLNode, SQLNode> refs;
  private final Map<SQLNode, SQLNode> invRefs;
  private final Map<SQLNode, SQLNode> parents;

  private NodeMappingImpl(NodeMapping base) {
    this.base = base;
    this.refs = new IdentityHashMap<>();
    this.invRefs = new IdentityHashMap<>();
    this.parents = new IdentityHashMap<>();
  }

  public static NodeMapping build() {
    return new NodeMappingImpl(EmptyNodeMapping.INSTANCE);
  }

  public static NodeMapping build(NodeMapping base) {
    return new NodeMappingImpl(base);
  }

  @Override
  public boolean containsReferee(SQLNode node) {
    return refs.containsKey(node) || base.containsReferee(node);
  }

  @Override
  public boolean containsReferred(SQLNode node) {
    return invRefs.containsKey(node) || base.containsReferred(node);
  }

  @Override
  public SQLNode getRef(SQLNode node) {
    final SQLNode ref = refs.get(node);
    return ref != null ? ref : base.getRef(node);
  }

  @Override
  public SQLNode getInvertedRef(SQLNode node) {
    return invRefs.get(node);
  }

  @Override
  public SQLNode getParent(SQLNode child) {
    final SQLNode parent = parents.get(child);
    return parent != null ? parent : base.getParent(child);
  }

  @Override
  public void setRef(SQLNode n0, SQLNode n1) {
    refs.put(n0, n1);
    invRefs.put(n1, n0);
  }

  @Override
  public void setParent(SQLNode n0, SQLNode n1) {
    if (!containsReferee(n0) || !containsReferred(n1)) throw new NoSuchElementException();
    parents.put(n0, n1);
  }

  private static class EmptyNodeMapping implements NodeMapping {
    private static final NodeMapping INSTANCE = new EmptyNodeMapping();

    @Override
    public boolean containsReferee(SQLNode node) {
      return false;
    }

    @Override
    public boolean containsReferred(SQLNode node) {
      return false;
    }

    @Override
    public SQLNode getRef(SQLNode node) {
      return null;
    }

    @Override
    public SQLNode getInvertedRef(SQLNode node) {
      return null;
    }

    @Override
    public void setRef(SQLNode n0, SQLNode n1) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setParent(SQLNode n0, SQLNode n1) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SQLNode getParent(SQLNode child) {
      return null;
    }
  }
}
