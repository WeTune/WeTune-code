package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.sqlparser.SQLContext;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;

import java.util.EnumSet;
import java.util.Map;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class Tree extends Node {
  private final SQLContextImpl ctx;
  private Root root;

  private Tree(SQLContextImpl ctx) {
    this.ctx = ctx;
  }

  static Tree build(SQLContextImpl ctx) {
    return new Tree(ctx);
  }

  Root root() {
    if (root == null) root = ctx.getRoot(this);
    return root;
  }

  @Override
  public Map<String, Object> directAttrs() {
    return root().directAttrs();
  }

  @Override
  public <T> T put(String attrName, T obj) {
    return super.put(attrName, manage(obj));
  }

  @Override
  public NodeType nodeType() {
    return root().nodeType();
  }

  @Override
  public SQLContext context() {
    return ctx;
  }

  @Override
  public SQLNode parent() {
    return ctx.getTree(ctx.getParent(this));
  }

  @Override
  public void setParent(SQLNode parent) {
    ctx.setParent(this, retrieveTree(parent).root());
  }

  @Override
  public void setNodeType(NodeType type) {
    root().setNodeType(type);
  }

  @Override
  public void update(SQLNode other) {
    ctx.setRoot(this, root = retrieveTree(other).root());
  }

  @SuppressWarnings("unchecked")
  private <T> T manage(T obj) {
    if (obj instanceof SQLNode) {
      final Tree tree = retrieveTree((SQLNode) obj);
      ctx.setParent(tree, root());
      return (T) tree;

    } else if (obj instanceof EnumSet) return obj;
    else if (obj instanceof Iterable) return (T) listMap(this::manage, ((Iterable<?>) obj));
    else return obj;
  }

  private Tree retrieveTree(SQLNode node) {
    // treeOf returns a existing Tree if the root is registered in the ctx
    // o.w. a newly created Tree
    if (node instanceof Root) return ctx.retrieveTree((Root) node);
    else if (node instanceof Tree) return ctx.retrieveTree(((Tree) node).root());
    else throw new IllegalArgumentException();
  }
}
