package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLContext;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

public class SQLContextImpl implements SQLContext, NodeMgr {
  private final String dbType;
  private NodeMgr mgr = NodeMgr.empty(this);

  private SQLContextImpl(String dbType) {
    this.dbType = dbType;
  }

  public static SQLContextImpl build(String dbType) {
    return new SQLContextImpl(dbType);
  }

  @Override
  public String dbType() {
    return dbType;
  }

  @Override
  public Tree retrieveTree(Root root) {
    return mgr.retrieveTree(root);
  }

  @Override
  public Root getRoot(Tree tree) {
    return mgr.getRoot(tree);
  }

  @Override
  public Tree getTree(Root root) {
    return mgr.getTree(root);
  }

  @Override
  public Root getParent(Tree child) {
    return mgr.getParent(child);
  }

  @Override
  public void setRoot(Tree tree, Root root) {
    mgr.setRoot(tree, root);
  }

  @Override
  public void setParent(Tree tree, Root root) {
    mgr.setParent(tree, root);
  }

  @Override
  public void derive() {
    mgr = NodeMgr.basedOn(this, mgr);
  }

  @Override
  public Snapshot snapshot() {
    return new SnapshotImpl(mgr);
  }

  @Override
  public void setSnapshot(Snapshot snapshot) {
    this.mgr = snapshot.key();
  }

  public SQLNode manage(SQLNode node) {
    if (node instanceof Root) return retrieveTree((Root) node);
    else if (node instanceof Tree) return retrieveTree(((Tree) node).root());
    else throw new IllegalArgumentException();
  }

  private static class SnapshotImpl implements Snapshot {
    private final NodeMgr nodeMgr;

    private SnapshotImpl(NodeMgr nodeMgr) {
      this.nodeMgr = nodeMgr;
    }

    @Override
    public NodeMgr key() {
      return nodeMgr;
    }
  }
}
