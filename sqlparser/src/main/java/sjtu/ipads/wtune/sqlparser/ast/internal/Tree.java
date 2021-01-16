package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.sqlparser.SQLContext;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.rel.Attribute;
import sjtu.ipads.wtune.sqlparser.rel.Relation;

import java.util.EnumSet;
import java.util.Map;

import static sjtu.ipads.wtune.common.utils.FuncUtils.func;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.rel.Attribute.ATTRIBUTE;
import static sjtu.ipads.wtune.sqlparser.rel.Relation.isRelationBoundary;
import static sjtu.ipads.wtune.sqlparser.rel.Relation.resolve;

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
    return root != null ? root : (root = ctx.getRoot(this));
  }

  @Override
  public Map<String, Object> directAttrs() {
    return root().directAttrs();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T get(Key<T> key) {
    if (key == ATTRIBUTE)
      return (T) supplyIfAbsent((Key<Attribute>) key, func(Attribute::resolve).bind(this));
    else return super.get(key);
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
  public Relation relation() {
    final SQLNode parent = parent();
    if (parent != null && parent.relation() == null)
      // parent.relation() triggers cascading resolution
      throw new IllegalStateException("fail to resolve relation");

    if (relation != null) return relation; // already resolved beforehand or during cascading
    else if (isRelationBoundary(root())) return relation = resolve(root());
    else if (parent != null) return relation = parent.relation();
    else throw new IllegalStateException("cannot resolve relation for " + nodeType());
  }

  @Override
  public SQLNode parent() {
    return ctx.getTree(ctx.getParent(this));
  }

  @Override
  public void setParent(SQLNode parent) {
    ctx.setParent(this, retrieveTree(parent).root());

    relation = null;
    remove(ATTRIBUTE);
  }

  @Override
  public void setNodeType(NodeType type) {
    root().setNodeType(type);

    relation = null;
    remove(ATTRIBUTE);
  }

  @Override
  public void update(SQLNode other) {
    ctx.setRoot(this, root = retrieveTree(other).root());

    relation = null;
    remove(ATTRIBUTE);
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
