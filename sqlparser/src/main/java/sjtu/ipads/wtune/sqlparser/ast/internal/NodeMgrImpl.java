package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import java.util.IdentityHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class NodeMgrImpl implements NodeMgr {
  private final SQLContextImpl ctx;

  private final NodeMgr base;
  private final Map<Tree, Root> treeToRoot;
  private final Map<Root, Tree> rootToTree;
  private final Map<Tree, Root> parents;

  private NodeMgrImpl(SQLContextImpl ctx, NodeMgr base) {
    this.ctx = ctx;
    this.base = base;
    this.treeToRoot = new IdentityHashMap<>();
    this.rootToTree = new IdentityHashMap<>();
    this.parents = new IdentityHashMap<>();
  }

  public static NodeMgr build(SQLContextImpl context) {
    return new NodeMgrImpl(context, EmptyNodeMgr.INSTANCE);
  }

  public static NodeMgr build(SQLContextImpl context, NodeMgr base) {
    return new NodeMgrImpl(context, base);
  }

  @Override
  public Root getRoot(Tree tree) {
    final Root root = treeToRoot.get(tree);
    return root != null ? root : base.getRoot(tree);
  }

  @Override
  public Tree getTree(Root root) {
    final Tree tree = rootToTree.get(root);
    return tree != null ? tree : base.getTree(root);
  }

  @Override
  public Tree retrieveTree(Root root) {
    if (root == null) return null;

    final Root childRoot = getRoot(root);
    final Tree childTree = retrieveTree0(childRoot);

    final Map<AttrKey<?>, Object> attrs = childRoot.directAttrs();
    for (var pair : attrs.entrySet())
      pair.getKey().set(childTree, pair.getValue());

    return childTree;
  }

  @Override
  public Root getParent(Tree child) {
    final Root parent = parents.get(child);
    return parent != null ? parent : base.getParent(child);
  }

  @Override
  public void setRoot(Tree tree, Root root) {
    requireNonNull(tree);
    requireNonNull(root);

    final Root originRoot = treeToRoot.put(tree, root);
    //    if (originRoot != null)
    //      rootToTree.remove(originRoot);
    final Tree originTree = rootToTree.put(root, tree);

    if (originRoot != null) rootToTree.remove(originRoot);
    if (originTree != tree) {
      treeToRoot.remove(originTree);
      parents.remove(originTree);
    }
  }

  @Override
  public void setParent(Tree tree, Root root) {
    requireNonNull(tree);
    requireNonNull(root);

    // optimistically assume: getTree(root) != null && getRoot(tree) != null
    parents.put(tree, root);
  }

  private Tree retrieveTree0(Root root) {
    Tree tree = getTree(root);
    if (tree != null) return tree;

    tree = Tree.build(ctx);
    setRoot(tree, root);
    return tree;
  }

  private Root getRoot(SQLNode node) {
    if (node instanceof Root) return (Root) node;
    else if (node instanceof Tree) return ((Tree) node).root();
    else throw new IllegalArgumentException();
  }

  private static class EmptyNodeMgr implements NodeMgr {
    private static final NodeMgr INSTANCE = new EmptyNodeMgr();

    @Override
    public Tree retrieveTree(Root root) {
      return null;
    }

    @Override
    public Root getRoot(Tree tree) {
      return null;
    }

    @Override
    public Tree getTree(Root root) {
      return null;
    }

    @Override
    public void setRoot(Tree tree, Root root) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setParent(Tree tree, Root root) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Root getParent(Tree child) {
      return null;
    }
  }
}
