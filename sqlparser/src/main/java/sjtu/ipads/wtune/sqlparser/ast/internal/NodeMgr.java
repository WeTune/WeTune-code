package sjtu.ipads.wtune.sqlparser.ast.internal;

public interface NodeMgr {
  Tree retrieveTree(Root root);

  Root getRoot(Tree tree);

  Tree getTree(Root root);

  void setRoot(Tree tree, Root root);

  void setParent(Tree tree, Root root);

  Root getParent(Tree child);

  static NodeMgr empty(SQLContextImpl ctx) {
    return NodeMgrImpl.build(ctx);
  }

  static NodeMgr basedOn(SQLContextImpl ctx, NodeMgr base) {
    return NodeMgrImpl.build(ctx, base);
  }
}
